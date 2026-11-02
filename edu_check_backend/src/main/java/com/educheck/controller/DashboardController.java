package com.educheck.controller;

import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.config.CacheService;
import com.educheck.entity.CheckinStats;
import com.educheck.entity.User;
import com.educheck.service.CheckinStatsService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.Map;

// @+单词 注解 给类/变量 增加特殊功能
// 将接口返回的数据转成json格式
@RestController
// 定义当前控制的处理的接口 开头
@RequestMapping("/api/dashboard")
// 用于创建service对象
// 给所有 final 修饰的成员变量 创建对应对象
@RequiredArgsConstructor
// 用于后续生成文档提示
@Tag(name = "首页仪表盘",description = "首页数据总览接口")
public class DashboardController {
    // 创建用户 service对象
    private final UserService userService;

    // 创建打卡统计 service
    private final CheckinStatsService checkinStatsService;

    // 创建token  对象 （用户校验）
    private final TokenContextHolder tokenContextHolder;

    // Redis 缓存
    private final CacheService cacheService;

    // 指定请求方式 get
    // 这个方法处理的完整接口路径是 /api/dashboard/banners
    @GetMapping("/banners")
    // 用于文档提示
    @Operation(summary = "获取轮播图列表")
    // 用具体的方法处理前端请求
    public Result<String[]> banners(){
        return Result.success(new String[]{
                "/images/banner_1.webp",
                "/images/banner_2.webp",
                "/images/banner_3.webp"
        });
    }
    @GetMapping("/overview")
    @Operation(summary = "获取首页概览信息（Redis缓存60秒）")
    public Result<Map<String,Object>> overview(){
        // 获取用户id
        Long userId=tokenContextHolder.requireCurrentUserId();
        String cacheKey = "dashboard:overview:" + userId;

        // 尝试从缓存读取
        Map<String,Object> data = cacheService.get(cacheKey, Map.class);
        if (data != null) return Result.success(data);

        // 缓存未命中，查询数据库
        User user=userService.getById(userId);
        data=new HashMap<>();

        // 用户存在
        if(user!=null){
           Map<String,Object> userInfo=new HashMap<>();
            userInfo.put("userId",userId);
            userInfo.put("name",user.getName());
            userInfo.put("college",user.getCollege());
            userInfo.put("studentId",user.getStudentId());
           userInfo.put("avatar", user.getAvatar() != null ? user.getAvatar() : "/images/001.png");
           data.put("userInfo",userInfo);
        }
        // 从数据库读取真实统计数据
        CheckinStats stats = checkinStatsService.lambdaQuery()
                .eq(CheckinStats::getUserId, userId).one();
        Map<String,Object> statsMap=new HashMap<>();
        if (stats != null) {
            statsMap.put("dormTotal", stats.getDormTotal());
            statsMap.put("classTotal", stats.getClassTotal());
            statsMap.put("internTotal", stats.getInternTotal());
            statsMap.put("streakDays", stats.getStreakDays());
        } else {
            statsMap.put("dormTotal", 0);
            statsMap.put("classTotal", 0);
            statsMap.put("internTotal", 0);
            statsMap.put("streakDays", 0);
        }
        data.put("stats",statsMap);
        // 写入缓存，60秒过期
        cacheService.set(cacheKey, data, 60, TimeUnit.SECONDS);
        return Result.success(data);
    }
}
