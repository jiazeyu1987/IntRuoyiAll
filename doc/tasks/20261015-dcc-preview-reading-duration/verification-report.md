## 本轮追加Root身份边界已修复并冻结（最新覆盖）

真实SFC先5RED再GREEN；当前自有3文件41/41PASS、0fail/skip，viewer lint0/0、实际identity/reading块定向strict0diagnostics。旧31/28/200及types/build保留对应历史，不混作本轮新执行。snapshot绑定缓存actor/baseTenant/rawVisitTenant，并校验token存在；metadata/bytes/Office ready发布与reading RPC/summary都校验，变化则清正文与统计、锁存失效，旧capability不在新身份下重发。Query/token/后端/其他owner未改。

具体实际RED、命令、旧共享宿主新增auth/cache进口适配清单、同窗口storage通知及不可撤销旧网络动作的边界，见[identity-boundary-review.md](identity-boundary-review.md)。完整冻结指纹见round3-evidence.json。没有把客户端复现称作服务器越权，也未自行运行跨owner宿主/Maven/fulltypes/build；交Root统一整合验证。

---

## 本轮最终交付：heartbeat结束回包 / dispose关闭失败展示（已冻结）

仅继续Root明确的两个候选，未重开C01-C06或R01/R02，没有扩展后端行为。

- READ-CONT-03实际RED：helper收到同sessionId但endedAt非空的heartbeat，原来未报错（expected errors1 / actual0）。现在在active赋值前检查身份、结束状态；缺失状态或已结束明确报错，report取消timer，原session仍用于pause/stop收口。原身份不一致guard本来正确，作为相邻断言保留，没有冒充新缺陷。
- READ-CONT-04实际RED：切源dispose的stop被拒绝时，页面读取不到旧会话结束失败提示（expected拒绝消息 / actual空串）。现在独立readingCloseError真实渲染“上一正文阅读会话结束未确认”及失败原因；不标记关闭成功、不把旧清理失败当新正文授权错误，新scope仍只显示自己的服务器summary。组件已完全卸载时没有存活DOM可展示，保留原拒绝与脱敏日志，不宣称旧页面仍能显示提示。
- 单纯pause/stop失败原本已在当前owner错误区显示且不会focus自动重试，新测试在修复前就PASS；没有把它记为新RED或添加无意义生产补丁。

本轮实际两处RED各1FAIL；修复后定向三项（heartbeat回包、已有pause错误、新dispose错误）GREEN，最终三专属文件31/31PASS，0FAIL/skip。原pagehide持续挂起、ended start重放、visible/focus/sourceowner和真正ready门槛、旧capability收口均保留。

命令（IntRuoyiFronted）：

```powershell
pnpm exec node --test --test-name-pattern='heartbeat reply|pause/stop failure|dispose stop failure' tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-preview-reading-viewer.test.cjs
pnpm exec node --test tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs
pnpm exec node ../doc/tasks/20261015-dcc-preview-reading-duration/check-viewer-reading-types.cjs
```

31项中dispose错误提示有实际生产模板/Vue renderer的title断言，新正文22秒统计保持不变。新生产两文件ESLint0error/0warning；实际reading block + API/helper/context/source定向strict0diagnostics。这不是完整类型/构建；Root上一轮200/200及types/build只保留历史证据，不重写为本轮执行。

只改reading-lifecycle.ts、view/index.vue和两专属tests；未改API合同/后端/DO/Mapper/SQL/OnlyOffice/其他owner或测试宿主。完整指纹见round3-evidence.json，生产再次冻结交Root。无Maven/sharedtarget/fulltypes/build、依赖改动、E2E、实库/迁移、服务、remote、Git提交推送或子Agent。本轮按Root要求到此结束，不再扩展套件或源码。

---

## 前轮历史记录（不计为本轮执行）

## 本次继续的最新结果：两个新客户端缺陷已修复并冻结

