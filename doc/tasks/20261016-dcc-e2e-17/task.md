# E2E-17 全员下发与逐人签收

## Goal

独立验证E2E-17，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 当前证据交接已核验：R3原16项均映射4369/A1同分支有效PASS，4418只读ACTIVE/通知事实另存；独立未覆盖业务动作0，不再为填满4418重签。调度延迟5296秒另交Root/E15，不归因新包；四trace SHA/原CRC隐私审查与关闭事实齐全。无浏览器/签名授权；工程Git/cleanup未执行。最新交接remaining-16-evidence-handoff-20261021.md/json。

blocked — 2026-10-21单次只读核对完成：原4418/A1已ACTIVE且三人待签、各有精确下发待办；16项共同生效前置已解除，未验业务动作仍未执行。停在阅读/签收动作前等待Root归属放行，浏览器/context0及runner26148退出0，详细证据readonly-4418-20261021-handoff.md；不把原例4369既有PASS撤销。Git/cleanup无当轮授权，不作工程completed。

blocked — R3第三轮新4418/A1本轮9 PASS/0 FAIL/16 BLOCKED，T后163秒仍待生效；5629只是定向handler筛选结果，正式批处理5625覆盖与未激活原因未定。未操作共享调度；后续分支未执行具体阻塞。27 trace/2904帧/22截图证据PASS，browser/context0及runner28148退出0；正式交接rerun-20261020-r3-round3/handoff.json。禁止Git及apply，HOLD。

blocked — 2026-10-20第三轮重跑已结束并释放浏览器；新文件2054545668044084321/A1，6 PASS、0 FAIL、10 BLOCKED。已完成本次完整审批及原编制人未来时间签名，未受控/待生效三人无通知共6分支通过；计划10:45:28，10:46:59及10:49:05正式页仍待生效、未下发。正式UI仅见暂停且目标为旧E16的Root共享任务5628，无本例独占任务；按Root监督不操作它，生效后全部分支具体BLOCKED。29份trace CRC/隐私通过、27截图、browserClosed=true/context0，runner29252退出0。独立结果见rerun-20261020-round3-handoff.json；历史PASS不替代本轮。未改产品/共享配置、未构建重启/Git/子Agent/cleanup apply，总体协调文件未写。

ready_for_closeout — 用户2026-10-20确认每人仅归属一个部门，原同人多部门成功前提移出本版本范围（不记为执行PASS）。按修订后范围，现存真实UI三人逐签及待签3→2→1→0证据、R4五项Maven GREEN均通过，E2E-17业务验收PASS；未重跑。范围决定与证据依据见verification-report.md及decision-20261020-handoff.json。历史D17-01待决记录保留为范围变更前状态。

## BDD

- 本次文档复核：Given Root明确单人单部门且现有合同及证据可追溯；When 移除同人双归属成功前提；Then 当前范围沿用既有25项结果、移除项不计PASS/N/A，所有历史失败及未验证事实保留，不推断实际组织归属。

- Given 下发部门包含R1/R2/R3，每人仅归属一个部门（2026-10-20用户确认；不构造同人多部门场景）
- When 分别在未受控、受控未生效和受控已生效时查看收件人页面；生效后各人登录签收
- Then 仅受控且生效后下发；所选部门全部人员均能收到，非所选范围不应因默认会签混入；每个接收人分别电子签名，一人签收不能代替其他人；页面可追踪未签收人员。

### Root review 补充 Given/When/Then

- Given 独立账号凭据仅由环境变量注入，trace必须保留可复核操作及截图。
- When 每个context先登录后启动trace，签名输入前停止trace；签名提交、密码框清空后重新开始；对导出ZIP逐条解压脱敏并校验。
- Then 密码、认证网络响应/头、非图片正文资源不进入交付trace；保留截图和动作。每人通过文控正式页面记录真实文件编号/版本、签名人/时间和仍未签收人员；浏览器关闭与业务实体清理分别记录。

### 本轮调度前置阻塞 BDD（交Root，未改生产）

