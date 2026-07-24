# Execution Log: 工作站全部调整到车间1

- 2026-05-22 14:11: 已建立任务文档，并将前一条单工序工作站补数任务显式标记为 `Blocked`。
- BDD: 全部工作站归属统一到车间1 -> Given 当前工作站数据分散在多个车间 / When 执行本次真实数据调整 / Then 所有未删除工作站的 `workshop_id` 都应为 `900011`。
- BDD: 已绑定产线的工作站保持引用一致 -> Given 当前存在工作站 `900050` 绑定产线 `900040` / When 工作站被调整到 `车间1` / Then 被引用的产线 `900040` 也应属于 `车间1`，避免车间-产线归属不一致。
- RED: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT workshop_id, COUNT(*) AS workstation_count FROM mes_md_workstation WHERE deleted = 0 GROUP BY workshop_id ORDER BY workshop_id;"` -> FAIL（目标未满足），结果为 `900010 -> 24`、`900011 -> 69`。
- RED: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT id, code, name, workshop_id, production_line_id FROM mes_md_workstation WHERE deleted = 0 AND production_line_id IS NOT NULL ORDER BY id; SELECT id, code, name, workshop_id FROM mes_md_production_line WHERE deleted = 0 ORDER BY id;"` -> FAIL（引用不一致风险存在），结果为 `900050 / AUTO-WS-01 -> workshop_id=900010, production_line_id=900040`，且 `900040 / AUTO-LINE-01 -> workshop_id=900010`。
- GREEN: SQL update applied -> PASS，执行：
  - `UPDATE mes_md_workstation SET workshop_id = 900011 ... WHERE deleted = 0 AND workshop_id <> 900011`
  - `UPDATE mes_md_production_line SET workshop_id = 900011 ... WHERE id IN (SELECT DISTINCT production_line_id ... )`
- GREEN: post-update SQL verification -> PASS，结果为 `900011 -> 93`，不再存在其他 `workshop_id`。
- GREEN: post-update API verification -> PASS，`GET /admin-api/mes/md-workstation/get?id=900050` 与 `id=900056` 均返回 `workshopId=900011`；其中 `900050` 仍绑定 `productionLineId=900040`。
