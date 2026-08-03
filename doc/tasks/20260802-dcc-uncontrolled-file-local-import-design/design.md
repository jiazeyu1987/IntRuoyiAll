# DCC 未受控文件本地下载与自动归类设计

## 目标

在 `NAS 管理` 页的 `统计未受控文件` 能力基础上，扩展为一个可审计的处理闭环：

1. 后端继续扫描固定 NAS 根目录并识别未受控文件。
2. 页面展示未受控文件明细，用户勾选需要处理的文件。
3. 用户选择本地目标根目录后，系统将选中文件写入本地对应目录。
4. 后端按路径或名称解析 DCC 项目代码 item 与文件分类。
5. 唯一匹配时，复用正式 DCC 受控文件创建/元数据链路归档到项目代码关联文档。
6. 无法唯一匹配项目代码、item 或文件分类时，记录为 `未分类/待处理`，不得猜测、默认归类或静默跳过。

## 当前系统复用点

- `NAS 管理` 页面已有 `统计未受控文件` 按钮、任务轮询、报告下载和权限组合检查。
- 后端已有 `DccNasControlAuditServiceImpl` 扫描 `1. QMS documents`、`2.DHF`、`3.DMR`，并基于 `dcc_controlled_file_nas_source` 判断已受控、未受控、待确认和来源缺失。
- 后端已有 `DccControlledFileNasTransferServiceImpl` 从 NAS 读取文件、创建原始文件、调用 `submitControlledFileWithoutApproval` 并写入 NAS 来源映射。
- 当前 DCC/NAS 新写入已经统一使用 `dccProjectCodeId`，`productCode = DccProjectCode.projectCode`，`productName = DccProjectCode.projectName`，`productMasterId = null`。
- DCC 项目代码详情已有“未分类文件类型”和按文件名自动归类能力，正式分类候选来自 DCC 文件分类树。
- 后端已有 `dcc_file_category_match_rule` 规则表，支持 `CONTAINS / EXACT / PREFIX / SUFFIX / EXTENSION`，用于可维护的分类规则匹配。
- 项目代码识别已有文件名、目录、别名和规则识别链路；本设计复用其确定性匹配思想，但未受控文件的批处理不得调用默认目标或 AI 猜测目标。

## 术语约束

- `未受控文件`：NAS 中存在、且当前租户没有唯一 `EXACT / LEGACY_EXACT` 当前 ACTIVE 受控来源映射的文件。
- `DCC 项目代码 item`：当前系统证据中没有独立 item 表；本设计将 DCC 基础条目中的项目代码记录作为 item，即 `dcc_project_code.id`。若实施时发现已有正式 item 表，必须先补充设计和测试，不得临时映射。
- `文件分类`：DCC 文件分类树中的正式 `fileTypeTaxonomyId` 与 `fileTypeLevel1-5`，不是旧 lifecycle stage，也不是当前关联文件动态推导出来的文本。
- `未分类/待处理`：页面层正式业务状态，表示无法唯一识别项目代码 item 或文件分类；后端细分为 `UNCLASSIFIED_PENDING`（未命中）和 `AMBIGUOUS`（多候选歧义）。该状态不是 fallback，不创建默认受控文件，不写入随机项目，不写入默认分类。
- `本地对应目录`：用户通过浏览器选择的本地根目录下，按识别结果生成的相对目录。实现必须使用浏览器可审计的目录写入能力；若浏览器不支持目录写入，操作 fail fast。

## 开发前置门禁

