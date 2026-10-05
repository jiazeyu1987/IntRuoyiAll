# G59 正式共享受控/生效合同与修复入口设计

状态：design_ready_for_root_review。仅文档；没有源码、测试、target、Maven、实库/API/UI/Git变更。G58pins保持。根因见g59-native-platform-lifecycle-readonly-review.md，Root已接受但要求此明确源清单/transition合同后再授实现。

## 状态与精确动作

共享新增canonical `CONTROLLED_PENDING_EFFECT`（拼写按Root本轮确认，与DCC domain `CONTROLLED_PENDING_EFFECTIVE`明确分开）；非editable、非openCandidate、非active执行，两个unique flag都NULL。不把未来版标ACTIVE，不改createCandidateRef“仅一个未决candidate”守卫。状态是“已完成正式受控，尚未实际生效”，因此允许下一未决审批candidate存在；当前执行版仍唯一ACTIVE。

| from | action（建议固定） | to | domain to | flags及事务含义 |
|---|---|---|---|---|
| FINALIZING | COMPLETE_CONTROL | CONTROLLED_PENDING_EFFECT | CONTROLLED_PENDING_EFFECTIVE | closeopen；activeNULL；保旧ACTIVE |
| CONTROLLED_PENDING_EFFECT | ACTIVATE_CONTROLLED | ACTIVE | ACTIVE | 新active1；同txn旧低正式控版先OBSOLETE释放active；closeopenNULL |
| CONTROLLED_PENDING_EFFECT | OBSOLETE_CONTROLLED | OBSOLETE | OBSOLETE | 高版已生效时低pending正常自动作废，两个flagNULL |
| ACTIVE | OBSOLETE_ACTIVE（已有） | OBSOLETE | OBSOLETE | 正式替换/独立作废；释放active，保历史和真实后继link |

这三个新动作只加入DCC_CONTROLLED_FILE profile；MES_ROUTE和DCC_REGISTRATION_CERTIFICATE不支持，旧REGISTER/PUBLISH/FINALIZE/SUPERSEDE路径不改。StateMachine精确新增三transition；isActive只ACTIVE，openCandidate集不纳pending；无需增加宽泛状态fallback。新pending/domain并不代表正文可执行，也不代替DCC最新受控指针。

现SQL20260718_controlled_content_lifecycle.sql:146–176真实源码声明status为varchar64/domainvarchar128，unique索引基flags，没有ENUM/CHECK状态白名单，因此源码设计预计不需DDL；Root在真实库read-only再核实际schema，不能把源码DDL当真实部署证明。现Core读SelectOpenCandidate/SelectActive用flags无需新statusquery；HealthCheck只count flags保持。RegistrationProjectionService专属于注册证，不使用新动作，其原contract不改。全局valueOf会识别新enum，未见其他status switch消费需要扩大源。

## 有限生产文件/所有权

1. system/enums/controlledcontent/ControlledContentCanonicalStatus.java：新增pending值。
2. system/enums/controlledcontent/ControlledContentTransitionAction.java：三个精确动作。
3. system/service/controlledcontent/ControlledContentStateMachine.java：三允许transition，pending不可编辑/非open/非active。
4. system/service/controlledcontent/ControlledContentTransitionProfile.java：仅DCC_CONTROLLED_FILE支持。
5. system/service/controlledcontent/ControlledContentLifecycleCoreService.java：新增限DCC精确批量activation方法或只复用transitionVersionRefByDomainEvent+linkSuccessor；必须准确锁/身份/CAS/同事件审计载荷一致。不能改原createCandidateRefguard，不给MES/证书新能力。已有方法replay仅存在audit即return的宽合同不得成为新DCC修复payload旁路。
6. system/dal/mysql/controlledcontent/ControlledContentVersionRefMapper.java：如现BaseMapper缺锁读，则只新增exacttenant/type/contentkey/nativeversion FOR UPDATE/CAS helper；全unique索引保留。
7. dcc/service/file/DccControlledContentAdapter.java：明确completeNativeControl和activateNativeControl/obsoleteLowerRefs，校原身份/currentBPM并复用正式core；不变recordSubmitted旧caller或GET同步。
8. dcc/service/file/DccControlledFileLifecycleService.java：在completeControl受控samephysicaltxn完成sharedpending，在activateLocked里按已有真实lowercontrolled/DCC新active更新shared；先锁原Master→全部File→对应sharedrefs固定顺序，旧DCCproperty/current/latest/签名/BPM不变。

若core的新方法需要auditMapper增加event精确绑定只属上述closure，不新增table。G58 Workflow/培训3source不要因旁路问题重写；新改该list有限点会明记新版本supersede对应G58/G57seal，其他冻结资产保留。

## Caller/consumer闭环

