# 0626问题最终确认报告

1、**总评：不能将四项全部登记为确定缺陷。** 已确认历史展示差异、设备编号空白、两份成品文件合并分组；代码审查进一步确认设备可选、成品任务独立、数量告警口径分离。问题一根因、问题二候选配置适用性、问题三分类设计及问题四三场景真实表现仍有边界。

2、**核验方式与范围。** 四个子 Agent 分别追踪四类业务链，一个子 Agent 用独立 Playwright 会话只读查看；主 Agent 复核原始回执、关键源码及新旧截图。未改源码、未写业务数据、未重置周期、未新建订单、未重启服务，也未直接调用业务API或数据库。

3、**对象与版本边界。** localhost:8081／测试1；周期1009200626，批次900000001341，业务号EDHRB-1794943187955，AI30／5000件。完整工单为 SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891。审查工作区为int_main，HEAD为831ad4532321246e9e1763ec2ef47fb249c645b1，含既有未提交改动；未证明运行服务与该源码完全一致。

4、**问题一：展示不一致的历史记录成立，根因未确认。** 原始生产回执明确记载申请前列表“批次号缺失”、详情批号完整；原始活跃池截图支持缺失提示，但单张截图不能独立证明目标工单的完整身份及同时的详情值。本次历史批次总表可见完整生产批号，关闭后的原活跃详情返回不存在，无法原样重现申请前差异。

5、**问题一字段含义已核清。** 标准入口列表和详情都读取生产工单batch_code，并非一个生产批号、一个下游批次执行编号。列表按活跃订单workOrderId取工单；详情按正式工序快照work_order_id关联工单，实际关联是否一致尚未取证。[列表赋值](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:3852)、[详情SQL](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml:42)。

6、**问题一完整链条与判断。** 活跃池→teamLeader.ts列表API→组长Controller查询权限→按组长可见范围加载ACTIVE订单→Service映射工单批号→列表缺失提示。详情按钮传同一row.id→详情API→查询权限/订单范围→正式快照SQL→DetailService→Panel生产批号。标准按钮不带sourceWorkOrderCode；特殊来源工单参数会改变总表资料来源，只能列为核对完整URL的边界，不能当成本次根因。

7、**问题一排除方向与待验收。** [ReleaseGenerationService:272](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseGenerationService.java:272)要求申请前正式生产批号已存在；申请时才openOrCreate下游批次执行。不能以“尚未生成批次执行”解释生产批号缺失，也不能用工单号补值。新获授权周期应在申请前、生成后分别比对页面、完整URL及工单关联。

8、**问题二：入口与编号空白已确认，强制绑定违规不成立。** 原始无跳压截图确实显示“无设备”、无选择入口；本次历史页复查首检、巡检及包装首检，设备均显示“-”，方法仍有专用泵筒、砝码、泄漏器具要求。历史记录不能证明器具实际可用或使用，也不能还原提交前候选配置。

9、**问题二现行规则明确设备可选。** [frontend-development.md:214](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/docs/frontend-development.md:214)明确允许PQC未选设备或“其他”留空按空设备快照提交，不得阻断完整结果。方法要求与编号绑定是两件事；旧“设备全部必填”报告不能恢复为当前门槛。仍须确认本订单锁定QA版本是否应提供适用候选，“无设备”文案应避免被理解成不需要器具。

10、**问题二完整链条。** Panel独立展示方法，按equipmentOptions非空提供选择→工序查询API/PQC角色权限→ContextService读取订单锁定QA→EquipmentConfigService按项目/版本及启用设备组读取候选→提交重验任务、人员、ACTIVE状态及候选→空设备允许落库→复核签名/汇总原样保留快照→历史显示空设备。部分锁定快照带未绑定原因且候选空会阻断，未获取0626实际快照，不判定触发条件。

11、**问题二单位边界。** 326266原文的1.2N/15mm、1.5N/15mm是接收标准单位，应与方法中砝码说明分别核对。本次只读页仍可见相关原文；不能把N/15mm直接视为设备规格，也不能用FM-E-24／15N替代。未新增强制绑定、未虚填编号。对应实现为[ContextService:2205](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java:2205)。

12、**问题三：上传成功与两任务完成的历史证据成立，未发现错存依据。** 本次只读页面仍见两份PDF，钱凤琴上传，时间分别为2026-11-18 03:42:02、03:43:11（页面北京时间），均在成品检文件，其他文件为空。两项“我已处理”依赖原始任务截图及回执，本次未重新进入钱凤琴任务列表，不声称当前待办再次通过。

