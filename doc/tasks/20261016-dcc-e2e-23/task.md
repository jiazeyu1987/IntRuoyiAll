# E2E-23 附件独立权限及历史培训记录

## Goal

独立验证E2E-23，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R3第四轮业务验收PASS：20 PASS/0 FAIL/0 BLOCKED；MP3/MP4按2026-10-21用户明确要求移出范围。cleanRun=false，首次脚本错误及原始证据保留。浏览器/runner已关闭；Git/暂存数据清理未授权，未标completed。

## BDD

- 2026-10-20 音视频普通附件缺口：准备期BDD/来源计划保留于media-attachments-prepare-20261020.md及同名JSON。随后Root明确单次执行及唯一源名纠正放行；仅新编号A/1完成三身份两媒体6PASS，原主分支/交互/迟到均未重跑。最终事实与脚本失败分类见media-attachments-20261020-verification.md，不把准备期状态当最新状态。

- 2026-10-19 r2补证：见repair-20261019-r2-bdd.md；六PASS不重跑，仅真实页面生成历史地址后由NoAccess新context直接打开，核对稳定拒绝、无正文/附件/下载，报告状态分离原业务PASS和附加补证。

- Given 分别准备可查看不可下载、可查看可下载、均无权限账号；不同版本有不同附件
- When 逐个打开/下载附件及培训记录；切历史版复查；在下载确认框取消，另测连续点击及离开页面
- Then 查看与下载分别校验；拒绝时不能取得文件；每版记录单及上传人/时间正确；取消不下载，旧页面迟到结果不落到新版本；重复点击不能绕过确认或权限。

## Milestones

1. [x] 用例参数拆分、精确页面入口及独立真实账号配置回读。
2. [x] A/1正式审批、培训、生效；当前三类权限及真实文件下载。
3. [x] 取消、连续点击、确认框离开、自然迟到响应离开/切工作稿分别验证。
4. [x] B/1正式换版、完整签名及独立培训、生效，A/1自动作废。
5. [x] 历史权限缺陷完成BDD/有效RED/最小GREEN及Root原UI复验，原失败证据保留。
6. [x] 原六组业务最新连续执行6PASS，证据b9f7b31a run及real-e2e-result.json；本轮不重跑。
   - [x] r2附加历史只读及正文直达拒绝2PASS：沿原UI地址、NoAccess新context、等待6500ms，独立于原六PASS。
7. [ ] 全部参数完成后Root review及任务数据收尾。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## Parameterized Subcases

- `23-view-only-current`: 可查看不可下载账号从 DCC 工作台进入受控浏览，按文件编号筛选当前版，进入追溯详情；断言附件表格、在线查看、培训记录文件名/上传人/时间可见，下载按钮缺失。
- `23-view-download-current`: 可查看可下载账号完成同一当前版路径；断言附件/培训记录可见，下载确认框取消、连续点击、离开页面各自记录零 download 事件；随后对普通附件和培训记录分别确认下载，保存真实文件并校验非空、建议文件名、格式签名及内容证据。
- `23-no-access-current`: NoAccess有QUERY名称权限，正式浏览可见元数据；实际内容浏览禁用，进入详情明确拒绝且无附件/正文/下载，不能沿用早期“无行”前提。
- `23-view-only-history` / `23-view-download-history`: 从当前只读详情的“版本历史 > 只读记录”进入指定历史版，按历史文件编号和版本标签唯一定位，断言附件名称、用途、上传人、上传时间、培训记录均属于历史版；下载权限按当前账号分别隔离。工作稿不能替代正式历史版。
- `23-no-access-history`: 原正式历史OBSOLETE筛选无可读目标行/正文/下载已PASS；另补真实UI产生的历史地址在NoAccess独立会话直达的稳定拒绝，不用管理员替代，不重跑已通过筛选。

## Formal Page And Data Preconditions

- 正式入口：本人登录后经文控中心受控浏览，使用存储目录/全域的文件编号筛选；目标行点击 `追溯`，详情路由由页面导航自然产生。只读历史入口为版本历史表格目标行 `只读记录`，精确记录不同文件ID及版本。
- 角色：必须由 Root 确认同一租户的三个独立真实账号：`view-only`（查看权限、无下载）、`view-download`（查看与下载权限）、`no-access`（无该文件查看权限）。缺少任一真实账号、密码或精确角色/权限投影即 BLOCKED；禁止用 admin 冒充。
- 数据：必须由 Root 确认任务自有前缀 `DCC241016C23` 的当前版与至少一个历史版，二者附件名称/正文或培训记录可辨识且不同；目标文件编号、附件名、培训记录名、历史版本标签通过环境变量提供，不写默认值。
- 页面事实：附件表格必须显示用途、上传人、上传时间、首次提交轮次、类型、大小；培训区域必须显示对应版本培训记录文件名，附件行还必须核对培训记录用途、上传人、上传时间；在线查看必须等待 `protected-viewer-shell` 的实际 PDF canvas 非空像素、图片自然尺寸、文本正文、媒体 readyState 或 OnlyOffice 内容节点，不以弹框标题代替正文渲染。
- 下载事实：每个可下载正例必须在确认后收到一个 Playwright download 事件，保存到任务目录并校验非空、文件格式签名及内容证据；取消、连续点击、离开页面三步各自记录零 download 事件。`DCC23_*_CONTENT_MARKER` 必须由 Root 从任务自有文件正文确认后注入；若格式无法由脚本验证，分支为 BLOCKED。

