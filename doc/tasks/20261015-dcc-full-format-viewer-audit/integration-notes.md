# 全格式浏览集成说明（2026-10-15）

## 第四轮最终交还：只接本轮新增结果

- 新缺陷：脚本onload成功但DocsAPI缺失/DocEditor不可构造，或加载期间组件卸载，旧代码会漏发当前父error或留下失败的resolved缓存。实际RED为1项FAIL后追加2项FAIL；已统一在loader校验/释放，不改历史C03来源或async destroy合同。
- 本轮最终新增4/4 PASS（命令pattern `script onload|same-URL pending`），定向SFC编译1/1 PASS，Office lint通过。旧200/200/types/build、G09和C01–C06仅保留历史，不重复；没有把过程24项当最终新增统计。
- 当前Office SHA256 `46565c20cadd0edabcceff5ff8894e0a0de9e8dba644e738915024ac70769885`；新测试及baseline完整指纹见office-boundary-round4-fingerprints.json。OnlyOffice和自有test现在冻结，view/index.vue与shared harness未写。
- 正式loader返回经构造能力核验的SDK构造器；失败缓存仅由相同pending+base的尝试释放，并清该失败API。当前source经既有catch发typed error，旧generation/卸载不发；同URL在途重挂载后正式重试才opened。
- 跨viewer只读note：零clientWidth既可能是暂未布局也可能是父容器隐藏，当前OnlyOffice observer只更新几何且onDocumentReady不以元素可见性另发pause。若产品要求按元素隐藏暂停/恢复阅读，需要阅读owner明确恢复/错误清理合同；不能在本线程把0宽一概变成终止错误或修改viewer。未做真实DOM像素/外部Office验收，不声称此跨边界已完成。
- 第三轮manifest曾被过程脚本误写入第四轮中间值及重复test条目，已恢复第三轮归档字段/原指纹，并改为独立第四轮manifest；未用中间值覆盖当前正式证据。后续Root集成只使用本节最新输入。

## 第三轮最终交还确认

**第三轮来源fixture待办已关闭，来源：Root正式回执。** Root亲跑最终旧G09 suite取得exit1（8项因无来源无法建立editor配置）后，仅补 `window.__dccOnlyOfficeScriptBaseUrl: props.baseUrl` 一行，保留原全部断言；同9项实际 **exit0、9/9 PASS、0skip**，未改生产。本线程没有重跑，也没有改shared harness。

最终Office源码仍为 `cfbf08d4d3a9e7a5b13209b3b590bc700bdc307bbf6ce96b516f248f44675e0d`。生产、自有tests和指纹继续冻结；原1 PASS/8 FAIL及冻结JSON内相关字段保留为Root适配前历史，不再是当前阻塞。Root当前合并viewer组的结果由其单独记录，不能用这9项替代合并验证。

## 第三轮新增边界与既有父事件合同（2026-10-15）

- 本轮最新续修已取得独立实际 RED：SDK 销毁旧实例失败越过 mountEditor catch；服务 A 的已加载/在途脚本被当作服务 B 使用。Root 构建冻结期间只写 BDD/专属 RED，未动生产；收到 Root 构建成功及解除冻结后才继续独占 Office 生产修复。
- 已复用现有 typed error({documentUrl,message})：当前源初始化/旧实例销毁失败通知现有父 handleOfficeError，使其按当前 URL/kind 关闭阅读；不改事件类型、不改父 view、不新增业务写路径。切源迟到失败仍由 source/generation 守卫排除，不给新源发旧事件。
- 实际加载请求记录规范 base URL；已知服务 A 的已加载 SDK/在途 Promise 不可用于 B，服务冲突显式报错。追加实际 RED 又证明无来源旧 DocsAPI 仍可冒充当前请求，因此当前正式合同也拒绝没有 `__dccOnlyOfficeScriptBaseUrl` 的既有 SDK/Promise，不按请求地址补猜来源，不新增 fallback/外部服务/依赖。
- 真正脚本失败时只清理与该次 pending 相同的缓存，保留原始失败并通过当前源错误事件通知父页；随后显式正式加载可以重试。合法同服务规范地址（含尾斜线等价）复用仍通过。
- 新增 error 触发点属于既有子父合同的错误覆盖，Root 可按需要复核阅读关闭；本线程不改 view/index.vue、阅读 API/lifecycle 或其 harness。
- 异步 SDK onError 的销毁异常已先以2项RED复现再修复：不依赖仅包初始化的catch；回调先失效旧generation，再尝试销毁，失败详情合并到正式message，最后复核source/卸载/失败generation只发唯一error。销毁期间切源或卸载不向新画面写旧错误。

