# G01 Review Ready — 当前实现 checkpoint

状态：G01/G02软件范围已Root M08正式55/55验收接收，独立50文件冻结指纹无漂移，A31错误关闭。工程收尾因当轮Git禁令标blocked、不completed；不是产品测试失败。A23 main24/DetailApplicationPanel仍G03/G06，后续整合types及其他模块由Root统一推进。

## Root T03 已通过，无旧类型等待

Root唯一T03实际pnpm ts:check exit0、无diagnostics（11:15:00—11:16:29，Asia/Shanghai）。T01四处与T02三处旧错误已在当前源码消除，本线程不自行再跑完整types；该票为源码仍并行的类型诊断，不作为新业务PASS或全项目最终freeze验收。Root receipt已只读核对，记录于official-m08-receipt.json的typeDiagnostic元数据。当前G01独立50文件再次核SHA256仍无漂移，没有重开共享24文件或扩custom/附件范围。

## 最终软件范围接收：Root M08 55/55 PASS

2026-10-15 Root唯一M08实际exit0/67秒，23reactor BUILD SUCCESS；签名依赖11+DCC八scope其余44，共55/55、0FAIL/ERROR/SKIP，八fresh XML归档。A31路径修复后V6真实模型1test八分支正式通过，不再等待M07/M08。Root明确G01/G02软件范围联合正式接收；原始结果核对并在official-m08-receipt.json归档，本线程没有重跑/复刻Root命令。

当前独立50文件SHA256与冻结manifest一致无漂移。Root整仓源仍并行有3项漂移，55仅八个明确class输入范围，不扩大为custom/附件/正文投影/12项完全验收；最终整合types T03由Root执行。G01后续仅按Root新实际失败返回，不自行解冻main24或重复23private。

软件结果可接收，工程Git收尾当轮禁止，按docs/task-closeout-rules.md最终标blocked而非completed；自有任务目录仅清理preview并归档keep/delete统计，不清他任务文件。没有任何提交/推送/实库/E2E/发布。

cleanup preview实际exit0、keep23、临时delete候选168、warnings0、required evidence全部keep、实际删除0；cleanup-preview-summary.json保留摘要。apply未执行，Git收尾未授权。task.md先ready_for_closeout preview后按明确规则最终blocked，不伪造completed或cleanup apply PASS。

## A31 已修，可直接 Root M08

Root M07实际signature11PASS，DCC44项43PASS/0FAIL/1ERROR；唯一V6测试从Surefire模块cwd只上移一层再拼IntRuoyiBackend而形成重复目录。真实Surefire properties已只读核对：basedir/user.dir均为IntRuoyiBackend/yudao-module-dcc。

仅改DccWorkflowV6BpmnTest.java资源解析：有显式`dcc.repository.root`时要求绝对合法仓库；Maven未提供该属性时只用正式basedir=IntRuoyiBackend/yudao-module-dcc，确定两级父目录，不扫描user.dir/祖先、不找替代XML、不复制下载。根AGENTS/backend pom/module pom及两production V6 XML必须存在，缺失直接fail。private runner现传同显式root属性。原2动作×2mode×2training八分支、candidate端口与全部断言不改。

目标文件独立javac及A31ModelRootProbe从实际Maven模块cwd执行PASS：module basedir、backend user.dir、显式standalone root都精确同根；missing/empty explicit root、缺basedir、wrongmodule均拒绝。只验证路径契约，没有重跑23private或冒称原8引擎分支在Maven已PASS。写前hash a31-write-baseline.json；独立归还manifest只更新此专属test指纹，生产文件仍冻结，Root可M08同8scope复验。

## 立即独立归还（A30完成）

shared/lifecycle.ts新增正式pending状态/“待延迟生效确认”label，shared/stage-name.ts新增“延迟生效确认”label；仅union/statuslist/显示，不增第五矩阵阶段、withdraw允许或任何actionProjection默认许可。actual labels RED1→GREEN3，含原历史名称与状态保留、缺/blocked投影各敏感动作均拒；和strict datetime/actualBasicInfo2组合5PASS，exit0。

