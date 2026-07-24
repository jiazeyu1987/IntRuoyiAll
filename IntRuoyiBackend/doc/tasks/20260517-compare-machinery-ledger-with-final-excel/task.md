# Task: 对比设备台账与最终版 Excel

## Goal

核对当前设备台账数据与 `D:\ocr2\resource\球囊扩张导管工序(1).xlsx` 是否一致，并输出主表与明细层的对比结果。

## Milestones

- [x] M1: 读取最终版 Excel 并整理唯一设备与明细行基准。
- [x] M2: 读取当前数据库中的设备台账主表与工序明细。
- [x] M3: 比对主表唯一设备集合与明细数据集合。
- [x] M4: 记录结论与验证证据。
- [x] M5: 提交本次任务文档。

## Current Status

已完成。

## Expected Verification

- 明确给出当前设备台账是否与最终版 Excel 一致。
- 若不一致，列出多出、缺失或字段不匹配的差异。

## Final Verification

- 对比脚本：
  [compare_with_final_excel.py](/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/doc/tasks/20260517-compare-machinery-ledger-with-final-excel/compare_with_final_excel.py)
- 对比结果：
  `ignored_placeholder_rows=4`
  `Excel unique_machinery_count=31`
  `Excel detail_count=83`
  `DB unique_machinery_count=31`
  `DB detail_count=83`
- 集合与明细结论：
  `machinery_code_set_equal=true`
  `machinery_row_equal=true`
  `detail_row_equal=true`
  `all_equal=true`
- 差异结果：
  `missing_codes=[]`
  `extra_codes=[]`
  `machinery_row_differences=[]`
  `detail_missing_sample=[]`
  `detail_extra_sample=[]`
