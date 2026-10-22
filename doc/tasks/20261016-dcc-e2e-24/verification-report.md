# E2E24 当前验证报告

## 2026-10-21 原合同证据最终审计（当前状态）

当前 E24 正文范围按原始业务合同 **PASS**。逐声明 PASS/FAIL/UNKNOWN 及各正文格式可直接抽查的结果 JSON、脱敏 trace 和截图入口见 `../20261021-e24-final-evidence-audit/verification-report.md`。审计沿用既有脱敏证据，没有重跑浏览器或业务动作。

DWG 严格 RGBA 全等历史结果仍为 **FAIL**：22 个像素、43 个通道、最大通道差 1；这是后来加入的零容差显示一致性指标，不是 E24 原合同条款，也不证明源正文被写回。原合同层面的 DWG 重开证据在 `run-missing-formats-1792440609947/DWG-reopen-contract-audit.json`：同一版本及可见正文、两次自然 PDF 响应字节和 SHA 一致、非阅读写入为零；源哈希支持原件未变。服务器源件全字节没有读取，保持 UNKNOWN，但按用户确认不作为当前业务门禁。

E23 已补验并获 Root 接受：文件2054545668044084384的 `audio/wav` 与 `video/webm` 普通附件，在 ViewOnly/ViewDownload 正身份播放、NoAccess 拒绝，且仅 ViewDownload 两次下载的 SHA 分别与输入匹配。细节见 `../20261016-dcc-e2e-23/media-attachments-20261020-verification.md` 与 `media-attachments-20261020-handoff.json`。结论仅限 WAV/WEBM 及该 E23 附件权限场景，不扩张为其他后缀或 E24 正文格式通过。CAD 配对 PDF 阅读不代表原生 CAD 几何/装配验证；546/401 仍 UNKNOWN_UNTOUCHED。历史 FAIL 和早期阻塞记录保留，仅其过期的“当前状态”解释由本节及最终审计取代。任务 closeout 仍 blocked：仓库要求提交并推送，而本轮未获 Git 操作授权。

## 音视频范围澄清（当前，覆盖下方历史口径）

原验收合同 `doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md` 的 E2E-24 行已注明：音频、视频仅作为附件，不是正式受控正文；不纳入正文格式准入、正文工具、翻页或正文搜索。此前历史段落将 WEBM/WAV 正文准入或分页/搜索列为 BLOCKED 的说法仅保留历史，不再作为当前 E24 正文门禁。该范围变化不改变其它格式和正文分支结论。

附件查看/下载按 E2E-23 单独验收。E24 自身运行没有执行媒体附件分支；之后 E23 `media-attachments-20261020-verification.md` 及 `media-attachments-20261020-handoff.json` 补验 WAV 与 WEBM 普通附件，业务结果 6 PASS / 0 FAIL / 0 BLOCKED 并由 Root 接受。该结果仅覆盖 WAV/WEBM、文件2054545668044084384和报告所列三身份权限场景，不能外推至其他媒体后缀。媒体仍不属于 E24 正文验收阻塞。

## 2026-10-20 用户确认后的整例结论（当前）

E24正文范围整例 **PASS**。依据是原已接受的正文格式证据及本报告下方七缺失格式逐项验收证据；不重跑历史PASS。用户确认音频、视频只作为附件，不属于正式受控正文，因此WEBM/WAV正文准入、正文工具、翻页和正文搜索不再是E24门禁。此结论不代表WAV/WEBM附件已验证通过。

音视频附件的查看/下载权限遵循E23。之后的 E23 普通附件补充分支已通过 WAV/WEBM 两种格式实测并由 Root 接受：两正身份播放、NoAccess 拒绝、ViewDownload 原生下载且 SHA 与输入一致。具体后缀、fileId、SHA 与范围见本报告首段引用的 E23 报告和 handoff。不得据此推断其它后缀或受控正文准入。

本次只同步既有task、execution-log、verification-report及续验收汇总；未运行E2E、测试、服务或业务动作。任务保持`in_progress`供Root review，未执行Git提交/推送或closeout。

## 2026-10-20 七缺失格式续验收口（当前）

DOC/XLS/XLSX/DWG/SLDPRT/SLDASM/SLDDRW原范围已逐项获Root独立验收，不再重跑。当前唯一汇总为 `run-missing-formats-1792440609947/continuation-summary.json`；正文工具、水印、失焦停计/重开及版本隔离证据按格式分别保留，不以SLDASM代替SLDDRW。CAD载体为真实源件对应的发布者配套PDF，不声称原生CAD几何验证；无文本图纸搜索、单页图纸翻页分别记N/A。

DOC既有正式4269/工作4276从真实“工作小版本”筛选和操作面板进入，工作身份精确4276。四次自然stop/summary核验：旧47→58、重开58→69；新201→211、重开211→221；真实OnlyOffice表格及搜索1/1、独立水印和非阅读写0。错误入口旧STOPPED与拒绝页面另存保留。

浏览器上下文0、浏览器关闭、runner35329退出0；本run共16份trace已脱敏及保存自然响应白名单、SHA/CRC归档记录，raw已移除。无生产修改、服务操作或Git提交推送。该段原先将WEBM/WAV正文准入列为待决，现由本报告顶部的用户确认范围取代；原件全字节比较已移出业务门禁，旧UNKNOWN仅是工程清理边界。七格式与既有正文证据合并后构成当前正文范围整例PASS，不构成媒体附件专项PASS。

## 2026-10-20 原合同离线复核（Asia/Shanghai，当前）

当前结论：四种已支持格式的已验证阅读子链及R6 TXT正常翻页PASS保留；原件内容不变/不能保存回原件有现存证据支持，不再以“没有原件下载入口/未比对服务器全字节”阻塞。原合同也未要求在上传预览另行重复正式受控阅读的全部工具测试。下文历史记录中的相关范围门禁以本节及`contract-evidence-review-20261020.md`为准，原始运行结果不改写。

本次从原PNG/TXT脱敏trace离线提取了关闭重开后、进入详情前的可见帧，均为原ACTIVE A/1：原图正文标记和TXT中文首段仍在，受控章与新水印可见；两份阅读记录的写请求仅reading-session start/heartbeat/stop，非阅读业务写0，无正文编辑/保存入口或操作。结合上传前后本地完整SHA一致、最终UI源哈希前缀一致及PDF/Office原有重开画布指纹证据，按原合同判定阅读后正文不变及只读PASS。未获取服务器源件全字节，不宣称全字节比较完成。

