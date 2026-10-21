# DCC 正文浏览时间与阅读时长

## 目标

补齐正文浏览的后端模型、接口与前端生命周期契约，保留可追溯的访问事实，严格区分打开时间和累计阅读秒数。初始委派优先后端；续办已获准并完成实际 viewer 接线，OnlyOffice 子组件仍由其他 owner 管理。

## 基线与既有任务

- 分支 int_qms，HEAD f06833f06；用户指定 a3b8234ec 已包含在历史，二者差异均为任务交付记录，DCC 源码基线相同。
- 现有 G07 已建立精确正文 context 与访问事件；G09 已有正文 ready/登记时间展示。本任务复用既有授权，不把旧任务测试当作本次证据。
- 关联并行任务：doc/tasks/20261015-dcc-full-format-viewer-audit。该任务最初负责全格式 viewer；续办时本线程已获准接入 view/index.vue 并冻结交 Root，OnlyOffice 仍由对应 owner 管理。
- 所有并行修改和历史未跟踪验证资产原样保留；共享 Query 仅追加 assertPreviewReadingAccess，既有正文读取方法不改写。

## 浏览时间口径

- PREVIEW access event 的 occurredAt / metadata previewRegisteredAt 是“预览登记时间”，表示授权/签发事实；不是正文已经显示的证明，不冒充阅读时长。
- viewer 实际 ready 后，start 被服务器接受的 startedAt 才是本次“打开时间”（客户端报告 ready、服务器授时）。同一租户/用户/资源/精确版本的 MIN(startedAt)、MAX(startedAt) 分别是首次、最近打开；没有会话时返回 null，累计为 0。
- 前台可见时每 10 秒心跳。服务器只累计连续间隔 0..15 秒；超过 15 秒整段不计入。累计不是最近打开减首次打开，也不采信客户端耗时。
- stop 可补最后 15 秒以内的有效间隔；丢失 stop 的旧会话被下一次 start 收口到最后已确认心跳，不补时。累计按精确版本汇总；跨标签页、跨 access event 仍保证同用户同资源同文件单活跃，避免重叠累计。
- 本证据表示带权限与 token 约束、由 viewer 报告的前台心跳区间，不能证明人的注意力；新会话表不宣称替代统一 GxP 不可变账本或可信硬件时间认证。
- 培训专用 progress/session、600 秒门槛与普通正文浏览分离，本任务不增加或消耗培训累计秒数。

## BDD

- BDD: 登记与打开事实分离 -> Given metadata 建立服务器访问事件，When 正文实际 ready 且 start 成功，Then 分别保存 event 登记时间与 server startedAt，首次/最近打开仅由真实 session 统计。
- BDD: 有界服务端计时 -> Given 活跃会话及匹配证据，When 心跳间隔为 10/15/60 秒，Then 分别增加 10/15/0 秒；客户端时间和报告秒数不参与。
- BDD: 停止、卸载和切版 -> Given start/heartbeat 请求尚未返回，When 页面隐藏、卸载、暂停后恢复或切换版本，Then 迟到 start 仅停止自己的 session，等待在途 heartbeat 后 stop，旧上下文不污染新版本。
- BDD: 幂等与并发 -> Given 同 event/client start 重放或同用户多标签页，When 请求进入服务端，Then 同 client 返回原会话且不会复活已关闭会话，其他 start 在资源行锁内关闭旧会话再创建新会话；生成列唯一键作为数据库约束。
- BDD: 身份与当前权限 -> Given 租户/用户/版本/event/token/nonce/context 任一不匹配，账号停用、任务改派或业务权限撤销，When 请求生命周期接口，Then 拒绝且零 session 写入与零正文 bytes 读取。
- BDD: 多格式接线 -> Given PDF/Office/图片/文本/音视频正文真正 ready，When viewer owner 接入同一 API/helper，Then 使用相同生命周期。下载专用、local blob、附件/分发专用来源、加载失败不能推断成此正文的成功会话。本项已在续办完成生产接线及实际脚本/模板行为验证；真实页面/E2E仍未验收，不由单测冒充。

