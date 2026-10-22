# eDHR 主链连续运行 BUG 静态审查报告

日期：2026-09-28。原始审查范围：工作区源码中的实际页面、接口、服务与持久化状态。下文保存修复前的静态根因，不表示问题仍全部存在。用户后续授权08～15修复，最新进度、实际RED/GREEN及剩余问题见 [修复验证报告](../../doc/tasks/20260928-edhr-flow-08-15-fixes/verification-report.md)；目前尚未最终放行，也未进行真实业务联调。

## 结论

本轮按完整业务链及异常恢复分支核对，确认 **8 项会阻止某个合法业务步骤继续执行的问题**：6 项优先修复，2 项属于特定操作组合下的阻塞。其中 EDHR-FLOW-08 为前轮已有发现，本轮新增 EDHR-FLOW-09 至 EDHR-FLOW-15。

最影响连续生产的是：**同一生产组长只要有一张工单已完工、仍在等待放行，其他工单的生产复核也会被锁住。** 其他主要断点集中在 PQC 退回、更正、不合格评审和 QA 返工，以及分物料报工。

这不表示所有订单都会失败，也不表示已证明系统只有这 8 个 BUG。当前结论覆盖下表所列代码路径；环境、权限实际配置、历史数据和真实多人并发仍未运行验证。

## 全部阻塞问题总表

| 优先级 | 编号 | 员工的正常操作/触发条件 | 卡住的位置 | 现有操作能否恢复 |
| --- | --- | --- | --- | --- |
| P1 | EDHR-FLOW-15 | 同组长的 A 工单已完工等待放行，继续复核 B 工单 | B 的生产复核/分配被 A 的状态一起锁住 | A 完成上市放行并关闭后可解除；等待期间其他单受影响 |
| P1 | EDHR-FLOW-14 | 同一工序有多个输出物料，分别提交各物料完成量 | 组长确认时把不同物料相加，误报超过允许产量 | 重试不解决；不能用虚增超产比例或改造真实记录代替修复 |
| P1 | EDHR-FLOW-09 | PQC 组长将一条检验记录判为“不正确”，随后想纠正再确认 | 已拒绝的复核不能重开，任务也不能重新提交，检验进度无法达到 100% | 未找到保留原记录的正常重新确认路径 |
| P1 | EDHR-FLOW-10 | PQC 记录已经确认并汇集，尚未放行，发现录错需要更正 | 更正再次调用只接受“待汇集”的方法，事务回滚 | 同样的更正重试仍失败；只阻塞需更正的情形 |
| P1 | EDHR-FLOW-11 | 完工前，PQC 组长从检验记录发起不合格评审 | 页面只传活跃订单，后端却要求完工后才产生的批次执行 | 该前端入口无法正常受理；不能要求先批准问题数据再完工 |
| P1 | EDHR-FLOW-08 | 工单完工进入放行后，QA 判定返工 | 一线生产仍被“已完工”及旧申请锁阻止，无法进入下一轮 | 普通重建也受旧申请限制；缺少完整返工恢复路径 |
| P2 | EDHR-FLOW-12 | 工单正常升版后，把新活跃订单移除，再重新加入 | 新旧版本都算可复用历史，报“历史不唯一” | 移除后未找到普通重新加入的恢复路径；不代表升版本身失败 |
| P2 | EDHR-FLOW-13 | 两名生产组长同时关联并启用同一正式员工 | 人员配置能保存，但一线入口要求唯一组长，员工不能正常使用 | 可由组长停用多余关联恢复；应在配置时避免进入该状态 |

P1/P2 按本次“前期宽松、先跑通循环”的业务影响排序；P2 在这里包含可通过现有操作恢复、或仅在特定扩展操作中触发的 BUG，不等同于没有影响。

## 逐项证据、边界与修复建议

### EDHR-FLOW-15：一张待放行工单锁住同组其他工单

