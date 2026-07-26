# Execution Log

## User Intent

用户反馈：当前填写人是 `zhangkeying`，使用 `zhangkeying` 账号从个人控制台点击 eDHR 待办 `进入处理` 时，页面提示“当前 eDHR 批次状态不允许该操作”。期望当前填写人能够通过个人控制台继续处理当前可填写任务。

## BDD

- `BDD: current filler opens eDHR task from personal console -> Given` 当前登录用户是 eDHR 工作任务填写人 `zhangkeying`，个人控制台存在待处理 eDHR 工作任务，`When` 点击 `进入处理`，`Then` 系统应打开正式填写处理页面，不显示“当前 eDHR 批次状态不允许该操作”。
- `BDD: closed eDHR batch remains blocked -> Given` eDHR 批次处于关闭、归档或作废等不可处理状态，`When` 用户尝试进入处理，`Then` 后端应继续 fail-fast 返回明确状态错误。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位个人控制台打开 eDHR 待办的状态校验链路。
- completed: 定位个人控制台 `进入处理` 通过统一 eDHR 工作任务导航调用后端 `POST /task/open`；动态路线表单已提交后任务状态为 `TASK_STATUS_APPROVED` 且无传统 `executionId`，旧逻辑只允许传统 execution 表单关闭前再打开，因此抛出“当前 eDHR 批次状态不允许该操作”。
- completed: 新增后端回归用例 `openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller`，覆盖当前填写人通过 `workTaskId` 打开已提交动态路线表单。
- completed: 后端最小修复新增动态路线表单关闭前打开分支，要求任务具备完整 Form Center 上下文，且传入 `workTaskId` 必须属于同一批次任务的填写/返工任务，状态限定为 `TODO/DOING/OVERDUE/DONE`；关闭、归档、驳回、作废批次仍在前置状态校验中阻断。
- in_progress: 已用隔离 worktree 构建修复后的 `yudao-server-exec.jar` 并加载到本机 `48081`，后端 health 为 `UP`。

## TDD Evidence

- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected ServiceException "当前 eDHR 批次状态不允许该操作"`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 1, Failures: 0, Errors: 0`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller+openTask_allowsApprovedOrdinaryFillCompletedBeforeReleaseForHistoricalFiller+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 3, Failures: 0, Errors: 0`。

## Runtime Evidence

- Created clean worktree: `D:\IntRuoyiWorktree\edhr-personal-console-open-task-status`, branch `codex/edhr-personal-console-open-task-status`, source HEAD includes the fix.
- Built jar from clean worktree: `mvn.cmd -pl yudao-server -am -DskipTests package -> PASS`.
- Loaded jar SHA256 to local backend target: `3C774DC257F8E07F4AC6C3CD7BFAD0065E59A1094C1E0FA0969743435FD948AE`.
- Restarted local backend on `48081`; new PID `51896`; health check `http://127.0.0.1:48081/actuator/health -> {"status":"UP"}`.

## Blockers

- BLOCKED: 真实 Playwright E2E 需要 `zhangkeying` 可用登录凭据。只读数据库核对显示本地账号存在：`芋道源码/zhangkeying`、`测试租户/zhangkeying`；使用本地默认密码来源登录均返回账号密码错误。未获得用户明确授权前，不重置账号密码、不伪造 token、不用 admin-only 或 API-only 替代当前填写人真实前端路径。
