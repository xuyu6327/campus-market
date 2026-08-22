// pages/auth/auth.js - 登录注册
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    mode: 'login',
    submitting: false,
    account: '',
    password: '',
    rPhone: '',
    rPassword: '',
    rNickname: '',
    rStudentId: '',
    rRealName: ''
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },

  onAccount(e) { this.setData({ account: e.detail.value }); },
  onPassword(e) { this.setData({ password: e.detail.value }); },
  onRPhone(e) { this.setData({ rPhone: e.detail.value }); },
  onRPassword(e) { this.setData({ rPassword: e.detail.value }); },
  onRNickname(e) { this.setData({ rNickname: e.detail.value }); },
  onRStudentId(e) { this.setData({ rStudentId: e.detail.value }); },
  onRRealName(e) { this.setData({ rRealName: e.detail.value }); },

  doLogin() {
    const account = this.data.account.trim();
    const password = this.data.password;
    if (!account) { util.toast('请输入账号'); return; }
    if (!password) { util.toast('请输入密码'); return; }
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    request.post('/user/login', { account, password }, { noAuth: true, noToast: true })
      .then((data) => {
        // data: {token, userId, nickname, avatar, role, creditScore}
        request.setLogin(data.token, {
          userId: data.userId,
          nickname: data.nickname,
          avatar: data.avatar || '',
          role: data.role,
          creditScore: data.creditScore
        });
        getApp().globalData.user = data;
        util.toast('登录成功');
        setTimeout(() => this.afterLogin(), 600);
      })
      .catch((err) => {
        util.toast(err.message || '登录失败');
        this.setData({ submitting: false });
      });
  },

  afterLogin() {
    // 登录后回退到来源页；无来源则回首页
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.reLaunch({ url: '/pages/index/index' });
    }
  },

  doRegister() {
    const phone = this.data.rPhone.trim();
    const password = this.data.rPassword;
    const nickname = this.data.rNickname.trim();
    if (!util.validators.phone(phone)) { util.toast('手机号格式不正确'); return; }
    if (!password || password.length < 6 || password.length > 20) { util.toast('密码长度6-20位'); return; }
    if (!nickname) { util.toast('请输入昵称'); return; }
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    const payload = { phone, password, nickname };
    if (this.data.rStudentId.trim()) payload.studentId = this.data.rStudentId.trim();
    if (this.data.rRealName.trim()) payload.realName = this.data.rRealName.trim();

    request.post('/user/register', payload, { noAuth: true, noToast: true })
      .then(() => {
        util.toast('注册成功，请登录');
        this.setData({
          submitting: false,
          mode: 'login',
          account: phone,
          password: ''
        });
      })
      .catch((err) => {
        util.toast(err.message || '注册失败');
        this.setData({ submitting: false });
      });
  }
});
