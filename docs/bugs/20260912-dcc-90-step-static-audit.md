# DCC 90步全流程静态检查：逻辑缺陷记录

## 追加静态检查（2026-09-13，排除010）

- 本轮重新核对第1—90步，明确排除DCC-STATIC-010；未重新判断或修改010的既有状态。
- 新确认12项逻辑问题：DCC-STATIC-016至027，P1 6项、P2 6项；DCC-STATIC-016、021、022、023 已完成当前源码层修复，017至020及024至027仍为OPEN_STATIC_CONFIRMED。
- 001—015的历次修复汇总属于历史记录；本节追加不同触发条件或不同根因的问题，不将新问题混入旧修复结论。
- 逐步覆盖、当前源码指纹、排除理由及文档校验见 `doc/tasks/20260913-dcc-90-step-followup-audit/verification-report.md`。
- 证据为当前工作区代码、页面契约及随库流程/表结构；未执行产品测试、构建、E2E、API、数据库或线上配置核验。并发项明确采用代码交错推导。

## 审计范围与证据边界

- 用户请求：核对本对话第1—90步DCC流程并登记bug。
- 日期：2026-09-12；工作区：E:/IntRuoyi；基准HEAD：6c6487c9f151244910b9ff80454c397bc303e330。
- 以工作区实际源码为准，包含前次修复及其它既有未提交改动，不代表已部署版本。
- 检查方式：页面、请求、Controller、Service、Mapper、随库流程定义和现有测试的交叉静态核对。未执行产品测试、构建、E2E、API或运行数据库操作。
- 最新代码复核（2026-09-13，第三轮）：DCC-STATIC-001至016、021、022、023已完成当前源码层静态修复；010已补齐统一受控候选关闭及连续检入后的返工祖先解析。追加审计登记的017至020及024至027仍为开放项。
- P1表示正常业务闭环被阻断或正式内容/授权/影响证据受损；P2表示条件性恢复失败、入口错误或展示不一致。确认的是所列条件下的代码逻辑，不声称线上已发生。
- 完整90步覆盖、源码指纹、文档校验与收尾证据见 doc/tasks/20260912-dcc-90-step-static-audit/verification-report.md。

## 最新源码复核（2026-09-13，第三轮）

- **结论**：DCC-STATIC-010 的两个重开断点已补齐；001至016、021、022、023当前源码层均为 FIXED_STATIC_VERIFIED。追加审计项017至020及024至027仍按 OPEN_STATIC_CONFIRMED 保留。
- **010断点一**：修正版本重新送审时，旧A/1退回流程会同步取消 BPM、关闭DCC文件行，并调用统一受控候选 withdraw，释放 open candidate 后再登记新候选。
- **010断点二**：返工祖先识别已沿同 Master、同申请人、同 Revision 的 predecessor 链回溯，可越过 A/2 等 WORKING 修正版本找到原 A/1 退回版本并终结。
- **验证**：DCC-STATIC-001、002、003、006、007、008、009、010、011、012、013、014、015 静态合同 PASS；DCC 工作版本提交、平台适配器、审批工作流相关 150 项 Maven 定向测试 PASS。
- **边界**：本轮按用户要求只做静态合同和定向单元测试；未执行E2E、服务启动、数据库写入或线上配置核验。

## 缺陷索引

| 编号 | 级别 | 涉及步骤 | 问题 | 状态 |
|---|---|---|---|---|
| DCC-STATIC-001 | P1 | 3、9、10、11、12、28、48、55 | 新项目缺少正式负责人和编制权限的配置闭环 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-002 | P1 | 10 | 产品建档申请关闭弹窗后无法恢复审批，重复申请又被拦截 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-003 | P2 | 13、14、22、26、29、33、48 | 模板允许保存非末级分类，但按该模板新建上传必然失败 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-004 | P2 | 35、37、47、48、49、50 | 工作稿创建成功但响应丢失后，页面预检挡住同键重试 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-005 | P2 | 49、50、51、52、68、69、85、86、88 | 工作中和待发布状态缺少受保护预览通路 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-006 | P1 | 36、43、44、52、54、56、57、58、59、60、86、88 | 检入后审批预览旧原件，签名证据却绑定新源文件 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-007 | P1 | 40、52、54、56、77、79、80、88 | 检入新小版本不继承关联文件，发布时丢失影响范围 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-008 | P1 | 11、12、16、50、51、52、55 | 检出和检入缺少正式项目编制及类别上传权限复核 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-009 | P2 | 52、55、86、88 | 检入重放只比较上传票据，未绑定操作者和修改载荷 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-010 | P1 | 51、52、56、62、63 | 退回申请人后只能签名继续，无法完成文件内容修改 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-011 | P1 | 66、69、71、74、75、76 | 已保存电子分发名单中的人员停用后仍生成必需培训任务 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-012 | P2 | 73、74、75 | 重新开始培训会话时覆盖刚补记的阅读时长 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-013 | P1 | 36、52、54、77、80、88 | 影响评估关联B/1后发布B/2，修订跟踪无法自动完成 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-014 | P2 | 36、50、51、52、77、88 | 浏览页“升大版本”按钮的资格与后端OWNER规则不一致 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-015 | P2 | 51、52、53、88 | 同一人检出另一小版本时返回成功，却未切换实际检出基础版本 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-016 | P2 | 10、16 | 产品建档审批入口误用创建权限，只有审批权限的人员无法续办 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-017 | P2 | 11、12、16、55 | 不存在的角色、部门或岗位可保存为唯一负责人，正式授权实际无人获得 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-018 | P1 | 15、47、56、57、58、59 | 路线可保存审批方式、比例及必需开关，但实际审批仍按固定流程模型执行 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-019 | P1 | 15、47、56、57、58、59 | 同一审批环节可重复配置，启动流程时后续同环节人员被静默丢弃 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-020 | P2 | 15、47、54、56 | 审批路线生效时间未参与选用，未来路线保存后立即替代现行路线 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-021 | P1 | 42、43、44、52、56、57、58、59、60、86 | 工程图纸已上传配套PDF，工作稿及审批预览仍选择无法在线显示的CAD源件 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-022 | P2 | 51、52、88 | 仅修改备注的检入能力存在于后端，但正式检入页面强制上传新文件 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-023 | P2 | 52、55、88 | 检入重放校验使用未转义文本拼接，不同修改载荷可被误判为同一次请求 | FIXED_STATIC_VERIFIED |
| DCC-STATIC-024 | P1 | 73、74、75、76 | 并发开始培训阅读可留下多个活跃会话，同一时间段被重复累计 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-025 | P2 | 36、52、77、88 | 大版本创建时的正式基线未继承到后续小版本，历史响应也未返回该字段 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-026 | P1 | 68、69、70、76、78 | 发布重试再次失败后沿用同一事件键，后续重试无法恢复统一状态 | OPEN_STATIC_CONFIRMED |
| DCC-STATIC-027 | P1 | 34、35、36、77、84、88、90 | 修改正式文件基础信息未同步逻辑身份，旧编号可命中新编号文件 | OPEN_STATIC_CONFIRMED |

