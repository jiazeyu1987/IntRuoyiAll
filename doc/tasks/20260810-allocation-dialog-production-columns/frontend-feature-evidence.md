# Frontend Feature Evidence

## Feature Goal

分配报工弹框展示活跃订单要生产数量和生产系数，并扩大弹框宽度。

## Non-Goals

- 不修改 FIFO 自动分配算法。
- 不修改后端保存接口。
- 不新增分配提交字段。

## Entry Point

- Route/Page：生产组长工作台报工管理。
- Component：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- API type：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`

## Acceptance

- 弹框宽度从 `760px` 调整为约 30% 增量的 `988px`。
- 分配表格新增“要生产数量”列，读取当前行活跃订单的 `erpFixedQuantitySnapshot ?? quantity`。
- 分配表格新增“生产系数”列，读取当前行活跃订单正式响应中的 `productionCoefficient`。
- 分配确认请求仍只提交原分配字段，不把展示字段写入 allocations。

