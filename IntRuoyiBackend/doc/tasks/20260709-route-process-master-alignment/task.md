# Task: 工艺路线工序对应工序设置主数据

## 任务目标

- 将全部有效工艺路线中的工序关系统一对应到“工序设置”主数据的规范工序记录。
- 按 `tenant_id + code` 编码优先规则归并 `mes_pro_route_process.process_id`，让同编码路线工序引用同一个规范工序主数据。
- 不新增关系表，不删除或合并 `mes_pro_process` 主数据，不引入 fallback、降级或静默推断。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文档、SQL、测试读写必须显式 UTF-8，PowerShell 命令不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中 PowerShell 与数据库数据变更门禁。
- Database Schema Delivery：已读取 `database-schema-delivery` 与 `database-contract.md`；数据对齐 SQL 必须有 fail-fast、回滚说明和迁移验证。
- Backend API Delivery：已读取 `backend-api-delivery` 与 `backend-contract.md`；本任务不改 API，仅运行已有关联分页后端回归。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以现有 `mes_pro_route_process.process_id` 关系为唯一来源，按工序主数据编码归一，避免同编码路线关系散落到重复主数据。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 多路线工序统一对应 -> Given 同一工序编码存在多个工序主数据记录 / When 执行数据对应脚本 / Then 所有工艺路线关系统一指向该编码的规范工序记录。
- BDD: 全部路线范围 -> Given 多条工艺路线均包含同一编码工序 / When 执行数据对应脚本 / Then 所有路线下该编码工序均完成对应。
- BDD: 编码冲突阻塞 -> Given 同一编码存在多个不同工序名称 / When 执行数据对应脚本 / Then 脚本失败并输出冲突，不更新关系表。
- BDD: 缺少编码来源阻塞 -> Given 工艺路线工序关系没有有效 process_id 且无法取得编码 / When 执行数据对应脚本 / Then 脚本失败并说明缺少前置数据。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 数据契约测试。
- [x] M3：新增 fail-fast 数据对齐 SQL。
- [x] M4：运行本机只读预检、执行 SQL、复查关系数据。
- [x] M5：运行后端回归、数据库 evidence 校验、closeout preview 并处理提交边界。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_route_process_alignment_sql.py -q`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 本机 MySQL 只读预检、执行 SQL、复查 `mes_pro_route_process` 无空关联、无断链、可归一记录已更新。
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260709-route-process-master-alignment/database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-route-process-master-alignment --mode preview`

## Cleanup Keep

- `doc/tasks/20260709-route-process-master-alignment/database-schema-evidence.md`
- `doc/tasks/20260709-route-process-master-alignment/route-process-alignment-preview.tsv`
- `doc/tasks/20260709-route-process-master-alignment/route-process-alignment-post-verify.tsv`

## 当前状态

COMPLETED：已完成 RED/GREEN SQL 契约测试、本机只读预检和本机数据对齐。当前本机库已更新 18 条 `mes_pro_route_process.process_id`，复查后有效关系 421 条、空 `process_id` 0 条、断链 0 条、待归一 0 条；数据库证据校验和 closeout preview 均已通过。
