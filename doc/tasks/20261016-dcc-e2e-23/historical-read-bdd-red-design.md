# E2E-23 历史读取最小 BDD / RED 设计

当前阶段：TESTS_WRITTEN_AWAITING_ROOT_RED。Root已将Query历史修复唯一归属从18转交本例，当前仅授权测试；独立DccHistoricalAttachmentReadTest及DccAttachmentDownloadTest必要断言/夹具已写出。未改生产或主Query Test、未编译/执行Maven，Root取得有效服务RED后才可批准GREEN。下方保留已接受BDD/RED设计，不把设计或静态核对当测试通过。

## 固定事实与边界

- 保留A/1 `2054545668044084233`（OBSOLETE）、B/1 `2054545668044084247`（ACTIVE）、原稿4239、项目309、类别908766及三类原账号；不重建、不重签、不重绑培训。
- ViewOnly有现行查看资格，无DOWNLOAD/OBSOLETE；ViewDownload有查看及DOWNLOAD，无OBSOLETE；NoAccess无查看/下载。这里VIEW表示既有内容查阅资格，不是新增permission/角色。
- 真实RED已有：两类阅读账号B/1实际预览/下载按权通过，但历史表首列只有B/1及已替代草稿，A/1被过滤。作者能回读A原培训/上传时间，只作为夹具事实。证据见`historical-permission-boundary.json`、`roleChecks.*.r3.history`及本轮trace。
- 只修直接历史读取及精确版本附件，不修改作废命令、DAILY正文、全域浏览性能、项目授权、分配范围、存储权限框架、审批/培训流程，不新建权限体系或迁移。

## BDD

| 场景 | Given | When | Then |
| --- | --- | --- | --- |
| H1 历史发现 | 非作者、非管理者，无在途审批任务；同项目有VIEW，无OBSOLETE | 从B当前详情读取版本历史 | 列出精确A.id/A/1/OBSOLETE，仍指向当前B；不是把B的“来源A/1”文字当历史行；不产生作废能力 |
| H2 历史只读 | H1身份且无DOWNLOAD | 以精确VERSION_HISTORY上下文读A详情，预览A普通附件和培训记录 | 原A文件名、用途、上传人/时间/轮次保留，正文为A；不能换成B；附件下载拒绝且存储字节未被读取 |
| H3 历史附件下载 | 同一非作者有VIEW及既有类别/目录DOWNLOAD，无OBSOLETE | 确认非受控警告，下载A附件/培训 | 返回精确A附件字节与文件名，留痕A.id/A/1/attachmentId；不依赖作废权限，不返回B文件 |
| H4 无查看 | 无VIEW，包括只有DOWNLOAD或只有名称QUERY | 请求A历史详情、附件metadata/binary/download | 全部拒绝，无附件清单/正文泄露；无字节读取、无成功下载记录 |
| H5 跨范围 | 有类别VIEW/DOWNLOAD但无A项目读取资格，或不在既有分配范围 | 同样请求A | 列表不泄露A，详情及附件拒绝；B或另一项目的授权不能传给A |
| H6 身份/绑定错误 | 合法阅读人 | A路径配B附件ID、错版本、错master/租户、已删对象，或错用户/token | 在存储读取前拒绝，不能凭共享storageFileId借另一版本权限 |
| H7 日常与操作隔离 | H1/H3身份 | 以DAILY上下文打开作废A正文，或发起作废命令 | DAILY仍拒绝；canObsolete=false、allowedActions不含OBSOLETE，写命令仍受原作废授权拒绝 |
| H8 即时撤权与状态 | metadata已合法取得 | 撤销VIEW/项目/DOWNLOAD后读binary/download，另测WORKING/待生效状态 | 每次正式读取重新校验相应资格；旧token不保留撤销后的权利，未正式受控状态不因历史修复放行 |

## 后端直接读取关口

下列均为**待Root授权的候选修改位置**，不是本轮改动清单。路径以`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`为根。

