# E2E-09 草稿恢复

## Goal

独立验证E2E-09，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R5原R3真实上传入口因缺会话内上传预览而关联禁用，按指令立即停止；搜索0/实际业务变更请求0，trace及关闭QA通过。已ready_for_closeout、cleanup preview exit0；不apply/Git写，HOLD待Root review。

## R5 原R3路径一次真实UI核验

- Given Root批准一次原R3触发路径、当前生效关联文件和20秒等待；When fresh作者从文件上传菜单选择本例项目/文件夹并检查关联入口；Then 前置可用才打开同一关联选择器、仅一次固定编号搜索；前置不可用立即BLOCKED，不改用受控浏览、不新建上传预览/草稿、不跑其他分支。
- 原R3已关闭的会话与预览不复用；当前指令未单独放开此前禁止的重复上传写入。选择器自身只取ACTIVE，受控浏览中的版本选择控件不属于这个入口。
- 验证：真实UI DOM、自然请求唯一ID及白名单参数、首错截图、完整动作trace（去认证/网络payload）、CRC及关闭回执；保留原20秒，不增加搜索次数。
- 里程碑：运行态/来源 → fresh登录/真实原路径前置 → 搜索或即时BLOCKED → trace/关闭 → HOLD。

## R4 前置修正（离线，等待 Root review）

- Given R4 页面证据中目标是当前已生效受控版本，原脚本错误使用工作小版本/全局范围；When 校准待复验脚本；Then 使用当前生效受控版本、显式选择本例项目和当前项目页签，初始化结束后仅一次固定编号搜索，保留20秒等待。
- Given R3 最新自然请求无终态；When 离线检查时间线和源码；Then 不将 R4 前置修正宣称为 R3 产品修复，不猜测后端耗时根因。
- 里程碑：静态合同 RED → 最小独立脚本修正 → GREEN/语法及历史指纹核验 → HOLD。
- 验证范围：只运行离线静态检查，不执行脚本、不启动浏览器、不重复业务写入、不改产品或原始结果、不提交推送。

## R4 单次只读复验 BDD

- Given 当前Root R4 PID38176与原作者fresh身份、已有固定关联编号；When 从正式受控浏览菜单单次搜索DCC241016C09-REL；Then 分请求ID记录白名单参数及自然状态、20秒DOM结果/原首错，网络另限15秒被动观察，禁止重复搜索或任何业务写。
- 里程碑：运行门禁→fresh登录→一次搜索→脱敏CRC及关闭→交接HOLD。

## R3 阻塞诊断 Given/When/Then

- Given 原始R3 trace、首错、自然事件与当前直接组件；When 离线对齐tab/fill/等待及请求生命周期；Then 区分实际未完成请求、初始化交叠和locator失配，保留无法确认的后端根因，不用延长timeout猜修复。仅本例诊断资产，无浏览器/业务写入。
- 里程碑：证据指纹 → 时间线/源码链路 → 最小方案及一次只读复验设计 → 结构核验/HOLD。

## 2026-10-20 R3 第二轮 BDD / 里程碑

- Given int_qms 8061/48061、R3 当前运行来源快照与原作者/配置员独立 fresh 登录；When 四种立即/延迟与培训组合分别创建本轮新草稿，修改本例项目默认、fresh 作者新申请作对照，再从正式工作小版本浏览入口重开精确草稿；Then 保存属性及条件值、独立名单/人员、生效/培训、源文件/两附件/固定关联一致，延迟日期尚未确认，配置恢复并回读。
- 里程碑：规则及当前快照门禁 → 本人真实登录可用性 → 四分支完整执行或准确前置阻塞 → 脱敏/CRC/关闭核验 → 正式 handoff.json、HOLD。
- 约束：只验证收集，只维护本例资产；不改产品/DB/共享配置、不 build/restart/Git写/新线程/子Agent；请求超时保留首错，不猜根因；写入结果未知不重复点击，仅一次原对象只读核验仍无终态即停止。脚本等真实加载遮罩消失，不复用上轮PASS。请求模型gpt-6.1-sol/low取代旧high。

## 2026-10-20 R2 第二轮 BDD 与里程碑

- Given int_qms 8061/48061 新运行态（前端20976/后端41508）及原作者、配置员独立 fresh 登录；When 四种生效/培训组合新建草稿，配置员修改专属项目默认，fresh 作者对照新默认后从正式浏览重开原精确草稿；Then 原属性、名单、人员、模式、培训、两附件及固定关联均保持，延迟日期尚未确认，每分支恢复项目默认并回读。
- 里程碑：本轮 execute/runtimeReady 门禁 → 四分支真实操作和首次失败留证 → 独立证据 QA → context/browser/runner 关闭回执与 handoff.json → HOLD。
- 设计约束：源 Jar SHA256 709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147；引用协调目录 rerun-r2-runtime-proof.json；只新建本轮草稿，不重复第一轮终态；不解析业务响应 JSON、不用 API/DB 承担动作。产品代码未改，产品 TDD 不适用；脚本保留语法/断言自检。

