-- release-target-preflight: migrationId=20261026_strip_edhr_child_menu_prefixes; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND type IN (1,2) AND (parent_id=900220 OR id IN (6013,991502,2565,2590,6023,6021,6032,605071318,6025,5110,5130,5120,990210,900365,605071201,900272,5710,5718,5720,900434,991501,900440,5530,900436,900437,900438,900435,605071315,900033,900230,9008300,900432,900025)) AND LOWER(LEFT(name,4))='edhr' AND CHAR_LENGTH(TRIM(SUBSTRING(name,5)))=0))
THEN 'TARGET_PREFLIGHT_PASS:20261026_strip_edhr_child_menu_prefixes' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_strip_edhr_child_menu_prefixes:BASELINE_CONFLICT' END;
