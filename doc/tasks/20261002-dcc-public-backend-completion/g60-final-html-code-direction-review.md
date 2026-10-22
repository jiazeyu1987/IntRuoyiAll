# G60 最终 HTML v1.6 业务方向独立复核

Status: ready_for_closeout — 完成一次有限只读审查，发现 1 项确定主流程 P1（待生效受控文件独立作废与共享状态冲突），已即时交 Root。本文不修改生产/测试，不执行 Maven、types/build、DB/API/UI、服务或 Git；不声称全部 27 AC 已真实验收。

基线由 Root 提供为 main 工作区 int_qms HEAD `8c564`；本 Agent 不执行 Git。实际需求文件 SHA `53e9afc74cafd1539cd48d9f2384d364511dd4802c4d686431b444b549930222`；G59 最新 Repair SHA `d24f20b3aa0c26e0120bf352df42ac8a4d43e865792121165f6db98e2b99cff7`。G59 R3 只增加明确 JDBC `LocalDateTime`/`Timestamp` 精确时间合同，7 项原 XML 全 0 的记录来自 Owner；不把此前 Timestamp-only runtime 失败当成当前未修。

## 27 AC 的当前源码结论

下列路径简称位于 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`；测试简称位于该模块 `src/test/java/.../service/`。列出的测试是已有定向行为证据，本次没有重跑；只有 Root 明确真实页面证据才列为实际办理。

| AC | 当前正式业务接线 / 定向证据 | 本轮结论 |
|---|---|---|
| 01 | ProductCreate 配置审核身份快照；`productcreate/DccProjectProductCreateWriteService#writeApprovedRequest`56–114 同事务创建当前 tenant 项目、真实 DCC catalog/relation、模板目录及选定 leader USER/OWNER。正式产品 resolver 区分 MDM 与已批准 DCC 身份，不强加 14 位码。G39 OWNER 事务、G46 产品身份、G49 原申请 provider 测试。 | 已实现；G55 真实批准项目271/产品614、OWNER1与普通上传已通。审核配置变化、独立账号不是该单 admin 证据范围。 |
| 02 | `DccProjectAttributesService.beginDraft/saveDraft/freeze` 与正式申请属性轮次，来源默认和实际值分别保存；同一已提交申请不能改实际快照。Workflow1140附近冻结正式轮次。 | 已实现；G55 项目 CE 默认、本次 FDA 实际保存已真实证明。 |
| 03 | `DccWorkingApplicationDraftInitializer` 从正式受控源新建 REVISION 草稿时读取当前项目默认；独立 OBSOLETE 属性申请单独 begin/save/freeze，旧文件属性不回写。`DccSelectedWorkingAttributesDatabaseTest`、`DccWorkflowAttributesIntegrationTest`。 | 已实现；两种新申请与旧实际值不同的完整页面比较仍待 Root，不能写成未实现。 |
| 04 | `DccProjectAttributesService.beginDraft`49–58 已保存即回显；`forkForRework`63–80 复制原来源与实际值到新轮次；显式 restore 才重取默认。 | 已实现；不存在因重开自动覆盖已保存快照的源证据。 |
| 05 | `DccProjectAttributes.validated` 对 N/A、多选 Other 条件字段、身份和转移目标严格校验，FE 同合同。`DccProjectAttributesServiceTest`。 | 已实现；不把尚未确认的字段正式命名或 D01 交互细节扩大为新主线要求。 |
| 06 | 上传真实来源票据、实际属性/部门/培训和关联选择→二次确认；G56 R03 在 WORKING 合法阶段冻结关系后才 pending/BPM，同事务晚失败回滚。 | 已实现；G55 取消/再次确认与 G56 含1关联、选培训真实提交均已有 Root 证据。旧 File0/FROZEN 失败保留且被后继结果覆盖。 |
| 07 | 正式当前任务定义/租户/department obligation→signed ASSIGN→实际 setAssignee；后续 APPROVE 为另一签名。`DccWorkflowSignedArrangementTransactionTest`、指派 scope 测试。 | 已实现；G55 同 admin 两部门义务各自签名成立，不外推成不同用户收件。 |
| 08 | 真实 task approve/reject、必须密码签名、失败旧轮次及完整新会签轮次；G56 R01 首次 REVISION unsigned 初始安排读资格只读闭合，原 signed write 不放宽。 | 已实现；文件三节点分别驳回仍需页面验收。G55 项目申请8/9/10驳回重提不代替文件驳回。 |
| 09 | 正式 v4 UP/REVISION：会签→批准→可选 TRAINING receiveTask→文控审核→受控→独立下发；`DccOfflineTrainingRecordService` 为 doc_control + 精确 TRAINING_RECORD 类别权限，不要求逐人在线确认或第二批准。 | 已实现；G55 无培训、G56 文件4028线下记录→文控→受控16:46:51→下发16:57:02真实成立。G58 独立文控账号类别资格已修，但本轮不同账号上传记录仍由 Root 留证。 |
| 10 | Query checkout/checkin 持 project→Master/File 锁、正式 checkout 记录及 actor，票据正文检入→`VersionPolicy.nextWorking` 横杠小版，释放锁。 | 已实现；G58 实际 A/1-1 与 A/1-2 两次检入并释锁、原 A/1继续执行；两个账号竞争不由这证据代表。 |
| 11 | `RevisionService.createRevision`184–215 只用 Master 最新受控基线和实际选定合法 WORKING；`formalTarget` PARTIAL/REPLACEMENT、A/9→B/1，正文冻结且实际 intent 持久化。`DccRevisionServiceTest` selected body/intent，`VersionPolicyTest`。 | 已实现；Root 实际 PARTIAL 4029 旧共享 FINALIZING 阻断由 G59 正式同步及审计修复入口处理，实际重试仍待 Root，不当新候选已经通过。 |
| 12 | `Lifecycle.completeControl`63–78 只更新 latestControlled/shared pending；`activateLocked`125–160 生效才切执行并作废所有较低受控版，失败同事务回滚。关联消费 CONTROLLED，整改只 afterCommit。 | 已实现；G59 真实 H2 native/shared 及三版本日期测试覆盖 pending不提前ACTIVE、旧执行不变、晚失败回滚。自然未来日期页面验收仍待 Root。 |
| 13 | signed REVISION 安排→`RemediationService.recordControlled`77–135 精确成功事件、所选人员与 frozen relations、tasks/outbox→afterCommit dispatcher；未选择不建任务、失败/撤回无 CONTROLLED 不通知。 | 已实现；`DccWorkflowControlledRemediationIntegrationTest` 的 selected/failure/replay、`DccRelationRemediationTransactionTest` 对通知事实有定向证据；真实选人收件仍待验。D08 后续整改完成政策不擅补。 |
| 14 | `LatestControlledFileResolverImpl.resolveLatest`20–36 仅 Master.latestControlled，不退 currentActive；ACTIVE/pending 标识独立。`RelatedFileService` 当前关系 stable Master，history278–288固定当时版本/名称，冻结守卫298–313。 | 已实现；G56 含关系真实提交与受控 current set 成立；目标新受控版、原审批历史对比还需实际。G59 新 shared 状态没有替换该 authoritative native 指针。 |
| 15 | folder/global query、精确 selected body permission、历史 trace/viewer只读，R02 当前来源 boolean，历史与 current 分支分开。`DccRelationNameMetadataDatabaseTest`、Query/selector/readonly viewer 测试。 | 已实现；G55 PDF 实际 canvas、G56 readonly trace 已有真实证据；HTTP200不单独冒正文渲染。 |
| 16 | `DccProjectReferenceService.create/createBatch`24–72 锁 target project/folder/Master，正式 `DccProjectLeaderService` 只核当前项目 leader，不以 OWNER/admin 替代；计数 COUNT(DISTINCT project_id)。 | 已实现；G56 真实项目272/folder4引用源4026计数0→1已办理，单 admin 不外推不同项目负责人或跨账号拒绝。 |
| 17 | `ReferenceService.cancel`75–90 核相同 leader、expected reference、二确认；只删除目标 reference，count返回；FE source badge count>0、reference row另有文字。 | 已实现；G56 同一引用解除1→0、源 class/render和入口消失已记录，不宣称截图中不可见的橙色像素。其他引用/历史不删除。 |
| 18 | 模板直接权限保存，无审批；ProjectFolder maintenance 锁下拒 files/reference/child，使用中类型停用保历史。G34 旧审阅矩阵仅 LEGACY 范围，不能停用三 typed routes；G56 public folder正式入口已补。 | 已实现；相邻真实H2/权限/目录维护与 typed resolver 测试已有证据，不要求重跑全部基础数据排列。 |
| 19 | `Lifecycle.pendingDistribution`199–213 按生效日/id升序，未下发才提醒；formal distribute独立二确认、收件/签收事实不同，不改effectiveDate或造业务失败/驳回分支。 | 已实现；G55/G56电子下发真实完成；纸件收回具体职责/独立签名仍是 D06 未定项，不擅加。 |
| 20 | 精确文件操作面板→正式独立 OBSOLETE two-node BPM→approved effect，旧属性保留、20年、archive request、shared同步。 | **发现 P1：待生效受控版本允许作废，但 shared caller 固定 ACTIVE→OBSOLETE，见下节。** 已生效 ACTIVE 路径现有接线不因此一并报失败；完整实际作废仍待 Root。 |
| 21 | Workflow2370–2372 从真实票据取得完整 sourceOriginalName，`NameClaimService`152/164/294原字符串；generated VARBINARY 名称键+tenant全局唯一，不 lower/去后缀。 | 已实现；`DccRevisionExactNameTest` SOP.pdf/sop.pdf/SOP.PDF/SOP.docx exactnames；`DccLegacySourceNameOccupancyTest` 和逻辑并发测试验证正式占用层。四种名称跨项目实际操作仍待 Root。 |
| 22 | candidate保存 actual revisionChangeType/source/selected/reason；详情/历史按对应行读，不根据 B/1 猜类型。 | 已实现；`DccRevisionServiceTest.freezesSelectedBodyAndPersistsActualIntent` 覆盖局部/换版和实际正文，G58 真实工作稿非正式类型示例。 |
| 23 | 当前各 formal DTO/列表/详情显示受控/工作稿/审批/作废，shared执行指针明确“当前执行受控版本”，不把 latest controlled当active。 | 主方向已实现；G55/G56真实页面及FE修复证据有效，本轮不做全仓文字搜查扩成平台任务。 |
| 24 | `NameClaimService`104–119 NEW draft replay必须真实未提交自身正文；122–190 EXISTING_MASTER_VERSION核实际selected源，不是raw sameMaster放行NEW。230–280保留/释放，`ObsoleteRetentionService.plusYears(20)`。 | 已实现20年内占名和正式编号（project/type/number身份）；自身合法版本仍可用名号，NEW不同链拒。期满具体审批/归档未定，不擅放Master unique或删除历史。AC20待生效作废P1须先修才可验其20年实际效果。 |
| 25 | 编制 effectiveDate、controlledTime、activatedTime分别持久；ReviewDate禁止past；latest/active分离，多未来低版不会反向生效或毒化分钟job。 | 已实现；G59 pending/ACTIVATE/OBSOLETE shared在原事务同步，CAS/tuple严格。自然日期真实切换未验，不改时钟、数据日期或用API冒验。 |
| 26 | `DatePolicy`配置上海/7天；pending undistributed 日期排序、OVERDUE/DUE/UPCOMING/FUTURE共用单业务日；activation handler minute及 G57 exactjob5625 RAM恢复 opt-in，默认false不启全部shared jobs。 | 已实现；Root 已真实创建job5625、分钟日志；重启恢复24项原保存测试成立。不能只用Quartz outerSUCCESS声明所有tenant生效成功，实际行/日志仍由Root核。 |
| 27 | `RevisionService`204–216/`RevisionReworkPolicy`46–71 按确切失败前驱保相同目标；新 File/body/BPM/attrs/attempt，新历史不覆盖；exactkey replay不重复copy。 | 已实现；`DccRejectedRevisionRetryDatabaseTest`/selected iteration/Flowable组合定向验证独立失败链。G59同步不将失败目标提前active；真正文件A/2连续失败重提仍待页面验收。 |

