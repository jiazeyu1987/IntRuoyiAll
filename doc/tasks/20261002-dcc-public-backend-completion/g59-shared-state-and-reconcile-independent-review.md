# G59 原生受控与共享状态机交接独立审查

Status: prepared_for_root_design — 2026-10-05 20:02 +08:00有限只读架构审查与修复方案；当前确定P1，未改生产/测试、运行Maven、实际DB/API/UI、服务或Git。Root提供实际差异事实，另Owner原定位记录`g59-native-platform-lifecycle-readonly-review.md`保留；本报告不替代实际执行授权或校准写入证据。

## 确认主阻塞

Root真实A/1 ACTIVE4026→选4029 A/1-1发PARTIAL时，共享ref65592仍为canonical/domain FINALIZING、openflag1、activeflagNULL，准确同tenant1/DCC_CONTROLLED_FILE/Master2054545668044079116。DCC实际4026已经在11:51:36受控、生效且Master执行指针4026。`createCandidateRef`98–103依准确开放候选唯一守卫拒绝下一候选是正确保护，不应改成忽略DCC或清flag。

Native Finalization.activateRevision810–818调用`Lifecycle.completeControl`后return，确实跳过旧846行`recordFinalized`；当前Lifecycle的CONTROLLED/ACTIVATED只写DCC、Gxp与同步关系consumer，没有共享收口caller。G55真实初始主链因此证明DCC已受控/下发，但未证明共享候选结束和下次升版可办，最新实际P1必须覆盖原全链推导。

## 状态与唯一标记的有限推荐

现共享canonical enum没有待生效受控态；OPEN集合为DRAFT/REWORK/IN_REVIEW/READY_TO_PUBLISH/FINALIZING/FINALIZATION_FAILED，ACTIVE代表唯一当前执行版。正式schema源码`20260718_controlled_content_lifecycle.sql`155/175–176为varchar64及tenant/type/key/active、open两个nullable唯一索引，未在该SQL定义status CHECK；这可承载新enum，无新表或历史DML。真实当前结构额外约束仍由Root只读核，不从源码猜已有库完全同形。

建议增`CONTROLLED_PENDING_EFFECTIVE`作为**已完成受控且不执行**的共享状态，仅DCC_CONTROLLED_FILE profile允许对应新动作；不能用READY_TO_PUBLISH或FINALIZING假装受控完成。二者原语义仍可发布/处理中且占open，容易误锁下一申请；把待生效写ACTIVE则提前取得唯一执行资格并替代旧版，违背HTML“等新版本生效旧版才作废”。

| 业务事实/触发 | DCC真实状态 | 共享canonical动作建议 | active/open flag | 旧执行版 |
|---|---|---|---|---|
| 未提交/审批中 | WORKING/各pending | 原DRAFT→IN_REVIEW等保持 | NULL/1 | 保ACTIVE |
| 文控通过，正文处理中 | FINALIZING | 原READY→FINALIZING保持 | NULL/1 | 保ACTIVE |
| 真正受控成功，尚未到日期 | CONTROLLED_PENDING_EFFECTIVE | FINALIZING→CONTROLLED_PENDING_EFFECTIVE，DCC专属COMPLETE_NATIVE_CONTROL | NULL/NULL | 不变 |
| 当日受控可立即生效 | 同事务pending→ACTIVE | 先COMPLETE_NATIVE_CONTROL再ACTIVATE_NATIVE_CONTROL | 1/NULL | 精确低受控版→OBSOLETE |
| 到期每分钟激活 | pending→ACTIVE | pending→ACTIVE，DCC专属ACTIVATE_NATIVE_CONTROL | 1/NULL | 真正低版ACTIVE/pending→OBSOLETE并link后继 |
| 更高版本已执行，较低pending后来到期 | 低pending→OBSOLETE | pending→OBSOLETE，DCC专属RETIRE_NATIVE_CONTROL | NULL/NULL | 高版继续ACTIVE，不回退 |
| 独立作废批准 | ACTIVE→OBSOLETE | 既有OBSOLETE_ACTIVE保持 | NULL/NULL | 不造新ACTIVE |
| 原链处理失败/重试 | FINALIZATION_FAILED/FINALIZING | 既有FINALIZE_FAILED/RETRY保持 | NULL/1 | 不变 |

