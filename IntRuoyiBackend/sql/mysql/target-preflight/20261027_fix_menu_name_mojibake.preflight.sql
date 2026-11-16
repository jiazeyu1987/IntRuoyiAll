-- release-target-preflight: migrationId=20261027_fix_menu_name_mojibake; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  ((SELECT COUNT(*) FROM system_menu WHERE id IN (900180,900181,900182,980119,9008440,9008441,9008442,9008443,9008444,9008445,9008446,9008447,9008448,9008449,605071101,605071102) AND deleted=b'0')=16)
THEN 'TARGET_PREFLIGHT_PASS:20261027_fix_menu_name_mojibake' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261027_fix_menu_name_mojibake:BASELINE_CONFLICT' END;
