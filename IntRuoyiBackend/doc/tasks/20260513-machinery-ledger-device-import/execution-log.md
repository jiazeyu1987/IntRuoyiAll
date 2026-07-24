BDD: 设备主表与最终版唯一设备集合精确对齐 -> Given 最终版 Excel Sheet1 中的真实设备编码集合 When 执行最终版同步 Then `mes_dv_machinery` 只保留这些唯一设备编码且数量与 Excel 完全一致

BDD: 同一设备的多工序信息通过明细表承载 -> Given 同一设备编码在最终版 Excel 中对应多条工序记录 When 执行最终版同步 Then 主表只保留唯一设备，完整工序与产能逐行保存到 `mes_dv_machinery_process`

BDD: 工序明细展示产线而不是 Excel 行号 -> Given 最终版 Excel `Sheet1` 第一列代表产线分组值 When 同步设备工序明细 Then 明细表回填 `lineName`，供前端展示 `产线名称`

RED: 未新增独立失败命令 -> FAIL，当前轮为在既有最终版同步实现上的字段语义修正，未保留代码改动前的独立失败现场，因此不伪造 RED 结果

GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesDvMachineryFinalSheetSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS

GREEN: mvn -pl yudao-server -am "-Dmaven.test.skip=true" package -> PASS

GREEN: POST /admin-api/mes/dv/machinery/sync-final-sheet -> PASS，返回 `excelEffectiveRowCount=83`、`ignoredPlaceholderRowCount=0`、`machineryCount=31`、`processDetailCount=83`、`createdCount=0`、`updatedCount=31`、`deletedCount=0`

GREEN: GET /admin-api/mes/dv/machinery/page?pageNo=1&pageSize=10&code=A03190 -> PASS，返回 `total=1`，设备主表仍按唯一设备编码保留单条记录

GREEN: GET /admin-api/mes/dv/machinery-process/list-by-machinery?machineryId=41 -> PASS，`A03190` 明细返回两条产线记录，`lineName=球囊扩张导管` 与 `lineName=棘突球囊扩张导管`

GREEN: Playwright 搜索并打开 A03190 详情 -> PASS，工序明细表头显示 `产线名称`，且不再显示 `Excel源行号`
