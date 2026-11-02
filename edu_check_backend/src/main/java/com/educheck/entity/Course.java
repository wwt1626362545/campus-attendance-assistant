package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程表
 */
@Data
@TableName("course")
public class Course {

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名字 */
    private String name;

    /** 授课教师 */
    private String teacher;

    /** 上课地点 */
    private String location;

    /** 上课时间 */
    private String startTime;

    /** 下课时间 */
    private String endTime;

    /** 第几节 */
    private String section;

    /** 星期几 */
    private String weekDay;

    /** 起始周 */
    private Integer weekStart;

    /** 结束周 */
    private Integer weekEnd;

    /** 教师id */
    private Integer teacherId;

    /** 课程状态:1正常 0暂停 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}