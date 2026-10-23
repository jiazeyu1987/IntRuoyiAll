# Execution Log

## 2026-10-15 第三轮来源fixture正式关闭回执
- 来源为Root正式转交的实测结果：亲跑最终旧G09 suite实际exit1，8项因预载SDK未注入来源无法建立editor配置；只加window.__dccOnlyOfficeScriptBaseUrl: props.baseUrl一行，保留原全部断言，同9项当前实际exit0、9/9 PASS、0skip，未改生产。
- 据此关闭第三轮共享fixture待办；本线程未重跑、不改shared harness、生产、自有tests或指纹。冻结manifest中的1/8是适配前历史快照，不覆盖最新Root回执。
- Root正在合并viewer定向组，结果由Root归档。本线程只同步四份Markdown并作UTF-8/结构检查；不将Office9项或历史125/181/类型/构建混写为当前合并组通过。

## 2026-10-15 第四轮指定边界只读复查
- 当前OnlyOffice已覆盖来源身份、初始化/销毁交错、同URL重挂载、异步destroy及几何失效回归；本轮不重跑这些无变化套件。
- 只读源码发现新候选：`loadOnlyOfficeScript` resolve 后，`mountEditor` 发现 `window.DocsAPI?.DocEditor` 缺失时只设置 `errorMessage` 并 return，不emit父 typed error，也不清理已解析的全局 promise/base marker；同URL重挂载会重复使用已失败的 resolved promise，无法重新尝试脚本。
- 已在 task.md 写 BDD，下一步只在自有 failure-boundaries suite 加实际 RED；不改 view/index.vue、共享宿主或其他owner文件。
- 实际RED：新增onload无DocsAPI用例1/1 FAIL，父error事件0而应1。初次局部实现通过了父错误路径，但错误字面断言及retry helper仍按首次“无SDK”选项运行，修正明确第二次提供真实SDK；这些fixture调整不计产品RED。
- 过程曾跑受影响24项并通过（局部清理写在mount内），进一步针对本次指定边界新增2项实际RED：不可构造接口、卸载后onload无接口都残留resolved全局promise。随后将就绪验证与失败释放统一下沉loader，避免source/unmount守卫跳过共享依赖清理。
- 正式实现：typeof检查及Reflect.construct仅验证SDK构造能力、不执行SDK构造器；loader返回校验后的constructor给mount，缺失/非构造器正式抛错，沿既有current source catch发typed error。只有pending和base同时仍属本次请求时释放promise/base/失败API；不接受旧SDK或源文件fallback。
- 最终仅跑本次新增4项（3新失败类型+同URL pending重挂载/重试）：4/4 PASS、0skip。随后仅选择实际SFC script/template/style编译1项：1/1 PASS。组件lint exit0。未重复上一轮200/200、types/build、C01-C06完整套件。
- 同URL重挂载只共享同次加载；失败时旧generation不发错误，新generation唯一error，后续同URL真实SDK就绪才发opened。源码未改其他owner，生产/test已冻结收口。
- 第三轮manifest被过程脚本误写入第四轮中间值及重复test项，发现后已按第三轮原字段/原源码测试指纹恢复，第四轮独立建立office-boundary-round4-fingerprints.json，不覆盖历史C03。
- 最终OnlyOffice SHA256 46565c20cadd0edabcceff5ff8894e0a0de9e8dba644e738915024ac70769885；新增4项所在测试79bd3101bd194686456fbe0c3f0dd3d37e34054500a36f5c6f35ce7909c95165。限定本任务git diff --check exit0；ready_for_closeout清理preview exit0、keep8/delete0/warnings none；按禁止Git改blocked后apply实际exit3、无删除，不记PASS。

## 2026-10-15 第三轮最终交还确认
- Root已阅读并接收当前Office cfbf08d…交接及来源fixture待办，明确由其亲跑旧G09原9项、按真实结果只补同服务__dccOnlyOfficeScriptBaseUrl: props.baseUrl一行后复跑，保留原全部断言；后续合并/综合验证由Root执行。
- 本线程保持生产、两个自有tests及第三轮指纹冻结，不改shared harness、不再运行任何套件。本次只同步四份Markdown并验证文档UTF-8/结构。
- 当前22/22及专属9/9为本线程本轮真实结果，旧G091/8为来源fixture适配前观察；Root新回执尚未提供，不代写PASS。125/181/完整类型与此前2635输入构建仍为Root前检查点。

