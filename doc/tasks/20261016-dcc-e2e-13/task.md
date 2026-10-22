# E2E-13 批准时确认负责人

## 2026-10-20 第三轮完整真实 UI 重跑

- Given 当前 int_qms 8061/48061 指定运行与原真实账号，When 新建本次唯一 Retain/Adjust 申请并依次走主管、会签、批准、原上传人培训及文控至真实终态，Then 核对默认负责人U、保留U/调整O的签名绑定、不另增负责人节点、培训上传职责仍仅U；每分支独立 PASS/FAIL/BLOCKED。
- 本轮只用 Playwright 页面操作和 DOM 断言，不解析响应 JSON，不 API/DB，不修改产品、schema、公共配置、共享基线、服务或 Git，不启动子 Agent。原目录独立证据，finally 关闭 contexts/browser；新终态不能用旧 PASS 替代。
- 当前 in_progress；原账号凭据从既有 CurrentUser DPAPI 内存加载，不落明文。本次源文件和编号独立命名，全部结果留存，失败后继续其他可执行分支，不进入下一轮。
- 本次最终结果：两适用分支 PASS，均已走新申请完整链路到 ACTIVE/A1；Retain 新4319、负责人U/签名1091，Adjust 新4324、负责人O/签名1124，培训记录均由原U实际上传。18 trace / 41截图；browserClosed=true、contexts0、runner13944退出0。实现/验收交接 ready_for_closeout，原总体工程收尾仍由 Root 接管；独立本次证据 `runs/rerun-20261020-round3-103529/handoff.json`。未启动下一轮。

## 2026-10-20 正式详情等待提示：RED → GREEN

- Given ACTIVE 正式/浏览详情已按合同跳过 BPM 且 approvalLoading=false，When 渲染审批进度提示，Then 不显示“正在核验”；只有 approvalLoading=true 且无 scope 时显示等待，错误和范围提示保持优先。
- 范围：核对 reloadAll → loadApprovalDetail 与模板条件，只在既有 dcc-reader-bpm-boundary.test.cjs 补真实模板显示状态测试；不新增 helper/组件/状态，不重跑旧 77 项。
- 当前授权仅测试 RED；E24 浏览器活跃，生产 detail/view Vue 冻结，GREEN 待 Root 安全点放行。只更新原 13 记录，不建重复任务。
- RED 已交付：仅跑 dcc-reader-bpm-boundary.test.cjs，5 PASS / 1 FAIL、exit 1；真实模板在 ACTIVE 早退且 loading=false 时仍渲染等待。测试先确认实际加载等待、scope 已就绪不等待、error/notice 优先级与申请人范围提示均成立，再在正式详情断言得到业务 RED。生产 hash 保持不变，证据 `runs/20261020-formal-progress-waiting-red/`。
- 最新授权：Root 已独立接受 RED，确认 E24 maintenance-checkpoint browserClosed=true/contexts0，PID40864 不存在、session18328 exit0；放行仅第713行条件改为 approvalLoading && !approvalProgressScope。按原测试 GREEN → 同上轮7文件定向回归一次 → 两 SFC 编译；无 UI/完整构建/Maven/服务/权限变更，测试文件不再修改。
- 最新结果：ready_for_closeout，freezeReleased=true。原边界测试 GREEN 6/6、同7文件回归一次78/78、两个 SFC script/template 编译均 PASS，全部 exit 0。逐字节校验生产改动只有获准一行条件替换，view 与两测试 hash 不变。精确 diff、输出和 hash 见原 runs 下 `green-handoff.json`；Root 确认后安排24对原4275只读复验，本线程不启动 UI。总体工程收尾继续由 Root 接管。

## 2026-10-20 申请人辅助进度权限修复

