# eDHR 可视化填写配置 BDD/TDD 设计

## Task Goal

基于现有批记录表单、单元格规则、填写人配置、辅助模式与执行快照能力，设计一套操作简单的可视化填写配置方案，使模板管理员可以在原表上完成单元格类型纠错、辅助行编排、行描述维护和员工填写责任分配，并形成可直接进入实现的 BDD、严格 TDD、E2E 与测试数据文档。

## Milestones

1. `completed`：核对现有前后端能力、测试资产和版本治理边界。
2. `completed`：完成需求范围、复用方案和最小数据/API 设计。
3. `completed`：完成 BDD 场景、严格 TDD 顺序、E2E 路径和测试数据设计。
4. `completed`：完成结构校验、UTF-8 校验和任务验证。
5. `blocked`：任务提交与推送等待并发工作区改动解除或取得统一基线提交授权。

## Expected Verification

- BDD 场景覆盖单元格类型纠错、员工分配、辅助行编排、行描述、实时预览、运行态字段权限和版本隔离。
- 每项生产行为均有明确 RED 原因、最小实现目标、GREEN 命令和回归范围。
- 设计明确复用现有批记录表单列表、单元格规则、填写人候选来源、执行快照、字段审计和辅助模式草稿链路。
- 不新增与现有单元格规则、填写人配置或执行字段值重复的数据体系。
- `validate_acceptance_plan.py`、UTF-8 读取和 `git diff --check` 通过。

## Experience Gate Summary

- 先使用聚焦静态合同建立当前需求 RED，不通过扩大旧测试范围掩盖历史问题。
- 字段填写人显示和运行态范围必须来自正式后端数据，不能只做前端隐藏。
- 批记录模板规则必须进入版本快照；运行态不得从当前模板 JSON 静默回退。
- 文档只定义真实用户路径；缺少登录、租户、菜单、运行态或测试数据时记录为 blocker。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少映射、填写人、字段类型或失效字段引用时必须阻止保存或发布。
- `是否从根因和长期维护角度解决`：是；将现有分散入口收敛为模板版本内的可视化填写配置，并复用现有执行快照和字段审计链。
- `是否存在临时补丁或绕过`：否。

## Verification Summary

- `validate_acceptance_plan.py`：PASS。
- 所有任务 Markdown 以 UTF-8 严格读取：PASS。
- 必需验收标题与 Given/When/Then 结构检查：PASS。
- `git diff --check`：PASS，未发现当前已跟踪差异的空白错误。
- 本任务为设计文档交付，未修改生产代码，因此未运行 Maven、前端静态合同或真实 E2E。

## Closeout Blocker

- 根工作区存在其他任务正在修改的已跟踪文件和未跟踪任务目录。
- 项目规则要求脏工作区先提交全部改动作为基线，但当前任务所有权规则禁止把并发任务文件纳入本任务提交。
- 在并发改动解除或用户明确授权统一基线提交前，本任务不暂存、不提交、不推送，并保持 `ready_for_closeout`。

## Cleanup Keep

- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/design.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/bdd-scenarios.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/tdd-plan.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/e2e-plan.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/test-data.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/verification-report.md
