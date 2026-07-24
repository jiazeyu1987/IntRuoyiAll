# Execution Log: 同步球囊扩张压力泵工艺批记录路线数据

BDD: 压力泵批记录路线数据恢复 -> Given “球囊扩张压力泵”工艺路线存在批记录路线源数据 / When 执行同步 / Then 工序设置可重新显示对应批记录表单与填写人链接。
BDD: 同步范围受控 -> Given 数据库还有其他工艺路线 / When 执行同步 / Then 只影响“球囊扩张压力泵”相关路线、工序、批记录绑定。

GREEN: experience-preflight -> PASS，已读取 docs/powershell-memory.md、docs/experience-index.md 与 database-schema-delivery；本任务默认只操作本机数据库，不操作服务器或正式环境。

GREEN: sql-contract -> python -X utf8 -m pytest script/tests/test_mes_rt000006_batch_record_mapping_sql.py -q -> PASS，4 tests。
GREEN: readonly-preflight -> PASS，RT000006 / 球囊扩张压力泵已有 route_process=14、matched_reports=14、process_config=14、batch_record_bindings=14、permission_rules=42，缺口为 0。
GREEN: sync-execute -> PASS，已重新执行 sql/mysql/20260709_mes_rt000006_batch_record_mapping.sql。
GREEN: post-sync-readback -> PASS，RT000006 / 球囊扩张压力泵同步后仍为 route_process=14、matched_reports=14、process_config=14、batch_record_bindings=14、permission_rules=42，missing_process_config=0，missing_batch_binding=0。

GREEN: redbox-data-readback -> PASS，按工序设置红框列实际链路回查，RT000006 / 球囊扩张压力泵 14 行均可联出批记录表单、生产填写人、质量填写人、设备填写人，redbox_ready_rows=14。

RED: yellow-box-user-screenshot -> FAIL，用户截图显示测试租户工序设置黄框 4 列仍为“未配置”。只读排查确认 tenant=1 的 `route_id=922067 / RT000006 / 球囊扩张压力泵` 已完整，但测试租户 tenant=122 页面命中的是 `route_id=922060 / RT000006 / E2E-WORD-1783433099306`，该路线已有 `route_process_rows=14 / batch_configs=14 / batch_bindings=14 / matched_reports=14`，但 `enabled_fill_rules=0`，且 `pressure_pump_*_filler` 三个角色只存在 tenant=1。
GREEN: tenant122-pressure-pump-role-and-rule-sync -> PASS，使用 ASCII 稳定键 `system_role.code in (pressure_pump_production_filler, pressure_pump_quality_filler, pressure_pump_equipment_filler)` 与 `system_role_category.code=batch-record` 补齐 tenant=122 的 3 个压力泵填写员角色：生产 `id=910301`、设备 `id=910302`、质量 `id=910303`。
GREEN: tenant122-yellow-box-readback -> PASS，tenant=122 `route_id=922060 / RT000006` 回查结果为 `route_process_rows=14`、`batch_bindings=14`、`matched_reports=14`、`enabled_fill_rules=42`；14 道工序均可联出批记录表单、生产填写人、质量填写人、设备填写人。
