# 执行记录

## 用户意图

将流程1-11所有尚未进入主干的代码融合到 `D:/IntRuoyiWorktree/xiufu20260826`，已在主干的代码不重复处理。

## BDD

BDD: 流程代码选择性融合 -> Given 目标 worktree 基于当前 int_main 且初始干净；When 逐个核对流程分支并融合不在主线的代码提交；Then 目标 worktree 包含应融合的流程代码且不包含已在主线的重复提交、无关文档或并行改动。

## 初始证据

- 目标 worktree：`D:/IntRuoyiWorktree/xiufu20260826`
- 目标分支：`codex/xiufu20260826`
- 初始基线：`2faf0f33234614f46867e5d23e450c41ef62cc1f`
- 主工作树：`E:/IntRuoyi`，本任务不修改。

## 命令记录

- 已读取 `docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/powershell-memory.md`、`docs/task-closeout-rules.md`。
- 已创建目标 worktree，初始化期间等待 Git checkout 完成，最终状态干净。

## 候选提交核对

- 流程1、2、7、8、9、10、11：对应代码或验收提交已经在 `int_main`，不重复融合。
- 流程4：`ac93ad0f6` 的流程4代码与主干已有 Tx-A 结果持久化内容一致；流程4当前仍有后续收敛任务，不重复导入旧版本。
- 流程3：只融合 `b2c800336` 的流程3测试 fixture 文件。
- 流程5：只融合 `ae33715ed` 中流程5自有的测试 fixture 和测试 SQL；排除该提交中的 `docs/worktree-memory.md`。
- 流程6：融合 `9c74c6b0f` 和其后续 `7358d340e`，因为权威解析器和测试链尚未进入 `int_main`。
- 流程8、10旧分支基于更早主线，包含相对当前主线的删除，不能整体合并；其已验证代码以当前 `int_main` 为准。

## 选择性融合原则

只融合上述 task-owned 代码和测试文件。任何会删除当前主线较新代码、包含并行任务、运行产物或仅文档历史的分支均排除，并记录排除原因。

## 当前阻塞

- 初始目标 worktree 基线中的 runtime guard 是 v6，而共享登记表是 v7；已在目标 worktree 补齐 v7 runtime 基线，等待正常提交复验。

## 融合结果

- 已应用流程3 `b2c800336` 的测试 fixture，并按当前主线补齐签名密码 mock、删除过时的 `applyConfirmedAllocations` 断言。
- 已应用流程5 `ae33715ed` 的两个 task-owned 测试 fixture/SQL 文件，排除 `docs/worktree-memory.md`。
- 已应用流程6 `9c74c6b0f` 和 `7358d340e` 的权威建批解析器、入口合同、服务和测试代码。
- 流程1、2、4、7、8、9、10、11没有重复导入：对应代码已在主干，或旧分支相对当前主干会删除较新代码。

## 验证证据

- RED: 未加引号的 Maven `-Dtest` 多类参数 -> PowerShell 参数解析失败；随后改用引号包裹参数。
- RED: 首轮融合定向测试 -> 流程3 6 failures/2 errors，原因是当前主线新增签名密码必填及过时的完成服务断言；未修改生产代码。
- GREEN: `mvn -o -pl yudao-module-mes -am -DskipTests compile` -> `BUILD SUCCESS`，24/24 modules。
- GREEN: 流程3定向测试 -> `8/8 PASS`。
- GREEN: 流程3/5/6组合定向测试 -> `222/222 PASS`，0 failures、0 errors。
- GREEN: `git diff --check` -> PASS。
- NOT RUN: 完整流程4/7/8/10真实联调、数据库迁移、真实租户 Playwright E2E；本任务只做代码融合，不把定向测试冒充全链路验收。

## Runtime 基线复验

- 目标已按 v7 登记：slot 43，前端 `8258`，后端 `48258`。
- 目标 worktree 已同步 v7 所需的 runtime 文档和 guard/profile 文件。
- `branch-runtime-port-guard.ps1`：PASS。
- 未修改共享 runtime 登记表，不使用 `--no-verify`。

## 流程8材料 receipt 里程碑

