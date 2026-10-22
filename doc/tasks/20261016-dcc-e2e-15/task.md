# E2E-15 延迟生效，无培训

## Goal

独立验证E2E-15，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

2026-10-20最新正式第三轮：`rerun-20261020-r2-round3-handoff.json`。新文件4367/BPMefb9ca0a-cc4b-11f1-ada5-b082e25ec548，全适用15-A至G共7 PASS；实际生效/启用14:26:01，原T14:20:26、受控/签名14:10:27分别记录，原U正文canvas非空和历史真实状态独立通过。首次脚本错误/会话过期/PTY中断及trace缺口均保留，全部本例进程关闭HOLD，不开始第四轮。旧结果与修复阶段证据不计本轮。

2026-10-20四轮后状态展示修复最新入口：`repair-20261020-display/task.md`、`repair-20261020-display/verification-report.md`及`repair-20261020-display-handoff.json`。两处前端最小修复及40项定向测试通过，原4320待生效历史UI通过并已向Root发可激活回执；原确认task已完成，映射直接测试GREEN但同状态待办UI未重建。自然生效由16协调，原round3六PASS/一BLOCKED保留，不能用10-19历史PASS覆盖本轮15-G前置。

本轮范围以“2026-10-19 r2 证据整理范围”为准；以下10-16/17的代码、准备状态和BDD保留为历史记录，不是本轮待执行项。

2026-10-19 r4范围：只收口已完成来源检索的恢复条件与证据边界；PA2同VIEW前后PASS及5626定向自然触发PASS保持不变，不重跑、不启用全租户调度。

2026-10-17 Root追加窄范围：修复E2E-02 APPLICATION_CUSTOM会签部门无负责人提示。目标RED后仅拆null-leader分支返回具体部门名/ID及负责人缺失；Root r2 专属3项+完整resolver20项共23/23通过，0fail/error/skip，Maven exit0。源码冻结；前次customWorkflow继承fixture失败保留，不扩修。Root构建/部署后由E2E-02真实页面复验原参数；handoff已收口至GREEN_VERIFIED_23_PACKAGE_AND_UI_PENDING，不视为业务PASS。

本轮新增唯一生产归属：`BpmModelServiceImpl.updateModelBpmnXml` 的 editor/deployment source 资源隔离，以及专属 `BpmModelEditorSourceIsolationTest`。Root Maven 已取得目标 RED（1 test/1 failure/0 error/0 skip，四个断言命中资源覆写与未隔离），修复后隔离3项及三类相邻回归共28/28通过，0fail/error/skip。源码/测试冻结；全包构建和真实UI重导入及历史资源hash由Root/C05继续验证。禁止触碰实际库、catalog、迁移历史实例或启动 E2E。

2026-10-16 最新追加范围：只读 review 现有 V6 upload/revision、V2 catalog/digest、正式部署入口和精确定向测试，交付 `workflow-v6-review.md` 给 Root；此子任务期间不启动 E2E、不执行 Maven/部署/DB 操作，原 E2E 目标及历史证据保留。

## Current Status

blocked — 2026-10-19 r2证据整理及结构核验已完成，业务15-A至G PASS与Root接受结论保留；工程提交/推送被本轮用户禁止，依task-closeout-rules.md不能标completed。证据已达到ready_for_closeout，本轮无cleanup apply、重跑或任务启用。历史执行快照仍为39740/Jar060621，不代表本轮新修复运行态。

## 2026-10-19 r2 证据整理范围

