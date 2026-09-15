-- release-target-preflight: migrationId=20260829_mes_old_form_template_binding_switch; allowedEnvironments=test,backup,prod
SELECT CASE
  WHEN (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) IN (
        'bpm_form_template_version',
        'jimu_report',
        'jimu_report_category',
        'mes_pro_batch_record_definition',
        'mes_pro_batch_record_report',
        'mes_pro_batch_record_version',
        'mes_pro_route_flow_process_batch_record',
        'mes_pro_route_version'
      )
  ) = 8
  AND (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mes_pro_batch_record_version'
      AND column_name IN (
        'tenant_id',
        'definition_id',
        'version_no',
        'status',
        'source_version_id',
        'source_file_name',
        'source_file_sha256',
        'route_id',
        'source_route_id',
        'approval_instance_id',
        'submitted_by',
        'submitted_at',
        'approved_by',
        'approved_at',
        'reject_reason',
        'remark',
        'creator',
        'create_time',
        'updater',
        'update_time',
        'deleted'
      )
  ) = 21
  AND NOT EXISTS (
    SELECT 1
    FROM mes_pro_route_flow_process_batch_record rb
    LEFT JOIN bpm_form_template_version tv
      ON tv.tenant_id = rb.tenant_id
     AND tv.template_id = rb.form_template_id
     AND tv.id = rb.last_published_template_version_id
     AND tv.deleted = b'0'
    WHERE rb.deleted = b'0'
      AND rb.form_template_id IS NOT NULL
      AND rb.batch_record_report_id IS NULL
      AND (
        tv.id IS NULL
        OR (
          IFNULL(JSON_VALID(tv.jimu_schema_json), 0) <> 1
          AND NOT (
            IFNULL(JSON_VALID(tv.recognized_schema_json), 0) = 1
            AND JSON_LENGTH(tv.recognized_schema_json) > 0
          )
        )
        OR (
          JSON_VALID(tv.jimu_schema_json) = 1
          AND JSON_EXTRACT(tv.jimu_schema_json, '$.sheetLayoutJson') IS NULL
          AND NOT (
            IFNULL(JSON_VALID(tv.recognized_schema_json), 0) = 1
            AND JSON_LENGTH(tv.recognized_schema_json) > 0
            )
          )
        )
        OR (
          JSON_VALID(tv.recognized_schema_json) = 1
          AND JSON_LENGTH(tv.recognized_schema_json) > 0
          AND EXISTS (
            SELECT 1
            FROM JSON_TABLE(
              tv.recognized_schema_json,
              '$[*]' COLUMNS (
                field_code VARCHAR(128) PATH '$.fieldCode' NULL ON EMPTY,
                label VARCHAR(255) PATH '$.label' NULL ON EMPTY
              )
            ) fields
            WHERE COALESCE(NULLIF(TRIM(fields.label), ''), NULLIF(TRIM(fields.field_code), '')) IS NULL
          )
        )
      )
THEN 'TARGET_PREFLIGHT_PASS:20260829_mes_old_form_template_binding_switch'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20260829_mes_old_form_template_binding_switch'
END;
