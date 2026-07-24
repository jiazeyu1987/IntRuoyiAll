-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260611_mes_edhr_work_task_flow; type=permission; riskLevel=low
-- superseded by 20260718_system_entitlement_management.sql
-- scope: eDHR filler permissions are granted through the dynamic entitlement ledger.
-- Static role and direct user binding paths are intentionally disabled.

SET NAMES utf8mb4;
START TRANSACTION;

SELECT 'MES_EDHR_FILLER_MINIMAL is seeded in system_entitlement_policy by 20260718_system_entitlement_management.sql' AS migration_notice;

COMMIT;
