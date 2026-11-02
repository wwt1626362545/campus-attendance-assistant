package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 请假记录表
 */
@Data
@TableName("leave_record")
public class Leave {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 请假类型: sick病假/personal事假/official公假/annual年假 */
    private String type;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    /** 请假事由 */
    private String reason;

    /** 状态: pending待审批/approved已批准/rejected已驳回 */
    private String status;

    /** 驳回原因 */
    private String rejectReason;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ====== 非数据库字段，供前端展示使用 ======

    /** 请假类型名称 */
    @TableField(exist = false)
    private String typeName;

    /** 请假类型CSS类名 */
    @TableField(exist = false)
    private String typeClass;

    /** 状态名称 */
    @TableField(exist = false)
    private String statusName;

    /** 状态CSS类名 */
    @TableField(exist = false)
    private String statusClass;

    /** 请假天数 */
    @TableField(exist = false)
    private Long days;

    /** 提交时间字符串 */
    @TableField(exist = false)
    private String submitTime;

    /** 申请人姓名(非数据库) */
    @TableField(exist = false)
    private String studentName;

    /** 申请人学号(非数据库) */
    @TableField(exist = false)
    private String studentId;

    /** 审批人姓名(非数据库) */
    @TableField(exist = false)
    private String approver;

    /** 审批意见(非数据库) */
    @TableField(exist = false)
    private String approveComment;

    /** 状态描述(非数据库) */
    @TableField(exist = false)
    private String statusDesc;
}
