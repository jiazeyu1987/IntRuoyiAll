# 11月9日三个问题修复审查

验证工作区：`C:/IntRuoyiWT/word1106-validation`，前端8063，后端48063。
三名子Agent均使用gpt-6.1-sol、high。主Agent负责业务链路审查和验收证据复核。

## 验收结果

- 问题01：TXT及Word培训记录在真实页面上传成功，双击只有一次提交，流程最终受控；原102秒慢转换本轮未自然重现。
- 问题02：实际检入工作版同时具备局部/换版入口，同一编制人无需额外换版权限；换版B/1完成正式审批生效。
- 问题03：两库EDIT可进入管理、VIEW-only不能管理；GENERAL局部A/2及换版B/1完成生效，QMS作废完成批准，原项目引用保留并实时变为不可执行的作废状态，历史保留。

本任务业务动作均由Playwright在8063真实页面完成。工作区含既有其他改动，本报告仅验收上述三个问题，不表示全仓测试全部通过。

## 变更范围

1. 培训上传：保留工作区已有的长耗时请求超时设置、对话框提交防重及HTTP业务错误展示，核验其实际效果；不以HTTP400截图直接推断故障已经消失。
2. 检入后变更：保留已有的精确版本EDIT权限投影和刷新后检入结果合并修复，增加WORKING版本的权限投影回归，更新不再对应当前入口的测试夹具。
3. QMS及通用文件夹：补齐列表数据到FileCandidate的canEditFile传递，在文件库编辑权、实际文件编辑权和合法状态同时满足时显示操作面板，复用现有管理详情和申请流程。
4. 真实两库下游发现并修复：作废审批准备/重办仍误用类别权限，统一既有精确文件作废权限；变更payloadHash错误遍历库不适用的空targetMarkets，仅对普通项目按原规则排序，库scope完整参与幂等摘要。
5. 主Agent E2E另发现并修复：作废最终确认仍对库属性的空targetMarkets调用join；确认摘要按正式libraryScope显示“项目属性不适用”，普通项目仍显示完整实际属性，不伪造默认值。

## 逐节点审查

关键前端代码依据：`IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts:2546`（培训请求）、`src/views/dcc/controlled-file/detail/index.vue:6108`（培训确认）、`src/views/dcc/controlled-file/browser/index.vue:2250`（检入合并）、`:3740`（双变更入口）、`src/api/dcc/controlledFile/applicationRead.ts:950`（EDIT投影传递）、`src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue:149`（管理路由及权限）、`ProjectFolderFileTable.vue:102`（行入口）。下述后端入口/服务方法为实际业务链依据。

- 培训：detail/index.vue的submitApplicantTrainingRecordDialog保留文件ID、流程轮次、上传session及ticket校验，等待真实绑定返回true后才报告成功；workflow.ts的uploadControlledFileTrainingRecord等待同步转换；WorkflowController的training-record入口进入WorkflowServiceImpl.uploadTrainingRecord，锁定主档和实际版本，校验原编制人及培训状态，绑定上传票据和附件，再推动正式流程。HTTP失败仍作为错误返回，axios仅提取服务器业务消息。
- 原培训现场交叉核对：4592的首请求持续102秒，随后并发请求等待文件锁52秒后失败；只读页面显示其Z/4已在13:39受控、发布、下发且培训记录正式绑定，之后被正常新版替代。首请求响应体未保留，不能补造HTTP证据；正式状态与签名/培训证据支持首请求实际完成。延长等待和对话框防重针对这条真实故障链；不是把后台拒绝改为成功。只读证据：original-training-readonly.png/.txt。
- 变更：browser/index.vue的canStartRevision对局部和换版统一要求实际所选版本的canEditFile、有效基线和未检出状态；openRevision带入正式基线和精确工作小版本。QueryServiceImpl.toBrowserRespVO返回精确文件EDIT，mergeCheckinResult保留刷新结果里的正式权限；revision-options、RevisionServiceImpl.createRevision和工作流提交继续核验租户、项目、EDIT、逻辑文件、状态和审批来源，没有新增独立换版权限。
- 两库入口：ProjectBrowserPanel读取正式library.canEdit；applicationRead.loadDccProjectBrowserPage保留后端canEditFile；ProjectFolderFileTable按canManage显示入口，openOperation再次核验权限和WORKING/ACTIVE/CONTROLLED_PENDING_EFFECTIVE。管理详情继续使用原申请、作废和审批入口；后端CategoryPermissionSupport按正式库角色解析权限。
- 作废准备：DccObsoleteFormExecutionPreparationService.requireContext及requireReworkFile与主作废入口统一hasFilePermission(file,actor,OBSOLETE)，保留租户、登录人、项目、master、精确版本/状态、草稿及重办来源身份校验。库EDIT不被无关类别授权挡住，VIEW-only仍拒绝；普通项目仍需现有类别EDIT及项目编辑权。
- 变更摘要：DccControlledFileRevisionServiceImpl.payloadHash先validated，libraryScope存在时保留不可变库属性对象（无虚构市场值），普通项目继续原targetMarkets排序。基线、所选正文、变更意图、流程契约及附件服务端摘要继续参与同一幂等键，QMS/GENERAL scope不可混用。
- 作废提交确认：DetailObsoleteApplication.vue的submit保留requireEligible、草稿/轮次/fingerprint、已准备状态及确认前后身份核对，仅按库scope生成属性文案；两个真实组件处理器用例分别覆盖QMS/GENERAL确认和单次SUBMIT。测试补齐既有useRoute自动导入环境及真实bodyReadContext依赖，不以空stub绕过身份契约。
- 下游：新版激活沿FinalizationServiceImpl.activateRevision与LifecycleService更新受控/生效版本和历史状态；作废沿ObsoleteServiceImpl.applyApproved更新状态、审计及当前生效指针；项目引用按固定版本身份实时解析状态，没有自动把旧引用改为新版本。

