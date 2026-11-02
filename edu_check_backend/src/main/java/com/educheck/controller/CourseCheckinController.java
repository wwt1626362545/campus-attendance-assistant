package com.educheck.controller;

import com.educheck.common.RateLimiter;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.config.CacheService;
import com.educheck.dto.ClassCheckinRequest;
import com.educheck.entity.CheckinStats;
import com.educheck.entity.ClassCheckin;
import com.educheck.entity.Course;
import com.educheck.service.CheckinStatsService;
import com.educheck.service.ClassCheckinService;
import com.educheck.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Tag(name = "上课打卡", description = "课堂签到接口")
public class CourseCheckinController {

    private final CourseService courseService;
    private final ClassCheckinService classCheckinService;
    private final TokenContextHolder tokenContextHolder;
    // 创建打卡统计service
    private final CheckinStatsService checkinStatsService;
    private final RateLimiter rateLimiter;
    private final CacheService cacheService;

    @PostMapping("/checkin")
    @Operation(summary = "学生上课签到")
    public Result<ClassCheckin> checkin(
            @RequestBody ClassCheckinRequest request
    ) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        // 上课打卡限流：同一用户每10秒只能打卡一次
        if (!rateLimiter.tryAcquire("rate:course:" + userId, 1, 10, java.util.concurrent.TimeUnit.SECONDS)) {
            return Result.error("操作过于频繁，请稍后再试");
        }
        // 人脸验证通过判断
        if (request.getFaceVerified() == null || !request.getFaceVerified()) {
            return Result.error("请先完成人脸验证");
        }
        // 课程参数是否正常
        if (request.getCourseId() == null) {
            return Result.error("课程id不能为空");
        }
        // 校验课程信息是否存在
        Course course = courseService.getById(request.getCourseId());
        if (course == null) {
            return Result.error("课程不存在");
        }
/// 计算签到时间窗口
        LocalTime startTime = LocalTime.parse(course.getStartTime());
        LocalTime endTime = LocalTime.parse(course.getEndTime());
        LocalTime now = LocalTime.now();
        LocalTime windowOpen = startTime.minusMinutes(10);
// 检查一下是否早于签到窗口
        if (now.isBefore(windowOpen)) {
            // 计算还有多久能签到
            long minutesUntilOpen = Duration.between(now, windowOpen).toMinutes();
            return Result.error("签到尚未开始，离签到时间还有" + minutesUntilOpen + "分钟");
        }
        if (now.isAfter(endTime)) {
            return Result.error("课程已结束，不能签到");
        }
// 下面都是可签到情况
// 获取日期 只包括 年月日
        LocalDate today = LocalDate.now();
// 查询当前用户上课签到情况
        ClassCheckin existing = classCheckinService.lambdaQuery()
                // 当前用户
                .eq(ClassCheckin::getUserId, userId)
                // 当前课程
                .eq(ClassCheckin::getCourseId, request.getCourseId())
                // 当天
                .eq(ClassCheckin::getDate, today)
                .one();
// 确定签到状态
// 签到时间晚于上课时间 迟到 早于 正常
        String status = now.isBefore(startTime) ? "present" : "late";
        if (existing != null) {
            // 今天当前用户当前课程已经签到  更新签到信息
            existing.setCheckinTime(LocalDateTime.now());
            existing.setMethod(request.getMethod());
            existing.setLocationLat(request.getLocationLat());
            existing.setLocationLng(request.getLocationLng());
            existing.setLocationAddr(request.getLocationAddr());
            existing.setDynamicCode(request.getDynamicCode());
            existing.setStatus(status);
            classCheckinService.updateById(existing);
            return Result.success("重新签到成功", existing);
        }
// 今天当前用户当前课程没有签到  增加签到记录
        ClassCheckin checkin = new ClassCheckin();
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setMethod(request.getMethod());
        checkin.setLocationLat(request.getLocationLat());
        checkin.setLocationLng(request.getLocationLng());
        checkin.setLocationAddr(request.getLocationAddr());
        checkin.setDynamicCode(request.getDynamicCode());
        checkin.setStatus(status);
        checkin.setUserId(userId.intValue());
        checkin.setCourseId(request.getCourseId().intValue());
        checkin.setDate(today);
// 对数据库表进行插入操作
        classCheckinService.save(checkin);
// 打卡统计表进行更新
// 打卡统计表有没有当前用户的记录
        CheckinStats stats = checkinStatsService.lambdaQuery()
                .eq(CheckinStats::getUserId, userId).one();
// 打卡统计表有当前用户的记录  课程打卡数据+1
        if (stats != null) {
            stats.setClassTotal(stats.getClassTotal() + 1);
            checkinStatsService.updateById(stats);
        } else {
            // 打卡统计表没有当前用户的记录 初始化 课程打卡数据为1
            CheckinStats newStats = new CheckinStats();
            newStats.setUserId(userId);
            newStats.setDormTotal(0);
            newStats.setClassTotal(1);
            newStats.setInternTotal(0);
            newStats.setStreakDays(0);
            newStats.setTotalPoints(0);
            checkinStatsService.save(newStats);
        }
        return Result.success("签到成功", checkin);
    }
    @GetMapping("/today")
    @Operation(summary = "获取今天的课程（包括对应的签到状态）")
    public Result<List<Map<String, Object>>> todayCourse() {
        Long userId = tokenContextHolder.requireCurrentUserId();
        // 获取今天是周几
        java.time.DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        // 将英文的周几转成中文
        String weekDay = switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
            default -> null;
        };
        // 如果周几转换失败 返回前端空列表
        if (weekDay == null) {
            return Result.success(List.of());
        }
        // 根据今天是周几 查询今天对应课程
        List<Course> courses = courseService.lambdaQuery()
                .eq(Course::getStatus, 1)
                .eq(Course::getWeekDay, weekDay)
                .orderByAsc(Course::getStartTime)
                .list();

        // 假设今天没课 返回空列表
        if (courses.isEmpty()) {
            return Result.success(List.of());
        }
        // 获取今天的日期（只包括年月日）
        LocalDate today = LocalDate.now();
        // 获取现在的时间 （只包括时分秒）
        LocalTime now = LocalTime.now();
        // 查询当前用户今天的课程签到状态
        List<ClassCheckin> todayCheckins = classCheckinService.lambdaQuery()
                // 查询当前用户的
                .eq(ClassCheckin::getUserId, userId)
                // 查询今天的
                .eq(ClassCheckin::getDate, today)
                .list();

        // 将签到记录转成 Map key 是课程id  value 是签到记录
        // 修复：转换Integer课程id为Long，匹配Map<Long, ClassCheckin>，不改动原有变量名
        Map<Long, ClassCheckin> checkinMap = todayCheckins.stream().collect(
                Collectors.toMap(
                        item -> Long.valueOf(item.getCourseId()),
                        c -> c,
                        (ClassCheckin a, ClassCheckin b) -> a
                )
        );

        // 存储 自动生成旷课记录列表
        List<ClassCheckin> absentRecords = new ArrayList<>();
        // 课程上课时间结束 但学生没有签到 自动给学生这门课程标记为旷课
        // 循环今天的课程 一条条去比对
        for (Course course : courses) {
            // 当前课程在 签到记录里面不存在 = 学生没有签到
            if (!checkinMap.containsKey(course.getId())) {
                // 提取当前课程结束的时间
                LocalTime end = LocalTime.parse(course.getEndTime());
                // 当前时间超过课程结束时间（这门课已经下课）
                if (now.isAfter(end)) {
                    // 将 学生 这门课的签到记录标记为旷课
                    ClassCheckin absent = new ClassCheckin();
                    // 修复：Long转Integer适配实体入参
                    absent.setUserId(userId.intValue());
                    absent.setCourseId(course.getId().intValue());
                    absent.setDate(today);
                    // 记录标记旷课的时间
                    absent.setCheckinTime(LocalDateTime.now());
                    // 签到方式 auto (系统自动签到)
                    absent.setMethod("auto");
                    // 签到状态 absent = 旷课
                    absent.setStatus("absent");
                    // 加入 列表记录里面 后面统一操作
                    absentRecords.add(absent);
                }
            }
        }

        // =========【修复】移到循环外面，只执行一次=========
        if (!absentRecords.isEmpty()) {
            // 批量插入多条旷课签到记录
            classCheckinService.saveBatch(absentRecords);
            // 将旷课记录也附加到checkinMap 里面  返回签到
            for (ClassCheckin a : absentRecords) {
                checkinMap.put(Long.valueOf(a.getCourseId()), a);
            }
        }

        // =========组装返回数据（使用stream，修正你断裂的语法）=========
        List<Map<String, Object>> resultList = courses.stream().map(course -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", course.getId());
            item.put("name", course.getName());
            item.put("teacher", course.getTeacher());
            item.put("location", course.getLocation());
            item.put("startTime", course.getStartTime());
            item.put("endTime", course.getEndTime());
            item.put("section", course.getSection());
            item.put("weekDay", course.getWeekDay());
            // 根据课程id查询签到记录
            ClassCheckin checkin = checkinMap.get(course.getId());
            if (checkin != null) {
                item.put("checkinStatus", checkin.getStatus());
                item.put("checkinTime", checkin.getCheckinTime() != null ?
                        checkin.getCheckinTime().toString() : null);
            } else {
                // 对应课程没有签到记录 （说明这门课没有开始上课）
                // 解析课程开始时间
                LocalTime start = LocalTime.parse(course.getStartTime());
                // 解析课程结束时间
                LocalTime end = LocalTime.parse(course.getEndTime());
                // 签到窗口提前十分钟开启
                LocalTime windowOpen = start.minusMinutes(10);
                // 课程开始时间在十分钟之后
                if (now.isBefore(windowOpen)) {
                    // 不让用户签到
                    item.put("checkinStatus", "waiting");
                } else if (now.isAfter(end)) {
                    // 当前时间 晚于下课时间（已经下课，用户还没签到）
                    item.put("checkinStatus", "absent");
                } else {
                    // 处于签到时间段内，可以签到
                    item.put("checkinStatus", "ready");
                }
                item.put("checkinTime", null);
            }
            return item;
        }).collect(Collectors.toList());

        return Result.success(resultList);
    }

    @GetMapping("/schedule")
    @Operation(summary = "获取周课程表（所有课程按星期排序，Redis缓存30秒）")
    public Result<List<Course>> schedule() {
        // 从缓存读取
        List<Course> cached = cacheService.get("course:schedule", List.class);
        if (cached != null) return Result.success(cached);

        List<Course> courses = courseService.lambdaQuery()
                .eq(Course::getStatus, 1)
                .orderByAsc(Course::getWeekDay, Course::getStartTime)
                .list();
        cacheService.set("course:schedule", courses, 30, TimeUnit.SECONDS);
        return Result.success(courses);
    }
}

