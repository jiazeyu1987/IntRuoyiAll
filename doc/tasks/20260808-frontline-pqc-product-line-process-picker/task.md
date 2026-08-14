# 一线 PQC 工序选择按生产工单产品产线展示全工序

## Task Goal

一线 PQC 选择工序时，工序候选必须展示当前生产工单对应产品绑定的对应产线/工艺路线的全部工序；只有存在正式 `PENDING` PQC 任务的工序带出检验任务、规程快照和检验项，可继续填写提交。

## Milestones

- [x] 记录 BDD/RED，复现当前只展示单个 PQC 待检工序的问题。
- [x] 后端 `active-order/processes` 改为按生产工单产品对应产线/路线返回全量工序，PQC 任务上下文仅附着到待检工序。
- [x] 前端保持正式候选数组驱动，PQC 默认选中仍优先落到首个可填写任务工序。
- [x] 运行目标回归验证并记录 GREEN 证据。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`
- `git diff --check -- <本任务涉及文件>`

## Applicable Gates

- `docs/backend-development.md#PQC 真实提交前置必须覆盖活跃订单全部冻结工序`：本次用户明确改口径为生产工单产品对应产线/路线全工序，任务文档记录此验收范围变更；仍禁止用 `formBindings`、默认工序、空成功或吞异常替代正式路线工序来源。
- `docs/frontend-development.md#前端选择弹框即时反馈门禁`：PQC 工序选择仍由正式候选数组驱动；仅保持默认选中策略，不跳过提交校验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，重新明确“展示集合=生产工单产品对应产线/路线全工序”，“可提交上下文=PENDING PQC 任务”。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260808-frontline-pqc-product-line-process-picker/bug-regression-evidence.md`
