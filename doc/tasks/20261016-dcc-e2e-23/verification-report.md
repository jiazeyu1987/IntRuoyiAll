## Current E23 Conclusion

Root accepted the later A→B late-response isolation branch as PASS. The original `rerun-20261020-r2-round4` aggregate remains FAIL/HOLD with its first BLOCKED and calibration FAIL preserved; the original six main-branch PASS facts remain historical and were not rerun. No E23 product code changed. The later independent sanitized-trace privacy/CRC audit is PASS: 415/415 ZIP entries, 1,772 JSON lines and zero findings under the enumerated checks; raw trace is absent and cannot be retrospectively audited. See `runs/2026-10-20T08-27-03-769Z-709e78bd/trace-privacy-crc-audit.json` for exact boundaries. E23 remains on hold; no commit or push was made.

The subsequently authorized audio/video ordinary-attachment supplement is **business PASS, 6 PASS / 0 FAIL / 0 BLOCKED; cleanRun=false**. One new A/1 file `2054545668044084384` became ACTIVE through the original four approval nodes and applicant training. Both positive identities played both media through native controls; NoAccess was stably denied. Exactly two native downloads matched input SHA-256. Root accepted the business result. Ten sanitized traces passed privacy/CRC audit (3,570 ZIP entries / 9,858 JSON lines); the initial placeholder false-positive FAIL remains separately retained, with no ZIP modifications. The ending 33-source/Jar/runtime gate passed. All contexts/browser/runner closed. Final evidence: `media-attachments-20261020-handoff.json` and `media-attachments-20261020-verification.md`. The rejected source-name HTTP400 and six script assertion failures remain; no old round count or E24 result changes. No production edits, reruns, Git writes or data cleanup occurred. Task remains blocked with final Root evidence review accepted; Git/cleanup closeout remains unauthorized.

Preparation validation: UTF-8/JSON structure, BDD/action sections, fixture and encrypted-credential file presence, installed Playwright package, both media SHA values, runtime Jar/source SHA, old FAIL/HOLD outcome and Cleanup Keep paths all passed (exit 0). Focused `git diff --check` passed (exit 0). No E2E execution or cleanup was part of this validation.

# Historical rerun-20261020-r2-round4

## 2026-10-20 A-to-B branch runner preparation

Root authorized preparation of a single natural late-response branch for the original A/B objects and ViewDownload identity. The isolated path is `runs/2026-10-20T08-27-03-769Z-709e78bd/`; browser execution remains gated on Root confirming the E14 source diff is stable. The runner observes the exact A attachment 65 confirmed-download request, the native version-history transition to B, response headers/body completion, and B attachment DOM readiness with an explicit 60-second window. The verdict compares response time to the B route transition; data readiness is recorded independently. Finally persists observed timing, errors, trace, DOM/screenshot, and browser/context closure evidence. No execution release exists.

The verdict test first produced 1 RED/7 because the old rule required the response to arrive after B data was ready. After switching the criterion to the route transition, all 7 tests passed. `node --check` passed for all three runner/test files. `node .../switch-only.cjs --check-prepared` printed `WAITING_ROOT_E14_STABLE`, `browserStarted=false`, and `e2eExecuted=false`; exit 20 is the expected hold state. Playwright was not loaded, credentials were not decrypted, and no page was opened. Prior round FAIL/HOLD and original BLOCKED/FAIL evidence remain unchanged.

## 2026-10-20 Single-Branch Result

The explicitly authorized real-UI run passed. The exact confirmed request for A attachment 65 occurred at `08:39:32.563Z`; the native history action changed the route to B at `08:39:33.185Z`. The old A response arrived on B at `08:39:35.907Z` with HTTP 200 and `application/pdf`; its body finished at `08:39:35.920Z`. The B attachment DOM became ready at `08:39:36.552Z`, inside the 60-second window, with both B attachments visible and no A attachment. No response body was read and no native download event fired. The request was therefore naturally late relative to the B route, and the asserted B page state remained correct.

