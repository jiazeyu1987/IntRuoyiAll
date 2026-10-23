# DCC 文件内容页全格式浏览能力核对与修复

## 目标
核对并修复受控文件内容页在 PDF、图片、Office、文本、音频、视频及不可预览格式下的只读浏览工具边界：按格式提供可实现的放大、缩小、旋转、翻页和正文搜索，保留服务端 preview token、权限、水印与浏览登记时间，不修改源文件。

## Current Status
blocked — 第四轮新增SDK就绪边界4/4、SFC编译1/1及lint已通过，生产/tests冻结。ready_for_closeout下清理preview已通过、8项全保留；Git收尾未授权，不标completed。C01–C06/上一轮200及types/build仍保持历史关闭，无本线程待实现/重跑项。

## 里程碑（第一轮历史）
1. 已完成：读取仓库规则和既有 G09/viewer-tools 证据，建立格式能力矩阵。
2. 已完成：为文本/视频缺口及跨页搜索/几何边界建立实际 RED。
3. 已完成：在授权 viewer 范围实现 GREEN，OnlyOffice 无需改动。
4. 已完成：61 项定向测试、两项纯静态接线检查、定向 lint/编译、diff 检查和运行态限制报告；收尾门禁待记录。

## Expected Verification
- PDF/图片/Office/文本/音频/视频/不可预览格式能力静态合同。
- viewer 定向行为测试、OnlyOffice 只读回归、PDF 文本工具回归。
- 前端定向 lint/编译（依赖可用时）和 `git diff --check`。
- 不执行 E2E、服务重启、数据库写入、Git 提交或推送。

## BDD
- BDD: 格式工具能力 -> Given 文件内容已通过受控 preview token 返回且权限有效，When 用户进入对应格式内容页，Then 只改变视图状态提供该格式允许的缩放、旋转、翻页或正文搜索，源文件和下载权限不变。
- BDD: 受控水印与登记时间 -> Given 服务端返回水印和已核验浏览登记时间，When 正文画面成功显示，Then 当前画面持续覆盖水印并显示登记时间；切换版本或加载失败不得复用旧值。
- BDD: OnlyOffice 只读 -> Given Office document URL，When 初始化、切换、旋转或缩放，Then editor 保持 view/不可编辑权限、文档 URL 不变，旧 generation 回执不得污染新页面。
- BDD: 不可预览格式 -> Given 服务端将格式标记为 DOWNLOAD_ONLY 或返回不可预览原因，When 打开内容页，Then 显式展示不可预览状态，不把下载或源文件回退伪装成正文预览。
- BDD: 文本精确匹配 -> Given 文本包含同页重复关键字、跨分页边界短语和 HTML 字面内容，When 搜索并切换匹配，Then 定位并高亮对应真实字符，跨页短语不因视图分页消失，HTML 不作为标记执行。
- BDD: 旋转可达性 -> Given 图片、文本或视频宽高不同，When 缩放并旋转 90/180/270 度，Then 滚动区域按变换后宽高占位，左侧与上侧内容可达，水印随正文覆盖；切版/卸载清除旧尺寸和搜索状态。

## 设计约束检查
- 仅修改受授权的 DCC viewer 页面、OnlyOffice 预览交互和相关 viewer tests；不修改 `detail/index.vue`、审计表、后端阅读时长模型或公共 token/权限 API。
- 只读 CSS/Canvas/DOM 视图变换，不写回源文件。
- 真实页面像素和 E2E 未运行，不以静态或单测冒称运行态通过。
- 基线澄清：请求中的 `a3b8234ec` 是当前 HEAD `f06833f06` 的直接父提交；后者是文档提交，viewer 在两者间无代码差异。只读核对后继续当前 `int_qms`，未回退 HEAD。
- 既有 `20261014-dcc-gap-viewer-tools` 保留为 G09 原始实现主记录；本目录是本次新增“全格式工具/文本/媒体/几何”跟进任务，不复制旧任务 PASS 冒充当前验收。当前重新运行的回归见本目录报告。

## Cleanup Keep
- doc/tasks/20261015-dcc-full-format-viewer-audit/source-fingerprints.json
- doc/tasks/20261015-dcc-full-format-viewer-audit/office-review-fingerprints.json
- doc/tasks/20261015-dcc-full-format-viewer-audit/integration-notes.md
- doc/tasks/20261015-dcc-full-format-viewer-audit/office-boundary-round3-fingerprints.json
- doc/tasks/20261015-dcc-full-format-viewer-audit/office-boundary-round4-fingerprints.json

