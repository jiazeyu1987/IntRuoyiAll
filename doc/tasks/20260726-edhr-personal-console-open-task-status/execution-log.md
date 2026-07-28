# Execution Log

## User Intent

用户反馈：当前填写人是 `zhangkeying`，使用 `zhangkeying` 账号从个人控制台点击 eDHR 待办 `进入处理` 时，页面提示“当前 eDHR 批次状态不允许该操作”。期望当前填写人能够通过个人控制台继续处理当前可填写任务。

## BDD

- `BDD: current filler opens eDHR task from personal console -> Given` 当前登录用户是 eDHR 工作任务填写人 `zhangkeying`，个人控制台存在待处理 eDHR 工作任务，`When` 点击 `进入处理`，`Then` 系统应打开正式填写处理页面，不显示“当前 eDHR 批次状态不允许该操作”。
- `BDD: closed eDHR batch remains blocked -> Given` eDHR 批次处于关闭、归档或作废等不可处理状态，`When` 用户尝试进入处理，`Then` 后端应继续 fail-fast 返回明确状态错误。
- `BDD: terminal eDHR batch is not actionable in personal console -> Given` eDHR 工作任务仍是 `TODO` 但所属批次已关闭、归档、驳回或作废，`When` 当前责任人打开个人控制台待办和统计，`Then` 系统不应把该任务显示或计入可处理待办。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位个人控制台打开 eDHR 待办的状态校验链路。
- completed: 后端修复已允许当前填写人在批次关闭前重新打开已提交动态路线表单，同时保持关闭、归档、驳回、作废批次 fail-fast。
- in_progress: 使用用户提供的本地账号凭据完成真实路径复现；个人控制台目标待办 `EDHRT-1784803798526` 关联批次状态为 `VOIDED(60)`，`openTask` 阻断正确，根因转为个人控制台列表/统计未过滤终态批次残留待办。
- completed: 在 `MesProEdhrWorkTaskMapper` 的个人待办、个人统计、审批中心 TODO 与候选签名 TODO 查询源头排除关闭、归档、驳回、作废批次；保留已完成历史列表和 `openTask` 终态批次阻断。
- completed: 隔离 worktree 构建 `yudao-server-exec.jar`，复制到本机 `48081` 后二次确认 source/target SHA256 一致并重启，健康检查 `UP`。
- completed: 真实 Playwright 路径使用 `芋道源码/zhangkeying` 登录个人控制台，目标作废批次任务不在 `my-page` 响应和页面正文中，且未出现“当前 eDHR 批次状态不允许该操作”。
- completed: project-experience-consolidation -> 合并新增 `docs/e2e-rules.md#eDHR 终态批次个人待办门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- completed: bug-regression-fix-loop 证据文件补齐 `RED:`、`GREEN:`、`Verification`、`Blockers` 标记并通过校验脚本。
- blocked: task-closeout-cleanup preview 已执行；当前分支不能安全快进合并到 `int_main`，且主工作区 `E:\IntRuoyi` 存在无关脏改，暂不执行 apply/merge/remove worktree。
- completed: 当前 task worktree 已登记 `int_main slot=3`（8084/48084，仅登记不启动服务），`scripts\preflight\branch-runtime-port-guard.ps1 -> PASS`。

## TDD Evidence

- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected page total 1 but was 2`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 1, Failures: 0, Errors: 0`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches+getApprovalCenterTodoPage_excludesPersonalFillTasksAndKeepsApprovalTasks,MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller+openTask_allowsApprovedOrdinaryFillCompletedBeforeReleaseForHistoricalFiller+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0`。

## Runtime And E2E Evidence

- `mvn.cmd -pl yudao-server -am -DskipTests package -> PASS`，隔离 worktree 构建 jar 成功。
- 本机 `48081` 加载 jar：source SHA256 与 target SHA256 均为 `1F251FC510467CA86C620E6F81FE55CE6F2D1522219700CFB0E5307C2C85D21A`，健康检查 `health=UP`。
- 只读 DB 核对：目标任务 `EDHRT-1784803798526` 仍为 `TODO`，关联批次 `900000000739` 状态为 `60/VOIDED`。
- 真实 E2E 结果：`RESULT {"loginCode":0,"responseCount":2,"myPageTotals":[0,0],"stats":[],"hasTargetInApi":false,"hasTargetInPage":false,"hasTerminalStatusToast":false,"url":"http://localhost:8081/user/profile"}`。

## Blockers

- `task_closeout.py --mode preview -> blocked`：当前分支不能安全快进合并到 `int_main`，主工作区 `E:\IntRuoyi` 存在无关脏改，且 cleanup 脚本将等待人工隔离/合并窗口后再执行 apply。
- 当前不记录、不提交任何密码；`zhangkeying` 登录凭据仅用于本次真实 E2E。
