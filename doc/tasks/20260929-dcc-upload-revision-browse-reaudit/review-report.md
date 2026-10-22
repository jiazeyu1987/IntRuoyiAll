# DCC 上传、升版、浏览当前源码静态复审

审查日期：2026-09-29。主任务：`doc/tasks/20260929-dcc-upload-revision-browse-reaudit/`。本报告可独立交接，不依赖当前电脑盘符、聊天记录或运行环境。

## 结论与证据边界

本轮登记 **14 项当前代码逻辑问题：3 项 P1、10 项 P2、1 项 P3**，另列 **8 项条件风险/待确认项**。后续已按分组修复部分代码并运行定向静态/单元检查；没有运行 E2E、浏览器业务操作、业务 API 或数据库写入。下文“确认”仅表示触发条件和源码缺口可以静态串联，不表示已经在真实页面复现。

后续修复状态：URB2-004 至 URB2-011 已由子 Agent 实现并经主 Agent review；URB2-001/002/003/012/013/014 已由主 Agent 实现。定向静态合同、前端类型检查、DCC 编译及 227 个 DCC 定向单测已通过。上述 PASS 不包含 E2E、运行服务、真实数据库或真实权限矩阵。URB2-003 的跨失败事务审批证据持久化仍是残余风险：当前代码对缺少完整冻结证据的重试明确拒绝，不能声称已实现“审批已持久成功、派生失败后可恢复”的完整闭环。

基线分支 `int_qms`，HEAD `a9bcb6d36d96145ddc1252f111347b644b328deb`。审计的是当前工作区（开始时 380 条既有改动），不是 HEAD、已部署服务或其他 worktree。源码路径/方法锚点见下表，规范化 UTF-8/LF 的 SHA-256 指纹见本任务 verification-report.md；接收方源码变化时必须重新核对。

新编号使用 **URB2-001 至 URB2-014**，与历史 URB-001 至 URB-017 区分。URB2-004 起因于历史 URB-006；当前页面已补 raw/scoped session 对应，刷新后仅剩服务端身份投影的条件风险。不把历史 17 项直接计为当前 17 项失败。

## 三条链路及覆盖范围

1. **上传**：项目模板/分类/类别/目录选择 → SOURCE、DRAWING_PDF、ATTACHMENT 临时上传 → 后端按业务上下文重写 session、验证票据 → submitter 组装身份/培训值/附件集合 → POST submit 幂等创建文件与 Master/附件/源文件归属 → 冻结路线和部门负责人 → UPLOAD 会签/批准 → 按 needTraining 上传培训记录 → 人工分发 → 文控审核 → 盖章、生效、Master 指针更新。
2. **升版**：浏览页选择具体版本 → 检出（Master 锁、权限及归属）→ 新源文件/配套 PDF 上传或备注修改 → 检入分配大/小版本、创建 WORKING → 浏览版本选项/送审预检 → submitWorkingIteration 持久化培训值、选择 UPLOAD 或 REVISION → 同上审批及最终生效 → 旧 ACTIVE 和低版本 WORKING 收口。首次未生效 NEW 返工与正式文件修订分别核对。
3. **浏览**：目录/筛选 → browser-page 查询、权限过滤、逻辑文件聚合、分页 → 当前/历史版本选项 → 详情及路线/签名/培训/分发投影 → preview-metadata、令牌、受控二进制 → PDF/Office/其他媒体显示；同时检查浏览页检出、检入、送审、时间维护传递的身份。作废仅作为历史状态/共用详情加载分支，不作为本轮独立业务验收。

覆盖重点为上述主调用链、失败/重试窗口、异步回写、版本字段、审批证据和授权衔接。未做全仓穷尽证明，未检查运行环境连通性、迁移实际执行情况或真实数据完整性。共用缺陷只记一次。

## 源码索引

下文“U:840”表示本表 U 文件第 840 行；行号为本轮工作区位置，方法名为稳定锚点。

