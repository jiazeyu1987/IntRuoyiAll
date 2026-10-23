# 当前：R3 round1（2026-10-20 22:34）

本轮C05-A/B/C/D全PASS，无新产品问题。新file2054545668044084389/A1，待主管审核；完整本轮报告rerun-20261020-r3-round1/verification-report.md，唯一交接rerun-20261020-r3-round1/handoff.json。21证据/4trace净化CRC/隐私和资源关闭PASS。业务已结束，禁止Git使仓库收尾独立blocked。下列旧当前/最终等称谓均仅历史轮。

---

# E2E-05 当前验收结论（2026-10-20 R2第一批）

runId=rerun-20261020-r2-round1，业务run R2C05mvg833oh；C05-A/B/C/D全部PASS，无本轮业务失败/重试或新产品问题。当前后端PID41508/48061，新Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147，前端PID20976/8061。原账号本轮重新登录，5账号组织只读核验通过。新fileId2054545668044084330/A1，单次提交后待主管审核，3私有配置保存/重开一致，新表单3项空，独立S新待办及公共矩阵前后不变通过；D实际为无公共配置分支，已有规则预览不适用。

本轮完整报告：[rounds/rerun-20261020-r2-round1/verification-report.md](rounds/rerun-20261020-r2-round1/verification-report.md)，独立交接：[handoff.json](rounds/rerun-20261020-r2-round1/handoff.json)。21证据验证PASS，隐私472文件/37338ZIP条目PASS，4context/browser/wrapper/runner全部关闭。先ready_for_closeout后preview exit0；按当轮保留现场/禁止Git要求不执行apply或删除，仓库closeout独立blocked。业务已结束，HOLD，不执行第二批。

---

以下为历史轮次记录，旧“当前”“最终”等状态仅针对各自历史轮，不作为本轮新包验收依据。

# E2E-05 “其他”自定义矩阵 — 当前 PID16776

## 最终业务结论：C05-A/B/C/D 全部 PASS，待 Root review

当前运行 **PID16776 / 48061**，Jar SHA `494306625fb1d04261832e4145a8aa83c3734257c02069e9b1e7644749efcdec`，Root overnight-runtime-proof、更新的isolation proof与dispatch精确一致。原fixture复用，没有因PID变化造数据；同一工作稿没有重传/重建/重复提交。

| 分支 | 实际结果 | 关键证据 |
| --- | --- | --- |
| C05-A 私有矩阵保存/重开/提交 | PASS：原fileId正式浏览行重开，三项实际值与保存一致；标题/WORKING/未绑定BPM/原件文件名/0普通附件回读；精确二次确认后同fileId进入待主管审核，四阶段实际人员/部门一致 | evidence/U34096mvb8ugne/resume-1792176592523/result.json，detail-readonly-green.json、applicant-draft-reopened-actual-values.png、applicant-exact-submission-confirmation.png、applicant-submitted-route.png |
| C05-B 新申请隔离 | PASS：同U重新进入上传并选原P1/其他类型，会签部门/批准人/文控人三项均为空 | evidence/U34096mvb8ugne/post-submit-1792177140418/applicant-fresh-application-empty-private-fields.png，result.json三项空数组 |
| C05-C 主管仍取组织/独立新待办 | PASS：确认框明确主管组织来源，路线实际S；独立S新增目标编号待办，打开后精确fileId/taskId/BPM及待审状态一致 | 同post-submit目录supervisor-new-supervisor-todo.png和result.json；写前待办0保留于原run |
| C05-D 公共矩阵不改 | PASS：同观察账号、同类型编码/路径/唯一类别检查、同类别行，前后均新增可见/预览禁用，实际配置事实深比较一致，无新增公共矩阵 | 原run observer-before-* 与同post-submit目录observer-after-*，result.json before/after |

最终身份：fileId **2054545668044084181**，编号 **DCC241016C05-U34096MVB8UGNE-A**，版本 **A/1**，BPM **7c253fee-c992-11f1-8f08-b082e25ec548**，主管taskId **7c25671b-c992-11f1-8f08-b082e25ec548**。停在待直属主管审核；仅打开待办核对，未办理主管签名/后续审批。

