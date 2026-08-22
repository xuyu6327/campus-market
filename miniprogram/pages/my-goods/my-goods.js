// pages/my-goods/my-goods.js - 我的发布
const request = require('../../utils/request');
const util = require('../../utils/util');

const STATUS_TAG_CLASS = { 0: 'tag-gray', 1: 'tag-success', 2: 'tag-warning', 3: 'tag-info', 4: 'tag-warning' };

Page({
  data: {
    activeStatus: '',
    statusTabs: [
      { value: '1', label: '在售' },
      { value: '2', label: '已预订' },
      { value: '3', label: '已售出' },
      { value: '4', label: '待审核' },
      { value: '0', label: '已下架' }
    ],
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

  onStatusTap(e) {
    const s = e.currentTarget.dataset.status || '';
    this.setData({ activeStatus: s, pageNum: 1, hasMore: true });
    this.loadGoods(true);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.activeStatus !== '') params.status = this.data.activeStatus;

    request.get('/goods/my', params)
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => {
          const forceDown = g.status === 0 && g.takedownBy === 1;
          return Object.assign({}, g, {
            coverImage: util.resolveUrl(g.coverImage),
            priceText: util.formatPrice(g.price),
            statusName: util.GOODS_STATUS_MAP[g.status] || '',
            statusTagClass: STATUS_TAG_CLASS[g.status] || 'tag-gray',
            timeAgo: util.timeAgo(g.createTime),
            forceDown
          });
        });
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
      .catch(() => {
        this.setData({ loading: false, loadMoreText: '加载失败，请重试' });
      });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  takeDown(e) {
    const id = e.currentTarget.dataset.id;
    util.confirm('下架商品', '确定下架该商品吗？下架后其他用户将无法看到和预订。').then((ok) => {
      if (!ok) return;
      request.put('/goods/' + id + '/takedown')
        .then(() => { util.toast('已下架'); this.reload(); })
        .catch((err) => util.toast(err.message));
    });
  },

  relist(e) {
    const id = e.currentTarget.dataset.id;
    util.confirm('重新上架', '确定重新上架该商品吗？').then((ok) => {
      if (!ok) return;
      request.put('/goods/' + id + '/relist')
        .then(() => { util.toast('已上架'); this.reload(); })
        .catch((err) => util.toast(err.message));
    });
  },

  editGoods(e) {
    const id = e.currentTarget.dataset.id;
    // publish 是 tabBar 页，switchTab 不支持 query 参数，用全局标志传递编辑目标
    getApp().globalData.editGoodsId = id;
    wx.switchTab({ url: '/pages/publish/publish' });
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadGoods(true);
  }
});
