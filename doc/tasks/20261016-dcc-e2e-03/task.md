# E2E-03 源件与多附件

## Goal

独立验证E2E-03，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 原R3业务六组已由历史证据及continuation-20261021补齐PASS；工程Git未授权、未apply，不标completed。历史S传输超时慢因UNKNOWN，S#1272已核实VALID。

## 新四轮R2第一批准备

- 目标：重新完整执行原E2E03全部适用分支，分别新准备上传与升版申请、源件及两不同附件；不继承历史PASS。
- BDD：Given Root新运行包READY并明确放行本run，When六原账号在真实页面完成上传中/真实失败/页面可达保存失败及恢复、确认清单、审批检入升版，Then逐项记录全部上传保存成功门禁、申请/版本附件回读与隔离；首个真实失败留原证据，依赖阻塞不遮蔽独立分支。
- 里程碑：规则/凭据来源/执行入口准备 → hold → Root新包及单例放行 → 全部适用分支执行 → 关闭context/browser/runner、独立handoff。
- 预期验证：准备阶段仅语法、静态安全、浏览器路径存在与关门自检；六业务分支全NOT_RUN。执行阶段新对象新证据，逐项PASS/FAIL/BLOCKED，禁用API/DB/mock替代。
- 设计约束检查：原U/S/L/C/A/D及其既有单部门夹具；不扩大权限，不改生产、不Maven/package/重启/Git/子Agent；Root共享汇总仅Root写。任务本轮gate默认PREPARE_ONLY，旧dispatch不得绕过。

## 2026-10-20 共享浮层修复计划

- 目标：E2E03-R1-UI01 / E2E23-R4-UI-01，底层“显示字段/重置”不覆盖附件预览，关闭预览后仍可使用。
- BDD: Given页面表格与弹窗/抽屉各自拥有列设置，When打开附件预览或更上层弹窗，Then控件遵守所属容器层级，底层控件不能拦截上层内容；关闭上层后恢复可操作。
- 根因候选：App唯一挂载UserTableColumnGlobalEnhancer，其Teleport统一到body，styles/index.scss固定z-index:2100；读取真实DOM后确认。
- 里程碑：真实DOM根因证据 → 独立浏览器布局回归RED → 直接共享组件最小GREEN → 03原版本附件复验并交23入口 → 清理preview/工程状态说明。
- 预期验证：新增定向布局测试、已有列配置/observer与附件合同；真实Playwright以原U和原正式对象从浏览器入口复验，不造新业务数据。
- 设计约束检查：仅当前共享组件、对应样式与定向测试；不全局抬Dialog z-index，不改权限/版本/审批，不触碰02/08/18等归属，无Maven/包/服务重启。

## BDD

### 2026-10-20 第一轮独立全量适用分支重跑

- Given 当前int_qms运行证明及本人真实登录，When以本次唯一前缀通过UI填写新申请、真实选择源件及两附件，Then逐项记录上传中阻止、真实失败定位及恢复、就绪清单、确认取消保留和保存后回读。仅独立context离线触发网络失败，不拦截或mock。
- Given 新申请合法提交且审批岗位可用，When本人按真实任务完成链路并检出检入、升版，Then记录新旧对象/申请身份与源件/附件隔离。依赖失败仅阻塞依赖分支，独立适用分支继续，不以旧终态冒充本次。
- Given 当轮仅验收搜集问题，When发现错误，Then保存触发步骤、页面提示和直接原因，不改生产/公共配置；无法安全触发保存失败则该分支BLOCKED，不造假。最后关闭浏览器/runner、保留原始证据且不执行下一轮。

### 2026-10-19 R5 原batch14只读收口

- Given Root已提供r5-runtime-proof.json及59类归档证明，When启动本例页面验收，Then按R5正式字段、Jar SHA及部门投影类SHA核对，不再依赖旧派工字段。
- Given D910417当前仅原991295审签，When真实管理页临时绑定910233，Then先保存原角色，fresh D只读原4186/A2/BPM/batch14；六人接收部门、原发送时间正确，SENT不显示当前失败时间或错误横幅，发送/重试/签收请求为0。
- Given 页面断言成功或失败，When进入finally，Then恢复并回读D精确原角色、关闭所有本次浏览器并保留三份trace；任何恢复失败单列且不报PASS。FAILED条件由保留的前端43项覆盖，不重造批次失败、不重跑原六组。
- Given 当轮禁止Git、构建、重启与共享代码改动，When业务验证结束，Then仅更新任务证据并交Root，Git/清理完成门禁单列阻塞，不标记completed。

### 2026-10-19 R4 现有角色临时绑定与精确恢复

- Given R2前端43PASS、原六组及batch14原事实已接受，When继续R4，Then保留证据，不重跑上传、审批、发送、重试或签收。
- Given Root尚未放行03独占Maven且旧包不含部门投影，When准备验证，Then仅保存命令/基线/运行器，后端RED/GREEN和原batch14展示均明确NOT_RUN。
- Given Root新包证明8061/48061且明确包含部门投影、D当前仅原审签，When真实管理页临时绑定已有910233，Then先落盘精确原角色及恢复账，fresh D仅查看原186/A2/BPM/batch14六人部门/原发送时间和无当前失败时间。
- Given 临时授权保存可能成功但回读或业务断言失败，When进入finally，Then独立管理页先核对当前角色；仅在原审签加本次文控两项时移除文控，已是原角色则只读确认，其他组合明确阻塞且不猜改；最终精确比对原角色。恢复失败单列，不能写PASS。
- Given 无新包证明或无法先保存原角色/验证恢复入口，When启动运行器，Then授予前失败，0角色/业务写入。语法和恢复控制流自检不代表真实UI通过。