### 第三轮当前证据与源码冻结

- 正式受影响组合22/22 PASS；末次追加销毁中卸载后只重跑专属suite9/9 PASS，二者重叠不累计。Office lint与SFC script/template/style编译通过。
- 旧G09来源前置缺失的1 PASS/8 FAIL是RED历史；Root已完成下节的一行fixture适配并回传同9项exit0、9/9 PASS、0skip，待办关闭。中间版27/27、上一轮Root9/9仍只保留历史，不与本轮结果混加。
- 当前Office UTF-8/LF SHA256：`cfbf08d4d3a9e7a5b13209b3b590bc700bdc307bbf6ce96b516f248f44675e0d`。生产、自有测试现在冻结交还，当前完整指纹见office-boundary-round3-fingerprints.json；旧e8fd为改前检查点。
- Root的125后端/181前端/完整类型，以及本次改前构建exit0/2635输入0drift/3239产物均不在本线程重跑，不当作当前新执行或改后完整构建结果。后续需要的集成验证由Root决定。

### 第三轮七格式逐项对照与相容结论

依据本会话原文“所有格式文件按规则提供放大、缩小、旋转、翻页，并按对应规则提供正文搜索、浏览水印和浏览时间；只改变画面，不修改源文件”，以及Root本次相容条款；不从旧上传.txt继承日期规则，不猜缺失第2/6/8项，不加入OCR/转写/外部服务部署。

| 格式 | 当前源码依据与可实现工具 | 搜索/时间/水印及真实边界 |
|---|---|---|
| PDF | viewer的goToPdfPage/handlePdfPageJump、50–250% view zoom、90° rotation，getPdfPageViewportStyle占位 | pdf-tools从真实PDF文字提取搜索；扫描PDF无文本时明确不可搜索；逐页Canvas/overlay水印，成功渲染后接阅读。未发现本次新的工具缺口，不重跑未变PDF套件 |
| OFFICE | 本线程OnlyOffice固定logical W×720 + viewportStyle双向滚动、60–200% zoom/90° rotation；word/cell/slide分类，mode=view、禁edit/comment/review/download/print/copy，无保存callback | 未关闭原生toolbar/leftMenu，分页/工作表/幻灯片导航与正文查找交原生只读工具；水印及精确opened/error保留。本次新增错误路径已修，不以隔离Node证明DocumentServer工具可见性或像素 |
| IMAGE | viewer单img、50–250% zoom/rotation、flatPreview实测宽高滚动占位 | 无OCR则不伪造正文搜索/图像多页；水印与匹配DOM load成功门禁保留。多帧/特殊编码依浏览器，不以PDF或下载回退 |
| TEXT | 每80行视图分页/跳页、全正文跨页字面搜索/逐字符mark、zoom/rotation；Vue插值不执行HTML | 页面/角标水印与正文成功阅读接线保留；80行不假称源文档真实页数，编码按既有解码合同。未发现新增工具缺口；若需扩编码合同应另由viewer owner评审 |
| VIDEO | 原生播放/暂停/时间轴，zoom/rotation及实测滚动占位 | 时间轴不伪装文档翻页；不加入转写/字幕搜索；保留水印、nofullscreen/disablepictureinpicture/disableremoteplayback与loadeddata/error门禁。解码失败显式报错，不fallback |
| AUDIO | 原生播放/暂停/时间轴 | 无可旋转图像或文档页码，不造缩放/翻页/语音搜索；保留页面水印与匹配loadeddata后的阅读接线。没有把页面水印写入音轨 |
| DOWNLOAD_ONLY | loadPreview在取binary前返回，显示不支持在线预览 | 不发正文成功、不新增下载动作，不用源件或下载替代预览；CAD仅当服务端正式选定该版本PDF才走PDF，不从前端猜替代件 |

