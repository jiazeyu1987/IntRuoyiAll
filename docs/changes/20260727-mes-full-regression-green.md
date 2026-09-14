# 变更请求：MES 完整回归全绿

## Request Summary

- 请求来源：用户在 2026-07-27 明确选择扩大当前 eDHR 通知任务范围，要求修复完整 `yudao-module-mes` 回归失败后再收尾。
- 原始任务：eDHR 工作任务站内信发送给同一任务候选快照中的全部有效候选人。
- 新增范围：修复 `mvn -pl yudao-module-mes test` 暴露的全部 MES 测试失败、错误和缺失前置，直到完整模块回归通过。

## Current Baseline Reviewed

- eDHR 通知实现及目标测试已通过：目标场景 3/3，同类 `MesProEdhrWorkTaskServiceImplTest` 66/66。
- 标准 reactor 定向生命周期已于 2026-07-27 20:40:03 通过，3 tests、0 failures、0 errors，24 个模块均 `BUILD SUCCESS`。
- 完整 MES 模块回归已完整执行：2509 tests、58 failures、78 errors、31 skipped。
- 已确认失败包含排产契约/服务测试、缺失本机 Word/Excel fixture、数据库测试上下文与唯一键问题、legacy 测试契约漂移及其他既有测试。
- 当前共享工作区存在并发任务的未提交前端和任务文档改动，不能覆盖、回滚或混入本变更。

## Classification

技术约束与质量门禁范围变更：从单一业务行为修复扩展为 MES 模块回归基线修复。

## Impact

- 产品范围：不新增业务功能；必须保持 eDHR 通知候选快照语义不变。
- 代码范围：可能涉及 MES 生产代码、MES 测试、测试 fixture 和测试基础设施；每个失败簇必须独立确认根因后修改。
- 数据与 schema：若失败涉及数据库测试或 schema，必须先按 `docs/database-rules.md` 核对真实结构和迁移，不得直接写运行 SQL。
- API 与权限：不得因测试失败改变通知 API、候选快照来源、权限边界或引入 assignee/当前登录人 fallback。
- 验证范围：要求目标测试、失败簇回归、完整 `mvn -pl yudao-module-mes test` 和证据校验全部通过。
- 发布风险：全量修复可能扩大改动面；任何无法证明属于本次失败根因的变更不得纳入。
- 依赖与环境：缺失真实 Word/Excel fixture、数据库、并发文件冲突或测试服务缺失时必须 fail fast 并记录。

## Decision

接受，但拆分为受控复杂交付流程，先完成需求与测试计划审查，再按失败簇执行。不得以跳过测试、排除测试、放宽断言、mock 成功、默认数据或静默降级替代修复。

## Required Approvals

- 本次用户已明确确认扩大范围。
- 涉及数据库、远端服务、生产数据、发布或 worktree 操作时，仍需按项目对应门禁取得额外授权；当前范围不自动包含这些操作。

## Downstream Skill Reruns

- `supervised-complex-delivery`：创建需求分析、PRD、依赖任务图、测试计划和独立测试报告。
- `quality-assurance-test-suite`：建立失败簇到验收标准的测试矩阵并独立验证完整模块回归。
- `database-schema-delivery`：仅在确认需要修改 schema、migration 或数据库测试夹具时启用。
- `backend-api-delivery` / `bug-regression-fix-loop`：每个生产行为或测试回归根因修复继续执行 BDD -> RED -> GREEN。

## Blockers and Next Action

- 当前完整回归失败列表需要按失败簇分类，且缺失 fixture 的真实来源和归属尚未全部确认。
- 规划代理先产出 `request-analysis.md` 与 `prd.md`；审查通过后再产出 `dev-plan.md`、`test-plan.md`，随后按依赖图执行。
