# eDHR 主流程待完善列表

更新日期：2026-09-28。保留 15 条原始审计记录，其中 8 条为运行阻塞问题、6 条为后续完善/条件性风险、1 条已撤回阻塞判定。08～15及追加剩余问题已完成代码修复和主Agent验收，最终607项回归全部通过；下文原始证据保留用于说明根因。最新代码与测试状态见 [修复验证报告](../../doc/tasks/20260928-edhr-flow-08-15-fixes/verification-report.md)。未执行真实业务联调或 E2E。

本轮完整证据、影响边界和建议验收见[主链连续运行 BUG 报告](E:/IntRuoyi/docs/bugs/20260928-edhr-flow-runtime-blocker-audit.md)。下列编号延续原清单，不因优先级排序而重新编号。

## 本阶段处理原则

- 用户要求：前期限制不要过严，优先让多人协作流程能够向前推进，异常后能够恢复。
- 主链：ERP 同步 → 工艺路线与 QA 配置 → 生产/PQC 组长配置人员 → 加入活跃工单 → 一线生产/PQC 提交 → 双 100% 完工并申请 → PQC 生产放行 → 上传资料 → 上市放行 → 系统自动归档。
- 系统自动归档，没有人工归档负责人。本阶段指上市放行后记录由系统留存并可历史追溯，不擅自追加自动生成 PDF 的要求。
- 用户后续已授权修复08～15；01～07仍按原处理范围保留，资料齐套、权限细化、审批证据加强等暂不追加为运行硬门槛。
- P1：影响主流程继续执行或异常闭环，优先修复。P2：特定操作组合的阻塞、可恢复问题或后续完善，按条目分类区分。风险项：需补充具体业务数据验证，不宣称已复现。

## 总览

| 编号 | 问题 | 分类 | 当前处理 |
| --- | --- | --- | --- |
| EDHR-FLOW-15 | 一张工单已完工等待放行，锁住同组长其他工单的生产复核 | P1，多工单连续运行阻塞 | 静态确认；只检查本次实际涉及的订单 |
| EDHR-FLOW-14 | 多输出物料分次报工被相加，组长确认误判超产 | P1，正常分次报工阻塞 | 静态确认；统一按物料的数量口径 |
| EDHR-FLOW-09 | PQC 判“不正确”后无法重新提交并确认 | P1，PQC 退回恢复阻塞 | 静态确认；补齐保留旧证据的重提周期 |
| EDHR-FLOW-10 | 已确认并汇集的 PQC 记录更正时重新汇集报错 | P1，已确认记录更正阻塞 | 静态确认；更正不能复用仅首次汇集入口 |
| EDHR-FLOW-11 | 完工前发起不合格评审，却要求完工后才创建的批次 | P1，完工前异常处理阻塞 | 静态确认；打通真实页面与正式来源 |
| EDHR-FLOW-08 | QA 判返工后，生产提交仍被已完工状态和历史放行申请锁住 | P1，返工闭环阻塞 | 优先修复；静态确认 |
| EDHR-FLOW-12 | 升版后移除新活跃订单，再加入时报历史不唯一 | P2，组合操作阻塞 | 静态确认；区分已被替代历史与可恢复记录 |
| EDHR-FLOW-13 | 两名生产组长可同时启用同一正式员工，一线却要求唯一归属 | P2，可恢复的人员配置阻塞 | 静态确认；停用多余关系可恢复 |
| EDHR-FLOW-03 | 原“自动归档未完成”判定不适用：历史追溯不依赖 PDF | 已撤回阻塞判定 | 当前无需修复；PDF 自动生成未纳入需求 |
| EDHR-FLOW-01 | 资料未齐也可上市放行；放行后又不能补传 | P2；宽松策略与补传衔接 | 保留宽松门槛，明确上传时机；补传需求另行完善 |
| EDHR-FLOW-02 | 六类业务检查直接记为通过 | P2，检查结果真实性 | 暂不加严流程；后续区分未检查和通过 |
| EDHR-FLOW-04 | 带申请 ID 时，资料操作缺少订单归属校验 | P2，多人协作权限 | 后续完善同租户内的业务范围校验 |
| EDHR-FLOW-05 | 审批中资料可修改，放行快照未包含资料文件 | P2，证据一致性 | 后续完善文件版本与审批证据绑定 |
| EDHR-FLOW-06 | PQC 人员重新启用未检查是否已归属其他组长 | P2，人员归属 | 后续完善；暂未证明会直接卡死提交 |
| EDHR-FLOW-07 | ERP 更新工单后，执行快照与实时工单字段可能不一致 | 条件性风险 | 先验证具体变更场景，不强制禁止全部 ERP 更新 |

