-- Task-owned prerequisites only. Production and PQC events must be created through their real frontend pages.
DROP PROCEDURE IF EXISTS codx_pqc_20260807_apply;
DELIMITER $$
CREATE PROCEDURE codx_pqc_20260807_apply()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*) INTO v_count FROM mes_pqc_inspection_task
     WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='task fixture already exists'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pro_process_pool_event
     WHERE tenant_id=1 AND deleted=b'0' AND event_idempotency_key LIKE 'CODX-PQC-20260807%';
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='formal submissions already exist'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pro_process_pool_active_order
     WHERE id=30 AND tenant_id=1 AND deleted=b'0' AND work_order_id=980019
       AND route_id=922119 AND route_version_id=448 AND active_status='ACTIVE';
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='active PQC fixture order 30 is missing'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pro_work_order
     WHERE id=980019 AND tenant_id=1 AND deleted=b'0' AND code='PQC-E2E-FS-20260804'
       AND product_id=902149 AND quantity=100 AND status=1;
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='work order 980019 prerequisite is missing'; END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_qa_inspection_regulation regulation
      JOIN mes_qa_inspection_regulation_version version
        ON version.id=regulation.current_version_id AND version.tenant_id=regulation.tenant_id AND version.deleted=b'0'
     WHERE regulation.id=16 AND regulation.tenant_id=1 AND regulation.deleted=b'0'
       AND regulation.product_id=902149 AND regulation.route_id=922119 AND regulation.route_version_id=448
       AND regulation.route_process_id=928609 AND regulation.process_id=922985
       AND regulation.current_version_id=16 AND regulation.lifecycle_status='PUBLISHED'
       AND version.lifecycle_status='PUBLISHED';
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published QA regulation 16 is missing'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_qa_inspection_regulation_item
     WHERE tenant_id=1 AND deleted=b'0' AND regulation_version_id=16 AND inspection_type='PATROL'
       AND item_code IS NOT NULL AND item_name IS NOT NULL AND inspection_method IS NOT NULL
       AND standard_text IS NOT NULL AND result_type IS NOT NULL AND equipment_required=b'1';
    IF v_count < 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='structured PATROL QA items are missing'; END IF;

    SELECT
      (SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=1 AND deleted=b'0' AND active_order_id=30 AND route_process_id=928609 AND process_id=922985)
      + (SELECT COUNT(*) FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND work_order_id=980019 AND route_id=922119 AND process_id=922985)
      + (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND business_object_id=980019 AND recordbook_type='PRODUCTION')
      + (SELECT COUNT(*) FROM mes_pro_process_pool_team_leader_scope WHERE tenant_id=1 AND deleted=b'0' AND leader_user_id=1520 AND leader_type='PRODUCTION' AND scope_type='ORDER' AND work_order_id=980019)
      INTO v_count;
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='task-owned production prerequisite collision'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pqc_inspection_task
     WHERE tenant_id=1 AND active_order_id=30 AND route_process_id=928609 AND inspection_type='PATROL'
       AND business_date='2026-08-03' AND shift_code='CODX5' AND round_no BETWEEN 80701 AND 80705;
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='task identity collision'; END IF;

    START TRANSACTION;
    INSERT INTO mes_pro_process_pool_active_order_process_snapshot (
      active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,
      erp_fixed_quantity_snapshot,production_quantity_factor_snapshot,planned_quantity_snapshot,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES (30,980019,922119,448,928609,922985,100,1,100,
      'CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    INSERT INTO mes_pro_process_pool_team_leader_scope (
      leader_user_id,leader_type,scope_type,work_order_id,enabled,remark,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES (1520,'PRODUCTION','ORDER',980019,b'1','Task-owned real production source scope',
      'CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    INSERT INTO mes_pro_task (
      code,name,work_order_id,workstation_id,route_id,process_id,item_id,quantity,
      start_time,duration,end_time,color_code,status,remark,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES ('CODX-PQC-20260807-PROD-TASK','PQC source production task',980019,980010,922119,922985,902149,5,
      '2026-08-07 08:00:00',60,'2026-08-07 09:00:00','#00AEF3',0,'Task-owned real production source',
      'CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    INSERT INTO mes_pro_edhr_recordbook (
      recordbook_code,recordbook_name,template_id,template_code,template_name,template_version,
      recordbook_type,status,owner_user_id,business_scope,business_object_type,business_object_id,
      business_object_code,opened_at,entry_count,remark,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES ('CODX-PQC-20260807-PRODUCTION-RB','PQC source production recordbook',980010,
      'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1',
      'PRODUCTION','OPEN',964,'PQC5_E2E','WORK_ORDER',980019,'PQC-E2E-FS-20260804',NOW(),0,
      'Task-owned real production source recordbook','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    INSERT INTO mes_pqc_inspection_task (
      active_order_id,work_order_id,route_id,route_version_id,route_process_id,process_id,
      regulation_version_id,inspection_type,business_date,shift_code,round_no,
      planned_inspection_quantity,actual_inspection_quantity,task_status,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES
      (30,980019,922119,448,928609,922985,16,'PATROL','2026-08-03','CODX5',80701,5,0,'PENDING','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      (30,980019,922119,448,928609,922985,16,'PATROL','2026-08-03','CODX5',80702,5,0,'PENDING','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      (30,980019,922119,448,928609,922985,16,'PATROL','2026-08-03','CODX5',80703,5,0,'PENDING','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      (30,980019,922119,448,928609,922985,16,'PATROL','2026-08-03','CODX5',80704,5,0,'PENDING','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      (30,980019,922119,448,928609,922985,16,'PATROL','2026-08-03','CODX5',80705,5,0,'PENDING','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    SELECT
      (SELECT COUNT(*) FROM mes_pro_process_pool_active_order_process_snapshot WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_process_pool_team_leader_scope WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pqc_inspection_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      INTO v_count;
    IF v_count <> 9 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture must create exactly nine prerequisite rows'; END IF;
    COMMIT;
END$$
DELIMITER ;
CALL codx_pqc_20260807_apply();
DROP PROCEDURE codx_pqc_20260807_apply;

SELECT 'PRODUCTION_TASK',id FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
SELECT 'RECORDBOOK',id FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807';
SELECT 'PQC_TASK',id FROM mes_pqc_inspection_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' ORDER BY round_no;
