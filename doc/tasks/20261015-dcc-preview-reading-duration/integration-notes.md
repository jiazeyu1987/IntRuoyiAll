## 最新追加身份快照交接

READ-CONT-05实际5RED→GREEN，当前专属41/41PASS。viewer正规远程source捕获actor/baseTenant/rawVisitTenant+token存在，所有旧响应/ready/reading RPC在发布/发送前检查；失效清正文、Office地址、统计并锁存，只有明确reload重新捕获。不用新身份发送旧stop，不伪造旧网络已撤销或服务器零执行。只读Query/token与OnlyOffice不动。

新增auth/cache模块及三旧宿主适配精确清单已在identity-boundary-review.md，交Root统一，未修改他人宿主。新完整types/build由Root决定，旧成功回执不能覆盖改后源。源码再次冻结，指纹round3-evidence.json。当前只是客户端实际复现/修复，不称服务器越权；同窗口身份全局同步通知须auth/cache owner定义，本线程未越界修改。

---

## 本轮冻结交接：两个新前端边界已修复

ended heartbeat回包在active赋值前被拒绝，停止timer且明确显示错误；dispose stop失败增加独立“上一正文阅读会话结束未确认”提示，不吞错、不模拟关闭成功、不污染新scope summary。pause stop拒绝原本正确，已确认测试PASS，没有无意义改写。只改helper/viewer及专属tests，API合同与后端不变。

本轮真实RED→GREEN，最终专属31/31PASS、lint/定向类型通过，完整指纹round3-evidence.json。生产已冻结，按Root要求结束本轮。旧200/200/types/build不是本轮执行，未新增Maven/fulltypes/build或其他owner修改。

---

## 本次继续交接：pagehide持续挂起 / 已结束start重放已修复

本轮实际3RED→3GREEN，自有28/28、相邻26/26通过。只改view/index.vue和reading-lifecycle.ts及两专属测试；新字段endedAt来自既有正式API，未改后台合同。pagehide后即使document状态尚未更新，focus或迟到ready也不会先于pageshow开始；endedAt非空的重放明确报已结束，不建立心跳。继续保留当前source owner与真正ready/visible/focus、旧capability pause/stop和错误展示。

源码已再次冻结，完整指纹/新执行计数见continuation-evidence.json。Root刚完成的Vite是本轮GREEN前源码证据，改后是否追加集成types/build由Root决定；本线程未运行这些命令。后端/迁移本轮只读核对与边界见continuation-readonly-review.md，不把静态推导写成后端新RED或已运行库验证。旧R01/R02关闭状态不变。

---

## Root最终回执：软件与完整类型已通过

本次Root M01 fresh125/125PASS，前端Viewer82+Detail99=181PASS，完整ts:check exit0且2429输入0漂移。Workflow接口声明补丁的新旧emitted runtime字节相同，Root未重复业务套件；生产lint0error，detail既有两条换行warning保留。本次所有后端/类型待验已关闭，以下历史“待Root”不再是当前阻塞。正式证据 doc/tasks/20261015-dcc-six-thread-followup-review；所有生产/tests冻结，本线程只更新文档，运行/MySQL/E2E/Git边界仍保留。

---

## 当前正式状态：Root M01 已关闭软件验证

R01/R02/F01 已由 Root 实际 RED/GREEN 关闭，10类125/125PASS、0F/E/skip。ReadingSession29与ReadingController12均正式通过，不再待Maven或装配/依赖验证。Root viewer82与详情99各自PASS且零漂移，源码继续冻结。证据唯一指向 doc/tasks/20261015-dcc-six-thread-followup-review，本线程没有重跑或写生产。运行/MySQL/E2E/Git边界仍保留。下文“待Root验证”属于历史阶段，已解除。

---

## 当前交接：viewer 已接线，前端冻结（2026-10-15）

本节覆盖原“尚未接线”检查点。view/index.vue 已在真实 ready 路径消费 API/helper，显示独立 reading-summary 与 reading-error；visibility/focus/pagehide/pageshow/unmount/切版均已绑定。25 个专属 Node 测试通过，实际渲染累计、首次/最近打开和登记时间，且没有混用无合同来源。完整最新证据/指纹见 verification-report.md 顶部与 review-handoff.json。后端不再需要另一个 owner 补前端接线。

当前新增后端 review 修改仅 service / service test / reading Controller / ControllerTest；两类测试等待 Root 统一 Maven，不应使用历史 94 PASS 关闭新微秒/认证门禁问题。方法安全测试是实际拦截器代理；DAILY/失效任务拒绝链测试的 Query 端口为显式 mock，不宣称真实 BPM/DB。

三处其他线程测试宿主适配发生在最新“不要修改其他 harness”指令之前，之后已停止写入：dcc-g07-preview-registration.test.cjs、dcc-g09-viewer-tools.test.cjs、dcc-full-format-viewer-contract.test.cjs。请 Root 接管其最终回归。OnlyOffice 子组件已被并行 owner 更新，本线程未修改，当前外部指纹归档供 Root 合并复核。

---



## 原后端阶段历史检查点（不覆盖上文最新状态）

# Viewer Integration Note

## 交付边界

已实现后端模型、接口、只读汇总及前端独立 API/lifecycle helper。**view/index.vue、OnlyOffice 组件尚未接线，本任务未修改这两文件，不能宣称页面已经开始记录阅读时长。** viewer owner 可直接复用以下两个新增文件：

- IntRuoyiFronted/src/api/dcc/controlledFile/readingSession.ts：createPreviewReadingApi(scope, metadata)，复制当前证据与规范化正文 header，旧请求不再读取可变的新 source。
- IntRuoyiFronted/src/views/dcc/controlled-file/view/reading-lifecycle.ts：createPreviewReadingLifecycle({api, visible, newClientId, schedule, onError})。resume/pause/dispose 均返回 Promise；关闭时应处理失败，onError 绑定现有错误区，不吞异常。

