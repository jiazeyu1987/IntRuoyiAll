# E2E-24 正文工具、水印与浏览时间

## Goal

独立验证E2E-24，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 2026-10-21 原合同离线证据审计已完成，见 `../20261021-e24-final-evidence-audit/verification-report.md`。当前 E24 正文范围按原合同 PASS；DWG 严格 RGBA 全等 FAIL 保留为额外显示指标，不证明源正文写回。E23 已证明 WAV/WEBM 普通附件在所列权限场景通过，详见 E23 verification/handoff；不外推其他媒体后缀。服务器源件全字节仍 UNKNOWN，不属于当前 E24 正文门禁。七缺失格式最终接受证据见 `run-missing-formats-1792440609947/continuation-summary.json`；其浏览器已关闭、上下文0、runner退出0。历史 R4/R5 失败记录保留。仓库要求提交/推送才能完成，本轮未授权 Git 操作，故任务 closeout 仍 blocked。

## BDD

- Given 准备PDF、Office、图片、文本及实际支持的其他格式，内容有可辨识标记
- When 打开每种格式，放大/缩小/旋转/翻页并按适用规则搜索；观察水印及浏览时间；失焦、切版、关闭后重开
- Then 真实正文可见而非只有加载容器；PDF须完成非空画布渲染；各正文格式按正式能力逐项记录结果，不能以按钮存在代替实际效果，不能把不支持项直接算通过；阅读只改变画面，重开源件内容不变且不能保存回原件；水印和浏览时间对应当前人/版本。音视频仅作为附件，不进入E24正文验收；其附件查看/下载权限适用E23，但媒体附件本身未据此宣称通过。

- Given 原Office4240受控A/1已由原作者检出且UI无后继草稿，When 本人将同名但不同正文的源件检入一次并从正式入口切换原版和草稿，Then 每次正文标记、草稿标识及阅读统计归属所选版本；A1正文保持，不复用其他版本的阅读时间，不把草稿开放给日常无权账号。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root R5放行后完成PNG/TXT真实前端主链及本轮会话、trace收尾；旧UNKNOWN资产保留。
4. [ ] Root review脚本/trace/可见页面断言，逐分支给出实际结论。
5. [x] 第一阶段：七种缺失源格式的来源/真实性/配套载体清单、最小 BDD 和数据缺口准备完成。
6. [x] 用户重新放行及当前运行态核验后，仅执行七缺失格式路径和必要版本归属断言，逐项获Root验收。

## 2026-10-20 缺失正式格式准备 BDD

- Given 本例既有 Config18/Reader18、DCC241016C24Project、目录及类别，且真实 DOC/XLS/XLSX 夹具来源与哈希已核验；When 获准后通过真实页面上传、预览、提交、签名发布及阅读；Then 对每种扩展名单独核对可辨正文、适用工具、只读、水印和计时；表格必须检查实际工作表内容/导航，不用 DOCX 的 word 阅读器证据覆盖 cell 阅读器。
- Given 真实 DWG/SLDPRT/SLDASM/SLDDRW 及与之对应的 PDF（含来源/配对证据）；When 获准后通过正式页面同时上传源件和图纸 PDF 并完成正式流程；Then 实际阅读的是所选记录绑定的 PDF，非空图形及可辨标记可见，工具/水印/计时/重开均按载体验证；不声称原生 CAD 渲染。
- Given 同一缺失格式链有明确不同正文/配套 PDF 的两个版本；When 从正式入口选择各版本并关闭重开；Then 文件身份、正文标记、水印和阅读归属对应选中版本，旧版不串入新版；不复制 DOCX 切版 PASS，不增加无关版本流程组合。
- 数据缺失、格式只验到容器头或缺少真实配套 PDF 时记录具体 BLOCKED；源件不改字节/后缀冒充，图纸 PDF 不用任意既有 PDF 代替。修复前必须有有效 RED、根因及最小 GREEN；本阶段不修改产品或执行业务验收。
- Given 旧六类别总入口及其历史 PASS，When 本轮显式选择 DOC/XLS/XLSX 或图纸扩展名参数，Then 只返回被选择的缺失格式、真实源件/阅读载体分开，缺配套 PDF/哈希不符/未重新放行必须在浏览器操作前报错；旧六类别合同及 PASS 含义保持。先以离线 Node 测试 RED，再最小参数入口 GREEN。
- 追加授权：用户已明确放行真实 Playwright，运行证明为 E2E10 `r6-history-runtime-proof.json`，Jar SHA256 `d7de2891ceb94e5c4222e6cb9104d6b2d952ca43f9c1758d037167aa70cbcffc`，8061/48061、QMS PID30476；旧 dispatch 不作为本窗口授权。仅完整合法夹具逐个执行；发现产品缺陷先保存真实复现并报告，不改服务/Maven/共享生产。