本节只描述本轮新执行。此前 Root125后端/181前端/完整types，以及本轮 GREEN 前 Root 完整Vite2635输入零漂移/3239产物，均为各自已完成阶段的有效证据，不重写为改后新执行。R01/R02保持关闭。

### 新缺陷与实际 RED/GREEN

- READ-CONT-01：pagehide只暂停一次，没有持续挂起状态。尚无pageshow时排队focus使start从1变2；pagehide后迟到正文ready也使start从0变1。实际SFC新增两条均RED。现在维护readingPageActive，pagehide置false，pageshow才置true；初次ready、visible判断和focus入口都使用该门禁，保留source owner、document visible/focus和旧capability收口。
- READ-CONT-02：后端existing client start重放可合法返回已结束记录，helper原来不看endedAt，实际新增行为测试Missing expected rejection。现在helper要求正式结束状态；endedAt非空明确报告已结束，不安装心跳、不模拟活跃成功。不改后端幂等合同，测试端口也补齐正式endedAt:null字段，没有增加缺字段fallback。
- 真实新增RED为3执行/3FAIL/0skip；修复后的同3用例3/3PASS。实际失败内容与最终指纹见continuation-evidence.json。

### 本轮验证

工作目录IntRuoyiFronted：

```powershell
pnpm exec node --test --test-name-pattern='pagehide remains suspended|body becoming ready after pagehide|idempotent start replay' tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs
pnpm exec node --test tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs
pnpm exec node --test tests/unit/dcc-g07-preview-registration.test.cjs tests/unit/dcc-full-format-viewer-contract.test.cjs tests/unit/dcc-g09-viewer-tools.test.cjs
```

对应：新增3项GREEN；自有28/28PASS；相邻26/26PASS，均0FAIL/skip。只在新行为改动后运行这些受影响范围，没有重跑旧Maven/大类型/完整构建。

当前viewer/helper/API三文件ESLint 0error/0warning；`pnpm exec node ../doc/tasks/20261015-dcc-preview-reading-duration/check-viewer-reading-types.cjs`实际reading AST+真实API/helper/context/source定向strict为0diagnostics，仍不是完整types。

迁移本次8个静态断言及声明依赖文件检查通过，无数据库动作。服务器stop/heartbeat/row锁与授权路径只读依据，以及完整运行schema/授权锁等待缓存的未验证边界，见[continuation-readonly-review.md](continuation-readonly-review.md)。没有确认需本轮改写的新后端行为，因此没有新增Java/SQL修改或待Root执行的后端RED测试。

### 本轮实际文件与指纹

生产只改view/index.vue、reading-lifecycle.ts；专属测试只改dcc-preview-reading-viewer.test.cjs、dcc-preview-reading-lifecycle.test.cjs。API wrapper、Java、DO、Mapper、SQL、Query、OnlyOffice、其他owner文件及测试宿主均未修改。

- view/index.vue UTF-8/LF SHA-256：`09ea0b785198591e27ba602e3d44c2eb57764aa81c0c452ade7f8dff019e689a`
- reading-lifecycle.ts UTF-8/LF SHA-256：`16acc85ca3742983f48fafe141df8c8c5d266b13cb141cbf6947ca50ed070840`

完整本轮指纹见continuation-evidence.json；当前生产已再次冻结交Root。Root自行决定改后集成types/build是否需要，不能把GREEN前的完整构建当作这两个改后文件的构建证明。本轮没有Maven/sharedtarget/fulltypes/build、依赖变更、E2E、实库写入/迁移、服务、remote、Git提交推送或子Agent。

---

## 上轮已关闭与历史回执（不计为本轮执行）

## Root 最终正式回执（覆盖所有待后端/类型验证历史状态）

