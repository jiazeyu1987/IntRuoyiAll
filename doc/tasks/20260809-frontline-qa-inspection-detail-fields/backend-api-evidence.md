# Backend API Evidence

## Scope

- Service: `MesFrontlinePqcContextServiceImpl.toInspectionItem`。
- Behavior: 一线 PQC 读取已发布 QA 项目时，检验器具及设备、抽样方案原文必须存在；缺失时按精确字段路径 fail fast。

## API And Data Contract

- API shape 不变。
- `MesQaInspectionRegulationItemDO.inspectionTool/samplingPlanText` 继续直接映射到 `MesFrontlinePqcInspectionItem`。
- 不新增 schema、migration、配置或兼容字段。

## Auth Permissions Validation And Errors

- 鉴权与权限不变。
- Validation: `inspectionTool` 空白 -> `PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED`，字段路径 `inspectionItem.inspectionTool`。
- Validation: `samplingPlanText` 空白 -> `PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED`，字段路径 `inspectionItem.samplingPlanText`。
- 不吞异常，不返回默认或拼装文案。

## Required Services Fixtures And Migrations

- 不需要新增服务、fixture 或 migration。
- 使用现有静态跨层合同和目标 JUnit 编译/测试环境。

## BDD Scenarios

- BDD: 缺失检验器具原文 -> Given 已发布 QA 项目 `inspectionTool` 为空 When 一线 PQC 加载 Then 按 `inspectionItem.inspectionTool` 失败。
- BDD: 缺失抽样方案原文 -> Given 已发布 QA 项目 `samplingPlanText` 为空 When 一线 PQC 加载 Then 按 `inspectionItem.samplingPlanText` 失败。

## RED Evidence

- RED: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> FAIL。
- Expected: 并发版本缺少运行态两字段空白校验。

## GREEN Evidence

- GREEN: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS。
- BLOCKED: 目标 JUnit 因并发测试要求历史空字段继续返回 `null` 而失败；该口径与当前 fail-fast 合同互斥。

## Contract Verification

- 静态跨层合同证明两个字段继续直接映射，且缺失时按精确字段路径失败。
- `MesFrontlinePqcContextServiceTest`：38 个测试通过，1 个并发新增的历史空字段兼容测试与本任务合同冲突。

## Observability

- 复用现有 `PRO_FRONTLINE_SUBMIT_CONTEXT_REQUIRED` 业务异常和精确字段路径，无新增日志或指标。

## Blockers And Downstream Skills

- Blocker: 历史发布 QA 项目缺 `inspectionTool/samplingPlanText` 时应继续返回空白，还是阻塞并要求正式重发补齐，产品口径尚未统一。
- Follow-up: `independent-verification-gate`。
