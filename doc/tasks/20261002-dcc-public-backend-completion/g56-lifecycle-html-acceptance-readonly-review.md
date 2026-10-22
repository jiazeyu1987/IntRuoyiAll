# G56 HTML v1.6 生命周期与属性独立核对

日期2026-10-05；主树 `int_qms`，Root提供当前HEAD `8090e7193`。本owner按指定12项AC只读需求/当前生产/既有测试/主管理真实回执；仅写本报告，不执行Git、Maven、服务、DB/API、E2E或生产改动。原G28/G44矩阵的未集成/QAblocked/只有登录等状态是历史，不作为当前结论。

结论：指定生命周期主架构已有正式前后端、身份、签名、版本、快照、持久化与日期服务；**发现一项首次升版会签的整改读取接线阻断**，已交Root并由legacy owner修复。另有明确**运行配置门禁**：分钟生效Job尚未注册、当前运行Quartz被任务覆盖关闭；该Job及状态切换源码已存在。多数剩余是尚未走真实页面的完整分支，不能写成“未实现”，也不能拿单元数量或无培训上传通过替代。

## 已读真实证据边界

`doc/tasks/20261001-dcc-integration-unblock/g55-real-mainflow-review.md / g55-real-mainflow-root-review.json` 记录实际自有项目申请7批准→项目271/产品614/负责人OWNER；上传源默认CE、本次改FDA，二次确认取消保输入后正式提交；两部门分别指派/会签、批准选文件负责人、未选培训、文控签名受控、生效、文控下发，文件 `2054545668044084026` 为ACTIVE，6签名，下发2026-10-05 12:11:29。所有职责本次使用实际admin，回执明确 distinctActorsTested=false、trainingSelected=false、completeHtmlE2EClaim=false。这里的产品申请驳回重提8/9/10不是文件A/2失败重提，不能移作AC08/27证据。

## 按稳定AC核对

