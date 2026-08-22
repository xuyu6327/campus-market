// pages/admin/orders/orders.js - 订单管理
const request = require('../../../utils/request');
const util = require('../../../utils/util');

const STATUS_CLASS = { 0: 'tag-warning', 1: 'tag-success', 2: 'tag-danger', 3: 'tag-danger', 4: 'tag-info' };

Page({
  data: {
    keyword: '',
    status: '',
    orders: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    showDetailModal: false,
    detail: {}
  },

  onLoad() {
    this.loadOrders(true);
  },

  onReachBottom() {
    this.loadOrders(false);
  },

  onKeyword(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.reload(); },

  onStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status || '' });
    this.reload();
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadOrders(true);
  },

  loadOrders(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.status !== '') params.status = this.data.status;
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();

    request.get('/admin/order/list', params)
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((o) => Object.assign({}, o, {
          goodsImage: util.resolveUrl(o.goodsImage),
          priceText: util.formatPrice(o.goodsPrice),
          statusClass: STATUS_CLASS[o.status] || 'tag-info'
        }));
        const orders = reset ? list : this.data.orders.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          orders,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (orders.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  showDetail(e) {
    const id = e.currentTarget.dataset.id;
    request.get('/admin/order/' + id, {}, { noToast: true })
      .then((o) => {
        this.setData({
          detail: Object.assign({}, o, {
            priceText: util.formatPrice(o.goodsPrice),
            createTimeText: util.formatDateTime(o.createTime),
            tradeTimeText: util.formatDateTime(o.tradeTime),
            contactFailAtText: util.formatDateTime(o.contactFailAt)
          }),
          showDetailModal: true
        });
      })
      .catch((err) => util.toast(err.message || '加载失败'));
  },

  closeDetail() { this.setData({ showDetailModal: false }); },
  noop() {}
});
