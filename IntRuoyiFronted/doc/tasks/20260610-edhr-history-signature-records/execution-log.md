# 执行记录

## BDD

- BDD: 历史工序显示电子签名明细 -> Given 历史批记录中某道工序已有 FORM_REVIEW/SUBMIT/APPROVE 签名, When 用户点击该工序, Then 页面显示电子签名记录表格和签名明细。
- BDD: 接口返回单表签名记录 -> Given 批次复盘接口返回 executionReviews, When 某张表单有电子签名, Then 对应 executionReview 携带该执行记录的 signatureRecords。
- BDD: 无签名不伪造记录 -> Given 某张表单没有电子签名, When 用户查看该工序, Then 页面显示暂无电子签名记录，不展示 mock 或默认成功记录。

## RED / GREEN / REGRESSION

- RED: `node tests/e2e/edhr-batch-history-static.spec.js` -> FAIL，历史批记录页面缺少“电子签名记录”区块。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionControllerTest test` -> FAIL，`ExecutionReview.getSignatureRecords()` 不存在。
- GREEN: `node tests/e2e/edhr-batch-history-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionControllerTest test` -> PASS。
- GREEN: Playwright 真实只读 E2E -> PASS，登录 `测试租户/aoteman`，打开历史批记录页，接口返回单表 `signatureRecords`，选中执行记录 `110` 后页面显示 4 条电子签名明细。
- REGRESSION: 已重启本机 `int_main` 后端并验证历史页仍只读，不触发填写、签名、审批或归档写入操作。