BE路径相对 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`；FE相对 `IntRuoyiFronted/src/views/dcc/controlled-file/`。下面的“源码闭环”表示已读到真实caller与正式写入，不表示整项真实E2E PASS。

| AC | 需求与当前源码闭环 | 当前实际结论与下一必要验证 |
| --- | --- | --- |
| AC-03 | 上传 `upload/index.vue.ProjectApplicationAttributes`→项目defaults API→Workflow创建/`freezeApplicationAttributes`。从受控版创建新修订，`DccWorkingApplicationDraftInitializer` 的ACTIVE/受控待生效源分支明确从当前项目defaults建立REVISION草稿；升版 `DetailApplicationPanel`读取准确WORKING已保存属性。作废 `DetailObsoleteApplication.selectProject`→独立FormCenter action及OBSOLETE属性轮次。 | 源码保项目默认来源/actual分开、可改；上传CE→FDA已真实证明。升版/作废从当前项目默认开始、与旧文件actual不同的真实操作尚未完成；保存草稿续用其已保存来源不等于新申请借旧受控actual。 |
| AC-04 | `DccProjectApplicationSnapshotService.prepareDraft/submitReservedDraft/read/forkToNewApplication`→`DccProjectAttributesService.beginDraft/saveDraft/freeze/readSaved`→属性表、BPM轮次绑定；已submitted实际值变化拒绝。`ProjectApplicationAttributes.savedSnapshot`和详情申请历史按明确轮次读，只有用户显式恢复才走restore API。 | 源码已有草稿/在途/历史隔离及失败后fork。未真实“修改项目默认后读取旧草稿/在途/历史”的完整比较；建议用项目默认CE、上传actualFDA，再改NMPA观察原值。没有依据报告已保存快照被当前默认覆盖。 |
| AC-08 | `DetailSignoffAssignment`→签名指派API→`DccWorkflowSignoffAssignmentService.assign`，指派签名与实际会签签名独立。详情任务意见/密码→Controller approve/reject-task→Workflow `validateTaskAction/verifyPasswordAndCreateWorkflowSignature`→真实BPM动作/签名与状态。同轮reject保留REJECTED，修正文稿走检出/检入新工作身份并重新完整申请；公开任意return接口不开放，以正式失败申请修正入口办理。 | 无培训NEW通过已证明六签名、部门义务独立；文件会签/批准/文控驳回、未签名拒绝与完整重走均未真实完成。首次REVISION指派前整改GET存在下述P1，修复后优先验一次含整改的升版；不以项目产品驳回替文件驳回。 |
| AC-09 | `20260930_dcc_a_workflow_bpmn_v4.sql`中UPLOAD/REVISION：MATRIX_REVIEW→MATRIX_APPROVAL→needTraining gateway→可选TRAINING receive→DOC_CONTROL_REVIEW→end。`uploadTrainingRecord:1484`仅doc_control、准确File/round/category/session/SOURCE目的票据，绑定实际线下文件后trigger TRAINING进入文控；不逐人线上确认、不加培训审批。文控签名/盖章→Lifecycle.completeControl→独立下发。 | 无培训NEW真实走通；有培训NEW和两种升版分支只见源码/隔离测试，不能全称通过。培训VO只有session/ticket，没有强加签名密码或userIds；旧legacy线上培训代码不在native新路径，不误报为当前违反。 |
| AC-10 | 详情/浏览检出入口→Controller `/{id}/checkout / checkin`→Query真实project→Master/File锁、activeCheckout检查/唯一占用；显示实际actor；持票据检入复制真实正文、`VersionPolicy.nextWorking`、initializer/placement继承、markCheckedIn释放，权限/日志同事务。 | 源码有正式多人互斥与检入小版规则；本轮尚无两个不同真实账号争抢及两次检入E2E。不能从离线锁测试或同admin六签名推出跨人验证。 |
| AC-11 | `DetailApplicationPanel`选择明确较早合法工作正文/实际intent→`submitWorkingIteration`→RevisionService→`DccRevisionSourceSelection`/`DccRevisionReworkPolicy`/`VersionPolicy.formalTarget`，基线/selectedIteration/body hash保存。`nextMinor:276`使A/9进位`nextMajor`→B/1；REPLACEMENT直接nextMajor，检入仍横杠小版。 | 源码统一规则及明确源版本/正文，不默认最新正文。较早工作正文、A/9进位、换版真实审批/签名/新文件读回未完成；先解除首次REVISION指派整改GET阻断。 |
| AC-12 | `Lifecycle.completeControl:38`实际受控时间/最新受控指针与当前执行指针分离；CONTROLLED事件同事务持久化。`activateDue/activateLocked:98`在到期时锁链、切ACTIVE、旧低版OBSOLETE/20年/审计/执行指针同事务，失败传播。关联当前解析LatestControlled，历史仍独立快照，整改通知在CONTROLLED成功后的正式事件发送。 | 源码存在，较新版本提前受控仍旧执行；多未来版本退休规则也在当前activateLocked，旧矩阵中的“只作废currentActive”风险已被源码修复，不能重开。未来受控/失败/no通知/自然到期真实链尚未验；运行Job缺口单列。 |
| AC-20 | 受控选择器/操作面板→`DetailObsoleteApplication`→Controller obsolete→`ObsoleteService.obsoleteControlledFile`准确action/attrs/部门/BPM；v4 OBSOLETE仅MATRIX_REVIEW→MATRIX_APPROVAL→end。统一批准完成effect→`applyApprovedObsoleteControlledFile`核正式批准轮次/签名，状态OBSOLETE、清执行指针、审计、archive request及retain。同源原历史/属性不覆盖。 | 源码批准结束，不创建培训/文控/受控/下发节点；非REVISION整改不能被塞入作废指派（G54已修）。真实作废流程及批准后无后续节点/可读历史尚未走，本报告未用接口代办。 |
| AC-22 | File保存revisionChangeType/来源/selectedIteration/description；Query详情与版本历史投影各版本事实；`shared/lifecycle.revisionChangeLabel`及detail实际变更/来源/历史列，不从字母推断。 | 源码已具备；不同来源B/1、A/2以及历史切换的真实DOM核对尚未完成，不能把B/1本身当REPLACEMENT证据。 |
| AC-24 | `DccObsoleteRetentionService.retainedUntil`实际obsoleteAt.plusYears(20)；独立/自动作废同事务调用NameClaim.retainObsoleteIdentity，modern与完整verified legacy占用分别维护。名称完整UTF8binary key、编号正式复合身份仍active，releaseExpired需明确授权/到期/无当前占用，立即release禁止；同Master自己版本沿用。 | 源码20年已确定，不回到待讨论、不额外要求WORM或自动期满删证据。保留期原编号/全名另一链上传拒绝、自己后续版不受阻的真实页面未完成；20年无法等待验证，核正式期限及占用行为即可。 |
| AC-25 | 编制/升版DatePicker→真实请求effectiveDate；DatePolicy.requireReviewDate禁止空/过去日期。completeControl保存controlledTime/publishedTime而不覆盖effectiveDate；future保持CONTROLLED_PENDING_EFFECTIVE。ActivationJob→dueVersions→activateDue有正式@TenantJob与事务，实际activatedTime/旧作废留历史。 | 今日NEW即时受控/生效真实证明两事实；“受控日期早于生效日期”及自然分钟调度到期切换仍待。当前Job未注册且Quartz关闭是运行缺口，不是代码缺生命周期；Root已给RAM/正式UI唯一Job方案。 |
| AC-27 | 失败file不可删除或改号；检出/检入修正继承同直接前驱/原属性来源；`DccRevisionReworkPolicy.requireAttempt`明确合法完整失败前驱、目标仍formalTarget、attempt+1、互斥并保存reworkPredecessor；RevisionService新File/隔离source/body/BPM/快照，详情历史展示每次id/attempt/BPM。 | 源码精确同目标失败重提已具备，不能把新A/2误升级A/3或覆盖前次。连续两次失败后第三次A/2、真实正文与签名/BPM/属性历史逐次读取未验；项目申请8/9/10不代表该AC。 |

## 本轮确定的源码缺口（已分配修复，未重做生产）

G56-P1 / AC-08、09、11：`DetailSignoffAssignment.vue` revision分支在初次签名指派前调用 `listRelationArrangements(fileId,context.processInstanceId)`，ready等待其完成。`DccRelationAccessPolicyImpl.requireParticipant:27–35`却要求冻结obligation.processInstanceId==round。初 `Workflow.persistDepartmentTaskAssigneeSnapshots`不存尚未产生的BPM，submit后只把BPM写File；`bindBpmTaskByObligationId`只写task/node。snapshot的processInstanceId直到 `DccWorkflowSignoffAssignmentService.assign`签名后update才绑定。因此正式首次revision leader canAssign=true但整改读取拒绝，无法到签名POST；空整改也受阻。

这是实际源全调用链问题，未声称本轮已经真实执行REVISION页面。Root/legacy owner已共同确认并分配**只读入口**修复：只在实际当前REVISION Task、同tenant/准确file/definition/obligation/leader资格时允许未签名NULL绑定读取；历史已bound读取保持，write仍必须原签名后精确round，GET不回填/造currentset/发任务。G54仅让NEW/OBSOLETE不预读整改，此REVISION遗留不同入口不应被前次通过覆盖。修复最终receipt由Owner/Root提供，本报告不凭正在改的源码宣布关闭。

必要BDD：真实已创建首次revision Task＋NULL initial obligation→合法leader整改GET实际空list/当前保留安排可读且0writes；错tenant/file/definition/round/obligation/leader拒绝；随后原电子签名指派保存准确安排/绑定并转派；已bound历史读取不需要activeTask、写权限不降。不要把obligation初始化成假已签名或跳过预读当解决。

## 日期、7天提醒和分钟Job的正式配置方向

日期/提醒代码没有发现额外正常主line源阻断：

- `DccWorkflowDatePolicy.now/reminderDates`读取正式`dcc.workflow.zone-id`及reminder-lead-days；当前Root运行flags实际声明Asia/Shanghai和7，不重新询问。`PendingWorkflowDistributionList.vue`已挂workbench/index.vue；workflowLifecycle API→Controller pending-distribution核doc_control+categoryDISTRIBUTE→Lifecycle query当前tenant、受控ACTIVE/待生效、controlledTime非空、distributedTime为空，effectiveDate/id升序；remindersOnly<=同一业务日+7，返回OVERDUE/DUE/UPCOMING/FUTURE，不把已下发再当未办。
- `dccControlledFileActivationJob`是正式bean，execute @TenantJob，dueVersions仅该tenant待生效且日期<=业务日，逐个事务activate。ZonePolicy当前业务clock与默认JVMclock不同需核：Rootg53 launcher已有`-Duser.timezone=Asia/Shanghai`及DBtime_zone=+08，独立作废使用JVMnow，不能改这两实际runtime约定假证明日期一致。
- 注册模板 `20261003_dcc_controlled_file_activation_job_registration.sql`存在，只新增paused infra_job；paused/NORMAL记录不等于Quartz触发。本轮Root只读实际库已证全tenant pending=0、due=0且handler无infra_job行。本owner未重新查DB；这不是“源码无法生效”。
- Root明确采用本任务RAM Quartz启动true、startup-sync仍false，通过真实前端只创建handler `dccControlledFileActivationJob`、cron `0 * * * * ?`，因此不注册/启动其它既有jobs。保正式zone/lead7及任务槽位/PID/Jar归属；不要仅全局开启共享启动同步。技术retry/monitor参数沿Root已review具体值，不凭空编造业务策略。
- `JobStartupSyncRunner`正常部署已有启动sync，`JobService.createJob/syncJob/updateJobStatus`正式调用SchedulerManager；没有看到标准部署无法注册/恢复该handler的源码缺口。任务模式RAM跨重启会丢Quartz状态，下一次须正式重登记；不能把该已知本机隔离约定改成新平台。
- `TenantJobAspect`按正常无scope参数遍历正式enabledtenant；`TenantJobParam`的明确scope编码是手动触发合同，**不是JSON**。本次自动分钟Job不要偷偷塞手动tenant token改变约定，Root需审实际所有enabledtenant/due目标并只用正式参数。当前pending/due零的读事实不证明未来所有数据永远为空。
- 现Aspect会catch每tenant异常并返回聚合JSON，outer `JobHandlerInvoker`只看抛异常判断log.success；所以outerSUCCESS/statusNORMAL不足以证明生效或tenant成功。该已有平台行为先作为证据边界，不无因扩通用Job框架：分钟证明需实测相邻调度日志/tenant返回及新旧实际状态、执行指针、生命周期事件/审计一致。自然到期不要改全机时间、DB日期或直接API激活冒验收。

## 现有定向验证来源与下一实际顺序

仅列下一需要时可用的既有测试范围，本owner**没有执行**：

| 场景 | 既有正式测试来源 |
| --- | --- |
| 三种默认初始化/草稿及失败属性继承 | DccProjectAttributesServiceTest、DccWorkingApplicationDraftInitializerTest、DccWorkflowAttributesIntegrationTest、DccSelectedWorkingAttributesDatabaseTest |
| native培训、文控签名/受控及BPM节点 | DccControlledFileWorkflowServiceImplTest 的nativeTrainingFixture相关方法、DccWorkflowSignedArrangementTransactionTest、DccWorkflowV4BpmnTest |
| 当前版本/检出检入与所选正文升版 | DccControlledFileQueryServiceTest、DccWorkflowSelectedIterationDatabaseTest、DccControlledFileVersionPolicyTest |
| 未来状态切换、失败回滚、日期排序/多未来版本 | DccWorkflowLifecycleTransactionTest 的futureControlChangesLatestOnly/effectiveTodayAtomicallyObsoletesOld/concurrentDueActivation/failedControl/failedActivation/reminders...，DccWorkflowLifecycleReminderApiTest |
| 独立作废/保留期/占用 | DccControlledFileObsoleteServiceTest、DccObsoleteRetentionPolicyTest、DccLegacySourceNameOccupancyTest、DccWorkflowObsoleteRoundSignatureTest |
| 原目标失败重提/历史前驱 | DccRejectedRevisionRetryDatabaseTest、DccRevisionReworkPolicy经现RevisionService/SelectedIteration组合 |

不能仅为了填表重跑所有旧tests：先完成G56-P1的独占TDD/finalread，再真实有培训NEW→真实检出检入/较早正文PARTIAL/REPLACEMENT→会签/批准/文控拒绝并同A/2多次重提→独立作废/20年身份占用→未来受控与自然分钟日期切换。用同源Jar/UI建立任务自有对象，所有验收动作由Playwright真实页面完成；DB/API只读对照。每个未执行分支逐项保NOT_RUN，而不是把整HTML标PASS或把未验标为未实现。

Root处理提交/推送及主任务验收矩阵；本报告不改已确认业务规则、不新增批准流程/QA门禁、不要求用户再确认日期或20年期限。
