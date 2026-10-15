# G06 验证报告

## 最终软件验收已接受（M14补齐，当前权威记录）

格式收口单列：Root随后发现一个已授旧DB测试的多余CR，独占删除该字节并证明token等价，M15只复验其原30项。本线程不改该文件、不重复套件；这不是业务失败/RED，其余978及当前软件验收结果保留。以Root后续M15记录为该文件最终字节证据。

Root最终正式结果已实际只读核验：

- 后端有效47份class报告共1008/1008，0FAIL/ERROR/skip。依据为M12未受影响45类963PASS，加M14 G48声明18与独立父类27共45/45；M14真实exit0、23reactor全部SUCCESS。不是把M14单次45项称作单次1008项。
- 后端11865记录输入0漂移；M12到M14仅G12 fixture补明确required approvalSourceMode，无生产差异；此前唯一45ERROR已复验关闭。
- F02主申请行为38/38，T04完整pnpm ts:check exit0，B01完整Vite test构建exit0；前端2429生产记录输入0漂移。各行为范围有重叠，不叠加成唯一用例总数。
- 本线程G06冻结源码hash只读核验与a34-frozen-sources.json相同；仅整理记录，无生产/test写入或重复套件。

正式证据：doc/tasks/20261014-dcc-gap-fix-coordination/final-verification.json、final-source-check.json、m14-verification.json、m14-reactor-receipt.json、f02-final-application-node-receipt.json、t04-types-receipt.json、b01-verification.json。本任务final-software-acceptance.json保存最终摘要。

全软件域已由Root接收，当前没有软件失败或待修业务。仅Git提交/推送收尾未授权，工程不标completed；最终清理与报告由Root统一办理。未要求的E2E/部署不列作软件失败，也不私自执行。

以下历史M12错误及“等待联合”仅为当时记录，已由本节覆盖。

## M12正式软件接收（历史中间记录，已由M14补齐）

Root M12正式-am联合已结束，软件域接收。本线程只读核验m12-verification.json/m12-reactor-receipt.json/m12-source-drift.json和G06相关9份fresh XML的SHA，没有重跑任何套件或修改生产/test。

|正式M12 class|实际tests|FAIL/ERROR/skip|
|---|---:|---|
|DccCheckinAttachmentUploadContextTest|8|0/0/0|
|DccRevisionAttachmentsDatabaseTest|37|0/0/0|
|DccControlledFileQueryServiceTest|272|0/0/0|
|DccCustomDraftInheritanceIntegrationTest|6|0/0/0|
|DccRevisionDatabaseTest|58|0/0/0|
|DccCheckinDraftInitializationDatabaseTest|30|0/0/0|
|DccW08PreparedCreationDatabaseTest|63|0/0/0|
|DccW08PreparedSourceBoundaryTest|22|0/0/0|
|DccWorkflowSelectedIterationDatabaseTest|38|0/0/0|

旧5DB合计211。上述为本次官方fresh当前源码结果，既有private6/75/211证据仍按各原范围保留，不重复或扩大。

M12整体1008 tests，963PASS、45ERROR、0FAIL、0skip；exit1。唯一45ERROR属于G12/G48分类BeforeEach fixture未给required approvalSourceMode，Root仅授G12修fixture/M13复验；G06没有待修业务。记录输入11865份、source drift 0；不是118650。m12-software-acceptance.json保存正式摘要及XML报告hash。完整Root证据路径为doc/tasks/20261014-dcc-gap-fix-coordination/m12-verification.json、m12-reactor-receipt.json与m12-junit/。

继续冻结：a34-frozen-sources.json三资产hash当前只读核验相同。Root接收全部G06软件域，项目总体/Git/cleanup未授权收尾，task blocked只指工程门禁；无E2E/服务/实库/远端动作，不冒称已部署或真实页面验收。

下方“等待Root最终联合”等只记录过去时点，现已由M12结果覆盖。

## 当前A34最终checkpoint（优先于全部历史段）

Root M11正式当前附件37/引用16/权限13共66/66、23 reactor BUILD SUCCESS、3066记录源0漂移已实际读取m11-verification.json、reactor receipt及fresh附件XML，G06此前软件域已接收。其后仅Root A34最小新依赖解除Query/Revision两位置冻结；现重新冻结供Root最终联合。

