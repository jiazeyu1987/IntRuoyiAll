# eDHR 90步全流程静态检查：逻辑缺陷记录

## 审计范围与证据边界

- 用户请求：静态检查本对话第1—90步eDHR流程，并登记逻辑问题。
- 审计日期：2026-09-12。
- 工作区：`E:/IntRuoyi`；初始HEAD：`6c6487c9f151244910b9ff80454c397bc303e330`。工作区存在其他任务未提交改动，本报告核对工作区实际源码，不代表已部署版本。
- 检查方式：前端入口、请求、Controller、Service、Mapper、已有测试及业务约束交叉静态检查。未执行运行测试、E2E、API或数据库操作。
- 状态：90步静态检查及文档核验已完成，共14项问题（P1 11项、P2 3项）；当前修复状态以缺陷索引为准。逐步覆盖与源码指纹见 `doc/tasks/20260912-edhr-90-step-static-audit/verification-report.md`。
- P1：阻断正常业务闭环或破坏正式证据；P2：条件性业务错误或展示/操作误导。代码阅读只能证明所列触发条件下的逻辑，不代表运行环境已发生。

## 缺陷索引

| 编号 | 级别 | 涉及步骤 | 问题 | 状态 |
|---|---|---|---|---|
| EDHR-STATIC-001 | P1 | 17—18、21、26—28 | 已发布通用规程套版本可原地改成员或退回草稿 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-002 | P1 | 38—40、57—64 | 初始分配不变时复核提前返回，未生成正式复核 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-003 | P1 | 40、55、62—64 | 分配复核写入的签名证据与完工读取合同不一致 | REOPENED_INDEPENDENT_STATIC |
| EDHR-STATIC-004 | P1 | 33—40、55、61—64 | 同一工序正常多次报工被损耗回填判为重复 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-005 | P1 | 17—18、28、43—50、67—68 | 通用QA任务在生产放行时被按专用规程版本校验 | REOPENED_INDEPENDENT_STATIC |
| EDHR-STATIC-006 | P2 | 52—56、83、90 | 损耗表把同工序最新生产签名套到所有历史报废记录 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-007 | P1 | 29、38、61—68 | 完工申请后仍允许新增生产提交，破坏已固化来源边界 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-008 | P1 | 75—79、85—87 | 四类报告齐套直接把六类业务检查写成PASS | REOPENED_INDEPENDENT_STATIC |
| EDHR-STATIC-009 | P1 | 85—87、90 | QA处置签名只是自由文本，没有签署时身份验证 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-010 | P2 | 69—75、85 | 不合格冻结期间仍可上传正式报告附件 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-011 | P1 | 85—89 | 同工单多份不合格评审按特定顺序结束后永久残留冻结 | REOPENED_INDEPENDENT_STATIC |
| EDHR-STATIC-012 | P2 | 26—27、80—84 | 归档读取现行路线名称而非批次保存的历史名称 | FIXED_STATIC_VERIFIED |
| EDHR-STATIC-013 | P1 | 7、33、36、38、57—59 | 多输出物料拆分提交时，不同物料数量被累加成工序进度 | REOPENED_INDEPENDENT_STATIC |
| EDHR-STATIC-014 | P1 | 10、15、26—27、62、67—68 | 新路线发布物理删除旧正式表单绑定，旧订单放行无法解析 | FIXED_STATIC_VERIFIED |

## EDHR-STATIC-001：已发布通用规程套版本可以原地改写

