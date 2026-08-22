// pages/credit-logs/credit-logs.js - 信用分变更记录
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    currentScore: null,
    logs: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多'
  },

  onLoad() {
    request.get('/user/credit', {}, { noToast: true })
      .then((score) => this.setData({ currentScore: score }))
      .catch(() => {});
    this.loadLogs(true);
  },

  onReachBottom() {
    this.loadLogs(false);
  },

  loadLogs(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    request.get('/user/credit/logs', { pageNum: this.data.pageNum, pageSize: 20 })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((l) => Object.assign({}, l, {
          timeText: util.formatDateTime(l.createTime)
        }));
        const logs = reset ? list : this.data.logs.concat(list);
        const hasMore = records.length >= 20;
        this.setData({
          logs,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (logs.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  }
});
