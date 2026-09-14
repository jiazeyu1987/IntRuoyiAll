# 删除报工管理复核按钮

## 任务目标

删除截图所示“报工管理”表格操作列中的“复核”按钮，保留同列其它操作及原有业务行为。

## Milestones

- [x] M1：建立任务记录并冻结验收边界。
- [x] M2：定位真实页面入口、组件与现有静态合同。
- [x] M3：先补充失败合同，再做最小实现。
- [x] M4：完成定向回归、类型检查与差异检查。
- [x] M5：完成任务清理和最终记录。

## Expected Verification

- 任务专用静态合同证明“报工管理”操作列不再渲染“复核”按钮。
- 静态合同证明“详情、修改、分配”按钮仍保留。
- 运行相关相邻静态合同。
- 运行 `pnpm ts:check`。
- 运行 `git diff --check`。
- 运行 `frontend-feature-delivery` evidence validator。

## Current Status

completed：实现、验证、经验检查和 cleanup preview/apply 均已通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接移除目标 UI 入口并以聚焦合同锁定，不改变接口或状态链路。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- 命中 `docs/frontend-development.md#前端截图按钮统一静态契约门禁`：按截图锁定目标操作列，以专用静态合同先 RED，删除目标入口时保留其它按钮和既有 handler。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小合同，结束边界固定为当前操作列的 `</el-table-column>`。
- 命中 `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：共享工作台必须隔离生产组长与 PQC 组长行为；只隐藏生产组长“报工管理”的复核入口，PQC 复核入口继续保留。

## Cleanup Candidates

- 已删除 `doc/tasks/20260809-remove-report-review-button/frontend-feature-evidence.md`；关键 validator 与验收结论已归档到保留记录。

## Final Verification

- 生产组长报工管理复核入口已隐藏，PQC 复核与生产报工详情、修改、分配入口均保留。
- 任务专用合同、6 个相邻合同、`pnpm ts:check`、evidence validator、`git diff --check` 和 cleanup 均通过。
