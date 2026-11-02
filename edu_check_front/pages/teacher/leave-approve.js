/**
 * 教师端 - 请假审批
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    filterStatus: 'pending',
    page: 1,
    pageSize: 20,
    hasMore: false,
    leaves: [],
    stats: { total: 0, pending: 0, approved: 0, rejected: 0 },
    loading: true
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });

    try {
      let listResult;
      const statsData = await api.getTeacherLeaveStats();

      listResult = await api.getTeacherAllLeaves({
        page: this.data.page,
        size: this.data.pageSize,
        status: this.data.filterStatus !== 'all' ? this.data.filterStatus : undefined
      });

      this.setData({
        stats: {
          total: statsData?.total || 0,
          pending: statsData?.pending || 0,
          approved: statsData?.approved || 0,
          rejected: statsData?.rejected || 0
        },
        leaves: listResult?.records || [],
        hasMore: listResult?.records?.length >= this.data.pageSize,
        page: 1,
        loading: false
      });
    } catch (err) {
      console.warn('[请假审批] 加载失败:', err);
      app.showToast('加载请假数据失败');
      this.setData({ loading: false });
    }
  },

  switchFilter(e) {
    const status = e.currentTarget.dataset.status;
    if (status === this.data.filterStatus) return;
    this.setData({ filterStatus: status, page: 1, leaves: [] }, () => {
      this.loadData();
    });
  },

  approveLeave(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '审批确认',
      content: '确认批准该请假申请？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.approveLeave(id, { approved: true });
            app.showToast('已批准', 'success');
            this.loadData();
          } catch (err) {
            app.showToast(err.message || '操作失败');
          }
        }
      }
    });
  },

  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/leave/detail/detail?id=' + id });
  },

  rejectLeave(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '驳回请假',
      editable: true,
      placeholderText: '请输入驳回原因（可选）',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.approveLeave(id, {
              approved: false,
              rejectReason: res.content || '未说明原因'
            });
            app.showToast('已驳回', 'success');
            this.loadData();
          } catch (err) {
            app.showToast(err.message || '操作失败');
          }
        }
      }
    });
  }
});
