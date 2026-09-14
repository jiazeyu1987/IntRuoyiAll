# 一线 PQC 工序选择显示整条冻结工艺路线

## Task Goal

一线 PQC 选择工序时，工序候选必须展示该活跃订单所属冻结工艺路线的完整工序列表；只有存在正式 `PENDING` PQC 任务的工序带出检验任务、规程快照和检验项，可继续填写提交。

## Milestones

- [x] 记录 BDD/RED，复现当前仅展示待检任务工序的问题。
- [x] 后端 `active-order/processes` 将展示集合改为活跃订单冻结路线全量工序，任务上下文仅附着到待检任务工序。
- [x] 前端 PQC 默认选中优先落到首个可填写任务工序，避免打开后停在无任务工序。
- [x] 运行目标回归验证并记录 GREEN 证据。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`
- `git diff --check -- <本任务涉及文件>`

## Applicable Gates

- `docs/backend-development.md#PQC 真实提交前置必须覆盖活跃订单全部冻结工序`：展示集合不得使用当前路线新增工序；本任务按活跃订单 `routeVersionId` 对应的发布快照 `configSnapshots.flowGraph.nodes` 解析冻结全量路线。
- `docs/frontend-development.md#前端弹框候选加载与即时关闭门禁`：PQC 工序选择仍由正式候选数组驱动；仅调整默认选中策略，不跳过选择校验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，分离“工序展示集合”和“PQC 待检任务上下文”两个职责。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed - 实现、目标回归、证据校验和 task-closeout-cleanup preview/apply 均已完成；未执行暂存、提交或推送。