- 目标：保留PA2同U、同查看权限下T前禁用与T后非空正文；明确全租户自动调度未验证及旧中断trace缺失，检索现存原始文件，不补造证据。
- 里程碑：核对原始DOM/截图/operations与保存trace覆盖范围；同步报告和本目录repair-20261019-r2-handoff.json；做文档结构及引用检查。
- BDD：Given 原4216、PA1、PA2分工及Root接受记录已存在；When 只读检索本目录现存证据并核对时间、权限、trace范围；Then 业务PASS保持，缺失trace与全租户调度未验证单列，不能把PA1混淆观察或8-U确认前trace当作PA2 T前完整trace。
- RED/GREEN：文档整理采用结构检查；RED是本轮交接文件尚不存在的记录缺口，不是产品失败；GREEN须以实际引用和结构核验结果填写，不新增生产测试。
- 预期验证：本地JSON可解析、所引文件存在、PA2观察时间小于计划时间且后续正文观察在实际生效后、保存trace归档完整性与覆盖边界明确；不执行页面E2E、Maven、全量类型检查或服务操作。
- 设计约束检查：只改本任务目录；不改共享Query或任何生产/测试源码，不操作数据库/API/调度任务；不提交/推送，不启用子Agent。工程完成状态按task-closeout-rules.md另列，不能用业务PASS冒充工程completed。
- RED：本轮交接文件不存在的本地检查为true，仅表示文档缺口。GREEN：交接命令实际exit0，15项原始证据哈希及Root接受的27个关键trace哈希一致，PA2时间边界/禁用DOM核对通过；未生成新业务证据。
- 状态流转：in_progress → ready_for_closeout（文档结构通过）→ blocked（工程Git门禁禁止）；业务PASS独立保留。无需构建窗口、服务或浏览器，不空等Root放行。

### BDD: preserve accepted PA2 and targeted scheduler evidence while closing recovery boundaries

- Given PA2 has same user/view matrix evidence before and after the selected time, and job5626 has historical natural-tick success for the three task-owned files.
- When the r4 handoff records only recovery conditions and evidence limits after the prior source search is complete.
- Then keep the existing business PASS; state that tenant-wide resident scheduling is unaccepted, old missing trace chunks cannot be reconstructed, and no new browser, job, runtime, API, SQL, Maven, or Git action is performed.

- RED/GREEN: no production or test code changed, so production RED/GREEN and XML are not applicable. Documentation GREEN is the local JSON/reference structure check recorded in `repair-20261019-r4-handoff.json`; it must not be described as a new E2E PASS.

### Recovery conditions and boundaries

- Old PA2 07:06:30 daily-observation trace can only be restored from an original runner trace/temp backup whose event range covers that timestamp and whose provenance can be hashed; screenshots, DOM JSON, operations logs, or a new run cannot reconstruct the missing trace.
- Old original-F post-confirmation activity has the same rule: no saved raw trace with the relevant event range means the gap remains historical and explicit.
- Tenant-wide automatic scheduling requires a separately authorized, task-owned sample and current Root-approved runtime; configuration, natural tick, visible post-tick state, and final pause must all be performed/read through real pages. The accepted 5626 targeted job is insufficient evidence for that broader claim.

## BDD

### BDD: retain original G result and isolate the pending-access evidence supplement

- Given 原4216确认签名#858已保存，计划时间05:27:10已过而实际仍待生效；原日常全局查询超时导致选定时刻前正文按钮证据缺失。
- When 维护后继续真实UI核对原4216实际启用及正文；若按Root授权另建一份自有pending-access申请，只复用原项目/角色并选约10分钟后时间。
- Then 4216原结论不得被新样本覆盖；新样本仅补到期前日常正文限制，不重签/回滚4216，不改时钟/API催生效或写DB。当前尚未创建补充样本，先等待Root维护通知。

### BDD: E15-F mandatory delayed-confirmation audit policy delivery

- Given 原无培训延迟申请已由真实S/A/D审批到原U确认节点，config/gxp-audit-policy.yaml正式声明操作dcc.controlled-file.delayed-effective-confirm（Root已确认），运行策略当前不可用。
- When 原U通过真实页面选择近未来北京时间并以本人密码单次签名确认。
- Then 当前真实RED为必需GxP审计策略缺失/未启用，fresh UI不得出现受控、确认签名或生效推进；Root须核对运行策略并正式精确交付，不放宽签名/审计。修复后同一精确任务重新选择近期未来时间，成功时受控时间=真实本节点签名，才继续待生效及到期正文验收；不能凭前5节点签名或源码声明替代完整E2E。

### BDD: resume original E2E15 from verified current UI state