## 本次重跑BDD及里程碑（2026-10-20）

- Given 当前int_qms运行证明及作者/配置员本人fresh登录，使用本轮唯一文件名和新申请；When 四种生效/培训及属性组合实际保存，再修改本例项目默认、fresh新申请作对照，从正式浏览重开精确新草稿；Then 保存属性、独立会签/下发、人员、生效/培训、两附件及固定关联全部保持，延迟日期仍尚未确认；每分支默认值恢复并回读。
- 里程碑：规则/运行/本人登录核验 → 四分支逐项真实执行 → 独立结果/trace/截图及问题归档 → 浏览器/context/runner关闭回执。失败与前置阻塞分别记录，继续其余可执行分支，不重复提交旧终态。
- 预期验证：新DOM断言、自然请求method/path/status（不解析JSON）、截图及脱敏trace；源文件/附件本轮唯一。未改生产代码，产品TDD不适用；必要定位校准不改变业务预期。运行证明仅引用20261020-qms-e2e24-runtime-update/runtime-proof.json，预期PID20976/27908，8061/48061。

## 历史：2026-10-19文档修正收尾状态

blocked — 业务BUSINESS_ROOT_ACCEPTED，四个分支均已由Root验收；仅工程收尾未完成：R2文档结构GREEN后已进入ready_for_closeout，复核收尾规则后因本轮禁止Git提交/推送而记录blocked，未执行cleanup apply，不标completed。2026-10-19 R2仅修正文档一致性，不重跑已通过E2E、不改生产代码或业务数据。当前依据为[Root验收记录](../20261016-dcc-24-e2e-coordination/e2e09-root-review.json)及[business-results.json](business-results.json)，不再等待旧环境中断放行。

## 2026-10-19 R2 范围与历史替代

- 原22:05环境中断、关联待批准、四分支未执行记录保留为历史，已由2026-10-17完成的business-results.json及随后Root验收替代；不作为当前恢复入口。
- 原R0610-05“回归尚未执行”计划已由missing-post-message-result.json的两项真实页面PASS、岗位恢复回读和路线ready替代，不重跑。
- 原“项目审核员绑定待撤回”已由project-reviewer-withdrawal-ledger.json的保存及重开回读PASS替代；其completedAt为2026-10-17T16:16:00.127Z，保留账号及全部审批历史。
- prepared.json、execution-state.json、runtime-pause-20261017.json和interruption-checkpoint-20261017-2205.json均为准备/恢复阶段历史快照；保留原字段及原文件，不据其启动脚本或覆盖Root验收。旧PID/Jar只证明当次历史验收，不证明2026-10-19运行态或13/22/24新修复。
- 本轮预期验证：文档结构、引用存在性、四个分支ID/结果一致、原证据指纹不变；交接repair-20261019-r2-handoff.json。无需Maven、类型检查、构建或运行窗口。

## BDD

- Given 已保存实际属性、名单、关联、生效模式及附件；随后通过配置页面修改项目默认值
- When 关闭后重新进入原草稿
- Then 恢复原草稿实际保存值及生效方式，不被新默认属性覆盖；附件及关联仍对应原申请；延迟日期仍在后续确认任务填写。

## Milestones

1. [x] 用例参数、脚本及本例自有业务fixture完成；关联文件2054545668044084187/A1已生效。
2. [x] 四个草稿分支C09-I0/I1/D0/D1真实页面PASS，原文件ID、保存值、附件及固定关联恢复一致。
3. [x] Root review四分支ROOT_ACCEPTED，核验16份trace；项目默认值、临时作者菜单及项目审核员绑定均已有恢复证据。
4. [ ] 工程收尾：未授权Git提交/推送；证据及业务夹具保留，未执行cleanup apply。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C09，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-09/r3-path-once-r5.cjs
- doc/tasks/20261016-dcc-e2e-09/r3-path-once-r5


- doc/tasks/20261016-dcc-e2e-09/readonly-search-r4-calibrated.cjs
- doc/tasks/20261016-dcc-e2e-09/prerequisite-repair-r4/verify.cjs
- doc/tasks/20261016-dcc-e2e-09/prerequisite-repair-r4/verification.json
- doc/tasks/20261016-dcc-e2e-09/prerequisite-repair-r4/report.md
- doc/tasks/20261016-dcc-e2e-09/prerequisite-repair-r4/handoff.json


