package com.educheck.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.service.ClassCheckinService;
import com.educheck.vo.ClassCheckinHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classCheckin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "签到历史", description = "课堂签到历史记录查询接口")
public class ClassCheckinHistoryController {

    private final ClassCheckinService classCheckinService;

    @GetMapping("/history/short")
    @Operation(summary = "首页近期签到记录", description = "根据用户ID查询最近5条签到记录，LEFT JOIN course带出课程名称，按日期和签到时间倒序")
    public Result<IPage<ClassCheckinHistoryVO>> shortHistory(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("userId") Long userId) {
        log.info("[签到历史] shortHistory 被调用: userId={}", userId);
        IPage<ClassCheckinHistoryVO> page = classCheckinService.selectShortHistory(userId);
        return Result.success(page);
    }

    @GetMapping("/history/all")
    @Operation(summary = "全部签到历史分页", description = "根据用户ID分页查询全部签到记录，LEFT JOIN course带出课程名称，按日期和签到时间倒序")
    public Result<IPage<ClassCheckinHistoryVO>> allHistory(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("userId") Long userId,
            @Parameter(description = "页码，从1开始")
            @RequestParam(value = "page", defaultValue = "1") Long page,
            @Parameter(description = "每页条数")
            @RequestParam(value = "size", defaultValue = "10") Long size) {
        log.info("[签到历史] allHistory 被调用: userId={}, page={}, size={}", userId, page, size);
        Page<ClassCheckinHistoryVO> pageParam = new Page<>(page, size);
        IPage<ClassCheckinHistoryVO> result = classCheckinService.selectHistoryPage(pageParam, userId);
        return Result.success(result);
    }
}
