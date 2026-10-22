## 2026-10-20 最新：22-A / 22-B / 22-C 原分支真实 UI PASS

本轮范围替代先前“只读核对原L待办”的已结束范围。按最新委派复用原 Main A/1、A/2、Rel1 A/1/B/1、Rel2 A/1，以及既有四条引用；不操作审批、权限、检出、其它用例或Git。当前 int_qms，8061 PID20976 / 48061 PID22076；R6 Jar文件SHA256、后端进程Jar路径与协调r6-runtime-proof一致。Reader本人真实登录，凭据从原DPAPI读取。

| 原分支 | 本轮结论与真实页面依据 |
| --- | --- |
| 22-A 版本详情 | PASS：A/2→A/1→A/2，逐版核对名称/编号/状态、提交人U与负责人Reader、项目/文件夹/类别、提交备注/变更说明、四项属性、BPM/流程类型、提交/受控/实际生效/启用/下发时间及升版来源/选用草稿。A/1 NMPA/Y/N/N，A/2 CE/N/Y/N；返回A/2仍精确一致。两版均“文控标准化签名后生效”，作者预设/延迟日期按该模式不适用（未造日期或另验其它生效模式）。共同字段按原记录一致验证，未伪造每个字段都有差异。检出隔离继承R5 PASS，不重跑。 |
| 22-B 固定关联/阶段 | PASS：两个Main版本的Rel1仍固定A/1，真实浏览另证明Rel1当前B/1已生效；每次固定正文旧标识匹配、新版标识不存在。Main A/1独有后补Rel2 A/1及正文；A/2无Rel2且独有负责人910510/期限2026-10-22 12:00:00整改。原上传/后补来源、精确文件/BPM、原义务部门/签名人和阶段证据不混版。 |
| 22-C 引用去重/明细 | PASS：A/1显示2项目/3目录，逐行精确核对Target2-F1、Target2-F2、Target3-F1、文件名/编号/A1/已作废，并全部打开固定A1正文验证旧标识存在、新标识不存在；A/2只有Target3-F2一项目一目录，独立A2正文新标识存在、旧标识不存在。行计数/集合与项目去重同时断言。 |

执行：原fixture-ui.cjs启动Reader，委托原business-steps及本例acceptance-20261020.cjs完成7项参数断言（两版各三项及返回A2字段）。session76601、runner16704，finish exit0；context/browser正常关闭，OS核对runner/直接子进程/Chrome均不存在。无新增临时业务夹具、无业务变更，既有样本全部保留；页面自然产生登录及受控阅读start/heartbeat/stop遥测POST，读取遥测均HTTP200，不能将这些写成“零POST”。无需业务夹具清理，未清理证据。

fixture-1792430618420保留两版详情、3次关联固定正文、4条引用正文、关系/整改及引用计数截图（15条记录，14个唯一路径），脱敏trace-1.zip完整性PASS且无.network归档。成功trace导出1张直接截图动作帧并附callId/时间/SHA索引；未宣称逐操作全量导出。关系整改和A1三引用截图已目视核对；详情与正文均有真实DOM断言。

三次脚本校准错误原样保留：弹窗本身无版本号（改为来源行固定版本+正文标识验证）、popup初始URL为空（等待精确目标路由）、返回详情文件夹元数据“尚未读取”（等待精确原文件夹）。均未改变生产代码或绕过业务，修正后沿原页面重新执行通过。本轮无可复现产品缺陷，无生产修复。Node语法检查两个本例脚本exit0；没有全量测试或其它用例执行。

结构化证据：acceptance-20261020-result.json。R5原证据repair-20261019-r5-handoff.json、r5-checkout-ui-result.json保留原状。工程状态blocked：待Root review，本轮明确禁止Git/证据清理，未标completed。

---

## 2026-10-20 最新：只读核对结束，R5 历史 PASS 保留

