package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.CheckinStats;
import com.educheck.mapper.CheckinStatsMapper;
import com.educheck.service.CheckinStatsService;
import org.springframework.stereotype.Service;

@Service
public class CheckinStatsServiceImpl extends ServiceImpl<CheckinStatsMapper, CheckinStats>
        implements CheckinStatsService {

}