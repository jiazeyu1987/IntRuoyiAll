# E2E-06 项目内关联

## Goal

独立验证E2E-06，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked（仅工程closeout）— R3第一轮E06业务PASS，S1/S2/S3及S4项目隔离、完整P2未关联确认取消均由本轮真实页面完成。trace 3112项CRC/隐私PASS，凭据命中0，context/browser正常关闭，runner exit0且残留0。唯一正式交接：rerun-20261020-r3-round1/handoff.json。两自有工作草稿4388/4390保留；CONFIRM临时票据最终销毁未核验，不写清理PASS。已先进入ready_for_closeout并完成证据核验；因无Git提交推送授权不标completed。结束本turn等待Root，不开始下一轮。

## R3前历史状态

blocked — 工程closeout因用户禁止Git和清理独立阻塞；Root本轮明确批准后的E06业务复核为BUSINESS_CONTRACT_COVERED/PASS：S1/S2/S3及S2/S4隔离、P2最终确认取消覆盖原合同，非法HTTP分支记正式UI不适用/未触发，不再阻塞业务。只追加s4-contract-review-20261020.md及s4-contract-root-review-20261020.json；原run/result/handoff的3PASS/1BLOCKED、首次失败及trace不改。无新浏览器/测试/构建，保持hold。

## rerun-20261020-r2-round1 准备计划

- 目标及里程碑：建立独立run及可执行入口；核对原U凭据来源；完成无浏览器语法/结构/门禁检查；随后hold，Root新包READY单独放行后才真实执行全部S1/S2/S3/S4并交独立handoff。
- Given 原U、P1/P2及原候选需在新运行重新页面核验；When 用唯一前缀DCC261020C06R2R1执行新草稿关联保存、刷新和重开、项目切换及双tab；Then 每分支真实记录PASS/FAIL/BLOCKED，保留首次失败，旧结果不作为本轮通过。
- S4补验：确认切P2后保持关联为空，补齐项目文件夹、类型、私有矩阵、实际PDF、编号/版本/生效方式及必要属性，点击创建；真实确认框核对项目P2及“关联文件：未关联”后取消，监听该区间正式提交请求为0。不能停在缺必填或强行点确认提交；服务端跨项目拒绝无法自然触发时保留S4 BLOCKED，确认框补验独立记录。
- 预期验证：PREPARE_ONLY只读解密原DPAPI至内存并仅输出目标key存在与非空布尔、语法/结构及门禁；不登录，不启动browser/context，不发业务请求。原单部门约束/账号权限不改；正文仅PDF。
- 设计约束检查：仅本例任务资产、实际分支int_qms、端口8061/48061；执行gate必须携带本runId与Root新运行证明及摘要，不使用旧PID/JAR放行；Root独占汇总rerun-four-rounds-20261020-r2.md。禁止生产修改、Maven/package、重启、Git提交推送、子Agent及API/DB业务动作。

### 本轮S3执行器校准计划

- Given CONFIRM确认页取消后保留单文件，DRAFT2直接替换触发limit=1且未生成新预览、未保存S3；When 保留failure-11及第一段result/trace，关闭第一段会话后开启独立新context继续本轮S3；Then 新页面上传同一预定DRAFT2、选择P2 B/1、只保存一次并刷新/浏览重开，最终结论须标明校准续跑，不能抹去首轮失败。S1/S2/S4不重复，禁止生产改动或清理持久对象。

## 2026-10-20 S4合同与提交边界只读分析

- 目标：核对原需求R08与E2E-06第27行、现有本轮UI隔离证据、提交/草稿服务调用链及直接相关既有单测，判断S4是已证实产品漏洞还是不可由正常UI触发的非法输入防御覆盖缺口。
- Given S1/S2/S3及最终确认隔离已有本轮证据；When 只读需求、源码和既有XML，不开浏览器、不运行测试或构建；Then 单列原合同/静态推导/历史单测/本轮UI事实，保留S4 BLOCKED及首次失败，给最小补验建议，不改产品/共享台账。
- 里程碑与验证：1.原合同定位；2.实际提交与关系校验链；3.既有测试覆盖/限制；4.本例分析记录引用、行号、SHA256及结构核对。设计约束：int_qms，仅本例必要分析文档，不Git写入/DB/服务/子Agent，不缩小合同或造非法E2E。
- 验收口径更新（Root本轮随后明确授权）：原用例只要求残留被拦截，不要求生成非法HTTP；保留同项目候选/切换清空/合法固定版本/完整P2确认取消门禁，解除“必须真实HTTP拒绝”作为E06业务完成条件。正式UI非法输入分支记录不适用/未触发；此为追加review，不改历史计数，不运行任何旧门禁。
- 结果：原合同定位R08第52—53行和E2E第27行；当前提交/custom/working均委托项目校验；历史9项XML零失败，当前只读源码无证实跨项目漏洞。分析与证据引用/哈希结构核对结束，业务review PASS，工程closeout仍blocked。