- Given 当前int_qms8061/48061运行证明为e2e10-rework-matrix-runtime-proof.json，业务资产前缀DCC241016C15，旧目录未保存verified角色manifest或已提交业务checkpoint。
- When 先在真实页面盘点本例资产、核未重复提交，再用任务自有角色和正式APPLICATION_CUSTOM（仅本例不要求PUBLIC）完成无培训延迟申请及完整审批。
- Then 按原例15-A至G分别记录可见DOM、精确任务/版本、缺日期/缺密码/过期时刻拒绝、未来签名与待生效/到期终态；缺前置自行UI补齐，公共资源问题交Root。凭据只运行时处理，登录和签名输入不进入trace/截图；不能用接口/响应JSON/缓存注入替代UI。

### BDD: platform evidence read must share the suspended DCC transaction boundary with send

- 目标触发链修正：先复用既有prepare夹具在真实REQUIRES_NEW事务提交两条PENDING job（原父夹具发送端仍隔离），然后接入真实平台链；在单个真实DCC事务内逐条dispatchMessageJob并注册afterCommit，捕获该事务的ConnectionHolder。只在此事务内部断言零发送；提交后核对真实证据读不处于该旧事务/连接。不得改变prepare传播级别。Root首轮8tests/1fail/0errors是内部prepare已提交后错误断言0发送，归测试夹具预期错误，不能作为目标RED或生产GREEN授权；其余7PASS保持。
- Given 正式下发批次包含两名收件人；DCC提交后逐人派发，平台使用真实NotifySendServiceImpl事务、NotifyMessageServiceImpl/Mapper、模板及稳定businessKey持久化。
- When 真实afterCommit循环发送并读取精确消息证据，或首人平台已提交但DCC回执落库失败后重放。
- Then 平台send与getNotifyMessage证据读均在NOT_SUPPORTED范围内、不继承原DCC连接/SqlSession；两人全部完成，各自sentAt取原平台createTime。回执失败后重放复用同一消息ID、businessKey和createTime；已SENT不再发送。
- Given 私有测试新建平台行被故障注入为缺失或tenant/user/userType/template/businessKey不匹配；When 正式证据读；Then 真实Mapper返回的缺失/错配证据仍被拒绝，job/attempt持久FAILED，另一合法收件人继续完成；不mock getNotifyMessage结果、不松校验、不改运行消息/job。
- H2仅验证真实链路和NOT_SUPPORTED证据读边界，不声称具备MySQL RR快照。Root已完成platform-snapshot-visibility.json：独立私有MySQL库保留连接A原COMMIT后先读第一条可见，第二次独立提交对A同快照不可见、独立读和结束快照后可见。该控制实验仅数据库可见性，不是生产DCC/Spring全链RED；本线程不新增复制库脚本或任何数据库操作。
- 本轮只新增测试及本目录交接；生产发送器冻结，Root取得实际目标RED后才另行授权GREEN。原batch14任务118已SENT不得重发；119–123后续仅由Root/E2E03页面重试并复用现有平台业务键，不删改既有消息或job。

### BDD: same authenticated-session token refresh preserves its identity caches

- Given 同一已认证会话已保存当前 USER（含角色/权限）、ROLE_ROUTERS、base tenant及有值或缺失的VisitTenantId；真实service收到业务401，需要刷新token。
- When 执行真实Axios响应拦截器与真实auth/cache函数，刷新HTTP响应业务code为0或200且token pair完整，再回放当前或排队请求。
- Then 仅更新access/refresh token，原USER/roles/routes/visit值及缺失状态精确保留，不删后重建、不重新查询用户、不从Pinia/viewer兜底；回放请求使用新access token，单次并发刷新只请求一次且恢复原队列。
- Given 用户通过新账号登录持有新的token pair；When 调用真实既有setToken；Then 仍先清USER/ROLE_ROUTERS/VisitTenantId三项，base tenant及主题/登录表单缓存保持，不放宽跨账号污染保护。
- Given 刷新HTTP成功但业务失败或成功载荷缺token字段；When 真实401 handler处理；Then 不写入任何token、不回放当前请求，走既定重新登录提示及确认清理流程；已处于relogin状态时仍清理并跳转。缺USER时正常刷新不得凭空制造身份。
- 本轮只新增handler单元测试及原15记录。使用既有typescript转译完整TS源与真实axios链，浏览器存储及HTTP adapter为明确私有端口；不改生产auth/service、现有static/theme测试或依赖，不运行pnpm/Maven/浏览器。Root唯一执行RED/GREEN；不把此隔离验证当自然refresh网络时序或正文预览E2E证据。

