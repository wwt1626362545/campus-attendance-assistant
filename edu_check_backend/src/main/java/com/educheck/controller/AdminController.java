package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.config.CacheService;
import com.educheck.entity.User;
import com.educheck.service.*;
import com.educheck.entity.Leave;
import com.educheck.entity.RepairRecord;
import java.util.concurrent.TimeUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "管理员端用户管理接口")
public class AdminController {

    private final UserService userService;
    private final TokenContextHolder tokenContextHolder;
    private final LeaveService leaveService;
    private final com.educheck.service.RepairRecordService repairService;
    private final CacheService cacheService;

    /** 当前用户角色 */
    private String currentRole() {
        return tokenContextHolder.getCurrentRole();
    }

    /** 验证管理员身份 */
    private void requireAdmin() {
        if (!"admin".equals(currentRole())) {
            throw new com.educheck.common.ServiceException(403, "权限不足，需要管理员身份");
        }
    }

    /** 验证可用（管理员或教师） */
    private void requireAccess() {
        String role = currentRole();
        if (!"admin".equals(role) && !"teacher".equals(role)) {
            throw new com.educheck.common.ServiceException(403, "权限不足");
        }
    }

    /** 教师只能操作学生，管理员可操作全部 */
    private void checkUserAccess(User target) {
        if ("teacher".equals(currentRole()) && target != null && !"student".equals(target.getRole())) {
            throw new com.educheck.common.ServiceException(403, "教师只能管理学生账号");
        }
    }

    @GetMapping("/users")
    @Operation(summary = "分页查询用户（教师只看学生）")
    public Result<Page<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        requireAccess();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 教师只能看学生
        if ("teacher".equals(currentRole())) {
            wrapper.eq(User::getRole, "student");
        } else if (role != null && !"all".equals(role)) {
            wrapper.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getName, keyword)
                    .or().like(User::getStudentId, keyword));
        }
        wrapper.orderByAsc(User::getStudentId);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    @PostMapping("/user")
    @Operation(summary = "添加用户（教师只能添加学生）")
    public Result<User> addUser(@RequestBody User user) {
        requireAccess();
        // 教师只能创建学生账号
        if ("teacher".equals(currentRole()) && !"student".equals(user.getRole())) {
            return Result.error(403, "教师只能添加学生账号");
        }
        user.setPassword(user.getPassword() != null ? user.getPassword() : "123456");
        user.setStatus(1);
        userService.save(user);
        user.setPassword(null);
        return Result.success("创建成功", user);
    }

    @PutMapping("/user/{id}")
    @Operation(summary = "更新用户信息")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        requireAccess();
        User existing = userService.getById(id);
        if (existing == null) return Result.error(404, "用户不存在");
        checkUserAccess(existing);
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // preserve password if set
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        User updated = userService.getById(id);
        updated.setPassword(null);
        return Result.success("更新成功", updated);
    }

    @PutMapping("/user/{id}/toggle-status")
    @Operation(summary = "启用/禁用用户")
    public Result<User> toggleStatus(@PathVariable Long id) {
        requireAccess();
        User user = userService.getById(id);
        if (user == null) return Result.error(404, "用户不存在");
        checkUserAccess(user);
        user.setStatus(user.getStatus() != null && user.getStatus() == 1 ? 0 : 1);
        userService.updateById(user);
        user.setPassword(null);
        return Result.success(user.getStatus() == 1 ? "已启用" : "已禁用", user);
    }

    @DeleteMapping("/user/{id}")
    @Operation(summary = "删除用户（教师只能删学生）")
    public Result<Void> deleteUser(@PathVariable Long id) {
        requireAccess();
        User user = userService.getById(id);
        if (user == null) return Result.error(404, "用户不存在");
        checkUserAccess(user);
        userService.removeById(id);
        return Result.success("已删除", null);
    }

    @GetMapping("/stats")
    @Operation(summary = "系统统计概览")
    public Result<Map<String, Object>> stats() {
        requireAccess();
        long studentCount = userService.lambdaQuery().eq(User::getRole, "student").count();
        long teacherCount = userService.lambdaQuery().eq(User::getRole, "teacher").count();
        long activeCount = userService.lambdaQuery().eq(User::getStatus, 1).count();
        return Result.success(Map.of(
                "studentCount", studentCount,
                "teacherCount", teacherCount,
                "activeCount", activeCount,
                "totalCount", studentCount + teacherCount
        ));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "PC仪表盘数据（Redis缓存30秒）")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> dashboard() {
        requireAccess();
        // 尝试从缓存读取
        Map<String, Object> data = cacheService.get("admin:dashboard", Map.class);
        if (data != null) return Result.success(data);

        data = new HashMap<>();

        // 用户统计
        long studentCount = userService.lambdaQuery().eq(User::getRole, "student").count();
        long teacherCount = userService.lambdaQuery().eq(User::getRole, "teacher").count();
        data.put("studentCount", studentCount);
        data.put("teacherCount", teacherCount);

        // 请假统计
        List<Leave> allLeaves = leaveService.list();
        long pendingLeave = allLeaves.stream().filter(l -> "pending".equals(l.getStatus())).count();
        long approvedLeave = allLeaves.stream().filter(l -> "approved".equals(l.getStatus())).count();
        long rejectedLeave = allLeaves.stream().filter(l -> "rejected".equals(l.getStatus())).count();
        data.put("leavePending", pendingLeave);
        data.put("leaveApproved", approvedLeave);
        data.put("leaveRejected", rejectedLeave);
        data.put("leaveTotal", allLeaves.size());

        // 报修统计
        long repairTotal = repairService.count();
        long repairPending = repairService.lambdaQuery().eq(RepairRecord::getStatus, "pending").count();
        long repairDone = repairService.lambdaQuery().eq(RepairRecord::getStatus, "done").count();
        data.put("repairTotal", repairTotal);
        data.put("repairPending", repairPending);
        data.put("repairDone", repairDone);

        // 写入Redis缓存，30秒过期
        cacheService.set("admin:dashboard", data, 30, TimeUnit.SECONDS);
        return Result.success(data);
    }
}
