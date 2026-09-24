-- rollback-migration: allowedEnvironments=test,backup,prod; requiresBackup=true; type=schema; riskLevel=high
DROP TABLE release_table;
