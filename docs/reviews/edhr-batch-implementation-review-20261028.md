# eDHR批记录整改与主审复核（2026-10-28）

1、本轮范围：原[26类业务场景分析](edhr-batch-notification-approval-workbench-20261027.md)中确认的通知、审批和个人工作台缺口。用户指定的实施子Agent使用 `gpt-6.1-sol / high`；主Agent负责沿入口、权限、服务、落库、通知、审批及工作台逐节点复核，并集中处理回归失败。下表是本轮本地代码状态，不能当作已经部署或真实送达证明。

| 原问题 | 整改结果 | 实现依据 |
|---|---|---|
| QA不合格创建后无人接单 | 各创建入口明确选择QA评审人；创建正式QA交接和通知，完成处置后关闭原任务。历史待评审单通过显式指派补齐 | `MesProEdhrNonconformanceReviewServiceImpl`、`MesActiveOrderHandoffService`、不合格/重大偏差/批次驳回入口 |
| 偏差调查、纠正和质量签署缺少责任流转 | 调查、纠正和执行签署进入责任人待办；QA/质量节点进入审批；内容变更撤销旧版本任务；全部签署后派关闭任务 | `MesProEdhrDeviationReviewTaskService`、`MesProEdhrDeviationHandlingServiceImpl`、`DeviationDetail.vue` |
| QA让步、返工后只有消息 | 原生产组长的真实接手交接进入工作台；作废结果只告知，不派正常生产待办 | `MesActiveOrderHandoffService`、`MesEdhrWorkbenchSource`、MES工作台查询与导出 |
| 重开、补录无审批交接 | 申请时明确选择独立审批人；审批待办、通知和正式签名证据接通；批准后按原填写责任快照派修订任务；补录保留原记录并建立新版本 | `MesEdhrRecordChangeTaskService`、`MesProEdhrRecordChangeServiceImpl`、`MesEdhrRecordChangeEditGuard`、`MesEdhrSupplementRevisionService` |
| 活跃订单资料责任无闭环 | 当前生产组长按四类资料明确指派/转派；责任人收到通知及待办，本人上传当前订单、类别、轮次的资料后完成原任务 | `MesActiveOrderDossierCompletionService`、`MesActiveOrderDossierFileService`、`DossierResponsibilityAssignment.vue`、`DossierCompletionPage.vue` |
| 放行驳回缺少补正交接 | PQC正常驳回派给原申请人且须为当前生产组长；结果与补正指派合并通知。补正原因及有效新附件提交后建立新正式放行轮次，保留原驳回事实 | `MesPqcReleaseCorrectionService`、`MesPqcProductionReleaseServiceImpl`、`PqcReleaseCorrection.vue` |
| 放行结果未通知 | 生产放行、上市放行结果复用正式通知投递、审计及重试；纯结果不新建审批 | `MesReleaseOutcomeNotificationService`、PQC与上市放行业务服务 |
| 审批中心混入普通执行任务 | 限定正式审批类型；保留有非空签名节点身份的候选FILL。普通填写、补件、调查及归档不混入待审批 | `MesProEdhrWorkTaskMapper`、`MesProEdhrApprovalTaskAdapter` |
| 冻结/失效任务显示可办理 | 冻结的普通任务显示阻塞原因并禁用跳转；资料与补正核对真实订单、轮次、冻结、作废及责任归属；受控记录变更审批保留合法历史状态入口 | 三个 `MesOpen*TaskVisibility` 类、工作台查询/导出/导航 |
| 旧上传、人工关闭与直接作废旁路 | 删除无调用的旧四类上传初始化、人工关闭入口和金手指批量直接作废；保留真实归档证据及正式批准后的作废生效执行器 | 批次控制器、服务、VO、前端入口与PQC阶段初始化 |
| 独立作废未进入个人工作台 | 新增专用只读 `BPM_EDHR_VOID` 来源，引用原Flowable任务及原流程详情；校验正式申请、作废事件、流程及租户，不创建第二张审批 | BPM `workbench` 包、`ProjectionEdhrVoidEngineListener`、前端BPM来源注册 |
| 既有PQC/上市放行及责任交接 | 继续使用原任务与候选快照；交接转移原责任，不复制第二轮放行审批 | PQC、管理者代表阶段、上市放行交接及正式结果读取器 |

