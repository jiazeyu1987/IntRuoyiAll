# eDHR额外逻辑问题：既有14项之外

最新修复判定：见当前缺陷索引及文末“2026-09-14 后续调用链复核与修复（最新）”；此前OPEN统计为历史。026已恢复登记，补充编号现为015—032。新增主流程修复的当前证据见文末2026-09-15章节。

## 历次审计范围与边界（历史）

- 第一轮确认015—020六项；第二轮新增021、022；第三轮（2026-09-14）继续主流程审计，新增023—025，累计11项。
- 级别：P1 10项、P2 1项；本轮已更新016当前源码层修复状态，其它条目保持原登记状态。第二轮另补充原012未闭环证据，不新增编号。
- 既有001—014及其复核遗漏不重复统计。本轮不是全仓无遗漏证明。
- 证据来自当前工作区真实前端入口、Controller、Service及下游读取；未执行构建、业务测试、E2E、API或数据库操作。
- 完整源码索引、指纹和文档验证见 `doc/tasks/20260913-edhr-additional-logic-audit/verification-report.md`。

## 当前缺陷索引

| 编号 | 级别 | 问题 | 状态 |
|---|---|---|---|
| EDHR-STATIC-015 | P1 | 生产更正只改事件，正式反馈表仍保留旧损耗事实 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-016 | P1 | PQC更正绕过不合格冻结，冻结期间仍能改检验事实 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-017 | P2 | PQC更正允许损耗数量大于实际检验数量 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-018 | P1 | 普通订单备注中的模拟标记可把领料来源切换到另一订单 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-019 | P1 | 原始记录修订接口信任客户端签名和修改人身份 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-020 | P1 | 有损耗报工总量被要求等于合格分配量，正常损耗单无法放行 | FIXED_TARGETED_REGRESSION_20260914 |
| EDHR-STATIC-021 | P1 | 最终放行要求库存追溯，但正常领料链未生成所需来源 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-022 | P1 | 库存检查把同类型的不同合法明细判成重复来源 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-023 | P1 | 跨订单分配后的列表进度、完工及放行仍按目标工单查来源 | FIXED_FLOW_REGRESSION_20260914 |
| EDHR-STATIC-024 | P1 | 动态过程检验已返回正式证据，PQC放行仍只认传统执行编号 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-025 | P1 | 同工序多项检验逐项写同一动态表单，首项生效后阻断后续项 | FIXED_CURRENT_STATIC_20260914 |
| EDHR-STATIC-026 | P1 | 条件必填只在最终检查求值，填写与进度未统一不适用状态 | FIXED_TARGETED_REGRESSION_20260914 |
| EDHR-STATIC-027 | P1 | 动态损耗表已写入，但PQC放行仍强制要求传统损耗记录编号 | FIXED_FLOW_REGRESSION_20260914 |
| EDHR-STATIC-028 | P1 | 一线生产提前要求领料批号，无法先生产再完工回填 | FIXED_TARGETED_REGRESSION_20260915 |
| EDHR-STATIC-029 | P1 | 自动检验/损耗回填依赖人工待办及人工推进人，阻断生产放行和后续工序 | FIXED_TARGETED_REGRESSION_20260915 |
| EDHR-STATIC-030 | P1 | 路线动态表单按创建人而非真实提交人完成待办 | FIXED_TARGETED_REGRESSION_20260915 |
| EDHR-STATIC-031 | P1 | 放行报告待办阻断普通工序派发，与最终放行要求形成相互等待 | FIXED_TARGETED_REGRESSION_20260915 |
| EDHR-STATIC-032 | P1 | 物料配置保存/一线读取与活跃订单冻结的来源不一致 | FIXED_TARGETED_REGRESSION_20260915 |

## EDHR-STATIC-015：生产更正后两套正式事实不一致

