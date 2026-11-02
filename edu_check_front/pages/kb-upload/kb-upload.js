import api from '../../api/index';
const app = getApp();

Page({
  data: {
    result: null,
    uploading: false,
    formatGuide: true
  },

  /** 选择并上传知识库文件 */
  chooseFile() {
    const that = this;
    wx.chooseMessageFile ? wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['txt', 'json', 'md'],
      success(res) {
        const file = res.tempFiles[0];
        if (file.size > 5 * 1024 * 1024) { app.showToast('文件不能超过5MB'); return; }
        that.doUpload(file.path);
      }
    }) : app.showToast('当前环境不支持文件选择');
  },

  async doUpload(filePath) {
    this.setData({ uploading: true, result: null });
    try {
      const res = await api.uploadKnowledgeFile(filePath);
      this.setData({ result: { success: true, msg: `✅ 导入成功！新增 ${res.added || 0} 条知识库内容` } });
    } catch (err) {
      this.setData({ result: { success: false, msg: '❌ ' + (err.message || '上传失败') } });
    }
    this.setData({ uploading: false });
  },

  toggleGuide() { this.setData({ formatGuide: !this.data.formatGuide }); }
});
