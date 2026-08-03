# DCC 未受控文件本地下载与归类 BDD/TDD 设计

## Task Goal

基于当前 DCC NAS 目录、未受控文件统计、项目代码、item 和文件分类能力，设计“扫描未受控文件 -> 用户选择是否下载 -> 下载到本地对应目录 -> 按路径或名称归类”的正式实现方案，并将无法唯一识别项目代码、item 或文件分类的文件标记为“未分类/待处理”，不进行猜测归类。

## Scope

- 第一阶段已交付需求、系统设计、BDD、严格 TDD、真实 E2E 与测试数据文档。
- 当前阶段按上述文档推进开发实现与验证，优先完成 `dcc_nas_control_audit_file` 明细 schema、持久化模型和可执行 schema 验证。
- 本任务不操作真实 NAS 文件、不启动远端环境、不写入生产或正式业务数据库；M30 已在本机 Docker 测试库受控应用任务迁移并插入任务自有、可清理的 audit fixture，用于真实 E2E 前置 gate。
- 设计优先复用当前 DCC 页面、接口、服务、数据模型、分类规则和测试结构。

## Milestones

- [x] M1：核对项目规则、经验门禁、现有 DCC/NAS 实现和测试资产。
- [x] M2：形成需求边界、领域状态、交互流程、复用方案与接口/数据设计。
- [x] M3：形成 BDD 场景、严格 TDD 顺序、真实 E2E 路径和测试数据设计。
- [x] M4：完成结构校验、一致性复核、验证报告和任务收尾阻塞记录。
- [x] M5：二次优化潜在开发问题，补齐状态枚举、目录授权时序、幂等冲突、命令工作目录和测试数据清理门禁。
- [x] M6：将浏览器本地目录写入门禁沉淀到现有 E2E 长期规则和经验索引。
- [x] M7：按严格 TDD 完成 schema 切片，新增 `dcc_nas_control_audit_file` 明细表、DO、Mapper、迁移测试和测试 schema。
- [x] M8：优化开发文档潜在问题，补齐二进制/分块下载、本地目标已存在、路径过长、状态流转、并发处理和 content 权限校验门禁。
- [x] M9: Strengthen executable docs for files page API, recognition status split, import-selected rejection, content/local-write snapshot binding, no backend mutation on directory cancel, and cross-task/signature-invalid verification.
- [x] M12：优化后续开发潜在问题，补齐识别候选摘要持久化、识别结果写入规则、import 任务快照字段、幂等请求哈希、后端相对路径重算校验和显式选择范围门禁。
- [x] M13：按严格 TDD 完成确定性预识别后端切片，新增 `/files/recognize` 服务实现、候选摘要、原因码、期望本地相对路径和相邻回归验证。
- [x] M14：优化 import-selected 与本地回写开发文档，补齐整体原子拒绝、规范化请求哈希、audit/import 绑定、重复 local-write-result 幂等和冲突终态门禁。
- [x] M15：按严格 TDD 完成 import-selected 任务快照 schema 切片，新增 transfer task/task item/audit file 绑定字段、DO 字段、测试 schema 和 SQL 静态合同。
- [x] M16：优化 import-selected 后续实现门禁，补齐旧 NAS transfer 必填字段隔离、legacy processor 跳过、幂等并发锁和正式归档元数据来源验证。
- [x] M17：按严格 TDD 完成 import-selected 服务契约和 legacy processor 隔离切片，新增无旧字段请求 VO、服务入口签名和 `NAS_UNCONTROLLED_IMPORT` waiting processor 跳过逻辑。
- [x] M18: Strict TDD backend service-level import-selected creation slice, adding whole-request validation, canonical request hash, task/item snapshot inserts, audit import bindings, and atomic invalid-selection rejection.
- [x] M19：按严格 TDD 完成 import-selected 服务级幂等与并发保护切片，覆盖同 key/hash 复用、不同 hash 冲突、重复 audit id 前置拒绝和事务内二次幂等检查。
- [x] M20：按严格 TDD 完成 import-selected controller 契约切片，新增 `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected` 路由、写入权限组合和 `@Valid @RequestBody` 请求体。
- [x] M21：按严格 TDD 完成 content binary download controller/service 契约切片；在隔离 worktree 完成二进制 content controller、快照绑定服务、失败路径和相邻回归验证。
- [x] M22：按严格 TDD 完成 local-write-result controller/service 契约切片，覆盖重复成功回放幂等、冲突终态拒绝、LOCAL_WRITTEN 前后状态流转和不重复归档。
- [x] M23：按严格 TDD 完成 `LOCAL_WRITTEN` 后归档元数据缺失显式阻塞切片，覆盖 `ARCHIVE_METADATA_REQUIRED`、不读取 NAS、不提交 workflow、不创建受控文件和重复回写幂等。
- [x] M24：补齐正式归档元数据来源后，按严格 TDD 完成创建受控文件、ACTIVE NAS 来源映射和已归档重复回写保护；正式归档仅使用处理项级 `archive*Snapshot` 元数据，缺失时继续 `ARCHIVE_METADATA_REQUIRED`。
- [x] M25：按严格 TDD 完成前端静态契约和最小 UI/API 集成切片，覆盖目录授权前置、相对路径校验、content 下载、本地写入、local-write-result 回写、未分类待处理和 `ARCHIVE_METADATA_REQUIRED` 可见状态。
- [x] M26：复核 M24 正式归档元数据前置并固化文档门禁，明确缺少处理项级归档元数据快照时必须保持 `ARCHIVE_METADATA_REQUIRED` 阻塞，不得伪造成功归档。
- [x] M27：完成文档一致性审计并补齐可检索门禁标记，确保 TDD/test-data 文档显式覆盖 `未分类/待处理`、`showDirectoryPicker`、`LOCAL_WRITTEN`、`ARCHIVE_METADATA_REQUIRED` 和 `NAS_UNCONTROLLED_IMPORT`。
- [x] M28：完成真实 E2E 前置审计，确认本机运行态、测试租户页面入口和 Chrome File System Access API 可用，同时阻塞记录运行库缺少 `dcc_nas_control_audit_file` 及 import snapshot 迁移、Playwright bundled Chromium 缺失导致真实 E2E 不可放行。
- [x] M29：按严格 TDD 补齐真实 E2E 可执行前置门禁 `e2e:dcc:nas-uncontrolled-local-import:real:check`，只读检查本机运行态、运行库 schema、浏览器 File System Access API 和任务自有 audit 样本；缺迁移或缺样本时 fail fast，不声明真实 E2E PASS。
- [x] M30：按严格 TDD 修复并应用本地测试库迁移，插入任务自有 audit fixture，并验证 `DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID=1` 时真实 E2E 前置 gate PASS；完整页面下载在当时仍阻塞于未授权创建/定位真实 NAS 样本文件。
- [x] M31：完成独立完成度审计补强，修正 full 真实页面 E2E 非 check 模式阻塞提示，明确未创建/未确认共享 NAS 源文件时必须 fail fast，不能把 `real:check` PASS 误写成完整页面 E2E PASS。
- [x] M32：按严格 TDD 修正待处理文件本地下载范围，允许 `UNCLASSIFIED_PENDING/AMBIGUOUS` 选择下载到 `_未分类待处理`，并验证仅进入 `PENDING_MANUAL_REVIEW`、不创建受控文件或 ACTIVE NAS 来源映射。
- [x] M33：复跑授权前可执行验收 gate，确认静态合同、真实前置 gate 和验收计划仍通过，full 真实页面 E2E 当时仍阻塞于未创建/未确认的共享 NAS 源文件字节。
- [x] M34：按用户授权尝试在共享 NAS 创建/确认 3 个任务源文件并运行 full E2E，确认当前配置 NAS 账号对目标测试目录不可写，full E2E 阻塞于 NAS `UnauthorizedAccessException`。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`
- 使用 UTF-8 重新读取本任务文档，确认无乱码。
- `node -e "<doc consistency marker scan>"`，确认任务文档和验收文档均包含 `ARCHIVE_METADATA_REQUIRED`、`未分类/待处理`、`showDirectoryPicker`、`LOCAL_WRITTEN` 和 `NAS_UNCONTROLLED_IMPORT`。
- 核对每个生产行为均有 BDD 场景和 RED/GREEN 实施入口。
- 核对真实 E2E 通过现有前端入口完成，API 仅用于最终只读核验。
- 核对“无法唯一识别”始终落到“未分类/待处理”，不存在默认项目、默认 item、默认分类或静默跳过。
- 核对 RED/GREEN 命令从 `E:\IntRuoyi` 可定位到 `IntRuoyiBackend`、`IntRuoyiFronted` 和 `IntRuoyiBackend/script/tests`。
- 核对 import task 只能在本地目录授权和相对路径校验成功后创建，取消目录选择或浏览器不支持时无后端处理任务。
- Check content and local-write-result bind current user, tenant, import task, audit file, source signature, and local relative path snapshot.
- 核对识别候选摘要必须可持久化，`MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS` 的原因码、候选摘要和期望本地相对路径均可被测试验证。
- 核对 `UNCLASSIFIED_PENDING / AMBIGUOUS` 文件可被显式选择下载到 `_未分类待处理` 本地相对路径，但本地写入成功后只能进入 `PENDING_MANUAL_REVIEW`，不得自动归档。
- 核对 `import-selected` 使用显式 `auditFileId` 列表、后端重算本地相对路径、相同幂等键不同 `request_hash` 返回冲突。
- 核对 `import-selected` 任一选中项无效时整体拒绝且无部分 `SELECTED`、无半创建任务。
- 核对规范化 `request_hash` 对 `selectedFiles` 顺序不敏感，重复 `auditFileId` 在 hash 前失败。
- 核对 audit 明细与 import task/item 绑定可查询，重复活动绑定被拒绝。
- 核对重复 local-write-result 幂等返回且不重复触发 DCC 归档，冲突终态回写被拒绝。
- 核对 `NAS_UNCONTROLLED_IMPORT` 不依赖旧 NAS 转移任务级 `templateCategoryId/effectiveDate/dccProjectCodeId` 默认值，旧 `NAS` / `LOCAL_FOLDER` 入口仍保留必填校验。
- 核对 `NAS_UNCONTROLLED_IMPORT` 不会被现有 waiting processor 自动读取 NAS、提交 DCC 或写 ACTIVE NAS 来源映射。
- 核对正式归档元数据缺失时进入 `ARCHIVE_METADATA_REQUIRED` 或明确失败/阻塞状态，不使用当前日期、空模板或旧任务值默认成功。
- `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js`
- `pnpm e2e:dcc:nas-uncontrolled-local-import:static`
- `$env:DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID='1'; pnpm e2e:dcc:nas-uncontrolled-local-import:real:check`
- `$env:DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID='1'; $env:DCC_NAS_UNCONTROLLED_IMPORT_ALLOW_NAS_WRITE='1'; pnpm e2e:dcc:nas-uncontrolled-local-import:real`（full 模式必须使用真实 NAS 源文件和真实浏览器本地目录写入；当前若阻塞，原因必须指向 NAS 测试目录写权限或源文件真实缺失）
- `pnpm e2e:dcc:nas-uncontrolled-local-import:real:check`（未提供任务自有样本 ID 时仍预期 fail fast，不得声明真实 E2E PASS）
- `node tests/e2e/nas-control-audit-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\frontend-feature-evidence.md`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/database-schema-evidence.md`

