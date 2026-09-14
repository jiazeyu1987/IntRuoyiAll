-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- MES M6: PQC inspection events use the PQC task as their formal source and must not depend on production submit device context.

ALTER TABLE `mes_pro_process_pool`
    MODIFY COLUMN `device_id` bigint NULL COMMENT '设备ID；PQC_TASK 来源事件可为空',
    MODIFY COLUMN `workstation_id` bigint NULL COMMENT '工位ID；PQC_TASK 来源事件可为空';

ALTER TABLE `mes_pro_process_pool_event`
    MODIFY COLUMN `device_account_id` bigint NULL COMMENT '设备账号ID；PQC_TASK 来源事件可为空',
    MODIFY COLUMN `device_id` bigint NULL COMMENT '设备ID；PQC_TASK 来源事件可为空',
    MODIFY COLUMN `workstation_id` bigint NULL COMMENT '工位ID；PQC_TASK 来源事件可为空';
