# 测试文档 - 一线报工活跃订单自动分配

## Purpose and Scope

本测试文档定义后续代码实现的 BDD、严格 TDD、真实 E2E、测试数据和阻塞条件。测试目标是证明一线提交后自动分配到所选活跃订单、超量允许并红色标识、生产组长可重新分配。

## Evidence Reviewed

- PRD：`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/prd.md`。
- 开发文档：`doc/tasks/20260814-frontline-active-order-submit-allocation-docs/development-plan.md`。
- 当前代码差距：缺 `activeOrderId`、缺初始分配事务、分配保存会截断超量、组长列表已有基础红色标签。
- E2E 门禁：写入型 E2E 必须使用真实页面、真实账号、任务自有数据和可清理数据。

## Feature Scenarios

BDD: 一线提交自动分配到所选订单 -> Given 一线生产选择活跃订单 O1 且填写产出数量 50 When 员工电子签名并提交 Then 系统创建报工事件，并生成 O1=50 的初始分配记录。

BDD: 超量提交红色标识 -> Given O1 当前工序可承接数量为 30 When 一线生产选择 O1 并提交 50 Then 提交成功，生产组长报工管理列表中 O1 显示红色“待调整 20”。

BDD: 生产组长重新分配 -> Given 一线提交已初始分配 O1=50 When 生产组长调整为 O1=30、O2=20 Then 系统保存新分配版本，列表 O1 不再显示超量，审计记录从 O1=50 变为 O1=30/O2=20。

BDD: 初始选择可追溯 -> Given 组长已把当前分配从 O1 调整到 O2 When 查看提交详情和审计 Then 仍能看到一线最初选择的是 O1。

## Failure Scenarios

BDD: 缺活跃订单拒绝提交 -> Given 一线生产未选择活跃订单 When 点击提交 Then 系统拒绝并提示请选择活跃订单。

BDD: 非活跃订单拒绝 -> Given O9 未加入当前组长活跃订单池 When 前端或接口提交 O9 Then 系统拒绝并提示订单不在当前活跃订单池。

BDD: 分配总量不等于提交数量拒绝 -> Given 提交数量为 50 When 组长调整分配合计为 40 Then 系统拒绝保存并提示分配总量必须等于本次提交数量。

BDD: 已锁定订单拒绝调整 -> Given O1 分配已进入放行锁定 When 组长试图调整 O1 数量 Then 系统拒绝并提示该订单分配已锁定。

## Boundary Scenarios

BDD: 刚好等于剩余量 -> Given O1 可承接数量 50 When 一线提交 50 Then 自动分配成功且无红色标识。

BDD: 超量一件 -> Given O1 可承接数量 49 When 一线提交 50 Then 自动分配成功且红色标识 1。

BDD: 多个同工单活跃记录 -> Given 同一工单存在两个活跃订单记录 O1A 和 O1B When 一线选择 O1B 提交 Then 初始分配必须绑定 O1B 的 `activeOrderId`，不能按 `workOrderId` 选错。

## TDD Sequence

### 后端 RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSelectedActiveOrderSubmitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前提交上下文没有 `activeOrderId`，且一线提交后没有创建初始分配。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationOverageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前分配服务会把超量截断到订单剩余量。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionOverageReadModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前读模型不能稳定返回选中活跃订单和订单级超量状态。

### 后端 GREEN

- GREEN: 同上三个 Maven 命令 -> PASS，证明 `activeOrderId`、初始分配、超量保留、红色标识读模型和调整审计均通过。

### 前端 RED