The run saved the full timeline, DOM, screenshot, trace archive, and context/browser closure proof under the isolated run directory. Separate trace privacy and CRC audits were NOT_RUN / NOT_VERIFIED. The final branch status is PASS, not a product defect reproduction. The prior round's BLOCKED/FAIL and overall FAIL/HOLD are not overwritten; six main PASS branches were not rerun.

The Root's follow-up specifying a six-file release list arrived after this single run had already started and passed. The consumed `execution-release.json` is preserved as-is: its runner SHA was verified before launch and it pinned the 33 changed frontend source files present in the worktree, including all six runtime-proof files and both exact E14 handoff hashes. Post-run six-file review is separately recorded in `root-six-source-review.json`; it is not represented as a pre-run release and no second browser run was made.

FAIL；六主分支6PASS，八交互7PASS/1FAIL；历史正文直达拒绝PASS，音视频NOT_RUN。A→B首次BLOCKED及同对象一次计时校准FAIL均保留，具体根因未确认，不重试掩盖。全部自有context/browser/runner已关闭，HOLD。

本轮唯一入口：rerun-20261020-r2-round4-handoff.json；详细结果：runs\2026-10-20T06-59-50-186Z-709e78bd\round4-result.json；详细报告：runs\2026-10-20T06-59-50-186Z-709e78bd\verification-report.md。旧轮结果仅历史，不计本轮通过。

---

# Verification Report

## 当前权威结论：原六分支PASS；r2只补历史地址直达拒绝

原六组业务已由Root复核为 **6PASS / 0FAIL / 0BLOCKED**，权威结果为real-e2e-result.json，连续执行证据为runs/2026-10-18T21-41-30-958Z-b9f7b31a/full-six-attachment-permission-results.json，运行包b9f7b31a。下方264efbd4时期的2FAIL/1BLOCKED、编译/审计夹具失败及设计状态均为当时原始证据，已被后续六PASS覆盖，不能作为当前业务结论。更早bb6d2c94证据保留，最新连续六PASS使用b9f7b31a。

本轮不重跑六分支，只补尚未计入的无权限账号直接打开历史地址。repair-20261019-r2-existing-pass-audit.json核对六项状态与连续run证据文件摘要，未覆盖原数据、签名、培训或附件。

本轮RED：ViewOnly正式浏览→B操作面板→历史A/1“查看详情”→viewer“查看详情”，生成未修改的真实A/1只读URL。NoAccess新独立context本人登录打开该URL，原路由守卫将from=version-history的只读态送回browser，A详情请求0、8次采样无明确拒绝；打开同一UI生成的viewer地址，真实A详情请求后出现cannot access，toast消失后空页。全过程无附件/正文/下载泄露，属于导航与错误展示，非后端权限放行。

本轮真实GREEN：两种原生历史地址均在同一NoAccess本人context以浏览器完整导航打开，`dcc-detail-read-error`明确显示“Current user cannot access this controlled file”，等待6500ms后仍可见且文本一致；每种地址均自然GET精确A4233详情，未解析响应JSON作为依据。两次均附件行0、正文canvas/text0、下载按钮0、download事件0，无A附件名/培训名泄露。结果runs/2026-10-19T10-33-14-846Z-ba0a8ead/direct-history-denial-green.json；原失败采样及超时保留，未把路由拦回浏览页冒充后端权限拒绝。

最小前端修正：remaining.ts承认既有历史只读导航；detail/index.vue沿原加载异常保留accessExplanationError，并在fileDetail未取得时显示持久error alert，原失败/新读序号与身份边界保留。未修改Query、guard、Controller、下载/目录/权限框架，不影响13/22的共享Query改动。定向测试RED9项/3失败，修复后相关5文件34项PASS；日志repair-r2-direct-denial-red.log与repair-r2-direct-denial-green.log。

运行态：本轮dispatch/reboot1019证明为8061 PID20976、48061 PID25124、Jarba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3。该旧workflow-v6不含13/22/24新后端修复，本轮只验证现有原历史拒绝与Vite加载前端改动，不把旧运行态写成这些后端修复PASS。登录无trace，按实际时间戳创建唯一trace目录；补证结果和工程收尾状态分别记录到repair-20261019-r2-handoff.json。