## 最终修复结果（2026-09-13）

- **代码结论**：DCC-STATIC-001至016、021、022、023均已完成当前源码层修复；追加审计登记的017至020及024至027仍为开放项。
- **010补齐**：A/2/A/3重新送审时会沿同Master、同申请人、同Revision且版本号前进的predecessor链识别PENDING_APPLICANT_REWORK前置版本；新审批流程创建后，旧A/1退回流程被发起人取消、旧版本标记为WITHDRAWN、统一受控候选同步WITHDRAWN并链接新候选，避免继续阻塞“修正后重新送审”。
- **011同步**：静态合同不再写死旧局部变量名recipientUserIds，改为锁定当前正式orderedRecipientUserIds快照仍会调用保存名单人员有效性校验。
- **静态验证**：DCC-STATIC-001、002、003、006、007、008、009、010、011、012、013、014、015及DCC详情返工静态合同全部PASS；DCC工作版本提交、平台适配器、审批工作流相关151项Maven定向测试PASS；未执行E2E。

## 独立复核结果（2026-09-13，修复前）

- **代码结论（历史）**：14项原始业务问题已处理，010仅部分修复。完整逐项清单见 `doc/tasks/20260913-dcc-bug-status-recheck/verification-report.md`；本结论已被上方“最终修复结果” supersede。
- **010剩余问题**：现在可把退回的A/1检入为A/2，但A/1仍保持待申请人修改状态及旧BPM任务；A/2送审会被“同Master存在其它未完成流程”拦截，仍无法完成修改后重新送审。
- **011验证状态**：当前代码已校验保存名单人员是否有效；本轮脚本因旧断言写死recipientUserIds而实际传入orderedRecipientUserIds失败，属于测试合同未同步。
- **本轮执行**：14个静态合同实际重跑，13 PASS、1 FAIL（011）；010合同虽然PASS，但未覆盖原审批终结与新版本重新送审的衔接。
- **边界**：本轮没有重跑366项Maven或前端全量类型检查，没有执行E2E或发布。下面修复汇总保留上轮报告背景，各项“实际逻辑”保留原缺陷描述，不作为当前实现的逐行说明。

## 原修复报告汇总（2026-09-13）

- **原报告声明**：上轮报告曾将DCC-STATIC-001至015全部标记修复；后续复核曾将010重新标记为部分修复，并指出011测试合同过期。本轮最终修复后，以本文件“最终修复结果”为准。
- **主证据**：`doc/tasks/20260913-dcc-90-step-static-fixes/verification-report.md`。
- **关键验证**：14 个 DCC-STATIC Node 静态合同 PASS；DCC 定向 Maven 回归 `366 tests` PASS；前端 `pnpm ts:check` PASS；`git diff --check` PASS（仅换行提示）。
- **验证边界**：按用户 2026-09-13 当轮要求，本次不执行 Playwright E2E、数据库写入、服务重启、远端访问、Git commit 或 Git push。

## DCC-STATIC-001：新项目缺少正式负责人和编制权限的配置闭环

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：3、9、10、11、12、28、48、55。
- **触发条件**：通过“新增项目代码”或“产品建档申请”生成全新项目，然后按正常页面流程准备上传。
- **预期行为**：管理员能通过正式入口配置该项目的负责人/编制权限，使新项目可完成后续上传。
- **实际逻辑**：两个创建服务只保存项目资料，没有写入 dcc_project_access_rule；上传强制从该表解析 OWNER/EDIT。全仓生产代码中该表只有 DO、查询 Mapper 和授权读取服务，未找到维护 Controller、前端入口或创建时授权逻辑；建表迁移也不创建授权数据。
- **业务影响**：即使项目、模板、账号菜单权限都已配置，新的项目仍可能无法上传。第11步目前只能依赖额外配置手段，不能由所述页面流程闭环。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java:173`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImpl.java:84`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:42`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1641`；`IntRuoyiBackend/sql/mysql/20260911_dcc_project_access_rule.sql:4`。
- **建议修复边界**：提供受控的项目权限维护/初始化流程；负责人文本与正式授权分开，禁止用管理员通用权限绕过项目授权。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 全新项目和有管理权限的文控，When 在页面配置正式编制人员后上传，Then 合法人员可创建工作稿，未授权人员仍被拒绝。

## DCC-STATIC-002：产品建档申请关闭弹窗后无法恢复审批，重复申请又被拦截

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：10。
- **触发条件**：提交产品建档申请后关闭弹窗、刷新页面，或需要交由另一个审批人处理。
- **预期行为**：申请进入可查询的待办/申请列表，申请人及审批人能重新打开并继续处理。
- **实际逻辑**：创建返回的申请 ID 仅保存在页面 ref 中；每次打开弹窗都会清空该 ID。审批按钮只接收这个临时 ID。Controller 只有创建和按 ID 批准，没有申请列表/详情/撤销入口；服务又禁止同名同代码存在第二个待批申请。
- **业务影响**：待批申请保留在数据库，但页面无法找回；重新提交被重复校验阻止，跨人审批与刷新后续办中断。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:743`；`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:2454`；`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:2470`；`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:2509`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProductOnboardingController.java:33`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImpl.java:164`。
- **建议修复边界**：补正式申请查询与审批待办入口，并让重开弹窗能定位原申请；不要通过删除待批数据或允许重复申请规避。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 已提交且待批的建档申请，When 关闭刷新或由另一审批人登录，Then 能找回原申请并继续批准，不重复建档。

## DCC-STATIC-003：模板允许保存非末级分类，但按该模板新建上传必然失败

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：13、14、22、26、29、33、48。
- **触发条件**：分类达到三级且绑定唯一启用类别，但下面仍有启用子分类；将该分类保存为项目模板项。
- **预期行为**：可保存的模板项应满足新建上传的分类要求，或保存时明确指出不能用于上传。
- **实际逻辑**：模板编辑器 checkStrictly=true 允许选中父节点；后端只校验启用、三级深度和唯一类别。新建上传却要求 activeDescendantIds 只有自身，即必须最末级。
- **业务影响**：管理员看到模板保存成功，编制人员按模板逐项选择后，最终提交仍报分类层级错误。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectFileTemplateEditor.vue:203`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectFileTemplateServiceImpl.java:105`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1996`。
- **建议修复边界**：统一模板可选/可保存分类与上传校验，保留对已变更分类的明确失效提示。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 有子分类的三级节点，When 配置项目上传模板，Then 提前拒绝该非末级节点或引导选末级，合法模板能够上传。

