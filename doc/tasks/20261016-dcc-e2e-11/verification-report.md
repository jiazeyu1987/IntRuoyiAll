# E2E-11 最终业务核验（2026-10-18）

最新执行为本文末节`rerun-20261020-r2-round2`，新运行态、新申请业务7/7 PASS，结果在`artifacts/rerun-20261020-r2-round2/handoff.json`，本轮待Root复核。下列ROOT_ACCEPTED属于历史执行。

业务结果：PASS，7/7分支通过；reviewStatus=ROOT_ACCEPTED，Root独立review已接受。工程状态：blocked（本轮禁止Git提交/推送且Root要求保留现场），未标completed。

Root结论：[e2e11-root-review.json](../20261016-dcc-24-e2e-coordination/e2e11-root-review.json)，2026-10-18T05:25:16.916171+08:00。已核对原取消/空密码零请求、各角色前后待办、单次签名854/05:11:03/原file和BPM、后续未指派及截图/trace完整性；接受rejectRemark正式合同所支持的脚本校准，原失败保留。按最新指令停止新业务和清理；工程状态单列blocked。

- 文件 `DCC241016C11F001 / A/1`，文件ID `2054545668044084221`；项目303、文件夹42，APPLICATION_CUSTOM，非PUBLIC覆盖。
- BPM `0fc79f9b-ca6e-11f1-9342-b082e25ec548`；原主管task `0fc7edd8-ca6e-11f1-9342-b082e25ec548`。
- U我发起的唯一申请且无本人待办；S取消/空密码分别零approve-task请求，刷新仍同一task/BPM；S签名前L1/C1均无待办。
- 只执行一次正式主管签名；自然approve-task HTTP200，S待办清空、已办为通过。
- L1会签task `431954cb-ca6f-11f1-9342-b082e25ec548`：签名确认指派可用、未自动选人、审核通过禁用；C1仍无待办。未执行指派或正式会签。
- 同轮签名 `854`，签名人 `DCC241016C11S`，服务器时间 `2026-10-18 05:11:03`，审批通过，节点记录意见 `DCC241016C11-supervisor-approve` 与 #854/VALID/文件/版本/BPM完整一致。

最终机器结果：`verification-result.json`。主运行：`node doc/tasks/20261016-dcc-e2e-11/execute-e2e11.cjs`（原退出1，因脚本错误预期通过意见显示在已办备注）。正式DccApprovalTaskAdapter:381调用ApprovalTaskResultSupport.rejectRemark:37，仅REJECT返回备注，因此APPROVE为空属现有合同；保留原失败。修正为已办通过/空备注，并保留详情节点精确意见断言；在原申请上只读续验，所有续验退出0，无重复上传/签名，不用生产改动换取通过。

只读续验命令：`node doc/tasks/20261016-dcc-e2e-11/run-step.cjs verify-post-approval.cjs L1`；`verify-post-task-lists.cjs C1`；`verify-post-task-lists.cjs S`（todo及view=done）。所有动作通过真实Playwright前端；无API/DB动作、无响应JSON oracle、无mock/force。trace在登录后开始，签名密码前停止并脱敏；密码输入截图遮罩。

关键证据：

- 原始动作及失败：`artifacts/e2e11-2026-10-17T21-10-22-921Z/result.json` 和同目录脱敏trace。
- L1入口/精确签名：`artifacts/2026-10-17T21-16-04-649Z-verify-post-approval/step-result.json`、trace。
- 可见截图：`artifacts/2026-10-17T21-16-12-395Z-assignment.png`、`artifacts/2026-10-17T21-16-12-395Z-history.png`，已人工视觉核验；粉色条为密码遮罩。
- S已办：`artifacts/2026-10-17T21-14-39-410Z-verify-post-task-lists/step-result.json`。
- S无待办：`artifacts/2026-10-17T21-16-58-608Z-verify-post-task-lists/step-result.json`。
- C1无待办：`artifacts/2026-10-17T21-13-08-567Z-verify-post-task-lists/step-result.json`。

运行态沿Root已核验PID2812/24564、8061/48061、Jar SHA4000f1ef44c515241063de6c975701741e1436f75ed35f14bd6cc7ad071deb7b。未修改生产代码、未运行Maven/服务/远端/Git写入/子Agent。本文不宣称PUBLIC矩阵、PDF渲染、E2E12及后续全链通过。

