/**
 * 校园考勤助手 - 定位工具
 */
function getLocationWithAddress() {
  return new Promise((resolve, reject) => {
    wx.chooseLocation({
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude,
          address: res.address || ''
        });
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.includes('cancel')) {
          reject(new Error('已取消定位'));
        } else {
          reject(new Error('获取位置失败：' + (err.errMsg || '')));
        }
      }
    });
  });
}

function getRealLocation() {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: (err) => {
        reject(new Error('定位失败，请检查手机定位权限：' + (err.errMsg || '')));
      }
    });
  });
}

module.exports = { getLocationWithAddress, getRealLocation };
