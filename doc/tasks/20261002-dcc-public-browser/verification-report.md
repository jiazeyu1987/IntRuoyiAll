# Verification report

## G44 bounded route blocker display

Root真实页面已证明两个同错误原因无法定位审批人。本增量只改upload readiness li展示。实际Vue parser提取生产template AST子树，Vue compiler/renderer执行formalVO字段fixture：同message两阶段/两大Long账号分别显示，缺身份明确未记录，原error/ready分支不显示旧blocker，文本字符不变成markup。有效RED3/4→GREEN4，受影响upload application/departments/display三文件27PASS，ownedeslint最终exit0。网络/Element宿主是离线边界，不是真实业务PASS；Root类型/HMR/页面验证待其执行。当前uploadSHA 651eb911f93f7f5cb6de5d7cc2d0425e40d4654528c6e3a0cdabf7b011343faa；反向替换该唯一显示块精确恢复原ac6d4acc...c7169，原G20seal保留。完整目标未完成。

Current state: independent frontend changes are reviewable; full project browsing and relation parent integration remain open. No completion claim for F01-R07 as a whole.

## RED / GREEN
- Initial project browser test file: 5 FAIL because public project coordinator/mount did not exist and latest-only WORKING gate remained. Raw red.log retained.
- Upload folder test: 3 FAIL for cross-project computed validation, stale loading after clearing, and same-ID/wrong-project folder selection. Actual handler/tree tests; upload-folder-red.log retained.
- Working handler test: one expected FAIL (2 entries vs 1) proved explicit actionLocked was ignored. working-locked-red.log retained.
- GREEN: 6 project wrapper/actual-browser-handler tests, 3 mounted parent Vue integration tests and 22 upload tests passed (31 Node tests); existing WORKING static contract also passed.
- Local ESLint on browser index, project-browser.ts, ProjectBrowserPanel.vue and upload index passed. git diff --check passed after removing a trailing blank line in the adjusted static contract.

## Boundaries
Actual project/folder/selector/reference wrappers and actual Vue SFCs/handlers are executed offline. Axios transport, router/auth and Element host are fixture boundaries. These are not real-page E2E and not evidence of database/runtime deployment. No full types/build/Maven here; Root owns those checks. No Git commit/push, service, real DB or remote work.

## Implemented
- Public controlled-project panel loads real project pagination, preserves empty logical folders, and maps exact B projectFolderId independently from NAS directoryId.
- Global selector reads contain no project/folder; totals retain server facts; old list requests cannot replace a changed scope.
- D project references component mounts with enabled-account + formal target leader identity, pinned version wrappers, exact cancellation, server usage count and orange badge. No role/admin/OWNER bypass.
- Body buttons depend on actual canPreview; operation navigation preserves exact selected Long ID. Preview opens a separate browser window.
- Existing browser permits owned, unlocked earlier WORKING iterations to enter their actual detail application; explicit actionLocked and checkout blocks remain.
- Upload folder snapshot has its own project identity; pending switch does not validate old folders under new project, cancellation retains old selection, clearing releases loading and invalidates late requests.

## Remaining for Root coordination
1. Supply a distinct authoritative all-file project browser projection and shared wrapper including WORKING/in-flight/all allowed version entries. Current latestControlled selector remains only the controlled chooser. The public tab label is explicit about this restricted scope.
2. Current/history DccFileRelations public-parent integration is not complete. Existing current response lacks body permission and names; formal detail can supply canPreview without overwriting history, but mutable UI needs a formal editable capability or agreed parent authority. See coordination.md.
3. Root owns final shared types/build, cleanup and Git authorization. Task remains in_progress for these required integration items.

## Owned files changed
- src/views/dcc/controlled-file/browser/index.vue
- src/views/dcc/controlled-file/browser/project-browser.ts (new)
- src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue (new)
- src/views/dcc/controlled-file/upload/index.vue
- scripts/dcc-public-browser.test.mjs (new)
- scripts/dcc-public-browser-vue.test.mjs (new)
- tests/unit/dcc-upload-project-folder-context.test.cjs (new)
- tests/unit/dcc-upload-application-page.test.cjs (fixture dependency only)
- tests/e2e/dcc-working-iteration-submit-static.spec.js (old latest-only expectation replaced with selected-working contract)

