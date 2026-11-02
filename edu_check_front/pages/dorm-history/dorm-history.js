/**
 * 查寝打卡记录详情页
 */
import api from '../../api/index';
const app = getApp();

Page({
  data: { list: [], summary: { total: 0, normal: 0, late: 0, missed: 0 },
    page: 1, pageSize: 20, hasMore: true, loading: false },

  onLoad() { this.loadRecords(); },

  onPullDownRefresh() {
    this.setData({ page: 1, list: [], hasMore: true });
    this.loadRecords(() => wx.stopPullDownRefresh());
  },

  async loadRecords(callback) {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const result = await api.getDormHistory({ page: this.data.page, size: this.data.pageSize });
      const records = result?.records || [];
      const combined = this.data.page === 1 ? records : [...this.data.list, ...records];
      const normal = combined.filter(r => r.status === 'normal').length;
      const late = combined.filter(r => r.status === 'late').length;
      this.setData({
        list: combined,
        summary: { total: combined.length, normal, late, missed: combined.length - normal - late },
        hasMore: records.length >= this.data.pageSize,
        loading: false
      });
    } catch (err) {
      console.warn('[查寝记录] 加载失败:', err);
      this.setData({ loading: false });
    } finally { callback && callback(); }
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 });
    this.loadRecords();
  }
});