- 按最新委派收缩为只读核对原 E2E-22 是否仍有可操作且未通过的需求；不重跑检出/撤销或其它业务写入。
- 实际分支 int_qms；48061 PID22076、R6 Jar SHA256 478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a 与 r6-runtime-proof.json 一致。旧 dispatch/PID 和历史 R5 运行态保留为历史，不作为本轮运行态。
- 原 L（DCC241016C22L）真实页面登录，进入审批中心→待办。两次只读 DOM 回读和可见文本断言均为“暂无审批任务”，原 3481664b... 不具备继续指派前提。个人工作台的分发待签收不是原审批任务，不扩大操作。
- fixture-1792429895498：session81245，runner38756，finish exit0，context/browser 正常关闭；OS 检查 runner/直接子进程/Chrome 均无残留。自然请求只有登录 POST，其余均 GET，业务写入 0；未触发检出、撤销、签名、关联、权限或数据清理。
- dom-4.json、dom-7.json、两张截图及有效脱敏 trace-1.zip 均保留；trace ZIP 完整性通过且无 .network 条目，最终截图已目视核对。结论 READ_ONLY_NO_NEW_E2E_RESULT，不是新增 E2E PASS。
- R5 八步 PASS 仍以 repair-20261019-r5-handoff.json / r5-checkout-ui-result.json 为准。当前范围没有识别到仍可操作的未通过原需求；后续需要 Root/用户明确未通过业务项。工程状态 blocked（待 Root review，本轮禁止 Git）；未运行 cleanup、提交、广泛测试或服务操作。
- 结构化结果：resume-20261020-readonly-result.json；当前 manifest 与本轮 checkpoint 标明只读范围，保留原 R5 环境及旧 dispatch 为历史。原 e2e-result/runner-checkpoint 中的 R5 业务证据不改写为本轮结果。

---

# r4最新：只待新包检出隔离与本轮任务锁撤销，准备不计PASS

当前实际分支int_qms，dispatch仍指向旧120105/ba0a8ead。未收到Root包含检出修复的新Jar/hash放行，不运行该新包E2E。本轮没有代码修改、浏览器或业务命令。

Query277/核心323、前端49及原A1/A2冻结展示页面PASS保留，不再编辑Query，不重跑R29/R30、关联或引用。旧nameVisible断言已通过，不再列当前失败。

准备的唯一验收：原U新独立context确认旧锁已释放→原4260/A2检出一次→A2 actor/time/reason精确正例→原4249/A1历史三项无污染→返回A2仍精确正例→UI撤销本轮新锁→两版未检出回读并确认任务锁已清除。原12:42撤销不重放，发生失败准确记录本轮业务与清理结果。

repair-20261019-r4-handoff.json列真实命令/退出码/XML、代码归属、保留页面证据、运行包要求及未执行边界。计划E2E status=NOT_RUN_WAIT_ROOT_NEW_PACKAGE、exitCode=null；不把准备或旧包结果计新包PASS。

---
# r3最新：Query277与核心323通过，仅待新包检出隔离页面验收

已核对E2E13最新实际Query XML：277 tests/0 failures/0 errors，含旧nameVisible修正、检出精确投影及revisionOptions守卫。四核心套件合计323项全通过。证据：doc/tasks/20261016-dcc-e2e-13/runs/20261019-repair-r2/r2-targeted-final/TEST-cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryServiceTest.xml，以及13的repair-20261019-r2-handoff.json。组合process exit1由额外OfflineTraining夹具2项错误导致，不能混同为Query失败或整个命令通过。旧277失败/待回归状态均为历史，已被上述新证据替代。

前端冻结职责/原义务部门49测试与原A1/A2只读UI PASS继承，不重跑展示、R29/R30或主链。

唯一remaining：Root新Jar/hash明确包含检出投影并放行后，原U独立context检出原4260/A2，A2 actor/time/reason与本次事实一致；进入4249/A1历史断言三项均未检出，返回A2仍为本次事实；最后真实UI撤销本次新锁及双版回读。原12:42锁已撤销，不重放旧取消。

