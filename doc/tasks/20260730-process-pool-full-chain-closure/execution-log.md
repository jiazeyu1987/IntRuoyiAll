# Execution Log

## 2026-07-30 Bootstrap
- User request: 在新 worktree 中补齐生产一线报工/记录本/资源池全链路。
- Worktree created: `D:\IntRuoyiWorktree\process-pool-full-chain-closure`.
- Branch: `codex/process-pool-full-chain-closure-20260730`.
- Base commit: `b48265b9`.
- Runtime slot: `int_main slot=1`, frontend `8082`, backend `48082`.
- BDD: full-chain frontline process pool -> Given a production work order has planned start time and authorized device-account/process/employee/template bindings, When production and PQC users submit real report-work entries from the frontline page, Then the system persists formal feedback, recordbook entries, process-pool events, FIFO allocations, clamped review copies, and team-leader visibility through real APIs and UI.
- GREEN: experience-preflight -> PASS, read worktree, branch runtime, local runtime, backend, frontend, database, E2E, login, and matching worktree experience rules.

## RED Candidates
- RED: frontend static contract -> should fail while `FrontlineFixedTemplatePanel.vue` validates payload and shows success without calling `ProFeedbackApi.frontlineSubmit`.
- RED: backend binding source contract -> should fail while `MesFrontlineDeviceAccountRouteBindingSource` and `MesFrontlineTemplateBindingSource` have no production bean.
- RED: PQC process-pool contract -> should fail while frontend PQC submit is blocked and PQC result enum is not mapped to pool `SUCCESS/FAILURE`.

## 2026-07-30 T1 Binding Source
- BDD: binding source formal data -> Given a device/shared frontline account has configured route/process/device/workstation bindings and an actual employee has a configured fixed template, When the frontline page loads processes and switches employee, Then data comes from formal binding tables and missing bindings fail fast instead of using mock/default candidates.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineBindingSourceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlineDeviceAccountRouteBindingSourceImpl` / formal binding persistence did not exist.
- Implemented: added `mes_frontline_device_account_route_binding` and `mes_frontline_employee_template_binding` migration, H2 fixture schema, DO/Mapper classes, and production binding source implementations.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineBindingSourceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Next: T2 starts from a frontend static RED that proves the one-line submit button cannot stop at payload validation or block PQC submission.

## 2026-07-30 T2 Frontline Real Production/PQC Submit
- BDD: zero-loss frontline submit -> Given an operator completes a production or PQC report with loss quantity `0`, When the formal submit request is built, Then zero is accepted as a valid non-negative quantity and the submit is not rejected as missing.
- BDD: total loss is not duplicated as labor scrap -> Given the simplified frontline template captures only total loss and no labor/material/other classification, When the formal feedback payload is built, Then `lossQuantity` contains the total and all classified scrap fields remain empty.
- BDD: actual employee electronic signature -> Given the selected actual employee has electronic-signature authorization and a matching password, When the frontline combined submit records its signature, Then the persisted signature belongs to that employee, uses server time, and returns the generated signature ID; disabled authorization, invalid password, or failed persistence must fail before downstream records are written.
- BDD: formal report-work context provenance -> Given frontline submit requires work order, production task, item, approver, recordbook, and feedback type, When the production/PQC page builds the request, Then every field must come from an explicit report-work route context or a formal backend response; missing fields must remain visible blockers and must not receive defaults.
- RED: `node tests/e2e/frontline-real-submit-static.spec.js` -> FAIL, expected reason: `FrontlineFixedTemplatePanel.vue` had no non-negative quantity validator, so zero loss still passed through the positive-ID/quantity guard.
- GREEN: `node tests/e2e/frontline-real-submit-static.spec.js` -> PASS after adding `requireNonNegativeNumber`, retaining total loss only in `lossQuantity`, and leaving labor/material/other scrap classification empty.
- RED: `node tests/e2e/frontline-real-submit-static.spec.js` -> FAIL, expected reason: missing formal report-work context was not exposed before the electronic-signature prompt.
- GREEN: `node tests/e2e/frontline-real-submit-static.spec.js` -> PASS after adding `formalSubmitContextMissingFields`, submit blocking, and the visible `请从正式报工入口进入` status without any default IDs.
- Added focused signature regression: `MesFrontlineSubmitSignatureServiceImplTest` covers authorized actual-employee password signing, disabled authorization, invalid password, server-time audit fields, and insert-without-generated-ID failure.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineSubmitSignatureServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackRawLimitBypassTest,MesFrontlineSubmitSignatureServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests, 0 failures/errors.
- GREEN: `node tests/e2e/frontline-real-submit-static.spec.js; node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS, both focused frontend contracts.
- GREEN: `git diff --check` -> PASS; only Git CRLF conversion warnings were emitted.
- BLOCKED: `pnpm ts:check` -> FAIL before TypeScript execution because `node_modules/.bin/cross-env.cmd` is missing (`'cross-env' is not recognized`); `node_modules/vue-tsc/bin/vue-tsc.js` is also missing. Static contracts are not treated as a TypeScript-check substitute.
- Context provenance audit: the panel reads `workOrderId/taskId/itemId/approveUserId/recordbookId/feedbackType` only from `route.query`. `feedback/index.vue`, `BatchProductionFillPage.vue`, and `BatchPqcFillPage.vue` render the panel without a formal submit context; `EdhrBatchRecordTabs.vue` navigates with only `{ path: nextPath }` and discards query; `ProFeedbackApi` has no formal frontline report-work context resolver.
- Context impact: the direct current entries cannot produce a real combined submit because five required report-work fields plus recordbook binding have no formal source. T2 now fails fast before signature instead of inventing defaults, but the entry-to-context data chain remains a product blocker outside the boundary fixes completed here.
- Scope guard: no FIFO, review-copy, team-leader workbench, E2E implementation, or `task-state.json` edit was performed.

