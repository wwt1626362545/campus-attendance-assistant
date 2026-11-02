/**
 * 教师控制台 - 数据概览
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    summary: {
      totalStudents: 0,
      classChecked: 0,
      classNotChecked: 0,
      dormChecked: 0,
      dormNotChecked: 0
    },
    loading: true
  },

  async onLoad() {
    await this.loadSummary();
  },

  async loadSummary() {
    this.setData({ loading: true });
    try {
      const summary = await api.getTodayCheckinSummary();
      this.setData({ summary: summary || this.data.summary });
    } catch (err) {
      console.warn('[教师控制台] 加载数据失败:', err);
      app.showToast('加载数据失败');
    } finally {
      this.setData({ loading: false });
    }
  },

  goStudents(e) {
    const filter = e.currentTarget.dataset.filter || '';
    wx.navigateTo({ url: '/pages/teacher/students?filter=' + filter });
  },

  goLeaveApprove() {
    wx.navigateTo({ url: '/pages/teacher/leave-approve' });
  },

  goAnnouncePublish() {
    wx.navigateTo({ url: '/pages/teacher/announce-publish' });
  },

  goDynamicCode() {
    wx.navigateTo({ url: '/pages/teacher/dynamic-code' });
  }
});
