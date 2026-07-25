# Execution Log

## User Intent

- 用户指出红框中的每一项都要单独一行，且“一行”指表格的一行，不是同一单元格内换行。
- 用户指出截图黄色范围内的两条描述应放入测试目标项。

## Scope Boundary

- Owned frontend page: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- Owned static contract: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`
- Owned real E2E assertion text: `IntRuoyiFronted/tests/e2e/system-codex-test-management-real.e2e.js`
- Owned sample E2E data script: `doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs`
- Current workspace had unrelated dirty files before this task; this task will not stage or modify unrelated paths.

## BDD / TDD

- BDD: 方法目标展开成表格行 -> Given 一个测试项有多个方法项和多个目标项 / When 用户打开测试管理列表 / Then 每个方法项或目标项占用独立表格行，同一测试项公共列合并显示。
- BDD: 排产手动重排目标归属 -> Given 手动重排样例包含“重排成功、仅目标两个工单产品编号变橙色、最近一次成功排产时间更新、生产排产甘特图范围” / When 用户查看测试管理列表 / Then 这些核验描述显示在测试目标项列，方法项列只保留操作步骤。
- RED: pending。

## Command Log

- 读取 `frontend-feature-delivery`、`frontend-contract.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：通过。
