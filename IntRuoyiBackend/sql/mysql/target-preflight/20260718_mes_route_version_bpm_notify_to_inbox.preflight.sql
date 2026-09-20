-- release-target-preflight: migrationId=20260718_mes_route_version_bpm_notify_to_inbox; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) IN ('system_notify_template')) = 1
THEN 'TARGET_PREFLIGHT_PASS:20260718_mes_route_version_bpm_notify_to_inbox' ELSE 'TARGET_PREFLIGHT_BLOCKED:20260718_mes_route_version_bpm_notify_to_inbox' END;