- 实施前必须重新检索是否存在独立 DCC item 模型或 item 表；若存在，先更新本文档、BDD 和 TDD，再实现，不得把 `dcc_project_code.id` 临时映射为 item。
- 实施前必须确认当前运行环境支持浏览器目录写入能力；若不支持，只能通过本文档的 fail-fast 路径验收，不得改成 ZIP、浏览器默认下载目录、服务器暂存目录或后台写本机路径。
- 实施前必须确认 `dcc_file_category_match_rule`、DCC 文件分类树和项目代码启用数据在目标租户中可查询；缺失时正向归档路径阻塞，但待处理路径仍可验证。
- 实施前必须确认新增 audit 明细和 import 处理项均带 `tenant_id`、`audit_file_id`、`path_hash` 与文件签名快照；没有这些字段不得进入编码。
- 实施 `recognize` 前必须确认 audit 明细或等价快照表具备可持久化的识别候选摘要字段；推荐字段为 `classification_candidates_json`。若当前已落地 migration 缺少该字段，必须用追加 migration 和 schema 测试补齐，不得只把候选摘要放在内存、日志或一次性响应里。
- 实施 `import-selected` 前必须确认处理任务和处理项具备 `idempotency_key`、规范化 `request_hash`、`audit_file_id`、`source_signature`、识别结果快照、本地相对路径快照，以及 audit 明细与 import task/item 的可查询绑定关系；缺字段时必须先补 migration/DO/Mapper/Test schema。
- 若复用 `dcc_controlled_file_nas_transfer_task`，`template_category_id`、`effective_date`、任务级 `dcc_project_code_id` 或同类旧 NAS 转移输入不得成为 `NAS_UNCONTROLLED_IMPORT` 创建任务的必填字段，也不得填入 `0`、当前日期、旧页面选择项或任意默认值来绕过约束；旧 `NAS` / `LOCAL_FOLDER` 入口仍必须在服务层保持原有必填校验。
- 实施 `import-selected` 前必须确认 `NAS_UNCONTROLLED_IMPORT` 不会被现有 `processWaitingTasks()`、`processFileItem()`、旧 NAS transfer 异步轮询或同类 legacy processor 自动读取 NAS 内容、提交 DCC 或写入 NAS 来源映射；该 source type 只能由 content 下载和 `local-write-result` 后置归档驱动。
- 实施 `import-selected` 前必须明确请求原子性：任一选中明细不合法时拒绝整个请求，不创建部分 import task，不把部分 audit 明细推进到 `SELECTED`。
- 实施 `local-write-result` 前必须明确重复回写幂等：相同处理项、相同签名、相同相对路径和相同结果的重复请求返回当前状态；冲突结果必须拒绝，且 `LOCAL_WRITTEN` 不得重复触发第二次归档。
- 实施正式归档前必须明确 DCC 提交所需模板分类、生效日期、变更原因等元数据的正式来源；缺少正式来源时该文件只能进入明确阻塞状态，例如 `ARCHIVE_METADATA_REQUIRED`，不得用当前日期、历史 transfer 任务值、空模板或默认模板伪造归档成功。
- 实施前必须确认测试命令的工作目录：Maven 命令从 `IntRuoyiBackend` 或使用 `-f IntRuoyiBackend/pom.xml` 执行；SQL 静态测试放在 `IntRuoyiBackend/script/tests/`；前端静态/E2E 命令从 `IntRuoyiFronted` 或使用 `pnpm --dir IntRuoyiFronted` 执行。
- 代码检索必须限定到 DCC、NAS、项目代码、分类和验收文档相关目录，避免被无关模块的损坏 `target` 目录或并发任务产物误阻塞。
- 实施前必须确认浏览器写入方案不把文件二进制转成 JSON/base64 大字段；大文件应使用二进制响应、流式读取或明确的分块方案，防止前端内存峰值被误判为业务失败。
- 实施前必须确认本地相对路径生成规则稳定可测：目录段优先使用项目代码、分类 ID 或受控安全名，展示名称单独展示；不能把 Windows 非法字符清洗成另一个可能冲突的业务目录后继续成功。

## 状态模型

### 未受控明细

在当前 `dcc_nas_control_audit_task` 基础上新增明细表，避免只依赖 Excel 报告：

- `dcc_nas_control_audit_file.id`
- `task_id`
- `nas_share_name`
- `root_path`
- `normalized_relative_path`
- `path_hash`
- `file_name`
- `file_size`
- `modified_at`
- `source_signature`: 由 `path_hash + file_size + modified_at` 组成的处理前快照，推荐保存为 `sha256(path_hash + "|" + file_size + "|" + modified_at_utc_epoch_millis)`；任一字段缺失时不得进入下载或归档处理。
- `control_status`: `NOT_CONTROLLED`
- `classification_status`: `PENDING_RECOGNITION` / `MATCHED` / `UNCLASSIFIED_PENDING` / `AMBIGUOUS`
- `matched_project_code_id`
- `matched_file_type_taxonomy_id`
- `matched_file_type_level1-5`
- `classification_reason`
- `classification_candidates_json`: 有界 JSON，只保存候选项目代码和候选分类的脱敏摘要，例如候选 id、代码、名称、规则 id、匹配片段、分数或优先级；不得保存文件内容、NAS 凭据、本地绝对路径或其它租户数据。
- `download_status`: `NOT_SELECTED` / `SELECTED` / `CONTENT_READY` / `LOCAL_WRITTEN` / `LOCAL_WRITE_FAILED`
- `archive_status`: `NOT_STARTED` / `ARCHIVED` / `PENDING_MANUAL_REVIEW` / `FAILED`
- `selected_import_task_id`
- `selected_import_task_item_id`
- `local_relative_path`
- `local_write_error_code`
- `local_write_error`
- `archive_error_code`
- `archive_error`
- `controlled_file_id`
- 审计字段和 `tenant_id`

约束：

