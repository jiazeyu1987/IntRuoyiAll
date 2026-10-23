# E2E-19 目录与搜索

## Goal

独立验证E2E-19，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — R3本轮23业务PASS WITH OBSERVATIONS待Root复核；原runner BLOCKED/exit20保留，证据及关闭/preview完成，按无Git/apply边界HOLD。

## BDD

- 本轮前置 Given 原目录913890已存在且启用，配置账号无项目指派，仅具备查询、目录维护及目录绑定所需类别维护权限；When 本人独立登录按名称查询原目录并在类别页选择绑定；Then 正常回读原目录及可见字段，任何类别迁移缺口单独记录，不将公共待办告警当作目录业务失败。
- 暂停约束：runtime-pause.json.active=true，所有当前浏览器入口在启动前校验暂停标志；只允许离线脚本/文档检查。Root通知健康恢复后才可解除本例暂停标志。此次没有执行迁移。

- Given P1/P2有可区分且权限不同的文件
- When 按项目/文件夹浏览；按名称、编号搜索当前项目及授权项目；分别打开结果
- Then 列表和搜索显示正确来源项目、文件夹及版本状态；仅展示可访问范围；浏览按钮打开选中版本；无权限项目不能通过搜索结果泄露正文。

## 参数化子用例与正式入口

- `19-readerBoth-p1Main-browse`、`19-readerBoth-p1Sibling-browse`、`19-readerBoth-p2Main-browse`：具备P1/P2阅读权限的真实账号分别进入“DCC工作台 -> 受控浏览 -> 项目文件与引用 -> 项目文件夹”，选择项目和叶子文件夹，验证当前生效受控版本行的项目、来源文件夹、版本和状态，点击同一行“浏览”并在新窗口核对固定版本正文标识。
- `19-readerP1-p1Main-browse`、`19-readerP1-p1Sibling-browse`：仅P1权限账号重复P1两文件夹浏览；结果不得出现P2。
- 两个账号分别对“全局文件”和选定“项目文件夹”执行名称、编号搜索；查询输入必须使用任务自有的唯一名称/编号，监听页面自然 `/dcc/controlled-files/browser-page` 请求，按可见表格行断言来源项目、文件夹、显示版本和状态。
- `19-readerP1-denied-P2-name/number`：仅P1账号在“全局文件”按P2文件名称和编号搜索，必须无可见结果、无“浏览”按钮、无P2正文；不得拼接详情 URL。
- `19-current-project-name/number`：正式“当前项目”页签选择P1后无需选文件夹，按名称/编号返回P1两文件夹文件并逐行验证固定版本正文；已PASS。

正式菜单/路由依据：动态菜单应由真实账号登录后的可见“DCC工作台”进入，工作台按钮进入 `/dcc/controlled-file/browser`；页面正式组件为 `ProjectBrowserPanel.vue`，页签文案为“全局文件”“当前项目”“项目文件夹”，输入占位符为“项目名称或编码”“文件名称、编号或项目名称”，结果组件为 `ProjectFolderFileTable.vue`，正文路由由行内“浏览”自然打开 `/dcc/controlled-file/detail/:id?viewer=1`。

## 角色与数据前置

- 需要两个独立真实阅读账号：`readerBoth` 可访问P1/P2，`readerP1` 只可访问P1；两账号必须有真实页面可见显示名、精确角色名称与角色 code、`dcc:controlled-file:query` 权限投影和同一测试租户确认。`admin` 仅可作为环境可用性对照，不能替代任一角色。
- P1/P2项目、两个P1不同叶子文件夹、至少一个P2叶子文件夹、三个互不重复的 ACTIVE 文本受控版本必须由Root安排并通过正式页面准备，统一使用 `DCC241016C19` 前缀；三个正文需含可辨识且互不相同的正文标识，不能只凭元数据或API响应。
- 任务夹具清单应记录项目名称/编码、完整文件夹路径、文件名/编号、controlledFileId、显示版本、状态标签、正文标识和页面准备证据路径；不得记录密码、token、cookie或认证响应。
- 以上缺口已按最新授权主动准备。最终fixture-manifest.json记录完整真实页面准备、精确最小角色、三个ACTIVE A/1 PDF及正文；全部23项已验证。

## 脚本与运行约束

