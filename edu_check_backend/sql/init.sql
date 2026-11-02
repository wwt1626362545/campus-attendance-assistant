-- =============================================
-- 校园考勤助手 - 完整数据库建表脚本
-- 执行方式: source sql/init.sql
-- =============================================

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `student_id` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `college` varchar(100) DEFAULT NULL,
  `major` varchar(100) DEFAULT NULL,
  `grade` varchar(20) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'student',
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `teacher` varchar(100) DEFAULT NULL,
  `location` varchar(200) DEFAULT NULL,
  `start_time` varchar(10) DEFAULT NULL,
  `end_time` varchar(10) DEFAULT NULL,
  `section` varchar(20) DEFAULT NULL,
  `week_day` varchar(10) DEFAULT NULL,
  `week_start` int(11) DEFAULT NULL,
  `week_end` int(11) DEFAULT NULL,
  `teacher_id` int(11) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 上课签到表
DROP TABLE IF EXISTS `class_checkin`;
CREATE TABLE `class_checkin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `course_id` int(11) NOT NULL,
  `date` date NOT NULL,
  `checkin_time` datetime DEFAULT NULL,
  `method` varchar(20) DEFAULT NULL,
  `location_lat` decimal(10,7) DEFAULT NULL,
  `location_lng` decimal(10,7) DEFAULT NULL,
  `location_addr` varchar(300) DEFAULT NULL,
  `dynamic_code` varchar(20) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 查寝打卡表
DROP TABLE IF EXISTS `dorm_checkin`;
CREATE TABLE `dorm_checkin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `date` date NOT NULL,
  `checkin_time` datetime DEFAULT NULL,
  `location_lat` decimal(10,7) DEFAULT NULL,
  `location_lng` decimal(10,7) DEFAULT NULL,
  `location_addr` varchar(300) DEFAULT NULL,
  `building` varchar(50) DEFAULT NULL,
  `room` varchar(50) DEFAULT NULL,
  `in_dorm_area` tinyint(1) DEFAULT NULL,
  `face_verified` tinyint(1) DEFAULT NULL,
  `face_image_url` varchar(500) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 人脸信息表
DROP TABLE IF EXISTS `user_face`;
CREATE TABLE `user_face` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `face_url` varchar(500) DEFAULT NULL,
  `face_token` varchar(200) DEFAULT NULL,
  `registered` tinyint(1) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 打卡统计表
DROP TABLE IF EXISTS `checkin_stats`;
CREATE TABLE `checkin_stats` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `dorm_total` int(11) DEFAULT 0,
  `class_total` int(11) DEFAULT 0,
  `intern_total` int(11) DEFAULT 0,
  `streak_days` int(11) DEFAULT 0,
  `total_points` int(11) DEFAULT 0,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 知识库表
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `category` varchar(50) DEFAULT NULL,
  `question` varchar(500) DEFAULT NULL,
  `answer` text,
  `keywords` varchar(500) DEFAULT NULL,
  `synonyms` varchar(500) DEFAULT NULL,
  `sort_order` int(11) DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 未回答问题日志表
DROP TABLE IF EXISTS `unanswered_log`;
CREATE TABLE `unanswered_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question` varchar(500) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `answered` tinyint(1) DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================
-- 智能问答知识库默认数据
-- =============================================
DELETE FROM `knowledge_base`;
INSERT INTO `knowledge_base` (`category`, `question`, `answer`, `keywords`, `synonyms`, `sort_order`) VALUES
('general', '校园考勤助手有哪些功能？', '校园考勤助手支持三大打卡：查寝打卡、上课打卡、实习打卡。还提供请假审批、公告、智能问答等功能。', '功能,介绍,打卡,考勤', '签到=打卡|功能=用途,作用', 1),
('general', '如何切换学生/教师账号？', '在"我的"页面底部点击"切换账号"即可切换。', '切换,账号,学生,教师', '切换=转换|账号=账户', 2),
('general', '查寝打卡怎么用？', '首页点击查寝打卡，定位拍照人脸验证后提交。', '查寝,打卡,宿舍', '查寝=查房|打卡=签到', 3),
('general', '上课签到怎么用？', '首页选择课程，定位签到，输入动态码，确认提交。', '上课,签到,打卡', '上课=课堂|签到=打卡', 4),
('general', '实习打卡怎么用？', '首页点击实习打卡，定位填日志确认提交。', '实习,打卡', '实习=工作|打卡=签到', 5),
('general', '请假功能怎么使用？', 'Tab点击请假，提交申请，等待教师审批。', '请假,申请', '请假=休假|审批=审核', 6),
('frontend', '前端页面结构是怎样的？', '共4个Tab：首页、公告、请假、我的。', '页面,结构,前端', '页面=界面|结构=构成', 7),
('backend', '后端技术栈是什么？', 'Spring Boot 3.2 + MyBatis-Plus + MySQL。', '技术栈,后端', '后端=服务端|数据库=MySQL', 8),
('database', '数据库有哪些核心表？', '用户、课程、签到、查寝、实习、请假、公告等。', '表,数据库', '表=数据表|数据库=DB', 9);

