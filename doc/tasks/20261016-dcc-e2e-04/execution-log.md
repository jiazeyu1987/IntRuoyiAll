# Execution Log

- 2026-10-16：确认实际Git分支 `int_qms`。已读取本例 `task.md`、24例协调目录 `thread-contract.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md` 和 `docs/branch-runtime-ports.md`。本例 dispatch 为 `prepare_only`，environment `runtimeVerified=false`、`roleFixtureVerified=false`；未启动服务/浏览器、未登录、未写共享或业务数据。
- 2026-10-16：核对正式页面组件和入口。DCC工作台的可见按钮进入受控文件上传页；上传页两个独立多选控件分别有 `data-testid="dcc-upload-signoff-departments"` 和 `data-testid="dcc-upload-distribution-departments"`。工作稿详情 `DetailApplicationPanel.vue` 有可见“准备本次申请”、名单编辑及“保存本次申请草稿”入口；初始申请确认仍需在页面确认后提交。
- 2026-10-16：核对 Playwright 真实依赖：`IntRuoyiFronted/package.json` 声明 `test:e2e=playwright test`、Playwright `1.60.0`；本机安装模块解析到前端 `node_modules` 的相同版本；Chrome 和 Edge 可执行文件存在。新增脚本直接用该安装版本，只读取响应 method/path/status；没有HTTP、APIRequest、路由注入、页面JS注入、存储伪装、mock或force点击。
- `node --check doc/tasks/20261016-dcc-e2e-04/e2e-04.js`：退出码 `0`，PASS（语法检查，不启动浏览器）。
- `node doc/tasks/20261016-dcc-e2e-04/e2e-04.js --check`：退出码 `0`，PASS；检查依赖版本、页面选择器标识、绕过/注入调用静态排除，以及 dispatch prepare/runtime/role/port/branch gate 与浏览器启动顺序；结果存于 `static-check.json`，其 `businessStatus=NOT_RUN`、`browserLaunched=false`。
- `node doc/tasks/20261016-dcc-e2e-04/e2e-04.js`：退出码 `20`，预期 `PREPARED_WAITING / ROOT_GATE_NOT_EXECUTE`；脚本在配置、凭据检查和Playwright导入/启动前退出，未访问前端、未启动浏览器，结果 `browserLaunched=false`、`businessStatus=NOT_RUN`。
- `node IntRuoyiFronted/tests/e2e/login-default-credentials-static.spec.mjs`：退出码 `0`，PASS；登录缺省凭据静态合同通过。`rg -n 'VITE_APP_DEFAULT_LOGIN_USERNAME|VITE_APP_DEFAULT_LOGIN_PASSWORD' IntRuoyiFronted/.env IntRuoyiFronted/src IntRuoyiFronted/types` 无命中（退出码 `1`，表示没有命中）。未在脚本或证据内填入账号密码。
- task文档/fixture JSON 结构检查：PASS，7项声明任务资产均存在、任务必要章节齐全、12个子用例ID唯一、fixture/prepared JSON 可读。
- `git status --short --untracked-files=all -- doc/tasks/20261016-dcc-e2e-04`：本任务7项文件均可见为未跟踪；脚本采用 `.js`（package未声明ES module类型），避免 `.cjs` 按 `.gitignore` 隐藏。未执行stage/commit/push。
- 真实页面尚未运行：没有E2E PASS或业务FAIL。待Root提供精确T1项目/文件夹/文件类型/默认部门名称、不同的增补会签与下发部门、作者账号角色及组织负责人可见前置材料、任务自有源文件，并在dispatch中核实运行源码与角色fixture后放行。
- 未运行Maven或完整构建；无本例生产源码、共享配置、矩阵或Git改动。


## 2026-10-16 已授权真实UI执行续轮

