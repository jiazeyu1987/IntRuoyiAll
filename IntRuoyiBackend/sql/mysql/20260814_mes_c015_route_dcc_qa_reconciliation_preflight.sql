-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260814_mes_c015_route_dcc_qa_reconciliation_bootstrap; type=preflight; riskLevel=high
-- C015 reconciliation preflight is read-only. Any ambiguity requires an approved, ID-based repair manifest.

DROP PROCEDURE IF EXISTS preflight_mes_c015_route_dcc_qa_reconciliation;
DELIMITER $$
CREATE PROCEDURE preflight_mes_c015_route_dcc_qa_reconciliation()
BEGIN
  DECLARE v_blocker_count bigint DEFAULT 0;
  DECLARE v_required_table_count int DEFAULT 0;

  DROP TEMPORARY TABLE IF EXISTS c015_reconciliation_blocker_report;
  CREATE TEMPORARY TABLE c015_reconciliation_blocker_report (
    blocker_scope varchar(96) NOT NULL,
    source_id bigint DEFAULT NULL,
    input_manifest_sha256 char(64) NOT NULL,
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
    INSERT INTO c015_reconciliation_blocker_report
    VALUES ('required_tables', NULL, SHA2('c015-required-tables-v1', 256),
            13 - v_required_table_count, 'C015 required MES/DCC/QA/PQC base tables are missing');
    SELECT * FROM c015_reconciliation_blocker_report ORDER BY blocker_scope, source_id;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 preflight failed: required base tables are missing';
  END IF;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'forbidden_route_binding_column', NULL, SHA2('c015-route-forbidden-columns-v1', 256), COUNT(1),
         'route-DCC authority must not contain binding_status or route_version_id'
    FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pro_route_dcc_project_binding'
     AND column_name IN ('binding_status', 'route_version_id')
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'item_product_master_column', NULL, SHA2('c015-item-product-master-column-v1', 256), 1,
         'mes_md_item.product_master_id must exist before route-DCC data can be verified'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_md_item'
        AND column_name = 'product_master_id'
   ) AND EXISTS (SELECT 1 FROM mes_pro_route_dcc_project_binding WHERE deleted = b'0');

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'route_binding_version_column', NULL, SHA2('c015-route-version-column-v1', 256), 1,
         'versioned route-DCC history column is missing while binding data exists'
   WHERE NOT EXISTS (
     SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE()
        AND table_name = 'mes_pro_route_dcc_project_binding'
        AND column_name = 'version'
   ) AND EXISTS (SELECT 1 FROM mes_pro_route_dcc_project_binding);

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'route_generated_identity', NULL, SHA2('c015-route-generated-identity-v1', 256), 1,
         'active_route_id exists with a non-canonical generation expression'
    FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_pro_route_dcc_project_binding'
     AND column_name = 'active_route_id'
     AND (extra NOT LIKE '%STORED GENERATED%'
          OR LOWER(generation_expression) NOT LIKE '%case%route_id%');

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'route_current_index_signature', NULL, SHA2('c015-route-current-index-v1', 256), COUNT(1),
         'uk_mes_pro_route_dcc_current exists with a non-canonical signature'
    FROM (
      SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
        FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'mes_pro_route_dcc_project_binding'
         AND index_name = 'uk_mes_pro_route_dcc_current'
       GROUP BY non_unique
    ) current_index
   WHERE non_unique <> 0 OR columns_in_order <> 'tenant_id,active_route_id'
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'route_history_index_signature', NULL, SHA2('c015-route-history-index-v1', 256), COUNT(1),
         'uk_mes_pro_route_dcc_history_version exists with a non-canonical signature'
    FROM (
      SELECT non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
        FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'mes_pro_route_dcc_project_binding'
         AND index_name = 'uk_mes_pro_route_dcc_history_version'
       GROUP BY non_unique
    ) history_index
   WHERE non_unique <> 0 OR columns_in_order <> 'tenant_id,route_id,version'
  HAVING COUNT(1) > 0;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_md_item'
       AND column_name = 'product_master_id'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'mes_pro_route_dcc_project_binding'
       AND column_name = 'version'
  ) THEN
    INSERT INTO c015_reconciliation_blocker_report
    SELECT 'current_route_dcc_authoring_identity', binding.id,
           SHA2(CONCAT('c015-route-binding:', binding.tenant_id, ':', binding.id), 256), 1,
           'current route binding must resolve route products to exactly one productMaster matching the enabled DCC project'
      FROM mes_pro_route_dcc_project_binding binding
      LEFT JOIN dcc_project_code project
        ON project.id = binding.dcc_project_code_id
       AND project.tenant_id = binding.tenant_id
       AND project.deleted = b'0'
       AND project.status = 'ENABLE'
      LEFT JOIN mes_pro_route_product route_product
        ON route_product.route_id = binding.route_id
       AND route_product.tenant_id = binding.tenant_id
       AND route_product.deleted = b'0'
      LEFT JOIN mes_md_item item
        ON item.id = route_product.item_id
       AND item.tenant_id = binding.tenant_id
       AND item.deleted = b'0'
     WHERE binding.deleted = b'0'
     GROUP BY binding.id, binding.tenant_id
    HAVING COUNT(DISTINCT project.id) <> 1
        OR MAX(project.product_master_id) IS NULL
        OR COUNT(route_product.id) = 0
        OR SUM(CASE WHEN item.id IS NULL OR item.product_master_id IS NULL THEN 1 ELSE 0 END) > 0
        OR COUNT(DISTINCT item.product_master_id) <> 1
        OR MIN(item.product_master_id) <> MAX(project.product_master_id);

    INSERT INTO c015_reconciliation_blocker_report
    SELECT 'route_binding_version', binding.id,
           SHA2(CONCAT('c015-route-version:', binding.tenant_id, ':', binding.id), 256), 1,
           'route-DCC binding version must be positive and unique per tenant/route'
      FROM mes_pro_route_dcc_project_binding binding
     WHERE binding.version IS NULL OR binding.version <= 0;

    INSERT INTO c015_reconciliation_blocker_report
    SELECT 'route_binding_duplicate_current', NULL, SHA2('c015-route-current-duplicate-v1', 256), COUNT(1),
           'multiple current route-DCC bindings require an approved ID-based disposition'
      FROM (
        SELECT tenant_id, route_id
          FROM mes_pro_route_dcc_project_binding
         WHERE deleted = b'0'
         GROUP BY tenant_id, route_id
        HAVING COUNT(1) > 1
      ) duplicate_current
    HAVING COUNT(1) > 0;

    INSERT INTO c015_reconciliation_blocker_report
    SELECT 'route_binding_duplicate_version', NULL, SHA2('c015-route-version-duplicate-v1', 256), COUNT(1),
           'duplicate route-DCC history versions require an approved ID-based disposition'
      FROM (
        SELECT tenant_id, route_id, version
          FROM mes_pro_route_dcc_project_binding
         GROUP BY tenant_id, route_id, version
        HAVING COUNT(1) > 1
      ) duplicate_version
    HAVING COUNT(1) > 0;
  END IF;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'qa_dcc_referential_identity', regulation.id,
         SHA2(CONCAT('c015-qa-dcc:', regulation.tenant_id, ':', regulation.id), 256), 1,
         'every QA master must reference one existing DCC project in the same tenant'
    FROM mes_qa_inspection_regulation regulation
    LEFT JOIN dcc_project_code project
      ON project.id = regulation.dcc_project_code_id
     AND project.tenant_id = regulation.tenant_id
     AND project.deleted = b'0'
   WHERE regulation.dcc_project_code_id IS NULL OR project.id IS NULL;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'active_order_qa_history_identity', active_order.id,
         SHA2(CONCAT('c015-active-order-qa-history:', active_order.tenant_id, ':', active_order.id), 256), 1,
         'active-order frozen DCC/QA/version must retain same-tenant ownership and PUBLISHED/RETIRED lifecycle'
    FROM mes_pro_process_pool_active_order active_order
    LEFT JOIN dcc_project_code project
      ON project.id = active_order.dcc_project_code_id
     AND project.tenant_id = active_order.tenant_id
     AND project.deleted = b'0'
    LEFT JOIN mes_qa_inspection_regulation regulation
      ON regulation.id = active_order.qa_regulation_id
     AND regulation.tenant_id = active_order.tenant_id
     AND regulation.deleted = b'0'
     AND regulation.dcc_project_code_id = active_order.dcc_project_code_id
     AND regulation.owner_module = 'MES_QA'
    LEFT JOIN mes_qa_inspection_regulation_version version
      ON version.id = active_order.qa_regulation_version_id
     AND version.tenant_id = active_order.tenant_id
     AND version.deleted = b'0'
     AND version.regulation_id = active_order.qa_regulation_id
     AND version.lifecycle_status IN ('PUBLISHED', 'RETIRED')
   WHERE active_order.deleted = b'0'
     AND (active_order.dcc_project_code_id IS NULL
       OR active_order.qa_regulation_id IS NULL
       OR active_order.qa_regulation_version_id IS NULL
       OR project.id IS NULL
       OR regulation.id IS NULL
       OR version.id IS NULL);

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'removed_active_order_route_identity', active_order.id,
         SHA2(CONCAT('c015-removed-route:', active_order.tenant_id, ':', active_order.id), 256), 1,
         'REMOVED active order must retain an exact frozen route/version and a valid frozen process graph'
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
       ));

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'removed_active_order_process_snapshot_identity', active_order.id,
         SHA2(CONCAT('c015-removed-process-snapshot:', active_order.tenant_id, ':', active_order.id), 256), 1,
         'REMOVED active order process snapshots must exactly and completely match the frozen route version graph'
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
     );

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'removed_active_order_pqc_task_identity', active_order.id,
         SHA2(CONCAT('c015-removed-pqc-task:', active_order.tenant_id, ':', active_order.id), 256), 1,
         'REMOVED active order must retain the complete QA-owned PQC task identity set from its frozen QA version'
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
     );

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'qa_duplicate_active_dcc', NULL, SHA2('c015-qa-active-duplicate-v1', 256), COUNT(1),
         'multiple active QA masters for one DCC project require an approved ID-based disposition'
    FROM (
      SELECT tenant_id, dcc_project_code_id
        FROM mes_qa_inspection_regulation
       WHERE deleted = b'0'
       GROUP BY tenant_id, dcc_project_code_id
      HAVING COUNT(1) > 1
    ) duplicate_qa
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'qa_active_index_signature', NULL, SHA2('c015-qa-active-index-v1', 256), COUNT(1),
         'uk_mes_qa_regulation_active_dcc exists with a non-canonical signature'
    FROM (
      SELECT index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
        FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'mes_qa_inspection_regulation'
         AND index_name = 'uk_mes_qa_regulation_active_dcc'
       GROUP BY index_name, non_unique
    ) active_index
   WHERE non_unique <> 0 OR columns_in_order <> 'tenant_id,active_dcc_project_code_id'
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'qa_generated_identity', NULL, SHA2('c015-qa-generated-identity-v1', 256), 1,
         'active_dcc_project_code_id exists with a non-canonical generation expression'
    FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'mes_qa_inspection_regulation'
     AND column_name = 'active_dcc_project_code_id'
     AND (extra NOT LIKE '%STORED GENERATED%'
          OR LOWER(generation_expression) NOT LIKE '%case%dcc_project_code_id%');

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'qa_legacy_index_signature', NULL, SHA2('c015-qa-legacy-index-v1', 256), COUNT(1),
         'legacy QA DCC index has an unknown signature and cannot be dropped automatically'
    FROM (
      SELECT index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_order
        FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'mes_qa_inspection_regulation'
         AND index_name = 'uk_mes_qa_regulation_dcc_project'
       GROUP BY index_name, non_unique
    ) legacy_index
   WHERE non_unique <> 0 OR columns_in_order <> 'tenant_id,dcc_project_code_id,deleted'
  HAVING COUNT(1) > 0;

  INSERT INTO c015_reconciliation_blocker_report
  SELECT 'pqc_rule_key', task.id, SHA2(CONCAT('c015-pqc-rule:', task.tenant_id, ':', task.id), 256), 1,
         'PQC inspection rule identity must be one of FIRST/PATROL_AM/PATROL_PM/FINAL'
    FROM mes_pqc_inspection_task task
   WHERE task.inspection_rule_key IS NULL
      OR CHAR_LENGTH(task.inspection_rule_key) > 32
      OR task.inspection_rule_key NOT IN ('FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL');

  SELECT COUNT(1) INTO v_blocker_count FROM c015_reconciliation_blocker_report;
  SELECT * FROM c015_reconciliation_blocker_report ORDER BY blocker_scope, source_id;
  IF v_blocker_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'C015 preflight failed: reconciliation blockers require an approved ID-based manifest';
  END IF;
END$$
DELIMITER ;

CALL preflight_mes_c015_route_dcc_qa_reconciliation();
DROP PROCEDURE IF EXISTS preflight_mes_c015_route_dcc_qa_reconciliation;
