## 2026-10-20 四轮重跑第一轮 E2E-03

本次独立真实UI重跑六条合同分支PASS，0合同FAIL、0合同BLOCKED；历史PASS不作为本次证据。另有一项预览叠层观察，未修改产品。

|分支|本次结果|本次run内证据|
|---|---|---|
|U1 三材料就绪/确认/取消保留|PASS|U1-confirmation.json、initial-draft-submit-confirmation.json|
|U2 附件上传中阻止|PASS|upload-A.json、upload-B.json实际DOM及按钮disabled采样|
|U3 源件/两附件真实失败、阻止及恢复|PASS|upload-SOURCE-resume.json、upload-A.json、upload-B.json|
|U4 保存失败后重试|PASS（保存前路线预检断网）|U4-save-failure-2.json、run.json|
|R1 检入/局部变更确认|PASS|checkin-negative-SOURCE/RA/RB.json、checkin-save-result.json、revision-confirmation.json、revision-formal-submitted.json|
|R2 新旧版本附件隔离|PASS|final-A1-isolation.json、final-A2-isolation.json及四份preview结果|

新前缀DCC261020C03R10090749。A/1：2054545668044084286，BPM763b89a4-cc27-11f1-a8a5-b082e25ec548；工作版2054545668044084289；正式局部变更A/2：2054545668044084291，BPM6f87ec09-cc29-11f1-a8a5-b082e25ec548。两个轮次均由S/L/C/A/D本人登录、从真实待办办理；A/2已ACTIVE，仅RA/RB，旧A/1已作废，仍仅A/B；四附件在线预览与本地任务文件逐字一致。真实检入保存请求断网失败后同表单恢复成功，当前未检出。两个正式申请各提交一次。

E2E03-R1-UI01：旧A/1正文预览→查看详情→附件A在线查看，底页多个“显示字段/重置”悬浮控件叠在预览上；正文仍准确。preview-A1-A.png、preview-A1-B.png及U-final-new-old-isolation.trace.zip可复核，具体实现根因未定位，未修复。

边界：U4仅保存预检网络失败，不声称数据库持久化异常已测；修订上传中瞬态未独立采样，R1合同的就绪和确认已通过，并额外验证三材料失败、实际checkin保存失败/恢复。源件失败时按钮可点但真实点击被本地校验阻止，普通附件上传中/失败则禁用。下载/打印/签收/音视频扩展和下一轮未执行；无遗漏的六条合同分支。9条脚本定位/状态校准错误及截图原样保留，校准后同一业务数据继续，没有重放提交或更改业务状态绕过。旧版入口先正文预览、再只读详情，其锚点不同于管理详情。

浏览器closed=true、context=0、PTY76643 exit0。13份trace CRC及162份原始JSON解析通过，36张截图、9脚本node --check通过；关键截图视觉确认内容并单列叠层问题。未读取业务响应JSON作预期；网络日志仅路径/方法/状态。分支、对象、限制及问题完整机器结果见rerun-20261020-handoff.json；原始证据目录：doc/tasks/20261016-dcc-e2e-03/rerun-20261020-round1-2026-10-20T01-34-50-748Z。

当轮无生产/公共配置变更、无DB/API业务调用、无构建重启及Git提交推送。历史服务端临时资产未重新核验，不把浏览器关闭冒充服务器清理；本次失败选择经UI移除或取消重开，成功材料已进入本次申请/版本。工程完成门禁与业务PASS分开记录。

# E2E-03 源件与多附件：本轮真实执行报告

## 2026-10-19 R5 原batch14页面验收完成

Root r5-runtime-proof.json已按R5正式字段、Jar SHA及服务/响应VO类SHA核对。实际命令沿用repair-20261019-r4-readonly-with-role.cjs（本轮输出标R5），exit0；原D新context经正式浏览页进入4186/A2/BPM，再打开发送与签收。六人接收部门均为DCC241016C03部门，原16:31:51—16:31:54发送时间保持，均已发送；本批已全部送达，无当前失败时间、error alert或重试按钮，登录后业务写请求0。业务断言只取页面DOM，不读接口JSON或DB。

repair-20261019-r5-role-ui-2026-10-19T15-24-47-809Z保留临时绑定前精确原审签、绑定910233回读及finally撤回回读，最终仅原审签；repair-20261019-r5-batch14-ui-2026-10-19T15-24-56-531Z保留六行、面板截图与请求元数据。两个浏览器均closed/0contexts；三trace已落盘。截图经人工视觉复核，角色恢复及部门/发送事实可见。详见repair-20261019-r5-handoff.json。

前端43、后端20和原六组PASS保留，未重跑通过分支；当前FAILED的失败时间条件由原前端测试覆盖，本轮未在原批次制造失败。无发送、重试、签收或新版本；平台2620—2625/jobs118—123只读佐证仍交Root。未修改生产代码、构建、重启或Git。历史临时资产账中的服务器状态NOT_VERIFIED保持，不能用浏览器关闭或本轮角色恢复冒称清除。

以下R4及更早记录为历史，R5新包页面验收已解除其中“等新包/页面未执行”的旧阻塞。


## 2026-10-19 R4后端定向RED/GREEN完成；新包展示待Root

本轮以repair-dispatch-20261019-r4.md为准：17先持有Maven，03须等Root明确放行。R3所述宽权限角色禁止重绑已被R4明确授权替代：仅允许真实UI临时给任务D910417绑定已有doc_control910233用于原batch14只读，保存原角色并最终恢复；公共角色定义、菜单和数据权限仍禁止修改。

R4运行器复用原只读batch14脚本，新增授权前原角色落盘、授权结果不确定时finally独立UI回读恢复、已恢复时不重复保存、其他角色组合拒绝猜改。仅当批次六人展示与原时间核对通过、无业务写请求、角色精确恢复且浏览器关闭均成功时才有总PASS。真实页面证据将分别保留只读脚本原输出目录与R4角色恢复目录。新包证明缺失或未明确包含部门投影时在浏览器启动前退出。

准备检查实际exit0：3个脚本语法通过；缺运行包证明及旧reboot1019证明各实际exit1，均为预期门禁拒绝，未生成R4 UI目录、未启动浏览器或授权。这不是产品RED/GREEN，也不是E2E PASS。命令/退出码见repair-20261019-r4-preparation-checks.json。

先用归档HEAD服务/VO作为基线运行原departmentName断言：Maven exit1，编译成功，目标测试1 run/1 failure，精确为JSON `departmentName` 期望“实际接收部门”而实际空值。原始Surefire XML及txt、stdout/stderr和源指纹在repair-20261019-r4-backend-red-2026-10-19T13-59-32-439Z。首个执行wrapper因stream finish监听时序没有落盘finally日志；立即用同次前置备份恢复两份固定文件并逐一hash核对。该证据问题已披露，断言结果以Maven stdout、Surefire XML及恢复hash为准。

第一次GREEN发现旧baseline `.class` 时间晚于恢复后的固定源码，Maven报告Nothing to compile并重现同一个断言失败；这次不是固定源码GREEN，XML/log单独保留于repair-20261019-r4-backend-green-2026-10-19T14-02-17-363Z。更新runner在RED/GREEN前显式推进两源文件mtime，RED替换基线，finally恢复固定备份并校验内容hash；有效GREEN实际编译1320个主源码和569个测试源码，Maven exit0，四个fresh XML共20测试、0 failure/error/skipped，见repair-20261019-r4-backend-green-2026-10-19T14-04-40-324Z。GREEN结束源码恢复hash与固定SHA一致，投影/IdentityJson/Facts/ServiceImpl四类目标回归全通过。原前端43PASS及六组验收保留、未重跑。

R4后端窗口现已释放给Root，handoff列出准确命令、退出码、fresh XML、源码与测试指纹。Root统一新包和有效proof之前，batch14真实UI、临时D角色授予及精确恢复均NOT_RUN；未发送/重试/签名/签收，无构建打包或服务重启、无Git提交推送、cleanup apply或证据删除。历史临时资产未知状态保留。

以下R3及更早记录仅说明历史结果，当前调度与资格边界以上述R4为准。

