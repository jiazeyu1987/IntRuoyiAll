# Execution Log

- 2026-10-20 范围续记：用户确认音频、视频只作为附件，取代本日志旧记录中的E24媒体正文准入/翻页/搜索待定项。E24正文整例PASS由现有已验正文证据及七格式续验收构成；不代表WAV/WEBM附件专项PASS。E23权限规则适用于音视频附件。静态UI核对未见直接准入冲突：正文源件选择器不含音视频扩展名，普通附件选择器未设前端扩展名白名单；服务端媒体格式准入与媒体专项权限仍未验证。仅同步文档，未运行E2E/测试/服务/业务动作；任务留给Root review。
- 2026-10-16 Root创建本例记录；用户授权每个E2E一个实际线程，Root统一启动和review。当前仅准备，尚无真实E2E执行结论。
- 2026-10-16 准备核对：分支 `int_qms`；目标前端 `http://127.0.0.1:8061`，后端 `48061`。dispatch 中 E2E-24 为 `prepare_only`、runtimeVerified=false、roleFixtureVerified=false，因此按公共约定未启动浏览器。
- 2026-10-16 页面入口核对：登录后必须从可见“受控浏览”菜单进入 `/dcc/controlled-file/browser`，再按任务自有文件标识定位行并点击“预览”；详情 viewer 使用 `dcc-controlled-preview-file-pane` / `protected-viewer-shell`。
- 2026-10-16 格式参数拆分：PDF（canvas 宽高及非白像素、页码、正文搜索、缩放/旋转）；Office（OnlyOffice mount/iframe 正文 ready，工具由文档内提供）；IMAGE（naturalWidth/naturalHeight、缩放/旋转）；TEXT（pre 正文标记、分页、正文搜索、缩放/旋转）；VIDEO/AUDIO（媒体 readyState/duration、水印，翻页/正文搜索语义待明确）。
- 2026-10-16 静态验证计划：`node --check doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs`；真实执行命令待 Root 放行：`node doc/tasks/20261016-dcc-e2e-24/dcc-e2e-24-real.e2e.cjs`（从仓库根目录运行）。
- 2026-10-16 静态检查结果：`node --check` 退出码 0；脚本结构扫描无 `fetch`/`axios`/`APIRequest`/`SQL`/`mock`/localStorage 登录注入；`prepared.json` 结构检查 PASS；真实脚本在当前 gate 下退出码 0，`result.json` 为 `PREPARED_WAITING` 且 `browserStarted=false`。这不是业务 PASS。
- 2026-10-16 最终准备复核：popup 入口、PDF 非空 canvas、Office/图片/文本/媒体正文就绪、水印身份、阅读累计时间、失焦恢复、关闭重开和非阅读业务写请求为 0 的断言已写入脚本；Office 文档内分页/搜索及音视频翻页/正文搜索保持待明确，执行时只能记 BLOCKED，不能伪造 PASS。
- 2026-10-16 Root review 返修：格式输入现在必须精确覆盖六项 `PDF/OFFICE/IMAGE/TEXT/VIDEO/AUDIO`，缺项/重复/额外/类型错配在浏览器启动前写 `BLOCKED` 并退出码 2；PDF 增加正文标记、两页实际画布指纹变化和搜索正文标记；Office 增加 frame body 正文标记及 `contenteditable=true` 只读断言；重开按正文标记/图片指纹/媒体时长复核；失焦计时用 1 秒误差边界验证冻结，恢复后要求至少增长 1 秒；网络响应证据仅保存 pathname、method、status，trace 在登录完成后启动，避免登录凭据进入 trace。
- 2026-10-16 返修补充：任一格式产生 `BLOCKED` 时整体结果退出码为 2；PDF 下一页使用第二页 canvas 指纹比较，避免仅比较第一页 canvas。
- 2026-10-16 返修验证：`node --check` 退出码 0；prepare-only 脚本退出码 0，`result.json` 仍为 `PREPARED_WAITING` 且 `browserStarted=false`；敏感证据扫描、脚本合同扫描、任务文档合同扫描均 PASS。
- 2026-10-18 按最新授权恢复真实UI：int_qms；8061 PID24564、48061 PID18204，运行包 `backend-workflow-v6-20261018-072846.jar` SHA256 `a27b8d660697531b920a4728cb9f2d30fd55c1afd0cf40d131f7eb4e7523d402`，来源 Root dispatch。共享 roleFixtureVerified=false 不作为本例阻塞。
- 2026-10-18 原Reader内存凭据随旧进程退出且无安全缓存，未登录猜测/重置；保留旧账号记录。新建 Config18/Reader18 并通过各自独立context首次改密及登录，密码仅DPAPI当前用户加密缓存，所有配置及项目申请/审核走真实页面。
- 2026-10-18 复用专用Viewer角色；创建专用Setup18、Root目录、TYPE分类、CAT自定义类别及私有类别权限。项目 `DCC241016C24P` 由Config18申请、Reader18（精确项目审核员角色）审核，页面终态COMPLETED。目录模板只读引用既有标准模板，不改模板及公共矩阵。
- 2026-10-18 C24-01真实RED：PNG/TXT/WEBM/WAV 各被正式源件入口前端类型校验拒绝，自然upload-preview POST均0；截图、source-ingress.json及脱敏trace留在run-20261018。原图片/文本明确要求未通过；媒体分页/搜索语义仍未知。直接前后端白名单与最终发布依赖由Root审查，本线程未改生产。
- 2026-10-18 候选策略RED：实际前端submitter校验函数、当前运行Jar实际后端策略类均拒PNG/TXT/WAV/MP4（退出码1）；ZIP/EXE仍拒绝。Java仅编译任务自有定向自检类，未Maven/全量构建；不当作真实E2E。
- 2026-10-18 PDF提交前预览局部PASS：非空两页canvas、翻页画布差异、正文标记搜索、缩放及旋转/复原真实效果；见09-pdf-body-tools.png与manifest。未把提交前预览代替正式正文/阅读计时。DOCX正在继续。
- 2026-10-18 一次真实UI空闲退出使旧PDF临时上传session-cleanup返回401，保存08-preview-session-expired.png及pdf-session-expired.zip；该临时会话残留未确认。另一次页面正常移除源件的session-cleanup为200且文件列表清空，不将HTTP200等同于旧401会话已经清理。
- 2026-10-18 当前runner PID34492，exec进程handle59339；精确恢复状态见checkpoint-live.json及fixture-manifest.json。旧prepared/result保留历史，不覆盖为业务PASS。
- 2026-10-18 本例组织关系已由真实用户管理页保存并回读：Config18/Reader18均归属DCC241016C24Dept、岗位DCC241016C24Post（组织角色ID31），部门负责人及作者直属主管均为Reader18。正式审批路线仍准确提示阶段权限、电子签名授权与有效签名图片缺失，未绕过此门禁。
- 2026-10-18 Root更新窗口：停止新增业务动作；当前源件列表0，无受控file/BPM/task已创建。3个context/browser全关闭；runner PID34492正常退出码0，经Get-Process确认不存在。runtime-pause.json active=true；共享dispatch未改。全部trace已脱敏封存，无raw.zip残留。
- 2026-10-18 Root只读确认DOCX已保存物理文件9198354931816及临时票据546（uploader910473，AVAILABLE，未绑定业务，15:05:13已过期，cleanup ACTIVE）。原失败保留，未重传或API/SQL清理。Root允许恢复后同一账号/同一DOCX开新UI上传会话复验；不能复用过期票据、不能把类别提示部署当Office已修复。
- 2026-10-18 封存脚本语法及本例runtime pause读取自检PASS；此后未启动浏览器。当前工程状态in_progress/PAUSED_RUNTIME_WINDOW，原E2E24没有整体PASS。

