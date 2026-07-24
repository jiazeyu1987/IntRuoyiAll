# Task: DCC 上传路线第三层岗位名称显示

## Goal

修复 `DCC受控上传` 页的审批路线预览，让第三层批准岗位 `900335 / 900336`
显示真实岗位名称，而不是回退成 `岗位#900335 / 岗位#900336`。

## Scope

- 仅修改前端仓库中的 DCC 路线相关岗位名称显示逻辑。
- 修复 `DCC受控上传` 页第三层路线预览的岗位名称回退问题。
- 同步补齐共享固定本地岗位名称解析，避免同类路线预览继续泄露内部编号。
- 不改后端接口契约，不改 live 审批矩阵内容。
- 不在本任务中处理登录按钮或岗位分配维护逻辑。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-login-xbutton-click-fix/task.md`
- Status before this task: blocked.
- Impact: the shared login-button task is paused separately, so this task can
  focus only on the DCC upload-route display regression.

## Milestones

- [x] M1: Create this task package before production-code edits.
- [x] M2: Record BDD scenarios and capture RED evidence for the numeric fallback display.
- [x] M3: Implement the minimal upload-route position-name lookup fix.
- [x] M4: Run targeted verification and update evidence.
- [x] M5: Commit only task-scoped files if verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-route-position-name-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\scripts\verify-dcc-upload-route-position-name-display.mjs`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\bug-regression-evidence.md`

## Current Status

Completed.

## Blocker And Impact

- Blocker: none for code delivery.
- Impact: the upload-page third-stage preview now shows readable岗位名称 for
  `900335 / 900336`, and shared fixed local-position fallback names cover this
  restored approval-matrix pair.

## Final Verification Result

- RED:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-route-position-name-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\scripts\verify-dcc-upload-route-position-name-display.mjs` -> FAIL,
    stage 3 showed `岗位#900335 / 岗位#900336`.
- GREEN:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-route-position-name-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\scripts\verify-dcc-upload-route-position-name-display.mjs` -> PASS
  - `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\bug-regression-evidence.md` -> PASS
