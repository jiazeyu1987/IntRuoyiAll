BDD: 黑名单菜单不用冲突 ID -> Given 991104 已被 DCC 菜单占用 / When 执行 NAS 黑名单修复 SQL / Then 新菜单使用未冲突的新 ID，且 permission 为 `srm:nas-locator:config`。
BDD: srm_admin 获得黑名单菜单绑定 -> Given system_role 中存在 `srm_admin` / When 执行修复 SQL / Then 各租户 `srm_admin` 都获得新黑名单菜单绑定。
BDD: 持有 srm_admin 的测试租户账号可见按钮 -> Given `aoteman` 持有测试租户 `srm_admin` / When 前端加载权限菜单 / Then `get-permission-info` 中包含 `srm:nas-locator:config`。
INFO: previous-task-completed -> PASS，上一任务已明确根因为 991104 菜单 ID 冲突。
RED: pre-fix-db-contract -> FAIL，当前真实库 `system_menu.id=991104` 已被 DCC 占用，`permission='srm:nas-locator:config'` 缺失，`srm_admin` 不具备黑名单菜单。
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py -k nas_locator -q -> PASS
GREEN: local-runtime-db-apply -> PASS，已应用 `20260701_srm_t6_nas_locator_blacklist_config.sql`，新增 `991105` 并将其授权给 `srm_admin`。
GREEN: post-apply-readonly-verify -> PASS，测试租户套餐 `113` 已包含 `991105`，角色 `910294 / srm_admin` 已绑定 `991105`。
