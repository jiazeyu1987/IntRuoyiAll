# E2E-17 全员下发与逐人签收验证报告

## 当前原16项证据交接结论

remaining-16-evidence-handoff-20261021.md/json逐项对应原R3的16个BLOCKED、4418只读事实和4369同键真实UI PASS。已核验16/16有效旧PASS及引用截图存在，当前独立未覆盖业务动作0，不为填满4418重复签收；4418旧9 PASS/16 BLOCKED不改。四trace文件SHA与原CRC/隐私报告一致，共288帧，raw0；runner26148不存在且原exit0、browserClosed=true/context0。延迟5296秒单列Root/E15共享调度，非本次包归因。本轮无浏览器/签名/业务操作，只做文件核对。

## 2026-10-21 4418单次只读窗口

实际Playwright核对PASS（仅只读观察，不是签收业务PASS）：4418/A1 ACTIVE，实际生效/下发01:01:13，原U正式名单三人待签，各本人有精确下发消息。原16项生效/下发前置已具备；签收/重复/权限/范围外本人页面及签后追踪尚未执行，不改旧分支计数。详细逐项判定见readonly-4418-20261021-handoff.md；原始本次证据artifacts/readonly-4418-20261021/result.json。四份脱敏trace CRC/隐私PASS，4截图、288帧；runner26148退出0且不存在，browser closed/context0；无业务写请求。生效早于本轮部署，不能归因于新包；具体调度根因未知，HOLD动作前。

## 2026-10-21 原合同证据对账

六项原始断言及精确截图逐项对应见evidence-reconciliation-20261021.md/json。原例的R3新fixture为4369/A1（文件2054545668044084369），25 PASS，原合同全员范围与逐人签收PASS保留。后续独立fixture4418/A1为9 PASS/16 BLOCKED，不造成原例断言缺口。正式模型单一deptId；同人跨部门正例依用户确认标为不适用/需求范围外，不执行、不计PASS。历史4234/A1的49分40秒单列为共享调度/排期问题，5625覆盖及4418延迟根因未定。仅文档对账，无新业务运行或新门禁。

## Root复核补充：调度证据范围更正

本例查询明确按处理器dccControlledFileTargetedActivationJob筛选，“共1条”只表示该筛选下5629暂停且目标旧4228，不是整个系统唯一生效任务。Root转述E15只读观察：正式批处理5625启用并每分钟运行；本例未重开页面验证该观察。静态DccControlledFileActivationJob#execute按租户遍历dueVersions并activateDue，与仅处理tenantId:fileId的定向handler不同；静态入口不证明本次5625覆盖4418。

T+163秒仍待生效、无分发及9 PASS/16 BLOCKED结论保持。5625是否覆盖4418、为何未激活仍未定，撤回以5629暂停/无本例独占目标解释根因的推断。原始result、动作、截图、trace、首次错误和关闭回执不变；原始reason中的旧说法仅为历史措辞，以本补充为准。未重开业务、签名或调度。详情scheduler-scope-correction.json。


## R3第三轮本轮终态（新4418/A1）

业务BLOCKED：9 PASS、0 FAIL、16 BLOCKED、0 NOT_RUN。原合同6 PASS/10 BLOCKED；附加T前正文3 PASS，逐人重复/权限6 BLOCKED。未用旧PASS替代本轮。

新文件2054545668044084418/A1，BPM 7eece4dc-cc9a-11f1-ada5-b082e25ec548。真实UI新建、S主管签名、L签名指派、C会签、A批准并确认原U负责人、D文控、原U延迟确认已完成。受控/确认23:29:59、签名#1332，T23:32:57；三人未受控/待生效均无通知，T前三人全部版本无正文入口。仅单人单部门，多部门同人移除且不计PASS。

23:35:40（T后163秒）U正式页仍待生效，无生效/启用/下发时间及分发记录；本例只读按定向handler筛选仅见5629暂停且目标旧4228；未改作业。该筛选不覆盖正式批处理5625；5625覆盖情况及未激活根因未定。生效后名册、六人通知、三人正文ready/本人水印、首次签收、重复、权限及3→2→1→0未执行并逐项BLOCKED。未把T后无通知记成范围隔离PASS；仅确定后续操作所需生效前置未达到，原因未定，未证明产品缺陷。

