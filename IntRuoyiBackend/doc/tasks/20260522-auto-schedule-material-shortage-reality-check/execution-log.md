# Execution Log: 自动排产物料短缺真实性核对

- 2026-05-22 14:23: 已建立分析任务文档。
- BDD: 物料短缺 warning 与真实库存现状一致 -> Given 当前预览仅剩 `MATERIAL` warning / When 对比 warning 的 `availableQty` 与 `mes_wm_material_stock` 现状 / Then 能判断这些 warning 是否源于真实零库存而不是排产逻辑误报。
- GREEN: `POST /admin-api/mes/pro/auto-schedule/preview` with `workOrderIds=[903200,903245]` -> PASS，结果 `blockingIssueCount=0`、`shortageCount=54`。
- GREEN: warning payload inspection -> PASS，本次 `54` 条 warning 的 `availableQty` 全部为 `0`。
- GREEN: `SELECT item_id, SUM(quantity) AS stock_qty FROM mes_wm_material_stock WHERE deleted = 0 GROUP BY item_id HAVING SUM(quantity) > 0 ORDER BY stock_qty DESC LIMIT 40` -> PASS，当前仅 `item_id=900200` 存在正库存 `10`，不在本次短缺清单中。
- Conclusion: 剩余 `54` 条 warning 与当前本地库存现状一致，更接近真实零库存/库存数据缺失，而不是排产算法误报。