- Given 无 taskId、无培训进度资格、无 `bpm:task:query` 的待审申请人，When 读取详情辅助进度，Then 不请求通用 BPM，显示读取范围提示，保留 DCC 轮次、节点签名和申请证据，不产生证据错误或推断空阶段。
- Given 实际 taskId 或培训进度资格，When 读取详情，Then 保留原任务身份核验、真实错误及培训专属读取；错轮次和迟到响应仍拒绝。
- 里程碑：单列 fixture 错误 → fixture 基线 → 业务 RED → 单 detail/index.vue GREEN → 两测试及直接相关回归一次、两个 SFC 实际编译 → ready_for_closeout 交 Root。
- 已获当轮单文件 GREEN 授权；无 Maven、UI、构建、后端、服务、数据库、Git 操作。工程最终收尾仍由 Root 接管。
- 本轮状态：ready_for_closeout；fixture 错误与业务 RED 分开留存，GREEN 定向回归 77/77（含既有签名/阅读 56 项），detail/view 两个 SFC script/template 实际编译 PASS。源码冻结释放，24 后续由 Root 放行。证据：`runs/20261020-applicant-bpm-boundary-red/`。

## Goal

独立验证E2E-13，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 仅工程收尾由Root接管、未授权Git提交推送；E2E-13原合同业务结论为scoped PASS，5/5核心断言有保存UI及Root review证据，无核心缺口。正文/下载/DAILY属附加范围，不再阻塞本例；历史失败原样保留，无主链重跑。最新结果及Root交接见e2e-13-scoped-result-20261020.json、root-handoff-scoped-pass-20261020.json。

## r2 BDD与验证门禁

- Given 两个FormalQueryGuard样本允许项目5读取且满足硬file scope；When 测试目录QUERY/PREVIEW；Then 名称发现不授正文/编辑，正文须同时满足项目/范围/目录。先记录旧夹具RED，再仅补实际缺失项目范围。
- Given H2真实File/Master/Project与实际Flowable等待执行、当前上传人U及签名负责人O；When 上传人/负责人读取培训入口或通知重放；Then 上传仍仅原U，O仅当前培训职责，角色/批准权限不得成为上传条件；硬范围、类别、tenant、账号和实时等待执行仍须校验，失效准确报错，零副作用断言保留。
- 旧测试37错误先实际复跑归档，按当前生产合同和正式测试schema维护，不以删除有效业务断言、吞异常或放宽生产换GREEN。隔离H2/Flowable测试写入属于本轮单测，不操作运行库。
- 本轮13首先串行Maven，其他线程等待；定向选择原323项以及旧Offline类。Root独占运行切包/重启，旧workflow-v6不作为新修复E2E通过。

## 2026-10-19 恢复范围与验证门禁

- 用户明确要求仅修复未闭环参数；保留4218/4222、签名853/860、U原上传dialog及Root核心验收，不重做既有通过链路。
- 本轮只核对O原入口的申请证据、关联分区与独立正文鉴权；先BDD与直接RED，后最小GREEN；Maven/构建由Root串行执行。
- Root已串行释放Query：本线程只追加assertRelationNameVisible中current native waiting + hard file scope + real canOrganizeTraining名称准入；22两投影及其他段保持。只改指定证据测试，不改QueryServiceTest或全局browse/body。
- Root已确认READY及原数据保护；本轮已执行O原入口只读Playwright诊断，仅O原入口受影响分区，无API/DB业务动作、服务重启、Git提交推送、子线程或全量作业。

## 最新验收范围（取代下述历史执行状态）

- Retain 4218/A1保留U，签名853；Adjust 4222/A1选O，签名860。默认U、取消不写、缺密码拦截、无新增负责人节点、U实际上传对话框均已有真实页面证据。
- 最新包a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402，O从原工作台进入4222，文件元数据/负责人O/签名860可读且无上传按钮，培训上传写请求0。
- 申请证据辅助区仍按历史审批权限拒绝；正文及完整审批证据权限不在本例PASS声明中。Root明确不扩修、不改前端吞错。
- 当前结果见verification-report.md、e2e-13-result.json及runs/20261018-resume/step-file-task-signature-trace-map.json。前次BLOCKED保存在history/20261018-owner-metadata-blocked；下述旧状态属于历史。
- 当前资产全部保留，cleanup attempted=false/completed=false。Root接管review及工程收尾；本线程无后续浏览动作。

## BDD

