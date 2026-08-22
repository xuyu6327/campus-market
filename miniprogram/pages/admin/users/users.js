// pages/admin/users/users.js - 用户管理
const request = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    keyword: '',
    status: '',
    users: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    showDetailModal: false,
    detail: {},
    showCredit: false,
    creditValue: '',
    creditReason: ''
  },

  onLoad() {
    this.loadUsers(true);
  },

  onReachBottom() {
    this.loadUsers(false);
  },

  onKeyword(e) { this.setData({ keyword: e.detail.value }); },
  onSearch() { this.reload(); },

  onStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status || '' });
    this.reload();
  },

  reload() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadUsers(true);
  },

  loadUsers(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    const params = { pageNum: this.data.pageNum, pageSize: 10 };
    if (this.data.status !== '') params.status = this.data.status;
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();

    request.get('/admin/user/list', params)
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((u) => Object.assign({}, u, {
          avatar: util.resolveUrl(u.avatar),
          avatarText: u.nickname ? u.nickname.charAt(0) : '?'
        }));
        const users = reset ? list : this.data.users.concat(list);
        const hasMore = records.length >= 10;
        this.setData({
          users,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (users.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  // ---------- 详情 ----------
  showDetail(e) {
    const id = e.currentTarget.dataset.id;
    request.get('/admin/user/' + id, {}, { noToast: true })
      .then((u) => {
        this.setData({
          detail: Object.assign({}, u, {
            avatar: util.resolveUrl(u.avatar),
            createTimeText: util.formatDateTime(u.createTime)
          }),
          showDetailModal: true
        });
      })
      .catch((err) => util.toast(err.message || '加载失败'));
  },

  closeDetail() { this.setData({ showDetailModal: false }); },
  noop() {},

  // ---------- 操作 ----------
  banUser() {
    const id = this.data.detail.id;
    util.confirm('封禁用户', '确定封禁该用户吗？封禁后无法登录。').then((ok) => {
      if (!ok) return;
      request.put('/admin/user/' + id + '/ban')
        .then(() => { util.toast('已封禁'); this.closeDetail(); this.reload(); })
        .catch((err) => util.toast(err.message));
    });
  },

  enableUser() {
    const id = this.data.detail.id;
    request.put('/admin/user/' + id + '/enable')
      .then(() => { util.toast('已启用'); this.closeDetail(); this.reload(); })
      .catch((err) => util.toast(err.message));
  },

  resetPwd() {
    const id = this.data.detail.id;
    util.confirm('重置密码', '确定将该用户密码重置为 admin123 吗？').then((ok) => {
      if (!ok) return;
      request.put('/admin/user/' + id + '/reset-password')
        .then(() => { util.toast('已重置为 admin123'); })
        .catch((err) => util.toast(err.message));
    });
  },

  openCredit() {
    this.setData({ showCredit: true, creditValue: '', creditReason: '' });
  },

  closeCredit() { this.setData({ showCredit: false }); },

  onCreditValue(e) { this.setData({ creditValue: e.detail.value }); },
  onCreditReason(e) { this.setData({ creditReason: e.detail.value }); },

  submitCredit() {
    const changeValue = parseInt(this.data.creditValue, 10);
    const reason = this.data.creditReason.trim();
    if (!changeValue || isNaN(changeValue)) { util.toast('请输入变更值（不可为0）'); return; }
    if (!reason) { util.toast('请输入调整原因'); return; }

    request.put('/admin/user/' + this.data.detail.id + '/credit', {
      changeValue,
      reason
    })
      .then(() => {
        util.toast('已调整');
        this.setData({ showCredit: false });
        this.closeDetail();
        this.reload();
      })
      .catch((err) => util.toast(err.message));
  }
});
