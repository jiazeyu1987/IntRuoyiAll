# Verification Report

## Scope

- Backend local profile logging defaults.
- Standard local backend restart script runtime log path.
- Regression contracts for local runtime logging configuration.

## Changes Verified

- `application-local.yaml` now defaults backend application logs to `${INTRUOYI_BACKEND_LOG_FILE:${INTRUOYI_RUNTIME_LOG_DIR:../output/runtime/${INTRUOYI_RUNTIME_PROFILE:int_main}/logs}/${spring.application.name}.log}`.
- Self-owned MyBatis mapper packages under `cn.iocoder.yudao.module.*.dal.mysql` default to `info` instead of `debug`.
- `runtime-control.storage-guard.log-dir` follows the same runtime log directory default.
- `restart-int-ruoyi-local.ps1` creates `$RuntimeDir\logs`, passes `--logging.file.name=$backendLogFile`, and passes the matching storage guard log directory.

## RED / GREEN Evidence

- RED: `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest" test` failed because SQL DEBUG remained enabled and logs still defaulted to `${user.home}/logs`.
- GREEN: `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest,RuntimeControlLocalConfigTest" test` passed with 4 tests and 0 failures.
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_local_config.py script/tests/test_runtime_control_scripts.py -q` passed with 16 tests.
- Static scan: no default self-owned `*.dal.mysql: debug`, shared `${user.home}/logs/${spring.application.name}.log`, or old storage guard default remains in `application-local.yaml`.
- Diff hygiene: `git diff --check -- <task-owned paths>` passed.
- Cleanup: task-closeout preview/apply passed with no delete candidates and no blockers.

## Risk

- Existing currently running backend process still uses the command-line and config active at its start time. A backend restart is required before the new application log path affects runtime behavior.

## Blockers

- No implementation blocker.
- Closeout commit/push is blocked by broad pre-existing non-task dirty workspace changes unless the user authorizes the required baseline commit workflow.
