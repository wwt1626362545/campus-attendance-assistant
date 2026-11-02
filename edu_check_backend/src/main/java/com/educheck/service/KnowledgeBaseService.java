package com.educheck.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.educheck.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /** 获取热门问题（按 sort_order 排序，取前 6 条） */
    List<KnowledgeBase> getHotQuestions();

}