- 脚本：`doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs`；示例夹具合同：`fixture.example.json`；trace脱敏工具：`sanitize-trace.py`。
- 静态准备命令：`node --check doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs`、`python -X utf8 -m py_compile doc/tasks/20261016-dcc-e2e-19/sanitize-trace.py`、`node doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs --prepare-check`。
- 当前真实执行读取本例gate=execute、accountSetupGate=authorized、businessGate=execute_after_own_verified_fixture及fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST；不要求共享角色状态或Root再次批准。本例fixture-manifest.json必须准确证明自身完整可用，run-case.cjs只在内存加载凭据、显式提供8061/48061和本例manifest。
- 维护恢复后类别续跑命令：`node doc/tasks/20261016-dcc-e2e-19/prepare-config-actor.cjs --continue-categories`。启动前仍检查runtime-pause；复用已回读的配置账号/角色，独立登录后重新查询原目录，再继续类别步骤。此模式跳过管理员账号创建及角色授权，不重建目录；当前未执行。
- 真实脚本只读页面和打开正文，不通过fetch、axios、APIRequest、SQL、mock、localStorage造登录或业务数据；监听页面自然请求仅作HTTP归因，断言来自可见DOM、导航、输入值和正文。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后原23项真实前端执行全部PASS；任务自有数据按要求保留复核。
4. [x] Root已review真实runner、trace/截图/正文断言并接受23项业务，独立20项测试PASS。
5. [x] Root追加授权的admin只读前置盘点；不替代业务多账号验收。
6. [x] 自行通过真实页面创建两普通阅读角色、三新账号，完成独立登录/首次改密和角色/菜单回读。
7. [x] 本例P1/P2由独立项目审核人审批到COMPLETED；目录模板、六节点分类树已创建。
8. [x] 目录绑定问题独立真实复核，保存BDD/RED及GREEN阻塞；停用三个账号并记录保留对象。
9. [x] 根据Root只读根因更正目录结论；新建无指派配置账号及最小角色，真实回读原目录913890。
10. [x] 类别保存、UPLOAD权限纠正、完整真实上传/审批/签名及3份ACTIVE正文完成。
11. [x] 双账号最小查询角色、P1精确VIEW/P2隔离、原21可用参数全部真实PASS。
12. [x] 当前项目跨全部文件夹名称/编号两分支：唯一前端写者完成有效RED/GREEN，原23项真实UI全PASS。
13. [ ] Root review已接受；按最新要求保留全部对象/trace，禁止apply/Git/追加业务，正式工程收尾blocked。

## 追加只读前置范围

以下为上一轮只读授权的历史边界。最新授权允许本例任务账号/角色/菜单/业务fixture的真实页面准备和核验后执行，禁止直接API/SQL及共享基线修改仍适用。

- Given Root更新int_qms运行态，dispatch.E2E-19.preflightGate为read_only_preflight，runtimeVerified=true，主gate仍prepare_only且roleFixtureVerified=false。
- When 独立Playwright脚本在浏览器启动前校验分支、8061/48061、Root health证据和Jar SHA；在内存读取根AGENTS测试身份；真实登录后点击可见“文控中心 -> 受控浏览”，读取项目列表、文件夹、版本范围、表头和名称/编号搜索结果。
- Then 只记录页面可见事实及自然请求method/path/status，保存非敏感read-only-inventory.json、11张最终截图和脱敏登录后trace；不得解析业务响应JSON、直接API/SQL或创建/修改/上传/审批/签名/权限/清理业务数据；admin不替代U、双阅读人或拒绝账号。
- 当前最终运行：runs/read-only-2026-10-15T19-44-08-312Z，退出0表示盘点脚本完成，状态READ_ONLY_INVENTORY_WITH_BLOCKERS。20条当前页项目、5项目抽样、3项无文件夹/2项显示G44文档、候选行0；系统异常/超时不能解释为数据库无文件或验收PASS。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 本目录测试资产及Root明确分配的三个前端文件/贴近行为测试；此前R1624-07两文件已冻结。不扩大其它生产范围，不重启服务、不跑共享Maven/完整构建、不改共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C19，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-19/task.md
- doc/tasks/20261016-dcc-e2e-19/execution-log.md
- doc/tasks/20261016-dcc-e2e-19/verification-report.md
- doc/tasks/20261016-dcc-e2e-19/prepared.json
- doc/tasks/20261016-dcc-e2e-19/e2e-19.cjs
- doc/tasks/20261016-dcc-e2e-19/fixture.example.json
- doc/tasks/20261016-dcc-e2e-19/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-19/.gitignore
- doc/tasks/20261016-dcc-e2e-19/fixture-session.cjs
- doc/tasks/20261016-dcc-e2e-19/ui-runtime.cjs
- doc/tasks/20261016-dcc-e2e-19/prepare-projects.cjs
- doc/tasks/20261016-dcc-e2e-19/approve-projects.cjs
- doc/tasks/20261016-dcc-e2e-19/prepare-file-config.cjs
- doc/tasks/20261016-dcc-e2e-19/diagnose-directory.cjs
- doc/tasks/20261016-dcc-e2e-19/cleanup-task-users.cjs
- doc/tasks/20261016-dcc-e2e-19/run-case.cjs
- doc/tasks/20261016-dcc-e2e-19/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-19/business-results.json
- doc/tasks/20261016-dcc-e2e-19/product-blocker.md
- doc/tasks/20261016-dcc-e2e-19/verify-evidence.py
- doc/tasks/20261016-dcc-e2e-19/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-19/prepare-config-actor.cjs
- doc/tasks/20261016-dcc-e2e-19/runtime-pause.cjs
- doc/tasks/20261016-dcc-e2e-19/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-19/read-only-inventory.cjs
- doc/tasks/20261016-dcc-e2e-19/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/login-after-readonly-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/00-post-login.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/01-browser-entry.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/02-global-inventory.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/03-global-task-name-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/04-global-task-number-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/05-global-all-versions-empty-query.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/06-project-1-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/06-project-2-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/06-project-3-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/06-project-4-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-44-08-312Z/06-project-5-folders.png

- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T01-52-41-458Z/01-reviewer-role-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T01-52-41-458Z/admin-before-secret.zip
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T01-52-41-458Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T02-10-39-225Z/context-0.zip
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T02-10-39-225Z/context-1.zip
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T02-10-39-225Z/P1-completed.png
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T02-10-39-225Z/P2-completed.png
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-16T02-10-39-225Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/cleanup-users-2026-10-16T03-10-48-422Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/cleanup-users-2026-10-16T03-10-48-422Z/task-users-disabled.png
- doc/tasks/20261016-dcc-e2e-19/runs/cleanup-users-2026-10-16T03-10-48-422Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/directory-diagnostic-2026-10-16T03-04-31-595Z/01-maintenance-name-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/directory-diagnostic-2026-10-16T03-04-31-595Z/02-binding-directory-tree.png
- doc/tasks/20261016-dcc-e2e-19/runs/directory-diagnostic-2026-10-16T03-04-31-595Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/directory-diagnostic-2026-10-16T03-04-31-595Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-26-37-842Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-26-37-842Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-26-37-842Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-37-14-542Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-37-14-542Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-37-14-542Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-44-11-356Z/01-task-taxonomy.png
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-44-11-356Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-44-11-356Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/file-config-2026-10-16T02-44-11-356Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/01-project-form.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/02-task-folder-template.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/P1-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/P1-submitted.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/P2-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/P2-submitted.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-16T01-38-20-073Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-20-40-930Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-20-40-930Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-32-05-853Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-32-05-853Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-35-10-617Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-35-10-617Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/00-post-login.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/01-browser-entry.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/02-global-inventory.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/03-project-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/login-after-readonly-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T18-54-09-432Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/00-post-login.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/01-browser-entry.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/02-global-inventory.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/03-global-task-name-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/04-global-task-number-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/login-after-readonly-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-26-00-334Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/00-post-login.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/01-browser-entry.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/02-global-inventory.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/03-global-task-name-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/04-global-task-number-search.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/05-global-all-versions-empty-query.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/06-project-1-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/06-project-2-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/06-project-3-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/06-project-4-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/06-project-5-folders.png
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/login-after-readonly-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/read-only-2026-10-15T19-35-42-309Z/read-only-inventory.json
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T21-57-05-608Z/0-role-both-created.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T21-57-05-608Z/admin-1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/2-readerBoth-identity-role.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/3-readerBoth-directory.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/4-readerP1-directory.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/5-project-create-initial.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/7-project-create-entry.png
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/admin-0-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/admin-1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/admin-6-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/admin-8-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/readerBoth-9-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/setup-2026-10-15T22-40-54-138Z/readerP1-10-trace.zip

- doc/tasks/20261016-dcc-e2e-19/credentials.dpapi.json

- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/01-minimal-role-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/02-configuration-user-role.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/03-original-directory-found.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/04-original-directory-details.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/admin-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/admin-final.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/configuration.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-16T11-09-44-275Z/result.json
- doc/tasks/20261016-dcc-e2e-19/assets/DCC241016C19-NAME-p1Main.pdf
- doc/tasks/20261016-dcc-e2e-19/assets/DCC241016C19-NAME-p1Sibling.pdf
- doc/tasks/20261016-dcc-e2e-19/assets/DCC241016C19-NAME-p2Main.pdf
- doc/tasks/20261016-dcc-e2e-19/assets/p1Main-preview.png
- doc/tasks/20261016-dcc-e2e-19/assets/p1Sibling-preview.png
- doc/tasks/20261016-dcc-e2e-19/assets/p2Main-preview.png
- doc/tasks/20261016-dcc-e2e-19/assets/reviewer-signature.png
- doc/tasks/20261016-dcc-e2e-19/checkpoint.json
- doc/tasks/20261016-dcc-e2e-19/new-project-owner-blocker.json
- doc/tasks/20261016-dcc-e2e-19/owner-access-blocker.json
- doc/tasks/20261016-dcc-e2e-19/permission-scope-review.json
- doc/tasks/20261016-dcc-e2e-19/resume-session.cjs
- doc/tasks/20261016-dcc-e2e-19/root-test-handoff.json
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-18T05-07-43-147Z/P1-completed.png
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-18T05-07-43-147Z/P2-completed.png
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-18T05-07-43-147Z/context-0.zip
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-18T05-07-43-147Z/context-1.zip
- doc/tasks/20261016-dcc-e2e-19/runs/approve-projects-2026-10-18T05-07-43-147Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/03-original-directory-found.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/04-original-directory-details.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/05-T1-directory-selected.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/configuration.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-00-45-166Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/03-original-directory-found.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/04-original-directory-details.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/05-T1-directory-selected.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/configuration.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-07-27-926Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-10-13-376Z/03-original-directory-found.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-10-13-376Z/04-original-directory-details.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-10-13-376Z/05-T2-directory-selected.png
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-10-13-376Z/configuration.zip
- doc/tasks/20261016-dcc-e2e-19/runs/config-actor-2026-10-18T03-10-13-376Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/identity-2026-10-18T05-58-53-259Z/admin-identity.png
- doc/tasks/20261016-dcc-e2e-19/runs/identity-2026-10-18T05-58-53-259Z/identity-0.zip
- doc/tasks/20261016-dcc-e2e-19/runs/identity-2026-10-18T05-58-53-259Z/identity-1.zip
- doc/tasks/20261016-dcc-e2e-19/runs/identity-2026-10-18T05-58-53-259Z/readerBoth-identity.png
- doc/tasks/20261016-dcc-e2e-19/runs/identity-2026-10-18T05-58-53-259Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/01-project-form.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/02-task-folder-template.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/P1-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/error-stop.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T04-56-17-790Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/01-project-form.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/02-task-folder-template.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/P1-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/P1-submitted.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/P2-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/P2-submitted.png
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/projects-2026-10-18T05-04-16-121Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/0-no-task-category-before-retry.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/1-own-l2-planning-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/10-T1-T2-view-matrix-reader-scope.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/2-task-users-reactivated.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/3-existing-precise-actions-reused-no-menu-write.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/4-workflow-role-grants-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/5-reviewer-electronic-signature-authorized.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/6-reviewer-signature-image-enabled.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/7-p1Main-source-preview-not-ready.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/8-config-own-project-code-query-no-row.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/9-T1-view-matrix-saved.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/admin-before-reauth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/admin.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/config-before-reauth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/config.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T03-04-56-334Z/reviewer.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/0-persisted-T1-T2-view-matrix.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/1-new-project-preview-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/2-new-p1R2-preview-explicit-error.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/3-source-latest-explicit-state.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/config-before-final-readonly-login.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/config.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/matrix-final-trace-frame.jpeg
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/request-identity-only.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T05-07-53-376Z/reviewer.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/0-T1-no-UPLOAD-before.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/1-T1-UPLOAD-reopened-exact-author.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/2-T2-no-UPLOAD-before.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/3-T2-UPLOAD-reopened-exact-author.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/4-p1Main-preview-after-category-UPLOAD.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/5-p1Main-submission-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/6-p1Main-after-submit-state.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/7-p1Main-supervisor-task-detail.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/8-p1Main-supervisor-before-signature.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/9-maintenance-last-supervisor-action-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/admin.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/config.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/reviewer-before-supervisor-sign.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T06-11-16-023Z/reviewer.zip
- doc/tasks/20261016-dcc-e2e-19/source-preview-checkpoint.json
- doc/tasks/20261016-dcc-e2e-19/verify-login-identity.cjs
- doc/tasks/20261016-dcc-e2e-19/actual-signoff-timeout.json
- doc/tasks/20261016-dcc-e2e-19/assignment-timeout.json
- doc/tasks/20261016-dcc-e2e-19/current-project-search-gap.json
- doc/tasks/20261016-dcc-e2e-19/current-project-search-gap.md
- doc/tasks/20261016-dcc-e2e-19/remaining-fixture.cjs
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-global-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-browse-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerBoth-p2Main-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-denied-P2-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-denied-P2-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/19-readerP1-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/readerBoth-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/readerP1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T10-42-03-692Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/01-original-T1-upload-rule.png
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/02-T1-upload-only-rule-disabled.png
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/03-specific-category-denial-no-owner-message.png
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/04-original-upload-rule-restored-reopened.png
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/config.zip
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/category-message-2026-10-18T07-49-40-282Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/current-project-entry-2026-10-18T10-54-12-434Z/current-project-name-blocked-by-folder-requirement.png
- doc/tasks/20261016-dcc-e2e-19/runs/current-project-entry-2026-10-18T10-54-12-434Z/current-project-number-blocked-by-folder-requirement.png
- doc/tasks/20261016-dcc-e2e-19/runs/current-project-entry-2026-10-18T10-54-12-434Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/current-project-entry-2026-10-18T10-54-12-434Z/trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-44-18-984Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/0-original-matrix-task-and-supervisor-evidence.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/1-assignment-before-password.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/10-p1Main-doc-control-task-owner-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/11-p1Main-doc-control-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/12-p1Main-doc-control-signed-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/13-p1Main-active-browser-row.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/14-p1Sibling-preview-ready.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/15-p1Sibling-submission-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/16-p1Sibling-submission-complete.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/17-p2Main-preview-ready.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/18-p2Main-submission-confirmation.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/19-p2Main-submission-complete.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/2-assignment-single-submit-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/20-p1Sibling-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/21-p1Sibling-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/22-p1Sibling-32fabf66-cad3-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/23-p1Sibling-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/24-p1Sibling-assignment-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/25-p1Sibling-assignment-timeout-before-refresh.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/26-p1Sibling-assignment-readonly-refresh-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/27-p1Sibling-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/28-p1Sibling-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/29-p1Sibling-signoff-timeout-original.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/3-assignment-saved-reopened-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/30-p1Sibling-post-timeout-current-todo.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/31-p1Sibling-readback-still-unavailable-no-repeat.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/32-actual-project-edit-menu-permission.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/4-p1Main-actual-signoff-before-password.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/5-actual-signoff-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/6-p1Main-approval-task-visible.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/7-p1Main-approval-owner-confirmed-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/8-approval-single-attempt-result.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/9-p1Main-approved-owner-confirmed.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/admin.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/author-before-project-grant-login.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/p1Main-active-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/p1Sibling-023aeeb7-cad4-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/p1Sibling-32fabf66-cad3-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/p1Sibling-assignment-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/p1Sibling-assignment-timeout-original.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-approval-scene-check.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-before-actual-signoff.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-before-approval-sign.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-before-assignment-sign.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-before-doc-control-sign.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer-before-remaining-login.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T07-52-16-242Z/reviewer.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/0-fresh-remaining-two-todos.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/1-p1Sibling-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/10-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/11-p2Main-matrix-action-state.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/12-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/13-p2Main-assignment-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/14-p2Main-assignment-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/15-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/16-p2Main-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/17-p2Main-6e97646c-cadb-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/18-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/19-p2Main-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/2-p1Sibling-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/20-p2Main-3b4501a3-cadc-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/21-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/22-p2Main-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/23-p2Main-3fe11f26-cadc-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/24-P1-owner-original-rule.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/25-P1-owner-and-precise-reader-VIEW-reopened.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/26-P2-owner-only-no-readerP1-rule.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/27-all-three-active-correct-source-rows.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/28-ReadBoth-exact-baseline-reopened.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/29-DirectoryConfig-exact-baseline-reopened.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/3-p1Sibling-7249cce7-cad5-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/30-readerP1-query-only-role-readback.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/31-Both-actual-role-binding-readonly.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/32-P1-actual-role-binding-readonly.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/33-readerP1-only-own-two-project-files.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/4-p1Sibling-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/5-p1Sibling-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/6-p1Sibling-0abf6596-cadb-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/7-p2Main-current-task.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/8-p2Main-approval-before-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/9-p2Main-3e433ca7-cad3-11f1-a035-b082e25ec548-after-sign.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/admin.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Main-ACTIVE-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Main-readerP1-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Sibling-0abf6596-cadb-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Sibling-7249cce7-cad5-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Sibling-ACTIVE-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p1Sibling-readerP1-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-3b4501a3-cadc-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-3e433ca7-cad3-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-3fe11f26-cadc-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-6e97646c-cadb-11f1-a035-b082e25ec548-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-ACTIVE-fixed-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/p2Main-assignment-before-password.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/readerBoth.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/readerP1.zip
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/result.json
- doc/tasks/20261016-dcc-e2e-19/runs/resume-2026-10-18T09-57-04-778Z/reviewer.zip
- doc/tasks/20261016-dcc-e2e-19/verify-category-upload-message.cjs
- doc/tasks/20261016-dcc-e2e-19/verify-current-project-entry.cjs