## 2026-10-20 表格转换缺表最小修复 BDD

- Given 真实 XLS4270/XLSX4271 的两张有内容表在原生 UI 可见、源文件无打印区域，When 现有客户端向 ONLYOFFICE 请求正式 PDF，Then XLS/XLSX 请求显式带 spreadsheetLayout 且 JWT 同步签署相同字段；DOC/DOCX 请求不增加该字段。当前官方参数默认值与保留打印区域的业务决策分开记录，未验证的打印区域规则不计 PASS。
- RED：仅扩展已有 DccOnlyOfficeConversionClientImplTest 的格式参数及请求/JWT语义断言，旧实现必须因 XLS/XLSX 缺布局失败，DOC/DOCX继续通过；不得用编译失败作为 RED。
- GREEN：向 Root 先报告方案，再按核准的最小字段修改 Impl；定向测试通过仅表示客户端合同通过，必须等待新运行包与真实后继版本 UI 验收才能关闭业务缺陷。旧4270/4271发布件及失败截图保留。
- 当前 UI checkpoint 已封存且浏览器0、runner18156退出。独占定向测试窗口获准；不构建/重启服务、不修改旧发布物。CAD及版本归属未验项保留。

- Given 同一 sourceFileId/path/name 的历史表格转换可能已被 ONLYOFFICE 按 key 缓存，When 启用新表格布局规则，Then XLS/XLSX key 与旧规则不同且重试稳定；DOC/DOCX 保持既有 key。先用固定历史 key 的回归用例取得 RED，再在已有 rawKey 增加固定表格规则版本后缀。
- 本段生产修改权限以 Root 本轮明确授权为准：仅转换客户端/已有测试，以及转换服务 buildConversionKey/已有测试；不扩展到其他生产文件。
- Given 原4270/4271及A1源件保留，When 用已有POI/openpyxl仅将任务自有A2副本两表A1改为独立新标记并从正式页面各检入一次，Then 解析回读两标记、活动表及打印设置保持，旧版和后继版正文/水印/计时归属分别验证；新包放行前只准备脚本，不启动UI。单表SimpleMultiCell旧准备件不能用于此次缺表修复验收。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。
- 2026-10-20 用户明确：原合同不要求新增原件下载入口或服务器全字节比对；仅复核已有R5/R6证据，不重跑已通过子链。以已保存源哈希、重开正文、只读工具及非阅读业务写0验证原件不变。音视频只作为附件，E23权限规则适用；2026-10-21 纳入 E23 已接受的 WAV/WEBM 普通附件专项证据，其他后缀不外推，不属于E24正文PASS门禁。546/401保持UNKNOWN且不清理。

## 设计约束检查

