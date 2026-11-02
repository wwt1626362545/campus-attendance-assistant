/**
 * 意见反馈 - 学生可提交，教师可查看全部
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    role: 'student',
    form: {
      type: 'suggestion',
      content: '',
      contact: ''
    },
    submitting: false,
    canSubmit: false,
    list: [],
    loading: true,
    replyMap: {}
  },

  async onLoad() {
    const role = app.globalData.role || wx.getStorageSync('userRole') || 'student';
    this.setData({ role });
    await this.loadList();
  },

  async loadList() {
    this.setData({ loading: true });
    try {
      if (this.data.role === 'teacher') {
        const result = await api.getTeacherFeedback({ page: 1, size: 50 });
        this.setData({ list: result?.records || [] });
      } else {
        const result = await api.getMyFeedback({ page: 1, size: 50 });
        this.setData({ list: result?.records || [] });
      }
    } catch (err) {
      console.warn('[反馈] 加载失败:', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 选择反馈类型 */
  setType(e) {
    const type = e.currentTarget.dataset.type;
    this.setData({ 'form.type': type });
  },

  onContentInput(e) {
    const content = e.detail.value;
    this.setData({
      'form.content': content,
      canSubmit: content.trim().length > 0
    });
  },

  onContactInput(e) {
    this.setData({ 'form.contact': e.detail.value });
  },

  async submit() {
    const content = (this.data.form.content || '').trim();
    if (!content) {
      app.showToast('请输入反馈内容');
      return;
    }
    this.setData({ submitting: true });
    try {
      await api.submitFeedback({
        type: this.data.form.type,
        content: content,
        contact: this.data.form.contact
      });
      app.showToast('提交成功', 'success');
      this.setData({
        form: { type: 'suggestion', content: '', contact: '' },
        canSubmit: false
      });
      this.loadList();
    } catch (err) {
      app.showToast(err.message || '提交失败');
    } finally {
      this.setData({ submitting: false });
    }
  },

  /** 阻止事件冒泡 */
  noop() {},

  /** 教师输入回复 */
  onReplyInput(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ [`replyMap.${id}`]: e.detail.value });
  },

  /** 教师提交回复 */
  async submitReply(e) {
    const id = e.currentTarget.dataset.id;
    const reply = (this.data.replyMap[id] || '').trim();
    if (!reply) { app.showToast('请输入回复内容'); return; }
    try {
      await api.replyFeedback(id, { reply });
      app.showToast('回复成功', 'success');
      this.loadList();
    } catch (err) {
      app.showToast(err.message || '回复失败');
    }
  },

  /** 反馈类型展示名 */
  typeName(type) {
    const map = {
      suggestion: '建议',
      bug: '问题反馈',
      complaint: '投诉',
      other: '其他'
    };
    return map[type] || type;
  },

  /** 格式化时间 */
  formatTime(t) {
    if (!t) return '';
    const d = new Date(t);
    const pad = n => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
});
