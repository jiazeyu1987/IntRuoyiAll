# 工作站班次小时运行库结构修复执行日志

## 2026-06-09

- BDD: 工作站查询包含班次小时字段 -> Given 应用查询 `mes_md_workstation` 并选择 `shift_hours` When 当前运行库已包含该列 Then SQL 查询成功返回工作站记录。
- BDD: 补字段不修改已有产能数据 -> Given 工作站已有 `single_standard_hourly_capacity` 等业务数据 When 执行 `ADD COLUMN shift_hours` Then 仅新增空列，已有字段值保持不变。
- RED: 用户页面/API 查询工作站 -> FAIL，错误为 `Unknown column 'shift_hours' in 'field list'`。
- RED: `SHOW COLUMNS FROM mes_md_workstation LIKE 'shift_hours'` -> FAIL，返回空结果；`single_standard_hourly_capacity` 存在。
- GREEN: `python -m pytest script\tests\test_mes_workstation_shift_hours_sql.py` -> PASS，2 tests，说明迁移脚本和初始化 SQL 已包含 `shift_hours`。
- RED: `SELECT id, code, name, single_standard_hourly_capacity, shift_hours FROM mes_md_workstation WHERE id IN (1,2,3,4,5) AND deleted = 0 AND tenant_id = 1;` -> FAIL，`Unknown column 'shift_hours' in 'field list'`。
- GREEN: `ALTER TABLE mes_md_workstation ADD COLUMN shift_hours decimal(10,2) NULL COMMENT '班次小时数' AFTER single_standard_hourly_capacity;` -> PASS，运行库补齐正式字段。
- GREEN: `SHOW COLUMNS FROM mes_md_workstation LIKE 'shift_hours'` -> PASS，返回 `shift_hours decimal(10,2) NULL`。
- GREEN: `SELECT id, code, name, single_standard_hourly_capacity, shift_hours FROM mes_md_workstation WHERE tenant_id=1 AND deleted=b'0' ORDER BY id LIMIT 5;` -> PASS，返回 `AUTO-WS-01`、`WS-B020`、`WS-B010`、`WS-B030`、`WS-B040` 等真实工作站记录。
- GREEN: 数据安全校验 -> PASS，执行前后 `workstation_count=93`、`single_capacity_non_null_count=93`、`single_capacity_sum=5579.06` 保持一致，`shift_hours_non_null_count=0`。
