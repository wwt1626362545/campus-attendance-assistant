/**
 * 教师端 - 学生打卡详情
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    student: null,
    stats: null,
    todayClasses: [],
    todayDorm: null,
    recentClassRecords: [],
    recentDormRecords: [],
    loading: true
  },

  async onLoad(options) {
    const studentId = options.studentId;
    if (studentId) {
      await this.loadStudentDetail(studentId);
    }
  },

  async loadStudentDetail(studentId) {
    this.setData({ loading: true });
    try {
      const detail = await api.getStudentCheckinDetail(studentId);
      this.setData({
        student: detail.student,
        stats: detail.stats,
        todayClasses: detail.todayClasses || [],
        todayDorm: detail.todayDorm,
        recentClassRecords: detail.recentClassRecords || [],
        recentDormRecords: detail.recentDormRecords || []
      });
    } catch (err) {
      console.warn('[学生详情] 加载失败:', err);
      app.showToast('加载学生数据失败');
    } finally {
      this.setData({ loading: false });
    }
  }
});