- 例子：生产组长同时负责 A、B 两张活跃工单。A 已完成并申请 PQC 放行；B 的员工继续报工，组长确认 B 时却被提示订单已锁定。
- 页面确认生产报工调用 `/submission/allocation/confirm`；Controller 实际委托 `MesReportAllocationCommandService.save`，不是未使用的旧方法。[页面调用](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:8894)；[接口委托](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:704)。
- `save` 读取该组长所有 `activeStatus=ACTIVE` 的订单，把全部订单 ID 放进 `releaseCandidates` 后统一检查。只要其中一张 `businessStatus != ACTIVE`，或已有放行申请，就抛出锁定错误，尚未进入 B 自身的确认处理。[扩大检查范围](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:253)；[锁定判断](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:882)。
- A 完工仅把 `businessStatus` 改为 `COMPLETED`，`activeStatus` 仍为 `ACTIVE`，因此仍在上述查询中。上市放行收尾才将其改为 `CLOSED/RELEASED`。[查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:47)；[完工](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:122)；[放行关闭](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:342)。
- 影响边界：锁住的是该组长其他工单的生产复核/分配；不能笼统说其他员工所有页面都不能提交。A 正常完成上市放行后，该状态检查不再因 A 阻止 B；若 A 因返工无法结束，会放大 EDHR-FLOW-08 的影响。
- 最小修复方向：仅检查当前报工已有分配与本次目标分配涉及的订单，保留这些订单真正已完工/已放行时的锁；不要把整个候选列表当作本次写入范围。
- 建议验收（未执行）：Given 同组长有 A、B 两单；When A 完工等待 PQC 或上市放行，B 提交并确认生产报工；Then B 能继续，A 自身仍禁止普通报工；再覆盖 A 进入 QA 评审的情况。

### EDHR-FLOW-14：多个输出物料分次报工被误算为超产

- 例子：某工序计划量 100，要求输出物料甲、乙各完成 100，冻结超产比例为 0%。员工先提交甲 100，再提交乙 100，这是前端和提交校验允许的操作；组长确认时却被当成产量 200，超过上限 100。
- 页面明确要求“至少填写一个输出物料”，只发送本次填写的物料；后端也允许冻结物料集合的非空子集，并把本次物料数量最小值作为报工分配量。[前端允许分次](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:4804)；[构造提交明细](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:5753)；[提交校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackMaterialSubmissionValidator.java:29)。
- 初始分配按每次提交的单个进度量落库，尚未带正式复核。[初始分配调用](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:171)；[初始分配写入](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:167)。
- 后续确认按订单/工序汇总其他报工分配量，不按输出物料区分；再用“其他量 + 本次量”与计划量乘超产比例比较。即使分配没变，也先经过此检查，无法靠“原样确认”避开。[汇总与门禁](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:483)；[检查先于复核](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:278)；[上限计算](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java:786)。
- 订单进度本身按各物料累计量取最小值，甲 100、乙 100 应为进度 100，说明提交、进度与确认门禁的数量口径不一致。[正式进度计算](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesOutputMaterialProgressCalculator.java:45)。完工回填还要求正式生产复核事实，因此不能把页面进度到 100% 当作已经绕过该缺陷。
- 影响边界：多输出物料分次或不均衡报工，且相加后的分配量超过冻结上限时触发；单输出物料或每次同时等量提交所有物料不由此例证明失败。两物料各 100 的例子在超产比例低于 100% 时会触发。
- 最小修复方向：按物料实际完成量/实际目标检查超产，并与进度、分配的定义一致；不能简单把超产比例放大，也不应强迫员工把分别发生的报工合成虚假记录。
- 建议验收（未执行）：Given 两个输出物料各需 100；When 甲乙分两次各报 100 并分别复核；Then 均可确认、进度为 100、允许完工；另覆盖真实某物料超上限、部分分配和同次多物料提交。

### EDHR-FLOW-09：PQC 判“不正确”后没有正常重新确认路径