## 2026-10-19 R3当前：资格只读核验完成，后端与新包展示未放行

前端R2实际RED/GREEN43及原六组验收保留，本轮没有重跑或修改生产代码。24独占Maven、14独占类型检查；03仍须等待Root按24→13→17→03→10明确放行，不能因无Maven进程就自行占用。Root当前快照为idle/已派工，尚无03窗口或包含部门投影的新包证明。后端RED/GREEN仍NOT_RUN，不把准备基线、源码审查或脚本语法通过当测试通过。

真实Playwright只读管理页核验exit0：D910417仍只有DCC241016C03审签；唯一现有doc_control910233仍含压力泵900450、打印605071312、下发菜单和全部数据范围。0角色/菜单/数据权限写请求，0新增临时授权，浏览器closed、0contexts；trace及截图在repair-20261019-r3-qualification-2026-10-19T11-51-02-347Z。仅核验资格，未访问batch14，更非展示修复E2E PASS。

最小角色前置存在正式配置冲突：DccDistributionTaskServiceImpl.requireDistributionScope要求hasAnyRoles(userId,"doc_control")；PermissionServiceImpl.hasAnyRoles只按精确角色编码匹配；RoleServiceImpl.validateRoleDuplicate对创建/编辑均拒绝重复编码，角色分类不参与例外。因此新建任务自有、不同编码的最小角色不能通过守卫，同编码又不能创建。没有尝试重复保存、修改公共角色、重新绑定旧宽权限或弱化守卫。该资格须由Root明确合法方案，当前NOT_VERIFIED/BLOCKED。

已保存HEAD原服务/VO两份RED基线及baseline/fixed SHA、精确RED和GREEN回归命令，见repair-20261019-r3-backend-plan.json。只写任务目录，未替换共享源码或改target；执行时Root须独占窗口、确认源码指纹，原服务/VO配新测试得到实际departmentName断言RED，再恢复固定实现执行GREEN和相邻回归。编译/环境失败不能冒称RED。新包后仍仅回读原186/A2/BPM/batch14六人原时间/部门，不发送或重试；未知临时资产继续沿用R2精确账目NOT_VERIFIED。工程无Git、无cleanup apply，证据保留。

## 2026-10-19 R2当前：通知展示修复准备，未宣称新运行态通过

Root新派工只重开SENT历史失败时间/部门名称展示。前端原SFC RED已复现SENT仍有失败时间；状态判断修复后43轻量回归通过。后端逐人响应补departmentName，通过distribution.departmentId读取正式DeptApi名称，不借用接收人当前部门/签名快照，不修改Query、不回写历史，不改通知发送。新增后端投影与无写入回归尚未运行，遵守13先占Maven及Root串行构建排期；也未运行全量类型检查。详见repair-20261019-r2-handoff.json。

旧六组及原batch14发送Root验收结论继续保留；没有重发、重签或新版本。重启后旧workflow-v6/newFixesIncluded=false不能证明部门修复。新定向只读UI脚本已准备，等待Root带该修复的新Jar/健康证据、原D最小可恢复资格安排，再核原六人部门/身份/发送时间/无重试。无重复平台消息由Root按原2620—2625/jobs118—123只读前后核验，本线程不API/DB替代页面。

失败临时资产状态已逐项补列：NET01正式UI清理完成；早期identity-fix失败源件无可见选择/票据，serverTemporaryState仍NOT_VERIFIED；offline拒绝源件可见行移除但服务器状态未核验；检入失败正式cancel/reopen与185/186已绑定票据分别记录，不把关浏览器等同服务器清空。禁止猜测清理，复核证据与既有cleanup preview981项全部保留。技术ctx修正披露与自然refresh NOT_VERIFIED不变。以下为上轮已接受记录。


## 当前最终结论：业务已获 Root 最终接受，工程 Git/cleanup 未完成

Root 于 2026-10-17T17:41:07.064408+08:00 完成最终review：原六组PASS，四正文逐内容、新两下载逐bytes独立比对通过；原batch14六人SENT且无新增平台消息。D仅原991295Review，Author三临时project menu已撤销、原签名菜单链仍在，0 context/browser/driver。运行最终PID40784/48061，SHA6379718091162ce1e80a1d7a5a08af8a3c65483285612f11d9cd36a55b232d35。根证据：[Root验收](../20261016-dcc-24-e2e-coordination/e2e03-root-review.json)、[通知最终只读核验](../20261016-dcc-24-e2e-coordination/e2e03-notification-final-readonly.json)。工程Git/cleanup未完成，非业务阻塞。

原 U1/U2/U3/U4/R1/R2 六组 PASS 维持，完整证据索引为 six-groups-final-review.json。OV20 运行 PID40784/48061、SHA6379718091162ce1e80a1d7a5a08af8a3c65483285612f11d9cd36a55b232d35，Root health/UI proof 已核。原186/A2/BPM d65124cd-c9ed-11f1-9408-b082e25ec548/batch14，正式面板先核 U 一人 SENT、五人旧证据错误 FAILED。17:30:36.661 仅单击一次重试，17:30:39.322 HTTP200；重新经原文件正式面板读回六人已发送，全未打开/未签收，无重批/重签/新版本。UI 未打印 numeric batchId，14 由 Root 唯一核定当前文件版本/BPM；未解析请求体/响应体/组件状态作 oracle。

| 接收人 | 终态 | 原实际发送时间（页面回读） |
| --- | --- | --- |
| U | SENT | 2026-10-17 16:31:51（前后整行一致） |
| S | SENT | 2026-10-17 16:31:52 |
| L | SENT | 2026-10-17 16:31:53 |
| C | SENT | 2026-10-17 16:31:53 |
| A | SENT | 2026-10-17 16:31:53 |
| D | SENT | 2026-10-17 16:31:54 |

本次 OV20 重试1次，含修复前历史失败的任务总重试2次；没有再点击。Root最终只读核验已确认平台原消息2620—2625及原发送时间/参数完全未变、没有新消息，原job118—123全部SENT；独立核验来源为 ../20261016-dcc-24-e2e-coordination/e2e03-notification-final-readonly.json。本线程真实UI仍是业务验收依据，未以直接接口/DB替代页面动作。原 FAIL、后置脚本失败和此前一发五失败均保留，未覆盖。

成功后立即用系统用户910417分配角色弹框移除临时doc_control910233，重开只 DCC241016C03审签；ledger restored=true。关闭旧有资格 D context，再 fresh 官方 D 登录；原186详情的“发送与签收”及“重试自动下发”入口均0。脚本1868误期望恢复后仍能打开面板而超时，属验收脚本预期错误且无业务点击，原始response保留；1870按实际入口消失独立留证，不判产品失败。Author临时项目菜单继续保持已撤回，不重新增加；公共角色定义未改。

完成 trace 保存后，关闭剩余14个任务 context及其 browser（0 contexts、disconnected），核对专有 ui-live.cjs --file-queue PID14720归属后停止driver并确认不存在。未动前后端服务和并行线程资产，业务文件/账号/权限fixture保留给Root复核。最新证据密钥扫描0。

技术披露保持：Root对185两个内部ctx字段做必要修正，原事实/业务身份保持，实际业务仍由真实页面完成；原U OWNER为既有初始化事实。自然refresh补充试验仍NOT_VERIFIED、Root已移出原六组门禁。旧A1已作废下载STATUS_NOT_ACTIVE/按钮0为不适用。另观察到通知SENT面板仍显示“实际失败时间：2026-10-17 17:30:37”，原截图/DOM保留；Root已将历史failureTime与部门标签缺失一并接受为非阻塞展示备注，不扩大修复。

当前业务验收与资格恢复已获Root最终接受（../20261016-dcc-24-e2e-coordination/e2e03-root-review.json），业务阻塞全部解除、review不再pending；遵从用户禁止Git提交/推送，不能标仓库任务completed。已找到task-closeout-cleanup/scripts/task_closeout.py独立runner（该目录无SKILL.md），preview exit0但拟删981项，包括Root需保留证据，因此不执行apply。初次仅按文件名搜索误判缺runner的记录已纠正，未作为最终阻塞。下文均为历史，若状态不同以本节及six-groups-final-review.json为准。


