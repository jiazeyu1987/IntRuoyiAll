# DCC 全格式 viewer 验证报告

## 最新第四轮：SDK onload 就绪与重挂载（2026-10-15）

本轮仅收口新增边界，不重开C01–C06。上一轮Root 200/200、types/build均为有效历史检查点，没有在本线程重跑或改写为本次执行。仅修改OnlyOffice组件及自有failure-boundaries测试，未写view/index.vue、其他owner文件/共享宿主或依赖。

### 实际新 RED 与修复

- 新用例首先实际1/1 FAIL：script onload但无DocsAPI时只设置本地error，无当前父error；resolved全局promise/base未释放，同URL重挂载卡在失败缓存。
- 初步修复后再实际2/2 FAIL：DocEditor存在但不可构造时失败缓存未清；组件已卸载时先退出而没有释放失败loader缓存。这两项是本次指定的新边界，不是重开此前来源/async destroy。
- 最终在loader内统一验证API存在及构造能力，Reflect.construct只作能力检查，不执行SDK构造器。失败时仅在pending及base均仍属于本次请求时清promise/base/无效API，原错误继续抛给当前源typed error处理；卸载/旧generation不发事件，但共享依赖仍正常收口。
- loader返回已经验证的构造器供mount创建实例，保留mode=view、原精确URL/权限/generation/watermark；同URL合法SDK重新加载就绪后才opened，不用旧SDK/下载/源件fallback。

### 本轮新增验证

工作目录IntRuoyiFronted，最终实际命令：

```powershell
node --test --test-name-pattern='script onload|same-URL pending' tests/unit/dcc-full-format-office-failure-boundaries.test.cjs
node --test --test-name-pattern='SFC compiles' tests/unit/dcc-full-format-office-review.test.cjs
pnpm exec eslint src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue
```

新增 **4/4 PASS、0skip**，仅选择的实际SFC script/template/style编译 **1/1 PASS**，lint exit0。四项覆盖接口缺失、非构造器（对象/箭头函数）、卸载后完成、同URL在途重挂载及正式重试。中途局部实现的24项受影响Office回归只作为过程记录；最终仅选择新4项及编译1项，不与此前22/200混加，不再重复旧边界全集。未跑旧G09、Maven/sharedtarget/fulltypes/build。

### 只读审查与边界

- 初始化/销毁/切源交错的原generation/source检查不改；同URL两次在途mount只允许当前generation报告一次loader失败，随后正式重试才成功。缓存释放同pending+base身份保护不清其他加载尝试。
- 几何源码viewportStyle/scalerStyle按DOM clientWidth和720逻辑高计算，stage observer仍核对element/卸载身份。0宽可能来自未布局/父容器隐藏，不能未经恢复合同将正常隐藏一律变成永久文档错误；元素隐藏后的阅读暂停/恢复属于viewer/阅读owner边界，已在integration note说明，不冒称已验证像素/可见性。
- 原分页/搜索、只读配置、旋转/缩放wrapper未改；没有伪格式工具、OCR、转写、外部SDK服务或部署。未执行E2E、实库/迁移、服务启停、remote或Git提交推送。
- 当前Office UTF-8/LF SHA256：`46565c20cadd0edabcceff5ff8894e0a0de9e8dba644e738915024ac70769885`；failure-boundaries测试：`79bd3101bd194686456fbe0c3f0dd3d37e34054500a36f5c6f35ce7909c95165`。独立第四轮证据见office-boundary-round4-fingerprints.json，第三轮归档保持原字段/指纹，不覆盖历史。
- 生产/tests已冻结交还；剩余Root按新输入选择必要集成验证，Git收尾未授权，任务不标completed。本轮SDK缺失等新增实现已收口，没有请求额外授权。
- 第四轮ready_for_closeout下cleanup preview exit0，8项证据全keep、delete空、warnings none；按禁止Git收尾改blocked后apply实际exit3、零删除，不记apply PASS。限定任务文件diff检查及UTF-8/结构通过。

## 第三轮历史新增错误边界（2026-10-15）

