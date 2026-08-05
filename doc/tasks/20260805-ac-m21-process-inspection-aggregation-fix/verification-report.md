# AC-M21 Process Inspection Aggregation Verification Report

## Summary

- Implemented structured AC-M21 process-inspection aggregation for approved PQC reviews.
- Added persistent aggregate detail rows sourced from formal PQC task and piece-detail records.
- Added tenant, event, review, task, regulation version, round, item, piece, equipment, standard, measured value, judgement, and aggregated timestamp traceability.
- Added PQC task `SUBMITTED -> CONFIRMED` CAS to represent final confirmed revision before detail insert.

## Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected missing AC-M21 aggregate detail model/mapper; also revealed unrelated module test compile blockers.
- GREEN: selected production `javac` for AC-M21 changed production classes -> PASS.
- GREEN: selected AC-M21 test `javac` -> PASS.
- GREEN: JUnit Console `MesPqcProcessInspectionAggregationServiceTest` -> PASS, 7/7 tests successful.
- GREEN: JUnit Console `MesProcessPoolSchemaTest` and `MesQaPqcSchemaTest` -> PASS, 5/5 tests successful.
- GREEN: backend API evidence validator -> PASS.
- GREEN: database schema evidence validator -> PASS.
- GREEN: experience keyword routing for AC-M21/PQC process inspection aggregation -> PASS.
- BLOCKED: 2026-08-05 stale blocker recheck `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` timed out after 304s before producing new Surefire PASS evidence; `jcmd` showed Maven dependency resolution stuck in `java.io.WinNTFileSystem.canonicalize0`.
- GREEN: after Maven concurrency cleared, reran `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `BUILD SUCCESS`, 12 tests run, 0 failures, 0 errors, 0 skipped, total time 06:01.
- PREVIEW: task-closeout cleanup preview kept `task.md`, `execution-log.md`, and `verification-report.md`; delete candidates were task-local intermediate evidence files and `javac-classes`; no blocked paths or warnings.
- APPLY: task-closeout cleanup apply deleted only task-local intermediate evidence files and `javac-classes`; kept `task.md`, `execution-log.md`, and `verification-report.md`.

## Changed Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/MesProProcessPoolPqcRecordMapper.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcProcessInspectionAggregateDetailDO.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcProcessInspectionAggregateDetailMapper.java`
- `IntRuoyiBackend/sql/mysql/20260805_mes_pqc_process_inspection_aggregate_detail.sql`
- `IntRuoyiBackend/sql/mysql/20260802_mes_pqc_inspection_task.sql`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProcessPoolSchemaTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesQaPqcSchemaTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/create_tables.sql`
- `IntRuoyiBackend/yudao-module-mes/src/test/resources/sql/clean.sql`
- `docs/backend-development.md`
- `docs/experience-index.md`

## Integration Notes

- Full all-test Maven module verification remains outside the scoped AC-M21 closeout gate; the AC-M21 minimum Maven gate now passes.
- Git closeout/commit/push requires careful staging because the current worktree/index contains many unrelated task changes.
