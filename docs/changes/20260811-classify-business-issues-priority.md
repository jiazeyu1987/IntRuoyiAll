# Classify retained business issues by priority

## Request

- Requester: user in the current thread.
- Request: classify the 13 retained non-PQC issue documents using P0, P1, and subsequent priority levels.

## Baseline

- Reviewed the retained issue set: 001-010 and 013-015.
- Reviewed each issue's trigger, business impact, recommended rule, and current-code evidence.
- The task is a documentation audit; it does not have a linked implementation release plan or production incident frequency data.

## Classification

- Type: documentation requirement change and business-risk prioritization.
- P0: a trigger can silently cause wrong production, wrong inspection basis, unauthorized execution, irreversible relationship loss, or broken regulated traceability.
- P1: a trigger primarily causes visible interruption, handoff failure, or unavailable task/form/device flow; it is serious but normally fails visibly rather than silently accepting a wrong result.
- P2: a conditional staffing or collaboration scenario is unsupported and blocks continuity, while the current path generally rejects the ambiguous operation.
- P3: optimization with no direct production, quality, authorization, or traceability impact.

## Impact

- Product and users: provides a remediation order for business owners and delivery planning.
- Design, data, APIs, operations, and release: no executable behavior or data changes; later implementation tasks must assess these impacts independently.
- Tests: add structural checks that every retained issue has exactly one priority and that the summary mapping covers all 13 identifiers once.
- Risk: this is severity-based prioritization from current evidence; incident frequency and remediation effort do not reduce the assigned business-impact level.

## Decision

- Accepted.
- P0: 001, 002, 003, 005, 007, 010, 013, 014, 015.
- P1: 004, 006, 009.
- P2: 008.
- P3: none.
- Required approval: satisfied by the user's explicit request to classify the issue set.

## Downstream

- Add the assigned priority and concise basis to every retained issue document.
- Add a task-local priority summary with criteria and remediation order.
- Update task status, expected verification, cleanup keep-list, execution log, and verification report.
- Re-run change-request validation, priority coverage validation, UTF-8 validation, Markdown diff validation, and task closeout cleanup.

## Blockers

- None.
- Outcome: the accepted mapping was written into every retained issue and passed exact one-to-one coverage validation.
