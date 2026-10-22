# 执行日志

- 2026-10-21：核对分支为 `int_qms`；已读取 `docs/task-closeout-rules.md` 与 `docs/worktree-restrictions.md`。
- 既有证据：E13 Adjust 文件 4424 的自然培训确认 POST 于 2026-10-20 15:42:07.423 发出，约 30,012ms 后 `net::ERR_ABORTED`，无 response；原页面报 Axios `timeout of 30000ms exceeded`，对话框仍打开。之后仅对原对象只读刷新，见 ACTIVE/A1、唯一培训附件、Uploader U、Owner O 及 BPM 事实。没有再次提交。
- 分类：30 秒终止由产品 Axios 全局 30,000ms timeout 与培训确认 handler 的 await 请求链解释；ERR_ABORTED 是该等待超时的浏览器传输表现。业务已保存是单独的只读刷新证据，不能从 requestfailed 推导回滚。Playwright 后续错误/等待发生在请求终止之后，不是 POST 超时原因。
- 页面源码链：`submitApplicantTrainingRecordDialog` 单次 await `uploadControlledFileTrainingRecord`；成功才关弹框并 reload，catch 留在原弹框显示错误，finally 清除 submitting。API helper 单次 POST `/dcc/controlled-files/{id}/training-record`。静态报告显示后端该操作还同步推进 BPM 并执行 after-commit 分发/消息链，但没有栈、SQL/锁或分段耗时与请求关联，不能定位具体慢调用。
- 定向单测：新增 Axios 30 秒超时状态用例；`node --test tests/unit/dcc-detail-training-context.test.cjs` PASS，8/8。用例断言绑定 helper 调用数为1、无成功提示、弹框仍显示、submitting 已清除、timeout 错误可见。该回归用例在当前实现上直接 PASS，因此不是 RED/GREEN 修复证明；无生产代码变更。
- 范围结论交 Root review：可确定前端请求链在全局 Axios 30 秒 timeout 处终止；页面层没有在 30 秒自行关闭/导航/重试。服务端慢调用仍未定位。没有业务 UI、网络或服务器操作。
- 2026-10-21 Root 复核：原始网络证据 `doc/tasks/20261016-dcc-e2e-13/rerun-20261020-r3-round3/network-events.jsonl` 中 request 于 `2026-10-20T15:42:07.423Z` 发出，`2026-10-20T15:42:37.435Z` 以 `net::ERR_ABORTED` 结束，耗时 30011.7882ms，无 response。对应页面证据记录 `timeout of 30000ms exceeded`，确认对话框仍在。
- 现存 E13 jstack preflight 的采集时刻为 `2026-10-20T17:08:53Z`，比请求晚约 86 分钟，且 metadata 显示 businessActions=0；没有与这次请求同刻的 in-flight 栈。现存 E18 两份线程快照为 2026-10-18，不能归因到该请求。
- 对已有 runtime 日志按文件 ID `2054545668044084424`、BPM ID `056119c7-cc9c-11f1-ada5-b082e25ec548`、training-record 路径及目标时间检索，未找到匹配记录。缺少可关联的服务端日志、线程栈或分段耗时，故只能确认客户端 30 秒等待超时，不能定位服务端直接慢点。同步 BPM/after-commit 链仍是静态代码中可见的可能耗时路径，不是已证实根因。
- 本次只读复核未重跑测试、未发起 UI/网络请求，也未修改生产代码；保留前次 8/8 单测结果作为既有验证记录。
- 2026-10-21 按后续指示继续只读检查批准负责人 POST。区分 E13 R3 的 `/admin-api/dcc/controlled-files/2054545668044084424/approve-task` 与 `/training-record`：前者是带批准文件负责人的审批动作，不能用培训确认 POST 的 30 秒超时结论代替。网络证据中的四个 `/approve-task` 请求均有 200 response，耗时分别约 1.54s、1.96s、0.71s、5.56s；没有批准 POST 发生 30 秒 abort 的记录。
- 静态调用链：页面 `approval-actions.ts` 提交动作 -> API `approveControlledFileTask` -> `POST /dcc/controlled-files/{id}/approve-task` -> `DccControlledFileController.approveTask` -> `DccControlledFileWorkflowServiceImpl.approveTask` -> `approveTaskWithProcessDefinitionKey`。后者依序校验任务与阶段/负责人、评估审批准备度、读取运行任务、验证密码并生成签名、绑定所选负责人、授权签名动作、必要时绑定/持久化文控附件、读取路由快照、调用 `BpmTaskServiceImpl.approveTask`，再同步 BPM 任务（含变量和 next assignee 设置、`taskService.complete`）并同步文件状态。
- 该链是静态代码路径，不能说明历史某次请求实际进入的分支、各调用耗时或事务提交/after-commit耗时。现存网络档案只提供端到端时间与状态；jstack 与目标审批时刻不匹配，也没有 requestId/traceId 到服务端日志映射。缺失请求体/任务阶段与具体慢 span、请求时间窗内的栈、SQL/锁等待或依赖调用时间，故直接慢点仍无法定位。
- 状态更新为 BLOCKED：已满足用户要求的只读调用链梳理，但请求内关联证据不可得；不重跑 E13、不重复业务操作、不改生产逻辑、不延长超时。
- 2026-10-21 E13 验收归属复核：原合同来源为 `doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md:39`，批准时负责人核心断言共 5 项。保存的 scoped 复核 `doc/tasks/20261016-dcc-e2e-13/e2e-13-scoped-result-20261020.json`（记录时间 `2026-10-20T00:54:36.622686+08:00`）及 `scope-review-20261020.md` 将 C13-01~C13-05 全部记为 PASS，未发现核心证据缺口：默认 U/可选 O、确认值与签名持久化、无批准后负责人节点且下一任务为 D、U 可进入培训上传对话框、改 O 不转移上传职责。
- 原批准负责人断言的保存 UI 证据：`runs/20261018-resume/evidence/Retain-owner-default-U.png`、`Retain-owner-and-signature-persisted.png`、`Retain-U-training-upload-dialog.png`、`Retain-O-no-training-upload-todo.png`；`Adjust-owner-default-U.png`、`Adjust-confirmed-owner-O.png`、`Adjust-owner-O-signature-persisted.png`、`Adjust-U-training-upload-dialog.png`、`Adjust-O-after-fix-readable-owner-no-upload.png`，以及同目录 `fixture-manifest.json`、`step-file-task-signature-trace-map.json`。对应 scoped 对象为 Retain 4218/A1 owner U/signature 853、Adjust 4222/A1 owner O/signature 860；这些是批准负责人核心断言证据，不是培训 POST 成功证据。
- R3 新链路报告 `doc/tasks/20261016-dcc-e2e-13/rerun-20261020-r3-round3/verification-report.md` 的整体 FAIL 只落在 Adjust `confirmationTransport=FAIL`：`/training-record` 在 `2026-10-20T15:42:07.423Z` 发出，约 30 秒后 `net::ERR_ABORTED`、无 response；同轮 Retain 的培训确认传输 PASS。该报告同时把默认/调整负责人、签名绑定、无新增负责人节点、U/O培训职责、原 U 实际上传等业务断言列为 PASS；因此不能把训练超时归因于 `/approve-task`，也不能说负责人核心断言未通过。
- 当前验收结论：原始“批准时确认负责人”核心范围为 `SCOPED_PASS`（5/5 PASS）；R3 完整新链路整体为 FAIL，唯一已证失败是 Adjust 培训确认传输。负责人核心断言没有尚未通过项；实际培训记录上传成功/完成、正文/DAILY 属非本例或未声明范围。历史超时慢因仍 OPEN，服务端具体慢点没有请求内证据，任务保持 BLOCKED。