## 第三轮续修与临时冻结（2026-10-15）
- 最新授权继续本线程和本目录，仅独占 OnlyOfficeReadOnlyViewer.vue 与自有 format helpers/tests；不改 view/index.vue 或阅读 owner harness，不新建线程/子 Agent。
- Root 唯一执行 Maven/shared target/fulltypes/build；禁止依赖变更、E2E、实库/迁移、服务启停、发布、Git 提交推送。本次 Root 构建期间生产前端只读，未收到解除冻结前不得生产修改。
- 需求相容依据：本会话“所有格式文件按规则提供放大、缩小、旋转、翻页，并按对应规则提供正文搜索、浏览水印和浏览时间；只改变画面，不修改源文件”，结合 Root 最新相容条款与七格式正式 taxonomy；不采用桌面旧上传.txt的预设日期规则，不猜测缺失第2/6/8条，不扩展 OCR/转写/部署。
- 本次仅为真实新增缺口跑定向 Node；原有 125 后端、181 前端与完整类型均不重复、不改成当前新证据。
- BDD: Office 销毁失败可见 -> Given 旧 Office 已打开且 SDK destroyEditor 抛出真实异常，When 切到新文档调用 mountEditor，Then 脱离旧实例引用、阻止新 editor 创建，当前页面显示真实失败并由既有 error({documentUrl,message}) 合同通知父页，不产生未处理 Promise；旧 opened 不可重发，卸载不重复销毁已脱离实例。
- BDD: Office 既有脚本服务身份 -> Given 本组件已从服务 A 加载或正在加载 SDK，When 后续文档指定服务 B，Then 不把 A 的全局 SDK/在途 Promise 当 B 使用，显式拒绝服务身份冲突，不下载源件/回退旧服务；同服务仍复用，真实脚本失败允许后续正式加载重新尝试。
- BDD: Office 缺失脚本来源 -> Given window 中已存在未记录来源的旧 DocsAPI 或在途脚本 Promise，When 当前文档请求初始化，Then 显式拒绝无法核验的服务来源，不把请求 baseUrl 猜作旧 SDK 来源、不发 opened；测试的已就绪 SDK 适配器也必须提供真实预期服务身份。
- BDD: Office 异步服务错误的销毁失败 -> Given 当前文档已 opened，When SDK 在 mountEditor 返回后异步 onError 且 destroyEditor 再抛错，Then callback 不冒泡未处理异常，页面与唯一父 error 事件同时保留服务失败和销毁失败，实例脱离且旧 opened/error 不重发；销毁期间切源或卸载时不把旧失败写给新源。

## 第四轮续修（2026-10-15）
- 只读复查目标：SDK script `onload` 成功但 `window.DocsAPI.DocEditor` 缺失/非构造器；既有错误/重挂载/缓存路径，不重复上一轮已验证的 source/async destroy/geometry 套件。
- BDD: Office SDK onload 缺失 -> Given script onload 成功而 DocsAPI/DocEditor 缺失，When 当前文档初始化，Then 当前父页收到带精确 documentUrl 的 typed error、不发 opened，失败的全局 promise/base marker 释放；同 URL 显式重新挂载可重新加载脚本并在真实 DocEditor 出现后成功，不能把旧失败缓存或源文件回退当作成功。
- BDD: Office 无效构造器与卸载加载失败 -> Given onload 暴露非构造器 DocEditor，或加载时组件已卸载而脚本未暴露接口，When loader校验完成，Then 失败的同一次脚本缓存/无效API被释放，不向卸载组件发事件；有效SDK重试保留原URL和只读配置，不清掉别的加载尝试。
- 本次新增实际RED共3项：onload缺失接口不发父error；不可构造接口保留失败缓存；卸载后失败缓存不释放。最小修复统一在loader验证构造能力、绑定pending/base身份释放失败API/缓存，返回已验证构造器，由mount当前源错误路径正式通知。
- 最终新增4/4 PASS（含同URL并发挂载后失败仅当前generation报错与合法重试）；仅选择SFC编译用例1/1 PASS、组件lint exit0。未重跑C03/source/async destroy/几何完整套件或200全集；初始中间实现的24项结果只属过程记录，不代表最终新增结果。
- 对几何失效只读复核：clientWidth为DOM非负整数，0可表示尚未布局或父容器隐藏；当前watch会更新为0而未分类为源文件失败。不能无条件把正常隐藏变成永久错误；如需按元素可见性暂停阅读，交viewer/阅读owner确认恢复合同，本轮不改该跨owner行为、不宣称像素/可见性验收。

### 第三轮结果与交还
- 当前真实 RED：首次 4 项 3 FAIL；无来源旧 SDK 追加 1 项 FAIL；异步 onError 清理追加 2 项 FAIL，均已取得对应 GREEN。
- 当前受影响组合 22/22 PASS，末次新增“销毁中卸载”后专属 suite 9/9 PASS，lint/SFC 编译通过；没有重复 Maven/fulltypes/build 或无变化的 PDF/阅读/详情全集。
- 旧G09宿主未声明来源的1 PASS/8 FAIL保留为适配前RED历史。Root回传：亲跑原失败后仅在host.window增加 `__dccOnlyOfficeScriptBaseUrl: props.baseUrl`，原全部断言不变，同9项实际exit0、9/9 PASS、0skip。来源fixture待办已关闭，来源为Root回执，本线程未重跑。
- 当前 Office SHA256 `cfbf08d4d3a9e7a5b13209b3b590bc700bdc307bbf6ce96b516f248f44675e0d`；第三轮基线、当前源码和两个 owned tests 指纹见 office-boundary-round3-fingerprints.json。view/index.vue 始终仅观察，归阅读 owner。
- Root已阅读最终cfbf08d交接并明确接手来源fixture：亲跑旧G09原9项，按真实结果仅补同服务marker一行后复跑，保留全部断言；本线程不修改shared harness、不再重复测试，只完善最终文档。Root适配后的新结果以其正式回执为准，不提前改写历史1/8为PASS。
- 后续正式回执已到：Root已完成上述一行fixture适配并复跑，同9项9/9 PASS、0skip。该共享待办关闭；当前无本线程待修复或待重跑项，合并viewer组仍由Root独占。

