-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=schema; riskLevel=medium
CREATE TABLE demo_metadata_only (id bigint);