- M01：-am 定向10类 fresh XML **125/125 PASS，0 failure/error/skip**；3个临时恢复生产源均匹配原GREEN字节指纹，0 mismatch。R01/R02/F01软件验证已关闭。
- 前端：本次 Viewer12文件82 + Detail12文件99，**本次合计181 PASS**；各组无fail/skip且对应源码输入零漂移。这里仅汇总本次Root两个正式分组，不混加本线程旧25/83/94/115等历史计数。
- 完整类型：Root补齐signatureSummary.processInstanceId正式字段后，**pnpm run ts:check exit0，2429个源码输入0漂移**；不再有前端类型待验或缺依赖阻塞。
- Workflow补丁仅接口声明；Root已验证新旧TypeScript emitted runtime字节相同，因此未重复业务套件。不是本线程改生产或重跑。
- Root当前10个生产文件lint exit0、0 error；仅detail有2条既有换行warning，未写成全零warning。本线程原3文件定向lint0/0仍只是其当时范围。
- 正式证据统一引用 [Root六线程跟进记录](../20261015-dcc-six-thread-followup-review/verification-report.md)。本次只更新自有文档/证据JSON；所有生产/tests继续冻结，不重复套件，不参与Root正在执行的最终指纹或清理。
- 剩余边界仅为真实运行、MySQL迁移/多连接、浏览器Office/媒体与退出网络、E2E、Git提交/推送工程收尾未验；不能用这些边界重开已关闭的软件缺陷。

---

## 当前正式结论：R01 / R02 / F01 软件验证已关闭

Root 已完成唯一 M01 实际 RED → GREEN，-am 定向 10 类 Maven exit 0，fresh XML **125/125 PASS，0 failure/error/skip**。本任务不再有“待 Maven”“测试装配/缺依赖”软件阻塞；源码与测试继续冻结，本线程没有自行重跑或写生产代码。

正式证据以 [Root 六线程跟进目录](../20261015-dcc-six-thread-followup-review/) 为准，尤其是 [backend-red-summary.json](../20261015-dcc-six-thread-followup-review/backend-red-summary.json)、[backend-green-summary.json](../20261015-dcc-six-thread-followup-review/backend-green-summary.json)、[backend-red-state.json](../20261015-dcc-six-thread-followup-review/backend-red-state.json) 和 Root execution-log.md。本节覆盖后文所有“等待 M01/待运行”的历史检查点。

### 实际 RED 与恢复

- R01：6 个 Clock 参数执行，15s+1µs 的 heartbeat/stop 两条实际断言失败，expected 4 / actual 19；证明旧先取整逻辑错误。
- R02：review/approve-only × TASK/HISTORY 四条均被实际旧 query 表达式以 AuthorizationDeniedException 拒绝，无缺 Bean/setup 错误；真实方法安全边界得到执行。
- F01：Root 另 owner 的 DAILY 详情工作稿预览断言实际失败，expected false / actual true。该修复属于对应 owner，本任务仅引用同一验证票，不认领其实现。
- Root RED 合计 11 执行，3 断言失败 + 4 实际旧权限拒绝、0 skip；不是“11 条全部失败”。
- Root 将 3 个冻结生产文件按原始字节 SHA 恢复 GREEN，0 mismatch；本线程没有覆盖临时 RED 或修改其他 hunk。

### 正式 GREEN 分项（均包含在 125 内，不重复相加）

|类|PASS|
|---|---:|
|ReadingSession|29|
|ReadingController|12|
|SelectedDaily|6|
|SelectedDetailService|7|
|SelectedDetailHTTP|3|
|DailyBrowser|11|
|BodyReadCurrent|27|
|DetailGuard|19|
|BodyContext|3|
|OfficeToken|8|

Root FE viewer **82 PASS / 0 drift** 与详情 **99 PASS / 0 drift** 是两个独立范围，分别引用，不与旧 25/83/94/115 或其他历史数字相加。读取 Root [fe-viewer-result.json](../20261015-dcc-six-thread-followup-review/fe-viewer-result.json) 和 [fe-detail-result.json](../20261015-dcc-six-thread-followup-review/fe-detail-result.json)，均 exit 0、sourceDrift 为空。