## 里程碑

1. [x] 复核规则、基线、既有审计/token/viewer，实现口径核对。
2. [x] RED：新增服务行为用例；最小契约 stub 下真实执行 4 项，正向因 UnsupportedOperationException 失败，反向出现严格 mock 的 unused stub，未伪称全部为产品断言失败。
3. [x] GREEN：模型、迁移、八个接口、精确 DCC 授权复用、单活跃会话、幂等重放和汇总。
4. [x] 前端独立 API/helper 与卸载/隐藏/在途请求测试；不改并行父页。
5. [x] 定向 Maven、Node、lint、严格 helper 类型、SQL policy gate；验证报告与 cleanup。
6. [x] 实际 viewer 接线与 Root M01 软件 RED/GREEN 已完成；真实 MySQL/运行态/E2E与Git收尾保留为未验边界，不重开软件缺陷。

## 预期验证

- 原后端阶段 Maven -pl yudao-module-dcc -am 定向记录保留；本轮新增 ReadingSessionService/ControllerTest 与真实 Query/Guard 回归由 Root 统一执行，不在本线程启动 Maven/shared target。
- Node：reading API/lifecycle 与既有 G07 body context 合同。
- 新前端两文件 ESLint；pure lifecycle strict TypeScript；diff、文档结构和迁移完整依赖闭包 policy gate。
- 不执行 E2E、服务启停、真实或隔离数据库写入、Git 提交或推送；只做 Mockito/MockMvc 和静态 schema 验证。

## 设计约束检查

- 新 session 绑定 tenant/user/resource/file/version/event/tokenId/nonce/bodyContext；用户与租户来自认证上下文。原始 token 只在请求头传输，不存表、不进入请求 JSON，Controller 关闭请求参数访问日志，DTO toString 脱敏。
- DCC 每次 start/heartbeat/stop/summary 经真实 Query 入口重新核验精确任务/版本、项目、目录、类别、账号与权限；统一文件独立核验 INFRA_FILE version、event、水印与 token，不能混用对象 ID。
- start/heartbeat/stop 先验权，再锁真实资源行，锁内重做授权、读取/写入会话，所有成功写入同一事务；更新数量或插入 ID 异常直接失败。
- MySQL 迁移只 CREATE 新表，不回填任何文件、版本、签名、访问事件或培训历史；active_marker 为数据库生成列，DO 不含该字段。H2 fixture 同步，未执行数据库。
- 计时接口是有界观测证据，不替代内容权限、培训确认、下发回执或电子签名。token 到期明确报错，不能静默续期；stop 到期失败不补时，下一次合法 start 收口旧会话。
- 共享生产 hunk 为 Query interface/implementation 的单个新增授权方法；初始阶段未改父页；续办已授权修改 view/index.vue 并完成接线，OnlyOffice 保留其 owner 最新改动。

## Current Status

blocked — 本轮heartbeat/dispose与追加身份缓存边界均已实际RED/GREEN，专属41/41PASS，viewer lint/定向类型通过；源码再次冻结交Root，后端/Query/token/其他owner未改。清理preview/apply通过，旧共享宿主auth/cache适配与完整types/build由Root决定；用户禁止Git工程收尾，不能标completed。

## 续办 BDD 与验证（2026-10-15）

