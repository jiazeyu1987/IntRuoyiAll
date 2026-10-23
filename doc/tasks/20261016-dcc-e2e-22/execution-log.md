## 2026-10-20 最新：22-A / 22-B / 22-C 原分支真实 UI PASS

本轮范围替代先前“只读核对原L待办”的已结束范围。按最新委派复用原 Main A/1、A/2、Rel1 A/1/B/1、Rel2 A/1，以及既有四条引用；不操作审批、权限、检出、其它用例或Git。当前 int_qms，8061 PID20976 / 48061 PID22076；R6 Jar文件SHA256、后端进程Jar路径与协调r6-runtime-proof一致。Reader本人真实登录，凭据从原DPAPI读取。

| 原分支 | 本轮结论与真实页面依据 |
| --- | --- |
| 22-A 版本详情 | PASS：A/2→A/1→A/2，逐版核对名称/编号/状态、提交人U与负责人Reader、项目/文件夹/类别、提交备注/变更说明、四项属性、BPM/流程类型、提交/受控/实际生效/启用/下发时间及升版来源/选用草稿。A/1 NMPA/Y/N/N，A/2 CE/N/Y/N；返回A/2仍精确一致。两版均“文控标准化签名后生效”，作者预设/延迟日期按该模式不适用（未造日期或另验其它生效模式）。共同字段按原记录一致验证，未伪造每个字段都有差异。检出隔离继承R5 PASS，不重跑。 |
| 22-B 固定关联/阶段 | PASS：两个Main版本的Rel1仍固定A/1，真实浏览另证明Rel1当前B/1已生效；每次固定正文旧标识匹配、新版标识不存在。Main A/1独有后补Rel2 A/1及正文；A/2无Rel2且独有负责人910510/期限2026-10-22 12:00:00整改。原上传/后补来源、精确文件/BPM、原义务部门/签名人和阶段证据不混版。 |
| 22-C 引用去重/明细 | PASS：A/1显示2项目/3目录，逐行精确核对Target2-F1、Target2-F2、Target3-F1、文件名/编号/A1/已作废，并全部打开固定A1正文验证旧标识存在、新标识不存在；A/2只有Target3-F2一项目一目录，独立A2正文新标识存在、旧标识不存在。行计数/集合与项目去重同时断言。 |

执行：原fixture-ui.cjs启动Reader，委托原business-steps及本例acceptance-20261020.cjs完成7项参数断言（两版各三项及返回A2字段）。session76601、runner16704，finish exit0；context/browser正常关闭，OS核对runner/直接子进程/Chrome均不存在。无新增临时业务夹具、无业务变更，既有样本全部保留；页面自然产生登录及受控阅读start/heartbeat/stop遥测POST，读取遥测均HTTP200，不能将这些写成“零POST”。无需业务夹具清理，未清理证据。

fixture-1792430618420保留两版详情、3次关联固定正文、4条引用正文、关系/整改及引用计数截图（15条记录，14个唯一路径），脱敏trace-1.zip完整性PASS且无.network归档。成功trace导出1张直接截图动作帧并附callId/时间/SHA索引；未宣称逐操作全量导出。关系整改和A1三引用截图已目视核对；详情与正文均有真实DOM断言。

三次脚本校准错误原样保留：弹窗本身无版本号（改为来源行固定版本+正文标识验证）、popup初始URL为空（等待精确目标路由）、返回详情文件夹元数据“尚未读取”（等待精确原文件夹）。均未改变生产代码或绕过业务，修正后沿原页面重新执行通过。本轮无可复现产品缺陷，无生产修复。Node语法检查两个本例脚本exit0；没有全量测试或其它用例执行。

结构化证据：acceptance-20261020-result.json。R5原证据repair-20261019-r5-handoff.json、r5-checkout-ui-result.json保留原状。工程状态blocked：待Root review，本轮明确禁止Git/证据清理，未标completed。

---

## 2026-10-20 只读恢复核对，无新增 E2E 结果

- 按最新委派收缩为只读核对原 E2E-22 是否仍有可操作且未通过的需求；不重跑检出/撤销或其它业务写入。
- 实际分支 int_qms；48061 PID22076、R6 Jar SHA256 478fff77d0b6d839c0a3466557df5232b9b12a60c35f00908c164cfaf949f24a 与 r6-runtime-proof.json 一致。旧 dispatch/PID 和历史 R5 运行态保留为历史，不作为本轮运行态。
- 原 L（DCC241016C22L）真实页面登录，进入审批中心→待办。两次只读 DOM 回读和可见文本断言均为“暂无审批任务”，原 3481664b... 不具备继续指派前提。个人工作台的分发待签收不是原审批任务，不扩大操作。
- fixture-1792429895498：session81245，runner38756，finish exit0，context/browser 正常关闭；OS 检查 runner/直接子进程/Chrome 均无残留。自然请求只有登录 POST，其余均 GET，业务写入 0；未触发检出、撤销、签名、关联、权限或数据清理。
- dom-4.json、dom-7.json、两张截图及有效脱敏 trace-1.zip 均保留；trace ZIP 完整性通过且无 .network 条目，最终截图已目视核对。结论 READ_ONLY_NO_NEW_E2E_RESULT，不是新增 E2E PASS。
- R5 八步 PASS 仍以 repair-20261019-r5-handoff.json / r5-checkout-ui-result.json 为准。当前范围没有识别到仍可操作的未通过原需求；后续需要 Root/用户明确未通过业务项。工程状态 blocked（待 Root review，本轮禁止 Git）；未运行 cleanup、提交、广泛测试或服务操作。
- 结构化结果：resume-20261020-readonly-result.json；当前 manifest 与本轮 checkpoint 标明只读范围，保留原 R5 环境及旧 dispatch 为历史。原 e2e-result/runner-checkpoint 中的 R5 业务证据不改写为本轮结果。

