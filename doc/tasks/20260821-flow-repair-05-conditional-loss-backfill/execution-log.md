# Execution Log

## User Intent and Scope

- 线程专项：流程修复 5。
- 用户要求：完成流程5条件损耗实现并融合主线；不修改数据库、不启动服务、不运行写入型 E2E。
- 专项目标：流程修复 4 在活跃订单完成节点统一启动三类回填时，由流程修复 5 判断工序级实际损耗；有实际损耗才回填损耗单，无损耗只在完成 receipt 保存明确的 NO_LOSS 事实和 `lossReportStatus=NOT_REQUIRED`，不创建损耗单或任何无损耗报告。流程 5 必须消费流程 1 的 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId` 五字段绑定快照并只读校验；每工序和订单 receipt 必须显式保存 `hasActualLoss`。覆盖正式一线生产事实、正数判定、原因、来源快照、签名、部分工序、重复幂等及流程修复 4/6/7/8/10/11 契约。

## Read Gates

- 已读取：AGENTS.md、docs/task-closeout-rules.md、docs/experience-index.md、docs/product/production-role-system-operations.md、docs/backend-development.md 的“活跃订单申请放行资料必须只使用正式来源”章节、docs/frontend-development.md、docs/e2e-rules.md。
- 已读取技能：system-design-docs、bdd-tdd-acceptance-planner 及其结构参考。
- 命中的经验门禁：活跃订单申请放行资料只使用正式来源；缺少正式绑定、来源快照、状态事实或材料必须 fail fast；禁止 mock/API-only/直接 SQL/默认成功。

## Milestone 1: Read-only Audit (Historical Baseline)

状态：completed。

- 实现前基线：申请服务先建批再写 dossier，损耗 evidence 被无条件要求，零损耗返回 ZERO_LOSS_CONFIRMATION_UNSUPPORTED。
- 上述条目是实现前根因证据；当前流程5条件 writer、source reader、dossier/PQC 条件字段和对应测试已按本任务目标更新。
- 跨线程复核发现本专项原文曾以 `materialPickListId` 表达领料来源，未完整冻结流程 1/6 五字段绑定合同；实现线程不得以单字段或缺少 `lossRecordId` 推断 `hasActualLoss=false`。
- 只读检索还触达 target_corrupt_m4_20260802_1327 损坏构建目录；未读取其内容，生产源码和测试源码仍可定位。该环境噪声不改变代码结论。

## Milestone 2: Target and Root Cause

状态：completed。

BDD: 正数损耗按工序建单 -> Given 双100、流程 1 五字段绑定快照、正式反馈和签名完整且损耗数量大于0，When 流程 4 完成节点调用流程 5，Then 仅该工序生成损耗单并返回 REQUIRED/hasActualLoss=true/lossQuantity>0，工序及订单 receipt 的 lossReportStatus=SUCCESS。
BDD: 无损耗不建单 -> Given 正式无损耗确认事实、流程 1 五字段绑定快照存在且损耗数量为0，When 流程 4 完成节点调用流程 5，Then 流程 5 返回 NO_LOSS/hasActualLoss=false/lossQuantity=0，流程 4 工序及订单 receipt 的 lossReportStatus=NOT_REQUIRED，不生成 lossRecordId、损耗单或任何无损耗报告。
BDD: 部分工序损耗 -> Given A 工序正损耗、B 工序明确无损耗且两者绑定快照完整，When 流程 4 完成节点调用流程 5，Then 仅 A 建单并为 true，A 的 receipt lossReportStatus=SUCCESS，B 返回 NO_LOSS/lossReportStatus=NOT_REQUIRED/false/0，订单级 receipt 的 hasActualLoss=true 且 lossReportStatus=SUCCESS。
BDD: 原子回滚 -> Given 任一工序来源或损耗写入失败，When 流程 4 完成节点执行，Then 三类回填和完成 receipt 整体回滚；流程 6 的后继建批失败只由流程 6 按 receipt 重试，不重跑流程 5。
BDD: 幂等与来源快照 -> Given 同一完成版本、五字段绑定快照和来源哈希重试，When 重复提交，Then 返回同一回执及 hasActualLoss；绑定字段、来源变化或同键载荷变化必须冲突。
BDD: 缺失事实阻塞 -> Given 任一工序缺少正式零损耗事实、五字段绑定快照或签名，When 流程 4 统一回填，Then 返回 BLOCKED，不提交成功 receipt，流程 6 不得建批，不能把缺失推断为 false。

历史根因基线：损耗单曾被当成无条件第三份资料，零损耗没有正式状态，流程 1/6 五字段领料绑定快照与流程 4/6 的 hasActualLoss/lossReportStatus receipt 合同曾未贯通，且提交/复核、完成回填和后继建批职责混杂。当前流程 5 条件实现已融合主线；流程 4/6 receipt 持久化和后继编排仍由对应线程验证。

## Milestone 3: Design Output

状态：completed。

- task.md：目标、当前事实、根因、边界、里程碑、设计约束、blocker、迁移/回滚和状态。
- development-plan.md：五字段领料绑定正式来源、正数判定、条件损耗 decision、NO_LOSS/hasActualLoss receipt fact、部分工序、接口、状态所有者、幂等、事务和跨线程契约。
- test-plan.md：BDD、失败/边界、严格 RED/GREEN/REGRESSION 顺序、回归矩阵和测试 blocker。
- 接口/错误码合同：流程 5 只接受流程 1 五字段绑定快照，逐工序返回 `REQUIRED/NO_LOSS/BLOCKED`、`hasActualLoss`、`lossQuantity`；绑定或布尔合同失败分别返回 `LOSS_SOURCE_PICK_LIST_BINDING_REQUIRED`、`LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED`、`LOSS_HAS_ACTUAL_LOSS_REQUIRED` 或 `LOSS_HAS_ACTUAL_LOSS_CONFLICT`。

## RED/GREEN/REGRESSION Status

RED: 初始基线测试 -> FAIL，零损耗仍被旧合同阻塞；该失败作为实现前证据保留。
GREEN: mvn -pl yudao-module-mes -DskipTests compile -> PASS；流程5定向 JUnit 27/27 -> PASS（writer 11、source reader 4、dynamic-form 3、dossier completeness 4、frontline feedback splitter 3、loss reason snapshot validator 2）。
REGRESSION: git diff --check -> PASS；branch-runtime-port-guard.ps1 -> PASS；服务、数据库迁移和写入型 E2E 保持 NOT_RUN。

## Blockers

1. `MesFrontlineRuntimeConfigProcessScopeTest` 在主线组合中失败，断言前线运行时参数校验源码契约；流程5核心测试不受影响，该问题交由前线运行时 owner 处理。
2. 流程4订单级 receipt 持久化、流程6建批消费、流程7映射、流程8四材料、流程10最终放行和流程11迁移/全链路回归仍需跨线程验证。
3. 历史完成记录缺少正式损耗事实或五字段绑定快照时，仍须由流程11迁移门禁阻塞。

## Final Evidence

- 流程5代码/测试已由 `24fdf7767ac02c4b6d4a3c4709194e195fea624a` 提交，并以 `16e47106e043ad93b4d43d699d269996703a47e1` 融合当前 `int_main`；当前 `int_main` HEAD 为 `83f5d11a5a477463ef33444eb4ad52aa79cdd17a`，已确认 `16e47106e` 为其祖先；其后仅追加流程文档和复验经验收尾提交，未改变流程5代码。
- 在当前主线执行 `mvn -pl yudao-module-mes -DskipTests compile`（Maven 3.9.16）PASS；流程5定向 JUnit 共27/27 PASS：LossReportWriter 11、LossSourceReader 4、LossReportDynamicFormPortImpl 3、DossierCompletenessChecker 4、FrontlineFeedbackPayloadSplitter 3、LossReasonSnapshotValidator 2，覆盖正损耗、NO_LOSS、缺失事实 BLOCKED、dossier 条件门禁、来源校验和流程6交接边界。
- 首次定向测试命令因 PowerShell 未将逗号分隔的 `-Dtest` 值作为单个参数传给 Maven，触发 `Unknown lifecycle phase` 参数解析错误；改为整体引用 `-Dtest` 参数后重新执行，得到上述27/27 PASS，该错误不是代码或测试失败。
- 早期代码验证基线曾记录 writer/source/dynamic-form/splitter 共21项 PASS；随后在当前主线补充 dossier completeness 与 loss reason snapshot validator，形成上述27/27最终证据。
- `git diff --check` PASS；runtime v5 guard PASS（int_main 8081/48081）。
- 未启动前后端服务，未运行数据库命令或写入型 E2E；五份任务文档保留，删除项为零。

## Revision Milestone

状态：completed。

- 已按复核意见移除将无损耗报告作为交付物的表述，固定无损耗仅写入流程 4 完成 receipt 的 NO_LOSS 事实和 `lossReportStatus=NOT_REQUIRED`。
- 已校正职责链：流程 4 统一启动三类回填；流程 5 只做条件损耗；流程 6 建批；流程 7 消费并映射来源；流程 8 管四材料硬门禁；流程 10 管最终放行状态/角色/审计；流程 11 管 BDD/TDD、回归和迁移总门禁。
- 已补齐提交/复核不触发回填、缺失事实、部分工序、重复幂等、失败回滚和放行后追溯场景。
- 已补齐流程 1/6 五字段领料绑定快照、逐工序/订单级 `hasActualLoss`、`lossQuantity=0` 的 NO_LOSS 合同，并明确不能从缺少 lossRecordId 推断 false。
- 已完成本轮复核修订：五份文档的正式来源字段、接口表、BDD、状态/错误码、验证结论和未运行 blocker 已统一。
- 主线程复验记录：当前 `int_main` HEAD `83f5d11a5a477463ef33444eb4ad52aa79cdd17a` 上 compile 与流程5定向27/27 JUnit 均 PASS；非流程5的 `MesFrontlineRuntimeConfigProcessScopeTest` 仍因前线运行时参数校验静态契约失败，保持跨任务 blocker，未修改其 owner 代码。
