# 测试服第三方报工导入不生效原因排查执行日志

## User Intent

- 用户反馈：修改后本机可以，发布到测试服务器后第三方报工仍报不上，询问原因。
- 用户补充截图：测试服导入后弹框中多行进度增量为 `0%`，失败原因包含 `未找到与排产工序唯一匹配的未完成...`、`生产任务缺少工作站，且工序没有唯...`，怀疑存在多种原因。

## Rule And Skill Evidence

- Read `bug-regression-fix-loop` and `references/bug-contract.md`.
- Read `docs/server-access.md`, `docs/release-backup-restore.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`.
- Read `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md` and `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`.

## BDD

- `BDD: 测试服第三方直报应加载同一本机已验证修复 -> Given 本机导入李萍.xlsx 已产生正式报工并更新排产进度, When 用户把当前版本发布到测试服后在测试服执行同一路径导入, Then 测试服发布包必须包含修复提交且导入成功后正式报工列表/排产工单进度更新；若未更新，应能定位为发布包、运行态版本或正式数据条件问题。`

## Investigation Log

### Superseded Earlier Finding

- `git status --short --branch` shows `int_main...origin/int_main [ahead 2]` with many unrelated dirty files; current app HEAD is `b99246f58`.
- `git grep ... HEAD -- ThirdPartyFeedbackImportServiceImpl.java` did not find `DirectWorkstationResolution` or `resolveDirectFeedbackWorkstation`.
- `git grep ... origin/int_main -- ThirdPartyFeedbackImportServiceImpl.java` did not find `DirectWorkstationResolution` or `resolveDirectFeedbackWorkstation`.
- Working tree file `ThirdPartyFeedbackImportServiceImpl.java` does contain `resolveDirectFeedbackWorkstation` and `DirectWorkstationResolution`, proving the local runtime was built from uncommitted working-tree source.
- Test server frontend `http://172.30.30.58:8081/release-info.json` returns releaseTag `release-20260802-intmain-head-test-r260802b-r1`.
- Test server release-info sourceRepos backend/admin-frontend commit is `b99246f58ff7d556caee24307ec89b662d0427e3`, `dirty=false`.
- `git log --all --grep="persist direct feedback import progress"` found fix commit `b8533d59a fix: persist direct feedback import progress`.
- `git branch --all --contains b8533d59a` shows only `codex/third-party-feedback-import-20260802` and `origin/codex/third-party-feedback-import-20260802`.
- `git merge-base --is-ancestor b8533d59a b99246f58` -> not ancestor.
- `git merge-base --is-ancestor b8533d59a origin/int_main` -> not ancestor.
- Backend health on test server: `http://172.30.30.58:48081/actuator/health` -> `{"status":"UP"}`.
- Frontend HTTP on test server: `http://172.30.30.58:8081/` -> HTTP `200`.
- This earlier finding is no longer the current blocker because the test server has since been republished with a newer feedback fix release.

### Current Follow-up Investigation

