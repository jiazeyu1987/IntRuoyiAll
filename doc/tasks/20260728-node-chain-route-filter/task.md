# 测试管理串行路线筛选

## Task Goal

在 `系统管理 > 测试管理` 列表快速筛选区域增加 `串行路线` 下拉框；选择某条串行路线后，列表只显示该串行路线对应的测试节点。

## Milestones

- [x] 创建隔离 worktree 和任务记录。
- [x] 审计当前测试管理筛选、节点串字段和静态契约。
- [x] 先补 RED 静态契约覆盖串行路线下拉。
- [x] 实现下拉展示、筛选参数和重置行为。
- [x] 运行聚焦验证并记录证据。
- [x] 按“先融合再测试”融合最新 `origin/int_main` 后复测。

## Expected Verification

- 快速筛选区域显示 `串行路线` 下拉框。
- 下拉选项来自当前测试项中的节点串/串行路线名称。
- 选择一条串行路线后，列表请求带上对应筛选条件，只显示该串行路线节点。
- 清空选择后恢复显示全部测试项。
- 不影响项目、测试项名称、测试租户和执行按钮的现有行为。

## Current Status

ready_for_closeout

## 经验门禁

### 前端静态契约隔离门禁

- Trigger: 当前任务只改测试管理页一个筛选控件。
- Preflight check: 使用任务专用最小静态契约覆盖新增下拉、筛选参数和清空行为。
- Blocker: 静态契约不能稳定先 RED 后 GREEN，或失败点无法证明属于当前需求时停止。
- Verification: 运行 `node tests/e2e/system-codex-test-node-chain-static.spec.js` 或任务专用合同。
- Forbidden action: 禁止修改无关大契约或把无关 `ts:check` blocker 当作本任务通过证据。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### 并行主工作区隔离门禁

- Trigger: `E:\IntRuoyi` 正在被并行任务写入。
- Preflight check: 本任务只在 `D:\IntRuoyiWorktree\20260728-node-chain-route-filter` 工作，基于 `origin/int_main`。
- Blocker: 需要修改主工作区并行任务文件，或目标 worktree 与远端主线无法融合时停止。
- Verification: 提交和推送只包含本任务源码、测试和任务记录。
- Forbidden action: 禁止回滚、清理或暂存主工作区并行改动。
- Evidence: `docs/worktree-memory.md#并行主工作区远端快进融合门禁`。
- Current evidence: 已在任务 worktree 将 `origin/int_main` `1cab989a` 融合为 `17853328`，并重跑节点串静态合同、测试管理静态回归、`pnpm ts:check` 与端口守卫。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用测试管理已有节点串字段与筛选契约。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-node-chain-route-filter/task.md
- doc/tasks/20260728-node-chain-route-filter/execution-log.md
- doc/tasks/20260728-node-chain-route-filter/verification-report.md
- doc/tasks/20260728-node-chain-route-filter/frontend-feature-evidence.md