动作命名供Root统一，不在本报告实现。COMPLETE/ACTIVATE/RETIRE仅该DCC profile支持，MES_ROUTE和DCC_REGISTRATION_CERTIFICATE不得自动获得新动作；原发布/作废/签名协议保持。共享pending可多条而同时open审批候选仍唯一，active仍唯一；不删唯一约束、不通过允许两个active临时过渡解决顺序。

Native同一Master→File锁和物理事务内，在CONTROLLED保存与真实事件已建立后同步adapter/core完成pending；激活先精确收口将被作废低受控refs再candidate ACTIVE，状态/两flag/successor/domain均与DCC一致。晚core/audit/outbox错误须回滚双方；新DCC专属core入口采用锁定精确refs、原状态/flag/version/tenant前像及rowcount1，不借旧core只eq(id)更新掩盖丢写。不要求重构其它模块完整核心。事件幂等key使用真实已持久化CONTROLLED/ACTIVATED事件身份；同key重放仍核已提交payload及当前合法afterstate，不能仅audit存在就接受任意changedsubject。

## 消费者与来源语义

源码所有主要平台active消费者由`getActiveRef`/Mapper.selectActive读取active_unique_flag1：DCC提交/旧formcenter、MES Route adapter均如此，不把READY或FINALIZING当执行版。因此新pending必须activeflagNULL，旧ACTIVE保flag1；不扩大getActive到pending，DCC当前关联仍用原Master.latestControlledFileId投影，正文执行仍按currentActive。

注册证`ControlledContentRegistrationProjectionService#validateRefAndAudit`250–255只允许ACTIVE/activeflag对应及READY_TO_PUBLISH/openflag对应，并严格核最后审计；它按独立contentType读取投影，不能因增加DCC态就放宽注册证状态/flag规则。HealthCheck只比较active/open数量，新DCC native“open”统计应明确不纳入已经受控的pending；数量一致不代替nativeID/时间/domain精确一致。

还有一个新态引入后必须声明的边界：DCC native升版基线读Master.latestControlled，可为待生效A/2；现Adapter.recordSubmitted取共享getActiveRef（仍执行A/1）作为sourceVersionRefId/sourceNativeVersionId，Core.validateSourceActiveRef167严格仅ACTIVE。若这些source字段表示真实升版/正文来源，应新增DCC专属已受控基线来源校验并按真实revisionBase对应pending ref，而非盲用旧active或全局允许任意pending；若保它们作为“提交时执行前像”，应明确不冒正文基线，正文仍由native准确snapshot证明。Root需在有限实现合同内定清，不能一边声称shared源是实际revision基线一边继续active猜值。初始未来受控时没有执行active不代表可REGISTER_ACTIVE补一个。

## 最小源码/测试建议

必要共享enum/action/profile/stateMachine四文件；Core及Mapper只增加DCC专属严核锁定/CAS方法；DccControlledContentAdapter加pending/activate/retire桥；DccControlledFileLifecycleService把它接实际CONTROLLED/ACTIVATED/低pending淘汰事务。Finalization native分支仍以Lifecycle为唯一入口，不再并行调用旧recordFinalized；原旧分支保留。必要对应状态机/真实H2核心+生命周期组合测试，现MES/注册证有影响的原合同测试做有限邻接验证，不全库随机测试。

Given真实shared core+native Lifecycle+Mapper组合，When当日受控，Then共享旧candidate不再open、同ID ACTIVE，下一真实public升版可建candidate。When未来受控，Then共享closedpending、旧active精确不变、无提前执行，下一合法候选不会被旧finalizing误挡。When到期/更高版日期先到，ThenDCC/shared/successor/20年作废同事务，无回退执行。When晚审计/core失败，Then双方文件/指针/flags/审计/通知回滚；真实另一opencandidate仍拒。重放只一审计、不重写旧签名。测试不得把adapter mock成功当跨状态交接证明。