## G60-P1：待生效受控文件独立作废在共享状态处必然失败

这是新 G59 canonical 精确同步后与已确认 AC20 的具体主线冲突，不是未执行 E2E 推测：

1. `file/DccControlledFileObsoleteServiceImpl#validateObsoleteAllowed`345–346、`#applyApprovedObsoleteControlledFile`195，以及 `DccControlledFileObsoleteFormEffectExecutor#isDccObsolete`112–113明确允许 ACTIVE 或 CONTROLLED_PENDING_EFFECTIVE。该文件已受控，只是尚未到生效日期，符合“从受控文件操作面板作废”的正式合同。
2. 批准 effect 锁 Master185→File186，保存 native OBSOLETE/真实时间与20年保留，末尾240调用 `DccControlledContentAdapter#recordObsoleted`。
3. Adapter247–253 仍固定传 expectedFrom=ACTIVE、action=OBSOLETE_ACTIVE。G59已经让真实 pending native 对应 shared CONTROLLED_PENDING_EFFECTIVE、两个uniqueFlag均NULL，因此 `ControlledContentLifecycleCoreService#transitionVersionRefByDomainEvent`340–342准确拒 from 状态不符，整个批准 effect 事务回滚。不能用复原旧 FINALIZING/ACTIVE 假投影规避。
4. 现 `DccWorkflowObsoleteTransactionTest`53/fixture使用 mock Adapter；190–198 的 pending独立作废和到期扫描证明 native事务，却未走真实共享core。因此其 GREEN 不覆盖本缺口。现 G59 native真实core测试覆盖自动作废，不覆盖这一独立 approved effect caller。

