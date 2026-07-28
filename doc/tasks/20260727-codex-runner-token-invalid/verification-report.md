# Verification Report

## Scope

- 修复本机测试管理执行入口的 Runner token 对齐问题。
- 修复后端重启后测试管理页面因节点串 schema 缺失连续显示“系统异常”。
- 修复 Windows Codex wrapper 被终止但不触发 Node child `close` 时 Runner 当前执行无法收敛的问题。

## Results

- PASS: Runner token 注册探针使用受控 token 返回业务码 `0`。
- PASS: 标准本地后端重启脚本复用 `.runtime/codex-test-runner/runner-token.txt`，缺失时安全生成一次、空文件 fail-fast，并在停止、构建和启动后端前注入同一 `CODEX_TEST_RUNNER_TOKEN`。
- PASS: 根 `.gitignore` 包含 `.runtime/` 与 `**/.runtime/`；`git check-ignore -v .runtime/codex-test-runner/runner-token.txt` 命中保护规则。
- PASS: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -q`，15 tests passed。
- PASS: `restart-int-ruoyi-local.ps1` PowerShell parser。
- PASS: 本地 Docker MySQL 已应用 `20260727_system_codex_test_node_chain.sql`；三列和节点串索引存在。
- PASS: `python -X utf8 -m pytest -q script/tests/test_codex_test_node_chain_migration.py script/tests/test_codex_test_management_migration.py`，4 tests passed。
- PASS: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js`。
- PASS: `node tests/e2e/system-codex-test-management-static.spec.js`。
- PASS: `node --check scripts/codex-test-runner.mjs`。
- PASS: 只读 Playwright 通过真实登录和侧边菜单进入 `系统管理 > 测试管理`；页面无“系统异常”，相关请求失败数 `0`、业务失败数 `0`。
- PASS: 真实页面创建批次 `11`、`12` 后，Runner 会话 `33` 成功领取执行项并从空闲计数 `0` 进入运行计数 `1`。
- PASS: 两次 Codex 超时均出现 child 未触发 `close` 的实际条件；Runner 在 5000 ms 有界等待后退出当前任务，持续 heartbeat 且 `current_running_count=0`。
- PASS: 批次 `11/12` 无 `PENDING/CLAIMED/RUNNING` 执行项，任务 Runner 无 Codex/cmd 后代，且无残留 `codex-test-result-*` 文件。
- PASS: 最终 Playwright 真实页面从登录页经侧边菜单进入 `系统管理 > 测试管理`；Runner 显示“可用”，租户、Runner 状态、节点串选项、测试项分页和监控请求均为 HTTP `200` / 业务码 `0`，页面无“系统异常”和 token 错误，控制台错误数 `0`。
- PASS: 当前实际 Runner 会话 `36` 等待一个 heartbeat 周期后仍为 `ONLINE`、`current_running_count=0`、heartbeat age `1` 秒，小于 `60` 秒超时；PID `65964` 无 Codex/cmd 后代进程。
- PASS: `git diff --check` 针对本任务实现、测试和任务文档未发现空白错误。
- PASS: 最终 task-closeout cleanup preview/apply 保留四份核心任务证据，删除两份已不再占用的旧后端日志和一次性 token 对齐脚本，无 blocked 或 warnings。

## Runtime State

- Backend: PID `55984`, port `48081`, health `UP`，运行不可变 Jar `output/runtime/int_main/backend-runtime-control-20260727-214426.jar`。
- Frontend: PID `41928`, port `8081`。
- Main Runner: PID `65964`, session `36`, `ONLINE`，`current_running_count=0`；等待一个 heartbeat 周期后 heartbeat age `1` 秒。
- Final controlled registration probe: business code `0`, probe session `39`。
- Batch `11`: `FAIL`, six execution cases `BLOCKED`。
- Batch `12`: `FAIL`, six execution cases `BLOCKED`。
- Current isolated runtime: 并发 worktree 使用 `8088/48088`；本任务未停止、取消或修改该运行态，其批次 `14` 独立达到 `PASS`。

## Limits

- 批次 `11/12` 因 Codex 600 秒超时进入终态，不是批记录业务节点闭环 PASS 证据；固定解析样本缺失仍是该业务 E2E 的正式前置阻塞。
- 主动取消的 RED 证据来自批次 `10`：服务端取消后旧 Runner 会话仍长期计数 `1`。GREEN 运行态使用相同 `stopChild` 路径的 timeout 场景验证了 child 不触发 `close` 时可有界归零，并由聚焦静态合同锁定 cancel/timeout 共用该路径。
- 最终提交和推送被大量非本任务并发脏改动阻塞，任务不得标记 `completed`。
- 隔离节点串运行态中的活动或历史执行不属于本任务，不作为本任务残留，也未被清理或取消。

