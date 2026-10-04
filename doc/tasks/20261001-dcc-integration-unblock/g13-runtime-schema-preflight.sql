SELECT 'runtime', @@hostname, @@port, DATABASE(), CURRENT_USER(), @@version;
SELECT 'table', TABLE_NAME FROM information_schema.TABLES
 WHERE TABLE_SCHEMA=DATABASE() AND (TABLE_NAME LIKE 'dcc_%' OR TABLE_NAME LIKE '%migration%' OR TABLE_NAME='infra_job')
 ORDER BY TABLE_NAME;
SELECT 'column', TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, EXTRA
 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('dcc_controlled_file','dcc_controlled_file_master','dcc_controlled_file_signature',
 'dcc_project_code','dcc_project_product_create_request','dcc_project_application_attributes',
 'dcc_project_application_round_link','dcc_project_file_placement','dcc_project_folder',
 'dcc_project_file_reference','dcc_project_reviewer_config','dcc_workflow_lifecycle_event',
 'dcc_controlled_file_name_claim','infra_job') ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT 'index', TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART
 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
 AND TABLE_NAME IN ('dcc_controlled_file','dcc_controlled_file_master','dcc_controlled_file_name_claim',
 'dcc_project_file_reference','infra_job') ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;
SELECT 'job', id, handler_name, status, cron_expression FROM infra_job
 WHERE handler_name='dccControlledFileActivationJob';