| 标识 | 仓库相对路径 |
|---|---|
| U | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` |
| US | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts` |
| B | `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue` |
| D | `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` |
| V | `IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue` |
| W | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java` |
| Q | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java` |
| F | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java` |
| UP | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadServiceImpl.java` |
| C | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java` |
| M | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java` |
| A | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentServiceImpl.java` |
| H | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileVersionHistoryRespVO.java` |
| S | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSourceUploadSession.java` |
| CV | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccOnlyOfficeDocumentPdfConversionService.java` |
| POL | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicy.java` |
| SB | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSignatureBindingService.java` |
| FAIL | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationFailureService.java` |
| AD | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledContentAdapter.java` |
| AUTH | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileDetailAuthorizationGuard.java` |
| DIR | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/directory/DccDirectoryController.java` |
| CAT | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileCategoryController.java` |
| BPM | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java` |
| BI | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java` |
| BL | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/framework/flowable/core/listener/BpmProcessInstanceEventListener.java` |
| BE | `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/framework/flowable/core/event/BpmProcessInstanceEventPublisher.java` |
| SIG | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSignatureVerificationServiceImpl.java` |
| API | `IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts` |
| PA | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPaperDistributionAckServiceImpl.java` |
| LC | `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/ControlledContentLifecycleCoreService.java` |

## 问题总览

| 编号 | 级别 | 主要链路 | 问题 |
|---|---|---|---|
| URB2-001 | P1 | 升版/浏览 | 版本选项丢失培训值，刷新后送审把 true 写成 false |
| URB2-002 | P1 | 上传/升版生效 | 图纸最终盖章不使用配套 PDF，审批预览与生效来源分离 |
| URB2-003 | P1 | 生效失败重试 | 重试绕过完整审批签名校验，失败回滚与审批完成事实脱节 |
| URB2-004 | P2 | 上传重试 | 原始 session 与后端带业务前缀 session 比较，重试放行永远不命中 |
| URB2-005 | P2 | 上传取消/切换 | 请求失效后 loading 和附件尝试记录未复位 |
| URB2-006 | P2 | 上传目录 | 旧类别目录响应能覆盖新类别目录 |
| URB2-007 | P2 | 培训证据上传 | 取消/重开培训弹窗后，旧上传响应仍能写回并被提交 |
| URB2-008 | P2 | 首次上传返工 | NEW 工作稿送审也强制检查 REVISION 路线 |
| URB2-009 | P2 | 上传/升版 | 前端固定两段版本格式，拒绝后端支持的多段版本 |
| URB2-010 | P2 | 图纸检入 | 只更新配套 PDF 被当作正文无变化 |
| URB2-011 | P2 | 连续修改 | 旧 WORKING/返工前驱被当作另一未完成流程，阻断后续检出 |
| URB2-012 | P2 | 详情/审批浏览 | 合法文件参与者被辅助目录/类别菜单权限挡住整个详情 |
| URB2-013 | P2 | 详情刷新 | 首次代次检查之后的辅助异步加载仍可覆盖较新详情状态 |
| URB2-014 | P3 | 培训完成反馈 | 实际进入分发，却提示已进入文控批准 |

## 确认问题明细

### URB2-001 [P1] 浏览版本选项丢失 needTraining，送审会取消已选择的培训

- **触发条件**：检入时勾选培训，产生 needTraining=true 的 WORKING；重新加载浏览页后选择该工作稿并送审。立即检入后的本地 merge 可能暂时保留字段，不能代表刷新后的合同正确。
- **调用链/依据**：Q:2552 给顶层行设置 needTraining；H 的历史版本 VO 完整字段列表没有 needTraining，Q:2986 `buildVersionHistory` 也未投影。B:1620 `buildCurrentVersionOption` 和 B:1647 `hydrateCurrentBrowserVersionActionState` 均未补该字段；B:1673/1711 从这些选项返回所选版本；B:1901 发送 `Boolean(file.needTraining)`，undefined 因而变为 false。W:605、646-665 将该值写回业务记录及 BPM，M:55 起 SQL 现已真实持久化。
- **影响**：培训 checkbox 的选择被静默取消，批准后跳过应有培训。B:1986 检入弹窗默认值同样受字段缺失影响。历史 URB-001 修复了后端漏存，但不能解决这里的读出/传递缺口。
- **修复边界**：每个具体版本返回自己的 needTraining；前端完整保留，缺失时报合同错误，不用其他版本或 Boolean(undefined) 推断。送审若不允许更改培训，服务端可直接消费已保存值，但须统一请求合同。
- **验收 BDD**：Given true/false 两个工作版本，When 检入后刷新、选择对应版本并送审，Then 所选值、请求、数据库、BPM 网关一致；true 必须进入培训。覆盖立即送审与刷新后送审。