- `task_id + tenant_id + path_hash` 必须可索引查询；重复扫描同一路径允许生成新的 audit 明细，但同一处理任务不得重复处理同一 `audit_file_id`。
- 明细创建时 `classification_status=PENDING_RECOGNITION`、`download_status=NOT_SELECTED`、`archive_status=NOT_STARTED`。
- `classification_reason` 必须使用稳定错误码或稳定原因码，至少覆盖 `MATCHED`、`PROJECT_CODE_NOT_FOUND`、`PROJECT_CODE_AMBIGUOUS`、`FILE_CATEGORY_NOT_FOUND`、`FILE_CATEGORY_AMBIGUOUS`、`FORMAL_CATEGORY_TREE_MISSING`；前端展示文案不得作为后端判断依据。
- `classification_candidates_json` 只用于审计和人工判断，不得作为后续归档的事实来源；归档事实来源只能是 import 处理项锁定的项目代码和分类快照。
- 已创建 import task 后发生的 `LOCAL_PATH_COLLISION`、非法相对路径、浏览器写入失败都写入 `download_status=LOCAL_WRITE_FAILED` 和 `local_write_error_code`，不得把它们扩展成新的 download status 枚举。
- 浏览器不支持目录写入或用户取消目录选择发生在 import task 创建之前时，不写后端状态、不创建处理任务、不产生需要清理的 import 证据；页面只展示 fail-fast 原因。
- `path_hash` 不能设置为阻止重复扫描的唯一约束；重复扫描同一 NAS 路径应生成新的 audit 证据，重复归档由 import 处理项和已归档 audit file 状态控制。
- `selected_import_task_id/selected_import_task_item_id` 只记录当前有效 import 绑定；若采用等价的 task item 关联而不落 audit 字段，必须用 schema/mapper/service 测试证明页面查询、重复提交拦截和并发冲突都能按 `audit_file_id` 找到绑定关系。
- 同一个 `audit_file_id` 在任一未完成 import task 中已有绑定时，新的 `import-selected` 必须拒绝；若后续要支持失败后重新选择目录重试，必须新增显式 retry 入口、状态流转和 BDD/TDD，首版不得通过创建第二个活动 import task 来重试。

### 处理任务

新增轻量处理任务表或复用 `dcc_controlled_file_nas_transfer_task` 扩展 `sourceType`：

- 推荐扩展 `dcc_controlled_file_nas_transfer_task.sourceType = NAS_UNCONTROLLED_IMPORT`，复用任务轮询、失败列表和 item 处理结构。
- 若复用该表，`NAS_UNCONTROLLED_IMPORT` 的事实来源只能是 task item 的 audit/识别/本地路径快照；任务头上的 `template_category_id`、`effective_date`、`dcc_project_code_id`、`product_master_id` 或旧 NAS 转移全局选择值必须允许为空且不得参与目标项目、item、分类判断。
- 旧 NAS 转移和本地文件夹导入仍必须通过服务校验要求其业务必填字段；不能因为 import 任务需要 nullable schema 而放松原入口的正式入参校验。
- 处理项必须绑定 `audit_file_id`，防止用户选择后 NAS 文件变化或重复处理。
- 任务头必须保存 `audit_task_id`、`source_type=NAS_UNCONTROLLED_IMPORT`、`operator_user_id`、`idempotency_key`、`request_hash` 和统计计数；相同 `tenant_id + operator_user_id + idempotency_key + request_hash` 返回原任务，相同 key 但 `request_hash` 不同必须返回明确冲突，不得复用或覆盖原任务。
- `tenant_id + operator_user_id + idempotency_key + deleted` 必须有唯一约束或等价事务锁保护；若因历史数据无法立即加唯一约束，服务测试必须证明同 key 并发只产生一个任务且不同 `request_hash` 返回冲突，不能仅依赖普通索引或前端防抖。
- `request_hash` 必须由规范化请求生成：先校验 `selectionScope=EXPLICIT_IDS`、非空、无重复 `auditFileId`，再按 `auditFileId ASC` 对 `selectedFiles` 排序，使用 `auditTaskId + selectionScope + auditFileId + sourceSignature + localRelativePath` 的稳定 UTF-8 JSON 计算 SHA-256；同一选择仅提交顺序不同不得产生不同 hash。
- 每个处理项必须保存 `audit_file_id`、`source_signature`、`classification_status_snapshot`、`matched_project_code_id_snapshot`、`matched_file_type_taxonomy_id_snapshot`、`matched_file_type_level1-5_snapshot`、`classification_reason_snapshot`、`classification_candidates_json_snapshot`、`local_relative_path` 和状态字段，后续重跑不得因为分类规则变化或前端重试悄悄改变目标。
- 若复用 `dcc_controlled_file_nas_transfer_task_item`，必须以新增字段表达 `audit_file_id` 和上述快照；不得把 `source_file_id`、`nas_path` 或 `item_name` 复用成 audit 绑定事实来源。
- 处理项必须区分本地写入和 DCC 归档：`LOCAL_WRITTEN` 只说明浏览器写入成功，`ARCHIVED` 才说明正式受控文件创建成功。
- 同一租户下 `idempotency_key` 必须幂等返回原 import task；不同 key 处理已归档 audit file 必须返回冲突或已处理状态，不得创建第二个受控文件。
- 本地写入结果是浏览器端用户授权目录写入后的回执，必须记录操作者、时间、audit file、相对路径和错误码；后端不得据此保存或推断本地绝对路径。
- `local-write-result` 必须是处理项级幂等操作；重复 `LOCAL_WRITTEN` 不得重复调用归档链路，重复 `LOCAL_WRITE_FAILED` 只能返回既有失败状态，成功后再提交失败或失败后再提交成功必须按显式 retry 规则处理，首版直接返回冲突。
- `NAS_UNCONTROLLED_IMPORT` 任务创建成功后仍不得进入现有 waiting processor 的自动处理队列；任务状态若复用旧枚举，必须用 source type 分支保证旧 processor 跳过该任务，并由 content/local-write-result 驱动后续状态。