## 2026-07-30 T2 Formal Submit Context Closure
- BDD: formal submit context resolver -> Given a selected production task and an authorized device-account process binding, When the frontline panel prepares production or PQC submit, Then `taskId/itemId/approveUserId/recordbookId/feedbackType` are resolved by backend formal context and missing binding data fails fast before signature.
- RED: `node tests\e2e\frontline-real-submit-static.spec.js` -> FAIL, expected reason: missing `ProFeedbackApi.resolveFrontlineSubmitContext`, no `ProTaskSelectDialog`, and report-work context still stitched from route query.
- RED: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> FAIL, expected reason: eDHR fill tabs discarded route query and lost report-work context.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineSubmitContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: submit-context VO/service/controller did not exist.
- Implemented: added submit-context VO, controller endpoint, production service, binding columns for approver/recordbook/feedback type, mapper query, frontend API resolver, task selector wiring, backend-context based request build, and tab query preservation.
- GREEN: `node tests\e2e\frontline-real-submit-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineSubmitContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures/errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackRawLimitBypassTest,MesFrontlineSubmitSignatureServiceImplTest,MesFrontlineSubmitContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests, 0 failures/errors.
- GREEN: `git diff --check` -> PASS; only Git CRLF conversion warnings were emitted.
- BLOCKED then resolved: `pnpm ts:check` initially failed because worktree `node_modules` lacked `cross-env.cmd` and `vue-tsc.js`; ran `pnpm install --frozen-lockfile` in `IntRuoyiFronted`, then `pnpm ts:check` -> PASS.
- T2 status: completed. Remaining scope starts at T3 FIFO orchestration; no FIFO, review-copy, team-leader workbench, or full-chain E2E completion is claimed here.


## 2026-07-30 T3 FIFO Orchestration
- BDD: FIFO formal orchestration -> Given available OUTPUT quantity fragments and target production work orders, When FIFO allocation runs, Then the service locks formal fragments and work orders, orders targets only by production work order planned start time, persists allocation lines, and updates fragment available quantity/status without schedule-order fallback.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolFifoOrchestrationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesProcessPoolFifoOrchestrationService` did not exist.
- Implemented: added `MesProcessPoolFifoOrchestrationCommand` and `MesProcessPoolFifoOrchestrationService`, formal available OUTPUT fragment query, target work-order lock/read, existing target allocation read, FIFO allocation delegation, and fragment progress update.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolFifoOrchestrationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures/errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolFifoAllocationSchemaTest,MesProcessPoolFifoAllocationServiceTest,MesProcessPoolFifoAllocationConcurrencyTest,MesProcessPoolAllocatedFragmentLockTest,MesProcessPoolFifoOrchestrationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests, 0 failures/errors.
- T3 status: completed. Remaining scope starts at T4 automatic review-copy rules.