### 2026-10-19 R3 仅补未闭环

- Given R2前端43项已通过且未再改相关代码，When继续，Then复用原RED/GREEN证据，不重跑已接受六组或43项。
- Given 新部门响应测试与固定实现已准备、24当前独占Maven，WhenRoot串行放行03，Then先对原服务/VO基线执行有效断言RED，再固定实现GREEN及相邻回归；编译/依赖失败不当作产品RED，禁止并行碰共享target。
- Given 原D仅任务Review且读取守卫要求精确doc_control，When通过真实管理页只读核对D和现有角色、结合正式角色编码唯一性规则，Then准确判定能否创建任务最小角色；不得借相似编码、admin或公共宽权限角色绕过，不修改公共角色定义。
- Given Root新包明确包含部门投影且D合法最小资格已就绪，When从原186/A2/BPM打开发送与签收，Then只读原batch14，6人部门/原发送时间正确、无当前失败时间、0发送/重试/签收；随后正式页面恢复临时资格。前置未满足明确NOT_RUN/BLOCKED。

### 2026-10-19 R2 定向展示修复（本轮重新开放）

- Given 原186/A2/BPM/batch14已SENT且保留历史failureAt，When读取正式通知面板，Then显示当前已送达，不显示当前“实际失败时间”；FAILED仍显示正式失败时间，审计字段不清空、不回写。
- Given 原6条收件义务各自绑定distribution.departmentId，When读取回执，Then部门名称取该精确接收部门的正式当前名称，不取接收人当前部门或签名部门快照；未找到部门时保持明确未记录，不编造名称。
- Given 原通知2620—2625与原发送时间/参数已验收，When仅复验展示和接收人，Then不触发重试/发送/签收/新版本；已通过六组不重跑。无重复平台消息由Root只读核验佐证。
- Maven/全量类型检查窗口由Root串行，13先占；本例先轻量前端RED/GREEN、准备后端回归与运行切包需求，不用旧Jar宣称部门修复通过。
- 补失败运行临时资产账：区分已正式UI清理、票据已绑定业务、历史服务端状态未核验；不删除trace/截图/复核证据，不把浏览器关闭等同服务器临时件已清除。

- Given 分别准备上传及升版申请、两个不同附件
- When 上传源件与多个附件；分别在上传中、失败及页面可触达的保存失败状态尝试提交，再恢复成功重试
- Then 源件和每个附件全部上传、保存成功才可提交；失败明确定位；确认框清单完整，提交后对应申请和版本可查看全部附件，不串版。失败状态须由真实页面流程观察，不伪造成功。
- 恢复BDD：Given 原草稿2054545668044084182已保存且Root修复详情初始化，When 原U全新context通过真实页面重开原文件，Then 先确认文件编号、源件和两个不同附件均可见且无初始化pageerror；只在实际回读通过后继续原流程，禁止重放上传建档。
- 独立失败BDD：Given Root明确授权仅本例独立Playwright context离线，原D任务及初始申请保持不动，When 在另一未保存上传表单分别令源件、附件A、附件B遭遇真实网络离线失败，再恢复online经真实控件移除/重新选择，Then 逐材料记录上传中、明确失败提示、提交禁用或实际本地阻止及恢复就绪；仅网络离线，不mock/拦截响应、不改共享网络/服务/权限/DB，不保存或提交新申请，临时件经页面清理。客户端校验拒绝与网络上传失败分开记录。

## Parameterized Subcases

- `U1-upload-ready-confirmation`: 新建上传；一个源件、附件A、附件B，三者均在页面回显为就绪；检查提交按钮解除禁用、确认框列出三项，取消确认后输入保持。
- `U2-uploading-block`: 新建上传；源件已回显，普通附件仍处于真实“上传中”；检查页面状态和提交按钮禁用，不提交申请。
- `U3-upload-failed-block`: 新建上传；使用任务自有、由真实页面产生失败状态的附件输入；检查页面明确显示失败对象和原因，失败附件未移除前不能提交。禁止拦截请求或伪造失败响应。
- `U4-save-failed-retry`: 新建上传；源件和两个附件均成功后，在页面可触达的真实保存失败状态点击保存/提交，记录页面错误和无成功申请，恢复有效状态后重新保存/提交。已观察真实审计策略缺失失败及恢复后保存导航；不再重复制造失败或保存，等待详情修复后回读原文件完成恢复验收。
- `R1-revision-checkin-confirmation`: 既有任务自有 ACTIVE 版本；检出后检入变更源件、附件A、附件B，确认生成同一主档的 WORKING 小版本，进入“填写审批申请”并检查升版确认框列出三项附件。
- `R2-version-isolation`: 提交升版后从真实详情页分别查看旧版和新版附件清单；每个附件只属于对应版本，不能串版。需在R1完成后执行。

## Formal Page Entrypoints

