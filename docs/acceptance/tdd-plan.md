# DCC 未受控文件本地下载与自动归类 TDD 计划

## Purpose and Scope

本计划将未受控文件处理设计拆成严格 RED -> GREEN 的实现序列。生产行为必须先有失败测试，再做最小实现，最后运行相邻回归。范围覆盖后端 schema、服务、Controller、前端静态合同、真实 E2E 和不允许 fallback 的错误路径。无法唯一识别项目代码、item 或分类的文件必须作为正式 `未分类/待处理` 业务状态验证，不得用默认项目、默认分类或静默成功替代。

## Evidence Reviewed

- `DccNasControlAuditControllerTest`：已有 audit start/get/download controller 合同入口。
- `DccControlledFileNasTransferServiceTest`：已有 NAS 转移、目录复用、分类绑定、来源映射和本地文件夹导入测试结构。
- `DccProjectCodeServiceImplTest`：已有项目代码关联文件分类、分类规则和未分类状态测试结构。
- `DccControlledFileProjectCodeRecognitionServiceTest`：已有项目代码识别链路测试结构。
- `nas-control-audit-static.spec.js`：已有 NAS 统计按钮和报告下载静态合同。
- `system-nas-management.test.mjs`：已有 NAS 管理页、本地文件夹导入和转移弹框静态合同。
- `dcc-project-code-associated-unclassified-auto-classify-static.spec.js`：已有项目代码详情未分类按文件名归类合同。
- `dcc-project-code-list-unclassified-auto-classify-static.spec.js`：已有列表页全分页处理未分类合同。

## TDD Sequence

### Command Working Directory

- 以下命令默认从仓库根目录 `E:\IntRuoyi` 执行。
- 后端 Maven 命令必须使用 `-f IntRuoyiBackend/pom.xml`，避免从根目录误找 `pom.xml`。
- 后端 SQL 静态测试必须放在 `IntRuoyiBackend/script/tests/`，与现有 DCC SQL 验证结构一致。
- 前端命令必须使用 `pnpm --dir IntRuoyiFronted ...`，新增脚本应同步写入 `IntRuoyiFronted/package.json`。
- 检索命令必须限定到 `IntRuoyiBackend/yudao-module-dcc`、`IntRuoyiBackend/sql/mysql`、`IntRuoyiBackend/script/tests`、`IntRuoyiFronted/src`、`IntRuoyiFronted/tests/e2e` 和本任务文档，避免无关损坏 `target` 目录阻塞。

### M0 开发前置门禁

1. RED：新增静态/文档门禁测试，检索是否存在独立 DCC item 模型；若发现正式 item 表或服务，当前 `dcc_project_code.id` 映射必须失败并要求更新设计。
2. RED：新增环境前置检查，断言浏览器目录写入能力、DCC 分类树、项目代码和 `dcc_file_category_match_rule` 数据前置被显式记录。
3. GREEN：补齐实现前置脚本或测试 fixture，使缺项时输出 `BLOCKER` 而不是跳过或默认成功。
4. REGRESSION：确认文档设计任务不修改生产代码、不操作真实 NAS、不写数据库。

### M1 Schema 与明细持久化

1. RED：新增 schema 测试断言 `dcc_nas_control_audit_file` 存在、字段完整、`source_signature`、`classification_status=PENDING_RECOGNITION` 初始态、`download_status`、`archive_status`、错误码字段、path hash 索引、task 外键索引和 tenant 范围索引存在。
2. GREEN：新增非破坏性 migration 和 test schema，保留现有 `dcc_nas_control_audit_task`，并避免用唯一约束阻止同一 NAS 路径跨 audit task 重复生成证据。
3. REGRESSION：运行 `DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails` 和 `IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py`。

### M2 Audit 扫描写明细

1. RED：新增 `DccNasControlAuditServiceImplTest`，模拟扫描包含已受控、未受控、待确认和跳过目录，断言未受控明细被持久化并可分页查询。
2. RED：新增 `DccNasControlAuditControllerTest` 合同，断言 `GET /dcc/controlled-files/nas-control-audit/{taskId}/files` 存在、权限为 `dcc:controlled-file:query`、返回 `PageResult`，并支持状态/关键词过滤。
3. GREEN：在 `DccNasControlAuditServiceImpl.handleScannedFile` 中写入明细，同时保持 Excel 报告输出，并用 `sha256(path_hash|file_size|modified_at_utc_epoch_millis)` 生成 `source_signature`。
4. GREEN：新增明细分页 VO、Service 和 Mapper 查询，按当前租户和 `taskId` 限定，不返回文件内容、本地绝对路径或其它租户数据。
5. REGRESSION：运行 `DccNasControlAuditControllerTest`，确认 start/get/download 合同不回退。