清理：cleanupAttempted=false，cleanupCompleted=false。按Root合同保留全部原申请、角色/组织/项目/类别/模拟签名图片及trace供review；未撤回/删除/拒绝。`checkpoint-latest.json`为当前现场，历史暂停checkpoint.json仅作历史。

---

以下为原准备阶段历史记录，旧prepare_only/等待门禁已被本轮授权取代。

# Verification Report

2026-10-18 latest checkpoint: PAUSED_FOR_ROOT_HMR. Seven task-owned identities passed independent real UI login; own roles, post24, departments and U→S / department→L1 relations are preserved in fixture-manifest.json. Own project inventory was empty, and the last create attempt stopped at a stale selector after navigation to product catalog before any project/file submission. No E2E-11 business PASS/FAIL is claimed. checkpoint.json records exact actors, current runtime, last page/error and remaining preparation; all own browsers are closed for Root's HMR window. No cleanup or withdrawal was performed.

E2E-11 主管先审并签名：PREPARED_WAITING，尚未执行真实E2E。

准备结果：

- 分支已核对为 `int_qms`。Root dispatch 当前为 `prepare_only`，因此本轮未启动浏览器、未登录、未写入业务数据。
- 可复跑脚本：[e2e-11-supervisor-signature.cjs](e2e-11-supervisor-signature.cjs)，trace 清理器：[sanitize-trace.py](sanitize-trace.py)。命令：`node --check doc/tasks/20261016-dcc-e2e-11/e2e-11-supervisor-signature.cjs` 和 Python AST 解析均退出码 0。
- Playwright runner 为前端 `node_modules` 中的 Playwright 1.60.0，Chromium 可执行文件存在；未启动浏览器。
- 脚本 gate 在浏览器启动前读取协调目录 `dispatch.json`，并核对分支 `int_qms`。只有本例 `gate=execute`、环境 `int_qms`、前端 `8061`、后端 `48061` 且 Root 已确认 runtime/role fixture 时才继续；`--gate-only` 也不读取账号或启动浏览器。

参数化子用例与正式入口：

- `author-submitted-and-no-self-todo`：U 在 `/approval-center/my-initiated` 看到任务自有文件，在 `/approval-center/todo` 看不到自己的待办。
- `supervisor-cancel-keeps-same-task`：S 从审批中心 DCC 行的 `审核` 进入 DCC 处理态，核对文件、版本、BPM 和 taskId；取消签名后刷新，仍是同一主管任务，且自然 `approve-task` POST 为 0。
- `supervisor-empty-signature-blocked`：S 不填密码点击确认签名，页面显示密码必填，未发出写请求，刷新后同一主管任务仍在。
- `no-early-leader-or-signoff-todo`：所有选定部门负责人 L 和会签候选人 C 使用独立 context，在主管签名前均看不到本例任务。
- `supervisor-signed-approval`：S 输入意见后停止 BrowserContext trace，再填密码并确认签名；仅观察自然请求状态。回到审批中心核对待办移除、已办意见和任务身份。
- `department-leader-assignment-todo`：主管通过后，每个 L 进入 DCC 会签待办，页面显示 `签名确认指派`、本部门说明且未自动选人，确认按钮仍禁用。
- `exact-application-signature-evidence`：在真实 DCC 详情的“本次申请信息/前序节点记录/所选申请轮次签名”可见表格中核对文件 ID、上传申请、BPM 轮次、主管显示名、动作、服务器签名时间、签名记录 ID 和节点绑定。

前置缺口（BLOCKED，未冒充 PASS）：

- `dispatch.json` 本例 gate 为 `prepare_only`；协调环境 `runtimeVerified=false`、`roleFixtureVerified=false`。
- 未提供 Root 核准的任务自有文件 ID、文件编号、版本、BPM 轮次及已最终提交页面证据。
- 未提供可独立登录的 U、S、各部门 L/C 账号及同租户/部门/角色/签名授权证明；不能用 admin 冒充任一角色。
- 未完成 Root 的 runtime/role fixture 放行；当前 `dispatch.json` 仍为 `prepare_only` 且 `roleFixtureVerified=false`。
- 签名推进后脚本只记录明确残留，不自动撤回、驳回、删除或修改业务对象；任何恢复动作需 Root 另行批准并通过真实页面完成。

