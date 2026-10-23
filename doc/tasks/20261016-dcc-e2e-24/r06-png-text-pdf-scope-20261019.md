# R1624-06 PNG/TXT最小完整修复提案

状态：READY_FOR_ROOT_OWNERSHIP，生产未改。本文为主方案，取代同目录旧四格式原生发布提案。Root明确不采用publication_artifact_kind/NATIVE_SIGNED、无PDF章、迁移或跨生命周期改造。

## 继承与范围

PDF/Office工具、两账号阅读隔离、Office4240→4253→4254草稿及原A1/草稿读时隔离，均引用`run-2026-10-18T22-10-44-428Z-3b2de8ff/root-version-continuation-proof.json`，不重跑、不重签、不重建。当前只实现PNG/TXT；WEBM/WAV不放行SOURCE，发布/盖章和分页/全文搜索语义待明确。

Root当前运行证明`reboot1019-runtime-proof.json`：8061/PID20976、48061/PID25124、120105包/hash ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3，32文件/0待办保护，5627暂停。该包未含本轮修复。Maven、构建及部署Root独占，本线程待唯一生产范围。

## 根因与最小完整链

- 前端`upload/submitter.ts`和后端`DccControlledFileUploadTypePolicy`都拒绝PNG/TXT。UploadService建立原件票据前拒绝，Workflow提交又复用同一策略，不能只改前端accept。
- `Finalization.resolveStampedPublishedArtifact`对非PDF调用`DccDocumentPdfConversionService`，当前唯一实现统一OnlyOffice；本地真实像素/全文转换缺失。
- 最小方案：PNG/TXT精确准入→保留真实原名/原件/hash→审批原生IMAGE/TEXT预览→本地实际内容生成`DccConvertedPdf`→原`stampAndStorePdf`→现有真实PDF存储/签名hash绑定/受控/归档。
- 源件ID不填入发布/盖章ID，真实生成的盖章PDF沿现有模型同时作为published/stamped产物。无DDL、无新发布模式、不放松原stamp必有守卫。
- 审批原件工具和正式受控PDF工具分别验收：正式PNG转换PDF不冒称原生图片渲染，也不虚构OCR搜索；TXT转换PDF必须包含全文并能搜索。

## 可复用能力与依赖依据

| 能力 | 源码依据 | 用法 |
| --- | --- | --- |
| PDF库 | DCC pom已有PDFBox 2.0.32 | PDDocument/PDPage/PDPageContentStream，不添库。 |
| PNG解码/无损嵌入 | JDK ImageIO；签名PDF类已用LosslessFactory | 实际像素嵌入，保比例、不裁切，不OCR。 |
| 中文字形 | 签名PDF已用PDType0Font，现有路径C:/Windows/Fonts/simhei.ttf | 只复用字体嵌入技术，不能复制电脑路径为默认依赖。新增显式字体Resource配置，缺文件/字形准确失败。 |
| 转换合同 | DccDocumentPdfConversionService、DccConvertedPdf | 接口不变，返回可解析真实PDF及派生.pdf名。 |
| 源件读取权限 | OnlyOffice转换类requireConversionAccess | PNG/TXT读取真实bytes同样先通过CONVERT业务授权；不新增token旁路。 |
| 盖章 | DccPdfStampService | 复用正式章资源与真实盖章方法，失败不推进受控。 |
| 发布签名与归档 | 原Finalization、SignatureBindingService、Lifecycle、ObsoleteStorage | 绑定实际PDF字节hash，原件独立保存，原消费者保持。 |

没有证据证明OnlyOffice对PNG/TXT完整、确定转换；不能只把它们送远程converter当完成。无需FFmpeg/OCR/转录。本机只读确认simhei.ttf存在、大小9753388字节，但未据此证明服务器交付或字体嵌入许可。唯一待Root明确的外部资源是可嵌入且覆盖目标中文的TTF；方案不打包来源/许可未明字体，不使用替代字体静默降级。

## 转换规则

PNG：精确.png与真实PNG magic，ImageIO可解码、宽高/像素上限明确；以实际像素无损嵌入A4内保持比例完整缩放，预留页顶受控章区域，正文不被固定右上角章覆盖。损坏、空内容、超限准确失败，不画文件名占位。

TXT：精确.txt，严格UTF-8（可明确支持UTF-8 BOM），非法字节不替换/猜编码；CRLF/LF排版换行、空行保留，tab按明确固定tab stop展开，其余不可显示控制字符拒绝。按嵌入字体实际宽度换行、按页高分页，长行不截断，中文/英文标记均可由PDFTextStripper完整提取。空内容、缺字体、无字形、超页数/大小准确拒绝。每页留顶端章区域；源字节/hash不变。

所有内容大小约束结合原UploadSizePolicy并在解码/排版前检查，不能通过丢内容满足上限。