### M3 确定性预识别

1. RED：新增 schema/VO 合同测试 `mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot`，断言 audit 明细或等价快照具备 `classification_candidates_json`、稳定 `classification_reason`、`expectedLocalRelativePath` 响应字段；若缺字段，先追加 migration、DO、Mapper 和 test schema。
2. RED：新增后端服务测试 `recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique`。
3. RED：新增失败路径测试 `recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing`，断言项目缺失、分类缺失分别写入稳定原因码，且不会补默认项目或默认分类。
4. RED：新增歧义路径测试 `recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates`，断言候选摘要被持久化且不得按排序取第一。
5. RED：新增 `recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots`，断言普通 recognize 只更新 `PENDING_RECOGNITION` 明细。
6. GREEN：新增识别服务，复用项目代码启用列表、别名/规则链路和 DCC 文件分类树/规则表，输出 `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`，并持久化识别原因、候选摘要和后端生成的期望本地相对路径。
7. REGRESSION：运行 `DccProjectCodeServiceImplTest` 和 `DccControlledFileProjectCodeRecognitionServiceTest` 相邻用例。

### M4 选中文件处理任务

1. RED：新增 schema/DO 合同 `mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots`，断言 transfer task 具备 `audit_task_id`、`idempotency_key`、`request_hash`，task item 具备 `audit_file_id`、`source_signature`、识别快照、本地相对路径、本地写入/归档状态字段，audit 明细或等价关联可查询当前 import task/item 绑定。
2. RED：扩展同一 schema/SQL 合同，断言 `dcc_controlled_file_nas_transfer_task.template_category_id` 与 `effective_date` 在 migration 和 test schema 中允许 `NULL`，确保 `NAS_UNCONTROLLED_IMPORT` 不需要伪造旧 NAS 转移输入；同时补服务测试保证旧 `NAS` / `LOCAL_FOLDER` 创建入口仍要求其正式必填字段。
3. RED：新增 `DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_requiresCompletedAuditSelectedFileIdsAndLocalRelativePaths`。
4. RED：新增 `createUncontrolledImportTask_snapshotsRecognitionSourceSignatureAndLocalRelativePath`，断言处理项锁定 `auditFileId`、`source_signature`、识别结果快照、`local_relative_path` 和 `idempotencyKey`。
5. RED：新增 `createUncontrolledImportTask_rejectsPendingRecognitionDuplicateIdsAndSignatureMismatch`，断言未识别、重复 id、跨 task/tenant、签名不匹配或已绑定任务均不能创建 import task。
6. RED：新增 `createUncontrolledImportTask_rejectsInvalidSelectionAtomically`，断言同一请求中任一选中项无效时整个请求失败，不创建任务头/任务项，不把任何 audit 明细推进到 `SELECTED`。
7. RED：新增 `createUncontrolledImportTask_usesCanonicalRequestHashIndependentOfSelectionOrder`，断言相同 `idempotencyKey`、相同选中文件但不同 `selectedFiles` 顺序返回原任务，重复 id 必须在 hash 前失败。
8. RED：新增 `createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs`，断言请求 VO 不接受也不需要任务级 `templateCategoryId`、`effectiveDate`、任务级项目代码或任务级分类；创建出的 import task 这些旧字段为空，item 快照才是事实来源。
9. RED：新增 `processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten`，断言现有 `processWaitingTasks()` 或旧文件处理器遇到 `sourceType=NAS_UNCONTROLLED_IMPORT` 时不读取 NAS、不调用 `submitControlledFileWithoutApproval`、不写 ACTIVE 来源映射。
10. RED：新增 `createUncontrolledImportTask_requiresUniqueIdempotencyOrTransactionalLock`，断言同一操作者同一 `idempotencyKey` 的并发请求只产生一个任务；同 key 不同 hash 必须冲突，不能因普通索引或前端防抖产生双任务。
11. RED：新增 `processUncontrolledMatchedFile_waitsForLocalWrittenBeforeArchive`，断言 `MATCHED` 文件在 `LOCAL_WRITTEN` 之前不调用 `submitControlledFileWithoutApproval`。
12. RED：新增 `processUncontrolledPendingFile_doesNotCreateControlledFile`，断言待处理文件不调用 submit。
13. RED：新增 `createUncontrolledImportTask_reusesSameIdempotencyKeyAndRejectsArchivedAuditFileWithDifferentKey`，断言重复 key 返回原任务，已归档 audit file 不会因不同 key 创建第二个受控文件。
14. RED：新增 `createUncontrolledImportTask_concurrentDifferentKeysOnlyOneArchiveAllowed`，断言同一 `auditFileId` 并发提交时事务内只能创建一个可处理绑定，另一个返回冲突或已处理。
15. RED：新增 `createUncontrolledImportTask_rejectsSameIdempotencyKeyWithDifferentRequestHash`，断言同 key 不同 `auditFileId/sourceSignature/localRelativePath` 返回明确冲突。
16. RED：新增 `createUncontrolledImportTask_rejectsClientLocalRelativePathMismatch`，断言后端重新生成期望相对路径并拒绝被篡改的项目、分类、待处理前缀或本地绝对路径。
17. RED：新增 `createUncontrolledImportTask_requiresExplicitIdsSelectionScope`，断言首版只接受 `selectionScope=EXPLICIT_IDS`，不得把分页筛选条件隐式扩大为全量处理。
18. GREEN：扩展 `sourceType=NAS_UNCONTROLLED_IMPORT`，处理项绑定 `auditFileId`、识别快照、本地写入状态、错误码、归档状态、幂等键和规范化 `request_hash`，并在事务内检查状态流转。
19. GREEN：保留旧 `NAS` / `LOCAL_FOLDER` 入口的服务层必填校验，仅让 `NAS_UNCONTROLLED_IMPORT` 在 schema 和服务创建路径上不要求旧任务头目标字段。
20. REGRESSION：运行 `DccControlledFileNasTransferServiceTest` 全类。

