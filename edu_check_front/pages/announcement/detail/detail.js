/**
 * 公告详情
 */
import api from '../../../api/index';

const app = getApp();

Page({
  data: {
    detail: null,
    loading: true
  },

  async onLoad(options) {
    const id = options.id;
    if (id) {
      await this.loadDetail(id);
    }
  },

  async loadDetail(id) {
    this.setData({ loading: true });
    try {
      const d = await api.getAnnouncementDetail(id);
      this.setData({ detail: d, loading: false });
    } catch (err) {
      app.showToast(err.message || '加载失败');
    } finally {
      this.setData({ loading: false });
    }
  }
});
