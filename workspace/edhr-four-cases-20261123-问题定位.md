# 四订单14项记录：只读定位结果（2026-11-23）

1、范围与结论

依据协调汇总、生产/PQC/QA岗位回执、生产记录截图09，以及当前工作区源码做静态定位。没有重跑E2E，没有调用业务接口、读写数据库、修改代码、重启服务或重复签署。当前源码与当时运行包是否完全一致尚未核对，因此“代码可解释现象”不等于“运行根因均已复现”。以下保留原P01—P14编号。

2、P14：上市签名详情异常——已确认前后端动作契约缺口

业务链：上市批准生成审批中心签名 → 活跃订单签名证据服务识别BPM签名并核对正式放行交易/任务/回执 → 前端签名详情渲染动作中文名。后端合法支持审批中心APPROVE动作，前端字典只有MARKET_RELEASE等MES动作，遇APPROVE直接throw，解释原始报错及详情无法完成渲染。不能通过改写原签名动作解决。

代码：[前端动作字典及异常](../IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue#L2355)；同文件2095行在模板中调用。后端`MesActiveOrderSignatureEvidenceService.java`166—168行进入BPM分支，458行起核对放行证据，505行核对APPROVE回执。修复方向：按明确业务上下文支持审批中心合法动作，同时保留未知动作报错。

3、P13：更正后总不合格、下游汇总通过——已确认计算口径分裂

检验提交和正式更正后端均规定：损耗scrapQuantity＞0即总判FAILURE，即使所有逐件项目通过。正常单更正后保留损耗1，因此总不合格有代码依据，并非仅凭“旧缓存”可以解释。下游过程检验汇总却只汇总processInspectionItems的项目判定，未纳入总inspectionResult及损耗条件，故能显示通过。该项优先级高于一般展示瑕疵。

代码：`MesFrontlinePqcContextServiceImpl.java:1945`、`MesProcessPoolPqcInspectionCorrectionService.java:973`；`ActiveOrderSubmissionDetailPanel.vue:3532`项目判定汇总、3715—3730行总体汇总。PQC补充回执明确：05:19:12复核后总不合格仍在。修复前须明确“检测用消耗是否应使总判不合格”；不能仅把总判改成通过，也不能用复核正确替代质量合格。未据此判定后端放行校验必然失效，仍需专门核对放行准入链。

4、P10：负责人详情缺总判定——与P13同类展示模型缺口

负责人/NCR复用活跃订单详情组件，其过程检验记录显示逐项目判定和项目汇总结论。该结论不能表达“样本全通过、总损耗1、正式总判不合格”的状态。需在负责人可见详情明确展示正式总判与项目判定、消耗/报废原因，避免审批人把项目通过误认为整条检验通过。不能因该页缺总判就认定数据库未保存。

依据：`ActiveOrderSubmissionDetailPanel.vue:740—776`、`3715—3730`；`MesTeamLeaderActiveOrderDetailServiceImpl.java:1642—1748`构建submittedItems/processInspectionItems。当前已定位显示链；尚未核对当时完整接口响应。

5、P03：损耗明细1、生产表损耗0——已找到明确置零路径，记录级触发仍待核对

截图09的首笔粗洗记录输出物料列为“未记录”，损耗列0。前端buildProductionRecordRows在无materials时直接写lossQuantity:0；有materials但lossQuantity缺失时也归零。另一个损耗汇总独立读取submission.lossDetails，因此出现“明细1、表格0”有直接代码解释。不能将0解释成真实无损耗。

代码：`ActiveOrderSubmissionDetailPanel.vue:2861`独立汇总lossDetails、3081—3122行生产表构造、3138—3143行合计；后端`MesTeamLeaderActiveOrderDetailServiceImpl.java:1954—1983`从原payload.materialDetails读取物料损耗，缺数组返回空。需核对事件327089的正式响应和原payload，确认是无物料、缺字段还是分配投影所致；本轮未查库，不把具体丢字段环节写死。

6、P01：SOP读取失败——已定位冻结快照缺字段的拒绝分支

业务链：一线生产选工序 → process-sop接口 → 校验当前人员可访问活跃订单及锁定路线/版本 → 解析冻结工序节点 → 缺sopFileUrl键即报原错误。缺字段与字段为null（明确未绑定）是两种状态。错误发生在文件下载前，不能认定文件服务故障。

代码：`FrontlineFixedTemplatePanel.vue:2367`请求；`MesFrontlineDeviceAccountController.java:142—151`；`MesFrontlineActiveOrderProcessServiceImpl.java:115—136`。当前`MesProRouteVersionSnapshotIdentityWriter.java:21`已显式保留null的sopFileUrl，但这不证明历史冻结快照已补齐。需核对订单实际冻结版本、flowGraph节点及生成该版本的发布路径；不得临时读当前工艺文件替代冻结依据。是否须阻断生产提交需要独立业务要求，不能自行增加。

7、P02：78/72未显示数量超限——现有记录不足以确认为缺陷

代码累计上限并非固定72，而是ERP冻结数量×冻结流程损耗系数；超过上限才设置quantityConflict。若该周期系数1.1，72×1.1＝79.2，78尚未超限，未显示标识符合当前规则。原记录也写明采用100%～110%解释，却把108.33%作为超限显示验收，断言存在冲突。

代码：`MesProductionQuantityRange.java:10—20`；`MesTeamLeaderActiveOrderServiceImpl.java:3675—3695`计算overage并投影，3901—3905行输出标志；`TeamLeaderWorkbenchPage.vue:1789`按标志显示。工艺维护页`RouteLossCoefficient.vue:9—10`有同一口径说明。必须先只读核对该周期实际冻结系数；若需求是“超过ERP数就提示”，这是另一项明确的业务规则变更，不能默认当作现实现错误。

8、P09：带申请任务上下文切已处理页报唯一匹配失败——已确认筛选与任务定位冲突

页面从待办保留applicationId/workTaskId，切页签时仍将这两个定位条件连同viewStatus发给后端。申请仍在评审中、不属于已处理集合时自然可能返回空；前端却对带applicationId的任何查询均要求唯一一条，抛出原错误。另外自动进入待放行流程对underReview行也直接拒绝，解释首次任务状态提示。

代码：`PqcProductionReleasePage.vue:369—384`查询/强制唯一；549—559行自动待放行状态判定；`MesPqcProductionReleaseServiceImpl.java:457—481`按viewStatus和申请/任务身份共同查询。修复方向：明确任务定位与普通列表浏览的交互边界，空集合可否作为正常结果必须按当前页签语义处理，不能放松真实签署前的身份唯一性。

9、P06：发起NCR后仍有复核/审查按钮——已确认前端状态条件不足

canReviewSubmission只检查只读、历史页、released、复核状态、聚合状态和事件ID；canOpenPqcSubmissionNonconformanceReview只再检查activeOrderId，没有检查NCR进行中/冻结。因而原记录仍未普通复核时按钮继续显示可由代码解释。后端正式复核在创建签名前检查NCR冻结，不可据按钮存在推断已绕过冻结或可重复写入。

代码：`TeamLeaderWorkbenchPage.vue:5087—5097`及1370/1390行按钮；`MesTeamLeaderSubmissionReviewServiceImpl.java:118—125`冻结校验。修复应同步真实评审/冻结状态到行操作能力；重复NCR提交的后端幂等/唯一性本轮未实测。

10、P05：NCR详情与工单404——需拆成两项

NCR列表“详情”明确调用openActiveOrderDetail并复用批记录式活跃订单详情，不是点击后意外路由到了其他页面；正式评审处理另有“处理”入口，受权限及责任人条件限制。因此缺QA责任人/评审任务信息属于详情能力不足，不能仅凭外观认定NCR创建失败。

代码：`NonconformanceReviewPage.vue:132`、283—301行及681行。工单404则走共享`WorkOrderCodeLink.vue:16`固定路径`/mes/pro/work-order`；SQL中存在此菜单，页面仍404需核对吴学燕当时菜单授权及动态路由注册，尚不能认定路径拼错。不应为消除404擅自扩岗授权。

11、P07：交接上下文/返工旧入口失效——部分属于预期防护，缓存恢复问题待复现

生产前端明确拦截未通过原任务/周期校验的交接；返工旧周期不能继续操作新周期也有前后端双重防护。因此“旧入口失效”本身不能当成缺陷。刷新正常菜单后恢复的缓存现象，需保留当时URL、当前task/round/activeOrderId和失效原因才能判定是否为错误残留。

代码：`FrontlineFixedTemplatePanel.vue:1961`、5960、6074；`NonconformanceReviewPage.vue:963`；`TeamLeaderWorkbenchPage.vue:10888`；`MesActiveOrderHandoffService.java:607`。尚未确认缓存未清理的具体缺陷，不建议放松周期身份检查。

12、P08：交接通知回执空——当前代码存在明确“不投递”规则，不能直接判通知故障

通知服务对QA_REVIEW、QA_DECISION_HANDOFF、NCR通知、生产/PQC退回等已停用站内信；PRODUCTION_REVIEW/PQC_REVIEW保留工作台职责但不投递通知。通知回执接口还只返回当前收件人的记录，所以本人可见待办并不保证该回执列表有数据。需先核对截图对应taskType及当前用户是否原收件人。

代码：`MesActiveOrderHandoffDeliveryService.java:19—21`、33—39、74—76；`MesActiveOrderHandoffController.java:27—28`；`ActiveOrderHandoffPanel.vue`的refreshReceipts。若属于不投递任务，页面应明确“该类任务不发送通知”，而不是让空表暗示缺少审计。实际发送失败、日志和重试情况本轮未核对。

13、P11：审计停用/无外部可信时间——环境与能力边界，不等于签名详情故障

签名展示代码专门区分“本地摘要与时间值通过、创建审计已停用”和完整创建审计核验；时间信任NOT_BOUND明确显示“未绑定外部可信时间”。后台签名事实服务也返回此信任级别。记录本身对边界的描述成立；不能声称完整审计合规，也不能在本次问题定位中擅自开启配置。

代码：`activeOrderSignatureVerificationDisplay.ts:34—49`；签名模块`ElectronicSignatureVerifiedFactService.java:165/336`。需核对目标环境审计开关和预期信任要求，当前只确认代码支持并披露该边界。

14、P12：另一会话占用——原始回执已定位到浏览器工具层

原《085822-PQC生产放行回执.md》明确写“浏览器请求在页面打开前即被占用保护拒绝”，且未产生页面截图。这不支持“业务系统PQC页面锁定”的推断。后续正式放行回执又已恢复，因此应归为当时自动化会话资源冲突，不能作为当前产品缺陷或补签理由。

15、P04：正常单误填——操作事件，已走更正链，不能与P13混写

原外观首件误选不合格属于输入问题。退回、更正、重提、独立复核已有岗位回执；原事件327155不变、逐件值变为全合格、损耗仍1。其后总不合格仍在由P13的后台规则解释。不能把“误选已更正”扩写成“整条质量总判已合格”。

16、建议处理顺序

先明确并统一P13/P10的正式总判与项目判定；再修P14签名动作展示、P03损耗置零、P09页签状态、P06冻结操作提示。P01先查历史冻结快照来源；P02先核冻结系数，P05工单404先查该岗实际路由，P07/P08按任务类型和上下文细分。P04/P11/P12分别保留操作、环境能力、工具占用分类，不合并为未修复产品缺陷。

17、源码索引说明

以上Vue/TS均位于`IntRuoyiFronted/src`，MES Java位于`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro`（controller另按包路径），签名Java位于`IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service`。行号为本次读取时当前工作区行号。该记录不修改原执行报告及原截图。
