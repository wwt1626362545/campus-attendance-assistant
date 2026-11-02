package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.DynamicCodeGenerateRequest;
import com.educheck.entity.*;
import com.educheck.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "教师端管理", description = "教师端学生管理、动态码等接口")
public class TeacherController {

    private final DynamicCodeService dynamicCodeService;
    private final CourseService courseService;
    private final UserService userService;
    private final ClassCheckinService classCheckinService;
    private final DormCheckinService dormCheckinService;
    private final CheckinStatsService checkinStatsService;
    private final TokenContextHolder tokenContextHolder;

    // ==================== 动态码 ====================

    @PostMapping("/dynamic-code/generate")
    @Operation(summary = "生成动态码")
    public Result<Map<String, Object>> generateDynamicCode(@RequestBody DynamicCodeGenerateRequest request) {
        Long courseId = request.getCourseId();
        int duration = request.getDuration() != null ? request.getDuration() : 60;

        // 检查课程是否存在
        Course course = courseService.getById(courseId);
        if (course == null) {
            return Result.error(404, "课程不存在");
        }

        // 将之前的活跃动态码置为过期
        dynamicCodeService.lambdaUpdate()
                .eq(DynamicCode::getCourseId, courseId)
                .eq(DynamicCode::getStatus, "active")
                .set(DynamicCode::getStatus, "expired")
                .update();

        // 生成6位随机数字动态码
        String code = String.format("%06d", new Random().nextInt(1000000));
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        DynamicCode dynamicCode = new DynamicCode();
        dynamicCode.setCourseId(courseId);
        dynamicCode.setDynamicCode(code);
        dynamicCode.setSessionId(sessionId);
        dynamicCode.setDuration(duration);
        dynamicCode.setExpiresAt(LocalDateTime.now().plusSeconds(duration));
        dynamicCode.setStatus("active");
        dynamicCodeService.save(dynamicCode);

        Map<String, Object> data = new HashMap<>();
        data.put("dynamicCode", code);
        data.put("sessionId", sessionId);
        data.put("duration", duration);
        return Result.success("生成成功", data);
    }

