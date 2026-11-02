package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态码表
 */
@Data
@TableName("dynamic_code")
public class DynamicCode {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程ID */
    private Long courseId;

    /** 动态码(6位数字) */
    private String dynamicCode;

    /** 会话ID */
    private String sessionId;

    /** 有效时长(秒) */
    private Integer duration;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 状态: active有效/expired已过期 */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