- 当前生产修改仅限本轮 Root 明确授权的转换客户端、转换服务 buildConversionKey 及其两份已有测试；仅在独占窗口跑必要定向 Maven。其余源码/共享.env 不改，不构建运行包、不重启服务、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；本轮运行证明为 `../20261020-qms-e2e24-runtime-update/runtime-proof.json`，Jar ee67ec8f…，Root已明确放行，准备文件executionRelease.authorized=true。仅沿本例原对象的单次后继链执行；不修改生产、不构建或重启。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C24；本轮已授权经真实前端创建本例账号、角色/菜单权限和业务 fixture，不修改已有账号密码、共享角色/菜单定义或公共矩阵。
- 本目录由本例线程维护，Root独占协调目录。自行核验本例 fixture 后继续执行，不以共享 roleFixtureVerified=false 停工。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792438891279

- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792437223338
- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792434821434
- doc/tasks/20261016-dcc-e2e-24/prepare-spreadsheet-a2.py
- doc/tasks/20261016-dcc-e2e-24/spreadsheet-successor.cjs

- doc/tasks/20261016-dcc-e2e-24/missing-formats.cjs
- doc/tasks/20261016-dcc-e2e-24/missing-formats.test.cjs
- doc/tasks/20261016-dcc-e2e-24/fixtures/formal-missing-source

- doc/tasks/20261016-dcc-e2e-24/contract-evidence-review-20261020.md
- doc/tasks/20261016-dcc-e2e-24/contract-evidence-review-20261020.json
- doc/tasks/20261016-dcc-e2e-24/contract-review-20261020
- doc/tasks/20261016-dcc-e2e-24/remaining-coverage-review-20261019.md
- doc/tasks/20261016-dcc-e2e-24/repair-20261019-r6-handoff.json
- doc/tasks/20261016-dcc-e2e-24/r6-txt-pagination.cjs
- doc/tasks/20261016-dcc-e2e-24/run-r6-txt-pagination
- doc/tasks/20261016-dcc-e2e-24/source-download-boundary-20261020.md
- doc/tasks/20261016-dcc-e2e-24/run-r5-source-only
- doc/tasks/20261016-dcc-e2e-24/r5-source-readback.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-current-run.json
- doc/tasks/20261016-dcc-e2e-24/r5-prepare.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-published.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-verify-viewer.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-final-readonly.cjs
- doc/tasks/20261016-dcc-e2e-24/r5-seal-results.py
- doc/tasks/20261016-dcc-e2e-24/run-r5-pngtxt-1792422918342
- doc/tasks/20261016-dcc-e2e-24/fixtures/checkin-A2
- doc/tasks/20261016-dcc-e2e-24/run-2026-10-18T22-10-44-428Z-3b2de8ff
- doc/tasks/20261016-dcc-e2e-24/same-name-working-fixture.json

- doc/tasks/20261016-dcc-e2e-24/task.md
- doc/tasks/20261016-dcc-e2e-24/execution-log.md
- doc/tasks/20261016-dcc-e2e-24/verification-report.md
- doc/tasks/20261016-dcc-e2e-24/prepared.json
- doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs
- doc/tasks/20261016-dcc-e2e-24/result.json
- doc/tasks/20261016-dcc-e2e-24/trace.zip
- doc/tasks/20261016-dcc-e2e-24/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-24/checkpoint-live.json
- doc/tasks/20261016-dcc-e2e-24/checkpoint.json
- doc/tasks/20261016-dcc-e2e-24/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-24/issues-20261018.md
- doc/tasks/20261016-dcc-e2e-24/run-20261018
- doc/tasks/20261016-dcc-e2e-24/fixtures
- doc/tasks/20261016-dcc-e2e-24/ui-session.cjs
- doc/tasks/20261016-dcc-e2e-24/resume-preview.cjs
- doc/tasks/20261016-dcc-e2e-24/prepare-organization.cjs
- doc/tasks/20261016-dcc-e2e-24/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-24/source-policy-candidate-red.cjs
- doc/tasks/20261016-dcc-e2e-24/BackendSourcePolicyRed.java
- doc/tasks/20261016-dcc-e2e-24/seal-runtime-window.cjs
- doc/tasks/20261016-dcc-e2e-24/make-fixtures.cjs
- doc/tasks/20261016-dcc-e2e-24/make-office-fixture.py
- doc/tasks/20261016-dcc-e2e-24/make-media-fixtures.cjs
- doc/tasks/20261016-dcc-e2e-24/ui-actions-20261018.jsonl
- doc/tasks/20261016-dcc-e2e-24/prepared-preparation-20261016.json
- doc/tasks/20261016-dcc-e2e-24/result-preparation-20261016.json