## 2026-07-30 T4 Automatic Review-Copy Rules
- BDD: automatic review-copy rules -> Given an event has raw frontline values and formal review-copy rules for process/device/template/field, When a reviewer submits a review copy from rules, Then mappings are generated from the formal rule source, out-of-range values clamp to min/max, original and corrected values are both retained, and FIFO-affecting fields derive their source quantity fragment.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolReviewCopyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesProcessPoolReviewCopyRuleDO` / mapper and `generateAndSubmitReviewCopyFromRules` did not exist.
- Implemented: added review-copy rule DO/Mapper, MySQL/H2 schema, service automatic-rule method, source quantity fragment derivation, schema contract assertions, and missing-rule fail-fast behavior.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolReviewCopyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures/errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolReviewCopySchemaTest,MesProcessPoolReviewCopyControllerTest,MesProcessPoolReviewCopyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests, 0 failures/errors.
- T4 status: completed. Remaining scope starts at T5 team-leader workbench.


## 2026-07-30 T5 Team-Leader Workbench
- BDD: team-leader read-only workbench -> Given production and PQC submissions have entered the formal process pool with FIFO, audit-copy, and revision states, When a team leader opens the workbench for a submit date, Then the page loads through a dedicated read-only team-leader API and shows submitted events, PQC result, FIFO state, audit-copy state, and original-record modification state without write actions or browser-storage data.
- RED: `node tests\e2e\process-pool-team-leader-workbench-static.spec.js` -> FAIL, expected reason: `src/api/mes/pro/processpool/teamLeaderWorkbench.ts` did not exist.
- RED: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTeamLeaderWorkbenchServiceTest,MesProcessPoolTeamLeaderWorkbenchControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `ProcessPoolTeamLeaderWorkbenchRespVO` / team-leader workbench service and controller did not exist.
- Implemented: added dedicated team-leader read-only backend controller/service/VO, frontend API wrapper, hidden route, and `TeamLeaderWorkbenchPage.vue`; the new surface reuses the formal process-pool timeline read model while enforcing `mes:pro-process-pool-team-leader:query` on its own endpoint.
- GREEN: `node tests\e2e\process-pool-team-leader-workbench-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTeamLeaderWorkbenchServiceTest,MesProcessPoolTeamLeaderWorkbenchControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests, 0 failures/errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineDateFilterTest,ProcessPoolTimelineContentSummaryTest,ProcessPoolTimelineTraceabilityTest,ProcessPoolTeamLeaderWorkbenchServiceTest,MesProcessPoolTeamLeaderWorkbenchControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures/errors.
- GREEN: `node tests\e2e\process-pool-review-copy-and-revision-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- T5 status: completed. Remaining scope starts at T6 full-chain real E2E.

## 2026-07-30 T6 Full-Chain Entry Closure
- BDD: formal FIFO orchestration entry -> Given production output fragments are available and a team leader selects target production work orders, When FIFO orchestration is submitted from the dedicated process-pool page, Then the backend locks formal fragments and production work orders, sorts only by work-order planned start time, persists allocations, and returns the visible allocation result.
- BDD: automatic review-copy entry -> Given a production event has formal enabled limit rules for its process, device, template, and fields, When a reviewer submits from the review-copy page using the automatic-rule action, Then field mappings come from the rule table, allocation-affecting fields resolve the matching event quantity fragment, values clamp to configured limits, and the original event remains unchanged.
- BDD: full-chain real frontend path -> Given a task-owned local fixture provides a real device account, employee, production/PQC tasks, recordbook, signature authorization, rules, and production work orders, When Playwright uses the real `8082/48082` frontend/backend pair to submit production and PQC reports, run FIFO, submit an automatic review copy, and open the team-leader workbench, Then the UI and final read-only verification show report-work, recordbook, process-pool, PQC, FIFO, and review-copy evidence for the same task marker.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: `process-pool-full-chain-real-flow.e2e.js` did not exist, so the required real Playwright path and cleanup contract were absent.

## 2026-07-30 T6 Timeline Read-Model Closure
- BDD: tenant-scoped employee and FIFO projection -> Given a process-pool event references an actual employee and production output has FIFO allocation lines, When the timeline or team-leader workbench reads that event, Then the employee nickname and aggregated FIFO status/summary come from formal tenant-scoped tables and the one-to-many allocation lines do not duplicate the event row.
- RED: `node yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL, expected reason: `actualEmployeeUserName`, `fifoAllocationStatus`, and `fifoAllocationSummary` were still hardcoded `NULL`; first assertion failed because `actual_employee.nickname AS actualEmployeeUserName` was absent.
- Implemented: joined `system_users` by `tenant_id + actual_employee_id`, aggregated OUTPUT fragment totals and FIFO allocation lines by `tenant_id + source_event_id`, and projected `PENDING` / `PARTIAL` / `ALLOCATED` plus allocated/pending quantities without directly joining one-to-many allocation rows to the timeline.
- GREEN: `node yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineDateFilterTest,ProcessPoolTimelineContentSummaryTest,ProcessPoolTimelineTraceabilityTest,ProcessPoolTeamLeaderWorkbenchServiceTest,MesProcessPoolTeamLeaderWorkbenchControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures/errors.
- NOTE: an optional standalone Node XML-parser probe could not run because `@xmldom/xmldom` is not installed in the backend workspace; no dependency or fallback was added. Maven resource processing and the focused mapper contract passed.

## 2026-07-30 T6 Real E2E Iteration 1
- Runtime reload: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS; copied the target Jar to `output/runtime/int_main-slot1/yudao-server-exec.jar`, restarted only owned backend PID `58508` as PID `12504`, verified `48082` health `UP`, and verified target/runtime SHA-256 both equal `AF834E6AEE0CBB2FC1AE051E08A80AF7205F9A263A746B1A0E329B1F5930515B`.
- RED: real Playwright full-chain command on explicit `8082/48082` -> FAIL before browser business writes because fixture post-insert count queries compared `utf8mb4_unicode_ci` columns with an `utf8mb4_0900_ai_ci` user variable; cleanup hit the same collation conflict.
- Recovery evidence: write API evidence remained empty; task-created signature authorization row was removed by `finally`; exact marker `PPFC-1785433550946-39908` prerequisite rows were then removed transactionally and all scoped residual counts verified as zero.
- BDD: collation-safe task fixture cleanup -> Given task fixture tables use both `utf8mb4_unicode_ci` and `utf8mb4_0900_ai_ci`, When preparation and cleanup identify marker-owned rows, Then comparisons use escaped SQL literals that adopt each target column collation and never compare a user variable directly to mixed-collation columns.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: marker SQL literals and the prohibition on comparison with `@run_key` were absent.
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> PASS after replacing marker comparisons with escaped exact/prefix/contains literals; `node --check tests\e2e\process-pool-full-chain-real-flow.e2e.js` and focused `git diff --check` -> PASS.

## 2026-07-30 T6 Real E2E Iteration 2
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785433871727-63436` -> FAIL at the production page before any business write because the task route/process IDs from the backend response did not match the numeric route-query context under strict equality.
- Browser evidence: the real page loaded the selected task but displayed `工序：未选择` and `员工：未选择`; failure screenshot: `IntRuoyiFronted/output/playwright/process-pool-full-chain-real-flow/PPFC-1785433871727-63436/99-failure.png`.
- Runtime evidence: the page fell back to an unrelated first process and attempted employee `914520`, producing `实际员工 914520 在当前工序 923030 下没有正式模板绑定`; the intended task-owned production process was never selected.
- Cleanup evidence: `writeEvidence=[]`, direct API business writes remained `0`, the task-created signature authorization row `922742` was removed, and all fixture residual counts including tasks, routes, feedback, FIFO lines, work orders, recordbooks, review rules, signatures, process-pool events, recordbook entries, and user-post binding were `0`.
- BDD: route-query process identity -> Given route/process IDs may arrive as JSON strings while route query context is normalized to numbers, When the frontline page chooses its initial process, Then route ID, route-process ID, and process ID use one string-semantic identity comparison and select the exact requested process instead of the first available process.
- BDD: isolated employee fixture -> Given a shared existing post can contain unrelated employees, When the full-chain E2E prepares its workstation worker and user-post bindings, Then it creates a task-owned post bound only to the login employee and removes both bindings and the post during cleanup.
- RED: `node tests\e2e\frontline-real-submit-static.spec.js` -> FAIL, expected reason: `FrontlineFixedTemplatePanel.vue` did not import the shared route-query identity helper and still compared the initial process IDs with strict equality.
- Schema evidence: the current `system_post` table exposes the formal `id/code/name/sort/status/remark/audit/deleted/tenant_id` columns; shared post `13` already had three unrelated active employees in tenant `122`, so it was not a deterministic employee fixture.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: the real-flow runner reused constant post `13` and did not create or clean a task-owned post.
- Implemented: `FrontlineFixedTemplatePanel.vue` now uses `sameRouteQueryId` for current-process identity and initial route/process matching; the real E2E creates one marker-owned `system_post`, binds only login employee `914523`, uses that post for both workstations, and removes user-post then post in `finally` cleanup.
- GREEN: `node tests\e2e\frontline-real-submit-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> PASS.
- GREEN: `node --check tests\e2e\process-pool-full-chain-real-flow.e2e.js` -> PASS.
- GREEN: focused `git diff --check` -> PASS; only the existing Windows line-ending warning was emitted.

## 2026-07-30 T6 Real E2E Iteration 3
- Runtime gate: `pnpm ts:check` -> PASS; frontend `8082` returned HTTP `200`, backend `48082` health was `UP`, both listeners belonged to the current worktree, and the stable runtime Jar SHA-256 remained `AF834E6AEE0CBB2FC1AE051E08A80AF7205F9A263A746B1A0E329B1F5930515B`.
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785434593673-56264` -> FAIL at the one-shot process-label assertion before any business write.
- Product-path evidence: backend requests used the exact task-owned production context `routeId=922286`, `routeProcessId=928943`, `processId=923032`, switched actual employee `914523`, and resolved submit context for task `974593`; `pageErrors=[]`.
- Fixture evidence: task-owned post count was `1`; `finally` removed signature authorization row `922743`, user-post binding, and post, with every scoped residual count equal to `0`.
- Root cause: `[data-frontline-production-operator]` is rendered before the component finishes asynchronous process, employee, template, and submit-context initialization; the E2E read `strong.innerText()` once instead of waiting for the expected process label.
- BDD: asynchronous initial-process rendering -> Given the production shell is visible before process initialization finishes, When the E2E verifies the selected process, Then it waits for the exact task-owned process text to become visible before asserting or entering quantities.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: `assertFrontlineProcess` did not wait for a locator filtered by the expected process name.
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> PASS after waiting for the exact process label; Node syntax and focused diff checks also passed.