13、**问题三完整链条一致。** [UploadService:39](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesReleaseAttachmentUploadService.java:39)将报告/记录两个责任角色均映射FINISHED_PRODUCT_FILE，分别生成任务→Panel独立按钮传相同categoryKey、不同workTaskId→Controller/上传范围校验→附件按类别入库→CompletionService核订单、类别、上传人→指定任务DONE并审计taskId与attachmentId，非一份文件完成两任务。

14、**问题三设计待裁定。** 文件表和列表只有大类categoryKey，没有独立报告/记录子类型列；下游签署清单沿用该分类，当前两份靠文件名区分。协调员及成品检岗位指南仍写“记录归其他文件”，与实现和历史页面冲突。已确认的是文档与实现不一致，尚不能裁定哪方应改；不能搬文件。任务语义可在独立责任来源与附件审计中追溯，不等于文件列表已结构化区分。

15、**问题四：未确认参数提示遮蔽数量提示；已查明两种数量口径。** 一线提交时CapacityService按报工与冻结上限计算提示，SubmitService把productionQuantityWarning存入正式事件rawPayload，Panel弹出提示。上限按冻结ERP数量×流程损耗系数计算；5000×1.1=5500，但本次未读取0626冻结快照，不能断言该周期系数具体取值来源。

16、**活跃池显示的是当前正式分配超限。** [Service:3667](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:3667)按当前分配事实计算quantityConflict，不读取原始报工告警；[Calculator:200](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesOutputMaterialProgressCalculator.java:200)按订单/工序过滤分配并折算物料产出。报5501、分配5000、余501时，未分配量不另计入列表，因此数量标签可以不出现。

17、**问题四渲染与放行边界。** [Workbench:1784](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:1784)为两个独立v-if，并非互斥标签；背景类优先显示数量样式。该结论不排除真实窄列裁切。前端放行条件不含这两种提示，后端超产计算未新增超限阻断，符合仅提示规则。是否持续展示“原报工曾超限/仍有余量”应先确认设计。

18、**问题四实际测试范围。** 子 Agent运行现有 `node --test tests/unit/active-order-parameter-warning.test.cjs`，3/3通过。测试仅覆盖背景优先级、参数标签单独渲染与前端放行函数；没有证明两个标签同时可见或后端整链通过。新的真实周期中“仅数量／仅参数／两者并存”三场景均未执行，不能标PASS。

19、**余501跨订单分配仍为未测项。** 静态链为前端候选与保存→teamLeader API→Controller组长复核权限→AllocationCommandService校验工序/员工、活跃状态、关闭/冻结锁、同产品/规格/项目/冻结工艺版本→替代旧分配并插入源/接收订单CURRENT记录→重建数量片段、汇总及审计。总分配须大于0且不超实际报工量。候选存在不证明符合上述条件。

20、**跨单实际边界。** 未获明确适用且授权的接收订单，未执行跨单保存，也未确认接收方实际落库或进度变化，不记失败也不记通过。依据为[AllocationCommandService:318](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:318)。新周期及接收对象授权前，不重置已关闭0626证据。

21、**本次新增页面证据。** 独立Playwright以测试1／张飞红从批次历史详情查看，入口 `/mes/pro/feedback/edhr-batch-execution/active-order-detail?batchExecutionId=900000001341&from=execution&tab=FINISHED_PRODUCT_FILE`。截图：[总表](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-summary-current.png)、[成品文件](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-finished-files-current.png)、[其他文件](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-other-files-current.png)。

22、**本次新增PQC与关闭态证据。** [无跳压首检](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-pqc-nojump-first-current.png)、[无跳压巡检](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-pqc-nojump-patrol-current.png)、[包装首检](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-pqc-pack-first-current.png)、[关闭后的活跃详情](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/review-readonly/0626-closed-active-detail-current.png)。关闭态入口不存在仅作为验证边界，不扩定新缺陷。

23、**原始依据。** [生产回执](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/production/0626-production-receipt.md)、[PQC回执](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/pqc/0626-pqc-receipt.md)、[成品资料回执](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/documents/0626-finished-goods-upload-receipt.md)、[上市放行回执](D:/ProjectPackage/IntRuoyi/IntRuoyiAll/workspace/eDHR/流程协调员/output/full-market-validation-20261118/market-release/0626-market-release-result.md)。相对路径起点为流程协调员工作区，证据并未缺失。

24、**最终处理建议。** 问题一保留“生产批号展示不一致，根因待复现”；问题二保留“设备文案/适用候选待核”，不加必填；问题三保留“分类规则和子类型展示待设计裁定”，不搬文件；问题四保留“报工告警与分配告警口径待统一”，不认定遮蔽。跨单仍未测。旧500、版本错误本次未专项复现，不能宣称已修复；模拟流程证据不代表真实检验或真人质量批准。