最终汇总 **final-result.json / execution-result.json**，完整后置结果 **evidence/U34096mvb8ugne/post-submit-1792177140418/result.json**。该结果显式引用实际A-PASS原运行，A没有重复提交。B截图QA发现原全页视口未包含私有区域，已仅将本例截图限定该真实区域并重跑C/B/D只读后置；原PASS结果保留，未重做A。打开S待办自然触发reading-session/start留痕POST，新申请自然触发route-preview POST，如实记录，不能宣称全部零POST；无再次上传/working/submit/approve。

维护后两TDZ和详情只读FORUPDATE错误未复现，同稿详情加载/附件区实际GREEN。此前upload-bind保存错误也已越过；历史RED分别保留不删除。前置/业务fixture最小修正全部来自真实页面：U岗位、S自有角色查询权限、本例类别U UPLOAD/VIEW与S VIEW；没有改其他账号/类别或公共审阅矩阵。生产修复/迁移/服务由Root负责，本线程只改自身测试资产、使用自身业务数据。

已抽看最终私有配置空态、提交路线、主管待办截图；脚本10项结构检查PASS。此为四业务分支PASS，**不是流程终审通过或任务completed**。44项fixture清理记录与2项当前业务记录保留给Root，另有历史失败上传/ticket未知单列；cleanupAttempted=false/cleanupCompleted=false，无撤回/删除/Git写入。详情见residual-register.json。清理/Root review及仓库最终提交不在本线程自动执行范围，下方所有旧“当前/新阻塞”章节均为保留历史。

## 历史阶段：草稿已保存，业务曾阻于详情初始化

- 最新Root调整完整watch依赖顺序后，resume-1792174477005新context原U真实浏览行重开同fileId，无pageerror，两TDZ错误均未复现；组件开始渲染。但详情数据仍为空，编号-、版本未记录，DOM显示系统异常/发布后续加载失败，因此详情业务GREEN尚未取得。
- 新C05-PRE-07：同期后端02:15:19.707 SQLState25006/error1792 “Cannot execute statement in a READ ONLY transaction”，关键调用链AttachmentService.readOrdinaryAttachmentManifest:177→requireAttachmentVersion:252→selectByIdAndTenantForUpdate。detail-readonly-transaction-blocker.json/md已单列实际DOM/GET/日志证据；旧TDZ JSON不覆盖。
- 本次全部自然GET，未重传/重建/保存/提交，同一草稿2054545668044084181仍保留；私有值/附件未读成功，不能宣称四业务分支通过。待Root按正式只读/锁合同修后继续，本线程没有生产修改或服务操作。下方TDZ阻塞为历史。

- 最新Root移动currentUserId后再用新context原U经真实浏览行重开同草稿；旧currentUserId错误未复现，新3条pageerror为Cannot access 'controlledFileId' before initialization。compiled Vite detail/index.vue:269/290；源早期approvalProcessInstanceId依赖controlledFileId，被delayedConfirmationIdentity/watch读取时controlledFileId尚未初始化。最新证据evidence/U34096mvb8ugne/resume-1792173677766/result.json、detail-initialization-blocker.json（旧currentUserId证据嵌套保留）。
- 本次仅只读请求，未重传/重建/保存/提交；详情空白，尚不能验证三私有值/附件。按Root明确边界停止原fileId2054545668044084181，无生产修改。四业务子例尚未PASS；下方currentUserId错误为上一轮历史。

- Root upload-bind策略修复证据已核对PASS：固定test-only操作，副本/实际首次与重复均通过，原policy/events/ledger行不变。旧U34096mvb5mr4i无file/事务回滚由Root确认；原页面已随browser关闭，不重放旧ticket，旧临时资产仍保留未知。
- 新run U34096mvb8ugne 源件真实重传、单次保存后已导航详情，fileId2054545668044084181。浏览列表显示同编号规范为大写 DCC241016C05-U34096MVB8UGNE-A，版本A/1、工作版本。原审计保存阻塞已实际越过。
- 原重开严格编号匹配因大小写失败，修正本例脚本以页面真实大写编号，从同一已保存草稿继续，禁止新上传/保存；恢复结果独立 evidence/U34096mvb8ugne/resume-1792172142993/，原result保留。
- 新真实阻塞C05-PRE-06：浏览目标行“操作面板”进入同fileId后详情空白，原trace捕获ReferenceError：Cannot access 'currentUserId' before initialization。对应detail/index.vue早期computed/watch读取currentUserId，定义晚于watch。detail-initialization-blocker.json/md 保存源码/trace线索，未修改生产detail/index.vue。
- 当前fileId存在但未完成私有值正式重开/最终提交、无BPM，四业务子例未PASS。保留同草稿待Root修复后恢复，不重新创建。下方审计保存RED为历史，不作为最新结论。