### M5 本地下载确认

1. RED：新增 Controller 测试断言 content API 校验 `importTaskId`、audit file、path hash、size、modified time 和 `source_signature`。
2. RED：新增 local-write-result Controller 测试断言只接收相对路径和结果状态，不接收本地绝对路径。
3. RED：新增 `contentEndpointRejectsCrossTaskTenantAndArchivedAuditFile`，断言跨任务、跨租户、猜测 auditFileId、已归档或未绑定 import task 均不返回文件内容。
4. RED：新增 `localWriteResultRequiresSnapshotMatchedLocalRelativePath`，断言回写必须匹配处理项快照，`LOCAL_WRITE_FAILED` 必须有错误码，`LOCAL_WRITTEN` 不得携带错误码或本地绝对路径。
5. RED：新增 `localWriteFailed_doesNotArchiveMatchedFile`，断言本地写入失败时不创建受控文件或 NAS 来源映射。
6. RED：新增 `localWriteSucceeded_archiveFailureKeepsStatusesSeparated`，断言本地写入成功但归档失败时 `downloadStatus=LOCAL_WRITTEN` 且 `archiveStatus=FAILED`。
7. RED：新增 `localRelativePathRejectsTraversalReservedNamesAndCollisions`，断言盘符、`..`、非法字符、Windows 保留名和规范化冲突均写入 `downloadStatus=LOCAL_WRITE_FAILED` 与具体 `localWriteErrorCode`。
8. RED：新增 `contentEndpointReturnsBinaryOrChunkedPayloadNotJsonBase64`，断言 content API 使用二进制响应、流式读取或明确分块协议，禁止 JSON/base64 承载文件内容。
9. RED：新增 `localRelativePathRejectsExistingTargetAndPathTooLong`，断言本地目标已存在或路径过长时写入 `LOCAL_PATH_COLLISION` 或 `LOCAL_PATH_TOO_LONG`，不覆盖、不截断、不自动改名。
10. RED：新增 `localPrecheckFailureBeforeImportTaskDoesNotMutateBackendState`，断言目录授权后、`import-selected` 前发现目标存在、路径过长或规范化冲突时无 import task、无 content、无 local-write-result、audit 状态不变。
11. RED：新增 `localWriteResultIsIdempotentAndDoesNotArchiveTwice`，断言相同处理项重复 `LOCAL_WRITTEN` 只返回既有状态，不重复创建受控文件或 ACTIVE NAS 来源映射。
12. RED：新增 `localWriteResultRejectsConflictingResultAfterTerminalState`，断言成功后提交失败或失败后提交成功返回明确冲突，除非另有显式 retry 入口。
13. RED：新增 `archiveAfterLocalWritten_requiresFormalArchiveMetadata`，断言正式 DCC 归档所需模板分类、生效日期、变更原因或等价元数据缺少正式来源时返回 `ARCHIVE_METADATA_REQUIRED` 或明确失败状态，不得使用当前日期、旧 transfer 任务值、空模板或默认模板继续归档。
14. RED：新增 `archiveAfterLocalWritten_archivesOnlyFromFormalMetadataSnapshot`，断言只有处理项级正式归档元数据快照存在时才允许读取 NAS 原件、上传原始文件、提交 workflow、创建受控文件和写 ACTIVE NAS 来源映射；若只存在 `matchedProjectCodeId/matchedFileTypeTaxonomyId/classificationCandidatesJson`，测试必须失败。
15. RED：新增 schema/VO 合同断言正式归档快照至少包含 `categoryId`、`directoryId`、`dccProjectCodeId`、`fileTypeTaxonomyId`、`changeType`、`fileName`、`fileNumber`、`versionNo`、`effectiveDate`、`remark/source`，并与 `auditFileId + sourceSignature + localRelativePath` 绑定。
16. GREEN：实现内容下载、本地写入结果回写、后置归档触发和路径校验 API。
17. GREEN：先把正式归档元数据来源建模为明确入参、配置或处理项快照；缺失时只记录可见阻塞，不创建 DCC 受控文件。
18. REGRESSION：运行 `DccControlledFileControllerTest` 或新增专用 Controller 测试。