- **真实入口**：生产组长工作台“修改报工”→`correct-production-report`。
- **触发条件**：一笔原始损耗为0的生产记录，组长使用有效签名把损耗更正为2，并填写相应损耗原因；或把旧损耗5改为2。
- **预期行为**：更正后，事件、正式生产反馈、物料事实及后续损耗回填使用同一修订后的数据，并保留原值审计。
- **实际逻辑**：`MesProcessPoolProductionReportCorrectionService.correct`经revisionService修改事件rawPayload、管理摘要及输出数量片段；没有更新或版本化对应`MesProFeedbackDO`。完工损耗条件仍直接取旧反馈的unqualifiedQuantity，损耗writer也将新事件明细与旧反馈总量比较。
- **业务影响**：页面可提示修改成功，但完工可能仍按旧损耗计算；原无损耗改为有损耗时，完工草稿还可能继续标记NO_LOSS，后续正式校验再因数量/明细冲突阻断。
- **代码证据**：`service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java:117`；`service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java:107`；`service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java:499`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java:507`（路径前缀见下方说明）。
- **与原14项区别**：003是签名证据合同、004是多笔来源限制；本项是一次业务更正没有同步下游实际消费的数量事实，不是同一根因。
- **建议修复边界**：统一更正生效后的正式数量来源，更新或建立版本化反馈/物料事实与损耗判断字段；不能只修改展示JSON或放宽下游一致性检查。
- **BDD（建议验收，未执行）**：Given 原损耗0，When 组长签名更正为2，Then 详情、正式反馈、完工损耗条件和最终损耗单均为2，原0仍可审计。

## EDHR-STATIC-016：不合格冻结期间仍能更正PQC记录

- **真实入口**：PQC组长工作台“修改PQC表单”→`correct-pqc-inspection`。
- **触发条件**：某PQC提交已发起不合格审查、工单被冻结，尚未最终放行；有该员工管理范围及更正权限的PQC组长修改原检验值或数量。
- **预期行为**：评审冻结期间，原检验事实保持受控；允许的补充材料或专用处置需通过明确授权入口，普通更正入口应先拒绝。
- **实际逻辑**：页面只判断历史/已放行；更正服务只检查管理范围、任务状态和“是否已最终放行”，不检查工单temporaryFrozen、批次FROZEN或待处理不合格评审。随后写入更正签名、覆盖事件、删除重建逐件结果，并可能重新汇集。
- **业务影响**：QA正在评审的原始检验依据可以被改动，出现评审依据与当前检验事实不一致。此处有更正签名和日志，但这不代替冻结前置校验。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:4800`、`:8891`；`service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:85`、`:132`、`:398`；`service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java:113`。
- **与原14项区别**：007针对完工后新增生产提交，010针对报告附件上传；本项是独立PQC更正端点对已存在检验事实的写入，冻结绕过并未被这两个入口修复覆盖。
- **建议修复边界**：更正服务在签名和任何数据更新前，锁定工单并执行统一不合格冻结检查；必要的评审修订必须有单独受控流程。
- **BDD（建议验收，未执行）**：Given PQC提交已进入QA评审冻结，When 原组长更正检测值，Then 明确拒绝，事件、逐件结果、汇集和更正签名零新增/修改；解冻后按规则更正。
- **修复证据（2026-09-14）**：PQC更正服务接入`MesProEdhrNonconformanceReviewService.ensureWorkOrderNotFrozen(task.getWorkOrderId(), "PQC更正")`，检查位于签名、事件修订、正式PQC表更新和汇集之前；定向静态合同与Maven单测通过，证据见`doc/tasks/20260914-edhr-static-016-pqc-correction-freeze/verification-report.md`。

## EDHR-STATIC-017：更正入口接受“检验5件、损耗10件”

- **真实入口**：PQC表单修改弹窗可分别输入检验数量和损耗数量。
- **触发条件**：将实际检验数量设为5、损耗数量设为10，提供5件样本结果、原因与有效密码。
- **预期行为**：与一线首次提交一致，损耗数量不得大于实际检验数量；前后端均应在写入前拒绝。
- **实际逻辑**：弹窗损耗输入仅限制最小0，无最大值；请求构建仅检查整数非负。后端validateCommand同样只检查检验量>0、损耗>=0，没有比较两者，随后保存到任务/事件/PQC记录。
- **业务影响**：修改成功后可存在不可能的检验数量关系，并进入检验失败判定、汇集和损耗展示。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:3831`、`:9005`；`service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:138`；对照首次提交：`service/pro/frontline/MesFrontlinePqcContextServiceImpl.java:1474`。
- **与原14项区别**：008是放行结论判定，013是生产进度；本项是PQC更正输入缺少数量关系约束，首次提交已有该限制。
- **建议修复边界**：复用首次提交的数量约束，在更正VO/服务中强制校验，前端动态限制并再次检查；不以最终放行时拒绝代替入口验证。
- **BDD（建议验收，未执行）**：Given 检验5件，When 更正损耗为10，Then 前后端拒绝且原记录不变；损耗0—5可按其他规则继续。

## EDHR-STATIC-018：备注文字能改变普通订单的正式领料归属

- **真实入口**：工单新增/草稿编辑支持自由文本备注；加入活跃订单后，一线批号查询与完工冻结共用正式领料resolver。
- **触发条件**：普通订单A备注含有合法的`[sourceActiveOrderId=...]`片段，指向同租户订单B的活跃记录；A和B可能使用相同物料但实际批次不同。
- **预期行为**：普通订单A的领料按A的正式订单身份获取。仅明确、受控的模拟复制对象可以使用经验证的来源关联，不能靠自由备注决定来源。
- **实际逻辑**：resolver无条件解析workOrder.remark中的标记，并把生产订单编号替换为B的编号；只核对来源存在及租户，没有验证目标订单为模拟复制对象或正式来源关联。普通备注因此改变正式领料查询和完工冻结结果。
- **业务影响**：A可能展示并固化B的领料单和物料批号，形成错误批次追溯；或因B来源问题阻断本来正确的A。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/workorder/WorkOrderForm.vue:132`；`controller/admin/pro/workorder/vo/MesProWorkOrderSaveReqVO.java:76`；`service/pro/processpool/team/MesFormalProductionPickListSourceResolver.java:67`；`service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceService.java:47`；`service/pro/feedback/frontline/MesProFeedbackMaterialBatchQueryServiceImpl.java:41`。
- **与原14项区别**：原14项没有涉及普通订单备注驱动的跨订单领料来源重定向；不是路线版本绑定或分配数量问题。
- **建议修复边界**：使用显式、不可由自由备注替代的复制来源字段/关系，并验证目标对象确属模拟及其归属；正式订单始终按自身订单编号查询，不能给所有订单保留备注驱动的替代来源。
- **BDD（建议验收，未执行）**：Given 普通订单A备注中出现上述文字，When 查询/冻结领料，Then 不得把B来源绑定到A；合法模拟复制通过专用来源关系验证。