- Root 接管后未再次启动浏览器或重试写入。离线提取原 applicant-trace.zip 中保存点击后截图，+2114ms清晰显示“GxP 审计策略不存在或未启用：dcc.project-file-placement.upload-bind”；精确操作已由原页面证实。evidence/U34096mvb5mr4i/original-save-frames/05-2114ms.jpg，原trace未修改，原保存点击1次。
- 原trace.network为0字节，因此该次自然响应业务码不可从现存证据恢复；源码ErrorCode映射1002032000明确只作源码依据，observedNaturalBusinessCode=null。没有为了拿业务码重放保存。985B源件预览可见，但ticketId/绑定/存储/DB状态未知，继续保留现场。
- four-branch-script-review.md 已只读复核四分支：A计划覆盖保存/正式重开/三项回显/精确提交/路线，实际停于保存；B/C/D完整链未执行。明确最近5待办缺全量核对路径、映射弹框未直接显示精确类别ID等限制，未新增包装/无关防御或改生产。Root修复通知前不再重试。

- Root 演员安全数字规范修复（3组22tests PASS、Vite热更新）后，原 U/P1/其他类型真实预检显示“审批路线已就绪，共4个节点”。证据 fixture-evidence/1792165210298-DCC241016C05-fixture-final-preflight.png、root-workflow-resume-result.json；Root 已接受前置 GREEN。旧定义/native账号/矩阵身份错误均未复现，下方旧 RED 为历史。
- 原 U/S/观察账号各自登录开始完整05。类别映射/公共矩阵写前只读检查通过，同一类别无公共矩阵。S工作台原缺待办面板，仅给其任务自有角色增加既有 bpm:process-instance:query，重新登录已显示面板，其他账号/共享角色未变。
- U34096mvb4xvq4、U34096mvb50b16、U34096mvb55hju 是业务写入前的脚本定位/权限准备轮，均无上传/草稿/申请残留；结果及trace保留。修正仅在本例脚本：真实Dialog包装器定位、combobox aria-controls限定当前下拉并等待关闭。
- U34096mvb589zj 的源件预览 HTTP400 已定位本例类别无权限规则。已仅给本例类别增加U上传/查看、S查看，保存一次并回读3条启用规则；未修改项目负责人、其他账号/类别或公共矩阵。类别配置时一次下拉点击被全局表格控件遮挡，发生在保存前；改用唯一搜索结果的真实键盘选择后通过，失败trace保留。
- 最新完整轮 U34096mvb5mr4i：源件预览 DOM 成功、4节点路线已就绪；点击保存申请草稿，自然 POST working HTTP200，页面仍停上传页，60秒未进入详情，未当成功或重复保存。记录了2项业务残留：已预览上传源件、工作稿保存尝试；无可见fileId/BPM。
- 随后使用原U只读全局/全部版本，精确编号 DCC241016C05-U34096mvb5mr4i-A 查询0条，零写请求；只证明当前账号可见范围没有该记录。evidence/U34096mvb5mr4i/save-readonly-inventory.json 保存独立trace/截图，原失败结果未覆盖。
- 同秒本机运行日志记录 2026-10-17 00:01:16.128 serviceExceptionHandler 与 GxpAuditServiceImpl.append:50；源码该行在审计策略不存在/未启用时抛GXP_AUDIT_POLICY_NOT_FOUND。日志未给具体operation/message，Root需核对精确审计策略及事务/ticket状态。新阻塞 C05-PRE-05，详见 working-save-blocker.json/md。共享审计策略超出本例最小账号资格范围，本线程未擅改或绕过。
- C05-A 原始运行 FAIL 停于保存，B/C/D完整前后链未执行完成；没有四业务子例PASS。root-workflow-resume-result.json 保留预检PASS并另列最新业务阻塞，prepared/manifest/execution-result当前描述均已同步为新运行态及新阻塞，旧PID43284/C05-PRE-04仅作历史。
- manifest.status=UI_VERIFIED 仅表示本例路线前置真实通过；当前业务状态与资源残留分别记录。原发布/恢复/重保存/诊断/RED全部保留，未覆盖。没有生产源码/服务/Maven/Git直接写操作。

