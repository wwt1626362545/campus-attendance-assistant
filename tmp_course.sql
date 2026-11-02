
-- =====================================================
-- 课程表
-- =====================================================
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `teacher` VARCHAR(50) DEFAULT NULL COMMENT '授课教师',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '上课地点',
    `start_time` VARCHAR(20) DEFAULT NULL COMMENT '上课时间 HH:mm',
    `end_time` VARCHAR(20) DEFAULT NULL COMMENT '下课时间 HH:mm',
    `section` VARCHAR(20) DEFAULT NULL COMMENT '节次',
    `week_day` VARCHAR(10) DEFAULT NULL COMMENT '星期几',
    `week_start` INT DEFAULT NULL COMMENT '起始周',
    `week_end` INT DEFAULT NULL COMMENT '结束周',
    `teacher_id` BIGINT DEFAULT NULL COMMENT '教师用户ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_id` (`teacher_id`),
    INDEX `idx_week_day` (`week_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- =====================================================
-- 上课签到表
-- =====================================================
DROP TABLE IF EXISTS `class_checkin`;
CREATE TABLE `class_checkin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `course_id` BIGINT DEFAULT NULL COMMENT '课程ID',
    `date` DATE NOT NULL COMMENT '签到日期',
    `checkin_time` DATETIME DEFAULT NULL COMMENT '签到时间',
    `method` VARCHAR(20) DEFAULT NULL COMMENT '签到方式: location/qrcode',
    `location_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    `location_lng` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    `location_addr` VARCHAR(255) DEFAULT NULL COMMENT '定位地址',
    `dynamic_code` VARCHAR(10) DEFAULT NULL COMMENT '动态码',
    `status` VARCHAR(20) NOT NULL DEFAULT 'present' COMMENT '状态: present/absent/late',
    `session_id` VARCHAR(50) DEFAULT NULL COMMENT '签到会话ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_course_id` (`course_id`),
    INDEX `idx_date` (`date`),
    INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上课签到表';





-- 默认课程（计算机专业周课表，周一至周日每天5个时段，覆盖08:00-20:40）
DELETE FROM `course`;
INSERT INTO `course` (`id`, `name`, `teacher`, `location`, `start_time`, `end_time`, `section`, `week_day`, `week_start`, `week_end`, `teacher_id`) VALUES
-- ===== 周一 =====
(1,  '数据结构与算法',           '张教授', '教学楼A区 201教室',  '08:00', '09:40', '1-2',  '周一', 1, 18, 2),
(2,  '线性代数',                 '李教授', '教学楼B区 302教室',  '10:00', '11:40', '3-4',  '周一', 1, 18, 2),
(3,  '大学英语',                 '陈教授', '教学楼C区 105教室',  '14:00', '15:40', '5-6',  '周一', 1, 18, 2),
(4,  '体育',                     '孙老师', '体育馆',             '16:00', '17:40', '7-8',  '周一', 1, 18, 2),
(5,  'Python程序设计',           '王教授', '实验楼 401机房',     '19:00', '20:40', '9-10', '周一', 1, 18, 2),
-- ===== 周二 =====
(6,  '习近平新时代中国特色社会主义思想概论', '王教授', '教学楼A区 301教室', '08:00', '09:40', '1-2', '周二', 1, 18, 2),
(7,  '计算机网络',               '刘教授', '教学楼B区 205教室',  '10:00', '11:40', '3-4',  '周二', 1, 18, 2),
(8,  '操作系统原理',             '赵教授', '教学楼A区 202教室',  '14:00', '15:40', '5-6',  '周二', 1, 18, 2),
(9,  '概率论与数理统计',         '李教授', '教学楼B区 303教室',  '16:00', '17:40', '7-8',  '周二', 1, 18, 2),
(10, '人工智能导论',             '周教授', '教学楼D区 201教室',  '19:00', '20:40', '9-10', '周二', 1, 18, 2),
-- ===== 周三 =====
(11, '数据结构与算法',           '张教授', '教学楼A区 201教室',  '08:00', '09:40', '1-2',  '周三', 1, 18, 2),
(12, '数据库系统原理',           '吴教授', '教学楼A区 305教室',  '10:00', '11:40', '3-4',  '周三', 1, 18, 2),
(13, '大学英语',                 '陈教授', '教学楼C区 105教室',  '14:00', '15:40', '5-6',  '周三', 1, 18, 2),
(14, '计算机组成原理',           '孙教授', '教学楼B区 201教室',  '16:00', '17:40', '7-8',  '周三', 1, 18, 2),
(15, '软件工程',                 '周教授', '教学楼D区 101教室',  '19:00', '20:40', '9-10', '周三', 1, 18, 2),
-- ===== 周四 =====
(16, '操作系统原理',             '赵教授', '教学楼A区 202教室',  '08:00', '09:40', '1-2',  '周四', 1, 18, 2),
(17, '计算机网络实验',           '刘教授', '实验楼 402机房',     '10:00', '11:40', '3-4',  '周四', 1, 18, 2),
(18, '线性代数',                 '李教授', '教学楼B区 302教室',  '14:00', '15:40', '5-6',  '周四', 1, 18, 2),
(19, 'Web前端开发',              '陈教授', '实验楼 403机房',     '16:00', '17:40', '7-8',  '周四', 1, 18, 2),
(20, '形势与政策',               '周教授', '教学楼D区 101教室',  '19:00', '20:40', '9-10', '周四', 1, 18, 2),
-- ===== 周五 =====
(21, '概率论与数理统计',         '李教授', '教学楼B区 303教室',  '08:00', '09:40', '1-2',  '周五', 1, 18, 2),
(22, '计算机组成原理',           '孙教授', '教学楼B区 201教室',  '10:00', '11:40', '3-4',  '周五', 1, 18, 2),
(23, '数据库系统原理',           '吴教授', '教学楼A区 305教室',  '14:00', '15:40', '5-6',  '周五', 1, 18, 2),
(24, '数据结构与算法实验',       '张教授', '实验楼 401机房',     '16:00', '17:40', '7-8',  '周五', 1, 18, 2),
(25, '大学生心理健康',           '马老师', '教学楼C区 201教室',  '19:00', '20:40', '9-10', '周五', 1, 18, 2),
-- ===== 周六 =====
(26, '人工智能导论',             '周教授', '教学楼D区 201教室',  '08:00', '09:40', '1-2',  '周六', 1, 18, 2),
(27, 'Python程序设计',           '王教授', '实验楼 401机房',     '10:00', '11:40', '3-4',  '周六', 1, 18, 2),
(28, 'Web前端开发',              '陈教授', '实验楼 403机房',     '14:00', '15:40', '5-6',  '周六', 1, 18, 2),
(29, '软件工程',                 '周教授', '教学楼D区 101教室',  '16:00', '17:40', '7-8',  '周六', 1, 18, 2),
(30, '创新实践',                 '赵教授', '实验楼 501实验室',   '19:00', '20:40', '9-10', '周六', 1, 18, 2),
-- ===== 周日 =====
(31, '数据结构与算法（练习）',   '张教授', '教学楼A区 201教室',  '08:00', '09:40', '1-2',  '周日', 1, 18, 2),
(32, '操作系统原理（练习）',     '赵教授', '教学楼A区 202教室',  '10:00', '11:40', '3-4',  '周日', 1, 18, 2),
(33, '线性代数（练习）',         '李教授', '教学楼B区 302教室',  '14:00', '15:40', '5-6',  '周日', 1, 18, 2),
(34, '计算机网络（练习）',       '刘教授', '教学楼B区 205教室',  '16:00', '17:40', '7-8',  '周日', 1, 18, 2),
(35, '概率论与数理统计（练习）', '李教授', '教学楼B区 303教室',  '19:00', '20:40', '9-10', '周日', 1, 18, 2);



-- 默认打卡统计
DELETE FROM `checkin_stats`;
INSERT INTO `checkin_stats` (`user_id`, `dorm_total`, `class_total`, `intern_total`, `streak_days`, `total_points`) VALUES
(1, 42, 68, 15, 7, 268);


-- 上课签到记录（15条）
DELETE FROM `class_checkin`;
INSERT INTO `class_checkin` (`id`, `user_id`, `course_id`, `date`, `checkin_time`, `method`, `location_lat`, `location_lng`, `location_addr`, `dynamic_code`, `status`, `session_id`) VALUES
(1, 1, 1, '2026-03-03', '2026-03-03 08:02:00', 'location', 30.275000, 120.156000, '教学楼A区 301教室', NULL, 'present', 'S20260301'),
(2, 1, 1, '2026-03-10', '2026-03-10 08:01:00', 'location', 30.275010, 120.156010, '教学楼A区 301教室', NULL, 'present', 'S20260302'),
(3, 1, 1, '2026-03-17', '2026-03-17 08:05:00', 'qrcode', NULL, NULL, NULL, '827364', 'present', 'S20260303'),
(4, 1, 1, '2026-03-24', '2026-03-24 07:58:00', 'location', 30.275020, 120.156020, '教学楼A区 301教室', NULL, 'present', 'S20260304'),
(5, 1, 1, '2026-03-31', '2026-03-31 08:00:00', 'location', 30.274980, 120.155980, '教学楼A区 301教室', NULL, 'present', 'S20260305'),
(6, 1, 1, '2026-04-07', '2026-04-07 08:03:00', 'qrcode', NULL, NULL, NULL, '915672', 'present', 'S20260401'),
(7, 1, 1, '2026-04-14', '2026-04-14 08:10:00', 'location', 30.275030, 120.156030, '教学楼A区 301教室', NULL, 'late', 'S20260402'),
(8, 1, 1, '2026-04-21', '2026-04-21 07:55:00', 'location', 30.274990, 120.155990, '教学楼A区 301教室', NULL, 'present', 'S20260403'),
(9, 1, 1, '2026-04-28', '2026-04-28 08:02:00', 'qrcode', NULL, NULL, NULL, '638291', 'present', 'S20260404'),
(10, 1, 1, '2026-05-05', '2026-05-05 08:00:00', 'location', 30.275040, 120.156040, '教学楼A区 301教室', NULL, 'present', 'S20260501'),
(11, 1, 1, '2026-05-12', '2026-05-12 08:08:00', 'location', 30.275000, 120.156000, '教学楼A区 301教室', NULL, 'present', 'S20260502'),
(12, 1, 1, '2026-05-19', '2026-05-19 07:56:00', 'location', 30.274970, 120.155970, '教学楼A区 301教室', NULL, 'present', 'S20260503'),
(13, 1, 1, '2026-05-26', '2026-05-26 08:01:00', 'qrcode', NULL, NULL, NULL, '746123', 'present', 'S20260504'),
(14, 1, 1, '2026-06-02', '2026-06-02 09:00:00', 'location', 30.275050, 120.156050, '教学楼A区 301教室', NULL, 'late', 'S20260601'),
(15, 1, 1, '2026-06-09', '2026-06-09 07:59:00', 'location', 30.275010, 120.156010, '教学楼A区 301教室', NULL, 'present', 'S20260602');


-- 更多学生打卡统计
INSERT INTO `checkin_stats` (`user_id`, `dorm_total`, `class_total`, `intern_total`, `streak_days`, `total_points`) VALUES
(4, 38, 52, 0, 5, 180),
(5, 45, 60, 0, 12, 210),
(6, 20, 35, 0, 3, 110),
(7, 50, 70, 0, 20, 300),
(8, 0, 0, 25, 0, 100),
(9, 30, 0, 0, 8, 150);


-- 补充上课签到记录（新学生的打卡数据，让教师端学生管理有数据可看）
INSERT INTO `class_checkin` (`id`, `user_id`, `course_id`, `date`, `checkin_time`, `method`, `status`, `session_id`) VALUES
(16, 4, 1, '2026-06-08', '2026-06-08 08:01:00', 'location', 'present', 'S20260603'),
(17, 5, 1, '2026-06-08', '2026-06-08 08:03:00', 'location', 'present', 'S20260603'),
(18, 6, 1, '2026-06-08', '2026-06-08 08:00:00', 'location', 'present', 'S20260603'),
(19, 7, 1, '2026-06-08', '2026-06-08 08:05:00', 'location', 'present', 'S20260603'),
(20, 9, 1, '2026-06-08', '2026-06-08 08:10:00', 'location', 'late', 'S20260603'),
(21, 4, 2, '2026-06-08', '2026-06-08 10:02:00', 'location', 'present', 'S20260604'),
(22, 5, 2, '2026-06-08', '2026-06-08 10:00:00', 'location', 'present', 'S20260604'),
(23, 6, 2, '2026-06-08', '2026-06-08 10:15:00', 'location', 'late', 'S20260604'),
(24, 7, 2, '2026-06-08', '2026-06-08 09:58:00', 'location', 'present', 'S20260604'),
(25, 1, 2, '2026-06-08', '2026-06-08 10:01:00', 'location', 'present', 'S20260604');



