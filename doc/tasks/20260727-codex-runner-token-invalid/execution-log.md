# Execution Log

## User Intent

用户反馈：在测试管理列表点击“执行”时提示“Codex Runner token 无效或未配置”。

2026-07-27 用户追加要求：在测试管理页签中完成一次真实 E2E 运行测试，并等待真实 Runner 执行结果。

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

BDD: 标准本地后端重启保持 Runner token -> Given 工作区已有受控 Runner token 文件或首次启动尚未生成；When 通过 `restart-int-ruoyi-local.ps1 -Component backend` 重启 `int_main` 后端；Then 脚本必须使用工作区唯一 token 文件，在缺失时安全生成一次、文件为空时 fail-fast，并在启动 Java 前把同一 token 注入后端环境，后续页面按需 Runner 注册不得再次提示 token 无效。

BDD: 测试管理真实 E2E 执行进入终态 -> Given 本机前后端、测试租户、管理员账号、Runner token、Codex CLI、Playwright 和目标测试项前置/清理闭环均可用；When 用户通过真实测试管理页面按可见业务名称点击行级“执行”；Then 页面必须创建真实 executionId，由在线 Runner 领取并进入终态，且最终无 token 错误、系统异常、遗留活动执行项或 Runner 运行计数残留。

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

## Milestone 8

- Status: verification_complete.
- BDD: 标准本地后端重启保持 Runner token -> Given 工作区已有受控 Runner token 文件或首次启动尚未生成；When 通过 `restart-int-ruoyi-local.ps1 -Component backend` 重启后端；Then 脚本复用工作区唯一 token，空文件 fail-fast，并在停止、构建和启动后端前注入同一 token。
- RED: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -k workspace_codex_runner_token_file -q` -> FAIL，预期原因是根 `.gitignore` 缺少 `.runtime/` 与 `**/.runtime/`，持久 token 文件存在误纳入 Git 的风险。
- GREEN: 添加根 `.gitignore` 保护后重跑同一聚焦测试 -> PASS，`1 passed, 14 deselected`；`git check-ignore -v .runtime/codex-test-runner/runner-token.txt` 命中 `**/.runtime/`。
- REGRESSION: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，`15 passed`。
- REGRESSION: `restart-int-ruoyi-local.ps1` PowerShell parser -> PASS。
- REGRESSION: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js`、`node tests/e2e/system-codex-test-management-static.spec.js`、`node --check scripts/codex-test-runner.mjs` -> PASS。
- REGRESSION: `python -X utf8 -m pytest -q script/tests/test_codex_test_node_chain_migration.py script/tests/test_codex_test_management_migration.py` -> PASS，`4 passed`。
- Runtime registration: 使用工作区受控 token 对当前 `48081` 后端执行最终注册探针 -> PASS，业务码 `0`，探针会话 `39`；未记录 token 明文。
- Runtime idle heartbeat: 等待一个 heartbeat 周期后只读核对实际 Runner 会话 `36` -> `ONLINE`、`current_running_count=0`、heartbeat age `1` 秒，低于后端 `60` 秒超时。
- Runtime ownership: 当前后端 PID `55984` 运行 `output/runtime/int_main/backend-runtime-control-20260727-214426.jar`，health `UP`；主 Runner PID `65964` 运行 `IntRuoyiFronted/scripts/codex-test-runner.mjs --loop`，仅有 `conhost.exe` 子进程。
- Real E2E: Playwright 通过真实登录和侧边菜单进入 `系统管理 > 测试管理`；Runner 显示“可用”，租户、Runner 状态、节点串选项、测试项分页和监控请求均为 HTTP `200` / 业务码 `0`；页面无“系统异常”和 `Codex Runner token 无效或未配置`，控制台错误数 `0`，浏览器会话已关闭。
- Isolation: `8088/48088` 节点串运行态和其 Runner 未被操作；并发批次 `14` 独立达到 `PASS`。
- EXPERIENCE: `project-experience-consolidation` -> 将标准重启复用工作区唯一 Runner token 的前置门禁合并到 `docs/local-runtime.md`，并在 `docs/experience-index.md` 增加精确关键词路由；未新建长期经验文档。

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

## 2026-07-27 Runtime Reopen