## 2026-10-15 第三轮续修启动
- 最新续修仍沿用本目录和文件归属；读取根规则、前端/closeout/编码规则及 Root 六线程任务的需求相容条款。实际分支只读核对 int_qms；Office 当前 SHA256 e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1，与上一轮冻结一致。
- 125/181/完整类型及 Office 9/9 为上一轮有效历史证据，不重跑无变化套件、不把历史结果记为本次执行。
- Root 本次构建输入已登记 2635，随后要求生产前端暂时只读；本线程此前只读取，未开始生产修改。先在自有独立测试验证两个候选错误路径，等待明确解除冻结才可生产修复。
- 已先写 BDD：旧 editor 销毁异常的 UI/Promise/实例边界，以及已加载/在途脚本不同服务地址的身份隔离；后续实际 RED 另记，不因“继续”制造格式工具或假运行验收。
- 实际 RED：`node --test tests/unit/dcc-full-format-office-failure-boundaries.test.cjs` exit 1，4 项中 1 PASS / 3 FAIL：SDK_CLOSE_FAILED 未被可见错误路径捕获；已加载服务 A 被直接用于服务 B（创建了第二个 editor）；在途 A Promise 被复用为 B（创建了错误来源 editor）。原脚本失败后正式重试边界原本 PASS。
- Root 随后明确解除生产构建冻结：其完整 Vite 构建实际 exit0、2635 输入 0drift、3239 产物。该结果来自 Root，覆盖本次生产修复前检查点；本线程没有执行构建，改后是否需再 types/build 由 Root 决定。
- 首次 GREEN 修复后正式通知父 error 事件，两个旧空事件/首事件为opened的测试期望不再符合事先新增 BDD；精确改为断言当前 URL 的唯一 error、真实旧脚本失败保留、后续合法 retry 唯一 opened，保留错误/成功身份检查，不删除边界。
- 中间版受影响 Office 组合27/27 PASS（新增4+原7+G09 9+静态7），随后同服务合法复用用例专属5/5 PASS。该中间版仍接受无来源预载SDK，不能代表最终严格来源版本的共享宿主通过。
- 追加实际 RED：`--test-name-pattern='unidentified legacy'` exit1，旧预载 SDK 无来源仍创建了 editor；仓库 rg 确认 src 内 DocsAPI 加载方只有本 Office 组件。正式拒绝缺 marker 的现有SDK/Promise，自有测试仅为合法注入提供其真实fixture base，不伪造生产marker。
- 最终来源守卫后当前自有/相邻20/20 PASS；旧 G09 suite实际1PASS/8FAIL，原因是其SDK注入host无来源。未修改该共享宿主，已给Root一行来源前置适配及保留全部断言要求。
- Root再指出异步SDK onError中的销毁异常；新增2项实际RED均由SDK_CLOSE_FAILED冒泡失败。最小修复使回调保留原服务错误及销毁错误，失败generation先失效，销毁后复核当前源才更新UI并emit唯一error，不用空catch。
- 当前最终受影响组合 `node --test tests/unit/dcc-full-format-office-failure-boundaries.test.cjs tests/unit/dcc-full-format-office-review.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs` exit0，22/22 PASS、0skip；Office lint exit0，SFC script/template/style编译通过。
- 补“销毁中卸载”精确边界后只重跑自有边界suite：9/9 PASS、0skip；生产代码未再改。22与9重叠，不叠加成31个独立用例。
- 当前生产OnlyOffice SHA256 `cfbf08d4d3a9e7a5b13209b3b590bc700bdc307bbf6ce96b516f248f44675e0d`；旧e8fd指纹是第三轮改前，历史manifest不覆盖。当前manifest为office-boundary-round3-fingerprints.json。
- 格式逐类相容核对：PDF/Office/IMAGE/TEXT/VIDEO/AUDIO/DOWNLOAD_ONLY的适用工具、水印、时间及不可实现边界见最新integration note。没有新增OCR/转写/伪音频分页，未使用旧上传.txt日期规则；没有改view/index、阅读harness、其它模块或依赖。
- 最终限定本任务文件 git diff --check exit0；cleanup preview在真实blocked状态下exit3，提示 `BLOCKED task status is blocked; expected ready_for_closeout or completed`，不称PASS，未运行apply/删除。新SDK来源fixture待Root加一行及Git收尾未授权，不能将任务completed。

