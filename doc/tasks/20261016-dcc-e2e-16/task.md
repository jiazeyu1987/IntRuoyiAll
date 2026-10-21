# E2E-16 延迟生效，需培训

## Goal

独立验证E2E-16，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 2026-10-20第三轮独立重跑收集完毕：新文件4318/A1，11 PASS / 0 FAIL / 1 BLOCKED（16-L）。T=10:50:52；10:52:16原U同项目308/目录49仍待生效/正文禁用。5628只读确认暂停且目标旧4228，无新4318自然日志；按Root监督未改状态/目标/注册，未建调度。browser/context已关闭，runner实际exit0，18份trace完整性/隐私检查PASS。ready_for_closeout后preview exit0但含历史及新证据删除候选，未apply；本轮禁止Git/清理，工程收尾交Root review。独立结果rerun-20261020-round3/result.json与rerun-20261020-round3-handoff.json；不启动下一轮。

## 本轮完整重跑BDD

Given 原U/S/L/C/A/D/O账号、本例独占项目/类别和新命名源文件；When U上传延迟+培训申请，S/L/C签名、A将负责人从U改O、D审核，O查看责任但不能替U上传，U从正式线下培训入口上传新记录，再执行缺日期/缺密码/过去日期拦截与未来时间签名；Then 培训前无可完成延迟确认，培训后任务归原U，记录留本申请/版本/U/时间，受控时间取本节点签名，严格T前日常正文拒绝，自然到期实际ACTIVE后同U当前生效目录正文稳定非空。调度若缺运行注册不擅自扩权删除/新建，具体BLOCKED并继续可执行分支。临时独占配置变化finally恢复/回读，保留每个失败。

blocked — R7业务验证16-L PASS，工程收尾交Root review，无Git/cleanup apply授权。已正式UI删除自有暂停5627并回读消失，一次新建5628（原同名/handler/全部字段、仅参数4261），自然日志48044实际生效1；原U18同308/49核ACTIVE，实际生效/启用/下发均2026-10-19 23:57:00。首见ACTIVE后23:58:22恢复5628原4228全字段并暂停；23:59:10同U18当前生效目录正文稳定非空、mask0。T17:30/签993不变，原T前及4228链PASS继承，8人签收不动。5627已被5628替换，不伪称ID未变。完整repair-20261019-r7-handoff.json；无需再排期或激活。

## BDD

- R7正式UI重新注册：Given 新会话确认5627暂停且原全字段与R6基线一致、Root批准只替换此任务，When UI删除旧5627并确认消失，再一次UI创建同名handler/cron/retry等全字段而参数精确4261的新ID，Then 自然cron激活4261后恢复新任务原4228全配置并暂停回读，原U18从项目308/目录49查看同A1稳定非空正文及实际启用时间；留存5627→新ID替换账，任何不确定提交先读不重建，handler冲突停报。

- 历史前置口径（已被下一条Root校正替代）：原先把独立签收义务纳入停止条件，未执行任何代签收；不再据此阻塞。
- Root校正BDD：Given 4228已激活且无培训/延迟确认任务，8人签收义务独立保留，When 仅复用5627的原handler/cron将参数由4228改4261并等待自然触发，Then 原U18在同项目308/目录49读4261/A1正文，原T/签名993不变；最后UI恢复原4228参数及全配置、暂停并逐项回读。
- Given 上传选择延迟并勾培训，A已把负责人改为O
- When D通过；按真实培训入口完成培训及必要记录，由U上传记录单；再由U执行延迟确认
- Then 培训完成前不提前派发可完成的延迟确认；完成后任务给原作者U，日期及签名规则同15；记录单保存申请、版本、上传人U及时间，负责人变更不改变上传职责。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 原全业务链真实前端执行至ACTIVE；作业暂停归还，业务证据/自有数据保留交Root收尾。
4. [ ] Root review脚本/trace/可见页面断言，逐分支给出实际结论。
5. [x] R5方案被Root否决；重复handler为现有约束，不确认为DCC缺陷，不实施全局改造。
6. [x] R6复用5627真实UI配置/自然等待/原U18核对/全部恢复暂停，保存实际阻塞证据。
7. [x] R7正式UI一次替换注册5627→5628，精确4261自然激活，同U18同目录正文正例及原全字段恢复暂停。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C16，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Prepared Assets