- **涉及步骤**：17、18、21、26、27、28。
- **触发条件**：通用规程套V1已发布并被产品绑定；从套版本列表点击“编辑”，修改成员或把状态设为草稿，再保存。
- **预期行为**：已发布套版本成员和身份保持不变；变更形成新的草稿/发布版本，既有绑定继续指向原内容。
- **实际逻辑**：版本列表编辑按钮不限制发布状态；后端已有版本分支只校验所属套和版本号重复，随后更新状态、版本号、生效日期并删除重建成员。
- **业务影响**：同一套版本ID/版本号先后代表不同检验内容，新加入订单读取变更后的成员；已绑定发布版本还可以变成草稿，破坏版本可追溯性。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue:809`；`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java:430`、`:451`、`:955`；订单读取成员：`service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:2434`（同一MES Java根目录）。
- **建议修复边界**：前后端同时禁止已发布版本原地修改；对已有引用的版本保持不可变；新版本发布才改变新绑定。
- **BDD（待修复验收，未执行）**：Given V1已发布且有产品引用，When 编辑V1成员或降为草稿，Then 拒绝且版本与成员零变更；新建V2不改变V1。
- **修复状态（2026-09-13）**：已补后端服务门禁，`saveCommonRegulationSetVersion` 和 `deleteCommonRegulationSetVersion` 均只允许 `DRAFT` 套版本原地变更，拒绝点在成员删除/重建前；前端套版本表格、文档组成维护入口、编辑 handler 和删除 handler 均对非草稿版本失败关闭。验证证据见 `doc/tasks/20260913-edhr-static-findings-fix/execution-log.md`。

## EDHR-STATIC-002：不调整初始分配时无法形成复核事实

- **涉及步骤**：38、39、40、57、59、61—64。
- **触发条件**：一线提交为订单自动分配数量；生产组长确认这些数量正确，以原数量提交复核。
- **预期行为**：首次正式复核应校验并记录组长签名，即使数量没有变化也必须形成复核关联。
- **实际逻辑**：初始分配写入`reviewId=null`。`save`发现原分配与目标分配相同时，在`requireReview`之前直接重算并返回；页面仍提示“复核已提交”。
- **业务影响**：看似完成复核但正式复核不存在；完工记录缺少复核关联，申请放行在后续正式来源检查中受阻。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:8694`、`:8716`；`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:193`、`:282`、`:292`；`MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java:387`（同目录）。
- **建议修复边界**：区分“数量无变化”和“复核已存在”；首次签名复核不得走无写入分支，后续重放才允许回读。
- **BDD（已修复验收）**：Given 初始分配10且无复核，When 组长签名确认仍为10，Then 生成唯一复核并关联分配；重复同一确认不重复创建。
- **修复记录（2026-09-13）**：`MesReportAllocationCommandService.save` 在数量无变化分支先检查当前 `CURRENT` 分配是否缺少 `reviewId`；缺正式复核时调用签名复核，写入完整 APPROVED 复核证据，并通过 `attachReviewToCurrentRowsByEventId` 只关联仍为空的当前分配行。
- **验证记录（2026-09-13）**：`MesReportAllocationCommandServiceTest.unchangedInitialAllocationWithoutReviewMustCreateFormalReviewAndAttachCurrentRows` 覆盖初始数量不变、`reviewId=null` 时生成签名复核、关联当前分配且不新增版本；`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-002-allocation-review-contract.spec.cjs` 与 `mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 均通过。

## EDHR-STATIC-003：分配复核证据不能满足完工校验

- **涉及步骤**：40、55、62、63、64。
- **触发条件**：生产组长调整分配，走`requireReview`创建新的生产复核记录，包括提供有效签名密码的情况。
- **预期行为**：正式批准复核应具备完整签名快照，生产反馈批准人与复核人一致，后续完工可直接验证同一证据。
- **实际逻辑**：`requireReview`仅在密码非空时签名，创建APPROVED记录时不写`reviewSignatureSnapshotJson`；后续损耗来源校验却强制要求该字段非空，并核对反馈批准人及分配/复核时间一致。
- **业务影响**：正常分配复核保存成功，完工时仍报告生产复核签名证据缺失；共享分配页面不带密码也会创建APPROVED记录，之后`requireReview`只复用已有记录而不会补签。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:8675`；`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:518`；`MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java:214`（同目录）。
- **建议修复边界**：统一复核写入与完工读取的证据合同。共享分配本身可以与签名复核分离，但不得把无签名分配标成不可补签的正式批准；正式批准应写完整签名快照。
- **BDD（待修复验收，未执行）**：Given 合法生产提交，When 组长签名确认分配，Then 完整签名/批准证据可通过完工校验；缺密码不得产生批准记录。
- **修复状态（2026-09-13）**：已静态复核当前代码：`MesReportAllocationCommandService.requireReview` 在 APPROVED 复核写入前强制要求签名密码，创建或补齐复核时写入 `reviewSignatureId`、`reviewSignatureUserId`、`reviewSignatureSnapshotJson` 与同一 `reviewedAt`；前端分配复核提交要求电子签名密码；完工损耗来源读取仍按反馈批准人、复核签名人、分配确认时间与签名快照核对同一证据。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` 通过；`mvn -pl yudao-module-mes -am "-Dtest=MesReportAllocationCommandServiceTest#shouldRejectAllocationReviewWithoutSignaturePasswordBeforeApprovedReviewIsCreated+shouldPersistProductionLeaderTypeWhenAllocationCreatesReview+unchangedInitialAllocationWithoutReviewMustCreateFormalReviewAndAttachCurrentRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。任务证据见 `doc/tasks/20260913-edhr-static-003-signature-contract/verification-report.md`。