真正未独立覆盖的能力明确保留：PNG/TXT上传预览的全部原生工具、PDF/PNG/TXT各自独立版本切换不能借用正式PDF工具或Office4240→4253→4254证据冒充独立PASS；不据此扩大原合同为双入口或逐格式版本矩阵。WEBM/WAV正式受控能力尚未验证，分页/搜索语义待业务澄清，不实现伪功能、不记PASS。546/401保持UNKNOWN_UNTOUCHED，属于历史残留状态，不清理、不列新产品缺陷。

历史记录：当时将音视频合同列为待决并判整例 blocked；用户随后确认媒体仅为附件、属于 E23，且最新七格式汇总已接受正文范围。当前状态以本报告首部最终审计为准。原段涉及的 trace CRC、帧提取等历史执行记录不变。

## 2026-10-19 R6 最小剩余项复测（America/New_York，历史）

仅补原TXT2054545668044084263的上一页/下一页，真实Reader18由受控浏览进入ACTIVE A/1。结果`PASS_TXT_NEXT_PREVIOUS`：页码1/4→2/4→1/4，滚动0→1285→222；685×969画布第一页130002、第二页60949非白像素，第二页画布SHA不同，回到第一页与初始SHA相等，第一页“上一页”禁用。三张截图保留，第二页已视觉核对真实中文及受控章。未操作页码跳转、搜索、变换、水印/计时主链或其它格式。

现有R6运行包PID22076/48061、8061 PID20976及Jar SHA478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a已核对。脚本node --check exit0，真实运行PASS，浏览器关闭、runner exit0且无残留进程。自然非阅读业务写0，未API/SQL、权限变更、构建/Maven或服务重启。证据`run-r6-txt-pagination/pagination-result.json`、三张页面截图、脱敏`pagination.zip`；无raw残留。

剩余分类详见`remaining-coverage-review-20261019.md`，交接见`repair-20261019-r6-handoff.json`：可执行的TXT分页缺口已补；本轮无新增已证实产品缺陷。原件正式入口及服务器全字节、原生源件工具/重开一致性仍是范围入口缺口；WEBM/WAV正式受控及分页/搜索语义待业务决定；旧546/401仍UNKNOWN_UNTOUCHED。历史Office根因和运行环境500保留后续原对象PASS，不重跑。版本切换精度：Root实际4240→4253→4254是Office链，不能据旧聚合两格式填值声称PDF独立切版也通过。当前result.json已作此口径更正，历史原始证据不覆盖。

本轮最小复测已结束，整例仍blocked；未提交/推送或cleanup apply，不宣称completed。以下R5全部通过事实及限制继续保留；“当前R5/可供准备13重启”是当时窗口状态，不代表R6再次等待新包。

当前R5状态：`PNG_TXT_UI_CHAIN_PASS_SOURCE_BYTES_UNVERIFIED_MEDIA_PENDING`。新包门禁已解除，PNG/TXT真实UI上传、审批、签名盖章生效、正式PDF阅读及落位检查已完成。2026-10-20补核确认正式下载固定读取发布PDF，缺少原始PNG/TXT下载入口，服务器原件全字节仍未验证；未新增DOWNLOAD规则或产品功能。WEBM/WAV业务合同、Git授权仍阻塞整体完成。当前浏览器/runner已关闭，可供Root准备13新包安全重启。详见`source-download-boundary-20261020.md`、`repair-20261019-r5-handoff.json`与`run-r5-pngtxt-1792422918342/r5-png-txt-summary.json`。

R4及R5放行前“等待新包、没有UI执行”的内容仅为历史记录，已被以下实际运行证据替代；不得据此继续报告旧包阻塞。

## R5新包真实页面验收（2026-10-19）

Root `r5-runtime-proof.json`：8061/48061，PID19844，Jar `backend-workflow-v6-20261019-230954.jar`，SHA256 `3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c`，59类核验、health UP、真实登录、显式simhei.ttf Resource，newFixesIncluded=true。本例只复用该运行态，未构建或重启服务。

| 项目 | PNG / IMAGE | TXT / TEXT |
| --- | --- | --- |
| 精确记录 | 2054545668044084262，BPM 862ba81d-cbd0-11f1-876f-b082e25ec548 | 2054545668044084263，BPM b9b5525e-cbd0-11f1-876f-b082e25ec548 |
| 真实源件预览 | 800×400原图、标记与色块可见 | 独立R5中文夹具4239字、最长1365字行，源件首屏逐字匹配前缀 |
| 业务链 | 原作者Config18上传/提交，Reader18主管、指派、会签、批准及文控签名，ACTIVE A/1 | 同项真实页面完成，ACTIVE A/1 |
| 正式PDF | 685×969，186652非白/14802红像素，原图及受控章可见 | 685×969，129956非白/19731红像素，中文正文/受控章可见，共4页 |
| 工具/搜索 | 缩放、旋转、复原PASS；纯图片无文本层，不记搜索PASS，不做OCR | 同项工具PASS；中文首段/长行及终末完整标记R5END搜索PASS（末标记第4页） |
| 真实聚焦/失焦 | 403→413；失焦12秒414→414，隐藏心跳0，恢复414→424 | 0→10；失焦12秒10→10，隐藏心跳0，恢复10→21 |
| 重开 | 424→425，当前Reader18与A/1，新水印 | 21→22，同项PASS |
| 最终管理页 | master A/1，发布件/盖章件可用，5项签名“已校验”，批准文件负责人Config18 | 同项PASS |
| 原件未变证据 | 本地全SHA不变；页面源哈希0b27bfa1b012一致 | 本地全SHA不变；页面源哈希083e49d89d05一致 |

全字节边界：服务器源件未下载；当前类别DOWNLOAD_DENIED。页面源哈希前缀一致、原件预览及本地全SHA不变不能替代服务器全字节比对，不宣称此项PASS。未调整权限、未直接API下载、未读写数据库。存储绑定只声明正式UI可用性、master身份与实际正文/章，不宣称读取了后台完整存储ID或文件字节。

脚本边界：headless实际失焦未建立，保留原FAIL；改用headed Chromium关闭焦点模拟后，确实document.hasFocus=false，12秒零心跳并恢复通过。PNG随后因旧`.detail-header-shell`选择器在只读详情不存在而失败，保留FAIL及前序已通过字段；改用当前business-records和正式管理页补核签名/落位，未重跑已通过PNG阅读。TXT完整最终只读run为PASS。初始TXT canvas截图有瞬时加载遮罩；最终`TEXT-final-viewer.png`已视觉核对清晰中文和受控章，不能单用初始截图证明清晰渲染。

