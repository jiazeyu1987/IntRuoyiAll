# eDHR 批记录：通知、审批、个人工作台业务分析与代码核对

本轮整改与主审的最终本地验证结果见[2026-10-28整改复核](edhr-batch-implementation-review-20261028.md)：后端917项定向验证通过。本文保留初始分析、前轮检查及并行环境记录；不能把历史缺口或旧环境结果当作本轮最终结论。

1、分析基线日期：2026-10-27；实施复核日期：2026-10-28。范围为“批记录”下的批次执行、历史追溯、不合格评审、偏差管理、作废五个页签，以及这些页签直接提供的业务动作。共归并为下表 26 类场景；四类必需报告按同一种责任交接合并统计，不把查询按钮逐个算作功能。

2、报告初始基线为工作区静态分析，包含已有未提交改动。初轮本地验证未迁移或部署，浏览器曾发现前后端版本不一致及个人待办来源查询失败；这些属于历史状态。后续已补齐本地迁移、修复运行查询并通过页面检查，最新环境结果见第38—51项及 `output/edhr-workbench-acceptance-20261028.json`。“已接通”仍不等于实际通知送达或全流程E2E通过。

3、业务判断先以职责和状态转换为依据：通知用于让相关人知情；审批用于授权质量结论或变更受控记录；个人工作台用于承接“具体责任人下一步必须完成的动作”。签名不自动等于审批；查看历史、打印、系统回填不应产生审批。

4、用户明确的主线是活跃订单→生产及 PQC→双100%完工→PQC生产放行→上市放行。批次执行承载下游记录及证据，不能另造一轮重复生产审批。文档中的四类上传派单、人工归档，与当前代码主路径已有差异；不能以旧文档或残留类存在证明当前链路完成。

5、业务矩阵。“需要”是本次业务分析建议；“条件需要”以存在真实责任交接为前提。审批可在业务页面完成，不强制所有动作使用 BPM。个人工作台应引用同一正式任务，不另外复制一张审批单。

| 编号 | 功能/业务事件 | 通知需要 | 审批需要 | 个人工作台需要 | 应接收的人及边界 |
|---|---|---|---|---|---|
| 01 | 批次列表、详情、来源资料查看 | 否 | 否 | 否 | 有权限的人主动查阅 |
| 02 | 放行预检、完整性检查 | 发现需他人修复的阻塞时 | 否 | 阻塞需转交处理时 | 缺项责任人；即时校验提示不算通知 |
| 03 | 正式来源自动回填、批次及证据关联 | 成功通常不需要；失败需处理时需要 | 否 | 自动处理不需要 | 不再要求生产/PQC重复确认同一正式事实 |
| 04 | 确有业务需要的人工补填、退回修订 | 是 | 仅更改已确认记录或配置的复核节点需要 | 是 | 当前填写人、修订人、必要复核人 |
| 05 | 必需资料补齐：来料检、灭菌、成品检报告/记录 | 是 | 上传本身否；资料结论在放行审查 | 是 | 实际资料责任人；若由放行人自己补齐，可并入其原任务 |
| 06 | 其他文件上传、删除未定稿附件 | 一般否；明确补件请求需要 | 放行前普通整理否 | 一般否 | 不为每次选择文件、预览或删除草稿单独派单 |
| 07 | PQC生产放行（相邻上游入口） | 是 | 是 | 是 | 正式PQC放行候选人 |
| 08 | 上市放行 | 是 | 是 | 是 | 正式管理者代表/获授权放行人 |
| 09 | 放行责任交接与收回 | 是 | 一般否 | 是，转移原任务 | 新旧责任人；交接不能复制第二次放行审批 |
| 10 | 放行通过、驳回、撤回结果 | 是 | 结果本身否 | 驳回后有补正动作才需要 | 申请人、生产组长及真正承接下一步的人 |
| 11 | 最终归集、归档生成 | 人工归档或失败需处理时 | 否 | 人工归档或失败需处理时 | 自动成功归档不应再造关闭审批 |
| 12 | 历史追溯、反查、签名/审批/审计记录查看 | 否 | 否 | 否 | 读取既有证据，不产生新任务 |
| 13 | 归档预览、下载、打印 | 否 | 否 | 否 | 权限控制、必要审计即可 |
| 14 | 发起不合格评审、冻结对象 | 是 | 发起本身否 | 是 | QA处置责任人；同步告知受影响生产/放行责任人 |
| 15 | QA让步、返工、作废决定 | 是 | 是 | 是 | QA正式评审并签署；不要求再做一份相同决定的审批 |
| 16 | QA决定后的让步恢复、返工接手 | 是 | 执行已批准决定本身否 | 是 | 原生产组长；需要实际接手，不能只有消息 |
| 17 | QA作废结果、作废列表及详情 | 结果需要；查看不需要 | 不重复审批QA已作出的作废决定 | 否 | 相关人知情，停止原正常待办，保留历史 |
| 18 | 发起偏差 | 是 | 记录事实本身否 | 是 | 接收人/质量责任人；发起签名是事实确认 |
| 19 | 偏差调查、纠正措施、验证 | 是 | 调查执行本身否；验证需责任确认 | 是 | 调查负责人、纠正责任人、验证人，按实际阶段交接 |
| 20 | 偏差质量审核与关闭 | 是 | 是 | 是 | QA/质量授权人；按风险决定是否增加管理者代表 |
| 21 | 重大偏差转不合格评审 | 是 | 在不合格处置节点审批 | 是 | QA；不得同时要求两套重复质量结论 |
| 22 | 独立作废申请、审批、撤回 | 是 | 作废决定需要；撤回待决申请一般不需要新增审批 | 是 | 作废审批人；结束后通知申请人并关闭待办 |
| 23 | 重开原记录、已确认记录补录/变更 | 是 | 是 | 是 | 变更审批人，再交修订责任人；不能静默改正式记录 |
| 24 | 不适用的可选表单跳过 | 一般否；产生下一任务时通知下一人 | 已批准可选规则内不需要额外审批 | 结束原任务，不单独建跳过任务 | 必需质量证据不能以可选跳过替代 |
| 25 | 模拟、金手指、批量管理工具 | 正常生产不需要 | 不能替代正式审批 | 正常生产不需要 | 仅在获授权的测试/管理范围内使用 |
| 26 | 任务超期、责任人变更 | 是 | 否 | 保留或转移原待办 | 当前责任人及有明确职责的升级接收人 |

