-- Task-owned H2 tables missing from the common unit-test schema; no production migration.
CREATE TABLE IF NOT EXISTS mes_pro_process_pool_active_order (
 id bigint PRIMARY KEY, leader_user_id bigint, work_order_id bigint, route_id bigint, route_version_id bigint,
 dcc_project_code_id bigint, qa_regulation_id bigint, qa_regulation_version_id bigint,
 rework_source_active_order_id bigint, rework_review_id bigint, erp_fixed_quantity_snapshot decimal(24,6),
 active_status varchar(32), business_status varchar(32), joined_at timestamp, sort_order bigint,
 removed_at timestamp, release_decision_id bigint, released_by bigint, released_at timestamp,
 udi_control_document_no varchar(128), simulated boolean, simulation_stage varchar(32), simulation_run_id varchar(128),
 version int, tenant_id bigint, deleted boolean DEFAULT false, creator varchar(64) DEFAULT '',
 create_time timestamp DEFAULT current_timestamp, updater varchar(64) DEFAULT '', update_time timestamp DEFAULT current_timestamp
);
CREATE TABLE IF NOT EXISTS mes_pro_process_pool_report_allocation (
 id bigint PRIMARY KEY, event_id bigint, review_id bigint, leader_user_id bigint, active_order_id bigint,
 work_order_id bigint, route_process_id bigint, process_id bigint, allocated_quantity decimal(24,6),
 allocation_mode varchar(32), lifecycle_status varchar(32), created_version int, superseded_version int,
 confirmed_at timestamp, simulated boolean, simulation_stage varchar(32), simulation_run_id varchar(128),
 tenant_id bigint, deleted boolean DEFAULT false, creator varchar(64) DEFAULT '', create_time timestamp DEFAULT current_timestamp,
 updater varchar(64) DEFAULT '', update_time timestamp DEFAULT current_timestamp
);
CREATE TABLE IF NOT EXISTS mes_pro_process_pool_team_device (
 id bigint PRIMARY KEY, tenant_id bigint, device_code varchar(64), device_name varchar(128), deleted boolean DEFAULT false
);
CREATE TABLE IF NOT EXISTS mes_pro_process_pool_team_employee_profile (
 id bigint PRIMARY KEY, tenant_id bigint, display_name varchar(128), employee_name varchar(128), deleted boolean DEFAULT false
);
CREATE TABLE IF NOT EXISTS mes_pro_process_pool_submission_review (
 id bigint PRIMARY KEY, tenant_id bigint, event_id bigint, leader_user_id bigint, review_signature_id bigint,
 reviewed_at timestamp, deleted boolean DEFAULT false
);
ALTER TABLE mes_pro_process_pool_active_order_process_snapshot ADD COLUMN IF NOT EXISTS process_code_snapshot varchar(64);
ALTER TABLE mes_pro_process_pool_active_order_process_snapshot ADD COLUMN IF NOT EXISTS process_name_snapshot varchar(128);
ALTER TABLE mes_pro_process_pool_active_order_process_snapshot ADD COLUMN IF NOT EXISTS simulated boolean;
ALTER TABLE mes_pro_process_pool_active_order_process_snapshot ADD COLUMN IF NOT EXISTS simulation_stage varchar(32);
ALTER TABLE mes_pro_process_pool_active_order_process_snapshot ADD COLUMN IF NOT EXISTS simulation_run_id varchar(128);
DELETE FROM mes_pro_process_pool_active_order;
DELETE FROM mes_pro_process_pool_report_allocation;
DELETE FROM mes_pro_process_pool_team_employee_profile;