- Status: in_progress.
- RED: 当前受控 token 对 `POST /admin-api/system/codex-test-runner/register` 的正确请求返回业务码 `1002031011`，再次复现“Codex Runner token 无效或未配置”。
- Runtime ownership: `48081` 当前监听 PID `4000`，启动时间 `2026-07-27 21:44:45`，运行不可变 Jar `output/runtime/int_main/backend-runtime-control-20260727-214426.jar`；Jar SHA256 为 `64569267E03EB99019D09292AD86EA3A323D5A71FE2EA220F1067AC60E0070FE`，修改时间早于进程启动时间。
- Ownership boundary: PID `4000` 来自并发任务 `20260727-batch-record-list-detail-500` 的稳定 Jar 修复，该任务已 `ready_for_closeout`；并发节点串 Runner 会话 `34`、前后端 `8088/48088` 和批次 `13/14` 位于 `D:\IntRuoyiWorktree\20260727-codex-test-node-chain-build`，不依赖 `48081`，本任务不停止、不取消、不修改。
- Safety preflight: `48081` health 为 `UP`，前端 `8081` 为 HTTP `200`，检查时 `48081` 持续连接数为 `0`；主工作区旧 Runner PID `55972` 已不存在。
- Next action: 复用 PID `4000` 的同一不可变 Jar 与原启动参数，不从脏工作区重建；为后端和主工作区 Runner 生成同一受控本地 token，完成注册、页面和 heartbeat 复验。

## 2026-07-27 Runtime Reopen Resolution

- Runtime realignment: PASS；不可变 Jar 后端已由 PID `55984` 在 `48081` 运行，health `UP`，主 Runner PID `65964` 注册为会话 `36`。
- Token contract: PASS；受控注册业务码 `0`，标准重启脚本已获得工作区持久 token 契约，后续重启不再依赖一次性手工环境注入。
- Page and heartbeat: PASS；真实测试管理页面无系统异常/token 错误，等待一个 heartbeat 周期后会话 `36` 仍为 `ONLINE`、运行计数 `0`、heartbeat age `1` 秒。
- Remaining blocker: 仅剩大量非本任务并发脏改动导致无法安全提交和推送；不影响本次实现与 E2E 结论，但阻止任务状态标记为 `completed`。
- Closeout state: `ready_for_closeout`；实现、运行态、真实 E2E、回归测试、经验沉淀和文档结构验证已完成，等待 task-closeout cleanup preview/apply。
- CLEANUP PREVIEW: PASS；keep 为四份核心任务证据，delete 为两份旧后端日志和一次性 token 对齐脚本，blocked/warnings 均为空。
- CLEANUP APPLY: PASS；三项临时产物已删除，主工作区后端/Runner 与隔离节点串运行态未受影响。
- CLOSEOUT BLOCKER: `git status --short --branch --untracked-files=all` 仍包含大量非本任务并发源码、测试和任务文档改动，无法安全建立基线提交或只提交本任务；因此不执行 commit/push，状态保持 `ready_for_closeout`。

## Milestone 9 Real E2E Run

- Status: BLOCKED；真实运行已完成，但业务结果未通过。
- Preflight: PASS；`http://127.0.0.1:8081` HTTP `200`，`48081` health `UP`，受控 token 注册探针业务码 `0`，主 Runner 会话 `36` 初始 `ONLINE/current_running_count=0/heartbeat age=2s`，Codex CLI `0.145.0` 与 Playwright CLI 可用。
- Target: 测试租户 `芋道源码`，测试项 `独立顺序验证-20260727-后续项`（caseId `35`）；测试方法仅查看测试管理标题、测试项页签和 Runner 状态，不修改业务数据。
- Real page path: Playwright 打开本机登录页，以 `芋道源码/admin` 登录，依次点击 `系统管理 > 测试管理`；按页面可见行定位 caseId `35`，点击同行“执行”，页面提示“已创建执行批次 17”并切换到“运行监控”。
- Execution claim: PASS；批次 `17` 由 Runner 会话 `36` 领取，页面显示“执行中/正在执行测试方法项第 1 项”，数据库 `execution_case=RUNNING`；进程树实际存在 `cmd.exe -> node.exe -> codex.exe`。
- Execution heartbeat: 执行期 Runner `current_running_count=1`，heartbeat age 持续低于 `60s`。
- Terminal result: FAIL；批次开始于 `2026-07-27 22:54:26`，结束于 `2026-07-27 23:04:27`，执行批次 `FAIL`，执行项与检查点均为 `BLOCKED`。
- Failure reason: `Codex Runner 执行失败：codex exec timed out after 600000ms`；页面“测试记录 > 查看结果”显示相同失败描述，未伪造通过。
- Page errors: 结果抽屉出现 `接口请求超时,请刷新页面重试!` 与 `timeout of 30000ms exceeded`；Playwright 控制台记录重复 Axios 30 秒超时，最终控制台统计为 `1 error / 2 warnings`。
- Settlement: PASS；终态后会话 `36` 的 `current_running_count=0`，无 Codex/cmd 后代，无 `%TEMP%\codex-test-result-17-*` 文件。
- Idle heartbeat: FAIL；会话 `36` 的 heartbeat age 持续超过 `60s`，Runner stderr 记录 `/heartbeat timed out after 30000ms` 和 `/register timed out after 30000ms`。新 PowerShell 注册探针仍在 `89ms` 返回业务码 `0`，说明 token 与后端注册接口可用，过期发生在旧 Runner HTTP 客户端。
- Runtime cleanup: PASS；确认 PID `65964` 空闲、运行计数 `0` 且仅有 `conhost.exe` 后停止该任务自有旧 Runner；并发隔离 Runner 会话 `41` 和批次 `18` 未停止、取消或修改。
- Evidence: `output/playwright/20260727-codex-runner-token-e2e/real-run-case-35/batch-17-failed.png`。
- Secret cleanup: PASS；原始 Playwright 登录快照和带鉴权网络 trace 已删除，仅保留不含凭据的失败结果截图。

