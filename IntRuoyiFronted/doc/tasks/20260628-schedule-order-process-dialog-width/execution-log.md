# 执行日志：20260628-schedule-order-process-dialog-width

BDD: 工艺排产路线查看弹窗在桌面端完整展示右侧列 -> Given 用户在排产工单列表点击查看工艺排产路线 / When 弹窗渲染 7 个关键列 / Then 弹窗宽度必须足够让 预计结束 列在桌面视口内完整可见，不再被右侧裁掉。

BDD: 窄视口下工艺排产路线仍可横向查看全部列 -> Given 工艺排产路线弹窗列宽总和大于可用视口 / When 用户打开查看弹窗 / Then 弹窗内容区必须提供明确的横向滚动承载，不能直接裁掉最后一列。

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-route-progress-dialog-width-static.spec.js` -> FAIL，当前查看弹窗仍写死 `width="960px"`，新的静态契约已证明该宽度不足以稳定承载右侧 `预计结束` 列。

CHANGE: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\scheduleorder\index.vue` 将“工艺排产路线”查看弹窗改为 `width="min(1280px, calc(100vw - 32px))"`，并为表格增加 `schedule-order-pool__process-dialog-table` 横向滚动容器及 `:deep(.el-table) { min-width: 1080px; }` 约束；`tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js` 新增弹窗宽度与横向承载门禁，同时把既有 `mes-schedule-order-route-progress-columns-static.spec.js` 的弹窗截取改成最小片段匹配，避免多行属性或前置弹窗导致误报。

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-route-progress-dialog-width-static.spec.js` -> PASS

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-route-progress-columns-static.spec.js` -> PASS

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-route-progress-view-static.spec.js` -> PASS
