-- Task-owned production prerequisites only. Formal production and PQC events must come from their real frontend pages.
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

    SELECT COUNT(*) INTO v_count FROM mes_pro_process_pool_event
     WHERE tenant_id=1 AND deleted=b'0' AND event_idempotency_key LIKE 'CODX-PQC-20260807%';
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='formal submissions already exist'; END IF;

    SELECT
      (SELECT COUNT(*) FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      INTO v_count;
    IF v_count <> 0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='task-owned prerequisite collision'; END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_pro_process_pool_active_order active_order
      JOIN mes_pro_work_order work_order
        ON work_order.id=active_order.work_order_id AND work_order.tenant_id=active_order.tenant_id AND work_order.deleted=b'0'
     WHERE active_order.tenant_id=1 AND active_order.deleted=b'0' AND active_order.active_status='ACTIVE'
       AND active_order.route_id=980091 AND active_order.route_version_id=622
       AND (active_order.id, active_order.work_order_id, work_order.code) IN (
         (35,980022,'CODX-AO5-20260807-01'),(36,980023,'CODX-AO5-20260807-02'),
         (37,980024,'CODX-AO5-20260807-03'),(38,980025,'CODX-AO5-20260807-04'),
         (39,980026,'CODX-AO5-20260807-05'))
       AND work_order.product_id=924008 AND work_order.quantity=10 AND work_order.status=1;
    IF v_count <> 5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='five formal active orders are missing'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pro_route_version
     WHERE id=622 AND tenant_id=1 AND deleted=b'0' AND route_id=980091
       AND active=b'1' AND lifecycle_status='ACTIVE';
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='active route version 622 is missing'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pro_route_process
     WHERE id=980631 AND tenant_id=1 AND deleted=b'0' AND route_id=980091
       AND process_id=922985 AND workstation_id=980010;
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='formal route process/workstation binding is missing'; END IF;

    SELECT COUNT(*) INTO v_count
      FROM system_users user
      JOIN system_user_post user_post
        ON user_post.user_id=user.id AND user_post.tenant_id=user.tenant_id AND user_post.deleted=b'0'
      JOIN mes_md_workstation_worker worker
        ON worker.post_id=user_post.post_id AND worker.tenant_id=user.tenant_id AND worker.deleted=b'0'
     WHERE user.id=659 AND user.username='shangmengying' AND user.tenant_id=1 AND user.deleted=b'0' AND user.status=0
       AND user_post.post_id=14 AND worker.workstation_id=980010;
    IF v_count <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='post-bound production device account 659 is missing'; END IF;

    SELECT COUNT(*) INTO v_count
      FROM mes_qa_inspection_regulation regulation
      JOIN mes_qa_inspection_regulation_version version
        ON version.id=regulation.current_version_id AND version.tenant_id=regulation.tenant_id AND version.deleted=b'0'
      JOIN mes_qa_inspection_regulation_item item
        ON item.regulation_version_id=version.id AND item.tenant_id=version.tenant_id AND item.deleted=b'0'
     WHERE regulation.id=36 AND regulation.tenant_id=1 AND regulation.deleted=b'0'
       AND regulation.product_id=924008 AND regulation.route_id=980091 AND regulation.route_version_id=622
       AND regulation.route_process_id=980631 AND regulation.process_id=922985
       AND regulation.current_version_id=36 AND regulation.lifecycle_status='PUBLISHED'
       AND version.lifecycle_status='PUBLISHED' AND item.inspection_type='FINAL'
       AND item.item_code IS NOT NULL AND item.item_name IS NOT NULL
       AND item.inspection_method IS NOT NULL AND item.standard_text IS NOT NULL AND item.result_type IS NOT NULL;
    IF v_count < 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='published structured FINAL QA regulation is missing'; END IF;

    SELECT COUNT(*) INTO v_count FROM mes_pqc_inspection_task
     WHERE tenant_id=1 AND deleted=b'0' AND task_status='PENDING'
       AND inspection_type='FINAL' AND regulation_version_id=36
       AND route_id=980091 AND route_version_id=622 AND route_process_id=980631 AND process_id=922985
       AND (id,active_order_id,work_order_id,planned_inspection_quantity) IN (
         (198,35,980022,3),(202,36,980023,3),(206,37,980024,3),(210,38,980025,3),(214,39,980026,3));
    IF v_count <> 5 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='five formal pending PQC tasks are missing'; END IF;

    START TRANSACTION;
    INSERT INTO mes_pro_task (
      code,name,work_order_id,workstation_id,route_id,process_id,item_id,quantity,
      start_time,duration,end_time,color_code,status,remark,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES
      ('CODX-PQC-20260807-PROD-01','PQC source production task 01',980022,980010,980091,922985,924008,10,'2026-08-07 08:00:00',60,'2026-08-07 09:00:00','#00AEF3',0,'Task-owned real production source 01','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-PROD-02','PQC source production task 02',980023,980010,980091,922985,924008,10,'2026-08-08 08:00:00',60,'2026-08-08 09:00:00','#00AEF3',0,'Task-owned real production source 02','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-PROD-03','PQC source production task 03',980024,980010,980091,922985,924008,10,'2026-08-09 08:00:00',60,'2026-08-09 09:00:00','#00AEF3',0,'Task-owned real production source 03','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-PROD-04','PQC source production task 04',980025,980010,980091,922985,924008,10,'2026-08-10 08:00:00',60,'2026-08-10 09:00:00','#00AEF3',0,'Task-owned real production source 04','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-PROD-05','PQC source production task 05',980026,980010,980091,922985,924008,10,'2026-08-11 08:00:00',60,'2026-08-11 09:00:00','#00AEF3',0,'Task-owned real production source 05','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    INSERT INTO mes_pro_edhr_recordbook (
      recordbook_code,recordbook_name,template_id,template_code,template_name,template_version,
      recordbook_type,status,owner_user_id,business_scope,business_object_type,business_object_id,
      business_object_code,opened_at,entry_count,remark,
      creator,create_time,updater,update_time,deleted,tenant_id
    ) VALUES
      ('CODX-PQC-20260807-RB-01','PQC source production recordbook 01',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',659,'PQC5_E2E','WORK_ORDER',980022,'CODX-AO5-20260807-01',NOW(),0,'Task-owned source recordbook 01','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-RB-02','PQC source production recordbook 02',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',659,'PQC5_E2E','WORK_ORDER',980023,'CODX-AO5-20260807-02',NOW(),0,'Task-owned source recordbook 02','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-RB-03','PQC source production recordbook 03',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',659,'PQC5_E2E','WORK_ORDER',980024,'CODX-AO5-20260807-03',NOW(),0,'Task-owned source recordbook 03','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-RB-04','PQC source production recordbook 04',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',659,'PQC5_E2E','WORK_ORDER',980025,'CODX-AO5-20260807-04',NOW(),0,'Task-owned source recordbook 04','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1),
      ('CODX-PQC-20260807-RB-05','PQC source production recordbook 05',980010,'RRM-20260801-PRODUCTION-RECORD-TPL','RRM production recordbook template','V1','PRODUCTION','OPEN',659,'PQC5_E2E','WORK_ORDER',980026,'CODX-AO5-20260807-05',NOW(),0,'Task-owned source recordbook 05','CODX-PQC-20260807',NOW(),'CODX-PQC-20260807',NOW(),b'0',1);

    SELECT
      (SELECT COUNT(*) FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      + (SELECT COUNT(*) FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807')
      INTO v_count;
    IF v_count <> 10 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture must create exactly ten prerequisite rows'; END IF;
    COMMIT;
END$$
DELIMITER ;
CALL codx_pqc_20260807_apply();
DROP PROCEDURE codx_pqc_20260807_apply;

SELECT 'PRODUCTION_TASK',id,work_order_id FROM mes_pro_task WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' ORDER BY work_order_id;
SELECT 'RECORDBOOK',id,business_object_id FROM mes_pro_edhr_recordbook WHERE tenant_id=1 AND deleted=b'0' AND creator='CODX-PQC-20260807' ORDER BY business_object_id;
