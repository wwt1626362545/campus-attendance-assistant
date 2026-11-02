package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    // 主键id  自增
    @TableId(type = IdType.AUTO)
    private Long id;
    //问题分类
    private String category;

    // 问题
    private String question;

    // 对应问题的答案
    private String answer;

    // 关键字
    private String keywords;

    // 同义词扩展
    private String synonyms;

    //排序
    private Integer sortOrder;

    // 创建时间 自动填充 创建的时候自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}