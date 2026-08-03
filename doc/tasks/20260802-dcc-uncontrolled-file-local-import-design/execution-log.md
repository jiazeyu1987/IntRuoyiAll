# Execution Log

## User Intent

- 用户要求按照 BDD + 严格 TDD 方式完成文档设计。
- 设计应尽量复用当前系统。
- 点击“统计未受控文件”后，用户可选择是否将新的未受控文件下载到本地对应目录。
- 选中文件下载后，系统依据路径或名称，将文件归入对应 DCC 项目代码下某个 item 的某个文件分类。
- 无法判断项目代码、item 或分类时，必须标记为“未分类/待处理”。

## Command Intent

- 读取项目任务、后端、前端、数据库、E2E 和编码规则，确保设计符合现有门禁。
- 读取 `bdd-tdd-acceptance-planner` 技能及验收文档结构。
- 只读检索现有 DCC/NAS 实现、分类规则、测试和历史任务证据。
- 生成任务设计与验收文档，不运行生产构建、不修改生产代码、不操作真实 NAS 或数据库。
- 当前继续按已生成开发文档执行实现与验证，优先推进 schema 明细表切片；本轮只修改迁移、测试 schema、DO/Mapper、测试与任务证据，不操作真实 NAS 或真实业务数据库。
- 按用户要求继续优化开发文档，补齐大文件传输、本地路径可写性、状态机、并发和 content 权限门禁，确保按文档开发时能被测试验证拦住潜在问题。
- 本轮继续优化 import-selected 和 local-write-result 文档门禁，只修改设计、BDD/TDD/E2E、测试数据和任务证据，不新增生产代码、不操作真实 NAS、本地目录或业务数据库。
- 本轮继续加固 import-selected 后续实现文档门禁，只修改设计、BDD/TDD/E2E、测试数据和任务证据，补齐旧 NAS transfer 必填字段隔离、legacy processor 跳过、幂等并发保护和正式归档元数据来源验证。

## BDD Evidence

BDD: 扫描并选择下载可识别的未受控文件 -> Given NAS 已连接且文件尚未进入 DCC 管理 When 用户扫描并选择下载 Then 文件进入本地对应目录并归入唯一识别的项目代码、item 与文件分类。

BDD: 无法唯一归类的文件进入待处理 -> Given 未受控文件无法从路径或名称唯一识别项目代码、item 或分类 When 用户选择下载 Then 文件保留可追溯下载结果并标记为“未分类/待处理”，不得默认归类。

BDD: 目录授权前不创建导入任务 -> Given 用户已勾选未受控文件并打开下载归类预览 When 浏览器不支持目录选择或用户取消目录选择 Then 后端不创建 import task、不下载内容、不回写本地成功、不创建 DCC 受控文件。

BDD: 幂等与已归档冲突 -> Given 同一 audit file 已经完成归档 When 用户使用相同或不同 idempotencyKey 重复提交 Then 相同 key 返回原任务，不同 key 返回已处理或冲突状态，不创建第二个受控文件。

BDD: 未受控扫描明细可审计持久化 -> Given NAS audit 扫描发现未受控文件 When 系统保存 audit task Then 每个未受控文件都有 `dcc_nas_control_audit_file` 明细、source signature、初始识别/下载/归档状态和 tenant-scoped path hash 索引。

BDD: 大文件下载使用二进制传输 -> Given 未受控文件超过 JSON/base64 安全承载范围 When 用户选择本地目录并开始下载 Then content 接口使用二进制、流式或明确分块传输，失败回写 `LOCAL_WRITE_FAILED`。

BDD: 本地目标路径不可安全写入时阻塞 -> Given 目标本地路径已存在、过长或规范化冲突 When 用户确认下载 Then 系统记录 `LOCAL_PATH_COLLISION` 或 `LOCAL_PATH_TOO_LONG`，不得覆盖、截断、自动改名或归档。

BDD: 并发处理同一 audit file 只有一个归档结果 -> Given 两个请求同时提交同一 audit file When 后端创建或执行 import task Then 只有一个请求可归档，另一个返回明确冲突或已处理。


BDD: Import-selected rejects unrecognized or stale snapshot -> Given audit file is PENDING_RECOGNITION, cross-task, or has stale sourceSignature When import-selected is called Then backend rejects without creating import task, reading NAS content, writing local result, or creating controlled file.

BDD: Content download is task and tenant bound -> Given user knows another task or tenant auditFileId When content is requested through current importTaskId Then backend rejects without returning bytes, moving to CONTENT_READY, local-write-result, or archive.

BDD: Deterministic pre-recognition only updates audit snapshot -> Given audit files are still `PENDING_RECOGNITION` When `/files/recognize` runs Then backend writes `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`, stable reason codes, candidate summary and expected local relative path without reading file bytes or creating DCC controlled files.

BDD: Import-selected is atomic -> Given selected files contain a valid audit file and an invalid audit file When import-selected is called Then backend rejects the whole request without partial task rows, partial SELECTED statuses, content reads, local-write-result, or DCC archive.

