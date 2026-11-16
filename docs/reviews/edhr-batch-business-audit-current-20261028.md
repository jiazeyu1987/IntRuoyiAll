# eDHR批记录：业务需要与当前实现对照

1、范围和结论

核对日期：2026-10-28。对象为 E:/IntRuoyi 当前工作区源码，包括已授权整改中的未提交改动，不代表运行服务已更新。当前“批记录”包含批次执行、历史追溯、不合格评审、偏差管理、作废五个页签，按业务目的归并为26类场景。关联详情、活跃订单、审批中心、个人工作台一并追踪，后台残留接口与真实页面入口分别判断。

先确定业务需要，再检查页面→接口→角色/权限→前置状态→业务落库→任务/通知→工作台及后续动作。签名不等于审批，纯通知不等于待办，存在方法不等于闭环，源码与专项测试不等于部署后的真实送达。

大部分原缺口已有代码：QA派审、偏差交接、返工接手、重开补录派审、资料补齐、PQC补正、整批作废工作台，以及新补的冻结告知、期限配置、单记录作废、变更否决分支、上市负向决定。**仍不能认定全部解决**：审批归类、冻结覆盖、QA结果告知有具体差异，新版本迁移与真实页面验收尚未全部完成。

2、业务判断标准

| 类别 | 应当需要的条件 | 接收对象与结束条件 |
|---|---|---|
| 通知 | 分派、责任转移、退回补正、冻结/解除、质量结果、约定到期 | 本次责任人及实际受影响的人；不向所有同角色人员群发 |
| 审批 | 正式放行、质量处置、失效、已确认内容变更的授权决定 | 本次有权决定的人；保留同意、不同意/退回、意见及签名 |
| 个人工作台 | 本人还有应当实际办理的动作 | 展示原正式任务，办理成功后关闭；转派更新归属，同一事项不复制审批 |
| 电子签名 | 证明谁确认了哪份内容或事实 | 填写、上传、调查、验证也可签名，不能据此都算审批 |

查询、预览、打印、保存草稿、自动处理成功通常不需通知。原负责人可原地重试的同步失败，应报错并保留原任务，不能统一再建“异常审批”。冻结应保留任务并说明阻塞，QA等解除问题的任务仍须可办理。

3、26类功能逐项统计

“已有代码”仅指追踪到相关实现，不等于环境验收。“按条件”必须有真实责任交接、人工处置或明确时限。

