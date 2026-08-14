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
- E2E SCRIPT CHECK: `node --check tests\\e2e\\team-leader-workbench-real-flow.e2e.js` -> PASS。
- REAL E2E: `TLW_FRONTEND_URL=http://127.0.0.1:8081 TLW_BACKEND_URL=http://127.0.0.1:48081 pnpm e2e:team-leader-workbench:real` -> BLOCKED，缺少真实写入型 E2E 前置条件；结果文件：`IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json`。
- REAL E2E RERUN: 2026-08-10T05:15:04Z 复跑仍为 BLOCKED；脚本未进入写入业务路径，未使用默认 admin 基线数据或 API-only 替代真实 E2E。

## Remaining Risk

- 本机前端 8081 和后端 48081 可用，但真实写入型 Playwright E2E 缺少任务自有 `TLW_*` 测试租户、账号、工单、工序、设备、记录本和电子签名夹具。按项目规则，不能用默认 admin 基线数据、mock、API-only 或非任务自有业务记录替代成功 E2E。
