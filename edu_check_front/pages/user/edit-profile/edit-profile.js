import api from '../../../api/index';

const app = getApp();
const BASE = 'http://localhost:8081/api';

Page({
  data: {
    avatarUrl: '',
    form: {
      name: '',
      phone: '',
      email: '',
      college: '',
      major: '',
      grade: ''
    },
    saving: false
  },

  async onLoad() {
    await this.loadProfile();
  },

  /** 头像路径转可访问URL */
  fullAvatarUrl(path) {
    if (!path) return '';
    if (path.startsWith('http')) return path;
    let name = path;
    if (name.includes('name=')) name = name.substring(name.indexOf('name=') + 5);
    else name = name.substring(name.lastIndexOf('/') + 1);
    if (name.includes('?')) name = name.substring(0, name.indexOf('?'));
    return BASE + '/auth/avatar-file?name=' + encodeURIComponent(name);
  },

  async loadProfile() {
    try {
      const profile = await api.getUserProfile();
      if (profile) {
        this.setData({
          avatarUrl: this.fullAvatarUrl(profile.avatar) || '',
          form: {
            name: profile.name || '',
            phone: profile.phone || '',
            email: profile.email || '',
            college: profile.college || '',
            major: profile.major || '',
            grade: profile.grade || ''
          }
        });
      }
    } catch (err) {
      app.showToast('加载个人信息失败');
    }
  },

  /** 选择并上传头像 */
  changeAvatar() {
    const that = this;
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        const filePath = res.tempFilePaths[0];
        // 先显示本地预览
        that.setData({ avatarUrl: filePath });
        // 上传到服务器
        wx.showLoading({ title: '上传中...' });
        api.uploadAvatar(filePath).then(result => {
          wx.hideLoading();
          if (result && result.avatar) {
            that.setData({ avatarUrl: that.fullAvatarUrl(result.avatar) });
            app.showToast('头像已更新', 'success');
          }
        }).catch(err => {
          wx.hideLoading();
          console.error('[头像] 上传失败:', err);
          app.showToast(err.message || '上传失败');
          // 恢复原有头像
          that.loadProfile();
        });
      }
    });
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({ [`form.${field}`]: value });
  },

  async save() {
    const { name } = this.data.form;
    if (!name.trim()) {
      app.showToast('姓名不能为空');
      return;
    }
    this.setData({ saving: true });
    try {
      await api.updateUserProfile(this.data.form);
      await app.refreshUserData();
      app.showToast('保存成功', 'success');
      wx.navigateBack();
    } catch (err) {
      app.showToast(err.message || '保存失败');
    } finally {
      this.setData({ saving: false });
    }
  }
});