| 编号 | 功能、主要入口 | 是否需要通知 | 是否需要审批 | 是否进入个人工作台 | 当前代码判断 |
|---|---|---|---|---|---|
| 01 | 批次列表、详情、来源查看／批次执行 | 否 | 否 | 否 | 只读入口，不应因此新增任务。E01 |
| 02 | 放行预检、记录/资料完整性检查／批次执行 | 按条件：缺项须由他人补齐 | 检查本身否 | 仅实际补件责任人 | 检查和显式资料指派已有；提示检查失败不等于派单。E02 |
| 03 | 自动回填正式记录、关联证据／批次执行 | 成功否；需交给他人的失败才需要 | 否 | 原责任人保留原任务；真实交接才派单 | 同步失败不标记成功、不完成原任务；不能据没有专属异常待办判为缺失。真实失败重试仍待页面验证。E07 |
| 04 | 填写、签名、正式复核/批准、退回修订／详情 | 分派、退回、转派时需要 | 普通填写否；正式复核/批准需要 | 填写、复核、批准、修订责任人 | 任务链已有；审批查询仍允许带签名位的候选FILL，不能称已完全按业务区分。E03、G01 |
| 05 | 来料、灭菌、成品等必需资料补齐／关联活跃订单 | 是：指定资料责任人 | 上传本身否 | 是：资料责任人 | 显式选人→订单/类别/轮次任务→通知→本人上传完成原任务已有；同轮换人保留期限。E02 |
| 06 | 其他附件、文件整理、未确认内容保存 | 自行操作否；受托补件需要 | 否；已确认内容变更另见23 | 仅受托补件人 | OTHER_FILE可复用资料交接；普通保存/上传不应额外审批。E02 |
| 07 | PQC生产放行申请与决定 | 是：审批人、申请人、后续责任人 | 是：PQC质量放行 | 是：正式PQC审批人；驳回后补正人 | 申请、候选、签署、完成、结果通知及补正新轮次已有。E04 |
| 08 | 上市放行申请与决定 | 是：审批人及结果相关人 | 是：上市放行 | 是：上市审批人；拒绝后指定QA | 批准原链保留；新增拒绝签名、结束原任务、冻结并转NCR。新代码仍待整体验收。E05 |
| 09 | 上市放行交接、交接收回 | 是：新旧责任人 | 交接本身不另加质量审批 | 更新原放行任务归属 | 原任务责任、授权及通知已有，不应另造第二张上市审批。E05 |
| 10 | 放行结果、撤回、补正重提 | 是：申请人、原处理人和后续责任人 | 结果告知/申请人撤回不另审批；重新放行仍走审查 | 结果本身否；补正需要 | PQC补正已有；新补上市撤回、拒绝后QA让步再补正，保留旧PQC批准及上市否决，新轮次独立。E04、E05 |
| 11 | 完工后系统留存、历史归档及异常 | 主线自动成功否；确需交接的异常才需要 | 不再重复放行审批 | 主线不新增人工归档任务 | 明确需求是上市放行后系统留存并可历史追溯，不要求自动PDF或人工归档负责人。旧报告的PDF缺口判断撤回。E07 |
| 12 | 历史查询、反向追溯、签名/审批审计查看／历史追溯 | 否 | 否 | 否 | 查询与正式详情已有；历史追溯不依赖PDF生成。E01、E07 |
| 13 | 已有归档预览、下载、打印／历史与详情 | 否 | 否 | 否 | 保留证据查看和输出；不因查看/打印另派审批。可选PDF生成与主线留存分开。E07 |
| 14 | 创建不合格、冻结批次/工单／不合格评审及关联入口 | 是：QA及实际受影响人 | 创建/冻结本身不另审批 | QA评审；受阻任务保留并提示 | 四类创建路径已接冻结告知；告知记录为DONE、不进待办。新任务覆盖与冻结展示仍见G02。E06 |
| 15 | QA让步、返工、作废处置／不合格评审 | 是：原组长、后续执行及受影响人 | 是：QA质量处置 | 是：本次指定QA | 身份、签名、处置落库与原任务完成已有；QA作废不重复同目的独立作废审批。结果覆盖见G03。E06 |
| 16 | QA批准后继续生产、返工接手 | 是：组长及真实执行人 | 执行已批准返工不重复审批 | 是：实际接手/执行的人 | QA_DECISION_HANDOFF进入待办，正式接手后关闭；返工走后继周期。上市拒绝后QA让步可衔接补正。E06、E05 |
| 17 | QA作废结果、作废记录查看／不合格评审、作废 | 结果需要；查看否 | 查看结果否 | 纯结果否 | 组长结果交接仅通知且已完成，不派正常生产待办；作废页为只读台账。其余受影响人的结果通知不足。E06、G03 |
| 18 | 发起偏差、指定接收调查人／偏差管理 | 是：接收人 | 发起/事实陈述否 | 是：接收/调查人 | 明确责任人、接收任务与通知已有。E08 |
| 19 | 偏差调查、纠正、效果验证、执行签署 | 是：下一阶段/变更后的责任人 | 调查纠正否；验证按职责确认事实 | 是：当前调查、纠正、验证人 | 执行任务及内容版本签署任务已有，内容变更使旧任务失效。不能全部算质量审批。E08 |
| 20 | 偏差质量审核、重大偏差决定、关闭 | 是：指定审核/关闭人 | 质量授权决定需要；关闭核对执行证据 | 是：当前审核/关闭人 | 审核、关闭、版本和证据已有；普通五类/重大六类签署的必要性仍需分级职责依据。E08、G04 |
| 21 | 重大偏差转不合格评审 | 是：QA及受冻结影响人 | 转单不另造同目的审批，复用QA处置 | 复用新NCR的QA任务 | 明确选QA、转单派审、冻结告知已有，不应重复质量审批。E08、E06 |
| 22 | 独立整批/单记录作废申请与决定 | 是：审批人、申请人、实际受影响人 | 是：独立失效授权 | 是：原正式审批；退回后补正人 | 整批走业务策略/BPM原任务；单记录新补页面、独立审批人、批准/驳回/退回/重提及任务通知。实际环境待验。E09 |
| 23 | 已确认批次/记录重开、补录、批准后修订 | 是：审批人、申请人、修订人 | 是：已确认内容变更授权 | 是：审批、补正、实际修订 | 派审/修订已有；新补正式驳回、退回补正、新签名新轮次，旧证据保留；禁止自审、源版本/冻结/上市进行中限制已有。E09 |
| 24 | 按规则跳过可选项目、推进下一工序 | 跳过否；新责任交接需要 | 合法可选项不额外审批；不绕过必填/质量门禁 | 下一项实际处理人，不建“已跳过待办” | 是否可跳过须按具体模板/工序规则核验；本次不将全部跳过范围判为已验收，也不统一加审批。 |
| 25 | 演练、阶段模拟、金手指辅助／测试模式 | 正式生产不因此通知 | 不得替代正式质量审批 | 不得用模拟待办替代真实任务 | 旧批量直接作废入口已删；测试模拟仍保留，非正式主线。记录变更限制已覆盖金手指不能绕过冻结/上市进行中。E10、E09 |
| 26 | 约定期限、超期提醒、责任变更 | 约定到期或实际换人时需要 | 否 | 保留原任务，提醒不复制待办 | 新路线时限覆盖偏差、变更、补件、补正、QA交接；无规则不编造SLA。有受理人通知本人，否则冻结候选；校验正数消息回执。环境生效待验。E11 |