- RED: `pnpm --dir IntRuoyiFronted test e2e:frontline-active-order-submit:static` -> FAIL, 当前一线提交 payload 未包含 `activeOrderId`。
- RED: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-overage:static` -> FAIL, 当前红色标识合同未锁定订单级超量口径。
- RED: `pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation-current:static` -> FAIL, 当前弹窗存在前端预填路径，不能证明后端已保存初始分配。

### 前端 GREEN

- GREEN: 同上三个 pnpm 静态合同命令 -> PASS，证明 payload、列表标识、当前分配读取和调整交互符合设计。

## RED Commands

- 后端服务测试先失败，失败原因必须是缺字段、缺初始分配、超量被截断或读模型缺超量状态。
- 前端静态合同先失败，失败原因必须是 payload、红色标识口径或分配读取合同不满足。
- 不能用“测试类不存在”“脚本不存在”作为有效 RED；如缺测试入口，必须先补测试入口并让断言失败。

## Expected Failures

- 当前 `MesProFrontlineProcessPoolContextReqVO` 没有 `activeOrderId`。
- 当前一线提交服务没有调用初始分配保存。
- 当前 `capAllocationsToAvailableCapacity` 会把分配数量截断。
- 当前生产组长列表红色标识主要依赖未分配或工单数量推断，未锁定正式订单级超量字段。

## GREEN Commands

- 后端：运行新增定向 Maven 测试，并根据改动范围补跑现有 MES 相关回归。
- 前端：运行新增静态合同、相关页面合同和必要的 `pnpm --dir IntRuoyiFronted ts:check`。
- E2E：运行真实 Playwright 路径，覆盖一线提交、组长列表、红色标识、组长调整和清理。

## Refactor Checks

- 不新增 fallback 或静默截断。
- 不用 `workOrderId` 替代 `activeOrderId`。
- 不把前端预填当作正式初始分配。
- 不影响 PQC 活跃订单、批记录正式来源和已放行订单锁定规则。

## User Paths

1. 一线生产账号打开生产填写入口。
2. 选择活跃订单 O1。
3. 选择工序、员工、设备并填写数量 Q。
4. 输入所选员工电子签名并提交。
5. 生产组长账号打开报工管理列表。
6. 找到本次提交，确认 O1 当前分配为 Q，超量时红色标识可见。
7. 打开分配弹窗，把部分数量调整到 O2。
8. 保存后刷新列表，确认当前分配、红色标识和审计变化。

## Browser or Client Steps

- 必须通过真实前端页面操作选择、提交、查看和调整。
- 请求监听必须覆盖一线提交接口、组长列表接口、分配当前快照接口、分配保存接口和审计接口。
- 浏览器控制台不得有目标链路错误。

## API Verification

- 只读核验提交事件 ID、选中活跃订单 ID、初始分配记录、当前分配记录、超量字段、审计记录和清理结果。
- API 只能作为最终只读验证或页面操作后的辅助检查，不能代替用户路径。

## Console and Log Checks

- 页面无 `pageerror`。
- 目标接口 HTTP 成功且业务码成功。
- 后端日志无提交成功但初始分配失败的半成品。
- 列表刷新失败时必须有分层错误，不得误报提交失败或重复写入。

## Required Test Data

- 任务自有前缀活跃订单 O1、O2。
- O1 当前工序可承接数量小于本次提交数量。
- O2 当前工序可承接数量足以承接调整数量。
- 一线生产账号、实际员工档案、电子签名。
- 生产组长账号、生产组长人员范围、活跃订单池权限和报工管理权限。
- 可清理的工序、设备、损耗原因和必要记录本上下文。

## Reset Procedure

- 清理任务自有报工事件、初始分配、调整分配、审计、活跃订单、人员范围和测试订单。
- 清理前先只读快照目标 ID，禁止扩大范围。
- 清理后复核任务自有残留为 0，非目标数据不变。

## Data Ownership

- 测试数据必须全部带任务自有前缀或稳定任务标识。
- 不得使用生产租户或无关真实业务订单。
- 密码只能通过安全环境变量注入，不写入日志、文档或截图。

## Test Blockers

- 缺测试租户、账号、签名、权限、活跃订单、正式工序、设备或可清理数据时，真实 E2E BLOCKED。
- 缺浏览器或前端入口时，真实 E2E BLOCKED。
- 如果后端无法持久化 `activeOrderId` 或初始分配，不能进入 GREEN。

## Evidence Log Template

- BDD: <场景名> -> Given/When/Then
- RED: <命令> -> FAIL, <预期失败原因>
- GREEN: <命令> -> PASS
- E2E: <命令> -> PASS/BLOCKED, <账号、订单、事件、分配、红色标识、清理证据>
