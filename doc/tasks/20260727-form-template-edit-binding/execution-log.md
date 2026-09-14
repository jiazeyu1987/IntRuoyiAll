# Execution Log

## User Intent

用户反馈：表单中心/表单模板列表中点击“编辑”按钮时报错“当前模板未绑定批记录表单，无法执行该操作”。

## Rule And Skill Intake

- 使用技能：`bug-regression-fix-loop`。
- 已读取：`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取：`bug-regression-fix-loop/references/bug-contract.md`。
- Git 预检：`git status --short --branch` 显示既有脏工作区，需先保护既有改动，再隔离当前任务实现。
- 经验门禁：命中前端静态契约隔离门禁、Windows 换行与脚本行为同步门禁、脏工作区基线门禁。
- 并行基线：`fc07fc8a chore: preserve dirty worktree baseline before edhr list fix`、`32df0a46 chore: preserve concurrent task baseline before edhr list fix`、`5b9abcd8 chore: capture pre-existing dirty worktree baseline` 已在本任务过程中保护并行改动；`5b9abcd8` 已包含本任务代码、测试和证据文件，后续不改写历史，仅提交经验沉淀与最终收尾记录。

## BDD

- BDD: 编辑已发布表单模板 -> Given 表单模板列表中存在已发布且可预览的模板 When 用户点击该模板的编辑操作 Then 前端应进入该模板编辑流程，而不是因误判缺少批记录表单绑定直接报错。
- BDD: 编辑缺少绑定的批记录模板 -> Given 批记录相关模板确实缺少记录表单绑定 When 用户点击需要绑定关系的操作 Then 页面应显示真实绑定缺失错误，不能静默成功或降级。

## TDD Evidence

- RED: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> FAIL, expected reason: 编辑按钮仍调用 `openSelectedTemplateDesigner('edit')`，触发批记录绑定校验。
- GREEN: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `node tests/e2e/form-center-static.spec.js` -> FAIL, unrelated pre-existing blocker: `Expected content to include: activeMenu: '/mdm/form-center/policy'`，当前任务改用聚焦静态合同隔离验证。

## Milestone Updates

- 2026-07-27: 建立任务目录与初始 BDD，准备处理既有脏工作区基线。
- 2026-07-27: 根因定位为预览区“编辑”误复用 `openSelectedTemplateDesigner('edit')`，导致普通 FormCenter 模板也先经过批记录绑定校验。
- 2026-07-27: 更新 `form-template-batch-record-button-alignment-static.spec.js`，先 RED 复现错误路径，再把编辑按钮改回 `openSelectedTemplateAction('edit')` 并 GREEN。
- 2026-07-27: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260727-form-template-edit-binding\bug-regression-evidence.md` -> PASS。
- 2026-07-27: `git diff --check -- <task files>` -> PASS，仅出现 CRLF 提示，无空白错误。
- 2026-07-27: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-form-template-edit-binding --mode preview` -> PASS，keep 仅包含 task/execution/verification/bug-regression evidence，无 delete/blocked/warnings。
- 2026-07-27: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-form-template-edit-binding --mode apply` -> PASS，无删除项。
- 2026-07-27: 项目经验沉淀已合并到 `docs/frontend-development.md#表单模板编辑与批记录绑定动作边界门禁`，并在 `docs/experience-index.md` 增加可命中关键词；`rg -n "当前模板未绑定批记录表单|表单模板编辑与批记录绑定动作边界门禁" docs/experience-index.md docs/frontend-development.md` -> PASS。

## Blockers

- 宽 FormCenter 静态合同存在既有无关失败：`activeMenu: '/mdm/form-center/policy'` 缺失，不属于本次编辑按钮修复范围。