### URB2-002 [P1] 图纸最终盖章忽略 drawingPdfFileId

- **触发条件**：DWG/SLDPRT/SLDASM/SLDDRW 源文件与有效配套 PDF 一起提交/检入，并完成到文控审核。
- **调用链/依据**：Q:1958-1999 在未生效图纸预览时选择 drawingPdfFileId；Q:852-878 校验检入配套 PDF。F:677 `resolveStampedPublishedArtifact` 却调用 F:719 `resolveSourceFileId`，只选 sourceFileId/originalFileId，完全未读取 drawingPdfFileId；非 PDF 进入 F:689 `convertToPdf`。CV:40 起仅接收选中的单个源文件，转换命令无配套 PDF 上下文。
- **影响**：审批人所见配套 PDF 没有成为生效盖章输入；系统无谓依赖 CAD 转换路径，即使配套 PDF 有效也可能因转换失败不能生效。这里不声称已实际验证某部署的转换器支持范围；已确认的是正式生效来源与已上传/已预览 PDF 脱节。
- **修复边界**：按版本、源类型和正式产物角色确定盖章输入；图纸使用本轮已绑定且经校验的配套 PDF，Office 保持正式转换路径。不能退回历史版本 PDF。
- **验收 BDD**：Given 当前 CAD 及新配套 PDF 与历史配套内容不同，When 上传/升版最终生效，Then 受控副本由本轮 PDF 盖章而来，签名绑定指向该副本；配套缺失/损坏应失败，不能转换其他来源冒充。

### URB2-003 [P1] 生效失败重试未复核完整审批证据

- **触发条件**：最终审核事件进入 finalizeOrdinaryApproval 后，完整签名校验或盖章/落库失败，文件被记为 FINALIZATION_FAILED，随后文控执行 retryStamp。
- **调用链/依据**：正常路径 F:385-389 执行 `DccFrozenApprovalSignatures.requireComplete` 和逐条验签。失败进入 F:405-408、1201-1219，并由 FAIL:`recordFailure` 独立事务记录失败。重试 F:208-219 只验证失败状态与发布权限，直接进入 F:539/575/588；F:615 只调用 SB:48 `bindPublishedCopy`，后者只要求列表非空、每条 evidenceHash 非空及副本绑定一致，未验证缺失的必需阶段或 HMAC 真伪。
- **回滚证据窗口**：W:1270 的事务内先于 W:1311 完成文控签名写入；SIG:65 也为默认传播事务。BPM 完成监听 BL:46-50 → BI:1141 → BE:22 为同步调用，最终化失败会传播回该事务；失败记录则在 afterCompletion(ROLLED_BACK) 后另行提交。因而不能仅凭 FINALIZATION_FAILED 推定最终人工签名和 BPM 完成已经持久化。
- **影响**：失败恢复路径可进入“仅绑定现存部分签名并尝试激活”的分支；签名不完整/无效这一原始阻断条件未被恢复路径保留，并可能留下文控待办与业务 ACTIVE 不一致。未实际执行该故障序列。
- **修复边界**：明确人工审批提交与派生生效失败的事务边界；重试须证明原审批已持久完成并重新执行完整证据门禁，不能把失败标识当批准证据。权限校验仍保留。
- **验收 BDD**：Given 最终签名写入后盖章失败导致回滚，以及 Given 缺签/坏 HMAC 导致失败，When 重试，Then 均不得在无完整有效审批事实时 ACTIVE；正常“审批已持久成功、派生失败”可恢复，BPM、签名、Master 和文件状态一致。