## EDHR-STATIC-004：多次报工导致损耗回填必然阻塞

- **涉及步骤**：33—40、55、61—64。
- **触发条件**：同一活跃订单的同一工序有两次合法生产提交，例如上午40、下午60，正式复核与累计数量均完整。
- **预期行为**：按有效提交及其复核、分配逐笔汇总损耗；无损耗也可形成覆盖全部生产记录的无损耗依据。
- **实际逻辑**：损耗读取在验证每条提交之前直接以`matchingEvents.size()>1`判定重复，要求删除重复生产提交；而生产进度和工序完成量允许累计多条分配。
- **业务影响**：正常分次生产即使双进度已满，仍无法完成订单；错误建议还可能诱导删除真实生产记录。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java:81`；`MesTeamLeaderActiveOrderCompletionProgressPortImpl.java:64`；`MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java:163`（同目录）。
- **建议修复边界**：按事件正式身份去重而非按工序限制只能有一条提交；保留逐笔损耗与签名，再按工序汇总。
- **BDD（待修复验收，未执行）**：Given 同工序两笔已复核提交40与60，When 完工回填，Then 汇总两笔真实损耗并成功，不删除合法提交。
- **修复记录（2026-09-13）**：已移除损耗来源读取器的同工序多事件阻断，改为逐事件校验生产反馈、分配、复核签名和结构化 `lossDetails`；完工回填保持按工序快照覆盖校验，允许同一工序多笔正式来源。
- **验证记录（2026-09-13）**：`MesTeamLeaderActiveOrderReleaseLossSourceReaderTest.shouldAcceptMultipleFormalProductionSubmitsForTheSameSnapshot` 覆盖两笔事件、反馈、分配和复核；`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` 与 `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 均通过。

## EDHR-STATIC-005：通用检验任务不能通过生产放行资料校验

- **涉及步骤**：17、18、28、43、50、67、68。
- **触发条件**：订单同时生成专用与通用QA任务，全部完成并经PQC组长确认后申请PQC生产放行。
- **预期行为**：每个任务按自身冻结的规程版本读取来源，专用和通用规程都进入正式过程检验记录。
- **实际逻辑**：读取器遍历订单全部任务，却为每条任务都读取订单主字段中的专用QA版本；写入器随后要求任务规程版本与该专用版本一致，并仅接受专用规程所属模块。
- **业务影响**：通用任务在一线可提交、复核，但到生产放行被判为QA来源不一致，组合规程闭环中断。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:2364`；`MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java:101`、`:144`；`MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java:379`（前三者同目录）；正式调用：`service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java:170`（同一MES Java根目录）。
- **建议修复边界**：逐任务读取和校验冻结QA身份，兼容业务正式支持的通用规程来源；不能删掉通用任务或改为只检查专用任务。
- **BDD（待修复验收，未执行）**：Given 专用与通用任务全部确认且来源完整，When PQC生产放行，Then 两类检验事实都写入对应记录，版本与各自任务一致。
- **修复记录（2026-09-13）**：已改为 reader 按每个 `MesPqcInspectionTaskDO.regulationVersionId` 读取冻结 QA 版本，writer 按 task 自身版本和 `ownerModule` 校验专用 `MES_QA` 与通用 `MES_QA_COMMON` 来源；新增定向 Java 回归和 `mes-edhr-static-005-process-inspection-qa-version-static.spec.cjs` 静态合同。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-005-process-inspection-qa-version-static.spec.cjs` 与 `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest#readsDedicatedAndCommonQaTasksByEachTaskFrozenRegulationVersion,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest#commonQaTaskPlansAlongsideDedicatedQaTaskByItsFrozenTaskVersion" "-Dsurefire.failIfNoSpecifiedTests=false" test` 均通过；定向证据见 `doc/tasks/20260913-edhr-static-005-targeted-fix/verification-report.md`。

## EDHR-STATIC-006：损耗表的生产签名错误归属

