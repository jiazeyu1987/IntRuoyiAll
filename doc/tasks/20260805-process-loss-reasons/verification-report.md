# Verification Report

## Summary

- Status: PASS for AC-D04 simulation and verification; ready for closeout.
- Code-level verification: PASS.
- Runtime smoke: PASS on worktree slot 12 (`8093/48093`).
- Runtime API verification: PASS on task-owned fixture data.
- Real write-type Playwright E2E: PASS with two production leaders and task-owned route-process fixture data.

## Acceptance Matrix

| Requirement | Evidence | Status |
|---|---|---|
| 生产组长只能看到自己通过“工序开始”配置获得权限的工序 | `MesRouteStartProductionLeaderAuthorizationServiceImpl` parses active route snapshot `routeStartProductionLeaders`; runtime API and real UI both show routeProcess `980628/980629` and exclude unauthorized `980630` | PASS |
| 一个工艺路线多个生产组长时，损耗原因数据共通 | LOSS reason create sets `leaderUserId=null` and binds to `routeProcessId`; unique key uses `route_process_id` not `leader_user_id` | PASS |
| 一个组长新增/修改/删除后，其他有权限组长可见 | Real UI: leader A created reason id `13`, leader B saw and updated it, leader A deleted it, leader B saw disabled state | PASS |
| 报工下拉来自后端配置，不是固定列表 | Frontend static contract verifies dropdown uses `runtimeConfig.defectReasons`; runtime config backend filters `routeProcessId + LOSS + enabled` | PASS |
| 禁用或删除原因不能用于新报工 | `MesFrontlineLossReasonValidatorImpl` rejects non-enabled reasons; submit test verifies no records are written | PASS |
| 跨工序原因提交被后端拒绝 | Validator compares submitted reason `routeProcessId` to current report `routeProcessId`; submit test covers rejection | PASS |
| 历史报工保留当时损耗原因快照 | Migration and payload splitter persist `loss_reason_id/code/name_snapshot`; submit test verifies snapshot payload | PASS |

## Commands

- GREEN: `node IntRuoyiFronted\tests\e2e\process-loss-reason-maintenance-static.spec.cjs` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderRuntimeConfigServiceTest,MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 19, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `pnpm.cmd ts:check` -> PASS.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\20260805-process-loss-reasons\migration-policy-gate.json` -> PASS.
- GREEN: backend/frontend/database evidence validators -> PASS.
- GREEN: `git diff --check` -> PASS, LF/CRLF warnings only.
- GREEN: `python -X utf8 .\doc\tasks\20260805-process-loss-reasons\acd04_simulate_environment.py --db-source local-config` -> PASS, generated task-owned fixture users/routes/reasons.
- GREEN: `python -X utf8 .\doc\tasks\20260805-process-loss-reasons\acd04_verify_runtime_api.py` -> PASS, generated `runtime-api-verification.json`.
- GREEN: `node --check .\doc\tasks\20260805-process-loss-reasons\acd04_verify_frontend_ui.e2e.cjs` -> PASS.
- GREEN: `node .\doc\tasks\20260805-process-loss-reasons\acd04_verify_frontend_ui.e2e.cjs` -> PASS, generated `frontend-ui-verification.json`.

## Runtime

- Backend health: `http://127.0.0.1:48093/actuator/health` -> `UP`.
- Frontend entry: `http://127.0.0.1:8093/` -> HTTP `200`.
- Default-login smoke: Playwright login to `http://127.0.0.1:8093/login?redirect=/index` with local default `芋道源码/admin` returned login business code `0`, permission business code `0`, and landed on `/index`; no password, token, cookie, or authorization header was recorded.
- Simulated task users: `acd04lead1`, `acd04lead2`, `acd04worker` under tenant `测试租户`; passwords were injected through process environment only and not recorded.
- Runtime API evidence: leaders only see routeProcess `980628/980629`; unauthorized routeProcess `980630` is absent; backend runtime config includes enabled LOSS reason and excludes disabled/deleted/cross-process reasons.
- Real UI evidence: production leader page shows loss reason tab, standard list, operation panel, shared add/update/delete flow, and no target HTTP/page errors.
- Worktree: `D:\IntRuoyiWorktree\20260805-process-loss-reasons`, branch `codex/20260805-process-loss-reasons`, slot `12`.

## Blockers

- None for AC-D04 simulation and verification.
- Remaining closeout work: cleanup preview/apply, project experience consolidation, implementation/closeout commits, and push.
- No fallback used: no mock data, no API-only acceptance, no default admin substitution, no frontend fixed list.
