/* ============================================================
  校园二手交易平台 — API 请求封装
  ============================================================ */

const API_BASE = '';

/**
 * 获取 JWT Token
 */
function getToken() {
  return localStorage.getItem('token');
}

/**
 * 获取当前用户信息
 */
function getCurrentUser() {
  const raw = localStorage.getItem('user');
  return raw ? JSON.parse(raw) : null;
}

/**
 * 保存登录态
 */
function setLogin(token, user) {
  localStorage.setItem('token', token);
  localStorage.setItem('user', JSON.stringify(user));
}

/**
 * 清除登录态
 */
function clearLogin() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

/**
 * 检查是否已登录
 */
function isLoggedIn() {
  return !!getToken();
}

/**
 * 显示 Toast 提示
 */
function showToast(msg, duration) {
  duration = duration || 2000;
  let el = document.querySelector('.toast-global');
  if (!el) {
    el = document.createElement('div');
    el.className = 'toast toast-global';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._timer);
  el._timer = setTimeout(function() { el.classList.remove('show'); }, duration);
}

/**
 * 核心请求方法
 * @param {string} method - HTTP 方法
 * @param {string} url - API 路径（不含 base）
 * @param {object} data - 请求体（GET/DELETE 时转为 query）
 * @param {object} options - 额外选项
 * @param {boolean} options.noAuth - 不需要认证
 * @param {boolean} options.noToast - 出错时不自动弹 toast
 * @returns {Promise<object>} 响应 data 字段
 */
async function request(method, url, data, options) {
  options = options || {};
  var fullUrl = API_BASE + url;

  var fetchOptions = {
    method: method,
    headers: {
      'Content-Type': 'application/json'
    }
  };

  // 添加认证头
  if (!options.noAuth) {
    var token = getToken();
    if (token) {
      fetchOptions.headers['Authorization'] = 'Bearer ' + token;
    }
  }

  // 处理请求体
  if (data) {
    if (method === 'GET' || method === 'DELETE') {
      // 转为 query 参数
      var params = new URLSearchParams();
      Object.keys(data).forEach(function(key) {
        if (data[key] !== undefined && data[key] !== null && data[key] !== '') {
          params.append(key, data[key]);
        }
      });
      var qs = params.toString();
      if (qs) fullUrl += '?' + qs;
    } else {
      fetchOptions.body = JSON.stringify(data);
    }
  }

  try {
    var res = await fetch(fullUrl, fetchOptions);
    var body = await res.json();

    if (body.code !== 0 && body.code !== 200) {
      // token 失效（本地有 token 但接口返回 401）：清除登录态并引导重新登录
      if (body.code === 401 && getToken()) {
        clearLogin();
        showToast('登录已过期，请重新登录');
        if (!window.location.pathname.endsWith('auth.html')) {
          // 走 redirectToLogin()，管理端（admin/ 子目录 + iframe）会覆盖为 ../auth.html
          setTimeout(function() { redirectToLogin(); }, 1200);
        }
      } else if (!options.noToast) {
        showToast(body.msg || '请求失败');
      }
      throw new ApiError(body.code || -1, body.msg || '请求失败');
    }

    return body.data;
  } catch (err) {
    if (err instanceof ApiError) throw err;
    // 网络错误
    if (!options.noToast) {
      showToast('网络异常，请检查连接');
    }
    throw new ApiError(-1, '网络异常');
  }
}

/**
 * 自定义 API 错误
 */
function ApiError(code, msg) {
  this.code = code;
  this.msg = msg;
}
ApiError.prototype = Object.create(Error.prototype);
ApiError.prototype.constructor = ApiError;

/**
 * 快捷方法
 */
function get(url, data, options) { return request('GET', url, data, options); }
function post(url, data, options) { return request('POST', url, data, options); }
function put(url, data, options) { return request('PUT', url, data, options); }
function del(url, data, options) { return request('DELETE', url, data, options); }

/**
 * 上传文件
 */
async function uploadFile(file, options) {
  options = options || {};
  var formData = new FormData();
  formData.append('file', file);

  var token = getToken();
  var headers = {};
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  try {
    var res = await fetch(API_BASE + '/file/upload', {
      method: 'POST',
      headers: headers,
      body: formData
    });
    var body = await res.json();
    if (body.code !== 0) {
      if (!options.noToast) showToast(body.msg || '上传失败');
      throw new ApiError(body.code, body.msg);
    }
    return body.data;
  } catch (err) {
    if (err instanceof ApiError) throw err;
    showToast('上传失败，请重试');
    throw new ApiError(-1, '上传失败');
  }
}

/**
 * 格式化时间（相对时间）
 */
function timeAgo(dateStr) {
  if (!dateStr) return '';
  var now = new Date();
  var date = new Date(dateStr);
  var diff = Math.floor((now - date) / 1000);
  if (diff < 60) return '刚刚';
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前';
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前';
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前';
  return date.toLocaleDateString('zh-CN');
}

/**
 * 格式化价格
 */
function formatPrice(price) {
  return '¥' + Number(price).toFixed(price % 1 === 0 ? 0 : 2);
}

/**
 * 商品成色映射
 */
var CONDITION_MAP = {
  1: '全新未拆',
  2: '几乎全新',
  3: '轻微使用痕迹',
  4: '明显使用痕迹',
  5: '故障/坏件',
  6: '故障/坏件'
};

/**
 * 商品状态映射
 */
var GOODS_STATUS_MAP = {
  0: '已下架',
  1: '在售',
  2: '已预订',
  3: '已售出'
};

/**
 * 订单状态映射
 */
var ORDER_STATUS_MAP = {
  1: '待交易',
  2: '已完成',
  3: '已取消',
  4: '超时取消'
};

/**
 * 跳转到登录页
 */
function redirectToLogin() {
  var current = window.location.pathname + window.location.search;
  window.location.href = 'auth.html?redirect=' + encodeURIComponent(current);
}

/**
 * 需要登录的守卫
 * 未登录时先提示，再延迟跳转登录页
 */
function requireAuth() {
  if (!isLoggedIn()) {
    showToast('请先登录');
    setTimeout(function() { redirectToLogin(); }, 600);
    return false;
  }
  return true;
}
