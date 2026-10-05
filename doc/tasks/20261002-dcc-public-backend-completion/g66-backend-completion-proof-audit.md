# G66 HTML完成证明：15项后端核对

2026-10-06。真实三拒绝4035/37/39→4041同A/3第4次受控、下发；旧A/2作废。先有限审读，再获Root授权补缺XML；生产/测试0修改。未实现与未足证明分开，批次不累加。范围限15AC。

## 证据键（所有XML均原始字节归档）

根目录：本任务。文件为`TEST-cn.iocoder.yudao.module.dcc.service.<包>.Dcc<类>Test.xml`（包默认file）。

| 键 | 当前证据及源码匹配 |
| --- | --- |
| E1 | `g64-final-junit`：NativeRejectionTransaction7、ControlledFileFinalizationServiceImpl59、RejectedRevisionRetryDatabase50、WorkflowNativeCompletionEvidence4；120/4全0。G64生产/直属测试SHA匹配；Retry50实际执行继承SelectedIteration/Checkin断言，非50次实签名。 |
| E2 | `g64-current-control-r3-final-junit`：RelationControlledEventIntegration16全0，源码SHA匹配，真实SharedCore/Notify服务DB；替代旧G54缺Adapter及r2编号未对齐。 |
| E3 | `g59-final-r2-junit`：NativePlatformLifecycleTransaction8、WorkflowLifecycleTransaction14；Core/Lifecycle/测试SHA仍匹配。Adapter由G60当前源及E2闭合。 |
| E4 | `g62-final-junit`：WorkflowObsoleteTransaction13、ControlledFileObsoleteService16、ControlledFileObsoleteFormEffectExecutor10，另BPM义务2；G62 Obs/测试SHA匹配，替代G60旧caller。G60 PendingObsoletePlatformTransaction3仍匹配。 |
| E5 | `g56-linked-upload-order-junit`：WorkflowLifecycleReminderApi3，仅日期分类/单时钟/格式局部；不把已变WF的旧整批称当前通过。 |
| E6 | `g66-current-attributes-junit`：projectcode.ProjectAttributesService12；file.WorkflowAttributesIntegration筛选12、SelectedWorkingAttributesDatabase本地2、WorkingAttributesHttpContract4；30/4全0，4测试+2宿主+6生产SHA现封存，无修宿主/重跑继承50。 |

真实收据根为主任务`20261001-dcc-integration-unblock`：R1=`g64-three-rejection-rounds-real-proof.json`，R2=`g64-fourth-same-target-control-real-proof.json`/`g64-final-retry-distribution-real-proof.json`；R3=`g61-training-actor-acceptance-checkpoint.json`；R4=`g60-partial-future-control-real-proof.json`/`g62-natural-activation-real-proof.json`/`g62-natural-date-activation-readonly.json`；R5=`g63-independent-obsolete-real-proof.json`。真实页面办理、SQL仅旁证；旧remaining由后来收据取代。

方法别名均后端DCC `service/`：Attr/Snap为`projectcode/attributes/DccProjectAttributesService/DccProjectApplicationSnapshotService`；WF/Query/Version/Life/Obs分别是`file/DccControlledFileWorkflowServiceImpl/QueryServiceImpl/VersionPolicy/LifecycleService/ObsoleteServiceImpl`；Assign=`DccWorkflowSignoffAssignmentService`，Relations=`relations/DccRelationRemediationService`。

## AC证据与仍需证明的部分

