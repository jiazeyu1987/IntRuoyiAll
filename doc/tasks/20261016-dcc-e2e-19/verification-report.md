# E2E-19 目录与搜索 — 验证报告

## R3第四轮E19本轮结果（2026-10-21，待Root复核，HOLD）

完整23项真实UI全部PASS（目录15、授权全局4、P2拒绝2、当前项目2），0 FAIL/0 BLOCKED/0 NOT_RUN/0正文未ready；两账号各自fresh context登录并核验昵称、正式菜单及实际访问范围。复用本例三个既有ACTIVE A/1对象，逐项来源/版本及PDF非空canvas、正文marker重新验证，未使用旧PASS。精确角色名称来源为原UI分配证据；当前个人中心无角色字段，未虚称重新回读角色分配。

原runner BLOCKED/exit20保留：两个console超时均由当前trace定位api.unisvg.com外部图标。无pageError或DCC HTTP错误。27条未完成/中止自然请求快照另列元信息，未当作30秒业务超时；业务DOM全部达标。初次启动旧businessGate匹配错误未启浏览器，原错误单独保留，仅校准本例R3授权识别。未重跑业务或修改产品。

R3 proof及2095来源快照/Jar收尾仍匹配；8061/PID20976、48061/PID41508。52张截图、两当前脱敏trace及network-diagnostics、逐分支对象/账号/版本和关闭回执见rerun-20261020-r3-round4/handoff.json。所有context/browser/runner关闭，残留进程0；本例4账号、27对象记录保留，无权限/fixture写入、无API/DB业务动作。旧环境字段差异及其它例UNKNOWN继续保留。无Git/apply/重启/追加轮次。


R3收尾：当前两trace CRC及5份现有凭据扫描PASS（0损坏、0明文命中）；52截图。已先ready_for_closeout后cleanup preview实际exit0，本轮全部资产keep，delete仅原pycache，实际删除0；未apply/Git。业务BUSINESS PASS WITH OBSERVATIONS待Root复核，原runner BLOCKED/exit20不改写；工程blocked且HOLD，验证交接完成即停止，不循环收尾。


## 20261020-r2-round4 Root接受结果

Root已审核本轮handoff、23条逐项结果、两次原始attempt、closure-receipt及3份当前trace CRC，例级接受 **BUSINESS PASS WITH OBSERVATIONS**：23项业务均在本轮完成；据Root通知，权威台账已记PASS。本例保持HOLD。

观察项保留：api.unisvg.com外部图标超时单列；原runner BLOCKED/exit20及首遍7项等待失败不改写。旧environment字段与实际冻结运行态（8061/PID20976、48061/PID41508，Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147）的差异披露继续保留。

本次仅补充本例三份文档；不启浏览器、不重跑、不改原结果或共享台账，不执行Git、cleanup apply或服务操作。结构核对通过；工程状态沿用blocked，业务接受不等同于工程completed。


## 20261020-r2-round4 最新结果（待Root复核，HOLD）

原23项本轮真实UI覆盖齐全：首遍6 PASS、7次5秒等待FAIL、10项因trace路径守卫中断未执行；原结果/错误完整保留。校准任务自有输出目录守卫与expect等待至30秒后，原7项同对象复核及其余10项全部PASS；合并业务23 PASS、0遗漏，不使用历史PASS。所有来源行、固定A/1、PDF非空canvas和唯一正文标识、P2拒绝断言保持。

补跑runner仍BLOCKED/exit20：readerBoth控制台超时由trace定位到外部图标https://api.unisvg.com/ep.json?icons=files；非业务断言失败。保留原runner状态，不声称整轮无异常。首遍路径守卫中断为测试资产适配错误，已修正并完成脱敏；首遍stopReason不改写。

冻结8061/PID20976、48061/PID41508、Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147及6份前端hash收尾仍相符；结果中总dispatch旧PID/hash只作原始快照。新3份脱敏trace、全部截图/逐项结果、5凭据/84ZIP扫描PASS（0明文、0损坏）见rerun-20261020-r2-round4/handoff.json。浏览器已关闭，自有runner及已知进程残留0；未改产品/权限/业务fixture，4账号及27对象继续保留。未Git/apply/重启/新一轮，工程未completed。


## 20261019-r2 相邻测试修复最终结果

本轮只修复旧相邻16项测试harness。真实RED：`node --test tests/unit/dcc-detail-closure.test.cjs`，exit1，12PASS/4FAIL；四项均在真实DetailApplicationPanel加载时缺fileTypeTaxonomies模块。核对正式源码：getFileTypeTaxonomyUploadOptions仅由beginTypeChange主动类型变更动作调用，普通open/selectIteration/selectIntent不应读取候选类型。

最小修复仅在该测试的application()依赖夹具增加模块/函数；函数被意外调用时显式报错，不返回空候选或默认成功。所有原测试正文与断言已与修改前HEAD逐字核对保持一致，未改生产代码或共享Query。GREEN/REGRESSION：`node --test --test-reporter=tap tests/unit/dcc-detail-closure.test.cjs`，exit0，16PASS/0FAIL/0SKIP；node --check及定向git diff --check均exit0。详见repair-20261019-r2-red/green.log与json及repair-20261019-r2-handoff.json。

