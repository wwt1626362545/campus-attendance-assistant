package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.TokenContextHolder;
import com.educheck.config.CacheService;
import com.educheck.entity.Announcement;
import com.educheck.entity.User;
import com.educheck.service.AnnouncementService;
import com.educheck.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "公告管理", description = "公告发布与查看接口")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final TokenContextHolder tokenContextHolder;
    private final UserService userService;
    private final CacheService cacheService;

    @GetMapping
    @Operation(summary = "获取公告列表(分页，首页第一页缓存60秒)")
    public Result<Page<Announcement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        // 非搜索首页列表缓存60秒
        if (page == 1 && (keyword == null || keyword.isEmpty())) {
            String cacheKey = "announce:list:" + (type != null ? type : "all");
            Page<Announcement> cached = cacheService.get(cacheKey, Page.class);
            if (cached != null) return Result.success(cached);
        }
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getIsTop, Announcement::getCreatedAt);
        if (type != null && !"all".equals(type)) {
            wrapper.eq(Announcement::getType, type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Announcement::getTitle, keyword)
                    .or().like(Announcement::getSummary, keyword));
        }
        Page<Announcement> listResult = announcementService.page(new Page<>(page, size), wrapper);
        listResult.getRecords().forEach(this::enrichAnnounce);
        // 首页列表写入缓存
        if (page == 1 && (keyword == null || keyword.isEmpty())) {
            cacheService.set("announce:list:" + (type != null ? type : "all"), listResult, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
        return Result.success(listResult);
    }

    @GetMapping("/top")
    @Operation(summary = "获取置顶公告（Redis缓存30秒）")
    public Result<Announcement> getTop() {
        Announcement cached = cacheService.get("announce:top", Announcement.class);
        if (cached != null) return Result.success(cached);

        Announcement top = announcementService.lambdaQuery()
                .eq(Announcement::getStatus, 1)
                .eq(Announcement::getIsTop, 1)
                .orderByDesc(Announcement::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (top != null) {
            enrichAnnounce(top);
            cacheService.set("announce:top", top, 30, java.util.concurrent.TimeUnit.SECONDS);
        }
        return Result.success(top);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情")
    public Result<Announcement> detail(@PathVariable Long id) {
        Announcement announce = announcementService.getById(id);
        if (announce == null) {
            return Result.error(404, "公告不存在");
        }
        // 阅读次数+1
        announce.setReadCount(announce.getReadCount() != null ? announce.getReadCount() + 1 : 1);
        Announcement updateStats = new Announcement();
        updateStats.setId(id);
        updateStats.setReadCount(announce.getReadCount());
        announcementService.updateById(updateStats);
        // 透传字段给前端
        announce.setTypeName(getTypeName(announce.getType()));
        announce.setDate(announce.getCreatedAt() != null
                ? announce.getCreatedAt().toLocalDate().toString() : "");
        announce.setSource(announce.getDepartment());
        return Result.success(announce);
    }

    /** 填充前端展示暂态字段 */
    private void enrichAnnounce(Announcement a) {
        a.setTypeName(getTypeName(a.getType()));
        if (a.getCreatedAt() != null) {
            a.setDate(a.getCreatedAt().toLocalDate().toString());
        }
        a.setSource(a.getDepartment());
    }

    private String getTypeName(String type) {
        return switch (type != null ? type : "") {
            case "notice" -> "通知公告";
            case "activity" -> "活动预告";
            case "academic" -> "学术讲座";
            case "policy" -> "思政教育";
            default -> "综合公告";
        };
    }

    /** 清除公告缓存 */
    private void clearAnnounceCache() {
        cacheService.delete("announce:top");
        cacheService.delete("announce:list:all");
        cacheService.delete("announce:list:notice");
        cacheService.delete("announce:list:activity");
        cacheService.delete("announce:list:academic");
        cacheService.delete("announce:list:policy");
    }

    @PostMapping
    @Operation(summary = "发布公告")
    public Result<Announcement> create(@RequestBody Announcement request) {
        Long userId = tokenContextHolder.requireCurrentUserId();
        User user = userService.getById(userId);
        request.setPublisherId(userId);
        request.setPublisher(user != null ? user.getName() : "");
        request.setStatus(1);
        announcementService.save(request);
        clearAnnounceCache();
        return Result.success("发布成功", request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新公告")
    public Result<Announcement> update(@PathVariable Long id, @RequestBody Announcement request) {
        Announcement existing = announcementService.getById(id);
        if (existing == null) {
            return Result.error(404, "公告不存在");
        }
        request.setId(id);
        announcementService.updateById(request);
        clearAnnounceCache();
        return Result.success("更新成功", announcementService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        Announcement existing = announcementService.getById(id);
        if (existing == null) {
            return Result.error(404, "公告不存在");
        }
        announcementService.removeById(id);
        clearAnnounceCache();
        return Result.success("删除成功", null);
    }

    @GetMapping("/teacher/list-all")
    @Operation(summary = "教师端获取全部公告列表")
    public Result<Page<Announcement>> teacherListAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreatedAt);
        Page<Announcement> teacherResult = announcementService.page(new Page<>(page, size), wrapper);
        teacherResult.getRecords().forEach(this::enrichAnnounce);
        return Result.success(teacherResult);
    }
}