## 历史：原六组完整PASS；当时新增batch14恢复部分失败、D临时角色待恢复

**原E2E03六组已通过，整体授权任务仍BLOCKED。** 原A1文件182已作废/后继186；同原名但不同正文内容检入工作稿185，经PARTIAL A1→A2确认/取消保持、唯一正式提交及原S/L/C/A/D完整签名，186/BPM d65124cd-c9ed-11f1-9408-b082e25ec548已ACTIVE。初始和升版各源件/每附件真实上传中/离线失败阻止/online恢复及保存失败恢复均独立留证，没有以初始负例替代升版，也未把HTTP200业务拒绝或后置脚本错误写成成功。

| 原组 | 真实结论 |
| --- | --- |
| U1 | PASS：源件/双附件就绪、保存回读、确认/取消保持、初始提交及受控生效 |
| U2 | PASS：初始与升版逐源件/两附件真实上传中禁用或实际处理程序阻止 |
| U3 | PASS：两链路逐材料真实context-offline上传失败、提示、阻止及正式UI恢复 |
| U4 | PASS：初始真实审计保存失败恢复；升版真实offline保存失败、原名合同拒绝保留、窄fixture纠正后保存185 |
| R1 | PASS：PARTIAL A1→A2实际完整审批/新ACTIVE、原版已作废且后继186 |
| R2 | PASS：旧182只A/B、新186只RA/RB；四正文实际在线查看逐内容一致，新版两下载逐字节一致；旧已作废下载STATUS_NOT_ACTIVE、按钮0，按Root接受不适用，不改状态/权限绕规则 |

R2原viewer/cache FAIL均保留，成功新文件独立命名。WebStorageCache正式两层解包v2在同页成功旧B弹框确认user910412/tenant1；早期个人中心采样/旧v1子模块require缓存不充分，不据此声称“成功预览时无USER”。原U自然到期正常导航转login、无refresh观测，补充实验NOT_VERIFIED保留；Root明确它不是原六组额外完成门禁，不再等待30分钟/重登制造PASS或扩认证排查。

技术及资格披露继续有效：Root仅修已存185两内部ctx字段，所有实际业务UI完成；原U OWNER是项目批准初始化已有事实，本轮未提升。为D VIEW临时Author角色只增加990200/990210/991106项目链，已正式UI撤回且fresh U确认原上传/浏览/签名菜单保留、项目菜单消失。项目291最终仅U OWNER + D VIEW；category908744 D仅VIEW/DISTRIBUTE；base913895 D仅query/preview，U query/preview/download，PFS913901/mapping未改；admin原指派未改。

新增通知复验：existing doc_control910233已知含本轮无关压力泵/打印菜单及全部数据范围，Root明确批准只对D910417可恢复临时绑定、只办原batch14，不改公共角色/不使用无关菜单。仅一次真实“发送与签收→重试自动下发”，原6人effectiveDate缺失失败留存，POST automatic-retry HTTP200后详情reload卸载弹框使后置等待失败；没有重复点击。重新打开同file186/A2/BPM的正式结果确认批次仍FAILED：U已发送，S/L/C/A/D五人 **DCC notification platform send evidence is missing or mismatched**。尚非通知通过；未签收、重批、重签D或新建版本。Root已禁止再次发送并追平台已创建/域ACK事实，D临时资格按最新指示暂留，必须后续UI移除回读原Review并关闭该D context。

当前D无弹框、context online、唯一batch retry1；Author临时菜单已恢复。旧batch13未重试，D历史A1查询未出现该行，保留读取失败，不强发已作废版本。原六组证据six-groups-final-review.json，新增失败original-batch14-after-single-retry-partial-failure.json，所有原FAIL保留。整体task未ready/completed，等待Root新错根修与临时D资格恢复；没有执行Git提交推送或cleanup apply。下文为历史。

## 历史：A/2完整审批ACTIVE，R1通过；R2新附件下载通过，在线查看身份前置待修复

既有网络时序补充：fresh U 14:21:10登录/权限200→14:24:33自然refresh-token200→14:32:28检查USER键缺失。但base授权后的首次预览FAIL在14:22:57已发生，早于该刷新；不能只凭这次刷新解释更早失败。fresh-U-refresh-token-timeline.json记录完整时间边界，仅既有脱敏日志只读核对，无新页面动作。

**当前5组业务覆盖已达相应通过，完整E2E仍因R2未完成而BLOCKED。** OV16新后端10976/SHA9dcf9bb3240e9bc680ccc0818b26971b491702c4203c14e8b37c7596c61f1bf7下，原U从已存185自然读取服务上下文，确认A1/工作稿/A2/RA RB、取消保持后唯一POST真实生成正式候选 **186（2054545668044084186）** 与BPM **d65124cd-c9ed-11f1-9408-b082e25ec548**。随后原S签名779、L指派780→C签名781、A批准782、D文控783，完整UI流转；fresh U当前有效版列表及loaded详情确认A/2 ACTIVE、受控/生效/启用时间、发布/盖章件可用，旧182版本历史为已作废/后继186。新RA/RB两行独立，loaded身份与附件局部图已保存，未拿加载中长图作附件验收。

技术修正披露：Root对旧代码保存的185错误ctx在精确备份、副本/实际first1 repeat0验证后只修两个内部ctx字段；182全行及185其他事实/签名/附件/属性/Master摘要保持，没有创建/提交/审批业务。该技术前置不冒充E2E业务动作；本例所有实际提交和签名仍由真实页面完成，不手工改key/缓存/组件状态。旧幂等冲突FAIL保留，新独立成功证据未覆盖历史。

R2权限前置精确收敛：类别DOWNLOAD已补仍见DIRECTORY_DOWNLOAD_DENIED。Root只读核对实际PFS913901与唯一base913895/DCC241016C03DIR映射，禁止直接手绑leaf。admin原指派看不到base，带913895页面竟选中质量管理，已拦住写入（目录写0）；本例既有Config真实目录页可见base。其正式权限页树节点data-key913895/任务目录名核对后，仅保存U查询/预览/下载，唯一PUT913895/access-rules，重开回读；页面未保存QA默认草稿先移除，不把它写成既有QA授权删除。PFS手绑标志/映射、global/OWNER/PRINT/admin原指派均未改。fresh U显示DIRECTORY_PREVIEW和下载放行，RA/RB真实下载内容逐字节匹配，sha与111/139字节正确。

R2仍缺：RA在线查看原权限FAIL保留；base授权后实际点击报 **“正文身份缓存尚未就绪”**，没有附件metadata请求。Root明确只读检查三值后，证据为localStorage user不存在、tenantId包装值string1、visitTenantId不存在；页面U可见/get-permission-info自然响应200。未输出token/cookie/password/整USER，未写缓存或绕守卫。旧版历史入口当前viewer同样无内容身份加载，旧附件列表及实际查看/允许下载尚未完成，不将历史状态图或A2下载冒充全R2。

自动下发仅独立观察：新A2详情有下发时间及已下发待签收；Root只读报告batch14 FAILED/6消息模板缺effectiveDate，原batch13也是独立失败。未重签D、改生效日期/模板或反复重试通知，未加doc_control角色/菜单或扩签收。Root正在换通知修复Jar，本例已按安全点暂停，context online且无新业务/预览下载/重试。当前身份JSON供Root根修viewer，等待明确恢复。

最新结果A2-R2-progress-result.json，全部实证在custom-context-resume-artifacts；下载文件在downloads，身份三值在preview-identity-three-values.json，停点notification-deployment-pause.json。U1/U2/U3/U4（初始+升版逐材料/保存负例）与R1保留通过；R2部分通过，在线查看与旧版实际下载/隔离仍待续。凭据扫描0。下文为历史。

## 历史：PARTIAL确认已GREEN，原185唯一POST被服务端幂等冲突拒绝

