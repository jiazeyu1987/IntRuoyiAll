# DAILY / 精确详情 / reading-session 复核交接

## R3 GREEN候选已实现，交Root唯一验证（最新）
- Root正式schema review后已允许name/type真实nullable值原样进入签名tuple，不新增必填/默认描述。定位id/config/path及size校验保持，公开prepareSourceBoundAccess仍@Transactional。
- 当前专属测试静态预计Query67+token17；Root所需普通正文prepareAccess→prepareSourceBoundAccess、verify→verifySourceBound最小stub迁移及保留全部原断言的清单见verification-report.md。旧55/12为更早静态计数，非执行证据。
- Root实际RED33/24业务FAIL/0ERROR/0SKIP、9对照PASS确认后，本线程已实现source-bound typed tuple签发/验证与Query metadata/binary/reading接入，生产5文件、专属tests2文件待RootGREEN。
- 来源tuple为infraId/configId/path/name/type/size，普通正文强制新入口；无绑定旧token不降级、新签发异常不fallback，nonce和前端仍opaque。无schema或metadata bytes读取。
- Root共享fixture适配清单、静态55+12用例数与未验边界在verification-report.md最新节，指纹r3-green-handback.json；没有擅改其它owner测试。原RED证据仍在Root目录。
- 不声称源码候选已GREEN，不声称覆盖同config/path对象存储原位bytes替换；Office callback保持其独立signed infraId合同。reading Service/Controller及详情reader未动。

## Root R3扩展归属与RED测试交还（最新）
- Root已接受R3并决定本轮继续：唯一授权本线程Query metadata/binary/assertPreviewReadingAccess、preview access、token来源绑定新合同及专属tests；不改reading Service/Controller、详情reader、前端字段或签名审批。
- 当前仅新增DccPreviewSourceBindingTest：真实Query+Guard+preview access+audit+HMAC签发/验证，mock外部Mapper/BPM/storage；公开metadata成功后，原样拿token/nonce请求公开binary。4项不变源成功对照，24项来源tuple变化负向，5项撤权对照，静态共33项；未执行。
- 测试方法、夹具要求、Root唯一Maven建议命令见verification-report.md最新节；未使用缺失新API或继承父类大套件制造伪RED。生产仍未改，Root实际RED完成后再通知GREEN。
- FileDO没有hash字段，当前事实只绑定infraId/config/path/name/type/size；不能声称覆盖对象存储同路径原位篡改。独立typed source-bound合同与强制拒绝旧无绑定token将在RED后设计实现，不在nonce放私有协议、不改前端opaque字段。

## R3 普通预览 source pointer/hash 未绑定（待Root分配，静态新发现）

### 触发与影响
- 用户U对同一受控文件版本V成功取普通PDF/图片等预览metadata，metadata当时解析预览存储文件F1并创建access event、watermark和viewer token。
- 在binary请求前，同一DCC行的 `sourceFileId` / `publishedFileId` 指针（或对应FileDO bytes路径/hash）变为F2，而V、租户、用户和事件仍保持可匹配、token未过期。
- 当前普通 `readPreviewBinary` 重新调用 `resolveBinaryFileId(file, PREVIEW)`，对当前F2做business access，再按同一受控文件版本/事件/token验证并读取F2。普通 `DccViewerTokenPayload` 没有infraFileId或bytes hash；`DccPreviewAccessResult`/`DccControlledFileAccessEventDO`也不保存metadata时选中的preview artifact身份。
- 结果是静态上可能读到metadata之后的新bytes；这不是已运行复现，也不是账号停用/改派绕过。Office路径不同：OnlyOffice payload已有`infraFileId`并在callback核对。

### 代码依据
1. `DccControlledFileQueryServiceImpl.getPreviewMetadataInScope` 约1989行先解析当前 `binaryFileId`、调用 `requireBusinessFileAccess`，随后 `DccControlledPreviewAccessService.prepareAccess`只传 `file/version/event purpose`。
2. `DccPreviewAccessRequest`、`DccPreviewAccessResult` 与 `DccControlledFileAccessEventDO` 无预览infra file ID/hash字段。
3. `DccControlledFileQueryServiceImpl.readPreviewBinary` 约2639行再次 `resolveBinaryFileId`；`DccViewerTokenExpectedContext`仅tenant/user/file/version/event/purpose/ttl/nonce/tokenId；`requireMatchingPreviewEvidence`也只比对受控文件版本和event/trace身份。
4. 对照 `DccOnlyOfficePreviewTokenService.issueControlledFile` / `PreviewTokenPayload.infraFileId` / `readOnlyOfficePreviewBinary` 约2380行，Office已有来源文件身份绑定，说明普通预览合同缺少同等来源事实。

