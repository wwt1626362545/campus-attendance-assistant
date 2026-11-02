package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 上课签到表
 */
@Data
@TableName("class_checkin")
public class ClassCheckin {

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户id */
    private Integer userId;

    /** 签到课程的id */
    private Integer courseId;

    /** 签到日期 */
    private LocalDate date;

    /** 签到时间 */
    private LocalDateTime checkinTime;

    /** 签到方式 */
    private String method;

    /** 纬度 */
    private BigDecimal locationLat;

    /** 经度 */
    private BigDecimal locationLng;

    /** 定位地址 */
    private String locationAddr;

    /** 动态码 */
    private String dynamicCode;

    /** 打卡状态 */
    private String status;

    /** 签到会话id */
    private String sessionId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}