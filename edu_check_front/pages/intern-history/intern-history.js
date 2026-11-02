/**
 * 实习打卡记录详情页
 */
import api from '../../api/index';
const app = getApp();

Page({
  data: { list: [], summary: { total: 0, present: 0, late: 0, absent: 0 },
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
      const result = await api.getInternHistory({ page: this.data.page, size: this.data.pageSize });
      const records = result?.records || [];
      const combined = this.data.page === 1 ? records : [...this.data.list, ...records];
      const present = combined.filter(r => r.status === 'present').length;
      const late = combined.filter(r => r.status === 'late').length;
      this.setData({
        list: combined,
        summary: { total: combined.length, present, late, absent: combined.length - present - late },
        hasMore: records.length >= this.data.pageSize,
        loading: false
      });
    } catch (err) {
      console.warn('[实习记录] 加载失败:', err);
      this.setData({ loading: false });
    } finally { callback && callback(); }
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 });
    this.loadRecords();
  }
});
