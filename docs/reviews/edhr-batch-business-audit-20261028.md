# eDHR批记录：业务分类与当前实现核对

> 这是本日较早的整改前快照，其中F01—F06的部分缺口随后已有代码补齐。当前结论以[最新业务与代码对照](E:/IntRuoyi/docs/reviews/edhr-batch-business-audit-current-20261028.md)为准；尤其不得再把自动生成PDF当作主线归档要求。

1、范围与结论

核对日期：2026-10-28。对象为当前工作区源码，包含此前整改后的未提交改动；不等同于已部署版本。当前批记录包含“批次执行、历史追溯、不合格评审、偏差管理、作废”五个页签，按业务目的归并为26类场景。详情、工作台和活跃订单中的关联办理入口一并纳入；后台残留接口单独说明，不冒充当前页面可操作功能。

此前报告的QA派单、偏差交接、重开补录派审、资料补齐、PQC驳回补正、整批作废工作台等缺口已有代码补齐。但不能认定全部业务问题解决：冻结告知覆盖、超期覆盖、审批的否决分支、单张记录作废仍存在缺口或残留。以下首先给出业务应然，再对照实现；不以已有任务类型反推业务必要性。

2、业务判断口径

| 类别 | 业务判断 | 接收对象与完成条件 |
|---|---|---|
| 通知 | 责任交给别人、处理条件发生实质变化、收到质量结果或已到约定期限，需要对方知情 | 发给明确责任人和实际受影响的人；避免向所有具备权限的人群发 |
| 审批 | 对放行、质量处置、失效或已确认记录变更作出授权决定 | 指定有权决定的人；应能表达同意和不同意/退回，保留决定及签名证据 |
| 个人工作台 | 本人仍有可执行且应执行的动作 | 责任人完成真实业务动作后关闭原任务；纯结果消息不新建待办 |
| 电子签名 | 证明谁在何时确认了什么 | 填写、上传、调查、验证也可能签名；是否签名不能单独决定是否属于审批 |

同一审批可以同时需要通知和工作台展示，但应引用同一正式任务。任务转派要更新责任和通知，不应复制第二张同业务审批。冻结时保留任务与阻塞原因，不能继续表现为可正常执行。

3、26类业务功能清单

表中“按条件”必须有实际交接、质量影响或约定时限作为依据，不能据此给每次查询、保存、上传都派通知。实现状态均指静态代码链路；证据编号见第7项。