证据位于run-r5-pngtxt-1792422918342：source-IMAGE/TEXT.json、signed-IMAGE/TEXT.json、readonly-headed/r5-final-readonly.json、final-text/r5-final-readonly.json、final-text/final-binding-source.json及截图/脱敏trace。正式阅读期间自然网络监听仅reading-session start/heartbeat/stop写请求，非阅读业务写0；只记录path/method/status，不解析响应JSON。聚合脚本`r5-seal-results.py`实际assert与ZIP CRC/去network payload检查PASS。

保留RED 9/6失败、后端18/18及回归60/60 GREEN、原PDF/Office/4240→4253→4254 PASS，不重跑。本轮无生产修改、Maven、Git变更、服务重启或外部线程消息。浏览器关闭，交互runner exit0，无r5-ui-session进程、无raw.zip残留，6份本轮trace脱敏保留。业务文件保留为已发布验收证据；旧546/旧401依旧UNKNOWN，不重复检索/猜删。WEBM/WAV三项合同仍待定，整例不标completed。

## 2026-10-18 22:57恢复窗口最终核对（历史）

整体：BLOCKED_REMAINING_VERSION_AND_FORMATS，原完整用例未PASS。已完成当前正式A/1全部独立阅读参数；同链切版与四种原生格式仍未完成。成功的A/1参数不代表全格式或跨版本PASS。

| 参数 | PDF正式A/1 | Office正式A/1 |
| --- | --- | --- |
| 原对象身份 | 2054545668044084235 / 原BPM fb7fb798-cad2-11f1-a035-b082e25ec548 | 2054545668044084240 / 原BPM 8477da2d-cae3-11f1-a035-b082e25ec548 |
| 正文与工具 | PASS：两页、正文标记、翻页画布变化、缩放/旋转/复原 | PASS：正式发布转换PDF的同项能力；原生OnlyOffice任务正文证据另保留151101窗口 |
| 真实非空画布 | 704×911；676采样像素中16非白 | 704×911；676采样像素中22非白 |
| 当前人水印及A/1页面身份 | Reader18、Config18均PASS | Reader18、Config18均PASS |
| 失焦12秒冻结与恢复 | Reader2397→2410，失焦/恢复均2410，恢复心跳2420；Config0→12，失焦/恢复均12，恢复心跳23 | Reader2244→2257，失焦/恢复均2257，恢复心跳2267；Config0→12，失焦/恢复均12，恢复心跳22 |
| 重开正文与只读行为 | 两账号canvas指纹均保持；新水印trace；累计保留；非阅读业务写0 | 两账号同项PASS |
| 账号累计隔离 | Reader2425→2427，只有自身核对窗口耗时增长；编制人阅读未叠加 | Reader2271→2272，同项PASS |
| 同链切版、跨版本归属 | 未验证 | 未验证：全部版本仅A/1共1条，无工作稿可切换；未重复原checkin/签名 |

账号隔离采用真实UI在编制人阅读前/后分别打开Reader统计。增长上限按Reader两次自身开窗实际耗时计算（各8秒上限，实际增长2/1秒），不把编制人期间的阅读计入Reader。失焦用例独立保持1秒误差门限，本次四项实际均零增长、零隐藏心跳。

### 运行与范围

- Root恢复同一200656包，SHA256 264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5；本例只读核对PID14864/48061、前端24564/8061与恢复证明browser-replacement-recovery-proof.json。新独立context本人沿正式租户输入/选项登录；未注入认证状态或读取接口JSON作业务oracle。
- 恢复后第一步只读查询Office编号全部版本：原A/1共1条，UI未见工作稿；打开原file详情确认原BPM、A/1和签名记录。原未确认检入未重发，未创建替代文件、重签、撤销检出或推断物理票据已清理。
- 仅按Root本轮明确授权新增项目304的Reader18 VIEW，真实保存并重新打开回读；原Config18 OWNER保持。目录913929两用户规则沿用，未重复写目录或扩张其它权限。
- 四份正式生效正文结果全部PASS且自然请求无非200；之前PID34440退出导致的500/登录失败保留历史并归为环境，恢复同包后实际阅读已通过，不再列为账号权限或阅读产品缺陷。
- IMAGE/TEXT/VIDEO/AUDIO保持R1624-06 BLOCKED，原六格式参数未缩减；音视频分页/搜索语义未定义。不扩白名单，不制造盖章ID或用另一文件冒充同链版本。

### 可复核证据

run-20261018-225739-recovery/parameter-coverage.json为逐参数矩阵；published-PDF/OFFICE-DCC241016C24Reader18/Config18.json为四份当前版本结果；actor-reading-isolation.json及before-author/after-author两份DOM计时为账号隔离；original-office-checkin-readback.json为原检入只读回读；own-project-reader.json为项目VIEW保存/回读。三份正文/隔离trace及原对象、权限trace均脱敏，画布指标与实际canvas截图在canvas-metrics/。原OnlyOffice原生页/搜索和正式任务读时证据在run-20261018-151101。

源DOCX本地原哈希保持，A2夹具只在原唯一一次检入尝试使用；此次未重上传。所有业务仅Playwright真实页面；API/SQL/mock/force/生产代码/构建/服务/Git操作均未执行。

### 停止与保留

工作runner16932及所属browser41992已关闭，补录画布runner10452正常退出0并无直接子进程残留；所有context/browser关闭，业务静止。checkpoint-live.json和checkpoint.json保持同一封存内容。runtime-pause.active=false表示已执行Root恢复放行，本例当前为剩余参数阻塞。原对象、未确认票据/检出历史与旧临时残留全部保留供Root复核，不宣称业务资产清理完成；任务未标completed，未执行cleanup apply或Git。

## 此前窗口历史记录


## 2026-10-18 22:54 Root运行态归因与暂停（最新）

Root明确核实48061/PID34440已退出且连接拒绝。此前观察到的登录失败、业务500当前归类为环境阻塞，不能据此认定账号权限或阅读计时代码缺陷；原HTTP500/页面报错证据保持原样。Root将恢复同一200656已验证Jar，不加载新源码，本例不改服务、代码、DB或权限。

本例已关闭的context/browser/runner保持关闭，49792退出0；再次核对38304/49792及直接子进程均不存在。runtime-pause.json active=true，checkpoint两份同步。原Office/PDF A/1已生效成功事实保留；Office检出/源件上传及唯一一次checkin未确认状态保留，不撤销、不重建、不重复提交。项目VIEW未保存，当前不扩权。

