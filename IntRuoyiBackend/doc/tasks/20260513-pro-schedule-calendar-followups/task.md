# Task: Schedule Calendar follow-up hardening

## Goal

Close the remaining backend/runtime gaps after the schedule calendar v1 delivery:

- Bring canonical database bootstrap scripts closer to the runtime schema actually required by auto-schedule + schedule-calendar.
- Resolve or explicitly bound the remaining current-schedule issue persistence gap so the behavior is consistent with the blocking shortage rule.

## Scope

- Update backend/runtime documentation and validation evidence for the follow-up work.
- Update canonical bootstrap SQL where the runtime feature requires tables that currently exist only in helper patches.
- If the issue persistence gap can be fixed without violating the existing blocking shortage rule, implement it; otherwise record the exact product boundary and leave the runtime behavior explicit.

## Previous Task Check

- `doc/tasks/20260513-pro-schedule-calendar-v1/task.md` is completed.

## Milestones

- [x] F1: Previous backend task checked complete before new work.
- [x] F2: Follow-up task document created before production code changes.
- [x] F3: Inspect canonical schema parity and issue persistence constraints.
- [x] F4: Implement safe schema parity updates.
- [x] F5: Implement or explicitly bound the issue persistence follow-up.
- [x] F6: Targeted verification and evidence update pass.
- [ ] F7: Follow-up changes committed on `feature/auto-schedule-first-loop`.

## Expected Verification

- Canonical bootstrap SQL contains the runtime tables required by the shipped feature.
- Follow-up behavior around schedule issues is explicit and testable.
- No hidden downgrade or silent behavior change is introduced.

## Current Status

Implementation and targeted verification are complete on
`feature/auto-schedule-first-loop`. Remaining work is the final scoped Git
commit for this follow-up.