### BDD / RED交接计划
- BDD R3 -> Given V metadata固定选中F1，When binary前V指针切到F2，Then普通预览必须拒绝context mismatch或仍读取F1，不能读取F2；账号停用/改派/状态变化的既有二次拒绝仍需保留。
- RED（尚未执行）：由Root在唯一Maven票下冻结F1/F2，真实Query+preview access fixture取得普通PDF/图片metadata后改变source/published指针，调用binary并断言F2零storage bytes；另覆盖FileDO path/hash变化。该测试需要跨Query/preview access/token合同，当前线程不改源码/测试宿主。
- GREEN候选（未决定）：在正式access event/viewer token中绑定选定infraFileId及必要hash，binary使用绑定事实并拒绝当前指针漂移；不得用当前最新指针静默替代。是否绑定FileDO hash需Root依据现有不可变文件合同确定。

### 当前边界
- 本次R3只读复核没有运行RED、没有生产改动、没有Maven/Node/shared target/fulltypes。不存在本线程GREEN或回归PASS。
- R3与R1/R2/F01软件关闭独立；旧125/181/types仅为上一轮证据。R3交给Root确认正式不变式和文件归属后再推进。

## R3 交接完成
- 本轮静态交接已完成，未继续扩展扫描。R3仍是跨 owner 未修复缺口，不得写成已解决或GREEN。

## 本次继续R2（最新，只读无新缺陷）
- 再次逐项追踪日常列表allowedActions、精确详情reader/Query窄context、metadata/binary、Office callback及reading start/heartbeat/stop/summary，在草稿/待生效、任务改派与账号停用边界未发现新的具体正文授权旁路。
- F01窄入口仍传入原context并先正文guard，只有DAILY复用根行/历史选项限制；generic get保持原合同，无根ThreadLocal历史遍历污染。跨端token/session重新鉴权链仍在。
- 列表动作投影不是授权凭证；改派后的原任务受理人拒绝不应误伤用户独立拥有的ACTIVE项目阅读或合法历史读取。本次无新行为修改，不新增重复测试，不重新启动旧套件。
- 涉及14份源码/指纹及逐入口依据见verification-report.md最新R2节和daily-entry-review-r2.json。Root旧125/181/完整类型引用长期归档doc/tasks/20261015-dcc-six-thread-followup-review/verification-results.json，仅为上一轮证据。
- 没有新增需Root分配的修复项。仍保留真实运行/MySQL/E2E/并发/外部Office未验边界，Git提交推送等禁止动作未执行。

## Root M01正式关闭（当前结论）
- Root上一轮已完成R01/R02/F01真实RED/GREEN及相邻回归：-am定向10类exit0，freshXML125/125，0失败/错误/跳过。长期完整证据引用doc/tasks/20261015-dcc-six-thread-followup-review/verification-results.json及execution-log.md；临时票据已被Root归档清理，不再依赖其路径。
- F01实现已只读确认：SelectedDetailReader调用query.getControlledFile(actor,id,context)，Query新增窄重载先核验精确context/guard，再取得原generic投影，仅DAILY复用restrictDailyBodyProjection；generic两参数get保持原行为，未把根context绑定ThreadLocal遍历历史。
- SelectedDaily6与DailyBrowser11均在本票通过，SelectedDetailService7/HTTP3与BodyReadCurrent27/Guard19/Context3/Office8为同票相邻覆盖。F01软件验证已关闭，不再等待分配/补测/Maven。
- R01已比较完整Duration后再累计整数秒；R02受控四路为认证入口并保留精确业务/token守卫。ReadingSession29、ReadingController12通过，跨端DAILY拒绝review保持有效范围。
- Root真实RED11中3断言失败、4实际旧权限拒绝已保存；Root确认3生产文件原字节SHA恢复0mismatch。本线程没有执行恢复或重跑。
- Root前端viewer82和详情99独立通过、各0drift；不与125或历史115/77/344/26累加。完整类型回执时仍在复跑，本线程后续只读fe-types-green-result.json见exit0/0drift，未运行types。
- 当前仅保留运行/MySQL/E2E/Git未验边界；本线程未改生产/测试，Query两文件仍停写。下方待分配/待实现描述仅为历史检查点，不再作为当前阻塞。

## Root接受并分配F01（历史交接）
- Root已将F01整体交给精确详情owner，授权其Query interface/Impl窄context详情入口、DccSelectedDetailReadService及自有测试；generic get保持不变，仅DAILY复用restrictDailyBodyProjection，不用根ThreadLocal遍历历史。
- 本线程Query interface/Impl均停写，只做实现后的只读复核；不新增重复实现或测试，保持reading-session的assertPreviewReadingAccess不变。
- DAILY精确详情最终补测由Root统一收口。跨端正文/token/session拒绝的既有源码review仍有效；不能把已分配等同已修复或已通过补测。
- 本次接收指令后的首次源码读取仍为旧authorizedProjection→query.getControlledFile(actor,id)，尚未看到窄context入口；此记录为owner修改前检查点，待其结果后更新。