## 精确65592已发生事实的补投影，不造历史

对当前**initial已ACTIVE、无旧sharedactive**的一个受控投影漂移，可在严格proof wrapper内复用既有`DccControlledContentAdapter.recordFinalized(null, actualFile, currentActor, reviewedStableEventKey)`→Core FINALIZING→ACTIVE/FINALIZE_SUCCESS/正式transitionAudit。这不是REGISTER_ACTIVE新ref，不DELETE/SQL清flag，不重新受控/盖章/改日期，不调用全旧流程；“复用旧方法”只有该精确特例成立，不能用于未来pending或带旧active需要自动OBSOLETE的新版本。旧finalizeVersionRefs会将旧active SUPERSEDED，不能替代native日期模型的OBSOLETE。

Wrapper/执行资料必须完整冻结并在同Master/File/ref锁下重核：

- exacttenant1/typeDCC_CONTROLLED_FILE/key及Master2054545668044079116、File2054545668044084026、ref65592、versionA/1；shared当前FINALIZING/domainFINALIZING/open1/activeNULL，唯一该open、无其它ACTIVE/在途candidate或字段漂移。
- native File当前ACTIVE且受控/生效/预设日期/原正文hash、实际受控输出hash/大小/身份/签名绑定全部准确；Masterlatest/currentActive均该File，源ownership/正文与封存manifest对应。没有只靠可修改status或hash长度的自称证明。
- 精确BPM轮次/业务file/tenant/正式定义及实际结束通过；真实ASSIGN/各部门会签/批准负责人/文控审核的既有VALID签名及统一签名/证据摘要关联验证完整，无更换actor或制造新旧批准时间。
- 已存在CONTROLLED/ACTIVATED事件tenant/Master/nativeID/version/BPM/发生时间准确，Gxp事件与原native状态切换事实可追溯。共享原submit/approve/start-finalization审计ID/key/载荷保留，预期缺失finalize-success，修复记录只追加。
- 当前真实authenticated同tenant启用文控技术维护身份及明确Root审过的exact执行scope/reason/requestId/artifactRawSHA。新审计记录真实修复时点/真实操作者/原业务事件locator，不冒原11:51时点批准签名。开发阶段不凭空新增质量批准资料。

现`recordFinalized`核心审计使用固定成功reason，故外围必须把“补齐已发生事实”的真实原因、scope及before/after写正式现有审计管道并同物理事务；是否有真实适用的既有operation由Root核源policy，不能借无关成功规则或手INSERT Gxp。复用新专属可审执行调用，不把既有legacy39登记入口scope悄悄扩大为通用repair。任何preimage/资料不足即失败零写，没有catch后盲放行下一候选。精确重放核既有successaudit全部identity/payload及afterstate一致；Core当前仅(ref/action/key)存在return的行为不能单独作为proof。

此补投影执行当前仅方案、无actual写。若缺适用正式审计操作/可调用入口，先交最小boundedcommand设计及Root授权，再实现；不为一条漂移创建全局维护REST或repair平台。有效BDD：完整65592证据调用既有transition后仅该ref+追加审计、native原列/签名/BPM/hash全不变；下一publiccandidate正式通过；缺签名/正文/轮次/其它open/changedpreimage零写；lateaudit失败回滚共享；一致重放零新审计；wrongscope严格拒。

## 读时重点SHA

StateMachine `cb48bb252c59bcf7353e74a263f4adf2b41eefb7cddeeeb04e35bc76d55e4f06`；Core `f7c7482a04ea783732f719cb4fccb52596e679ce14d8cd51bd33c02c52e5e27c`；DCC adapter `fe4f801bafc92821084057b4160067cfd542363430cffc7445b1b924fac93299`。其它路径为现有正式文件，Owner实现后须按最终manifest复核，不以该读时hash当最终source。此有限准备不能称state扩展、旧投影修复或真实PARTIAL已通过。
