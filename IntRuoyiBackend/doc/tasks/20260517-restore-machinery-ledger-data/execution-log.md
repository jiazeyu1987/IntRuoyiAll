BDD: 设备台账恢复到上次验收通过的最终版 -> Given 当前设备台账主表和工序明细表为空 When 用最终版 Excel 执行恢复 Then 主表恢复为 31 台设备，工序明细恢复为 83 条

BDD: 工序明细恢复时保持工序关联 -> Given 当前工序主数据缺失部分名称 When 恢复设备工序明细 Then 先补齐缺失工序主数据，再恢复设备台账，避免把明细降级成无工序关联数据

RED: restore_from_final_excel.py -> FAIL，首次执行因当前库中缺失 16 个工序主数据，无法按原规则恢复 `process_id`

GREEN: restore_from_final_excel.py -> PASS，补齐缺失工序主数据后恢复成功，输出 `machinery_count=31`、`process_detail_count=83`、`created_process_count=16`

GREEN: 直接查库回归 -> PASS，`mes_dv_machinery=31`、`mes_dv_machinery_process=83`、`mes_dv_machinery_type=1`

GREEN: 明细抽样回归 -> PASS，`A03190` 工序明细数 `2`，`A03196` 工序明细数 `8`

GREEN: Playwright 真实页面路径 -> PASS，设备台账页面重新命中 `GET /admin-api/mes/dv/machinery/page?pageNo=1&pageSize=10`，请求状态为 `200`

GREEN: 原因排查 -> PASS，确认“清空”不是 `最终版 Excel 同步` 逻辑本身直接造成；恢复前连 `mes_dv_machinery_type` 也为空，而同步逻辑并不会删除设备类型，说明后续发生过更大范围的数据库清表、重置或空库导入
