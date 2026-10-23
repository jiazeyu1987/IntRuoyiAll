# Execution Log

## R3 round1 最新交付

本轮新授权完整重跑已完成：10/10 PASS，首次实际exit0，无重试。唯一正式交付rerun-20261020-r3-round1/handoff.json；本次临时源件清理、trace隐私/CRC和context/browser/runner全关通过，当前hold供Root review。旧结果/UNKNOWN保留，工程因当轮禁止Git提交推送blocked。

## rerun-20261020-r2-round1 正式EXECUTE交付

- 已在Root新包709e78bd/41508以U2本人fresh登录完整重跑；businessRunId=DCC241016C01-20261020050805303，首次10/10 PASS、实际exit0、无重试。
- 独立rerun-20261020-r2-round1/handoff.json、branches.json、13截图/trace/result、安全及全关回执齐全。没有正式申请/版本或最终提交，本次临时源件清理成功，context/browser/runner关闭，本例脚本进程数0、服务PID保持。
- 只适配运行证明格式，不改页面定位或业务断言，不改产品/权限/共享协调文件，无build/restart/直接API/DB/Git/Agent。详情见本轮目录execution-log.md与verification-report.md。
- 已先ready_for_closeout，最终工程blocked因本轮禁止Git提交推送，当前hold不启动第二轮。历史失败UNKNOWN保留，不以本轮PASS覆盖。

## rerun-20261020-r2-round1 PREPARE_ONLY

- 仅准备，未启动浏览器、未读取或解密凭据内容、未登录或执行业务动作。原合同10分支不变，未修改权限/生产代码/服务/共享协调文档。
- 入口为 `rerun-20261020-r2-round1/round1.cjs`；独立记录及回执在同目录。fixture账号为芋道源码/DCC241016C01U2，原DPAPI凭据来源存在；本轮未验证实际登录。
- runner仅增加artifact group隔离；两个脚本node语法检查通过，默认预检实际Node exit2/PREPARED_WAITING。详细命令与证据边界见round1目录execution-log和verification-report。
- 当前blocked只表示等待新package READY及Root单独执行授权，不以历史PASS/REPAIR_ONLY替代本轮，不继续启动下一阶段。

## 2026-10-20第一轮完整重跑

- 用户当轮授权完整重跑，替代上轮“不得重跑PASS”的限制；仅验收收集问题，不改产品。已读取根AGENTS、相关E2E/登录/本机/端口/worktree/收口/UTF-8规则及四轮计划、原合同；使用ponytail技能保持最小测试校准。
- git branch --show-current → int_qms/exit0；git rev-parse HEAD → f06833f06c868dc3a451eb63d01381a467ecfe92；监听8061/PID20976、48061/PID27908与用户派工一致。
- dispatch.json仍指向旧backend PID25124及旧Jar；不修改协调目录，以用户明确指定的doc/tasks/20261020-qms-e2e24-runtime-update/runtime-proof.json为本轮运行来源，脚本通过显式参数读取并记录，原dispatch仅保留用例归属/gate检查。
- 当前PID27908命令Jar路径和48061匹配；Get-FileHash实际值ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab与Root证明一致（exit0）。复核本例合同与当前入口/确认/清理锚点；原10分支及预期不变。
- 测试最小校准：e2e-01.cjs增加显式--runtime-proof及关闭状态；新增rerun-20261020.cjs复用DPAPI内存凭据、独立launcher回执，不覆盖旧receipt；verify-evidence.cjs增加本次run限定扫描与独立回执，不覆盖旧安全检查。无生产修改，不执行生产RED/GREEN/Maven/XML。
- 命令node --check doc/tasks/20261016-dcc-e2e-01/e2e-01.cjs及rerun-20261020.cjs均exit0。实际执行node doc/tasks/20261016-dcc-e2e-01/rerun-20261020.cjs → exit0，runId=DCC241016C01-20261020013336716；launcher-1792460016090实际childExitCode=0、runnerExited=true。
- 北京时间2026-10-20 09:33:36至09:33:50，原U2新context本人登录并确认账号；本次原10分支全部实际PASS，无FAIL/BLOCKED或遗漏。选原P1R2/Folder/T1R2，重新核对默认值与矩阵/默认下发/培训，修改CE、注册人否、生产方是、转移是，通过唯一新PDF上传和非空canvas；确认框固定U2/Dept及S2/Dept且无编辑控件，取消保留本次输入。
- 本次文件输入编号DCC241016C01-20261020013336716-DEFAULTS；文件名DCC241016C01-20261020013336716.pdf。原页面移除本次源件，session-cleanup自然HTTP200、成功toast、文件行/预览消失均通过；cleanupAttempted=true/completed=true/residual=[]。无最终提交请求、申请或正式版本；A/1仅输入。原历史失败资产UNKNOWN不变。
- 本次结果、13关键截图及trace保留于artifacts/DCC241016C01-20261020013336716；12确认和13清理图已视觉复核。不是逐步trace截图导出声明。pageErrors=[]，无本次业务问题；矩阵通用运行能力说明不代表本例简单规则失败。
- 命令node doc/tasks/20261016-dcc-e2e-01/verify-evidence.cjs doc/tasks/20261016-dcc-e2e-01/artifacts/DCC241016C01-20261020013336716 doc/tasks/20261016-dcc-e2e-01/rerun-20261020-evidence-safety.json → PASS/exit0，1份trace，已知凭据字节命中0、network资源0字节。
- browser/context关闭且browserDisconnectedAt已记录；runner实际exit0；Get-CimInstance仅筛本例脚本进程数0，监听8061/20976和48061/27908保持一致（exit0），独立回执rerun-20261020-process-close.json。没有残留本例runner，不触碰共享服务。
- 已先标ready_for_closeout，保留所有历史及本次证据/fixture；不执行cleanup apply或Git写操作，工程状态按当轮限制最终blocked。业务PASS与工程状态分开，执行结束后不擅自启动下一轮；详情rerun-20261020-handoff.json。
- 最终node断言核验exit0：10真实PASS、独立launcher退出0、当前PID/Jar身份、13截图/trace存在、临时清理、空pageErrors/最终提交请求、browser/context关闭与runner进程0全部一致；结果SHA256=bf53d82cbafc545d5702d97034337d14443178c464250dc4b140d0ae001bbf98。git status限定本例目录只读检查确认新脚本与回执可见，未add/commit/push；原result及launcher/安全回执未覆盖。