## 2026-10-21 单新文件诊断准备（当前范围）

in_progress — Root接受离线准备；E15独占浏览器，本轮不启动浏览器、不采样、不创建业务文件。
目标：单个全新A/1、立即生效且需要培训，仅一次training-record确认；原批准负责人5/5不重验，历史文件/未知请求不操作。
里程碑：M1 离线脚本与自检；M2 Root核验运行态并排期；M3 真实UI合法准备链产生新fileId/BPM；M4 一次提交及有限关联诊断；M5 按证据判断慢段，无复现不能记根因修复。
BDD: Given Root运行proof与实际PID/端口/Jar一致，When准备新文件，Then只沿U提交、S主管、L指派、C会签、A批准、D文控到U培训入口，不重验已通过负分支。
BDD: Given真实UI新对象身份和Root一次gate，When唯一training-record自然POST发出，Then仅在途10/20/28秒串行采样，每次最多10秒；结束取消后续及通知在途采集停止，失败停止、忙跳过、不补采。
BDD: Given旧对象、身份不符、重复POST或失效runtime，When校验，Then停止，不重试、不扩大timeout、不猜测改生产。
边界：不Maven/产品改动/服务重启/API或DB业务写入/Git；本轮只允许任务脚本与文档、离线必要自检。之后业务仍须Root单独排期。
证据：约20秒成功仅会话摘要，排除为可用证据；保留已定位3.752秒与14.246秒，历史30秒慢因OPEN。
正式复用来源：E13 runs/20261018-resume/fixture-manifest.json中的DCC241016C13R18P1/文件夹/分类/U,S,L,C,A,D；rerun-20261020-r3-round3/session.cjs调用原credential-store.cjs保护入口，禁止输出密码或改角色。
源码待证：uploadTrainingRecord行锁→票据/附件绑定→同步BPM trigger→正式化/生效→afterCommit同步分发及消息。需同窗口栈与既有日志区分，时间重叠不等于唯一请求关联。
当前验证门禁：只做新脚本语法与假时钟/假采集器自检，不重跑已通过8项产品单测；真实UI/采样未执行，不能写E2E PASS。