- **涉及步骤**：52、53、56、83、90。
- **触发条件**：同一工序先由甲报工，PQC对甲的生产提交记录报废；之后乙又在该工序提交生产记录。
- **预期行为**：每条损耗记录显示其关联生产提交的实际签名人和时间；无明确关联时不得推断人员。
- **实际逻辑**：页面先在整个工序全部生产提交中选取最新生产签名，再把同一个人员/日期填入该工序所有PQC报废行，未按每行的生产来源关联选择。
- **业务影响**：新增一笔生产提交即可改变旧损耗记录展示的生产人员，造成责任归属失真；此问题确认到详情展示，不据此断言归档PDF也使用同一组件。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:2745`、`:2748`、`:2770`；检验提交保留来源事件：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java:1308`。
- **建议修复边界**：详情返回并使用逐条正式生产关联，保留每笔生产/PQC事件与签名身份，不按“最新”补齐缺失关系。
- **BDD（待修复验收，未执行）**：Given 甲对应的PQC报废记录已存在，When 乙在同工序新增报工，Then 旧报废记录生产人仍是甲，日期不变。
- **修复状态（2026-09-13）**：已验证当前代码按 PQC 报废行关联的 `productionSubmitEventId` 返回行级 `productionEventIds` 与 `productionSubmitterSignatures`，前端损耗行使用 `resolveLossProductionSignatures(submission)` 读取当前行来源签名，不再用同工序最新生产签名覆盖历史报废记录。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-006-pqc-loss-signature-contract.spec.cjs` PASS；`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` PASS；`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS（Tests run: 9, Failures: 0, Errors: 0, Skipped: 0）。任务证据见 `doc/tasks/20260913-edhr-static-006-loss-signature/verification-report.md`。

## EDHR-STATIC-007：订单完成后仍接受一线生产提交

- **涉及步骤**：29、38、61—68。
- **触发条件**：订单已完成回填并申请PQC放行，尚未最终放行；现场人员继续通过原订单提交生产。
- **预期行为**：进入完工/放行审核的来源应锁定；如需补录应经过显式受控重新开放流程。
- **实际逻辑**：`markCompleted`只把业务状态改为COMPLETED，活跃状态仍为ACTIVE；一线授权和初始分配仅检查ACTIVE及身份，不检查完工状态或放行申请。正常报工冻结检查只阻止不合格冻结。
- **业务影响**：完工资料固化后仍可新增生产事实。后续复核会重算完成记录，使PQC放行来源哈希不一致；即使未复核，新增事实也未纳入原完工依据。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:85`；`service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java:38`；`service/pro/processpool/team/MesReportAllocationCommandService.java:180`；`service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java:145`（均同一MES Java根目录）。
- **建议修复边界**：完成状态及放行审核状态必须在生产写入口统一阻断；补录需明确重开、作废旧凭据并重新审核，不能只让末端报哈希冲突。
- **BDD（待修复验收，未执行）**：Given 订单已完成且待PQC放行，When 一线再次提交该订单，Then 明确拒绝且生产、签名、分配均零新增。
- **修复状态（2026-09-13）**：已验证并补齐当前代码合同。一线提交授权在锁定活跃订单后调用 `assertActiveOrderOpenForProduction`，拒绝非 `ACTIVE` 业务状态与任何已存在放行申请；分配入口在初始分配、保存和驳回生产提交写入前统一调用 `assertActiveOrdersOpenForProduction`，放行申请存在即锁定生产来源边界。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-007-completed-order-submit-guard-static.spec.cjs` PASS；`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-findings-fix-static.spec.cjs` PASS；`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSubmitAuthorizationTest,MesReportAllocationCommandServiceTest,MesReportAllocationReleaseStateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS（Tests run: 44, Failures: 0, Errors: 0, Skipped: 0）。任务证据见 `doc/tasks/20260913-edhr-static-007-completed-order-submit-guard/verification-report.md`。

## EDHR-STATIC-008：报告齐套被写成全部业务检查通过

- **涉及步骤**：75、76、77、79、85—87。
- **触发条件**：四类报告上传完成，系统创建管理者代表最终放行事务。
- **预期行为**：生产记录完整性、检验、偏差、返工、报废及库存检查应取正式核验结果；未核验不能标记PASS。
- **实际逻辑**：`buildTransaction`直接把`dhrStatus/inspectionStatus/deviationStatus/reworkStatus/scrapStatus/inventoryStatus`全部设为PASS，失败数和阻塞数设为0，检查数量却为4；该阶段只核对四类报告证据及角色。
- **业务影响**：形成并展示没有对应业务核验依据的通过结论。管理者审批分支继续校验附件、来源回执、候选和签名，但未重新生成这六类检查的真实结果；不能把四类附件齐套当成六类业务均通过。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java:157`；同目录`MesProductionReleaseManagerApprovalServiceImpl.java:151`；`service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:656`；`service/pro/batchrecord/MesProEdhrFourMaterialGateServiceImpl.java:52`（同一MES Java根目录）。
- **业务约束依据**：`docs/product/edhr-urs-balloon-pressure-pump.md:108`、`:117`；根AGENTS禁止默认成功掩盖缺少正式来源。
- **建议修复边界**：复用真实业务核验及其证据；未适用与已通过分别记录，检查计数与实际检查结果保持一致。
- **BDD（待修复验收，未执行）**：Given 四类报告齐套但某项业务检查没有正式依据，When 创建最终放行待办，Then 不得把该项写成PASS，并输出明确未就绪原因。
- **修复状态（2026-09-13）**：已补 `MesProductionReleaseBusinessReadinessService`，管理者放行阶段创建事务时读取 DHR 完整性、检验、偏差、返工、报废和库存六类正式业务检查；事务状态、检查数量、失败数量、阻塞数量和 readiness 快照均来自正式检查结果，不再由四类报告齐套硬编码为 PASS。最终管理者审批前会重新计算 readiness，发现阻塞或待预检则拒绝签核。验证证据见 `doc/tasks/20260913-edhr-static-008-manager-readiness-fix/verification-report.md`。
- **验证记录（2026-09-13）**：已执行 HEAD 旧逻辑 RED、静态合同 GREEN、定向 Maven GREEN、bug evidence validator GREEN 和 `git diff --check`；未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-009：QA处置签名没有身份验证

