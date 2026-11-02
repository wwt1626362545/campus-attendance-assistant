package com.educheck.controller;

import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.FaceRegisterRequest;
import com.educheck.dto.FaceVerifyRequest;
import com.educheck.entity.UserFace;
import com.educheck.service.BaiduCloudFaceService;
import com.educheck.service.UserFaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
@Tag(name="人脸管理",description = "人脸录入与验证接口")
public class FaceController {
    private final UserFaceService userFaceService;
    private final TokenContextHolder tokenContextHolder;
    // 定义一个位置 存储前端传递过来的图片
    @Value("${face.upload-dir:uploads/face}")
    private String faceUploadDir;
    // 定义百度 service
    private final BaiduCloudFaceService baiduCloudFaceService;
    // 编写前端需要的人脸认证接口
    // 编写前端需要的人脸认证接口
    @PostMapping("/verify")
    @Operation(summary = "人脸验证")
    public Result<Map<String,Object>> verify(
            @RequestBody FaceVerifyRequest request
    ){
        Long userId = tokenContextHolder.requireCurrentUserId();
        if(request.getImage()==null || request.getImage().isEmpty()){
            return Result.error("人脸图片不能为空");
        }
        if(!userFaceService.isFaceRegistered(userId)){
            return Result.error("请先录入人脸");
        }

        // 获取已录入人脸图片 base64 数据
        String rBase64 = userFaceService.getUserFaceBase64(userId);
        if(rBase64 == null){
            return Result.error("已录入的人脸图片不存在，请重新录入");
        }

        // 调用百度AI进行人脸比对
        BaiduCloudFaceService.FaceMatchResult result =
                baiduCloudFaceService.compare(request.getImage(), rBase64);

        // 如果调用百度AI失败，返回百度AI返回的错误信息
        if(!result.success()){
            return Result.error(result.errorMsg());
        }

        // 百度正常返回结果，组装数据
        Map<String,Object> data = new HashMap<>();
        // 将百度返回的相似度评分放进去
        data.put("score", result.score());
        // 告诉前端是否识别通过
        data.put("passed", result.passed());
        // 相似度 80分以上 表示通过
        data.put("threshold", 80);

        if(result.passed()){
            return Result.success("验证通过", data);
        }else {
            return Result.error(400, "人脸检测失败，相似度不足");
        }
    }
    // 编写人脸录入的接口
    @PostMapping("/register")
    @Operation(summary = "人脸录入")
    public Result<Map<String,Object>> register(
            // @RequestBody 定义请求的参数类型
            @RequestBody FaceRegisterRequest request
    ){
        Long userId=tokenContextHolder.requireCurrentUserId();
        if(request.getImage()==null || request.getImage().isEmpty()){
            return Result.error("人脸图片不能为空");
        }
        try {
            // 确保文件路径存在
            Path uploadDir= Paths.get(faceUploadDir);
            // 如果文件目录不存在 创建这个目录
            if(!Files.exists(uploadDir)){
                Files.createDirectories(uploadDir);
            }
            // 将人脸图片保存到服务器
            // 确定图片保存 后缀
            String format=request.getFormat()!=null?request.getFormat():"jpg";
            // 拼接 得到文件名字+后缀
            // uuid 唯一字符串 时间戳+随机数
            String fileName="face_"+userId+"_"+
                    UUID.randomUUID().toString().substring(0,8)+"."+format;
            // 拼接得到文件的完整路径
            Path filePath=uploadDir.resolve(fileName);
            // 解码base64 图片字符串 转为二进制字节数组
            byte[] imagesBytes= Base64.getDecoder().decode(request.getImage());
            Files.write(filePath,imagesBytes);
// 查询该用户是否 录入过人脸信息
            UserFace existing=userFaceService.getUserFace(userId);
// 如果用户已经录入人脸 执行什么操作？ 用找到记录并更新
            if(existing!=null){
                // 删除已有的该用户图片
                try {
                    // 拿到旧图片的路径
                    Path oldPath=Paths.get(faceUploadDir,existing.getFaceUrl());
                    Files.deleteIfExists(oldPath);
                }catch (IOException e){
                    log.warn("删除旧图片失败: {}",e.getMessage());
                }
                // 将新图片的路径存进去
                existing.setFaceUrl(fileName);
                existing.setVersion(existing.getVersion()!=null?existing.getVersion()+1:2);
                // 表示用户录入过人脸
                existing.setRegistered(1);
                // 修改记录更新时间
                existing.setUpdatedAt(LocalDateTime.now());
// 通过mybatisPlus 对数据库对应记录进行更新操作
                userFaceService.updateById(existing);
                log.info("人脸信息更新成功");
            }else{
                // 如果用户没有录入人脸 执行什么操作？ 用户人脸表新增记录
                UserFace userFace=new UserFace();
// 设置插入 用户人脸表需要的数据
// userId.intValue() 将 Long类型数据 转成 Integer
                userFace.setUserId(userId.intValue());
                userFace.setFaceUrl(fileName);
                userFace.setRegistered(1);
                userFace.setVersion(1);
// 将用户人脸记录 插入到 对应表里面
                userFaceService.save(userFace);
                log.info("人脸信息录入成功");
            }
            Map<String,Object> data=new HashMap<>();
            data.put("registered",true);
            data.put("message",existing!=null?"人脸更新成功":"人脸录入成功");
            return Result.success(data);
        } catch (IOException e){
            log.error("保存人脸图片失败:userId={}",userId,e);
            return Result.error("人脸图片保存失败");
        }

    }
    @GetMapping("/status")
    @Operation(summary = "获取人脸录入状态")
    public Result<Map<String, Object>> status() {
        Long userId=tokenContextHolder.requireCurrentUserId();
        UserFace userFace=userFaceService.getUserFace(userId);
        // 定义一个变量存放返回结果
        Map<String,Object> data=new HashMap<>();
// 存入用户录入状态
        data.put("registered",userFace!=null);
// 有这个人的信息 把其他信息也放入
        if(userFace!=null){
            data.put("version",userFace.getVersion());
            data.put("registeredAt",userFace.getCreatedAt());
            data.put("updatedAt",userFace.getUpdatedAt());
        }
// 将打包好的数据返回给前端
        return Result.success(data);

    }

}
