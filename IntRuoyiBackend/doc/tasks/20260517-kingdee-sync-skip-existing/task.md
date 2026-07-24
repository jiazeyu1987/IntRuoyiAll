# Task: 金蝶生产工单同步遇到重复编码时跳过继续

## Goal
当同步金蝶生产订单时，如果工单编码已经存在于本地 MES 工单表中，接口应跳过该条并继续处理后续订单，不应因重复编码直接失败。

## Milestones
- [x] M1: 复现当前同步在重复工单编码上中断的行为。
- [x] M2: 增加回归测试，覆盖“已存在则跳过，继续后续同步”。
- [x] M3: 最小化修改同步逻辑，避免重复编码抛错终止整批同步。
- [x] M4: 真实接口回归验证同步结果包含跳过计数且不中断。

## Verification
- `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest test`
- `mvn -pl yudao-server -am -DskipTests package`
- `POST /admin-api/mes/pro/work-order/sync-kingdee`

## Status
- Completed.

## Completed Work
- 在 `MesKingdeeProductionOrderSyncServiceImpl` 中增加了本批次已处理工单编码集合，并在创建前先跳过已存在的本地工单编码。
- 新增两条回归测试，覆盖“本地已存在编码跳过”与“同批次重复 `billNo` 跳过”。
- 通过真实接口验证同步不再因重复编码中断，返回 `createdCount=806`、`skippedCount=194`。

## Final Verification
- `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest test` -> PASS
- `POST /admin-api/mes/pro/work-order/sync-kingdee` -> PASS, response `code=0`
