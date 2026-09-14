# 一线 PQC 产品产线全工序选择回归证据

## Bug Summary

一线 PQC 选择工序时仍然只能看到有 `PENDING` PQC 任务的单个工序，未展示当前生产工单对应产品绑定的对应产线/工艺路线全部工序。

## Expected Behavior

`active-order/processes` 返回生产工单产品当前路线的全部正式工序；只有存在正式 `PENDING` PQC 任务的工序携带 `pqcTaskId`、规程快照和检验项，未待检工序不可提交。

## Reproduction

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

后端候选工序来源仍按活跃订单冻结路线快照或待检任务上下文收敛，导致只有一个 PQC 待检任务时，候选列表被缩小为单工序，未按生产工单产品对应路线读取完整工序集合。

## Regression Test

- 更新 `MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask`：构造同一路线两道工序、仅第一道存在 `PENDING` PQC 任务，断言返回 `[4001, 4002]`，第二道 `pqcTaskId == null` 且检验项为空。

## RED Evidence

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected: <[4001, 4002]> but was: <[4001]>`

## GREEN Evidence

- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test -> PASS`
- `GREEN: mvn -q -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test -> PASS`
- `GREEN: node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs -> PASS`
- `GREEN: git diff --check -- <task files> -> PASS, only CRLF normalization warnings`

## Verification

- 后端单用例、后端整类、前端静态契约和差异空白检查均已通过。

## Risk And Regression Scope

- 风险范围集中在一线 PQC `active-order/processes` 候选工序来源。
- 提交链路仍依赖正式 `pqcTaskId`，未给无待检任务工序创建默认成功或降级提交路径。
- 产品路线绑定仍通过 `requireProductRoute` fail fast，未引入任意路线或空成功。

## Blockers And Follow-Up

- 无阻塞。
- Windows Maven 增量编译存在已知清理卡顿风险，本次 GREEN 使用一次性 `-Dmaven.compiler.useIncrementalCompilation=false`，未写入构建配置。