| 文件 / 具体方法 | 必须处理的问题与最小边界 |
| --- | --- |
| `service/file/DccControlledFileQueryServiceImpl.java` → `buildVersionHistory(...)` | 当前先经`canAccessQuery`、再`canSeeObsolete`过滤，映射时又经`canAccessDetail`。仅删除第二个filter无效。对本版本链的正式历史行使用既有阅读资格，逐行保留项目/分配/身份约束；不要全局放宽`canAccessQuery`或全域浏览 |
| 同文件 → `getControlledFile(user,id)`、`getControlledFile(user,id,context)`、`toRespVO(...)`及必要的`canAccessDetail`读取调用点 | 当前“只读记录”路由不携带正文上下文，实际走无上下文metadata入口；显式VERSION_HISTORY入口又会回调同一入口。两条纯详情读链都须按精确A的历史阅读资格投影，不能借B/root上下文授权整条链，不能把metadata准入当作默认正文准入或放宽共用写入校验 |
| `service/file/DccControlledFileDetailAuthorizationGuard.java` → `requireBodyRead(...VERSION_HISTORY)`；`isAllowed(...)`历史详情使用点 | 复用现有启用用户、租户、project、master、assignment及查看规则，历史读不查OBSOLETE action。现`requireBodyRead`主要校验上下文/项目/状态，不能单凭intent充当VIEW授权。`isAllowed`被多处复用，不可直接删掉全局obsolete限制后宣称完成 |
| Query → `getAttachmentPreviewMetadata`、`readAttachmentPreviewBinary`（公共`readAttachmentPreviewFile`的实际读取点） | 两处都调用`canReadBinary(PREVIEW)`，后者仍有`canSeeObsolete`。让精确版本附件按历史阅读资格通过，两次都校验；保留所属版本、审计、viewer token/版本/用户证据和存储校验 |
| Query → `canReadBinary(...)`中显式VERSION_HISTORY/附件读取分支 | 仅允许被验证的历史上下文/附件分支不依赖作废操作。不得把默认正文读取视作历史；`DAILY`的ACTIVE+activatedTime要求保持 |
| Query → `readAttachmentDownloadFile` | 当前调用主文件`decideDownloadBinary`，其ACTIVE-only导致历史附件被拒。附件单独复用VIEW+既有类别/目录DOWNLOAD、项目和分配资格；保留非受控确认、requestId去重、正式附件归属、物理事实和下载审计 |
| Query → `requireAttachmentBusinessFileAccess`、`assertBusinessFileAccess`的附件分支、必要的`canReadBusinessBody`分支 | metadata/binary还有真实BusinessFileAccessService回调。预览已有`AttachmentBusinessRead`精确作用域，下载目前未绑定该作用域、回调又用主文件ACTIVE策略。复用这个局部机制限定actor/file/storage/operation与正式附件归属，并finally清理；默认DIRECT_LINK/主文件DOWNLOAD不得因此放行 |
| Query → 选定详情的附件能力投影；`controller/admin/file/vo/DccControlledFileAttachmentRespVO.java` | 页面目前以共享`file.canDownload`显示附件按钮。不能为附件把主文件下载能力一并置true。建议给已有附件VO增加最小`canDownload`能力投影，按行返回既有授权结果；它不是新增权限。前端仅改附件按钮使用该明确字段的合同，须Root另行分配后实施 |

可在现有guard/Query内部复用一个小的历史阅读判断，避免各关口复制授权；不新增授权服务、permission code或“兼容”旧数据的默认放行。保留`DccSelectedDetailReadService.authorizedProjection`及`selectedProjection`精确身份校验，它们应接通正确Query历史投影，当前没有证据要求修改该服务。

调用合同已核对：`ControlledFileBrowseRecords.openVersion`使用`buildControlledFileTraceabilityPath`，只有traceability/from等导航字段；`detail-read-context.resolveDetailReadSelection`不会把它自动升级成显式正文上下文，`index.vue.loadData`因而调用无上下文`getControlledFile`。不能只修带context重载，或把该纯metadata请求擅自按DAILY正文拒绝。历史正文仍需显式VERSION_HISTORY，附件请求由既有精确fileId/attachmentId绑定校验。

**保持原样并回归**：`DccDownloadPolicyService.decide`及Query的`readDownloadFile/readDownloadBinary/decideDownloadBinary`主正文通路继续ACTIVE-only；作废操作的类别/角色权限不变。Controller三个附件路由已有明确fileId/attachmentId，无证据需要增加路由或把可伪造的参数当新授权。

## RED 设计（测试已编写，尚未编译/执行）