BDD: Import request hash is canonical -> Given the same selected audit files are submitted with the same idempotencyKey in a different order When import-selected is called again Then backend returns the original task; duplicate audit ids fail before hashing and are not silently deduplicated.

BDD: Import-selected idempotency is transaction protected -> Given an identical idempotent import task appears after the first lookup but before insert When the backend enters task creation transaction Then it rechecks the key/hash under lock and returns the existing task without task/item/audit writes.

BDD: Import-selected controller is write-permission protected -> Given an authorized user submits selected uncontrolled audit files through the NAS audit task API When `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected` is called Then the controller requires NAS transfer write permissions, validates the request body, binds current login user and audit task id, and delegates to the import-selected service without exposing legacy transfer defaults.

BDD: Local write result replay is idempotent -> Given a matched audit file has already completed LOCAL_WRITTEN and archive When the same local-write-result is replayed Then backend returns current state without creating another controlled file or ACTIVE NAS source mapping; conflicting terminal results are rejected.

BDD: Import-selected task snapshots are schema-backed -> Given selected audit files will be locked into an import task When the backend creates `NAS_UNCONTROLLED_IMPORT` Then task header stores audit task, idempotency key and canonical request hash, task items store audit file/source signature/recognition/local path snapshots, and audit files expose current import task/item binding for duplicate-selection checks.

BDD: Import-selected does not reuse legacy NAS transfer defaults -> Given NAS_UNCONTROLLED_IMPORT reuses the transfer task table When import-selected creates a task Then task-level template/effective date/project defaults are not required or fabricated, and old NAS/LOCAL_FOLDER flows still keep their required-input validation.

BDD: Legacy processor skips uncontrolled import tasks -> Given a NAS_UNCONTROLLED_IMPORT task exists When existing NAS transfer processors run before content and LOCAL_WRITTEN Then they skip the task and do not read NAS content, submit DCC files, or write ACTIVE NAS source mappings.

BDD: Archive metadata missing is visible -> Given a matched file is LOCAL_WRITTEN but formal DCC archive metadata source is missing When backend attempts archive Then it records ARCHIVE_METADATA_REQUIRED or an explicit blocked/failed state instead of using current date, empty template, or old task defaults.

## Milestone Log

### M1

- 状态：`completed`
- 已完成：读取任务收尾、PowerShell/UTF-8、前后端开发、数据库、E2E 和技术栈路由规则；读取 BDD/TDD 验收规划技能；核对 `docs/experience-index.md` 中 DCC 分类、规则 seed、真实 E2E、no-fallback 相关门禁。
- 阻塞：无。

### M2

- 状态：`completed`
- 已完成：形成 `design.md`，明确复用 NAS 管理页、未受控统计任务、NAS transfer 链路、DCC 项目代码、文件分类树和 `dcc_file_category_match_rule`；定义 `未分类/待处理` 为正式业务状态。
- 阻塞：无。

### M3

- 状态：`completed`
- 已完成：生成 `docs/acceptance/bdd-scenarios.md`、`docs/acceptance/tdd-plan.md`、`docs/acceptance/e2e-plan.md`、`docs/acceptance/test-data.md`，覆盖可识别归档、待处理、浏览器目录写入 fail-fast、NAS 文件变化、权限不足、重复提交和分页边界。
- 阻塞：无。

### M4

- 状态：`ready_for_closeout`
- 已完成：执行验收结构 validator、UTF-8 读取检查和 `git diff --check`；新增 `verification-report.md`；运行 task-closeout-cleanup preview/apply，结果无删除项；按 project-experience-consolidation 将 closeout 状态格式经验合并到 `docs/task-closeout-rules.md` 和 `docs/experience-index.md`。
- 阻塞：最终 Git 收尾未执行。当前工作区存在任务开始前的其它任务改动和未跟踪目录，且分支 `int_main` 已 ahead 2；按项目规则必须先处理脏工作区基线和 push 阻塞，本任务不能清理、改写或混合提交并发任务资产。

### M5

- 状态：`completed`
- 已完成：按用户要求优化文档，补强开发前置门禁、两阶段处理时序、`LOCAL_WRITTEN` 后置归档、路径安全校验、路径冲突阻塞、仅查看不下载、本地写入失败不归档、NAS 文件变化复核和归档失败状态分离。
- 阻塞：无。

### M6

- 状态：`completed`
- 已完成：继续优化潜在开发问题，补齐 `PENDING_RECOGNITION` 初始识别状态、`local_write_error_code/archive_error_code` 错误码、`source_signature` 生成格式、先目录授权再创建 import task 的请求时序、取消目录选择无后端任务、已归档 audit file 重复提交冲突、`auditTaskId/importTaskId` 命名边界，以及从仓库根目录可执行的后端 Maven、后端 SQL pytest、前端 pnpm 命令。
- 阻塞：无。

### M7

