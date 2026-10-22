# G01 Execution Log

## 2026-10-14 启动（Asia/Shanghai）

- 接收本轮 G01 SPEC_ONLY：只写本任务记录和独立设计，不写共享生产文件；明确禁止子 Agent、Git 写入、切分支、worktree、E2E、服务、远程、真实业务库写入。
- 已读取根 AGENTS.md、docs/task-closeout-rules.md、backend-development.md/frontend-development.md 适用基础规则、powershell-encoding.md、powershell-preflight-lessons.md、powershell-memory.md、database-rules.md 适用基础规则，以及本轮 requirements.md/coordination.md。
- 只读命令 git branch --show-current → int_qms，退出码 0。工作区存在大量既有/并行 dirty/untracked；不触碰、不基线提交。
- 仅根 AGENTS.md 被 rg --files -g AGENTS.md 检出，未发现更近层规则。
- 首次大规则输出被截断；后续用分段读取适用条款，不以截断内容视为已完整复核。
- docs/dcc-main-flow-no-training-distribution.md 是旧目标文本，与本轮生效/培训需求不同；仅作历史背景，最新 requirements.md 为本轮权威。
- 本任务目录创建前 ABSENT；task.md/execution-log.md 原状态 ABSENT。生产文件尚未改动；后续记录只读源码规范化 UTF-8/LF SHA256 作为设计基线，不替代写入前重新核对。

## 证据边界

本节初始SPEC阶段证据见下文；现已WRITE_GRANTED实施，定向RED/GREEN实际结果追加于后段。未运行Maven，也未取得Root验证票，私有编译不能冒称完整reactor源码验证。

## 第一阶段实际验证与 C01/C02 转交

- node --test doc/tasks/20261014-dcc-gap-delayed-effective/delayed-upload-red.cjs：退出 1，3 tests，2 baseline PASS/1 expected RED。直接执行实时生产 TS helper，DELAYED/null 被当前合同拒绝。
- cwd=IntRuoyiFronted，node --test tests/unit/dcc-upload-ic05-application-options.test.cjs tests/unit/dcc-detail-application-ic05.test.cjs tests/unit/dcc-upload-effective-activation-times.test.cjs：退出 0，53/53 PASS。本轮独立执行，非复制旧报告。
- Node v24.19.0；未安装依赖。delayed-upload-red.cjs 被 .gitignore:103 忽略，已列 Cleanup Keep；本轮不暂存。
- 文档初次 validator 错把源码内既存 U+FFFD 当无效 UTF-8；只读 TextDecoder fatal 确认 DccElectronicSignatureManagementServiceImpl.java UTF-8 bytes 合法，既存字面替代字符 2 个（1331/1335行），不重写共享源码。validator 改严格字节 decode，并对既存字面字符记录 warning。
- 第二次 validator 明确报告新增确认组件路径 ABSENT；应登记为本轮新文件而非现存路径。已修正新增清单，不伪称验证通过。
- 收到 Root C01，已同步 DELAYED、上海时间输入格式。后续 Root C02 正式核准 V2/V6/stage/status/meaning/六列及新签名 payload，更正 E 秒精度而 S 保留原精度，不额外要求文控批准角色。
- C02 局部前端 WRITE_GRANTED：DelayedEffectiveConfirmationPanel.vue、DetailApplicationPanel.vue、workflow/effective-facts.ts、applicationRead.ts 及专属 FE tests。BE A/B 在 M01 释放通知前不写；Lifecycle/Finalization 只 G02，Query/DetailGuard/原Controller 只 G07，G05文件 Root 保护。upload/submitter/workflow.ts 与父页仍未转交。
- 当前开始前端真实 TDD；生产修改前先完成自有 source-baseline.json 的规范化/原始 hash capture。新测试和新组件修改前状态 ABSENT。

## 2026-10-15 当前实际实施状态与关键证据

