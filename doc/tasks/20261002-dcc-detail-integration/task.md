# DCC 详情公共流程接线

## Root 本轮业务确认与 Review

用户已确认：失败升版重提仍原目标版本（A/2），失败申请/BPM/正文/属性/签名历史独立保留；项目/产品审核人由后台配置，提交时带出，不能猜admin。Root正式合同/HTML v1.4已更新，请与backend_public_repair协调实际接口，不再用这两项未知阻塞。

Root允许本Agent收口shared/lifecycle.ts及所属前端测试：按真实后端WORKING、CONTROLLED_PENDING_EFFECTIVE和本轮文控培训/受控/生效语义补状态/标签；当前有效统一改受控（已生效），旧“申请人上传培训记录”不能错误描述文控职责。保留历史状态读取，不擅自放宽操作权限或把待生效显示可执行。

Review需处理：INITIAL目前通过buildRevisionCommand(...,'PARTIAL',...)再覆盖为INITIAL，应抽取共用申请字段校验并用明确INITIAL载荷构建，不以另一业务动作绕类型合同；确认时冻结正文/实际属性/日期/部门/培训，换所选工作版本不得把默认来源展示错成另一版本。新增产品审核配置前端接线在本批详情依赖稳定后与后端协调；只用正式已配置账号，不扩大批准人规则。

## Goal
在主管理指定整合树接入 INITIAL、正式升版、独立作废、会签指派与同签名整改、受控后下发，保持真实身份、快照和失败。

## Milestones
1. BDD/RED 与正式 API 合同核对。
2. 复用模块组件接详情入口与申请表。
3. GREEN、局部 lint、主管理 Review。

## Expected Verification
仅运行新定向前端合同/单元测试与相关既有测试、局部 lint。Root 负责全量 type/build；无 E2E、服务、实库、提交推送授权。

## Current Status

ready_for_closeout — G44 normal UI human-audit identity software frozen: 1 System production + 2 tests, effectiveRED then184/11 PASS (overlap explicit), original41/OWNER/G43/Auth/policy unchanged. No actualDB, Redis, token, service, package or Git by this agent; Root review/deployment and realUI audit remain. Historicalevents untouched.


## G02 Root Review

本批没有已启动的子 Agent 写入这些文件，Root 暂接显式 INITIAL 构造与 shared/lifecycle 标签，保持唯一写入归属。

- Given 初始上传申请没有受控基线，When 构造 INITIAL 载荷，Then 直接校验共同申请字段并明确 INITIAL，不借 PARTIAL 或伪造基线身份；actual 深拷贝，日期/培训/部门非法时拒绝。
- Given WORKING、受控待生效和已生效文件，When 公共页面显示状态，Then 分别显示工作版本、受控待生效、受控已生效；培训说明责任人为文控，READY_TO_PUBLISH 不误报已生效。
- 定向单元 RED/GREEN、既有详情及升版组件行为回归、正式 type check 和局部 lint；真实 E2E 依赖运行环境，单元结果不冒称页面验收。

## G03 Project/product reviewer frontend

Root 接后台 Owner 已冻结的 GET/PUT reviewer-config API，唯一修改前端 projectProductRequests.ts、ProductCatalogTabPanel.vue 和独立审核配置组件/模型；后端仍由 backend_closure 独占，原四树不写。

- Given 未配置或停用的审核账号，When 打开新建/重提申请，Then 显示真实配置状态，提交前重新读取并拒绝无效配置，不指定默认admin。
- Given 有配置维护权限和文控角色，When 选择正式启用账号并填写原因确认保存，Then 调用正式配置API并核对回执；失败不提示成功，不改在途申请冻结审核身份。
- Given 待审核申请和当前用户，When 显示/办理审核，Then 仅本申请冻结审核人出现并可触发审核入口；当前配置变更不改变其资格，历史显示原提交时人员。批准沿用后端当前独立规则。
- Given Long字符串身份，When 配置、申请读取、审核或重提，Then 不发生Number精度转换；确认取消不发送写请求。

## 设计约束检查
- 写归属仅 detail/index.vue、独立详情组件、workflow.ts/applicationRead.ts 与对应新测试。
- defaults 与 actual 分离；历史不读当前默认；新申请初始化与草稿回显分开。
- 按服务器版本/任务/部门/权限事实接线；不制造轮次或默认成功。
- 正式升版允许较早合法小版；新版生效才废旧版；作废批准即结束。
- 用户明确本轮不提交推送，收尾记录 blocked 并交主管理。

