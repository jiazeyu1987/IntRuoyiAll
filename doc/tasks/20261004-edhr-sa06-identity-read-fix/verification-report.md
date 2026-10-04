# SA06 Verification Report

## Current Status
ready_for_closeout

## Bug / Expected / Root Cause
同租户 system user 与 employee profile 可共享数字ID；原SQL跨域COALESCE使正式实际人员错名。签名名称取当前名单还会在改名后改变历史证据。修复：持久身份域决定实际人员表；正式生产/PQC提交取严格验证的冻结signatureIdentity；既有实际员工=签名者约束保留，登录操作者A可与B实际员工/签名者不同。

## Reproduction boundary / Historical SQL exploration
本轮正式验证采用最终reader与真实mapper双域碰号合同，不把历史SQL探索记为业务复现或完成门禁；遵循管理者最新收口指令，不再扩展历史夹具。旧SQL提取自HEAD 64f07c9ea2fd10968dd2ab85f64d065dc5956cdb，不切换源码。H2自有夹具系统用户7=System S、临时工档案7=Temporary B。
保留已发生探索结果：以下历史对照命令退出1；1用例/1失败/0错误/0跳过，expected Temporary B but was System S。前两次缺列是夹具错误；这三轮均不记业务复现、不作为完成门禁。

```powershell
& C:/Users/BJB110/Documents/Codex/tools/apache-maven-3.9.16/bin/mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesSa06IdentityMapperTest#actualDetailsAndPqcPartiesResolveSameIdOnlyWithinPersistedDomain' '-Dsa06.mapperRoot=C:/Users/BJB110/.codex/worktrees/a37a/IntRuoyi/doc/tasks/20261004-edhr-sa06-identity-read-fix/baseline-mappers' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dfile.encoding=UTF-8'
```

## Final Verification
RED: 仅保留前端定向断言已发生的失败记录：模拟角色无ID实际返回“未签名”，预期“模拟记录：B（模拟）（2026-10-04 09:00:00）”；退出1。不作为默认完成门禁。
GREEN: 以下命令在最终冻结源码运行，maven-review-ready.log，BUILD SUCCESS，退出0，2026-10-04T18:58:06+08:00结束。

```powershell
$env:JAVA_HOME='C:/Users/BJB110/.jdks/jdk-17.0.20+8'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
& C:/Users/BJB110/Documents/Codex/tools/apache-maven-3.9.16/bin/mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am test '-Dtest=MesProcessPoolActiveOrderDetailReadMapperXmlTest,MesProBatchRecordExecutionSignatureServiceTest,MesProductionSignatureEvidenceServiceTest,MesProcessPoolProductionReportRevisionLogServiceTest,MesProcessPoolTimelineSubmissionPayloadDisplayTest,ProcessPoolTimelinePqcGroupSqlTest,MesActiveOrderSignatureEvidenceServiceTest,MesPqcCorrectionSignatureEvidenceTest,MesSa06IdentityMapperTest,MesSubmissionSignatureIdentityReaderTest,MesTeamLeaderActiveOrderDetailServiceImplTest,MesTeamLeaderActiveOrderSimulationServiceTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseSignoffServiceTest,MesActiveOrderDossierFileServiceTest,MesProductionReleaseReportServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dfile.encoding=UTF-8'
pnpm --dir IntRuoyiFronted exec node scripts/sa06-simulation-signature-display.test.mjs
```

仅本轮计入最终结论：后端16类232用例，失败0/错误0/跳过0；前端独立7项合同断言PASS，退出0，不与JUnit用例累加。
`-Dsurefire.failIfNoSpecifiedTests=false`仅允许上游模块未匹配选择类；已逐类核对MES16类本轮log与Surefire XML数量及runtime，未把未匹配类算通过。

### Actual test JVM
- java.home: `C:\Users\BJB110\.jdks\jdk-17.0.20+8`
- java.runtime.version: `17.0.20+8-LTS`
- java.vendor: `Microsoft`
- java.version: `17.0.20`
- java.class.version: `61.0`

取自最终16类Surefire XML properties，全部一致，不以javac target 17代替runtime证明。

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| MesProcessPoolActiveOrderDetailReadMapperXmlTest | 1 | 0 | 0 | 0 |
| MesProBatchRecordExecutionSignatureServiceTest | 20 | 0 | 0 | 0 |
| MesProductionSignatureEvidenceServiceTest | 28 | 0 | 0 | 0 |
| MesProcessPoolProductionReportRevisionLogServiceTest | 7 | 0 | 0 | 0 |
| MesProcessPoolTimelineSubmissionPayloadDisplayTest | 1 | 0 | 0 | 0 |
| ProcessPoolTimelinePqcGroupSqlTest | 6 | 0 | 0 | 0 |
| MesActiveOrderSignatureEvidenceServiceTest | 17 | 0 | 0 | 0 |
| MesPqcCorrectionSignatureEvidenceTest | 69 | 0 | 0 | 0 |
| MesSa06IdentityMapperTest | 2 | 0 | 0 | 0 |
| MesSubmissionSignatureIdentityReaderTest | 6 | 0 | 0 | 0 |
| MesTeamLeaderActiveOrderDetailServiceImplTest | 31 | 0 | 0 | 0 |
| MesTeamLeaderActiveOrderSimulationServiceTest | 5 | 0 | 0 | 0 |
| MesProductionReleaseManagerApprovalServiceTest | 11 | 0 | 0 | 0 |
| MesProductionReleaseSignoffServiceTest | 1 | 0 | 0 | 0 |
| MesActiveOrderDossierFileServiceTest | 16 | 0 | 0 | 0 |
| MesProductionReleaseReportServiceTest | 11 | 0 | 0 | 0 |

