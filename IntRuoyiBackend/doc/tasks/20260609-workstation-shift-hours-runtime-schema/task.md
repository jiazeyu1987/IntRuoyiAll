# 工作站班次小时运行库结构修复

## 任务目标

修复本机运行库查询 `mes_md_workstation.shift_hours` 报 `Unknown column 'shift_hours' in 'field list'` 的问题。代码与迁移脚本已经包含工作站 `shift_hours` 字段，本任务只让当前 MySQL 运行库结构与代码模型一致，不修改业务数据。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-worker-capacity-edit/task.md`。
- 检查结果：该任务已标记 `completed`，并记录了 `mes_md_workstation.shift_hours` 的正式字段设计与静态 SQL 验证。本次问题是运行库未执行对应 DDL。
- 备注：仓库中存在不相关的 `20260609-edhr-work-order-selector-search-bug` 任务文档处于 `in_progress`，本次用户反馈为工作站 schema 运行错误，修复不依赖该 eDHR 任务，不修改其业务代码或证据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺字段按 schema 根因修复，不在代码中隐藏异常。
- `是否从根因和长期维护角度解决`：是。让运行库表结构与已提交的 DO、VO、初始化 SQL 保持一致。
- `是否存在临时补丁或绕过`：否。执行正式迁移字段 `shift_hours`，不改查询、不删字段、不伪造默认成功。

## BDD 场景

- BDD: 工作站查询包含班次小时字段 -> Given 应用查询 `mes_md_workstation` 并选择 `shift_hours` / When 当前运行库已包含该列 / Then SQL 查询成功返回工作站记录。
- BDD: 补字段不修改已有产能数据 -> Given 工作站已有 `single_standard_hourly_capacity` 等业务数据 / When 执行 `ADD COLUMN shift_hours` / Then 仅新增空列，已有字段值保持不变。

## 里程碑

- [x] M1：复现运行库缺失 `shift_hours` 列。
- [x] M2：执行非破坏性 DDL 补齐 `mes_md_workstation.shift_hours`。
- [x] M3：验证原报错 SQL 可正常查询，静态迁移测试仍通过。
- [x] M4：记录证据并提交本任务文档。

## 预期验证

- `SELECT ... shift_hours ... FROM mes_md_workstation ...` 执行前失败，执行后成功。
- `SHOW COLUMNS FROM mes_md_workstation LIKE 'shift_hours'` 返回字段。
- `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py` 通过。

## 当前状态

completed

## 完成记录

- 已确认项目代码、测试建表 SQL、MySQL 初始化 SQL 和迁移脚本均包含 `shift_hours`。
- 已确认本机运行库 `mes_md_workstation` 缺少 `shift_hours`，直接查询该列复现 `Unknown column 'shift_hours' in 'field list'`。
- 已执行非破坏性 DDL：`ALTER TABLE mes_md_workstation ADD COLUMN shift_hours decimal(10,2) NULL COMMENT '班次小时数' AFTER single_standard_hourly_capacity;`。
- 执行后 `SHOW COLUMNS` 返回 `shift_hours decimal(10,2) NULL`。
- 执行后工作站数量仍为 `93`，非空单人小时产能数仍为 `93`，单人小时产能汇总仍为 `5579.06`；新增 `shift_hours` 当前非空数为 `0`，未回填业务数据。

## 最终验证

- RED: `SELECT id, code, name, single_standard_hourly_capacity, shift_hours FROM mes_md_workstation WHERE id IN (1,2,3,4,5) AND deleted = 0 AND tenant_id = 1;` -> FAIL，`Unknown column 'shift_hours' in 'field list'`。
- GREEN: `SHOW COLUMNS FROM mes_md_workstation LIKE 'shift_hours'` -> PASS，返回 `shift_hours decimal(10,2) NULL`。
- GREEN: `SELECT id, code, name, single_standard_hourly_capacity, shift_hours FROM mes_md_workstation WHERE tenant_id=1 AND deleted=b'0' ORDER BY id LIMIT 5;` -> PASS，返回真实工作站记录。
- GREEN: `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py` -> PASS，2 tests。

## Cleanup Keep

- `doc/tasks/20260609-workstation-shift-hours-runtime-schema/database-schema-evidence.md`