**第三轮共享fixture待办已关闭（来源：Root正式回执）**：Root亲跑最终旧G09 suite取得exit1（8项因缺SDK来源无法建立editor配置），仅在window host补 `__dccOnlyOfficeScriptBaseUrl: props.baseUrl` 一行，保留原全部断言；同9项实际 **exit0、9/9 PASS、0skip**，未改生产。本线程Office cfbf08d…、自有tests与指纹继续冻结，没有重跑或修改共享宿主。Root当前合并viewer组结果另行记录，不由这9项推断整体通过。

第三轮沿用原目录与OnlyOffice独占范围。Root构建冻结期间只读生产、先写BDD/独立RED；收到Root明确解除冻结后才改生产。最新生产输入是 `OnlyOfficeReadOnlyViewer.vue`，另改本线程自有review fixture并新增专属边界suite；没有写view/index.vue、阅读harness或其它owner资产。需求相容与七类格式逐项依据见integration-notes.md。

### 本次实际 RED 与最小修复

1. 初始新增4项：1 PASS/3 FAIL，复现旧editor销毁异常绕过可见错误路径、已加载SDK跨服务复用、在途SDK Promise跨服务复用。销毁引用先脱离，cleanup移入当前source的try/catch；失败真实显示并复用既有typed error通知父页，不新建editor冒充成功。
2. 额外无来源SDK实际RED：已有旧DocsAPI没有服务来源也创建editor。rg核对src只有当前Office组件一处SDK加载方；正式记录真实加载请求的规范base URL，并拒绝不同服务或来源未记录的既有SDK/Promise，不按当前请求base猜来源、不回退旧SDK或源件。失败缓存按pending身份清理；合法同服务复用和正式重试仍通过。
3. Root指出的异步SDK onError销毁异常由2项独立RED复现：mountEditor已返回后SDK_CLOSE_FAILED直接冒泡。正式回调现在先失效generation、处理销毁，将服务失败与销毁失败同时写入唯一正式message；销毁后重查source/失败generation/卸载，不把旧失败写给新源。不使用空catch或假销毁成功。

### 本次定向 GREEN / REGRESSION

工作目录 `IntRuoyiFronted`，实际执行：

```powershell
node --test tests/unit/dcc-full-format-office-failure-boundaries.test.cjs tests/unit/dcc-full-format-office-review.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs
pnpm exec eslint src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue
```

最终组合 **22/22 PASS，0 skip**，Office lint exit0，实际SFC script/template/style编译通过。补销毁中卸载用例后，只重跑新增专属suite，**9/9 PASS，0 skip**；生产未再改。22与9覆盖重叠，不叠加为31。覆盖48几何组合、只读/URL/代际、真实失败缓存释放、异步清理时切源/卸载，以及同服务合法复用。

本线程适配前亲跑共享 `dcc-g09-office-readonly.test.cjs` 原宿主为 **1 PASS/8 FAIL，exit1**，保留为来源前置缺失的RED历史。Root随后亲跑原失败，仅给host.window补 `__dccOnlyOfficeScriptBaseUrl: props.baseUrl`，保持权限/URL/generation/几何全部原断言不变；Root正式回传同9项 **exit0、9/9 PASS、0skip**，来源fixture待办关闭。本线程未重跑或修改该宿主。上一轮Root9/9及中间版27/27不替代这次新回执。

### 当前指纹与验证边界

- 当前生产Office UTF-8/LF SHA256：`cfbf08d4d3a9e7a5b13209b3b590bc700bdc307bbf6ce96b516f248f44675e0d`。改前为e8fd检查点，当前源码和两项自有测试完整指纹见office-boundary-round3-fingerprints.json。
- view/index.vue的最终只读观察值为 `09ea0b785198591e27ba602e3d44c2eb57764aa81c0c452ade7f8dff019e689a`，其变化归阅读owner；本线程没有写入，不将其静态观察冒充完整计时/实库验证。
- Root此前125后端/181前端/完整类型是历史有效检查点。Root本次Vite构建exit0、2635输入0drift、3239产物是在本次Office改动前完成；以上均非本线程新执行，也不证明本次改后完整类型/构建已过。Root决定改后必要的集成验证。
- 本轮没有执行Maven/shared target、fulltypes、完整构建、依赖修改、E2E、外部Office/媒体解码/像素、实库/迁移、服务启停、远程发布或Git提交推送；没有新建线程/子Agent。
- 当前生产及自有测试冻结交还；共享fixture已由Root一行适配及9/9回执关闭，无本线程待实现/重跑项。合并viewer组与后续验证由Root记录；Git收尾未授权，不标completed。冻结manifest中的旧1/8为适配前快照，不作为当前阻塞，历史轮次不覆盖本次回执。
- 本次限定任务文件git diff --check exit0；真实blocked状态下cleanup preview exit3，明确要求ready_for_closeout/completed，不记PASS、不运行apply或删除；任务目录保留7个正式证据文件。