| AC | 当前方法与实际断言（不是仅测试名） | 真实组合 / 边界 |
| --- | --- | --- |
| 02 | Attr.beginDraft/saveDraft、Snap.submitReservedDraft；E6三路径实SQL默认/actual分离、项目默认不改。E1早稿CE不被后稿MADSAP覆盖，旧行不改。 | 上传主链真走；手改差值、审批查看同值、其他文件不变的实UI对比仍需归档。 |
| 03 | Attr.initialize/Initializer受控source分支首次建新草稿读当时项目默认；E6三路径独立保存、E1新FDA≠旧NMPA、E4旧属性不改。 | 已存4042 CE草稿再选换版仍CE属04；此前“所有换版读最新default”过宽，纠正为首次初始化，不需源修。 |
| 04 | WF.readReplacementApplicationAttributes读selected的REVISION reserved草稿；createRevision→Initializer.WORKING继承原default/actual，非历史受控属性；E6/E1旧行不覆。 | Root4042在CE下已存，project后来OTHER/Y，r12/319仍CE正确；提交新Candidate/BPM不是重初始化。真正未保存新草稿读OTHER/Y。 |
| 05 | Attr.encode→Attributes.validated；E6 OTHER/NA/转移条件逐项拒错及正确保存、HTTP精确actor/Long。 | D01互斥/条件必填在HTML仍建议待核。仅用户已确认三组字段可选/带出/改/独立保存作确定要求。 |
| 07 | Assign.assign存指派签名后setAssignee；E2实际Flowable7→9、义务/签名持久化同事务；原G56指派23有自己/他人独立断言。 | R1/R2每轮ASSIGN→APPROVE实签主要账号1自指派；他人真实账号收任务/会签仍弱。 |
| 08 | WF.rejectTask+Finalization.callback exact-key CAS；E1真实Flowable拒绝、foreign事件拒、晚失败四域回滚，新BPM全义务。 | R1三节点实签拒绝、165文控空密码拒；R2第四轮重走会签。主链已真实闭合。 |
| 09 | WF.createApprovalProcess needTraining；E1正式v4上传/升版true/false模型，仅true有TRAINING receive、全部DOC_CONTROL_REVIEW；Life保两日期。 | R3独立文控线下记录→DOC_REVIEW→受控，R4升版无培训仍审核，R2下发；完整“升版有培训”现场未归档。 |
| 10 | Query.checkout/checkin + Master/checkout锁；E1横杠小版、正文隔离/释放锁、失败恢复。QueryTest非requester/等锁后复核/持有人显示有断言，无当前XML。 | 实际多轮检入足；两个合法账号争同锁且第二人见持有人后失败的组合未足。 |
| 11 | Version.formalTarget+Revision.freezeCandidate；E1四分支A1→A2、PARTIAL_A9→B1、REPLACEMENT→B1，较早正文bytes[10,11,12]/SHA，旧行/签名/指针不改。 | R4真早稿A1-1→A2。滚字母/换版后台强H2+BPM已有，不重跑政策unit；两完整前端场景未归档。 |
| 12 | Life.completeControl/activateDue、Relations受控事件；E3/E2 pending仅latest/open变化、保旧ACTIVE，晚失败pointer/shared/event/task/outbox回滚，历史关联不改。 | R4关联A2待生效/历史A1、00:00自然生效旧A1作废闭合；失败分支强事务，不破坏实库造失败。 |
| 13 | Assign安排、Relations仅CONTROLLED建任务/outbox、afterCommit消息；E2两合法关联只选20，30任务0，控前0/控后1、期限/人/BPM准、失败0/重放1。 | 当前后台组合足；真实“会签选整改→失败0→受控后责任人打开消息/待办”仍缺整链证据。 |
| 20 | Obs.submitInstance冻结义务/applyApproved签名作废；E4实际Form/effect、独立属性/旧BPM/retention回滚；pending→sharedOBSOLETE3case。 | R5只两审批任务、终结/0剩余、签名424–426、旧BPM/培训保、20年到2046。关联/引用作废标示由FE补对应证据。 |
| 25 | Life pending→激活旧OBSOLETE/新ACTIVE；Date拒past。E3并发一event/audit、失败保旧ACTIVE、独立时间。 | R4自然00:00 job5625租户1生效2/4租户结果都有，无改钟；R2控/下发各存。不能拿outerSUCCESS充租户成功。 |
| 26 | Life.pendingDistribution排除已下发、日期/id升序、≤today+lead；E3 H2[1,2]→下发后[2]，E5四日期分类/缺配置拒。 | Shanghai/7天/分钟job及下发已配置；混合到期/临期/已办排序、消除提醒实UI仍弱。E5示例3天不证明实库7天。 |
| 27 | Rework.requireAttempt+Revision.freezeCandidate；E1连三A2、独立正文/BPM/尝试/前驱及default/actual Map、旧签名原行不变。 | R1/R2三失败+第四同A3、旧11签名/三BPM保、新437–440、旧A2到新受控才作废；逐轮正文/属性来源实对比还弱。 |

## 有限下一动作及新增收据

1. 一次属性UI组合：不同默认/actual，上传/升版/作废回显修改；保存/提交后改默认，看原草稿/在途/历史/新申请，只读比对每轮来源actual及其他行摘要。
2. 两个已有真实签名资格/合法范围账号：指派他人会签、检出竞争；权限拒绝不充锁失败，不造签名图/默认授角色。
3. 一次实际部分关联整改+责任人消息/待办；后台E2不用重跑或改生产。
4. 复用旧失败轮次补正文/属性/签名对照，现待下发表补排序/7天/已办移除。若完成口径要求每场景实UI，再办A9/换版，不注版本/改钟/SQL代办。

E6当前补证CLI0 03:42:24，`g66-current-attributes-verification-receipt.json`；另ReviewerConfiguration13/1 CLI0 03:48:27（含DecisionPersistence继承），`g66-current-reviewer-verification-receipt.json`：配置改8不改旧冻结1，8不能接管旧案，新提交冻结8/批准独立，原源18434f…未变。16源及5原XML已封`g66-current-proof-fingerprints.json`。两次仅收据源目录编目误差已更正，不是测试失败。Maven已FREE，无源码修复/重包。

没有新确定生产缺口；这里是完成证明缺口，不扩大未确认D01–10，也不因旧remaining重复主链。当前证据仍不能给全27项签发完整PASS。
