# Execution Log: Enable CRM module

BDD: CRM backend module is included in the runtime build -> Given the server currently returns the disabled-module message for `/admin-api/crm/**`, When CRM is enabled according to `https://doc.iocoder.cn/crm/build/`, Then the root Maven reactor includes `yudao-module-crm` and `yudao-server` depends on `yudao-module-crm` so CRM controllers can be compiled into the server.

BDD: CRM approval dependencies are included in the local reactor -> Given CRM contract and receivable approvals use BPM approval, When CRM is enabled for `yudao-server`, Then the root Maven reactor includes `yudao-module-bpm` and `yudao-server` depends on `yudao-module-bpm` so Maven does not resolve BPM from a missing remote snapshot.

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260512-mes-paperless-batch-processing-plan/task.md` is completed.
- M2: Completed. This task document and execution log were created before production code changes.
- RED: `mvn -pl yudao-server -am "-Dtest=CrmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason after test dependency setup: root `pom.xml` did not include `yudao-module-crm`, and `yudao-server/pom.xml` did not depend on `yudao-module-crm`.
- GREEN attempt: `mvn -pl yudao-server -am "-Dtest=CrmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, CRM entered the reactor but Maven could not resolve `cn.iocoder.boot:yudao-module-bpm:2026.04-SNAPSHOT`; official CRM guidance states CRM contract and receivable approvals use BPM approval, so BPM is a required local module prerequisite.
- GREEN: `mvn "-Dflatten.skip=true" -pl yudao-server -am "-Dtest=CrmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `CrmModuleEnablementTest` ran 4 tests with 0 failures, 0 errors, and 0 skipped.
- GREEN: `mvn "-Dflatten.skip=true" -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package` -> PASS, reactor build completed with CRM and BPM in the local module graph.
- Runtime prerequisite: local SQL search found CRM dictionary/menu seed rows but no `CREATE TABLE ... crm_` DDL. Import the official CRM/BPM database scripts before exercising CRM business endpoints against a real database.