证据校验：本轮2份trace共809个ZIP条目完整性/已知凭据明文扫描通过（明文0、raw0）；已人工查看稳定只读拒绝截图，右上本人NoAccess身份与持久拒绝一致。定向diff空白检查通过；runner17636正常退出0且无残留。未跑Maven、构建、全量类型检查或已通过业务分支，未Git提交/推送与cleanup apply；当前业务PASS，工程未completed。

以下各节均为保留的历史过程及原始失败证据，不覆盖上面的最新六PASS与本轮附加补证结论。

## Root compiled-red后的B正控夹具修正

Root真实运行18新例：3failure/8error/7pass，原证据`../20261016-dcc-24-e2e-coordination/e2e21-23-compiled-red-DccHistoricalAttachmentReadTest.xml`保持。Root此前仅补缺失DccDownloadFileBinary import，编译失败不计产品RED；编译后H1到“缺精确A行”断言，历史metadata/preview/download仍因旧ACCESS_DENIED失败。B正控实际详情及preview通过后在下载audit失败，完整正控尚未通过。

源码及XML精确定位为`recordDownloadAccess`要求accessLogMapper.insert>0，独立测试遗漏该端口，默认0；旧DccAttachmentDownloadTest已配置insert返回1。本轮只在新类wireBusinessAndPreviewPorts加同样的一行Mapper返回值，保留真实审计逻辑、事务、全部断言及三次真实provider回调要求。没有改生产、权限、数据或断言；按SHA移除新行可精确还原Root编译后测试源码，冻结生产/主Test及原XML SHA保持。

状态FIXTURE_PORT_REPAIRED_PENDING_ROOT_RERUN，证据history-audit-fixture-repair.json；本例未运行Maven，不能预写B正控PASS或完整有效RED。Root下一步仍是原selector的B正控+H1；其余归属、第二门禁尚卡外层的失败保留，不能当安全覆盖已绿。

## Root串行交接后的测试交付

测试已写、未编译/执行：独立同包`DccHistoricalAttachmentReadTest`包含12个测试方法、参数展开计划18例，真实Query/guard/assignment/查看矩阵/类别权限/附件归属/下载策略及provider回调。当前B实际详情/预览/下载为正控；H1缺A行、精确A普通/培训metadata、两次预览门禁及独立下载为目标RED；无VIEW/仅DOWNLOAD/跨project/assignment/DAILY/主文件下载及作用域清理有负向断言。

`DccAttachmentDownloadTest`原合并断言拆为历史合法下载正例、待生效拒绝、底层存储拒绝，并补启用用户/登录/VIEW/正式master-B指针夹具，其他负例保留。独立新类不继承主Query Test，不mock被测Query/guard及资格判定；存储传输端口调用真实provider→Query授权。

Root独占Maven，当前没有服务测试RED/GREEN结论；只做源码静态核对和UTF-8/范围校验。Query、guard、主下载策略和主Query Test与交接基线SHA保持，测试文件外不改生产/运行态/数据。方法清单及建议先B正控再H1见`historical-read-test-handoff.md`，静态证据见`history-tests-written.json`。

## 2026-10-18 Root确认缺陷后的设计交付

Root明确历史查看与附件下载按各自既有资格独立校验，不依赖OBSOLETE作废操作权限；此前真实两账号缺A历史入口已确认产品缺陷，不再是待咨询的需求边界。本轮仅本例目录形成historical-read-bdd-red-design.md及同步证据状态，原3PASS/2FAIL/1BLOCKED保持。

设计覆盖：精确A历史行、选定详情、两次附件预览授权、独立附件下载及BusinessFileAccess二次门禁；无VIEW/仅DOWNLOAD/跨项目/错版本附件/撤权拒绝，DAILY作废正文及作废写操作继续拒绝。列出Query/guard具体读方法，明确不能全局删除obsolete限制或放宽主正文下载；指出当前附件UI共用file.canDownload的能力投影合同及旧测试合并拒绝OBSOLETE/待生效的冲突。

