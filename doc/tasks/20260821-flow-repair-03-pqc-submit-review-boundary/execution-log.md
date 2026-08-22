# Execution Log

## User Intent

用户要求只做流程修复 3 的代码审计、需求澄清和开发文档设计，冻结一线 PQC 提交与 PQC 组长复核只形成过程检验来源事实的边界，并向流程 4、6、7、8、9、10、11 提供字段级契约。禁止生产代码、数据库、服务运行和写入型 E2E 变更。

最新复核要求进一步冻结：活跃订单与独立场景两个建批分支；流程 7 独占放行前 Origin/TraceLink、适用 PQC 映射及放行后追溯读模型；流程 10 只拥有最终放行、签名、CAS 和审计；多入口不得旁路统一门禁。

## Command And Action Intent

- 读取 `AGENTS.md`：确认项目级无 fallback、任务文档、BDD/TDD、PQC 业务术语和收尾规则。
- 读取 `docs/task-closeout-rules.md` 与 `docs/experience-index.md`：确认文档结构、状态和经验门禁。
- 读取 `docs/product/production-role-system-operations.md`：确认四角色只形成来源、双进度完成后统一回填的产品裁定。
- 读取 `docs/backend-development.md` 相关章节：确认正式来源、结构化汇集、设备快照、幂等和迁移阻断。
- 读取 `docs/frontend-development.md`：确认前端 PQC 任务、逐件样本、设备可选性、签名和连续提交门禁。
- 读取 `docs/e2e-rules.md`：只设计未来真实路径，当前不启动服务、不运行写入型 E2E。
- 只读检索后端 PQC 任务、逐件明细、组长复核、结构化汇集和批记录单元格来源映射。

## BDD Scenarios

- `BDD: 一线PQC按正式任务提交结构化来源 -> Given 活跃订单、冻结路线工序、QA规程版本、PQC任务和签名身份均有效，When 一线PQC提交完整逐件结果与所选设备快照，Then 系统只生成一次待复核来源事实并冻结内容哈希，不写正式过程检验单。`
- `BDD: PQC组长确认同一来源事实 -> Given 待复核来源属于当前租户和组长人员范围且来源版本未变化，When PQC组长确认，Then 同一事务写复核审计、将来源推进为已确认并生成唯一结构化汇集版本，不写正式过程检验单或批次执行。`
- `BDD: PQC组长退回后重新提交 -> Given 待复核来源被明确退回，When 一线PQC基于新修订号重新提交，Then 原版本保持可追溯且不再具备消费资格，新版本使用新的内容哈希和版本号进入待复核。`
- `BDD: 终态后重复命令幂等 -> Given 同一业务幂等键和相同请求内容已成功，When 客户端重试，Then 返回同一提交或复核结果且不新增记录；相同幂等键不同内容明确冲突。`
- `BDD: 并发复核只有一个胜者 -> Given 两个PQC组长基于同一expectedVersion同时复核，When 两个命令竞争，Then 只有一个状态转换成功，另一个得到版本冲突且不产生孤立复核或汇集明细。`
- `BDD: 流程4完成节点只消费确认版本 -> Given 流程2已完成生产事实复核、活跃订单生产和检验进度均为100%且存在唯一已确认结构化PQC来源，When 生产组长在流程4完成节点执行统一回填，Then 产生唯一 formalProcessInspectionDocumentId 和 completionBackfillReceipt；流程3不执行回填。`
- `BDD: 流程6只消费完成回填凭证 -> Given 流程4已原子完成批记录、正式过程检验单及适用损耗单回填，When 流程6收到 completionBackfillReceiptId/hash/version，Then 仅创建或复用 batchExecutionId，不直接读取流程3 aggregate。`
- `BDD: 缺正式来源时阻断 -> Given 逐件明细、确认记录、规程版本、设备快照或来源身份任一缺失/不一致，When 提交、复核或完成节点尝试继续，Then 系统明确失败且不得从raw payload、当前配置、旧IPQC、formBindings或生产提交推断。`
- `BDD: 两个合法建批分支 -> Given 活跃订单持有流程4 completionBackfillReceipt 或独立场景持有流程9 IndependentBatchPrerequisiteReceipt，When 流程6创建/复用批次，Then 活跃订单缺凭证/快照冲突分别返回 BACKFILL_RECEIPT_REQUIRED/SOURCE_SNAPSHOT_MISMATCH，独立入口缺失/无效分别返回 ENTRY_PREREQUISITE_MISSING/ENTRY_SOURCE_INVALID，且不得伪造 activeOrderId 或流程3 aggregate。`
- `BDD: 放行前映射硬门禁 -> Given 流程6已建批但流程7 Origin/TraceLink、适用PQC映射缺失或hash不一致，When 上传材料或申请放行，Then 分别传递 TRACE_MAPPING_BLOCKED/TRACE_SOURCE_CONFLICT 且流程8/10均阻断；流程7 READY 后才进入四材料门禁。`
- `BDD: 多入口统一放行 -> Given 批次详情、PQC/生产申请、管理者代表批准或独立批次入口，When 用户继续放行，Then 入口只能适配统一流程8/10，gate未满足/快照冲突分别返回 RELEASE_GATE_BLOCKED/RELEASE_SNAPSHOT_MISMATCH，且流程3/PQC组长不能改变批次、材料或RELEASED。`
- `BDD: 批次映射与放行追溯 -> Given 流程7放行前映射READY、流程8四份固定材料齐全且流程10管理者代表已签名，When 流程10放行并由流程7查询追溯，Then 可追到对应场景正式来源；活跃订单分支还可追到正式过程检验单、PQC确认汇集和逐件设备快照。`