## 最新自然预检响应技术诊断

- 按 Root 新增明确授权，原 U 只通过真实页面重走配置，记录自然预检响应的少量类型/空值字段，没有主动 API 请求。preview-transport-diagnostic.json 独立保存，不覆盖原业务预检/manifest/resume 证据；Root 唯一负责生产 helper/index.vue 修复。
- 最后一次自然响应 HTTP200/code0、approvalSourceMode=APPLICATION_CUSTOM、matrixId 不存在、routeId=null、approvalMatrix=null、ready=true。applicationCustomMatrix 两演员 ID 均是 number，canonical 内对应 ID 和 requesterId 均为 string。
- 同页 DOM 仍为“审批矩阵身份缺失或精度损失”；业务判断 businessReadinessPassedByDom=false，不能将响应 ready=true 记为通过。当前新 blocker 的字符串/数字传输冲突已有自然响应技术证据，具体源码调用栈未采集。
- 技术诊断正常结束，未上传/保存/提交任何文件。没有落完整响应、canonical 全文、票据或 headers；trace 登录后开启且 snapshots=false。完整 C05 四子例仍 BLOCKED。

## 最新 PID34096 恢复预检：C05-PRE-04

- Root 的 workflow-deployment-verified.json 已为两个精确模型设置 editorSourceIsolationVerified=true、metadataUnchanged=true，10 历史资源/97 绑定保留；workflow-definition-digests-after.json 的两摘要与原批准 V2 精确一致。本例已逐字段校验，恢复预检与主 E2E runner 绑定新 runtime proof/dispatch，不沿用旧 PID43284 proof。
- 原 U 于北京时间 23:23 再次登录，真实选择 P1、私有文件夹、合法自定义类型、会签部门/A/D；route-readiness 由旧“系统异常”变为精确“审批矩阵身份缺失或精度损失”。4 次自然 POST route-preview HTTP200，仅作为请求元数据，业务断言来自 DOM。没有读取响应 JSON。
- 当前 Root 核验支持原定义问题已修复，页面也未复现旧定义/账号异常；本轮新前置仍 FAIL，manifest 不标 UI_VERIFIED。新 blocker=C05-PRE-04，完整四子例均 BLOCKED，未启动完整业务 runner、未上传源文件、未保存草稿/正式提交。
- 只读代码定位：approval-matrix.ts:31 identity 只接受 string；:103 customValue 校验两演员，:246 accept 比较请求/响应 custom matrix；后端 DccApplicationCustomMatrix.java:4 暴露无字符串标注的两个 Long，YudaoJacksonAutoConfiguration.java:35/NumberSerializer.java:31 对安全整数输出 number。当前 A/D910375/910376 在安全范围，是本次错误的高置信合同冲突线索；未捕获实际响应载荷，不能把静态推断标作运行字段证明。详细证据/边界见 matrix-identity-blocker.md。
- root-workflow-resume-result.json 已记录当前 runtime/选择/DOM/截图/自然请求/trace/新阻塞；截图 fixture-evidence/1792164218487-final-preflight-failure.png。原发布/恢复/重保存日志未覆盖，旧 manifest 归档 rounds/before-isolated-runtime-resume。
- 本轮脚本语法/主 runner 10 项结构检查 PASS，但配置预检真实 FAIL；没有生产代码修改、服务/Maven/Git 写操作。待 Root 处理传输合同后按既有授权继续，无须再次确认正常 fixture 准备权限。

## 最新隔离新包 V6 仅重保存执行

- Root 明确放行后，核对 workflow-isolation-runtime-proof.json 与最新 dispatch：PID34096，Jar SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce，health=UP、testGreen=true，3 隔离/25 相关测试 PASS。新 proof SHA 为 7230f4cc929d56d942e07e678209f426e623c76cf9e4ecec531067d4f6606642；没有放宽成任意 PID 或修改原批准。
- --resave-v6-only 实际于北京时间 23:19:28、23:19:50 各保存一个批准 V6 XML，合计两个自然 PUT /admin-api/bpm/model/update HTTP200，全部自然非 GET 请求仅这两项；0 发布。UPLOAD/REVISION 均通过自然只读模型 GET 的精确 ID 核对，不按版本号猜行。
- 页面 GREEN：保存后原可见名称/分类/范围/类型/表单/部署标签完全一致，六条历史行的 ID/版本/文本完全一致，现有上传 v6=49065d37-c970-11f1-aae9-b082e25ec548、升版 v6=59c2e12a-c970-11f1-aae9-b082e25ec548 均保留。原 approval/deployment-result/restore-result 指纹保持不变。
- 独立证据 resave-v6-result.json、resave-v6-journal.json、deployment-evidence/1792163999691-resave-v6-only-trace.zip。实际脚本退出 0，仅代表 UI 保存与回读通过；编辑源真正分离、canonical 与已发布 V2 一致、现有部署资源/10 旧资源/97 历史绑定/metadata 不变仍由 Root 只读核验。
- 本轮没有 U 预检、正式文件上传保存/提交或完整 C05，没有服务/SQL/API 直接写入/Maven/Git 操作。按最新边界停止，待 Root 资源验收与后续放行。隐私扫描 PASS：215 文件、22093 ZIP 条目，无凭据输出。

