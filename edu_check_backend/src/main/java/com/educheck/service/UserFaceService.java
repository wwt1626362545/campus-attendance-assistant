package com.educheck.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.educheck.entity.UserFace;

public interface UserFaceService extends IService<UserFace> {
    UserFace getUserFace(Long userId);
    // 用户是否已录入人脸
    boolean isFaceRegistered(Long userId);

    //获取 用户人脸图片的 base64 数据
    String getUserFaceBase64(Long userId);
}