- BDD V01 -> Given 正规正文 source 与完整 preview metadata，When TEXT/PDF 真正呈现或当前 generation 的 IMAGE/AUDIO/VIDEO load / Office opened 到达，Then 生产 viewer 调用正式 start，之后读取 summary，分开展示预览登记时间、首次/最近打开与累计有效阅读秒数。
- BDD V02 -> Given metadata 未返回、正文未 ready、加载失败、DOWNLOAD_ONLY、local blob、附件、分发或登记证来源，When 打开 viewer 或收到陈旧 ready，Then 不调用普通正文 lifecycle，不伪造 0 秒/成功统计。
- BDD V03 -> Given 正文 ready，When document hidden/window blur、pagehide/pageshow、focus 恢复、切版或卸载，Then pause/resume/dispose 正式接线，旧 start/heartbeat/summary 响应不能污染新 generation，关闭旧请求收口后再开始新会话。
- BDD V04 -> Given 心跳/summary/token 请求失败，When 响应返回，Then 独立错误区显示真实失败，停止心跳且不在 focus 后自动重试；提供显式重新加载正文动作通过原 metadata/binary 链路取得新证据；缺少会话标识生成依赖也须记录失败且不在 focus 后自动重试。
- BDD V05 -> Given start 和 summary 在途，When 切换所选版本或卸载，Then 旧请求只携带旧 scope/token/session，迟到 summary 被丢弃，迟到 start 的会话被明确停止，无定时器残留。
- BDD V06 -> Given 原全格式工具与水印，When 增加统计区与生命周期，Then TEXT/媒体/PDF/Office 现有只读工具、水印及登记时间测试继续通过。
- 续办验证：新增实际生产 SFC 脚本/模板行为测试（RED/GREEN），复跑 API/helper/G07 登记、全格式、G09 viewer/Office/PDF、原生只读配置/缩放定向测试；定向 lint、SFC 编译/类型检查、源码 UTF-8/LF 指纹及 diff 检查。不将 E2E/实库/全量构建作为当轮门禁。
- 授权范围扩展：允许修改 view/index.vue 和必要的专属前端测试；旧“父页未接线”结论保留为历史检查点，不能覆盖本次结果。

## Cleanup Keep

- doc/tasks/20261015-dcc-preview-reading-duration/task.md
- doc/tasks/20261015-dcc-preview-reading-duration/execution-log.md
- doc/tasks/20261015-dcc-preview-reading-duration/verification-report.md
- doc/tasks/20261015-dcc-preview-reading-duration/integration-notes.md
- doc/tasks/20261015-dcc-preview-reading-duration/migration-policy-gate.json
- doc/tasks/20261015-dcc-preview-reading-duration/verification-results.json
- doc/tasks/20261015-dcc-preview-reading-duration/check-viewer-reading-types.cjs
- doc/tasks/20261015-dcc-preview-reading-duration/review-handoff.json
- doc/tasks/20261015-dcc-preview-reading-duration/root-red-restore-points.md
- doc/tasks/20261015-dcc-preview-reading-duration/continuation-evidence.json
- doc/tasks/20261015-dcc-preview-reading-duration/continuation-readonly-review.md
- doc/tasks/20261015-dcc-preview-reading-duration/round3-evidence.json
- doc/tasks/20261015-dcc-preview-reading-duration/identity-boundary-review.md


## Root 最新分工约束

本轮不执行 Maven/shared target/full types/build，由 Root 统一；仅本线程定向 Node、lint 和源码检查。既有其他线程测试 harness 从现在起不再修改；此前续办已做的最小 import/宿主字段适配如实交 Root review，不擅自回滚。后端合同保持原样。

## Root review 微秒边界补修

BDD V07 -> Given 固定服务器 Clock 与 lastHeartbeatAt，相差 15 秒、15 秒加 1 微秒或 16 秒，When heartbeat 或 stop，Then 分别增加 15/0/0 秒，按完整 Duration 判定存活窗口后才能截取整数累计秒数。仅获准修改 ReadingSessionService 和所属 Test，不改 schema/Query/Controller。新增测试先写，Maven RED/GREEN 由 Root 统一执行；本线程不得把未执行的 RED/GREEN 写成 PASS。

## Root review：controlled 阅读接口方法安全

BDD V08 -> Given 仅有 review/approve 权限但已认证的真实 LoginUser 与合法 APPROVAL_TASK/HISTORY 身份，When 通过实际方法安全代理调用 controlled start/heartbeat/stop/summary，Then 与正式正文一样进入服务器精确对象守卫，不要求额外 query 菜单。Given DAILY 无正文授权或任务失效，When 服务端 guard 拒绝，Then 拒绝传播且零 session 写入。Given 未认证/匿名，Then 方法安全先拒绝，零服务调用。统一 file-preview 四路保留原权限表达式。仅 Controller 与其专属 Test 新增写权，Maven 仍由 Root 统一，测试编写/静态推导不冒充已执行 RED/GREEN。