## 离线准备交付命令与结果

2026-10-21：observe.cjs/training-ui.cjs 的 node --check PASS；两个ps1 AST语法解析PASS；node doc/tasks/20261021-e13-err-aborted/self-check.cjs PASS（假页面/时钟/采集器：10/20/28秒上限、终态取消、在途取消信号、失败停止、串行忙跳过、拒绝历史ID、轮转日志增量）。不是产品RED/GREEN或E2E证明；未运行runtime校验器、jstack、浏览器或产品测试。

Root排期后从仓库根执行只读运行核验：
```powershell
powershell.exe -NoProfile -NonInteractive -File doc/tasks/20261021-e13-err-aborted/verify-runtime.ps1 -ProofPath doc/tasks/20261016-dcc-24-e2e-coordination/e15-isolation-runtime-proof.json
```
此proof当前预期PID32436/48061、frontend20976/8061、backend-e15-isolation-20261021.jar及SHA BB73D9417E092D43C5BC0AF08E90C43A32F2E930AEE5CF1CEAE865B00F930321。代码从proof读取，实际Win32_Process/监听/Jar校验后输出安全字段；绝不输出完整commandline。Root先核对proof本身仍为本次获准运行证据，不能用任意其他proof切换运行目标。首次身份快照固定进程启动时间，之后每次采样复核，防止PID复用。

本模块不包含新文件创建/审批/登录，也不是独立E2E runner。Root完成唯一新文件合法UI准备链后，在其已有U页面runner中接入：
```javascript
const {runTrainingOnce}=require('./doc/tasks/20261021-e13-err-aborted/training-ui.cjs')
await runTrainingOnce({page: uPage,expect,proofPath: absoluteProofPath,logDir: actualRuntimeLogDirectory,
  runDir: absoluteNewTaskPrivateRunDirectory,trainingPdf: absoluteUniqueTrainingPdf,
  gate:{gate:'execute',caseId:'E13-NEW-TRAINING',identitySource:'REAL_UI_NEW_FILE',
    freshObjectConfirmed:true,rootAuthorization: rootSingleWindowReceipt,
    newFilePrefix: uniquePrefix, fileNumber: realUiFileNumber,fileId: realUiFileId,
    processInstanceId: realUiBpmId,version:'A/1',effectiveMode:'IMMEDIATE',needTraining:true}})
```
uniquePrefix格式为DCC20261021C13NEW-加大写字母/数字/短横线唯一后缀；页面正式编号必须以前缀开始。新fileId/BPM/编号须取自本次真实页面创建回执，不能由模板编造。Root同时记录当前U/租户和准备链；module复核真实详情URL/BPM/编号和培训入口。trainingPdf须是本次唯一名的合法PDF；日志目录由当前Root运行回执提供，不猜旧路径。