Root 已补齐后端正式存在的 signatureSummary.processInstanceId 前端可选 string/null 声明；本任务没有修改该生产文件。只读归档时 [fe-types-green-result.json](../20261015-dcc-six-thread-followup-review/fe-types-green-result.json) 已记录 exit 0、2429 输入、sourceDrift 为空。该完整类型运行属于 Root，不冒充本线程定向类型或额外重跑，也不再列为本任务缺依赖阻塞。

### 保留的验收边界

本次软件行为验证已关闭；真实运行环境、MySQL 迁移/多连接并发、浏览器/Office/媒体实际行为与退出网络送达、E2E、Git 提交/推送仍未在本任务验收。测试端口与方法安全代理不代替真实库/页面证据。任务保持 blocked 仅因未授权 Git 工程收尾，不能用此状态重新打开 R01/R02/F01 软件缺陷。

---

## 历史记录（以下待运行状态均已被上文 Root M01 正式回执解除）


## Root 最新回执：前端合并验收通过，M01 正在执行

- Root 已接收本线程冻结源码，并回执合并 12 文件 Node 82/82 PASS、0 skip，7 个生产输入零漂移；Office 旧共享合同 9/9 已修复并通过。这是 Root 传入的独立验收事实，不冒充本线程额外重跑。
- Root 正在执行唯一 M01 后端真实 RED → GREEN：只临时恢复 Duration 旧 getSeconds 比较及 controlled 四路旧 query 权限，再恢复交付实现，不动其他 hunk。
- 当前尚未收到 M01 结果，不能把进行中写成 RED 已复现或 GREEN 已通过。Root 的临时 RED 源码不视为未经授权的漂移，本线程不读取/覆盖其 Java 临时状态。
- 所有 Java/tests 与前端继续冻结；本次仅更新自有报告，没有执行额外 Node/Maven/类型/构建或其他套件，也没有修改生产代码或测试。

---

## 2026-10-15 续办最新交付：viewer 已接线并冻结，后端 review 补修待 Root 验证

本节优先于下文原“后端阶段/未接线”历史检查点。生产 view/index.vue 已实际消费 createPreviewReadingApi/createPreviewReadingLifecycle；无需另一个线程补接线。当前前端源码已冻结交 Root review，UTF-8/LF SHA-256 见 review-handoff.json。

### 本轮实际改动

- TEXT/PDF 实际正文呈现、匹配 generation/src 的媒体 load/loadeddata、当前 Office opened 后才开始；无合同附件/分发/登记证、本地 blob、DOWNLOAD_ONLY 和正文失败不创建普通 DCC session。统一存储预览仍走独立 infra 路由。
- 增加真实统计区域：预览登记时间与累计有效阅读秒数、首次/最近打开分别呈现；统计只读服务端 summary，无本地秒表或 0 秒成功占位。
- visibility/focus、pagehide/pageshow、切版和组件卸载接入 pause/resume/dispose；旧 owner 收口后新 owner 才能 start。旧 start/summary/heartbeat 响应受原 source/证据和 generation 约束，旧响应不覆盖新统计。
- 心跳/统计/token/UUID 依赖失败均明确显示；失败 owner 不在 focus 自动重试。显式“重新加载正文后重试计时”重新经过原 metadata/binary 链路；不暗中续签 token。原格式工具、水印与预览保护逻辑保留，未修改 OnlyOffice 子组件。
- Root review 授权补修服务：完整 Duration 比较后再取整数秒，15s+1µs 不得累计 15。
- Root review 授权补修 Controller：controlled 四路改为 isAuthenticated()，随后保留真实 service/Query Guard/task/tenant/version/event/token；统一 file-preview 四路原权限不动，未授予新菜单权限。

### 实际 RED/GREEN

