// pages/admin/reports/reports.js - 举报管理
const request = require('../../../utils/request');
const util = require('../../../utils/util');

const STATUS_CLASS = { 0: 'tag-warning', 1: 'tag-info', 2: 'tag-danger', 3: 'tag-danger', 4: 'tag-gray' };

Page({
  data: {
    status: '',
    reports: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    showDetailModal: false,
    detail: {},
    handleStatus: 1,
    handleResult: '',
    isMalicious: false,
    submitting: false
  },

  onLoad() {
    this.loadReports(true);
  },

  onReachBottom() {
    this.loadReports(false);
  },

  onStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status || '' });
    this.reload();
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadReports(true);
  },

  loadReports(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.status !== '') params.status = this.data.status;

    request.get('/admin/report/list', params)
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((r) => Object.assign({}, r, {
          statusClass: STATUS_CLASS[r.status] || 'tag-gray',
          timeAgo: util.timeAgo(r.createTime)
        }));
        const reports = reset ? list : this.data.reports.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          reports,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (reports.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  showDetail(e) {
    const id = e.currentTarget.dataset.id;
    request.get('/admin/report/' + id, {}, { noToast: true })
      .then((r) => {
        this.setData({
          detail: Object.assign({}, r, { createTimeText: util.formatDateTime(r.createTime) }),
          showDetailModal: true,
          handleStatus: 1,
          handleResult: '',
          isMalicious: false
        });
      })
      .catch((err) => util.toast(err.message || '加载失败'));
  },

  closeDetail() { this.setData({ showDetailModal: false }); },
  noop() {},

  onHandleStatus(e) {
    this.setData({ handleStatus: Number(e.currentTarget.dataset.status) });
  },

  onHandleResult(e) { this.setData({ handleResult: e.detail.value }); },

  toggleMalicious() {
    this.setData({ isMalicious: !this.data.isMalicious });
  },

  submitHandle() {
    const result = this.data.handleResult.trim();
    if (!result) { util.toast('请填写处理结果说明'); return; }
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    request.put('/admin/report/' + this.data.detail.id + '/handle', {
      status: this.data.handleStatus,
      handleResult: result,
      isMalicious: this.data.isMalicious
    })
      .then(() => {
        util.toast('处理完成');
        this.setData({ showDetailModal: false, submitting: false });
        this.reload();
      })
      .catch((err) => {
        util.toast(err.message);
        this.setData({ submitting: false });
      });
  }
});
