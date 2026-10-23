# R1624-06 四格式完整合同与最小修复提案

状态：`SUPERSEDED_BY_ROOT_PNG_TEXT_SCOPE`。Root明确不采用本文原生无PDF章/新模式提案；主方案为`r06-png-text-pdf-scope-20261019.md`。本文仅保留完整调用链分析历史，不再准备原生发布实现。

## 已通过范围继承

- PDF/Office正式A/1两账号的工具、水印、真实失焦、重开及账号累计隔离沿用已有真实UI证据，不整体重跑。
- Root最终证据：`run-2026-10-18T22-10-44-428Z-3b2de8ff/root-version-continuation-proof.json`，状态`PDF_OFFICE_VERSION_BRANCH_PASS_REMAINING_FORMATS_BLOCKED`。
- 原Office4240→4253→4254是同一文件链，4254精确AUTHORING、原生新标记和草稿/原A1读时隔离已PASS：草稿重开114→115秒，自身7秒；原A1保留旧标记、不含新标记。原4240/4253/4254与签名不得重建、重检入或重签。
- 仅处理PNG/TXT/WEBM/WAV；不顺便开放全部`image/*`、`text/*`、视频/音频扩展名或Office新类型。

## 根因及完整调用链

下表路径均相对仓库根。检查源码只能说明合同，不冒充真实页面执行。

