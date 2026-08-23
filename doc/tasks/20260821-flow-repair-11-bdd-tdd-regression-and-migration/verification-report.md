# 验证报告：流程修复 11 设计开发与验证

## 1. 验证范围

本报告验证专项文档、流程11无副作用实现、合同引用和当前代码审计结论。未启动服务，未访问或修改数据库，未运行生产迁移或写入型 E2E；流程11 Python runner/pytest、BPM/ERP 编译源修复、全模块 Maven compile、受影响 Node 静态检查和 `MesFrontlinePqcContextServiceTest` 定向 JUnit 已运行通过。

## 2. 已核对正式来源

- AGENTS.md、docs/task-closeout-rules.md、docs/experience-index.md。
- docs/product/production-role-system-operations.md。
- docs/backend-development.md 中“活跃订单申请放行资料必须只使用正式来源”章节。
- docs/frontend-development.md、docs/e2e-rules.md。
- 流程修复 01、02、03、04、05、06、07、08、09、10 的当前任务目录、development-plan.md、test-plan.md 或合同证据。
- 当前代码与测试中的 active-order release、completion/backfill、batch execution、四材料和 release 路径。

流程修复 04、05、07、10 的独立任务文档均已存在并已纳入本专项，不再列为缺失文档 blocker。四份材料及流程 1-10 的接口、状态 owner、入口顺序和门禁合同已冻结；未完成的是生产代码、测试、真实 E2E 和迁移证据。
流程修复 06 最新合同已核对：Tx-A 失败返回 BACKFILL_ATOMIC_ROLLBACK，不提交 completionBackfillReceipt，不产生 BACKFILL_FAILED receipt；失败尝试由流程 4 审计，流程 6 只消费成功的 BACKFILL_SUCCEEDED receipt 并独占 BATCH_* 状态。

## 3. 目标顺序和职责符合性

1. 流程 1 绑定生产工单对应的正式领料单及分录。
2. 流程 2/3 由一线生产、一线 PQC 提交签名事实，各自组长只复核来源事实。
3. 生产和检验进度均 100% 后，生产组长点击完成。
4. 流程 4 在同一 Tx-A 完成节点统一回填批记录、过程检验单及适用损耗；流程 5 逐工序形成 REQUIRED/NO_LOSS/BLOCKED，订单 receipt 记录 SUCCESS/NOT_REQUIRED 及 hasActualLoss、lossQuantity、lossReportStatus 和零损耗正式快照。
5. Tx-A 成功后流程 6 进入 BATCH_PROVISIONING；流程 9 先校验排产/PQC/手工/独立入口凭证，活跃订单直接消费成功 receipt。
6. 流程 6 Tx-B 创建/复用批次后，流程 7 先执行 Tx-C Origin/TraceLink 及工单、领料、三类回填映射；映射完成后才进入 BATCH_READY。
7. 仅 BATCH_READY 批次进入流程 8 四材料 gate：来料检报告、灭菌报告、成品检报告、成品检记录；MATERIALS_READY 后由流程 10 唯一写 RELEASED，流程 7 再提供放行后追溯。

旧三项资料一律作为历史迁移中的 BLOCKED_LEGACY 场景，不能作为当前流程兼容成功条件；成品检报告与成品检记录不可互代。

## 4. 当前代码符合性结论

结论：No-Go，代码尚未符合目标态。

- MesTeamLeaderActiveOrderReleaseGenerationService 的 active-order release/apply 路径仍有先生成申请/PQC_RELEASE 的证据，未证明完成 receipt、统一回填和流程 6 建批是必经前置。
- 现有批次/资料单测、writer 测试和真实流程脚本仍固化先建批再写资料的旧顺序，需按完成回填后建批调整并保留四节点断言。
- 多入口 submit/approve/release 仍需收敛到流程 8 manifest gate 和流程 10 唯一 finalization/CAS；独立合法入口无 activeOrderId 不应被一律拒绝。
- 正式领料绑定、双 100% 完成、回填 receipt、四节点 manifest、Origin/TraceLink、历史分类和最终状态 owner 尚未形成可执行闭环证据。
- 当前总方案已删除 BACKFILL_FAILED receipt 作为状态；失败尝试记录不得被流程 6 消费，成功才产生 BACKFILL_SUCCEEDED receipt。

