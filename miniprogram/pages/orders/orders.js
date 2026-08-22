// pages/orders/orders.js - 我的订单
const request = require('../../utils/request');
const util = require('../../utils/util');

const STATUS_CLASS = { 0: 'tag-warning', 1: 'tag-success', 2: 'tag-danger', 3: 'tag-danger', 4: 'tag-info' };

Page({
  data: {
    tab: 'buy',   // buy 我买到的 / sell 我卖出的
    keyword: '',
    activeStatus: '',
    statusOptions: [
      { value: '0', label: '待交易' },
      { value: '1', label: '已完成' },
      { value: '2', label: '买家取消' },
      { value: '3', label: '卖家取消' },
      { value: '4', label: '超时取消' }
    ],
    orders: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多'
  },

  onLoad() {
    this.loadOrders(true);
  },

  onReachBottom() {
    this.loadOrders(false);
  },

  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab === this.data.tab) return;
    this.setData({ tab, pageNum: 1, hasMore: true });
    this.loadOrders(true);
  },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },

  doSearch() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadOrders(true);
  },

  onStatusTap(e) {
    const s = e.currentTarget.dataset.status || '';
    this.setData({ activeStatus: s, pageNum: 1, hasMore: true });
    this.loadOrders(true);
  },

  loadOrders(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const url = this.data.tab === 'buy' ? '/order/buy' : '/order/sell';
    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.activeStatus !== '') params.status = parseInt(this.data.activeStatus, 10);
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();

    request.get(url, params)
      .then((page) => {
        const records = (page && page.records) || [];
        const isBuy = this.data.tab === 'buy';
        const list = records.map((o) => Object.assign({}, o, {
          goodsCoverImage: util.resolveUrl(o.goodsCoverImage),
          priceText: util.formatPrice(o.goodsPrice),
          statusName: util.ORDER_STATUS_MAP[o.status] || '未知',
          statusClass: STATUS_CLASS[o.status] || 'tag-info',
          otherLabel: isBuy ? '卖家' : '买家',
          otherName: isBuy ? (o.sellerNickname || '未知') : (o.buyerNickname || '未知'),
          timeAgo: util.timeAgo(o.createTime)
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

  goDetail(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id });
  }
});
