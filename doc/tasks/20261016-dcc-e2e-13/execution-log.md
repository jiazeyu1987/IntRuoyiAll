# Execution Log

## 2026-10-20 第三轮完整真实 UI 重跑

- 当轮明确 E2E 放行；已读 AGENTS、E2E/login/closeout、本机运行与 worktree 端口规则及四轮计划。实际分支 int_qms；8061 PID20976、48061 PID27908 与指定 runtime-proof 一致。只操作原13独占新申请，不碰协调汇总。
- 计划两分支从本次新上传到合同终态，沿当前正式 UI 入口执行；凭据只在内存，trace 避开密码输入/签名敏感区。关闭回执及每分支证据另存本次 runs，不覆盖历史。
- 运行证明：实际 Jar SHA256 ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab，与指定 runtime-proof 一致；原7账号均本次 fresh 登录及个人资料 DOM 身份核对。官方 login-preflight 任务副本仅按用户禁止 JSON oracle 移除响应 JSON 断言、校准复制路径，保留真实登录/页面断言，exit0。
- 本次 Retain：DCC241020C13R3103529RETAIN，文件2054545668044084319，BPM474b6b07-cc2f-11f1-a8a5-b082e25ec548，批准负责人U/签名1091；Adjust：DCC241020C13R3103529ADJUST，文件2054545668044084324，BPMa8b337d7-cc30-11f1-a8a5-b082e25ec548，负责人O/签名1124。两条均UI完成新上传、主管、负责人指派、实际会签、批准、文控、原U培训上传，页面确认ACTIVE/A1、培训证据及下发时间。
- 两分支默认U、取消零审批写、空密码零审批写、无额外负责人选择节点均通过；Adjust的O真实可读本轮元数据与负责人事实且无上传控件，U仍有上传入口并完成上传。Retain的O无本次目标上传待办。没有修改共享/原独占配置或账号，无需恢复配置；本次新终态数据保留供Root复核。
- 6条旧脚本定位/就绪错误全部保留：已有登录访问/login被重定向、旧顺序过早寻找培训入口、标题多处strict匹配、要求O全列表为空、上传未完过早点确认、负责人远程选项未先检索。均按当前真实DOM最小校准后继续本条原申请，未重复提交/改预期掩盖产品失败。错误明细见本次result/handoff。
- 本次18 trace CRC与隐私脱敏检查PASS，41截图、最终DOM/动作日志齐全；session48257 exit0，runner13944实际不存在，browserClosed=true、contextsBefore/AfterClose=0、close issues=[]。最初无stdin启动38100自动退出且未执行业务，独立记在回执。8061/48061仍为20976/27908。
- 交接：`runs/rerun-20261020-round3-103529/handoff.json`，结果两分支PASS、无业务FAIL/BLOCKED；本轮无产品/DB/API/响应JSON oracle/mock/共享配置/构建/重启/Git/子Agent，未启动下一轮、未cleanup apply。总体工程收尾沿用Root接管，历史证据原样。

## 2026-10-20 正式详情等待提示：仅测试 RED

- 后续 GREEN 放行：Root 接受实际模板 RED，确认 E24 安全退出，明确授权只改第713行等待条件。复用已有测试及 SFC 编译脚本，GREEN 后只跑相同7文件回归一次；结果和精确本轮 diff 记录到本节 runs 目录。
- GREEN 完成：第713行 `!approvalProgressScope` → `approvalLoading && !approvalProgressScope`，其余生产字节原样；原测试文件未再修改。边界测试6/6、同7文件一次回归78/78、detail/view 两 SFC 的 compileScript/compileTemplate 全通过，三命令 exit 0。
- 逐字节精确替换断言通过，view 及两个测试 hash 保持不变；新的 detail SHA256 为 513F5929351365C28A3F4E9A8338DCE9C88D7E638DC79D675D4BA7407DABF5C1。`green-one-line.diff`、`green-source-hashes.json`、实际输出和 `green-handoff.json` 保存在原 `runs/20261020-formal-progress-waiting-red/`。
- 实现 ready_for_closeout，freezeReleased=true；Root 确认后放24原4275只读复验，再 DOC/CAD。本线程未启动 UI/完整构建/Maven/服务，无权限或其他生产变更、无 Git 提交。既有总体工程 blocked/Root接管结论保留。

- Root/E24 证据：`../20261016-dcc-e2e-24/run-missing-formats-1792438891279/XLSX-A2-applicant-active-scope-notice-missing.txt/png`，ACTIVE XLSX4275 等待 30 秒仍显示正在核验。
- 静态相连调用确认：reloadAll 等待 loadData 后调用 loadApprovalDetail；正式/浏览早退主动设 approvalLoading=false，实际 BPM/培训异步读取才设 true，finally 复位 false。当前模板只检查 !approvalProgressScope，因此把有意不读取显示为等待；无需新增读取或权限。
- 先记录 BDD，再仅修改原边界测试验证真实 alert 条件及状态；本轮禁止生产修改、UI、构建、服务和全量回归。
- RED 命令（cwd IntRuoyiFronted）：`node --test tests/unit/dcc-reader-bpm-boundary.test.cjs` → exit 1，5 PASS / 1 FAIL。真实 SFC 相邻三条 alert 经 compileTemplate 生成 render 并执行；ACTIVE 正式分支已零 BPM 请求、loading=false，却仍返回“正在核验”标题。无 fixture/编译错误，不是 403。
- RED 之前通过的同级断言：加载且无 scope 显示等待、已有 scope 不等待、真实 error 高于 notice、notice 高于等待、待审无权限申请人显示范围提示。正式/浏览各早退状态在同一小测试表中，当前首个 ACTIVE 断言失败，尚不声明余下状态通过。
- 建议后续 GREEN 仅将模板等待条件改为 approvalLoading && !approvalProgressScope；保持相邻 error/notice 优先级与原 DCC 分区。当前未实施，等待 Root 安全点放行。
- 仅测试文件与原 13 记录/证据变更；detail SHA256 B08B5AF971B32321E17899A867565650175DDB39E382E18833B1A8F922543954、view SHA256 8FDB3FFAD4844959FDEF90B4D2735EF5906A5CE35579CFAE3D582B2432194A9B 保持冻结。日志、turn-only.diff、handoff.json 见 `runs/20261020-formal-progress-waiting-red/`。

