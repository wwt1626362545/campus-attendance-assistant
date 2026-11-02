import api from '../../../api/index';
const app = getApp();

Page({
  data: {
    list: [],
    loading: true,
    importing: false
  },

  async onLoad() {
    await this.loadData();
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const result = await api.getUnansweredQuestions({ page: 1, size: 100 });
      const records = result?.records || [];
      records.forEach(r => r.answer = '');
      this.setData({ list: records });
    } catch (err) {
      app.showToast('加载失败');
    } finally {
      this.setData({ loading: false });
    }
  },

  onAnswerInput(e) {
    const id = e.currentTarget.dataset.id;
    const val = e.detail.value;
    const list = this.data.list.map(item => {
      if (item.id === id) item.answer = val;
      return item;
    });
    this.setData({ list });
  },

  /** 一键导入知识库 */
  async importAll() {
    const list = this.data.list;
    const filled = list.filter(item => item.answer && item.answer.trim());
    if (filled.length === 0) {
      app.showToast('请先填写至少一条答案');
      return;
    }
    this.setData({ importing: true });
    try {
      let added = 0;
      for (const item of filled) {
        await api.addKnowledgeItem({
          question: (item.question || '').replace(/\|/g, '，').trim(),
          answer: item.answer.trim(),
          category: item.category || 'general',
          unansweredId: item.id  // 传入未命中ID，后端自动标记已处理
        });
        added++;
      }
      app.showToast('✅ 已导入 ' + added + ' 条', 'success');
      this.loadData();
    } catch (err) {
      app.showToast('导入失败：' + (err.message || ''));
    }
    this.setData({ importing: false });
  },

  /** 复制到剪贴板（TXT格式） */
  exportTxt() {
    const list = this.data.list;
    if (list.length === 0) { app.showToast('没有可导出的问题'); return; }
    let content = '# 可导入知识库的格式\n';
    content += '# 问题|答案|分类\n\n';
    list.forEach(item => {
      const q = (item.question || '').replace(/\|/g, '，').trim();
      if (!q) return;
      const answer = (item.answer || '').trim() || '待补充';
      content += q + '|' + answer + '|' + (item.category || 'general') + '\n';
    });
    wx.setClipboardData({
      data: content,
      success: () => {
        wx.showModal({
          title: '✅ 已复制到剪贴板',
          content: '请粘贴保存为 .txt 文件，然后在「知识库扩充」页面上传。\n\n或者直接点击「一键导入」立即导入已填答案。',
          showCancel: true,
          cancelText: '知道了',
          confirmText: '去知识库扩充',
          success: (res) => {
            if (res.confirm) {
              wx.navigateTo({ url: '/pages/kb-upload/kb-upload' });
            }
          }
        });
      },
      fail: () => { app.showToast('复制失败'); }
    });
  }
});