6、2026-10-27 初始实现对照（历史基线，后续修复状态见第30项起）。状态不按“出现过类名或接口”评定；入口未调用的旧链路明确单列。

| 场景 | 当前代码事实 | 判断 |
|---|---|---|
| 01、12、13、17的查看 | 当前列表/详情/追溯/打印入口读取业务数据，不为查看动作创建审批 | 符合业务；不应补通知或审批 |
| 02 | 有预检和检查结果；没有看到把一般缺项按责任人转成补正待办的统一链路 | 即时检查已具备；跨人整改闭环不足，不能把提示当派单 |
| 03、04 | 有正式来源物化，也保留 FILL/REWORK/REVIEW/APPROVE 任务能力；PQC创建批次路径还调用初始填写任务生成 | 能力存在；不能认定自动回填后的全部填写/审批仍有业务必要，需按实际剩余字段区分 |
| 05 | 当前主入口的资料上传写活跃订单资料表并留审计，没有通知/个人任务创建；旧四类上传初始化会建FILL任务，但生产代码中没有调用它 | 当前主链不能认定已完成资料责任派单；旧路径有任务能力但未接通，且自身没有分派通知 |
| 06 | 当前详情提供资料上传/删除，校验身份、冻结和上市放行状态，落文件关系及审计 | 普通整理无需单独审批，现状方向正确；收到补件要求时仍需明确待办 |
| 07 | 放行申请持久化创建PQC工作任务并登记通知；工作台可读取同一工作任务；PQC决定关闭该任务 | 核心审批、分派通知、工作台代码已接通 |
| 08 | PQC批准后直接初始化管理者代表任务并登记通知；上市放行校验身份、资料和签名，完成任务并更新终态 | 核心审批、分派通知、工作台代码已接通 |
| 09 | 更新原任务处理人及候选快照，保存交接记录、更新权限并登记交接通知 | 已接通；没有必要为交接再建重复审批 |
| 10 | 放行决定落库并完成原任务；主要通知服务当前针对分派和交接，决定分支未见通知申请人/生产组长或创建补正任务 | 结果留痕有，结果告知和驳回后补正交接不足 |
| 11 | 活跃订单受管放行分支直接置ARCHIVED，不建人工归档任务；非受管分支置CLOSED并建ARCHIVE任务、通知 | 两条路径不同；不能把旧归档派单当当前主链行为，也不能仅凭ARCHIVED宣称PDF归档包生成成功 |
| 14、15 | 创建评审并冻结批次/工单；处置校验QA权限、材料、签名，写正式决定和状态；创建路径未调用QA任务派发 | 业务评审已实现；QA收到通知、个人工作台收到待办未达成 |
| 16、17结果 | QA处置后给原生产组长创建交接并发通知；让步/返工的交接为TODO，但notificationOnly=true | 通知有；需要行动的交接没有进入个人工作台，且被审批聚合排除；作废只通知则合理 |
| 18、19、20 | 偏差有发起、接收人、调查、纠正责任人、内容版本、节点签署和关闭；没有通知和任务派发调用 | 页面能力有；责任人收到通知、阶段待办、审批任务归属未闭环 |
| 21 | 重大偏差校验后调用不合格创建并关联，继承其冻结与处置 | 转换能力有，继承QA派单缺口 |
| 22 | 普通批次作废走业务审批编排；BPM策略可创建审批并处理回调，BPM有分派/结果消息；金手指另走直接生效 | 正常审批能力有；部署策略未验证。个人工作台注册来源不含BPM作废，不能当作工作台已覆盖 |
| 23 | 重开、补录申请写change_event为SUBMITTED，有后续审批方法及页面；未见该申请创建工作任务或通知 | 审批动作有，申请到审批人的交接缺失 |
| 24 | 跳过校验可选规则、责任人、原因及签名，结束对应填写任务；正式受管报告禁止走该跳过入口 | 规则内跳过无需另加审批，当前有边界保护 |
| 25 | 页面保留模拟和金手指入口；金手指批量作废可直接生效 | 正常业务不需要的管理旁路确实存在；不据此认定其测试用途也不需要 |
| 26 | 普通工作任务有超期处理和重新分派消息方法 | 代码能力存在；调度配置、实际投递与所有候选人通知覆盖未运行验证 |

