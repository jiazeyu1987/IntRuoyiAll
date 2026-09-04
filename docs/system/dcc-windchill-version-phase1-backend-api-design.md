# DCC 第一阶段 Windchill 版本后端 API 设计

## Purpose and Scope

本文定义 DCC 第一阶段的后端模块边界、API 合同、错误模型、事务和幂等策略。设计覆盖创建逻辑文件及 A/1、检出、检入、撤销检出、从指定小版本创建下一大版本、提交最新工作小版本、版本历史和只读迁移预检。

现有审批任务处理、电子签名、文控发布、预览、下载、培训和分发接口继续承担原职责，但它们必须绑定明确的 Iteration ID。旧的“一次请求同时建档并提交审批”和“无请求体检入只释放锁”接口在切换后退出正式写路径，不保留兼容执行分支。

## Evidence Reviewed

- 第一阶段 PRD、用户流程、验收标准和数据模型设计。
- 当前 `DccControlledFileController`、`DccControlledFileWorkflowServiceImpl`、`DccControlledFileQueryServiceImpl` 和 `DccControlledFileFinalizationServiceImpl`。
- 当前 `DccUploadTicketService`、源文件独占服务、路线就绪服务、签名服务和受控内容生命周期核心。
- 当前 `/dcc/controlled-files/submit`、`/{id}/checkout`、`/{id}/checkin`、审批任务和发布接口。
- 项目后端、数据库、无 fallback 和任务验证规则。

## Modules

### `DccControlledFileIdentityService`

- 规范化文件编码。
- 校验 DCC 项目有效、分类节点为有效叶子。
- 按复合身份创建或查询 Master。
- 捕获数据库唯一键冲突并返回确定的身份冲突错误。

### `DccProjectAccessService`

- 从 `dcc_project_access_rule` 解析 USER、DEPT、ROLE、POSITION 当前生效规则。
- 将 OWNER、EDIT、VIEW 归一为包含关系明确的项目能力。
- 与全局接口权限、类别动作和目录/正文权限做交集判断。
- 无生效项目规则时默认拒绝，不读取项目负责人文本或项目修正任务补齐。

### `DccControlledFileRevisionService`

- 创建初始 Revision A 和 Iteration A/1。
- 从指定历史小版本创建下一 Revision `/1`。
- 按 Excel 式字母进位算法分配 revision sequence、revision code 和 iteration number。
- 校验单开放 Revision、来源归属和非当前正式来源理由。

### `DccControlledFileCheckoutService`

- 创建 ACTIVE 检出记录。
- 校验最新工作小版本、检出人和项目/动作权限。
- 检入时绑定 SOURCE 上传凭证、创建下一 Iteration、终结检出记录。
- 撤销检出时终结检出记录且不创建 Iteration。

### `DccControlledFileApprovalBindingService`

- 校验只能提交最新工作小版本。
- 冻结 Iteration、源文件哈希、元数据、关联文件和路线快照。
- 创建平台开放候选并启动现有 BPM 流程。
- 驳回时释放 Revision 送审指针，但保留被驳回 Iteration 和平台审计。

### `DccControlledFileVersionQueryService`

- 提供 Master 摘要、正式版本、开放 Revision、检出信息和动作投影。
- 提供 Revision/Iteration 分组历史，不让前端按字符串推断版本关系。
- 普通浏览只投影当前正式 Iteration。

### `DccControlledFileVersionEventService`

- 写入工作版本动作成功和失败事件。
- 按事件键和请求摘要提供幂等回执。
- 审计写入失败时中止对应关键业务事务。

### `DccControlledFileVersionMigrationPreflightService`

- 只读计算现有主档身份、版本、文件、审批、签名和平台生命周期问题。
- 返回冻结边界、问题分类、样本和导出结果。
- 不提供业务数据修复或自动映射接口。

### 继续复用的模块