- Given 精确job5626暂停，旧参数指向其他用例，Root租约仅允许先单改参数并回读，再启用。
- When 配置admin真实UI保持其他字段不变，保存1:2054545668044084234，再取消表单并自然搜索/重开读取。
- Then 当前实际仍为旧参数和暂停状态；JobServiceImpl.updateJob仅允许NORMAL状态，阻断本次顺序。禁止启用旧目标绕过；由Root解决源头或明确调整安全顺序后再验本例自然执行。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后真实业务执行及browser/runner收尾；受控文件和签名保留供Root review。
4. [x] 单部门适用范围复核完成；同人跨部门前提按Root委派移出，不冒充已执行PASS。历史未验事实保留。

## Expected Verification

- 当前仅文件证据对账：原16个BLOCKED逐项引用4369同键PASS和真实截图，分离4418只读事实/旧运行计数。验证映射16/16、四trace SHA及原CRC/隐私报告、runner关闭；不以新fixture完整度新增业务门禁，不重复签收。E15占唯一调度UI窗口，本线程无浏览器授权。

- 2026-10-21 当前唯一放行：一次Playwright只读核对原R3 fixture 4418/A1状态、版本、下发名册和待办；用原U及原接收人，停在签收动作前。runtime proof为e15-isolation-runtime-proof.json，backend32436/48061、frontend20976/8061，SHA BB73D9417E092D43C5BC0AF08E90C43A32F2E930AEE5CF1CEAE865B00F930321。保存截图/脱敏trace/实际关闭回执，不改历史PASS、不触发job、不新造数据、不补签。

- 最新R3第三轮取代旧只复核/不重跑授权：新唯一4418/A1完整合同及附加25分支，本轮9 PASS/16 BLOCKED；等待生效前置缺口不复用旧PASS。完成关闭后停止，不发线程消息。
- 本次仅文档结构/原E2E-17合同逐项覆盖/保护证据SHA核对；不运行UI、Maven、API或DB，不改生产、夹具或组织归属；范围复核无业务阻塞，工程禁Git及未apply事实保持。
- 当前最新授权范围（rerun-20261020-r2-round3）：新独立文件4369/A1，原合同16项+T前正文3项+逐人重复/权限6项，共25项真实Playwright UI验收；已全部PASS。58份trace解压CRC/隐私、53截图和浏览器/进程关闭证据已核验。只读cleanup preview已完成，禁止apply及Git；历史授权和旧阻塞仅保留审计，不作为本轮未完成业务。第四轮HOLD。
- 2026-10-20仅核对R5交接、IC32定案、最新原例、正式人员/名册来源及保护证据；文档结构和SHA验证。仅当已批准规则及既有真实数据均证明可合法构造才允许进入未验分支UI；本轮条件不成立，故未执行。业务需互斥选择撤回前提并明确是否N/A，或定义正式多归属来源及按人/按部门义务。历史PASS不重跑，异常拒绝/幂等不替代原正例。
- R5仅处理未闭环D17-01：A为业务确认本版本同人跨部门成功前提不适用，异常重复拒绝/重放幂等另行验收；B为定义正式多归属来源及按人/按部门签收义务。未收到业务定案，保持DECISION_REQUIRED/NOT_CONSTRUCTIBLE，不自动N/A/PASS。R5不重跑真实UI、三人签收、5项Maven或异常拒绝/幂等测试；继承真实UI主链PASS及R4 5/0/0/0 GREEN，R2 3PASS/2ERROR仅保留为维修前RED。完整案例仍blocked。
- 2026-10-19 R2最新范围取代上一轮“仅分析”：原49分40秒延迟审计、既有IC32规范冲突判定、2个旧fixture维护；BDD及Root实际5项RED见repair-20261019-r2-bdd.md，测试改好交Root串行5项GREEN。除受影响测试外，已通过E2E/参数不重复运行；没有成立的调度产品RED就不改生产或新建样本，当前不操作5627。Maven13先占，本例仅准备命令，不在共用target并发执行。
- 2026-10-19当前唯一范围：按recovery-dispatch-20261019.md只读核对原跨部门前提、单deptId输入/API/Mapper、直接成员名册及重复/重放规则；交Root判定和可执行验收选项。已通过分支不重跑，原文件/签名/manifest不改；本轮不等待环境READY启动UI。以下2026-10-18门禁仅保留为历史，不重新执行作业/策略/签名流程。
- 本轮Root仅分配水印问题只读定位及BDD/最小RED设计；生产文件须后续唯一分配。交付preview-watermark-investigation.md和preview-watermark-source-evidence.json；不改头校验/VIEW/审计、不操作5627、不重签。审计策略缺行由Root查正式YAML/真实库并精确处理。
- 本轮最新Root授权取代前述“不新增job”及先改暂停job顺序：保存5626原UI/日志，正式UI仅删除已暂停5626并确认原行消失；正式UI创建一次Root自有DCC241016Root定向生效，handler仍dccControlledFileTargetedActivationJob，param1:2054545668044084234、cron0 * * * * ?、重试次数/间隔/监控0。回读新ID和状态，等自然分钟触发；首见4234 ACTIVE立即UI暂停并回读，交还Root后继续三人逐签。提交结果不确定先查不重建；不改Infra源码、5625、原文件签名或生效时间。
- 本轮Root独占租约授权：配置admin通过真实基础设施→定时任务操作既有5626。先核原ID/handler/暂停/旧参数，单改参数为1:2054545668044084234并重开回读；启用后仅等自然cron，执行日志核精确参数和返回1。首次确认本例ACTIVE立即UI暂停并回读归还，不改5625、不执行一次、不新建作业、不改其他字段。实际激活延迟如实记录。
- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 按Root本轮补充：R1/R2/R3的时序负例须同时保留原有查看规则、签名图片及签名授权；生效前后不修改阅读授权。六个空通知页面先记DOM断言通过，待生效后三人以同一授权实际打开精确下发版本，才补足资格连续性证明；不得以缺VIEW造成不可见判负例通过。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C17，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-17/remaining-16-evidence-handoff-20261021.md
- doc/tasks/20261016-dcc-e2e-17/remaining-16-evidence-handoff-20261021.json