### BDD: distribution effectiveDate uses the committed effectiveness fact for new modes

- Given 正式已激活版本的模式为 AFTER_STANDARDIZATION_SIGNATURE 或 DELAYED，旧 effective_date 为空或与实际 effective_time 日期不同，原下发批次和收件人义务已提交。
- When 真实 MessageDelivery 在业务事务提交后派发或重放同一消息任务，并经过正式 NotifySendServiceImpl 模板必填参数校验。
- Then effectiveDate 取该精确版本 effective_time 的日期；提交前和业务回滚均不发送。新模式缺少实际 effective_time 时必须在平台调用前明确拒绝并持久化 FAILED，不能取旧预设日期、延迟选定日期、activated_time、当前时间或占位符；补齐真实事实后同任务重放保持原业务幂等键。PRESET_DATE 和 null legacy 仍使用既有 effective_date；正式模板必填校验、审计和发送证据校验不放宽。
- 本轮只新增私有 H2 事务回归及交接；不改生产/模板/YAML、不运行 Maven、不操作运行库。DCC 派发、Mapper、批次/尝试审计、事务是真实实现；平台发送使用明确的隔离端口并调用正式模板校验，不属于完整通知 E2E。Root 先取得目标 RED，原批次13/14的页面重试留给E2E-03。

### BDD: inherited custom draft context must identify the new target application

- Given 初版已通过真实Workflow提交，源幂等key已占用；已终结源审批及受控状态作为明确私有夹具，矩阵/会签/定义/属性历史均可追溯。
- When 经真实Query检出检入创建新working，读取服务实际保存context key并作为本次提交幂等key，或进一步派生另一个target。
- Then 新target及reserved round的key独立于源已提交key、不同target不同、同target多次读取及重提稳定；按保存key提交不撞源，矩阵和会签、定义/属性继承保持，源canonical/已提交历史不改。同key同载荷重放不重复，新提交不通过前端UUID/fallback或放宽幂等冲突。Root先跑RED，生产和真实draft先不改。

### BDD: activation event uses one representable instant without preceding control

- Given activated_time旧列为秒而受控/事件/批次来源为微秒，Root独占任何单列前向精度迁移；G02正式Flowable/签名及实际Batch Mapper/consumer可重读字段。
- When 非整秒时钟触发立即激活或延迟到期激活。
- Then 按拟定datetime6激活合同，字段update/set、ACTIVATED事件和批次sourceOccurredAt取同一微秒activatedAt且不早于controlledTime/选定生效时刻；源签名和受控微秒不改。错误字段/事件/登记仍拒绝，接收失败回滚、成功重放不重复。旧datetime0无法支持该时序与精确一致性，不能靠截秒或放宽consumer修补。先交Root RED，不改生产/DatePolicy/effective_time。

### BDD: CONTROLLED event time equals the database-representable control fact

- Given 正式V6真实Flowable/签名夹具已有完整批准和受控副本；file.controlled_time、workflow occurred_at及relation controlled_at可保存微秒。
- When Lifecycle完成受控并由真实关系消费者核验、保存和重放CONTROLLED事件。
- Then 延迟事件时间=file.controlledTime=原签名S，即使S不等于终处理now；立即受控使用不丢失微秒、数据库可表示的新控制事实，亚微秒处理时钟按既有MICROS时钟合同向下归一，仅作用于新控制事实，不重写任何电子签名。消费者接受合法微秒但仍拒绝亚微秒及错file/event时间；同event重放不变不重复。Root先运行本轮RED，生产和全局DatePolicy暂不改。

### BDD: ordinary attachment manifest read must not take write locks

- Given 详情读取运行在真实只读事务中，附件清单仍需验证精确租户/版本、访问权限及原附件字节事实。
- When 实际 AttachmentServiceImpl.readOrdinaryAttachmentManifest 读取当前版本。
- Then 只使用普通版本查询，不调用 selectByIdAndTenantForUpdate；错租户/版本/附件仍拒绝。freeze/checkin写入口仍对源和目标调用FOR UPDATE且拒绝跨版本身份。先交Root定向RED，生产helper和详情readOnly暂不改。

