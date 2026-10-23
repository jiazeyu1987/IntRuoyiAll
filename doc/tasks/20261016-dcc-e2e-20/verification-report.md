# E2E-20 日常入口与正文权限

## 当前有效验收（2026-10-19 R2整理）

业务PASS：原27参数27 PASS、0 FAIL、0 BLOCKED。Root于2026-10-19 05:36接受，10:20/10:40保留结论，见doc/tasks/20261016-dcc-24-e2e-coordination/verification-report.md对应检查点。工程状态blocked：用户禁止Git提交/推送，未执行cleanup apply，不标completed。

| 范围 | 有效证据与结论 |
| --- | --- |
| ACTIVE/待生效、两账号四入口及撤权三项 | 原run2026-10-18T15-03-51-377Z累计19 PASS；Reader VIEW及原备注已恢复，恢复后A1非空正文已核 |
| WORKING八项 | 后续run2026-10-18T20-20-07-652Z，真实检入新工作稿2054545668044084251后，Reader/Denied各四入口拒绝PASS；详情入口不可用时按真实页面无入口记录，不冒称实际打开正文 |
| 同链与正文正控 | A1=2054545668044084231 ACTIVE、A2=2054545668044084245待生效至2026-10-25 12:00:00、WORKING=2054545668044084251；检入后A1仍非空canvas686×970/采样340且PDF搜索命中ACTIVE BASELINE |
| P06历史测试 | Root实际XML：e2e1624-red-and-history-candidate-DccWorkingSubmissionConditionTest.xml中唯一新增方法expected1/actual0，1 failure/0 error；后续e2e20-complete-binding-green-DccWorkingSubmissionConditionTest.xml两项0 failure/0 error，替代“待Root执行RED” |
| 检入链后续修复 | Root e2e20-complete-checkin-regression-green-process.json exit0及e2e20-complete-checkin-green-summary.json的112项PASS；保留此前source-preview/attachment失败，不从文件名green推定命令成功 |

最终逐项结论为parameter-results.json；汇总为business-checkpoint.json#integratedOriginal27Final及artifacts/acceptance-resume/2026-10-18T20-20-07-652Z/original-27-permission-parameters-final.json。19项保留实际包264efbd4…；后八项包bb6d2c94002289e595a0e4de27d4ba69dd8c82e5c622625317591e424ce318be，不追改成当前包。本轮未验证13/22/24新修复，也不使用旧workflow-v6宣称新修复通过。

最终run目录保留Manager-5、Reader-5、Reader-6、Denied-5四个trace与逐项截图，trace-redaction-report.json四项均确认凭据缺席、认证头及敏感字段脱敏。checkpoint.json记录最终run为STOPPED、runnerPid=null。浏览器/权限恢复沿用原证据，本轮没有新进程盘点；旧browser-closed.json、runtime-pause.json、runner-handle.json只是阶段快照，不能证明当前运行环境或要求重放业务。

残留：原账号/角色、项目283/目录/类别/签名配置、原A1/A2/历史草稿及新工作稿4251均保留供审查；未删除资产。早期错误文件名上传的临时件未获完整清理回执，继续保留“清理未确认”边界，不能写全量零残留。Reader VIEW恢复和上述业务PASS不等于全部工程清理完成。

本轮范围仅文档：BDD见task.md；RED为报告首段NOT_RUN、manifest=false与最终27 PASS冲突（实际exit1）；GREEN为文档结构、计数/唯一性、证据路径、Root测试及trace脱敏记录的离线检查。未运行Playwright/Maven/类型检查、未改生产/测试代码、未操作服务或数据。

## 2026-10-19 R3 仅错误名称上传残留核查

核查完成；清理状态仍UNCONFIRMED，不能断言当前仍存在或已经清理。原27项业务PASS保持，未执行新页面动作或删除。

