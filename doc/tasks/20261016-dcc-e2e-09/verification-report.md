> R4已授权单次只读复验结束：编号请求HTTP200/226ms完成，20秒目标行FAIL；脚本WORKING/global与固定ACTIVE文件不匹配，不判产品超时。未重复搜索，原R3超时未复现/未修复、0PASS/4BLOCKED不变。业务写0、全部会话关闭。[报告](readonly-search-r4/report.md) / [交接](readonly-search-r4/handoff.json)。

> R3 阻塞离线诊断完成：按钮点击前等待最新自动查询；初始化与tab查询交叠有源码/时间证据，后端根因未知。未启动浏览器，设计只读复验待Root。[诊断](diagnosis-20261020-r3/diagnosis.md) / [交接](diagnosis-20261020-r3/handoff.json)。原0PASS/4BLOCKED不变。

> 2026-10-20 R3 第二轮最新：业务 BLOCKED（0 PASS / 4 BLOCKED），关联搜索 loading 20秒且首错时自然 GET待完成17.313秒。两角色fresh身份确认，未保存草稿或改配置；证据完整性13项通过但不是业务PASS，全部会话关闭并HOLD。[本轮报告](rerun-20261020-r3-round2/report.md) / [handoff](rerun-20261020-r3-round2/handoff.json)。下列旧轮结果仅为历史。

> 2026-10-20 R2 第二轮最新结果：四分支最终断言 PASS；原始 FAIL 与 D0 单次加载门禁校准 PASS 分开保留。28 项证据 QA 通过，会话全部关闭并 HOLD。详见 [本轮报告](rerun-20261020-r2-round2/report.md) 与 [handoff.json](rerun-20261020-r2-round2/handoff.json)。以下历史记录保留。

# E2E-09 草稿恢复验证报告

## 2026-10-20 第二轮本次结果

全新四分支真实Playwright重跑PASS，4/4，无遗漏，待Root复核。新文件ID为2054545668044084292、2054545668044084302、2054545668044084303、2054545668044084307。9份脱敏trace、15张截图、24项证据核验及关闭回执已保存；context/browser/runner全关闭、runner退出0。所有默认恢复，未改生产代码或公共配置。

本次依据为[本次报告](rerun-20261020-round2-report.md)及[本次交接](rerun-20261020-round2-handoff.json)。外部图标8条超时和一次脚本文件名定位校准均留痕。下方Root接受仅为上一轮历史结论，不替代本次执行；本次工程收尾仍blocked，未提交推送或cleanup apply。

## 以下为2026-10-19整理的历史验收报告

当前业务状态：BUSINESS_ROOT_ACCEPTED。

Root已完成业务review并接受全部四个参数分支；当前结论以[Root验收记录](../20261016-dcc-24-e2e-coordination/e2e09-root-review.json)和[business-results.json](business-results.json)为准。下方旧暂停、未执行及BLOCKED内容全部为历史记录，不代表当前状态。

## 当前验收结果

|子分支|实际文件ID|结果|
|---|---|---|
|C09-I0：立即生效、无需培训、手改属性|2054545668044084193|ROOT_ACCEPTED|
|C09-I1：立即生效、需要培训、沿用旧默认|2054545668044084201|ROOT_ACCEPTED|
|C09-D0：延迟生效、无需培训、条件属性|2054545668044084202|ROOT_ACCEPTED|
|C09-D1：延迟生效、需要培训、沿用旧默认|2054545668044084203|ROOT_ACCEPTED|

四分支均通过真实页面保存草稿、修改本例项目默认值、fresh新申请读取新默认作对照，再从受控浏览的实际文件行重开精确ID；保存前后属性、独立会签/下发名单、生效模式、培训、批准/文控选择逐值一致，两个附件及固定关联编号、文件名、A/1均正确保留。每分支结束后项目默认值恢复并回读。

两份延迟草稿均显示“延迟生效时间：尚未确认”，上传和草稿日期输入为0。作者三个临时项目菜单已撤回，fresh登录确认无项目菜单。Root已核验16份trace ZIP完整性；runner退出码0，自有浏览器已关闭。

历史验收运行态（2026-10-17）：int_qms，8061/48061，后端PID36320，Jar SHA256 `1e8d1c791ed7f6342c29ec44a4d3d50523a8cb8cc172b505c702b6920167feb0`，证明见协调目录goal-restore-runtime-proof.json。仅对应本例当次验收；未核验2026-10-19运行态，不证明13/22/24新修复已加载。