状态DESIGN_ONLY：未创建或修改Java测试、未改Query/主Test/其他生产文件、未执行Maven/E2E、未改权限或业务数据。Query及主Test由18独占，后续仅按Root串行分配进入真实RED。已做UTF-8/结构及17个实际源码方法锚点只读核验，exit0；BDD8场景、RED6组均为设计，redExecuted=false，原A/B及三账号标识保持。另核对原生只读历史路由无正文context，设计同时覆盖普通metadata读和显式历史读，不能只修带context入口；不得把设计写成测试RED或修复GREEN。

## 历史记录：264efbd4续跑结果（2FAIL/1BLOCKED已被b9f7b31a六PASS覆盖）

当时整例BLOCKED（当前3PASS、历史2FAIL/1BLOCKED），并非当前结论。原换版缺陷R1624-12当时已通过真实UI关闭；历史查阅缺陷后续已修复并由原六分支复验。该轮原结果保存在real-e2e-result-before-root-history-green.json及对应run中；real-e2e-result.json现已为较新六PASS，不得用它证明旧BLOCKED。

运行态int_qms，8061 PID24564 / 48061 PID34440，Jar264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5，与Root browser-replacement-runtime-proof一致。作者、审核人及三类阅读身份均新独立context本人真实登录；过期会话只读回查另建context，未复用他人身份。所有业务经真实页面；未API/SQL/mock/force/storage注入、子Agent、构建、服务/Git/生产修改。

R1624-12真实GREEN：原工作稿4239从版本下拉精确选择；准备申请及换版选择均回显本例Dept、批准人和文控Reviewer，原错误消失，目标B/1及training=true确认清单正确。正式提交仅一次，生成2054545668044084247，BPM a970d468-caf8-11f1-ad52-b082e25ec548。主管签名943、指派946、实际会签950、批准951（负责人Author）、文控953均VALID可见。指派后正式返回待办，重新打开同一任务核对，未重签。

B培训由原作者真实上传DCC241016C23-training-B.pdf，21:45:47；21:46:05受控/启用、21:46:09下发。B普通附件保留原上传时间18:47:28，两行原上传人均Author，首次轮次均为BPM a970d468。原A/1自动作废；作者经版本历史原生“查看详情”打开viewer，再点“查看详情”进入只读事实页，核对A普通附件16:23:02、A培训17:56:41、原BPM33daffa5及原上传人；A无B培训、B无A培训。作者本人事实核对不代替三种阅读身份验收。

| 参数 | 本轮结果 | 真实证据 |
| --- | --- | --- |
| 23-view-only-current | PASS | 新账号会话进入B/1，两行事实及文本/PDF实际正文正确，附件下载按钮0 |
| 23-view-download-current | PASS | B/1文本与培训PDF均实际预览及确认下载，保存文件并提取正文命中B标记 |
| 23-no-access-current | PASS | 本人正式浏览无匹配B/1行，监听0下载，无附件入口 |
| 23-view-only-history | FAIL（原BDD未达） | 版本表首列仅B/1、草稿，无作废A/1可进入记录；来源受控版本列的A/1不是可进入行 |
| 23-view-download-history | FAIL（原BDD未达） | 同上；历史附件下载未实际到达，不伪称已拒绝或已下载 |
| 23-no-access-history | BLOCKED | 本人无文件/历史入口，不拼URL、不拿作者页面替代历史拒绝行为 |

B PDF685×969，分布采样165942点、非白2618点，搜索excerpt命中TRAINING VERSION B；两读者文本命中ORDINARY ATTACHMENT VERSION B。B文本90B，SHAafc8c8daa9f4a99b7dde5f4ff788de0aebf4ffe43efaccaa93e5d3130fc9b524；B PDF1668B、%PDF-签名及pypdf提取正文均正确，SHA2bbe376c714b94cf2010210234fc1825d7d7d720a8dc4d802ec85589bbfabb11。文件与截图在runs/20261018-264efbd4。原A下载、取消、连续点击、离开及迟到证据原样保留，未重放。

边界证据：两读者fresh登录及B预览正常，历史表只有B/1及已替代工作稿；同一正式A/1作者可见且原记录完整。源码只读线索为QueryService.buildVersionHistory→canSeeObsolete，仅作者或类别OBSOLETE操作可见作废记录；历史下载共用ACTIVE-only策略仅为静态线索，未在历史读者UI到达，不能当实际下载拒绝。当前结论是原23历史BDD与现有权限合同不一致，需Root确认或唯一授权源头修复；不授作废写权限逼过测试。

