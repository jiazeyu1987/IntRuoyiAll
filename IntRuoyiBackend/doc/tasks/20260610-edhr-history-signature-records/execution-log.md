# 执行记录

## BDD

- BDD: 接口返回单表签名记录 -> Given 批次复盘接口返回 executionReviews, When 某张表单有电子签名, Then 对应 executionReview 携带该执行记录的 signatureRecords。

## RED / GREEN / REGRESSION

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionControllerTest test` -> FAIL，`ExecutionReview.getSignatureRecords()` 不存在。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionControllerTest test` -> PASS。
- REGRESSION: Playwright 真实只读 E2E -> PASS，历史页通过接口返回的单表 `signatureRecords` 显示 4 条电子签名明细。