## 2026-10-19 r4：仅历史失败资产证据核查

- 已读根AGENTS、docs/task-closeout-rules.md、docs/e2e-rules.md相关清理门禁、docs/powershell-encoding.md，以及repair-dispatch-20261019-r4.md与本例root-handoff.json；git branch --show-current实际exit0，输出int_qms。
- 授权范围仅失败上传身份与正式清理回执；不创建、不删除、不重跑原PASS。先更新本任务目标、里程碑、验证和设计约束，再输出本轮handoff。
- Root正式review为ACCEPTED_BUSINESS_PASS，10分支，原成功运行保持历史PASS；该review不证明早期失败批次已清理。
- 限定来源检查：本例顶层文件清单与最新root-handoff、协调目录e2e01命名回执、r2/r3/r4派工；未发现新增资产身份或清理来源。未扩大搜索目录、未重新扫描trace/HAR、未反复按文件名检索页面。
- 原失败result：upload-preview HTTP400；cleanupAttempted=true、cleanupCompleted=false、serverState=unknown；已记录网络中session-cleanup为0条，顶层无ticket/asset/sessionId/attachmentId/storageKey身份字段。仅能识别本地文件名及测试输入编号，不能映射精确服务端资产。09-stopped.png与trace.zip仍存在，本轮没有重新渲染/重跑或声明新页面PASS。
- 失败result SHA256=94a82592226eec8f236cda2848fa1fc5dc970bd26c346c17386912baf27ab3df；独立成功result SHA256=f24f6c269430238fa754f193b08ef8f44ab161f1e75649d455efa480b4cee0c8，与Root接受值一致。成功批次的session-cleanup HTTP200不得回填失败批次。
- 结论UNKNOWN：既不证明服务端仍存在资产，也不证明没有资产或已经清理。恢复条件为新增且可追溯到原失败运行的正式上传会话/ticket/附件或存储身份，以及关联原身份的正式清理/状态证据；只有同名PDF、成功批次清理或当前页面空态不足以恢复判定。
- 本轮无生产/脚本代码修改；BDD为文档核查边界，不执行生产RED/GREEN、Maven、JUnit或E2E，XML不适用。未进行浏览器/API/SQL/账号/业务动作、删除、服务或Git写操作；不向其他线程发消息。仅本例文档与handoff归本线程。
- 状态先标ready_for_closeout后结构验证；本轮不执行cleanup apply，按派工保留全部历史证据及fixtures，不删除任何资产。工程收口因禁止Git提交推送保持blocked，不宣称completed。
- 实际node -e结构核验exit0，输出PASS: r4 handoff structure, evidence paths, UNKNOWN boundary, original PASS preserved；核对UNKNOWN边界、原10分支保留、无新E2E/XML、证据及改动文档路径存在。精确命令记录在r4 handoff.commands中；此PASS仅文档结构检查。原失败与成功result的SHA256复核一致（exit0）。

