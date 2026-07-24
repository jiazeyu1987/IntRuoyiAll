# 生产工单列表管理员操作与筛选项清理执行日志

BDD: 红框操作仅管理员可见 -> Given 用户打开生产工单列表 / When 用户不是管理员 / Then 导出、增量同步、全部展开、全部折叠、创建 ERP 测试单不可见；管理员仍可见这些操作。

BDD: 蓝框筛选项删除 -> Given 用户打开生产工单列表 / When 查看查询区 / Then 工单名称、客户、产品、工单类型筛选项不再显示。

BDD: 查询按钮移动到首行右侧 -> Given 用户打开生产工单列表 / When 查看查询区 / Then 查询、重置及管理员操作按钮显示在筛选首行右侧空白区域，不再单独占用下一行左侧。

BDD: 表格少量行不保留底部空白 -> Given 用户打开生产工单列表且分页为 10 条 / When 当前页只显示 10 条数据 / Then 表格高度随内容自然收起，分页直接跟随表格内容，不出现大块空白。

RED: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> FAIL，当前页面仍显示蓝框内“工单名称 / 产品 / 客户 / 工单类型”筛选项，且红框操作未统一限制为管理员可见。

RED: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> FAIL，当前查询表单缺少 `work-order-query-form` 与 `work-order-query-actions` 布局约束，查询按钮组仍单独占用下一行左侧。

RED: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> FAIL，当前生产工单表格仍使用 `:height="workOrderTableHeight"` 固定高度，10 条数据时底部会保留大块空白区域。

GREEN: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> PASS，生产工单表格已改为 `:max-height="workOrderTableMaxHeight"`，10 条数据时高度随内容自然收起，避免底部大块空白。

GREEN: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> PASS，查询表单已使用 flex 换行布局，查询按钮组通过 `margin-left: auto` 移动到筛选首行右侧空白区域。

GREEN: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js` -> PASS，蓝框筛选项已删除，导出、增量同步、展开/折叠、创建 ERP 测试单均由管理员可见性控制。

GREEN: `node tests/e2e/workorder-create-erp-order-static.spec.js` -> PASS，创建 ERP 测试单操作仍保留权限、行级 loading 和确认提示，并新增管理员可见性约束。

GREEN: `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS，重点列展示与复制契约未回归。

GREEN: `node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，产品名称/产品编码候选过滤契约未回归。

GREEN: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js; node tests/e2e/workorder-create-erp-order-static.spec.js; node tests/e2e/workorder-key-columns-static.spec.js; node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，生产工单查询区布局、管理员操作、重点列与产品候选过滤静态契约均通过。

GREEN: `node tests/e2e/workorder-admin-actions-filter-cleanup-static.spec.js; node tests/e2e/workorder-create-erp-order-static.spec.js; node tests/e2e/workorder-key-columns-static.spec.js; node tests/e2e/workorder-product-candidate-filters-static.spec.js` -> PASS，生产工单表格自适应高度、查询区布局、管理员操作、重点列与产品候选过滤静态契约均通过。

BLOCKER: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> FAIL，阻塞于无关已修改文件 `src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue` 的 `EdhrBatchExecutionReviewTaskEvent.closedAt` 类型错误；本次生产工单目标静态验证已通过。