---

# Execution Log

## 2026-10-19 23:30 R5限定八步真实UI PASS
- 命令：`$env:DCC_E2E_POWERSHELL=(Get-Process -Id $PID).Path; $env:DCC_E2E_22_START_USER='DCC241016C22U'; node doc/tasks/20261016-dcc-e2e-22/scripts/fixture-ui.cjs`。session71992，runner40896，2026-10-19T15:18:35Z至15:30:06Z，正常finish exit0。
- U独立登录真实页面，原A2行未检出；于23:24:30检出原4260/A2一次。A2 by/time/reason精确匹配本次U/时间/原因，原4249/A1历史三项均未检出，返回A2仍为本次事实。23:28真实UI撤销本轮新锁，行内检出按钮可用/本人锁标签消失；随后A2/A1只读记录三项均未检出。任务自有新锁清除已由页面回读证明，不只以toast判断。
- 自然业务写请求仅POST原4260 checkout和checkout/cancel各1次、均HTTP200；无权限、角色或调度变更，无API/SQL业务动作，无Maven/构建/服务/Git操作。Query277/前端49/GREEN5及既有页面/关联/引用不重跑；最新八步范围无新正文或完整属性验收结论。
- DOM、16张主动截图/局部断言截图、有效脱敏trace-1.zip保留在fixture-1792423115516；从成功trace提取八幅关键帧并记录callId、时间和SHA256于trace-frame-index.json。局部正负例/释放图与A1导出帧已目视核验。
- 两次临时只读定位器错误原样保留：项目浏览表误用存储列表.browser-list-wrap，定位超时；改用原脚本的精确项目行后继续，期间无业务请求、无生产改动、无重复写。
- OS核验runner40896不存在、直接子进程为空、Chrome为空。r5-checkout-ui-result.json与repair-20261019-r5-handoff.json、fixture-manifest/e2e-result/runner-checkpoint同步最新范围和实际新包身份。业务PASS，工程blocked等待Root review/禁止Git，未cleanup apply、未标completed。

## 2026-10-19 R5 Root 新包放行
- 按本轮明确指令读取 `doc/tasks/20261016-dcc-24-e2e-coordination/r5-runtime-proof.json`。Root证明int_qms PID19844运行Jar `backend-workflow-v6-20261019-230954.jar`，SHA256 `3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c`，59 classes逐字节核对、health UP、真实admin UI登录PASS、newFixesIncluded=true。
- 本机只读复核该Jar文件SHA与proof一致、PID19844命令行指向该Jar。旧dispatch仍指向前一PID/Jar，但Root本轮明确指定新proof并放行，故按新proof验收。
- fixture-manifest已有本例U账号DCC241016C22U、Author角色、真实页面组织核验和登录记录；不先变更权限，仅通过本人真实页面验证页面可见操作。若最低权限缺失，停止并按用户限定的本例账号最小授权/精确恢复范围处理。
- 只跑原A2检出、A1历史隔离、A2正例、取消本轮新锁及双版本清除回读；不重跑Query277、前端49、5项GREEN、R29/R30或关联/引用。真实DOM、截图、脱敏trace和自然页面写请求元数据将归档至本例运行目录。

## 2026-10-19 R5 新包门禁复核
- 读取 repair-dispatch-20261019-r5.md、根 AGENTS、task-closeout、E2E、登录、运行态、worktree 与端口规则，以及 R4 最新交接；当前分支为 int_qms。
- R5 dispatch 仍记录 int_qms 旧包 PID 25124、Jar SHA256 ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3、roleFixtureVerified=false；未发现 Root 新包/角色夹具放行。因此不启动 Playwright，不运行任何业务写操作。
- 有效后端 RED=4 tests/3 failures/0 errors、exit 1；Root GREEN=5/0/0、exit 0。Query 277/0/0、前端49项及既有页面证据均保留、不重跑；Query合并命令 exit 1 的两个外部 OfflineTraining fixture errors单独注明。
- R5八步E2E均未执行，无R5 DOM/截图/trace/写请求事实；没有创建新锁，旧4260锁已于12:42由UI撤销，不重放。详见 repair-20261019-r5-handoff.json。