## EDHR-STATIC-019：可自行填写签名编号和修改人，服务端未验真

- **真实入口**：路由`pro/process-pool/event-revision`的“原始记录修改”页面→`update-original`；页面直接提供修改人ID、签名ID、签名人ID和签名快照JSON输入。
- **触发条件**：具备原始记录修订权限的账户，针对满足“最新复核已拒绝”等前置条件的记录，提交一个未使用的任意签名ID及相同的自填修改人/签名人ID。
- **预期行为**：修改人取当前认证账户；重新签名必须验证真实签名凭据属于该人、该事件和该变更内容，不能仅接受前端声称的编号与JSON。
- **实际逻辑**：Controller直接reqVO.toBO；VO原样带入modifiedByUserId、revisionSignatureUserId和签名快照。服务只检查两个自填人员ID相等、签名ID没有被事件/修订表用过，没有查询验证该签名实际存在、密码验证结果、签名对象或当前登录人匹配。
- **业务影响**：可把修订审计记到另一人员名下，并让不存在的签名成为“重新签名”证据。此结论以已有修订权限为前提，不是声称无权限用户可任意调用。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/EventRevisionPage.vue:28`；`controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java:51`；`controller/admin/pro/processpool/vo/ProcessPoolEventRevisionUpdateReqVO.java:56`；`service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java:133`、`:167`、`:84`。
- **与原14项区别**：009是QA不合格处置自由文本签名，本项是另一个仍公开的原始记录修订入口，正常correct-*端点的服务端签名不会自动覆盖它。
- **建议修复边界**：当前账户身份由服务端注入；统一签名服务在本次修改事务验证或生成与实际payload绑定的证据，同时验证记录管理范围；不只校验“签名ID未重复”。
- **BDD（建议验收，未执行）**：Given 用户A具备修订权限，When 提交自填用户B或不存在的签名编号，Then 零写入拒绝；本人完成正式签名才可修订并记录真实A。

## EDHR-STATIC-020：正常有损耗报工被数量公式矛盾阻断

- **真实入口**：一线按输出物料填写合格完成数量及损耗→组长确认分配→PQC生产放行损耗资料计划。
- **触发条件**：本次合格完成100、损耗3，分配给本订单100件；即使人员、签名、物料、表单等其他资料完整。
- **预期行为**：正式报工总量103、合格分配100、损耗3应满足同一生产事实；损耗不能当作合格产出分配，但也不能因此阻断放行。
- **实际逻辑**：提交服务applyFormalFeedbackAggregate把feedbackQuantity设为progressQuantity+lossQuantity（103），qualifiedQuantity为100，unqualifiedQuantity为3。初始分配及报工池使用progressQuantity（100）。损耗writer却要求feedbackQuantity==allocation.allocatedQuantity，也就是103==100，失败即停止。
- **业务影响**：正常有损耗的生产记录在该放行资料检查中被误判为总量与分配不一致；试图分配103又会超过100的报工池，不能通过正确业务操作消除。
- **代码证据**：`service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:171`、`:586`；`service/pro/processpool/team/MesReportAllocationCommandService.java:280`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossReportWriterImpl.java:449`。
- **与原14项区别**：004是同工序多笔限制，013是多物料/部分分配进度；本项一笔、一个物料、全量合格分配即可触发，是总量/合格量/损耗量定义不一致。
- **建议修复边界**：统一字段业务含义，分别核对报工总量=合格量+损耗、订单分配对应的合格量与损耗归属；不能把损耗加入合格分配来通过校验。
- **BDD（建议验收，未执行）**：Given 合格100损耗3且正式分配100，When 完整资料申请生产放行，Then 数量一致性检查通过，批记录分别体现100和3；无损耗及多笔有损耗场景同口径。

## EDHR-STATIC-021：第四份资料无法衔接最终放行，所需库存来源没有生成链路

