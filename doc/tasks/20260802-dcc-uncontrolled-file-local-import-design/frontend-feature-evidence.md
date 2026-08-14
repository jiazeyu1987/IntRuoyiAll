# Frontend Feature Evidence

## Feature

- Goal: 在现有 `系统管理 > NAS 管理` 的“统计未受控文件”弹窗中补齐前端静态契约和最小 UI/API 集成，使用户可在统计完成后选择是否将新的未受控文件下载到本地授权目录，并按后端识别快照归入 DCC 项目代码 / item / 文件分类；无法唯一识别的文件保持“未分类/待处理”。
- Non-goals: 不实现正式归档成功路径、不创建受控文件、不写 ACTIVE NAS 来源映射、不操作真实 NAS 或真实本地文件系统、不用 ZIP 或浏览器默认下载目录替代授权目录写入。
- Owned frontend files: `IntRuoyiFronted/src/views/system/nas/index.vue`, `IntRuoyiFronted/src/api/system/nas/index.ts`, `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts`, `IntRuoyiFronted/tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js`, `IntRuoyiFronted/package.json`.

## Acceptance

- A1: API wrapper exposes files page, recognize, import-selected, content binary download and local-write-result endpoints using backend snapshot fields `auditFileId/sourceSignature/expectedLocalRelativePath`.
- A2: The page requests `showDirectoryPicker` and validates backend-provided local relative paths before creating `import-selected`; unsupported browser, canceled directory picker, or invalid relative path does not create an import task.
- A3: The page writes each downloaded Blob through `getDirectoryHandle/getFileHandle/createWritable/write/close` and posts `LOCAL_WRITTEN` only after `close()` succeeds.
- A4: Local write failure posts `LOCAL_WRITE_FAILED` with explicit error code/message and does not mark the file as successful.
- A5: The page does not store or send local absolute paths, keeps `UNCLASSIFIED_PENDING/AMBIGUOUS` visible and selectable only for local `_未分类待处理` download/pending manual review, and surfaces `ARCHIVE_METADATA_REQUIRED` as “归档元数据待补齐”.

## BDD Scenarios

BDD: Browser directory authorization gates import-selected -> Given a completed NAS uncontrolled audit task and selected downloadable files When the user chooses to download to a local directory Then the page must obtain `showDirectoryPicker` authorization and validate every `expectedLocalRelativePath` before calling `/import-selected`.

BDD: Local write success is reported only after close -> Given an import-selected task and a selected audit-file snapshot When the content Blob is downloaded and the local writable stream closes successfully Then the page posts `LOCAL_WRITTEN` with the same source signature and local relative path snapshot.

BDD: Local write failure remains visible -> Given the local file write fails after content download When the writable stream or file handle throws Then the page posts `LOCAL_WRITE_FAILED` with `LOCAL_WRITE_FAILED` error code and displays the failure instead of treating the file as imported.

BDD: Unrecognized files remain pending -> Given audit rows are `UNCLASSIFIED_PENDING` or `AMBIGUOUS` When the user opens the completed audit task Then those rows remain visible as “未分类/待处理” or “待确认”, can be selected for local `_未分类待处理` download, and are not eligible for automatic DCC archive.

BDD: Archive metadata blocker is explicit -> Given a matched file reaches local write success but backend returns `ARCHIVE_METADATA_REQUIRED` When the page reloads audit-file rows Then the page displays “归档元数据待补齐” rather than claiming archive success.

## RED Evidence

RED: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> FAIL, expected reason: `package.json` did not expose `e2e:dcc:nas-uncontrolled-local-import:static`, and the NAS page/API lacked the local directory import contract.

## GREEN Evidence

GREEN: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> PASS.

GREEN: `pnpm e2e:dcc:nas-uncontrolled-local-import:static` -> PASS.

GREEN: `node tests/e2e/nas-control-audit-static.spec.js` -> PASS, existing statistics/report-download contract preserved.

GREEN: UTF-8/trailing whitespace check for frontend slice files -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.

GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts IntRuoyiFronted/src/api/system/nas/index.ts IntRuoyiFronted/src/views/system/nas/index.vue IntRuoyiFronted/tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> PASS, only Git LF-to-CRLF warnings.

GREEN: `pnpm ts:check` -> PASS after the unrelated DCC upload page computed exposure issue was fixed by moving `formData` and file-type-taxonomy computed declarations before their first use.

## Verification

- Static contract verifies API wrappers for `/files`, `/files/recognize`, `/import-selected`, `/content`, and `/local-write-result`.
- Static contract verifies request ordering: `showDirectoryPicker` and local relative path validation precede `importSelectedNasUncontrolledFiles`.
- Static contract verifies content download, local write, `writable.close()`, `LOCAL_WRITTEN`, and `LOCAL_WRITE_FAILED` sequencing.
- Static contract verifies path guards for backslashes, absolute paths, drive letters, `.`, and `..`.
- Static contract verifies no local absolute path fields are stored or sent by the new flow.
- Static contract verifies visible and selectable local-pending handling for `UNCLASSIFIED_PENDING/AMBIGUOUS`, plus visible `ARCHIVE_METADATA_REQUIRED`.
- `pnpm ts:check` was rerun after fixing the unrelated DCC upload page computed exposure issue and passed for the current frontend workspace.

## Blockers

- Real Playwright E2E remains pending because this slice is a static frontend contract and minimal UI/API integration only; real E2E requires running frontend/backend, login/tenant setup, browser directory picker automation, and traceable task-owned test data.
- Formal archive success remains blocked until M24 defines and verifies a formal archive metadata source for category, directory, file number, version, change type and effective date.
