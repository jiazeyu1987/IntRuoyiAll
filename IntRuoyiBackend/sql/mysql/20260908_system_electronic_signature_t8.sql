-- T8: electronic signature trusted time, seal, privileged audit and archive recovery evidence.

CREATE TABLE IF NOT EXISTS `system_electronic_signature_time_evidence` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL,
    `trusted_time_source` varchar(128) NOT NULL,
    `source_reported_at` datetime NOT NULL,
    `server_observed_at` datetime NOT NULL,
    `drift_millis` bigint NOT NULL,
    `evidence_hash` char(64) NOT NULL,
    `status` varchar(32) NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_esign_time_tenant_source` (`tenant_id`, `trusted_time_source`, `server_observed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名可信时间证据';

CREATE TABLE IF NOT EXISTS `system_electronic_signature_seal` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL,
    `seal_date` date NOT NULL,
    `record_count` int NOT NULL,
    `first_signature_id` bigint NOT NULL,
    `last_signature_id` bigint NOT NULL,
    `previous_seal_hash` char(64) DEFAULT NULL,
    `seal_hash` char(64) NOT NULL,
    `worm_evidence_id` varchar(128) NOT NULL,
    `status` varchar(32) NOT NULL,
    `sealed_at` datetime NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_esign_seal_tenant_date` (`tenant_id`, `seal_date`, `deleted`),
    KEY `idx_esign_seal_tenant_hash` (`tenant_id`, `seal_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名日封存链';

CREATE TABLE IF NOT EXISTS `system_electronic_signature_privileged_audit` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL,
    `reviewer_user_id` bigint NOT NULL,
    `operation_code` varchar(128) NOT NULL,
    `reason` varchar(500) NOT NULL,
    `result_status` varchar(32) NOT NULL,
    `evidence_hash` char(64) NOT NULL,
    `audited_at` datetime NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_esign_privileged_tenant_time` (`tenant_id`, `audited_at`, `reviewer_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名特权访问审计';

CREATE TABLE IF NOT EXISTS `system_electronic_signature_archive_recovery` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `tenant_id` bigint NOT NULL,
    `archive_id` bigint NOT NULL,
    `restored_at` datetime NOT NULL,
    `restored_by` bigint NOT NULL,
    `business_record_hash` char(64) NOT NULL,
    `signature_record_hash` char(64) NOT NULL,
    `snapshot_hash` char(64) NOT NULL,
    `restore_evidence_hash` char(64) NOT NULL,
    `result_status` varchar(32) NOT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_esign_recovery_tenant_archive` (`tenant_id`, `archive_id`, `restored_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子签名归档恢复核验证据';
