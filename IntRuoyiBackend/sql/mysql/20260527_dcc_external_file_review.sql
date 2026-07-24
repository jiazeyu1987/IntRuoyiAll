-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS `dcc_external_file_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `controlled_file_id` bigint NOT NULL COMMENT 'DCC 主记录编号',
  `external_source` varchar(128) NOT NULL COMMENT '外来来源',
  `external_owner` varchar(128) NOT NULL COMMENT '外来归属或责任方',
  `review_reason` varchar(500) NOT NULL COMMENT '评审原因',
  `participant_user_ids` varchar(500) NOT NULL COMMENT '参与人用户编号，逗号分隔',
  `review_conclusion` varchar(64) DEFAULT NULL COMMENT '评审结论',
  `conclusion_comment` varchar(1000) DEFAULT NULL COMMENT '结论说明',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出物文件编号',
  `closed_time` datetime DEFAULT NULL COMMENT '闭环时间',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_external_file_review_file` (`controlled_file_id`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 外来文件评审扩展';

-- R07 uses an independent BPM definition key. Deploy the definition per tenant before enabling the feature.
-- Required key: dcc-external-file-review
