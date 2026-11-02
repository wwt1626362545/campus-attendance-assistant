/**
 * 我的课程表 - 按天显示课程
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    weekSchedule: [],      // 所有课程原始数据
    scheduleList: [],      // 当前选中天的课程
    weekdays: [],          // 星期标签 [{index, name, date, isToday, isSelected}]
    selectedDay: 0,        // 当前选中的星期索引 0=周一
    weekLabel: '',         // 周标签
    weekRange: '',         // 日期范围
    today: '',             // 今天星期几
    loading: true
  },

  onLoad() {
    const weekMap = ['日', '一', '二', '三', '四', '五', '六'];
    const today = '星期' + weekMap[new Date().getDay()];
    this.setData({ today });
    this.initDates();
    this.loadSchedule();
  },

  /** 初始化本周日期 */
  initDates() {
    const now = new Date();
    const dayOfWeek = now.getDay(); // 0=周日
    const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const monday = new Date(now);
    monday.setDate(now.getDate() + mondayOffset);

    const weekNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    const weekdays = [];
    const todayDayIndex = dayOfWeek === 0 ? 6 : dayOfWeek - 1;

    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      const m = d.getMonth() + 1;
      const day = d.getDate();
      weekdays.push({
        index: i,
        name: weekNames[i],
        date: m + '/' + day,
        isToday: i === todayDayIndex,
        isSelected: i === todayDayIndex
      });
    }

    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    const range = (monday.getMonth()+1) + '/' + monday.getDate() + ' - ' + (sunday.getMonth()+1) + '/' + sunday.getDate();

    this.setData({
      weekdays,
      selectedDay: todayDayIndex >= 0 ? todayDayIndex : 0,
      weekLabel: monday.getFullYear() + '年' + (monday.getMonth()+1) + '月',
      weekRange: range
    });
  },

  /** 加载课程数据 */
  async loadSchedule() {
    this.setData({ loading: true });
    try {
      const schedule = await api.getCourseSchedule();
      this.setData({
        weekSchedule: schedule || [],
        loading: false
      });
      this.filterByDay();
    } catch (err) {
      console.warn('[课程表] 加载失败:', err);
      this.setData({ loading: false });
    }
  },

  /** 按选定星期筛选课程 */
  selectDay(e) {
    const day = parseInt(e.currentTarget.dataset.day);
    const idx = this.data.weekdays.findIndex(w => w.index === day);
    if (idx < 0) return;

    const weekdays = this.data.weekdays.map((w, i) => ({
      ...w,
      isSelected: i === idx
    }));

    this.setData({ selectedDay: day, weekdays });
    this.filterByDay();
  },

  filterByDay() {
    const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    const targetDay = dayNames[this.data.selectedDay];
    const list = this.data.weekSchedule
      .filter(c => c.weekDay === targetDay)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
    this.setData({ scheduleList: list });
  },

  /** 上一周 */
  prevWeek() {
    // 暂简化：数据来自数据库所有课程，周切换暂无意义
    app.showToast('当前显示全部课程');
  },

  /** 下一周 */
  nextWeek() {
    app.showToast('当前显示全部课程');
  }
});
