# 执行日志：工艺路线工序排产配置全局统一

INFO: experience-index -> matched docs/powershell-memory.md, docs/worktree-memory.md, docs/agent-memory/project-error-prevention.md, docs/release-backup-restore.md, docs/login-access.md before E2E, and D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md.

GREEN: experience-preflight -> PASS, main workspaces contain unrelated dirty changes; paired clean worktrees were created from backend int_main@4b187d7b4e and frontend int_main@fb4d755eb on branch codex/20260710-route-process-schedule-config-unification.

INFO: runtime-contract -> backendPort=48121, frontendPort=8091, database=ruoyi-vue-pro-route-config-unification, protected file config id=28 unchanged.

BDD: same-route-process-multi-product -> Given multiple product orders share routeVersionId and routeProcessId / When the scheduler workbench loads / Then one row aggregates orders, demand, and feedback.

BDD: same-process-different-routes -> Given the same processId appears in two routes / When the scheduler workbench loads / Then two route-process rows are shown and updated independently.

BDD: canonical-route-config -> Given one generic route config and product-specific historical configs / When migration and settings save run / Then only the generic active config is used.

BDD: capacity-not-multiplied-by-orders -> Given multiple WIP orders share one route process / When statistics are calculated / Then shift capacity is the canonical route-process capacity once.

BDD: migration-fails-on-canonical-conflict -> Given zero or multiple active generic configs in a group / When migration runs / Then it fails before changing data.

BDD: current-route-process-definition -> Given an active WIP snapshot still references a historical base process while its current route process points to another base process / When the workbench loads / Then the current route process code and name are returned.

RED: route-process-grouping -> FAIL, 旧实现按 processId 合并并以 processId 保存。
RED: current-route-process-definition -> FAIL, workbench returned the historical snapshot process instead of the current route-process definition.
GREEN: current-route-process-definition -> PASS, the targeted regression test returns the current route-process code and name.
GREEN: targeted-backend -> PASS, 65 tests passed for schedule-order statistics/settings, canonical config save/sync, and route copy.
RED: route-copy-missing-config -> FAIL, copyRoute did not reject a route process without canonical config.
GREEN: route-copy-missing-config -> PASS, route copy now fails with 1040271013 and writes item_id=NULL only.
RED: migration-real-success-path -> FAIL, old product unique index blocked soft delete; retired config references remained.
GREEN: migration-contract -> PASS, 17 release/migration policy tests passed.
GREEN: migration-real-fail-fast -> PASS, missing generic group and missing WIP canonical config both failed before index changes.
GREEN: migration-real-success -> PASS, controlled isolated prerequisites produced activeProduct=0, invalidGroups=0, productRefs=0, activeWipMissingRouteVersion=0, activeWipMissingGeneric=0; invalid deleted-route history was retired and duplicate active config was rejected by the new unique index.
GREEN: full-server-package -> PASS, yudao-server reactor package succeeded.
BLOCKER: full-mes-test -> 1616 tests ran with 23 failures and 40 errors. Representative failures in scheduler permission, route enable, auto-schedule night shift, and feedback progress were reproduced in a clean baseline worktree at the task base commit.
GREEN: frontend-build -> PASS, pnpm build:prod succeeded.
GREEN: login-preflight -> PASS, 测试租户/aoteman entered http://127.0.0.1:8091/mes/pro/scheduler-workbench.
GREEN: real-playwright-available-scenarios -> PASS, 44 route-process rows loaded; current process codes/names were populated; one route-process aggregated 34 orders; night shift was switched and restored through the UI; “球囊扩张导管” contained no “全检导丝”.
BLOCKER: real-playwright-cross-route-split -> 测试租户当前在排数据没有同一基础工序同时属于两条工艺路线，无法完成跨路线分行的真实 E2E；后端 BDD 单测已通过。
GREEN: cleanup -> PASS, validation services stopped; isolated databases and users removed; runtime logs and temporary scripts removed; source database unchanged.

INFO: admin-final-verification-authorization -> 用户明确要求使用“芋道源码/admin”进行最终验证并在通过后融合；本阶段限定为只读页面验证，禁止向该租户发送 MES 写请求，登录口令不落盘。

GREEN: experience-preflight-admin-readonly -> PASS, target worktrees and ports are isolated; frontend=8091, backend=48121, MySQL=local Docker 127.0.0.2:23306/ruoyi-vue-pro, Redis=127.0.0.2:26379 isolated database index; tenant=芋道源码, account=admin; source database migration and MES writes are forbidden; backend startup will disable Quartz and all configured local automatic jobs; browser verification will fail on any MES POST/PUT/PATCH/DELETE request.