## Current Status

blocked

设计文档、BDD/TDD/E2E 验收文档、潜在问题优化、M7 schema 明细切片、M11 files page API 切片、M13 确定性预识别后端切片、M14 import-selected/local-write 文档门禁优化、M15 import-selected 任务快照 schema 切片、M16 import-selected 后续实现门禁加固、M17 import-selected 服务契约/legacy processor 隔离、M18 服务级原子创建、M19 服务级幂等并发保护、M20 controller 契约、M21 content binary download、M22 local-write-result、M23 归档元数据缺失显式阻塞、M24 正式归档成功路径、M25 前端静态契约/最小 UI/API 集成、M26 正式归档元数据阻塞门禁、M27 文档一致性审计、M28 真实 E2E 前置审计、M29 可执行真实 E2E 前置门禁、M30 本地测试库迁移/样本前置验证、M31 独立完成度审计补强、M32 待处理文件本地下载范围修正、M33 当前 gate 复核和 M34 授权后 full E2E 尝试均已完成。M34 复跑证明 `pnpm e2e:dcc:nas-uncontrolled-local-import:static` 和 `DCC_NAS_UNCONTROLLED_IMPORT_AUDIT_TASK_ID=1` 的 `real:check` 仍通过；但在用户明确授权共享 NAS 测试文件后，`DCC_NAS_UNCONTROLLED_IMPORT_ALLOW_NAS_WRITE=1` 的 full E2E 仍失败于 `prepareSharedNasSourceFiles failed: New-Item : Access to the path 'codex-dcc-uncontrolled-local-import' is denied`。当前完整真实页面 E2E 必须继续阻塞：运行库 schema、浏览器目录能力、audit 样本和待处理本地路径前置已满足，但配置的 NAS 账号无法在 `\\172.30.30.4\质量体系文件\codex-dcc-uncontrolled-local-import` 创建任务源文件，因此尚未证明真实 NAS 源字节、浏览器本地写入和页面归类全链路。影响：不能把前置 gate PASS、静态合同、API-only 或数据库 fixture 写成“已完成真实下载到本地目录并归类”的页面 E2E PASS。最终 `completed` 状态暂不标记：需要给配置 NAS 账号授予任务测试目录写权限、提供一个已确认可写的共享 NAS 子目录，或由用户手动放置并确认 3 个任务源文件存在后，才能继续 full E2E；当前工作区还存在任务开始前及其它并发任务脏文件，且分支当前相对 `origin/int_main` 为 behind 2，不能把本任务与其它并发任务资产混在一个收尾提交里。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。路径或名称无法唯一识别时进入正式的“未分类/待处理”业务状态，不视为 fallback。
- `是否从根因和长期维护角度解决`：是。设计将识别、下载、归档、待处理和重试建模为可审计状态与正式服务边界。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`：文件分类必须来自正式 DCC 文件分类树，自动归类不得写回“未分类文件类型”后宣称成功。
- `docs/database-rules.md#DCC 文件类别规则种子门禁`：`dcc_file_category_match_rule` 缺失、歧义、未知类型或插入不完整必须 fail fast，不得用硬编码 fallback 或直接 SQL 修受控文件分类。
- `docs/e2e-rules.md#规划型 E2E 前置与业务 RED 分离门禁`：本任务仅输出设计和验收 gate，不提前实现生产代码；后续实施必须先通过前置 RED。
- `docs/e2e-rules.md#浏览器本地目录写入门禁`：涉及 `showDirectoryPicker`、本地目录授权和本地写入结果回写时，必须验证目录授权前无后端写入任务、`LOCAL_WRITTEN` 前无正式归档、取消授权无 import task；`real:check`、DB fixture 和静态合同不能替代授权共享 NAS 源文件与 full 页面下载证据。
- `docs/e2e-rules.md#Element Plus 表格选择门禁` 与真实 E2E 规则：后续写入型验证必须按页面可见业务唯一文本选择文件，API 仅用于最终只读核验。
- 严格 no-fallback 门禁：无法唯一判断项目代码、item 或分类时进入正式 `未分类/待处理` 状态，不允许默认项目、默认 item、默认分类、ZIP 降级或静默成功。

## Acceptance Outputs

- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md`

## Cleanup Keep

- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/database-schema-evidence.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/backend-api-evidence.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/frontend-feature-evidence.md
