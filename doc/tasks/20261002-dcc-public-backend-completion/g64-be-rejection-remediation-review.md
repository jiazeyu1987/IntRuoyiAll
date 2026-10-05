# G64 后端驳回重提与关联整改核对

- 状态：ready_for_closeout（本次单轮源码/原封存证据核对，不代表完整业务 E2E 完成）。
- 日期：2026-10-06；分支：int_qms。
- 范围：HTML v1.6 AC-08、AC-13、AC-27；仅只读正式链路、测试源码及已有原始 XML。
- 本轮没有修改 Java/测试，没有 Maven、真实 DB/API/浏览器/服务或 Git 写操作。未发现需要立即修复的新确定生产差异；不无因重跑历史测试宿主。

## 需求与当前正式行为

| AC | 已核正式行为 | 尚需真实页面证明 |
| --- | --- | --- |
| AC-08 | 公开 reject-task 先校验当前真实 task/文件/BPM/阶段，再密码电子签名、签名任务授权和正式 BPM 驳回。失败过程事件同步本地 REJECTED 与共享版本 REJECTED，未执行受控事件。申请人从失败申请检出/检入修正文档，提交新的独立候选与 BPM；新 BPM 从会签开始，冻结完整本轮路由/部门义务。 | 真实会签、批准、文控三个节点逐次签名驳回；文控未签名被拒；每次重新走全部会签。 |
| AC-13 | 只有当前 REVISION 指派允许保存整改安排；安排与实际指派签名同事务。保存只记录选中的关联 Master、合法责任人和期限。只有主文件正式 CONTROLLED 事件才生成本轮任务/outbox；受控即通知，不等待未来生效。失败/中断未发 CONTROLLED，因此不产生本次通知。 | 同一申请至少两个关联仅选择其中一个；失败轮次零通知；成功受控后选中人收到正式消息/任务、未选人无本次任务，期限不变。 |
| AC-27 | 正式目标由受控基线和同一变更类型计算；已有 A/2 时只能选择精确失败前驱的修正文档继续申请 A/2，不能重新消耗为 A/3。每次新候选隔离复制正文、清除旧审批/控制事实、记录尝试号和前驱，绑定新 BPM；旧候选正文、属性、签名和原 BPM 保留。新版未生效不改变当前执行受控指针。 | 实际打开各次失败申请及新申请，核对 A/2、独立身份/正文/BPM/属性/原签名；失败不影响 A/1 执行状态。 |

“文控驳回回申请节点”的当前实现是原 BPM 终结为 REJECTED、申请人正式检出/检入修正后创建新申请/BPM；没有假造 APPLICANT_REWORK UserTask。已有真实 APPLICANT_REWORK 的历史路线另有精确终结/新申请处理。普通三流程公开“退回/转办/加签”已停用，真实验收应使用“驳回签名”，不能调用停用的 return-task 代替。

## 精确源码链（路径相对仓库根）