- A04/A06授upload/index.vue、submitter.ts、workflow.ts、approval-matrix.ts整文件；保留G12空登记说明可提交与长度/归属校验。A07释放BE源冻结，A11/A13/A16补授FrozenApproval/Adapter/Resolver/SubjectAdapter精确接线；Lifecycle/Finalization不写。
- 指纹：source-baseline.json为前端写前49既存/11新增，UTF8字节、23锚点PASS；a06-write-baseline.json另捕approval-matrix，a07-backend-write-baseline.json捕38个BE文件；新增生产/专属测试原状态ABSENT。
- FE新submission 5项真实RED（实时函数/保存/读恢复/FlowRule/显示均拒新mode）→GREEN5；随后增加matrix V2/V1历史保护RED1→GREEN1。panel/API6项初始缺生产RED（其中一个负向TypeErrorPASS不作行为证据）→实际Vue+时间校验GREEN6；合计新增12项PASS。
- 首次相邻回归旧是/否文案与更严格保存定义消息失败，已改授范围内旧文案断言，保留零请求/定义不可替换断言，未伪称全回归PASS。
- 私有SelectionProbe：编译即时生产DccApplicationEffectiveSelection后RED DELAYED/null拒绝→GREEN接受/null，非空日期拒绝；不写target。
- private run-private-java.cjs：参数文件绕Windows超长classpath。初次javac的fixture错误（SignatureDO无tenant builder、泛型assertEquals、verify命名冲突）修自有test；随后真实JUnit Evidence新类缺失RED8→GREEN8。当前已增加currentAPPROVE缺/错signedAuth及active签名不能control场景，待同命令fresh复验。
- 同私有runner --model：真实Flowable隔离H2部署即时V6 XML，缺模型RED1→GREEN1（2动作×2生效方式×2training共8分支）；候选invoker为明确mock端口；取得实际canonical digest并注册，非计算原XML字节hash冒称图hash。
- --hmac：即时Evidence creator真实canonical/HMAC，RED2（旧v4/未拒缺round）+历史baselinePASS1→GREEN3；Management即时新源码已编译，当前完整verify重算链仍需新增测试，不称全验签。
- --command：确认Service不存在RED1→GREEN1负向真实命令（错用户、E早于真实S、空密码/原因零签名/BPM/文件写）。成功/重放/晚失败同事务仍待组合，不拿注解或mockPASS代替。
- 已落正式八字段VerifiedConfirmation、六持久化字段、新Rule/flowRule/canonical graph登记及Bound实际变量/原requester候选检查、signature新meaning/payload/create/management分支、确认Req/Resp/GET/POST/服务CAS+审计+BPM调用；尚未全链组合。迁移仅写文件不执行。
- 已接G07 bodyReadContext wrapper hunk（metadata params/binary headers same normalized ctx，dispatch混ctx拒绝）；wrapper真实函数定向测试待补。A09 iterations两nullable字段严格decoder/G08显示helper只读渲染已接，保留请求raw版本/ID。
- 禁止项均未执行：E2E、Maven、服务/实库/远端/部署/worktree/Git写入/子Agent。私有依赖classpath来自已有报告，只读旧依赖，不声称全源码验证。

## 阶段归还与新review修正