## 5. 需求追踪矩阵

| 需求 | 责任合同 | 当前证据/结论 |
|---|---|---|
| 正式工单/领料绑定 | 流程 1 -> 流程 4 receipt | 设计已纳入，代码闭环待实现 |
| 双签名与组长复核 | 流程 2/3 | 合同已冻结，代码字段/权限闭环待实现 |
| 双 100% 后完成 | 流程 4 | 当前实现未证明统一 owner |
| 同节点三类回填 | 流程 4/5/6 | Tx-A 失败必须原子回滚且无 receipt；成功才提交 BACKFILL_SUCCEEDED，逐工序损耗和订单 receipt 语义待实现 |
| 回填后创建/复用批次 | 流程 6/7/9 | Tx-B 先 BATCH_PROVISIONING，流程 7 Tx-C 映射成功后才 BATCH_READY；当前旧测试倒序，RED 计划未运行 |
| 四材料硬门禁 | 流程 8 | 四节点保留；缺一及两成品检节点互代必须阻断 |
| 多创建入口 | 流程 9 -> 流程 6 | 活跃订单消费不可变 completionBackfillReceipt；独立入口消费不可变 IndependentBatchPrerequisiteReceipt 和正式 source relation；BatchProvisioningState/batchExecutionId 由流程 6 可变持有，待代码实现 |
| 多放行入口 | 流程 8 -> 流程 10 | 所有入口共用 gate，唯一 RELEASED，待实现 |
| 放行前映射与放行后完整追溯 | 流程 7 | Tx-C 映射缺失必须 TRACE_MAPPING_BLOCKED 并阻断流程 8/10；代码待实现 |
| 历史迁移/回滚 | 流程 11 汇总 | 五类分类器、只读 dry-run 报告和回滚计划已实现并通过 12 个合同场景；fixture dry-run 已执行，生产历史数据 dry-run、人工复核和回滚演练仍未执行 |
| 流程修复 1-10 | 各线程最新合同 | 01-10 均已读取并纳入，不存在“04/05/07/10 文档缺失” blocker |

## 6. BDD/TDD 与分层验证证据

test-plan.md 和 execution-log.md 已记录 Given/When/Then、RED、GREEN、REGRESSION markers。流程1-10的 Maven/Node/Playwright 合同仍明确标注“计划，未运行”；流程11 Python runner、pytest 和 Maven 基线编译另有实际结果，计划中的 PASS 不是实际通过。测试范围覆盖：回填后建批、无损耗不建单、四材料缺一阻塞、成品检报告/记录不可互代、多创建入口、多放行入口、放行后完整追溯和历史关系不明阻断。

## 6.1 流程 11 迁移实现验证

- 已验证 `IntRuoyiBackend/script/flow_repair_11_migration.py` 为纯函数实现，输出五类冻结分类，不连接数据库、不执行 SQL、不猜测来源关系。
- 已验证 12 个 BDD 合同场景：四材料逐节点持久化证据、旧三材料 `BLOCKED_LEGACY`、缺第四节点、hash/version 冲突、独立凭证未绑定、独立凭证加正式 source relation、缺正式 source relation、失败尝试无成功 receipt、来源不明、已放行复核、批准/未批准回滚计划、重复批次 ID 阻断。标准 Python runner 实际通过。
- `build_rollback_plan` 仅在 `PROVABLE_UNBOUND + APPROVED` 时返回 `write_allowed=true`，回滚范围固定为 `NEW_ORIGIN_TRACE_LINKS_ONLY`；其它分类或未批准均阻断。
- `build_dry_run_report` 返回逐节点 material evidence、分类计数、唯一 batchExecutionId、`write_allowed=false` 和 `side_effects=[]`；重复 ID 返回稳定错误，不生成写入操作。
- `python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q` -> PASS，`12 passed in 0.16s`；M13 的 Maven 全模块 compile 曾在 `MesFrontlinePqcContextServiceImpl.java:736` 因缺少 `EquipmentOption` 符号处失败，M14 恢复 DTO 契约后已重新通过，因此 Java 流程1-10合同测试和完整回归仍未取得证据。

