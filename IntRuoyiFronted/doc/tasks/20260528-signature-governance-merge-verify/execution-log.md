# Execution Log：20260528-signature-governance-merge-verify

BDD: frontend main tenant verification -> Given 前端支线已合并进 `int_main` 且主前端已重启, When 使用 `芋道源码 / admin / admin123` 访问电子签名治理页, Then 页面必须加载最新前端代码并调用真实后端接口。

GREEN: frontend task document created -> PASS.

GREEN: git merge --no-ff --no-commit codex/20260528-signature-governance-docs (frontend int_main) -> PASS, automatic merge completed without conflicts and commit is intentionally held for runtime verification.

GREEN: `node scripts\signature-governance-page-contract.test.mjs` -> PASS, 3 tests.

GREEN: `node tests\e2e\signature-governance-e2e-static.spec.js` -> PASS.

RED: `node scripts\vite-dev-file-handle-contract.test.mjs` -> FAIL, expected reason: Vite AutoImport still generated tracked `src/types/auto-imports.d.ts` in dev mode, increasing Windows file handle churn.

GREEN: `node scripts\vite-dev-file-handle-contract.test.mjs` -> PASS after AutoImport d.ts generation was disabled for dev/build.

BLOCKER: frontend Vite dev server on `8081` -> FAIL, `EMFILE: too many open files`; impact: main tenant Playwright could not reliably use dev server.

GREEN: `pnpm build:local` with `VITE_BASE_URL=http://127.0.0.1:48081` -> PASS, production frontend artifact built from latest main worktree.

GREEN: Vite preview on `8081` -> PASS, login page returned HTTP 200.

GREEN: `SIGNATURE_GOVERNANCE_E2E_BASE_URL=http://127.0.0.1:8081 SIGNATURE_GOVERNANCE_E2E_TENANT=测试租户 SIGNATURE_GOVERNANCE_E2E_USERNAME=aoteman SIGNATURE_GOVERNANCE_E2E_PASSWORD=admin123 node tests\e2e\signature-governance-policy.e2e.js` -> PASS.

GREEN: Playwright main tenant verification with `芋道源码 / admin / admin123` -> PASS, page `/signature-governance` showed `READY`; policy API returned `READY`, `ready=true`, modules `DCC, EDHR, INTAUTH, SHOWROOM`, and no failed `admin-api` responses.

GREEN: `git worktree remove D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3` -> PASS, frontend支线 worktree 已删除。

GREEN: `Test-Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3` -> PASS, returned `False`; `git worktree list` no longer includes `20260528-signature-governance-docs`.

GREEN: `task-closeout-cleanup --mode preview` -> PASS, cleanup plan keeps `task.md` and `execution-log.md`, deletes only task-local `verification-report.md`, and reports no blockers.

GREEN: `task-closeout-cleanup --mode apply` -> PASS, deleted only `doc/tasks/20260528-signature-governance-merge-verify/verification-report.md`; task core records remain.