## 2026-10-18 第二次运行窗口暂停（最新）

- 已明确业务静止；Root统一更新48061前停止全部新增业务动作。runner PID26968（handle22110）退出码0；本例browser PID29828父进程26968及已记录子进程均消失，其他浏览器未操作。全部context关闭，checkpoint-live.json与checkpoint.json一致，runtime-pause.json active=true。
- 本窗口运行Jar151101，SHA256 bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f；旧窗口历史证据保留。
- PDF：file2054545668044084235 / BPM fb7fb798-cad2-11f1-a035-b082e25ec548 / task fb802be5-cad2-11f1-a035-b082e25ec548 / A/1；待直属主管审核，未签名。实际两页非空画布、正文搜索、缩放旋转、水印、真实失焦12秒冻结及恢复、同版本关闭重开源画布一致已逐项通过。
- Office：file2054545668044084240 / BPM8477da2d-cae3-11f1-a035-b082e25ec548 / A/1。主管、负责人指派/会签、批准已完成；最后文控task1ba5f6a4-caec-11f1-a035-b082e25ec548签名显示成功，随后刷新待办仅余PDF。最终ACTIVE/受控件尚未核实，禁止再次签该task或重建文件。两页原生搜索各1/1、原生页码/画布变化、缩放旋转、水印、失焦冻结恢复、同版本重开一致已通过。
- 正式正文证据：run-20261018-151101/registered-reading-PDF.json、registered-reading-OFFICE.json、reopened-PDF.json、reopened-OFFICE.json及对应脱敏zip。Office使用真实canvas及原生搜索验证，隐藏contenteditable不是编辑能力证据；历史准备脚本旧断言尚待离线校准，不用其旧描述否定实际正文证据。
- 待完成：同链真实换版和用户/版本统计隔离；Office终态；PDF后续审批；IMAGE/TEXT/VIDEO/AUDIO入口R1624-06。整体未PASS。旧临时文件546及PDF cleanup401残留保持未清理/未确认，不冒称零残留。
- 当前窗口7份raw trace已去除网络payload/header/cookie及URL query后封存，raw文件全部移除。签名凭据输入期间未开启trace；不输出凭据。
- Root恢复后先核对新运行态，再以本人新context只读检查上述原对象，不因超时重复业务动作。