4、尚未达到与尚未证实

| 编号 | 分类 | 当前代码事实及影响 | 应达到的业务边界 |
|---|---|---|---|
| G01 | 仍存在过宽审批归类 | WorkTaskMapper.approvalCenterQuery允许候选FILL且signature_cell_key非空进入审批；getApprovalCenterAuthorizedPrefix直接投影，适配器提供办理导航。该筛选没有确认签名职责属于正式复核/批准。普通上传/归档普遍混入已收窄，但不能称全部清理。 | 普通填写/本人事实确认属于执行待办；正式质量复核/批准才按职责进入审批。这里是分类过宽，不等于额外执行了一次审批。E03 |
| G02 | 冻结告知与阻塞提示未覆盖全部新任务 | 冻结告知普通任务白名单为FILL/REVIEW/APPROVE/RELEASE_APPROVE/REWORK/ARCHIVE/CLOSE，不含记录变更审批/补正，且后两类建单未设置workOrderId。工作台冻结提示及服务isOrdinaryBatchTask只列六类普通任务，也不含RELEASE_APPROVE、RECORD_CHANGE_APPROVE/CORRECTION。后端已禁止冻结下记录变更，页面仍可能显示待处理。 | 按正式批次/工单关系覆盖实际受阻责任与状态提示；QA/偏差处置保持可办理，不能仅靠点击后报错。E03、E06、E09 |
| G03 | QA处置结果只交组长 | qaDisposed只向productionLeader(target)建立QA_DECISION_HANDOFF；未复用冻结影响集合告知其他受影响生产、检验、放行责任人。收到冻结告知的人未必收到解除或作废结果。 | 实际受影响人收到结果；仅有后续动作的人进待办。作废不再派正常生产执行任务。E06 |
| G04 | 偏差签署数量业务依据未确认 | 普通固定编制、核验、部门负责人、QA、质量负责人五类，重大加管理者代表。它们不全是审批。 | 结合偏差等级、质量授权与职责规则判断，不能全删或全部算审批。E08 |
| G05 | 新版本运行集成/真实页面未完成 | 新期限字段/配置、否决字段与模板、上市负向通知、补正唯一列均有迁移；专项通过不证明目标库已应用、消息实际送达。扩展放行回归仍有7项失败，不能报告整类通过。 | 完成真实构建与必要迁移，真实账号从页面办理、签署，核验通知、工作台、终态及旧证据。不可用API代办冒充页面E2E。 |
| G06 | 正式时限、调度生效未证实 | 无规则返回无期限；历史无期限任务不编造日期补齐，因此代码支持提醒不等于全部任务正在提醒。 | 仅给已约定节点配置真实时限，并核验调度及送达；不能擅设统一SLA。E11 |