- 2026-10-16：Root创建本例记录；用户授权每个E2E一个实际线程，Root统一启动和 review。未执行真实浏览器业务动作。
- 2026-10-16：已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 及本例 `thread-contract.md`。
- 分支核对：`git branch --show-current` → `int_qms`；本例端口契约为前端 `8061`、后端 `48061`。
- 页面核对：真实动态菜单入口“文控中心 → 文件上传”；路由 `/dcc/controlled-file/upload`；组件 `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`；项目属性组件 `project-attributes/ProjectApplicationAttributes.vue`；矩阵展示锚点和二次确认锚点已逐项核对。
- 参数化拆分：10 个独立子用例，详见 `task.md` 的 Subcases And Page Contract；预期值不写死在脚本，全部来自 Root 确认的同目录 fixture。
- 脚本：`doc/tasks/20261016-dcc-e2e-01/e2e-01.cjs`；正式依赖为 `IntRuoyiFronted/node_modules/playwright`，版本 `1.60.0`；默认浏览器来源为 Playwright Chromium，存在性已检查。
- 静态检查：`node --check doc/tasks/20261016-dcc-e2e-01/e2e-01.cjs` → exit `0`；`node -e "JSON.parse(fixture.template.json)"` → exit `0`；禁止 API/mock/DB 业务动作扫描未发现 `fetch`、`axios`、`APIRequest`、路由拦截或 mock。脚本中的单个 `canvas.evaluate` 只读取真实 PDF canvas 像素，作为非空渲染断言。
- Gate 保护探针：`node doc/tasks/20261016-dcc-e2e-01/e2e-01.cjs` → exit `2`，输出 `PREPARED_WAITING`，`browserLaunched=false`；dispatch 当前 `E2E-01.gate=prepare_only`，未启动浏览器、未登录、未写业务数据。
- 当前前置缺口：dispatch 记录 `runtimeVerified=false`、`roleFixtureVerified=false`，且 backend 运行态/角色与任务自有 fixture 仍由 Root 核对；不能记录 E2E PASS，也未创建 trace、截图或业务结果。

## 最新授权及修复后真实恢复

- 2026-10-16读取最新execution-authorization.md、dispatch.json及unblock-final-runtime-health.json。旧prepare_only限制已取消；gate=execute，采用PER_CASE_REAL_UI_MANIFEST，未修改共享false状态。
- 当前身份：int_qms，8061/PID36084；48061/PID43284，health UP；Jar SHA256 caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff。Root的77项回归及5份schema验证只作为运行前置，不作为本例业务PASS。
- 上次交互进程已结束，旧随机凭据未猜测/重置；旧R1资产和证据保留。本轮创建同前缀ConfigR2/U2/S2/L2/A2/D2身份，密码仅在Windows用户DPAPI保护的任务临时配置维护，并被本目录.gitignore排除。
- 所有账号、角色分配、组织关系和业务配置均由真实页面完成。六账号分别独立context登录；首次强制改密期间未录trace。配置账号无原项目指派，业务身份无管理员角色；未授全量范围，未修改admin原指派或共享角色定义。
- 新P1R2/ProductR2由本例A2使用正式项目审核员职责在真实页面审核至COMPLETED。ConfigR2为项目OWNER，仅为U2授本项目EDIT；旧P1保持原正式权限范围。
- DCC241016C01Dir经无项目指派配置账号查询确认0后首次成功创建并回读，没有重复创建已有目录。原创建尝试没有成功落点。
- 自定义阶段DCC241016C01Stage被类别页明确拒绝生命周期阶段非法，保留原证据；在既有合法阶段下创建专用T1R2，未修改共享阶段。专用类别、NEW路线、矩阵和SOURCE大小策略全部真实页面保存，主体仅本例Dept/A2/D2。
- 本例部门挂于既有根下，未改根部门字段；U2主管为S2，部门负责人为L2。完整对象清单见fixture-manifest.json。
- U2从正式菜单进入上传，选P1R2/Folder/T1R2并确认类型选择后，页面明确显示EXECUTION_CONTRACT_UNREGISTERED_DEFINITION；批准人分区显示“审批矩阵未带出唯一的批准节点”。不是共享roleFixtureVerified=false导致停止。
- 原始准备/阻断：artifacts/setup-20261016115718377/evidence.json；旧R1证据目录artifacts/setup-20261016031350046保留。