- 新建上传：`/dcc/controlled-file/upload`，页面锚点 `dcc-upload-single-page-workbench`；源件区为 `dcc-upload-section-file`，普通附件区为 `dcc-upload-attachment-files`，失败/上传中状态为 `dcc-upload-attachment-upload-state`，提交确认清单为 `dcc-upload-submit-confirmation`。
- 受控浏览及检入：`/dcc/controlled-file/browser`，页面锚点 `dcc-controlled-browser-checkout`、`dcc-controlled-browser-checkin`、`dcc-controlled-browser-checkin-upload`、`dcc-checkin-attachments`、`dcc-controlled-browser-checkin-submit`。
- 升版申请及版本附件：`/dcc/controlled-file/detail/:id`，详情面板锚点 `dcc-detail-attachments`、`dcc-revision-panel`、`dcc-revision-submit`；版本历史在同一详情页的“版本历史”分区。浏览页行内“填写审批申请”进入正式详情流程。

## Roles And Data Requirements

### 本轮资格准备BDD

- Given 本例现有账号910412—910417和自有角色、部门关系已保留，Root明确允许继续资格前置准备。
- When 通过真实系统管理页核对同租户、启用及正式岗位列表，为缺岗位的本例账号绑定任务自有岗位，保存后重开回读；分别独立登录业务账号维护签名图片，管理员仅对本例账号配置签名授权。
- Then U与S都具备非空正式岗位列表；需要签名的本例身份均有有效授权和启用签名图片，页面可见账号及图片归属明确。受控文件上传/提交/流程发布写请求保持0，凭据不入trace或截图。

- 申请人：真实测试账号必须具备页面菜单/按钮权限 `dcc:controlled-file:submit` 和查询所需 `dcc:controlled-file:query`，并在同一测试租户有可用部门、直属主管、文件类型正式 taxonomy/category、审批路线、项目文件夹和批准人候选。账号标签和页面真实姓名分别记录，不记录密码。
- 升版操作人：真实账号须能在受控浏览看到任务自有 ACTIVE 版本并由本人执行检出/检入；不能用管理员替代固定业务角色。后续主管、会签、批准、文控账号不属于本例准备阶段的替代身份，若正式提交后需要继续审批必须由Root核对独立账号和角色。
- 新建数据：项目、叶子文件夹、正式文件类型、编号前缀 `DCC241016C03`；源件、附件A、附件B文件名和内容必须不同且可在页面辨认。
- 升版数据：同一任务自有主档的当前 ACTIVE 版本、可检出权限、变更源件和两个不同附件；旧版附件与升版附件必须使用不同内容或文件名以验证隔离。
- 禁止使用公共矩阵、其它用例对象或数据库/API 预置业务状态；缺任一正式数据或角色时记录 BLOCKED。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后真实前端执行及临时权限/浏览器恢复；业务fixture按要求保留。
4. [x] Root最终review已接受原六组与原batch14恢复；工程Git/cleanup未完成。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C03，不改其它用例对象或公共矩阵；新增本例账号、角色、组织、类型和矩阵已获最新授权，全部走真实页面。
- 本目录由本例线程维护，Root独占协调目录；本例真实 fixture 核验通过后继续 E2E，不重复等待批准。

## 剩余升版及版本隔离执行准备（仅准备，未执行）

1. Root完成生产根修并明确恢复后，核对新PID/SHA；原D新context从审批中心原行重开原file/BPM/task，确认原任务仍待办、前序772—775不变。禁止重签S/L/C/A；仅在Root恢复后执行一次仍待办的D任务，先持久化真实回执，再等页面受控/生效终态，不凭HTTP200推进。
2. 原U新context经受控浏览选择原编号当前有效版，核对原源件及旧附件A/B、版本/文件身份；从真实“操作面板”进入“检出 / 检入”，在目标行使用dcc-controlled-browser-checkout。缺入口或权限时报具体BLOCKED，不扩大权限。
3. 在同一任务文件的检入弹窗，选择不同内容/不同文件名的本例升版源件及附件RA/RB；用真实filechooser完成。分别记录源件、每个附件实际上传中状态及检入/提交禁用；窗口未观察到不写PASS、不通过mock/拦截造状态。真实可达的失败输入按正式校验核对对象/原因、阻止及移除恢复，无法安全触发即保留该参数BLOCKED。
4. 修改说明和检入备注明确本次真实变更；dcc-controlled-browser-checkin-submit仅提交一次。持久化从页面得到的同主档新WORKING小版本身份，核对源件及RA/RB清单，不从接口JSON推断；检入仅生成工作小版本，不等同升版审批已提交。
5. 选定该工作正文，进入正式“填写审批申请”或详情“发起局部 / 换版变更”；明确局部/换版意图、受控基线、送审正文及目标版本。确认框核对源件和RA/RB、基线/正文/目标及同轮配置；取消保留，再由原身份提交一次并持久化新BPM/候选文件身份。
6. 后续审批仍按真实主管/负责人指派/会签/批准/文控账号分别办理，凭据输入不录trace；审批或发布失败先保留原身份/回执/精确错误，不重复提交。必须实际达到新版受控/生效终态才验收隔离。
7. 从真实版本历史分别打开原版及新版，逐项核对编号、精确版本/文件ID、各自源件及附件清单：原版A/B、新版RA/RB；在线查看与允许下载都实际点击并核对对应内容/文件名。记录旧/新状态、前驱/后继与当前有效版落位。按钮缺失、无法下载或没有旧新版对照时只记录对应参数BLOCKED，不用API/DB补动作。