- 已读取最新execution-authorization.md和dispatch：gate=execute、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture、PER_CASE_REAL_UI_MANIFEST；env runtimeVerified=true。旧prepare_only/等待Root另行放行口径已被用户最新授权取消，本轮没有更改共享dispatch。
- 运行依据：int_qms，前端8061、后端48061；Root核验Jar SHA b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071、PID24528、health UP。实际Git分支再次核对int_qms。
- 起初的租户/菜单/弹窗辅助脚本出现定位、表单DOM和加载时序问题，均属于脚本前置错误，不记业务FAIL。后续改用持续真实Playwright会话ui-session.js及ui-journal.jsonl逐步完成；不通过隐藏DOM、force、组件注入或接口补动作。
- 真实系统管理UI完成DCC241016C04角色分类及AUTHOR/SUPERVISOR/LEADER/APPROVER/DOC五角色；只给本例角色选择具体菜单/按钮，没有授管理员或修改共享角色菜单。
- 真实UI建立DCC241016C04U/S/L1/L2/LS/LD/A/D八账号，组织为本例D0/D1/D2/DS/DD五部门。逐项选择部门负责人及作者U直属主管S，保存后刷新/编辑表单确认。部门列表缓存人员昵称曾未即时刷新，刷新后证明实际保存，不误判为产品失败。
- 8账号各自独立context登录；首次改密由正式登录页完成。用户名均为本例前缀字母数字；身份、个人资料输入值和菜单真实可见，角色对应关系在管理员用户列表真实行核验。密码输入前停止trace，成功后才恢复；受保护凭据只存CurrentUser DPAPI临时配置并被本例.gitignore排除，不输出原值。
- 专用分类节点经真实UI建立：技术文档现有根只读作父节点，其下新建本例阶段/T1/章节/正文，编码DCC241016C04T2/T3/T4/T5；没有修改其它线程节点或公共矩阵。
- 真实类别检查UI：DCC241016C04正文→检查对应类别，弹窗显示“系统异常”；自然GET /admin-api/dcc/file-type-taxonomies/106/active-category，HTTP200。作者U真实菜单“文控中心→文件上传”进入正式组件，页面“系统异常”，会签控件disabled。详情证据为ui-011及ui-013截图/DOM文件。
- 只读当前运行日志output/runtime/int_qms/logs/dcc24-r8/stdout.log，定位缺列approval_source_mode及draft_approval_source_mode；已摘录到backend-schema-blocker.txt。未执行SQL或数据库迁移，不通过API写入或解析JSON作为业务通过依据。
- 项目申请正式表单已打开盘点，随后通过“取消”关闭；未提交项目/受控工作稿/审批申请，未上传源文件。fixture-manifest.json准确记录accountsVerified=true、businessDataVerified=false、status=BLOCKED_SCHEMA和23项本例残留。
- e2e-04.js已改用真实动态菜单uploadMenuPath、PER_CASE_REAL_UI_MANIFEST和本例独立登录/业务数据双核验；共享roleFixtureVerified=false不会阻断。没有把manifest假改VERIFIED。业务脚本基于实际schema blocker将12分支分别BLOCKED，退出21。
- node --check e2e-04.js / ui-session.js / run-e2e.js / audit-evidence.js及两个早期辅助脚本语法均通过；e2e-04.js --check退出0，gate=AUTHORIZED。静态合同与真实业务结论分别记录。
- node audit-evidence.js退出0：26份trace网络载荷已移除，未发现管理员/本例密码明文；截图只在无可见密码输入框页面采集。证据审计结果见evidence-audit.json。
- 本例浏览器context已关闭；readline未自然退出的本例ui-session Node按精确命令行确认归属后清理，process-cleanup.json记录PID；共享服务及其它线程进程未操作。
- cleanupAttempted=false / cleanupCompleted=false，仅保留23项本例账号、角色、部门与分类资产用于Root复核和恢复后复跑，不把浏览器关闭写成业务清理完成。未提交推送、未改生产源码/共享配置/共享矩阵，task状态blocked。
- 最终一致性验证退出0：task状态blocked、manifest=BLOCKED_SCHEMA且8账号独立登录核验true、businessDataVerified=false、23项残留、e2e-result内12分支均BLOCKED、prepared声明资产均存在；初次PowerShell属性计数写法错误已修正为数组计数后通过，不是业务用例失败。
- 最终node语法检查6脚本PASS；run-e2e.js真实退出21；26trace脱敏审计退出0。当前分支int_qms；只维护本例目录，未执行Git写操作。

## 2026-10-16 新运行态恢复

