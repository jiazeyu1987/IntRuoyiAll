-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- 生产工单金蝶同步记录源唯一键必须包含 tenant_id，避免一个租户的源映射阻止其它租户记录映射。

ALTER TABLE `mes_kingdee_production_order_sync_record`
    DROP INDEX `uk_mes_kingdee_production_source`;

ALTER TABLE `mes_kingdee_production_order_sync_record`
    ADD UNIQUE INDEX `uk_mes_kingdee_production_source`
        (`tenant_id`, `source_fid`, `source_material_number`, `deleted`);
