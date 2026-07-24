BDD: Controlled preview shows a visible controlled stamp -> Given a user opens a
protected DCC PDF preview, When the viewer renders a page canvas, Then the
rendered preview must overlay a visible `受控` stamp so the page is visually
distinguishable from the original file.

BDD: Controlled stamp is applied during page rendering -> Given the protected
viewer renders one or more PDF pages, When each page render completes, Then the
viewer must invoke the shared controlled-stamp drawing logic for the page
canvas instead of relying on the backend binary to already contain the mark.

RED: `node doc/tasks/20260516-dcc-controlled-preview-stamp/scripts/verify-dcc-controlled-preview-stamp.cjs` -> FAIL, the protected preview had no shared `受控` stamp text contract or post-render stamp invocation.

GREEN: `node doc/tasks/20260516-dcc-controlled-preview-stamp/scripts/verify-dcc-controlled-preview-stamp.cjs` -> PASS.

GREEN: `pnpm exec eslint src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/presentation.ts` -> PASS.

GREEN: `python C:/Users/BJB110/.codex/skills/bug-regression-fix-loop/scripts/validate_bug_regression.py --evidence D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/doc/tasks/20260516-dcc-controlled-preview-stamp/bug-regression-evidence.md` -> PASS.

GREEN: `python C:/Users/BJB110/.codex/skills/frontend-feature-delivery/scripts/validate_frontend_feature.py --evidence D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/doc/tasks/20260516-dcc-controlled-preview-stamp/frontend-feature-evidence.md` -> PASS.