BDD: same-chain version switch -> Given original Office A/1 ACTIVE; When author checks out original file, uploads distinct A2 source, checks in A/2 and selects A/1/A/2 through UI; Then visible body markers and actor/version reading summaries match selected version, and A/1 source remains unchanged.

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


- 200656封存离线校验：7个修改脚本 node --check 全部退出0；六格式整体BLOCKED、originalBusinessPassed=false；checkpoint两份SHA一致；全部历史raw.zip计数0；runner38304/49792及直接子进程均不存在。本检查仅证明脚本语法和证据结构，不是E2E业务PASS。

## 2026-10-18 22:54 Root运行态归因与暂停（最新）

Root明确核实48061/PID34440已退出且连接拒绝。此前观察到的登录失败、业务500当前归类为环境阻塞，不能据此认定账号权限或阅读计时代码缺陷；原HTTP500/页面报错证据保持原样。Root将恢复同一200656已验证Jar，不加载新源码，本例不改服务、代码、DB或权限。

本例已关闭的context/browser/runner保持关闭，49792退出0；再次核对38304/49792及直接子进程均不存在。runtime-pause.json active=true，checkpoint两份同步。原Office/PDF A/1已生效成功事实保留；Office检出/源件上传及唯一一次checkin未确认状态保留，不撤销、不重建、不重复提交。项目VIEW未保存，当前不扩权。

恢复条件为Root明确健康和真实UI登录放行；届时先用新独立context真实登录、只读核对原file/BPM/task/版本和原checkin结果，再决定后续动作。当前不以旧运行证明或源码候选替代新运行态验证。


- 225739恢复窗口：同包SHA/PID14864只读核对成功；本人新context沿正式租户输入/选项登录。Office正式全部版本按编号查询仅A/1共1条，未重复checkin或签名。本项目304仅新增Reader18 VIEW并真实PUT/GET回读，原Config18 OWNER保持；未重写目录规则。证据在run-20261018-225739-recovery。

## 2026-10-18 22:57恢复窗口最终核对（最新）

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

## R5 放行前新运行包门禁（历史，已被后续新包执行替代）