- `DccControlledFileUploadService` 和 `DccUploadTicketService`：创建及检入前上传。
- `DccControlledFileSourceOwnershipService`：Iteration 源文件独占和哈希。
- `DccControlledFileRouteReadinessService`：送审路线和人员前置检查。
- `DccSignatureVerificationService`：具体送审 Iteration 的签名。
- `DccControlledFileFinalizationService`：发布失败处理和新旧正式版本切换。
- `DccControlledContentAdapter`：平台候选、ACTIVE、SUPERSEDED 和审计映射。

现有超大 `WorkflowServiceImpl` 和 `QueryServiceImpl` 不继续承载新增版本规则；第一阶段按上述领域职责拆分后，由 Controller 组合调用。

## API Contracts

所有写接口必须接收 `idempotencyKey`。服务端根据动作、用户、目标对象和请求正文生成 `requestHash`，同键同摘要返回原回执，同键不同摘要返回冲突。

### 1. 创建逻辑文件和 A/1

`POST /admin-api/dcc/controlled-file-masters`

权限：`dcc:controlled-file:submit`，同时要求项目 OWNER 或 EDIT、类别 UPLOAD 权限。

请求：

```json
{
  "dccProjectCodeId": 101,
  "fileTypeTaxonomyId": 301,
  "categoryId": 21,
  "directoryId": 45,
  "fileNumber": "WI-001",
  "fileName": "装配作业指导书",
  "sessionId": "dcc-session-...",
  "sourceUploadTicket": "UT-...",
  "drawingPdfUploadTicket": null,
  "relatedControlledFileIds": [],
  "needTraining": false,
  "remark": "初始建档",
  "idempotencyKey": "..."
}
```

返回：`masterId`、`revisionId`、`iterationId`、`revisionCode=A`、`iterationNo=1`、`displayVersion=A/1`、`status=WORKING`。

行为：只建档，不启动 BPM。当前 `/dcc/controlled-files/submit` 的“创建并送审”职责在切换后拆除。

### 2. 逻辑文件摘要

`GET /admin-api/dcc/controlled-file-masters/{masterId}`

权限：`dcc:controlled-file:query`，并执行项目、类别、目录和当前用户职责校验。

返回：身份字段、当前正式 Iteration、开放 Revision、最新工作 Iteration、当前送审 Iteration、ACTIVE 检出记录以及后端 `actionProjection`。

前端不得根据状态字符串自行补出检出、修订或送审按钮。

### 3. 版本历史

`GET /admin-api/dcc/controlled-file-masters/{masterId}/versions`

参数：`includeWorking`、`includeSuperseded`。普通阅读用户服务端固定 `includeWorking=false`，即使客户端传 true 也不能越权。

返回按 Revision 分组：

```json
{
  "masterId": 1,
  "currentReleasedIterationId": 33,
  "openRevisionId": 12,
  "revisions": [
    {
      "revisionId": 11,
      "revisionCode": "A",
      "status": "ACTIVE",
      "sourceIterationId": null,
      "releasedIterationId": 33,
      "iterations": []
    }
  ]
}
```

每个 Iteration 返回 `iterationId`、`iterationNo`、`displayVersion`、`status`、直接前驱、文件哈希、修改说明、审批/发布时间和动作投影。

### 4. 检出

保留现有路径并增加正式请求体：

`POST /admin-api/dcc/controlled-files/{iterationId}/checkout`

权限：新增 `dcc:controlled-file:checkout`，并要求项目 OWNER 或 EDIT、类别 EDIT 权限。

请求：

```json
{
  "reason": "修正装配步骤",
  "idempotencyKey": "..."
}
```

前置条件：目标为开放 Revision 的最新 WORKING Iteration、未送审、Master 无 ACTIVE 检出、项目启用。

返回：`checkoutId`、`baseIterationId`、`checkedOutBy`、`checkedOutTime`、`reason`、`status=ACTIVE`。

### 5. 检入

保留现有路径但改为绑定上传凭证并创建 Iteration：