- FE实际wrapper/taskCtx/Daily/dispatch/error3项PASS，新模式12项、savedapplication28、matrix25、G12optionalreason4共72/72PASS退出0（source编译test不全类型构建）。不重复已PASS超范围回归。
- Evidence追加currentAPPROVE但缺/错signatureID/actor/action或RUNNING必须拒绝，active签名method可验证但不允许control；新9/9PASS退出0。
- HMAC actualManagement verifySignature重算与Hmac creation一致，所选E/round改→INVALID；Subject encode/decode/load事务scope真实行为；C05newv5MICROS hash前绑定S/default旧行为已接。当前private编译包含即时signatureadapter/service模块source。
- R04业务意图篡改新增actualSubject test RED1（E/round附加JSON保持claim仍被接受）→完整摘要/严格JSON/同source与scope修正；reject未知/重复字段，没有14v5无scope默认。
- 容量新增schema真实RED1：producer完整正常req编码5515chars/5515bytes，在H2 VARCHAR2048 INSERT报22001。最小fixed claims+hash/serveronecallscope替换后subject399chars/399bytes，保持原2048真实表保存/readbackPASS，7/7签名testsPASS；reason前后端/API/VO一致500，不扩大DB/截断。
- private javac命令即时源码已扩Workflow/Revision/Resolver/ExecutionContract/Controller/DO/签名模块及confirmService完整编译，--command负向1PASS。main-handback.json冻结归还文件hash，G01停止清单文件写入供Root→G03→G06。剩余私有签名/父页及完整Timestamp6/真实End事务仍继续。
- C05共享schema不写，生产migration声明system_electronic_signature.signed_at、dccsignature.signed_at、file.controlled_time三列DATETIME6 guard及新6列，清单供Root/G05接公共H2fixture。noUPDATE/no实库。

## C05真实producer roundtrip（2026-10-15）

- node run-private-java.cjs --roundtrip：初次fixture重载any编译错误修专属fixture；真实JUnit2项1PASS/1RED，唯一RED为篡改统一signedAt仍VALID（既有Query读SERVER_CLOCK文字代替列）。
- 新MICROS使用明确SERVER_MICROS_CLOCK证据标识，旧SERVER_CLOCK历史路径完全保留；新verify消费数据库真实S，微秒S生成在hash前，后续不truncate。
- 同命令GREEN2/2，exit0；实际统一/DCC三Mapper与H2 TIMESTAMP6保存readback + 两actualverify。signedresult/统一record/DCCprojection/fileconfirmedAt/controlledTime严格相等S，E篡改INVALID，S篡改MISMATCH；latefailure注入tx后两签名表0，retry成功。文件控制列此producer测试只SQL核验存储能力，不冒称完整Lifecycle业务执行。
- Root/G02 producer callable和所有mockport边界已在review-ready前段持久记录；A23共享24文件+DetailApplicationPanel正式归还停止写；R05最后parser修复恰在归还更新前已RED1/GREEN2，后续Root已交G03维护。

## 当前父页与命令验证（M03期间）

- A25实际修scope轮次三property：改从已验flowContract.attributeRound，requireDelayedConfirmationRound完整V2/file/BPM/ready验证。缺helper RED1（负向TypeError不作行为证据）→4/4实际GREEN含parenthandler成功/缺轮次零请求/late response隔离。相邻context10项亦PASS，未跑完整type构建。
- 正向command第一次真实到FrozenGate负责人缺失（测试fixture），补正式owner签名事实；随后真正执行LambdaUpdateWrapper报未初始化TableInfo cache（测试fixture）；补TableInfoHelper。CAS/审计拒绝测试收紧必须检查实际错误含保存冲突/late audit failure，不接受任意异常PASS。
- private源编译最近被已归还main的G03CustomService新依赖/G06AttachmentService新方法挡住，未改共享生产去迎合私有旧依赖；runner声明OWN_CURRENT_SOURCE_WITH_PREVIOUS_RETURNED_MAIN_AND_EXTERNAL_DEPENDENCY_CLASSES，未声称当前全源码通过。Root M03独占reactor诊断期间不运行Maven/全types；按Root实际编译结果后续修自身问题。