测试脚本定位/证据错误单列：fresh浏览默认A/1，须通过原生版本下拉选择草稿，不能依赖旧context已选草稿；提交摘要只含审批阶段，不含版本号，版本身份改核对实际页眉及URL；指派成功会返回待办，须原任务重开；作者历史原生入口先打开viewer，须再点查看详情读取记录。某次fresh ViewDownload查询后空列表及一次过期context截图失败保留日志，后续本人精确fileNumber查询响应200与真实DOM回读成功；未宣称这些是被本次部署修复的产品缺陷。

最终校验：全部34份脱敏trace检查30130个ZIP条目，已知凭据明文0、raw包0，exit0。95个operation与9个独立脚本语法、4份UTF-8文档结构PASS；summarize-resumed-run.cjs实际ObservedNodeExitCode=20，按任一BLOCKED规则整例BLOCKED，计数3PASS/2FAIL/1BLOCKED。唯一runner25272正常关闭browser后exit0，精确PID已不存在。

当前未完成门禁：三个历史行为未全部通过，任务状态blocked，不进入ready_for_closeout或cleanup apply，不提交推送。A4233、原稿4239、B4247及全部证据保留，后续仅从原身份与原历史参数恢复，禁止重提、重签或重绑培训。

## 2026-10-18 19:30 当前真实验收

整例 BLOCKED：3个当前版参数PASS，3个正式历史参数受R1624-12阻塞；不将工作稿冒充B/1或历史受控版。当前权威结果为real-e2e-result.json与fixture-manifest.json；以下早期停机/准备记录仅为历史。

环境：int_qms；8061 PID24564 / 48061 PID25996；Jar bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f。所有业务和配置通过Playwright真实页面，无API/DB业务动作、响应JSON oracle、mock、强制点击或生产源码修改。

| 参数 | 结果 | 可见业务证据 |
| --- | --- | --- |
| 23-view-only-current | PASS | 本人ViewOnly查到A/1；两行附件名称/用途/原上传人/时间/BPM正确；文本正文及PDF非白画布和正文搜索匹配；下载按钮0 |
| 23-view-download-current | PASS | 本人ViewDownload同一A/1两类正文预览；确认非受控警告后各取得真实download事件，保存并验证文件名、字节、格式、A标记 |
| 23-no-access-current | PASS | 本人NoAccess正式浏览查询无目标文件、无附件入口及下载；不扩张为历史正文拒绝已验证 |
| 三个history参数 | BLOCKED | B换版属性读取丢失APPLICATION_CUSTOM，未创建B/1，原A仍ACTIVE |

A/1文件2054545668044084233，BPM33daffa5-cace-11f1-a035-b082e25ec548。主管/指派/实际会签/批准/文控签名899/904/908/910/911均真实完成；Author上传培训A后原超时没有重绑，正式新页面回读确认17:57:49受控并启用、17:57:50发布。普通附件16:23:02、培训记录17:56:41，原上传人均DCC241016C23Author；培训用途为培训记录单，首次提交轮次精确匹配BPM。

PDF画布685×969，分布采样165942点、非白2604点；界面搜索命中DCC241016C23 TRAINING VERSION A。两账号文本正文均匹配ORDINARY ATTACHMENT VERSION A。截图位于runs/20261018-bf2fd496/<actor>-<attachment>.png。

真实下载：文本90字节，SHA900590d0809cdb50137e7d51dedf47ae4ca538d7509a03bd7b6b0f0ff073b307；PDF1668字节，SHAf8baea316edde59fa1ce4a305c299466b6587e610eb5c6c9c1741f3aa7365ccf，%PDF-头及pypdf正文提取命中A标记。文件位于本轮downloads目录。

独立交互结果：取消0请求/0下载；真实dblclick后0请求/0下载（第二击使弹框遮罩关闭）；确认框开启后返回浏览并处理遗留确认，0请求/0下载。自然在途下载的离开时间1792322884071早于响应1792322886489，0下载；第二次从A/1切工作稿4239时间1792322982353早于旧A响应1792322985422，0下载，目标仅显示B普通附件，无A培训。后者仅证明不同ID工作稿切换，不能替代正式B/1历史验收。