### 正式归档元数据来源门禁

- 当前代码复核结论：`dcc_nas_control_audit_file` 和 `dcc_controlled_file_nas_transfer_task_item` 只持久化识别快照、本地相对路径、本地写入状态和归档状态，未持久化可直接提交 DCC 的正式归档元数据快照。
- `matched_file_type_taxonomy_id` 不是 `DccControlledFileSubmitReqVO.categoryId`；`classification_candidates_json` 只允许作候选审计，不得作为归档事实来源；`task.template_category_id`、`task.effective_date`、`task.dcc_project_code_id` 对 `NAS_UNCONTROLLED_IMPORT` 必须允许为空且不得被当作旧默认值复用。
- 正式成功归档前必须存在处理项级、可审计、可重放校验的元数据来源，至少覆盖 `categoryId`、`directoryId`、`dccProjectCodeId`、`fileTypeTaxonomyId`、`changeType`、`fileName`、`fileNumber`、`versionNo`、`effectiveDate`、`remark/source`，以及是否需要 `processType`、`needTraining`、`sourceFileId/drawingPdfFileId` 等现有提交链路要求的字段。
- 上述字段可以来自新增归档元数据快照列、独立归档元数据表、或显式 UI/服务端配置生成的快照；但必须先有 schema/VO/service 测试证明来源明确、租户隔离、与 `auditFileId + sourceSignature + localRelativePath` 绑定，并且不会因后续分类规则或项目代码变化被静默改写。
- 若任一必需元数据缺失、字段来源无法审计、分类只能从候选 JSON 解析、目录只能从旧任务头推断，`LOCAL_WRITTEN` 后必须保持 `download_status=LOCAL_WRITTEN`，并写入 `archive_status=FAILED`、`archive_error_code=ARCHIVE_METADATA_REQUIRED`；不得读取 NAS 原件、上传原始文件、提交 workflow、创建受控文件或写 ACTIVE NAS 来源映射。
- M24 成功路径开发前必须先新增 RED：`archiveAfterLocalWritten_archivesOnlyFromFormalMetadataSnapshot`，断言只有正式元数据快照存在时才调用 `fileService.createFileAndReturnId(...)`、`submitControlledFileWithoutApproval(...)` 和 `nasSourceMapper.insert(...)`；缺失时继续通过 `ARCHIVE_METADATA_REQUIRED` 阻塞测试。

### 分类与归档矩阵

| 项目代码 item | 文件分类 | 分类状态 | 本地目录 | DCC 归档 |
| --- | --- | --- | --- | --- |
| 未执行识别 | 未执行识别 | `PENDING_RECOGNITION` | 不生成 | 不创建，必须先 recognize |
| 唯一命中 | 唯一命中 | `MATCHED` | `<项目代码>/<阶段>/<文件类型>/<原 NAS 相对路径>` | 仅在 `LOCAL_WRITTEN` 且正式归档元数据快照存在后创建受控文件；缺失时 `ARCHIVE_METADATA_REQUIRED` |
| 未命中 | 任意 | `UNCLASSIFIED_PENDING` | `_未分类待处理/<原 NAS 相对路径>` | 不创建，`archive_status=PENDING_MANUAL_REVIEW` |
| 多候选 | 任意 | `AMBIGUOUS` | `_未分类待处理/<原 NAS 相对路径>` | 不创建，展示候选和歧义原因 |
| 唯一命中 | 未命中或多候选 | `UNCLASSIFIED_PENDING` 或 `AMBIGUOUS` | `_未分类待处理/<原 NAS 相对路径>` | 不创建，等待人工处理 |

## 识别规则

### 项目代码 item 识别

优先顺序：

1. 路径片段精确命中启用项目代码或项目名称。
2. 文件名精确或边界命中启用项目代码。
3. 项目代码别名或已确认规则命中。
4. 没有命中时，标记 `UNCLASSIFIED_PENDING`，原因写明 `PROJECT_CODE_NOT_FOUND`。
5. 多个候选或同分候选时，标记 `AMBIGUOUS`，原因写明 `PROJECT_CODE_AMBIGUOUS`，并返回候选摘要供人工判断。

禁止行为：

- 禁止用第一个项目代码、当前列表选中项、当前登录人、历史 product master、空 projectCode 或默认项目补齐。
- 禁止在多个同分候选时按排序取第一。
- 禁止项目代码缺失时仍创建受控文件。

### 识别结果写入规则