## BDD
- Given 当前 WORKING 上传草稿，When 打开并正式提交，Then 保留草稿属性，提交 INITIAL、日期、部门、说明、培训并二次确认。
- Given 受控基线和服务器可选小版本，When 局部/换版申请，Then 提交实际所选正文、实际意图与本次属性；不加最新小版门禁。
- Given 部门负责人的正式 ASSIGN 任务，When 签名指派，Then 同一个载荷包括关联整改安排，实际会签仍独立签名。
- Given 已受控的当前文件，When 文控下发，Then 选择接收部门/人员/方式并二次确认，不改生效日期。
- Given 当前受控文件，When 独立作废，Then 从项目当前默认初始化并保存独立属性，显示文件版本并二次确认。

## Cleanup Keep
- doc/tasks/20261002-dcc-detail-integration/g25-legacy-name-occupancy-proposal.md
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-runtime-review-summary.json
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-review.md
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-mapping.py
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-mapping-contract.json
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-mapping.sql
- doc/tasks/20261002-dcc-detail-integration/test_g23_legacy_name_mapping.py
- doc/tasks/20261002-dcc-detail-integration/g23-legacy-name-preparation-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g21-rehearsal-driver-review.md
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-review.md
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-schema.py
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-schema-contract.json
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-schema-queries.sql
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-environment-query.sql
- doc/tasks/20261002-dcc-detail-integration/test_g21_postflight_schema.py
- doc/tasks/20261002-dcc-detail-integration/g21-postflight-preparation-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g21-preparation-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g21-structure-prerequisite-review.md
- doc/tasks/20261002-dcc-detail-integration/g21-structure-prerequisite-queries.sql
- doc/tasks/20261002-dcc-detail-integration/g21-structure-prerequisites.json
- doc/tasks/20261002-dcc-detail-integration/g21_structure_prerequisites.py
- doc/tasks/20261002-dcc-detail-integration/test_g21_structure_prerequisites.py
- doc/tasks/20261002-dcc-detail-integration/g21-prior-source-consistency.json
- doc/tasks/20261002-dcc-detail-integration/g19-working-browser-navigation-review.md
- doc/tasks/20261002-dcc-detail-integration/g19-configuration-review.md
- doc/tasks/20261002-dcc-detail-integration/g17-migration-execution-review.md
- doc/tasks/20261002-dcc-detail-integration/g14-migration-execution-review-manifest.json
- doc/tasks/20261002-dcc-detail-integration/g13-migration-closure-evidence.json
- doc/tasks/20261002-dcc-detail-integration/g14-migration-structure-evidence.json
- doc/tasks/20261002-dcc-detail-integration/g17-ledger-semantic-review-proof.json
- doc/tasks/20261002-dcc-detail-integration/g13-owner-ui-review.md
- doc/tasks/20261002-dcc-detail-integration/integration-notes.md
- doc/tasks/20261002-dcc-detail-integration/public-ui-closure-audit.md
- doc/tasks/20261002-dcc-detail-integration/g26-legacy-registration-plan.md
- doc/tasks/20261002-dcc-detail-integration/g26-legacy-core-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g26-legacy-core-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g27-legacy-final-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g27-legacy-final-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g25-legacy-migration-policy.json
- doc/tasks/20261002-dcc-detail-integration/g26-gxp-policy-prospective.yaml
- doc/tasks/20261002-dcc-detail-integration/g27-main-package-preflight.json
- doc/tasks/20261002-dcc-detail-integration/g27-main-package-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g28-schema.py
- doc/tasks/20261002-dcc-detail-integration/g28-schema-test.py
- doc/tasks/20261002-dcc-detail-integration/g28-driver.py
- doc/tasks/20261002-dcc-detail-integration/g28-driver-test.py
- doc/tasks/20261002-dcc-detail-integration/g28-schema-contract.json
- doc/tasks/20261002-dcc-detail-integration/g28-schema-capture.sql
- doc/tasks/20261002-dcc-detail-integration/g28-schema-row-counts.sql
- doc/tasks/20261002-dcc-detail-integration/g28-source-first.sql
- doc/tasks/20261002-dcc-detail-integration/g28-source-repeat.sql
- doc/tasks/20261002-dcc-detail-integration/g28-plan.json
- doc/tasks/20261002-dcc-detail-integration/g28-request-template.json
- doc/tasks/20261002-dcc-detail-integration/g28-execution-plan.md
- doc/tasks/20261002-dcc-detail-integration/g28-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g28-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g29-collector.py
- doc/tasks/20261002-dcc-detail-integration/g29-collector-test.py
- doc/tasks/20261002-dcc-detail-integration/g29-collector-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g29-collector-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g29-driver-r2.py
- doc/tasks/20261002-dcc-detail-integration/g29-driver-r2-test.py
- doc/tasks/20261002-dcc-detail-integration/g29-collector-r2.py
- doc/tasks/20261002-dcc-detail-integration/g29-collector-r2-test.py
- doc/tasks/20261002-dcc-detail-integration/g29-dump-parser.py
- doc/tasks/20261002-dcc-detail-integration/g29-dump-parser-test.py
- doc/tasks/20261002-dcc-detail-integration/g29-r2-plan.json
- doc/tasks/20261002-dcc-detail-integration/g29-r2-source-first.sql
- doc/tasks/20261002-dcc-detail-integration/g29-r2-source-repeat.sql
- doc/tasks/20261002-dcc-detail-integration/g29-r2-schema-contract.json
- doc/tasks/20261002-dcc-detail-integration/g29-r2-schema-capture.sql
- doc/tasks/20261002-dcc-detail-integration/g29-r2-schema-row-counts.sql
- doc/tasks/20261002-dcc-detail-integration/g29-r2-preserved-dump-read-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g29-r2-generated-dump-review.md
- doc/tasks/20261002-dcc-detail-integration/g29-r2-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g29-r2-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g32-manifest.py
- doc/tasks/20261002-dcc-detail-integration/g32-manifest-test.py
- doc/tasks/20261002-dcc-detail-integration/G32ManifestSupport.java
- doc/tasks/20261002-dcc-detail-integration/g32-input-contract.json
- doc/tasks/20261002-dcc-detail-integration/g32-execution-design.md
- doc/tasks/20261002-dcc-detail-integration/g32-runtime-extraction-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g32-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g32-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g34-basic-data-completion-audit.md
- doc/tasks/20261002-dcc-detail-integration/g34-matrix-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g34-matrix-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g39-owner-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g39-owner-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g39-maintenance-pin-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g39-maintenance-pin-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g39-conditional-answer-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g39-conditional-answer-verification-receipt.json

