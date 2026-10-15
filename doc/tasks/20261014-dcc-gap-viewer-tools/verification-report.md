# G09 验证报告（最新 A20 + C04/第一阶段归档）

## 最新 A20 Office 服务错误事件（2026-10-15）

`node --test tests/unit/dcc-g09-office-readonly.test.cjs` 初始实际RED：8项/6通过/2失败；after-opened缺父事件、props URL已变而watch未执行的旧错误写入当前errorMessage。

实现仅子组件emit声明、DocsAPI onError增加精确URL核对、同当前generation输出error({documentUrl,message})，保留真实message及原destroy/失效语义。旧generation/旧URL/卸载均不发；重复同失败回执不重发。初始化配置/脚本catch没有新增error事件；事件仅由真实service onError产生，没有业务请求。

最终 `node --test tests/unit/dcc-g09-office-readonly.test.cjs scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs`：exit0，**16/16 PASS**（Office9+相邻7），0skip。当前SFC脚本构造实际DocEditor配置触发真实onError回调，包含opened后错误、切URL前watch窗口、切URL后回执、同URL新generation、卸载和重复回执；只读/旋转/水印/SFC编译回归保留。

组合用例只读提取当前view父页正式handleOfficeError，实际子error驱动该handler，断言registered=false、父错误等于子实际message；切版旧callback不清新时间。未修改父页，测试未把父handlermock替代正式实现。该隔离组合不是真实服务UI/E2E。

ESLint Node API 限定OnlyOffice组件：errors0/warnings0；`git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue` exit0。未重跑冻结后端24项，未跑完整ts:check或大构建。A20初始/最终源码指纹分别baseline-a20/final-a20-sha256.json，旧C04同两文件hash仅作历史基线。

A20 cleanup preview在ready_for_closeout时exit0，keep16/delete139（均本任务既有私有验证产物）；Git/集成门禁未结束改blocked，apply实际BLOCKED exit1且未删除，不记PASS，不标completed。组件及测试请求Root接收，等待后续授权。

## 最新 C04 状态（2026-10-15）

本轮新增登记时间/Office 旋转源码与测试：后端当前源私有编译 + H2/事务/HMAC/metadata **24/24 PASS**；Office 实际 SFC 与相邻合同 **12/12 PASS**。RED/GREEN、私有 classpath 边界和限制详见 c04-verification-report.md。Root 已转交 G07 Query/viewer 和 G01 metadata decoder；本线程 5 个生产文件与专属测试冻结待 review。共享接线、媒体分页、累计阅读时长、真实 Office UI 均未称完成。

Root不可用修正后，仅追加受影响Online5/5和hunk/正式decoder3/3（含无event/无time的原错误优先、离线未记录），未重复完整大范围验证。

下文是首轮已接收证据，19 项不能代替本轮 24+12。

## 结论与边界

明确的 PDF 分页/搜索及 Office 只读复核已实现并可 review；不是 G09 全项关闭。浏览时间含义、音视频工具范围尚待用户答复。未做 E2E、运行服务验收、Git 提交推送、数据库写入或后端修改；未运行无 Root 票据的 Maven、全量类型检查、大构建。

## 真实 RED

1. `node --test tests/unit/dcc-g09-viewer-tools.test.cjs` -> exit 1，初始 6 项：1 PASS / 5 FAIL。失败为实际查看器缺少搜索输入、页码输入、无文本状态、当前页状态和正式控件；权限拒绝用例独立通过。
2. `node --test tests/unit/dcc-g09-pdf-text.test.cjs` -> exit 1，3 FAIL，专属 `pdf-tools.ts` 尚不存在。测试使用本地真实 vendor PDF.js 与生成的有效 PDF 字节；未拿 mock 冒称真实提取。
3. `node --test tests/unit/dcc-g09-office-readonly.test.cjs` -> exit 1，3 PASS / 1 FAIL：切版后缩放仍为 110%，预期 100%。实现新正文加载重置后 4/4 PASS。
4. `node --test --test-name-pattern='unsafe rendering-time cleanup' tests/unit/dcc-g09-viewer-tools.test.cjs` -> exit 1：渲染中切版先 cleanup 抛错，旧文档未销毁。改为直接 destroy 后 1/1 PASS。
5. `node --test --test-name-pattern='counts occurrences' tests/unit/dcc-g09-viewer-tools.test.cjs` -> exit 1：真实 Vue 异步 watch 在 nextTick 清掉刚提交的 3 个命中。输入清理改为同步 watch 后通过。

