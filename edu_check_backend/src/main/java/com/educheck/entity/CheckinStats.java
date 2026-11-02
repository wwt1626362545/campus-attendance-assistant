package com.educheck.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("checkin_stats")
public class CheckinStats {

    /*** 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 查寝打卡总数
     */
    private Integer dormTotal;

    /**
     * 上课签到总数
     */
    private Integer classTotal;

    /**
     * 实习打卡总数
     */
    private Integer internTotal;

    /**
     * 连续打卡天数
     */
    private Integer streakDays;

    /**
     * 总积分
     */
    private Integer totalPoints;

    /**
     * 更新时间
     */

    // 更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}