## DCC-STATIC-004：工作稿创建成功但响应丢失后，页面预检挡住同键重试

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：35、37、47、48、49、50。
- **触发条件**：首次新建工作稿已提交成功，但响应丢失，用户留在上传页再次点击提交；该文件还没有正式现行版本。
- **预期行为**：同一上传会话重试能拿回已经创建的工作稿，或明确引导打开该工作稿。
- **实际逻辑**：提交前总要调用现行版本查询；查到 Master 后强制 activeMasters.size()==1。新工作稿对应 Master 尚无 currentActiveControlledFileId，查询报编号冲突。前端据此直接 return，无法到达已修复的后端同键回读入口。
- **业务影响**：用户收到重复/版本冲突，却无法确认首次是否成功；后端幂等已正确，页面恢复路径仍断开。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:301`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:385`；`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue:2034`；`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue:2044`；`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue:2045`。
- **建议修复边界**：区分“逻辑文件存在但只有工作稿”和“正式版本冲突”；同会话恢复应读取既有创建结果，不能先被现行版条件挡住。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 首次工作稿创建成功但响应丢失，When 同上传会话重试，Then 返回原工作稿且不新增文件，不显示不相干的正式版冲突。

## DCC-STATIC-005：工作中和待发布状态缺少受保护预览通路

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：49、50、51、52、68、69、85、86、88。
- **触发条件**：申请人打开刚创建/检入的 WORKING，或文控打开已有盖章PDF的 READY_TO_PUBLISH 版本核对内容。
- **预期行为**：相应编制和文控人员可按职责预览当前版本；普通用户仍不得提前读取未发布版本。
- **实际逻辑**：expectsPreviewArtifact 及 canReadBinary 只支持 ACTIVE、SUPERSEDED 和四个待审批状态；WORKING、READY_TO_PUBLISH 未覆盖。浏览响应据此返回 canPreview=false；通用下载又只允许 ACTIVE。
- **业务影响**：文件行和版本资料能出现，但准备送审或待发布核对时无法通过正式预览入口查看正文。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1420`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1626`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:2099`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:2712`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/download/DccDownloadPolicyService.java:12`；`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:232`。
- **建议修复边界**：按职责补齐工作稿和待发布版本的预览状态与正确文件来源，保持普通查阅权限隔离。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 合法申请人的工作稿及文控待发布版本，When 点击预览，Then 读取该版本正文并记审计；普通阅读用户无权读取。

## DCC-STATIC-006：检入后审批预览旧原件，签名证据却绑定新源文件

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：36、43、44、52、54、56、57、58、59、60、86、88。
- **触发条件**：A/1 上传原件S1，检入修改后的源文件S2生成A/2，再将A/2送审。
- **预期行为**：审批人员看到A/2的实际S2内容，签名证据也绑定同一份内容。
- **实际逻辑**：copyForCheckin 将 sourceFileId 改为新源文件，却沿用 originalFileId；待审批预览 resolveBinaryFileId 始终返回 originalFileId。签名证据则对 revision.sourceFileId 计算摘要。
- **业务影响**：审批页面显示旧正文S1，而签名证据声明审核了S2，造成版本与审核内容不一致；升版保留旧 originalFileId 时同样受影响。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:566`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:716`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:717`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1610`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSignatureEvidenceServiceImpl.java:57`。
- **建议修复边界**：明确原始历史来源与当前受审内容两种身份，预览必须按送审版本的受审文件读取；同步校验图纸源件和预览件的版本关系。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given A/2源文件与A/1不同，When 预览并签署A/2，Then 页面字节与签名摘要属于同一A/2内容，历史A/1仍保持不变。

## DCC-STATIC-007：检入新小版本不继承关联文件，发布时丢失影响范围

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：40、52、54、56、77、79、80、88。
- **触发条件**：A/1 上传时关联文件X，随后正常检入生成A/2并发布。
- **预期行为**：未主动变更关联关系时，A/2保留关联X，发布通知和影响评估包含X。
- **实际逻辑**：检入只插入新受控文件、源文件所有权及上传绑定，没有复制 related-file 关系。关联查询和发布关系快照均按具体 controlledFileId 读取，不会自动沿 predecessor 或 Master 继承。
- **业务影响**：一次普通内容修改就使关联关系从正式版本消失，相关负责人可能收不到通知，影响评估任务漏建。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:568`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:706`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1973`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java:206`。
- **建议修复边界**：检入时明确继承/版本化关联文件集合，并保留可追溯来源；不可在发布时猜测关系补齐。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given A/1关联X且本次仅修改正文，When 检入A/2并发布，Then A/2关联和发布影响快照仍包含X，A/1历史关系不被改写。

## DCC-STATIC-008：检出和检入缺少正式项目编制及类别上传权限复核

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：11、12、16、50、51、52、55。
- **触发条件**：文件申请人仍有DCC查询权限，但其项目OWNER/EDIT或类别上传权限已经被撤销；随后继续检出或做备注检入。
- **预期行为**：实际修改文件版本时重新验证正式项目及类别写权限，查询权限不能用于创建新版本。
- **实际逻辑**：checkout/checkin Controller 只要求 query；检出服务检查可查询及 requester 相等，检入检查 checkout.actorId。两条写路径没有调用项目编制授权或类别UPLOAD校验；备注检入也不经过上传凭证权限。
- **业务影响**：被撤销编制权的原申请人仍可取得编辑锁并创建新小版本，虽然后续送审可能被拒绝，但版本写入边界已经被绕过。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java:466`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:444`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:768`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:533`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:542`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:531`。
- **建议修复边界**：在检出和检入各自的写边界复查项目OWNER/EDIT与类别UPLOAD；保留对实际检出人的归属校验。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 原申请人的编制或上传权限已撤销，When 检出或检入，Then 明确拒绝且无新锁/新版本；权限恢复后才可正常办理。

