# Execution Log

- 2026-10-16 Root创建本例记录；用户授权每个E2E一个实际线程，Root统一启动和review。当前仅准备，尚无真实E2E执行结论。
- 2026-10-16 读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 及公共 `thread-contract.md`。当前分支核对为 `int_qms`；`int_qms` 基准端口为 `8061/48061`。
- 2026-10-16 静态入口核对：审批中心待办/已办页面为 `src/views/approval-center/index.vue`；DCC 处理详情为 `/dcc/controlled-file/detail/:id` 的 `handling=approval` 路径；版本历史“查看详情”由 `openHistoryDetail` 进入带精确 `bodyReadContext` 的 viewer；工作版本显示函数 `getDccVersionDisplayText` 映射为“草稿”。
- 2026-10-16 新增 `e2e-21-real.cjs`，仅使用 Playwright 页面操作、可见 DOM、canvas 像素和页面导航断言；没有 `fetch`、`axios`、`APIRequest`、SQL、mock、路由拦截或 localStorage 登录注入。脚本在加载 Playwright 和创建 browser 前读取 Root `dispatch.json`，当前 gate 为 `prepare_only` 时返回 `PREPARED_WAITING`。
- 2026-10-16 静态检查：`node --check doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs` exit 0；`node doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs` exit 0，输出 `PREPARED_WAITING`、`browserStarted:false`。未启动浏览器，未触发任何业务写请求。
- 2026-10-16 runner 来源核对：`IntRuoyiFronted/package.json` 的正式 runner 为 `test:e2e: playwright test`；任务脚本从 `IntRuoyiFronted/node_modules/playwright` 加载 Playwright 1.60.0。Playwright Chromium 缓存可执行文件存在；本轮没有 launch。
- 2026-10-16 Root review 修正：不再允许空正文标记或仅 canvas 非空作为固定正文通过；execution-input 必须提供每个旧/当前正文独立、不可互相包含的可搜索标记，脚本先验证当前版再验证旧版，旧标记必须命中、当前标记必须未命中。
- 2026-10-16 Root review 修正：trace 在登录成功后才开始；登录失败先导航 `about:blank` 再生成脱敏 trace。每分支（当前对照、三类固定正文、草稿、三类拒绝）持久化 `result.json`、trace 和截图；总汇总写入 run 目录 `result.json`。新增 `redact-trace.py` 和 `execution-input.template.json`，未启动浏览器。
- 2026-10-16 修正后静态验证：Node 语法 exit 0；Python `py_compile` exit 0；模块合同 exit 0；prepare-only 预检 exit 0，输出 `PREPARED_WAITING/browserStarted:false`；禁用网络/API/存储注入扫描无命中。Python 语法检查生成的 `__pycache__` 已清理。
- 2026-10-16 用户最新授权更正：读取协调目录 `execution-authorization.md` 和最新 `dispatch.json`；本例 `gate=execute`、`accountSetupGate=authorized`、`businessGate=execute_after_own_verified_fixture`，环境 `fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST`。共享 `environment.roleFixtureVerified=false` 保持原值，遵循本例独立manifest模式。
- 2026-10-16 通过真实 `/login` 页面登录授权配置账号并点击可见“系统管理”菜单，核对“用户管理/角色管理/部门管理/菜单管理”入口；点击可见“文控中心”菜单确认“上传审批/文件上传/受控浏览”入口。登录成功后没有在密码页开启trace。已建立本例fixture准备阶段；尚未创建业务账号/数据。

- 2026-10-18 本轮恢复：int_qms，8061 PID24564/48061 PID18204；Root核验运行Jar SHA a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402。复用既有7个账号、原目录及L2–L5；本例配置账号独立登录成功。runner PID12772，handle59663；新证据见runs/2026-10-18T03-10-13-063Z。
- 2026-10-18 类别 DCC241016C21CAT 已真实保存并由列表回读；自有L2改为设计和开发策划语义，目录仍为原DCC241016C21受控子目录；本例最小夹具培训/分发未开启。文件夹模板 DCC241016C21文件夹模板 已真实保存及回读，正在原正式产品目录项目申请链准备项目。以上仅前置配置结果，不是E2E-21业务PASS。