- doc/tasks/20261016-dcc-e2e-19/current-project-adjacent-exact.log
- doc/tasks/20261016-dcc-e2e-19/current-project-behavior-green.json
- doc/tasks/20261016-dcc-e2e-19/current-project-behavior-green.log
- doc/tasks/20261016-dcc-e2e-19/current-project-behavior-red.json
- doc/tasks/20261016-dcc-e2e-19/current-project-behavior-red.log
- doc/tasks/20261016-dcc-e2e-19/project-search-handoff.json
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-current-project-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-global-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-browse-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerBoth-p2Main-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-denied-P2-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-denied-P2-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/19-readerP1-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/readerBoth-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/readerP1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/2026-10-18T12-05-58-909Z/result.json

- doc/tasks/20261016-dcc-e2e-19/closeout-preview-final.log

- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-green.json
- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-green.log
- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-red.json
- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-red.log
- doc/tasks/20261016-dcc-e2e-19/repair-20261019-r2-cleanup-preview.log

- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-current-project-name-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-current-project-number-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-global-name-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-global-number-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Sibling-browse-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Sibling-name-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p2Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p2Main-browse-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p2Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p2Main-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/19-readerBoth-p2Main-number-stop.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/closure-receipt.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/handoff.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/readerBoth-trace.zip
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/result.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/runner-exit-receipt.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-current-project-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-global-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p2Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerBoth-p2Main-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-denied-P2-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-denied-P2-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/19-readerP1-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/readerBoth-trace.zip
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/readerP1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/result.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/waiting-calibration-and-remaining/runner-exit-receipt.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r2-round4/cleanup-preview.log

- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-handoff.json

- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/active-process-snapshot.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/closure-receipt.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/handoff.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/network-diagnostics.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/preflight.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/prelaunch-original-gate-error.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/runner-exit-receipt.json
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-current-project-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-global-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-browse-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-name-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerBoth-p2Main-number-p2Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-denied-P2-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-denied-P2-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-global-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-browse-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-name-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Main-number-p1Main-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-browse-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-browse-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-name-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-name-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-number-list.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/19-readerP1-p1Sibling-number-p1Sibling-body.png
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/readerBoth-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/readerP1-trace.zip
- doc/tasks/20261016-dcc-e2e-19/runs/rerun-20261020-r3-round4/result.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/evidence-validation.json
- doc/tasks/20261016-dcc-e2e-19/rerun-20261020-r3-round4/cleanup-preview.log

## R1624-07 类别上传拒绝诊断 BDD / TDD

- Given 启用且生命周期有效的类别10，用户99的类别UPLOAD权限明确拒绝。
- When 调用真实DccControlledFileUploadServiceImpl.uploadPreviewFile（SOURCE、有效NEW_UPLOAD上下文）。
- Then 保留错误码1080000341，文案为“当前用户没有该文件类别的上传权限”；不读取原件、不调用fileService或uploadTicketService创建资源。项目无权分支保持原语义。
- RED：先只补DccControlledFileUploadApiTest方法，由Root独占Maven执行；未运行不写PASS/有效RED。当前生产源码尚未修改。


## 当前项目搜索唯一前端范围 BDD / TDD

- Root分配三个生产文件及贴近行为测试；先仅补RED，不动backend/SQL/workflow/Query。
- Given P1已选且两个文件夹各有ACTIVE文件，P2也有文件。When State.search(project)及真实adapter执行项目查询，无单文件夹；Then GLOBAL带精确dccProjectCodeId、无folder，接受同项目不同folder、拒异项目；未选项目明确拒绝且零请求，原global/directory身份不变。
- 前端行为测试执行真实transpile模块，不把源码字符串合同当行为GREEN。

- 最终GREEN/REGRESSION：current-project-behavior-green.json记录20PASS、相邻定向1PASS；runs/2026-10-18T12-05-58-909Z/result.json记录原23项全PASS。

## Root最终接受（2026-10-18）

Root已接受原23项业务PASS：阅读真实runner确认无API代替业务，复核当前项目名称/编号在同一项目返回两个文件夹及两个正文marker，查看两行截图，并独立重跑20项定向测试PASS；三前端文件均在授权范围。较大相邻测试16项12PASS/4FAIL保持原始结果：4项是旧harness缺fileTypeTaxonomies依赖，未归类为生产回归，也未修改或吞掉失败。

Root通知运行态已更新为后端PID34440及新hash；本次委派未给出新hash全文，不推断、不覆盖原23项验收记录的实际PID25996/SHA bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f。本例复用已发生的真实事实和前端修复证据，不追加无理由的23项重跑。

本次仅同步本例证据记录。已核对既有cleanup preview（exit0，执行时ready_for_closeout）：79份ZIP、fixture-manifest、受本机保护且不供提交共享的凭据及最新23项全部结果/截图均在keep；delete仅任务内pycache，实际删除0。全部4账号、27条任务对象记录及必要trace保留，不执行apply、Git或追加业务。业务已接受；工程状态按无提交推送边界保持blocked，不能写completed。


## 20261019-r2 旧相邻测试夹具修复

- 目标/范围：Root repair-dispatch-20261019-r2.md分配E2E-19，核对DetailApplicationPanel真实导入及申请/换版/异步选择契约，仅维护tests/unit/dcc-detail-closure.test.cjs测试夹具，不改生产/共享Query。
- BDD：Given 原相邻回归16项，四项通过application()挂载真实DetailApplicationPanel，组件新增fileTypeTaxonomies类型候选依赖而旧harness未提供；When 执行完整该16项测试；Then 先准确记录四项模块解析RED，再按真实调用边界补齐测试依赖，原申请身份、版本、属性、异步竞态及失败清空断言全部保留并通过。
- 里程碑：1 RED日志；2 调用契约核对；3 最小测试夹具修复；4 全16项定向GREEN；5 repair-20261019-r2-handoff.json及报告纠正、cleanup preview。
- Expected Verification：node --test tests/unit/dcc-detail-closure.test.cjs；node --check tests/unit/dcc-detail-closure.test.cjs；定向git diff --check。测试专用依赖可用轻量Node执行，无Maven、全量类型检查、后端切包或新页面E2E需求。
- 约束：独立前端测试由本例执行，不启子Agent，不占用Maven/类型检查窗口，不启动服务/业务浏览器，不提交推送，不动其它资产。所有已通过业务结果、账号、fixture和trace继续保留。明确保留RED历史，GREEN后纠正当前结论，不吞异常或放宽断言。

## 20261019-r2 相邻测试修复最终结果

本轮只修复旧相邻16项测试harness。真实RED：`node --test tests/unit/dcc-detail-closure.test.cjs`，exit1，12PASS/4FAIL；四项均在真实DetailApplicationPanel加载时缺fileTypeTaxonomies模块。核对正式源码：getFileTypeTaxonomyUploadOptions仅由beginTypeChange主动类型变更动作调用，普通open/selectIteration/selectIntent不应读取候选类型。

