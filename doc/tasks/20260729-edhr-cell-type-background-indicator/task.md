# eDHR 单元格类型背景色标识

## Task Goal

隐藏 eDHR 执行填写页原表模式中每个可填写单元格右上角的类型小标识，改用不同字段类型对应的单元格背景色区分文本、数字、日期、日期时间、勾选、签名和附件。

## Milestones

1. `completed`：确认截图红框对应共享模板单元格类型标识和执行页入口。
2. `completed`：补充聚焦静态合同并记录 RED。
3. `completed`：实现执行页类型背景色展示，保留共享组件默认小标识模式。
4. `completed`：运行聚焦合同、相邻回归和类型检查，类型检查保留无关历史阻塞。
5. `completed`：直接复核当前源码和 `HEAD`，确认实现未被覆盖；重新运行聚焦合同并完成清理。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-cell-type-background-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-original-rule-legend-hidden-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：新增任务专用合同，避免修改并发任务正在编辑的宽合同。
- 静态合同与真实 E2E 同步门禁：本次只改变视觉标识方式，不改变真实填写路径或 API 契约。
- 同文件并行改动选择性暂存门禁：`ExecutionPage.vue` 有并发改动，提交时只暂存本任务相关 hunk。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过共享组件显式展示模式和类型样式解决。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Concurrency Review

- 先前基于不完整检索结果误判实现被覆盖；随后直接读取当前源码与 `HEAD`，确认 `cell-type-display="background"`、`cellTypeDisplay`、类型 class 和七种背景色规则均完整保留。
- 已在当前工作树重新运行聚焦合同和图例隐藏合同，结果均为 PASS；当前未发现本任务文件与并发改动冲突。

## Verification Scope Note

- `edhr-batch-template-simulate-static.spec.js` 已运行但失败在既有无关断言“模拟页必须校验批次执行 ID”，本次只保留同范围的 `edhr-batch-template-simulate-red-box-hidden-static.spec.js` 作为模拟页回归合同。
- `pnpm ts:check` 首次超时后复跑失败在 `src/views/form-center/business-action/ActionFormPanel.vue:257` 的既有 `updatedTime` 类型错误，未涉及本任务文件。

## Cleanup Keep

- doc/tasks/20260729-edhr-cell-type-background-indicator/frontend-feature-evidence.md
- task-closeout preview/apply 均通过，未删除任何文件。

## Final Result

- 实现、聚焦验证、相邻回归、任务清理和远端推送均已完成。
- 脏工作树基线提交：`e03d6ff755f711bda4be9e5ddb33bf1d40606faa`，已推送至 `origin/int_main`。
- 最终结果：执行填写页不再显示单元格类型 item，改为七种字段类型对应的背景色标识。
