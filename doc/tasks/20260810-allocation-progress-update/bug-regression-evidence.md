# Bug Regression Evidence

## Bug Summary

报工分配确认链路存在两个回归风险：分配行数量为 null 或 0 时后端按非法数量拒绝；当当前分配已经与请求一致但生产进度尚未同步时，保存逻辑直接返回快照，不会触发订单工序完成进度重算。

## Expected Behavior

- 分配数量为空或 0 时按 0 处理，不写入分配行，不提示必须为正数。
- FIFO 自动分配或手动分配只要让订单当前工序累计分配满额，就必须触发订单工序完成进度重算，进而让活跃订单生产进度刷新。

## Reproduction Command

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Root Cause

- `MesReportAllocationCommandService.aggregateDesired` 把 null、0 和负数统一视为非法数量，未按业务要求把空/0 规范化为 0。
- `MesReportAllocationCommandService.save` 在 `before.equals(desired)` 时直接返回，跳过 `MesTeamLeaderOrderProcessCompletionService.reconcileAffectedAllocations`，导致已有满额分配无法补触发完成进度同步。

## Regression Test

- `MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving`
- `MesReportAllocationCommandServiceTest#manualFullAllocationSameAsCurrentMustStillReconcileCompletionProgress`
- `MesReportAllocationCommandServiceTest#fifoFullAllocationSameAsCurrentMustStillReconcileCompletionProgress`

## RED

RED:

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldTreatNullAndZeroAllocationQuantitiesAsZeroWhenSaving" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: FAIL，后端抛出 `PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED`，位置为 `MesReportAllocationCommandService.aggregateDesired`。

## GREEN

GREEN:

```powershell
mvn -q -pl yudao-module-mes "-Dtest=MesReportAllocationCommandServiceTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result: PASS，`MesReportAllocationCommandServiceTest` 全类通过。

## Risk And Regression Scope

## Verification

风险集中在报工分配确认链路。改动只跳过 null/0 分配行，负数仍然失败；同量确认只在已有当前分配时触发完成进度重算，不重写分配版本和分配流水。

## Blockers And Follow-up

当前无阻塞。未执行真实浏览器 E2E；本次修复的核心风险已由后端服务层回归覆盖，前端残留旧校验文案搜索为空。