自有独立全部BE/FE按independent-handback.json当前SHA256冻结，Root可立即接管；Root M06旧signature模块实际PASS/mainPASS，但DCCtestCompile因G06缺import挡住，七DCC目标未执行，此次不称confirm/end正式通过。G01不重跑23、不扩其他源，后续只按Root实际失败精确修订。若Root需要重新改本批文件，按整文件重新归属，避免与后续owner并写。

## 当前联合交付 checkpoint（2026-10-15）

G01当前自有BE源码已收口，private --suite在Root M05新main/test依赖上编译32个显式即时source，23/23PASS、0skip、sourceStable=true，exit0；receipt及输出在private-java-current-source/suite-receipt.json、suite-compile.log、suite-junit.log。包含Evidence9/HMAC及Subject7/真实producerTimestamp6+actual两验签2/公开service端口4/真实Flowable模型1。此后本线程没有BE生产修改；Root可冻结该checkpoint纳入最终联合-am，不能将前次旧deps24main编译等同本次。

G02 public-command-11实际2PASS已Root接收：公开confirm→actual统一producer/两投影/实际GxP→官方BpmTaskServiceImpl→同步End→Lifecycle/Finalization→晚审计失败全事务回滚重试。跨owner报告明确引用，不重复执行G02旧14冒新公开链。

FE自有A25/A27及展示已实测：parent exactround5/nativeprogress3/旧native8/actualBasicInfo+datetime2；A28旧testdouble修复已被Root M05全reactor main/testCompile接受；M05四目标80PASS仅其范围。剩余只读status helper中文映射请求见下文，及Root最终types/freeze联合门禁。24main与DetailApplicationPanel仍G03/G06owner，本线程不并写。

请Root阶段接收自有BE checkpoint后直接最终联合票；后续必要修复按实际失败再收敛，当前不扩展新规则、不反复重跑已PASS组合。Git/E2E/发布未授权，不标工程completed。

## A28 / M04 唯一testCompile错误已修（供Root M05）

`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/signature/policy/SignatureGovernancePolicyDccAdapterTest.java:CapturingDccSignatureVerificationService` 已补新增interface方法，严格UnsupportedOperationException拒本旧策略意外调用延迟签名；原legacy execute参数/权限/未知动作/计数断言未改。Root M04已证明当前全部main正式-am编译通过，只有该旧double缺实现挡住testCompile，目标4类尚未执行，非业务fail。现在可按Root唯一票M05复跑同4target；本线程没有执行Maven/sharedtarget/fulltype。

A27 native-approval-progress已真实RED3→GREEN3，和旧native duty8及parentround4组合15PASS。V2/V6 exact组合、主管/文控后training/DELAYED actualtask、E/S只读、历史不借当前file、立即无confirm，正式职责“原编制人延迟生效确认”。父页已传本次mode；scope缺属性三error已修、parentwatch竞态再RED1→GREEN5（route改但watch未触发时旧response也丢弃）。

新status正式中文显示最小依赖（当前未获shared helper写权）：`IntRuoyiFronted/src/views/dcc/controlled-file/shared/lifecycle.ts` 若当前缺PENDING_DELAYED_EFFECTIVE_CONFIRM标签，应新增“待延迟生效确认”/审批中tag映射及正式status union；不靠名称猜任务资格。Root可整文件转交G01或当前owner接，父页任务名称与进度已直接正式中文，不擅自扩大shared helper归属。

## T02/A25 实际轮次缺口已修（最新）

detail父页不再读不存在的ApprovalProgressScope.attributeRound；从该scope.flowContract取本次V6 `attributeRound`，再与已验applicationRoundSelection的file/BPM/lock核验。新增requireDelayedConfirmationRound位于已授application-round-context.ts；无ready/空round/错file/BPM/历史V1/未知定义均拒绝，未默认1，未cast。将scope声明提前，避免watch初始化引用TDZ。