## 历史状态（保持）

blocked — 工程closeout因用户禁止Git提交推送而独立阻塞。2026-10-18 Root已正式接受E2E06，状态ROOT_ACCEPTED_WITH_EXPLICIT_BOUNDARY，见../20261016-dcc-24-e2e-coordination/e2e06-root-review.json：3真实流程PASS、UI隔离不变量已验证；跨项目HTTP拒绝未由UI触发，原S4 BLOCKED历史保留。Root独立DccCurrentFileRelationsTest 9项PASS（严格异常码/零insert及同项目ACTIVE精确A/1正例），测试汇总见协调目录e2e10-rework-matrix-test-summary.json，不能称页面HTTP拒绝PASS。本例停止执行，浏览器保持关闭，原失败/trace/自有数据保留，不重跑或扩大清理。

2026-10-20第一轮重跑已结束：S1/S2/S3本次真实页面PASS，S4点击提交后被页面必填校验拦截、跨项目HTTP拒绝未触发，保持BLOCKED。独立run为runs/20261020-round1；新草稿4285/4290均刷新并从受控浏览重开核对，无重复保存4285。浏览器已关闭，执行器exit0，当前trace CRC/隐私核验PASS。工程closeout仍独立blocked；不开始下一轮，不改历史结果、生产或运行态，不清理、提交推送或启用子Agent。

## 2026-10-20 第一轮完整重跑

- 目标：重新执行S1本项目合法关联固定版本持久化、S2项目切换清空、S3另一项目合法关联持久化、S4真实页面残留跨项目选择边界及清空后的提交入口；不复用旧终态伪造本次PASS。
- Given 原U账号及任务自有P1/P2、候选可通过当前页面确认；When 用本次唯一前缀DCC261020C06R1的新源件/新工作草稿执行真实上传关联、切换和保存/刷新/浏览重开；Then 各分支按本次DOM/截图/自然请求判定PASS/FAIL/BLOCKED，缺入口不注入，真实失败保留并只收集不修生产。
- 里程碑：运行态及本人登录核验；候选当前ACTIVE页面回读；新S1；新S2/S4；新S3；独立结果/隐私与trace完整性/关闭证据并结束第一轮。
- 预期验证：所有业务只Playwright真实页面；自然请求仅method/path/status，响应JSON不作oracle。缺服务/权限/候选准确BLOCKED。S4若正式UI清空或禁用则保留HTTP拒绝未触发，不能构造非法状态。每人只归属一个部门，音视频仅附件；本例只用PDF。
- 设计约束检查：int_qms；8061 PID20976、48061 PID27908；最新runtime-proof来自20261020-qms-e2e24-runtime-update，Jar SHA256 ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab。仅本任务目录，原账号DPAPI只读；复用候选须当前页面回读，新关联源件及草稿必须本轮新建。

2026-10-19最新授权S4补证已执行并在正式提交按钮禁用处停止，浏览器关闭；结果BLOCKED_FORMAL_SUBMIT_BUTTON_DISABLED_AFTER_RESET，详见下方“S4提交校验补证”及repair-20261019-s4-submit-handoff.json。旧S1/S2/S3通过结论和原S4历史均保留；未触碰历史损坏trace。

两个自有APPLICATION_CUSTOM候选已完整签名审批至ACTIVE，两个草稿固定版本已刷新及从浏览列表重开核验。XButton最小修复组件GREEN 5/5，普通/首次改密登录回归PASS，Root类型检查PASS。当前manifest为runs/20261017-acceptance/fixture-manifest.json。

最终隐私核验另有历史损坏trace-25.zip无法读取；其余150个可读文本条目凭据命中0、原始network条目0，完整扫描BLOCKED。当前业务trace-50.zip可读。cleanup preview因blocked状态exit3，未执行apply；没有将这些阻塞写成PASS。

## 2026-10-19 R2 证据修复