- 最新收口private --command：补精确owner审批事实及TableInfo metadata后3/3PASS，CAS失败消息与audit晚失败消息被逐项断言，正向签一次/BPM一次、同载荷重放不二签/变E冲突拒绝。此前断言任意Exception可能假PASS已修严，当前结果非先前宽松负向测试复用。
- M03Root正式诊断结果只读确认exit1：失败点为G03当前Workflow saveCustomDraft/readCustomDraft缺方法，本线程不写该文件。sharedtarget重编译使私有原deps部分缺类，精确增加未修改的AuthorizationAudit接口/DO与AuthorizationServiceImpl即时读编译满足自有Management引用；并行主申请用前次checkpoint class只读。明确不是全当前reactor。
- 父页实际loadDelayedConfirmation单独4/4PASS：成功带真实attributeRound7，未核验round0请求，task变化后late response清空；scope声明移watch之前避免初始化TDZ，identity再含mode/status避免同file状态变化保留旧context。

## A27 / A28 定向收口

- native-approval-progress当前旧V2不认RED3→GREEN3；确认只真实同轮任务驱动完成，新的本次mode为DELAYED才展示待进入分支，历史无actual确认task不借当前file推断；立即没有确认节点，训练顺序与V1/V4保留。组合native旧8+round4=15PASS。
- parenthandler补精确read-context快照，RED route变但watch未运行旧response仍发布→GREEN5/5。identity即时相等与generation双验证，错误同样不跨任务写入。
- M04Root正式reactor：全部main编译通过，testCompile唯一CapturingDccSignatureVerificationService缺新方法；A28已补精确抛UnsupportedOperationException实现，不加生产默认、不删原策略断言，不自行Maven。供Root M05联合4target继续。
- 公开command私有4/4PASS，新增BPM晚失败真实抛错传播并不返回CONFIRMED；audit已调用、签名1次，不以mock内存状态冒称真实事务回滚。真实producerDB2/2与G02公开命令官方BPM/End+ledger组合证据分别保留。

## Root M05之后的当前依赖checkpoint

- Root通知M05正式exit0：23reactor BUILD SUCCESS、四类80/0/0/0，main与testclassesfresh。该80仅会签/派生/引用范围，不复制到G01验收。
- runner改全新private-java-current-source目录，不再让旧returned-main私有class覆盖Root M05fresh；自有32source显式javac，dependency用Root M05fresh及外部classpath。--command4PASS后一次--suite收口：23/23PASS、0skip、sourceStable=true、exit0；receipt/hash及compile/junit日志持久。未写sharedtarget/运行Maven。
- --suite包含Evidence9/Hmac&Subject7/actualUnifiedDccProducerTimestamp6AndActualVerifiers2/PublicCommandMockPorts4/ActualFlowableV6Model1；source稳定，不将端口mock与DB真实行为混称全验收。
- FEactualBasicInfo挂载和datehelper新增2/2PASS：epoch明确Shanghai显示、fractional S string不改、非法日历拒、四投影来自所选file、缺事实明确未记录、rawfile不变。仅定向Vue测试，不完整types。
- root当前G02 public-command-11 exit source_stable/junit0已被Root独立读取接收；本线程只引用其2项公开实际链，并不重跑旧内域14PASS替代。

## A30最后标签与独立checkpoint冻结

- 写前a30-write-baseline.json保存shared/lifecycle.ts与stage-name.ts原SHA256；actual label RED1（新status为“-”）+2旧/权限baselinePASS→GREEN3；与strictdate/actualBasicInfo2组合5PASS exit0。
- 只新增DCC_CONTROLLED_FILE_STATUSES的PENDING_DELAYED_EFFECTIVE_CONFIRM、statusdefinition精确“待延迟生效确认”和stage-name精确“延迟生效确认”；矩阵四阶段表/withdraw允许表/actionProjection判定均原样，负向敏感动作逐项断言不放行。
- Root M06非业务失败：旧signature真实测试PASS/mainPASS，DCCtestCompile由G06漏TenantContextHolder import阻断，七DCC目标未跑。此轮未私有Java读sharedtarget/未Maven，没有用旧23或G02旧14替代正式target。
- 当前独立BE/FE文件按independent-handback.json保存指纹并冻结交Root，main24/DetailApplicationPanel归还继续有效。本线程无新业务扩展/无Git/服务/真实库/E2E/发布。若最终联合发现实际失败再按Root精确owner返修，不自主重开其它共享源。