## Script And Command

- 脚本：`doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs`
- 静态语法：`node --check doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs`
- 准备检查：`node doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs`（当前 gate 非 execute 时只写 PREPARED_WAITING，不启动浏览器）
- 执行放行后才允许：`node doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs`

## 设计约束检查

- r2授权最小前端导航/拒绝展示修复及focused测试，其余只改本目录证据；不碰共享Query、13/22资产，不重启服务、不跑共享Maven/全量类型检查/构建、不改.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C23，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## 2026-10-16执行授权更新

- 用户“重启成功之后通知E2E线程让他们自己准备测试账号并继续E2E”及本轮“继续”授权本例通过真实前端创建带DCC241016C23前缀的账号、角色、菜单授权、专用项目/类型/版本数据；旧prepare_only与等待Root提供fixture要求已被取代。
- 配置使用AGENTS已授权管理员；业务身份分别登录，不授管理员、不改已有密码/共享角色/公共矩阵。账号创建和密码表单阶段关闭trace/截图；凭据只在进程内处理，证据不记录。
- 先落fixture-manifest.json记录创建状态、逐身份UI核验、真实缺口与清理残留；fixture未完成不能运行正式业务断言或写PASS。发现产品阻断不改生产，保存真实DOM与BDD交Root。

## Cleanup Keep
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/01-original-permissions.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/02-interactions.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/03-unexecuted-permissions.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/04-interactions-current-selectors.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/05-native-history.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/06-interactions-native.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/07-wav-permissions.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/08-wav-admission-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/09-history-filter-finalize.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/10-native-return-version-isolation.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/audit-trace.py
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T16-04-40-732Z-709e78bd/session.cjs


- doc/tasks/20261016-dcc-e2e-23/media-attachments-20261020-handoff.json
- doc/tasks/20261016-dcc-e2e-23/media-attachments-20261020-verification.md
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/media-session.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/01-upload-prepare.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/02-submit-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/03-reviewer-open.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/04-supervisor-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/05-submit-terminal-readonly.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/06-signoff-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/07-approval-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/08-signoff-inbox-readonly.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/09-actual-signoff-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/10-current-approval-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/11-document-control-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/12-training-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/13-approval-dialog-readonly.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/14-approval-sign-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/15-viewonly-open.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/16-viewonly-playback.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/17-viewdownload-open.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/18-viewdownload-playback.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/19-two-downloads-once.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/20-noaccess-own-session.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/21-viewonly-detail-readonly.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/22-viewonly-video-control.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/23-finalize-results.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/audit-media-trace.py
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-41-36-535Z-709e78bd/seal-media-evidence.py

- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-34-24-643Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-34-24-643Z-709e78bd/media-session.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T12-34-24-643Z-709e78bd/01-upload-prepare.cjs

- doc/tasks/20261016-dcc-e2e-23/media-attachments-prepare-20261020.md
- doc/tasks/20261016-dcc-e2e-23/media-attachments-prepare-20261020.json

- doc/tasks/20261016-dcc-e2e-23/r2-switch-only-preparation.json
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T08-27-03-769Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T08-27-03-769Z-709e78bd/switch-only.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T08-27-03-769Z-709e78bd/switch-verdict.cjs
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T08-27-03-769Z-709e78bd/switch-verdict.test.cjs

- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation.md
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-evidence.json
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-handoff.json
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-focused-tests.log
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-calibrated-tests.log
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-harness.cjs
- doc/tasks/20261016-dcc-e2e-23/r2-switch-investigation-analyze.py

- doc/tasks/20261016-dcc-e2e-23/rerun-20261020-r2-round4-handoff.json
- doc/tasks/20261016-dcc-e2e-23/rerun-20261020-r2-round4-plan.json
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T06-59-50-186Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T06-59-50-187Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T06-59-50-188Z-709e78bd
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T07-07-57-566Z-709e78bd