`node --test tests/unit/dcc-delayed-confirmation-round.test.cjs`：原helper缺失RED1→GREEN4/4；真实父页loadDelayedConfirmation handler执行exact(file20,taskT,BPMR,attributeRound7)并保存response；round不记录零请求并错误；切换任务后late response不污染。加相邻application-round-context10项，组合12PASS（在补parent2项前），Root无需再按旧3070三property错误重复派单。

还需最小新图显示接线文件（当前未列G01可写，先登记）：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/native-approval-progress.ts` getNativeApprovalFlowUnavailableReason只认V1/V4，会拒V2；buildNativeApprovalProgress的publicFlow只认V1。必要hunk为正式V2/V6组合可见、主管/文控后training顺序保持、确认阶段只依本次actualTask或正式本申请mode显示，不把只存在file字段当历史证据；与native progress专属tests真实调用复核。否则新scope可取但原进度computed仍报“不支持”。请求Root授此helper或交其当前owner接，不擅自并写。

公开command测试当前3项2PASS/1待fixture：首次正向跑到FrozenGate真实缺负责人，修自有owner审批fixture后再缺Lambda metadata，已补TableInfoHelper；因其他owner共享主申请源出现custom/附件新依赖，私有编译收窄自有即时源+已归还checkpoint依赖，不把该编译阻塞称业务RED。M03正在Root独占，当前不重复大编译/不写sharedtarget，等待Root跨模块诊断。

该旧fixture阻断已复验消除：private --command最新3/3PASS，真实公开service方法在已声明mockports下成功→CAS→audit→BPM→proof，以及同键同E/reason只读回同原S；改E冲突不二签/二推，CAS失败及audit失败均验证具体原因且BPM0。新增BPM晚失败传播测试待本轮单次执行。真实数据库/官方BpmTaskService/End联合仍交G02，不扩大mock回归范围结论。

T02新parent实际round4/4PASS及相邻context10PASS（两次定向含4+10、不称根fulltypes已通过）；BrowseRecords/BasicInfo/父页新4投影已用严格上海date展示helper，保持所有关系与rawID。Root最新M03 receipt exit1仅当前owner G03 saveCustomDraft/readCustomDraft未接，不改其代码；自有即时private编译当前PASS，-am最终联合仍等Root票。

## Producer Ready：当前真实签名DB checkpoint

`DccSignatureVerificationService.verifyPasswordAndCreateDelayedConfirmationSignature(actor,file,task,process,attributeRound,E,password,reason)` 已可调用；真实 `ElectronicSignatureServiceImpl` 先已授权adapter/scope→MICROS权威S→bindServerSignedAt完整canonical/HMAC→统一证据hash/记录；DCC从同事务统一已保存canonical取最终evidence，不签后重算。fixed subject399chars；签名原因500。只有正式作用域hash绑定req能新v5签名，无scope/未知/重复/篡改/错租户/源件签前拒。

新增 `DccDelayedSignatureRoundtripTest` 的即时javac/JUnit 2/2PASS：真实统一Service+DccProducer+三实际MyBatisMapper+H2TIMESTAMP6+两实际验签器（Dcc verifySignatureEvidence/unified verifyEvidence）。权威S=result=统一DB重读=DCCprojection=文件confirmedAt=controlledTime；E篡改DCC INVALID；S篡改统一MISMATCH；latefailure真实tx回滚统一及DCC签名为0，重试成功。account/password/image/filebytes和GxpAudit delivery是明确mock外部端口，不声称password实际系统目录/审计ledger/BPM运行态已验收。

首次真实DB RED1揭示旧统一verifyEvidence以SERVER_CLOCK文字替代signedAt导致仅S篡改仍VALID；C05newMICROS timeEvidence改显式SERVER_MICROS_CLOCK使真实S参与复算，旧SERVER_CLOCK分支/历史v4/v3字节完全保留，GREEN2。不是truncate已产生S。Root/G02现可用该producer接公开confirm→正式BPM→End组合，不应再fixture手插确认signature。

private source当前含新signatureAPI/service/query、DCC producer/Subject/Evidence/Management/Command及主申请checkpoint源，javac exit0；未运行Maven/sharedtarget。最终联合M03待Root唯一票。公开ConfirmationService成功/BPM端与完整同事务证明仍待G02联合fixture和本线程mockcommand正向/replay/CASfail（不把producer2PASS当公开确认全通过）。

## 2026-10-15 立即阶段归还 Root→G03→G06（G01停止下列文件写入）

清单及归还即时SHA256见main-handback.json。Checkpoint：私有javac编译即时DO/WorkflowServiceImpl/Revision/Resolver/ExecutionContract/Bound/Rule/Signature/Command/Controller等列出源文件→退出0，command负向JUnit1PASS；既有外部module依赖来自只读旧classpath，不称-am完整源码通过。前端共享helper/applicationRead/实际wrapper组合72项PASS（见execution-log），C04/Checkin新hunk尚需专属parser/wrapper回归；剩余parent/签名组合独立继续，不占这些main文件。

归还：DccControlledFileWorkflowServiceImpl.java、DccControlledFileWorkflowService.java；DccControlledFileRevisionServiceImpl.java；DccControlledFileDO.java（延迟4字段及A22custom3字段）；DccControlledFileApprovalRouteAssigneeResolver.java（未修改G04已归还逻辑）；DccApplicationExecutionContractService.java；DccBoundWorkflowContractResolver.java；DccApplicationEffectiveSelection.java；DccWorkflowDatePolicy.java；DccLifecycleEffectiveContract.java/DccLifecycleEffectiveContractResolver.java；DccWorkflowDefinitionCatalog.java；公共DccControlledFileSubmitReqVO/SubmitIterationReqVO/WorkingApplicationSaveReqVO/RoutePreviewReqVO/RespVO/WorkingApplicationAttributes.java（多数未改，明示交还）；前端upload/index.vue、submitter.ts、approval-matrix.ts、revision/revision-model.ts、API workflow.ts/applicationRead.ts。

已接main锚点：PUBLIC_V2新准备且保留V1绑定读/重提；createApprovalProcess冻结effectiveMode/requester与确认start-map；无培训文控→单requester任务、必要trainingtrigger→单requester任务；状态PENDING_DELAYED_EFFECTIVE_CONFIRM；普通approve阶段拒绝；DELAYED/null提交意图及strictPrepared定义拒隐式升级；新显式已登记definition transition按目标catalog契约。未把确认加第五矩阵来源。

main剩余必要hunk（交下一owner/Root，不继续并写）：后续G03自定义来源/原配置、G06附件manifest，Query（G06）应将新pending状态纳入合法编制/审批source读取并映射file四投影至RespVO；G07body task读资格认可DELAYED_EFFECTIVE_CONFIRM真实任务；这些不是G01签名私有文件须继续占main的理由。对现存未获授权DccApprovalMatrixBaselineService/WorkingDraftInitializer等PUBLIC常量调用，Root最终全源码测试若发现V2缺口，按精确方法重新归属，不擅自改。

继续G01独占：签名DO/Evidence(CreateReq/Service/Impl)/Verification/Management/SubjectAdapter；C05signature模块adapter/service；新ConfirmationEvidence/Service/Req/Resp、WorkflowController、WorkflowFileStateAudit、FrozenApprovalSignatures/ControlledContentAdapter、新迁移和V6XML、专属tests；detail父页/BasicInfo/BrowseRecords/确认panel/effective-facts（新显示）。不会改TokenService（A22已G03）。

容量实际RED→GREEN：合法480中文字符reason/中文角色完整req原subject5515ASCII/UTF8bytes，H2 VARCHAR2048真实插入失败22001；server-scoped intent+fixed hash claims后399chars/bytes，原2048表保存/readbackPASS。reason统一500，未扩2048/截断/造默认。R04篡改追加JSON/E/round和缺scope拒绝；完整HMAC/actualManagement重建篡改INVALID，7项私有签名testsPASS，统一微秒签名完整DB/回滚仍待。

## 需要 Root 精确补授的签名适配文件

- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSignatureSubjectAdapter.java`：encodeSubjectId/decodeSubjectId 的 workflow envelope 判断只认v4，nodeOrder新增DELAYED_EFFECTIVE_CONFIRM→35（位于DOC_CONTROL_REVIEW30和DISTRIBUTION40之间）。必要差异：新v5 envelope保持本轮process与canonical选择哈希绑定，原v4字节不改；否则统一签名adapter无法加载确认subject或缺轮次。不增加action/新岗位角色。此路径未在原A/B明确列出，当前尚未写。