本轮A34实际当前独立命令：

```powershell
pwsh -NoProfile -File doc/tasks/20261014-dcc-gap-revision-attachments/run-private-tests.ps1 -Phase a34-green
```

真实**6/6、javac0/JUnit0、0skip/abort、source drift=[]**；a34-green/receipt.json run_id=ec8e60d46b42401d82ff22baa8583be8，2026-10-15 13:26:40+08结束。Root较早读取5/5；第6项是冻结消息到达前完成的同范围公共Legacy保持例，生产文件未因它再改。类直接继承BaseDbUnitTest，无继承业务测试，完整6方法/两源码SHA/test SHA见a34-frozen-sources.json。

- 首次fixture错误：旧基础schema没有附件表，补仅本测试私有、依当前正式附件facts字段的空表；不是业务RED。routeSnapshots NPE是Root后来明确更正的G03 rework-4 fixture，不是本组首错；生产没有null fallback。
- 有效RED：3场景均失败，分别实际Query没有custom三字段、实际Revision target没有reserved round、未写custom字段所以晚CAS约束不触发；a34-red/receipt.json及test.log留本轮有效结果。
- 接线后3/3 GREEN：actualExecutionContractService、actualCustomMatrixService、真实V6 definition、initializer/round/Mapper/H2参与；未mock CustomDraftInheritance record。
- 补强RED：新增a34_exact_custom_file数据库约束后确实拒绝Revision INSERT复制源canonical。保存a34-no-old-canonical-red.log/receipt；另一CAS例当时受未清理constraint影响，已准确修测试finally清理而未放宽constraint。生产在INSERT前清空target三custom字段，source只读，target真实Round后typed重建再原CAS落库。
- 最终6项覆盖真实检入、正式候选、新target INSERT从未保存别的file canonical、两个路径CAS回滚/重试及公共Legacy null保持。Query/Revision的原tenant+id+WORKING+nullBPM CAS affected=1与本地target同步；public不额外初始化custom round。
- 新测试自包含H2业务库/V6 definition引擎；external storage/项目权限/目录是显式端口，本组给定无附件且正式empty manifest hash。完整artifact字节/所有票据/实际审批/驳回重提组合仍分别由M10/M11与G03组合证据证明，不拿本6替代。

A34源新增必要Revision Resource workingApplicationDraftInitializer，实际custom源才调用建立target真实轮次；手工构造该服务的组合fixture必须wire真实initializer。Root最后冻结指令后未写任何生产或test，只整理记录。没有重复已过75/211、没有自跑Maven/全类型/构建；Root T04/B01已PASS属于各自输入时点。

前端最终只读检查G03期望helper/旧确认框与G06检入组件父页4文件28/28，m11-fe-regression.log；Root A35另接收旧确认fixture5/5。之前缺helper错误已关闭，不作为当前blocker。

当前无已知未修的本轮授权业务缺口；Root仍需对A34后源码执行最终正式联合，G03负责实际reject/checkin/candidate/resubmit组合。无E2E/服务/真实业务库/远端/Git收尾，不标工程completed。

---

以下所有“当前/待复验”仅是各历史时点，不覆盖本节。

## 当前冻结checkpoint：M10资源修正 / Root M11复验中

截至2026-10-15，生产及相关BE测试按Root冻结。以下为当前有效结论；旧阶段缺类/缺3列/fixture失败已被当前结果取代，不作为仍阻塞依据。

