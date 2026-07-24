# Execution Log

## BDD

BDD: 识别系统中残留旧工序 ID -> Given 系统中存在工序主数据更新和历史排产快照，When 对代码及所有相关数据库引用执行只读审计，Then 应列出仍引用旧或已删除工序的记录、使用路径、风险等级和可复核证据。

## Evidence

- `GREEN: task-bootstrap -> PASS`：已读取项目规则、经验索引、PowerShell 编码经验、上一任务状态和独立验证门禁。
- `GREEN: previous-task-status -> PASS`：上一任务 `20260710-direct-work-report-import-process-route-fix` 状态为 `completed`。
- `GREEN: experience-preflight -> PASS`：数据库仅执行 `SHOW TABLES`、`DESCRIBE` 和参数化只读查询，未执行写入。
- `GREEN: schema-inventory -> PASS`：数据库共 721 张表；识别 56 张含精确 `process_id` / `route_process_id` 的表，并扩展检查 `next_process_id`、`applicable_process_id`、前后继及边关系字段。
- `GREEN: live-table-audit -> PASS`：排除备份、迁移和 legacy 表后，检查 35 张非备份 MES 表，查询错误数 0。
- `GREEN: current-route-process-integrity -> PASS`：当前非删除 `mes_pro_route_process.process_id` 未发现缺失或已删除工序引用。
- `GREEN: active-schedule-risk -> FAIL-FINDING`：活动状态排产快照存在 984 条旧工序与当前路线工序不一致。
- `GREEN: active-task-risk -> FAIL-FINDING`：活动排产关联草稿任务存在 662 条旧工序不一致，另有 2 条生产中任务的路线/工序组合失效。
- `GREEN: feedback-risk -> FAIL-FINDING`：存在 91 条非终态报工的路线/工序组合已失效。
- `GREEN: workstation-bom-risk -> FAIL-FINDING`：107 个非删除工位引用已删除工序；72 条非删除路线 BOM 使用失效路线/工序组合。
- `GREEN: code-path-review -> FAIL-FINDING`：普通报工、报工审批、物料消耗、IPQC 和计划实际日报仍存在直接使用易变 `process_id` 的路径。
- 初始动态全表探针因 PowerShell 引号错误及后续大结果查询超时失败；已终止本任务遗留的 Python/PowerShell 进程，改为受控脚本分阶段查询。未写入数据库，最终审计无查询错误。
- `GREEN: closeout-preview -> PASS`：确认仅删除一次性审计脚本，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- `GREEN: closeout-apply -> PASS`：一次性审计脚本已删除，未触及生产源码、测试或数据库。

## Current Status

审计完成，系统洁净性验证结果为 FAIL；任务收尾完成，状态为 `completed`。
