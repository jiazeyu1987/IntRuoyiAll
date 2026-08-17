-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260816_mdm_enterprise_company_scope,20260718_controlled_content_lifecycle; type=schema; riskLevel=high
-- Purpose: Create the six-table tenant-scoped domestic registration-certificate core.

DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_core_contract;
DELIMITER $$
CREATE PROCEDURE assert_dcc_registration_certificate_core_contract()
BEGIN
  DECLARE present_table_count int DEFAULT 0;

  SELECT COUNT(*)
    INTO present_table_count
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME IN (
       'dcc_registration_certificate',
       'dcc_registration_certificate_version',
       'dcc_registration_certificate_snapshot',
       'dcc_registration_certificate_snapshot_entrusted',
       'dcc_registration_certificate_file',
       'dcc_registration_certificate_audit'
     );

  IF present_table_count <> 0 AND present_table_count <> 6 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC registration certificate core partial schema detected';
  END IF;

  IF present_table_count = 6 THEN
    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_column;
    CREATE TEMPORARY TABLE tmp_dcc_reg_cert_expected_column (
      table_name varchar(128) NOT NULL,
      column_name varchar(128) NOT NULL,
      column_type varchar(64) NOT NULL,
      is_nullable char(3) NOT NULL,
      generated_column boolean NOT NULL,
      PRIMARY KEY (table_name, column_name)
    );
    INSERT INTO tmp_dcc_reg_cert_expected_column
      (table_name, column_name, column_type, is_nullable, generated_column)
    VALUES
      ('dcc_registration_certificate', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate', 'owner_company_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate', 'product_master_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate', 'project_code_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate', 'first_obtained_date', 'date', 'YES', FALSE),
      ('dcc_registration_certificate', 'current_version_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate', 'pending_version_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate', 'current_snapshot_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate', 'status', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate', 'row_version', 'int', 'NO', FALSE),
      ('dcc_registration_certificate', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate', 'create_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate', 'updater', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate', 'update_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate', 'deleted', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate', 'tenant_id', 'bigint', 'NO', FALSE),

      ('dcc_registration_certificate_version', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'certificate_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'version_no', 'int', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'version_type', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'certificate_no', 'varchar(128)', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'approval_date', 'date', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'effective_date', 'date', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'expiry_date', 'date', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'classification', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'category_changed', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'base_snapshot_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'status', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'formalized_at', 'datetime', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'formalized_by', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'voided_at', 'datetime', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'voided_by', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'void_reason', 'varchar(1024)', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'create_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'updater', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_version', 'update_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'deleted', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'tenant_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_version', 'current_unique_flag', 'tinyint', 'YES', TRUE),
      ('dcc_registration_certificate_version', 'pending_unique_flag', 'tinyint', 'YES', TRUE),

      ('dcc_registration_certificate_snapshot', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'version_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'revision_no', 'int', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'source_change_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'product_name', 'varchar(255)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'registrant_name', 'varchar(255)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'model_specification', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'structure_composition', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'intended_use', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'technical_requirements', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'residence_address', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'production_address', 'text', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'entrusted_production', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'self_production', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'entrusted_enterprises_json', 'json', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'entrusted_enterprise_count', 'int', 'YES', TRUE),
      ('dcc_registration_certificate_snapshot', 'effective_at', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'create_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'updater', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot', 'update_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'deleted', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot', 'tenant_id', 'bigint', 'NO', FALSE),

      ('dcc_registration_certificate_snapshot_entrusted', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'snapshot_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'enterprise_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'enterprise_name_snapshot', 'varchar(255)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'sort_order', 'int', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'create_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'updater', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'update_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'deleted', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_snapshot_entrusted', 'tenant_id', 'bigint', 'NO', FALSE),

      ('dcc_registration_certificate_file', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'owner_type', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'owner_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'file_kind', 'varchar(64)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'infra_file_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'original_name', 'varchar(512)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'mime_type', 'varchar(128)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'file_size', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'sha256', 'char(64)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'status', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'bound_at', 'datetime', 'YES', FALSE),
      ('dcc_registration_certificate_file', 'bound_by', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_file', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_file', 'create_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'updater', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_file', 'update_time', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'deleted', 'bit(1)', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'tenant_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_file', 'bound_file_unique_flag', 'bigint', 'YES', TRUE),

      ('dcc_registration_certificate_audit', 'id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'tenant_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'owner_company_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'certificate_id', 'bigint', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'version_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'snapshot_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'business_file_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'event_key', 'varchar(256)', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'event_type', 'varchar(64)', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'actor_id', 'bigint', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'result', 'varchar(32)', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'result_code', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'request_trace_id', 'varchar(128)', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'detail_json', 'json', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'occurred_at', 'datetime', 'NO', FALSE),
      ('dcc_registration_certificate_audit', 'creator', 'varchar(64)', 'YES', FALSE),
      ('dcc_registration_certificate_audit', 'create_time', 'datetime', 'NO', FALSE);

    IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_expected_column AS expected_column
        LEFT JOIN information_schema.COLUMNS AS actual_column
          ON actual_column.TABLE_SCHEMA = DATABASE()
         AND actual_column.TABLE_NAME = expected_column.table_name
         AND actual_column.COLUMN_NAME = expected_column.column_name
       WHERE actual_column.COLUMN_NAME IS NULL
          OR LOWER(actual_column.COLUMN_TYPE) <> expected_column.column_type
          OR actual_column.IS_NULLABLE <> expected_column.is_nullable
          OR (expected_column.generated_column
              AND actual_column.EXTRA NOT LIKE '%STORED GENERATED%')
          OR (NOT expected_column.generated_column
              AND (actual_column.EXTRA LIKE '%STORED GENERATED%'
                OR actual_column.EXTRA LIKE '%VIRTUAL GENERATED%'))
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core column contract mismatch';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM (
          SELECT table_name, COUNT(*) AS column_count
            FROM tmp_dcc_reg_cert_expected_column
           GROUP BY table_name
        ) AS expected_table
        LEFT JOIN (
          SELECT TABLE_NAME AS table_name, COUNT(*) AS column_count
            FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME IN (
               'dcc_registration_certificate',
               'dcc_registration_certificate_version',
               'dcc_registration_certificate_snapshot',
               'dcc_registration_certificate_snapshot_entrusted',
               'dcc_registration_certificate_file',
               'dcc_registration_certificate_audit'
             )
           GROUP BY TABLE_NAME
        ) AS actual_table ON actual_table.table_name = expected_table.table_name
       WHERE actual_table.column_count <> expected_table.column_count
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core column contract mismatch';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_column;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_generation;
    CREATE TEMPORARY TABLE tmp_dcc_reg_cert_expected_generation (
      table_name varchar(128) NOT NULL,
      column_name varchar(128) NOT NULL,
      normalized_expression varchar(1024) NOT NULL,
      PRIMARY KEY (table_name, column_name)
    );
    INSERT INTO tmp_dcc_reg_cert_expected_generation
      (table_name, column_name, normalized_expression)
    VALUES
      ('dcc_registration_certificate_version', 'current_unique_flag',
       '(casewhen((deleted=0x00)and(status=''current''))then1elsenullend)'),
      ('dcc_registration_certificate_version', 'pending_unique_flag',
       '(casewhen((deleted=0x00)and(status=''pending_effective''))then1elsenullend)'),
      ('dcc_registration_certificate_snapshot', 'entrusted_enterprise_count',
       'json_length(entrusted_enterprises_json)'),
      ('dcc_registration_certificate_file', 'bound_file_unique_flag',
       '(casewhen((deleted=0x00)and(bound_atisnotnull))theninfra_file_idelsenullend)');

    IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_expected_generation AS expected_generation
        LEFT JOIN information_schema.COLUMNS AS actual_generation
          ON actual_generation.TABLE_SCHEMA = DATABASE()
         AND actual_generation.TABLE_NAME = expected_generation.table_name
         AND actual_generation.COLUMN_NAME = expected_generation.column_name
       WHERE actual_generation.COLUMN_NAME IS NULL
          OR REGEXP_REPLACE(
               REPLACE(REPLACE(REPLACE(LOWER(actual_generation.GENERATION_EXPRESSION), '`', ''),
                 '_utf8mb4', ''), CHAR(92), ''),
               '[[:space:]]+', '') <> expected_generation.normalized_expression
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core exact generated expression mismatch';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_generation;

    IF EXISTS (
      SELECT 1
        FROM (
          SELECT 'dcc_registration_certificate_version' AS table_name,
                 'uk_dcc_reg_cert_version_no' AS index_name,
                 'tenant_id,certificate_id,version_no' AS column_names
          UNION ALL SELECT 'dcc_registration_certificate_version', 'uk_dcc_reg_cert_current',
                           'tenant_id,certificate_id,current_unique_flag'
          UNION ALL SELECT 'dcc_registration_certificate_version', 'uk_dcc_reg_cert_pending',
                           'tenant_id,certificate_id,pending_unique_flag'
          UNION ALL SELECT 'dcc_registration_certificate_snapshot', 'uk_dcc_reg_cert_snapshot_revision',
                           'tenant_id,version_id,revision_no'
          UNION ALL SELECT 'dcc_registration_certificate_snapshot_entrusted', 'uk_dcc_reg_cert_entrusted',
                           'tenant_id,snapshot_id,enterprise_id'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'uk_dcc_reg_cert_bound_file',
                           'tenant_id,bound_file_unique_flag'
          UNION ALL SELECT 'dcc_registration_certificate_audit', 'uk_dcc_reg_cert_audit_event',
                           'tenant_id,event_key'
        ) AS expected_index
        LEFT JOIN (
          SELECT TABLE_NAME, INDEX_NAME, MAX(NON_UNIQUE) AS non_unique,
                 GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS column_names,
                 SUM(SUB_PART IS NOT NULL) AS prefix_column_count
            FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE()
           GROUP BY TABLE_NAME, INDEX_NAME
        ) AS actual_index
          ON actual_index.TABLE_NAME = expected_index.table_name
         AND actual_index.INDEX_NAME = expected_index.index_name
       WHERE actual_index.INDEX_NAME IS NULL
          OR actual_index.non_unique <> 0
          OR actual_index.column_names <> expected_index.column_names
          OR actual_index.prefix_column_count <> 0
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core unique index contract mismatch';
    END IF;

    IF EXISTS (
      SELECT 1
        FROM (
          SELECT 'dcc_registration_certificate' AS table_name,
                 'chk_dcc_reg_cert_master_status' AS constraint_name
          UNION ALL SELECT 'dcc_registration_certificate_version', 'chk_dcc_reg_cert_version_type'
          UNION ALL SELECT 'dcc_registration_certificate_version', 'chk_dcc_reg_cert_version_status'
          UNION ALL SELECT 'dcc_registration_certificate_snapshot', 'chk_dcc_reg_cert_snapshot_json_array'
          UNION ALL SELECT 'dcc_registration_certificate_snapshot', 'chk_dcc_reg_cert_production_relation'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_owner_type'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_kind'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_status'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_binding'
          UNION ALL SELECT 'dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_size'
          UNION ALL SELECT 'dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_event_key'
          UNION ALL SELECT 'dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_result'
          UNION ALL SELECT 'dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_trace'
        ) AS expected_check
        LEFT JOIN information_schema.TABLE_CONSTRAINTS AS actual_check
          ON actual_check.CONSTRAINT_SCHEMA = DATABASE()
         AND actual_check.TABLE_NAME = expected_check.table_name
         AND actual_check.CONSTRAINT_NAME = expected_check.constraint_name
         AND actual_check.CONSTRAINT_TYPE = 'CHECK'
       WHERE actual_check.CONSTRAINT_NAME IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core CHECK contract mismatch';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_check;
    CREATE TEMPORARY TABLE tmp_dcc_reg_cert_expected_check (
      table_name varchar(128) NOT NULL,
      constraint_name varchar(128) NOT NULL,
      normalized_expression text NOT NULL,
      PRIMARY KEY (table_name, constraint_name)
    );
    INSERT INTO tmp_dcc_reg_cert_expected_check
      (table_name, constraint_name, normalized_expression)
    VALUES
      ('dcc_registration_certificate', 'chk_dcc_reg_cert_master_status',
       '(statusin(''draft'',''pending_first_effective'',''active'',''expired_unrenewed'',''voided''))'),
      ('dcc_registration_certificate_version', 'chk_dcc_reg_cert_version_type',
       '(version_typein(''initial_certificate'',''renewal_certificate''))'),
      ('dcc_registration_certificate_version', 'chk_dcc_reg_cert_version_status',
       '(statusin(''draft'',''pending_effective'',''current'',''old'',''voided''))'),
      ('dcc_registration_certificate_snapshot', 'chk_dcc_reg_cert_snapshot_json_array',
       '(json_type(entrusted_enterprises_json)=''array'')'),
      ('dcc_registration_certificate_snapshot', 'chk_dcc_reg_cert_production_relation',
       '(((entrusted_production=0x01)or(self_production=0x01))and(((entrusted_production=0x01)and(entrusted_enterprise_count>=1))or((entrusted_production=0x00)and(entrusted_enterprise_count=0))))'),
      ('dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_owner_type',
       '(owner_typein(''version'',''change'',''supporting_document''))'),
      ('dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_kind',
       '(file_kindin(''registration_certificate'',''change_approval'',''renewal_acceptance_receipt'',''renewal_supplement_notice''))'),
      ('dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_status',
       '(statusin(''staged'',''bound'',''cleanup_required'',''voided''))'),
      ('dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_binding',
       '(((statusin(''bound'',''voided''))and(bound_atisnotnull)and(bound_byisnotnull))or((statusin(''staged'',''cleanup_required''))and(bound_atisnull)and(bound_byisnull)))'),
      ('dcc_registration_certificate_file', 'chk_dcc_reg_cert_file_size',
       '(file_size>=0)'),
      ('dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_event_key',
       '(trim(event_key)<>'''')'),
      ('dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_result',
       '(resultin(''success'',''failure''))'),
      ('dcc_registration_certificate_audit', 'chk_dcc_reg_cert_audit_trace',
       '(trim(request_trace_id)<>'''')');

    IF EXISTS (
      SELECT 1
        FROM tmp_dcc_reg_cert_expected_check AS expected_check_expression
        LEFT JOIN information_schema.TABLE_CONSTRAINTS AS actual_check
          ON actual_check.CONSTRAINT_SCHEMA = DATABASE()
         AND actual_check.TABLE_NAME = expected_check_expression.table_name
         AND actual_check.CONSTRAINT_NAME = expected_check_expression.constraint_name
         AND actual_check.CONSTRAINT_TYPE = 'CHECK'
        LEFT JOIN information_schema.CHECK_CONSTRAINTS AS actual_check_expression
          ON actual_check_expression.CONSTRAINT_SCHEMA = actual_check.CONSTRAINT_SCHEMA
         AND actual_check_expression.CONSTRAINT_NAME = actual_check.CONSTRAINT_NAME
       WHERE actual_check_expression.CONSTRAINT_NAME IS NULL
          OR REGEXP_REPLACE(
               REPLACE(REPLACE(REPLACE(LOWER(actual_check_expression.CHECK_CLAUSE), '`', ''),
                 '_utf8mb4', ''), CHAR(92), ''),
               '[[:space:]]+', '') <> expected_check_expression.normalized_expression
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core exact CHECK expression mismatch';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_dcc_reg_cert_expected_check;

    IF EXISTS (
      SELECT 1
        FROM (
          SELECT 'trg_dcc_reg_cert_master_immutable_bu' AS trigger_name,
                 'dcc_registration_certificate' AS table_name, 'UPDATE' AS event_type
          UNION ALL SELECT 'trg_dcc_reg_cert_master_immutable_bd',
                           'dcc_registration_certificate', 'DELETE'
          UNION ALL SELECT 'trg_dcc_reg_cert_version_immutable_bu',
                           'dcc_registration_certificate_version', 'UPDATE'
          UNION ALL SELECT 'trg_dcc_reg_cert_version_immutable_bd',
                           'dcc_registration_certificate_version', 'DELETE'
          UNION ALL SELECT 'trg_dcc_reg_cert_snapshot_immutable_bu',
                           'dcc_registration_certificate_snapshot', 'UPDATE'
          UNION ALL SELECT 'trg_dcc_reg_cert_snapshot_immutable_bd',
                           'dcc_registration_certificate_snapshot', 'DELETE'
          UNION ALL SELECT 'trg_dcc_reg_cert_entrusted_immutable_bu',
                           'dcc_registration_certificate_snapshot_entrusted', 'UPDATE'
          UNION ALL SELECT 'trg_dcc_reg_cert_entrusted_immutable_bd',
                           'dcc_registration_certificate_snapshot_entrusted', 'DELETE'
          UNION ALL SELECT 'trg_dcc_reg_cert_file_immutable_bu',
                           'dcc_registration_certificate_file', 'UPDATE'
          UNION ALL SELECT 'trg_dcc_reg_cert_file_immutable_bd',
                           'dcc_registration_certificate_file', 'DELETE'
          UNION ALL SELECT 'trg_dcc_reg_cert_audit_immutable_bu',
                           'dcc_registration_certificate_audit', 'UPDATE'
          UNION ALL SELECT 'trg_dcc_reg_cert_audit_immutable_bd',
                           'dcc_registration_certificate_audit', 'DELETE'
        ) AS expected_trigger
        LEFT JOIN information_schema.TRIGGERS AS actual_trigger
          ON actual_trigger.TRIGGER_SCHEMA = DATABASE()
         AND actual_trigger.TRIGGER_NAME = expected_trigger.trigger_name
         AND actual_trigger.EVENT_OBJECT_TABLE = expected_trigger.table_name
         AND actual_trigger.EVENT_MANIPULATION = expected_trigger.event_type
         AND actual_trigger.ACTION_TIMING = 'BEFORE'
       WHERE actual_trigger.TRIGGER_NAME IS NULL
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'DCC registration certificate core trigger contract mismatch';
    END IF;
  END IF;
END$$
DELIMITER ;

CALL assert_dcc_registration_certificate_core_contract();

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Registration certificate aggregate id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `product_master_id` bigint NOT NULL COMMENT 'MDM product master id',
  `project_code_id` bigint DEFAULT NULL COMMENT 'Optional DCC project code id',
  `first_obtained_date` date DEFAULT NULL COMMENT 'First certificate obtained date',
  `current_version_id` bigint DEFAULT NULL COMMENT 'Current formal version id',
  `pending_version_id` bigint DEFAULT NULL COMMENT 'Pending effective version id',
  `current_snapshot_id` bigint DEFAULT NULL COMMENT 'Current display snapshot id',
  `status` varchar(32) NOT NULL COMMENT 'Aggregate status',
  `row_version` int NOT NULL DEFAULT 0 COMMENT 'Optimistic lock revision',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_reg_cert_master_status` CHECK (`status` IN
    ('DRAFT', 'PENDING_FIRST_EFFECTIVE', 'ACTIVE', 'EXPIRED_UNRENEWED', 'VOIDED')),
  KEY `idx_dcc_reg_cert_owner_product` (`tenant_id`, `owner_company_id`, `product_master_id`),
  KEY `idx_dcc_reg_cert_project_code` (`tenant_id`, `project_code_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Domestic registration certificate aggregate';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate version id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `version_no` int NOT NULL COMMENT 'Monotonic version number',
  `version_type` varchar(32) NOT NULL COMMENT 'Initial or renewal certificate',
  `certificate_no` varchar(128) DEFAULT NULL COMMENT 'Formal certificate number',
  `approval_date` date DEFAULT NULL COMMENT 'Approval date',
  `effective_date` date DEFAULT NULL COMMENT 'Effective date',
  `expiry_date` date DEFAULT NULL COMMENT 'Expiry date',
  `classification` varchar(64) DEFAULT NULL COMMENT 'Certificate classification',
  `category_changed` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Renewal category change flag',
  `base_snapshot_id` bigint DEFAULT NULL COMMENT 'Base formal snapshot id',
  `status` varchar(32) NOT NULL COMMENT 'Version lifecycle status',
  `formalized_at` datetime DEFAULT NULL COMMENT 'Formalization time',
  `formalized_by` bigint DEFAULT NULL COMMENT 'Formalization actor',
  `voided_at` datetime DEFAULT NULL COMMENT 'Void time',
  `voided_by` bigint DEFAULT NULL COMMENT 'Void actor',
  `void_reason` varchar(1024) DEFAULT NULL COMMENT 'Void reason',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `current_unique_flag` tinyint GENERATED ALWAYS AS
    (CASE WHEN (`deleted` = b'0' AND `status` = 'CURRENT') THEN 1 ELSE NULL END) STORED,
  `pending_unique_flag` tinyint GENERATED ALWAYS AS
    (CASE WHEN (`deleted` = b'0' AND `status` = 'PENDING_EFFECTIVE') THEN 1 ELSE NULL END) STORED,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_reg_cert_version_type` CHECK
    (`version_type` IN ('INITIAL_CERTIFICATE', 'RENEWAL_CERTIFICATE')),
  CONSTRAINT `chk_dcc_reg_cert_version_status` CHECK
    (`status` IN ('DRAFT', 'PENDING_EFFECTIVE', 'CURRENT', 'OLD', 'VOIDED')),
  UNIQUE KEY `uk_dcc_reg_cert_version_no` (`tenant_id`, `certificate_id`, `version_no`),
  UNIQUE KEY `uk_dcc_reg_cert_current` (`tenant_id`, `certificate_id`, `current_unique_flag`),
  UNIQUE KEY `uk_dcc_reg_cert_pending` (`tenant_id`, `certificate_id`, `pending_unique_flag`),
  KEY `idx_dcc_reg_cert_version_status` (`tenant_id`, `status`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Immutable registration certificate version facts';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate snapshot id',
  `version_id` bigint NOT NULL COMMENT 'Certificate version id',
  `revision_no` int NOT NULL COMMENT 'Snapshot revision number',
  `source_change_id` bigint DEFAULT NULL COMMENT 'Later change fact id',
  `product_name` varchar(255) NOT NULL COMMENT 'Product name snapshot',
  `registrant_name` varchar(255) NOT NULL COMMENT 'Registrant name snapshot',
  `model_specification` text DEFAULT NULL COMMENT 'Model and specification snapshot',
  `structure_composition` text DEFAULT NULL COMMENT 'Structure and composition snapshot',
  `intended_use` text DEFAULT NULL COMMENT 'Intended use snapshot',
  `technical_requirements` text DEFAULT NULL COMMENT 'Technical requirements snapshot',
  `residence_address` text DEFAULT NULL COMMENT 'Residence address snapshot',
  `production_address` text DEFAULT NULL COMMENT 'Production address snapshot',
  `entrusted_production` bit(1) NOT NULL COMMENT 'Entrusted production flag',
  `self_production` bit(1) NOT NULL COMMENT 'Self production flag',
  `entrusted_enterprises_json` json NOT NULL COMMENT 'Authoritative entrusted enterprise facts',
  `entrusted_enterprise_count` int GENERATED ALWAYS AS
    (JSON_LENGTH(`entrusted_enterprises_json`)) STORED,
  `effective_at` datetime NOT NULL COMMENT 'Snapshot effective time',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_reg_cert_snapshot_json_array` CHECK
    (JSON_TYPE(`entrusted_enterprises_json`) = 'ARRAY'),
  CONSTRAINT `chk_dcc_reg_cert_production_relation` CHECK (
    (`entrusted_production` = b'1' OR `self_production` = b'1')
    AND ((`entrusted_production` = b'1' AND `entrusted_enterprise_count` >= 1)
      OR (`entrusted_production` = b'0' AND `entrusted_enterprise_count` = 0))
  ),
  UNIQUE KEY `uk_dcc_reg_cert_snapshot_revision` (`tenant_id`, `version_id`, `revision_no`),
  KEY `idx_dcc_reg_cert_snapshot_change` (`tenant_id`, `source_change_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Immutable registration certificate display snapshot';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_snapshot_entrusted` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Entrusted enterprise projection id',
  `snapshot_id` bigint NOT NULL COMMENT 'Certificate snapshot id',
  `enterprise_id` bigint NOT NULL COMMENT 'MDM entrusted enterprise id',
  `enterprise_name_snapshot` varchar(255) NOT NULL COMMENT 'Enterprise name snapshot',
  `sort_order` int NOT NULL COMMENT 'Authoritative array order',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_reg_cert_entrusted` (`tenant_id`, `snapshot_id`, `enterprise_id`),
  KEY `idx_dcc_reg_cert_entrusted_enterprise` (`tenant_id`, `enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Entrusted enterprise query projection';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate business file id',
  `owner_type` varchar(32) NOT NULL COMMENT 'Business owner type',
  `owner_id` bigint NOT NULL COMMENT 'Business owner id',
  `file_kind` varchar(64) NOT NULL COMMENT 'Registration certificate file kind',
  `infra_file_id` bigint NOT NULL COMMENT 'Infra file id',
  `original_name` varchar(512) NOT NULL COMMENT 'Original file name snapshot',
  `mime_type` varchar(128) NOT NULL COMMENT 'MIME type snapshot',
  `file_size` bigint NOT NULL COMMENT 'File size snapshot',
  `sha256` char(64) NOT NULL COMMENT 'Content SHA-256 snapshot',
  `status` varchar(32) NOT NULL COMMENT 'Business file status',
  `bound_at` datetime DEFAULT NULL COMMENT 'Formal bind time',
  `bound_by` bigint DEFAULT NULL COMMENT 'Formal bind actor',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Logical deletion flag',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `bound_file_unique_flag` bigint GENERATED ALWAYS AS
    (CASE WHEN (`deleted` = b'0' AND `bound_at` IS NOT NULL) THEN `infra_file_id` ELSE NULL END) STORED,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_reg_cert_file_owner_type` CHECK
    (`owner_type` IN ('VERSION', 'CHANGE', 'SUPPORTING_DOCUMENT')),
  CONSTRAINT `chk_dcc_reg_cert_file_kind` CHECK (`file_kind` IN
    ('REGISTRATION_CERTIFICATE', 'CHANGE_APPROVAL', 'RENEWAL_ACCEPTANCE_RECEIPT', 'RENEWAL_SUPPLEMENT_NOTICE')),
  CONSTRAINT `chk_dcc_reg_cert_file_status` CHECK
    (`status` IN ('STAGED', 'BOUND', 'CLEANUP_REQUIRED', 'VOIDED')),
  CONSTRAINT `chk_dcc_reg_cert_file_binding` CHECK (
    (`status` IN ('BOUND', 'VOIDED') AND `bound_at` IS NOT NULL AND `bound_by` IS NOT NULL)
    OR (`status` IN ('STAGED', 'CLEANUP_REQUIRED') AND `bound_at` IS NULL AND `bound_by` IS NULL)
  ),
  CONSTRAINT `chk_dcc_reg_cert_file_size` CHECK (`file_size` >= 0),
  UNIQUE KEY `uk_dcc_reg_cert_bound_file` (`tenant_id`, `bound_file_unique_flag`),
  KEY `idx_dcc_reg_cert_file_owner` (`tenant_id`, `owner_type`, `owner_id`, `file_kind`),
  KEY `idx_dcc_reg_cert_file_infra` (`tenant_id`, `infra_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registration certificate business file reference';

CREATE TABLE IF NOT EXISTS `dcc_registration_certificate_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Certificate audit id',
  `tenant_id` bigint NOT NULL COMMENT 'Tenant id',
  `owner_company_id` bigint NOT NULL COMMENT 'Owning company enterprise id',
  `certificate_id` bigint NOT NULL COMMENT 'Registration certificate aggregate id',
  `version_id` bigint DEFAULT NULL COMMENT 'Certificate version id',
  `snapshot_id` bigint DEFAULT NULL COMMENT 'Certificate snapshot id',
  `business_file_id` bigint DEFAULT NULL COMMENT 'Registration certificate business file id',
  `event_key` varchar(256) NOT NULL COMMENT 'Tenant-scoped idempotent event key',
  `event_type` varchar(64) NOT NULL COMMENT 'Stable audit event type',
  `actor_id` bigint DEFAULT NULL COMMENT 'Actor user id',
  `result` varchar(32) NOT NULL COMMENT 'Success or failure result',
  `result_code` varchar(64) DEFAULT NULL COMMENT 'Stable operation result code',
  `request_trace_id` varchar(128) NOT NULL COMMENT 'Request trace id',
  `detail_json` json NOT NULL COMMENT 'Immutable event detail',
  `occurred_at` datetime NOT NULL COMMENT 'Business occurrence time',
  `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_dcc_reg_cert_audit_event_key` CHECK (TRIM(`event_key`) <> ''),
  CONSTRAINT `chk_dcc_reg_cert_audit_result` CHECK (`result` IN ('SUCCESS', 'FAILURE')),
  CONSTRAINT `chk_dcc_reg_cert_audit_trace` CHECK (TRIM(`request_trace_id`) <> ''),
  UNIQUE KEY `uk_dcc_reg_cert_audit_event` (`tenant_id`, `event_key`),
  KEY `idx_dcc_reg_cert_audit_company` (`tenant_id`, `owner_company_id`, `occurred_at`),
  KEY `idx_dcc_reg_cert_audit_certificate` (`tenant_id`, `certificate_id`, `occurred_at`),
  KEY `idx_dcc_reg_cert_audit_version` (`tenant_id`, `version_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Append-only registration certificate audit';

DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_master_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_master_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_version_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_version_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_snapshot_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_snapshot_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_entrusted_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_entrusted_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_file_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_file_immutable_bd`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_audit_immutable_bu`;
DROP TRIGGER IF EXISTS `trg_dcc_reg_cert_audit_immutable_bd`;

DELIMITER $$
CREATE TRIGGER `trg_dcc_reg_cert_master_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate`
FOR EACH ROW
BEGIN
  IF OLD.`status` <> 'DRAFT' AND NEW.`status` = 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate master cannot return to DRAFT';
  END IF;
  IF OLD.`status` = 'VOIDED' AND NEW.`status` <> 'VOIDED' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'VOIDED registration certificate master status is terminal';
  END IF;
  IF OLD.`status` <> 'DRAFT' AND (
    NOT (OLD.`id` <=> NEW.`id`)
    OR NOT (OLD.`tenant_id` <=> NEW.`tenant_id`)
    OR NOT (OLD.`owner_company_id` <=> NEW.`owner_company_id`)
    OR NOT (OLD.`product_master_id` <=> NEW.`product_master_id`)
    OR NOT (OLD.`project_code_id` <=> NEW.`project_code_id`)
    OR NOT (OLD.`first_obtained_date` <=> NEW.`first_obtained_date`)
    OR NOT (OLD.`deleted` <=> NEW.`deleted`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate master facts are immutable';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_master_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate`
FOR EACH ROW
BEGIN
  IF OLD.`status` <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate master cannot be deleted';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_version_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_version`
FOR EACH ROW
BEGIN
  IF OLD.`status` <> NEW.`status` AND NOT (
    (OLD.`status` = 'DRAFT' AND NEW.`status` IN ('CURRENT', 'PENDING_EFFECTIVE'))
    OR (OLD.`status` = 'PENDING_EFFECTIVE' AND NEW.`status` IN ('CURRENT', 'VOIDED'))
    OR (OLD.`status` = 'CURRENT' AND NEW.`status` IN ('OLD', 'VOIDED'))
    OR (OLD.`status` = 'OLD' AND NEW.`status` = 'VOIDED')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid registration certificate version status transition';
  END IF;
  IF OLD.`status` <> 'DRAFT' AND (
    NOT (OLD.`id` <=> NEW.`id`)
    OR NOT (OLD.`tenant_id` <=> NEW.`tenant_id`)
    OR NOT (OLD.`certificate_id` <=> NEW.`certificate_id`)
    OR NOT (OLD.`version_no` <=> NEW.`version_no`)
    OR NOT (OLD.`version_type` <=> NEW.`version_type`)
    OR NOT (OLD.`certificate_no` <=> NEW.`certificate_no`)
    OR NOT (OLD.`approval_date` <=> NEW.`approval_date`)
    OR NOT (OLD.`effective_date` <=> NEW.`effective_date`)
    OR NOT (OLD.`expiry_date` <=> NEW.`expiry_date`)
    OR NOT (OLD.`classification` <=> NEW.`classification`)
    OR NOT (OLD.`category_changed` <=> NEW.`category_changed`)
    OR NOT (OLD.`base_snapshot_id` <=> NEW.`base_snapshot_id`)
    OR NOT (OLD.`formalized_at` <=> NEW.`formalized_at`)
    OR NOT (OLD.`formalized_by` <=> NEW.`formalized_by`)
    OR NOT (OLD.`deleted` <=> NEW.`deleted`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate version facts are immutable';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_version_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_version`
FOR EACH ROW
BEGIN
  IF OLD.`status` <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate version cannot be deleted';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_snapshot_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_snapshot`
FOR EACH ROW
BEGIN
  DECLARE linked_version_status varchar(32);
  IF NOT (OLD.`id` <=> NEW.`id`)
     OR NOT (OLD.`version_id` <=> NEW.`version_id`)
     OR NOT (OLD.`tenant_id` <=> NEW.`tenant_id`) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cross-version snapshot reattachment is forbidden';
  END IF;
  SELECT `status` INTO linked_version_status
    FROM `dcc_registration_certificate_version`
   WHERE `id` = OLD.`version_id`
     AND `tenant_id` = OLD.`tenant_id`
     AND `deleted` = b'0';
  IF linked_version_status IS NULL OR linked_version_status <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate snapshot is append-only';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_snapshot_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_snapshot`
FOR EACH ROW
BEGIN
  DECLARE linked_version_status varchar(32);
  SELECT `status` INTO linked_version_status
    FROM `dcc_registration_certificate_version`
   WHERE `id` = OLD.`version_id`
     AND `tenant_id` = OLD.`tenant_id`
     AND `deleted` = b'0';
  IF linked_version_status IS NULL OR linked_version_status <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Formal registration certificate snapshot cannot be deleted';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_entrusted_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_snapshot_entrusted`
FOR EACH ROW
BEGIN
  DECLARE entrusted_version_status varchar(32);
  IF NOT (OLD.`id` <=> NEW.`id`)
     OR NOT (OLD.`snapshot_id` <=> NEW.`snapshot_id`)
     OR NOT (OLD.`tenant_id` <=> NEW.`tenant_id`) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Cross-snapshot entrusted projection reattachment is forbidden';
  END IF;
  SELECT version_row.`status` INTO entrusted_version_status
    FROM `dcc_registration_certificate_snapshot` AS snapshot_row
    JOIN `dcc_registration_certificate_version` AS version_row
      ON version_row.`id` = snapshot_row.`version_id`
     AND version_row.`tenant_id` = snapshot_row.`tenant_id`
     AND version_row.`deleted` = b'0'
   WHERE snapshot_row.`id` = OLD.`snapshot_id`
     AND snapshot_row.`tenant_id` = OLD.`tenant_id`;
  IF entrusted_version_status IS NULL OR entrusted_version_status <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate entrusted projection is append-only';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_entrusted_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_snapshot_entrusted`
FOR EACH ROW
BEGIN
  DECLARE entrusted_version_status varchar(32);
  SELECT version_row.`status` INTO entrusted_version_status
    FROM `dcc_registration_certificate_snapshot` AS snapshot_row
    JOIN `dcc_registration_certificate_version` AS version_row
      ON version_row.`id` = snapshot_row.`version_id`
     AND version_row.`tenant_id` = snapshot_row.`tenant_id`
     AND version_row.`deleted` = b'0'
   WHERE snapshot_row.`id` = OLD.`snapshot_id`
     AND snapshot_row.`tenant_id` = OLD.`tenant_id`;
  IF entrusted_version_status IS NULL OR entrusted_version_status <> 'DRAFT' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Registration certificate entrusted projection cannot be deleted';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_file_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_file`
FOR EACH ROW
BEGIN
  IF OLD.`status` <> NEW.`status` AND NOT (
    (OLD.`status` = 'STAGED' AND NEW.`status` IN ('BOUND', 'CLEANUP_REQUIRED'))
    OR (OLD.`status` = 'BOUND' AND NEW.`status` = 'VOIDED')
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Invalid registration certificate file status transition';
  END IF;
  IF OLD.`bound_at` IS NOT NULL AND (
    NOT (OLD.`id` <=> NEW.`id`)
    OR NOT (OLD.`tenant_id` <=> NEW.`tenant_id`)
    OR NOT (OLD.`owner_type` <=> NEW.`owner_type`)
    OR NOT (OLD.`owner_id` <=> NEW.`owner_id`)
    OR NOT (OLD.`file_kind` <=> NEW.`file_kind`)
    OR NOT (OLD.`infra_file_id` <=> NEW.`infra_file_id`)
    OR NOT (OLD.`original_name` <=> NEW.`original_name`)
    OR NOT (OLD.`mime_type` <=> NEW.`mime_type`)
    OR NOT (OLD.`file_size` <=> NEW.`file_size`)
    OR NOT (OLD.`sha256` <=> NEW.`sha256`)
    OR NOT (OLD.`bound_at` <=> NEW.`bound_at`)
    OR NOT (OLD.`bound_by` <=> NEW.`bound_by`)
    OR NOT (OLD.`deleted` <=> NEW.`deleted`)
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Bound registration certificate file metadata is immutable';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_file_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_file`
FOR EACH ROW
BEGIN
  IF OLD.`bound_at` IS NOT NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Bound registration certificate file cannot be deleted';
  END IF;
END$$

CREATE TRIGGER `trg_dcc_reg_cert_audit_immutable_bu`
BEFORE UPDATE ON `dcc_registration_certificate_audit`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate audit is append-only';
END$$

CREATE TRIGGER `trg_dcc_reg_cert_audit_immutable_bd`
BEFORE DELETE ON `dcc_registration_certificate_audit`
FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Registration certificate audit cannot be deleted';
END$$
DELIMITER ;

CALL assert_dcc_registration_certificate_core_contract();
DROP PROCEDURE IF EXISTS assert_dcc_registration_certificate_core_contract;
