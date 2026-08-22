// pages/notifications/notifications.js - 消息（通知 + 私信）
const request = require('../../utils/request');
const util = require('../../utils/util');

// 通知图标映射（type 1-7，与后端一致）
const TYPE_ICONS = {
  1: 'message', 2: 'clock', 3: 'clock', 4: 'phone', 5: 'star', 6: 'bell', 7: 'flag'
};

Page({
  data: {
    tab: 'notice',   // notice 通知 / chat 私信
    noticeUnread: 0,
    chatUnread: 0,
    // 通知
    notifications: [],
    notifPageNum: 1,
    notifHasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    // 私信会话
    conversations: [],
    convPageNum: 1,
    convHasMore: true,
    convLoading: false,
    convLoadMoreText: '上拉加载更多'
  },

  onShow() {
    if (!request.isLoggedIn()) {
      util.redirectToLogin();
      return;
    }
    this.loadBadges();
    if (this.data.tab === 'notice') {
      this.reloadNotifications();
    } else {
      this.reloadConvs();
    }
  },

  onReachBottom() {
    if (this.data.tab === 'notice') {
      this.loadNotifications(false);
    } else {
      this.loadConvs(false);
    }
  },

  onTabTap(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ tab });
    if (tab === 'chat') {
      this.reloadConvs();
    } else {
      this.reloadNotifications();
    }
  },

  // ---------- 未读角标 ----------
  loadBadges() {
    let total = 0;
    let left = 2;
    const done = () => getApp().globalData.unreadTotal = total;
    const one = (p) => {
      p.then((n) => { total += (n || 0); }).catch(() => {})
        .then(() => { if (--left <= 0) done(); });
    };
    one(request.get('/notification/unread-count', {}, { noToast: true }).then((n) => {
      this.setData({ noticeUnread: n || 0 });
      return n;
    }).catch(() => 0));
    one(request.get('/chat/unread-count', {}, { noToast: true }).then((n) => {
      this.setData({ chatUnread: n || 0 });
      return n;
    }).catch(() => 0));
  },

  // ---------- 通知 ----------
  reloadNotifications() {
    this.setData({ notifPageNum: 1, notifHasMore: true });
    this.loadNotifications(true);
  },

  loadNotifications(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.notifHasMore) return;
    this.setData({ loading: true });

    request.get('/notification/list', { pageNum: this.data.notifPageNum, pageSize: 20 })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((n) => Object.assign({}, n, {
          typeIcon: TYPE_ICONS[n.type] || 'bell',
          timeAgo: util.timeAgo(n.createTime)
        }));
        const notifications = reset ? list : this.data.notifications.concat(list);
        const notifHasMore = records.length >= 20;
        this.setData({
          notifications,
          notifHasMore,
          notifPageNum: this.data.notifPageNum + 1,
          loading: false,
          loadMoreText: notifHasMore ? '上拉加载更多' : (notifications.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  onNotifTap(e) {
    const ds = e.currentTarget.dataset;
    const id = ds.id;
    const read = Number(ds.read || 0);
    const type = Number(ds.type || 0);
    const related = ds.related || '';
    // 标记已读
    if (read === 0) {
      request.put('/notification/' + id + '/read', {}, { noToast: true })
        .then(() => {
          const notifications = this.data.notifications.map((n) => {
            if (n.id === id) return Object.assign({}, n, { isRead: 1 });
            return n;
          });
          this.setData({ notifications });
          this.loadBadges();
        })
        .catch(() => {});
    }
    // 点击跳转（CLAUDE.md 3.7 relatedId 语义）：type 1-6 → 订单详情；type 7 → 商品详情
    if (related) {
      if (type === 7) {
        wx.navigateTo({ url: '/pages/detail/detail?id=' + related });
      } else {
        wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + related });
      }
    }
  },

  readAll() {
    request.put('/notification/read-all')
      .then(() => {
        util.toast('已全部标记已读');
        const notifications = this.data.notifications.map((n) => Object.assign({}, n, { isRead: 1 }));
        this.setData({ notifications });
        this.loadBadges();
      })
      .catch((err) => util.toast(err.message));
  },

  // ---------- 私信会话 ----------
  reloadConvs() {
    this.setData({ convPageNum: 1, convHasMore: true });
    this.loadConvs(true);
  },

  loadConvs(reset) {
    if (this.data.convLoading) return;
    if (!reset && !this.data.convHasMore) return;
    this.setData({ convLoading: true });

    request.get('/chat/conversations', { pageNum: this.data.convPageNum, pageSize: 10 }, { noToast: true })
      .then((page) => {
        const records = (page && page.records) || [];
        const list = records.map((c) => Object.assign({}, c, {
          otherAvatar: util.resolveUrl(c.otherAvatar),
          avatarInitial: (c.otherNickname || '?').charAt(0),
          timeText: util.formatConvTime(c.lastTime)
        }));
        const conversations = reset ? list : this.data.conversations.concat(list);
        const convHasMore = records.length >= 10;
        this.setData({
          conversations,
          convHasMore,
          convPageNum: this.data.convPageNum + 1,
          convLoading: false,
          convLoadMoreText: convHasMore ? '上拉加载更多' : (conversations.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ convLoading: false, convLoadMoreText: '加载失败，请重试' }));
  },

  goChat(e) {
    wx.navigateTo({ url: '/pages/chat-detail/chat-detail?id=' + e.currentTarget.dataset.id });
  }
});