- doc/tasks/20261002-dcc-detail-integration/g43-development-entry-bdd.md
- doc/tasks/20261002-dcc-detail-integration/g43-development-entry-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g43-development-entry-verification-receipt.json

- doc/tasks/20261002-dcc-detail-integration/g43-auth-expiry-bdd.md
- doc/tasks/20261002-dcc-detail-integration/g43-auth-expiry-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g43-auth-expiry-verification-receipt.json

- doc/tasks/20261002-dcc-detail-integration/g43-auth-directory-bdd.md
- doc/tasks/20261002-dcc-detail-integration/g43-auth-directory-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g43-auth-directory-verification-receipt.json

- doc/tasks/20261002-dcc-detail-integration/g44-signature-readiness-review.md

- doc/tasks/20261002-dcc-detail-integration/g44-human-audit-identity-bdd.md
- doc/tasks/20261002-dcc-detail-integration/g44-human-audit-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g44-human-audit-verification-receipt.json

- doc/tasks/20261002-dcc-detail-integration/g44-project-product-upload-integration-review.md

## G05 detail relations / application history / selected working body

- Given 当前关联来源可能是同一 Master 的另一版本，When 详情同时加载当前关联和历史审批关联，Then 校验稳定 Master 与真实 source；旧页面只读，保存仅发送当前实际 source，历史保留原快照和版本。
- Given 上传、升版和已结束作废有正式 BPM/attribute round 映射，When 选择申请轮次，Then 精确请求该映射的 evidence，展示本轮属性与签名，不从签名字符串猜类型。
- Given A/2 有多次失败/重提，When 查看详情与版本历史，Then 展示申请尝试号、返工前驱、文件 ID 与实际 BPM，避免同号混淆。
- Given 已保存工作正文 defaultSource=CE、actual=FDA 而项目当前默认=NMPA，When 从受控详情选择该工作正文，Then 读取所选 selectedIterationId 的合法本人草稿，显示 CE/FDA 并保持提交同一正文属性；切换/晚响应不能串稿。
- 定向 RED/GREEN 使用 Node/Vue 运行时测试；不启动服务、读写实库、Git 或 E2E。Root 负责完整类型/构建与最终收尾。