## 第二轮历史：Office 及阅读接线后只读复核（2026-10-15）

**共享合同待办已关闭（来源：Root 回传）**：Root 亲跑旧 9 项取得 8 PASS / 1 旧几何 FAIL 后，仅对测试作最小 fixture 适配：导出 viewportStyle，逻辑视窗断言 1000×720，并新增 90°/110% 占位 792×1100；原权限/URL/generation 断言保留。Root 回传同 9 项实际 **9/9 PASS、0 skip**，未改生产。本线程 Office 生产、两项自有测试及指纹继续冻结，未重跑套件；后续综合前端结果由 Root 单独记录。

当前交付范围仅 `OnlyOfficeReadOnlyViewer.vue` 和两项新增自有测试；**未修改 view/index.vue 或任何 shared harness**。以下第二轮结论优先于后文第一轮归档。

### 修复与 BDD 证据

- Office 以原生 `stageWidth × 720` 为稳定逻辑视窗，缩放/旋转只改变 CSS 和外层滚动占位，保留 editor 实例、document URL、只读权限和水印。fixed-height stage 里面新增独立 overflow:auto scroller，避免内部滚动条改变外层宽度量测造成反馈。
- 实际 SFC computed values 驱动 320/321/1000 三种宽 × 60/100/110/200% × 0/90/180/270° 共 **48 个几何组合**。以独立旋转角点求边界，验证全部落入非负可滚动矩形，且没有通过缩小 iframe 逻辑宽高挤压原生工具。此证据不是浏览器滚动/像素验收。
- SDK await 成功/失败、opened、error 均使用 generation + base URL + document URL + title + unavailableReason 精确 source 守卫；watch 前窗口的旧回执不污染当前 source。
- 构造同步 onError 后返回的失效实例被销毁，不成为当前实例；旧 stage/卸载后的尺寸回调不再写回。
- 初始独立 SFC 测试为 1 PASS / 6 FAIL，修复后 7/7 PASS。追加 110% 精度 RED：旧浮点运算使 720×1.1 上取整为 793，改为整数百分比优先运算后预期 792 PASS。

### 当前定向结果

在 `IntRuoyiFronted` 执行：

```powershell
node --test tests/unit/dcc-full-format-office-review.test.cjs tests/unit/dcc-full-format-post-reading.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs
pnpm exec eslint src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue
```

独立组合 **19/19 PASS，0 skip**，ESLint exit 0；实际 Office script/template/scoped-style 编译 PASS。自有测试仅执行真实 SFC 脚本/computed/SDK 适配器及只读 AST，不启动浏览器、外部 Office 或任何读取/写入真实服务。

适配前本线程实际执行 `node --test tests/unit/dcc-g09-office-readonly.test.cjs`，**8 PASS / 1 FAIL，exit 1**，作为 RED 历史保留。唯一失败是共享测试期待 `scalerStyle` 宽/高除以 zoomScale；Root 已适配逻辑视窗 1000px×720px、90°/110% 占位 792px×1100px，并回传同 9 项实际 **9/9 PASS、0 skip**。此新结果来自 Root，本线程未重跑、不改 shared harness；共享合同待办关闭。

未重跑第一轮 61 项或阅读线程的共享 harness，也未执行 fulltypes/build/Maven。不能将第一轮数字作为当前完整阅读集成的通过证据。

### 只读复核与集成边界

