# 执行日志：MES 重排可选理由补齐操作日志与本机 Schema 契约

## 2026-06-26

- 初始化任务：根据用户最新报错 `Field 'reason' doesn't have a default value`，确认问题已从前端必填校验转移到后端操作日志表落库约束。
- 已核对真实本地库：`SHOW CREATE TABLE mes_pro_schedule_order_operation_log;` 显示 `reason varchar(500) ... NOT NULL COMMENT '操作原因'`，与“手动重排理由可选”新契约冲突。
- 已核对源码迁移：`sql/mysql/20260624_mes_schedule_order_freeze_audit.sql` 当前同样把 `mes_pro_schedule_order_operation_log.reason` 定义为 `NOT NULL`。
- 已核对服务链路：`MesProAutoScheduleServiceImpl` 在 `replanApply` 成功写日志时直接把 `reqVO.getReason()` 传入 `MesProScheduleOrderOperationLogDO.reason`，当 replan reason 为空时会触发真实库约束失败。
- GREEN: experience-preflight -> PASS，已按命中经验文档 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md` 核对本轮只涉及本机数据库 schema 修正，不触发服务器发布、远端写入、备份或恢复动作。
- BDD: 手动重排 apply 缺少 reason 仍可写操作日志 -> Given 用户已生成有效重排预览且调用 replanApply 时 reason 为空 / When 后端写入 mes_pro_schedule_order_operation_log / Then reason 列允许为空，不得再因 Field 'reason' doesn't have a default value 失败。
- BDD: 自动排产 apply 继续要求 reason -> Given 用户发布自动排产结果 / When apply 请求缺少 reason / Then 服务层仍返回 PRO_SCHEDULE_ORDER_REASON_REQUIRED，不依赖数据库约束兜底。
- BDD: 真实本机库与新建库契约一致 -> Given 本机运行库或后续新建库需要创建/修复 mes_pro_schedule_order_operation_log / When 执行迁移 SQL 或本机重启 schema 保底流程 / Then reason 列必须允许 NULL，避免运行态与源码契约漂移。
- GREEN: mysql alter table -> PASS，已执行 `ALTER TABLE mes_pro_schedule_order_operation_log MODIFY COLUMN reason varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作原因';`，本机真实库修正完成。
- GREEN: show create table -> PASS，`SHOW CREATE TABLE mes_pro_schedule_order_operation_log;` 已回读到 `reason varchar(500) ... DEFAULT NULL COMMENT '操作原因'`。
- GREEN: python -X utf8 -m pytest script/tests/test_mes_schedule_order_freeze_audit_sql.py -q -> PASS，`3 passed`。
- GREEN: python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q -> PASS，`14 passed`。
- GREEN: curl.exe --fail --silent --show-error --max-time 15 http://127.0.0.1:48081/actuator/health -> PASS，返回 `{"status":"UP"}`。