|范围|实际结果|来源与边界|
|---|---|---|
|Root M10正式-am十一类|539 tests；523PASS、0FAIL、16ERROR、0skip；main/testCompile PASS|Root当轮明确核验；16 ERROR全为本test QueryFixture使用仓库相对schema路径，在Maven模块cwd重复拼目录。不是业务RED。|
|只修M10这16场景资源路径后|16/16 PASS，javac0/JUnit0、drift=[]|m10-query-fixture/receipt.json，run_id=14b1980d46bc42758de5e96288107731；2026-10-15 12:26:40+08结束；未重跑75/211。|
|Root模式实际校验|显式standalone root、Maven exact-module basedir均通过；缺root/basedir、相对root拒绝|同一requireRepositoryRoot由私有launcher实际反射调用后还原system properties；AGENTS和两个POM/真实schema marker必需，缺资源不复制不猜测。|
|私有当前附件与相邻|75/75，0skip、exit0、drift=[]|a20-regression/receipt.json，run_id=0a8407ec606541019f6045d806d0a759；12:09:08+08。45项新Query/Revision/角色/附件及30相邻。|
|私有旧5DB实际回归|211/211，0skip、exit0、drift=[]|a24-db/receipt.json，run_id=89f6fce1437d432ead82a78fa1f7dbea；12:07:06+08。原权限/锁/版本/轮次/late/图纸断言保留。|
|Query+新增Office/延迟源|私有272/272；Root M09正式该272及新Body23 PASS|Root记录Query一处输入漂移，非finalfreeze；G06独立只读编译G07 Body类当时21/21，之后该owner增到23。|
|前端当前canonical八文件|75/75，0skip|a20-frontend-regression.log；包含原两旧fixture24及新增组件/父页和G07/G08。没有源码新变更后重复跑。|

当前修复命令（根目录）：

```powershell
pwsh -NoProfile -File doc/tasks/20261014-dcc-gap-revision-attachments/run-private-tests.ps1 -Phase m10-query-fixture
```

该phase显式编译当前Query/Revision/附件/Checkout/DTO及真实依赖，classes仅在任务目录；runner传绝对dcc.repository.root。只选择16个configureQuery场景，不用整37/75/211代替Root要求的修复验证。首次selector写入Java argfile未引用#方法名导致0 tests，runner已exit1拒绝PASS；引用修正后真实发现/运行16并PASS，上表只用该有效run。

M10后仅该test资源定位及task私有runner/launcher/文档变动；9份冻结生产资产均与m10-frozen-sources.json相同。m10-path-before.json保存修前test hash，m11-frozen-sources.json保存修后checkpoint。没有修改Frozen生产、共享schema或业务断言；没有运行Maven/fulltypes/build/E2E/实库/服务/远端/Git。

Root现M11正式复验RevisionAttachmentsDatabaseTest整37、ReferenceUsagePageDB整16、FormalAuthorityIntegration；未收到最终实际exit前不称本任务正式全部通过。G03公共确认框旧fixture仍须加载正式attachment helper，当前两旧确认测试失败已精准转Root；本线程未越权改文件。

---

以下为完整历史审计记录，所有“当前/待运行/未授权”仅指该历史阶段，不覆盖上方冻结状态。

## A18 canonical fixture当前结果

Root A18正式授权两旧fixture依赖补齐，未改生产/Query/API，2026-10-15实际分支再次确认int_qms。

- 改前直接`node --test tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-browser-checkin-training-default.test.cjs`真实exit1，失败为checkinAttachmentState/ref未注入和真实模板相关context缺失；这是fixture接入RED，不包装成新的产品业务缺口。
- 只在stateFor补显式ready空附件state/ref（readReadyManifest=[]、markSaved、cleanupAll），模板declarations补正式CHECKIN context/key/state/ref，在培训run补空ready附件state。原source/identity/training/drawing/late/assertions未删未改。
- 修改后**同一canonical Node命令**实际exit0，24 tests、24PASS、0FAIL/skip/cancel。a18-canonical-green.log为本轮实际输出，不再依赖stdin注入。
- 合并当前8文件命令实际exit0，73 tests、73PASS、0FAIL/skip/cancel，a18-canonical-regression.log：两旧fixture24项+当前新组件/父页19项+G07/G08当前相邻30项。
- AST逐项核对：minor-only原65断言/20测试名，training-default原11断言/4测试名均exactly unchanged。修改前a18-before.json、修改后a18-after.json保存hash/断言/计数；测试名与assertions数组deepEqual通过。

```powershell
node --test tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-browser-checkin-training-default.test.cjs
node --test tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-browser-checkin-training-default.test.cjs tests/unit/dcc-checkin-attachments-panel.test.cjs tests/unit/dcc-browser-checkin-attachments.test.cjs tests/unit/dcc-version-display.test.cjs tests/unit/dcc-version-display-a09.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs tests/unit/dcc-browser-selected-version-display.test.cjs
```