- 页面确实提供“正确/不正确”复核选择，PQC 分支提交 `APPROVED/REJECTED`。[界面选项](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:3449)；[实际提交](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:8927)。
- 后端发现已有复核就只接受完全一致的幂等回放；改成批准会报终态复核已存在。只有批准分支会汇集并确认 PQC 任务，拒绝后任务仍是 `SUBMITTED`。[复核服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java:81)。
- 一线对已提交任务只能原样回放，换内容重提会冲突；任务选项只暴露待提交任务。更正服务虽然可改未汇集数据，但不会重新打开已拒绝的复核，也不会让任务重新进入提交周期。[提交状态校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java:1318)；[更正正式数据](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:406)。
- 完工的检验进度只计 `CONFIRMED`，因此这条必需任务一直达不到完成条件。[完工进度](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionProgressPortImpl.java:93)。模拟任务中的重置方法不属于员工正常恢复入口。
- 最小修复方向：增加保留旧复核证据的“更正/重新提交/重新复核”周期，并同步任务、事件和汇集状态；不要删除拒绝记录或自动当作批准。
- 建议验收（未执行）：Given PQC 提交被判不正确；When 更正后重新提交并被组长确认；Then 旧拒绝可追溯、新复核生效、检验达到 100%，可继续完工放行；覆盖连续两次退回。

### EDHR-FLOW-10：已确认 PQC 记录的更正必然撞上“已汇集”校验

- 页面对未放行 PQC 记录开放更正，后端也允许任务为 `SUBMITTED` 或 `CONFIRMED`，因此确认后更正是暴露给用户的能力。[页面入口](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:4911)；[更正任务校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:195)。
- `correct` 更新正式检验表后，发现原记录为 `AGGREGATED`，再次调用 `aggregateApprovedPqcSubmission`；更新正式表不会把汇集状态改回待汇集。[更正调用链](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:138)；[正式表更新](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:406)。
- 被调用方法只接受 `PENDING`，对 `AGGREGATED` 立即抛出已汇集错误，导致更正事务回滚。[汇集入口限制](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceImpl.java:60)。
- 影响边界：未确认、尚未汇集的普通更正不因本项失败；已确认记录没有修改需求时也能继续。出现真实录入错误后，员工无法通过已提供的更正功能把记录改正确再推进。
- 最小修复方向：提供“按修订替换/重新生成汇集事实”的事务路径，保留更正审计；不能直接复用只负责首次汇集的方法，也不能忽略旧汇集明细。
- 建议验收（未执行）：Given 已确认、未放行的 PQC 记录；When 修改样本值并签名；Then 更正与汇集明细一致落库、保留旧版本、后续完工/放行读取新事实；再次更正也可完成。

### EDHR-FLOW-11：完工前不合格评审依赖完工后的批次

- PQC 组长在未确认检验记录上可点击不合格评审，跳转时传活跃订单 ID；评审页面提交的也是 `activeOrderId` 与原因，没有传原 PQC 提交身份。[按钮及跳转](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:8815)；[评审创建请求](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue:516)。
- 后端见 `activeOrderId` 就进入 `createFromActiveOrder`，立即要求找到唯一的活跃订单批次执行来源；没有则报来源无效。[分支入口](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:131)；[强制寻找批次](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:208)。
- 当前主链要先双 100% 完工，再 `openOrCreate` 批次执行，最后生成放行申请；加入活跃工单时不会提前创建这一批次。[完工与创建顺序](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.java:97)；[双 100% 校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java:143)。
- 因而首次执行、尚未完工的活跃工单从该入口发起评审会失败。后端另有以 `PQC_SUBMISSION` 为来源的创建分支，但当前页面没有走它，不能把“另一个接口分支存在”当作员工入口已经贯通。
- 最小修复方向：完工前以实际 PQC 提交/活跃订单为可验证来源创建评审，或使页面接入已有正确来源分支；在下游批次产生后建立关联，不强迫先把问题检验确认通过。
- 建议验收（未执行）：Given 新活跃订单尚未完工、PQC 发现问题；When 从该记录发起不合格评审；Then 可正常创建并由 QA 处置，随后按处置结果继续对应业务路径。

### EDHR-FLOW-08：放行阶段 QA 判返工后不能恢复生产

