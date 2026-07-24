-- release-migration: allowedEnvironments=backup,prod; dependsOn=20260624_dcc_view_matrix_independent_seed; type=data; riskLevel=medium
-- DCC FVM matrix completion while retaining DCC_OTHER_TEMPLATE_900250.
-- Scope: local tenant_id=1; additive only; does not delete, disable, or migrate historical files.
SET @dcc_fvm_completion_tenant_id := 1;
SET @dcc_fvm_completion_actor := CONVERT('dcc_fvm_matrix_retain_other_completion_20260625' USING utf8mb4) COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS apply_dcc_fvm_matrix_retain_other_completion_20260625;
DELIMITER $$
CREATE PROCEDURE apply_dcc_fvm_matrix_retain_other_completion_20260625()
BEGIN
  DECLARE v_active_total BIGINT DEFAULT 0;
  DECLARE v_dcc_fvm_count BIGINT DEFAULT 0;
  DECLARE v_other_count BIGINT DEFAULT 0;
  DECLARE v_template_route_id BIGINT DEFAULT NULL;
  DECLARE v_template_node_count BIGINT DEFAULT 0;
  DECLARE v_view_source_count BIGINT DEFAULT 0;
  DECLARE v_dhf001_count BIGINT DEFAULT 0;
  DECLARE v_view_category_count BIGINT DEFAULT 0;
  DECLARE v_view_rule_count BIGINT DEFAULT 0;
  DECLARE v_view_dot_count BIGINT DEFAULT 0;
  DECLARE v_view_triangle_count BIGINT DEFAULT 0;
  DECLARE v_review_category_count BIGINT DEFAULT 0;
  DECLARE v_review_node_incomplete BIGINT DEFAULT 0;
  DECLARE v_other_after_count BIGINT DEFAULT 0;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(*),
         SUM(category_record.code LIKE 'DCC_FVM%'),
         SUM(category_record.code = 'DCC_OTHER_TEMPLATE_900250')
    INTO v_active_total, v_dcc_fvm_count, v_other_count
  FROM `dcc_file_category` category_record
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1;

  IF v_dcc_fvm_count <> 59 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_CATEGORY_BASELINE_CHANGED';
  END IF;

  IF v_other_count <> 1 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_OTHER_MISSING';
  END IF;

  SELECT route_record.id
    INTO v_template_route_id
  FROM `dcc_category_approval_route` route_record
  JOIN `dcc_file_category` category_record
    ON category_record.id = route_record.category_id
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code = 'DCC_OTHER_TEMPLATE_900250'
    AND route_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND route_record.deleted = 0
    AND route_record.active = 1
  ORDER BY route_record.version_no DESC
  LIMIT 1;

  IF v_template_route_id IS NULL THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_TEMPLATE_ROUTE_MISSING';
  END IF;

  SELECT COUNT(*)
    INTO v_template_node_count
  FROM `dcc_category_approval_route_node` template_node
  WHERE template_node.route_id = v_template_route_id
    AND template_node.tenant_id = @dcc_fvm_completion_tenant_id
    AND template_node.deleted = 0;

  IF v_template_node_count = 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_TEMPLATE_NODE_MISSING';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_completion_view_source;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_completion_view_source (
    excel_file_name varchar(255) NULL,
    excel_column_letter varchar(16) NULL,
    subject_label varchar(255) NOT NULL,
    subject_top_header varchar(128) NULL,
    subject_sub_header varchar(128) NULL,
    marker varchar(8) NOT NULL,
    scope_type varchar(32) NOT NULL,
    subject_type varchar(32) NOT NULL,
    subject_id bigint NULL,
    PRIMARY KEY (subject_label, marker)
  ) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

  INSERT INTO tmp_dcc_fvm_completion_view_source
    (excel_file_name, excel_column_letter, subject_label, subject_top_header, subject_sub_header,
     marker, scope_type, subject_type, subject_id)
  SELECT source_rule.excel_file_name,
         source_rule.excel_column_letter,
         source_rule.subject_label,
         source_rule.subject_top_header,
         source_rule.subject_sub_header,
         source_rule.marker,
         source_rule.scope_type,
         source_rule.subject_type,
         source_rule.subject_id
  FROM `dcc_category_view_matrix_rule` source_rule
  JOIN `dcc_file_category` source_category
    ON source_category.id = source_rule.category_id
  WHERE source_category.tenant_id = @dcc_fvm_completion_tenant_id
    AND source_category.deleted = 0
    AND source_category.active = 1
    AND source_category.code = 'DCC_FVM_DHF_002'
    AND source_rule.tenant_id = @dcc_fvm_completion_tenant_id
    AND source_rule.deleted = 0
    AND source_rule.active = 1
    AND (
      (source_rule.subject_label = 'QMS' AND source_rule.marker = '●')
      OR (
        source_rule.subject_label = CONVERT(UNHEX('E696B0E59381E5BC80E58F91E983A8') USING utf8mb4)
          COLLATE utf8mb4_general_ci
        AND source_rule.marker = '▲'
      )
    );

  SELECT COUNT(*) INTO v_view_source_count
  FROM tmp_dcc_fvm_completion_view_source;

  IF v_view_source_count <> 2 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_VIEW_SOURCE_MISSING';
  END IF;

  INSERT INTO `dcc_category_view_matrix_rule`
    (`category_id`, `excel_file_name`, `excel_row_no`, `excel_column_letter`,
     `subject_label`, `subject_top_header`, `subject_sub_header`, `marker`,
     `scope_type`, `subject_type`, `subject_id`, `active`, `remark`,
     `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
  SELECT target_category.id,
         view_source.excel_file_name,
         5,
         view_source.excel_column_letter,
         view_source.subject_label,
         view_source.subject_top_header,
         view_source.subject_sub_header,
         view_source.marker,
         view_source.scope_type,
         view_source.subject_type,
         view_source.subject_id,
         1,
         NULL,
         @dcc_fvm_completion_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_completion_actor,
         @dcc_fvm_completion_actor,
         0
  FROM `dcc_file_category` target_category
  CROSS JOIN tmp_dcc_fvm_completion_view_source view_source
  WHERE target_category.tenant_id = @dcc_fvm_completion_tenant_id
    AND target_category.deleted = 0
    AND target_category.active = 1
    AND target_category.code = 'DCC_FVM_DHF_001'
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_category_view_matrix_rule` existing_rule
      WHERE existing_rule.category_id = target_category.id
        AND existing_rule.tenant_id = @dcc_fvm_completion_tenant_id
        AND existing_rule.deleted = 0
        AND existing_rule.active = 1
        AND existing_rule.subject_label = view_source.subject_label
        AND existing_rule.marker = view_source.marker
    );

  SELECT COUNT(*)
    INTO v_dhf001_count
  FROM `dcc_category_view_matrix_rule` view_rule
  JOIN `dcc_file_category` category_record
    ON category_record.id = view_rule.category_id
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code = 'DCC_FVM_DHF_001'
    AND view_rule.tenant_id = @dcc_fvm_completion_tenant_id
    AND view_rule.deleted = 0
    AND view_rule.active = 1;

  IF v_dhf001_count <> 2 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_DHF001_MISSING';
  END IF;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_completion_template_node;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_completion_template_node
  ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT template_node.stage_no,
         template_node.stage_code,
         template_node.stage_name,
         template_node.stage_order,
         template_node.candidate_source_type,
         template_node.candidate_source_id,
         template_node.candidate_source_ids,
         template_node.approve_method,
         template_node.approve_ratio,
         template_node.require_all_approvals,
         template_node.required,
         template_node.sort
  FROM `dcc_category_approval_route_node` template_node
  WHERE template_node.route_id = v_template_route_id
    AND template_node.tenant_id = @dcc_fvm_completion_tenant_id
    AND template_node.deleted = 0;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_completion_route_target;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_completion_route_target
  ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT category_record.id AS category_id,
         category_record.code AS category_code
  FROM `dcc_file_category` category_record
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code LIKE 'DCC_FVM%'
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_category_approval_route` existing_route
      WHERE existing_route.category_id = category_record.id
        AND existing_route.tenant_id = @dcc_fvm_completion_tenant_id
        AND existing_route.deleted = 0
        AND existing_route.active = 1
    );

  INSERT INTO `dcc_category_approval_route`
    (`category_id`, `version_no`, `active`, `effective_time`, `remark`,
     `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
  SELECT route_target.category_id,
         COALESCE((
           SELECT MAX(existing_route.version_no)
           FROM `dcc_category_approval_route` existing_route
           WHERE existing_route.category_id = route_target.category_id
             AND existing_route.tenant_id = @dcc_fvm_completion_tenant_id
         ), 0) + 1,
         1,
         NOW(),
         'DCC FVM matrix completion copied from retained OTHER route',
         @dcc_fvm_completion_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_completion_actor,
         @dcc_fvm_completion_actor,
         0
  FROM tmp_dcc_fvm_completion_route_target route_target;

  DROP TEMPORARY TABLE IF EXISTS tmp_dcc_fvm_completion_created_route;
  CREATE TEMPORARY TABLE tmp_dcc_fvm_completion_created_route
  ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci AS
  SELECT route_record.id AS route_id,
         route_record.category_id
  FROM `dcc_category_approval_route` route_record
  JOIN tmp_dcc_fvm_completion_route_target route_target
    ON route_target.category_id = route_record.category_id
  WHERE route_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND route_record.deleted = 0
    AND route_record.active = 1
    AND route_record.creator = @dcc_fvm_completion_actor
    AND NOT EXISTS (
      SELECT 1
      FROM `dcc_category_approval_route_node` existing_node
      WHERE existing_node.route_id = route_record.id
        AND existing_node.tenant_id = @dcc_fvm_completion_tenant_id
        AND existing_node.deleted = 0
    );

  INSERT INTO `dcc_category_approval_route_node`
    (`route_id`, `stage_no`, `stage_code`, `stage_name`, `stage_order`,
     `candidate_source_type`, `candidate_source_id`, `candidate_source_ids`,
     `approve_method`, `approve_ratio`, `require_all_approvals`, `required`, `sort`,
     `tenant_id`, `create_time`, `update_time`, `creator`, `updater`, `deleted`)
  SELECT created_route.route_id,
         template_node.stage_no,
         template_node.stage_code,
         template_node.stage_name,
         template_node.stage_order,
         template_node.candidate_source_type,
         template_node.candidate_source_id,
         template_node.candidate_source_ids,
         template_node.approve_method,
         template_node.approve_ratio,
         template_node.require_all_approvals,
         template_node.required,
         template_node.sort,
         @dcc_fvm_completion_tenant_id,
         NOW(),
         NOW(),
         @dcc_fvm_completion_actor,
         @dcc_fvm_completion_actor,
         0
  FROM tmp_dcc_fvm_completion_created_route created_route
  CROSS JOIN tmp_dcc_fvm_completion_template_node template_node;

  SELECT COUNT(DISTINCT view_rule.category_id),
         COUNT(*),
         SUM(view_rule.marker = '●'),
         SUM(view_rule.marker = '▲')
    INTO v_view_category_count, v_view_rule_count, v_view_dot_count, v_view_triangle_count
  FROM `dcc_category_view_matrix_rule` view_rule
  JOIN `dcc_file_category` category_record
    ON category_record.id = view_rule.category_id
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code LIKE 'DCC_FVM%'
    AND view_rule.tenant_id = @dcc_fvm_completion_tenant_id
    AND view_rule.deleted = 0
    AND view_rule.active = 1;

  IF v_view_category_count <> 59 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_VIEW_NOT_59';
  END IF;

  IF v_view_rule_count <> 243 OR v_view_dot_count <> 195 OR v_view_triangle_count <> 48 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_VIEW_RULES_NOT_243';
  END IF;

  SELECT COUNT(DISTINCT route_record.category_id)
    INTO v_review_category_count
  FROM `dcc_category_approval_route` route_record
  JOIN `dcc_file_category` category_record
    ON category_record.id = route_record.category_id
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code LIKE 'DCC_FVM%'
    AND route_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND route_record.deleted = 0
    AND route_record.active = 1;

  IF v_review_category_count <> 59 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_REVIEW_NOT_59';
  END IF;

  SELECT COUNT(*)
    INTO v_review_node_incomplete
  FROM (
    SELECT route_record.id,
           COUNT(route_node.id) AS node_count
    FROM `dcc_category_approval_route` route_record
    JOIN `dcc_file_category` category_record
      ON category_record.id = route_record.category_id
    LEFT JOIN `dcc_category_approval_route_node` route_node
      ON route_node.route_id = route_record.id
     AND route_node.tenant_id = @dcc_fvm_completion_tenant_id
     AND route_node.deleted = 0
    WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
      AND category_record.deleted = 0
      AND category_record.active = 1
      AND category_record.code LIKE 'DCC_FVM%'
      AND route_record.tenant_id = @dcc_fvm_completion_tenant_id
      AND route_record.deleted = 0
      AND route_record.active = 1
    GROUP BY route_record.id
    HAVING node_count <> v_template_node_count
  ) incomplete_route;

  IF v_review_node_incomplete <> 0 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_REVIEW_NODES_INCOMPLETE';
  END IF;

  SELECT COUNT(*)
    INTO v_other_after_count
  FROM `dcc_file_category` category_record
  WHERE category_record.tenant_id = @dcc_fvm_completion_tenant_id
    AND category_record.deleted = 0
    AND category_record.active = 1
    AND category_record.code = 'DCC_OTHER_TEMPLATE_900250';

  IF v_other_after_count <> 1 THEN
    ROLLBACK;
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC_FVM_RETAIN_OTHER_COMPLETION_OTHER_NOT_RETAINED';
  END IF;

  COMMIT;
END$$
DELIMITER ;

CALL apply_dcc_fvm_matrix_retain_other_completion_20260625();
DROP PROCEDURE IF EXISTS apply_dcc_fvm_matrix_retain_other_completion_20260625;