最小修复仅在该测试的application()依赖夹具增加模块/函数；函数被意外调用时显式报错，不返回空候选或默认成功。所有原测试正文与断言已与修改前HEAD逐字核对保持一致，未改生产代码或共享Query。GREEN/REGRESSION：`node --test --test-reporter=tap tests/unit/dcc-detail-closure.test.cjs`，exit0，16PASS/0FAIL/0SKIP；node --check及定向git diff --check均exit0。详见repair-20261019-r2-red/green.log与json及repair-20261019-r2-handoff.json。

纠正当前报告：4项旧harness依赖失败已经修复，当前该相邻回归16项全PASS；下文“保留4失败”仅描述2026-10-18的历史结果，原始RED证据保留，不覆盖/吞掉。原目录/搜索23项业务仍为Root接受的23PASS，本轮按指令不重跑；无新的页面E2E，也不以旧workflow-v6声称其它后端新修复通过。本轮不需要运行态切包、Maven或全量类型检查窗口，不启动浏览器/服务，不修改任何账号或fixture；27个对象记录、4账号、79份trace继续保留。未提交推送，正式工程收尾仍受无Git/apply授权边界限制。


20261019-r2里程碑1—5均完成；cleanup preview exit0，全部修复证据及79trace在keep，仅任务pycache在delete，未apply。


## 2026-10-20 第四轮真实页面重跑

- 本次全23适用分支：23 PASS、0 FAIL、0 BLOCKED；执行文件 `runs/2026-10-20T03-06-07-879Z/result.json`；独立交接 `rerun-20261020-handoff.json`。没有省略分支，不复用历史PASS替代本次运行。
- 两个本例真实读者账号均在新的独立context通过页面登录、个人中心昵称核对；三份现存ACTIVE A/1版本逐项按目录浏览/搜索、授权全局搜索、P2无权搜索和当前项目跨文件夹搜索，并打开固定版本核对各自正文标识。无业务fixture修改。
- 页面/console错误0，749个自然业务请求HTTP错误0；2份独立脱敏trace及52张截图留在新run目录。runner exit0、contexts/browser关闭true；cleanup未执行，因阅读可能形成正式审计记录。
- 现场runtime为8061/PID20976、48061/PID27908、health UP、Jar SHA256 ee67ec8f...ba141566ab，与`20261020-qms-e2e24-runtime-update/runtime-proof.json`相符。24总dispatch backend PID/hash为旧值；本run结果保留原dispatch快照并以现场已核证运行态补充，不改总dispatch。
- 本轮唯一脚本校准：分支失败保留证据后继续独立只读分支；无产品代码修改，断言未放宽。

## rerun-20261020-r2-round4 执行合同

- 目标：原23分支全部重新登录/执行，目录15、授权全局4、P2拒绝2、当前项目2，沿用本例三份正式ACTIVE A/1及独立真实阅读账号。
- Given Root正式EXECUTE、int_qms冻结8061/PID20976及48061/PID41508，Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147；When 两账号独立context从真实登录菜单进入，逐项浏览、名称/编号搜索并打开固定版本；Then 页面显示精确来源/版本/正文，P2无权查询无结果，各分支独立计PASS/FAIL/BLOCKED/NOT_RUN。
- 里程碑：运行态复核→新路径/输入及脚本语法校验→23项新真实页面→逐分支证据/隐私完整性→context/browser/runner关闭回执和handoff后HOLD。
- 预期验证：原e2e-19.cjs实际执行，保留首次错误、继续其它独立可执行分支；PDF非空canvas及唯一marker仍为正式断言；不增加源件下载、格式、生命周期重建等门槛。
- 约束：仅任务脚本命名参数校准和任务证据；不改产品/SQL/服务/Git、不Maven/全量类型检查、不子Agent/其它轮。API响应仅path/method/status观察，业务oracle全为DOM。无计划fixture变更；如确有自有权限临时变更，需原值和finally恢复回读。


## 20261020-r2-round4 最新结果（待Root复核，HOLD）

原23项本轮真实UI覆盖齐全：首遍6 PASS、7次5秒等待FAIL、10项因trace路径守卫中断未执行；原结果/错误完整保留。校准任务自有输出目录守卫与expect等待至30秒后，原7项同对象复核及其余10项全部PASS；合并业务23 PASS、0遗漏，不使用历史PASS。所有来源行、固定A/1、PDF非空canvas和唯一正文标识、P2拒绝断言保持。

补跑runner仍BLOCKED/exit20：readerBoth控制台超时由trace定位到外部图标https://api.unisvg.com/ep.json?icons=files；非业务断言失败。保留原runner状态，不声称整轮无异常。首遍路径守卫中断为测试资产适配错误，已修正并完成脱敏；首遍stopReason不改写。

