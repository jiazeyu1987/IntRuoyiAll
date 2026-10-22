# R3第一轮当前结果

业务PASS；正式证据入口：[handoff.json](rerun-20261020-r3-round1/handoff.json)。S1/P1固定A/1、S2切换清空、S3/P2固定B/1及S4双页隔离/完整P2未关联确认取消全部本轮真实页面验证。新工作草稿4388、4390均刷新重开。非法HTTP正式UI不适用/未触发，不宣称HTTP拒绝。

原始业务错误0；trace 3112项CRC PASS，凭据命中0，raw network entries 0。context/browser正常关闭、runner exit0、残留0。两草稿保留；CONFIRM临时票据最终销毁未核验。工程收尾独立blocked（无Git授权），不开始下一轮。

---

# E2E-06 验证报告（更新2026-10-20）

## 2026-10-20 Root后续合同复核（最新口径）

Root本轮明确要求以正式用例“任何残留跨项目选择均被拦截”为合同，不要求必须构造非法HTTP。E06业务合同已由既有S1/S2/S3及S2/S4 UI隔离+P2完整确认取消覆盖，追加review为BUSINESS_CONTRACT_COVERED/PASS；服务端非法HTTP分支仅为正式UI不适用/未触发限制，不再作为E2E业务BLOCKED。原始run/result/handoff的3PASS/1BLOCKED、首失败及trace全部保持原样，下方先前BLOCKED为历史执行口径。

只读分析见s4-contract-review-20261020.md；追加机器记录s4-contract-root-review-20261020.json包含合同、最小UI证据、调用链/单测定位及SHA256。新建申请公共/custom、草稿保存均经validateAndBindRelatedFiles校验项目，跨项目业务码1080000302；既有DccCurrentFileRelationsTest XML为历史9/9 PASS（本次未跑）。未发现本例已证实产品缺陷；完整Controller事务/HTTP拒绝未实测，不能冒充本轮服务端验证PASS。没有新增E2E/浏览器/构建/测试、产品/共享修改或Git/数据库/服务/子Agent操作。工程closeout独立blocked，继续hold。

## rerun-20261020-r2-round1 执行结束（当前）

Root新包SHA256 709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147，8061/48061，原U独立fresh context真实登录。S1/S2/S3本轮PASS，原S4跨项目服务端拒绝BLOCKED；最终确认页补验独立PASS。所有结果来自本轮页面，无旧结果替代。

S1新草稿2054545668044084332固定P1CAND A/1；S3新草稿2054545668044084335固定P2CAND B/1；均实际刷新并从受控浏览精确行重开。S2切P2清空旧关联/编号/预览，P2文件夹可加载，正式切换自带session-cleanup HTTP200。双tab旧页仍是P1，没有形成P2携带P1选择。

S4按新要求补齐合法必填及真实CONFIRM PDF，保持关联0，点击创建后到达最终“确认提交”。项目P2、“关联文件：未关联”、无P1候选；点击取消后内容保留，该区间submit/working写请求0。不能据此宣称服务端跨项目拒绝已覆盖。

首次失败全部保留：failure-7为切换后会签部门选择未保留，随后正常页面补填成功；failure-11为确认取消保留单文件，直接替换DRAFT2触发limit=1、未生成预览且未保存。第一段checkpoint-1.json/trace-1.zip及两份失败现场封存；新context仅续跑未保存S3一次后PASS，不重跑已完成分支，不抹除首失败，不据此断言产品缺陷。

两runner实际exit0；两个context/browser均await关闭，精确runner及已记录Chromium后代进程0。validator exit0；trace-1共2577条目、trace-2共851条目CRC全PASS，凭据命中0，原始network内容0，pageErrors=0。仅扫描新run；历史trace-25未打开或修改。独立交接runs/rerun-20261020-r2-round1/handoff.json，含逐项状态、对象ID、首失败、截图、命令/退出码、CRC/隐私及进程证据。

业务结束后hold，不启动第二轮；全部数据和证据保留。CONFIRM未正式保存，其最终临时票据销毁未独立核验，不写清理PASS。用户禁止Git及清理，工程closeout独立BLOCKED。

