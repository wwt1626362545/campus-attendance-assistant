package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 意见反馈表
 */
@Data
@TableName("feedback")
public class Feedback {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 反馈类型: suggestion建议/bug问题反馈/complaint投诉/other其他 */
    private String type;

    /** 反馈内容 */
    private String content;

    /** 联系方式 */
    private String contact;

    /** 状态: pending待处理/resolved已处理 */
    private String status;

    /** 回复内容 */
    private String reply;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
