# DCC 普通附件闭环验证报告

## 当前结果：加载身份与首次点击身份隔离已修复

详情 owner 的 loaded-fact-followup 指出一个区别于在途变化的新窗口：file/canDownload 在 T1 加载，身份先切到 T2，随后才首次点击。真实 SFC 新边界 RED 3 项 1 PASS/2 FAIL：旧事实仍打开预览（true，期望 false）和下载确认（1，期望 0）；同身份和显式新 file 对象刷新对照通过。

本次唯一生产改动仍为 ControlledFileAttachments.vue：将 readAuthority 单独记录为 loadedAuthority，只由 props.file 对象首次接收/明确替换的同步 immediate watch 更新。auth/fingerprint 变化不重绑旧数据。preview 与 download 入口均先 ensureLoadedAuthority；不匹配时失效旧 generation，清空选择/预览/下载状态并显示刷新提示，确认/API 前返回。原并发锁、在途指纹、权限/hash 与卸载保护保持。

新增边界同命令 GREEN 3/3；最终只执行原受影响组，实际 22/22 PASS、0 fail/skip。用例分别覆盖 actor/base tenant/visit tenant 在首次点击前改变；继续原地修改旧清单触发 fingerprint 仍不能重绑；同身份正常、新 file 对象刷新恢复合法操作。没有扩展扫描。

```powershell
node --test --test-name-pattern='loaded T1|same authority remains' tests/unit/dcc-attachment-download-disposal.test.cjs
node --test tests/unit/dcc-attachment-download-disposal.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs
```

两个命令 cwd 均为 IntRuoyiFronted；本轮仅组件、专属测试和任务记录变动。没有修改父页/viewer/API/后端，未运行 Maven/shared target/fulltypes/build、E2E、业务库/迁移、服务、远程、Git 提交推送或子 Agent。Root 接收的旧 19 与独立 36 PASS 为历史回执，不冒充本次执行。最终 GREEN 后生产和测试冻结，指纹见 loaded-authority-review.json。

## 上一轮结果：未卸载组件的状态变化与确认并发已修复

本轮实际新增三个缺陷分支的 RED：同 ID 附件事实原地修改后旧确认仍请求（实际 1，期望 0）；确认未结束时双击产生两次请求（实际 2，期望 1）；props 不变但 actor/tenant 缓存变化后旧确认仍请求（实际 1，期望 0）。上轮卸载修复保持，未重开 C01-C06，也未把上轮 200/200、类型/构建写成本轮新执行。

唯一生产修改仍为 `ControlledFileAttachments.vue`。增加完整附件清单、raw version、下载权限、actor/base tenant/visit tenant 指纹；props 对象或响应式事实变化时同步使 generation 失效并清理原选择/下载状态；确认后和二进制后直接读取当前指纹，防止纯缓存变化继续使用旧操作。下载锁提前到确认前，重复点击直接返回；旧 generation 的 finally 不会释放新操作的锁。不改变下载 API、hash 校验或服务端权限。

本轮最终 canonical 命令（cwd IntRuoyiFronted）：

```powershell
node --test tests/unit/dcc-attachment-download-disposal.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs
```

实际 exit 0，19/19、0 fail/skip。专属 11 项包含同 ID 新对象、原地附件/权限变化、三个独立身份变化、纯租户缓存改变后的二进制、旧回包不保存且不释放新锁、取消/关闭、合法单次下载及上轮卸载保护；相邻 8 项保留原精确 Long 身份、文件名/审计头、JSON 错误与权限拒绝断言。首次专属 RED 7 项 5 PASS/2 FAIL，补身份后 RED 8 项 5 PASS/3 FAIL。首次受影响回归曾因新增 auth/store 端口未接入而有 5 个 fixture 错误；同组件专属宿主仅增加两行端口，保留原所有断言后取得最终 19/19。

上传票据未保存/取消重试本轮只读未见新缺口：组件 ready 拒绝未完成/失败项，cleanupAll 在上传或保存期间拒绝清理、清理失败保留精确票据可重试，markSaved 使已绑定票据退出临时清理；AttachmentService 的 MANDATORY 事务和精确票据/版本校验保持。本轮未修改或重跑这些已过套件。

本轮修改文件为组件、两个组件专属测试及原任务记录。没有修改 viewer、详情父页、workflow、后端 Query/服务，未运行 Maven/shared target/fulltypes/build、E2E、业务库/迁移、服务、远程、Git 提交推送或子 Agent。证据与最终指纹独立保存 `retained-state-review.json`；更广类型/构建由 Root 决定并唯一执行。原文缺失条目和冲突文档不作推断。

