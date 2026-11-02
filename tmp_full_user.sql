-- =====================================================
-- 用户表
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `student_id` VARCHAR(20) DEFAULT NULL COMMENT '学号',
    `password` VARCHAR(255) NOT NULL DEFAULT '123456' COMMENT '密码',
    `college` VARCHAR(100) DEFAULT NULL COMMENT '学院',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
    `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色: student/teacher/admin',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_id` (`student_id`),
    INDEX `idx_college` (`college`),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';



-- =====================================================
-- 用户人脸表
-- =====================================================
DROP TABLE IF EXISTS `user_face`;
CREATE TABLE `user_face` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `face_url` VARCHAR(255) DEFAULT NULL COMMENT '人脸图片存储路径',
    `face_token` VARCHAR(255) DEFAULT NULL COMMENT '百度云返回的face_token',
    `registered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已注册人脸: 0否 1是',
    `version` INT NOT NULL DEFAULT 0 COMMENT '注册次数（每次更新+1）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户人脸表';



-- =====================================================
-- 打卡统计表
-- =====================================================
DROP TABLE IF EXISTS `checkin_stats`;
CREATE TABLE `checkin_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `dorm_total` INT NOT NULL DEFAULT 0 COMMENT '查寝打卡总数',
    `class_total` INT NOT NULL DEFAULT 0 COMMENT '上课签到总数',
    `intern_total` INT NOT NULL DEFAULT 0 COMMENT '实习打卡总数',
    `streak_days` INT NOT NULL DEFAULT 0 COMMENT '连续打卡天数',
    `total_points` INT NOT NULL DEFAULT 0 COMMENT '总积分',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡统计表';




-- =====================================================
-- 查寝打卡表
-- =====================================================
DROP TABLE IF EXISTS `dorm_checkin`;
CREATE TABLE `dorm_checkin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `date` DATE NOT NULL COMMENT '打卡日期',
    `checkin_time` DATETIME DEFAULT NULL COMMENT '打卡时间',
    `location_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    `location_lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    `location_addr` VARCHAR(255) DEFAULT NULL COMMENT '定位地址',
    `building` VARCHAR(100) DEFAULT NULL COMMENT '宿舍楼',
    `room` VARCHAR(50) DEFAULT NULL COMMENT '宿舍号',
    `in_dorm_area` TINYINT NOT NULL DEFAULT 0 COMMENT '是否在宿舍区域',
    `face_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '人脸是否验证通过',
    `face_image_url` VARCHAR(255) DEFAULT NULL COMMENT '人脸图片URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'normal' COMMENT '状态: normal/late',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_date` (`date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查寝打卡表';



-- 默认用户
DELETE FROM `user`;
INSERT INTO `user` (`id`, `name`, `student_id`, `password`, `college`, `major`, `grade`, `role`) VALUES
(1, '张小飞', '2024011001', '123456', '计算机科学与技术学院', '软件工程', '2024级', 'student'),
(2, '张大飞', 'T2024001', '123456', '计算机科学与技术学院', NULL, NULL, 'teacher'),
(3, 'admin', 'admin', '123456', NULL, NULL, NULL, 'admin');

-- 更多学生
INSERT INTO `user` (`id`, `name`, `student_id`, `password`, `college`, `major`, `grade`, `role`) VALUES
(4,  '王同学', '2024011002', '123456', '计算机科学与技术学院', '软件工程',     '2024级', 'student'),
(5,  '刘同学', '2024011003', '123456', '计算机科学与技术学院', '计算机科学与技术', '2024级', 'student'),
(6,  '陈同学', '2024011004', '123456', '计算机科学与技术学院', '软件工程',     '2024级', 'student'),
(7,  '赵同学', '2024011005', '123456', '计算机科学与技术学院', '人工智能',    '2024级', 'student'),
(8,  '孙同学', '2024012001', '123456', '计算机科学与技术学院', '数据科学',    '2023级', 'student'),
(9,  '周同学', '2024012002', '123456', '计算机科学与技术学院', '软件工程',     '2023级', 'student');