### BDD: custom signoff department missing leader identifies its exact cause

- Given 本次自定义审批有合法作者、直属主管、批准人和文控，所选会签部门中一部门存在且启用但没有负责人，另一个部门有真实负责人。
- When 调用真实 resolver 的 resolveCustomPreview 或 resolveCustomReserved。
- Then 仍抛业务异常阻断路线，不返回部分候选；使用既有 CONTROLLED_FILE_ROUTE_NOT_READY 携带明确“会签部门”“部门负责人”及缺负责人部门名称/身份，不改变全局错误码或自动选人。部门配置完整时保留全部会签义务和批准/文控。

### BDD: BPM editor source must not mutate a shared historical deployment resource

- Given 真实私有 Flowable H2 引擎已部署一个完整 BPMN 定义，并构造旧 seed 所具有的同一 bytearray ID：`ACT_RE_MODEL.EDITOR_SOURCE_VALUE_ID_` 指向该部署的 `ACT_GE_BYTEARRAY` 资源；模型保存真实 tenant、metadata 和 deploymentId。
- When 调用真实 `BpmModelServiceImpl.updateModelBpmnXml` 导入不同 BPMN XML，不发布新模型。
- Then 旧部署资源 bytes 和完整图 digest 保持不变；模型编辑源指向新的独立资源并包含新 XML；原资源 ID/部署关系保留，tenant、metadata、model ID/key/name/category/version 和 deploymentId 不变。当前代码预期 RED 为旧部署 bytes/digest 及 editor source ID 被原地改写；本线程不运行 Maven，不把编译/环境错误当目标 RED。

- Given 上传时选择延迟，未勾培训
- When D审核通过；U打开延迟生效确认任务，分别不填日期、不签名、选择早于确认时刻日期，再选未来有效日期并签名；到期后刷新页面
- Then 上传不要求日期；文控后直接给原作者U任务；任务提示“文件生效时间由编制人根据文件输入、输出及来源之间的衔接关系确定。”；缺日期、缺签名或早于实际确认时间均不能完成及后续受控。成功时受控时间取本节点签名时间；到选定时间前不作为已生效文件开放正文，到期按实际生效状态验收。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置要求。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后真实前端执行完成；任务自有5626暂停，浏览器关闭，原始资产按要求保留。
4. [x] Root于2026-10-18接受脚本/trace/可见页面断言，15-A至G通过，样本与时间边界保留；工程收尾另列。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## BDD: own real frontend fixture

- Given 最新 dispatch 允许本例账号配置，int_qms 8061/48061 已由 Root 核验且所有新建对象使用 DCC241016C15 前缀。
- When 通过真实管理员页面创建本例组织、角色、账号和授权，并在独立 context 用各角色真实登录验证菜单和任务范围。
- Then 只有可见页面证据完整的角色/数据写入 fixture-manifest.json 为 verified；缺正式入口、配置或运行异常准确记 BLOCKED，不用共享账号或 API 替代。

## BDD: delayed confirmation Spring mapper wiring regression

- Given Spring context 同时注册正式命名 `fileMapper` 的 infra `FileMapper` 与正式命名 `dccControlledFileMapper` 的 DCC `DccControlledFileMapper`，并注册真实 `DccDelayedEffectiveConfirmationService` 及其隔离依赖。
- When 使用真实 `CommonAnnotationBeanPostProcessor` 和 `AnnotationConfigApplicationContext` refresh 上下文。
- Then context refresh 成功，服务 `fileMapper` 字段注入的是 `dccControlledFileMapper` bean，不能注入 infra `fileMapper`；当前生产代码预期 RED 为 `BeanNotOfRequiredTypeException`，Root 修复 `@Resource(name = "dccControlledFileMapper")` 后转 GREEN。

## Spring Regression Test

- Test path: `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDelayedEffectiveConfirmationServiceSpringWiringTest.java`
- Scope: 仅验证 Spring 注入和 context refresh；其它 DCC 服务依赖使用隔离 mock bean，不调用数据库、HTTP、Flowable 或业务写入。
- Root command: `mvn -pl yudao-module-dcc -Dtest=DccDelayedEffectiveConfirmationServiceSpringWiringTest test`（由 Root 统一执行；本线程不运行 Maven）。