## 2026-10-20 申请人辅助进度权限边界

- 24 已安全关闭浏览器和 contexts，原会话 exit 0；收到单 detail/index.vue 最小 GREEN 放行。
- 先记录既有 fixture 错误，补真实读取选择 fixture，再保存定向业务 RED；随后实现独立范围提示，保留实际任务及培训合同。验证结果追加于本节对应 runs 目录。
- 原两测试首次运行 exit 1：10 PASS、2 个 `resolveDetailReadSelection` 缺失的 fixture 错误；仅补当前正式函数依赖后 12/12 PASS、exit 0。
- 加首条定向业务断言后 12 PASS / 1 FAIL、exit 1：实际私有任务读取 1 次，预期 0 次；此结果为有效业务 RED。
- GREEN 仅改 detail/index.vue：无 taskId、无培训资格、无 BPM 查询资格时停止辅助 BPM 读取并提示范围，DCC 轮次上下文保留，提示不进入 contextError；实际任务和培训专属读取不变。
- 既有两测试加直接相关读取边界、原四份签名/阅读回归一次通过 77/77、exit 0；申请证据实组件覆盖本轮/显式轮次、版本、节点及真实绑定签名，实际任务 403 和培训失败仍抛出。
- 两个 SFC 的 compileScript/compileTemplate 均 PASS、exit 0；view 源码 hash 原样。turn-only.diff、source-hashes.json 和命令/退出码 handoff.json 均在 `runs/20261020-applicant-bpm-boundary-red/`。
- 当前实现 ready_for_closeout，冻结释放；原总体工程收尾仍交 Root，24 浏览器复验由 Root 后续放行。本轮未运行 UI、Maven、构建或服务操作，未提交。

## 2026-10-18 最终核心范围PASS（取代旧BLOCKED当前结论）

- Root确认47项适用GREEN并部署a27b8d...；fresh O从原工作台进入4222/A1，负责人O #910463、签名#860、绑定BPM准确，元数据可读且无培训上传按钮/写请求。两分支PASS；U原上传对话框和原批准证据保留，未重提重签。
- 申请证据辅助区仍有历史权限拒绝，单列限制，不声明正文/完整审批证据PASS，不扩修。旧37夹具错误未修复且未计PASS；W06 port mock不算DB集成。
- browser/runner正常close、exit0；所有原业务资产保留。旧BLOCKED顶层材料先归档history/20261018-owner-metadata-blocked；当前结果/step-file-task-signature-trace/O新证据更新。
- 验证完成后按ready_for_closeout规则核对；因明确禁止Git且Root要求仅整理结果，工程状态blocked，未执行cleanup apply，不标completed。


## 2026-10-18 有效RED后最小GREEN实现（待Root执行）

- Root RED：training-owner-metadata-red，exit1，1test/0failure/1error，真实readApplication→authorizedProjection→history body guard抛training owner is not historical approver；保留该准确计数，不记作0error。
- 获得Root明确授权后，仅修改DccSelectedDetailReadService.java以及ServiceTest/HttpTest。当前原生等待培训、无task、精确当前绑定BPM且canReadProgress通过时读取已有metadata query；版本/轮次检查和签名过滤保留，正文/历史/任务仍原guard，无异常fallback。
- 已补原U、无职责、错轮次、有task、非原生/非等待、投影替换及正文独立拒绝；HTTP覆盖真实workbench仅processInstanceId路由。没有改ProgressService或前端，没有Maven/服务/数据库/Git操作。
- 生产与直接测试现在冻结，Root定向GREEN selector为DccSelectedDetailReadServiceTest,DccSelectedDetailReadHttpTest；原guard/offlineEntry/W06/daily projection复用回归范围及文件SHA见owner-metadata-green-handoff.json。

## 2026-10-18 Root唯一写入分配：先RED

- 已写BDD：owner-metadata-fix-bdd.md。当前只新增DccSelectedDetailReadServiceTest的waitingTrainingOwnerReadsBoundApplicationMetadataInsteadOfApprovalHistoryBody；Root执行Maven。
- selector：DccSelectedDetailReadServiceTest#waitingTrainingOwnerReadsBoundApplicationMetadataInsteadOfApprovalHistoryBody。真实reader当前会进入APPROVAL_HISTORY guard并抛training owner is not historical approver；没有setField到不存在的新依赖，因此不以测试装配失败冒充RED。
- 尚未运行Maven、尚未改生产；生产DccSelectedDetailReadService.java当前SHA256 A98B91CC1358EDB420735592B4FDC82F34B56302668976B5C2AF40BDD60178BC。有效RED后才按Root给出的最小metadata分支修改，并补严格负例/原正文守卫回归。

## 2026-10-18 最新收口

- Retain4218：批准任务e134ab17-ca6c-11f1-9342-b082e25ec548，签名853，保留U910457；D任务db6aaabc-ca6e-11f1-9342-b082e25ec548后，U可进入上传对话框，O无该文件待办，PASS。
- Adjust4222：批准任务f816882b-ca70-11f1-9342-b082e25ec548，签名860，选择O910463；D任务5a9ade8f-ca71-11f1-9342-b082e25ec548已过，原U可进入上传对话框。
- Root review要求排除通用VIEW拒绝。本例仅为类别DCC241016C13R18CAT新增显式USER O的VIEW，唯一主体标签为“仅用户910463查看”；正式预览只解析到O且无阻塞，不加TRAINING_RECORD/管理/全局OWNER。
- fresh O仍在原4222实际入口收到Current user cannot access this controlled file，文件头为空；查看说明已是当前查看矩阵参与人。因此Adjust为BLOCKED_O_READ_CONTEXT，未再把通用查看失败判为培训职责PASS，也未宣称O可上传。
- 当前Root运行证明：PID39740、ee59c01bc755da17b29360f6ba21c253f048c537606ce006351935ac456eda4a，targeted-activation-runtime-proof.json。原批准/U入口证据按Root指令保留，不重提不重签。
- 正常关闭browser/context及runner（exit0），保留全部原资产且不清理。61个R18 trace完成凭据脱敏及zip校验；finalize-resume-evidence.py校验通过并产出逐步file/task/signature/trace映射。历史BLOCKED顶层材料已先归档。