## TDD Evidence Status

- `RED: planned / NOT RUN -> 后续实施先新增服务与合同测试，证明当前终态规则、版本消费、两类receipt判别、pre-release映射或多入口旁路至少一项未实现。`
- `GREEN: NOT RUN -> 后续实现满足条件后的预期结果：最小正式实现满足合同后，相同聚焦测试应通过。`
- `REGRESSION: planned / NOT RUN -> 运行PQC提交/复核、活跃订单与独立场景建批、流程7映射、四材料、多放行入口、最终CAS和追溯查询相邻测试。`
- 本任务只做文档，不运行生产代码测试、服务、数据库或写入型 E2E。

## Audit Evidence

- `MesTeamLeaderSubmissionReviewServiceImpl`：PQC 组长批准时调用结构化汇集；访问范围、事件类型有校验，但终态后再次复核的有效版本规则未闭合。
- `MesPqcProcessInspectionAggregationServiceImpl`：在事务内校验事件、任务和逐件明细，以 CAS 将记录推进到已汇集、任务从 `SUBMITTED` 推进到 `CONFIRMED`，再写结构化汇集明细。
- `MesPqcInspectionTaskDO`：保存活跃订单、工单、路线/版本/工序、QA 工序、规程版本、检验类型、业务日期、班次、轮次、数量、提交哈希和事件身份。
- `MesPqcInspectionPieceDetailDO`：保存样本、项目、方法、标准、设备身份、上下限、单位、精度、结果类型、实测值和判定。
- `MesPqcProcessInspectionAggregateDetailDO`：保存复核、任务、活跃订单、工单、路线/规程/轮次、逐件值和设备快照来源链。
- `MesProBatchRecordCellLinkServiceImpl`：把 `PQC_AGGREGATE_DETAIL` 作为可映射来源，证明汇集明细不是正式过程检验单本身。

## Milestone Updates

- M1 规则与产品资料：`completed`。
- M2 当前代码只读审计：`completed`。
- M3 设计文档职责修订：`completed`。
- M4 文档验证：`completed`；最新结构扫描未发现待决定 owner、旧统一前置误读、流程 10 追溯所有权、四份材料门禁冲突或未运行 GREEN PASS 误报。
- M5 closeout: `completed`；旧凭证名、旧对外错误码和过时材料口径扫描为 0；cleanup preview/apply 保留五份文档，删除、blocker、warning 均为 0。

## Current Blockers

