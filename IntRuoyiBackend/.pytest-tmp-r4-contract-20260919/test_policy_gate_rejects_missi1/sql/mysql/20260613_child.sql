-- release-migration: allowedEnvironments=test; dependsOn=20260613_parent; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS child_table (id bigint);