### M6 前端静态合同

1. RED：新增 `tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js`，断言 NAS 页面包含未受控明细列表、选择、识别预览、先选择本地目录再创建 import task、`showDirectoryPicker` fail-fast、处理进度和待处理展示。
2. RED：新增 API wrapper 合同，断言新增 files、recognize、import-selected、content、local-write-result 接口存在。
3. RED：新增本地相对路径静态合同，断言安全目录段、目标文件已存在、路径过长和规范化冲突均阻塞，不调用 `import-selected`。
4. RED：新增取消目录选择和浏览器不支持路径合同，断言无 import task、无 content、无 local-write-result、无 DCC 写请求。
5. RED：新增显式选择范围静态合同，断言页面只把已勾选行的 `auditFileId` 发送给后端，分页场景文案显示当前选择数量，不声明“已处理全部筛选结果”。
6. GREEN：最小改造 `system/nas/index.vue`、`src/api/system/nas/index.ts` 和 `package.json` 新增 `e2e:dcc:nas-uncontrolled-local-import:static` 脚本。
7. REGRESSION：运行 `pnpm --dir IntRuoyiFronted e2e:dcc:nas-uncontrolled-local-import:static`、`node IntRuoyiFronted/tests/e2e/nas-control-audit-static.spec.js`、`node IntRuoyiFronted/scripts/system-nas-management.test.mjs`。

### M7 真实页面 E2E

1. RED：新增 `tests/e2e/dcc-nas-uncontrolled-local-import-real.e2e.js --check`，先证明入口、脚本、运行库 schema、浏览器 File System Access API 和目标样本存在。
2. GREEN：本地测试库迁移与任务自有 audit fixture 准备完成后，`DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID=<id>` 的 `real:check` 必须 PASS；未传样本 ID 时仍 fail fast。
3. RED：完整真实页面 E2E 在未授权创建/确认任务自有 NAS 源文件时必须 BLOCKED，不得把前置 gate PASS、DB fixture、API-only 或静态合同写成“已下载到本地对应目录”。
4. GREEN：获得共享 NAS 测试目录授权并确认源文件字节存在后，用真实页面选择本地目录，分别选择 `MATCHED` 与 `UNCLASSIFIED_PENDING/AMBIGUOUS` 文件，下载 content、写入本地目录、回写 `LOCAL_WRITTEN`；其中 `MATCHED` 才可进入正式归档，`UNCLASSIFIED_PENDING/AMBIGUOUS` 必须写入 `_未分类待处理` 并保持 `PENDING_MANUAL_REVIEW`。
5. REGRESSION：复验项目代码详情三栏、受控文件详情、NAS 来源映射和任务自有 DB/NAS/local 样本清理。

