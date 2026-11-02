package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.InternCheckinRequest;
import com.educheck.entity.CheckinStats;
import com.educheck.entity.InternCheckin;
import com.educheck.entity.InternPhoto;
import com.educheck.entity.Internship;
import com.educheck.entity.User;
import com.educheck.service.CheckinStatsService;
import com.educheck.service.InternCheckinService;
import com.educheck.service.InternPhotoService;
import com.educheck.service.InternshipService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intern")
@RequiredArgsConstructor
@Tag(name = "实习打卡", description = "实习打卡接口")
public class InternController {

    private final InternshipService internshipService;
    private final InternCheckinService internCheckinService;
    private final InternPhotoService internPhotoService;
    private final CheckinStatsService checkinStatsService;
    private final UserService userService;
    private final TokenContextHolder tokenContextHolder;

    @GetMapping("/my")
    @Operation(summary = "获取我的实习信息")
    public Result<Internship> myInternship() {
        Long userId = tokenContextHolder.requireCurrentUserId();
        Internship internship = internshipService.lambdaQuery()
                .eq(Internship::getUserId, userId)
                .eq(Internship::getStatus, 1)
                .one();
        return Result.success(internship);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取实习统计")
    public Result<Map<String, Object>> stats() {
        Long userId = tokenContextHolder.requireCurrentUserId();
        Internship internship = internshipService.lambdaQuery()
                .eq(Internship::getUserId, userId)
                .eq(Internship::getStatus, 1)
                .one();

        Map<String, Object> data = new HashMap<>();
        if (internship != null) {
            data.put("company", internship.getCompany());
            data.put("role", internship.getRole());
            data.put("position", internship.getRole());
            data.put("address", "");
            long completedDays = internCheckinService.lambdaQuery()
                    .eq(InternCheckin::getUserId, userId)
                    .eq(InternCheckin::getInternshipId, internship.getId())
                    .count();
            data.put("completedDays", (int) completedDays);
            data.put("totalDays", internship.getTotalDays());
            data.put("progress", internship.getTotalDays() > 0
                    ? (int) (completedDays * 100 / internship.getTotalDays()) : 0);
        } else {
            data.put("company", "");
            data.put("role", "");
            data.put("position", "");
            data.put("address", "");
            data.put("completedDays", 0);
            data.put("totalDays", 0);
            data.put("progress", 0);
        }
        return Result.success(data);
    }

    @PostMapping("/checkin")
    @Operation(summary = "实习打卡")
    public Result<InternCheckin> checkin(@RequestBody InternCheckinRequest request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        if (request.getFaceVerified() == null || !request.getFaceVerified()) {
            return Result.error("请先完成人脸验证");
        }
        InternCheckin existing = internCheckinService.lambdaQuery()
                .eq(InternCheckin::getUserId, userId)
                .eq(InternCheckin::getDate, LocalDate.now())
                .one();
        if (existing != null) {
            return Result.error("今天已经打过卡了");
        }
        InternCheckin checkin = new InternCheckin();
        checkin.setUserId(userId);
        checkin.setInternshipId(request.getInternshipId());
        checkin.setDate(LocalDate.now());
        checkin.setCheckinTime(LocalDateTime.now());
        checkin.setLocationAddr(request.getLocationAddr());
        checkin.setLogContent(request.getLogContent());
        LocalTime now = LocalTime.now();
        checkin.setStatus(now.isAfter(LocalTime.of(9, 30)) ? "late" : "normal");
        internCheckinService.save(checkin);

        CheckinStats stats = checkinStatsService.lambdaQuery()
                .eq(CheckinStats::getUserId, userId).one();
        if (stats != null) {
            stats.setInternTotal(stats.getInternTotal() + 1);
            checkinStatsService.updateById(stats);
        } else {
            CheckinStats newStats = new CheckinStats();
            newStats.setUserId(userId);
            newStats.setDormTotal(0);
            newStats.setClassTotal(0);
            newStats.setInternTotal(1);
            newStats.setStreakDays(0);
            newStats.setTotalPoints(0);
            checkinStatsService.save(newStats);
        }
        return Result.success("打卡成功", checkin);
    }

    @GetMapping("/history")
    @Operation(summary = "获取实习打卡记录")
    public Result<Page<InternCheckin>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        LambdaQueryWrapper<InternCheckin> wrapper = new LambdaQueryWrapper<InternCheckin>()
                .eq(InternCheckin::getUserId, userId)
                .orderByDesc(InternCheckin::getDate, InternCheckin::getCheckinTime);
        Page<InternCheckin> internPage = internCheckinService.page(new Page<>(page, size), wrapper);
        for (InternCheckin r : internPage.getRecords()) {
            if (r.getInternshipId() != null) {
                Internship ins = internshipService.getById(r.getInternshipId());
                if (ins != null) r.setCompany(ins.getCompany());
            }
        }
        return Result.success(internPage);
    }

    @GetMapping("/admin/history")
    @Operation(summary = "管理端获取全部实习打卡记录")
    public Result<Page<InternCheckin>> adminHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String role = tokenContextHolder.getCurrentRole();
        if (!"admin".equals(role) && !"teacher".equals(role)) {
            return Result.error(403, "权限不足，需要管理员或教师身份");
        }
        LambdaQueryWrapper<InternCheckin> wrapper = new LambdaQueryWrapper<InternCheckin>()
                .orderByDesc(InternCheckin::getDate, InternCheckin::getCheckinTime);
        Page<InternCheckin> internPage = internCheckinService.page(new Page<>(page, size), wrapper);
        for (InternCheckin r : internPage.getRecords()) {
            if (r.getInternshipId() != null) {
                Internship ins = internshipService.getById(r.getInternshipId());
                if (ins != null) r.setCompany(ins.getCompany());
            }
            User user = userService.getById(r.getUserId());
            if (user != null) r.setUserName(user.getName());
        }
        return Result.success(internPage);
    }

    @PostMapping("/photo/upload")
    @Operation(summary = "上传实习照片")
    public Result<InternPhoto> uploadPhoto(@RequestBody Map<String, Object> request) {
        Long checkinId = Long.valueOf(request.get("checkinId").toString());
        String imageUrl = (String) request.get("imageUrl");
        InternPhoto photo = new InternPhoto();
        photo.setCheckinId(checkinId);
        photo.setImageUrl(imageUrl);
        internPhotoService.save(photo);
        return Result.success("上传成功", photo);
    }

    @DeleteMapping("/photo/{id}")
    @Operation(summary = "删除实习照片")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        InternPhoto photo = internPhotoService.getById(id);
        if (photo == null) {
            return Result.error(404, "照片不存在");
        }
        internPhotoService.removeById(id);
        return Result.success("删除成功", null);
    }
}