## 7. 未解决 blocker

1. 流程 1/4/5/6/7/8/9/10 的最终字段、状态 owner、receipt、manifest、finalization/CAS 合同尚未全部在生产代码落地；Tx-A 失败无 receipt、流程 5 损耗状态、流程 6 BATCH_*、流程 7 Tx-C 映射和流程 9 canonical receipt 校验语义也尚未落地。
2. 当前实现和旧测试仍存在先建批、资料后写及多入口直接放行路径，必须完成 RED/GREEN/REGRESSION。
3. 真实租户、角色、签名、正式工单/领料单、PQC 汇总和四份附件尚未准备或使用，真实 Playwright E2E 尚未执行。
4. 生产历史批次/申请尚未执行授权后的真实只读 dry-run、人工复核和回滚演练；本线程已完成规范化 fixture dry-run。缺映射、缺 receipt 或已放行来源不完整必须分别进入 TRACE_MAPPING_BLOCKED、LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED、ALREADY_RELEASED_REVIEW_REQUIRED。
6. 远端融合已完成，但本地主工作树未做覆盖式快进：本地 `E:\IntRuoyi` 的 dirty/untracked `AGENTS.md`、运行时文档、任务文档和 Word fixture 会被快进覆盖，因此 `git merge --ff-only` 在本地被保护性拒绝；未执行 reset/checkout/stash/clean。干净流程11 worktree 已合入 `5f0138e4c`，并由 `git push origin HEAD:int_main` 推送包含集成提交 `c22d4df23` 的远端分支，祖先关系核验通过。

以上 blocker 是实现、数据和验证前置，不是流程修复 04、05、07、10 文档缺失；当前四材料合同也不以旧三项历史数据作为兼容成功条件。

## 8. 验证命令与结果

- 已执行：`python IntRuoyiBackend/script/run_flow_repair_11_contracts.py` -> PASS 12；`python -m py_compile ...` -> PASS；`python -m pytest IntRuoyiBackend/script/tests/test_flow_repair_11_migration.py -q` -> PASS（12 passed）；规范化 fixture dry-run -> PASS，总数 8、唯一批次 ID 8，分类计数 1/1/4/1/1，`write_allowed=false`、`side_effects=[]`。
- 已执行（M13 历史结果）：初次 bundled Maven compile 因 `MesFrontlinePqcContextServiceImpl.java:736` 缺少 `EquipmentOption` 失败。
- 已执行（M14/M16）：恢复 QA DTO 后 MES reactor 24/24 `BUILD SUCCESS`；补齐被 `**/runtime/` 错误忽略的 BPM/ERP 源后，完整 `mvn -pl yudao-module-bpm,yudao-module-erp,yudao-module-infra,yudao-module-mes -am -DskipTests compile` -> PASS；Flow11 runtime guard（8090/48090）-> PASS。
- 已执行：Flow11 Python runner 12 场景、pytest `12 passed`、py_compile、两个受影响 E2E `node --check`、`git diff --check` -> PASS。
- 已执行：`test_branch_runtime_profile.py -q --basetemp D:\IntRuoyiWorktree\flow11-pytest-temp` -> PASS，16 passed；slot=31 可用，slot=41 按当前上限拒绝，不再出现 `must be between 1 and 30`。
- 已核对：远端 `origin/int_main` 包含集成提交 `c22d4df23` 及流程11分支，`git merge-base --is-ancestor codex/20260822-flow-repair-11-design-development origin/int_main` -> PASS；本地主工作树未强行更新，未把独立 worktree 结果冒充本地 checkout 证据。
- 已执行：只读 rg --files、rg -n，核对五份文档、流程合同引用、四材料节点和 BDD/RED/GREEN/REGRESSION markers；自定义标记扫描确认独立入口、四个 BATCH_*、损耗三态和映射门禁均存在且旧凭证/待冻结措辞不存在。
- 已执行：只读 git diff --check；对未跟踪 Markdown 另以 rg 扫描尾随空格。
- 未执行：流程1-10生产代码合同回归、服务、生产数据库迁移、SQL、人工批准/回滚演练、真实 Playwright E2E 和任何写入型 E2E；上述流程11无副作用验证不替代这些跨流程证据。