- 状态：`completed`
- 已完成：按 `project-experience-consolidation` 收尾门禁，将浏览器本地目录写入的通用 E2E 门禁合并到 `docs/e2e-rules.md#浏览器本地目录写入门禁`，并在 `docs/experience-index.md` 增加关键词索引，避免后续开发把 ZIP、默认下载目录、API-only 下载或目录授权前创建后端任务误写成通过。
- 阻塞：无。

### M8

- 状态：`completed`
- 已完成：读取 `database-schema-delivery` 技能和数据库契约；按严格 TDD 完成 `dcc_nas_control_audit_file` schema 切片，新增迁移、test schema、DO、Mapper、JUnit schema 测试和 SQL 静态合同测试；修正 schema 测试的非破坏性断言范围，只扫描本次 migration，不放宽全局非破坏性规则。
- 验证：Maven schema JUnit PASS，SQL pytest PASS，database-schema-evidence validator PASS。
- 阻塞：无。

### M9

- 状态：`completed`
- 已完成：按用户要求优化文档潜在问题，补齐二进制/分块内容下载、content 绑定权限校验、本地目标已存在、路径过长、安全目录段、状态流转终态、并发处理同一 audit file 的事务门禁，并同步更新 BDD、TDD、E2E 和测试数据文档。
- 阻塞：无。

### M10

- Status: completed
- Completed: strengthened executable docs for no backend mutation on directory cancel/unsupported browser/precheck failure, files page API contract, UNCLASSIFIED_PENDING vs AMBIGUOUS split, import-selected rejection rules, content/local-write-result snapshot binding, cross-task/signature-invalid E2E, and test-data cleanup boundaries.
- Verification: acceptance validator PASS, UTF-8 read check PASS, scoped git diff --check PASS.
- Blockers: none for documentation; implementation remains in progress.

