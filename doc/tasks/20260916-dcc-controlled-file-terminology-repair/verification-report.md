# Verification Report

## Scope
DCC controlled-file terminology repair on current `int_qms`: remove user-visible uses of retired business labels `工作稿`, `工作版本`, `现行版`, `现行版本` from controlled-file production scan roots while preserving internal status semantics.

## Conclusion
PASS. The controlled-file production scan roots no longer expose the retired labels in user-visible backend/frontend code. The new terminology contract and directly affected stable DCC frontend contracts pass. Frontend type-check and local build pass.

## Changes Verified
- Backend missing-source message now describes a pending controlled file source instead of a draft source.
- Upload page labels and prompts now use `当前有效版本`, `创建受控文件`, and `受控文件已创建`.
- Browser/detail rework prompts now describe `待提交受控文件版本`.
- Shared detail/status presentation now uses `当前有效版本` and `待提交` instead of retired current/draft labels.
- Static contract added at `IntRuoyiFronted/scripts/dcc-controlled-file-terminology.test.mjs`.

## Verification Evidence
- `Push-Location IntRuoyiFronted; node --test scripts\dcc-controlled-file-terminology.test.mjs scripts\dcc-new-upload-version-entry.test.mjs scripts\dcc-checkin-upload-state.test.mjs scripts\dcc-controlled-browser-version-selector.test.mjs scripts\dcc-ordinary-removed-actions.test.mjs; Pop-Location` -> 13 pass, 0 fail.
- `Push-Location IntRuoyiFronted; pnpm ts:check; Pop-Location` -> PASS.
- `Push-Location IntRuoyiFronted; pnpm build:local; Pop-Location` -> PASS, with existing Vite CJS and Browserslist age warnings only.
- `rg -n "工作稿|工作版本|现行版|现行版本|master 主档|创建工作版本" IntRuoyiBackend\yudao-module-dcc\src\main\java IntRuoyiFronted\src\views\dcc\controlled-file IntRuoyiFronted\src\api\dcc\controlledFile --glob '!**/dcc-controlled-file-terminology.test.mjs'` -> no production hits.
- `git diff --check` -> PASS, with Git CRLF conversion warnings only.
- Cleanup tooling check -> no runnable `task-closeout-cleanup` found in project path or user `.codex`; direct keep review retained only `task.md`, `execution-log.md`, and `verification-report.md`.

## Boundary
No real-page Playwright E2E, database write, service restart, deployment, or remote-server operation was executed in this task. The prior DCC-MAIN-01..20 reverification remains closed separately in `doc/tasks/20260916-dcc-20-reverification/verification-report.md`.

## Non-gate Diagnostic
A historical preview-detail script was probed and found stale because it reads deleted `src/views/dcc/controlled-file/mine/index.vue` and asserts unrelated preview action buttons. It was not used as the terminology gate and no unrelated production behavior was changed for that script.
