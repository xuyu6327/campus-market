// utils/request.js - API 请求封装（仿照 Web 版 api.js）
// 统一 {code, msg, data} 响应；401 清登录态跳登录页；上传封装 wx.uploadFile
const BASE = 'http://localhost:8080';

function getToken() {
  return wx.getStorageSync('token') || '';
}

function getCurrentUser() {
  return wx.getStorageSync('user') || null;
}

function setLogin(token, user) {
  wx.setStorageSync('token', token);
  wx.setStorageSync('user', user);
}

function clearLogin() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('user');
}

function isLoggedIn() {
  return !!getToken();
}

/**
 * 核心请求方法
 * @param {string} method - GET/POST/PUT/DELETE
 * @param {string} url - API 路径（不含 base）
 * @param {object} data - 请求参数（GET/DELETE 转为 query）
 * @param {object} options - {noAuth, noToast}
 * @returns {Promise<any>} 响应 data 字段
 */
function request(method, url, data, options) {
  options = options || {};
  return new Promise((resolve, reject) => {
    let fullUrl = BASE + url;

    // GET/DELETE 参数转为 query 字符串
    if (data && (method === 'GET' || method === 'DELETE')) {
      const params = [];
      Object.keys(data).forEach((key) => {
        const v = data[key];
        if (v !== undefined && v !== null && v !== '') {
          params.push(encodeURIComponent(key) + '=' + encodeURIComponent(v));
        }
      });
      if (params.length) {
        fullUrl += (fullUrl.indexOf('?') >= 0 ? '&' : '?') + params.join('&');
      }
    }

    const header = { 'Content-Type': 'application/json' };
    if (!options.noAuth && getToken()) {
      header['Authorization'] = 'Bearer ' + getToken();
    }

    wx.request({
      url: fullUrl,
      method: method,
      data: (method === 'GET' || method === 'DELETE') ? undefined : data,
      header: header,
      success(res) {
        const body = res.data || {};
        if (body.code !== 0 && body.code !== 200) {
          // token 失效：清除登录态并引导重新登录
          if (body.code === 401 && getToken()) {
            clearLogin();
            wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
            setTimeout(() => {
              wx.reLaunch({ url: '/pages/auth/auth' });
            }, 1200);
          } else if (!options.noToast) {
            wx.showToast({ title: body.msg || '请求失败', icon: 'none' });
          }
          reject(new Error(body.msg || '请求失败'));
          return;
        }
        resolve(body.data);
      },
      fail() {
        if (!options.noToast) {
          wx.showToast({ title: '网络异常，请检查连接', icon: 'none' });
        }
        reject(new Error('网络异常'));
      }
    });
  });
}

function get(url, data, options) { return request('GET', url, data, options); }
function post(url, data, options) { return request('POST', url, data, options); }
function put(url, data, options) { return request('PUT', url, data, options); }
function del(url, data, options) { return request('DELETE', url, data, options); }

/**
 * 上传文件（wx.uploadFile，multipart，复用 /file/upload）
 * @param {string} filePath - 本地临时文件路径（wx.chooseMedia 返回）
 * @returns {Promise<string>} 图片访问 URL（相对路径）
 */
function upload(filePath, options) {
  options = options || {};
  return new Promise((resolve, reject) => {
    const header = {};
    if (!options.noAuth && getToken()) {
      header['Authorization'] = 'Bearer ' + getToken();
    }
    wx.uploadFile({
      url: BASE + '/file/upload',
      filePath: filePath,
      name: 'file',
      header: header,
      success(res) {
        let body = null;
        try { body = JSON.parse(res.data); } catch (e) { body = null; }
        if (!body || body.code !== 0) {
          if (!options.noToast) {
            wx.showToast({ title: (body && body.msg) || '上传失败', icon: 'none' });
          }
          reject(new Error((body && body.msg) || '上传失败'));
          return;
        }
        resolve(body.data);
      },
      fail() {
        if (!options.noToast) {
          wx.showToast({ title: '上传失败，请重试', icon: 'none' });
        }
        reject(new Error('上传失败'));
      }
    });
  });
}

module.exports = {
  BASE,
  getToken,
  getCurrentUser,
  setLogin,
  clearLogin,
  isLoggedIn,
  request,
  get,
  post,
  put,
  del,
  upload
};
