# Execution Log: 修复审批中心已办页系统异常

## User Intent

- 用户截图显示进入“审批中心 > 已办”后，列表区域出现“系统异常”，且当前页面无审批任务数据。

## BDD

- BDD: 已办审批列表正常加载 -> Given 用户进入审批中心“已办”页 / When 前端以 `viewType=DONE` 请求统一审批任务分页 / Then 系统必须返回正式 DONE 视图结果或空态，不显示“系统异常”。

## TDD Evidence

- RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `APPROVAL_RESULT_UNSUPPORTED: BPM done task-done-legacy status=null`.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest#pageDoneKeepsLegacyHistoricTaskWhenTaskStatusIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmNativeApprovalTaskProviderTest,ApprovalCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests.
- RED: `node doc\tasks\20260804-approval-center-done-system-exception\approval-center-done-real.e2e.js` -> FAIL, DONE API returned `code=500,msg=系统异常`; backend log exposed legacy DCC historical DONE snapshot missing required display metadata.
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest#pageDoneKeepsLegacyHistoricalSnapshotWhenVersionNoOrCategoryIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL before DCC historical category handling, `APPROVAL_BUSINESS_CATEGORY_REQUIRED`.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest#pageDoneKeepsLegacyHistoricalSnapshotWhenVersionNoOrCategoryIsMissing" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS.
- REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalTaskAdapterTest,DccApprovalTaskTimelineAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 13 tests.
- E2E GREEN: `node doc\tasks\20260804-approval-center-done-system-exception\approval-center-done-real.e2e.js` -> PASS, exit code 0; DONE API `code=0`, `total=3222`, rendered 20 rows, no `系统异常`, no page errors, no console errors, no DONE target network failures, target write request count 0.

## Work Log

- 创建任务目录和初始任务文档。
- 已确认工作树已有大量未提交改动，且 `IntRuoyiFronted/src/views/approval-center/index.vue` 已有未提交修改；后续只做最小差异并避免覆盖无关改动。
- Root cause: `BpmNativeApprovalTaskProvider.toDoneSummary(...)` 直接把 `FlowableUtils.getTaskStatus(task)` 交给 `ApprovalTaskResultSupport.fromBpmTaskStatus(...)`；legacy historic DONE task 缺少 `TASK_STATUS` 时抛异常，导致 `/approval-center/tasks/page?viewType=DONE` 整页失败。
- Fix: 新增 `resolveDoneApprovalResult(...)`，仅当 `TASK_STATUS` 为空时返回空 `approvalResult`；非空未知状态仍沿用 `fromBpmTaskStatus(...)` fail-fast。
- Frontend contracts: `node tests/e2e/approval-center-done-standard-list-static.spec.js` -> PASS；`node tests/e2e/approval-center-done-result-remark-static.spec.js` -> PASS；`node tests/e2e/approval-center-chinese-copy-static.spec.js` -> PASS。
- Experience consolidation: 已新增 `docs/backend-development.md#统一审批中心 BPM 已办历史状态门禁`，并在 `docs/experience-index.md` 增加可检索关键词；`rg` 索引验证通过。
- Cleanup: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-done-system-exception --mode preview` -> PASS, keep task/core evidence, delete none, blocked none.
- Cleanup: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-done-system-exception --mode apply` -> PASS, deleted none.
- Git closeout blocker: after verification, commit `6f9ed0e83 chore: baseline existing workspace changes` appeared and includes this task's implementation files together with unrelated files. Current workspace also contains unrelated unmerged paths (`UU` / `AA`) outside this task scope, so this task cannot safely create a clean task-owned commit or push without rewriting or mixing unrelated work.
- Real E2E scope: added task-owned Playwright script `approval-center-done-real.e2e.js`, using the default local login identity from `IntRuoyiFronted\.env`, opening `/approval-center/done`, asserting the `viewType=DONE` API, UI row/empty-state, absence of `系统异常`, and zero target write requests.
- Runtime patch: generated `output\runtime\int_main\backend-runtime-control-20260804-approval-done-e2e-category.jar` from the prior task Jar and updated nested DCC/BPM module classes. SHA256: `500DCE99D0D3FFE45ADB7F7250F8C4D2527F0CE1812E7887026AB1390165DAB7`.
- Runtime patch verification: nested `yudao-module-dcc-2026.04-SNAPSHOT.jar` and `yudao-module-bpm-2026.04-SNAPSHOT.jar` both remained `compress_type=0`; patched class hashes matched local compiled classes (`DccApprovalTaskAdapter.class` `af2d49f1638beccb34af4ead7a272986ca79d3c4278d4daec21c7d02ac2ae91a`, `BpmNativeApprovalTaskProvider.class` `eb113d98c1d732cb2f27235953e686deab101d41e0647c5ed0e77e4162618136`).
- Runtime restart: stopped task-owned PID `49940` running `backend-runtime-control-20260804-approval-done-e2e.jar`; started PID `49968` with `backend-runtime-control-20260804-approval-done-e2e-category.jar`; `http://127.0.0.1:48081/actuator/health` -> `UP`; frontend `http://127.0.0.1:8081/` -> HTTP 200.
- E2E script correction: initial PASS assertions exposed a script-scoping issue where a tab-switch-aborted TODO request was counted as a DONE target failure. The script now asserts only `/approval-center/done` and `viewType=DONE` as target failures while recording non-DONE approval-center aborts separately.
- E2E artifact: `doc\tasks\20260804-approval-center-done-system-exception\e2e-artifacts\approval-center-done-real-result.json` records DONE `status=200`, `code=0`, `total=3222`, `listSize=20`, `targetWriteRequestCount=0`, `pageErrors=[]`, `consoleErrors=[]`, `targetNetworkFailures=[]`; screenshot saved to `e2e-artifacts\approval-center-done-real.png`.
- Experience consolidation: updated `docs\e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁` for multi-tab target scoping, added `docs\backend-development.md#统一审批中心 DCC 已办历史快照展示门禁`, and indexed both in `docs\experience-index.md`; `rg -n "TODO ERR_ABORTED|统一审批中心 DCC 已办历史快照展示门禁|APPROVAL_BUSINESS_CATEGORY_REQUIRED|DONE 目标链路" docs\e2e-rules.md docs\backend-development.md docs\experience-index.md` -> PASS.
- Cleanup after E2E: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-done-system-exception --mode preview` -> PASS, kept E2E script/result/screenshot, blocked none.
- Cleanup after E2E: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-approval-center-done-system-exception --mode apply` -> PASS, deleted only task-owned runtime patch nested jars and `start-approval-done-e2e-category.ps1`; blocked none.

## Blockers

- 当前分支 `int_main` 已领先 `origin/int_main`，且工作区存在与本任务无关的 unmerged paths；按项目 Git policy，最终提交/推送需要先处理既有冲突和已混入的 baseline commit 边界。
