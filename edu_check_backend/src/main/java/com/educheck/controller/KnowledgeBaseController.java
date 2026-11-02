package com.educheck.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.common.Result;
import com.educheck.common.WordTokenizer;
import com.educheck.entity.KnowledgeBase;
import com.educheck.entity.UnansweredLog;
import com.educheck.mapper.KnowledgeBaseMapper;
import com.educheck.service.DeepSeekService;
import com.educheck.service.KnowledgeBaseService;
import com.educheck.service.UnansweredLogService;
import com.educheck.common.TokenContextHolder;
import com.educheck.config.CacheService;
import com.educheck.entity.Leave;
import com.educheck.entity.Announcement;
import com.educheck.entity.DormCheckin;
import com.educheck.entity.ClassCheckin;
import com.educheck.entity.Course;
import com.educheck.service.LeaveService;
import com.educheck.service.AnnouncementService;
import com.educheck.service.ClassCheckinService;
import com.educheck.service.DormCheckinService;
import com.educheck.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "智能问答", description = "知识库与智能问答接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DeepSeekService deepSeekService;
    private final UnansweredLogService unansweredLogService;
    private final TokenContextHolder tokenContextHolder;
    private final LeaveService leaveService;
    private final AnnouncementService announcementService;
    private final ClassCheckinService classCheckinService;
    private final DormCheckinService dormCheckinService;
    private final CourseService courseService;
    private final CacheService cacheService;
    private static final List<String> CAMPUS_KEYWORDS = List.of(
            "打卡", "考勤", "签到", "请假", "公告"
            , "课程", "实习",
            "查寝", "宿舍", "人脸", "权限", "账号", "密码",
            "教师", "学生", "数据库", "接口", "API", "功能", "页面",
            "重置", "修改", "添加", "删除", "查看", "切换",
            "token", "JWT", "角色", "成绩", "课表", "教室",
            "校园", "教务", "前端", "后端",
            "小程序", "通信", "请求", "网络",
            "手机", "微信", "审批", "反馈",
            "动态码", "意见", "录入",
            "上课", "课表", "明天", "今天"
    );
    @PostMapping("/search")
    @Operation(summary = "智能问答回答用户问题")
    public Result<Map<String,Object>> search(
            // 用map对象接收前端传递的所有参数
            @RequestBody Map<String,Object> request
    ){
        // 从请求对象里面提取用户的问题
        // trim() 去掉收尾空格
        String question=((String) request.getOrDefault("question", "")).trim();
        // 提取用户问题类型
        String category=(String) request.getOrDefault("category", "all");
// 取出之前聊天的历史记录
        List<Map<String,String>> history=
                (List<Map<String, String>>) request.getOrDefault("history",List.of());
        // 如果前端提交的问题是空  也对应返回一个空的结果
        if(question.isEmpty()){
            return Result.success(Map.of("answer", "", "sources", List.of()));
        }

// ===== 智能发布公告流程（放在请假之前，避免公告内容含"请假"被拦截） =====
        boolean explicitAnnounce = question.contains("发布公告") || question.contains("发公告") || question.contains("创建公告") || question.contains("新建公告");
        boolean announceFlowInProgress = false;
        if (history != null) {
            for (int j = Math.max(0, history.size() - 4); j < history.size(); j++) {
                String role = history.get(j).get("role");
                String content = history.get(j).get("content");
                if ("user".equals(role)) continue;
                if (content != null && (content.contains("公告标题") || content.contains("公告类型") || content.contains("公告摘要") || content.contains("公告正文") || content.contains("发布部门"))) {
                    announceFlowInProgress = true; break;
                }
            }
        }
        if (explicitAnnounce || announceFlowInProgress) {
            String role = tokenContextHolder.getCurrentRole();
            if (!"teacher".equals(role) && !"admin".equals(role)) {
                return Result.success(Map.of("answer", "只有教师和管理员才能发布公告。", "sources", List.of()));
            }
            if (question.contains("|")) {
                String[] parts = question.split("\\|");
                if (parts.length >= 4) {
                    String typeRaw = parts[1].trim();
                    String type = typeRaw.contains("通知") || "notice".equals(typeRaw) ? "notice"
                            : typeRaw.contains("活动") || "activity".equals(typeRaw) ? "activity"
                            : typeRaw.contains("学术") || "academic".equals(typeRaw) ? "academic"
                            : typeRaw.contains("思政") || "policy".equals(typeRaw) ? "policy" : "notice";
                    Announcement ann = new Announcement();
                    ann.setTitle(parts[0].trim());
                    ann.setType(type);
                    ann.setSummary(parts[2].trim());
                    ann.setContent(parts[3].trim());
                    ann.setDepartment(parts.length >= 5 ? parts[4].trim() : "教务处");
                    ann.setStatus(1);
                    announcementService.save(ann);
                    return Result.success(Map.of("answer", "✅ 公告《" + ann.getTitle() + "》已发布！\n📌 前往「公告」Tab 可查看", "sources", List.of()));
                }
            }
            return Result.success(Map.of("answer",
                    "📢 请按以下格式发送（用 | 分隔）：\n标题 | 类型 | 摘要 | 正文 | 发布部门(可选)\n\n" +
                    "类型可选：通知公告 / 活动预告 / 学术讲座 / 思政教育\n\n" +
                    "例如：\n期末考试安排 | 通知公告 | 本学期考试安排 | 请查看详情 | 教务处",
                    "sources", List.of()));
        }

// ===== 智能请假流程（提前到关键词过滤之前，避免"病假""明天"等被拦截） =====
        boolean explicitLeave = question.contains("帮我请假") || question.contains("我要请假") || question.contains("请个假") || question.contains("提交请假") || question.contains("请假") || question.contains("请病假") || question.contains("请事假") || question.contains("申请请假") || question.contains("想请假");
        boolean leaveFlowInProgress = false;
        if (history != null) {
            // 取最后一条AI回复看是否在请假流程中
            String lastAiContent = null;
            for (int j = history.size() - 1; j >= Math.max(0, history.size() - 4); j--) {
                if (!"user".equals(history.get(j).get("role"))) {
                    lastAiContent = history.get(j).get("content");
                    break;
                }
            }
            // 如果AI最后一条提到"请假""类型""时间""事由""理由"，当前用户回复合理就继续
            if (lastAiContent != null && (lastAiContent.contains("请假") || lastAiContent.contains("类型") || lastAiContent.contains("时间") || lastAiContent.contains("事由") || lastAiContent.contains("理由"))) {
                // 接在请假对话后面的合理回复：请假相关字眼、时间词、理由词
                if (question.contains("假") || question.contains("病") || question.contains("事") || question.contains("时间") || question.contains("到") || question.contains("感冒") || question.contains("因为") || question.contains("发烧") || question.contains("疼") || question.contains("需要")) {
                    leaveFlowInProgress = true;
                }
            }
        }
        if (explicitLeave || leaveFlowInProgress) {
            java.util.Map<String,Object> act = deepSeekService.detectAction(question, history);
            if (act != null && "apply_leave".equals(act.get("action"))) {
                return handleLeaveApply(act);
            } else if (act != null && "clarify".equals(act.get("action"))) {
            } else if (explicitLeave || leaveFlowInProgress) {
                String allUserMsgs = question;
                if (history != null) {
                    for (int h = 0; h < history.size(); h++) { if ("user".equals(history.get(h).get("role")) && history.get(h).get("content") != null) allUserMsgs += " " + history.get(h).get("content"); } }
                String fbType = allUserMsgs.contains("病") ? "sick" : allUserMsgs.contains("事") ? "personal" : allUserMsgs.contains("公") ? "official" : allUserMsgs.contains("年") ? "annual" : null;
                if (fbType == null) { return Result.success(java.util.Map.of("answer", "请问请什么类型的假？（病假/事假/公假/年假）", "sources", java.util.List.of())); }
                boolean hasTime = allUserMsgs.contains("今天") || allUserMsgs.contains("明天") || allUserMsgs.contains("月") || allUserMsgs.contains("日") || allUserMsgs.contains("周");
                if (!hasTime) { return Result.success(java.util.Map.of("answer", "开始时间和结束时间？(例如: 今天下午到明天)", "sources", java.util.List.of())); }
                if (allUserMsgs.length() < 12) { return Result.success(java.util.Map.of("answer", "请假事由是什么？", "sources", java.util.List.of())); }
                try {
                    com.educheck.entity.Leave lv = new com.educheck.entity.Leave();
                    lv.setUserId(tokenContextHolder.requireCurrentUserId());
                    lv.setType(fbType);
                    // 根据关键词解析日期（支持"今天到后天""明天"等）
                    java.time.LocalDate todayL = java.time.LocalDate.now();
                    java.time.LocalDate sD = todayL, eD = todayL.plusDays(1);
                    int rangeIdx = Math.max(allUserMsgs.indexOf("到"), allUserMsgs.indexOf("至"));
                    if (rangeIdx >= 0) {
                        // 有范围："今天到后天" → start=今天 end=后天
                        String startP = allUserMsgs.substring(0, rangeIdx);
                        String endP = allUserMsgs.substring(rangeIdx);
                        if (endP.contains("后天")) eD = todayL.plusDays(2);
                        else if (endP.contains("明天")) eD = todayL.plusDays(1);
                        if (startP.contains("后天")) sD = todayL.plusDays(2);
                        else if (startP.contains("明天")) sD = todayL.plusDays(1);
                    } else {
                        // 单天："后天" → start=end=后天
                        if (allUserMsgs.contains("后天")) { sD = todayL.plusDays(2); eD = todayL.plusDays(2); }
                        else if (allUserMsgs.contains("明天")) { sD = todayL.plusDays(1); eD = todayL.plusDays(1); }
                    }
                    lv.setStartDate(sD.atStartOfDay());
                    lv.setEndDate(eD.atTime(18, 0));
                    lv.setReason(allUserMsgs.length() > 60 ? allUserMsgs.substring(0, 60) : allUserMsgs);
                    lv.setStatus("pending");
                    leaveService.save(lv);
                    return Result.success(java.util.Map.of("answer", "✅ 请假申请已提交！（通过智能助手）\n\n📌 前往底部「请假」Tab 可查看审批进度", "sources", java.util.List.of()));
                } catch (Exception ex) {
                    log.error("请假兜底失败", ex);
                    return Result.success(java.util.Map.of("answer", "请假提交失败，请稍后重试或手动提交。", "sources", java.util.List.of()));
                }
            }
        }
// 校园关键词过滤
        boolean isCampusRelated = CAMPUS_KEYWORDS.stream().anyMatch(k -> question.contains(k));

// ===== 先查询知识库（不限制 campus 关键词，导入的条目也能匹配） =====
        boolean kbMatched = false;
        {
            LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
            String cat = category;
            if(!"all".equals(cat)){
                wrapper.eq(KnowledgeBase::getCategory, cat);
            }
            wrapper.orderByAsc(KnowledgeBase::getSortOrder);
            List<KnowledgeBase> allKb = knowledgeBaseService.list(wrapper);
            String q = question.toLowerCase();
            List<String> questionWords = WordTokenizer.segment(q);
            List<Map<String,Object>> scored = new ArrayList<>();
            for(KnowledgeBase kb : allKb){
                int score = 0;
                String kw = (kb.getKeywords()!=null?kb.getKeywords():"").toLowerCase();
                String syn = (kb.getSynonyms()!=null?kb.getSynonyms():"").toLowerCase();
                String kq = (kb.getQuestion()!=null?kb.getQuestion():"").toLowerCase();
                String ka = (kb.getAnswer()!=null?kb.getAnswer():"").toLowerCase();
                for (String keyword : kw.split(",")){
                    keyword = keyword.trim();
                    if(!keyword.isEmpty() && q.contains(keyword)) score += 10;
                }
                if(!syn.isEmpty()){
                    for(String group : syn.split("\\|")){
                        group = group.trim();
                        if(!group.contains("=")) continue;
                        String[] parts = group.split("=", 2);
                        for(String s : parts[1].split(",")){
                            s = s.trim();
                            if(!s.isEmpty() && q.contains(s)) score += 8;
                        }
                    }
                }
                for(String w : questionWords){
                    if(kq.contains(w)) score += 3;
                    if(ka.contains(w)) score += 1;
                }
                // 不加入排序权重，避免旧条目永远压过导入条目
                if(score > 0){
                    Map<String,Object> item = new HashMap<>();
                    item.put("id", kb.getId());
                    item.put("category", kb.getCategory());
                    item.put("question", kb.getQuestion());
                    item.put("answer", kb.getAnswer());
                    item.put("score", score);
                    scored.add(item);
                }
            }
            scored.sort((a, b) -> (int) b.get("score") - (int) a.get("score"));
            List<Map<String,Object>> top = new ArrayList<>();
            if(!scored.isEmpty()){
                int topScore = (int)scored.get(0).get("score");
                for(Map<String,Object> item : scored){
                    if(top.size() >= 2) break;
                    if((int)item.get("score") >= topScore * 0.6) top.add(item);
                }
            }
            // 精确匹配：用户问题与某条知识库问题完全一致，直接返回
            Map<String,Object> exactResult = null;
            for (KnowledgeBase kb : allKb) {
                if (kb.getQuestion() != null && kb.getQuestion().trim().equals(question.trim())) {
                    exactResult = new HashMap<>();
                    exactResult.put("answer", kb.getAnswer());
                    exactResult.put("sources", List.of(Map.of("question", kb.getQuestion(), "answer", kb.getAnswer())));
                    break;
                }
            }
            if (exactResult != null) {
                kbMatched = true;
                return Result.success(exactResult);
            }
            // 得分>20（多个关键词/分词匹配）才认为真正命中，否则交给AI分析
            if(!top.isEmpty() && (int)top.get(0).get("score") > 20){
                kbMatched = true;
                top.forEach(item -> item.remove("score"));
                top.forEach(item -> item.remove("score"));
                String llmAnswer = null;
                List<KnowledgeBase> matchedKb = top.stream().map(m -> {
                    KnowledgeBase kbe = new KnowledgeBase();
                    kbe.setId(((Number)m.get("id")).longValue());
                    kbe.setCategory((String) m.get("category"));
                    kbe.setQuestion((String) m.get("question"));
                    kbe.setAnswer((String) m.get("answer"));
                    return kbe;
                }).collect(Collectors.toList());
                llmAnswer = deepSeekService.ask(question, matchedKb, history);
                Map<String,Object> result = new HashMap<>();
                if(llmAnswer != null){
                    result.put("answer", llmAnswer);
                } else if(!top.isEmpty()){
                    result.put("answer", top.get(0).get("answer"));
                }
                result.put("sources", top);
                return Result.success(result);
            }
        }

// ===== 智能分析（仅当知识库未匹配时） =====
        String lowerQ = question.toLowerCase();
        if (!(explicitLeave || leaveFlowInProgress) && isCampusRelated) {
            // 查询最新公告
            if (lowerQ.contains("公告") || lowerQ.contains("通知") || lowerQ.contains("最新消息")) {
                try {
                    List<Announcement> anns = announcementService.lambdaQuery()
                            .eq(Announcement::getStatus, 1)
                            .orderByDesc(Announcement::getCreatedAt)
                            .last("LIMIT 5").list();
                    if (!anns.isEmpty()) {
                        StringBuilder sb = new StringBuilder("📋 最新公告：\n");
                        for (Announcement a : anns) {
                            sb.append("• ").append(a.getTitle())
                              .append(" (").append(a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : "").append(")\n");
                        }
                        return Result.success(Map.of("answer", sb.toString(), "sources", List.of()));
                    }
                } catch (Exception e) { log.warn("公告查询失败", e); }
            }

            // 查询考勤/打卡情况（含近期记录）
            if (lowerQ.contains("考勤") || lowerQ.contains("打卡记录") || lowerQ.contains("签到记录") || lowerQ.contains("出勤") || (lowerQ.contains("打卡") && lowerQ.contains("历史"))) {
                try {
                    Long userId = tokenContextHolder.requireCurrentUserId();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    long classToday = classCheckinService.lambdaQuery()
                            .eq(ClassCheckin::getUserId, userId)
                            .eq(ClassCheckin::getDate, today).count();
                    long dormToday = dormCheckinService.lambdaQuery()
                            .eq(DormCheckin::getUserId, userId)
                            .eq(DormCheckin::getDate, today).count();
                    // 最近5条上课记录
                    List<ClassCheckin> recentClasses = classCheckinService.lambdaQuery()
                            .eq(ClassCheckin::getUserId, userId)
                            .orderByDesc(ClassCheckin::getDate, ClassCheckin::getCheckinTime)
                            .last("LIMIT 5").list();
                    // 最近5条查寝记录
                    List<DormCheckin> recentDorms = dormCheckinService.lambdaQuery()
                            .eq(DormCheckin::getUserId, userId)
                            .orderByDesc(DormCheckin::getDate, DormCheckin::getCheckinTime)
                            .last("LIMIT 5").list();
                    java.time.format.DateTimeFormatter dtFmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm");
                    StringBuilder sb = new StringBuilder("📊 考勤统计\n");
                    sb.append("• 今日上课签到：").append(classToday).append(" 次\n");
                    sb.append("• 今日查寝打卡：").append(dormToday).append(" 次\n\n");
                    if (!recentClasses.isEmpty()) {
                        sb.append("📖 最近上课记录：\n");
                        for (ClassCheckin cc : recentClasses) {
                            String timeStr = cc.getCheckinTime() != null ? cc.getCheckinTime().format(dtFmt) : "--:--";
                            String statusStr = "present".equals(cc.getStatus()) ? "✅ 已签到"
                                    : "late".equals(cc.getStatus()) ? "⏰ 迟到"
                                    : "absent".equals(cc.getStatus()) ? "❌ 缺勤" : "⚪ 未签到";
                            sb.append("• ").append(cc.getDate()).append(" ").append(timeStr).append("  ").append(statusStr).append("\n");
                        }
                    }
                    if (!recentDorms.isEmpty()) {
                        sb.append("🏠 最近查寝记录：\n");
                        for (DormCheckin dc : recentDorms) {
                            String timeStr = dc.getCheckinTime() != null ? dc.getCheckinTime().format(dtFmt) : "--:--";
                            String statusStr = "normal".equals(dc.getStatus()) ? "✅ 正常"
                                    : "late".equals(dc.getStatus()) ? "🌙 晚归" : "⚪ 未打卡";
                            sb.append("• ").append(dc.getDate()).append("  ").append(timeStr).append("  ").append(statusStr).append("\n");
                        }
                    }
                    return Result.success(Map.of("answer", sb.toString(), "sources", List.of()));
                } catch (Exception e) { log.warn("考勤查询失败", e); }
            }

            // 查询今日课程 / 课表
            if (lowerQ.contains("课程") || lowerQ.contains("课表") || lowerQ.contains("今天上什么") || lowerQ.contains("今天有什么课")) {
                try {
                    Long userId = tokenContextHolder.requireCurrentUserId();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                    String cnDay = dayNames[today.getDayOfWeek().getValue()];
                    List<Course> courses = courseService.lambdaQuery()
                            .eq(Course::getWeekDay, cnDay)
                            .orderByAsc(Course::getStartTime)
                            .list();
                    if (!courses.isEmpty()) {
                        StringBuilder sb = new StringBuilder("📚 今日课程（" + today + "）：\n");
                        for (Course c : courses) {
                            sb.append("• ").append(c.getName())
                              .append(" (").append(c.getStartTime()).append("-").append(c.getEndTime())
                              .append(") ").append(c.getLocation() != null ? c.getLocation() : "")
                              .append("\n");
                        }
                        return Result.success(Map.of("answer", sb.toString(), "sources", List.of()));
                    } else {
                        return Result.success(Map.of("answer", "📚 今日没有课程安排，好好休息！", "sources", List.of()));
                    }
                } catch (Exception e) { log.warn("课程查询失败", e); }
            }

            // 教师端：公告管理（通过对话发布公告）
            if (lowerQ.contains("发布公告") || lowerQ.contains("发公告") || lowerQ.contains("创建公告") || lowerQ.contains("新建公告")) {
                try {
                    String role = tokenContextHolder.getCurrentRole();
                    if (!"teacher".equals(role) && !"admin".equals(role)) {
                        return Result.success(Map.of("answer", "只有教师和管理员才能发布公告。", "sources", List.of()));
                    }
                    // 检查是否携带了完整公告信息（用 | 分隔）
                    if (question.contains("|")) {
                        String[] parts = question.split("\\|");
                        if (parts.length >= 4) {
                            String typeRaw = parts[1].trim();
                            String type;
                            if ("通知公告".equals(typeRaw) || "notice".equals(typeRaw)) type = "notice";
                            else if ("活动预告".equals(typeRaw) || "activity".equals(typeRaw)) type = "activity";
                            else if ("学术讲座".equals(typeRaw) || "academic".equals(typeRaw)) type = "academic";
                            else if ("思政教育".equals(typeRaw) || "policy".equals(typeRaw)) type = "policy";
                            else type = "notice";
                            Announcement ann = new Announcement();
                            ann.setTitle(parts[0].trim());
                            ann.setType(type);
                            ann.setSummary(parts[2].trim());
                            ann.setContent(parts[3].trim());
                            ann.setDepartment(parts.length >= 5 ? parts[4].trim() : "教务处");
                            ann.setStatus(1);
                            announcementService.save(ann);
                            return Result.success(Map.of("answer", "✅ 公告《" + ann.getTitle() + "》已发布！\n📌 前往「公告」Tab 可查看", "sources", List.of()));
                        }
                    }
                    return Result.success(Map.of("answer",
                            "📢 请按以下格式发送公告信息（用 | 分隔）：\n" +
                            "标题 | 类型 | 摘要 | 正文 | 发布部门(可选)\n\n" +
                            "类型可选：通知公告 / 活动预告 / 学术讲座 / 思政教育\n\n" +
                            "例如：\n" +
                            "期末考试安排 | 通知公告 | 本学期期末考试时间安排 | 请各位同学查看详情 | 教务处",
                            "sources", List.of()));
                } catch (Exception e) { log.warn("公告管理失败", e); }
            }
        }

        if(!isCampusRelated){
            // 未命中校园关键词也记录到未命中日志
            try {
                UnansweredLog log = new UnansweredLog();
                log.setQuestion(question);
                log.setCategory("all".equals(category) ? null : category);
                log.setAnswered(0);
                unansweredLogService.save(log);
            } catch (Exception ex) { /* ignore */ }
            return Result.success(Map.of("answer", "请咨询校园助手相关的问题",
                    "sources", List.of()));
        }

// 知识库已在 AI 分析前查询过且未匹配，此处记录未命中日志并返回
        try {
            UnansweredLog ulog = new UnansweredLog();
            ulog.setQuestion(question);
            ulog.setCategory("all".equals(category) ? null : category);
            ulog.setAnswered(0);
            unansweredLogService.save(ulog);
        } catch (Exception ex) { /* ignore */ }
        return Result.success(Map.of("answer", "未找到相关答案，请换一种方式提问",
                "sources", List.of()));

    }



    private Result<java.util.Map<String,Object>> handleLeaveApply(java.util.Map<String,Object> action) {
        try {
            Long userId = tokenContextHolder.requireCurrentUserId();
            com.educheck.entity.Leave leave = new com.educheck.entity.Leave();
            leave.setUserId(userId);
            leave.setType((String) action.getOrDefault("type", "sick"));
            String startStr = (String) action.get("startDate");
            String endStr = (String) action.get("endDate");
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (startStr != null) {
                startStr = startStr.replace("T", " ");
                if (!startStr.contains(":")) startStr += " 08:00";
                if (startStr.split(":").length == 2) startStr += ":00";
                leave.setStartDate(java.time.LocalDateTime.parse(startStr, fmt));
            }
            if (endStr != null) {
                endStr = endStr.replace("T", " ");
                if (!endStr.contains(":")) endStr += " 18:00";
                if (endStr.split(":").length == 2) endStr += ":00";
                leave.setEndDate(java.time.LocalDateTime.parse(endStr, fmt));
            }
            leave.setReason((String) action.getOrDefault("reason", "\u901a\u8fc7\u667a\u80fd\u52a9\u624b\u7533\u8bf7"));
            leave.setStatus("pending");
            leaveService.save(leave);
            String answer = "\u2705 \u8bf7\u5047\u7533\u8bf7\u5df2\u63d0\u4ea4\uff01\u7c7b\u578b\uff1a" + leave.getType() + "\u65f6\u95f4\uff1a" + (startStr != null ? startStr : "\u5f85\u5b9a") + " ~ " + (endStr != null ? endStr : "\u5f85\u5b9a") + "\u4e8b\u7531\uff1a" + leave.getReason() + "\u8bf7\u7b49\u5f85\u8f85\u5bfc\u5458\u5ba1\u6279\u3002\n\n\ud83d\udccc \u524d\u5f80\u5e95\u90e8\u300c\u8bf7\u5047\u300dTab \u53ef\u67e5\u770b\u5ba1\u6279\u8fdb\u5ea6";
            return Result.success(java.util.Map.of("answer", answer, "sources", java.util.List.of()));
        } catch (Exception e) {
            log.error("\u81ea\u52a8\u8bf7\u5047\u5931\u8d25: {}", e.getMessage());
            return Result.success(java.util.Map.of("answer", "\u274c \u8bf7\u5047\u63d0\u4ea4\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002", "sources", java.util.List.of()));
        }
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门问题列表")
    public Result<List<KnowledgeBase>> getHotQuestions() {
        // Redis缓存热门问题，5分钟过期
        List<KnowledgeBase> list = cacheService.get("knowledge:hot", List.class);
        if (list == null) {
            list = knowledgeBaseService.getHotQuestions();
            if (list != null) {
                cacheService.set("knowledge:hot", list, 300, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
        return Result.success(list);
    }

    @PostMapping("/items")
    @Operation(summary = "添加知识库条目（自动生成关键词，标记对应未命中为已处理）")
    public Result<KnowledgeBase> addItem(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        String answer = (String) request.get("answer");
        if (question == null || question.trim().isEmpty()) return Result.error("问题不能为空");
        if (answer == null || answer.trim().isEmpty()) return Result.error("答案不能为空");
        KnowledgeBase kb = new KnowledgeBase();
        kb.setQuestion(question.trim());
        kb.setAnswer(answer.trim());
        kb.setCategory((String) request.getOrDefault("category", "general"));
        // 自动生成关键词
        List<String> words = WordTokenizer.segment(kb.getQuestion());
        kb.setKeywords(String.join(",", words.subList(0, Math.min(words.size(), 8))));
        kb.setSortOrder((int) knowledgeBaseService.count() + 1);
        knowledgeBaseService.save(kb);
        // 清除热门问题缓存，使新条目立即可见
        cacheService.delete("knowledge:hot");
        // 标记对应的未命中问题为已处理
        Number unansweredId = (Number) request.get("unansweredId");
        if (unansweredId != null) {
            UnansweredLog log = unansweredLogService.getById(unansweredId.longValue());
            if (log != null) {
                log.setAnswered(1);
                unansweredLogService.updateById(log);
            }
        }
        return Result.success("添加成功", kb);
    }

    @GetMapping("/unanswered")
    @Operation(summary = "获取未回答问题列表")
    public Result<Page<UnansweredLog>> unanswered(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<UnansweredLog> wrapper =
                new LambdaQueryWrapper<UnansweredLog>()
                .eq(UnansweredLog::getAnswered, 0)
                .orderByDesc(UnansweredLog::getCreatedAt);
        return Result.success(unansweredLogService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/unanswered/export")
    @Operation(summary = "导出未回答问题为TXT（可用于知识库扩充）")
    public org.springframework.http.ResponseEntity<byte[]> exportUnanswered() {
        try {
            List<UnansweredLog> list = unansweredLogService.lambdaQuery()
                    .eq(UnansweredLog::getAnswered, 0)
                    .orderByDesc(UnansweredLog::getCreatedAt)
                    .list();
            StringBuilder sb = new StringBuilder();
            sb.append("# 未命中问题导出 - 可用于知识库扩充\n");
            sb.append("# 请补充答案后删除 # 号行\n\n");
            for (UnansweredLog log : list) {
                String q = (log.getQuestion() != null ? log.getQuestion().replace("|", "，") : "").trim();
                if (!q.isEmpty()) {
                    String cat = log.getCategory() != null ? log.getCategory() : "general";
                    sb.append(q).append("|待补充|").append(cat).append("\n");
                }
            }
            byte[] data = sb.toString().getBytes("UTF-8");
            String fileName = java.net.URLEncoder.encode("未命中问题_export.txt", "UTF-8");
            return org.springframework.http.ResponseEntity.ok()
                    .header("Content-Type", "text/plain;charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + fileName)
                    .body(data);
        } catch (Exception e) {
            log.error("导出失败", e);
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }
}