- Given 申请进入A批准，默认申请人为U
- When A打开批准弹框，分别保留U和调整为O，完成签名批准；U与O查看培训记录单上传入口
- Then 弹框负责人默认U且可调整；不另增批准后负责人选择节点；负责人为本次确认者；即使改为O，培训记录单仍由原上传人U负责上传，不能把职责自动转给O。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 两份原申请批准、签名、D审核、U上传入口及O最新metadata/无上传入口验证完成。
4. [ ] Root review脚本/trace/可见页面断言，逐分支给出实际结论。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 最新Root唯一写入范围为DccSelectedDetailReadService.java及其直接测试；有效RED前仅写测试，Maven/部署由Root串行执行。该明确分配取代下述原生产冻结中这两个文件的限制，其他生产文件继续冻结。

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C13，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## 2026-10-16 最新执行授权与实际结果

- execution-authorization.md 已明确取代 prepare_only/等待Root放行限制；当前 gate=execute、accountSetupGate=authorized，fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST。
- 不再依赖共享 roleFixtureVerified；本例 fixture-manifest.json 保存真实页面的创建、角色分配重开核验、独立登录、菜单和收尾证据。
- 当前环境已由 Root 核对：8061/48061，后端 PID24528，Jar SHA256 b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071。
- A/U/O 均为本例字母数字账号，在首次登录强制改密后分别登录；业务角色菜单已收窄，不含管理员或日志。此项只证明身份和入口，不证明签名授权、项目范围或批准业务成功。
- 创建了本例五级类型；一级编码由系统生成且只读，其名称及其余可编辑编码均带本例前缀。所有新增类型均已清理。
- 新类型叶子 87 的“检查对应类别/重新检查”持续显示“系统异常”；U 文件上传页显示“文件类别候选加载失败：系统异常”。未上传正文、未提交申请、未签名批准。
- retain-requester 与 adjust-to-other 均为 BLOCKED；业务写入集合为空。cleanupAttempted=true、cleanupCompleted=true、residual=[]，保留正常审计历史及本地验证证据。

## Parameterized Subcases

- `retain-requester`: A 在 `MATRIX_APPROVAL` 批准弹框中看到负责人默认值为 U，保留 U 后以真实密码签名批准；按正式流程完成文控等后续前置并确认页面进入培训记录阶段后，U 在真实工作台看到本例培训记录单上传入口。
- `adjust-to-other`: A 在第二份独立申请的 `MATRIX_APPROVAL` 批准弹框中看到负责人默认值为 U，将负责人切换为 O 后签名批准；负责人事实应保存为 O，但培训记录单上传入口仍只由原上传人 U 可见，O 不得因负责人选择而获得上传入口。
- 两个场景分别由 `DCC_E2E_C13_RETAIN_FILE_NUMBER` / `DCC_E2E_C13_RETAIN_VERSION` / `DCC_E2E_C13_RETAIN_TASK_ID` / `DCC_E2E_C13_RETAIN_PROCESS_INSTANCE_ID` 与对应 `ADJUST` 变量指定；脚本要求审批中心可见候选行唯一，点击后 URL 的 taskId、processInstanceId、详情文件编号和详情版本全部精确匹配。

## Exact Page Entry And Anchors

- 审批入口：真实 `/approval-center/todo`，模块筛选 `DCC`，关键词筛选 `DCC241016C13`；目标行必须可见，并点击同一行“审核”，不能调用 BPM 或 DCC API。
- 处理页：由审批中心行跳转的 `/dcc/controlled-file/detail/{id}?handling=approval&from=approval-center&processInstanceId=...&taskId=...`；必须同时看到当前任务、`data-testid="dcc-approval-upload-action-panel"`、可见“批准通过”按钮和非空任务状态，排除“当前没有待处理审批任务”。
- 批准弹框：`data-testid="dcc-task-action-readiness"`、`data-testid="dcc-approval-file-owner-picker"`、标签“文件负责人”、登录密码、审批意见、按钮“确认签名”。负责人默认值来自页面选项显示，不从响应 JSON 推断。
- 培训阶段：批准后先完成真实流程要求的后续文控节点，必须由页面确认已到“等待本次上传人上传线下培训记录”；随后真实 `/dcc/controlled-file/workbench` 的 `data-testid="dcc-workbench-offline-training-records"` 中，U 看到精确文件编号/版本行，点击后进入正式 `management=1&from=workbench&processInstanceId=...` 详情并看到“上传培训记录”按钮；O无精确行时保留无待办证据；O有行时必须进入原文件并先断言精确文件/版本元数据可读，再断言无上传按钮。通用VIEW拒绝不能证明职责守卫。
- 电子签名证据：每个场景保存批准弹框、培训阶段、U 入口和 O 无入口截图及独立 Playwright trace；详情页必须同时显示所选负责人事实和非“未记录”的负责人批准签名事实，并确认批准后没有负责人选择节点。