纠正当前报告：4项旧harness依赖失败已经修复，当前该相邻回归16项全PASS；下文“保留4失败”仅描述2026-10-18的历史结果，原始RED证据保留，不覆盖/吞掉。原目录/搜索23项业务仍为Root接受的23PASS，本轮按指令不重跑；无新的页面E2E，也不以旧workflow-v6声称其它后端新修复通过。本轮不需要运行态切包、Maven或全量类型检查窗口，不启动浏览器/服务，不修改任何账号或fixture；27个对象记录、4账号、79份trace继续保留。未提交推送，正式工程收尾仍受无Git/apply授权边界限制。


## Root最终接受（2026-10-18）

Root已接受原23项业务PASS：阅读真实runner确认无API代替业务，复核当前项目名称/编号在同一项目返回两个文件夹及两个正文marker，查看两行截图，并独立重跑20项定向测试PASS；三前端文件均在授权范围。较大相邻测试16项12PASS/4FAIL保持原始结果：4项是旧harness缺fileTypeTaxonomies依赖，未归类为生产回归，也未修改或吞掉失败。

Root通知运行态已更新为后端PID34440及新hash；本次委派未给出新hash全文，不推断、不覆盖原23项验收记录的实际PID25996/SHA bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f。本例复用已发生的真实事实和前端修复证据，不追加无理由的23项重跑。

本次仅同步本例证据记录。已核对既有cleanup preview（exit0，执行时ready_for_closeout）：79份ZIP、fixture-manifest、受本机保护且不供提交共享的凭据及最新23项全部结果/截图均在keep；delete仅任务内pycache，实际删除0。全部4账号、27条任务对象记录及必要trace保留，不执行apply、Git或追加业务。业务已接受；工程状态按无提交推送边界保持blocked，不能写completed。


## 2026-10-18 最终业务验证：23 PASS / 0 FAIL / 0 BLOCKED

原23项由两个独立普通阅读账号通过真实页面全部通过，runner退出0。最新证据：`runs/2026-10-18T12-05-58-909Z/result.json`、同目录两账号脱敏trace及逐项列表/正文截图。746条自然请求记录无HTTP错误；pageError、consoleError均0，浏览器已关闭。业务PASS与正式收尾分开：未获Git提交/推送授权，27个任务对象继续保留给Root复核，cleanupCompleted=false。

“当前项目”正式页签已补齐。选择P1R2且不选单文件夹，名称、编号均一次返回目录一/二两行，来源项目306正确；逐行打开固定文件2054545668044084229、2054545668044084237的A/1并搜索各自PDF正文标识。自然请求断言GLOBAL+精确dccProjectCodeId=306且无projectFolderId；原全局、目录及P2隔离21项同时通过。没有用API/DB执行或替代业务断言。

Root指定唯一写者后仅改ProjectBrowserPanel.vue、project-browser.ts、applicationRead.ts三处前端生产文件及贴近行为测试：增加明确project范围，保留项目ID、无文件夹要求并拒异项目响应；未选项目零请求明确提示。后端、SQL、权限合同均未扩大。真实State/adapter测试RED为9项4PASS/5FAIL（exit1，预期合同缺口），随后GREEN20/20（exit0）；相邻project-browser wrapper定向1PASS。较大dcc-detail-closure测试集尝试16项12PASS/4FAIL，四项失败为其它application组件测试harness缺少fileTypeTaxonomies依赖，未改无关源码或测试，未冒充全套通过。命令与日志见project-search-handoff.json。

最终18份CJS（含新增行为测试）语法检查PASS；5份当前凭据扫描79个trace ZIP及任务文本，明文命中0、损坏ZIP0。当前项目列表截图已人工查看：页签选中、全部文件夹提示、两行不同来源文件夹及A/1正确。R1624-07此前已由Root独立复核关闭，旧RED、21PASS/2BLOCKED及其它历史快照保留，仅作历史证据。

## 历史阶段记录（不覆盖上述最终结果）

## 当前结果：21 PASS / 0 FAIL / 2 BLOCKED

原23参数已在新运行包bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f上真实执行。证据：runs/2026-10-18T10-42-03-692Z/result.json、两账号脱敏trace、逐项列表及固定正文截图。DCC HTTP错误、pageError、consoleError均0；browserCleanupCompleted=true，Node业务runner退出20表示仍有精确阻塞，整例未PASS。

|原参数组|实际结论|
|---|---|
|两个账号按项目/文件夹浏览及名称/编号搜索，逐结果打开固定版本正文|15 PASS|
|两个账号在授权范围内按名称/编号搜索，逐结果核对来源/版本/正文|4 PASS|
|单项目账号按P2名称/编号搜索：无结果、无浏览按钮、无P2正文|2 PASS|
|当前项目跨全部文件夹按名称/编号搜索|2 BLOCKED：缺正式范围入口|

三份文件4229/4237/4238（完整ID见manifest）均由本人真实上传、另一自有账号完成主管审核→负责人签名指派→实际会签→批准并明确文件负责人→文控标准化签名，最后由两账号真实回读ACTIVE A/1正文标识。P1两文件夹/P2一文件夹、三个独有正文标识均覆盖。两普通账号最终菜单各只有6807查询；本例配置角色恢复6801/6803/6807，准备期临时上传/签名/项目编辑及误选导出已回收并重开回读。P1正式规则保留作者OWNER并只加USER910361 VIEW，P2保留作者OWNER且未给910361 VIEW。

剩余两项已另起新context实际复核：选P1R2而不选单文件夹，名称及编号查询均提示“请先选择项目逻辑文件夹”，自然browser-page响应均0；范围仅“全局文件”“项目文件夹”。证据runs/current-project-entry-2026-10-18T10-54-12-434Z/result.json及两图/trace。直接源码和最小前端范围、BDD见current-project-search-gap.md；按Root生产冻结指令尚未修改这条生产链，待Root指定唯一写者。不能用已PASS的文件夹或全局搜索替代它们。

