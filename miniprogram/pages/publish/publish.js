// pages/publish/publish.js - 发布商品（编辑模式：编辑并申请上架 PUT /goods/{id}）
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    isEdit: false,
    editId: null,
    // 分类
    catOptions: [],      // [{id, name}]
    catNames: [],
    catIndex: -1,
    // 表单
    title: '',
    description: '',
    price: '',
    originalPrice: '',
    conditionIndex: -1,
    conditionNames: ['全新未拆', '几乎全新', '轻微使用痕迹', '明显使用痕迹', '故障/坏件'],
    images: [],          // 已上传的图片 URL（相对路径）
    tradeLocation: '',
    contactMethod: 1,    // 1手机 2QQ 3微信
    contactValue: '',
    contactPlaceholder: '请输入手机号',
    submitting: false
  },

  onShow() {
    if (!request.isLoggedIn()) {
      util.redirectToLogin();
    }
  },

  onLoad(options) {
    // 编辑模式来源：my-goods 通过 globalData.editGoodsId 传递（tabBar 页 switchTab 不支持 query）
    const editId = options.id || getApp().globalData.editGoodsId;
    getApp().globalData.editGoodsId = null;
    this.loadCategories();
    if (editId) {
      // 编辑模式：强制下架/待审核商品 → 编辑并申请上架
      this.setData({ isEdit: true, editId });
      wx.setNavigationBarTitle({ title: '编辑商品并申请上架' });
      this.loadGoodsDetail(editId);
    }
  },

  loadCategories() {
    request.get('/goods/category', {}, { noAuth: true, noToast: true })
      .then((list) => {
        const cats = (list || []).filter((c) => c.status === undefined || c.status === 1);
        // 扁平化：一级 + 子级（带缩进提示）
        const options = [];
        cats.forEach((c) => {
          if (!c.parentId) {
            options.push({ id: c.id, name: c.name });
            cats.forEach((sub) => {
              if (sub.parentId === c.id) {
                options.push({ id: sub.id, name: '　↳ ' + sub.name });
              }
            });
          }
        });
        this.setData({
          catOptions: options,
          catNames: options.map((o) => o.name)
        });
        // 编辑模式：定位当前分类
        if (this.data.isEdit && this.data.editCategoryId) {
          const idx = options.findIndex((o) => o.id === this.data.editCategoryId);
          if (idx >= 0) this.setData({ catIndex: idx });
        }
      })
      .catch(() => {});
  },

  loadGoodsDetail(id) {
    request.get('/goods/detail/' + id, {}, { noAuth: true, noToast: true })
      .then((g) => {
        const conditionIndex = Math.max(0, (g.goodsCondition || 1) - 1);
        const contactPlaceholder = g.contactMethod === 1 ? '手机号（如需修改请重新填写）' : (g.contactMethod === 2 ? '请输入QQ号' : '请输入微信号');
        this.setData({
          editCategoryId: g.categoryId,
          title: g.title || '',
          description: g.description || '',
          price: g.price ? String(g.price) : '',
          originalPrice: g.originalPrice ? String(g.originalPrice) : '',
          conditionIndex: conditionIndex < 5 ? conditionIndex : 4,
          images: (g.images || []).map((u) => util.resolveUrl(u)),
          tradeLocation: g.tradeLocation || '',
          contactMethod: g.contactMethod || 1,
          contactValue: g.contactMethod === 1 ? '' : (g.contactMethod === 2 ? (g.contactQq || '') : (g.contactWechat || '')),
          contactPlaceholder
        });
        // 分类定位（等分类加载完成）
        const idx = this.data.catOptions.findIndex((o) => o.id === g.categoryId);
        if (idx >= 0) this.setData({ catIndex: idx });
      })
      .catch((err) => util.toast(err.message || '加载商品信息失败'));
  },

  // ---------- 输入 ----------
  onCatChange(e) { this.setData({ catIndex: Number(e.detail.value) }); },
  onTitle(e) { this.setData({ title: e.detail.value }); },
  onDescription(e) { this.setData({ description: e.detail.value }); },
  onPrice(e) { this.setData({ price: e.detail.value }); },
  onOriginalPrice(e) { this.setData({ originalPrice: e.detail.value }); },
  onConditionChange(e) { this.setData({ conditionIndex: Number(e.detail.value) }); },
  onTradeLocation(e) { this.setData({ tradeLocation: e.detail.value }); },
  onContactValue(e) { this.setData({ contactValue: e.detail.value }); },

  onContactMethod(e) {
    const m = Number(e.currentTarget.dataset.method);
    const tips = { 1: '请输入手机号', 2: '请输入QQ号', 3: '请输入微信号' };
    this.setData({ contactMethod: m, contactValue: '', contactPlaceholder: tips[m] });
  },

  // ---------- 图片 ----------
  chooseImages() {
    const remain = 9 - this.data.images.length;
    if (remain <= 0) { util.toast('最多上传9张图片'); return; }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const files = res.tempFiles || [];
        this.uploadImages(files.map((f) => f.tempFilePath));
      }
    });
  },

  uploadImages(paths) {
    if (!paths || paths.length === 0) return;
    const path = paths.shift();
    wx.showLoading({ title: '上传中...', mask: true });
    request.upload(path)
      .then((url) => {
        // 上传返回相对路径，展示时转绝对
        this.setData({ images: this.data.images.concat(util.resolveUrl(url)) });
        this.uploadImages(paths);
      })
      .catch((err) => {
        util.toast(err.message || '上传失败');
        this.uploadImages(paths);
      })
      .finally(() => {
        if (paths.length === 0) wx.hideLoading();
      });
  },

  removeImage(e) {
    const idx = e.currentTarget.dataset.index;
    const images = this.data.images.slice();
    images.splice(idx, 1);
    this.setData({ images });
  },

  // ---------- 提交 ----------
  submit() {
    const { title, description, price, originalPrice, conditionIndex, images, tradeLocation, contactMethod, contactValue, isEdit } = this.data;

    if (this.data.catIndex < 0) { util.toast('请选择商品分类'); return; }
    if (title.trim().length < 4) { util.toast('标题至少4个字'); return; }
    const p = parseFloat(price);
    if (!price || isNaN(p) || p <= 0) { util.toast('请输入有效价格'); return; }
    if (p > 99999) { util.toast('价格不能超过99999'); return; }
    let op = null;
    if (originalPrice) {
      op = parseFloat(originalPrice);
      if (isNaN(op) || op <= 0 || op > 99999) { util.toast('原价无效'); return; }
    }
    if (conditionIndex < 0) { util.toast('请选择商品成色'); return; }
    const condition = conditionIndex + 1;
    // 成色图片数量校验（与后端一致）
    const minImages = condition === 3 ? 2 : (condition >= 4 ? 3 : 1);
    if (images.length < minImages) { util.toast('该成色至少需要' + minImages + '张图片'); return; }
    if (images.length === 0) { util.toast('请至少上传1张图片'); return; }
    const contactVal = contactValue.trim();
    if (!contactVal) { util.toast('请输入联系方式'); return; }
    if (contactMethod === 2 && !util.validators.qq(contactVal)) { util.toast('QQ号应为5-15位数字'); return; }
    if (contactMethod === 3 && !util.validators.wechat(contactVal)) { util.toast('微信号应为6-20位字母/数字/下划线'); return; }
    if (contactMethod === 1 && contactVal && !util.validators.phone(contactVal)) { util.toast('手机号格式不正确'); return; }

    // 存储的是绝对 URL，需转回相对路径提交（后端存相对路径）
    const relativeImages = images.map((u) => u.replace(request.BASE, ''));

    const payload = {
      title: title.trim(),
      description: description.trim() || undefined,
      categoryId: this.data.catOptions[this.data.catIndex].id,
      price: p,
      originalPrice: op,
      goodsCondition: condition,
      images: relativeImages,
      tradeLocation: tradeLocation.trim() || undefined,
      contactMethod: contactMethod,
      contactQq: contactMethod === 2 ? contactVal : undefined,
      contactWechat: contactMethod === 3 ? contactVal : undefined,
      contactPhone: contactMethod === 1 ? (contactVal || undefined) : undefined
    };

    this.setData({ submitting: true });
    const pReq = isEdit
      ? request.put('/goods/' + this.data.editId, payload)
      : request.post('/goods', payload);

    pReq.then((res) => {
      if (isEdit) {
        util.toast('已提交审核，请等待管理员审核');
        setTimeout(() => wx.navigateBack(), 800);
      } else {
        util.toast('发布成功！');
        const id = res;
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/detail/detail?id=' + id });
        }, 800);
      }
    }).catch((err) => {
      util.toast(err.message || (isEdit ? '提交失败' : '发布失败'));
      this.setData({ submitting: false });
    });
  }
});