### URB2-004 [P2] 上传重试修复比较了不同格式的 session（历史 URB-006 仍未闭环）

- **触发条件**：普通上传已经在服务端提交，成功响应丢失；用户在原页面用原票据重试，尚无 ACTIVE 现行版。
- **调用链/依据**：U:752 创建客户端 session；UP:104-116 调用 S:`newUploadPrefix/scope`，生成 `dcc-new:<业务摘要>:<客户端会话摘要>`；UP:268 把票据 scoped session 原样返回。U:840-847 重试条件却要求 `previewUpload.sessionId === uploadSessionId`。API 的解析只读取返回字符串，没有反向改写；US:`buildSubmitPayload` 使用返回 session 作为幂等键。
- **影响（修复前）**：正常后端响应不满足旧等式，U:1878-1883 不会抑制版本链冲突，U:2257-2263 先退回，后端 W:854-887 的同键回读不可达。当前已通过页面内 raw/scoped session binding 修复；若页面刷新丢失对应关系，仍需服务端身份投影支撑。
- **修复边界**：明确保存客户端会话与服务端业务会话的对应关系，以已提交尝试身份确认结果；不要硬编码相等或只匹配错误文案，也不能放松新请求的重复身份守卫。
- **验收 BDD**：Given 使用真实 scoped session 的已提交但丢响应申请，When 原键原载荷重试（待审批、已 ACTIVE 两阶段），Then 回到同一文件/流程且零新增；不同载荷/不同键仍拒绝。

### URB2-005 [P2] 取消上传后加载状态可能永久保留

- **触发条件**：SOURCE/PDF 请求尚未返回时移除文件或切换项目/模板；或者普通附件上传中切换上下文。
- **调用链/依据**：U:1214/1222 的 reset 只增加序号、清文件，未复位 uploadPreviewLoading/uploadDrawingPdfLoading；旧回调 U:2055、2140 只有序号仍相等才关闭 loading。附件 reset U:1228 使 generation 失效，却没有清空 attachmentUploadAttempts，U:806 仍从旧 UPLOADING 项汇总为 true，迟到回调在 U:2178 退出也不把该项结束。按钮 U:464/522/558 绑定这些 loading。
- **影响**：界面已经清空，但选择按钮持续忙碌；附件旧 FAILED 项也可持续给出错误提示。正常页面继续选择可能受阻，通常需要重载恢复。
- **修复边界**：reset 同时终结当前加载/尝试状态，并让失效请求只清理自己的票据；不能简单允许旧 finally 修改新请求 loading。
- **验收 BDD**：Given 主文件/PDF/附件分别延迟返回，When 取消或换上下文，Then 新上下文按钮可用、旧提示消失；旧请求结束不得改变新上下文状态，新上传可正常完成。

### URB2-006 [P2] 上传目录异步响应缺少类别身份保护

- **触发条件**：从模板 A 切到 B（自动类别不同），A 的目录加载较慢，在 B 返回后才结束或失败。
- **调用链/依据**：U:1565 `handleFileTypeTaxonomyChange` 重置后异步执行 U:1244 `syncAutoCategoryFromSelectedFileTypeTaxonomy`；U:1591 `loadUploadDirectoryTree(categoryId)` 在 await 后直接写全局 tree/directoryId，catch 也直接清空，无类别/请求代次判断。
- **影响**：显示 B 类别但选择 A 的目录；后端 W:1986-1988 正确验证绑定后会拒绝提交，或者旧失败清空 B 的正确选择。本项不声称后端会把文件存入错误目录。
- **修复边界**：捕获类别和请求代次，对 success/catch/finally 同时保护；提交依赖的 tree 必须属于当前类别。
- **验收 BDD**：Given A 慢 B 快，When 切换至 B 后 A 成功/失败，Then 目录、已选值和错误只对应 B，可正常提交。

### URB2-007 [P2] 培训记录上传仍有取消后迟到回写