R1624-07诊断修复已由Root review关闭：有效RED→36 GREEN→完整包/部署→运行态原UI负例显示具体类别UPLOAD拒绝，原条授权finally完整恢复。该独立修复通过不等于本例全部通过。所有历史RED/超时/更正均保留；超时动作均先读实际任务/签名再续跑，未重复已完成签名。

当前无维护暂停，全部浏览器/runner已关闭。27个本例对象记录保留给Root review及剩余两项修复后复验，包含4个账号、3份ACTIVE文件、目录/类别/项目/角色等；cleanupCompleted=false。没有Git提交推送、服务操作、API/SQL业务替代或新子Agent。下文均为历史快照，当前以本节、business-results.json及checkpoint.json为准。

## 当前维护 checkpoint（2026-10-18）

维护期间追加事实：Root只读确认首文件已处于PENDING_MATRIX_REVIEW，当前活任务75975027-cac3-11f1-8473-b082e25ec548（MATRIX_REVIEW），指派910388；原超时主管动作随后已推进。原主管签名禁止重放，签名记录及当前待办仍须恢复后由本例通过正式UI回读。Root15:30 Thread.print确认DCC业务请求线程0；本例继续保持暂停、浏览器关闭，没有新增页面或业务动作。以下“后台是否完成未知”为当时页面证据，现已由Root追加的阶段事实更新。

按Root安全更新48061指令暂停，runtime-pause.active=true；本例全部context/browser已正常关闭，runner残留0。**完整业务仍未通过。**

Root最终归因已明确：validatePreviewCategory拒绝的是类别UPLOAD权限，复用了项目OWNER错误文案。真实“类别权限”页确认T1 908762/T2 908763原配置0条，分别只为USER910357添加UPLOAD并重开回读；原项目306、原PDF随即预览HTTP200且可见，然后正式提交并从统一待办进入主管审核。VIEW/DOWNLOAD边界未改，未再重建项目或修改OWNER。

已成功创建文件2054545668044084229（DCC241016C19-NUM-P1MAIN，A/1），BPM bb533cbb-cabd-11f1-8473-b082e25ec548；最后任务bb53b208-cabd-11f1-8473-b082e25ec548，账号Reviewer/910388。一次真实主管签名在页面显示“timeout of 30000ms exceeded”，弹窗未关，后台是否已完成未知；没有重试。密码输入前停止trace，finally清空密码后恢复trace，维护截图确认填充密码0。恢复后必须以新context本人登录，只读核对原任务和签名终态再决定动作。证据runs/resume-2026-10-18T06-11-16-023Z/result.json及checkpoint.json。

R1624-07由Root分配唯一写者后，先补真实uploadPreviewFile行为RED；Root确认唯一文案断言FAIL、code/零读取/零创建均通过后，仅改validatePreviewCategory这一行具体文案，同类拒绝测试只更新该场景消息。Root全类36测试GREEN、完整server packagePASS，候选Jar151101/SHA bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f；当前服务在维护前仍是旧消息版本，尚不能宣称运行态负例提示已复验。两生产/测试文件继续冻结，由Root独占Maven和部署。

下节保留维护前原始推进快照，其中“OWNER拒绝待归因”现已由本节的类别UPLOAD实际缺口及误导文案解释替代。

## 2026-10-18 当前结论

**BLOCKED，完整业务PASS 0/23。** 本轮已解除旧维护暂停并继续真实UI；原目录913890回读、T1/T2类别保存、新项目申请43/44独立审核COMPLETED、账号岗位/直属主管、Reviewer签名授权及图片登记均已推进。源预览仍被正式OWNER校验拒绝，尚无受控文件创建/审批任务，不能计目录、搜索或权限业务通过。

- 新P1R2=306、新P2R2=307；上传人DCC241016C19Both=910357，项目审核人DCC241016C19Reviewer=910388。申请确认截图明确选择910357为负责人，实际审核终态见runs/approve-projects-2026-10-18T05-07-43-147Z/result.json。
- **归因更正：Root只读DB和运行Jar已确认旧286/287、新306/307均有910357有效OWNER规则106/107/149/150，initializer真实存在且字节匹配target。撤回此前由Config查询0行推断“旧项目无规则”的结论；旧项目并未缺规则或被修复。** 新R2为此前Root明确授权准备的额外本例fixture，旧项目全部保留。
- 最新同一场景：新项目P1R2、T1、目录一、文件编号DCC241016C19-NUM-p1Main、原件DCC241016C19-NAME-p1Main.pdf；审批路线显示4节点已就绪，input.files为1且文件名匹配。自然POST /admin-api/dcc/controlled-files/upload-preview HTTP400，页面错误Current user is not the DCC project owner。证据：runs/resume-2026-10-18T05-07-53-376Z/readerBoth.zip及result.json。已停止再次建项目、重复上传或写权限，Root追实际reader/参数。
- 独立身份只读核验PASS：登录表单实际username为DCC241016C19Both，页面昵称及个人中心值均为本例双项目阅读人；另一admin context真实用户行明确映射910357及普通员工岗位。业务写入0，证据runs/identity-2026-10-18T05-58-53-259Z/result.json。它证明当前独立登录身份，不代替失败请求当时完整参数证据。
- 原trace脱敏策略删除request postData。留存DOM与自然文件夹GET指向306，但上传请求实际posted projectId未保留，不能由DOM推成已证实。request-identity-only.json仅提取允许保留的请求身份元数据，未读取响应JSON作为oracle。
- T1/T2查看矩阵已通过UI分别保存双/单阅读角色与仅双阅读角色；重开断言遇显示标签/刷新状态问题，仍需恢复后准确回读，不计完整权限验收。新增公共菜单0：草稿已取消，复用现有review/approve菜单只给本例Workflow角色授权。
- 本轮另保留脚本RED：新项目名加R2而代码未同步，正式页面拒绝“项目代码已存在或正在申请中”，未创建对象；已统一新项目/产品编码并成功提交审核。新项目第一次原件选择早于类别匹配完成，前端拒绝且source请求0；待路线真实就绪后才出现上述400，二者分别记录。