## G06 详情实际依赖收口 BDD

- Given 正式OWNER可从他人工作正文发起换版，但原工作草稿仍限本人，When 明确选择REPLACEMENT，Then 读取replacement-attributes（精确baseline/selected IDs），生成独立申请，不调用原稿保存/恢复；PARTIAL不可提交提示不覆盖合法REPLACEMENT。
- Given 用户仅有关系名称可见权限，When 当前/历史关系加载，Then 只使用relation-permissions轻量正式身份/名称/预览投影，不请求更强详情或项目详情；当前集读取失败，历史快照仍可独立展示。
- Given 关系只读，When 目录接口不可访问，Then 不发目录请求、不挡历史；只有真正可编辑的当前源才加载正式目录，目录失败明确显示并关闭编辑。
- 新SFC/wrapper行为RED后GREEN，定向Node/Vue回归与lint；不启动服务/实库/Git/E2E或全量type/build。

## G07 公共浏览wrapper BDD

- Given 公共项目浏览未传选项，When 请求正式分页，Then latestVersionOnly=true；不由客户端聚合或重排。
- Given 用户明确选择全版本或真实状态，When wrapper读取，Then latestVersionOnly=false与精确status按合同传递，保持browserScope及服务器total/page；选择器selectorScope不变。
- Given 非boolean latestVersionOnly、无效/非精确状态或服务器返回不同状态/非最新行，When读取，Then明确拒绝，不能以默认值或筛选静默掩盖。

## G08 实际办理BPM属性轮次 BDD（G07-UI-04修复）

- Given file.native BPM=U而审批办理BPM=O，When 已核验任务列表真实processInstanceId与请求任务，Then 父组件给O选择上下文，子组件只按正式application-rounds唯一mapping读取O属性/签名；未核验路由不触发证据读取、不授予操作权限。
- Given 普通详情无审批路由，When 打开，Then 默认本版native BPM；没有正式mapping明确显示缺失，不回退另一申请。
- Given route/任务/文件变化与旧异步证据未回，When 新上下文核验，Then 同步清旧证据、旧列表/证据/核验响应不得落入新上下文。
- Given 请求外来/空/重复mapping或任务BPM失配，When 加载，Then 明确报错、清旧选中与证据，不能选择本版另一轮假装当前办理证据。
- 服务端DccApplicationHistoryGuard已核对真实BPM tenant/definition/native businessKey或OBSOLETE FormCenter objectId/version；本轮不修改API/后台权限，继续用正式证据服务最终鉴权。

## G11 UI-03 跨项目候选目录 BDD

- Given Source/引用目标属于项目A且已选文件不为空，When打开统一selector并在左侧分页/搜索选正式项目B及逻辑folder，Then用B+folder精确selector查询，顶部Source、原已选/写目标仍A；项目根无folder不发送非法project-only查询。
- Given 项目/目录请求未完成已切项目、关闭或卸载，When旧响应返回，Then不能覆盖新目录/候选页或重新打开；仅打开才请求，不占服务/实库。
- Given 名称授权项目或目录读取失败，When选择左侧目录，Then左侧局部错误可见，显式全局搜索仍可查询授权候选；禁止NAS推断、回退旧树或前端拼total。
- 正式getProjectDiscoveryPage服务器分页/Long、getProjectFolders/buildProjectFolderTree结构验证；独立loader和真实转译SFC行为RED→GREEN，保留G10 auto-open单次消费。

## G14 G13负责人签名公开handler修复 BDD