7、确认的缺口A：不合格评审“等QA处理”，但没有创建QA交接。`create`写评审、冻结和审计后直接返回；`QA_REVIEW`只保留类型、展示和处置时尝试关闭旧任务。不能因为存在QA_REVIEW常量就算已派单。业务后果是QA需要主动查列表，且冻结中的对象可能等待无人承接。

依据：[不合格创建](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:314)、[QA处置及结果交接](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java:297)。

8、确认的缺口B：偏差“有接收人/纠正责任人字段”，但填写这些人不会自动通知或派单。签署只按节点权限校验并记录当前内容签名，没有生成该节点责任人的审批待办。因此“页面可签署”与“审批已流转到个人”必须分别评价。

依据：[偏差发起](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationServiceImpl.java:198)、[签署与关闭](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationHandlingServiceImpl.java:171)、[偏差详情操作](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue:239)。

9、确认的缺口C：QA让步/返工后原生产组长有真实接手动作，但记录被标为仅通知。交接保存并发消息，个人工作台却只接入EDHR_WORK_TASK等来源，没有接入该交接表；审批聚合也过滤notificationOnly。结果是消息可见不代表个人待办可见。QA作废后的只读告知应继续仅通知，不应一并改成待办。

依据：[结果交接创建](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java:302)、[交接落库及通知](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java:403)、[工作台来源清单](E:/IntRuoyi/IntRuoyiFronted/src/workbench/manifest.json:1)、[审批交接过滤](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/MesActiveOrderHandoffApprovalProjection.java:41)。

10、确认的缺口D：重开/补录申请没有任务交接。`requestReopenBatch`、`requestSupplement`只保存申请；存在approve方法并不能保证审批人会收到任务。业务上应把已确认记录变更交给授权审批人，再把批准后的修订交给处理人。

依据：[重开申请](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java:363)、[补录申请](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java:518)。

11、确认的缺口E：当前资料上传缺少按责任派单闭环。主页面的“其他文件”等实际调用活跃订单资料服务，该服务保存文件关联、版本和审计，没有生成补件任务。旧四类上传初始化虽会建立任务，但`initializeRequiredReportStage`在生产源码中只有接口和实现定义，未找到调用；当前PQC批准直接创建上市放行任务。

依据：[当前详情组件](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue:16)、[实际上传](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:4167)、[资料落库](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:161)、[旧四类派单](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesProductionReleaseReportStageInitializerImpl.java:69)、[当前PQC后续](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:218)。