`POST /admin-api/dcc/controlled-files/{baseIterationId}/checkin`

权限：`dcc:controlled-file:checkin`。

请求：

```json
{
  "checkoutId": 88,
  "sessionId": "dcc-checkin-...",
  "sourceUploadTicket": "UT-...",
  "drawingPdfUploadTicket": null,
  "metadataChanges": {
    "fileName": "装配作业指导书（修订）",
    "remark": "同步修正名称"
  },
  "changeSummary": "修正步骤 3 的扭矩说明",
  "idempotencyKey": "..."
}
```

`sourceUploadTicket` 与 `metadataChanges` 至少一个产生真实变化。仅元数据变化时 sourceUploadTicket 可省略，服务端复制前驱源文件形成新的独占文件记录并保持 SHA-256 一致。允许变更字段为 `fileName`、`categoryId`、`directoryId`、`needTraining`、`remark` 和 `relatedControlledFileIds`；`dccProjectCodeId`、`fileTypeTaxonomyId`、`fileNumber` 禁止通过检入修改。

返回新 Iteration 和已终结 Checkout。客户端不提交 `iterationNo` 或 `versionNo`。没有内容变化且没有允许字段差异时返回 `DCC_CHECKIN_NO_CHANGE`。

前置条件：Checkout 为 ACTIVE、属于当前用户和目标 Master、baseIterationId 与检出记录一致、Revision 仍为 IN_WORK、当前最新 Iteration 未漂移。

### 6. 撤销检出

`POST /admin-api/dcc/controlled-files/{baseIterationId}/checkout/cancel`

权限：`dcc:controlled-file:checkout-cancel`。

请求：`checkoutId`、`reason`、`idempotencyKey`。

返回：Checkout 状态 `CANCELLED`，版本历史数量不变。

第一阶段不提供管理员强制撤销他人检出接口。

### 7. 创建下一大版本

`POST /admin-api/dcc/controlled-files/{sourceIterationId}/revisions`

权限：新增 `dcc:controlled-file:revise`，同时要求项目 OWNER 和类别 REVISE 权限。项目 EDIT 或 VIEW 均不能创建 Revision。

请求：

```json
{
  "revisionReason": "工艺要求发生正式变化",
  "sourceSelectionReason": "采用 A/2，A/3 中的临时试验内容不纳入本次修订",
  "idempotencyKey": "..."
}
```

行为：从 sourceIterationId 创建下一 Revision `/1`。来源不是 Master 当前正式 Iteration 时，`sourceSelectionReason` 必填。系统同时保存创建时当前正式 Iteration 快照，不得静默换成最新版本。实际复制内容来自 sourceIterationId；后续平台生命周期候选的 source ref 仍指向提交时当前 ACTIVE 正式版本，两种来源分别记录。

返回：新 `revisionId`、`iterationId`、`displayVersion=B/1`、`sourceIterationId`、`releasedBaselineIterationId`。

### 8. 提交最新工作小版本

`POST /admin-api/dcc/controlled-files/{iterationId}/submit`

权限：沿用 `dcc:controlled-file:submit`，并执行项目、类别、路线和签名前置检查。

请求：`selectedSignoffUserIds`、其他现有送审选项、`idempotencyKey`。版本号、项目、分类和源文件 ID 不由客户端重复提交。

前置条件：Iteration 为 Revision 的 `latest_iteration_id`，Revision 为 IN_WORK，无 ACTIVE 检出，无其他平台开放候选，路线就绪。

行为：创建路线快照和平台候选引用，启动 BPM，并将 Revision/Iteration 更新为 IN_REVIEW。

### 9. 审批、驳回和发布接口