- **触发条件**：培训记录 A 上传未完成时移除或关闭弹窗，重新打开并上传 B；A 晚于 B 返回。
- **调用链/依据**：D:5269 `handleApplicantTrainingRecordChange` 直接把 await 结果赋给共享 dialog.file，没有 uid/session/请求代次判断。D:5126/5143 在尚无已返回票据时允许清理结束；D:5422/5445 重设 session 并清空弹窗，但不失效 A。D:5464 提交使用 dialog.file 内的 session 和票据。
- **影响**：用户取消的 A 可以重新变成已选证据，甚至覆盖 B 后被正式提交；仅修复主上传页的 URB-005 没有覆盖此共用培训入口。
- **修复边界**：培训弹窗也采用请求身份与生命周期保护，关闭/移除后清理迟到票据；提交冻结当前选择且阻止未完成上传。
- **验收 BDD**：Given A 慢 B 快，When 取消 A 并选择 B，Then 只提交 B；关闭后 A 失败不能污染重开的弹窗错误/加载状态。

### URB2-008 [P2] 首次上传返工工作稿被错误的升版路线预检阻断

- **触发条件**：尚未首次生效的 NEW 被驳回/退回，经检入形成 NEW WORKING，准备重新送审；NEW 路线就绪而 REVISION 路线缺失或不同。
- **调用链/依据**：Q:1005 首次未生效返工保留 NEW；W:686-699 正式选择 UPLOAD。B:1898 无论工作稿来源均调用 `assertRevisionRouteReadiness`；B:1872 写死 actionType=REVISION。
- **影响**：合法首次上传返工因无关升版配置不能送审；即使两者都有配置，页面预检的人员/阻断结果也不对应后端实际流程。
- **修复边界**：由该工作稿正式业务动作确定路线，前后端共享语义；不能为了通过页面预检要求同时配置无关路线。
- **验收 BDD**：Given 仅 NEW 路线就绪的首次上传返工，When 送审，Then 检查并启动 UPLOAD；已正式受控的大小版修订独立检查 REVISION。

### URB2-009 [P2] 多段版本策略未贯通前端

- **触发条件**：使用后端明确支持的 majorIdentitySegmentCount=2，初始/工作版本如 A/1/1、A/1/2。未确认当前运行环境是否启用该策略。
- **调用链/依据**：POL:30-67 生成并接受配置版本；U:764、1657 的正则只接受字母加一个数字段；B:1789 `parseWindchillVersion` 只接受纯字母 revisionCode 或两段 versionNo，遇到 A/1/2 返回 undefined，B:1815-1832 因而隐藏送审。
- **影响**：配置合法版本不能在上传页面输入，检入生成的合法多段工作稿无法通过浏览页送审。默认两段格式下不触发。
- **修复边界**：前端使用后端版本策略/正式动作投影表达合法格式和最新工作稿，不复制硬编码的两段解析。
- **验收 BDD**：Given 策略1与策略2，When 初始上传、大小版检入和显式送审，Then 每种合法格式均可完成，非法格式明确拒绝。

### URB2-010 [P2] 图纸配套 PDF 的变更不计入检入差异

- **触发条件**：重新上传相同 CAD 字节与内容已更新的配套 PDF，备注不变（修改说明有填写）。
- **调用链/依据**：B:2187-2205 有源票据且 PDF 合法即可发检入；Q:592 已解析新 PDF，但 Q:601-606 只比较 CAD sourceSha256 和 hasRemarkChange，满足相同源摘要就抛 CHECKIN_NO_CHANGE，完全未比较新旧 PDF。
- **影响**：审批/浏览正文确实改变的图纸版本被当作“无变化”拒绝；迫使操作者无谓修改备注才能绕过差异判定。
- **修复边界**：把本轮受控文件包中具有业务效果的配套正文纳入差异身份；不能用任意说明文字代替内容差异。若业务要求 CAD/PDF 必须同时改变，应明确业务校验及提示，不把 PDF 已变误判为全部无变。
- **验收 BDD**：Given CAD 相同、PDF 不同，When 检入，Then 按正式规则识别真实变更并产生正确版本；二者及元数据完全相同才拒绝无变化。