枚举穷尽switch已搜索：WorkflowServiceImpl.requiredPermission/toPendingStatus（已授，须拒普通approve绕过独立确认）、LifecycleEffectiveContractResolver（已授已接）；另SubjectAdapter字符串stage switch属于上列新签名适配依赖。

## 真实签名原精度持久化需冻结（发现现存schema差异）

当前统一 `ElectronicSignatureServiceImpl.sign` 取 LocalDateTime.now（纳秒）；20260908_system_electronic_signature_t3.sql 的 signed_at 是 DATETIME，DCC 20260513/20260515 signed_at 也是DATETIME。新签名将真实S参与DCC HMAC后重读若被数据库截秒/截微秒，验签必不一致；MySQL DATETIME最高微秒不能保留任意Java纳秒。G01不能截断S来掩盖C02要求。

提请Root冻结精确保存策略：新payload签名projection及文件confirmedAt增加明确原始S canonical文本快照（含纳秒）并供新payload getter/验签/审计消费，旧signed_at列和旧签名字节保留；或由统一签名owner提供自身正式时间精度/存储修正合同。当前G01只写已授权六列，尚未增额外列、不改signature模块。此为完整真实DB验签/重放门禁，不阻断已明确确认任务及其它TDD接线。

以上旧S文本提案已由C05替代：当前已接serverTimeResolution default ORIGINAL/new v5 MICROS与sign服务hash前tick clock，DDL精确三列升6且无UPDATE。尚需Root核准最小第二个adapter hook `bindServerSignedAt(command,authorizedSnapshot,S)`（默认原snapshot），仅v5将DCC候选canonical按权威S定稿/HMAC，再返回最终统一canonicalContent。原因：S在统一服务内部生成，现有DCC调用方只能签后得到S；现实现的签后DCC重算不满足C05，正在替换，不能称C05全链完成。

