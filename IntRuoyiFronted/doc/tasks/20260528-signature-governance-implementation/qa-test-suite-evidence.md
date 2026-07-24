# QA Test Suite Evidence：电子签名治理 E2E 门禁

## Scope And Feature Under Test

- Scope：电子签名治理前端工作台和四个真实用户路径 E2E 门禁。
- Feature：长期留存、周期审阅、CSV质量包、统一策略。
- Release recommendation：GO for task-specific commit. Final reviewer Round 4 passed; cleanup preview has been executed and temporary E2E/runtime artifacts have been removed. Worktree fast-forward merge/delete remains blocked by branch/main-worktree state and is not claimed complete.

## Requirement-To-Test Matrix

| Requirement | E2E file | Expected user path | Current result |
| --- | --- | --- | --- |
| `AC-RR-001` - `AC-RR-005` | `tests/e2e/signature-governance-retention-recovery.e2e.js` | 登录测试租户，进入 `/signature-governance`，触发长期留存预检、DCC回执、eDHR回执和恢复演练 | PASS with real MinIO/Object Lock samples |
| `AC-PR-001` - `AC-PR-006` | `tests/e2e/signature-governance-periodic-review.e2e.js` | 登录测试租户，进入周期审阅页签，填写真实 review projection source 并创建审阅批次 | PASS |
| `AC-CSV-001` - `AC-CSV-004` | `tests/e2e/signature-governance-csv-package.e2e.js` | 登录测试租户，进入 CSV 质量包页签，填写材料、追溯、培训、变更控制、QA批准并评估 release gate | PASS |
| `AC-CMP-001` - `AC-CMP-007` | `tests/e2e/signature-governance-policy.e2e.js` | 登录测试租户，进入统一策略页签，确认 DCC/eDHR/Showroom/IntAuth 同源且 `ready=true` | PASS |

## Test Types Used

- Static E2E contract：verifies four E2E files, shared helper, required env guards, real endpoint strings and response assertions.
- Actual tenant guard：verifies the login form's selected tenant before submitting credentials, so a configured test-tenant E2E cannot silently run against the page's default tenant.
- Syntax verification：`node --check` on helper and four E2E entry files.
- Real Playwright E2E：executed against current worktree frontend/backend with test tenant data.

## Test Data And Fixtures

- Base URL：`http://127.0.0.1:18198`
- Backend：current backend worktree on `http://127.0.0.1:48198`
- Tenant/account：`测试租户 / aoteman / admin123`
- MinIO bucket：`signature-governance-e2e-20260528`
- DCC evidence sample：sourceId `302`, objectKey `dcc/signature-302.txt`
- eDHR archive sample：sourceId `9`, objectKey `edhr/archive-9.txt`
- Recovery sample：backupId `backup-sg-20260528-001`, runtime `isolated-minio-restore-20260528`

The E2E helper blocks protected live tenant names and writes scenario result JSON under `test-results/signature-governance/`.

## RED Evidence

RED: `node tests\e2e\signature-governance-e2e-static.spec.js` -> FAIL, expected reason: `tests/e2e/signature-governance-real-flow-helper.js` and four E2E entry files were missing.

RED: `node tests\e2e\signature-governance-e2e-static.spec.js` -> FAIL, expected reason: helper did not assert the actual selected tenant before login.

RED: `node tests\e2e\signature-governance-e2e-static.spec.js` -> FAIL, expected reason: helper did not require real review projection and CSV quality package sample environment variables.

RED: `node tests\e2e\signature-governance-policy.e2e.js` -> FAIL, expected reason: test tenant reached current policy API but menu permissions for `signature-governance:*` were not seeded.

RED: `node tests\e2e\signature-governance-policy.e2e.js` -> FAIL, expected reason: policy response did not expose top-level `ready=true` for E2E to assert backend readiness explicitly.

## GREEN Evidence

GREEN: `node scripts\signature-governance-page-contract.test.mjs` -> PASS, 3 tests.

GREEN: `node tests\e2e\signature-governance-e2e-static.spec.js` -> PASS.

GREEN: `node --check tests\e2e\signature-governance-real-flow-helper.js` -> PASS.

GREEN: `node --check tests\e2e\signature-governance-retention-recovery.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\signature-governance-periodic-review.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\signature-governance-csv-package.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\signature-governance-policy.e2e.js` -> PASS.

GREEN: `npm run ts:check` -> PASS.

GREEN: `node tests\e2e\signature-governance-retention-recovery.e2e.js` -> PASS.

GREEN: `node tests\e2e\signature-governance-periodic-review.e2e.js` -> PASS.

GREEN: `node tests\e2e\signature-governance-csv-package.e2e.js` -> PASS.

GREEN: `node tests\e2e\signature-governance-policy.e2e.js` -> PASS.

GREEN: `review-fix-loop round 4 final reviewer` -> PASS, `final_decision: pass`.

## Failed, Skipped, Flaky, Or Blocked Tests

- Failed：none in final run.
- Skipped：none.
- Flaky：none observed.
- Earlier blocked states：missing E2E env vars, wrong backend port `48098`, missing menu permissions and missing top-level policy `ready` were all resolved and recorded in `execution-log.md`; these are superseded by the final PASS evidence.

## CI Impact And Release Recommendation

- CI impact：no CI wiring in this slice; commands are explicit Node scripts.
- Release recommendation：GO for task-specific commit. Final reviewer Round 4 passed; cleanup preview has been executed and temporary E2E/runtime artifacts have been removed. Worktree fast-forward merge/delete is not claimed because cleanup preview reported branch/main-worktree blockers.
