package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告表
 */
@Data
@TableName("announcement")
public class Announcement {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告类型: notice通知/activity活动/academic学术/policy思政 */
    private String type;

    /** 公告摘要 */
    private String summary;

    /** 公告正文 */
    private String content;

    /** 发布部门 */
    private String department;

    /** 是否置顶: 1置顶 0普通 */
    private Integer isTop;

    /** 发布者名称 */
    private String publisher;

    /** 发布者用户ID */
    private Long publisherId;


    /** 阅读次数 */
    @TableField("read_count")
    private Integer readCount;

    /** 类型名称(非数据库字段) */
    @TableField(exist = false)
    private String typeName;

    /** 日期字符串(非数据库字段) */
    @TableField(exist = false)
    private String date;

    /** 来源(非数据库字段) */
    @TableField(exist = false)
    private String source;
    /** 状态: 1正常 0下架 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
