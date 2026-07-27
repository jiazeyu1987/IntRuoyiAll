# Codex Runner token 无效或未配置

## Task Goal

修复系统管理测试管理页面点击“执行”时提示“Codex Runner token 无效或未配置”的问题，并处理后端重启后页面连续“系统异常”及 Runner 在 Windows 子进程不触发 `close` 时运行计数无法归零的回归。

## Milestones

1. 定位执行入口、Runner token 配置来源、后端校验和现有测试。
2. 用 BDD 场景建立先失败的回归测试，确认根因。
3. 实施最小根因修复并通过 GREEN。
4. 完成数据库迁移、Runner 生命周期、前端页面和真实执行路径验证。
5. `ready_for_closeout` 后执行 cleanup preview/apply，完成任务记录和推送。

## Expected Verification

- 任务专用回归测试先 RED 后 GREEN。
- 相关前端静态检查/构建或后端测试通过，或在日志中记录与本任务无关的既有阻塞。
- 若运行真实页面，必须核对前端入口、后端入口、测试租户、测试账号、Runner token、Codex CLI、Playwright 和 Runner heartbeat。
- timeout/cancel 后必须证明 Runner 当前会话运行计数归零、无活动执行项、无 Codex 后代进程和临时结果文件。
- `git diff --check` 通过，任务提交已推送到当前分支 `origin`，且分支不再 ahead。

## Applicable Experience Gate

### Codex Runner 自动测试门禁

- Trigger: 修改、运行或验收系统管理测试管理、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前确认前后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任；后端用当前 token 完成注册探针，并核对执行中/空闲 heartbeat。
- Blocker: token 与后端运行态不一致、注册失败、heartbeat 过期、租户或测试数据前置条件缺失时停止；不得把 Runner 进程存在当作在线证明。
- Verification: 记录页面执行入口、Runner 注册/领取/执行期心跳/空闲心跳/回写证据、最终 UI 状态和必要的只读核验。
- Forbidden action: 禁止 API-only、mock 成功、离线跳过、裸调用 Codex CLI、前端硬拦截或顺序降级冒充真实 E2E。
- Evidence: `docs/e2e-rules.md` 的“Codex Runner 自动测试门禁”及相关历史任务验证报告。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；不增加降级路径，不隐藏 token 或 Runner 错误。
- 是否从根因和长期维护角度解决：是；先确认 token 配置契约和校验链路，再做最小修复。
- 是否存在临时补丁或绕过：否；不改用 API-only、mock 或默认成功。

## Current Status

in_progress

## Milestone 9

- Status: blocked.
- 用户明确要求在 `系统管理 > 测试管理` 中执行一次真实 E2E 运行测试。
- 使用 Playwright 从本机登录页进入测试管理，通过页面可见测试项和行级“执行”按钮创建一次真实执行，不使用 API 代替页面启动。
- 执行前确认 `8081/48081`、测试租户、管理员账号、Runner token、Runner heartbeat、Codex CLI、Playwright 浏览器和目标测试项前置/清理闭环。
- 等待执行进入终态，记录 executionId、页面状态、Runner 执行中/空闲 heartbeat、检查点结果、控制台/网络错误和必要的只读 API/数据库终态核验。
- 若目标前置、固定样本或清理责任缺失，必须 fail-fast，不改跑其它测试项、不造占位数据、不使用 API-only 或 mock。
- 已通过真实页面选择只读测试项 `独立顺序验证-20260727-后续项` 并点击行级“执行”，创建批次 `17`。
- 批次 `17` 由主 Runner 会话 `36` 领取，真实启动 `cmd.exe -> node.exe -> codex.exe` 执行链，持续 heartbeat 并运行至 600 秒上限。
- 最终页面和数据库一致显示 `execution=FAIL`、执行项/检查点 `BLOCKED`，失败原因 `Codex Runner 执行失败：codex exec timed out after 600000ms`。
- 终态后运行计数归零，无 Codex/cmd 后代，无 `codex-test-result-17-*` 临时文件；但会话 `36` 后续 heartbeat/register 请求持续 30 秒超时并超过 60 秒离线门禁。
- 已仅停止本任务空闲且无非控制台子进程的旧 Runner PID `65964`；未触碰并发隔离 Runner 会话 `41`、批次 `18` 或其进程。
- 页面结果抽屉还出现 `接口请求超时,请刷新页面重试!` 与 `timeout of 30000ms exceeded`，因此本次真实 E2E 结论为失败，不得判定通过。

## Milestone 8

