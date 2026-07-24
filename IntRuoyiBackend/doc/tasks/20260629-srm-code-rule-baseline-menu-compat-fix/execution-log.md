# Execution Log：修复 SRM 编码规则基线菜单兼容以放行测试服发布

BDD: SRM D7-1 基线 SQL 应兼容测试服旧菜单标题并完成就地升级 -> Given 测试服已存在 `system_menu.id=991000` 且 `path='/srm'` 但名称仍为 `供应商关系管理` / When 执行 `20260618_srm_d7_1_code_rule_baseline.sql` / Then SQL 应将该菜单标准化为 `SRM`，并继续完成 D7-1 菜单、权限、租户包与角色绑定，而不是在 clean menu id range precheck 直接失败。

GREEN: `ssh root@172.30.30.58 ... SELECT id, name, permission, type, parent_id, path, component, component_name FROM system_menu ...` -> PASS，测试服当前 `991000-991006` 已存在，其中 `991000.name='供应商关系管理'`、`path='/srm'`，`991001-991006` 与编码规则子菜单契约已基本存在。

GREEN: `ssh root@172.30.30.58 ... SELECT release_tag, migration_id, status, error_message FROM infra_release_migration WHERE migration_id='20260618_srm_d7_1_code_rule_baseline' ...` -> PASS，确认最新失败 releaseTag=`release-20260629-1745-committed-head-v3`，错误为 `Missing SRM clean menu id range; conflicting system_menu rows exist`。

RED: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_phase1_schema_sql.py` -> FAIL（修复前缺少“先把 `991000.name='供应商关系管理'` 标准化为 `SRM` 再进入 clean menu id range guard”的契约断言，且 SQL 本身会先因旧标题触发 fail fast）。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_phase1_schema_sql.py` -> PASS，已新增断言：D7-1 baseline SQL 必须在 clean menu id range guard 之前，将 `id=991000` 且 `path='/srm'`、旧标题为 `供应商关系管理` 的菜单标准化为 `SRM`。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py` -> PASS，SRM D7-D10 契约测试仍通过，说明此次兼容修复未破坏后续依赖 SQL 的发布契约。