- doc/tasks/20261016-dcc-e2e-23/rerun-20261020-round4-handoff.json
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T03-05-32-408Z-ee67ec8f
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T03-09-30-110Z-ee67ec8f
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T03-16-24-257Z-ee67ec8f
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T03-20-27-432Z-ee67ec8f
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-20T03-26-24-660Z-ee67ec8f
- doc/tasks/20261016-dcc-e2e-23/integrated-current-B-recheck.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-current-dom.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-current-query-recheck.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-enter-current-trace.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-full-attachment-acceptance.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-global-performance-window.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-history-inspect.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-history-open.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-noaccess-history.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-noaccess-inspect.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-noaccess-open-current.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-noaccess-storage.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-noaccess-version-denials.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-six-finalize.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-storage-performance.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-storage-query-readback.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-trace-close.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-viewdownload-A.cjs
- doc/tasks/20261016-dcc-e2e-23/integrated-viewonly-A.cjs
- doc/tasks/20261016-dcc-e2e-23/open-existing-history.cjs
- doc/tasks/20261016-dcc-e2e-23/real-e2e-result-before-b9f7b31a.json
- doc/tasks/20261016-dcc-e2e-23/real-e2e-result-before-root-history-green.json
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-18T21-03-07-645Z-bb6d2c94
- doc/tasks/20261016-dcc-e2e-23/runs/2026-10-18T21-41-30-958Z-b9f7b31a
- doc/tasks/20261016-dcc-e2e-23/verify-existing-A-attachments.cjs

- doc/tasks/20261016-dcc-e2e-23/task.md
- doc/tasks/20261016-dcc-e2e-23/execution-log.md
- doc/tasks/20261016-dcc-e2e-23/verification-report.md
- doc/tasks/20261016-dcc-e2e-23/prepared.json
- doc/tasks/20261016-dcc-e2e-23/e2e-23-attachment-permission-real.e2e.cjs
- doc/tasks/20261016-dcc-e2e-23/ui-session.cjs
- doc/tasks/20261016-dcc-e2e-23/ui-helpers.cjs
- doc/tasks/20261016-dcc-e2e-23/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-23/pause-checkpoint-20261018.json
- doc/tasks/20261016-dcc-e2e-23/checkpoint.json
- doc/tasks/20261016-dcc-e2e-23/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-23/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-23/summarize-resumed-run.cjs
- doc/tasks/20261016-dcc-e2e-23/review-B-stage.cjs
- doc/tasks/20261016-dcc-e2e-23/open-reader-current.cjs
- doc/tasks/20261016-dcc-e2e-23/verify-B-attachments.cjs
- doc/tasks/20261016-dcc-e2e-23/real-e2e-result.json
- doc/tasks/20261016-dcc-e2e-23/root-workflow-resume-result.json
- doc/tasks/20261016-dcc-e2e-23/historical-permission-boundary.json
- doc/tasks/20261016-dcc-e2e-23/historical-read-bdd-red-design.md
- doc/tasks/20261016-dcc-e2e-23/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-23/repair-20261019-r2-bdd.md
- doc/tasks/20261016-dcc-e2e-23/repair-r2-verify-evidence.cjs

## rerun-20261020-r2-round4 BDD

- Given 原三个独立账号、当前B/1与历史A/1正式夹具，以及Root冻结环境。
- When 本人重新登录，通过真实受控浏览分别预览、确认下载、取消/连续点击/离开、自然迟到离开及A到B切换，并由无权账号打开本轮原生历史地址。
- Then 六主分支与八项交互独立给出PASS/FAIL/BLOCKED，首次错误保留，不把静态或旧PASS算本轮结果；音视频未执行为NOT_RUN。
- 本轮为真实验收重跑，无产品改动，不适用生产RED/GREEN；任务脚本以node --check及真实页面断言自检。首次真实产品FAIL不重试掩盖。

## R2 A到B迟到分支调查（2026-10-20）

- 目标：仅对首测BLOCKED和一次校准FAIL核对trace路由/响应先后、详情读取代次、附件卸载与runner等待点，区分已证明runner问题和未证明产品假设。
- 里程碑：原始证据顺序提取；当前代码调用与代次核对；现有定向单测；窄调查handoff。
- BDD（调查假设）：Given A下载请求未完成且用户通过原生历史入口选择B；When 路由身份切到B、A响应迟到、B详情仍在加载；Then 旧附件实例不得保存A下载，B读取不被旧代次覆盖，runner应分开记录routeChangedAt、responseAt和B附件就绪；未收到响应不得因关闭页面而推断迟到验证通过或产品请求取消。
- 约束：保留四轮原始结果；不重跑六主分支、不启动新E2E、不改权限或浮层、不构建/重启/提交推送/清理；生产修改须真实缺陷证据、窄BDD与当前源码最小RED，新增共享前端改动交Root排期。
- 当前状态：调查已完成并HOLD；确认runner时点判定/观察窗口问题，未证实产品缺陷，无共享前端改动。原第四轮仍FAIL/HOLD，首次BLOCKED与一次校准FAIL均不覆盖。调查入口r2-switch-investigation-handoff.json。