- 脚本：`doc/tasks/20261016-dcc-e2e-16/e2e-16-delayed-training-real.e2e.cjs`
- 脚本只从前端正式页面完成上传、批准、培训、培训记录上传和延迟确认；不调用 `fetch`、`axios`、`APIRequest`、SQL、mock 或浏览器存储注入。
- 角色链：U（申请/记录单上传/延迟确认）、A（批准并将负责人选为O）、O（本轮线下培训组织，不能替代U上传记录）、D（文控审核人）。D 的签名必须由审批中心真实待办完成，不能用接口补齐。
- 正式入口：`/dcc/controlled-file/upload`（受控文件提交）、`/approval-center/todo`（批准/文控待办）、`/dcc/controlled-file/workbench`（原生线下培训任务）、管理详情的“上传培训记录”、文件详情中的 `dcc-delayed-effective-confirmation` 面板（原作者U确认）。后续业务选择器尚无本例申请可做真实校准。
- 任务自有输入：文件编号、正文、普通附件、培训记录单均通过环境变量注入；脚本不提供默认路径，不记录密码、token、cookie或原始认证响应。
- Root review 约束：执行参数必须同时提供精确申请标识、文件编号、版本及原作者 U 的页面显示名；培训、记录单上传和延迟签名均在详情/任务页面复核该身份，负责人 O 仅用于批准时选择。
- Root review 约束：每个角色 context 关闭和 browser 关闭结果写入 `result.json`；任何关闭异常必须进入 `cleanupErrors` 与 `residual`，不得静默吞掉。

## Execution Authorization Update

- 来源：`doc/tasks/20261016-dcc-24-e2e-coordination/execution-authorization.md`及最新用户指令；替代此前prepare_only/等待Root放行要求。
- 运行态：int_qms 8061/48061，Root已核验后端PID24528及Jar SHA256 b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071。
- 本例可通过真实页面创建前缀DCC241016C16的账号、权限角色、组织及项目/类型/文件夹/矩阵，禁止修改共享角色、公共矩阵及已有基线密码。
- 真实业务前，修正准备脚本中旧培训链推断和身份/trace/清理不足；准确核对当前绑定流程的线下培训入口，不能把历史10分钟阅读链强加给本例。

## Current Execution Facts

- R7最新：4261独立16-L样本严格T前PASS继承，时间后自然生效及同U18/同308/49正文PASS。5627由UI明确删除、替代5628恢复原全部字段/4228并暂停；下方5627/4228历史自然日志只证明原训练主链，不代表当前任务ID。全例无条件PASS仍交Root结合原训练链与补充样本范围review，不自行扩大。

- 当前唯一原申请4228/A1、原BPM及原U18，S/L/C/A/D、O责任及U培训记录、原U延迟签名、三项错误参数均有逐项UI证据。
- 5627自然日志48043于19:26成功实际生效1，19:26:50重开确认暂停。U从原Project18/Folder18当前生效范围打开原A1，ACTIVE及正文非空，加载mask0、阅读统计正常。
- 原受控/延迟签名913时间18:01:50不变，计划18:04:48，实际生效与启用19:26:00；未回填日期，未重签/重提。
- 全例结论、唯一证据限制、作业/业务独立证据及browser closeout见root-review.json和verification-report.md。历史U2/未创建申请等仅适用于10月16日旧运行，不能覆盖本轮。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-16/scheduler-plan-20261020

