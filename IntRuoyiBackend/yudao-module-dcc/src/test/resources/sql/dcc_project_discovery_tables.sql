-- Isolated read-projection dependencies; project discovery executes the production SQL mapper.
CREATE TABLE IF NOT EXISTS mes_pro_route (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_md_item (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, code VARCHAR(64), deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_pro_route_product (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, route_id BIGINT, item_id BIGINT, deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_pro_route_version (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, route_id BIGINT, version_no VARCHAR(64),
 active BIT DEFAULT 0, lifecycle_status VARCHAR(32), deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_pro_batch_record_definition (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, batch_record_name VARCHAR(255),
 route_key VARCHAR(128), current_version_id BIGINT, deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_pro_batch_record_report (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, batch_record_definition_id BIGINT,
 form_slot_type VARCHAR(32), deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_pro_batch_record_version (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, version_no VARCHAR(64), deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_qa_inspection_regulation (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, owner_module VARCHAR(32),
 dcc_project_code_id BIGINT, deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mes_qa_inspection_regulation_version (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, regulation_id BIGINT, lifecycle_status VARCHAR(32),
 version_no VARCHAR(64), published_at DATETIME, deleted BIT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS mdm_product (
 id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, status VARCHAR(32),
 dcc_product_code VARCHAR(64), deleted BIT DEFAULT 0
);