## 实际复跑和退出码

- 命令：node doc/tasks/20261016-dcc-e2e-01/run-r2.cjs --available-branches；这是明确的部分验证入口，整体永不PASS。
- 首次部分复跑available-20261016124844695因脚本同步计数早于远程下拉加载而失败，未记业务PASS；改为等待当前aria-controls面板中的唯一正式选项，原失败保留。
- 后续真实复跑为5项PASS、5项BLOCKED。视觉检查发现早期阻断截图位于确认框退场/未滚动到预检，补充隐藏断言和真实滚动后另存新证据，旧证据没有删除。
- 最新有效结果：artifacts/available-20261016130038158/result.json；trace.zip及7张关键截图齐全。launcher-receipt.json实际childExitCode=2；PowerShell实测NATIVE_EXIT=2。
- PASS：01-defaults、04-market-edit、05-identity-edit、06-transfer-edit、09-training。BLOCKED：02-compiler、03-organization、07-matrix、08-distribution、10-confirmation。
- 无源件上传/工作稿保存/审批提交请求，businessWriteRequests=[]；没有申请ID或受控版本。测试末通过页面刷新清除未保存输入，formCompleted=true。fixture未删除/停用，残留及保留原因已写manifest/result，不宣称fixture清理完成。
- trace已停止并保留，context/browser关闭，交互返回CLOSED；后续版本trace文件名包含时间以避免重开同账号覆盖历史。
- 交互辅助进程在CLOSED后仍保留stdin句柄，已在所有trace/context/browser完成关闭后仅终止本任务辅助进程；源码补充readline.close及stdin.pause用于后续正常退出。不是共享服务操作，不影响E2E子进程实际exit2回执。
- 5份cjs脚本均node --check通过。verify-evidence.cjs实际PASS/exit0：19份trace无本轮已知凭据字节，network/HAR资源均0字节。旧R1未知随机凭据未做字节比对，只结合录制边界及空HAR核对。
- 状态blocked交Root review。用户禁止Git提交推送且业务门禁仍阻塞，未标completed；未运行cleanup apply或删除证据。

## Root定义修复期间：仅账号/岗位前置

- 最新指令限定本轮只核对本例原账号、组织、正式岗位和签名资格，不上传/提交文件，不发布流程，不修改共享配置。复用原U2/S2及加密凭据，不重建账号/fixture。
- BDD：Given U2和S2已存在且同租户启用；When 分别通过用户管理页读部门/岗位，若岗位为空则通过组织角色页新增本例专用岗位并绑定，保存后重开回读；Then 二者均有非空正式岗位，账号、部门和主管关系保持本例事实。requireNativeAccount同时校验U与S，错误码不能直接归因仅S。
- 签名资格只按真实页面核对并记录，不将本轮前置结果覆盖原完整E2E结论；完整原用例等Root新包/映射恢复通知。
- 本轮真实盘点：U2(910405)与S2(910406)均启用、未锁定、属于本例部门，但正式岗位均为空。通过组织角色页创建DCC241016C01PostU2(id15)/DCC241016C01PostS2(id16)，分别绑定编制岗位/主管岗位。
- 保存后原用户列表因岗位字典缓存未更新仍显示“-”，未把该瞬态当作保存失败重放；分别重开编辑页确认真实已选岗位，并整页重载用户管理后再次核对两个列表行显示正确岗位。部门未被清空，U2→S2主管关系页面回读一致。
- U2/S2使用原DPAPI凭据独立context重新登录成功，无重建账号、无重置密码。
- 电子签名→用户授权页只读核对：U2/S2均“启用、未锁定、未授权、签名停用”。没有改授权、上传签名图片或执行签名；图片状态未另行假定。本例01不接管03的审签/图片准备。
- 证据：artifacts/setup-20261016145756004/evidence.json，11张关键截图及各context trace；结构化回执native-account-preflight.json。自然请求写元数据只有2次system/post/create和2次system/user/update，文件上传/提交/流程发布均0。
- 收到Root“新包构建中，安全点关闭浏览器、停止页面写入”后已立即保存checkpoint并shutdown；交互进程返回CLOSED/exit0，全部context/browser关闭。未开始上传或完整业务复跑；本轮岗位前置PASS不覆盖历史E2E BLOCKED。

## 资源隔离新运行态恢复

