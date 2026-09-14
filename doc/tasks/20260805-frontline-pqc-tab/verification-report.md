# Verification Report

## Scope

- 目标：将批次执行内部 `PQC填写` tab 拆出为独立动态菜单页签 `一线PQC`，并证明本机 `芋道源码/admin` 可见。
- 非目标：不修改 PQC 检验项、结构化 `itemResults`、QA 规程、设备编号、复核或汇集事实链路。

## Implementation Evidence

- 前端批次执行内部 tab 已移除 `PQC填写` 和 `pqc` 路由映射。
- 独立页面 `BatchPqcFillPage.vue` 渲染标题 `一线PQC`，保留正式 `<FrontlineFixedTemplatePanel mode="pqc" />`。
- 动态路由标题更新为 `一线PQC`，仍指向 `/mes/pro/feedback/edhr-batch-pqc-fill` 与 `MesProEdhrBatchPqcFill`。
- 菜单迁移新增 `system_menu.id=900438`，名称 `一线PQC`，组件 `mes/pro/edhr-batch/BatchPqcFillPage`，并绑定租户套餐和 admin 角色。
- 本轮补修迁移 `INSERT` 的 `type/sort` 列对齐：新增 `2 AS type, 4 AS sort`，防止真实执行列数错位。

## Database Evidence

- 本机容器：`int-ruoyi-mysql`，库 `ruoyi-vue-pro`。
- 已执行依赖迁移：`20260804_mes_edhr_qa_menu.sql`。
- 已执行目标迁移：`20260805_mes_edhr_frontline_pqc_menu.sql`。
- 菜单核对：`id=900438`，`HEX(name)=E4B880E7BABF505143`，`name=一线PQC`，`sort=4`，`visible=1`，`deleted=0`。
- admin 绑定核对：`super_admin` 租户 1、`tenant_admin` 租户 121、`super_admin` 租户 122 均已绑定 `menu_id=900438`；默认登录 `芋道源码/admin` 属于本机验证范围。
- 租户套餐核对：包含 eDHR 父菜单的目标套餐已包含 `menu_id=900438`。

## Verification

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> FAIL，新增断言捕获 `20260805_mes_edhr_frontline_pqc_menu.sql` 缺少 `2 AS type`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> PASS，3 passed。
- GREEN: `node tests\e2e\edhr-frontline-pqc-tab-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q` -> PASS，6 passed。
- GREEN: `node --check tests\e2e\edhr-frontline-pqc-menu-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-frontline-pqc-menu-real.e2e.js` -> PASS，`permissionMenuVisible=true`、`pageMenuVisible=true`、`internalTabCount=0`、`legacyPqcTabCount=0`、`writeRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file <19-file dependency closure> --output doc\tasks\20260805-frontline-pqc-tab\migration-policy-gate.json` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-frontline-pqc-tab/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS。

## Blockers

- Full `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql` remains blocked by unrelated `20260805_erp_nas_table_auto_sync.sql` metadata `type=schema,job`.
- Full `git diff --check` remains blocked by unrelated conflict markers in `MesPqcProcessInspectionAggregationServiceTest.java`, `MesTeamLeaderSubmissionReviewServiceTest.java`, and `docs/powershell-memory.md`.
- These blockers are outside this task-owned PQC tab scope; task-owned verification passed.
