# Verification Report

## Scope

AC-M20 “PQC 组长确认 PQC 检验单”修复，覆盖后端复核校验、PQC 任务确认闭环、结构化汇集明细、数据库迁移、前端操作入口和退回原因校验。

## Completed Verification

- PASS: `mvn -pl yudao-module-mes "-DskipTests" compile`
  - MES 模块生产代码编译通过。
- PASS: `mvn -pl yudao-module-mes -am -Pmes-ac-m20-pqc-review-targeted-tests "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Surefire: 22 tests, 0 failures, 0 errors, 0 skipped。
  - 覆盖 PQC 组长角色硬校验、退回原因、自我复核拒绝、重复/并发终态拒绝、正式 PQC task `SUBMITTED -> CONFIRMED`、结构化汇集明细和 schema/迁移契约。
- PASS: `node tests\e2e\team-leader-pqc-review-gate-static.spec.js`
  - 复核按钮仅对空状态或 `PENDING` 行显示。
  - 修正按钮仅对 `REJECTED` 行显示。
  - `openReview` / `openCorrection` 均有二次状态阻断。
  - `REJECTED` 复核提交前必须填写复核说明。
- PASS: `pnpm ts:check`
  - 前端类型检查通过。
- PASS: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
  - 全量 432 个迁移元数据通过。
  - 新增迁移 `20260805_mes_process_pool_ac_m20_pqc_review_closure` 被识别，依赖 `20260803_mes_process_pool_pqc_process_inspection_aggregation` 有效。
- PASS: `git diff --check`
  - 无 whitespace error；仅有 Git 在 Windows 下的 CRLF 转换提示。
- PASS: `mvn -pl yudao-server -am -Pmes-ac-m20-pqc-review-targeted-tests "-DskipTests" package`
  - 后端可打包并生成 `yudao-server\target\yudao-server-exec.jar`。
- PASS: 本任务 worktree 运行态
  - Backend `http://127.0.0.1:48083/actuator/health` 返回 `UP`。
  - Frontend `http://127.0.0.1:8083/` 返回 HTTP `200`。

## Blocked Verification

- BLOCKED: 真实写入型 Playwright E2E
  - 当前环境 `Get-ChildItem Env:RRM_*` 无任何任务专用变量。
  - `role-requirement-matrix-real-flow.e2e.js` 要求正式 `RRM_FRONTEND_URL` / `RRM_BACKEND_URL`、任务专用非生产测试租户、多角色账号/密码、`RRM_SIGNATURE_IDS_JSON`、生产订单、工艺路线、调拨/发货/补料/退料、批记录报表和 QA 规程版本 ID。
  - 缺少上述前置时不能证明 PQC 检验员真实提交、PQC 组长真实确认/退回、未确认/退回不算完成、自我确认和重复/并发确认在真实页面链路被拒绝。
- NOT RUN: `node tests\e2e\role-requirement-matrix-real-flow.e2e.js --check`
  - 该脚本硬编码写入旧任务 `doc/tasks/20260801-role-requirement-matrix-implementation` 和前端 `test-results/role-requirement-matrix-real-flow`；为避免修改非本任务证据目录，本轮未运行，只记录其源码声明的前置缺口。

## Resume 2026-08-05

- Runtime slot blocker resolved for this task by reserving `int_main slot 2`: frontend `8083`, backend `48083`.
- Previous Maven/JUnit and runtime blockers are resolved by the completed verification above.

## int_main Merge Verification 2026-08-05

- PASS: `git -C E:\IntRuoyi -c core.editor=true cherry-pick --continue`
  - Created `0626a3a0b fix: close AC-M20 PQC review loop` on `int_main`.
  - Branch runtime port guard passed for `int_main/int_main`: frontend `8081`, backend `48081`.
- PASS: `node tests\e2e\team-leader-pqc-review-gate-static.spec.js`
  - AC-M20 front-end gate still passes after fusion into `int_main`.
- PASS: `mvn -pl yudao-module-mes -am -Pmes-ac-m20-pqc-review-targeted-tests "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - Surefire: 25 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `pnpm ts:check`
  - Frontend type check passes in `E:\IntRuoyi\IntRuoyiFronted`.
- PASS: AC-M20 dependency-scoped migration gate
  - `run-release-migration-policy-gate.py` passed for the 14-file MES dependency chain ending at `20260805_mes_process_pool_ac_m20_pqc_review_closure`.
- PASS: `git show --check --pretty=short --no-renames HEAD`
  - No whitespace errors in commit `0626a3a0b`.
- BLOCKED: full `--sql-root IntRuoyiBackend\sql\mysql` migration gate on current `int_main`
  - Blocked by non-AC-M20 migration `20260805_erp_nas_table_auto_sync.sql` with invalid `type=schema,job`; AC-M20 scoped gate passed and this task did not modify that file.

## Result

AC-M20 代码级修复已完成并融合到 `int_main` 提交 `0626a3a0b`，通过聚焦后端、前端、迁移、打包、类型检查和运行态健康验证；但真实写入型 Playwright E2E 缺少任务专用 `RRM_*` 前置，当前不能标记为最终 `ACCEPTED`。当前结论为：代码修复成功，最终验收阻塞于真实 E2E 前置数据。
