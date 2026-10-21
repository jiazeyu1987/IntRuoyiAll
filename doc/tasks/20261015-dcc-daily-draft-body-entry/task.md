# DCC 日常草稿正文入口闭环

## Goal
日常项目目录、搜索、存储目录不得开放草稿、工作小版本和待生效正文；审批任务、审批历史和版本历史保留精确身份授权。修复列表布尔权限与动作列表不一致导致申请人仍看到预览入口的问题。

## Baseline
- 实际分支 int_qms，HEAD f06833f06；相对委派基线 a3b8234ec 仅任务收尾文档变化，DCC 源码相同。
- 既存 tracked 修改仅两个 LOG_FILE_IS_UNDEFINED；大量未跟踪历史验证资产不属于本任务。保留全部并行资产。
- 既有主任务 doc/tasks/20261014-dcc-gap-daily-browser-access 已实现正文上下文；本任务补修列表动作投影，不重述其验证为本任务 PASS。

## Milestones
1. 规则与源码复核，BDD / RED。
2. 日常布尔权限与动作投影同步，GREEN。
3. 正文授权、审批/历史定向回归及静态审查，报告与收尾。

## Expected Verification
- 新增真实 Query 投影行为测试，覆盖日常草稿/工作稿/待生效/历史/ACTIVE；以既有测试回归专门编制入口。
- Maven -pl yudao-module-dcc -am 定向 JUnit：新投影测试、QueryServiceTest、BodyReadCurrentSourceTest、DetailAuthorizationGuardTest、BodyReadContextTest、BodyReadHttpContractTest。
- Node 定向正文上下文、工作稿导航测试；git diff --check；文档结构与 cleanup preview。
- 不执行 E2E、服务启停、数据库写入、提交、推送。

## Follow-up Scope — reading-session / 精确详情
- 沿用本目录。2026-10-15 续派要求先只读 review；禁止本线程 Maven/shared target/fulltypes/build，Root 唯一执行共享构建。
- Root 回执上轮7类后端115/115、10文件前端77/77，均为既有集成检查点；本次不重跑，也不将首轮344/26写成新PASS。
- 本次检查日常根行和历史版本选项动作、正文/token/Office/infra、精确详情与阅读 start/heartbeat/stop/summary 的 DAILY 一致性。
- 若需跨文件，先写 integration note 等待Root分配；保护 assertPreviewReadingAccess，不改Controller/Viewer/详情/共享测试schema。
- 本次只读发现F01后，Root交精确详情owner完成Query窄context入口与reader接线；仅DAILY复用restrictDailyBodyProjection，generic get不变、不使用根ThreadLocal遍历历史。
- Root M01正式回执：-am定向10类125/125通过，0失败/错误/跳过，R01/R02/F01软件验证已关闭；本线程只读核对入口及证据，不自行重跑或改生产。详情99、viewer82为独立前端套件，不与125或历史115/77/344/26混加。
- 统一证据目录doc/tasks/20261015-dcc-six-thread-followup-review；本任务只更新报告。运行、MySQL、E2E和Git仍未验；不存在当前待Maven或缺依赖阻塞。

## 本次继续R2范围与结果
- 按用户本轮要求独立只读检查所有日常正文入口及草稿/待生效/改派/账号停用边界，重点复核SelectedDetailReader→Query窄context路径；所有Query、Controller、SelectedDetailReadService及其他owner文件保持停写。
- 已逐项追踪列表根行/版本选项、精确详情、metadata/token、binary、Office callback、reading start/heartbeat/stop/summary至真实守卫。未发现新的具体DAILY正文放行缺口，不新增实现或测试。
- 本次只执行文档结构/源码锚点/14份源指纹核验；不启动Maven/shared target/fulltypes/build、不改依赖，不重复旧11/125套件。旧125后端/181前端/完整类型仍为上一轮证据。
- 明确改派只撤销原APPROVAL_TASK权限，独立ACTIVE项目阅读与合法历史权限继续各自鉴权；动作投影不是token或最终正文授权。
- 具体入口表和限制见verification-report.md最新R2节，14份具体文件raw/UTF8-LF指纹见daily-entry-review-r2.json。无新跨文件缺陷，不提出修复分配请求。