## 2026-07-30 T6 Real E2E Iteration 4
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785434767087-60040` -> FAIL after the correct process and employee were visibly selected because `ensureFrontlineEmployee` reopened the employee picker while the component's automatic selection update detached that option from the DOM.
- Browser evidence: failure screenshot shows the task-owned process, employee `Codex单元格链接E2E`, device card, and status `准备提交`; `pageErrors=[]` and no business write request was sent.
- Cleanup evidence: signature authorization row `922744`, task-owned post, and all scoped fixtures were removed; every residual count was `0`.
- BDD: isolated employee auto-selection -> Given the task fixture binds exactly one active employee to its task-owned post, When the component finishes process initialization, Then the E2E waits for that employee label and does not reopen the employee picker or issue a duplicate switch.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: `ensureFrontlineEmployee` still called `employeeCard.click()` and drove a redundant picker selection.
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> PASS after waiting only for the auto-selected employee label; Node syntax and focused diff checks passed.

## 2026-07-30 T6 Real E2E Iteration 5
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785434991633-72412` -> production submit PASS, then PQC employee verification timed out because the unscoped locator resolved the hidden production-page employee node retained in the DOM.
- Real write evidence: `POST /admin-api/mes/pro/feedback/frontline/submit` returned HTTP `200`, business code `0`, `feedbackId=774`, `recordbookEntryId=1`, `recordbookEventId=1`, and `processPoolEventId=7`.
- Browser evidence: production screenshot `01-production-submit.png` was captured; the PQC failure screenshot showed the task-owned order/task/process and no console/page errors, but the fixed five-column header compressed the employee card at the right edge.
- Cleanup evidence: signature authorization row `922745`, production write products, task-owned post, and all fixture data were removed; every residual count was `0`.
- BDD: visible operator instance -> Given route transitions may retain a hidden previous fill-page instance in the DOM, When E2E reads process or employee cards, Then locators are scoped to `.frontline-operator-screen:visible`.
- BDD: container-safe frontline header -> Given the application content area is narrower than the browser viewport, When production or PQC fixed-template headers render, Then four or five top cards divide the actual container width without fixed-width overflow or employee-card clipping.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: process and employee helpers were not scoped to the visible operator instance.
- RED: `node tests\e2e\frontline-real-submit-static.spec.js` -> FAIL, expected reason: production/PQC headers still used fixed-width columns, including the overflowing `320px 320px 420px 1fr 240px` PQC layout.
- GREEN: both focused static contracts -> PASS after visible-instance scoping and container-width card grids; Node syntax and focused diff checks passed.