- doc/tasks/20261016-dcc-e2e-24/pause-current-runtime.cjs

- doc/tasks/20261016-dcc-e2e-24/run-20261018-151101
- doc/tasks/20261016-dcc-e2e-24/run-20261018-200656
- doc/tasks/20261016-dcc-e2e-24/prepare-own-directory-reading.cjs
- doc/tasks/20261016-dcc-e2e-24/prepare-own-project-reader.cjs
- doc/tasks/20261016-dcc-e2e-24/open-active-list.cjs
- doc/tasks/20261016-dcc-e2e-24/verify-published-body.cjs
- doc/tasks/20261016-dcc-e2e-24/office-formal-tools.cjs
- doc/tasks/20261016-dcc-e2e-24/registered-reading.cjs
- doc/tasks/20261016-dcc-e2e-24/reopen-registered-body.cjs
- doc/tasks/20261016-dcc-e2e-24/prepare-registered-body.cjs
- doc/tasks/20261016-dcc-e2e-24/submit-working.cjs
- doc/tasks/20261016-dcc-e2e-24/open-own-task.cjs
- doc/tasks/20261016-dcc-e2e-24/sign-own-task.cjs
- doc/tasks/20261016-dcc-e2e-24/repair-own-task-menu.cjs
- doc/tasks/20261016-dcc-e2e-24/prepare-signature-image.cjs
- doc/tasks/20261016-dcc-e2e-24/office-continuous.cjs

- doc/tasks/20261016-dcc-e2e-24/run-20261018-225739-recovery
- doc/tasks/20261016-dcc-e2e-24/inspect-original-office-checkin.cjs
- doc/tasks/20261016-dcc-e2e-24/probe-actor-reading.cjs
- doc/tasks/20261016-dcc-e2e-24/capture-published-canvas-metrics.cjs

- doc/tasks/20261016-dcc-e2e-24/r06-remediation-design-20261019.md
- doc/tasks/20261016-dcc-e2e-24/r06-png-text-pdf-scope-20261019.md
- doc/tasks/20261016-dcc-e2e-24/r06-source-contract-red.cjs
- doc/tasks/20261016-dcc-e2e-24/run-20261019-r06-design

- doc/tasks/20261016-dcc-e2e-24/r06-direct-red-freeze-20261019.md

- doc/tasks/20261016-dcc-e2e-24/repair-20261019-r2-handoff.json

- doc/tasks/20261016-dcc-e2e-24/r2-temporary-assets-readonly.cjs
- doc/tasks/20261016-dcc-e2e-24/run-20261019-r2-temp-1792406172271
- doc/tasks/20261016-dcc-e2e-24/run-20261019-r2-temp-1792406293126

- doc/tasks/20261016-dcc-e2e-24/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-24/run-r3-maven.py
- doc/tasks/20261016-dcc-e2e-24/run-20261019-r3-java


- doc/tasks/20261016-dcc-e2e-24/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-24/repair-20261019-r5-handoff.json
- doc/tasks/20261016-dcc-e2e-24/seal-r4-handoff.cjs

- doc/tasks/20261016-dcc-e2e-24/cad-paired-tools.cjs
- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792439951654
- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792440482892
- doc/tasks/20261016-dcc-e2e-24/run-missing-formats-1792440609947

- doc/tasks/20261016-dcc-e2e-24/version-reading-isolation.cjs

## Sensitive Local Keep

credentials.dpapi 为当前Windows用户DPAPI加密的本例恢复凭据；保留供后续本人登录，不作为验证证据输出或提交。旧Reader没有安全缓存，不猜测或重置。


