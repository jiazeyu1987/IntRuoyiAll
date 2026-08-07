SELECT wo.id AS work_order_id,
       wo.code AS work_order_code,
       wo.status AS work_order_status,
       wo.quantity,
       so.id AS schedule_order_id,
       so.code AS schedule_order_code,
       so.status AS schedule_status,
       so.route_id,
       so.route_version_id,
       sp.id AS schedule_process_id,
       sp.route_process_id,
       sp.process_id,
       CAST(sp.enabled AS UNSIGNED) AS enabled,
       sp.production_quantity_factor,
       sp.planned_quantity,
       sp.plan_date,
       wo.tenant_id
FROM mes_pro_work_order wo
JOIN mes_pro_schedule_order so
  ON so.work_order_id = wo.id AND so.tenant_id = wo.tenant_id AND so.deleted = b'0'
JOIN mes_pro_schedule_order_process sp
  ON sp.schedule_order_id = so.id AND sp.tenant_id = so.tenant_id AND sp.deleted = b'0'
WHERE wo.tenant_id = 1
  AND wo.deleted = b'0'
  AND wo.code LIKE 'CODX-AO5-20260807-%' COLLATE utf8mb4_unicode_ci
ORDER BY wo.code;

SELECT r.id AS regulation_id,
       r.regulation_code,
       r.product_id,
       r.route_id,
       r.route_version_id,
       r.route_process_id,
       r.process_id,
       r.lifecycle_status,
       r.current_version_id,
       v.lifecycle_status AS version_status,
       CAST(v.final_inspection_applicable AS UNSIGNED) AS final_inspection_applicable,
       i.inspection_type,
       i.item_code,
       i.first_inspection_quantity,
       i.patrol_inspection_ratio
FROM mes_qa_inspection_regulation r
JOIN mes_qa_inspection_regulation_version v
  ON v.id = r.current_version_id AND v.tenant_id = r.tenant_id AND v.deleted = b'0'
JOIN mes_qa_inspection_regulation_item i
  ON i.regulation_version_id = v.id AND i.tenant_id = v.tenant_id AND i.deleted = b'0'
WHERE r.tenant_id = 1
  AND r.deleted = b'0'
  AND r.regulation_code = 'CODX-AO5-QA-20260807' COLLATE utf8mb4_unicode_ci
ORDER BY i.inspection_type;

SELECT COUNT(*) AS active_order_count
FROM mes_pro_process_pool_active_order ao
JOIN mes_pro_work_order wo
  ON wo.id = ao.work_order_id AND wo.tenant_id = ao.tenant_id AND wo.deleted = b'0'
WHERE ao.tenant_id = 1
  AND ao.deleted = b'0'
  AND ao.active_status = 'ACTIVE'
  AND wo.code LIKE 'CODX-AO5-20260807-%' COLLATE utf8mb4_unicode_ci;
