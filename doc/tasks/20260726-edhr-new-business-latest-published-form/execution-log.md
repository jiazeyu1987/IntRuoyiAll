# Execution Log

## User Intent

用户反馈：在个人工作台点击 eDHR 待办“进入/处理”时报“当前 eDHR 批次状态不允许该操作”，并明确要求新业务只能使用最新已发布的批记录表单。

## BDD

- `BDD: new eDHR business freezes latest published batch record form -> Given` 同一批记录定义存在多个已发布版本且工艺路线或历史绑定仍指向旧版本，`When` 创建新批次、返工或其它新业务，`Then` 系统只冻结最新已发布版本及其报表上下文。
- `BDD: historical eDHR business keeps frozen form version -> Given` 历史批次已冻结旧批记录版本，`When` 后续发布新版本并重新打开历史业务，`Then` 历史批次仍使用原冻结版本，不自动升级。
- `BDD: actionable workbench task opens normally -> Given` 个人工作台展示属于可处理批次的新业务待办，`When` 当前责任人点击“进入/处理”，`Then` 正式打开填写页面且不提示“当前 eDHR 批次状态不允许该操作”。
- `BDD: terminal batch remains blocked and hidden -> Given` 批次已关闭、归档、驳回或作废，`When` 查询个人待办或尝试打开任务，`Then` 待办不进入可处理列表且打开接口继续 fail-fast。

## Milestone Updates

- in_progress: 已创建任务记录并读取匹配经验门禁，开始定位新业务版本选择与个人工作台打开链路。
- completed: 检测到 `E:\IntRuoyi` 主工作区存在并行任务改动且 `int_main` 领先远端，已按 worktree 规则创建 `D:\IntRuoyiWorktree\edhr-latest-published-form`，分支 `codex/edhr-latest-published-form`，登记 `int_main slot=7`（8088/48088，仅登记，未启动服务）。
- completed: 截图中的 `E2E-REL-reject-seed-*` 任务属于状态 `60/VOIDED` 的历史批次；`openTask` 返回“当前 eDHR 批次状态不允许该操作”是正确终态保护，根因是个人工作台仍展示终态批次残留 `TODO`。
- completed: 当前 HEAD 已包含提交 `bd08562f fix: filter terminal edhr personal todos`；该提交在个人待办、个人统计、审批中心 TODO 与候选签名 TODO 查询源头排除关闭、归档、驳回、作废批次，并保留历史已完成列表。
- completed: 新业务表单选择已由 `MesProEdhrBatchExecutionServiceImpl#resolveLatestApprovedRouteFormBinding` 统一调用 `MesProBatchRecordVersionMapper#selectLatestApprovedByDefinitionId`；历史任务继续读取冻结的 `batchRecordVersionId`，不执行运行时迁移。
- completed: 只读数据库证据显示批记录定义 `47` 当前最新已发布版本为 `130/V14.0/APPROVED`，旧版本 `118/V13.0`、`98/V12.0`、`79/V4.0` 均为 `OBSOLETE`；截图时期终态批次使用 `98` 或更早版本，2026-07-25 创建的既有批次冻结当时最新的 `118/V13.0`，新业务当前应解析为 `130/V14.0`。
- completed: `mvn.cmd -pl yudao-server -am -DskipTests package -> BUILD SUCCESS`，从隔离 worktree 生成当前后端制品。
- completed: 构建期间并发“重启前后端服务”任务占用 `48081`；经任务交接确认其不再操作该端口后，停止已确认归属 `E:\IntRuoyi` 的旧 PID `53292`，复制隔离制品并启动新 PID `14740`。
- completed: 隔离制品与 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` SHA256 均为 `48324A7C340C025B84D3CD78C59D6BD10B4C6BC02F7C74EDE79A5F94161A8F85`；`http://127.0.0.1:48081/actuator/health -> {"status":"UP"}`。
- completed: `node scripts/preflight/login-preflight.mjs` 使用批准的本机默认身份来源登录 `http://127.0.0.1:8081/user/profile`，结果 `PASS: tenant=芋道源码 username=admin target=/user/profile`，证明真实前端和新后端联通。
- completed: 实际缺陷提交对应的 `zhangkeying` 真实 Playwright 证据已存在：目标终态任务不在 `my-page` 响应和页面正文中，且未出现终态状态 toast；本任务再次运行 `validate_bug_regression.py`，结果 `Bug regression evidence is valid`。
- completed: project-experience-consolidation 检查确认 `docs/e2e-rules.md#eDHR 终态批次个人待办门禁` 已完整覆盖本次可复用经验，无需重复修改长期经验文档。
- ready_for_closeout: 实现、回归、构建、运行态和可用真实前端验证均完成，进入 cleanup preview/apply、提交、推送和合并收尾。
- completed: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1 -> PASS`，当前 worktree 解析为 `int_main slot=7`、前端 `8088`、后端 `48088`。
- completed: 本任务记录提交为 `89d5107b docs: verify edhr latest published form handling`，仅包含当前任务 `task.md`、`execution-log.md` 和 `verification-report.md`；已推送到 `origin/codex/edhr-latest-published-form`，推送后分支不再 ahead。
- blocked: `task_closeout.py --task-id 20260726-edhr-new-business-latest-published-form --mode preview` 正确保留三份核心记录、无删除项，但提示当前分支不能 fast-forward 到 `int_main`，且主工作区 `E:\IntRuoyi` 存在并发任务脏改动；未执行 apply、merge 或 worktree 删除。

## TDD Evidence

- `RED: prior fix evidence for MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches -> FAIL, expected page total 1 but was 2`。
- `GREEN: prior fix evidence for MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches -> PASS, Tests run: 1, Failures: 0, Errors: 0`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_excludesTodoTasksFromTerminalBatches,MesProEdhrBatchExecutionServiceTest#openOrCreate_resolvesLatestApprovedRouteBindingReportAndShowsCurrentFillersToReadonlyViewer+openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask+getDetail_showsLatestCurrentFillersForExistingOldVersionRouteTaskWithoutMigratingTask+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 5, Failures: 0, Errors: 0`。
- 本任务未新增生产代码：两项正式行为均已存在于当前 HEAD，未添加重复分支、fallback 或兼容逻辑。