**完整E2E仍BLOCKED，不能把HTTP200当提交成功。** Root OV14/15恢复后，原U fresh context通过原工作版本入口重开185，仅选PARTIAL；实际确认框列齐原源件、RA/RB、A/1基线、草稿正文及A/2目标，取消后说明保持。原custom intent前端守卫错误已真实越过，确认结果PARTIAL-A2-confirmation-result.json为PASS。

随后只发出一次POST /controlled-files/2054545668044084185/submit（12:55:51.151），HTTP200但页面原文 **Controlled file submit idempotency key conflicts with another request**。运行日志12:55:51.259对应WorkflowServiceImpl.submitWorkingIteration:1085 / CONTROLLED_FILE_SUBMIT_IDEMPOTENCY_CONFLICT（1080000342）。源码此处核对同tenant/user/key的既有提交结果与本次iteration/master/hash/BPM，前端saved custom分支以saved.applicationContextKey作为key；实际key/既有结果字段未由本例读取，Root追原新工作稿身份来源。不生成新UUID、不绕过守卫、不修改业务状态。

失败UI与唯一POST已持久化后，再以原U独立新context只读全部版本：仅A/1和草稿两行，无A/2；从该草稿行重开仍是185、BPM未绑定、RA/RB两行保留。新只读会话POST/PUT/PATCH/DELETE为0，无并行候选或新BPM可见；未直接检查全量DB事务。Root禁止再提交后未重试、未改key/新建/重检出。浏览器/驱动已关闭、cleanupError=null，凭据审计0。

最新结构结果partial-submit-idempotency-blocker.json及revision/root结果同步；精确失败UI、服务器摘录、确认框/取消、fresh全部版本和工作稿身份局部图及两份trace位于partial-intent-resume-artifacts。U1—U4及初始/升版负例证据保留；R1已完成确认，正式提交/完整审批仍阻塞，R2正式旧新版实际查看下载隔离尚未执行。等待Root窄修复明确恢复后只从同185继续。下文为历史。

## 历史：升版工作稿185已保存，PARTIAL提交被自定义意图守卫阻止

**当前BLOCKED，R1/R2未完成。** 原A/1 ACTIVE保留；原U完成唯一检出，同逻辑文件检入生成工作稿 **2054545668044084185**。更改正文保留正式原名DCC241016C03-source.pdf，使用不同本地目录，内容/哈希从2ad539f9095e变为0b93d989ae58；RA/RB附件不同于初版A/B。工作稿详情按实际加载后截取局部身份图和附件图，显示只属于185的RA/RB两行，不用早先加载中长图证明附件或账本。

升版侧SOURCE/RA/RB已分别真实观察上传中阻止、BrowserContext离线上传失败及online恢复，初始负例未替代升版证据。源件按钮可点但实际点击被失败/上传中处理程序阻止；RA/RB原生禁用，ElUpload失败行自动退出而自定义失败状态保留，使用正式取消检入清理/重开同一检出恢复，不声称有不存在的单项删除按钮。首轮源码失败行class定位FAIL原JSON保留，复验GREEN另名保存；RA同样分开保留。

检入保存一次真实offline请求失败、三材料保持；online返回HTTP200但被原名合同拒绝，实际UI“检入请求缺少真实文件或修改说明”已从原trace精确帧保留（不是保存PASS）。新U只读全部版本仍唯一A/1、原U检出未变；正式取消清理/重开，按合同将新正文放在不同目录但保持原名后，单次检入成功，185从真实页面导航取得并回读RA/RB。没有另建Master、重复checkout或重签原D。

按Root明确fixture授权，admin真实类别页仅补本例U缺少DOWNLOAD，原VIEW/UPLOAD保持，未加READ/PRINT/OWNER/角色/菜单。首次保存后重开loading-mask定位FAIL保留；独立admin只读重开确认3条规则均启用，新U重新真实登录。此项为前置，不是产品代码缺陷；实际旧新版下载仍待R2执行。

仅预览过REPLACEMENT，无正式请求或候选；随后按Root收窄为PARTIAL目标A/2。原页面和完全独立fresh U直接PARTIAL都在属性读回后报 **“请先读取并核对所选正文的已保存申请属性”**，正式submit请求0。Root确认DetailApplicationPanel.selectIteration的APPLICATION_CUSTOM分支遗漏snapshotIntent=intent，公共分支有赋值，提交守卫因此永久不匹配。生产代码冻结由Root补BDD/RED/GREEN及最小一行修复，本例不注入状态、不反复保存/提交、不改矩阵或权限。185及其材料保留，未产生新正式候选/BPM。

最新结果revision-resume-result.json及根结果已同步；初始/升版U2/U3/U4材料覆盖PASS独立列明，R1正式确认/提交/完整审批与R2旧新版实际查看下载隔离继续BLOCKED。浏览器/驱动已关闭、cleanupError=null，证据凭据扫描0。等待Root明确Vite就绪后仅恢复同185 PARTIAL，不重新检出/检入。下文为历史。

## 历史：OV10原D通过、初版A/1 ACTIVE，暂停于首次检出前

Root新运行PID27124/Jar SHA4c02f2e6ba13a725a8e79649718f2b6320e9c66b7eb8b48c5bc401ad8f221dbe下，原D任务只签一次，approve-task HTTP200并返回待办。随后原U fresh context从真实受控浏览当前有效版唯一行打开原文件2054545668044084182，实际显示A/1 ACTIVE，受控/生效/日常启用时间及发布/盖章件可用、原双附件共2个。真实终态PASS，不以HTTP200或审批列表缓存行判定；D不重签。原预检脚本S772账本加载过早断言FAIL保留，不冒称完整前置PASS。

U已从原详情“检出 / 检入”进入目标浏览行，当前ACTIVE及“检出”入口可见，尚未点击检出。Root要求为精确审计策略补齐稳定审计表，已在此安全点停止所有新业务及预览下载动作；context online、浏览器保留，等待Root恢复。Root报告独立自动下发batch13 FAILED/缺dcc.distribution.batch.prepare策略；本例当前详情发布后续/分发区未显示批次具体错误，仅按Root报告另记，不假称UI核验batch13、不重试下发、不扩签收。该下发前置不作为重签D或新建文件理由。

最新结果activation-time-resume-result.json；initial-active-result.json、audit-policy-pause-checkpoint.json及OV10-initial-active-before-audit-policy-pause.trace.zip位于activation-time-resume-artifacts。初始SOURCE/A/B上传中及离线失败/恢复PASS独立保留；升版侧这些参数、保存恢复及R1/R2尚未执行，完整E2E未PASS。

## 历史：OV09运行态原D单次恢复出现自动下发事实新阻塞

**完整E2E-03仍BLOCKED。** Root实际库两列迁移及OV09新包PID23792/48061、Jar SHA366200e49d5c6b3a94b0b4374ae7f2cde92aaebd181edbea175fffcad9e6f57c恢复后，先以D新context只读核对原file2054545668044084182/BPM3bbac54d-c992-11f1-8f08-b082e25ec548/任务2b7beaca-c994-11f1-8f08-b082e25ec548仍待文控0/1，无D签名，前序772—775保留。然后仅对该原任务签名一次，未重签S/L/C/A或重建申请。

11:28:19原approve-task请求→11:28:21 HTTP400，当前页面明确提示 **“自动下发缺少同版本受控生效事实”**。原DCC_CONTROLLED_EVENT_INVALID本轮未出现。失败弹框取消前保存visible DOM、完整任务身份及字段错误信息，密码值未保存；凭据输入期间trace关闭。运行异常code1080000014仍经FinalizationService:481抛出；源码对应DccDistributionBatchService.requireFile:231—236对同版本租户/主档/版本/BPM、controlledTime/activatedTime、事件时间及状态的核对。未读取实际事件字段，不猜唯一失败谓词；生产最小根修归Root。

Root明确禁止再签后，未再确认任何签名；取消原框，再以D新context从真实待办重新读取同file/BPM/task。仍待文控0/1、尚未受控/生效、S/L/C/A签名772—775及双附件保留、签名账本无D，页面事实与本次命令回滚一致。未直接检查全量数据库事务。后置签名弹框隐藏等待FAIL保留，实际产品错误以上述原页面提示及HTTP400为准。