旧证据与以下历史状态均保留；当前状态以本节及checkpoint.json/new-project-owner-blocker.json为准。

最新状态：**按Root重启指令暂停；原目录回读前置已通过**。运行脚本在暂停指令处理时已经结束，全部context/browser已关闭，当前本例Node执行进程0。没有在暂停后启动浏览器、发起业务写入或执行迁移。

维护进度更新：Root的schema-source-execution.json记录实际库固定五份迁移于2026-10-16 19:30:05（北京时间）完成，status=PASS；迁移由Root执行，本例没有SQL操作。后端恢复通知尚未收到，本例暂停标志保持active=true，业务23分支仍未执行，不将迁移成功当作页面或E2E通过。

离线续跑准备：prepare-config-actor.cjs增加`--continue-categories`，校验本例既有配置身份与三个精确授权后跳过管理员账号/角色准备，独立登录、回读原目录并继续类别。类别级联浮层Escape修正尚未真实重放；ui-runtime登录遇可见错误立即报错，自动首次改密限于任务自有身份；run-case的项目级搜索缺口记录不再依赖可选blocker存在。以上只有离线检查，不计业务GREEN。

本轮离线检查实际退出0：12份CJS语法、暂停门禁、续跑manifest约束和任务文档结构均PASS；browserLaunched=false、businessActions=0。runtime-pause及manifest/prepared已区分Root迁移授权/完成与本例无迁移执行权限，active仍为true。

## 新配置身份与目录回读

- 用户910403：DCC241016C19Config，显示名DCC241016C19目录配置人；角色991290：DCC241016C19DirectoryConfig。通过真实用户/权限角色页面创建，仅授6801目录维护、6803类别维护、6807目录查询，保存后重开按DOM节点ID回读；未授admin或审计权限，没有为该账号创建任何项目指派。
- 独立context完成首次改密和真实登录，经可见“文控中心 -> 文档目录”查询本例原目录，目标行1条。点击同一行编辑自然读取`/admin-api/dcc/directories/913890`（GET/200），表单编码、名称均准确。目录回读PASS只对应这一前置，不代表完整E2E-19通过；没有重复建目录、改admin指派或解除后端权限。
- 根因更正依据：Root的directory-scope-inspection.json确认原目录存在启用、admin有4条ACTIVE指派；E2E-17的root-directory-diagnosis.json区分正常目录读取与公共待办/类别缺字段异常。撤回旧报告将公共“系统异常”推为目录接口故障的结论。
- 类别表单停止在级联浮层遮挡绑定目录控件，未进行类别保存，当前run的DCC写请求为0。已离线增加Escape关闭浮层及隐藏断言，但暂停期间不重放，不能写成修复GREEN。类别列表空与“系统异常”另记录；schema缺口由Root独立授权处理，本轮没有SQL迁移。
- 本轮命令 `node doc/tasks/20261016-dcc-e2e-19/prepare-config-actor.cjs`，退出20，状态DIRECTORY_VERIFIED_CATEGORY_BLOCKED。证据位于runs/config-actor-2026-10-16T11-09-44-275Z/，包含5张截图、3个登录后脱敏trace及result.json。

runtime-pause.json已经保存并由浏览器入口读取；离线门禁检查确认在暂停标志下拒绝启动。fixture-manifest/prepared/business-results已更新：无目录缺失阻塞，无共享false阻塞，完整业务仍待类别/正文和恢复通知。新配置账号保持启用以便恢复；原三个业务账号仍停用。Root通知健康恢复后才能解除本例暂停标志；不能仅凭旧health或脚本语法通过恢复写入。

下面保留此前准备和历史现象。与本节冲突的旧“目录不可见需修生产代码”归因已撤回，实际创建/清理及trace证据仍保留。

上一轮结论（归因已由上节更正）：已准备账号、角色、项目及分类，旧配置身份看不到本例目录。dispatch为execute，accountSetupGate=authorized，businessGate=execute_after_own_verified_fixture；脚本已改读PER_CASE_REAL_UI_MANIFEST，不再因共享fixture=false或等待Root批准停止。原23业务分支逐项BLOCKED，没有整例或文件权限PASS。

## 最新一轮真实页面准备与结果