- 初始实际生产 SFC 集成测试：1 PASS / 9 FAIL，具体为正文 ready 后没有 start/summary、没有统计状态、没有生命周期监听与关闭。本轮 10/10 GREEN 后继续补统计区域真实 Vue renderer、成功心跳刷新、pagehide/pageshow、UUID 失败和 900 秒边界。
- pagehide 专属 RED：未绑定 window:pagehide，1 FAIL；接线后 1 PASS。
- UUID 专属 RED：初始化失败后 focus 再次同步抛错，1 FAIL；将启动失败关联到当前 owner.failed 后 GREEN。没有创建默认 client ID 或随机数替代。
- 组合回归出现的是旧测试宿主未认识新 imports/字段，以及新 renderer 宿主缺 Canvas.style、URL、ResizeObserver；修复测试适配后通过。没有将这些宿主问题当产品行为 RED。
- 在最新“不改其他线程 harness”指令之前，已做三处最小宿主适配：dcc-g07-preview-registration.test.cjs、dcc-g09-viewer-tools.test.cjs、dcc-full-format-viewer-contract.test.cjs。此后未继续写这些文件；精确快照在 review-handoff.json，交 Root 接管统一回归。

### 当前冻结源码实际 PASS

工作目录 IntRuoyiFronted：

```powershell
pnpm exec node --test tests/unit/dcc-preview-reading-viewer.test.cjs tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs
```

exit 0，**25/25 PASS、0 FAIL/SKIP**：生产 SFC/API/helper 组合 16，API 3，生命周期 helper 6。统计区使用真实生产模板 + Vue renderer 实际断言“10 秒”、首次/最近打开和登记时间为四个不同服务端事实；成功 heartbeat/pause 后以端口返回的 20/23 秒刷新，不在浏览器算时长。900 秒测试使用 HTTP 端口返回到期拒绝，验证客户端停表/显式错误/不自动刷新 metadata，不能冒称真实 HMAC 到期集成验收。

- ESLint Node API：view/index.vue、readingSession.ts、reading-lifecycle.ts，3 文件 0 error / 0 warning。
- `pnpm exec node ../doc/tasks/20261015-dcc-preview-reading-duration/check-viewer-reading-types.cjs`：实际 reading AST block + 真实 API/helper/context/source 类型，strict diagnostics=0。仅 HTTP adapter 使用与现有 wrapper 相同的声明；不是 full types 或全项目构建。
- 原全格式、PDF.js、Office、G07 source/registration 组合在中途检查点 **83/83 PASS**。该数对应新增 pagehide/UUID 末尾补修前的检查点，不能替代当前冻结源码的 Root 统一回归。
- 本轮未启动 Maven、未写 shared target、未执行 full types/build。初始后端 94/94 是本任务之前阶段的实际记录；Root 的 115/115、77/77 是 Root 传入的独立验证回执，不冒充本线程新运行。

### 后端必须交 Root 的待运行类

1. DccOnlineFilePreviewReadingSessionServiceTest：新增 clockMicrosecondLivenessBoundary 六个真实固定 Clock 场景，heartbeat/stop 各覆盖 15s、15s+1µs、16s。已先写测试再改实现；未执行本次 RED/GREEN。Root 可仅恢复旧 getSeconds/整数比较两行做 RED，再恢复完整 Duration 比较做 GREEN；预期 15s+1µs 两场景暴露旧行为，这只是预期。
2. DccPreviewReadingSessionControllerTest：实际 Spring AuthorizationManagerBeforeMethodInterceptor + CGLIB 代理；完整 ss bean 与表达式处理器已装配，恢复旧 annotation 应是 query 权限拒绝，不能拿 missing bean 当 RED。四个 review/approve × TASK/HISTORY 正向、两种未认证/匿名、两种 DAILY/失效 task 的实际 reading service + 显式 Query guard 拒绝端口，以及原统一权限表达式合同。未执行本次 RED/GREEN，装配能否运行仍以 Root Maven 回执为准。
3. 真实 Query/Guard 的任务改派/账号停用/历史身份强回归保留原测试，由 Root 选其统一票；本轮没有改 Query、schema 或其他 Controller。

### 源码与并行边界

view/index.vue SHA-256：`42253a6c8ead376e2135fb259d8368ae34cc77091c41e9a31c62efff4b29e8df`。