## 本次继续R3范围与新交接
- 继续只读检查“已打开预览后，改派/停用/状态变化/源bytes位置变化”的二次metadata/binary/Office/reading授权；旧125/181/types不作为本次新执行。
- 新发现R3：普通非Office预览的access event/viewer token只绑定受控文件ID+版本+用户+事件+purpose，不绑定metadata时解析的预览infraFileId或bytes hash。若同一版本的sourceFileId/publishedFileId在metadata后更换，binary会重新解析新指针，当前event/token仍可能匹配。Office token已有infraFileId绑定。
- 这是跨 QueryImpl、DccControlledPreviewAccessService / token合同及测试的边界；本线程不修改Query/Guard/reading owner，不运行Maven。已写integration note交Root分配；尚未宣称运行RED或修复GREEN。

## Root扩展授权：R3测试先行
- Root已接受R3并将普通预览来源绑定的QueryImpl metadata/binary/assertPreviewReadingAccess、preview access、token新合同及专属测试唯一交给本线程。reading Service/Controller、详情reader、签名审批与前端字段不在本次写入范围。
- 当前阶段仅新增调用既有公开Query API、可在当前生产源码编译的真实行为测试；生产文件保持原样，Root唯一Maven实际RED完成后另行通知GREEN。禁止本线程Maven/私有Java/fulltypes/build/实库。
- BDD R3-01 -> Given DAILY ACTIVE或精确APPROVAL_TASK版本V且预览F1合法，When 真实metadata/token签发后同版本preview指针改为F2，Then公开binary不得读取F2，须明确来源不匹配。
- BDD R3-02 -> Given同一infraFileId的metadata成功，When FileDO.configId或path改变，Then旧token不得打开新位置；分别覆盖PDF和IMAGE。
- BDD R3-03 -> Given来源不变，When用metadata原始opaque token/nonce取binary，Then正常读取F1；Given账号停用、项目范围撤销或任务改派，Then仍在任何storage bytes之前拒绝。
- RED尚未执行；本轮先交测试类/方法/fixture与来源合同边界，不能以编译缺API或未执行测试伪称RED。

## Root实际RED回执与GREEN授权
- Root正式-am已执行DccPreviewSourceBindingTest：33执行/24业务断言失败/0错误/0跳过，9成功/撤权对照通过；1865个DCC Java输入无漂移。证据由Root保管在doc/tasks/20261015-dcc-six-thread-followup-review/r2-be-red-result.json，禁止本线程改写。
- 已解除本线程R3生产冻结，开始最小GREEN：typed来源身份包含infraId/configId/path/name/type/size，独立source-bound issue/verify和preview prepare入口；正文metadata/binary/reading强制匹配，拒绝无绑定旧token，不改nonce、前端字段或schema，不读metadata bytes冒hash。
- BDD R3-04 -> Given已签发source-bound token，When来源claim被篡改、缺失或tuple任一正式字段不匹配，Then在storage之前明确拒绝；旧通用token保留其它原用途，不能降级用于新正文入口。
- BDD R3-05 -> Given同一metadata证据，WhenassertPreviewReadingAccess执行，Then真实来源不变才通过且不读bytes；换源/旧无绑定token/停用/改派仍拒绝，保留原权限守卫。
- 本线程不运行Maven/私有Java；实现与专属tests冻结后交Root唯一GREEN/相邻回归。shared fixture缺新入口stub时仅列清单给Root，不改其它owner测试断言。

## Root核心GREEN回执与四fixture迁移授权
- Root正式-am核心GREEN：DccPreviewSourceBindingTest67 + DccViewerTokenSourceBoundTest17 =84/84，0failure/error/skip，7个Java输入0漂移；证据Root r2-be-green-result.json。本线程不重复执行已稳定84。
- Root现在仅授权修改四旧fixture：DccControlledFileQueryServiceTest、DccControlledFileBodyReadCurrentSourceTest、DccW08SupervisorReadDatabaseTest、DccNativeUploadCompletionDatabaseTest。5生产+2专属tests继续冻结；不写FE、不运行Maven。
- 迁移普通正文prepareSourceBoundAccess/verifySourceBound stub/captor和必要FileDO真实事实；附件/分发/统一legacy入口保留，全部原业务断言、负向thenThrow保留，never同时覆盖新入口。无新业务变化或新RED要求。

