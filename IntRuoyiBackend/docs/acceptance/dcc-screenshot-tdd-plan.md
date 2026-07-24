# DCC 截图需求 TDD 计划

## Purpose and Scope

本文为 DCC 截图 14 条需求定义严格 TDD 顺序。每项生产行为必须先写失败测试，再做最小实现，最后做回归验证。实现必须在当前 `yudao-module-dcc`、`yudao-module-system`、BPM 能力和 `yudao-ui-admin-vue3` 现有 DCC 页面基础上修改，不新建平行系统，不引入 mock 成功、静默降级或 fallback。

## Evidence Reviewed

- `docs/acceptance/dcc-screenshot-bdd-scenarios.md`
- `docs/product/dcc-screenshot-requirements-prd.md`
- `docs/product/dcc-screenshot-requirements-user-flows.md`
- `docs/product/dcc-screenshot-requirements-acceptance-criteria.md`
- `doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md`
- 后端 Maven 模块：`yudao-module-dcc`、`yudao-module-system`
- 前端脚本：`pnpm ts:check`、`node scripts/*.test.mjs`、Playwright E2E

## TDD Sequence

### R01 上传可编辑源文件和图纸 PDF

- RED Commands:
  - 后端：`mvn -pl yudao-module-dcc -Dtest=DccControlledFileSubmitAttachmentValidationTest test`
  - 前端：`node scripts/dcc-controlled-file-drawing-pdf-required.test.mjs`
- Expected Failures:
  - 提交 VO 和服务尚不支持源文件类型、图纸 PDF 伴随文件字段及逐文件校验。
  - 上传页尚未阻止缺少 PDF 的图纸类提交。
- Minimal Implementation Target:
  - 扩展现有 `DccControlledFileSubmitReqVO`、受控文件数据表和上传页状态，记录源文件与 PDF 伴随件。
  - 在 `DccControlledFileWorkflowServiceImpl` 提交前校验图纸扩展名和 PDF 伴随件。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileSubmitAttachmentValidationTest test`
  - `node scripts/dcc-controlled-file-drawing-pdf-required.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccControlledFileWorkflowServiceImplTest test`
  - `pnpm ts:check`

### R02 密码强度和定期强制更新

- RED Commands:
  - `mvn -pl yudao-module-system -Dtest=AdminUserPasswordPolicyTest test`
  - `node scripts/system-password-policy-frontend.test.mjs`
- Expected Failures:
  - 现有密码长度仍允许 4-16 位，未强制英文加数字。
  - 缺少密码最后更新时间、周期配置和强制更新拦截。
- Minimal Implementation Target:
  - 在现有用户创建、重置、个人修改密码、注册/重置密码 VO 和服务中统一密码策略。
  - 定期强制更新必须等业务确认周期后实现；未确认前该子项记录为 blocker，不写默认周期。
- GREEN Commands:
  - `mvn -pl yudao-module-system -Dtest=AdminUserPasswordPolicyTest test`
  - `node scripts/system-password-policy-frontend.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-system -Dtest=AdminUserServiceImplTest,AdminAuthServiceImplTest test`
  - `pnpm ts:check`

### R03 下载提醒和下载留痕

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileDownloadAuditTest test`
  - `node scripts/dcc-controlled-file-download-warning-audit.test.mjs`
- Expected Failures:
  - 前端下载前未强制展示非受控文件提醒。
  - 下载留痕未显式断言下载人 id 和下载时间，留痕写入失败路径可能未阻断下载。
- Minimal Implementation Target:
  - 复用现有 `/dcc/controlled-files/{id}/download` 和 `dcc_controlled_file_access_log`，补齐下载时间断言和失败处理。
  - 前端在 `triggerControlledFileDownload` 调用前增加确认弹窗。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileDownloadAuditTest test`
  - `node scripts/dcc-controlled-file-download-warning-audit.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest test`
  - `node scripts/dcc-controlled-file-download-auth.test.mjs`

### R04 文件视图修改中标识

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileEditingStatusProjectionTest test`
  - `node scripts/dcc-controlled-file-editing-badge.test.mjs`
- Expected Failures:
  - 响应 VO 尚未提供清晰的 `editing` 或可推导状态字段。
  - 列表、目录浏览、详情页未统一展示 `修改中`。
- Minimal Implementation Target:
  - 复用现有受控文件状态，后端响应提供修改中状态投影。
  - 前端在现有 DCC 列表、浏览、详情组件中显示 `修改中` 标签。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileEditingStatusProjectionTest test`
  - `node scripts/dcc-controlled-file-editing-badge.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest test`
  - `pnpm ts:check`

### R05 回退、转交、加签和撤回后处理

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileTaskActionExtensionTest test`
  - `node scripts/dcc-controlled-file-task-actions.test.mjs`
