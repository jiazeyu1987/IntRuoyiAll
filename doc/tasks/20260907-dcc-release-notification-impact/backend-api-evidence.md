# Backend API Evidence

## Scope

- P2 impact-assessment lifecycle plus P3 idempotent publication notification, follow-up query and frontend-facing command APIs.
- Runtime MySQL migration, Playwright acceptance and service restart are excluded until P4.

## API And Data Contract

- `POST /dcc/publication-impact-tasks/{id}/start`: current assignee starts a `PENDING` task with `expectedVersion`.
- `POST /dcc/publication-impact-tasks/{id}/decision`: current assignee records `NO_REVISION_REQUIRED` or `REVISION_REQUIRED`; both require a reason and CAS version.
- `POST /dcc/publication-impact-tasks/{id}/reassign` and `/reopen`: document-control management commands with reason and CAS version.
- `POST /dcc/publication-impact-tasks/{id}/link-revision`: links the only open major revision for the same related Master and requester.
- `POST /dcc/publication-impact-tasks/{id}/create-revision`: retains the existing `dcc:controlled-file:submit` controller permission, calls the existing major-revision workflow, then links the concrete new revision in the same transaction.
- Publication materialization creates one task per `tenant_id + batch_id + related_master_id`. The task freezes publication/relation/file identity, uses `row_version` for CAS, and records the linked revision explicitly.
- `GET /dcc/publication-followups/files/{controlledFileId}` reuses current controlled-file detail authorization before returning structured frozen visibility users, notification reasons and impact directions.
- `GET /dcc/publication-followups/my-impact-tasks` is assignee-scoped and database-paged. Its default work predicate includes ordinary unfinished tasks plus `REVISION_REQUIRED` work in `NOT_STARTED/REVISION_LINKED`, and excludes terminal `NOT_APPLICABLE/RESOLVED` work.
- `GET /dcc/publication-followups/management-page` applies all approved filters in the database and aggregates child rows only for the current page.
- `POST /dcc/publication-followups/notification-deliveries/{id}/retry` retries only `PENDING/FAILED` delivery with reason and CAS version; `SENT` cannot replay.
- `GET /dcc/publication-impact-tasks/{id}/revision-options` returns the Master's current revision family and authoritative unique open revision. Frozen related-file identity remains display/audit data, not the source of current operations.

## Auth, Permissions, Validation, And Errors

- Start, decision and revision linking are assignee-only in the service. Reassignment/reopen require both the formal `doc_control` role and `dcc:controlled-file:approve` at Controller and Service layers.
- Major-revision creation keeps the existing controller submit permission and the workflow's authoritative `source.requesterId == actor` rule; no OWNER substitute or permission widening was added.
- Empty reason, stale version, invalid state, cross-Master revision, inactive target assignee and unauthorized actor must fail without side effects.
- Existing links accept only one open workflow revision. ACTIVE, REJECTED, WITHDRAWN, SUPERSEDED, OBSOLETE and FINALIZATION_FAILED revisions are rejected.
- Publication management and retry require `doc_control`, `dcc:controlled-file:approve`, and `dcc:controlled-file:publication-followup:manage` in both Controller and Service.
- Management status filters are enum-validated. `assigneeUserId` is parsed and range-checked as `1..Long.MAX_VALUE` before reaching a Long-typed Mapper.
- Notification materialization rejects immutable identity drift across tenant, batch, candidate, user, business key or template.

## Required Config, Services, Fixtures, And Migrations

- P1 publication follow-up schema and service.
- P2 impact-assessment and P3 publication-notification migrations and H2 fixtures.
- Existing user, controlled-file workflow and finalization services. No new runtime configuration is required.
- Existing platform `sendSingleMessageIdempotentlyToAdmin` and its tenant-scoped `system_notify_message.business_key` uniqueness contract.

## BDD Scenarios

`BDD: related Master becomes one impact task -> Given P1 relation snapshots exist, When P2 materializes tasks, Then each related Master has one task with a frozen assignee or UNASSIGNED state.`

`BDD: impact decision never auto-revises -> Given an assignee chooses REVISION_REQUIRED, When the decision completes, Then no controlled-file version is created until the existing major-revision path is explicitly used.`

`BDD: linked revision resolves follow-up -> Given a required-revision task is linked to a valid revision, When that revision publishes, Then follow-up becomes RESOLVED without rewriting the assessment decision.`