恢复条件为Root明确健康和真实UI登录放行；届时先用新独立context真实登录、只读核对原file/BPM/task/版本和原checkin结果，再决定后续动作。当前不以旧运行证明或源码候选替代新运行态验证。

## 此前执行历史


## 2026-10-18 200656窗口执行结果（最新）

- 当前整体BLOCKED，原完整六格式及切版/用户隔离未通过，不能提交整例PASS。恢复前核对int_qms、新PID34440、Jar200656和SHA256 264efbd4ee539115f1a7d5b43941029a2fd7a03f03b7df9d9e8964d8cf3ffeb5。Root21:04健康/登录证明只代表放行时点，后续实际新会话已失败，未把旧证明当当前登录成功。
- 原Office file2054545668044084240 / BPM8477da2d-cae3-11f1-a035-b082e25ec548 / A/1真实详情确认ACTIVE；master A/1、发布件和盖章件可用。上次文控签名未重复。
- 原PDF file2054545668044084235 / BPMfb7fb798-cad2-11f1-a035-b082e25ec548 / A/1，继续原主管taskfb802be5-cad2-11f1-a035-b082e25ec548，实际主管、负责人指派、会签、批准、文控签名完成。最后文控taska91a8208-caf9-11f1-ad52-b082e25ec548签名成功并待办清空；后续新Config18会话真实项目列表确认受控（已生效）。各任务签名证据在manifest及signed-PDF.json，不以HTTP受理代替终态。
- PDF正式生效正文：两页非空canvas、正文搜索、缩放/旋转/复原和Config18水印实际通过；reading-session/start200、summary200，heartbeat自然返回500，页面显示“阅读计时或统计失败”，累计0秒，focus等待失败。证据published-PDF-DCC241016C24Config18.json、published-runtime-200656.zip及失败截图。151101窗口Reader18正式审批任务正文、真实失焦12秒冻结/恢复和同版本重开PASS继续保留，不能覆盖本次发布阅读失败。
- Office正式发布预览：原file2054545668044084240通过可见浏览打开新窗，30秒仍无版本正文身份；之后截图仍是系统启动加载图，未达到正文阶段，不能判为DOCX源件本身不支持。证据published-OFFICE-DCC241016C24Config18.json、office-published-preview-loading-timeout.png和published-runtime-200656.zip。
- Office同链换版：原Config18真实检出A/1、上传带独立A2正文标记的真实DOCX；本地原DOCX哈希不变，新DOCX哈希不同。检入按钮只提交一次，45秒窗口未关闭；只读另页仍看到A/1被Config18检出。第一次检入HTTP结果未捕获；不能因页面/计时超时判定没有写入。当前checkin=UNCONFIRMED，未重复提交，未撤销检出或清除可能绑定的票据。后续全部版本查询出现页面30000ms timeout，最终新版本身份仍未知。
- 夹具：类别3条私有规则真实回读均启用；类别VIEW与目录正文授权、项目VIEW是独立权限。新增本例目录Root（913929）两条USER规则，仅Config18/Reader18名称和内容查看=true，下载=false，保存/重读PASS；其他目录仅只读GET，唯一PUT为本例913929。Reader18仍缺本例项目正式VIEW；已确定正式入口为原项目“权限→添加查看”，未用分配修正或负责人文本代替。准备该授权时新会话在登录页显示租户识别失败，尚未进入项目权限页，saved=false。
- 登录失败DOM保存在own-project-reader.json；登录凭据阶段未开启trace；失败登录PNG不交付，保留DOM并修正后续错误截图先清空密码字段。所有已完成trace去除network payload/header/cookie/URL query，当前raw.zip残留0。
- IMAGE/TEXT/VIDEO/AUDIO仍由R1624-06源件/发布/盖章合同阻塞，原入口RED保留；未改白名单、制造stampedId或把媒体分页/搜索语义标为PASS。
- 自动化问题单独记账：runner38304因未处理popup等待Promise退出1；已改为Promise.all同时等待并提前校验入口，后续runner49792正常退出0。Office初始化等待失败时现在保留新窗DOM/截图。旧OnlyOffice mount iframe/innerText/hidden contenteditable假断言已离线改为复用真实验证过的原生搜索、canvas及可见保存编辑入口断言；此集成仅语法检查，不宣称完整E2E回归通过。
- 当前业务静止，全部context/browser关闭，49792及其直接子进程均不存在；checkpoint-live.json与checkpoint.json封存一致。runtime-pause.active=false表示Root放行已执行，当前是实际环境/产品阻塞，不冒称Root再次下达暂停。保留所有原对象、未确认检入和历史临时残留。生产、Git、服务、API/DB业务操作均未执行。

## 此前窗口历史记录


## 2026-10-18 第二次运行窗口暂停（最新）

- 已明确业务静止；Root统一更新48061前停止全部新增业务动作。runner PID26968（handle22110）退出码0；本例browser PID29828父进程26968及已记录子进程均消失，其他浏览器未操作。全部context关闭，checkpoint-live.json与checkpoint.json一致，runtime-pause.json active=true。
- 本窗口运行Jar151101，SHA256 bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f；旧窗口历史证据保留。
- PDF：file2054545668044084235 / BPM fb7fb798-cad2-11f1-a035-b082e25ec548 / task fb802be5-cad2-11f1-a035-b082e25ec548 / A/1；待直属主管审核，未签名。实际两页非空画布、正文搜索、缩放旋转、水印、真实失焦12秒冻结及恢复、同版本关闭重开源画布一致已逐项通过。
- Office：file2054545668044084240 / BPM8477da2d-cae3-11f1-a035-b082e25ec548 / A/1。主管、负责人指派/会签、批准已完成；最后文控task1ba5f6a4-caec-11f1-a035-b082e25ec548签名显示成功，随后刷新待办仅余PDF。最终ACTIVE/受控件尚未核实，禁止再次签该task或重建文件。两页原生搜索各1/1、原生页码/画布变化、缩放旋转、水印、失焦冻结恢复、同版本重开一致已通过。
- 正式正文证据：run-20261018-151101/registered-reading-PDF.json、registered-reading-OFFICE.json、reopened-PDF.json、reopened-OFFICE.json及对应脱敏zip。Office使用真实canvas及原生搜索验证，隐藏contenteditable不是编辑能力证据；历史准备脚本旧断言尚待离线校准，不用其旧描述否定实际正文证据。
- 待完成：同链真实换版和用户/版本统计隔离；Office终态；PDF后续审批；IMAGE/TEXT/VIDEO/AUDIO入口R1624-06。整体未PASS。旧临时文件546及PDF cleanup401残留保持未清理/未确认，不冒称零残留。
- 当前窗口7份raw trace已去除网络payload/header/cookie及URL query后封存，raw文件全部移除。签名凭据输入期间未开启trace；不输出凭据。
- Root恢复后先核对新运行态，再以本人新context只读检查上述原对象，不因超时重复业务动作。

