# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 现象：一线 PQC 加载活跃订单时把旧路线版本与当前路线工序组合，导致已发布 QA 规程查找失败。
- 期望：待执行任务使用当前唯一 ACTIVE 路线及匹配 PUBLISHED QA 规程；旧 PENDING 任务刷新；已提交任务保持冻结。

## Reproduction

- 真实数据：`activeOrderId=30 / old routeVersionId=448 / current routeProcessId=980645 / processId=922985`。
- 自动化回归命令：待 RED 测试确定后补充。

## Root Cause

- `listProcessesByActiveOrder` 从当前路线工序表读取新 routeProcess，却继续把 `activeOrder.routeVersionId` 作为规程版本条件；路线重新发布后产生不可能匹配的跨版本组合。

## Regression Test

- 待 RED 阶段补充。

## RED And GREEN

- RED：pending。
- GREEN：pending。

## Risk And Regression Scope

- 风险集中在活跃订单版本切换、PQC PENDING 任务刷新、已提交任务追溯和 QA 规程精确身份。

## Blockers And Follow-up

- 当前无 blocker。