保留2个首次脚本错误：loader require未定义、补充trace重复start；仅校准读取/trace窗口，未重做成功动作。U DAILY拒绝补拍发生在T后仍待生效，不计T前PASS；三收件人T前检查各自早于T。文档组装曾出现list.get错误，写文件前停止后修正，未影响业务现场。Root旧E03签名UNKNOWN保留不被本轮结果覆盖。

27份脱敏trace/2904帧、22独立截图，解压CRC/隐私及本轮分支证据结构PASS（不是业务PASS）。browser closed/context0；runner28148退出0且无进程或直属子进程。新文件和合法签名保留复核，未改产品/共享配置、未直接API/DB执行业务，未Maven/build/restart/Git/子Agent/消息。本轮HOLD，不开始下一轮。

正式交接：rerun-20261020-r3-round3/handoff.json。原始结果：artifacts/rerun-20261020-r3-round3/result.json。


## Root单部门口径复核（2026-10-20，本次仅文档）

原验收合同E2E-17已明确每人仅归属一个部门。保留未受控/待生效不下发、生效后所选部门全员下发、范围外及默认会签排除、每人独立签收与待签跟踪，以及既有正文/重复幂等/权限补充。移除同人跨多个所选部门成功正例，记REMOVED_FROM_SCOPE_BY_USER，未执行、不计PASS或N/A。最新4369/A1的25 PASS/0 FAIL/0 BLOCKED为既有执行，本次无新E2E或Maven。

现有名册仅证明ReceiveA的R1/R3、ReceiveB的R2这三名不同接收人的下发义务；不推断未记录实际归属、全量组织规则或现在的组织现状。9次辅助首次错误、查询超时观察、R1 trace拒绝原因未确定、激活延迟610秒、旧4321/A1的10 BLOCKED及更早无效/未完成补核事实全部保留。范围变更只消除同人双归属业务阻塞，不撤销失败和未验证记录。当前适用范围无业务阻塞；工程因本轮禁止Git提交推送及未执行cleanup apply仍blocked，HOLD。

详细交接及证据保护：scope-review-20261020.json；最新运行交接：rerun-20261020-r2-round3-handoff.json。


## rerun-20261020-r2-round3 工程收尾回执

官方task_closeout.py --mode preview退出0：keep54、delete候选397、warnings0；其中114项为本轮证据，全部保留供Root review。仅预览，未执行apply、未删除文件或业务数据。本轮明确禁止Git提交/推送，按docs/task-closeout-rules.md将工程状态记blocked；业务25 PASS/0 FAIL/0 BLOCKED不变，不标completed。浏览器context0、runner35172已退出0；当前HOLD，禁止开始第四轮。预览原文：artifacts/rerun-20261020-r2-round3/cleanup-preview.txt。


## rerun-20261020-r2-round3 最终业务结果：PASS

新独立文件2054545668044084369/A1，BPM aefaf959-cc4c-11f1-ada5-b082e25ec548。原合同16项、额外T前正文3项、Root补充逐人重复/权限6项，共25 PASS、0 FAIL、0 BLOCKED。未复用历史PASS；同人多部门前提按用户2026-10-20决定移出本版本范围，不记为执行PASS。

实际受控/延迟确认14:12:52（签名#1220），原T14:22:50；Root安排E16唯一操作5629，精确日志48058自然14:33:00→14:33:02，14:34:29暂停。正式U页面实际生效/启用/下发均14:33:00，原时间和签名不变；排队延迟610秒如实保留，不宣称准时自动生效。

两部门冻结名册为ReceiveA的R1/R3和ReceiveB的R2；范围外OUT及会签L/C无通知。三人各自真实非空PDF canvas、唯一新正文搜索、本人身份水印及首次签收；正式待签3→2→1→0。重复提交均幂等成功，逐人原签收时间、意见、名单、待签人数不变。权限仅按真实前端边界核验：签前签后日常目录未新增本文件、上传菜单缺失，精确下发正文无下载/打印/编制操作；未扩大到API/DB权限探测。

T前三人日常/全部版本列表无目标正文入口；原U真实点击DAILY入口被拒绝且canvas0，不将入口隐藏虚记disabled。接收人是精确下发阅读授权，不能要求其在日常目录有文件。本轮PDF以canvas和预览登记时间验收，Office专属打开提示不适用于PDF。