## 2026-07-30 T6 Real E2E Iteration 6
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785435334326-47024` -> production submit PASS, PQC submit PASS, FIFO allocation PASS, automatic review-copy PASS, then final read-only database verification failed at `productionRecordbookContainsMarker`.
- Real write evidence: production returned `feedbackId=775`, `recordbookEntryId=2`, `recordbookEventId=2`, `processPoolEventId=8`; PQC returned `feedbackId=776`, `recordbookEntryId=3`, `recordbookEventId=3`, `processPoolEventId=9`.
- FIFO evidence: total allocated quantity `50`; early work order `925920` received `20` first and late work order `925921` received `30` second.
- Review-copy evidence: automatic rule submission returned review copy ID `3`; screenshots exist for production, PQC, FIFO, and review-copy stages; `pageErrors=[]`.
- Root cause: the recordbook marker is embedded in values such as `<runMarker>-DEVICE-PARAMETER`, while the verification used `JSON_SEARCH(..., 'one', <bare runMarker>)` and therefore required an exact standalone JSON scalar.
- Cleanup evidence: signature authorization row `922746`, all four write-stage products, task-owned post, and all fixture data were removed; every residual count was `0`.
- BDD: recordbook marker containment -> Given the run marker is intentionally embedded inside persisted device-parameter and payload strings, When final read-only verification checks traceability, Then it uses an escaped `%<runMarker>%` contains literal instead of exact JSON scalar matching.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: `verifyDatabaseState` still used `JSON_SEARCH(..., 'one', <bare marker>)` and did not define or use an escaped `%${state.runMarker}%` containment literal.

## 2026-07-30 T6 Real E2E Iteration 7
- GREEN: recordbook containment contract, Node syntax check, and focused diff check passed after final verification switched to `entry_content_json LIKE %<runMarker>%`.
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785435654610-65120` -> production submit PASS, PQC submit PASS, FIFO allocation PASS, automatic review-copy PASS, database verification PASS, then the team-leader assertion consumed a `total=0` workbench response.
- Real write evidence: production `feedbackId=777`, `recordbookEntryId=4`, `recordbookEventId=4`, `processPoolEventId=10`; PQC `feedbackId=778`, `recordbookEntryId=5`, `recordbookEventId=5`, `processPoolEventId=11`; FIFO allocated `20` to earlier work order `925924` and `30` to later work order `925925`; automatic review copy ID `4`.
- Runtime date evidence: the local persisted event date returned by the database was `2026-07-31`, which is after the authoritative task date `2026-07-30`; the E2E intentionally filters by the persisted server date rather than assuming the task date.
- Cleanup evidence: signature authorization row `922747`, all write-stage products, task-owned post, and all fixture data were removed; every scoped residual count was `0`.
- Root cause: `TeamLeaderWorkbenchPage.vue` calls `getWorkbench()` from `onMounted`, while the E2E response predicate matched only the endpoint and method; it could consume the initial page-load response instead of the later search response containing the selected submit date and employee.
- BDD: exact workbench search response -> Given the workbench performs an automatic initial request before the E2E fills its filters, When Playwright clicks Search, Then it waits only for the GET response whose query contains the persisted `submitDate` and selected `employeeUserId`.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: the workbench response predicate did not inspect `submitDate` and `employeeUserId`.