## 上一窗口历史记录


2026-10-18 当前为 `PAUSED_RUNTIME_WINDOW`。原用例尚未完成，没有整体业务PASS。按Root要求停止新增动作，3个context及browser已关闭，runner PID34492/handle59339正常退出0，确认进程不存在。runtime-pause.json active=true；恢复状态见checkpoint-live.json。

## 本轮运行与身份

- int_qms；8061 PID24564，48061 PID18204；原运行Jar072846，SHA256 `a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402`，来源Root dispatch/runtime proof。
- Config18（910473）和Reader18（910489）各自独立context本人登录、首次改密并核对页面身份。密码只存当前用户DPAPI缓存，不输出凭据；未授管理员角色。
- 所有账号、权限、组织、目录、分类、类别、项目申请/审核均走真实前端。未API/SQL/mock执行业务，未Git写、Maven、服务启停或修改生产源码。

## 原参数结果

|格式|当前结论|证据及未完成项|
|---|---|---|
|PDF|局部PASS，整项未完成|提交前两页非空canvas，翻页内容指纹不同，正文标记搜索，缩放/旋转/复原真实生效。正式file、水印版本归属、焦点计时、切版和关闭重开仍未执行。|
|OFFICE/DOCX|BLOCKED|真正DOCX的upload-preview显示30秒超时，未出现预览文件信息、未建立iframe；后台已保存临时物理文件与ticket，未绑定正式file。|
|IMAGE/PNG|BLOCKED|SOURCE前端白名单拦截，自然upload-preview POST数0。|
|TEXT/TXT|BLOCKED|SOURCE前端白名单拦截，自然upload-preview POST数0。|
|VIDEO/WEBM|BLOCKED|同一源件入口拦截、自然upload-preview POST数0；原文分页/搜索语义待明确。|
|AUDIO/WAV|BLOCKED|同一源件入口拦截、自然upload-preview POST数0；原文分页/搜索语义待明确。|

六格式均保留，未以小集合代表全量。详见run-20261018/result.json、source-ingress.json与fixture-manifest.json。

## 问题与证据

- **C24-01**：前后端SOURCE白名单同时排除PNG/TXT/媒体。预览枚举支持不等于正式源件入口可达。四项真实拦截有截图/DOM/自然请求数。Root审查非PDF最终发布、OnlyOffice转换、盖章ID及归档依赖，本线程没有单改白名单或绕过守卫。
- **C24-02**：DOCX在upload-preview返回元数据前客户端超时，trace该自然POST status=-1（未取得HTTP响应）；未进入iframe阶段。Root确认infra_file9198354931816（36812字节、正确DOCX MIME）及temporary_file546已保存，uploader910473、AVAILABLE、未绑定、15:05:13过期、cleanup ACTIVE。不能记零保存、正文成功或清理完成。页面无requestId，响应阶段根因仍待核，原DOCX未重传。
- BDD、直接源码与候选RED见issues-20261018.md。前端候选RED直接调用实际submitter及实际trimText，PNG/TXT/WAV/MP4均RED，ZIP/EXE保持拒绝，退出1；后端仅编译任务自有自检，调用原运行Jar实际策略类，同义RED退出1。不是Maven/全量构建或真实E2E通过。
- 09-pdf-body-tools.png为真实PDF局部工具证据。Office的超时原DOM在trace和脚本失败输出；10-docx-timeout-current-state.png只反映后续页面状态，不当作原错误提示截图。

## 已完成资产与恢复

保留原Viewer、专用Setup18、Root目录、TYPE文件分类、CAT自定义类别908768及私有查看/上传规则。项目DCC241016C24P申请41由Reader18审核到COMPLETED，产品目录646、关系39；既有标准模板只读复用，项目副本文件夹G44文档。组织岗位DCC241016C24Post（31）和DCC241016C24Dept已创建，两人部门/岗位保存后回读，部门负责人和作者直属主管为Reader18。

没有已创建受控file、BPM或task。正式路线仍提示阶段权限、签名授权、有效签名图片缺失共12项，未假报就绪。恢复后复用这些已成功资产，不重复创建。

## 封存与清理

当前上传页源件列表0，无需要移除的当前源件。旧PDF临时会话曾在空闲退出后cleanup HTTP401，残留未确认；另一当前PDF经真实页面移除cleanup HTTP200且列表清空，不证明旧401已清理。

DOCX546及物理文件保留，未直接API/DB清理。Root明确允许恢复后同账号/同DOCX开新真实UI上传会话，记录自然事件时间；不复用已过期ticket。当前先等Root窗口结束通知，不把类别拒绝提示部署当Office超时已修复。

四份trace已去除network payload/header/cookie与URL query并封存，另存仅path/method/status摘要：source-ingress.zip、pdf-session-expired.zip、pdf-office-preview.zip、pre-window-route-readiness.zip（均在run-20261018）。无raw.zip残留。脚本语法、本例暂停门禁及封存结构检查通过；原准备记录另存prepared-preparation-20261016.json/result-preparation-20261016.json。

## 以下为历史准备记录

下面PREPARING/prepare_only描述仅保留20261016历史，不作为当前运行态或业务结论。

E2E-24 正文工具、水印与浏览时间：PREPARING，尚未执行真实E2E。准备、前置、真实执行、Root review及清理状态必须分别记录。

## 准备结果

- 状态：`PREPARED_WAITING`。Root dispatch 的 `E2E-24.gate=prepare_only`，且 `runtimeVerified=false`、`roleFixtureVerified=false`；脚本在浏览器启动前检查 gate，当前运行会写入 `result.json` 后退出，不会启动 Chromium。
- 脚本：`doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs`。
- 静态命令：`node --check doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs`。
- 真实运行命令（仅 Root 将 gate 改为 execute 且确认环境后）：`node doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs`。
- 已执行静态结果：`node --check` 退出码 0；prepare-only 运行退出码 0，`result.json` 为 `PREPARED_WAITING` 且 `browserStarted=false`；脚本结构扫描未发现 `fetch`、`axios`、`APIRequest`、SQL、mock 或 localStorage 登录注入。