保留9次首次辅助错误：main标签、DCC项目空格、异步选项、批准owner-picker时点、通用空keyword全部版本查询30秒超时、误要求R1日常目录出现文件、误套Office提示、PTY长JSON截断、R1保存结果短trace脱敏ValueError。最后一项精确原因未确定，拒绝及原ZIP均按既有工具删除；只读重拍已保存页的新trace通过，未重做首次签收，未放宽脱敏器。完整错误及校准保留在本轮result/ui-actions及traceIssues。通用查询超时为本轮保留观察项，未改产品。

node rerun-20261020-r2-verify.cjs退出0：58份sanitized trace逐条解压CRC/凭据/认证资源扫描PASS，6112帧；53独立截图，raw ZIP0。三人首次签收正式时间：R1 2026-10-20 14:43:50, R2 2026-10-20 14:47:40, R3 2026-10-20 14:48:29。浏览器已关闭/context0，runner35172退出0且进程不存在。

最终交接：rerun-20261020-r2-round3-handoff.json；证据：artifacts/rerun-20261020-r2-round3/result.json、evidence-verification.json。业务实体/签名/截图及trace保留Root review，未清理业务数据；未改产品、共享配置、job/T、旧文件、构建/重启/Maven、Git、DB写入或子Agent。当前HOLD，不开始第四轮。

| 分支 | 结果 |
|---|---|
| UNCONTROLLED_R1 | PASS |
| UNCONTROLLED_R2 | PASS |
| UNCONTROLLED_R3 | PASS |
| PENDING_EFFECTIVE_R1 | PASS |
| PENDING_EFFECTIVE_R2 | PASS |
| PENDING_EFFECTIVE_R3 | PASS |
| BEFORE_T_BODY_R1 | PASS |
| BEFORE_T_BODY_R2 | PASS |
| BEFORE_T_BODY_R3 | PASS |
| ACTIVE_R1 | PASS |
| ACTIVE_R2 | PASS |
| ACTIVE_R3 | PASS |
| ACTIVE_OUT | PASS |
| ACTIVE_L | PASS |
| ACTIVE_C | PASS |
| RECEIPT_R1 | PASS |
| REPEAT_R1 | PASS |
| PERMISSIONS_R1 | PASS |
| RECEIPT_R2 | PASS |
| REPEAT_R2 | PASS |
| PERMISSIONS_R2 | PASS |
| RECEIPT_R3 | PASS |
| REPEAT_R3 | PASS |
| PERMISSIONS_R3 | PASS |
| INDEPENDENT_TRACKING | PASS |

---

## 2026-10-20 第三轮独立重跑结果

**BLOCKED：6 PASS、0 FAIL、10 BLOCKED**，当前结论以本段及rerun-20261020-round3-handoff.json为准；下文此前修订范围PASS属于历史执行，未用于本轮判定。

本次Playwright新建DCC20C17R31792463779953.pdf，文件2054545668044084321、A/1，BPM71325842-cc2f-11f1-a8a5-b082e25ec548。原U/S/L/C/A/D及R1/R2/R3本人登录，通过真实UI完成新申请、主管、指派、会签、批准、文控及U延迟时间签名。未受控、受控待生效阶段R1/R2/R3各自站内信均无本次正式下发通知，共6分支PASS。

原编制人选择北京时间10:45:28，受控及确认签名时间10:42:31、签名#1106。10:46:59（计划后91秒）和10:49:05正式页面仍显示受控待生效，实际生效/日常启用/下发时间尚未产生、分发表为空。定向任务筛选共1条：Root共享5628暂停，目标为旧E16/4228；无本例独占任务。本轮Root明确禁止修改该共享对象，未操作任何调度。自然生效、所选部门全员下发、范围外/默认会签排除、三人正文水印、三人本人签收及3→2→1→0共10分支BLOCKED。计划后六名账号无通知仅记观察，不冒充生效后的范围隔离PASS。直接原因属于运行前置不足，未证明产品代码缺陷。

逐分支、对象、账号、自然HTTP状态、截图及操作记录均位于artifacts/rerun-20261020-round3-1792463779952/。29份脱敏trace共2437图像资源解压CRC/隐私检查PASS，27张独立截图；无raw ZIP。browserClosed=true、context0，runner29252退出0并已确认进程不存在。原历史数据未重签；本次新待生效申请及合法签名保留供Root复核，无临时基线变更须恢复。