## 优先解决的闭环问题

### EDHR-FLOW-08：QA 返工后不能恢复一线生产

- 触发：生产和检验完成，生产组长提交放行申请；在 PQC 生产放行或上市放行阶段发起不合格评审，QA 选择“返工”。
- 员工表现：QA 页面显示“已确认返工，返回主流程”，组长列表也会隐藏已结束的返工申请状态，但原活跃订单无法正常继续提交生产数据。
- 静态证据链：
  1. 完工处理调用 `markCompleted`，将 `businessStatus` 写成 `COMPLETED`（S02:122）。
  2. `dispose` 返工分支恢复批次状态、解除工单临时冻结、关闭旧放行申请，但没有恢复活跃订单业务状态，也没有建立新的返工完工回执（S01:320）。
  3. 一线生产提交要求业务状态必须为 `ACTIVE`（S03:61）。
  4. 即使仅把业务状态恢复，`resolveReleaseApplicationLockedActiveOrderIds` 仍将所有历史申请视为锁定，没有排除 `NONCONFORMANCE_REWORK`（S04:94）。
  5. 与此不一致，组长列表会跳过返工关闭的申请，申请生成服务也支持新的返工周期业务键（S05:2685；S06:451）。
- 排除已有恢复入口：普通重建要求不存在放行申请；有历史返工申请时仍会拒绝（S05:1622）。不能把清理历史数据当成正常返工流程。
- 影响范围：不是所有正常订单都会卡住；已经完工并进入放行后的返工循环会受影响。生产提交这一环已足以使返工无法闭环，不据此泛称所有 PQC 入口都相同。
- 修复边界：让 QA 返工后的订单状态、历史申请锁、返工周期和完工回执保持一致。保留旧周期证据；不通过删除历史申请、跳过签名或全局放开状态来绕过。
- 关联风险：`completeForRelease` 会重用旧完工回执；源数据改变时 `matchesReceiptSources` 会报幂等冲突（S07:68、121；S08:94）。后续修复不能只放开提交入口，还要验证返工后重新完工的回执版本处理。
- 验收建议：Given 已完工并进入放行的订单；When QA 判返工、员工完成返工录入、组长再次完工；Then 新周期能重新申请、PQC 放行、上市放行，旧周期证据保留；同时验证二次返工。

### EDHR-FLOW-09：PQC 判“不正确”后无法正常重新确认

- 触发：一线 PQC 提交后，组长判“不正确”；员工或组长纠正数据后想重新确认。
- 原因：复核服务将已有 REJECTED 也视为终态，只接受原请求回放；检验任务留在 SUBMITTED，一线不能带新内容重提，更正服务也不重开复核周期。完工只计 CONFIRMED 任务，无法达到检验 100%。
- 修复边界：保留旧拒绝与更正证据，建立重新提交、复核、确认周期；不通过删除历史或自动批准解决。验收需覆盖第二次退回。

### EDHR-FLOW-10：已确认 PQC 记录无法通过更正功能保存

- 触发：PQC 已确认/已汇集但尚未放行，需要纠正样本值等数据。
- 原因：更正更新正式检验表后，再次调用只接受 PENDING 的首次汇集方法；记录仍为 AGGREGATED，抛错并回滚更正。
- 修复边界：提供保留修订证据的汇集更新路径，确保明细、事件、任务和后续回填一致。未确认记录的更正不由此例证明失败。

### EDHR-FLOW-11：完工前不合格评审错误依赖下游批次

- 触发：尚未双 100% 完工，PQC 组长从检验记录发起不合格评审。
- 原因：实际页面只传 activeOrderId，后端立即要求唯一批次执行；当前主链批次却要完工回填后才创建。后端另有 PQC_SUBMISSION 分支，但该页面没调用。
- 修复边界：让完工前评审使用当时真实存在的 PQC 提交/活跃订单来源，后续关联批次；不能要求先批准问题数据再完工以绕开。

### EDHR-FLOW-12：升版后移除再加入被历史不唯一阻止