## Verification Evidence

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，输出 `BDD/TDD acceptance plan validation passed.`
- GREEN: `node -e "<utf8 read check>"` -> PASS，10 个本任务、验收和经验文档均 `contains_replacement=false`。
- GREEN: `git -C E:\IntRuoyi diff --check -- docs/acceptance/... docs/task-closeout-rules.md docs/experience-index.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/...` -> PASS，仅提示 LF 将被 Git 转换为 CRLF，无 trailing whitespace 或 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode preview` -> PASS，keep 4 个正式任务文档，delete/blocked/warnings 均为 none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode apply` -> PASS，deleted_paths 为 none。
- GREEN: `project-experience-consolidation` -> PASS，新增 closeout 状态格式规则，避免 `Current Status` 因反引号或前置说明被 cleanup apply 解析为 `unknown`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，本轮优化后结构校验仍通过。
- GREEN: `node -e "<utf8 read check>"` -> PASS，本轮优化触达文档均 `contains_replacement=false`。
- GREEN: `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS，仅提示 LF 将被 Git 转换为 CRLF，无 whitespace error。
- GREEN: `task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode preview/apply` -> PASS，本轮优化后 cleanup apply 无删除项、无 blocked、无 warnings。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，本轮二次优化后结构校验仍通过。
- GREEN: `node -e "<utf8 read check>"` -> PASS，本轮二次优化触达 5 个文档均 `contains_replacement=false`。
- GREEN: `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS，仅提示 LF 将被 Git 转换为 CRLF，无 whitespace error。
- GREEN: `project-experience-consolidation` -> PASS，浏览器本地目录写入门禁已合并到 `docs/e2e-rules.md`，关键词索引已写入 `docs/experience-index.md`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，长期经验合并后结构校验仍通过。
- GREEN: `node -e "<utf8 read check>"` -> PASS，10 个任务、验收和经验文档均 `contains_replacement=false`。
- GREEN: `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/... docs/acceptance/... docs/e2e-rules.md docs/experience-index.md` -> PASS，仅提示 LF 将被 Git 转换为 CRLF，无 whitespace error。
- Documentation-only verification: 本任务未修改生产代码、数据库、运行环境或真实 NAS 文件；后续实现 RED/GREEN 命令已写入 `docs/acceptance/tdd-plan.md`。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，初始失败原因为 `dcc_nas_control_audit_file` migration 不存在，符合 schema RED 预期。
- RED: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> FAIL，初始失败原因为 SQL 静态合同或 migration 尚未存在，符合 schema RED 预期。
- FIX: Maven GREEN 首次重跑失败在新增测试把历史 runtime schema 全量执行 `DELETE FROM dcc_` 扫描；已收敛为仅对本次 migration 做非破坏性断言，runtime schema 仅用于验证新表可发现，未放宽全局规则。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，2 passed in 3.18s。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，本轮文档补强后验收结构仍通过。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`
- GREEN: `node -e "<utf8 read check>"` -> PASS，本任务 9 个任务、验收和 schema 证据文档均 `contains_replacement=false`。
- GREEN: `git diff --check -- <本任务文档与schema切片文件>` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。

- GREEN: python -X utf8 C:/Users/BJB110/.codex/skills/bdd-tdd-acceptance-planner/scripts/validate_acceptance_plan.py --root E:/IntRuoyi -> PASS, M10 doc strengthening after user request.
- GREEN: python -X utf8 -c utf8_read_check -> PASS, 8 task/acceptance docs contain no replacement characters.
- GREEN: git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md -> PASS, only Git LF-to-CRLF warnings.

## Existing Worktree Baseline

- 任务开始时根仓库分支为 `int_main`，相对 `origin/int_main` ahead 2。
- 工作区存在其它任务改动和未跟踪目录；本任务不得改写或清理这些并发任务资产。
- 提交前按项目 Git 规则需单独处理任务开始时的脏工作区基线，并记录 commit hash 与文件清单；本轮未执行 baseline commit、implementation commit 或 push，原因是当前任务只应收口文档设计，且不得混合提交其它并发任务资产。

### M11

- Status: completed
- Completed: implemented backend files page query slice for GET /dcc/controlled-files/nas-control-audit/{taskId}/files, including controller contract, page request/response VOs, service method, mapper filters, and stable id ordering.
- RED: targeted controller contract failed because the /files endpoint mapping was missing.
- GREEN: targeted controller contract and adjacent audit service/controller regression passed.
- Blockers: none for this slice; recognize/import/content/local-write/frontend/E2E remain in progress.

## M11 Verification Evidence

- RED: targeted DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission -> FAIL, missing endpoint mapping /dcc/controlled-files/nas-control-audit/{taskId}/files.
- GREEN: targeted DccNasControlAuditControllerTest#nasControlAudit_mapsFilesPageWithControlledFileQueryPermission -> PASS, Tests run 1, Failures 0, Errors 0, Skipped 0.
- REGRESSION: DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest -> PASS, Tests run 3, Failures 0, Errors 0, Skipped 0.

### M12

- Status: completed
- Completed: optimized development documents for recognition candidate persistence, stable recognition reason codes, import task/item snapshot fields, idempotency request hash conflicts, backend-regenerated local-relative-path verification, pre-import local path precheck boundaries, explicit selected-id scope, and E2E/test-data evidence for those gates.
- Verification: acceptance validator PASS, UTF-8 read check PASS, scoped git diff --check PASS.
- Blockers: none for documentation; implementation remains in progress.

## M12 Verification Evidence

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: PowerShell UTF-8 read check for `design.md`, `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, and `test-data.md` -> PASS, all `contains_replacement=False`.
- GREEN: `git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS, only Git LF-to-CRLF warnings.

### M13

- Status: completed
- Completed: implemented deterministic backend pre-recognition for `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`; it reuses enabled DCC project codes, active file categories, active category match rules and active taxonomy paths to populate audit-file classification snapshots.
- Behavior: unique project + unique category writes `MATCHED`; missing project/category writes `UNCLASSIFIED_PENDING`; multiple project/category candidates writes `AMBIGUOUS`; unknown category rule type fails fast instead of silently downgrading.
- Boundaries: recognition only updates `dcc_nas_control_audit_file` snapshot fields and does not read NAS content, download bytes, create import tasks, write local result, archive files, or create DCC controlled files.
- Blockers: import-selected, content binary download, local-write-result, frontend static contract and real E2E remain in progress.

## M13 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognition snapshot columns/fields such as `classification_candidates_json` were missing.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: recognize VO/service implementation was not present.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS, 2 passed in 0.17s.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8 read check for `task.md`, `execution-log.md`, `verification-report.md`, and `backend-api-evidence.md` -> PASS, all `contains_replacement=False`.
- GREEN: `git -C E:\IntRuoyi diff --check -- <M13 implementation, schema, SQL contract and task evidence files>` -> PASS, only Git LF-to-CRLF warnings.

### M14

- Status: completed
- Completed: optimized executable development docs for import-selected atomicity, canonical request hash ordering, audit/import binding visibility, active duplicate binding rejection, local-write-result idempotency, and conflicting terminal write-result rejection.
- Scope: documentation and task evidence only; no production code, NAS files, local folders, runtime services, or business data were modified.
- Verification: acceptance validator PASS, UTF-8 read check PASS, scoped git diff --check PASS.
- Blockers: none for documentation; import-selected, content binary download, local-write-result, frontend static contract and real E2E remain implementation work.

## M14 Verification Evidence

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8 read check for `task.md`, `execution-log.md`, `design.md`, `verification-report.md`, `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, and `test-data.md` -> PASS, all `contains_replacement=False`.
- GREEN: `git -C E:\IntRuoyi diff --check -- <M14 task and acceptance docs>` -> PASS, only Git LF-to-CRLF warnings for acceptance docs.

### M15

