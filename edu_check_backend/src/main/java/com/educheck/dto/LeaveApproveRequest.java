package com.educheck.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "请假审批请求")
public class LeaveApproveRequest {

    @Schema(description = "是否批准")
    private Boolean approved;

    @Schema(description = "驳回原因")
    private String rejectReason;
}
