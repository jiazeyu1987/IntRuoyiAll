# Execution Log: DCC 上传审批落盘真实 E2E

BDD: real upload reaches a persisted active controlled file -> Given a real DCC PDF, a live uploadable category, and the real frontend path / When the user uploads the file, previews the fixed route, submits it, and completes all live approval stages / Then the controlled file must reach a published `ACTIVE` state instead of stopping at a pending or finalizing status.

BDD: final persistence keeps published file metadata and bytes readable -> Given the same controlled file has finished the live approval flow / When the E2E reads the final controlled-file detail plus the published infra-file metadata and content / Then `publishedFileId`, published timestamps, and readable PDF bytes must all exist.

BDD: missing runtime prerequisites fail fast -> Given the runtime may miss file storage, categories, positions, directory bindings, matrix data, user assignments, or BPM definitions / When the real E2E runs / Then it must stop with the exact blocker instead of pretending persistence succeeded.

- M1: Completed. Previous frontend task `20260516-dcc-position-hide-combined-role` was explicitly blocked by user reprioritization before this task started.
- RED: pre-task coverage gap -> FAIL, the repository had no real browser E2E that asserted upload -> approval -> persisted published-file metadata and readable file bytes.
- M2: Completed. The BDD scenarios and initial RED evidence were recorded before the new script was added.
- M3: Completed. Added `doc/tasks/20260516-dcc-upload-approval-persistence-real-e2e/scripts/verify-dcc-upload-approval-persistence-real-e2e.mjs`.
- RED: initial `playwright-cli run-code` attempt -> FAIL for tooling/session setup, because the named Playwright session had to be opened first.
- RED: same real script after opening the browser session -> FAIL, local backend `48081` was unavailable until `mvn --% -pl yudao-server -am -DskipTests package` rebuilt the executable jar and a fresh runtime came online.
- RED: same real script after backend restore -> FAIL, the login step needed to follow the current tenant preset `瑛泰源码` and clear stale browser storage before re-entering the real login path.
- RED: authenticated DCC category discovery -> FAIL until tenant-1 file categories were imported from live IntAuth.
- RED: rule-page navigation -> FAIL until hidden direct routes for `/dcc/controlled-file/distribution` and `/dcc/controlled-file/training` were added to the frontend router.
- RED: `upload-preview` -> FAIL until the master file config was switched from the broken sample Qiniu S3 config to the DB-backed local file store.
- RED: `提交审批` -> FAIL until category `产品技术要求` was bound to a real directory.
- RED: route preview -> FAIL until the category matrix and required DCC position assignments were restored for the live category.
- RED: approval API -> FAIL until a fresh `yudao-server` package was rebuilt so the signature enum class existed in the runtime jar.
- RED: stage-advance detection -> FAIL because the browser detail page did not refresh in-place after a successful approval action; the script was then tightened to trust a successful `approve-task` response and reopen the task list for the next stage.
- GREEN: live runtime data repair -> PASS after the following real prerequisite fixes were applied:
  - `IntAuth` backend started on `http://127.0.0.1:8020`
  - DCC file categories imported into tenant `1`
  - DCC approval positions imported into tenant `1`
  - fixed local positions `900333 / 900334` restored as `部门负责人 / 部门授权代表`
  - category `INTAUTH-1 / 产品技术要求` bound to directory `3.DMR`
  - category `INTAUTH-1` permission rules granted to real user `admin`
  - category `INTAUTH-1` four-stage matrix saved with live position ids
  - referenced live positions assigned to real user `admin`
  - `infra_file_config.id=4` switched to master so uploads use the DB file store instead of the failing sample S3 config
  - `yudao-server` rebuilt and restarted from the latest local jar
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-approval-persistence-e2e-green3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\scripts\verify-dcc-upload-approval-persistence-real-e2e.mjs` -> PASS, controlled file `11` progressed through `文控审核 -> 审核会签 -> 批准 -> 文控批准`, reached final API status `ACTIVE`, exposed `publishedFileId=2217`, and the published file download remained a readable PDF.
