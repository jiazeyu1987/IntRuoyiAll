-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260613_preflight; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS child_table (id bigint);
