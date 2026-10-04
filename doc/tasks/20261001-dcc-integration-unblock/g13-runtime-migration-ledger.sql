SELECT migration_id,sha256,status,target_environment FROM infra_release_migration
 WHERE migration_id LIKE '%dcc%' ORDER BY migration_id,target_environment;
SELECT TABLE_NAME,TABLE_ROWS FROM information_schema.TABLES
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('dcc_controlled_file','dcc_controlled_file_master',
 'dcc_controlled_file_signature','dcc_project_code','dcc_project_product_create_request','infra_release_migration');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('infra_release_migration','ACT_GE_PROPERTY','act_ge_property');