## 当前复核：公共附件下载卸载竞态已修复

本次新增专属用例复现并修复一项公共组件缺陷的两个分支。原 `ControlledFileAttachments.vue` 只有 props watch 使 generation 失效，卸载没有失效处理；旧确认接受后仍发送下载请求，旧文件回包在卸载后仍调用浏览器保存。初始两项均 RED；补取消/关闭、合法当前下载、撤销权限对照后，实际 RED 为 5 项、3 PASS/2 FAIL。Root 解除前端构建冻结并将组件唯一转交后，增加最小 onBeforeUnmount 钩子，使 generation 失效并清空原预览/下载状态。相同专属命令 GREEN 5/5，受影响组件/API 契约回归 8/8，0 skip。

本线程新增 `IntRuoyiFronted/tests/unit/dcc-attachment-download-disposal.test.cjs`，使用当前生产 SFC 与生产附件身份 helper，未改旧测试宿主。本次唯一生产改动为 `IntRuoyiFronted/src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue`：Vue import 加 onBeforeUnmount、增加三行卸载钩子；未改接口/权限/hash/下载 handler 现有校验。冻结期间仅写测试和文档，生产修改发生于 Root 明确解除冻结和转交之后。上一轮 125/181/完整类型、旧 211/129、Root 修改前 Vite exit 0/2635 输入零漂移/3239 产物均是历史证据，不当作本修复后全量验证。

本次实际命令（cwd IntRuoyiFronted）：

```powershell
node --test tests/unit/dcc-attachment-download-disposal.test.cjs
node --test tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs
```

前者同命令从 RED 3 PASS/2 FAIL 变为 GREEN 5/5；后者修改后的受影响回归 8/8，保持精确 Long 身份、服务器文件名/审计头、JSON 错误拒绝、无权限/跨版本/迟到确认拒绝等原断言。没有重复原业务大套件。

其余指定边界本次只读未见新增缺口，依据为：

- 源文件或任一附件未完成：browser submitCheckin 在 source loading/failed、attachment ready/uploading/cleaning/readReadyManifest 处拒绝请求；保存失败不会 markSaved 或显示成功。上传 submitForm 在初始/最终附件快照及确认后上下文处验证，未更改这些实现。
- 升版确认取消/关闭：DetailApplicationPanel 的 cancel/close 仅解除 busy 并保留输入，没有删除已保存附件；确认前后校验完整 ID 列表和服务器 hash，旧版本或缺清单/hash 明确拒绝。
- 升版来源小版本与申请轮次：Revision freezeCandidate 用 selected.getId() 读取和冻结附件，保留 selectedIteration/predecessor/source version；AttachmentService 校验准确来源与候选版本，并在首次 bindApplicationRound 后不改写历史首次申请轮次。
- 历史培训归属：ordinaryRows 仅把 ATTACHMENT 交给冻结；TRAINING_RECORD 不复制到新候选。阅读计时不写附件/培训事实。
- SelectedDetailReadService 新投影：当前调用 Query 的三参精确上下文 overload，先验证原 ID/raw version 和 Guard；overload 仍调用同一正式详情投影，仅 DAILY 限制 PREVIEW action，不过滤/拼接 attachments、不重算客户端 hash、不授予 UPLOAD。toRespVO 从 file.id 读取完整附件；仅合法未送审 WORKING 上传方读取服务器 ordinary manifest。历史/审核详情没有 hash 是既有权限/生命周期边界，不应伪造空清单或临时默认 hash。
- 跨版本下载：服务端 readAttachmentDownloadFile 先校验精确租户/文件、当前下载策略，再 getAttachment(id,attachmentId) 限定归属，并验证存储业务权限；上下文详情读取不提升下载权限。前端 props 变化的 watch 和权限复核仍有效，本次 RED 仅补足未覆盖的卸载路径，不宣称已证明权限绕过。

本次未执行 Maven/shared target/fulltypes/build、E2E、实库写入/迁移、服务、远程、Git 提交推送或子 Agent。仅执行新增 5 项的 RED/GREEN 和受影响 8 项回归；最终生产/测试指纹与实际退出码见 focused-boundary-review.json。后续是否需要完整类型/构建由 Root 决定并唯一执行，不能宣称本次已执行。历史验证与下方相邻复核结论保留其当时范围。

RED 快照之后阅读 owner 继续修改 viewer/index.vue 和 reading-lifecycle.ts；这两份在新证据中标为 PARALLEL_CHANGE，本线程未编辑或重新验证 viewer，不声称全仓零漂移。新组件及专属测试的本次最终 SHA 已记录供 Root 集成核对。