## 第二轮授权与验证（2026-10-15）
- 仅可修改 OnlyOfficeReadOnlyViewer.vue 与自有 format helpers/tests；view/index.vue、detail/index.vue、计时/阅读模型、shared harness 均只读。先前里程碑与 61 项 PASS 仅属第一轮。
- 保留定向 Node、SFC 编译和组件 lint；本轮禁止 Maven/fulltypes/build、E2E、服务、实库、Git、remote 和 subagents；入口只读核对后不再执行 Git 命令。首轮共享 harness 不重写，父页建议只写 integration note，由 Root 协调。
- BDD: Office 切源异步隔离 -> Given SDK 脚本仍在加载或旧编辑器仍有回执，When document URL/base URL/title/unavailableReason 改变但 watch 尚未执行，Then 旧初始化、错误和 opened 不更新当前状态、不触发阅读成功；当前源真实失败仍可见。
- BDD: Office 同步构造失败 -> Given DocEditor 构造中同步触发 onError，When 构造返回实例，Then 该失效实例必须销毁一次、不成为当前实例、不发 opened；迟到回执与卸载不重复销毁。
- BDD: Office 量测回调隔离 -> Given 旧 stage 已被替换或组件卸载，When 已排队 ResizeObserver 回调迟到，Then 不以旧宽度覆盖新 stage、旋转尺寸或卸载后的状态。
- BDD: Office 虚拟视窗占位 -> Given 可见容器高 720、宽 320 或 1000，When 60%/100%/200% 缩放及 0/90/180/270 度旋转，Then iframe 逻辑视窗保持本容器基准宽与 720 高，视图外接矩形全部落在非负滚动占位内，横纵滚动均可到达；缩放不通过反向缩小 iframe 逻辑宽高实现，不重建 editor、不改文档 URL/权限。
- BDD: Office 百分比尺寸精度 -> Given 720 逻辑像素与 110% 缩放，When 计算旋转后的宽度，Then 占位为 792px；用整数百分比运算避免先求 1.1 浮点倍率产生额外一像素。
- BDD: 原生工具与只读边界 -> Given Word/Excel/PowerPoint 的正式文件名与受权 document URL，When 缩放/旋转/复原或切源，Then native documentType/view 配置、权限、水印和 URL 保留，无保存回调或源文件写回；原生分页/搜索可见性及真实像素验收不在本轮范围。

## 第二轮完成与后续收尾
- 新 Office 行为 7 + 阅读接线后只读合同 5 + 相邻 Office 静态 7，19/19 PASS；组件 ESLint 和实际 SFC 三部分编译 PASS。
- 旧 Office 适配前本线程真实结果为 8 PASS / 1 FAIL，保留为 RED 历史；Root 已亲跑原失败，再导出 viewportStyle 并适配固定逻辑视窗及滚动占位断言，同 9 项实际 9/9 PASS、0 skip（来源：Root 回传）。共享合同待办关闭，本线程未重跑。
- 未重跑第一轮完整 61 项，因为阅读线程正在修改 shared harness；旧数字仅为第一轮证据。
- 本轮不重跑 cleanup 脚本，因为其 repo_root() 执行 git rev-parse，与当轮禁止 Git 冲突；只核对当前目录全部六项为正式需保留证据，无临时删除资产。此检查不是 cleanup preview/apply PASS，任务保持 blocked。

## 最终交还与冻结（2026-10-15）
- Root 已完成 `dcc-g09-office-readonly.test.cjs` 最小 fixture 适配：导出 viewportStyle，原生逻辑视窗 1000×720，90°/110% 占位 792×1100，保留原权限/URL/generation 断言；Root 回传同 9 项 9/9 PASS、0 skip。后续综合前端回归仍由 Root 记录。
- 本线程不再修改 OnlyOffice 生产、自有测试、shared harness 或已有源码指纹，不重复任何测试套件；最后一次动作只更新本目录文档并做文档 UTF-8/结构核对。
- 本线程冻结交付：OnlyOffice SHA256 `e8fd6ce9f5760655e477f6036f6efa65f60a71dcd71b76366dd35568c509f7c1`；完整三项 owned 指纹仍见 office-review-fingerprints.json。
