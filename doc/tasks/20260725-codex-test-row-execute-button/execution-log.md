# Execution Log

## User Intent

- 用户要求在截图黄框内增加“执行”按钮。
- 点击该按钮后，只针对当前表格项执行。

## Scope Boundary

- Owned frontend page: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- Owned static contract: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`
- Current workspace has unrelated dirty files from concurrent tasks; this task will not stage or modify unrelated paths.

## BDD / TDD

- BDD: 单项执行按钮 -> Given 测试管理列表存在多个测试项 / When 用户点击某一行操作列的“执行”按钮 / Then 系统只以该行测试项 ID 调用执行接口，不依赖复选框已选集合，也不影响其他行。
- RED: `node tests/e2e/system-codex-test-management-static.spec.js` -> FAIL, expected reason: `startSingleCaseExecution` 不存在，操作列没有单项执行契约。
- GREEN: `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/system-codex-test-management-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS, exit code 0。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-codex-test-row-execute-button/frontend-feature-evidence.md` -> PASS, Frontend feature evidence is valid.

## Command Log

- 读取 `frontend-feature-delivery`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`：通过。
- 读取 `frontend-contract.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：通过。
- 经验门禁：静态合同同步、Element Plus 表格操作、Codex Runner 前置条件均命中；本次只做页面入口与静态/类型验证，不触发真实 Runner。
- 静态合同新增断言：页面必须包含 `startSingleCaseExecution`、行内 `@click="startSingleCaseExecution(row)"`、`caseIds: [caseId]` 和 `executionMode: row.defaultExecutionMode`。
- 页面实现：操作列宽度从 `180` 调整为 `220`，增加 link 型 `执行` 按钮；按钮权限沿用 `system:codex-test:execute`，禁用条件为未选择测试租户、正在执行或当前行无 ID。
- 单项执行函数：`startSingleCaseExecution(row)` 只读取当前行 `row.id`，调用现有 `CodexTestApi.startCodexTestExecution`，请求体 `caseIds` 只包含当前 `caseId`，`executionMode` 使用 `row.defaultExecutionMode`。
- project-experience-consolidation：检索 `docs/*memory*.md` 和 `docs` 中 `单项执行/Codex Runner` 相关文档；既有 `docs/system/frontend-design.md` 已覆盖单项执行范围，`docs/e2e-rules.md#codex-runner-自动测试门禁` 已覆盖 Runner 门禁；无新增长期经验文档。
- 状态更新：实现与验证完成，`task.md` 设置为 `ready_for_closeout`，准备执行 cleanup preview/apply。
- cleanup preview：`task_closeout.py --task-id 20260725-codex-test-row-execute-button --mode preview` -> ready；keep `task.md`、`execution-log.md`、`verification-report.md`、`frontend-feature-evidence.md`；delete none；blocked none；warnings none。
- cleanup apply：`task_closeout.py --task-id 20260725-codex-test-row-execute-button --mode apply` -> applied；deleted none；linked worktree false。
- 状态更新：cleanup 已完成，任务状态设置为 `completed`；等待任务自有文件提交与 `git push origin int_main`。