| 编号 | 业务功能/所在页签 | 需要通知 | 需要审批 | 进入个人工作台 | 当前实现判断 |
|---|---|---|---|---|---|
| 01 | 批次列表、详情、源数据查看／批次执行 | 不需要 | 不需要 | 不需要 | 无须新增交接；这些属于查看入口，不能计为待办。E01 |
| 02 | 放行预检、记录/资料完整性检查／批次执行 | 按条件：发现需他人补齐的缺项时 | 检查本身不需要 | 按条件：实际补件责任人 | 有检查与显式资料指派；不能把检查失败本身算作已向所有缺项责任人派单。E02、E03 |
| 03 | 自动回填正式记录、建立证据关联／批次执行 | 成功不需要；失败且需人工处理时需要 | 不需要 | 仅需人工修复时 | 成功无需派单；本次未取得覆盖所有自动回填失败的专属责任交接证据，不判定异常闭环已完成。E01 |
| 04 | 普通填写、签名、退回修订／批次执行详情 | 派给他人或退回本人时需要 | 普通填写不需要；表单正式复核/批准节点按其职责执行 | 填写人、复核人或被退回人 | 已有工作任务、候选人和正式复核/批准任务；普通执行任务已从审批列表区分。已确认记录变更另见23。E04 |
| 05 | 来料、灭菌、成品等必需资料补齐／批次执行、活跃订单关联入口 | 需要：指定资料责任人 | 上传本身不需要 | 需要：资料责任人 | 已接通显式选人、类别/订单/轮次任务、通知、本人上传完成原任务。不能把“四类资料”误认作四次审批。E03 |
| 06 | 其他附件上传、普通文件整理、未确认内容保存／批次执行 | 直接自行操作不需要；受托补件时需要 | 不需要；已确认内容的受控变更除外 | 仅受托补件时需要 | 已支持OTHER_FILE资料责任交接；普通自行操作不应额外派单。受控变更见23。E03 |
| 07 | PQC生产放行申请及决定／批次执行关联入口 | 需要：审批人、申请人及后续责任人 | 需要：PQC放行 | 需要：正式PQC审批责任人 | 申请、候选人、签署、任务完成和批准结果通知已具备；正常驳回已有补正交接。E05 |
| 08 | 上市放行／批次执行关联入口 | 需要：上市放行人和结果接收人 | 需要：上市放行 | 需要：正式放行责任人 | 批准路径、工作台、结果通知已具备；当前活跃订单链路显式禁止驳回/撤回，负向决定未闭环。E06 |
| 09 | 上市放行责任交接、交接收回／批次执行、工作台 | 需要：新旧责任人 | 责任转移本身不新增质量审批 | 转移原待办 | 已有原任务责任更新及交接通知，不应复制一张放行审批。E06 |
| 10 | 放行批准、退回、撤回等结果及补正／批次执行 | 需要：申请人、被撤回任务的原责任人等实际相关人员 | 单纯告知不需要 | 结果不需要；补正需要 | PQC正常驳回补正、PQC批准及上市批准已接通；不能据此认定上市退回/撤回及所有旧分支结果通知均已实现。E05、E06 |
| 11 | 完工关闭、最终归档及归档异常／批次执行 | 自动成功不需要；人工归档/异常交接时需要 | 不应再加一次重复放行审批 | 仅实际归档责任人/异常处理人 | 活跃订单正式放行后进入归档状态；非该链路仍有人工归档任务。状态ARCHIVED不等于本次已验收PDF/A文件实际生成。E07 |
| 12 | 历史查询、反向追溯、签名与审批审计查看／历史追溯 | 不需要 | 不需要 | 不需要 | 属于只读追溯，不应增加待办。E01 |
| 13 | 归档预览、下载、打印／历史追溯及详情 | 不需要 | 不需要 | 不需要 | 无须因查看、导出或打印再建审批；真实归档生成与查看须分开。E01、E07 |
| 14 | 创建不合格并冻结批次/工单／不合格评审及关联入口 | 需要：QA及当前受影响的生产、PQC、放行责任人 | 创建/冻结本身不是另一项审批 | 需要：QA评审任务；受影响普通任务应显示阻塞 | QA任务及通知已接通，冻结状态已落库；创建链路只向指定QA派交接，未覆盖其他受影响责任人的冻结告知。普通任务已有阻塞显示。E08、E04 |
| 15 | QA让步、返工、作废处置／不合格评审 | 需要：原组长及真实后续责任人 | 需要：QA质量处置 | 需要：指定QA | 已有明确QA、权限/任务身份校验、签署、处置落库与原评审任务关闭。QA处置中的作废不应再叠加同目的独立作废审批。E08 |
| 16 | QA批准后继续生产、返工接手／不合格评审关联活跃订单 | 需要：接手组长及后续执行人 | 已批准返工的执行不重复审批 | 需要：真正接手/执行的人 | 已将QA_DECISION_HANDOFF作为行动任务投影；后继活跃订单继续生产交接，正式接手动作关闭原任务。E08、E04 |
| 17 | QA作废结果、作废记录查看／不合格评审、作废 | 结果应通知实际相关人；查看不需要 | 结果查看不审批 | 纯结果不需要 | QA作废后组长交接标为仅通知并完成，不派正常生产待办；作废页为只读台账。对全部受影响人的结果覆盖不能仅凭组长通知认定。E08、E01 |
| 18 | 发起偏差、指派接收调查人／偏差管理 | 需要：接收人 | 发起和陈述事实不属于质量审批 | 需要：接收/调查人 | 已有偏差接收工作任务及通知链路。E09 |
| 19 | 偏差调查、纠正、效果验证及执行签署／偏差管理 | 需要：下一阶段或新责任人 | 普通调查/纠正不审批；验证签名按事实确认职责判断 | 需要：调查、纠正、验证责任人 | 已有调查/纠正任务及按内容版本派发的执行签署任务；内容变更使旧任务失效。不能把所有签名都归为质量审批。E09 |
| 20 | 偏差QA/质量负责人审核、重大偏差质量决定及关闭／偏差管理 | 需要：指定审核/关闭人 | 需要：质量决定；关闭应核对执行证据 | 需要：当前审核/关闭责任人 | 已接通质量审核任务和最终关闭任务、版本与签名证据。固定五/六类签署是否均必需，仍需正式偏差分级规则支持，不能直接认定全部必要。E09 |
| 21 | 重大偏差转不合格评审／偏差管理→不合格评审 | 需要：QA及受影响责任人 | 转单不应再造一层同目的审批 | 复用新建NCR的QA任务 | 已接通明确QA选人与NCR派单；冻结告知覆盖仍同14。E09、E08 |
| 22 | 独立作废申请、决定、撤回／批次执行→作废台账 | 需要：审批责任人、申请人、实际受影响人 | 需要：独立作废决定 | 需要：原正式审批任务 | 整批作废已有正式BPM及BPM_EDHR_VOID工作台来源，复用原Flowable任务。单张已确认记录作废仍保留旧后台接口，无前端调用、无正式任务/通知接入；不能合并判为全部达到。E10、E11 |
| 23 | 已确认批次/记录重开、补录及批准后修订／详情、变更页 | 需要：指定审批人、申请人和实际修订人 | 需要：记录变更决定 | 需要：审批及批准后的实际修订任务 | 明确选人、禁止自审、正式派审、批准通知及修订任务已接通；当前变更页与控制器只有批准，没有重开/补录的驳回、退回决定链路。E11 |
| 24 | 按规则跳过可选项目、推进下一工序／批次执行 | 跳过本身不需要；形成下一人责任时需要 | 正式规则允许的可选项不额外审批；不可绕过必填/质量门禁 | 下一项实际处理任务，不建“已跳过待办” | 不能仅由“存在跳过按钮”判断业务允许；本次不将所有模板/工序允许跳过范围判定为已验收，也无依据要求增加统一跳过审批。 |
| 25 | 演练预检、模拟与金手指辅助／批次执行测试模式 | 正常生产不需要新增此类通知 | 不能替代正式质量审批 | 正常生产不应以模拟任务替代正式任务 | 旧批量直接作废入口已删除；仍有testModeEnabled下的阶段模拟。它们是测试辅助残留，不是当前主业务必经步骤，不因“存在”就全部删除。E12 |
| 26 | 超期提醒、责任变更／各业务链路 | 已约定时限且超期/换人时需要 | 提醒/转派不新增质量审批 | 使用原任务，换人后更新归属 | 原通用任务有到期扫描和通知；新偏差审核、变更、补件、补正创建时未设置dueTime，QA交接模型也无该期限；候选人任务的超期发送只取assigneeUserId。尚不能宣称全面覆盖。E13 |

