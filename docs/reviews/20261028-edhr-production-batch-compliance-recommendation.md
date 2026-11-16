# eDHR 生产批次、分次流转与清场落地建议

核查日期：2026-10-28（北京时间）。性质：业务与系统设计建议，不是现场合规验收或产品注册结论。

源码基准：本地 E:/IntRuoyi，读取时 HEAD 为 c97ef0f2a，并存在大量并行未提交改动；以下描述本次读取的工作区实现，不证明同版本已部署、经过生产验证。未修改业务代码、数据库或服务，未执行生产操作或 E2E。

## 1. 决策

**选择：保留15个真实操作工序，以现有活跃订单为生产入口，在获批生产批内按有唯一身份的小数量分次流转；按独立质量控制需要设置亚批；清场按实际区域、设备、切换事件和经验证的连续运行边界管理；定型阶段纸电并行。**

不把每天、每个周转箱、每200个产品自动定义为独立生产批；不把15步机械地删除为3步；不预先限定只清场3次；不为减少记录而普遍新增半成品入库。依据是用户已说明的订单波动、临时工、技能差异、狭小场地、低毛利，以及下述法规和现有源码结构。

这是基于现有证据的适配性判断，不是已经通过对照试验的“效率最大化”结论。实际收益要用合格产出、等待工时、在制数量及记录工时验证。

## 2. 适用法规与版本边界

