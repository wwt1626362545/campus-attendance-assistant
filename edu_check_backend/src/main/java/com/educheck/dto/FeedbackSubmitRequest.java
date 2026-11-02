package com.educheck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "反馈提交请求")
public class FeedbackSubmitRequest {

    @Schema(description = "反馈类型: suggestion/bug/complaint/other")
    private String type;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式")
    private String contact;
}
