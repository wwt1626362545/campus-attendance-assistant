import api from '../../api/index';
const app = getApp();

Page({
  data: {
    userRole: '',
    roleFilter: 'all',
    keyword: '',
    users: [],
    loading: true,
    stats: {},
    showForm: false,
    editUser: null,
    form: { name: '', studentId: '', password: '123456', role: 'student', college: '' },
    submitting: false
  },

  onLoad() {
    const role = app.globalData.role || wx.getStorageSync('userRole') || '';
    this.setData({ userRole: role });
    this.loadData();
  },
  onShow() { this.loadData(); },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [userRes, statsRes] = await Promise.all([
        api.getAdminUsers({
          page: 1, size: 100,
          role: this.data.roleFilter !== 'all' ? this.data.roleFilter : undefined,
          keyword: this.data.keyword || undefined
        }),
        api.getAdminStats()
      ]);
      this.setData({ users: userRes?.records || [], stats: statsRes || {} });
    } catch (e) { console.warn(e); }
    this.setData({ loading: false });
  },

  onRoleFilter(e) {
    this.setData({ roleFilter: e.currentTarget.dataset.role }, () => this.loadData());
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value });
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.loadData(), 500);
  },

  showAddForm() {
    this.setData({ showForm: true, editUser: null, form: { name: '', studentId: '', password: '123456', role: 'student', college: '' } });
  },

  showEditForm(e) {
    const u = e.currentTarget.dataset.user;
    this.setData({
      showForm: true, editUser: u,
      form: { name: u.name, studentId: u.studentId, password: '', role: u.role, college: u.college || '' }
    });
  },

  hideForm() { this.setData({ showForm: false }); },

  onNameInput(e) { this.setData({ 'form.name': e.detail.value }); },
  onSidInput(e) { this.setData({ 'form.studentId': e.detail.value }); },
  onPwdInput(e) { this.setData({ 'form.password': e.detail.value }); },
  onCollegeInput(e) { this.setData({ 'form.college': e.detail.value }); },

  onRoleChange(e) { this.setData({ 'form.role': e.detail.value }); },

  async submit() {
    const { form, editUser } = this.data;
    if (!form.name || !form.studentId) { app.showToast('请填写完整信息'); return; }
    this.setData({ submitting: true });
    try {
      if (editUser) {
        await api.updateAdminUser(editUser.id, form);
        app.showToast('更新成功', 'success');
      } else {
        await api.addAdminUser(form);
        app.showToast('创建成功', 'success');
      }
      this.hideForm();
      this.loadData();
    } catch (e) { app.showToast(e.message || '操作失败'); }
    this.setData({ submitting: false });
  },

  async toggleStatus(e) {
    const id = e.currentTarget.dataset.id;
    try {
      await api.toggleUserStatus(id);
      app.showToast('操作成功', 'success');
      this.loadData();
    } catch (e) { app.showToast(e.message || '操作失败'); }
  },

  async deleteUser(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({ title: '确认删除', content: '确定删除该用户？不可恢复。', success: async (r) => {
      if (r.confirm) {
        try { await api.deleteAdminUser(id); app.showToast('已删除', 'success'); this.loadData(); }
        catch (e) { app.showToast(e.message || '删除失败'); }
      }
    }});
  }
});