- Test server current release-info: `release-20260802-feedback-fix-test-r260802h-r1`.
- Test server backend and frontend containers both run image tag `release-20260802-feedback-fix-test-r260802h-r1`.
- Test server release-info source commit: `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, `dirty=false`.
- Test server backend health remains `UP`; frontend HTTP remains reachable.
- Source code path confirmed: `importDirectWorkReportWorkbook` creates `MesProFeedbackDO`, submits formal feedback, then recalculates schedule progress. If a row is skipped before formal feedback creation, progress is not updated.
- `李萍.xlsx` exists locally and is parsed according to `parseLiPingDirectWorkReport`; due inherited task/order logic, it produces 69 processable rows, including some `组装` / `组件` / miscellaneous timing rows.
- Test DB `mes_pro_feedback_import_record` has no records created on `2026-08-02`; latest `李萍.xlsx` audit records remain at `2026-08-01 00:22:30`, all with `feedback_id=0`.
- Test DB `mes_pro_feedback` has no target formal feedback rows created on `2026-08-02` for schedule orders `127`, `129`, `131`.
- Schedule order progress evidence: `881MO093613` remains `3518 / (3518 + 22482) = 13.530769%`; `881MO093615` remains `4278 / (4278 + 21722) = 16.453846%`; `881MO093617` remains `130 / (130 + 25870) = 0.500000%`.
- Excel normalized work orders: 14 unique codes. Test server has only 5 corresponding non-deleted `mes_pro_work_order` rows: `881MO093613`, `881MO093615`, `881MO093616`, `881MO093617`, `881MO098538`.
- User and approver check: 20 Excel feedback user codes are missing from `system_users.username`; approver `李萍` and `李萍临时工` each match one user, but `李萍（临时工）` matches none.
- Workstation check: screenshot-relevant process codes such as `Z2530`, `Z2560`, `Z2570`, `Z2580`, `Z2600`, `Z2630`, `Z2773`, `Z2774`, `Z2776`, `Z2972`, `Z2973`, `Z2974`, `Z2975`, `Z3850`, `Z5200` have `process_ws_count=0` in `mes_md_workstation`.
- Classification using backend execution order on the 69 parsed rows:
  - `ACTIVE_TASK_NOT_FOUND`: 28 rows.
  - `WORK_ORDER_NOT_FOUND`: 19 rows.
  - `PROCESS_NOT_FOUND`: 17 rows.
  - `WORKSTATION_NOT_FOUND`: 3 rows.
  - `FEEDBACK_USER_NOT_FOUND`: 2 rows.
- Examples:
  - Row 3 `881MO093613-1-11 / Z2570` has four active tasks `PT-3782`, `PT-3783`, `PT-3784`, `PT-3785`; Excel task code does not match any `PT-*`, so task is not unique.
  - Row 7 `881MO093613-1-22 / Z3850` maps to unique task `PT-3802`, but task workstation is null and process workstation count is 0.
  - Row 12 `MO000094794-19 / Z2480` has no matching system work order.
  - Row 6 inherited `881MO093617-1-11 / 组装 / OOE000035` has no matching schedule process.
  - Row 61 `881MO093613-1-23 / Z2560` maps to unique task `PT-3803`, but feedback user `A2020130` is missing.

## Root Cause

- Current root cause is not missing deployment. The test server is already on the feedback fix release.
- Current root cause is that every parsed row is blocked before formal feedback creation by one or more real business prerequisites: missing system work order, non-schedule summary rows, ambiguous split active tasks, missing user account, and missing workstation assignment/configuration.
- Progress remains 0% because the fixed implementation deliberately calculates progress only from formal feedback rows in `mes_pro_feedback`; skipped rows do not update schedule progress.

## Next Required Work

- Decide whether to repair test data, import mapping rules, or both.
- Data repair path: create/assign valid workstations for relevant tasks/processes, create missing users, and ensure imported production orders have effective schedule orders and enabled schedule processes.
- Import rule path: define formal mapping from Excel task code `881MO...-1-序号` to system task `PT-*`, and define whether `组装` / `组件` / miscellaneous timing rows should be ignored or mapped.
- Split-task path: define a business allocation rule for rows where one schedule process has multiple active `PT-*` tasks; the agent must not guess this allocation under no-fallback policy.
- Do not modify test server business data until the user approves the exact repair scope and rollback/verification plan.

## Data Repair Execution

- User authorized test server data repair with: `补测试服数据/工作站/用户`.
- Inserted 21 missing feedback/approver users into `system_users` for tenant `1`, marked with `remark = 'CODX_TPFB_20260802'`.
- Inserted 18 test-only workstations into `mes_md_workstation` for tenant `1`, marked with `remark = 'CODX_TPFB_20260802'` and code prefix `TPFB-WS-20260802-`.
- Did not update or delete existing work orders, schedule orders, tasks, task statuses, task codes, feedback rows, or split-task assignments.
- `GREEN: post-repair preflight SQL -> PASS, post-repair classification changed from WOULD_SUBMIT 0 to WOULD_SUBMIT 5 while ACTIVE_TASK_NOT_FOUND / WORK_ORDER_NOT_FOUND / PROCESS_NOT_FOUND remain formal blockers for other rows.`

## Verification After Repair

- `GREEN: real browser import -> PASS, 芋道源码租户管理员从报工页签点击第三方导入并上传 C:\Users\BJB110\Desktop\文档\李萍.xlsx，接口返回 importedCount=5、pendingCount=0、submittedCount=5、skippedRows=65，生成 FB-000157 至 FB-000161。`
- Existing local E2E script `tests/e2e/mes-direct-work-report-import-real-flow.e2e.js` reached the real import response but failed on an obsolete assertion that still expected `submittedCount === 0`; this is a test expectation issue, not a server import failure.
- `GREEN: DB formal feedback verification -> PASS, mes_pro_feedback ids 157-161 exist for import records 220-224 and tasks PT-3802/PT-3803/PT-3954/PT-3912.`
- `GREEN: DB schedule verification -> PASS, schedule order 131 / 881MO093613 progress_percent = 1.965385 and schedule order 127 / 881MO093615 progress_percent = 0.965385; related process rows show Z3850=204, Z2560=307, Z2550=132, Z3810=119 reported quantity.`
- `GREEN: UI visibility verification -> PASS, 报工列表页面显示 5 条本次导入明细；排产工单页面 `/mes/pro/schedule-order` 显示 881MO093613 进度 1.97%、881MO093615 进度 0.97%。`
- Remaining skipped rows are expected under current formal rules: missing work order, missing schedule process, or active task not uniquely resolvable. No fallback allocation was introduced.

## Experience Consolidation

- Updated existing long-term rule `docs/e2e-rules.md` with a user-column-configuration/list-visibility gate: when a configurable table hides identifier columns, real E2E should assert visible business-field combinations and pair that with DB/API binding evidence instead of treating hidden IDs as product failure.

## Retest 2026-08-02 17:24

- User requested another test-server import retest.
- Pre-retest baseline: max `mes_pro_feedback.id = 161`, max `mes_pro_feedback_import_record.id = 224`; schedule progress was `881MO093613 = 1.965385`, `881MO093615 = 0.965385`, `881MO093617 = 0.500000`.
- `GREEN: real browser import retest -> PASS, 芋道源码租户管理员从报工页签点击第三方导入并上传 C:\Users\BJB110\Desktop\文档\李萍.xlsx，接口返回 importedCount=5、submittedCount=5、pendingCount=0、skippedRows=65，生成 FB-000162 至 FB-000166，导入记录 225 至 229。`
- `GREEN: DB formal feedback retest -> PASS, mes_pro_feedback ids 162-166 exist and are bound to import records 225-229; status=2; tasks are PT-3802, PT-3803, PT-3954, PT-3803, PT-3912.`
- `GREEN: DB schedule retest -> PASS, schedule order 131 / 881MO093613 progress_percent = 3.930769 and schedule order 127 / 881MO093615 progress_percent = 1.930769.`
- `GREEN: UI retest -> PASS, 报工列表显示本次导入明细字段，排产工单页面 `/mes/pro/schedule-order` 显示 881MO093613 进度 3.93%、881MO093615 进度 1.93%。`
- Remaining `skippedRows=65` are still expected formal blockers: `WORK_ORDER_NOT_FOUND=19`, `ACTIVE_TASK_NOT_FOUND=28`, `PROCESS_NOT_FOUND=17`; no `WORKSTATION_NOT_FOUND` or `FEEDBACK_USER_NOT_FOUND` remains in this retest.