- Given点击A提交后readiness仍挂起，When切route/file/BPM/task或关闭再重开同task（ABA）并新弹框B就绪，Then旧A调用不得续签B；readiness返回true且点击上下文/表单仍完全相同才可确认。
- Given本次确认后进入签名transport，When原上下文改变或ABA后旧成功/失败返回，Then仅在原记录准确提示核对，不关闭新弹框、不释放新busy、不覆盖新owner/密码/错误；已提交成功不能误写为失败重提。
- Given二次确认取消或表单变更，When处理结果，Then取消零写保留输入，修改要求重新确认；payload使用入口冻结file/task/owner/form值。
- 明确signature credentials只存在内存、不进入日志/报告。生产唯一detail/index.vue/approval-actions.ts及必要独立helper；专属真实父handlerRED→GREEN；Root全量types/build，禁止后台/公共workflow/服务/实库/Git/E2E。

## G17 迁移执行准备只读 Review BDD

- Given18根/44闭包与真实MySQL只读schema/ledger日志，When逐SQL/列/索引/生成表达式比较，Then区分旧hash已应用、结构已满足未登记、真实新增缺口及seed/config待核对，不能缺账本即重跑。
- Given base/catalog旧ledger哈希与当前不同，WhenRoot提供精确历史来源/语义diff，Then重建原文校验ledgerSHA、核对当前真实结构及数据变更边界，保留原ledger；不acceptedEquivalent不同语义、不重播历史seed。
- 仅报告/manifest结构验证，无生产SQL/源码/DB连接写入、Git/服务/Maven/types/build。Root准备最终具体数据库授权。

## G19 项目属性与审核配置修复 BDD

- Given 项目/负责人ID为正式Long字符串，When打开并保存项目属性配置，Then exact字符串保持；unsafe number、外来项目响应、目录重复/缺负责人/账号身份非法均明确拒绝且零写，不以类型注解替代真实守卫。
- Given 读取/保存尚未结束已关闭/换项目/卸载，When旧响应返回，Then不能串入新项目默认值/负责人/ready/busy或emit新项目成功；已成功旧写只准确提示原记录核对。
- Given 真实登录角色仅super_admin（无doc_control），When打开审核配置，Then无业务角色资格、不读配置/账号、不写；doc_control+真实project-code:update权限正常，不修改通用checkRole或后端。
- Given 审核保存二次确认挂起后角色/账号/权限变化，When确认返回，Then重检原授权上下文且零写；配置晚响应/关闭ABA/卸载不改新弹框。
- 生产仅ProjectAttributeConfigurationDialog.vue/ProjectReviewerConfiguration.vue、projectAttributes.ts中configure包装及必要自有helper；真实SFC/API/permission转译有效RED→GREEN，局部lint；禁止types/build/Maven/服务/DB/Git/E2E。

## G21 17项外部结构前置合同 BDD

- Given g20外部前置25项，When扣除后台Owner8项BPM/策略/notification并对正式SQL+protected schema/default/index/generated日志建立合同，Then17项逐项有目标表/列/nullable/collation/default/unique顺序和表达式，历史原SHA保留，缺ledger不造APPLIED。
- Given新19候选正准备加字段，When验证base前置，Then只要求当前真实必要的旧基础结构，不要求本次owner/attributes/source/attempt/lifecycle待加列先存在。
- Given同名但错shape、缺列、nullable/collation/default失配、wrongunique顺序或错误generated表达式，When离线validator读取事实，Then明确FAIL并指出migration/事实；合法只读fixture通过。
- 本轮只写自己的SELECT/JSON/validator/negative tests与记录，Root执行只读数据库采集；不连接库、不改生产/正式SQL/Root文件，不服务/Git/Maven/types/build。

## G21 postflight最终schema BDD

- Given19候选正式SQL/已保护旧schema，When离线推导升级最终目标，Then17新表/51新增nullable列、reason2000、P1/A/C约束撤换及最终320表达式均精确断言，不只是同名字段存在。
- Given首跑与重跑，WhenRoot只读采集同一schema并validator，Then最终指纹一致；错列type/nullable/charset/collation/default、错unique顺序/表达式/前缀或generated语义拒绝。
- Given新SQL未显式指定的DB/charset默认，When验证，Then要求Root先冻结真实environment事实并解析引用，不猜默认collation/engine；仅此运行项待采集，不伪造数据库PASS。
- 仅自有g21-postflight准备工具/SQL SELECT/JSON/validator与negative tests，无DB连接/正式SQL/生产修改，Root未来授权执行及rehearsal。

## G21 演练driver独立只读 Review BDD

