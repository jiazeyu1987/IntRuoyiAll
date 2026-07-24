# Task: 对齐设备台账到最终版 Excel

## Goal

以 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx` 的 `Sheet1` 为唯一事实来源，对齐设备台账主表与设备工序明细，并将工序明细中的展示语义从 `Excel源行号` 调整为 `产线名称`。

## Milestones

- [x] M1: 确认最终版 Excel 结构、唯一设备规则和主表对齐边界。
- [x] M2: 实现最终版同步服务、设备工序明细表和按设备查询明细接口。
- [x] M3: 完成真实同步，保证主表唯一设备编码与最终版 Excel 完全一致。
- [x] M4: 将工序明细中的 `Excel源行号` 替换为 `产线名称`，并从 `Sheet1` 第一列分组值回填。
- [x] M5: 完成后端测试、真实接口回查和页面联调验证。
- [x] M6: 整理任务文档并提交本次任务改动。

## Locked Decisions

- 设备主表按唯一 `设备编码` 建档，不按 Excel 行级重复建设备。
- `设备编码 = /` 视为占位符，不进入主表，也不进入明细表。
- `设备标准小时产能 = 10.5小时日产能 / 10.5 / 设备数量`。
- 同一设备若对应多个不同工序或多个不同小时产能，则主表 `processName` 和 `standardHourlyCapacity` 置空，完整信息保留在明细表。
- 工序明细中的 `产线名称` 取自最终版 Excel `Sheet1` 第一列分组值；当前文件中该列标题为 `产品名称`。
- 默认车间固定为 `AUTO-WSHOP / AutoScheduleWorkshop`。
- 默认设备类型固定为 `DEFAULT-MACHINERY-TYPE / 默认设备类型`；不存在时自动创建。
- 默认设备状态固定为 `2`（生产中）。

## Current Status

已完成。

## Expected Verification

- 最终版同步接口成功执行。
- 设备主表唯一设备编码集合与最终版 Excel 完全一致，不多不少。
- 工序明细接口返回 `产线名称`，不再只依赖 `sourceRowNo`。
- 前端详情页工序明细列头显示 `产线名称`，不再显示 `Excel源行号`。

## Final Verification

- 后端定向单测通过：
  `mvn -pl yudao-module-mes -am "-Dtest=MesDvMachineryFinalSheetSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 后端打包通过：
  `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`
- 真实同步接口通过：
  `POST /admin-api/mes/dv/machinery/sync-final-sheet`
  返回：
  `excelEffectiveRowCount=83`
  `ignoredPlaceholderRowCount=0`
  `machineryCount=31`
  `processDetailCount=83`
  `createdCount=0`
  `updatedCount=31`
  `deletedCount=0`
- 主表接口回查通过：
  `GET /admin-api/mes/dv/machinery/page?pageNo=1&pageSize=10&code=A03190`
  返回 `total=1`，设备 `A03190` 主表仍为唯一设备。
- 明细接口回查通过：
  `GET /admin-api/mes/dv/machinery-process/list-by-machinery?machineryId=41`
  返回两条 `A03190` 明细，`lineName` 分别为 `球囊扩张导管`、`棘突球囊扩张导管`。
- Playwright 页面联调通过：
  `http://localhost:8081/mes/dv/machinery`
  搜索并打开 `A03190` 详情，工序明细列头显示 `产线名称`，且不再显示 `Excel源行号`。
- 页面截图：
  [machinery-line-name-check.png](/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/doc/tasks/20260513-machinery-ledger-device-import/machinery-line-name-check.png)
