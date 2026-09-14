# 工艺路线表单槽位数量徽标

## Task Goal

在工艺路线流转关系图中，用户选择左侧“表单槽位”配置项时，每个工序节点在截图黄框位置仅显示附加表单数量；只有 `MAIN` 批记录表单或 legacy 记录的工序不显示数量徽标、`0` 或红色未绑定提示。

## Milestones

- [x] 创建任务记录并完成脏工作区基线隔离。
- [x] 先写静态合同，验证当前实现缺少节点表单数量徽标。
- [x] 实现节点附加表单数量计算、徽标渲染和零绑定隐藏口径。
- [x] 运行静态合同、节点布局回归和 TypeScript 检查。
- [x] 完成验证报告、经验沉淀、cleanup 和提交推送。

## Expected Verification

- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js`
- `node tests/e2e/mes-route-flow-binding-border-static.spec.js`
- `pnpm e2e:mes:route-flow-node-text-center:static`
- `pnpm ts:check`
- 若本地前后端、登录前置和真实数据可用，再做工艺路线编辑页真实只读视觉验证。

## Current Status

completed

## 经验门禁

- Frontend feature delivery: 保持现有前端契约、路由、状态和样式模式；先记录 BDD，再 RED/GREEN/REGRESSION。
- 前端静态契约隔离门禁: 使用任务专用最小静态合同覆盖当前需求；若全量 `pnpm ts:check` 失败在无关历史问题，记录 blocker，不得伪造通过。
- 静态合同与真实 E2E 同步门禁: 修改静态合同后重跑目标合同；不得为通过合同修改无关 DOM 或文案。
- 脏工作区基线门禁: 当前任务实现前若有既有脏改动，先独立基线提交并记录 hash；本任务文件不得混入基线。
- PowerShell/UTF-8 门禁: 中文任务文档使用 UTF-8 写入和复读校验；PowerShell 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用现有路线工序 `formBindings`，统一在节点计算非 `MAIN` 的有效附加表单数量。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260726-route-flow-form-slot-count-badge/task.md
- doc/tasks/20260726-route-flow-form-slot-count-badge/execution-log.md
- doc/tasks/20260726-route-flow-form-slot-count-badge/frontend-feature-evidence.md
- doc/tasks/20260726-route-flow-form-slot-count-badge/verification-report.md
- doc/tasks/20260726-route-flow-form-slot-count-badge/real-e2e-output/form-slot-count-badge-real-result.json
- doc/tasks/20260726-route-flow-form-slot-count-badge/real-e2e-output/form-slot-count-badge-real.png
