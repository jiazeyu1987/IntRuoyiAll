# Execution Log

## 2026-10-15 基线与授权

- 分支 int_qms，HEAD f06833f06；指定基线 a3b8234ec 已包含于历史，二者只差任务文档。已读 AGENTS、task-closeout、backend/frontend、database、DCC 版本安全/浏览和 GxP 审计规则。
- 用户限制：不执行 E2E、服务重启、数据库写入、Git 提交/推送；优先后端模型/接口/生命周期，不碰 view/index.vue 或 OnlyOffice。只新增两个独立前端文件与测试，接线交 viewer owner。
- 工作区初始已有两个 LOG_FILE_IS_UNDEFINED 修改和大量历史任务资产；当前其他线程继续改 Controller、Query 邻域、detail/workflow/viewer 和测试，本任务未回滚、暂存或提交任何文件。

## 实施与口径

- 预览 access event / watermark issuedAt 是登记时间，只有 viewer ready 后的 server startedAt 进入首次/最近打开统计。每 10 秒心跳，0..15 秒有效，超时整段零累计；旧 session 被新 start 收口时不补未确认尾部。
- 新增 dcc_controlled_file_preview_view_session，tenant/user/resource/file/version/event/token ID/nonce/body wire 绑定；不保存原始 token。
- 新增 DccPreviewReadingSessionController，DCC / unified 各 start、heartbeat、stop、summary 共 8 个接口，原始 token 只在 header 传输。Controller 请求日志关闭，DTO toString 脱敏。
- Query interface/implementation 仅追加 assertPreviewReadingAccess：复用 exact body guard、权限、事件、水印、viewer token 和业务存储 provider，不读取 bytes、不产生新 event。
- 新 session API/helper 捕获原 source/证据，迟到 start 自停，pause 等待在途 heartbeat，隐藏/卸载不残留 timer；附精确 API 集成说明。未改 viewer 父页/OnlyOffice。
- MySQL 只 CREATE 新表，生成 active_marker 保证单活跃、client 唯一键幂等；不回填旧文件、签名、事件或培训。H2 create/clean fixture 同步但未执行数据库。

## RED

- Maven 属性未加引号的命令两次被 PowerShell 拆分；不是产品缺陷证据。加引号后确认新增 API/model 不存在导致编译失败；随后修复 Mockito BaseMapper insert/update 重载歧义。
- 最小 session stub 下真实 JUnit 四项均 ERROR：两个正向 UnsupportedOperationException；反向 strict Mockito 未消费 stub。未把 fixture/编译错误混称业务 FAIL。
- 前端 lifecycle 文件缺失时 5 FAIL（ENOENT），API 文件缺失时 3 FAIL（ENOENT），随后实现 GREEN。
- policy gate 只指定新 SQL 时因 base dependsOn 不在输入集合失败；补齐明确三个文件的完整闭包后通过，不靠忽略依赖。

## GREEN / REGRESSION

- 第一轮 session/unified preview/controller：30/30 PASS。
- 第二轮 exact body guard/HTTP/schema/summary：85/85 PASS。
- 最终 Maven -pl yudao-module-dcc -am 定向九类：94/94 PASS，0 failure/error/skip；Session 23、HTTP 3、schema 2、真实 Query current-source 27、Context 3、Guard 19、Office token 8、Unified Service 6、Unified Controller 3。具体命令及 XML 指纹归档在 verification-report.md / verification-results.json。
- 最终 Node 21/21 PASS：API 3 + 生命周期 6 + G07 context 12。覆盖 long ID、旧 metadata 捕获、token header-only、pending start 卸载/隐藏、heartbeat 在途停止及暂停期间恢复。
- 定向 ESLint 两文件 0/0；pure lifecycle TypeScript strict 0 diagnostics；git diff --check exit 0。
- migration policy gate status passed / migrationCount 3，含 base/protection/new 依赖闭包，未执行迁移。
- 所有测试为 Mockito/MockMvc、Node 端口、静态 schema/policy；没有 E2E、服务启停、真实或隔离数据库写入。未授权 Git，未提交/推送。