当前dispatch旧120105/ba0a8ead不能证明此后端修复。24独占Maven、14全量类型，本例仅交接不抢占；本轮零生产修改、零浏览器与业务命令。详细文件/方法/实际结果/运行态依赖/剩余项见repair-20261019-r3-handoff.json。

---
# r2最新结果：冻结展示已修复并原UI通过，检出隔离待新后端

- 检出投影：Root有效RED4/3失败→GREEN5/0失败；Master互斥布尔保留，A1/A2 actor/name/time/reason按精确baseIterationId隔离。旧nameVisible测试合同已对齐，只改该方法，Root串行扩大回归待执行；不放宽生产。
- 冻结职责根因：正式已生效详情有意不加载实时tasks/scope，旧traceRole却只读实时tasks。已改正式详情按同BPM/版本/task/action的冻结meaningCode解释职责，缺值/跨版/矛盾仍未记录，未扩BPM权限或查现组织。
- 原义务部门根因：关系stage header不含departmentName，已有nodeRecords.assignmentTarget.obligationDepartmentName实际来自原taskAssigneeSnapshot；此前没传给该显示区。现仅file/BPM/task/signature/actor/stage和ASSIGN唯一匹配时使用原快照；确实缺快照仍未记录，不用签名人部门替代。
- 实际前端RED4项2失败，检出原因缺显示项追加RED5项1失败；修复后49项全PASS。结果见frozen-signature-presentation-red.json、checkout-reason-ui-red.json、frozen-signature-presentation-green.json。
- 实际只读UI PASS：Reader新context在原Main4260/A2显示S/L/C/A/D五项正确职责，原A1/4249及A2/4260各自BPM都显示原冻结义务部门。证据frozen-signature-presentation-ui.json，3张截图已目视核验，trace目录fixture-1792407117723；未重签或整体重跑R29/R30。
- 检出原因：原只读记录只有人/时间，无原因；现新增一个既有checkedOutReason描述项，未检出时不展示残留原因。自有脚本verify-main-checkout-isolation对A2 actor/time/reason正例和A1全部未检出负例显式断言，随后撤销本次新锁回读。当前旧包不含后端修复，尚未运行该新后端E2E。

Root所需窗口：13的Maven结束后，串行执行对齐的nameVisible及Query相关回归、全量类型检查、统一构建切包。只在明确新Jar/hash已加载后运行检出受影响参数。详见repair-20261019-r2-handoff.json（files/methods/testCommands/results/runtimeRequired/remaining/e2eEvidence）。

业务与工程分开：本例未完成全部受影响验收，未ready_for_closeout；无Git提交/推送授权。保留全部原资产/失败与新证据。runner21924/browser24672已正常关闭，exit0、归属进程为空。本轮零检出/签名/审批/配置业务命令，无DB/API业务操作。

---

以下保留历次记录；状态冲突以本节及r2 handoff为准。
# 最新：Root检出隔离定向GREEN 5/0/0，等待新运行包

已读取e2e22-checkout-isolation-green-process.json及同名XML，确认exit0、5tests/0failures/0errors，包含四项原RED及revisionOptions守卫。实际GREEN已同步checkout-isolation-green-freeze.json和checkout-isolation-red-handoff.json。

源码保持冻结，Query所有权仍在Root，由Root串行处理13窄点后统一回归/构建。当前运行包尚不作为此修复E2E证据；本线程没有运行新UI或业务动作。

待明确新Jar/hash放行，只复验原MainA2检出、A1/A2详情与历史检出事实隔离、真实UI撤销本次新锁及回读。已通过主链及其他参数继承，不整体重跑。

---
# 2026-10-19 检出投影实现冻结，等待Root GREEN与新包UI

