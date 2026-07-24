# Execution Log: 补齐展厅前台大屏菜单管理条目

BDD: 菜单 seed 应声明前台大屏菜单 -> Given 展厅前端静态路由已存在 `display/screen/home` / When 校验 showroom 菜单 seed / Then seed 中必须包含 `前台大屏` 及其关联的大屏子路由条目。

BDD: 本地菜单库应同步出现前台大屏菜单 -> Given showroom 菜单 seed 已更新 / When 将脚本执行到当前本地数据库 / Then `system_menu` 中应出现前台大屏菜单记录，并且不得顺带写入 `system_role_menu`。

RED: `python -m pytest script/tests/test_showroom_sql_scripts.py -q` -> FAIL，`20260519_showroom_menu_seed.sql` 只声明了后台页签，缺少 `前台大屏` 与 `display/screen/*` 大屏子路由条目。

GREEN: 更新 `sql/showroom/20260519_showroom_menu_seed.sql`，新增 `980109-980114` 前台大屏菜单记录 -> PASS。
GREEN: `python -m pytest script/tests/test_showroom_sql_scripts.py -q` -> PASS，4 passed。
GREEN: 执行更新后的 `sql/showroom/20260519_showroom_menu_seed.sql` 到本地数据库 -> PASS。
GREEN: `SELECT id, name, path, component_name, visible, parent_id FROM system_menu WHERE id BETWEEN 980100 AND 980114 ORDER BY id` -> PASS，返回 `980109 前台大屏` 及 `980110-980114` 大屏隐藏子路由。
GREEN: `SELECT COUNT(*) FROM system_role_menu WHERE menu_id BETWEEN 980109 AND 980114` -> PASS，结果为 `0`。

NOTE: 本次按用户问题范围仅补齐 screen 大屏菜单，未顺带加入 Pad / 手机前台菜单。
*** Add File: D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-frontstage-screen-menu-management\bug-regression-evidence.md
# Bug Regression Evidence: 20260520-showroom-frontstage-screen-menu-management

## Bug Summary And Expected Behavior

- Bug: 展厅前台大屏页签虽然存在于前端静态路由，但不在菜单管理对应的 `system_menu` 数据里，因此无法在菜单管理中看到或维护。
- Expected: `system_menu` 应包含 `前台大屏` 及其大屏子路由条目，至少让菜单管理可见并可维护这套 screen 前台入口。

## Reproduction Command Or Path

- Command: `python -m pytest script/tests/test_showroom_sql_scripts.py -q`
- Symptom before the fix: 新增的前台大屏菜单 seed 断言失败，证明仓库里的 showroom 菜单脚本缺少 `display/screen/*` 记录。
- Runtime probe: `SELECT id, name, path, component_name, visible, parent_id FROM system_menu WHERE id BETWEEN 980100 AND 980114 ORDER BY id`

## Root Cause

- `sql/showroom/20260519_showroom_menu_seed.sql` 当初只补了展厅后台页签 `980100-980108`，没有把已经存在的 screen 大屏静态路由同步落到 `system_menu`。
- 因此前端路由可访问，但菜单管理依赖的数据库菜单树里没有对应条目。

## Regression Test Added Or Updated

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_sql_scripts.py`

## RED Command And Expected Failure

- `python -m pytest script/tests/test_showroom_sql_scripts.py -q`
- Expected failure before the fix: showroom 菜单 seed 中找不到 `前台大屏`、`display/screen/home` 与大屏子路由条目。
- `RED: python -m pytest script/tests/test_showroom_sql_scripts.py -q -> FAIL, showroom menu seed missing frontstage screen menu entries`

## GREEN Command And Passing Result

- `python -m pytest script/tests/test_showroom_sql_scripts.py -q` -> PASS
- `GREEN: python -m pytest script/tests/test_showroom_sql_scripts.py -q -> PASS`
- `GREEN: SELECT id, name, path, component_name, visible, parent_id FROM system_menu WHERE id BETWEEN 980100 AND 980114 ORDER BY id -> PASS`
- `GREEN: SELECT COUNT(*) FROM system_role_menu WHERE menu_id BETWEEN 980109 AND 980114 -> PASS`

## Risk And Regression Scope

- 只调整 showroom 菜单 seed 与当前本地数据库的菜单数据。
- 不修改角色授权，不改 Pad / 手机前台菜单，不改前端页面组件。
- 新增的 `980110-980114` 为大屏隐藏子路由条目，`visible=0`，不会直接新增新的可见侧边栏入口。

## Blockers And Follow-Up Actions

- 当前 `ruoyi-vue-pro` 工作区存在与本任务无关的未提交改动，本次不安全自动提交。

## Verification

- Verified by RED/GREEN pytest regression.
- Verified by direct database probe of `system_menu`.
- Verified that `system_role_menu` for the new menu ids remains `0`.