- 现有 `/{iterationId}/approve-task`、`reject-task`、`return-task`、`transfer-task`、`sign-task` 保持路径，业务对象明确为 Iteration。
- 驳回完成后 Iteration 为 REJECTED，Revision 回到 IN_WORK，`submitted_iteration_id` 清空。
- 现有 `/{iterationId}/publish` 只接受 READY_TO_PUBLISH 的 Iteration。
- 发布成功时更新 Revision、Iteration、Master、平台生命周期和发布快照。
- 新建文件的首个 Revision 与后续 Revision 都必须批准后独立发布，不允许首版审批完成自动 ACTIVE。

### 10. 项目访问规则管理

`GET /admin-api/dcc/project-codes/{projectCodeId}/access-rules`

权限：`dcc:project-code:query`；返回当前生效和计划生效的 USER、DEPT、ROLE、POSITION 项目规则。

`PUT /admin-api/dcc/project-codes/{projectCodeId}/access-rules`

权限：`dcc:project-code:update` 且文控角色。

请求为完整规则集合，每项包含 `subjectType`、`subjectId`、`accessLevel`、`validFrom`、`expireTime`、`changeReason`。后端校验主体存在、时间区间有效、同一主体不重复，并要求启用项目至少保留一个当前已生效的 OWNER；仅有未来计划 OWNER 时拒绝保存。

保存采用完整集合替换事务；历史规则转为失效并保留，不物理删除。响应返回后端正式规则，前端不得以提交数组作为成功后的权威状态。

### 11. 迁移只读预检

`GET /admin-api/dcc/controlled-file-version-migration/preflight-summary`

权限：`dcc:controlled-file:migration:query` 且文控角色。

参数：可选 `tenantId` 仅限具备跨租户审计权限的管理端；普通文控固定当前租户。

返回：`snapshotMaxMasterId`、统计时间、主档数、版本数、可自动形成候选映射数和各类阻塞数。

`GET /admin-api/dcc/controlled-file-version-migration/preflight-issues`

参数：`snapshotMaxMasterId`、`issueType`、分页。返回主档和版本级问题，不修改数据。

`GET /admin-api/dcc/controlled-file-version-migration/preflight-export`

返回同一冻结边界下的盘点工作簿或 CSV 包。导出内容不得包含文件正文、访问令牌或凭据。

第一阶段不提供在线“一键迁移”接口；正式迁移只允许使用经评审的 release migration 和批准映射包。

## Error Model

新增错误码建议保持 DCC 现有错误码域，至少包括：

| 错误标识 | 含义 |
|---|---|
| `DCC_LOGICAL_FILE_IDENTITY_CONFLICT` | 复合身份已经存在 |
| `DCC_PROJECT_NOT_ACTIVE` | DCC 项目不存在或停用 |
| `DCC_PROJECT_ACCESS_DENIED` | 当前用户缺少所需项目访问级别 |
| `DCC_PROJECT_ACCESS_RULE_INVALID` | 项目访问主体、级别或有效期非法 |
| `DCC_PROJECT_OWNER_REQUIRED` | 启用项目缺少有效 OWNER |
| `DCC_FILE_TYPE_NOT_LEAF` | 文件类型不是有效分类叶子 |
| `DCC_REVISION_SEQUENCE_OVERFLOW` | 大版本序列数值超出服务端整数容量 |
| `DCC_OPEN_REVISION_ALREADY_EXISTS` | 已有开放大版本 |
| `DCC_REVISION_SOURCE_INVALID` | 来源不属于当前大版本或不可使用 |
| `DCC_REVISION_SOURCE_REASON_REQUIRED` | 非当前正式来源缺少理由 |
| `DCC_ITERATION_NOT_LATEST` | 目标不是最新工作小版本 |
| `DCC_ITERATION_NOT_EDITABLE` | 正式、审批中或终态版本不可编辑 |
| `DCC_CHECKOUT_ALREADY_ACTIVE` | 逻辑文件已由他人检出 |
| `DCC_CHECKOUT_NOT_OWNER` | 当前用户不是检出人 |
| `DCC_CHECKOUT_BASE_DRIFTED` | 检出后最新工作版本发生漂移 |
| `DCC_CHECKIN_NO_CHANGE` | 检入没有内容或允许元数据变化 |
| `DCC_CHECKIN_METADATA_IDENTITY_FORBIDDEN` | 元数据检入尝试修改逻辑文件身份 |
| `DCC_VERSION_COMMAND_IDEMPOTENCY_CONFLICT` | 同一幂等键对应不同请求 |
| `DCC_SUBMITTED_ITERATION_NOT_LATEST` | 尝试提交旧小版本 |
| `DCC_MIGRATION_PREFLIGHT_BLOCKED` | 迁移盘点存在阻塞 |

