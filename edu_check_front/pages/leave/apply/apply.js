/**
 * 请假申请页面（支持日期+时间选择）
 */
import api from '../../../api/index';

const app = getApp();

Page({
  data: {
    form: {
      type: 'sick',
      startDate: '',
      startTime: '08:00',
      endDate: '',
      endTime: '18:00',
      reason: ''
    },
    typeOptions: [
      { value: 'sick', name: '病假' },
      { value: 'personal', name: '事假' },
      { value: 'official', name: '公假' },
      { value: 'annual', name: '年假' }
    ],
    typeIndex: 0,
    currentTypeName: '病假',
    submitting: false,
    minDate: ''
  },

  onLoad() {
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, '0');
    const d = String(today.getDate()).padStart(2, '0');
    const h = String(today.getHours()).padStart(2, '0');
    const mi = String(today.getMinutes()).padStart(2, '0');
    const dateStr = y + '-' + m + '-' + d;
    const timeStr = h + ':' + mi;
    this.setData({
      minDate: dateStr,
      'form.startDate': dateStr,
      'form.endDate': dateStr,
      'form.startTime': timeStr,
      'form.endTime': '18:00'
    });
  },

  onTypeChange(e) {
    const idx = parseInt(e.detail.value);
    const typeObj = this.data.typeOptions[idx];
    if (typeObj) {
      this.setData({
        'form.type': typeObj.value,
        typeIndex: idx,
        currentTypeName: typeObj.name
      });
    }
  },

  onStartDateChange(e) {
    this.setData({ 'form.startDate': e.detail.value });
  },

  onStartTimeChange(e) {
    this.setData({ 'form.startTime': e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ 'form.endDate': e.detail.value });
  },

  onEndTimeChange(e) {
    this.setData({ 'form.endTime': e.detail.value });
  },

  onReasonInput(e) {
    this.setData({ 'form.reason': e.detail.value });
  },

  /** 获取datetime字符串: YYYY-MM-DD HH:mm:00 */
  getDatetime(date, time) {
    const t = (time && time.length === 5) ? time + ':00' : '08:00:00';
    return date + ' ' + t;
  },

  async submit() {
    const { startDate, endDate, startTime, endTime, reason } = this.data.form;
    if (!reason.trim()) {
      app.showToast('请输入请假事由');
      return;
    }

    const startFull = this.getDatetime(startDate, startTime);
    const endFull = this.getDatetime(endDate, endTime);

    if (startFull > endFull) {
      app.showToast('结束时间不能早于开始时间');
      return;
    }

    this.setData({ submitting: true });
    try {
      // 后端期望 LocalDateTime 字段: startDate, endDate 传 datetime 字符串
      await api.applyLeave({
        type: this.data.form.type,
        startDate: startFull,
        endDate: endFull,
        reason: reason
      });
      app.showToast('提交成功', 'success');
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (err) {
      app.showToast(err.message || '提交失败');
    } finally {
      this.setData({ submitting: false });
    }
  }
});