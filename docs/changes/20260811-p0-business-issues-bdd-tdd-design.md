# Design BDD/TDD remediation for nine P0 business issues

## Request

- Requester: user in the current thread.
- Request: prepare a TDD and BDD repair design for each of the nine retained P0 issues.

## Baseline

- Reviewed the P0 mapping in `doc/tasks/20260811-business-issues-001-015/priority-classification.md`.
- Reviewed issue documents 001, 002, 003, 005, 007, 010, 013, 014, and 015.
- Reviewed current route deletion and publish projection, batch-record execution/version metadata, employee process scope, device parameter rules, DCC obsolete flow, MDM product status/reference query, DCC product catalog deletion, and DCC project-code product binding.
- Reviewed recent route, batch record, active-order DCC/QA resolution, MDM binding, and production BDD/TDD task evidence.

## Classification

- Type: accepted remediation-planning requirement for nine critical business risks.
- Delivery type: documentation and acceptance design only; production implementation is explicitly out of scope for this task.

## Impact

- Product and users: defines the required business behavior and remediation order for production, QA, DCC, MDM, and route administrators.
- Data and APIs: proposes formal schema and contract changes where needed, but does not apply them in this task.
- Tests: defines strict test-first sequences, focused backend/frontend commands, regression gates, and real UI paths.
- Release and operations: each issue must be implemented as a separately gated delivery slice; no release is performed now.
- Risk: the nine issues share identity and lifecycle dependencies, so the design package must state implementation order and prohibit partial rollout that creates mixed authority sources.

## Decision

- Accepted.
- Create one independent remediation design per P0 issue and one consolidated acceptance package.
- Required approval: satisfied by the user's explicit request.
- Preserve the existing issue identifiers and do not combine separate root causes into one implementation task.

## Downstream

- Run the BDD/TDD acceptance planner over the nine designs.
- Create consolidated `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, and `test-data.md` inside the task package.
- Validate exact issue coverage, Given/When/Then presence, RED/GREEN evidence templates, UTF-8, and cleanup retention.
- Do not run implementation, database-schema, backend, frontend, release, or deployment skills in this documentation-only task.

## Blockers

- None for design.
- Next action: complete code-evidence review, then write and validate all nine remediation designs.