输出：private/<唯一run>/runtime-identity.json、fresh-ui-gate.json、network/metadata.jsonl、stacks/stack-10s或20s或28s/collection-metadata.json及private原栈、logs/log-baseline.json与增量、result.json、terminal.png/terminal-dom.txt。Root runner负责既有脱敏trace和context/browser关闭。遇失败不再次调用模块；run目录已存在即拒绝，未知业务结果不重放。45秒只限制诊断观察，不修改Axios30秒；无响应达到上限停止诊断，保持UNKNOWN。请求终态取消计时器，并以标记通知当前自有jstack停止；不会停止后端。

当前状态仍为in_progress/等待Root运行排期；历史慢因OPEN。未把新样本成功或假采集器PASS当根因修复。


## 2026-10-21 14:40+08 有限诊断能力preflight

Root补充授权后，一次安全运行身份核验匹配PID32436/48061、frontend20976/8061及获准Jar SHA。安全JSON：private/preflight-20261021T064021251Z/runtime-identity.json。
首次采集前复核因PowerShell ConvertFrom-Json自动将ISO时间转成DateTime，字符串比较误报backendStartUtc变化；此时尚未启动jstack、未生成采样目录。仅修正为DateTimeOffset UTC ticks精确比较，随后同快照复核通过。
唯一实际jstack于2026-10-21T06:40:49.3868662Z开始、06:40:50.5216867Z结束（上海14:40:49–14:40:50），exit0、1130ms、success=true、cancelled=false。metadata：private/preflight-20261021T064021251Z/capability-stack/collection-metadata.json。原栈只留private，未输出内容。仅能力检查，不是training-record现场，无业务动作，不证明根因或E2E通过。
脚本语法及必要离线self-check再次PASS；补充重复目标POST立即停止后续采样断言。任务cjs被仓库ignore规则忽略，已写Cleanup Keep；未经授权不force-add、不提交。未Maven/产品测试/重启/浏览器/业务创建。Root可按上文import命令在其合法新对象准备链后接入；模块不负责创建文件/审批/登录或启动浏览器。


## 2026-10-21 Root最新分工（覆盖此前U/S/L创建和暂停方案）

in_progress — 离线入口已完成，待Root页面窗口。E18原stage-a单runner负责本次两个全新文件及同一GO内各一次L；其中一份为E13立即生效+必要培训的唯一文件。E13不创建文件、不操作U提交/S/L、不设置L暂停，不新增第二套准备业务路径。E18关闭浏览器、Root核对本轮L结果后交接，E13仅C/A/D→原U培训一次。旧文件及旧UNKNOWN请求不可用。

BDD: Given E18本轮真实UI交接新file/BPM/C任务且L=PASS，When Root释放E13窗口，Then复用原session/approvals完成精确C/A/D，再沿正式培训待办进入原U页面，只确认上传一次；C任务不符/任一步失败立即停止。
BDD: Given 同fileId已建立一次执行claim，When再次启动，Then在浏览器启动前拒绝；未知提交不重放。原负责人5/5、取消/缺密码负分支不重验。

### Root可直接放行的命令（仓库根）

```powershell
node doc/tasks/20261021-e13-err-aborted/run-new-file.cjs 'C:\绝对路径\E18本轮交接.json' 'ROOT本次单窗口回执' 'C:\当前运行日志目录'
```
命令名称保留run-new-file，但只承接本次新文件，不包含业务创建。入口复用原rerun-20261020-r3-round3/session.cjs登录、凭据保护、trace、角色、页面字段及approvals.cjs的openTask/signCurrent；仅在任务private生成本次独立session与config，不执行/改写历史runner目录。批准A只做一次必要有效输入并确认，无负责人参数回归。独立12分钟总界限（父进程13分钟保险），异常关闭自身browser，不重试。