## 2026-10-15 Root 回传关闭共享合同待办
- 来源为用户转交的 Root 实测结果：先亲跑旧 9 项取得 8 PASS / 1 旧几何 FAIL，再仅适配测试，导出 viewportStyle，将原生逻辑视窗断言改为 1000×720，并新增 90°/110% 占位 792×1100；保留原权限/URL/generation 断言，同 9 项实际 9/9 PASS、0 skip，未改生产。
- 据此关闭旧 Office 共享合同待办；本线程未重跑，不修改冻结的 Office 生产、自有两项测试或指纹文件。本次仅同步四份 Markdown 并验证文档 UTF-8/结构，后续综合前端回归继续由 Root 执行。
- 先前 8/9 与冻结 JSON regression 字段保留为交接前真实历史；不将 Root 的新结果冒称本线程亲跑。任务仍因禁止 Git/整体收尾门禁为 blocked，不标 completed。

## 2026-10-15 最终交还
- Root 明确接手旧 G09 Office 两条几何断言的 fixture 适配，先保留 RED，再按固定 iframe + 滚动占位调整；其最终综合前端验证由 Root 执行。
- 按指令冻结 OnlyOffice 生产与自有两项测试，不再运行套件，不改 shared harness，不重写任何源码/测试指纹。
- 本次仅更新四份 Markdown 文档的交还状态并做 UTF-8/结构检查；旧 Office 8/9 是交接前真实历史，不改写为 Root 尚未交付的 PASS。任务受 Git/整体验证门禁保持 blocked，本线程无剩余实现动作。

## 2026-10-15 第二轮启动（第一轮记录见下文）
- 沿用本目录；用户收窄写入范围至 OnlyOffice 组件和自有格式测试。view/index.vue 及 shared harness 归阅读线程独占，本轮只读观察并给 integration note。
- 重新读取根规则、frontend-development、closeout、PowerShell 编码规则；入口只读核对分支仍为 int_qms、HEAD f06833f06。后续不执行 Git 命令，不运行服务/实库/E2E/Maven/fulltypes/build/remote/subagents。
- 发现候选缺陷：SDK 加载异常 catch 缺精确 source 检查；base/title/unavailable 变化的 watch 前窗口未覆盖；构造同步 onError 时实例可能在失效后被赋为当前；尺寸 observer 旧回调不检查 stage 身份或卸载。
- 已先在 task.md 写 Given/When/Then，随后建立独立 Office SFC 测试，不修改任何 shared harness。
- RED：`node --test tests/unit/dcc-full-format-office-review.test.cjs` exit 1，7 项中 1 PASS / 6 FAIL；实际缺 viewport、旧脚本错误污染新 source、base/title/unavailable 前置失效不足、构造同步错误后实例未销毁、旧 observer 覆盖新宽、模板缺滚动祖先。
- GREEN：仅修改 OnlyOffice 组件，实现固定 native W×720 逻辑视窗 + 按缩放/旋转外接矩形占位 + 内层双向滚动；统一 source 守卫，构造返回检查与旧 observer 失效，7/7 PASS。
- 百分比精度追加 RED：110% × 720 得 793px、应为 792px；先乘整数百分比再除 100 修复浮点上取整，独立几何测试覆盖 320/321/1000 × 60/100/110/200% × 0/90/180/270° 共 48 组合 GREEN。
- 最终独立组合 `node --test tests/unit/dcc-full-format-office-review.test.cjs tests/unit/dcc-full-format-post-reading.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs` exit 0，19/19 PASS、0 skip；组件 lint exit 0，真实 script/template/scoped-style 编译 PASS。
- 原 G09 Office suite 真实回归 exit 1，8 PASS / 1 FAIL：唯一失败为旧反向缩放宽高期望；未改其共享 harness。Root 所需两行尺寸同步和新 viewport 结果已写 integration-notes.md，不称全集 GREEN。
- 阅读接线只读核对：本地成功 mark 已连接 startPreviewReading，失败/清理已连接 closePreviewReading；工具/水印/类型规则保留，未改 view 或模型。只读被测 viewer UTF-8/LF SHA256 `42253a6c8ead376e2135fb259d8368ae34cc77091c41e9a31c62efff4b29e8df`。
- 当前 OnlyOffice UTF-8/LF SHA256 `e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1`；基线与两项新测试指纹在 office-review-fingerprints.json。第一轮 source-fingerprints.json 保留为历史，不覆盖它的旧 viewer 观察。
- 本轮未运行服务、实库、E2E、外部 Office、Maven/fulltypes/build、remote/subagents 或 Git 写入；入口只读核对后未再执行 Git。
- 本轮不执行依赖 git rev-parse 的 cleanup 脚本；目录六个正式证据文件全部需保留，无临时资产，不把手工清单核对称为 cleanup PASS。旧共享几何期望及禁止 Git 收尾使任务保持 blocked，第一轮 cleanup 结果仅为历史。