### URB2-011 [P2] 连续检入会被自己的旧工作稿/返工前驱挡住

- **触发条件**：ACTIVE A/1 → 检入 WORKING A/2 → 再检入 WORKING A/3 → 对 A/3 继续检出；另一路是 PENDING_APPLICANT_REWORK A/1 → WORKING A/2 后继续修改。
- **调用链/依据**：Q:615-640 新建 WORKING 并释放原版本检出，M:196-208 只清 checked_out_*，不收口原 WORKING。Q:486 `rejectWhenMasterHasOtherUnfinishedWorkflow` 将除了当前 id 的任何未完成版本判冲突；Q:770-780 的排除列表不含 WORKING/返工前驱。W:713-738 的送审却已专门允许旧 WORKING、识别合法返工前驱，两个入口口径不一致。
- **影响**：自己的正常修改链被当作其他进行中流程，最新工作稿无法继续编辑。可能仍能送审，所以不声称整个逻辑文件永久不可用。
- **修复边界**：检出时区分同一合法前驱链的旧工作版本与真正其他审批候选，按 Master 锁和明确来源判断；不得简单放开所有未完成流程。
- **验收 BDD**：Given 连续至少三次编辑及带返工前驱的工作稿，When 检出最新工作稿，Then 可继续修改；不同候选审批仍拒绝，并发检出只允许一个归属。

### URB2-012 [P2] 文件详情授权与辅助接口权限不一致，合法审批人看不到详情

- **触发条件**：用户是当前受控文件的有效审批参与者，有相应审批及 BPM 权限，但没有 dcc:controlled-file:query 菜单权限。
- **调用链/依据**：C:451-455 详情仅要求已登录并交由 AUTH 文件级授权，AUTH:`hasCurrentRunningApprovalTask/canAccessBrowseScope` 支持任务参与者。D:4671-4696 却将详情与类别、目录等放进一个 Promise.all，无条件调用 CAT:81 起的 getCategoryList 与 DIR:55-59 的 getDirectoryTree；两者均额外要求 query 菜单权限。任一拒绝时不执行 fileDetail=detail。
- **影响**：服务端明确允许的文件详情访问在页面被无关辅助列表权限挡住，审批内容/预览无法正常展示；授权预览模式同样无条件加载类别/目录。未声称当前测试账号恰好缺该权限。
- **修复边界**：为合法任务/授权查看提供足够且受控的名称投影，页面按必要性加载辅助信息；不得为修复随意放开全目录或全部类别权限。
- **验收 BDD**：Given 仅有该文件任务权限、无全局 query 的用户，When 从真实审批入口查看，Then 可读该文件允许的详情/预览，其他文件及全局目录仍不可越权。

### URB2-013 [P2] 详情刷新代次保护没有覆盖后续辅助写入

- **触发条件**：同一详情实例发生两次 reloadAll；旧请求 A 已通过首次代次判断，但停在慢的活动/草稿动作查询，B 完成更新后 A 才返回。无需假设跨文件路由复用组件。
- **调用链/依据**：D:4693 判断后先写 fileDetail，随后 await loadActiveObsoleteAction/loadDraftObsoleteAction/loadActivePublishAction；D:4597-4661 的 success/catch 没有代次保护。D:4700-4720 又继续覆盖 accessExplanation、paperRecords、模板及名称映射，未再次核对 sequence；D:4873 的外层检查发生在这些写入之后。D:4786 的错误恢复查询同样直接回写。
- **影响**：主详情为新状态，动作实例、分发记录或权限说明却来自旧刷新；旧错误也可能覆盖新成功结果。URB-011 的签名分页修复不覆盖这些支线。
- **修复边界**：整个详情加载上下文传到每个辅助请求，每个异步回写点检查；过期链不得启动新一轮签名/审批加载去使当前请求失效。
- **验收 BDD**：Given A 辅助请求慢、B 全部成功，When A 最后成功/失败，Then 所有详情、动作、权限说明、签名、分发与 loading 保持 B 的一致快照。