证据D-auto-distribution-blocker.json及controlled-time-resume-artifacts内D-single-resume-failure-dialog.json、D-auto-distribution-runtime-error.txt、D-after-single-resume-readonly-result.json与两份trace。浏览器/驱动关闭、cleanupError=null，凭据审计0命中。初始SOURCE/A/B上传中/离线上传失败恢复已有PASS继续独立保留；升版侧上传/失败/保存恢复及R1/R2仍需初版ACTIVE，未执行、不冒用初始证据。下文为历史。

## 历史：原初始申请已提交，D最终受控事件失败并完成只读回滚核对

**失败参数补验（独立初始上传NET01，原D保持）：** 已按Root明确授权，仅本例独立BrowserContext.setOffline产生真实net::ERR_INTERNET_DISCONNECTED。源件、附件A、附件B分别观察Network Error与对应文件选择项，恢复online后真实移除失败项并重新选择上传就绪；未mock/route.fulfill、未API上传、未改变共享网络/服务/策略/DB。源件提交可点但被实际源件身份字段校验阻止；源件上传进行中真实点击提交显示“请等待当前项目属性读取或提交完成”。A/B各自上传中及失败后均原生禁用提交。三材料逐项证据initial-offline-failure-coverage.json，整份初始草稿历史保存失败与恢复仍单独关联，不伪称逐材料独立保存失败。

初次独立分支使用原源件名，online恢复被正式名称占用规则拒绝；该真实服务端拒绝与脚本错误预期toast原记录保留。后改用专用NET文件名重新完成三材料失败/恢复，未改原文件。所有独立临时件经页面清理成功、context恢复online、浏览器关闭，formal save/submit/sign请求0，证据审计0命中。升版侧source/A/B上传中/失败、升版保存失败及R1/R2仍等待ACTIVE基线，原六组不省略。

**完整E2E-03仍BLOCKED。** 新运行PID16776/Jar SHA494306625fb1d04261832e4145a8aa83c3734257c02069e9b1e7644749efcdec下，原U新context真实重开原文件2054545668044084182：详情及双附件共2行回读GREEN，正文非空canvas预览GREEN；确认框列齐源件/双附件，取消保持输入，再真实提交为A/1。原BPM **3bbac54d-c992-11f1-8f08-b082e25ec548**，双附件冻结在该轮次。初始建档只成功一次，没有重传/重建。

S主管签名#772、L会签指派#773→C独立会签#774、A批准#775均通过正式详情电子签名完成；A确认本版负责人U。L签名后页面返回待办，脚本等待原指派页提示超时属于后置断言错误；C本人新会话正式待办及签名证明指派已成功，没有重放指派。A首次签名停在正式负责人二次确认，未发approve-task，取消后补齐该确认再签一次；原raw失败响应保留，不冒充业务失败。

D任务 **2b7beaca-c994-11f1-8f08-b082e25ec548** 在03:03:36返回approve-task HTTP400，页面原文 **DCC_CONTROLLED_EVENT_INVALID**；精确异常code1080000014经FinalizationService.finalizeOrdinaryApproval:481抛出。调用栈、原错误DOM、失败签名回执均保存在overnight-artifacts。源码事件消费器把occurredAt传给recordControlled；该方法校验时间纳秒必须为0，而日期策略now保留纳秒，属于有源码支持的精度根因候选；未捕获实际事件全字段，不把该候选伪写成唯一已证实谓词。生产最小根修归Root。

收到502恢复指示后先核对本例驱动无存活、浏览器上一轮关闭，再以D全新context只读进入原待办。原fileId/BPM/taskId完全相同，仍“待文控审核”、阶段0/1、尚未受控/生效；本轮签名账本只有S/L/C/A的772–775，无D签名，双附件仍在。该页面事实与失败命令回滚一致；没有直接检查全量DB事务。新会话未点击审核/签名、未重试D、未重签前序身份。浏览器再次关闭、cleanupError=null，证据凭据扫描0命中。

| 原组 | 当前结论 |
| --- | --- |
| U1 | 初始上传就绪/保存回读/确认取消保持/提交及同轮附件清单PASS；不宣称最终生效或附件下载通过 |
| U2 | 初始源件上传中实际处理程序阻止、A/B各自上传中禁用PASS；升版侧参数待ACTIVE |
| U3 | 初始SOURCE/A/B逐项真实网络上传失败/提交阻止/online恢复PASS；升版侧参数待ACTIVE |
| U4 | 初始真实审计保存失败→策略恢复→原草稿回读及提交PASS；D后续错误独立记录 |
| R1/R2 | 初版未ACTIVE，升版检入及旧新版隔离BLOCKED |

最新结果doc-control-finalization-blocker.json及root-workflow-resume-result.json。原只读核验结果rollback-readonly-artifacts/D-rollback-readonly-result.json，trace fresh-D-original-task-rollback-readonly.trace.zip。原6组范围保持；Root明确恢复前不得再次D确认、重建申请或重签前序人。下文为历史。

## 历史：详情TDZ复验GREEN，后端只读事务读取失败

**初始化修复定向PASS，完整详情及E2E仍BLOCKED。** 原U全新浏览器/context经真实菜单→受控浏览→全部版本→原编号唯一行→操作面板，重开原文件2054545668044084182。pageErrors=0、consoleErrors=0，页面组件已渲染，已自然发出原文件GET详情请求，越过前两轮TDZ。

详情GET HTTP200不代表业务读取成功：正式编号仍为“-”、无正式版本事实区，页面显示“发布后续加载失败/系统异常”。同时间运行日志准确报 **Cannot execute statement in a READ ONLY transaction.**（SQL state25006/error1792），调用链为附件服务requireAttachmentVersion:252→readOrdinaryAttachmentManifest:177→QueryService.toRespVO:3726→getControlledFile:626。源码表明只读清单方法调用FOR UPDATE版本锁读取，与详情readOnly事务冲突。生产修复由Root负责，本例未改生产代码或事务设置。

源件内容及双附件回读均BLOCKED；页面空清单属于详情未读取状态，不是附件数量或丢失证据。本轮受控业务写入0、未重传/保存/新建/提交/审批，原fileId及全部fixture保留。最新结果detail-readonly-transaction-blocker.json、根结果同步；诊断、系统异常截图、精确运行日志摘录及tdz-cleared-detail-readonly-transaction-failed.trace.zip位于detail-green-artifacts。浏览器已关闭，cleanupError=null，凭据扫描0命中。

Root修复运行态后仍先用U新context只读回看原文件正文和双附件，再继续原申请/审批/升版及六组缺失参数；未将定向初始化GREEN替代详情完整成功或整例PASS。下文均为历史证据。

## 历史：原文件只读恢复触发controlledFileId TDZ

**当前BLOCKED；本轮受控业务写入0。** Root移动currentUserId后，用原U全新浏览器/context登录，从可见文控中心→受控浏览→全部版本搜索原编号。唯一行显示DCC241016C03-source.pdf、DCC241016C03F01、原项目/文件夹、A/1及工作版本，证明原保存结果在列表可见；未重复保存或上传。

点击该行“操作面板”进入原文件2054545668044084182，实际pageerror为 **Cannot access 'controlledFileId' before initialization**，不再是currentUserId错误。watch(delayedConfirmationIdentity)求值applicationRoundSelection时，controlledFileId尚未声明；源码另有较晚声明的viewerMode/isBrowserTraceabilityPage依赖需Root一并核对，此处仅为静态风险，未伪称运行已触发。详情读取请求0，正文内容与双附件保存回读仍BLOCKED，不能判附件丢失或断言草稿完整恢复通过。

最新结构结果controlled-file-id-tdz.json；完整诊断、列表截图、空白截图及original-file-controlledFileId-tdz.trace.zip在detail-recovery-artifacts。浏览器已关闭、cleanupError=null，业务数据及原fileId保持，凭据扫描0命中。Root继续负责生产修复；修复后先只读重开，再继续原六组申请/审批/升版。旧TDZ和审计失败证据均保留，下文为历史。

## 历史：草稿保存后currentUserId初始化失败

