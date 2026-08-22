// pages/order-detail/order-detail.js - 订单详情
const request = require('../../utils/request');
const util = require('../../utils/util');

const STATUS_ICONS = { 0: '⏰', 1: '⭐', 2: '✕', 3: '✕', 4: '⏰' };

Page({
  data: {
    orderId: null,
    order: null,
    priceText: '0',
    goodsCoverImage: '',
    peerRole: '',
    peerName: '',
    statusIcon: '⏰',
    createTimeText: '',
    tradeTimeText: '',
    showActions: false,
    // 评价
    showReview: false,
    reviewScore: 5,
    reviewContent: '',
    reviewAnonymous: true,
    submittingReview: false
  },

  onLoad(options) {
    const id = options.id;
    if (!id) { util.toast('缺少订单ID'); return; }
    this.setData({ orderId: id });
    this.loadOrder();
  },

  loadOrder() {
    request.get('/order/' + this.data.orderId)
      .then((o) => {
        const peerRole = o.isBuyer ? '卖家' : '买家';
        const peerName = o.isBuyer ? (o.sellerNickname || '未知') : (o.buyerNickname || '未知');
        const showActions = (o.status === 0) || (o.status === 1);
        this.setData({
          order: Object.assign({}, o, { statusName: util.ORDER_STATUS_MAP[o.status] || '未知' }),
          priceText: util.formatPrice(o.goodsPrice),
          goodsCoverImage: util.resolveUrl(o.goodsCoverImage),
          peerRole,
          peerName,
          statusIcon: STATUS_ICONS[o.status] || '📋',
          createTimeText: util.formatDateTime(o.createTime),
          tradeTimeText: util.formatDateTime(o.tradeTime),
          showActions
        });
      })
      .catch((err) => {
        // 保留 order 为空，展示错误空态
        this.setData({ loadError: err.message || '加载失败' });
      });
  },

  goGoods(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  // ---------- 操作 ----------
  cancelOrder(e) {
    const role = e.currentTarget.dataset.role;
    const tip = role === 'buyer' ? '（将扣除3信用分）' : '';
    util.confirm('取消订单', '确定取消此订单吗？' + tip).then((ok) => {
      if (!ok) return;
      const url = role === 'buyer'
        ? '/order/' + this.data.orderId + '/buyer-cancel'
        : '/order/' + this.data.orderId + '/seller-cancel';
      request.put(url)
        .then(() => { util.toast('已取消'); this.loadOrder(); })
        .catch((err) => util.toast(err.message));
    });
  },

  confirmOrder() {
    util.confirm('确认交易', '确认交易已完成？').then((ok) => {
      if (!ok) return;
      request.put('/order/' + this.data.orderId + '/confirm')
        .then(() => { util.toast('交易完成！'); this.loadOrder(); })
        .catch((err) => util.toast(err.message));
    });
  },

  contactFail() {
    request.post('/order/' + this.data.orderId + '/contact-fail')
      .then(() => { util.toast('已提交，24h内卖家未响应将自动取消'); this.loadOrder(); })
      .catch((err) => util.toast(err.message));
  },

  // ---------- 评价 ----------
  openReviewModal() {
    this.setData({ showReview: true, reviewScore: 5, reviewContent: '', reviewAnonymous: true });
  },

  closeReview() {
    this.setData({ showReview: false });
  },

  noop() {},

  onStarTap(e) {
    this.setData({ reviewScore: Number(e.currentTarget.dataset.score) });
  },

  onReviewContent(e) {
    this.setData({ reviewContent: e.detail.value });
  },

  toggleAnon() {
    this.setData({ reviewAnonymous: !this.data.reviewAnonymous });
  },

  submitReview() {
    if (this.data.submittingReview) return;
    const content = this.data.reviewContent.trim();
    this.setData({ submittingReview: true });
    request.post('/review', {
      orderId: parseInt(this.data.orderId, 10),
      score: this.data.reviewScore,
      content: content || undefined,
      isAnonymous: this.data.reviewAnonymous ? 1 : 0
    })
      .then(() => {
        util.toast('评价成功');
        this.setData({ showReview: false, submittingReview: false });
        this.loadOrder();
      })
      .catch((err) => {
        util.toast(err.message || '评价失败');
        this.setData({ submittingReview: false });
      });
  }
});