工程收尾与业务验收分开：Git提交/推送未授权，未执行；测试资产和全部Root审阅证据保留，未执行cleanup删除。停止新增E2E、业务写入和测试，本例业务范围不扩展到正文权限、培训执行或生效流程。

## 2026-10-19 R2 文档修正与历史替代依据

本轮仅修正过时状态及证据指向，不重跑E2E、不改生产代码、不启动浏览器。当前业务BUSINESS_ROOT_ACCEPTED；工程blocked：用户禁止Git提交/推送，未执行cleanup apply，未标completed。文档整理不产生新的业务PASS。

|历史时点与旧结论|替代证据与有效结论|
|---|---|
|2026-10-16账号准备及2026-10-17中断：fixture未完成、关联待批准、四分支BLOCKED|business-results.json，completedAt=2026-10-17T15:50:29.928Z：关联已生效、四分支PASS；随后e2e09-root-review.json四分支ROOT_ACCEPTED。Root文件无独立验收时间字段，不补造时间。|
|2026-10-17部署前：R0610-05回归尚未执行，恢复计划仍待确认|missing-post-message-result.json：作者/主管缺岗位两分支PASS、岗位均恢复回读、路线4节点ready；execution-log.md同日已记实际执行，旧runtime-regression-plan.md标为已执行历史计划。|
|2026-10-17准备：dcc_project_reviewer绑定待撤回|project-reviewer-withdrawal-ledger.json，completedAt=2026-10-17T16:16:00.127Z：唯一保存写入，重开确认角色为空、账号及历史保留；替代此前报告误写的2026-10-18日期。|
|准备/中断快照prepared.json、execution-state.json、runtime-pause-20261017.json、interruption-checkpoint-20261017-2205.json|保留原始字段作历史，不用旧runtimeReady/businessFixtureReady/paused覆盖Root有效接受，也不据此重新启动。|

本轮BDD/RED/GREEN与结构验证命令见execution-log.md；原JSON、trace及截图均保留。文档验证交接为[repair-20261019-r2-handoff.json](repair-20261019-r2-handoff.json)，与Root业务验收分开。

结构验证：`python -X utf8 doc/tasks/20261016-dcc-e2e-09/verify-repair-20261019-r2.py`，修正前exit 1（六项旧状态/历史标注缺失），修正后exit 0（20项PASS）；7份原始证据SHA256不变。结果为文档GREEN，不构成新E2E或当前运行态验证；无构建/运行窗口需求。

## 精确角色绑定收口

按账本completedAt=2026-10-17T16:16:00.127Z（2026-10-19 R2纠正原日期误写），已通过真实用户角色页，仅撤回DCC241016C09REVIEWER（账号910448）的dcc_project_reviewer绑定。保存后重新打开同一账号角色弹窗，角色由“项目审核员”变为空；只产生1次`/system/permission/assign-user-role`保存请求，runner退出码0，新浏览器已关闭。账号、项目及全部历史审批保留，公共角色定义未修改，四分支业务接受结论不变且未重跑。

证据：[project-reviewer-withdrawal-ledger.json](project-reviewer-withdrawal-ledger.json)及其中列明的保存前、重开回读截图和脱敏trace。manifest已仅移除该条残留绑定，其余测试资产继续保留。

## 历史执行记录

以下原文仅保留验收前的环境中断、前置缺口、旧脚本阻塞和恢复过程，用于追溯；所有“尚未”“未执行”“暂停”“BLOCKED”均为当时快照，已由上方BUSINESS_ROOT_ACCEPTED结论覆盖。

历史暂停状态快照（现已解除）：2026-10-17 22:05 Root确认运行环境中断，本例暂停。R0610-05两个缺岗位提示分支已真实PASS且岗位恢复；关联候选已实际推进到待批准。完整草稿恢复尚未执行，四个参数分支仍为BLOCKED，不能当业务PASS。

### 历史：22:05中断恢复记录

仅整理中断前已存证据，没有启动浏览器或新写入。原文件2054545668044084187/DCC241016C09-REL/A1，BPM 11684100-ca2d-11f1-aa13-b082e25ec548：主管签名、部门负责人签名指派本人、实际会签签名均有真实页面回执。21:54:24刷新待办后明确显示“批准并确认文件负责人/待批准”，该刷新已记录在ui-journal.jsonl末条，因此恢复时不能重复前述动作或再次上传。

批准签名、文控签名、ACTIVE终态均未执行或未确认；四个恢复草稿尚未创建。原岗位22已在负例后真实恢复，项目297、账号及其余夹具保留。恢复后首先fresh登录CONFIG，按原编号打开当前待办和签名历史，再只办理尚未完成的节点。

