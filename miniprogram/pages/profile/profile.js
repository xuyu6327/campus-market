// pages/profile/profile.js - 我的
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    user: {},
    avatarInitial: '?',
    creditDesc: '初始信用分 100',
    unreadTotal: 0,
    showEdit: false,
    editAvatar: '',
    editForm: { nickname: '', studentId: '', realName: '', wechat: '', qq: '' },
    saving: false
  },

  onShow() {
    if (!request.isLoggedIn()) {
      util.redirectToLogin();
      return;
    }
    this.loadProfile();
    this.loadUnread();
  },

  loadProfile() {
    request.get('/user/info')
      .then((u) => {
        const score = u.creditScore;
        let desc = '初始信用分 100';
        if (score >= 150) desc = '信用优秀 ★';
        else if (score >= 100) desc = '信用良好';
        else if (score >= 50) desc = '信用一般';
        else desc = '信用较低，注意履约';
        // 头像 URL 转绝对路径
        u.avatar = util.resolveUrl(u.avatar);
        this.setData({
          user: u,
          avatarInitial: u.nickname ? u.nickname.charAt(0) : '?',
          creditDesc: desc
        });
        getApp().globalData.user = u;
      })
      .catch((err) => util.toast(err.message || '加载失败'));
  },

  loadUnread() {
    getApp().refreshUnread();
    this.setData({ unreadTotal: getApp().globalData.unreadTotal });
  },

  // ---------- 导航 ----------
  goMyGoods() { wx.navigateTo({ url: '/pages/my-goods/my-goods' }); },
  goFavorites() { wx.switchTab({ url: '/pages/favorites/favorites' }); },
  goHistory() { wx.navigateTo({ url: '/pages/history/history' }); },
  goOrders() { wx.navigateTo({ url: '/pages/orders/orders' }); },
  goNotifications() { wx.switchTab({ url: '/pages/notifications/notifications' }); },
  goReviews() { wx.navigateTo({ url: '/pages/reviews/reviews' }); },
  goCreditLogs() { wx.navigateTo({ url: '/pages/credit-logs/credit-logs' }); },

  // ---------- 编辑资料 ----------
  openEdit() {
    const u = this.data.user;
    this.setData({
      showEdit: true,
      editAvatar: util.resolveUrl(u.avatar),
      editForm: {
        nickname: u.nickname || '',
        studentId: u.studentId || '',
        realName: u.realName || '',
        wechat: u.wechat || '',
        qq: u.qq || ''
      }
    });
  },

  closeEdit() { this.setData({ showEdit: false }); },
  noop() {},

  onEditInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['editForm.' + field]: e.detail.value });
  },

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const file = (res.tempFiles || [])[0];
        if (!file) return;
        wx.showLoading({ title: '上传中...', mask: true });
        request.upload(file.tempFilePath)
          .then((url) => {
            wx.hideLoading();
            this.setData({ editAvatar: util.resolveUrl(url), pendingAvatar: url });
            util.toast('头像已选择，保存后生效');
          })
          .catch((err) => {
            wx.hideLoading();
            util.toast(err.message || '上传失败');
          });
      }
    });
  },

  saveProfile() {
    const u = this.data.user;
    const f = this.data.editForm;
    const data = {};
    if (f.nickname.trim() && f.nickname.trim() !== u.nickname) data.nickname = f.nickname.trim();
    if (f.studentId.trim() && f.studentId.trim() !== u.studentId) data.studentId = f.studentId.trim();
    if (f.realName.trim() && f.realName.trim() !== u.realName) data.realName = f.realName.trim();
    if (f.wechat.trim() && f.wechat.trim() !== u.wechat) data.wechat = f.wechat.trim();
    if (f.qq.trim() && f.qq.trim() !== u.qq) data.qq = f.qq.trim();
    if (this.data.pendingAvatar) data.avatar = this.data.pendingAvatar;

    if (Object.keys(data).length === 0) { this.closeEdit(); return; }
    this.setData({ saving: true });

    request.put('/user/info', data)
      .then(() => {
        util.toast('保存成功');
        this.setData({ showEdit: false, saving: false });
        this.loadProfile();
      })
      .catch((err) => {
        util.toast(err.message || '保存失败');
        this.setData({ saving: false });
      });
  },

  // ---------- 退出 ----------
  doLogout() {
    util.confirm('退出登录', '确定退出登录吗？').then((ok) => {
      if (!ok) return;
      request.clearLogin();
      getApp().globalData.user = null;
      wx.reLaunch({ url: '/pages/auth/auth' });
    });
  }
});
