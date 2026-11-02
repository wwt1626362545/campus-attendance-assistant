/**
 * 首页逻辑 - 学生/教师双角色
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    userInfo: {},
    stats: { dormTotal: 0, classTotal: 0, internTotal: 0, streakDays: 0 },
    banners: [
      { src: '/images/banner_1.webp' },
      { src: '/images/banner_2.webp' },
      { src: '/images/banner_3.webp' }
    ],
    loading: true,
    role: 'student',
    connected: true
  },

  async onLoad() {
    this.initBanners();
    this.syncAppState();
  },

  async onShow() {
    this.syncAppState();
    await this.loadDashboardData();
  },

  /** 同步全局状态 */
  syncAppState() {
    const role = app.globalData.role || wx.getStorageSync('userRole') || 'student';
    this.setData({
      role,
      connected: app.globalData.connected
    });
  },

  /** 初始化轮播图 */
  initBanners() {
    api.getBanners().then(paths => {
      if (paths && paths.length > 0) {
        console.log("前端从后端获取轮播成功");
        this.setData({
          banners: paths.map(src => ({ src }))
        });
      }
    }).catch(() => {});
  },

  /** Unsplash图片加载失败时回退本地图片 */
  onBannerError(e) {
    const idx = e.currentTarget.dataset.index;
    const key = `banners[${idx}].src`;
    this.setData({ [key]: '/images/banner_3.png' });
  },

  /** 从后端API加载首页数据 */
  async loadDashboardData() {
    this.setData({ loading: true });
    const BASE = 'http://localhost:8081/api';

    try {
      // 补全身像URL
      if (app.globalData.userInfo && app.globalData.userInfo.avatar && !app.globalData.userInfo.avatar.startsWith('http')) {
        let n = app.globalData.userInfo.avatar;
        if (n.includes('name=')) n = n.substring(n.indexOf('name=') + 5); else n = n.substring(n.lastIndexOf('/') + 1);
        if (n.includes('?')) n = n.substring(0, n.indexOf('?'));
        app.globalData.userInfo.avatar = BASE + '/auth/avatar-file?name=' + encodeURIComponent(n);
      }
      if (app.globalData.userInfo?.name) {
        this.setData({
          userInfo: app.globalData.userInfo,
          stats: app.globalData.checkinStats || this.data.stats
        });
      }

      const overview = await api.getDashboardOverview();
      if (overview) {
        // 补全身像URL
        if (overview.userInfo && overview.userInfo.avatar && !overview.userInfo.avatar.startsWith('http')) {
          let name = overview.userInfo.avatar;
          if (name.includes('name=')) name = name.substring(name.indexOf('name=') + 5);
          else name = name.substring(name.lastIndexOf('/') + 1);
          if (name.includes('?')) name = name.substring(0, name.indexOf('?'));
          overview.userInfo.avatar = BASE + '/auth/avatar-file?name=' + encodeURIComponent(name);
        }
        this.setData({
          userInfo: overview.userInfo || this.data.userInfo,
          stats: overview.stats || this.data.stats
        });
        app.globalData.userInfo = overview.userInfo;
        app.globalData.checkinStats = overview.stats;
      }
    } catch (err) {
      console.warn('[首页] 加载数据失败:', err);
      this.setData({
        userInfo: app.globalData.userInfo || this.data.userInfo,
        stats: app.globalData.checkinStats || this.data.stats
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  // ==================== 学生端 ====================

  goDormHistory() {
    wx.navigateTo({ url: '/pages/dorm-history/dorm-history' });
  },

  goClassHistory() {
    wx.navigateTo({ url: '/pages/course-history/course-history' });
  },

  goInternHistory() {
    wx.navigateTo({ url: '/pages/intern-history/intern-history' });
  },

  goDormCheckin() {
    wx.navigateTo({ url: '/pages/dorm/dorm' });
  },

  goClassCheckin() {
    wx.navigateTo({ url: '/pages/course/course' });
  },

  goInternCheckin() {
    wx.navigateTo({ url: '/pages/intern/intern' });
  },

  // ==================== 教师端 ====================

  goTeacherPanel() {
    wx.navigateTo({ url: '/pages/teacher/teacher' });
  },

  goStudents() {
    wx.navigateTo({ url: '/pages/teacher/students' });
  },

  goLeaveApprove() {
    wx.navigateTo({ url: '/pages/teacher/leave-approve' });
  },

  goDynamicCode() {
    wx.navigateTo({ url: '/pages/teacher/dynamic-code' });
  }
});