## 参数化子用例

`DCC24_FORMAT_CASES_JSON` 必须由 Root/环境注入六个且仅六个格式项：`PDF`、`OFFICE`、`IMAGE`、`TEXT`、`VIDEO`、`AUDIO`。每项含 `id`、`format`、`marker`、`bodyMarker`、`searchTerm`、`expectedPreviewKind`；PDF 另含 `minPages >= 2`。缺项、重复项、额外项或 preview kind 不匹配时，脚本在浏览器启动前写 `BLOCKED`、列出 `missing/duplicates/unexpected/kindMismatches`，并以退出码 2 结束，不能由小集合代表全格式。任一格式完整能力为 `BLOCKED` 时整体也写 `BLOCKED` 并退出码 2。

- PDF：真实 `canvas` 可见、宽高大于 0、采样到非白像素；`bodyMarker` 搜索 excerpt 必须出现；至少两页，下一页前后页码与画布指纹均变化，再返回第一页；缩放和旋转状态实际改变后复原。
- Office：OnlyOffice mount 与 iframe 可见且无错误；frame `body` 必须包含 `bodyMarker`，且没有 `contenteditable=true` 正文节点；分页/正文搜索仍以文档内真实工具为准，无法证明时该格式记 `BLOCKED`。
- 图片：`img` 已完成加载且 `naturalWidth/naturalHeight > 0`；记录图片元素截图 SHA-256 与尺寸，重开后必须一致；缩放/旋转状态实际改变后复原。
- 文本：`pre.protected-viewer-text` 可见并含任务标记；分页、正文搜索和缩放/旋转有实际页面结果。
- 音频/视频：媒体元素可见、`readyState >= 1` 且时长可读，重开后时长一致；翻页/正文搜索不作伪造验收，原需求语义仍待 Root/业务确认，因此完整格式项记 `BLOCKED`。
- 执行状态口径：Office 若仅完成 mount/iframe、缩放/旋转而无法由稳定真实 DOM 证明文档内分页/搜索，子用例记 `BLOCKED`；音频/视频同样因翻页/正文搜索语义未明确记 `BLOCKED`，不会把媒体加载成功写成整项 PASS。
- 公共断言：水印必须可见且同时包含环境注入的当前操作者标签与版本标签；失焦前读取累计秒数，失焦期间等待 1.5 秒及 2.5 秒采样，增长不得超过 1 秒误差边界，恢复焦点后等待一个心跳周期并至少增长 1 秒；关闭/重开后仍能读取服务端统计；正文不执行保存/下载写回。

## 前置缺口

1. dispatch 尚未放行执行；int_qms 8061 前端未监听，48061 为 Root 统一核对的旧外部 Jar，运行态来源未确认。
2. 尚未确认任务自有文件夹、ACTIVE 受控版本及各格式文件/正文标记；缺少时必须 `BLOCKED`，不得用旧共享文件替代。
3. 尚未确认当前登录身份具有受控浏览菜单、文件查看矩阵和正文阅读权限；需真实角色账号，不能用 admin 冒充其它角色。
4. 水印操作者/版本标签及阅读时间初始值需由执行环境提供；脚本不保存密码、token、cookie 或原始认证响应。
5. Office OnlyOffice 容器可达性、frame 正文可读性/只读事实及音视频“翻页/正文搜索”业务语义待确认；未确认项不得记 PASS。
6. trace 在登录完成后才启动，网络证据只记录 `path/method/status`，不保存完整 URL、query、token、cookie 或认证响应。

## 当前结论

静态准备不等于业务 PASS。当前结论为 `PREPARED_WAITING`，真实子用例结论、trace、截图和清理残留待 Root 放行后补录；当前 `cleanupAttempted=false`、`cleanupCompleted=false`。

- 200656封存离线校验：7个修改脚本 node --check 全部退出0；六格式整体BLOCKED、originalBusinessPassed=false；checkpoint两份SHA一致；全部历史raw.zip计数0；runner38304/49792及直接子进程均不存在。本检查仅证明脚本语法和证据结构，不是E2E业务PASS。

- 恢复窗口封存校验：6个执行/核验脚本及画布补录脚本语法检查退出0；四份当前A/1真实结果逐项PASS，像素数值与账号隔离已落盘；原Office file/BPM/task身份保持且resubmitted=false；六格式整体BLOCKED且originalBusinessPassed=false；全部raw.zip计数0，checkpoint两份一致。旧全量入口未运行，不将语法检查或辅助函数复用当整例PASS。

## 2026-10-19 R06限定方案与RED

读取最新recovery-dispatch、原manifest及Root原版本续跑证明，确认PDF/Office及4240→4253/4254精确AUTHORING/读时隔离已通过，不再次整体运行。Root最新决定仅PNG/TXT真实内容转PDF、沿既有真章/签名/受控链；旧NATIVE_SIGNED/新模式/DDL提案已明确撤回。主方案r06-png-text-pdf-scope-20261019.md列出6个生产候选文件、实际PDFBox2.0.32/ImageIO/PDType0Font复用、字体Resource缺口、严格UTF-8/像素/全文和授权/票据/最终盖章测试。

RED: node doc/tasks/20261016-dcc-e2e-24/r06-source-contract-red.cjs --png-text -> 退出1（预期），调用当前实际前端validator：PNG/TXT预期valid=true而实际false；WEBM/WAV/EXE/ZIP/SVG/HTML及空/多选仍正确拒绝。结果与源码LF哈希存run-20261019-r06-design/png-text-policy-red.json。后端缺项是静态审计，不冒称服务单测；未Maven/构建/改生产/运行真实UI。

Root reboot1019证明环境READY：8061/20976、48061/25124、120105/hash ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3，newFixesIncluded=false。环境可诊断不等于拟修代码已通过。待Root唯一生产写权与字体配置方式后先行为RED再GREEN；音视频待业务口径，不扩白名单。

- R06 RED冻结：仅3个既有类新增9个r06方法。PNG转换/原像素、TXT准确缺字体、PNG/TXT原件SOURCE ticket/hash、真实转换器+真实DccPdfStampService受控链与内容/授权负例，均只用现有生产类/方法。无新类/新字段反射、无OS字体要求。9项尚未Java/Maven执行，预期6行为FAIL/3守卫PASS仅供归因，Root实际XML为准。精确-Dtest参数、文件LF hash、6生产基线及夹具边界存run-20261019-r06-design/direct-red-freeze.json。测试冻结，等Root串行有效RED后才生产GREEN。