Root已分配同包独立`DccHistoricalAttachmentReadTest.java`及旧下载测试必要拆分，保持主`DccControlledFileQueryServiceTest.java`不改。实际独立测试还使用真实查看矩阵、类别权限及附件归属服务；Mapper/存储/外部端口隔离，BusinessFileAccess端口回调真实provider及Query，不能mock `canReadBinary/isAllowed/assertBusinessFileAccess`制造GREEN。

公共夹具：A=OBSOLETE、B=ACTIVE且同tenant/master/project/category，正式发布件和两版独立附件/培训存在；reader不是requester/OWNER/管理员/审批人，OBSOLETE始终false；VIEW由现有矩阵/目录事实提供，DOWNLOAD独立开关。先验证B当前详情/附件确实可读，避免空mock或缺依赖冒充目标RED。

| 设计测试 | 调用真实入口及首要断言 | 当前预期失败位置 |
| --- | --- | --- |
| R1 historyRowKeepsExactObsoleteVersionWithoutObsoleteAction | `getControlledFile(reader,B)`：history按id找到A且版本A/1、statusOBSOLETE、current=B；无OBSOLETE能力 | 历史行被过滤；这是第一条最小服务行为RED |
| R2 selectedHistoryReturnsOnlyItsOwnAttachmentFacts | 先按实际“只读记录”链调用`query.getControlledFile(reader,A)`，再覆盖`DccSelectedDetailReadService.read(reader,A,VERSION_HISTORY(A,A/1))`接真实Query/guard：两者均为A原两行及时间/轮次，绝非B；无下载资格时附件canDownload=false | 无上下文metadata及显式context投影再次拒绝A；不得只测mock query返回值 |
| R3 historicalAttachmentPreviewPassesBothGates | `getAttachmentPreviewMetadata`后`readAttachmentPreviewFile`；有效token端口与审计齐全，真实BusinessFileAccessService回调Query校验；断言A字节 | preview的obsolete gate；修第一处后仍可能在binary/二次存储门禁失败 |
| R4 historicalAttachmentDownloadIsIndependent | reader VIEW=true/DOWNLOAD=true/OBSOLETE=false，`readAttachmentDownloadFile(A,attachmentA,confirmed,id,audit)`；含普通/培训两参数，捕获A审计及准确字节 | ACTIVE-only策略；通过外层后还要暴露真实business callback的同一限制 |
| R5 deniedReadersNeverReadStorage | 参数覆盖无VIEW、仅DOWNLOAD、跨project、assignment拒绝、附件跨版本；调用R2–R4相同公共入口 | 原已拒绝项应继续GREEN；verify无storage读取、无成功下载记录，而非只看异常 |
| R6 historyReadNeverBecomesDailyOrObsoleteWrite | 同一reader通过历史读取后，再走DAILY的metadata/binary及原作废写授权；原主文件download仍拒绝旧版 | DAILY/作废/主文件download应保持GREEN；验证局部作用域异常后也不泄漏 |

已发现必须调整的旧合同：`DccAttachmentDownloadTest.historyOrPendingStateAndBusinessStorageDenialNeverReturnAttachment`把OBSOLETE和待生效一并断言拒绝。Root实施时应拆开：合法历史附件改正例；待生效、无权和存储拒绝继续负例。不能保留旧断言再加绕过，或删除安全断言。`DccDownloadPolicyServiceTest`主文件ACTIVE-only原断言不变。

RED合格门禁：R1必须先到“缺A历史行”的业务断言失败；R2–R4分别到真实授权/策略失败，不接受编译错误、未stub必要身份、空预览证据或假成功。保存原JUnit输出后才可最小GREEN。Root尚未分配测试文件/构建时，本设计不写“RED已执行”。

## 原UI复验与交付

Root部署后只复用原A/B与三类账号：新context登录，从B详情历史表首列进入精确A，再逐类验证A附件和培训的正文、上传人/时间/用途/轮次、各自下载资格及保存文件。无权身份不得用作者/直达URL代替；原无历史入口参数须按修复后的实际正式入口继续验证。保留此前当前版、取消/连续点击/迟到响应证据，不重放成功业务。

前端合同点仅限已有`ControlledFileAttachments.vue`附件按钮以及对应TS附件VO；需Root后续明确分配。没有准确附件能力投影时，后端入口GREEN不能宣称历史下载真实UI GREEN。当前3PASS/2FAIL/1BLOCKED结果保持，源码修复及最终E2E待后续串行授权。
