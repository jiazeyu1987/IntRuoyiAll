# G04 执行记录

## 2026-10-14 预检
- 已读根 AGENTS.md、docs/backend-development.md 和 frontend-development.md 的通用与 DCC 适用规则、task-closeout-rules.md、powershell-encoding.md、测试发布前置规则；未发现子目录 AGENTS.md。
- 已读 coordination/requirements.md 与 coordination.md；当前 WRITE_GRANTED 只覆盖本任务明确授予文件。
- git branch --show-current -> int_qms；HEAD 822c31fc0856516cf59ba9cd84bf2bc7f455d17f。工作区大量预存并行改动，不做基线提交、不回滚、不清理他人资产。
- 当前后端 project 用 selected.containsAll(required) 拒绝移除原必选部门；前端 resolvedUploadApprovers 对全部 stage 的 required/● applied=false 一律拒绝；空提示为“请选择至少一个会签部门”，与需求不一致。
- 已读当前 DccPublicMatrixNativeDerivationTest、AssigneeResolverTest、dcc-upload-signoff-departments 和 runtime-approval-groups 测试；不复用旧报告 PASS。
- 首次定位测试目录误用 service/route，文件实际位于 service/file；未修改缺失路径，后续已核对正确位置和指纹。
- 计划任务私有 javac/JUnit 定向编译，禁止 Maven 和共享 target 写入；只读 target 依赖用于 isolated developer test，不能声称完整工程编译或运行验收。

## RED（本轮实际执行）
- node --test tests/unit/dcc-upload-removable-signoff.test.cjs -> exit 1；5 tests，3 pass / 2 fail。实际 helper 拒绝移除 A 的 applied=false，精确空提示仍为旧文案。
- ./doc/tasks/20261014-dcc-gap-removable-signoff/run-private-tests.ps1 -Phase red -> exit 1；当前生产源码定向 javac 成功，JUnit 31 tests，26 pass / 5 fail。删除 A/增补名单被 APPROVAL_MATRIX_MANDATORY_SIGNOFF_REMOVED 拒绝；空名单原因/提示不符；解析路径同样失败。
- RED 输出只在任务 red/ 内；javac 的警告诊断被默认编码解码为乱码，测试中文输出正常。后续命令显式增加 -J-Dfile.encoding=UTF-8，原 RED 日志不重写。
- 更新旧专属测试中“删必选拒绝”的过期预期；仍保留非法身份、非部门会签、批准组及原政策校验。

## 最小生产修复
- project 仅移除 containsAll(required) 保留约束，仍校验正数/唯一和非空，原 RuleGroup 的 required/marker 原样保留，applied 反映本次参与。
- front helper 仅允许 SIGNOFF 源组未参与，不改变批准或文控原必需组守卫；指定精确空提示。
- resolver 显式空名单走正式 ROUTE_NOT_READY + 指定提示，仍不读取部门目录；非法身份继续 runtime mismatch。

## M01 冻结与前端 GREEN
- Root 转发 G12 唯一 Maven M01 RED 票冻结通知后，立即冻结 BE 生产/测试写入；未启动共享编译，BE GREEN 待票回收。
- 冻结时 BE 生产 SHA256：derivation=fed8661e22be80d1cd8cc7d8295d9767cc30176dac47b6033b46281c4b80d440；resolver=505527cf5f369afd23078881c2cda5e09b3d0cac413ae872fe9b69a2ec85834a。
- node --test tests/unit/dcc-upload-removable-signoff.test.cjs tests/unit/dcc-upload-signoff-departments.test.cjs tests/unit/dcc-upload-runtime-approval-groups.test.cjs -> exit 0；24 tests / 24 pass / 0 fail。包含实际 TypeScript helper 和既有页面预检片段执行、局部真实接口类型校验；无全类型/构建，无 E2E。
- 只读继续追踪当前 Workflow 将 candidateSourceIds 逐部门生成 obligationId；派生节点保持 ALL/100/true。完整 B/C 实际推进验证需 Root 集成票或本任务隔离引擎测试，不能用路线字段断言冒称真实签名运行验收。
- FE 增补专属页面预检回归及精确提示断言后，上述三文件命令重新执行 -> exit 0；25 tests / 25 pass / 0 fail。实际 refreshRouteReadiness 发送并保留 B/C，公共 baseline 原CSV与本次下发默认数组未改。
- 冻结期间只读 DccFrozenApprovalSignatures：当前原生最终门禁逐 selected department 核对本轮唯一指派、指派签名与实际会签通过证据；不能以重复 B 或同负责人一次通过代替 C。计划解除后对当前源码增加自有专属回归，不改该共享门禁。

## R02 默认预检复核（M02 只读阶段）
- Root 指出 null 分支 optional 非空拒绝可能阻断首次默认加载；本轮核对 int_qms，追踪当前真实 upload refresh -> matrix.begin(undefined) -> route-preview DTO -> Workflow PUBLIC overload -> resolvePublicNative -> project(null)。
- 结论：确有源码调用链阻断；合法可选会签来源导致 ready=false/nodes=[]，首次两份默认名单不回填；手工会签后 createDefaultPreview().begin() 同样无选名单，也会阻断下发初始复制。原条件未改不能证明符合新要求。
- 详细来源、精确锚点、只读指纹、BDD、最小生产差异与真实 RED 方案见 default-preview-review.md。
- 按 M02 禁止修改 BE 生产/测试、禁止共享编译；该默认分支尚未新增/执行 RED，不冒称已运行复现。前次 FE 25/25未覆盖 BE optional 默认派生，报告边界同步更新。

