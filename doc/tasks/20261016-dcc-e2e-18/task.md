# E2E-18 升版申请与历史附件

## Goal

独立验证E2E-18，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — PARTIAL与REPLACEMENT两个原业务参数均完整真实UI PASS，原逻辑文件A1/4232→A2/4252→B1/4256；正文、两附件及培训逐版归属通过。R28后续已由Root在22原C/A/D签名真实复核通过，不再待复验。2026-10-19仅核对已有17张PNG及28份脱敏trace索引并同步过期记录，未重复业务。业务结论见latest-acceptance-review.json及root-integrated-result.json；工程收尾因未授权Git提交/推送及未执行cleanup apply而blocked，不标记completed，保留证据和对象。

## BDD

- Given 存在受控版及可选择的小版本草稿，旧版有附件和培训记录
- When 从真实入口选择草稿发起升版、填写实际变更类型及说明、添加多个附件并完成相应流程；回看新旧详情
- Then 升版详情正确展示来源受控版、所选草稿记录、实际变更类型和说明；新旧附件、培训记录及申请信息各自归属原版本；草稿在用户页面不暴露小版本号。

- REPLACEMENT保留原参数：Given 已通过本例PARTIAL产生真实A/2及其独立附件/培训，When 原U检入不同正文和两份REPLACEMENT新附件，选择该草稿、换版类型及说明，完成S/L/C/A/D和U新培训，Then 得到B/1、来源A/2及精确草稿，回看A/1、A/2、B/1不混用正文/附件/培训。编号沿原逻辑文件不变；不通过新建另一原版重复相同准备，不省略REPLACEMENT完整审批。

### 浏览查询性能回归 Root唯一分配

- Given 同一用户浏览多个候选文件，部分目录可QUERY、部分仅PREVIEW、部分均无授权，正文详情守卫独立拒绝。
- When 通过真实服务入口getControlledFileBrowserPage查询；随后撤销目录授权并再次独立查询。
- Then 第一次返回且只返回有查询或预览目录授权的候选，同用户同accessType目录集合每请求最多读取一次；第二次重读两种集合且无候选可见，不能跨请求缓存绕过撤权；名称投影不授予正文能力。
- RED：Root实测1test/1failure/0error，真实浏览3个正确可见行及预览/下载拒绝通过，QUERY wanted1实际多次；证据为协调目录e2e18-directory-permission-red-surefire.xml。
- GREEN：Root已唯一分配并完成最小实现，等待Root Maven；仅browser候选过滤局部EnumMap、私有6参canAccessQuery和4参目录助手；旧4/5参及原3参委托路径保持原行为，Objects.requireNonNull及异常传播保留，QUERY/PREVIEW短路不变。源码和测试冻结，不新增参数或扩大SQL/正文守卫/目录服务/全局缓存范围。

## Milestones

### 电子分发名单查询 Root唯一分配行为RED

- Given 当前用户不是申请人，矩阵和目录无可读授权；多份ACTIVE候选中仅本租户本人有效电子分发集合内的两份允许可见，另有无收件资格、待审及外租户收件资格的候选。
- When 实际调用getControlledFileBrowserPage；随后撤回本租户本人集合并再次独立查询。
- Then 正确返回两份且不授予正文预览/下载；每请求批量名单读取一次，逐文件count读取零次；第二请求返回零并重新读取名单，保留待审资格及tenant/user边界。
- 先仅修改DccControlledFileQueryServiceTest，生产继续冻结；RED待Root独占Maven实测，不提前声明PASS/FAIL，不提前改keyword/SQL/缓存/正文守卫。
- Root实际RED：e2e18-electronic-access-red-process.json退出1，1test/1failure/0error/0skip；正确可见两行、待审/外租户拒绝先通过，失败为现有bulk未调用。Root随后唯一授权QueryServiceImpl最小GREEN，测试保持冻结。
- GREEN边界：仅browserSummary候选请求用当前tenant/user查询既有批量收件资格一次并持有局部Set；沿私有重载传至ACTIVE资格判断。旧详情/正文/其他endpoint继续原count逻辑，原目录EnumMap保留；无新Mapper/SQL/策略类/ThreadLocal/全局缓存，不提前keyword过滤，异常/空值不降级。

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root接续完成原PARTIAL/REPLACEMENT真实前端业务终态，证据保留不重建。
4. [x] 两参数页面事实、附件/培训内容及Root后续R28复核已核对，原失败历史保留。
5. [ ] 工程收尾：Git提交/推送和cleanup apply，缺当轮授权，不得标记completed。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C18，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Prepared Detail