Root有效RED已成立：4tests/3failures/0errors，合法EXACT通过，旧版借actor88、OTHER_VERSION及无锁残留77精准失败。证据：协调目录e2e22-checkout-isolation-red-process.json和同名.xml。

在Root明确授权后，仅修改DccControlledFileQueryServiceImpl的两个fillCheckoutProjection，6行增加/2行替换：保留Master checkedOut布尔；先清空actor/name/time/reason；只在锁baseIterationId精确匹配file.id/history.id时填原事实。readLogicalCheckout、Master写锁、检入/撤销、assertRelationNameVisible及其他Query逻辑未动。

冻结SHA256：221e080af27cc84e958a18bd02097d796db164a596775908eff39f91f0ca54aa。精确diff与命令见checkout-isolation-green.diff、checkout-isolation-green-freeze.json。

GREEN/回归：NOT_RUN_PENDING_ROOT；本线程未启动Maven/构建/UI/服务。当前120105运行包不含本修复，待Root新包明确放行后仅复验原A2检出/A1未检出/A2仍持锁及UI撤销本次新锁。原通过主链和R28/29/30证据继承，不重签、不整体重跑。原12:42锁已撤销，不重放旧撤销。

---
# 2026-10-19 最新范围：检出信息串版定向修复，RED待Root执行

本轮继承Root接管后的最新fixture-manifest，Main4249/A1与4260/A2审批、不同属性、整改、引用及R28/29/30原UI已通过；不按下方历史旧BLOCKED重建或重签。当前唯一未闭环是检出字段隔离：只检出A2时，两版本详情均显示U/12:39:25。此前仅采集字段不算隔离通过，原锁12:42已经真实UI撤销。

根因：Query两个fillCheckoutProjection直接把同一Master锁的四个版本事实复制给全部所选/历史版本；旧单测还断言全部历史时间相同。最小修复保留Master互斥checkedOut布尔，by/name/time/reason只归baseIterationId精确版本，其余显式清空；写锁、检入和撤销逻辑不改。

BDD、精确测试diff、Root Maven命令已准备：checkout-version-isolation.md、checkout-isolation-red-test.diff、checkout-isolation-red-handoff.json。生产本轮未改；实际RED=NOT_RUN_PENDING_ROOT。Root确认后才能生产GREEN，Maven/构建/运行态由Root串行。

自有E2E脚本已补真正的A2正例、A1负例及本次时间比较，新增verify-main-checkout-isolation；待新修复包放行后仅复验此参数并真实UI撤销本次新锁。没有启动浏览器或业务写入。

当前READY包120105/hash ba0a8ead，PID25124/20976；reboot1019-runtime-proof.json明确newFixesIncluded=false。当前包不能证明本轮修复通过。

---

以下历史报告按当时执行阶段保留，最新状态以本节与最新manifest/checkout交接为准。
# R22-01 实现已冻结，等待Root GREEN（最新）

Root实际RED：6项/1通过/5失败，退出1；未选的upload/revision/obsolete原签名合同控制通过，其余因真实parent缺后补选择控件失败。进程证明：doc/tasks/20261016-dcc-24-e2e-coordination/e2e22-supplement-entry-red-process.json。

在Root明确放行后，仅修改生产DetailSignoffAssignment.vue，SHA256=1e0db0f9cd2607b05e69221021e09969e8d1a657b6dbde6bbff69eed83f47f32。复用原slot与同项目DccFileSelector，仅将确认的固定后补ID并入原签名saveAssignment；原上传关系、签名字段、revision整改及后端二次验证保持。取消保留、明确清空、上下文切换清理分别处理。

SignoffAssignmentPanel.vue、workflow-actions.ts及后端VO指纹保持；没有新组件/API/DTO/权限。原测试仅补纯渲染组件注册，目标断言未改。

GREEN/REGRESSION：NOT_RUN_PENDING_ROOT；Root独占测试，本线程没有运行Node测试、类型/lint/构建或E2E。Root命令仍为在IntRuoyiFronted执行node --test tests/unit/dcc-signoff-supplemental-relations.test.cjs，并执行相邻指派/整改范围回归。冻结清单见r22-01-green-freeze.json。

