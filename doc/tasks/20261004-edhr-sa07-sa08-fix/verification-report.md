# SA07 / SA08 Verification Report

## Current Status
review_ready — 实现及定向/相邻验证通过，待管理者独立 review 与集成。

## Result
基线 d3f1793c8831cc4d070b786553e3a2c002e17ca9；分支 codex/edhr-sa07-sa08-fix；工作目录 C:/Users/BJB110/.codex/worktrees/1c91/IntRuoyi。实际 Microsoft OpenJDK 17.0.20+8-LTS / Maven 3.9.16；使用本 worktree 自有 target。

原冻结版本命令 exit 0（历史证据）：**28 类 / 338 tests / 0 failures / 0 errors / 0 skipped**。final-regression.log 为当次输出；surefire-summary.json 从当次日志选取类名并逐类核对本次 Surefire XML，保留用例名称、时间和统计，未用旧报告推断通过。

## Implementation and acceptance
- SA07：新增 MesProductionSubmissionReadBinding，分别核验原始事件/source order/source snapshot 和查看目标的 CURRENT allocation/target snapshot。原签名绑定 A，B 用于正式分配关联；保留 tenant、route/process、operator、signer、domain、canonical/hash 核验，不补来源、不重写签名、不新增历史兼容。
- SA07 合同：真实 MES subject adapter、ElectronicSignatureServiceImpl、事件 writer、签名 query 与 H2 mapper 持久化；执行生产详情 XML SQL及真实详情 assembler。A/B 使用不同订单、工单与 routeProcessId。当前/归档详情及生产组长/PQC/批次三类证据入口均读取原冻结姓名；当前改名不覆盖签名姓名。来源移除、目标 inactive 后归档仍可读。无 CURRENT allocation、错 allocation tenant/workorder/process、错目标 route/snapshot、错来源 snapshot，以及真实 event 的 source/tenant/actor/operator/domain 和签名 evidenceHash 篡改均拒绝。
- SA08：核验真实 BPM/BPM_APPROVAL_TASK/APPROVE 证据及 EDHR/EDHR_WORK_TASK/sourceTaskId/businessKey、签名者、认证和完整性，沿申请/任务/事务/批次核对正式外键和 tenant。实际统一签名 ID 回传最终命令，贯穿 SQL approval_signature_id、decision、终端事件、业务审计、GXP 审计命令和 upstream 父签名 ID；删除 subject 替代 ID 的旧表达。
- SA08 合同：真实 BPM subject adapter / ApprovalSignatureRecordServiceImpl 写入统一记录，真实 MesProEdhrApprovalTaskAdapter 回调 → releaseService / manager prepare / signoff → H2 SQL finalization、正式审计 writer、详情 SQL及真实 evidence reader。入口不预填 signatureId；首次和重放使用同一真实 ID。重放不增加签名、decision 或审计，不重复上游关闭。错误 task/source/actor/tenant/hash 首次请求均拒绝，事务、申请、任务、decision 和审计零推进；DCC 和 OTHER_TASK 的有效签名也被拒绝。
- 相邻验证：inline MES 上市放行、PQC 正式写读及 release detail、员工切换/冻结身份/SA06 SQL、当前/归档详情、分配、上传职责、NCR/作废放行守卫及显式 P1 模拟边界均通过。

## Doubles and limits
签名 writer/adapter/query、来源关联、signoff、管理核验、finalization、事务/decision/终端事件/正式 operation audit SQL 和详情 assembler 为真实实现。认证、人员目录、图片存储、外部完工/物料 readiness/receipt 和上游服务为边界 doubles；GXP 验证实际审计命令的 record ID，未测内核落库。详情 mapper 桥接执行生产 XML BoundSql 和 JDBC 映射，H2 仅处理 CHAR/UNSIGNED、位字面量与 JSON 方言，不改变生产 SQL 绑定条件。

结果属于 Java17/H2/MyBatis 合同与回归，不声称真实 MySQL、页面 E2E、服务或并发验收。每条生产签名增加正式关联只读查询，未做负载测试。主工作区并行 P1 writer/Test、Clock/时区和已办完成者投影未复制、未修改，原冻结版本当时尚未做组合验证（当前管理者组合结果见下文）；集成应按精确 diff 保留 managerApproval 的并行 Clock 改动及 backend-development 既有增量，不能整文件覆盖。

