// pages/detail/detail.js - 商品详情
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    goodsId: null,
    detail: {},
    images: [],
    priceText: '0',
    originalPriceText: '0',
    conditionName: '',
    timeAgo: '',
    sellerAvatar: '',
    sellerInitial: '?',
    isGuest: false,
    bookBtnText: '加载中...',
    bookBtnClass: 'primary',
    goodRate: null,
    reviews: [],
    // 预订弹窗
    showBookModal: false,
    bookPhone: '',
    bookQq: '',
    bookWechat: '',
    // 举报弹窗
    showReportModal: false,
    reportReasons: util.REPORT_REASONS,
    reportReasonIndex: -1,
    reportDesc: ''
  },

  onLoad(options) {
    const id = options.id;
    if (!id) { util.toast('缺少商品ID'); return; }
    this.setData({ goodsId: id });
    this.loadDetail();
  },

  onUnload() {
    this.setData({ showBookModal: false, showReportModal: false });
  },

  loadDetail() {
    request.get('/goods/detail/' + this.data.goodsId, {}, { noToast: true })
      .then((g) => {
        const isGuest = !request.isLoggedIn();
        this.setData({
          detail: g,
          images: (g.images || []).map((u) => util.resolveUrl(u)),
          priceText: util.formatPrice(g.price),
          originalPriceText: util.formatPrice(g.originalPrice),
          conditionName: util.CONDITION_MAP[g.goodsCondition] || '',
          timeAgo: util.timeAgo(g.createTime),
          sellerAvatar: util.resolveUrl(g.sellerAvatar),
          sellerInitial: g.sellerNickname ? g.sellerNickname.charAt(0) : '?',
          isGuest
        });
        this.renderBookBtn(g, isGuest);
        this.loadReviews(g.sellerId);
      })
      .catch((err) => {
        this.setData({ bookBtnText: '加载失败', bookBtnClass: 'offline' });
        util.toast(err.message || '加载失败');
      });
  },

  // 底部按钮逻辑（对照 Web 版 detail.html renderBottomBtn）
  renderBookBtn(g, isGuest) {
    let text = '加载中...';
    let cls = 'primary';
    if (isGuest) {
      text = '登录后预订';
      cls = 'primary';
    } else if (g.isOwner) {
      if (g.status === 1) { text = '下架商品'; cls = 'owner'; }
      else if (g.status === 0) { text = '重新上架'; cls = 'owner'; }
      else if (g.status === 2) { text = '已被预订'; cls = 'disabled'; }
      else if (g.status === 3) { text = '已售出'; cls = 'disabled'; }
      else if (g.status === 4) { text = '待审核'; cls = 'disabled'; }
    } else if (g.sellerStatus === 0) {
      text = '卖家已被冻结，暂停交易';
      cls = 'disabled';
    } else if (g.status === 0) { text = '已下架'; cls = 'offline'; }
    else if (g.status === 2) { text = '已被预订'; cls = 'disabled'; }
    else if (g.status === 3) { text = '已售出'; cls = 'disabled'; }
    else if (g.status === 4) { text = '已下架'; cls = 'offline'; }
    else { text = '我要预订'; cls = 'primary'; }
    this.setData({ bookBtnText: text, bookBtnClass: cls });
  },

  // 收藏
  toggleFavorite() {
    if (!util.requireAuth()) return;
    const g = this.data.detail;
    const wasFav = !!g.favorited;
    const p = wasFav
      ? request.del('/goods/' + this.data.goodsId + '/favorite', {}, { noToast: true })
      : request.post('/goods/' + this.data.goodsId + '/favorite', {}, { noToast: true });
    p.then(() => {
      const count = Math.max(0, (g.favoriteCount || 0) + (wasFav ? -1 : 1));
      this.setData({ 'detail.favorited': !wasFav, 'detail.favoriteCount': count });
      util.toast(wasFav ? '已取消收藏' : '已收藏');
    }).catch((err) => util.toast(err.message || '操作失败'));
  },

  // 私聊
  startChat() {
    if (!util.requireAuth()) return;
    const g = this.data.detail;
    if (!g || !g.sellerId) { util.toast('无法发起会话'); return; }
    request.post('/chat/conversation', { goodsId: parseInt(this.data.goodsId, 10), targetUserId: g.sellerId }, { noToast: true })
      .then((convId) => {
        wx.navigateTo({ url: '/pages/chat-detail/chat-detail?id=' + convId });
      })
      .catch((err) => util.toast(err.message || '发起会话失败'));
  },

  // 底部主按钮点击
  onBookTap() {
    const g = this.data.detail;
    const cls = this.data.bookBtnClass;
    if (!util.requireAuth()) return;
    if (cls === 'primary') {
      this.openBookModal();
    } else if (cls === 'owner') {
      if (g.status === 1) this.takeDown();
      else if (g.status === 0) this.relist();
    }
    // disabled / offline 无操作
  },

  takeDown() {
    util.confirm('下架商品', '确定下架该商品吗？下架后其他用户将无法看到和预订。').then((ok) => {
      if (!ok) return;
      request.put('/goods/' + this.data.goodsId + '/takedown')
        .then(() => { util.toast('已下架'); this.loadDetail(); })
        .catch((err) => util.toast(err.message || '操作失败'));
    });
  },

  relist() {
    util.confirm('重新上架', '确定重新上架该商品吗？').then((ok) => {
      if (!ok) return;
      request.put('/goods/' + this.data.goodsId + '/relist')
        .then(() => { util.toast('已重新上架'); this.loadDetail(); })
        .catch((err) => util.toast(err.message || '操作失败'));
    });
  },

  // ---------- 预订 ----------
  openBookModal() {
    this.setData({ showBookModal: true, bookPhone: '', bookQq: '', bookWechat: '' });
  },

  closeBookModal() {
    this.setData({ showBookModal: false });
  },

  noop() {},

  onBookPhone(e) { this.setData({ bookPhone: e.detail.value }); },
  onBookQq(e) { this.setData({ bookQq: e.detail.value }); },
  onBookWechat(e) { this.setData({ bookWechat: e.detail.value }); },

  submitBook() {
    const phone = this.data.bookPhone.trim();
    const qq = this.data.bookQq.trim();
    const wechat = this.data.bookWechat.trim();
    if (!phone && !qq && !wechat) { util.toast('请至少填写一种联系方式，方便卖家联系您'); return; }
    if (phone && !util.validators.phone(phone)) { util.toast('手机号格式不正确'); return; }
    if (qq && !util.validators.qq(qq)) { util.toast('QQ号应为5-15位数字'); return; }
    if (wechat && !util.validators.wechat(wechat)) { util.toast('微信号应为6-20位字母/数字/下划线'); return; }

    const payload = { goodsId: parseInt(this.data.goodsId, 10) };
    if (phone) payload.buyerPhone = phone;
    if (qq) payload.buyerQq = qq;
    if (wechat) payload.buyerWechat = wechat;

    request.post('/order', payload)
      .then(() => {
        this.setData({ showBookModal: false, bookBtnText: '已预订', bookBtnClass: 'disabled' });
        util.toast('预订成功！');
        this.loadDetail();
      })
      .catch((err) => util.toast(err.message || '预订失败'));
  },

  // ---------- 举报 ----------
  openReport() {
    if (!util.requireAuth()) return;
    this.setData({ showReportModal: true, reportReasonIndex: -1, reportDesc: '' });
  },

  closeReport() {
    this.setData({ showReportModal: false });
  },

  onReportReason(e) {
    this.setData({ reportReasonIndex: e.currentTarget.dataset.index });
  },

  onReportDesc(e) {
    this.setData({ reportDesc: e.detail.value });
  },

  submitReport() {
    const idx = this.data.reportReasonIndex;
    if (idx < 0) { util.toast('请选择举报类型'); return; }
    const reason = this.data.reportReasons[idx];
    const description = this.data.reportDesc.trim();
    request.post('/report', {
      targetType: 2,
      targetId: parseInt(this.data.goodsId, 10),
      reason: reason,
      description: description || ''
    }).then(() => {
      util.toast('举报已提交，等待处理');
      this.setData({ showReportModal: false });
    }).catch((err) => util.toast(err.message || '提交失败'));
  },

  // ---------- 卖家主页 / 评价 ----------
  goSellerProfile() {
    const g = this.data.detail;
    if (!g || !g.sellerId) return;
    wx.navigateTo({ url: '/pages/user-profile/user-profile?id=' + g.sellerId });
  },

  loadReviews(sellerId) {
    if (!sellerId) return;
    request.get('/review/user/' + sellerId, { pageNum: 1, pageSize: 5 }, { noToast: true })
      .then((data) => {
        const records = (data && data.records) || [];
        this.setData({
          goodRate: data && data.goodRate !== null && data.goodRate !== undefined ? data.goodRate : 100,
          reviews: records.map((r) => Object.assign({}, r, {
            starText: '★'.repeat(Math.max(0, r.score || 0)),
            timeAgo: util.timeAgo(r.createTime)
          }))
        });
      })
      .catch(() => {});
  }
});