## 2026-07-30 T6 Real E2E Iteration 8
- GREEN: the workbench response predicate now matches the selected persisted submit date and employee ID; focused static contract, Node syntax, and diff checks passed.
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785435866214-50904` -> all four write stages, database verification, workbench filtered page response, list API assertions, and visible row assertions PASS; the final detail assertion read the drawer before asynchronous detail content was rendered.
- Workbench evidence: filtered page response returned `total=2`; production row showed actual employee `Codex单元格链接E2E`, FIFO `ALLOCATED`, and review copy `SUBMITTED`; PQC row showed `SUCCESS`.
- Real write evidence: production event `12`, PQC event `13`, FIFO targets `925928` then `925929`, automatic review copy ID `5`; `pageErrors=[]`.
- Cleanup evidence: signature authorization row `922748`, all write-stage products, task-owned post, and all fixture data were removed; every scoped residual count was `0`.
- Root cause: the E2E waited only for the drawer shell to become visible, but `openDetail` sets `detailVisible=true` before awaiting the detail API; immediate `drawer.innerText()` can observe the loading shell before employee/FIFO/review-copy content exists.
- BDD: rendered team-leader detail -> Given opening a production event displays the drawer before its read-only detail request completes, When Playwright verifies the detail, Then it waits for the exact event-ID response, asserts API statuses, and waits for the actual employee plus rendered FIFO and audit-copy summaries.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: the runner lacked the detail endpoint constant, target event response wait, and rendered-content waits.

## 2026-07-30 T6 Real E2E Iteration 9
- GREEN: exact event-ID detail response wait, API employee/FIFO/audit assertions, and employee rendering wait passed.
- RED: real Playwright full-chain command on explicit `8082/48082` with marker `PPFC-1785436079720-78384` -> all business, database, workbench page, and detail API assertions PASS; the final UI assertion expected integer text `已分配 50，待分配 0`, while the real drawer rendered `已分配 50.000000，待分配 0.000000`.
- Read evidence: workbench page returned `total=2`; detail endpoint returned HTTP `200`, business code `0`, event ID `14`; actual employee, `ALLOCATED`, and `SUBMITTED` assertions passed.
- Future-date evidence: the local runtime again persisted and displayed `2026-07-31`, which is after the authoritative current date Thursday, July 30, 2026; this remains recorded as a local runtime clock/date anomaly rather than being normalized away by the E2E.
- Cleanup evidence: signature authorization row `922749`, production event `14`, PQC event `15`, review copy `6`, task-owned post, and every scoped fixture were removed; all residual counts were `0`.
- BDD: decimal-equivalent FIFO summary -> Given the formal read model may serialize DECIMAL quantities with trailing zero scale, When the detail E2E verifies FIFO quantities, Then it accepts numeric-equivalent trailing zeros and requires the drawer to render the exact summary returned by the detail API.
- RED: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> FAIL, expected reason: the runner still hardcoded the integer FIFO summary instead of validating numeric semantics and exact API summary rendering.

## 2026-07-30 T6 Final Full-Chain PASS
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow-static.spec.js` -> PASS after validating DECIMAL numeric equivalence and requiring the drawer to render the exact FIFO summary returned by the detail API.
- GREEN: `node tests\e2e\process-pool-full-chain-real-flow.e2e.js` on explicit worktree runtime `8082/48082` -> PASS with marker `PPFC-1785436288416-51980`.
- Real E2E evidence: production submit returned `feedbackId=783`, `recordbookEntryId=10`, `recordbookEventId=10`, `processPoolEventId=16`.
- Real E2E evidence: PQC submit returned `feedbackId=784`, `recordbookEntryId=11`, `recordbookEventId=11`, `processPoolEventId=17`, and database evidence `pqcResult=SUCCESS`.
- Real E2E evidence: FIFO allocated `50` total from production output, first to earlier planned work order `925936` quantity `20`, then later work order `925937` quantity `30`.
- Real E2E evidence: automatic review copy returned `reviewCopyId=7`; rule `OUTPUT_QUANTITY` retained raw `50`, corrected to `40`, with limits `20..40`.
- Real E2E evidence: team-leader workbench returned `total=2`; detail endpoint matched event `16`; rendered UI showed actual employee `Codex单元格链接E2E`, FIFO `ALLOCATED`, and audit copy `SUBMITTED`.
- Real E2E anti-fallback evidence: `mockUsed=false`, `directApiBusinessWrites=0`, `pageErrors=[]`, six stage screenshots captured under `IntRuoyiFronted/output/playwright/process-pool-full-chain-real-flow/PPFC-1785436288416-51980/`.
- Cleanup evidence: electronic signature authorization status `REMOVED_TASK_ROW`; all task fixture residual counts were `0`.
- Environment risk: T6 run recorded persisted submit date `2026-07-31` while the task-date context was `2026-07-30`; Windows/Docker/MySQL local clock drift is recorded as environment risk and was not hidden by business-code normalization.