4、未达到与尚未证实的事项

| 问题 | 业务影响 | 核对到的代码事实 | 建议处理边界 |
|---|---|---|---|
| F01 冻结告知只覆盖QA派单 | 生产、PQC或放行责任人可能仍不知道停止原因 | NCR创建派QA后直接更新批次/工单冻结；qaReviewCreated仅以指定QA构造接收人，工单冻结是Mapper直接更新 | 按实际受冻结影响的当前任务责任快照告知；不新建一批重复执行待办，不向全部角色群发 |
| F02 新任务超期覆盖不足 | 有待办，但无期限时不会被现有扫描选中 | selectDueActiveTasks只选择dueTime非空且到期的TODO/DOING；上述四类新任务创建未设期限，QA交接查询due_at为NULL | 先明确哪些节点有业务时限，再接入期限和提醒；无时限要求不能擅自编造SLA |
| F03 候选人任务的超期接收人不完整 | 只有候选快照、没有个人受理人的任务没有明确通知对象 | sendOverdueNotify直接setUserId(task.getAssigneeUserId())，未展开候选快照 | 按正式候选责任规则确定接收人，不能把空assignee算作通知已送达 |
| F04 单张记录作废旧接口未闭环 | 后台可以提交/批准作废，但当前页面没有正式交接入口 | requestVoidExecution仅创建SUBMITTED事件；approveVoidExecution签名后使记录失效；两者没有变更任务派发/完成调用；前端只保留API导出，没有Vue调用 | 若保留独立单记录作废，应接上受控流程；若当前业务不支持，应撤除失联能力。两种选择不能由代码存在与否替代业务决定 |
| F05 重开、补录只有批准 | 审批人发现证据不足时只能挂起，无法形成不同意/补正的正式结果 | RecordChangePage仅request/submit/approve，签名按钮为“签名批准”；控制器无重开/补录驳回/退回接口；任务服务只在EFFECTIVE后完成 | 增加明确否决或退回补正决定、通知及任务终态；不能以系统校验报错替代业务否决 |
| F06 活跃订单上市放行禁止负向决定 | 上市放行人发现问题时，没有该节点的正式驳回/撤回路径 | MesProductionReleaseManagerApprovalServiceImpl.assertActionSupported对关联活跃订单的REJECT/WITHDRAW显式报UNSUPPORTED_RELEASE_ACTION | 质量审批至少需明确“不予放行”的业务路径；可使用受控退回或NCR，但必须有真实可达入口及责任交接。撤回范围需按状态/权限定义 |
| F07 异常处理和实际交付证据未覆盖 | 自动回填失败、归档生成失败是否真正交给责任人，及通知是否实际送达，不能从正常成功路径推定 | 本次仅核对源码；归档状态与真实文件、通知模板/投递、调度启用是不同事实 | 对约定需人工处理的异常逐项核对；以部署后真实页面和投递回执确认实际达到，不将静态结论升级为环境验收 |

