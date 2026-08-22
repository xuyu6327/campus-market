// pages/report/report.js - 提交举报
const request = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    targetType: 2,   // 1用户 2商品
    targetId: null,
    targetLabel: '',
    reasons: util.REPORT_REASONS,
    reasonIndex: -1,
    description: '',
    submitting: false
  },

  onLoad(options) {
    const targetType = Number(options.targetType || 2);
    const targetId = options.targetId;
    if (!targetId) { util.toast('缺少举报对象'); return; }
    this.setData({
      targetType,
      targetId,
      targetLabel: targetType === 1 ? '用户（ID: ' + targetId + '）' : '商品（ID: ' + targetId + '）'
    });
  },

  onReasonTap(e) {
    this.setData({ reasonIndex: e.currentTarget.dataset.index });
  },

  onDescription(e) {
    this.setData({ description: e.detail.value });
  },

  submit() {
    if (this.data.reasonIndex < 0) { util.toast('请选择举报理由'); return; }
    if (this.data.submitting) return;
    this.setData({ submitting: true });

    request.post('/report', {
      targetType: this.data.targetType,
      targetId: parseInt(this.data.targetId, 10),
      reason: this.data.reasons[this.data.reasonIndex],
      description: this.data.description.trim() || ''
    })
      .then(() => {
        util.toast('举报已提交，等待处理');
        setTimeout(() => wx.navigateBack(), 800);
      })
      .catch((err) => {
        util.toast(err.message || '提交失败');
        this.setData({ submitting: false });
      });
  }
});