- doc/tasks/20261016-dcc-e2e-17/readonly-4418-20261021.cjs
- doc/tasks/20261016-dcc-e2e-17/readonly-4418-20261021-handoff.md
- doc/tasks/20261016-dcc-e2e-17/artifacts/readonly-4418-20261021/result.json

- doc/tasks/20261016-dcc-e2e-17/evidence-reconciliation-20261021.json
- doc/tasks/20261016-dcc-e2e-17/evidence-reconciliation-20261021.md
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/scheduler-scope-correction.json
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-session.cjs
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-verify.cjs
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/handoff.json
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/task.md
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/execution-log.md
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/verification-report.md
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/preflight.json
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r3-round3/cleanup-preview.txt

- doc/tasks/20261016-dcc-e2e-17/scope-review-20261020.json
- doc/tasks/20261016-dcc-e2e-17/scope-review-cleanup-preview-20261020.txt
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r2-round3-handoff.json
- doc/tasks/20261016-dcc-e2e-17/artifacts/rerun-20261020-r2-round3/steps/recipient-helpers.cjs
- doc/tasks/20261016-dcc-e2e-17/artifacts/rerun-20261020-r2-round3/steps/repeat-and-permissions.cjs
- doc/tasks/20261016-dcc-e2e-17/artifacts/rerun-20261020-r2-round3/evidence-verification.json
- doc/tasks/20261016-dcc-e2e-17/artifacts/rerun-20261020-r2-round3/cleanup-preview.txt

- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r2-session.cjs
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r2-verify.cjs
- doc/tasks/20261016-dcc-e2e-17/rerun-20261020-r2-round3-activation-handoff.json

