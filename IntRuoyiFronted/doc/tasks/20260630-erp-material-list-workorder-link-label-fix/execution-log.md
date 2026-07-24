# Execution Log：生产用料清单主表生产工单列名与跳转修正（前端）

- `2026-06-30 任务创建`：建立前端任务文档，准备按严格 TDD 修正生产用料清单主表列名和点击跳转。
- `BDD: 生产用料清单主表显示生产工单链接 -> Given 某生产用料清单汇总行只对应一个生产工单 / When 打开生产用料清单主表 / Then 该列标题显示为生产工单，行内工单号可点击并跳到生产工单页对应条目。`
- `BDD: 多工单或无工单时不伪造精确跳转 -> Given 某汇总行对应多个工单或没有工单 / When 打开生产用料清单主表 / Then 页面显示摘要或无，但不伪造指向错误条目的 openId 跳转。`
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js -> FAIL`，断言命中主表缺少“生产工单”标题与 `handleOpenGroupWorkOrder` 链接入口。
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js -> PASS`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js -> PASS`
