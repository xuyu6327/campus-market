// pages/admin/goods/goods.js - 商品管理
const request = require('../../../utils/request');
const util = require('../../../utils/util');

const STATUS_CLASS = { 0: 'tag-gray', 1: 'tag-success', 2: 'tag-warning', 3: 'tag-info', 4: 'tag-warning' };

Page({
  data: {
    keyword: '',
    status: '',
    goods: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    showDetailModal: false,
    detail: {},
    showTakedown: false,
    takedownReason: '',
    showReview: false,
    reviewReason: ''
  },

  onLoad() {
    this.loadGoods(true);
  },

  onReachBottom() {
    this.loadGoods(false);
  },

  onKeyword(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.reload(); },

  onStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status || '' });
    this.reload();
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadGoods(true);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.status !== '') params.status = this.data.status;
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();

    request.get('/admin/goods/list', params)
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => {
          const images = util.parseImages(g.images);
          return Object.assign({}, g, {
            coverImage: util.resolveUrl(images[0]),
            priceText: util.formatPrice(g.price),
            statusClass: STATUS_CLASS[g.status] || 'tag-gray'
          });
        });
        const goods = reset ? list : this.data.goods.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          goods,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (goods.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  // ---------- 详情 ----------
  showDetail(e) {
    const id = e.currentTarget.dataset.id;
    request.get('/admin/goods/' + id, {}, { noToast: true })
      .then((g) => {
        this.setData({
          detail: Object.assign({}, g, {
            images: util.parseImages(g.images).map((u) => util.resolveUrl(u)),
            priceText: util.formatPrice(g.price),
            originalPriceText: util.formatPrice(g.originalPrice),
            createTimeText: util.formatDateTime(g.createTime)
          }),
          showDetailModal: true
        });
      })
      .catch((err) => util.toast(err.message || '加载失败'));
  },

  closeDetail() { this.setData({ showDetailModal: false }); },
  noop() {},

  // ---------- 强制下架 ----------
  openTakedown() {
    this.setData({ showTakedown: true, takedownReason: '' });
  },

  closeTakedown() { this.setData({ showTakedown: false }); },

  onTakedownReason(e) { this.setData({ takedownReason: e.detail.value }); },

  submitTakedown() {
    const reason = this.data.takedownReason.trim();
    if (!reason) { util.toast('请填写下架原因'); return; }
    request.put('/admin/goods/' + this.data.detail.id + '/takedown', { reason })
      .then(() => {
        util.toast('已强制下架');
        this.setData({ showTakedown: false });
        this.closeDetail();
        this.reload();
      })
      .catch((err) => util.toast(err.message));
  },

  // ---------- 审核 ----------
  openReview() {
    this.setData({ showReview: true, reviewReason: '' });
  },

  closeReview() { this.setData({ showReview: false }); },

  onReviewReason(e) { this.setData({ reviewReason: e.detail.value }); },

  reviewApprove() {
    this.submitReview(true);
  },

  reviewReject() {
    const reason = this.data.reviewReason.trim();
    if (!reason) { util.toast('驳回时请填写原因'); return; }
    this.submitReview(false);
  },

  submitReview(approve) {
    request.put('/admin/goods/' + this.data.detail.id + '/review', {
      approve,
      reason: this.data.reviewReason.trim() || undefined
    })
      .then(() => {
        util.toast(approve ? '已通过，商品重新上架' : '已驳回');
        this.setData({ showReview: false });
        this.closeDetail();
        this.reload();
      })
      .catch((err) => util.toast(err.message));
  }
});
