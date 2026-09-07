# Database Schema Evidence

## Goal And Affected Entities

- Add tenant-scoped impact-assessment tasks, idempotent publication notification deliveries and immutable audit rows derived from P1 publication snapshots.

## Engine And Migration Tool

- MySQL 8 formal migration plus H2 test fixture; release migration policy gate validates the complete dependency closure.

## Schema And Constraints

- `dcc_publication_impact_task` stores one tenant-scoped task per publication batch and related Master, with a second unique relation-snapshot key.
- Task columns hold frozen publication/relation identity, assignee snapshot, task status, decision/reason, explicit linked revision, resolution time, `row_version`, and immutable creation token.
- `dcc_publication_impact_audit` stores append-only action facts with before/after status, assignee, decision/revision snapshots, actor, reason, event time and row-version transition.
- Assignee/status and linked-revision/status indexes support the P3 workbench and publication resolution queries.
- `dcc_publication_notification_delivery` stores one row per tenant/batch/candidate with a second unique tenant/business-key constraint, `PENDING/SENT/FAILED`, attempt count, platform message id, safe error summary and row-version CAS.
- `dcc_publication_notification_audit` is append-only and records materialization, attempts, retry, SENT and FAILED transitions.
- P3 seeds the formal `{param}` notification template and stable publication-follow-up menu. Only active tenant `doc_control` roles already holding approve receive the menu; existing DCC-capable tenant packages are extended instead of creating an orphan package.

## Data Safety Analysis

- Additive schema only; no historical backfill and no runtime database execution in this turn.
- Business uniqueness must prevent more than one task per publication batch and related Master.
- Materialization uses the P1 frozen requester status, not a new current-user lookup. The creation token identifies the unique-key winner so a lost insert race cannot duplicate the `MATERIALIZE` audit.
- Only P1 candidates frozen as ACTIVE create delivery rows; INACTIVE/MISSING candidates remain audit facts and are never defaulted to retryable success or PENDING delivery.
- P3 migration is additive for new publications and contains no historical candidate/file backfill.

## Rollback Or Recovery Plan

- Before runtime deployment, the schema is validated statically and in H2 only. Runtime first/repeat execution is deferred to P4.
- No destructive DML is permitted; migration failure must stop deployment without fallback.
- Business mutations and their audit insert share one transaction. Audit failure rolls back task creation or state change; create-and-link failure rolls back the workflow-created revision.
- Notification attempt, SENT and FAILED state/audit writes use separate new transactions around the independently committed platform idempotent message. ACK loss leaves a retryable row and does not duplicate the platform message.
- Runtime rollback is restore from pre-migration backup if schema/menu/template deployment fails; no down migration or destructive compensating DML is run in P3.

## BDD Scenarios

`BDD: additive impact schema -> Given P1 publication snapshots exist, When the P2 migration is applied, Then task/audit tables and unique/CAS indexes exist without modifying historical files.`

`BDD: additive delivery schema -> Given P1 active notification candidates exist for a new publication, When P3 materializes delivery, Then each candidate and stable platform business key has one versioned row and every transition has an immutable audit row.`

`BDD: menu and template are replay-safe -> Given the DCC parent, doc-control role, approve permission and tenant packages are valid, When the P3 migration is reapplied, Then the stable menu/template remain unique and no unrelated role/package is created or widened.`

## RED And GREEN

- RED: `python -X utf8 -m pytest script/tests/test_dcc_publication_impact_assessment_sql.py -q` failed `3/3` because `20260907_dcc_publication_impact_assessment.sql` did not exist.
- GREEN: P1/P2 SQL contracts passed `6` tests; H2 schema/persistence and transaction paths are included in the `286`-test adjacent backend suite.
- P3 RED: `test_dcc_publication_notification_sql.py` initially failed `3/3`; hardened checks then exposed missing platform dependency, invalid template syntax, orphan package creation and over-broad role copying.
- P3 GREEN: combined P1/P2/P3 SQL contracts passed `9` tests; the final adjacent H2/backend suite passed `316` tests.

## Migration Verification

- Migration metadata: additive schema, `riskLevel=medium`, depends on `20260907_dcc_publication_followup`, allowed for test/backup/prod release tooling.
- The release migration policy gate passed the complete `13`-file dependency closure through `20260907_dcc_publication_impact_assessment.sql`.
- Static checks confirm no historical DML against publication relation snapshots and no history JSON blob in place of structured audit rows.
- DCC module compilation and scoped whitespace/diff checks passed.
- `20260907_dcc_publication_notification.sql` depends on both `20260815_system_notify_message_business_key` and the P2 impact migration.
- The complete release migration policy dependency closure through P3 passed with `migrationCount=15`.
- Static contracts verify delivery/audit unique keys, template JSON parameters, stable menu ownership, exact doc-control+approve role binding, existing-package extension and fail-fast preconditions.

## Blockers

- No P2/P3 schema-design or local-verification blocker remains.
- Runtime MySQL first/repeat execution for P1-P3 is intentionally unauthorized and remains a P4 gate; it is not claimed as completed.
