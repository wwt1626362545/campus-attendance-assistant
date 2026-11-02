package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.UnansweredLog;
import com.educheck.mapper.UnansweredLogMapper;
import com.educheck.service.UnansweredLogService;
import org.springframework.stereotype.Service;

@Service
public class UnansweredLogServiceImpl
        extends ServiceImpl<UnansweredLogMapper, UnansweredLog>
        implements UnansweredLogService {

}