除本次Office错误边界外，没有把外部服务/浏览器解码或自然不适用工具当作新的可实现源码缺陷。viewer/阅读source/timer仍由其owner维护，本线程只读观察，不改它的工具、权限、token、计时模型或测试宿主。

### 第三轮旧 Office fixture 一行来源前置（Root已完成）

文件 `IntRuoyiFronted/tests/unit/dcc-g09-office-readonly.test.cjs` 的 `office(onEvent)` 宿主，`host.window` 里直接注入 `DocsAPI` 构造器，却没有记录它来自哪个 Document Server。按新的 fail-fast 来源合同，这不是“已加载且来源匹配”的合法前置。

Root已在该 `window` 对象增加唯一来源字段并亲跑9/9 PASS：

```javascript
__dccOnlyOfficeScriptBaseUrl: props.baseUrl,
```

该 fixture 的 props.baseUrl 为其自有 `https://office.example.test`。此变更只补模拟 SDK 的真实预期身份，**不应删改**原权限、document URL、generation、旧回执、水印、固定视窗或 viewport 几何断言。生产代码不能为了未记录来源的测试宿主重新接受未知旧 SDK。

本线程仅适配自己 `dcc-full-format-office-review.test.cjs` 和新边界suite，没有修改上述共享测试或阅读owner harness。最终来源守卫版本的共享9/9来自Root本次正式回执；中间版与上一轮9/9仍保持历史，不冒充本次新执行。

## 第二轮历史交还状态（不覆盖第三轮 SDK 来源待办）

**共享合同待办已关闭。** Root 回传：亲跑原 9 项取得 8 PASS / 1 旧几何 FAIL 后，导出 viewportStyle、按固定 iframe + 滚动占位适配，保留权限/URL/generation 断言；同 9 项实际 **9/9 PASS、0 skip**，仅改测试、未改生产。该结果来源于 Root 回传，本线程没有重跑。

本线程生产、自有测试及指纹继续冻结。原 19/19 是本线程定向结果，旧 8/9 仅是适配前 RED 历史；后续综合前端验证由 Root 记录，不据单个 Office suite 宣称整体或 E2E 通过。

OnlyOffice 冻结 SHA256：`e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1`；三项 owned 源码/测试指纹原样保留在 `office-review-fingerprints.json`。

## 文件归属

本轮只修改 `IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue`，新增独立 `dcc-full-format-office-review.test.cjs` 与 `dcc-full-format-post-reading.test.cjs`。不修改阅读线程的 `view/index.vue`、任何 shared harness、阅读 API/lifecycle/后端模型或详情页。

`source-fingerprints.json` 是第一轮归档，不能作为当前阅读接线后 viewer 的最终指纹。本轮 owned 文件及只读观察位置由 `office-review-fingerprints.json` 独立记录；viewer 指纹仅表示该次读取的源码，阅读线程可继续变更。

## Office 几何与源码修复

- 旧公式将 iframe 宽/高除以 zoomScale 再变换，使逻辑文档视窗随 CSS 缩放变小；quarter-turn 同时交换逻辑视窗的长短边。不能由此证明原生工具在 200%/窄宽时仍可达。
- 新公式固定原生 iframe 的逻辑宽=`stageWidth`、高=`720`；zoom 不改两者，rotation 不交换原生宽高。外层虚拟 viewport 宽高等于变换后的外接矩形向上取整，scaler 在其中心变换；内层 overflow:auto，外层 stage 保持固定高度及稳定宽度量测，避免内部滚动条反复改变基准宽。
- `320/321/1000` 三种宽度 × `60/100/110/200%` × `0/90/180/270°` 共 48 个组合由实际 SFC computed style 驱动，独立角点几何断言所有边界位于非负可滚动占位内。原生文档/工具实际渲染和像素仍未验收。
- 切源保护现同时比较 generation、规范 base URL、精确 document URL、documentTitle 和 unavailableReason；await 成功/失败与 SDK opened/error 统一使用同一守卫。
- SDK 构造中同步 onError 使 generation 失效时，返回实例直接销毁，不赋为当前 editor。observer 的旧 element/卸载回调也不再更新 stageWidth。
- Office 仍 mode=view，edit/comment/review/download/print/copy 全 false，无保存/回调写文件路径；Word/cell/slide 分类、原生导航/搜索配置和水印保留。未增加 iframe DOM 穿透、API Command/保存或文档下载。

