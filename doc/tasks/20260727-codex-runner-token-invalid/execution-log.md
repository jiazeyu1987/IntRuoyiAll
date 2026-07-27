# Execution Log

## User Intent

用户反馈：在测试管理列表点击“执行”时提示“Codex Runner token 无效或未配置”。

## Command Intent

- 检查前端执行按钮、Runner API wrapper、token 配置来源及后端校验。
- 建立先失败的回归测试，修复后运行目标测试和相关回归验证。
- 记录真实页面验证所需的 Runner、租户、账号和运行态前置条件。

## Milestone 1

- Status: completed
- Completed work: 已读取项目前端开发、任务收尾、Codex Runner 自动测试门禁和 bug regression fix loop 规则；确认前端执行入口调用 `/system/codex-test-execution/start`，后端按需启动 Runner 并用 `yudao.codex-test.runner.token` 校验注册。
- Verification evidence: `48081` 当前进程属于 `E:\IntRuoyi\IntRuoyiBackend`；`application-local.yaml` 使用 `CODEX_TEST_RUNNER_TOKEN`；当前 PowerShell/User/Machine 环境均未配置该变量；现有 Runner 注册日志持续报 token 无效。
- Root cause: 当前 `48081` 后端启动时未注入与受控 token 文件相同的 `yudao.codex-test.runner.token`，导致 Runner 注册 token 与后端校验值不一致。
- Remaining blockers: 需要重启当前 `int_main` 后端并重新注册本机 Runner。

## BDD Scenarios

BDD: 测试管理执行入口使用有效 Runner token -> Given 测试管理页面存在可执行测试项且 Runner token 已按当前配置注册；When 用户点击该测试项的“执行”；Then 前端请求应携带当前有效 token，后端接受注册身份并进入执行流程，不提示“Codex Runner token 无效或未配置”。

BDD: Runner token 确实缺失或失效时明确失败 -> Given Runner token 缺失或与后端注册状态不一致；When 用户点击“执行”；Then 页面应显示真实的 token 配置/校验错误，且不得伪造执行成功、改跑其他 Runner 或吞掉异常。

BDD: Codex CLI 失败摘要可回写终态 -> Given Runner 已使用有效 token 领取测试项但 Codex CLI 返回超过 512 字符的失败信息；When Runner 上报 `complete-case`；Then 执行项应进入 `BLOCKED` 终态，摘要必须符合 `progress_message` 长度契约，不得因数据库截断停留在 `RUNNING`。

BDD: 后端重启后测试管理初始化请求可用 -> Given `int_main` 前后端已启动且管理员登录有效；When 用户进入 `系统管理 > 测试管理`；Then 测试租户、Runner 状态、节点串选项和测试项列表请求应返回可用结果，页面不得连续显示通用“系统异常”。

BDD: 节点串执行必须从首节点开始 -> Given 测试项属于有序节点串；When 用户从测试管理页面发起执行；Then 选择范围必须从第 1 节点开始且连续，单独执行后续节点应 fail-fast，不得创建残缺执行批次。

BDD: Runner 取消后必须有界收敛 -> Given 服务端已取消执行且 Windows Codex wrapper 被强制终止后没有触发 Node child `close` 事件；When Runner 收到取消心跳并停止该进程树；Then 当前执行必须在独立的有界等待时间内退出，后续空闲心跳的 `currentRunningCount` 必须回到 `0`，不得遗留 `CLAIMED/RUNNING` 执行项、后代进程或 `codex-test-result-*` 文件。

## Milestone 6

