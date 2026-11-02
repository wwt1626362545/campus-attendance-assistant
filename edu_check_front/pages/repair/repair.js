import api from '../../api/index';
const app = getApp();

Page({
  data: {
    isTeacher: false,
    form: {
      dormBuilding: '',
      dormRoom: '',
      phone: '',
      category: 'other',
      description: ''
    },
    currentCategoryName: '其他',
    submitting: false,
    list: [],
    loading: true,
    tabType: 'my',
    replyMap: {},
    categoryOptions: [
      { value: 'plumbing', name: '水管龙头' },
      { value: 'electric', name: '电路照明' },
      { value: 'furniture', name: '家具损坏' },
      { value: 'door', name: '门窗锁具' },
      { value: 'network', name: '网络故障' },
      { value: 'other', name: '其他' }
    ]
  },

  onLoad() {
    const role = app.globalData.role || wx.getStorageSync('userRole') || '';
    const isTeacher = role === 'teacher' || role === 'admin';
    this.setData({ isTeacher, tabType: isTeacher ? 'my' : 'my', currentCategoryName: '其他' });
    this.loadList();
  },

  onShow() { this.loadList(); },

  getCatName(val) {
    const opt = this.data.categoryOptions.find(o => o.value === val);
    return opt ? opt.name : val;
  },

  formatTime(t) {
    if (!t) return '';
    const d = new Date(t);
    const pad = n => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  },

  async loadList() {
    this.setData({ loading: true });
    try {
      let records;
      if (this.data.tabType === 'all' && this.data.isTeacher) {
        const res = await api.getAllRepairs({ page: 1, size: 50 });
        records = res?.records || [];
      } else if (this.data.tabType === 'my' && this.data.isTeacher) {
        const res = await api.getAllRepairs({ page: 1, size: 50, status: 'pending' });
        records = res?.records || [];
      } else {
        const res = await api.getMyRepairs({ page: 1, size: 50 });
        records = res?.records || [];
      }
      records.forEach(r => { r.catName = this.getCatName(r.category); });
      this.setData({ list: records });
    } catch (e) { console.warn('[报修] 加载失败', e); }
    this.setData({ loading: false });
  },

  switchTab(e) {
    const tab = e ? e.currentTarget.dataset.tab : 'my';
    this.setData({ tabType: tab }, () => this.loadList());
  },

  onBuildingInput(e) { this.setData({ 'form.dormBuilding': e.detail.value }); },
  onRoomInput(e) { this.setData({ 'form.dormRoom': e.detail.value }); },
  onPhoneInput(e) { this.setData({ 'form.phone': e.detail.value }); },
  onDescInput(e) { this.setData({ 'form.description': e.detail.value }); },

  onReplyInput(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ [`replyMap.${id}`]: e.detail.value });
  },

  onCategoryChange(e) {
    const val = e.detail.value;
    const opt = this.data.categoryOptions.find(o => o.value === val);
    this.setData({ 'form.category': val, currentCategoryName: opt ? opt.name : '其他' });
  },

  async submit() {
    const { dormBuilding, dormRoom, description } = this.data.form;
    if (!dormBuilding || !dormRoom) { app.showToast('请填写宿舍信息'); return; }
    if (!description.trim()) { app.showToast('请描述问题'); return; }
    this.setData({ submitting: true });
    try {
      await api.submitRepair(this.data.form);
      app.showToast('提交成功', 'success');
      this.setData({ form: { dormBuilding: '', dormRoom: '', phone: '', category: 'other', description: '' }, currentCategoryName: '其他' });
      this.loadList();
    } catch (e) { app.showToast(e.message || '提交失败'); }
    this.setData({ submitting: false });
  },

  async handleRepair(e) {
    const id = e.currentTarget.dataset.id;
    const reply = this.data.replyMap[id] || '';
    wx.showModal({
      title: '确认处理',
      content: reply ? `回复内容：${reply}\n确认处理该报修？` : '确认已处理该报修？',
      success: async (r) => {
        if (r.confirm) {
          try {
            await api.handleRepair(id, { status: 'done', reply: reply || '已处理' });
            app.showToast('操作成功', 'success');
            this.loadList();
          } catch (e) { app.showToast(e.message || '操作失败'); }
        }
      }
    });
  }
});
