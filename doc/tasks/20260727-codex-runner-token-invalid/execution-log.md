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

## Milestone 6

- Status: in_progress
- Experience preflight: PASS; 已读取 `docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/frontend-development.md`、Playwright 与 bug regression fix loop 技能。
- Real path: Playwright 以 `芋道源码/admin` 登录 `http://127.0.0.1:8081`，在 `系统管理 > 测试管理` 选择目标租户 `测试租户`，按可见业务名称选择只读测试项 `批记录节点：归档追溯`，通过同行“执行”创建批次 `8`。
- Token verification: PASS; 批次从 `PENDING` 进入 `RUNNING` 并被 Runner 领取，页面未出现 `Codex Runner token 无效或未配置`。
- New regression: Runner 将 Codex CLI 的长失败信息传给 `complete-case`，后端写入 `system_codex_test_execution_case.progress_message varchar(512)` 时发生 `Data too long for column 'progress_message'`，批次未进入终态。
- Safety cleanup: Playwright 超时后通过正式取消接口取消批次 `8`；只读终态核验确认批次 `8=CANCELED`、活动执行项为空、Runner 在线且 `currentRunningCount=0`，无 `codex-test-result-8-*` 后代进程或临时结果文件，无 MES 写请求。
- Remaining work: 先用静态合同 RED/GREEN 限制 Runner 的 `complete-case` 摘要长度，再复跑同一只读真实路径。

## TDD Evidence

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

## Milestone 5

- Status: ready_for_closeout
- Cleanup preview: PASS; initial preview had no blocked paths and identified only task-owned temporary artifacts for deletion.
- Cleanup apply: first attempt blocked with `PermissionError WinError 32` because the active `48081` backend process still held `backend-token-alignment.stderr.log`.
- Cleanup apply: PASS after marking the two active backend log files as `Cleanup Keep`; deleted `restart-backend-with-token.ps1`, `runner-status-real-e2e.cjs`, `runner-status-real-summary.json`, and `runner-status-real.png`.
- Final runtime verification: backend health `UP` on PID `45548`, frontend PID `41928`, Runner PID `53624`; correct Runner registration contract probe returned business code `0` and `runnerSessionId=14`.
- Probe correction note: an intermediate closeout probe used an array for `capabilities` and then omitted `tenant-id`; those probe mistakes produced `系统异常` / management-tenant validation errors, not a token failure.
- EXPERIENCE: Existing experience index already routes `task-closeout runtime log lock`, `PermissionError WinError 32`, and long-running process logs under task directories; no new long-term experience document is needed.