## 隔离新包 V6 仅重保存准备

- 已在现有真实 UI runner 增加独立 --resave-v6-only 模式及纯本地 --check-resave-plan。执行必须读取 Root workflow-isolation-runtime-proof.json，要求 ready=true、backendPid 为新整数、jarSha256 为新包完整 SHA256、health=UP、testGreen=true，且 PID/SHA 与最新 dispatch 精确相同；旧 PID43284/旧 Jar 明确拒绝。旧模式的原批准 PID 核对没有放宽。
- 新模式限定原批准两个精确模型和 V6 XML SHA，要求现有上传 v6=49065d37-c970-11f1-aae9-b082e25ec548、升版 v6=59c2e12a-c970-11f1-aae9-b082e25ec548，页面只保存两次、零发布。保存前后可见配置、六条历史行/定义 ID 一致，Root 负责编辑源分离及部署资源/历史实例/metadata/canonical 的只读验收。
- 原 approval/deployment-result/restore-result 使用指纹保护；独立 resave-v6-result.json 与独占 resave-v6-journal.json 留痕，成功或不确定均不自动重试。proof 文件及 dispatch 在每次保存前和回读后重新核对。
- 本轮执行仅语法检查、14 项纯本地 gate 检查及 --check-resave-plan，均 PASS。未启动浏览器，未请求服务，未保存/发布或运行 U 预检；不是重保存/完整 E2E PASS。resave-v6-prepared.json 已可交 Root 核对 proof 字段及执行命令。
- 维护窗口内继续保持暂停；proof 到位后仍须 Root 执行放行。未来重保存并完成 Root 只读核验后再用原 U 预检，完整 C05 等正式放行。

## 最新 U/S 原生资格复核

- Root 已验恢复 PASS：10 旧资源哈希、97 历史实例绑定及 metadata 不变，两新 v6 canonical 精确 V2。22:46 的新日志为 NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED，已经越过旧定义错误。Root 代码检查表明先校验申请人 U，再校验主管 S，同名异常不能直接归因 S。
- 真实用户页 RED：U910373 启用，修改弹框岗位为空；S910374 启用且已绑定 DCC241016C05审批岗位，未修改 S。按最新授权仅给 U 选择既有 DCC241016C05POST 并保存一次，自然 PUT /admin-api/system/user/update HTTP200；未改其他账号。
- 首次保存后回读因页面初始列表与筛选加载竞争失败，保留 native-account-ui-result.json 及 trace，没有重复保存。只读 --read-only 重新加载、等待初始表格完成后再查询，GREEN：U/S 均启用且岗位回显 DCC241016C05审批岗位。native-account-ui-readback.json 记录零写请求与独立 trace/截图。
- U 直属主管弹框两次重新打开均显示精确 S 用户名和昵称，无“已失效”。S 的唯一用户行 ID 为 910374；关系弹框本身不显示数值 ID，证据明确区分页面对应关系与直接呈现值，未解析接口 JSON。
- 当前只证明 U 缺岗位已通过真实页面补齐并持久化；未重跑预检，不能断言 NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED 已消失。tenantId/postIds DTO 投影由 Root 继续核对；本例保持流程导入/发布、预检、正式文件业务与服务操作暂停，等待统一新运行态放行。C05 四子例仍 BLOCKED。
- 证据隐私扫描 PASS：199 文件/20244 ZIP 条目，无凭据输出。

## 最新共享流程委派与紧急恢复（覆盖下方旧状态描述）