## rerun-20261020-r2-round1 准备（历史准备时快照）

PREPARE_ONLY_HOLD；新run独立prepared.json、execution-gate.json、prepare-verification.json及execution-plan.md已准备。原U账号DPAPI只读可解密且目标key非空，Chromium/Playwright与原PDF可用；尚未登录/核验当前候选。本轮S1/S2/S3/S4均待真实执行，不能引用下方历史通过结论。

runner语法与prepare-check native exit0；无放行默认/--execute均native exit3并在启动浏览器前等待。结构核验覆盖本轮runId、全分支计划、S4补齐P2必填后核对确认框P2/未关联并取消、动作区间请求计数、正常及异常context/browser关闭；仅证明准备，不算E2E。当前无result/trace/业务动作。Root新包READY及单独放行为唯一待满足前置，不改生产或共享汇总。

## 2026-10-20 第一轮重跑（最新）

本次独立run为runs/20261020-round1，S1/S2/S3均重新执行PASS，S4 BLOCKED。原U真实登录，原候选当前页面ACTIVE回读；新草稿2054545668044084285固定P1CAND A/1、新草稿2054545668044084290固定P2CAND B/1，均刷新并经受控浏览精确行重开，无重复草稿保存。S2确认切换后旧关联/编号/预览清空，P2目录可重新选择。

S4双页保持独立项目；同页旧P1关联清为0后，本次正式按钮可用且真实点击，出现类型/分类/实际文件/编号四项必填校验，无正式submit请求；跨项目HTTP拒绝目标仍未触发，不能记PASS。10-19的按钮禁用属于历史。脚本11次失败及校准保留于events/failure截图；折叠菜单、确认弹窗和累计历史请求误判均未归为产品缺陷。

执行器显式stop、browser.close后exit0、精确node进程数0。本轮validator exit0，当前trace 3717条目全CRC PASS、凭据命中0、原始network内容0、pageErrors=0；仅扫描新run，不修改历史trace-25结论。关键证据为本轮result.json、DRAFT1/2-fixed-relation-panel.png、S2-after-switch.png、S4-two-tab-boundary.json、S4-form-validation-blocked.png与verification.json。当前结果及工程closeout边界见rerun-round1-20261020-handoff.json。不执行清理/Git/生产修改/后续轮次。

## 2026-10-19 S4 正式页面补证（历史）

按本轮授权只执行P1关联→确认切P2→尝试正式提交入口，未重跑S1/S2/S3完整流程。结果为**BLOCKED_FORMAL_SUBMIT_BUTTON_DISABLED_AFTER_RESET**：切换前P1CAND A/1已选1，切换后P2关联已选0、编号/名称为空、文件夹未选、正文预览消失。正式“创建受控文件”按钮可见但禁用，按要求在此停止，没有强制点击、注入状态或增加入口。

页面前置校验卡可见“文件编号/版本：待填写”“生效方式及日期：待核对”“文件类别/审批人链路：待匹配”“项目存储文件夹：待选择”。没有点击后的校验消息，也没有跨项目拒绝HTTP。自然请求仅登录、路线预览、正文临时上传和正式切换自带会话处理四个POST（均200）；正式提交、保存草稿和关联写请求均0。由页面及自然请求可确认本次旧P1关联未被提交；没有新对象可重开，不将此称作DB持久化核验，也不改变原有P1草稿的合法关联。

与原S2合并，确认这条正式页面路径通过清空旧关联和禁用不完整表单提交来阻止携带旧选择。原S4跨项目HTTP拒绝仍未触发，历史BLOCKED保留。没有证明产品缺陷，未改生产代码。新证据在[runs/20261019-s4-submit/result.json](runs/20261019-s4-submit/result.json)、[提交入口截图](runs/20261019-s4-submit/04-P2-submit-validation.png)、[独立交接](repair-20261019-s4-submit-handoff.json)。执行器正常exit0只表示运行到明确阻止点并关闭浏览器；新trace 327条目CRC及凭据/原始network内容扫描PASS，验证脚本exit0。未读取或处理trace-25，历史完整隐私扫描结论不变；没有清理、Maven或服务操作。