保留两项脚本定位/时序错误：批准人前缀命中多账号后改精确匹配；延迟确认成功后密码框随面板移除，改用正式事实回读，未重复提交或签名。未用API/DB/响应JSON执行业务或作oracle，未改产品/schema/公共配置、未构建重启、Git、子Agent、cleanup apply，未启动下一轮。结构和trace检查只属于证据核验，不是新增E2E PASS。

## 2026-10-20 范围变更前状态（历史）

### 当前范围结论（用户确认后）

用户于2026-10-20正式确认“同一个人不能属于多个部门”。据此，E2E-17 Given 修订为每人仅归属一个部门；同人多部门成功前提从当前版本验收范围移除。该参数状态为`REMOVED_FROM_SCOPE_BY_USER`，没有执行，也不记为测试PASS。

修订后的E2E-17业务验收为**PASS**。依据是原4234/A1真实UI证据：所选部门对应R1/R2/R3三名不同接收人，各自登录并完成本人签名，未签数3→2→1→0，范围外人员未混入；Root已接受该主链。R4原5项定向Maven为5/5 PASS，作为代码回归证据。以上均为继承的既有执行结果，本轮只做范围重判，没有重跑UI/Maven。

此前`DECISION_REQUIRED`、`NOT_CONSTRUCTIBLE`及`overallFullyAccepted=false`记录反映原多部门前提仍在范围内时的历史状态，保留作历史，不再代表当前范围结论。当前结论仅覆盖E2E-17修订后的业务范围，不代表E2E-01–24整体通过。详见最新范围同步字段`decision-20261020-handoff.json#scopeDecision`。

当时的证据性收口：未发现可引用的正式业务批准，D17-01仍待决；三人逐签真实UI PASS及R4五项GREEN保留。该状态已被本轮用户确认的范围调整取代，历史机器交接细节保留在`finalEvidenceCloseout`字段。

当时仍为产品/验收范围决策阻塞D17-01，DECISION_REQUIRED/NOT_CONSTRUCTIBLE；该结论现为历史。详见[d17-01-disposition-20261020.md](d17-01-disposition-20261020.md)及[最新handoff](decision-20261020-handoff.json)。当时复核IC32:9、旧E2E-17要求、单deptId人员模型和正式直接成员来源，未发现获批多归属来源或真实双归属证据，因此未执行该参数UI复测。

两个互斥选择待业务决定：A撤回本版本成功前提，明确该参数N/A还是移出验收范围；B定义正式多归属数据源及按人/按部门签收、通知、计数语义。异常重复拒绝和重放幂等不能替代该成功正例。本轮保护证据SHA未变；人员表单历史指纹变化已复核，仍为单值deptId，其他11份历史源码指纹一致。仅文档/来源核验，无新Maven/UI/运行库检查。以下R2/R3“待执行”记录为历史，当前夹具结果以R4 GREEN为准。

## R3精确回归待执行（历史，已由R4完成）

本轮只需两旧夹具的实际GREEN与原5项定向回归，测试内容保持R2修复后SHA一致，未新增生产改动。当前Maven由24独占，17排在13之后，尚未获Root执行窗口；本轮实际tests0，GREEN未取得，不能以10月18日旧target XML或结构校验替代。精确命令、本次5方法/0失败/0错误/0跳过及新XML判据见repair-20261019-r3-handoff.json。

跨部门问题列为D17-01业务决策项，A/B最小选项见repair-20261019-r3-plan.md；仍NOT_CONSTRUCTIBLE，未擅自PASS/N/A。原人工晚登记不再列产品调度故障；已接受三人签收不重复执行、5627不操作。

## 2026-10-19 R2维修状态

已完成2个旧fixture的最小修改，**GREEN待Root串行Maven，尚未通过编译/运行**。Root原附加5项实际结果为3PASS/2ERROR：Controller stub仍用旧无context重载；W08旧断言把下发token当普通DAILY正文授权。前者改精确daily(900L) stub/verify并保留所有响应头断言，后者保留两次显式下发成功并增加DAILY拒绝和零额外storage/opened断言；只改两个既有测试方法，不改Query/Controller/权限。原RED源文件和XML已先封存，命令/结果边界见[维修交接](repair-20261019-r2-handoff.json)、[BDD](repair-20261019-r2-bdd.md)。

