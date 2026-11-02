package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实习照片表
 */
@Data
@TableName("intern_photo")
public class InternPhoto {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 打卡记录ID */
    private Long checkinId;

    /** 图片URL */
    private String imageUrl;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