**完整E2E-03仍BLOCKED；详情组件真实运行FAIL。** 本节取代下文过期OWNER归因。类别缺少UPLOAD触发了误导性的owner错误文本；已仅对本例类别DCC241016C03CAT为U增加UPLOAD及必要VIEW，保存重开核对两条规则。未提升OWNER、修改项目/共享权限或重建夹具。

源件PDF及两个不同附件真实上传并就绪，附件上传中提示及提交禁用断言局部PASS；前次PDF非空canvas证据保留。首次保存曾实际提示“GxP 审计策略不存在或未启用：dcc.project-file-placement.upload-bind”，保留失败trace及截图。Root恢复该精确审计操作后，本次只保存一次，自然POST working HTTP200，并跳转文件 **2054545668044084182** 的正式详情地址。

详情仅显示应用框架，Playwright捕获 **Cannot access 'currentUserId' before initialization**。源码IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue中delayedConfirmationIdentity第3086行读取currentUserId，第3106行watch在setup阶段求值，而currentUserId到第3553行才初始化；尚未观察到该文件详情读取请求。这是页面初始化错误，不能将附件区域等待超时归为附件丢失。生产修复由Root维护。

| 原子例 | 当前结论 | 尚缺 |
| --- | --- | --- |
| U1 新建/确认 | BLOCKED；源件/双附件就绪及保存导航已观察 | 保存回读、确认取消保持、提交及申请查看 |
| U2 上传中拦截 | PARTIAL_PASS；本次双附件上传中禁用 | 源件及升版各材料参数 |
| U3 上传失败恢复 | PARTIAL_PASS；源件权限失败及补规则后恢复 | 独立附件及升版失败/阻止/恢复 |
| U4 保存失败重试 | BLOCKED；真实审计失败及重试导航均已观察 | 保存附件回读及后续业务恢复闭环 |
| R1 升版检入/确认 | BLOCKED | 初版ACTIVE及完整检出检入/确认 |
| R2 版本隔离 | BLOCKED | 旧新版页面清单、查看和下载隔离 |

当前结果：detail-initialization-blocker.json、root-workflow-resume-result.json。最新trace：audit-recovery-artifacts/audit-recovery-saved-detail-tdz.trace.zip；诊断及空白截图同目录。原upload-attachments-result.json最终FAIL保留，未改成整例PASS。凭据审计0命中；本例浏览器已关闭，自有驱动9048已退出，cleanupError=null；业务数据未清理，文件及全部自有fixture保留。修复后从原文件继续，禁止重复上传建档或绕过详情提交。

运行身份沿用Root证据：int_qms，8061/48061，后端PID34096，Jar SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce。未运行构建/服务操作、提交推送、API/DB写入或生产代码修改。文档/JSON结构验证另见execution-log。

## 历史：矩阵ID修复后的源件拒绝（OWNER归因已纠正）

**预检PASS，完整业务BLOCKED。** 全新浏览器进程/context中重新登录U并重选原夹具，实际显示“审批路线已就绪，共4个节点”。原定义和矩阵身份错误本次均已解除，根结果记录两个cleared=true。

随后点击正式“选择文件”按钮，通过真实filechooser选择本例原PDF；自然POST `/admin-api/dcc/controlled-files/upload-preview` 返回HTTP400，页面明确显示 **Current user is not the DCC project owner**。没有可见成功上传预览，未继续附件选择或保存/提交。

- Config真实项目管理列表按本例DCC241016C03P1查询无数据；A项目管理列表同样为空。本例产品可在产品目录查到，但项目代码没有可点击管理链接。正式OWNER权限入口在项目管理详情/行内，未能从上述真实入口补本例授权。
- 不把项目负责人文本当OWNER，不使用API/SQL补授权，不修改admin原指派、共享菜单、公共矩阵或全量范围，也不重建本例项目。
- 六账号岗位/授权/本人图片仍保留并已核验。原六子用例在新OWNER前置处BLOCKED；没有业务PASS。源件选择请求已真实发生，不能再笼统写本轮“全部受控请求为0”：成功上传0、保存/提交/检入/审批写请求0。
- 根结果：`root-workflow-resume-result.json`，业务续跑结果：`identity-fix-results.json`。证据：`identity-fix-artifacts/root-route-readiness.png`、`route-after-identity-fix.trace.zip`、`source-upload-owner-rejected.png`、`source-upload-owner-rejected.trace.zip`、`response-405.json`及入口核查截图。
- 源件内部input定位失败单独保存为脚本问题；之后真实filechooser已触发正式上传。没有删除失败断言或把请求错误改写成功。
- 所有本例浏览器已关闭，cleanupError=null。未能在剩余已认证页面找到失败源件选择，相关清理attempted/completed=false、serverTemporaryState=NOT_VERIFIED；未收到可见正式票据/文件ID，未假称服务端临时件已清理。配置夹具保留，原始结果继续保留。
- 新脚本语法/JSON结构与证据扫描通过，密码/Bearer命中0；前端生产代码由Root维护，本例未修改。

下文均为更早阶段历史记录；当前以本节及根结果JSON为准。

## 历史：V6隔离包恢复后的优先复验

**本轮BLOCKED的具体原因是“审批矩阵身份缺失或精度损失”，不是继续确认旧定义错误。** 运行PID34096、Jar SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce。Root优先结果已保存为`root-workflow-resume-result.json`。

- 六账号岗位、签名授权及本人签名图片均已真实回读完成；S/L/C/A/D新增图片，U原图复用，全部启用v1、640×180实际可见。各账号/原业务夹具未重建。
- 最新页面已完整选择本例项目、文件夹、类型、会签部门、批准人A、文控D；审批人链路仍显示不完整，具体提示为上述新矩阵错误。成功条件仍须“审批路线已就绪”，未放宽断言。
- U/S已在当前运行态再次保存并重开核对：启用开关true、正式岗位非空（本例岗位14）、部门正确；U当前直属主管S。证据在`workflow-resume-artifacts/applicant-supervisor-recheck.json`及对应截图。
- `previousDefinitionErrorCleared=NOT_VERIFIED`：没有以脚本未完成或新的矩阵错误为依据声称旧错误仍在。前一次readonly定位超时已修复为真实下拉选择，保留为独立脚本错误证据；没有注入控件属性/值。
- 确认前页面没有独立编制人/主管汇总字段，故只记录真实登录U、用户关系U→S及上传页可见批准/部门事实，未用接口JSON替代页面断言。
- 新预检截图`workflow-resume-artifacts/root-route-readiness.png`、`route-after-U-S-recheck.trace.zip`、六份`signature-ready-*.trace.zip`已保存。受控文件上传/保存/提交/检入/审批写请求0、流程发布0；浏览器清理成功，凭据扫描0命中。

下文为此前阶段记录，原始RED/BLOCKED保留；当前结论以本节及指定根结果JSON为准。

## 历史：资格准备进展及安全停止

已遵照Root重启前通知停止新增页面写入并关闭本例浏览器。账号、部门和原业务夹具均复用，未重建；完整业务E2E继续保持BLOCKED，待Root新运行PID/SHA与映射恢复通知。

| 前置项 | 实际页面结果 | 证据 |
| --- | --- | --- |
| 正式岗位 | 本例岗位14／DCC241016C03POST启用，保存后重开核对 | qualification-artifacts/post-manifest.json |
| U与S岗位 | 两者分别保存并重开回读非空正式岗位，不只核对主管 | post-readback-U.png、post-readback-S.png |
| 其余审签岗位 | L/C/A/D分别保存并重开回读同一本例岗位，部门归属保留 | qualification-artifacts/post-bindings.json |
| 六账号启用及签名授权 | U/S/L/C/A/D用户均启用、授权已启用、未锁定；刷新后回读 | signature-authorizations.json、authorization-reloaded-*.png |
| U本人签名图片 | U独立登录本人页面，图片上传启用v1后刷新回读，实际640×180渲染可见，文件名DCC241016C03U-signature.png，hash b0df039aedd7 | signature-images.json、signature-image-U.png、qualification-signature-U-completed.trace.zip |
| S/L/C/A/D图片 | 待续；仅准备本地任务测试图片，未上传 | qualification-results.json |

