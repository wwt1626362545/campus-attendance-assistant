package com.educheck.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.educheck.entity.InternPhoto;
import com.educheck.mapper.InternPhotoMapper;
import com.educheck.service.InternPhotoService;
import org.springframework.stereotype.Service;

@Service
public class InternPhotoServiceImpl extends ServiceImpl<InternPhotoMapper, InternPhoto> implements InternPhotoService {
}