Root review 返修结论：

- 已将 trace API 从 page 级改为 `BrowserContext.tracing`；每个登录 context 独立，登录完成后才开始 trace。主管签名密码填入前停止 trace，确认结果后重新导航并重新开始 trace；trace 会先由任务内 Python 清理器脱敏再保留。
- 已依据真实源码入口 `approval-center` 的 DCC `PROCESS_IN_MODULE` 路径改为 DCC 详情处理页，不再把主管审批假设为统一审批中心弹框；脚本定位 `data-approval-action="review"`、`dcc-approval-upload-view`、`dcc-approval-upload-action-panel` 和 `确认签名`。
- 取消分支会刷新同一主管待办并比较 taskId/BPM 轮次；同时在所有部门负责人和会签候选人独立 context 中核对主管签名前无本例待办。
- 通过分支观察自然 `approve-task` 请求 HTTP 200，核对待办移除、已办意见、文件编号/版本/BPM 轮次，并在申请历史可见表格核对主管显示名、服务器签名时间、真实签名记录 ID 与前序节点绑定。
- cleanup 不再谎报已尝试；脚本固定写入 `cleanupAttempted=false`、`cleanupCompleted=false`，若主管签名已推进则保留明确残留状态并交 Root review。

真实执行待 Root 放行后补充：每个子用例独立结果、实际页面 URL/可见文本、任务版本与申请、trace/screenshot 路径、写请求数量、页面终态，以及 `cleanupAttempted`、`cleanupCompleted`、`residual`。当前清理未执行。

## 2026-10-20 第二轮独立重跑结果

本节为本轮最新结果，上方为历史准备记录。本次新真实执行7/7 PASS，0 FAIL、0 BLOCKED、0缺失；待Root复核。历史ROOT_ACCEPTED不作为本次通过依据。

- 汇总：`rerun-20261020-round2-handoff.json`；业务原始结果：`artifacts/e2e11-r20-1792462616193/result.json`。
- 范围：作者已提交且无本人待办；主管取消保持同任务；空签名阻止且0写请求；会签前无L1/C1待办；主管正式签名；L1待指派且审核禁用、C1仍无待办；同轮签名与意见可见。七项分别PASS。
- 新文件编号`DCC241016C11R201792462559199`，新源文件同编号前缀，file `2054545668044084308`，A/1，BPM `43176521-cc2c-11f1-a8a5-b082e25ec548`。主管task `4317b35e-cc2c-11f1-a8a5-b082e25ec548`；取消/空密码均未改变；正式审批仅1次POST/200。
- 页面签名`1073`，S，作者部，服务器时间`2026-10-20 10:17:41`，VALID，意见`DCC241016C11-supervisor-approve`。留在L1未指派会签任务`6e1393e2-cc2c-11f1-a8a5-b082e25ec548`，未推进E2E12。
- 原真实U/S/L1/C1分别重新登录，未使用admin代办。Root明确用户管理查询本人不是额外门槛；正式上传确认框的编制人U/作者部/主管S及S真实待办核对通过。前3次准备脚本阻塞保留，均未创建申请，不列为产品缺陷。
- 运行态int_qms，8061/PID20976、48061/PID27908，SHA `ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab`。dispatch旧身份差异记录在handoff；实际身份按runtime-proof及监听核对。
- 准备退出0、业务runner退出0，browserClosed/contextClosed=true、remainingContexts=0；准备含失败尝试和业务的6个runner PID均已退出。截图、12份脱敏trace及46项产物哈希见`rerun-20261020-round2-audit.json`；CRC/隐私/证据引用检查PASS，确认框及指派/签名截图人工视觉复核PASS。
- 仅测试脚本校准，无生产代码变更；BDD保留全部7项，生产TDD不适用。未执行API/DB业务动作、公共配置变更、构建/重启、Git写操作、cleanup preview/apply或下一轮。证据和待会签新申请保留；工程状态blocked（无Git授权），业务已执行结束。

## rerun-20261020-r2-round2 正式结果

业务PASS，7/7，0 FAIL/0 BLOCKED/0缺失，待Root review；本轮已HOLD。独立本轮证据目录`artifacts/rerun-20261020-r2-round2/`，未复制历史PASS。

