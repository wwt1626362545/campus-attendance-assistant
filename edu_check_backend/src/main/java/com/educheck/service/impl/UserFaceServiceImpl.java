package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.UserFace;
import com.educheck.mapper.UserFaceMapper;
import com.educheck.service.UserFaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
@Service
public class UserFaceServiceImpl
        extends ServiceImpl<UserFaceMapper, UserFace>
        implements UserFaceService {
    @Value("${face.upload-dir:uploads/face}")
    private String faceUploadDir;

    @Override
    public boolean isFaceRegistered(Long userId) {
        try {
            return lambdaQuery()
                    // 查看当前用户
                    .eq(UserFace::getUserId, userId)
                    // 已注册过
                    .eq(UserFace::getRegistered, 1)
                    // 的数据条数 >0 进行判断
                    .count()>0;
        } catch (DataAccessException e) {
            log.warn("检查用户人脸注册状态失败:{}",e.getMessage());
            return false;
        }
    }




    public String getUserFaceBase64(Long userId) {

        // 根据用户拿到用户的信息
        UserFace userFace = getUserFace(userId);
        // 检测 用户人脸 和 人脸链接是否 存在
        if(userFace==null || userFace.getFaceUrl()==null){
            return null;
        }
        try {


            Path filePath= Paths.get(faceUploadDir,userFace.getFaceUrl());
            if(!Files.exists(filePath)){
                log.warn("用户人脸图片不存在:{}",filePath);
                return null;
            }
            // 读取数据 转成 base64 格式
            byte[] fileBytes=Files.readAllBytes(filePath);
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            log.error("读取用户人脸图片失败:userId={}",userId,e);
            return null;
        }
    }

    @Override
    public UserFace getUserFace(Long userId) {
        try {
            // 返回从数据库拿到的数据
            // 构建一个 mybatisplus 查询
            return lambdaQuery()
                    // eq 相等 用用户id 进行相等查询
                    .eq(UserFace::getUserId,userId)
                    // 查询 注册状态为 1（已经录入过人脸）
                    .eq(UserFace::getRegistered, 1)
                    // 从返回的结果拿一条信息
                    // 返回多条会报错
                    .one();
        } catch (DataAccessException e){
            log.warn("查询人脸信息失败:{}",e.getMessage());
            return null;
        }
    }
}