package com.educheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.educheck.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