12、确认的缺口F：放行结果的告知没有与分派通知一样形成闭环。PQC驳回、上市放行拒绝分支写决定、完成原任务、记审计后返回。没有看到把驳回结果通知申请人并明确派出补正工作的链路。消息服务已实现的“分派”和“交接”不等于“结果告知”也已实现。

依据：[PQC驳回](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:247)、[上市放行拒绝](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:365)、[放行分派通知](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationService.java:33)。

13、初审时怀疑普通执行任务混入审批中心，后续对照 `MesEdhrApprovalCenterQueryContractTest` 发现此判断不成立：候选签名任务可以使用 `FILL` 类型，测试专门证明候选分支必须保留。这不是已确认的多余实现，本次不改审批候选查询；若需进一步区分“签名填写”与普通填写，应依正式签名节点身份（如 `signatureCellKey`）和完整测试另行证明，不能仅按taskType删除。

依据：[候选任务契约测试](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/approval/MesEdhrApprovalCenterQueryContractTest.java:57)。

14、确认的待办状态问题：个人工作台读取TODO/OVERDUE任务，只排除终态批次及非归档的CLOSED批次，没有排除FROZEN=15。创建不合格评审后普通任务仍可列为“待处理”，但实际资料维护等入口拒绝冻结对象。应显示挂起/阻塞及原因，或移出可执行待办，而不是让用户反复进入才发现不能做。

依据：[工作台查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml:4)、[批次过滤](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesOpenWorkTaskVisibility.java:16)、[冻结阻止资料修改](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:269)。

15、主线外代码确实保留：旧四类报告上传阶段、人工关闭/归档分支、初始人工填写流、模拟及金手指批量作废。它们不能算当前活跃订单主线必须经过的节点。其中金手指批量作废直接调用生效执行器；正式生产若必须经过独立批准，该旁路不能充当合格实现。是否删除测试能力需结合其明确用途决定，本次未删改。

依据：[当前受管放行直接进入历史状态](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:1083)、[旧关闭并派归档](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:4904)、[创建后初始填写](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:1245)、[金手指直通作废](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:602)。

16、不能直接判定多余的代码：普通偏差关闭要求编制、验证、部门负责人、QA、质量负责人五类签署，重大偏差增加管理者代表。业务上需要职责确认和质量批准，但是否所有偏差都必须五/六签，需要正式业务规则支持；不能仅凭代码有这些节点就说它们必需，也不能在未核对规则时全删。

依据：[签署节点集合](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationHandlingServiceImpl.java:49)、[关闭校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationHandlingServiceImpl.java:229)。

17、已接通的核心不应重做：PQC/上市放行工作任务、候选人快照、分派通知、工作台读取、决定后任务完成，以及上市放行交接/收回。建议沿这些正式任务统一接入缺失场景，避免另造审批数据。消息需要实际投递回执；工作台需要匹配处理人、可操作状态和业务入口。

依据：[PQC派发](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationPersistenceService.java:91)、[管理者代表派发](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java:180)、[工作台适配器](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesEdhrWorkbenchSource.java:10)、[交接更新同一任务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesMarketReleaseHandoverService.java:143)。

18、普通作废审批已接入业务编排/BPM，BPM本身有任务分派和审批结果消息；个人工作台目前的来源清单不含BPM任务。因此应区分“审批中心有作废待审批”与“个人工作台也有”。不能为解决工作台缺入口，再创建第二个作废审批。

依据：[作废提交](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java:207)、[BPM任务通知](E:/IntRuoyi/IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java:1515)、[BPM结果通知](E:/IntRuoyi/IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java:1158)。

19、原处理建议已部分完成：QA结果接手、偏差接收/纠正闭环、放行决定结果通知和冻结待办提示已接通。仍需明确QA评审、重开/补录的正式接收角色，以及放行驳回后的修订人。审批候选查询按已验证的签名任务契约保留，没有按普通 `FILL` 类型误删候选分支。历史查看、打印和普通附件整理不增加审批。

20、需明确的业务选择仅有：必需资料由独立资料责任人提交还是由放行人补齐；最终归档自动完成还是人工交付；偏差签署层级按何种风险划分。不同选择影响派单节点，但不影响“审批与执行待办分开、需要行动就有责任人、只读操作不审批”的结论。

21、本次核对依据包括当前前端五页签和详情入口、API映射、控制器权限、业务服务、任务/状态落库、通知发送、审批聚合、个人工作台来源和终态过滤。未核对运行数据库中的菜单授权、审批策略、通知模板、定时任务和实际投递记录；不能把本报告作为生产环境验收通过证明。

