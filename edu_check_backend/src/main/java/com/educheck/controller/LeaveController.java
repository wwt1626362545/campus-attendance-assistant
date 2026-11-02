package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.LeaveApplyRequest;
import com.educheck.dto.LeaveApproveRequest;
import com.educheck.entity.Leave;
import com.educheck.entity.User;
import com.educheck.service.LeaveService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "请假管理", description = "请假申请与审批接口")
public class LeaveController {

    private final LeaveService leaveService;
    private final TokenContextHolder tokenContextHolder;
    private final UserService userService;

    /** 填充前端展示用的暂态字段 */
    private void enrichLeave(Leave l) {
        // 类型名称和样式
        if (l.getType() != null) {
            switch (l.getType()) {
                case "sick" -> { l.setTypeName("病假"); l.setTypeClass("sick"); }
                case "personal" -> { l.setTypeName("事假"); l.setTypeClass("personal"); }
                case "official" -> { l.setTypeName("公假"); l.setTypeClass("official"); }
                case "annual" -> { l.setTypeName("年假"); l.setTypeClass("annual"); }
                default -> { l.setTypeName("其他"); l.setTypeClass("other"); }
            }
        }
        // 状态名称和样式
        if (l.getStatus() != null) {
            switch (l.getStatus()) {
                case "pending" -> { l.setStatusName("审批中"); l.setStatusClass("pending"); l.setStatusDesc("请耐心等待教师审批"); }
                case "approved" -> { l.setStatusName("已通过"); l.setStatusClass("approved"); l.setStatusDesc("教师已批准此请假"); }
                case "rejected" -> { l.setStatusName("已驳回"); l.setStatusClass("rejected"); l.setStatusDesc("教师已驳回此请假"); }
                default -> { l.setStatusName("未知"); l.setStatusClass("other"); }
            }
        }
        // 请假天数（跨天算1天，不满1天也算1天）
        if (l.getStartDate() != null && l.getEndDate() != null) {
            long daysBetween = ChronoUnit.DAYS.between(l.getStartDate().toLocalDate(), l.getEndDate().toLocalDate()) + 1;
            l.setDays(daysBetween);
        } else {
            l.setDays(0L);
        }
        // 提交时间
        if (l.getCreatedAt() != null) {
            l.setSubmitTime(l.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 申请人信息
        if (l.getUserId() != null) {
            User student = userService.getById(l.getUserId());
            if (student != null) {
                l.setStudentName(student.getName());
                l.setStudentId(student.getStudentId());
            }
        }
        // 审批人信息
        if (l.getApproverId() != null) {
            User approverUser = userService.getById(l.getApproverId());
            if (approverUser != null) {
                l.setApprover(approverUser.getName());
            }
        }
        // 审批意见
        if (l.getStatus() != null && "rejected".equals(l.getStatus())) {
            l.setApproveComment(l.getRejectReason() != null ? l.getRejectReason() : "未说明原因");
        } else if (l.getStatus() != null && "approved".equals(l.getStatus())) {
            l.setApproveComment("同意请假申请");
        }
        }
    }

    @GetMapping
    @Operation(summary = "获取当前用户请假列表")
    public Result<Page<Leave>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<Leave>()
                .eq(Leave::getUserId, userId)
                .orderByDesc(Leave::getCreatedAt);
        if (type != null && !"all".equals(type)) {
            wrapper.eq(Leave::getStatus, type);
        }
        Page<Leave> result = leaveService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::enrichLeave);
        return Result.success(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取当前用户请假统计")
    public Result<Map<String, Object>> stats() {
        Long userId = tokenContextHolder.requireCurrentUserId();
        List<Leave> all = leaveService.lambdaQuery().eq(Leave::getUserId, userId).list();
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("pending", all.stream().filter(l -> "pending".equals(l.getStatus())).count());
        data.put("approved", all.stream().filter(l -> "approved".equals(l.getStatus())).count());
        data.put("rejected", all.stream().filter(l -> "rejected".equals(l.getStatus())).count());
        return Result.success(data);
    }

    @PostMapping
    @Operation(summary = "申请请假")
    public Result<Leave> apply(@RequestBody LeaveApplyRequest request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        Leave leave = new Leave();
        leave.setUserId(userId);
        leave.setType(request.getType());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason());
        leave.setStatus("pending");
        leaveService.save(leave);
        return Result.success("提交成功", leave);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取请假详情")
    public Result<Leave> detail(@PathVariable Long id) {
        Leave leave = leaveService.getById(id);
        if (leave == null) {
            return Result.error(404, "请假记录不存在");
        }
        enrichLeave(leave);
        return Result.success(leave);
    }

    // ==================== 教师端接口 ====================

    @GetMapping("/teacher/pending")
    @Operation(summary = "教师端获取待审批请假列表")
    public Result<Page<Leave>> teacherPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<Leave>()
                .eq(Leave::getStatus, "pending")
                .orderByDesc(Leave::getCreatedAt);
        Page<Leave> result = leaveService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::enrichLeave);
        return Result.success(result);
    }

    @GetMapping("/teacher/all")
    @Operation(summary = "教师端获取全部请假列表")
    public Result<Page<Leave>> teacherAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<Leave>()
                .orderByDesc(Leave::getCreatedAt);
        if (status != null && !"all".equals(status)) {
            wrapper.eq(Leave::getStatus, status);
        }
        Page<Leave> result = leaveService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::enrichLeave);
        return Result.success(result);
    }

    @GetMapping("/teacher/stats")
    @Operation(summary = "教师端获取请假统计")
    public Result<Map<String, Object>> teacherStats() {
        List<Leave> all = leaveService.list();
        Map<String, Object> data = new HashMap<>();
        data.put("total", all.size());
        data.put("pending", all.stream().filter(l -> "pending".equals(l.getStatus())).count());
        data.put("approved", all.stream().filter(l -> "approved".equals(l.getStatus())).count());
        data.put("rejected", all.stream().filter(l -> "rejected".equals(l.getStatus())).count());
        return Result.success(data);
    }

    @PostMapping("/teacher/approve/{id}")
    @Operation(summary = "教师审批请假")
    public Result<Leave> approve(@PathVariable Long id, @RequestBody LeaveApproveRequest request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        Leave leave = leaveService.getById(id);
        if (leave == null) {
            return Result.error(404, "请假记录不存在");
        }
        if (!"pending".equals(leave.getStatus())) {
            return Result.error("该请假已经审批过了");
        }
        if (Boolean.TRUE.equals(request.getApproved())) {
            leave.setStatus("approved");
        } else {
            leave.setStatus("rejected");
            leave.setRejectReason(request.getRejectReason());
        }
        leave.setApproverId(userId);
        leave.setApproveTime(LocalDateTime.now());
        leaveService.updateById(leave);
        enrichLeave(leave);
        return Result.success("操作成功", leave);
    }
}
