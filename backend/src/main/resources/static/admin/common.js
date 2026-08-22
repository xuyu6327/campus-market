/* ============================================================
  管理后台公共脚本：权限守卫 / 工具函数 / 分页 / 弹窗
  依赖：../api.js（先加载）
  ============================================================ */

/* --- 覆盖 api.js 的登录跳转：admin 子目录 + iframe 场景 --- */
window.redirectToLogin = function () {
  var url = '../auth.html';
  if (window.top && window.top !== window) {
    window.top.location.href = url;
  } else {
    window.location.href = url;
  }
};

/* --- HTML 转义（所有动态内容拼 innerHTML 前必须调用） --- */
function escapeHtml(str) {
  if (str === undefined || str === null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/* --- 兼容数组/分页对象两种返回 --- */
function toList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.records)) return data.records;
  return [];
}

/* --- 图片解析：后端 images 可能是 JSON 字符串或数组 --- */
function parseImages(images) {
  if (!images) return [];
  if (Array.isArray(images)) return images;
  try {
    var arr = JSON.parse(images);
    return Array.isArray(arr) ? arr : [];
  } catch (e) {
    return [];
  }
}

/* --- 管理员权限守卫：未登录/非管理员重定向 --- */
async function requireAdmin() {
  if (!isLoggedIn()) {
    window.redirectToLogin();
    return false;
  }
  try {
    var info = await get('/user/info', null, { noToast: true });
    if (info.role !== 1) {
      showToast('无管理员权限');
      setTimeout(function () { window.location.href = '../index.html'; }, 800);
      return false;
    }
    window.__adminInfo = info;
    return true;
  } catch (e) {
    return false;
  }
}

/* --- 渲染管理员昵称（页面顶部） --- */
function renderAdminName(elId) {
  var el = document.getElementById(elId);
  if (el && window.__adminInfo) {
    el.textContent = window.__adminInfo.nickname || '管理员';
  }
}

/* --- iframe 内页面间跳转 --- */
function goPage(name) {
  if (window.top && window.top !== window && window.top.adminNav) {
    window.top.adminNav(name);
  } else {
    window.location.href = name;
  }
}

/* --- 确认弹窗 --- */
function confirmDialog(title, text) {
  return new Promise(function (resolve) {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay show';
    overlay.innerHTML =
      '<div class="modal-content">' +
      '<div class="modal-header"><span>' + escapeHtml(title) + '</span></div>' +
      '<p class="confirm-text">' + escapeHtml(text) + '</p>' +
      '<div class="confirm-actions">' +
      '<button class="btn btn-ghost" id="confirmCancel">取消</button>' +
      '<button class="btn btn-primary" id="confirmOk">确定</button>' +
      '</div></div>';
    document.body.appendChild(overlay);
    function close() { overlay.remove(); }
    overlay.querySelector('#confirmCancel').onclick = function () { close(); resolve(false); };
    overlay.querySelector('#confirmOk').onclick = function () { close(); resolve(true); };
    overlay.onclick = function (e) { if (e.target === overlay) { close(); resolve(false); } };
  });
}

/* --- 详情/信息弹窗（只读展示，标题 + 内容 HTML） --- */
function alertDialog(title, contentHtml) {
  var overlay = document.createElement('div');
  overlay.className = 'modal-overlay show';
  overlay.innerHTML =
    '<div class="modal-content admin-modal-lg">' +
    '<div class="modal-header"><span>' + escapeHtml(title) + '</span>' +
    '<button class="modal-close" id="alertClose">&times;</button></div>' +
    '<div class="modal-body">' + contentHtml + '</div></div>';
  document.body.appendChild(overlay);
  function close() { overlay.remove(); }
  overlay.querySelector('#alertClose').onclick = close;
  overlay.onclick = function (e) { if (e.target === overlay) close(); };
}

/* --- 分页渲染：page = {records,total,size,current,pages} --- */
function renderPagination(containerId, page, onChange) {
  var el = document.getElementById(containerId);
  if (!el) return;
  var total = page.total || 0;
  var pages = page.pages || 0;
  var current = page.current || 1;
  if (!total || !pages) {
    el.innerHTML = '<span class="page-total">共 0 条</span>';
    return;
  }
  var html = '<span class="page-total">共 ' + total + ' 条</span>';
  html += '<button class="page-btn" data-p="1"' + (current <= 1 ? ' disabled' : '') + '>首页</button>';
  html += '<button class="page-btn" data-p="' + (current - 1) + '"' + (current <= 1 ? ' disabled' : '') + '>上一页</button>';
  var start = Math.max(1, current - 3);
  var end = Math.min(pages, current + 3);
  if (start > 1) html += '<span class="page-ellipsis">…</span>';
  for (var i = start; i <= end; i++) {
    html += '<button class="page-btn' + (i === current ? ' active' : '') + '" data-p="' + i + '">' + i + '</button>';
  }
  if (end < pages) html += '<span class="page-ellipsis">…</span>';
  html += '<button class="page-btn" data-p="' + (current + 1) + '"' + (current >= pages ? ' disabled' : '') + '>下一页</button>';
  html += '<button class="page-btn" data-p="' + pages + '"' + (current >= pages ? ' disabled' : '') + '>末页</button>';
  el.innerHTML = html;
  el.querySelectorAll('.page-btn').forEach(function (btn) {
    btn.onclick = function () {
      if (btn.disabled) return;
      onChange(parseInt(btn.getAttribute('data-p'), 10));
    };
  });
}