- **真实入口**：批次详情完成第四个生产放行报告节点→`task/special-node/complete`→`MesProductionReleaseReportServiceImpl.complete`→初始化最终放行阶段。
- **触发条件**：新建正常活跃订单，已具备正式领料/批号，生产、检验、批记录填写及其他前置均满足；未通过历史导入或库外手段预填该订单的调拨追溯表。
- **预期行为**：当前正式领料及库存来源应能通过受支持的业务入口产生放行检查需要的证据；齐套后可生成最终放行待办。未发生的业务不能靠伪造记录凑齐。
- **实际逻辑**：完工领料来源由`MesTeamLeaderActiveOrderPickListCompletionSourceService`解析并绑定正式领料单；库存检查却只查询`mes_pro_process_pool_active_order_transfer_trace`，并强制要求TRANSFER、SHIPMENT、BATCH_TRACE三种来源。全后端生产源码及SQL/XML搜索只找到调拨trace服务写入，两个写方法没有外部生产调用；其转换器也只生成TRANSFER。Controller仅提供GET查询，未发现SHIPMENT/BATCH_TRACE生成实现。
- **业务影响**：即使生产及四份附件本身正确，第四节点完成仍因库存readiness阻塞而抛错；同一事务内的节点完成及交接回滚，不能建立最终放行待办。实际上传的文件是否已存储不等于报告节点已完成。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue:5605`；`service/pro/productionrelease/report/MesProductionReleaseReportServiceImpl.java:129`、`:175`；`service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java:120`；`service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessService.java:67`；`service/pro/batchrecord/MesOrderReleaseCompletenessServiceImpl.java:64`、`:240`、`:267`；`service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceService.java:47`；`service/pro/processpool/team/MesActiveOrderTransferTraceServiceImpl.java:53`、`:78`、`:171`；`controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:570`。
- **与既有问题区别**：008是报告齐套冒充检查PASS及修复后的检验判定口径，本项是库存正式证据的生产与消费链路断开；020是有损耗数量冲突，本项无损耗订单也会触发。
- **建议修复边界**：先明确上市放行前应具备哪些库存证据，使正式领料/补料/批次来源经实际业务链产出并被检查消费；若确需调拨或发货，须有该阶段可达的生成、绑定和核验入口。不能删除全部检查、硬编码PASS或要求直接补数据库。
- **BDD（建议验收，未执行）**：Given 正常无损耗订单且其他前置齐全，When 经真实页面完成生产、检验、放行及四份资料，Then 系统从正式来源完成库存核验并建立最终放行待办，不需人工造trace数据。

## EDHR-STATIC-022：多物料或多批次的正常库存明细被判为重复

- **真实入口**：与021相同，第四报告节点完成及最终放行检查调用`evaluateInventoryConsistency`。
- **触发条件**：一个活跃订单存在两条不同正式调拨明细，例如物料A与B，或同物料两个批次；各自明细ID及幂等键不同。为隔离本项，其他必备类型和检查前置已齐备。
- **预期行为**：不同来源单据/明细可同属TRANSFER；应按真实来源身份判断重复，并完整核对所有明细。
- **实际逻辑**：trace生成器按每个`MesWmTransferDetailDO`写一行，保存transferLineId、transferDetailId和独立幂等键；消费方只按sourceType分组，任一类型行数大于1即返回“存在重复库存追溯来源”，未比较单据、明细、物料或批次身份。
- **业务影响**：合法多物料、多批次或多次调拨订单会被阻止进入最终放行。删掉其中一条虽可能避开计数，却会丢失正式追溯证据。
- **代码证据**：`service/pro/processpool/team/MesActiveOrderTransferTraceServiceImpl.java:127`、`:171`、`:173`、`:185`；`service/pro/batchrecord/MesOrderReleaseCompletenessServiceImpl.java:252`。
- **与既有问题区别**：021是缺生成链路；本项在来源已经齐全时仍必定误拒绝，补齐来源并不能修好该分组规则。与004损耗记录去重、013产出进度计算也不是同一对象或判定。
- **建议修复边界**：重复识别使用正式来源的稳定复合身份；同类型不同合法明细全部保留，数量和批次分别核对，不能把同类只留一条当作修复。
- **BDD（建议验收，未执行）**：Given 同订单两条不同调拨明细且其他证据齐全，When 执行库存核验，Then 两条均纳入核验并可通过；同一来源明细真实重复时应阻断。
- **证据边界**：这是持久化明细合同与消费算法的确定冲突。021在全新正常订单上可能先阻断，本轮没有声称已运行到第二个错误。

## EDHR-STATIC-023：报工分给另一订单时，完成量计算找不到合法来源

- **真实入口**：生产组长工作台“新增分配行”选择另一活跃订单，签名确认→`submission/allocation/confirm`→`MesReportAllocationCommandService.save`。
- **触发条件**：同一组长有A、B两个正常活跃订单，使用同一路线、工序和输出物料；A提交报工100，组长合法分配给B100，B目标100且该报工未由B重复提交。
- **预期行为**：确认分配后，B应从正式分配关系读取A的原始报工，按分配量计入B进度并保留原始来源身份。
- **实际逻辑**：前端可选择其他订单；后端按目标活跃订单验证工序及数量，并把allocation.workOrderId写为B。但同一事务随即调用`reconcileAffectedAllocations`，按B的workOrderId查询原始生产事件，查不到仍属于A的event。新计算器发现分配eventId未被查回，抛出`PRODUCTION_EVENT_FOR_CURRENT_ALLOCATION`。即使补查event，计算器自身仍要求event.workOrderId等于B。
- **业务影响**：跨订单分配确认整体回滚，目标订单不能取得应有进度，无法通过共享报工正常完成订单。这里不是已保存分配后“仅页面少显示”，错误发生在确认事务内。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:4768`、`:6883`、`:8686`；`controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:608`；`service/pro/processpool/team/MesReportAllocationCommandService.java:229`、`:278`、`:347`、`:361`、`:483`；`service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionService.java:115`、`:138`；`dal/mysql/pro/processpool/MesProProcessPoolEventMapper.java:159`；`service/pro/processpool/team/MesOutputMaterialProgressCalculator.java:65`、`:133`。
- **与既有问题区别**：013是同一来源已经查到后忽略分配量导致多算；本项是合法跨订单来源根本查不到并被错误身份约束拒绝。修正013的数量公式不能修好此入口。
- **建议修复边界**：从目标订单的有效分配集合回查原始event，分别核验来源身份和目标身份，再按分配量计算。不要改写原始报工所属订单或隐藏已有共享分配入口来掩盖衔接错误。
- **BDD（建议验收，未执行）**：Given A报工100且B同工序目标100，When 组长签名把100分给B，Then 确认成功、B进度100且可完工，来源仍追溯到A；再覆盖A40/B60拆分及重新分配。