- 2026-10-16：核对当前分支为 `int_qms`；读取 `docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 及线程约定。Root dispatch 中 E2E-22 为 `prepare_only`，环境 `runtimeVerified=false`、`roleFixtureVerified=false`。
- 2026-10-16：根据唯一业务来源拆为 22-A 版本详情字段隔离、22-B 关联固定版本隔离、22-C 引用项目去重与明细隔离。正式入口核对为可见“受控浏览/文件浏览”菜单 -> `/dcc/controlled-file/browser` -> 版本范围“全部版本” -> 行内“浏览”弹窗 -> “查看详情”进入追溯详情；详情中的文件信息与记录、固定关联、版本历史、引用使用明细分别作为断言区域。
- 2026-10-16：新增 `scripts/e2e-22-detail-reference-isolation.cjs`。脚本仅在浏览器启动前读取 `doc/tasks/20261016-dcc-24-e2e-coordination/dispatch.json`；gate 非 `execute` 时输出 `PREPARED_WAITING` 并明确 `browserStarted=false`。放行路径只使用 Playwright 页面、可见菜单、DOM、真实弹窗和页面导航；账号从 `DCC_E2E_TENANT`、`DCC_E2E_USERNAME`、`DCC_E2E_PASSWORD` 读取，夹具从 `DCC_E2E_22_FIXTURE_JSON` 读取，不落盘凭据。
- 2026-10-16：`node --check doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs` 退出码 `0`；禁止动作扫描（fetch/axios/APIRequest/apiGet/localStorage/sessionStorage/page.evaluate/route.fulfill 等）为 `PASS`；`node doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs` 在当前 gate 下退出码 `0`，输出 `PREPARED_WAITING`、`browserStarted=false`。未启动浏览器、未写业务数据、未产生 trace/截图。
- 2026-10-16：本机可见 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 与 Edge `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`；前端工程存在 `IntRuoyiFronted/node_modules/playwright`。真实运行仍需 Root 放行并核对 8061/48061 运行 Jar 与身份。
- 2026-10-16：修正登录脚本按可见文本输入与密码类型定位，避免“记住我”复选框导致索引偏移；最终 `node --check`、prepare-only gate run、prepared JSON parse 和禁止动作扫描均通过。业务结论仍为 `PREPARED_WAITING`，未启动浏览器。
- 2026-10-16 Root review返修：execute路径现强制核对 dispatch `entry.gate=execute`、`environment.runtimeVerified=true`、`environment.roleFixtureVerified=true`、`environment.backendPort=48061`；未知 gate 和未验证运行态/角色均输出 `BLOCKED`，不会启动浏览器。
- 2026-10-16 Root review返修：fixture 合同现要求至少 2 个版本、页面可见版本文本唯一且版本事实可区分；每版本必须提供状态、备注、编制人（独立于文件负责人）、项目、文件夹、类型、检出人/时间、生效方式、作者所选日期、实际生效时间、受控时间及至少 3 项实际属性。每版本必须提供固定关联、原提交/原上传、后补、整改记录、至少 3 条引用行，且引用至少来自 2 个项目；缺字段/缺数据分别 `BLOCKED`。
- 2026-10-16 Root review返修：放行路径逐个版本打开详情、申请/关联阶段证据、每个固定关联正文、每版本引用使用明细及每个引用固定正文；创建 `e2e-result.json`，执行路径启动 tracing 并保留 per-step screenshots/trace，stdout 不作为唯一证据。登录等待不再吞异常。
- 2026-10-16 Root review返修第二轮：脚本改为先按 `branch= int_qms`、`gate`、profile、8061/48061、runtimeVerified、roleFixtureVerified 进行门禁；夹具合同要求双版本、稳定 master、唯一页面版本文本、编制人与文件负责人分离、实际属性、全部时间、检出正反例、固定关联、原/补/整改证据、同项目多文件夹和第二项目引用。静态 gate run 退出码 `0`、结果 `PREPARED_WAITING`。
- 2026-10-16 Root review返修第二轮：execute 真实页面从唯一可见入口版本开始，逐版本通过历史记录行进入详情；每个版本独立核对字段/属性、申请阶段、固定关联正文、引用行分页及每条引用正文。PDF 必须非空 canvas 且搜索版本标记；TEXT 必须显示正文标记并排除另一版本标记。每个参数结果写入 result.json，执行 trace 经过 `sanitize-trace.py` 删除 network 资源和敏感字段后保留；脚本仍未在 prepare_only 启动浏览器。
- 2026-10-16：最终静态门禁：`node --check` 退出码 `0`；prepare-only run 退出码 `0`、`PREPARED_WAITING`、`browserStarted=false`；合同扫描和禁止动作扫描均 `PASS`；prepared/result JSON parse `PASS`；无 execute artifacts、无 Python cache。当前 dispatch `runtimeVerified=true`、`roleFixtureVerified=false`，仍等待角色夹具放行。
- 2026-10-18 11:42：旧runner PID19432已通过handle40466正常finish，trace/context/browser关闭后以stdin Ctrl+C释放残留进程；只保留当前PID37296/handle26253写本例manifest和业务。Reader账号910474已真实首次改密并独立登录受控浏览；Config角色991336已创建，正在配置最小菜单。缓存均为当前用户DPAPI，不含明文凭据。
- 2026-10-18：Cfg910488和Reader910474均已真实首次改密/独立登录；本例存储目录、独立类别、技术文档下四级分类分支及模板24（F1/F2）已创建。项目P1正式提交后页面PENDING_REVIEW；Reader仅追加正式项目审核员角色绑定以办理本例申请，未改角色定义。尚无文件/版本业务PASS。当前Jar a27b8d…d402，后端18204。
- 2026-10-18：P1由Reader本人真实页面审核通过，DOM两次核对终态COMPLETED，并留截图；P2/P3分别通过Cfg真实页面提交。P3已保存PENDING_REVIEW DOM；P2最后审核动作目标行未定位到，prompt等待超时，未取得写回执。脚本click promise未及时安装拒绝处理导致runner8700/handle7168退出码1；属于harness错误，未认定QMS业务FAIL。自己的脚本现改为Promise.all，并补异常trace/context/browser收尾，node --check退出码0；未真实复验，未记GREEN。
- 2026-10-18 15:21：按Root48061安全更新窗口停止新增业务写入。runtime-pause.json active=true；OS核对本例node及PID8700直接子进程均无残留，browserClosed/runnerExited=true。精确恢复信息写runner-checkpoint.json；没有受控文件ID/BPM/task，没有临时源件上传或重传。保留既有三个脱敏trace和DOM/P1截图，最后失败段trace因异常退出未保存。P2结果保持待只读确认，P1/P2/P3、账号、模板和目录不得重建。全部原业务参数仍未完成，未报告E2E PASS。
- 2026-10-18 恢复后：fresh Reader UI确认P3待审并审核到COMPLETED；Cfg申请页及Reader项目页均无P2，Root只读诊断同样仅P1/P3。本例Author/Review/Approve角色、Dept、Post32和U910510/S910511/L910512/C910513/A910514/D910515均已页面核对组织/职责，六人独立首次改密登录及本人TEST签名PNG上传启用完成，授权页六人已启用。当前9476/handle98263，下一步补一次P2、OWNERRole单个项目编辑能力和真实项目编制/查看规则。
- 2026-10-18 19:47：bf2fd496 runtime下P2已真实补建并独立审核COMPLETED；P1/P2/P3正式授权逐项保存回读，类别908769已回读9条本例规则。八个账号均可独立登录，六签名者授权/本人图片已启用。U通过真实上传页提交Rel1/A/1，源件DCC241016C22Rel1-A.pdf，现UI_SUBMITTED无重复提交；当前9476/handle98263，继续S本人待办。

## 2026-10-18T20:56:21.0368661+08:00 Root统一更新48061暂停
- 收到Root停止新业务写入指令后仅只读inspect：Rel1 A/1待直属主管审核，0/1完成；签名/前序节点暂无记录。S此前按钮超时发生在会话返回登录后；同一S真实重登并打开原待办，无重复提交、无签名动作。
- fileId=2054545668044084243；BPM=58b280de-cae9-11f1-a035-b082e25ec548；taskId=58b2a80b-cae9-11f1-a035-b082e25ec548。Rel2/Main未开始。
- finish正常退出码0；runner9476及其browser48444归属进程均不存在，context/browser关闭，20份脱敏trace保留。runtime-pause.json active=true；runner-checkpoint.json/e2e-result.json同步。
- 待Root明确新PID/hash/health/真实登录放行后新建S context，从原待办续跑。当前业务结论BLOCKED，不是PASS。

## 2026-10-18T21:19:56.5853990+08:00 Root明确恢复264efbd4
- Root证明48061 PID34440、8061 PID24564、health UP和官方真实登录PASS；自身暂停标记解除，新context使用原S本人登录，原Rel1对象续跑。
- 修正任务runner恢复状态时遗漏applications/objects的脚本问题：保留prior-manifest业务对象，新一轮仍重置actions/network/trace列表并归档原manifest。语法检查通过。生产代码未修改。

## 2026-10-18T22:21:38.5463939+08:00 Rel1/Rel2生效、Main前置查阅配置
- Rel1 #2054545668044084243 A/1已在真实受控浏览及加载完成详情确认ACTIVE，S/L/C/A/D五人独立context本人签名完整，负责人Reader与提交人U分离。Rel2 #2054545668044084248经同一真实审批链终态确认受控（已生效）。
- Main原上传选择Rel1 A/1被正式canPreview校验阻止；源码核对普通受控查阅使用独立VIEW矩阵，本例此前仅配置类别VIEW权限，属于fixture前置缺口。已通过上传控件删除未提交Main临时源件；未创建Main主档/BPM。正在Cfg本人页面补本例Category八个精确用户的VIEW矩阵，未修改共享类型。
- A确认弹窗数字编号已统一大写，曾因脚本大小写预期超时；当时仍停在同一确认框，已核对原task及负责人后完成一次确认，无重复签名。

## 2026-10-18T22:38:06.4718051+08:00 R22-01真实缺入口，交Root安排源码修复
- 原Main #2054545668044084249 / BPM1842903f-cb00-11f1-ad52-b082e25ec548，L任务3481664b-cb00-11f1-ad52-b082e25ec548待会签0/1；仅S已签名。L指派区真实可见按钮只有签名确认指派，后补/关联入口计数0，业务RED。
- Main原上传Rel1 A/1固定正文canvas非空，A标记命中、B标记不命中，局部PASS；未生成源新版或Main新版，禁止扩大为跨版本PASS。
- 独立VIEW矩阵八个精确本例用户已保存及重新打开读回；仅本例类别配置。Rel1/Rel2 ACTIVE终态已从真实受控浏览确认。
- 运行器finish正常退出0，runner47716/Chrome21488及归属进程核查为空；29份脱敏trace及2张截图保留。业务残留保留供原待办续跑；无生产/Git/服务/构建写入。
- 详见defect-supplement-entry.md、supplement-source-fingerprints.json、e2e-result.json与runner-checkpoint.json。四个任务CJS语法检查PASS，仅静态解析不冒充业务PASS。

## 2026-10-18T22:56:11.0644081+08:00 Root22:54后端不可达暂停
- 本例在Root通知前已因R22-01缺后补入口停止；最后finish正常退出0，runner47716、browser21488及归属子进程再次核查为空。没有新的context、重登录、业务写入或不确定提交/检入/签名。
- runtime-pause.json active=true，等待Root恢复同一200656 Jar、health及真实登录明确放行。Main原file2054545668044084249 / BPM1842903f-cb00-11f1-ad52-b082e25ec548 / L任务3481664b-cb00-11f1-ad52-b082e25ec548不变；Rel1/Rel2生效与原上传正文局部证据保留。
- R22-01真实UI缺入口结论保留；本次服务不可达是独立运行态阻塞，不归因账号权限，不扩权、不改服务/生产代码/DB。

## 2026-10-18T23:02:15.7405975+08:00 同包PID14864明确恢复
- Root证明health UP、官方真实登录22:57:39 PASS，8061/24564及48061/14864；同200656/hash264efbd4，无待验新后端源码加载。解除本例pause，新L独立context真实登录，从Main原待办只读回读，不重复创建/提交/签名，不因先前500改权限。R22-01既有RED保留。

## 2026-10-18T23:16:25.6127437+08:00 Root唯一授权前端测试准备（未执行）
- 新增一个最近的前端Node/SFC行为测试：IntRuoyiFronted/tests/unit/dcc-signoff-supplemental-relations.test.cjs。编译真实DetailSignoffAssignment、SignoffAssignmentPanel及DetailRelationArrangements模板，通过控件事件点击签名确认指派，真实workflowLifecycle wrapper的POST边界捕获请求；不是静态正则检查。
- 用现有FileSelectorState的真实选择校验作为选择器边界，校验relations目的、精确项目、固定Rel2字符串ID、取消/清空/未选、保留原Rel1和签名字段、revision整改。原关联写接口在测试中禁止独立调用，要求进入原签名事务。
- 本线程未运行测试、语法检查或lint；Root独占执行，实际单测RED状态NOT_RUN_PENDING_ROOT。Root命令：在IntRuoyiFronted执行node --test tests/unit/dcc-signoff-supplemental-relations.test.cjs。
- 四份原生产源码SHA256均与缺陷归档相同；仅新增授权测试和本例BDD/证据，不改生产。详见r22-01-test-only-source-check.json及r22-01-red-handoff.json。
- 恢复会话仅L真实登录进入个人工作台，未进入Main或执行业务；收到测试准备指令即finish，handle59698退出0，runner17416及子进程核查为空。Main原file/BPM/task不变，不空指派。

## 2026-10-18T23:37:10.3116281+08:00 R22-01 单文件实现冻结、待Root GREEN
- RED：Root运行node --test tests/unit/dcc-signoff-supplemental-relations.test.cjs退出1（e2e22-supplement-entry-red-process.json），Root报告6项1通过5失败，缺真实parent控件；未选的原三流程签名控制已通过。
- 唯一生产变更DetailSignoffAssignment.vue：在既有slot加入补充关联及确认名单，调用既有DccFileSelector relations模式；当前任务文件正式relation-permissions按需提供租户/项目/固定版本来源，候选查询强制当前项目。既有选择器自行读取正式项目目录；没有新增目录/API/DTO/组件/权限。
- 本地确认名单并入原saveAssignment，取消保留上次确认、确认空集合清空后补；没有独立关系写入，不改原上传关系、签名字段或revision整改validate。上下文watch同步清理来源/名单/选择器，generation忽略过期准备，签名时校验原file/BPM/task及部门候选。
- 纯渲染注册ElCheckbox/ElTableColumn/ElDatePicker/ElTag清除harness组件warnings，目标断言未修改。没有新TS运行依赖，沿用原API模块及现有选择器导入。
- 生产SHA256 1e0db0f9cd2607b05e69221021e09969e8d1a657b6dbde6bbff69eed83f47f32；受保护SignoffAssignmentPanel/workflow-actions/后端VO指纹保持。Root独占GREEN/回归，本线程没有运行Node测试/构建/类型/lint/E2E。Main原L待办零动作，供Root节点放行后原位验收。
- 冻结清单、命令及指纹见r22-01-green-freeze.json。

## 2026-10-18T23:52:20.5310830+08:00 Root六项GREEN后明确原UI放行
- Root e2e22-supplement-entry-green-process.json exit0，6项全部PASS；生产源码冻结，相邻Node回归由Root执行。当前14864/48061、8061/24564、同264efbd4后端。
- 新L独立context先原Main4249/BPM1842903f/L任务3481664b只读回读，再真实页面补Rel2 A/1、保留Rel1 A/1，后续本人签名指派C。无重建或新空指派。

## 2026-10-19T00:09:22.2684811+08:00 R22-01原任务真实UI GREEN
- L原待办3481664b：真实补充关联选择原Rel2文件4248/A1，确认后显示本次后补1；原上传Rel1文件4243/A1保持。L本人签名指派C成功，未重建Main。自然POST过滤记录只保留任务/会签人/后补ID/整改字段，不保留密码；supplementalRelatedControlledFileIds含2054545668044084248。
- C新独立context从实际待办进入原Main2054545668044084249、同BPM1842903f；冻结集合明确两行：Rel1 A/1原上传选择，Rel2 A/1后补选择；关联阶段显示L签名身份对应本节点、Rel2本次补充，节点记录显示L会签指派C。真实已保存后补回读通过，详见manifest.supplementUiVerified及signed-supplement-freeze-readback。
- 原失败证据保留；仅R22-01入口/签名后补子步骤更新为PASS，完整两版本/引用/整改仍待后续。
2026-10-19 11:37 Root续验：22原4257单次提交4258/B1，BPM31cef883；S本人签名与L本人指派C均成功，瞬态DOM捕获bodyDenied通知，停止重复签名。L正常页面响应（仅诊断）精确POST reading-session/stop返回1080000012；非先前推测task-action-readiness，撤回该项ignoreErrorMessage及专属测试，GET旧改动也不保留。R1624-11与R28归同一关闭根因。仅结束已有会话改为验证原源绑定HMAC、租户/用户/版本/事件/水印及精确session上下文，其他继续读取权限保持。有效RED1FAIL/0ERROR；首GREEN正向已过但旧拒绝stub未调用触发Mockito清洁检查，移除后参数化72项仅wrong-version预期异常类型错误，保留生产原IllegalArgumentException并纠正测试，不改守卫消错。r3运行中；C待办保留，浏览器正常finish/exit0、trace已脱敏。未部署新后端、未将测试通过当业务通过。
2026-10-19 12:00 R29原流程：Rel1B1已ACTIVE、旧A1 OBSOLETE后，Main原冻结关联证据被Query.assertRelationNameVisible拒绝，导致前端未能核验原轮次；原读失败保留。有效服务RED1FAIL/0ERROR精准命中同一assertRelationNameVisible。最小Query修改仅关联名称/正文资格及权限投影使用既有canReadHistoricalVersion，历史canEdit固定false、编辑命令明确拒绝；日常canViewFileName、正文上下文、选择器ACTIVE限制不改。首次GREEN368/369通过，唯一新正例在原fixture的mock versionPolicy缺返回报正式source指针错误；只给该新测试接入真实defaultPolicy，严格守卫不动。r2 369/369 PASS，XML和汇总归档，完整包构建中。Reader已在原P2F1、P2F2、P3F1真实引用MainA1，明细显示2项目/3目录且逐行A1正确，截图保存；浏览器正常finish/exit0、所有trace脱敏。R28已由原C/A/D真实签名、stop请求和零瞬态通知/零关闭失败三项独立通过，root-review保存，未将整例22写成PASS。
2026-10-19 12:14 Root已部署R29最终包120105/SHA ba0a8ead…，PID37360/48061，前端49700/8061；30原文件全列hash、0待办、暂停5627前后完全相同，health UP与Reader真实登录通过。原Main再次回读原/后补冻结证据及Rel1旧A1实际PDF非空、旧标记命中/新标记无命中，R29 root-review保存。原MainA1真实同名新正文检入4259，局部A2草稿CE/注册人否/生产方是已保存；一次旧toast定位失败不算保存失败，取消未提交确认后从正式列表重开同4259，实际勾选值回读正确，才单次正式提交4260/A2。S真实签名成功且R28观察继续无通知/关闭失败，L原会签指派任务已打开、正在选Rel1A1整改。工作稿未重建、原MainA1未改属性，3条A1引用保持；无Git。

## 2026-10-19T16:30:57.8583681+08:00 检出串版根因与定向RED交Root
- 最新Root manifest明确MainA1=4249/A2=4260，原12:39:25仅检出A2，两版本却采集同一U/时间；原锁12:42已真实UI撤销。继承R28/29/30与原审批/不同属性/整改/引用通过，不据旧BLOCKED重建。
- Query两个fillCheckoutProjection直接复制Master活动锁，buildVersionHistory也共用该lock；baseIterationId未参与版本展示。旧logicalCheckoutIsVisibleFromFormalAndOtherHistoryVersions测试反而锁定所有历史同actor/time，现改为版本精确事实与Master互斥分离。
- 最小方案checkedOut布尔继续表示Master互斥；by/name/time/reason仅baseIterationId==selectedId复制，其余显式清空。写入selectActiveByMasterId/唯一锁/checkout-checkin-cancel服务不改。生产本轮未修改，先等待Root执行RED及明确放行。
- 更新直接行为测试：公共getControlledFile与历史列表，另参数化真实私有两个overload，覆盖EXACT/OTHER_VERSION/NO_LOCK、Long精度与旧残留清理。精确diff/命令/指纹见checkout-isolation-red-handoff.json。Root独占Maven，本线程未跑。
- 自有脚本新增verify-main-checkout-isolation强断言并保存截图；旧verify-main-version-facts不再仅记录检出字段。本次无浏览器/业务动作。

## 2026-10-19T16:48:56.9159964+08:00 Root有效RED成立，授权两个overload生产GREEN
- Root e2e22-checkout-isolation-red：4tests/3failures/0errors；EXACT控制通过，公共旧版借用actor88、OTHER_VERSION及NO_LOCK残留精准失败。证据：协调目录同名process.json/xml。
- 已核对Query SHA与RED前相同。唯一生产修改两个fillCheckoutProjection：checkedOut仍读Master锁布尔，显式清空by/name/time/reason，baseIterationId与精确selectedId匹配才填入。readLogicalCheckout、写锁/检入/撤销及assertRelationNameVisible不动。Root独占后续GREEN/Maven，暂不UI。

## 2026-10-19T16:51:12.8871104+08:00 两个投影overload最小GREEN实现冻结
- Query精确6增2删，diff来自本轮改前snapshot对比，不含原工作区既有变更。当前SHA221e080af27cc84e958a18bd02097d796db164a596775908eff39f91f0ca54aa，checkout-isolation-green.diff及green-freeze.json已保存。
- checkedOut依然表示Master锁；先清空四个版本事实，再用Objects.equals(baseIterationId, selectedId)判定填入。两个投影分支均覆盖无锁与其他版本，精确分支保留原actor查询与Long比较。readLogicalCheckout、所有互斥写侧与assertRelationNameVisible未改。
- Root统一GREEN/Maven/回归及运行包；本线程没有执行任何测试或新UI动作，原Main对象和已通过参数未重跑。

## 2026-10-19T16:54:09.7191797+08:00 Query所有权释放给Root
- Root review确认检出投影精确6增2删，当前Query停止编辑，交Root统一GREEN并串行处理13的独立窄点。精确生产/测试SHA见checkout-isolation-query-ownership-release.json。
- 本线程仅维持原检出隔离UI脚本，待新Jar/hash明确放行再跑；原主链不重跑。

## 2026-10-19T16:59:03.9529104+08:00 Root定向GREEN已实际通过5/0/0
- 已读协调目录e2e22-checkout-isolation-green-process.json及XML，exit0、5tests、0failures、0errors，包含四项原RED及revisionOptions守卫；Root执行日志保留。
- actual GREEN同步red-handoff、green-freeze、result、checkpoint和task。Query保持冻结，所有权仍Root；等待13窄点一起统一回归/新包。
- 自有脚本verify-main-checkout-isolation及本次锁UI撤销准备保留，未运行新代码E2E，未重跑通过主链，未产生业务动作。

## 2026-10-19T17:09:47.2762172+08:00 精确修正扩大回归唯一旧合同
- Root277项Query回归唯一旧断言把baseIterationId901的actor投影到900；已按Root测试窗口只改nameVisibleBrowserAndHistoryExposeActualCheckoutIdentityWithoutBodyOrReason一个方法。900三个事实为null，901保留actor88/name/time，两个checkedOut=true及全部权限/元数据隐藏断言保留。
- 改前/改后内容检查证明其他测试方法完全不变；生产SHA前后相同。本线程没有运行Maven/UI，冻结diff与精确测试SHA见checkout-name-only-test-freeze.json，交Root统一复跑。

## 2026-10-19T18:47:04.1435857+08:00 r2冻结展示实际Node RED
- 原正式详情loadApprovalDetail刻意不读实时任务，buildSignatureTraceRow却只用实时scope/tasks，签名摘要已有meaningCode/process/version，属于前端映射遗漏。关联header无departmentName，但同签名原nodeRecords.assignmentTarget已通过DccApplicationNodeRecordProjection从原taskAssigneeSnapshot冻结departmentName，原页面confirmedFields也显示，属于已有冻结证据未接到此展示；不从现组织或签名人部门反推。
- 新测试实际parent函数和原义务formatter：4项2通过2精准失败；合法冻结职责与合法原义务名称被显示未记录；跨轮次/缺值负例保持通过。命令和结果见frozen-signature-presentation-red.json。
- 最小修复只前端读取已返回冻结字段，无后端Query非检出方法/权限/新API写入。

## 2026-10-19T19:05:45.7678822+08:00 r2冻结展示GREEN与原UI定向PASS，交Root后端窗口
- 前端49项/0失败/exit0（冻结职责、原义务快照、只读检出原因、原native duty/supervisor/delayed/relation evidence/selected relation）。本例轻量Node已执行；未启动Maven、全量类型或构建。
- 原MainA2管理详情：S直属主管、L部门负责人指派、C会签人、A批准人、D文控审核人逐行真实DOM通过；原A2和历史A1同各自BPM关联stage原义务部门均真实显示DCC241016C22Dept，使用同task/signature/actor/stage绑定的原任务指派快照，不用现组织或签名人部门。截图已目视核验。
- Reader21924独立登录只读运行，trace目录fixture-1792407117723；3张截图、trace-1.zip。A1经正式版本历史→viewer→查看详情进入；ALL项目列表只显示当前版的脚本定位超时保留为原始工具错误，未误判产品或拼URL。
- runner/context/browser正常关闭exit0，21924/24672及子进程核查为空；仅自然读取/阅读遥测，无检出/签名/审批/配置业务命令，mainCheckout旧锁仍active=false，未重放撤销。
- 当前120105旧包不含检出修复。Root13先Maven，申请随后串行nameVisible+Query回归、全量类型及统一新包。已通过主链/R29/R30不重跑；完整业务不能提前写PASS。文件/方法/命令/结果/运行态依赖/剩余项/证据全部在repair-20261019-r2-handoff.json。

## 2026-10-19T19:54:27.6487724+08:00 r3范围与最新Query回归收口
- 按repair-dispatch-20261019-r3.md保留已通过分支，仅检出隔离新包UI未闭环。当前dispatch仍为旧120105/ba0a8ead，不执行检出修复验收。24独占Maven、14全量类型，本例不运行/抢占。
- 实际只读核对13/runs/20261019-repair-r2/r2-targeted-final Query XML：277/0/0；nameVisible、logicalCheckoutKeepMaster、3个projection参数、revisionOptions守卫逐项无failure/error。四核心套件323/0/0；组合process exit1来自额外OfflineTraining夹具2error，准确区分，不把整个命令记PASS。
- r3 handoff与r2/green-freeze/name-only-freeze最新回归证据已同步，旧失败不再作当前阻塞；历史原始报告不删除。49前端测试和原A1/A2冻结展示UI已通过，不再重跑R29/R30或展示。
- 旧Main锁active=false/restoredThroughUi=true；本轮零业务命令、无浏览器。新包后仅原A2新锁/双版actor-time-reason正负例/撤销及回读，异常保留现场并记录本次新锁清理状态。

## 2026-10-19T21:02:34.3764079+08:00 r4唯一未闭环参数准备交接
- 已读repair-dispatch-20261019-r4.md、根AGENTS及执行/端口/登录/收尾规则，核对实际int_qms。最近层无额外任务AGENTS。Query不再编辑，原277/核心323、前端49及原页面通过保留。
- 当前dispatch仍旧120105/ba0a8ead，尚无本轮新包通知；不启动浏览器、不检出、不撤销旧锁。原MainB4260锁manifest active=false/restoredThroughUi=true。
- 定向流程仅新U context：先读旧锁不存在→原A2检出一次→A2真实人/时间/原因→A1历史三项未检出→返回A2仍本次锁→真实UI撤销本轮新锁→双版未检出回读→关闭进程。失败保留原始断言及精确新锁清理结果，禁止重复提交。
- repair-20261019-r4-handoff.json包含原始process实际命令与退出码/XML、代码归属、保留页面证据、运行态依赖、未执行E2E(exitCode=null)。准备不计新包PASS，Maven/类型/服务/Git均未操作。
2026-10-20 R2第四轮：新独立目录 e2e-artifacts/rerun-20261020-r2-round4；22-A/B/C完整真实页面PASS，7项Reader+6项新锁事实，原始trace/截图目录fixture-1792479737976及fixture-1792480278436。新锁UI撤销，两runner26172/40412 exit0且OS进程树无残留。详情与首错见独立verification-report.md/handoff.json；工程blocked、HOLD，不覆写历史报告。