-- 公告表
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `type` varchar(50) NOT NULL DEFAULT 'notice',
  `summary` varchar(500) DEFAULT NULL,
  `content` text,
  `department` varchar(100) DEFAULT NULL,
  `is_top` tinyint(1) NOT NULL DEFAULT 0,
  `publisher` varchar(100) DEFAULT NULL,
  `publisher_id` bigint(20) DEFAULT NULL,
  `read_count` int(11) NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 请假记录表
DROP TABLE IF EXISTS `leave_record`;
CREATE TABLE `leave_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'sick',
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `reason` varchar(1000) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `reject_reason` varchar(500) DEFAULT NULL,
  `approver_id` bigint(20) DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 实习信息表
DROP TABLE IF EXISTS `internship`;
CREATE TABLE `internship` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `company` varchar(200) NOT NULL,
  `role` varchar(100) NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `total_days` int(11) NOT NULL DEFAULT 0,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 实习打卡记录表
DROP TABLE IF EXISTS `intern_checkin`;
CREATE TABLE `intern_checkin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `internship_id` bigint(20) NOT NULL,
  `date` date NOT NULL,
  `checkin_time` datetime NOT NULL,
  `location_addr` varchar(300) DEFAULT NULL,
  `log_content` varchar(2000) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'normal',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 实习照片表
DROP TABLE IF EXISTS `intern_photo`;
CREATE TABLE `intern_photo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `checkin_id` bigint(20) NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 意见反馈表
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `type` varchar(20) NOT NULL DEFAULT 'suggestion',
  `content` text NOT NULL,
  `contact` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `reply` varchar(1000) DEFAULT NULL,
  `reply_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 动态码表
DROP TABLE IF EXISTS `dynamic_code`;
CREATE TABLE `dynamic_code` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `dynamic_code` varchar(10) NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `duration` int(11) NOT NULL DEFAULT 60,
  `expires_at` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 测试公告
INSERT INTO `announcement`(`title`,`type`,`summary`,`content`,`department`,`is_top`,`publisher`,`publisher_id`) VALUES
('计算机学院学术讲座','academic','清华大学张教授讲解AI前沿...','<p>讲座主题：人工智能前沿技术与发展趋势</p><p>主讲人：清华大学 张教授</p><p>时间：2026年6月15日 14:00</p>','计算机学院',0,'计算机学院',1),
('毕业生就业双选会','activity','100余家企业参会...','<p>为促进毕业生就业，学校将于6月20日在体育馆举办大型双选会。</p>','就业指导中心',0,'就业指导中心',1);

-- 管理员账号（密码: admin123）
INSERT INTO `user`(`name`,`student_id`,`password`,`college`,`role`,`status`) VALUES
('系统管理员','admin','admin123','信息中心','admin',1);

-- 宿舍报修表
DROP TABLE IF EXISTS `repair_record`;
CREATE TABLE `repair_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `student_name` varchar(50) DEFAULT NULL,
  `student_id` varchar(50) DEFAULT NULL,
  `dorm_building` varchar(50) DEFAULT NULL,
  `dorm_room` varchar(50) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `description` text,
  `images` varchar(2000) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `reply` varchar(1000) DEFAULT NULL,
  `handler_id` bigint(20) DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍报修记录表';

-- 测试实习信息
INSERT INTO `internship`(`user_id`,`company`,`role`,`start_date`,`end_date`,`total_days`) VALUES
(1,'字节跳动科技有限公司','前端开发实习生','2026-03-01','2026-08-31',180);
