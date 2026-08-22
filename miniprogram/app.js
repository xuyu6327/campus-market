// app.js - 全局逻辑
const request = require('./utils/request');

App({
  globalData: {
    user: null,          // 当前登录用户（UserInfoVO 结构）
    unreadTotal: 0,      // 消息角标总数（通知未读 + 私聊未读）
    editGoodsId: null    // 发布页编辑模式目标商品（tabBar switchTab 无法传参，用全局标志）
  },

  onLaunch() {
    // 恢复本地登录态，并拉取最新用户信息（头像/信用分可能变化）
    const token = request.getToken();
    const cached = request.getCurrentUser();
    if (cached) this.globalData.user = cached;
    if (token) {
      request.get('/user/info', {}, { noToast: true }).then((u) => {
        this.globalData.user = u;
        request.setLogin(token, u);
      }).catch(() => { /* 401 已由请求层统一处理 */ });
    }
  },

  // 刷新未读角标（通知 + 私聊），供 tab 页 onShow 调用；resolve 未读总数
  refreshUnread() {
    return new Promise((resolve) => {
      const app = this;
      let total = 0;
      const done = () => {
        app.globalData.unreadTotal = total;
        resolve(total);
      };
      if (!request.getToken()) { done(); return; }
      let left = 2;
      const one = (p) => {
        p.then((n) => { total += (n || 0); }).catch(() => {})
          .then(() => { if (--left <= 0) done(); });
      };
      one(request.get('/notification/unread-count', {}, { noToast: true }));
      one(request.get('/chat/unread-count', {}, { noToast: true }));
    });
  }
});