All code paths above are relative to IntRuoyiFronted in the integration worktree.

## G05 upload closure evidence

Upload-only delivery: ready_for_closeout for Root review; shared task remains in_progress.

- RED retained: g05-upload-summary-red.log, 3 expected failures; g05-upload-preflight-behavior-red.log proves false readiness during account-directory failure. GREEN summary/actual-submit handlers: 18 PASS. Final upload regression: 42 PASS in g05-upload-final.log. Three owned source files lint errors=0/warnings=0. Layout and training static contracts PASS; owned diff whitespace check PASS.
- Changed production files: upload/index.vue, upload/submitter.ts, upload/signoff-departments.ts. Tests: unit/dcc-upload-application-page.test.cjs and new unit/dcc-upload-confirmation-summary.test.cjs. G04 departments and exact Long submission remain covered.
- Confirmation displays every v1.4 upload summary fact and the controlled/effective date notice. Cancel/close preserve input and create no approval request; source, actual attribute-panel snapshot, relation facts or matrix approvers changing during async preflight/confirmation visibly block submission. Confirmed submission retains detached actual draft/file/attachments.
- Real enabled users are loaded through system/user/simple-list; only MATRIX_APPROVAL resolvedUserIds become approve identities. Directory or matrix errors remain visible, unmatched/unsafe/disabled/duplicate identities block, and exact Long strings survive display and tests. The approve list is read-only and independent of editable signoff departments.
- Empty effective dates remain empty until explicit choice. Past-date allowance is not asserted. Version wording distinguishes working checkin from formal partial/replacement approval, and upload uses the term 受控版本.
- Remaining: Root owns full type/build and real page E2E. Historical name/version static contract is blocked by its numeric-only wrapper signature and obsolete today-default expectation. Full source-name claim currently occurs only in formal submit; no read-only source-selection availability endpoint was found. Root/backend must coordinate the required early name-check entry. Earlier-date handling and multi-approver policy remain unconfirmed business rules; current route rule is displayed as configuration, not declared confirmed business policy.
- Evidence is offline actual handler/transport fixture coverage plus static contracts; no E2E, deployment, service or real DB success is claimed.

## G06 public pending distribution evidence

Delivery ready_for_closeout; shared Current Status remains in_progress for Root's integrated closeout.

- Files: src/views/dcc/controlled-file/workbench/index.vue, src/views/dcc/controlled-file/workflow/PendingWorkflowDistributionList.vue, src/api/dcc/controlledFile/workflowLifecycle.ts and new tests/unit/dcc-workbench-distribution-public.test.cjs. This batch leaves G05 upload, shared workflow/applicationRead and other agents' files unchanged.
- Valid RED: g06-workbench-red.log (5 public mount/role/error failures); g06-workbench-context-red.log (old tenant response incorrectly displayed). GREEN/adjacent directed regression: 39 PASS in g06-workbench-regression.log. Three source files lint 0/0; workbench/entry/file-context static contracts and owned whitespace check PASS.
- Public panel uses the existing GET workflow-lifecycle/pending-distribution with real remindersOnly. No PENDING_MANUAL_DISTRIBUTION alternate read remains. Explicit current doc_control role and distribute permission are both required; super_admin alone fails the business role. Exact decimal Long file IDs navigate to detail/{id}?mode=manage.
- UI separates controlled date from preset effective date, preserves ascending service date order/classification, displays OVERDUE/DUE/UPCOMING/FUTURE labels and future execution warning. Already-distributed records disappear on refresh; global refresh and return/re-entry read current facts. The pending metric consumes component state and shows — during loading/failure instead of a guessed zero; filtered values are labelled reminder counts.
- Missing reminder configuration/network/business errors are rendered locally with no success table or fallback status list. Role/context change removes or invalidates old rows; the loader rechecks nonreactive official tenant-cache values across asynchronous read. Partial/stale responses cannot supply current metric success.
- All evidence is offline mounted actual Vue/API-wrapper fixtures plus static checks. Root still owns full types/build, service/database deployment, real Playwright E2E and final Git/cleanup.
- Router integration dependency found during final source review: remaining.ts detail beforeEnter currently admits viewer/approval/browser-traceability/browser-management, but not mode=manage from workbench. The new owned handler emits exact path with mode=manage/from=workbench/returnTo; Root must add the proper formal guard branch and verify it. No origin spoofing or viewer flag is used to evade this guard. Until Root resolves it, the mounted navigation fixture proves handler output only, not successful real router entry.