- Status: completed
- Completed: implemented import-selected task snapshot schema slice for `NAS_UNCONTROLLED_IMPORT`, including additive migration fields on `dcc_controlled_file_nas_transfer_task`, `dcc_controlled_file_nas_transfer_task_item`, and `dcc_nas_control_audit_file`; synced DO fields, DCC test schema, JUnit schema contract and SQL static contract.
- Scope: schema/persistence contract only; no import-selected service/API behavior, content download, local-write-result, NAS files, local folders or business data were modified.
- Diagnostic: the targeted Maven run initially had a stale surefire report from the RED phase and briefly showed the known Windows Maven `WinNTFileSystem.delete0` cleanup stack; final command output completed with `BUILD SUCCESS`.
- Blockers: none for this schema slice; import-selected service/API, content binary download, local-write-result, frontend static contract and real E2E remain implementation work.

## M15 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `DCC NAS uncontrolled import task snapshot migration must exist`.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS, 4 passed in 1.49s.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\database-schema-evidence.md` -> PASS, `Database schema evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8 read check for M15 task/evidence files -> PASS, all `contains_replacement=False`.
- GREEN: scoped `git diff --check` for M15 tracked files -> PASS, no whitespace errors.
- GREEN: trailing whitespace check for new SQL contract file -> PASS.

### M16

- Status: completed
- Completed: strengthened executable docs for legacy NAS transfer field isolation, `NAS_UNCONTROLLED_IMPORT` processor isolation, idempotency/concurrency locking, stable audit-row locking, and formal archive metadata source requirements.
- Scope: documentation and task evidence only; no production code, NAS files, local folders, runtime services or business data were modified.
- Verification: acceptance validator PASS, UTF-8 read check PASS, scoped git diff --check PASS.
- Blockers: none for documentation; import-selected service/API, content binary download, local-write-result, frontend static contract and real E2E remain implementation work.

## M16 Verification Evidence

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8 read check for `task.md`, `execution-log.md`, `design.md`, `verification-report.md`, `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, and `test-data.md` -> PASS, all `contains_replacement=False`.
- GREEN: `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS, only Git LF-to-CRLF warnings.

### M17

- Status: completed
- Completed: implemented the first import-selected backend contract/isolation slice: added `DccNasUncontrolledImportSelectedReqVO` without legacy transfer target fields, added `createUncontrolledImportTask(Long userId, Long auditTaskId, DccNasUncontrolledImportSelectedReqVO reqVO)` to the transfer service, added `SOURCE_TYPE_NAS_UNCONTROLLED_IMPORT`, and made the legacy waiting processor skip that source type before claiming, reading NAS content, submitting DCC files, or writing ACTIVE NAS source mappings.
- Boundary: `createUncontrolledImportTask` deliberately fails fast with `UnsupportedOperationException` until the next TDD slice implements atomic validation and persistence; this avoids default success, fallback, or partial import behavior.
- Diagnostic: the first GREEN attempt failed because the now-skipped task no longer consumed an old `selectById` test stub; the unused stub was removed and the same targeted command passed.
- Blockers: full import-selected task creation, canonical request hash, audit-file locking/binding, content binary download, local-write-result, archive, frontend static contract and real E2E remain in progress.

## M17 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `DccNasUncontrolledImportSelectedReqVO` class missing.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: legacy processor claimed `sourceType=NAS_UNCONTROLLED_IMPORT` through `taskMapper.claimWaitingTask(77L, ...)`.
- FIX: same targeted Maven command first failed after implementation with Mockito `UnnecessaryStubbingException` because `taskMapper.selectById(77L)` was no longer used once the processor correctly skipped the import task; removed that obsolete test stub.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M17 task/evidence/backend files -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.
- GREEN: `git -C E:\IntRuoyi diff --check -- <M17 tracked task/backend files>` -> PASS, only Git LF-to-CRLF warnings.

### M18

- Status: completed
- Completed: implemented service-level `createUncontrolledImportTask` persistence for explicit selected audit files: validates the full request before writing, rejects duplicates/stale/non-importable audit files, computes a canonical order-insensitive request hash, inserts `NAS_UNCONTROLLED_IMPORT` task and task-item snapshots without legacy task target fields, and updates audit rows with `downloadStatus=SELECTED` plus import task/item bindings.
- Boundary: this slice is service-level only; controller route, content download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.
- Diagnostic: the first GREEN attempt after implementation failed because the test captured two audit updates with two single-invocation verifies; changed the test capture to `times(2)` and reran successfully.
- Blockers: idempotency conflict/reuse, content binary download, local-write-result idempotency, archive metadata failure path, controller mapping, frontend and real E2E still need subsequent RED/GREEN slices.

## M18 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service still threw `UnsupportedOperationException` from the fail-fast M17 stub.
- FIX: first GREEN attempt failed with Mockito `TooManyActualInvocations` because the test captured two audit row updates using two one-time verifies; changed to `verify(auditFileMapper, times(2)).updateById(...)`.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.

### M19