- doc/tasks/20261016-dcc-e2e-17/task.md
- doc/tasks/20261016-dcc-e2e-17/execution-log.md
- doc/tasks/20261016-dcc-e2e-17/verification-report.md
- doc/tasks/20261016-dcc-e2e-17/prepared.json
- doc/tasks/20261016-dcc-e2e-17/e2e-17-full-distribution-signoff.cjs
- doc/tasks/20261016-dcc-e2e-17/artifacts/prepared-or-run.json
- doc/tasks/20261016-dcc-e2e-17/artifacts/trace-safety-manifest.json
- doc/tasks/20261016-dcc-e2e-17/sanitize_trace.py
- doc/tasks/20261016-dcc-e2e-17/verify_trace_safety.py
- doc/tasks/20261016-dcc-e2e-17/artifacts/trace-safety-contract-check.json
- doc/tasks/20261016-dcc-e2e-17/root-directory-diagnosis.cjs
- doc/tasks/20261016-dcc-e2e-17/root-directory-diagnosis.json
- doc/tasks/20261016-dcc-e2e-17/ui-session.cjs
- doc/tasks/20261016-dcc-e2e-17/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-17/live-session.cjs
- doc/tasks/20261016-dcc-e2e-17/vault.ps1
- doc/tasks/20261016-dcc-e2e-17/sanitize-own-trace.cjs
- doc/tasks/20261016-dcc-e2e-17/checkpoint.json
- doc/tasks/20261016-dcc-e2e-17/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-17/business-issues.json
- doc/tasks/20261016-dcc-e2e-17/acceptance-progress-20261018.json
- doc/tasks/20261016-dcc-e2e-17/activation-handoff.json
- doc/tasks/20261016-dcc-e2e-17/job5626-handoff.json
- doc/tasks/20261016-dcc-e2e-17/signature-policy-issue.json
- doc/tasks/20261016-dcc-e2e-17/distribution-preview-issue.json
- doc/tasks/20261016-dcc-e2e-17/preview-watermark-investigation.md
- doc/tasks/20261016-dcc-e2e-17/preview-watermark-source-evidence.json
- doc/tasks/20261016-dcc-e2e-17/resume-ui-helpers.cjs
- doc/tasks/20261016-dcc-e2e-17/receipt-signature-bind-issue.json
- doc/tasks/20261016-dcc-e2e-17/final-acceptance.json
- doc/tasks/20261016-dcc-e2e-17/overlap-parameter-assessment-20261019.md
- doc/tasks/20261016-dcc-e2e-17/overlap-parameter-assessment-20261019.json
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r2-bdd.md
- doc/tasks/20261016-dcc-e2e-17/activation-delay-assessment-20261019-r2.md
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r3-plan.md
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-17/repair-20261019-r5-handoff.json
- doc/tasks/20261016-dcc-e2e-17/d17-01-disposition-20261020.md
- doc/tasks/20261016-dcc-e2e-17/decision-20261020-handoff.json
- doc/tasks/20261016-dcc-e2e-17/artifacts/repair-20261019-r4/original-five-directed-maven.log
- doc/tasks/20261016-dcc-e2e-17/artifacts/repair-20261019-r4/DccControlledFilePreviewDownloadApiTest.xml
- doc/tasks/20261016-dcc-e2e-17/artifacts/repair-20261019-r4/DccW08DistributionPreviewTest.xml
- doc/tasks/20261016-dcc-e2e-17/artifacts/repair-20261019-r4/DccDistributionSignatureTransactionTest.xml








## rerun-20261020-r2-round3 EXECUTE

- Status: in_progress; Root 2026-10-20正式授权EXECUTE，覆盖全部适用分支，旧PASS不计。
- Given 原10个独立账号、每人单部门；新运行proof PID/端口/SHA一致，新文件新BPM。
- When 真实UI创建、审批、原作者选当前约+10分钟T；T前核验后将精确上下文交Root，等待唯一调度操作人E16放行。
- Then 未受控/待生效不下发且正式正文不可读；ACTIVE后三人真实预览、水印、各自签收，待签3→2→1→0；OUT及会签不混入。
- Milestones: runtime门禁 → 新申请全审批 → T前取证交接 → Root定向激活 → 全分支取证 → trace验证及关闭回执 → HOLD。
- Constraints: 仅本目录测试资产；无产品改动/Maven/构建/重启/Git/DB写入/API业务/mock/子Agent；不操作5628及旧4320/4318/4321。首次失败保留，脚本定位只做必要校准。
- Verification: 新run result逐项DOM/截图/trace；实际PDF canvas非空；精确版本/BPM/接收人签名；trace隐私/CRC；browser/context/runner关闭。

### Root生效后补充分支（2026-10-20）

Given 三人独立电子签收已保存且授权未改；When 同一精确下发上下文再打开并尝试重复签收，分别查看签前/签后下载打印编制维护入口；Then 本地已保存页禁止重复点击；重新打开后按当前正式幂等规则执行重复提交，正式签收时间和待签名单不变（不得把幂等成功虚记为按钮禁用），签收不新增下载/打印/编制权限。逐人分别记录，不用静态合同或文字提示替代动作。根合同16项+T前正文入口3项之外，新增逐人重复签收3项、逐人权限边界3项（总25）。