G01—G03是具体实现差异，G04是业务依据不足，G05—G06是运行/验证边界；不能混为同一种“功能没写”。

5、旧问题哪些已经补齐，哪些代码不应保留为主线

| 旧结论或实现 | 当前判断 |
|---|---|
| QA无派审、偏差无交接、重开补录无派单、返工结果只有通知 | 已有任务/通知链路，不能继续复述旧报告为完全缺失；整体运行仍待验 |
| 冻结只通知QA | 已补冻结责任快照告知，但仍有G02覆盖差异 |
| 新任务无期限、候选超期只发assignee | 已补显式时限、QA扫描、候选展开及回执校验；实际提醒受G05/G06限制 |
| 单记录作废只有失联接口 | 已有RecordChangePage真实调用及独立派审；不再是无Vue调用的残留 |
| 重开补录只能批准 | 已有驳回、退回、原申请人补正重提新轮次；旧结论过时 |
| 活跃订单上市放行禁止拒绝/撤回 | 新入口与服务已补；拒绝转QA、撤回补正、QA让步后新轮次。最终集成未通过不能算交付完成 |
| 自动归档必须自动PDF/人工归档员 | 属错误扩张，撤回。明确业务只要求系统留存及历史追溯，不应因此加任务 |
| 旧四类上传初始化、人工关闭入口、金手指批量直通作废 | 当前工作区已有删除；不恢复为主线必经步骤。历史事件名称映射不等于旧能力仍可办理 |
| 非活跃订单人工归档、可选PDF生成/查看 | 不能因主线自动留存就全部判死代码；按实际调用范围保留 |
| 上传/调查/验证的签名审计 | 必要证据可保留；纠正审批分类，不一律删签名 |
| 测试模式演练/模拟 | 非正式主线，不能作为质量审批或生产待办；是否删除是单独需求 |
| 查询/预览/下载/打印/草稿/自动回填成功统一通知 | 业务通常不需要。本次没有证据证明这些额外行为普遍存在，不为凑问题作断言 |

6、两类选人字段

QA评审人用于不合格单的本次质量处置，涵盖批次驳回、活跃订单发起、新建不合格、重大偏差转NCR、新增上市不予放行转QA。明确选本次责任人，不能向所有有QA权限的人派审。

记录变更审批人用于重开、补录、单张记录作废的独立决定；申请人不能自审，退回由原申请人补正。整批作废另走业务审批策略/BPM；QA处置中的作废已经是质量决定，不重复同目的独立作废审批。

7、代码依据与调用链

