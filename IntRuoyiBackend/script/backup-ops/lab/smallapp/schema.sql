CREATE TABLE smallapp_tenants (
 tenant_id BIGINT NOT NULL PRIMARY KEY,
 name VARCHAR(80) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE smallapp_users (
 id BIGINT NOT NULL PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 username VARCHAR(80) NOT NULL,
 password_hash VARCHAR(160) NOT NULL,
 active TINYINT NOT NULL,
 failed_logins INT NOT NULL DEFAULT 0,
 locked_until DATETIME(6) NULL,
 UNIQUE KEY uq_user_tenant (id,tenant_id),
 UNIQUE KEY uq_tenant_username (tenant_id,username),
 FOREIGN KEY (tenant_id) REFERENCES smallapp_tenants(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE smallapp_sessions (
 session_hash BINARY(32) NOT NULL PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 csrf CHAR(64) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 FOREIGN KEY (user_id,tenant_id) REFERENCES smallapp_users(id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE smallapp_records (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 owner_id BIGINT NOT NULL,
 record_key VARCHAR(80) NOT NULL,
 title VARCHAR(120) NOT NULL,
 content TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_record_tenant (id,tenant_id),
 UNIQUE KEY uq_record_key (tenant_id,record_key),
 FOREIGN KEY (owner_id,tenant_id) REFERENCES smallapp_users(id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE smallapp_objects (
 id CHAR(32) NOT NULL PRIMARY KEY,
 tenant_id BIGINT NOT NULL,
 bucket VARCHAR(63) NOT NULL,
 object_key VARCHAR(200) NOT NULL,
 size_bytes INT NOT NULL,
 sha256 CHAR(64) NOT NULL,
 state ENUM('STAGED','ACTIVE','DELETE_PENDING') NOT NULL,
 UNIQUE KEY uq_object_tenant (id,tenant_id),
 UNIQUE KEY uq_object_key (bucket,object_key),
 FOREIGN KEY (tenant_id) REFERENCES smallapp_tenants(tenant_id),
 CHECK (size_bytes BETWEEN 1 AND 65536)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE smallapp_attachments (
 tenant_id BIGINT NOT NULL,
 record_id BIGINT NOT NULL,
 object_id CHAR(32) NOT NULL,
 filename VARCHAR(200) NOT NULL,
 PRIMARY KEY (record_id,tenant_id),
 UNIQUE KEY uq_attachment_object (object_id,tenant_id),
 FOREIGN KEY (record_id,tenant_id) REFERENCES smallapp_records(id,tenant_id),
 FOREIGN KEY (object_id,tenant_id) REFERENCES smallapp_objects(id,tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