本次没有业务动作；Main4249/BPM1842903f-cb00-11f1-ad52-b082e25ec548/L任务3481664b-cb00-11f1-ad52-b082e25ec548保持未指派，等待Root节点放行后原UI验收。未将实现完成写成GREEN或完整E2E PASS。

---
# R22-01 前端行为RED准备交接（最新，待Root执行）

仅新增IntRuoyiFronted/tests/unit/dcc-signoff-supplemental-relations.test.cjs和本例BDD/证据；生产源码未修改，四份原源码SHA256保持。Root独占测试，本线程未执行测试、语法检查或lint，实际单测RED为NOT_RUN_PENDING_ROOT。

命令：在IntRuoyiFronted运行node --test tests/unit/dcc-signoff-supplemental-relations.test.cjs。详情见r22-01-red-handoff.json。

测试编译真实DetailSignoffAssignment/SignoffAssignmentPanel/DetailRelationArrangements模板，经控件事件走真实签名handler与workflowLifecycle请求边界；Element Plus及既有选择器为单元边界，选择校验复用真实FileSelectorState。断言Rel2固定字符串ID、原Rel1不变、原签名字段、revision整改字段、取消/清空/未选及精确项目隔离。它不是Playwright E2E，不替代原业务参数。

恢复至PID14864同包264efbd4后，L新独立context真实登录个人工作台成功；收到Root测试准备指令后即关闭，未进入Main或产生业务动作。handle59698退出0，runner17416及其子进程核查为空。Main4249/BPM1842903f-cb00-11f1-ad52-b082e25ec548/L任务3481664b-cb00-11f1-ad52-b082e25ec548原位保留；R22-01前次真实缺入口RED仍有效，恢复登录不等于补关联通过。

---
# E2E-22 最新真实执行结论（2026-10-18，264efbd4运行态）

业务结论：FAIL_UI_SUPPLEMENT_ENTRY；工程状态blocked，等待Root唯一源码写入者补正式入口。未达到ready_for_closeout，未宣称整例PASS。

- 环境：int_qms，8061前端24564，48061后端34440；Jar SHA256 264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5。Root runtime proof：browser-replacement-runtime-proof.json。
- 已完成：八个任务身份真实登录与独立context；P1/P2/P3完成、权限读回；本例类别9条权限规则、独立查看矩阵8个精确用户保存后重新读回；Rel1和Rel2 A/1经S/L/C/A/D真实签名，在受控浏览确认“受控（已生效）”。
- Main文件2054545668044084249，BPM1842903f-cb00-11f1-ad52-b082e25ec548；S已签名，L原待办3481664b-cb00-11f1-ad52-b082e25ec548停在会签0/1，未指派。
- Main原上传关联Rel1 A/1：固定版本入口、原上传来源、非空PDF canvas、A正文标记命中/B标记不命中均通过。这只是正向基线子步骤，不是源升级后的固定关联隔离PASS。
- 真实RED：L指派区只有会签人、意见、密码、签名确认指派，后补/关联操作数量0。缺正式UI入口导致不能准备非空后补关系；不使用API/SQL/注入补造记录，也不空签名越过必验分支。

| 参数 | 本轮结论 |
|---|---|
| 22-A 两版本基本字段、实际属性、时间、检出隔离 | BLOCKED：两个目标版本未形成 |
| 22-B 原上传固定关联及正文基线 | PASS_SUBSTEP |
| 22-B 源升级后固定关系仍指向旧版 | BLOCKED：尚未创建源B/1 |
| 22-B 后补关联入口 | FAIL：真实可见入口0 |
| 22-B 整改安排/轮次隔离 | BLOCKED：revision阶段未到达 |
| 22-C 引用固定版本及按项目去重 | BLOCKED：Main尚未生效，未造引用 |