## 已由 Root 关闭的旧 Office 测试合同（本线程没有改 shared harness）

适配前本线程完整执行 `tests/unit/dcc-g09-office-readonly.test.cjs`：**8 PASS / 1 FAIL**，此处保留为 RED 历史。唯一失败是 `Office visible rotation and reset only change viewport geometry and preserve native document identity` 中两条旧反向缩放尺寸断言；Root 已按下表完成最小适配并回传同 9 项 **9/9 PASS、0 skip**。

在容器宽 `1000`、旋转 `90°`、缩放 `110%` 下，Root 已在该测试同步以下期望，并添加 viewportStyle 导出及占位断言：

| 字段 | 旧断言 | 当前正式结果 |
|---|---|---|
| `scalerStyle.width` | `720/1.1` px | `1000px`（原生逻辑宽） |
| `scalerStyle.height` | `1000/1.1` px | `720px`（原生逻辑高） |
| 新 `viewportStyle.width` | 旧组件没有 | `792px`（720×110÷100） |
| 新 `viewportStyle.height` | 旧组件没有 | `1100px`（1000×1.1） |

补充精度用例已先 RED 后修正：占位使用整数百分比优先运算，避免 `720 * 1.1` 浮点误差导致额外一像素。奇数宽度/60% 的不足 1px 必需取整仍保留，独立角点断言禁止负溢出。

本线程没有修改该测试或其 parent handler 宿主；Root 的适配与 9/9 结果仅关闭这项共享合同待办。正式新几何/异步用例的独立 suite 与冻结指纹原样保留；指纹清单内的 8/9 regression 字段是冻结时历史快照，最新结果以本文 Root 回传记录为准。

## 阅读接线后只读复核与 view 建议

- 当前 `markRegisteredPreviewVisible` 成功门禁后接 `startPreviewReading`；清理登记时间接 `closePreviewReading`。因此第一轮“非 Office 仅本地成功，尚无统一计时启动”的旧 note 已过时，应以当前阅读线程的正式合同为准。只读检查不验证任何实库计时持久化。
- PDF/文本搜索与分页、PDF 字节副本、PDF/图片/文本/视频视图变换、各格式水印及 Office 只读参数仍在当前模板；工具 handler 仅改变 view state，不调用阅读写接口或原件保存。
- 音视频仍以原生时间轴播放；音频不伪造旋转/分页/转写搜索，图片不伪造 OCR/多页翻页。视频保留 nofullscreen/disablepictureinpicture/disableremoteplayback 的页面水印边界；具体浏览器支持需真实运行态。
- 不可预览原因及 DOWNLOAD_ONLY 仍在二进制请求前短路，不构造下载或源文件回退，不当作成功正文。
- 对 `view/index.vue` 的可选整合建议：保持 `handleOfficeError` 的当前 URL/kind/卸载守卫与 `clearRegisteredPreview` 关闭阅读的接线；保留 Office key 与 renderVersion 绑定，以免同 URL 的不同挂载世代相互冒用。当前只读检查中这两点均保留，无须本线程改 view。

## 证据边界

本线程定向组合 19/19 PASS（新 Office 7、只读后置核对 5、相邻只读/缩放静态 7）；共享 Office 合同由 Root 适配后回传 9/9 PASS、0 skip，待办关闭。本线程没有重跑或改写冻结指纹。组件 ESLint 与 script/template/scoped-style 编译的已有 PASS 保留；上述不是外部 OnlyOffice 服务、浏览器像素或媒体 E2E。