- 读取到的正式 viewer 已将正文成功 mark 接 `startPreviewReading`，清理/错误接 `closePreviewReading`，替代第一轮尚未接入时的限制说明。此复核不证明计时实库持久化结果。
- PDF/文本搜索分页、字节副本、图片/视频/文本变换占位、全部格式水印、Office keyed renderVersion 和不可预览短路仍保留。单独执行当前工具 handler 只改变 view state，不调用计时或原件写接口。
- 音频保留时间轴而无伪旋转/分页/转写；视频保留页面水印和关闭原生独立显示入口的配置；DOWNLOAD_ONLY/不可预览原因不取二进制、不以下载或源件回退冒充成功正文。
- Office 原生分页/搜索可见性、文档实际画面/工具布局、媒体编解码与像素效果仍需真实运行态验收；本轮未授权 E2E，因此未运行、不请求确认、不宣称通过。

### 指纹与收尾

- 当前 Office UTF-8/LF SHA256：`e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1`。
- 只读被测 viewer SHA256：`42253a6c8ead376e2135fb259d8368ae34cc77091c41e9a31c62efff4b29e8df`；其源由阅读线程独立修改，本线程仅记录读取时点。
- 完整基线/当前/测试指纹见 `office-review-fingerprints.json`；`source-fingerprints.json` 作为第一轮历史保留。
- 本轮入口按仓库规则做只读分支核对后未再执行 Git；没有任何 Git 写入、服务、实库、E2E、remote 或 subagents。
- 共享几何合同已由 Root 9/9 关闭；用户仍禁止 Git，任务状态为 blocked，不标 completed，后续综合前端验证由 Root 记录。冻结指纹清单内 8/9 regression 是交接前历史快照，不覆盖本文最新回传。本轮未运行内部使用 git rev-parse 的 cleanup 脚本，仅核对六项正式证据保留清单，不能把该清单核对写成 cleanup PASS；后文第一轮清理结果不冒充本轮结果。

## 第一轮历史报告（仅对应当时源码）

## 范围与基线

2026-10-15，在当前 `int_qms` 分支实现与验证本次授权的前端 viewer 修复。请求基线 `a3b8234ecfba6bbafff2e8aa4dc3d1e60eccb1d2` 是实际 HEAD `f06833f06c868dc3a451eb63d01381a467ecfe92` 的直接父提交；后者为文档提交，两者间 viewer 无代码差异。未回滚 HEAD 或并行改动。

既有 `doc/tasks/20261014-dcc-gap-viewer-tools/` 是 G09 原始实现主记录；本目录记录新增全格式核对及文本/媒体/几何修复，所有以下 PASS 均在本轮当前源码重跑取得，不用历史 PASS 替代。

## 改动文件

| 路径 | 本轮变更 |
|---|---|
| `IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue` | 文本分页/搜索/命中高亮/视图变换，视频变换，图片/文本/视频实测宽高及滚动占位，媒体播放水印边界 |
| `IntRuoyiFronted/tests/unit/dcc-full-format-viewer-contract.test.cjs` | 新增 10 项实际 SFC 脚本/模板、格式边界及隔离保护断言测试 |
| `IntRuoyiFronted/tests/unit/dcc-g09-viewer-tools.test.cjs` | 测试宿主补登记时间绑定；按真实查询变化验证 watch，不依赖 watcher 数组下标 |
| `IntRuoyiFronted/tests/e2e/dcc-common-file-preview-source.spec.js` | 纯静态变换能力合同扩至 TEXT/VIDEO，补实测占位接线断言 |
| `IntRuoyiFronted/tests/e2e/dcc-controlled-file-protection.contract.test.js` | 同步唯一纯变换断言函数的格式范围并补几何接线断言；完整入口未执行 |

OnlyOffice 生产组件、公共 API、vendor、依赖、lockfile、`detail/index.vue`、审计表、后端和阅读时长模型均未由本任务修改。OnlyOffice 与 HEAD 的 UTF-8/LF SHA256 相同，见 `source-fingerprints.json`。

## 当前格式能力