- 2026-10-18 Root R1624-07维护安全停步：当前A版确认框已通过真实页面取消，正式文件/BPM/task均未创建。文件列表按Delete走正式移除回调，页面源件及预览已清空，自然POST /dcc/controlled-files/upload-temporary/session-cleanup HTTP200；截图 runs\2026-10-18T06-57-33-391Z\setup-1792308257494-maintenance-source-removed.png。所有context/browser已正常关闭，runner已exit0。runtime-pause.json active=true；checkpoint保存原项目P1 COMPLETED和后续上传位置。未改共享dispatch，未启停服务。

2026-10-18T07:42:43.029657+00:00 Root授权151101新运行态恢复；仅解除本例pause，新context本人登录回读原对象后续跑。旧结果runtime hash保持。

151101运行态：READER原项目回读后正式上传A，文件2054545668044084230；TASK真实待办显示A/1。夹具RED为仅菜单缺bpm:task:query；UI补原有查询节点，fresh TASK待办GREEN。待办入口由过期工作台定位改为审批中心可见查看按钮。

检入RED原因修正：服务doCheckinControlledFile要求bound.fileName等于原sourceOriginalFileName；B文件命名不同导致REQUEST_INVALID，非修改说明为空。修正本例夹具文件名，B/C正文内容不变，不改生产代码，不认作业务缺陷。已取消未绑定B临时上传，原A检出保留。

2026-10-18T09:05:18.765019+00:00 A完整发布GREEN、B工作稿检入GREEN；REPLACEMENT缺APPLICATION_CUSTOM投影真实RED。未正式提交B。浏览器runner关闭，checkpoint已落盘，交Root唯一分配。

Root要求只读区分来源：已保存前后DOM、自然path-status、save/preview计数；Query检入三字段有事务UPDATE，working GET能读已保存custom；replacement短构造五字段null是当前直接投影丢失点。RevisionService清旧字段后重绑路径未被执行，未判其通过或失败。详见replacement-custom-matrix-bdd.md。无生产修改/Maven。

2026-10-18T09:31:09.626193+00:00 Root唯一分配后只在现有custom数据库夹具新增一个行为测试：DccCustomRejectedCheckinResubmitTest#replacementHttpReadPreservesSavedCustomProjectionWithoutWrites。仅本方法diff与基线校验一致。测试冻结待Root Maven验证RED；生产未改，E2E原对象未动。

2026-10-18T09:53:58.131204+00:00 Root首轮1fail/0error为测试JSON可选null比较差异，非有效产品RED。仅将defaultSource/actual改为DccProjectAttributes领域相等，其余五custom/定义/选择仍精确断言。旧surefire保留；再次冻结待Root重跑，无生产改动/Maven。

2026-10-18T09:57:54.868600+00:00 按Root补充统一HTTP data反序列化为DccWorkingApplicationAttributes，逐一精确比较原19字段（含effectiveDate及嵌套属性）；working分支和五custom均未跳过，无默认填补。仅本方法变更、19领域断言结构校验通过；冻结待Root有效RED。

2026-10-18T10:20:33.141741+00:00 Root确认有效replacement五字段null RED后，已在唯一分配readReplacementApplicationAttributes增加13行custom投影分支。复用reserved/canonical校验，保留OWNER/基线/REPLACEMENT资格和unavailable，不调用普通working读取、不写源。本方法外字节文本一致；候选冻结待Root GREEN/回归。本线程未运行Maven、未动原E2E对象。

2026-10-18T11:02:50.347458+00:00 Root回归Browser12/12、CustomRejected42/42通过；SelectedWorking45PASS/2error为旧测试未传selected earlier附件manifest。仅两处请求复用父private a29ExpectedServerManifest（现有ReflectionTestUtils调用），不改父夹具/附件guard、不造空hash。冻结待Root单类47回归，旧失败日志保留。

2026-10-18T11:04:47.488956+00:00 Root明确授权后父helper仅private改protected，方法体及共享数据/端口/cleanup完全一致校验通过；子类两处直接调用a29ExpectedServerManifest(earlier,request/req)。两个文件精确diff已存selected-working-manifest-two-files.patch，冻结待Root47邻近类，不重跑12+42。

