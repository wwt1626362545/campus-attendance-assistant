package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实习信息表
 */
@Data
@TableName("internship")
public class Internship {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 实习公司 */
    private String company;

    /** 实习岗位 */
    private String role;

    /** 实习开始日期 */
    private LocalDate startDate;

    /** 实习结束日期 */
    private LocalDate endDate;

    /** 总实习天数 */
    private Integer totalDays;

    /** 状态: 1进行中 0已结束 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