## A31：M07唯一资源cwd错误修复

- 当轮已复核int_qms/规则与Root A31；Root M07实际exit1，signature11PASS、新DCC44中43PASS/0FAIL/1ERROR，只有production模型文件在模块cwd被错拼成Backend/Backend/config。不是业务RED，不能删/跳测试。
- 真实Surefire XML只读确认basedir=user.dir=C:/IntRuoyiAll-int_main/IntRuoyiBackend/yudao-module-dcc；写前a31-write-baseline.json记录V6test/privateRunner hash。首次保存baseline Node命令有多余右括号退出1，尚未写生产；修语法后成功，不记业务RED。
- V6test新增requireRepositoryRoot：standalone显式dcc.repository.root绝对路径；Maven只按确定模块basedir推repo根。root marker、backend/module pom及两正式V6 XML缺失即fail。移到引擎创建前校验，不扫描cwd/替代资源、不影响原八分支/断言。
- privateRunner JVM args显式传-Ddcc.repository.root=<正式绝对root>；新增自有A31ModelRootProbe。仅编译V6test+probe到自有a31-path-private，从Maven模块cwd启动probe，结果A31_PATH_CONTRACT_PASS，compile0/probe0；覆盖module/backend cwd、standalone及缺/错/空输入拒绝。未重跑23private/未Maven/sharedtarget。
- 当前A31 test路径修复交Root M08同8scope正式复验；43项正式成功范围保留，但因该模型ERROR仍不称全部8scope通过。

## Root M08正式软件验收通过

- Root明确句柄11251实际exit0/67秒、23reactor BUILD SUCCESS，签名11+DCC44共55/55/0F/E/skip，8fresh XML归档；只读核m08-reactor-receipt.json/m08-verification.json与55计数一致。V6模型1测试8分支真实执行，A31唯一cwdERROR关闭。
- Root明确“G01/G02软件范围联合正式接收”；没有把55扩custom/附件/正文新投影/12项整体完成。Root整仓2286source漂移3另留档；自有独立50source重新SHA256核无漂移。
- 归档official-m08-receipt.json，保留8class精确counts/freshXMLhash与实际票据来源；本线程不自行Maven或重复23private。Root T03当前整合types唯一执行，若其产生G01实际错误再接精准返修。
- 软件已验收，按收尾规则先ready_for_closeout做自有cleanup preview；本轮Git提交/推送被明确禁止，最终blocked不completed。未执行Git/服务/实库/E2E/发布，也不清其他任务资产。

- 自有cleanup工具SKILL.md本机缺失，仅script文件存在；未将其当已应用skill，不触发额外权限要求。依项目task-closeout-rules使用现存只读script preview：exit0，keep23/delete候选168/warnings0，所有必留证据在keep；摘要cleanup-preview-summary.json，实际删除0。apply未执行，未声称PASS。
- task状态先ready_for_closeout完成preview，随后严格按Git当轮禁令设blocked，不标completed。Root可据official-m08-receipt.json及冻结source接收软件；全工程types/其他模块仍Root范围，不反复触碰已归还共享文件。

## Root T03 类型结果同步（无源码改动）

- Root明确T03完整pnpm ts:check正式exit0、无diagnostics；只读核t03-types-receipt.json实际11:15:00—11:16:29 Asia/Shanghai及票据模式diagnostic/current-source-mutable。关闭旧T01/T02类型等待，不复制类型PASS为业务PASS。
- 实际再次核分支int_qms、独立冻结50源码SHA256无漂移；本轮只改自有记录/元数据，没有源码改动、没有全types/Maven/旧23private重复。Root全项目freeze/custom/附件/正文等仍各owner推进。
- task保持blocked仅因明确Git收尾禁令，G01软件M08 55PASS/类型T03通过均已明示；不把此记录状态当业务阻塞继续开发无关模块。
