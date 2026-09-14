# DF01 Active Order Selection Identity

## Task Goal

Return every effective PQC active-order row as its own selectable option, using activeOrderId as the stable identity. The list must not filter by PQC task state and must not deduplicate by workOrderId + routeId.

## Milestones

- [ ] M1: Record BDD and create RED coverage for duplicate work-order/route active orders.
- [ ] M2: Implement the minimal backend contract change.
- [ ] M3: Run GREEN and regression evidence.
- [ ] M4: Record supervisor handoff evidence.

## Expected Verification

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlineActiveOrderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以 activeOrderId 作为正式身份，去除列表层工单+路线去重。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout：DF01 已完成 BDD、RED、GREEN、静态复核、backend evidence 和主管独立复核；等待本地提交与 fast-forward 合并。
