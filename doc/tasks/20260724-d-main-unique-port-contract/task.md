# D Main Unique Port Contract

## Task Goal

Assign D-Main a unique local runtime profile so all five IntRuoyi base directories use distinct port pairs.

## Milestones

- [x] Review conflict.
- [x] Add int_main_d with 8101/48101.
- [ ] Verify guard and profile.
- [ ] Commit and push.

## Expected Verification

- D-Main resolves to 8101/48101.
- E-Main remains 8081/48081.
- Batch, Shedule, and QMS remain 8041/48041, 8021/48021, and 8061/48061.

## Current Status

in_progress

## Design Constraints

- Fallback/degradation/swallowed exceptions: no.
- Root cause and maintainability: yes; use a path-bound profile and guard.
- Temporary workaround: no.