[原激活延迟复核](activation-delay-assessment-20261019-r2.md)：计划17:50:21时本例5627未登记，旧5626暂停/旧目标；18:39:43.905客户端尝试创建后，真实页面下一次18:40:00，日志48025自然触发成功返回1，实际18:40:01。2980秒差值不能当已登记cron漏触发，当前无调度生产RED，不改生产、不新建样本、不启全量扫描，也不把人工排期延迟写成准时。登记提交精确完成时刻未捕获，未编造。

跨部门参数进一步查到IC32集成验收2026-10-13明确“保存的直接部门启用成员、不读取下属部门”规则；不扩大模型，原E2E后加多归属前提待业务明确，仍未验收。三签和3→2→1→0既有PASS与final/manifest原证据不动，本轮无新增E2E或业务写入。

## 2026-10-19 仅未验收参数复核

本轮依据Root恢复派单只分析“同一人属于多个所选部门”，未重跑任何已通过分支。已读最新Root复核、manifest及final-acceptance：原4234/A1三人签收和计数证据继续继承，不因旧blocked记录重建对象。

结论：当前单deptId＋启用直接部门成员的正式模型使跨部门同人成功输入不可达，属原验收前提与模型冲突；不是本轮已复现的去重生产缺陷。正式来源对重复账号整单ROSTER_DIRECTORY_INVALID，重复部门先ROSTER_REQUEST_INVALID；下游按部门义务建记录，不应宣称已有通用跨部门合并能力。父子部门/多岗位/多角色/换部门后重试均不能构造同次合法多归属。

[只读判定与最小业务选项](overlap-parameter-assessment-20261019.md)及[源码/原证据指纹](overlap-parameter-assessment-20261019.json)已交付。建议业务明确“按当前模型，将原跨部门成功前提判不适用，另验异常重复拒绝与重放幂等”；这属于待确认范围调整，不是原正例等价替代，未经确认不改原参数PASS/N/A。若仍要求同人跨部门，须单独定义正式多归属来源及按人还是按部门签收义务，本轮禁止扩模型或伪造名册。

本轮没有新增Playwright、Maven、API/DB、业务/组织/生产、服务或Git写；既有测试仅定位待Root按需串行执行，不记已运行PASS。Root环境READY不改变本例仅分析范围，原主链不复验。

## 最终结论

CONSTRUCTIBLE_BRANCHES_PASS_WITH_UNACCEPTED_OVERLAP_PARAMETER。可构造业务分支已真实通过，Root已明确接受三次本人签收及U的3→2→1→0正式DOM回读；收件行图像补证也已完成。单部门组织模型无法构造同人跨多个所选部门，原参数独立未验收，不记整例全参数PASS。

## 2026-10-19 R4 定向 Maven 回归

Root按24→13→17顺序授予本例独占Maven窗口。本轮运行repair-20261019-r4-handoff.json所列原5项精确方法，命令exit 0；本轮新生成并归档的三份Surefire XML合计5 tests、0 failures、0 errors、0 skipped。实际方法为ControlledFilePreview的daily(900L)委派校验、W08下发正文读取/上下文恢复及失败不产生成功回执、签收正文成功及审计策略缺失回滚。

该结果验证R2修复后的两个测试fixture；没有生产代码修改、部署、E2E重跑或业务写入。此前3 PASS/2 ERROR仅作为维修前RED保留。已通过的三人签收主链证据未重跑；D17-01跨部门同人仍NOT_CONSTRUCTIBLE且未获业务范围决议，因此整体仍未完整验收/未标completed。

## 2026-10-19 R5 D17-01决策状态

按R5派单仅更新[业务决策材料](overlap-parameter-assessment-20261019.md)及其JSON状态，并新增[本轮交接](repair-20261019-r5-handoff.json)。选项A要求业务明确本版本同人跨部门成功前提不适用，并将异常重复身份拒绝/重放幂等作为独立验收；选项B要求业务定义正式多归属来源及按人或按部门的签收义务。当前未收到业务选择或批准依据，继续保持`DECISION_REQUIRED`、`NOT_CONSTRUCTIBLE`、`businessPass=false`、`notApplicableApproved=false`。

