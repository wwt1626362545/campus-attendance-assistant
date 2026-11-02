/**
 * 公告页面 - 列表 + 收藏 + 教师置顶管理
 */
import api from '../../api/index';

const app = getApp();

Page({
  data: {
    currentCategory: 'all',
    searchKeyword: '',
    searchVisible: false,
    hasMore: true,
    page: 1,
    pageSize: 20,
    announceList: [],
    topAnnounce: null,
    loading: false,
    isTeacher: false,
    /** 收藏的公告ID集合 */
    favoriteIds: [],
    /** 只看收藏 */
    showFavOnly: false
  },

  onLoad() {
    this.initRole();
    this.loadFavorites();
    this.loadData();
  },

  onShow() {
    this.initRole();
    this.loadData();
  },

  initRole() {
    const role = app.globalData.role || wx.getStorageSync('userRole') || '';
    this.setData({ isTeacher: role === 'teacher' });
  },

  /** 从本地缓存加载收藏 */
  loadFavorites() {
    const fav = wx.getStorageSync('favAnnounceIds') || [];
    this.setData({ favoriteIds: fav });
  },

  /** 给列表项标记收藏状态 */
  markFav(records) {
    const ids = this.data.favoriteIds;
    (records || []).forEach(item => {
      item.isFav = ids.includes(item.id);
    });
    return records;
  },

  /** 保存收藏到本地缓存 */
  saveFavorites(ids) {
    wx.setStorageSync('favAnnounceIds', ids);
  },

  /** 切换收藏 */
  toggleFav(e) {
    const id = e.currentTarget.dataset.id;
    let ids = [...this.data.favoriteIds];
    if (ids.includes(id)) {
      ids = ids.filter(i => i !== id);
    } else {
      ids.unshift(id);
    }
    this.saveFavorites(ids);
    this.setData({ favoriteIds: ids });
    // 更新列表中该项的收藏状态
    const list = this.data.announceList.map(item => {
      if (item.id === id) item.isFav = ids.includes(id);
      return item;
    });
    this.setData({ announceList: list });
  },

  /** 切换只看收藏 */
  toggleFavOnly() {
    this.setData({ showFavOnly: !this.data.showFavOnly }, () => this.loadData());
  },

  /** 加载置顶 + 列表 */
  async loadData() {
    this.setData({ loading: true });

    if (this.data.currentCategory === 'all' && !this.data.showFavOnly) {
      try {
        const top = await api.getTopAnnouncement();
        this.setData({ topAnnounce: top || null });
      } catch (err) {
        console.warn('[公告] 置顶加载失败:', err);
      }
    } else {
      this.setData({ topAnnounce: null });
    }

    try {
      const listResult = await api.getAnnouncements({
        page: 1,
        size: this.data.pageSize,
        type: this.data.currentCategory !== 'all' ? this.data.currentCategory : undefined,
        keyword: this.data.searchKeyword || undefined
      });
      let records = listResult?.records || [];
      // 收藏模式下只保留已收藏的
      if (this.data.showFavOnly) {
        const favIds = this.data.favoriteIds;
        records = records.filter(r => favIds.includes(r.id));
      }
      this.markFav(records);
      this.setData({
        announceList: records,
        hasMore: records.length >= this.data.pageSize,
        page: 1
      });
    } catch (err) {
      console.warn('[公告] 列表加载失败:', err);
    }

    this.setData({ loading: false });
  },

  /** 搜索框展开/收起 */
  toggleSearch() {
    this.setData({ searchVisible: !this.data.searchVisible });
  },

  /** 清除搜索 */
  clearSearch() {
    this.setData({ searchKeyword: '', searchVisible: false, page: 1, announceList: [] }, () => {
      this.loadData();
    });
  },

  /** 搜索输入 */
  onSearch(e) {
    this.setData({ searchKeyword: e.detail.value });
    if (this.searchTimer) clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      this.setData({ page: 1, announceList: [] });
      this.loadData();
    }, 500);
  },

  /** 切换分类 */
  switchCategory(e) {
    const category = e.currentTarget.dataset.category;
    if (category === this.data.currentCategory) return;
    this.setData({ currentCategory: category, page: 1, announceList: [], showFavOnly: false });
    this.loadData();
  },

  /** 查看详情 */
  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/announcement/detail/detail?id=' + id });
  },

  /** 加载更多 */
  async loadMore() {
    if (!this.data.hasMore || this.data.loading) return;
    const nextPage = this.data.page + 1;
    this.setData({ loading: true });
    try {
      const result = await api.getAnnouncements({
        page: nextPage,
        size: this.data.pageSize,
        type: this.data.currentCategory !== 'all' ? this.data.currentCategory : undefined
      });
      const newRecords = result?.records || [];
      this.markFav(newRecords);
      const newList = [...this.data.announceList, ...newRecords];
      this.setData({
        announceList: newList,
        hasMore: newRecords.length >= this.data.pageSize,
        page: nextPage,
        loading: false
      });
    } catch (err) {
      console.warn('[公告] 加载更多失败:', err);
      this.setData({ loading: false });
    }
  },

  /** 教师发布 */
  goPublish() {
    wx.navigateTo({ url: '/pages/teacher/announce-publish' });
  }
});
