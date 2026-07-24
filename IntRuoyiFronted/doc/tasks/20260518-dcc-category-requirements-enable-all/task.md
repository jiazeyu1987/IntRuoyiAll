# Task: DCC 文件类别要求分发和要求培训全量开启

## Goal

修复 `DCC文件类别` 列表中“分发/培训”列与编辑弹窗真实布尔值不一致的问题，并把当前所有文件类别的 `要求分发`、`要求培训` 统一开启，确保前端展示与真实数据一致。

## Scope

- 先确认并显式暂停上一个同仓前端任务，再创建本任务包。
- 严格按 BDD + TDD 先补失败回归，再做最小前端修复。
- 修复 `DCC文件类别` 列表页“分发/培训”列的真实值展示，不再硬编码“必须”。
- 修复类别表单默认值与回显处理，使前端与后端布尔值语义一致。
- 通过真实前端入口 `http://localhost:8081` 验证列表与编辑弹窗显示一致。
- 不引入 mock、fallback 或额外测试专用前端控件。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused schedule-calendar frontend task remains isolated and does not block this DCC file-category requirement-alignment slice.

## Milestones

- [x] M1: Block the previous same-repository frontend task and create this task package first.
- [x] M2: Record BDD scenarios and add RED verification for list/edit requirement mismatch.
- [x] M3: Implement the minimal frontend list/form fix.
- [x] M4: Run targeted verification and real-page regression checks.
- [x] M5: Preview closeout artifacts and prepare the task-scoped frontend commit.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\scripts\verify-dcc-category-requirements-source.mjs`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-category-requirements-enable-all run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\scripts\verify-dcc-category-requirements-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\bug-regression-evidence.md`

## Current Status

Completed. Frontend list rendering, form defaults, real runtime data alignment, regression verification, and closeout preview are complete.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\scripts\verify-dcc-category-requirements-source.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-category-requirements-enable-all run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\scripts\verify-dcc-category-requirements-real-e2e.mjs` -> PASS, updated 47 historical categories and verified `图纸` edit dialog now shows both switches enabled
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-category-requirements-enable-all\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260518-dcc-category-requirements-enable-all --mode preview` -> PASS

## Blocker And Impact

- None.