## R06 GREEN候选冻结（Java待Root验证）

Root e2e24-png-text-direct-red-summary.json确认9tests/6failures/0errors，准入两项、PNG转换/最终受控及TXT准确字体错误共6个有效行为FAIL。随后严格仅改获准6生产文件，新增本地PNG/TXT真实PDF转换；原件保留，后续真章/签名/归档走原链。前端node r06-source-contract-red.cjs --png-text退出0，原9格式保持、PNG/TXT通过、WEBM/WAV及危险/空/多选拒绝；历史RED文件未覆盖。

新纯转换器8项真实PDF Renderer/TextStripper/像素/中文全文测试，连同既有9项和新增TXT真实最终盖章/绑定1项，共18项Java候选。未执行Java/Maven/构建，新UI未跑。冻结10文件LF hash、方法名、精确selector及配置键见run-20261019-r06-design/green-freeze.json。Root传-Ddcc.r06.test-font=C:/Windows/Fonts/simhei.ttf运行本机测试，运行服务私有传yudao.dcc.conversion.text-font=file:C:/Windows/Fonts/simhei.ttf；无默认/自动回退、不复制字体、不声称许可或正式服务器交付已验收。

PNG16M像素前置限制、TXT4MiB解码/200页排版限制仅覆盖直接内存/排版风险，原UploadSizePolicy前置上限继续保留；达到限制准确拒绝，不截断。PNG/TXT实际内容保留160pt章区，签名绑定仍指真实盖章PDF。Finalization/Lifecycle/Query/VO/schema/view/token均未改，媒体合同待明确。

## r2恢复与Maven交接
已读repair-dispatch-20261019-r2、根AGENTS及对应docs。Root先放13 Maven，本例不并发构建。现有10个生产/测试文件LF hash全部匹配冻结值，未因重复继续重做已成功资产。18 Java候选及受影响回归仍NOT_RUN，精确selector、字体参数、文件方法及残留列表已写repair-20261019-r2-handoff.json交Root；green-freeze仅交接名，不当GREEN。PNG/TXT新包验收与原生审批预览/受控PDF工具分别覆盖；既有PDF/Office/4253/4254不重跑。媒体无正式发布/盖章/搜索分页口径，三条明确问题列于handoff。旧ticket546和旧PDF401尚无当前正式清理证明；源码仅支持当前持有session/ticket清理，不直接删infra物理文件或用新会话替代。

- r2临时资产真实UI核验：新独立admin context本人登录→基础设施→文件管理→文件列表，按DCC241016C24OFFICE/PDF路径筛选，正式列表各7/3条。Office有14:35:13原件，但可见表无物理ID、ticket、uploader或绑定状态，不能以名称/时间认定就是ticket546；PDF旧原件同样无原session/cleanup状态。未执行删除/业务POST，businessWrites=[]。通用infra删除不承担DCC票据清理，故残留保持未确认。证据run-20261019-r2-temp-1792406293126/temporary-assets-readonly.json及脱敏trace。第一次菜单只展开未进入文件列表的定位超时另存1792406172271，属于脚本入口校准，不判产品缺页。两次context/browser均关闭，runner20096退出0，无残留。

## r3 Java实际GREEN及回归（2026-10-19）

Root明确24独占Maven并撤销13等待后执行。candidate-1：18tests/2failures/0errors/0skip、exit1；纯转换器8、转换服务3和最终真实盖章绑定3均通过，两失败是UploadApi损坏PNG/非法UTF8仍断言旧扩展名消息。实际内容已经准确拒绝，按新合同只把两方法改为相同SOURCE_TYPE_INVALID code+精确PNG/UTF8原因，verifyNoInteractions(nameClaim/fileService/ticket)保持，没有削弱断言或改生产迎合。原失败XML保留。

candidate-2：18tests/0failures/0errors/0skip，Maven exit0。受影响regression-1：60tests/0failures/0errors/0skip，Maven exit0（转换器8、完整OnlyOffice服务7、完整UploadApi40、Finalization受影响5）。真实PNG像素/Renderer比例/章区、中文长行多页TextStripper全文/字体嵌入、UTF8/字体资源/缺字形/损坏/上限、真实盖章PDF存储及绑定目标均实际通过。只证明单元/相关回归，不是新运行态完整页面PASS。

证据run-20261019-r3-java/{candidate-1,candidate-2,regression-1}/process.json、summary.json及各4类XML；精确命令保存于process，测试字体显式-Ddcc.r06.test-font=C:/Windows/Fonts/simhei.ttf。本机字体未复制，不声称正式服务器交付/许可验收。Maven -q成功日志可空，以真实退出码与XML为准。前端node r06-source-contract-red.cjs --png-text再次exit0，PNG/TXT及旧9类通过、WEBM/WAV和危险/空/多选拒绝。

已只读确认无classworlds/surefire进程，Maven窗口释放并通知Root。当前六生产文件没有追加改动，Query/Lifecycle/VO/schema/view/token不动。repair-20261019-r3-handoff.json记录准确文件hash、方法、命令、各轮结果及新包依赖。下一步Root打包核实际classes/策略并私有配置text-font Resource，返回新PID/hash/health/真实登录证明后，才执行PNG/TXT完整页面链。PDF/Office与4253/4254及546/旧401只读核验不重复；媒体规则具体问题仍待决策，整例未PASS。


## R4 运行包门禁复核

R4复核：R3交接10个源码/测试LF哈希全部匹配，18项候选和60项回归保持GREEN；不重跑Maven或已通过PDF/Office/4253/4254。Root最新reboot1019-runtime-proof.json仍指向旧workflow-v6-20261019-120105.jar，SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3，newFixesIncluded=false，不能验收新PNG/TXT。等待Root新PID/Jar SHA、实际修复class、显式私有字体Resource、health与真实登录证明后，仅执行PNG/TXT原文至审批签名盖章发布的真实页面链。本轮未启动浏览器、无业务写入。媒体三项合同待决；ticket546和旧PDF cleanup401仍UNKNOWN，不猜删。详见repair-20261019-r4-handoff.json。

较早green-freeze中的UploadApi测试哈希早于R3两条拒绝消息断言修正；R3最终hash及candidate-2/regression-1为当前冻结依据，历史证据不改写。新页面业务动作只能由Playwright真实页面完成，API/mock不能代替；新包前不宣称E2E GREEN。


## 2026-10-20 缺失格式当前验证

