-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260920_dcc_project_product_create_approval,20260910_dcc_project_file_template; type=schema; riskLevel=medium
-- IC-1 B only; historical values remain NULL; no business DML or guessed leader IDs.
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_code' AND column_name = 'project_leader_user_id') = 0, 'ALTER TABLE dcc_project_code ADD COLUMN project_leader_user_id BIGINT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_code' AND column_name = 'default_attributes_json') = 0, 'ALTER TABLE dcc_project_code ADD COLUMN default_attributes_json LONGTEXT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'project_leader_user_id') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN project_leader_user_id BIGINT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'default_attributes_json') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN default_attributes_json LONGTEXT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'folder_template_id') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN folder_template_id BIGINT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;
SET @dcc_b_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'dcc_project_product_create_request' AND column_name = 'folder_template_snapshot_json') = 0, 'ALTER TABLE dcc_project_product_create_request ADD COLUMN folder_template_snapshot_json LONGTEXT NULL', 'SELECT 1');
PREPARE dcc_b_stmt FROM @dcc_b_ddl;
EXECUTE dcc_b_stmt;
DEALLOCATE PREPARE dcc_b_stmt;

CREATE TABLE IF NOT EXISTS dcc_project_application_attributes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, project_code_id BIGINT NOT NULL,
 application_type VARCHAR(16) NOT NULL, application_id BIGINT NOT NULL, application_round INT NOT NULL,
 default_source_json LONGTEXT NOT NULL, actual_attributes_json LONGTEXT NOT NULL, submitted BIT NOT NULL,
 tenant_id BIGINT NOT NULL DEFAULT 0, creator VARCHAR(64), updater VARCHAR(64),
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIT NOT NULL DEFAULT 0,
 UNIQUE(tenant_id, application_type, application_id, application_round)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_folder_template (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(128) NOT NULL, description VARCHAR(2048),
 active BIT NOT NULL, structure_json LONGTEXT NOT NULL, edited_by_user_id BIGINT NOT NULL, ever_used BIT NOT NULL,
 tenant_id BIGINT NOT NULL DEFAULT 0, creator VARCHAR(64), updater VARCHAR(64),
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_folder_template_history (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, template_id BIGINT NOT NULL, operator_user_id BIGINT NOT NULL,
 operation VARCHAR(16) NOT NULL, before_json LONGTEXT, after_json LONGTEXT,
 tenant_id BIGINT NOT NULL DEFAULT 0, creator VARCHAR(64), updater VARCHAR(64),
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS dcc_project_folder (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, project_code_id BIGINT NOT NULL, parent_id BIGINT NOT NULL,
 name VARCHAR(128) NOT NULL, sort_order INT NOT NULL, active BIT NOT NULL,
 source_template_id BIGINT NOT NULL, source_node_key VARCHAR(64) NOT NULL,
 tenant_id BIGINT NOT NULL DEFAULT 0, creator VARCHAR(64), updater VARCHAR(64),
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted BIT NOT NULL DEFAULT 0,
 UNIQUE(tenant_id, project_code_id, source_node_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
