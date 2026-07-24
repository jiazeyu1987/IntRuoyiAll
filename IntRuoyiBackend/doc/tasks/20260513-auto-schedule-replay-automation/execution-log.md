# Execution Log: Auto schedule local replay automation

BDD: Reset local replay data -> Given the local MySQL container is available, When the operator runs the replay helper in clean or seed mode, Then the helper executes the committed SQL scripts against `ruoyi-vue-pro` and reports success or the exact failure.

BDD: Verify local replay state -> Given the demo data has been seeded, When the operator runs the replay helper in verify mode, Then the helper prints the current demo work order, formal task count, and `quantityScheduled` for the `900080` work order.

## Evidence

- M1/M2: Completed. Previous backend task was checked completed and this follow-up task was documented before script changes.
- GREEN: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action Replay` -> PASS, schema apply plus clean/seed reset the demo work order to a zero-task initial state.
- GREEN: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action Verify` -> PASS, the helper printed the expected demo work order and zero-task initial state.
- GREEN: `powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action ReplayAndExercise` -> PASS, schema/data replay plus login -> preview -> apply -> DB verify completed successfully.
