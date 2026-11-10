-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_archive_download_request (
 id bigint NOT NULL AUTO_INCREMENT, tenant_id bigint NOT NULL,
 controlled_file_id bigint NOT NULL, version_no varchar(64) NOT NULL, applicant_id bigint NOT NULL,
 attachment_id bigint DEFAULT NULL, reason varchar(500) NOT NULL, status varchar(20) NOT NULL,
 reviewer_id bigint DEFAULT NULL, review_reason varchar(500) DEFAULT NULL, reviewed_time datetime DEFAULT NULL,
 creator varchar(64) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '', update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id),
 KEY idx_archive_download_grant (tenant_id,controlled_file_id,applicant_id,status,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