- doc/tasks/20261016-dcc-e2e-16/rerun-20261020-round3
- doc/tasks/20261016-dcc-e2e-16/rerun-20261020-recipes.cjs
- doc/tasks/20261016-dcc-e2e-16/rerun-20261020-round3-handoff.json

- doc/tasks/20261016-dcc-e2e-16/replace-registration-recipes.cjs
- doc/tasks/20261016-dcc-e2e-16/run-20261019-r7-replacement
- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r7-handoff.json

- doc/tasks/20261016-dcc-e2e-16/reuse-activation-recipes.cjs
- doc/tasks/20261016-dcc-e2e-16/reuse-preflight-recipes.cjs
- doc/tasks/20261016-dcc-e2e-16/run-20261019-r6-readonly
- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r6-handoff.json
- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r5-handoff.json

- doc/tasks/20261016-dcc-e2e-16/run-20261019-r4-dedicated-job

- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r4-handoff.json

- doc/tasks/20261016-dcc-e2e-16/dedicated-job-diagnostic.cjs

- doc/tasks/20261016-dcc-e2e-16/dedicated-job-recipes.cjs

- doc/tasks/20261016-dcc-e2e-16/run-20261019-r3-exact-job

- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r3-handoff.json

- doc/tasks/20261016-dcc-e2e-16/run-20261019-r2-after-boundary

- doc/tasks/20261016-dcc-e2e-16/repair-20261019-r2-handoff.json

- doc/tasks/20261016-dcc-e2e-16/time-after-recipes.cjs

- doc/tasks/20261016-dcc-e2e-16/16-L-progress.json

- doc/tasks/20261016-dcc-e2e-16/16-L-activation-request.json

- doc/tasks/20261016-dcc-e2e-16/run-20261019-time-boundary

- doc/tasks/20261016-dcc-e2e-16/time-boundary-recipes.cjs

- doc/tasks/20261016-dcc-e2e-16/16-L-recovery-plan-20261019.md

- doc/tasks/20261016-dcc-e2e-16/root-review.json

- doc/tasks/20261016-dcc-e2e-16/activation-ui-recipes.cjs

- doc/tasks/20261016-dcc-e2e-16/run-20261018

- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101

- doc/tasks/20261016-dcc-e2e-16/activation-request.json

- doc/tasks/20261016-dcc-e2e-16/ui-recipes.cjs
- doc/tasks/20261016-dcc-e2e-16/check-trace-privacy.py
- doc/tasks/20261016-dcc-e2e-16/business-results.json
- doc/tasks/20261016-dcc-e2e-16/real-ui-driver.cjs
- doc/tasks/20261016-dcc-e2e-16/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-16/e2e-16-delayed-training-real.e2e.cjs
- doc/tasks/20261016-dcc-e2e-16/task.md
- doc/tasks/20261016-dcc-e2e-16/execution-log.md
- doc/tasks/20261016-dcc-e2e-16/verification-report.md
- doc/tasks/20261016-dcc-e2e-16/prepared.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-admin-category-list-system-error.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-admin-upload-candidates-system-error.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-cleanup-department.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-cleanup-post.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-cleanup-role.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-cleanup-user.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-fixture-department.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-fixture-u-role-menu.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-fixture-u-user-role.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-login-U.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-u-upload-candidates-system-error.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/16-user-create-observed.png
- doc/tasks/20261016-dcc-e2e-16/artifacts/admin-category-error.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/admin-upload-error.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/trace-privacy-check.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/u-upload-error.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-1.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-13-26-326Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-13-26-326Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-13-26-326Z-3.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-13-26-326Z-4.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-3.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-4.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-5.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-2026-10-15T22-56-25-619Z-6.zip
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-execution-2026-10-15T22-13-26-326Z.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-execution-2026-10-15T22-56-25-619Z.json
- doc/tasks/20261016-dcc-e2e-16/artifacts/ui-execution.json
- doc/tasks/20261016-dcc-e2e-16/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-16/.private/.gitignore
- doc/tasks/20261016-dcc-e2e-16/.private/credentials.dpapi
- doc/tasks/20261016-dcc-e2e-16/create-inputs.py
- doc/tasks/20261016-dcc-e2e-16/run-20261018/checkpoint.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-A.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-C.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-CFG.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-D.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-L.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-O.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/16-login-U.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/application-confirmation.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/application-ready.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/category-created.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/category-permissions.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/directory-verified.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/inbox-S.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/inbox-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/menu-inventory.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/project-completed.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/project-pending-review.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/root-observed-application.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/runtime-pause-current-S-page.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/runtime-pause-current-S-page.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-A.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-authorizations.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-C.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-D.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-L-wait-failure.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-L.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/signature-U.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/source-name-occupied-recovery.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/submission-uncertain-initiated.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/submission-uncertain-initiated.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/taxonomy-verified.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/trace-privacy-check.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-03-27-523Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-10.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-11.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-12.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-13.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-14.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-15.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-16.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-17.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-18.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-19.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-3.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-4.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-5.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-6.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-7.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-8.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T03-19-23-567Z-9.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-10.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-11.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-12.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-13.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-14.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-15.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-16.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-17.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-18.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-19.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-3.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-4.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-5.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-6.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-7.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-8.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-2026-10-18T05-03-34-649Z-9.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-execution-2026-10-18T03-03-27-523Z.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-execution-2026-10-18T03-19-23-567Z.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018/artifacts/ui-execution-2026-10-18T05-03-34-649Z.json

- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-A-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-attachment.pdf
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-attachment.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-C-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-D-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-L-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-O-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-S-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-source.pdf
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-source.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-training-record.pdf
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-training-record.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018/inputs/DCC241016C16-U-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/activation-terminal-ui.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-formal-facts-table.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-folder-body-stable.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-folder-body-stable.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-folder-body.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-folder-body.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-project-folder.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/active-original-project-folder.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/daily-all-before-activation.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/daily-all-before-activation.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/daily-current-effective-before-activation.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/daily-current-effective-before-activation.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/delayed-missing-date.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/delayed-missing-signature.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/delayed-past-time.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/delayed-post-response.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-A.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-A.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-C.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-C.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-D.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-D.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-L.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-L.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-S.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-U.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/inbox-U.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/enabled-exact-target-reopened.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/enabled-exact-target-reopened.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/natural-log-48043.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/natural-log-48043.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/paused-final-readback.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/paused-final-readback.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/preflight-paused-old-target.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/job5627/preflight-paused-old-target.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/leader-assigned.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/owner-o-approved.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/pending-activation-access.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/pending-activation-facts-table.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/pending-activation-facts.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/pending-activation-facts.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/pending-activation-viewport.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/resumed-S-original-task.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/S-sign-adjacent-network-order.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/signed-C.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/signed-D.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/signed-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-A.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-C.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-D.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-L.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-S.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/task-U.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/trace-privacy-check.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-context-U.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-O-after-view-matrix.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-O-after-view-matrix.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-O-before-record-current.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-O-before-record-current.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-record-audit.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/training-view-matrix-preview-timeout.png
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-10.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-11.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-12.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-13.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-14.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-15.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-3.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-4.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-5.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-6.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-7.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-8.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T07-46-05-820Z-9.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T10-03-59-502Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T11-12-18-618Z-1.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-2026-10-18T11-12-18-618Z-2.zip
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-execution-2026-10-18T07-46-05-820Z.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-execution-2026-10-18T10-03-59-502Z.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/artifacts/ui-execution-2026-10-18T11-12-18-618Z.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/checkpoint.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/minor-status-label-issue.json
- doc/tasks/20261016-dcc-e2e-16/run-20261018-151101/training-owner-entry-issue.json
- doc/tasks/20261016-dcc-e2e-16/closeout-preview.log
