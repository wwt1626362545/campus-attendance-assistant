package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

@TableName
@Data
public class UserFace {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String faceUrl;
    private String faceToken;
    private Integer registered;
    private Integer version;
   @TableField(fill = FieldFill.INSERT)
   private LocalDateTime createdAt;
   @TableField(fill = FieldFill.INSERT_UPDATE)
   private LocalDateTime updatedAt;





}
