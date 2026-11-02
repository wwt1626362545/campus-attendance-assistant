package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.RepairRecord;
import com.educheck.mapper.RepairRecordMapper;
import com.educheck.service.RepairRecordService;
import org.springframework.stereotype.Service;

@Service
public class RepairRecordServiceImpl extends ServiceImpl<RepairRecordMapper, RepairRecord> implements RepairRecordService {}