## DCC-STATIC-009：检入重放只比较上传票据，未绑定操作者和修改载荷

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：52、55、86、88。
- **触发条件**：一次仅修改备注的检入已经完成；之后以空上传票据再次提交不同备注/原因，或由另一具有query权限的账号请求同一基础版本。
- **预期行为**：仅同一操作者、同一业务载荷的重试可回读；异人或异载荷应拒绝，不能伪装保存成功。
- **实际逻辑**：无活动检出时，服务仅比较 latest.status==CHECKED_IN 和票据相等，然后直接返回现有版本；actor校验在此分支之后。两个空票据也相等，且不校验备注、修改说明或当前读取权限。
- **业务影响**：修改后的新备注可能被静默忽略，接口仍返回成功；其他账号可在这个分支拿到不应直接返回的版本元数据。未将此结论扩大为正文下载泄露。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:519`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:523`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:528`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:533`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java:474`。
- **建议修复边界**：持久化并比较操作者与规范化请求摘要，回读前执行访问授权，空票据不能作为完整幂等身份。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 已完成一次无文件的备注检入，When 同键异备注或不同用户重放，Then 拒绝；原操作者同载荷才返回原结果。

## DCC-STATIC-010：退回申请人后只能签名继续，无法完成文件内容修改

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：51、52、56、62、63。
- **触发条件**：审核人因正文错误将任务退回 APPLICANT_REWORK，申请人按提示处理修改。
- **预期行为**：申请人能通过受控入口修正内容并重新送审，或者明确走新工作版本流程。
- **实际逻辑**：退回将文件状态设为 PENDING_APPLICANT_REWORK；检出/检入可编辑状态只含WORKING、REJECTED、ACTIVE。处理回退入口只调用approveTask并推进BPM，未接收替换正文；该状态也没有普通预览通路。
- **业务影响**：需要改正文的退回无法按页面闭环，申请人可能只能签署继续，把同一错误正文重新送入审批。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1106`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1387`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:651`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1626`；`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:4056`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:961`。
- **建议修复边界**：明确退回修改与不可变送审版本的衔接，提供生成/绑定修订内容的入口，再允许处理回退提交。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 申请人因正文错误收到退回，When 完成修正并重新提交，Then 后续审批读取修正内容且保留原审批与版本历史。
- **修复证据**：`doc/tasks/20260913-dcc-static-010-applicant-rework-content-change/verification-report.md`；`doc/tasks/20260913-dcc-static-010-011-final-fix/verification-report.md`；本轮补充 `doc/tasks/20260913-dcc-static-010-reopen-final/verification-report.md`，静态合同和定向单测已覆盖旧退回流程、统一受控候选终结、连续检入祖先解析与新版本重新送审衔接。

## DCC-STATIC-011：已保存电子分发名单中的人员停用后仍生成必需培训任务

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：66、69、71、74、75、76。
- **触发条件**：文控批准时人员U启用并进入电子分发快照；发布前U被停用或删除，类别要求在线培训。
- **预期行为**：发布前明确阻断失效培训对象，并提供受控处置，不创建无法完成的必需任务。
- **实际逻辑**：发布优先读取已保存分发计划；resolveSavedElectronicDistributionRecipients 只取非空用户ID，createTrainingRecords直接生成必需任务，未重新验证账号有效性。全部培训确认仍是下发的硬条件，也未找到任务转交/豁免的正式入口。
- **业务影响**：文件永久卡在培训中，无法人工下发。前次修复覆盖的是“现取部门名单”过滤，本条是已保存快照的人员随后失效，触发条件不同。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:659`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:713`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:725`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:809`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:860`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingAssignmentAckServiceImpl.java:74`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:272`。
- **建议修复边界**：发布前校验快照收件人当前可办理性；人员失效时明确受控调整方案，不能默默删人或当作培训完成。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 已批准名单中的U随后停用，When 发布培训文件，Then 给出具体人员阻塞且不生成无法完成的培训任务，合法调整后可继续。
- **修复证据**：`doc/tasks/20260913-dcc-static-010-011-final-fix/verification-report.md`；静态合同已同步orderedRecipientUserIds当前变量名并保持保存名单人员有效性校验断言。

## DCC-STATIC-012：重新开始培训会话时覆盖刚补记的阅读时长

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：73、74、75。
- **触发条件**：用户有未正常结束的旧阅读会话，重新打开培训页；旧会话从最后心跳到关闭还应补记若干秒。
- **预期行为**：补记时长保留在累计进度中，新会话从最新累计值开始。
- **实际逻辑**：startViewSession先读取旧progress；closeOtherActiveSessions重新读库并增加旧会话尾部时长；随后又使用开始时的旧progress调用updateProgressMetadata(...,0)，把新增时长覆盖回去。
- **业务影响**：重新进入或切换阅读会话后累计时间回退，用户需要重复阅读才能达到确认门槛。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:151`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:155`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:168`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:386`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:399`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:417`。
- **建议修复边界**：先完成旧会话结算再读取最新进度，或使用加锁/原子累加避免旧对象覆盖；保留正常注意力与心跳计时规则。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 累计100秒且旧会话待补5秒，When 开启新会话，Then 累计至少105秒，不回退到100秒。

## DCC-STATIC-013：影响评估关联B/1后发布B/2，修订跟踪无法自动完成

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：36、52、54、77、80、88。
- **触发条件**：影响任务创建或关联B/1，编制人继续检入B/2并将最新B/2正式发布。
- **预期行为**：同一目标大版本通过合法小版本演进后正式发布，应完成对应影响任务的修订跟踪，并记录实际发布版本。
- **实际逻辑**：linkExistingMajorRevision把具体行ID保存为linkedRevisionControlledFileId；检入不更新该关联；发布收口只查询等于publishedRevision.id的任务。关联B/1的记录不会被B/2发布查询到，原任务也不是NOT_STARTED，不能直接再关联。
- **业务影响**：文件已经按要求修订发布，影响任务仍显示修订未完成，管理批次无法正常收口。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java:216`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java:227`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:566`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java:332`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java:66`。
- **建议修复边界**：将修订跟踪绑定到明确的大版本身份及其合法小版本链，发布时核对来源链并记录最终发布行，不能仅按旧小版本ID匹配。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 任务关联B/1，When 合法检入B/2并发布，Then 原任务变为修订已完成且记录B/2；其他大版本发布不得误关闭。