## Root M01 正式软件关闭回执

R01微秒边界、R02认证门禁、F01 DAILY详情软件验证均已关闭。正式来源 doc/tasks/20261015-dcc-six-thread-followup-review：RED11执行，3断言失败+4实际AuthorizationDenied，无setup错误；GREEN10类125/125、0F/E/skip，3生产文件原始字节恢复0mismatch。Root前端viewer82与详情99各自PASS/0drift，禁止与历史数字混加。本次仅文档与证据JSON更新，没有重新运行套件或修改生产/测试。

## Root最终前端/类型回执

本次Root前端两组共181PASS（Viewer82、Detail99），均0fail/skip/输入漂移；完整pnpm run ts:check exit0，2429源码输入0漂移。Workflow仅接口声明且新旧emitted runtime字节一致，未重复业务套件。Root10生产lint0error、detail两条既有换行warning保留。正式证据 doc/tasks/20261015-dcc-six-thread-followup-review；本线程只更新文档，无生产/tests/套件动作。

## 本次继续的未覆盖边界（不重开R01/R02）

- READ-CONT-01 Given pagehide已暂停，document.visibilityState/hasFocus尚未切换且尚无pageshow，When排队focus/visibility事件到达，Then不能重启start或heartbeat；仅pageshow且当前页面可见/聚焦才恢复。源码候选：handleReadingPageHide未保存页面隐藏状态，handleReadingActivityChange只看document。先写实际SFC行为RED。
- READ-CONT-02 Given后端为已关闭client的幂等start重放返回endedAt非空原会话，When阅读helper处理该start响应，Then明确报告已结束并不安装心跳/不将其当活跃会话。源码依据：startInternal existing分支明确返回原ended记录；helper.resume只检查sessionId和10秒策略。先写真实helper行为RED，不改后端既有幂等合同。
- 本轮验证范围：只运行新增两条定向Node RED与修复后受影响自有测试；Root生产前端冻结期间不改view/API/helper，不改其他owner文件/测试宿主。Maven/shared target/fulltypes/完整build仍由Root唯一执行。
- 阅读迁移仅静态复核：CREATE IF NOT EXISTS、base/protection依赖闭包、生成列唯一键、无历史DML、DO不写active_marker；不执行数据库。服务器并发与授权缓存若只有静态推导须明确标为风险，不冒充实际缺陷或已复现RED。

## 本次第二轮继续 BDD（2026-10-15）

- READ-CONT-03 -> Given 活跃会话收到后端 heartbeat 回包，When `sessionId` 不一致或 `endedAt` 非空，Then lifecycle 拒绝该回包、停止定时器、显示真实错误，绝不把回包写回 active；旧 session 仍由 pause/stop 走原 capability 收口。
- READ-CONT-04 -> Given viewer 在途 heartbeat 或 stop，When 页面隐藏/卸载触发 pause/dispose 且请求失败，Then 当前 owner 的错误区显示失败，生命周期不自动 resume，不伪造新统计；旧 owner 切源后的清理失败只留脱敏日志，不污染新 owner。
- 本轮只新增前端专属 RED/GREEN 与相邻 viewer/helper 回归；后端无新行为，不写 Java 测试、不跑 Maven。

## 本轮追加身份边界（Root定向授权）

READ-CONT-05 Given正规viewer已在actor/baseTenant/visitTenant快照下发起metadata/bytes或建立owner，When缓存任一身份变化后旧响应、Office opened、tick或summary返回，Then旧正文/Office地址/统计不发布或继续显示，不用新身份发送旧capability心跳/stop；失效明确报错且不得focus自动重开，只有显式重新加载才能按新身份重新授权。先真实SFC控制缓存和在途Promise跑RED，不凭静态推导称服务器越权。只改独占viewer/reading，Query/token/其他owner宿主不动，新增auth进口适配清单交Root。