## Closeout

- task.md 先设置 ready_for_closeout，核心 PASS/RED/限制已复制进默认保留报告，准备运行本任务 cleanup preview/apply。
- 最终需 blocked：viewer 接线/运行库部署尚未发生，且用户当轮禁止 Git 收尾，不能标工程 completed。


- Cleanup preview / apply 均 exit 0：保留 task/log/report/integration note 和两个正式 JSON；只删除本任务 7 个临时 stdout 日志及 1 个组装 fragment。未删除任何其他任务资产。
- 清理后状态按当前授权设为 blocked；没有 Git 提交/推送，也没有把未接线/未部署写成产品完成。

## 续办：实际 viewer 计时/统计闭环

- 用户转交 Root review：原两个 helper 没有生产消费者。本次已授权 view/index.vue，继续原任务，不新建重复目录。
- 已重新读取 AGENTS / frontend-development / task-closeout 与 G09 当前交付；新行为 BDD V01..V06 已先写 task.md。后端合同保持，不跑服务/E2E/数据库写入/Git/全量构建。

## 续办最终检查点与冻结

- 初始真实 SFC 1PASS/9FAIL -> 10/10 GREEN；新增 Vue renderer 统计区、成功 heartbeat/pause、pagehide/pageshow、UUID 初始化失败和 900s 端口拒绝后，专属最终 25/25 PASS（SFC16/API3/lifecycle6），0skip。
- pagehide 未绑定真实行为 RED 1FAIL -> GREEN 1PASS；UUID 初始化失败后 focus 再次同步抛错真实 RED 1FAIL -> 将失败关联到当前 owner.failed 后 GREEN。读取失败不显示假 0，旧 summary 不能覆盖新版本。
- 中途全格式/Office/PDF.js/G07 组合83/83实际PASS；它不是末尾冻结后的统一回归回执。最新指令后只运行本线程三文件 Node，未再修改其他测试 harness。此前三处宿主适配精确清单及指纹交 Root。
- 当前三生产文件 ESLint0/0；实际 reading AST block + API/helper/context/source 严格定向检查0diagnostics，未跑full types/build。
- Root review 微秒计时：先写六个固定Clock参数化行为测试，再改完整Duration比较；本线程没有Maven/sharedtarget票，RED/GREEN未运行。
- Root review 认证门禁：先写实际方法安全代理与ss表达式Bean、review/approve-only × TASK/HISTORY、匿名/未认证、实际reading service显式guard拒绝端口测试，再改 controlled 四路isAuthenticated；统一四路权限不变。测试装配API仅用javap只读确认，未编译/执行，待Root统一票，不能写PASS。
- 最终 view/index.vue 已冻结为42253a6c8ead376e2135fb259d8368ae34cc77091c41e9a31c62efff4b29e8df（UTF8/LF）；10个源码/测试完整指纹见review-handoff.json。
- 发现OnlyOffice被并行owner更新，当前e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1；未触碰或回滚，旧hash不能覆盖现状。
- 没有E2E/真实DB/服务/Git提交推送/remote/subagents；准备本任务cleanup，报告已归档核心结果，后端待Root验证。

- 最新回执：两个 Java 生产类及两个测试类已完成实现并冻结，不等待 Root 先跑；最小 RED 恢复点与命令归档 root-red-restore-points.md。Root 统一实际 RED/GREEN，本线程不伪记 PASS。
- 本次 cleanup preview/apply 均 exit0，保留9个核心记录/校验脚本/指纹，删除8个本任务临时日志/fragment；未动其他任务资产。check-viewer-reading-types.cjs 已显式 Cleanup Keep，仍受仓库忽略规则影响，未来获准提交时须由 Root 处理 force-add，本轮未 stage。
- 当前状态 blocked，仅表示后端验证票/Root review及禁止Git收尾；viewer工程代码已接线且冻结，没有遗留“待viewer owner接线”的产品缺口。

