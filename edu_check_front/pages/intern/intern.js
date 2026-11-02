/**
 * 实习打卡 — 仿照查寝打卡，人脸验证独立步骤
 */
import api from '../../api/index';
import { getLocationWithAddress } from '../../utils/location';

const app = getApp();

Page({
  data: {
    internInfo: { company: '', position: '', address: '' },
    internshipId: null,
    // 定位
    locationChecked: false,
    locationAddr: '',
    // 人脸
    faceVerified: false,
    faceVerifying: false,
    facePhotoUrl: '',
    faceVerifyMsg: '',
    // 日志
    dailyLog: '',
    checkedIn: false,
    canCheckin: false,
    submitting: false,
    loading: true,
    history: []
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [stats, historyResult, internship] = await Promise.all([
        api.getInternStats(),
        api.getInternHistory({ page: 1, size: 5 }),
        api.getMyInternship()
      ]);
      const internInfo = {
        company: stats?.company || '',
        position: stats?.position || stats?.role || '',
        address: stats?.address || ''
      };
      const todayCheckin = historyResult?.records?.find(r => r.date === this.getToday());
      this.setData({
        internInfo,
        internshipId: internship?.id || null,
        history: historyResult?.records || [],
        checkedIn: !!todayCheckin,
        locationChecked: false,
        locationAddr: '',
        faceVerified: false,
        faceVerifying: false,
        facePhotoUrl: '',
        faceVerifyMsg: '',
        dailyLog: '',
        canCheckin: false,
        loading: false
      });
      if (todayCheckin) {
        this.setData({
          locationAddr: todayCheckin.locationAddr || '',
          dailyLog: todayCheckin.logContent || '',
          locationChecked: true,
          faceVerified: true
        });
      }
    } catch (err) {
      console.warn('[实习] 加载失败:', err);
      this.setData({ loading: false });
    }
  },

  getToday() {
    const d = new Date();
    return d.getFullYear() + '-' +
      String(d.getMonth() + 1).padStart(2, '0') + '-' +
      String(d.getDate()).padStart(2, '0');
  },

  // ==================== 定位 ====================

  async getLocation() {
    wx.showLoading({ title: '定位中...' });
    try {
      const result = await getLocationWithAddress();
      this.setData({
        locationChecked: true,
        locationAddr: result.address || result.latitude.toFixed(4) + ',' + result.longitude.toFixed(4)
      });
      this.checkCanCheckin();
      wx.hideLoading();
      app.showToast('定位成功', 'success');
    } catch (err) {
      wx.hideLoading();
      app.showToast(err.message || '定位失败');
    }
  },

  // ==================== 人脸验证 ====================

  async startFaceCheck() {
    if (this.data.faceVerifying) return;
    try {
      const faceStatus = await api.getFaceStatus();
      if (!faceStatus || !faceStatus.registered) {
        this.showFaceRegisterPrompt();
        return;
      }
    } catch (err) {
      this.showFaceRegisterPrompt();
      return;
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      camera: 'front',
      success: async (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        this.setData({
          facePhotoUrl: tempFilePath,
          faceVerifying: true,
          faceVerifyMsg: '人脸验证中...'
        });
        try {
          const fs = wx.getFileSystemManager();
          const base64 = fs.readFileSync(tempFilePath, 'base64');
          await api.verifyFace(base64);
          this.setData({
            faceVerified: true,
            faceVerifying: false,
            faceVerifyMsg: '验证通过 ✓'
          });
          this.checkCanCheckin();
          app.showToast('人脸验证通过', 'success');
        } catch (err) {
          this.setData({
            faceVerified: false,
            faceVerifying: false,
            faceVerifyMsg: err.message || '验证失败'
          });
          app.showToast(err.message || '人脸验证失败');
        }
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.includes('cancel')) {
          app.showToast('已取消拍照');
        } else {
          app.showToast('相机调用失败');
        }
      }
    });
  },

  showFaceRegisterPrompt() {
    wx.showModal({
      title: '提示',
      content: '请先进行人脸录入',
      confirmText: '去录入',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/face/face' });
        }
      }
    });
  },

  // ==================== 日志 ====================

  onLogInput(e) {
    this.setData({ dailyLog: e.detail.value });
    this.checkCanCheckin();
  },

  checkCanCheckin() {
    this.setData({
      canCheckin: this.data.locationChecked &&
                  this.data.faceVerified &&
                  this.data.dailyLog.trim().length > 0 &&
                  !this.data.checkedIn
    });
  },

  // ==================== 打卡 ====================

  async doInternCheckin() {
    if (this.data.submitting || this.data.checkedIn) return;
    this.setData({ submitting: true });
    try {
      await api.internCheckin({
        internshipId: this.data.internshipId,
        locationAddr: this.data.locationAddr,
        logContent: this.data.dailyLog,
        faceVerified: true
      });
      this.setData({ submitting: false, checkedIn: true, canCheckin: false });
      app.showToast('打卡成功', 'success');
      const historyResult = await api.getInternHistory({ page: 1, size: 5 });
      if (historyResult?.records) {
        this.setData({ history: historyResult.records });
      }
    } catch (err) {
      this.setData({ submitting: false });
      app.showToast(err.message || '打卡失败');
    }
  },

  viewAllHistory() {
    wx.navigateTo({ url: '/pages/intern-history/intern-history' });
  }
});