# 流程修复 10：放行终态与追溯闭环

## 任务目标

- 只读审计放行完成后的最终状态、权限、并发、快照、审计和追溯出口。
- 设计多放行入口共用的唯一终态合同，并与流程修复 7、8、9、11 对接。
- 本轮只处理流程10终态追溯分区回归；不修改数据库、不运行写入型 E2E，不改变流程7负责的 Origin/TraceLink 来源映射。

## 里程碑

- [x] M1：读取项目规则、产品规则、开发规则和适用经验门禁。
- [x] M2：完成当前代码与相邻流程任务的只读审计。
- [x] M3：完成目标态、根因、修改边界、接口/数据/状态设计。
- [x] M4：完成 BDD、RED/GREEN/REGRESSION、迁移/回滚和 blocker 设计。
- [x] M5：完成文档结构与一致性验证。

## 预期验证

- 指定任务目录包含 `task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md`。
- 五份文档覆盖用户要求的全部设计主题，且状态、接口、幂等、追溯和跨线程合同互相一致。
- 实现验证使用 Maven 定向合同测试、yudao-server package 和只读启动/health smoke；不运行写入型 E2E。

## 适用经验门禁

- 正式来源：完成节点前不得回填或创建批次，批记录只用工序正式绑定来源，三类回填成功后才创建/复用批次。
- 终态待办：`CLOSED`、`ARCHIVED`、`REJECTED`、`VOIDED` 对象不得保留可办入口。
- 放行资料：四份独立正式材料必须齐套；不得以旧“三份材料”表述、前端开关或模拟文件降级。
- E2E：后续实现仅可在任务自有测试环境执行真实多角色和签名凭据路径；本次只读文档任务未执行 E2E。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以唯一状态所有者、统一终态命令、正式来源快照、条件唯一约束和可验证追溯为主线。
- 是否存在临时补丁或绕过：否。

### 本轮权威上下文实现（2026-08-25）

- `MesReleaseAuthoritativeContextPortImpl` 现在先读取已存在的流程6批次并要求 `READY_TO_CLOSE/CLOSED`，再读取流程7 Origin/TraceLink 预检，最后从 `MesReleaseMaterialGateReceiptPort` 读取流程8持久化 `MATERIALS_READY` 凭证。
- 活跃订单入口才校验流程4 `BACKFILL_SUCCEEDED`、领料绑定、双进度和三类回填；MANUAL/SCHEDULED/PQC_INDEPENDENT 入口改为读取并验证 canonical `IndependentBatchPrerequisiteReceipt`，不要求伪造 activeOrderId、pickListId 或 completionBackfillReceipt。
- 流程8持久化 receipt 端口尚未由流程8 owner 提供时，流程10返回结构化 `AUTHORITATIVE_RECEIPT_CONTEXT_REQUIRED`；不从四份材料临时计算 receipt，不使用请求体嵌套对象，也不写默认成功。
- 本轮 Maven compile：PASS；新增测试因现有非流程10测试源语法错误尚未执行，已记录在 execution-log 与 verification-report。

### 本轮打包和运行态 smoke（2026-08-25）

- 当前 `int_main` HEAD：`d3a1fecca83cb87b80d8002593cf7a291fb2593b`。
- `yudao-server` 使用 `maven.test.skip=true` 打包成功；这是为了隔离既有非流程10测试源错误，不是测试通过证明。
- 新构建的 `yudao-server-exec.jar` 实际启动成功，日志出现 `Started YudaoServerApplication`，48081 曾监听，`/actuator/health` 返回 HTTP 200 `{"status":"UP"}`；本轮启动的进程已停止。
- 已有配置 smoke 仍证明 `MesReleaseAuthoritativeContextPort` 恰好一个 Bean；新增权威上下文组合测试暂受既有 MES/BPM 测试源错误阻断。
- 流程8未提供持久化 `MATERIALS_READY` receipt 适配器前，流程10继续返回结构化 blocker，绝不默认放行。

## Current Status

ready_for_closeout：流程10专项实现、融合和主线程验证已完成；本轮补强了权威上下文读取边界；跨流程权威凭证适配、迁移、outbox 和全链路 E2E 仍 No-Go。

### 权威上下文接口冻结（2026-08-24）

