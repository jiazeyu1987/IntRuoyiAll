-- release-target-preflight: migrationId=20261125_mes_replan_request_receipt; allowedEnvironments=test,backup,prod
SELECT CASE
 WHEN DATABASE() IS NULL THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_mes_replan_request_receipt:DATABASE_MISSING'
 WHEN EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='mes_pro_replan_request') THEN 'TARGET_PREFLIGHT_BLOCKED:20261125_mes_replan_request_receipt:TABLE_ALREADY_EXISTS'
 ELSE 'TARGET_PREFLIGHT_PASS:20261125_mes_replan_request_receipt' END;
