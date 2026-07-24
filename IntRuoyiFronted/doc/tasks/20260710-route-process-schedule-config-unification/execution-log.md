# 执行日志：排产员工作台按路线工序统一展示

INFO: experience-index -> matched worktree, PowerShell, frontend style, smart scheduling statistics, and login/E2E gates.

GREEN: experience-preflight -> PASS, clean frontend worktree created at fb4d755eb on codex/20260710-route-process-schedule-config-unification.

BDD: route-process-row -> Given multiple products share one route process / When the table loads / Then one route-process row is rendered.

BDD: route-isolation -> Given one process appears in two routes / When one row is changed / Then only that route-process payload is submitted.

RED: route-process-static-contract -> FAIL, old page lacked route columns and still submitted processId.
GREEN: route-process-static-contract -> PASS, route columns, stable route-process key, and new request payload are present.
GREEN: task-typecheck-scope -> PASS, no vue-tsc errors in scheduler workbench or schedule-order API; full typecheck retains unrelated baseline errors.
GREEN: frontend-build -> PASS, pnpm build:prod completed.
GREEN: login-preflight -> PASS, official login entered the task worktree page on port 8091.
GREEN: route-process-workbench-real-e2e-available-scenarios -> PASS, 44 route-process rows loaded; current process code/name populated; one row aggregated 34 orders; night shift request used routeVersionId + routeProcessId and was restored; “球囊扩张导管” contained no “全检导丝”.
BLOCKER: route-process-workbench-real-e2e-cross-route -> 测试租户没有同一基础工序同时属于两条工艺路线的在排数据，无法完成该场景真实验证；静态契约和后端 BDD 已通过。
INFO: real E2E fails explicitly on the missing data prerequisite and records `result=BLOCKED`; it does not downgrade to matching process names or mock data.
GREEN: admin-login-preflight -> PASS, official login entered the task worktree scheduler workbench with tenant=芋道源码 and account=admin.
GREEN: admin-readonly-available-scenarios -> PASS, 26 route-process rows loaded through task backend 48131; route identifiers and names were populated; “棘突球囊扩张导管 / RX口检测” aggregated 5 orders; no MES POST/PUT/PATCH/DELETE request was observed.
BLOCKER: admin-readonly-cross-route-split -> 芋道源码当前在排工艺路线只有“棘突球囊扩张导管”，不存在同一基础工序同时属于两条在排工艺路线的真实数据，无法完成跨路线分行验证。
GREEN: admin-readonly-cross-route-retry -> PASS, 49 route-process rows loaded through task backend 48131; base process “RX口检测” appeared separately in “棘突球囊扩张导管” and “球囊扩张导管”; one route-process row aggregated 6 orders; no MES POST/PUT/PATCH/DELETE request was observed.
GREEN: frontend-implementation-commit -> PASS, implementation commit=fd8da756b.
GREEN: merge-latest-int-main -> PASS, merge commit=4aafa29e5 preserved the route-process workbench changes and latest mainline features.
RED: integrated-static-contract -> FAIL, latest mainline reintroduced nightShiftMixed display and filter semantics.
GREEN: integrated-static-contract -> PASS, obsolete mixed-night-shift semantics removed in commit=5dee7f102.
GREEN: integrated-production-build -> PASS, pnpm build:prod completed.
GREEN: integrated-admin-readonly-e2e -> PASS, 49 route-process rows loaded through backend 48131; “RX口检测” appeared separately in two active routes; one route-process row aggregated 6 orders; no MES writes were observed.
GREEN: integrated-runtime-cleanup -> PASS, verification services stopped and isolated Redis DB 15 cleared.
GREEN: latest-main-refresh -> PASS, later unrelated DCC and tab-cache commits were merged; route-process static contract remained green.
GREEN: final-fast-forward-merge -> PASS, frontend int_main advanced to 287f5530403a without overwriting unrelated dirty files.
GREEN: post-merge-main-verification -> PASS, the scheduler workbench route-process static contract passed in the main workspace.
BLOCKER: closeout-script-apply -> automatic apply correctly stopped because the main worktree contained unrelated dirty changes after the task was already fused.
GREEN: closeout-preview-scope -> PASS, preview kept task.md and execution-log.md and selected only task-specific artifacts plus frontend-feature-evidence.md for deletion.
GREEN: closeout-cleanup -> PASS, all preview-selected frontend artifacts and one-off evidence were removed.
GREEN: worktree-removal -> PASS, Git worktree registration was removed and the residual ignored node_modules/build directory was safely deleted from the task root.
GREEN: final-status -> PASS, task records marked completed and no task runtime, temporary credentials, or task worktree remains.