INFO: admin-runtime-ownership-collision -> initial backend port 48121 was taken over by another registered worktree runtime; no foreign process was stopped. This task moved to backend 48131, frontend 8091, Redis database 15, and revalidated process command lines plus health ownership before final E2E.
GREEN: admin-login-preflight -> PASS, official login preflight entered the scheduler workbench with tenant=芋道源码 and account=admin through frontend 8091 and task backend 48131.
GREEN: admin-readonly-available-scenarios -> PASS, 26 route-process rows loaded from task backend 48131; route identifiers and names were returned; “棘突球囊扩张导管 / RX口检测” aggregated 5 orders; browser observed no MES POST/PUT/PATCH/DELETE request.
BLOCKER: admin-readonly-cross-route-split -> 芋道源码当前在排工艺路线只有“棘突球囊扩张导管”，不存在同一基础工序同时属于两条在排工艺路线的真实前置数据，无法完成跨路线分行 Playwright 验证。
GREEN: admin-readonly-runtime-cleanup -> PASS, task backend 48131 and frontend 8091 stopped, isolated Redis database 15 cleared, temporary credential-free probe removed, and the unrelated runtime on port 48121 was not modified.

GREEN: admin-readonly-cross-route-retry -> PASS, retry loaded 49 route-process rows from task backend 48131; active routes were “棘突球囊扩张导管” and “球囊扩张导管”; base process “RX口检测” appeared as separate route-process rows in both routes; one “棘突球囊扩张导管 / RX口检测” row aggregated 6 orders; no MES POST/PUT/PATCH/DELETE request was observed.
INFO: user-merge-authorization -> 用户在已知完整 MES 基线既存失败的情况下要求管理员验证通过后融合；任务目标测试、迁移验证、构建和真实 E2E 已通过，开始提交与融合门禁。
GREEN: task-implementation-commits -> PASS, backend implementation commit=8032e79b62; frontend implementation commit=fd8da756b.
GREEN: merge-latest-int-main -> PASS, backend merge commit=be5612d47f after preserving both new H2 schema tables and request logs; frontend merge commit=4aafa29e5.
GREEN: integrated-backend-targeted -> PASS, 66 tests passed after merging the latest int_main.
GREEN: integrated-migration-contract -> PASS, release/migration contract tests passed after merging the latest int_main.
RED: integrated-frontend-static-contract -> FAIL, latest int_main reintroduced the obsolete nightShiftMixed display semantics into the merged workbench.
GREEN: integrated-frontend-static-contract -> PASS, removed the obsolete mixed-night-shift branch and filter option; fix commit=5dee7f102.
GREEN: integrated-frontend-build -> PASS, pnpm build:prod completed after the merge fix.
GREEN: integrated-full-server-package -> PASS, yudao-server reactor package completed after merging the latest int_main.
GREEN: integrated-admin-readonly-e2e -> PASS, task branches loaded 49 route-process rows from backend 48131; “RX口检测” remained split between “棘突球囊扩张导管” and “球囊扩张导管”; one “棘突球囊扩张导管 / RX口检测” row aggregated 6 orders; no MES writes were observed.
GREEN: integrated-runtime-cleanup -> PASS, task backend/frontend stopped, isolated Redis DB 15 cleared, temporary credential-bearing environment removed, and unrelated runtimes were not modified.
GREEN: latest-main-refresh -> PASS, later concurrent DCC and tab-cache commits were merged into the task branches; task static contract remained green.
GREEN: final-fast-forward-merge -> PASS, backend int_main advanced to 461a96660f84 and frontend int_main advanced to 287f5530403a without touching unrelated dirty files.
GREEN: post-merge-main-verification -> PASS, main backend ran 66 targeted MES tests plus migration contract tests; main frontend route-process static contract passed.
BLOCKER: closeout-script-apply -> automatic apply correctly stopped because both main worktrees contained unrelated dirty changes, although the task commits were already fused.
GREEN: closeout-preview-scope -> PASS, preview kept task.md, execution-log.md, and verification-report.md; it selected only three backend evidence files and frontend artifacts/evidence for deletion.
GREEN: closeout-cleanup -> PASS, preview-selected backend evidence files were deleted in cleanup commit 461a96660f84; frontend one-off evidence and Playwright/build artifacts were removed.
GREEN: worktree-removal -> PASS, backend and frontend Git worktree registrations were removed; the remaining ignored frontend node_modules/build tree was deleted from the verified task root without affecting other worktrees.
GREEN: final-status -> PASS, surviving task records marked completed; no task runtime, Redis data, temporary credentials, or task worktree remains.
