/**
 * 教师端 - 学生管理（查看打卡情况）
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    students: [],
    allStudents: [],
    page: 1,
    pageSize: 50,
    hasMore: false,
    keyword: '',
    filter: '',
    loading: true,
    filterLabel: ''
  },

  filterLabels: {
    'class_checked': '已上课签到',
    'class_not': '未上课签到',
    'dorm_checked': '已查寝打卡',
    'dorm_not': '未查寝打卡'
  },

  async onLoad(options) {
    const filter = options.filter || '';
    this.setData({
      filter,
      filterLabel: this.filterLabels[filter] || ''
    });
    await this.loadStudents();
  },

  async loadStudents() {
    this.setData({ loading: true });
    try {
      const result = await api.getStudentsPage({
        page: this.data.page,
        size: this.data.pageSize,
        keyword: this.data.keyword || undefined
      });

      let list = result?.records || [];

      // 客户端筛选
      if (this.data.filter) {
        list = list.filter(s => {
          switch (this.data.filter) {
            case 'class_checked': return s.todayCheckin === 'present' || s.todayCheckin === 'late';
            case 'class_not': return s.todayCheckin === 'none' || s.todayCheckin === 'absent';
            case 'dorm_checked': return s.todayDorm === 'normal' || s.todayDorm === 'late';
            case 'dorm_not': return s.todayDorm === 'none';
            default: return true;
          }
        });
      }

      this.setData({
        allStudents: result?.records || [],
        students: list,
        hasMore: result?.records?.length >= this.data.pageSize
      });
    } catch (err) {
      console.warn('[学生管理] 加载失败:', err);
      app.showToast('加载学生数据失败');
    } finally {
      this.setData({ loading: false });
    }
  },

  clearFilter() {
    this.setData({ filter: '', filterLabel: '', students: this.data.allStudents });
  },

  onSearch(e) {
    const keyword = e.detail.value;
    this.setData({ keyword, page: 1 }, () => {
      this.loadStudents();
    });
  },

  viewStudent(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/teacher/student-detail?studentId=${id}`
    });
  },

  refresh() {
    this.setData({ page: 1 }, () => {
      this.loadStudents();
    });
  }
});