- 目标：核查trace-25有效原始副本的恢复可能性，完整校验trace-50，补齐UI不可达与独立后端验证边界及R2交接。
- 当前状态：本轮证据调查与结构核验已完成；工程closeout仍独立blocked，不覆盖历史验收。
- BDD：Given trace-25损坏、已有三个页面流程通过；When 只读检查本例归档、原始临时目录和trace-50；Then 保留原件与历史失败，明确副本是否存在及CRC结果，不把恢复调查记成新的业务PASS。
- RED：历史完整隐私扫描因trace-25缺中央目录BLOCKED；本轮先复核该失败及交接缺口。GREEN门禁是证据核验和报告结构一致，不能消除无法恢复的历史损坏或升级S4结论。
- 里程碑：1.登记范围；2.只读核查原始来源和ZIP完整性；3.补报告/交接并核对原件哈希。
- 预期验证：标准库ZIP全条目CRC、SHA256前后不变、JSON/报告引用检查；不重复三个已通过流程，不运行Maven/全量类型检查。
- 设计约束检查：int_qms；仅本任务证据资产；不注入页面状态、不制造跨项目提交、不改生产代码；如未发现真实入口遗漏则不启动浏览器；构建和服务操作均不需要。Root后端9项验证单列，不能代替真实UI HTTP拒绝。
- 调查结果：本例61个ZIP中无其他trace-25同名文件或相同前64KiB的有效原始副本；错误指向的原临时目录及本例trace-work均不存在。仅在已核查来源内未找到副本，不推断外部备份不存在。trace-50的2558条目CRC全部PASS，六份原始证据SHA256前后相同。
- 交接：repair-20261019-r2-handoff.json；完整隐私扫描仍因历史损坏BLOCKED，S4 HTTP拒绝仍未由UI触发。无需Maven、前端构建、服务重启或新运行态。
- 收尾验证：JSON/引用/哈希/五份历史评估断言exit0；本轮cleanup preview实际exit1并报告blocked（与10-18历史exit3区分），未apply。没有伪设ready_for_closeout绕过Git授权与证据保留边界。

## 2026-10-19 R4 证据边界收口

- 目标：按R4调度要求归档E2E-06的既有边界，不重跑已通过UI，不重复trace-25来源检索；保留S1/S2/S3 PASS、S4未触发跨项目提交、Root独立后端测试及trace隐私扫描限制。
- Given S1/S2/S3已有真实页面PASS且Root接受其业务结论；When 本轮只读核对R4 dispatch、R2交接、原页面记录、Root review和既有XML；Then 仅补交接文件，明确S4仍BLOCKED，缺失证据及受限恢复条件，不将后端测试或准备/旧包证据升级成新页面PASS。
- RED：现存S4记录明确`crossProjectSubmitAttempted=false`，因此没有真实UI生成的跨项目提交请求/拒绝响应证据；损坏trace-25仍不可读，已查来源内无原始副本。本轮不构造上述缺失证据。
- GREEN：R4交接JSON解析、关键状态/命令/退出码/XML/页面文件引用断言通过；S1/S2/S3仍引用原始页面证据，Root测试仍标记为Root历史执行。本轮无业务动作、代码改动、Maven、浏览器或运行态操作。
- 恢复条件：只有未来获准的真实页面交互能自然形成不一致选择状态时，才可尝试可见提交并记录自然请求/响应及页面结果；若UI持续隔离，则S4继续BLOCKED。trace-25仅在另行发现来源可信、可校验的原始备份时恢复到新副本并重新扫描，不重建ZIP、不覆盖原件。
- 交接：repair-20261019-r4-handoff.json。完整隐私扫描仍BLOCKED_UNREADABLE_ARCHIVE；工程closeout仍blocked，所有业务和历史证据保留。

## 2026-10-19 S4 提交校验补证（当前授权）

- 目标：仅补原S4正式页面动作。用户本轮明确要求P1选关联、同页切P2确认清空、尝试提交并记录页面可见校验；与已有S2合并判断，不制造跨项目非法状态，不重跑S1/S2/S3完整流程。
- Given 原账号U、P1/P2及ACTIVE候选和SWITCH本地源件已存在；When 在真实上传页选择P1关联后确认切P2，并操作可见提交入口；Then 记录旧P1选择清空、提交入口是否可用和实际页面校验、自然业务请求数量；若UI阻止动作，记录具体阻止点后停止。不得把客户端阻止升级成服务端拒绝HTTP PASS。
- 里程碑：1.核对规则、分支及Root当前运行态；2.只执行S4补充页面动作；3.记录截图/DOM/自然请求元数据/trace和实际退出码；4.合并既有S2给出精确结论。
- 验证：Playwright真实页面；使用既有DPAPI账号凭据，不改密码/权限/配置，不读接口JSON，不调用API/DB，不注入组件或存储状态。旧S1/S2/S3证据保留不改，trace-25不读取、不扫描、不检索。
- 设计约束检查：int_qms；Root R6 PID22076/48061、前端8061，Jar哈希478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a。dispatch旧PID25124已由最新r6-runtime-proof.json和实际进程归属核对替代，不修改协调目录。正文关联入口要求临时正文，复用原SWITCH源件；仅允许正式项目切换自带的会话处理，不操作独立清理或其他业务分支。
- 当前补证状态：证据已记录并停止。真实页面P1选中P1CAND A/1→确认切P2→关联0、编号/名称空、正文预览消失；正式“创建受控文件”按钮可见但禁用，未强制点击。S4补充分支为BLOCKED_FORMAL_SUBMIT_BUTTON_DISABLED_AFTER_RESET，不宣称点击后的校验或HTTP拒绝PASS。与原S2合并确认该UI路径不携带旧关联；无正式提交/保存草稿/关联写请求，未创建新持久化对象，未做DB核验。工程closeout继续独立blocked，不提交推送、不清理、不重启服务、不启用子Agent。