- Root恢复通知：后端34096/48061，Jar SHA256 e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce，health及官方真实登录PASS；依据workflow-isolation-runtime-proof.json和最新dispatch。
- 复用原ConfigR2/U2/S2账号、已绑定岗位及P1R2/T1R2等fixture，优先只读页面预检并记录原定义错误是否解除和新具体缺口；不会仅因新PID使fixture失效或重建。
- 第一轮新包预检确认原定义错误解除，A2及会签/默认下发部门已带出；真实页面列出S2/L2/A2阶段权限、签名授权/图片以及L2/A2正式岗位缺口，已先写root-workflow-resume-result.json交Root review。
- 按Root追加授权，复用本例主管岗位16绑定L2/A2/D2并重开回读；新建本例Review/Approve两最小角色，菜单权限分别仅6816 DCC Review / 6817 DCC Approve。S2/L2/D2绑定Review，A2绑定Approve；原Participant仅增加本人“我的签名”入口，未修改共享角色定义或授业务人管理员。
- 签名授权由配置管理员仅对本例S2/L2/A2/D2启用并刷新回读；四人分别独立登录，从正式个人入口上传带本人账号及TEST FIXTURE标识的PNG并启用。真实图片naturalWidth>0、文件名/启用状态及再次从正式菜单打开均核对通过，没有用管理员替业务人上传。
- 图片准备时S2初次内联状态等待失败、L2浏览器刷新曾进入404，保留原异常/截图；没有重复上传。已从正式菜单重新打开并核对原已保存图片，未改路由或用接口补状态；该旁路观察不作为原E2E-01上传业务通过依据。
- 补齐后预检真实显示“审批路线已就绪，共4个节点”，编制身份/组织基线不变，A2、会签/下发Dept正确。manifest按真实核验更新为fixtureComplete=true，旧运行身份保留在runtimeHistory。

## 最终完整E2E-01

- 首次完整尝试：artifacts/DCC241016C01-20261016155412944/result.json，7分支PASS、3尚未执行；upload-preview HTTP400，页面显示Current user is not the DCC project owner，整体FAIL/exit1，旧trace及失败清理回执保留。
- 起初按文案尝试把自有项目U2的EDIT临时升为OWNER；继续核对源码发现NEW_UPLOAD实际允许EDIT，DccControlledFileCategoryPermissionSupport在缺类别UPLOAD规则时也复用DCC_PROJECT_ACCESS_DENIED的owner文案。该初始归因不准确，已在任何重试前恢复并回读U2的最小EDIT，仅为本例专用类别增加USER U2的UPLOAD规则。ConfigR2原OWNER保留，没有扩大既有admin范围或改公共矩阵。
- 最终命令：node doc/tasks/20261016-dcc-e2e-01/run-r2.cjs；实际FULL_NATIVE_EXIT=0，launcher-receipt.json的childExitCode=0。
- 最终结果：artifacts/DCC241016C01-20261016161107874/result.json，原10分支全部PASS。二次确认可见固定编制人U2/部门Dept、直属主管S2/部门Dept、实际CE/注册人否/生产方是/转移是及本例转移至、批准人A2及原组规则、会签/下发Dept、不需要培训；编制人/主管没有编辑控件。
- 真实PDF源件通过页面上传并渲染非空canvas；点击“创建受控文件”只打开二次确认，逐项核对后点击“取消”，原输入保留。没有点击最终确认提交、没有保存工作稿、没有创建申请或正式版本。
- 通过源件行真实移除动作完成upload-temporary/session-cleanup，页面清理成功提示及文件列表/预览消失均断言；本轮cleanupAttempted=true、cleanupCompleted=true、residual=[]，finalSubmissionRequests=[]。首次400尝试因未出现清理成功Toast而保留cleanupCompleted=false/unknown历史回执，没有伪改成成功。
- 最新trace、13张关键截图、PDF及结果已保留；视觉复核12-confirmation-actual-values.png确认编制人/主管/属性/矩阵/名单真实可见。
- 当前业务结论PASS，Root review及工程收口仍待完成；专用账号/岗位/角色/签名图片/项目/类型/矩阵/策略/权限规则保持供review，未把临时源件清理等同全部fixture删除。无生产源码、Git、服务、流程发布或SQL改动。
- 最终证据安全检查PASS/exit0，32份trace归档无本轮已知凭据字节，network/HAR资源为空；全部6份任务cjs语法、JSON/文档结构和10分支PASS/exit0/临时清理/证据存在性检查通过。配置辅助会话CLOSED/exit0，所有本例浏览器已关闭。