22、业务背景参考：[五页签定义](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue:8)、[角色操作口径](E:/IntRuoyi/docs/product/production-role-system-operations.md:11)、[较早90步流程](E:/IntRuoyi/docs/product/edhr-production-inspection-release-flow.md:77)。90步文档含当时实现说明，不是本次所有“应当”判断的唯一依据；其上传、归档顺序与当前代码差异已在上文显式说明。

23、2026-10-27 实施复核：QA让步/返工结果接手现为可操作交接任务，作废结果仍是只读通知；偏差接收人及纠正责任人会收到个人任务/通知，责任人修改会撤销旧任务并重派，关闭偏差会结束相关任务；冻结批次保留待办记录但响应标记不可办理并给出阻塞原因。偏差责任人必须已启用，且具备偏差查询、偏差处理和eDHR工作任务查询权限，避免派单后无法打开入口。

24、剩余未完成项：QA不合格评审创建后仍无权威QA候选人名单，不能猜人派单；重开/补录审批和放行拒绝后的实际修订人也没有已配置的负责人规则。增加这些任务前需先确定正式候选来源。当前不合格QA处置 UI 和偏差签名层级仍可执行，但不等于其入口已形成完整派单闭环。

25、首轮定向回归与更宽工作任务测试状态见此前记录；其中更宽的 `MesProEdhrWorkTaskServiceImplTest` 曾受H2市场交接表缺失影响，不能宣称整类全绿。续做中已补齐核心任务目标测试、导航、通知和投影测试；截至2026-10-28的具体验证集见第29项。

26、2026-10-28 继续复核：QA结果接手任务已进入正式个人工作台来源与S3/S4投影；冻结普通批次任务在工作台显示阻塞原因且前端禁止进入，QA/偏差事项不受批次冻结拦截；生产、PQC批准/驳回及上市批准结果通知原申请人，结果消息复用持久投递、审计和本人重试能力，不创建待办。

27、接收人配置复核：`20260901_mes_pqc_leader_nonconformance_review_permission.sql` 明确禁止 `pqc_leader_permission` 获得 `mes:pro-edhr-nonconformance-review:dispose`。所以QA创建者/发起人角色不能推断为处置审批人；当前没有可用于冻结QA接收候选人的业务角色合同。`mes:pro-edhr-change:approve` 有菜单权限且测试全角色脚本仅给super_admin同步，不构成正式生产审批责任规则。QA评审和重开/补录审批派单继续阻断，直至有明确的组织角色或经业务确认的接收规则。

28、偏差闭环再修正：纠正措施记录已带 `completedAt` 时只完成现有纠正任务，不再先建立新任务；偏差常规关闭及关键偏差转不合格评审都会完成其接收/纠正任务，避免原责任待办残留。任务指派要求接收人已启用并同时具备偏差查询、偏差处理、eDHR工作任务查询权限。

29、继续验证：Maven本轮130项（偏差处理21、不合格转审10、QA交接51、放行通知23、PQC放行24及偏差任务目标用例1）全部通过；工作台后端reactor编译及27项定向回归通过。前端交接行为21项、通知入口2项、注册14项、S4契约3项、偏差导航契约及TypeScript检查通过；通知迁移契约测试通过；任务范围 `git diff --check` 通过。未执行真实浏览器E2E、数据库迁移、部署或投递。

30、2026-10-28 实施状态说明：第6—29项保留为分析和前轮验证记录，不代表最新代码仍存在同样缺口。本轮采用明确选人的责任交接：QA评审人在不合格创建/重大偏差转审/批次驳回入口选择；记录变更审批人在重开或补录申请中选择。后台校验启用状态、当前租户、对应业务权限，记录变更申请人不得自审。未配置历史单据须经显式“指派QA评审人”补齐，不按角色猜人或批量回填。

31、按原问题逐项落实（本地代码状态，运行验收另列）：