## Cleanup Keep
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/blocked.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/active/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/blocked.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/105-A2-source-render.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/105-A2-source-render.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/106-A2-attachments-RA.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/106-A2-attachments-RB.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/106-A2-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/110-A1-attachments-A.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/110-A1-attachments-B.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/110-A1-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/command.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/dispatch.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/error-101-A2-source.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/next-steps.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-101-A2-source.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-105-A2-source-render.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-106-A2-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-107-open-A1.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-108-A1-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-109-A1-settled.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-110-A1-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/response-111-close.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/selected-frozen.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/final-previews/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/fixture-markers.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/error.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/final.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/L-before.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/L-before.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/network.jsonl
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/A-after.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/A-before.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/A-before.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/A-pre-sign.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/assignment-revision.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/C-after.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/C-before.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/C-before.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/C-pre-sign.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/D-after.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/D-before.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/D-before.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/D-pre-sign.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/L-after.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/L-before.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/L-before.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/L-pre-sign.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/network.jsonl
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/signature-A-revision.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/signature-C-revision.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/flow-live/signature-D-revision.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/handoff.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/002-A2-attachments-RA.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/002-A2-attachments-RB.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/002-A2-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/command.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/dispatch.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/next-steps.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/response-001-frozen-inspect.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/response-002-A2-attachments.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/response-003-close-readonly.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/selected-frozen.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/live/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/red-plan.md
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/A1-verified.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/A1-verified.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/A2-verified.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/A2-verified.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/login-diagnostic.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-diagnostic/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/source-verification/result.json
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-actions.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-active.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-final.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-flow-live.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-flow.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-live.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-sources-diagnostic.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021-sources.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021.cjs
- doc/tasks/20261016-dcc-e2e-03/login-isolated-context-diagnostic-20261021.cjs
- doc/tasks/20261016-dcc-e2e-03/continuation-20261021/audit.json

- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/A2-source-preview.png
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/audit.json
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/isolation-key-restored-20261021.cjs

- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/audit.json
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/blocked.png
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/continued/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/continued/result.json
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/continued/U-exact-rows.png
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/continued/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/continued/U-original-detail.png
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/U-menu.png
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021.cjs
- doc/tasks/20261016-dcc-e2e-03/applicant-readonly-20261021-continued.cjs

- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/actual-dom/actual-menu.png
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/actual-dom/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/actual-dom/result.json
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/audit.json
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/blocked.png
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021.cjs
- doc/tasks/20261016-dcc-e2e-03/menu-done-readonly-20261021-actual.cjs

- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/audit.json
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/blocked.png
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/done-readonly-20261021.cjs

- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/audit.json
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/blocked.png
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/handoff.md
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/result.json
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/task.md
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/readonly-20261021.cjs


- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1.cjs
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/gate.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/run.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/prepared.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/handoff.md

- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020-closeout-preview.log
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/runner-closure.json

- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-audit-20261020.py
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020-handoff.md
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/baseline-A2-RA.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/baseline-result.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/baseline.trace.zip
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/final-A2-active.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/final-A2-isolation.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green-A2-RA.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green-result.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green.trace.zip
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green2-A1-A.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green2-A1-B.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green2-A2-RA.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green2-result.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/green2.trace.zip
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/probe-A2-RA.png
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/probe-result.json
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/probe.trace.zip
- doc/tasks/20261016-dcc-e2e-03/repair-overlay-20261020/red-layout.txt

- doc/tasks/20261016-dcc-e2e-03/detail-recovery-artifacts

- doc/tasks/20261016-dcc-e2e-03/task.md
- doc/tasks/20261016-dcc-e2e-03/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/prepared.json
- doc/tasks/20261016-dcc-e2e-03/e2e-03.cjs
- doc/tasks/20261016-dcc-e2e-03/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-03/resume-results.json
- doc/tasks/20261016-dcc-e2e-03/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/verification-report-before-resume.md
- doc/tasks/20261016-dcc-e2e-03/ui-live.cjs
- doc/tasks/20261016-dcc-e2e-03/ui-command.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-fixture.cjs
- doc/tasks/20261016-dcc-e2e-03/select-document-control.cjs
- doc/tasks/20261016-dcc-e2e-03/record-resume-results.cjs
- doc/tasks/20261016-dcc-e2e-03/close-run.cjs
- doc/tasks/20261016-dcc-e2e-03/audit-evidence.ps1
- doc/tasks/20261016-dcc-e2e-03/.gitignore
- doc/tasks/20261016-dcc-e2e-03/resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/live-artifacts
- doc/tasks/20261016-dcc-e2e-03/fixtures
- doc/tasks/20261016-dcc-e2e-03/artifacts
- doc/tasks/20261016-dcc-e2e-03/activated-users.json
- doc/tasks/20261016-dcc-e2e-03/qualification-artifacts
- doc/tasks/20261016-dcc-e2e-03/qualification-results.json
- doc/tasks/20261016-dcc-e2e-03/qualification-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/prepare-post.cjs
- doc/tasks/20261016-dcc-e2e-03/bind-posts.cjs
- doc/tasks/20261016-dcc-e2e-03/authorize-signatures.cjs
- doc/tasks/20261016-dcc-e2e-03/prepare-signature-image.cjs
- doc/tasks/20261016-dcc-e2e-03/record-qualification.cjs
- doc/tasks/20261016-dcc-e2e-03/workflow-resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/root-workflow-resume-result.json
- doc/tasks/20261016-dcc-e2e-03/workflow-resume-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/qualification-results-before-workflow-resume.json
- doc/tasks/20261016-dcc-e2e-03/resume-route-readiness.cjs
- doc/tasks/20261016-dcc-e2e-03/recheck-applicant-supervisor.cjs
- doc/tasks/20261016-dcc-e2e-03/record-workflow-resume.cjs
- doc/tasks/20261016-dcc-e2e-03/identity-fix-artifacts
- doc/tasks/20261016-dcc-e2e-03/identity-fix-results.json
- doc/tasks/20261016-dcc-e2e-03/identity-fix-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/run-upload-attachments.cjs
- doc/tasks/20261016-dcc-e2e-03/clear-rejected-source.cjs
- doc/tasks/20261016-dcc-e2e-03/record-identity-fix.cjs