- Given driver/contract/prepared/postflight与Root支持模块冻结，When无flag、输入漂移或异库进入，Then对应门禁必须在transport前拒绝；全链原库仅只读及固定新clone创建，不把flag当实际授权。
- Given受保护922 schema/210 data恢复与19原SQL首跑/重跑，When比较历史同count但payload、exact33/new19/原6与schema，Then任一差异明确失败，first schema FAIL不进入repeat，每phase raw分开保留。
- Given演练clone来自backup snapshot、fresh前置来自当前原库，When给结论，Then明确只能证明snapshot演练，不冒称当前原库业务数据可迁移。
- 本Agent只写独立报告/记录，可运行26离线tests/AST和安全negative；不得actual authorize、DB/服务/Git/Maven/types/build/E2E或改生产/tools。

## G23 历史25 claim只读映射 BDD

- Given tenant1当前25 active claim且C migration只加nullable原名列，When后续新上传SOURCE preflight/countUnresolvedNames，Then全租户新申请failClosed；不得改守卫或把19候选偷偷扩为回填。
- Given claim→Master→全部版本→actual source_file_id→infra_file.name，When只读采集，Then保留exact Long/tenant/version/原名UTF8 bytes/sourceSHA，ownership/ticket仅真实关联交叉检查；normalized_name/title/原受控PDF不当源文件名。
- Given多个版本不同源名、缺Master/源ID/metadata/源SHA、跨tenant或证据冲突，Whenvalidator，Then列UNCONFIRMED并保留理由；数量变化/字段缺失/重复/unsafeID拒绝，不能截断25或默认成功。
- Given完整metadata映射，When形成review，Then仅PROPOSED_VERIFIED_METADATA_MAPPING且source_bytes_verified=false、write_authorized=false；Root纯SELECT采集，当前不DB/生产/正式SQL/历史行/服务/Git/Maven/types/build。

## G25 历史口径新确认与occupancy设计 BDD

- Given用户确认保留全部历史原名占用，When已核验39正文证据并激活独立scope，Then历史多Master同名和本Master多名按多对多保留，不更新旧File/Master/claim/签名；新NEW exact重复拒绝。
- Given actual selected ownMaster正文与正式C/权限完整，WhenREVISION/replay使用旧占用，Then核精确服务器intent和本版证据，不能因legacyoccupied非空一律误拒，也不能因sameMaster让NEW重名通过。
- Givenmetadata候选无实际bytes或任一39证据missing/mismatch，When登记激活，Then全部拒绝且正式unresolved gate不解除；新3表/配置与原19迁移分别审查执行。
- 本轮仅design proposal/记录，Root唯一DBOwner与生产分配；无Java/正式SQL/Maven/DB/服务/types/build/Git。

## G25 Core Current Verification / Limitations

- Real existing NameClaim mapper RED: full valid sealed evidence still countUnresolvedNames=1 (expected0); g25-legacy-behavior-red.log exit1. Class missing test is scaffold only.
- Independent review two-scope contamination RED: PREPARED evidence same claim/file returned UNVERIFIED.pdf alongside legitimate SOP.pdf; g25-legacy-two-scope-red.log exit1. Formal mapper exact outer scope is being fixed.
- Same-source historical projection uses independent copies, preserves old row NULL. NEW and authorized existing version server entry points are separate; raw sameMaster NEW is forbidden.
- Existing C public creation Master uniqueness continues to prevent complete old-number re-creation after20years. NameClaim/registry layer tests do not establish public end-to-end reuse. Root reviews separate safe design; no Master/history/unique rewrite in this batch.
- Standalone new migration empty3 sidecars is separate from frozen19; no actual DB execution by this agent.

## G27 Final Delivery / Cleanup Keep

- doc/tasks/20261002-dcc-detail-integration/g26-legacy-registration-plan.md
- doc/tasks/20261002-dcc-detail-integration/g26-legacy-core-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g26-legacy-core-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g27-legacy-final-delivery-fingerprints.json
- doc/tasks/20261002-dcc-detail-integration/g27-legacy-final-verification-receipt.json
- doc/tasks/20261002-dcc-detail-integration/g25-legacy-migration-policy.json
- doc/tasks/20261002-dcc-detail-integration/g26-gxp-policy-prospective.yaml
