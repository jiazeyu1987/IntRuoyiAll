# 不合格审批完整链路 E2E 验证报告

## Result

PASS。最新代码下已在独立 worktree `D:\IntRuoyiWorktree\20260902-nonconformance-review-full-e2e` 完成不合格审批 MVP 的真实页面完整链路验证、冻结三操作真实页面验证，以及用户新增的 `PQC生产放行` 与 `PQC组长 > PQC管理` 双入口 E2E 验证。

## Runtime Evidence

- Runtime profile: `int_main` worktree slot `7`。
- Frontend: `http://127.0.0.1:8088/` -> HTTP `200`。
- Backend: `http://127.0.0.1:48088/actuator/health` -> `{"status":"UP"}`。
- 重启后本地依赖容器已恢复：`int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1`。
- MinIO ready: `http://127.0.0.1:9000/minio/health/ready` -> HTTP `200`。

## Real E2E Evidence

- Full chain: `IntRuoyiFronted/output/playwright/nonconformance-review-mvp/20260902-yudao-17/result.json` -> PASS。
- Full chain batch: `900000001024`，`NCR-E2E-20260902-FIXTURE-03`。
- Full chain review ids: `19/20/21`。
- Full chain dispositions: `concession_release`、`rework`、`void`。
- Full chain verified through real login page, real menu/page flow, real material upload, QA opinion, QA signature, and domain trace page.
- Frozen actions: `IntRuoyiFronted/output/playwright/nonconformance-review-mvp/20260902-yudao-16/result.json` -> PASS。
- Frozen action results: 生产报工返回 `1040750474`；PQC提交返回 `1040750474`；PQC放行不可执行且写请求数为 `0`。
- Double entry read-only: `IntRuoyiFronted/output/playwright/nonconformance-review-mvp/20260902-yudao-20-entry-both-source-trace/result.json` -> PASS。
- Double entry batch: `900000000926`，`EDHRB-1785810846141`，work order `RRM-20260801-PP-MO-001`。
- `PQC生产放行` 入口进入同一不合格评审页，source 为 `PQC_RELEASE`，sourceId 为 `104`。
- `PQC组长 > PQC管理` 入口进入同一不合格评审页，source 为 `PQC_SUBMISSION`，sourceId/eventId 为 `160`。
- Double entry write guard: review create/dispose 写请求数 `0`，pageErrors `0`，targetConsoleErrors `0`。
- Double entry screenshots: `entry-pqc-release.png`、`entry-pqc-management.png`、`trace.zip` 和 24 张逐步截图已生成。
- int_main reboot continuation: `IntRuoyiFronted/output/playwright/nonconformance-review-mvp/20260902-int-main-04-entry-both-after-reboot/result.json` -> PASS。
- int_main runtime: `http://127.0.0.1:8081/` -> HTTP `200`；`http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。
- int_main `PQC生产放行` 入口进入同一不合格评审页，source 为 `PQC_RELEASE`，sourceId 为 `104`，batchExecutionId 为 `900000000926`。
- int_main `PQC组长 > PQC管理` 入口进入同一不合格评审页，source 为 `PQC_SUBMISSION`，sourceId/eventId 为 `160`，batchExecutionId 为 `900000000926`。
- int_main double entry write guard: review create/dispose 写请求数 `0`，pageErrors `0`，targetConsoleErrors `0`。
- int_main screenshots: `entry-pqc-release.png`、`entry-pqc-management.png`、`trace.zip` 和 24 张逐步截图已生成，逐步截图索引为 `IntRuoyiFronted/output/playwright/nonconformance-review-mvp/step-screenshots/20260902-int-main-04-entry-both-after-reboot/step-screenshots-index.md`。

## Database Evidence

- Batch `900000001024`: status `60`，关联工单 `980008` 保持 `temporary_frozen=1`。
- Review `19`: source `PQC_RELEASE`，status `closed`，disposition `concession_release`，材料/意见/签名/追溯快照均有值。
- Review `20`: source `PQC_SUBMISSION`，status `closed`，disposition `rework`，材料/意见/签名/追溯快照均有值。
- Review `21`: source `PQC_RELEASE`，status `closed`，disposition `void`，材料/意见/签名/追溯快照均有值。
- Review `18`: frozen action verification review，status `closed`，disposition `void`。
- Target pending review count: `0`。
- Frozen action fixture cleanup: 临时员工有效数 `0`，临时路线版本 `740` 有效数 `0`。

## Automated Verification

- `node doc\tasks\20260902-nonconformance-review-full-e2e\fixture-contract.spec.cjs` -> PASS。
- `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` -> PASS。
- `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` -> PASS。
- `pwsh -NoProfile -File doc\tasks\20260902-nonconformance-review-full-e2e\export-trace-step-screenshots.ps1` -> PASS，逐步截图共 `136` 张，其中双入口 run 为 `24` 张。
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrNonconformanceReviewApplicationScopeTest,MesProcessPoolEventFreezeGateTest" test` -> PASS，Tests run: 8。
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProcessPoolEventFreezeGateTest,MesProcessPoolEventServiceTest,MesProcessPoolPqcEventTest,MesP0FrontlineSubmitIdempotencyTest" test` -> PASS，Tests run: 15。
- `validate_backend_api.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\backend-api-evidence.md` -> PASS。
- `validate_database_schema.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\database-schema-evidence.md` -> PASS。
- `validate_bug_regression.py --evidence doc\tasks\20260902-nonconformance-review-full-e2e\bug-regression-evidence.md` -> PASS。
- `git diff --check` -> PASS，仅报告 Windows CRLF 提示，无空白错误。
- `node --check tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js` in `int_main` -> PASS。
- `node tests\e2e\edhr-nonconformance-review-mvp-static.spec.js` in `int_main` -> PASS。
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` in `int_main` -> PASS，frontend `8081`，backend `48081`。

## Issues Found And Fixed

- ISSUE-001: Vite first-load dependency optimization caused the first Playwright run to reload back to `/index`; resolved by rerunning after runtime warmup and recording it as runtime warmup, not a product failure.
- ISSUE-002: Route code filter returned both `RT000028` and `RT000028-IDI`; fixed E2E locator/response matching to select exact route code.
- ISSUE-003: Shared route could not create a fresh batch because formal cell-rule confirmation blocked it with `1040750243`; created transaction-guarded task-owned fixture instead of mutating shared template state.
- ISSUE-004: Current frontline production/PQC submit paths did not call unified nonconformance freeze gate; fixed backend service and covered by regression tests.
- ISSUE-005: Batch-sourced reviews did not freeze associated work order or keep it frozen after void; fixed creation/disposition state handling and covered by regression tests.

## Acceptance Mapping

- Unified entry from PQC submission and PQC release: PASS via `20260902-yudao-17` with sources `PQC_SUBMISSION` and `PQC_RELEASE`；PASS via `20260902-yudao-20-entry-both-source-trace` for the two explicit frontend entrances `PQC生产放行` and `PQC组长 > PQC管理`。
- Active work order/batch freeze: PASS via backend regression and DB `temporary_frozen` evidence。
- Frozen blocks production reporting, PQC submit, PQC release: PASS via `20260902-yudao-16`。
- QA list/detail/material/opinion/signature/disposition: PASS via `20260902-yudao-17`。
- Concession release returns to main flow: PASS via review `19` closed with `unfrozen_at` and disposition `concession_release`。
- Rework returns directly to main flow without rework confirmation: PASS via review `20` closed with `unfrozen_at` and disposition `rework`。
- Void becomes read-only trace terminal: PASS via review `21`，batch status `60`，and work order kept frozen。
- Trace includes reason, report/material, disposition, QA signature, freeze/unfreeze or void time: PASS via domain trace page assertions and DB `trace_snapshot_json`。

## Remaining Closeout

Verification is complete. 当前 `int_main` 与任务分支均位于 `9ac7af7df fix: complete nonconformance review e2e flow`，但 `int_main` 仍领先 `origin/int_main` 且存在其它非本任务脏改；cleanup preview/apply、推送和最终收尾提交仍受当前项目 closeout 规则约束。
