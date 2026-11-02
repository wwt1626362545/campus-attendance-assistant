package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

// 自动生成get set方法
@Data
// 映射对应数据库表
@TableName("unanswered_log")
public class UnansweredLog {

    // 主键id  自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private String question;
    // 分类
    private String category;

    // 知识库是否补充该问题 1 已补充 0未补充
    private Integer answered;

    // 创建时间 自动填充 创建的时候自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}