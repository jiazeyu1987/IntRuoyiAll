# 生产放行闭环测试计划

## Test Strategy

- 所有行为先记录 BDD，再执行可解释的业务 RED、最小 GREEN 和相关回归。
- 单元/集成测试验证事务、权限、幂等、CAS、唯一约束、映射来源和错误 JSON。
- 前端命名静态合同只证明接口/页面实现，不冒充真实 E2E。
- 最终 E2E 使用 Playwright、真实前端入口、真实测试租户/账号和任务自有数据；API/DB 只在页面完成后做只读核验。
- 目标环境前置缺失时测试记 BLOCKED，并精确列出缺失项；禁止 mock、SQL 推状态、API-only 或默认管理员降级。

## Test Cases

### TC-01 SP-0 角色与候选解析

- test_case_id: `TC-01`
- mapped_task_ids: `[T1]`
- mapped_acceptance_ids: `[AC-03, AC-07, AC-20, AC-21, AC-30]`
- environment or setup: 两租户；目标用户/同名用户；启用、停用、未绑定、重复角色和空候选 fixtures。
- steps: 执行迁移两次；按 tenantId+roleCode 解析；切换角色成员和状态；以正反向用户校验候选和权限。
- expected_result: 角色/权限精确、迁移幂等；只返回当前租户启用角色成员；缺失和歧义结构化失败；无用户名/固定 ID 授权。
- evidence: SQL 静态合同、定向 JUnit、权限扫描、真实登录后的候选查询。

### TC-02 共享 schema、CAS、错误和 ID 合同

- test_case_id: `TC-02`
- mapped_task_ids: `[T2]`
- mapped_acceptance_ids: `[AC-05, AC-06, AC-11, AC-19, AC-28, AC-29, AC-30, AC-31, AC-34]`
- environment or setup: 迁移前/后 schema fixture、18 位 ID、并发事务和旧状态数据。
- steps: 执行迁移及 schema assertions；构造重复 scope/CAS；触发 blocker advice；检查旧状态预检和审计回滚。
- expected_result: 精确字段/索引/nullable 条件；重复写被约束；CAS 只更新一行；错误响应保留 data；ID 字符串；旧记录阻塞。
- evidence: schema test、Mapper/Advice JUnit、并发集成测试、JSON contract。

### TC-03 组长进度和归属门禁

- test_case_id: `TC-03`
- mapped_task_ids: `[T3, T4]`
- mapped_acceptance_ids: `[AC-01, AC-02, AC-29]`
- environment or setup: 生产/检测进度 99.99/100 组合；目标/非目标生产组长。
- steps: 前端按钮与直接后端请求分别尝试完工。
- expected_result: 任一未 100 或非归属用户均无写入并返回对应 blocker；双 100 且归属正确才继续。
- evidence: service/controller tests、前端 named test、Playwright 请求计数和页面提示。

### TC-04 SP-1 原子申请、回执和幂等

- test_case_id: `TC-04`
- mapped_task_ids: `[T3, T4]`
- mapped_acceptance_ids: `[AC-04, AC-05, AC-06, AC-27, AC-28, AC-31]`
- environment or setup: 双 100 工单、PQC 候选；故障注入点；同一业务身份的同/异请求幂等键、同一请求键对应不同权威快照；响应超时/刷新失败模拟。
- steps: 提交申请；核对对象增量；重放；按 activeOrderId 查回执；注入 resolver/task 失败；运行页面恢复路径。
- expected_result: 仅申请+PQC task；下游增量零；同键或同一权威业务身份的异请求键均返回同一回执；请求键载荷冲突明确；事务回滚；成功事实不被刷新失败覆盖。
- evidence: `MesProductionReleaseApplySp1Test`、controller JSON test、SP-1 named test/Playwright、只读核验。

### TC-05 PQC 权限和拒绝终态

- test_case_id: `TC-05`
- mapped_task_ids: `[T5, T6]`
- mapped_acceptance_ids: `[AC-07, AC-08, AC-29, AC-30, AC-31]`
- environment or setup: zhulijiang 正向、另一角色成员正向、非角色/跨租户负向；待审申请。
- steps: 查询候选任务；空/有效原因拒绝；拒绝后尝试通过、重申请和撤回。
- expected_result: 角色+候选双校验；拒绝原子终态；无任何下游对象；后续动作 `UNSUPPORTED_RELEASE_ACTION`。
- evidence: PQC service/controller tests、SP-2 named test、真实页面拒绝 E2E。