R5未运行Playwright、Maven、异常重复拒绝/幂等测试，也未操作服务或业务数据。真实三人逐签UI主链PASS为继承证据；R4原5项定向Maven为已验证GREEN；R2原5项3 PASS/2 ERROR仅为维修前RED。原跨部门成功正例未执行且不可构造，属于当前业务阻塞，不能用独立异常拒绝或重放结果替代。

决策与handoff JSON解析、状态/证据断言及修改Markdown行尾空白检查通过；`final-acceptance.json`和`fixture-manifest.json`保护SHA保持原值。验证仅覆盖文档结构与证据保护，不是新的测试或E2E结果。

原文件2054545668044084234，编号DCC241016C17DISTRIBUTION，版本A/1，BPM157cc551-cad1-11f1-a035-b082e25ec548，租户芋道源码/1。全过程复用原文件、原流程、原三收件义务；没有重建文件、改写旧签名或调整阅读授权。

## 真实业务结果

| 分支 | 结果与证据 |
| --- | --- |
| 未受控不下发 | PASS：R1/R2/R3独立本人站内信无正式下发模板，原截图时间早于受控时间；fixture-manifest.businessResults.UNCONTROLLED_* |
| 受控待生效不下发 | PASS：三人原截图在17:38:25之后、17:50:21之前，无正式下发模板；生效后三人原授权实际读到同一下发版本，资格连续性已补证 |
| 受控及生效时序 | 受控17:38:25，原计划17:50:21，实际生效/日常启用/下发18:40:01；调度延迟49分40秒，未回填或宣称准时 |
| 所选部门全员 | PASS：ReceiveA为R1/R3，ReceiveB为R2；原U正式分发记录精确对应两部门、三人，三人本人正式消息均可见 |
| 范围外/默认会签排除 | PASS：既有独立OUT/L/C站内信无正式下发模板；L/C的审批待办伴生消息已严格排除，不按文件号泛匹配 |
| 精确正文与水印 | PASS：三人各从原通知进入原下发/收件义务，真实PDF正文搜索命中本例唯一源文字，身份水印可见；自然preview均application/pdf且必需水印头存在，不读JSON作oracle |
| 三人分别电子签收 | PASS：各自新context本人登录、本人密码签名，U逐次新会话回读仅当前人已签；签名时间如下 |
| 仍未签人员追踪 | PASS：U实际未签数3→2→1→0；R1之后R2/R3待签，R2之后R3待签，没有代签或模拟人数 |
| 同人跨所选部门去重 | NOT_CONSTRUCTIBLE_WITH_CURRENT_FORMAL_ORG_MODEL：直接单deptId模型，不扩组织、复制账号或伪造重复名册；见business-issues.json |

| 本人 | 分发/收件义务 | 正式DOM签名时间（北京时间） | 本人签后U回读 |
| --- | --- | --- | --- |
| R1 / 910496 | 273 / 276 | 2026-10-18 22:26:58 | 1已签、2待签 |
| R2 / 910497 | 274 / 278 | 2026-10-18 22:27:16 | 2已签、1待签 |
| R3 / 910498 | 273 / 277 | 2026-10-18 22:27:33 | 3已签、0待签 |

## 最终图像补证

最后一轮仅U真实登录及正式页面只读操作，补采文件身份、下发表及两个部门行。表格按部门呈现两行，包含三个收件人；不是伪造三个物理表格行。常规列宽会省略长收件人文本，因此额外用真实hover提示显示完整姓名和签名时间，没有改列配置。

- 完整R1/R3收件明细：[ReceiveA真实提示截图](artifacts/acceptance-2026-10-18T15-10-44-468Z/U-recipient-full-signature-text-0.png)。
- 完整R2收件明细：[ReceiveB真实提示截图](artifacts/acceptance-2026-10-18T15-10-44-468Z/U-recipient-full-signature-text-1.png)。
- [原下发表](artifacts/acceptance-2026-10-18T15-10-44-468Z/U-three-signed-distribution-table.png)、[原文件A/1身份](artifacts/acceptance-2026-10-18T15-10-44-468Z/U-original-file-A1-identity.png)。
- 新鲜DOM、原URL、U身份、两部门行、三个完整收件状态/时间均在fixture-manifest.finalLocatorReadback及final-acceptance.json中，记录时间2026-10-18T15:12:03.431Z。

