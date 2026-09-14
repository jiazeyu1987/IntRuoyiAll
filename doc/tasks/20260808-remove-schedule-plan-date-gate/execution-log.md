# Execution Log

## User Intent

- 用户要求去除“排产工序缺少计划日期”这个限制。

## BDD

- BDD: Scheduled process without plan date can become active order -> Given a confirmed work order has one effective scheduled process with published QA regulation but `planDate` is null / When the production leader searches candidates and adds the active order / Then the candidate is eligible and PQC tasks are generated with a deterministic business date instead of blocking with “排产工序缺少计划日期”.

## Command Evidence

- Skill: `bug-regression-fix-loop` loaded with evidence contract.
- Rule files read: `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/backend-development.md`, `docs/engineering/technology-stack-routing.md`.
- Experience gate: `docs/experience-index.md` routes active-order/PQC work to `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`; Maven verification also follows `docs/powershell-memory.md` `-D` quoting and `-am` reactor gates.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" test` -> FAIL before target Surefire because upstream module had no matching tests; rerun required `"-Dsurefire.failIfNoSpecifiedTests=false"`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL as expected: `shouldAllowScheduledCandidateWithoutPlanDate` candidate was ineligible, and `shouldGeneratePqcTasksForScheduledProcessWithoutPlanDateUsingJoinedDate` threw `排产工序缺少计划日期`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`.
- REGRESSION: `rg "排产工序缺少计划日期|requireBusinessDate|resolvePqcBusinessDate" <service/test>` -> old production blocker text no longer exists in `MesTeamLeaderActiveOrderServiceImpl`.
- REGRESSION: `git diff --check -- <task-owned paths>` -> PASS; warnings only reported LF-to-CRLF normalization on touched files.
- Experience consolidation: updated existing `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线` instead of creating a new long-term memory document.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-remove-schedule-plan-date-gate --mode preview` -> PASS, keep only `task.md`, `execution-log.md`, `verification-report.md`; delete none; blocked none; warnings none.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-remove-schedule-plan-date-gate --mode apply` -> PASS, deleted none.

## Milestone Updates

- in_progress: Located the plan-date gate in `MesTeamLeaderActiveOrderServiceImpl`.
- in_progress: Copied the applicable experience gate into `task.md`.
- completed: Added regression coverage for scheduled processes with null `planDate`.
- in_progress: Removed plan-date as candidate blocker and resolved PQC business date from `planDate` when present, otherwise active-order `joinedAt`.
- completed: Targeted Maven regression passed and task status moved to `ready_for_closeout`.
- completed: Cleanup preview/apply passed and task status moved to `completed`.