- 触发：工单正常升版，随后在未放行时移除新的活跃订单，再重新加入。
- 原因：升版保留旧 REMOVED/VERSION_UPGRADED 记录并创建新记录；普通加入把所有 ACTIVE/REMOVED 记录都算作可恢复历史，多于一条即拒绝。
- 修复边界：按正式版本关系区分已被替代记录和当前可恢复版本，不删除历史。正常升版后直接继续生产不必触发此项。

### EDHR-FLOW-13：生产员工配置可保存双重归属，一线入口却拒绝

- 触发：A、B 两名生产组长都关联并启用同一正式员工。
- 原因：关联只检查当前组长内重复，启用也不校验跨组占用；一线工作台和实际员工校验却要求唯一启用的负责组长。
- 恢复/修复边界：停用多余关系可以恢复。配置端应遵守当前唯一归属合同或提供明确转组；本项与尚未证明会卡住一线 PQC 的 EDHR-FLOW-06 分开登记。

### EDHR-FLOW-14：分物料报工被错误累计为超产

- 触发：工序两个输出物料各计划完成 100，员工分别提交甲 100、乙 100，冻结超产比例小于 100%。前端明确允许只填一个物料。
- 原因：初始分配各记进度量 100；确认时按订单/工序跨物料累加为 200，再按计划量 100 校验上限。正式进度却是各输出物料累计量的最小值，仍为 100，口径不一致。
- 修复边界：以正确物料维度校验超产，并贯通分配、正式复核与完工；不虚增超产比例，不强迫把分次真实作业改成一次虚假记录。

### EDHR-FLOW-15：待放行工单阻止同组其他工单复核

- 触发：同一组长的 A 工单已完工等待 PQC/上市放行，继续确认 B 工单生产报工。
- 原因：确认服务把该组长所有 activeStatus=ACTIVE 的工单一起做写入锁检查。A 完工后仍为该状态，但 businessStatus=COMPLETED，因此 B 的复核也报已锁定。
- 恢复/修复边界：A 上市放行关闭后可解除，不能说放行后永久卡住下一单；等待期间已影响多单连续作业。只校验当前报工原有及新目标分配涉及的订单，并保留真正相关订单的锁。

09 至 15 的逐项前端入口、调用方法、状态写入、源码行号、例子和建议验收统一记录在[完整报告](E:/IntRuoyi/docs/bugs/20260928-edhr-flow-runtime-blocker-audit.md)，以上条目均尚未修复或运行复现。

## 已撤回的阻塞判定

### EDHR-FLOW-03：历史追溯已与上市放行衔接，撤回“自动归档未完成”判定

- 用户指出：点击上市放行后已经能够进行历史追溯。此前将“另行生成最终 PDF”当成系统归档的必要步骤，属于审计范围判断错误。
- 复核证据：历史追溯页面固定传 `releasedOnly: true`（S23:138、160）；后端按存在 `RELEASED` 放行事务筛选批次，没有要求先存在归档 PDF（S24:132、160）。
- 详情链路：历史列表通过 `batchExecutionId` 打开订单详情（S23:201）；详情服务核对批次与活跃订单来源后调用 `getArchivedFormalDetail`（S25:23），不依赖人工归档任务。
- 上市放行本身会保存放行决定、追加追溯证据、收尾上游状态，并自动将主链批次标记为已归档（S09:754、820）。
- 当前结论：上市放行 → 系统历史留存/追溯已有代码链路，不将此项作为主流程阻塞 BUG；无需新增人工归档负责人。
- 另一个独立功能：`generateArchive` 生成最终 PDF，仍检查人工任务（S10:4906；S11:894）。这不证明历史追溯不可用，也不意味着原始记录没有保存。用户未要求自动生成 PDF，本项不转成新增开发任务。
- 验收边界：本轮只做代码复核，未运行 E2E；不据此保证每条历史数据、每个用户权限均无其他问题。

## 后续完善项

### EDHR-FLOW-01：宽松放行和资料补传的衔接

- 事实：PQC 批准直接进入 `MANAGER_RELEASE_PENDING`，返回空的上传任务列表（S12:153）；最终放行传入 `materialGateRequired=false`（S09:540）；页面也明确提示资料上传不是确认前提（S18:430）。
- 当前决定：按用户要求，不把资料齐套设为本阶段新增硬门槛，也不把这种宽松策略本身算作运行 BUG。
- 需关注的实际操作边界：上市放行后，`assertMutable` 明确禁止上传和删除（S13:230）。若同事先点上市放行，再想补资料，会被拒绝。按既定“先上传、后上市放行”顺序操作可避开；如需要后补，应另行提供受记录的补充资料流程，而非强制增加前置审批。
- 验收建议：分别覆盖放行前上传、允许缺资料放行、放行后补充资料的业务决定。

