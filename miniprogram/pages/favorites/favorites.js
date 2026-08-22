// pages/favorites/favorites.js - 我的收藏
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

  onShow() {
    if (!request.isLoggedIn()) {
      util.redirectToLogin();
      return;
    }
    // 可能从详情页取消收藏后返回，刷新列表
    this.reload();
  },

  onReachBottom() {
    this.loadGoods(false);
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadGoods(true);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    request.get('/goods/favorites', { pageNum: this.data.pageNum, pageSize: 10 })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => Object.assign({}, g, {
          coverImage: util.resolveUrl(g.coverImage),
          priceText: util.formatPrice(g.price),
          originalPriceText: util.formatPrice(g.originalPrice),
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
  },

  removeFav(e) {
    const id = e.currentTarget.dataset.id;
    request.del('/goods/' + id + '/favorite')
      .then(() => {
        util.toast('已取消收藏');
        this.setData({ goodsList: this.data.goodsList.filter((g) => g.id !== id) });
      })
      .catch((err) => util.toast(err.message));
  }
});