## F01 精确DAILY详情投影未收窄同链工作稿动作（已关闭，以下为发现时证据）

证据性质：当前源码静态推导，未执行HTTP/Java/页面行为复现。不是正文泄露或计时越权的已验证缺陷。主分支int_qms / HEAD f06833f06；规范化指纹见followup-review-fingerprints.json。

### 触发条件与前后差异
- 同一Master含已激活ACTIVE版本A与可用源件WORKING版本W。
- 当前用户具备A的正常阅读权限，同时是W的申请人，且满足既有项目/目录等范围。
- 使用显式DAILY context读取A的精确详情。日常列表中W选项已被restrictDailyBodyProjection设置canPreview=false且移除PREVIEW，但该详情响应可重新返回W.canPreview=true和PREVIEW动作。
- 该条件下直接用DAILY读W的metadata/binary/reading-session仍被requireBodyRead拒绝。历史记录入口携带明确历史上下文后的独立授权不在本发现中否定；不能以F01关闭合法APPROVAL_HISTORY/VERSION_HISTORY。

### 完整代码链（仓库相对路径）
1. IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java，readControlledFileDetail约486行：存在bodyReadContext参数时调用selectedDetailReader.read。
2. IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSelectedDetailReadService.java，authorizedProjection约55行：guard.requireBodyRead(actor,A,DAILY)验证A后，调用query.getControlledFile(actor,A.id)，没有将context带入投影或进行DAILY选项收窄。
3. IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java，getControlledFile约617行→toRespVO约3620行→buildVersionHistory约4430行：当前ThreadLocal bodyReadContext为空，历史W的canReadBinary采用原requester编制放行（约3000行），再buildActionProjection加入PREVIEW。
4. 同文件restrictDailyBodyProjection约2542行仅由日常browser路径约432/476行调用，未被上述详情路径消费。

### 建议修复归属
- Root先确认“精确DAILY详情响应的历史选项也必须使用DAILY投影”的合同（本次委派的一致性要求支持此判断）。
- 建议在精确详情reader→Query边界传入明确的投影意图，并对DAILY根行和版本选项复用restrictDailyBodyProjection。不要把context只塞入整个toRespVO的ThreadLocal：其历史遍历可能拿根行context.requireFile校验另一个历史ID，导致合法版本链异常。
- 不要全局改getControlledFile默认投影来解决：该方法也被专门编制、审批/历史和管理读取复用，须保留各自授权及动作合同。
- 涉及DccSelectedDetailReadService.java / DccControlledFileQueryService.java及精确详情测试，均超出本轮本线程允许的QueryImpl/DailyBrowser测试范围，因此当前不修改代码，等待Root分配。Controller/Viewer/详情前端与assertPreviewReadingAccess均不需为F01放宽。

### 验收BDD与最小测试建议（尚未执行）
- Given ACTIVE A和同Master WORKING W，当前用户也是W申请人，When 精确DAILY详情读A，Then 根行保留原阅读权，W的canPreview=false且allowedActions无PREVIEW，元数据/其他管理动作保留。
- Given 同一W，When 以DAILY请求精确详情、metadata、binary、reading start/heartbeat/stop/summary，Then 在token签发、正文读取和session写入前拒绝。
- Given 合法精确AUTHORING / APPROVAL_TASK / APPROVAL_HISTORY / VERSION_HISTORY，When 读取对应详情和正文，Then 按原身份授权，不被DAILY选项过滤误伤；任务/轮次/租户替换仍拒绝。
- Root按最终改动选最小新RED/GREEN，不重跑无变化的115/77；历史344不是新PASS。本线程未新增未运行测试来伪称RED。

## reading-session 只读结论
- controlled四类入口均经ReadingSessionService.authorize→Query.assertPreviewReadingAccess→withBodyReadContext→Guard.requireBodyRead；DAILY仅ACTIVE且activatedTime非空，工作/草稿/待生效拒绝。start/advance锁后再复核，summary查询前亦授权。
- context purpose绑定event，viewer token绑定tenant/user/file/version/event/purpose/nonce/id；requireMatchingPreviewEvidence核对同round。已有审批token不能当DAILY token使用。
- generic ONLINE_FILE分支校验专用event版本/purpose后重新business provider授权；正式DCC源件不能靠更换路径绕开无context的DAILY门禁。
- 未改阅读线程assertPreviewReadingAccess；未发现上述正文/token/session链新增DAILY草稿放行。此结论仅源码复核，未覆盖真实服务/库/外部存储/并发/E2E。

## 交还与权限
- 本轮只修改本目录文档/指纹，不写生产或测试源码；不得把他人并行变化归为本线程实现。
- Root115/115仅7类、前端77/77仅10文件，是委派中回执的先前检查点，未在本轮重跑。
- F01已由Root M01补测通过并关闭；本线程只读复核和归档，不再等待本次Maven。Git/E2E/真实DB写入/服务/remote/subagents仍禁止。
