-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope,20260731_mes_process_pool_team_leader_p1_runtime_config,20260802_mes_pqc_inspection_task,20260730_mes_process_pool_foundation; supersedes=20260812_mes_pqc_dcc_qa_c00_preflight,20260812_mes_pqc_dcc_qa_c00_backfill,20260812_mes_pqc_dcc_qa_c00_rollback; type=data; riskLevel=high; requiresTargetPreflight=true; applyOrder=10
-- Application activation requires no historical C00 data to reconstruct.
-- Non-empty histories require separately approved ID manifests; this migration
-- neither supplies empty approvals nor marks the old dry-run scripts APPLIED.
-- QA roots/versions and unrelated process-pool events are permitted.
DROP PROCEDURE IF EXISTS release_20261022_c00_empty_history;
DELIMITER $$
CREATE PROCEDURE release_20261022_c00_empty_history()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name IN ('mes_pro_process_pool_active_order','mes_pqc_inspection_task','mes_pro_process_pool_event')) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C00_ACTIVATION_BASE_TABLES_REQUIRED';
  END IF;
  IF EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order)
     OR EXISTS (SELECT 1 FROM mes_pqc_inspection_task)
     OR EXISTS (SELECT 1 FROM mes_pro_process_pool_event WHERE deleted=b'0'
       AND event_type='PQC_INSPECTION' AND feedback_source_type='MES_PQC_INSPECTION_TASK') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C00_HISTORY_REQUIRES_APPROVED_ID_MANIFESTS';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
      AND table_name='mes_pro_route_dcc_project_binding') THEN
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='mes_pro_route_dcc_project_binding' AND column_name IN ('tenant_id','route_id','deleted')) <> 3 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C00_EXISTING_ROUTE_BINDING_STRUCTURE_INVALID';
    END IF;
    SET @release_c00_route_duplicates=0;
    SET @release_c00_check='SELECT COUNT(*) INTO @release_c00_route_duplicates FROM (SELECT tenant_id,route_id FROM mes_pro_route_dcc_project_binding WHERE deleted=b''0'' GROUP BY tenant_id,route_id HAVING COUNT(*)>1) duplicates';
    PREPARE release_c00_stmt FROM @release_c00_check;
    EXECUTE release_c00_stmt;
    DEALLOCATE PREPARE release_c00_stmt;
    IF @release_c00_route_duplicates>0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='C00_DUPLICATE_CURRENT_ROUTE_BINDINGS';
    END IF;
  END IF;
END$$
DELIMITER ;
CALL release_20261022_c00_empty_history();
DROP PROCEDURE release_20261022_c00_empty_history;
