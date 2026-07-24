# Task: 替换侧边栏品牌图标为指定蓝色 Logo

## Goal

将登录后左侧侧边栏头部品牌图标替换为用户提供的第一张蓝色 Logo，仅影响当前后台壳品牌区，不连带修改登录页、二维码或其他共用 `logo.png` 的入口。

## Scope

- 定位当前后台壳品牌区组件与现有图标引用。
- 新增侧边栏专用品牌素材，避免影响其他共用 `logo.png` 的页面。
- 仅更新 `src/layout/components/Logo/src/Logo.vue` 的展示引用。
- 补充一条聚焦侧边栏品牌图标引用的前端回归检查。
- 使用真实前端入口执行一次 Playwright 验证。

## Protected Scope

- 不修改任何 API、请求参数、响应结构、后端逻辑或菜单数据。
- 不修改登录页、二维码、AI 音频封面等其他仍引用 `src/assets/imgs/logo.png` 的页面。
- 不引入 fallback、占位图或静默降级。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: completed for code delivery; no unfinished frontend task blocks this scope.
- Impact: safe to proceed with this isolated branding change.

## Milestones

- [x] M1: Create this task package and execution log before production edits.
- [x] M2: Record BDD plus RED evidence for the current wrong sidebar brand icon.
- [x] M3: Add a sidebar-only brand asset and update the shell logo component.
- [x] M4: Run focused GREEN verification and a real-page Playwright check.
- [x] M5: Update task records, preview cleanup, and commit only scoped frontend files.
- [x] M6: Follow up on logo scaling so the full shell icon is visible.

## Expected Verification

- `node scripts/sidebar-brand-logo.test.mjs`
- `pnpm exec eslint src/layout/components/Logo/src/Logo.vue`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-logo run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-sidebar-brand-logo-replace\scripts\verify-sidebar-brand-logo.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-scale run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-sidebar-brand-logo-replace\scripts\verify-sidebar-brand-logo-scale.mjs`

## Cleanup Candidates

- `doc/tasks/20260516-sidebar-brand-logo-replace/scripts/`
- `doc/tasks/20260516-sidebar-brand-logo-replace/sidebar-brand-logo-scale-check.png`

## Current Status

Completed for implementation and verification. The sidebar shell now uses a sidebar-only blue brand icon with follow-up scale correction so the full icon is visible, shared `logo.png` entry points remain unchanged, and the live page verification on `http://127.0.0.1:8081/index` passed.

## Final Verification Result

- RED: reconstructed pre-change check against `HEAD:src/layout/components/Logo/src/Logo.vue` -> FAIL, the previous sidebar logo component still pointed at the shared `@/assets/imgs/logo.png` and did not reference `sidebar-brand-logo.svg`.
- GREEN: `node scripts/sidebar-brand-logo.test.mjs` -> PASS.
- GREEN: `pnpm exec eslint src/layout/components/Logo/src/Logo.vue scripts/sidebar-brand-logo.test.mjs` -> PASS.
- PRECONDITION: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, local frontend `8081` and backend `48081` both returned HTTP `200` afterward.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-logo open http://127.0.0.1:8081/login?redirect=%2Findex` -> PASS.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-logo run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-sidebar-brand-logo-replace\scripts\verify-sidebar-brand-logo.mjs` -> PASS, returned `{"url":"http://127.0.0.1:8081/index","logoText":"瑛泰管理系统","logoSrc":"http://127.0.0.1:8081/src/assets/imgs/sidebar-brand-logo.svg"}` and captured `doc/tasks/20260516-sidebar-brand-logo-replace/sidebar-brand-logo-shell.png`.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-sidebar-brand-logo-replace --mode preview` -> PASS, only the task-local Playwright script directory and screenshot were marked for deletion.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-sidebar-brand-logo-replace --mode apply` -> PASS, task-local Playwright script and screenshot were removed after evidence was recorded.
- FOLLOW-UP RED: `node scripts/sidebar-brand-logo.test.mjs` -> FAIL, `Logo.vue` still lacked `object-contain` and still forced a square shell logo box.
- FOLLOW-UP RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-scale run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-sidebar-brand-logo-replace\scripts\verify-sidebar-brand-logo-scale.mjs` -> FAIL, the live shell returned `logo_object_fit_expected_contain_actual_fill`.
- FOLLOW-UP GREEN: `node scripts/sidebar-brand-logo.test.mjs` -> PASS, the shell logo now uses `object-contain` and a width-greater-than-height render box.
- FOLLOW-UP GREEN: `pnpm exec eslint src/layout/components/Logo/src/Logo.vue scripts/sidebar-brand-logo.test.mjs` -> PASS.
- FOLLOW-UP GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session sidebar-brand-scale run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-sidebar-brand-logo-replace\scripts\verify-sidebar-brand-logo-scale.mjs` -> PASS, the live shell returned `{"clientWidth":50,"clientHeight":32,"objectFit":"contain","resolvedSrc":"http://127.0.0.1:8081/src/assets/imgs/sidebar-brand-logo.svg"}` and captured `doc/tasks/20260516-sidebar-brand-logo-replace/sidebar-brand-logo-scale-check.png`.
- FOLLOW-UP CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-sidebar-brand-logo-replace --mode preview` -> PASS, only the follow-up task-local Playwright script and screenshot were marked for deletion.
- FOLLOW-UP CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-sidebar-brand-logo-replace --mode apply` -> PASS, deleted the follow-up task-local Playwright script directory and screenshot after the verification result had been recorded.
