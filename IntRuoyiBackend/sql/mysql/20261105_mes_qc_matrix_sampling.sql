-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260802_mes_qa_inspection_regulation,20260802_mes_pqc_inspection_task; type=schema; riskLevel=medium
-- Schema only. Existing approved regulations/tasks are not converted by this migration.
SET NAMES utf8mb4;
ALTER TABLE mes_qa_inspection_regulation_item
    ADD COLUMN inspection_level VARCHAR(8) DEFAULT NULL COMMENT 'GB/T 2828.1检验水平I/II/III/S1/S2/S3/S4',
    ADD COLUMN aql DECIMAL(18,6) DEFAULT NULL COMMENT '接收质量限，独立于名义样本量计算';
ALTER TABLE mes_pqc_inspection_task
    ADD COLUMN inspection_sampling_plan_json LONGTEXT COMMENT '逐项目冻结矩阵抽样依据';