## Root 前端合并回执与 M01 进行中

- Root 回执：12文件Node82/82PASS、0skip，7生产输入零漂移；Office旧共享合同9/9已修复。未在本线程重跑，不改写为自有验证。
- 唯一M01由Root执行真实后端RED→GREEN，只临时恢复两处旧行为再恢复；当前结果未回执，不写已通过。
- 保持所有Java/tests和前端冻结；此次只更新task/report/log/review-handoff四个自有报告文件，未执行额外套件，未改生产/测试，等待M01。

## Root M01 正式回执：软件问题已关闭

- 只读核对Root目录backend-red-summary.json / backend-green-summary.json / backend-red-state.json：RED11执行，R01两个15s+1µs实际expected4/actual19、R02四个实际AuthorizationDenied、F01一个DAILY工作稿预览断言失败；合计3断言+4权限拒绝、0skip，无缺Bean/setup错误。
- Root正式-am十类GREEN125/125，0F/E/skip：29/12/6/7/3/11/27/19/3/8，各项在125内；前三生产源码按Root原始字节SHA恢复0mismatch。此为Root实际运行，不是本线程重跑。
- Root FE viewer82与详情99分别通过/0drift，不混加历史计数。Root补可选signatureSummary.processInstanceId类型；只读归档时fe-types-green-result.json已exit0、2429输入0drift，本线程未跑完整types或修改该生产文件。
- R01/R02/F01不再保留待Maven/缺依赖阻塞；当前仅运行态/MySQL/E2E/Git边界未验。所有源码/tests继续冻结；本次只改自有报告与JSON。

## Root最终前端与工程校验回执

- 本次Root Viewer12文件82+Detail12文件99=181PASS，各组0fail/skip、0输入漂移；只汇总本次两个正式范围，未混加历史数字。
- 完整pnpm run ts:check exit0，2429源码输入0漂移；signatureSummary正式可选string/null补齐后Workflow emitted runtime新旧字节相同，未重复业务套件。
- Root10生产文件lint exit0/0error，detail两条既有换行warnings如实保留；不宣称0warning。
- M01 fresh125PASS且三临时恢复源原GREEN指纹匹配；R01/R02/F01、前端类型均已获正式回执，不再待Root验证或缺依赖。
- 本次仅更新自有报告/JSON，全部生产与tests冻结，未重跑套件、未重新取生产指纹或执行清理；Root继续最终指纹与清理。运行/MySQL/E2E/Git边界仍未验。

## 本次继续：只读边界核对与新RED准备

- 实际分支int_qms/HEAD f06833f06；复读规则与原目录，未新建任务/子Agent。Root29项无漂移回执作为本次起点；旧125/181/types是上一轮证据，不称本次执行。
- Root完整Vite构建期间保持生产前端只读；独占专属测试和BDD可写。候选READ-CONT-01：pagehide没有持续挂起标记；READ-CONT-02：已关闭start幂等响应仍被helper当active。准备实际Node RED，不改生产前端。

## 本次继续 GREEN 与再次冻结

- Root解除生产前端构建冻结后才改GREEN；之前只写BDD和两专属tests。READ-CONT-01两条实际RED（focus starts2/expected1，late ready starts1/expected0），READ-CONT-02实际RED（ended start replay未拒绝）；共3FAIL/0skip。
- 最小生产修改仅view/index.vue持续pagehide状态门禁及reading-lifecycle.ts正式endedAt校验；专属测试端口补齐endedAt:null，无自动续签/吞错或替代成功。相同3条GREEN。
- 本轮新执行自有28/28PASS、相邻26/26PASS；新runtime变更后必要回归，不与旧125/181/types混算。3生产ESLint0/0；实际reading block定向strict0diagnostics，未跑完整types/build。
- 新迁移8项静态断言及声明依赖文件存在检查PASS，无SQL执行。后端正常授权失败前置与session锁/结束幂等源码依据已归档；锁等待授权可见性和完整运行schema仍不作已验证结论。
- 没有新增Java/后端RED测试或改生产后端，不存在未跑RED先改后端；Query只读。
- view冻结SHA 09ea0b785198591e27ba602e3d44c2eb57764aa81c0c452ade7f8dff019e689a；helper冻结SHA 16acc85ca3742983f48fafe141df8c8c5d266b13cb141cbf6947ca50ed070840；完整证据continuation-evidence.json。
- 未改其他owner文件/测试宿主，未运行Maven/sharedtarget/fulltypes/build或禁止动作。准备仅本任务清理并交Root判断改后集成验证。

