# Frontend Feature Evidence

## Feature Goal

排产工单自动重排确认时，局部阻断不再禁用整批应用；阻断工单在列表中红色提示，并可查看最新阻断原因。

## Non-Goals

- 不改变全局日历 token、无选择范围、冻结/完成/取消等现有不可应用门禁。
- 不新增 mock 数据或静默成功。

## Entry Points

- `IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`
- `IntRuoyiFronted/src/api/mes/pro/task/autoSchedule/index.ts`
- `IntRuoyiFronted/src/api/mes/pro/scheduleorder/index.ts`

## API States

- Summary includes applied, blocked, and skipped counts.
- Schedule order row includes `blockingIssueCount` and `latestBlockingIssueMessage`.

## BDD Scenarios

- Mixed replan scope applies healthy orders.
- Blocked orders visible in list.

## RED

- Pending.

## GREEN

- Pending.

## UI Checks

- Pending.

## Blockers

- Pending.