- doc/tasks/20261016-dcc-e2e-03/category-upload-artifacts
- doc/tasks/20261016-dcc-e2e-03/audit-recovery-artifacts
- doc/tasks/20261016-dcc-e2e-03/category-save-blocker.json
- doc/tasks/20261016-dcc-e2e-03/detail-initialization-blocker.json
- doc/tasks/20261016-dcc-e2e-03/category-upload-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/audit-recovery-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/prepare-category-upload-permission.cjs
- doc/tasks/20261016-dcc-e2e-03/continue-upload-save.cjs
- doc/tasks/20261016-dcc-e2e-03/inspect-saved-detail.cjs
- doc/tasks/20261016-dcc-e2e-03/inspect-existing-working.cjs
- doc/tasks/20261016-dcc-e2e-03/record-category-save-blocker.cjs
- doc/tasks/20261016-dcc-e2e-03/record-detail-blocker.cjs

- doc/tasks/20261016-dcc-e2e-03/record-controlled-file-id-blocker.cjs
- doc/tasks/20261016-dcc-e2e-03/controlled-file-id-tdz.json
- doc/tasks/20261016-dcc-e2e-03/detail-recovery-evidence-audit.json

- doc/tasks/20261016-dcc-e2e-03/detail-green-artifacts
- doc/tasks/20261016-dcc-e2e-03/detail-green-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/detail-readonly-transaction-blocker.json
- doc/tasks/20261016-dcc-e2e-03/record-detail-readonly-blocker.cjs

- doc/tasks/20261016-dcc-e2e-03/runtime-update-hold.json

- doc/tasks/20261016-dcc-e2e-03/overnight-artifacts
- doc/tasks/20261016-dcc-e2e-03/overnight-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/rollback-readonly-artifacts
- doc/tasks/20261016-dcc-e2e-03/rollback-readonly-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/doc-control-finalization-blocker.json
- doc/tasks/20261016-dcc-e2e-03/verify-existing-detail.cjs
- doc/tasks/20261016-dcc-e2e-03/check-initial-confirmation.cjs
- doc/tasks/20261016-dcc-e2e-03/check-source-preview.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-submitted-application.cjs
- doc/tasks/20261016-dcc-e2e-03/sign-current-task.cjs
- doc/tasks/20261016-dcc-e2e-03/assign-signoff.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-doc-control-rollback.cjs
- doc/tasks/20261016-dcc-e2e-03/record-doc-control-blocker.cjs

- doc/tasks/20261016-dcc-e2e-03/offline-failure-artifacts
- doc/tasks/20261016-dcc-e2e-03/offline-failure-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/initial-offline-failure-coverage.json
- doc/tasks/20261016-dcc-e2e-03/verify-network-upload-failure.cjs
- doc/tasks/20261016-dcc-e2e-03/continue-network-source.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-source-uploading.cjs
- doc/tasks/20261016-dcc-e2e-03/cleanup-independent-uploads.cjs
- doc/tasks/20261016-dcc-e2e-03/record-offline-failure-coverage.cjs

- doc/tasks/20261016-dcc-e2e-03/controlled-time-resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/controlled-time-resume-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/D-auto-distribution-blocker.json
- doc/tasks/20261016-dcc-e2e-03/capture-D-resume-failure-dialog.cjs
- doc/tasks/20261016-dcc-e2e-03/record-D-auto-distribution-blocker.cjs

- doc/tasks/20261016-dcc-e2e-03/activation-time-resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/activation-time-resume-result.json
- doc/tasks/20261016-dcc-e2e-03/capture-initial-active.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-audit-policy-pause.cjs

- doc/tasks/20261016-dcc-e2e-03/activation-time-resume-evidence-audit.json

- doc/tasks/20261016-dcc-e2e-03/revision-resume-result.json
- doc/tasks/20261016-dcc-e2e-03/revision-upload-negative.cjs
- doc/tasks/20261016-dcc-e2e-03/recover-revision-attachment.cjs
- doc/tasks/20261016-dcc-e2e-03/revision-save-negative.cjs
- doc/tasks/20261016-dcc-e2e-03/login-isolated-context.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-no-working-after-rejected-checkin.cjs
- doc/tasks/20261016-dcc-e2e-03/restore-checkin-original-name.cjs
- doc/tasks/20261016-dcc-e2e-03/save-corrected-revision.cjs
- doc/tasks/20261016-dcc-e2e-03/prepare-own-view-download.cjs
- doc/tasks/20261016-dcc-e2e-03/reproduce-partial-custom-submit-block.cjs
- doc/tasks/20261016-dcc-e2e-03/check-revision-confirmation.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-formal-revision-submission.cjs
- doc/tasks/20261016-dcc-e2e-03/record-revision-progress.cjs

- doc/tasks/20261016-dcc-e2e-03/partial-intent-resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/capture-partial-hmr-pause.cjs

- doc/tasks/20261016-dcc-e2e-03/partial-submit-idempotency-blocker.json
- doc/tasks/20261016-dcc-e2e-03/partial-intent-resume-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/submit-partial-once.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-partial-idempotency-reject.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-no-formal-after-partial-reject.cjs
- doc/tasks/20261016-dcc-e2e-03/record-partial-idempotency-blocker.cjs