- 精确历史场景：run2026-10-18T07-45-23-331Z，Manager/910350，项目283、master2054545668044079203、基线A1/2054545668044084231；ui-actions步骤44选择DCC241016C20-C.pdf，步骤46检入后弹框仍在，步骤47保存checkin-C-result-dom.txt显示“已上传：DCC241016C20-C.pdf”。步骤49另页全部版本仅A1；这只能佐证未见新正式版本，不能证明临时件不存在。
- 本地输入身份：artifacts/acceptance-resume/2026-10-18T03-06-47-233Z/DCC241016C20-C.pdf，34375 bytes，SHA256 7500A0700159BCB84E69B706EBD44F7E21FF0EB36A3B66640DF977B146766F40。步骤52将同一内容复制成inputs/C/DCC241016C20-A.pdf并真实检入4241，两本地文件哈希和长度相同。因此源内容哈希也不能单独当远端删除条件。
- 原trace可补充定位：Manager-52-trace.zip的call@664为该C.pdf的setInputFiles，call@672/@682/@712保留错误名称上传DOM。原弹框有“取消”和“按Delete键可删除”；步骤51之后会话退出，步骤52关闭旧context并重新登录，未保留对该C临时件完成移除/取消的回执。
- 排除误用：03-06-47那次runtime-pause-cleanup.json清理的是更早A.pdf上传；步骤56取消属于后续B输入流程且实际ERROR，不是C清理成功。当前run自然请求记录中没有session-cleanup；三份Manager trace.network均仅1 byte，Manager-52资源为1084张jpeg，无可回读远端临时fileId/上传session映射的网络正文。trace上下文ID是浏览器身份，不能当服务端上传session。
- 缺口/下一步：远端临时源件ID、关联预览件ID、上传会话归属/时间与有效状态均未知；历史证据未提供跨会话只读定位这次未检入临时件的入口及回执，不等于证明产品全局没有入口。若后续获得正式只读临时件清单/审计入口，先关联本人/项目/基线/原会话及远端ID，再由Root安排清理；不得按名称、内容哈希、浏览器context或HTTP200删除/判清理。本轮已能确认缺口，无需打开当前页面或申请运行态窗口。

R3仅新增本节、任务BDD/日志和repair-20261019-r3-handoff.json。结构验证确认引用、身份字段与本地哈希一致，原parameter-results.json字节哈希保持。无生产/测试代码修改、Maven/类型检查、服务操作、API/SQL或业务重跑。

## 2026-10-19 R4 无新增来源，保留UNKNOWN

R4派发与最新R3交接未提供该次C.pdf上传的新增远端临时件ID、正式只读映射或对应清理回执。本轮不重复既有历史检索，沿用R3已列明的具体缺口：远端身份、当前有效状态和清理结果均未知；不判残留存在，也不判已清理。

原27 PASS及parameter-results.json保持，未产生新页面证据、新包PASS或测试XML。仅验证本轮文档结构、交接引用及原参数文件哈希；Maven、类型检查、Playwright、服务操作、业务写入和cleanup apply均NOT_RUN。实际命令与退出码见repair-20261019-r4-handoff.json，工程状态继续blocked。

## 历史报告（以下阶段均由上述最终证据替代）

2026-10-16上传候选GREEN仅解除前置；2026-10-18先5 PASS/22 BLOCKED，再19 PASS/8 BLOCKED；最终工作稿4251补齐后于2026-10-19获Root接受27 PASS。下列旧“最新”“尚未执行”“仍需”等词均仅指对应历史时点，原文保留，不作为当前状态或恢复指令。

上传候选前置GREEN PASS，原缺列阻塞已解除；E2E20正文权限业务尚未执行，不代表整例通过。原RED及当时的BLOCKED事实保留。

## 2026-10-18 继续原流程及安全暂停

本轮新增自有类型160（DCC241016C20Type）、类别908767（APPLICATION_CUSTOM）、绑定目录DCC241016C20Storage、Reviewer业务角色991337，并通过真实页面补岗位和本人签名授权/图片。项目283及既有4账号复用，没有重建。

E20-P01实际复现：4节点路线已就绪，但源件上传400，页面误报“Current user is not the DCC project owner”。本例类别权限页实际0规则；直接源码的类别UPLOAD守卫复用OWNER错误，与Root对19的栈核对一致。20只给自己的类别908767补Manager USER/UPLOAD，关闭重开读回1条；未改项目OWNER、Reader/Denied阅读边界或生产源码。

同项目、同类别、同DCC241016C20-A.pdf真实复验为upload-preview200，页面预览可见、文件名正确且无预览错误，路线4节点就绪。此为准备前置GREEN，不是E2E20正文权限PASS，也不替代Root对错误文案生产修复的验收。

Root通知48061安全更新窗口后立即停止后续写入，没有提交正式文件或申请。临时源件通过真实“移除”清理：DOM提示“已清理本次上传临时文件”，源件行/预览消失，自然session-cleanup POST200。正式file/申请/BPM/task均未创建；精确接续点见pause-checkpoint.json，runtime-pause.active=true。恢复后从已有资产读回继续，27正文分支仍待执行。

证据位于artifacts/acceptance-resume/2026-10-18T03-06-47-233Z/，含E20-P01原失败截图、类别规则前后/重开DOM、同文件复验JSON、runtime-pause-cleanup.json及本轮trace。账号密码未输出、未猜测或重置；未解析响应JSON作通过依据。

## 历史：2026-10-16 上传候选GREEN真实结论

