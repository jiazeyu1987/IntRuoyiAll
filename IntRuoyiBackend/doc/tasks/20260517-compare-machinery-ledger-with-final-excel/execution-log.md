BDD: 当前设备台账与最终版 Excel 一致性核对 -> Given 最终版 Excel 和当前数据库设备台账 When 对主表唯一设备与工序明细逐项比对 Then 给出一致或差异结论，并列出可定位的差异项

GREEN: compare_with_final_excel.py -> PASS，Excel 有效唯一设备 `31` 条、有效明细 `83` 条；数据库唯一设备 `31` 条、明细 `83` 条

GREEN: 主表一致性 -> PASS，`machinery_code_set_equal=true` 且 `machinery_row_equal=true`

GREEN: 明细一致性 -> PASS，`detail_row_equal=true`

GREEN: 总体结论 -> PASS，`all_equal=true`，没有多余、缺失或字段不匹配项