## 8.2 当前本地 int_main 融合与验证

- 本节记录较早的集成树快照；其中 45/46 BPM 结果和缺失脚本描述已由 8.3 物理主工作树复验 supersede，不作为最终证据。
- 最终证据以 8.3 和 M19 为准：本地 `int_main` 当前为 `5e6117f9292f0cfab73d405042b03a22c7342e84`，已包含流程11 `8fe9228b2`，主工作树未覆盖其它 dirty/untracked 改动。

## 8.3 物理 E:\IntRuoyi 最终门禁

- Maven 主线 compile 已实际通过 24/24 modules；前端 `ts:check`、Node 静态检查、runtime guard 和 diff-check 已通过。
- 三个流程11 Python 文件及 BPM/ERP task-owned 源和测试源的 staged 删除已确认是本线程此前受保护 ref 更新造成的意外物理删除；已按路径恢复并复验，未覆盖其它 dirty/untracked 文件。runner、pytest、py_compile、ERP 6/6 JUnit 和 BPM 46/46 JUnit 均实际通过。
- 流程11自身迁移工具、回归基线和编译源已完成并验证；本次主线程全链路仍 No-Go，剩余流程1-10生产回归、真实 Playwright E2E、历史迁移、人工批准和回滚 blocker。

## 8.1 M14 编译修复验证

