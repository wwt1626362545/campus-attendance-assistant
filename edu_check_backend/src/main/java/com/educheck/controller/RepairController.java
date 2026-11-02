package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.entity.RepairRecord;
import com.educheck.entity.User;
import com.educheck.service.RepairRecordService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/repair")
@RequiredArgsConstructor
@Tag(name = "宿舍报修", description = "宿舍报修申请与处理")
public class RepairController {

    private final RepairRecordService repairService;
    private final TokenContextHolder tokenContextHolder;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "提交报修申请")
    public Result<RepairRecord> submit(@RequestBody Map<String, Object> request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        User user = userService.getById(userId);
        RepairRecord r = new RepairRecord();
        r.setUserId(userId);
        r.setStudentName(user != null ? user.getName() : "");
        r.setStudentId(user != null ? user.getStudentId() : "");
        r.setDormBuilding((String) request.get("dormBuilding"));
        r.setDormRoom((String) request.get("dormRoom"));
        r.setCategory((String) request.getOrDefault("category", "other"));
        r.setDescription((String) request.get("description"));
        r.setImages((String) request.get("images"));
        r.setPhone((String) request.get("phone"));
        r.setStatus("pending");
        repairService.save(r);
        return Result.success("提交成功", r);
    }

    @GetMapping
    @Operation(summary = "获取我的报修列表")
    public Result<Page<RepairRecord>> myList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        return Result.success(repairService.page(new Page<>(page, size),
                new LambdaQueryWrapper<RepairRecord>()
                        .eq(RepairRecord::getUserId, userId)
                        .orderByDesc(RepairRecord::getCreatedAt)));
    }

    @GetMapping("/list")
    @Operation(summary = "管理员获取全部报修")
    public Result<Page<RepairRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<RepairRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !"all".equals(status)) {
            wrapper.eq(RepairRecord::getStatus, status);
        }
        wrapper.orderByDesc(RepairRecord::getCreatedAt);
        return Result.success(repairService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/stats")
    @Operation(summary = "报修统计")
    public Result<Map<String, Object>> stats() {
        long total = repairService.count();
        long pending = repairService.lambdaQuery().eq(RepairRecord::getStatus, "pending").count();
        long done = repairService.lambdaQuery().eq(RepairRecord::getStatus, "done").count();
        return Result.success(Map.of("total", total, "pending", pending, "done", done));
    }

    @PutMapping("/handle/{id}")
    @Operation(summary = "处理报修")
    public Result<RepairRecord> handle(@PathVariable Long id, @RequestBody Map<String, String> request) {
        RepairRecord r = repairService.getById(id);
        if (r == null) return Result.error(404, "报修不存在");
        Long userId = tokenContextHolder.requireCurrentUserId();
        r.setStatus(request.getOrDefault("status", "done"));
        r.setReply(request.get("reply"));
        r.setHandlerId(userId);
        r.setHandleTime(LocalDateTime.now());
        repairService.updateById(r);
        return Result.success("操作成功", r);
    }
}