## Parameterized Subcases (历史定义)

- `S1-P1-persist`: 在P1上传页打开“关联”，选择本项目ACTIVE受控文件版本；通过真实保存进入详情，由冻结编号/固定所选版本验证持久化。
- `S2-project-switch-reset`: 已有P1文件/文件夹/正文/关联输入时切换到P2；确认真实切换提示后，旧关联、文件夹和正文输入清空，P2目录重新加载。
- `S3-P2-persist`: 在P2重新上传任务自有正文，选择本项目ACTIVE版本并保存，再由详情固定关联表核对编号/版本。
- `S4-stale-cross-project-submit`: 尝试仅通过真实页面保留旧P1选择后提交；页面无法制造该状态时单独BLOCKED，不记未适用或整体PASS；若页面实际保留，断言确认关联被拦截且无成功提交。

2026-10-18 Root最终复核口径：以上S4为原服务拒绝触发目标及历史记录；已有双tab与S2证明真实UI隔离不变量。服务拒绝路径UI不可达，禁止注入、增加入口或扩展资产；Root独立后端拒绝/零insert补证已PASS并单列。Root协调范围扩大至06–15，本线程原用例停止，不承接新增业务执行。

## Exact Entry And Fixture Contract

- 真实入口：登录页 -> 动态菜单“文控中心 / 文件上传” -> `/dcc/controlled-file/upload` -> “关联”按钮 -> `文件选择器 · 关联文件`。
- 关联窗口的关键可见控件：`[data-testid="current-file"]`、项目目录树、搜索框、“来源项目”列、候选行复选框、“已选”标签、“确认关联”按钮。
- 页面源码依据：`DccFileSelector.vue` 在 `purpose=relations` 时把 `relationProjectId` 设为来源项目并禁用跨项目行；`selectProject` 拦截跨项目点击；`FileSelectorState` 在 reset/select/save 再次校验项目、租户、ACTIVE受控状态和可查看权限。上传页 `handleProjectCodeChange` 的确认后清理会清空文件夹、正文、关联和部门选择；`persistUploadRelations` 再次拒绝 `row.projectId !== current.projectId`。
- 任务自有数据：P1、P2各有可区分的有效项目代码、活动逻辑文件夹、一个可访问ACTIVE受控版本和一个可上传正文文件；候选正文版本必须在页面可见且 `canPreview`，版本号和编号可辨认。建议编号/文件名带 `DCC241016C06`，不覆盖其它例数据。
- 角色：申请人必须是有 `dcc:controlled-file:submit` 的真实账号；项目/文件夹/候选版本需在该账号的租户和 VIEW 权限范围内。最新授权允许通过页面创建本例前缀账号、角色及菜单授权；配置admin不作为业务身份，不授管理员，不修改共享角色/矩阵/既有密码。
- 当前执行器为ui06-r17.cjs及ui06-flows-r17.cjs；初始配置身份来自已授权AGENTS，任务账号凭据仅在当前Windows用户DPAPI保护配置处理，不进入证据或提交。旧环境变量runner仅保留10-16历史。
- 环境：默认要求 `http://127.0.0.1:8061`、后端 `48061`、dispatch `profile=int_qms`；浏览器启动前检查runtimeVerified和accountSetupGate；业务执行以PER_CASE_REAL_UI_MANIFEST及本例fixture-manifest.json中的真实页面核验为准。

## Recovery And Cleanup