- Status: verification_complete.
- 已将标准本地后端重启的 Runner token 契约收敛到工作区唯一文件 `.runtime/codex-test-runner/runner-token.txt`。
- token 文件缺失时安全生成一次，已存在时复用，空文件 fail-fast；脚本在停止旧后端、构建和启动 Java 前注入同一 `CODEX_TEST_RUNNER_TOKEN`。
- 根 `.gitignore` 已保护 `.runtime/` 与 `**/.runtime/`，受控 token 文件不会出现在 Git 状态或提交中。
- 运行控制脚本完整测试 `15 passed`；PowerShell parser、Runner 子进程静态合同、测试管理静态合同、Node 语法和迁移测试全部通过。
- 真实页面最终复验通过：登录后从 `系统管理 > 测试管理` 进入，Runner 显示“可用”，租户、Runner 状态、节点串选项、测试项分页和监控请求均为 HTTP `200` / 业务码 `0`，页面无“系统异常”和 token 错误，浏览器控制台错误数 `0`。
- 当前 `48081` 后端 PID `55984` health 为 `UP`；主 Runner PID `65964`、会话 `36` 在等待一个 heartbeat 周期后仍为 `ONLINE`、`current_running_count=0`、heartbeat age `1` 秒，小于 `60` 秒超时。
- 隔离节点串运行态 `8088/48088` 未被操作；其批次 `14` 独立完成 `PASS`，不作为本任务实现证据。

## Milestone 6

- 用户明确要求执行真实 E2E 验证。
- 通过 Playwright 从 `系统管理 > 测试管理` 页面按可见测试项名称定位并点击“执行”。
- 只选择已存在且测试方法包含前置复位、页面操作、结果核验和清理恢复的测试项。
- 验证执行批次进入终态、Runner heartbeat 未过期、页面不再提示 token 错误，并记录测试数据与清理结果。
- 已应用正式节点串迁移，测试管理页面初始化请求恢复。
- 已通过真实页面创建批次 `11`、`12` 并由 Runner 领取；两批次因 Codex 600 秒超时进入 `FAIL`，六个执行项均为 `BLOCKED`，不作为业务闭环 PASS 证据。
- 两次运行均复现 Windows wrapper 被终止后不触发 `close`，修复后的 Runner 在 5 秒有界等待后恢复空闲；最新会话 `33` 的 `current_running_count=0`。
- 最终只读 Playwright 复验确认页面无“系统异常”，请求失败数和业务失败数均为 `0`。

## Closeout Blocker

运行态 token 阻塞已解除：当前不可变 Jar 后端 PID `55984` 与主 Runner PID `65964` 使用同一受控 token，最终注册探针业务码 `0`，实际 Runner 会话 `36` 的空闲 heartbeat 持续有效。

最终提交/推送仍被大量并发任务脏改动阻塞，当前无法安全建立“既有脏改动基线提交”并保证不提交未完成或敏感的其他任务内容。

## Cleanup Evidence

- Cleanup preview: PASS; 删除范围仅包含本任务临时日志、重启脚本、一次性真实页面验证脚本、摘要 JSON 和截图。
- Cleanup apply: 首次执行被活动后端文件锁阻塞，`backend-token-alignment.stderr.log` 与 `backend-token-alignment.stdout.log` 仍由当前 `48081` 后端运行态占用。
- Cleanup adjustment: 为避免中断已修复并可用的本机后端运行态，活动后端日志临时列入 `Cleanup Keep`，其余本任务临时产物继续清理。
- Cleanup apply: PASS; 已删除 `restart-backend-with-token.ps1`、`runner-status-real-e2e.cjs`、`runner-status-real-summary.json` 和 `runner-status-real.png`。
- Final token probe: PASS; 按真实 Runner 请求头和 payload 注册返回业务码 `0`，`runnerSessionId=14`。
- Runner settlement cleanup preview: PASS; 仅计划删除本次一次性 Playwright 脚本及 stdout/stderr 日志，无 blocked 或 warnings。
- Runner settlement cleanup apply: PASS; 已删除 `runner-cancel-settlement-real.e2e.cjs` 及其两份日志，保留核心任务记录、bug regression evidence 和活动后端日志。
- Runtime ownership cleanup: 并发任务切换 Runner token 后，本任务 Runner PID `55972` 开始注册失败；已仅停止该任务自有进程并恢复 tracked pid 文件到任务开始前内容，未触碰并发批次 `13` 或 Runner 会话 `34`。
- Final cleanup preview: PASS；保留 `task.md`、`execution-log.md`、`verification-report.md` 和 `bug-regression-evidence.md`，仅计划删除两份已不再占用的旧后端日志和一次性 `restart-current-backend-with-runner-token.ps1`，无 blocked 或 warnings。
- Final cleanup apply: PASS；三项任务临时产物已删除，当前不可变 Jar 后端、主 Runner 和隔离 `8088/48088` 运行态均未停止或修改。
- Completion state: 保持 `ready_for_closeout`；最终提交/推送仍被非本任务并发脏改动阻塞，不得标记 `completed`。

## Cleanup Keep

- doc/tasks/20260727-codex-runner-token-invalid/task.md
- doc/tasks/20260727-codex-runner-token-invalid/execution-log.md
- doc/tasks/20260727-codex-runner-token-invalid/verification-report.md
- doc/tasks/20260727-codex-runner-token-invalid/bug-regression-evidence.md
- output/playwright/20260727-codex-runner-token-e2e/real-run-case-35/batch-17-failed.png
