# DCC 双文件上传开发设计

## 1. 设计目标

本设计补充主流程 M04、M10、M11、M17、M19、M20、M30 的上传与浏览合同。结论是：普通受控文件上传时必须提交一份不可编辑的在线浏览版，第一版定义为真实PDF；可编辑源文件可提交但不是必填。日常在线预览、审批预览、目录默认阅读和历史预览均使用不可编辑版本或由其派生的受控副本，不使用可编辑源文件。

这不是自动转换方案。系统不把 Word、Excel、图纸或其他源文件自动转成PDF；如果用户只上传可编辑源文件而没有浏览版PDF，提交必须失败。没有可编辑源文件时，系统也不能把浏览版PDF伪装成源文件。

## 2. 角色与命名

| 业务角色 | 必填 | 第一版格式 | 存储职责 | 浏览职责 |
|---|---|---|---|---|
| 可编辑版本 | 否 | `doc`、`docx`、`xls`、`xlsx`、`dwg`、`sldprt`、`sldasm`、`slddrw` | 保存原始可编辑源文件，仅授权用户下载 | 不参与日常在线浏览 |
| 不可编辑版本 | 是 | 真实PDF，服务端检查文件头及PDF可读性 | 保存本内容快照的浏览版原件 | 工作流中默认预览来源 |
| 受控浏览副本 | 生效后必有 | 由不可编辑版本盖章或派生出的PDF | 保存已受控、已盖章、可审计副本 | 受控后默认在线浏览来源 |

代码命名建议采用 `editableSource` 和 `readOnlyView` 两个业务词。历史字段 `sourceFileId` 可继续承载可编辑源文件，但新流程必须允许为空；新增 `readOnlyFileId` 保存必填浏览版PDF。`originalFileId` 不再作为新流程的业务含义入口，保留给历史数据读取和迁移识别。

## 3. 数据设计

### 3.1 快照字段

`dcc_controlled_file` 增加 `read_only_file_id`，对应DO字段 `readOnlyFileId`。该字段是新流程提交和内容变更时的强制字段，指向用户上传的不可编辑浏览版PDF。`source_file_id` 变为可空，只有上传了可编辑源文件才保存。

`published_file_id` 和 `stamped_file_id` 继续表示已生效后的受控派生件。普通在线浏览统一走一个解析方法：

| 内容状态 | 解析来源 |
|---|---|
| 审批中、驳回待修改、撤回待修改、历史未受控轮次 | `readOnlyFileId` |
| 受控、失效、作废且受控副本存在 | 优先 `stampedFileId`，再按现有发布件策略取 `publishedFileId` |
| 受控副本缺失 | 返回明确缺失错误，不回退到 `sourceFileId` 或 `originalFileId` |

`readOnlyFileId` 与 `sourceFileId` 可以指向不同文件，哈希也分别记录。若当前只保留一个 `sourceSha256`，后续实现应拆分为 `editableSourceSha256` 和 `readOnlySha256`，至少在签名证据中绑定 `readOnlySha256`。

### 3.2 历史边界

历史行没有 `readOnlyFileId` 时，查询层只能按显式迁移规则识别，不允许在普通读取时用源文件自动补齐。存在歧义的历史文件保留只读历史，新的内容变更必须补齐浏览版PDF后才能进入新流程。

## 4. 上传与提交接口

### 4.1 上传预览用途

`DccControlledFileUploadPreviewReqVO.purpose` 增加两个新用途：

| purpose | 文件规则 | 预览响应 |
|---|---|---|
| `EDITABLE_SOURCE` | 可选源文件；禁止PDF作为“可编辑版本”；只校验扩展名、大小、租户和临时票据 | 返回文件名、大小、ticket，不返回可编辑在线预览作为主浏览 |
| `READ_ONLY_VIEW` | 必填真实PDF；服务端校验PDF文件头、可读页数和大小策略 | 返回PDF预览信息、水印、ticket |

旧 `SOURCE`、`DRAWING_PDF` 仅用于历史流程或未迁移入口。新普通上传页面不能再把 `SOURCE` 同时当作源文件和浏览文件；`DRAWING_PDF` 也不能替代通用浏览版PDF。

### 4.2 提交请求

新提交合同：

| 字段 | 必填 | 说明 |
|---|---|---|
| `readOnlyUploadTicket` | 是 | 来自 `READ_ONLY_VIEW` 上传；缺失或不是PDF直接拒绝 |
| `editableUploadTicket` | 否 | 来自 `EDITABLE_SOURCE` 上传；缺失表示本快照没有可编辑版本 |
| `readOnlyFileName` | 否 | 由服务端按ticket解析后回填展示，不信任客户端决定 |
| `editableSourceFileName` | 否 | 仅当上传可编辑源文件时保存 |