- 完工将活跃订单写为 `COMPLETED`；QA 返工只恢复批次状态、解除工单临时冻结、关闭旧申请，没有恢复活跃订单业务状态或建立新返工完工周期。[完工状态](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:122)；[QA 处置](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java:320)。
- 一线生产要求订单业务状态为 `ACTIVE`；即使只改回该状态，历史放行申请锁也没有排除返工关闭的申请。[生产授权](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java:42)；[旧申请锁](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationReleaseStateService.java:94)。
- 列表却会忽略返工关闭的旧申请，申请生成也支持返工周期键，形成页面“返回主流程”与真正写入口不一致。普通重建仍要求不存在历史申请，不能作为正常恢复方法。[重建限制](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:1622)；[列表过滤](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:2685)。
- 最小修复方向：统一返工后的状态、历史申请锁、提交事实和新完工回执周期，保留旧周期证据。现有完工服务会重用旧回执，修复不能仅放开报工而不处理重新完工时的源摘要冲突。[回执复用](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java:68)。
- 建议验收（未执行）：Given 已完工并处于 PQC/上市放行的订单；When QA 判返工，员工重新录入并再次完工；Then 新周期可重新申请、生产放行、上市放行且旧周期可追溯；覆盖二次返工。让步放行、作废需分别保持自身语义。

### EDHR-FLOW-12：正常升版产生的历史阻止移除后重新加入

- 升版批准时把旧订单改成 `REMOVED/VERSION_UPGRADED`，保留旧记录，并强制创建新版本活跃订单。[升版处理](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderVersionUpgradeServiceImpl.java:294)；[旧记录状态](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:322)。
- 普通重新加入查询同工单的 `ACTIVE/REMOVED` 历史，不排除已经被升版替代的旧记录；只要多于一条就报历史不唯一。因此升版后的新订单再正常移除，就无法通过普通入口重新加入。[历史查询](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java:147)；[重新加入判断](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:2272)。
- 影响边界：正常升版完成后，新订单继续生产不必触发本项；问题是升版与“移除/重新加入”两个正式功能组合后无法继续，候选列表也会将它判为历史不唯一。
- 最小修复方向：区分已被替代的版本历史与当前可恢复记录，按明确的版本关系选择目标，保留全部历史；不能删除旧订单证明。
- 建议验收（未执行）：Given 同工单已完成一次或多次路线/QA 升版；When 当前新订单移除并重新加入；Then 恢复正确版本、不误用旧快照、历史仍可追溯。

### EDHR-FLOW-13：生产人员允许重复归属，但一线入口不接受

- 正式员工关联只检查“当前组长 + 系统用户”是否重复，新增关联默认启用；启用操作也不排除该员工在其他组长下已有启用关联。[新增关联](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java:156)；[重复检查](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderRuntimeConfigServiceImpl.java:558)。
- 一线解析登录用户的负责组长时，要求启用关系对应的组长数量恰好为 1；通过班组入口选择员工也使用唯一负责组长检查。两名组长都能保存该员工，最后却让员工入口报上下文无效。[登录归属解析](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java:127)；[实际员工校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineDeviceAccountContextServiceImpl.java:360)。
- 数据表的唯一键包含组长维度，并没有用数据库约束拒绝这种跨组长重复关系。[表结构](E:/IntRuoyi/IntRuoyiBackend/sql/mysql/20260731_mes_process_pool_team_leader_p1_runtime_config.sql:23)。
- 恢复方式：停用多余组长关系，保留唯一启用归属。此项可恢复，但配置时成功、使用时失败仍属于上下游逻辑矛盾；与 EDHR-FLOW-06 的 PQC 人员问题不能混为同一个已证明阻塞。
- 最小修复方向：按一线已有唯一归属合同，在关联/重新启用时校验或提供明确转组操作，不扩大权限门槛；若业务确需多组共享，则需一线显式选择有效组长范围。
- 建议验收（未执行）：Given 员工已在 A 组启用；When B 组尝试关联或重新启用旧关系；Then 不会形成一线无法解释的双重启用关系，完成转组后员工能正常选择工序并提交。

## 全链路覆盖与排除结论

