# 路线表单复制弹层稳定性优化

## Task Goal

修复工艺路线字段明细右侧“动态表单列表”的“复制”弹层交互：选择来源工序后不应被下拉选择误关闭，确认复制成功后应明确关闭弹层。

## Milestones

- [x] M1：记录 BDD 场景并用静态契约复现当前弹层状态不可控问题。
- [x] M2：最小化修复 Popover 与内部 Select 的状态边界。
- [x] M3：运行聚焦静态验证并记录 RED/GREEN 证据。

## Expected Verification

- `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过显式控制 Popover 可见状态和 Select 弹层归属，消除点击外部误判。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

### Element Plus 下拉选择门禁

- Trigger: `el-select` 位于 Popover 或 click-outside 容器中，选择后可能误触外层关闭。
- Preflight check: 确认下拉选项面板归属不会被外层判断为外部点击；必要时使用受控 `v-model:visible` 和 `:teleported="false"`。
- Blocker: 选择来源后外层 Popover 在确认前关闭，或确认成功后没有显式关闭。
- Verification: 静态合同断言选择后保持弹层可确认、成功后主动关闭。
- Evidence: `docs/e2e-rules.md#element-plus-下拉选择门禁`。

## Cleanup Keep

- doc/tasks/20260726-route-flow-copy-popover-stability/bug-regression-evidence.md
- doc/tasks/20260726-route-flow-copy-popover-stability/frontend-feature-evidence.md
