# Execution Log: 电子批记录接入模拟填写入口

BDD: 表单模板区显示模拟填写入口 -> Given 用户在电子批记录页面已选中某个报表 / When 查看右侧表单模板操作区 / Then 操作区显示 模拟填写 按钮。
BDD: 从电子批记录页跳转现有模拟页 -> Given 用户在电子批记录页面点击 模拟填写 / When 跳转页面 / Then 系统进入现有 eDHR 模板模拟填写页，而不是新页面。
BDD: 现有模拟页支持 reportId 直达 -> Given 模拟填写页收到 reportId 查询参数 / When 页面加载 / Then 直接按该 reportId 加载模板规则、签名位、左侧模板内填写和右侧表单显示。
RED: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> FAIL，当前电子批记录右侧操作区还没有 `模拟填写` 入口契约。
GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS，电子批记录右侧操作区已提供 `模拟填写` 入口并跳转现有模拟填写路由。
GREEN: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS，现有模拟填写页已兼容 `reportId` 直达模式。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-simulate-entry\frontend-feature-evidence.md` -> PASS。