- Expected Failures:
  - 现有接口仅覆盖 approve/reject/withdraw，缺少回退目标、转交、加签和撤回后删除/重提动作。
  - 申请人退回待办提醒和原流程重提未被验证。
- Minimal Implementation Target:
  - 复用 BPM task API 扩展 DCC 任务动作接口，增加动作权限校验、回退目标、转交、加签和撤回后动作。
  - 保持原流程实例重提，不新建流程实例。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileTaskActionExtensionTest test`
  - `node scripts/dcc-controlled-file-task-actions.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileTaskActionApiTest,DccControlledFileWorkflowServiceImplTest test`
  - `pnpm ts:check`

### R06 新增文件类别、现行有效版本、14 位产品编号

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileMetadataExtensionTest test`
  - `node scripts/dcc-controlled-file-submit-metadata.test.mjs`
- Expected Failures:
  - 提交 VO 和数据库尚未记录截图要求的文件类别文本、现行有效版本读取结果和 14 位产品编号。
  - 前端上传页尚未展示现行有效版本和产品编号校验。
- Minimal Implementation Target:
  - 优先复用现有 `DccFileCategoryDO`、版本链和 `DccControlledFileMasterDO`。
  - 补齐 14 位产品编号字段、校验、查询响应和表单显示。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileMetadataExtensionTest test`
  - `node scripts/dcc-controlled-file-submit-metadata.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileWorkflowServiceImplTest test`
  - `pnpm ts:check`

### R07 外来文件评审流程

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccExternalDocumentReviewDefinitionBlockerTest test`
- Expected Failures:
  - 业务字段、节点、参与人、结论和输出物未确认，无法形成可执行流程测试。
- Minimal Implementation Target:
  - 当前阶段只允许写 blocker 测试或计划文档，不实现生产流程。
  - 业务确认后复用 BPM 和 DCC 流程配置能力新增流程。
- GREEN Commands:
  - 阻塞解除前无 GREEN 命令；不得提交生产代码。
- REGRESSION:
  - 阻塞解除后补齐 BPM 流程、接口、前端入口和 E2E 回归。

### R08 体系记录所有人下载

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccSystemRecordDownloadPermissionTest test`
  - `node scripts/dcc-system-record-download-permission.test.mjs`
- Expected Failures:
  - 当前下载权限只按目录/类别权限，不支持确认后的体系记录识别放行。
  - `INT/RE` 完整格式未确认，不能安全放开。
- Minimal Implementation Target:
  - 在下载权限判断中复用现有 `canReadBinary` 和访问日志，增加经确认的体系记录识别。
  - 即使所有人可下载，也必须保留非受控提醒和下载留痕。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccSystemRecordDownloadPermissionTest test`
  - `node scripts/dcc-system-record-download-permission.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccDirectoryAccessPermissionServiceTest,DccControlledFilePreviewDownloadApiTest test`

### R09 是否需要培训触发上传培训记录

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileTrainingRecordGateTest test`
  - `node scripts/dcc-controlled-file-training-record-gate.test.mjs`
- Expected Failures:
  - 当前培训能力是发布后培训任务，尚未验证第四节点前申请人上传培训记录。
  - 表单未保存 `是否需要培训` 和培训记录文件。
- Minimal Implementation Target:
  - 复用现有培训表或新增受控审批培训记录表，记录申请人上传的培训记录。
  - 流程进入第四节点前校验培训记录；未选择培训时不强制。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileTrainingRecordGateTest test`
  - `node scripts/dcc-controlled-file-training-record-gate.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccTrainingTaskServiceTest,DccControlledFileWorkflowServiceImplTest test`

### R10 电子发放回收接收人加签

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccElectronicDistributionCountersignTest test`
  - `node scripts/dcc-electronic-distribution-countersign.test.mjs`
- Expected Failures:
  - 电子发放接收人任务缺少加签动作接口和前端入口。
- Minimal Implementation Target:
  - 复用现有 `dcc_controlled_file_distribution_recipient` 与消息/任务模型，为接收人加签新增受控动作。
  - 校验操作者必须是接收人或具备确认权限。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccElectronicDistributionCountersignTest test`
  - `node scripts/dcc-electronic-distribution-countersign.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest,DccControlledFileFinalizationServiceImplTest test`

### R11 纸质发放回收记录导出和打印

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccPaperDistributionRecordExportPrintTest test`
  - `node scripts/dcc-paper-distribution-export-print.test.mjs`
- Expected Failures:
  - 现有纸质发放仅有确认接口，缺少完整记录维护、导出和打印接口。
- Minimal Implementation Target:
  - 复用现有纸质发放表，补齐发放人、接收人、发放日期、回收人、回收日期等字段和导出/打印服务。
  - 前端在现有发放页面增加维护、导出、打印入口。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccPaperDistributionRecordExportPrintTest test`
  - `node scripts/dcc-paper-distribution-export-print.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest,DccBaseSchemaTest test`

