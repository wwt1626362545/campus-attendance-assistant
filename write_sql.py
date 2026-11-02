import os
BT = chr(96)  # backtick
SQ = chr(39)  # single quote

sql = f'''-- =============================================
-- 校园考勤助手 - 新功能模块建表SQL
-- 公告模块、请假模块、实习打卡、意见反馈、动态码
-- =============================================

-- 1. 公告表
DROP TABLE IF EXISTS {BT}announcement{BT};
CREATE TABLE {BT}announcement{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}title{BT} varchar(200) NOT NULL COMMENT {SQ}公告标题{SQ},
  {BT}type{BT} varchar(50) NOT NULL DEFAULT {SQ}notice{SQ} COMMENT {SQ}公告类型: notice通知/activity活动/academic学术/policy思政{SQ},
  {BT}summary{BT} varchar(500) DEFAULT NULL COMMENT {SQ}公告摘要{SQ},
  {BT}content{BT} text COMMENT {SQ}公告正文{SQ},
  {BT}department{BT} varchar(100) DEFAULT NULL COMMENT {SQ}发布部门{SQ},
  {BT}is_top{BT} tinyint(1) NOT NULL DEFAULT 0 COMMENT {SQ}是否置顶: 1置顶 0普通{SQ},
  {BT}publisher{BT} varchar(100) DEFAULT NULL COMMENT {SQ}发布者名称{SQ},
  {BT}publisher_id{BT} bigint(20) DEFAULT NULL COMMENT {SQ}发布者用户ID{SQ},
  {BT}status{BT} tinyint(1) NOT NULL DEFAULT 1 COMMENT {SQ}状态: 1正常 0下架{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  {BT}updated_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT {SQ}更新时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_type{BT} ({BT}type{BT}),
  KEY {BT}idx_is_top{BT} ({BT}is_top{BT}),
  KEY {BT}idx_created_at{BT} ({BT}created_at{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}公告表{SQ};

-- 2. 请假记录表
DROP TABLE IF EXISTS {BT}leave_record{BT};
CREATE TABLE {BT}leave_record{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}user_id{BT} bigint(20) NOT NULL COMMENT {SQ}用户ID{SQ},
  {BT}type{BT} varchar(20) NOT NULL DEFAULT {SQ}sick{SQ} COMMENT {SQ}请假类型: sick病假/personal事假/official公假/annual年假{SQ},
  {BT}start_date{BT} date NOT NULL COMMENT {SQ}开始日期{SQ},
  {BT}end_date{BT} date NOT NULL COMMENT {SQ}结束日期{SQ},
  {BT}reason{BT} varchar(1000) NOT NULL COMMENT {SQ}请假事由{SQ},
  {BT}status{BT} varchar(20) NOT NULL DEFAULT {SQ}pending{SQ} COMMENT {SQ}状态: pending待审批/approved已批准/rejected已驳回{SQ},
  {BT}reject_reason{BT} varchar(500) DEFAULT NULL COMMENT {SQ}驳回原因{SQ},
  {BT}approver_id{BT} bigint(20) DEFAULT NULL COMMENT {SQ}审批人ID{SQ},
  {BT}approve_time{BT} datetime DEFAULT NULL COMMENT {SQ}审批时间{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  {BT}updated_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT {SQ}更新时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_user_id{BT} ({BT}user_id{BT}),
  KEY {BT}idx_status{BT} ({BT}status{BT}),
  KEY {BT}idx_created_at{BT} ({BT}created_at{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}请假记录表{SQ};

-- 3. 实习信息表
DROP TABLE IF EXISTS {BT}internship{BT};
CREATE TABLE {BT}internship{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}user_id{BT} bigint(20) NOT NULL COMMENT {SQ}用户ID{SQ},
  {BT}company{BT} varchar(200) NOT NULL COMMENT {SQ}实习公司{SQ},
  {BT}role{BT} varchar(100) NOT NULL COMMENT {SQ}实习岗位{SQ},
  {BT}start_date{BT} date DEFAULT NULL COMMENT {SQ}实习开始日期{SQ},
  {BT}end_date{BT} date DEFAULT NULL COMMENT {SQ}实习结束日期{SQ},
  {BT}total_days{BT} int(11) NOT NULL DEFAULT 0 COMMENT {SQ}总实习天数{SQ},
  {BT}status{BT} tinyint(1) NOT NULL DEFAULT 1 COMMENT {SQ}状态: 1进行中 0已结束{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  {BT}updated_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT {SQ}更新时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_user_id{BT} ({BT}user_id{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}实习信息表{SQ};

-- 4. 实习打卡记录表
DROP TABLE IF EXISTS {BT}intern_checkin{BT};
CREATE TABLE {BT}intern_checkin{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}user_id{BT} bigint(20) NOT NULL COMMENT {SQ}用户ID{SQ},
  {BT}internship_id{BT} bigint(20) NOT NULL COMMENT {SQ}实习ID{SQ},
  {BT}date{BT} date NOT NULL COMMENT {SQ}打卡日期{SQ},
  {BT}checkin_time{BT} datetime NOT NULL COMMENT {SQ}打卡时间{SQ},
  {BT}location_addr{BT} varchar(300) DEFAULT NULL COMMENT {SQ}定位地址{SQ},
  {BT}log_content{BT} varchar(2000) DEFAULT NULL COMMENT {SQ}实习日志{SQ},
  {BT}status{BT} varchar(20) NOT NULL DEFAULT {SQ}normal{SQ} COMMENT {SQ}打卡状态: normal正常/late迟到{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_user_id{BT} ({BT}user_id{BT}),
  KEY {BT}idx_internship_id{BT} ({BT}internship_id{BT}),
  KEY {BT}idx_date{BT} ({BT}date{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}实习打卡记录表{SQ};

-- 5. 实习照片表
DROP TABLE IF EXISTS {BT}intern_photo{BT};
CREATE TABLE {BT}intern_photo{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}checkin_id{BT} bigint(20) NOT NULL COMMENT {SQ}打卡记录ID{SQ},
  {BT}image_url{BT} varchar(500) NOT NULL COMMENT {SQ}图片URL{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_checkin_id{BT} ({BT}checkin_id{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}实习照片表{SQ};

-- 6. 意见反馈表
DROP TABLE IF EXISTS {BT}feedback{BT};
CREATE TABLE {BT}feedback{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}user_id{BT} bigint(20) NOT NULL COMMENT {SQ}用户ID{SQ},
  {BT}type{BT} varchar(20) NOT NULL DEFAULT {SQ}suggestion{SQ} COMMENT {SQ}反馈类型: suggestion建议/bug问题反馈/complaint投诉/other其他{SQ},
  {BT}content{BT} text NOT NULL COMMENT {SQ}反馈内容{SQ},
  {BT}contact{BT} varchar(100) DEFAULT NULL COMMENT {SQ}联系方式{SQ},
  {BT}status{BT} varchar(20) NOT NULL DEFAULT {SQ}pending{SQ} COMMENT {SQ}状态: pending待处理/resolved已处理{SQ},
  {BT}reply{BT} varchar(1000) DEFAULT NULL COMMENT {SQ}回复内容{SQ},
  {BT}reply_time{BT} datetime DEFAULT NULL COMMENT {SQ}回复时间{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  {BT}updated_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT {SQ}更新时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_user_id{BT} ({BT}user_id{BT}),
  KEY {BT}idx_status{BT} ({BT}status{BT}),
  KEY {BT}idx_created_at{BT} ({BT}created_at{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}意见反馈表{SQ};

-- 7. 动态码表
DROP TABLE IF EXISTS {BT}dynamic_code{BT};
CREATE TABLE {BT}dynamic_code{BT} (
  {BT}id{BT} bigint(20) NOT NULL AUTO_INCREMENT COMMENT {SQ}主键ID{SQ},
  {BT}course_id{BT} bigint(20) NOT NULL COMMENT {SQ}课程ID{SQ},
  {BT}dynamic_code{BT} varchar(10) NOT NULL COMMENT {SQ}动态码(6位数字){SQ},
  {BT}session_id{BT} varchar(64) NOT NULL COMMENT {SQ}会话ID{SQ},
  {BT}duration{BT} int(11) NOT NULL DEFAULT 60 COMMENT {SQ}有效时长(秒){SQ},
  {BT}expires_at{BT} datetime NOT NULL COMMENT {SQ}过期时间{SQ},
  {BT}status{BT} varchar(20) NOT NULL DEFAULT {SQ}active{SQ} COMMENT {SQ}状态: active有效/expired已过期{SQ},
  {BT}created_at{BT} datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT {SQ}创建时间{SQ},
  PRIMARY KEY ({BT}id{BT}),
  KEY {BT}idx_course_id{BT} ({BT}course_id{BT}),
  KEY {BT}idx_dynamic_code{BT} ({BT}dynamic_code{BT}),
  KEY {BT}idx_session_id{BT} ({BT}session_id{BT})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT={SQ}动态码表{SQ};

-- =============================================
-- 插入测试数据
-- =============================================

-- 测试公告
INSERT INTO {BT}announcement{BT}({BT}title{BT}, {BT}type{BT}, {BT}summary{BT}, {BT}content{BT}, {BT}department{BT}, {BT}is_top{BT}, {BT}publisher{BT}, {BT}publisher_id{BT}, {BT}status{BT}) VALUES
({SQ}计算机学院学术讲座：人工智能前沿{SQ}, {SQ}academic{SQ}, {SQ}本次讲座邀请到清华大学张教授讲解AI领域最新进展...{SQ}, {SQ}<p>讲座主题：人工智能前沿技术与发展趋势</p><p>主讲人：清华大学 张教授</p><p>时间：2026年6月15日 14:00-16:00</p><p>地点：计算机学院学术报告厅</p>{SQ}, {SQ}计算机学院{SQ}, 0, {SQ}计算机学院{SQ}, 1, 1),
({SQ}2026届毕业生就业双选会公告{SQ}, {SQ}activity{SQ}, {SQ}为促进毕业生就业，学校将于6月20日举办大型双选会...{SQ}, {SQ}<p>为促进毕业生就业，学校将于6月20日在体育馆举办大型双选会。</p><p>届时将有100余家企业参会，提供岗位2000余个。</p>{SQ}, {SQ}就业指导中心{SQ}, 0, {SQ}就业指导中心{SQ}, 1, 1);

-- 测试请假
INSERT INTO {BT}leave_record{BT}({BT}user_id{BT}, {BT}type{BT}, {BT}start_date{BT}, {BT}end_date{BT}, {BT}reason{BT}, {BT}status{BT}) VALUES
(1, {SQ}sick{SQ}, {SQ}2026-06-10{SQ}, {SQ}2026-06-11{SQ}, {SQ}感冒发烧，需要休息两天{SQ}, {SQ}approved{SQ}),
(1, {SQ}personal{SQ}, {SQ}2026-06-20{SQ}, {SQ}2026-06-20{SQ}, {SQ}家里有急事需要处理{SQ}, {SQ}pending{SQ});

-- 测试实习信息
INSERT INTO {BT}internship{BT}({BT}user_id{BT}, {BT}company{BT}, {BT}role{BT}, {BT}start_date{BT}, {BT}end_date{BT}, {BT}total_days{BT}) VALUES
(1, {SQ}字节跳动科技有限公司{SQ}, {SQ}前端开发实习生{SQ}, {SQ}2026-03-01{SQ}, {SQ}2026-08-31{SQ}, 180);

-- 测试实习打卡
INSERT INTO {BT}intern_checkin{BT}({BT}user_id{BT}, {BT}internship_id{BT}, {BT}date{BT}, {BT}checkin_time{BT}, {BT}location_addr{BT}, {BT}log_content{BT}, {BT}status{BT}) VALUES
(1, 1, {SQ}2026-06-01{SQ}, {SQ}2026-06-01 09:00:00{SQ}, {SQ}北京市海淀区字节跳动大厦{SQ}, {SQ}今天完成了首页模块的开发任务{SQ}, {SQ}normal{SQ}),
(1, 1, {SQ}2026-06-02{SQ}, {SQ}2026-06-02 08:55:00{SQ}, {SQ}北京市海淀区字节跳动大厦{SQ}, {SQ}修复了三个线上bug{SQ}, {SQ}normal{SQ}),
(1, 1, {SQ}2026-06-03{SQ}, {SQ}2026-06-03 09:10:00{SQ}, {SQ}北京市海淀区字节跳动大厦{SQ}, {SQ}参与需求评审会议{SQ}, {SQ}normal{SQ});

-- 测试意见反馈
INSERT INTO {BT}feedback{BT}({BT}user_id{BT}, {BT}type{BT}, {BT}content{BT}, {BT}contact{BT}, {BT}status{BT}) VALUES
(1, {SQ}suggestion{SQ}, {SQ}希望增加更多课程表显示模式{SQ}, {SQ}10086@qq.com{SQ}, {SQ}pending{SQ}),
(1, {SQ}bug{SQ}, {SQ}首页加载有时候会卡顿{SQ}, {SQ}{SQ}, {SQ}pending{SQ});

-- 测试动态码（一个已过期的示例）
INSERT INTO {BT}dynamic_code{BT}({BT}course_id{BT}, {BT}dynamic_code{BT}, {BT}session_id{BT}, {BT}duration{BT}, {BT}expires_at{BT}, {BT}status{BT}) VALUES
(1, {SQ}123456{SQ}, {SQ}test-session-001{SQ}, 60, DATE_ADD(NOW(), INTERVAL -1 HOUR), {SQ}expired{SQ});
'''

path = r'F:\sx\cs_edu0717\edu_check_backend\sql\init_new_tables.sql'
with open(path, 'w', encoding='utf-8') as f:
    f.write(sql)
print('SQL file written successfully')
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
    print(f'Total lines: {len(lines)}')
    print(f'Has backtick: {chr(96) in lines[0]}')
