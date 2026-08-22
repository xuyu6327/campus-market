// pages/admin/admin.js - 管理端入口：仪表盘（统计 + 宫格导航）
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    loading: true,
    stats: [],
    grid: [
      { key: 'users', icon: '👤', bg: 'green', text: '用户管理' },
      { key: 'goods', icon: '📦', bg: 'orange', text: '商品管理' },
      { key: 'orders', icon: '📋', bg: 'blue', text: '订单管理' },
      { key: 'reports', icon: '🚩', bg: 'red', text: '举报管理' },
      { key: 'categories', icon: '🗂️', bg: 'yellow', text: '分类管理' }
    ]
  },

  onShow() {
    // 管理员身份校验
    request.get('/user/info', {}, { noToast: true })
      .then((u) => {
        if (!u || u.role !== 1) {
          util.toast('无权限访问');
          setTimeout(() => wx.navigateBack(), 800);
          return;
        }
        this.loadDashboard();
      })
      .catch(() => {
        util.redirectToLogin();
      });
  },

  loadDashboard() {
    request.get('/admin/dashboard', {}, { noToast: true })
      .then((d) => {
        const s = d || {};
        // 11 项指标 → 前端展示 8 个核心卡片
        this.setData({
          loading: false,
          stats: [
            { value: s.totalUsers || 0, label: '总用户', icon: '👥', bg: 'green' },
            { value: s.todayNewUsers || 0, label: '今日新增用户', icon: '✨', bg: 'blue' },
            { value: s.totalGoods || 0, label: '总商品', icon: '📦', bg: 'orange' },
            { value: s.onSaleGoods || 0, label: '在售商品', icon: '🏷️', bg: 'yellow' },
            { value: s.totalOrders || 0, label: '总订单', icon: '📋', bg: 'purple' },
            { value: s.pendingOrders || 0, label: '待交易订单', icon: '⏳', bg: 'blue' },
            { value: s.totalTrades || 0, label: '累计成交', icon: '✅', bg: 'green' },
            { value: s.pendingReports || 0, label: '待处理举报', icon: '🚩', bg: 'red' }
          ]
        });
      })
      .catch(() => {
        this.setData({ loading: false });
        util.toast('仪表盘加载失败');
      });
  },

  goModule(e) {
    const key = e.currentTarget.dataset.key;
    const pages = {
      users: '/pages/admin/users/users',
      goods: '/pages/admin/goods/goods',
      orders: '/pages/admin/orders/orders',
      reports: '/pages/admin/reports/reports',
      categories: '/pages/admin/categories/categories'
    };
    wx.navigateTo({ url: pages[key] });
  }
});