## DCC-STATIC-014：浏览页“升大版本”按钮的资格与后端OWNER规则不一致

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：36、50、51、52、77、88。
- **触发条件**：现行文件已发布，合法项目OWNER需要从现行/历史版本发起下一大版本，且OWNER不是原申请人。
- **预期行为**：浏览页按正式项目OWNER和可选来源版本提供新建修订版入口。
- **实际逻辑**：canCreateMajorRevision仅允许status==WORKING且requester为当前用户；后端createMajorRevision要求正式项目OWNER，并要求Master已有现行版本。现行ACTIVE版本及非申请人的合法OWNER看不到浏览入口；首次工作稿却可能看到一个后端必拒绝的入口。
- **业务影响**：浏览页展示与实际可办理资格相反。其他上传/影响评估入口可能仍可升版，本条不声称所有升版入口都不可用。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:1658`；`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:1661`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:620`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:624`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:626`。
- **建议修复边界**：由后端投影正式升版资格与来源选项，前端按投影展示；不要用requester或WORKING猜测OWNER权限。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 当前正式版本及不同于申请人的项目OWNER，When 打开浏览页，Then 可按授权新建修订版；无现行版或无OWNER者不出现误导入口。

## DCC-STATIC-015：同一人检出另一小版本时返回成功，却未切换实际检出基础版本

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：51、52、53、88。
- **触发条件**：用户已检出同一Master下的A/1，再在版本列表选择A/2并点击检出。
- **预期行为**：明确提示已检出的基础版本，或受控地完成基础版本切换；返回结果必须对应实际检出记录。
- **实际逻辑**：检测到同一actor的活动检出后，服务不比较baseIterationId，直接把本次请求的file标成checkedOut并返回。数据库检出记录仍指向A/1，而随后A/2检入/撤销会因baseIterationId不匹配被拒绝。
- **业务影响**：界面把A/2显示为已检出，用户却无法检入或撤销该行，显示状态与真实编辑锁不一致。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:459`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:461`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:465`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:536`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:613`。
- **建议修复边界**：同人重放也必须比较基础版本ID；非同一基础版本时明确拒绝并返回实际锁定版本信息。
- **BDD（修复验收，静态/单元证据见修复汇总）**：Given 已检出A/1，When 对A/2请求检出，Then 不返回A/2检出成功；同一A/1重复请求才可幂等回读。

## 已排除的重复问题与保留边界

- 前次已修复的根行之后业务异常吞掉、重试actor/权限、转办加签目标资格、最后两人培训确认竞态、旧通知重放幂等、审批完成文案，本轮不作为未修复缺陷重复登记。
- DCC-STATIC-004是前端预检挡住幂等恢复，未否定后端已完成的同键回读；DCC-STATIC-011是已固化名单人员随后失效，未否定现取部门名单已过滤停用人员。
- 生效日期当前作为业务资料保存，90步也已说明实际生效依赖发布门禁；未确认“必须按日期自动生效”的需求，不把无定时发布认定为缺陷。
- 实际BPM部署模型、真实人员/目录/模板配置、线上对象存储内容、水印访问和消息最终送达均未验证，不以静态阅读冒充运行验收。
- 新发布通知链（DccPublicationNotification*）与旧分发/培训消息链分别检查；提交后发送的真实平台事务持久性仍需要专项运行验证，本轮未把未经运行证实的事务猜测计入上述15项。

## 追加问题明细（016—027，静态确认待修复）

### DCC-STATIC-016 产品建档审批入口误用创建权限，只有审批权限的人员无法续办

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：10、16。
- **触发条件**：已有待审批产品建档申请；处理人具备项目查询和 update 权限，但没有 create 权限。
- **预期行为**：获准查询待审批并批准申请的人员，能从正式页面进入待审批清单并办理。
- **实际逻辑**：pending 接口允许 create 或 update，approve 接口要求 update；承载待审批列表的弹窗却只能由要求 create 的“产品建档申请”按钮打开。
- **业务影响**：常见的申请人与审批人分权配置下，审批人有服务端权限却没有页面入口，建档生成项目被卡住。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:39`；`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:2846`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProductOnboardingController.java:42`。
- **关系与边界**：与002“关闭后丢失申请ID”不同：恢复列表已存在，本项是审批角色无法进入恢复列表。
- **建议修复边界**：按查询/审批权限提供可达入口，弹窗内创建与批准动作分别守卫。
- **BDD（后续修复验收，未执行）**：Given 仅有查询与审批权限且存在待审批申请 When 从项目页面打开待办并批准 Then 能完成批准，仍不能创建申请。
- **修复证据（2026-09-13）**：产品建档入口改为 create 或 update 任一权限可见；弹窗加载正式待审批列表，审批人可恢复待办并通过 update 守卫批准；创建申请按钮仍仅由 create 守卫，恢复待办后表单锁定避免审批路径变成编辑回退。新增后端 `/pending` 查询和 Mapper 待审批列表，服务测试覆盖待审批读取。验证：`node IntRuoyiFronted/tests/e2e/dcc-static-016-product-onboarding-approval-entry-static.spec.cjs` PASS；`node IntRuoyiFronted/tests/e2e/dcc-project-code-product-onboarding-static.spec.js` PASS；`mvn.cmd -q -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` PASS；`pnpm.cmd ts:check` PASS；`git diff --check` PASS。证据见 `doc/tasks/20260913-dcc-static-016-approval-entry/verification-report.md`。

### DCC-STATIC-017 不存在的角色、部门或岗位可保存为唯一负责人，正式授权实际无人获得

- **级别/状态**：P2 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：11、12、16、55。
- **触发条件**：维护正式项目权限时，将最后一条有效OWNER替换为不存在的ROLE、DEPT或POSITION编号，设置启用及有效时间。
- **预期行为**：保存前校验授权主体真实存在；无效主体不能满足“至少一个当前有效负责人”的门禁，旧权限应保留。
- **实际逻辑**：页面对非USER主体使用任意正整数输入；后端只检查类型、正整数、权限级别及时间，不检查主体存在性。OWNER门禁仅看规则自身字段，随后整组删除并重建权限。
- **业务影响**：页面保存成功，但不存在的主体匹配不到任何真实人员，项目可能失去所有负责人；新建、送审和升大版本等动作会拒绝。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue:1326`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:52`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:89`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:147`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java:185`。
- **关系与边界**：与001“缺少权限维护入口”不同；本项是已新增入口允许保存失效的主体引用。
- **建议修复边界**：保存时核验主体存在及可授权状态，再判断有效OWNER；前端用真实主体选择器，失败不替换原授权。
- **BDD（后续修复验收，未执行）**：Given 项目已有有效OWNER When 保存仅引用不存在角色的OWNER规则 Then 明确拒绝且原权限保持不变。

