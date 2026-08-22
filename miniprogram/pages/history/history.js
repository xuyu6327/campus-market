// pages/history/history.js - 浏览历史
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    goodsList: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多'
  },

  onLoad() {
    this.loadGoods(true);
  },

  onReachBottom() {
    this.loadGoods(false);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    request.get('/goods/history', { pageNum: this.data.pageNum, pageSize: 10 })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => Object.assign({}, g, {
          coverImage: util.resolveUrl(g.coverImage),
          priceText: util.formatPrice(g.price),
          conditionName: util.CONDITION_MAP[g.goodsCondition] || ''
        }));
        const goodsList = reset ? list : this.data.goodsList.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          goodsList,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (goodsList.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