E18/Root保存的交接JSON（字段取自本次真实UI，不得模板造身份）：
```json
{
  "source":"E18_CURRENT_REAL_UI",
  "assignmentResult":"PASS",
  "freshObjectConfirmed":true,
  "fileId":"本次E13新文件数字ID",
  "masterId":"真实证据值或UNKNOWN_UI_NOT_EXPOSED",
  "processInstanceId":"本次真实BPM",
  "fileNumber":"本次实际编号",
  "newFilePrefix":"本次唯一DCC开头大写字母数字短横线前缀",
  "cTaskId":"已产生的本轮C真实任务ID",
  "version":"A/1",
  "effectiveMode":"IMMEDIATE",
  "needTraining":true,
  "evidencePath":"本次E18真实UI证据的绝对路径"
}
```
项目/角色须为原E13 DCC241016C13R18资产，交接文件必须已走同一原C/A/D/U路线，不兼容其他角色/项目。原凭据保护入口缺失/登录失败直接停，不增权或改密码。必须Root先核对E18关闭浏览器，命令本身不抢占共享窗口。

### 运行证据与离线验证

private/<本轮唯一run>/result.json含接收的对象、当前C任务、签名attempt和关闭回执；source-sha256.json记录复用原脚本指纹；evidence/保存原session trace与截图。private/handoff-<fileId>.claim防止同对象再跑。training/子目录保存runtime identity/gate/network/stacks/logs/result/终态页面。trace按原脚本暂停凭据输入，未经脱敏审核不对外发布。

node --check run-new-file.cjs/new-file-sequence.cjs/training-ui.cjs及生成session的vm.Script语法PASS；离线结构断言确认无flow.prepare/U/S/L执行路径、保留精确C任务守卫和一次runTrainingOnce接入。observer假时钟/取消/串行/重复目标自检PASS。本轮未启动浏览器、创建业务文件或签名；jstack能力检查沿用14:40既有一次PASS，不重复。未复现不能作为根因修复，历史慢因OPEN。


## Root终态边界补充：传输、业务、根因分别记录

BDD: Given唯一培训提交已终结，When保存原页面截图/DOM并只读重开同一新file/BPM一次，Then仅在精确编号、ACTIVE/A1、本版唯一同名培训记录单、原U上传人及附件轮次BPM均有页面证据时，trainingBusiness=PASS；不能仅凭requestfinished/HTTP200判业务PASS。不重验负责人，不预览/下载/签收附件。
失败/超时保留原标签页弹窗和错误截图/DOM；另开同context只读标签页核验一次后关闭，不刷新或关闭原弹窗，不再次提交。观察上限未得终态时不再打开核验，业务UNKNOWN。未能满足页面断言/加载失败记录UNKNOWN及reason，不能推定保存失败或自动重提。
输出分列：trainingTransport={terminal,httpStatus,requestObserved}；trainingBusiness={status,精确文件/版本/上传人/附件/BPM事实或reason}；rootCause=OPEN。传输失败与业务已保存可以同时存在；即便两者成功，也不声称历史慢因已修复。结果先落盘UNKNOWN基线，核验结束更新；证据采集异常可使总runner BLOCKED，但原始传输结果仍保留。
新增training/business-readback-dom.txt与business-readback.png；terminal-dom.txt/terminal.png保留原页面现场。run-new-file.cjs原命令不变，gate中原U由现有固定角色账号传递。
离线：两新改脚本node --check、生成session语法PASS；既有observer离线自检PASS。真实页面终态核验尚未执行，不能将语法/observer结果当该核验已通过。


## 2026-10-21 E18不导航C的最小适配

