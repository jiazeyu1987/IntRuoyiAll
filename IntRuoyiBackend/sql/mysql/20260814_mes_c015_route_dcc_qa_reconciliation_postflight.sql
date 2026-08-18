-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_schema; type=postflight; riskLevel=high
-- C015 exact target-state verification. Rerunning this script performs no DDL.

DROP PROCEDURE IF EXISTS postflight_mes_c015_route_dcc_qa_reconciliation;
DELIMITER $$
CREATE PROCEDURE postflight_mes_c015_route_dcc_qa_reconciliation()
BEGIN
  DECLARE v_blocker_count bigint DEFAULT 0;
  DECLARE v_required_table_count int DEFAULT 0;

  DROP TEMPORARY TABLE IF EXISTS c015_reconciliation_postflight;
  CREATE TEMPORARY TABLE c015_reconciliation_postflight (
    check_name varchar(96) NOT NULL,
    affected_row_count bigint NOT NULL,
    blocker_reason varchar(512) NOT NULL
  );

  SELECT COUNT(1)
    INTO v_required_table_count
    FROM information_schema.tables
   WHERE table_schema = DATABASE()
     AND table_name IN ('mes_md_item', 'mes_pro_route', 'mes_pro_route_version',
                        'mes_pro_route_product', 'dcc_project_code',
                        'mes_pro_route_dcc_project_binding', 'mes_qa_inspection_regulation',
                        'mes_qa_inspection_regulation_version', 'mes_qa_inspection_regulation_process',
                        'mes_qa_inspection_regulation_item', 'mes_pro_process_pool_active_order',
                        'mes_pro_process_pool_active_order_process_snapshot', 'mes_pqc_inspection_task');
  IF v_required_table_count <> 13 THEN
    INSERT INTO c015_reconciliation_postflight
    VALUES ('required_tables', 13 - v_required_table_count,
            'C015 required MES/DCC/QA/PQC base tables are missing');
    SELECT * FROM c015_reconciliation_postflight ORDER BY check_name;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 postflight failed: required base tables are missing';
  END IF;

  INSERT INTO c015_reconciliation_postflight
  SELECT 'item_product_master_column', 1, 'mes_md_item.product_master_id exact column is missing'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_md_item'
        AND column_name = 'product_master_id' AND data_type = 'bigint'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'route_version_column', 1, 'route-DCC version must be BIGINT NOT NULL'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
        AND column_name = 'version' AND data_type = 'bigint' AND is_nullable = 'NO'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'route_generated_column', 1, 'route-DCC active_route_id must be a stored generated column'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
        AND column_name = 'active_route_id' AND extra LIKE '%STORED GENERATED%'
        AND LOWER(generation_expression) LIKE '%case%route_id%'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'route_forbidden_columns', COUNT(1), 'binding_status and route_version_id are forbidden'
    FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
     AND column_name IN ('binding_status', 'route_version_id')
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_postflight
  SELECT 'route_current_unique', 1, 'route current unique index signature is missing or invalid'
   WHERE NOT EXISTS (
     SELECT 1 FROM (
       SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
         FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
          AND index_name = 'uk_mes_pro_route_dcc_current'
        GROUP BY non_unique
     ) route_current
     WHERE non_unique = 0 AND columns_in_order = 'tenant_id,active_route_id'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'route_history_unique', 1, 'route history version unique index signature is missing or invalid'
   WHERE NOT EXISTS (
     SELECT 1 FROM (
       SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
         FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_dcc_project_binding'
          AND index_name = 'uk_mes_pro_route_dcc_history_version'
        GROUP BY non_unique
     ) route_history
     WHERE non_unique = 0 AND columns_in_order = 'tenant_id,route_id,version'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'qa_dcc_column', 1, 'QA dcc_project_code_id must be BIGINT NOT NULL'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
        AND column_name = 'dcc_project_code_id' AND data_type = 'bigint' AND is_nullable = 'NO'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'qa_generated_column', 1, 'QA active DCC identity must be a stored generated column'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
        AND column_name = 'active_dcc_project_code_id' AND extra LIKE '%STORED GENERATED%'
        AND LOWER(generation_expression) LIKE '%case%dcc_project_code_id%'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'qa_active_unique', 1, 'QA active DCC unique index signature is missing or invalid'
   WHERE NOT EXISTS (
     SELECT 1 FROM (
       SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
         FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
          AND index_name = 'uk_mes_qa_regulation_active_dcc'
        GROUP BY non_unique
     ) qa_active
     WHERE non_unique = 0 AND columns_in_order = 'tenant_id,active_dcc_project_code_id'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'qa_legacy_unique', 1, 'legacy QA DCC deleted-flag unique index must be removed'
   WHERE EXISTS (
     SELECT 1 FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'mes_qa_inspection_regulation'
        AND index_name = 'uk_mes_qa_regulation_dcc_project'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'pqc_rule_key_column', 1, 'inspection_rule_key must be varchar(32) NOT NULL'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'mes_pqc_inspection_task'
        AND column_name = 'inspection_rule_key' AND column_type = 'varchar(32)' AND is_nullable = 'NO'
   );

  INSERT INTO c015_reconciliation_postflight
  SELECT 'pqc_rule_key_domain', COUNT(1), 'inspection_rule_key is outside FIRST/PATROL_AM/PATROL_PM/FINAL'
    FROM mes_pqc_inspection_task
   WHERE inspection_rule_key NOT IN ('FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL')
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_postflight
  SELECT 'removed_active_order_route_identity', COUNT(1),
         'REMOVED active orders must retain exact frozen route/version process graphs'
    FROM (
      SELECT active_order.id
        FROM mes_pro_process_pool_active_order active_order
        LEFT JOIN mes_pro_route route
          ON route.id = active_order.route_id
         AND route.tenant_id = active_order.tenant_id
         AND route.deleted = b'0'
        LEFT JOIN mes_pro_route_version route_version
          ON route_version.id = active_order.route_version_id
         AND route_version.tenant_id = active_order.tenant_id
         AND route_version.deleted = b'0'
         AND route_version.route_id = active_order.route_id
       WHERE active_order.deleted = b'0'
         AND active_order.active_status = 'REMOVED'
         AND (active_order.route_id IS NULL
           OR active_order.route_version_id IS NULL
           OR route.id IS NULL
           OR route_version.id IS NULL
           OR CASE
                WHEN JSON_VALID(route_version.route_snapshot_json) = 1
                  THEN COALESCE(JSON_LENGTH(JSON_EXTRACT(route_version.route_snapshot_json,
                         '$.configSnapshots.flowGraph.nodes')), 0)
                ELSE 0
              END = 0
           OR EXISTS (
             SELECT 1
               FROM JSON_TABLE(
                 CASE WHEN JSON_VALID(route_version.route_snapshot_json) = 1
                        THEN route_version.route_snapshot_json ELSE JSON_OBJECT() END,
                 '$.configSnapshots.flowGraph.nodes[*]'
                 COLUMNS (
                   route_process_id bigint PATH '$.routeProcessId',
                   process_id bigint PATH '$.processId'
                 )
               ) route_node
              GROUP BY route_node.route_process_id
             HAVING route_node.route_process_id IS NULL
                 OR COUNT(1) <> 1
                 OR COUNT(DISTINCT route_node.process_id) <> 1
           ))
    ) removed_route_blocker
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_postflight
  SELECT 'removed_active_order_process_snapshot_identity', COUNT(1),
         'REMOVED active-order process snapshots must exactly match every frozen route-version process'
    FROM (
      SELECT active_order.id
        FROM mes_pro_process_pool_active_order active_order
        JOIN mes_pro_route_version route_version
          ON route_version.id = active_order.route_version_id
         AND route_version.tenant_id = active_order.tenant_id
         AND route_version.deleted = b'0'
         AND route_version.route_id = active_order.route_id
       WHERE active_order.deleted = b'0'
         AND active_order.active_status = 'REMOVED'
         AND (
           NOT EXISTS (
             SELECT 1
               FROM mes_pro_process_pool_active_order_process_snapshot process_snapshot
              WHERE process_snapshot.tenant_id = active_order.tenant_id
                AND process_snapshot.active_order_id = active_order.id
                AND process_snapshot.deleted = b'0'
           )
           OR EXISTS (
             SELECT 1
               FROM mes_pro_process_pool_active_order_process_snapshot process_snapshot
               LEFT JOIN JSON_TABLE(
                 CASE WHEN JSON_VALID(route_version.route_snapshot_json) = 1
                        THEN route_version.route_snapshot_json ELSE JSON_OBJECT() END,
                 '$.configSnapshots.flowGraph.nodes[*]'
                 COLUMNS (
                   route_process_id bigint PATH '$.routeProcessId',
                   process_id bigint PATH '$.processId'
                 )
               ) route_node
                 ON route_node.route_process_id = process_snapshot.route_process_id
                AND route_node.process_id = process_snapshot.process_id
              WHERE process_snapshot.tenant_id = active_order.tenant_id
                AND process_snapshot.active_order_id = active_order.id
                AND process_snapshot.deleted = b'0'
                AND (process_snapshot.work_order_id <> active_order.work_order_id
                  OR process_snapshot.route_id <> active_order.route_id
                  OR process_snapshot.route_version_id <> active_order.route_version_id
                  OR process_snapshot.erp_fixed_quantity_snapshot IS NULL
                  OR active_order.erp_fixed_quantity_snapshot IS NULL
                  OR process_snapshot.erp_fixed_quantity_snapshot <> active_order.erp_fixed_quantity_snapshot
                  OR process_snapshot.production_quantity_factor_snapshot IS NULL
                  OR process_snapshot.production_quantity_factor_snapshot <= 0
                  OR process_snapshot.planned_quantity_snapshot IS NULL
                  OR process_snapshot.planned_quantity_snapshot <>
                       process_snapshot.erp_fixed_quantity_snapshot
                       * process_snapshot.production_quantity_factor_snapshot
                  OR route_node.route_process_id IS NULL)
           )
           OR EXISTS (
             SELECT 1
               FROM JSON_TABLE(
                 CASE WHEN JSON_VALID(route_version.route_snapshot_json) = 1
                        THEN route_version.route_snapshot_json ELSE JSON_OBJECT() END,
                 '$.configSnapshots.flowGraph.nodes[*]'
                 COLUMNS (
                   route_process_id bigint PATH '$.routeProcessId',
                   process_id bigint PATH '$.processId'
                 )
               ) route_node
              WHERE NOT EXISTS (
                SELECT 1
                  FROM mes_pro_process_pool_active_order_process_snapshot process_snapshot
                 WHERE process_snapshot.tenant_id = active_order.tenant_id
                   AND process_snapshot.active_order_id = active_order.id
                   AND process_snapshot.deleted = b'0'
                   AND process_snapshot.route_process_id = route_node.route_process_id
                   AND process_snapshot.process_id = route_node.process_id
              )
           )
         )
    ) removed_process_snapshot_blocker
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_postflight
  SELECT 'removed_active_order_pqc_task_identity', COUNT(1),
         'REMOVED active orders must retain the complete QA-owned PQC task identity set'
    FROM (
      SELECT active_order.id
        FROM mes_pro_process_pool_active_order active_order
        JOIN mes_qa_inspection_regulation_version qa_version
          ON qa_version.id = active_order.qa_regulation_version_id
         AND qa_version.tenant_id = active_order.tenant_id
         AND qa_version.deleted = b'0'
         AND qa_version.regulation_id = active_order.qa_regulation_id
       WHERE active_order.deleted = b'0'
         AND active_order.active_status = 'REMOVED'
         AND (
           NOT EXISTS (
             SELECT 1
               FROM mes_pqc_inspection_task pqc_task
              WHERE pqc_task.tenant_id = active_order.tenant_id
                AND pqc_task.active_order_id = active_order.id
                AND pqc_task.deleted = b'0'
           )
           OR (SELECT COUNT(DISTINCT qa_rule.rule_key)
                 FROM JSON_TABLE(
                   CASE WHEN JSON_VALID(qa_version.inspection_type_rules_json) = 1
                          THEN qa_version.inspection_type_rules_json ELSE JSON_ARRAY() END,
                   '$[*]'
                   COLUMNS (
                     rule_key varchar(32) PATH '$.key',
                     inspection_type varchar(32) PATH '$.inspectionType',
                     required_flag boolean PATH '$.required'
                   )
                 ) qa_rule
                WHERE qa_rule.rule_key IN ('FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL')
                  AND qa_rule.inspection_type = CASE
                        WHEN qa_rule.rule_key = 'FIRST' THEN 'FIRST'
                        WHEN qa_rule.rule_key IN ('PATROL_AM', 'PATROL_PM') THEN 'PATROL'
                        WHEN qa_rule.rule_key = 'FINAL' THEN 'FINAL'
                      END
                  AND qa_rule.required_flag IS NOT NULL) <> 4
           OR EXISTS (
             SELECT 1
               FROM mes_pqc_inspection_task pqc_task
               LEFT JOIN mes_qa_inspection_regulation_process qa_process
                 ON qa_process.id = pqc_task.qa_process_id
                AND qa_process.tenant_id = active_order.tenant_id
                AND qa_process.deleted = b'0'
                AND qa_process.regulation_version_id = active_order.qa_regulation_version_id
               LEFT JOIN JSON_TABLE(
                 CASE WHEN JSON_VALID(qa_version.inspection_type_rules_json) = 1
                        THEN qa_version.inspection_type_rules_json ELSE JSON_ARRAY() END,
                 '$[*]'
                 COLUMNS (
                   rule_key varchar(32) PATH '$.key',
                   inspection_type varchar(32) PATH '$.inspectionType',
                   required_flag boolean PATH '$.required'
                 )
               ) qa_rule
                 ON qa_rule.rule_key COLLATE utf8mb4_unicode_ci = pqc_task.inspection_rule_key
                AND qa_rule.inspection_type COLLATE utf8mb4_unicode_ci = pqc_task.inspection_type
                AND qa_rule.required_flag = 1
              WHERE pqc_task.tenant_id = active_order.tenant_id
                AND pqc_task.active_order_id = active_order.id
                AND pqc_task.deleted = b'0'
                AND (pqc_task.work_order_id <> active_order.work_order_id
                  OR pqc_task.route_id <> active_order.route_id
                  OR pqc_task.route_version_id <> active_order.route_version_id
                  OR pqc_task.regulation_version_id <> active_order.qa_regulation_version_id
                  OR pqc_task.route_process_id IS NOT NULL
                  OR pqc_task.process_id IS NOT NULL
                  OR qa_process.id IS NULL
                  OR qa_rule.rule_key IS NULL
                  OR pqc_task.shift_code <> CASE
                       WHEN pqc_task.inspection_rule_key = 'FIRST' THEN 'FIRST'
                       WHEN pqc_task.inspection_rule_key = 'PATROL_AM' THEN 'AM'
                       WHEN pqc_task.inspection_rule_key = 'PATROL_PM' THEN 'PM'
                       WHEN pqc_task.inspection_rule_key = 'FINAL' THEN 'FINAL'
                     END
                  OR pqc_task.round_no <> 1
                  OR NOT EXISTS (
                    SELECT 1
                      FROM mes_qa_inspection_regulation_item qa_item
                     WHERE qa_item.tenant_id = active_order.tenant_id
                       AND qa_item.deleted = b'0'
                       AND qa_item.regulation_version_id = active_order.qa_regulation_version_id
                       AND qa_item.qa_process_id = pqc_task.qa_process_id
                       AND qa_item.inspection_type = pqc_task.inspection_type
                  ))
           )
           OR EXISTS (
             SELECT 1
               FROM mes_qa_inspection_regulation_process qa_process
               JOIN JSON_TABLE(
                 CASE WHEN JSON_VALID(qa_version.inspection_type_rules_json) = 1
                        THEN qa_version.inspection_type_rules_json ELSE JSON_ARRAY() END,
                 '$[*]'
                 COLUMNS (
                   rule_key varchar(32) PATH '$.key',
                   inspection_type varchar(32) PATH '$.inspectionType',
                   required_flag boolean PATH '$.required'
                 )
               ) qa_rule
                 ON qa_rule.required_flag = 1
              WHERE qa_process.tenant_id = active_order.tenant_id
                AND qa_process.deleted = b'0'
                AND qa_process.regulation_version_id = active_order.qa_regulation_version_id
                AND EXISTS (
                  SELECT 1
                    FROM mes_qa_inspection_regulation_item qa_item
                   WHERE qa_item.tenant_id = active_order.tenant_id
                     AND qa_item.deleted = b'0'
                     AND qa_item.regulation_version_id = active_order.qa_regulation_version_id
                     AND qa_item.qa_process_id = qa_process.id
                     AND qa_item.inspection_type = qa_rule.inspection_type COLLATE utf8mb4_unicode_ci
                )
                AND NOT EXISTS (
                  SELECT 1
                    FROM mes_pqc_inspection_task pqc_task
                   WHERE pqc_task.tenant_id = active_order.tenant_id
                     AND pqc_task.deleted = b'0'
                     AND pqc_task.active_order_id = active_order.id
                     AND pqc_task.regulation_version_id = active_order.qa_regulation_version_id
                     AND pqc_task.qa_process_id = qa_process.id
                     AND pqc_task.inspection_rule_key = qa_rule.rule_key COLLATE utf8mb4_unicode_ci
                )
           )
           OR EXISTS (
             SELECT 1
               FROM mes_pqc_inspection_task pqc_task
              WHERE pqc_task.tenant_id = active_order.tenant_id
                AND pqc_task.active_order_id = active_order.id
                AND pqc_task.deleted = b'0'
              GROUP BY pqc_task.qa_process_id, pqc_task.inspection_rule_key
             HAVING COUNT(1) <> 1
           )
         )
    ) removed_pqc_task_blocker
  HAVING COUNT(1) > 0;

  SELECT COUNT(1) INTO v_blocker_count FROM c015_reconciliation_postflight;
  SELECT * FROM c015_reconciliation_postflight ORDER BY check_name;
  IF v_blocker_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 postflight failed: target schema or data contract is incomplete';
  END IF;
END$$
DELIMITER ;

CALL postflight_mes_c015_route_dcc_qa_reconciliation();
DROP PROCEDURE IF EXISTS postflight_mes_c015_route_dcc_qa_reconciliation;