## R5新包页面验收 BDD

Given Root新包proof及本例既有配置/账号/PNG与中文长行TXT，When 由真实页面上传、预览、审批、签名盖章并发布，Then 原件保持、正式PDF实际非空且正文/章/签名/绑定可核，水印和读时归属正确。

## R5最终验收边界

PNG文件2054545668044084262、TXT文件2054545668044084263均由原账号真实UI上传、审批、文控签名并生效为A/1；仅前端操作，未调用业务API/SQL。正式PDF正文/章、缩放旋转复原、水印、失焦停计时、恢复/重开读时及5项已校验签名、发布件/盖章件可用均有证据。PNG纯图片无文本层，搜索不适用且不记PASS；TXT中文长行及第4页末尾可搜。

原件完整SHA只验证本地夹具未变；服务器源件仅页面12位哈希前缀一致，CATEGORY_DOWNLOAD_DENIED下未下载全字节比对，禁止扩大权限补验。全例仍待此项及WEBM/WAV三项合同，旧TEMP546/cleanup401保持UNKNOWN。旧PDF/Office/4253/4254、Java18/60不重跑。

汇总：run-r5-pngtxt-1792422918342/r5-png-txt-summary.json。历史headless失焦和PNG旧详情选择器失败原样保留；headed读时和最终管理页补验证据分别说明实际通过范围，未把失败运行改为PASS。

## R5原件全字节补验范围与BDD

Given 原PNG/TXT两记录、原fixture及已通过主链证据，When 仅从正式UI核对类别DOWNLOAD规则和原件下载入口，Then 有明确原件入口才为本例用户/类别临时补最小DOWNLOAD并在finally精确恢复原规则，真实UI下载后比对全SHA；无原件入口则记录精确产品缺口和最小修复分析，不以发布PDF、存储管理员、URL/API/SQL或换文件替代。

本轮只补上述缺项；禁止重跑PNG/TXT主链、PDF/Office或Java测试，禁止共享源码/服务/Maven变更。13独占Maven，19844新包沿用。初步静态链：browser openDownload → triggerControlledFileDownload → /{id}/download → readDownloadBinary → resolveBinaryFileId(DOWNLOAD) 固定publishedFileId，并非sourceFileId；因此先做零写入页面/配置核对，不先扩大授权。

补核结果：BLOCKED_NO_FORMAL_SOURCE_DOWNLOAD_ENTRY。未增加任何DOWNLOAD；类别全规则前后相等，目录全快照未形成可比对稳定证据且无修改。已响应Root停止当前只读浏览，所有浏览器关闭、runner退出；安全重启点见run-r5-source-only/source-only-result.json。精确边界及入口链见source-download-boundary-20261020.md。

## Latest E24 targeted recheck

repair-r4-targeted-recheck/verification-report.md and handoff.json are the latest targeted rerun. XLSX BLOCKED after first foreground growth passed, because real inactive predicate did not become true. DWG/SLDDRW not run following immediate stop gate. Browser closed, contexts 0, runner exit 0. Historical failures remain open.

## Latest E24 native focus correction

repair-r4-native-focus/verification-report.md supersedes only XLSX current result: native focus prerequisite fixed, script7/7 and ONE real XLSX run4/4 PASS; original triggers reached. DWG/SLDDRW not rerun and historical failures remain. Earlier all-three BLOCKED report preserved as historical run. Browser closed, contexts0, runner24524 exit0.

## Latest E24 remaining final report

repair-r4-remaining/verification-report.md and handoff.json: DWG exactfullRGBA FAIL22pixels43channels max1; sourcebytes BLOCKED no original UI. SLDDRW nativefocus/pause/original35s/resume/reopen continuity PASS122==122. XLSX not rerun, prior4/4PASS retained. Browserclosedcontexts0; runner Ctrl+Cexit1 after trace seal due observer interval, disclosed. No more testing.
