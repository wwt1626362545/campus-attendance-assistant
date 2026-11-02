package com.educheck.controller;

import com.educheck.common.JwtUtil;
import com.educheck.common.RateLimiter;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.LoginRequest;
import com.educheck.entity.User;
import com.educheck.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "登录注册接口")
public class AuthController {

    /** 提供头像文件访问 */
    @GetMapping("/avatar-file")
    public org.springframework.http.ResponseEntity<byte[]> getAvatarFile(@RequestParam String name) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", "face").toAbsolutePath().resolve(name);
            if (!filePath.toFile().exists()) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            byte[] data = java.nio.file.Files.readAllBytes(filePath);
            String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "jpg";
            String contentType = "image/" + ("png".equals(ext) ? "png" : "jpg");
            return org.springframework.http.ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .body(data);
        } catch (Exception e) {
            log.error("读取头像失败", e);
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenContextHolder tokenContextHolder;
    private final RateLimiter rateLimiter;

    @PostMapping("/avatar")
    @Operation(summary = "上传头像")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Long userId = tokenContextHolder.requireCurrentUserId();
            String name = file.getOriginalFilename();
            String ext = "jpg";
            if (name != null && name.contains(".")) ext = name.substring(name.lastIndexOf(".") + 1);
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + ext;
            // 保存到项目根目录下的 uploads/face/（与 WebMvcConfig 映射路径一致）
            java.nio.file.Path dirPath = java.nio.file.Paths.get("uploads", "face").toAbsolutePath();
            java.io.File dir = dirPath.toFile();
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new java.io.File(dir, fileName));
            String avatarUrl = "/uploads/face/" + fileName;
            User update = new User();
            update.setId(userId);
            update.setAvatar(avatarUrl);
            userService.updateById(update);
            return Result.success(Map.of("avatar", avatarUrl));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（将 token 加入黑名单）")
    public Result<Void> logout() {
        tokenContextHolder.blacklistCurrentToken();
        return Result.success("已退出登录", null);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        log.info("登录请求: studentId={}", request.getStudentId());

        // 登录限流：同一学号每分钟最多5次
        if (!rateLimiter.tryAcquire("rate:login:" + request.getStudentId(), 5, 1, java.util.concurrent.TimeUnit.MINUTES)) {
            return Result.error(429, "登录过于频繁，请1分钟后再试");
        }

        // 按学号查用户 → 校验密码/状态 → 签发 JWT token → 返回用户信息

        User user = userService.lambdaQuery()
                .eq(User::getStudentId, request.getStudentId())
                .oneOpt()
                .orElse(null);

        // 用户不存在或密码错误均返回同一提示，防止枚举账号
        if (user == null) {
            log.warn("登录失败: 用户不存在 studentId={}", request.getStudentId());
            return Result.error(401, "学号或密码错误");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            log.warn("登录失败: 密码不匹配");
            return Result.error(401, "学号或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("name", user.getName());
        data.put("role", user.getRole());
        data.put("studentId", user.getStudentId());

        return Result.success("登录成功", data);
    }
}

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息接口")
class UserController {

    private final UserService userService;
    private final TokenContextHolder tokenContextHolder;

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息")
    public Result<User> getProfile() {
        Long userId = tokenContextHolder.requireCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) return Result.error(404, "用户不存在");
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新用户信息")
    public Result<User> updateProfile(@RequestBody User user) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        user.setId(userId);
        // 不允许通过此接口修改密码，防止绕过
        user.setPassword(null);
        userService.updateById(user);
        User updated = userService.getById(userId);
        updated.setPassword(null);
        return Result.success(updated);
    }
}