| 服务端 previewKind | 当前工具与水印/时间 | 适用边界 |
|---|---|---|
| PDF | 50%–250% 缩放、90° 旋转/复原、上一/下一页/跳页、真实 PDF.js 文本搜索与命中页导航；逐页水印；成功渲染后显示服务器登记时间 | 纯扫描/图片 PDF 显示无可搜索文本；不 OCR。页上像素高亮不是本轮新增范围，PDF 保持已有页码/摘录定位 |
| IMAGE | 50%–250% 缩放、90° 旋转/复原；实测宽高生成占位，页面与角标水印；对应 img load 后显示登记时间 | 单图没有正文分页或文本搜索；多帧/多页图片不添加假翻页；TIFF 等是否能显示取决于浏览器解码，失败显式报错 |
| OFFICE | 保留 OnlyOffice 60%–200% 视图缩放、90° 旋转/复原；原生分页/工作表/幻灯片导航及搜索工具继续由文档服务提供；水印保留，精确 document URL opened 后显示登记时间 | 已验证只读配置和回执隔离，未用真实 DOCX/XLSX/PPTX 服务画面证明原生工具可见或像素行为。需要真实 Document Server/受权样本验收 |
| TEXT | 每 80 原始行一页，上一/下一页/跳页；整段正文不区分大小写的字面搜索，包含跨视图分页短语；逐字符区分同页命中并高亮；50%–250% 缩放、90° 旋转/复原；水印随正文变换 | TXT/CSV/JSON/XML/HTML/代码等按服务端 TEXT 分类作为纯文本展示，不执行 HTML/脚本；保留现有 TextDecoder 路径，未增加编码探测或格式渲染器。80 行只是视图分页，不是源文件页码 |
| VIDEO | 原生播放/暂停/时间轴；新增 50%–250% 缩放、90° 旋转/复原及实测占位；画面水印和角标；loadeddata 后显示登记时间 | 时间轴代替翻页，无正文/OCR/字幕搜索。抑制原生全屏、画中画和远程播放入口，防止直接离开页面水印层；各浏览器实际控件/编解码需运行态验收 |
| AUDIO | 原生播放/暂停/时间轴，页面水印/角标；loadeddata 后显示登记时间 | 无图像可旋转、无文档页码或正文；不生成伪旋转/翻页/语音转写搜索。页面水印不是向音轨写水印 |
| DOWNLOAD_ONLY | 保持明确“不提供在线预览”，不加载 preview 二进制、不显示成功登记时间 | 不新增下载动作，不把原件/下载回退当成预览。CAD 只有服务端正式选择该版本 PDF 产物并返回 PDF 时才走 PDF 预览 |

## 受控边界与阅读时长集成

- 工具只改变 DOM/CSS、页码、搜索或 Canvas 画面；未添加源文件保存、转换上传或写回请求。preview bytes 只读，PDF worker 仍使用字节副本。
- 继续走统一 `getOnlineFilePreviewMetadata` / `previewOnlineFileWithWatermark`；未改变 token/nonce/accessEventCode/watermarkTraceCode、精确版本、任务或 distribution 身份。
- 服务端不可预览原因先于二进制读取，权限拒绝不重试为其他上下文，不回退到源文件。
- 几何 observer 绑定当前 generation 与 DOM ref；切版和卸载断开，旧回调不能恢复旧几何；object URL 按既有路径回收。
- `previewRegisteredAt` 是既有服务器 PREVIEW 登记时刻，Shanghai 显示；metadata 本身不等于正文成功。缺少字段显示未记录，不用客户端当前时间/水印时间或默认值补齐。
- **阅读时长 integration note**：当前只有 Office `handleOfficeOpened` 会向父级 emit `opened`；PDF/TEXT/图片/音视频在成功渲染/DOM load 后只置本地 `markRegisteredPreviewVisible`。累计阅读时长线程若需统一启动/停止/可见性事件，应由其确认并接入合同。本任务未改计时/审计/详情页，也不把登记时刻当累计秒数。

## RED 证据

1. 初始新合同 3 项：1 PASS / 2 FAIL，缺文本工具与视频变换。
2. 新增真实 SFC 行为合同扩至 7 项：5 PASS / 2 FAIL，跨分页短语丢失、没有实测几何占位；对应修复后 7/7 PASS。
3. 旧 common preview 静态合同和隔离 protection 纯变换断言仍要求 PDF/IMAGE，按产品扩展同步为 PDF/IMAGE/VIDEO/TEXT，同时保留原防护断言；更新后 PASS。
4. 首次组合回归两个真实 PDF.js 用例因未配置既有 canvas 模块前置失败，另一个旧 harness 按 watcher 下标清理而失败；配置真实现有 canvas 模块，并改为实际变更 PDF 查询后验证同步清命中，全部通过。不将依赖前置失败写成产品功能 RED。