## 历史阶段二：相邻阅读计时与精确详情复核

继续原任务，只读复核当前工作区；**未发现相邻合并新增的多附件缺口，本轮没有修改生产代码或测试，没有重跑已通过的套件。** 下方 211/129 是前次本线程实际执行结果；本轮 Root 独立回执为后端 7 类 115 PASS（附件 DB 41）和前端 10 文件 77 PASS，两轮结果不相加、不替换。

- 后端 Root 七份现有 XML 已逐份只读核对：正文读取 27、日常正文投影 11、附件数据库 41、精确详情 HTTP 3、精确详情服务 7、阅读会话服务 23、阅读会话 Controller 3，合计 115，0 FAIL/ERROR/skip；SHA 与观察时间见 `adjacent-integration-review.json`。前端 77 来自用户提供的 Root 回执，本线程未执行其命令。
- 本轮记录 33 份文件的原字节和 UTF-8/LF SHA256；附件服务/Revision/Workflow/上传服务、CheckinAttachmentsPanel/browser 父页、DetailApplicationPanel/revision helper、共享附件组件、附件身份 helper 和自有四份测试均保持前次快照。相邻改动单独记录，不覆盖历史 validation-sources.json。

|核对点|当前源码依据|结论与边界|
|---|---|---|
|附件预览身份与正文计时隔离|`ControlledFileAttachments.vue` 的 previewSource、`filePreview.ts` 的 DCC_CONTROLLED_FILE_ATTACHMENT 分支、`view/index.vue` 的 startPreviewReading|仍使用 `(controlledFileId, attachmentId)` 两个精确 ID；普通附件不进入 CONTROLLED_FILE/ONLINE_FILE 正文计时。仅只读 viewer，不改阅读线程独占文件|
|详情读取保持原精确版本|`DccSelectedDetailReadService.authorizedProjection`、详情 `loadData` 和 `resolveDetailReadSelection`|先校验文件/版本/任务身份与 Guard，再以原 ID 调用 Query；没有换为 Master 最新版本，没有删除或拼接其它版本附件。签名按申请过滤的变更不改普通附件持久化|
|切换与迟到回包|共享附件组件的同步 watch、generation 和 download 确认后校验；详情 reloadAll 的 fileDetail 清空和 route/actor/tenant 重读|旧附件预览/下载状态随当前详情失效；父页继续向共享组件传精确 fileDetail。没有以旧附件 ID 拼到新版本来补数据|
|检入保存与完整响应|Query doCheckinControlledFile/requireSavedCheckinManifest/toCheckinRespVO，CheckinAttachmentsPanel 与 browser submitCheckin|新增 Query 阅读鉴权不触及这些写方法；独立票据、全部就绪门禁、成功完整响应、BOUND 精确重放保持原实现|
|升版确认与重提|DetailApplicationPanel selectIteration/confirmation/submitRevision，revision-model readApplicationOrdinaryAttachmentExpectation|所选工作稿仍按原 ID 读取并校验 Master/项目；确认展示双附件，确认前后比较 IDs 和服务器 hash；驳回/返工必须新正文的前后端门禁保持|
|历史培训不复制|AttachmentService ordinaryRows/freezeOrdinaryAttachments 与 Revision freezeCandidate|只冻结 ATTACHMENT，TRAINING_RECORD 仍保留在历史版本；阅读计时不写附件表或培训事实，精确详情读取不调用附件复制/绑定|

本轮只执行文档/JSON/源码指纹结构验证；未执行 Maven、共享 target 写入、fulltypes、build、E2E、实库、服务、Git 提交/推送、远程或子 Agent。受限的 browser/DetailApplicationPanel 未出现需要改动的新缺口，因此没有提出修改请求，也没有新增平行 API。真实页面、运行中服务和实际存储边界仍未验证。

相邻任务中曾记录的“viewer 尚未接线”“Maven 被阅读依赖阻塞”属于它们当时的历史状态；本次直接读取当前 viewer 已接线源码和 Root 七类 XML，不能沿用这些过期描述作为当前产品 blocker。

本轮文档/JSON 结构、33 份复核输入稳定性检查 PASS；cleanup preview/apply 在 ready_for_closeout 状态均 exit 0，keep 6/delete 0/warnings none。最终 blocked 仅表示用户禁止 Git 收尾，当前附件复核没有软件域待修项。

## 历史阶段一：原源码闭环复核

