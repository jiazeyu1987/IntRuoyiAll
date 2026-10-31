-- release-target-preflight: migrationId=20261023_mes_team_leader_cleanup_identity; allowedEnvironments=test,backup,prod
SELECT CASE WHEN ((SELECT COUNT(*) FROM system_menu WHERE id=900436 AND deleted=0 AND status=0 AND type=2
 AND permission='mes:pro-process-pool-team-leader:query'
 AND path='/mes/pro/process-pool/production-leader'
 AND component='mes/pro/processpool/ProductionLeaderWorkbenchPage')=1 AND (SELECT COUNT(*) FROM system_menu WHERE id=991613 AND permission='mes:pro-process-pool-team-leader:maintain' AND type=3 AND deleted=0 AND status=0 AND parent_id=900436)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-process-pool-team-leader:maintain' AND deleted=0)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE id=900451 AND permission='mes:pro-process-pool-team-leader:release-apply' AND type=3 AND deleted=0 AND status=0 AND parent_id IN(900310,900436))=1
 AND (SELECT COUNT(*) FROM system_menu WHERE permission='mes:pro-process-pool-team-leader:release-apply' AND deleted=0)=1
 AND (SELECT COUNT(*) FROM system_menu WHERE id=900452 AND permission='mes:pro-process-pool-team-leader:version-upgrade' AND type=3 AND deleted=0 AND status=0 AND parent_id=900436)=1) AND NOT EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_schema=DATABASE() AND routine_name='ensure_mes_team_leader_cleanup_identity')
THEN 'TARGET_PREFLIGHT_PASS:20261023_mes_team_leader_cleanup_identity' ELSE 'TARGET_PREFLIGHT_BLOCKED:20261023_mes_team_leader_cleanup_identity:PREREQUISITE_OR_IDENTITY_CONFLICT' END;
