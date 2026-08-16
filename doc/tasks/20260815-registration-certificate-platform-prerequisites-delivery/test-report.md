# Test Report

Overall status: PRODUCT FUSION PASS; READY FOR CLOSEOUT. SP-01, SP-02, SP-03 and TC-18 are independently verified and safely fast-forwarded to `int_main` at `68578ad1c7b60a0228f12eccae55e345ff64b4ca`. TC-17 passed main plus independent read-only runtime verification.

## SP-01 System Registration Projection

- Commit: `e0db53516e250cbef346bfc4040200fcb044697c`
- Independent tester: `/root/registration_sp01_independent_tester` (non-writer)
- Focused verification: PASS, 27 tests, 0 failures/errors/skips
- Adjacent regression: PASS, 54 tests, 0 failures/errors/skips
- Branch/runtime guard: PASS, `int_main` slot 18, ports `8099/48099`
- Acceptance: `AC-01..AC-04` PASS
- Scope/diff/cleanliness: PASS
- Integration status: PASS; replacement integration HEAD exactly equals the verified commit, focused replay passed 27 tests, and slot-14 branch guard passed

## SP-02 Infra/DCC Business File Access Gate

- Candidate commit: `3a8caab09e95e8c02287fde8efbce1ce7652302c`
- Independent tester: `/root/sp03_executor_v2/sp02_independent_tester` (non-writer)
- Focused verification: product PASS, A/B/C = 60/26/164 tests, 0 failures/errors/skips
- Adjacent regression: product PASS, Infra/DCC = 82/672 tests, 0 failures/errors/skips
- Scope/diff/cleanliness: PASS, 33 approved paths, clean worktree, `git diff --check` exit 0
- Branch/runtime guard: PASS under the user-supplied authoritative v4 contract; slot 18 maps to `8099/48099`, and the foreign slot 20 is valid within `1..30`.
- Acceptance: `AC-05..AC-09` independently PASS; candidate is integration-eligible.

## SP-03 System Notify Idempotency

- Candidate commit: `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`; clean current-main recovery worktree, exactly 19 task-owned product/test paths; non-writer read-only code review PASS with no P0/P1/P2 findings
- Independent pre-commit focused verification: PASS, 35 tests, 0 failures/errors/skips
- Independent pre-commit System notify regression: PASS, 44 tests, 0 failures/errors/skips
- Independent pre-commit migration contract/policy: PASS, 6 pytest cases and a four-migration dependency DAG
- IoT caller regression: independently PASS against the final rebuilt SP-03 System artifact, 7 tests, 0 failures/errors/skips
- Current-main recovery verification: PASS for focused 35, System 44, migration pytest 6 and four-node migration policy; all commands have final exit code 0 and non-zero fresh reports.
- Broad caller diagnostic: exit 1 with 442 Infra tests, 3 failures, 1 error and 10 existing skips; the same four unrelated runtime-control failures were separately reproduced without SP-03. It is recorded as baseline diagnostic, never as PASS.
- Targeted reachable-caller gate: PASS on current-main source with fresh, non-skipped reports: BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7 tests, all with zero failures/errors/skips.
- Showroom fixture contract: PASS after synchronizing its H2-only `system_role.category_id` and nullable `system_notify_message.business_key` plus tenant-scoped unique constraint; the same two real notification tests are GREEN. No Showroom production behavior changed.
- Trade contract: PASS as an explicit static/compile contract only. The sole Member call remains statically unreachable after the pre-existing fixed unconditional return, and the real Trade leaf packages successfully against the SP-03 API. Activating the call would change order-delivery behavior and is prohibited; disabled Trade tests are not counted as evidence.
- Independent clean-commit replay attempt 1: AC-10..AC-15 PASS and all focused/System/migration/targeted caller/IoT/Trade contract checks PASS, but tester withheld overall release because two broad diagnostics each added a Windows JUnit temporary-directory cleanup error to the four known unrelated Infra failures. The implicated class passed 3/3 in isolation. A later root rerun reproduced exactly the four known baseline failures, so the extra error is localized as transient environment evidence; this root rerun does not replace the required independent retry.
- Earlier TC-17 prerequisite state was BLOCKED before the user selected the local Docker target and authorized temporary read-only credentials. That historical blocker is now resolved by the verified evidence below.

## TC-18 Infra Timeout Process Lifecycle Repair

