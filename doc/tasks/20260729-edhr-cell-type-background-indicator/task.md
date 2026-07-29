# eDHR 单元格类型背景色标识

## Task Goal

隐藏 eDHR 执行填写页原表模式中每个可填写单元格右上角的类型小标识，改用不同字段类型对应的单元格背景色区分文本、数字、日期、日期时间、勾选、签名和附件。

## Milestones

1. `completed`：确认截图红框对应共享模板单元格类型标识和执行页入口。
2. `completed`：补充聚焦静态合同并记录 RED。
3. `completed`：实现执行页类型背景色展示，保留共享组件默认小标识模式。
4. `in_progress`：运行聚焦合同、相邻回归和类型检查。
5. `pending`：更新证据、cleanup、提交并推送。

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

in_progress

## Cleanup Keep

- doc/tasks/20260729-edhr-cell-type-background-indicator/frontend-feature-evidence.md