新流程应停止在前端发送 `originalUploadTicket = sourceUploadTicket = 同一文件ticket`。后端不做“如果没有 `readOnlyUploadTicket` 就拿 `sourceUploadTicket` 顶替”的兼容逻辑；缺失浏览版PDF就是业务错误。

建议错误码：

| 错误码 | 场景 |
|---|---|
| `CONTROLLED_FILE_READ_ONLY_UPLOAD_REQUIRED` | 提交时缺少不可编辑浏览版 |
| `CONTROLLED_FILE_READ_ONLY_FILE_INVALID` | 浏览版不是可读真实PDF |
| `CONTROLLED_FILE_EDITABLE_FILE_INVALID` | 可编辑版本格式不在白名单或被上传为PDF |
| `CONTROLLED_FILE_EDITABLE_FILE_NOT_FOUND` | 用户请求下载可编辑版本，但该快照未上传 |
| `CONTROLLED_FILE_PREVIEW_ARTIFACT_MISSING` | 应浏览的不可编辑或受控派生件缺失 |

## 5. 工作流与浏览规则

### 5.1 首次上传与重提

首次上传、驳回后重提、撤回后重提都使用同一合同：`readOnlyUploadTicket` 必填，`editableUploadTicket` 可选。重提时只修改资料而不换正文，也必须生成明确的新内容快照；如果沿用旧PDF，应以文件服务复制或封存旧 `readOnlyFileId`，不得原地改旧行。

### 5.2 小版本与大版本检入

产生新内容快照的检入同样必须有新的不可编辑浏览版PDF。可编辑源文件可选；未上传时新版本没有源文件下载入口。小版本自动盖章、大版本审批生效均从 `readOnlyFileId` 生成受控浏览副本，不读取可编辑源文件。

### 5.3 审批签名与盖章

审批、批注、签名证据绑定的是本轮 `readOnlyFileId`、PDF哈希、业务版本和内容修订序号。可编辑源文件只作为受控资料留存或授权下载对象，不能作为审批正文，也不能用于生成日常浏览内容。

最后一名会签人批准后，`DccPdfStampService` 的输入改为 `readOnlyFileId`。若盖章成功但事务切换失败，保留失败状态和重试能力；若盖章输入缺失，明确失败，不能使用 `sourceFileId` 补救。

## 6. 查询、预览和下载

后端查询层增加统一方法，例如 `resolveViewFileId(controlledFile, accessType)`，只返回不可编辑浏览链路的文件ID。现有 `resolveBinaryFileId`、`resolvePreviewArtifactProjection` 需要调整为：

1. 普通预览、审批预览、目录浏览、历史浏览都调用浏览链路。
2. 可编辑源文件下载使用单独接口或 `role=EDITABLE_SOURCE` 的显式下载合同，并做独立权限校验。
3. 缺少可编辑源文件时返回 `CONTROLLED_FILE_EDITABLE_FILE_NOT_FOUND`，页面显示“未上传可编辑版本”。
4. 缺少浏览版或受控派生件时返回 `CONTROLLED_FILE_PREVIEW_ARTIFACT_MISSING`，页面显示不可预览原因，不显示源文件。

普通 `download` 若当前语义是下载受控内容，应继续下载浏览链路副本；不要改成源文件下载。源文件下载必须有单独按钮、单独权限和单独审计。

## 7. 前端交互

上传页拆成两个上传槽位：

| 槽位 | 文案 | 必填 | accept | 预览 |
|---|---|---|---|---|
| 在线浏览版PDF | `不可编辑版本（必填）` | 是 | `.pdf` | 上传成功后立即显示PDF预览 |
| 可编辑源文件 | `可编辑版本（可选）` | 否 | `.doc,.docx,.xls,.xlsx,.dwg,.sldprt,.sldasm,.slddrw` | 只显示文件名、大小、状态和删除 |

提交按钮只受浏览版PDF是否有效控制。可编辑源文件上传失败只阻止提交对应源文件；用户可删除源文件后继续提交。页面不再写“图纸源文件需同步上传PDF”作为唯一规则，而是统一提示所有新内容都必须有浏览版PDF。

详情页和目录页默认打开不可编辑浏览版。存在可编辑源文件且用户有权限时显示“下载可编辑版本”；没有时显示只读状态，不显示空按钮。审批页、批注页和签名页只能基于浏览版PDF操作。

## 8. TDD与验收场景

### BDD-D01 仅上传浏览版

- Given：用户有上传权限，未选择可编辑源文件，已上传真实PDF作为不可编辑版本。
- When：提交新受控文件并进入审批预览。
- Then：提交成功，快照 `readOnlyFileId` 非空、`sourceFileId` 为空；审批和普通预览均显示该PDF；源文件下载入口显示未上传。

### BDD-D02 同时上传两种版本

- Given：用户上传 `规程.docx` 作为可编辑源文件，并上传 `规程.pdf` 作为不可编辑版本。
- When：提交并完成受控。
- Then：两份文件分别保存，签名和盖章基于PDF；普通用户在线浏览受控PDF，授权用户可单独下载docx。