最终关闭核验：精确匹配本例脚本的node进程数为0；runner已正常exit0，结果写入前已await browser.close。新result/currentAssessment、执行日志和报告口径一致，历史Root接受状态`ROOT_ACCEPTED_WITH_EXPLICIT_BOUNDARY`不变。按最新指令到此结束，没有后续页面复跑、清理或Git写入。

## 2026-10-19 R4 证据边界

本轮未执行新的业务操作，也未重跑原通过流程、Maven或trace-25来源检索。R4 handoff为[repair-20261019-r4-handoff.json](repair-20261019-r4-handoff.json)，只整理既有证据：S1/S2/S3 PASS保持不变；S4仍BLOCKED，因为真实双tab流程没有形成跨项目提交（`crossProjectSubmitAttempted=false`）。因此没有真实UI触发的服务拒绝HTTP证据。Root review接受三项页面流程与UI隔离不变量，并将Root独立9项后端测试PASS单独列示；两者不互相替代。

trace-50既有2558条目CRC PASS；历史完整隐私扫描仍为`BLOCKED_UNREADABLE_ARCHIVE`，trace-25原件保留。已完成来源调查没有找到可校验副本，本轮未重复搜索且未核查外部备份。未来只有真实页面交互自然形成不一致选择并触发可见提交，才可能补充S4页面请求/响应证据；若UI继续隔离，S4保持BLOCKED。trace-25仅可从来源可信的原始备份恢复为新副本并重做扫描，不得重建或覆盖原件。

当前结论：**ROOT_ACCEPTED_WITH_EXPLICIT_BOUNDARY**，依据[Root独立review](../20261016-dcc-24-e2e-coordination/e2e06-root-review.json)（2026-10-18 00:50）。接受3个真实流程及UI隔离不变量：新P2页与旧P1页保持独立，S2同页切换清空旧输入。跨项目HTTP拒绝未由真实UI触发；原S4 BLOCKED和PARTIAL_PASS_S4_BLOCKED机器字段作为历史口径保留，当前解释与Root结论见currentAssessment。

Root已独立补证DccCurrentFileRelationsTest **9项PASS**：严格CONTROLLED_FILE_RELATED_FILE_INVALID异常码、零relation insert，以及同项目ACTIVE精确A/1持久化正例，见[定向测试汇总](../20261016-dcc-24-e2e-coordination/e2e10-rework-matrix-test-summary.json)。这份后端证据独立于真实UI验收，不宣称页面HTTP拒绝PASS。本线程未运行该测试，原用例已停止，浏览器关闭。工程closeout因用户禁止Git提交推送而独立blocked；原失败、trace和自有数据保留，不扩大清理或重跑。

## 2026-10-19 R2 证据复核

本轮只执行本地只读取证与报告整理，没有新增业务动作，也没有重跑S1/S2/S3。命令`python doc/tasks/20261016-dcc-e2e-06/audit-traces-r2.py`实际exit0，见[独立核验结果](repair-20261019-r2-audit.json)和[交接](repair-20261019-r2-handoff.json)。这是证据检查通过，不能改变历史损坏、隐私扫描或S4的业务结论。

