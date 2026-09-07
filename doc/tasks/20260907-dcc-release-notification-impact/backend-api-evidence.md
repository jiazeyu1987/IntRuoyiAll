# Backend API Evidence

## Scope

- P2 impact-assessment service and API only: materialize tasks, start review, reassign, decide, reopen, link a revision, and resolve after linked revision publication.
- P3 notification delivery and frontend are excluded.

## API And Data Contract

- `POST /dcc/publication-impact-tasks/{id}/start`: current assignee starts a `PENDING` task with `expectedVersion`.
- `POST /dcc/publication-impact-tasks/{id}/decision`: current assignee records `NO_REVISION_REQUIRED` or `REVISION_REQUIRED`; both require a reason and CAS version.
- `POST /dcc/publication-impact-tasks/{id}/reassign` and `/reopen`: document-control management commands with reason and CAS version.
- `POST /dcc/publication-impact-tasks/{id}/link-revision`: links the only open major revision for the same related Master and requester.
- `POST /dcc/publication-impact-tasks/{id}/create-revision`: retains the existing `dcc:controlled-file:submit` controller permission, calls the existing major-revision workflow, then links the concrete new revision in the same transaction.
- Publication materialization creates one task per `tenant_id + batch_id + related_master_id`. The task freezes publication/relation/file identity, uses `row_version` for CAS, and records the linked revision explicitly.

## Auth, Permissions, Validation, And Errors

- Start, decision and revision linking are assignee-only in the service. Reassignment/reopen require both the formal `doc_control` role and `dcc:controlled-file:approve` at Controller and Service layers.
- Major-revision creation keeps the existing controller submit permission and the workflow's authoritative `source.requesterId == actor` rule; no OWNER substitute or permission widening was added.
- Empty reason, stale version, invalid state, cross-Master revision, inactive target assignee and unauthorized actor must fail without side effects.
- Existing links accept only one open workflow revision. ACTIVE, REJECTED, WITHDRAWN, SUPERSEDED, OBSOLETE and FINALIZATION_FAILED revisions are rejected.

## Required Config, Services, Fixtures, And Migrations

- P1 publication follow-up schema and service.
- New P2 impact-assessment migration and H2 fixture.
- Existing user, controlled-file workflow and finalization services. No new runtime configuration is required.

## BDD Scenarios

`BDD: related Master becomes one impact task -> Given P1 relation snapshots exist, When P2 materializes tasks, Then each related Master has one task with a frozen assignee or UNASSIGNED state.`

`BDD: impact decision never auto-revises -> Given an assignee chooses REVISION_REQUIRED, When the decision completes, Then no controlled-file version is created until the existing major-revision path is explicitly used.`

`BDD: linked revision resolves follow-up -> Given a required-revision task is linked to a valid revision, When that revision publishes, Then follow-up becomes RESOLVED without rewriting the assessment decision.`

## RED And GREEN

- RED: `mvn -o -pl yudao-module-dcc '-Dtest=DccRelatedFileImpactAssessmentServiceTest,DccPublicationImpactAssessmentSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=true' test` failed at test compilation because the P2 task/audit/API contracts did not exist.
- RED correction: the resolve lost-race suite failed because an automatic CAS miss propagated through finalization and converted a valid publication into `FINALIZATION_FAILED`.
- GREEN: the complete P2 suite passed `41` tests; the adjacent P1+P2 suite passed `286` tests with zero failures.
- GREEN: the focused current-read/lost-race suite passed `25` tests and includes real H2 publication and coordinator rollback paths.

## Contract Or Integration Verification

- Real H2 transaction tests prove task/audit and state/audit atomic rollback, plus rollback of a workflow-created revision when explicit task linking fails.
- A real finalization transaction proves a legitimate resolver/reopen race does not roll back publication: A/1 becomes SUPERSEDED, B/1 remains ACTIVE, and the Master points to B/1.
- CAS miss review uses a tenant/id/deleted-scoped `SELECT ... FOR UPDATE` current read. Only an already-resolved, reopened, or changed-link task is accepted as a legitimate race; an unexplained same-link `REVISION_LINKED` state still fails fast.
- Architecture tests prove the impact state service does not depend on Workflow or Finalization; the one-way coordinator owns `Impact + Workflow` orchestration.
- Independent tester passed AC-06 through AC-13 and P2-AC1 through P2-AC5 after the corrective rerun.

## Observability Touchpoints

- Immutable impact audit records cover materialization/initial assignment, reassignment, start, decision, reopen, revision link and resolution, including before/after status, assignee, decision/link snapshots and row versions.

## Blockers And Downstream Skill Needs

- No P2 code or local-verification blocker remains.
- Runtime MySQL migration, frontend/E2E and service restart remain explicitly deferred to P3/P4 authorization.