## 2026-10-15
- 实际分支：`int_qms`；HEAD 为 `f06833f06c868dc3a451eb63d01381a467ecfe92`，工作区存在并行脏改动，未触碰无关文件。
- 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、DCC 预览设计及既有 `20261014-dcc-gap-viewer-tools` 证据。
- Integration note：浏览登记时间由父 viewer/服务端 metadata 链路提供；本任务只保留和展示现有字段，不改阅读时长或审计模型。
- HEAD 澄清：`a3b8234ec` 是当前 `f06833f06` 的直接父提交，后者为文档提交；两者间 viewer 无差异，不切换或回滚分支。

## RED / GREEN / REGRESSION
- 初始 RED：`node --test tests/unit/dcc-full-format-viewer-contract.test.cjs` -> exit 1，3 项中 2 FAIL：文本工具区/分页/搜索缺失、视频无变换面板；已有受控状态断言 PASS。
- 第一阶段 GREEN：文本固定 80 行分页、页码校验、正文搜索和 CSS 变换，视频增加 CSS 变换；原三项合同 3/3 PASS。
- 新增真实 SFC 行为 RED：同测试扩至 7 项，5 PASS / 2 FAIL：跨分页边界短语 0 命中（应 1）、图片/文本/视频缺少实测宽高占位。
- GREEN：搜索按整段正文字符偏移建立索引，分页只影响展示；命中使用 Vue 文本插值和 mark，上一/下一处对应不同字符。ResizeObserver 量测 DOM 尺寸，旋转交换占位宽高并居中；水印随正文变换。7/7 PASS。
- 追加模板/隔离行为：实际 Vue renderer 验证文本不注入 HTML、同页命中高亮、变换后水印以及三类媒体真实模板元素；切版、旧 observer 回调、卸载与 object URL 回收验证 PASS。该 renderer 是隔离单测，不是浏览器或 E2E。
- 相邻合同 RED：旧 common-preview 和 protection 变换断言写死 PDF/IMAGE，两者按新增 VIDEO/TEXT 范围更新并保留所有原有保护断言；protection 只提取纯断言函数执行，不启动文件内 E2E 入口。
- 一次脚本补丁因上下文不匹配拒绝写入，随后按精确锚点分块写入成功；未覆盖无关文件。
- 首次组合回归 21 PASS / 3 FAIL：两个真实 PDF.js 用例因未设置既有 G09_CANVAS_MODULE 前置失败；另一个旧测试按 watcher 数组下标手动清理导致失败。已配置真实现有 canvas 模块，PDF 原字节/真实文本提取成功；测试改为实际修改 PDF 查询并验证同步 watch，不依赖 watcher 顺序。
- 最终 viewer/PDF/Office/登记时间组合：45/45 PASS，0 skip；追加 canonical source/context/metadata/binary wrapper 回归 16/16 PASS，0 skip；两项纯静态预览接线检查 PASS。共 61 项定向单测与两项静态检查。
- 新增旧 observer/查询变更断言后，本任务 10/10 再通过；生产代码未再改变。
- 定向 ESLint：viewer + OnlyOffice exit 0，无输出；实际 SFC script/template/scoped-style 编译 PASS；限定任务文件 git diff --check exit 0。
- PDF.js 在 Node 提示使用 legacy build，仅为已有 vendor 提示；未改 vendor、依赖或 lockfile。

## Integration Note
- 保留 `previewRegisteredAt` 服务器登记时间显示和成功画面门禁，不改累计阅读时长、后端模型、审计表或 `detail/index.vue`。
- 当前 `opened` 事件仍只从 `handleOfficeOpened` 发出；PDF/TEXT/IMAGE/VIDEO/AUDIO 成功显示使用本地 `markRegisteredPreviewVisible`。阅读时长线程若需要统一成功/停止事件，必须自行确定合同；不能将 metadata 或登记时间自动当作累计阅读时长。
- 不把 DOWNLOAD_ONLY、权限拒绝、预览不可用或原件下载作为成功正文；所有工具只操作已取回 preview 内容或 CSS/DOM 状态。

## 收尾
- 实现及验证后先设为 ready_for_closeout；实际 cleanup preview exit 0，keep=4/delete=0/warnings=none，未碰其他任务资产。
- 按当轮禁止提交/推送规则改 blocked；实际 cleanup apply exit 3，提示 `BLOCKED task status is blocked; expected ready_for_closeout or completed`，零删除，不记 PASS，不标 completed。
- 最终三份任务文档 UTF-8/结构检查 PASS，6 个源码/测试文件 UTF-8/LF 指纹保存并复核；OnlyOffice 基线和当前相同。最终清单只有本任务新增/修改资产；没有 stage/commit/push。