- trace-25.zip原件45,420,457字节，标准库仍报`BadZipFile: File is not a zip file`。原session记录tracing.stop缺失截图资源；对应playwright-artifacts-tyYNZa临时目录和本例trace-work均不存在。本任务61份ZIP已列入副本调查，没有其他trace-25同名档案，也没有与原件前64KiB相同的有效原始副本。已查来源内无法恢复；不推断外部备份不存在，没有重建ZIP或覆盖原件。
- trace-50.zip为97,852,696字节，**2558条目全量解压CRC PASS**，并与Root原review条目数一致。trace-25/50、session、S2、S4和旧privacy-scan共六份原证据SHA256前后相同。CRC只证明归档完整，不等于凭据扫描通过；历史完整隐私扫描仍为BLOCKED_UNREADABLE_ARCHIVE。
- UI边界沿用真实S4双tab及S2同页切换。静态复核限定在上传页和关联选择器：`DccFileSelector.vue`的`selectionContext/load/selectProject`只允许来源项目；`selector-state.ts`在reset/select/save检查项目身份；上传页`handleProjectCodeChange`关闭关联窗口并清空所选，`persistUploadRelations`再核对上下文和项目。没有发现本例正常入口遗漏；静态复核不冒充新页面验证，也不证明所有可能并发时序。未注入、绕过或新增入口。
- Root的DccCurrentFileRelationsTest 9项PASS仅作为独立后端边界：严格异常码、零insert及同项目合法A/1。跨项目HTTP拒绝未由真实UI触发，原S4 BLOCKED保留。无需构建/重启/运行态加载；本轮不等待Root排期。工程closeout独立blocked，资产继续保留。
- JSON/引用/原件哈希及五份历史评估结构断言exit0；本轮cleanup preview实际exit1，明确拒绝blocked状态，未apply。旧closeout-preview.json的exit3仅是10-18历史，不覆盖或混用。

## 分支结果

| 分支 | 结果 | 真实页面依据 |
|---|---|---|
| S1-P1-persist | PASS | P1目录、本项目搜索均可选A/1；搜索P2候选无结果。草稿#2054545668044084212固定关联表显示P1CAND/A/1，刷新及从受控浏览原行重开仍一致。 |
| S2-project-switch-reset | PASS | P1已选文件夹、正文、编号、关联后切P2，真实确认后全部清空。页面自然session-cleanup HTTP200，P2逻辑文件夹重新可选。 |
| S3-P2-persist | PASS | P2重新上传DRAFT2，目录和本项目搜索可选P2CAND/B/1，搜索P1候选无结果；草稿#2054545668044084213固定关联刷新、浏览列表重开仍为B/1。 |
| UI隔离不变量（已有S4双tab证据 + S2） | 已验证 | P2新tab不会改变旧P1页面及关联窗口；同页切P2会清空旧输入。Root已复核现有证据，无新增执行。 |
| S4-stale-cross-project-submit 原触发目标 | BLOCKED历史保留 | 跨项目服务拒绝未由UI触发；Root已单列后端拒绝/零insert定向证据，未将其写成真实UI跨项目HTTP提交PASS。 |

根目录result.json、fixture-manifest.json、prepared.json已同步，当前权威证据在runs/20261017-acceptance。10-16历史BLOCKED独立保留于runs/20261016-archive；旧中断checkpoint为历史，不再表示当前暂停。

## 运行身份及真实前置

- int_qms；前端8061 PID24564，后端48061 PID36320；Jar SHA256 `1e8d1c791ed7f6342c29ec44a4d3d50523a8cb8cc172b505c702b6920167feb0`，Root证明goal-restore-runtime-proof.json。
- 本例7账号、4专用权限角色、2部门、1岗位、基础存储目录913903、PUBLIC及APPLICATION_CUSTOM分类/类别、专用矩阵、模板、P1/P2项目均真实UI准备。项目申请COMPLETED，负责人正式初始化U，没有全局OWNER或管理员绕过。
- PUBLIC类别/矩阵保留且无候选提交；未改变共享岗位900332。按Root边界使用本例“其他”APPLICATION_CUSTOM类别，显式选择本例会签部门、A、D；下发/培训非必需，下发部门为空。
- P1候选#2054545668044084209，DCC241016C06P1CAND，A/1；P2候选#2054545668044084210，DCC241016C06P2CAND，B/1。每份均由独立context重新登录S主管签名、L签名指派C、C会签、A批准并指定U负责人、D文控签名。U详情回读受控（已生效）、ACTIVE及实际受控/生效时间，不以POST成功替代终态。
- 基础目录仅授权U查询/预览/下载、D查询/预览，未写派生叶子目录。所有配置、业务和签名由Playwright真实UI完成；无API/SQL、mock、存储注入或组件方法调用。

## 关键证据

以下文件位于runs/20261017-acceptance：