完整复现、直接源码依据、修复BDD见defect-supplement-entry.md；当前状态以e2e-result.json、runner-checkpoint.json和fixture-manifest.json为准。

本轮证据目录：e2e-artifacts/fixture-1792329547050/，29份脱敏trace、2张已目视核验截图及dom-48.json。签名凭据输入前停trace；指派区截图遮蔽密码控件。四个CJS文件node --check退出0，仅语法通过。

finish正常退出0，runner47716、Chrome21488及归属进程核查为空，全部context/browser已关闭。runtime-pause.active=false（运行态已放行），当前停止原因是业务缺入口。保留全部本例正式对象用于原待办续跑；仅失败的未提交Main临时源件已真实UI删除。cleanupAttempted=false、cleanupCompleted=false；未修改生产源码/共享配置，未Git写入、构建或服务操作。Root review前保留证据，不执行cleanup apply。

恢复要求：Root安排补齐后补固定关联真实UI与原签名事务字段，保持原file/BPM/task；新L context重新真实登录并先只读确认任务，再执行后补关系及本人签名；随后继续原两版本、时间/检出、固定关系、引用去重、整改全部参数，不重建Main。

---

以下为历史记录，遇状态冲突以本页顶部和最新e2e-result.json为准。
# Verification Report

## 2026-10-19 R5 Status

业务状态：R5限定八步真实Playwright验收PASS。Root的 `r5-runtime-proof.json` 放行新包；本机SHA核对与proof一致，PID19844命令行指向该Jar。旧dispatch不覆盖本轮明确的新proof。工程状态blocked：本轮禁止Git，待Root review，不标completed。

- 有效工程验证：Root定向RED 4项/3失败/0错误；Root GREEN 5项/0失败/0错误。
- 保留既有通过：Query 277项、前端49项及原页面证据不重跑；Query整体Maven invocation退出1是因额外OfflineTraining夹具2项错误，不将整条命令写成PASS。
- 最新放行范围仅原A2/A1检出隔离八步：U真实登录，原4260/A2于2026-10-19 23:24:30检出一次；A2 actor/time/reason与本次事实完全一致，4249/A1历史三项均未检出，再回A2仍保留本次事实。UI撤销后A2/A1三项均回读为未检出，任务新锁已清除。没有重新验正文、完整属性、关联或引用。
- 自然写请求：一次 `POST /admin-api/dcc/controlled-files/2054545668044084260/checkout`、一次同文件 `/checkout/cancel`，均HTTP200；另有本人登录请求。业务通过依据为真实DOM断言，未读取响应JSON作通过依据；无角色/权限/调度写请求，无API/SQL业务调用。
- 唯一运行目录 `e2e-artifacts/fixture-1792423115516/`：五组精确检出投影断言、逐步DOM及截图、有效脱敏 `trace-1.zip`、从trace导出八幅关键步骤帧及 `trace-frame-index.json`。A2持锁、A1隔离、A2释放三幅局部截图与A1 trace帧已目视核验。
- 两次临时只读定位器超时原样保留：在项目浏览表使用了存储列表的 `.browser-list-wrap`；按原项目表定位器恢复，错误期间零业务请求，未改生产或重复检出/撤销，不视为业务FAIL。
- runner40896正常finish，exit0；context/browser关闭，OS核查runner/直接子进程/Chrome均为空。仅恢复本轮新锁，无临时授权或调度需要恢复。既有任务夹具保留。
- 详细运行事实与逐步状态见 `repair-20261019-r5-handoff.json` 和 `r5-checkout-ui-result.json`。

## 2026-10-18 Latest Runtime Pause

当前业务结论：未通过，等待Root更新窗口结束后从原checkpoint继续。运行身份为int_qms，8061 PID24564、48061 PID18204，Jar SHA256 `a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402`。本轮没有生产代码修改、API/SQL业务动作、强制click或响应JSON通过断言。