- 已冻结 HTTP 输入边界：客户端只能提交 batchExecutionId、凭证 ID/hash 和最终化控制字段，不能提交可被采用的嵌套完成、独立批次或材料凭证。
- 流程10只能通过 MesReleaseAuthoritativeContextPort 从流程4/6/7/8持久化 owner 读取正式快照；流程6提供已持久化 batchExecutionId，流程7映射必须 READY，流程8必须 MATERIALS_READY。
- 本轮仅增加 @JsonIgnore 输入阻断和合同测试，不改变 finalizeRelease 主事务，不新增凭证解析器。
- 验证：合同单测 1/1 PASS；权威上下文组合回归 11/11 PASS。
- 状态保持 ready_for_closeout；持久化 owner 适配器未接入仍为结构化 No-Go。

### 终态分区回归复核（2026-08-23）

- 当前验证基线：int_main HEAD 7770f36fb6ed64f4e306320410d131f184cf2789。
- 根因已闭合：批次追溯的 completedTraceOnly 不再强制只查 RELEASED；现在同时接受 RELEASED 决策和 ARCHIVED/REJECTED 批次终态，仍排除 VOIDED。放行事实写入仍由流程10 finalizeRelease/finalizeApproval 负责，流程7的 Origin/TraceLink 来源映射不在本轮修改边界。
- 单类 GREEN：MesProEdhrTraceTerminalPartitionContractTest 2/2 PASS。
- 流程10原定向合同回归：47/47 PASS；加入终态分区和配置 smoke 后扩展回归：49/49 PASS。
- yudao-server package：BUILD SUCCESS。
- 48081 已由既有 runtime-control 服务监听，/actuator/health 返回 {"status":"UP"}；本轮未停止该非本轮启动的长期服务。
- 本轮没有新增默认成功 Bean；流程4/6/8权威适配器缺失时仍由结构化 blocker fail-fast。
- 独立启动日志：E:/IntRuoyi/output/runtime/int_main/bean-fix-20260823-1551/logs/yudao-server.log；只匹配 Started YudaoServerApplication 和“项目启动成功”，未匹配 APPLICATION FAILED、BeanCreationException 或 MesReleaseAuthoritativeContextPort 缺失。

### 启动 Bean 注册修复（2026-08-23）

流程10已在 `9b18ee093` 中将 `MesReleaseAuthoritativeContextPort` 改为主应用明确导入的 `@Configuration/@Bean` 注册；实现类不再依赖组件扫描时机。无流程4/6/8权威适配器时，唯一 Bean 仍为结构化 blocker 实现，不返回默认成功或放行。

- 配置 smoke test：`MesReleaseAuthoritativeContextConfigurationTest`，2/2 PASS（唯一 Bean 和结构化 blocker）。
- 流程10定向合同回归：47/47 PASS。
- `yudao-server` Maven package：BUILD SUCCESS。
- 实际启动：48081 监听，`/actuator/health` 返回 `{\"status\":\"UP\"}`。
- 运行时 JAR 与当前构建产物中配置类、blocker 类 SHA-256 一致。
- 启动日志无 `MesReleaseAuthoritativeContextPort` 缺失 Bean 或 `APPLICATION FAILED TO START`。

### 主流程统一冻结合同（2026-08-22）

流程10拥有唯一最终放行状态和 release manifest/签名审计。活跃订单关系仅在适用时要求；独立批次不得因无 activeOrderId 被拒绝。独立追溯显示 originType、独立凭证、工单/路线/批号、来源快照、适用事实、三类回填、四材料版本/hash、放行决定和审计链；不适用关系返回 `NOT_APPLICABLE`+原因码，应有关系缺失返回 `MISSING/BLOCKED`。

流程10专项实现、融合和主线程验证完成：代码以 `7f3547c17` fast-forward 融合到 `int_main`；主线程 Maven compile BUILD SUCCESS，流程10 focused suite 45/45 PASS，流程6/8/9/审批中心合同 suite 29/29 PASS，commit diff-check 与 runtime guard PASS。流程4/6/8权威凭证适配器、生产迁移/历史回填、outbox 投递和全链路真实 E2E 仍为 No-Go；本状态不宣称全链路完成。

## Cleanup Keep

- doc/tasks/20260821-flow-repair-10-final-release-state-and-trace/development-plan.md
- doc/tasks/20260821-flow-repair-10-final-release-state-and-trace/test-plan.md

## 修改边界

- 本专项实现已修改流程10生产代码、对应测试和迁移脚本，并以 task-owned commit `7f3547c17` 融合到 `int_main`；后续仅可由各领域 owner 补齐权威适配器、迁移/outbox 和真实 E2E。
- 禁止：直接改写 ERP 领料事实、伪造流程4/6/8凭证、默认成功、绕过权限/签名或运行写入型 E2E。