|阶段|实际结果|证据|
|---|---|---|
|任务角色|新建DCC241016C19分类、两普通角色：991243 / DCC241016C19ReadBoth，991251 / DCC241016C19ReadP1|fixture-manifest.json；runs/setup-2026-10-15T21-57-05-608Z的角色创建截图、runs/setup-2026-10-15T22-40-54-138Z的回读证据、菜单树/用户角色重开DOM与脱敏trace|
|任务账号|910357 / DCC241016C19Both、910361 / DCC241016C19P1、910388 / DCC241016C19Reviewer均通过真实页面创建，分别完成首次强制改密及独立context登录；普通阅读角色菜单叶子6807实际回读，未授管理员|fixture-manifest.json中的created/actors；普通阅读账号目录截图；审核账号角色回读图|
|项目审核职责|本例新审核账号复用既有项目审核员dcc_project_reviewer，仅为本例新用户分配该职责，未修改既有角色定义/菜单；项目审批由本人独立登录处理|runs/approve-projects-2026-10-16T02-10-39-225Z/result.json与context-0.zip/context-1.zip|
|目录模板与项目|新建DCC241016C19目录模板，含目录一/目录二；两项目申请22、23分别对应DCC241016C19P1/P2及各自产品，均从PENDING_REVIEW经页面审批到COMPLETED|runs/projects-2026-10-16T01-38-20-073Z；上述approve-projects最终run，P1/P2-completed.png|
|文件配置|DCC241016C19Storage目录新增弹框保存后关闭；新建本例一级阶段及L2/L3/L4/T1/T2六节点。一级分类编码由正式页面自动生成FT_ROOT_20261016104416340，名称带本例前缀；其余编码均本例前缀|runs/file-config-2026-10-16T02-44-11-356Z/result.json、01-task-taxonomy.png、trace.zip|
|当前产品阻塞|T1类别选择绑定目录时，本例目录不在正式树中；独立目录名称查询0可见行并提示系统异常。T1尚未保存、T2未创建、三份ACTIVE正文均未创建；没有使用共享目录/矩阵补齐|product-blocker.md；runs/directory-diagnostic-2026-10-16T03-04-31-595Z/result.json及两图/trace.zip|
|实际业务runner|`node doc/tasks/20261016-dcc-e2e-19/run-case.cjs` 实际退出20；business-results.json逐一列23项BLOCKED/未执行；明确sharedFalseUsedAsBlocker=false|business-results.json，stdout包含具体目录绑定缺口，不是旧的共享角色门禁|
|部分清理|三个新账号通过页面停用，刷新后全部再次确认停用；没有删除或重置已有账号|runs/cleanup-users-2026-10-16T03-10-48-422Z/result.json、task-users-disabled.png、trace.zip|

两普通阅读人已实际证明自己的昵称和目录入口可见；本例固定角色的保存结果由配置页面重开回读，不能以管理员目录结果替代业务阅读权限。初始登录进入强制改密属于新账号正式流程，已通过各自真实表单完成，未猜密码或重置既有密码。

当前保留16个任务对象记录：1角色分类、2查询角色、3已停用账号、1已使用目录模板、2已完成项目/产品申请、6分类节点、1当前不可在绑定树找到的目录。无文件、审批中受控版本或正文上传对象。cleanupAttempted=true、cleanupCompleted=false；停用账号不代表这些配置和已完成申请已删除。Root后续复测需通过页面恢复本例账号，并先解决目录可见/可绑定问题。精确保留对象见fixture-manifest.json.residual。

## 写请求、证据保护与脚本修正

本轮包含用户已授权的真实UI写入：system角色分类/角色创建、菜单授权、用户创建、用户角色分配、新账号首次改密，dcc目录/分类/目录模板创建、项目申请22/23及其审核通过，以及三个用户的update-status停用。自然请求只记录path/method/status；未直接fetch/APIRequest/axios/SQL、未解析响应JSON作为业务事实。独立目录诊断本身写请求0，不把该0扩大为整个准备阶段零写入。

凭据仅在内存或本机Windows DPAPI保护的临时配置处理；输入用户密码、首次改密及登录前均不启动trace，配置表单含密码时停止trace，提交后真实刷新页面再恢复。credentials.dpapi.json被本目录.gitignore明确排除，不属于提交或共享证据。证据扫描对4份当前凭据检查20个trace ZIP及任务文本，明文命中0、损坏ZIP0；这项PASS仅指证据保护/完整性，不是业务PASS。

已修正本例旧脚本的共享fixture门禁、Root再次批准要求、租户selected-item定位、用户菜单定位、个人中心旧资料列表断言，以及固定TEXT源件夹具限制。正式上传支持PDF/Office，未来正文验证按真实支持格式要求非空PDF canvas及正文标记搜索，或Office实际正文。当前没有正文，不声明这些内容断言已通过。

本轮保留所有失败原迹：隐藏checkbox输入定位失败（后改为点击可见checkbox并回读节点）、个人中心旧布局断言失败、新账号首次改密前权限监听超时、失效context登录页、分类“确定/保存”文案错误、续跑变量作用域错误。一次项目审批脚本在原生prompt处等待顺序错误，任务自有Node PID20088及其Chromium子进程被停止，原run标记ABORTED_SCRIPT_NATIVE_DIALOG_DEADLOCK且evidenceIncomplete=true；没有业务通过结论。后续脚本先安装真实dialog处理，再从页面复核PENDING_REVIEW/COMPLETED后处理，本轮两个项目的终态证据来自后续独立run。

10份任务CJS最终语法检查通过；复跑脚本会先回读已创建的本例分类/目录记录，避免盲目重复新建。两个交互UI helper结束后仍持有stdin的Node PID21348、43808已核对归属并停止；未停止/重启共享Java、Vite或其它线程进程。未改生产源码、共享配置、共享dispatch、数据库迁移；无Git提交/推送。