## Original exact final command (338)
在 IntRuoyiBackend 目录执行：
```powershell
$env:JAVA_HOME='C:/Users/BJB110/.jdks/jdk-17.0.20+8'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
java -version *> ../doc/tasks/20261004-edhr-sa07-sa08-fix/java17-runtime.log
mvn -version *> ../doc/tasks/20261004-edhr-sa07-sa08-fix/maven-runtime.log
mvn -pl yudao-module-mes -am test '-Dtest=MesSa07Sa08SignatureContractTest,MesSubmissionSignatureIdentityReaderTest,MesActiveOrderSignatureEvidenceServiceTest,MesSa06IdentityMapperTest,MesTeamLeaderActiveOrderDetailServiceImplTest,MesProEdhrBatchActiveOrderDetailServiceTest,MesPqcReleaseOrderDetailServiceTest,MesProductionReleaseSignoffServiceTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionSignatureEvidenceServiceTest,MesProBatchRecordExecutionSignatureServiceTest,MesTeamLeaderActiveOrderSimulationServiceTest,MesFrontlinePqcSignatureContractTest,MesProEdhrReleaseServiceImplTest,MesProEdhrApprovalTaskAdapterSignaturePropagationTest,MesProductionReleaseSignatureContractTest,MesPqcCorrectionSignatureEvidenceTest,MesFrontlineSubmitIdentityTraceTest,MesStage2_5P1BoundaryTest,MesActiveOrderDossierFileServiceTest,MesEdhrBatchLifecycleGuardTest,MesProEdhrNcrBatchDisposeStateTest,MesProEdhrBatchVoidApprovalDependencyContractTest,MesProEdhrBatchVoidEffectServiceImplTest,MesActiveOrderSignatureEvidenceControllerTest,MesProductionSubmitSignatureContextTest,MesReportAllocationCommandServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DfailIfNoTests=false' *> ../doc/tasks/20261004-edhr-sa07-sa08-fix/final-regression.log
exit $LASTEXITCODE
```
27 个短类名选择器匹配 28 类（MesProEdhrReleaseServiceImplTest 有两个包路径）；依赖模块无匹配测试不是 MES 测试跳过。逐类统计见 surefire-summary.json。

## Failure evidence
- baseline-test.log：旧 reader，exit 1，8/0/1/0，合法 A 来源/B 查看被旧 target=context 判定拒绝。
- sa08-red-control.log：exit 1，1/1/0/0。仅去掉 ID 回传赋值，实际审批链 SQL 存入 null，真实 ID 断言失败；finally 已原字节恢复源码。这是单因子故障对照，不冒称完整旧基线。
- contract1～5 的编译/租户/H2/夹具前提失败与修复详见 execution-log.md，不算业务 RED。contract6、contract7 均 exit 0，3/0/0/0；最终负例也在 338 例集中回归通过。

## Original exact manifest and evidence
22 项自有改动：6 production、15 test/fixture、1 既有文档。source-fingerprints.json/markdown 列出全部路径、UTF-8 无 BOM/LF 字节数和 SHA256；owned-files.json 为清单。unchanged-reference-fingerprints.json 记录五个关键未改源码与 HEAD 一致的指纹。git diff --check exit 0；文档结构、证据校验及指纹复核见 document-validation.log。

## Blockers / pending
无实现/必需定向验证 blocker。源码及必需验证 review 和夹具修改后的组合已由管理者 PASS；最终记录核对、提交/集成和清理由管理者另行安排，本状态仅 review_ready。未运行 commit/push/merge、cleanup apply、archive、共享服务或真实数据库写入/E2E。

## Integration fixture revision — current review_ready
三个独立证据：历史自有 **28 类/338 tests**；修改后自有 **4 类/67 tests**；管理者 a37a 组合 **30 类/412 tests**。三次均 0 failures/0 errors/0 skipped、exit 0，但覆盖与源码版本不同，不能互相替代。原 338 日志/清单/用例汇总保留为历史证据。

本轮自有仅改变 MesProEdhrReleaseServiceImplTest 实际 adapter 的依赖装配：注册真实 releaseService、work task/transaction mapper，按真实构造器注册具体依赖并由 Spring 实例化；review 回调保持真实执行。组合含第三个 completed actor resolver 时使用其真实实现，缺依赖由 Spring 明确失败。没有 mock review/resolver、生产兼容构造器、fallback、并行源码复制或生产扩改。

main PQC 正式详情夹具的 main-pqc-reader-fixture.patch 仅补 MesSubmissionSignatureIdentityReader 参数；已有 wildcard import 足够。本线程仅生成 patch，没有修改 main/a37a。自有 PQC 5 例不包含 main 新夹具。

自有修改后验证使用实际 Microsoft JDK 17.0.20+8-LTS、自有 target；integration-fixture-regression.log exit 0，67/0/0/0。当次日志/XML统计、时间戳及用例名见 integration-surefire-summary.json，精确命令见 integration-fixture-command.json。

管理者机械应用两项 patch 后实际 JDK17.0.20 组合重跑 exit 0，30 类/412/0/0/0；正式审批回调及 completed actor resolver、PQC 新详情夹具、P1/Clock 组合通过。管理者确认 42 项组合输出及 23 项 main 参考来源未漂移，源码及必需验证 review PASS。只读核对 combination-summary.json、combination-regression-revision1.log、exit-code-revision1.txt，引用及哈希见 integration-manager-combination-evidence.json。最初 testCompile 失败日志保留，不误用为重跑结果。

当前 22 项自有资产精确指纹见 integration-source-fingerprints.json；较原 review_ready 仅上述测试变化，另 21 项不变。两项 patch 的输入/输出及 patch 哈希见对应 manifest，交接见 integration-fixture-handoff.md。

真实 E2E 未执行，不声称真实 MySQL、页面或生产服务验收。本线程未执行提交、推送、合并、服务、真实数据库写入、远程、cleanup apply 或 archive。当前 review_ready；完成最终自有记录后停手，管理者核对并安排后续精确集成与完整审查。