### EDHR-FLOW-02：六项检查结果直接标为通过

- 事实：活跃订单正式事实分支为 DHR、检验、偏差、返工、报废、库存六项调用 `activeOrderFactCheck`，统一返回 `PASS`，没有逐项读取相应业务状态（S14:107、150）。
- 影响：不会因为这六项检查而卡住当前主流程，但“通过”可能被员工误解为系统已真实核验。
- 待完善：保留宽松流程，同时区分“本阶段不检查”和“检查通过”；将来启用某项门槛时再增加对应真实检查与清晰提示。

### EDHR-FLOW-04：资料操作缺少订单范围校验

- 事实：`resolveContext` 仅在 `applicationId == null` 时检查生产组长归属；传申请 ID 时只检查申请和订单对应关系（S13:203）。Controller 使用的权限包含组长维护、PQC 批准等较宽功能权限（S19）。
- 影响：同租户内，具备接口权限的人可能对其他组的订单资料进行操作。未据此认定存在跨租户访问，也未将其认定为所有人的正常上传阻塞。
- 待完善：按订单归属、任务候选或明确资料维护授权校验，不以是否传申请 ID 决定是否检查。

### EDHR-FLOW-05：审批证据未绑定资料文件版本

- 事实：资料在上市放行前仍可上传/删除（S13:230），`activeOrderFactsSnapshotHash` 包含订单和申请事实，但没有文件清单、版本或文件摘要（S15:22）；上市批准时重算的也是该类事实摘要（S16:164）。
- 影响：审批期间换过资料，快照比较仍可能通过；属于证据一致性问题，不是本阶段顺序流转阻塞。
- 待完善：冻结审批所看的资料版本，或把变更明确记录为补充资料；不直接新增整套人工审批链。

### EDHR-FLOW-06：PQC 人员启用可能产生双重归属

- 触发：A 组长停用某检验员；B 组长关联该检验员；A 再启用原关联。
- 事实：新增关联时检查其他组长占用，`updatePersonnelStatus` 只更新启停状态，没有重复同样的占用校验（S17:103、125）。
- 影响：同一个人可能同时出现在两个 PQC 组长范围中。当前静态证据不足以宣称它一定导致个人工作台无法运行。
- 待完善：启用时校验当前归属；若业务允许共享人员，应明确显示其多个授权范围。

### EDHR-FLOW-07：ERP 工单更新与冻结执行事实不一致

- 触发条件：工单加入活跃执行后，ERP 再同步并更新同一工单的数量、批号等字段；部分产品变更可能形成新的来源键，不一概视为覆盖原工单。
- 事实：`buildUpdatedWorkOrder` 写入 ERP 新字段（S20:413）；活跃订单保留已有路线、QA 和数量快照；正式回填又读取当前工单产品和批号（S08:122）。
- 影响：可能出现旧执行事实关联新工单字段。若已有完工回执，再发生会改变源摘要的变更，重新完工申请也可能因源证据不一致被拒绝。
- 当前分类：条件性风险，未使用实际 ERP 变更单验证，不把全部同步认定为必然卡死。
- 待完善：明确执行中允许变更的字段、差异提示和受影响事实的更新规则；先覆盖“改批号后返工重提”的具体场景。

## 已核对的正常机制与运行前提

- 完工服务确实校验生产、检验双 100%，不是只靠前端按钮显示（S07:143）。
- PQC 批准、创建上市放行事务、交接任务在事务中执行；中途失败应回滚，可在修正前提后重试（S12:124）。
- 正常重复申请、PQC 决定回放存在幂等处理，不能笼统宣称“重试一定卡死”（S06:148；S12:662）。
- 被返工关闭的上市事务为 `REJECTED`，`selectCurrentByBatchExecutionId` 排除 `REJECTED/WITHDRAWN`；已排除“只因旧驳回事务存在就永远不能重新放行”的误报（S01:395；S22:24）。
- 工艺路线、QA 生效配置、工单批号与数量、人员关系和业务权限、PQC 候选、管理者代表候选及签名凭据仍是运行前提。缺失会阻止相关步骤，但属于配置前提，不能仅凭静态代码判为 BUG。管理者代表缺失会让 PQC 批准事务一并失败，需提前配置（S21:108）。
- 资料按主链应在上市放行前上传；不要求人为增加归档办理人。
- 本次未运行真实多人联调，因此不能承诺所有配置和运行环境下均可闭环。