### DCC-STATIC-018 路线可保存审批方式、比例及必需开关，但实际审批仍按固定流程模型执行

- **级别/状态**：P1 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：15、47、56、57、58、59。
- **触发条件**：在路线中将批准环节设为ALL/100%，或修改审核通过比例、关闭某环节必需开关，再使用仓库提供的标准DCC流程模型送审。
- **预期行为**：配置必须与执行一致；若业务限定固定四环节策略，应禁止保存不受支持的配置并明确展示固定规则。
- **实际逻辑**：路线保存及部分快照记录了approveMethod/approveRatio/required；创建BPM流程只传文件与人员映射。随库BPMN将审核固定为100%，批准固定为任一人完成，四环节顺序固定，未消费上述可编辑规则。
- **业务影响**：配置显示全员批准时仍可能一人即通过；配置较低比例或可选环节时又仍等待固定规则，审批证据与维护人员理解不一致。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/routes/components/RouteForm.vue:103`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImpl.java:351`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:766`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:787`；`IntRuoyiBackend/sql/mysql/20260717_dcc_applicant_rework_bpmn_contract.sql:30`。
- **关系与边界**：确认的是DCC配置未接入执行的源码契约；标准BPMN有明确证据，未读取实际部署模型，不声称线上模型一定与仓库相同。
- **建议修复边界**：统一配置和执行的权威规则：实现所允许配置，或将固定策略设为只读并在服务端拒绝冲突值。
- **BDD（后续修复验收，未执行）**：Given 批准环节配置ALL/100%且有两名批准人 When 仅一人批准 Then 不得宣称满足全员批准；若仅支持ANY，则保存ALL时必须拒绝。

### DCC-STATIC-019 同一审批环节可重复配置，启动流程时后续同环节人员被静默丢弃

- **级别/状态**：P1 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：15、47、56、57、58、59。
- **触发条件**：保存环节编号为1、2、2、3、4的路线，两条2号环节分别配置不同且具备资格的审核人员。
- **预期行为**：固定四环节应拒绝重复编号；若允许同环节多个候选来源，应按正式规则完整合并，不能丢人。
- **实际逻辑**：saveRoute校验先distinct，重复编号被消除后仍通过1—4检查；节点均写入。生成审批人员Map时按stageCode取键，重复键采用(left,right)->left，仅保留第一条。快照重建人员映射也相同。
- **业务影响**：路线表中配置的第二组会签人员不会收到任务；流程可能在缺少预期部门审核的情况下继续。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/routes/components/RouteForm.vue:49`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImpl.java:378`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java:91`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:2934`；`IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql:186`。
- **关系与边界**：与018不同：这里是同环节节点身份重复造成候选人丢失，任何审批完成比例都无法补回被丢弃的人。随库节点表未提供环节唯一约束。
- **建议修复边界**：固定环节在前后端均要求恰好一条；若采用多来源设计，必须显式合并去重并统一预览、快照、运行授权。
- **BDD（后续修复验收，未执行）**：Given 固定四环节路线 When 保存两条2号环节 Then 保存拒绝重复；不得保存成功后仅派发第一组人员。

### DCC-STATIC-020 审批路线生效时间未参与选用，未来路线保存后立即替代现行路线

- **级别/状态**：P2 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：15、47、54、56。
- **触发条件**：今天保存一条生效时间为明天的新路线，并变更其中审批人员；今天继续提交文件。
- **预期行为**：今天仍使用当前已生效路线，新路线只在约定生效时间后用于新提交；不支持定时则应限制字段并提示。
- **实际逻辑**：保存直接将新路线设active=true并立即停用旧路线。运行侧selectLatestActiveByCategoryId只按active和最大版本取值，不判断effectiveTime。
- **业务影响**：未来调整的审批责任提前生效；尚未到岗的人员立即收到任务，当前有效配置无法继续用于提交。
- **代码证据**：`IntRuoyiFronted/src/views/dcc/controlled-file/routes/components/RouteForm.vue:27`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/route/DccApprovalRouteAdminServiceImpl.java:121`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/route/DccCategoryApprovalRouteMapper.java:28`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java:69`。
- **关系与边界**：这是审批路线的生效时间；不涉及受控文件effectiveDate字段是否只是展示信息。
- **建议修复边界**：让路线保存、启停和运行选择遵守同一生效时间规则，保留未来路线生效前的有效路线。
- **BDD（后续修复验收，未执行）**：Given 旧路线已生效且新路线明天生效 When 今天提交文件 Then 选择旧路线；到期后的新提交选择新路线。

### DCC-STATIC-021 工程图纸已上传配套PDF，工作稿及审批预览仍选择无法在线显示的CAD源件

- **级别/状态**：P1 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：42、43、44、52、56、57、58、59、60、86。
- **触发条件**：上传受支持的DWG或SolidWorks图纸源件及有效配套PDF，创建工作稿或进入审批后使用受控预览。
- **预期行为**：审批人员能通过当前图纸版本绑定的PDF阅读图纸，并能追溯该PDF与当前源件的对应关系。
- **实际逻辑**：上传校验强制图纸携带PDF，但resolveBinaryFileId在WORKING及待审批状态直接选sourceFileId，未选drawingPdfFileId。CAD扩展名落入DOWNLOAD_ONLY，预览页提示不支持在线预览；正式下载策略又要求已发布且ACTIVE。
- **业务影响**：已完成配套上传仍无法在正常受控预览路径阅读图纸；审核、会签及签名前的内容核对受阻。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1592`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:935`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:1680`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/DccControlledFilePreviewKindEnum.java:50`；`IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue:295`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/download/DccDownloadPolicyService.java:12`。
- **关系与边界**：与006“检入后预览旧原件”不同：即使初次A/1源件完全正确，也因选错预览载体失败。本项不开放未发布原件下载；检入时配套件如何同步应在修复验收中一并检查。
- **建议修复边界**：按图纸类型选择与当前源件明确绑定的有效PDF，维护检入后的对应关系，签名证据保持关联；缺少当前配套件时明确拒绝。
- **BDD（后续修复验收，未执行）**：Given 当前版本有DWG及其配套PDF When 申请人或当前审批人预览 Then 展示该版本配套PDF并保留源件关联；替换图纸后不得误用旧配套件。
- **修复证据（2026-09-14）**：`doc/tasks/20260913-dcc-static-021-drawing-preview-pdf/verification-report.md`；新增静态合同 `dcc-static-021-drawing-preview-pdf-contract.spec.cjs` PASS；`DccControlledFileQueryServiceTest` 三个定向回归 PASS，覆盖 WORKING 图纸预览读取当前配套 PDF、缺少当前配套 PDF 拒绝预览、替换图纸源件未带当前配套 PDF 时拒绝且不复制旧 PDF；未执行 E2E、服务启动/重启、数据库写入或远程操作。