完整 10 个本线程交付源码/测试指纹见 review-handoff.json。OnlyOffice 当前已被其他线程更新为 `e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1`，本线程未写入/回滚。之前旧 OnlyOffice 指纹不再用于宣称当前子组件不变；真实 Office 行为与全格式合并回归由 Root 覆盖。

### 未验证边界与状态

后端两项 review 补修的正式 RED/GREEN、真实浏览器视觉/焦点与 iframe、关闭页签网络送达、真实 Office/媒体、MySQL 迁移/多连接事务仍未在本轮验证。没有 E2E、真实 DB、服务、Git 提交/推送、remote 或 subagents。未虚构数据库落地或部署成功。工程状态保留 blocked；前端实现冻结交 review，后端等待 Root 验证票。

---



## 原后端阶段历史检查点（不覆盖上文最新状态）

# Verification Report

## 结论

后端模型、八个接口、精确上下文授权复用、幂等/单活跃约束、只读统计和前端独立 API/helper 已实现。最终软件验证：Maven 94/94、Node 21/21 PASS，0 failures/errors/skips。没有修改 view/index.vue 或 OnlyOffice 组件；实际页面接线仍由 viewer owner 完成。本报告不声称端到端需求已上线。

## 口径

- access event occurredAt / previewRegisteredAt：预览登记、授权与签发事实。
- session startedAt：客户端报告正文 ready 被服务器接受的本次打开时间；首次/最近打开按精确版本内 MIN/MAX startedAt。
- accumulatedSeconds：服务器确认的有效心跳区间，10 秒上报、间隔超过 15 秒该段计 0；不以 last-first、水印时间或客户端秒数替代。
- 多标签页跨 event 单活跃；遗留会话收口到最后心跳，无卸载请求时不补尾部。已结束 client start 重放不会复活旧会话。
- 普通浏览不参与培训 600 秒门槛，也不替代统一 GxP 账本或对注意力的证明。

## RED / GREEN / REGRESSION

1. 初始 Maven 命令因 PowerShell 拆分带点属性失败，修正参数引号；这是命令问题，不是业务 RED。新增契约缺失及 Mockito BaseMapper 重载歧义的编译失败也单独记录，不当作行为断言失败。
2. 最小 session 服务 stub 下执行四项 JUnit：4 errors，正向用例真实触发 UnsupportedOperationException；反向 fixture 因 stub 未消费而触发严格 Mockito unused stubbing。这是新生命周期尚未实现的开发 RED，不冒称四条业务拒绝逻辑均已证明有缺陷。
3. 实现后首次定向 30 PASS（session 21 + unified preview 6 + controller 3）。
4. 扩展 exact DCC guard/HTTP/schema/summary 后 85 PASS；最后追加真实配置密钥 HMAC 与 Office token 回归，得到最终 94 PASS。
5. 新增前端 helper 文件前 5 RED，API 文件前 3 RED，均为目标模块不存在的真实 ENOENT；实现后 8 GREEN，再追加暂停期间恢复竞态与 G07 邻域，最终 21 PASS。
6. SQL policy 首次仅选新迁移，因未包含 dependsOn 报 missing base schema；显式提供 base/protection/new 三项完整闭包后 PASS。没有执行 SQL。

## 最终后端命令

在 IntRuoyiBackend 执行：

