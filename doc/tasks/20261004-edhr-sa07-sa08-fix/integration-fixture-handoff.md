# Integration fixture handoff

## Current Status
review_ready — 管理者源码及必需验证 review PASS；本线程完成最终记录后停手。

## Scope and exact assets
- integration-own-fixture.patch：仅 MesProEdhrReleaseServiceImplTest 的实际 adapter 依赖装配变更；真实 review、真实具体依赖，组合第三参数 resolver 由 Spring 使用真实 mapper 装配。精确输入/输出及 patch 指纹见 integration-own-change-manifest.json。
- main-pqc-reader-fixture.patch：参考 main PQC 正式详情夹具，只补缺失 reader 参数，既有 import 足够。精确输入/输出及 patch 指纹见 main-pqc-reader-fixture-manifest.json。本线程未将参考整文件纳入自有资产，未修改 main/a37a。
- integration-source-fingerprints.json：当前全部22项自有资产（6 production、15 test/fixture、1既有文档）的 UTF8 LF 指纹；相对原 source-fingerprints.json，仅上述自有测试变化，其余21项一致。原清单保留历史338证据。
- integration-manager-combination-evidence.json：管理者 a37a revision1 证据的只读引用及哈希。管理者已机械应用两个 patch；参考 patch manifest 中 applied_to_main_or_reference=false 仅描述本线程行为，非管理者应用状态。

## Distinct verification evidence
| Evidence | Executor / source | Result | Boundary |
| --- | --- | --- | --- |
| historical final-regression.log + surefire-summary.json | 本线程原冻结版本 | 28类/338/0/0/0，exit0 | 历史版本 |
| integration-fixture-regression.log + integration-surefire-summary.json | 本线程修改后版本，JDK17.0.20+8 | 4类/67/0/0/0，exit0 | 未含main新增PQC夹具或第三resolver生产源码 |
| combination-regression-revision1.log + combination-summary.json | 管理者a37a实际组合，JDK17.0.20 | 30类/412/0/0/0，exit0 | 两项patch、正式审批回调/resolver、新PQC夹具、P1/Clock组合通过 |

管理者确认42项组合输出与23项main参考来源未漂移，源码及必需验证review PASS。本线程只读核对最新组合日志、exit和summary；未执行该组合测试。最初 combination-regression.log 为 testCompile 失败，不能作为 revision1 结果。

## Integration ownership
后续由管理者核对最终自有记录并按既有授权精确本地提交、保护并行资产、融合及完整审查；保留 managerApproval 并行 Clock 和 backend-development 既有增量，不整文件覆盖。真实 E2E 未执行。本线程未提交/推送/合并、操作服务/DB/远程、修改main或清理worktree，现已停手。