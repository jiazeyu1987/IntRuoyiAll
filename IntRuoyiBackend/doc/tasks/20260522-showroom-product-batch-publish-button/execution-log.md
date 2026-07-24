# Execution Log: 20260522-showroom-product-batch-publish-button

BDD: 批量发布只尝试可直发产品并暴露失败原因 -> Given 当前筛选结果里同时存在 `DRAFT`、`REJECTED`、`PUBLISHED`、`APPROVED` 四类产品 / When 后端执行 `batchPublishProducts` / Then 只应对可直发的 `DRAFT` 与 `REJECTED` 发起发布尝试，并把失败产品的真实原因写入汇总返回。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomApiRuntimeBatchPublishTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 任务开始前仓库中缺少 `ShowroomApiRuntimeBatchPublishTest`，无法锁定“只直发可发布行并暴露失败原因”的回归行为。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomApiRuntimeBatchPublishTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