基线为 `int_qms` / `f06833f06c868dc3a451eb63d01381a467ecfe92`，包含 `a3b8234ec`。当前上传、检入、正式升版和确认送审的普通附件实现已经接通，本轮保留生产实现，没有新增或恢复 API。

本轮修改四份测试文件：修复自定义驳回重提夹具误作用于继承测试的问题；补四个后端参数化执行场景和两个前端行为用例。结果以本轮 fresh 执行为准，不复用历史 PASS。

## 业务覆盖与现有源码

|业务要求|现有正式实现/方法|本轮证据|
|---|---|---|
|上传可添加多个附件|`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` 的 multiple 上传、resolveReadyAttachmentUploads/submitForm；`upload/submitter.ts` 的 buildUploadPayload|前端附件状态、上传页面真实 handler 与确认摘要测试|
|检入多附件全部保存|`browser/CheckinAttachmentsPanel.vue` 独立 CHECKIN 会话；`browser/index.vue` submitCheckin；`DccControlledFileQueryServiceImpl.doCheckinControlledFile` 与 requireSavedCheckinManifest|真实 Query + ticket/attachment Mapper + H2 事务，双附件保存/精确 BOUND 重放/失败回滚|
|正式升版附件归属|`DccControlledFileRevisionServiceImpl.createRevision/createInitialCandidate`；`DccControlledFileAttachmentServiceImpl.freezeOrdinaryAttachments/requireFrozenOrdinaryAttachmentManifest`|真实 Revision 创建目标版本、独立字节副本、上传事实与 manifest hash、第二附件失败回滚后重试|
|申请轮次保存|`DccControlledFileWorkflowServiceImpl.submitWorkingIteration` 清单期望校验及 bindApplicationRound；`DccControlledFileAttachmentServiceImpl.bindApplicationRound`|普通附件首次轮次不可覆盖；工作稿初始化/升版数据库回归；自定义驳回重提真实 Flowable 用例|
|驳回/返工需新正文|Query doCheckinControlledFile 的状态与 uploadTicket 门禁；前端 submitCheckin 的 requiresFreshSource|新增两种状态各正/负四场景：新正文与双附件保存成功，双附件不能代替新正文；原历史培训不复制|
|确认展示清单并阻断变化|`DetailApplicationPanel.vue` confirmation/submitRevision/submitInitial；`revision-model.ts` readApplicationOrdinaryAttachmentExpectation|真实前端函数确认双附件文件名、服务器 hash/期望 IDs、缺 hash/清单变化零写；新增 cancel/close 均保留双附件且零提交|
|上传/保存失败不成功|前端上传状态、清理状态和完整检入响应校验；后端真实事务及票据验证|待上传/失败/清理失败/缺保存清单零成功；第二附件/晚阶段失败全量回滚及完整重试|

上述“源码方法”仅表示静态接线依据；动态证据的权限、账户和存储边界均为测试端口，不声称真实页面、实际账号、NAS 或业务数据库已经验收。

## RED / GREEN / REGRESSION

### RED：本轮真实暴露的测试夹具缺口

执行以下 Maven 定向命令（cwd `IntRuoyiBackend`）：