- doc/tasks/20261016-dcc-e2e-09/verify-readonly-search-r4.cjs

- doc/tasks/20261016-dcc-e2e-09/readonly-search-r4.cjs

- doc/tasks/20261016-dcc-e2e-09/verify-rerun-20261020-r3-round2.cjs

- doc/tasks/20261016-dcc-e2e-09/rerun-20261020-r3-round2.cjs

- doc/tasks/20261016-dcc-e2e-09/rerun-20261020-r2-round2-calibration.cjs
- doc/tasks/20261016-dcc-e2e-09/verify-rerun-20261020-r2-round2.cjs

- doc/tasks/20261016-dcc-e2e-09/rerun-20261020-r2-round2.cjs

- doc/tasks/20261016-dcc-e2e-09/task.md
- doc/tasks/20261016-dcc-e2e-09/execution-log.md
- doc/tasks/20261016-dcc-e2e-09/verification-report.md
- doc/tasks/20261016-dcc-e2e-09/prepared.json
- doc/tasks/20261016-dcc-e2e-09/draft-recovery.e2e.cjs
- doc/tasks/20261016-dcc-e2e-09/fixture.example.json
- doc/tasks/20261016-dcc-e2e-09/.gitignore
- doc/tasks/20261016-dcc-e2e-09/login-ui.cjs
- doc/tasks/20261016-dcc-e2e-09/login-diagnosis-real.cjs
- doc/tasks/20261016-dcc-e2e-09/login-diagnosis-result.json
- doc/tasks/20261016-dcc-e2e-09/account-setup-real.cjs
- doc/tasks/20261016-dcc-e2e-09/account-setup-result.json
- doc/tasks/20261016-dcc-e2e-09/secure-credentials.cjs
- doc/tasks/20261016-dcc-e2e-09/ui-session.cjs
- doc/tasks/20261016-dcc-e2e-09/pause-save.cjs
- doc/tasks/20261016-dcc-e2e-09/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-09/execution-state.json
- doc/tasks/20261016-dcc-e2e-09/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-09/trace-sanitizer-unit.py
- doc/tasks/20261016-dcc-e2e-09/runtime-regression-plan.md
- doc/tasks/20261016-dcc-e2e-09/verify-repair-20261019-r2.py
- doc/tasks/20261016-dcc-e2e-09/repair-20261019-r2-red.json
- doc/tasks/20261016-dcc-e2e-09/repair-20261019-r2-green.json
- doc/tasks/20261016-dcc-e2e-09/repair-20261019-r2-handoff.json

## 历史：2026-10-17 22:05中断时状态（已替代）

以下保留原状态原文；替代依据见上方2026-10-19 R2范围及verification-report.md时间表。

blocked — Root 22:05确认Docker/8061/48061环境中断，本例停止浏览器操作，已保存interruption-checkpoint-20261017-2205.json。原关联候选2054545668044084187/A1在21:54真实刷新待办后已进入“批准并确认文件负责人/待批准”；主管、指派和会签不可重复。四个草稿分支尚未执行；等待Root健康放行后先回读原任务和签名历史，不重建任何已存资产。

当时里程碑：参数/账号及脚本准备已完成，完整fixture未完成；真实四分支及Root review均未勾选。此后完成情况已由当前Milestones替代。

## 以下为2026-10-16至2026-10-17准备阶段历史记录

以下“当前”“尚未”“待重启”“BLOCKED”等仅指原记录时点，不是本例当前执行指令。fixture完整性、业务及恢复结果以Root验收和具体结果账本为准，原记录保留便于审计。

## 第一阶段设计与参数（2026-10-16）

当前分支已核对为int_qms；基线f06833f06c868dc3a451eb63d01381a467ecfe92。仅编写本例测试资产；不改生产代码，生产代码RED/GREEN不适用。脚本语法、源入口及安全结构检查仅是准备证据。

|子用例|保存生效模式|保存培训|属性与新默认的差异|共同断言|
|---|---|---|---|---|
|C09-I0|文控审核后立即生效|不需要|实际值显式修改|重开保持实际属性、模式、名单、关联固定版本、两个附件及原文件身份|
|C09-I1|文控审核后立即生效|需要|实际值沿用旧默认|同上；不能被后来项目新默认覆盖|
|C09-D0|延迟生效|不需要|实际值显式修改，含其他市场/转移条件字段|同上；没有上传/草稿日期输入，延迟确认日期尚未填写|
|C09-D1|延迟生效|需要|实际值沿用旧默认|同上；保存及重开不提前要求具体日期|