in_progress：用户明确要求恢复E2E-04，8061/48061，后端PID43284，Jar SHA caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff，Root的unblock-final-runtime-health.json为ready/health UP。前轮manifest/result/report已归档到rounds/blocked-before-caafeb，旧RED/BLOCKED保留。复用已创建账号、部门和分类，不重复创建目录；admin只作本例账号/最小角色配置，使用本例无项目指派的配置账号核对新目录。不变更admin原项目指派、共享矩阵/公共配置，不提交、不重启服务。

## 恢复轮实际执行（caafeb / PID43284）

- 保留旧RED/BLOCKED于rounds/blocked-before-caafeb；实际运行身份复制Root的unblock-final-runtime-health.json到rounds/resume-caafeb/runtime-identity.json。旧schema问题在本例真实类别匹配页已解除。
- 经真实系统管理页面新增本例CONFIG最小配置角色及CFG账号，无项目指派。CFG独立首次改密/登录成功；发现目录读取依赖dcc:controlled-file:query后，仅补本例角色对应受控浏览查询权限。未调整admin原项目指派或任何共享角色定义/菜单。
- CFG正式目录页按DCC241016C04前缀查询无行后只新增一次本例DIR目录，回查唯一行。新建类别初次绑定原自定义“DCC241016C04阶段”被业务校验拒绝，记录为夹具字段错误；保留旧节点并在正式FT_DESIGN_PLAN_STAGE下建立本例T1R2类型126，类别CAT 908745保存且“已匹配唯一启用文件类别”可见。
- CFG经真实矩阵编辑器维护本例D1/D2两会签及A批准规则，显式选择参与标记，刷新正式预览后保存；编辑器保存保持打开属于正式行为，重读及列表均确认本例值。只改本例类别矩阵。
- 真实页面建立专用Folders模板13与Folder节点，填写项目/产品/负责人U及NMPA/Y/Y/N默认属性，二次确认提交项目申请30。仅为本例A绑定既有精确dcc_project_reviewer角色，不改共享角色定义/菜单；A重新独立登录，在真实“项目申请与审批”中用浏览器原生意见框审核，申请30页面COMPLETED，实际项目293和本例文件夹可选。
- PDF技能用于任务源件：marker命令成功一次后用Bundled Python/reportlab生成非空1页DCC241016C04-source-r2.pdf；pypdf校验标记和SHA256，Poppler渲染后目视检查无剪裁/重叠。默认python无reportlab已准确识别，改用已提供的bundled runtime，未安装依赖或修改共享环境。源件未上传。
- 作者U真实菜单进入上传页并选择本例项目、文件夹、T1R2。当前页面首次选类型也要求确认切换（已有项目/属性输入），已按正式确认框点击确定，脚本openUpload同步补该确认及可见就绪/错误判据。
- 新RED（前置）：正式route-preview页面显示EXECUTION_CONTRACT_UNREGISTERED_DEFINITION，两份默认名单均为空，批准人未生成。只读源码定位catalog require/verify门禁；没有从响应JSON或DB猜实际definitionId/digest，未改公共流程/catalog绕过。
- preflight-r2.js独立可复跑真实作者路径再次重现，退出21；rounds/resume-caafeb中保留DOM/截图/脱敏trace/result。主run-e2e.js读取本例真实未就绪manifest，退出21并将12分支分别BLOCKED，写入新Jar/PID身份。
- 当前9账号、6专用角色、32项本例残留；项目申请30已COMPLETED，sourceUploaded=false、approvalApplicationSubmitted=false。业务清理未尝试，保留供Root review；未提交、不停/重启服务、不迁移、不改其它线程。
- 最终检查：7个JS脚本node --check均退出0，主静态合同gate=AUTHORIZED；新preflight与主业务入口均退出21。35份当前/历史trace凭据明文与网络载荷审计PASS；新preflight的pageErrors/evidenceErrors/processCleanupErrors均为空，acceptanceWrites=0。
- fixture manifest保留旧schema blocker于previousBlockers，同时记录新EXECUTION_CONTRACT_UNREGISTERED_DEFINITION。项目30派生项目293/产品PROD/Folder由真实完成页和作者选择器核验，纳入该残留清单记录；当前仍无受控文件申请/上传票据。没有本例UI/预检Node进程残留。