旧U-original-all-three-signed-final.png实际为公共页500，已由invalidLateFinalCapture明确剔除；旧视口顶部图片单独不能显示下方收件行，不作为最终三签图像。Root接受的三次新鲜DOM仍完整保留，最新locator/hover图像另行补齐。

## 运行与授权边界

- 实际签收运行：int_qms，8061/48061，后端34440，200656 Jar，SHA256 264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5。
- 签收完成后后端34440异常退出。Root核实22:37:51 Windows事件1000；公共页500/租户识别失败不等于签收失败，不归因账号权限。
- 最终只读图像运行：同一200656/hash，恢复后端14864、前端24564；Root健康和官方真实登录证明为browser-replacement-recovery-proof.json。该轮没有重签、配置或作业写入，没有加载待验新源码。
- 旧5626仅按Root显式更新租约经UI逻辑删除，原日志保留来源于Root只读核实；新5627一次创建，日志48025自然执行精确本例目标返回1。首次确认ACTIVE即UI暂停并重开回读，随后归还Root/16，此后本例没有再操作5627。
- 本例无API/SQL/mock/force/storage注入、子Agent、Maven/构建、生产/服务/Git写。配置admin只承担授权配置/作业动作，三次业务签收均本人账号。

## 故障与修复前后证据

缺失的三项正式test-only策略均由Root完成精确备份/副本和源库首次重复验证，不由本例改门禁或造业务：signature.dcc.distribution-receipt.create、dcc.distribution.recipient.body-opened、dcc.distribution.recipient.signature-bind。各原RED、BDD、Root配置证据路径及本例原流程GREEN分别保留在signature-policy-issue.json、distribution-preview-issue.json、receipt-signature-bind-issue.json及execution-log.md。

正文缺头通过只读链路定位到成功写头前的body-opened审计，Controller/前端必需头和VIEW未改；Root补正式策略后原R1/R2/R3真实PDF、水印与头存在均通过。两次结果不明/会话失效均先U原行回读再推进，没有盲重签。

有一个失败结果窗口trace被安全脱敏器拒绝，原raw及失败输出已自动删除，不冒称该窗口trace通过。旧可变名R1-receipt-result.png曾被后续GREEN覆盖，不再把该路径当历史拒绝图片；原错误文字、工具/操作日志及Root精确修复证据仍在。成功签收链及最终只读补图的合法脱敏trace保留。

## 证据入口及资源收尾

[最终结构化证据](final-acceptance.json)包含原身份、6个负例资格连续性、三人实际PDF/自然响应、三次签名及计数、范围排除和最终图像。fixture-manifest.json保留全过程与历史，两个最新run的ui-actions.jsonl为实际执行记录，不能重放其中历史写入。

最后runner41444正常退出0，browser/context已关闭，无raw ZIP；原签名/文件/名册/任务自有配置保留供Root review，未删除审计样本。单部门重叠参数仍未验收，整体不标completed；Root本轮禁止Git写，未提交推送。没有以资源关闭冒充业务数据清理或完成整个原参数集。

本轮只做必要的本例Node语法、helper离线断言、JSON结构和git diff --check；无新增Maven/全量构建测试。历次验证报告原文已归档到当前补图run的verification-report-before-final-consolidation.md，不以过期blocked/paused结论覆盖上述最终业务结果。


## rerun-20261020-r2-round3 — in_progress

Root正式EXECUTE；运行SHA709e78bd…aaa147、backend41508/48061、frontend20976/8061匹配。新文件2054545668044084369/A1，BPM aefaf959-cc4c-11f1-ada5-b082e25ec548。原U/#910490无培训，确认签名#1220于14:12:52，未来T=14:22:50。未受控/待生效三人无通知6项及T前三人正文入口隐藏3项已通过；U实际点击DAILY正文入口，页面拒绝访问且canvas0。已交Root精确handoff，等待唯一调度E16；未改job/旧文件。首次main标签、DCC项目空格、动态选项与owner picker定位错误完整保留；通用空keyword全部版本首次30秒超时另存截图，精确本例查询成功，不隐藏首次失败。新runner35172仍存活，后续结果未验收。