## GREEN / REGRESSION

工作目录：`IntRuoyiFronted`，日期 2026-10-14。

```powershell
$env:G09_CANVAS_MODULE='C:/Users/D01020/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/@napi-rs/canvas'
node --test tests/unit/dcc-g09-viewer-tools.test.cjs tests/unit/dcc-g09-pdf-text.test.cjs tests/unit/dcc-g09-office-readonly.test.cjs
```

exit 0，19 PASS / 0 FAIL。`G09_CANVAS_MODULE` 为本机已安装的真实 canvas 库，其他机器需设置自己的现有模块路径；缺失会显式失败。未新增依赖或改 lockfile。PDF.js 在 Node 提示建议 legacy build，但真实当前 vendor 提取成功，未切换生产 vendor 或注入伪 DOMMatrix。

覆盖证据：

- 真实 PDF.js 对 3 页真实文本 PDF 提取、大小写无关字面搜索、重复命中 `[1,2,2]`、空白归一、非正则文本及原 bytes 不变。
- 真实 PDF.js 对仅含嵌入 raster 图像的 2 页 PDF 提取结果为空；不把外加水印当正文命中。
- 执行实际 SFC script：命中导航、上一/下一处、页码跳转/非法页码、滚动页码同步、旋转/缩放后 viewport 尺寸、worker 字节副本及原件不变。
- 执行实际 SFC template 的 Vue renderer：初始 3 页各有一个水印 overlay；搜索、跳页、缩放和旋转后仍为 3 个且全部携带原 traceCode；打印快捷键保持拦截。
- 切版旧文本迟到不能覆盖新版本唯一文本；卸载后迟到文本/渲染不恢复页面或索引；文本提取错误明确显示而非伪装扫描件；缺画布不继续成功。
- 实际 Office 配置传入 view，edit/comment/review/download/print/copy 全 false；无保存回调；精确 document URL；旧 onDocumentReady、卸载回执被拒绝；错误显性；切版重置缩放和按键状态。
- PDF 与 Office SFC script/template 编译通过。Canvas 业务流程使用隔离适配器，未宣称完成真实浏览器像素画面验收。

相邻验证：

- `node --test scripts/dcc-onlyoffice-readonly-config.test.mjs scripts/dcc-onlyoffice-zoom-controls.test.mjs` -> exit 0，7 PASS。
- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> exit 0；该脚本仅静态读源码，没有 Playwright/网络行为，不是 E2E。
- `node --test tests/unit/dcc-viewer-trace-entry.test.cjs` -> 最终 exit 0，3 PASS。先前并行窗口曾 1 FAIL（导航结果为 `[object Object]`），当时未修改受保护详情父页或该测试；后续当前源码重跑通过，不归为本 G09 修复成果。
- TypeScript Node API，`createProgram([pdf-tools.ts], {noEmit:true,strict:true,target:ES2022,module:ESNext,skipLibCheck:true})` -> 0 diagnostics。
- 从当前 SFC 抽取实际 ViewerCanvas 接口，针对现有 vendor declaration 与 PdfTextPage 的局部 TypeScript 边界检查 -> 0 diagnostics。探索用的更小代理接口不满足同等结构而报类型不重叠，已用正式实际接口核验；不是全量 vue-tsc 通过证据。
- ESLint Node API，限定 `view/index.vue`、`OnlyOfficeReadOnlyViewer.vue`、`pdf-tools.ts` -> 各 errors=0 / warnings=0。
- `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue` -> exit 0。

## 已知限制 / 待验证

- 正文搜索定位至命中页并展示正文片段及处数；不宣称具有 canvas 字形位置高亮或 OCR。
- 真实页面的布局、像素画面、Office 服务原生翻页/搜索工具、所有格式的浏览时间，需后续授权与明确口径；当前未验证。
- `presentation.ts` 原 SHA256 未变；未写 detail 父页、公共 API、后端、vendor。
- Git 收尾未获授权，工程不能标记 completed。Root 仍需独立 review 并调度后续统一验证。

## 收尾证据

清理 preview 在 ready_for_closeout 状态 exit 0，7 文件全部 keep、delete 空、warnings none。根据本轮禁止 Git 提交推送与尚未明确的剩余范围，将任务记录改为 blocked；apply 返回 BLOCKED（shell exit 1）且未删除文件。未把该受阻命令写成 PASS，未标 completed。