B工作稿2054545668044084239的真实检入成功：正文B内容、原始同名源件DCC241016C23-source-A.docx、B普通附件18:47:28。误选的临时异名源件已通过页面移除，未重复检入。准备申请显示合法自定义审批，选择换版目标B/1后提示“所选工作稿原审批矩阵基线未记录，请核对正式保存来源”，审批/培训/生效方式消失，提交禁用；证据及BDD见replacement-custom-matrix-bdd.md，与协调R1624-12同根因，21为唯一生产修复者。

配置回读：类别查看矩阵精确解析ViewOnly/ViewDownload二人；项目309三名阅读用户均VIEW且启用；目录913932的ViewOnly为名称/内容true、下载false，ViewDownload三项true，NoAccess名称true/内容false/下载false。NoAccess最初保存内容true的夹具错误已在验收前通过可见switch core修正并回读，未把配置错误记产品FAIL。ViewDownload首次浏览空数据经真实重新查询已正确显示；此前等待超时不记产品FAIL。关闭弹框旧英文定位超时已改为页面中文按钮后复验通过。

执行资产：ui-session.cjs以及本轮r2-*.cjs保留真实动作与断言；主单体e2e-23-attachment-permission-real.e2e.cjs仍受完整A/B夹具门禁限制，未以其未执行分支宣称通过。汇总脚本summarize-current-run.cjs验证实际证据并以20退出表示BLOCKED。登录/签名输入关闭trace；verify-traces.cjs实际核验全部21份trace、20806个ZIP条目，已知凭据明文0、raw包0（exit0）。validate-task-assets.cjs核验66份operation及5个独立脚本语法、4份UTF-8文档结构通过。任务数据保留用于原点恢复，未执行任务完成清理，不标completed。

## 2026-10-18 本轮真实推进及安全退出

结论：BLOCKED_RUNTIME_UPDATE_WINDOW。Root要求48061安全更新后，本例已停止新增写入，全部context/browser关闭，runner PID31636（handle8300）与旧PID46860均已确认不存在。原E2E-23六个业务参数均尚未执行，不能标记PASS。

运行身份：int_qms，8061 PID24564 / 48061 PID18204；Jar a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402。下一候选Jar由Root维护，本例不重启、不修改共享dispatch。

本轮通过Playwright真实页面完成：

- 回读复用本例角色分类及原四个角色；新建Reviewer角色。为四类身份分别配置菜单/按钮权限，前三类菜单保存后重新打开逐项回读；不授管理员/全量范围。
- 创建四个业务账号与本例ProjectReviewer账号；前三类业务账号与Author独立context登录并核对昵称和菜单。ProjectReviewer仅复用正式项目审核员职责角色及本例Reviewer角色，未更改共享角色定义。
- 本例目录DCC241016C23Dir（913932）、三层专用分类分支、专用类别DCC241016C23Cat（908766）创建成功；查看矩阵页面解析到ViewOnly及ViewDownload两名实际用户，无NoAccess。下载规则只授ViewDownload；上传和培训记录上传规则只授Author，并重新打开权限页回读。
- 创建目录模板DCC241016C23Template，项目文件夹DCC241016C23Folder；新建项目DCC241016C23PROJ / 产品DCC241016C23PROD；申请46由ProjectReviewer本人页面审核，真实申请行到COMPLETED终态。产品目录651、关系44来自Author后续上传页可见事实。
- 创建DCC241016C23Dept及DCC241016C23Post，部门负责人为本例ProjectReviewer；通过用户页把Author、ProjectReviewer配置到本例部门/岗位。签名授权页已启用并回读ProjectReviewer本人授权。
- 生成带不同正文标记的A/B PDF源件、PDF培训记录、文本普通附件和任务签名图片，均位于fixtures；PDF经过文本提取及渲染检查。这些只是本地夹具，不代表已上传或版本业务通过。

当前精确checkpoint：

