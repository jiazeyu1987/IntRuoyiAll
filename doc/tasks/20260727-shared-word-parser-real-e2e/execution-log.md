# Execution Log

## 2026-07-27

- User intent: 进行真实数据的 E2E 验证，Word 数据位于 `resource` 目录。
- BDD: 表单模板真实 Word 导入 -> Given `resource` 中存在真实 Word 文件 When 用户通过表单中心导入弹窗上传 Word Then 前端必须调用 `/form-center/templates/import-doc` 并展示导入结果或明确失败原因。
- BDD: 批记录真实 Word 导入 -> Given `resource` 中存在真实批记录 Word 文件 When 用户通过批记录报表导入入口上传 Word Then 前端必须调用 `/mes/pro/batch-record-report/recognize-uploaded` 或 `/upload-extra-slot` 并展示导入结果或明确失败原因。
- BDD: E2E 不允许降级 -> Given 页面、登录、数据或运行态前置条件缺失 When 验证无法继续 Then 任务必须记录 BLOCKER，不得用 mock、API-only、直接 SQL 写入或切换接口替代。
- Read: `playwright` skill, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/experience-index.md`, `docs/backend-development.md`.
- GREEN: experience-preflight -> PASS, matched `docs/backend-development.md#edhr-批记录-word-表格解析门禁` and `docs/e2e-rules.md#官方登录前置与-admin-only 全量验证门禁`.
- Resource inventory -> PASS, found `resource/批记录压力泵.doc`, `resource/损耗单.doc`, `resource/过程检验记录.docx`.
- Runtime preflight -> PASS, `http://127.0.0.1:8081/` returned HTTP 200 and `http://127.0.0.1:48081/actuator/health` returned HTTP 200.
- Playwright preflight -> PASS, `npx` available at `D:\Programs\npx.ps1`.
- Existing E2E review: `edhr-word-template-import-real-flow.e2e.js` can validate main batch-record Word import through the real page when `EDHR_WORD_IMPORT_SAMPLE_DOC` points to `resource/批记录压力泵.doc`.
- Existing E2E review: form-center template import has UI/API contract in `TemplateImportDialog.vue` and `template.ts`; no matching real import script was found, so it requires a task-owned browser validation path.
- Created task-owned Playwright script: `doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js`.
- GREEN: `node --check doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js` -> PASS.
- RED: initial task-owned E2E run -> FAIL fast, missing test password source in first script version; fixed by reusing the existing MES Word E2E test-account password source without logging secrets.
- RED: task-owned E2E run -> FAIL, first form-center route candidate `/approval-center/manager/form-center/template` returned 404 for `测试租户/aoteman`; script updated to probe `/mdm/form-center/template` and record candidate route evidence.
- RED: task-owned E2E run -> FAIL, form-center upload wait timed out because the script had not proved the Element Plus upload list received the file; script updated to assert visible uploaded file name and request trigger before waiting for response.
- GREEN: `node doc/tasks/20260727-shared-word-parser-real-e2e/shared-word-parser-real-e2e.js` -> PASS as a validation run with status `BLOCKED` for MES business state; evidence written to `real-e2e-evidence.json`.
- Form-center real E2E -> PASS, tenant/user `测试租户/aoteman`, route `/mdm/form-center/template`, file `resource/过程检验记录.docx`, endpoint `/admin-api/form-center/templates/import-doc`, response `templateId=30`, `versionNo=V1.0`, `recognizedFields=56`, screenshot `artifacts/form-center-20260727-shared-word-parser-real-e2e.png`.
- MES batch-record real E2E -> BLOCKED, tenant/user `测试租户/aoteman`, route `/mes/pro/batch-record-form-list`, file `resource/批记录压力泵.doc`, endpoint `/admin-api/mes/pro/batch-record-report/recognize-uploaded/preflight`, `allowedActions=[]`, confirm disabled, latest version `V3.0/PENDING_APPROVAL`, current version `V1.0`, next version `V4.0`, screenshot `artifacts/mes-preflight-20260727-shared-word-parser-real-e2e.png`.
- Experience consolidation -> updated existing `docs/e2e-rules.md` with Element Plus upload control gate; no new long-term experience document created.
- GREEN: evidence JSON parse -> PASS, status `BLOCKED`, results `form-center:PASS,mes-batch-record:BLOCKED`.
- GREEN: artifact existence check -> PASS, verified form-center screenshot, MES preflight screenshot, and `verification-report.md` exist.
- GREEN: `git diff --check -- docs/e2e-rules.md` -> PASS with line-ending warning only (`LF will be replaced by CRLF the next time Git touches it`).
- Closeout note: task remains `blocked` because MES import cannot be completed until the pending batch-record version is resolved. No cleanup/apply, commit, or push was performed because verification is not fully passable and the shared worktree contains unrelated dirty/ahead state.