当前task runner不存在；进程中断导致最后活跃trace段未正常导出，不能声称中断前全段trace已保全。既有脱敏trace、签名回执、截图和journal仍保留。精确恢复点见interruption-checkpoint-20261017-2205.json。

### 历史：2026-10-17 运行与准备历史快照

- 本轮实际基线：int_qms，8061/48061；Root证明PID40784、Jar SHA6379718091162ce1e80a1d7a5a08af8a3c65483285612f11d9cd36a55b232d35。两原账号在全新context登录及原页面入口均PASS，非业务PASS。
- 本例项目297/DCC241016C09P1及产品DCC241016C09PROD，由配置员提交、独立新账号910448/DCC241016C09REVIEWER审核至COMPLETED。该账号仅新增既有精确dcc_project_reviewer绑定，未修改公共角色定义或全局审核配置；恢复计划是撤回这条本例账号绑定。正式申请模式及唯一角色选项已核对；分类列表未展示未分类正式角色的情况与配置包下载事件超时留在操作日志，不伪称已从该列表回读其全部菜单。
- 新账号910448已本人首次改密并独立登录；目录模板14/DCC241016C09Folders与项目文件夹、两专属部门、岗位22/DCC241016C09POST均已创建并回读。作者910401直属主管为910402，三本例账号均绑定本例会签部和岗位22。
- 本例五级分类、类别DCC241016C09CAT、存储目录DCC241016C09DIR已真实保存并回读。配置员含正式目录查询权限；目录没有重复创建。类别选择APPLICATION_CUSTOM，所有本次选择只随申请保存，E2E-09不要求覆盖公共矩阵变化，不改任何公共矩阵。完整四分支仍覆盖实际属性/名单/生效/附件/固定关联，不以类别来源替代业务断言。
- 作者的本例类别上传/查看两条权限已保存重开；配置员签名授权已启用，本人上传测试账号专属签名图片V1、900×240、已启用。该图片明确标注测试模拟身份，不代表现实人员签名。
- 尚未上传任何正文或附件，尚无草稿或文件审批申请。本轮首次表单已配置人员和名单，未提交。关联候选仍需本例自己的ACTIVE文件，不能借01–05或03资产。

### 历史：新缺项与Root处理

20:32作者真实页面出现“系统异常”。Root与本例从当前运行日志核对NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED，原校验同时要求系统岗位非空；精确账号为作者910401和直接主管910402，最初两人无岗位。已通过真实用户编辑补本例岗位22，独立重登后预检变为明确的签名授权/图片缺项，随后已完成这两项准备。

Root负责R0610-05错误映射最小修复和50项定向回归，本例没有改生产代码。部署后需按runtime-regression-plan.md暂清主管本例岗位验证中文原因，再恢复回读；该回归尚未执行，不能标GREEN。

runtime-pause-20261017.json已写paused:true和最后动作、对象、恢复点；全部自有浏览器关闭，测试资产和登录后脱敏trace保留。没有为暂停而提交业务，也没有清理或重建测试数据。

### 历史：登录诊断及纠正

- 对照scripts/preflight/login-preflight.mjs修正为真实click展开租户、fill搜索、等待可见option后click；选中后断言可见标签，不能断言被清空的搜索框值。点击登录后等待导航及真实账号显示名。
- 18:39重验已选中“芋道源码”，但自然租户请求HTTP500、页面提示“租户识别失败”；当时48061未就绪，Root随后确认环境曾停止。这是运行态前置阻塞，不是产品RED。
- 18:43环境恢复后两个全新浏览器context均真实登录成功到/index；18:44再次登录并完成角色只读盘点。证据：evidence/login-2026-10-16T10-43-01-764Z/result.json、login-diagnosis-result.json。
- 登录辅助函数已抽出login-ui.cjs；account-setup-real.cjs与draft-recovery.e2e.cjs复用同一流程，无认证响应JSON断言、API登录或localStorage注入。

### 历史：已确认对象及报告纠正

旧account-setup-result.json的accounts={}表示此前没有创建用户。原“可能创建了角色/账号”描述不准确：只存在已在页面确认的作者角色991236。历史原文件保留在evidence/account-setup-result-before-login-fix.json。

本轮解除登录阻塞后，真实页面完成以下准备：

