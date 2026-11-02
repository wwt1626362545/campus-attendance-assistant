package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.entity.ClassCheckin;
import com.educheck.mapper.ClassCheckinMapper;
import com.educheck.service.ClassCheckinService;
import com.educheck.vo.ClassCheckinHistoryVO;
import org.springframework.stereotype.Service;

@Service
public class ClassCheckinServiceImpl
        extends ServiceImpl<ClassCheckinMapper, ClassCheckin>
        implements ClassCheckinService {

    @Override
    public IPage<ClassCheckinHistoryVO> selectHistoryPage(Page<ClassCheckinHistoryVO> page, Long userId) {
        return baseMapper.selectHistoryPage(page, userId);
    }

    @Override
    public IPage<ClassCheckinHistoryVO> selectShortHistory(Long userId) {
        Page<ClassCheckinHistoryVO> page = new Page<>(1, 5);
        return baseMapper.selectHistoryPage(page, userId);
    }
}