BDD: 四份材料形成权威回执 -> Given 批次执行的来料检、灭菌、成品检报告、成品检记录均已批准且附件 hash/来源快照有效；When 门禁评估返回 MATERIALS_READY；Then 服务端持久化版本化 receipt，流程10只能按 tenant、batch、receiptId、sourceSnapshotHash 和 receiptHash重新读取。

RED: `MesReleaseMaterialGateReceiptPortImplTest` -> FAIL，正式 DO/Mapper/adapter 尚不存在。
GREEN: `MesReleaseMaterialGateReceiptPortImplTest`、`MesProEdhrFourMaterialGateServiceTest`、`MesProEdhrReleaseServiceImplTest`、`MesReleaseAuthoritativeContextPortImplTest` -> `43/43 PASS`。
GREEN: MES compile -> `BUILD SUCCESS`。
GREEN: `MesReleaseMaterialGateReceiptSqlContractTest` -> `1/1 PASS`。

实现内容：

- 新增 `mes_pro_edhr_material_gate_receipt` 持久化表、DO、Mapper 和 receipt adapter。
- MATERIALS_READY 评估成功时生成不可变版本 receipt；相同来源和 manifest 重复评估幂等。
- 预检快照保存 receiptId、receiptHash、版本集合 hash；流程10在请求缺少 receiptId 时只从持久化预检快照读取。
- 租户、批次、来源快照、材料类型集合和 receipt hash 任一不匹配均返回阻断，不信任请求体材料对象。

## 流程4/7联动里程碑

BDD: 活跃订单 release dossier 不重复回填 -> Given 活跃订单已在点击完成节点形成成功 Tx-A receipt；When release dossier 规划和写入；Then 只读取并返回 Tx-A 的三类回填证据，不调用旧批记录、过程检验或损耗 writer。

GREEN: `MesPqcReleaseDossierPortImplTest` -> `1/1 PASS`。

BDD: 建批成功自动触发 Tx-C -> Given 流程6已持久化成功建批审计；When production-release 建批事务提交；Then 发布 provisioned 事件，由流程7唯一 Tx-C producer 在 AFTER_COMMIT 消费正式审计和来源绑定，成功或失败均由流程7 outbox 记录。

GREEN: MES 24模块 compile -> `BUILD SUCCESS`；Tx-C producer 和事件监听代码通过编译。
GREEN: `MesProEdhrBatchTraceTxCProducerEventTest` -> `1/1 PASS`。

## 全融合回归

- 流程3、5、6、7、8、10相关定向集合：`307/307 PASS`，0 failures、0 errors。
- `git diff --check`：PASS。
- `mvn -o -pl yudao-server -am -DskipTests package`：`BUILD SUCCESS`，30/30 modules。
- 已将验证 Jar 复制到 `output/runtime/xiufu20260826/yudao-server-exec.jar`，SHA-256 为 `A8395D4D4A895492ABC5B97008C769C5F72D9A5C04197EE8743F458E9B5A5EBC`。
- 目标后端以该稳定运行 Jar 启动在 `48258`，进程命令行指向目标 worktree，日志出现 `Started YudaoServerApplication`；`GET /actuator/health` 返回 HTTP `200`、`{"status":"UP"}`。
- `branch-runtime-port-guard.ps1`：PASS，slot 43，前端 `8258`，后端 `48258`。
- 只读 schema 核对：本地 Docker MySQL 可连接，但 `mes_pro_edhr_material_gate_receipt` 尚不存在；正式迁移未执行，不能把 SQL 文件存在或应用启动成功写成迁移完成。
- 真实数据库迁移、真实 Tx-C outbox 写入闭环、真实租户 Playwright E2E：未完成。迁移属于数据库写入，当前任务没有明确授权执行本地业务库 DDL；真实租户、账号、四份测试材料和清理权限也未冻结。

## 集成状态