`BDD: platform commit survives DCC ACK loss -> Given a stable batch/user business key, When the platform message commits but the DCC SENT acknowledgement fails, Then the delivery remains retryable and the retry returns the same platform message id before DCC becomes SENT.`

`BDD: notification failure never invalidates publication -> Given one recipient delivery fails after publication commit, When the post-commit dispatcher continues, Then that delivery becomes FAILED, other recipients continue, and the controlled file remains ACTIVE.`

`BDD: required revision stays actionable -> Given an assignee records REVISION_REQUIRED, When the workbench reloads, Then COMPLETED+NOT_STARTED remains visible for source selection or existing revision linking until the linked revision becomes RESOLVED.`

## RED And GREEN

- RED: `mvn -o -pl yudao-module-dcc '-Dtest=DccRelatedFileImpactAssessmentServiceTest,DccPublicationImpactAssessmentSchemaTest' '-Dsurefire.failIfNoSpecifiedTests=true' test` failed at test compilation because the P2 task/audit/API contracts did not exist.
- RED correction: the resolve lost-race suite failed because an automatic CAS miss propagated through finalization and converted a valid publication into `FINALIZATION_FAILED`.
- GREEN: the complete P2 suite passed `41` tests; the adjacent P1+P2 suite passed `286` tests with zero failures.
- GREEN: the focused current-read/lost-race suite passed `25` tests and includes real H2 publication and coordinator rollback paths.
- P3 RED: notification/schema/query classes initially failed test compilation; SQL contracts failed `3/3`; frontend contracts failed because the API and three surfaces did not exist.
- P3 corrective RED proved invalid template/menu contracts, missing post-commit/REQUIRES_NEW boundaries, stale batch aggregation, unsafe filters, missing revision options and the disappearing `COMPLETED+NOT_STARTED` work item.
- P3 GREEN: `27` focused tests, `316` P1-P3 adjacent tests and `26` platform idempotency tests passed with zero failures.

## Contract Or Integration Verification

- Real H2 transaction tests prove task/audit and state/audit atomic rollback, plus rollback of a workflow-created revision when explicit task linking fails.
- A real finalization transaction proves a legitimate resolver/reopen race does not roll back publication: A/1 becomes SUPERSEDED, B/1 remains ACTIVE, and the Master points to B/1.
- CAS miss review uses a tenant/id/deleted-scoped `SELECT ... FOR UPDATE` current read. Only an already-resolved, reopened, or changed-link task is accepted as a legitimate race; an unexplained same-link `REVISION_LINKED` state still fails fast.
- Architecture tests prove the impact state service does not depend on Workflow or Finalization; the one-way coordinator owns `Impact + Workflow` orchestration.
- Independent tester passed AC-06 through AC-13 and P2-AC1 through P2-AC5 after the corrective rerun.
- P3 real H2 tests prove attempt/ACK/FAILED transactions use separate `REQUIRES_NEW` boundaries; ACK loss progresses through FAILED row version 2 and an idempotent retry to SENT row version 4 with the same business key/message id.
- Batch status refresh locks tenant+batch first and uses locking current reads for delivery/task children, preventing MySQL repeatable-read snapshots from leaving concurrent all-SENT or FAILED batches stale.
- The post-commit scheduler rejects use without an active publication transaction and contains callback failures after commit so they cannot convert a committed ACTIVE version into finalization failure; pending/failed delivery remains observable.
- Independent tester passed P3 product behavior, P3-AC1 through P3-AC4 and AC-17 before this formal evidence refresh.

## Observability Touchpoints

- Immutable impact audit records cover materialization/initial assignment, reassignment, start, decision, reopen, revision link and resolution, including before/after status, assignee, decision/link snapshots and row versions.
- Immutable notification audit records cover materialization, attempt/retry, SENT and FAILED with actor, reason, attempt count, platform message id, safe error category and row-version transition.
- Follow-up batch status exposes `PROCESSING/READY/PARTIAL_FAILED/COMPLETED` without making message delivery or human impact work a file-effectiveness prerequisite.

## Blockers And Downstream Skill Needs

- No P2/P3 code or local-verification blocker remains.
- Runtime MySQL migration, Playwright E2E and service restart remain explicitly deferred to P4 authorization.