## A07 续工
- 2026-10-15 Root 释放 BE 源冻结并回收 M02；本任务继续已授范围私有编译，Maven仍须Root唯一票。本轮实际分支再次核对为 int_qms。
- R02 改前生产 hash 与前轮冻结一致：derivation=fed8661e22be80d1cd8cc7d8295d9767cc30176dac47b6033b46281c4b80d440；resolver=505527cf5f369afd23078881c2cda5e09b3d0cac413ae872fe9b69a2ec85834a。
- 首次默认 BDD 在生产修复前新增真实 project(null)、原生 route-preview 与 HMAC/预检合同测试；最终显式 B/C 测试将调用当前冻结签名门禁，覆盖遗漏/拒绝/旧轮次与同负责人重复证据。
- 私有 runner 增加只读编译当前 Workflow/Readiness/Token/FrozenApprovalSignatures 源码作为软件合同消费端（不修改这些生产文件），所有输出仍仅自有任务目录。其余依赖读现有classpath，不代表完整工程源码编译。
- default-red 首次扩编 exit 1：当前 Workflow 需要 BPM DTO.setProcessDefinitionId，旧依赖 class 里缺少该方法，测试尚未执行，不称业务 RED。后续只读补编当前 BpmProcessInstanceCreateReqDTO.java 到任务 private-classes，未修改 DTO 或共享 target。
- RED ./doc/tasks/20261014-dcc-gap-removable-signoff/run-private-tests.ps1 -Phase default-red -> exit 1；定向当前源码编译成功，39 tests / 35 PASS / 4 FAIL / 0 skip。3 项实际 project(null) 与1项实际 Workflow.PUBLIC preview 的 optional 默认分支失败；最终预检返回 PUBLIC_MATRIX_DERIVATION_UNSUPPORTED + OPTIONAL_SIGNOFF_SELECTION_REQUIRED。该真实结果补足前轮静态判断。
- R02 最小修复只改 project(null)：正式 required/optional 部门集合求全量并集且排序；原来源校验、RuleGroup.marker/required、批准规则和显式名单不变。
- GREEN ./doc/tasks/20261014-dcc-gap-removable-signoff/run-private-tests.ps1 -Phase green -> exit 0；当前指定源码39 tests / 39 PASS / 0 fail / 0 skip。包含当前 Workflow.PUBLIC preview的null默认成功、显式删除/增补真实预检、真实技术HMAC绑定名单与当前完整签名证据门禁。
- 前端首轮新增默认测试命令26项/1 FAIL，原因自有测试在matrixAfter与approvalMatrix共享同对象的fixture上重复push节点（测试fixture失真，未改生产）。已在自有测试去重来源引用；完整三文件Node命令重新执行 -> exit 0，26 tests / 26 PASS。首次默认两份数组独立，随后B/C保持，下发A/B不变；共享fixture零修改。
- REGRESSION run-private-tests.ps1 -Phase regression -> exit0，76 tests / 76 PASS / 0fail / 0skip；额外当前FrozenApprovalSignaturesTest、NativeSupervisorSourceTest、RouteReadinessServiceTest只读回归。全部显式编译来源前后hash相同SOURCE_STABILITY: PASS，未写共享target。
- 最终三生产hash与8份生产/测试manifest已记录；review-ready发出文件归还请求，停止写这些生产/测试，让Root按G01/G03顺序接管。没有跨聊天自行发消息。
- 将状态先设ready_for_closeout，执行task_closeout.py --task-id 20261014-dcc-gap-removable-signoff --mode preview -> exit0；12份自有长期证据/runner在keep，phase私有class/args/log在delete，没有范围外资产。长期报告与private-verification-manifest已归档4阶段发现数/失败名/源码hash及日志hash，原私有证据仍可供Rootreview。
- 按task-closeout-rules“当轮禁止Git提交/推送”条件设blocked（仅工程收尾），软件实现/验证结果保持通过。后续cleanup apply应如实记录状态门禁，不能为伪completed移除阻塞。
- cleanup apply实际返回BLOCKED task status is blocked，外层命令exit1；没有删除任何资产，不标PASS。任务工程状态不表示软件验证失败，Root可继续review原私有证据。
- Root反馈已接受R02最小diff及39/39、26/26、相邻76报告；本线程最终再核对manifest8份SHA256全部匹配，review-ready顶段已移除旧M02冻结且增加正式归还回执。Resolver/Derivation/FE helper及所属测试停止写入，Root可接管转G01/G03。不再重复私有大测试，工程reactor等Root最终唯一票。
- A13正式接收：Root核验归还回执、接受软件Review，另实跑前端2文件15项PASS。Resolver已转G01 V2接入，Derivation/signoff-helper由Root保护；G04全部源码/测试保持冻结，未复跑测试。本次只更新自有交接记录；39/76私有与FE26报告不扩展为完整reactor、部署或E2E，后续manifest/工程收尾由Root统一。

