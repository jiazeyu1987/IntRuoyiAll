SET @production_line_column_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'mes_md_workstation'
              AND column_name = 'production_line_id'
        ),
        'DO 0',
        'ALTER TABLE `mes_md_workstation` ADD COLUMN `production_line_id` bigint NULL COMMENT ''production line id'' AFTER `process_id`'
    )
);
PREPARE production_line_column_stmt FROM @production_line_column_sql;
EXECUTE production_line_column_stmt;
DEALLOCATE PREPARE production_line_column_stmt;

CREATE TABLE IF NOT EXISTS `mes_md_production_line` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `code` varchar(64) DEFAULT NULL COMMENT 'line code',
    `name` varchar(255) DEFAULT NULL COMMENT 'line name',
    `workshop_id` bigint DEFAULT NULL COMMENT 'workshop id',
    `calendar_plan_id` bigint DEFAULT NULL COMMENT 'calendar plan id',
    `status` tinyint DEFAULT NULL COMMENT 'status',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_md_production_line_code` (`code`),
    UNIQUE KEY `uk_mes_md_production_line_name` (`name`),
    KEY `idx_mes_md_production_line_workshop_id` (`workshop_id`)
) COMMENT='MES production line';

CREATE TABLE IF NOT EXISTS `mes_pro_capacity_plan` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `line_id` bigint NOT NULL COMMENT 'line id',
    `calendar_date` datetime NOT NULL COMMENT 'calendar date',
    `shift_id` bigint NOT NULL COMMENT 'shift id',
    `capacity_minutes` int NOT NULL COMMENT 'capacity minutes',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT 'enabled',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_capacity_plan_scope` (`line_id`, `calendar_date`, `shift_id`),
    KEY `idx_mes_pro_capacity_plan_date` (`calendar_date`)
) COMMENT='MES planned capacity';

CREATE TABLE IF NOT EXISTS `mes_pro_capacity_actual` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `line_id` bigint NOT NULL COMMENT 'line id',
    `calendar_date` datetime NOT NULL COMMENT 'calendar date',
    `shift_id` bigint NOT NULL COMMENT 'shift id',
    `capacity_minutes` int NOT NULL COMMENT 'capacity minutes',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT 'enabled',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_capacity_actual_scope` (`line_id`, `calendar_date`, `shift_id`),
    KEY `idx_mes_pro_capacity_actual_date` (`calendar_date`)
) COMMENT='MES actual capacity';

CREATE TABLE IF NOT EXISTS `mes_pro_task_schedule_ext` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `task_id` bigint NOT NULL COMMENT 'task id',
    `schedule_source` varchar(32) NOT NULL COMMENT 'schedule source',
    `locked` bit(1) NOT NULL DEFAULT b'0' COMMENT 'locked',
    `locked_reason` varchar(255) DEFAULT NULL COMMENT 'locked reason',
    `generated_request_id` varchar(128) DEFAULT NULL COMMENT 'generated request id',
    `risk_status` varchar(32) DEFAULT NULL COMMENT 'risk status',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_task_schedule_ext_task_id` (`task_id`)
) COMMENT='MES task schedule extension';

INSERT INTO `mes_pro_task_schedule_ext`
(`task_id`, `schedule_source`, `locked`, `risk_status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT `id`, 'MANUAL', b'0', 'NONE', 'system', NOW(), 'system', NOW(), b'0', IFNULL(`tenant_id`, 0)
FROM `mes_pro_task` t
WHERE NOT EXISTS (
    SELECT 1
    FROM `mes_pro_task_schedule_ext` ext
    WHERE ext.`task_id` = t.`id`
);

CREATE TABLE IF NOT EXISTS `mes_pro_task_dependency` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `source_task_id` bigint NOT NULL COMMENT 'source task id',
    `target_task_id` bigint NOT NULL COMMENT 'target task id',
    `source_process_id` bigint DEFAULT NULL COMMENT 'source process id',
    `target_process_id` bigint DEFAULT NULL COMMENT 'target process id',
    `dependency_type` varchar(32) DEFAULT NULL COMMENT 'dependency type',
    `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT 'enabled',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pro_task_dependency_source_task_id` (`source_task_id`),
    KEY `idx_mes_pro_task_dependency_target_task_id` (`target_task_id`)
) COMMENT='MES task dependency';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_issue` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `issue_type` varchar(64) NOT NULL COMMENT 'issue type',
    `severity` varchar(32) NOT NULL COMMENT 'severity',
    `work_order_id` bigint DEFAULT NULL COMMENT 'work order id',
    `task_id` bigint DEFAULT NULL COMMENT 'task id',
    `process_id` bigint DEFAULT NULL COMMENT 'process id',
    `workstation_id` bigint DEFAULT NULL COMMENT 'workstation id',
    `material_id` bigint DEFAULT NULL COMMENT 'material id',
    `calendar_date` datetime DEFAULT NULL COMMENT 'calendar date',
    `shift_id` bigint DEFAULT NULL COMMENT 'shift id',
    `required_qty` decimal(14, 2) DEFAULT NULL COMMENT 'required quantity',
    `available_qty` decimal(14, 2) DEFAULT NULL COMMENT 'available quantity',
    `shortage_qty` decimal(14, 2) DEFAULT NULL COMMENT 'shortage quantity',
    `message` varchar(500) DEFAULT NULL COMMENT 'message',
    `resolved` bit(1) NOT NULL DEFAULT b'0' COMMENT 'resolved',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    KEY `idx_mes_pro_schedule_issue_work_order_id` (`work_order_id`),
    KEY `idx_mes_pro_schedule_issue_task_id` (`task_id`),
    KEY `idx_mes_pro_schedule_issue_severity` (`severity`)
) COMMENT='MES current schedule issue';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_calendar_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `skip_statutory_holidays` bit(1) NOT NULL DEFAULT b'0' COMMENT 'skip statutory holidays',
    `weekend_rest_mode` varchar(16) NOT NULL COMMENT 'DOUBLE/SINGLE/NONE',
    `date_shift_mode_by_date_json` text NULL COMMENT 'per-date shift mode json',
    `temporary_freeze_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT 'temporary freeze enabled',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_schedule_calendar_rule_tenant_id` (`tenant_id`)
) COMMENT='MES schedule calendar rule';

CREATE TABLE IF NOT EXISTS `mes_pro_schedule_calendar_simulation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `simulation_date` datetime NOT NULL COMMENT 'simulation current date',
    `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mes_pro_schedule_calendar_simulation_tenant_id` (`tenant_id`)
) COMMENT='MES schedule calendar simulation state';

SET @schedule_calendar_simulation_date_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'mes_pro_schedule_calendar_simulation'
              AND column_name = 'current_date'
        ) AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'mes_pro_schedule_calendar_simulation'
              AND column_name = 'simulation_date'
        ),
        'ALTER TABLE `mes_pro_schedule_calendar_simulation` CHANGE COLUMN `current_date` `simulation_date` datetime NOT NULL COMMENT ''simulation current date''',
        'DO 0'
    )
);
PREPARE schedule_calendar_simulation_date_stmt FROM @schedule_calendar_simulation_date_sql;
EXECUTE schedule_calendar_simulation_date_stmt;
DEALLOCATE PREPARE schedule_calendar_simulation_date_stmt;
