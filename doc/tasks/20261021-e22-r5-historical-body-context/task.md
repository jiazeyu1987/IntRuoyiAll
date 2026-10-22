# E22 R5: Historical body context in project browser

## Goal
Allow an already-authorized reader to preview a selected SUPERSEDED or OBSOLETE controlled-file version from project browsing, with an exact VERSION_HISTORY body-read context.

## BDD
- BDD: historical version preview -> Given a reader can list and read an eligible retained version and its body artifact exists, When the reader previews that row in project browsing, Then the row exposes preview and the viewer request carries that row's controlledFileId and expectedVersionNo under VERSION_HISTORY.
- BDD: unauthorized or unavailable history -> Given retained-version authorization or the body artifact is absent, When project browsing projects or opens the version, Then no new permission is inferred and the existing authorization/artifact failure remains effective.

## Milestones
- [x] Inspect R4 evidence, current branch, applicable rules, and existing behavior.
- [x] Apply the focused projection and context changes.
- [x] Run frontend TypeScript validation.
- [x] Add/run focused backend RED/GREEN coverage and the browser-route context unit test.
- [x] Record validation and whether real UI rerun remains necessary.
- [x] Verify current int_qms runtime class/JAR fingerprint and build/stage a minimal candidate package when stale.
- [x] Restart only the identified int_qms backend process with schema updates disabled and the startup sync opt-in omitted; verify service health.
- [x] Run the original E22 real Reader UI path once; stop at the documented authorization blocker when the historical list is empty.

## Expected verification
- Focused DCC query-service unit test.
- Frontend typecheck/unit test if an existing targeted test is available.
- Compare source-compiled, packaged, and running historical-browser projection class SHA-256 values.
- Health check and exact int_qms process/JAR/port identity after restart.
- Real Playwright UI path only: authorized Reader opens project browser, selects all versions, previews the retained row, and sees exact historical version/body. No additional business writes.

## Design constraints
- Reuse retained-version authorization; do not add or broaden permissions.
- Keep attachment availability independent from lifecycle and preview projection.
- Preserve exact selected version identity and version number.
- Do not modify unrelated existing workspace changes.

## Current Status
blocked

## Activation job isolation subphase (Root authorized 2026-10-21)

Goal: isolate individual due-file RuntimeExceptions while keeping the overall strict tenant/Quartz run observably failed. BDD and constraints: `activation-job-failure-isolation-review.md`; actual RED/GREEN and transaction evidence: `activation-job-test-report.md`.

Milestones: valid H2/job/aspect RED obtained; minimal job/annotation/aspect implementation complete; four targeted suites GREEN (28 tests); Maven exited. Subphase ready_for_closeout for Root review. Overall status remains blocked. No packaging, real job, browser, runtime writes/restart, or Git actions in this authorization.

Design constraints: failOnError default false; DCC only opt-in; no per-record Error catch/retry, no Catalog/history/schema/config/Invoker changes; failed IDs/count retained in aggregate root message and originals suppressed. Existing late-ledger H2 test proves write rollback; new batch H2 test proves continuation and later successful commit.

## 2026-10-21 Runtime Scope Update
- User explicitly authorized checking the int_qms runtime package, preparing a minimal build/deployment candidate if stale, restarting after package verification, and running only the original E22 real UI path.
- No new business data preparation or mutation is in scope; reuse the existing E22 fixture and accounts only. Stop if its required reader/fixture is not already usable from the real UI.
- Preserve the current int_qms runtime profile and ports 8061/48061, existing application profile, and task runtime parameters. Do not touch other ports or services.
- Package source: `IntRuoyiBackend/yudao-server/target/yudao-server-exec.jar`; SHA-256 `9B594469AAFE47C3EE96A516948876ED9CB245AE648FCF84DD4524E1D0C9621D`. Staged candidate: `output/runtime/int_qms/backend-e22-r5-20261021.jar`, same SHA. E22 query class source and packaged SHA-256 both `98B4EDAC7A644D9FB62C2E0136793D463E2803C055B56C5F1B001F0EA982AF84`. E15 diagnostic resolver source and packaged SHA-256 both `8632DC217E924441C50ED869ED7A0BE19C1E9967790EE25A33B086C5C56620A8`.
- Existing runtime jar `backend-rerun-r2-20261020-1301.jar` SHA-256 was `709E78BD665DA04CC0FDE3DDFFFFB401A028A8835139646FA4754EC753AAA147`; it did not contain the current E22 projection class.
- Restarted only QMS PID 38176 to candidate PID 37016 on 48061. Health `UP`; profile `dcc-local-development`; `flowable.database-schema-update=false`; no `dcc.local-activation-startup-sync.*` arguments, so job 5625 was not startup-triggered. Frontend PID 20976 on 8061 remained untouched.
- The Reader real UI reached `/dcc/controlled-file/browser`, selected “全部版本”, filtered the existing Source project, and rendered three Main rows: A/1 “已作废”, “草稿 / 已替代”, and A/2 “受控（已生效）”. The page also displayed `Current user cannot access this controlled file`; row previews were disabled/unavailable under the current Reader authorization. No preview was opened, and A/2 was not treated as historical success. The old R4 DOM is not proof of this run.
- Job 5625 was not triggered. No task actions were performed in the timer page.
- Current run screenshots/DOM and sanitized trace are in this task directory; `ui-r5.cjs` is the runner. No business writes were performed.

## R6 directed real UI correction (2026-10-21)
Authorized directed rerun r6-directed/verification-report.md: PASS for Main A/1 historical body. Browse enabled; real click opened exact VERSION_HISTORY/A/1; original-only PDF marker visible, newer marker absent. Earlier R5 disabled-preview inference is superseded, not a proven product failure. Prior accepted A/1 PASS preserved. Does not claim all E22 or E2E01–24 complete. Owned browsers closed.

## R7 launcher key restoration BDD (authorized, no service action)
Given g56 existing environment and the original provisioned private key file, When assembling the E22 launcher environment, Then inject that exact existing key into Spring JSON while preserving all other config/env; never print/persist key material.
Given missing/blank/short key or invalid JSON, When preparing launch, Then fail before port probe, log creation or process creation. Existing historical log directory remains untouched; next launch uses a unique log directory.
Scope: restart-qms-r5.py + task-owned unittest only. No Java changes, new keys, repackaging or stop/restart. Audit: 20261013 start-verified-runtime.py lines14-18 proves original file purpose and >=32-byte gate; TokenProperties uses the same requirement. RED -> GREEN required before Root review.

R7 launcher correction: RED5/GREEN5, real configuration assembly preflight PASS; ready_for_closeout for code-review subphase, runtime restoration reserved for Root. Overall historical task status does not imply attachment suite completion.

## R8 exact-transition configuration restoration (Root authorized)
Given the original reviewed registry, v6 registry, deployment and approval evidence, When launcher configuration is assembled, Then restore precisely both reviewed mappings, preserving the original key and all unrelated configuration without latest-definition lookup.
Given any missing/changed evidence or conflicting existing registry, When assembling configuration, Then fail before port probe/process creation without exposing configuration secrets.
Scope: task launcher/test/records only; no production change, service action, checkin, build or Git. RED/GREEN checks cover exact rows, input preservation, evidence provenance and failure safety. Root independently reviews and owns the sole later restart. Startup-sync remains disabled and schema=false. Current subphase in_progress.

R8 implementation subphase: ready_for_closeout for Root independent review; RED7/GREEN12 and real config-only preflight PASS. Overall task blocked pending Root-owned restart and real UI verification. Preserve r8-transition-preflight.json, restart-qms-r5.py and test_restart_qms_r5.py for review.