- **涉及步骤**：85、86、87、90。
- **触发条件**：拥有处置权限的用户在不合格评审页面输入任意非空“QA签名”，提交让步、返工或作废。
- **预期行为**：处置签名应经签署时身份确认，并绑定实际签名人、处理内容和评审对象；自由文本不能冒充可验证电子签名。
- **实际逻辑**：页面使用普通文本框；Controller只校验非空；服务仅`requireText`并把文本写入`qaSignature`，不调用电子签名/密码验证，也不保存可验签凭据。
- **业务影响**：可以填入他人姓名或任意文字形成QA签名展示。虽然另存当前登录用户ID，但签名文本与该用户之间没有受验证的关联。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue:122`、`:385`；`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrNonconformanceReviewDisposeReqVO.java:30`；`service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:146`、`:176`（同一MES Java根目录）。
- **业务约束依据**：`docs/product/edhr-urs-balloon-pressure-pump.md:129`要求签署时确认身份并显示人、时间、含义和范围。
- **建议修复边界**：接入正式电子签名验证与证据关联；保留处置前后状态、材料、意见和签名含义，不能只自动填用户名。
- **BDD（待修复验收，未执行）**：Given 待处置评审，When 只提供自由文本或他人姓名而无本人签名验证，Then 不得完成处置；有效签名与本次评审唯一关联。
- **修复状态（2026-09-13）**：已验证当前工作区实现不再接受自由文本 `qaSignature` 作为处置签名；处置 VO、前端 API 和页面均要求 `signaturePassword`，后端在更新评审、解冻或关闭任务前调用 `MesProBatchRecordExecutionSignatureService.recordQaDispositionSignature`，以 `EDHR_NONCONFORMANCE_REVIEW` + 评审 ID 记录 `QA_DISPOSITION` 电子签名，并把 `qaSignatureSnapshotJson` 写入追溯快照。
- **验证记录（2026-09-13）**：证据见 `doc/tasks/20260913-edhr-static-009-qa-disposal-signature/verification-report.md`。已执行静态 RED（HEAD 旧逻辑自由文本签名）、静态合同 GREEN、定向 Maven GREEN；未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-010：批次不合格冻结没有拦住报告上传

- **涉及步骤**：69—75、85。
- **触发条件**：处于四报告待上传阶段的批次被不合格评审冻结，原报告候选人保留上传入口或重试已有上传请求。
- **预期行为**：冻结期间报告写入入口应拒绝，不新增文件或待确认附件；解冻后方可继续。
- **实际逻辑**：报告prepare校验申请状态和候选后进入`validateBatchForSpecialAttachmentSave`；后者排除归档、拒绝、作废及已最终放行，却不排除FROZEN，也不调用不合格冻结校验。完成报告的另一入口反而会拒绝FROZEN。
- **业务影响**：同一冻结批次出现“仍能上传文件、不能完成任务”的不一致；冻结期间依然新增业务附件。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceImpl.java:89`；`service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:2154`、`:2854`、`:2875`（同一MES Java根目录）。
- **建议修复边界**：上传准备、附件保存与报告完成使用同一权威冻结门禁，并在文件存储前校验；不允许高权限绕过不合格冻结。
- **BDD（已修复验收）**：Given 报告待上传批次或工单已冻结，When 原候选人准备上传或完成报告附件，Then 在文件存储、附件读取、附件记录和任务完成前拒绝，且文件/附件零新增；解冻后正常上传。
- **修复记录（2026-09-13）**：已将旧报告附件 prepare/save/complete 统一到 `validateBatchForSpecialAttachmentSave`，该门禁在文件存储、附件读取、附件记录和任务完成前复用 `nonconformanceReviewService.ensureBatchNotFrozen(batchExecutionId, "eDHR批次操作")`，并保留 `BATCH_STATUS_FROZEN` 状态拒绝；新增 `MesProductionReleaseReportServiceImpl.requireProcessable` 对新报告服务入口复用 `ensureBatchNotFrozen(application.getBatchExecutionId(), "生产放行报告上传")` 与 `ensureWorkOrderNotFrozen(application.getWorkOrderId(), "生产放行报告上传")`，保证 prepare/complete 写入口同口径受冻结门禁保护。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-010-frozen-report-upload-static.spec.cjs` PASS；`MesProEdhrBatchExecutionServiceTest.productionReleaseReportPrepareRejectsFrozenBatchBeforeFileStorage`、`productionReleaseReportCompletionRejectsFrozenBatchBeforeFileMetadataReadEvenForGoldenFinger`、`savePendingSpecialNodeAttachmentsRejectsFrozenBatchBeforeBookingPendingFiles`、`MesProductionReleaseReportServiceTest.frozenBatchRejectsPrepareAttachmentBeforeStoragePort`、`frozenWorkOrderRejectsCompleteBeforeReportNodePort` 均在定向 Maven 回归中通过。未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-011：多份评审结束顺序导致工单残留冻结

- **涉及步骤**：85—89。
- **触发条件**：同工单不同PQC提交先后建立评审A、B；原工单未冻结。A记录冻结前false，B记录冻结前true；先处置A，再处置B，均选择可恢复流程的结论。
- **预期行为**：冻结状态由仍有效的冻结原因决定；全部评审结束且无原始冻结原因时，应恢复未冻结，与结束顺序无关。
- **实际逻辑**：创建只按具体来源检查重复，允许同工单多个来源评审；每份单保存当时`temporaryFrozen`，处置时直接恢复该单旧值。B最后恢复true，工单无待评审仍被临时冻结。
- **业务影响**：处理完所有不合格评审后生产/PQC仍被阻断；反向结束顺序却可能正常，形成顺序相关的业务死锁。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:92`、`:126`、`:188`、`:249`；`dal/mysql/pro/batchrecord/MesProEdhrNonconformanceReviewMapper.java:114`（同一MES Java根目录）。
- **建议修复边界**：以工单所有有效冻结原因重新计算状态，区分评审引入的冻结与原始人工冻结；同工单变更需统一加锁。
- **BDD（已修复验收）**：Given 同工单A、B两份评审且初始未冻结，When 任意顺序结束可恢复处置，Then 工单冻结状态按剩余有效冻结原因重新计算；无待评审、无作废处置、无原始外部冻结时恢复未冻结。
- **修复记录（2026-09-13）**：`MesProEdhrNonconformanceReviewServiceImpl.dispose` 不再按当前评审保存的历史 `previousWorkOrderTemporaryFrozen` 直接回写，而是调用 `recomputeWorkOrderTemporaryFreeze`，以原始外部冻结快照、作废处置和 `selectBlockingCountByWorkOrderId` 的剩余有效评审共同决定最终冻结状态；`MesProEdhrNonconformanceReviewMapper` 增加原始外部冻结快照计数和工单阻塞计数读取。
- **验证记录（2026-09-13）**：`MesProEdhrNonconformanceReviewApplicationScopeTest.closingLaterReviewDoesNotKeepReviewIntroducedFreeze` 覆盖后结束的评审历史快照为 true、但无剩余有效冻结原因时恢复未冻结；综合定向 Maven 回归 `MesProEdhrNonconformanceReviewApplicationScopeTest` PASS（14 tests）。未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-012：归档路线名称受现行主数据变化影响

