# Task: Enable BPM module

## Goal

Enable `yudao-module-bpm` in the backend Maven build so BPM backend APIs are compiled into `yudao-server` instead of returning the disabled-module response for `/admin-api/bpm/**`.

## Scope

- Follow the official BPM enablement guidance at `https://doc.iocoder.cn/bpm/`.
- Enable the BPM Maven module in the root reactor.
- Enable the BPM dependency in `yudao-server`.
- Add regression coverage for the Maven enablement contract.
- Record the required BPM database SQL prerequisite explicitly instead of adding fallback behavior.

## Milestones

- [x] M1: Previous backend task state checked before starting.
- [x] M2: Task document created before production code changes.
- [x] M3: BDD scenario and RED test recorded.
- [x] M4: BPM Maven module and server dependency enabled.
- [x] M5: Targeted verification run and result recorded.
- [x] M6: Task status finalized and current-task changes committed when verification passes.

## Expected Verification

- `mvn -pl yudao-server -am "-Dtest=BpmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package`

## Current Status

Completed. BPM is enabled in the root Maven reactor and `yudao-server`; targeted runtime-classpath regression and package verification both pass.

## Runtime Prerequisites

- The official BPM SQL (`bpm_` tables) must be imported into the target database before runtime BPM operations can succeed. This task only enables the backend build wiring.

## Blockers

- None for the build wiring task.

## Final Verification Result

- `mvn -pl yudao-server -am "-Dtest=BpmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- `mvn -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package` -> PASS.
- Evidence validators for backend API and bug regression evidence -> PASS.
