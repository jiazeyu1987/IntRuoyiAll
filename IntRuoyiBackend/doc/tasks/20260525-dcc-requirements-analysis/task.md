# 任务：DCC 截图需求分析（后端）

- 任务编号：`20260525-dcc-requirements-analysis`
- 创建日期：`2026-05-25`
- 状态：`已完成`
- 仓库：`ruoyi-vue-pro`
- Worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-requirements-analysis\ruoyi-vue-pro`
- 分支：`task/20260525-dcc-requirements-analysis`

## 任务目标

在 IntRuoyi 后端仓库中记录用户提供的 DCC 截图需求，形成后续后端流程、权限、审计、文件和账号策略设计可复用的需求分析与产品草稿。

## 证据来源

- 用户提供的 DCC 需求截图。
- 截图表头：`优先级`、`需求模块`、`需求内容`。
- 后端上一同仓任务 `20260524-ebr-report-visual-fidelity` 已完成。

## 里程碑

- [x] M1：确认 IntRuoyi 后端 worktree 和同名分支。
- [x] M2：创建任务文档和 BDD 记录。
- [x] M3：转写截图中 14 条可见需求。
- [x] M4：按产品主题分组并识别后端影响。
- [x] M5：形成 DCC 专属 PRD、用户流程和验收标准草稿。
- [x] M6：完成 UTF-8 读取、结构检查、Git 状态检查和提交。
- [x] M7：启动 4 个子 agent 补充后端、前端、BDD/TDD/E2E、复用风险开发文档。
- [x] M8：主 reviewer 按四项放行标准完成文档评审。

## 预期验证

- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/task.md`
- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md`
- `Get-Content -Encoding utf8 docs/product/dcc-screenshot-requirements-prd.md`
- `rg -n "Purpose and Scope|Evidence Reviewed|Functional Requirements|Acceptance Criteria|Open Questions|Product Blockers" docs/product/dcc-screenshot-requirements-prd.md`
- `git status --short`

## 当前状态

已完成。DCC 截图需求已整理为任务分析、PRD、用户流程、验收标准、子 agent 开发设计文档和 reviewer 放行报告，且未修改生产代码。

## 已完成工作

- 创建后端任务目录 `doc/tasks/20260525-dcc-requirements-analysis/`。
- 转写截图中的 14 条可见需求。
- 归并为文件受控审批、流程动作、下载/发放审计、账户/视图状态、外来文件评审 5 类。
- 新增 DCC 专属产品草稿，避免覆盖既有自动排产产品文档。
- 通过 4 个子 agent 补充后端接口设计、前端交互设计、BDD/TDD/E2E 计划、真实测试数据计划和复用风险矩阵。
- 主 reviewer 复核文档满足截图目标、TDD+BDD+subagent-driven、逻辑自洽接口清晰、复用当前系统能力四项放行标准。

## 最终验证

- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/task.md` -> PASS，中文可读。
- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md` -> PASS，14 条可见需求完整。
- `rg -n "Purpose and Scope|Evidence Reviewed|Functional Requirements|Acceptance Criteria|Open Questions|Product Blockers" docs/product/dcc-screenshot-requirements-prd.md` -> PASS，PRD 关键章节齐全。
- `rg -n "TODO|TBD|fill in later|to be decided" ...` -> PASS，无弱占位词。
- `rg -n "BDD:|RED:|GREEN:|Subagent|复用|不得|阻塞" doc/tasks/20260525-dcc-requirements-analysis docs/acceptance` -> PASS，开发文档包含 BDD/TDD/subagent-driven、复用约束和 blocker。
- `Get-Content -Encoding utf8 doc/tasks/20260525-dcc-requirements-analysis/review-report-round-1.md` -> PASS，reviewer 放行结论为 `PASS_FOR_DOCUMENT_RELEASE`。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-dcc-requirements-analysis --mode preview --worktree-closeout off --extra-keep doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md --extra-keep docs/product/dcc-screenshot-requirements-prd.md --extra-keep docs/product/dcc-screenshot-requirements-user-flows.md --extra-keep docs/product/dcc-screenshot-requirements-acceptance-criteria.md` -> READY，delete none，blocked none。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-dcc-requirements-analysis --mode preview --worktree-closeout off --extra-keep doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md --extra-keep doc/tasks/20260525-dcc-requirements-analysis/backend-development-design.md --extra-keep doc/tasks/20260525-dcc-requirements-analysis/reuse-integration-risk-matrix.md --extra-keep doc/tasks/20260525-dcc-requirements-analysis/review-report-round-1.md --extra-keep docs/product/dcc-screenshot-requirements-prd.md --extra-keep docs/product/dcc-screenshot-requirements-user-flows.md --extra-keep docs/product/dcc-screenshot-requirements-acceptance-criteria.md --extra-keep docs/acceptance/dcc-screenshot-bdd-scenarios.md --extra-keep docs/acceptance/dcc-screenshot-tdd-plan.md --extra-keep docs/acceptance/dcc-screenshot-e2e-plan.md --extra-keep docs/acceptance/dcc-screenshot-test-data.md` -> READY，delete none，blocked none。
- `git status --short` -> PASS，仅包含本任务文档、DCC 专属产品草稿、子 agent 开发文档与 reviewer 放行报告。

## 阻塞与待确认

- 无阻塞影响本次分析交付。
- 后续实现前需确认文件受控审批第四节点、角色权限、编码规则、通知渠道、模板规则和密码更新周期。

## Cleanup Keep

- `doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md`
- `doc/tasks/20260525-dcc-requirements-analysis/backend-development-design.md`
- `doc/tasks/20260525-dcc-requirements-analysis/reuse-integration-risk-matrix.md`
- `doc/tasks/20260525-dcc-requirements-analysis/review-report-round-1.md`
- `docs/product/dcc-screenshot-requirements-prd.md`
- `docs/product/dcc-screenshot-requirements-user-flows.md`
- `docs/product/dcc-screenshot-requirements-acceptance-criteria.md`
- `docs/acceptance/dcc-screenshot-bdd-scenarios.md`
- `docs/acceptance/dcc-screenshot-tdd-plan.md`
- `docs/acceptance/dcc-screenshot-e2e-plan.md`
- `docs/acceptance/dcc-screenshot-test-data.md`