冻结8061/PID20976、48061/PID41508、Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147及6份前端hash收尾仍相符；结果中总dispatch旧PID/hash只作原始快照。新3份脱敏trace、全部截图/逐项结果、5凭据/84ZIP扫描PASS（0明文、0损坏）见rerun-20261020-r2-round4/handoff.json。浏览器已关闭，自有runner及已知进程残留0；未改产品/权限/业务fixture，4账号及27对象继续保留。未Git/apply/重启/新一轮，工程未completed。



20261020-r2-round4收尾：已先标记ready_for_closeout，cleanup preview实际exit0；本轮全部70项新增资产及前轮handoff均keep，delete仅原任务pycache，实际删除0。未apply/Git，工程状态blocked；业务覆盖23 PASS，原首遍7 FAIL及补跑runner BLOCKED/exit20不改写。交接rerun-20261020-r2-round4/handoff.json，随后HOLD，不追加新一轮。


## 20261020-r2-round4 Root接受结果

Root已审核本轮handoff、23条逐项结果、两次原始attempt、closure-receipt及3份当前trace CRC，例级接受 **BUSINESS PASS WITH OBSERVATIONS**：23项业务均在本轮完成；据Root通知，权威台账已记PASS。本例保持HOLD。

观察项保留：api.unisvg.com外部图标超时单列；原runner BLOCKED/exit20及首遍7项等待失败不改写。旧environment字段与实际冻结运行态（8061/PID20976、48061/PID41508，Jar SHA709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147）的差异披露继续保留。

本次仅补充本例三份文档；不启浏览器、不重跑、不改原结果或共享台账，不执行Git、cleanup apply或服务操作。结构核对通过；工程状态沿用blocked，业务接受不等同于工程completed。



## R3第四轮执行合同（2026-10-20）

Given Root已正式放行R3第四轮E19、最新模型gpt-6.1-sol / low、唯一rerun-r3-runtime-proof.json与2095来源快照，When 两阅读账号fresh context真实登录、个人中心身份及实际可见菜单/访问范围核验，完整重跑目录15、授权全局4、拒绝P2两项、当前项目两项，Then 逐分支留下本轮来源项目/文件夹/版本状态、固定A/1非空PDF及唯一正文marker，禁止旧PASS替代。本例无新增附加格式或写入合同。

复用本例P1/P2项目306/307、目录45/46/47、ACTIVE固定版本2054545668044084229、2054545668044084237、2054545668044084238；读取合同不新建业务对象，本轮独立证据runs/rerun-20261020-r3-round4，正式handoff置rerun-20261020-r3-round4/handoff.json。原R2及其它例UNKNOWN保持独立。

里程碑：规则/BDD与快照→原23真实UI→首次错误/网络元信息/脱敏CRC→关闭回执及逐分支handoff→HOLD。只验证搜集，不生产代码/DB/API业务动作/响应JSON oracle/构建/重启/Git写/子Agent/共享文件变更。角色精确名称以原UI分配证据为来源，本轮fresh身份、菜单及P1/P2隔离重新核验，不虚称个人中心展示角色字段。长加载先等待真实loading；所有原断言保留，未知写动作禁止重放。本例只读，无提交/签名动作。


## R3第四轮E19本轮结果（2026-10-21，待Root复核，HOLD）

完整23项真实UI全部PASS（目录15、授权全局4、P2拒绝2、当前项目2），0 FAIL/0 BLOCKED/0 NOT_RUN/0正文未ready；两账号各自fresh context登录并核验昵称、正式菜单及实际访问范围。复用本例三个既有ACTIVE A/1对象，逐项来源/版本及PDF非空canvas、正文marker重新验证，未使用旧PASS。精确角色名称来源为原UI分配证据；当前个人中心无角色字段，未虚称重新回读角色分配。

原runner BLOCKED/exit20保留：两个console超时均由当前trace定位api.unisvg.com外部图标。无pageError或DCC HTTP错误。27条未完成/中止自然请求快照另列元信息，未当作30秒业务超时；业务DOM全部达标。初次启动旧businessGate匹配错误未启浏览器，原错误单独保留，仅校准本例R3授权识别。未重跑业务或修改产品。

R3 proof及2095来源快照/Jar收尾仍匹配；8061/PID20976、48061/PID41508。52张截图、两当前脱敏trace及network-diagnostics、逐分支对象/账号/版本和关闭回执见rerun-20261020-r3-round4/handoff.json。所有context/browser/runner关闭，残留进程0；本例4账号、27对象记录保留，无权限/fixture写入、无API/DB业务动作。旧环境字段差异及其它例UNKNOWN继续保留。无Git/apply/重启/追加轮次。


R3收尾：当前两trace CRC及5份现有凭据扫描PASS（0损坏、0明文命中）；52截图。已先ready_for_closeout后cleanup preview实际exit0，本轮全部资产keep，delete仅原pycache，实际删除0；未apply/Git。业务BUSINESS PASS WITH OBSERVATIONS待Root复核，原runner BLOCKED/exit20不改写；工程blocked且HOLD，验证交接完成即停止，不循环收尾。