- **涉及步骤**：26、27、80—84。
- **触发条件**：批次已保存生产时的路线编码和名称，归档前路线主数据被改名或删除。
- **预期行为**：最终归档使用该批次保存的路线身份和历史名称，不能随现行主数据变化。
- **实际逻辑**：`buildArchiveManifest`按路线ID读取当前路线，再用当前`code/name`生成归档；路线已删除时直接写null，未使用批次已有的`routeCode/routeName`。
- **业务影响**：最终归档描述的路线名称可能与实际生产采用的版本不一致，或丢失可读路线信息。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:7984`；批次身份字段：`dal/dataobject/pro/batchrecord/MesProEdhrBatchExecutionDO.java`（同一MES Java根目录）。
- **建议修复边界**：归档使用冻结批次/路线快照中的元数据；历史快照缺失时明确报告缺失，不能自动转读当前路线。
- **BDD（已修复验收）**：Given 批次采用路线旧名并保存冻结路线快照，When 现行路线改名或删除后生成归档，Then 归档仍显示批次保存的路线编码、名称和版本；冻结路线身份或快照缺失时明确阻断。
- **修复记录（2026-09-13）**：`MesProEdhrBatchExecutionServiceImpl.buildArchiveManifest` 改为先执行 `requireArchiveFrozenRouteIdentity`，校验批次 `routeCode`、`routeName` 和 `routeSnapshotJson` 中的路线 ID/编码/名称一致，然后将 manifest 的 `routeCode`、`routeName` 写入批次保存值，不再读取当前路线主数据 `routeMapper.selectById`。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-012-archive-route-contract.spec.cjs` PASS；`MesProEdhrBatchExecutionServiceTest.generateArchive_usesFrozenBatchRouteIdentityAfterCurrentRouteRenameAndDelete`、`generateArchive_requiresFrozenRouteIdentityAndSnapshot` 均在定向 Maven 回归中通过。未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-013：输出物料拆开提交导致进度虚增

