# Execution Log: Auto schedule replan and task-lock slice

BDD: Bounded replan -> Given current formal tasks already exist, When the planner replans only a chosen work-order/date scope, Then unrelated work orders and tasks outside that scope remain unchanged.

BDD: Replan respects protected tasks -> Given the chosen scope contains locked, manual, or finished tasks, When replan is requested, Then the backend preserves those tasks and reports the impacted protected set before write.

BDD: Explicit task lock -> Given a planner locks a task, When preview/apply/replan runs again, Then automatic scheduling must preserve that task as protected.

BDD: Explicit task unlock -> Given a planner unlocks a previously locked task, When preview/apply/replan runs again, Then that task can become replaceable according to the normal auto-schedule rules.

## Evidence

- M1/M2: Completed. Slice boundary chosen from the reviewed checklist and backend task docs created before production code changes.
- RED: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProTaskServiceImplTest" test` -> FAIL, `MesProTaskServiceImpl` had no explicit `lockTask/unlockTask` behavior and no task-schedule-ext write path for manual locking.
- GREEN: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProTaskServiceImplTest" test` -> PASS, explicit task lock/unlock service behavior now creates or updates `mes_pro_task_schedule_ext` and rejects finished tasks.
- GREEN: `mvn -pl yudao-server -am -DskipTests compile` -> PASS, backend lock/unlock API additions compile through the reactor.
- RED: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest" test` -> FAIL, there was no explicit `replanPreview/replanApply` behavior, and the new tests could not obtain protected-task impact or pass replan apply through the calendar-token contract.
- GREEN: `mvn -f D:\wt\intsched-be\yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest,MesProTaskServiceImplTest" test` -> PASS, backend now exposes `replanPreview/replanApply`, returns protected-task impact for preview, and replan apply reuses the protected-task scheduling path under the same calendar token contract.