## Role And Data Preconditions

- A：本租户、正式启用且有 DCC `MATRIX_APPROVAL` 真实批准任务和电子签名资格；U：本例申请人、正式启用、有 `TRAINING_RECORD` 类别权限和本项目范围；O：本租户正式启用账号，可作为负责人选项但不应继承 U 的上传职责。
- 两条独立申请/审批任务必须由 Root 或前序真实页面准备，文件名/编号/申请批次均带 `DCC241016C13`，并且状态准确落在 `PENDING_MATRIX_APPROVAL`；其中一条负责人初始为 U，另一条也必须以 U 作为申请人默认负责人。
- 类别、项目、目录、审批路线、电子签名图片/策略、培训规则和用户组织范围需在同一租户存在且可由正式页面确认。缺任一角色、权限、有效路线、电子签名资格、任务自有数据或当前运行 Jar/schema 身份，均为 BLOCKED；不得用 admin 代替 A/U/O，不得猜密码或改权限基线。

## Cleanup Scope

- 只清理本例前缀的两条申请、附件/培训记录、流程任务及产生的测试文件；清理必须通过正式页面并保留结果证据。无法从页面安全定位或撤销时，记录精确残留，不用 SQL/API 清理。
- 本轮已通过 Playwright 真正删除本例 3 个用户、4 个角色（含诊断角色）、5 个类型。批准申请未创建；收尾状态见 fixture-manifest.json，不能沿用准备阶段的 cleanupAttempted=false。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-13/task.md
- doc/tasks/20261016-dcc-e2e-13/execution-log.md
- doc/tasks/20261016-dcc-e2e-13/verification-report.md
- doc/tasks/20261016-dcc-e2e-13/prepared.json
- doc/tasks/20261016-dcc-e2e-13/e2e-13-result.json
- doc/tasks/20261016-dcc-e2e-13/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-13/e2e-13-approval-owner-real.e2e.cjs
- doc/tasks/20261016-dcc-e2e-13/real-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-13/ui-actions.jsonl
- doc/tasks/20261016-dcc-e2e-13/sanitize-traces.py
- doc/tasks/20261016-dcc-e2e-13/evidence-redaction.json
- doc/tasks/20261016-dcc-e2e-13/artifact-integrity.json
- doc/tasks/20261016-dcc-e2e-13/evidence
- doc/tasks/20261016-dcc-e2e-13/runs/20261018-resume
- doc/tasks/20261016-dcc-e2e-13/history/20261016-blocked
- doc/tasks/20261016-dcc-e2e-13/resume-checkpoint.json
- doc/tasks/20261016-dcc-e2e-13/resume-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-13/resume-fixture.cjs
- doc/tasks/20261016-dcc-e2e-13/resume-project.cjs
- doc/tasks/20261016-dcc-e2e-13/resume-flows.cjs
- doc/tasks/20261016-dcc-e2e-13/resume-approvals.cjs
- doc/tasks/20261016-dcc-e2e-13/credential-store.cjs
- doc/tasks/20261016-dcc-e2e-13/finalize-resume-evidence.py

- doc/tasks/20261016-dcc-e2e-13/verify-original-O-after-fix.cjs

- doc/tasks/20261016-dcc-e2e-13/owner-metadata-fix-bdd.md

- doc/tasks/20261016-dcc-e2e-13/owner-metadata-green-handoff.json

- doc/tasks/20261016-dcc-e2e-13/history/20261018-owner-metadata-blocked