## EDHR-STATIC-024：动态过程检验回填成功后仍被判为没有正式证据

- **真实入口**：路线设计器配置已发布的PROCESS_INSPECTION动态表单；PQC组长在工作待办批准生产放行，触发三类正式记录写入及回执核验。
- **触发条件**：过程检验使用正式动态表单且模板、映射、来源等前置满足；动态writer成功返回EFFECTIVE实例及字段审计快照，传统批记录和损耗分支也满足各自合同。
- **预期行为**：放行汇总应识别正式动态表单证据，核对实例和审计快照后进入四份资料阶段；不同证据类型须保留各自身份。
- **实际逻辑**：过程检验writer的动态分支只把编号加入formCenterInstanceIds，并continue，不生成batchRecordExecutionIds。其结果类明确区分两类编号；但`MesPqcReleaseDossierPortImpl.requireFormalWrite`仍要求inspectionWrite.batchRecordExecutionIds非空，且构造processInspectionEvidenceIds时也只复制该旧字段，完全不读取formCenterInstanceIds。
- **业务影响**：全部过程检验采用动态表单时，即使动态回填成功，PQC批准仍报缺少批记录/过程检验证据并回滚，四类报告待办不能生成。混合传统和动态目标时也存在动态实例未进入该放行回执的缺口。
- **代码证据**：`IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue:1111`、`:1202`；`IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue:1387`；`service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:109`、`:155`、`:157`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java:187`、`:197`、`:200`、`:232`、`:233`；`service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java:290`、`:433`。
- **与既有问题区别**：005是QA项目/规程版本校验；本项是已成功生成的另一类正式证据没有接入上层回执。021/022发生在第四报告后的库存检查，本项在PQC批准时已阻断。
- **建议修复边界**：贯通动态实例、传统执行记录及审计证据的类型化回执、校验和后续追溯；不得把FormCenter实例ID冒充传统executionId，也不能只删除空集合检查。
- **BDD（建议验收，未执行）**：Given 过程检验全部绑定动态表单且来源齐全，When PQC批准生产放行，Then 正式实例和审计证据被接收并生成四类报告待办；纯传统、纯动态及混合场景分别核验。

## EDHR-STATIC-025：多项检验共用一张动态表单时，第一项写完即锁定

- **真实入口**：同024，PQC批准生产放行后的过程检验回填；适用同生产工序多个正式PQC任务共用一个PROCESS_INSPECTION动态表单绑定。
- **触发条件**：同工序有两个不同检验任务，例如两个检验项目；两项来源、映射均通过plan，首项可满足模板必填字段。该工序只有一个正式过程检验表单目标。
- **预期行为**：全部检验项目应先合并成完整表单内容和来源证据，再提交生效；或者按明确业务身份形成各自独立记录，不能中途锁死未完成记录。
- **实际逻辑**：reader按每个PQC任务构造source，writer也逐任务循环；`requireCurrentBatchTask`按同一工序和binding选择同一个batchTask及FormCenter实例。动态port处理第一项时立即submit并要求EFFECTIVE。第二项再次进入同一实例时只能走validateReplay；auditHeadHash包含每项独立的evidenceHash，而evidenceHash包括PQC任务ID和该项来源，故不同任务不是同一次写入的重放，返回“已生效过程检验FormCenter instance与本次正式来源不一致”。
- **业务影响**：正常同工序多项目检验无法完成PQC生产放行，事务回滚；即使024已经接上动态证据回执，这里的第二项写入仍会更早阻断。
- **代码证据**：`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionReaderImpl.java:76`、`:83`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionWriterImpl.java:126`、`:156`、`:183`、`:185`、`:907`、`:916`、`:918`、`:874`、`:1087`、`:1216`；`service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImpl.java:146`、`:165`、`:168`、`:170`、`:257`、`:260`、`:339`。
- **与既有问题区别**：024是上层没有接收动态证据；本项是底层按项目写入和按表单生效的粒度冲突。两个问题须分别修复；不是“缺一份报告”或“填写人未签名”。
- **建议修复边界**：以实际目标实例组织完整检验内容、来源和审计，再统一写入及生效；不能循环解锁、覆盖首项或放宽重放一致性校验。
- **BDD（建议验收，未执行）**：Given 同工序两项检验已确认且映射完整，When PQC批准生产放行，Then 同一过程检验记录包含两项来源，完整提交后生效；重试不重复写入，不丢首项，不在第二项报来源不一致。
- **证据边界**：为隔离025，场景要求首项必填校验可通过；未将缺少模板配置产生的提前报错混入本项。没有执行真实页面复现。