|分支|结果|本轮页面事实|
|---|---|---|
|作者已提交且无本人待办|PASS|U我发起的有唯一新申请、待办无本文件|
|主管取消|PASS|0次approve-task；刷新仍同一主管task/BPM|
|空密码签名|PASS|真实必填校验、0次approve-task、同任务|
|未提前会签|PASS|L1/C1独立登录无本文件任务|
|主管正式签名|PASS|1次POST/200；S待办消失、已办通过|
|会签负责人入口|PASS|L1待指派、会签人未默认选定、审核通过禁用，C1无任务|
|本次申请签名证据|PASS|同file/BPM签名1155、S/作者部、意见及真实服务器时间一致|

- 运行态：int_qms，8061/20976、48061/41508，SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147；启动前已写gate.json，EXECUTE/runtimeReady=true，冻结前端6文件哈希匹配。无构建或重启。
- 新文件编号`DCC241016C11R21792474382784`，源文件同编号前缀，file`2054545668044084337`，A/1，BPM`ccb4a110-cc47-11f1-ada5-b082e25ec548`，主管task`ccb5165d-cc47-11f1-ada5-b082e25ec548`。
- 首次失败：prepare/中S打开详情后默认5秒编号断言见“本次申请身份尚未核验”；申请已提交。原始结果/trace/截图及退出1保留在first-failure.json与prepare/。未重复上传；一次recovery/只读恢复从S真实待办打开同一申请，延长加载等待至60秒后编号/动作面板可见，0审批POST。script-calibration.json单独记录校准；没有产品修改，不据此宣称产品修复。
- 正式业务在recovery/manifest上执行一次，业务runner退出0，7分支全通过。仅签名一次，签名`1155`，S，作者部，`2026-10-20 13:38:37`（Asia/Shanghai），VALID，意见`DCC241016C11-supervisor-approve`。最终L1待指派task`828a9e47-cc48-11f1-ada5-b082e25ec548`，未执行部门指派或第三轮。
- 原U/S/L1/C1本轮准备及业务均真实fresh登录；确认框核验U/作者部/S及本次会签部，保留原一人一部门关系，不改账号、组织、权限或公共矩阵。
- prepare/recovery/business三次浏览器会话均关闭、context剩余0；runner/launcher PID5976、29056、42200、36232、40804均已不存在，回执与audit.json可追溯。10份脱敏trace CRC/凭据扫描PASS，43项产物哈希与引用检查PASS；确认框、指派区、同轮签名区截图人工视觉核对可辨识。
- 先标ready_for_closeout，工程因禁止Git及保留证据最终记blocked，业务PASS和HOLD独立记录。不清理业务、不cleanup apply、不启动第三轮；只保留本轮测试资产，无生产实现/TDD变更。


## R3第二轮最终交接

R3 proof/2095源码一致；原始类型选择超时现场保留，fresh只读页面证实目标类型存在，独立续跑仅增加分类请求就绪等待。新申请4402/A/1，BPM c7698d79-cc96-11f1-ada5-b082e25ec548。前三项PASS；第四项L1关键词查询后仍显示历史行、空表断言FAIL，原因未定；后三项BLOCKED未执行。正式审批写0次，无签名未知结果。12份trace CRC/隐私检查通过，6个runner已结束，browser/context全关闭。状态经过ready_for_closeout核对后blocked/HOLD；未Git、未cleanup apply、未重启、未下一轮。正式交接：rerun-20261020-r3-round2/handoff.json。


## R4修复交接


BDD详见task.md的R4节；记录先于生产修改。

- RED（生产修改前）：`node --test IntRuoyiFronted/tests/unit/approval-center-latest-list.test.cjs` → exit1，0/6通过，6项均为预期状态断言失败；包括旧行覆盖、提前loading结束、旧失败清空及徽标错误。
- RED归档补证：通过只读fs hook向同一测试提供HEAD的原SFC实现，exit1；见 `repair-r4-red-baseline.txt`。没有回写生产文件或切换分支。
- GREEN：相同node --test命令 → exit0，6/6通过；见 `repair-r4-green.txt`。
- `node IntRuoyiFronted/tests/e2e/approval-center-route-filter-visible-static.spec.js` → PASS（纯静态合同，不启动浏览器）。
- `node IntRuoyiFronted/tests/e2e/approval-center-tab-return-no-reload-static.spec.js` → PASS（纯静态合同）。
- `node IntRuoyiFronted/tests/e2e/approval-center-todo-standard-list-static.spec.js` → FAIL，原合同要求 `openReviewDialog(row)` 直接审核入口。使用只读fs hook加载HEAD SFC同样FAIL，见 `repair-r4-existing-baseline.txt`；本次差异没有改模板，保留为既有失败，不修改合同或扩大生产范围。
- `git diff --check -- IntRuoyiFronted/src/views/approval-center/index.vue IntRuoyiFronted/tests/unit/approval-center-latest-list.test.cjs` → exit0。