- doc/tasks/20261016-dcc-e2e-03/custom-context-resume-artifacts
- doc/tasks/20261016-dcc-e2e-03/custom-context-resume-evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/A2-R2-progress-result.json
- doc/tasks/20261016-dcc-e2e-03/verify-saved-working185.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-A2-active-terminal.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-one-attachment-preview.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-attachment-download.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-old-version-list-readonly.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-directory-scope-readonly.cjs
- doc/tasks/20261016-dcc-e2e-03/prepare-base-U-access.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-preview-identity-three-values.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-notification-pause.cjs
- doc/tasks/20261016-dcc-e2e-03/record-A2-R2-progress.cjs

- doc/tasks/20261016-dcc-e2e-03/six-groups-final-review.json
- doc/tasks/20261016-dcc-e2e-03/open-version-formal-ui.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-old-attachment-eligibility.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-preview-identity-decoded-v2.cjs
- doc/tasks/20261016-dcc-e2e-03/inspect-D-distribution-role-readonly.cjs
- doc/tasks/20261016-dcc-e2e-03/inspect-doc-control-role-definition.cjs
- doc/tasks/20261016-dcc-e2e-03/observe-same-U-natural-refresh.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-natural-expiry-login-result.cjs
- doc/tasks/20261016-dcc-e2e-03/prepare-project291-D-view.cjs
- doc/tasks/20261016-dcc-e2e-03/temp-author-project-menu.cjs
- doc/tasks/20261016-dcc-e2e-03/prepare-D-narrow-distribution-fixture.cjs
- doc/tasks/20261016-dcc-e2e-03/temp-D-doc-control-role.cjs
- doc/tasks/20261016-dcc-e2e-03/retry-original-batch14-once.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-batch14-partial-failure.cjs
- doc/tasks/20261016-dcc-e2e-03/verify-author-project-menu-restored.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-historical-batch13-readonly.cjs
- doc/tasks/20261016-dcc-e2e-03/capture-pending-temp-D-state.cjs
- doc/tasks/20261016-dcc-e2e-03/record-six-groups-and-batch14.cjs

- doc/tasks/20261016-dcc-e2e-03/OV20-safe-hold.json

- doc/tasks/20261016-dcc-e2e-03/recover-batch14-OV20.cjs

- doc/tasks/20261016-dcc-e2e-03/close-current-context.cjs

- doc/tasks/20261016-dcc-e2e-03/verify-D-role-restored-OV20.cjs

- doc/tasks/20261016-dcc-e2e-03/capture-D-restored-entry-absent-OV20.cjs

- doc/tasks/20261016-dcc-e2e-03/close-task-browser-OV20.cjs

- doc/tasks/20261016-dcc-e2e-03/record-OV20-final-review.cjs

- doc/tasks/20261016-dcc-e2e-03/OV20-evidence-audit.json

- doc/tasks/20261016-dcc-e2e-03/finalize-OV20-closeout.cjs
- doc/tasks/20261016-dcc-e2e-03/OV20-final-handoff.json
- doc/tasks/20261016-dcc-e2e-03/OV20-closeout-preview.log

- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r2-cleanup-state.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r2-frontend-red.log
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r2-frontend-green.log
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r2-readonly.e2e.cjs

- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r3-handoff.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r3-backend-plan.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r3-backend-red-baseline
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r3-qualification-readonly.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r3-qualification-2026-10-19T11-51-02-347Z

- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-readonly-with-role.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-preparation-check.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-preparation-checks.json
- doc/tasks/20261016-dcc-e2e-03/run-r4-backend-test.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-backend-red-2026-10-19T13-59-32-439Z
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-backend-green-2026-10-19T14-02-17-363Z
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r4-backend-green-2026-10-19T14-04-40-324Z