DOC4269 A/1正式阅读子链PASS；XLS4270/XLSX4271发布缺第一张正文表FAIL。最小布局及缓存key修复有效RED后15项定向GREEN，源码/测试冻结；真实修复尚待新包后继版本验收。准确UI证据、RED/GREEN XML、4份源码/2份class SHA和离线A2派生/脚本准备统一见 `run-missing-formats-1792434821434/repair-handoff.json`。后继业务UI未启动，旧发布件未改；等Root合并13冻结与10新包放行。整例blocked，非E2E PASS。


## 2026-10-20 XLS A2 实际链与维护安全点

- Root放行新运行包及E13冻结前端后，实际命令 `$env:DCC24_MISSING_FORMAT='xls'; node doc/tasks/20261016-dcc-e2e-24/ui-session.cjs`，session41149/PID21508；run-missing-formats-1792437223338。分支int_qms，8061/48061、PID20976/27908。
- 单次检出/上传/检入：4270/A1→4272草稿；实际回执草稿ID、原生两表A1新标记均核对。原版仍是旧正文。检入说明textarea定位校准后沿原未提交弹窗继续，未重复写入。
- 正式局部变更4273/A2，BPM 7cb46ff6-cbf2-11f1-a8a5-b082e25ec548；主管、部门负责人指派、实际会签、批准及文控5签名实际完成。新发布PDF两页分别命中DCC24 XLS A2 FIRST SHEET/SECOND SHEET；翻页、搜索、缩放/旋转/复原、当前人水印PASS。
- Reader18真实失焦12秒，51→62（停止）→62（失焦）→62（恢复）→72；失焦heartbeat0。关闭重开75秒，画布指纹相同、水印更新，非阅读写0。证据published-XLS-DCC241016C24Reader18.json。
- 首轮复查搜索停第2页，旧assertPdf要求当前第1页，保留page2-script-calibration.json/png；通过真实页码回1校准任务脚本后只读重验成功，不计产品失败。历史详情使用错误header锚点及storage入口误找操作面板两次超时也保留，不作为产品失败。
- 申请人提交成功后BPM辅助GET /admin-api/bpm/task/list-by-process-instance-id，两次HTTP200/code403/msg没有该操作权限；原trace提取仅code/msg并脱敏。独立于签名前阅读停止→approve-task证据，不扩大角色、不声称所有签名分支覆盖。
- 4270作废后DAILY入口自然GET HTTP200/code1080000012拒绝，符合期望。真实追溯最终加载旧A1/旧BPM/5签名；无正文按钮，历史正文及计时隔离尚未执行。不得用初始只读空壳或定位超时作产品失败，也不得将旧版阅读计为PASS。
- Root因窄前端维护要求安全点暂停：全部trace已停止并脱敏、raw移除，browserClosed=true，runner exit0且PID21508不存在。运行服务PID27908/20976仍在，未改生产/构建/重启。XLSX4271没有spreadsheetRepair attempt。execution-checkpoint.json与trace-seal.json已封存。待Root重新放行后从真实管理版历史正文入口补缺失项，再继续XLSX原4271单次后继；禁止重提重签。
- CAD后继来源边界纠正：可采用同扩展名另一真实模型及其精确配套PDF，无须同Git路径历史。Root提供DWG两候选仓库及pano Control Horns直接配对；未下载/未执行CAD。SLDDRW固定树仅一件，不能假称第二件。


## 2026-10-20 XLS/XLSX successor verification and safe pause

XLS4270/4273 history isolation PASS: exact natural stop totals equal reopened UI18/36s. XLSX4271 single checkin produced working4274, then formal4275 A/2 (BPM c9feefe3-cbf5-11f1-a8a5-b082e25ec548). Two native sheets and A1 cell markers verified before submission; actual five signatures1030..1034 completed and formal A/2 ACTIVE. Published PDF has2 pages and both A2 sheet markers; actual tools/watermark/focus/reopen PASS. During12s unfocused interval count45 remained45 with zero heartbeat; resumed45 then56. Old4271/new4275 history bodies and renewed actor watermarks remain distinct; natural final stop totals26/16s exactly match respective reopened UI counters. Non-reading writes0 on read-only published path.

Submit applicant boundary: actual permission explanation visible, generic BPM requests0 and HTTP/business403=0; completed DCC round shows five actual nodes and VALID signatures. ACTIVE detail separately reproduces misleading perpetual verification notice: formalRead early return leaves scope undefined while approvalLoading=false; this is a pending presentation defect, not a claimed pass. Root instructed safe pause before DOC/CAD for13 existing-test RED and later approved one-line template GREEN. No production edit/retest/re-sign performed here.

Evidence: run-missing-formats-1792438891279/maintenance-checkpoint.json, XLS-history-natural-evidence.json, XLSX-history-natural-evidence.json, XLSX-history-isolation-audit.json, XLSX-A2-author-natural-evidence.json, published-XLSX-DCC241016C24Reader18.json; four traces CRC-checked and raw traces removed after whitelisted natural evidence extraction. All browser contexts closed; browserClosed=true; runner40864 exited0. Resume only after Root release, first read-only existing4275 detail; remaining DOC/CAD gaps stay open. Historical pre-fix contract evidence and failed script calibrations retained.


## 2026-10-20 safe-window CAD source preparation

While browser contexts remained0, downloaded only Root exact candidates at pinned commits;12 upstream native/PDF/README files matched Git blob IDs and byte counts. Gear284bb5728edf9a63b758886d5946213540759d25 supplies independent SLDASM/SLDDRW successors with exact3-page engineering PDF (40T,20T,FINAL ASSEMBLY/FA-001). Pano Control Horns native/PDF supplies visibly different SLDPRT successor. DWG house repository71a08f23630c4c81d5055cfb5434729b3844028c and floorplan/elevation5bdb0e47140f8bbbc59d630513971cbb11409cbb supply publisher-paired baseline/successor. Poppler renders reviewed; PDF body markers/page counts in CAD-offline-pdf-validation.json and full pairs in CAD-pair-readiness.json. Existing baselines not redownloaded or regenerated.

Provenance updated with source URLs/commits/bytes/hashes and declared license status: Pano MIT; gear and both DWG repositories have no declared license in metadata, no license inferred. SolidWorks geometry/dependency resolution not performed; unchanged author native bytes and author pairing evidence only. Default system Python lacked pypdf; configured bundled Python parsed/rendered successfully; one source PDF reports duplicate PageMode upstream, render succeeds without rewriting original. Offline readiness is not DCC UI PASS. No browser reopened, business writes0, no production edits.