“三个属性”按正式组件核对为目标市场、身份（注册人/生产方两个值）、文件转移三个组；其他市场说明、转移至也要保存和回显。各用例以自己的实际选值作为oracle，不按新默认推断。审批路线只读显示的批准人也纳入前后快照；本例不提交审批，不替代E2E-15/16后续确认任务验收，不推断缺失的第2尾部/6/8规则。

## 正式页面及角色前置

- 文件上传：动态菜单“文件上传”，路由/dcc/controlled-file/upload，组件IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue，按钮“保存申请草稿”；保存后应由页面导航进入/dcc/controlled-file/detail/{id}管理态。
- 重开：动态菜单“受控浏览”，/dcc/controlled-file/browser，版本浏览范围“工作小版本”，按自有编号搜索目标行，行内“操作面板”；进入同一管理详情，点击“准备本次申请”，组件detail/DetailApplicationPanel.vue。不能直接拼详情地址，也不能用新建上传替代恢复。
- 修改默认：全局“基础数据”>“DCC项目代码”，/mdm/project-code，目标行“详情”，抽屉“DCC基础条目”中的“修改项目默认属性与负责人账号”；弹窗“项目默认属性与负责人账号”。不修改负责人、不修改公共矩阵。
- U需精确权限dcc:controlled-file:submit、dcc:controlled-file:query，目标自有项目/目录范围可见，真实账号及组织关系有效。配置人员另需dcc:project-code:query、dcc:project-code:update及用户目录查询能力。角色code及候选真实账号尚未从已授权页面确认，明确列为缺口，禁止猜测、用admin充当这些角色或扩权。
- 本例自行以真实页面准备DCC241016C09专属项目、文件夹/正式产品关系、类型/专用矩阵、组织、项目内ACTIVE关联版本。以上尚未完成；不得将账号准备等同业务fixture通过。
- fixture-manifest.json记录已有真实对象，verifiedThroughRealUI和businessFixtureReady保持false，独立重新登录/菜单及业务范围未核验前不得改为true。凭据只在环境变量或Windows当前用户DPAPI保护的本例临时配置中处理，该临时文件已忽略，不作为报告/提交证据。
- 最新运行态按Root两次通知区分：恢复后已真实登录成功；随后Root准备部署用户部门清空修复，当前按通知关闭浏览器。此前18:39租户识别自然请求HTTP500属于环境未恢复，不能算产品RED；不自行启停服务。

## 本轮登录纠正与真实对象

- 旧登录错误：未先展开租户控件；点击选项后检查搜索框仍有值；点击登录后立即检查URL。已改为官方预检的click/fill/等待可见option/click，断言已选标签，等待页面导航与可见账号名。不读取认证响应JSON。
- 18:43两个全新context管理员登录成功到/index；18:44重新登录并在角色列表确认为991236/DCC241016C09AUTHOR，分配人数0。旧accounts={}记录确实没有创建用户，不能泛称创建了账号。
- 本轮随后通过真实页面创建角色991289/DCC241016C09CONFIG，创建账号910401/DCC241016C09AUTHOR与910402/DCC241016C09CONFIG，分别绑定自己角色，并完成本人首次强制改密后到/index。
- 作者权限已回读4项：1001用户查询、1017部门查询、6806文件上传、6807受控浏览。配置员已回读用户查询和DCC项目代码编辑子项；990210项目查询是父节点，回读显示与实际菜单需在重启后继续核对，不能只据点击过就判为完整通过。
- 当前用户无部门/主管/审批组织配置，未授管理员，未修改已有基线用户密码。首次密码输入未开启trace；当前留存5张改密完成后的截图与自然请求path/method/status。
- 四个子例C09-I0/C09-I1/C09-D0/C09-D1均BLOCKED_NOT_EXECUTED；原因是业务fixture未完成且Root要求部署暂停，非产品FAIL。
- 维护窗口继续脚本review：修正菜单父项误折叠、关联按钮“确认关联”、培训复选框、配置嵌套弹窗回读、批准人姓名规范比较及登录失败context关闭。trace在登录后开启，结束后删除网络体/认证字段/非图像资源、去URL查询参数并删除原始包；脱敏失败记录证据失败，不称已产出业务trace。上述选择器尚待环境恢复后的真实业务DOM验证。

## 数据恢复与证据保留

- 默认配置在成功/失败退出时尝试通过同一配置页面恢复，并重开配置弹窗逐字段回读；只操作manifest中明确归属本例的项目。
- 四份草稿及源件/附件是待Root review证据，不自动删除；记录cleanupAttempted、cleanupCompleted、residual。正式草稿删除入口尚未确认，删除与临时票据清理交Root统一收尾，不能写成已清理。
- trace在账号密码登录完成后才开启，不保存storageState，不记录请求体/响应JSON/token/cookie，登录阶段不截图。证据仅记录自然请求method/path/status及可见字段。