## GREEN / REGRESSION

命令工作目录 `IntRuoyiFronted`。PDF 文本测试沿用既有 `G09_CANVAS_MODULE` 前置，须指向本机已安装的真实 `@napi-rs/canvas`；本轮没有新增依赖或替换 PDF.js。Node 环境给出的 legacy build 建议为 vendor 既有警告，真实当前 vendor 文本提取通过。

```powershell
node --test tests/unit/dcc-full-format-viewer-contract.test.cjs tests/unit/dcc-g09-viewer-tools.test.cjs tests/unit/dcc-g09-office-readonly.test.cjs tests/unit/dcc-g09-pdf-text.test.cjs tests/unit/dcc-g07-preview-registration.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs
```

exit 0，**45/45 PASS，0 skip**：本任务 10、G09 viewer 12、Office 行为 9、真实 PDF 文本 3、登记时间 4、Office 相邻只读/缩放 7。覆盖：真实 PDF.js 解析有效文本/扫描 PDF、源 bytes 不变；实际生产 SFC 脚本和 Vue 自定义 renderer 的页码/搜索/高亮/水印；切版/卸载/迟到回执；OnlyOffice 原始权限配置。

```powershell
node --test tests/unit/dcc-body-read-wrapper-g01.test.cjs tests/unit/dcc-g07-body-read-context.test.cjs
```

exit 0，**16/16 PASS，0 skip**：原 canonical source、metadata/binary wrapper、任务/历史/版本/distribution 上下文及拒绝不回退。合计 **61 项定向测试 PASS**。

```powershell
node tests/e2e/dcc-common-file-preview-source.spec.js
node tests/e2e/unified-online-file-preview-static.spec.js
pnpm exec eslint src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue
```

均 exit 0。前两者仅同步读取源码做断言，不启动 Playwright、浏览器或服务。新单测的生产 script/template/scoped-style 编译通过。限定本任务文件 `git diff --check` exit 0。

最后追加旧 observer 迟到回调与文本查询同步失效断言后，本任务 10/10 再通过，生产代码未再改动。

`dcc-controlled-file-protection.contract.test.js` 完整入口会启动 `.e2e.js`，**未执行**；只通过 AST 提取其 `testPreviewTransformControlsAreBoundedAndTypeScoped` 纯断言函数隔离运行，不执行任何 E2E 前置/浏览器入口，不宣称整文件 PASS。

## 未执行与剩余运行态限制

- 未执行 E2E、真实像素/滚动范围验收、服务启动/停止/重启、数据库写入、远程操作、发布、Git stage/commit/push、全量类型检查或大构建。
- 单测的 Vue renderer 和 DOM/请求适配器不是真实页面；几何数值、模板与水印接线验证不等于浏览器截图/像素验收。
- Office 原生分页/搜索、真实格式渲染、图片多帧/浏览器解码、视频/音频编解码和浏览器原生播放控件均需后续明确授权的真实运行态验收；没有按扩展名宣称所有文件已可预览。
- 累计阅读时长与审计持久化由独立线程处理，未由本任务认领。
- 用户明确禁止 Git 提交/推送，仓库要求提交推送后才可 completed，因此实现/定向验证已交付但任务收尾状态必须 blocked。

## 收尾证据

- 已先设置 `ready_for_closeout` 并运行仓库指定的 `task_closeout.py --task-id 20261015-dcc-full-format-viewer-audit --mode preview`：exit 0，四个文件全部 keep，delete 为空，warnings none。
- 依 `docs/task-closeout-rules.md` 的当轮禁止提交/推送规则，将状态改为 `blocked` 后实际运行 apply：exit 3，明确 `BLOCKED task status is blocked; expected ready_for_closeout or completed`。未删除文件，不记 apply PASS，不标 completed。
- `task.md`、`execution-log.md`、`verification-report.md` 已通过 UTF-8 严格解码和任务结构检查；源码最终指纹保存在 `source-fingerprints.json`，OnlyOffice 与基线相同。