F04描述的是保留旧能力的闭环问题，不把当前“作废”只读台账误报为缺少提交按钮。F05/F06是本次业务复核明确指出的否决分支不足；此前“主链已接通”的结论须限定为其已实现的批准路径。

5、不需要却写了、已清理、不能直接删除的内容

| 分类 | 内容 | 当前结论 |
|---|---|---|
| 已纠正的过度审批归类 | 普通填写、上传、调查、归档任务混入审批候选列表 | 当前审批查询已限制正式审批类型；保留有正式签名节点身份的候选FILL。不能再复述为当前仍全部混入 |
| 已删除的多余旁路 | 旧四类上传初始化、人工关闭入口、金手指批量直接作废 | 已有本地删除改动；不再把这些旧实现算作主线已完成能力，也不应恢复为主线必经步骤 |
| 当前仍保留的未接通能力 | 单张记录VOID后端申请/批准接口与前端API导出 | 当前无Vue调用、无正式派单闭环。属于待业务确认保留或移除的残留，不等同于整批正式作废多余 |
| 不是多余审批 | 保存、上传、调查、验证时的电子签名与审计 | 签名和审计可有独立业务价值，不能仅因“不需要审批”就删除证据 |
| 不能认定全部多余 | 非活跃订单链路的人工归档任务、真实PDF/A生成 | 当前活跃订单主线无需重复人工关闭，但真实归档证据仍必要；不能删掉所有归档代码 |
| 测试辅助，不是正式主线 | testModeEnabled下阶段模拟、演练预检 | 不纳入正式质量审批和生产待办统计；是否保留测试工具与正式审批规则是两件事 |
| 规则不足，不能擅删 | 普通/重大偏差固定五/六类签署 | 需以偏差等级、质量授权和组织职责说明哪些节点必须；本次没有支持“全部必要”或“全部删除”的正式业务依据 |
| 不应补写的额外行为 | 查询、预览、下载、打印、保存草稿、自动回填成功通知；纯结果告知待办；同一事项第二张审批 | 本次没有发现足以逐项认定这些额外行为普遍存在的完整调用链，不为凑问题而推定；后续也不应因此统一加消息/审批/待办 |

6、选人字段的业务用途

QA评审人用于“不合格评审”的正式处置责任，包括批次驳回、不合格新建及重大偏差转NCR等入口。它不等于所有上传文件的审核人，也不是任意具备QA权限的人都自动收到。

记录变更审批人用于重开、补录的本次独立审批。申请人不得自审，后台校验租户、启用状态和权限。整批独立作废走已配置BPM；单张记录作废旧接口没有因此自动获得同样的派审链路。

7、主要代码依据

以下路径相对于仓库根目录。核对时同时追踪前端调用、控制器、服务、任务持久化和工作台读取；链接锚点对应本次读取时源码，后续并行修改可能移动行号。