### R12 流程导出、打印和 Word 模板配置

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowExportPrintTest test`
  - `node scripts/dcc-controlled-file-workflow-export-print.test.mjs`
- Expected Failures:
  - 当前受控文件流程详情缺少流程导出/打印接口和模板配置校验。
- Minimal Implementation Target:
  - 复用现有受控文件详情、路线快照、签核摘要、文件基础信息生成导出/打印内容。
  - Word 模板配置必须等占位符确认后实现；模板不可读或占位符缺失时失败。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowExportPrintTest test`
  - `node scripts/dcc-controlled-file-workflow-export-print.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileSignatureServiceTest test`

### R13 会签节点申请人自行选择

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileSubmitterSelectedCountersignTest test`
  - `node scripts/dcc-controlled-file-submitter-selected-countersign.test.mjs`
- Expected Failures:
  - 当前路线快照主要来自配置路线，申请人无法在提交时选择会签人员。
- Minimal Implementation Target:
  - 复用 BPM `startUserSelectAssignees` 和现有路线快照，支持提交时传入会签人员。
  - 校验选择人员范围、必填和去重。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileSubmitterSelectedCountersignTest test`
  - `node scripts/dcc-controlled-file-submitter-selected-countersign.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest test`

### R14 第四节点文控上传受控章 PDF

- RED Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileDocControlStampedPdfGateTest test`
  - `node scripts/dcc-controlled-file-stamped-pdf-gate.test.mjs`
- Expected Failures:
  - 第四节点完成前未强制文控上传加盖受控章 PDF。
  - 受控章 PDF 与现有 stampedFileId/finalization 关系未清晰校验。
- Minimal Implementation Target:
  - 复用 `stampedFileId`、`DccPdfStampService` 或现有最终化服务，明确第四节点文控上传 PDF 的保存和校验点。
  - 未上传 PDF 时拒绝完成第四节点。
- GREEN Commands:
  - `mvn -pl yudao-module-dcc -Dtest=DccControlledFileDocControlStampedPdfGateTest test`
  - `node scripts/dcc-controlled-file-stamped-pdf-gate.test.mjs`
- REGRESSION:
  - `mvn -pl yudao-module-dcc -Dtest=DccPdfStampServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest test`

## RED Commands

后续 worker 应按需求拆分先补失败测试，命令统一记录到 `doc/tasks/<task-id>/execution-log.md`，格式为 `RED: <command> -> FAIL, <expected reason>`。禁止先写生产代码再补测试。

## Expected Failures

预期失败必须指向真实缺口：接口字段缺失、服务校验缺失、数据库字段缺失、前端入口缺失、BPM 动作缺失、真实测试数据缺失或业务前置条件未确认。不得把环境未启动、依赖缺失或 mock 未配置当成需求 RED 证据。

## GREEN Commands

每个需求的 GREEN 只允许在最小实现完成、对应 RED 测试通过后记录，格式为 `GREEN: <command> -> PASS`。涉及前后端联动的需求必须同时有后端 GREEN、前端静态或组件 GREEN、真实路径 E2E GREEN。

## Refactor Checks

- 复用当前 DCC 模块、BPM、系统用户和前端 DCC 页面，不新增平行模块。
- 删除或避免隐式 fallback；缺少流程、节点、角色、模板、文件或审计写入时失败。
- 保持接口清晰：提交字段、任务动作、下载、导出、打印、发放、培训记录分别有明确 VO 和错误码。
- 迁移或 schema 变更必须配套 `DccBaseSchemaTest` 或对应 Mapper 测试。

## Evidence Log Template

```text
BDD: Rxx <场景名> -> Given <前置条件> / When <用户动作> / Then <可观察结果>
RED: <命令> -> FAIL, <预期失败原因>
GREEN: <命令> -> PASS
REGRESSION: <命令> -> PASS
BLOCKER: <阻塞项> -> <影响范围>
```

## Subagent-Driven Verification Responsibilities

- Worker-A：后端 RED/GREEN、schema、Mapper、服务、错误码、BPM 动作和密码策略。
- Worker-B：前端 RED/GREEN、类型检查、DCC 页面入口、表单校验和交互提示。
- Worker-D：Playwright 真实路径 E2E、真实测试租户、下载文件与导出/打印产物验证。
- Reviewer：复核 BDD、RED/GREEN、REGRESSION 证据是否覆盖 14 条需求且无 mock 成功。

## Test Blockers

- 第四节点定义、文控角色和 BPM 节点 key 未确认：阻塞 R09、R12、R14。
- 密码强制更新周期未确认：阻塞 R02 的定期强制更新生产实现。
- `INT/RE` 编码完整格式未确认：阻塞 R08 的所有人下载放行。
- 外来文件评审流程未确认：阻塞 R07 生产实现。
- 测试租户真实用户、角色、文件类别、审批路线、下载权限和发放数据未准备：阻塞 E2E GREEN。
