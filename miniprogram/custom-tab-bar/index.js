// custom-tab-bar/index.js - 自定义底部导航（图标 + 居中发布按钮 + 消息角标）
Component({
  data: {
    selected: 0,
    unread: 0,
    list: [
      { pagePath: '/pages/index/index', text: '首页', icon: 'home' },
      { pagePath: '/pages/notifications/notifications', text: '消息', icon: 'message' },
      { pagePath: '/pages/publish/publish', text: '发布', icon: 'add', special: true },
      { pagePath: '/pages/profile/profile', text: '我的', icon: 'user' },
      { pagePath: '/pages/favorites/favorites', text: '收藏', icon: 'heart' }
    ]
  },

  methods: {
    onTap(e) {
      const idx = e.currentTarget.dataset.index;
      const item = this.data.list[idx];
      if (!item || idx === this.data.selected) return;
      wx.switchTab({ url: item.pagePath });
    },

    // 供 tab 页面 onShow 调用：同步选中态与角标
    sync(selected, unread) {
      const patch = { selected: selected };
      if (typeof unread === 'number') patch.unread = unread;
      this.setData(patch);
    }
  }
});
