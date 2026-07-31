# Execution Log

## Intent

用户在上一轮确认 3 条正式串行路线均失败后要求继续。本任务按“定位并修复阻塞，再通过真实页面完整复验 3 条路线”推进，不重启或停止归属不明的共享运行态。

## Rule Preflight

- Read: `docs/task-closeout-rules.md`
- Read: `docs/powershell-encoding.md`
- Read: `docs/local-runtime.md`
- Read: `docs/e2e-rules.md`
- Read: `docs/login-access.md`
- Read: `docs/experience-index.md`
- Skill: `bug-regression-fix-loop`
- Skill: `playwright`
- Existing unrelated path preserved: `doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/`

## BDD Scenarios

- `BDD: Runner 可启动 Codex 完成串行路线 -> Given 本机前后端、测试租户、Runner 和 Codex CLI 前置条件有效且 3 条正式节点串完整; When 用户从测试管理页面分别顺序执行工艺路线、批记录和智能排产节点串; Then Runner 按顺序完成每个节点、持续心跳并结构化回写，3 个批次最终全部 PASS 且运行计数归零`
- `BDD: Codex CLI 配置错误应在正式路线前失败 -> Given Runner 继承了不受支持的认证、插件目录或 feature 配置; When 执行受控短预算 CLI 自检; Then 自检明确失败并阻止创建正式长运行批次，不得静默降级或等待 600000ms`

## Milestone Updates

- M1 completed: Runner 同构短预算 CLI 自检退出码 `0`，证明远程插件认证与旧 feature 警告当前为非致命 warning。
- M1 completed: 写入型任务未应用只读快速策略，Codex 子进程从仓库根目录启动并继承用户级 `xhigh`；工艺路线任务实际创建任务文档并执行 Git 基线提交，智能排产任务创建任务文档后在 600 秒内未完成业务页面测试。
- M1 completed: `resource/批记录节点-解析样本.docx` 不存在，批记录路线存在独立的正式固定样本阻塞。
- M2 completed: 已更新 Runner 隔离、统一执行参数和失败诊断静态合同，并获得预期 RED。

## Verification Evidence

- Previous verification report: `doc/tasks/20260730-test-management-serial-routes-verification/verification-report.md`
- Previous result: 工艺路线与批记录首节点 Codex 子进程 `exit 1`；智能排产首节点 `600000ms` 超时。
- CLI reproduction: Runner 同构参数执行最短结构化 prompt -> PASS；Codex CLI `0.145.0`，当前认证类型为 API key，stderr 中插件和旧 feature 信息为 warning。
- Repository side effect evidence: `doc/tasks/20260730-route-node-basic-maintenance-e2e/` 创建于工艺路线执行窗口；其日志记录自动执行了 Git 基线提交 `2e2d1eb0`。`doc/tasks/20260730-smart-scheduling-workorder-admission-e2e/` 创建于智能排产首节点执行窗口并停留 `in_progress`。
- Runtime preflight: frontend `8081` HTTP `200`；backend `48081` health `UP`；Runner session `94` `ONLINE/currentRunningCount=0`；无活动 execution/case。
- Fixed sample preflight: `E:\IntRuoyi\resource\批记录节点-解析样本.docx` missing。
- `RED: node tests\e2e\codex-test-runner-readonly-timeout-static.spec.js -> FAIL, expected reason: mutating tasks have no controlled reasoning effort or repository-isolation policy`
- `RED: node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js -> FAIL, expected reason: CODEX_TEST_WORKDIR still points at repository root`
- `RED: node tests\e2e\codex-test-runner-failure-diagnostics-static.spec.js -> FAIL, expected reason: non-zero exit diagnostics preserve warning prefix instead of sanitized stderr tail`

## Blockers

- 批记录路线的固定 Word 样本缺失；Runner 修复后仍需补齐正式样本，不能使用其它文件替代。
