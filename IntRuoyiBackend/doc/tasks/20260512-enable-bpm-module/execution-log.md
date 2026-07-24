# Execution Log: Enable BPM module

BDD: BPM backend module is included in the runtime build -> Given the server currently returns the disabled-module message for `/admin-api/bpm/**`, When BPM is enabled according to `https://doc.iocoder.cn/bpm/`, Then the root Maven reactor includes `yudao-module-bpm` and `yudao-server` depends on `yudao-module-bpm` so BPM controllers can be compiled into the server.

## Evidence

- M1: Completed. Previous backend task `doc/tasks/20260512-enable-crm-module/task.md` is already explicitly blocked/interrupted and will not be mixed with this task.
- M2: Completed. This task document and execution log were created before production code changes.
- Root cause: `ruoyi-vue-pro/pom.xml` comments out `yudao-module-bpm`, and `ruoyi-vue-pro/yudao-server/pom.xml` comments out the `cn.iocoder.boot:yudao-module-bpm` dependency. The startup/banner disabled-module response is therefore expected.
- External prerequisite: The official BPM enablement guide requires importing BPM SQL (`bpm_` tables) and restarting the backend after enabling the Maven module. This task records that prerequisite and does not add fallback behavior.
- RED: `mvn -pl yudao-server -am "-Dtest=BpmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, Maven cannot resolve `cn.iocoder.boot:yudao-module-bpm:2026.04-SNAPSHOT` while building the current reactor; `BpmModuleEnablementTest` is present but the build fails before `yudao-server` tests because BPM is missing from the reactor.
- M4: Completed. Enabled `<module>yudao-module-bpm</module>` in the root Maven reactor and enabled the `cn.iocoder.boot:yudao-module-bpm` dependency in `yudao-server`.
- GREEN: `mvn -pl yudao-server -am "-Dtest=BpmModuleEnablementTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `BpmModuleEnablementTest` loads `cn.iocoder.yudao.module.bpm.controller.admin.task.BpmTaskController` from the `yudao-server` test runtime classpath.
- REGRESSION: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" "-Dspring-boot.repackage.skip=true" package` -> PASS, the backend reactor packages successfully with BPM included.
- M5: Completed. Targeted verification and package verification passed.
- M6: Completed. Task status finalized; current-task changes are ready for an isolated commit.
