# G59 新共享状态交接核心方向独立审查

Status: ready_for_closeout — G59 最终 R2 独立源码及保存证据复核完成。Core R01/R02、Repair R03 均已闭合；未发现新增确定主线 P1。以下早期缺口段落保留为历史，最终结论见末段。本 Agent 没有修改生产/测试或运行 Maven/DB/API/UI/服务/Git；旧差异样本修复及后续真实升版验收仍由 Root 执行，本文不关闭整体业务目标。

## 主架构方向成立

- Finalization原生事务382锁Master、383锁File，Lifecycle.completeControl的closeLowerWorkingIterations先锁完整File链，再调用sharedControl。后续activateLocked重复同链读并非shared→未持有File逆序。定时activateDue同样Master→File→chain→shared，shared `selectChainForUpdate`按准确tenant/type/key、nativeVersion/id排序。未发现该正式native主链新增锁顺序冲突。
- COMPLETE_CONTROL从FINALIZING进入新CONTROLLED_PENDING_EFFECTIVE，两个uniqueFlag为NULL；该状态不在OPEN候选集合，也不等于执行ACTIVE。未来日期旧active保持，下一审批候选不再被已完成受控的旧FINALIZING误阻；原open唯一guard未删除。
- 激活时`obsoleteLowerControlled`对真正较低受控ACTIVE/pending同步原生OBSOLETE、实际作废时间、20年占用及共享OBSOLETE+后继，然后新candidate共享ACTIVE。共享core在新ACTIVE前拒任何其它activeflag1，避免临时双ACTIVE；高版已执行时低pending被作废、不倒退执行。原control/date/Gxp/关系outbox在同物理事务，不能新加afterCommit假同步。
- Adapter.recordSubmitted28–42对nativeUP/REVISION取实际revisionSourceControlledFileId，核Master.latestControlled精确同ID和nativebaseline事实，经DCC专属createDccCandidateRef链接该sharedsource；不盲getActive替正文源。原MES/注册证createCandidate仍按原ACTIVE来源校验，不从新增pending得权限。
- 三个新增action只DCC_CONTROLLED_FILE profile支持，`requireDccKey`亦拒非DCC；stateMachine全局认识新态不代表其它profile可用。注册证专属READY/ACTIVE投影校验未放宽，旧legacy finalization分支保留。

## R01 原tuple漂移可被正常transition悄悄消除（P1）

当前Core `transitionDccControlledRef`202只检查from canonical，未核from domainStatus及原active/open tuple；206–213 CAS虽含tenant/type/key/from/process及rowcount1，却没有原domain/flags条件。例如真实FINALIZING ref被损坏为openNULL（应1）或domainACTIVE，正常COMPLETE_CONTROL仍会更新为pending并清flags，用一个正常受控事件隐藏此前漂移。这与本批“严核前像、不自动repair、不松共享guard”合同不符。

最小修正：锁链后按当前canonical检查domain与真实uniqueFlag预期（FINALIZING=NULL/1，pending=NULL/NULL，ACTIVE=1/NULL），CAS包含这些精确原值/NULL条件；链内同identity/count及要链接successor的nativeMaster/formal状态一致。DCC createCandidate source105–107现在只接受sourcecanonical为ACTIVE/pending，同样应核它的domain/flagtuple，防引用一个矛盾来源。一个真实H2数据漂移参数case验证明确拒绝且原行/审计零变化即可，不重构通用旧Core或扩大平台。

## R02 replay可遗漏原后继载荷（P2）

当前Core189–200已比原事件重放更严格，校验action/from/to/actor/reason/version/process/当前after状态和flags；但197只在本次successor非NULL时比较两个saved successor。若原OBSOLETE事件已link后继X，同eventKey相同主体/原因改传successor=NULL，会跳过此载荷核验并return旧ref，误把不同请求作为相同事件成功。

应准确比较successor包含NULL语义：本次NULL对应ref两successor字段均NULL，非NULL必须精确native/refID。要求本次动作payload与既有审计/state完全一致，不catchconflict后默认成功。两动作的真实caller仍由Adapter发稳定值，不为修此增加REST或客户端随意调用；同class原事件后继遗漏case足够。