## RED Commands

- `node scripts/preflight/dcc-uncontrolled-import-design-preflight.mjs`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#processUncontrolledMatchedFile_waitsForLocalWrittenBeforeArchive" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#processUncontrolledPendingFile_doesNotCreateControlledFile" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#localWriteFailed_doesNotArchiveMatchedFile" test`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q`
- `pnpm --dir IntRuoyiFronted e2e:dcc:nas-uncontrolled-local-import:static`
- `pnpm --dir IntRuoyiFronted e2e:dcc:nas-uncontrolled-local-import:real:check`
- `$env:DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID='<task-owned-audit-id>'; pnpm --dir IntRuoyiFronted e2e:dcc:nas-uncontrolled-local-import:real:check`

## Expected Failures

- Schema RED fails because `dcc_nas_control_audit_file` table and indexes do not exist.
- Audit service RED fails because current audit only writes Excel rows and task counts, not queryable detail rows.
- Recognition RED fails because current logic does not persist per-uncontrolled-file project/category target snapshots.
- Recognition snapshot RED fails if candidate summaries are only returned transiently or logged instead of being persisted in audit/import snapshot fields.
- Transfer RED fails because current NAS transfer takes one selected `dccProjectCodeId` and category context for all files, not per audit file classification or per-item source signature.
- Legacy-field RED fails if `NAS_UNCONTROLLED_IMPORT` still requires or fills task-level `templateCategoryId` / `effectiveDate` / global target fields, or if old `NAS` / `LOCAL_FOLDER` required-input validation was accidentally loosened.
- Processor isolation RED fails if existing waiting task processors automatically read NAS content or call DCC submit for `NAS_UNCONTROLLED_IMPORT` before content download and `LOCAL_WRITTEN`.
- Import idempotency RED fails if the same `idempotencyKey` can be reused for a different selected-file payload, or if the backend trusts a client-mutated local relative path.
- Idempotency concurrency RED fails if there is no unique constraint or transactional lock protecting `tenant + operator + idempotencyKey`, allowing concurrent duplicate import tasks.
- Import atomicity RED fails if one invalid selected file can leave a partially created import task, task item, audit import binding, or `download_status=SELECTED`.
- Canonical hash RED fails if the same selected files in a different order produce a different `request_hash`, or if duplicate ids are silently deduplicated before validation.
- Local write RED fails because there is no local-write-result API, no path-hash guarded content endpoint for audit files, and no rule that archives only after `LOCAL_WRITTEN`.
- Local-write idempotency RED fails if browser/network retries can trigger a second archive or overwrite a terminal write result.
- Archive metadata RED fails if formal DCC submit can proceed by using current date, empty template, stale transfer task values or other implicit defaults when required archive metadata has no formal source.
- Import task RED fails if pending-recognition files, signature mismatch, duplicate audit ids or cross-task ids can create a processing task.
- Content authorization RED fails if a guessed auditFileId, cross-task binding or cross-tenant binding can download NAS content.
- Path safety RED fails because current uncontrolled import flow has no target local relative-path validator, target-exists check, path-length check or collision gate.
- Binary transfer RED fails if the content endpoint attempts to put file bytes into JSON/base64 instead of a binary stream or explicit chunks.
- Frontend RED fails because the dialog only shows summary and report download, not selectable file details, directory-first import creation, or local directory write flow.
- Command/script RED fails until the new package script and backend SQL static test file are added in the named project locations.
- Real precondition RED fails if runtime migrations are not applied, `DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID` is absent, or the task-owned audit fixture lacks matched/pending/archive-blocker rows.
- Full real page E2E RED/BLOCKED remains expected if the task-owned NAS source files are not present or not authorized for creation under the configured shared NAS path.

## GREEN Commands

- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest,DccControlledFileNasTransferServiceTest,DccProjectCodeServiceImplTest,DccControlledFileProjectCodeRecognitionServiceTest" test`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py IntRuoyiBackend/script/tests/test_dcc_file_category_match_rule_sql.py -q`
- `pnpm --dir IntRuoyiFronted e2e:dcc:nas-uncontrolled-local-import:static`
- `pnpm --dir IntRuoyiFronted e2e:dcc:project-code-associated-unclassified-auto-classify:static`
- `pnpm --dir IntRuoyiFronted e2e:dcc:project-code-list-unclassified-auto-classify:static`
- `pnpm --dir IntRuoyiFronted ts:check`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-DskipTests" compile`

## Refactor Checks

- No new fallback path, default project, default item, default classification, swallowed exception or mock success.
- Recognition service exposes explicit reasons for `UNCLASSIFIED_PENDING` and `AMBIGUOUS`.
- Existing NAS report download remains compatible.
- Existing NAS transfer and local folder import still pass current tests.
- `NAS_UNCONTROLLED_IMPORT` may allow nullable old task header fields, but old `NAS` and `LOCAL_FOLDER` flows still fail fast when their required template category, effective date or selected target metadata is missing.
- Existing waiting processors explicitly skip `NAS_UNCONTROLLED_IMPORT`; import-selected task creation alone never downloads content, submits DCC files or writes NAS source mappings.
- Existing DCC project-code associated-file classification still uses formal DCC taxonomy tree.
- Browser local directory write failures are visible and do not mark backend status as written.
- Browser unsupported/cancel paths do not mutate backend status and do not create import tasks.
- Backend never stores local absolute paths.
- Backend does not create an import task before the browser directory is authorized and relative-path checks pass.
- Backend rejects import-selected before recognition, on source signature mismatch, duplicate ids, cross-task ids or archived audit files.
- Backend computes and stores `request_hash`; same idempotency key with a different request hash conflicts instead of reusing or mutating the existing task.
- Backend computes `request_hash` from canonical sorted selected-file payload after duplicate detection; selected-file order alone must not create a conflict.
- Import-selected is atomic: any invalid selected row rejects the whole request and leaves audit statuses/import bindings unchanged.
- Backend regenerates expected local relative paths from persisted recognition snapshots and rejects client path drift.
- First implementation only processes explicit selected audit ids; all-filter/all-pages processing requires a separate server selection snapshot design.
- Backend never creates a controlled file before a matched item has `LOCAL_WRITTEN`.
- Backend never creates a controlled file without a formal source for required archive metadata; missing template category/effective date/change reason produces visible blocked or failed state, not default-success.
- Content and local-write-result endpoints are bound to current user, tenant, import task, audit file, source signature and local relative path snapshot.
- Local-write-result is idempotent for identical terminal results and conflict-safe for contradictory terminal results; archive creation remains one-time per audit file.
- Illegal local relative paths, existing local targets, path-too-long cases and normalized collisions fail fast without overwrite, truncation, auto-rename or default success.
- Content download uses binary/stream/chunk semantics, not JSON/base64 payloads.

## Evidence Log Template

- `BDD: <scenario name> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS`
- `BLOCKER: <precondition> -> FAIL, <impact>`
- `E2E: <real path> -> PASS/BLOCKED, <tenant/account/data ownership>`

## Test Blockers

- Missing `docs/database-rules.md`, `docs/frontend-development.md`, `docs/backend-development.md`, or `docs/e2e-rules.md` blocks implementation.
- Missing test NAS share or sample files blocks real E2E but not unit/static tests.
- Missing writable test tenant, DCC project code, file classification tree, category match rules or cleanup authorization blocks write E2E.
- Browser without File System Access API blocks local-directory write E2E; this is a product precondition, not a reason to switch to ZIP.
- Any `NAS_UNCONTROLLED_IMPORT` item whose project code, item or category cannot be uniquely resolved must remain visible as `未分类/待处理`; this path may verify local write to the pending folder but must not create a controlled file or ACTIVE NAS source mapping.
- Undefined formal archive metadata source blocks successful DCC 归档路径；当前 `dcc_nas_control_audit_file` 与 `dcc_controlled_file_nas_transfer_task_item` 未保存可直接提交的 `categoryId/directoryId/effectiveDate/versionNo/changeType/fileNumber` 等快照时，只能验证本地写入和 `ARCHIVE_METADATA_REQUIRED` 阻塞路径，不得用旧 NAS 转移默认值、候选 JSON、当前日期或空模板替代。