- 只有项目代码 item 和文件分类均唯一命中时，才能写入 `classification_status=MATCHED`，并同时持久化 `matched_project_code_id`、`matched_file_type_taxonomy_id`、`matched_file_type_level1-5` 和 `classification_reason=MATCHED`。
- 项目代码唯一命中但文件分类未命中时，可保留 `matched_project_code_id` 作为人工判断上下文，但 `matched_file_type_taxonomy_id` 必须为空，状态为 `UNCLASSIFIED_PENDING`，原因必须为 `FILE_CATEGORY_NOT_FOUND`。
- 项目代码唯一命中但文件分类多候选时，可保留唯一项目代码上下文，但分类 id 必须为空，状态为 `AMBIGUOUS`，原因必须为 `FILE_CATEGORY_AMBIGUOUS`。
- 项目代码未命中时，项目和分类归档快照均不得补默认值，状态为 `UNCLASSIFIED_PENDING`，原因必须为 `PROJECT_CODE_NOT_FOUND`；分类候选只可作为候选摘要展示，不得推动归档。
- 项目代码多候选时，项目归档快照必须为空，状态为 `AMBIGUOUS`，原因必须为 `PROJECT_CODE_AMBIGUOUS`；不得按排序、最新创建时间或当前页面上下文取第一。
- 普通 `recognize` 只能更新 `PENDING_RECOGNITION` 明细；若产品需要“强制重新识别”，必须新增显式请求参数、权限、审计字段和 TDD，不得复用普通 recognize 静默覆盖已绑定 import 的快照。

### 文件分类识别

优先顺序：

1. `dcc_file_category_match_rule` 的启用规则按权重、匹配类型和精确度计算。
2. DCC 文件分类树阶段直接子分类的确定性名称匹配。
3. 项目代码详情当前“按文件名归类未分类”的相似度 helper 可作为前端预览提示，但后端写入必须以正式规则或唯一候选验证为准。
4. 没有命中时，标记 `UNCLASSIFIED_PENDING`，原因写明 `FILE_CATEGORY_NOT_FOUND`。
5. 多个候选或同分候选时，标记 `AMBIGUOUS`，原因写明 `FILE_CATEGORY_AMBIGUOUS`，并返回候选摘要供人工判断。

禁止行为：

- 禁止归入 `未分类文件类型` 后宣称分类完成。
- 禁止缺正式分类树时用文件扩展名硬编码分类。
- 禁止只改前端三栏展示而不持久化 `fileTypeTaxonomyId`。

## 本地下载目录策略

- 页面在用户点击“下载并归类”时调用 `window.showDirectoryPicker()` 获取用户授权的本地根目录。
- 成功识别的文件写入：`<项目代码安全目录>/<文件分类阶段安全目录>/<文件类型安全目录>/<原 NAS 相对路径文件名>`。
- 未分类/待处理文件写入：`_未分类待处理/<原 NAS 相对路径>`，并在页面明细中显示待处理原因。
- 本地绝对路径不得传给后端、不得落库；后端只记录相对路径和用户确认结果。
- 本地相对路径必须复用当前本地文件夹导入的相对路径校验思想：统一 `/` 分隔、禁止盘符、禁止 `..`、禁止空段、禁止 Windows 保留名和非法字符。
- 安全目录段必须可追溯到正式业务对象，建议包含稳定 ID 或项目代码，例如 `<projectCode>__<projectCodeId>`、`<taxonomyId>__<safeName>`；展示名称可以使用原中文名，但写入路径必须经过同一校验器验证。
- 若两个待写入文件规范化后产生同一 `local_relative_path`，或目标文件在本地根目录下已存在，当前文件必须标记 `download_status=LOCAL_WRITE_FAILED`、`local_write_error_code=LOCAL_PATH_COLLISION` 并阻塞写入，不得覆盖已有文件、自动改名或取第一条。
- 若生成路径超过浏览器或 Windows 可写路径限制，当前文件必须标记 `download_status=LOCAL_WRITE_FAILED`、`local_write_error_code=LOCAL_PATH_TOO_LONG`，不得截断路径后继续成功。
- 本地路径校验分两阶段：目录授权后、`import-selected` 前的前端预检失败不得创建 import task、不得回写后端状态；import task 创建后因目标文件被并发创建、句柄写入失败或最终 `close()` 失败，才通过 `local-write-result` 回写 `LOCAL_WRITE_FAILED`。
- 后端必须根据 audit 明细识别快照重新生成期望 `local_relative_path` 并与前端提交值逐字节比较；不匹配时拒绝 `import-selected`，不得相信前端传入的项目代码、分类或本地目录片段。
- 首版处理范围仅支持显式 `auditFileId` 列表。若要支持“处理当前筛选条件下全部文件”，必须新增服务端 selection snapshot/token 及对应 BDD/TDD，不得把当前页勾选误报为全量处理。
- 若浏览器不支持 File System Access API 或用户取消目录选择，页面必须显示真实失败原因，且不得创建 import task 或回写下载状态。
- 若 import task 创建后本地写入失败，前端必须回写 `LOCAL_WRITE_FAILED` 和真实失败原因，后端不得改成已下载。
- 本地写入成功不等同于 DCC 归档成功；两者状态分别记录。
- 用户选择“不下载/仅查看”时，只保留 audit 明细和识别预览，不创建 import 任务、不读取文件内容、不写本地目录、不创建受控文件。

## 处理时序

