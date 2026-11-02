package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.KnowledgeBase;
import com.educheck.mapper.KnowledgeBaseMapper;
import com.educheck.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl
        extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    @Override
    public List<KnowledgeBase> getHotQuestions() {
        return this.lambdaQuery().orderByAsc(KnowledgeBase::getSortOrder).last("LIMIT 50").list();
    }
}