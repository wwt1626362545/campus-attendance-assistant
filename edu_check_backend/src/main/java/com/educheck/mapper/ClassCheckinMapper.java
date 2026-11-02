package com.educheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.educheck.entity.ClassCheckin;
import com.educheck.vo.ClassCheckinHistoryVO;
import org.apache.ibatis.annotations.Param;

public interface ClassCheckinMapper extends BaseMapper<ClassCheckin> {

    /**
     * 联表分页查询签到历史（LEFT JOIN course 带出课程名称）
     */
    IPage<ClassCheckinHistoryVO> selectHistoryPage(Page<ClassCheckinHistoryVO> page,
                                                   @Param("userId") Long userId);
}