## Preserved rounds (not cumulative)
| Log | Tests | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| maven-baseline-behavior-red.log | 1 | 1 | 0 | 0 | FAIL |
| maven-baseline-contract-red.log | 1 | 0 | 1 | 0 | FAIL |
| maven-baseline-red.log | 1 | 0 | 1 | 0 | FAIL |
| maven-final-regression.log | 232 | 0 | 0 | 0 | PASS (historical) |
| maven-final.log | 158 | 0 | 0 | 0 | PASS (historical) |
| maven-frozen-source.log | 0 | 0 | 0 | 0 | FAIL |
| maven-initial.log | 46 | 1 | 0 | 0 | FAIL |
| maven-regression.log | 232 | 0 | 1 | 0 | FAIL |
| maven-review-ready.log | 232 | 0 | 0 | 0 | PASS (final authoritative) |
| maven-targeted.log | 54 | 1 | 0 | 0 | FAIL |

初始46例及定向54例失败为详情mock签名时间合同未同步，分别修正返回签名时间；232例失败轮唯一补正错误源于改取统一evidence.actorDisplayName，而真实FIELD_CHANGE evidence该字段为空。最终采用既有完整补正revision/review/subject/audit验证后的revisionSignatureSnapshot.actorName，并增加当前名单改名不影响冻结名称断言；其它动作保留原已验证绑定来源，未删除业务断言或放宽身份/审计/哈希守卫。后续0例编译失败为该校验误放void audit方法，已移回String correction方法。早期被覆盖的编译失败日志不提供用例计数、不作为验证依据；所有保留轮如上。

## Coverage and limits
- H2/MyBatis执行实际详情完整SQL与event-parties SQL，双域同号、PQC系统域、未知域和错租户无名称猜测；时间线执行生产ActualEmployeeName/TimelineAuthorityJoins片段，修订姓名筛选执行真实count SQL。CHAR/UNSIGNED仅作H2方言转换；不宣称完整MySQL数据库、时间线全页或并发验收。
- 当前及归档详情分别覆盖当前实际人员名与冻结正式签名名；真实reader覆盖操作者A/员工签名者B、域/租户/关联/hash错配及正式缺identity拒绝。
- Stage1真实simulation signature writer产出的持久投影与production/PQC事件writer输出喂给真实reader，模拟独立合同可读、role=SIMULATION_SESSION、姓名含（模拟）、无正式signatureId、无统一正式证据查询。前端真实formatter及跳转函数验证“模拟记录/姓名/时间”、正式无ID未签名、无ID按钮禁用且无跳转。Node24原生类型剥离只执行定向函数；未安装依赖、未跑完整TS检查或真实页面。
- 相邻正式签名、PQC补正、资料上传、放行报告/经理批准/签字生命周期回归包含在最终232例中。
- 更改文件完整清单与UTF-8/LF SHA256见source-fingerprints.md。task目录受现有gitignore忽略，交接时须保留或由管理者按规则显式纳入最终记录。

## Blockers / Integration boundary
本轮本地修复及必需回归无阻塞，停review_ready。管理者主干未提交P1增量不在本worktree；融合时须另核对，不能以本轮结果声称P1最终融合通过。未连接业务数据库、执行业务API/DB写入、启动/重启服务、真实E2E、提交/推送/合并或删除worktree。用户指定由管理者处理提交/融合，正常closeout清理与completed状态留待其复审后执行。

## Durable experience
已按project-experience-consolidation合并至docs/backend-development.md既有规则；无新建长期经验文件。

## 管理者复审与本地集成门禁（2026-10-04）
管理者完整源码复审 PASS；16项最终指纹一致。隔离环境加入主干实际两项P1增量后，实际Java17定向16类247例，失败/错误/跳过0；管理者前端7项合同PASS。两项临时输入逐字节恢复，不纳入本修复提交。管理者批准必要本地实现提交与融合；无远程推送、共享服务、数据库或E2E操作。工作线程232例与组合247例独立记账，不能累加。实现和验证已通过，保留待融合复审证据；尚未completed。