## Runtime And Frontend Evidence

- `mvn.cmd -pl yudao-server -am -DskipTests package -> PASS, BUILD SUCCESS`。
- `source SHA256 == target SHA256 == 48324A7C340C025B84D3CD78C59D6BD10B4C6BC02F7C74EDE79A5F94161A8F85`。
- `48081` 监听 PID `14740`，命令行使用 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --spring.profiles.active=local --server.port=48081 --yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend`。
- `http://127.0.0.1:48081/actuator/health -> {"status":"UP"}`。
- `http://127.0.0.1:8081/ -> HTTP 200`。
- `node scripts/preflight/login-preflight.mjs ... --target-path /user/profile --target-text 个人工作台 -> PASS`，密码仅从本机 `.env` 读取，未写入命令日志或任务文档。

## Verification Boundary

- 当前批准的本机来源不包含 `zhangkeying` 密码，因此本轮未重复登录该账号，也未猜测、重置密码或伪造 token。
- 精确责任人路径已有提交 `bd08562f` 对应的真实 Playwright 证据；本轮加载的后端源码包含该提交，并通过同一回归测试与运行态哈希校验。

## Blockers

- 无实现或运行态 blocker。

## Closeout Boundary

- 当前状态保持 `ready_for_closeout`，等待主工作区并发任务完成并恢复 clean 后再执行 cleanup apply、ff-only merge 和 worktree removal。
- 不删除 `.git/index.lock`，不覆盖主工作区 staged/unstaged 文件，不重启当前 PID `14740`。
