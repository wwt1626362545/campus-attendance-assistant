-- =============================================
-- 校园考勤助手 - 新功能模块建表SQL
-- 公告模块、请假模块、实习打卡、意见反馈、动态码
-- =============================================

-- 1. 公告表
DROP TABLE IF EXISTS `nnouncement`;
CREATE TABLE `nnouncement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `	itle` varchar(200) NOT NULL COMMENT '公告标题',
  `	ype` varchar(50) NOT NULL DEFAULT '
otice' COMMENT '公告类型: notice通知/activity活动/academic学术/policy思政',
  `summary` varchar(500) DEFAULT NULL COMMENT '公告摘要',
  `content` text COMMENT '公告正文',
  `department` varchar(100) DEFAULT NULL COMMENT '发布部门',
  `is_top` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶: 1置顶 0普通',
  `publisher` varchar(100) DEFAULT NULL COMMENT '发布者名称',
  `publisher_id` bigint(20) DEFAULT NULL COMMENT '发布者用户ID',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0下架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`	ype`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';