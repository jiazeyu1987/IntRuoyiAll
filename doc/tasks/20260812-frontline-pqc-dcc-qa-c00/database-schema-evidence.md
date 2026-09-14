# C00 Database Schema Evidence

## Data

- Route-DCC data source: approved routeId to dccProjectCodeId manifest consumed by C00 backfill, not product, route label, current QA, or DCC string matching.
- Active-order snapshots: dccProjectCodeId, qaRegulationId, and qaRegulationVersionId are backfilled only from the approved activeOrderId to QA snapshot manifest; locked task versions may be audited separately but must not become the active-order snapshot source.
- PQC task submission data: inspectionRuleKey is mapped only from FIRST, FINAL, PATROL AM, and PATROL PM evidence; CanonicalPqcSubmissionV1 hash requires one formal PQC event.

## Migration

- Added SQL package under IntRuoyiBackend/sql/mysql:
  - 20260812_mes_pqc_dcc_qa_c00_schema.sql
  - 20260812_mes_pqc_dcc_qa_c00_preflight.sql
  - 20260812_mes_pqc_dcc_qa_c00_backfill.sql
  - 20260812_mes_pqc_dcc_qa_c00_postflight.sql
  - 20260812_mes_pqc_dcc_qa_c00_rollback.sql
- The preflight script declares dependsOn=20260811_mes_qa_dcc_project_scope and runs before the C00 schema; it uses information_schema plus dynamic SQL to avoid failing when C00-created route-DCC structures are not present yet.
- The schema migration declares dependsOn=20260811_mes_qa_dcc_project_scope.
- It creates mes_pro_route_dcc_project_binding, adds active-order QA snapshot columns, adds PQC task rule/hash/event columns, and adds the PQC event generated column only.
- Postflight owns zero-blocker tightening: active-order QA snapshot NOT NULL, task inspectionRuleKey NOT NULL, old task identity index replacement, task rule identity unique key, submittedEventId unique key, and PQC event task unique key.

## Safety

- No business service, frontend, DCC backend, QA service, active-order service, or error-code file was modified.
- C00 scripts use blocker reports, input_manifest_sha256, affected_row_count, and explicit maintenance-window comments.
- No fallback, mock success, compatibility branch, context table, item-type table, or duplicate DCC-QA relation model was introduced.

## Rollback

- 20260812_mes_pqc_dcc_qa_c00_rollback.sql is dry-run evidence by default.
- It requires active-order and PQC submit writes to remain stopped before any reversal.
- Rollback reports planned reversal of generated event key, task submission columns, active-order QA snapshot columns, and route-DCC relation table.

## BDD:

- BDD: 最小增量 schema -> Given 已执行 20260811 QA-DCC migration, When 执行后继 migration, Then 只补路线关系和活跃订单快照且可重复执行。
- BDD: 批准清单回填活跃订单快照 -> Given 历史 activeOrder 已有业务批准的 activeOrderId 到 DCC/QA/发布版本清单, When 运行 backfill, Then 只按批准清单回填三快照；零 task、多 task 或唯一 task version 都不得替代批准清单。
- BDD: task规则身份可迁移 -> Given 历史 task 含 FIRST、AM巡检、PM巡检和旧合并 PATROL, When 回填 inspectionRuleKey, Then 前三者唯一映射且旧合并 PATROL 进入阻塞清单。

## RED:

- RED PRECHECK: target command initially passed before C00 assertions existed, proving the existing test did not cover the new C00 contract.
- RED: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL after adding C00 assertions; expected reason: missing 20260812_mes_pqc_dcc_qa_c00_schema.sql.

## GREEN:

- GREEN: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 7 tests run. Latest contract also asserts c00_backfill_approved_active_order_snapshot and forbids UNIQUE_TASK_VERSION-derived active-order snapshots.
- Regression: mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 14 tests run.

## Verification

- Static SQL contract scan passed for route-DCC binding, active-route generated key, active-order QA snapshots, task rule/hash/event columns, PQC event generated key, release metadata, input hash, affected row count, and blocker report markers.
- Forbidden SQL scan found no task-owned C00 SQL matches for duplicate DCC-QA relation table, item-type table, or active-order PQC context table names.
- git diff --check reported no whitespace errors; only the existing Windows LF-to-CRLF warning for MesQaPqcSchemaTest.java.
- Supervisor return verification passed: schema no longer contains premature task/event unique constraints; postflight contains zero-blocker dynamic creation for `uk_mes_pqc_task_rule_identity`, `uk_mes_pqc_task_submitted_event`, and `uk_mes_pro_process_pool_event_pqc_task`.

## Blockers

- None.