## 第三轮主流程优先级补充（2026-09-14）

- 新增023—025全部为P1主流程阻断，仍待修复；只新增证据明确的衔接问题。
- 采用动态过程检验表单的订单，先处理024、025，确保PQC批准可进入四份资料阶段；需要跨订单分配的订单，同时处理023。
- 新增问题须与既有损耗、库存、检验判定、归档身份及进度问题一并安排整链验收；旧项修复状态以其最新复核为准，本轮不重新编号或冒充再次全量复核。
- 审计期间进度计算器出现并行改动，已改为按分配量限制物料计入值；重新读取后确认按目标工单查询来源及强制原始workOrderId相等的两处逻辑仍在，023结论不受该数量改动影响。
- 第三轮排除映射scope名称疑点：`ROUTE_VERSION`在现有cell-link Scope生产方实际使用批记录versionId，与backfill读取相同，不能仅凭字段名称判错。
- 本轮仅静态阅读与文档核验；完整运行验收仍须从真实页面完成订单到归档追溯。

## 第二轮主流程优先级及原问题补充（历史）

| 顺序 | 主流程环节 | 需要解决的衔接 | 对应记录 |
|---|---|---|---|
| 1 | 生产达到100%→完成订单 | 进度只能计本订单实际分配量；有损耗时总量、合格量和损耗须能核对 | 原013、020 |
| 2 | 检验回填及放行复核 | 生产端与消费端使用相同报废/逐件判定口径，合法QA处置可继续 | 原008 |
| 3 | 自动回填→批记录有效填写 | 当前代码还要求完成普通批记录填写及提交签名，不能只上传四份文件 | 现存人工步骤，未登记缺陷 |
| 4 | 第四份资料→最终放行待办 | 正式库存证据须有生成入口，正常多明细不能被拒绝 | 021、022 |
| 5 | 最终放行→归档追溯 | 批次创建及归档都必须使用同一冻结路线身份 | 原012补充 |

- **原012补充场景**：订单冻结路线V1旧名之后、PQC批准创建批次之前，发布路线V2并改名。`MesProRouteVersionPublishProjectionServiceImpl.java:394`会更新现行路线名称；`MesProEdhrBatchExecutionServiceImpl.java:1165`、`:1186`读取V1快照，却在`:1187`、`:1188`写入现行路线编码/名称。归档`:8052`、`:8064`要求批次身份与V1快照一致，因此该批次天生无法满足归档条件。
- **原012结论**：归档不再直接读现行路线这一局部修复成立，但上游批次创建尚未统一冻结身份；按“旧订单能够走完归档”的主流程合同重新打开012，不增加新编号。验收需覆盖“冻结订单后、创建批次前改名发布”，不能只构造一个已经正确保存旧名的批次。
- **普通表单步骤排除误报**：回填仅保存字段审计值，不代表完成填写。`MesProEdhrBatchExecutionServiceImpl.java:1211`会创建初始填写待办；`MesProEdhrWorkTaskServiceImpl.java:782`及`:1031`提供填写后推进路径；`MesProductionReleaseBusinessReadinessService.java:130`检查有效提交证据。配置齐全且签署完成时有正常路径，本轮未把缺少自动提交当成代码缺陷。
- **最小后续验收顺序（未执行）**：先走单订单、单物料、无损耗、检验全合格到归档；再走多物料/多批次、有损耗及QA处置；最后走订单冻结后路线升版。每条都需真实页面完成至历史追溯，静态审计不代替走通验收。

## 路径说明

上述以`service/`、`controller/`开头的Java路径，均相对于：

`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/`

前端路径和任务路径均相对于当前IntRuoyi仓库根目录。行号以本次静态检查为准，后续代码变化后需要重新核对。

## 排除与未扩大范围

- 活跃订单移除已调用requireNoReleaseApplication，未登记“放行中直接移除”猜测。
- 管理者最终签名、四报告冻结、归档任务责任人和状态门禁均有实际检查；未因没运行E2E就认定这些入口错误。
- PQC记录没有独立inspectionQuantity列，数量保存在任务/JSON中，排除“更正漏改不存在数量列”的怀疑。
- 既有008/013和其他14项问题不重复登记；相关仍待修复状态继续以原记录及独立复核报告为准。
- 损耗writer对多个损耗原因的后续校验也应在修复020时覆盖，但本轮不把被020前置阻断覆盖的后续风险另计为一个已运行可达问题。

