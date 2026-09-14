# Execution Log

## User Intent

用户确认新的排产业务规则：已报工/历史任务只用于计算已完成量和剩余量；点击排产后，剩余未完成工序必须按当前最新工艺路线重新找工作站、产线和产能。老任务/老报工缺工作站，不应该出现 `受保护任务未绑定工作站` 阻断。

## Environment

- Worktree：`D:\IntRuoyiWorktree\replan-current-route-after-feedback`
- Branch：`codex/replan-current-route-after-feedback`
- Base：`origin/int_main`
- Runtime slot：`int_main slot=2`，frontend `8083`，backend `48083`
- Services：本任务暂不启动前后端服务。

## BDD / TDD

- BDD: replan remaining quantity by current route -> Given 某工单已有正式报工导致旧任务受保护但旧任务 `workstationId` 为空, When 用户手动重排该工单, Then 历史任务只扣减已完成/剩余量，不因缺旧工作站阻断；剩余量按当前工艺路线绑定的可用工作站、产线和产能生成新计划。

## Milestone Updates

- M1 completed：已读取 `docs\worktree-restrictions.md`、`docs\branch-runtime-ports.md`、`docs\powershell-memory.md`、`docs\task-closeout-rules.md`、`docs\backend-development.md`、`docs\powershell-encoding.md` 和 bug-regression-fix-loop skill。
- M1 completed：从 `origin/int_main` 创建隔离 worktree，避免混入主工作区大量未提交并行改动。
- M2 completed：新增 `replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation`，复现反馈保护任务缺旧工作站会被错误阻断。
- M3 completed：将 `FEEDBACK`/`FINISHED` 保护任务识别为进度事实，不再参与未来资源强制、旧工作站补水、产线可用性占用或工序日产能占用；剩余任务改按当前工艺路线资源选择。
- M4 completed：目标回归通过；完整类回归受基线测试夹具漂移阻断，详见 `verification-report.md`。
- M5 blocked for closeout apply：已同步更新第三方报工重排经验门禁；实现提交 `585d4526c9289288d4481abac948baf61f57aa6f` 已推送到 `origin/codex/replan-current-route-after-feedback`；cleanup preview 无删除项，但因主工作区 `E:\IntRuoyi` 脏改动阻断自动 merge/remove worktree。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，失败原因：旧逻辑返回 `受保护任务未绑定工作站`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityFromCurrentRouteWhenFeedbackTaskHasNoWorkstation+replanPreview_shouldNotReserveFeedbackProtectedRouteProcessCapacityWithoutLineKey" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`Tests run: 96, Failures: 0, Errors: 7, Skipped: 0`；错误集中在最新发布工艺路线配置夹具缺 `productionQuantityFactor` / `shiftHours`。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-replan-current-route-after-feedback\bug-regression-evidence.md` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`frontend 8083`，`backend 48083`。
- GREEN: `git push origin codex/replan-current-route-after-feedback` -> PASS，远端分支已创建。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-replan-current-route-after-feedback --mode preview` -> cleanup 无删除项，但主工作区 `E:\IntRuoyi` 脏改动阻断自动 ff-only merge/remove worktree。
- CHECK: `git diff --check` -> PASS。

## Blockers

- Full `MesProAutoScheduleServiceImplTest` 当前受基线夹具漂移阻断：最新工艺路线配置相关用例缺少生产数量系数/班次小时等测试夹具，单跑同类失败用例也复现，与本次保护任务资源语义变更无直接关系。
- Closeout apply 当前受主工作区 `E:\IntRuoyi` 脏改动阻断，不能安全自动合并并删除本 worktree。
