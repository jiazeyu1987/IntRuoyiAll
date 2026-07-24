# 执行记录

BDD: 模板内显示复核签名 -> Given formViewModel.signatureCellMarkers 包含 FORM_REVIEW 签名位, When 历史页渲染该工序模板, Then 对应单元格显示签名人和签名时间。

BDD: 模板内显示审批签名 -> Given signatureRecords 包含 APPROVE 签名, When 点击历史批记录工序, Then 模板 APPROVE 单元格显示审批签名。

BDD: 外部签名表不作为主视图 -> Given 用户查看历史批记录, When 打开已归档批次, Then 签名主视觉位于模板单元格内，不默认展示模板外明细表。

RED: `node tests/e2e/edhr-inline-signature-cells-static.spec.js` -> FAIL, 报表 API 缺少签名单元格 marker 类型和读写接口。

GREEN: `node tests/e2e/edhr-inline-signature-cells-static.spec.js` -> PASS，模板配置入口、只读组件签名位渲染契约通过。

GREEN: `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS，历史批记录入口和模板组件复用未回退。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，前端类型检查通过。

REGRESSION: Playwright 真实页面验证 -> PASS，测试租户配置签名位后，历史批记录模板单元格内显示 `芋道1\n2026-06-10 02:11`，模板外签名主表数量为 0。