- doc/tasks/20261016-dcc-e2e-13/recovery-analysis-20261019.md

- doc/tasks/20261016-dcc-e2e-13/recovery-red-handoff-20261019.json

- doc/tasks/20261016-dcc-e2e-13/query-training-name-candidate-20261019.patch

- doc/tasks/20261016-dcc-e2e-13/diagnose-original-O-20261019.cjs

- doc/tasks/20261016-dcc-e2e-13/runs/20261019-recovery

- doc/tasks/20261016-dcc-e2e-13/recovery-green-handoff-20261019.json

- doc/tasks/20261016-dcc-e2e-13/repair-20261019-r2-handoff.json

- doc/tasks/20261016-dcc-e2e-13/run-repair-maven.py

- doc/tasks/20261016-dcc-e2e-13/runs/20261019-repair-r2

## 2026-10-19 r3 scope and BDD

- in_progress: only the two inherited public-creation fixtures; core323 PASS retained, not rerun. Maven window belongs to24 until Root explicitly releases to13. Runtime build/deploy remains Root-only.
- Given the current public upload entry requires logical-folder derived storage, exact prepared definition and matrix baseline ports, When the real Workflow creates a draft in the isolated H2 transaction, Then original default-source/actual snapshots and idempotent replay assertions execute.
- Given a real draft, Master, name claim, round and attributes have been inserted, When ticket.markBound throws the exact late failure, Then the original transaction rolls all those inserts back. Keep every original assertion and verify the failure was reached.
- RED retained verbatim from r2 final: Offline37,35PASS/1FAIL/1ERROR, actual null publicUploadPlacementService before intended paths. It is fixture RED, not product regression. Original XML copied with provenance; no historical result rewritten.
- Plan: isolate updated ports in a new helper used only by these two parent tests. Actual Workflow, real placement orchestrator, real H2 repositories/snapshot services and transaction; storage, identity/authorization, immutable definition/matrix and placement ledger are explicit isolated ports. Do not mock Workflow create success. No production edits or running DB access.
- Expected: after Root Maven authorization run only DccOfflineTrainingEntryDatabaseTest (37 cases); then wait for Root new runtime before original O910463/4222/A1 auxiliary evidence and body preview UI. No approval replay, no core323 rerun.

## 2026-10-19 r3 prepared handoff

Only two parent public-creation tests now use the isolated current-placement/preparation helper; production and other parent fixture callers unchanged. All original assertions retained. Late ticket callback additionally proves File/Master/claim/round/attributes exist before failure and verifies exact exception and ticket invocation. diff check and Python AST PASS. No Maven invoked:24 owns current window and Root must explicitly release to13. Original37 result remains35PASS/1FAIL/1ERROR, core323PASS retained. New UI not executed; new runtime required. See repair-20261019-r3-handoff.json.

## Cleanup Keep r3

- doc/tasks/20261016-dcc-e2e-13/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-13/runs/20261019-repair-r3

## 2026-10-19 r4 preparation only

Root r4 assigns only original O910463/4222 auxiliary evidence and actual body verification after explicit new runtime release. Core323 and Offline37 PASS retained; no Maven rerun or Query edit. Runtime requires prior backend name metadata fix plus current frontend TRAINING_APPLICATION context; no new env/config/SQL/role changes. Exact UI steps and evidence boundaries saved in r4-runtime-acceptance.md; structured handoff repair-20261019-r4-handoff.json. No browser started, no new-runtime PASS, no service/Git/business actions.

## Cleanup Keep r4

- doc/tasks/20261016-dcc-e2e-13/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-13/r4-runtime-acceptance.md

## 2026-10-19 r5 runtime gate recheck

- Branch int_qms confirmed. R5 dispatch and current r4 handoff read; required E2E/login/runtime/worktree rules read.
- Evidence review found only old dispatch/reboot proof from 16:30: int_qms backend PID25124, Jar SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3, old workflow-v6. No r5 new-package release proof or explicit E2E-13 release is present. Old E2E-13 `gate=execute` is prior authorization and does not satisfy r5 release gate.
- Refused to start browser against old package. No login, UI or business actions; no Maven/API/SQL/service/Git actions.
- Preserved core323/323, OfflineTraining37/37, frontend body-context14/14 results without rerun.
- Prepared `r5-runtime-acceptance.md` and `repair-20261019-r5-handoff.json`; status remains blocked pending Root package proof and explicit release. New package page PASS is false; DOM/screenshots/traces remain empty.