## Root Runtime And Regression Evidence

- Wiring、`DccDelayedEffectiveConfirmationServiceTest`、`DccDelayedEffectiveConfirmationEvidenceTest`、`DccDelayedEffectiveHmacTest` 共 21/21，0 fail/error/skip；正式 RED 已归档 `doc/tasks/20261016-dcc-24-e2e-coordination/wiring-red-junit.xml`，GREEN 进程记录为 `wiring-green-process.json`，Maven exit 0。
- 完整 reactor package-r2 exit 0；独立 `int_qms` 运行 Jar SHA-256 为 `b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071`，PID `24528`，health HTTP 200/UP；官方登录前置脚本 exit 0，仅证明环境登录可用，不代表本例业务通过。
- 本例业务仍未执行：公共 dispatch 为 `prepare_only`，`roleFixtureVerified=false`；不能用 admin 登录前置或 21 项 Spring 测试替代 U 原作者/D 文控审核人的独立角色链路。

## E2E Script Review Blockers Before Execute

- Trace privacy：当前脚本在 `runInContext` 中于 `login()` 前调用 `context.tracing.start({ screenshots: true, snapshots: true, sources: true })`，会把登录页面和认证过程纳入 trace；执行前必须调整为登录完成后再启动 trace，并在任何签名密码输入前暂停/停止 trace，禁止认证资源、密码输入或认证页面快照进入证据。
- `15-C` 缺日期：当前只等待可见 alert，没有断言具体必填错误、任务仍未完成或没有后续受控推进。
- `15-D` 缺签名：当前只等待可见 alert，没有锁定电子签名密码错误文案、任务仍未完成或没有后续受控推进。
- `15-E` 时间边界：当前只用环境变量过去时间并断言“不得早于本次确认时间”，没有记录/断言页面可见的最终签名时刻边界；应逐分支证明选择时间早于最终确认签名时刻时被阻止。
- `15-F`/`15-G`：当前只写成功提示，未断言受控时间取本节点签名时间、确认后刷新得到“受控（待生效）”、待生效正文不开放、到期刷新得到“受控（已生效）”及实际生效时间。
- Result granularity：当前成功路径写入单一 `EXECUTED_REVIEW_REQUIRED`，没有为 15-A 至 15-G 写出独立 PASS/FAIL/BLOCKED、停止位置和 cleanup 状态；这些缺口在脚本修订并经 Root review 前保持 BLOCKED。

## Parameterized Subcases

- `15-A` 申请阶段：U 在正式“受控文件提交”入口选择 `延迟生效`，不勾选 `需要培训`；断言页面不要求具体生效日期，确认摘要不出现日期字段。
- `15-B` 文控节点：D 在审批中心 DCC 行进入详情处理态，看到“文控审核”任务和“审核通过”签名入口；通过后不得把确认任务分配给 D。
- `15-C` 缺日期：U 在“延迟生效确认”面板不填“生效时间”提交，断言可见错误且任务保持未完成。
- `15-D` 缺签名：U 选择合法未来时间但不填“电子签名密码”提交，断言可见电子签名密码错误且任务保持未完成。
- `15-E` 过期时间：U 填入早于任务服务端 `serverTime` 的时间，断言“生效时间不得早于本次确认时间”，不产生成功保存结果。
- `15-F` 合法未来时间：U 填未来时间、签名原因和当前密码，断言“延迟生效确认及电子签名已保存”。
- `15-G` 到期状态：确认后到选定时刻前刷新详情，断言“受控（待生效）”、正文入口不开放；到期后再次刷新，断言“受控（已生效）”、实际生效时间可见。

## Formal Page Entrypoints

- 上传：动态菜单“受控文件提交” -> `/dcc/controlled-file/upload`；稳定锚点为 `dcc-upload-single-page-workbench`、`dcc-upload-effective-mode`、`dcc-upload-need-training` 和 `dcc-upload-submit-confirmation`。
- 审批：动态菜单“审批中心” -> DCC 模块真实列表；行内“处理/详情”进入非 viewer 详情，并要求 `dcc-approval-upload-action-panel` 与当前任务按钮可见。
- 延迟确认：DCC 详情处理态 -> `dcc-delayed-effective-confirmation`；页面提示、时间、原因、密码和确认按钮均来自正式组件。
- 状态：详情页正式事实区展示“受控日期”“延迟生效时间”“延迟确认签名时间”“实际生效时间”；状态标签来自 `PENDING_DELAYED_EFFECTIVE_CONFIRM`、`CONTROLLED_PENDING_EFFECTIVE`、`ACTIVE`。

