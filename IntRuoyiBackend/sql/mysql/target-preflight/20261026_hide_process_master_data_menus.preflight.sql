-- release-target-preflight: migrationId=20261026_hide_process_master_data_menus; allowedEnvironments=test,backup,prod
-- Inspect the current baseline before pending dependencies run. Migration-time guards remain authoritative.
SELECT CASE WHEN
  (EXISTS (SELECT 1 FROM system_menu WHERE id=900220 AND type=1 AND status=0 AND deleted=b'0' AND ((parent_id=5100 AND path='edhr-batch-processing') OR (parent_id=0 AND path='/mes/edhr-batch-processing'))))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5110 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/md/item/type/index') AND BINARY path IN ('item-type','/mes/md/item-type')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5130 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/md/unitmeasure/index') AND BINARY path IN ('unit-measure','/mes/md/unit-measure')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5120 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/md/item/index') AND BINARY path IN ('item','/mes/md/item')))
  AND (EXISTS (SELECT 1 FROM system_menu WHERE id=5718 AND deleted=b'0' AND status=0 AND type=2 AND BINARY component IN ('mes/pro/mes-process/index') AND BINARY path IN ('mes-process','/mes/pro/mes-process')))
THEN 'TARGET_PREFLIGHT_PASS:20261026_hide_process_master_data_menus' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261026_hide_process_master_data_menus:BASELINE_CONFLICT' END;
