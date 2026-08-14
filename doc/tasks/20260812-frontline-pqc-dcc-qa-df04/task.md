# DF04 Unique DCC Project Resolver

## Task Goal

Add an independent MES resolver that starts from the active-order route snapshot identity and the formal route-DCC relationship, and returns exactly one enabled, same-tenant, non-deleted DCC project identity. Missing, disabled, deleted, cross-tenant, or ambiguous data must fail fast.

## Milestones

- [x] Read required project rules and DF04 design/test contracts.
- [x] Record BDD scenarios before production changes.
- [x] RED: add resolver contract tests and confirm they fail for the missing resolver.
- [x] GREEN: implement the smallest formal resolver.
- [x] Regression: run the targeted Maven tests and static checks.
- [x] Closeout: record verification evidence and final status.

## Expected Verification

- `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineDccProjectResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- `python C:/Users/BJB110/.codex/skills/backend-api-delivery/scripts/validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df04/backend-api-evidence.md`

## Applicable Experience Gate

- `docs/experience-index.md` exists and was inspected.
- Applicable backend gate: formal route-DCC identity must be authoritative; do not infer DCC through product/material master data or other runtime presentation data.
- Applicable Maven gate: a filesystem-level `target` failure must be reported as an environment blocker instead of changing the validation command or hiding the failure.
- The DF04 design file was absent from this worktree. The identical required path exists in the main workspace and was read read-only; its resolver contract matches the supervisor's approved DF04 payload.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`: 否。
- `是否从根因和长期维护角度解决`: 是。Resolver only composes the formal route-DCC relation with DCC project identity validation.
- `是否存在临时补丁或绕过`: 否。

## Current Status

completed：RED、GREEN、组合回归、禁止推算扫描、后端证据校验、主管复核和独立验证均已完成；实现提交 d781ca689 已 fast-forward 合入 int_main；task-closeout-cleanup 预览和 apply 均通过，正式证据全部保留，任务具备删除 worktree 和释放槽位条件。
