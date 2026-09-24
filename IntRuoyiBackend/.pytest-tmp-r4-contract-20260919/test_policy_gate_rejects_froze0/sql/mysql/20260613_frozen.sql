-- release-migration: allowedEnvironments=test,backup; dependsOn=; type=schema; riskLevel=low
CREATE TABLE IF NOT EXISTS frozen_table (id bigint);