FinalizationService.activateRevision(native)→Lifecycle.completeControl；completeControl先实际受控artifact/时间/Gxp/outbox，然后sharedpending，同一天再activateLocked两步同transaction、最后双方ACTIVE；任一failure双方与outbox一起回滚。原FinalizationService.recordFinalizationStarted只过渡起点保留，不能提前ACTIVE。

Quartz DccControlledFileActivationJob→Lifecycle.activateDue→activateLocked：同租户实clock/duepredicate、actualpendingfacts；高受控版未来日期未到不改变当前执行，低pending如已被高active覆盖需同步sharedOBSOLETE而不能复活。独立ObsoleteService已有platformAdapter.recordObsoleted需复核它exact原ACTIVE先被修复，不能扩大作废流程或复写旧申请。

现RelationControlledEventConsumer监听CONTROLLED只生成既有整改outbox，新增shared同步直接在正式Lifecycle service/core同txn调用；不新增async监听、GETrepair、afterCommit成功假象。DCC最新受控关联与引用投影继续从DCC，canonicalpending不让关联先当正文ACTIVE。

## 显式历史投影修复入口（仅准备；Root下一授权具体实现）

建议原详情管理操作增加“校验生命周期投影”专用维护入口：GET `/{id}/lifecycle-projection-repair-preview` 只读（或同现DccWorkflowLifecycleController独立path），POST `/{id}/repair-lifecycle-projection` 显式command。前端受现admin配置维护权限可见，后二确认，填写实际“修复原因”，展示唯一tenant/File/Master/ref、原canonical/domain、真实DCC状态、原控制/生效/BPM/签名/artifact、预计只更新sharedref/audit，不改原DCC。仅taskown样本提交；不新增通用后台平台或SQL执行页。

请求合同只有expected file/master/ref/currentBPM、expectedCanonical=FINALIZING、原ref完整preimage摘要、sourceFactsHash、idempotencyKey、reason。服务器从真实同tenantFile/project/Master+完整保存审批签名/官方验签+已结束真实原BPM+CONTROLLED/ACTIVATED event+受控artifact/time查全部事实，不信任client approved/actor/time/状态布尔。Preview收到的proof摘要不能授权限，POST重锁重读同facts。actor必须当前真实enabledadmin/doccontrol维护身份、现dccquery/update权限及file硬scope，不adminID常量/假LoginUser。Root确认是否用现技术维护权限而非新菜单；保原业务View资格。

允许样本预像：File4026 ACTIVE，Master2054545668044079116 latest/current均4026；ref65592 exacttenant1/type/key/nativeversion/version A/1 canonical/domainFINALIZING open1 activeNULL；原actualcontrolled/activated/approved日期、真实结束BPM/VALID签名、artifact全部完整，无其它同Keyopen/active。pending样本可修到pending但不能伪造active；此轮具体只4026精确scope+必要另真实已控4028需Root先sealed名单，不自动扫全库。POST重放相同key/source/preimage返回已有审计结果，参数漂移拒绝；预像已自然变化明确拒绝，不重新猜源。

修复调用新正式adapter/core COMPLETE_CONTROL与ACTIVATE_CONTROLLED产生真实transitionaudit，原refid/source/history不删不换；修复audit的时间是本次实际执行时间，不冒充原批准或控制时间。原business controlled/activated occurrence单独携带保存事实。不手造签名/QA记录或直接更新openflag。正式Gxp如要求覆盖新的技术repair操作，需一个明确`dcc.controlled-file.lifecycle-projection.repair`配置（真实actor、reason required、before/after，no fabricatedsignature），由Root在现开发配置框架准备并执行；不能为了不加配置借用“新受控”operation伪造本次又批准/盖章。操作与core共享同txn，lateGxp/transitionaudit异常全rollback。Root授权用户修不足已存在，本owner不额外问技术许可；实际具体数据写由Root在review最终impact后执行。

## 有效BDD与收敛验证

- 旧实际Lifecycle.completeControl+真实core/ref FINALIZING场景，When当日生效完成，再public/newcandidate，Then原core仍open拒是有效RED；依赖/缺class编译不算。
- Futurecontrol真实DCC/sharedpending一致，openNULL/activeNULL，旧active不变，另审批candidate允许；当天及到期新active+全部低正式控版OBSOLETE，两边、uniqueflags、successor真实一致；higherfuture未动。
- ActualH2锁/unique并发与lateaudit失败回滚双方；replay同event/hash零新audit、wrongversion/BPM/tenant/另一opencandidate强拒。system原core/stateMachine/MES/注册证相邻合同回归，DCC日期/旧历史回归限必要现caller。
- Explicitrepair exactpreview/POST authenticatedactor、change预像拒、缺control/签名/BPM/artifact拒、latefailure回滚、samekey0extra；不执行实库或用API代Root真实页面验收。TDD实现待Root本设计review明确授权。