## 2026-10-18 恢复

- 当前分支int_qms；已读取根AGENTS、docs及e2e06-15-acceptance/thread-contract/execution-authorization/dispatch。Root当前证明：8061 PID24564，48061 PID31976，Jar SHA256 9f90ac84c858b2b0db3ba25de6a885760a6f5a4ff2798c3531389850198300cc；共享roleFixtureVerified=false不阻塞本例。
- BDD沿用原行：两份独立申请进入A批准；分别保留默认U和改为O，签名后负责人事实正确且无额外节点；实际抵达培训记录阶段后，上传职责仍是U。可采用本例APPLICATION_CUSTOM，结论不覆盖PUBLIC配置。
- 新证据目录runs/20261018-resume；历史清理记录表明未创建批准申请，但仍将通过原页面只读确认。新建资产使用DCC241016C13R18标识；已有COMPLETED项目且U有EDIT/UPLOAD时直接复用，不为owner文本重建。

- 2026-10-16：核对当前分支为 `int_qms`；读取 `docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 及公共 thread contract。
- 2026-10-16：核对唯一业务来源 `e2e-test-cases.md` 的 E2E-13 BDD；拆分 `retain-requester`、`adjust-to-other` 两个批准参数分支，并补充 A/U/O、同租户、审批路线、电子签名和培训规则前置。
- 2026-10-16：源码核对确认负责人选择组件为 `ApprovalFileOwnerPicker.vue`，仅在 `MATRIX_APPROVAL` 上传/升版批准弹框出现；处理页路由由审批中心真实 DCC 行生成；培训入口为 DCC 工作台 `dcc-workbench-offline-training-records`，上传资格按申请人 U 的正式规则计算。
- 2026-10-16：新增 `e2e-13-approval-owner-real.e2e.cjs`。脚本首先读取 Root `dispatch.json`；本例 `gate=prepare_only` 时在 `chromium.launch()` 前写入 `e2e-13-result.json` 并退出 `PREPARED_WAITING`，没有浏览器或业务写入。
- 2026-10-16：静态命令 `node --check doc/tasks/20261016-dcc-e2e-13/e2e-13-approval-owner-real.e2e.cjs` 返回退出码 0；脚本无 `fetch`、`axios`、`APIRequest`、SQL、mock 或 localStorage 登录路径。
- 2026-10-16：未运行浏览器。脚本在 `gate=execute` 前还会校验 `int_qms`、8061/48061、`runtimeVerified=true` 和 `roleFixtureVerified=true`，不满足时在 `chromium.launch()` 前 BLOCKED。
- 2026-10-16：实际运行 `node doc/tasks/20261016-dcc-e2e-13/e2e-13-approval-owner-real.e2e.cjs`，退出码 0；结果为 `PREPARED_WAITING`，`browserStarted=false`，dispatch 环境仍报告 `runtimeVerified=false`、`roleFixtureVerified=false`，未进行业务写入。
- 2026-10-16：复跑 `node --check ...` 退出码 0；`prepared.json` 结构校验通过；禁止动作扫描未发现 `fetch`、`axios`、`APIRequest`、`localStorage`、mock 或 `page.request`。
- Root review返修：移除两个场景共用前缀首行选择；每个场景改为独立文件编号/版本/taskId/processInstanceId环境变量，唯一候选、URL流程身份和详情文件身份精确匹配。
- Root review返修：批准后先刷新详情并等待“等待本次上传人上传线下培训记录”，再进入U/O fresh context；增加负责人签名事实、批准后无负责人节点、截图、trace及明确 `cleanupAttempted=false`/残留记录。
- 2026-10-16：返修后 `node --check` 退出码 0；在当前 `prepare_only` 下运行脚本退出码 0，结果仍为 `PREPARED_WAITING`、`browserStarted=false`、`cleanupAttempted=false`，无业务写入。
- 2026-10-16：静态契约校验通过：两个精确 fixture 环境变量、trace/screenshot、培训阶段等待、residual 字段均存在；禁止动作扫描通过。
- 2026-10-16：进一步绑定每个场景的 `processInstanceId`；U 分支必须从工作台点击精确行进入正式培训管理详情并看到“上传培训记录”，O 分支必须没有精确待办行。

## 最新授权后的真实执行（取代前述准备阶段状态）

- 已读取 execution-authorization.md 及当前 dispatch，gate=execute，accountSetupGate=authorized，fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST；不再等待Root批准，不修改共享dispatch或共享角色夹具标志。
- 通过真实管理员页面创建本例A/U/O及本例角色；登录和密码变更均未录trace。每个新账号在自己的真实登录页完成首次改密，使用独立context核对昵称/菜单；管理员重新打开分配角色弹框核对精确角色持久化。
- 账号分配初期遇到“普通用户不能被分配管理员或日志权限”；随后通过菜单页面关闭父子联动、清空自建角色授权，分别勾选最小业务菜单，分配及重开核验成功。没有业务账号得到管理员或日志权限。
- 创建本例五级文件类型；首级编码为系统自动生成的只读字段，名称带本例前缀；其余类型编码/名称均带本例前缀。叶子ID87，“检查对应类别/重新检查”持续报系统异常。
- U独立身份文件上传页实际显示“文件类别候选加载失败：系统异常”。未上传正文、未提交申请、未推进任何批准任务。业务写入集合为空，两个分支均为BLOCKED。
- 证据在fixture-manifest.json、ui-actions.jsonl及evidence/real-ui。账号登录/改密完成后才启动trace，未执行签名输入；敏感传输头和凭据字段已用sanitize-traces.py脱敏，三个zip校验通过，退出码0。
- 通过真实页面删除本例A/U/O、三个角色及诊断角色Z、五级类型共12个对象；列表复验无残留。cleanupAttempted=true、cleanupCompleted=true、residual=[]；审计历史保留。临时凭据文件不存在，遗留本例Node/Chromium测试进程已关闭。
- 两个.cjs脚本node --check退出码0；正式runner读取本例manifest、正确忽略共享roleFixtureVerified=false，并以业务BLOCKED退出码2结束。不能把历史账号核验或脚本可解析记为批准业务PASS。
- 任务最终工程状态blocked；不执行Git提交/推送，不执行证据cleanup apply；Root review产品阻塞及真实页面证据后安排修复复验。
- 最终校验：runner/session语法退出码均0，业务runner实际退出码2；证据完整性检查退出码0，3个脱敏trace压缩包有效，12个对象清理记录齐全，两个批准分支均BLOCKED。新增artifact-integrity.json供Root核对文件哈希。
- Root HMR暂停：当前单次动作已结束，项目审核员角色绑定停在用户筛选控件前，未执行该绑定；21个本例已准备对象保留，项目/file/task尚无，正文未上传、申请未提交。resume-checkpoint.json已保存；browserClosed=true、runnerEnded=true、runnerExitCode=0。收到Root恢复前无新页面动作。
- 后端维护暂停：browserClosed=true、runnerEnded=true、exit=0。Retain文件2054545668044084218已到培训记录阶段且U可进入上传/O无待办；Adjust文件2054545668044084222已A选O签名#860并D审核，U上传对话框可达；O工作台出现上传行，但落点空文件/拒绝查看矩阵，需恢复后独立核对入口含义，不据此声称职责转移。两份原申请/全部资产保留，不撤回、不清理、不再提交。检查点resume-checkpoint.json。
- 最后后端更新暂停：正常browserClosed=true、runnerEnded=true、exit=0。O的通用VIEW拒绝结论已依Root review撤回为待核验；DCC241016C13R18CAT的O VIEW规则仅打开维护弹框，未保存。Retain4218与Adjust4222及签名#853/#860保留，无重提、重签、清理。恢复只补O VIEW并核对实际上传守卫。

- 最终结构/路径/签名映射验证PASS；63个trace脱敏及zip完整性PASS；finalize-resume-evidence.py退出0；两份相关JS语法检查退出0；三个冻结生产/直接测试文件SHA与交接值完全一致。没有再次启动浏览器。

## 2026-10-19 定向恢复分析

- 已读取最新Root review边界及恢复分派，实际分支int_qms；本轮环境未READY，没有启动浏览器或重跑通过参数。
- 纠正辅助错误根因：原07:42:40日志落在Query.assertRelationNameVisible，申请证据已过metadata guard但关联源名称检查仅认可canUpload(U)，漏当前确认O职责；不是正文history guard本身。
- 另识别原工作台route含processInstanceId，resolveDetailBodyReadContext将无task O推成APPROVAL_HISTORY，已有TRAINING_APPLICATION职责正文路径不可达；应保留项目/硬scope/正式round独立拒绝。
- 精确方案、文件、BDD及定向测试计划见recovery-analysis-20261019.md。Query由22独占，尚未改生产或测试；Root协调后做有效RED，Maven/构建仍由Root串行执行。

## 2026-10-19 Root READY后的定向诊断与前端TDD

- O原入口诊断复现辅助申请证据/关联区错误；预览按钮0，未点击正文，不声称本轮正文实际网络拒绝。无业务写请求，browser正常关闭；早期轮次加载中观测单独保留。
- 获Root精确归属后，只改指定后端证据测试与前端正文函数/直接测试。前端有效RED14/13/1 exit1（APPROVAL_HISTORY而非TRAINING_APPLICATION），随后GREEN14/14 exit0。完整负例覆盖错round/file/owner/signature、职责结束、无PREVIEW/进度授权、有task，以及真实task/显式context优先。
- 后端真实Query/Guard/Offline/requireWaiting行为RED测试及未应用Query候选diff见recovery-red-handoff-20261019.json，待Root Maven。未改Query生产、QueryServiceTest、O权限；没有重签/重建或全量测试。

## 2026-10-19 有效后端RED后的名称准入窄修冻结

- Root有效RED：e2e13-training-evidence-name-red，1test/0failure/1error，真实Query→关联桥接名称拒绝1080000012，非编译/夹具问题。
- 22冻结释放Query后，仅追加assertRelationNameVisible的3行：native waiting、硬file scope、真实offlineTraining.canOrganizeTraining后允许本文件名称metadata；没有catch/fallback，没有改global browse/body，二进制替换断言证明其他Query字节保持。
- 指定W08证据测试复用真实Query/Guard/Offline/requireWaiting与真实版本策略，补7种非法职责、其他关联文件独立拒绝，以及6种真实等待校验/文控证据失败传播；外部端口mock，不冒充DB/BPM集成。
- 生产与指定测试已冻结，recovery-green-handoff-20261019.json记录文件SHA及Root selector：DccW08ApplicationRelationEvidenceTest,DccRelationFormalQueryGuardTest,DccControlledFileDetailAuthorizationGuardTest。本线程未跑Maven；定向GREEN和新包UI均待Root，前端已有14/14不重复运行。

## 2026-10-19 r2最新定向结果与Maven窗口释放

- 实际基线：FormalQuery3项1failure/1error；旧Offline37项37error，全由已移除roles注入阻断，原XML归档red-baseline。
- FormalQuery两个样本补真实项目可读List.of(5L)；第一次Set类型写错导致testCompile失败，原日志保留，已排除旧Surefire XML不算新执行。
- 旧Offline测试恢复原上传人99、确认负责人88、当前账号query/类别/硬范围与真实receive-task规则；删除过时roles字段注入，保留角色/approve不决定上传的正例参数、失效拒绝、无正文提升、零查询副作用、通知两职责幂等和签名人员不改断言。File/Master/Project及Flowable engine为隔离H2实际对象，bound contract是显式mock外部端口，不冒充catalog集成。
- 最终一次定向：W08 24/24，FormalQuery3/3，DetailGuard19/19，Query277/277，共323项全PASS；旧Offline37项35PASS/1failure/1error。合计360项358PASS，exit1。旧37已不再全错误；剩余两个公共创建测试继承父类configurePublicCreation缺现行publicUploadPlacementService，尚未到目标创建/回滚断言，不是培训产品失败；未删除、跳过或模拟成功。
- 两项需要Root协调共享DccWorkflowAttributesIntegrationTest父夹具归属：真实逻辑文件夹placement和prepared执行/baseline合同。当前仅指定子类维护，不为绕过失败扩生产或伪造公共上传成功。
- 最后进程已退出，repair-20261019-r2-handoff.json明确mavenWindowReleased=true；后续Maven由Root接管24串行窗口，本线程不再自启。尚未新包/页面复验，原两单签名/权限保留。

## 2026-10-19 r3 scope and BDD

- in_progress: only the two inherited public-creation fixtures; core323 PASS retained, not rerun. Maven window belongs to24 until Root explicitly releases to13. Runtime build/deploy remains Root-only.
- Given the current public upload entry requires logical-folder derived storage, exact prepared definition and matrix baseline ports, When the real Workflow creates a draft in the isolated H2 transaction, Then original default-source/actual snapshots and idempotent replay assertions execute.
- Given a real draft, Master, name claim, round and attributes have been inserted, When ticket.markBound throws the exact late failure, Then the original transaction rolls all those inserts back. Keep every original assertion and verify the failure was reached.
- RED retained verbatim from r2 final: Offline37,35PASS/1FAIL/1ERROR, actual null publicUploadPlacementService before intended paths. It is fixture RED, not product regression. Original XML copied with provenance; no historical result rewritten.
- Plan: isolate updated ports in a new helper used only by these two parent tests. Actual Workflow, real placement orchestrator, real H2 repositories/snapshot services and transaction; storage, identity/authorization, immutable definition/matrix and placement ledger are explicit isolated ports. Do not mock Workflow create success. No production edits or running DB access.
- Expected: after Root Maven authorization run only DccOfflineTrainingEntryDatabaseTest (37 cases); then wait for Root new runtime before original O910463/4222/A1 auxiliary evidence and body preview UI. No approval replay, no core323 rerun.

## 2026-10-19 r3 prepared handoff

Only two parent public-creation tests now use the isolated current-placement/preparation helper; production and other parent fixture callers unchanged. All original assertions retained. Late ticket callback additionally proves File/Master/claim/round/attributes exist before failure and verifies exact exception and ticket invocation. diff check and Python AST PASS. No Maven invoked:24 owns current window and Root must explicitly release to13. Original37 result remains35PASS/1FAIL/1ERROR, core323PASS retained. New UI not executed; new runtime required. See repair-20261019-r3-handoff.json.

Root explicitly released Maven window from24 to13. Branch int_qms confirmed; started r3-offline-pass1 selector DccOfflineTrainingEntryDatabaseTest only. Shared services untouched.

Actual r3-offline-pass1:37tests35PASS/1FAIL/1ERROR exit1. Both placement failures resolved; now create reaches insertControlledFile and request.needTraining null unboxing. Fix only explicit attributeCreationRequest.needTraining=false, preserving production and original assertions. Run narrowed inherited two public paths first, then full37 once corrected.

GREEN: r3-public-paths-pass2 -Dtest=DccOfflineTrainingEntryDatabaseTest#h02PublicFileCreation* -> actual2tests0failure0error0skip exit0. Exact late exception and pre-failure five-table row assertions reached. Started r3-offline-final full37; core323 not rerun.

## 2026-10-19 r3 actual GREEN and window release

- Root explicitly authorized exclusive Maven after24 release. r3-offline-pass1 actual37:35PASS/1FAIL/1ERROR exit1; placement now reached real creation but explicit needTraining missing. Added only needTraining=false to the two-test request.
- GREEN: r3-public-paths-pass2, DccOfflineTrainingEntryDatabaseTest#h02PublicFileCreation*, actual2/2PASS exit0. Real Workflow/public placement orchestrator/H2 transaction; all original success/default-source/actual/replay and rollback assertions retained. Late ticket answer proves two File and two Master rows plus one claim/round/attributes row before exact failure, then rollback assertions pass.
- REGRESSION: r3-offline-final, DccOfflineTrainingEntryDatabaseTest, actual37/37PASS,0failures/0errors/0skipped, exit0. XML and process/log preserved in runs/20261019-repair-r3; no unrelated323 rerun.
- Maven window RELEASED, no active Maven from this task. No further Maven without Root authorization. No shared runtime restart, production code edit, running DB/API business action, Git commit/push or other-task asset change.
- Remaining: Root new package confirmation, then original O910463/4222/A1/current BPM auxiliary information and actual body preview only. Original approval main chain remains accepted; new UI has NOT run. Status remains in_progress for pending UI, not completed.

## 2026-10-19 r4 preparation only

Root r4 assigns only original O910463/4222 auxiliary evidence and actual body verification after explicit new runtime release. Core323 and Offline37 PASS retained; no Maven rerun or Query edit. Runtime requires prior backend name metadata fix plus current frontend TRAINING_APPLICATION context; no new env/config/SQL/role changes. Exact UI steps and evidence boundaries saved in r4-runtime-acceptance.md; structured handoff repair-20261019-r4-handoff.json. No browser started, no new-runtime PASS, no service/Git/business actions.

## 2026-10-19 r5 runtime gate recheck

- Branch int_qms confirmed. R5 dispatch and current r4 handoff read; required E2E/login/runtime/worktree rules read.
- Evidence review found only old dispatch/reboot proof from 16:30: int_qms backend PID25124, Jar SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3, old workflow-v6. No r5 new-package release proof or explicit E2E-13 release is present. Old E2E-13 `gate=execute` is prior authorization and does not satisfy r5 release gate.
- Refused to start browser against old package. No login, UI or business actions; no Maven/API/SQL/service/Git actions.
- Preserved core323/323, OfflineTraining37/37, frontend body-context14/14 results without rerun.
- Prepared `r5-runtime-acceptance.md` and `repair-20261019-r5-handoff.json`; status remains blocked pending Root package proof and explicit release. New package page PASS is false; DOM/screenshots/traces remain empty.

## 2026-10-19 r5 new runtime RELEASED by Root

Root explicitly releases only original O910463/4222 A1 auxiliary and real body UI. New proof r5-runtime-proof.json:23:13 PID19844/48061,Jar230954,SHA3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c,healthUP,realUIlogin,59class verification,newFixesIncluded=true. This supersedes earlier old-dispatch runtime blocker; do not use old proof to block.
BDD: Given original signed O and original waiting file/BPM, When O opens exact workbench row then reads application evidence and clicks actual preview, Then auxiliary metadata matches original4222/A1/BPM/sign860, no training upload authority, actual body uses TRAINING_APPLICATION exact same file/version/round and visible original PDF page. Preserve all original approval assets and test PASS.
Only task-owned standalone Playwright evidence script; no production/Query changes, no Maven/API/SQL. Login tracing disabled; post-login trace redacted before handoff. Actual preview audit/reading requests recorded separately from business workflow writes.


## 2026-10-19 R5 新包实际复验及 Root 窄修复准备

- Root23:13新包放行已覆盖此前旧包BLOCKED：int_qms/PID19844/8061/48061/SHA3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c。真实Playwright登录原O，从原工作台进入原4222/A1/BPM；负责人O/#910463、签名860保持，无培训上传按钮。
- 实际页面：本次申请属性与5个真实节点/签名856、857、859、860、861可读；所选版本关联出现Current user cannot access this controlled file。预览按钮0，viewer未打开，TRAINING_APPLICATION和正文内容均尚未被页面验证，完整新包E2E为FAIL。
- 命令：node --check doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs; node doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs。实际工具退出1；result.json的processExitCode=2为脚本设置值，保留原件并在process-receipt.json解释差异，未伪报通过。
- 证据：runs/20261019-repair-r5/O-1792423338598/result.json、4张截图与original-O-r5.trace.zip。trace脱敏PASS（1包/3个JSON流/ZIP验证），页面异常0、登录后写请求0、培训上传写请求0，browserClosed=true；登录不计入该写请求计数。没有临时账号授权/锁/后台任务需恢复，原任务资产继续保留。
- 静态直接根因：getRelationPermissions未复用assertRelationNameVisible的当前培训负责人名称窄准入；expectsPreviewArtifact漏掉native training waiting状态，resolvePreviewArtifactProjection提前null，canPreview及PREVIEW动作被关，前端精确培训上下文尚未执行。未将下载PUBLISHED_FILE_MISSING解释为培训源文件不存在。
- 最新Root授予相关Query/DetailGuard/Training前端及直接测试独占归属，但要求先提交根因+Given/When/Then+可执行RED并经Root确认，再执行生产修复/Maven。已先写r5-new-runtime-findings.md的BDD，再新增独立DccTrainingOwnerProjectionRegressionTest；14个准备案例只选择此类，预计两条正向投影RED，普通无权人/失效职责/硬范围/tenant及真实Guard精确TRAINING_APPLICATION文件、版本、轮次/DAILY负向保留。测试外部持久化与实时职责端口隔离，不宣称DB/Flowable集成覆盖。
- 新测试未编译/未运行；未执行Maven、生产源码修复、服务/构建、Git、API/SQL业务动作、子Agent或跨线程消息。既有323/37/14及批准主链全部保留，不重跑。状态in_progress，待Root确认新RED包；具体两处最小候选、命令和验收见r5-new-runtime-findings.md。

R5准备结构检查PASS：JSON解析、证据文件引用、trace ZIP完整性、task当前状态、14个准备案例结构及git diff --check。Java编译/RED未运行。verify-original-O-r5.cjs受.gitignore:103忽略，已进入Cleanup Keep；仅未来获准提交时显式add，不在本轮stage。分析基线HEAD f06833f06c868dc3a451eb63d01381a467ecfe92，源码归一化指纹见本run/source-analysis-fingerprints.json。

## R5 Root批准两处窄修复并分配唯一Maven窗口

Root接受r5-new-runtime-findings.md根因、BDD和新增测试；立即执行单类DccTrainingOwnerProjectionRegressionTest有效RED，预期2条业务失败，编译/fixture错误不得计RED。有效RED后仅getRelationPermissions复用assertRelationNameVisible、expectsPreviewArtifact加入native waiting；同14例GREEN后释放Maven。旧323/37/14不重跑；Root负责运行包与原O页面复验。无普通发现/Guard/前端绕过、fallback或额外授权。当前分支int_qms已核对；对应规则和调用点已读。

R5 initial r5-owner-projection-red: actual exit1, no fresh XML, testCompile fails solely on new fixture DccProjectCodeDOBuilder.tenantId(long) (inherited field unsupported by builder). NOT BUSINESS RED. Changed only fixture to project.setTenantId(1L); production unchanged. Re-run same class under fresh label r5-owner-projection-red-valid.

VALID RED: r5-owner-projection-red-valid ->14tests,12PASS/1FAIL/1ERROR/0skip,exit1. Exact original-owner relation call denied at Query1259 (business ServiceException, not fixture error); exact approved-source artifact assertion expected source but actual null. All other12 including real TRAINING_APPLICATION Guard and unauthorized cases PASS. Fresh XML copied by mtime-checked runner, SHA saved valid-business-red.json. Production still unchanged at RED receipt. Proceed only approved two Query edits.


## R5 两处窄修复完成定向RED/GREEN，Maven窗口释放

- Root当轮明确批准生产两处设计与本例唯一Maven窗口。先保留编译尝试r5-owner-projection-red：exit1/no fresh XML，只有新fixture DccProjectCodeDO builder不支持继承tenantId，改为显式setTenantId；不计业务RED。
- VALID RED：r5-owner-projection-red-valid，实际14tests/12PASS/1FAIL/1ERROR/0skip，exit1，freshXML已归档。两处均业务断点：getRelationPermissions原O权限拒绝（ServiceException），预览源artifact断言期望源文件实际null。其余12例通过。
- 生产仅改DccControlledFileQueryServiceImpl#getRelationPermissions复用既有assertRelationNameVisible；expectsPreviewArtifact纳入isNativeWaitingFile。原正式身份/placement、独立正文和编辑授权保持原计算。普通发现/Guard/前端无改动；无fallback或新授权。对本轮前镜像做精确两处源码比较PASS，补丁与前后SHA256已保存。
- GREEN：r5-owner-projection-green，同一14例全部PASS，0failures/0errors/0skip，exit0，freshXML及同案例身份校验PASS。覆盖原O/4222/A1/BPM真实Query与DetailGuard、普通无权人、失效职责、硬范围、tenant、错误轮次/文件/版本、DAILY拒绝及缺源文件准确失败；依赖端口隔离，不声称DB/Flowable集成或页面正文通过。
- 命令为python doc/tasks/20261016-dcc-e2e-13/run-repair-maven.py <上述label> DccTrainingOwnerProjectionRegressionTest 20261019-repair-r5；wrapper保存实际mvn -q -pl yudao-module-dcc -am -Dtest=DccTrainingOwnerProjectionRegressionTest -Dsurefire.failIfNoSpecifiedTests=false -Dfile.encoding=UTF-8 test、cwd、时间、退出码与mtime检查后的XML。详细命令/源码/编译class/XML哈希见runs/20261019-repair-r5/approved-fix-result.json。
- Maven窗口RELEASED，本例Maven已退出；不追加构建或全量回归。旧323/37/14与批准主链证据保留不重跑。本轮同14例已覆盖两个改动决策及权限边界，无新失败或未解决疑点需要扩大回归。
- Root统一后续包和原O真实页面复验；本轮未再跑浏览器、未重启服务、未操作运行库/账号/角色、未提交推送、未启用子Agent/跨线程消息。当前代码GREEN不替代真实正文验收。任务保持in_progress，等待Root出包/页面闭环。

R5最终交接检查PASS：git diff --check exit0（仅Git LF/CRLF提示）；JSON/当前状态/fresh XML与同14案例身份/源码及编译class SHA256/引用文件完整性均PASS。任务runner全部退出，进程检查未见Maven Launcher。Maven窗口RELEASED；后续运行态及完整任务收尾由Root接管，未把代码GREEN记为页面正文PASS或任务completed。


## 2026-10-20 R6 新包窄UI复验准备

Root已核对两处Query源码哈希，正在r6-training-projection-package完整打包，明确尚未重启；当前只准备原O910463/4222/A1关联分区、实际TRAINING_APPLICATION正文和应拒绝日常入口。新包health+真实页面登录且Root明确放行前不开浏览器，旧R5 proof不作为本次运行身份。
BDD/步骤/日常入口与正文端点拒绝的证据区分见r6-runtime-acceptance.md；关联分区独立等加载后取DOM、正文须人工视觉确认、自然阅读审计请求如实单列，禁止用手拼URL或API补齐。当前无浏览器/登录/UI动作；源码哈希与已批准值匹配，无新增源码改动。Maven仍RELEASED，旧323/37/14及新增单类14GREEN均保留不重跑；无服务/数据库/审批/权限/Git操作。状态PREPARED_WAITING_ROOT_NEW_RUNTIME_AND_UI_RELEASE，准备就绪后交Root放行，不循环等待。

R6正式放行：Root r6-runtime-proof.json，PID22076/48061，Jar backend-workflow-v6-20261020-000513.jar，SHA478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a，关键class字节、healthUP、真实UI登录PASS。已核对本机监听48061=22076/8061=20976。现在仅真实Playwright原O/4222/A1关联/实际正文/日常入口边界；沿已写BDD执行，无Maven或生产修改。

## R6实际复验及精确失败诊断

Root已放行R6 PID22076/48061/SHA478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a。真实原O910463/4222/A1关联分区PASS，原BPM/负责人/签名860保持、上传按钮0。预览自然TRAINING_APPLICATION上下文正确，但viewer详情GET返回HTTP200/业务1080000012，页面明确Current user cannot access this controlled file；身份0/canvas0/正文预览请求0。服务日志确认DccControlledFileDetailAuthorizationGuard.bodyDenied:196；仅首帧，内部具体谓词尚未证明。不是仅定位错误，binary/转换未到达。日常入口因默认项目模式下隐藏全域导致脚本前置未完成，不当作拒绝PASS或业务FAIL。

实际运行verify-original-O-r6.cjs和diagnose-original-O-r6.cjs，语法检查exit0，两次实际工具exit1，result内exitCode2原样保留并有process-receipt.json。证据在runs/20261020-repair-r6；两包trace脱敏/ZIP验证PASS，两个browserClosed=true，页面异常0、登录后写0、培训上传写0。最新授权只读取自然失败响应的code/msg用于诊断，不作为PASS依据。没有API调用/DB/生产/Maven/服务/Git/权限/审批动作。

精确请求/DOM/服务错误证据：runs/20261020-repair-r6/viewer-failure-proof.json；最小修复门禁、尚缺内部谓词及BDD/RED设计：r6-viewer-findings.md。结构化交接repair-20261020-r6-handoff.json已更新。历史GREEN全部保留不重跑；当前in_progress，未声称正文PASS或任务完成。

R6交接结构验证PASS：18个证据哈希、2个脱敏trace ZIP、自然上下文与原对象精确一致、JSON/引用、两次关闭与零登录后业务写、脚本语法及git diff --check。诊断脚本受既有.gitignore忽略，已列Cleanup Keep；本轮无stage/提交。artifact-integrity.json包含4个分析源码LF指纹。未定位到Guard内部唯一谓词，不扩大诊断或改生产，交Root核对。

## R6继续修复授权与只读谓词核对

Root本轮明确要求持续处理：只读核对原O/4222/A1/BPM每个Guard谓词，确证产品缺陷后允许最小BDD/TDD RED/GREEN及串行Maven，不再等方案批准；运行包/服务仍Root独占。允许真实业务记录/正式只读诊断；禁止DB写/账号权限/新增日志/更换对象。保留关联PASS和323/37/14，不重跑；正文只重跑失败链，另修日常脚本的可见存储入口前置。本轮先从当前R6配置来源确认真实本地库、正式schema，再只SELECT/SHOW。

## 2026-10-20 R6 最终只读定位与日常入口交接

blocked — 原O缺少现行正文合同要求的项目302阅读资格，属测试前置/数据缺口；关联PASS保留，正文metadata拒绝且binary未到达。日常可见存储目录/全域/文件编号查询已实际exit0，结论仅入口不可用，不是DAILY端点拒绝。无权限/Guard/生产修改，Maven已释放；按监督提醒结束诊断。

- 项目302全部规则仅id145/USER910457/OWNER，原O910463无匹配项目阅读规则；requireReadableProject确为不满足的正文前置。原对象、A1、BPM与签名860保留，未改权限、不放宽Guard、不新增生产RED/GREEN。
- 只读真实库SELECT/SHOW证据及谓词边界见runs/20261020-repair-r6/predicate-readonly-summary.json和r6-viewer-findings.md；后续完整bound resolver/binary未验收，停止扩展诊断。
- 日常脚本先保留daily-O-1792427733934自然query matcher错误exit1；只改matcher后daily-O-1792427825375实际exit0。真实“存储目录→全域→文件编号/包含→原编号→查询”，目标行0、空态可见且人工视觉确认。只认UI入口不可用，不认DAILY端点拒绝或独立状态门禁通过。
- 两轮新增trace脱敏/ZIP检查PASS，浏览器均关闭、页面异常0、登录后写请求0、培训上传写请求0（登录单列）；无直接API/DB写/权限/生产/Maven/构建服务/Git/子Agent/跨线程消息。旧323/37/14及新增14GREEN不重跑。Maven RELEASED，本轮未获取。
- 按最新监督只在E2E-13目录维护证据，到此结束。正文前置仍缺，不标ready_for_closeout/completed，不执行cleanup apply，不删除原业务资产；Root接收前置缺口交接。

## 2026-10-20 原E2E-13范围复核：scoped PASS（取代附加范围对本例的阻塞）

最新Root指令要求回到原始用例，只验批准弹框默认U/可改O、确认负责人及签名、无额外负责人节点、培训上传职责仍归U。已对照原合同第39行、两份原单保存UI manifest/逐步映射/截图/trace和既有e2e13-root-review.json：5项核心断言全部PASS，无核心证据缺口。新结果e2e-13-scoped-result-20261020.json和root-handoff-scoped-pass-20261020.json；详细矩阵scope-review-20261020.md。

正文/附件下载与DAILY不是原断言，按最新授权移出E2E-13完成门禁；R6项目302正文阅读前置缺口与DAILY仅UI入口不可用结论保持，不声称正文/DAILY端点通过。原报告、旧handoff、R5/R6失败/RED/GREEN、trace与SQL证据均原样保留；本次只新增范围结论，原e2e-13-result.json未覆盖。旧323/37/14及新增14保留，不再执行已通过主链或扩诊断。本轮无浏览器/Maven/API/DB/生产/权限/服务/Git/子Agent/跨线程消息，Maven RELEASED、browserClosed=true。工程收尾由Root接管，不宣称cleanup/提交推送/completed。

## 2026-10-20 签名前阅读收敛定向修复（Root当轮授权）
状态：in_progress；E2E13既有SCOPED_PASS保持，当前仅协助E2E24前端。
Given 阅读start尚未返回，When 点击真实签名，Then 等start及stop完成后才提交签名。
Given 签名前暂停且summary尚未完成，When focus或正文就绪回调到达，Then 不再启动阅读且等待已发summary收敛。
Given 明确签名失败或stop失败，When 处理结果，Then 前者仅当前任务/表单仍有效才恢复，后者明确报错且不得提交。
里程碑：既有测试RED → 两Vue最小GREEN → 定向回归 → ready_for_closeout。约束：不改后端、axios、readingSession；不新增统计成功业务门禁，不启动UI/Maven/构建。

## 2026-10-20 签名前阅读暂停协助交接
目标：审批真实签名前停止旧阅读，阻止focus/迟到回调重启；实现限两个Vue及既有两测试。Given/When/Then见execution-log同日记录。
RED：3个时序断言实际失败（迟到start期间签名1次、未暂停、focus重启2次）；GREEN：3/3。Root指出原ref绑定在浏览模式而非审批内嵌viewer，原56GREEN不足证明接线，不作为最终验收；既有测试新增审批预览区ref断言取得实际RED后修正。原handoff/patch及修正RED单独保存，原56GREEN工具输出保留在聊天，最终日志为修正后运行。
最终回归：node --test tests/unit/dcc-approval-dialog-context.test.cjs tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-approval-success-inbox-cache.test.cjs，56/56，exit0；两个SFC script/template编译PASS。旧夹具缺少当前详情/延迟生效依赖的ReferenceError已单独归档并补齐普通审批夹具，不算产品RED。
设计约束：复用lifecycle.pause/resume及既有关闭队列；统计失败仍按既有明确报错，不新增历史统计成功业务门禁；stop失败不提交；明确失败且任务/表单/viewer仍有效才恢复；成功或结果不明保持暂停，后者提示原记录核对。
实现里程碑：ready_for_closeout；工程状态仍blocked，由Root接管收尾，未授权Git提交推送，不标completed、不运行cleanup apply、不清理其他任务或原E2E资产。源文件编辑窗口已释放，E2E24 UI待Root放行；本线程未运行UI、Maven、构建或服务操作。
证据目录：runs/20261020-signature-reading-repair；交接handoff.json，精确本轮diff为turn-only.diff。E2E13既有SCOPED_PASS保持，不重跑。

## rerun-20261020-r2-round3 EXECUTE
已获本轮完整UI重跑授权；独立runs/rerun-20261020-r2-round3，runtime门禁通过，旧PASS不计。无产品/Git/DB/服务变更。

## 2026-10-20 rerun-20261020-r2-round3

# E2E-13 rerun-20261020-r2-round3
Status: ready_for_closeout; HOLD. Original engineering closeout remains Root-owned; Git prohibited this round.
Contract: two applicable new UI branches PASS; cleanRun=false, transport observations and script/navigation calibration failures retained for Root review.
- Retain: PASS; fileId=2054545668044084366; BPM=e951ed39-cc4b-11f1-ada5-b082e25ec548; owner=DCC241016C13R18U; signature=1191; ACTIVE/A1.
- Adjust: PASS; fileId=2054545668044084370; BPM=dc6a7151-cc4c-11f1-ada5-b082e25ec548; owner=DCC241016C13R18O; signature=1218; ACTIVE/A1.
All business actions: real Playwright pages using original seven task accounts. No API/DB/mock actions or response JSON oracle. Runtime matches execute gate.
First failure: Retain single training submission showed 30000ms timeout; no resubmit. Readonly reload verified ACTIVE/A1 and exact U training attachment, owner and BPM. Adjust project load timeout happened before upload/submission; one readonly reload recovered candidates. Original first screenshots/DOM and failed commands preserved.
Evidence: handoff.json, result.json, ui-actions.jsonl, final DOM, 17 sanitized CRC-checked traces, 77 screenshots, artifact-integrity.json. browserClosed=true, contexts=0, runner 3220 exited 0.
No product/build/Maven/restart/Git/DB/subagent/job action. New task files retained; no shared fixture changed. No fourth round.
