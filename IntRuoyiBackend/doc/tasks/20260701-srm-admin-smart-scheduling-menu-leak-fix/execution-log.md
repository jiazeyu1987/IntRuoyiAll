# 执行日志：修复 SRM管理员 残留智能排产菜单授权

- 2026-07-01 Asia/Shanghai：已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/server-access.md`，确认本轮先做仓库内正式修复与契约测试，不直接修改测试服数据库。
- BDD: srm_admin 角色只能拥有 SRM 菜单树 -> Given 数据库中已存在 srm_admin 角色且角色上挂有历史非 SRM 菜单 / When 执行 SRM 管理员菜单收口 migration / Then 角色仅保留 SRM 菜单树授权，非 SRM 菜单授权被正式回收。
- BDD: 历史 role_id=910240 被其他角色占用时不应被改写成 srm_admin -> Given tenant 1 中 role_id=910240 已被非 SRM 历史角色占用且尚不存在 code=srm_admin 的角色 / When 执行 SRM 管理员 migration / Then 系统必须为 srm_admin 分配独立安全角色记录，而不是直接把 910240 改名复用。
- BDD: admin 继续通过 srm_admin 角色获得 SRM 菜单 -> Given tenant 1 存在启用 admin 用户 / When 执行修正后的 SRM 管理员 migration / Then admin 仍会绑定 srm_admin 角色并且仅获得 SRM 菜单树。
- 2026-07-01 Asia/Shanghai：只读根因已确认：测试服 `wangmin2(id=1526)` 通过 `role_id=910240 / srm_admin` 间接拿到 `menu_id=900120 / 智能排产`；`20260629_srm_admin_role_visibility.sql` 固定使用 `910240`，且不清除旧的非 SRM 菜单授权，导致角色改名后菜单污染继续存在。
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py -q` -> FAIL，旧 `20260629_srm_admin_role_visibility.sql` 仍固定依赖 `910240`，且缺少 `20260701_srm_admin_role_menu_scope_cleanup.sql` 补偿 migration。
- GREEN: `apply_patch` -> 更新 `sql\mysql\20260629_srm_admin_role_visibility.sql`，改为动态解析 `srm_admin` 角色 ID，并在补 SRM 菜单前软删除非 SRM 菜单授权。
- GREEN: `apply_patch` -> 新增 `sql\mysql\20260701_srm_admin_role_menu_scope_cleanup.sql`，补偿收口已落地环境的 `srm_admin` 菜单范围。
- GREEN: `apply_patch` -> 更新 `script\tests\test_srm_admin_role_visibility_sql.py` 并新增 `script\tests\test_srm_admin_role_scope_cleanup_sql.py`，覆盖动态角色解析与菜单收口合同。
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py -q` -> PASS，10 passed。