PASS：runId=2026-10-16T11-43-48-641Z，本例Manager在独立context登录，选中项目283及DCC241016C20Folder。页面文件类型错误消失，DOM显示70个候选、其中24个可选；页面自然GET upload-options为HTTP200且业务码0。候选名称/禁用状态采自可见DOM，不由响应data替代。

新runtime PID43284/48061，Jar SHA256 caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff。命令为 `node doc/tasks/20261016-dcc-e2e-20/regress-upload-options-green.mjs --run-after-root-restored`，执行时用环境变量指定该SHA。sourceUploads=0、controlledFileBusinessWrites=0，未用上传/提交替代前置复验。

证据目录：artifacts/green-upload-options/2026-10-16T11-43-48-641Z/，包括PASS result.json、visible-file-type-candidates.png、green-trace.zip及通过的trace-redaction-report.json；汇总标记为upload-options-green-evidence.json。

中断后先盘点原session/进程：原session已不存在、本例Node及浏览器均已关闭。PASS结果和证据在中断后完整落盘；未重复登录或执行。原exec退出码未回传，observedExitCode=null，不能宣称捕获到exit0。该事实与可见DOM/自然业务码/trace证据分开记录于green-review-summary.json。

原RED保留，fixtureVerified仍false。参数结果更新为NOT_RUN_FIXTURE_PENDING：旧上传候选阻塞已解除，下一步仍须准备并核验自身ACTIVE/待生效/DRAFT同链和不同正文权限。此处没有27项业务PASS。

## 原RED真实执行及环境（历史保留）

- 分支int_qms；前端8061、后端48061；使用Root已核验Jar SHA256 `b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071`。
- 已按最新execution-authorization自行推进；dispatch=execute、accountSetupGate=authorized、fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST；没有修改共享dispatch。
- `node doc/tasks/20261016-dcc-e2e-20/reproduce-upload-options-blocker.mjs`：真实Playwright，独立登录Manager，选择本例项目与文件夹，可见错误“系统异常”，实际退出码2。
- 主脚本本例manifest门禁：退出码2，原因是本例版本fixture尚未完整核验；静态node --check通过。静态通过不代表业务通过。

## 已完成的任务自有准备

|对象|页面核验事实|
|---|---|
|Reader 910348 / Denied 910349 / Manager 910350|账号与昵称均带DCC241016C20前缀，各自独立context完成真实登录/首次改密|
|Reviewer 910354|同前缀独立账号，复用精确业务角色dcc_project_reviewer；未改共享角色定义|
|三条新角色991237 / 991238 / 991239|同前缀Reader/Denied/Manager，真实页面勾选菜单和绑定用户；未授管理员|
|项目283 / 产品DCC241016C20Product|Manager真实提交、Reviewer页面审核，申请终态COMPLETED，项目正式生成|
|模板5 / 项目文件夹|DCC241016C20Template / DCC241016C20Folder，保存后页面回读|
|组织|DCC241016C20Dept；本例Reviewer作为负责人及Manager直属主管|
|项目正式权限|Manager OWNER、Reader VIEW、Denied VIEW真实保存回读；正文差异尚未验证，不能当权限负例已就绪|

所有对象清单与状态见fixture-manifest.json。`fixtureVerified=false`，ACTIVE/待生效/DRAFT均未创建；未上传源件、未提交受控文件、未执行签名审批或正文撤权。

## 真实阻断与BDD

Given 本例Manager拥有正式文件上传菜单/提交权限、本例项目283 OWNER、有效项目属性及项目文件夹。

When 通过真实前端进入 `/dcc/controlled-file/upload` 并选择本例项目/文件夹，刷新后再核对“文件类型”。

Then 实际显示“系统异常”，没有可选择类型，无法建立本例版本链。自然GET `/admin-api/dcc/file-type-taxonomies/upload-options` 为HTTP200；未解析业务响应JSON，不能据HTTP200声称成功，也不能仅凭页面泛化错误归因具体后端异常。

- RED：独立真实复跑exit2，截图可见“文件类型”错误。
- 原RED阶段当时未执行GREEN；现在上传候选前置已由上述独立真实GREEN复验闭环。生产修复、迁移及共享矩阵未由本例执行。
- 版本正文、目录/搜索/详情/地址权限和旧页撤权后行为此前被该前置阻塞；最新候选前置已解除，后续业务仍需自身fixture并逐项实际验证。不把原前置失败算作业务权限FAIL。

## 参数结论

parameter-results.json按4入口 × 3版本状态 × 2阅读权限展开24分支，并单列旧页重读、刷新、新开3个撤权分支。27项均未开始被验收业务动作；最新为NOT_RUN_FIXTURE_PENDING，原BLOCKED历史另存。原7组准备清单仅为分组，不代表7组已经通过；主脚本业务部分仍须在可用fixture上校准完整参数执行，不能由局部分支通过宣称整例PASS。