- Candidate: `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`, parent `aeb2c9011e23d9a5d70610b4fb4c50c156d2186d`, exact two-path authorized Infra diff.
- Independent tester: `/root/tc18_independent_tester` (non-writer).
- Focused replay: PASS, exit 0, 7 tests, zero failures/errors/skips.
- Adjacent replay: PASS, exit 0, 72 tests, zero failures/errors/skips.
- Broad diagnostics A/B: both expected exit 1 and both exactly Infra 446/3/1/10; only the four registered unrelated runtime-control baselines remained. The repaired executor passed 7/7 in both runs, with no TempDir/directory-lock error or residual process.
- Scope, clean index/worktree, `git diff --check`, slot-23 runtime guard and final process check: PASS.
- TC-18 verdict: PASS. This removes the independent Windows handle-race blocker but does not waive the four unrelated broad baseline failures and does not close TC-17.

## Final SP-03 + TC-18 Replay Attempt 1

- Independent tester: `/root/sp03_final_independent_tester` (non-writer), exact candidate `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`.
- Fresh PASS: TC-18 7/72, SP-03 focused 35, System 44, migration pytest 6/policy 4, and callers BPM 1, DCC 2, Infra 6, MES 2, Showroom 2; all zero failures/errors/skips.
- BLOCKED at IoT before test discovery because the isolated Maven repository lacked the source `yudao-dependencies:2026.04-SNAPSHOT` BOM. Trade and broad were correctly not run after fail-fast, so the candidate was not released.
- Root installed only the formal source BOM into the same isolated repository with tests skipped as preparation; exit 0. Resumed independent IoT/Trade/broad verification is required.

## Final SP-03 + TC-18 Replay Completion

- Exact candidate: `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`, 21 paths, clean worktree/index, expected parent and passing slot-22 runtime guard.
- Independent tester: `/root/sp03_final_independent_tester` (non-writer).
- Resumed IoT: PASS, exit 0, fresh 7 tests, zero failures/errors/skips.
- Trade static/compile contract: PASS, leaf package exit 0; the sole production call remains behind the fixed unconditional return. Skipped tests were not counted as behavioral evidence.
- Broad diagnostic: expected exit 1, Infra 446/3/1/10 with only the exact four registered unrelated baselines; executor 7/7 and no TempDir/directory-lock error.
- Full same-candidate evidence: TC-18 7/72, SP-03 35, System 44, migration pytest 6/policy 4, BPM 1, DCC 2, Infra 6, MES 2, Showroom 2 and IoT 7, all scoped tests zero failures/errors/skips.
- Verdict: AC-10..AC-15 PASS; TC-18 PASS; candidate may enter integration. This does not waive TC-17 or authorize final `int_main` fusion.

## Integration Merge

- Integration commit: `9888417f704f1ac8b9af82bb22d51e1db511d374`.
- Merge parents: verified SP-01/SP-02 integration `4542fa3da4569e50ae93c5eddc3f4d67d940646b` and verified SP-03/TC-18 candidate `5b22aa0520b869f6df22e2e7b12f2af4ff1cbdac`.
- Pre-commit gate: PASS, exact 21 staged paths, zero extra/missing/unmerged, cached diff-check PASS and slot-14 guard PASS.
- Post-commit structure: PASS, all four verified task commits are ancestors, worktree clean and combined delta exactly 67 paths.
- Combined integration regression: PASS by root and an independent non-writer tester.

## Root Combined Integration Regression

- Toolchain: PASS, Maven 3.9.9, Java 21.0.10, target 17.
- SP-01: PASS, focused/regression 27/54.
- SP-02: PASS, A/B/C 60/26/164 and Infra/DCC regression 82/677.
- SP-03: PASS, focused/System 35/44; migration pytest 6 and policy 4.
- Reachable callers: PASS, BPM 1, DCC 2, Infra 6, MES 2, Showroom 2, IoT 7.
- Trade: static/compile contract PASS only; one call remains behind the fixed unconditional return, leaf package exit 0.
- All listed product tests had zero failures/errors/skips and fresh non-zero XML reports.
- Broad diagnostic: expected exit 1, Infra 456/3/1/10 and full fresh reactor 526/3/1/10. The only outcomes were the same four registered unrelated Infra baselines; executor 7/7 with no Windows TempDir/handle failure.
- Git/runtime: PASS, clean integration worktree/index, 67 paths, no unmerged/conflict markers, diff-check and slot-14 guard PASS.
- Independent integration audit: PASS on exact HEAD `9888417f704f1ac8b9af82bb22d51e1db511d374`; AC-01..AC-15 and TC-18 PASS, all scoped tests used fresh non-zero reports, security ordering and exact 67-path scope PASS, and no task process remained.
- TC-17 runtime schema gate: BLOCKED on missing approved non-production target and read-only credentials; therefore TC-19 and final fusion remain blocked.
- Fresh prerequisite audit: BLOCKED with the same condition. No matching Process/User/Machine DB environment-variable names or task-selected protected credential file exists. Local 3306/23306 listeners were not treated as target evidence. Python `pymysql` and `mysql.connector` are available, so execution can start without dependency installation once the four required inputs are supplied.