### BDD-D03 缺少浏览版

- Given：用户只上传可编辑源文件。
- When：点击提交。
- Then：前端阻止提交；直接调用后端也返回 `CONTROLLED_FILE_READ_ONLY_UPLOAD_REQUIRED`，不创建Master、快照、BPM或名称占用。

### BDD-D04 浏览版不是有效PDF

- Given：用户把非PDF内容改名为 `.pdf` 上传到不可编辑槽位。
- When：上传预览或提交。
- Then：服务端返回 `CONTROLLED_FILE_READ_ONLY_FILE_INVALID`；页面显示具体错误，不保存临时票据为可提交状态。

### BDD-D05 可编辑槽位上传PDF

- Given：用户把PDF上传到可编辑源文件槽位。
- When：前端校验或后端 `EDITABLE_SOURCE` 校验。
- Then：返回 `CONTROLLED_FILE_EDITABLE_FILE_INVALID`；不能自动把这份PDF挪到不可编辑槽位。

### BDD-D06 检入新版本

- Given：A/1已受控，用户检入A/2。
- When：用户只上传新的浏览版PDF，未上传可编辑源文件。
- Then：A/2可按小版本或大版本规则继续；A/2日常浏览使用新PDF或其盖章副本，A/1历史仍使用A/1原浏览版。

### BDD-D07 浏览派生件缺失

- Given：快照有可编辑源文件，但 `readOnlyFileId` 或生效后的 `stampedFileId` 缺失。
- When：用户打开在线预览。
- Then：返回明确缺失错误，不回退展示可编辑源文件或历史 `originalFileId`。

### BDD-D08 授权源文件下载

- Given：一个快照上传了可编辑源文件，另一个快照未上传。
- When：有源文件权限的用户分别点击下载可编辑版本。
- Then：前者下载源文件并记录审计；后者返回 `CONTROLLED_FILE_EDITABLE_FILE_NOT_FOUND`。无权限用户不能看到或调用成功。

### BDD-D09 历史数据接入

- Given：旧数据只有 `sourceFileId` 或 `originalFileId`，没有 `readOnlyFileId`。
- When：用户尝试对旧文件发起新的内容变更。
- Then：系统要求补齐新的不可编辑浏览版PDF；不能按旧字段猜测并自动进入新流程。

### 计划测试包

| 包 | 目标 |
|---|---|
| D1 后端上传策略 | 验证 `EDITABLE_SOURCE` 和 `READ_ONLY_VIEW` 的扩展名、PDF真伪、大小策略和错误码 |
| D2 提交服务 | 验证缺少浏览版不写Master/快照/BPM，源文件可空且不被PDF替代 |
| D3 预览解析 | 验证审批、受控、历史预览只走不可编辑链路，缺失时不fallback |
| D4 签名盖章 | 验证签名哈希和盖章输入绑定 `readOnlyFileId` |
| D5 前端上传表单 | 验证两个槽位、按钮状态、payload字段和错误提示 |
| D6 源文件下载 | 验证权限、缺失提示和审计，不影响普通浏览下载 |

## 9. 代码改造锚点

| 路径 | 当前锚点 | 改造点 |
|---|---|---|
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java` | `originalUploadTicket`、`sourceUploadTicket`、`drawingPdfUploadTicket` | 新增 `readOnlyUploadTicket`、`editableUploadTicket`，新流程不再混用同一ticket |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileUploadPreviewReqVO.java` | `purpose` | 增加 `EDITABLE_SOURCE`、`READ_ONLY_VIEW` 并更新校验说明 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java` | `sourceFileId`、`originalFileId`、`publishedFileId`、`stampedFileId` | 新增 `readOnlyFileId`；`sourceFileId` 可空且只表示可编辑源文件 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java` | `prepareSubmitContext`、提交文件解析 | 校验浏览版必填、源文件可选，保存两条文件角色 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java` | `finalizeOrdinaryApproval`、`activateRevision` | 盖章输入改为浏览版PDF，不从源文件读取 |
| `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` | `resolveBinaryFileId`、预览投影解析 | 默认浏览只走不可编辑链路；源文件下载单独解析 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts` | `EDITABLE_SOURCE_EXTENSIONS`、`buildSubmitPayload` | 移除PDF源文件白名单，构造 `readOnlyUploadTicket` 和可选 `editableUploadTicket` |
| `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` | `previewUpload`、`drawingPdfUpload` | 拆分为浏览版PDF必填槽和可编辑源文件可选槽 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` | `checkinUpload`、`drawingPdfUploadTicket` | 检入也使用浏览版PDF必填、源文件可选的双文件合同 |
| `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` | 详情预览和下载入口 | 默认打开不可编辑版本，源文件下载按钮按权限和存在性展示 |