-- 默认打卡统计
DELETE FROM `checkin_stats`;
INSERT INTO `checkin_stats` (`user_id`, `dorm_total`, `class_total`, `intern_total`, `streak_days`, `total_points`) VALUES
(1, 42, 68, 15, 7, 268);

-- 更多学生打卡统计
INSERT INTO `checkin_stats` (`user_id`, `dorm_total`, `class_total`, `intern_total`, `streak_days`, `total_points`) VALUES
(4, 38, 52, 0, 5, 180),
(5, 45, 60, 0, 12, 210),
(6, 20, 35, 0, 3, 110),
(7, 50, 70, 0, 20, 300),
(8, 0, 0, 25, 0, 100),
(9, 30, 0, 0, 8, 150);


-- 查寝打卡记录（29条）
DELETE FROM `dorm_checkin`;
INSERT INTO `dorm_checkin` (`id`, `user_id`, `date`, `checkin_time`, `location_lat`, `location_lng`, `location_addr`, `building`, `room`, `in_dorm_area`, `face_verified`, `status`) VALUES
(1, 1, '2026-05-08', '2026-05-08 22:15:00', 30.274151, 120.155079, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(2, 1, '2026-05-09', '2026-05-09 22:08:00', 30.274158, 120.155082, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(3, 1, '2026-05-10', '2026-05-10 22:30:00', 30.274145, 120.155075, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(4, 1, '2026-05-11', '2026-05-11 22:05:00', 30.274160, 120.155085, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(5, 1, '2026-05-12', '2026-05-12 23:15:00', 30.274140, 120.155070, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'late'),
(6, 1, '2026-05-13', '2026-05-13 22:10:00', 30.274155, 120.155078, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(7, 1, '2026-05-14', '2026-05-14 21:55:00', 30.274148, 120.155073, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(8, 1, '2026-05-15', '2026-05-15 22:20:00', 30.274162, 120.155088, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(9, 1, '2026-05-16', '2026-05-16 22:12:00', 30.274153, 120.155080, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(10, 1, '2026-05-17', '2026-05-17 22:25:00', 30.274147, 120.155072, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(11, 1, '2026-05-18', '2026-05-18 21:58:00', 30.274159, 120.155083, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(12, 1, '2026-05-19', '2026-05-19 23:05:00', 30.274142, 120.155069, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'late'),
(13, 1, '2026-05-20', '2026-05-20 22:18:00', 30.274156, 120.155081, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(14, 1, '2026-05-21', '2026-05-21 22:02:00', 30.274149, 120.155076, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(15, 1, '2026-05-22', '2026-05-22 22:10:00', 30.274157, 120.155084, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(16, 1, '2026-05-23', '2026-05-23 22:30:00', 30.274144, 120.155071, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(17, 1, '2026-05-24', '2026-05-24 21:50:00', 30.274161, 120.155086, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(18, 1, '2026-05-25', '2026-05-25 22:08:00', 30.274152, 120.155079, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(19, 1, '2026-05-26', '2026-05-26 23:20:00', 30.274138, 120.155067, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'late'),
(20, 1, '2026-05-27', '2026-05-27 22:15:00', 30.274154, 120.155078, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(21, 1, '2026-05-28', '2026-05-28 22:05:00', 30.274150, 120.155077, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(22, 1, '2026-05-29', '2026-05-29 22:22:00', 30.274146, 120.155074, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(23, 1, '2026-05-30', '2026-05-30 21:48:00', 30.274163, 120.155089, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(24, 1, '2026-05-31', '2026-05-31 22:35:00', 30.274143, 120.155068, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(25, 1, '2026-06-01', '2026-06-01 22:00:00', 30.274158, 120.155082, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(26, 1, '2026-06-02', '2026-06-02 22:12:00', 30.274151, 120.155080, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(27, 1, '2026-06-03', '2026-06-03 23:10:00', 30.274141, 120.155070, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'late'),
(28, 1, '2026-06-04', '2026-06-04 22:08:00', 30.274155, 120.155081, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal'),
(29, 1, '2026-06-05', '2026-06-05 22:18:00', 30.274149, 120.155076, '浙江省杭州市西湖区学生公寓3号楼', '学生公寓3号楼', '518室', 1, 1, 'normal');





