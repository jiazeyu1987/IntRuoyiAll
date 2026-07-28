# 工艺路线表单槽位新增后实时计数

## Task Goal

修复工艺路线流转关系图中“表单槽位”节点数量徽标在右侧新增动态表单后仍停留为旧数量的问题；新增并选择模板后，节点右上角数量应立即从 `1` 更新为 `2`。

## Milestones

- [x] 建立任务记录并确认已有完成任务不能覆盖当前补缺。
- [x] 写入聚焦回归合同，复现新增动态表单仍被当作 `MAIN` 的问题。
- [x] 实现新增动态表单槽位归类修复。
- [x] 运行目标静态合同与相关回归验证。
- [x] 更新验证报告与任务状态。

## Expected Verification

- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js`
- `node tests/e2e/mes-route-flow-binding-border-static.spec.js`
- 视影响范围补充 `pnpm ts:check` 或记录无关 blocker。

## Current Status

completed

## 经验门禁

- 前端静态契约隔离门禁：本次只补动态表单新增后的计数合同，避免修改无关宽合同。
- 静态合同与真实 E2E 同步门禁：修改静态合同后必须重跑目标合同。
- 同文件并行改动选择性暂存门禁：`RouteFlowGraphDesigner.vue` 当前已有并行改动，提交前需区分本任务 hunks。
- PowerShell/UTF-8 门禁：中文任务文档使用 UTF-8，PowerShell 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从新增动态表单的槽位默认归类修复，不改为徽标侧硬算或降级。
- `是否存在临时补丁或绕过`：否。

## Final Verification Result

- PASS：目标静态合同、相邻边框回归、节点布局回归、前端类型检查、bug evidence 校验、cleanup apply 与 UTF-8 复读均通过。

## Cleanup Keep

- doc/tasks/20260726-route-flow-form-slot-live-count/task.md
- doc/tasks/20260726-route-flow-form-slot-live-count/execution-log.md
- doc/tasks/20260726-route-flow-form-slot-live-count/bug-regression-evidence.md
- doc/tasks/20260726-route-flow-form-slot-live-count/verification-report.md