所有错误返回业务错误码、稳定标识和可操作 message。不得把身份冲突改写成“自动升版”，不得把检入失败改写成释放锁成功。

## Transactions and Idempotency

### 写事务加锁顺序

所有版本写事务统一按以下顺序：

```text
Master FOR UPDATE
-> Revision FOR UPDATE
-> ACTIVE Checkout FOR UPDATE
-> Iteration / Upload Ticket / Lifecycle Ref
```

禁止出现先锁 Iteration 再锁 Master 的反向路径。

### 创建 A/1

- 先锁定或依赖 Master 复合身份唯一索引。
- 在一个事务中创建 Master、Revision A、Iteration A/1、源文件所有权和版本事件。
- 上传凭证必须在同一事务中标记为已绑定。
- 数据库唯一键冲突转换为身份冲突，不重试成其他身份。

### 项目访问规则

- 锁定 DCC 项目和该项目当前规则集合。
- 校验替换后至少一个 OWNER 当前生效。
- 原子失效旧规则、插入新规则并记录调整原因；失败时保留旧规则集合。

### 检出

- 通过 ACTIVE 生成列唯一索引争抢检出锁。
- 重复同键同请求返回原 Checkout；不同请求摘要返回幂等冲突。

### 检入

- 锁定 Master、Revision、Checkout；有上传凭证时继续锁定并校验上传凭证。
- 校验 `latest_iteration_id == base_iteration_id`。
- 计算允许元数据字段差异；无内容和元数据差异时拒绝。
- metadata-only 时复制前驱源文件形成独立文件记录并复核 SHA-256；不能复用前驱可变文件记录。
- 原子完成：旧 WORKING -> ITERATED、新 Iteration 插入、Revision 最新指针更新、上传绑定或源文件复制、源文件所有权、Checkout -> CHECKED_IN、版本事件。
- 任一失败整体回滚，Checkout 保持 ACTIVE。

### 创建下一大版本

- 锁定 Master 后确认 `open_revision_id` 为空。
- 校验项目 OWNER，从显式 sourceIterationId 读取内容，通过确定性字母算法分配下一 revision sequence/code。
- 原子插入 Revision、`/1` Iteration、源文件独占副本、Master 开放 Revision 指针和事件。

### 提交审批

- 锁定 Master 和 Revision，重新确认提交的是最新 Iteration 且没有 ACTIVE Checkout。
- 创建平台候选、路线快照和 BPM 流程的事务边界必须沿用现有可靠编排；任何一步失败不得留下“已送审但无流程”的版本。

### 发布

- 文件转换/盖章失败进入 FINALIZATION_FAILED，但旧正式版本保持 ACTIVE。
- 最终数据库事务原子更新旧/新 Iteration、旧/新 Revision、Master 正式指针、平台生命周期和发布快照。
- 发布回调使用稳定事件键去重。

## Open Questions

本阶段 API 口径已收口。现有测试数据是否符合自动映射条件由迁移预检结果决定。

## Design Blockers

- 未完成运行库只读盘点和映射审批前，不得编写正式回填迁移。
- 当前 BPM 创建流程与本地状态写入的最终一致性边界需要在实施计划中用失败注入测试证明。
- 首版审批完成自动发布的现有路径必须在新模型切换前移除，否则 AC-P1-21 无法满足。