## 精确拟改生产文件（待Root唯一授权）

后端简称根为`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`。

| 文件 | 最小改动 |
| --- | --- |
| IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts | 公共名单/accept/regex/提示只加png/txt。WEBM/WAV/ZIP/EXE维持拒绝。index.vue引用公共常量，先不改。 |
| 后端service/file/DccControlledFileUploadTypePolicy.java | 同步精确两类型准入与提示，提交仍复用公共策略。 |
| 后端service/file/DccControlledFileUploadServiceImpl.java | 读完真实bytes、名称claim/ticket前调用共用PNG/TXT内容校验，不改项目/类别/用途/租户/actor/session。 |
| 后端新增service/file/DccImageTextPdfConverter.java | 实际PNG/全文PDF转换及共用内容校验，无业务写副作用，返回现有DccConvertedPdf；字体Resource/大小上限明确。 |
| 后端service/file/DccOnlyOfficeDocumentPdfConversionService.java | 精确PNG/TXT分支：requireSource→原CONVERT授权→FileService读bytes→本地转换。其它Office分支及原缺配置错误顺序不变；本地不造OnlyOffice token。 |
| 后端新增service/file/DccImageTextPdfProperties.java | 显式yudao.dcc.conversion.text-font Resource；PNG不依赖字体，TXT缺配置准确失败。Root提供运行值，本线程不改环境/服务配置。 |

优先复用SOURCE_FILE_TYPE_INVALID、PDF_CONVERSION_FAILED并保留准确原因，不预定改公共ErrorCodeConstants。Finalization、生命周期、Query、共享viewer/readToken、DO/schema均不改；额外文件如确有必要，先报Root。

保留既有唯一转换接口实现，在其窄分支调用本地纯转换器，避免新增第二Spring实现/Primary路由器或重命名全局注入。服务名较窄不作为重构理由。

## BDD与严格TDD

BDD PNG：Given带可辨识像素图案的真实PNG、原名/hash；When合法SOURCE上传与受控转换；Then PDF保留实际像素和完整比例、真实章不遮正文，源件不变，实际盖章PDF被签名/归档绑定。

BDD TXT：Given含中文/ASCII标记、空行、tab、长行、多页的UTF-8正文；When上传和受控；Then PDFTextStripper全文/顺序及PDFRenderer非空均通过，真实章/签名/归档目标正确；非法UTF-8/缺字体/无字形拒绝，不截断或替换。

BDD失败原子性：Given损坏源件或转换/盖章/绑定失败；When正式受控处理；Then不激活master、不替代原正式版本，失败准确可见，源件与已完成审批保留，重试仅按现有正式入口/幂等。

### 测试文件与责任

| 测试文件 | RED/GREEN和真实断言 |
| --- | --- |
| 本任务r06-source-contract-red.cjs --png-text | 已执行当前限定RED退出1：PNG/TXT预期通过但实际拒绝；WEBM/WAV及危险/多选仍正确拒绝。四格式历史RED另存，避免把媒体纳入GREEN要求。 |
| 新后端src/test/java/.../service/file/DccImageTextPdfConverterTest.java | 真实ImageIO/PDFRenderer像素/比例、真中文PDFTextStripper完整多页、编码/字体/空/损坏/超限。禁止只断言PDF头或伪造%PDF。 |
| 现有DccOnlyOfficeDocumentPdfConversionServiceTest.java | 本地两类型先CONVERT授权再读源，拒绝时不读bytes/不调用OnlyOffice；旧Office分支的授权/config/token/转换调用维持。 |
| 现有DccControlledFileUploadApiTest.java | 已核对实际文件存在；PNG/TXT真实ticket/hash；损坏内容在claim/ticket前拒绝、事务无残留；原用途/项目/actor/name/session守卫保持。 |
| 现有DccControlledFileFinalizationServiceImplTest.java | 两种真实转换PDF再真实DccPdfStampService，验证存储字节和published/stamped/signature目标；失败不激活master。 |
| 本任务新r06-png-text-real.e2e.cjs | 新包READY后仅两格式真实UI：SOURCE原生审批预览→合法签核→受控PDF终态→正文/真章/水印/读时/重开/签名及原件证据。 |

Java/Maven由Root串行；先行为RED，再限定文件GREEN，再必要单元回归，最后只复验PNG/TXT真实UI。已通过PDF/Office真实业务与原4253/4254不重复。音视频发布/盖章/分页/搜索不实现、不造章；整体六格式仍如实列BLOCKED。

## 交付门禁

方案/复用库/字体编码/文件清单已具体。等待Root确认6个生产文件范围和字体资源方式后才写生产；不因旧包环境READY宣称新代码通过。当前没有新后端测试或新真实UI通过结论。
