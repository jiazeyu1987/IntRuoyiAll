# Task: 生产工单金蝶新增默认已确认后端

## Goal

调整 Kingdee 生产工单同步逻辑：对于本地之前不存在、这次新创建的工单，默认统一落为 `已确认` 状态，而不是按 ERP 状态再映射为草稿或完成。

## Scope

- 先创建当前后端任务文档，再开始生产代码修改。
- 严格按 BDD + TDD 先补失败测试，再做最小实现。
- 仅修改 Kingdee 生产工单同步路径，不改普通手工创建工单逻辑。
- 不新增 fallback；如果同步前置条件缺失，继续按现有 fail-fast 规则处理。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-batch-record-generic-layout-rules-followup/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused layout follow-up does not block this work-order sync behavior change.

## Milestones

- [x] M1: Create backend task directory, task doc, execution log, and evidence file.
- [x] M2: Record BDD scenarios and RED evidence for new-sync status behavior.
- [x] M3: Implement the minimal Kingdee sync status change.
- [x] M4: Run targeted backend verification and update evidence.
- [ ] M5: Commit only backend files produced by this task.

## Expected Verification

- `mvn --% -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed for code delivery. New Kingdee-synced work orders now default to `已确认` and the targeted unit test passes.
