# Frontline PQC DCC QA Delivery Test Report

## Wave 7 Round-2 Independent Verification

- status: FAIL
- tested_task_ids: [DF10, DF11]
- actual_result: Both second-round independent gates rejected incomplete formal contracts even though their focused commands passed.
- DF10 gaps: inspectionTypeRules, taskSummary, task-option ruleSort/inspectionTypeRule/taskStatus, and complete published-version item fields.
- DF11 gaps: the same frontend contract fields, complete task states, frozen activeOrderId endpoint migration, pure stable projection/stale-response isolation, backend parity, and scope compliance.
- follow_up: Both tasks were returned to their executors. DF10 has since produced a new 5-test GREEN implementation and awaits round-3 independent verification. DF11 has produced the strict DTO/projection migration but remains in typecheck repair under the documented narrow page-local adapter clarification.
- evidence:
  - D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df10/doc/tasks/20260812-frontline-pqc-dcc-qa-df10/independent-test-report.md
  - D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df11/doc/tasks/20260812-frontline-pqc-dcc-qa-df11/independent-test-report.md

## TC-C00-SCHEMA

- status: PASS
- tested_task_ids: [C00]
- mapped_acceptance_ids: [AC-01, AC-03, AC-04, AC-05, AC-07, AC-09, AC-10, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-c00
- tester: supervisor independent verification after two tester-agent runs stalled before writing evidence

### Expected Result

- C00 freezes the schema/migration baseline for route-DCC binding, active-order QA snapshots, PQC task rule identity, canonical submission hash, and unique formal PQC event linkage.
- Preflight is a schema-before read-only gate from 20260811 and must not depend on C00 schema objects already existing.
- Schema DDL must not prematurely tighten historical-data-sensitive unique constraints before backfill/postflight.
- Postflight owns zero-blocker NOT NULL tightening, old task identity index replacement, task rule identity uniqueness, submitted event uniqueness, and PQC event task uniqueness.
- C00 must not introduce duplicate DCC-QA binding, item-type table, active-order context table, fallback behavior, mock success, or silent downgrade.

### Actual Result

- Maven schema command PASS: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`; Surefire reports 7 tests, 0 failures, 0 errors.
- Maven regression command PASS: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`; Maven reports 14 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Static SQL layering PASS: preflight depends on `20260811_mes_qa_dcc_project_scope`, does not depend on `20260812_mes_pqc_dcc_qa_c00_schema`, and uses `information_schema` / dynamic SQL safety for C00-created structures.
- Static schema constraint PASS: `schema.sql` does not contain `uk_mes_pqc_task_rule_identity`, `uk_mes_pqc_task_submitted_event`, `uk_mes_pro_process_pool_event_pqc_task`, or early `DROP INDEX uk_mes_pqc_task_qa_identity`.
- Static postflight tightening PASS: `postflight.sql` contains the three uniqueness constraints, old identity index replacement, NOT NULL tightening, and `@c00_postflight_blocker_count = 0` gate with failure signaling.
- Forbidden model scan PASS: no C00 SQL hit for `dcc_project_code_qa_regulation_binding`, `mes_qa_inspection_regulation_item_type`, `mes_pro_process_pool_active_order_pqc_context`, fallback/default-success/silent-downgrade markers.
- Change-scope review PASS: worktree changes are limited to C00 SQL files, `MesQaPqcSchemaTest.java`, and C00 task evidence files.

### Evidence

- Surefire schema report: `D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-c00/IntRuoyiBackend/yudao-module-mes/target/surefire-reports/cn.iocoder.yudao.module.mes.MesQaPqcSchemaTest.txt`.
- Regression output captured by supervisor at 2026-08-12 13:25:39 +08:00: 14 tests, 0 failures/errors, BUILD SUCCESS.
- Static check result: all 13 layering/forbidden checks returned true, `forbidden_hits=[]`, `all_pass=true`.

### Blockers

- None for C00 task-level acceptance.

### Unresolved Risks

- C00 was validated through static schema assertions and dry-run SQL evidence only; no live database migration sandbox was executed in this task scope.

## TC-DF01-ACTIVE-ORDERS

- status: PASS
- tested_task_ids: [DF01]
- mapped_acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df01
- tester: supervisor independent verification after the assigned tester agent stalled without writing test-report evidence

### Expected Result

- All effective active orders are returned as separate rows.
- activeOrderId is exposed as the stable page selection identity.
- Duplicate workOrderId + routeId pairs are not deduplicated.
- PQC task state does not filter the active-order list.
- Database or master-data gaps continue to fail fast through existing service exceptions.

### Actual Result

- Maven command PASS: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test.
- Surefire result: 4 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS at 2026-08-12T14:57:10+08:00.
- RED was valid before implementation: the same command failed because MesFrontlineActiveOrderCandidate lacked activeOrderId().
- Static scope review PASS: changed files are limited to DF01 active-order candidate/VO/controller conversion/list service tests and DF01 task evidence.
- Static no-dedupe review PASS: MesFrontlinePqcContextServiceImpl no longer contains ActiveOrderKey, LatestActiveOrderContext current de-dupe logic, or selectActiveOrderIdsByTaskStatus active-order filtering.
- Contract review PASS: activeOrderId is present in MesFrontlineActiveOrderCandidate, MesFrontlineActiveOrderRespVO, controller mapping, controller test, and service test.
- diff check PASS: git diff --check reported only line-ending warnings.
- Backend evidence validator PASS: python C:/Users/BJB110/.codex/skills/backend-api-delivery/scripts/validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df01/backend-api-evidence.md.

### Evidence

- DF01 backend evidence: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df01/doc/tasks/20260812-frontline-pqc-dcc-qa-df01/backend-api-evidence.md.
- DF01 task execution log: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df01/doc/tasks/20260812-frontline-pqc-dcc-qa-df01/execution-log.md.
- Target test output captured by supervisor: 4 tests, 0 failures/errors, BUILD SUCCESS.

### Blockers

- None for DF01 task-level acceptance.

### Unresolved Risks

- The assigned independent tester agent did not complete its writeback, so supervisor performed the independent verification and recorded the tester stall.
- DF01 is now contained by int_main at commit `a145f0dc0 feat(mes): preserve PQC active order identity`; `git diff --name-status int_main..task/20260812-frontline-pqc-dcc-qa-df01` is empty.
- Post-authorization merge evidence: `git merge --ff-only task/20260812-frontline-pqc-dcc-qa-df01` fast-forwarded `int_main` to `a145f0dc0`; DF01 targeted Maven regression rerun in the clean DF01 worktree passed again at 2026-08-12T15:42:42+08:00 with 4 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS.
- Preservation evidence: original overlapping dirty content is retained in stash `supervisor-temp-df01-overlap-before-ff-20260812`; obsolete DF01 docs/tests were not restored, and the unrelated existing runtime-config mapping hunk was restored to preserve the pre-existing main worktree change.

## TC-DF02-ACTIVE-ORDER-SNAPSHOT

- status: PASS
- tested_task_ids: [DF02]
- mapped_acceptance_ids: [AC-03, AC-04, AC-05, AC-06, AC-09, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df02 and E:/IntRuoyi after fast-forward merge
- tester: supervisor independent verification after clean executor was interrupted without final handoff

### Expected Result

- activeOrderId resolves only to the server-side active-order route snapshot and locked QA snapshot.
- Missing, removed, cross-tenant, route-incomplete, and QA-snapshot-incomplete cases fail fast.
- Resolver does not accept client route overrides, does not write database rows, and does not infer by product, formBindings, routeProcessId, or processId.

### Actual Result

- Maven command PASS in DF02 worktree backend root: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test.
- Surefire result PASS: MesFrontlineActiveOrderSnapshotResolverTest 5 tests, 0 failures, 0 errors, 0 skipped.
- Static review PASS: changed implementation is limited to a read-only resolver selecting activeOrderId/workOrderId/routeId/routeVersionId/dccProjectCodeId/qaRegulationId/qaRegulationVersionId from active_order by id and ACTIVE status.
- Forbidden scan PASS: no insert/update/delete, FOR UPDATE, product_id, formBindings, routeProcessId, processId, or workOrder+route active-order lookup in the resolver implementation.
- Evidence validator PASS: backend-api-delivery evidence validator accepted DF02 backend evidence.
- Fast-forward merge PASS: int_main advanced to commit eb44e4c80.
- Post-merge evidence PASS: int_main Surefire report confirms 5 tests / 0 failures / 0 errors for MesFrontlineActiveOrderSnapshotResolverTest.

### Evidence

- DF02 commit: eb44e4c80 feat(mes): resolve active order PQC snapshot.
- DF02 task evidence: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df02/doc/tasks/20260812-frontline-pqc-dcc-qa-df02/.
- int_main report: E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/target/surefire-reports/cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineActiveOrderSnapshotResolverTest.txt.

### Blockers

- None for DF02 task-level acceptance.

### Unresolved Risks

- Main worktree still contains many unrelated dirty and untracked paths. Later merges must continue exact path-overlap checks before fast-forward.

## TC-DF05-DCC-QA-REGULATION

- status: PASS
- tested_task_ids: [DF05]
- mapped_acceptance_ids: [AC-03, AC-04, AC-05, AC-07, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df05 and E:/IntRuoyi after fast-forward merge
- tester: supervisor independent verification after executor handoff

### Expected Result

- QA regulation ownership is direct by dccProjectCodeId.
- QA save/publish contracts do not infer relation from product, route, MES routeProcessId, MES processId, or formBindings.
- DCC project list reads QA status by current-page DCC project IDs and ignores stale responses.
- Backend rejects legacy result types NUMBER and CHOICE, keeping BOOLEAN, NUMERIC, and TEXT as the accepted result type contract.

### Actual Result

- Worktree frontend contract PASS: node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs.
- Worktree DCC status column contract PASS: node tests/e2e/dcc-project-code-qa-status-column-static.spec.cjs.
- Worktree Maven regression PASS: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test, Surefire 8 tests / 0 failures / 0 errors.
- Scope review PASS: DF05 changed only QA regulation service/test, QA API type contract, DCC ProjectCodeTabPanel QA status projection, two frontend static contracts, and DF05 task evidence.
- Branch runtime guard PASS before commit and before/after merge.
- Commit PASS: 37414e367 feat(mes): bind QA regulation to DCC project code.
- Fast-forward merge PASS: int_main advanced from eb44e4c80 to 37414e367.
- Post-merge frontend contracts PASS on E:/IntRuoyi.
- Post-merge Maven regression PASS on E:/IntRuoyi: low-noise command returned exit code 0 and Surefire report confirms 8 tests / 0 failures / 0 errors.

### Evidence

- DF05 task evidence: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df05/doc/tasks/20260812-frontline-pqc-dcc-qa-df05/.
- int_main Surefire report: E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/target/surefire-reports/cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationServiceTest.txt.
- First post-merge Maven attempt was stopped at PID 65132 after it exceeded normal runtime and was diagnosed as the current task-owned Maven process stuck in test compilation/file close; it was not used as PASS evidence.

### Blockers

- None for DF05 task-level acceptance.

### Unresolved Risks

- Main worktree still contains many unrelated dirty and untracked paths. DF05 merge used exact path-overlap check and found overlap count 0 before fast-forward.

## TC-DF03-ROUTE-DCC

- status: PASS
- tested_task_ids: [DF03]
- mapped_acceptance_ids: [AC-03, AC-04, AC-05, AC-07, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df03
- tester: supervisor verification after two independent tester agents stalled without writing test-report evidence

### Expected Result

- Route-DCC binding is a formal relation from routeId to dccProjectCodeId and does not infer from product, QA regulation, formBindings, route name, processId, or routeProcessId.
- Save/rebind uses expectedVersion CAS and validates the DCC project code exists and is ENABLE.
- Unbind uses only route update permission, writes a deleted tombstone, and advances the relation version monotonically to avoid ABA.
- GET reads current relation and latest version without requiring DCC or QA permissions.
- Frontend route edit page reads/saves/deletes DCC binding through dedicated APIs and does not treat route save success as DCC binding success.

### Actual Result

- RED was valid: DF03 Maven target failed because PRO_ROUTE_DCC_PROJECT_INVALID was missing, so disabled DCC project code rejection could not compile.
- Maven command PASS after implementation: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesRouteDccProjectBindingServiceTest,MesRouteDccProjectBindingControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test.
- Surefire result PASS: 10 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Frontend static contract PASS: node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs.
- diff check PASS: git diff --check returned exit code 0 with LF/CRLF working-copy warnings only.
- Evidence validators PASS: backend-api-delivery and frontend-feature-delivery validators accepted DF03 evidence files.
- Static no-inference review PASS: Route-DCC backend files do not reference product, formBindings, QA, routeName, processId, or routeProcessId as relation inference inputs.

### Evidence

- DF03 task evidence: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df03/doc/tasks/20260812-frontline-pqc-dcc-qa-df03/.
- Surefire reports: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df03/IntRuoyiBackend/yudao-module-mes/target/surefire-reports/cn.iocoder.yudao.module.mes.service.pro.route.MesRouteDccProjectBindingServiceTest.txt and controller report.
- Frontend static contract output: mes-route-dcc-project-binding-static contract PASS.

### Blockers

- None for DF03 task-level acceptance.

### Unresolved Risks

- Real browser write-path verification was not run in DF03; INT12/VAL13 must verify the full route-DCC to frontline PQC chain through confirmed login, route configuration, and task-owned data.

## 2026-08-12 20:26:52 +0800 DF03 Post-Merge Verification

- Result: PASS.
- Commit: 5d503ea5e feat(mes): add route DCC project binding.
- Merge evidence: int_main and DF03 worktree resolve to the same commit; DF03 branch is ancestor/equal to int_main; no diff remains from int_main to DF03 branch.
- Backend: MesRouteDccProjectBindingControllerTest 3 tests PASS; MesRouteDccProjectBindingServiceTest 7 tests PASS.
- Frontend: mes-route-dcc-project-binding-static contract PASS.
- Guard: branch runtime port guard PASS for int_main 8081/48081.
- Scope note: Verification used the clean DF03 worktree because E:/IntRuoyi contains unrelated concurrent dirty changes; no unrelated changes were cleaned, reverted, staged, or committed.

## 2026-08-12 23:55:15 +0800 DF04 Verification

- Result: PASS.
- Commit: `d781ca689 feat(mes): resolve route DCC project`.
- Target backend verification: `MesFrontlineDccProjectResolverTest` 10 tests PASS。
- Combined regression: DF02 snapshot 5 + DF03 service 7 + DF03 controller 3 + DF04 resolver 10 = 25 tests PASS，0 failures/errors/skips。
- Independent gate: PASS，无 Critical/High/Medium/Low 问题；报告位于 DF04 任务记录的 `independent-test-report.md`。
- Static review: 正式路线-DCC关系为唯一来源；缺失、多条、停用、删除和跨租户均 fail fast；产品、物料、QA、表单和工序推算扫描为 0。
- Merge evidence: `int_main` 已 fast-forward 到 `d781ca689`，且 `git diff --name-status int_main..task/20260812-frontline-pqc-dcc-qa-df04` 为空。
- Environment note: E:/IntRuoyi 主工作区的两次合并后 Maven 尝试均长时间无输出并被终止，因此未计入通过；PASS 依据为相同提交的干净任务 worktree 目标测试、组合回归及独立复验。
- Residual risks: 当前为 mapper mock 单测，真实数据库组合路径由后续 DF06/INT12 覆盖。

## 2026-08-13 00:30:40 +0800 DF04 Closeout / DF06 Start Gate

- DF04 closeout: PASS，收尾提交 `66b5607a8` 已 fast-forward 合入 `int_main`，DF04 worktree 已删除，slot 18 已释放。
- DF06 start gate: PASS，DF06 worktree 已快进到 `66b5607a8`，状态干净，slot 18 已重新登记为 frontend 8099 / backend 48099，分支端口 guard 在 DF06 worktree 内通过。
- Next verification owner: `/root/df06_worker` must provide RED/GREEN/regression evidence for `MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest` before supervisor review.

## 2026-08-13 02:50:52 +0800 DF06 Independent Verification

- Result: PASS.
- Commit ready on DF06 branch: `eb723a8aa feat(mes): lock QA version for active PQC orders`.
- Target backend verification: `MesTeamLeaderActiveOrderServiceTest` + `MesProcessPoolActiveOrderMapperTest` = 33 tests PASS。
- Touched-test regression: 4 related test classes = 39 tests PASS。
- Extra schema verification: `MesQaPqcSchemaTest` = 7 tests PASS。
- Evidence validator: backend-api evidence PASS。
- Static review: no product/material/formBindings/selectEnabledList/fallback/default-success inference in DF06 touched production files; DF06-created PQC tasks keep `routeProcessId/processId` unset and use QA identity through `qaProcessId + inspectionRuleKey`.
- Blocker: merge into `int_main` was not attempted because the main working tree has uncommitted overlapping changes on two DF06 paths. This blocks DF07 start until resolved or explicitly authorized.

## 2026-08-13 03:15:34 +0800 DF06 Post-Sync / Post-Merge Verification

- Result: PASS.
- Final merged commit: `fd6e923a5`; `int_main` and `task/20260812-frontline-pqc-dcc-qa-df06` have no remaining diff.
- Post-sync target backend verification: 33 tests PASS。
- Post-sync touched-test regression: 39 tests PASS。
- Post-sync schema verification: `MesQaPqcSchemaTest` 7 tests PASS。
- Static gates: backend-api evidence validator PASS, git diff --check PASS, forbidden inference scan PASS。
- Cleanup: DF06 worktree removed and slot 18 released after confirming no 8099/48099 listeners.

## 2026-08-13 06:07:00 +0800 DF08 Independent / Post-Merge Verification

- Result: PASS.
- Final merged commit: 7d9f41e92; int_main and task/20260812-frontline-pqc-dcc-qa-df08 have no remaining diff.
- Target backend verification: MesQaInspectionRegulationServiceTest 13 tests PASS。
- Static gates: backend-api evidence validator PASS, git diff --check PASS, forbidden scan PASS。
- Contract checks: FIRST/PATROL_AM/PATROL_PM/FINAL rule keys are preserved, PATROL_AM/PATROL_PM are not collapsed into PATROL, resultType remains BOOLEAN/NUMERIC/TEXT only, equipment options are preserved, and no product/material/route-process inference was added.
- Cleanup: DF08 worktree removed and slot 18 released after confirming no 8099/48099 listeners.

## Wave 7 DF10 Round-3 Independent Verification

- Result: FAIL.
- Mandatory Maven gate failed during `yudao-module-mes` compilation before tests ran: `MesFrontlineDeviceAccountController` still calls `setAcceptanceStandard` and `setProcessInspectionMethod`, but DF10 removed those setters from the shared PQC item VO.
- Architecture gate failed: DF10 does not call the frozen DF07 `MesQaInspectionRegulationService#getLockedVersionProcessesForOrder` boundary required by its task contract. It adds a parallel private locked-QA resolver and directly reads regulation/version/process/item mappers, duplicating lifecycle and ownership validation.
- Static checks: backend evidence validator PASS; `git diff --check` PASS with line-ending advisories; owned-path and precise forbidden-source scans PASS.
- Decision impact: DF10 remains unmergeable and must return to the worker for a real RED/GREEN repair, then undergo another independent verification round.
- Full report: `D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df10/doc/tasks/20260812-frontline-pqc-dcc-qa-df10/independent-test-report-round-3.md`.

## Wave 7 DF10 / DF11 Round-4 Independent Verification

- Result: PASS.
- DF10 backend gate: `MesQaInspectionRegulationServiceTest` plus `MesFrontlinePqcContextServiceTest` ran 18 tests with 0 failures, 0 errors and 0 skipped; backend-api validator, bug-regression validator, `git diff --check` and forbidden scans passed.
- DF10 contract review: one-line PQC projection now consumes `MesQaInspectionRegulationService#getLockedVersionForOrder`; the private locked QA mapper aggregate is absent; dedicated PQC item VO no longer exposes `acceptanceStandard/processInspectionMethod` aliases.
- DF11 frontend gate: `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs`, `pnpm ts:check`, frontend-feature validator, bug-regression validator, `git diff --check` and production-source forbidden scans passed.
- DF11 contract review: activeOrderId is the picker/request/cache identity; task source is under `pqcTaskOptions`; stale active-order response isolation is covered; unused production loader/rule-order exports are absent.
- Reports: `D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df10/doc/tasks/20260812-frontline-pqc-dcc-qa-df10/independent-test-report-round-4.md`; `D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-df11/doc/tasks/20260812-frontline-pqc-dcc-qa-df11/independent-test-report-round-4.md`.
- Blocker: commit, merge and cleanup were not attempted because `E:/IntRuoyi` is dirty with extensive unrelated changes outside DF10/DF11 ownership.

## Wave 7 DF10 / DF11 Clean Integration Verification

- Result: PASS for the clean integrated branch; BLOCKED for E:/IntRuoyi fast-forward.
- Integrated branch: task/20260812-frontline-pqc-dcc-qa-df11 at 817687224, after merging DF10 commit fa520e027.
- Backend: MesQaInspectionRegulationServiceTest + MesFrontlinePqcContextServiceTest ran 18 tests with 0 failures/errors; BUILD SUCCESS at 2026-08-14T05:10:33+08:00.
- Frontend: frontline-pqc-qa-process-contract-static.spec.cjs PASS; pnpm ts:check exit 0.
- Static gates: branch runtime port guard PASS for DF11 slot 14 / 8095 / 48095; backend-api, frontend-feature and bug-regression evidence validators PASS; git diff --check PASS; forbidden scans PASS.
- Merge blocker: the protected main-workspace patch for 6 dirty overlapping files can apply cleanly for 4 files, but conflicts on MesFrontlinePqcContextServiceImpl.java and FrontlineFixedTemplatePanel.vue; therefore int_main was not fast-forwarded and DF10/DF11 worktrees were not cleaned.
## DF10/DF11 int_main integration - 2026-08-14

- Decision: PASS
- int_main: 817687224
- Contract authority: DF10/DF11 formal contract
- Conflict handling: protected patch retained; conflicting old edits were not reapplied
- Verification: integrated backend 18 tests PASS; frontend static contract and ts:check PASS; all evidence validators PASS; int_main diff check and runtime-port guard PASS
