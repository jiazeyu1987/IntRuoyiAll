# Execution Log

## 2026-09-16 kickoff
- Read required rules: task-closeout-rules, DCC 20-item handoff, backend/frontend development rules and PowerShell encoding rules.
- Branch/status: `int_qms`, tracking `origin/int_qms`, clean before this terminology repair.
- Static scan found user-visible DCC occurrences of forbidden terms: backend `工作稿`; frontend `工作版本`; frontend `现行版/现行版本`.

## 2026-09-16 implementation
- Replaced user-visible retired labels in DCC controlled-file backend query message and frontend upload/browser/detail/shared presentation code.
- Added `IntRuoyiFronted/scripts/dcc-controlled-file-terminology.test.mjs` to scan production controlled-file roots and lock the replacement wording.
- RED diagnostic: a historical preview-detail script failed because it reads deleted `src/views/dcc/controlled-file/mine/index.vue` and asserts unrelated preview action buttons. The temporary test edit was reverted, and that stale script was excluded from this terminology gate.
- GREEN: `Push-Location IntRuoyiFronted; node --test scripts\dcc-controlled-file-terminology.test.mjs scripts\dcc-new-upload-version-entry.test.mjs scripts\dcc-checkin-upload-state.test.mjs scripts\dcc-controlled-browser-version-selector.test.mjs scripts\dcc-ordinary-removed-actions.test.mjs; Pop-Location` -> 13 pass, 0 fail.
- GREEN: `Push-Location IntRuoyiFronted; pnpm ts:check; Pop-Location` -> PASS.
- GREEN: `Push-Location IntRuoyiFronted; pnpm build:local; Pop-Location` -> PASS, with existing Vite CJS and Browserslist age warnings only.
- GREEN: `rg -n "工作稿|工作版本|现行版|现行版本|master 主档|创建工作版本" IntRuoyiBackend\yudao-module-dcc\src\main\java IntRuoyiFronted\src\views\dcc\controlled-file IntRuoyiFronted\src\api\dcc\controlledFile --glob '!**/dcc-controlled-file-terminology.test.mjs'` -> no production hits.
- GREEN: `git diff --check` -> PASS, with Git CRLF conversion warnings only.
- Closeout rule reread completed before commit/summary.
- Cleanup tooling check: project and user `.codex` search plus `Get-Command task-closeout-cleanup*` found no runnable `task-closeout-cleanup`; no generated temp evidence exists under this task directory, so cleanup was handled by direct keep review only.
- Status set to `ready_for_closeout`.
