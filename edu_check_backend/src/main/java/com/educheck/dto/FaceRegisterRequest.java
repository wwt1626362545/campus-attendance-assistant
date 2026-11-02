package com.educheck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "人脸录入请求")
public class FaceRegisterRequest {
    @Schema(description = "人脸图片base64数据")
    private String image;
    @Schema(description = "图片格式")
    private String format="jpg";
}