### DCC-STATIC-022 仅修改备注的检入能力存在于后端，但正式检入页面强制上传新文件

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：51、52、88。
- **触发条件**：当前工作版本已检出，用户只需修改允许变更的备注，源文件内容保持不变。
- **预期行为**：按现有版本PRD，允许仅业务元数据有真实差异的检入，并生成新小版本及结构化差异。
- **实际逻辑**：后端以hasUpload或hasRemarkChange判断真实变化，支持只改备注；浏览页检入弹窗只有源文件和修改说明，无备注字段，submitCheckin在缺少uploadTicket时直接返回。修改说明也没有作为remark提交。
- **业务影响**：用户不能通过正式页面完成合法的仅元数据检入；重传相同源件且不传备注又会被后端无变化门禁拒绝。
- **代码证据**：`docs/product/dcc-windchill-version-phase1-prd.md:106`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:549`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:687`；`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:961`；`IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:1895`。
- **关系与边界**：依据现有PRD FR-P1-04与后端实现确认页面缺口；不把API可调用当作页面路径已完成，也不把010退回修改链路纳入本项。
- **建议修复边界**：在检入页面提供受允许的元数据字段，与后端统一“源件或元数据至少一项真实变化”校验并保留差异审计。
- **BDD（后续修复验收，未执行）**：Given 工作版本已由本人检出 When 只修改备注并填写修改说明、不上传新源件 Then 生成下一小版本、源件哈希不变且记录备注差异。
- **修复证据（2026-09-13）**：`doc/tasks/20260913-dcc-static-022-remark-only-checkin/verification-report.md`；新增 DCC-STATIC-022 静态合同与既有浏览页检出/检入静态合同均 PASS；未执行 E2E、服务启动/重启、数据库写入、远程操作。

### DCC-STATIC-023 检入重放校验使用未转义文本拼接，不同修改载荷可被误判为同一次请求

- **级别/状态**：P2 / FIXED_STATIC_VERIFIED。
- **涉及步骤**：52、55、88。
- **触发条件**：同一操作者、同一基础版本和相同票据重放检入；修改说明或备注含换行及文字remark=。该场景可由检入API合法文本构造。
- **预期行为**：只有所有业务字段相同的请求才能回读已有版本，字段分配不同的载荷必须拒绝冒充重放。
- **实际逻辑**：normalizeCheckinReplayPayload把两个字段直接拼成changeDescription=<值>、换行、remark=<值>。字段内的同样分隔文本未转义，编码不是一一对应。
- **业务影响**：修改说明与备注内容不同的请求可返回之前版本，用户提交的变化被忽略且系统未报载荷冲突。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:702`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:711`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:720`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:732`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileCheckinReqVO.java:1`。
- **关系与边界**：009已增加操作者及常规载荷比较，本项是该编码方式的新边界，不否认普通不同载荷已被拒绝；无需依赖010。静态反例：A的说明为“核对\nremark=旧说明”、备注为“最终说明”；B的说明为“核对”、备注为“旧说明\nremark=最终说明”。两者拼接结果完全相同，其中\n表示实际换行。
- **建议修复边界**：逐字段比较规范化值或使用无歧义结构化编码，保留空值/未传字段的业务语义，勿用未转义分隔符组成重放身份。
- **BDD（修复验收）**：Given 同一基础版本已有A载荷检入成功 When 使用上述B载荷重试 Then 明确拒绝不一致载荷；完全相同A请求仍回读原结果。
- **修复证据（2026-09-13）**：`DccControlledFileQueryServiceImpl` 已改为 `CheckinReplayPayload(changeDescription, remark)` 结构化字段元组比较，移除未转义字符串拼接；`node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-023-checkin-replay-structured-payload-contract.spec.cjs` PASS，`node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-009-checkin-replay-payload-contract.spec.cjs` PASS，`mvn -pl yudao-module-dcc -am "-DskipTests" compile` PASS。证据见 `doc/tasks/20260913-dcc-static-023-checkin-replay-payload/verification-report.md`。

### DCC-STATIC-024 并发开始培训阅读可留下多个活跃会话，同一时间段被重复累计

- **级别/状态**：P1 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：73、74、75、76。
- **触发条件**：同一用户、同一培训进度尚无活跃会话，两个不同clientSessionId的开始请求并发到达，之后两会话交替发送心跳。
- **预期行为**：同一人的同一培训在任意时刻最多一个计时主体，重叠阅读区间只能计入一次，600秒门禁不能因多开缩短。
- **实际逻辑**：start先用普通查询关闭其它会话，再查当前client并插入，未锁定进度行。两个事务可同时观察到无会话并各插入一条；末尾进度更新虽取得写锁，却不重新检查活跃会话。后续每条心跳分别按自己的lastHeartbeat累计到同一进度。
- **业务影响**：构造顺序：两会话同时开始，5秒后先后提交各自心跳，可累计约10秒。重复交替可提前达到600秒并允许确认，培训门禁失真。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:151`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:175`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:387`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java:418`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileTrainingViewSessionMapper.java:13`；`IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql:589`。
- **关系与边界**：与012“重启会话时丢失补记时长”不同；012回读修复没有串行化会话创建。本项为源码并发交错推导，未运行并发实验；随库表只有进度索引，无单活跃会话约束。
- **建议修复边界**：以用户培训进度为范围串行化开始/结束/心跳，保证唯一有效计时主体或对重叠区间去重，并覆盖并发首次开始。
- **BDD（后续修复验收，未执行）**：Given 同一培训进度无活跃会话 When 两个客户端同时开始并交替心跳 Then 仅一个会话有效或重叠秒数只计一次，未满600秒不能确认。

