# Execution Log

## 2026-08-06

- User intent: “增加一个QA的权限角色,如果没有的话,把QA的选线角色赋予admin,只有QA的权限角色可以看到QA页签”。
- Scope: QA 规程相关页签可见性、QA 权限角色/选线权限种子、admin 对应授权；不扩大到其它 PQC/班组长页签。
- Skill: `frontend-feature-delivery` + `database-schema-delivery`。
- BDD: QA 角色可见 QA 页签 -> Given 用户拥有 QA 权限角色 When 打开 PQC 组长/规程相关页面 Then 可以看到 QA 页签并进入 QA 内容。
- BDD: 非 QA 角色不可见 QA 页签 -> Given 用户没有 QA 权限角色 When 打开同一页面 Then QA 页签不可见且不能通过前端普通页签入口进入。
- BDD: admin 具备 QA 选线能力 -> Given 系统缺少独立 QA 选线角色 When admin 登录 Then admin 仍拥有 QA 选线/QA 页签所需权限，避免配置后 admin 看不到 QA。
- Inspection: QA 路由 `/mes/pro/process-pool/qa-regulation` 原先使用 `mes:pro-process-pool-team-leader:query`；动态菜单 `900434` 原先也复用班组长权限。
- Inspection: QA 规程保存/发布和手动选线仍走正式后端权限 `mes:qc-template:query/update`，因此 QA 角色需要同时获得 QA 菜单和现有后端 API 权限菜单。
- RED: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> FAIL, missing `20260806_mes_qa_role_permission_tab.sql`.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py` -> FAIL, missing QA role permission migration.
- Change: QA 路由权限改为 `mes:qa-inspection-regulation:query`。
- Change: `20260804_mes_edhr_qa_menu.sql` 的 QA 菜单种子改为正式 QA 权限，避免 fresh seed 继续复用班组长权限。
- Change: 新增 `20260806_mes_qa_role_permission_tab.sql`，恢复/创建 `qa` 角色，绑定 QA 菜单 `900434` 和 API 权限菜单 `5631/5633`，将 tenant 1 `admin` 用户加入 `qa` 角色，并软限制非 `qa` 角色的 QA 菜单授权。
- GREEN: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> PASS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py` -> PASS, 7 passed.
- GREEN: `node tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- <task-owned-files>` -> PASS, only Git line-ending warnings.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file <19-file dependency closure>` -> PASS, migrationCount=19.
- RED: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> FAIL, tenant 1 admin was not explicitly included when tenant packages do not include tenant 1 package_id=0.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py` -> FAIL, missing explicit `SELECT 1 AS tenant_id` contract.
- Change: `20260806_mes_qa_role_permission_tab.sql` now explicitly inserts tenant 1 into the QA role target tenant set before role creation and admin assignment.
- GREEN: `node tests\e2e\qa-regulation-role-permission-static.spec.cjs` -> PASS after tenant 1 inclusion.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py` -> PASS, 4 passed after tenant 1 inclusion.
- GREEN: Full related regression rerun passed: backend SQL tests 7 passed, eDHR QA menu static PASS, role-matrix QA static PASS, migration dependency closure PASS.
- Experience: 已按 `project-experience-consolidation` 合并长期经验到 `docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`，并在 `docs/experience-index.md` 增加可检索关键词路由。
- Note: Full SQL directory migration gate is blocked by pre-existing unrelated metadata issue in `20260805_erp_nas_table_auto_sync.sql` (`invalid type: schema,job`); the current migration dependency closure passes.
- Closeout blocker: shared worktree has many unrelated dirty changes before this task; no commit/push performed to avoid staging unrelated user/agent changes.