真实前端已完成：Reader账号910474、Cfg账号910488创建、首次改密、独立context登录；本例Reader角色991280、Config角色991336及最小菜单配置；存储目录DCC241016C22Storage、类别DCC241016C22Category、技术文档下专用分类分支、文件夹模板24（F1/F2）。P1项目由Cfg提交、Reader以正式项目审核员职责独立审核，页面终态COMPLETED。P3最后页面PENDING_REVIEW；P2已提交但最后审核点击未定位到目标行，没有取得写回执，恢复后须先只读核对。未修改共享角色定义或公共矩阵。

本轮问题均为测试工具/夹具问题：首次登录强制改密已走正式页面完成并安全缓存；Element Plus隐藏input和自定义弹窗标题定位已调整；分类阶段正式来源要求技术文档根，原自有根保留为未使用分支，另补齐正确分支；项目申请入口依赖产品目录，已补本例Config对应正式菜单。最后prompt/click等待顺序导致未处理promise拒绝和runner退出，已只修本例脚本为Promise.all，Node语法检查退出码0；尚未沿真实审核流程复验该脚本修正，不能标GREEN。

暂停证据：runtime-pause.json active=true；最后runner PID8700/handle7168退出，OS核对本例node及其直接子进程均无残留，browserClosed/runnerExited=true。保留最后run `e2e-artifacts/fixture-1792305808023/` 下三个脱敏trace、DOM及P1终态截图。最后失败段因异常退出未完成tracing.stop，未保存该段trace；此限制不隐藏。精准账号、对象、运行身份、未知申请ID及恢复顺序见runner-checkpoint.json。

详情字段/属性/时间、固定关联正文、引用项目去重明细、原提交/后补/整改四组业务验收均未运行至终态，不记PASS。当前无文件ID、版本、BPM或taskID；未上传临时源件，未尝试数据清理，已成功准备的本例资产保留供恢复使用。

## 2026-10-16 Preparation History (superseded gates)

## Status

E2E-22 详情与关联引用隔离：`PREPARED_WAITING`。本轮只完成脚本/结构准备；不是 E2E PASS。Root dispatch gate 为 `prepare_only`，因此浏览器未启动，真实页面、业务数据、trace、截图和清理均未执行。

## Subcases and Entry Points

- **22-A 版本详情字段隔离**：同一文件编号至少两个可辨识版本；每版属性、备注、编制人、项目、文件夹、类型、检出人/时间及生效事实必须可区分。通过真实“受控浏览/文件浏览”菜单进入 `/dcc/controlled-file/browser`，选择“全部版本”，对每个版本逐一点击行内“浏览”，在弹窗点击“查看详情”，读取只读详情的文件信息、版本历史、申请/节点记录。
- **22-B 关联固定版本隔离**：源文件关联对象至少存在旧/新版本差异；正式详情的 `data-testid=dcc-detail-relations` 必须显示“所选版本关联”、固定版本号、原上传/后补来源及整改安排，点击“查看固定版本”打开固定正文/只读记录，不得自动追新版。
- **22-C 引用项目去重与明细隔离**：同一目的项目至少多个文件夹引用，并有第二目的项目引用；从“文件信息与记录”点击“查看引用项目和所用版本”，在“引用使用明细”中核对全部引用项目、可见项目、目录引用数量，以及项目、文件夹、编号、固定所用版本和状态。

## Role and Fixture Gates

- 目标租户必须在登录后页面确认是“芋道源码”；账号必须是 Root 核对过的真实角色账号。不能用 `admin` 代替申请人、项目负责人、文控审核人或只读查看角色，不能猜密码、重置密码或改菜单/角色。
- `DCC241016C22` 夹具必须由真实前端创建，或由 Root 明确提供可只读复验的同租户夹具：至少两个版本、关联文件的版本差异、同项目多文件夹引用、第二项目引用；如要求验证申请冻结证据，还需正式页面形成原上传、后补和整改记录及可辨识的申请轮次。
- 运行前 Root 需确认 `int_qms` 前端 `8061`、后端 `48061`、运行 Jar 来源/提交及前端菜单权限；当前 dispatch 标记 `runtimeVerified=false`、`roleFixtureVerified=false`，故属于待确认前置，不是产品失败。

