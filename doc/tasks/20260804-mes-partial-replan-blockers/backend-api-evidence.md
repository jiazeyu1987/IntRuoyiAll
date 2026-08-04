# Backend API Evidence

## Scope

- `MesProAutoScheduleServiceImpl` 自动排产/重排应用。
- `ScheduleApplier` 应用落库范围与 issue 持久化。
- 排产工单列表响应中的阻断 issue 摘要字段。

## API And Data Contract

- Apply response summary should expose applied, blocked, and skipped work order counts.
- Schedule order rows should expose blocking issue count and latest blocking issue message.
- Per-work-order BLOCKING issues are formal domain records; global/unattributable blockers still fail fast.

## BDD Scenarios

- Mixed replan scope applies healthy orders.
- All selected orders blocked.
- Blocked orders visible in list.

## RED

- Pending.

## GREEN

- Pending.

## Contract Verification

- Pending.

## Observability

- BLOCKING issues must remain queryable through `mes_pro_schedule_issue` and visible in API responses.

## Blockers

- Pending.
