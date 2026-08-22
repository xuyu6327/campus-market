// utils/util.js - 通用工具：状态映射 / 格式化 / 校验 / 相对URL转绝对
const request = require('./request');

// ---------- 状态映射 ----------
const CONDITION_MAP = {
  1: '全新未拆',
  2: '几乎全新',
  3: '轻微使用痕迹',
  4: '明显使用痕迹',
  5: '故障/坏件',
  6: '故障/坏件'
};

const GOODS_STATUS_MAP = {
  0: '已下架',
  1: '在售',
  2: '已预订',
  3: '已售出',
  4: '待审核'
};

const ORDER_STATUS_MAP = {
  0: '待交易',
  1: '已完成',
  2: '买家取消',
  3: '卖家取消',
  4: '超时取消'
};

const REPORT_REASONS = ['虚假商品', '描述不符', '违禁品', '广告骚扰', '联系方式违规', '其他违规'];

// ---------- 格式化 ----------
// 相对时间（列表用）
function timeAgo(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr.replace(/-/g, '/'));
  if (isNaN(date.getTime())) return '';
  const diff = Math.floor((Date.now() - date.getTime()) / 1000);
  if (diff < 60) return '刚刚';
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前';
  const m = date.getMonth() + 1;
  const d = date.getDate();
  return m + '月' + d + '日';
}

// 完整时间（订单详情等）
function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr.replace(/-/g, '/'));
  if (isNaN(d.getTime())) return '';
  const p = (n) => (n < 10 ? '0' + n : '' + n);
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

// 聊天消息时间：今天显示 HH:mm，昨天显示"昨天 HH:mm"，更早显示"M月d日 HH:mm"
function formatChatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr.replace(/-/g, '/'));
  if (isNaN(d.getTime())) return '';
  const now = new Date();
  const p = (n) => (n < 10 ? '0' + n : '' + n);
  const hm = p(d.getHours()) + ':' + p(d.getMinutes());
  const sameDay = d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
  if (sameDay) return hm;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday = d.getFullYear() === yesterday.getFullYear() && d.getMonth() === yesterday.getMonth() && d.getDate() === yesterday.getDate();
  if (isYesterday) return '昨天 ' + hm;
  return (d.getMonth() + 1) + '月' + d.getDate() + '日 ' + hm;
}

// 会话列表时间：今天 HH:mm，昨天"昨天"，更早"M月d日"
function formatConvTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr.replace(/-/g, '/'));
  if (isNaN(d.getTime())) return '';
  const now = new Date();
  const p = (n) => (n < 10 ? '0' + n : '' + n);
  if (d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()) {
    return p(d.getHours()) + ':' + p(d.getMinutes());
  }
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (d.getFullYear() === yesterday.getFullYear() && d.getMonth() === yesterday.getMonth() && d.getDate() === yesterday.getDate()) {
    return '昨天';
  }
  return (d.getMonth() + 1) + '月' + d.getDate() + '日';
}

// 价格格式化：整数不带小数，非整数保留两位
function formatPrice(price) {
  const n = Number(price || 0);
  return n % 1 === 0 ? n.toFixed(0) : n.toFixed(2);
}

// ---------- 图片 URL 处理（后端返回相对路径 /uploads/xxx） ----------
function resolveUrl(u) {
  if (!u) return '';
  if (/^https?:\/\//i.test(u)) return u;
  if (u.indexOf('/') === 0) return request.BASE + u;
  return request.BASE + '/' + u;
}

// 解析 images JSON 字符串（AdminGoodsVO 等场景），兼容数组
function parseImages(images) {
  if (!images) return [];
  if (Array.isArray(images)) return images;
  try {
    const arr = JSON.parse(images);
    return Array.isArray(arr) ? arr : [];
  } catch (e) {
    return [];
  }
}

// ---------- 校验 ----------
const validators = {
  phone: (v) => /^1[3-9]\d{9}$/.test(v),
  qq: (v) => /^\d{5,15}$/.test(v),
  wechat: (v) => /^[a-zA-Z0-9_-]{6,20}$/.test(v)
};

// ---------- 提示 / 确认 ----------
function toast(title) {
  wx.showToast({ title: title, icon: 'none' });
}

// wx.showModal 封装为 Promise，返回 true=确认
function confirm(title, content) {
  return new Promise((resolve) => {
    wx.showModal({
      title: title || '提示',
      content: content || '',
      success(res) { resolve(!!res.confirm); },
      fail() { resolve(false); }
    });
  });
}

// ---------- 登录守卫 ----------
function requireAuth() {
  if (!request.isLoggedIn()) {
    toast('请先登录');
    setTimeout(() => {
      wx.reLaunch({ url: '/pages/auth/auth' });
    }, 600);
    return false;
  }
  return true;
}

// 未登录时跳登录页（不提示）
function redirectToLogin() {
  wx.reLaunch({ url: '/pages/auth/auth' });
}

// 转义（小程序 WXML {{}} 天然转义，此处仅兜底）
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

module.exports = {
  CONDITION_MAP,
  GOODS_STATUS_MAP,
  ORDER_STATUS_MAP,
  REPORT_REASONS,
  timeAgo,
  formatDateTime,
  formatChatTime,
  formatConvTime,
  formatPrice,
  resolveUrl,
  parseImages,
  validators,
  toast,
  confirm,
  requireAuth,
  redirectToLogin,
  escapeHtml
};