最小修复只处理该正式 caller：依据锁定 File 的真实前像，ACTIVE仍精确 ACTIVE→OBSOLETE/原动作，pending精确 CONTROLLED_PENDING_EFFECTIVE→OBSOLETE/DCC专属 OBSOLETE_CONTROLLED；使用同Master/File/BPM/source identity与共享tuple/CAS，独立作废无successor，不从shared实际任意状态猜 expectedFrom，不放宽Core或GET修复。保持批准事务、原签名/历史、20年占用、审计和分钟到期不复活。

必要定向 BDD：Given 正式 pending native＋真实 pending shared 且已批准独立作废轮次，When 正式 effect执行，Then 同事务变为两边OBSOLETE、latest历史保留/currentActive不误清另一执行版、名称编号20年、以后到期扫描false；晚 shared/Gxp失败所有native/shared/retention/audit事实不变。保 ACTIVE路径回归即可，不扩大通用目录、整个Quartz或新的作废平台。

读时指纹：ObsoleteService `e648d32a79e1ded1ba1dee76153ceb8e289fa51bd13427d2d360fec558ce4c67`；Adapter `873051b489cf300867abc95977d11a386947f507ab49dbbf54d2e62e07ccaa37`。已向Root即时报告，本文未修源、未运行新测试。其它已确认业务方向本轮未发现额外确定P1；这个结论不等于完整负向排列或全部真实验收完成。

