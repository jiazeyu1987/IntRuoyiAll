## Summary

一线生产默认全屏、普通页显示不完整、以及用户澄清的“工序选择/员工选择按 1920:1080 比例局部 grid 排布”已修正：进入生产填写页时 panel 不再使用 fixed/inset/z-index 默认覆盖全视口；右上按钮按一线 PQC 逻辑保持显式“最大化/主页”切换；普通页不再通过整块 1920×1080 stage 缩放来适配宽度，而是让生产 screen 进入正常响应式页面流，并将顶部工序、员工、最大化按钮组成局部 16:9 selection grid，避免右侧区域横向裁切。

## Passed

- `node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-production-prototype-parity-static.spec.cjs` -> PASS
- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS
- `node --check tests\e2e\edhr-frontline-production-fullscreen-toggle-real.e2e.cjs` -> PASS
- `pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260806-frontline-production-fullscreen-logic\bug-regression-evidence.md` -> PASS
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs doc/tasks/20260806-frontline-production-fullscreen-logic` -> PASS
- `node -e <task file trailing whitespace check>` -> PASS

## Closeout Blocked

- Implementation verification is PASS.
- Commit/push closeout remains blocked because the branch is behind `origin/int_main` and the workspace has many unrelated dirty/untracked files. This task did not stage, commit, push, or revert unrelated work.
- Latest user feedback on local 1920:1080 proportional grid is covered by `edhr-frontline-production-fullscreen-toggle-static.spec.cjs`, `edhr-frontline-production-pixel-parity-static.spec.cjs`, and the real E2E script syntax contract above.

## Files Touched

- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`
- `doc/tasks/20260806-frontline-production-fullscreen-logic/task.md`
- `doc/tasks/20260806-frontline-production-fullscreen-logic/execution-log.md`
- `doc/tasks/20260806-frontline-production-fullscreen-logic/verification-report.md`
- `doc/tasks/20260806-frontline-production-fullscreen-logic/bug-regression-evidence.md`