- 业务语义、字段身份、状态 owner、终态/受控修订、两个建批分支、四份材料和禁止替代规则已冻结；独立入口 canonical 凭证为 `IndependentBatchPrerequisiteReceipt`，跨线程失败必须传递已冻结稳定码，具体载体名称可由实现统一映射。
- 生产代码、自动化测试、真实 E2E 和历史迁移对账证据尚未完成；这些实现/验证 blocker 不阻止本文档定稿，但阻止生产 GREEN、迁移通过或放行结论。



## Project Experience Consolidation

- Existing backend formal-source and PQC aggregation gates already cover the durable lesson; no long-term experience document was changed or created.
- 本轮多入口和状态所有权是当前业务合同，用户范围又限定为五份任务文档，因此不改长期经验文档。

## Main-Thread Implementation And Verification Addendum (2026-08-22)

本阶段在用户授权后执行了流程 3 task-owned 代码实现、提交、融合和主线程验证；不把流程 4/6/7/8/9/10/11 的缺口伪装成流程 3 通过。

### Ownership And Commits

- 流程 3 实现提交：`d809c9995`；集成到当前 `int_main` 的提交：`aeb58c37d`。
- 当前主线程 HEAD：`1197ce3e0ee0b63c8fdcfb51bcf2bc80e9e9bfed`；无需重复融合流程 3。
- 主干后端整合提交：`8759b45f9`；流程 3 任务记录保留提交：`1197ce3e0`。
- task-owned 代码仅冻结 `sourceRevision=submittedEventId`、`payloadHash=submittedContentHash` 的提交回执/只读结果和终态复核幂等冲突行为；流程 3 不创建正式过程检验单、批次、材料或 `RELEASED`。

### BDD/TDD Evidence

- `BDD: 一线 PQC 提交与组长复核来源边界 -> Given 正式任务、逐件事实、设备/签名快照和租户范围有效，When 提交、确认或重试，Then 只产生不可变来源/aggregate，不提前回填正式单或建批。`
- `RED: 流程 3 提交回执身份与幂等冲突测试（实现前） -> FAIL，原回执未暴露 sourceRevision/payloadHash 契约；该 RED 属于 task-owned 实现前证据。`
- `GREEN: mvn -f IntRuoyiBackend/yudao-module-mes/pom.xml -Dtest=MesFrontlinePqcSubmissionConcurrencyTest,MesFrontlinePqcContextServiceTest,MesFrontlinePqcSubmitReceiptControllerTest,MesTeamLeaderSubmissionReviewServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，27/27。`
- `REGRESSION: mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dmaven.test.skip=true compile -> PASS；流程 3 相关源码可在当前主线程编译。`
- `REGRESSION: python -X utf8 -m pytest IntRuoyiBackend\\script\\tests\\test_branch_runtime_profile.py --basetemp <task-owned writable temp> -> PASS，17/17。`
- `REGRESSION: scripts\\preflight\\branch-runtime-port-guard.ps1 -> PASS，int_main/int_main: 8081/48081。`
- `git diff --check -- IntRuoyiBackend -> PASS`；未运行服务、写入型 E2E 或生产数据库操作。

### Non-Task-Owned Blockers

- 流程 4 的双 100% 完成节点统一回填、流程 6 的两类 receipt 建批、流程 7 的放行前映射/过程检验记录、流程 8 四份材料门禁、流程 10 最终放行和流程 11 总体验证仍需各线程实现并验证。
- 历史 `CONFIRMED`/aggregate/正式单据/批次执行的迁移对账尚未完成；无法证明来源链完整的数据必须保持 migration blocker。
- 真实角色页面 E2E 尚未运行；不能用 API-only 或静态测试代替。
- 主线程默认 pytest 临时目录存在 Windows ACL blocker（`WinError 5`）；使用任务自有可写 `--basetemp` 后 17/17 通过，未修改系统 ACL。

### Closeout Evidence

- task status transitioned `ready_for_closeout -> completed` after verification.
- `task-closeout-cleanup.py --mode preview` listed only the task-owned pytest output for deletion after the eight task records were explicitly kept.
- `task-closeout-cleanup.py --mode apply` deleted only that temporary pytest output; no production code, formal test, database, runtime registry or unrelated task file was touched.
