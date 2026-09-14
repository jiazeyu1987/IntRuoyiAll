# Execution log

## 2026-09-14 task start
- User intent: 在当前分支逐个修复20项DCC问题；完整范围见 task.md。
- Branch verified: int_qms, HEAD 2f7acecdbd4761717b4e063adca63a720b7a824f, tracking origin/int_qms, no ahead reported.
- Existing dirty state: .gitignore; .runtime/; IntRuoyiFronted/scripts/route-view-source-integrity.test.mjs; IntRuoyiFronted/src/views/dcc/controlled-file/logs/; doc/tasks/20260914-fix-dcc-logs-import/.
- Command intent: 读取规则、核对分支、识别基线与构建测试工具；未运行测试或修改生产代码。
- Baseline blocker: two existing .runtime files exceed 100 MB; requesting explicit exclusion of local tools from baseline. Files remain untouched.
- Previous DCC logs task reports ready_for_closeout. Its source restoration is unrelated and will be preserved.
- Validation: task requirements recorded for all 20 items. No item is claimed fixed or verified.
- User approved excluding .runtime/ from Git while retaining all files. Added root ignore rule; remaining existing changes will be committed separately from task implementation.
- Experience path D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md is absent; no style changes planned before resolving that gate. Current project frontend/backend rules are available.
- Baseline commit: b0f8119e5. Files: .gitignore; IntRuoyiFronted/scripts/route-view-source-integrity.test.mjs; IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue; previous task task.md, execution-log.md, verification-report.md. Branch runtime port guard passed 8061/48061; staged diff whitespace passed.
- BDD: DCC-MAIN-07/08/17 ordinary approval operations removed -> Given ordinary DCC old return/transfer/before-sign/after-sign requests; When submitted, including concurrently; Then reject with the business action-not-allowed error before any mapper, signature or BPM operation. Historical records remain untouched. External review is a separate flow outside the ordinary-file removal scope.
- Test authoring correction: initial compile failed because this branch uses string sign types; corrected test inputs to before/after. This compile error is not counted as RED evidence.
- RED: Maven -pl yudao-module-dcc -am -Dtest=DccOrdinaryApprovalRemovedActionsTest -Dsurefire.failIfNoSpecifiedTests=false test -> 4 executed, 3 failures + 1 concurrent execution error. Removed ordinary actions reached dependencies instead of returning the business rejection. Ancestor modules have no matching test, hence failIfNoSpecifiedTests=false; DCC test count explicitly checked.
- Implemented immediate business rejection at the three ordinary-file service entry points. External-review-specific methods preserved. Generic BPM controller already invokes external-signature guard; form-center-owned exception remains under review.
- GREEN: DccOrdinaryApprovalRemovedActionsTest -> 4 tests passed, no skipped tests.
- RED/GREEN frontend: node --test scripts/dcc-ordinary-removed-actions.test.mjs -> initially ordinary return rendered; after restricting controls to external review, PASS. Added handler-time guards for dialogs opened before a context change. This is visibility-contract evidence, not real E2E.
- RED: BpmTaskExternalSignatureGuardTest -> 7 tests, 1 failure: form-center ownership allowed a generic DCC route mutation. Narrowed mutation guard to reject DCC regardless of form-center ownership; normal approve/reject retains its existing formal signing route.
- REGRESSION: 127 backend tests passed (BPM guard 7; workflow 100; external review 6; approval adapter 10; removed actions 4). Four old ordinary mutation success tests updated to assert rejection/no writes per new requirement; external review tests retained. Frontend visibility tests 2 passed, including generic BPM DCC controls. Real E2E remains pending.
- BDD: DCC-MAIN-13 directory hierarchy -> Given A contains B; When assigning A's parent to A/B; Then fail without updating. Given corrupt cyclic ancestors; When listing child directories; Then fail explicitly within bounded queries. Concurrent topology edits must serialize current hierarchy reads before parent validation.
- RED: DccDirectoryCycleTest -> 3 failures after completing read-path fixture: self/descendant edits accepted; corrupted path exceeded 10-query safety budget. No production data was used.
- Implemented tenant-scoped topology locking via MyBatis FOR UPDATE before parent validation; cycle detection for tree/path/ancestor/subtree traversal. New specific cycle error. No schema modification.
- GREEN: DccDirectoryCycleTest 3 passed. Directory regression found an existing fixture missing assignment_file.master_id although the mapper joins by master_id; added 9100L matching the fixture's actual file master, preserving all authorization assertions.
- RED frontend: dcc-directory-cycle.test.mjs 2 failures: self/descendant selections invoked update API. Added pre-submit hierarchy validation including stale/missing parent and duplicate nodes; no style changes.
- GREEN directory: backend DccDirectoryCycleTest 3 + DccDirectoryAdminServiceImplTest 19 passed; frontend pre-submit 2 passed. Concurrent database topology race test and full real E2E still pending.
- BDD: DCC-MAIN-18 -> Given overlapping directory/page/tenant requests; When old success/failure returns after context changes; Then no old list, permission, total, error or loading state overwrites current context. Current-context errors must remain explicit.
- RED/GREEN DCC-MAIN-18: first 6 list behavior tests failed, then passed with request sequence and directory/page/tenant/user context check. Two additional metadata/cache races failed and passed with context checks. A route-sync overlap test exposed early guard release; replaced boolean-only lifetime with outstanding-navigation count. Superseded failures are logged; current errors stay visible and reject.
- Frontend grouped behavior tests: 13 passed. Full vue-tsc exited with Node default ~4 GB heap exhaustion; not treated as a type-check pass. Will rerun using appropriate memory after confirming machine capacity.
- Current-branch scope discovery: int_qms does not contain the int_main checkout/checkin or project-file-template services. Items 03/04/05/09/10/20 require formal implementation on this branch, not transplantation of conclusions from int_main. Product-code binding also differs; inspect existing product resolver before applying item 15.

## Merge latest int_main into int_qms
- User explicitly requested merging latest int_main. Preserve the entire 20-item goal and all partial remediation before integration.
- Read worktree-restrictions, branch-runtime-ports, powershell-memory and task-closeout rules before merge. No worktree creation, runtime restart or database mutation is involved.
- Pre-merge partial fixes: ordinary mutation guards; directory cycle checks; frontend response ownership guards. These are not the completed 20-item implementation. Save as a separate integration baseline commit under the authorized dirty-baseline rule.
- Verification before preservation: 127 approval/backend tests, 22 directory tests, 13 frontend behavior tests passed. Full type check exceeded default heap; 8GB rerun session was interrupted and completion output unavailable. Do not claim full build/type/E2E verification.
- Merge verification plan: inspect conflict resolution; ensure incoming latest main commit is ancestor; verify branch runtime guard 8061/48061; rerun changed behavior tests and affected backend compilation/tests; confirm checkout/checkin and project templates present; push and compare origin/int_qms.