具体必要路径仍C05已授 ElectronicSignatureSubjectAdapter.java / ElectronicSignatureServiceImpl.java、A16 SubjectAdapter、SignatureEvidenceService(Impl)和DCC SignatureVerificationServiceImpl；new v5 envelope携候选canonical供已授权snapshot服务端验证后定稿，旧v4/v3bytes不改。返回S必须与最终DCC HMAC投影/原统一record同源；相关真实Timestamp6 roundtrip+actualverify组合将断言同一S，不能只clock单测。

另一个精确升版前端阻断路径待补授：`IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts:buildApplicationFields`（75—83）目前仅两旧模式，DetailApplicationPanel实际submitInitial/buildRevisionCommand仍在此拒DELAYED；必要差异为DELAYED/null与立即共null规则，旧PRESET_DATE仅历史精确V1申请仍支持，实际payload后续guard要求V2；不改RevisionPanel/G08显示/版本策略。专属G01调用真实revision函数RED后实现即可，不能只helperGREEN称已覆盖升版。

## 2026-10-15 关键后端依赖已出现（A07/A11/A13）

- 实际源码已新增 `DccDelayedEffectiveConfirmationEvidence`，`requireConfirmed(DccControlledFileDO)` 和 `requireConfirmed(file, BoundExecution)` 返回唯一 `VerifiedConfirmation(E,S,signatureId,processInstanceId,taskId,attributeRound,actorId,evidenceHash)`。`requireSignedForActiveTask` 仅供保存确认前验证签名，不可用于受控。
- 文件四字段、签名两字段、Rule DELAYED_CONFIRMATION_MODE、V6 FlowRule 与真实模型序列化 digest、Bound resolver 运行选择与原 requester map 校验均已落地，可供 G02 编译接线。
- 私有 javac 编译上述即时源码 + 本任务 tests；真实 JUnit 验证器8/8 PASS。Bound/cryptographic verifier/任务为明确 mock ports，不是全验签或正式 reactor 验证。同步 End 分支严格要求 TASK_STATUS=APPROVE 和相同 DCC signature/actor/action 的 task-local 凭证，历史分支要求同 task ended+APPROVE；不能签名存在就当完成。后续需真实同引擎事务验证。
- 实际 V6 XML 上传/升版 2模式×2培训分支执行 Flowable 隔离 H2；RED 缺新模型，GREEN 1测试覆盖8条分支；候选选择为声明 mock port，实际主管/会签候选授权未在此测试证明。
- 即时 canonical model digest：upload=36bc2379b56d0ada0571fccd573024fb05f951632e0963cbcd9ab1702af7b2c2；revision=84d039f7fab6c81159fb049637a0ff70b6eb3e57d6dfc6b47a64475e421935d5；已注册保留所有V4/V5。
- G02可消费以上正式代码；仍需本线程完成签名payload生产创建/验签、确认命令/提交与Root唯一Maven票。没有声明完整后端GREEN。

