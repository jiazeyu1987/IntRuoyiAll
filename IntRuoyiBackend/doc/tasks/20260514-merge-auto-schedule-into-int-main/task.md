# Task: Merge auto-schedule branch into int_main backend

## Goal

Merge the committed backend history from `feature/auto-schedule-first-loop` into
`int_main` so the main backend branch includes the auto-schedule and production
schedule calendar capability now shipped on the feature branch.

## Scope

- Backend repository only
- Merge committed backend history from `feature/auto-schedule-first-loop` into `int_main`
- Exclude unrelated uncommitted worktree files
- Run targeted backend verification after merge

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-merge-dcc-v1-into-int-main/task.md`
- Status before this task: blocked, but its blocker is specific to incomplete DCC
  branch history and does not prevent merging the committed auto-schedule branch.

## Milestones

- [x] B1: Previous backend task checked and blocker scope confirmed.
- [x] B2: Merge task document created before Git changes.
- [x] B3: Merge committed feature history into `int_main`.
- [ ] B4: Run targeted backend verification on merged result.
- [ ] B5: Record evidence and complete the merge commit on `int_main`.

## Expected Verification

- `git merge --no-ff feature/auto-schedule-first-loop`
- `mvn -f pom.xml -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Blocked on verification after merge. The committed backend auto-schedule and
schedule-calendar history has been integrated into `int_main`, but the targeted
MES module verification is currently blocked by existing `int_main` compile
problems outside the merge scope.