## 正式接口

|资源|生命周期 POST|只读 GET|
|---|---|---|
|DCC 正文|/dcc/controlled-files/{controlledFileId}/reading-session/{start,heartbeat,stop}|/dcc/controlled-files/{controlledFileId}/reading-summary|
|统一存储预览|/dcc/file-preview/files/{infraFileId}/reading-session/{start,heartbeat,stop}|/dcc/file-preview/files/{infraFileId}/reading-summary|

DCC 需原 metadata/binary 相同的 X-DCC-Body-Read-Context header。所有 POST 通过 X-DCC-Viewer-Token header 传原 viewerToken，JSON 仅包含 accessEventCode、watermarkTraceCode、viewerTokenId、viewerTokenNonce，start 额外 clientSessionId，heartbeat/stop 额外 sessionId。GET summary 通过既有五个 preview header 传证据。用户/租户走当前认证上下文，不能从客户端 body 声明。

start 返回 sessionId（前端规范为字符串）、openedAt（预览登记 UTC 时间）、startedAt（ready 接受 UTC 时间）、lastHeartbeatAt、endedAt、accumulatedSeconds、heartbeatIntervalSeconds=10、maxHeartbeatGapSeconds=15。summary 返回当前用户当前精确版本 firstOpenedAt、lastOpenedAt（MIN/MAX startedAt）、accumulatedSeconds、openedSessionCount；无会话返回 null/null/0/0。

## 接线顺序

1. 只为 DCC_CONTROLLED_FILE 的普通正文 context 或统一存储预览创建 scope。附件、分发、登记证等专用来源没有本次 lifecycle 授权合同，不得改写成普通正文或猜业务 ID。
2. 在 PDF 第一批实际正文渲染、文本完成载入、Office 当前 generation opened、图片 load 或媒体实际 loadeddata 后创建/启用 lifecycle。metadata 单独成功、下载专用格式、local blob、加载失败均不能 start。
3. visible 同时检查 document.visibilityState、document.hasFocus()、当前 generation 未卸载且实际正文仍成功。每 10 秒 schedule 心跳；visibility hidden、blur、route leave/unmount、viewer error、版本/source 切换调用 pause/dispose。
4. visibility/focus 恢复且同 generation 正文已 ready 时 resume，生成新的 clientSessionId（如 crypto.randomUUID）；不要重放已结束的 client ID。
5. 换版时 dispose 旧实例，为新 metadata 创建新 API 和新实例。旧实例捕获旧 scope/证据；迟到 start 自行 stop，pause 等待在途 heartbeat 后 stop，不能向新版本补写。
6. 卸载网络请求能否真正送达受浏览器限制。不得用未认证 sendBeacon 或将 token 拼到 URL；未送达时服务端不补尾部时长，新合法 start 会收口旧 session。
7. token 900 秒后明确失效；当前没有自动换 token 协议，不得静默续期。若需长时间阅读，应通过原正规 metadata/binary 流程重新加载当前同版本并创建新生命周期，不能复用到期 token。
8. 界面必须分清“预览登记时间”“首次/最近打开”“累计有效阅读时长”，不能把 watermark.timestamp 或 last-first 展示为累计。

## 数据与验证边界

新增正式迁移 IntRuoyiBackend/sql/mysql/20261015_dcc_preview_reading_session.sql；依赖闭包 policy gate 已通过，仅创建新证据表、生成列单活跃唯一键和幂等 client 键。不回填历史阅读时长。H2 测试 fixture 已同步，但没有执行真实/隔离数据库写入。

服务端无正文 bytes 读取的授权入口为 DccControlledFileQueryService.assertPreviewReadingAccess；复用 exact body context/guard/business provider/token。已有培训 600 秒计时独立，不共用表与累计值。

软件验证包含真实 Query/Guard/infra provider 的账号停用、任务改派、错误轮次；显式配置密钥的真实 HMAC token 测试；前端 helper 竞态和 API transport 测试。没有真实 Office/PDF/媒体页面、MySQL 多连接并发、数据库迁移、服务启停或 E2E 证据。


## Root review：Duration 微秒边界（待 Root 验证票）

现有 advance 先 getSeconds 再比较，使 15.000001 秒错误累计 15。修复只改完整 Duration 比较，不改变 API/表/Query/Controller。所属 Test 新增 clockMicrosecondLivenessBoundary 参数化行为测试，固定实际 service Clock，分别覆盖 15s、15s+1µs、16s 的 heartbeat/stop，共 6 场景。当前本线程不执行 Maven/shared target；Root 可临时恢复该 service 原两行执行 RED（预期超限两个场景失败），恢复完整 Duration 比较后执行 GREEN。本段是预期，不是已执行证据。

## Root review：认证门禁对齐（待 Root 统一 Maven）

controlled start/heartbeat/stop/summary 原 query 菜单门禁会在正式正文 Guard 前误拒绝合法 review/approve-only 审批人。现授权将四路改为 isAuthenticated()，与真实正文一致；仍逐次进入服务端 assertPreviewReadingAccess + exact task/history/tenant/version/event/token，未扩大统一 file-preview 四路，也不授予任何菜单权限。ControllerTest 将用实际 AuthorizationManagerBeforeMethodInterceptor 方法安全代理运行认证边界；DAILY/失效任务用真实 reading service 和显式 guard 端口拒绝验证传播与零写，不冒称 mock 端口是实际 BPM/DB。真实 Query/Guard 对应边界保留既有专属行为测试，由 Root 合并验证。
