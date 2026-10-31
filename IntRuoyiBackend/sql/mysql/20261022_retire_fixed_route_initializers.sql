-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; supersedes=20260709_mes_rt000006_batch_record_mapping,20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup,20260717_mes_balloon_excel_device_workstation_binding; type=data; riskLevel=low; requiresTargetPreflight=true
-- These immutable historical operations install a particular RT000006 business route,
-- repair one Excel import, and bind a fixed 49-row Excel data set. They are not schema
-- migrations or application-wide seed data. Replaying them against the current bound
-- 50-process routes would mutate live business configuration to match an obsolete import.
-- Retire only those named operations. Do not insert their APPLIED ledger rows, alter
-- existing route/workstation bindings, or retire the general process-reference migration.
-- A remaining RT000006 installation, orphan reference, or incomplete Excel binding
-- requires explicit technical investigation instead of being silently ignored.
DELIMITER $$
CREATE PROCEDURE release_20261022_retire_fixed_route_initializers()
BEGIN
 IF EXISTS(SELECT 1 FROM mes_pro_route WHERE id=922067 OR code='RT000006')
 OR EXISTS(SELECT 1 FROM mes_pro_route_process WHERE route_id=922067)
 OR EXISTS(SELECT 1 FROM mes_pro_route_flow_config WHERE route_id=922067)
 OR EXISTS(SELECT 1 FROM mes_pro_route_flow_process_config WHERE route_id=922067)
 OR EXISTS(SELECT 1 FROM mes_pro_route_flow_process_batch_record WHERE route_id=922067)
 OR EXISTS(
  SELECT 1 FROM mes_pro_route r
  JOIN mes_pro_route_process rp ON rp.route_id=r.id AND rp.tenant_id=r.tenant_id AND rp.deleted=b'0'
  LEFT JOIN mes_pro_process p ON p.id=rp.process_id AND p.tenant_id=rp.tenant_id AND p.deleted=b'0'
  LEFT JOIN mes_md_workstation w ON w.id=rp.workstation_id AND w.tenant_id=rp.tenant_id AND w.deleted=b'0'
  WHERE r.tenant_id=1 AND r.deleted=b'0' AND r.code IN('ROUTE-XLSX-00001','ROUTE-XLSX-00002')
  AND (p.id IS NULL OR w.id IS NULL OR NOT(w.process_id <=> rp.process_id))
 ) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='FIXED_ROUTE_RETIREMENT_REQUIRES_ABSENT_RT000006_AND_VALID_CURRENT_BINDINGS';
 END IF;
END$$
DELIMITER ;
CALL release_20261022_retire_fixed_route_initializers();
DROP PROCEDURE release_20261022_retire_fixed_route_initializers;