## 验证

- 运行包核对发现旧48063包缺少顶层EDIT投影，不能仅以源码和单测通过认定运行修复已生效。已正常构建31模块source reactor（BUILD SUCCESS，2分41秒），第一版包`2026-11-09-19-05-03`记录见runtime-switch-1905.json。13个业务模块字节码核对，仅Query六参数toBrowserRespVO有实际指令变化，其余5个内部类仅元数据不同，其他模块业务类一致。主Agent核对槽位2、端口48063、旧PID34368及工作区归属后切换此任务后端，没有停止或修改其他服务。
- 两库角色曾出现auth正式授权与Redis菜单角色缓存旧值不一致；通过标准UI保留同一授权集合重新保存后恢复。未直接写Redis、未清理共享缓存、未猜测修改权限服务。旧运行包顶层投影缺失是随后独立确认的入口障碍，详见general-entry-root-report.md。

- 目标工作区DccControlledFileQueryServiceTest：309项通过，包含本任务新增WORKING版本权限用例。日志：output/word1109-query-tests.log。
- 两库下游新增修复集中回归：作废准备/草稿/重办/runtime四类203项、原RevisionService70项、新库payloadHash7项，共280项通过；加强普通项目类别拒绝的单例另行通过。初轮缺测试用BPMN及新摘要fixture无效的失败记录保留；BPMN从主工作区逐字节补齐，仅供测试读取，扫描确认未进入生产包。扩大检查遇到的既有taxonomy测试bean缺失未冒称通过，详见obsolete-permission-report.md。培训额外既有category页面测试2项夹具失配亦保留在training-report.md。
- 两项修复再正常构建最终包`2026-11-09-19-29-36`，SHA `521e1df80d996482a0ea5be436ca993684c72a5d69266cfbad08ae0682c28415`；相对19:05包10045个main源码/资源及pom中仅2文件变化，13个业务模块仅上述两个DCC类变化。主Agent复核后只切换任务48063（旧PID51376），最终启动记录runtime-switch.json。
- 相关前端分组：培训13项、变更41项、两库入口31项通过，具体去重范围以各Agent报告为准。
- 主Agent新增库作废确认回归，作废dialog/session/API三个分组共47项通过。日志：output/word1109-e2e/obsolete-final-frontend-tests.log。初轮旧测试环境缺useRoute/bodyReadContext依赖的失败日志保留，没有当作业务通过证据。
- 最终前端修复后的全量vue-tsc（8192MB）：通过。日志：output/word1109-final-typecheck.log。
- 菜单迁移检查与本任务差异空白检查：通过。本次未新增权限标识、路由或index页面。
- 培训真实页面E2E：WORD1109-TRAINING-01（2054545668044084603）和Word源文件WORD1109-TRAINING-02（4605）均完成真实上传、五次签名、原编制人上传培训记录及最终受控。双击确认分别只有1次training-record POST，HTTP200/code0/data=true；TXT请求9172ms，Word请求12998ms、UI15243ms。页面ACTIVE，盖章件和发布件可用，精确轮次培训附件保留且下发完成。证据：output/word1109-e2e/training-result.json、training-word-result.json、training-completed.png、training-word-completed.png。主Agent已复核页面、截图及被动响应。本次未自然重现原102秒慢转换，不将正常Word转换表述为完整长耗时故障重现。
- 变更真实页面E2E：WORD1109-CHANGE-A初始A/1生效，真实检出检入得到A/1-1，双入口同时可用。同一编制人实际提交局部A/2，UI撤回后同一正文提交换版B/1，未增加独立换版权限。B/1再完成五步真实签署至ACTIVE，旧A/1作废、当前生效版B/1、后继4607及撤回A/2记录保留。证据：change-report.md、change-finalization-report.md及对应截图。
- 两库初始文件：WORD1109-LIB-QMS、WORD1109-LIB-GENERAL均完成真实上传及五步签署至A/1 ACTIVE。主Agent签批QMS并独立复核两库页面及被动响应。自有角色VIEW-only时同一正式文件可浏览且无管理入口；授权标准保存及运行包更新后，两库EDIT投影、操作面板进入现有管理详情均PASS。
- GENERAL真实管理终态：操作面板→检出→原文件名上传真实新正文→检入→局部A/2→五次真实签署→ACTIVE；再次检出检入→换版B/1→五次真实签署→ACTIVE。当前4613 B/1生效，4609 A/1和4611 A/2作废，历史版本及签名保留。主Agent已复核无加载遮罩的最终B/1行截图。证据：library-e2e-summary.json、library-GENERAL-partial/replacement-active-result.json及对应页面截图。
- QMS真实作废：主Agent从库操作面板进入既有详情，完成草稿41保存、规则核对、准备及确认提交；依次真实签署主管、会签指派、实际会签及批准。只读终态核验确认4608 A/1最终OBSOLETE，master9407的currentActiveVersionNo/currentActiveControlledFileId均null；原5次签名1758-1762和作废4次签名1778-1781仍全部VALID。终态详情网络失败0、DCC写请求0。最初PREVIEW权限错误和确认摘要空值错误均有失败证据，修复后完整重走页面成功，未跳过审批准备或直接改状态。证据：qms-obsolete-final-facts.json、qms-obsolete-final.png/.txt。
- 引用实时状态：事前通过UI创建任务专有project321/folder101的ref30，固定指向QMS4608 A/1；作废批准后主Agent只读通过真实页面刷新引用，断言同一ref30、来源master、精确版本、创建人和引用数量均保留，selectedVersion及permissions均OBSOLETE/executable=false，页面显示“源文件已作废”“已作废 · 仅文控可查询归档及下载”。reference-obsolete-result.json为PASS，整个只读核验DCC写请求0。此项验证状态更新，不冒称旧引用自动切到新版。
- 最终引用补证：首轮截图仍有加载遮罩及超时toast，因此另行记录导航前网络观察器并只读定位，未隐去初始失败。唯一超时为browser-page的初始全局请求（latestVersionOnly=true、空keyword、pageNo=1/pageSize=20，无project/folder身份，约31.5秒ERR_ABORTED）；folder101的共享表格请求明确携带dccProjectCodeId=321/projectFolderId=101，200/code0/list空，两个引用请求均200/code0，原ref30状态正确。最终所有loading遮罩0、可见toast空、DCC写0，稳定页面证据reference-obsolete-query-result.json/.png/.txt。本任务没有修改初始全局查询性能，不将此环境问题称为已修复。