| 环节 | 实际锚点 | 结果及影响 |
| --- | --- | --- |
| 前端SOURCE选择 | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts`：`EDITABLE_SOURCE_EXTENSIONS`、`validateControlledFileSelection` | 九类Office/PDF/图纸名单；四个目标扩展名全部拒绝。既有真实UI选择均在上传请求前被拒。 |
| 上传后端 | `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadServiceImpl.java`：`validatePreviewPurposeName` | SOURCE通过`DccControlledFileUploadTypePolicy.isAllowedEditableSourceName`，第二道同样拒绝；用途、同租户、类别UPLOAD、项目编制、大小、独立会话和ticket守卫不能放松。 |
| 原件与票据 | 同类`storePreviewFile`、`buildUploadResponse`及`DccControlledFileWorkflowServiceImpl.validateSubmission...`附近源件策略调用 | 真实源字节/名称存入infra，ticket冻结hash、purpose、actor、session；提交再用同一类型策略。不能只改前端或把普通附件冒充SOURCE。 |
| 正文类型 | `.../enums/DccControlledFilePreviewKindEnum.java`：`resolve`；`.../service/filepreview/DccOnlineFilePreviewServiceImpl.java`；前端`.../controlled-file/view/index.vue` | 已有IMAGE/TEXT/VIDEO/AUDIO原生渲染；这不证明这些格式已获受控发布合同。用户MIME亦不能作为任意扩展名放行依据。 |
| 转换与盖章 | `.../service/file/DccControlledFileFinalizationServiceImpl.java`：`resolveStampedPublishedArtifact`、`stampAndStorePdf`；`DccOnlyOfficeDocumentPdfConversionService.convertToPdf` | 所有非PDF统一OnlyOffice转换，结果必须真实PDF，再盖章并以PDF落库。媒体没有已定义的有效转换或盖章规则。 |
| 受控落位 | `DccControlledFileLifecycleService.completeControl`、`DccControlledContentAdapter.requireNativeControlledFile` | 强制`publishedFileId`、`stampedFileId`同时存在；只让媒体绕过转换会在此失败。 |
| 其它消费者 | `DccControlledFileQueryServiceImpl`受控阶段/作废守卫、`DccControlledFileFinalizationServiceImpl`幂等/重放守卫 | 也依赖stamp必有。Query由22独占，本线程不能改。 |
| 签名与哈希 | `DccControlledFileSignatureBindingService.bindPublishedCopy`、`requireControlledCopyFile` | 可绑定真实非空物理副本及SHA256，并非必须PDF；可复用，不再造一套签名。须保留当前四级签名完整性和精确租户/版本绑定。 |
| 作废存储 | `DccObsoleteFileStorageService.resolveArtifactFiles` | 枚举真实非空artifact ID，不要求PDF；原件/发布件可沿用，但新增产物须测试事务及去重。 |
| 打印 | `DccControlledFilePrintServiceImpl` | 读取发布件/源件后交打印链；原生媒体不可自动按PDF打印。须明确拒绝不适用打印，不能宣称有PDF打印件。 |

## 必须先定的业务合同

现存数据没有“原生发布无需可视PDF章”的正式配置，类别DO也没有可将盖章需求合法置为不适用的字段。不能将`stampedFileId=sourceFileId`、伪造一页PDF、改MIME或`null`自动当成功。

建议Root选择一个显式合同后再分配生产范围：

**推荐：四格式原生受控发布，真实电子签名封存字节，可视PDF章明确不适用。** PNG/TXT/WEBM/WAV保持原格式；发布生成独立真实物理副本，源件及发布副本字节相同，原件不变；已有签名绑定服务绑定该真实副本SHA256。`publishedFileId`指向真实新副本，`stampedFileId`保持空，界面显示“原生受控副本，电子签名绑定；PDF盖章不适用”，不得显示“盖章成功”。

这需要Root明确批准“电子签名原生副本可作为受控发布产物”的业务规则。它不是现有合同已经支持的行为。若业务坚持所有格式必须有真实可视PDF盖章件，则WEBM/WAV完整发布仍BLOCKED，应保留拒绝并明确原因；不能通过仅转换PNG/TXT宣称四格式均修复。

图片或文本也可以做真实像素/正文转换生成PDF，但这会改变正式浏览格式、原生工具与媒体合同不一致；不建议引入四条互不相同的发布流程来绕开本次问题。

## 最小实现边界（提案，未获生产写入授权）

1. 一个明确的原生产物模式，拟增加`dcc_controlled_file.publication_artifact_kind`，值`STAMPED_PDF`与`NATIVE_SIGNED`。新四格式明确写入`NATIVE_SIGNED`；既有PDF/Office/图纸分支维持原算法。迁移需Root校验真实schema与历史数据：只对实际存在的PDF发布/盖章产物设置旧模式；异常行必须报告，不能用后缀猜测修复数据。若Root决定用独立原生发布证据表而非列，二者只选其一，不双建模型。
2. 集中新增`DccControlledFilePublicationArtifactPolicy`：严格校验已持久模式、源件/真实发布物理文件、MIME/内容、hash和签名证据；PDF模式继续要求真实盖章件；原生模式只允许四个精确类型、真实副本、有效签名绑定且stamp为空。未知模式/缺物理文件/类型冲突/跨租户/哈希不符均显式失败。
3. `FinalizationService`复用该策略分流：PDF/Office/图纸继续原转换盖章；四格式原生copy-and-bind，再推进现有生命周期，任何失败不启用、不修改原ACTIVE/master，不伪造stamp，不吞异常。副本需有失败清理和幂等复用检查。
4. 同一策略用于生命周期、平台投影、幂等/重放守卫。Query相关检查由22/Root在其唯一范围内接入，不由24改。先列齐调用点，不能仅删除`stampedFileId != null`。
5. 上传前后端及提交策略只增加PNG/TXT/WEBM/WAV。验证扩展名+实际内容：PNG签名和解码、TXT严格UTF-8与非空、WEBM EBML/容器可读及媒体类型、WAV RIFF/WAVE头和可读采样；非法或不一致必须拒绝，不用客户端MIME决定类型。容器可读与编解码支持需采用仓库已有能力并明确限制，缺依赖不静默假成功。
6. DTO/详情按明确模式展示原生发布与stamp不适用；不改共享viewer/readToken或放宽正文鉴权。若确需相关字段/门禁，由Root自改。原生媒体打印入口/后端应给准确不适用错误，不借用转换失败当业务说明。

### 拟改文件与唯一归属

| 范围 | 文件 | 本轮处理 |
| --- | --- | --- |
| 四格式准入 | `IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts`、后端`DccControlledFileUploadTypePolicy.java`、`DccControlledFileUploadServiceImpl.java` | 待Root授24唯一写入；index.vue已引用公共accept/message，先不复制第二份名单。Workflow已有公共策略调用无需预定修改。 |
| 原生发布 | 新`DccControlledFilePublicationArtifactPolicy.java`、`DccControlledFileFinalizationServiceImpl.java` | 待Root确认业务规则及唯一范围后归24。 |
| 持久模式 | `DccControlledFileDO.java`、迁移SQL/测试schema | 仅Root确认并安排schema交付后处理。无写库授权。 |
| 受控消费者 | `DccControlledFileLifecycleService.java`、`DccControlledContentAdapter.java`、`DccControlledFilePrintServiceImpl.java` | 待Root划范围；仅原生artifact相关门禁。 |
| Query与公共预览 | `DccControlledFileQueryServiceImpl.java`、共享viewer/readToken及必要VO/详情展示 | Query22独占，viewer/readToken Root独占；24仅提交依赖合同和精确接入点，不改这些文件。 |
| 测试 | 独立`DccR06NativePublication...Test.java`、真实前端validator定向测试及本任务四格式UI脚本 | 可先准备本任务RED；Java/Maven/构建由Root串行。历史PDF/Office业务不重跑。 |

上表是最小完整合同所需的依赖面，不是授权清单。Root若保留“所有格式必须PDF章”的规则，本方案不得实施为原生无章发布。

## BDD与严格RED/GREEN设计

### BDD R06-INGRESS

Given 原有合法Config18、项目304/本例类型和类别、四个真实标记源件；When 从正式SOURCE选择PNG/TXT/WEBM/WAV；Then 各自通过前后端准入并保留精确原名/原字节/hash，预览种类分别IMAGE/TEXT/VIDEO/AUDIO。EXE/ZIP、空文件、扩展名/MIME/真实内容不符拒绝，失败不占名称/票据。

- 当前前端RED：`node doc/tasks/20261016-dcc-e2e-24/r06-source-contract-red.cjs`，执行实际TypeScript选择函数，四个真实候选均预期valid=true；预期因当前窄白名单退出1。后端静态缺项只记审计，不冒充服务单测。
- 后端RED交Root：真实UploadService+ticket持久事务/当前类型策略，四目标成功保存前应失败；非法内容用真实bytes，不绕守卫、不改有效夹具迎合结果。GREEN后保留异常回滚、会话/租户/actor/name/hash约束。

### BDD R06-PUBLISH（待Root批准原生合同）

Given 四格式分别完成合法审批且冻结原件/版本；When 正式文控受控处理；Then 原格式真实副本发布、sha匹配、全部签名绑定、stamp不适用被显式记录，master只在完整效果成功后更新。原件物理ID不冒充发布/盖章ID。

- 服务RED：四格式不得调用OnlyOffice/PDF盖章；真实存储副本类型与hash不变，签名指向真实副本；当前实现会调用统一PDF转换，先以实际该行为失败。
- 生命周期RED：无显式原生模式或缺副本/签名/内容hash必须拒；明确原生模式且真实签名副本完整才可受控；PDF模式缺盖章仍拒。真实事务回滚/幂等必须覆盖，不只mock成功返回。
- 负例：native被写伪stamp、跨tenant/source/version、绑定hash篡改、重复完成、存储失败、签名绑定失败均不推进ACTIVE/master。相关Query消费者由其归属线程补精确测试。

### BDD R06-REAL-UI

Given Root新包/hash/schema/health和本人登录放行；When 每个剩余格式独立真实页面创建本任务专属SOURCE、审批及受控终态，再开正式发布正文；Then 按适用能力逐项验证真实正文、当前人/版本水印、有效阅读失焦/恢复、关闭重开字节/像素/时长不变，无保存回原件，且无错误盖章成功展示。

PNG：像素/尺寸/像素指纹、放缩/旋转；TXT：实际文本、80行分页/搜索及变换；WEBM：实际视频帧、时长/播放进度、适用变换；WAV：实际音频ready/时长/播放进度。媒体分页/全文搜索、音频放缩/旋转没有原业务语义，不做伪分页、OCR/转录，也不标PASS；Root/业务需独立给口径。用例整体仍按未定义项如实BLOCKED。

复验只跑四格式及其新发布产物约束；已通过PDF/Office正文、原4253/4254切版和读时隔离只引用已有证据。必要的旧格式单元回归由Root安排，不重复真实业务审批。

## 当前结论

根因不是单个accept字符串。可以交付的最小完整修复需要一个明确的原生受控/盖章规则；在Root定规则及划唯一生产范围之前，本线程保留四格式入口RED并停止生产写入。此前通过结果继续有效，当前没有新真实UI PASS或新后端测试PASS。
