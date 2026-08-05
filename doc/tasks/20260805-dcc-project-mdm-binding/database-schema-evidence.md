# Database Schema Evidence

## Data Change Goal and Affected Entities

- 目标：为芋道源码租户下唯一高置信匹配的 DCC 项目代码写入 `dcc_project_code.product_master_id`。
- 受影响实体：`dcc_project_code`、`mdm_product`。

## Database Engine and Migration Tool

- 数据库：本机 Docker MySQL，业务库 `ruoyi-vue-pro`。
- 迁移工具：本任务不是 schema 迁移，不新增迁移文件；采用一次性受控数据更新事务。

## Schema, Migration, Fixture, Seed, Index, or Constraint Changes

- 不修改 schema、migration、fixture、seed、index 或 constraint。

## Data Safety Analysis

- 只更新 `tenant_id=1`、`deleted=b'0'`、当前 `product_master_id IS NULL` 且 MDM 产品在同租户、未删除、唯一同名的项目代码。
- 不删除、不覆盖已有绑定、不跨租户绑定、不绑定歧义或无匹配项目。

## Rollback or Recovery Plan

- 更新前导出命中行的 `id/project_code/project_name/product_master_id` 快照。
- 如需回滚，仅按本任务更新的 DCC 项目代码主键将 `product_master_id` 恢复为更新前值。

## BDD Scenarios

- BDD: QA 规程项目代码自动绑定产品 -> Given 芋道源码租户存在 DCC 项目代码且 MDM 产品主数据中有唯一同名产品 When 用户选择该项目代码 Then `product_master_id` 应绑定到该 MDM 产品，QA 规程配置可获得产品主数据。
- BDD: 歧义项目不静默绑定 -> Given 一个 DCC 项目代码可能对应多个 MDM 产品或只有模糊近似名称 When 执行批量对应 Then 系统不应写入绑定，必须在候选清单中保留待确认。

## RED Command and Expected Failure

- RED: `python -X utf8 -` -> FAIL, expected reason: `IDI` has a unique MDM product but `product_master_id` is empty before repair.
- Command: `python -X utf8 -` 只读断言 `IDI` 项目代码已经绑定 MDM 产品。
- Result: FAIL as expected。
- Expected reason: `IDI / 按压式球囊扩充压力泵` 在 MDM 中存在唯一同名产品 `14 / INT-15`，但更新前 `dcc_project_code.product_master_id` 为空。

## GREEN Command and Passing Result

- GREEN: `python -X utf8 -` -> PASS, transaction updated exactly 51 high-confidence bindings and final verification passed.
- Command: `python -X utf8 -` 生成候选并通过 MySQL 事务写入。
- Result: PASS，事务输出 `updated_count=51`。
- Rule counts: `exact_name=29`，`registration_suffix_removed=7`，`generic_prefix_removed=15`。
- Updated project ids: `125,126,128,129,130,131,132,134,137,143,145,146,147,151,156,158,159,162,167,168,169,170,171,175,176,182,185,187,188,189,190,191,198,204,207,209,210,211,216,223,225,226,227,229,230,231,233,237,239,241,243`。

## Migration Verification

- 不涉及 schema migration。
- Verification command: `python -X utf8 -` 复核统计、关键项目和引用完整性。
- Verification result: PASS。
- 芋道源码租户：`total=119`，`bound=51`，`unbound=68`。
- 测试租户：`total=133`，`bound=7`，`unbound=126`，未被本次任务改变。
- `invalid_bound_reference_count=0`。
- `tenant1_bound_to_non_enable_count=0`。
- 压力泵项目代码映射：`ID -> INT-12`，`IDE -> INT-13`，`IDE(CE) -> INT-13`，`IDE(FDA) -> INT-13`，`IDI -> INT-15`，`IDPR -> INT-14`。

## Blockers

- 68 条剩余未绑定项目不满足本次等值规则，需后续业务确认或补齐 MDM 正式名称/产品族规则后再绑定。
