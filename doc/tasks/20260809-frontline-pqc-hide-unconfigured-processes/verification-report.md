# 一线 PQC 正式 QA 工序修复验证报告

## Result

PASS（本任务范围）。球囊扩张压力泵的历史 `CODX_QA` 规程不再被一线 PQC 当作正式数据，目标订单不再展示错误的“粗洗工序”。

## Code Verification

- `MesQaInspectionRegulationDO` 统一声明正式 owner `MES_QA`。
- 发布规程查询、按工序查询和按产品查询均限定 `owner_module=MES_QA`。
- 一线 PQC 构建上下文时再校验 owner，非正式记录快速失败。
- Maven 命令：`mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest,MesQaInspectionRegulationServiceTest" test`。
- 结果：47 tests，0 failures，0 errors，0 skipped，`BUILD SUCCESS`。

## Data Verification

- 精确目标：tenant `1`、product `902149`、route `922119`、routeVersion `627`、creator `20260808-pressure-pump-active-orders`。
- 退役规程：`CODX_QA / RETIRED = 14`。
- 取消未执行任务：`CANCELLED = 112`。
- activeOrder `48/49` 剩余 `PENDING = 0`。
- 未删除任何规程、版本或任务；未修改生产工艺路线。

## Real-Path Verification

- Playwright 1.60 通过真实前端登录访问 `/mes/pro/feedback/edhr-batch-pqc-fill`。
- 活跃订单 API 与订单选择器中均无 `881MO090889`，当前其他可执行订单数为 5。
- 页面无 JavaScript 错误，无检验提交类业务写入；仅页面初始化自动写入一次当前员工上下文。

## Residual Note

- 球囊扩张压力泵当前 QA 规程页是 `DRAFT`。按正式数据契约，在 QA 规程发布并生成新待检任务前，目标订单不应出现于一线 PQC。
- 工作区已有静态回归 `mes-pqc-task-generation-static.spec.cjs` 因缺少 `SHIFT_AM="AM"` 断言失败；与本任务的 QA owner 修复无关，未修改并行任务范围。
