# Verification Report

## Summary

- Direct controlled-file downloads now only remain in the受控浏览列表 row action entry.
- Detail page and viewer-mode toolbars no longer render direct controlled-copy download buttons.

## Passing Evidence

- `node tests/e2e/dcc-download-entry-browser-only-static.spec.js` -> PASS.
- `pnpm e2e:dcc:download-entry:static` -> PASS.
- `node tests/e2e/dcc-list-detail-entry-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS on clean rerun, session `41503` exited code 0.
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-dcc-download-entry-browser-only/frontend-feature-evidence.md` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS with LF/CRLF normalization warnings only.
- `rg -n -F -e '下载当前受控副本' -e '下载受控文件' -e '@click="openDownload"' -e 'downloadLoading' -e 'triggerControlledFileDownload' IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` -> no matches.
- `task_closeout.py --task-id 20260803-dcc-download-entry-browser-only --mode preview/apply` -> PASS; temporary frontend feature evidence was deleted after its PASS result was copied into this report.
- Frontend code commit: `72712e92d chore: baseline concurrent download entry updates`.
- Closeout docs commit: `fa4ba2225 docs: close DCC download entry task`.
- Push preflight large-object scan -> PASS, no blob over 100 MB in `origin/int_main..HEAD`.

## Regression Notes

- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> FAIL on existing assertion that browser operation column must not route to detail; current browser operation column intentionally contains “追溯”.
- `dcc-browser-file-number-detail-entry-static.spec.js` remains isolated to an older assertion about the browser operation-column detail route and is not introduced by the detail-page download removal.
- Push partially succeeded: code and prior closeout records reached remote through `57d1345d1`. Final completion commit `baad4eb0f` remains local because the final push retried with proxy cleared but failed with GitHub 443 reset/unreachable. Local `int_main` remains ahead of `origin/int_main` by 1 commit until network recovers.
