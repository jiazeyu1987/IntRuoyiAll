# Execution Log：生产订单与生产用料清单双向关联展示（前端）

- `2026-06-30 任务创建`：建立前端任务文档，准备按严格 TDD 实现双向关联展示与跳转。
- `BDD: 生产工单页显示生产用料清单入口 -> Given 某生产工单存在关联生产用料清单 / When 打开生产工单页 / Then 行内显示生产用料清单链接或摘要，点击后跳到生产用料清单页。`
- `BDD: 生产工单页对无关联数据明确留空 -> Given 某生产工单没有关联生产用料清单 / When 打开生产工单页 / Then 行内显示无或留空，不渲染错误链接。`
- `BDD: 生产用料清单页显示对应生产工单 -> Given 某生产用料清单存在关联生产工单 / When 打开生产用料清单主表或明细 / Then 页面显示生产工单编号链接，点击后跳到生产工单页并打开目标工单。`
- `BDD: 生产用料清单未映射时不伪造跳转 -> Given 某生产用料清单没有对应生产工单 / When 打开页面 / Then 页面显示无对应生产订单，不渲染假链接。`
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js -> FAIL`，缺少双向关联字段与跳转实现。
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js -> PASS`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js -> PASS`
- `GREEN: login-preflight -> PASS，node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/work-order --target-text 生产工单 --timeout 90000`
- `GREEN: login-preflight -> PASS，node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /erp/production/material-list --target-text 生产用料清单 --timeout 90000`
- `GREEN: 真实页面路由落点 -> PASS，node 内嵌 Playwright 已验证 /mes/pro/work-order?code=SMART-SCHED-20260630-RERUN9-MO&openId=925815 自动筛选并打开目标工单详情，/erp/production/material-list?productionOrderNo=SMART-SCHED-20260630-RERUN9-MO 自动回填生产订单筛选框。`
- `GREEN: 真实页面数据现状 -> PASS，当前测试租户本地页面已显示新列“生产用料清单/对应生产订单”，但真实接口扫描确认暂无可点击的双向关联样本，因此本轮只完成列渲染与路由落点验收，不伪造样本。`