```powershell
mvn -q -pl yudao-module-dcc -am "-Dtest=DccOnlineFilePreviewReadingSessionServiceTest,DccPreviewReadingSessionControllerTest,DccPreviewReadingSchemaContractTest,DccControlledFileBodyReadCurrentSourceTest,DccControlledFileBodyReadContextTest,DccControlledFileDetailAuthorizationGuardTest,DccOnlyOfficePreviewTokenServiceTest,DccOnlineFilePreviewServiceTest,DccOnlineFilePreviewControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

exit 0。该属性用于 -am 依赖模块没有指定测试时继续；最终九个目标类均有实际 XML，不将缺失类当 PASS。各 XML SHA-256 和计数保存在 verification-results.json。

|测试类|通过数|
|---|---:|
|DccOnlineFilePreviewReadingSessionServiceTest|23|
|DccPreviewReadingSessionControllerTest|3|
|DccPreviewReadingSchemaContractTest|2|
|DccControlledFileBodyReadCurrentSourceTest|27|
|DccControlledFileBodyReadContextTest|3|
|DccControlledFileDetailAuthorizationGuardTest|19|
|DccOnlyOfficePreviewTokenServiceTest|8|
|DccOnlineFilePreviewServiceTest|6|
|DccOnlineFilePreviewControllerTest|3|
|合计|94|

Session 测试覆盖：10/15/60 秒、重复心跳、关闭重放、跨会话收口、错租户/用户/版本/event/watermark、到期 token、忽略租户、权限撤销、更新失败、时钟倒退、summary 精确身份、显式配置密钥的实际 HMAC 验证和 nonce 篡改。Query 新增四项使用真实 Query + Guard + infra provider，证明准确 task 合法、改派/停用/错 round 拒绝且零 storage bytes / 新 metadata event；服务依赖端口仍为 mock，不能声称真实 BPM/MySQL 验收。

## 前端与静态验证

在 IntRuoyiFronted 执行：

```powershell
pnpm exec node --test tests/unit/dcc-preview-reading-api.test.cjs tests/unit/dcc-preview-reading-lifecycle.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs
```

21 PASS（新增 API 3、lifecycle 6、既有 G07 12），0 fail/skip。API 测试执行真实 TS wrapper 的转译结果与 mock HTTP 端口，helper 测试使用确定性 Promise/timer；未启动浏览器。

- ESLint Node API 检查 readingSession.ts 与 reading-lifecycle.ts：0 errors / 0 warnings。
- TypeScript createProgram：pure reading-lifecycle.ts strict + ES2022 + noEmit，0 diagnostics。未跑全项目 ts:check，不以此证明整个 Vue 项目类型通过。
- git diff --check：本任务相关 tracked 文件 exit 0；CRLF 转换提示不是 whitespace failure。新文件结构和文档另行检查。
- migration policy gate：显式 base/protection/new 三个 --sql-file，status passed / migrationCount 3，证据见 migration-policy-gate.json。只校验正式文件与完整依赖，不连接数据库。
- MySQL schema 静态合同断言仅新增表、active_marker 生成列、client 幂等唯一键、用户文件单活跃唯一键和精确版本统计 SQL。H2 fixture 已同步，未执行隔离数据库。

## 未验证边界与交接

- view/index.vue / OnlyOffice 并行 owner 尚未接入新增 API/helper。PDF/Office/图片/文本/音视频真实 ready、visibility/focus、unmount 和错误区的 UI 接线仍需完成，具体步骤见 integration-notes.md。
- 未执行新迁移；运行库不存在新表时应明确报错，不能返回默认成功。未验证实际 MySQL 首次/重复建表、租户插件 SQL、生成列行为、多连接并发与事务回滚；Mockito/静态唯一键合同不能替代这些证据。
- 未连接真实 BPM、OnlyOffice、对象存储或浏览器服务，未做服务启停/E2E。真实加载失败与浏览器退出网络请求送达不能由单测推定。
- 当前 token TTL 为 900 秒，没有自动续期协议；过期请求按原 verifier 明确失败。既有共享 viewer token 服务保留历史默认密钥分支，本任务没有新增或修改该分支；新增真实 HMAC 测试显式配置密钥，不证明生产配置就绪。
- 没有历史阅读时长回填，旧 metadata 登记不能倒推为阅读。统计只覆盖新生命周期真正接入后形成的会话。
- 用户当轮禁止 Git 提交/推送；依 docs/task-closeout-rules.md 不能标工程 completed。清理检查后记录 blocked，并保留所有并行资产。

## Cleanup

已先设 ready_for_closeout，归档核心 RED/GREEN/PASS/限制后，cleanup preview/apply 均 exit 0；保留 6 个核心文件，删除 7 个临时日志与 1 个 fragment。清理后设 blocked，不标工程 completed。