- Root 批准后通过真实页面精确核对 tenant1 两个 model-v4 ID，并导入批准 V6 XML，各保存一次、发布一次。UPLOAD 新 v6=49065d37-c970-11f1-aae9-b082e25ec548，REVISION 新 v6=59c2e12a-c970-11f1-aae9-b082e25ec548。deployment-result.json 保留实际页面回读、自然请求和截图；它证明 UI 部署动作完成，不证明历史资源安全验收通过。
- Root 随后的历史校验发现 seed 编辑资源与旧部署资源别名，导入保存污染两个旧资源。按 Root 最新授权，通过相同精确模型的正式导入页面保存 workflow-definition-before 两个旧 XML，原始 SHA 分别 19168160c63d827fa7a2adf613651724f434058e0e8a7511b468990fd271b07f / 832eeb34ac311d615115b4c49889b5910843b1f6a17b47d06f7b778370b8d759。
- 恢复脚本 --restore 实际完成两次保存、零发布；可见表单/范围/类型/名称及 v6 标签未变，历史页定义 ID/版本/文本完全一致。restore-result.json 与独立 restore journal/trace 保存证据，未覆盖 deployment-result.json。旧资源字节恢复和新 v6 资源未变须由 Root 只读哈希复核，不以 UI 历史行替代字节验证。
- 发布后、收到紧急暂停前，本例 U 独立登录重走原项目/类型/会签/A/D 配置，route-readiness 仍为“系统异常”；fixture-finalize 退出非零，不记 UI_VERIFIED。无源文件上传、草稿保存或正式申请。截图 fixture-evidence/1792162022662-final-preflight-failure.png；自然请求和完整页面文字见 post-deployment-route-readiness.json。
- 最新边界：保持暂停，不预检、不发布、不重新导入 V6、不执行业务，不操作服务。Root 负责资源隔离修复、映射加载、48061 健康和后续放行。C05 四子例继续 BLOCKED，未运行完整业务脚本，未清理现场或做 Git 写入。
- 首次 --execute 在写入前仅因版本标签空白定位失败；零写日志已原样归档 deployment-evidence/prewrite-selector-*.json，修正后才执行实际两次发布。当前脚本使用独占写前 journal，任何已写成功/不确定都禁止自动重跑。

## 结论

**新 BLOCKED：C05-PRE-03，上传流程定义合同不匹配。**

旧缺列/类别保存阻塞已通过本例真实页面复验解除；P1 项目申请由本例审核账号处理到 `COMPLETED`。本轮已实际准备岗位、签名授权及签名图片，并由申请人进入正式上传页配置私有矩阵，但审批路线显示“系统异常”。同秒运行日志为 `EXECUTION_CONTRACT_UNREGISTERED_DEFINITION`。

配置预检是本轮真实 RED；四个完整业务子例均未取得 PASS，不把前置恢复或项目完成当整例成功。

| 子例 | 本轮状态 | 实际边界 |
| --- | --- | --- |
| C05-A 私有矩阵保存、正式重开、提交并应用 | BLOCKED | 会签部门、批准人、文控人已真实选入；路线定义合同校验失败，未能继续草稿/提交链 |
| C05-B 新申请不继承私有三项配置 | BLOCKED | 原私有草稿未能保存，不能完成跨申请持久化隔离验收 |
| C05-C 主管仍取组织及新增精确主管待办 | BLOCKED | U→S 组织关系已保存并回读；无本次正式 DCC BPM/主管待办 |
| C05-D 同一类型公共矩阵前后不变 | BLOCKED | 没有完成业务写入前后对照；公共矩阵及流程定义没有修改 |

本轮资料：[manifest](fixture-manifest.json)、[执行结果](execution-result.json)、[运行身份](rounds/unblock-pid43284/runtime-proof.json)。旧轮报告/manifest/结果保留在 rounds/before-schema-unblock；旧截图/trace 保留原路径。

## 运行身份与授权

- 实际分支 `int_qms`；前端 8061、后端 48061；Root 当前证明 backend PID43284、frontend PID36084。
- Jar SHA256：`caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff`。dispatch 与 unblock-final-runtime-health.json 已交叉核对，health UP/官方登录 PASS 只作运行前置。
- 当前 gate=execute、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture、PER_CASE_REAL_UI_MANIFEST；共享 false 没有被用作阻塞或被伪改为 true。
- 只维护本例目录和 DCC241016C05 自有数据。使用已有无项目指派的本例配置账号；管理员原项目指派未改变、原目录未重复创建。本例没有服务、SQL/API 调用、数据库迁移、生产源码或 Git 写操作。