1. `start` 只做扫描和明细持久化，不创建受控文件。
2. `recognize` 只做确定性项目代码 item 和文件分类预识别，保存识别快照，不读取文件内容。
3. 用户选择“不下载/仅查看”时流程结束，只展示 audit 明细、识别预览和报告下载入口。
4. 用户确认下载时，前端先检查 `window.showDirectoryPicker()` 能力并获取本地根目录授权；用户取消或浏览器不支持时不得创建 import task。
5. 前端基于后端返回的相对路径预览做本地相对路径校验、规范化冲突校验和目标文件存在性检查；任一阻塞必须显示明细，不得覆盖、自动改名或继续归档。
6. 本地目录授权和相对路径校验通过后，`import-selected` 创建处理任务并锁定 `auditFileIds + source_signature + recognition snapshot + local_relative_path + idempotencyKey`。
   - `import-selected` 必须先完成全量校验再写库；空选择、重复 id、跨任务/租户、签名不匹配、未识别、已归档、已有活动 import 绑定、相对路径不匹配或幂等冲突中任一项存在时，整个请求失败且 audit 明细状态保持不变。
   - `import-selected` 只创建可审计任务和快照，不读取 NAS 文件内容、不调用旧 NAS transfer processor、不调用 `submitControlledFileWithoutApproval`、不写入 `dcc_controlled_file_nas_source`。
7. 前端逐个调用 `content` 下载文件内容；后端每次读取前必须复核 `path_hash + file_size + modified_at + source_signature`，通过后才可将对应处理项推进到 `CONTENT_READY`，且响应必须使用二进制流或明确分块，不得把文件内容包装进 JSON/base64。
8. 前端先写入本地目录；写入失败时回写 `LOCAL_WRITE_FAILED`，匹配文件也不得进入 DCC 归档。
9. 只有 `classification_status=MATCHED` 且 `local-write-result=LOCAL_WRITTEN` 的文件，后端才调用正式 DCC 归档链路。
10. `UNCLASSIFIED_PENDING` 和 `AMBIGUOUS` 文件即使本地写入成功，也只进入 `PENDING_MANUAL_REVIEW`，不创建受控文件。
11. DCC 归档失败时保留 `LOCAL_WRITTEN`，同时写入 `archive_status=FAILED`、`archive_error_code` 和失败原因，页面必须分别展示本地成功与归档失败。

## 状态流转约束

- audit 明细只能从 `PENDING_RECOGNITION` 流转到 `MATCHED`、`UNCLASSIFIED_PENDING` 或 `AMBIGUOUS`；已经进入 import task、`LOCAL_WRITTEN`、`PENDING_MANUAL_REVIEW`、`ARCHIVED` 或 `FAILED` 的明细不得被普通 recognize 请求静默重写。
- `download_status` 只能按 `NOT_SELECTED -> SELECTED -> CONTENT_READY -> LOCAL_WRITTEN` 或 `NOT_SELECTED/SELECTED/CONTENT_READY -> LOCAL_WRITE_FAILED` 流转；不得从失败状态自动改回成功。
- `archive_status=ARCHIVED` 是终态；后续重复请求只能返回原任务、已处理或冲突，不得回退为 `NOT_STARTED` 或重新归档。
- `UNCLASSIFIED_PENDING` 和 `AMBIGUOUS` 的 `archive_status` 应进入 `PENDING_MANUAL_REVIEW`，除非用户后续通过正式人工处理入口重新识别并发起新的归档证据。
- 并发处理同一 `audit_file_id` 时，后端必须在事务内检查当前状态、幂等键和已归档标记；并发冲突必须返回明确错误，不得依赖前端按钮禁用作为唯一保护。
- `import-selected` 必须以事务包住任务头、任务项和 audit 明细状态更新；不得先写入部分 `SELECTED` 再因后续选中文件失败而留下半成品状态。
- 事务内必须按稳定顺序锁定选中 audit 明细，例如按 `auditFileId ASC`；更新 audit 明细 import 绑定时必须带上当前 `download_status`、`archive_status`、`selected_import_task_id IS NULL` 或等价条件，并校验影响行数等于选中数量，防止并发双任务各自认为成功。

## 接口设计

接口路径以下均为后端 Controller 业务路径，前端实际请求需带当前系统统一的 `/admin-api` 前缀。

### 统计并持久化明细

- `POST /dcc/controlled-files/nas-control-audit/start`
  - 保持现有入口。
  - 扫描时除了 Excel 写入，还要插入 `dcc_nas_control_audit_file` 明细。

- `GET /dcc/controlled-files/nas-control-audit/{taskId}/files`
  - 查询未受控文件明细，必须按当前租户和 `taskId` 限定，支持 `pageNo/pageSize`、`keyword`、`classificationStatus`、`downloadStatus`、`archiveStatus` 过滤。
  - 默认排序为 `id ASC` 或稳定的扫描顺序；不得因分页或重新识别导致同一请求返回顺序漂移。
  - 返回字段至少包含 `auditFileId`、`nasShareName`、`normalizedRelativePath`、`fileName`、`fileSize`、`modifiedAt`、`sourceSignature`、识别状态、下载状态、归档状态、匹配项目代码 item、匹配分类、待处理原因、候选摘要、错误码、识别后的 `expectedLocalRelativePath` 和只含相对路径的已锁定 `localRelativePath`。
  - 响应不得包含文件内容、本地绝对路径、NAS 服务器凭据或其它租户数据。
  - `taskId` 不存在、非当前租户、用户无权访问或任务不是 NAS control audit 类型时必须返回明确错误，不得返回空页冒充成功。

