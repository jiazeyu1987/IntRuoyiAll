# 执行日志：20260630-erp-production-material-list-grouped-popup

BDD: 主表按单据汇总展示 -> Given 进入 ERP 生产用料清单页面 / When 查询分组列表 / Then 主表每个 sourceBillNo 只显示一行。
BDD: 点击单据号查看整单子项 -> Given 主表存在某个单据号 / When 点击单据号 / Then 弹窗展示该单据全部子项明细。

GREEN: `Get-Content -Encoding utf8` 读取经验门禁与根仓任务证据 -> PASS。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` 在脚本新增前不存在该验证入口 -> FAIL。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` -> PASS。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /erp/production/material-list --target-text 生产用料清单 --timeout 90000` -> PASS。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-readonly.e2e.js` -> PASS。