## Root相邻回归后两DB fixture定向修复
- Root15类实际565执行/0failure/109error/0skip，Query272、CurrentSource27与其余11类通过；仅W08的32个入口fixture error、Native77个category初始化error。本线程只改这两类，已绿13类与生产/专属tests保持冻结。
- W08需补真实硬范围、round/冻结路线Mapper连接，并从真实主管任务/编制本人申请构造精确context；签发与binary/Office回调复用固定context，不猜taskId、不改ACTIVE/Guard，不降低原身份与撤权断言。
- Native仅本suite新建分类显式PUBLIC_MATRIX，不改父全局/schema/default；completion专属@BeforeEach/清理仅对子类声明方法生效，父用例不删不skip。Root唯一复跑这两类。

## Native余下9项fixture追踪
- Root仅两DB类复跑已确认W08 100/100 PASS；Native77中68PASS、3failure/6error。当前仅Native completion可写，W08/核心84/其他13类冻结。
- 已有工作稿预览补精确controlledFileId20，避免v5既有工作稿误用当前新建V2选择；真实附件service读取空普通附件清单，按正式canonical计算manifest hash，提交请求携带该事实，不让主管错误断言被附件前置覆盖。
- direct-new继承场景仍显式PUBLIC_V1/v5而正式新建预览为V2，需Root决定允许更新该继承场景的精确准备图/请求合同；本轮不伪造ready/proof、不改生产或父用例，不把未修项写PASS。

## BDD
- BDD D01 -> Given 申请人拥有 WORKING 正文编制权限且投影含 PREVIEW / CHECKIN，When 日常项目/目录/搜索返回该版本，Then canPreview=false 且 allowedActions 无 PREVIEW，CHECKIN 保留。
- BDD D02 -> Given 列表包含工作、草稿、审批中、待生效、失效及作废版本，When 日常限制投影，Then 所有非 ACTIVE 正文入口关闭，历史资料身份保留，ACTIVE 原有拒绝不得被放宽。
- BDD D03 -> Given 指定 workingFileId + workingMasterId 专门编制入口，When 申请人打开所选版本，Then 保留既有 AUTHORING 入口；最终正文由后端重新校验申请人/项目/类别/Master。此入口与日常目录/搜索分别验证，不改其既有授权合同。
- BDD D04 -> Given 日常读取草稿或待生效正文，When metadata/binary/Office/infra 访问，Then 后端拒绝且无正文读取；Given 精确合法审批任务/已办历史，When 使用其版本/轮次/任务，Then 可读对应正文且篡改任务/租户/权限即拒绝。

## 设计约束检查
- 复用既有正文上下文与授权守卫；不新增授权旁路、不改BPM与历史规则。
- 申请人身份和管理权限不赋予日常正文权限；专门编制仍须后端正式授权。
- 不隐藏资料记录、不替换成现行正文、不用前端按钮替代后端拒绝。
- 按 AGENTS / docs/backend-development.md / docs/frontend-development.md / docs/task-closeout-rules.md 及 DCC 安全、浏览设计执行。

## Current Status
in_progress Root核心84、相邻13类及W08 100已通过并冻结；仅Native77剩余fixture。本轮补真实manifest与existing draft preview输入，direct-new父场景V1/v5与当前创建V2合同仍需Root授权同步该case输入，不标关闭。只交Native待Root唯一重跑，保护12份Java无漂移。

## Cleanup Keep
- doc/tasks/20261015-dcc-daily-draft-body-entry/verification-summary.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/integration-notes.md
- doc/tasks/20261015-dcc-daily-draft-body-entry/followup-review-fingerprints.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/daily-entry-review-r2.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/daily-entry-review-r3.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-red-test-handback.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-green-handback.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-fixture-three.diff
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-fixture-three-handback.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-db-fixture-fix.diff
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-db-fixture-fix-handback.json
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-native-manifest-fixture.diff
- doc/tasks/20261015-dcc-daily-draft-body-entry/r3-native-manifest-fixture-handback.json
