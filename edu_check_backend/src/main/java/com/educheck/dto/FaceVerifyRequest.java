package com.educheck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
// 用于文档提示
@Schema(description = "人脸验证请求")
public class FaceVerifyRequest {

    @Schema(description = "当前图片的base64数据")
    private String image;

    @Schema(description = "图片格式 比如 jpg")
    private String format = "jpg";
}