| 证据 | 入口及关键下游 |
|---|---|
| E01 | [五页签](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue:8)→BatchExecutionListPage/Detail→批次API/控制器/服务；BatchRecordHistoryPage→releasedOnly→正式活跃订单详情；BatchVoidedPage为只读台账 |
| E02 | BatchIntegrityCheck→完整性接口/服务；DossierResponsibilityAssignment→dossierCompletion API→资料控制器→[资料派发服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierCompletionService.java:59)→任务/通知；DossierCompletionPage→本人上传→completeUploadedCategory |
| E03 | [审批候选条件](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java:34)→getApprovalCenterAuthorizedPrefix→MesProEdhrApprovalTaskAdapter；[冻结可办理判断](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java:698)；[工作台联合查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml:5)→MesEdhrWorkbenchSource→页面导航 |
| E04 | PQC页面→MesPqcProductionReleaseServiceImpl→正式申请/PQC任务→签署/完成；[补正服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseCorrectionService.java:119)→原申请人任务→资料更新→新PQC轮次；MesReleaseOutcomeNotificationService→投递持久化→提交后发送/重试 |
| E05 | [上市负向决定组件](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/production-release/components/MarketReleaseNegativeDecision.vue:1)在批次/放行列表接入→release API→统一终结命令→ManagerApprovalService校验原申请/版本/责任→[负向收尾](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:346)→NCR或补正→结果通知；补正服务核对原否决事务和本轮QA让步证据 |
| E06 | 不合格页面/各创建入口→NonconformanceReviewServiceImpl→签名、冻结、处置落库→[QA派单、冻结告知、结果交接](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java:300)→MesNcrFreezeRecipientResolver→delivery投递；qaDisposed→组长QA_DECISION_HANDOFF→工作台→正式接手关闭 |
| E07 | [明确归档要求](E:/IntRuoyi/docs/bugs/20260928-edhr-flow-improvement-backlog.md:11)；[上市放行后留存](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:1156)→历史查询→getArchivedFormalDetail；CompletionSourceBackfillService失败不标成功，openTask失败不完成原填写任务；可选generateArchive失败不完成原归档任务 |
| E08 | [偏差详情职责](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue:497)→偏差/处理/转NCR接口→HandlingServiceImpl→DeviationReviewTaskService→RECEIVE/CORRECT/SIGN/APPROVE/CLOSE任务、版本失效、通知/工作台 |
| E09 | 整批作废：批次列表→RecordChangeController→BusinessApprovalOrchestrator→正式策略/BPM→[原Flowable工作台来源](E:/IntRuoyi/IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/workbench/EdhrVoidWorkbenchSource.java:11)。记录变更：[RecordChangePage](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr/RecordChangePage.vue:1)→change.ts→控制器→[决定与重提、单记录VOID、重开补录](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java:125)→MesEdhrRecordChangeTaskService→任务/结果通知→工作台；OutcomeReader读取正式决定与签名意见 |
| E10 | 当前diff删除旧关闭/金手指批量作废控制器、VO和前端调用；BatchExecutionDetailPage测试模拟仍保留，不能替代正式审批 |
| E11 | 工艺路线“业务处理时限”→DeadlineController→[显式时限规则](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesEdhrTaskDeadlineService.java:52)→新任务dueTime；MesEdhrWorkTaskOverdueJob→普通/QA扫描→[受理人或候选通知及回执](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java:3150)；新增字段/模板/唯一列迁移位于IntRuoyiBackend/sql/mysql/20261028_mes_* |

8、验证证据与准确边界

当前记录变更七组JUnit共80项、零失败/零跳过，见[结果](E:/IntRuoyi/.tmp/edhr-sol61-f05-regression/reports/TEST-junit-jupiter.xml)。最新超期回执专项18项通过，见[结果](E:/IntRuoyi/.tmp/edhr-overdue-receipts-regression/reports/TEST-junit-jupiter.xml)。补正下游四组58项、前端补正行为3项通过；上市负向定向40项通过，但扩展原放行H2回归仍有7项失败，集中于正向批准门禁及签名/审计身份夹具，整类不能判通过。不同轮次、重叠测试不累加成总数。

这些结果只支持相应服务行为，不证明G01—G03已解决，不证明运行数据库已更新或消息实际送达。约定测试租户既有VOID策略已通过正式UI开启BPM；这是环境准备，不是新代码申请/审批/工作台/结果E2E。记录见[UI准备证据](E:/IntRuoyi/output/edhr-e2e-readiness-20261028/ui-preparation.md)。

本次统计更新报告，没有把尚未完成的集成、实际通知投递或真实业务页面验收写成全部通过。