- Status: completed
- Completed: implemented service-level import-selected idempotency hardening: same `idempotencyKey + requestHash` returns the existing task even when selected files arrive in a different order, different request hash throws a conflict before audit reads or writes, duplicate audit ids fail before hashing/writes, and the creation transaction rechecks the idempotency key with `FOR UPDATE` before inserting.
- Boundary: this slice is still service-level only; controller route, content download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.
- Diagnostic: RED reproduced the race gap because the service inserted a new task `8202` instead of returning the transaction-visible existing task `8102`; first GREEN run then exposed an obsolete Mockito insert stub after the new guard skipped insertion, and the unused stub was removed.
- Blockers: content binary download, local-write-result idempotency, archive metadata failure path, controller mapping, frontend and real E2E still need subsequent RED/GREEN slices.

## M19 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service returned newly inserted task `8202` instead of existing idempotent task `8102`.
- FIX: first M19 GREEN attempt failed with Mockito `UnnecessaryStubbingException` because `taskMapper.insert(...)` was no longer used after the transaction recheck correctly returned the existing task; removed the obsolete insert stub.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M19 task/evidence/backend files -> PASS, `UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`.
- GREEN: `git -C E:\IntRuoyi diff --check -- <M19 tracked task/backend files>` -> PASS, only Git LF-to-CRLF warnings.

### M20

- Status: completed
- Completed: implemented the import-selected controller contract for `POST /dcc/controlled-files/nas-control-audit/{taskId}/import-selected`; the endpoint requires the NAS transfer write permission combination, validates `@RequestBody DccNasUncontrolledImportSelectedReqVO`, binds `getLoginUserId()` and the path `taskId`, and delegates to `DccControlledFileNasTransferService#createUncontrolledImportTask`.
- Boundary: this slice exposes the already verified service-level import-selected creation/idempotency through the controller only; content binary download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.
- Blockers: content binary download, local-write-result idempotency, archive metadata failure path, frontend and real E2E still need subsequent RED/GREEN slices.

## M20 Verification Evidence

- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing endpoint mapping `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected`.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M20 task evidence files -> PASS, `UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`.
- GREEN: `git diff --check -- <M20 task evidence and controller files>` -> PASS, only Git LF-to-CRLF warnings.

### M21

