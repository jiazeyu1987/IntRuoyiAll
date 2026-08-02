-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_qa_inspection_regulation; type=schema; riskLevel=medium
-- MES M3：PQC 任务身份、QA 规程发布版本快照关系和逐件检验明细

CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `active_order_id` bigint NOT NULL COMMENT '活跃订单ID',
    `work_order_id` bigint NOT NULL COMMENT '生产订单ID',
    `route_id` bigint NOT NULL COMMENT '工艺路线ID',
    `route_version_id` bigint NOT NULL COMMENT '工艺路线版本ID',
    `route_process_id` bigint NOT NULL COMMENT '工艺路线工序ID',
    `process_id` bigint NOT NULL COMMENT '工序ID',
    `regulation_version_id` bigint NOT NULL COMMENT 'QA规程发布版本ID',
    `inspection_type` varchar(32) NOT NULL COMMENT '检验类型：FIRST/PATROL/FINAL',
    `business_date` date NOT NULL COMMENT '业务日期',
    `shift_code` varchar(32) NOT NULL COMMENT '班次编码',
    `round_no` int NOT NULL COMMENT '轮次',
    `planned_inspection_quantity` int NOT NULL COMMENT '计划检验数量',
    `actual_inspection_quantity` int NOT NULL DEFAULT 0 COMMENT '实际检验数量',
    `task_status` varchar(32) NOT NULL COMMENT '任务状态：PENDING/SUBMITTED/CANCELLED',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pqc_task_identity` (`tenant_id`, `active_order_id`, `route_process_id`, `inspection_type`, `business_date`, `shift_code`, `round_no`, `deleted`),
    KEY `idx_mes_pqc_task_regulation_version` (`tenant_id`, `regulation_version_id`),
    KEY `idx_mes_pqc_task_work_order` (`tenant_id`, `work_order_id`, `route_process_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES PQC 检验任务';

CREATE TABLE IF NOT EXISTS `mes_pqc_inspection_piece_detail` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `task_id` bigint NOT NULL COMMENT 'PQC检验任务ID',
    `sample_no` int NOT NULL COMMENT '逐件样本序号',
    `item_code` varchar(64) NOT NULL COMMENT '检验项目编码',
    `item_name` varchar(128) NOT NULL COMMENT '检验项目名称',
    `inspection_method` varchar(512) NOT NULL COMMENT '检验方法',
    `standard_text` varchar(1024) NOT NULL COMMENT '合格标准',
    `result_type` varchar(32) NOT NULL COMMENT '结果类型',
    `item_result` varchar(64) DEFAULT NULL COMMENT '检验结果',
    `measured_value` varchar(128) DEFAULT NULL COMMENT '实测值',
    `judgement` varchar(32) DEFAULT NULL COMMENT '判定',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pqc_piece_item` (`tenant_id`, `task_id`, `sample_no`, `item_code`, `deleted`),
    KEY `idx_mes_pqc_piece_task` (`tenant_id`, `task_id`, `sample_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES PQC 逐件检验明细';