| 原问题 | 本轮处理 | 主要代码依据 |
|---|---|---|
| QA不合格评审无派单 | 创建正式 `QA_REVIEW` 交接和通知；指定QA才可处置；处置后完成原任务。历史待评审单有显式指派入口 | `MesProEdhrNonconformanceReviewServiceImpl`、`MesActiveOrderHandoffService`、不合格/重大偏差/批次驳回前端入口 |
| 偏差调查、纠正和质量签署无责任流转 | 保留调查/纠正待办，新增按当前内容版本、摘要、节点和指定人绑定的执行签署、质量审批及关闭待办；修改版本撤销旧任务 | `MesProEdhrDeviationHandlingServiceImpl`、`MesProEdhrDeviationReviewTaskService`、`DeviationDetail.vue` |
| QA让步/返工接手只有通知 | 真实接手交接进入同一个 `EDHR_WORK_TASK` 工作台来源及S3/S4；作废结果继续只读告知，不派生产执行任务 | `MesActiveOrderHandoffService`、`MesEdhrWorkbenchSource`、`MesWorkbenchTodoMapper.xml`、`MesProjectionExportMapper.xml` |
| 重开/补录无正式审批待办 | 指定独立审批人、通知、审批中心和个人待办；以正式签名和事件证据完成审批。重开允许受控修订，补录建立新版本；按原填写责任快照派修订任务 | `MesEdhrRecordChangeTaskService`、`MesProEdhrRecordChangeServiceImpl`、`MesEdhrRecordChangeEditGuard`、`MesEdhrSupplementRevisionService`、`RecordChangePage.vue` |
| 活跃订单资料责任无闭环 | 当前生产组长可按四类资料明确指派、转派；通知责任人并进入工作台；本人上传同订单、同类别、当前轮次的新资料后完成原待办 | `MesActiveOrderDossierCompletionService`、`MesActiveOrderDossierFileService`、`DossierResponsibilityAssignment.vue`、`DossierCompletionPage.vue` |
| PQC正常驳回后无补正交接 | 原申请人且为当前生产组长接收一张补正任务；通知合并为结果与指派，避免重复；上传有效新附件及原因后重新提交正式放行轮次。历史正常驳回有显式接收入口 | `MesPqcReleaseCorrectionService`、`MesPqcProductionReleaseServiceImpl`、`PqcReleaseCorrection.vue`、`TeamLeaderWorkbench.vue` |
| 放行结果未告知 | 生产放行和上市放行结果复用持久通知、审计与重试能力；不因纯结果再创建审批。正式作废/不合格分支沿各自质量链路处理 | `MesReleaseOutcomeNotificationService`、PQC/上市放行业务服务 |
| 审批与执行任务混淆 | 审批候选限定正式审批类型，另保留具有非空 `signatureCellKey` 的候选FILL签署节点；普通FILL、资料补齐、调查、归档不混入待审批 | `MesProEdhrWorkTaskMapper`、`MesProEdhrApprovalTaskAdapter` |
| 冻结、失效轮次的普通任务仍可办理 | 冻结普通工作任务显示阻塞原因并禁用跳转；资料/补正任务额外核对真实订单、当前放行轮次、冻结/作废状态；记录变更审批可在受控历史状态下办理 | `MesOpenWorkTaskVisibility`、`MesOpenDossierTaskVisibility`、`MesOpenPqcCorrectionTaskVisibility`、工作台查询与导出 |
| 旧上传、人工关闭和直通作废旁路 | 删除未调用的旧四类上传初始化、人工关闭入口及金手指批量直接作废；保留真实归档证据和正式批准后的作废生效执行器 | 批次控制器/服务/VO、`edhr-batch`前端、PQC初始化结构 |
| 既有PQC/上市放行与交接 | 复用原正式任务和候选快照，不重复建立放行审批；受影响的通知、签名证据、任务归属与轮次回归一并核验 | PQC放行、管理者代表阶段、上市放行交接及审批结果读取器 |

32、没有随意删除的业务规则：偏差现有五类/重大偏差六类签署仍保留，区分“执行确认”和“质量批准”；未把普通上传或所有签名扩成审批。真实人工归档证据路径、正式作废生效执行器也保留。查询、历史、预览、下载、打印、保存草稿、自动回填成功不新建通知/审批/待办。

33、迁移与权限：新增QA评审人、偏差节点责任人、记录变更/补件/补正通知模板及正常驳回后新轮次约束的迁移脚本均在 `IntRuoyiBackend/sql/mysql/20261028_mes_*.sql`；使用原有权限，新增详情入口在 `config/menu-migrations.json` 记录具体免迁移理由。脚本依赖、目标表/列和模板必须在运行环境真实具备，代码没有以缺模板或缺表时模拟成功。

34、独立作废的个人工作台已补充代码：`gpt-6.1-sol / high` 子Agent实现专用只读来源 `BPM_EDHR_VOID`，复用原BPM任务和流程详情入口，正式申请、作废事件、流程实例及租户须相互一致，不另建审批。主审修正页面权限交集、S4授权有效期及查询超时；回归修正跨Mapper片段引用，新增11项作废测试及共64项BPM/S4验证全部通过。引擎实体事件使分派、候选及状态变化失效。

