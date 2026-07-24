# execution-log

BDD: 排产工单按钮归入排产工单页签 -> Given 用户在排产工单页面, When 查看排产工单页签, Then 同步工单、导出、手动重排、批量操作和显示字段都在该页签工具栏内, 页签外标题栏不再重复显示这些按钮。
BDD: 同步工单按钮归入同步工单页签 -> Given 用户切换到同步工单页签, When 查看待同步差异列表工具栏, Then 状态统计、重置、选中工单加入排产工单池和显示字段都在同步工单页签工具栏内。

RED: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> FAIL, 页签外标题栏仍渲染同步工单等全局按钮。
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS
BDD: 两个页签列表填满剩余区域且固定表头表尾 -> Given 用户打开排产工单或同步工单页签, When 页面高度大于当前列表内容高度, Then 列表区域填满页脚上方剩余空间, 表头和分页固定, 只有表体中间区域滚动。
RED: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> FAIL, 页面卡片未占满可视高度、同步工单仍使用固定 520 高度、不可排原因列仍限制宽度导致右侧空白。
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS
BDD: 同步工单统计删除并按钮右移 -> Given 用户切换到同步工单页签, When 查看筛选工具栏, Then 紫框里的可入池/已入池/警告/阻断统计不再渲染, 重置、选中工单加入排产工单池和显示字段位于筛选行右侧黄色位置。
RED: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> FAIL, 同步工单页签 actions 工具栏仍要求状态统计且按钮区仍可被强制换行。
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS
BDD: 删除顶部标题栏并将按钮放到紫框位置 -> Given 用户打开排产工单页面, When 查看页签栏与筛选工具栏, Then 红框顶部标题栏不再渲染, 同步工单、导出、手动重排和显示字段按钮位于筛选行右侧紫框位置。
RED: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> FAIL, 页面仍渲染 ContentWrap title 且排产工单 toolbar-actions 被强制换到筛选行下方。
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS
BDD: 排产工单按钮位于页签内容红框工具栏 -> Given 用户停留在排产工单页签, When 查看页签栏下方的筛选/操作区, Then 排产工单控制按钮位于筛选行下方的页签内容工具栏内, 不再和页面顶部标题区或筛选第一行混在一起。
RED: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> FAIL, 排产工单页签缺少独立 schedule template class 和 toolbar-actions 换行约束。
GREEN: node tests/e2e/mes-schedule-order-tab-controls-toolbar-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-tab-static.spec.js -> PASS
GREEN: node tests/e2e/mes-schedule-order-sync-without-promise-date-static.spec.js -> PASS
GREEN: pnpm.cmd ts:check:schedule -> PASS