## 已完成的真实页面前置

- **自定义类别恢复**：独立配置账号保存并重开 `DCC241016C05CUSTOM` / `DCC241016C05其他类别`，来源为 APPLICATION_CUSTOM，绑定原私有目录，默认分类为合法的“技术文档/设计和开发策划阶段/DCC241016C05自定义其他/…/DCC241016C05自定义正文”。[类别重开截图](fixture-evidence/1792151963330-category-restored.png)。旧缺列不再作为当前 blocker。
- **P1 完成**：通过正式项目/产品申请入口创建 `DCC241016C05P1`、`DCC241016C05PRODUCT`、任务私有文件夹模板及文件夹。已有普通项目审核员角色从正式项目页的审核方式与唯一可见角色选项核对，仅增加本例账号的角色绑定；共享角色定义和菜单定义不变。`DCC241016C05Rmva4c8zyapprover` 独立登录后页面审核到 COMPLETED，项目负责人为本例 U。
- **审批人员前置**：本例 S/A/D 通过用户页绑定 `DCC241016C05POST`；专用部门负责人已在部门页保存/回显。管理员只为本例三账号启用电子签名授权；三账号各自在新 context 登录，从“我的签名”上传带本例标识的测试图片，页面均显示“已启用”。这些是 UI 配置回显，不宣称未执行到的服务端完整路线检查已通过。
- **申请人选择**：U 实际从工作台“上传文件”进入上传页，选中 P1、私有文件夹、其他类型、本例会签部门、A、D；页面项目产品来源为“已批准项目产品目录”，可见产品目录ID632、关系ID25、创建申请ID26。未上传正文、未创建 DCC 工作版本、未提交 DCC 申请。
- 原五个账号仍为本例账号：U910373、S910374、A910375、D910376、配置观察账号910377。岗位、签名图片、权限/组织绑定及本轮证据见 manifest；没有管理员冒充业务人。

## C05-PRE-03：Given / When / Then 与原始 RED

- Given 最新运行身份，合法 APPLICATION_CUSTOM 类别与已完成 P1；本例 U、S、A、D 独立账号和组织关系、岗位、签名授权/图片已从真实页面核对。
- When U 在上传页选定本例项目/文件夹/其他类型，再配置本次会签部门、批准人 A、文控人 D，等待正式路线预检。
- Then 应展示可核对的正式路线结果；实际 `dcc-upload-route-readiness` 持续显示“系统异常”，没有可用的提交/草稿验收链。

命令：`node fixture-setup.real.e2e.mjs --finalize-only`，本轮 cmd.exe 下实际退出 **2**。被测动作全部由真实页面完成；没有直接调用 route-preview，也没有解析其响应 JSON。

- [20:40 本次上传页失败截图](fixture-evidence/1792154447249-final-preflight-failure.png)
- [同次 trace](fixture-evidence/1792154422656-DCC241016C05-fixture-final-preflight-trace.zip)
- 自然页面请求：4 次 POST `/admin-api/dcc/controlled-files/route-preview`，HTTP200；它们来自人员选择引发的页面预检，HTTP200 不被当作业务成功。
- [运行日志摘录](rounds/unblock-pid43284/workflow-definition-log-excerpt.txt)：20:40:31.745、20:40:32.227，`DccWorkflowDefinitionCatalog.require -> DccApplicationExecutionContractService.verify -> preparePublicPreview` 抛 `EXECUTION_CONTRACT_UNREGISTERED_DEFINITION`；原日志位置为 output/runtime/int_qms/logs/dcc24-deptfinal/application.log，调用继续到 `DccControlledFileWorkflowServiceImpl.previewCustomRoute`。
- 源码 `DccApplicationExecutionContractService.preparePublicPreview` 对普通上传选定 PUBLIC_MATRIX_NATIVE_V2，并由 immutable catalog 验证 process key / contract / 完整模型摘要；当前异常只证明这组身份未通过校验，不能凭页面版本号推断确切图摘要。

## 只读流程定义诊断与 GREEN 门禁

