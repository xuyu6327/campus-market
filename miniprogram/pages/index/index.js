// pages/index/index.js - 首页：分页 / 分类 / 价格 / 成色 / 排序（含热门 popular）
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    keyword: '',
    showHot: false,
    hotWords: ['教材', '考研资料', '手机', '自行车', '台灯', '耳机', '四六级'],
    categories: [],
    activeCat: '',          // '' = 全部
    sortBy: 'newest',       // newest / price_asc / price_desc / popular
    goodsCondition: '',     // '' = 全部
    conditionLabel: '成色',
    conditionOptions: [
      { value: '1', label: '全新未拆' },
      { value: '2', label: '几乎全新' },
      { value: '3', label: '轻微使用痕迹' },
      { value: '4', label: '明显使用痕迹' },
      { value: '5', label: '故障/坏件' }
    ],
    showCondition: false,
    showPrice: false,
    minPrice: '',
    maxPrice: '',
    goodsList: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多'
  },

  onLoad() {
    this.loadCategories();
    this.loadGoods(true);
  },

  onShow() {
    // tab 页：刷新未读角标（app 全局）
    getApp().refreshUnread();
  },

  onReachBottom() {
    this.loadGoods(false);
  },

  // ---------- 搜索 ----------
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    const kw = this.data.keyword.trim();
    if (!kw) { util.toast('请输入搜索关键词'); return; }
    this.setData({ showHot: false });
    this.reloadList();
  },

  onHotTap(e) {
    this.setData({ keyword: e.currentTarget.dataset.word, showHot: false });
    this.reloadList();
  },

  // 搜索框聚焦时展示热门搜索词
  showHotPanel() {
    this.setData({ showHot: true });
  },

  // ---------- 分类 ----------
  onCatTap(e) {
    const id = e.currentTarget.dataset.id || '';
    this.setData({ activeCat: id });
    this.reloadList();
  },

  // ---------- 排序 ----------
  onSortTap(e) {
    this.setData({ sortBy: e.currentTarget.dataset.sort });
    this.reloadList();
  },

  // ---------- 成色筛选 ----------
  toggleCondition() {
    this.setData({ showCondition: !this.data.showCondition, showPrice: false });
  },

  onConditionTap(e) {
    const v = e.currentTarget.dataset.value;
    const label = v ? (util.CONDITION_MAP[v] || '成色') : '成色';
    this.setData({ goodsCondition: v, conditionLabel: label, showCondition: false });
    this.reloadList();
  },

  // ---------- 价格区间 ----------
  togglePrice() {
    this.setData({ showPrice: !this.data.showPrice, showCondition: false });
  },

  onMinPriceInput(e) { this.setData({ minPrice: e.detail.value }); },
  onMaxPriceInput(e) { this.setData({ maxPrice: e.detail.value }); },

  onPriceFilter() {
    this.setData({ showPrice: false });
    this.reloadList();
  },

  // ---------- 列表 ----------
  reloadList() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadGoods(true);
  },

  loadGoods(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = {
      pageNum: this.data.pageNum,
      pageSize: 10,
      sortBy: this.data.sortBy
    };
    if (this.data.activeCat) params.categoryId = this.data.activeCat;
    if (this.data.goodsCondition !== '') params.goodsCondition = this.data.goodsCondition;
    if (this.data.minPrice) params.minPrice = this.data.minPrice;
    if (this.data.maxPrice) params.maxPrice = this.data.maxPrice;
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();

    request.get('/goods/list', params, { noToast: true })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((g) => this.decorateGoods(g));
        const goodsList = reset ? list : this.data.goodsList.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          goodsList,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (goodsList.length > 0 ? '— 没有更多了 —' : '暂无商品')
        });
      })
      .catch(() => {
        this.setData({
          loading: false,
          loadMoreText: '加载失败，请重试'
        });
      });
  },

  decorateGoods(g) {
    return Object.assign({}, g, {
      coverImage: util.resolveUrl(g.coverImage),
      priceText: util.formatPrice(g.price),
      originalPriceText: util.formatPrice(g.originalPrice),
      conditionName: util.CONDITION_MAP[g.goodsCondition] || '',
      timeAgo: util.timeAgo(g.createTime)
    });
  },

  // ---------- 分类加载 ----------
  loadCategories() {
    request.get('/goods/category', {}, { noAuth: true, noToast: true })
      .then((list) => {
        // 只显示一级分类
        const cats = (list || []).filter((c) => c.parentId === 0);
        this.setData({ categories: cats });
      })
      .catch(() => {});
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