已读取R5派发、根AGENTS、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`及`docs/worktree-restrictions.md`。当前分支`int_qms`。R5最新已知Root proof仍为R4所核`reboot1019-runtime-proof.json`：旧`backend-workflow-v6-20261019-120105.jar`、SHA `ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3`、`newFixesIncluded=false`。派发目录没有更新的E2E-24新包proof；因此按门禁不启动浏览器、不操作业务、不重跑Maven。

本轮证据分类：有效行为RED 9项/6失败保留；后端候选18/18与受影响回归60/60 GREEN保留，0 failure/error/skip且exit 0；PNG/TXT前端策略GREEN保留。真实PNG/TXT E2E未运行。原PDF/Office与4240→4253→4254通过证据保留且不重跑。WEBM/WAV三个业务合同问题待决；TEMP546、旧PDF cleanup401继续UNKNOWN，不重复搜索或猜测删除。详见`repair-20261019-r5-handoff.json`。

等待Root证明新Jar包含converter、upload与finalization路径，显式配置字体Resource，并提供PID/Jar SHA/health/真实UI登录proof后，才通过Playwright真实页面执行PNG/TXT源上传、实际原文渲染（中文与长行）、审批、文控签章、发布/存储绑定及回读，核对原件不变、非空PDF画布、正文搜索、状态、水印与浏览时间。API/mock不得替代被验收动作。


## R4 运行包门禁复核

R4复核：R3交接10个源码/测试LF哈希全部匹配，18项候选和60项回归保持GREEN；不重跑Maven或已通过PDF/Office/4253/4254。Root最新reboot1019-runtime-proof.json仍指向旧workflow-v6-20261019-120105.jar，SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3，newFixesIncluded=false，不能验收新PNG/TXT。等待Root新PID/Jar SHA、实际修复class、显式私有字体Resource、health与真实登录证明后，仅执行PNG/TXT原文至审批签名盖章发布的真实页面链。本轮未启动浏览器、无业务写入。媒体三项合同待决；ticket546和旧PDF cleanup401仍UNKNOWN，不猜删。详见repair-20261019-r4-handoff.json。

较早green-freeze中的UploadApi测试哈希早于R3两条拒绝消息断言修正；R3最终hash及candidate-2/regression-1为当前冻结依据，历史证据不改写。新页面业务动作只能由Playwright真实页面完成，API/mock不能代替；新包前不宣称E2E GREEN。

## R5新包真实UI执行及本轮封存（最终）

新包门禁已解除：Root r5-runtime-proof.json，8061/48061、PID19844、Jar backend-workflow-v6-20261019-230954.jar、SHA256 3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c，59类/health/真实登录/显式字体Resource核验齐全。本轮仅Playwright真实前端复用原Config18与Reader18及原项目/分类/目录/组织/签名配置，未调用业务API/SQL或mock，未改生产源码、未构建/重启。

PNG2054545668044084262、TXT2054545668044084263完成真实源上传预览、提交、主管签名、同部门指派和会签、批准文件负责人及文控签名，最终ACTIVE A/1。TXT原夹具不含中文，另建fixtures/r5/DCC241016C24TEXT.txt，不覆盖旧夹具；4239字、1365字长行、末标记R5END。正式PDF分别非空685×969、红章可见，TXT首段/长行/第4页末尾可搜索；PNG纯图片无文本层，不将搜索写PASS、不做OCR。

实际headed失焦12秒：PNG414→414、隐藏心跳0、恢复424、重开425；TXT10→10、隐藏心跳0、恢复21、重开22。当前Reader18/A1水印及重开新水印、缩放旋转复原通过。自然网络监听只记录path/method/status，两个最终viewer运行均非阅读业务写0。两份最终管理页均master A/1、发布件可用、盖章件可用、5项签名已校验，批准负责人Config18，证据final-text/final-binding-source.json。

原件边界：本地全SHA未变，服务器页面源件哈希前12位0b27bfa1b012/083e49d89d05与上传前一致；类别下载策略CATEGORY_DOWNLOAD_DENIED，未下载服务器原件做全字节比对，不扩权限、不使用API补验，不宣称该项完成。WEBM/WAV三项合同仍待定；旧TEMP546和旧PDF cleanup401仍UNKNOWN，不重复检索/猜删。已发布任务自有文件保留作验收证据。

工具失败不冒充业务失败：保留headless失焦FAIL；headed实际聚焦/失焦通过。PNG前序viewer已通过，后续旧detail-header-shell选择器找不到造成FAIL，原报告原样保留；最终管理页补核签名和发布落位通过，不重跑已通过PNG viewer。TXT最终只读run为PASS。最终TXT viewer截图已视觉核对清晰中文和红章；初始canvas截图有瞬时遮罩，作为历史证据保留。

验证与封存：5份r5-*.cjs经node --check全exit0；r5-seal-results.py实际assert、6份ZIP CRC、无network payload及raw.zip残留检查PASS。全部浏览器关闭，交互runner exit0，按命令行核对r5-ui-session进程0。6份trace均脱敏保留；签名凭据填写阶段未开启trace。脚本已进入Cleanup Keep；r5-ui-session.cjs受doc/tasks CJS忽略规则影响，未来获准提交时需要git add -f，本轮不暂存。

收尾：task Current Status=blocked；cleanup preview实际exit3，提示“task status is blocked; expected ready_for_closeout or completed”；未执行apply，未称清理PASS。尚有原件字节/媒体验收与Git授权边界，不设ready_for_closeout或completed。git只读核对分支int_qms，现有ahead2保持，无提交/推送。PDF/Office/4240→4253→4254和Java18/60保留不重跑。

当前结果：PNG_TXT_UI_CHAIN_PASS_SOURCE_BYTES_UNVERIFIED_MEDIA_PENDING。repair-20261019-r5-handoff.json、result.json、fixture-manifest.json与r5-current-run.json已更新，汇总run-r5-pngtxt-1792422918342/r5-png-txt-summary.json；旧“等待Root新包”只作放行前历史。

## R5原件全字节补核及Root重启安全点

本轮授权仅补原件全字节；已通过PNG/TXT主链、PDF/Office、Java18/60均不重跑。按原Config18真实页面核对类别908768完整3条规则（上传/查看/查看，无DOWNLOAD），两原文件2054545668044084262/4263 A/1精确详情及源哈希前缀一致；正式页面无原件下载入口。静态完整链确认现有下载固定publishedFileId，而非sourceFileId，不能通过增加DOWNLOAD获得PNG/TXT原件。未增加权限或产品功能，未执行下载，不以发布PDF冒充原件。详见source-download-boundary-20261020.md。

类别原规则前后UI完整字段完全相等；目录初读两行后完整回读遇3行，未形成稳定全快照相等证据，此项记UNCONFIRMED，未修改任何规则、无恢复操作。原英文抽屉按钮选择器/交互语法/目录旧条数失败保留，不转写产品结论。Root通知准备13新包后立即停止只读核对并关闭浏览器，交互runner exit0、r5-ui-session进程0；safeRestartPoint=true，DCC非GET请求0、权限变更0、下载0。已到安全重启点，未自行重启服务或发送外部线程消息。

run-r5-source-only保留两对象身份/权限/详情、配置前后快照、source-only-result.json与2份脱敏trace；无raw.zip，ZIP CRC/无network payload、对象精确身份、零写入、类别相等及文档结构assert全PASS；r5-source-readback.cjs node --check exit0。result/handoff/current-run补入sourceByteFollowup，原通过结果不变。原件全字节仍NOT_VERIFIED_NO_FORMAL_SOURCE_DOWNLOAD_ENTRY，WEBM/WAV仍未验，546/401未触碰，整例blocked。

## 2026-10-19 R6 剩余范围审查与定向分页
先审查task/report及原合同并记录remaining-coverage-review-20261019.md。唯一可执行缺口为原TXT4263的上一页/下一页；Given原ACTIVE A/1四页TXT与Reader18，When真实UI下一页再上一页，Then页码、滚动位置、不同页画布及返回第一页不变均成立。其余原件入口/源件工具与媒体合同列范围阻塞；无新证实产品缺陷。最新监督进一步限定只补上/下一页，页码跳转也不执行。分支int_qms，8061 PID20976，48061 PID22076，R6 Jar SHA478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaF949f24a核对一致；不构建、不重启、不改生产、不Maven。

R6最小复测实际PASS：仅原TXT4263上一页/下一页，页码1/4->2/4->1/4、scrollTop 0->1285->222，第一页130002、第二页60949非白像素；返回第一页SHA严格一致，第二页不同，首屏上一页禁用。三截图与脱敏trace保存，ZIP CRC和无network payload/raw校验PASS。浏览器closed，runner close exit0且无进程；非阅读业务写0、权限/生产/构建/Maven/重启/Git均0。更新task/report/result及R6交接，修正PDF独立切版证据口径，保留所有历史PASS和原始FAIL。无新增明确产品缺陷可修；原件/原生工具入口、媒体语义与旧546/401未知保持blocked，不执行cleanup apply或宣称整例completed。按监督要求最小复测及报告后结束。


## 2026-10-20 原合同证据复核（Asia/Shanghai）

按用户要求仅离线复核R5/R6保存证据并更新当前门禁及handoff，不重跑已通过格式/UI链/TXT分页。已读原E2E-24合同、task-closeout-rules及e2e-rules，分支int_qms。取消将新增原件下载/服务器全字节比对作为验收门槛；审查现有hash、正文重开和无保存操作证据。保留历史原始记录；WEBM/WAV待业务澄清，546/401保持UNKNOWN。先从原脱敏trace提取重开前后可见帧（离线派生证据），不启动浏览器、不构建/重启/写业务或Git。


### 2026-10-20 原合同复核收口

已完成离线证据审查并同步task、verification-report、result和当前R6 handoff，新增contract-evidence-review-20261020.md/json。两份重开正文帧均从既有trace原样提取，没有新浏览器/页面动作。上传前后本地SHA、页面源哈希前缀、重开原A/1正文和无编辑保存操作/非阅读业务写0共同支持原合同的内容不变及只读断言；服务器全字节未读取且不是门禁，不新增下载入口。

结构验证PASS：97份历史证据SHA未改变，9份既有ZIP CRC正常，2个派生帧与原资源相同，当前handoff/result分类一致，UTF-8/路径/任务必需章节及Cleanup Keep正确；定向git diff --check exit0。没有重跑E2E或Java测试，历史PASS/FAIL保持。WEBM/WAV未验证且待具体业务合同决定；546/401为UNKNOWN_UNTOUCHED，不清理。分支int_qms，read-only status显示既有ahead 2，本轮未提交或推送。

收到Root收口指令后结束当前轮；不追加浏览器、源件下载、全字节工作、测试、功能、权限或清理。任务整体保留blocked，不宣称全格式PASS或completed。当前正式分类与边界以contract-evidence-review-20261020.md及repair-20261019-r6-handoff.json为准。


## 2026-10-20 缺失正式格式扩展入口与执行放行

BDD 见 task.md 新增七扩展名场景。RED：旧六类别 parseCases 适配入口运行显式扩展名选择测试退出1，拒绝 DOC/XLS 等独立扩展选择。GREEN：`node --test doc/tasks/20261016-dcc-e2e-24/missing-formats.test.cjs` 5项通过（选择范围、真实夹具、配对PDF、哈希、放行门禁）；此为脚本单测，不是业务E2E。初始模块缺失仅记脚手架错误，不计业务RED。

用户新授权生效；E2E10 新证明 SHA d7de2891ceb94e5c4222e6cb9104d6b2d952ca43f9c1758d037167aa70cbcffc；实际 Jar SHA 相同，8061 PID20976，48061 PID30476。保留原账号/目录/审批路线。Office后继表格离线真实内容为E5=15（不是占位字符串）；DOC后继为真实表格正文。DWG缺配对PDF、三种CAD缺第二工程版本，保持未验。不得将离线解析算UI通过。


### 2026-10-20 缺失格式续验、表格最小TDD与后继准备

真实UI：DOC4269完成上传、五项签名及发布阅读，正文/适用工具/水印/12秒失焦冻结及恢复/重开通过，切版未验；XLS4270/XLSX4271原生两表有正文，但发布PDF均遗漏第一张表，旧失败件及截图保留。浏览器关闭、runner18156退出，7份trace脱敏封存。签名后accessDenied因果不足，不改权限或13线程前端。

获Root明确授权的独占定向TDD：客户端RED 4项2失败；补缓存key RED 8项4失败，均业务断言失败、无编译错误。仅客户端JWT前xls/xlsx加空布局，转换服务rawKey仅表格加固定规则后缀；GREEN客户端4+完整转换服务11=15项/0失败/0错误/0跳过，进程exit0，无Maven残留，已释放窗口。源码/测试冻结，准确SHA和XML/命令见 run-missing-formats-1792434821434/repair-handoff.json；不是新包E2E PASS。

依Root追加授权，以现有POI HSSF/openpyxl从原SampleSS/A1生成合法A2副本，仅两表A1新标记；同库回读两标记、活动表及打印设置一致，其他单元值不变，原件哈希不变。provenance标为派生件并记录父/新hash。单表SimpleMultiCell准备件不用于此次修复验收。spreadsheet-successor.cjs仅准备原4270/4271一次检入和重验，所有写动作前核新包放行，尝试后禁止盲目重复；发布帮助器支持准确后继record/version。语法/5项既有离线合同/导入无浏览器门禁通过。尚未启动新业务浏览器；等13定向冻结与10新包证明由Root合并放行。task保持blocked，CAD/必要切版/媒体合同缺口保留，无提交推送/cleanup apply。


### 2026-10-20 新包身份及剩余前置离线交接

新runtime-proof PID27908/48061、8061/PID20976和Jar ee67ec8f…实哈希匹配；从BOOT-INF/lib内DCC模块读取两转换class，与冻结hash一致；4份后端源码/测试及23份源/派生夹具hash一致，A2引用有效。仅核验证据，不启动业务浏览器。prepared/manifest旧放行指向d7de旧包，已保留previousExecutionRelease并替换为新proof、authorized=false，等13冻结与Root明确UI放行；不把10线程登录账号用作本例业务账号。

DOC4269不同正文同名next夹具已齐，旧初始提交状态字段过期不能触发重提；DOC适配边界及四CAD逐项前置统一写入原repair-handoff.json的remainingFormatPrerequisites。仅定点查询既选上游固定tree和三条同图纸路径历史：LibreDWG无circle.pdf，唯一PDF为无关差异报告；三SW路径各仅一次初始提交，无对应第二内容版本。不同DWG格式版/不同投影PDF不能冒充不同正文；未补得合法准确配对，不下载/重复生成已有夹具，不拓宽准入。CAD首链3组可待授权执行，但单页载体须fixture-specific inspectBody，不能套两页DOCX断言。无生产/构建/服务/DB/API写入。完成准备后直接交Root，不循环等待。


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

Root重新放行：green-handoff.json（20261020-formal-progress-waiting-red），四文件SHA全部匹配；78/78及双SFC为13既有证据，本例不重跑。现只读复验4275后继续DOC/CAD，生产冻结、服务不重启。

2026-10-20 CAD fixture entry BDD: Given pinned genuine CAD source/PDF pairs and unchanged task-owned same filenames; When selecting either generation; Then every source/PDF hash must match provenance and swapped/missing successor PDF is rejected before UI. RED node --test missing-formats.test.cjs: 4/5, DWG lacks paired PDF. GREEN after minimal existing config/check extension: 5/5. Production untouched. DOC first body/reopen audit has no captured stop response on direct popup close; do not claim exact stop isolation yet; add focus-stop UI checkpoint before final PASS.

2026-10-20 runner calibration: 63797 normal close exit0. Read-only95385 stopped exit1 after missing reading-summary locator; no business writes and no exact isolation PASS. Wrong guessed run path was a local read error only. Unique current runner35329, run1792440609947; DWG4277 created once with house.dwg/House Plan.pdf, submitted BPM3ce14fc0-cbf9-11f1-a8a5-b082e25ec548. Four approval tasks plus department assignment signed through actual Reader18 UI; published checks pending. Fixture GREEN5/5 does not imply E2E PASS.

2026-10-20 Root原合同校准：DWG两次preview HTTP200/209939bytes/SHA256均7e8d3ff38fd577d0c634519813316ff8ec6fed1cce050b081cf27a60f2cbf903；同A1/第1页/685x969/100%0度/Car Porch且非阅读写0。原canvas严格相等FAIL保留、原因未证实，不增加像素编码门槛。BDD Given同版本自然PDF响应 When重开 Then精确byte身份+版本+正文可视+无编辑写确认原合同；只在四CAD分支校准已有verify-published-body，旧Office/PDF行为保持。RED扩展既有missing-formats.test的两响应身份断言5/6；GREEN6/6（含零响应/单响应/403/空字节/错hash拒绝），未改生产。有限render-audit因已缓存模块缺snapshot导出而停止；无业务写入，已捕获可视一致，不重复查字体/引擎，未保留无用snapshot新增。

2026-10-20 DWG4278 successor: early UI click rejected with 请等待文件上传完成; original trace confirms checkout+2upload-preview only, checkin POST0. After both upload-list items is-success and paired upload no is-loading, actual single checkin POST HTTP200/code0; visible receipt 草稿4278. No duplicate object or business request. Exact original pinned successor DWG/PDF hashes retained. Existing-version focus-stop audit uses real UI blur, natural stop/summary and reopening; no upload/sign rerun.

2026-10-20 SLDPRT4279 formal publication PASS: five real signatures, exact paired PDF, actual tools, focus11→hidden11/no heartbeat→22, stable two HTTP200 PDF responses28727bytes/SHA256 f96f7ff99fdef6b48088d27246c55234b2c83bee5a787be273affb807e15cd32. Initial fresh global list five-second assertion was script loading calibration only; actual ACTIVE readback resumed the same object, no duplicate submission/signature. Single successor4280 saved after both upload items is-success and paired upload no loading. Version isolation old0→stop10→reopen10→stop21; next0→stop10→reopen10→stop20, natural stop/summary exact and non-reading writes0. Original page zoom50%/scroll screenshots show complete bent rod vs three-hole plate, independently accepted by Root; search remains N/A no text layer. Current run1792440609947 trace-seal.json contains sanitized/CRC/hash/natural whitelist evidence; raw traces removed after extraction. DOC stale entry STOPPED remains retained and not a PASS; no production guard change.

2026-10-20 final continuation: Root independently accepted all seven missing formats. SLDASM4281/4282 exact reading old0->12->reopen12->22, next0->11->reopen11->22; SLDDRW4283/4284 old0->11->reopen11->22, next0->11->reopen11->22. Each chain has its own real five-signature publication and three-page successor gear PDF with visible FINAL ASSEMBLY/FA-001. DOC exact4276 real working-entry correction PASS: old4269 47->58->reopen58->69; next4276 201->211->reopen211->221; real OnlyOffice table/search1/1 and independent watermarks, non-reading writes0. Historical stale-entry STOPPED/denial retained separately. Browser contexts0, browserClosed=true, runner35329 exit0; all16 current traces sanitized, raw removed, natural whitelist and SHA/CRC records retained. Authoritative summary: run-missing-formats-1792440609947/continuation-summary.json. No production or new business actions after acceptance. Full E24 only retains original WEBM/WAV formal-admission and pagination/search-semantics pending; no media admission/OCR/transcription. Full-byte comparison removed from business gate; old UNKNOWN are engineering cleanup boundaries, not new business blockers. No Git commit/push or cleanup apply. Final documentation structure/pointers verified.

- R4 diagnosis-only: read current AGENTS/task-closeout/e2e rules, ponytail and Root supervision/issues. Goal: offline trace+source root-cause report. No browser/product/test changes/build/restart/DB/Git writes/agents. BDD and proposed RED only, no tests executed; preserve R3 first failures and XLSX unexecuted remainder.
- R4 report repair-r4-root-cause.md saved; offline structure/evidence path checks PASS. DWG tiny visible raster delta distinguished from source mutation; SLDDRW delay/15s hypothesis bounded; XLSX late start-stop focus precondition and unproven recovery race separated. No tests run or code changes. Waiting Root review; R3 errors and unexecuted remainder unchanged.

- R4 scoped TDD authorization: only reading-lifecycle.ts and its existing unit test. BDD: Given delayed start returns while hidden and self-stop is pending, When visible resumes, Then after old stop exactly one fresh client start and heartbeat; hidden/disposed must never restart, failures propagate, pending-pause recovery remains. RED before production edit, GREEN and adjacent regression afterward. No browser/build/restart/DB/Git/agents. Candidate defect is not proven R3 XLSX cause.
- R4 actual RED11:10PASS/1FAIL before production edit; one-line starting.then(resume) GREEN11/11; final regression61/61 including stop-rejection/no-retry. Review report/diff saved. ready_for_closeout; Git closeout blocked by authorization. No browser run; candidate not proven R3 XLSX cause.
- R4 browser preparation only: repair-r4-browser-prep/targeted.cjs and gate-plan.md prepared; syntax+12 static checks PASS, no browser execution. Waiting exclusive-foreground Root gate; original failures unchanged.
- R4 first-error script repair: repair-r4-script-fix/verification-report.md. Readiness order restored; no product defect proven. UnitRED4fail/GREEN5pass, adjacent61pass, no E2E. Root review pending.
