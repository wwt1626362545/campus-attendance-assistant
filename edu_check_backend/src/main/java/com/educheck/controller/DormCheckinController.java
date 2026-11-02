package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.common.RateLimiter;
import com.educheck.dto.DormCheckinRequest;
import com.educheck.entity.CheckinStats;
import com.educheck.entity.DormCheckin;
import com.educheck.service.CheckinStatsService;
import com.educheck.service.DormCheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
// 返回的数据转json
@RestController
// 增加统一前缀 url
@RequestMapping("/api/dorm")
// 给final 修饰的变量自动创建对象
@RequiredArgsConstructor
// 文档提示
@Tag(name = "查寝打卡",description = "查寝打卡接口")
public class DormCheckinController {
    // 定义 自动创建对应对象
    private final DormCheckinService dormCheckinService;
    // 定义 创建token管理
    private final TokenContextHolder tokenContextHolder;
    private final CheckinStatsService checkinStatsService;
    private final RateLimiter rateLimiter;
    // 创建打卡统计 service
    @PostMapping("/checkin")
    @Operation(summary = "查寝打卡")
    public Result<DormCheckin> checkin(
            @RequestBody DormCheckinRequest request
    ){
        Long userId=tokenContextHolder.requireCurrentUserId();
        // 打卡限流：同一用户每10秒只能打卡一次
        if (!rateLimiter.tryAcquire("rate:dorm:" + userId, 1, 10, java.util.concurrent.TimeUnit.SECONDS)) {
            return Result.error("操作过于频繁，请稍后再试");
        }
        // 人脸验证检查
        if(request.getFaceVerified()==null||!request.getFaceVerified()){
            return Result.error("请先完成人脸验证");
        }
        // 正常打卡 晚归 (23:00以后打卡判定为晚归)
        // 取当前时间
        LocalTime now=LocalTime.now();
        LocalTime curfew= LocalTime.of(23, 0);
        // 打卡状态设置
        String status = !now.isBefore(curfew) ? "late" : "normal";
        log.info("服务器当前LocalTime：{}，计算得到status：{}", now, status);
// 查询该用户今天有没有查寝打卡
        DormCheckin existing = dormCheckinService.lambdaQuery()
                // 拿到当前用户数据
                .eq(DormCheckin::getUserId, userId)
                // 取当天数据
                .eq(DormCheckin::getDate, LocalDate.now())
                .one();

        if(existing != null){
            // 今天用户有查寝打卡记录  需要进行更新
            existing.setCheckinTime(LocalDateTime.now());
            existing.setLocationLat(request.getLocationLat());
            existing.setLocationLng(request.getLocationLng());
            existing.setLocationAddr(request.getLocationAddr());
            existing.setBuilding(request.getBuilding());
            existing.setRoom(request.getRoom());
// 根据前端参数 是否在宿舍 存入 1 或者 0
            existing.setInDormArea(request.getInDormArea() != null
                    && request.getInDormArea() ? 1 : 0);
// 已经经过人脸验证
            existing.setFaceVerified(1);
            // 如果有人脸图片 将人脸图片 存入查寝打卡记录里面
            if(request.getFaceImage()!=null&&!request.getFaceImage().isEmpty()){
                existing.setFaceImageUrl(request.getFaceImage());
            }
            existing.setStatus(status);
            //数据库对记录更新
            dormCheckinService.updateById(existing);
            return Result.success("更新查寝打卡成功", existing);
        }
        //今天第一次打卡 创建实体，存信息
        DormCheckin checkin=new DormCheckin();
        checkin.setUserId(userId);
        checkin.setDate(LocalDate.now());
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setLocationLat(request.getLocationLat());
        checkin.setLocationLng(request.getLocationLng());
        checkin.setLocationAddr(request.getLocationAddr());
        checkin.setBuilding(request.getBuilding());
        checkin.setRoom(request.getRoom());
        checkin.setInDormArea(request.getInDormArea() != null
                && request.getInDormArea() ? 1 : 0);
        checkin.setFaceVerified(1);
        if(request.getFaceImage()!=null&&!request.getFaceImage().isEmpty()){
            checkin.setFaceImageUrl(request.getFaceImage());
        }
        checkin.setStatus(status);
        // 向数据库插入记录
        dormCheckinService.save(checkin);
// 走的是插入逻辑 打卡统计表也需要发生对应变化
// 查询今天该用户打卡统计表 有没有记录
        CheckinStats stats=checkinStatsService.lambdaQuery()
                .eq(CheckinStats::getUserId,userId).one();
// 该用户今天有打卡统计记录 更新记录
        if(stats!=null){
            // 查寝打卡次数+1
            stats.setDormTotal(stats.getDormTotal()+1);
            checkinStatsService.updateById(stats);
        }else {
            // 今天该用户没有打卡统计记录 增加记录
            CheckinStats newStats=new CheckinStats();
            newStats.setUserId(userId);
            newStats.setDormTotal(1);
            newStats.setClassTotal(0);
            newStats.setInternTotal(0);
            newStats.setStreakDays(0);
            newStats.setTotalPoints(0);
            // 向数据库插入 该用户的 打卡统计记录
            checkinStatsService.save(newStats);
        }
        return Result.success("打卡成功",checkin);
    }
    @GetMapping("/history")
    @Operation(summary = "获取查寝打卡记录")
    public Result<Page<DormCheckin>> history(
            // 第几页
            @RequestParam(defaultValue = "1") int page,
            // 一页多少条
            @RequestParam(defaultValue = "20") int size
    ){
        Long userId = tokenContextHolder.requireCurrentUserId();
        // 开始查询
        LambdaQueryWrapper<DormCheckin> wrapper = new LambdaQueryWrapper<DormCheckin>()
                // 查询当前用户的打卡记录
                .eq(DormCheckin::getUserId, userId)
                // 按日期、打卡时间倒序，最新记录在前
                .orderByDesc(DormCheckin::getDate, DormCheckin::getCheckinTime);
        return Result.success(dormCheckinService.page(
                new Page<>(page,size),wrapper
        ));

    }
    @GetMapping("/today-status")
    @Operation(summary = "获取今天打卡状态")
    public Result<DormCheckin> todayStatus(){
        // 获取用户id
        Long userId=tokenContextHolder.requireCurrentUserId();
        // 根据条件查询 单条打卡记录
        DormCheckin checkin=dormCheckinService.lambdaQuery()
                // 用当前用户去筛选
                .eq(DormCheckin::getUserId,userId)
                // 用今天的日期去筛选
                .eq(DormCheckin::getDate, LocalDate.now())
                .one();
        return Result.success(checkin);
    }
}