## 源码索引

下列路径相对仓库根目录；正文行号对应 2026-09-28 检查时源码，后续以方法名定位为准。

| 索引 | 文件 | 关键方法 |
| --- | --- | --- |
| S01 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java` | `dispose`、`closeManagerReleaseForNonconformance` |
| S02 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java` | `markCompleted` |
| S03 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineSubmitAuthorizationServiceImpl.java` | `assertActiveOrderOpenForProduction` |
| S04 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationReleaseStateService.java` | `resolveReleaseApplicationLockedActiveOrderIds` |
| S05 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java` | `loadLatestReleaseApplications`、`requireNoReleaseApplication` |
| S06 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseGenerationService.java` | `businessKey`、`replayExisting` |
| S07 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java` | `completeForRelease`、`complete` |
| S08 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java` | `matchesReceiptSources`、`prepare` |
| S09 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java` | `approve`、`closeBatchAfterFinalRelease` |
| S10 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java` | `generateArchive`、`getLatestArchive` |
| S11 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrWorkTaskServiceImpl.java` | `validateArchiveTask` |
| S12 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java` | `approve`、`replayOrRejectProcessedApplication` |
| S13 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java` | `resolveContext`、`assertMutable` |
| S14 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseBusinessReadinessService.java` | `activeOrderFactCheck` |
| S15 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseFormalFactSnapshots.java` | `activeOrderFactsSnapshotHash` |
| S16 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerApprovalServiceImpl.java` | `prepareForFinalization` |
| S17 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcLeaderPersonnelServiceImpl.java` | `updatePersonnelStatus` |
| S18 | `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` | `submitRelease` |
| S19 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesActiveOrderDossierFileController.java` | `upload`、`delete` |
| S20 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java` | `buildUpdatedWorkOrder` |
| S21 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java` | `initializeManagerReleaseStage` |
| S22 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrReleaseTransactionMapper.java` | `selectCurrentByBatchExecutionId` |
| S23 | `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue` | `buildQuery`、`openActiveOrderDetail` |
| S24 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java` | `releasedTransactionExistsSql` |
| S25 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchActiveOrderDetailService.java` | `getDetail` |

## 审计边界

- 当前结论来自源码分支、状态写入、权限/锁判断及页面调用链，不是线上故障统计。
- 上一轮记录保留审计历史；本轮确认 EDHR-FLOW-08 至 EDHR-FLOW-15 共 8 项运行阻塞，其中 6 项 P1、2 项特定操作组合/可恢复的 P2。另有 6 项完善/风险记录，EDHR-FLOW-03 维持撤回。
- 用户已明确要求跳过 AOCI；本轮后续分析未依赖 AOCI，不建立或维护索引。
- 原始审计阶段未改业务代码；后续08～15授权修复已修改代码并运行本地回归，最终结果以链接的验证报告为准。未修改运行数据库/服务，未执行E2E或Git提交/推送。

## 08～15 修复回归中保留的既有问题

以下为扩展回归揭示、与保存基线核对的遗留项，不计为08～15已修复；本轮保留失败断言，不修改测试掩盖问题。

- 冻结批次工序快照不完整时，详情和复核时间线仍可能读取当前路线配置；两项测试失败。需另行统一冻结证据缺失时的行为，不能静默补用当前配置。
- 动态表单仅配置 SIGNATURE 规则时，预览未生成签名标记；一项测试失败。需修正动态表单布局转换。
- legacy ReportConfirmationService 的手工分配未在写入前检查工序剩余容量；一项测试失败。实际页面调用 AllocationCommand，已有容量保护与通过回归；须另行清理旧入口或补齐其合同，不能把旧测试删除。

此外，本轮已修复拒收批次重做时审计来源摘要嵌套层级错误，以及与反查改动集成后的重复完工校验冲突；wave10相关回归通过。

最终更新：上文保留的冻结快照、动态签名预览、legacy容量三类问题均已修复；remaining-green13.log 607/607通过。历史静态描述与失败证据保留用于追溯，当前状态以修复验证报告为准。未部署、未做真实E2E。
