package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_record")
public class RepairRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentName;
    private String studentId;
    private String dormBuilding;
    private String dormRoom;
    private String category;
    private String description;
    private String images;
    private String phone;
    private String status;
    private String reply;
    private Long handlerId;
    private LocalDateTime handleTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
