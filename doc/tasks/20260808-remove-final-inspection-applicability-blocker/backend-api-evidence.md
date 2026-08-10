# Backend API Evidence

## Scope

- Service scope: `MesOrderReleaseCompletenessServiceImpl.evaluateInspectionResult`.
- Behavior scope: release/PQC completeness no longer blocks when historical published QA regulation version has `finalInspectionApplicable=null`.

## Contract

- `finalInspectionApplicable=true`: FINAL PQC identity is required.
- `finalInspectionApplicable=false`: FINAL PQC identity is not required, but `finalInspectionNotApplicableReason` must be non-empty.
- `finalInspectionApplicable=null`: FINAL PQC identity is not required and no longer blocks release completeness.

## Auth, Permissions, Validation

- No auth or permission contract changed.
- QA regulation save/publish validation remains unchanged for new submissions.
- Route, active order, process snapshot, regulation existence, FIRST/PATROL task identity, and explicit false-without-reason blockers remain unchanged.

## BDD

- BDD: Historical missing applicability no longer blocks -> Given FIRST/PATROL confirmed tasks and a published regulation version with null applicability, When checking release completeness, Then PASS without requiring FINAL.
- BDD: Explicit false still needs reason -> Given applicability is false and reason is blank, When checking release completeness, Then BLOCKER remains.
- BDD: Explicit true still requires FINAL -> Given applicability is true and FINAL task is missing, When checking release completeness, Then BLOCKER remains.

## RED

- `mvn -pl yudao-module-mes test "-Dtest=MesOrderReleaseCompletenessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> FAIL, expected reason: historical null applicability still returned BLOCKER before the code change.

## GREEN

- Blocked: standard Maven GREEN could not safely run while non-task-owned `yudao-module-mes` Maven/Java processes were still writing the same module target.
- Not counted as GREEN: the supplemental isolated-output attempt was stopped because Maven still wrote to default `target\classes`.

## Observability

- Existing release completeness failure reason text remains the observable surface for blockers.

## Blockers

- Standard JUnit verification is blocked until same-module concurrent Maven processes finish.
- Backend evidence validator is pending because the required GREEN test evidence is not available yet.