cwd=IntRuoyiFronted。没有生产变更，未重复后端测试；此前BE28/58是A15/A17证据，不称A18重新运行。Query完整请求hash/BOUND重放、formal server fact hash/确认送审仍只为精确待转交hunk，见a17-integration-hunks.md顶部新增节，未拿isolated服务代替全链。

以下“旧fixture未授权/失败”段落仅保留A17历史边界，已被本A18正式canonical结果覆盖。

## A15/A17 最新验证与未接入事实

- 当前BE命令仍为任务私有run-private-tests.ps1 -Phase green / regression。最新GREEN：28/28，javac0/JUnit0、0skip；最新regression：58/58，javac0/JUnit0、0skip、drift=[]。新增formalBaseline policy及真实sourceOwnership Mapper在runner列为当前源码显式编译，仍只读依赖target，不代表整个现有共享源码编译。
- A15补formal RED：26项24PASS/2FAIL。其中真实产品FAIL是selectedIterationVersion错误仍可冻结；另一项为新SOURCE票seed先以ATTACHMENT用途插入撞现有slot的fixture错误。两类准确区分，保存a15-formal-target-red.log/receipt。fixture改为独立session种子再显式SOURCE用途，formal gate实现后26/26 GREEN。
- 随后PARTIAL/INITIAL positive guard测试曾2FAIL（测试用BaseMapper.selectById要求全DO列，而任务schema有意只列相关列），改用正式selectByIdAndTenantForUpdate的SELECT *读方法，未改sharedSchema或产品错误处理；当前28/28通过。
- 新SOURCE全事务用例：实际createVerifiedCopy/enlist、SQL next.source更新、真实claimSubmissionSource、真实SOURCE markBound，再新bindCheckinAttachments。第二attachment DB约束失败后六表精确快照与物理bytes均回原样；重试3张票BOUND至21、2附件+1body ownership成功。不是mock调用次数或仅子附件回滚。
- 新formal用例：selectedId/predecessor/version、三基线ID/recorded原version/target formal intent不符均零新文件和DB写；合法PARTIAL/requester、INITIAL/no baseline、REPLACEMENT/非上传人OWNER三条通过，来源字段不改。

### A15/A17 前端实际命令

```powershell
node --test tests/unit/dcc-checkin-attachments-panel.test.cjs tests/unit/dcc-browser-checkin-attachments.test.cjs tests/unit/dcc-version-display.test.cjs tests/unit/dcc-version-display-a09.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs tests/unit/dcc-browser-selected-version-display.test.cjs
```

cwd IntRuoyiFronted。最新exit0，49/49，0fail/skip。新增组件实际SFC编译/真实Vue挂载及事件状态；API端口mock、未真实前端E2E。两份新增专属测试共19项（12 component/7 actual handler）；其余为当前G07/G08相邻测试（并行owner后来增一项，因此前一轮48→最新49，未复用旧报告）。父页save结果未返回全普通清单时零成功、不markSaved，防止尚未Query接线时伪造闭环。

RED实际执行：组件文件缺失时8/8 FAIL（新建组件TDD基线）；实际父页6项1PASS/5FAIL（payload缺失、未完成仍请求、取消忽略附件清理）；补duplicate formal session RED10项9PASS/1FAIL；pending save unmount及context切换的副作用RED分别11/12项中1FAIL；source保存但遗漏普通附件的父页RED7项6PASS/1FAIL。生产GREEN后各项都加入最新49项，不把源码字符串搜索替代业务。

Vue parse/compileScript/compileTemplate/TypeScript语法转换：browser/index.vue和新CheckinAttachmentsPanel.vue实际PASS，无全类型或Vite大构建。

### 旧fixture的明确未完成项

直接canonical运行dcc-browser-checkin-minor-only.test.cjs、dcc-browser-checkin-training-default.test.cjs会因为fixture未注入新必需checkinAttachmentState/ref而FAIL；不是可忽略PASS，未修改生产来兼容旧fixture。该两文件写权限尚未转交。

stdin只在隔离host内注入空ready附件state/ref与渲染declarations后，24项原断言exit0/24PASS；不是canonical仓库命令通过。完整注入位置见a17-integration-hunks.md。Root需精确授权这两份fixture修复后再运行canonical回归。

### 运行与接入边界

