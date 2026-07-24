# 执行日志

## 2026-06-11

- BDD: DCC 数据入口与运行控制台入口必须独立 -> Given DCC 上传/验证需要访问测试服务器前端, And 运行控制台动作需要访问当前 worktree 前端和当前代码后端, When 完整流程脚本派生子步骤环境变量, Then `DCC_BACKUP_E2E_BASE_URL` 不得覆盖 `RUNTIME_CONTROL_E2E_BASE_URL`。
- 真实流程证据: 远端 DCC 写入后，完整流程 B3 rehearsal 走到 `172.30.30.58:48081`，没有使用本地当前代码 `48082`，原因是脚本把 DCC base URL 复用给 Runtime base URL。
- RED: `node tests\e2e\dcc-flow-split-base-url.test.cjs` -> FAIL, 完整流程脚本仍使用单一 `BASE_URL` 覆盖 DCC 和 Runtime 入口。
- GREEN: `node tests\e2e\dcc-flow-split-base-url.test.cjs` -> PASS。
- GREEN: `node --check scripts\dcc-incremental-backup-restore-real-flow-gate.mjs` -> PASS。
