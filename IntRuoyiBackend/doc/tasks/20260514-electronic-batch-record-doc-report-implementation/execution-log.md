# Execution Log: Electronic Batch Record DOC Report Backend

BDD: pilot DOC import creates or updates generated reports -> Given the approved pilot `.doc` sample is uploaded to the batch-record import API, When the backend validates and parses every discoverable Word table, Then it should create or update one JimuReport report per parsed table and persist the generated-report metadata in a single transaction.

BDD: generated-report list only shows electronic batch-record reports -> Given generated reports exist for the approved pilot sample, When the electronic batch-record list API is queried, Then it should return only the locally tracked generated reports and their linked JimuReport identifiers.

BDD: deleting a generated report removes both persistence layers -> Given a generated electronic batch-record report exists, When the delete API is called, Then it should delete the linked JimuReport record and the local generated-report metadata together.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordReportServiceImplDbTest" test` -> FAIL, before implementation the batch-record report service, parser, mapper, metadata DO, and generated-report APIs did not exist.

GREEN: local package-only `javac` compile -> PASS, the new `batchrecordreport` main package compiles successfully against the current compiled module outputs and dependency classpath.

GREEN: local package-only test `javac` compile -> PASS, the new `batchrecordreport` test package compiles successfully against the same local dependency classpath.

GREEN: local parser probe -> PASS, the approved pilot sample at `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc` was parsed into `11` report source tables with deterministic titles and row/column counts.

GREEN: `mvn -pl yudao-module-mes -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordReportServiceImplDbTest" test` -> PASS.
