from __future__ import annotations

from .result import success_result


ALLOWED_JOB_STATUSES = [
    "CREATED",
    "PRECHECKING",
    "PRECHECK_FAILED",
    "READY",
    "LOCKED",
    "BACKING_UP",
    "CLEARING_TARGET",
    "CLONING",
    "VERIFYING",
    "SUCCEEDED",
    "FAILED",
    "ROLLING_BACK",
    "ROLLED_BACK",
]


TENANT_CLONE_DDL = """CREATE TABLE infra_tenant_clone_job (
  id BIGINT NOT NULL PRIMARY KEY,
  job_code VARCHAR(64) NOT NULL,
  source_tenant_id BIGINT NOT NULL,
  target_tenant_id BIGINT NOT NULL,
  profile VARCHAR(64) NOT NULL,
  mode VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  current_phase VARCHAR(64) NULL,
  requested_by BIGINT NULL,
  request_payload JSON NOT NULL,
  precheck_report JSON NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(512) NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_tenant_clone_job_code (job_code),
  KEY idx_tenant_clone_job_target (target_tenant_id, status)
);

CREATE TABLE infra_tenant_clone_id_map (
  id BIGINT NOT NULL PRIMARY KEY,
  job_id BIGINT NOT NULL,
  table_name VARCHAR(128) NOT NULL,
  source_pk VARCHAR(128) NOT NULL,
  target_pk VARCHAR(128) NOT NULL,
  pk_type VARCHAR(32) NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_clone_id_map (job_id, table_name, source_pk),
  UNIQUE KEY uk_clone_target_id_map (job_id, table_name, target_pk)
);"""


def generate_tenant_clone_ddl(name: str) -> dict[str, object]:
    if name != "tenant-clone-job":
        raise ValueError(f"unsupported DDL name: {name}")
    return success_result(ddl=TENANT_CLONE_DDL, allowedStatuses=ALLOWED_JOB_STATUSES)

