package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("intern_checkin")
public class InternCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long internshipId;

    private LocalDate date;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkinTime;

    private String locationAddr;

    private String logContent;

    private String status;

    /** 实习公司名称（非数据库） */
    @TableField(exist = false)
    private String company;

    /** 学生姓名（非数据库，管理端展示用） */
    @TableField(exist = false)
    private String userName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
