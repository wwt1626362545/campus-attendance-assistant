package com.educheck.controller;

import com.educheck.common.Result;
import com.educheck.common.WordTokenizer;
import com.educheck.entity.KnowledgeBase;
import com.educheck.service.KnowledgeBaseService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "知识库文件上传扩充")
public class FileUploadController {

    private final KnowledgeBaseService knowledgeBaseService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/knowledge")
    @Operation(summary = "上传文件扩充知识库（支持txt/json）")
    public Result<Map<String, Object>> uploadKnowledge(
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return Result.error("请选择文件");

        String originalName = file.getOriginalFilename();
        String ext = originalName != null ? originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase() : "";

        if (!Set.of("txt", "json", "md").contains(ext)) {
            return Result.error("仅支持 txt/json/md 格式");
        }

        // 读取文件内容
        String content = new String(file.getBytes(), "UTF-8");
        int added = 0;

        if ("json".equals(ext)) {
            // JSON格式: [{"question":"...","answer":"...","category":"..."}]
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(content, List.class);
            for (Map<String, Object> item : list) {
                KnowledgeBase kb = new KnowledgeBase();
                kb.setQuestion((String) item.getOrDefault("question", ""));
                kb.setAnswer((String) item.getOrDefault("answer", ""));
                kb.setCategory((String) item.getOrDefault("category", "general"));
                String kw = (String) item.getOrDefault("keywords", "");
                if (kw.isEmpty() && kb.getQuestion() != null) {
                    List<String> words = com.educheck.common.WordTokenizer.segment(kb.getQuestion());
                    kw = String.join(",", words.subList(0, Math.min(words.size(), 8)));
                }
                kb.setKeywords(kw);
                kb.setSortOrder(knowledgeBaseService.count() > 0 ? (int) knowledgeBaseService.count() + 1 : 1);
                knowledgeBaseService.save(kb);
                added++;
            }
        } else {
            // txt/md：按行解析，每行格式：问题|答案|分类
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length >= 2) {
                    KnowledgeBase kb = new KnowledgeBase();
                    kb.setQuestion(parts[0].trim());
                    kb.setAnswer(parts[1].trim());
                    kb.setCategory(parts.length >= 3 ? parts[2].trim() : "general");
                    // 自动生成关键词
                    List<String> words = com.educheck.common.WordTokenizer.segment(kb.getQuestion());
                    kb.setKeywords(String.join(",", words.subList(0, Math.min(words.size(), 8))));
                    kb.setSortOrder(knowledgeBaseService.count() > 0 ? (int) knowledgeBaseService.count() + 1 : 1);
                    knowledgeBaseService.save(kb);
                    added++;
                }
            }
        }

        // 保存文件到本地
        Path dir = Paths.get(uploadDir, "knowledge");
        Files.createDirectories(dir);
        String saveName = UUID.randomUUID().toString() + "_" + originalName;
        Files.copy(file.getInputStream(), dir.resolve(saveName));

        return Result.success(Map.of("added", added, "file", saveName));
    }
}