- Status: completed
- BDD: NAS uncontrolled import content download is binary and snapshot-bound -> Given a selected `NAS_UNCONTROLLED_IMPORT` task item is bound to the current user, audit file, source signature and local relative path snapshot When the frontend requests the content endpoint Then backend returns `ResponseEntity<byte[]>` binary bytes with content disposition and `X-Source-Signature`, and does not mutate local-write/archive/controlled-file state.
- BDD: NAS uncontrolled import content rejects stale or cross-task requests -> Given the selected audit file no longer matches the import task, source signature or local relative path snapshot When content is requested Then backend fails fast before reading NAS bytes and before updating audit/task item/archive state.
- Completed: added service contract `DccControlledFileNasTransferService#readUncontrolledImportContent(...)`, service implementation with import task/audit file/task item snapshot checks, and controller contract/implementation for `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content`.
- Contract: controller requires `dcc:controlled-file:submit`, `dcc:controlled-file:directory:manage`, and `dcc:controlled-file:category:manage`; request binds path `importTaskId`, path `auditFileId`, query `sourceSignature`, and query `localRelativePath`; response is `ResponseEntity<byte[]>`, not `CommonResult`.
- Isolation note: main worktree GREEN was previously blocked by concurrent non-task DCC Maven writes to shared `target`; M21 was verified in isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803` detached at `72712e92d chore: baseline concurrent download entry updates`.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: service interface did not expose `readUncontrolledImportContent(...)`.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing `DccNasUncontrolledImportController`.
- GREEN: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M21 task evidence files -> PASS, `UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`.
- GREEN: `git -C E:\IntRuoyi diff --check -- <M21 task evidence files>` -> PASS, only Git LF-to-CRLF warnings.
- Remaining: local-write-result, formal archive, frontend static contract and real E2E remain in progress; task-owned closeout/commit remains blocked by mixed concurrent workspace state.

### M22

- Status: completed
- BDD: NAS uncontrolled import local-write-result is snapshot-bound -> Given a selected `NAS_UNCONTROLLED_IMPORT` task item belongs to the current user and matches audit file/source signature/local relative path snapshots When the browser reports `LOCAL_WRITTEN` Then backend updates audit download status and task item local-write status only, and does not read NAS content, create a controlled file, submit workflow, archive, or write an ACTIVE NAS source mapping.
- BDD: NAS uncontrolled import local-write-result terminal replay is guarded -> Given a selected file has already reached terminal local-write state When the same `LOCAL_WRITTEN` result is replayed Then backend returns current task state without mutating or archiving again; when a conflicting terminal status is submitted Then backend fails fast before mutation or archive side effects.
- Completed: added `DccNasUncontrolledImportLocalWriteResultReqVO`, controller route `POST /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/local-write-result`, service contract `recordUncontrolledImportLocalWriteResult(...)`, snapshot/state validation, `LOCAL_WRITTEN` and `LOCAL_WRITE_FAILED` state updates, idempotent success replay, and conflicting terminal rejection.
- Contract: controller requires `dcc:controlled-file:submit`, `dcc:controlled-file:directory:manage`, and `dcc:controlled-file:category:manage`; request binds path `importTaskId`, path `auditFileId`, and `@Valid @RequestBody` fields `sourceSignature`, `localRelativePath`, `localWriteStatus`, `localWriteErrorCode`, and `localWriteError`; response remains `CommonResult<DccControlledFileNasTransferRespVO>`.
- Boundary: M22 records local browser write outcome and explicitly prevents archive side effects; formal DCC archive creation, archive metadata required/blocked state, frontend static contract and real E2E remain separate milestones.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: local-write-result controller/service/VO contract was not implemented when the M22 tests were introduced.
- FIX: first M22 GREEN attempts exposed test harness issues after implementation: replay/conflict tests needed the no-op transaction manager because `recordUncontrolledImportLocalWriteResult(...)` is transactional, and obsolete stubs caused Mockito `UnnecessaryStubbingException`; tests were tightened without adding fallback behavior.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M22 task/backend files -> PASS, `UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`.
- GREEN: `git diff --check -- <M22 task evidence and backend files>` -> PASS, only Git LF-to-CRLF warnings.
- Remaining: formal archive execution, frontend static contract and real E2E remain in progress; task-owned closeout/commit remains blocked by mixed concurrent workspace state.

### M23

- Status: completed for archive metadata required blocker slice.
- BDD: Archive metadata missing after local write is visible -> Given a matched `NAS_UNCONTROLLED_IMPORT` audit file is selected and the browser has written it locally When `local-write-result` posts `LOCAL_WRITTEN` but no formal DCC archive metadata source exists Then backend keeps `downloadStatus=LOCAL_WRITTEN`, records `archiveStatus=FAILED` and `archiveErrorCode=ARCHIVE_METADATA_REQUIRED`, and does not read NAS, upload an original file, submit workflow, create a controlled file, or write an ACTIVE NAS source mapping.
- BDD: Archive metadata blocker replay is idempotent -> Given a matched file already reached `LOCAL_WRITTEN` with `ARCHIVE_METADATA_REQUIRED` When the same `LOCAL_WRITTEN` local-write-result is replayed Then backend returns the current task state without mutating audit/task item state and without repeating any archive side effects.
- Completed: added explicit `AUDIT_FILE_ARCHIVE_STATUS_FAILED` and `ARCHIVE_METADATA_REQUIRED` handling in `recordUncontrolledImportLocalWriteResult(...)`; matched files now move from local-write success into a visible archive metadata blocker instead of staying `NOT_STARTED` or fabricating legacy task metadata.
- Completed: tightened the service test harness copies for task/item/category snapshots so audit-file id, source signature, recognition snapshot, local-write status and archive status are preserved in map-backed tests.
- Boundary: this slice intentionally does not create controlled files or ACTIVE NAS source mappings because the formal archive metadata source is still undefined by design; M24 remains required for the success archive path after that source is modeled and tested.
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: existing implementation left `archiveStatus=NOT_STARTED` instead of `FAILED/ARCHIVE_METADATA_REQUIRED`.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenAndArchiveMetadataBlockWithoutSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_requiresArchiveMetadataForMatchedLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 19, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M23 task/evidence/backend files -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.
- GREEN: `git diff --check -- <M23 task evidence and backend files>` -> PASS, only Git LF-to-CRLF warnings.
- Remaining: formal archive success metadata source, controlled-file creation, ACTIVE NAS source mapping, frontend static contract and real E2E remain in progress; task-owned closeout/commit remains blocked by mixed concurrent workspace state.

### M25

- Status: completed for frontend static contract and minimal UI/API integration slice.
- BDD: Browser directory authorization gates import-selected -> Given a completed NAS uncontrolled audit task and selected matched files When the user chooses to download to a local directory Then the page obtains `showDirectoryPicker` authorization and validates each backend `expectedLocalRelativePath` before calling `/import-selected`.
- BDD: Local write success is reported only after close -> Given an import-selected task and selected audit-file snapshot When content Blob is downloaded and local writable stream closes successfully Then the page posts `LOCAL_WRITTEN` with the same source signature and local relative path snapshot.
- BDD: Local write failure remains visible -> Given the local file write fails after content download When file handle or writable stream throws Then the page posts `LOCAL_WRITE_FAILED` with explicit error code/message and displays the failure instead of claiming import success.
- BDD: Unrecognized files remain pending -> Given audit rows are `UNCLASSIFIED_PENDING` or `AMBIGUOUS` When the user opens completed audit task details Then those rows remain visible as “未分类/待处理” or “待确认” and are not selectable for automatic local import.
- BDD: Archive metadata blocker is explicit -> Given a matched file reaches local write success but backend returns `ARCHIVE_METADATA_REQUIRED` When the page reloads audit-file rows Then the page displays “归档元数据待补齐” instead of archive success.
- Completed: added static contract `dcc-nas-uncontrolled-local-import-static.spec.js` and package script `e2e:dcc:nas-uncontrolled-local-import:static`.
- Completed: extended NAS API wrapper with files page, recognize, import-selected, content binary download and local-write-result calls, using `auditFileId/sourceSignature/expectedLocalRelativePath` snapshots.
- Completed: extended NAS management page with completed-audit file table, recognition refresh, matched-row-only selection, `showDirectoryPicker`, nested local directory/file creation, Blob write/close, `LOCAL_WRITTEN` after close, `LOCAL_WRITE_FAILED` on write failure, and visible `ARCHIVE_METADATA_REQUIRED`/未分类待处理 states.
- Boundary: this frontend slice does not implement formal archive success, controlled-file creation, ACTIVE NAS source mapping, real local filesystem E2E, or fallback ZIP/default download behavior.
- RED: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> FAIL, expected reason: `package.json` lacked `e2e:dcc:nas-uncontrolled-local-import:static`, and NAS page/API lacked the local directory import contract.
- GREEN: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> PASS.
- GREEN: `pnpm e2e:dcc:nas-uncontrolled-local-import:static` -> PASS.
- GREEN: `node tests/e2e/nas-control-audit-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS, current frontend workspace type check completed successfully.
- GREEN: UTF-8/trailing whitespace check for M25 frontend files -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts IntRuoyiFronted/src/api/system/nas/index.ts IntRuoyiFronted/src/views/system/nas/index.vue IntRuoyiFronted/tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> PASS, only Git LF-to-CRLF warnings.
- Remaining: formal archive success metadata source, controlled-file creation, ACTIVE NAS source mapping and real Playwright E2E remain in progress; final closeout/commit/push remains blocked by mixed concurrent workspace state.