## Cleanup Keep r5

- doc/tasks/20261016-dcc-e2e-13/repair-20261019-r5-handoff.json
- doc/tasks/20261016-dcc-e2e-13/r5-runtime-acceptance.md


## 2026-10-19 R5 新包实际复验及 Root 窄修复准备

- Root23:13新包放行已覆盖此前旧包BLOCKED：int_qms/PID19844/8061/48061/SHA3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c。真实Playwright登录原O，从原工作台进入原4222/A1/BPM；负责人O/#910463、签名860保持，无培训上传按钮。
- 实际页面：本次申请属性与5个真实节点/签名856、857、859、860、861可读；所选版本关联出现Current user cannot access this controlled file。预览按钮0，viewer未打开，TRAINING_APPLICATION和正文内容均尚未被页面验证，完整新包E2E为FAIL。
- 命令：node --check doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs; node doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs。实际工具退出1；result.json的processExitCode=2为脚本设置值，保留原件并在process-receipt.json解释差异，未伪报通过。
- 证据：runs/20261019-repair-r5/O-1792423338598/result.json、4张截图与original-O-r5.trace.zip。trace脱敏PASS（1包/3个JSON流/ZIP验证），页面异常0、登录后写请求0、培训上传写请求0，browserClosed=true；登录不计入该写请求计数。没有临时账号授权/锁/后台任务需恢复，原任务资产继续保留。
- 静态直接根因：getRelationPermissions未复用assertRelationNameVisible的当前培训负责人名称窄准入；expectsPreviewArtifact漏掉native training waiting状态，resolvePreviewArtifactProjection提前null，canPreview及PREVIEW动作被关，前端精确培训上下文尚未执行。未将下载PUBLISHED_FILE_MISSING解释为培训源文件不存在。
- 最新Root授予相关Query/DetailGuard/Training前端及直接测试独占归属，但要求先提交根因+Given/When/Then+可执行RED并经Root确认，再执行生产修复/Maven。已先写r5-new-runtime-findings.md的BDD，再新增独立DccTrainingOwnerProjectionRegressionTest；14个准备案例只选择此类，预计两条正向投影RED，普通无权人/失效职责/硬范围/tenant及真实Guard精确TRAINING_APPLICATION文件、版本、轮次/DAILY负向保留。测试外部持久化与实时职责端口隔离，不宣称DB/Flowable集成覆盖。
- 新测试未编译/未运行；未执行Maven、生产源码修复、服务/构建、Git、API/SQL业务动作、子Agent或跨线程消息。既有323/37/14及批准主链全部保留，不重跑。状态in_progress，待Root确认新RED包；具体两处最小候选、命令和验收见r5-new-runtime-findings.md。

## Cleanup Keep r5 new runtime

- doc/tasks/20261016-dcc-e2e-13/r5-new-runtime-findings.md
- doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs
- doc/tasks/20261016-dcc-e2e-13/runs/20261019-repair-r5

## R5 Root批准两处窄修复并分配唯一Maven窗口

Root接受r5-new-runtime-findings.md根因、BDD和新增测试；立即执行单类DccTrainingOwnerProjectionRegressionTest有效RED，预期2条业务失败，编译/fixture错误不得计RED。有效RED后仅getRelationPermissions复用assertRelationNameVisible、expectsPreviewArtifact加入native waiting；同14例GREEN后释放Maven。旧323/37/14不重跑；Root负责运行包与原O页面复验。无普通发现/Guard/前端绕过、fallback或额外授权。当前分支int_qms已核对；对应规则和调用点已读。


## R5 两处窄修复完成定向RED/GREEN，Maven窗口释放