## Script and Static Evidence

- Script: `doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs`
- Prepared manifest: `doc/tasks/20261016-dcc-e2e-22/prepared.json`
- Syntax: `node --check doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs` -> exit `0`。
- Gate check: `node doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs` -> exit `0`，JSON `status=PREPARED_WAITING`、`browserStarted=false`。
- Forbidden-action scan -> `PASS`；未发现 fetch/axios/APIRequest/apiGet、storage 注入、页面脚本注入、mock 或 API/DB 写入。
- Browser source available for later run: Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`; front-end dependency `IntRuoyiFronted/node_modules/playwright` exists。实际运行时由 Root 传入 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` 并在日志记录。
- Result evidence: `doc/tasks/20261016-dcc-e2e-22/e2e-result.json` is written on every invocation. Execute runs additionally write `e2e-artifacts/e2e-22-trace.zip` (or `e2e-22-trace-failed.zip`) and one screenshot per key step. The result file is the review source; stdout is only a transport summary.
- Trace sanitization: `scripts/sanitize-trace.py` retains UI trace/snapshot/screenshot entries while dropping Playwright network archives and redacting query secrets/password values. Raw trace is removed after sanitization; login is completed before tracing starts.

## Blockers and Pending Verification

`BLOCKED/PENDING`：dispatch 尚未 execute；当前 `runtimeVerified=true`，但 `roleFixtureVerified=false`。真实角色、页面菜单投影和 DCC241016C22 夹具尚未核对，故 22-A/22-B/22-C 均未给业务结论。execute 还会阻止少于两个可区分版本、缺任一必需字段、编制人与文件负责人未分离、少于三项属性、缺固定关联/引用/原提交/后补/整改记录或引用项目不足两项目的夹具。放行后缺数据写具体 `BLOCKED`，页面字段串版或固定版本断言失败写 `FAIL`；不得用第 2 尾部、第 6 或第 8 未定义规则补齐。

## Cleanup

本轮没有浏览器、业务写入或清理动作：`cleanupAttempted=false`、`cleanupCompleted=false`、`residual=none created by preparation`。Root 放行后的任何写入夹具必须在每次页面写成功后记录状态，并通过同一真实页面清理；清理失败不得覆盖原始结论。

## 2026-10-18T20:56:21.0368661+08:00 最新暂停检查点（替代旧维护状态）
- 业务静止：是；context/browser/runner关闭；runner9476、browser48444及直接子进程核查为空，runner退出码0。
- 最终只读页面：Rel1 #2054545668044084243，A/1，待直属主管审核，0/1已完成，签名及节点记录暂无数据。证据：C:\IntRuoyiAll-int_main\doc\tasks\20261016-dcc-e2e-22\e2e-artifacts\fixture-1792312491653\dom-137.json。
- 原BPM/task保存在runner-checkpoint.json；P1/P2/P3已COMPLETED，角色/组织/签名及类别权限已真实UI准备。Rel2/Main未开始，全部业务参数仍BLOCKED。
- 20份脱敏trace保留；仅任务证据整理，无新业务写入、无生产/运行态/配置修改。


## 2026-10-18T22:56:11.0644081+08:00 Root22:54运行态补充
Root确认后端34440退出，正在恢复同一软件版本。本例此前已停止且无新动作，归属进程核查仍为空；runtime-pause.active=true。原业务FAIL_UI_SUPPLEMENT_ENTRY与现场继续保留，本次后端不可达不覆盖或混同该业务缺入口。没有不确定写入需要重试；恢复先UI只读核对原Main L待办。
