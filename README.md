# 校园考勤助手

校园考勤管理项目，包含微信小程序、Spring Boot 后端和 PC 管理端。

## 功能

- 查寝、上课和实习三类打卡
- GPS 定位、人脸识别和动态码签到
- 请假申请与审批
- 公告发布与查看
- 教师端学生管理与考勤统计
- 管理端数据看板
- 智能问答、意见反馈和报修

## 目录

- `edu_check_front/`：微信小程序
- `edu_check_backend/`：Spring Boot 后端
- `pc-dashboard/`：PC 管理端
- `校园考勤助手-项目答辩PPT.pptx`：项目答辩材料

## 后端启动

1. 准备 Java 17、Maven、MySQL 8 和 Redis。
2. 将 `edu_check_backend/src/main/resources/application.example.yml` 复制为 `application.yml`。
3. 根据本地环境填写数据库、JWT、DeepSeek 和百度云配置，或通过同名环境变量注入。
4. 执行 `edu_check_backend/sql/init.sql` 初始化数据库。
5. 在 `edu_check_backend` 目录执行 `mvn spring-boot:run`。

## 小程序启动

使用微信开发者工具导入 `edu_check_front/`，修改 `utils/request.js` 中的后端地址后运行。

真实的 `application.yml`、上传文件、日志和本地 IDE 配置不会提交到仓库。
