# 工作站单人小时产能默认补 30

## 任务目标

为 `芋道源码/admin` 租户中当前缺少 `单人标准小时产能` 的工序工作站补默认值 `30`。当前系统中单人小时产能字段归属 `mes_md_workstation.single_standard_hourly_capacity`，不是工序主表字段；本任务只补工作站缺失值，不新增字段、不新增资源表、不覆盖已有非空产能。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-worker-capacity-edit/task.md`。
- 检查结果：该任务已标记 `completed`，本任务在其确认的工作站人工产能模型上做受保护数据修正。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只做显式 SQL 数据修正，不改变应用兜底逻辑。
- `是否从根因和长期维护角度解决`：是。把默认人工小时产能写入正式工作站字段，前端和排产接口都读取同一底层数据。
- `是否存在临时补丁或绕过`：否。SQL 带租户、未删除、空值条件；已有配置不覆盖。

## BDD 场景

- BDD: 缺失单人小时产能的工作站补 30 -> Given admin 租户工作站 `single_standard_hourly_capacity` 为空 / When 执行默认产能 SQL / Then 这些工作站产能更新为 `30.00`。
- BDD: 已配置产能的工作站不被覆盖 -> Given admin 租户工作站已有非空产能 / When 执行默认产能 SQL / Then 原产能保持不变。
- BDD: 其他租户不受影响 -> Given 非 admin 租户也存在工作站 / When 执行默认产能 SQL / Then 非 admin 租户数据不被修改。

## 里程碑

- [x] M1：添加 RED 静态测试，锁定 SQL 必须只补 admin 租户空值。
- [x] M2：编写受保护 SQL 脚本并运行测试。
- [x] M3：执行前只读统计，确认待补数据范围。
- [x] M4：在本机数据库执行 SQL，执行后校验空值归零且非 admin 不受影响。
- [x] M5：更新任务证据并提交本任务文件。

## 预期验证

- `python -m pytest script\tests\test_mes_workstation_default_single_capacity_sql.py`
- 执行前 SQL 只读统计：admin 租户待补工作站数。
- 执行后 SQL 校验：admin 租户 `single_standard_hourly_capacity IS NULL` 的未删除工作站为 `0`，本次更新行均为 `30.00`。

## 当前状态

completed

## 完成记录

- 已新增受保护 SQL：`sql/mysql/20260609_mes_workstation_default_single_capacity_30.sql`。
- SQL 只更新 `tenant_id=1`、`deleted=b'0'`、`single_standard_hourly_capacity IS NULL` 的工作站。
- 执行前：admin 租户未删除工作站 93 个，其中缺失单人小时产能 89 个，已有 30 值 1 个。
- 执行结果：`pending_update_count=89`、`updated_to_default_count=89`、`remaining_missing_count=0`。
- 执行后：admin 租户缺失数 0，30 值数量 90；测试租户 30 值数量仍为 0，未受影响。

## 最终验证

- RED: `python -m pytest script\tests\test_mes_workstation_default_single_capacity_sql.py` -> FAIL，目标 SQL 文件不存在。
- GREEN: `python -m pytest script\tests\test_mes_workstation_default_single_capacity_sql.py` -> PASS，2 tests。
- GREEN: 执行前 SQL 只读统计 -> PASS，admin 待补 89 条，测试租户不在更新范围。
- GREEN: 执行 SQL -> PASS，`pending_update_count=89`、`updated_to_default_count=89`、`remaining_missing_count=0`。
- GREEN: 执行后 SQL 校验 -> PASS，admin 缺失数 0，测试租户 30 值数量 0。
- GREEN: 回滚边界只读校验 -> PASS，本次 89 条 `update_time=2026-06-09 11:10:06`，原有 1 条 30 未混入。

## Cleanup Keep

- `doc/tasks/20260609-workstation-default-single-capacity-30/database-data-evidence.md`