2026-10-17: 继续e537恢复轮；int_qms实际分支核验；规则重读，复用既有夹具，本轮真实页面预检及12分支待执行。

- e537恢复轮完成自有岗位18、两项任务菜单审核/批准权限、5名签名授权及本人图片，真实route-ready=4节点。类别908745仅增USER U/UPLOAD；原项目规则只读核对为审批创建的OWNER，本轮不改。临时CONFIG角色已移除。
- 岗位编辑UI加载竞态误加C04岗位至C02NoSupervisor，已真实页面仅移除本例岗位并回读原C02岗位保持；之后每次先核对弹窗昵称/加载再保存。
- 本例脚本文件夹节点、浮层aria-controls、正式配置确认修正后，7个上传页分支通过，真实save-draft生成2054545668044084183；详情页白屏，5条currentUserId TDZ pageErrors。主run退出1，原trace/结果保留。
- 独立新context完成04-09，两名单D1/D2及批准人全文与04-01 trace精确相等。汇总8PASS/1FAIL/3BLOCKED，未最终提交。阻塞由Root处理生产源码；只维护本任务目录。
- JS语法/静态合同PASS；audit-evidence.js退出0，58份trace脱敏审计PASS。任务浏览器正常关闭；原工作稿保留供review，业务清理BLOCKED详情白屏；不执行cleanup apply/提交推送，不标completed。

2026-10-17 Root通知TDZ修复后恢复原工作稿续跑；新context先只读，禁止新建重传。已核对int_qms、现有dispatch与最小声明前移。

## 2026-10-17 Root修复后原稿只读续验

Root移动currentUserId后，本例U新context打开原工作稿2054545668044084183，旧currentUserId错误未再出现，但仍白屏，精确pageerror为 `Cannot access 'controlledFileId' before initialization`。刷新只读再次复现。正式body DOM为空，DCC写请求=[]；未新建、未重传、未提交。

证据：root-detail-resume-result.json保留完整浏览器堆栈；r4-detail-resume-ui-001原稿截图/DOM、002刷新复现截图/DOM；脱敏trace见manifest。源码只读定位detail/index.vue:3068早读controlledFileId，3107 watch建立时求值，3545才声明。按Root明确要求不改production，保留原工作稿待Root修复后续跑。当前8PASS/1FAIL/3BLOCKED不变，历史错误及历史结果保持原样。

本次只读续验收尾：audit-evidence.js退出0，60份trace凭据/网络载荷脱敏PASS；本任务Node进程0，浏览器已关闭。原稿保留，业务阻塞controlledFileId TDZ。

2026-10-17 Root完成watch声明链根因修复后恢复原稿；先新context只读复验GREEN，再继续原业务。

## Watch声明链根因修复后：TDZ GREEN，详情业务读取仍BLOCKED

原U新context进入原fileId2054545668044084183后，页面已能渲染；刷新复验pageErrors=[]，currentUserId/controlledFileId TDZ均未复现。真实页面仍报“系统异常”，编号/版本等业务字段为空，无“准备本次申请”按钮；不能把骨架渲染当原稿成功加载。

正式页面自然GET详情HTTP200，未解析响应JSON；同期运行后端日志明确`Cannot execute statement in a READ ONLY transaction.`。链路：QueryService.getControlledFile只读事务→toRespVO:3726→AttachmentService.readOrdinaryAttachmentManifest:177→requireAttachmentVersion:252→Mapper.selectByIdAndTenantForUpdate（SELECT FOR UPDATE）。此为新后端阻塞，生产修复归Root，本例不改源码/数据库/服务。

root-watch-resume-green.json和rounds/resume-watch-fixed/readonly-result.json区分TDZ GREEN与业务读取BLOCKED；backend-readonly-blocker.txt为精确只读日志摘录。r5-watch-resume-ui-001/002截图及DOM证明页面。全程DCC写请求=[]，未重传重建、未最终提交。原8PASS/1FAIL/3BLOCKED保持，04-06/07/08/10须在原稿继续。