Root既有review已修正：正文错误不再catch吞掉，增加实际PDF非空canvas检查；成功与预期拒绝分别断言。撤权改为仅切换本例项目精确Reader VIEW规则，避免误改任意首条目录授权。历史缓存/本地缩放不作为新授权证据，旧页后续正文重试须单独执行。

## 证据、凭据与残留

- 独立复现：`artifacts/repro-upload-options/result.json`、`blocker.png`、`repro-trace.zip`。
- 准备证据：`artifacts/real-ui-fixture/`中四账号身份页面、角色分配、模板、项目COMPLETED、项目权限和上传错误截图；3份准备trace及自然请求method/path/status记录。
- trace均在登录成功后启用，凭据输入时关闭；证据经过sanitize-traces.py脱敏，redaction report确认已知凭据不存在且认证头/敏感字段已脱敏。任务凭据为本Windows用户DPAPI密文，credentials.dpapi被.gitignore排除。
- cleanupAttempted=false、cleanupCompleted=false：保留4账号、3新角色、项目283/产品、模板5/独立目录、部门/直属主管和本例项目规则供修复后复跑。浏览器已关闭；无上传文件/受控版本或已执行但未恢复的撤权残留。
- 无Git提交推送、服务操作、生产修改、共享矩阵修改、API/SQL/mock或子Agent。工程收口及后续产品修复由Root review。

## 2026-10-18 更新窗口checkpoint
- 业务静止：全部context/browser关闭，runner正常exit0、trace脱敏完成；runtime-pause.json active=true，进程归属Node23828→Chrome21056，检查无残留。
- 准备完成：同链A/1 ACTIVE、C4241/B4242 WORKING；批准/文控签名及最终ACTIVE通过真实UI回读。尚未形成CONTROLLED_PENDING_EFFECTIVE。
- 未通过声明：27项正文权限分支仍未执行；E20-P03/P04待Root恢复后原对象复核，不把准备过程记E2E PASS。


## 2026-10-18 新包264efbd4真实验收
- 5 PASS：Reader的A1目录/搜索/详情/地址，待生效A2搜索浏览禁用；实际canvas686x970，分布非白采样340，PDF UI搜索ACTIVE BASELINE命中本例原件文本。截图、trace、自然请求见artifacts/acceptance-resume/2026-10-18T13-18-44-749Z/及parameter-results.json。
- 22 BLOCKED：8草稿参数缺WORKING（原4241/4242已替代、A2检出500），其它14参数因fresh Reader真实租户识别自然GET500未执行。撤权三分支未执行，Reader VIEW未撤销，因此无待恢复权限。
- 正式终态：A1 ACTIVE，A2 CONTROLLED_PENDING_EFFECTIVE，延迟生效2026-10-25 12:00:00、原编制人签名947。并未生效A2或作废A1。
- P04在稳定加载的原4242未复现，已补精确DOM/checked/确认/持久回读证据及组件竞态RED设计，不把猜测定根因。P05根因因后续全局500未分离，不改生产。
- 全部本例浏览器关闭，runner exit0/trace脱敏，进程无残留；runtime-pause.json active=true。任务blocked，未执行cleanup apply/Git提交推送，不标completed。


## Root22:54环境补充
- Root确认34440已退出、48061连接拒绝，之前登录/业务500归于后端不可达，不能标为账号权限FAIL或已证实生产检出缺陷。
- 本例已静止并再查无残留进程，等待同版恢复和真实登录放行；已成功业务不重建，不确定检出先UI读锁，不重复写。


## 历史：2026-10-18 验收与RED交接19PASS/8BLOCKED（被27PASS替代）
- 同200656/264efbd4恢复PID14864后实际补齐14分支；27项累计19PASS、8个WORKING参数BLOCKED、0FAIL，详见parameter-results.json。原ACTIVE/待生效、Denied四入口和真实撤权三分支均已分别记录。
- Reader VIEW和原备注已恢复、回读，并重新打开A1实际PDF非空，canvas686x970/非白采样340。所有context/browser已关闭，runner exit0/trace脱敏，未留撤权残留。
- P06 Root已确认生产SQL漏待生效状态；只获准追加真实MyBatis/私有H2 RED，原并发测试与Mapper/Query保持原文/哈希。expected1 actual0是预期RED，不是已执行结论；Root待独占Maven实际运行。
- 状态继续blocked，不拿历史SUPERSEDED草稿替代WORKING，不改Mapper/Query，不跑Maven，不提交推送。