图片明确标为任务测试身份签名素材，不模拟真实人员签名。凭据不进入截图/trace；资格证据凭据扫描0命中。受控文件上传/提交写请求0、流程发布写请求0；浏览器清理completed=true、cleanupError=null。局部前置核验不能当作E2E-03业务通过。

本节取代下文“岗位/签名授权均未完成”的历史描述；旧运行态业务阻塞、原始证据和原用例范围均保留。

**此前结论：BLOCKED，无业务PASS。** 已通过真实页面创建7个专用账号、3个自有角色及配置夹具，项目创建已审核至COMPLETED；该旧运行态上传前正式流程定义校验失败，源件/附件及受控申请写请求为0。不能把账号创建、项目审核或静态检查当成E2E-03通过。

## 本轮新运行身份及授权

- int_qms；8061前端PID36084，48061后端PID43284。Jar SHA256：caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff。
- Root证据：`../20261016-dcc-24-e2e-coordination/unblock-final-runtime-health.json`，health UP；实际监听及最新dispatch身份已核对。
- 最新gate=execute、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture。已改用PER_CASE_REAL_UI_MANIFEST；共享roleFixtureVerified=false未作为阻塞，未修改共享dispatch。

## 本轮真实夹具成果

| 对象 | 本例标识及实际页面结果 |
| --- | --- |
| 配置账号 | DCC241016C03Config／配置员，910411；无部门/项目指派、无管理员角色；已独立登录 |
| 业务账号 | U/S/L/C/A/D，910412—910417；分别独立context登录，完成正式首次登录改密；六份identity截图与trace |
| 自有角色 | Author991293、Config991294、Review991295，角色名和code均带DCC241016C03前缀 |
| 组织 | DCC241016C03部门，负责人L；作者U直属主管S |
| 目录／模板 | DCC241016C03DIR；模板12 DCC241016C03Folders，独立项目文件夹 |
| 项目／产品 | DCC241016C03P1／DCC241016C03PROD；Config提交、A审核，页面COMPLETED；申请29、产品目录633、关系26 |
| 类型／类别 | 自有五级类型与DCC241016C03CAT类别；APPLICATION_CUSTOM，作者上传页唯一匹配可见 |
| 测试文件 | fixtures/中任务源件PDF及内容不同的附件A/B均已准备，SHA256见manifest；未上传 |

项目创建须使用正式`dcc_project_reviewer`职责，本例A复用该角色绑定；未修改其共享角色定义/菜单。配置角色只补了本例目录查询和产品目录入口权限，未修改admin原项目指派或添加全量范围。

`fixture-manifest.json`为verified=false：当前流程登记校验未通过，后续审签岗位、电子签名授权/图片仍须通过正式页面准备；未宣称完整夹具可用。完整对象及残留见manifest，业务身份和角色行证据见`resume-artifacts/account-rows.json`。

## 新阻塞：正式流程摘要校验失败

- Given 新运行态、真实作者U、已完成项目、文件夹、对应类型/类别、主管关系以及本次批准人A/文控D。
- When 上传页选择本例会签部门和审批人，由页面自然触发POST `/admin-api/dcc/controlled-files/route-preview`。
- Then 应展示可执行的准备结果或明确业务缺口；实际页面为“系统异常”“审批人链路：不完整”，HTTP200。网络仅记录path/method/status，未用响应JSON作断言。
- 2026-10-16 21:14:38.115运行日志只读诊断：`IllegalStateException: EXECUTION_CONTRACT_UNREGISTERED_DEFINITION`；调用链`DccWorkflowDefinitionCatalog.require:23` → `DccApplicationExecutionContractService.verify:348` → `preparePublicPreview:303` → `DccControlledFileWorkflowServiceImpl.previewCustomRoute:434`。
- 摘录：`resume-artifacts/runtime-error-excerpt.txt`；核心页面证据：`new-runtime-unregistered-definition-blocked.png`、`upload-route-readiness-blocked.trace.zip`、`response-172.json`、`response-173.json`。
- 源码校验表明活动部署未命中正式合同/流程key/摘要登记。本例未读取或猜测部署摘要，未修改共享模型/目录或放宽校验。GREEN未执行，需Root统一核对部署和正式登记一致性后继续。

## 本轮各子用例

| 子用例 | 结果 | 停止位置 |
| --- | --- | --- |
| U1 新建源件＋双附件就绪、保存、确认及提交查看 | BLOCKED | 上传前审批准备失败 |
| U2 源件／各附件上传中拦截 | BLOCKED | 未开始上传，不能造延迟状态 |
| U3 源件／各附件失败定位、拦截和恢复 | BLOCKED | 未观察真实失败触发，不使用mock/路由拦截 |
| U4 真实保存失败与恢复重试 | BLOCKED | 无已确认触发器，审批准备错误不能冒充保存失败通过 |
| R1 升版检入源件＋双附件、确认及提交 | BLOCKED | 本例尚无ACTIVE版本，依赖初始链成功 |
| R2 新旧版本附件查看／下载及隔离 | BLOCKED | 没有本例新旧版本，不复用其它线程对象替代 |

`resume-results.json`另逐项记录上传/升版×源件/附件A/附件B×上传中/上传失败/保存失败参数BLOCKED。受控文件上传、保存/提交、检出/检入、approve-task写请求均为0；项目配置与项目创建写入已在夹具清单独立列出，不能概称全部无写入。

## 本轮证据与清理

- 真实页面驱动`ui-live.cjs`由`ui-command.cjs`执行；完整步骤和逐命令结果在resume-artifacts/，日志未保存响应正文。主脚本保留businessError、evidenceErrors、cleanupError/cleanupErrors，缺失截图/trace不记完整验收。
- node语法、3份JSON结构、证据审计通过；`evidence-audit.json`凭据扫描0命中。trace在登录/首次改密后启动，snapshots=false、trace.network为0字节；截图前确认无可见密码输入。
- 凭据仅在内存和当前Windows用户DPAPI保护临时配置中，*.dpapi已在本例目录忽略，不提交/共享。
- browser-cleanup.json记录浏览器清理attempted/completed=true、cleanupError=null。夹具保留供Root review和复跑，业务数据清理未尝试、未完成；残留精确列在manifest，不假称清理完成。
- 未提交推送、未改生产或共享矩阵/菜单定义、未启停服务、未构建或迁移。旧证据保留在live-artifacts/及verification-report-before-resume.md。

## 以下为旧准备阶段历史记录（不代表当前授权或运行状态）

原阶段：PREPARING，未执行真实业务E2E；已被上述本轮真实结果取代。

## Preparation

- 分支：`int_qms`；目标运行 profile：`int_qms`，前端 `8061`，后端 `48061`。
- 当前 dispatch：本例 `gate=prepare_only`，环境 `runtimeVerified=false`、`roleFixtureVerified=false`；脚本在浏览器启动前严格退出 `PREPARED_WAITING`，实际退出码 `20`。
- 脚本：[e2e-03.cjs](/C:/IntRuoyiAll-int_main/doc/tasks/20261016-dcc-e2e-03/e2e-03.cjs)。命令：`node doc/tasks/20261016-dcc-e2e-03/e2e-03.cjs`，工作目录仓库根目录。静态语法命令 `node --check ...` exit `0`。
- Playwright 从 `IntRuoyiFronted/node_modules/playwright` 加载，版本 `1.60.0`；缓存 Chromium 可执行文件已静态确认存在。未启动浏览器，未生成 trace/截图/业务数据。

## Subcase Contract

