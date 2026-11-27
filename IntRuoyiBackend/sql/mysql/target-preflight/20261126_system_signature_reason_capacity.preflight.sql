-- release-target-preflight: migrationId=20261126_system_signature_reason_capacity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='system_electronic_signature'
    AND COLUMN_NAME='reason' AND COLUMN_TYPE IN ('varchar(500)','varchar(1000)')
    AND IS_NULLABLE='NO' AND CHARACTER_SET_NAME='utf8mb4' AND COLLATION_NAME='utf8mb4_unicode_ci'
)=1 THEN 'TARGET_PREFLIGHT_PASS:20261126_system_signature_reason_capacity'
ELSE 'TARGET_PREFLIGHT_BLOCKED:20261126_system_signature_reason_capacity:REASON_SCHEMA' END;