## 交接限制

未执行浏览器E2E、完整类型检查、构建打包、重启、数据库操作、Git写入或跨线程通知。单元回归证明函数乱序提交门禁，不替代真实页面复验；既有直接审核入口合同仍失败。任务经过ready_for_closeout核对，因无Git/清理授权及等待Root review标blocked/HOLD，不标completed。无新运行进程或浏览器。


# E11 R4真实页面定向复验交接

结果：剩余四项业务断言PASS；原R3前三项PASS未重跑，明确区分历史与新证据。状态HOLD，等待Root review。

运行8061/PID20976、48061/PID38176，Jar SHA256 709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147匹配；作者fresh前端登录和身份核验通过。当前审批中心源码哈希运行前后匹配。

复用任务自有文件2054545668044084402 / DCC241016C11R31792508286568 / A/1，BPM c7698d79-cc96-11f1-ada5-b082e25ec548。R3该申请正式审批POST为0；R4先通过S fresh页面核验同一未签名主管task c769b4a6-cc96-11f1-ada5-b082e25ec548后再继续，未重放任何UNKNOWN签名。

- L1/C1在主管前无目标待办：PASS。
- S正式签名：PASS，仅1次approve-task POST200；页面离开签名框、S待办无该申请、已办通过。
- L1待指派会签：PASS，候选为空、签名确认指派可用、审核通过禁用；C1仍无目标待办。
- 同申请签名证据：PASS，签名1352、主管S、审批通过、同BPM及意见DCC241016C11-supervisor-approve、节点链接#1352 VALID一致。服务器页面原始时间2026-10-21 01:06:25仅作为原始证据保存，不当作客户端日期。

## 筛选乱序覆盖边界

所有业务列表按R3原入口加载/重置/关键词查询，记录自然GET的query/start/end/status，待该页面自然挂起列表请求结束后再检查可见DOM。R4常规请求未出现旧请求晚于关键词响应完成。

另fresh L1只读8次快速“任务前缀查询→完整编号查询”，每次所有自然请求结束后，输入保持完整编号且可见目标行恰好1条。八次均稳定，但未自然观察到晚到旧响应顺序。因此真实页面筛选稳定PASS、自然乱序完成验证NOT_OBSERVED；不能用这组证据替代真正乱序完成场景。确定性旧成功/旧失败/finally/徽标/路由保护已有独立6/6 GREEN函数语义回归。

未拦截、延迟或模拟网络，不读响应JSON作业务依据，不直接API/DB。延迟生效job未影响本例，未绕过其错误或推进该链路。

## 运行命令与证据

- node doc/tasks/20261016-dcc-e2e-11/r4-login-preflight.cjs → exit0，真实作者身份核验。
- node doc/tasks/20261016-dcc-e2e-11/r4-real-execute.cjs doc/tasks/20261016-dcc-e2e-11/artifacts/repair-r4-real/fixture-manifest.json → exit0，剩余4PASS/原3NOT_RERUN_R3_PASS。
- node doc/tasks/20261016-dcc-e2e-11/r4-race-probe.cjs → exit0，8次筛选稳定，lateOldOrder NOT_OBSERVED。
- node doc/tasks/20261016-dcc-e2e-11/r4-real-audit.cjs → exit0，6份trace CRC/隐私检查PASS，4个runner进程不在运行，browser/context关闭且剩余0。

正式交接 repair-r4-real/handoff.json；原始结果、时序、截图、trace、脚本校准和audit位于artifacts/repair-r4-real/。无本轮首错。申请保留在L1未指派阶段，未清理业务数据，历史申请不变。

未构建、重启、Git写入、子Agent或通知其他线程。经ready_for_closeout核对后标blocked/HOLD（等待Root review及无Git/cleanup apply授权），不标completed。
