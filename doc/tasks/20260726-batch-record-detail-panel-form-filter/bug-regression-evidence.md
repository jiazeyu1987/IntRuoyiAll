# Bug Regression Evidence

## Bug Summary

批记录表单配置页右侧“字段明细”面板在选中“批记录表单”字段时，会把“过程检验记录”等非批记录表单显示在红框区域。

## Expected Behavior

选中“批记录表单”字段时，字段值和可点击链接只展示明确归属 `MAIN` 槽位的批记录表单；其它路线表单、过程检验表单、损耗单或参数记录表不得显示，也不得让节点边框误判为批记录已绑定。

## Reproduction

- Path: 工艺路线批记录配置画布 -> 选中工序 -> 左侧配置项点击“批记录表单” -> 观察右侧“字段明细”。
- RED command: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`。

## Root Cause

右侧明细复用 `normalizeRecordBindingSlotType` 做槽位过滤；该函数在缺少显式 `formSlotType` 或 `formBindingKey` 槽位时默认返回 `MAIN`，导致非批记录表单被归入“批记录表单”字段。

## Regression Test

新增 `IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js`，锁定：

- 右侧明细使用非 fallback 的 `resolveRecordBindingSlotType`。
- `getRecordBindingsBySlotType` / `getLegacyBatchRecordsBySlotType` 只纳入显式槽位匹配项。
- 节点绑定状态也使用显式槽位匹配，避免非批记录表单把节点标成已绑定。

## RED / GREEN

- RED: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> FAIL, 缺少非 fallback 槽位解析器。
- GREEN: `node tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS。

## Verification

- `node tests/e2e/mes-route-flow-clickable-detail-values-static.spec.js` -> PASS。
- `node tests/e2e/mes-route-flow-batch-record-panel-visible-static.spec.js` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue IntRuoyiFronted/tests/e2e/mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS，只有 CRLF working-copy warning。

## Risk And Scope

修复范围限定在路线流转图右侧字段明细和节点绑定状态的槽位过滤；不修改表单编辑器默认创建逻辑、不修改保存 API、不引入 fallback。

## Blockers

并发改动导致既有 `mes-route-flow-legacy-batch-record-detail-static.spec.js` 在保存载荷断言失败，本任务未修改该载荷逻辑。