- 参数子用例：`partial`（局部变更）和`replacement`（换版变更）；各自使用任务前缀说明并独立记录状态。
- 正式页面入口：`/dcc/controlled-file/browser?browserMode=storage` 的文件编号全域筛选找到任务自有主档；行内“追溯”进入所选版本的 `dcc-readonly-version-detail`，可读版本历史、文件附件、培训记录及申请历史。回到列表切换到“草稿”后点“填写审批申请”，在详情申请面板选“局部变更/换版变更”、送审草稿和说明，最后点 `dcc-revision-submit`。
- 执行脚本：`doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs`。脚本先读取公共`dispatch.json`，只有本例gate为`execute`且Root确认运行态与角色夹具后才加载Playwright；`prepare_only`直接返回并写`runtime-result.json`。
- 静态命令结果：`node --check doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs` -> PASS；准备命令 -> `PREPARED_WAITING`，`browserStarted=false`。
- 脚本当前执行范围是申请人登录、读取旧版可见附件/培训证据、选草稿并提交升版申请，结果明确为 `BLOCKED_PENDING_APPROVAL`；它不把“已提交”计作流程完成。后续还需Root确认角色矩阵后补齐真实审批/签名、培训记录、文控终态和页面清理步骤。
- 执行脚本要求每个子用例独立文件编号、唯一可选草稿标识及旧版可见培训记录名；执行gate放行后再从运行环境注入 `DCC18_*` 变量，不把账号密码写进文件。
- Root review约束：每个分支还必须注入旧版/草稿版本标签、正文标记、附件名称清单和旧版培训记录；正文与附件字段必须互斥并逐项匹配可见页面内容。trace或收尾失败保留错误并阻止完整成功；提交对象残留供Root review，当前阶段不清理。
- trace保护：登录重定向完成后才开启Playwright trace，登录密码和认证响应不进入trace；网络证据只保存脱敏URL、method、status。
- 版本证据：旧版和草稿正文分别打开真实受控预览并用PDF正文搜索命中独立标记，检查canvas尺寸和非白像素；附件、培训和版本字段逐项匹配且清单互斥，禁止只比较列表行数或画布存在。
- Root最新gate检查显示运行态已确认、角色夹具未确认；继续保持 `prepare_only`，不启动浏览器。已提交升版候选会单独重选版本并核验，不在本阶段清理。

## Execution Authorization Update

- 2026-10-16用户授权与execution-authorization.md取代此前prepare_only/等待Root及账号授权缺失限制。本例gate=execute、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture。
- 配置操作者从根AGENTS读取已授权测试身份；新建用户/角色/组织/项目/类型/业务文件均使用DCC241016C18前缀。业务角色独立登录，不使用管理员替代。
- fixture-manifest.json保留每项真实UI创建、登录、角色/菜单/数据核验及残留。缺口如实标明，不能假设fixture可用。
- trace只在登录完成后启动，snapshots=false避免HAR/网络认证头和响应内容；密码创建、签名时暂停。此选择依据本机Playwright 1.60 coreBundle.js中snapshots控制HarTracer启用的实现。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-18/root-integrated-result.json
- doc/tasks/20261016-dcc-e2e-18/latest-acceptance-review.json
- doc/tasks/20261016-dcc-e2e-18/business-checkpoint.json
- doc/tasks/20261016-dcc-e2e-18/business-checkpoint-before-final-status-20261019.json
- doc/tasks/20261016-dcc-e2e-18/checkpoint-before-final-status-20261019.json
- doc/tasks/20261016-dcc-e2e-18/verification-report-before-final-status-20261019.md
- doc/tasks/20261016-dcc-e2e-18/reconcile-final-evidence.py
- doc/tasks/20261016-dcc-e2e-18/closeout-preview-20261019.txt
- doc/tasks/20261016-dcc-e2e-18/actor-login.cjs
- doc/tasks/20261016-dcc-e2e-18/ui-fixture.cjs
- doc/tasks/20261016-dcc-e2e-18/ui-project.cjs
- doc/tasks/20261016-dcc-e2e-18/workflow-ui.cjs
- doc/tasks/20261016-dcc-e2e-18/lifecycle-ui.cjs
- doc/tasks/20261016-dcc-e2e-18/pdf-ui-evidence.cjs
- doc/tasks/20261016-dcc-e2e-18/attachment-version-ui.cjs
- doc/tasks/20261016-dcc-e2e-18/verify-replacement-bodies.cjs
- doc/tasks/20261016-dcc-e2e-18/finalize-root-evidence.cjs

- doc/tasks/20261016-dcc-e2e-18/assets/replacement-from-A2
- doc/tasks/20261016-dcc-e2e-18/attachment-version-ui.cjs
- doc/tasks/20261016-dcc-e2e-18/finalize-root-evidence.cjs
- doc/tasks/20261016-dcc-e2e-18/replacement-from-A2-fixture.json
- doc/tasks/20261016-dcc-e2e-18/root-integrated-result.json
- doc/tasks/20261016-dcc-e2e-18/run-2026-10-18T22-07-23-489Z
- doc/tasks/20261016-dcc-e2e-18/run-2026-10-19T00-12-14-051Z
- doc/tasks/20261016-dcc-e2e-18/verify-replacement-bodies.cjs

- doc/tasks/20261016-dcc-e2e-18/task.md
- doc/tasks/20261016-dcc-e2e-18/execution-log.md
- doc/tasks/20261016-dcc-e2e-18/verification-report.md
- doc/tasks/20261016-dcc-e2e-18/prepared.json
- doc/tasks/20261016-dcc-e2e-18/dcc-e2e-18-real-flow.cjs
- doc/tasks/20261016-dcc-e2e-18/runtime-result.json
- doc/tasks/20261016-dcc-e2e-18/real-ui-session.cjs
- doc/tasks/20261016-dcc-e2e-18/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-18/ui-execution.json
- doc/tasks/20261016-dcc-e2e-18/ui-commands.jsonl
- doc/tasks/20261016-dcc-e2e-18/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-18/checkpoint.json
- doc/tasks/20261016-dcc-e2e-18/runtime-pause-trace-check.json
