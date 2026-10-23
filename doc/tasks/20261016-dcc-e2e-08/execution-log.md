# Execution Log

- 2026-10-16：核对当前分支为 `int_qms`；已读取根 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 和公共 `thread-contract.md`。
- 2026-10-16：Root dispatch 当前为 `prepare_only`，环境 `int_qms` / `8061` / `48061` 尚未 runtime verified，角色 fixture 尚未 verified；按约定未启动浏览器、未登录、未写业务数据。
- 2026-10-16：源码入口核对：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue` 的正式页面路由为 `/dcc/controlled-file/upload`；项目切换位于 `handleProjectCodeChange`，包含清除选择确认和项目属性替换确认；类型切换位于 `handleFileTypeTaxonomyChange`；关联窗口为 `DccFileSelector`，只允许当前项目的 ACTIVE、可预览受控版本；“其他”自定义控件为 `CustomApplicationMatrixEditor`。
- 2026-10-16：新增 `project-type-switch-protection.e2e.cjs`，脚本在浏览器启动前读取 dispatch 并要求本例 gate 为 `execute`、精确 int_qms 端口及统一 `runtimeVerified`/`roleFixtureVerified` 均为 true；默认 `prepare_only` 仅输出 `PREPARED_WAITING`。
- 2026-10-16：静态验证：`node --check doc/tasks/20261016-dcc-e2e-08/project-type-switch-protection.e2e.cjs` 退出码 0；结构扫描确认 dispatch 先读、无 fetch/axios/APIRequest/localStorage/page.evaluate、使用真实页面 locator、包含 trace 和截图。脚本被 `.gitignore` 的 `doc/tasks/**/*.cjs` 忽略，需由 Root review 决定是否强制纳入证据提交；本线程不执行 Git 写操作。
- 2026-10-16：门禁演练命令 `$env:DCC_E2E_RUN_ID='prepare-static-check'; node doc/tasks/20261016-dcc-e2e-08/project-type-switch-protection.e2e.cjs` 退出码 0，生成 `artifacts/prepare-static-check/result.json`，状态为 `PREPARED_WAITING`、`browserStarted=false`、`businessWrites=false`；未启动浏览器。
- 2026-10-16：运行器来源核对：前端 `package.json` 使用 Node 直接执行任务脚本；依赖为 Playwright `1.60.0`，模块解析自 `IntRuoyiFronted/node_modules/playwright`，Chromium 可执行文件存在于本机 Playwright 缓存。真实执行仍需 Root 先将 dispatch 更新为 `execute` 并核实环境。
- 2026-10-16：脚本修订后复核：`node --check .../project-type-switch-protection.e2e.cjs` 退出码 0；再次运行 prepare gate rehearsal 退出码 0，结果仍为 `PREPARED_WAITING`。结构扫描确认无原始 query/token URL 记录，事件仅保留自然非 GET 响应的脱敏 pathname 和 HTTP 状态。
- 2026-10-16：未执行真实 Playwright；未生成 trace、页面截图或业务结果。`prepared.json` 记录参数、前置角色/数据要求和分支清单。
- 2026-10-16 Root review 补强：脚本现为 11 个独立子用例，覆盖项目第一层取消、项目属性第二层取消、项目确认、已维护类型取消/确认、“其他”取消/确认、仅属性/仅会签/仅下发手改取消。每个子用例独立 context、登录后才开始 trace；`snapshots=false,sources=false`，认证字段不进入 trace。
- 2026-10-16 Root review 补强：每次源件上传前落盘 `assetId`、文件名、项目和 `upload-attempted`；源件显示后断言非空 PDF canvas；确认切换断言关联、源件预览、编号及相应名单清空。finally 通过真实上传列表删除入口等待自然 session-cleanup，无法核实时保留 residual 并停止后续写入。
- 2026-10-16：追加 `structure-check.cjs`、`fixture-template.json` 和 `static-review-check.json`；语法与结构检查退出码均为 0，未启动浏览器。当前 dispatch 已 `runtimeVerified=true`，但 `roleFixtureVerified=false`、gate 仍 `prepare_only`，真实执行继续等待 Root 放行。
- 2026-10-16：最终准备复核：`node --check` 两个脚本、`node structure-check.cjs`、三个 JSON 解析均退出码 0；确认 11 个子用例、登录后 trace、关闭 snapshots/sources、逐资产清理台账、非空 PDF canvas、第二层取消及仅属性/会签/下发分支均存在。`artifacts` 当前无文件，未产生本轮业务证据。
- 2026-10-17 实际推进：本例角色/账号、组织岗位、项目 P1/P2、类型树、类别及两个公共矩阵均走真实 UI 准备；P1/P2 审核至 COMPLETED，5 个本例业务身份完成签名图片页面回读。管理员只作配置，业务角色独立 context 登录。详见 fixture-manifest.json 和对应 fixture-evidence；旧准备状态为历史。
- 2026-10-17 实际验证：属性 3 分支 PASS；名单/类型 7 分支 PASS，其中 5 项明确无生效关联前置，不能替代完整关联场景。属性证据记录 SHA637971…，最后名单/类型证据记录 SHA1e8d1c…；未进行源件正式上传、提交或审批。
- 2026-10-17 22:05 环境中断盘点：upload-candidate.cjs 尚未写成、工具未派发候选动作，manifest 无候选；没有在途正文上传/正式文件审批。保存 runtime-interruption-20261017-2205.json，恢复本例暂停保护；只读核对期间没有浏览器启动或 UI 写入。依赖和运行态由 Root 恢复。

Root恢复放行：同一Jar 1e8d1c79…，后端36320/48061、前端24564/8061，health及真实登录PASS；保留22:05检查点，先fresh登录回读已有资产，再补完整关联场景。

恢复推进：两份原候选正式提交、主管签名、指派、会签和批准已走真实UI；P1流程详情当前处理人为共享标准化文控戊20261013，自己的DOCCTRL无待办，按原要求等待Root协调原冻结任务正式处理路径，未放宽ACTIVE前提。候选尚未生效，完整关联尚未运行。

2026-10-18：完整关联七分支PASS；按Root授权用OTHER仅准备两个ACTIVE输入，主验收PUBLIC T1/T2/OTHER范围不变。所有原始失败与修正依据保留，历史临时资产逐项复核无服务器残留；已关闭本例浏览器。进入ready_for_closeout，Root review和Git门禁单独记录。脚本被gitignore忽略，若日后授权提交需逐项force-add公开证据，严禁加入DPAPI。

最终结构检查PASS；cleanup preview exit0仅生成计划；按用户禁止Git提交/推送将正式收尾置blocked后，apply exit3准确拒绝，无文件删除。当地cleanup SKILL.md缺失，但现有独立脚本已读源码并按仓库规则运行。最终P1/ALPHA、P2/BRAVO受控正文截图目视通过。隐私扫描开始于浏览器关闭、全部正式报告落盘后。

最终隐私扫描PASS（48403项，files=[]，credentialValuesPrinted=false）；验收汇总、正式收尾分离状态及Root review入口已落盘。

2026-10-18：引用Root独立review ../20261016-dcc-24-e2e-coordination/e2e08-root-review.json（2026-10-18T00:50:03.868326+08:00，ROOT_ACCEPTED），接受原7完整关联+10独立检查业务证据。本次仅同步报告和状态引用；E2E-08停止执行，无新增业务、重跑、清理、浏览器启动或Git写入。工程closeout因用户禁止Git独立blocked；原失败、trace、自有数据和已关闭浏览器状态保持。

## 第二轮新重跑 rerun-20261020-round2-1792462063637

Given 当前int_qms及运行证明 runtime-proof.json；When 使用原本人账号在8061真实页面重新执行7完整关联、3属性和7名单/类型分支；Then 逐分支以当前DOM断言，独立记录PASS/FAIL/BLOCKED、身份、trace、截图和关闭回执。使用本轮唯一源文件名；不提交旧终态、不改共享配置或产品。仅验收及问题收集，不做Git、重启、子Agent或cleanup apply。本轮状态in_progress，历史ROOT_ACCEPTED只适用于历史结果。

## 第二轮新重跑结果

17分支本轮均有PASS证据，首遍16 PASS/1 BLOCKED，TYPE-CANCEL独立复测1 PASS。间歇分类解析错误E2E08-R2-01保留OPEN_INTERMITTENT。详见rerun-20261020-handoff.json和rerun-20261020-round2-1792462063637/report.md；20份trace隐私/CRC扫描PASS。两次runner退出0且进程不存在，浏览器关闭、context=0。只进行了真实前端验收及本例测试资产校准，无产品/公共配置/数据库/Git/服务/子Agent操作；未启动下一轮。工程closeout因禁止Git独立blocked。历史ROOT_ACCEPTED仅属于历史运行，本轮不继承该复核结论。

## 分类首次加载间歇错误诊断（2026-10-20）

状态in_progress。本轮限定只读生产源码及本例诊断测试；Upload页和共享帮助函数由02独占，不修改。历史FAIL/BLOCKED保持不变。
BDD: Given 首次类别目录未就绪而项目/文件类型已可选择；When 正式类型分类解析先完成、基础类别目录后完成；Then 正式有效分类最终应可用，不能留下错误且需要用户重选。用真实生产函数的竞态行为测试证明当前行为与正常顺序差异，独立记录RED；未经Root串行放行不改页面，不以诊断对照GREEN声称产品已修复。
预期验证：生产函数/调用顺序与精确源码指纹、类别先完成和后完成两顺序、缺失/多绑定/接口失败/过时请求边界；必要时仅最小真实UI复现，不全量重跑。无Maven/package/重启、API/DB业务动作、Git、子Agent。

## 分类首次加载根因诊断交付

本轮仅生产代码只读+本例竞态行为测试。实际生产onMounted并行启动基础类别目录和类型候选；正式分类解析先返回时，availableCategories为空产生唯一分类错误；目录后到不会触发重新解析，categoryId持续null。历史失败自然响应事件active-category(8)早于file-categories(9)，与此时序一致。根因报告classification-race-diagnosis-20261020/diagnosis-report.md；RED退出1（3 PASS/1 FAIL），诊断对照退出0（4/4），后者不是产品GREEN。未开启浏览器，无API/DB业务动作或响应体读取；无Git/构建/重启/子Agent/产品文件修改。Upload由02独占，待Root串行安排初始目录完成屏障；问题保持OPEN，历史BLOCKED不变。

## 串行放行分类竞态修复（2026-10-20）

Root已明确释放02独占窗口，本轮只改Upload首次目录就绪门禁及专属测试。BDD: Given 类型候选先完成且首次类别目录仍加载；When 作者准备选择类型；Then 控件保持加载/禁用，目录成功后首次选择立即得到唯一正式分类；目录失败明确报错并继续禁用，缺失/多绑定/权限/过时请求校验不变。先运行新专属业务RED，再最小修复、GREEN及相邻定向回归。历史诊断和FAIL不改写。完成后hold等待Round2，不开浏览器、不E2E、不构建重启、不Git。

### 串行修复交付

RED退出1（4 PASS/2 FAIL），最小产品diff +6/-1 后GREEN退出0（6/6），相邻三份单测退出0（26/26）。旧分类绑定静态合同退出1；精确修复前快照与修复后同一Number正则断言均失败，已记录PRE_EXISTING，未改该合同。初次GREEN中的VM缺Boolean仅修正测试环境后重跑，保留原始输出。报告、命令、指纹与精确diff见classification-race-fix-20261020/report.md；最新交接repair-20261020-fix-handoff.json。未开浏览器，未做API/DB、构建重启、Git或子Agent；hold等待Round2。工程closeout因用户本轮禁止Git为blocked，未执行cleanup apply。

## 正式EXECUTE rerun-20261020-r2-round2

本轮Root明确授权上一轮全部关闭后完整重跑。先记plan.md中的BDD/里程碑，再核对int_qms、后端41508/48061、前端20976/8061、指定Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147，写本轮gate.json execute/runtimeReady=true。旧runner仅本例副本校准运行证明/PID、独立输出及初载专项，12处校准见calibration.json；node --check退出0。执行node doc/tasks/20261016-dcc-e2e-08/rerun-r2-round2.cjs doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r2-round2，runner PID12732，退出0（业务结果另为FAIL）。

真实作者新登录并核验P1/P2正式A/1基线及非空正文；完整关联7/7 PASS、属性2/3 PASS、名单/类型6/7 PASS，原17分支合计15 PASS/2 FAIL/0 BLOCKED。分类首次加载专项PASS；自然时序目录先到，未强制网络迟到，不冒充迟到时序UI证明。两失败为属性二次取消、仅改下发取消后项目名称变为294，其余快照字段保持。首次失败截图/DOM/trace保留，未重跑；Root建议原页面有界等待到达时原context已按runner正常收尾关闭，持续性未定，不能抹去首次FAIL。问题E2E08-R2R2-01 OPEN_UI_DISPLAY，源码推断及边界见handoff.json。

19份trace CRC/隐私扫描PASS（7482项），临时源件全部页面清理。browserClosed=true、context=0、runner及登记六个进程均不存在；关闭证据closure.json/process-closure.json。最终逐17项及专项在rerun-20261020-r2-round2/handoff.json/report.md，整体FAIL。无产品改动、API/DB业务动作、Maven/构建/重启、Git、子Agent或cleanup apply；已HOLD，不启动第三轮。工程closeout受Git禁令blocked。

最终归档复扫：隐私/ZIP CRC PASS，19份trace、7485项；六个冻结前端文件哈希全部保持，任务结构核验PASS。未启动新业务轮次。

## E2E08-R2R2-01项目名称取消回填定位

按本轮授权只读核对两个失败的截图/DOM/trace及当前int_qms源码，写最小BDD后执行node --test doc/tasks/20261016-dcc-e2e-08/project-label-diagnosis-20261020/project-label-cancel.test.cjs，退出1：2 PASS/2 FAIL。测试使用生产SFC函数、Vue响应式和实际Element Plus标签查询/卸载缓存回调；远程P2候选在确认期间替换后，两个取消分支均恢复ID294但丢失正式映射、显示294，其他表单值保持；未过滤候选对照均PASS。首次脚本标签正则提取失败仅属harness，原日志保留，修正SFC解析后获得有效RED。

结论是产品选中项目映射缺口，不能通过放宽脚本断言解决。原trace网络流为空，未声称还原精确历史响应时刻；原页面已关闭，症状持续性仍未确定。只提出局部5hunk +8/-1 diff（保留已接受正式项目对象，成功搜索时维护显示映射，接受/重置时更新清空），未应用或GREEN。详见project-label-diagnosis-20261020/report.md、handoff.json、red-observations.json。等待Root共享Upload串行窗口，仅建议两个失败路径及必要边界GREEN，真实UI只跑失败分支另行放行。没有浏览器、已通过分支重跑、产品/公共矩阵修改、构建、重启、Git或清理；原首次FAIL不改写，test进程已退出。

## Root串行放行项目标签局部修复

用户明确E02已HOLD，批准E08只修复取消后项目名显示ID。先读完整工作区diff及E02交接，精确快照upload-before.vue保存原17+/2-等已有改动。只实施五处局部+8/-1：保留已接受正式项目对象、成功搜索结果维护其选项、接受新项目更新对象、清空/完成重置清除。请求参数过滤、矩阵、提交规则、E02属性编辑/capture恢复及类别初载修复不变。

BDD沿用两取消路径并补接受/清空/失败边界。命令node --test doc/tasks/20261016-dcc-e2e-08/project-label-fix-20261020/project-label-cancel.test.cjs，原有效诊断RED已先于修改；本轮扩展RED 3 PASS/4 FAIL退出1，GREEN 7/7退出0。清空测试VM缺message.confirm仅属harness校准，保留原输出；校准后用E2E08_SOURCE=upload-before.vue精确快照复核RED，再对当前源GREEN，未放宽断言。GREEN后停止所有测试执行。python verify-delivery.py核对仅五处授权字节替换，所有旧改动及8份原首次失败证据完整保留。

最终精准diff、命令、结果、字节SHA877479ffcfbe4f9266094d0305f9ae9e0df2458c107729c3a79d3974eb769530、规范LF哈希和后续建议见project-label-fix-20261020/report.md、handoff.json、source-verification.json。测试进程已退出；无Playwright/浏览器/业务重跑、Maven/build、重启、Git或清理。HOLD，后续真实UI仅建议Root另行放行两个原失败分支，并同页有界观察；原FAIL不改写，工程closeout因Git禁令blocked。

2026-10-20：按本轮授权，仅复验 ATTRIBUTE-ONLY-INNER-CANCEL、DISTRIBUTION-ONLY-CANCEL；独立证据 project-label-ui-20261020；原业务断言保留，增加浏览器自然加载模块源码指纹与取消后 5 秒采样，不扩大到其余 15 项。

2026-10-20 16:34：project-label-ui-20261020 定向真实 Playwright 复验 2 PASS / 0 FAIL / 0 BLOCKED，runner exit 0。两次正常登录、菜单进入，实际自然加载 SFC SHA 均为 877479ffcfbe4f9266094d0305f9ae9e0df2458c107729c3a79d3974eb769530。属性第二层取消后 5055ms、仅下发修改取消后 5052ms 采样均保持 P1 正式名称与逐字段原值。原 FAIL 的 8 个文件哈希未变；2 trace CRC/隐私扫描 PASS；终点截图人工查看。浏览器已关闭/context0，自有 runner+4 浏览器进程均退出。前后服务 PID 和源码 SHA 一致，窗口交回 Root。
状态里程碑：实现与本轮定向验证 ready_for_closeout；因当前 Git/清理禁令，最终工程状态 blocked，不执行 preview/apply、不写 completed。其余 15 项未复跑；本轮没有生产代码修改、构建、重启、Git 或业务清理。结果与边界见 project-label-ui-20261020/report.md、handoff.json。


## Root 接受定向修复复验（2026-10-20）

Root 已 review 并接受 ATTRIBUTE-ONLY-INNER-CANCEL、DISTRIBUTION-ONLY-CANCEL 两项当前修复真实页面复验。旧失败现存，新独立复验2PASS；旧 rerun-20261020-r2-round2 仍为15 PASS/2 FAIL，原 result/handoff/截图/trace 八文件 SHA 全部保留。新 project-label-ui-20261020 为独立2 PASS，非新跑完整17项；其余15项未复跑。模块自然加载源码指纹、5秒采样/截图、trace CRC/隐私、PID closure 已获Root接受。最新交接：[handoff](project-label-ui-20261020/handoff.json)。

本轮仅文档/交接及协调台账同步；按本轮明确授权，仅为本例向协调台账08项追加 repairRevalidation，覆盖原“Root独占协调目录”的本次必要例外，其余项及旧计数保持。结构与八文件SHA检查后HOLD；不操作浏览器/业务数据，不重跑其余15项。工程closeout保持blocked。

R3第二轮启动：按最新授权完整17分支和分类专项，先核验当前R3 proof/dispatch，fresh UI身份及自然请求可用性，保留首次超时与未跑项。计划见rerun-20261020-r3-round2/plan.md。


## R3第二轮独立完整合同结果

结果：PASS，17 PASS / 0 FAIL / 0 BLOCKED，共17分支。本轮全部真实页面新执行，不复用旧PASS。分类首次加载专项 PASS。

当前运行证明 doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-runtime-proof.json；2095来源文件哈希核验一致，8061/PID20976、48061/PID41508、Jar709e78bd。原作者芋道源码/DCC241016C08AUTHOR，各业务分支fresh context真实登录。

|分支|状态|证据目录|
|---|---|---|
|E2E-08-PROJECT-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-PROJECT-INNER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-PROJECT-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-TYPE-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-TYPE-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-OTHER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-OTHER-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-linked|
|E2E-08-ATTRIBUTE-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-ATTRIBUTE-ONLY-INNER-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-ATTRIBUTE-ONLY-CONFIRM|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-SIGNOFF-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-DISTRIBUTION-ONLY-CANCEL|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-TYPE-CANCEL-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-TYPE-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-OTHER-CANCEL-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-OTHER-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|
|E2E-08-PROJECT-CONFIRM-NO-RELATION|PASS|doc/tasks/20261016-dcc-e2e-08/rerun-20261020-r3-round2-contract|

首次监控错误把短导航GET中止判阻塞，原17项未跑result保留；校准监听耗时后执行合同。Python UTF-8读取脚本首次失败亦未触发浏览器，后显式UTF-8修正；不计业务失败。完整合同阶段P1正式A/1列表ACTIVE，但原5秒canvas未出现，7关联分支暂BLOCKED保留；一次同对象只读核验显式等待45秒，两基线非空正文通过，之后仅续执行原未跑7分支，未复跑已PASS10项。

详细原错误、DOM、实际基线fileId/版本、每分支逐字段前后值、临时源件/清理/残留见handoff和两个原始result。合同只测试切换及临时上传，不正式提交/签名/创建新版。现有A/1用于关联，不被重新创建。所有业务均Playwright正常UI，无API/DB业务动作或JSON oracle。

所有浏览器/context/runner已关闭，trace CRC及凭据扫描PASS，索引见handoff.closureIndex与evidence-index.json。原R2 FAIL及8文件SHA不变，旧未知事故不因本轮结果被关闭。未改产品/数据库/公共配置，不构建/重启/Git/子Agent，不改协调台账。工程closeout仍blocked，HOLD等Root。

最终结构核验PASS：17唯一分支、notRun0、residual0、21trace/8166扫描项隐私CRC通过、全部资源关闭；2095当前源码快照结束复核一致。首次监控误阻塞和5秒预览失败保留。最终handoff为本轮分阶段17项新执行汇总，三个原始result不改写。HOLD。