35、本轮最终本地验证：MES综合853项与BPM/S4相关64项共917项全部通过，0失败、0跳过；原工作任务/NCR事务、记录变更和字段审计失败已关闭。前端23项原定向检查、子Agent作废导航及注册契约17项均通过（存在重复，不相加统计）；完整TypeScript和菜单检查通过。后端使用真实依赖快照并优先编译当前受影响源码，没有声称全仓库Maven或真实E2E通过；详细证据见本文开头整改复核链接。

37、独立作废投影的S4发布前置：本轮增加第六个工作台来源，并更新前后端 `registryRevision`。未来显式启用S4时，须沿现有generation构建/激活流程更新控制头；该要求不适用于当前S3直接查询模式。未更改运行数据库的registry/generation或审批策略。当前实际模式另见第52项。

36、运行环境边界：此前Playwright只读检查时，个人工作台返回 `SOURCE_QUERY_FAILED / 1002034011`；新增业务代码与数据库迁移尚未在该环境验收。未通过API/数据库代替真实前端执行业务动作，未实际验收通知送达。故本报告不能用于声称已部署、业务端到端通过或全部26场景运行达标。

38、后续环境执行记录（覆盖第29项当时的环境状态）：2026-10-28 09:40 已在本地 `127.0.0.1:23306/ruoyi-vue-pro` 执行偏差分派、放行结果两份通知迁移，5个模板均唯一且启用。回执为 `output/edhr-notification-migration-20261028.json`；重启后只读复查仍为5个有效模板，没有重复执行迁移。

39、真实页面验证发现工作台查询故障：按“批记录”筛选时，动态SQL的连续空行使租户SQL解析器截断筛选条件或解析失败。修复共享筛选片段的XML空白边界，并对MES固定SQL常量做首尾空白清理；保留租户、本人可见性、隐藏记录、业务任务资格和游标条件。正式JUnit共7项通过，包含5个来源、计数/分页、3种可见性、9种筛选/排序/游标变体，共270个组合。证据为 `output/edhr-count-diagnosis/junit-contract-results.txt`。

40、运行包边界：`output/runtime/int_main/edhr-query-2026-10-28-16-30-58/deployment.json` 记录基于当时正在运行的二进制更新两处查询资源的过程，所有Java类保持不变。全量Maven尝试曾被并行DCC源码的编译错误阻塞，不能据此声称全量构建通过。`output/edhr-runtime-acceptance-20261028.json` 对比的是旧09:52运行包与后续变化的编译产物，已作历史记录处理，其false项不能用来判断当前运行包。

41、电脑重启后的复核：修复包SHA256为 `be9455dc4b8db438cd69ff9d5cdbd183d943a16d3cf9ac920724e92ccd9815f6`，后端PID48656监听48081，健康状态UP。正确工作目录下JUnit再次7/7通过（270组合），证据为 `output/edhr-count-diagnosis/junit-reboot-corrected.log`；首次复跑因工作目录错误未找到XML，其失败日志单独保留，不作为功能失败或通过依据。当前源码中并行增加的任务名称映射不在该运行包内，其余查询修复已保留。

42、当时页面验收阻塞（后续结果见第44项）：Playwright访问 `http://127.0.0.1:8081/user/profile` 返回 `net::ERR_CONNECTION_REFUSED`。共享前端由另一任务恢复，本任务已停止自己的重复前端启动进程。子Agent只读核实新前端仍在Vite配置的UnoCSS依赖导入阶段，尚未监听8081；尚不能断言死锁、依赖损坏或插件故障，没有绕过插件、另启端口或停止其他任务进程。

43、18:09起恢复真实页面验证：原5个来源的真实MyBatis→租户解析→只读JDBC均成功；新增 `BPM_EDHR_VOID` 来源在 `countBrokenDomain` 的两处数字转文本标识连接报 `Illegal mix of collations`，导致整个工作台查询失败。源码采用两侧显式二进制比较后，保留批次/申请/流程的精确身份、租户及本人候选条件；没有移除来源、改变数据库排序规则或绕过异常。子Agent复核两种连接排序规则下8次真实只读查询全通过，补强的11项契约测试全部通过。