BDD/RED在product-blocker.md；GREEN/REGRESSION因生产修复不在本线程授权范围而NOT_RUN。不能用health UP、项目完成、账号创建成功或语法检查关闭目录缺口。未收到第2尾部/第6、8原文仍保持未知。

以下为历史只读授权阶段的结果，不能覆盖上述最新授权、已发生的UI写入和当前阻塞。

## 2026-10-16 追加只读盘点

- 实际命令：`node doc/tasks/20261016-dcc-e2e-19/read-only-inventory.cjs`，最终退出码0，机器状态 `READ_ONLY_INVENTORY_WITH_BLOCKERS`。最终时间（Asia/Shanghai）：2026-10-16 03:44:08—03:45:03。
- 浏览器启动前实际核对：int_qms分支、dispatch.preflightGate=read_only_preflight、runtimeVerified=true、固定8061/48061、Root runtime-health HTTP200/UP、health与dispatch Jar SHA一致 `b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071`。后端PID24528是Root health证据，不冒充本线程重新启动/核对了生产包内容。
- 使用身份标签 `芋道源码/admin`，页面显示名“瑛泰管理员”。凭据仅在内存从根AGENTS读取，登录后才开始trace，未持久化认证上下文。真实点击可见菜单“文控中心 -> 受控浏览”，到达 `http://127.0.0.1:8061/dcc/controlled-file/browser`；未手工拼业务入口或正文地址。
- 非敏感机器清单：`read-only-inventory.json`（最终run的原样副本）；原始运行记录：`runs/read-only-2026-10-15T19-44-08-312Z/read-only-inventory.json`。同目录保留11张截图及 `login-after-readonly-trace.zip`。trace包含登录后的真实菜单、表格、搜索和项目选择；无登录填密步骤。保留前几次脚本定位/加载问题及前置超时尝试，不以历史通过覆盖本次故障。
- 产物核验：最终trace ZIP完整性通过，608个条目、590个截图资源、1个trace事件流；授权密码字节命中0、未脱敏认证头值0；11张独立截图路径全部存在。根inventory与最终run清单SHA256相同。两份CJS语法检查退出0；只读脚本结构扫描直接网络JSON读取0、fetch/APIRequest/axios/localStorage调用0；唯一page.goto用于登录，其余业务入口来自真实菜单点击。

|只读动作|实际DOM事实|范围和结论|
|---|---|---|
|进入项目目录/搜索页|默认“项目文件与引用”；“全局文件”“项目文件夹”页签可见；默认版本范围“当前生效受控版本”|真实页面入口/控件可达，不代表业务权限或文件内容可用|
|项目列表|本页可见20条项目，包括IDI、DCC-E2E-DCFINAL两项目、G44项目、G53-P-20261005、HTML26/HTML29项目等，精确名称/编码保存在inventory|仅当前项目分页采样，不代表全部项目；没有修改这些历史/共享项目|
|结果可见字段|选择、文件名称、编号、所属项目、来源文件夹、版本、状态、引用情况、操作|9个真实表头已采集；候选文件行0，文件名称/编号/状态/版本值未取得，不能从历史或响应补造|
|默认全局列表|可见“系统异常”和“暂无数据”|不能把报错空表判成无数据或通过|
|按名称搜索任务前缀|输入DCC241016C19-NAME，自然GET browser-page，GLOBAL/latestVersionOnly=true，HTTP200；DOM“暂无数据/共0条”|当前admin查询的实际观察；不证明任务文件已准备或不可访问范围被隔离|
|按编号搜索任务前缀|输入DCC241016C19-NUM，范围同上，HTTP200；DOM“暂无数据/共0条”|与名称搜索分别留图；未解析响应JSON|
|全部版本/空关键词搜索|页面切到“全部版本”，自然GET在客户端被中止（net::ERR_ABORTED），页面“timeout of 30000ms exceeded”|真实目标查询阻塞，未调高生产超时或重启服务；无法由此取得候选文件|
|前五项目文件夹抽样|IDI、DCC-E2E-DCFINAL-20260924-DCFINAL-01、DCC-E2E-DCFINAL-20260924-02均显示“项目尚无文件夹”；G44项目、G53-P-20261005均显示G44文档|等待各项目自然folders请求完成，精确核对当前项目标题后采集；这是页面显示事实，不推断业务资料本来应是什么|
|点击可见文件夹|G44目录显示0条/暂无数据；G53目录显示“系统异常”及0条|未取得候选文件，无正文“浏览”动作；无文件来源/版本一致性PASS|

版本下拉16项真实文案均已留在inventory：当前生效受控版本、全部版本、工作小版本、草稿、各审批阶段及历史状态。没有把目录级搜索等同“当前项目跨全部文件夹搜索”；后者正式入口缺口仍保留。

## 请求分类、异常和后续缺口

