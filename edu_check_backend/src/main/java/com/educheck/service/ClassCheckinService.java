package com.educheck.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.entity.ClassCheckin;
import com.educheck.vo.ClassCheckinHistoryVO;

public interface ClassCheckinService extends IService<ClassCheckin> {

    /**
     * 分页查询签到历史（联表 course 带出课程名称）
     */
    IPage<ClassCheckinHistoryVO> selectHistoryPage(Page<ClassCheckinHistoryVO> page, Long userId);

    /**
     * 查询近期前 5 条签到记录（首页短列表）
     */
    IPage<ClassCheckinHistoryVO> selectShortHistory(Long userId);
}