| 证据 | 入口与下游链条 |
|---|---|
| E01 | [五页签定义](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue:8)；BatchExecutionListPage → batchExecution API → MesProEdhrBatchExecutionController/Service；BatchExecutionActiveOrderDetailPage → ActiveOrderSubmissionDetailPanel；历史与BatchVoidedPage为查询/详情入口 |
| E02 | BatchIntegrityCheck.vue → integrity API → 批次完整性控制器/服务；检查结果与实际资料派单分别核对，不把检查消息当作通知投递 |
| E03 | DossierResponsibilityAssignment.vue → dossierCompletion API → MesActiveOrderDossierFileController → [MesActiveOrderDossierCompletionService.assign](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierCompletionService.java:59) → mes_pro_edhr_work_task、通知；DossierCompletionPage → uploadActiveOrderDossierFile → 文件服务 → completeUploadedCategory → 原任务完成 |
| E04 | [审批任务类型查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java:22) → MesProEdhrApprovalTaskAdapter；MesEdhrWorkbenchSource → [个人待办普通任务与QA交接联合查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml:13) → 工作台导航；冻结任务返回阻塞原因 |
| E05 | 生产放行页面/补正组件 → MesPqcProductionReleaseServiceImpl → 正式申请/PQC审批任务 → MesPqcReleaseCorrectionService → 原申请人补正任务与新轮次；MesReleaseOutcomeNotificationService → 投递记录 → 提交后派发/重试 |
| E06 | 上市放行页面/MarketReleaseHandover → MesProEdhrReleaseServiceImpl、MesProductionReleaseManagerApprovalServiceImpl、交接服务 → 原放行任务/正式决定 → 结果通知；[活跃订单上市放行禁止驳回/撤回](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java:285)；旧通用finalizeReject/finalizeWithdraw有状态/任务更新，但不能反证当前受管链路支持 |
| E07 | BatchExecutionDetailPage归档入口 → 归档API/服务；[closeBatchAfterFinalRelease](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:1088) 区分活跃订单归档状态与非该链路createArchiveTaskAfterBatchClose |
| E08 | 不合格页面及各创建入口 → MesProEdhrNonconformanceReviewController/ServiceImpl → review/签名/冻结落库 → [qaReviewCreated](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java:297) → 指定QA任务、delivery.schedule；处置 → qaDisposed → QA_DECISION_HANDOFF → 工作台；requireWorkOrderUpdate直接Mapper冻结，没有受影响人员告知 |
| E09 | DeviationDetail.vue → 偏差、处理、转NCR接口 → MesProEdhrDeviationHandlingServiceImpl、MesProEdhrDeviationReviewTaskService → 调查/纠正及DEVIATION_SIGN/APPROVE/CLOSE任务 → 通知/工作台；[版本约束派单](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationReviewTaskService.java:58) |
| E10 | BatchExecutionListPage正式整批作废 → RecordChangeController → requestVoidBatchExecution → BusinessApprovalOrchestrator → 正式BPM；[EdhrVoidWorkbenchSource](E:/IntRuoyi/IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/workbench/EdhrVoidWorkbenchSource.java:11) → 原Flowable任务；ProjectionEdhrVoidEngineListener使工作台投影跟随原流程状态 |
| E11 | [单记录作废申请](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java:124)、approveVoidExecution及toResp；RecordChangePage.vue仅重开/补录与“签名批准” → change.ts → 控制器/重开补录服务 → [MesEdhrRecordChangeTaskService](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesEdhrRecordChangeTaskService.java:78)；前端全src搜索单记录VOID仅命中两个API导出 |
| E12 | 本地删除的旧关闭/金手指批量作废控制器方法、VO及对应前端调用；[保留的测试模式模拟入口](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue:34)，与正式主线分开 |
| E13 | MesEdhrWorkTaskOverdueJob → processOverdueTasksWithSummary → [selectDueActiveTasks](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java:201) → [sendOverdueNotify](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java:3145)；对照四类新任务assign/create方法及MesActiveOrderHandoffTaskDO/工作台due_at |

8、验证范围与历史结论

本次为业务分析和当前代码静态复核，只新增本报告，没有修改业务代码、数据库或发送通知，也未新跑运行环境验收。此前本地整改的测试结果及边界见[整改复核报告](E:/IntRuoyi/docs/reviews/edhr-batch-implementation-review-20261028.md)，其中917项后端定向验证不能用作本报告所有业务分支均已通过的证明。

“代码链路具备”“通知模板/迁移已应用”“真实账号可从页面办理”“消息实际送达”是不同层级的证据。当前报告只对已读调用链作静态结论；整套新版本部署、工作台来源激活、调度和真实页面业务验收状态未在本次重新验证。
