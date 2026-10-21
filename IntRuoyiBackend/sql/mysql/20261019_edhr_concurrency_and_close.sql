-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=high; requiresTargetPreflight=true
-- eDHR edit generations and attributable deviation closure. Apply once before new binaries.
ALTER TABLE mes_qa_inspection_regulation_version
  ADD COLUMN draft_revision BIGINT NOT NULL DEFAULT 0 COMMENT '草稿编辑修订号，独立于业务版本号';
ALTER TABLE mes_pro_edhr_deviation
  ADD COLUMN closed_by BIGINT NULL COMMENT '实际关闭人',
  ADD COLUMN closed_content_version INT NULL COMMENT '关闭依据的处理版本',
  ADD COLUMN closed_content_hash VARCHAR(64) NULL COMMENT '关闭依据的内容哈希',
  ADD COLUMN closed_signature_ids_json TEXT NULL COMMENT '关闭时有效签名集合';

ALTER TABLE mes_pro_process_pool_active_order ADD COLUMN dossier_revision BIGINT NOT NULL DEFAULT 0 COMMENT '资料集合修订号';

-- Durable intent is committed before external storage I/O. Bound objects are retained for signed evidence.
CREATE TABLE mes_dossier_upload_intent (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL, active_order_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 config_id BIGINT NOT NULL, storage_path VARCHAR(512) NOT NULL, file_id BIGINT NULL,
 state VARCHAR(24) NOT NULL, expires_at DATETIME NOT NULL, last_error VARCHAR(256) NULL,
 creator VARCHAR(64) NOT NULL DEFAULT '', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater VARCHAR(64) NOT NULL DEFAULT '', update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted BIT NOT NULL DEFAULT b'0',
 INDEX idx_dossier_upload_cleanup (state, expires_at),
 UNIQUE KEY uk_dossier_upload_path (config_id,storage_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
