# Execution Log

## User Intent

- 用户要求：只有拥有 `PQC组长权限角色` 的人可以看到 `PQC组长` 这个页签。

## BDD Scenarios

- BDD: PQC组长页签仅对 PQC组长权限角色可见 -> Given 用户拥有 `pqc_leader_permission` 角色 / When 登录并加载动态菜单 / Then 可以看到 `PQC组长` 页签并访问 PQC 组长工作台。
- BDD: 非 PQC组长权限角色不可见 -> Given 用户没有 `pqc_leader_permission` 角色，即使拥有通用组长查询权限 / When 登录并加载动态菜单 / Then 不显示 `PQC组长` 页签。

## Evidence

- Frontend route before fix: `PQC组长` hidden static route meta permission was `mes:pro-process-pool-team-leader:query`.
- Menu SQL before fix: `900435 / PQC组长` used `mes:pro-process-pool-team-leader:query`.
- Local DB before fix: active `900435` bindings existed for `super_admin` and general `pqc_permission` in addition to `pqc_leader_permission`.
- Backend API precheck: PQC page still needs existing generic `mes:pro-process-pool-team-leader:*` controller permissions, so the visible menu permission must be separated from hidden API/button permissions.

## TDD Evidence

- RED: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> FAIL，`PQC leader dynamic menu must point at the standalone QA-side PQC leader page`，因为 `900435 / PQC组长` 仍使用 `mes:pro-process-pool-team-leader:query`。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_pqc_leader_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q` -> FAIL，缺少 `20260806_mes_pqc_leader_role_permission_tab.sql` 且基础菜单 SQL 中 `900435` 仍为通用组长查询权限。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS，`PASS eDHR QA dynamic menu static contract`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_pqc_leader_role_permission_tab_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py IntRuoyiBackend\script\tests\test_mes_qa_role_permission_tab_sql.py -q` -> PASS，`12 passed`。
- GREEN: 本机 Docker MySQL 应用 `20260806_mes_pqc_leader_role_permission_tab.sql` -> PASS。
- REGRESSION: 本机库复核 `900435 / PQC组长` permission 为 `mes:pro-process-pool-pqc-leader:query`，隐藏按钮 `900439 / PQC组长通用查询` permission 为 `mes:pro-process-pool-team-leader:query`，非 `pqc_leader_permission` 有效绑定 `900435` 计数为 `0`，admin 的 `pqc_leader_permission` 有效绑定计数为 `1`。
- REGRESSION: `git diff --check -- <本任务触碰文件>` -> PASS，仅输出 CRLF 提示，无空白错误。
- BLOCKER: `node IntRuoyiFronted\tests\e2e\mes-process-pool-team-leader-static.spec.js` -> FAIL，既有无关断言 `PQC 组长列表必须提供稳定选择器承载逐项提交内容` 未通过；该失败不来自本次权限页签收敛。
- BLOCKER: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260806-pqc-leader-tab-role-visibility\migration-policy-gate.json` -> FAIL，既有无关 SQL `20260805_erp_nas_table_auto_sync.sql` 的 release metadata `type=schema,job` 不合法。
- PROJECT EXPERIENCE: 已检查经验归宿，`docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁` 已覆盖本次角色菜单授权经验，无需新增长期经验文档。