无Query/Revision/Workflow/公共API/提交确认接线，尚无统一formal manifest hash读写或新票完整checkin replay公共实现；未运行Maven/E2E/服务/实库/远端/Git。A17前端接线目前遇到后端Query缺附件清单会明确拒绝成功，不能称实际用户闭环。

---

下方为A08及SPEC阶段历史证据，不覆盖最新上述状态。

## A08 独立服务实际验证（2026-10-15）

4个生产文件与2个专属测试按Root A08实施；完整用户升版链路尚未接入。真实javac/JUnit使用任务私有classes，只读现有target及依赖classpath，不写共享target、不运行Maven，不将二进制依赖当全仓源码验证。

|阶段|实际命令（仓库根目录）|结果/证据|
|---|---|---|
|RED|pwsh -NoProfile -File doc/tasks/20261014-dcc-gap-revision-attachments/run-private-tests.ps1 -Phase red|javac exit0；JUnit exit1，21项4PASS/17FAIL/0skip；red/receipt.json及test.log。上传真实拒绝1080000339；服务API bindCheckinAttachments缺失，部分negative测试因此错误异常类型FAIL，未冒称业务失败已可达。|
|初次GREEN编译|同runner -Phase green|曾因误用不存在的DO.getProjectFolderId编译失败；实际字段归属已复核，目录projectFolder由PublicUploadPlacement真实记录验证，生产去掉不存在字段，没有修改DO。不得将此尝试当行为RED。|
|首轮GREEN|同runner -Phase green|javac0/JUnit0，21/21；后续补强测试后日志更新，首轮结论已记录执行日志，最终fresh报告见下。|
|补强RED|同runner -Phase green（当时新测试先于版本门禁修复）|javac0/JUnit1，24项23PASS/1FAIL；a08-version-guard-red.log/receipt.json保留真实结果。失败为不同formal版本或不递增target被错误接受。|
|最终GREEN|同runner -Phase green|javac0/JUnit0，24/24，0skip/abort，green/receipt.json，source drift=[]。|
|最终REGRESSION|同runner -Phase regression|javac0/JUnit0，54/54，0skip/abort，regression/receipt.json，source drift=[]。24项A08及30项相邻普通附件/培训/源副本回滚。|

新测试实际调用当前UploadService、AttachmentService、UploadTicketService、SourceOwnershipService、原事务回滚helper；H2的attachment/ticket/file/checkout Mapper和Spring TransactionTemplate真实执行。创建target为测试事务内正式候选fixture，不是假称调用了尚未授权的Revision/Workflow。physical FileService/FileConfigService客户端为明确存储测试端口，具有实际byte map及meta行、rollback delete；不连接真实NAS。账号/项目/category/目录/作用域为明确mock权威端口，不能把这些边界的端口拒绝测试当真实授权系统验收。

- 8项Upload测试：双独立正式session票据与真实槽位落库、同槽同内容回读/异内容拒绝、category与原workflow授权拒绝零存储、目录拒绝、storage失败、ticket insert失败及清理重试、caller晚失败整体回滚及重试、精确版本缺失/tenant ignore拒绝。
- 16项Attachment测试：双票全量真实绑定及上传事实；第二附件insert/票据markBound DB约束失败完整快照不变并重试；跨租户/操作者/用途/类别/版本前缀/过期/FAILED/不存在storage拒绝；同票或同session及null清单拒绝；检出/目录/身份/权限门禁；正式候选独立字节/原上传人/历史训练不复制/新轮次绑定；第二冻结insert和物理copy失败/caller晚失败完整回滚；精确freeze回读零新增与变更/次序/字节拒绝；空清单仍做OWNER/tenant/事务校验；同formal与递增工作版本、失效source状态拒绝。
- 相邻回归：DccOrdinaryAttachmentFactsTest、DccTrainingAttachmentTransactionTest、DccTrainingAttachmentServiceTest、DccRevisionRollbackSynchronizationTest；旧初传及历史培训方法未改。
- 当前授权变更及测试路径指纹在a08-before.json/a08-after.json；每运行的compiled-sources.json列实际编译source、receipt记录exit/finish/drift。均drift=[]，不复用旧test PASS。

未运行：Maven/全类型/大构建（无唯一票）、真实Query/Revision/Workflow/BPM集成、前端确认/取消/多附件UI接线、E2E/实库/服务/远端、Git提交推送。前端旧RED仍待后续阶段GREEN。task保持in_progress；A08 phase可review，不标工程completed。