## Milestone 10 Runtime And Readonly Runner Fix

- Status: verification_complete.
- Runtime blocker: 复验 `node doc/tasks/20260727-codex-runner-token-invalid/login-hang-probe.cjs` 在旧后端 PID `55984` 上仍超时；线程栈已定位登录链路阻塞在 Logback Console/stdout 写入。
- Runtime fix: 确认 PID `55984` 属于 `E:\IntRuoyi` 的 `48081` 不可变 Jar 后，停止旧进程并用同一 `output/runtime/int_main/backend-runtime-control-20260727-214426.jar` 启动新后端 PID `29284`，stdout/stderr 重定向到稳定运行目录，不从脏源码重建。
- Runtime GREEN: health `UP`；`node doc/tasks/20260727-codex-runner-token-invalid/login-hang-probe.cjs` 不再超时；按真实 Runner payload 注册探针返回业务码 `0`，探针会话 `52`。
- BDD: 只读测试管理自检必须快速完成 -> Given 测试项只查看测试管理页面；When Runner 领取该只读测试项；Then Codex 必须在只读预算内返回 JSON 结果，不得因项目规则探索占满 600 秒或遗留 Runner 运行计数。
- RED: `node tests/e2e/codex-test-runner-readonly-timeout-static.spec.js` -> FAIL，新增合同要求缺失 `CODEX_READONLY_REASONING_EFFORT`、只读 `--ignore-rules` 执行参数和最短 Playwright 路径 prompt。
- GREEN: `node tests/e2e/codex-test-runner-readonly-timeout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/codex-test-runner-http-client-static.spec.js` -> PASS。
- GREEN: `node --check scripts/codex-test-runner.mjs` -> PASS。
- REGRESSION: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js`、`node tests/e2e/system-codex-test-management-static.spec.js`、`node tests/e2e/system-codex-test-run-monitor-static.spec.js` -> PASS。
- Real E2E setup: 临时只读自检脚本通过真实登录进入 `系统管理 > 测试管理`，用页面“新增测试项”创建任务自有测试项，仅验证标题、测试项页签和 Runner 状态；失败/成功后均通过页面删除测试项。
- Real E2E RED cleanup: 批次 `21` 因临时自检数据包含“删除测试项”被判为写入/未知并未进入只读预算，后通过正式取消接口置为 `CANCELED`；活动执行项归零，后续自检数据已改为只读描述。
- Real E2E GREEN: `node doc/tasks/20260727-codex-runner-token-invalid/real-run-readonly-after-fix.e2e.cjs` -> PASS，真实页面创建执行批次 `24`，Runner 会话 `56` 领取并完成，执行项 `45=PASS`，检查点“测试管理只读区域可见=PASS”。
- Runner settlement: 执行终态后会话 `56` 的 `current_running_count=0`、heartbeat age `18s`，活动执行项数量 `0`，无 `codex-test-result-45-*` 临时文件，无任务自有 Runner/Codex 子进程残留。
- Page cleanup: 自检测试项 `44` 通过页面删除，`system_codex_test_case.deleted=1`；页面控制台错误数 `0`。
- Evidence: `output/playwright/20260727-codex-runner-token-e2e/readonly-after-fix/summary.json` 与 `output/playwright/20260727-codex-runner-token-e2e/readonly-after-fix/final.png`。
- EXPERIENCE: `project-experience-consolidation` -> 将只读 Runner 使用中等推理、`--ignore-rules` 和最短 Playwright 路径的门禁合并到 `docs/e2e-rules.md#Codex Runner 自动测试门禁`，并在 `docs/experience-index.md` 增加 `只读 Runner 超时`、`CODEX_TEST_CODEX_READONLY_REASONING_EFFORT`、`CODEX_TEST_CODEX_READONLY_IGNORE_RULES`、`xhigh 只读冒烟超时` 等路由关键词；未新建长期经验文档。
- CLEANUP PREVIEW/APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-codex-runner-token-invalid --mode preview/apply` -> PASS；delete 为空，blocked/warnings 为空，保留登录阻塞线程栈、只读真实 E2E 脚本、PASS summary 和截图。
- Closeout blocker: `git status --short --branch --untracked-files=all` 仍存在非本任务并发脏改动，不能安全提交/推送或标记 `completed`。
