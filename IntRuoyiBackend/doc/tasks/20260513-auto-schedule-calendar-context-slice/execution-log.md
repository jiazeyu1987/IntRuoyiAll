# Execution Log: Auto schedule calendar-context slice

BDD: Calendar-driven preview -> Given schedule calendar rules override weekend/day/night behavior and simulation state exists, When preview is generated, Then the resulting available scheduling windows reflect that effective calendar context.

BDD: Stale preview rejection -> Given preview was generated under one effective calendar context, When calendar rules or simulation state change before apply, Then apply fails and the current formal task set remains unchanged.

BDD: Frontend replay of preview context -> Given the frontend accepted a preview, When the user publishes it, Then the frontend sends the previewed calendar-context token rather than silently recomputing under a new calendar state.

## Evidence

- M1/M2: Completed. Slice boundary chosen from the reviewed checklist and task docs created before production code changes.
- RED: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest" test` -> FAIL, `preview_shouldChangeWhenCalendarRuleChanges` still schedules the day window, `preview_shouldChangeWhenSimulationDateChanges` still starts before the simulated current date, and `apply_shouldRejectWhenCalendarContextChangesAfterPreview` fails because preview response has no `calendarContextToken`.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> FAIL, `preview_shouldChangeWhenCalendarRuleChanges` still starts at `2026-05-14T08:00` after switching date-shift rule from `DAY` to `NIGHT`, proving auto-schedule ignores schedule-calendar rules.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> FAIL, `preview_shouldChangeWhenSimulationDateChanges` still starts at `2026-05-14T08:00` after simulation current date moves to `2026-05-15`, proving auto-schedule ignores simulated current date.
- RED: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> FAIL, `apply_shouldRejectWhenCalendarContextChangesAfterPreview` cannot read `calendarContextToken` from preview response, proving preview/apply are not bound to a shared calendar context.
- GREEN: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest" test` -> PASS, preview now shifts between day/night windows from schedule-calendar rules, simulation date pushes the earliest schedulable window, and apply rejects a stale preview token.
- GREEN: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProScheduleCalendarServiceImplTest" test` -> PASS, extracted shared calendar-rule helper keeps existing schedule-calendar rule, month, and day-detail behavior green.
- GREEN: `mvn -pl yudao-server -am -DskipTests compile` -> PASS, backend API contract and service wiring compile through reactor including `yudao-server`.
- GREEN: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" test` -> PASS, the final backend worktree state still keeps both auto-schedule and schedule-calendar target tests green.
- GREEN: `script\shell\mes-auto-schedule-first-loop-demo.ps1` now carries `calendarContextToken` from preview into apply, so the local replay script matches the new backend contract.
- BLOCKED: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action ExerciseApiFlow` -> FAIL, the service currently running on `http://127.0.0.1:48080/admin-api` does not expose `/mes/pro/auto-schedule/preview` and returns `No static resource admin-api/mes/pro/auto-schedule/preview.` This identified a runtime-environment mismatch, not a code regression.
- GREEN: current branch backend was packaged with `mvn -pl yudao-server -am -DskipTests package` and started on `http://127.0.0.1:48092/admin-api` using the same MySQL/Redis overrides as the already-running local instance (`23306` / `26379`).
- GREEN: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action ReplayAndExercise -BaseUrl http://127.0.0.1:48092/admin-api` -> PASS, schema replay, demo data rebuild, preview/apply, and DB verification all succeeded against the current branch backend.
- NOTE: `mvn ... clean ... test` hit a Windows file-lock failure while deleting `D:\wt\intsched-be\yudao-module-mes\target`; non-clean `test` and reactor compile both passed afterward, so the slice remains verified without fallback behavior.
- M7: Backend slice committed as `2aa1f655bd` with only task-scoped files; unrelated dirty files under `doc/tasks/20260513-pro-schedule-calendar-followups/` and `sql/{kingbase,opengauss,oracle,postgresql,sqlserver}/` were left untouched.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest test` -> PASS
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest test` -> PASS
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" test` -> PASS
