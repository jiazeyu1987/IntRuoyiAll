# Frontline PQC DCC QA Delivery Test Report

## TC-C00-SCHEMA

- status: PASS
- tested_task_ids: [C00]
- mapped_acceptance_ids: [AC-01, AC-03, AC-04, AC-05, AC-07, AC-09, AC-10, AC-11, AC-12, AC-13]
- tested_codebase: D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-c00
- tester: supervisor independent verification after two tester-agent runs stalled before writing evidence

### Expected Result

- C00 freezes the schema/migration baseline for route-DCC binding, active-order QA snapshots, PQC task rule identity, canonical submission hash, and unique formal PQC event linkage.
- Preflight is a schema-before read-only gate from 20260811 and must not depend on C00 schema objects already existing.
- Schema DDL must not prematurely tighten historical-data-sensitive unique constraints before backfill/postflight.
- Postflight owns zero-blocker NOT NULL tightening, old task identity index replacement, task rule identity uniqueness, submitted event uniqueness, and PQC event task uniqueness.
- C00 must not introduce duplicate DCC-QA binding, item-type table, active-order context table, fallback behavior, mock success, or silent downgrade.

### Actual Result

- Maven schema command PASS: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`; Surefire reports 7 tests, 0 failures, 0 errors.
- Maven regression command PASS: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`; Maven reports 14 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Static SQL layering PASS: preflight depends on `20260811_mes_qa_dcc_project_scope`, does not depend on `20260812_mes_pqc_dcc_qa_c00_schema`, and uses `information_schema` / dynamic SQL safety for C00-created structures.
- Static schema constraint PASS: `schema.sql` does not contain `uk_mes_pqc_task_rule_identity`, `uk_mes_pqc_task_submitted_event`, `uk_mes_pro_process_pool_event_pqc_task`, or early `DROP INDEX uk_mes_pqc_task_qa_identity`.
- Static postflight tightening PASS: `postflight.sql` contains the three uniqueness constraints, old identity index replacement, NOT NULL tightening, and `@c00_postflight_blocker_count = 0` gate with failure signaling.
- Forbidden model scan PASS: no C00 SQL hit for `dcc_project_code_qa_regulation_binding`, `mes_qa_inspection_regulation_item_type`, `mes_pro_process_pool_active_order_pqc_context`, fallback/default-success/silent-downgrade markers.
- Change-scope review PASS: worktree changes are limited to C00 SQL files, `MesQaPqcSchemaTest.java`, and C00 task evidence files.

### Evidence

- Surefire schema report: `D:/IntRuoyiWorktree/20260812-frontline-pqc-dcc-qa-c00/IntRuoyiBackend/yudao-module-mes/target/surefire-reports/cn.iocoder.yudao.module.mes.MesQaPqcSchemaTest.txt`.
- Regression output captured by supervisor at 2026-08-12 13:25:39 +08:00: 14 tests, 0 failures/errors, BUILD SUCCESS.
- Static check result: all 13 layering/forbidden checks returned true, `forbidden_hits=[]`, `all_pass=true`.

### Blockers

- None for C00 task-level acceptance.

### Unresolved Risks

- C00 was validated through static schema assertions and dry-run SQL evidence only; no live database migration sandbox was executed in this task scope.
- C00 is not yet merged into `int_main`; downstream Wave 1 tasks must not start from `int_main` until the supervisor merge gate is explicitly completed.