- Status: completed
- Experience preflight: PASS; 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/frontend-development.md`、Playwright 与 bug regression fix loop 技能。
- Real path: Playwright 以 `芋道源码/admin` 登录 `http://127.0.0.1:8081`，在 `系统管理 > 测试管理` 选择目标租户 `测试租户`，按可见业务名称选择只读测试项 `批记录节点：归档追溯`，通过同行“执行”创建批次 `8`。
- Token verification: PASS; 批次从 `PENDING` 进入 `RUNNING` 并被 Runner 领取，页面未出现 `Codex Runner token 无效或未配置`。
- New regression: Runner 将 Codex CLI 的长失败信息传给 `complete-case`，后端写入 `system_codex_test_execution_case.progress_message varchar(512)` 时发生 `Data too long for column 'progress_message'`，批次未进入终态。
- Safety cleanup: Playwright 超时后通过正式取消接口取消批次 `8`；只读终态核验确认批次 `8=CANCELED`、活动执行项为空、Runner 在线且 `currentRunningCount=0`，无 `codex-test-result-8-*` 后代进程或临时结果文件，无 MES 写请求。
- Remaining work: 先用静态合同 RED/GREEN 限制 Runner 的 `complete-case` 摘要长度，再复跑同一只读真实路径。
- RED: Playwright 单独执行 `批记录节点：归档追溯` -> FAIL，业务码 `1002031009`，预期原因是该测试项为节点串第 6 节点，后端按正式契约拒绝非首节点开始的选择；未创建新批次。

## Milestone 7

- Status: completed
- Schema recovery: 已对本地 Docker MySQL 应用 `IntRuoyiBackend/sql/mysql/20260727_system_codex_test_node_chain.sql`，`system_codex_test_case.node_chain_name/node_chain_sort`、`system_codex_test_execution.node_chain_execution` 和 `idx_system_codex_test_case_tenant_node_chain` 均存在。
- Page diagnostic: `CODEX_TEST_MANAGEMENT_DIAGNOSTIC_ONLY=1 node doc/tasks/20260727-codex-runner-token-invalid/runner-cancel-settlement-real.e2e.cjs` -> PASS；Playwright 通过真实登录和侧边菜单进入 `系统管理 > 测试管理`，页面无“系统异常”，相关请求失败数 `0`、业务失败数 `0`。
- Real execution: Playwright 逐行选择完整 `批记录节点闭环`，创建批次 `11`、`12`；Runner 会话 `33` 领取首个执行项并上报 `current_running_count=1`。
- Lifecycle GREEN: 两批次均在 Codex 600 秒超时后进入终态 `FAIL`，执行项全部 `BLOCKED`；Runner stderr 记录 `codex exec child did not emit close after 5000ms`，但会话 `33` 随后持续 heartbeat 且 `current_running_count=0`。
- Cleanup verification: 数据库活动执行项数量 `0`；Runner PID `55972` 仅保留自身与 `conhost.exe`，无 Codex/cmd 后代；`%TEMP%` 中无批次 `11/12` 的 `codex-test-result-*` 文件。
- Business-flow limit: 批记录节点固定样本前置仍不完整，因此批次 `11/12` 不作为业务节点闭环 PASS；本任务验证范围是页面恢复、Runner 领取、终态回写和子进程收敛。

## TDD Evidence