### TC-06 PQC 通过事务和申请唯一批次

- test_case_id: `TC-06`
- mapped_task_ids: `[T5]`
- mapped_acceptance_ids: `[AC-09, AC-10, AC-19, AC-34]`
- environment or setup: 完整正式来源；冻结版本与当前 active 不同；writer/task 故障注入；并发请求。
- steps: PQC 通过；每个写点注入异常；重复/并发通过；改变当前 active routeVersion。
- expected_result: 成功时一批次+三类映射+四任务；失败时全回滚；只按申请 key 唯一；始终使用冻结版本。
- evidence: `MesPqcReleaseBatchExecutionServiceTest`、事务/并发集成测试、唯一约束检查。

### TC-07 三类正式来源与零损耗

- test_case_id: `TC-07`
- mapped_task_ids: `[T5]`
- mapped_acceptance_ids: `[AC-11, AC-12, AC-13, AC-33]`
- environment or setup: 分别缺三类正式绑定；仅有 formBindings；零损耗；旧批次歧义。
- steps: 逐个移除正式来源并补动态槽位；尝试通过；执行零损耗正向；运行旧数据预检。
- expected_result: 动态槽位不能解除 blocker；三类错误独立；零损耗生成正式零值单；旧关系返回迁移 blocker。
- evidence: 三个既有 writer tests 扩展、PQC 集成测试、三链路回归。

### TC-08 四任务数量、负责人和候选查询

- test_case_id: `TC-08`
- mapped_task_ids: `[T7, T8]`
- mapped_acceptance_ids: `[AC-14, AC-16, AC-28, AC-30]`
- environment or setup: 来料/灭菌/成品三负责人，成品负责人承担两节点；跨角色/租户用户。
- steps: 初始化；按 nodeTypes/batchExecutionId 查询；分别登录三类账号；检查字符串 ID/version。
- expected_result: 恰好四任务；1/1/2 分配；只见自己的 TODO；无 nodeType 推断和 ID 精度损失。
- evidence: SP3ReportUploadGateTest、WorkTask controller test、named test、真实候选页 E2E。

### TC-09 报告上传、不可跳过、版本与幂等

- test_case_id: `TC-09`
- mapped_task_ids: `[T7, T8]`
- mapped_acceptance_ids: `[AC-15, AC-16, AC-27, AC-29, AC-31]`
- environment or setup: 四个任务、真实测试附件、错误哈希/旧版本/重复键、灭菌批号。
- steps: prepare/complete；跨负责人；缺附件/批号；同键重放/同键异载荷；调用 skip/delete/withdraw/overwrite。
- expected_result: prepare 不增版本；complete 增版本一次；正式附件/审计唯一；所有绕过失败且状态不变。
- evidence: special-node service/controller tests、SP-3 named test、真实上传 E2E 和文件哈希核对。

### TC-10 前三份门禁与第四份原子交接

- test_case_id: `TC-10`
- mapped_task_ids: `[T7, T9]`
- mapped_acceptance_ids: `[AC-17, AC-18, AC-19, AC-20, AC-31, AC-34]`
- environment or setup: 任意完成顺序；第四份并发；manager role/transaction/task 故障注入。
- steps: 完成 1..3 份后查状态和对象数；并发完成最后节点；逐点注入失败。
- expected_result: 前三份无最终对象；第四份成功时一事务创建快照/transaction/task；失败时第四份也未完成；并发唯一。
- evidence: SP3/SP4 JUnit、并发集成和 DB 唯一约束。

### TC-11 管理者授权、不支持动作和快照复核

- test_case_id: `TC-11`
- mapped_task_ids: `[T9, T10]`
- mapped_acceptance_ids: `[AC-20, AC-21, AC-22, AC-23, AC-29, AC-30]`
- environment or setup: xujianhai/另一角色成员；非角色；跨租户；一致/被篡改报告快照。
- steps: 查询任务；尝试拒绝/退回/撤回；改动附件证据；执行 approve。
- expected_result: 仅角色+候选能通过；不支持动作无副作用；快照变化阻断；不硬编码用户名。
- evidence: SP4ManagerReleaseTraceabilityTest、release controller test、SP-4 named/Playwright。

### TC-12 最终放行原子性、幂等与可追溯

