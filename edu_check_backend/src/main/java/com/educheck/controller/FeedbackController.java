package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.dto.FeedbackSubmitRequest;
import com.educheck.entity.Feedback;
import com.educheck.entity.User;
import com.educheck.service.FeedbackService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "意见反馈", description = "意见反馈接口")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final TokenContextHolder tokenContextHolder;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "提交反馈")
    public Result<Feedback> submit(@RequestBody FeedbackSubmitRequest request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setContent(request.getContent());
        feedback.setContact(request.getContact());
        feedback.setStatus("pending");
        feedbackService.save(feedback);
        return Result.success("提交成功", feedback);
    }

    @GetMapping
    @Operation(summary = "获取我的反馈列表")
    public Result<Page<Feedback>> myFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreatedAt);
        return Result.success(feedbackService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/teacher")
    @Operation(summary = "教师端查看所有反馈")
    public Result<Page<Feedback>> teacherFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .orderByDesc(Feedback::getCreatedAt);
        return Result.success(feedbackService.page(new Page<>(page, size), wrapper));
    }

    @PutMapping("/reply/{id}")
    @Operation(summary = "教师回复反馈")
    public Result<Feedback> reply(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) return Result.error(404, "反馈不存在");
        feedback.setReply(request.get("reply"));
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus("resolved");
        feedbackService.updateById(feedback);
        return Result.success("回复成功", feedback);
    }
}