---

以下保留第一阶段证据，其SPEC_ONLY状态仅指当时，不覆盖上方当前授权。

## 结论与边界

SPEC_ONLY证据交付。当前确认框存在可执行行为RED，生产修复未获授权、未实施、无GREEN。相邻35项Node单测本轮重新运行通过；不复制旧报告PASS。无真实页面/E2E、后端运行服务或真实业务数据库验收。

## 分支与观察基线

- 实际命令 `git branch --show-current` → exit 0，int_qms。
- 实际 `git rev-parse HEAD` → exit 0，822c31fc0856516cf59ba9cd84bf2bc7f455d17f。
- `git status --short --branch --untracked-files=no`显示已有大量并行改动；未写生产、未提交推送、未切分支。
- `source-baseline.json`记录33个只读规则/生产/测试/迁移的原字节及UTF-8 LF SHA256。正式文件交还后需重算，而非按旧HEAD覆盖。
- 本阶段目录建立前不存在；7份文件（6份Markdown/1份JSON）均为新建自有记录，无旧文件改前hash。

## 本轮相邻行为回归

cwd：IntRuoyiFronted。

```powershell
node --test tests/unit/dcc-upload-attachment-state.test.cjs tests/unit/dcc-revision-target-confirmation.test.cjs tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-training-attachment-facts.test.cjs
```

实际exit 0；35 tests、35 pass、0 fail、0 skipped。来源为当前生产Vue/TS函数及组件编译，外部API/账号/对话框端口由隔离单测host代替。

|当前用例文件|实际覆盖|对G06的局限|
|---|---|---|
|dcc-upload-attachment-state.test.cjs|0/1/多附件顺序、UPLOADING/FAILED/缺响应阻断、保存期间不可移除|普通上传页面状态，未接入browser检入。|
|dcc-revision-target-confirmation.test.cjs|基线/工作正文/目标版本确认、目标不可用/确认期间变更零写|原用例未断言普通附件名称。|
|dcc-browser-checkin-minor-only.test.cjs|真实submitCheckin固定MINOR、培训布尔、精确检出人/版本/会话、图纸会话、备注变化、重复点击/迟到响应/保存成功但刷新失败|原检入没有普通附件生产状态、票据清单或绑定。|
|dcc-shared-attachments-component.test.cjs|实际附件组件精确version/attachment预览、下载权限、取消/切版零请求、后端文件名保存|读组件行为；不是检入/正式冻结事务。|
|dcc-training-attachment-facts.test.cjs|真实培训上传事实展示、历史未记录事实不伪造、错版/有损ID拒绝|不证明培训与新正式候选附件写链路隔离。|

## 本轮真实RED与取消控制

以下通过stdin加载相邻测试host，并调用实际DetailApplicationPanel的confirmation与submitRevision声明。不是源码关键词检查，也未创建未接线的第二套生产helper。

cwd：IntRuoyiFronted。

```powershell
@'
const fs = require('node:fs');
const adjacent = fs.readFileSync('tests/unit/dcc-revision-target-confirmation.test.cjs', 'utf8');
const g06 = `
test('G06 RED: actual revision confirmation must list both saved ordinary attachments', async () => {
  const h = host();
  h.c.selectedApplicationFile.value.attachments = [
    { attachmentId: '601', controlledFileId: '11', fileName: 'g06-first.pdf', attachmentPurpose: 'ATTACHMENT', sortNo: 0 },
    { attachmentId: '602', controlledFileId: '11', fileName: 'g06-second.pdf', attachmentPurpose: 'ATTACHMENT', sortNo: 1 }
  ];
  await h.c.submit(h.command);
  const summary = h.calls.dialogs[0].text;
  assert.ok(summary.includes('g06-first.pdf') && summary.includes('g06-second.pdf'), 'Actual production confirmation omits both selected-version ordinary attachment names');
});
test('G06 control: canceling actual revision confirmation performs no submission', async () => {
  const h = host(async () => { throw 'cancel'; });
  await assert.rejects(h.c.submit(h.command), /\u5df2\u53d6\u6d88/);
  assert.equal(h.calls.writes.length, 0);
  assert.equal(h.c.selectedIterationId.value, '11');
  assert.equal(h.c.busy.value, false);
});
`;
new Function('require', adjacent + '\n' + g06)(require);
'@ | node -
```

