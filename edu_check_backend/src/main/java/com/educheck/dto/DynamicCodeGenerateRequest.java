package com.educheck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "动态码生成请求")
public class DynamicCodeGenerateRequest {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "有效时长(秒)")
    private Integer duration;
}