### 预识别与处理

- `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`
  - 对当前任务未受控明细执行确定性预识别。
  - 不读取文件内容、不创建受控文件、不写本地目录。
  - 返回 `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS` 统计，并返回或刷新每条明细的候选摘要和 `expectedLocalRelativePath` 预览。
  - 重复调用只更新仍为 `PENDING_RECOGNITION` 或用户明确要求重新识别的明细；已经绑定 import task 或已归档的明细不得被新规则静默改写。
  - 识别完成后必须持久化 `classification_reason`、候选摘要和识别快照；后续 import task 只能使用该快照，不能在处理过程中临时重算目标。

- `POST /dcc/controlled-files/nas-control-audit/{taskId}/import-selected`
  - 其中 `{taskId}` 明确为 `auditTaskId`。
  - 入参：`selectionScope=EXPLICIT_IDS`、`selectedFiles[{auditFileId, sourceSignature, localRelativePath}]`、`idempotencyKey`。不下载/仅查看不调用该接口。
  - 入参不得包含本地绝对路径、任务级项目代码、任务级分类、`templateCategoryId`、`effectiveDate` 或旧 NAS transfer 的全局选择字段；若正式归档需要这些元数据，必须通过另一个明确的归档元数据设计和 TDD 提供来源。
  - 后端按当前任务、选中明细、签名和本地相对路径生成规范化 `request_hash`；相同幂等键但请求哈希不同必须返回冲突；仅 `selectedFiles` 顺序不同必须返回原任务。
  - 后端只接受已完成统计任务的未受控明细。
  - 后端重新校验每个 `localRelativePath`，不接收本地绝对路径。
  - 后端必须重新生成每个文件的期望本地相对路径并要求与入参完全一致；若前端篡改项目代码、分类、目录层级或 `_未分类待处理` 前缀，必须拒绝整个请求或拒绝对应文件并记录明确错误。
  - 后端必须拒绝空选择、重复 `auditFileId`、非当前任务/租户明细、`sourceSignature` 不匹配、仍为 `PENDING_RECOGNITION` 的明细、已绑定其它 import task 的明细，以及已 `ARCHIVED` 的明细。
  - 首版必须采用整体原子语义：上述任一校验失败时整个请求失败，不创建任务头/任务项，不改变任何选中 audit 明细的 `download_status` 或 import 绑定。
  - import task 创建成功时将对应明细推进到 `download_status=SELECTED` 并写入 import 绑定；不允许在创建任务时直接标记 `CONTENT_READY`、`LOCAL_WRITTEN` 或 `ARCHIVED`。
  - 错误码至少覆盖 `EMPTY_SELECTION`、`DUPLICATE_AUDIT_FILE_ID`、`INVALID_SELECTION_SCOPE`、`AUDIT_FILE_NOT_IN_TASK`、`SOURCE_SIGNATURE_MISMATCH`、`PENDING_RECOGNITION`、`AUDIT_FILE_ALREADY_BOUND`、`AUDIT_FILE_ALREADY_ARCHIVED`、`LOCAL_RELATIVE_PATH_MISMATCH`、`IDEMPOTENCY_REQUEST_HASH_CONFLICT`。
  - 创建 `NAS_UNCONTROLLED_IMPORT` 处理任务。

- `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}`
  - 复用 NAS 转移任务响应结构，增加 matched/pending/archived/download status 统计。

- `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content`
  - 后端按 audit 明细读取 NAS 文件并返回二进制。
  - 必须校验 path hash、NAS share、文件大小或 modified time；发生变化时 fail fast，要求重新统计。
  - 必须校验当前登录用户、租户、import task、audit file 绑定关系和文件当前状态；不能让用户通过猜测 `auditFileId` 下载其它租户或其它任务的 NAS 文件。
  - 大文件必须使用 `application/octet-stream`、流式响应或明确分块协议；禁止把文件内容放入 JSON 字段。
  - 非分块下载响应必须至少返回 `Content-Type: application/octet-stream`、安全的 `Content-Disposition` 文件名和 `X-Source-Signature`；分块协议必须有明确 chunk id、总大小或结束信号，并同样禁止 CommonResult/JSON/base64 文件内容。
  - `source_signature` 不一致、audit file 未处于当前 import task、状态不允许下载或文件已归档时必须返回明确业务错误；不得返回 200 空文件或 JSON 成功壳。
  - 若 NAS 文件变化，记录 `NAS_SOURCE_CHANGED`，阻断本地写入和归档；同任务其它文件可继续处理。

