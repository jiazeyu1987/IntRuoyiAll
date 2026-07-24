DELETE ext
FROM mes_pro_task_schedule_ext ext
JOIN mes_pro_task task ON task.id = ext.task_id
WHERE task.work_order_id IN (900080, 900082);

DELETE dep
FROM mes_pro_task_dependency dep
WHERE dep.source_task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t)
   OR dep.target_task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t);

DELETE issue
FROM mes_pro_schedule_issue issue
WHERE issue.work_order_id IN (900080, 900082)
   OR issue.task_id IN (SELECT id FROM (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082)) t);

DELETE FROM mes_pro_task WHERE work_order_id IN (900080, 900082);
DELETE FROM mes_pro_work_order_bom WHERE id IN (900081, 900083) OR work_order_id IN (900080, 900082);
DELETE FROM mes_pro_work_order WHERE id IN (900080, 900082);
DELETE FROM mes_pro_capacity_plan WHERE id IN (900060, 900061, 900062) OR line_id IN (900040, 900041);
DELETE FROM mes_pro_capacity_actual WHERE line_id IN (900040, 900041);
DELETE FROM mes_md_workstation WHERE id IN (900050, 900051);
DELETE FROM mes_md_production_line WHERE id IN (900040, 900041);
DELETE FROM mes_cal_plan_shift WHERE id = 900031 OR plan_id = 900030;
DELETE FROM mes_cal_plan WHERE id = 900030;
DELETE FROM mes_pro_route_process WHERE id IN (900022, 900023) OR route_id = 900020;
DELETE FROM mes_pro_route_product WHERE id = 900021 OR route_id = 900020;
DELETE FROM mes_pro_route WHERE id = 900020;
DELETE FROM mes_pro_process WHERE id IN (900300, 900301);
DELETE FROM mes_md_workshop WHERE id = 900010;
DELETE FROM mes_wm_material_stock WHERE id = 900090 OR item_id = 900200;
DELETE FROM mes_md_item WHERE id IN (900100, 900200);
DELETE FROM mes_md_unit_measure WHERE id = 900001;
DELETE FROM mes_md_auto_code_record WHERE rule_id = 900070;
DELETE FROM mes_md_auto_code_part WHERE id IN (900071, 900072) OR rule_id = 900070;
DELETE FROM mes_md_auto_code_rule WHERE id = 900070;
