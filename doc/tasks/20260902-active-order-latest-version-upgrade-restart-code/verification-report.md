# Verification Report: Active Order Latest Version Upgrade Restart Code

## Scope Verified

- Worktree: `D:\IntRuoyiWorktree\20260902-active-order-latest-version-upgrade-restart-docs`
- Branch: `codex/20260902-active-order-latest-version-upgrade-restart-docs`
- Backend active-order version-upgrade preview/submit API contract.
- Backend active-order version-upgrade pending request persistence and old-order freeze contract.
- Backend approved version-upgrade effect service for old-batch void, old-task cancellation, old-order removal, forced-new latest-version active order, and request APPLIED update.
- MySQL additive migration for active-order version-upgrade requests.
- Frontend active-order row “升级” entry and confirmation dialog.
- Backend/frontend evidence files.

## Verification Commands

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs
```

Result: PASS.

```powershell
node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs
```

Result: PASS.

```powershell
node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs
```

Result: PASS after adding approved apply effect-chain contract.

```powershell
python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q
```

Result: PASS, 3 tests.

```powershell
mvn -f IntRuoyiBackend\pom.xml -rf :yudao-module-mes -DskipTests compile
```

Result: PASS.

```powershell
python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260902-active-order-latest-version-upgrade-restart-code\backend-api-evidence.md
```

Result: PASS.

```powershell
python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260902-active-order-latest-version-upgrade-restart-code\database-schema-evidence.md
```

Result: PASS.

```powershell
python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260902-active-order-latest-version-upgrade-restart-code\frontend-feature-evidence.md
```

Result: PASS.

```powershell
pnpm -C IntRuoyiFronted install --frozen-lockfile
```

Result: PASS - restored worktree-local frontend dependencies without changing `pnpm-lock.yaml`.

```powershell
pnpm -C IntRuoyiFronted ts:check
```

Result: PASS.

## Outcome

The submit blocker for missing approval persistence is fixed: submit now stores a pending approval request, freezes the old active order, and preserves frozen current/target version snapshots.
The approved-effect blocker is also fixed at service level: `applyApprovedUpgrade` can be called by a real approval callback to void the old batch, cancel old tasks, remove the old active order, force-create a new latest-version active order, mark the request `APPLIED`, and write maintenance audit.
BPM callback wiring and live terminal-state E2E are now verified in the worktree runtime. A fresh submit-from-active-order E2E still requires a currently visible active-order fixture with at least one latest-version difference.

## 2026-09-02 BPM Callback Wiring Verification

### Additional Scope Verified

- Submit path starts unified business approval through `BusinessApprovalOrchestrator` instead of leaving a disconnected pending row.
- `MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART` effect executor is registered and handles pending, approved, rejected, and cancelled BPM states.
- Rejected/cancelled approval releases the old active order from `VERSION_UPGRADE_PENDING` back to `ACTIVE`.
- Native BPM approval list renders “活跃订单升级重启” with request, source active order, work order, and target version summary tags.
- Migration seeds the business approval policy only when the required `mes-active-order-version-upgrade-v1` process definition exists; otherwise it fails fast.

### Additional Verification Commands

```powershell
node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs
```

Result: PASS.

```powershell
python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q
```

Result: PASS, 4 tests.

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs
```

Result: PASS.

```powershell
pnpm -C IntRuoyiFronted ts:check
```

Result: PASS.

```powershell
mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-bpm,yudao-module-mes -am -DskipTests compile
```

Result: PASS.

### 2026-09-03 Live E2E Result

- Worktree runtime `8093/48093` was active; backend health was UP. No `int_main` service restart was performed.
- Runtime DB prerequisites are present: request table exists, `MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART` policy is published, and Flowable has process definition key `mes-active-order-version-upgrade-v1`.
- Existing real approval E2E evidence shows Playwright approved process instance `7f9ca694-a6da-11f1-a6b9-00155d07b6dd` through the approval-center UI and then saw replacement active order `1009200001` in the active-order pool.
- New final-state E2E passed: Playwright logged in through the real frontend, opened the production-leader active-order pool, verified old active order `45` is absent from the active pool, verified new active order `1009200001` is visible, `ACTIVE/ACTIVE`, route version `742 / V12`, and opened its detail dialog.
- Read-only DB verification matched the UI: request `1` is `APPLIED/APPROVED/APPLIED`; old active order `45` is `REMOVED/VERSION_UPGRADED`; new active order `1009200001` is `ACTIVE/ACTIVE`; historic Flowable process has `END_TIME_`.
- Fresh submit E2E with currently visible active order `150` is blocked by valid business precheck: all controlled objects are already latest formal versions, so it is not a usable upgrade-submit fixture. Active order `1009200000` has a version difference but is filtered out of the active-order list by progress validation, so it cannot serve as a real frontend submit sample without a repaired/created visible fixture.

