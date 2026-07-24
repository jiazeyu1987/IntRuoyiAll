# Execution Log：DCC NAS目录转移后端实现

BDD: read NAS file bytes by relative path -> Given 当前 NAS 配置已指向 `\\172.30.30.4\质量体系文件` When DCC 后端按相对路径读取 NAS 文件 Then 必须返回文件字节与元信息，路径不存在或权限不足时必须显式失败

BDD: transfer selected NAS directories into DCC controlled tree -> Given 用户选择一个或多个 NAS 目录并提供模板类别与统一生效日期 When 调用 DCC NAS 批量转移接口 Then 后端必须按相对路径复用或新建 DCC 目录、复用或新建类别并逐文件汇总成功与失败结果

BDD: clone category governance from template category -> Given 某个 NAS 含文件目录在 DCC 中不存在对应类别 When 后端自动创建类别 Then 新类别必须克隆模板类别的审批矩阵、权限、分发、培训和需求开关

BDD: allow non-pdf controlled file submission -> Given 用户上传或 NAS 转移进入一个非 PDF 文件 When 创建 DCC 受控文件记录并进入审批链路 Then 后端必须允许提交、保留原文件为 published 文件，并跳过 PDF 盖章

BDD: branch preview metadata by content type -> Given DCC 中存在 PDF、图片、文本、Office 与其他二进制文件 When 查询预览元信息 Then 后端必须明确返回 `PDF / IMAGE / TEXT / OFFICE / DOWNLOAD_ONLY` 中一种预览类型

BDD: bypass approval for NAS transfer only -> Given 用户明确要求 NAS 转移不走审批 When 调用 NAS 转移接口 Then 后端必须绕过 BPM 审批路线与 IntAuth 主管映射，但仍保留版本校验、目录/类别校验与发布后分发/培训治理

RED: `Invoke-RestMethod POST http://127.0.0.1:48081/admin-api/dcc/controlled-files/nas-transfer` with `selectedNasPaths=["1. QMS documents/PD可编辑"]` -> FAIL, real NAS 转移已走到 DCC 提交阶段，但固定版本号 `V1.0` 被现有版本解析拒绝并返回 `Controlled file version format is invalid`

RED: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileUploadApiTest,DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 新增的 OnlyOffice fail-fast 用例与错误消息断言未对齐，需要把配置缺失表达收成框架可验证的形式

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileUploadApiTest,DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

GREEN: `mvn --% -pl yudao-server -am -DskipTests package` -> PASS, 最新 `yudao-server.jar` 已包含 NAS 转移、OnlyOffice fail-fast 和 `V1.0` 版本解析修复

GREEN: `Invoke-RestMethod GET http://127.0.0.1:48081/admin-api/infra/file/nas-files?path=1.%20QMS%20documents` -> PASS, 实时返回 `PD可编辑` 等 NAS 子目录，说明 SMB 目录读取仍然可用

GREEN: `Invoke-RestMethod POST http://127.0.0.1:48081/admin-api/dcc/controlled-files/nas-transfer` with `selectedNasPaths=["1. QMS documents/PD可编辑"]` on latest runtime -> PASS for file read and DCC submit handoff, failure stage advanced from `version format` to `submit` with `Approval position runtime mapping failed: 编制人直接主管 requires the submitter to have a direct manager in IntAuth`

INFO: local runtime `http://127.0.0.1:48081` now runs `backend-dcc-nas-transfer-20260522-165255.jar` with explicit MySQL `127.0.0.1:23306/ruoyi-vue-pro` and Redis `127.0.0.1:26379` overrides

RED: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL before implementation because workflow service had no no-approval submit path and NAS transfer still depended on BPM approval flow

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileUploadApiTest,DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after adding `submitControlledFileWithoutApproval(...)` and `activateWithoutApproval(...)`

GREEN: real transfer on current runtime with `selectedNasPaths=["1. QMS documents/PD可编辑"]` -> PASS for no-approval routing, failure advanced from `Approval position runtime mapping failed` to `Unable to resolve distribution recipients for department 121`

RED: real transfer on no-approval runtime with `selectedNasPaths=["1. QMS documents/PD可编辑"]` -> FAIL, direct activation path still inherited template category distribution/training governance and returned `Unable to resolve distribution recipients for department 121`

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileNasTransferServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after introducing skip-governance direct activation for NAS transfer

GREEN: `Invoke-RestMethod POST http://127.0.0.1:48081/admin-api/dcc/controlled-files/nas-transfer` with `selectedNasPaths=["1. QMS documents/PD可编辑"]`, `templateCategoryId=900250`, `effectiveDate=2026-05-22` on latest runtime -> PASS, response `createdFileCount=4`, `failedFileCount=0`, `failures=[]`

GREEN: `Invoke-RestMethod GET http://127.0.0.1:48081/admin-api/dcc/controlled-files/page?pageNo=1&pageSize=20&categoryId=900298&latestVersionOnly=true` -> PASS, category `PD可编辑` 下已存在 4 条 `ACTIVE` 受控文件，均为 `V1.0`

INFO: after the successful import above, rerunning the same NAS transfer on `PD可编辑` returns `Controlled file version must be greater than the current chain version`, which is expected duplicate-import behavior rather than a new transfer-path failure