    @GetMapping("/dynamic-code/active")
    @Operation(summary = "获取活跃动态码")
    public Result<DynamicCode> getActiveDynamicCode(@RequestParam Long courseId) {
        DynamicCode active = dynamicCodeService.lambdaQuery()
                .eq(DynamicCode::getCourseId, courseId)
                .eq(DynamicCode::getStatus, "active")
                .gt(DynamicCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(DynamicCode::getCreatedAt)
                .last("LIMIT 1")
                .one();
        return Result.success(active);
    }

    // ==================== 学生管理 ====================

    @GetMapping("/students")
    @Operation(summary = "获取学生列表(简单)")
    public Result<List<User>> getStudents() {
        List<User> students = userService.lambdaQuery()
                .eq(User::getRole, "student")
                .eq(User::getStatus, 1)
                .list();
        students.forEach(s -> s.setPassword(null));
        return Result.success(students);
    }

    @GetMapping("/students/page")
    @Operation(summary = "分页获取学生列表")
    public Result<Page<User>> getStudentsPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "student")
                .eq(User::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getName, keyword)
                    .or().like(User::getStudentId, keyword));
        }
        wrapper.orderByAsc(User::getStudentId);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        // 脱敏密码
        result.getRecords().forEach(s -> s.setPassword(null));
        // 附加今日打卡状态
        attachTodayCheckinStatus(result.getRecords());
        return Result.success(result);
    }

    /** 给学生列表附加今日上课打卡和查寝状态 */
    private void attachTodayCheckinStatus(List<User> students) {
        LocalDate today = LocalDate.now();
        for (User student : students) {
            // 今日上课打卡状态
            ClassCheckin classCheckin = classCheckinService.lambdaQuery()
                    .eq(ClassCheckin::getUserId, student.getId())
                    .eq(ClassCheckin::getDate, today)
                    .orderByDesc(ClassCheckin::getCheckinTime)
                    .last("LIMIT 1")
                    .one();
            student.setTodayCheckin(classCheckin != null ? classCheckin.getStatus() : "none");

            // 今日查寝打卡状态
            DormCheckin dormCheckin = dormCheckinService.lambdaQuery()
                    .eq(DormCheckin::getUserId, student.getId())
                    .eq(DormCheckin::getDate, today)
                    .one();
            student.setTodayDorm(dormCheckin != null ? dormCheckin.getStatus() : "none");
        }
    }

    @GetMapping("/students/checkin/{studentId}")
    @Operation(summary = "获取学生打卡详情")
    public Result<Map<String, Object>> getStudentCheckinDetail(@PathVariable Long studentId) {
        User student = userService.getById(studentId);
        if (student == null) {
            return Result.error(404, "学生不存在");
        }
        student.setPassword(null);

        LocalDate today = LocalDate.now();

        // 今日课程签到情况
        List<ClassCheckin> todayClasses = classCheckinService.lambdaQuery()
                .eq(ClassCheckin::getUserId, studentId)
                .eq(ClassCheckin::getDate, today)
                .list();

        // 今日查寝情况
        DormCheckin todayDorm = dormCheckinService.lambdaQuery()
                .eq(DormCheckin::getUserId, studentId)
                .eq(DormCheckin::getDate, today)
                .one();

        // 近期上课签到记录（最近5条，附上课程名称）
        List<ClassCheckin> recentClassRecords = classCheckinService.lambdaQuery()
                .eq(ClassCheckin::getUserId, studentId)
                .orderByDesc(ClassCheckin::getDate, ClassCheckin::getCheckinTime)
                .last("LIMIT 5")
                .list();
        List<Map<String, Object>> classRecordsWithCourse = new ArrayList<>();
        for (ClassCheckin cc : recentClassRecords) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cc.getId());
            item.put("date", cc.getDate());
            item.put("checkinTime", cc.getCheckinTime());
            item.put("status", cc.getStatus());
            if (cc.getCourseId() != null) {
                Course course = courseService.getById(cc.getCourseId().longValue());
                item.put("courseName", course != null ? course.getName() : "未知课程");
            } else {
                item.put("courseName", "未知课程");
            }
            classRecordsWithCourse.add(item);
        }

        // 近期查寝记录（最近5条）
        List<DormCheckin> recentDormRecords = dormCheckinService.lambdaQuery()
                .eq(DormCheckin::getUserId, studentId)
                .orderByDesc(DormCheckin::getDate, DormCheckin::getCheckinTime)
                .last("LIMIT 5")
                .list();

        // 统计信息
        CheckinStats stats = checkinStatsService.lambdaQuery()
                .eq(CheckinStats::getUserId, studentId)
                .one();

        Map<String, Object> data = new HashMap<>();
        data.put("student", student);
        data.put("stats", stats);
        data.put("todayClasses", todayClasses);
        data.put("todayDorm", todayDorm);
        data.put("recentClassRecords", classRecordsWithCourse);
        data.put("recentDormRecords", recentDormRecords);

        return Result.success(data);
    }

    @GetMapping("/checkin/today-summary")
    @Operation(summary = "获取今日打卡概览")
    public Result<Map<String, Object>> getTodayCheckinSummary() {
        LocalDate today = LocalDate.now();
        List<User> students = userService.lambdaQuery()
                .eq(User::getRole, "student")
                .eq(User::getStatus, 1)
                .list();

        long total = students.size();
        long classChecked = 0;
        long dormChecked = 0;

        for (User student : students) {
            ClassCheckin cc = classCheckinService.lambdaQuery()
                    .eq(ClassCheckin::getUserId, student.getId())
                    .eq(ClassCheckin::getDate, today)
                    .last("LIMIT 1")
                    .one();
            if (cc != null && !"absent".equals(cc.getStatus())) {
                classChecked++;
            }

            DormCheckin dc = dormCheckinService.lambdaQuery()
                    .eq(DormCheckin::getUserId, student.getId())
                    .eq(DormCheckin::getDate, today)
                    .one();
            if (dc != null) {
                dormChecked++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", total);
        data.put("classChecked", classChecked);
        data.put("classNotChecked", total - classChecked);
        data.put("dormChecked", dormChecked);
        data.put("dormNotChecked", total - dormChecked);
        return Result.success(data);
    }
}
