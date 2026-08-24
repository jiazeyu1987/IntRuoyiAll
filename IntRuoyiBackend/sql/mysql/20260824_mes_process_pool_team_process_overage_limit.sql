-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260801_mes_process_pool_team_leader_p3_report_allocation; type=schema; riskLevel=medium
-- MES 生产组长工序报工允许超量比例配置

CREATE TABLE IF NOT EXISTS \`mes_pro_process_pool_team_process_overage_limit\` (
    \`id\` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    \`leader_user_id\` bigint NOT NULL COMMENT '生产组长用户ID',
    \`route_process_id\` bigint NOT NULL COMMENT '工艺路线工序ID',
    \`process_id\` bigint NOT NULL COMMENT '工序ID',
    \`overage_percent\` decimal(9,4) NOT NULL COMMENT '允许超量百分比，0-100',
    \`enabled\` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
    \`creator\` varchar(64) DEFAULT '' COMMENT '创建者',
    \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    \`updater\` varchar(64) DEFAULT '' COMMENT '更新者',
    \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    \`deleted\` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    \`tenant_id\` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (\`id\`),
    UNIQUE KEY \`uk_mes_pp_team_overage_limit\` (\`tenant_id\`, \`leader_user_id\`, \`route_process_id\`, \`process_id\`, \`deleted\`),
    KEY \`idx_mes_pp_team_overage_leader\` (\`tenant_id\`, \`leader_user_id\`, \`enabled\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产组长工序允许超量比例';
