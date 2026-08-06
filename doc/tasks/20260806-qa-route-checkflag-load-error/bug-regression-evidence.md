# Bug Regression Evidence

## Bug Summary

- QA 规程配置页选择 `ID / 球囊扩张压力泵 / 112` 后，路线范围加载报错：`当前工艺路线未标记唯一质检工序，请先在工艺路线中维护 checkFlag。`
- 期望行为：产品已正式绑定工艺路线且 ACTIVE 版本可读取时，如果缺少 `checkFlag` 但 BATCH 配置存在唯一启用的正式批记录绑定工序，或发布投影在路线工序上存在唯一 `batchRecordReportId/code/name`，页面应继续带出 QA 适用范围。

## Reproduction

- Path: QA 规程配置页，选择 DCC 项目代码对应产品 `ID / 球囊扩张压力泵 / 112`。
- Command: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs`

## Root Cause

- `loadQaRouteScopeFromRouteBinding` 原先先读取路线工序并立即调用 `resolveQaRouteProcessFromRoute`。
- `BATCH` 批记录配置在解析工序之后才加载，导致多工序路线没有唯一 `checkFlag=true` 时直接报错。
- 该顺序没有利用已发布路线上的正式批记录绑定来唯一定位 QA 规程适用工序。
- 用户复测仍报错后，补充发现已发布路线可能只通过 `MesProRouteProcessDO.batchRecordReportId` 返回主批记录投影；前端只检查 `BATCH` 配置数组时仍会漏掉这个正式来源。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs`
- Updated: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## RED

RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL
- Expected reason: `QA route resolver must model formal batch-record binding as the deterministic no-checkFlag source.`
RED: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL
- Expected reason: `QA route resolver must also honor the formal published route-process batchRecordReport projection.`

## GREEN

GREEN: `node tests\e2e\qa-regulation-route-checkflag-fallback-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-screenshot-pages-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
GREEN: `node tests\e2e\qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
GREEN: `pnpm ts:check` -> PASS
GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-route-checkflag-fallback-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-route-checkflag-load-error` -> PASS, only CRLF normalization warnings.

## Verification

- Target regression, adjacent QA static contracts, TypeScript check, and diff whitespace check passed.

## Risk And Regression Scope

- The fix intentionally does not use `formBindings` because form slots are not formal batch records.
- Multiple `checkFlag=true` remains fail-fast.
- Multiple enabled BATCH `batchRecordReports` candidates also fail-fast instead of guessing.
- Multiple route-process `batchRecordReportId/code/name` candidates fail-fast and require `checkFlag`.

## Blockers And Follow-Up

- Implementation and verification are complete.
- Commit/push is not performed because shared `int_main` has many unrelated dirty changes; creating the required dirty-worktree baseline would include non-task files and needs user confirmation.