2、两类选人字段的用途已明确：QA评审人用于创建/指派不合格QA评审；记录变更审批人用于重开、补录审批。后台验证当前租户、启用状态与业务权限；记录变更禁止申请人自审。没有按创建者、当前登录人或猜测角色自动代选。

3、业务上不需要新增审批/待办的查询、追溯、预览、下载、打印、保存草稿、自动回填成功及普通文件整理，保持无额外派单。偏差五/六类签署缺少可删除的业务依据，因此保留层级并区分执行确认和质量审批。已有超期/转派机制继续使用；调度配置和实际送达不据静态代码宣称通过。

4、主审发现并修正的关键问题：作废工作台原先缺流程详情与审批权限交集，现同时要求三项既有权限，并交集S4授权有效期；新增查询限制10秒；跨Mapper复用授权SQL时的相对片段引用已改全限定名称。任务完成、签名主体、QA与原PQC身份、轮次、租户、冻结状态及投影失效均纳入定向验证。

5、本轮集中验证已通过：MES通知、审批、工作任务、签名、资料、放行补正及NCR事务综合853项；独立作废/BPM/S4相关64项（其中新增作废11项）。两组共917项，0失败、0跳过。此前单跑的签名47项、NCR19项已包含在综合集，不重复累计。前端原定向23项、新增作废与注册17项通过（存在重复注册用例，不相加统计）；完整TypeScript检查、菜单迁移检查及任务涉及代码的 `git diff --check` 通过。

实际执行证据：[MES综合JUnit](../../.tmp/edhr-sol61-final-regression/reports/TEST-junit-jupiter.xml)、[MES综合日志](../../.tmp/edhr-sol61-final-regression/tests.log)、[独立作废及S4日志](../../.tmp/edhr-void-verify/tests.log)、[前端检查](../../.tmp/edhr-sol61-frontend-resume.log)、[类型检查](../../.tmp/edhr-sol61-tscheck-resume.log)、[菜单检查](../../.tmp/edhr-void-menu-check.log)。

6、验证方式边界：共享target曾被并行构建重建，故后续Java定向验证使用已校验的真实依赖快照，并优先编译当前受影响源码，以JUnit实际执行；没有伪造缺失类、跳过失败或降低业务断言。这不是全仓库Maven构建通过证明。NCR过期夹具已适配真实签名创建审计和正式审批结果读取器，保留完整回滚、身份及证据一致性断言。

7、本地验证时列出的验收前置：应用本轮迁移，核对GXP策略及通知模板，部署匹配的前后端；若未来显式启用S4，须按现有generation构建/激活流程更新第六个工作台来源控制头。当前环境实际采用S3，无须为本轮直接查询激活S4。迁移执行及运行核验结果另见第9项。迁移清单如下：

- `20261028_mes_edhr_nonconformance_reviewer_dispatch.sql`
- `20261028_mes_edhr_deviation_review_assignments.sql`
- `20261028_mes_edhr_record_change_tasks.sql`
- `20261028_mes_edhr_dossier_completion_notify.sql`
- `20261028_mes_pqc_rejection_correction_round.sql`
- `20261028_mes_pqc_correction_notify.sql`

8、上述本地验证阶段未对运行数据库应用迁移、未停止/重启共享后端或发布。当时只读Playwright检查未通过，也没有真实页面QA、偏差、重开补录、补件补正及作废全流程和通知送达回执。此为历史验证边界，后续运行环境执行结果见第9项；完整业务E2E仍不得据页面检查标记通过。

9、后续运行环境记录（覆盖第8项当时未迁移、页面未通过的状态）：主线程已在用户原有授权下补齐本地迁移，并修复动态SQL空白与BPM标识连接排序规则错误。当前int_main健康，个人工作台筛选/隐藏/排序及PQC办理入口通过；四个业务列表和QA/记录变更选人入口通过。9个关键MES业务类与本报告定向测试的有效类路径字节一致。真实个人页响应contractVersion=3确认实际采用S3，不依赖S4 generation。详见[运行补验第38—52项](edhr-batch-notification-approval-workbench-20261027.md)及 `output/edhr-workbench-acceptance-20261028.json`。通知投递与审批全流程仍缺本任务自有前置数据，没有标记E2E通过。