| 环节 | 已核对的实际内容 | 本轮结果 |
| --- | --- | --- |
| ERP 同步 | 更新字段、数量/批号与执行快照、已有完工证据 | EDHR-FLOW-07 保持条件性风险；不把每次 ERP 更新都判为必然阻塞 |
| 路线、QA、正式绑定 | 活跃订单冻结、逐工序批记录绑定、专用/通用 QA 任务来源、表单槽位独立链 | 缺生效配置和正式绑定属于运行前提；不混为代码 BUG |
| 组长配置人员 | 生产/PQC 人员新增、启停、唯一归属及一线解析 | EDHR-FLOW-13；PQC 双重归属仍按 EDHR-FLOW-06 保留完善项 |
| 活跃工单 | 加入、移除、重建、升版、多单连续处理 | EDHR-FLOW-12、15；升版后正常继续与移除重加区分 |
| 一线生产与组长复核 | 提交、初始分配、更正、数量与超产、复核写入 | EDHR-FLOW-14、15；不以页面进度代替正式复核事实 |
| 一线 PQC 与组长复核 | 待提交/已提交/已确认、批准/不正确、更正、汇集、进度 | EDHR-FLOW-09、10 |
| 不合格、完工与返工 | 完工前评审、双 100%、回填、完工回执、放行阶段返工及第二周期 | EDHR-FLOW-11、08 |
| PQC 生产放行与上市放行 | 页面真实分支、事务、角色候选、签名、幂等、状态/版本、退回事务 | 正常重试存在回放；旧 REJECTED/WITHDRAWN 事务不会单独占住新申请，不重复误报 |
| 上传与历史追溯 | 上传时机、放行后只读、历史列表条件和详情 | 01/04/05 为完善项；03 撤回，不新增 PDF 或人工归档要求 |

重点排除：缺管理者代表候选会使 PQC 批准事务失败，但属于角色配置前提；放行申请/完工/批次创建处于事务链，不能凭步骤多就断言半成功；已有幂等回放不能等同于支持变更内容重新提交。PQC 确认后的更正、拒绝后的重新提交是两个不同状态问题，故分别列 09、10。

## 其他待完善、风险与已撤回记录

以下 6 项继续保存在[待完善列表](E:/IntRuoyi/docs/bugs/20260928-edhr-flow-improvement-backlog.md)，不混入上述 8 个确定的运行阻塞问题。

| 编号 | 内容 | 当前处理 |
| --- | --- | --- |
| EDHR-FLOW-01 | 允许缺资料上市放行，但之后不能补传 | 按先上传后放行可运行；是否增加受记录的补传另行决定，不加严门槛 |
| EDHR-FLOW-02 | 六项业务检查直接显示通过 | 检查结果真实性完善，不是当前卡点 |
| EDHR-FLOW-04 | 带申请 ID 的资料操作缺少订单归属校验 | 同租户业务范围权限完善 |
| EDHR-FLOW-05 | 审批快照未绑定资料文件版本 | 文件证据一致性完善 |
| EDHR-FLOW-06 | PQC 人员重新启用可能产生双重归属 | 已发现配置矛盾，但未证明会像生产员工一样直接卡住入口 |
| EDHR-FLOW-07 | ERP 字段更新与冻结执行证据可能不一致 | 条件性风险，需具体变更数据验证；部分产品变更也可能生成新的来源键，不能一概认为覆盖旧单 |

EDHR-FLOW-03“自动归档没有真正完成”已撤回。当前上市放行后系统留存并可历史追溯，不需要人工归档负责人；没有把自动生成 PDF 追加为本阶段要求。

## 建议处理顺序与验证边界

1. 先修 15、14：直接影响多张工单连续处理、多个物料正常分次报工。
2. 接着修 09、10、11：把 PQC 的退回、更正和不合格评审恢复通路接通。
3. 修 08 时一起覆盖新返工周期的提交、复核、完工回执和重新放行，防止只解开入口又卡在下一步。
4. 再处理 12、13：完善升版后的恢复，以及可由现有人员操作解除的配置冲突。

以上修复方向仅为报告建议，尚未实施或验收。推荐的 Given/When/Then 场景也未运行。文档结构与源码引用校验通过，只能证明报告可定位，不代表业务测试通过。本轮未运行构建、单元测试、E2E，未访问或写入数据库、未启停服务、未提交或推送 Git，按用户要求跳过 AOCI。

最终更新：上文保留的冻结快照、动态签名预览、legacy容量三类问题均已修复；remaining-green13.log 607/607通过。历史静态描述与失败证据保留用于追溯，当前状态以修复验证报告为准。未部署、未做真实E2E。