- Root当轮明确批准生产两处设计与本例唯一Maven窗口。先保留编译尝试r5-owner-projection-red：exit1/no fresh XML，只有新fixture DccProjectCodeDO builder不支持继承tenantId，改为显式setTenantId；不计业务RED。
- VALID RED：r5-owner-projection-red-valid，实际14tests/12PASS/1FAIL/1ERROR/0skip，exit1，freshXML已归档。两处均业务断点：getRelationPermissions原O权限拒绝（ServiceException），预览源artifact断言期望源文件实际null。其余12例通过。
- 生产仅改DccControlledFileQueryServiceImpl#getRelationPermissions复用既有assertRelationNameVisible；expectsPreviewArtifact纳入isNativeWaitingFile。原正式身份/placement、独立正文和编辑授权保持原计算。普通发现/Guard/前端无改动；无fallback或新授权。对本轮前镜像做精确两处源码比较PASS，补丁与前后SHA256已保存。
- GREEN：r5-owner-projection-green，同一14例全部PASS，0failures/0errors/0skip，exit0，freshXML及同案例身份校验PASS。覆盖原O/4222/A1/BPM真实Query与DetailGuard、普通无权人、失效职责、硬范围、tenant、错误轮次/文件/版本、DAILY拒绝及缺源文件准确失败；依赖端口隔离，不声称DB/Flowable集成或页面正文通过。
- 命令为python doc/tasks/20261016-dcc-e2e-13/run-repair-maven.py <上述label> DccTrainingOwnerProjectionRegressionTest 20261019-repair-r5；wrapper保存实际mvn -q -pl yudao-module-dcc -am -Dtest=DccTrainingOwnerProjectionRegressionTest -Dsurefire.failIfNoSpecifiedTests=false -Dfile.encoding=UTF-8 test、cwd、时间、退出码与mtime检查后的XML。详细命令/源码/编译class/XML哈希见runs/20261019-repair-r5/approved-fix-result.json。
- Maven窗口RELEASED，本例Maven已退出；不追加构建或全量回归。旧323/37/14与批准主链证据保留不重跑。本轮同14例已覆盖两个改动决策及权限边界，无新失败或未解决疑点需要扩大回归。
- Root统一后续包和原O真实页面复验；本轮未再跑浏览器、未重启服务、未操作运行库/账号/角色、未提交推送、未启用子Agent/跨线程消息。当前代码GREEN不替代真实正文验收。任务保持in_progress，等待Root出包/页面闭环。


## 2026-10-20 R6 新包窄UI复验准备

Root已核对两处Query源码哈希，正在r6-training-projection-package完整打包，明确尚未重启；当前只准备原O910463/4222/A1关联分区、实际TRAINING_APPLICATION正文和应拒绝日常入口。新包health+真实页面登录且Root明确放行前不开浏览器，旧R5 proof不作为本次运行身份。
BDD/步骤/日常入口与正文端点拒绝的证据区分见r6-runtime-acceptance.md；关联分区独立等加载后取DOM、正文须人工视觉确认、自然阅读审计请求如实单列，禁止用手拼URL或API补齐。当前无浏览器/登录/UI动作；源码哈希与已批准值匹配，无新增源码改动。Maven仍RELEASED，旧323/37/14及新增单类14GREEN均保留不重跑；无服务/数据库/审批/权限/Git操作。状态PREPARED_WAITING_ROOT_NEW_RUNTIME_AND_UI_RELEASE，准备就绪后交Root放行，不循环等待。

## Cleanup Keep r6

- doc/tasks/20261016-dcc-e2e-13/r6-runtime-acceptance.md
- doc/tasks/20261016-dcc-e2e-13/repair-20261020-r6-handoff.json

R6正式放行：Root r6-runtime-proof.json，PID22076/48061，Jar backend-workflow-v6-20261020-000513.jar，SHA478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a，关键class字节、healthUP、真实UI登录PASS。已核对本机监听48061=22076/8061=20976。现在仅真实Playwright原O/4222/A1关联/实际正文/日常入口边界；沿已写BDD执行，无Maven或生产修改。

## Cleanup Keep r6 execution