- 目标分支 `codex/xiufu20260826` 当前 HEAD：`24e2b06343c6a59d22b6f652e55a68ab0d40980b`。
- `int_main` 当前 HEAD：`24e2b06343c6a59d22b6f652e55a68ab0d40980b`，与目标分支一致。
- 主工作树 `E:/IntRuoyi` 在本轮复核时有 268 项 dirty/untracked 改动；流程负责人仍在并行写入，重叠范围继续变化。
- 重叠核对显示 `AGENTS.md`、`docs/branch-runtime-ports.md`、`docs/codex-branch-runtime-handoff.md`、`docs/worktree-restrictions.md`、`scripts/preflight/branch-runtime-port-guard.ps1`、`scripts/runtime/branch-runtime-profile.ps1` 的主工作树内容已与目标提交相同；以下 5 个代码/测试路径和 `docs/local-runtime.md` 仍与目标内容不同，不能在主工作树未分类提交前融合：`MesProEdhrBatchExecutionServiceImpl.java`、`MesProEdhrBatchTraceTxCProducer.java`、`MesProEdhrReleaseServiceImpl.java`、`MesPqcReleaseDossierPortImpl.java`、`MesProEdhrBatchExecutionServiceTest.java`、`docs/local-runtime.md`。
- 未在主工作树执行 merge、reset、stash、checkout、整体提交或清理；这些操作可能覆盖并行任务改动。
- 干净集成检查：目标 HEAD 是 `int_main` 的后代，`git merge-base --is-ancestor int_main HEAD` PASS；此前临时 clean worktree 的 fast-forward 检查也 PASS，检查目录和登记项均已删除。
- 主干 dirty 保真检查：将 `E:/IntRuoyi` 当前 tracked patch（约 740 KB）以三方方式应用到目标提交的临时 worktree；除 `MesProEdhrBatchExecutionServiceImpl.java` 的一个依赖字段冲突外，其余 tracked 改动均自动合并。冲突保留了流程6权威解析器、流程7事件发布器和主干已有的独立 receipt 服务字段，未覆盖任一方。
- 合并后快照编译在 BPM 模块失败：`FormCenterRuntimeServiceImpl.java:88` 引用缺失的 `FormTemplateFillRuleAutoDetectService`。该缺失来自主干并行 dirty 改动，流程1-11目标代码独立编译仍为 PASS；临时检查目录已删除，主工作树未修改。

## Flow4/Flow7 implementation follow-up

BDD: 放行批准只读完成回执 -> Given 活跃订单完成 Tx-A 已提交 `BACKFILL_SUCCEEDED` completion receipt；When 放行批准进入 dossier 适配器；Then 只按租户读取 Flow4 receipt，不再计划或写入批记录、过程检验和损耗三类旧 dossier writer。

GREEN: Flow4定向测试 `MesPqcReleaseBatchExecutionServiceTest, MesProductionReleaseApplySp1Test, MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortTest, MesPqcReleaseDossierPortImplTest` -> `30/30 PASS`。

BDD: 建批提交后触发 Tx-C -> Given 流程6成功持久化批次执行；When 建批事务提交；Then 发布只含 batch、事件/幂等键和持久化 witness 的 Flow7 event，由 `AFTER_COMMIT` application service 唯一调用 Tx-C producer，不能在提交前映射或重复触发。

RED: Flow7 application boundary contract -> FAIL，目标 worktree 尚无 witness-only event、application service 和 invoker；该 RED 由流程7 task-owned staged slice 提供。
GREEN: `MesProEdhrBatchTraceTxCApplicationServiceContractTest` -> `3/3 PASS`，并通过反射确认 listener phase 为 `AFTER_COMMIT`。
GREEN: Flow4/6/7/8/10/traceability combined suite -> `294/294 PASS`，0 failures/errors/skips。
GREEN: `mvn -o -pl yudao-module-mes -am -DskipTests compile` -> `BUILD SUCCESS`，24/24 modules。
GREEN: `mvn -o -pl yudao-server -am -DskipTests package` -> `BUILD SUCCESS`，30/30 modules；增量运行 Jar SHA-256 为 `51D2DAF5068F4333DA3D313354299A2796CB163B203359D5F200EB6E0BD52CAF`。
GREEN: 增量后端使用稳定运行 Jar 在 `48258` 完整启动，日志出现 `Started YudaoServerApplication`，health HTTP `200`、`{"status":"UP"}`；Flow7 application service/producer Spring Bean 链接成功。

实现边界：

