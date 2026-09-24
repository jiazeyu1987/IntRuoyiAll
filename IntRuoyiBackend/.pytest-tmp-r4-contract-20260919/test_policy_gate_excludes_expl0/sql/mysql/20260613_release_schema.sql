-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS release_table (id bigint);