## G07 current browser delivery evidence

- Owned files changed: browser/ProjectBrowserPanel.vue, browser/project-browser.ts, relations/DccProjectReferences.vue and scripts/dcc-public-browser.test.mjs / dcc-public-browser-vue.test.mjs. Existing browser/index.vue mounting remains sufficient and unchanged; G05/G06 unchanged.
- RED 4 failures retained. Color/leader/pinned/exact cancellation slice 10 PASS; relations/reference regressions 45 PASS. Three owned source files lint 0/0 and diff check PASS. Filename classes are set from real usage or actual reference existence; source clears after formal last cancellation count=0. Badge and selected-version facts remain intact.
- Default/status filter implementation calls existing browserScope with third options. applicationRead.ts ownership dependency currently blocks default/filter GREEN: still hardcoded latestVersionOnly=false. Pending mounted result 5 PASS/1 FAIL is preserved and not treated as complete. Root/detail_closure must complete wrapper before final integrated tests/types/build.
- Read-only gap: existing reference rows have no preview, operation or traceability navigation; selectedControlledFileId exists but FileVersion/ReferenceView omits canPreview. openPreview is only passed into the chooser. Proper selected-version body permission and pinned navigation require Owner coordination; no guessed permission or following-latest behavior added.
- This evidence is offline behavior/style/transport fixtures, not E2E. Shared task remains in_progress.

### G07 final status superseding the pending-wrapper snapshot

ready_for_closeout for Root Review. detail_closure added and verified the formal third browser options argument; the earlier hardcoded-false dependency is resolved.

- Actual browser wrapper/coordinator/rendered UI: 17 PASS. Browser + reference/relations combination: 62 PASS (g07-browser-regression-final.log). Final three owned source lint 0/0 and whitespace check PASS.
- Default is latest controlled with exact latestVersionOnly=true; explicit ALL/WORKING/approval/history choices request false plus the formal selected status on browserScope. UI shows one latest row before ALL and every selected actual version afterward; server totals are retained, no client pagination-filter substitution, stale response rejection remains. selectorScope calls remain unchanged.
- Source and actual reference filename text itself receives scoped orange, and last exact cancellation refreshes formal count zero/source ordinary color. Distinct-project count, leader permission, pinned version and cancelled reference ID tests remain passing.
- Current ownership changes only the three listed production files and two browser scripts; browser/index.vue needs no new mount change. No fulltypes/build/Maven/service/DB/Git/E2E ran here. Existing reference-row browsing/operation gap remains explicitly reported above for Root coordination.

## G08 UI-01 saved-reference readonly entry

ready_for_closeout for Root Review, superseding the G07 missing reference-row entry report. Shared Current Status remains in_progress.

- Production files: browser/ProjectBrowserPanel.vue, browser/project-browser.ts, relations/DccProjectReferences.vue, relations/project-reference-contract.ts. Tests updated: dcc-public-browser.test.mjs, dcc-public-browser-vue.test.mjs, dcc-project-reference-contract.test.mjs and dcc-relations-components.test.mjs under scripts/. Formal ProjectBrowserOptions type is used; no shared wrapper write.
- Effective RED: missing actions/denied source project supplementation (3 FAIL), late unmount still opening preview (1 FAIL), and post-create permission-read failure misreported as write failure (1 FAIL). Original logs retained. Final combined actual Vue/wrapper/contract regression 71 PASS; four source lint 0/0; owned diff check PASS.
- Every saved read uses reference.selectedControlledFileId and validates P15 tenant/Master/project/file/version. Lightweight source project/folder names replace source project detail supplementation; real canPreview is added without granting from name/reference/leader status. No latest-following semantics were introduced.
- Name-only rows remain visible with body disabled. Body rechecks pinned permission then uses exact formal viewer path. Trace performs its independent stronger getControlledFile read and identity check; denial stays visible, retains the saved row and never navigates. Successful read uses the existing readonly browser trace route with fixed selected ID, not management mode. Pending old context/unmount reads cannot open windows or navigate; revoked body permission and identity corruption are covered.
- Formal create succeeds before permission follow-up: failure feedback records already saved, closes completed chooser and requires real refresh before another batch. This is not a fabricated failure or success; stored write response and failed read are independently retained.
- Actual detail/preview renderer and authorization at runtime are not proved by the custom Vue/Axios/router host. Root must perform full types/build and real page E2E, including stronger detail denied versus permitted identities and fixed-reference history after later source revision/obsolete. UI-02/UI-03/UI-05 remain outside this UI-01 batch.