## A32 CUSTOM上传前端协助开始
- 用户/Root追加协助，原task目录追加里程碑而不新建thread/taskdir；已读G03 upload-three-handback.md/json与当前正式DTO/API，分支int_qms。
- 3文件hash实际核对完全MATCH：index F50AAFC702F9830542C99DF57FFC8A13C7D23E33A83C5D5FEED26FD89C20FBFF；submitter 0FDEEAAC685E3E042F8B39E57282B1181B6DAF50B1AE8F94E3AE433AB81762A0；approval-matrix B3C8B7C84635F3796BC2AC6FA00472B8A43CDCED2C8B28763E721B1265D3C364。
- 当前可见缺口：saveUploadDraft仍强制matrix.after（CUSTOM无public after）；confirmation facts无文控及custom配置；payload未核实际输入两账号与selection一致；CUSTOM state accept未核服务器canonical/context/phase/实际部门。先按BDD加实际测试再修，保留缺陷/fixture失败分类。
- A32基线现测：G03 editor11/state3/page2与IC13 state29保留；旧application-page45因fixture缺selectedFileTypeTaxonomyPath/isCustom依赖及scopeKey新增sourceMode发生假失败，只补该test自身依赖后45/45 PASS，生产未为fixture降级。
- A32 RED node --test tests/unit/dcc-upload-a32-contract.test.cjs tests/unit/dcc-upload-application-page.test.cjs -> 53 tests / 46 pass / 7 fail：canonical阶段/上下文/部门、RESERVED错阶段、payload旧选择、确认缺custom/docControl、实际CUSTOM草稿公共after守卫、项目切换保留旧custom。确认测试还需单独核VNode文本helper，避免把fixture错误当业务RED。
- Root M09-am票进行中，本任务仅Node/前端/只读，不运行Maven/fulltype/build/sharedtarget或依赖其可能更新中的class。
- A32类型确认清旧关联测试真实RED1/1FAIL（旧关联8保留），最小清理后GREEN1/1PASS；已保存CUSTOM精确preparedV1被V2替换测试真实RED1/1FAIL，增加实际合同/精确定义绑定守卫后定向11/11PASS（含G03当前Panel两项消费者回归，未修改Panel/test）。
- 核心A32+PUBLIC state/page组86/86曾通过；上传所有专属回归首轮缺依赖fixture修正后254/254PASS。旧VM测试仅补实际新增依赖，未删除业务断言、未修改公共fixture或做生产fallback。
- Root新增R09小授权：仅signoff-departments.resolvedUploadApprovers PUBLIC原组guard接受V1/V2已登记执行合同，派生仍V1；与A32一次归还，不复跑BE39/76。
- R09改前helper SHA256=c7e891e82468706cdb0597f14c6c7f529710d59939b32b9573a4c97800b8c5ac。实际helper RED：node --test --test-name-pattern=R09 tests/unit/dcc-upload-runtime-approval-groups.test.cjs -> 2项1PASS/1FAIL，V2原组被原V1-only守卫拒绝；unknown/raw原组仍正确拒绝。生产只把两处V1分支条件换为登记V1/V2谓词，不改变原派生版本或后续规则断言。
- R09先2/2GREEN，再加共用V2的CUSTOM明确来源回归出现3项2PASS/1FAIL（误当PUBLIC raw），按正式approvalSourceMode+custom derivation区分后3/3PASS。没有让PUBLIC来源落旧扁平分支，也没有给CUSTOM伪public ID。
- 最终合并Node回归：实际展开全部dcc-upload-*.test.cjs + dcc-detail-g03-custom-application.test.cjs -> exit0，260 tests /260PASS/0FAIL/0skip；4生产源运行前后hash一致。精确命令展开、manifest、loghash保存a32-r09-verification.json。只读G03当前Panel恢复/保存/确认2项也在该组合内，未改Panel/test。
- a32-upload-before记录初始3文件与R09 helper原hash；a32-upload-after记录4生产与11所属测试最终hash（15/15核MATCH）。git diff --check限4文件exit0（仅常见LF/CRLF提示，无空白错误）。没有Git写操作。
- review-ready顶段已更新A32+R09一次正式归还，G04停止4生产/测试写入，后端/公共API/Panel最终接入G03/Root。原G04 BE39/76与旧manifest作为历史证据保留，未复跑，不冒新工程或运行验收。
- A32/R09收尾：9份交接/JSON文件UTF-8可读；15/15最终文件hash再核MATCH；fresh结果JSON为260/260且source_stability=true。先ready_for_closeout执行cleanup preview exit0，保留长期报告/改前后/验证manifest，临时class/log/args属于本任务；按Git当轮禁止规则转blocked，不标completed。生产和测试保持归还冻结。
- A32/R09 cleanup apply按blocked状态实际拒绝（命令exit1；BLOCKED task status is blocked），零删除，未写PASS；Root仍可读取原私有/Node日志。无需重复大测试或自动提交推送。