- Author（910487）停在文件上传页；已选择本例项目/文件夹/五级文件类型，页面仍有“确认切换文件类型”弹窗未确认。源件和普通附件file input均为空、上传列表为空；未创建controlledFile/BPM/task，恢复后从真实页面重新确认此选择。
- 页面“文件类别”显示DCC241016C23正文是源码selectedFileTypeTaxonomyLeafName的正常展示；此前期待类别维护名称是脚本断言错误，已修本例脚本，不记产品FAIL。
- ProjectReviewer（910509）签名图片上传两次未取得对应上传响应，最后会话返回登录页。图片结果为UNKNOWN，不能重传或宣称清理；恢复后本人新context登录，通过“我的签名”页面只读确认服务端事实。
- 原业务用例23-view-only-current、23-view-download-current、23-no-access-current、23-view-only-history、23-view-download-history、23-no-access-history均为BLOCKED：正式A/B版本和对应培训附件尚未完成。

证据：pause-checkpoint-20261018.json、checkpoint.json、fixture-manifest.json、execution-20261018.jsonl、runs/20261018-acceptance。4份脱敏trace共有4552个ZIP条目，已检查无已知凭据明文，raw trace为0。签名/登录输入未纳入trace；新任务密码以CurrentUser DPAPI缓存，不输出明文。

退出说明：先正常close完成browser/context清理；stdin仍保留空闲Node后，在browserClosed=true条件下向本例已知handle发送SIGINT，退出码1；没有中断业务动作，不计产品失败。随后进程检查确认无runner残留。已成功写入的本例对象保留供恢复，不删除证据；清理未完成，清单见manifest.cleanup.residual。

恢复：收到Root通知后清除本例过期runtime-pause，新context本人登录并只读回到checkpoint；复用既有账号/目录/类别/项目。完成签名事实核验、A版上传/审批/培训/生效及B版历史链路后，继续原六参数业务验收。共享roleFixtureVerified=false不作为门禁；主脚本已改为本例fixture-manifest及runtime身份校验。

## 以下为2026-10-16历史准备证据（已被最新执行授权取代）

E2E-23 附件独立权限及历史培训记录：PREPARING，尚未执行真实E2E。准备、前置、真实执行、Root review及清理状态必须分别记录。

准备结论：`PREPARED_WAITING`。脚本和子用例清单已完成；当前 Root dispatch `E2E-23.gate=prepare_only`，且角色夹具尚未放行，因此没有启动浏览器、没有登录、没有业务写入，也没有真实 E2E PASS/FAIL。

前置缺口：

- 缺少 Root 确认的三类独立真实账号及密码来源：可查看不可下载、可查看可下载、均无权限；不能以 admin 代替。
- 缺少同一租户任务自有 `DCC241016C23` 当前版/历史版数据及精确文件编号、普通附件名、培训记录名、历史版本标签。
- `dispatch.json` 当前仍为 `runtime_up_role_fixture_pending` / `prepare_only`，`runtimeVerified=true`、`roleFixtureVerified=false`；8061/48061 由 Root 统一管理，本例未启动或重启。

已完成的静态验证：

- 已确认真实页面入口、组件和 DOM 锚点：DCC 工作台受控浏览、文件行追溯、详情附件区、版本历史查看详情、培训记录区。
- 已新增/返修脚本 `e2e-23-attachment-permission-real.e2e.cjs`，覆盖六个参数化分支；任一子例 `BLOCKED` 聚合为全局 `BLOCKED`，并独立记录取消、连续点击、离开页面的零 download 事实，以及确认下载后的真实文件保存、格式签名和内容证据。
- 脚本在加载 Playwright 前读取并校验 Root dispatch、当前分支和 int_qms 端口；gate 非 execute 时写 `prepared.json` 为 `PREPARED_WAITING`，退出码 20，浏览器启动标志为 false。
- trace 在登录成功后才启动，登录输入和认证请求不会进入 trace；真实执行时当前/历史版本均按文件编号与版本标签唯一定位，在线查看需正文渲染证据，培训记录需版本/文件名/上传人/时间证据。
- 待执行的静态命令：`node --check doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs`；当前阶段不运行浏览器命令。