| 子用例 | 页面动作与断言 | 当前状态 |
| --- | --- | --- |
| U1 新建上传就绪 | 真实页面选择源件、附件A、附件B；保存草稿；详情附件区可见两项；初始申请确认框列出源件和两项附件；取消后再次确认 | 待 execute |
| U2 上传中拦截 | 真实附件上传未完成时读取状态提示和提交按钮 disabled | 待 execute |
| U3 上传失败拦截 | 真实失败附件页面提示明确对象/原因；未移除失败项不能提交 | 待 execute；需任务自有失败文件/真实失败条件 |
| U4 保存失败恢复 | 页面可触达的真实保存失败后记录提示、无成功申请，恢复后重试 | BLOCKED 前置：尚未确认可安全、可重复触发条件 |
| R1 升版源件与附件 | 浏览页检出/检入变更源件和两个附件；WORKING 小版本同主档；升版确认框清单完整 | 待 execute |
| R2 版本隔离 | 受控浏览选择旧版/新版并分别进入追溯详情，旧版仅见旧附件，新版仅见新附件 | 待 execute；缺少版本/附件标识则 BLOCKED |

## Preconditions / Blockers

- Root 尚未核对 `48061` 运行 Jar 来源、健康状态及当前源码/应用 schema；用户已说明前端 `8061` 未监听、`48061` 为旧外部 Jar，本例因此不启动浏览器。
- Root dispatch 尚未确认角色夹具。申请人需要真实 `dcc:controlled-file:submit`/`query` 页面权限；升版操作人需要能对任务自有 ACTIVE 版本检出/检入。缺少独立账号时不得用 admin 冒充。
- 必须提供同一已确认租户内的任务自有项目、叶子文件夹、正式文件类型 taxonomy/category、有效审批路线和申请人组织关系；缺数据时不能通过 API/DB 补齐。
- 必须提供三个内容可区分的源件/附件文件，以及升版变更源件和两个附件；脚本通过环境变量读取路径，不写入密码或凭据。
- U4 的“保存失败”没有在当前源码/页面入口中确认可重复、可恢复的真实触发器；不能用 route interception、mock 响应或篡改请求制造失败。该项保持 BLOCKED，待Root确认真实条件。

## Evidence Boundary

- 真实业务动作全部由 Playwright 页面完成；脚本只监听自然请求 method/path/status，绝不调用 API、SQL、HTTP 客户端、mock、localStorage 或组件方法。
- 只有页面可见 DOM、输入值、按钮 disabled、确认框文本、导航和详情附件清单可作为通过依据；本轮准备阶段没有提交受理证据。
- trace/截图仅在 Root 将 gate 改为 `execute` 且前置通过后生成；认证页面不进入 trace，脚本不打印密码/token/cookie。
- 每个真实 case 的 result 分离 `businessError`、`evidenceErrors`、`cleanupError`/`cleanupErrors`；确认框隐藏等待、截图、trace.stop、context.close 和 browser.close 不再静默吞异常。截图或 trace 缺失不能记为完整验收，业务失败/前置 BLOCKED 原因保留不被 cleanup 错误覆盖。
- `browser.close` 若失败会写入任务目录 `cleanup-errors.json`；若该证据写入失败则显式输出 `evidenceError` 并以非零退出，不伪造清理完成。

## Root Review Checklist

- [ ] dispatch 改为 `execute`，`runtimeVerified`/`roleFixtureVerified` 均为 true，并核对端口与运行 Jar。
- [ ] 真实账号、租户、角色、项目、文件夹、类型、路线和任务自有文件已确认，环境变量仅在运行时注入。
- [ ] 每个子用例独立输出 PASS/FAIL/BLOCKED、截图/trace、页面提示、写请求统计和 cleanupAttempted/cleanupCompleted/residual。
- [ ] 旧版/新版附件详情分别核对，确认无串版；U4 若无正式触发条件保持 BLOCKED。

## R5工程收尾准确边界

ready_for_closeout后task_closeout.py preview实际exit0，delete候选1026；因明确禁Git且必须保留复核证据未apply，最终状态blocked，业务PASS。三zip CRC及trace条目检查PASS，scoped git diff --check exit0。三任务cjs被忽略，均在Cleanup Keep；未暂存。

- 2026-10-20本轮收尾最终补记：ready_for_closeout后preview exit0；keep 443 / delete 975（历史）/ warnings 0，本轮删除候选0，未apply。因当轮禁Git，最终工程状态blocked，E2E六条仍PASS。


## 2026-10-20 03/23共享浮层最小修复

真实根因为body Teleport加固定2100；原A2预览2055，2个底层控件真实截获点击。新增布局测试严格RED→GREEN，组件按所属overlay层级/普通页1定位，observer PASS、附件8/8 PASS；原U、原A2-RA/A1-A/B定向真实页面复验PASS，关闭后字段面板可打开。旧全局列配置合同在未改动的detail/index.vue结构断言失败，未扩大修复。4份新trace CRC/敏感字面量审计PASS，全部runner/browser已关闭；失败raw原样保留，23尚待本人PDF复验。详细证据/命令/复验入口见repair-overlay-20261020-handoff.md。按Root只定向验收，不重复审批/全量业务，不构建/重启/提交/cleanup apply。

收尾preview exit0：keep=466, delete=976, warnings=0；新修复证据删除候选0。当前blocked仅工程收口/独立23复验边界，不覆盖03业务PASS；未执行Git/apply。首次生成closure账因Python默认GBK解码失败，补显式UTF-8后4个runner退出码/browserClosed/context0均核对，未改raw结果。


## 新四轮R2第一批E2E03 PREPARE_ONLY

runId=rerun-20261020-r2-round1，独立目录/入口已准备，前缀DCC261020C03R2R1。原U/S/L/C/A/D及CurrentUser DPAPI来源核对，仅文件存在，不宣称本轮已登录。六分支全NOT_RUN；gate默认PREPARE_ONLY，需Root新包READY精确SHA与单独放行，旧dispatch和旧结果不得代替。入口及9 helper语法/禁用动作扫描PASS，--prepare exit0，关闭gate真实执行入口exit20且browserStarted=false/businessActions=0；默认凭据静态合同exit0。无浏览器/业务/生产改动/Maven/重启/Git/子Agent。完整顺序及待前置见本轮handoff.md；准备后hold，不写Root共享汇总。


## 新四轮R2第一轮执行结果（2026-10-20）

runId=rerun-20261020-r2-round1，业务六分支PASS；初始4333/A1，工作4334，正式4336/A2（均为205454566804408前缀完整ID见handoff）。新旧BPM、附件身份与6次在线正文校验均保存。本轮新包709e78bd...aaa147，六冻结前端文件最终SHA一致。首个异常为初始D签名45s响应未观测；保留原PENDING与超时，fresh U和fresh D同原任务确认#1144 VALID及ACTIVE后继续原链，未重签。A2 D正常HTTP200。保存失败分别标明初始save preflight和检入checkin真实请求，未称数据库失败。校准错误另记calibration.json。

16 traces CRC/凭据扫描PASS，关键预览截图人工视觉核对PASS，browserClosed=true/context0、PTY23241 exit0。无生产/构建/重启/Git/API/DB/mock/子Agent操作；Root汇总未触碰。详见本任务rerun-20261020-r2-round1/handoff.json/md、first-anomaly.json、evidence-audit.json、final-source-proof.json。仅当前轮结束后hold，工程提交门禁未授权，不标completed。

本轮最终收尾：ready_for_closeout 后 cleanup preview exit0，新run删除候选0、无warnings；保留全部原始证据未apply。11脚本语法/scoped diff check PASS，全部新文件凭据字面量0，16trace CRC/敏感字段0，JSON结构通过。随后工程状态按禁Git规则记录blocked；本轮已hold，业务六项PASS，首个等待异常仍OPEN。


## R3第一轮本轮新结果

4 PASS / 2 BLOCKED。初始4387完整审批ACTIVE；工作4391/正式4392升版S唯一签名30006ms ERR_ABORTED，HTTP无响应、页面30秒timeout；只读初始化GET亦30秒中止、终态UNKNOWN，未重签。已按Root停止并关闭context/browser/runner。原始错误和限制独立保留，无旧结果复用。唯一正式本轮交接：rerun-20261020-r3-round1/handoff.json；工程禁Git/保留证据，不标completed。

## 2026-10-21 原R3缺项补齐
见continuation-20261021/verification-report.md及handoff.json：六组累计业务PASS，原签名与版本状态、四附件及两PDF源件均已真实页面核验；工程Git/cleanup未完成，不标completed。