已同步 Root C01（DELAYED、LocalDateTime、YYYY-MM-DD HH:mm:ss、Asia/Shanghai、可信签名同一时钟/秒精度）。最快接收入口为 contract.md，含 stage/status/meaning/六字段和 G01/G02 明确文件拆分。

## Root 可复核结果

- int_qms 已实际核对。当前旧 PRESET_DATE 在上传正式提交要求非空日期，DetailApplicationPanel 和后端提交/VO/revision 同样约束。
- V5 正式源码模型文控→可选培训→End，无延迟确认；精确 catalog/绑定 resolver 防止直接换 XML，故需新正式不可变执行合同。
- 真实生产 helper RED：3 tests，2 baseline pass、1 expected fail（DELAYED/null 被拒绝），退出 1；并非只查字符串。执行时为提案 mode，后续 C01 已冻结同一 DELAYED token。
- 相邻 Node 本轮独立重跑 53/53 PASS，退出 0；仅旧行为/页面模块合同，不是新需求通过或运行验收。
- 已提供 design.md 的 BDD 最小合同、签名/时间/租户/版本/属性轮次/正式模型/回滚/重放设计，及 integration-notes.md 的精确文件、锚点、差异、owner 依赖和验收影响。
- 签名时间 S、后续处理 T、用户所选未来时间 E 必须分别保存；G02 受控时间只消费真实确认投影 S。所选 E 进入新 canonical/HMAC 字段，验签从签名快照重建，不用可变文件字段。

## 具体接入请求（由 Root 主动复核，不自行跨线程发送）

1. DELAYED/时间格式已 C01 冻结；核准 PUBLIC_MATRIX_NATIVE_V2 / PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM / DELAYED_EFFECTIVE_CONFIRM / PENDING_DELAYED_EFFECTIVE_CONFIRM / DELAYED_EFFECTIVE_CONFIRM_APPROVE、四文件投影和两签名字段、新 payload；明确原申请人确认签名岗位资格与新增审计操作 REQUIRED 合同。
2. 按 integration-notes.md A/B 转交核心文件后启动后端 TDD，LifecycleService/Finalization 两文件仅授 G02；G01 提供唯一 ConfirmationEvidence 验证器/DTO 与模型/签名接线。Root 唯一发 Maven 验证票，M01 不占用。模型真正部署及 transition 列表另有运行授权才能执行。
3. G12 归还 upload/index.vue/submitter.ts；G10/G08 归还详情页和 BasicInfoPanel 后再接前端；新确认 panel 也需 WRITE_GRANTED。
4. 对 G07 当前 Query/DetailAuthorizationGuard 的确认办理正文上下文和新详情字段，由 Root/当前 owner 接入；G01 不并写。

## 现阶段未通过/未执行

没有 GREEN，未运行 Java/Flowable 服务或 DB 测试，未验证真实主管节点/确认 task/签名事务。没有 E2E、服务重启、部署、真实库写入、Git 提交推送、子 Agent 或 worktree。

任务保留 blocked — 等待阶段转交；不是故障被标为已修复，不标 completed，不执行 cleanup apply。