### URB2-014 [P3] 培训上传成功提示跳过了实际分发节点

- **触发条件**：普通上传/升版三流程的培训记录提交成功。
- **调用链/依据**：W:1255-1264 将状态更新为 PENDING_MANUAL_DISTRIBUTION 并触发 TRAINING；D:5468 却固定提示“培训记录已上传，流程已进入文控批准”。
- **影响**：用户会去找文控批准，实际仍需分发，提示与正式流程 R02/R03 不一致。
- **修复边界**：使用当前动作结果的正式下一阶段，不借用旧流程文案。
- **验收 BDD**：Given 三流程需培训申请，When 提交记录成功，Then 显示进入分发，详情状态与下一动作一致。

## 条件风险与待确认项（不计入上述 14 项）

### URB2-R01 培训附件失效的恢复与门禁仍不闭环

继承历史 URB-R01。Q:3144-3149 按真实 FileDO 判断可用；F:314 仅检查 trainingRecordFileId 非空；W:1237 又禁止已绑定后重传。若文件记录/存储失效，可能页面禁止而命令校验不足，且缺少正式补证路径。未验证真实数据或删除入口是否可触发。后续验收：Given 已绑定培训证据失效，When 分发及补证，Then 明确阻断并经授权恢复、保留旧证据审计。

### URB2-R02 普通附件的跨版本生命周期仍待确定

继承历史 URB-R02。Q:625-626 只继承源文件归属和 relatedFiles，未调用普通 attachmentService；检入请求无普通附件维护合同。若要求附件跟随文件包，新版会看不到旧附件；如果版本专属，则需明确用户如何为新版添加附件。后续验收：含普通附件的文件大小版检入后，分别核对新旧版集合和审批内容身份。

### URB2-R03 培训记录详情仍只有名称提示，缺少正文查阅入口

继承历史 URB-R03。Q:3136-3152 只投影可用性/名称；D:1561-1568 文本提示无该记录专用预览/下载动作。文控是否必须从该审核页查阅正文需要确认；其他管理入口不可未经核实视为替代。后续验收：授权审核人能查看本轮证据正文，无权限者拒绝，失效记录明确报错。

### URB2-R04 浏览查询与版本投影的规模风险

继承历史 URB-R04。Q:313-428 先全量查候选、Java 授权过滤/聚合再 sliceRows；Q:2573、2986 逐逻辑文件查询并投影全部历史。未做性能测量，不声称已超时。后续验收：按真实权限及版本分布检查 SQL 行数、接口耗时、内存及列表完整性；不能先分页再过滤造成漏项。

### URB2-R05 培训/分发完成的业务口径仍需最终确认

继承历史 URB-R05。当前培训上传记录即推进，分发由授权操作者整单触发；早期技术设计的逐人阅读/签收属于设计默认条件，不能直接据此给当前代码定性为已确认违背用户要求。后续明确是“记录上传+人工分发”还是“逐人完成”，再设计相应门禁，不能把节点已走通说成逐人义务已完成。

### URB2-R06 取消的 BPM 接收活动是否被误判为已完成分发

URB-015 修复后 Q:3155-3164 以 DISTRIBUTION 历史活动 endTime 非空判完成，没有区分正常 trigger 与取消结束。若当前 Flowable 运行实现将撤回时结束的接收活动也记录 endTime，则未实际分发会显示已完成。未查运行历史表/引擎实际取消记录，不计确认缺陷。后续验收：在待分发直接撤回，与正常分发后再驳回对照，前者 false、后者 true。

### URB2-R07 切换项目/模板及离开页面的临时资源清理不完整

U:1377/1573 直接 reset 已返回票据，未先调用 cleanup；U:1256 不识别在途请求，U:2385 可能允许离开，U:2392 卸载未增加各上传序号。已有票据可能失去前端引用，在途返回也未必能被判定为失效。与 URB2-005 的按钮卡住分开记录资源风险；本轮未测量存储积累及服务端定时回收 SLA。后续验收：已完成及在途上传两种情况下切换/离开，任务票据最终均被绑定或清理，可审计且不误清其他会话。