实际exit 1，5 tests，4 pass、1 fail、0 skipped。FAIL为：`AssertionError [ERR_ASSERTION]: Actual production confirmation omits both selected-version ordinary attachment names`。原相邻3项PASS；新增取消控制PASS，submit请求数0、所选工作稿仍为11、busy=false。这个结果只证明隔离函数行为；没有创建真实申请或读取真实附件。

RED对应BDD：Given 所选工作稿真实响应含两普通附件；When 确认升版送审；Then 确认必须完整列出两项。当前没有实现，故不写GREEN，也不把4项PASS包装为G06通过。

## 后端相邻测试阅读证据

- DccOrdinaryAttachmentFactsTest：真实AttachmentService的0/1/多附件、上传人/hash/时间/版本、错误租户/上传人/用途/BOUND目标拒绝、首次申请轮次不可覆写；mapper为mock，事务active手工设置，不能证明完整事务回滚。未运行。
- DccCheckinDraftInitializationDatabaseTest：隔离jdbc:h2:mem:dcc_c_checkin_h08，真实Query/initializer/Mapper/版本及Flowable、两次检入→送审、精确重放、lateFailureRollsBackRealFileBodyCheckoutRoundsAttributesAndCandidate等；当前不覆盖检入普通附件。未运行。
- DccRevisionDatabaseTest：已有实际检入晚失败保留检出锁、正式候选及来源身份/权限/并发、真实归档/body回滚；没有G06两普通附件从工作稿到正式候选断言。未运行。
- DccRevisionRollbackSynchronizationTest：确定ROLLBACK清理正文、COMMITTED和UNKNOWN不误删；是正文清理回归，不代替附件事务。未运行。
- 当前创建附件迁移与培训事实迁移已只读核对；已有用途、精确版本、首次BPM轮次、上传人/时间字段，版本+storage索引并非单一全局附件绑定。槽位迁移确认多附件独立session需要。未执行SQL、未连接业务库。

## 待执行真实TDD

1. Root授予精确文件后，先新增生产调用行为RED与隔离H2事务RED，正式VO及入口拒绝应真实失败。
2. 修复唯一票据上传、Query全量保存、附件服务冻结和Revision/Workflow接线后取得GREEN。
3. 第二附件insert/票据markBound、target创建后附件冻结、BPM保存/轮次绑定晚失败必须核对完整数据库快照及重试；不用单个字符串或仅mock调用次数证明事务。
4. ROOT唯一验证票后拟运行（PLANNED，未执行）：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccRevisionAttachmentsDatabaseTest,DccCheckinAttachmentUploadContextTest,DccCheckinDraftInitializationDatabaseTest,DccRevisionDatabaseTest,DccOrdinaryAttachmentFactsTest,DccRevisionRollbackSynchronizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

其中新测试名为设计，文件尚未创建。`-am`编译依赖；`surefire.failIfNoSpecifiedTests=false`只供无指定类的依赖模块，不允许把0项目标测试当PASS。需记录票号、scope、调用句柄、exit与surefire报告，避免共享target冲突。

## 文档与工程收尾

- 自有7份文件结构验证实际exit0：标题/必需章节/状态/BDD/UTF-8/无冲突或尾空格、JSON及33份源路径/hash格式通过。git diff --check仅限本任务目录exit0；任务目录为未跟踪文件，因此不将其当作内容验证，内容由独立结构检查确认。
- 同一只读结构检查发现snapshot之后coordination.md与FE workflow.ts发生并行漂移；已重读G06/M01授权及CheckinReqVO，G06仍SPEC_ONLY、M01仅G12可Maven、当前检入请求仍无普通附件字段。未重写这些生产/协调文件，后续正式接入必须重新读取最新合同，当前观察指纹不冒称最终冻结。
- 未运行Maven/全类型/大构建：无Root唯一验证票。
- 未运行E2E/服务重启/远端/真实业务库写入：本轮明确禁止。
- 未运行Git提交推送及cleanup apply：工程收尾未授权，生产尚未接入；task仍in_progress，禁止标工程completed。