静态执行记录（Root review返修后重跑）：`node --check ...` exit 0；准备运行 exit 20（按 gate 约定），输出 `PREPARED_WAITING`、`browserStarted=false`、`businessActions=0`；脚本禁用模式扫描未发现 `fetch`、`axios`、`APIRequest`、`apiGet`、`apiPost`、`localStorage` 等写入/登录绕过路径；`git diff --check` exit 0。

真实执行门槛：Root 必须先确认 8061/48061 的运行态来源，更新本例 gate 为 execute、环境 status 为 ready_for_execute、runtimeVerified 与 roleFixtureVerified 为 true，并通过环境变量注入三账号、当前/历史文件编号与版本标签、附件/培训记录上传人和时间、格式及 `DCC23_*_CONTENT_MARKER`。PDF 内容校验依赖可用 `pdftotext`；其它无法可靠解析正文的格式必须 BLOCKED。真实结果须按每个 actor/version 分支写入 trace、截图与结果；任一 BLOCKED 必须使整体 BLOCKED，不得把本报告的准备状态写成 PASS。

- 本轮cleanup preview原文：BLOCKED task status is blocked; expected ready_for_closeout or completed。未运行apply，未写completed。

- 2026-10-20 R2 A→B离线调查完成/HOLD：首测trace证明B路由94141.670先于A响应97845.582，B附件98174.572就绪且原生下载0；BLOCKED来自runner把数据就绪当路由切换。一次校准5秒超时后截断，A下载与一条B详情请求在trace结束时尚无响应，不能判定产品取消/死锁。当前代码序列/卸载代次保护未见交叉取消；原样定向单测22PASS/10宿主ReferenceError保留，附件14项原样通过；仅内存补宿主approvalProgressNotice后18项PASS。未证实产品缺陷，未写生产RED或改共享前端/浮层，无新E2E、无六主分支重跑，无构建/重启/Git/DB/清理。调查报告及证据见r2-switch-investigation-handoff.json，原E2E状态不改。

# R3 第四轮 E23 验证

整体 BLOCKED；业务参数 20 PASS / 0 FAIL / 8 BLOCKED，cleanRun=false。原六权限、六交互、两自然迟到、历史正文直达与筛选均为本轮真实UI结果。WAV准入及三身份权限PASS；MP3/MP4各四参数缺合法输入BLOCKED。

复用自有A/1历史、B/1当前及音频ACTIVE A/1，仅重新读取与下载，不沿用旧PASS。预览断言含真实文本、PDF画布非白像素及关键字、WAV真实播放时间推进；下载含原生事件、文件内容及哈希。上传人、时间、BPM归属均由DOM核对。

首次12项脚本断言错误完整保留：旧testid、旧历史表布局、require作用域、双击确认框动画、历史页无未来B入口。无产品修复。针对尚未执行动作完成当前入口验证，不重跑已通过动作。A→B实际通过浏览器返回链切换；迟到响应完成于路由改变之后且0原生下载事件。

2095源文件与Jar前后门禁PASS；4份trace隐私/CRC PASS，raw trace 0；4 contexts/browser关闭，runner PID不存在。未提交申请或签名；新唯一源文件与WAV仅暂存上传，保留待授权处理。Git/数据删除/cleanup apply未执行，任务保持blocked。

正式入口：handoff.json；完整参数coverage.json；运行证据：C:\IntRuoyiAll-int_main\doc\tasks\20261016-dcc-e2e-23\runs\2026-10-20T16-04-40-732Z-709e78bd。

## 2026-10-21 验收范围更新（当前口径）

用户明确：“不需要支持MP3,MP4”。MP3、MP4共8项移出验收范围（OUT_OF_SCOPE），不记PASS，也不再作为阻塞。保留原六权限、六交互、两迟到隔离、历史正文拒绝及筛选、WAV准入与三身份权限。本轮业务结论更新为20 PASS / 0 FAIL / 0 BLOCKED；cleanRun=false，12项首次脚本错误继续保留。本次仅更新文档及汇总，不重跑E2E、不改产品。原始运行result/trace/audit记录保持原事实。Git、数据及暂存上传清理仍未授权，任务行政收尾保持blocked。