## G16 public checkin repair

ready_for_closeout for Root Review. Changed browser/index.vue plus owned checkin handler/render test and two checkin fixture scripts. Fixed MINOR-only action, working version explanation and controlled terminology; formal selected-WORKING detail entry and checkout permissions stay separate.

- Valid RED: initial 5 handler/render/wording failures; unsafe identity, missing history Master, stale-after-refresh, foreign upload-origin, wrong WORKING baseline and companion-PDF session failures have preserved logs. Final affected regression 24 PASS, selected-working entry 2 PASS, actual full SFC compile PASS, index lint 0/0 and diff check PASS.
- Exact selected safe Long and actual row Master/current checkout actor required. Source upload origin carries exact file/client/scoped/ticket, companion shares source session, and no placement fields enter request. Backend inheritance/checkout/ticket binding remains authoritative and was read-only reviewed.
- Late upload/write/list-read and late failures do not attach to, close or merge into a changed dialog. Success response verifies same Master/hyphen working version/formal baseline/checkout released. Already saved/read failure remains visibly already checked in; duplicate click writes once, genuine metadata-only checkin remains legal.
- Source fingerprint and full command boundaries retained in execution-log G16. No full types/build/backend execution/real E2E was run; Root must bind final runtime/Jar checks to this same source.

## G15 UI preparation evidence

ready_for_closeout for Root review of preparation only. Current-source 12-flow audit and bounded real-UI plan are in g15-current-public-flow-review.md / g15-ui-acceptance-plan.md. Old closed list relation/cross-project selector/usage/pinned-read/owner review gaps are not repeated as current.

- Runtime registry read confirms exact int_qms slot6/8067/48067; no start/restart/Docker/actual E2E. Shanghai/7-day reminder/each-minute activation are latest confirmed requirements, deployment remains Root verification.
- Task-owned dcc-public-ui-acceptance.e2e.cjs provides actual UI preflight, browse/association/usage cancel, upload-full-summary cancel and UI leave cleanup, reference-chooser cancel and reminder read. Env supplies real AGENTS login or prepared authorized roles and actual menu/object labels, no invented IDs/accounts/signatures or password output. No direct API/DB/evaluate/mock action bridge, network observations only method/path, trace starts after login.
- Node --check PASS and verify-ui-acceptance-preparation.cjs AST/dependency/12-flow/Cleanup Keep PASS in g15-preparation-structure.log. This static verification did not execute Playwright. Task scripts match an ignore rule; Root needs force-add only those intended retained scripts when committing. Reviews/plans/scripts have explicit Cleanup Keep.
- Script scope is intentionally bounded and fails on missing visible menu/config/data/roles; a PASS_SCOPED_UI_ONLY future run cannot fill whole goal matrix or prove migrations/Quartz/actual signatures/stamp/notification/int_qms merge. Full real UI steps and concrete remaining account/template/configuration/permission/preflight boundaries are documented for Root.

## G19 reviewer configuration guard

ready_for_closeout for Root Review. G15-R05 source mismatch is corrected; current userStore must explicitly have doc_control and update permission, initialized exact actor/tenant context. The shared helper and formal backend guard remain unchanged.