- doc/tasks/20261016-dcc-e2e-13/verify-original-O-r6.cjs
- doc/tasks/20261016-dcc-e2e-13/runs/20261020-repair-r6

## R6 viewer直接错误诊断

Root最新授权仅追真实页面失败请求/响应和详情读取链；允许本例脚本/证据，未批准生产修改。只读自然目标详情响应中的错误码/消息用于根因诊断，不作为业务通过依据。保持同原O/4222/A1/BPM，首个可见错误即留证返回；关联PASS保留，日常入口前轮脚本定位失败不当作拒绝通过。

## Cleanup Keep r6 diagnostic

- doc/tasks/20261016-dcc-e2e-13/diagnose-original-O-r6.cjs

## Cleanup Keep r6 findings

- doc/tasks/20261016-dcc-e2e-13/r6-viewer-findings.md

## R6继续修复授权与只读谓词核对

Root本轮明确要求持续处理：只读核对原O/4222/A1/BPM每个Guard谓词，确证产品缺陷后允许最小BDD/TDD RED/GREEN及串行Maven，不再等方案批准；运行包/服务仍Root独占。允许真实业务记录/正式只读诊断；禁止DB写/账号权限/新增日志/更换对象。保留关联PASS和323/37/14，不重跑；正文只重跑失败链，另修日常脚本的可见存储入口前置。本轮先从当前R6配置来源确认真实本地库、正式schema，再只SELECT/SHOW。

## R6日常脚本前置修正BDD

Given 同原O且默认浏览模式为项目；When 点击真实可见受控浏览/存储目录，再全域并用正式TableMultiFilter选文件编号和输入原编号查询；Then 页面完成全域过滤且无原待培训版本日常入口。只据可见DOM记UI入口不可用，不冒充DAILY端点拒绝。前轮隐藏全域定位超时作为脚本RED保留，本轮仅修改自有脚本并实际复验该不完整分支。

## Cleanup Keep r6 daily

- doc/tasks/20261016-dcc-e2e-13/verify-original-O-r6-daily.cjs

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

## Cleanup Keep scoped review

- doc/tasks/20261016-dcc-e2e-13/scope-review-20261020.md
- doc/tasks/20261016-dcc-e2e-13/e2e-13-scoped-result-20261020.json
- doc/tasks/20261016-dcc-e2e-13/root-handoff-scoped-pass-20261020.json
- doc/tasks/20261016-dcc-e2e-13/scope-review-20261020-integrity.json

## 2026-10-20 签名前阅读暂停协助交接
目标：审批真实签名前停止旧阅读，阻止focus/迟到回调重启；实现限两个Vue及既有两测试。Given/When/Then见execution-log同日记录。
RED：3个时序断言实际失败（迟到start期间签名1次、未暂停、focus重启2次）；GREEN：3/3。Root指出原ref绑定在浏览模式而非审批内嵌viewer，原56GREEN不足证明接线，不作为最终验收；既有测试新增审批预览区ref断言取得实际RED后修正。原handoff/patch及修正RED单独保存，原56GREEN工具输出保留在聊天，最终日志为修正后运行。
最终回归：node --test tests/unit/dcc-approval-dialog-context.test.cjs tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-approval-success-inbox-cache.test.cjs，56/56，exit0；两个SFC script/template编译PASS。旧夹具缺少当前详情/延迟生效依赖的ReferenceError已单独归档并补齐普通审批夹具，不算产品RED。
设计约束：复用lifecycle.pause/resume及既有关闭队列；统计失败仍按既有明确报错，不新增历史统计成功业务门禁；stop失败不提交；明确失败且任务/表单/viewer仍有效才恢复；成功或结果不明保持暂停，后者提示原记录核对。
实现里程碑：ready_for_closeout；工程状态仍blocked，由Root接管收尾，未授权Git提交推送，不标completed、不运行cleanup apply、不清理其他任务或原E2E资产。源文件编辑窗口已释放，E2E24 UI待Root放行；本线程未运行UI、Maven、构建或服务操作。
证据目录：runs/20261020-signature-reading-repair；交接handoff.json，精确本轮diff为turn-only.diff。E2E13既有SCOPED_PASS保持，不重跑。

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