- 最终运行监听到认证POST `/admin-api/system/auth/login` 2次，HTTP200；它们单列为AUTHENTICATION，不是业务写入。其余42条被记录请求为GET读取（只保存method/path/status），**非认证POST/PUT/PATCH/DELETE为0**。没有直接API/SQL、创建/修改/上传/审批/签名/权限/清理动作。数量只说明页面可观察请求，不据此声称后端绝无登录审计写入。
- 全局壳层控制台：个人工作台待处理数量、审批待办数量出现“系统异常”；记录在inventory.warnings，未忽略或写成通过。目录主列表“系统异常”和全部版本查询超时是当前目标链路的实测阻塞，不能由health UP推断其正常。
- 完整E2E-19仍缺：readerBoth/readerP1两个非admin真实账号及独立查看范围、任务自有P1/P2和三份可辨识正文文件、各固定版本/显示状态和页面准备证据、当前项目跨文件夹搜索正式入口，以及主gate=execute。admin项目清单不能代填为任务自有fixture，旧项目/旧文件不能作为本轮PASS。
- `cleanupAttempted=false`：未执行业务清理；脚本关闭context/browser。无新建/修改的业务数据需要本线程清理，既有共享/历史项目只读保留。没有Git提交/推送、生产源码或共享配置修改。`.cjs`仍被仓库 `.gitignore:103` 忽略，已进入Cleanup Keep；本轮没有stage授权，未执行git add。

以下保留第一阶段历史记录。其“未启动浏览器”“runtimeVerified=false”“等待运行态”等语句只描述第一阶段，当前运行态和只读执行以本节及最终inventory为准。

## 第一阶段历史验证记录

业务结论：**NOT_RUN / PREPARED_WAITING**。当前 dispatch 为 `prepare_only`；23个参数分支均未进行真实页面执行，不能记为E2E PASS。第一阶段的脚本与前置分析已完成，供Root review。

## 已核对事实

- 实际分支：`int_qms`；只读核对时 HEAD：`f06833f06c868dc3a451eb63d01381a467ecfe92`，未切换分支或执行Git写操作。
- Root dispatch 环境：`http://127.0.0.1:8061` / `48061`，`runtimeVerified=false`、`roleFixtureVerified=false`；8061未监听、48061为旧外部Jar是Root提供的前置事实，本线程未自行重启/探测服务或用旧运行包验收。
- `IntRuoyiFronted/package.json` 的真实 `test:e2e` 为 `playwright test`，依赖 `playwright=1.60.0`；`playwright.config.ts` 从 `playwright/test` 导入，正式Chromium project存在。本任务目录中的CJS通过明确的前端 `node_modules/playwright` 路径加载，不新增或修改共享package脚本。
- Node `v24.19.0`、Python `3.14.7`；Playwright缓存Chromium可执行文件存在：`C:\Users\D01020\AppData\Local\ms-playwright\chromium-1223\chrome-win64\chrome.exe`。仅核对文件，不启动浏览器。

## 正式页面及权限边界

|目的|源码位置与真实页面控件|核对结论|
|---|---|---|
|菜单与浏览入口|`IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql` 的 `controlled-file/browser` 菜单；`IntRuoyiFronted/src/views/dcc/controlled-file/workbench/index.vue` 的“受控浏览”按钮|正式路由 `/dcc/controlled-file/browser`；真实执行仍须由账号可见动态菜单进入，静态路由存在不代表角色可见|
|项目与文件夹|`browser/index.vue` 默认 `project` 模式；`browser/ProjectBrowserPanel.vue` 的项目名称/编码输入、“查找项目”、文件夹树|树由项目正式文件夹生成，按项目及叶子文件夹选择；脚本不调用Vue组件或直接设置状态|
|名称/编号搜索|`ProjectBrowserPanel.vue` 的“全局文件”“项目文件夹”页签及“文件名称、编号或项目名称”输入|两个字段分别使用互不重叠的任务名称/编号前缀；脚本填入后断言DOM value，监听自然查询路径及HTTP状态，不解析业务响应|
|来源及版本状态|`browser/ProjectFolderFileTable.vue`|固定断言文件名、编号、所属项目、来源文件夹、版本和状态六列；默认“当前生效受控版本”|
|打开所选版本|同表“浏览”调用 `ProjectBrowserPanel.vue` 的 `preview`；路径由 `view/presentation.ts` 正式生成|点击同一可见业务行后检查新窗口的固定文件ID、viewer query、`dcc-content-version-identity` 和可见正文标识，不拼URL打开正文|
|菜单权限|`DccControlledFileController.java` 的 `/browser-page` 使用 `dcc:controlled-file:query`|这是精确权限标识；实际阅读范围还受文件/目录及查看矩阵约束，不能据菜单推断正文可读|
|项目级搜索缺口|`browser/project-browser.ts` 的 `loadFiles`；`api/dcc/controlledFile/applicationRead.ts` 的 `loadDccProjectBrowserPage`|正式模式仅有 `GLOBAL` 与 `PROJECT_FOLDER`；后者同时要求projectId和folderId，前者禁止只传projectId。未发现“当前项目全部文件夹”页面入口；这是静态前置发现，不是已经真实复现的产品FAIL|

上述源码路径统一相对 `IntRuoyiFronted/src/views/dcc/controlled-file`，除表中单独注明者。

## 参数分支

机器可读完整23项清单在 `prepared.json.subcases`，全部状态均为未运行。

|分支|组合与数量|准备状态|
|---|---|---|
|按项目/文件夹浏览并打开正文|readerBoth × P1主目录/P1兄弟目录/P2目录，readerP1 × P1两个目录；5项|已编码，等待真实角色和三个ACTIVE文本样本|
|选定项目文件夹名称/编号搜索并逐项打开|以上5个账号/目录组合 × 两种查询；10项|已编码；仅证明所选文件夹，不替代项目全部文件夹|
|授权范围名称/编号搜索并逐项打开|两个真实阅读账号 × 两种查询；4项；readerBoth预期3行，readerP1预期2行|已编码，核对精确结果集合、来源、版本正文|
|无权限P2精准搜索|readerP1 × P2名称/编号；2项|已编码；预期空结果，无浏览按钮、无P2正文，不猜测URL|
|当前项目全部文件夹搜索|名称/编号；2项|BLOCKED：待Root确认正式入口/业务口径，不默认将“项目文件夹”等同“当前项目”|