### DCC-STATIC-025 大版本创建时的正式基线未继承到后续小版本，历史响应也未返回该字段

- **级别/状态**：P2 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：36、52、77、88。
- **触发条件**：从某个历史小版本创建B/1，当时正式版本为A/3；随后检入B/2，并在详情查看来源追溯。
- **预期行为**：直接来源与创建大版本时正式基线分别保存、继承和显示，B/2仍能追溯B系列建立时的A/3基线。
- **实际逻辑**：创建B/1写入revisionBaseActiveControlledFileId；copyForCheckin逐字段新建B/2时遗漏它。buildVersionHistory手工赋值又没有设置该响应字段，即使B/1已存值也不会在历史表返回。前端只在响应有值时显示“创建时正式基线”。
- **业务影响**：后续小版本丢失正式基线快照，历史页面也无法显示已存在的基线；直接来源仍保留，但不能替代正式基线证据。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1883`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:762`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:2598`；`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:3830`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileVersionHistoryRespVO.java:1`。
- **关系与边界**：与013影响任务按小版本ID跟踪失效不同；本项涉及版本创建依据的保存及展示，不改010祖先返工处理。
- **建议修复边界**：检入继承不可变的大版本基线，并在历史响应中完整投影；不要按当前ACTIVE反推历史基线。
- **BDD（后续修复验收，未执行）**：Given B/1从A/2派生且创建时正式基线为A/3 When 检入B/2并查看历史 Then B/1和B/2均显示基线A/3，B/2直接来源仍为B/1。

### DCC-STATIC-026 发布重试再次失败后沿用同一事件键，后续重试无法恢复统一状态

- **级别/状态**：P1 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：68、69、70、76、78。
- **触发条件**：文件发布失败后进行第一次重试，该重试也失败；修复原失败原因后再次点击重试。
- **预期行为**：每次新的合法重试都应从失败状态进入执行中；相同尝试的网络重放才应被幂等去重。
- **实际逻辑**：retryStamp固定使用dcc-finalization-retry:<id>。第一次重试的FAILED到FINALIZING事件单独提交，实际发布事务失败后失败服务另事务记录FAILED。第二次重试复用同键，统一核心发现旧RETRY_FINALIZATION审计便直接返回，不再改为FINALIZING；后续FINALIZE_SUCCESS要求FINALIZING而被拒绝。
- **业务影响**：原文件、盖章或分发配置问题即使已解决，第二次及后续重试仍可能固定报状态不匹配，正常页面重试无法完成发布。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:217`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java:387`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationFailureService.java:35`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledContentAdapter.java:154`；`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/ControlledContentLifecycleCoreService.java:203`；`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/ControlledContentLifecycleCoreService.java:310`；`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/controlledcontent/ControlledContentStateMachine.java:65`。
- **关系与边界**：本项是发布失败后的多次尝试，不涉及010旧审批/返工候选关闭。首次重试成功的路径不受这个触发条件影响。
- **建议修复边界**：区分一次尝试的稳定幂等键与失败后发起的新尝试编号，确保本地发布和统一状态的重试/失败/成功序列一致。
- **BDD（后续修复验收，未执行）**：Given 初次发布失败且第一次重试也失败 When 原因消除后第二次重试 Then 正常生效并保持唯一正式版本；同一次尝试重复请求仍不重复发布。

### DCC-STATIC-027 修改正式文件基础信息未同步逻辑身份，旧编号可命中新编号文件

- **级别/状态**：P1 / OPEN_STATIC_CONFIRMED。
- **涉及步骤**：34、35、36、77、84、88、90。
- **触发条件**：使用新逻辑身份创建并发布文件，文控通过基础信息修改把文件编号OLD改为NEW，或调整其项目/分类；之后在上传或浏览关联路径查询当前版本。
- **预期行为**：允许变更身份的正式入口必须保证文件、Master逻辑键和冲突检查一致；若身份不允许原地变更，应明确拒绝并指向批准的变更流程。
- **实际逻辑**：基础信息更新修改文件行的编号/项目/分类，但Master只更新fileNumber及旧类别目录名称，不更新dccProjectCodeId、fileTypeTaxonomyLeafId、normalizedFileNumber。冲突检查仍采用旧类别+目录+名称。新当前版本查询按旧逻辑键命中Master后，只检查所指文件ACTIVE，未复核身份是否一致。
- **业务影响**：OLD查询可返回fileNumber=NEW且matched=true；OLD逻辑键仍被占用，新建OLD被当作冲突。项目或分类修改也会造成同类错配；不同名称还可能绕过新逻辑身份冲突检查。
- **代码证据**：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileMetadataUpdateServiceImpl.java:104`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileMetadataUpdateServiceImpl.java:258`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMasterMapper.java:25`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:281`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1951`；`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:2024`。
- **关系与边界**：当前代码的旧身份查找分支可能让NEW查询恢复命中，因此不声称NEW一定查不到。确认的是旧逻辑键错误命中及身份不一致；不将文控明确授权的基础信息修改本身判为越权。
- **建议修复边界**：明确身份变更边界；若允许修改，在同一事务内校验新逻辑键唯一性并同步权威身份和相关投影，读取时拒绝不一致身份。
- **BDD（后续修复验收，未执行）**：Given 新身份文件OLD已正式生效 When 文控合法更名为NEW并再次按OLD查询 Then 不得返回NEW为OLD的当前版本；NEW按同一权威身份查询且冲突校验一致。