### 2026-09-03 Additional Verification Commands

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS.

```powershell
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-submit-real.e2e.cjs
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs
node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs
python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q
```

Result: PASS; SQL contract suite reported 7 tests.

### Closeout Preview

```powershell
python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260902-active-order-latest-version-upgrade-restart-code --mode preview
```

Result: BLOCKED for closeout only. The preview kept core task records and `e2e-artifacts/`, but refused apply because the main worktree `E:\IntRuoyi` is dirty and the feature branch cannot be fast-forward merged into `int_main` from the current state. No cleanup apply, commit, merge, or worktree removal was performed.

### 2026-09-03 Full Chain E2E Rerun

Scope requested by user: complete upgrade chain verification, not only old-order freeze.

Runtime:

- Worktree: `D:\IntRuoyiWorktree\20260902-active-order-latest-version-upgrade-restart-docs`
- Frontend/backend: `8093/48093`
- Backend health: `UP`
- No `int_main` restart or service mutation was performed.

Real frontend E2E:

```powershell
node tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS.

Verified terminal state:

- Old active order `45` is absent from the active-order pool.
- Replacement active order `1009200001` is visible in the active-order pool.
- Replacement order keeps work order `CODX-PQC-20260807-SP-WO-05`.
- Replacement order is `ACTIVE/ACTIVE`.
- Replacement order uses target process route version `742 / V12`.
- The detail dialog for replacement order `1009200001` opens from the real page.

Read-only DB verification:

- Upgrade request `1`: `source_active_order_id=45`, `target_active_order_id=1009200001`, `request_status=APPLIED`, `approval_status=APPROVED`, `freeze_status=APPLIED`, `approval_process_instance_id=7f9ca694-a6da-11f1-a6b9-00155d07b6dd`.
- Old active order `45`: `REMOVED / VERSION_UPGRADED`.
- New active order `1009200001`: `ACTIVE / ACTIVE`, `route_version_id=742`.
- Route version `633`: `V3 / SUPERSEDED`; route version `742`: `V12 / ACTIVE`.
- Flowable historic process `7f9ca694-a6da-11f1-a6b9-00155d07b6dd` has non-null `END_TIME_`.

### 2026-09-03 Frontend-Only E2E Gate

User gate: E2E must validate through actual frontend operations and must not directly call APIs.

Implementation of the gate:

- Tightened `IntRuoyiFronted/tests/e2e/active-order-version-upgrade-final-state-real.e2e.cjs`.
- The script no longer parses active-order list/detail API JSON as the assertion source.
- The script records only HTTP status for page-triggered list/detail requests.
- Assertions now come from visible frontend DOM: active-order table row text, absence of the old active-order ID in the visible active pool, and the detail dialog opened by the page button.

Verification commands:

```powershell
node --check tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
node tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS.

Static no-direct-API gate:

```powershell
rg -n "fetch\(|request\.get|APIRequest|axios|\.json\(|response\.json|docker exec|mysql|Invoke-RestMethod|Invoke-WebRequest" IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS, no matches. The E2E script does not directly call APIs, does not parse API JSON as the oracle, and does not use DB or shell HTTP calls.

Evidence artifact:

- `doc/tasks/20260902-active-order-latest-version-upgrade-restart-code/e2e-artifacts/active-order-version-upgrade-final-state-real-result.json`
- `verificationMode=FRONTEND_DOM_ONLY`
- Visible active-pool row contains `1009200001`, `CODX-PQC-20260807-SP-WO-05`, `V12`, `正式订单`, `0%` production progress and `0%` inspection progress.
- Old active order `45` is not present in the current active-order pool visible table.
- Detail dialog opened from the frontend `详情` button and shows `订单 CODX-PQC-20260807-SP-WO-05 · 工序提交详情` with `15` processes.

### 2026-09-03 Complete E2E Confirmation

User gate: verify the complete chain, not stopping at old-order freeze.

Previously completed real frontend chain evidence:

- `active-order-version-upgrade-submit-real.e2e.cjs`: submitted upgrade approval from the active-order row and froze old active order `45`.
- `active-order-version-upgrade-approve-real.e2e.cjs`: approved BPM process instance `7f9ca694-a6da-11f1-a6b9-00155d07b6dd` through the real approval-center page, applying the upgrade.
- `active-order-version-upgrade-final-state-real.e2e.cjs`: verified terminal state from the production-leader active-order pool.

Rerun verification:

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS.

Terminal state confirmed by real frontend DOM:

- Old active order `45` is no longer visible in the active-order pool.
- New active order `1009200001` is visible in the active-order pool.
- New active order row contains `CODX-PQC-20260807-SP-WO-05`, `V12`, `正式订单`, production progress `0%`, and inspection progress `0%`.
- New active order detail opens from the frontend `详情` button and shows `15` processes, proving the replacement order starts from the new route snapshot rather than continuing the old execution facts.

Script stability verification:

```powershell
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS.

```powershell
rg -n "fetch\(|request\.get|APIRequest|axios|\.json\(|response\.json|docker exec|mysql|Invoke-RestMethod|Invoke-WebRequest" IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS, no matches.

Note: The rerun captured a background approval-todo badge console error (`系统异常`). It did not block the verified upgrade chain because login, active-order pool visibility, replacement order row, page-triggered active-order requests, and detail dialog all completed successfully. This warning should be tracked separately if the approval badge is in scope.

### 2026-09-03 Feature Branch Commit

Feature branch commit: $full.

Committed scope includes backend implementation, frontend entry/API integration, SQL migrations, static tests, real E2E scripts, task records, PRD/user-flow docs, and preserved E2E artifacts.

### 2026-09-03 Fresh Full-Chain Rerun Boundary

User gate: continue E2E verification beyond old-order freeze and prove the terminal chain.

Changes to E2E coverage:

- `active-order-version-upgrade-submit-real.e2e.cjs`: now scans active-order pagination before declaring a source row missing.
- `active-order-version-upgrade-approve-real.e2e.cjs`: now scans active-order pagination before declaring the replacement row missing after approval.
- `active-order-version-upgrade-final-state-real.e2e.cjs`: now scans all active-order pages and asserts the old source active-order ID is absent from the full visible active pool, not just page 1.

Verification rerun:

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
```

Result: PASS. Playwright logged in through the real frontend, opened the production-leader active-order pool, scanned visible active-order IDs 150, 348, 396, and 1009200001, verified old active order 45 is absent from all visible active-pool pages, verified replacement active order 1009200001 is visible with work order CODX-PQC-20260807-SP-WO-05 and route version V12, and opened its detail dialog from the real page.

Static and contract rerun:

```powershell
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-submit-real.e2e.cjs
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-approve-real.e2e.cjs
node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs
node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs
python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q
```

Result: PASS; SQL contract suite reported 7 tests.

Fresh continuous-chain rerun boundary:

```powershell
node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-submit-real.e2e.cjs
```

Result: BLOCKED by current test data, not by a successful business assertion. The task-owned old-version candidate 1009200000 / CODX-AOUP-20260902205106 has route 633 / V3, but it is not visible in the real active-order pool DOM. The page rendered only active orders 150, 348, 396, and 1009200001. Read-only DB inspection found the candidate's active-order row has work_order_id=1009200000, while its process snapshots and PQC tasks are bound to work_order_id=980032; this inconsistent fixture prevents list readability. No direct DB write, route publication, QA publication, or shared master-data mutation was performed to manufacture a fresh upgradable source order.

Conclusion: the previously approved chain plus today's final-state rerun proves the post-approval terminal outcome. A brand-new submit -> approve -> final E2E instance still needs an approved setup path for a visible old-version task-owned fixture.