## 验证与源码边界

已看到Owner20:18:51原green，真实H2/core/lifecycle组合方向有意义，但本审查不跑/不凭这日志声称R01/R02已关闭。新版Caller/Event应继续真实成功受控、待生效和日期切换组合，latecore/Gxp失败双方回滚，另一真正open候选仍拒；原G55共有投影漂移的精准reconcile与实际执行尚另待Root计划，不因新态实现自动补旧数据。

read时锚点：Core.createDccCandidateRef101–109、transitionDccControlledRef169–215；Adapter.recordSubmitted24–49、recordNativeControl52–56、recordNativeActivation59–65、recordNativeObsoleted68–78；Lifecycle.completeControl61/75、activateLocked125–160、obsoleteLowerControlled165–194。Owner还在开发，最终source pin必须另核，不把途中hash当最终freeze。

当前read rawSHA：Core `2add559a04caf2524cce679d88538fa8481d561df757d7ec34eb6de3653381e0`；StateMachine `e858752015e98fc3dae1837a6d9a74e7682a330c710d088831998df698fc4588`；Profile `dedafa6c41808a3fe4280aece80a8eca33dfb2239bbf934bf1486f718265a97f`；Mapper `b4f1400049316c9714197868302ce2328d690c7b7f6afdb0a576131a7490476e`；Adapter `873051b489cf300867abc95977d11a386947f507ab49dbbf54d2e62e07ccaa37`；Lifecycle `5236cdf476b2f9528b24c7b6b428f10619417b6e1e1e696e81bd02857a93a767`。这些只用于区分后继明确变更，不替代最终manifest。

## 21:17后继Core R01/R02闭合

当前Core SHA `a55319b208dedb193c33c05c91bfa199efaf5b2a6a5d716c34af2025e0c998fb`已实现明确反馈：204–208核原canonical/domain相等与open/active tuple；212–222 CAS同时含当前tenant/type/key/canonical/process/domain及两flag精确值或IS NULL，rowcount1。DCC候选来源及successor都经`requireDccControlledTuple`233–238校Master/版本/可控态/domain及flags，不能静默认漂移。199–200重放精确比较successor两字段，包括NULL，不再只对非NULL比较。DCC-only profile/旧源守卫保持；当前主状态交接未发现额外确定P1。本Agent未重跑8项新core或最终相关回归，实际日志及编译pin由Root/Owner封存。

## 新补投影Service的有限审查

`DccLifecycleProjectionRepairService`当前SHA `07d42ef5266172f68a96208331b1a6ce5fa3f3ddf5e2b4ee3d8160952c42521f`：preview为readOnly，只有实际数据读取/正文GET/证据核验，未写currentset/历史或补shared。POST在事务下先Master79→File80→sharedchain181锁，再校client期望identity、server sourceFactsHash/完整chain preimage，转换shared两态及真实Gxp append都在同物理事务。仅更新共享ref/追加transitionAudit/Gxp，没有File/Master/BPM/签名UPDATE。

当前身份为真实LoginUser同actor/tenant、正式启用目录与doc_control角色、categorymanage/query/update三个现权限、assignment hard scope；Controller也核三权限，无泛admin旁路。Proof核native受控/生效日期/正式版本、真实结束同BPM tenant/businessKey/status、冻结部门/批准/文控署名；实际tenant签名先过滤再`DccFrozenApprovalSignatures.requireComplete`，每个证据由正式`verifySignatureEvidence`核HMAC/source bytes、image与受控copy binding（核现正式实现929–1002），APPROVE再核原已办Task身份/状态。Artifact额外GET核metadataID/config/path/尺寸与实际SHA，属于只读证明，不造正文或签名。Replay核原operation/actor/subject/reason/commandHash/preimage、当前sourceFactsHash及原after projectionHash，晚审计失败应回滚shared和审计；不靠仅相同key放行。

