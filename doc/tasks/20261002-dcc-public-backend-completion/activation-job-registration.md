# DCC activation job registration / enablement preparation

This package is prepared only. No database migration, Quartz operation, service restart or trigger was performed.

## Actual runtime contract

- Handler bean: `dccControlledFileActivationJob`; official `JobHandler` with `@TenantJob`. It accepts no business parameters and processes each tenant's recorded due versions. Dates use required `dcc.workflow.zone-id`; no zone default is inferred. Reminder lead days are a separate pending-distribution reminder setting and are not used by activation.
- Formal storage: existing `infra_job` (confirmed from current migration schema), status `1=NORMAL`, `2=STOP`. Registration is by stable unique `handler_name`; auto-generated database ID, no guessed reserved ID.
- New prepared migration: `sql/mysql/20261003_dcc_controlled_file_activation_job_registration.sql`, full dependsOn closure of `20260930_dcc_a_lifecycle`.
- It refuses to execute without explicit approved same-connection session variables: `@dcc_activation_cron`, `@dcc_activation_retry_count`, `@dcc_activation_retry_interval`, `@dcc_activation_monitor_timeout`. The user has approved `@dcc_activation_cron = '0 * * * * ?'` (every minute), required business zone `Asia/Shanghai`, and pending-distribution workbench reminder lead `7` days. These values must fit official Quartz/job validation and the actual `infra_job` schema. The new row is paused. Exact rerun is read-only; existing mismatched configuration or duplicate live handler fails; it does not alter other jobs or overwrite an existing approved enabled/paused state.

## Root review and authorized runtime steps

1. Freeze the actual connected DB and read the existing job rows for this handler. Use the confirmed cron `0 * * * * ?`, `dcc.workflow.zone-id=Asia/Shanghai`, and `dcc.workflow.reminder-lead-days=7`. Read and retain official job-management retry count/interval and monitor timeout for the task environment; do not infer them from the business cadence. Validate cron with the official job API/Quartz validator before preparing the operator's variable assignments.
2. Apply only the reviewed migration dependency package when specifically authorized. Re-read the unique assigned job ID, handler, params, cron and STOP state. Inserting an infra_job row is not proof that Quartz has registered it.
3. Confirm the task-owned application's Quartz configuration and resource ownership. `JobStartupSyncRunner` syncs stored rows only when the scheduler is enabled; `JobServiceImpl.syncJob()` rebuilds all persisted definitions and then pauses STOP rows. A manual `/infra/job/sync` is global and must be reviewed for its effect on other jobs, never called silently for this one handler.
4. After approved startup/synchronization, inspect the actual `qrtz_job_details` and `qrtz_triggers` for `dccControlledFileActivationJob`; verify paused status. Enable using official `/infra/job/update-status?id={actualId}&status=1` so Quartz is resumed as well as the row updated. Do not use direct status DML to claim enablement.
5. Verify one official actual trigger/log and task-owned due-version facts plus audit/retention; disable/review if any required policy/zone/storage permission fails. Record runtime evidence independently of isolated H2 PASS. The approved cadence is one check per minute; the business zone is Asia/Shanghai and the separate workbench reminder starts seven days in advance.

## Isolated evidence boundary

G07 actual lifecycle/H2/GxP tests cover ascending, same-day and reversed effective date ordering; complete-control immediate activation; concurrent job/direct replays; old pending reconciliation; higher future/uncontrolled protection; actual per-old-version audit and rollback. Prepared SQL is statically verified for paused registration, explicit parameters, handler uniqueness and absence of fixed IDs/history DML. Actual MySQL first/repeated execution and Quartz loading remain Root runtime work.