## A到B单分支新runner准备（等待E14稳定）

- 目标：仅准备原A/B对象和ViewDownload身份的单分支runner及独立证据路径，Root明确确认E14源diff稳定后才执行。
- BDD：Given A培训附件65真实确认下载已发出；When 从A版本历史原生切至B，旧请求响应迟到；Then 分别记录request、route、response headers、response finished/body、B附件ready时点；B附件正确且不含A、无原生下载。响应早于路由为BLOCKED；未观察到响应完成或B明确错误原样失败/阻塞；finally仍保存全部已观察事件与关闭结果。
- 验证：runner判断逻辑先RED再GREEN，node --check、无浏览器准备检查；显式60000ms ready窗口；不重跑六主分支，不改共享前端/测试和浮层。
- 启动门禁：没有Root本轮E14稳定确认与执行快照不得创建execution-release.json，不加载Playwright/启动浏览器。准备状态WAITING_ROOT_E14_STABLE，原第四轮FAIL/HOLD不变。
- 精确请求GET `/admin-api/dcc/controlled-files/2054545668044084233/attachments/65/download`，必须由真实确认框产生且query为 `nonControlledWarningConfirmed=true`。响应头记录status/content-type；分别保存B路由、响应头、body finished/error和B附件DOM ready时点，B ready上限60000ms。最终PASS判定只比较响应时点与B路由时点，不以后续DOM ready替代路由；原生download、HTTP/request/body失败或B ready断言失败为FAIL，响应早于路由或证据不全为BLOCKED。
- 执行release仅在Root确认后另行创建，需同run、固定确认语句/时间、E14稳定布尔值和Root核准的六个frontendSourceFiles SHA256；runner先核对int_qms、原runtime-proof后端PID/JAR SHA、监听PID/端口及前端指纹，再加载Playwright和解密账号。finally保留switch-manifest/result、事件JSONL、脱敏trace、DOM/截图及context/browser/runtime关闭证明。旧结果不可覆盖。
- runner RED/GREEN：`node --test .../switch-verdict.test.cjs` 首次7项中1项RED，正确暴露以B data-ready误替代route的旧判定；改为按routeAt比较后7/7 GREEN，覆盖迟到、早响应、body pending、错误页面、下载、业务错误及请求/body/HTTP失败。尚未加载Playwright或解密凭据。

## A到B单分支真实执行（2026-10-20）

- Given Root确认E14 diff稳定、int_qms及当前运行进程/源码身份一致，使用原A/B对象和ViewDownload身份。
- When 通过真实A培训附件65下载确认，随即通过版本历史原生导航切换B，并观察自然响应和B详情附件DOM，ready窗口60秒。
- Then 精确A请求08:39:32.563Z，B route 08:39:33.185Z，A响应HTTP 200于08:39:35.907Z且位于B，response body自然完成08:39:35.920Z，B两附件DOM ready于08:39:36.552Z；不读取response正文，Playwright download事件0，分支PASS。结果、时序、截图、DOM、脱敏trace及关闭证明位于独立run目录。
- 该分支完成前既有第四轮整体FAIL/HOLD和首测BLOCKED/校准FAIL未改写；六主PASS未重跑。E23未改生产文件、未执行build/Maven/服务操作/Git/DB/cleanup。
- Root六文件列表消息到达时本次单跑已启动并完成；执行放行快照实际固定33个dirty frontend src SHA（包含runtime-proof六文件及E14 handoff两个精确SHA）及runner SHA，原执行release原样保留。后到六文件核对另记root-six-source-review.json，未将事后记录冒充执行前release，不重跑。

## 2026-10-21 验收范围更新（当前口径）

用户明确：“不需要支持MP3,MP4”。MP3、MP4共8项移出验收范围（OUT_OF_SCOPE），不记PASS，也不再作为阻塞。保留原六权限、六交互、两迟到隔离、历史正文拒绝及筛选、WAV准入与三身份权限。本轮业务结论更新为20 PASS / 0 FAIL / 0 BLOCKED；cleanRun=false，12项首次脚本错误继续保留。本次仅更新文档及汇总，不重跑E2E、不改产品。原始运行result/trace/audit记录保持原事实。Git、数据及暂存上传清理仍未授权，任务行政收尾保持blocked。