## 业务边界

现有版本管理流程禁止对仍有项目引用的文件检出或发起/提交变更（QueryServiceImpl的REFERENCES_MUST_BE_CANCELLED、RevisionServiceImpl和WorkflowServiceImpl的引用锁校验）。本次沿用现有流程，未移除此规则；不能把无引用文件的验证描述为保留引用时换版通过。旧引用固定其实际选定的历史版本，状态实时读取与自动切到新版是不同的行为。

未关闭的额外观察：初始未选项目的全局文件列表查询超时，已用请求参数与目标folder101成功请求区分；属于额外性能问题，未在本任务扩展修改。原102秒培训慢转换未自然重现，相关额外夹具/bean失败也已分别披露。

验收期间没有远程部署或直接API/数据库写入来代替业务动作。工作区和槽位保留；只更新任务自有48063运行包。

## 提交范围核对

后续按用户要求提交推送：以int_qms的56c6e9a02为基线，提取本次三个问题的27个相关文件；已有工作区中的培训等待/错误显示和检入权限修复一并纳入。Query回归按方法合并，保留新版基线的引用锁测试及已有精确权限断言，没有用旧验证工作区整文件覆盖。其余既有未提交改动、运行包、登录态、账号文件及原始网络日志不纳入提交。8063真实E2E证据来自上述验证工作区，提交动作不改变验收端口或重启服务。

合入int_qms后重新执行QueryServiceTest的完整源reactor测试，BUILD SUCCESS；暂存区菜单迁移检查及git diff --cached --check通过。日志在验证工作区output/word1109-integrated-query-tests.log、word1109-staged-menu.log。