|对象|标识|实际状态|
|---|---|---|
|作者角色|991236 / DCC241016C09AUTHOR|复用本例先前角色；只配置并回读用户查询、部门查询、文件上传、受控浏览|
|配置员角色|991289 / DCC241016C09CONFIG|本轮创建；配置项目查询/编辑与用户查询；完整菜单投影待重启后独立登录核对|
|作者账号|910401 / DCC241016C09AUTHOR|本轮真实页面创建、绑定作者角色；本人独立context首次改密并登录到/index|
|配置员账号|910402 / DCC241016C09CONFIG|本轮真实页面创建、绑定配置员角色；本人独立context首次改密并登录到/index|
|项目/文件/草稿|无|尚未创建，业务fixture未完成|

两个账号尚未配置部门/主管/审批组织；没有使用管理员充当业务角色，没有修改共享角色/菜单定义或已有基线账号密码。配置员项目查询父节点仍需结合真实菜单完整回读，不能以点击过替代核验。

fixture-manifest.json记录精确对象，verifiedThroughRealUI=false、businessFixtureReady=false。密码保存在进程与Windows当前用户DPAPI保护的本地临时配置，未写入报告、截图或trace；本轮保留5张登录/配置完成后的页面截图和自然请求path/method/status。登录、创建密码和强制改密期间未启动trace；完整业务trace尚未产生。

### 历史：定向静态/组件验证

|命令|结果|
|---|---|
|node IntRuoyiFronted/tests/e2e/login-default-credentials-static.spec.mjs|PASS，exit 0|
|node IntRuoyiFronted/tests/e2e/login-auth-error-message-static.spec.js|PASS，exit 0|
|node IntRuoyiFronted/tests/e2e/dcc-project-attributes-unit.spec.js|PASS，exit 0；三申请属性、冻结草稿恢复和校验|
|node IntRuoyiFronted/tests/e2e/dcc-project-attributes-race-unit.spec.js|PASS，exit 0；六项异步覆盖/失败保留场景|

上述结果不替代真实草稿保存/重开E2E。未修改生产代码，未执行Maven、完整构建、迁移、Git提交或服务启停。

本例所有CJS语法检查、JSON结构与5张证据文件存在性检查通过；受保护凭据可由当前Windows用户恢复，扫描32个任务资产未发现两账号明文密码。

维护窗口只继续脚本review：修正菜单展开、关联确认、培训复选框、配置弹窗回读、批准人可见姓名比较和登录失败context关闭；账号核验同时汇总HTTP及pageerror。`trace-sanitizer-unit.py`的8项安全工具检查PASS，结果标注SECURITY_UTILITY_UNIT_ONLY，不属于业务E2E。真实trace尚未生成；这些业务选择器仍需环境恢复后的真实DOM核验。

### 历史：参数分支及恢复点

|子例|范围|结果|
|---|---|---|
|C09-I0|立即生效、无需培训、手改实际属性|BLOCKED，业务未执行|
|C09-I1|立即生效、需要培训、沿用旧默认|BLOCKED，业务未执行|
|C09-D0|延迟生效、无需培训、条件属性|BLOCKED，业务未执行|
|C09-D1|延迟生效、需要培训、沿用旧默认|BLOCKED，业务未执行|

Root准备部署用户部门清空修复后要求暂停，本例已保存manifest、停止写入并关闭全部自有浏览器。execution-state.json阻止误启动；健康确认后恢复原线程并运行account-setup-real.cjs，先在改密后的两个全新context核对账号/菜单/首次表单，再补任务自有组织、项目、类型/专用矩阵、关联版本及文件。不得等待共享roleFixtureVerified，也不得重复创建已有两角色/两账号。

cleanupAttempted=false，cleanupCompleted=false；残留为上述两角色、两用户，详见fixture-manifest.json。尚无项目或草稿需要清理。最终清理及Root review未完成，任务不标记completed。

## R4 前置离线修正验证
RED历史runner exit1；GREEN校准副本7/7；node --check两脚本PASS；原runner/R3结果/R4结果与首错SHA256未变。仅STATIC_PASS，不是E2E。详见prerequisite-repair-r4/report.md和verification.json。ready_for_closeout，等待Root review；未授权Git写。

## R5 original R3 path once
Real fresh-author UI upload menu/project/folder PASS; relation disabled/no preview => immediate BLOCKED. Search0, actual non-auth mutation0. Raw counter16 incorrectly included GETs; original result retained, correction and executed runner preserved. 498 trace events, CRC/credential/closure QA PASS; PID4980 absent, external exit0. No original-R3 search request, no new backend timing conclusion. Details r3-path-once-r5/report.md; ready_for_closeout, HOLD Root review.
