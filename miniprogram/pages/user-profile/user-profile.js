// pages/user-profile/user-profile.js - 用户公开主页
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    userId: null,
    user: {},
    avatarInitial: '?',
    goodsList: [],
    goodsPageNum: 1,
    goodsHasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    reviews: []
  },

  onLoad(options) {
    const id = options.id;
    if (!id) { util.toast('缺少用户ID'); return; }
    this.setData({ userId: id });
    this.loadProfile();
    this.loadGoods(true);
    this.loadReviews();
  },

  loadProfile() {
    request.get('/user/' + this.data.userId + '/profile', {}, { noToast: true })
      .then((u) => {
        u.avatar = util.resolveUrl(u.avatar);
        this.setData({
          user: u,
          avatarInitial: u.nickname ? u.nickname.charAt(0) : '?'
        });
        wx.setNavigationBarTitle({ title: (u.nickname || '用户') + '的主页' });
      })
      .catch(() => {});
  },

  onReachBottom() {
    this.loadGoods(false);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.goodsHasMore) return;
    this.setData({ loading: true });

    request.get('/goods/user/' + this.data.userId, { pageNum: this.data.goodsPageNum, pageSize: 10 }, { noToast: true })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => Object.assign({}, g, {
          coverImage: util.resolveUrl(g.coverImage),
          priceText: util.formatPrice(g.price),
          conditionName: util.CONDITION_MAP[g.goodsCondition] || ''
        }));
        const goodsList = reset ? list : this.data.goodsList.concat(list);
        const goodsHasMore = records.length >= 10;
        this.setData({
          goodsList,
          goodsHasMore,
          goodsPageNum: this.data.goodsPageNum + 1,
          loading: false,
          loadMoreText: goodsHasMore ? '上拉加载更多' : (goodsList.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  loadReviews() {
    request.get('/review/user/' + this.data.userId, { pageNum: 1, pageSize: 10 }, { noToast: true })
      .then((data) => {
        const records = (data && data.records) || [];
        this.setData({
          reviews: records.map((r) => Object.assign({}, r, {
            starText: '★'.repeat(Math.max(0, r.score || 0)),
            timeAgo: util.timeAgo(r.createTime)
          }))
        });
      })
      .catch(() => {});
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  goReport() {
    if (!util.requireAuth()) return;
    wx.navigateTo({ url: '/pages/report/report?targetType=1&targetId=' + this.data.userId });
  }
});
