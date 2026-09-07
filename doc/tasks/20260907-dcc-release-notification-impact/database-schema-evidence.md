# Database Schema Evidence

## Goal And Affected Entities

- Add tenant-scoped impact-assessment tasks and immutable audit rows derived from P1 publication relation snapshots.

## Engine And Migration Tool

- MySQL 8 formal migration plus H2 test fixture; release migration policy gate validates the complete dependency closure.

## Schema And Constraints

- `dcc_publication_impact_task` stores one tenant-scoped task per publication batch and related Master, with a second unique relation-snapshot key.
- Task columns hold frozen publication/relation identity, assignee snapshot, task status, decision/reason, explicit linked revision, resolution time, `row_version`, and immutable creation token.
- `dcc_publication_impact_audit` stores append-only action facts with before/after status, assignee, decision/revision snapshots, actor, reason, event time and row-version transition.
- Assignee/status and linked-revision/status indexes support the P3 workbench and publication resolution queries.

## Data Safety Analysis

- Additive schema only; no historical backfill and no runtime database execution in this turn.
- Business uniqueness must prevent more than one task per publication batch and related Master.
- Materialization uses the P1 frozen requester status, not a new current-user lookup. The creation token identifies the unique-key winner so a lost insert race cannot duplicate the `MATERIALIZE` audit.

## Rollback Or Recovery Plan

- Before runtime deployment, the schema is validated statically and in H2 only. Runtime first/repeat execution is deferred to P4.
- No destructive DML is permitted; migration failure must stop deployment without fallback.
- Business mutations and their audit insert share one transaction. Audit failure rolls back task creation or state change; create-and-link failure rolls back the workflow-created revision.

## BDD Scenarios

`BDD: additive impact schema -> Given P1 publication snapshots exist, When the P2 migration is applied, Then task/audit tables and unique/CAS indexes exist without modifying historical files.`

## RED And GREEN

- RED: `python -X utf8 -m pytest script/tests/test_dcc_publication_impact_assessment_sql.py -q` failed `3/3` because `20260907_dcc_publication_impact_assessment.sql` did not exist.
- GREEN: P1/P2 SQL contracts passed `6` tests; H2 schema/persistence and transaction paths are included in the `286`-test adjacent backend suite.

## Migration Verification

- Migration metadata: additive schema, `riskLevel=medium`, depends on `20260907_dcc_publication_followup`, allowed for test/backup/prod release tooling.
- The release migration policy gate passed the complete `13`-file dependency closure through `20260907_dcc_publication_impact_assessment.sql`.
- Static checks confirm no historical DML against publication relation snapshots and no history JSON blob in place of structured audit rows.
- DCC module compilation and scoped whitespace/diff checks passed.

## Blockers

- No schema-design or local-verification blocker remains.
- Runtime MySQL first/repeat execution is intentionally unauthorized in P2 and remains a P4 gate; it is not claimed as completed.