- Flow4 `MesPqcReleaseDossierPort` 只暴露 `readCompletionReceipt`；旧 `MesPqcReleaseDossierPlan`、`MesPqcReleaseDossierWriteResult` 和旧 dossier writer 生产依赖已移除。
- Flow7 `MesProEdhrBatchProvisionedEvent` 只传递 source snapshot/bundle、completion receipt、credential ID/hash witness；producer 仍从持久化批次审计、Flow1领料绑定和正式来源重新读取事实。
- Flow6 仍拥有批次执行和 `BATCH_*` 状态；Flow7 不写批次状态，不接受客户端 raw receipt/payload。

## int_main promotion audit (2026-08-26)

- 以当前 `int_main` HEAD `2faf0f33234614f46867e5d23e450c41ef62cc1f` 和目标 HEAD `ae32b0be5455d2bb813f419243993a7afd5892a6` 创建临时三方快照；主工作树当前 268 项 dirty/untracked，索引无暂存改动。
- 主干 tracked patch 大部分可自动叠加到目标；三个冲突文件为 `MesProEdhrBatchExecutionServiceImpl.java`、`MesPqcReleaseDossierPort.java`、`MesPqcReleaseDossierPortImpl.java`。
- `MesProEdhrBatchExecutionServiceImpl.java` 的冲突可保留权威解析器、Flow7 publisher 和主干独立 receipt service 三个依赖；但两个 dossier 文件是语义冲突：主干 Stage2.5 模拟仍调用旧 `plan/planForActiveOrder/write` 三 writer，目标流程4已移除该生产写入接口。不能用 ours/theirs 覆盖，必须由模拟任务改为正式完成 receipt/Flow6 handoff，或明确排除该模拟切片。
- 临时提升 worktree 和 patch 已删除；`E:/IntRuoyi` HEAD、文件和 dirty 内容未修改。

## int_main fast-forward (2026-08-26)

- 前置复核：`int_main` 原 HEAD=`2faf0f33234614f46867e5d23e450c41ef62cc1f`，目标 HEAD=`d19199aa24e2d64bbaa25fbf62ada95f8e6d40d3`，`git merge-base --is-ancestor int_main codex/xiufu20260826` PASS；主干索引无暂存项。
- 使用带旧值校验的原子 `git update-ref refs/heads/int_main <target> <old>` 完成 fast-forward，并同步了本任务收尾文档；当前 `E:/IntRuoyi` 的 `int_main` 和目标分支均解析到 `cfde01b5ab1f1a6b2832f91d7af0b9ea5171cc31`。
- 没有执行 merge/reset/checkout/stash/clean，也没有提交主干 dirty/untracked 文件；主工作树现有改动保留，包含被排除的 Stage2/Stage2.5/Stage4/5/6 模拟切片。
- 目标 worktree 的 294 项回归、30模块打包和 `48258` 启动验证是融合前已通过的干净代码证据；主工作树仍存在 dirty overlay，不能以 `E:/IntRuoyi` 当前文件直接宣称运行态已刷新。

## Local schema migration verification (2026-08-26)

BDD: 四材料 receipt 表可部署 -> Given 本地 Docker MySQL `ruoyi-vue-pro` 中目标表不存在；When 执行正式 `20260826_mes_edhr_material_gate_receipt.sql`；Then 仅新增权威 receipt 表，不写入业务数据，并可重复执行。

GREEN: migration 通过 Docker MySQL `source` 执行，exit 0；`mes_pro_edhr_material_gate_receipt` 为 `InnoDB/utf8mb4_unicode_ci`，18 columns、4 indexes、0 rows。
GREEN: 同一 migration 第二次执行 exit 0；未执行 DROP、业务数据 DML 或远端操作。

影响范围：schema blocker 已解除；真实 Tx-C outbox 业务写入、真实租户/账号/四份材料和 Playwright E2E 仍未执行。
GREEN: migration 后使用稳定 Jar `51D2DAF5068F4333DA3D313354299A2796CB163B203359D5F200EB6E0BD52CAF` 在 `48258` 启动，日志出现 `Started YudaoServerApplication`，health HTTP `200`、`{"status":"UP"}`，随后优雅停止。