- **涉及步骤**：7、33、36、38、57—59。
- **触发条件**：同工序冻结输出物料A、B，目标100；一次提交A=50、B=50，与分两次提交A=50、B=50，是相同物料事实。
- **预期行为**：相同各物料累计产出应形成相同工序完成量；不能因把物料拆成多次提交而增加进度。
- **实际逻辑**：提交允许冻结物料的非空子集；每次只在本次子集中取最小完成量。一次提交计50；拆开各计50，再由订单进度按分配求和得到100，没有按全体输出物料累计后取最小值。
- **业务影响**：不同物料数量被当作同一工序产出相加，尚未达到完整产出目标却展示100%。其他已登记缺陷可能先阻止最终放行，本项不据此宣称运行环境已经错误放行。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java:33`、`:48`；同目录`MesProFrontlineFeedbackSubmitServiceImpl.java:171`；`service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java:64`（同一MES Java根目录）。
- **建议修复边界**：保留允许部分物料提交的合同，进度改按订单/工序/物料累计事实计算，明确全体输出物料的完成口径；不靠禁止正常分次提交掩盖问题。
- **BDD（已修复验收）**：Given 同工序输出物料A、B各完成50，When 合并提交或分开提交，Then 工序完成口径按全体输出物料累计后的最小完成量计算，不因拆分提交虚增；A、B都累计达到目标时才完成。
- **修复记录（2026-09-13）**：`MesTeamLeaderActiveOrderCompletionProgressPortImpl` 增加 `calculateConservativeProcessProgress`，按活跃订单冻结工序快照读取 `outputMaterialIds`，再从正式生产提交事件 `materialDetails` 按物料累计产出并取全体输出物料的最小值作为工序完成口径；无输出物料快照时保留原正式分配完成量口径。
- **验证记录（2026-09-13）**：`MesTeamLeaderActiveOrderCompletionProgressPortImplTest.splitOutputMaterialsDoNotAccumulateAsProcessProgress` 覆盖 A=50/B=50 拆分提交不完成，`allOutputMaterialsMustReachTargetBeforeProcessIsComplete` 覆盖只有所有输出物料达标才完成；综合定向 Maven 回归该测试类 PASS（6 tests）。未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## EDHR-STATIC-014：路线升版破坏旧活跃订单的正式批记录绑定

- **涉及步骤**：10、15、26、27、62、67、68。
- **触发条件**：订单O已冻结路线V1和工序R1，尚未完成PQC生产放行；管理员发布同路线V2。
- **预期行为**：O继续使用V1的正式工序及批记录绑定，V2只影响新使用该版本的订单。
- **实际逻辑**：发布投影重新创建路线工序，并按路线ID和用途物理删除全部旧`mes_pro_route_flow_process_batch_record`行；新绑定指向新工序ID。旧订单批记录writer仍按冻结R1查询正式绑定，不从V1正式版本绑定集读取。
- **业务影响**：V2发布后O的冻结工序仍存在于订单快照，却找不到正式批记录绑定，PQC放行报`BATCH_RECORD_BINDING_REQUIRED`。只保存路线JSON并不足以保住下游消费的正式关系。
- **代码证据**：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java:403`、`:652`；`dal/mysql/pro/route/MesProRouteFlowProcessBatchRecordMapper.java:112`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java:261`（同一MES Java根目录）；旧订单冻结：`service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:958`。
- **建议修复边界**：正式绑定应有版本归属并保留旧版可读关系，writer严格读取订单冻结版本；不能回退到最新路线绑定或擅自升级旧订单。
- **BDD（已修复验收）**：Given O冻结V1且逐工序批记录绑定完整，When 同路线发布V2并让O申请PQC放行，Then O从冻结路线版本快照解析V1正式绑定并保留原表单身份；现行路线查询只读取V2当前投影，新订单使用V2。
- **修复记录（2026-09-13）**：`MesProRouteVersionPublishProjectionServiceImpl.projectUseConfigs` 不再物理删除旧 `mes_pro_route_flow_process_batch_record` 绑定，发布新版本时插入新投影并把新的 `routeBindingId` 回写到新路线快照；`MesProRouteServiceImpl.buildBatchRecordReportSnapshot` 保存冻结 `routeBindingId`；`MesProRouteFlowProcessBatchRecordMapper.selectCurrentProjectionListByRouteIdAndUseType` 通过当前 `route_flow_process_config` 排除历史投影；`MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl` 改为从 `MesProRouteVersionSnapshotResolver.resolveVersion(command.getRouteVersionId())` 的冻结 `batchUseConfigs` 解析逐工序批记录绑定。
- **验证记录（2026-09-13）**：`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-014-route-version-batch-binding-static.spec.cjs` PASS；`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-team-leader-active-order-release-application-static.spec.cjs` PASS；`MesProRouteVersionPublishProjectionServiceTest` 定向方法、`MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest`、`MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest` 均在定向 Maven 回归中通过。未执行 E2E、数据库写入、服务重启、Git 提交或推送。

## 暂不登记为缺陷的边界

- 实际检验数量允许调整：前端有明确调整控件，已有后端测试`submitPqcInspectionPermitsAdjustedActualQuantityForEveryInspectionType`明确覆盖此行为；不能把“实际少于计划”单独当成新增缺陷。
- 跨生产工序生成独立PQC任务：当前完整任务身份包含生产工序，近期任务已明确此合同；不以笛卡尔式任务数量直接判错。
- QA专用规程已发布/退休版本编辑：`saveDraftInternal`同时拒绝PUBLISHED与非DRAFT版本，未发现退休版本可被该入口原地改写。
- 四类附件必须齐套、灭菌批号必填、最终放行与归档为独立节点，代码有明确门禁；不把没有运行验证写成这些环节已运行通过。
- 共享分配允许与旧报工终结分离：本次不把“分配不要求PQC质量门禁”本身登记为缺陷；002/003记录的是正式复核证据无法衔接完成回填。

## 2026-09-13 独立代码复核

用户要求不相信修复完成自述，按当前实际代码逐项复核。范围已澄清为14项。

- 总判定：**未全部修复。9项原缺陷修复点成立，5项仍不完整。**
- 原缺陷修复点成立：001、002、004、006、007、009、010、012、014。
- 已重新标记未通过：003、005、008、011、013；上文修复记录保留为历史，不覆盖本次独立结论。
- 003：已有reviewId但缺签名的旧复核，原数量确认仍绕过补签。
- 005：通用版本身份已修，但整版全部项目仍与单项目任务比较，多项目回填失败。
- 008：新检查把正常多个项目/组合规程当作重复或多版本异常，且只按CONFIRMED判断检验通过。
- 011：只读取工单历史第一份评审的外部冻结值，后续轮次可能误清或误保人工冻结。
- 013：正式订单快照不写outputMaterialIds，新算法对真实快照不生效，仍走旧分配累加。
- 完整反例、当前源码行号及测试覆盖缺口：`doc/tasks/20260913-edhr-fix-independent-audit/verification-report.md`。
- 本次仅静态代码复核，未运行构建、业务测试、E2E或数据库操作，未修改业务代码；PASS_STATIC不表示运行态已验收。
