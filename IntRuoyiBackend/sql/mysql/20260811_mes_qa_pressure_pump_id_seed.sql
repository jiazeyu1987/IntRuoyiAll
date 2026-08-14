-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope; type=data; riskLevel=high
-- 将 ID / 球囊扩张压力泵 / 112 的前端 QA 固化为 DCC 直接所属的正式发布版本。
-- 回滚范围：仅删除 migrationKey=20260811-ID-PQC-ID-001-G0-v1 对应的版本、工序、项目和 DCC QA 根。

DROP PROCEDURE IF EXISTS seed_mes_qa_pressure_pump_id;
DELIMITER $$
CREATE PROCEDURE seed_mes_qa_pressure_pump_id()
seed_block: BEGIN
  DECLARE v_dcc_match_count int DEFAULT 0;
  DECLARE v_existing_root_count int DEFAULT 0;
  DECLARE v_process_count int DEFAULT 0;
  DECLARE v_logical_item_count int DEFAULT 0;
  DECLARE v_item_row_count int DEFAULT 0;
  DECLARE v_dcc_project_code_id bigint DEFAULT NULL;
  DECLARE v_tenant_id bigint DEFAULT NULL;
  DECLARE v_regulation_id bigint DEFAULT NULL;
  DECLARE v_regulation_version_id bigint DEFAULT NULL;
  DECLARE v_existing_migration_key varchar(128) DEFAULT NULL;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(1)
    INTO v_dcc_match_count
    FROM `dcc_project_code`
   WHERE `project_code` = 'ID'
     AND `project_name` = '球囊扩张压力泵'
     AND `doc_control_no` = '112'
     AND `status` = 'ENABLE'
     AND `deleted` = 0;
  IF v_dcc_match_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'DCC项目业务键必须唯一命中1条';
  END IF;

  SELECT `id`, `tenant_id`
    INTO v_dcc_project_code_id, v_tenant_id
    FROM `dcc_project_code`
   WHERE `project_code` = 'ID'
     AND `project_name` = '球囊扩张压力泵'
     AND `doc_control_no` = '112'
     AND `status` = 'ENABLE'
     AND `deleted` = 0;

  SELECT COUNT(1)
    INTO v_existing_root_count
    FROM `mes_qa_inspection_regulation`
   WHERE `tenant_id` = v_tenant_id
     AND `dcc_project_code_id` = v_dcc_project_code_id
     AND `deleted` = b'0';
  IF v_existing_root_count > 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '已有多个直接归属该DCC项目的QA规程';
  END IF;

  IF v_existing_root_count = 1 THEN
    SELECT regulation.`id`, regulation.`current_version_id`,
           JSON_UNQUOTE(JSON_EXTRACT(version_record.`snapshot_json`, '$.migrationKey'))
      INTO v_regulation_id, v_regulation_version_id, v_existing_migration_key
      FROM `mes_qa_inspection_regulation` regulation
      LEFT JOIN `mes_qa_inspection_regulation_version` version_record
        ON version_record.`id` = regulation.`current_version_id`
       AND version_record.`tenant_id` = regulation.`tenant_id`
       AND version_record.`deleted` = b'0'
     WHERE regulation.`tenant_id` = v_tenant_id
       AND regulation.`dcc_project_code_id` = v_dcc_project_code_id
       AND regulation.`deleted` = b'0';

    SELECT COUNT(1)
      INTO v_process_count
      FROM `mes_qa_inspection_regulation_process`
     WHERE `tenant_id` = v_tenant_id
       AND `regulation_version_id` = v_regulation_version_id
       AND `deleted` = b'0';
    SELECT COUNT(DISTINCT `item_code`), COUNT(1)
      INTO v_logical_item_count, v_item_row_count
      FROM `mes_qa_inspection_regulation_item`
     WHERE `tenant_id` = v_tenant_id
       AND `regulation_version_id` = v_regulation_version_id
       AND `deleted` = b'0';

    IF v_existing_migration_key = '20260811-ID-PQC-ID-001-G0-v1'
       AND v_process_count = 8
       AND v_logical_item_count = 18
       AND v_item_row_count = 51 THEN
      COMMIT;
      LEAVE seed_block;
    END IF;

    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = '已有直接归属该DCC项目的QA规程与本次迁移不一致';
  END IF;

  INSERT INTO `mes_qa_inspection_regulation` (
    `dcc_project_code_id`, `product_id`, `route_id`, `route_version_id`, `route_process_id`, `process_id`,
    `owner_module`, `regulation_code`, `regulation_name`, `lifecycle_status`, `current_version_id`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  ) VALUES (
    v_dcc_project_code_id, NULL, NULL, NULL, NULL, NULL,
    'MES_QA', 'PQC-ID-001', '（椎体）球囊扩张压力泵组装过程检验规程', 'PUBLISHED', NULL,
    'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id
  );
  SET v_regulation_id = LAST_INSERT_ID();

  INSERT INTO `mes_qa_inspection_regulation_version` (
    `regulation_id`, `version_no`, `lifecycle_status`, `effective_date`, `inspection_type_rules_json`,
    `published_at`, `retired_at`, `final_inspection_applicable`,
    `final_inspection_not_applicable_reason`, `snapshot_json`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  ) VALUES (
    v_regulation_id, 'G/0', 'PUBLISHED', '2025-09-30',
    JSON_ARRAY(
      JSON_OBJECT('key', 'FIRST', 'inspectionType', 'FIRST', 'label', '首检', 'required', TRUE,
                  'taskRule', '按发布规程固定数量生成首检任务'),
      JSON_OBJECT('key', 'PATROL_AM', 'inspectionType', 'PATROL', 'label', '上午巡检', 'required', TRUE,
                  'taskRule', '按订单数量 × 上午比例向上取整'),
      JSON_OBJECT('key', 'PATROL_PM', 'inspectionType', 'PATROL', 'label', '下午巡检', 'required', TRUE,
                  'taskRule', '按订单数量 × 下午比例向上取整'),
      JSON_OBJECT('key', 'FINAL', 'inspectionType', 'FINAL', 'label', '末检', 'required', TRUE,
                  'fixedQuantity', 3, 'taskRule', '需要末检时生成末检任务')
    ),
    NOW(), NULL, b'1', NULL,
    JSON_OBJECT(
      'migrationKey', '20260811-ID-PQC-ID-001-G0-v1',
      'source', 'QaRegulationPage.vue hardcoded ID regulation',
      'dccProjectCodeId', v_dcc_project_code_id,
      'regulationCode', 'PQC-ID-001',
      'regulationName', '（椎体）球囊扩张压力泵组装过程检验规程',
      'versionNo', 'G/0',
      'processCount', 8,
      'logicalItemCount', 18
    ),
    'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id
  );
  SET v_regulation_version_id = LAST_INSERT_ID();

  INSERT INTO `mes_qa_inspection_regulation_process` (
    `regulation_version_id`, `process_code`, `process_name`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  ) VALUES
    (v_regulation_version_id, 'ID-QA-001', '清洗', 1, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-002', '精洗', 2, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-003', '清洁', 3, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-004', '组装Ⅰ', 4, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-005', '光固Ⅰ', 5, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-006', '组装Ⅱ / 硅化Ⅰ', 6, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-007', '检测', 7, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id),
    (v_regulation_version_id, 'ID-QA-008', '光固Ⅱ', 8, 'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id);

  CREATE TEMPORARY TABLE `tmp_mes_qa_id_logical_item` (
    `process_code` varchar(64) NOT NULL,
    `item_sort` int NOT NULL,
    `item_code` varchar(64) NOT NULL,
    `item_name` varchar(128) NOT NULL,
    `inspection_method` varchar(512) NOT NULL,
    `inspection_tool` varchar(512) DEFAULT NULL,
    `sampling_plan_text` varchar(512) NOT NULL,
    `standard_text` varchar(1024) NOT NULL,
    `result_type` varchar(32) NOT NULL,
    `first_quantity` int DEFAULT NULL,
    `patrol_ratio` decimal(18,6) NOT NULL,
    `critical` bit(1) NOT NULL,
    `source_page` int NOT NULL,
    `source_item` varchar(512) NOT NULL,
    `source_excerpt` text NOT NULL,
    `source_method` text NOT NULL,
    PRIMARY KEY (`item_code`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

  INSERT INTO `tmp_mes_qa_id_logical_item` VALUES
    ('ID-QA-001', 1, 'ID-001-WASH-APP', '外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     'GB/T 2828.1，I，AQL=0.4', '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
     'BOOLEAN', NULL, 0.4, b'0', 4, '清洗/精洗 / 外观',
     '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-002', 1, 'ID-001-FINE-WASH-APP', '外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     'GB/T 2828.1，I，AQL=0.4', '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
     'BOOLEAN', NULL, 0.4, b'0', 4, '清洗/精洗 / 外观',
     '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-003', 1, 'ID-002-CLEAN-APP', '外观',
     '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     'GB/T 2828.1，I，AQL=0.4', '压力表等清洁后应清洁、无异物、浮尘。',
     'BOOLEAN', NULL, 0.4, b'0', 4, '清洁 / 外观',
     '压力表等清洁后应清洁、无异物、浮尘。',
     '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-004', 1, 'ID-003-ASSEMBLY-I-APP', '外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=0.4', '1）表面应清洁、无黑点、异物、无划伤、无注塑缺陷；2）硅化后齿条、螺盖表面应无成滴的多余硅油；3）组装后芯杆应无多余毛屑。',
     'BOOLEAN', 13, 0.4, b'0', 4, '组装Ⅰ / 外观',
     '1）表面应清洁、无黑点、异物、无划伤、无注塑缺陷；2）硅化后齿条、螺盖表面应无成滴的多余硅油；3）组装后芯杆应无多余毛屑。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-004', 2, 'ID-004-ASSEMBLY-I-RELEASE', '撤压',
     '将待检推杆与专用套筒（吸入 10ML 检测用纯化水）组装，将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上，观察能否顺利撤压。', '撤压机',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上应能顺利撤压。',
     'BOOLEAN', 5, 0.4, b'1', 4, '组装Ⅰ / 撤压',
     '将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上应能顺利撤压。',
     '将待检推杆与专用套筒（吸入 10ML 检测用纯化水）组装，将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上，观察能否顺利撤压。'),
    ('ID-QA-004', 3, 'ID-005-ASSEMBLY-I-NOJUMP', '无跳压',
     '将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水）上，将压力打至 30 atm 应无跳压现象，加压泄压各 5 次；40atm 的压力泵则将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水），压力打至 40 atm 无跳压现象，加压泄压各 5 次。', '/',
     '首件：5 件；GB/T 2828.1，S-3，AQL=1.0', '30atm 的压力泵压力打至 30atm 应无跳压现象；40atm 的压力泵则压力打至 40 atm 无跳压现象。',
     'BOOLEAN', 5, 1.0, b'1', 4, '组装Ⅰ / 无跳压',
     '30atm 的压力泵压力打至 30atm 应无跳压现象；40atm 的压力泵则压力打至 40 atm 无跳压现象。',
     '将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水）上，将压力打至 30 atm 应无跳压现象，加压泄压各 5 次；40atm 的压力泵则将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水），压力打至 40 atm 无跳压现象，加压泄压各 5 次。'),
    ('ID-QA-005', 1, 'ID-006-UV-I-SWIVEL-APP', '光固旋转接头 / 外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=0.4', '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     'BOOLEAN', 13, 0.4, b'0', 5, '光固Ⅰ / 光固旋转接头 / 外观',
     '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-005', 2, 'ID-007-UV-I-SWIVEL-STRENGTH', '光固旋转接头 / 牢固度',
     '用 15N 的砝码悬挂，停留 15s。', '15N 砝码',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
     'BOOLEAN', 5, 0.4, b'1', 5, '光固Ⅰ / 光固旋转接头 / 牢固度',
     '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。', '用 15N 的砝码悬挂，停留 15s。'),
    ('ID-QA-005', 3, 'ID-008-UV-I-GAUGE-APP', '光固压力表 / 外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=0.4', '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     'BOOLEAN', 13, 0.4, b'0', 5, '光固Ⅰ / 光固压力表 / 外观',
     '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-005', 4, 'ID-009-UV-I-GAUGE-STRENGTH', '光固压力表 / 牢固度',
     '用 15N 的砝码悬挂，停留 15s。', '15N 砝码',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
     'BOOLEAN', 5, 0.4, b'1', 5, '光固Ⅰ / 光固压力表 / 牢固度',
     '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。', '用 15N 的砝码悬挂，停留 15s。'),
    ('ID-QA-005', 5, 'ID-010-UV-I-GAUGE-TORQUE', '光固压力表 / 扭力值',
     '使用 5N·m 扭力扳手对连接处进行测试，无松动情况判定合格。', '扭力扳手',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '压力表固化后扭力值＞5N·m。',
     'BOOLEAN', 5, 0.4, b'1', 5, '光固Ⅰ / 光固压力表 / 扭力值',
     '压力表固化后扭力值＞5N·m。', '使用 5N·m 扭力扳手对连接处进行测试，无松动情况判定合格。'),
    ('ID-QA-005', 6, 'ID-011-UV-I-TUBE-APP', '光固延长管 / 外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=0.4', '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     'BOOLEAN', 13, 0.4, b'0', 6, '光固Ⅰ / 光固延长管 / 外观',
     '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-005', 7, 'ID-012-UV-I-TUBE-STRENGTH', '光固延长管 / 牢固度',
     '用 15N 的砝码悬挂，停留 15s。', '15N 砝码',
     '首件：5 件；GB/T 2828.1，S-3，AQL=1.0', '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
     'BOOLEAN', 5, 1.0, b'1', 6, '光固Ⅰ / 光固延长管 / 牢固度',
     '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。', '用 15N 的砝码悬挂，停留 15s。'),
    ('ID-QA-006', 1, 'ID-013-ASSEMBLY-II-APP', '外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=1.0', '组装后产品表面应无黑点、杂质、花纹、划痕等外观缺陷；产品内腔无异物、毛丝等活动异物；配件组装后无挤压形成的多余料丝等现象；胶塞表面应无成滴的硅油汇聚。',
     'BOOLEAN', 13, 1.0, b'0', 6, '组装Ⅱ / 硅化Ⅰ / 外观',
     '组装后产品表面应无黑点、杂质、花纹、划痕等外观缺陷；产品内腔无异物、毛丝等活动异物；配件组装后无挤压形成的多余料丝等现象；胶塞表面应无成滴的硅油汇聚。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-007', 1, 'ID-014-TEST-HIGH-PRESSURE', '高压检测',
     '将组装产品装到气密性检测工装上进行检测。', '气密性检测工装',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '将整体组装产品装到气密性检测工装上，通过大脚接头接上 30atm（30atm 压力泵）/38atm（40atm 压力泵）气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
     'BOOLEAN', 5, 0.4, b'1', 6, '检测 / 高压检测',
     '将整体组装产品装到气密性检测工装上，通过大脚接头接上 30atm（30atm 压力泵）/38atm（40atm 压力泵）气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
     '将组装产品装到气密性检测工装上进行检测。'),
    ('ID-QA-007', 2, 'ID-015-TEST-LOW-PRESSURE', '低压检测',
     '将高压检测合格的压力泵装到气密性检测工装上进行检测。', '气密性检测工装',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '将高压检测合格的压力泵装到气密性检测工装上，通过大脚接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
     'BOOLEAN', 5, 0.4, b'1', 6, '检测 / 低压检测',
     '将高压检测合格的压力泵装到气密性检测工装上，通过大脚接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
     '将高压检测合格的压力泵装到气密性检测工装上进行检测。'),
    ('ID-QA-008', 1, 'ID-016-UV-II-APP', '外观',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。', '目测',
     '首件：13 件；GB/T 2828.1，I，AQL=0.4', '光固位置应整洁均匀圆滑美观；胶水没有污染到其它地方；压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；不应有多余胶水外露。',
     'BOOLEAN', 13, 0.4, b'0', 7, '光固Ⅱ / 外观',
     '光固位置应整洁均匀圆滑美观；胶水没有污染到其它地方；压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；不应有多余胶水外露。',
     '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。'),
    ('ID-QA-008', 2, 'ID-017-UV-II-STRENGTH', '牢固度',
     '用 15N 的砝码悬挂，停留 15s。', '15N 砝码',
     '首件：5 件；GB/T 2828.1，S-3，AQL=0.4', '对连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
     'BOOLEAN', 5, 0.4, b'1', 7, '光固Ⅱ / 牢固度',
     '对连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。', '用 15N 的砝码悬挂，停留 15s。');

  INSERT INTO `mes_qa_inspection_regulation_item` (
    `regulation_version_id`, `qa_process_id`, `item_sort`, `inspection_type`,
    `item_code`, `item_name`, `inspection_method`, `inspection_tool`, `standard_text`,
    `sampling_plan_text`, `standard_lower_limit`, `standard_upper_limit`, `standard_unit`,
    `standard_precision`, `equipment_required`, `result_type`, `first_inspection_quantity`,
    `patrol_inspection_ratio`, `critical`, `failure_rule`, `source_note`, `source_original_page`,
    `source_original_item`, `source_original_excerpt`, `source_original_method`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
  )
  SELECT v_regulation_version_id, process_record.`id`, source_item.`item_sort`, inspection_kind.`inspection_type`,
         source_item.`item_code`, source_item.`item_name`, source_item.`inspection_method`, source_item.`inspection_tool`,
         source_item.`standard_text`, source_item.`sampling_plan_text`, NULL, NULL, NULL, NULL, b'0',
         source_item.`result_type`,
         CASE inspection_kind.`inspection_type`
           WHEN 'FIRST' THEN source_item.`first_quantity`
           WHEN 'FINAL' THEN 3
           ELSE NULL
         END,
         CASE WHEN inspection_kind.`inspection_type` = 'PATROL' THEN source_item.`patrol_ratio` ELSE NULL END,
         source_item.`critical`,
         '检验中，每一个检验项目均应合格；若出现不合格，则按不合格品评审结果处理。',
         '用户指定 PDF PQC-ID-001（G/0）5.1 检验内容。',
         source_item.`source_page`, source_item.`source_item`, source_item.`source_excerpt`, source_item.`source_method`,
         'migration:20260811-qa-dcc-id', NOW(), 'migration:20260811-qa-dcc-id', NOW(), b'0', v_tenant_id
    FROM `tmp_mes_qa_id_logical_item` source_item
    JOIN `mes_qa_inspection_regulation_process` process_record
      ON process_record.`tenant_id` = v_tenant_id
     AND process_record.`regulation_version_id` = v_regulation_version_id
     AND process_record.`process_code` = source_item.`process_code`
     AND process_record.`deleted` = b'0'
    JOIN (
      SELECT 'FIRST' AS inspection_type
      UNION ALL SELECT 'PATROL'
      UNION ALL SELECT 'FINAL'
    ) inspection_kind
      ON inspection_kind.`inspection_type` <> 'FIRST'
      OR source_item.`first_quantity` IS NOT NULL;

  SELECT COUNT(1)
    INTO v_process_count
    FROM `mes_qa_inspection_regulation_process`
   WHERE `tenant_id` = v_tenant_id
     AND `regulation_version_id` = v_regulation_version_id
     AND `deleted` = b'0';
  IF v_process_count <> 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'QA工序迁移结果必须为8条';
  END IF;

  SELECT COUNT(DISTINCT `item_code`), COUNT(1)
    INTO v_logical_item_count, v_item_row_count
    FROM `mes_qa_inspection_regulation_item`
   WHERE `tenant_id` = v_tenant_id
     AND `regulation_version_id` = v_regulation_version_id
     AND `deleted` = b'0';
  IF v_logical_item_count <> 18 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(DISTINCT `item_code`) <> 18';
  END IF;
  IF v_item_row_count <> 51 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(1) <> 51';
  END IF;
  IF (
    SELECT COUNT(DISTINCT `qa_process_id`)
      FROM `mes_qa_inspection_regulation_item`
     WHERE `tenant_id` = v_tenant_id
       AND `regulation_version_id` = v_regulation_version_id
       AND `deleted` = b'0'
  ) <> 8 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'COUNT(DISTINCT `qa_process_id`) <> 8';
  END IF;

  UPDATE `mes_qa_inspection_regulation`
     SET `current_version_id` = v_regulation_version_id,
         `updater` = 'migration:20260811-qa-dcc-id',
         `update_time` = NOW()
   WHERE `id` = v_regulation_id
     AND `tenant_id` = v_tenant_id
     AND `deleted` = b'0';

  DROP TEMPORARY TABLE `tmp_mes_qa_id_logical_item`;
  COMMIT;
END$$
DELIMITER ;

CALL seed_mes_qa_pressure_pump_id();

DROP PROCEDURE IF EXISTS seed_mes_qa_pressure_pump_id;