## EDHR-STATIC-026：条件不适用表单的填写及进度未闭环

此前第四轮已登记026，本次开始时列表和正文缺失，现恢复。最终readiness已按HAS_ACTUAL_LOSS过滤无损耗，但建批requiredFlag、填写同工序门禁和进度仍未统一；同工序含无损耗条件表时仍可能阻断后续填写。详见本次复核报告026章节及源码证据。部分修复，不算新增编号。

## 2026-09-14 全部编号独立复核（修复前）

- 按当前代码核对001—026：22项原缺陷修复点成立；020仅旧公式修复，最新补料单依据未实现；008、026部分修复；023仍未修复。
- 仍需闭环：008、020、023、026；另需接入无补料信息弹框确认及后端确认事实。
- 历史修复自述和结论保留，以当前索引及 `doc/tasks/20260914-edhr-bug-list-recheck/verification-report.md` 为本次结论。仅静态代码复核，未修改业务代码或运行业务测试/E2E。

## 2026-09-14 主流程修复与回归（第一轮）

- 在 `int_main` 修复剩余008、020、023、026及无补料确认链路；定向后端回归210项全部通过，前端类型检查和相关合同通过。
- 008：报废数量大于零时检验判定一致；缺少正式报废数量或整数溢出明确阻断。
- 020：正式损耗读取已审核生产补料单的实补数量；保留全部补料单号、分录、物料和批号。现场过程损耗保留作追溯，不再作为正式损耗数值及原因依据。
- 无补料：完成时未查到补料单先返回确认要求；生产组长确认后允许完工，保存无正式损耗及确认人。取消不完工，重试沿用已保存确认；未审核、无效数量和匹配不唯一的单据不能被确认成无补料。
- 023：分配确认时按分配记录读取来源事件，按目标订单分配数量计算进度，保留来源工单身份。
- 026：逐路线工序的正式损耗条件随完工回执传入建批；不适用损耗表不创建填写实例/待办，不阻断前后工序，不计入必填进度，最终检查使用一致条件。
- 扩展建批测试185项中有9项既有失败；用修改前服务代码隔离复跑仍是相同9项，未将其记为PASS或新业务缺陷。本次未执行真实订单E2E，不能据此宣称任意订单已经实测跑通。
- 正式证据：`doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md`。改动尚未Git提交/推送。

## 2026-09-14 后续调用链复核与修复（最新）

- 上轮023仅修复分配确认阶段，未覆盖列表、完工及资料读取，不能据210项测试直接关闭。本轮读取器均从目标订单当前分配回查原始事件，保留来源工单；批记录与损耗writer按来源事件验证反馈/签名，按目标分配验证数量归属。重新分配后最后事件也必须仍属于剩余来源。
- 023代码边界：`MesTeamLeaderActiveOrderServiceImpl.loadActiveOrderProgress`、`MesTeamLeaderActiveOrderCompletionProgressPortImpl.read`、`MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.read/validFormalJoin`、`MesPqcReleaseDossierPortImpl.loadBatchRecordSources`、`MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.validEventContext`、`MesTeamLeaderOrderProcessCompletionService.reconcileAffectedAllocations`。
- 验证：本轮133项定向回归与前端类型检查通过。活跃订单服务扩展测试有30项原有失败，隔离运行修改前服务复现相同30项；新跨订单列表测试仅旧代码失败。

## EDHR-STATIC-027：动态损耗表证据未贯通PQC放行

- 触发条件：本批存在正式补料损耗，LOSS_REPORT使用动态FormCenter表单，PQC批准生产放行。
- 原因：损耗writer正确返回动态实例编号和字段审计，但资料汇总及PQC服务两层仍要求传统损耗execution编号非空；只修任一层仍会被下一层阻断。原024只覆盖过程检验，不覆盖此分支。
- 影响：动态损耗已生效仍无法通过PQC批准，四份报告上传待办不能继续。
- 修复：新增独立的`lossReportFormCenterInstanceIds`、`lossReportFieldAuditIds`及`lossReportFieldAuditHeadHashes`，贯穿资料回执、PQC校验、持久化决定、幂等回放和API。动态/传统损耗可分别或混合存在，不混用ID；无损耗状态不得携带动态损耗实例，正损耗必须有有效证据。
- 代码边界：`MesPqcReleaseDossierPortImpl.write/requireFormalWrite`、`MesPqcProductionReleaseServiceImpl.approve/requireDossierWrite`及对应DTO/Controller。
- BDD验收：Given正式补料和有效动态损耗表，When调用真实资料汇总及PQC批准服务，Then进入REPORT_UPLOAD_PENDING并保存四个上传任务回执；再次请求返回同一动态实例/审计证据。混合证据均保留，缺审计被拒绝。
- 证据：`doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md`的“Follow-up”章节；未执行真实页面E2E，未提交或推送Git。

## 2026-09-15 普通订单主流程继续修复

### EDHR-STATIC-028：领料证据被提前到生产录入阶段

