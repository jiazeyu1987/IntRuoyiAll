-- release-migration: allowedEnvironments=test,backup; dependsOn=20260612_runtime_nightly_release_job; type=schema; riskLevel=medium
CREATE TABLE IF NOT EXISTS infra_runtime_release_package_config (id bigint PRIMARY KEY);