- RED: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js` -> FAIL, expected because Runner had no independent child settlement timeout and awaited the Windows wrapper `close` event indefinitely after timeout/cancel.
- GREEN: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js` -> PASS after adding a 5000 ms child settlement bound and routing timeout/cancel stop requests through the same bounded Promise.
- GREEN: Real batches `11` and `12` -> Runner logged that child `close` was absent after 5000 ms, then session `33` returned to `current_running_count=0` instead of remaining stuck at `1`.
- REGRESSION: `node tests/e2e/system-codex-test-management-static.spec.js` and `node --check scripts/codex-test-runner.mjs` -> PASS.
- REGRESSION: `python -X utf8 -m pytest -q script/tests/test_codex_test_node_chain_migration.py script/tests/test_codex_test_management_migration.py` -> PASS, 4 tests.
- RED: `Invoke-RestMethod POST http://127.0.0.1:48081/admin-api/system/codex-test-runner/register` with the existing task-owned token source -> FAIL, business code `1002031011` and message `Codex Runner token 无效或未配置`; this reproduces the reported runtime configuration failure before alignment.
- GREEN: Restarted the confirmed `E:\IntRuoyi` backend on `48081` with `SPRING_APPLICATION_JSON` injecting the existing controlled Runner token, then started `IntRuoyiFronted/scripts/start-codex-test-runner.ps1` with the same token source; `POST /admin-api/system/codex-test-runner/register` -> PASS, business code `0`, `runnerSessionId=13`.
- GREEN: `node doc/tasks/20260727-codex-runner-token-invalid/runner-status-real-e2e.cjs` -> PASS, real frontend `http://127.0.0.1:8081` opened `系统管理 > 测试管理`; `/admin-api/system/codex-test-runner/status` returned `online=true`, `requiredCapabilitiesPresent=true`, heartbeat age `0`, and page did not show `Codex Runner token 无效或未配置`.
- REGRESSION: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS.
- REGRESSION: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest,CodexTestExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures.
- REGRESSION: Runner idle check after one heartbeat interval -> PASS, Runner PID `53624` remained alive and `codex-runner.stderr.log` stayed empty.
- EXPERIENCE: Updated `docs/e2e-rules.md` and `docs/experience-index.md` so future Codex Runner tasks route the exact `Codex Runner token 无效或未配置` symptom to the token-injection/registration-probe gate.

## Blockers

- 当前工作区存在与本任务无关的未跟踪目录 `doc/tasks/20260727-route-flow-tab-return-state/`，按规则保留不修改。
- Closeout commit/push blocked: `git status --short --branch --untracked-files=all` shows many non-task-owned modified/untracked source, test, doc, and task files. Do not stage this task together with unrelated concurrent work.
- Full business node-chain PASS blocked: required fixed batch-record parse sample is absent; no substitute fixture, mock, direct SQL, or API-only path was used.
- EXPERIENCE: `project-experience-consolidation` -> merged the reusable Windows child-settlement gate into `docs/e2e-rules.md` and added routing keywords to `docs/experience-index.md`; no new long-term document was created.
- CLEANUP PREVIEW: PASS; only the task-owned one-off Playwright script and its stdout/stderr logs are scheduled for deletion, with no blocked paths or warnings.
- CLEANUP APPLY: PASS; deleted `runner-cancel-settlement-real.e2e.cjs` and its stdout/stderr logs, while preserving `task.md`, `execution-log.md`, `verification-report.md`, `bug-regression-evidence.md`, and the active backend logs.
- CONCURRENT RUNTIME: At `2026-07-27 21:50:02`, unrelated batch `13` started through worktree Runner session `34` (`node-chain-slot-7-runner`). The task-owned Runner PID `55972` later began token-invalid registration retries and was stopped without modifying batch `13`, session `34`, or the worktree process.

## Milestone 5

- Status: ready_for_closeout
- Cleanup preview: PASS; initial preview had no blocked paths and identified only task-owned temporary artifacts for deletion.
- Cleanup apply: first attempt blocked with `PermissionError WinError 32` because the active `48081` backend process still held `backend-token-alignment.stderr.log`.
- Cleanup apply: PASS after marking the two active backend log files as `Cleanup Keep`; deleted `restart-backend-with-token.ps1`, `runner-status-real-e2e.cjs`, `runner-status-real-summary.json`, and `runner-status-real.png`.
- Final runtime verification: backend health `UP` on PID `45548`, frontend PID `41928`, Runner PID `53624`; correct Runner registration contract probe returned business code `0` and `runnerSessionId=14`.
- Probe correction note: an intermediate closeout probe used an array for `capabilities` and then omitted `tenant-id`; those probe mistakes produced `系统异常` / management-tenant validation errors, not a token failure.
- EXPERIENCE: Existing experience index already routes `task-closeout runtime log lock`, `PermissionError WinError 32`, and long-running process logs under task directories; no new long-term experience document is needed.