## TC-17 Local Docker Runtime Schema Gate

- User-selected target: local non-production `int-ruoyi-mysql`, `127.0.0.1:23306`, schema `ruoyi-vue-pro`.
- Temporary credential: randomized account created in process, global privileges empty, schema privileges exactly `SELECT`, authenticated query proved, and account deletion proved by zero-row cleanup check. No secret was logged.
- Required tables: PASS; `system_notify_message`, `controlled_content_version_ref`, and `controlled_content_transition_audit` are InnoDB base tables.
- Notify migration state: PASS as `valid_pre_migration`; both `business_key` and its unique index are absent, with no half-migration. This does not claim deployment.
- Controlled-content unique indexes: PASS with the exact three names, uniqueness flags and ordered column lists required by `20260718_controlled_content_lifecycle.sql`.
- Main verifier: PASS, corrected command exit 0. The first command's alias syntax error exited 1 and is recorded only as verifier-command failure; cleanup count was independently zero.
- Independent TC-17 replay: PASS, exit 0 at `2026-08-16T16:28:07+08:00`; global privileges only `USAGE`, schema privilege exactly non-grantable `SELECT`, table privileges empty, all metadata contracts exact, and cleanup count zero.
- TC-17 verdict: PASS. No migration, business DML, SSH or remote access was executed.

## Reconciled Integration Root Replay

- Candidate: `68578ad1c7b60a0228f12eccae55e345ff64b4ca`; clean worktree/index/unmerged state, exact 67-path task delta from `ecb05caa6`, diff-check/conflict scan/slot-14 guard all PASS.
- SP-01: PASS, 27 focused and 54 adjacent tests.
- SP-02: PASS, prepare plus 60/26/164 focused and 82/677 Infra/DCC regression tests.
- SP-03: PASS, 35 focused, 44 System, migration pytest 6, migration DAG 4, reachable callers BPM/DCC/Infra/MES/Showroom/IoT 1/2/6/2/2/7; every listed test report has zero failures/errors/skips.
- TC-18: PASS, focused/adjacent 7/72, zero failures/errors/skips.
- Broad diagnostic: expected exit 1, 526/3/1/10; only the same four registered unrelated Infra baselines, while `RuntimeControlCommandExecutorImplTest` is 7/7 PASS without a TempDir/handle failure.
- Static security/diff safety: PASS; Infra DCC coupling count 0, required DCC provider/gate evidence present, exact/prefix overlap with all 11,513 existing main dirty paths 0. Independent non-writer replay is in progress and is required before TC-19/fusion may be restored to PASS.

## Reconciled Integration Independent Replay

- Non-writer replay on exact `68578ad1c7b60a0228f12eccae55e345ff64b4ca`: PASS for fusion adjudication; clean worktree, 67 incoming paths, diff-check, ancestor, conflict and slot-14 guard checks all PASS.
- Fresh tests: SP-01 27; SP-02 A/B/C 60/26/164; SP-03 focused/System 35/44, migration pytest 6 and migration policy 4; BPM/DCC/Infra callers 1/2/6; TC-18 focused 7. All are zero failures/errors/skips.
- Independent broad diagnostic: expected exit 1, 526/3/1/10, limited to the four registered unrelated Infra baselines; repaired executor is 7/7 PASS. No product/task/Git/database edits were made by the tester.

## TC-20 Fusion

- PASS: `int_main` fast-forwarded to `68578ad1c7b60a0228f12eccae55e345ff64b4ca` through normal hooks.
- Atomic preflight/postflight: both runtime guards, 67-path allowlist, task-commit ancestry, diff-check, conflict scan, empty index/unmerged state and zero dirty-path overlap all PASS.
- Existing main dirty state preservation: PASS; 11,514 paths and SHA-256 `2506c9555c289428efc9a3063fad173ab2a8b6ba3fa05e2d30638f84e630c119` unchanged before/after fusion.

## Closeout

- Cleanup preview/apply: PASS; only the task-owned slot-release helper was deleted, while all durable records were retained.
- Worktree closeout: PASS; integration, SP-02, SP-03-v4, legacy SP-03 source and TC-18 worktrees are removed. The only non-clean legacy source content was verified byte-for-byte against the committed SP-03 candidate before forced removal.
- Runtime slots: PASS; 14/18/22/23 are inactive and all corresponding task backend ports had zero listeners.