200656运行态Root放行：仅本例runner支持实时重载自己的UI步骤/完整app断点，修正原批准确认框以精确owner910390核对；新context回读原4230/4236后继续。旧runtime证据保留。

2026-10-18T13:44:14.887855+00:00 B1 4244五签名已受控生效，旧4230已作废/4236已替代。READER历史A viewer精确VERSION_HISTORY context，UI正文cannot access/无canvas，正例失败保留。CURRENT脚本过期popup定位触发未捕获等待，runner退出；核对其PID46912无浏览器子进程残留，新只读会话改为context page事件。未重提或重建业务。

2026-10-18T13:59:57.770617+00:00 200656原B1已完整受控GREEN；历史诊断只读分清查看矩阵未配与已办上下文丢失，闭合A任务不当有效TASK正例。Root指示不增广/不拼URL/不修改，已停步保存所有对象及3拒绝NOT_EXECUTED。

2026-10-18T14:30:37.078908+00:00 Root要求聚焦HISTORY实际caller：只读确定DccApprovalTaskAdapter历史detailQuery producer只给viewer/from；前端openModuleDetail/builder保留该query。首个最小Adapter行为RED设计已回传，未改源码/测试/Query，未跑Maven，不增权限。

2026-10-18T14:55:07.205633+00:00 Root唯一首分配后仅新增DccApprovalTaskAdapterTest#pageDoneCarriesExactApprovalHistoryBodyContextForRetainedOldVersion。实际adapter.page(DONE)合法非deleted旧A1、历史UPLOAD定义/旧task/旧round，wire decoder精确身份，无额外文件mapper交互或workflow调用；旧类其余文本完全一致。待Root Maven有效RED，生产/Query/前端/DTO未动。

2026-10-18T15:30:01.268200+00:00 Root有效history-link RED后仅Adapter DONE producer新增精确历史wire；历史正式definition映射类型，未知或缺身份无正文链接。TODO/deleted builder与基线逐段完全一致；增加两项必要branch测试，旧测试不改。候选冻结待Root Adapter全类/合同，未跑Maven、未动Query/前端/DTO/权限/原对象。

2026-10-18T15:47:47.770395+00:00 Root Adapter29PASS/1unused-stubERROR：仅删该未用stub并加一个合法LEGACY绑定行为测试（NEW/REVISION），生产hash不变。一次基线捕获延迟发生于编辑后，未用于差异证明；改用已保存before-green源+精确branch patch重建原阶段，核对仅分配delta一致。测试冻结待Root有效RED，未Maven。

2026-10-19T10:36:09.416666+00:00 20261019-r2仅报告整理：先读Root repair-dispatch、根AGENTS及closeout规则，实际分支int_qms。发现旧verification-report仍为历史阻塞，与Root 2026-10-19T05:39:21.709529+08:00 review PASS冲突；旧task里程碑仍未勾选。依据root-integrated-result.json 10PASS/0FAIL/0BLOCKED及协调acceptance21行更正，旧task/report全文归档report-history。历史prepared/gate/runner/manifest/checkpoint保持原字段，报告按时间说明被最终结果替代；不抹除旧RED。当前业务PASS，工程因无Git/cleanup apply未完成。本轮不改生产/测试、不重跑E2E、不Maven/构建/服务/数据库/权限/全租户job、不提交推送。

2026-10-19T10:41:39.246230+00:00 本轮文档结构验证PASS：10参数与Root一致，14截图引用（含1个task相对路径）存在，9trace ZIP目录可读，原runtime/finish/Root exit0一致，Root结果SHA保持。首次引用检查仅相对路径解析有误已修正，未改原证据；不是新E2E。handoff已生成，业务剩余0，工程未完成，不需运行态/构建窗口。

- 2026-10-20 repair-after-four-rounds: 根因更正：fixture-setup-driver secretCodec原为UTF8，第四轮工具误用Unicode.GetString解码；不是六账号真实密码已失效。新增本例repair-20261020-credentials解码helper及公开样本RED1/GREEN0；原六身份UTF8解封后真实登录成功，原10分支本次10PASS，10trace/31截图，runner16044退出0/browserClosed/contexts0。无产品或账号写入，原密文SHA不变。旧第四轮BLOCKED及来源误判原样保留；新结果和handoff见该repair目录。
