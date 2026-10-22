# Execution Log

- 基线与规定五文件已读取。技能 bug-regression-fix-loop 已读取；项目最近规则决定 BDD/TDD 按需。

## Milestone 1 — completed
- 核对管理者三份未提交审计报告和 d3f1793 基线。SA07 根因是把 allocation 目标当作原签名来源；SA08 根因是 prepare 中找到的签名 ID 没有传回 finalization，以及统一证据 reader 只接受 MES 合同。
- 仅在本 worktree 创建 codex/edhr-sa07-sa08-fix；预约槽位 5，不启动服务。主干并行 Clock、P1 和各自测试资产未复制。

## Milestone 2 — completed
- SA07 新增原始事件与目标 CURRENT allocation/快照分别核验；签名比较原始 activeOrderId。归档按正式 ignoreDeleted 活跃订单读取，仍验证 tenant/FK。
- SA08 实际 BPM 六字段 subject/adapter canonical/hash 校验，绑定正式申请/任务/事务/批次，实际签名 ID 传入 finalization SQL、decision、audit、upstream。重放重新核验同一正式记录。
- baseline-test.log：Java17 实际执行，exit 1，8 tests / 0 failures / 1 error / 0 skipped；新增跨单测试复现旧代码把目标替换来源的错误。
- iteration1-test.log：exit 1，testCompile 发现既有模拟测试构造器漏一个新增参数；已补齐。
- iteration2-test.log：exit 0，37 tests / 0 failures / 0 errors / 0 skipped。
- contract1-test.log：exit 1，testCompile 发现 decision DO getter 拼写错误；更正为 auditSnapshotJson，无生产运行失败。
- contract2-test.log：exit 1，3 tests / 0 failures / 3 errors / 0 skipped；工作任务夹具缺必填 task_code，独立合同夹具缺 JdbcTemplate bean。补齐任务属性并从真实 DataSource 构造 JdbcTemplate。
- contract3-test.log：exit 1，3 tests / 0 failures / 2 errors / 0 skipped；基础 H2 测试无租户填充，继承放行夹具使用租户 0。明确统一夹具与当前租户。
- contract4-test.log：exit 1，3 tests / 0 failures / 2 errors / 0 skipped；H2 不支持 MySQL 位字面量，JdbcTemplate 可变参数泛型推断错误。仅在测试安装位字面量转换，生产 SQL 绑定不变；显式 Long 参数。
- contract5-test.log：exit 1，3 tests / 0 failures / 1 error / 0 skipped；SA07 真正读写合同通过。SA08 finalization、事务签名 ID、decision、upstream 与重放已达预期，后续审计 SQL 缺 H2 JSON 函数。提前注册现有测试 JSON aliases。
- contract6-test.log：exit 0，3 tests / 0 failures / 0 errors / 0 skipped；真实 MES/BPM writer、adapter、query、H2 正式关联、详情 SQL、当前/归档 reader 与签名追溯通过。
- contract7-test.log：exit 0，3 tests / 0 failures / 0 errors / 0 skipped；增加真实 MesProEdhrApprovalTaskAdapter 回调及任务/来源/actor/tenant/hash 零推进验证，首次/重放不重复签名、decision、审计或上游关闭。
- sa08-red-control.log：exit 1，1 test / 1 failure / 0 errors / 0 skipped。仅暂时去掉新 finalization 签名 ID 回传赋值，其余测试与来源核验不变；真实 SQL 事务 approval_signature_id 为 null，实际 ID 一致性断言失败。该对照不是原始完整基线回退；命令 finally 已逐字节恢复源码。
- SA07 最终补充实际持久化 actor/operator/domain/source/tenant/hash 负例，覆盖三类证据入口。最终验收以集中回归为准。

## Milestone 3 — completed
- 冻结 22 项任务自有源码、测试及既有规范改动为 UTF-8 LF；清单 owned-files.json。git diff --check exit 0。
- 实际 Microsoft JDK 17.0.20+8、Maven 3.9.16，使用本 worktree 的 target；27 个定向/相邻测试类集中执行，未跑全仓、真实 MySQL、服务或 E2E。
- 已按 project-experience-consolidation 技能将来源/分配、BPM ID 传播及真实读写合同验证经验合并进 docs/backend-development.md，未新增长期经验文档。
- final-regression.log：exit 0，28 类 / 338 tests / 0 failures / 0 errors / 0 skipped。27 个短类名选择器匹配两个包下的 MesProEdhrReleaseServiceImplTest；统计与本轮 Surefire XML 逐类匹配，未用旧报告推断通过。
- 冻结后仅更新本任务记录；source-fingerprints.json/markdown 包含全部 22 项改动。unchanged-reference-fingerprints.json 证明五个关键未改源码与 HEAD 的 UTF-8/LF 内容一致。
- 状态 review_ready；未做 Git 提交/推送/融合、服务、真实数据库写入、E2E、cleanup apply 或 worktree archive。保留 review 所需 target 和失败/成功对照日志，后续集成与清理由管理者处理。

## Integration fixture revision — completed / review_ready
- 管理者授权仅夹具整改；只读 a37a 的 combination-regression.log/input-manifest.json/combined-output-manifest.json/command.json 及 main 当前原文。组合在 testCompile exit 1，测试未执行；错误为新增 adapter 必需第三依赖和 main PQC 正式详情夹具缺 reader 参数。
- 原 338/0/0/0 及原清单/指纹保持历史证据，不表示组合通过。当前 main/参考副本不修改，业务代码不扩改。

- 自有实际 adapter 合同改为 Spring 构造装配：实际 releaseService、work task/transaction mapper 注入，具体新依赖按真实类型创建；review 真实执行，没有 mock review/resolver、生产兼容构造器、fallback 或并行源码复制。
- main-pqc-reader-fixture.patch 仅一处构造调用增加 reader 参数；import 已覆盖。输入/输出 UTF8 LF 指纹已记录；本线程未写 main/a37a。
- integration-fixture-regression.log：实际 Java17、自有 target，exit 0，4 类/67 tests/0 failures/0 errors/0 skipped；当次 XML/日志/UTC时间戳逐类核对。覆盖真实审批首次/重放与错误关联负例，自有 PQC 5 例不包含 main 新夹具。
- 管理者最新授权明确两项 patch 已机械应用并指纹核验，实际 a37a 组合 30 类/412/0/0/0、Maven exit0、JDK17.0.20，42 项输出和23项main参考未漂移，源码及必需验证 review PASS。只读核对 revision1 日志/exit/summary，并冻结证据引用哈希；不另跑测试。
- 原 338 例、自有修改后 67 例和管理者组合 412 例分别记录；最初组合 testCompile 失败日志保持历史记录。真实 E2E 未执行。
- 已调用 project-experience-consolidation，核对 docs/backend-development.md 既有真实 writer/adapter/query 合同经验；本轮夹具整改遵守该经验，无新增长期文档或任务外修改。
- 状态 review_ready；最终清单 integration-source-fingerprints.json，交接 integration-fixture-handoff.md。最终记录核对和集成由管理者处理；本线程停手，不执行提交/推送/合并、服务、DB、main写入、远程或破坏性收尾。
## Manager integration gate
- 最终22项源码指纹及夹具交付核验通过；管理者30类412/0/0/0实际JDK17组合PASS，修复者已停手。按用户持续循环授权仅本地精确提交和融合，不远程推送，不提交main并行来源。进入ready_for_closeout，整体循环未完成。