从正式“流程模型”页筛选 `dcc-controlled-file-upload`，只读打开“更多 -> 历史”。页面有四个匹配模型行，显示 v1/v2/v3/v5；历史页可见版本至 v5，v5 定义编号为 `d20e0658-c70e-11f1-891b-b082e25ec548`。正式导航实际落点 `/bpm/manager/definition?key=dcc-controlled-file-upload`。

[流程历史截图](fixture-evidence/1792155166846-DCC241016C05-workflow-definition-history.png)。只读诊断最终退出 0。页面没有提供规范化完整模型摘要，未从 API JSON 补造该摘要，也不将历史列表顺序冒充选中的精确活动定义。

GREEN 为 BLOCKED。需 Root 核对当前正式批准的上传模型、已部署精确定义与 PUBLIC_MATRIX_NATIVE_V2 catalog 的匹配关系；本轮明确不修改公共配置，因此没有“发布/恢复/修改”共享流程或追加兼容分支。恢复后先以同一 U 重新核对路线正式就绪，再由本例真实 manifest 放行 C05-A/B/C/D；无需重新申请已有账号/E2E 授权。

## 验证、隐私和残留

- 主脚本、fixture 准备器及安全环境启动器 `node --check` 通过；10 项结构检查通过。静态 guard 只允许 `response.request()` 读取自然请求 method/path/status，继续拒绝直接 APIRequest 和注入动作。结构通过不计业务 PASS。
- 完整验收脚本在真实 manifest 未完整可用时，实际退出2并生成4个 BLOCKED；其业务验收浏览器未启动。此前用于准备和复现的浏览器确实执行过上述真实业务页面，不能误写为“未做 E2E 页面操作”。
- trace 均在登录完成后开始；密码创建/首次改密段不在 trace/截图内，snapshots=false/sources=false，未保留网络载荷。凭据只在进程和 CurrentUser DPAPI 私有文件中，不提交或分发。最终字节/ZIP 隐私检查见 evidence-privacy-check.json。
- 当前 **40 项残留/关联清理项**：23项旧轮条目已更新类别成功等状态，另增加私有模板、岗位及三账号岗位绑定、三签名授权/三图片、P1/产品/文件夹及请求、部门负责人、本例项目审核员绑定。计数包含实体和关联，不能当作40份文件/申请。
- cleanupAttempted=false、cleanupCompleted=false，保留现场给 Root review。后续只从真实页面清理本例数据及关联，尤其只移除本例用户的既有项目审核员绑定、不删除共享角色；审批请求审计按产品规则保留。没有 DCC 正文上传或正式 DCC BPM 申请需要清理。

## 最终证据检查
- final-evidence-structure.json PASS：四分支结果、唯一提交/后置0提交、同fileId/BPM、16个关联证据文件存在、manifest/prepared/root-resume最终状态一致；D是before/after截图结构，单次结构检查读取键修正后通过，未重复业务。
- 隐私扫描PASS：377文件/31018ZIP条目，无凭据值输出；主脚本10项结构检查PASS。
- 现场保持：44fixture清理记录+2当前业务记录，历史失败ticket另列；未执行cleanup apply/撤回/删除/Git提交推送，Root review前不标completed。


## 2026-10-20 第一轮独立重跑（当前结论）

四适用分支全部 PASS，无遗漏，原始 run R20mvg0qn28，未继承历史 PASS。新编号 DCC241016C05-R20MVG0QN28-A，fileId 2054545668044084288，BPM ae462725-cc27-11f1-a8a5-b082e25ec548；单次提交，止于待主管审核。完整本次报告 rounds/rerun-20261020/verification-report.md，汇总 rerun-20261020-result.json。2次前置校准原始结果保留，不列产品缺陷。浏览器/context/runner 已关闭，21产物验证及隐私扫描 PASS。保留所有历史及本轮现场，不执行下一轮。仓库 closeout 因当轮禁止 Git 写入独立 blocked。


## R2 第一批 PREPARE_ONLY（非业务结果）

runId=rerun-20261020-r2-round1；准备完成并hold。全部C05-A/B/C/D为NOT_RUN，不复制旧PASS。五原账号凭据经本地DPAPI内存检查可用，Playwright/源件存在，两个新入口语法及原业务函数不变检查PASS。执行门禁ready=false拒绝--execute，未启动浏览器/context/业务runner，未发新业务动作。等待Root新包READY和单独放行；prepared.json、prepare-validation.json、preparation-handoff.json保存在本例rounds/rerun-20261020-r2-round1目录。