### M26

- Status: completed for M24 archive metadata precondition hardening.
- BDD: Formal archive metadata snapshot is required -> Given a matched audit file reaches `LOCAL_WRITTEN` but the import task item only has recognition snapshots, local relative path, `matchedProjectCodeId`, `matchedFileTypeTaxonomyId`, or candidate JSON When backend evaluates formal DCC archive Then it must keep the visible `ARCHIVE_METADATA_REQUIRED` blocker and must not read NAS bytes, upload original file, submit workflow, create a controlled file, or insert ACTIVE NAS source mapping.
- Completed: rechecked the current M24 precondition against `DccControlledFileNasTransferTaskItemDO`, `DccNasControlAuditFileDO`, `DccControlledFileSubmitReqVO`, and `recordUncontrolledImportLocalWriteResult(...)`; current schema does not persist a processing-item-level formal archive metadata snapshot for `categoryId`, `directoryId`, `dccProjectCodeId`, `fileTypeTaxonomyId`, `changeType`, `fileNumber`, `versionNo`, `effectiveDate`, and source remark.
- Completed: updated `design.md`, `docs/acceptance/bdd-scenarios.md`, `docs/acceptance/tdd-plan.md`, `task.md`, and `verification-report.md` so future M24 development must first add RED/schema/VO/service evidence for the formal metadata source before enabling successful archive creation.
- BLOCKER: M24 formal archive success path -> FAIL, missing processing-item-level formal archive metadata source; impact: controlled-file creation and ACTIVE NAS source mapping must remain blocked by `ARCHIVE_METADATA_REQUIRED` instead of using legacy task defaults, current date, empty template, or `classificationCandidatesJson`.
- Verification: document-only hardening; no production code, NAS file, local directory, database data, workflow submit, controlled-file creation, or ACTIVE NAS source mapping was modified.

### M27

- Status: completed for document consistency and implementation-readiness audit.
- BDD: Documentation markers remain searchable -> Given a future developer follows only this task package and acceptance documents When they search for the critical gates Then each core document exposes the same `ARCHIVE_METADATA_REQUIRED`, `未分类/待处理`, `showDirectoryPicker`, `LOCAL_WRITTEN`, and `NAS_UNCONTROLLED_IMPORT` markers without relying on memory from this chat.
- RED: `node -e "<doc consistency marker scan>"` -> FAIL, expected reason: `docs/acceptance/tdd-plan.md` did not contain `未分类/待处理`, and `docs/acceptance/test-data.md` did not contain `未分类/待处理`, `showDirectoryPicker`, or `NAS_UNCONTROLLED_IMPORT`.
- Completed: updated `docs/acceptance/tdd-plan.md` to state that unresolvable project code/item/category must remain official `未分类/待处理`, and updated `docs/acceptance/test-data.md` to bind local-directory samples to `showDirectoryPicker` and `NAS_UNCONTROLLED_IMPORT`.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: `node -e "<doc consistency marker scan>"` -> PASS, `DOC_CONSISTENCY_PASS files=8 markers=5`.
- GREEN: `git diff --check -- docs/acceptance/tdd-plan.md docs/acceptance/test-data.md` -> PASS, only Git LF-to-CRLF warnings.
- Boundary: documentation-only consistency hardening; no production code, NAS file, local directory, database data, workflow submit, controlled-file creation, or ACTIVE NAS source mapping was modified.
