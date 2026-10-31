-- release-target-preflight: migrationId=20260827_system_user_login_lock; allowedEnvironments=test,backup,prod
-- Existing lock columns must match the additive migration, and dictionary value 21 must be unambiguous.
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='system_users' AND column_name='status')=1
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='system_users' AND (
      (column_name='login_failure_count' AND (data_type<>'int' OR is_nullable<>'NO' OR column_default IS NULL OR column_default<>'0'))
      OR (column_name='login_locked' AND (data_type<>'tinyint' OR is_nullable<>'NO' OR column_default IS NULL OR column_default<>'0'))
      OR (column_name='login_locked_time' AND (data_type<>'datetime' OR is_nullable<>'YES'))
    )
  )
  AND (SELECT COUNT(*) FROM system_dict_data WHERE dict_type='system_login_result' AND value='21')<=1
  AND NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='system_login_result' AND value='21' AND (deleted<>b'0' OR status<>0 OR label<>'账号已锁定'))
  THEN 'TARGET_PREFLIGHT_PASS:20260827_system_user_login_lock'
  ELSE 'TARGET_PREFLIGHT_BLOCKED:login-lock-schema-or-dictionary-conflict' END;