| 编号 | 已核对的正式依据 | 对本方案的作用 |
| --- | --- | --- |
| R1 | [国内新版医疗器械GMP，2025年第107号](https://mpa.jl.gov.cn/ztzl_85150/sqxzjcgs/jcbz/202610/t20261008_9726502.html) | 2026-11-01施行；第43—46条文件/记录/电子记录，第69—77条验证，第78—94条生产控制，第95—104条检验/放行，第131条批的定义。按新版设计，同时在生效前遵守现行要求。 |
| R2 | [国家药监局2026年检查指导原则发布通知](https://www.nmpa.gov.cn/xxgk/fgwj/gzwj/gzwjylqx/20260915163938138.html)、[无菌器械附录正式发布页](https://www.scsjczx.cn/zcfg/gfxwj/20240403123634110.html) | 新版GMP生效后，适用产品仍需同时满足相应现行附录。洁净车间不自动等同于无菌交付；按实际注册产品及工艺确定适用性。 |
| R3 | [FDA QMSR](https://www.fda.gov/medical-devices/postmarket-requirements-devices/quality-management-system-regulation-qmsr)、[21 CFR 820.10](https://www.ecfr.gov/current/title-21/chapter-I/subchapter-H/part-820/subpart-A/section-820.10) | QMSR已于2026-02-02生效，纳入ISO 13485:2016及FDA补充要求；不再把旧820.184作为现行DHR条款。 |
| R4 | [21 CFR Part 11](https://www.ecfr.gov/current/title-21/chapter-I/subchapter-A/part-11) | 适用电子记录/签名的真实性、权限、审计、签名含义、内容关联、身份与凭证控制。核对11.1、11.10、11.50、11.70、11.100、11.200。 |
| R5 | [FDA Part 11适用范围指南](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/part-11-electronic-records-electronic-signatures-scope-and-application) | 允许符合条件的纸电混合记录；保留纸张不自动豁免实际依赖的电子记录。指南中的旧QSR引用应结合现行QMSR理解。 |
| R6 | [FDA生产及质量管理系统软件CSA指南，2026年2月](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/computer-software-assurance-production-and-quality-management-system-software) | 软件保证按预定用途和风险安排证据；该指南是建议，不等同于法规，也不免除适用的系统验证义务。 |
| R7 | [欧盟MDR最新合并文本，2026-07-19，欧盟出版局正式来源](https://publications.europa.eu/resource/celex/02017R0745-20260719.ENG) | 核对Art.10(9)、Annex I 11.4—11.6、Annex IX 2.2及2.4：体系、制造控制、适用的无菌过程、质量体系重大变更。CE是符合性标志，按MDR和具体符合性评定路径落实。 |

EUR-Lex网页返回验证页面后，通过欧盟出版局官方CELEX资源取得完整文本，标题为“02017R0745 — EN — 19.07.2026 — 007.001”，并核对上述相关条款。没有把第三方摘要当作法规全文。

2026-09-24发布的[无菌医疗器械检查指导原则征求意见稿](https://www.nmpa.gov.cn/xxgk/zhqyj/zhqyjylqx/20260924105843187.html)不能当作已生效法规引用。

方案覆盖制造过程、批记录和电子系统控制。产品分类、技术要求、无菌/包装验证、注册变更、FDA申报路径以及公告机构证书条件，不能凭“像大注射器”推断。它们须作为既有受控产品档案的实施输入，方案不改变或豁免这些要求。

## 3. 当前系统核查结果

| 编号 | 实际业务节点及源码依据 | 已确认能力或缺口 | 设计决定 |
| --- | --- | --- | --- |
| C1 | [组长前端](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:9704) → [接口控制器](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:302) → [加入活跃订单服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:923) → [订单数据对象](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolActiveOrderDO.java:29) | 按订单和路线版本建立/复用活跃订单，保存路线与QA版本，并生成逐工序快照和检验任务。 | 沿用活跃订单入口及版本冻结；生产批与ERP订单对应关系明确保存。 |
| C2 | [一线页面](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:5725) → [前端API](E:/IntRuoyi/IntRuoyiFronted/src/api/mes/pro/feedback/index.ts:1181) → [报工接口](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesProFeedbackController.java:114) → [提交服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:108) → [事件对象](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/MesProProcessPoolEventDO.java:27) | 已记录工序、人员、数量、设备、提交时间、签名和原始载荷，并产生订单初始数量归属。 | 把流转身份及消费关系关联这些原始事件，不复制另一套报工系统。 |
| C3 | [生产复核与分配](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:313)、[双进度计算](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java:48)、[完工服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java:165) | 工序进度来自已归属的数量，检验进度来自已确认任务；完工检查双进度。 | 保留完工控制，但不能用整单100%作为每次小数量转序的统一前提。 |
| C4 | [申请放行](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java:151)、[PQC放行](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:143)、[最终放行准备](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java:100)、[最终核验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/productionrelease/core/MesReleaseFinalizationValidator.java:17)、[批次归档](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:4883) | 当前代码在完工/申请路径关联批次执行，再进入PQC及最终放行；归档有关闭状态、PDF/A及存储证据检查。 | eDHR批次执行继续承载下游证据与归档；不能用亚批管理绕过放行。旧90步文档部分时序与当前代码有差异，以当前源码描述能力。 |
| C5 | [清场确认默认值](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:1621)、同文件resetProductionClearanceConfirmations/buildProductionClearanceConfirmationPayload；提交服务buildProductionSubmitAfterStateJson | 清场、物料、清洁默认及重置为true，随报工提交；后端审计保存该载荷。该事实本身不能证明清场实际实施，更不能证明独立清场生命周期完整。 | 取消默认完成；建立清场事件、实际范围、执行/确认及有效状态，再引用到受其覆盖的生产记录。 |
| C6 | [物料证据装配](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:557)、[完工入口](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java:124) | 一线提交将投入批号标记PENDING_COMPLETION，batchCodes及领料来源为空，完工再冻结/关联正式领料来源。 | ERP记账后置可以保留，但使用当时的实际物料批号和流转归属必须另有同步原始证据。 |
| C7 | [QA规程页面](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue:1058)、[巡检任务数量计算](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:4326) | 该路径支持固定数或计划量乘比例；4385行方法明确乘比例除100后向上取整。与用户描述的数量区间取样规则不一致。 | 在该任务生成链路实现正式区间抽样规则；不能把保留抽样原文等同于计算已执行该规则。 |
| C8 | [人员候选](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java:302)、[运行配置](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java:418)、[提交授权](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java:122) | 已有班组启用人员、员工切换、模板及个人签名校验；所查路径未见“具体工序培训考核与有效期”自动核验。 | 角色和班组成员身份之外，增加岗位操作资格证据及执行时核验；不据此断言公司线下无培训。 |
| C9 | 订单、工序快照、事件、批次执行数据对象；[正式追溯类型](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchTraceLinkType.java:3)、[追溯校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchTraceabilityValidator.java:31) | 所查主链路有订单、生产、检验、领料、签名、放行关联；未形成已证实的流转单元数量台账、亚批拆合及下游消耗闭环。 | 把这些列为新增能力。库存调拨追踪不等于工序间实物流转。 |
| C10 | [CSV现状说明](E:/IntRuoyi/docs/csv-validation/README.md:10)、[正式送审包说明](E:/IntRuoyi/docs/csv-validation/official-submission-package/README.md:7) | 仓库文档明确不能替代现场执行、真实签章和生产运行证据。文档较早，本次没有核实企业其他系统中的最新批准。 | 不宣称当前系统已经完成三地合规验收；上线验证以实际受控证据为准。 |
| C11 | [GxP运行模式](E:/IntRuoyi/IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditDevelopmentMode.java:15) | 存在FORMAL及受环境约束的开发模式；本次未读取生产运行配置。 | 上线验收必须证明正式审计模式及测试数据隔离，不把“代码含审计”当作生产审计有效的证明。 |

补充核实：[报工数量片段分配服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationQuantityFragmentService.java:46)已经对报工产出片段进行订单归属、保留数量占用及版本校验。这是可复用的数量分配基础；其目标对象是报工归属，不足以单独证明现场容器身份、逐次实物交接、亚批拆合和跨工序消耗均已闭环。因此C9所述新增能力应在评估复用该基础后补齐，不应重复建设已有分配功能。

以上为沿已读取链路的静态证据。未发现专门对象或校验，表示在本次核查范围尚未证实，并非全仓或现场绝对不存在。源码在并行变化，方法名与路径比单一行号更稳定。

## 4. 具体落地业务规则

### 4.1 批次与流转

订单是需求和计划单位；生产批是质量、追溯及放行单位；流转单元是实际交接的一份产品。三者分别有身份，关联清楚。一个生产批是否允许覆盖整个大订单，应由批准的批定义、均一性和连续生产证据决定，不能自动把10万个全算同一批。（用户约束、R1第131条、R3、C1/C9）

同一批准生产批中，原第1道完成约200个且满足该段过程检验及交接条件后，可按获批路线进入第2道；第1道继续加工下一份。200只是现行交接量参考，不设为法定或固定批量。要求整批处理或等待结果的步骤维持其条件。（R1第78/83条、R7 Annex IX 2.2；设计建议）

每份交接保留唯一编号、批号、数量、当前位置、工序完成状态、质量状态、发出/接收者及时间。部分交接、拆分或合并时保留父子关系，不能覆盖原身份。下道只能领取已允许转序且尚未被消费的数量，重复提交不得重复增加额度。（C2/C9、R1第81—83/90条、R4 11.10(f)；设计建议）

正式亚批用于确有独立加工、设备/时间分组、隔离、抽样或合批需要的对象。其生成、检验批关系、合批工序、允许条件、数量核对、异常处置必须受控。同主批不自动允许合批，不同条件产出不自动具有均一性。无需将每只周转箱都提升为有整套审批流程的亚批。（R1第131条、R3/R7；设计建议）

不新增独立半成品库存来承接每道工序。已有半成品按实际生产、备库和ERP规则继续管理。工序在制品的受控暂存与独立库存半成品是不同业务，不能混用。（用户第6/7项和场地约束）

### 4.2 15道工序与3阶段

保留15道实际步骤、人员、参数、检验和追溯节点。生产看板或归档可以按经评审的阶段分组；不在缺少工艺依据时强行分成5+5+5，也不删除现有逐工序正式表单绑定。工序开始关系、正式批记录绑定、formBindings仍独立维护。（项目AGENTS.md、C1/C2、R1第43/74/78条）

合并管理阶段如果实际改变工艺、污染控制、检验时点或物料暴露条件，应按研发发起的正式变更处理；若仅为展示分组，也需确认不会影响原有执行控制。不得以记录数量减少作为质量风险已降低的证据。（R1、R7 Annex IX 2.4）

### 4.3 清场与连续生产

把清场对象落到实际工位、设备或经确认的区域。事件记录应说明前次/本次产品与批次、原因、范围、方法/文件版本、实际执行者、结果、所需复核及时间。生产开始检查有效的清场/清洁状态，不能每次报工都自动生成一次“已清场”。（C5、R1第86条、R4）

同一有效状态覆盖范围内的多份生产记录，可引用同一次真实清场证据；适用范围改变、发生规定切换/异常或达到批准时限后应失效，重新执行所需动作。不同地点不能仅因同属一个管理阶段而共用一份不覆盖现场的清场记录。（R1第86/92/93条；设计建议）

每日终清场保留为用户提出的生产约束，跨天在制品按批准条件清点、隔离标识、防护及交接；次日复开确认。日终清场不自动生成新生产批，也不代表前日所有产品已经完工。清洁、消毒与清场的频次和内容分别受控。（用户约束、R1、R2、R7 Annex I 11.5/11.6）

批量、连续生产时间、工序间最长等待/暴露时间和暂存条件须由实际工艺与验证决定。本建议不凭空给出8小时、24小时或“只清场3次”等数值。（R1第69—74/93条、R7）

### 4.4 抽样、人员、物料及异常

按既有正式抽样方案实现数量区间、适用检验批、检查水平/质量水平及Ac/Re等适用参数，不默认任何具体标准或AQL数值。版本冻结；明确过程巡检与合批/成品验收的关系。不得按亚批随意拆小检验批来减少取样，也不得把订单量百分比当作数量区间查表。（用户抽样规则、C7、R1第97/99/100条、R7 Annex IX 2.2(e)）

岗位资格表记录人员、产品/工序范围、培训与实操考核证据、有效期或复评条件、授权状态。系统只允许在资格范围内执行；临时工和正式工适用同等能力要求。已有共享终端可继续作为终端身份，但操作和签名必须绑定真实个人，不能由组长统一代签。（C8、R1生产人员/过程控制要求、R3、R4）

ERP调拨和耗料记账维持现有业务时点；在实际投料/换料时记录真实物料批号、数量或适用用量依据以及流转对象。后续ERP领料单到达时做来源对账并保存差异，不用整单领料批号反向猜测某一份产品用了什么。（用户流程、C6、R1追溯、R3/R7）

目前正式损耗读取包含按补料单汇总的口径，应保留其ERP业务含义，同时独立对账实际不合格、报废、返工、取样、余料和退料。没有补料单不能证明没有质量损耗；补料数量也不能未经证据直接当作实际报废量。（[损耗读取源码](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.java:141)、R1第82/87/88条；设计建议）

发现异常，按可能受影响的亚批、时间窗口、设备或物料来源标识隔离，并追查已经进入下道的产品；证据不足时不能任意缩小影响范围。返工与重新检验记录继续关联原批身份。缺陷品不得通过合批稀释或掩盖。（R1第87—90条、R3/R7）

### 4.5 完工、放行与纸电并行

现有“生产进度100%＋检验任务100%”是任务完整性指标，不等于每件产品100%全检，也不单独证明产品合格。最终关闭须同时满足实际数量对账、规定工序/检验、物料追溯、异常处置和适用的灭菌/成品资料及授权放行。（C3/C4、R1第104条、R3/R7）

流转单元允许转序与生产放行、上市放行分别管理。继续使用当前PQC生产放行与上市放行链路，但放行人的法定/组织授权需以受控职责文件为准，不把某个软件角色名称当作三地法规通用资格。（C4、R1、R3/R7）

纸电并行应定义每类字段的正式原始记录、转录/核对方式、更正流程和差异处置。电子记录如已用于转序、判定或放行，就不能因同时留纸而忽略适用电子记录控制；最终档案需保持纸电关联及全部原始来源。（R5、C10）

## 5. 最小系统增量与实施顺序

| 顺序 | 工作 | 完成标准 |
| --- | --- | --- |
| 先纠正现有记录与规则 | 清场不默认通过；正式数量区间抽样；投入物料即时身份；岗位资格核验 | 正式规则有受控版本，默认值不能代替事实，不满足条件时准确阻止相应动作。 |
| 再补分次流转闭环 | 在活跃订单下关联生产批、流转编号、上游交出/下游领入、数量占用、质量状态、必要拆合 | 不重复消费、不串批、不漏数量；异常能够定位已流入的下游对象。 |
| 延伸现有证据链 | 将新增证据关联生产/PQC事件、完工对账、放行快照、审计及归档 | 能从成品批反查实际物料、人员、设备、过程、清场和检验，也能从异常来源反查产品。 |
| 批准并试行 | 完成工艺/质量体系变更和必要验证，使用现有合规容器、标识及终端；纸电并行 | 企业质量批准与适用外部变更要求完成，现场证据与系统记录一致。 |
| 扩大使用 | 依据试行数据判断是否推广 | 合格产出、工时、积压、记录负担达到事先批准目标；无未关闭的关键风险。 |

最小实现不等于只增加一个“亚批号”字段。真正必要的是身份、数量、状态和原始证据的闭环；除此之外，不建议先建设新的排产平台或全工序半成品库存体系。

## 6. 必须完成的验证与外部变更判断

生产验证覆盖所拟连续生产边界、最不利等待与暂存、清场/清洁有效性、适用的污染控制及灭菌/包装影响。检验方法与取样方案要能支持拟采用的批和亚批定义。（R1/R2/R3/R7）

软件验证围绕关键风险：错批、重复扫码/提交、并发领取、少数/多报、部分交接、退回返工、不合格冻结、资格失效、清场失效、抽样区间边界、ERP延迟及差异、纸电不一致、记录更正后旧签名、归档完整性、恢复后追溯。使用FDA CSA风险方法安排工作，不能以普通单元测试替代企业预定用途验证。（R4/R6、C10）

签名保留个人身份、签名时间及含义、内容绑定与更正历史；验证共享终端下个人身份不能冒用。正式审计模式、留存策略、备份恢复、导出可读性和权限停用都要有运行证据。法规没有普遍要求某一特定WORM品牌，现有存储措施按效果和适用要求验证。（R4、C4/C11）

注册/质量体系变更由法规和质量负责人按产品实际证书路径作书面判断。欧盟适用Annex IX时，达到重大体系或产品范围变更条件的，按2.4通知公告机构并完成相应评估；FDA及国内变更申报是否触发，依据具体产品和变更影响判定。不得笼统说“内部批准后所有市场都可直接实施”。（R1第74条、R3、R7）

## 7. 效率判断与明确限制

建议用现有场地规定工序间在制品上限，以瓶颈工序消耗速度安排前道投料；有资格的人员补瓶颈岗位。容量可以用“下道实际消耗速度×允许缓冲时间”估算，再受场地与验证上限约束。该式是生产组织建议，不是法规规定值。（用户限制、C8、R1过程控制）

比较试行前后的每工时合格产出、下道等料时间、最大在制数量、清场实际耗时、记录/复核耗时、差错与不合格情况。预定工艺检查、法定/批准清场和必要记录不作为可随意压缩的效率成本。（R1/R3/R7；设计建议）

本次结论：上述方案最贴合已给出的生产约束和本地eDHR结构；**当前不能把现有系统认定为已完成国内GMP、FDA和CE全项合规验收。**需要关闭列出的实现与证据缺口，并完成适用的企业验证及变更程序后，才能在受控范围内正式采用。
