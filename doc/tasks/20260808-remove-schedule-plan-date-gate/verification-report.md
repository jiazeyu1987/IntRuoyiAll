# Verification Report

## Scope

- Remove the active-order/PQC blocker that rejected scheduled processes when `planDate` is null.
- Preserve formal route, QA regulation, quantity, and PQC task identity checks.
- Keep PQC `businessDate` non-null by using process `planDate` when present and persisted active-order `joinedAt` date when `planDate` is absent.

## RED

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" test` -> FAIL before target Surefire because upstream reactor modules had no matching test class; rerun required `"-Dsurefire.failIfNoSpecifiedTests=false"`.
- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected:
  - `shouldAllowScheduledCandidateWithoutPlanDate` returned ineligible.
  - `shouldGeneratePqcTasksForScheduledProcessWithoutPlanDateUsingJoinedDate` threw `排产工序缺少计划日期`.

## GREEN

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
- Result: `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`.

## Regression Checks

- Source search confirmed `MesTeamLeaderActiveOrderServiceImpl` no longer contains `排产工序缺少计划日期` or `requireBusinessDate`.
- `git diff --check -- <task-owned paths>` passed; Git reported only LF-to-CRLF normalization warnings.
- `task_closeout.py --task-id 20260808-remove-schedule-plan-date-gate --mode preview/apply` passed with no delete, blocked, or warning entries.

## Risks

- No fallback, silent downgrade, mock success, or swallowed exception was introduced.
- Formal fail-fast checks for route source, QA regulation, published version, inspection items, process identity, and positive quantities remain active.
