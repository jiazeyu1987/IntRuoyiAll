# 任务：发布必需 SQL product_name 幂等化

## 任务目标

修复测试服部署发布包时 `required-sql/20260604_dcc_controlled_file_product_name.sql` 重复执行报 `Duplicate column name 'product_name'` 的问题。该 SQL 必须在列已存在时安全跳过，在列不存在时添加列。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；SQL 仍必须执行，执行失败仍由发布脚本 fail fast。
- `是否从根因和长期维护角度解决`：是；迁移脚本本身变为幂等，符合发布包重复部署和断点重试需要。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 列不存在时添加 product_name -> Given 目标库 `dcc_controlled_file` 表不存在 `product_name` 列 / When 执行 `20260604_dcc_controlled_file_product_name.sql` / Then SQL 成功并添加该列。
- BDD: 列已存在时可重复执行 -> Given 目标库 `dcc_controlled_file` 表已存在 `product_name` 列 / When 再次执行同一 required SQL / Then SQL 成功结束，不报重复列错误。

## 里程碑

- [x] M1：定位 SQL 与现有测试覆盖，补 RED 复现重复执行失败。
- [x] M2：实现 SQL 幂等化。
- [x] M3：验证 SQL 结构与受影响发布脚本测试。

## 预期验证

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py`
- 真实 `deploy-release` 到测试服不再因 `product_name` 重复列失败。

## 当前状态

completed

## Current Status

completed

## 完成记录

- `20260604_dcc_controlled_file_product_name.sql` 已改为先查询 `information_schema.COLUMNS`，仅在 `product_name` 不存在时执行 `ALTER TABLE`。
- 已验证 SQL 静态契约、DCC Java schema 契约、发布包 required SQL 打包与应用顺序。
- 新发布包构建和测试服部署将在主发布目标中继续执行，用于验证本修复在真实发布链路中生效。