Java 文件前缀：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/`。

| 文件 / 方法锚点 | 关键闭环 |
| --- | --- |
| DccControlledFileWorkflowServiceImpl#rejectTask/#rejectTaskWithProcessDefinitionKey（约1650） | 当前任务资格→实际密码签名→authorizeSignedAction→BPM reject；旧申请留存。 |
| DccControlledFileFinalizationServiceImpl#markRejectedAfterApprovalEvent（430） | exact event/当前文件锁/CAS→REJECTED→platformAdapter.recordRejected；没有受控调用。 |
| DccControlledContentAdapter#recordRejected（105） | 共享 IN_REVIEW→REJECTED，以真实失败事件/BPM同步终结开放候选；重提不会只清本地状态。 |
| DccRevisionReworkPolicy#predecessor/#correctionMatchesBaseline/#requireAttempt（20、39、46） | exact 同租户/项目/Master/类型/编号/文件名/申请人/受控基线及完整失败链；同意图保留正式目标与尝试号。 |
| DccControlledFileRevisionServiceImpl#createRevision/#freezeCandidate（约180、262） | 校验失败前驱实际过程已终结、所选工作正文身份/哈希；隔离正文，新 ID，清除旧 BPM/控制/负责人/培训证据，保尝试前驱；继承本次正文的 placement/审批关联快照。 |
| DccControlledFileWorkflowServiceImpl#submitWorkingIteration/#createApprovalProcess（762、1268） | 新路由/部门义务快照、新 BPM、精确申请属性冻结、共享 recordSubmitted/recordResubmitted；不复用旧完成会签。 |
| DccControlledFileQueryServiceImpl#isEditableWorkingVersion/#canCheckoutLockedVersion（860、876） | REJECTED/PENDING_APPLICANT_REWORK 的正式修正文档入口，仍保 requester/硬范围/冻结与检出锁。 |
| DccWorkflowSignoffAssignmentService#assign（约97） | 当前 Task.processDefinitionId 定义决定 REVISION；签名后保存所选安排同事务，NEW/OBSOLETE 不执行整改。 |
| relations/DccRelationRemediationService#saveArrangements/#recordControlled（44、78） | 保存安排零任务/outbox；受控事件 exact tenant/source/BPM/key/time，锁 Master，校验全部名单；只本轮所选行创建任务/outbox；不可覆盖历史安排。 |
| DccControlledFileLifecycleService#completeControl（约45–77） | 原件受控事实+共享 pending/active 正式过渡与 CONTROLLED 事件在同事务；失败整体回滚。 |
| relations/DccRelationControlledEventConsumer#onLifecycleEvent（23） | 仅消费 CONTROLLED；审批拒绝、WORKING 或 ACTIVATE 不触发整改通知。 |
| relations/DccRelationNotificationPostCommitScheduler#scheduleAfterCommit（24）；DccRelationNotificationDispatcher#dispatch（28） | 提交后分发；outbox 稳定业务键；失败先落 FAILED，重放不重复已 SENT 消息，不把已提交受控冒充未提交失败。 |

本次快照主要原始源码 SHA-256：

| 文件 | SHA-256 |
| --- | --- |
| WorkflowServiceImpl | 566b0886c28534a5e0e662f9146893b6e4c2765bec0bc0bafa69a7d1a81e3b41 |
| RevisionReworkPolicy | 466a6b26e65b8eefcfc2bbb5ee28674087cf9f9c0a869fa043303dba22b2f6c4 |
| RevisionServiceImpl | 99e213acd7ffe0569b89a12e8dd419c17d86157159526ba5459aa0b925d55d82 |
| SignoffAssignmentService | cb6f0010cb5f4fd60946071f5f4100f18c6fc798628dff13a1b21c78f3799754 |
| FinalizationServiceImpl | b002e6dc276da53bfc9ed676ff7b47cd45cf7a4dfc4f07dd69960e677406704e |
| ControlledContentAdapter | 99a9768e957b14eaa9d3c1aebfe60d11cc91e1c11b8613aae566ba5ac3d0d3b0 |
| LifecycleService | 5236cdf476b2f9528b24c7b6b428f10619417b6e1e1e696e81bd02857a93a767 |
| RelationRemediationService | 7ab5cb9f94d8c93104b6759aa4548f8f5c008964d293e0aefc2d6b0a46e3fa55 |

## 已存在的真实隔离证据和限度

以下 XML 均位于本任务目录，逐一解析原始 bytes；本轮没有运行其测试，也不把历史有效测试升级为最新包完整组合 PASS。

| 原 XML | 结果 | 实际覆盖 / 证据边界 | 原始 SHA-256 |
| --- | --- | --- | --- |
| g56-linked-upload-order-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRejectedRevisionRetryDatabaseTest.xml | 50；失败/错误/跳过均0 | `twoRejectedAttemptsRetainA2AndIndependentBodiesBpmAndFrozenAttributes` 使用实际 Query 检入/Revision/Workflow/Flowable，逐次核三份正文/BPM/冻结属性、尝试1/2/3、旧签名不变、A/1 ACTIVE；拒绝和既有签名由 FIXTURE 插入/过程终结前置提供，不证明真实密码签名驳回。包含继承宿主测试，50不是50次真实驳回。 | 62250bb50c6a835f3b7be0bb88a6ef79701b9a31ac4b49c4ff1e421fa9bb9427 |
| g54-signoff-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationControlledEventIntegrationTest.xml | 15；均0 | SELECTED/UNSELECTED/MISSING_POLICY 三组真正 Flowable 指派、签名投影/安排/audit 同事务、正式 NotifyMessage 服务落库、受控提交后所选通知1/未选0、重放不重复。密码鉴权/HMAC是明示替身，READY_TO_PUBLISH 人工提供；没有完整审批链。 | 2c635035ac04fc0689783c8790a09239207a3e5341a545a17488bcbe63571fbc |
| g56-initial-arrangement-read-junit/TEST-cn.iocoder.yudao.module.dcc.service.file.DccRelationRemediationTransactionTest.xml | 16；均0 | 真实 H2 arrangement/event/task/outbox 事务、所选人/期限、拒绝控制与外层 rollback 零任务、same-key、FAILED 重放；控制事实/actor/sender 端口为隔离替身，不是完整线上审批/通知。 | c7640c9d660b6ff555f3be05e3934e771772c72b6e0a374b6c362012793119fe |

已读测试实际断言，不以名字推导结论。当前 G54 `DccRelationControlledEventIntegrationTest` 的 `@Import`/TestBeans 没有供给 G59 新增 mandatory `platformAdapter`；它的历史15/0确实有效，但若要以当前包再跑控制整合，须先只补准确新平台依赖与正式共享种子。该旧测试宿主差异不是生产调用缺失，本轮不无因修改/编译。失败重提测试当前 source SHA f971a791fcd715db0774df6dc8da2fd15cb236761969c1732b61c12a459880ca；控制组合 source SHA 2ec61791112acd08c97dba157d3e06f092008c24cf5d2b07ca7c061a05a7f551。

## 主管理可直接执行的最短真实页面链

1. 使用主管理已拥有的任务自有 A/1 受控链（如果当前基线已 A/2，不把它猜回 A/1；选另一实际 A/1 任务文件），从项目目录操作面板检出/检入，选择所需工作小版，局部变更申请 A/2。两次确认时保存 actual 新候选/BPM，勾选两个合法任务自有关联；无需任何接口种业务数据。
2. 会签负责人从正式待办打开当前 task：部门负责人指派，REVISION 的“整改”表只选其中一个关联，选择实际具备权限的责任人及期限，输入真实签名密码。第一次会签签名驳回。通过正式申请/版本历史记录旧 file/BPM/意见/签名；主管理仅只读核对这一失败轮次没有本轮整改任务/outbox/通知。
3. 申请人打开该失败申请，检出/检入修正文档；从当前受控基线的“发起局部 / 换版变更”或修正文稿“准备本次申请”选择这个精确修正文稿，保持 PARTIAL，再提交 A/2。观察全新会签义务/新签名，走完会签，在批准弹框输入实际密码并驳回。保留第二次失败。
4. 重复相同合法修正链申请 A/2，重新会签、批准（在批准弹框选择实际文件负责人），如选择培训则由独立文控上传真实线下培训记录。文控审核先做页面未填密码提交的拒绝观察，再输入真实密码签名驳回；没有提前受控，保存第三次失败及全部旧证据。
5. 再修正文档申请 A/2，完整会签/批准/培训（如需）/文控审核。受控之前所选责任人没有本次任务；受控之后所选人站内信和整改列表各有本次精确 source/BPM/relatedMaster/期限，未选关联无本次任务。若未来生效，受控即通知而 A/1 仍执行；自然生效后旧版作废另由现真实每分钟 job 验证。逐次打开四个申请及正文/三属性/历史签名/BPM，不能只检查新版本号。

真实 UI 锚点：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 的 `actionDialog`/`submitDccApprovalAction`（2093、约7050）→`detail/approval-actions.ts`（152–200）→公开 reject-task。`DetailSignoffAssignment.vue` 只在实际 currentTask REVISION 渲染 `DetailRelationArrangements.vue`；其“整改 / 关联文件 / 审阅版本 / 负责人 / 整改期限”是本轮所选来源，不读取当前最新文件冒充审批快照。历史通过 `DetailApplicationHistory.vue` 与详情“本版本 BPM / 来源受控版本 / 选用正文小版本”、签名表和版本历史核对。前端 owner 的近期精确 SFC 指纹若变化，执行前应按当前 handler 再核，不绕正式 route/ACL。

## 收口

本轮交付为静态正式链核对 + 原有效隔离证据审读 + 最短真实验收路径。AC-08/13/27 的完整当前包真实前端验收仍 pending，不能以上述测试数量签发全需求 PASS。没有新确定代码缺口，因此本轮无生产修改、无新增 RED/GREEN；Maven 未占用，可由主管理继续实际链路验收。


## 后续真实复现纠正（G64 actual，保留前述快照）

主管理真实 REVISION 会签签名驳回出现 HTTP500，事务零推进。实际最终堆栈 `DCC reject lost its status CAS`；原始 mapper SQL 仍固定 `process_definition_key='dcc-controlled-file-approval'`，对合法 native revision/upload 始终不命中。先前无新缺口是该读轮快照，现已被此实际证据取代；不得引用为当前全部正确。最小修复范围及真实组合 RED/GREEN 见 g64-native-reject-key-bdd.md；不是并发事实，也不以吞 CAS 或降级回读修复。


## G64 actual fix closure

有效 RED r2 复现四个 native 实际回调的同一 CAS 错误，legacy 通过。现 Mapper/Finalization 两生产以验证后的真实流程 key 闭合，严格 CAS 不变。最终 120/4 全0/CLI0，01:34:21，真实拒绝同事务/晚失败/身份负向7 + 原Finalization59 + 失败重提50 + native完成4；原始 XML/源码/编译类见 g64-backend-fingerprints.json、g64-backend-verification-receipt.json。当前 Finalization 源 SHA 2a166f83c346f239a07af4f50647e583237d376b81d37e03fc9af498affda581 取代上面只读快照 b002e6；Mapper SHA c0a116e050b5722bd16c3bf5b71cb0bfc0c652eefbbbf7d2e12e8d5b7d622ead。前述实际缺口已工程修复，主管理须重试原真实申请后才可标现场闭合。AC13完整真实通知组合仍待前端验收，不因为本次120项回归增加就宣称完成。


## AC-13 current isolated evidence supersedes the old-host limitation

The existing DccRelationControlledEventIntegrationTest now imports actual G59 Adapter/Core/StateMachine and current mandatory authorization dependency ports, with explicit CONTROLLED_FILE/REVISION source/baseline. In its signed combination the file carries the actual tenant1 Flowable process/definition/task round; real shared transitions close the candidate as CONTROLLED_PENDING_EFFECTIVE, retaining the old executing ACTIVE. Real official notification API/service/DB creates only the chosen linked Master20 task/message after commit; another legitimate linked Master30 is unselected and has zero tasks. Native late control outer failure restores DCC/shared/audit/event/task/outbox facts and sends no messages; exact event replay stays once. Original15 cases and assertions retained, one finite late-control case added.

Current r3 actual16/1 all0/CLI0, final source fff56a7e41318422c448ca174806b1292958ea542b45d5d856ab8762d6a398ee; raw XML and exact File300/Master30 JOIN fact outputs in g64-current-control-r3-verification-receipt.json. Only one test changed, production0. Earlier missing-Adapter and unbound-read-port setup failures are retained as fixture-only; no new production defect was disguised or repaired. Upstream READY/finalization preconditions, account/template permissions and HMAC remain explicit isolated ports. This closes the missing current control/shared/official-message combination evidence, while actual frontend signed selected-remediation E2E remains a distinct Root acceptance step.