Root已实际执行两库一条修复审计规则首次/重复并证明旧保护表0改/qualityrows0，属于Root实证，不由本Agent读取库或新增批准资料。本review未发现此事务/身份路径新的确定主线缺口；具体旧4026/65592执行仍须Root真实preview/POST与保护前像核验。

## Repair R03 事件证据没有核完整身份（P1，有限补严）

当前`prove`173–179查询保存事件按tenant/file/BPM过滤，后仅CONTROLLED/ACTIVATED各count及occurred_at匹配File时间；没有核这两事件的master_id、version_no、event_key。一个tenant/file/round/time恰好相同但Master/版本/键错误的事件，会被当作完整“已发生受控/生效”证明；将错误事件纳入sourceFactsHash只冻结了错误载荷，不能证明其正式正确。该缺口具体触发无需伪签名或松当前权限，属于本次精准proof主合同。

最小修正使用现正式Lifecycle241的稳定键 `DCC:<tenant>:<file>:<BPM>:CONTROLLED/ACTIVATED`，核event master/version及已有tenant/file/round/occurred_at全一致，保持合法pending无ACTIVATED。仅读验证，不改原事件或猜timestamp；必要一个H2事件master/version/key漂移参数case准确拒绝且零shared/Gxp写，不扩大修复平台/新审批。Root/Owner已收到反馈；此R03不是Core先前两个缺口的重复。

## 最终 R2 闭合与冻结证据

Repair 最终 SHA `b632351fa148f5647448890f999f5b91bcaaa535e411697c9820d4c04dd261e0`。`prove`177–179 已调用 `exactEvent`241–249，精确核 tenant、File、Master、版本、BPM、事件类型、正式稳定 event_key 及实际发生时间；pending 仍不得存在 ACTIVATED。原 R03 已关闭，没有删除、重建或回填旧事件。Core 最终仍为 `a55319b208dedb193c33c05c91bfa199efaf5b2a6a5d716c34af2025e0c998fb`，先前 R01/R02 修正保持。

独立只读核 `g59-backend-fingerprints-r2.json` SHA `d471f18748ab6fab87e0a52f13ee0ebf752e37f9858c95846e4e310d4b86232b`：13 个生产文件、5 个测试文件、15 个编译 class 的实际 bytes/SHA 共 33 项均匹配，0 漂移。没有把未知源码变更当格式化；Repair 与其专属测试的明确增量由 R2 receipt 记录，旧 manifest/RED/回归保留。

已保存的事件身份有效 RED XML 为 1 项测试、1 失败、0 错误（错误 Master/版本但同 File/BPM/时间，旧 proof 未拒绝）。最终 Repair R4 为 6 项、0 失败/错误/跳过，结束时间 2026-10-05 21:26:02 +08:00；RED 与最终 GREEN 四个 XML/log 实际 bytes/SHA 全匹配 receipt。源码测试169–173确实修改事件 Master/版本并断言拒绝、共享 ref 仍 FINALIZING、Gxp 事件零新增；事件键精确匹配另由实际 `exactEvent` 源码核验，不声称该单例已逐项覆盖所有事件字段。

原最终相关回归为 143 项/9 组，21:01:12 CLI 0，所有失败/错误/跳过均 0；其中旧 Repair 4 项被本次 6 项替代，两个数字有重叠，不合并成 149 项独立测试。Repair 用真实 H2/core/mapper、正式 Gxp kernel 和同一事务管理器验证提交、精确重放、晚 Gxp 失败回滚；账户/BPM/签名核验/正文读取为明确隔离 port，不能称实际人签名或实际页面验收。

最终源级判断：preview 只有正式只读证明；POST 保持 Master→File→共享链锁、完整当前来源/前像及请求重放、精确 native 已受控状态和历史签名/BPM/artifact 核验、真实身份/租户/权限/assignment scope、共享转换与 Gxp 同物理事务。原 File/Master/BPM/签名/生命周期事件不被修复入口改写。未发现新增确定主线 P1/P2，无需额外平台、审批或泛修复入口。本次独立审查冻结；剩余旧 4026/65592 精确实际修复、正常/未来日期升版的真实页面结果由 Root 另留证，不能用源码与隔离测试替代。