## 当前证明与未定边界

采用 Root 最新 `g56-html-ac-inventory.json`、`g56-fix-scope-matrix.json`及后继真实回执；旧报告中 R01/R02/R03 未修、job未创建、training/reference NOT_RUN 的原时间段只保留历史。G56培训/受控下发、reference0→1→0、G58两次真实检入、G59最终核心同步/精确修复 source及最新JDBC合同不能被旧 blocked覆盖。

仍只列必要真实办理：旧样本 audited repair和PARTIAL重新提交、独立作废、文件节点驳回同目标重提、独立文控培训账号及自然未来日切换，由Root按当前包处理。上述缺验收不自动形成源码缺陷，也不拿同账号多职责称多用户。

不擅实现三项未定：D08整改任务后续完成/冲突协调政策；Z之后字母规则（当前明确拒绝而不猜AA）；D10引用是否跟随最新受控（当前引用固定真实选定版本，关联才始终最新）。期满处置授权同样保待定，20年内占用是当前确定需求。没有新增QA批准、平台或权限旁路。

## 后继只读：真实作废弹框的路线配置提示

Root 实际待生效文件4033作废弹框 preview 请求 HTTP200、业务提示固定审批策略不完整。`DccApprovalRouteAdminServiceImpl.previewRoute`160–177准确选当前 category + actionType=OBSOLETE、启用且已生效的最新路线，来源表 `dcc_category_approval_route` 与 `dcc_category_approval_route_node`。无对应路线应报 APPROVAL_ROUTE_NOT_EXISTS；当前固定策略提示由已选路线的节点校验产生，属于已存在 OBSOLETE 配置不符合两节点合同，不能凭该文字猜具体坏字段，也不能借 NEW/REVISION 或 LEGACY 自动代用。本文没有访问实际库。