- Real permission/helper and actual SFC entry/handler RED 5 FAIL/2 PASS retained; final reviewer/configuration/project-confirm regression 32 PASS in g19-reviewer-role-regression.log. Actual SFC compile PASS, owned component lint 0/0 and diff check PASS.
- super_admin-only, missing update and uninitialized login yield hidden entry and zero read/write; actual enabled doc_control account sends exact Long/reason only after confirmation. Permission loss/restore, actor switch, dialog close/unmount, old configuration read, old save success/error and live tenant drift invalidate original generation; zero wrong-context writes/emits/close/unlock. Real directory error remains visible and partial data cannot save, correction/cancel preserves current legitimate inputs.
- Production only ProjectReviewerConfiguration.vue; new reviewer role-context test and existing reviewer fixture update. Fingerprints retained in G19 log. This is offline actual Vue/helper/API transport coverage, not fresh-login runtime/E2E. Root owns integrated types/build and actual server-role acceptance; overall task stays in_progress.

## G21 Git/task asset preparation

ready_for_closeout for Root Review of preparation. Deliveries: g21-git-candidate-manifest.json, g21-git-asset-review.md and reproducible readonly g21-build-candidate-manifest.py; all exact Cleanup Keep. No staging/commit/reset/delete/cleanup or other-task/production edits occurred.

- Per-tree repository-relative candidate/manual/excluded/force-add paths and raw/LF fingerprints/provenance are explicit. Six protected entries remain byte-identical and excluded; staged lists empty. Shared BPM/system/infra DCC necessities are separately justified, historical SQL digest/GxP policy remain conditional Review, unrelated old task media/raw logs/code copies are excluded.
- Two exact ignored .cjs scripts need Root force-add only after Review; existing permanent production/tests are not invisibly ignored in inspected scope. Intentional retired selector mapping is recorded, not missing; current imported paths exist. New 18 source paths require final Owner receipt, and two-tree HTML/shared confirmation differences require authoritative merge.
- Main primary/legitimate child/historical duplicate orchestration statuses and mode=preview versus actual cleanup receipts are distinguished. Legacy task history and excluded AGENTS task are untouched. Keep does not automatically authorize Git inclusion of raw logs.
- Structure/AST/relative path/exclusion/hash/ignore/no-staged validation and owned whitespace check PASS. No full validation/runtime/e2e/Git mutation was run; Root still must stop writers, rescan, approve exact candidate scope and complete business/migration/UI/merge evidence.

## G22 local-test GxP configuration preparation

ready_for_closeout for Root Review of prepared artifacts only. Shared task remains in_progress. No SQL/database connection or execution, production edits, E2E, services, Git mutations or global builds ran.

- Exact25 operation INSERT-only review SQL/complete field impact JSON and separate maximum1version approval-registration SQL are generated by g22_gxp_configuration.py using the actual formal parser and observed schema/core. Existing publish/old approval/other tenants/operations are untouched; replay0change and any existing identity/payload conflict blocks. Real quality inputs/write flags remain null/false, old approval reference cannot approve the new file.
- Valid RED: initial7missing-generator failures;2missing separate-registration function errors;1temporary-ownership/engine-guard failure;1actual report-byte/hash mismatch caused by Windows newline conversion;1offline version trailing-space identity collision missed. Final formal-parser/model/generated-byte/SQL-structure regression12PASS (g22-gxp-preparation-green.log). Actual report now hashes319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558 and is protected by the task-only .gitattributes. This is offline validation, not MySQL syntax/runtime/transaction proof.
- g22-gxp-local-test-configuration-plan.md contains source anchors, all25 scope, fixed raw policy/coverage hashes,1row versus25row versus existing19migration authorization boundaries, real person/role/account/ref/signature/time input template, fresh readonly schema/identity preflight, backup/stop-write and Root actual first/repeat/conflict/rollback verification plan. Transient routine DDL residue handling is explicit; no pre-drop or mysql--force workaround.
- No formal GxP audit policy-operation/version approval page/controller was found in inspected frontend/system sources. Existing signature-governance policy pane is a read of electronic-signature policy. Actual QA/quality approval and local-test database write scopes require user facts/confirmation; no assumption that admin/account1/dev is QA and no production CSV completion claim. QA non-self-approval and real signature/config-audit requirements stay visible rather than fabricating records.
- Ten exact durable G22 artifacts enter Cleanup Keep; raw logs remain local evidence only. Default reports archive effective RED/GREEN. Structural/AST/JSON/relative Keep/whitespace and actual policy/report hash checks PASS; no DB/network/process driver imports. G21 Git candidate manifest remains frozen; Root owns final asset rescan/approval and actual DB/UI verification.
