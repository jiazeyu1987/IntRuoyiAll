# Execution Log：DCC NAS 大目录转移后端闭环验证

BDD: 真实 NAS 大目录可完整转入 DCC -> Given 选中的真实 NAS 目录约有 `100` 个文件且包含多级子文件夹 / When 用户通过真实前端入口发起 `转移到 DCC` / Then 后端必须完成目录遍历、文件读入、目录/类别复用或创建，并把可导入文件写入 DCC 文控目录

BDD: 大目录转移失败必须暴露真实阻塞 -> Given NAS 大目录转移链路中任一阶段缺少前置条件或出现缺陷 / When 真实转移执行到失败点 / Then 系统必须返回明确失败阶段和原因，不得静默跳过、mock 成功或降级伪装通过

BDD: 修复后真实重试必须达到成功结果 -> Given 已针对 RED 失败做了最小修复 / When 对同一类真实 NAS 大目录重新执行转移验证 / Then 后端返回的成功/失败统计与 DCC 落库结果必须一致，并形成可复查证据

RED: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，`activateWithoutApproval_skipGovernance_pdfStampFailurePublishesOriginalPdf` 当前仍抛 `Missing root object specification in trailer.`，说明 NAS 无审批转移仍会被 PDF 盖章失败整体拦住

RED: 真实运行态 `selectedNasPaths=["1. QMS documents/5.STM实验室规程"]`, `templateCategoryId=900250`, `effectiveDate=2026-05-23` -> FAIL，返回 `createdFileCount=65`、`failedFileCount=45`，失败集中在 `submit` 阶段并带出 `Missing root object specification in trailer.`

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，并在定向单测中明确记录 `NAS transfer stamp failed, publish original PDF instead.`

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

GREEN: `mvn --% -pl yudao-server -am -DskipTests package` -> PASS

GREEN: 真实运行态 `selectedNasPaths=["1. QMS documents/5.STM实验室规程"]`, `templateCategoryId=900250`, `effectiveDate=2026-05-23` -> PASS，`createdFileCount=110`，`failedFileCount=0`，`skippedPreviewOnlyCount=2`

GREEN: 真实运行态 `selectedNasPaths=["2.DHF/大文控-研发转移项目/48 气囊式股动脉止血带 PB"]`, `templateCategoryId=900250`, `effectiveDate=2026-05-23` -> PASS，`createdFileCount=97`，`failedFileCount=0`，`skippedPreviewOnlyCount=1`

GREEN: 同一路径二次重跑 `2.DHF/大文控-研发转移项目/48 气囊式股动脉止血带 PB` -> PASS，`createdFileCount=97`，`failedFileCount=0`

GREEN: 独立 verifier 子 agent 只读复核 -> PASS，确认 `STM实验室规程` DCC 递归文件总数为 `110`、`48 气囊式股动脉止血带 PB` DCC 递归文件总数为 `97`，且两者都未再出现失败文件