## 2026-07-31 Independent Verification And Experience Consolidation
- GREEN: `node yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- GREEN: six frontend static contracts -> PASS: `frontline-real-submit-static`, `edhr-frontline-fill-tabs-static`, `process-pool-fifo-orchestration-static`, `process-pool-review-copy-and-revision-static`, `process-pool-team-leader-workbench-static`, `process-pool-full-chain-real-flow-static`.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS, `migrationCount=400`.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for branch `codex/process-pool-full-chain-closure-20260730`, profile `int_main`, frontend `8082`, backend `48082`.
- GREEN: `git diff --check` -> PASS; only line-ending conversion warnings were emitted.
- GREEN: backend final targeted surefire reports -> 58 tests, 0 failures, 0 errors.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260730-process-pool-full-chain-closure\backend-api-evidence.md` -> PASS.
- Experience consolidation: updated `docs/e2e-rules.md` with automatic first-load response predicate, asynchronous drawer rendering, embedded marker search, and DECIMAL API/UI consistency gates; updated `docs/experience-index.md` with routeable keywords.
- Main workspace check: `E:\IntRuoyi` currently has unrelated concurrent dirty files (`IntRuoyiFronted/tests/e2e/codex-runner-*`, `doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/`, `doc/tasks/20260730-test-management-serial-routes-repair/`). This task will not touch them; fast-forward fusion/worktree deletion is blocked until the main workspace is clean or a separate explicit handling path is authorized.
- Status update: `task.md` and `task-state.json` set to `ready_for_closeout`; `verification-report.md` updated with AC-01 through AC-07 matrix and final verification evidence.