handoff.cTaskId改为可选，不伪造。原C openTask从真实工作台按唯一编号找到待办，打开后先核对精确fileId/BPM，再提取URL中的当前taskId；编号须与交接一致且taskId非空。有cTaskId继续精确比较，无则记录页面发现值、URL、header及Single-C-discovered-exact-task.png，签名前保存cDiscovery事实。任何不符均停，不API/DB查任务、不新建文件、不重提。原一次claim保持。
日志参数必须精确为Root已核对的 output/runtime/int_qms/logs/e22-key-restore-20261021-113915-782490 且application.log存在；不扫描父目录或其他旧运行日志。Root放行命令：
```powershell
node doc/tasks/20261021-e13-err-aborted/run-new-file.cjs '<E18本轮交接JSON绝对路径>' '<Root单窗口回执>' 'C:\IntRuoyiAll-int_main\output\runtime\int_qms\logs\e22-key-restore-20261021-113915-782490'
```
脚本/生成session语法PASS，现有observer有限取消/串行/重复目标自检PASS；未启动浏览器/业务/额外采样。等待E18关闭后的Root独占排期。此前“交接必须有C任务”的描述由本节覆盖。


## 2026-10-21 15:29+08 本次真实新文件执行结果

Root唯一窗口放行后，接续E18新文件2054545668044084433（master2054545668044079324），编号DCC20261021C13NEW-1792566437587617200，BPM59d748a0-cd1e-11f1-82d6-b082e25ec548。未创建第三份文件、未重跑U提交/S/L或负责人参数。
原C真实待办发现task652ff2ac-cd1e-11f1-82d6-b082e25ec548并核对精确对象；C一次签名、A默认负责人必要批准一次、D一次签名均页面接受。A task05f62c5b-cd21-11f1-82d6-b082e25ec548，D task0afe2b0f-cd21-11f1-82d6-b082e25ec548。原U只确认培训一次。
传输：2026-10-21T07:29:01.732Z（上海15:29:01.732）发出唯一training-record POST；07:29:07.007Z HTTP200（5275.5147ms）；07:29:07.019Z requestfinished（5286.9768ms）。无30秒超时。请求在10秒前结束，所有采样计时器取消，现场jstack数量0；没有为了取栈而重复提交或改阈值。
业务事实：只读新标签页重开同file/BPM一次，ACTIVE/A1、唯一新培训记录单DCC20261021C13NEW-1792567706307Single-training.pdf、上传人DCC241016C13R18U及附件首次轮次BPM匹配，trainingBusiness=PASS。保存原terminal DOM/截图及business-readback DOM/截图；人工图审确认当前U、编号、ACTIVE/A1和本版BPM。
根因：OPEN。本次5.287秒成功仅说明新样本正常；不证明历史30秒慢因已找到或已修复。无请求内栈可用于确定慢段，禁止据此猜测改生产。
关闭：runner19652 exit0，2026-10-21T07:29:11.235Z（上海15:29:11.235）contextsBeforeBrowserClose=0、contextsAfterClose=0、browserClosed=true、issues=[]；errors=[]。唯一claim保留，无动作重放。
执行入口最初在浏览器启动/claim前因Windows PowerShell执行策略失败；改为显式使用Codex当前pwsh宿主E13_POWERSHELL_EXE，不修改系统策略，随后完成上述唯一业务执行。后续同类命令需先设置：$env:E13_POWERSHELL_EXE=(Get-Process -Id $PID).Path。这个技术启动修正不是业务失败，也不构成再次执行本对象的授权。
证据根：doc/tasks/20261021-e13-err-aborted/private/DCC20261021C13NEW-1792567706307/。
- result.json：三字段分离、C/A/D身份和关闭回执。
- training/result.json：传输HTTP200与业务PASS、rootCause OPEN。
- training/network/metadata.jsonl：唯一目标请求完整时序及采样取消。
- training/terminal-dom.txt、terminal.png、business-readback-dom.txt、business-readback.png：业务终态证据。
- training/logs/log-increments.json：complete=true，仅Root指定当前日志目录增量；原日志私有保留。
- evidence/：7份原session trace CRC全部PASS，仅私有保存，未作为脱敏可发布证明。
- private/handoff-2054545668044084433.claim：单次执行锁定。
当前定向新文件传输/业务均PASS；历史慢因OPEN、Git/工程收尾未授权，本诊断任务仍blocked，不标completed，不再申请或执行重复业务验证。
