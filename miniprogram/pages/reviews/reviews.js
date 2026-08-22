// pages/reviews/reviews.js - 我的评价（发出的/收到的）
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    mode: 'sent',   // sent 我发出的 / received 我收到的
    reviews: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多'
  },

  onLoad() {
    this.loadReviews(true);
  },

  onReachBottom() {
    this.loadReviews(false);
  },

  onModeTap(e) {
    const mode = e.currentTarget.dataset.mode;
    if (mode === this.data.mode) return;
    this.setData({ mode, pageNum: 1, hasMore: true });
    this.loadReviews(true);
  },

  loadReviews(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const url = this.data.mode === 'sent' ? '/review/sent' : '/review/received';
    request.get(url, { pageNum: this.data.pageNum, pageSize: 10 })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((r) => {
          const isSent = this.data.mode === 'sent';
          return Object.assign({}, r, {
            starText: '★'.repeat(Math.max(0, r.score || 0)),
            peerLabel: isSent ? '被评价人' : '评价人',
            peerName: isSent ? (r.evaluateeNickname || '匿名') : (r.evaluatorNickname || '匿名'),
            timeText: util.formatDateTime(r.createTime)
          });
        });
        const reviews = reset ? list : this.data.reviews.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          reviews,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (reviews.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  }
});
