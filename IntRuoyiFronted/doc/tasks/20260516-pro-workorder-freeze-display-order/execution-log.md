# Execution Log: 生产工单冻结编码高亮与排序调整

BDD: 冻结工单编码应使用红色显示 -> Given 生产工单列表中存在 `temporaryFrozen=true` 的工单 / When 页面渲染工单编码列 / Then 冻结工单编码应使用红色而非默认蓝色显示。

BDD: 非冻结工单应在同层级内优先显示 -> Given 生产工单列表同层级中同时存在冻结和非冻结工单 / When 页面加载树形列表 / Then 非冻结工单应排在冻结工单前面，同时保留原有树形结构。

RED: real page verification before the backend sort fix -> FAIL, the first page visible rows contained only frozen work orders, proving that frontend same-page sorting alone was insufficient because pagination had already been cut by the backend.

GREEN: `npx.cmd eslint --ext .vue src/views/mes/pro/workorder/index.vue` -> PASS.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-freeze-display-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-pro-workorder-freeze-display-order\scripts\verify-workorder-freeze-display.mjs` -> PASS, returned `{"url":"http://127.0.0.1:8081/mes/pro/work-order","firstFrozenIndex":2,"firstNormalCode":"881MO090863","firstNormalColor":"rgb(64, 158, 255)","firstFrozenCode":"881MO090756","firstFrozenColor":"rgb(245, 108, 108)","visibleRows":10}`.
