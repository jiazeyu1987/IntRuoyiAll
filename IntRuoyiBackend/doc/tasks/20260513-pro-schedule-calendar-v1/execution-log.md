# Execution Log: IntPP-style schedule calendar v1 backend

BDD: Save schedule calendar rules -> Given the production schedule calendar is enabled, When an operator updates holiday skip, weekend mode, or date-level shift overrides, Then the backend persists the singleton rules and returns the updated state.

BDD: Advance simulation date -> Given the current simulation date exists, When an operator advances one day or thirty days, Then the backend updates the stored simulation date and returns the new date.

BDD: Read month schedule calendar -> Given current formal production tasks exist, When the frontend requests one month of schedule calendar data, Then the backend returns per-day summaries derived from current tasks plus calendar rules.

BDD: Read selected-day detail -> Given a selected schedule date has current tasks, When the frontend requests the selected-day detail, Then the backend returns workshop/line/task/material detail for that day.

## Evidence

- M1/M2: Completed. Previous backend tasks were checked complete and this backend task document plus BDD scenarios were created before production code changes.
- RED: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProScheduleCalendarServiceImplTest" test` -> FAIL, `getDayDetail_shouldAggregateWorkshopLineAndMaterialIssues` hit `MD_ITEM_NOT_EXISTS` because item fail-fast validation compared full map size instead of required keys.
- GREEN: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProScheduleCalendarServiceImplTest" test` -> PASS
- GREEN: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProScheduleCalendarServiceImplTest" test` -> PASS after adding overlap-range task loading, cross-day occupancy expansion, empty-slice navigation coverage, invalid real-date validation, and issue-material fail-fast checks.
- GREEN: `Get-Content -Raw sql\mysql\mes-auto-schedule-first-loop.sql | docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro` -> PASS
- RED: `mvn -pl yudao-server -am -DskipTests package` -> FAIL, running `yudao-server.jar` locked the target artifact during Spring Boot repackage.
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS after stopping the running backend process
