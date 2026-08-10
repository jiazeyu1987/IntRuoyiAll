# Verification Report

## Summary

- 0/null 分配数量：后端已按 0 跳过，不再抛“有效分配数量”错误。
- 手动满额分配：当前分配与请求一致时仍触发订单工序完成进度重算。
- FIFO 满额分配：当前分配与请求一致时仍触发订单工序完成进度重算。

## Commands

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，命中旧逻辑 `aggregateDesired` 对 null/0 抛错。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving+manualFullAllocationSameAsCurrentMustStillReconcileCompletionProgress+fifoFullAllocationSameAsCurrentMustStillReconcileCompletionProgress" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- REGRESSION: `mvn -q -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- STATIC: `rg -n --encoding utf-8 "分配数量必须为正整数|allocatedQuantity.*NotNull|NotNull.*allocatedQuantity|Positive.*allocatedQuantity|DecimalMin.*allocatedQuantity" IntRuoyiFronted\\src IntRuoyiBackend\\yudao-module-mes\\src\\main\\java IntRuoyiBackend\\yudao-module-mes\\src\\test\\java -g "*.vue" -g "*.ts" -g "*.java"` -> no matches。

## Remaining Risk

- 未启动本地前端或执行写入型 Playwright E2E，避免在当前任务外创建真实业务数据。后端服务层已覆盖分配确认的正式根因。