- 本次cleanup preview/apply均exit0：保留11个核心文件，仅删除本轮3个临时日志；核心实际RED/GREEN/回归与只读结论已归档，不动其他任务。结构/UTF-8/空白检查及冻结源码指纹复核通过，状态blocked遵守Git禁止规则。

## 第二轮继续：新边界 RED 准备

- 已关闭pagehide/ended start重放候选；本轮只读进一步发现 heartbeat 回包 endedAt 未校验，以及 viewer pause/stop 失败显示路径需要覆盖。
- 先新增两个真实专属Node RED；暂不改生产，未运行Maven/fulltypes/build/E2E/服务/数据库/Git。

## 本轮按Root限定两个候选结束

- READ-CONT-03 ended heartbeat真实RED（未报错expected1/actual0）→helper检查结束状态GREEN并取消timer；身份不一致原guard保留。原session用于stop，未改API或后台。
- pause stop失败的新增测试修前已PASS，不伪记RED；切源dispose stop失败只console、页面无失败提示为真正READ-CONT-04 RED（expected拒绝内容/actual空），增加独立readingCloseError渲染GREEN；Vue实际模板title断言通过，新正文服务器summary22秒不被覆盖。
- 定向3项GREEN；最终自有3文件31/31PASS/0skip；两生产ESLint0/0；reading block定向strict0diagnostics。完整types/build/Maven由Root，未重跑；上一轮200/200/types/build保留历史。
- 只改两生产和两专属tests；未改其他owner宿主/源码、Java/SQL或OnlyOffice。原pagehide/ended start规则保留。按Root要求结束本轮、生产冻结，指纹round3-evidence.json。

## Root追加真实身份缓存窗口

- 新BDD READ-CONT-05后，实际SFC修改actor/baseTenant/visitTenant缓存并释放metadata/bytes/summary/Office ready，5条真实RED：旧metadata触发body、旧bytes发布、旧owner发heart、旧summary写入、Office emit opened。前三循环RED停在actor首个场景，不假称所有变体独立RED。
- 最小viewer身份快照门禁GREEN；actor/base/visit变体全部通过，加storage通知、base变化但visit不变、token删除、focus不复活/手动reload恢复、迟到start不跨身份stop。最终专属41/41PASS0skip，viewer lint0/0，定向identity/reading块strict0diagnostics。脚本新增外部端口声明后缺renderVersion的2diagnostics属工具宿主声明缺失，补齐后0，不冒称产品RED。
- 新进口auth/getAccessToken/getTenantId/getVisitTenantId、useCache(USER.user.id)。其他owner宿主未改，精确三文件适配清单交Root，见identity-boundary-review.md。
- 核验点在响应/ready/tick/focus/pageshow/storage，未宣称同窗口裸storage写同步派发事件；已发出的旧网络不能撤销，旧会话结束未确认不模拟成功。无服务器越权结论。
- 仅追加viewer和专属test/自有类型脚本，原lifecycle修复保留；API/Query/token/Java/SQL/OnlyOffice未改。未跑Maven/sharedtarget/fulltypes/build/跨owner旧宿主套件。全部再冻结交Root，最新指纹round3-evidence.json。

- 最终cleanup preview/apply均exit0，13文件全keep、delete空、warnings none；仅自有文档变更。5个记录源码/测试/API指纹复核无漂移，源码与tests冻结，当前blocked遵守Git禁止与Root统一集成验证分工。
