# Remove PQC-specific issues from business issue set

## Request

- Requester: user in the current thread.
- Request: remove PQC-related issues from `doc/tasks/20260811-business-issues-001-015/issues/` first.
- Scope interpretation: remove documents whose primary subject is PQC; retain cross-domain issues that only mention PQC as one affected role.

## Baseline

- Reviewed the active task, execution log, verification report, and all issue filenames in the 001-015 set.
- `011.md` is specifically about freezing the QA regulation version for PQC pending inspections.
- `012.md` is specifically about the event boundary between PQC submission, PQC leader review, and production reporting.
- The remaining issue documents are primarily about routing, QA, DCC, MDM, forms, or production governance rather than PQC itself.
- No PRD, system design, roadmap, or release plan change is required because this request only narrows the current audit-document scope.

## Classification

- Type: requirement scope change for a documentation deliverable.
- Decision owner: the user who requested the issue set and explicitly narrowed its scope.

## Impact

- Product and users: the delivered issue set will no longer contain PQC-specific findings.
- Design, data, and APIs: no impact; no product behavior, business data, schema, or API is changed.
- Tests: structural documentation verification changes from 15 continuous files to 13 retained files with original identifiers.
- Schedule and release: negligible; this is not a release or implementation change.
- Risk: renumbering could break existing references, so retained issue identifiers will not be changed.

## Decision

- Accepted.
- Delete `011.md` and `012.md` only.
- Keep `001.md`-`010.md` and `013.md`-`015.md` with their current identifiers.
- Required approval: satisfied by the user's explicit request; no additional product, release, data, or security approval is required.

## Downstream

- Update the existing task goal, expected verification, cleanup keep-list, execution log, and verification report.
- Re-run change-request evidence validation, issue-set structural validation, UTF-8 validation, and Markdown diff validation.
- No PRD, system-design, BDD/TDD implementation plan, roadmap, release, or operations rerun is needed because executable behavior is unchanged.

## Blockers

- None.
- Outcome: the two accepted PQC-specific issue documents were removed and the retained issue set passed structural verification.