### URB2-R08 重试最终化的并发与派生文件补偿

F:208-219 在主最终化事务外推进统一重试状态；F:575-585 重试使用普通读取，与正常 F:367-368 的 Master/file 锁路径不同。F:706 先生成外部存储副本，后续数据库回滚不天然删除物理对象。统一生命周期自身可能阻断部分并发，因此不直接断言必然双 ACTIVE。后续验收需覆盖双重试、重试与新业务并发、落库失败、重试再失败，核对唯一生效、副本归属/清理及 DCC/BPM/统一版本状态。

## 历史 17 项修复复核

这里只评价原触发点当前源码，不复用历史单测 PASS，也不把“已有代码”标为本轮运行通过。

| 历史编号 | 当前静态复核 |
|---|---|
| URB-001 | M 的 claimWorkingIterationSubmission 已写 need_training；另发现 URB2-001 前端把真值变 false。 |
| URB-002 | F:303-326 三流程分发已在 transactionTemplate 内锁读、CAS 并推进 BPM；未做真实事务/并发验证。 |
| URB-003 | BPM:1657-1672 缺失/多个 execution 会抛错；调用方校验 boolean。 |
| URB-004 | U:812/826/2248/2318 有附件 READY 集合校验及提交快照；未见原“未完成附件直接漏传”的同一路径，reset 另见 URB2-005。 |
| URB-005 | 主上传源文件/PDF/附件已有代次保护；取消加载复位与培训弹窗另见 URB2-005/007。 |
| URB-006 | 页面内 raw/scoped session 对应已补；页面刷新后的持久身份恢复仍属 URB2-004 残余风险。 |
| URB-007 | Q:895-908 的 CheckinReplayPayload 已包含 needTraining。 |
| URB-008 | D:4045-4054 前驱与当前 ID 使用字符串比较。 |
| URB-009 | B:3738-3754 时间维护及审计传 String(file.id)。 |
| URB-010 | V:556-758 元数据/二进制/媒体/文本/错误均增加代次保护，卸载失效；未运行媒体并发实验。 |
| URB-011 | D:4724-4783 签名分页包含文件/路由/页码/代次保护；辅助详情回写另见 URB2-013。 |
| URB-012 | PA:68-75 已执行文件详情授权；未做跨账号调用。 |
| URB-013 | W:948-1028 保存当时部门/人员名称，Q:3670-3692 使用快照；无目录改名运行验证。 |
| URB-014 | W:2990-3004 在内容批准完成时写 approvedTime，M:71 使用 COALESCE；无时间轴数据库验证。 |
| URB-015 | Q:3155-3164 已改查 BPM 历史，正常分发后失败不再仅按当前状态判断；取消活动歧义见 R06。 |
| URB-016 | B:2211-2249 分离检入成功与列表刷新失败，并保留新版本身份。 |
| URB-017 | Q:1005-1010 已生效 NEW 检入转 REVISION，W:686-699 另按正式基线判断；前端首次返工路线预检见 URB2-008。 |

## 排除项与后续处理顺序

- 未将“source 与 original 不同”直接认定为旧正文预览；Q 的未生效预览优先本版源文件，图纸还解析配套 PDF。
- 未将 revisionBaseActiveControlledFileId 在同大版本小迭代中保持原大版本基线认定为错误；必须先确认它的正式基线语义。
- 未将“培训上传后刷新失败显示为提交失败”计入问题：D:4862 reloadAll 自己报告并处理加载错误，没有向该提交 catch 传播。
- 未把所有 Number 调用一概认定为错 ID；本轮按实际数据身份和用途核对。
- 无需恢复 E2E 才能开始后续修复，但修复必须按仓库 BDD/RED/GREEN 规则另行实施。优先 URB2-001/002/003；随后重试、连续修改、路线/版本策略与授权；最后异步展示及文案。
- 本轮仅静态审查与登记。未来 E2E 仍需用户当轮明确要求，且必须走真实前端动作。
