# Execution Log: Schedule Calendar follow-up hardening

BDD: Canonical schema parity -> Given the schedule calendar and auto-schedule features are shipped, When a new environment is initialized from canonical SQL, Then the required runtime tables exist without requiring a helper patch first.

BDD: Issue persistence boundary -> Given blocking shortages still prevent automatic publish, When the current schedule calendar reads persisted issues, Then the runtime behavior is explicit about what can and cannot appear after a successful apply.

## Evidence

- F1/F2: Completed. Previous backend task was checked complete and this follow-up task document was created before new production code changes.
- RED: `rg -n "mes_md_production_line|mes_pro_capacity_plan|mes_pro_task_schedule_ext|mes_pro_schedule_issue" D:\wt\intsched-be\sql\mysql\ruoyi-vue-pro.sql` -> FAIL, canonical MySQL bootstrap lacked runtime tables required by the shipped feature.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session schedule-calendar run-code --filename D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3\doc\tasks\20260513-pro-schedule-calendar-v1\scripts\exercise-schedule-calendar.mjs` -> FAIL, `/mes/pro/schedule-calendar/rules` returned `500` because `current_date` caused SQL syntax errors during initial simulation-row insert.
- GREEN: `mvn -f yudao-module-mes\pom.xml "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" test` -> PASS
- GREEN: `Get-Content -Raw sql\mysql\mes-auto-schedule-first-loop.sql | docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro` -> PASS
- GREEN: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action ReplayAndExercise` -> PASS
- GREEN: authenticated `GET /admin-api/mes/pro/schedule-calendar/rules` -> PASS after renaming the simulation column to `simulation_date`