- trace-50.zip：U核对P2 ACTIVE及S1—S4关联动作；session-36320.json记录交互指令及自然请求path/method/status，先前准备/审批trace保留。
- candidate-P1-detail.png、candidate-P2-detail.png及各P1/P2-task/signature/assignment JSON：精确文件、任务、签名与终态。
- DRAFT1/2-relation-selected.png为选择截图；DRAFT1/2-fixed-relation.json为刷新、列表重开前后DOM；对应*-fixed-relation-panel.png已滚动至可见固定版本表并肉眼核对。
- S2-switch-confirmation.png、S2-after-switch-P2.png、S2-project-switch-reset.json：确认、清空及临时正文清理。
- S4-second-tab-P2.png、S4-old-tab-P1-context.png、S4-two-tab-boundary.json：双页可达性边界，crossProjectSubmitAttempted=false。
- evidence-consistency.json仅证明证据一致性，不新增E2E通过项。

未宣称逐步截图验收。trace在登录/改密/签名密码输入前停止，snapshots=false；网络仅路径/方法/状态，不读响应JSON、不保留请求头或payload。凭据仅在当前Windows用户DPAPI保护本地配置，不归档或提交。

## R0610-06 修复及验证

- 唯一生产修改为Root授予的XButton.vue，删除重复手调props.onClick一行，保留props声明及emit；Root已review冻结。未改XTextButton/LoginForm或增加全局防抖。
- 真实Vite挂载Vue/ElButton RED及离线SFC RED均exit1，@click/:onClick单击各2次；一行修复后离线GREEN exit0、5/5，事件对象一致、disabled/loading不触发、无pageerror或业务请求。x-button-red.json、x-button-offline-red.json、x-button-offline-green.json及对应trace保留。
- L普通单击登录恰好1次自然login；C/D首次改密均为初始login→改密→自动login一条链，DOM身份可见且无密码框。login-regression-DCC241016C06R17L/C/D.json及截图为证据。Root统一类型检查22:29 PASS（xbutton-types-process.json）；未重复已通过组件测试。

## 执行及未完成边界

- 实际命令node doc/tasks/20261016-dcc-e2e-06/ui06-r17.cjs，交互指令在session；最后显式stop、浏览器关闭、进程exit0。exit0仅表示执行器结束，不改变S4 BLOCKED。
- 两份执行器node --check exit0；finalize-evidence-r17.cjs exit0，断言两ACTIVE候选、每个4次审批签名及1次指派、3 PASS/1 BLOCKED、固定关联重开一致、恰好2次正式候选提交、正常stop和关键证据存在。
- 原脚本失败保留：全局浏览超时后改从实际项目/文件夹范围查找；双页属性标题exact不符；切换文件类型弹出真实确认后明确确认继续。未重复提交，不把这些定位/等待问题记产品FAIL。此前trace临时文件丢失亦保留，没有盲重试业务。
- S4服务保护独立证据由Root执行并review：DccCurrentFileRelationsTest 9项，errors/failures/skipped均0，严格异常码/零insert及同项目ACTIVE精确A/1正例通过。本线程仅读取Root review和测试汇总，没有运行Maven或将后端测试算作UI HTTP拒绝验证。
- SWITCH临时正文已由真实项目切换清理。两个ACTIVE候选、两个工作草稿及本例配置按Root review要求保留；manifest.cleanup为RETAINED_FOR_ROOT_REVIEW/completed=false，精确ID齐全。未动其他例/共享资产，未操作服务、DB、Git提交推送或子Agent。
- 最终隐私扫描exit1、BLOCKED_UNREADABLE_ARCHIVE：150个可读文本条目凭据命中0、原始network条目0；历史trace-25.zip缺中央目录无法读取，原件保留。当前业务trace-50.zip可读，未将历史损坏文件或旧PASS55冒充当前完整扫描通过。
- cleanup preview exit3，准确拒绝blocked状态，证据closeout-preview.json；未执行cleanup apply，不标completed。DPAPI配置已由本例run内.gitignore精确排除，未改全局Git规则或提交资产。
