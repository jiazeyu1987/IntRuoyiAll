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

## Corrective Post-Merge int_main E2E Verification

BDD: 融合后主线真实页面闭环 -> Given 当前分支为 `int_main`、功能提交已融合、固定运行态为前端 8081 和后端 48081，且 fixture 在租户 122 创建本轮任务自有 O1/O2、独立一线/组长账号和正式权限 When 一线真实登录选择计划量 6 的 O1 提交 10，组长真实登录查看红色待调整 4 并改配为 O1=6/O2=4 Then 版本 1 为 `FRONTLINE_SELECTED` 且 O1=10，版本 2 为 `MANUAL` 且总量 10、未分配 0、红色消失、审计完整。

BDD: 融合后验证链路不得复用旧工作树 -> Given 旧 worktree 事件 227/228 来自 8099/48099 When 执行本轮融合后验收 Then 运行态证据、事件 ID、结果 JSON、截图和清理结果必须来自 `E:\IntRuoyi` 的 8081/48081，旧证据不得计入本轮 PASS。

BDD: 融合后全链路零错误和零残留 -> Given 真实页面产生一线提交和组长确认两次目标业务写入 When 验证结束或中途失败 Then 目标页面错误、目标请求失败、目标 HTTP 错误、目标控制台错误均为 0，finally cleanup 必须执行，独立二次 cleanup 必须返回 `CLEAN` 且任务数据残留 0。

验证命令：

- `python -X utf8 doc/tasks/20260814-frontline-active-order-submit-allocation-docs/fas_fixture_orchestrator.py prepare ...`
- 在显式 `POST_MERGE_INT_MAIN` 和 `http://127.0.0.1:8081` / `http://127.0.0.1:48081` 环境下运行 `node tests/e2e/frontline-active-order-submit-allocation-real.e2e.js`。
- `python -X utf8 doc/tasks/20260814-frontline-active-order-submit-allocation-docs/fas_fixture_orchestrator.py cleanup ...` 进行独立二次清理。

通过条件：

- 真实页面 E2E 退出码 0，并生成新的融合后事件 ID、结果 JSON、证据 Markdown 和两张页面截图。
- 一线提交、红色标识、组长改配、两个分配版本、审计、写请求计数和四类错误计数全部满足业务断言。
- E2E finally cleanup 与独立二次 cleanup 均为 `CLEAN/0`。

阻塞条件：

- `int_main` 分支或融合提交缺失、8081/48081 归属不明、运行态不是当前主工作区构建、测试租户/登录/数据库/Redis/浏览器/fixture 任一正式前置缺失时必须 BLOCKED。
- E2E 脚本只能运行 8099/48099 或依赖已删除 worktree 时，先以静态/行为 RED 锁定该验证链路问题，再修复为显式主线模式；不得换端口或复用旧事件。

## Supplementary Yudao Source Admin Real E2E

BDD: admin 补充真实页面闭环 -> Given 当前分支为 `int_main`、8081/48081 归属 `E:\IntRuoyi`，且“芋道源码/admin”真实登录有效 When admin 在任务自有 O1/O2 上通过一线页面选择计划量 6 的 O1 提交 10，再通过生产组长页面把分配改为 O1=6/O2=4 Then 版本 1、红色待调整 4、版本 2、红色消失、总量 10、未分配 0 和审计必须与 P6 业务合同一致。

BDD: admin 基线不可修改 -> Given 租户 1 的 admin 是受保护基线账号 When 准备、执行和清理补充 E2E Then admin 用户、密码指纹、角色集合和既有签名授权指纹必须前后一致，只能新增并删除带本轮任务标识的业务 fixture。

BDD: admin 模式不得降级原验收 -> Given P5/P6 要求独立非 admin 一线与组长账号 When 新增 admin 补充模式 Then 原模式的固定租户、独立账号、端口、业务断言和清理合同必须继续通过静态及行为回归，admin PASS 不能替代角色隔离 PASS。

BDD: admin 补充链路零目标错误和零业务残留 -> Given 本轮允许的目标业务写入只有一线提交与组长确认 When 场景成功或中途失败 Then page error、目标请求失败、目标 HTTP 错误和目标 console error 均必须为 0，finally cleanup 与独立二次 cleanup 必须返回 `CLEAN/0`，任务自有业务数据残留 0。

验证命令：

- admin fixture 编排器自检及静态合同先 RED 后 GREEN。
- 使用显式 admin 模式在 `http://127.0.0.1:8081` / `http://127.0.0.1:48081` 执行真实 Playwright E2E，凭据只从本机受控配置注入且不输出。
- 独立复跑原 P6 静态合同、admin 结果机器断言、admin 基线前后指纹和二次 cleanup。

通过条件：

- 真实页面显示登录身份为“芋道源码/admin”，新事件、两版分配、红色状态、审计和写请求均满足业务断言。
- admin 用户、密码、角色和既有签名授权前后不变；本轮任务自有业务数据清理为 `CLEAN/0`。
- 原租户 122 独立账号验证合同继续 PASS；admin 结果明确标注为补充权限验证，不冒充角色隔离验收。

阻塞条件：

- admin 登录、页面入口、现有签名授权或主运行态任一前置缺失时，在目标业务写入前 BLOCKED。
- 任何方案需要修改 admin 用户、角色、密码、既有签名授权或正式业务数据时必须 BLOCKED。
