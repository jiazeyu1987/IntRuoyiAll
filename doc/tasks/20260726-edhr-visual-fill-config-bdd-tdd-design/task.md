# eDHR 可视化填写配置 BDD/TDD 设计

## Task Goal

基于现有批记录表单、单元格规则、填写人配置、辅助模式与执行快照能力，设计一套操作简单的可视化填写配置方案，使模板管理员可以在原表上完成单元格类型纠错、辅助行编排、行描述维护和员工填写责任分配，并形成可直接进入实现的 BDD、严格 TDD、E2E 与测试数据文档。

## Milestones

1. `completed`：核对现有前后端能力、测试资产和版本治理边界。
2. `completed`：完成需求范围、复用方案和最小数据/API 设计。
3. `completed`：完成 BDD 场景、严格 TDD 顺序、E2E 路径和测试数据设计。
4. `ready_for_closeout`：完成结构校验、UTF-8 校验和任务验证；收尾合并受主 worktree 状态阻塞。

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

## Cleanup Keep

- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/design.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/bdd-scenarios.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/tdd-plan.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/e2e-plan.md
- doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/docs/acceptance/test-data.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少映射、填写人、字段类型或失效字段引用时必须阻止保存或发布。
- `是否从根因和长期维护角度解决`：是；将现有分散入口收敛为模板版本内的可视化填写配置，并复用现有执行快照和字段审计链。
- `是否存在临时补丁或绕过`：否。

## Final Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\20260726-edhr-visual-fill-config-bdd-tdd-design`：PASS。
- `git diff --check`：PASS。
- UTF-8 读取任务文档和验收文档：PASS。

