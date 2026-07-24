# 执行日志：20260702-schedule-order-process-dialog-right-overflow

BDD: 工艺排产路线弹窗完整显示右侧预计结束列 -> Given 用户在排产工单列表打开工艺排产路线 / When 弹窗渲染当前 9 个工序汇总列 / Then 右侧预计结束列必须完整纳入弹窗可视或滚动区域，不被页面边界裁切。

BDD: 窄视口下工艺排产路线可横向查看全部列 -> Given 工艺排产路线列宽总和超过视口可用宽度 / When 用户打开弹窗 / Then 表格容器必须提供横向滚动，且主表最小宽度覆盖所有工序汇总列。
RED: `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js` -> FAIL，当前弹窗仍为旧响应式宽度，静态契约证明当前 9 个工序汇总列需要更大的可用宽度与更精确的主表最小宽度约束。

CHANGE: `src/views/mes/pro/scheduleorder/index.vue` 将“工艺排产路线”弹窗宽度调整为 `min(1360px, calc(100vw - 24px))`；主表新增 `schedule-order-pool__process-summary-table` 类；收紧工序编号、工序名称、产能、数量、状态、报工次数和时间列宽；将最小宽度约束限定到主表，避免展开报工明细表继承主表宽度。

GREEN: `node tests/e2e/mes-schedule-order-route-progress-dialog-width-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-schedule-order-route-progress-columns-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-schedule-order-route-progress-view-static.spec.js` -> PASS