使用文本文件是本例可辨识正文的夹具设计，未替代E2E-24的多格式验证，也未扩大到E2E-20的地址访问/撤权或E2E-21的历史正文验收。

## 真实身份和数据缺口

1. dispatch尚未放行，运行态和账号夹具尚未由Root核实。
2. 缺少两个非admin真实账号：readerBoth可读P1/P2、readerP1只可读P1。需要同租户、真实显示名、精确角色名称/code和页面权限证据；业务定义没有指定阅读角色code，不自行编造。`dcc:controlled-file:query`、查看矩阵和正式目录范围需分别核实。没有登录、没有猜密码、没有重置/分配角色或菜单。
3. 缺少任务自有项目、两个P1叶子文件夹与P2叶子文件夹、三份独立ACTIVE文本正文的真实页面准备证据。需要记录固定版本ID、显示版本、状态、唯一正文标识、完整文件夹路径及来源项目。示例 `fixture.example.json` 故意留空，不是假数据、不能直接执行。
4. 未提供“当前项目跨全部文件夹”正式入口。P1兄弟目录是区分项目级范围与单文件夹范围所需数据，不允许把逐文件夹结果拼装成项目级搜索PASS。

## 静态检查与复跑

工作目录：仓库根 `C:\IntRuoyiAll-int_main`。

|实际命令/检查|退出码|结论|
|---|---:|---|
|`node --check doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs`|0|语法通过|
|`python -X utf8 -m py_compile doc/tasks/20261016-dcc-e2e-19/sanitize-trace.py`|0|语法通过；创建了本任务内pycache，未作为业务证据|
|`node doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs --prepare-check`|0|STATIC_ONLY / PREPARED_WAITING，gate=prepare_only，browserLaunched=false，businessActions=0；生成23分支|
|从前端明确依赖路径加载 `playwright/test`、检查expect及导出plan|0|依赖可解析；没有启动浏览器|
|runner/dependency/脚本禁止直调结构扫描|0|正式runner和Chromium project存在；直调fetch/APIRequest/axios/localStorage命中0；不等于真实业务PASS|

Root放行后，将经过页面准备及review的manifest保存在本任务目录，显式提供以下环境变量，再运行 `node doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs`。脚本默认无账号密码，不复用已有认证上下文。

- `DCC19_BASE_URL=http://127.0.0.1:8061`、`DCC19_BACKEND_URL=http://127.0.0.1:48061`、`DCC19_FIXTURE=<本任务目录内经review的manifest路径>`。
- `DCC19_BOTH_USERNAME`、`DCC19_BOTH_PASSWORD`、`DCC19_P1_USERNAME`、`DCC19_P1_PASSWORD`：仅经Root已授权来源临时注入，不在命令、证据、任务记录保存值。
- 可选 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`：显式本机浏览器路径；缺失时使用已确认存在的Playwright Chromium文件，不下载浏览器。

真实模式在加载凭据/Playwright前检查dispatch和分支，在launch前再次核对；gate未放行时退出20并输出PREPARED_WAITING。登录在独立context完成，从登录页核对租户、个人中心核对账号及完整角色名称集合；不记录登录trace。业务trace在登录后启动，归档前由 `sanitize-trace.py` 删除认证头、cookie、token和密码等内容，保留DOM与截图。需要Root review真实运行后的选择器、trace内容与每个分支断言，本阶段不声称它们已实测。

## 收尾和边界

- 浏览器：未启动；业务写入：未执行；真实trace/截图：未生成，不能用静态检查代替。
- `cleanupAttempted=false`、`cleanupCompleted=false`、`residual=[]`：仅表示本阶段未产生业务数据，不代表未来夹具已经清理。将来浏览可能生成正式阅读/审计记录，不能据脚本只读交互宣称后端零写入。
- 只创建/修改本任务目录文件。未创建子Agent、未发送其它线程消息、未修改生产/共享配置、未运行Maven或完整构建、未启动/重启服务、未提交/推送。
- 第一阶段已可交Root review；真实执行及工程最终收口仍保留未完成。用户禁止Git写入且Root独占最后收口，本线程不执行cleanup apply，不删除Root尚未review的测试资产。
- 未收到的第2尾部、第6/8原文继续保持未知；不引入盖章、重提或作废关联规则。

最终cleanup preview实际退出0：保留全部脚本、79份trace及最新23项截图/结果；仅任务内__pycache__/sanitize-trace.cpython-314.pyc列入delete。未执行apply（Root保留资产/最终收尾）；Git未授权，正式状态blocked，业务结论仍23 PASS。

20261019-r2收尾验证：先ready_for_closeout后cleanup preview实际exit0，全部本轮证据及79trace保留，仅任务pycache列delete；未执行apply/Git。修复/验证完成，工程状态按无提交推送授权规则记blocked，交Root复核。


20261020-r2-round4收尾：已先标记ready_for_closeout，cleanup preview实际exit0；本轮全部70项新增资产及前轮handoff均keep，delete仅原任务pycache，实际删除0。未apply/Git，工程状态blocked；业务覆盖23 PASS，原首遍7 FAIL及补跑runner BLOCKED/exit20不改写。交接rerun-20261020-r2-round4/handoff.json，随后HOLD，不追加新一轮。
