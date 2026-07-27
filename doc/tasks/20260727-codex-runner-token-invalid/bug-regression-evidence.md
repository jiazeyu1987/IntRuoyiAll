# Bug Regression Evidence

## Bug Summary

- 测试管理点击执行曾提示“Codex Runner token 无效或未配置”。
- 后端重启后进入测试管理连续显示“系统异常”。
- 服务端取消或 Codex 超时终止 Windows 进程树后，wrapper 可能不触发 Node child `close`，导致 Runner 会话运行计数长期停留在 `1`。

## Expected Behavior

Runner token 必须与当前后端运行态一致；测试管理依赖的正式 schema 必须存在；timeout/cancel 后即使 child 不触发 `close`，Runner 也必须在有界时间内退出当前任务并恢复空闲，且不得遗留活动执行、后代进程或临时结果文件。

## Reproduction

- Token RED: Runner 注册返回业务码 `1002031011`。
- Schema RED: 测试管理初始化接口因缺少 `node_chain_name`、`node_chain_sort`、`node_chain_execution` 返回错误，页面连续显示“系统异常”。
- Lifecycle RED: 批次 `10` 已取消且 Codex 后代消失，但旧 Runner 会话 `31` 仍持续 heartbeat 并上报 `current_running_count=1`。
- Contract RED: `node tests/e2e/codex-test-runner-child-settlement-static.spec.js` 首次失败，因为 Runner 只等待 child `close`，没有独立收敛超时。

## Root Cause

- 后端启动时未注入与受控 Runner 相同的 token。
- 本地 Docker MySQL 未应用正式节点串迁移。
- `runCodexForTask()` 在调用 `stopWindowsProcessTree()` 后仍无限等待 child `close`；Windows wrapper 已消失但事件未触发时，`runOnce()` 无法离开当前任务。

## Regression Test

- Runner 子进程收敛静态合同要求独立 `CODEX_TEST_CHILD_SETTLE_TIMEOUT_MS`、stop request Promise 和自然退出 Promise 共同参与 `Promise.race`。
- timeout/cancel 异常路径必须 `await stopChild()` 后再退出，禁止仅杀进程后无限等待 `close`。
- 测试管理相邻静态合同、迁移 pytest 和真实页面诊断共同覆盖页面初始化与 Runner 链路。

## RED:

- `node tests/e2e/codex-test-runner-child-settlement-static.spec.js` -> FAIL，缺少有界子进程收敛契约。
- 批次 `10` 取消后会话 `31` -> `current_running_count=1`，不符合空闲心跳契约。

## GREEN:

- `node tests/e2e/codex-test-runner-child-settlement-static.spec.js` -> PASS。
- 批次 `11`、`12` 均真实触发 child 5000 ms 内不发 `close`；修复后的会话 `33` 返回 `current_running_count=0`。
- Playwright 真实页面诊断 -> PASS，无“系统异常”，请求失败数 `0`、业务失败数 `0`。
- 节点串迁移测试 -> PASS，4 tests。

## Verification

- `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- `node --check scripts/codex-test-runner.mjs` -> PASS。
- 批次 `11/12` 的活动执行项数量 `0`。
- Runner 无 Codex/cmd 后代。
- 批次 `11/12` 无 `codex-test-result-*` 临时文件。

## Risk And Scope

修复只改变 Runner 子进程生命周期等待，不增加 fallback、默认成功或异常吞噬。child 未触发 `close` 时会记录明确错误并释放 I/O 句柄；原始取消或超时错误继续按正式终态处理。

## Blockers And Follow-Up

- 批记录节点完整业务 PASS 仍缺固定解析样本，不得用替代文件、mock、直接 SQL 或 API-only 绕过。
- 最终提交/推送被非本任务并发脏改动阻塞。
