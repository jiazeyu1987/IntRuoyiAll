# Backend API Evidence

## Scope

- MES/eDHR 正式电子签名服务：
  - `MesProBatchRecordExecutionSignatureService`
  - `MesProEdhrBatchExecutionServiceImpl`
- BPM 审批电子签名服务：
  - `ApprovalSignatureRecordServiceImpl`

## API Contract and Data Contract

- 正式电子签名时间只由后端系统时间生成。
- 正式电子签名命令携带 `selectedSignedAt`、`selectedTimeZone` 或 `selectedTimeReason` 时 fail fast。
- eDHR Stage1 模拟记录保留为非正式模拟记录，签名模式为 `SIMULATION_SESSION`，不再使用容易混同正式签名的 `LOGIN_SESSION`。
- 原生 BPM 待办审批正式签名必须包含 `sourceTaskId` 与 `processInstanceId`，缺失时不生成统一签名证据；非流程直签业务仍允许无流程实例。

## Contract

正式电子签名 API 的合规契约是：服务器生成签名时间、账号 + 密码重认证、证据绑定业务内容与流程上下文、缺失关键上下文时 fail fast。

## Auth, Permissions, Validation, and Error Behavior

- MES/eDHR 正式签名仍调用 `AdminUserApi.reauthenticateForSignature(actorId, password)` 完成账号 + 密码重认证。
- 人工签名时间参数在重认证前被拒绝，避免生成任何正式签名证据。
- 原生 BPM 待办审批缺少流程/任务上下文时，在签名图片快照引用和统一签名前失败。

## Validation

通过服务层测试验证人工签名时间被拒绝、模拟签名模式不再混同正式签名、BPM 缺失流程上下文时拒绝生成统一签名。

## BDD Scenarios

- BDD: 正式电子签名禁止用户手动选择签名时间 -> Given 用户在 eDHR 正式签名命令中提交 selectedSignedAt, When 服务创建正式签名, Then 服务拒绝该请求并说明正式签名时间必须由系统生成。
- BDD: 正式电子签名必须密码重认证 -> Given 用户只持有登录会话但未提供电子签名密码, When 触发正式签名, Then 不产生正式电子签名记录。
- BDD: 非正式草稿/模拟记录不进入正式电子签名口径 -> Given 系统保存模拟签名记录, When 记录签名模式, Then 使用 `SIMULATION_SESSION` 标识非正式模拟记录。
- BDD: 多人审批保留顺序证据 -> Given 原生 BPM 审批任务创建签名, When 缺少流程实例或任务 ID, Then 服务拒绝创建正式签名证据。
- BDD: 非流程直签业务不被 BPM 顺序门禁误伤 -> Given 业务策略直签没有流程实例, When 创建正式电子签名, Then 服务允许签名并保留业务对象证据。

## RED

- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest#recordSubmitSignature_withSelectedTimeFailsFast' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 当前实现未前置拒绝 `selectedSignedAt`，继续调用统一签名后因未 mock 返回触发 NPE。

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest#recordSubmitSignature_withSelectedTimeFailsFast' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS。
- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=ApprovalSignatureRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，5 tests。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，14 tests。

## Verification

后端定向验证已通过；未执行真实 E2E。

## Observability Touchpoints

- 失败路径使用现有 ServiceException/IllegalArgumentException/NullPointerException 合约，不吞异常、不返回默认成功。
- 正式签名仍由统一签名内核写入证据哈希、签名时间、认证方式和业务内容绑定。

## Blockers

- 未执行真实前端 E2E；本轮用户要求为“修改验证”，未明确要求 E2E。按照项目规则，E2E 只在当轮明确要求时执行。
