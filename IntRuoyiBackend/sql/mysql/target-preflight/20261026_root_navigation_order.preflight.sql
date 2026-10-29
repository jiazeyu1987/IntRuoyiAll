-- release-target-preflight: migrationId=20261026_root_navigation_order; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND ((SELECT COUNT(*) FROM system_menu WHERE deleted=b'0' AND status=0 AND type=1 AND ((id=1200 AND path='/approval-center') OR (id=990200 AND path='/mdm') OR (id=980100 AND path IN ('showroom','/showroom')) OR (id=6800 AND path='/dcc') OR (id=900120 AND path IN ('smart-scheduling','/mes/smart-scheduling')) OR (id=1 AND path='/system') OR (id=5100 AND path='/mes')))=7)
THEN 'TARGET_PREFLIGHT_PASS:20261026_root_navigation_order' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_root_navigation_order:BASELINE_CONFLICT' END;
