# 20260611 DCC 真实流程入口隔离修正

## 任务目标

修正 DCC 增量备份恢复真实流程脚本中 DCC 前端入口和运行控制台入口混用的问题。DCC 上传/恢复验证可以走测试服务器真实前端，运行控制台提交必须仍可显式使用当前 worktree 前端和当前代码后端。

## 里程碑

- [x] M1 记录真实流程失败原因和 BDD 场景。
- [x] M2 补充 RED 测试，要求脚本保留独立的 Runtime base URL。
- [x] M3 修正脚本环境变量传递。
- [x] M4 运行静态验证并提交本任务改动。

## 预期验证

- `node tests\e2e\dcc-flow-split-base-url.test.cjs`
- `node --check scripts\dcc-incremental-backup-restore-real-flow-gate.mjs`
- `git diff --check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。把 DCC 数据入口和 Runtime 控制入口拆成显式契约。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 完成记录

- 完整流程脚本拆分 `DCC_BASE_URL` 和 `RUNTIME_BASE_URL`。
- 子步骤环境变量不再用 DCC 前端入口覆盖运行控制台入口。
- 验证结果：静态测试和 Node 语法检查通过。