- BDD: `MesFrontlinePqcContextService` 使用的发布版 QA DTO 必须提供设备选项字段和嵌套类型，避免源码与测试契约分裂。
- RED: 修复前 bundled Maven `mvn -pl yudao-module-mes -am -DskipTests compile` 在 `MesFrontlinePqcContextServiceImpl.java:736` 报 `MesQaInspectionRegulationPublishedVersionRespVO.EquipmentOption` 缺失。
- GREEN: 恢复 `equipmentRequired`、`equipmentOptions` 和 `EquipmentOption` 后同一命令 `BUILD SUCCESS`（24/24 modules）。
- REGRESSION: `mvn -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，9 tests run, 0 failures/errors/skipped。
- COMMIT: `006a954d65c770a4454f41ed60a0ea312b3ad55a`，仅包含本次 QA DTO、测试 fixture 和流程11任务文档收尾；正常 hook 通过。

## 9. Go/No-Go

流程11独立迁移切片已完成 runner 和 fixture dry-run 验证，但整体代码保持 No-Go。只有实现线程关闭旧顺序和直接放行路径，并取得四材料硬门禁、多入口幂等、完整追溯、生产历史迁移授权/回滚和真实 Playwright 证据后，才可重新评估 Go。

本轮 task.md 已标记 `completed`（流程11专项范围）：流程11代码、Python 验证、完整 Maven 编译、定向 JUnit、Node/TS 静态检查、主线融合和 staged 删除恢复均已取得实际证据。流程1-10生产回归、真实 E2E、生产历史迁移、人工批准和回滚仍阻断，因此全链路仍为 No-Go。流程 8 仅接受四节点当前有效 COMPLETED（有批准字段时 APPROVED，节点 version/file_hash/source_snapshot_hash 与 manifest 一致），历史迁移统一五类。五份正式文档和流程11迁移模块均保留。

## 10. 流程8全 MES 回归失败分类（M20）

流程8独立 worktree 的只读 Surefire 工件为 479 suites、3575 tests、59 failures、93 errors、19 skipped。流程11已将 152 条 failure/error（59+93）逐条列出到 `flow8-mes-regression-classification.md`，每条包含测试类/方法、原始 F/E 类型、primary 分类、owner、root-cause 判断、`blocksFlow8`、最小复现命令和后续动作；19 skipped 单独记录为覆盖缺口，不作为业务 RED。

| primary 分类 | 覆盖 | 结论 |
|---|---:|---|
| `F8-GATE` 四材料/放行 gate | 0 | 流程8定向 215 tests 仍有 PASS 证据；没有把其它模块失败冒充流程8 gate failure |
| `F7-TRACE` Origin/TraceLink/来源快照 | 5（4F/1E） | 流程7 owner；映射不完整必须 `TRACE_MAPPING_BLOCKED`，条件阻断流程8/10 |
| `A456` 流程4/5/6/9/10 | 84（37F/47E） | 回填、损耗、签名、批次和批记录由对应 owner 修复；流程11不越权改业务 |
| `PAR` 前线运行时、排产/路线、反馈、ERP及其它并行模块 | 63（18F/45E） | 对应并行 owner 处理；前线签名行是上游条件阻断，排产/反馈/ERP为条件阻断 |

环境/fixture/依赖是二次标记而不是第五类业务 RED：包括 scheduleIssueMapper NPE、候选路线快照不完整、Word 解析 0 表格、H2 `loss_reason_id`/generated-column、strict Mockito stub、缺少 fixture/依赖，以及 `batchrecordcelllink` `routeProcessId` 的 TS 静态错误。slot=31 按 runtime v6 的 1..40 合法范围处理，不进入失败分类。

本轮全量 Maven 重跑没有取得新的业务结果：未引用参数的 PowerShell 命令因参数拆分失败；修正引号后 JVM native memory allocation failure，未进入 surefire，均记录为工具/环境 blocker。XML 证据仍是只读历史基线，不使用 mock、API-only、直接 SQL、默认成功或 skip 代替真实回归。流程8 owner、流程7 owner、流程4/5/6/9/10及并行 owner、测试基础设施 owner 的持久化摘要见分类报告第 5 节。

本专项仍保持“流程11代码与分类工具已验证；全链路 No-Go”：流程1-10生产闭环、真实 Playwright、生产历史迁移、人工批准和回滚演练没有因本次分类而变为通过。

## 10.1 M21 受控 Maven 重跑结论

- 原始 native-memory blocker 的根因已确认：旧 Maven 进程的 ergonomic `MaxHeapSize` 约 8GB，在 `hs_err_pid49664.log` 中记录系统物理可用约 1.7GB 时分配约 2.3MB 失败；这是 JVM/主机资源问题，不是流程8业务断言。
- 通过单次进程级 `MAVEN_OPTS` 限制（`-Xmx2048m`、512MB Metaspace、128MB code cache、2 个编译线程、512KB 栈）并正确引用 Surefire 参数后，Maven 已进入 Surefire，native memory allocation failure 未再次出现。
- 当前主线受控重跑的 `yudao-module-mes/target/surefire-reports` 只读聚合为 240 suites、1589 tests、7 failures、195 errors、0 skipped。失败类已列入执行日志，主要属于流程4/5/6/7/9/10、前线运行时、排产/路线或 fixture/依赖 owner；未出现流程8四材料/最终放行 gate 的直接失败项。
- 因仍有真实 failure/error，本轮不能写成 Maven 回归通过；流程11工具验证完成，但流程8全 MES 回归和流程1-10全链路仍 No-Go。未修改流程8业务代码、未启动服务、未访问生产数据库、未执行真实 Playwright 或写入型迁移。