本轮收尾：只读证明及文档结构核对PASS；62份trace凭据/网络载荷脱敏审计PASS。任务浏览器关闭，原稿保留，业务因后端只读事务锁查询BLOCKED。

2026-10-17T02:21:58.368955+08:00 Root维护停止通知：已确认本例Node进程0、浏览器关闭、无当前页面写入；身份及停止证据maintenance-stop.json。原fileId2054545668044084183保留，未最终提交。等待新PID/SHA健康证明，维护连接错误不计业务FAIL。

## 跨例岗位误写事件补全

正式操作日志11629原C02岗位、11632误加C04岗位、11638仅移除C04岗位，均业务编号910426。已按Root要求以精确username查询及GET id=910426编辑弹框只读回读，昵称匹配，岗位精确仅[DCC241016C02验证岗位]，无C04；取消未保存，update请求0。完整事件incident.md，当前弹框证据incident-current-edit-dialog-readback.json及incident-readonly-ui-008截图。任务runner加入精确身份保存校验，13项边界测试PASS，未新增产品防御或编辑其他例账号。

Incident核验收尾：用户编辑弹框只读GET id910426/昵称/原岗位集合一致，已取消；13项脚本身份边界测试PASS，node --check ui-session.js PASS，63份trace凭据/网络载荷脱敏PASS。本任务浏览器已关闭，业务维护停止保持。

2026-10-17 Root维护结束：新PID16776/48061，SHA494306625fb1d04261832e4145a8aa83c3734257c02069e9b1e7644749efcdec。保留原8PASS，恢复原工作稿详情读取/名单及主管节点提交验证。

PID16776原稿恢复结果：04-06真实原触发GREEN、04-07保存刷新PASS；04-08新增下发D1持久化正确但错误提示回显不一致，保存前DD/D1、刷新D1/DD，FAIL保留交Root。04-10填真实说明并核对二次确认后仅1次POST /{id}/submit，旧路径wait超时后未重试；刷新待直属主管审核，BPM22c11270-c993-11f1-8f08-b082e25ec548。当前11PASS/1FAIL/0BLOCKED。S额外待办只读检查缺权限，未扩权/审批。66份trace脱敏PASS，浏览器关闭，原记录保留，等待Root review。

Root确认排序误报修复32tests PASS；原稿已提交且BPM22c11270-c993-11f1-8f08-b082e25ec548，按新明确授权建立唯一最小ECHO1工作稿复验，不重复原提交或重建项目/账号。

04-08排序误报已在唯一授权ECHO1稿2054545668044084184取得真实GREEN：DD→D1顺序保存，成功toast且无错误，刷新D1/DD、会签D2。原fileId2054545668044084183仍同BPM待直属主管，submit恰1；ECHO1 submit0。最终12PASS，69trace脱敏PASS，原RED保留。先标ready_for_closeout进行清理预览，工程收尾归Root，不提交推送。

Cleanup首次preview=delete0；首次apply输出日志closeout-apply.txt未预登记keep，被脚本当作自身待删除文件，因stdout文件占用WinError32拒绝，未删除业务或证据。保留closeout-apply-first-failed.txt，已仅补本任务输出日志keep后重新preview/apply。

最终结构核对PASS：12唯一分支全部PASS、原submit1/回归submit0、列表精确回读及保存无错误。cleanup最终preview/apply PASS delete0，先ready_for_closeout再依无Git授权/Root review保持blocked，不标completed。原RED、首次cleanup失败及所有验证证据保留。


## 2026-10-20 round1 rerun start
按Root转交当轮用户授权完整重跑12分支；只处理本例目录。运行前已核对int_qms、frontend PID20976/backend PID27908及新Jar SHA ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab；后端health UP。记录独立新申请，禁止旧PASS代替。

2026-10-20第一轮终态：12PASS，无适用分支遗漏。新文件4287/DCC241020C04R1单次提交、BPM31450180-cc28-11f1-a8a5-b082e25ec548；最终只读刷新PASS，所有浏览器关闭。结果rerun-20261020-round1/rerun-result.json；旧PASS和本轮原始FAIL均保留。未改生产或配置，工程Git禁止等待Root统一收口。
