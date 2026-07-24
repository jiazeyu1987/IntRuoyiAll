-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
-- Support resumable browser-selected local folder uploads by persisting per-chunk state.
CREATE TABLE IF NOT EXISTS `dcc_controlled_file_local_folder_upload_chunk` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL COMMENT 'dcc_controlled_file_nas_transfer_task.id',
  `relative_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Browser local folder relative path',
  `file_name` varchar(255) NOT NULL COMMENT 'Original file name',
  `file_size` bigint NOT NULL COMMENT 'Original file byte size',
  `chunk_index` int NOT NULL COMMENT 'Zero-based chunk index',
  `total_chunks` int NOT NULL COMMENT 'Total chunks of the original file',
  `chunk_size` bigint NOT NULL COMMENT 'Received chunk byte size',
  `chunk_sha256` varchar(64) NOT NULL COMMENT 'SHA-256 hex digest of the received chunk',
  `chunk_temp_path` varchar(1024) NOT NULL COMMENT 'Server-side persisted temporary chunk path',
  `status` varchar(32) NOT NULL DEFAULT 'COMPLETED' COMMENT 'Chunk upload status',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `updater` varchar(64) DEFAULT NULL,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_local_folder_chunk_position` (`task_id`, `relative_path`, `chunk_index`),
  KEY `idx_dcc_local_folder_chunk_file` (`task_id`, `relative_path`),
  KEY `idx_dcc_local_folder_chunk_status` (`task_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC local folder resumable upload chunks';
