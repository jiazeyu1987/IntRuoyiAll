BDD: 缺失单人小时产能的工作站补 30 -> Given admin 租户工作站 `single_standard_hourly_capacity` 为空 / When 执行默认产能 SQL / Then 这些工作站产能更新为 `30.00`。
BDD: 已配置产能的工作站不被覆盖 -> Given admin 租户工作站已有非空产能 / When 执行默认产能 SQL / Then 原产能保持不变。
BDD: 其他租户不受影响 -> Given 非 admin 租户也存在工作站 / When 执行默认产能 SQL / Then 非 admin 租户数据不被修改。
RED: `python -m pytest script\tests\test_mes_workstation_default_single_capacity_sql.py` -> FAIL，目标 SQL 文件不存在。
GREEN: `python -m pytest script\tests\test_mes_workstation_default_single_capacity_sql.py` -> PASS，2 tests。
GREEN: 执行前 SQL 只读统计 -> PASS，admin 租户 `workstation_count=93`、`missing_single_capacity_count=89`、`already_30_count=1`；测试租户 `missing_single_capacity_count=90`、`already_30_count=0`。
GREEN: 执行 SQL `sql/mysql/20260609_mes_workstation_default_single_capacity_30.sql` -> PASS，`pending_update_count=89`、`updated_to_default_count=89`、`remaining_missing_count=0`。
GREEN: 执行后 SQL 校验 -> PASS，admin 租户 `missing_single_capacity_count=0`、`default_30_count=90`；测试租户 `default_30_count=0`。
GREEN: 回滚边界只读校验 -> PASS，admin 租户本次更新 89 条 `update_time=2026-06-09 11:10:06`，原有 1 条 30 为 `update_time=2026-06-09 11:05:21`。