- test_case_id: `TC-12`
- mapped_task_ids: `[T9, T10]`
- mapped_acceptance_ids: `[AC-24, AC-25, AC-26, AC-27, AC-28, AC-31, AC-34]`
- environment or setup: 完整四报告、签核证据、五状态数据、故障注入、超安全整数 ID。
- steps: approve；重放/竞争；每写点失败；查询 trace（完整/缺失/篡改参数）；模拟刷新失败。
- expected_result: task/transaction/application 原子 RELEASED；同键幂等；未放行永不出 trace；成功后即时可见；刷新失败不改成功事实。
- evidence: release service/trace tests、SP-4 named/Playwright、只读 API/DB 核验。

### TC-13 全链路真实多账号 E2E

- test_case_id: `TC-13`
- mapped_task_ids: `[T11]`
- mapped_acceptance_ids: `[AC-01, AC-03, AC-04, AC-07, AC-09, AC-14, AC-16, AC-18, AC-20, AC-21, AC-24, AC-25, AC-26, AC-27, AC-28, AC-30, AC-32]`
- environment or setup: 先核验当前 `int_main` Jar/Vite 与监听来源；测试租户；生产组长、zhulijiang、来料、灭菌、成品、xujianhai、非候选账号；另备第二测试租户账号用于隔离负向验证。准备三条任务自有订单：双 100% 主链、双 100% PQC 拒绝、进度不足；四附件、灭菌批号、签核证据和明确的页面清理计划。
- steps: Playwright 依次登录并完成组长→PQC→四报告→管理者→trace；记录每个目标写请求；最后只读核对和清理。
- expected_result: 页面全链路通过；角色边界、状态、对象 ID、四附件、RELEASED 和 trace 一致；无 API-only/SQL 推状态/mock。
- evidence: Playwright result JSON、截图、目标请求/响应身份、只读核验、清理结果。

## 用户手动验收执行单（待执行）

本节用于用户后续在 `int_main` 当前代码上执行真实页面验收。它不替代 TC-13 的 Playwright 结果、后端定向回归或独立 tester 门禁；任何前置缺失都必须在产生业务写入前记录 `BLOCKED`。

### 固定前置

1. 记录当前 `int_main` 提交、前后端运行产物来源、监听 PID、测试租户和开始时间；运行态必须包含本任务当前代码，不能复用旧 Jar 或其它分支产物。
2. 准备七个相互独立且可登录的测试账号：生产组长、PQC、来料检负责人、灭菌负责人、成品检负责人、管理者代表和非候选账号；另准备一名不归属目标订单的生产组长，以及第二测试租户的同名或同角色账号用于隔离检查。
3. 准备三条可清理的任务自有订单：双 100% 主链订单、双 100% PQC 拒绝订单、至少一项进度不足订单；主链订单必须有冻结路线和三类正式来源，不能用 `formBindings`、默认 `MAIN` 或工序开始配置替代批记录表单来源。
4. 准备四份可识别的真实测试附件、灭菌批号和管理者 64 位 SHA-256 签核证据；记录文件名与哈希，但不得把密码、token 或其它秘密写入任务记录。
5. 在任何写入前确认测试数据命名、数据归属和清理负责人；缺少任一项时停止，不创建临时业务数据、不改 SQL 状态。

### 页面执行顺序

1. 以生产组长登录，先验证进度不足订单不能提交且显示结构化原因；再验证非归属订单不能提交。对主链和拒绝订单分别从工作台发起申请，记录页面状态、申请 ID、任务 ID 和每个目标写请求。
2. 以非候选账号登录，确认看不到或不能执行 PQC 处理；以 PQC 登录，先拒绝拒绝订单并确认无下游任务、不可重开，再通过主链订单并记录唯一批次、三类正式来源、四个报告任务和四类节点类型。
3. 分别以三类负责人进入真实待办页，确认只能看到并完成本人任务；上传四份附件并记录文件名、哈希、版本、任务状态。前三份完成后不得出现管理者任务；第四份完成后必须原子出现一个管理者任务和一个放行事务。尝试目标链路的跳过、删除待上传、撤回或覆盖时必须失败且状态不变。
4. 以非候选账号确认不能最终放行；以管理者代表登录，确认目标任务不提供拒绝、退回或撤回，提交签核后记录 `RELEASED`、放行事务 ID、批次 ID、四份附件和电子签名。
5. 在表单追溯页确认请求固定包含 `completedTraceOnly=true` 与 `releaseStatus=RELEASED`，主链订单即时可见、拒绝订单不可见，且四附件和电子签名可查看。
6. 在第二租户重复只读查询或按计划的最小写入验证，确认同名账号、订单号或批号不能跨租户读取、处理或泄露详情；跨租户请求必须显示无权或不存在。
7. 完成后使用页面和只读核验确认申请、任务、批次、放行事务、附件和审计一致；按预先记录的清理计划清理全部任务自有数据，并记录清理结果。