## Requested Real E2E Run

- Result: FAIL。
- Path: Playwright 真实登录 `http://127.0.0.1:8081`，点击 `系统管理 > 测试管理`，在可见业务行 `独立顺序验证-20260727-后续项` 点击“执行”，创建批次 `17`。
- Tenant/user: `芋道源码/admin`。
- Target scope: caseId `35`，只读查看测试管理标题、测试项页签和 Runner 状态，不修改业务数据。
- Runner: 会话 `36` 成功领取，执行中 `current_running_count=1`、heartbeat 未过期，并真实启动 Codex 子进程链。
- Terminal state: 批次 `17=FAIL`；执行项和检查点均为 `BLOCKED`。
- Failure: `Codex Runner 执行失败：codex exec timed out after 600000ms`。
- Page evidence: 测试记录页“查看结果”显示相同失败原因，同时出现 `接口请求超时,请刷新页面重试!` / `timeout of 30000ms exceeded`。
- Console: 最终 `1 error / 2 warnings`，包含重复 Axios 30 秒请求超时；无 token 无效提示。
- Settlement: `current_running_count=0`，无 Codex/cmd 后代，无 `codex-test-result-17-*` 文件。
- Idle gate: FAIL；会话 `36` 在终态后 heartbeat/register 连续超时，heartbeat age 超过后端 `60s` 阈值。新进程外注册探针业务码 `0`，说明 token 和后端接口本身仍可用。
- Cleanup: 仅停止空闲且无任务子进程的本任务旧 Runner PID `65964`；并发会话 `41` / 批次 `18` 未操作。
- Screenshot: `output/playwright/20260727-codex-runner-token-e2e/real-run-case-35/batch-17-failed.png`。
- Security: 原始登录快照与带鉴权 trace 已删除，未把密码、token 或 Authorization 头写入任务文档。

## Final Real E2E Run After Fix

- Result: PASS。
- Runtime repair: 旧后端 PID `55984` health `UP` 但登录请求挂起；已用同一不可变 Jar 启动新后端 PID `29284`，stdout/stderr 改为稳定日志文件，登录探针不再超时。
- Registration: 按真实 Runner payload 注册探针返回业务码 `0`，探针会话 `52`；未记录 token 明文。
- Runner fix: 只读任务追加 `--ignore-rules` 与 `model_reasoning_effort="medium"`，并在 prompt 中约束最短浏览器路径、临时 Node.js Playwright 脚本、禁止建档/改文件/构建/无关源码探索。
- RED/GREEN: 扩展后的 `node tests/e2e/codex-test-runner-readonly-timeout-static.spec.js` 先失败后通过；`node tests/e2e/codex-test-runner-http-client-static.spec.js` 与 `node --check scripts/codex-test-runner.mjs` 通过。
- Regression: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js`、`node tests/e2e/system-codex-test-management-static.spec.js`、`node tests/e2e/system-codex-test-run-monitor-static.spec.js` 通过。
- Path: Playwright 真实登录 `http://127.0.0.1:8081`，进入 `系统管理 > 测试管理`，通过页面新增任务自有只读测试项并点击同行“执行”。
- Execution: 批次 `24=PASS`；执行项 `45=PASS`；检查点“测试管理只读区域可见=PASS”，实际文本为页面显示“测试管理”“测试项”“Runner 状态”。
- Runner: 任务自有会话 `56` 领取并完成；终态后 `current_running_count=0`、heartbeat age `18s`，无任务自有 Runner/Codex 子进程残留。
- Cleanup: 自检测试项 `44` 通过页面删除，活动执行项数量 `0`，无 `%TEMP%\codex-test-result-45-*` 临时结果文件。
- Page: 控制台错误数 `0`；最终截图 `output/playwright/20260727-codex-runner-token-e2e/readonly-after-fix/final.png`，摘要 `output/playwright/20260727-codex-runner-token-e2e/readonly-after-fix/summary.json`。

## Current Limits

- 旧任务自有 Runner 会话 `53` 在一次失败脚本超时后保留 stale `current_running_count=1`，但其 heartbeat 已超过在线阈值、无活动执行项、无对应进程；后续最终会话 `56` 已作为当前验证会话归零。
- 最终提交/推送仍被非本任务并发脏改动阻塞；任务状态保持 `ready_for_closeout`，不得标记 `completed`。
