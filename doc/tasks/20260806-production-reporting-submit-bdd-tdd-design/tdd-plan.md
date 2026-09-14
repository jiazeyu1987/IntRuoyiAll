# TDD Plan

## Purpose and Scope

本计划把 BDD 场景拆成严格测试优先的实现顺序。实现阶段必须先写失败测试，再做最小生产代码改动，然后运行 GREEN 和回归验证。范围覆盖前端静态合同、后端提交契约、后端读模型、前端展示颜色、真实 E2E 和回归命令。

## Evidence Reviewed

- `TeamLeaderWorkbenchPage.vue` 当前生产报工列表仍配置 `workOrderCode`、`pqcResult`、`submissionContent`。
- `FrontlineFixedTemplatePanel.vue` 当前生产填写页从运行态配置生成 `configuredDefectReasons`、`configuredDeviceCards`，并把 `productionScrapQuantity` 计算为损耗原因数量合计。
- `buildFrontlineFormalSubmitPayload` 当前传 `lossReasonId: selectedLossReasonId.value`，只能表达首个非零损耗原因。
- `ProFrontlineFeedbackSubmitReqVO` 当前有 `feedbackPayload`、`recordbookPayload`、`processPoolContext` 和 `rawPayload`，但缺少强类型损耗明细数组和参数异常结构。
- `MesFrontlineLossReasonValidatorImpl` 当前按 `routeProcessId + lossReasonId` 校验单个损耗原因。
- `MesProFrontlineFeedbackRawLimitBypassTest` 已证明设备参数超限不能被后端拒绝或裁剪。

## TDD Sequence

1. 前端报工管理列表静态合同 RED：锁定生产组长页签删除红框列，并新增完整字段列或结构化展示块。
2. 前端提交 payload 静态合同 RED：锁定生产报工必须提交 `lossDetails[]`、`selectedDevice`、`deviceParameterReadings[]` 和参数异常判定所需上下限快照。
3. 后端提交契约 RED：新增后端单测，证明多损耗原因明细、设备、参数上下限和异常标记被校验、保存并进入工序池事件。
4. 后端配置作用域 RED：新增后端单测，证明损耗原因、设备和参数均按当前 `routeProcessId/processId/deviceId` 校验，不能跨工序或跨设备。
5. 前端参数异常展示 RED：新增静态合同，证明超限参数输入和报工管理列表值具备红色异常类、异常方向和可访问提示。
6. 最小实现：扩展 VO/DTO、payload splitter、提交服务校验、工序池读模型、前端类型、填写面板、报工管理列表。
7. GREEN：逐个运行 RED 命令并通过，再运行相邻回归。
8. Refactor：收敛命名、去除旧 `submissionContent` 生产展示路径、保留 PQC 专属链路边界。
9. 真实 E2E：使用任务自有测试数据完成生产组长配置、员工报工、班组长列表核验。

## RED Commands

- `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- 预期失败原因：当前生产组长报工管理表仍包含 `生产工单`、`PQC`、`提交内容`，且缺少 `完成数量`、`损耗数量`、`损耗明细`、`选用设备`、`设备参数`、`参数异常`。

- `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs`
- 预期失败原因：当前前端只提交首个 `lossReasonId`，不能提交每个损耗原因及数量，也没有结构化 `selectedDevice` 和 `deviceParameterReadings`。

- `node tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs`
- 预期失败原因：当前设备参数输入和报工管理列表没有稳定异常类，例如 `is-parameter-out-of-range` 或等价 marker。

- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 预期失败原因：新增测试类先失败，当前后端缺少多损耗明细合计校验、设备参数异常快照保存和结构化事件 payload。

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigProcessScopeTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 预期失败原因：当前读模型和校验链路还不能证明每个原因、设备、参数均按当前工序配置作用域读取并返回列表展示需要的结构化字段。

## Expected Failures

- 前端列表合同应指出生产组长 `report` 页签仍保留 `workOrderCode`、`pqcResult`、`submissionContent` 三个红框列。
- 前端 payload 合同应指出 `selectedLossReasonId` 只能选择第一个非零损耗原因，丢失其它损耗原因数量。
- 后端提交契约应指出 `MesFrontlineLossReasonValidator` 仅校验单个原因，未校验明细合计，也未校验设备和参数规则作用域。
- 参数异常展示合同应指出超限值虽能保存，但缺少前端红色异常提示和列表异常展示。
- 读模型合同应指出 `ProcessPoolTimelineEventVO` 只有 `submittedSummary` 与 `originalPayloadJson`，缺少 `outputQuantity`、`lossQuantity`、`lossDetails`、`selectedDevice`、`deviceParameterReadings` 等结构化字段。

## GREEN Commands

- `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs`
- `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs`
- `node tests/e2e/team-leader-production-report-abnormal-parameter-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigProcessScopeTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node tests/e2e/frontline-formal-submit-static.spec.cjs`
- `node tests/e2e/team-leader-report-allocation-static.spec.cjs`

## Refactor Checks

- 删除生产组长列表旧的 `resolveProductionSubmissionSummary(row)` 作为生产报工主展示路径；如保留，只能作为调试详情中的原始摘要，不可替代结构化字段。
- 新增类型命名必须表达正式业务含义，例如 `lossDetails`、`selectedDevice`、`deviceParameterReadings`、`parameterStatus`。
- 参数异常状态只用于展示和审计，不得阻断提交；不能新增后端拒绝超限参数的兼容分支。
- 损耗原因、设备、参数校验必须 fail fast；禁止用当前配置反推历史提交、用名称匹配代替 ID、或用空数组默认成功。
- PQC 组长报工管理列保留边界需单独断言，避免生产页签删列误伤 PQC 审核链路。

## Evidence Log Template

- BDD: 生产报工管理列表拆分提交内容 -> Given/When/Then
- RED: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL, 生产列表仍保留红框列且缺少结构化字段列
- GREEN: `node tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs` -> PASS
- RED: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> FAIL, 提交 payload 只能表达单个损耗原因
- GREEN: `node tests/e2e/frontline-production-submit-payload-detail-static.spec.cjs` -> PASS
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 后端缺少明细合计和作用域校验
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitDetailContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

## Test Blockers

- 若目标 Maven 因 Windows 页面文件、并发 Maven 或 `target` 损坏无法到达 Surefire，必须按 `docs\powershell-memory.md` Maven blocker 记录环境阻塞，不能写成业务失败。
- 若前端 `package.json` 缺少可运行脚本或 spec 文件不存在，先补入口合同，再进入业务 RED。
- 若真实 E2E 缺少账号、签名、生产工单、当前工序配置或本地运行态，记录为 E2E 前置 blocker，不得使用 API-only 或静态合同冒充真实页面通过。