- 触发：生产、PQC开始时ERP领料单尚未同步，符合用户明确允许的正常顺序。
- 原因：`MesFrontlineProcessMaterialServiceImpl.listFrozenMaterials`在加载输入物料时调用正式领料批号查询；无单或无批号直接阻断。签名提交把未知数量写为null后还会触发不可变Map拒绝null。
- 修复边界：生产阶段只展示冻结物料配置，签名原始记录明确标记PENDING_COMPLETION；正式领料仍在完工事务发现、校验并保存。详情读取完工时保存的批号、数量及全部匹配单号，不重写已签名的原始生产记录。
- 验证：生产无领料加载/提交的RED已复现；相关103项定向回归PASS。完工详情双来源批号测试RED复现后，快照读取及完工/回填28项回归PASS。

### EDHR-STATIC-029：自动回填依赖人工待办

- 触发：PQC生产放行自动填写多个工序的动态过程检验/损耗表；下游工序尚未生成FILL待办，或首工序损耗表负责人与PQC放行人不同。
- 原因：自动writer走公开人工submit，效果执行器调用`completeRouteFormFillAndCreateNextFill`，强制存在人工FILL待办并按其候选人判断。
- 修复：内部可信提交上下文仅供服务调用；校验DIRECT策略、检验/损耗槽位、当前PQC放行申请、批次/路线/版本/租户身份及冻结PQC候选人。自动回填可以完成尚无人工待办的证据；已存在待办记为AUTOMATIC_BACKFILL，并在同一事务保存操作者和证据摘要审计。
- 后续推进：自动完成的检验待办退出人工推进人的判断，生产负责人提交主记录可派发下一工序；人工检验模式的人员限制保留。
- 证据：双用例RED复现原自动待办问题；工序推进参数化用例只有自动场景RED。33项BPM及连接服务回归、24项工序推进回归PASS。没有执行真实页面E2E。

### EDHR-STATIC-030：人工提交错误地采用表单创建人

- 触发：动态路线表单由建批/PQC操作人创建，后续由配置的实际填写人提交。
- 原因：效果执行器使用`instance.applicantUserId`作为完成人，真实请求用户没有进入效果调用。
- 修复：公开submit将真实用户写入仅服务端创建的执行上下文；效果执行器用该用户做人工待办校验，保留原创建人历史事实。前端formData中的automaticBackfill/执行上下文字段不能获得内部自动权限。
- 证据：真实提交人77/创建人99的RED复现；公开表单不可伪造自动权限、正确演员传递及实际人工权限拒绝测试PASS。

- 核心任务记录：`doc/tasks/20260914-edhr-main-flow-fixes/`。整体目标仍为in_progress，以上定向通过不能替代从订单加入到历史追溯的完整核查。

### EDHR-STATIC-031：放行报告与普通批记录相互等待

- 原因：`createNextFillAfterBatchTask`将特殊报告节点当作下一项人工FILL候选，`hasUnsatisfiedSpecialBeforeNext`又把独立RELEASE_REPORT_NODE上传待办当作普通工序前置条件。报告最后一项提交要检查全部普通记录，正式报告完成接口也没有旧特殊节点恢复回调。
- 修复：下一项人工填写只选ROUTE_FORM；已有正式RELEASE_REPORT_NODE任务的报告不阻断普通记录派发。其他模式的特殊节点顺序限制保留；最终放行仍检查报告及全部普通记录。
- 证据：数据库回归复现“预期1条下一工序待办，实际0条”；修复后含前后工序、报告上传、PQC批准、管理者放行的87项回归PASS。

### EDHR-STATIC-032：物料配置存在两个不一致读取口径

- 页面正常输入输出物料编辑保存到`batchUseConfigs`，一线物料服务也读此字段。生产参数配置的前端类型和默认JSON只包含超量比例、损耗、设备和参数。
- 活跃订单冻结与生产配置校验却要求`productionProcessConfigs.inputMaterialIds/outputMaterialIds`，导致用户用正常物料控件配置后仍需重复编辑隐藏JSON，或订单进度使用另一套输出物料。
- 修复：订单从同一正式版本、同一routeProcessId的batchUseConfigs冻结输入输出物料；生产配置独立校验设备、损耗和参数，不再要求重复物料字段。缺少正式物料配置或输出集合仍阻断加入。
- 验证：正常页面形状的快照、高级生产JSON不重复材料时，旧代码在validateOutputMaterialIds处RED；包含正常无领料单加入、路线配置、物料加载及进度计算的30项定向回归PASS。

## 2026-09-15 本轮最终代码验证

028—032已完成修复及对应定向回归。补充发布/最终放行70项、来源/QA发布42项、普通活跃订单最终放行1项通过；生产/PQC/复核/完工127项和放行阶段87项通过（测试集合有重叠，不累加为总数）。当前审查范围内未发现仍待修复的普通主流程代码卡点。

结论为代码分析与定向回归通过，未执行真实订单浏览器E2E。正式任务完成仍待当前规则要求的Git集成授权；实现和验证证据见 `doc/tasks/20260914-edhr-main-flow-fixes/verification-report.md` 最终章节。