### 最低证据映射

| 验收范围 | 对应 AC | 必须记录的真实证据 |
| --- | --- | --- |
| 进度、归属、回执与刷新分层 | AC-01、AC-02、AC-04 至 AC-06、AC-27、AC-28、AC-29 | 页面截图、目标请求/响应、申请/任务字符串 ID、失败原因 |
| PQC 正反权限、拒绝、通过和批次来源 | AC-03、AC-07 至 AC-13、AC-19、AC-30、AC-31、AC-34 | 两账号页面结果、拒绝终态、通过后批次/映射/审计只读核验 |
| 四报告上传与第四份交接 | AC-14 至 AC-18 | 四账号页面、附件哈希/版本、四个任务状态、最终任务/事务数量 |
| 管理者放行、快照、追溯与真实主链 | AC-20 至 AC-26、AC-32 | 非候选与候选页面、签核结果、`RELEASED`、追溯请求参数、附件/签名截图 |
| 三条配置链路和无降级回归 | AC-29、AC-33 | 工序开始上传人、逐工序批记录表单、`formBindings` 三条独立页面/只读证据；异常无默认成功 |
| 数据清理与完整性 | AC-30、AC-31、AC-34 | 测试租户隔离记录、审计记录、清理前后只读结果 |

执行完成后，用户需将每一步的通过/失败、截图或结果文件位置、申请/任务/批次/事务 ID 和清理结果交给独立 tester 复核；缺任何 AC 的真实执行或独立证据时，P11 必须继续保持 `blocked`。

### TC-14 回归和无 fallback 扫描

- test_case_id: `TC-14`
- mapped_task_ids: `[T11]`
- mapped_acceptance_ids: `[AC-28, AC-29, AC-31, AC-33]`
- environment or setup: 完整源码和受影响模块测试集。
- steps: 复跑 team leader、writer、WorkTask、special-node、release/precheck、trace 测试；扫描用户名比较、formBindings 替代、默认管理员、吞异常和 default-success。
- expected_result: 相邻能力无回归；三配置链路独立；无未授权 fallback；所有失败可诊断。
- evidence: Maven/Node/TypeScript 回归命令、扫描输出、git diff check。

## Required BDD Scenarios

- BDD: 双 100% 后只创建 PQC 待办 -> Given 合法组长且双 100% / When 点击完工 / Then 只有申请和 PQC 待办。
- BDD: PQC 通过后才生成正式批次资料 -> Given PQC 待审申请 / When 角色候选通过 / Then 唯一批次、三类映射、四报告任务原子生成。
- BDD: PQC 拒绝终态 -> Given PQC 待审 / When 填写原因拒绝 / Then 无下游且不能重开。
- BDD: 三类负责人完成四份报告 -> Given 报告上传阶段 / When 三类账号完成四节点 / Then 前三份不创建最终待办，第四份原子创建。
- BDD: 管理者代表放行后可追溯 -> Given 四报告齐套且快照一致 / When 管理者角色候选通过 / Then RELEASED 且 trace 可见。
- BDD: 旧数据和正式来源缺失必须阻塞 -> Given 旧关系不明或仅动态槽位 / When 尝试推进 / Then 明确 blocker 且无部分写入。

## Verification Command Families

- Backend targeted: 从 worktree 根执行 `mvn --file IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=<real-test-class>" "-Dsurefire.failIfNoSpecifiedTests=false" test`，每条命令独立检查退出码。
- Frontend named: 在 `IntRuoyiFronted` 执行 `pnpm test <registered-target>`；先验证 target/spec 存在。
- Frontend type: 按前端规则运行受影响范围检查；全量 `pnpm ts:check` 仅在资源前置满足时执行并单独记录。
- E2E: 按实际 package script 和 spec 文件执行；未确认 script/route/account/data 前不得记录业务 RED/PASS。
- Final: 受影响 Maven 回归、所有 named tests、类型检查、真实多账号 E2E、只读核验、`git diff --check`。

## Independent Test Gate

- tester 不得是对应任务 executor，也不得修代码。
- 每个任务必须有 execution-log 的真实命令/结果和 test-report 的独立结果，才能在 task-state 标记 completed。
- 任一 AC 缺执行或独立测试证据、系统级 E2E 未通过、或 blocker 未清空时，总任务不得 completed。
