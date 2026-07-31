# Bug Regression Evidence

## Bug Summary

工艺路线右侧“动态表单列表”的“复制”弹层使用非受控 `el-popover trigger="click"`，内部来源工序 `el-select` 的下拉面板会脱离 Popover 容器，选择选项时容易被外层 click-outside 判断为外部点击而关闭；点击“复制到当前工序”成功后又没有显式关闭弹层。

## Expected Behavior

- 选择来源工序后，复制弹层保持打开，用户可以继续点击“复制到当前工序”。
- 复制成功后，表单绑定关系替换并同步草稿，成功提示出现，复制弹层明确关闭。

## Reproduction

- 在实现前运行 `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js`。
- 静态合同断言 `v-model:visible="processFormBindingCopyPopoverVisible"`、`:teleported="false"` 和成功后关闭逻辑。
- 旧源码缺少受控 Popover 状态，因此测试按预期失败。

## Root Cause

Popover 可见状态完全交给 Element Plus 自动触发管理，内部 `el-select` 未限制 `teleported`，导致选择事件与外层 click-outside 状态竞争；成功复制函数只清空来源工序并提示成功，没有同步关闭 Popover。

## Regression Test

- Updated: `IntRuoyiFronted/tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js`
- RED: `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js` -> FAIL, expected reason: missing controlled Popover state.
- GREEN: `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js` -> PASS.

## Verification

- `node tests/e2e/mes-route-flow-copy-process-form-bindings-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-flow-binding-border-static.spec.js` -> PASS.
- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js` -> PASS.

## Risk And Scope

风险集中在工艺路线配置右侧字段明细的动态表单整组复制入口。修复不改变复制数据结构、来源过滤、草稿同步或保存 API，仅收敛弹层状态边界。

## Blockers

当前工作区存在大量本任务外未提交改动，未执行提交/推送；本任务实现只做最小目标补丁并记录该限制。
