# Execution Log: DCC 受控预览受控章真实 E2E

BDD: controlled preview stamp appears after real upload and approval -> Given a
real DCC PDF is uploaded and passes the live four-stage approval path, When the
user opens the controlled-file preview from the detail page, Then the protected
preview canvas must contain a visible red `受控` stamp.

BDD: controlled preview verification uses the real frontend path -> Given the
frontend exposes upload, approval-task, detail, and viewer pages, When the E2E
script validates the stamp, Then it must drive the real pages instead of using
API shortcuts to simulate the preview.

BDD: missing runtime prerequisites fail fast -> Given the runtime lacks a real
PDF, approvable route, or a previewable final controlled file, When the E2E
script runs, Then it must stop with the exact blocker instead of pretending the
preview stamp passed.

- M1: Completed. The blocked `20260516-dcc-routes-switch-auto-query` task stays
  paused, and the prerequisite implementation task
  `20260516-dcc-controlled-preview-stamp` was already completed before this E2E
  task started.
- RED: pre-task coverage gap -> FAIL, the repository had no real browser E2E
  that reached the protected preview canvas and checked the `受控` stamp.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp-real-e2e\scripts\verify-dcc-controlled-preview-stamp-real-e2e.mjs` -> FAIL, the live runtime rejected finalization for category `产品技术要求` with `Missing required distribution departments`.
- RED: runtime inspection through the same real frontend paths -> FAIL for fixture availability, because `DCC 我的文件` had no `现行` previewable row for `admin`, and `DCC 文件类别` showed only one bound uploadable category, `产品技术要求`, which requires both distribution and training.
- GREEN: the same E2E script reached a real previewable controlled file after the live prerequisite repair, backend final-status fix, PDF.js worker fix, and red-stamp rendering correction.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp-real-e2e\scripts\verify-dcc-controlled-preview-stamp-real-e2e.mjs` -> PASS, controlled file `2054545668044042268` reached `现行`, the preview page rendered the protected canvas, and the top-right sample contained `1962` red stamp pixels.