正式维护入口是文控菜单 **上传审批**，路径 `/dcc/controlled-file/routes`（正式菜单SQL命名与component `dcc/controlled-file/routes/index`）；需 `dcc:controlled-file:route:manage`。页面“新增路线”或准确作废行“修改”→文件类别技术调研报告/908710→动作类型“作废”→路线生效时间不晚于当前。动作切换会重建两个节点：1会签/MATRIX_REVIEW，候选来源“部门负责人”DEPT，明确启用有负责人且签名就绪的部门、全部通过100%、必经；2批准/MATRIX_APPROVAL，候选来源“指定用户”USER的正式批准人，或显式审批岗位POSITION、任意通过、比例NULL、必经。后台保存生成准确stageCode/order/requireAll字段，只停用相同category/action旧有效路线，NEW/REVISION不受此次配置影响。作废不配置培训、文控、第三节点或历史LEGACY四阶段，亦不改文件类别权限规则模板充当流程路线。

`DetailObsoleteApplication.vue`94–97首先等待部门/账号与 route 三项读取；route校验失败直接catch，114–119 的正式 `attributesRef.selectProject` 尚未执行。因此本次“项目默认属性没加载”是同一前置配置错误的后果，当前没有独立 defaults 源码缺口依据。修好正式OBSOLETE路线后重新打开弹框应继续读取当前项目默认，保持不复制旧受控版实际值。候选账号还要满足现启用/审批岗位事实；不要新增审批人员或签名图片伪值来跳过。

本次读时 SHA：ActionPolicy `0a1d9ea939c6f1d2e32ce26d215d8dee482beabff3e52dde619215eb5f75e31b`；RouteAdmin `694e63848bb06ce432913aa7bf32031d14a8aabc4a081d1c3e07ad258de7646a`；RouteForm `482bfa4b735ed4f8161625bbe254427607cab292c58538d8ddf1eecafa69b3a1`。仅给Root实际UI配置路径，不改源或运行测试/配置，未增加新的确定产品P1。

### 路线914002后继候选未解析提示

Root真实配置兩阶段 OBSOLETE 后，DEPT100 + POSITION900332 预览报 ROUTE_PREVIEW_APPROVER_NOT_FOUND。`RouteAdmin.previewNode`295–296 的DEPT分支使用 `resolveDepartmentLeaders`339–359→正式DeptApi/DeptService→`system_dept.leader_user_id`，必须部门存在且启用、准确一行、负责人非NULL、负责人账号正式启用；没有以当前admin或上传人为负责人默认。上述缺失/停用统一产生此报错。若负责人仅没有系统岗位，后续 `DccApprovalParticipantPostValidator` 会报独立 APPROVER_POST_REQUIRED，不是本次文字。

POSITION分支309–315另读启用 DCC岗位分配（USER或POST成员），空集合也可报同一句。因此仅凭错误不能证明DEPT100具体哪字段坏；在Root给出的NEW5同POSITION900332已有效上下文中，应优先只读核DEPT100当前可见/启用/leader及leader启用事实，采用已正式有效910334无需放宽任何守卫。本Agent没有读取库、改配置或确认当前100实际leader值。

Workflow本次手选部门也没有admin旁路：`DccControlledFileApprovalRouteAssigneeResolver`135–137按本次selected部门替换默认，260–280执行同样正式部门及leader解析。成功提交时真实leader被冻结到义务；`SignoffAssignmentService`后续只核该义务保存的leaderUserId。旧任务admin1曾可签名办理只证明当时冻结资格，不证明当前DEPT100目录事实仍满足预览，也不代表两套解析政策冲突。
