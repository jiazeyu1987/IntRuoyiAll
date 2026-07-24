# Task: DCC 文控中心子页签乱码修复

## Goal

修复 `DCC文控中心` 下子页签中仍然显示乱码的用户可见文案，确保真实前端路径中相关页面、按钮、提示和隐藏路由标题都显示为可读的规范简体中文。

## Scope

- 先检查前一个同仓前端任务状态，并在开始本任务前显式暂停未完成旧任务。
- 先创建当前前端任务文档、执行日志与回归脚本，再开始生产代码修改。
- 仅修复本次实际存在乱码的前端文件：
  - `src/views/dcc/controlled-file/distribution/index.vue`
  - `src/views/dcc/controlled-file/shared/governance/CategoryDepartmentRulesSection.vue`
  - `src/router/modules/remaining.ts`
- 先补失败回归脚本，再做最小文案修复。
- 运行任务级回归脚本和聚焦前端校验。
- 不修改接口契约，不引入 fallback，不扩散到无乱码的其它页面。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260518-dcc-process-detail-stage-name-normalization/task.md`
- Status before this task: blocked / on hold due user priority switch.
- Impact: the previous same-repository frontend task was explicitly paused before this broader garbled-content fix started, so there is no unresolved task-order conflict.

## Milestones

- [x] M1: Pause the previous same-repository frontend task and create this task package.
- [x] M2: Add a failing regression script for the DCC document-center subtab garbled copy.
- [x] M3: Implement the minimal copy fix in the owned frontend files.
- [x] M4: Run targeted verification, update evidence, and prepare commit scope.

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-fix.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-doc-center-subtab-garbled-content-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-real-e2e.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`

## Current Status

Completed for code delivery and verification. The affected DCC document-center subtab surfaces now render readable Chinese again in the owned frontend files.

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-fix.mjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-doc-center-subtab-garbled-content-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260518-dcc-doc-center-subtab-garbled-content-fix\scripts\verify-dcc-doc-center-subtab-garbled-content-real-e2e.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS

## Blockers And Impact

- None currently.
