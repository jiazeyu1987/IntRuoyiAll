# Task: Enable CRM module

## Goal

Enable `yudao-module-crm` in the backend Maven build so CRM backend APIs are compiled into `yudao-server` instead of returning the disabled-module response for `/admin-api/crm/**`.

## Scope

- Follow the official CRM enablement guidance at `https://doc.iocoder.cn/crm/build/`.
- Enable the CRM Maven module in the root reactor.
- Enable the BPM Maven module required by CRM approval features.
- Enable the CRM dependency in `yudao-server`.
- Enable the BPM dependency in `yudao-server`.
- Add regression coverage for the Maven enablement contract.
- Record any missing database prerequisite explicitly instead of adding fallback behavior.

## Milestones

- [x] M1: Previous backend task state checked before starting.
- [x] M2: Task document created before production code changes.
- [x] M3: BDD scenario and RED test recorded.
- [x] M4: CRM Maven module and server dependency enabled.
- [x] M5: Targeted verification run and result recorded.
- [x] M6: Task status finalized and current-task changes committed when verification passes.

## Expected Verification

- `mvn "-Dflatten.skip=true" -pl yudao-server -am "-Dtest=CrmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn "-Dflatten.skip=true" -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package`

## Current Status

Completed for build-time CRM enablement. `yudao-module-crm` and its required local `yudao-module-bpm` dependency are enabled in the root Maven reactor and `yudao-server`, with regression coverage in `CrmModuleEnablementTest`.

The previous backend task `20260512-mes-paperless-batch-processing-plan` is marked completed. The working tree already contains unrelated user/agent changes from a different DingTalk import task; those files are not part of this task and will not be reverted or committed here.

## Runtime Prerequisite

- Prerequisite: official CRM/BPM table DDL must be imported before using runtime CRM business endpoints.
- Evidence: checked-in SQL seed data contains CRM dictionary/menu rows, but no `CREATE TABLE ... crm_` DDL was found in the local SQL files.
- Impact: the Maven build includes CRM after this task, but runtime requests that touch CRM tables can still fail with a missing-table/schema-not-imported error until the database scripts are imported.