- doc/tasks/20261016-dcc-e2e-03/verify-r5-runtime.cjs
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r5-handoff.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r5-artifact-audit.json
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r5-closeout-preview.log
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r5-role-ui-2026-10-19T15-24-47-809Z
- doc/tasks/20261016-dcc-e2e-03/repair-20261019-r5-batch14-ui-2026-10-19T15-24-56-531Z

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-current.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-ui.cjs
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-prep.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-checkin.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-formal.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-isolation.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-revision.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-save.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-upload.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-workflow.cjs

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-handoff.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-closeout-preview.log

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/A-initial-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/A-revision-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/artifact-audit.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/assignment-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/assignment-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/C-initial-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/C-revision-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-all-ready.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-all-three-ready.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-negative-RA.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-negative-RB.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-negative-SOURCE.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-RA-failure.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-RB-failure.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-real-save-failure.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-save-result.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkin-SOURCE-failure.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/checkout-confirmation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/closeout-preview-before-ready.log

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/closeout-preview-summary.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/command.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/confirmation-cancel-preserved.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/console-errors.jsonl

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/D-initial-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/D-revision-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-107.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-112.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-20.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-3.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-41.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-58.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-60.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/error-9.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/files/DCC261020C03R10090749-attachment-A.txt

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/files/DCC261020C03R10090749-attachment-B.txt

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/files/DCC261020C03R10090749-source.pdf

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/final-A1-isolation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/final-A1-old-attachments.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/final-A2-active.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/final-A2-isolation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/initial-active-before-revision.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/initial-draft-submit-confirmation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/initial-submitted-attachments.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/initial-submitted-readback.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/L-initial-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/L-revision-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/network.jsonl

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-application-preflight.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-application-preflight.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-draft-attachments-readback.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-draft-two-attachments.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-initial-active.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/new-working-RA-RB-only.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/old-A1-source-viewer.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A1-A.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A1-A.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A1-B.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A1-B.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A2-RA.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A2-RA.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A2-RB.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/preview-A2-RB.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-1.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-100.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-101.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-102.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-103.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-104.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-105.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-106.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-107.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-11.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-111.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-112.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-116.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-117.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-118.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-119.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-12.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-120.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-121.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-122.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-13.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-14.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-15.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-16.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-17.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-18.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-19.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-2.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-20.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-21.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-22.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-23.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-24.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-25.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-26.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-27.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-28.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-29.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-3.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-30.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-31.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-32.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-33.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-34.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-35.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-36.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-37.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-38.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-39.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-4.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-40.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-41.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-44.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-45.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-46.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-47.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-48.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-49.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-5.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-50.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-51.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-52.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-53.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-54.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-55.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-56.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-57.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-58.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-59.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-6.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-60.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-61.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-62.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-63.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-64.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-65.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-66.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-67.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-68.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-69.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-7.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-70.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-71.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-72.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-73.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-74.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-75.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-76.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-77.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-78.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-79.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-8.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-80.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-81.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-82.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-83.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-84.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-85.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-86.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-87.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-88.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-89.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-9.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-90.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-91.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-92.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-93.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-94.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-95.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-96.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-97.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-98.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/response-99.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-confirmation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-confirmation.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-files/DCC261020C03R10090749-revision-RA.txt

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-files/DCC261020C03R10090749-revision-RB.txt

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-files/DCC261020C03R10090749-source.pdf

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-formal-identity.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-formal-submitted.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/revision-prepared.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/run.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/runner-errors.jsonl

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/S-initial-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/S-revision-task.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/save-preflight-real-offline-failure-2.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-A-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-A-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-C-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-C-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-D-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-D-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-S-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/signature-S-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-A-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-A-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-C-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-C-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-D-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-D-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-L-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-L-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-S-initial.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/task-S-revision.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U-checkin-revision-submit.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U-final-new-old-isolation.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U-real-login-profile.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U-upload-submit.trace.zip

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U1-confirmation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U4-save-failure-2.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/U4-save-failure.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-A-failed.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-A-recovered.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-A.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-B-failed.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-B-recovered.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-B.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-SOURCE-failed.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-SOURCE-recovered.png

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-SOURCE-resume.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z/upload-SOURCE.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/assignment-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/assignment-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/calibration.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-all-ready.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-all-three-ready.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-negative-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-negative-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-negative-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-RA-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-RB-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-real-save-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-save-result.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkin-SOURCE-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/checkout-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/closeout-preview.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/command.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/confirmation-cancel-preserved.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/console-errors.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/D-initial-terminal-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/D-initial-terminal-readback.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/dispatch.cjs
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/error-008-submit-draft.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/error-047-preview-A.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/error-058-D-done-readback.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/executed-helper-manifest.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-A1-isolation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-A1-old-attachments.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-A2-active.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-A2-isolation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-source-proof.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/first-anomaly.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/handoff.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-A-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-active-attachments-A-B.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-active-before-revision.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-C-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-D-fresh-same-task-terminal.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-D-pending-after-timeout.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-D-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-draft-submit-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-L-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-S-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-submitted-attachments.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-submitted-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/initial-upload-submit.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/network.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-application-preflight.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-application-preflight.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-draft-attachments-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-draft-two-attachments.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-initial-active.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/new-working-RA-RB-only.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/next-steps.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A1-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A1-A.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A1-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A1-B.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A2-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A2-RA.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A2-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-A2-RB.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-history-A1-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-history-A1-A.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-history-A1-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/preview-history-A1-B.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-001-profile.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-002-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-003-prep.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-004-upload-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-004-upload-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-004-upload-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-005-confirm.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-006-save-offline.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-007-save-online.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-008-submit-draft.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-010-detail-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-011-prepare-entry.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-012-submit-draft.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-013-stop-initial-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-L-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-L-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-L-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-L-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-L-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-020-initial-S-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-A-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-C-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-030-initial-D-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-040-D-timeout-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-041-D-error-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-042-D-error-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-043-D-error-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-044-U-return.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-045-U-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-046-U-current.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-047-preview-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-050-U-redirect.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-051-verify-initial-active.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-052-preview-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-053-preview-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-054-active-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-055-stop-U-preview.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-056-fresh-D.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-057-D-readback-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-058-D-done-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-060-D-done-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-061-D-exact-original-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-062-D-terminal-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-070-U-revision-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-071-revision-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-072-browser.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-073-baseline.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-074-checkout.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-075-negative-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-075-negative-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-075-negative-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-076-checkin-ready.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-077-checkin-save.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-078-revision-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-080-prepare-revision-entry.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-081-revision-prepare.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-082-revision-submit.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-083-revision-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-C-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-L-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-L-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-L-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-L-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-L-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-090-revision-S-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-A-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-100-revision-D-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-110-U-final-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-111-final-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-112-final-A2.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-113-preview-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-114-preview-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-115-open-old-history.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-116-old-readonly.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-117-old-preview-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-118-old-preview-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-119-finish.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-120-final-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/response-121-close.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-A-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-C-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-confirmation.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-D-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-formal-identity.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-formal-submitted.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-L-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-prepared.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-S-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/runner-errors.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/save-preflight-real-offline-failure-1.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-A-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-A-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-C-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-C-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-D-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-D-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-S-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/signature-S-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-A-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-A-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-C-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-C-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-D-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-D-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-L-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-L-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-S-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/task-S-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/terminal-readback-requestfailed.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/U-checkin-revision-submit.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/U-final-A2-A1-isolation.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/U-real-login-profile.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/U1-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/U4-save-failure-1.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-A-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-A-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-B-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-B-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-SOURCE-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-SOURCE-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/upload-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/files/DCC261020C03R2R1-attachment-A.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/files/DCC261020C03R2R1-attachment-B.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/files/DCC261020C03R2R1-source.pdf
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-files/DCC261020C03R2R1-revision-RA.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-files/DCC261020C03R2R1-revision-RB.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/revision-files/DCC261020C03R2R1-source.pdf
- doc/tasks/20261016-dcc-e2e-03/rerun-r2-evidence-audit.py

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/closeout-check.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/final-validation.json

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r2-round1/R2-05-readonly-conclusion.md

- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/assignment-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-all-ready.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-all-three-ready.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-negative-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-negative-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-negative-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-RA-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-RB-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-real-save-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-save-result.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkin-SOURCE-failure.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/checkout-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/closeout-check.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/closeout-preview.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/closure.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/command.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/confirmation-cancel-preserved.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/console-errors.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/dispatch.cjs
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/error-067-S-terminal-readonly.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/evidence-audit.py
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/execution-log.md
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/final-source-check.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/final-validation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/first-anomaly-original-frame.jpeg
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/first-anomaly.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/gate.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/handoff.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/handoff.md
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/helper-final-manifest.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/helper-manifest.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-A-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-active-before-revision.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-C-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-D-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-draft-submit-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-L-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-S-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-submitted-attachments.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/initial-submitted-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/lifecycle.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/network.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-application-preflight.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-application-preflight.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-draft-attachments-readback.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-draft-two-attachments.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-initial-active.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/new-working-RA-RB-only.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/next-steps.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/pageerrors.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/preview-A1-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/preview-A1-A.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/preview-A1-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/preview-A1-B.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-001-profile.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-002-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-003-prep.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-004-upload-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-004-upload-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-004-upload-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-005-confirm.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-006-save-offline.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-007-save-online.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-008-prepare-entry.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-009-submit-draft.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-010-trace-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-C-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-L-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-L-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-L-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-L-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-L-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-020-initial-S-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-A-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-030-initial-D-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-040-U-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-041-U-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-042-browser.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-043-baseline.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-044-preview-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-045-preview-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-046-checkout-entry.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-047-checkout.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-048-negative-RA.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-048-negative-RB.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-048-negative-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-049-ready.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-050-checkin-save.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-051-prepare-entry.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-052-prepare-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-053-submit-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-054-stop-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-dialog.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-login.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-sign.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-task.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-060-revision-S-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-061-S-error-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-062-S-error-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-063-S-same-task-readonly.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-064-S-readonly-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-065-S-evidence-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-066-S-terminal-trace.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-067-S-terminal-readonly.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-069-terminal-final-snapshot.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-070-finalize-blocked.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-071-final-stop.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/response-072-close.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-confirmation.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-formal-identity.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-formal-submitted.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-prepared.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-S-first-abort-and-readonly.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-S-task.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/run.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/runner-errors.jsonl
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/runtime-check.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/S-revision-terminal-unavailable.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/save-preflight-real-offline-failure-1.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-A-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-C-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-D-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-request-events.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-S-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/signature-S-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-A-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-C-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-D-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-L-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-S-initial.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task-S-revision.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/task.md
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/U-initial-previews-checkin-revision-submit.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/U-initial-upload-submit.trace.zip
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/U-real-login-profile.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/U1-confirmation.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/U4-save-failure-1.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-A-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-A-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-A.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-B-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-B-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-B.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-SOURCE-failed.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-SOURCE-recovered.png
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/upload-SOURCE.json
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/verification-report.md
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/files/DCC261020C03R3R1-attachment-A.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/files/DCC261020C03R3R1-attachment-B.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/files/DCC261020C03R3R1-source.pdf
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-files/DCC261020C03R3R1-revision-RA.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-files/DCC261020C03R3R1-revision-RB.txt
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/revision-files/DCC261020C03R3R1-source.pdf
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-checkin.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-formal.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-isolation.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-prep.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-revision.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-save.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-upload.cjs
- doc/tasks/20261016-dcc-e2e-03/r3-20261020-workflow.cjs
- doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1.cjs

## 2026-10-21 原记录有界只读复核
访问被拒，原签名/任务终态UNKNOWN；见 readonly-20261021/handoff.md。

2026-10-21 已办有界只读复核：S fresh身份成功，已办路由404，筛选未执行，原终态UNKNOWN。详见 done-readonly-20261021/handoff.md。

2026-10-21正式菜单复核：源码存在已办；S实际展开审批中心仅待办，无已办，筛选未执行，终态UNKNOWN。见menu-done-readonly-20261021/handoff.md。

2026-10-21 U原申请复核：4392/A2原BPM已见S主管通过与22:36:45已校验签核，待会签；原task终态未直显。见applicant-readonly-20261021/handoff.md。

2026-10-21密钥恢复后隔离补验：原A2正式预览按钮生成DAILY预览被拒，冻结区无旧系统异常但尚未读取；按停点关闭，见isolation-key-restored-20261021/handoff.md及同原申请续办计划。