```powershell
mvn -pl yudao-module-dcc -am '-Dtest=DccRevisionAttachmentsDatabaseTest,DccCheckinAttachmentUploadContextTest,DccOrdinaryAttachmentFactsTest,DccTrainingAttachmentTransactionTest,DccTrainingAttachmentServiceTest,DccRevisionRollbackSynchronizationTest,DccCheckinDraftInitializationDatabaseTest,DccRevisionDatabaseTest,DccCustomRejectedCheckinResubmitTest,DccControlledFileFormEffectExecutorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

首轮 exit 1：207 项，169 PASS、1 FAIL、37 ERROR、0 skip。38 个失败均发生在 `DccCustomRejectedCheckinResubmitTest` 继承的父类方法：子类 `@BeforeEach` 对父类场景预先写入 V6 自定义准备，原测试再次初始化 Legacy 时冲突。没有将该测试夹具问题解释为产品附件逻辑失败。

修复使用 JUnit `TestInfo.getTestMethod().getDeclaringClass()` 区分父类原场景与子类自定义场景。全部 39 个方法/参数化执行仍保留，未添加 skip、过滤条件或弱化原断言。

随后四个新增后端场景首次运行时，两项正向场景因测试中未给出原审批绑定轮次而报 `EXECUTION_CONTRACT_SOURCE_BOUND_BPM_REQUIRED`。补入私有 H2 的明确历史绑定轮次并调用真实 DccApplicationRoundService 读取，不改产品门禁。该次属于夹具准备错误，不计为产品 RED。

### GREEN / REGRESSION

- 前端最终 11 个文件：129/129 PASS，0 FAIL/skip。cwd 为 `IntRuoyiFronted`，执行 `node --test`，文件清单如下。
- 后端同一定向命令最终 exit 0，`BUILD SUCCESS`，23 个 reactor 项目 SUCCESS，10 类共 211/211 PASS、0 FAIL/ERROR/skip。fresh XML 分项及 SHA256 记录在 verification-results.json；包含附件数据库 41、检入上传 8、普通附件事实 5、培训事务 9、培训服务 13、副本回滚 3、检入初始化 30、Revision 数据库 58、自定义驳回重提 39、退役入口拒绝 5。
- 生产功能已经存在，因此未制造人为产品 RED；本轮 RED/GREEN 针对实际失败的重提验证夹具。

```powershell
node --test tests/unit/dcc-upload-attachment-state.test.cjs tests/unit/dcc-upload-application-page.test.cjs tests/unit/dcc-checkin-attachments-panel.test.cjs tests/unit/dcc-browser-checkin-attachments.test.cjs tests/unit/dcc-revision-target-confirmation.test.cjs tests/unit/dcc-browser-checkin-minor-only.test.cjs tests/unit/dcc-browser-checkin-training-default.test.cjs tests/unit/dcc-application-g03-attachment-manifest.test.cjs tests/unit/dcc-upload-confirmation-summary.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-training-attachment-facts.test.cjs
```

## 本轮修改文件

- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccCustomRejectedCheckinResubmitTest.java`：隔离自定义 V6 的初始化/清理与父类 Legacy 场景。
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRevisionAttachmentsDatabaseTest.java`：驳回/返工双附件检入、历史培训隔离和必须新正文。
- `IntRuoyiFronted/tests/unit/dcc-browser-checkin-attachments.test.cjs`：双附件不得绕过驳回/返工的新正文要求。
- `IntRuoyiFronted/tests/unit/dcc-revision-target-confirmation.test.cjs`：取消/关闭实际升版确认，完整附件保留且零提交。
- 当前任务三份 Markdown 及证据文件。

## 与既有任务的关系

原实现主记录为 `doc/tasks/20261014-dcc-gap-revision-attachments/`，附件事实主记录另见 `doc/tasks/20261014-dcc-attachments-node-records/`，初始上传见 `doc/tasks/20260920-dcc-controlled-file-attachments/`。这些目录的当前状态分别为 blocked、blocked、completed；历史记录未被删除或覆盖。本目录记录集成提交之后的重新核验、实际夹具修复与新增边界测试，既有历史 RED/GREEN 仅作为来源，不冒充本轮执行结果。

## 未验证边界与收尾

- 未执行 E2E、服务启动/停止/重启、真实业务数据库写入、远程操作、发布、Git 提交或推送、子 Agent。
- 后端数据库测试均使用 `jdbc:h2:mem:` 和明确测试数据；物理存储为隔离测试端口，不证明实际 NAS/上传网络的运行态。
- 本轮未修改生产 TypeScript/Java，因此不重复全量类型检查或 Vite 打包；定向 Maven 仍使用 `-am` 编译依赖及 DCC 模块当前源码。
- 工作树并行改动（Query 日常预览、Controller、前端 API/detail/view 等）不归本任务，不回滚、不清理、不纳入本任务实现。
- 最终执行前观测的 2227 份输入中，运行结束后有 3 个其它任务页面发生漂移：`detail/ControlledFileBrowseRecords.vue`、`detail/DetailApplicationHistory.vue`、`detail/index.vue`。本次附件组件、DetailApplicationPanel、API helper、本任务四份测试和 DCC 后端输入均无漂移；不声称全仓输入未变化，也不对这三个其它页面做整体验收。
- 收尾最后一次观测进一步记录到 `view/index.vue` 和其它任务的 5 份 detail/content 测试变化，总计 9 份并行漂移；精确路径和首次完成时的观测分别保存在 verification-results.json。它们不属于本次定向前端 11 文件或自有修改，不追改、不重跑已通过的附件套件来声称全仓稳定。
- 依据 `docs/task-closeout-rules.md`，禁止提交/推送时最终状态必须为 blocked；软件验证结果与 Git 收尾状态分别记录。
- cleanup preview/apply 已在 ready_for_closeout 状态分别 exit 0，仅清理本任务五份临时 stdout 日志；三份 Markdown 与两份 JSON 保留。其后最终状态为 blocked，唯一工程收尾阻塞是本轮明确禁止的 Git 提交/推送。