- 任何切换/取消路径均不得提交审批；若有临时上传票据，按上传页真实“取消/清理”动作处理并记录结果。
- 两个候选按Root后续明确授权完成独立审批；两个关联草稿未提交审批。全部精确标识、当前状态及保留清单交Root review，不通过API、SQL或浏览器存储删除数据。
- 真实执行保留trace、关键截图和结果JSON供Root review；未放行前不创建浏览器artifact。

## BDD

- Given P1、P2均有可区分的文件版本
- When 在P1关联窗口选择文件；切换至P2后核对关联并重新选择；对旧页面保留的原项目选择尝试提交
- Then P1关联窗口仅能选择P1文件；项目切换后旧关联不能直接带入；任何残留跨项目选择均被拦截；合法关联保留所选版本。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 当前自有fixture真实UI准备和完整审批；旧E06-PRE-01已不再阻断，旧证据独立保留。
4. [x] S1/S2/S3 PASS；双tab与同页切换的UI隔离不变量已验证，S4服务触发目标原BLOCKED历史保留。
5. [x] Root单列独立后端拒绝/零insert证据9项PASS，业务验收ROOT_ACCEPTED_WITH_EXPLICIT_BOUNDARY；原用例停止。
6. [ ] 工程closeout因用户禁止Git独立blocked；现有失败、trace、自有数据保留，不扩大清理。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 2026-10-17 Root新授权生产例外：唯一归属IntRuoyiFronted/src/components/XButton/src/XButton.vue及新增最小真实Vue组件行为测试（tests/components/x-button-click/）。只修复重复click分发；不改XTextButton/LoginForm/全局防抖，类型检查由Root统一。已先记录BDD及真实单击两请求，再完成Vite组件RED、离线组件RED，之后才删除重复手调。

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C06，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-06/rerun-20261020-r3-round1
- doc/tasks/20261016-dcc-e2e-06/ui06-rerun-r3-round1.cjs
- doc/tasks/20261016-dcc-e2e-06/verify-rerun-r3-round1.py
- doc/tasks/20261016-dcc-e2e-06/s4-contract-review-20261020.md
- doc/tasks/20261016-dcc-e2e-06/s4-contract-root-review-20261020.json
- doc/tasks/20261016-dcc-e2e-06/ui06-rerun-r2-round1-s3.cjs
- doc/tasks/20261016-dcc-e2e-06/verify-rerun-r2-round1.py
- doc/tasks/20261016-dcc-e2e-06/task.md
- doc/tasks/20261016-dcc-e2e-06/execution-log.md
- doc/tasks/20261016-dcc-e2e-06/verification-report.md
- doc/tasks/20261016-dcc-e2e-06/prepared.json
- doc/tasks/20261016-dcc-e2e-06/e2e-06-project-relations.e2e.cjs
- doc/tasks/20261016-dcc-e2e-06/fixture-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-06/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-06/result.json
- doc/tasks/20261016-dcc-e2e-06/runtime-findings.md
- doc/tasks/20261016-dcc-e2e-06/verify-artifacts.ps1
- doc/tasks/20261016-dcc-e2e-06/artifact-scan.json
- doc/tasks/20261016-dcc-e2e-06/artifacts
- doc/tasks/20261016-dcc-e2e-06/runs
- doc/tasks/20261016-dcc-e2e-06/ui06-r17.cjs
- doc/tasks/20261016-dcc-e2e-06/ui06-flows-r17.cjs
- doc/tasks/20261016-dcc-e2e-06/finalize-evidence-r17.cjs
- doc/tasks/20261016-dcc-e2e-06/make-signature-fixtures-r17.ps1
- doc/tasks/20261016-dcc-e2e-06/verify-r17-privacy.ps1
- doc/tasks/20261016-dcc-e2e-06/runtime-pause-20261017.json
- doc/tasks/20261016-dcc-e2e-06/interruption-checkpoint-20261017-2205.json
- doc/tasks/20261016-dcc-e2e-06/audit-traces-r2.py
- doc/tasks/20261016-dcc-e2e-06/repair-20261019-r2-audit.json
- doc/tasks/20261016-dcc-e2e-06/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-06/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-06/ui06-s4-submit-20261019.cjs
- doc/tasks/20261016-dcc-e2e-06/verify-s4-submit-20261019.py
- doc/tasks/20261016-dcc-e2e-06/repair-20261019-s4-submit-handoff.json
- doc/tasks/20261016-dcc-e2e-06/ui06-round1-20261020.cjs
- doc/tasks/20261016-dcc-e2e-06/verify-round1-20261020.py
- doc/tasks/20261016-dcc-e2e-06/rerun-round1-20261020-handoff.json
- doc/tasks/20261016-dcc-e2e-06/ui06-rerun-r2-round1.cjs
