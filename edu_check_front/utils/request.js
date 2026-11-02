/**
 * 校园考勤助手 - HTTP 请求工具封装
 * 统一管理 wx.request、token、错误处理
 */

const BASE_URL = 'http://localhost:8081/api';
//const BASE_URL = 'http://192.168.31.23:8081/api';

class Request {
  constructor() {
    this.token = wx.getStorageSync('token') || '';
  }

  /** 设置token（登录后调用） */
  setToken(token) {
    this.token = token;
    wx.setStorageSync('token', token);
  }

  /** 清除token（退出登录） */
  clearToken() {
    this.token = '';
    wx.removeStorageSync('token');
  }

  /** 获取请求头 */
  getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (this.token) {
      headers['Authorization'] = this.token;
    }
    return headers;
  }

  /** 过滤掉参数中的 undefined/null/空字符串 */
  cleanParams(data) {
    if (!data || typeof data !== 'object') return data;
    const cleaned = {};
    for (const [key, value] of Object.entries(data)) {
      if (value !== undefined && value !== null && value !== '') {
        cleaned[key] = value;
      }
    }
    return cleaned;
  }

  /** 统一请求方法 */
  request(method, url, data = {}) {
    return new Promise((resolve, reject) => {
      wx.showNavigationBarLoading();

      const fullUrl = BASE_URL + url;
      const params = this.cleanParams(data);
      console.log('[HTTP]', method, fullUrl, params);
      wx.request({
        url: fullUrl,
        method,
        data: params,
        header: this.getHeaders(),
        timeout: 15000,
        success: (res) => {
          const { code, message, data: resData } = res.data || {};

          if (code === 200) {
            resolve(resData);
          } else if (code === 401) {
            // Token 过期，跳转登录
            this.clearToken();
            wx.redirectTo({ url: '/pages/login/login' });
            reject(new Error(message || '登录已过期'));
          } else {
            reject(new Error(message || '请求失败'));
          }
        },
        fail: (err) => {
          console.error(`[API错误] ${method} ${url}:`, err);
          reject(new Error('网络异常，请检查网络连接'));
        },
        complete: () => {
          wx.hideNavigationBarLoading();
        }
      });
    });
  }

  // HTTP 方法快捷封装
  get(url, params = {}) {
    return this.request('GET', url, params);
  }

  post(url, data = {}) {
    return this.request('POST', url, data);
  }

  put(url, data = {}) {
    return this.request('PUT', url, data);
  }

  del(url) {
    return this.request('DELETE', url);
  }

  upload(url, filePath) {
    return new Promise((resolve, reject) => {
      const fullUrl = BASE_URL + url + '?token=' + encodeURIComponent(this.token);
      wx.uploadFile({
        url: fullUrl,
        filePath: filePath,
        name: 'file',
        header: { 'Authorization': this.token },
        success: (res) => {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 200) resolve(data.data);
            else reject(new Error(data.message || '上传失败'));
          } catch { reject(new Error('上传失败')); }
        },
        fail: (err) => reject(new Error('网络异常'))
      });
    });
  }
}

const request = new Request();
export default request;
