-- release-target-preflight: migrationId=20261117_edhr_data_sync_root_menu; allowedEnvironments=test,backup,prod
SELECT CASE WHEN
 EXISTS (SELECT 1 FROM system_menu WHERE id=990200 AND parent_id=0 AND name='基础数据' AND sort=30 AND type=1 AND deleted=b'0')
 AND EXISTS (SELECT 1 FROM system_menu WHERE id=991511 AND name='数据同步' AND path='/mes/edhr-data-sync' AND type=1 AND status=0 AND deleted=b'0' AND parent_id IN (900220,0))
 AND NOT EXISTS (SELECT 1 FROM system_menu WHERE deleted=b'0' AND id<>991511 AND parent_id=0 AND sort=36)
 THEN 'TARGET_PREFLIGHT_PASS:20261117_edhr_data_sync_root_menu'
 ELSE 'TARGET_PREFLIGHT_BLOCKED:20261117_edhr_data_sync_root_menu:MENU_BASELINE_CONFLICT' END;