## Role And Data Preconditions

- 角色必须是独立真实账号：U 原作者、D 文控审核人；D 的权限至少能从真实审批中心进入 DCC 处理态并看到文控签名按钮，U 能看到自己的延迟确认任务。不能用 admin 冒充任一角色。
- 任务自有数据前缀 `DCC241016C15`：项目、文件分类、源文件、文件编号和正文内容均须由页面准备并可清理；本例需要未勾培训的延迟申请、可完成前置审批的正式路由和无培训记录分支。
- 当前缺口：`dispatch.json` 尚未放行；`runtimeVerified=false`、`roleFixtureVerified=false`；dispatch 当前记录 8061 已监听，但公共 `runtime-preflight.json` 初检曾为未监听；U/D 账号标签和凭据、项目/分类/正式审批路由、可清理前置数据尚未由 Root 提供。缺口存在时不得启动浏览器或猜测账号密码。

## Script And Commands

- 脚本：`doc/tasks/20261016-dcc-e2e-15/e2e-15-delayed-effective-real.e2e.js`
- 静态语法检查：`node --check doc/tasks/20261016-dcc-e2e-15/e2e-15-delayed-effective-real.e2e.js`
- Root 放行后真实运行命令（凭据仅通过环境变量注入，不写入文件）：`node doc/tasks/20261016-dcc-e2e-15/e2e-15-delayed-effective-real.e2e.js`
- 脚本在创建浏览器前重新读取公共 `dispatch.json`；gate 不是 `execute` 时写入 `e2e-15-result.json` 为 `PREPARED_WAITING` 并退出，不产生页面或业务写入。

## 设计约束检查

- 当前线程后续只补本目录文档；Root 已完成授权的一行生产修复、统一 Maven 验证和运行态重建，本线程不再改生产/测试、不重启服务、不跑 Maven/构建、不改共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C15，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-15/repair-20261019-r4-handoff.json

- doc/tasks/20261016-dcc-e2e-15/repair-20261019-r2-handoff.json

- doc/tasks/20261016-dcc-e2e-15/pending-access-pa2-checkpoint.json

- doc/tasks/20261016-dcc-e2e-15/final-acceptance.cjs

- doc/tasks/20261016-dcc-e2e-15/pending-access-supplement.cjs

- doc/tasks/20261016-dcc-e2e-15/checkpoint.json
- doc/tasks/20261016-dcc-e2e-15/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-15/e2e-15-result.json
- doc/tasks/20261016-dcc-e2e-15/resume-ui.cjs
- doc/tasks/20261016-dcc-e2e-15/resume-credentials.cjs
- doc/tasks/20261016-dcc-e2e-15/sanitize-trace.py

- doc/tasks/20261016-dcc-e2e-15/resume-session.cjs

- doc/tasks/20261016-dcc-e2e-15/root-platform-evidence-boundary-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-auth-refresh-cache-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-distribution-effective-date-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-custom-context-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-activation-time-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-controlled-event-time-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-attachment-read-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-missing-leader-handoff.json

- doc/tasks/20261016-dcc-e2e-15/root-bpm-isolation-test-handoff.json

- doc/tasks/20261016-dcc-e2e-15/workflow-v6-transition-review.md

- doc/tasks/20261016-dcc-e2e-15/workflow-v6-review.md

- doc/tasks/20261016-dcc-e2e-15/task.md
- doc/tasks/20261016-dcc-e2e-15/execution-log.md
- doc/tasks/20261016-dcc-e2e-15/verification-report.md
- doc/tasks/20261016-dcc-e2e-15/prepared.json
- doc/tasks/20261016-dcc-e2e-15/e2e-15-delayed-effective-real.e2e.js
- doc/tasks/20261016-dcc-e2e-15/e2e-15-result.json
