// pages/chat-detail/chat-detail.js - 聊天窗口（5 秒轮询增量拉取，onHide/onUnload 清理定时器）
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    convId: null,
    messages: [],
    inputText: '',
    loading: true,
    scrollToId: ''
  },

  // 轮询状态
  _timer: null,
  _lastId: 0,       // 已加载最后一条消息 ID（增量轮询基准）
  _loaded: false,
  _otherAvatar: '',
  _otherNickname: '',
  _me: {},

  onLoad(options) {
    const convId = options.id;
    if (!convId) { util.toast('缺少会话ID'); return; }
    this._me = request.getCurrentUser() || {};
    this.setData({ convId });

    // 会话信息：对方昵称/头像
    request.get('/chat/' + convId + '/info', {}, { noToast: true })
      .then((info) => {
        if (info) {
          this._otherAvatar = info.otherAvatar || '';
          this._otherNickname = info.otherNickname || '聊天';
          wx.setNavigationBarTitle({ title: this._otherNickname });
          // 刷新已有消息的头像
          this.refreshAvatars();
        }
      })
      .catch(() => {});

    this.loadInitial();
    // 5 秒轮询增量拉取
    this._timer = setInterval(() => this.poll(), 5000);
  },

  onHide() {
    this.stopPoll();
  },

  onUnload() {
    this.stopPoll();
  },

  stopPoll() {
    if (this._timer) {
      clearInterval(this._timer);
      this._timer = null;
    }
  },

  // 首载：最近 50 条
  loadInitial() {
    request.get('/chat/' + this.data.convId + '/messages', {}, { noToast: true })
      .then((list) => {
        this._loaded = true;
        this.setData({
          messages: (list || []).map((m) => this.decorate(m)),
          loading: false
        });
        this.scrollBottom();
      })
      .catch(() => {
        this.setData({ loading: false });
        util.toast('加载失败');
      });
  },

  // 增量轮询
  poll() {
    if (!this._loaded) return;
    request.get('/chat/' + this.data.convId + '/messages', { afterId: this._lastId }, { noToast: true })
      .then((list) => {
        if (!list || list.length === 0) return;
        const newOnes = list.map((m) => this.decorate(m));
        this.setData({ messages: this.data.messages.concat(newOnes) });
        this.scrollBottom();
      })
      .catch(() => {});
  },

  // 装饰消息：mine/头像/时间；更新 _lastId
  decorate(m) {
    if (m.id > this._lastId) this._lastId = m.id;
    const mine = m.senderId === this._me.userId;
    return {
      id: m.id,
      content: m.content || '',
      mine,
      avatarUrl: util.resolveUrl(mine ? (this._me.avatar || '') : this._otherAvatar),
      avatarText: mine ? (this._me.nickname ? this._me.nickname.charAt(0) : '我') : (this._otherNickname.charAt(0) || '?'),
      timeText: util.formatChatTime(m.createTime)
    };
  },

  refreshAvatars() {
    const messages = this.data.messages.map((m) => Object.assign({}, m, {
      avatarUrl: util.resolveUrl(m.mine ? (this._me.avatar || '') : this._otherAvatar),
      avatarText: m.mine ? (this._me.nickname ? this._me.nickname.charAt(0) : '我') : (this._otherNickname.charAt(0) || '?')
    }));
    this.setData({ messages });
  },

  scrollBottom() {
    const msgs = this.data.messages;
    if (msgs.length > 0) {
      this.setData({ scrollToId: 'msg-' + msgs[msgs.length - 1].id });
    }
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  send() {
    const text = this.data.inputText.trim();
    if (!text) return;
    request.post('/chat/' + this.data.convId + '/messages', { content: text })
      .then(() => {
        this.setData({ inputText: '' });
        // 立即拉取增量补上自己刚发的消息
        request.get('/chat/' + this.data.convId + '/messages', { afterId: this._lastId }, { noToast: true })
          .then((list) => {
            if (!list || list.length === 0) return;
            const newOnes = list.map((m) => this.decorate(m));
            this.setData({ messages: this.data.messages.concat(newOnes) });
            this.scrollBottom();
          })
          .catch(() => {});
      })
      .catch((err) => util.toast(err.message || '发送失败'));
  }
});
