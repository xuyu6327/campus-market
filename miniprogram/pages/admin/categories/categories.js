// pages/admin/categories/categories.js - 分类管理
const request = require('../../../utils/request');
const util = require('../../../utils/util');

Page({
  data: {
    categories: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    loadMoreText: '上拉加载更多',
    showForm: false,
    editId: null,
    form: { name: '', sortOrder: '' },
    parentOptions: [],
    parentNames: [],
    parentIndex: 0,
    submitting: false
  },

  onLoad() {
    this.loadCategories(true);
  },

  onReachBottom() {
    this.loadCategories(false);
  },

  loadCategories(reset) {
    if (this.data.loading) return;
    if (!reset && !this.data.hasMore) return;
    this.setData({ loading: true });

    // pageSize 传 100 取全量（与 Web 端约定一致）
    request.get('/admin/category/list', { pageNum: this.data.pageNum, pageSize: 100 }, { noToast: true })
      .then((page) => {
        const records = (page && page.records) || [];
        const categories = reset ? records : this.data.categories.concat(records);
        const hasMore = records.length >= 100;
        this.setData({
          categories,
          hasMore,
          pageNum: this.data.pageNum + 1,
          loading: false,
          loadMoreText: hasMore ? '上拉加载更多' : (categories.length > 0 ? '— 没有更多了 —' : '')
        });
      })
      .catch(() => this.setData({ loading: false, loadMoreText: '加载失败，请重试' }));
  },

  // ---------- 表单 ----------
  openAdd() {
    this.setData({
      showForm: true,
      editId: null,
      form: { name: '', sortOrder: '' },
      parentIndex: 0,
      parentOptions: [{ id: 0, name: '一级分类' }].concat(
        this.data.categories.filter((c) => !c.parentId).map((c) => ({ id: c.id, name: c.name }))
      )
    });
    this.setParentNames();
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id;
    const c = this.data.categories.find((x) => x.id === id);
    if (!c) return;
    const parentOptions = [{ id: 0, name: '一级分类' }].concat(
      this.data.categories.filter((x) => !x.parentId && x.id !== id).map((x) => ({ id: x.id, name: x.name }))
    );
    const parentIndex = Math.max(0, parentOptions.findIndex((x) => x.id === (c.parentId || 0)));
    this.setData({
      showForm: true,
      editId: id,
      form: { name: c.name || '', sortOrder: c.sortOrder !== undefined ? String(c.sortOrder) : '' },
      parentOptions,
      parentIndex
    });
    this.setParentNames();
  },

  setParentNames() {
    this.setData({ parentNames: this.data.parentOptions.map((o) => o.name) });
  },

  closeForm() { this.setData({ showForm: false }); },
  noop() {},

  onFormName(e) { this.setData({ 'form.name': e.detail.value }); },
  onFormSort(e) { this.setData({ 'form.sortOrder': e.detail.value }); },

  onParentChange(e) {
    this.setData({ parentIndex: Number(e.detail.value) });
  },

  submitForm() {
    const name = this.data.form.name.trim();
    if (!name) { util.toast('请输入分类名称'); return; }
    const sortOrder = parseInt(this.data.form.sortOrder, 10) || 0;
    const parent = this.data.parentOptions[this.data.parentIndex];
    const payload = {
      name,
      parentId: parent && parent.id ? parent.id : 0,
      sortOrder
    };
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    const pReq = this.data.editId
      ? request.put('/admin/category/' + this.data.editId, payload)
      : request.post('/admin/category', payload);

    pReq.then(() => {
      util.toast('保存成功');
      this.setData({ showForm: false, submitting: false });
      this.setData({ pageNum: 1, hasMore: true });
      this.loadCategories(true);
    }).catch((err) => {
      util.toast(err.message || '保存失败');
      this.setData({ submitting: false });
    });
  },

  // ---------- 启停（注意：status 是 query 参数，非 body） ----------
  toggleStatus(e) {
    const id = e.currentTarget.dataset.id;
    const c = this.data.categories.find((x) => x.id === id);
    if (!c) return;
    const next = c.status === 1 ? 0 : 1;
    util.confirm(next === 0 ? '禁用分类' : '启用分类', next === 0 ? '禁用后该分类下商品将不可见' : '').then((ok) => {
      if (!ok) return;
      request.put('/admin/category/' + id + '/status?status=' + next)
        .then(() => {
          util.toast('操作成功');
          this.setData({ pageNum: 1, hasMore: true });
          this.loadCategories(true);
        })
        .catch((err) => util.toast(err.message));
    });
  }
});
