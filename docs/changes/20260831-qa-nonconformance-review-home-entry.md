# QA 不合格待审入口可见性变更

## Request Summary

QA 页面需要直接看到不合格评审待审数量，并能从首页入口进入统一不合格评审页。

## Baseline Reviewed

- 统一不合格评审页已存在，列表标题为 `QA冻结批次列表`。
- 该页当前是隐藏路由，QA 从首页不容易直接发现。
- 首页已有“待办与异常”区域，适合承载待审数量 badge。

## Classification

前端可见性增强，小范围数据展示补充。

## Impact

- Product: QA 能在首页直接看到待审数量并进入处理页。
- Design: 复用首页现有待办 badge 风格，不引入新页面。
- Data/API: 首页汇总新增一个待审数量字段。
- Tests: 补首页统计单测和前端静态契约。
- Release: 仅新增字段和展示，不影响现有处置流程。

## Decision

Accept.

## Required Approvals

None.

## Downstream Reruns

- `frontend-feature-delivery`
- BDD / strict TDD for the new visible entry

## Blockers

None.