- `POST /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/local-write-result`
  - 前端本地写入后回写结果：`LOCAL_WRITTEN` 或 `LOCAL_WRITE_FAILED`。
  - 只记录相对路径、错误码和错误原因，不接收本地绝对路径。
  - 回写必须校验当前用户、租户、import task、audit file、`sourceSignature` 和 `localRelativePath` 均与处理项快照一致。
  - `LOCAL_WRITE_FAILED` 必须携带错误码；`LOCAL_WRITTEN` 不得携带失败错误码或本地绝对路径。
  - 相同处理项、相同 `sourceSignature`、相同 `localRelativePath` 和相同结果的重复回写必须幂等返回当前处理项状态；若已有结果与新结果冲突，必须返回明确冲突，不得覆盖。
  - `LOCAL_WRITTEN` 只能触发一次正式归档；重复回写、浏览器重试或网络重放不得创建第二个受控文件或第二条 ACTIVE NAS 来源映射。
  - 当文件 `MATCHED` 且结果为 `LOCAL_WRITTEN` 时触发正式归档；当结果为 `LOCAL_WRITE_FAILED`、`UNCLASSIFIED_PENDING` 或 `AMBIGUOUS` 时不得触发归档。

## 页面流程

1. 用户点击 `统计未受控文件`。
2. 页面保留现有确认框，说明会扫描固定三个根目录。
3. 扫描完成后页面显示明细表，不再只自动下载 Excel。
4. 用户筛选、勾选未受控文件。
5. 用户点击 `下载并归类`。
6. 页面显示分类预览：已匹配、未分类/待处理、歧义。
7. 用户确认下载后选择本地根目录；若取消、浏览器不支持或相对路径冲突，则不创建 import task。
8. 页面逐个下载并写入本地目录；每个文件本地写入成功后再回写结果。
9. 后端仅对 `MATCHED + LOCAL_WRITTEN` 文件创建或更新 DCC 受控文件归档任务。
10. 处理完成后，页面展示：
   - 本地写入成功数量
   - 已归档到项目代码 item 和文件分类数量
   - 未分类/待处理数量及原因
   - 失败数量及可导出的失败明细

## 数据一致性要求

- 统计结果与处理任务必须同租户、同 NAS share、同 path hash。
- 同一个 audit file 已归档后，重复提交同一个 `idempotencyKey` 返回原任务；不同 key 不能重复创建受控文件。
- 同一个 audit file 在任何任务中已经 `ARCHIVED` 后，新请求必须返回已处理状态或明确冲突，不得创建第二个受控文件。
- NAS 文件在统计后发生路径、大小或修改时间变化时，当前文件处理失败并要求重新统计。
- 已存在当前 ACTIVE 受控来源映射的文件不能再次作为未受控文件处理。
- `source_signature` 与读取时快照不一致时，必须设置 `NAS_SOURCE_CHANGED`，不得继续本地写入或归档。
- 本地写入状态、归档状态和识别状态必须分别计数；任何一个失败不得把整个任务显示为默认成功。
- 部分文件失败不影响同任务其它文件完成，但失败必须可见并可重试；不得整体默认成功。
- 已创建 import task 的处理项必须固定识别快照；后续分类规则、项目代码别名或文件分类树变化不得自动改变该任务的目标，只能重新统计/重新识别生成新的 audit/import 证据。
- import task 的规范化请求哈希必须对选择顺序不敏感、对选择内容敏感；重复 id 必须在 hash 前被拒绝，不能通过排序去重后静默成功。
- 任一 `import-selected` 校验失败都不得留下半创建任务、半更新 audit 明细或部分 `SELECTED` 状态。
- local-write-result 和后置归档必须具备处理项级幂等保护；同一 audit file 不得因为浏览器重试、本地回写重放或后端归档重试创建重复受控文件。
- 任何 `NAS_UNCONTROLLED_IMPORT` 处理项在 `LOCAL_WRITTEN` 前都不得触发 DCC submit；归档所需元数据缺失时必须可见阻塞或失败，不得通过旧任务头默认值绕过。

## 权限要求

- 页面展示统计按钮继续要求 `infra:nas:query` 和 `dcc:controlled-file:query`。
- 明细查询和确定性预识别接口使用 `dcc:controlled-file:query`，且只能修改当前 audit task 的识别快照，不得创建受控文件或读取文件内容。
- 选择处理和创建归档必须要求 `dcc:controlled-file:create` 或当前 NAS 转移链路等价权限。
- content 下载、local-write-result 回写和处理任务查询必须绑定当前用户、租户、import task 和 audit file；具备创建权限但不是该 import task 操作者时，不得读取文件内容或回写本地结果，除非另有明确管理员接管权限和审计设计。
- 自动写入项目代码元数据必须要求当前用户具备正式文控角色或既有元数据更新权限。
- 非授权用户可下载报告但不得发起处理任务。

## 不做范围

- 不新增 AI 猜测项目或分类。
- 不把浏览器不支持目录写入降级成 ZIP。
- 不直接 SQL 修 `dcc_controlled_file` 分类字段。
- 不把缺失项目代码、缺失分类树、权限不足或 NAS 文件变化处理成成功。
