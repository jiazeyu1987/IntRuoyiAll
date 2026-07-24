# Task: 恢复设备台账历史数据

## Goal

确认当前 `设备台账` 数据缺失的实际状态，并将用户之前可见的设备台账数据恢复到系统可用状态。

## Milestones

- [x] M1: 确认当前 `mes_dv_machinery` / `mes_dv_machinery_process` 实际数据状态。
- [x] M2: 确认可恢复来源与恢复阻塞项。
- [x] M3: 执行数据恢复。
- [x] M4: 用数据库与真实前端路径验证恢复结果。
- [x] M5: 更新任务文档并提交本次改动。
- [x] M6: 追查设备台账此前被清空的原因并形成结论。

## Locked Decisions

- 恢复目标采用上次已验收通过的最终版设备台账数据，即：
  `D:\ocr2\resource\球囊扩张导管工序(1).xlsx`
  对应的 `31` 台唯一设备、`83` 条工序明细。
- 当前库中设备表与设备工序明细表均为空，因此按“重建”而不是“差量修补”恢复。
- 为保证工序明细仍能按原规则关联工序，先补齐当前库中缺失的 16 个工序主数据，再恢复设备台账。

## Current Status

已完成。

## Expected Verification

- `mes_dv_machinery` 恢复为 `31` 条设备主数据。
- `mes_dv_machinery_process` 恢复为 `83` 条工序明细。
- `mes_dv_machinery_type` 至少恢复默认设备类型 `DEFAULT-MACHINERY-TYPE`。
- 真实前端设备台账页能够重新命中 `machinery page` 接口并返回 `200`。

## Final Verification

- 直接查库确认恢复前状态：
  `mes_dv_machinery = 0`
  `mes_dv_machinery_process = 0`
  `mes_dv_machinery_type = 0`
- 恢复脚本执行成功：
  [restore_from_final_excel.py](/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/doc/tasks/20260517-restore-machinery-ledger-data/restore_from_final_excel.py)
  输出：
  `ignored_placeholder_rows=4`
  `machinery_count=31`
  `process_detail_count=83`
  `created_process_count=16`
- 恢复后直接查库确认：
  `mes_dv_machinery = 31`
  `mes_dv_machinery_process = 83`
  `mes_dv_machinery_type = 1`
- 恢复后抽样确认：
  `A03190 -> 球囊成型机 / 吹球囊成型 / 9.523810`
  `A03196 -> 激光焊接机 / processName=null / standardHourlyCapacity=null`
  `A03190` 工序明细数 `2`
  `A03196` 工序明细数 `8`
- Playwright 真实页面路径确认：
  打开 `http://localhost:8081/mes/dv/machinery` 时，浏览器会话已重新命中
  `GET /admin-api/mes/dv/machinery/page?pageNo=1&pageSize=10`
  且返回 `200`。

## Notes

- 本次恢复同时补建了 16 个缺失的工序主数据，原因是当前工序主表不足以支撑原始设备工序明细恢复；如果不补齐，只能把明细降级成“无工序关联”。
- 原因结论：
  当前可确认的“清空”不是由我之前实现的 `最终版 Excel 同步` 逻辑直接造成。
  证据是：
  1. 该同步逻辑只会精确替换 `mes_dv_machinery` 和 `mes_dv_machinery_process`；
  2. 它不会清空 `mes_dv_machinery_type`，反而会在缺失时自动补建默认设备类型；
  3. 我恢复前现场却是 `mes_dv_machinery=0`、`mes_dv_machinery_process=0`、`mes_dv_machinery_type=0`。
  这说明后续发生过一次更大范围的数据库清表、重置或空库导入，至少影响了设备主表、明细表和设备类型表。
  受限于当前保留的 binlog 范围，只能确认最近一次对这三张表的写入是我本次恢复动作；现有证据不足以精确还原到底是哪一个人或哪一个外部脚本在更早时间执行了清空。