44、18:26真实页面验收通过：管理员从个人工作台按“批记录”筛出任务2833、2391，共2条；切换“已隐藏”显示当前筛选下0条，返回待办恢复2条；详情列排序查询成功；点击“进入/处理”进入 `/mes/production-release/pqc?applicationId=268&workTaskId=2833` 并打开“确认生产放行”窗口。所有观察到的工作台分页响应code=0。截图为 `output/playwright/edhr-batch-filter-final.png`、`edhr-batch-hidden-final.png`、`edhr-workbench-pqc-navigation-final.png`，观察结果为 `output/edhr-count-diagnosis/browser-final-results.txt`。本轮只验证办理入口，没有提交放行或修改现有业务记录。

45、当前运行核验：PID63764、48081健康状态UP；前端PID49192监听8081。实际包为 `output/runtime/int_main/edhr-query-2026-10-28-18-21-09/yudao-server-exec.jar`，SHA256为 `11a689832a7692b5b189dc2be940e9edfbc3b6c47d93c91b39861356fc35d58f`；system/MES/BPM三份查询资源均与当前源码完全一致。并行任务已启动与本线程准备包内容相同的修复包，本线程部署前身份检查发现运行环境变化即停止部署，随后只读复核并完成页面验证，未重复重启。最新回执为 `output/edhr-workbench-acceptance-20261028.json`。

46、验证边界：本轮数据库没有该管理员可处理的作废审批任务，相关8次真实SQL验证结果为空或0，不能等同于作废审批全流程通过。QA/偏差派单、通知实际投递及审批完成仍须使用任务自有业务数据走完整页面路径；查询契约、BPM契约及本轮筛选/跳转通过不能代替这些业务验收。第24、27项为当时接收规则的分析状态，最新代码整改另见第30—37项。

47、后续迁移补齐：已执行偏差签署责任、记录变更通知、资料补件通知、PQC正常驳回轮次、PQC补正通知共5份迁移；QA评审人列已符合要求，不重复执行。6个新增模板均唯一、启用且参数完全匹配；偏差JSON字段、PQC唯一索引及生成表达式5种只读计算场景均核验通过。加上第38项已执行的2份，本任务共执行7份迁移。回执：`output/edhr-remaining-migrations-20261028.json`。首次末尾校验因MySQL表达式引号转义而失败，后续仅修正只读校验，未重跑已成功的迁移。

48、迁移后真实页面补验：QA评审、偏差、记录变更、PQC放行四个列表查询全部code=0；QA创建明确显示评审责任人；记录变更的独立审批人选择器返回12名候选人且排除当前管理员。未提交创建或审批。证据：`output/edhr-count-diagnosis/business-pages-results.txt`、`assignment-entry-results.txt`。选择器检查首次超时源于脚本在打开选择器前等待其懒加载请求；修正操作顺序后通过，没有修改产品实现。

49、运行代码补核：从当前实际运行包提取记录变更任务、偏差审核任务、资料补件、PQC补正、补录版本、不合格评审、记录变更、偏差处理、放行结果通知9个关键业务类，与917项本地验证使用的有效类路径逐字节一致。部分target目录为旧产物，不能据其差异误判当前运行代码缺失。证据：`output/edhr-count-diagnosis/runtime-business-class-comparison.json`；不据此宣称全量Maven构建通过。

50、完整业务验收仍缺前置数据：当前新偏差任务、记录变更事件/审批、资料补件任务、PQC补正任务与正常PQC驳回申请均为0；QA待评审68/69及偏差7/8为9月历史记录，未确认属于本任务，未修改。后续须用前端建立任务自有活跃订单/正式批记录，具备独立责任人账号及真实材料，再验证通知实际送达、审批决定和待办结束；现有查询与入口通过不代替全链路E2E。

51、最新环境核验汇总：`output/edhr-workbench-acceptance-20261028.json` 已补记迁移、四页只读检查、选人入口、9个运行类一致性及完整业务验收的缺失前置。第36项的工作台查询故障已修复，第33项所列本地迁移前置已补齐；实际业务通知送达与审批闭环仍未标记通过。

52、实际工作台模式确认：Playwright从真实个人页观察 `/workbench/context` 响应code=0、`contractVersion=3`，与运行配置及S3装配代码一致。当前S3直接查询业务来源，不依赖S4 generation，因此不存在“当前环境尚未激活S4而不能运行”的缺项。未修改引擎模式、commit protocol或generation。证据：`output/edhr-count-diagnosis/active-workbench-protocol.txt`。
