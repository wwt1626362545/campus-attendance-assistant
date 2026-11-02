package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.DormCheckin;
import com.educheck.mapper.DormCheckinMapper;
import com.educheck.service.DormCheckinService;
import org.springframework.stereotype.Service;

@Service
public class DormCheckinServiceImpl
        extends ServiceImpl<DormCheckinMapper, DormCheckin>
        implements DormCheckinService {

}