# Task: 补齐展厅前台大屏菜单管理条目

## Goal

补齐 showroom 菜单 seed，使 `system_menu` 中除了后台页签，还包含 `前台大屏` 及其大屏子路由对应条目，方便在菜单管理中维护展厅前台大屏入口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\20260519_showroom_menu_seed.sql`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_sql_scripts.py`
- 当前本地 `ruoyi-vue-pro` 数据库 `system_menu`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-frontstage-screen-menu-management\**`

## Non-Scope

- 不修改前端静态路由定义
- 不新增角色授权
- 不改 Pad / 手机前台菜单，除非当前需求或验证强制要求
- 不改 showroom 业务接口

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-ai-cover-generation\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 无，可继续处理新的 showroom 菜单 seed 缺口

## Milestones

- [x] M1: 确认大屏静态路由与当前菜单 seed 差异，并创建任务文档。
- [x] M2: 先补 RED 测试，锁定“前台大屏不在菜单 seed”。
- [x] M3: 补齐菜单 seed 与本地库数据。
- [x] M4: 执行脚本验证与库内校验，记录 GREEN。
- [x] M5: 更新证据与收尾记录。

## Expected Verification

- `python -m pytest script/tests/test_showroom_sql_scripts.py -q`
- `SELECT id, name, path, component_name, visible FROM system_menu WHERE id BETWEEN 980100 AND 980114 ORDER BY id`

## Current Status

Completed on 2026-05-20.

## Final Verification Result

- PASS: `python -m pytest script/tests/test_showroom_sql_scripts.py -q`
- PASS: 执行更新后的 `sql/showroom/20260519_showroom_menu_seed.sql` 到本地 `ruoyi-vue-pro` 数据库
- PASS: `SELECT id, name, path, component_name, visible, parent_id FROM system_menu WHERE id BETWEEN 980100 AND 980114 ORDER BY id`
- PASS: `SELECT COUNT(*) FROM system_role_menu WHERE menu_id BETWEEN 980109 AND 980114`

## Blockers

- Commit blocker: 当前 `ruoyi-vue-pro` 工作区存在大量与本任务无关的未提交改动，本次不安全自动提交。
