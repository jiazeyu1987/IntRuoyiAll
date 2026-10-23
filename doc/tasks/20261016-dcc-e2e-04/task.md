# E2E-04 会签与下发名单独立

## Goal

独立验证E2E-04，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked

2026-10-20第一轮本例业务12PASS/0FAIL/0BLOCKED；独立结果rerun-20261020-round1/rerun-result.json。新文件2054545668044084287/DCC241020C04R1/A/1已单次提交，BPM31450180-cc28-11f1-a8a5-b082e25ec548，fresh只读刷新后待直属主管审核，冻结主管S。全部浏览器/runner结束；保留本轮脚本校准原FAIL以及全部历史证据。仅作Root review和收集结果，不执行下一轮。Git当轮禁止，工程blocked；Root明确只收集结果，未运行cleanup apply，不标completed。

历史业务PASS及原2054545668044084183/ECHO1证据保持原样。

## BDD

- Given T1默认两个会签部门
- When 增删会签部门，再单独增删下发部门；在新申请中重新选T1
- Then 会签部门均可删除；至少保留一个才可提交；两名单分别保存、互不串改；本次调整不修改公共矩阵，新申请仍取原默认配置。

## Subcases and page contract

唯一业务定义为 `doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md` 的 E2E-04；本轮覆盖如下单独记录的参数化断言：

| ID | 独立页面动作及通过依据 |
|---|---|
| 04-01 | 选正式T1文件类型后，页面两份当前名单均精确显示其两个正式默认会签部门；批准人展示值记作新申请基线。 |
| 04-02a | 删除默认会签部门D1；页面剩D2，下发名单仍为D1、D2。 |
| 04-02b | 从初始的两默认项中删除D2；页面仍有D1。两个默认项均能被单独删除。 |
| 04-03 | 删除最后一个会签部门；页面显示“请至少选择一个会签部门”，提交不能进入确认或发出正式提交请求；下发名单不变。 |
| 04-04 | 只向会签增加正式部门Ds，留下D2、Ds；下发仍为原D1、D2，路线页内预检需就绪。 |
| 04-05a | 只向下发增加不同部门Dd；会签仍为D2、Ds。 |
| 04-05b | 单独删除下发默认D1、D2，确认空下发名单合法且会签保持D2、Ds；再加回Dd。 |
| 04-06 | 选择受控文件、项目、项目文件夹、T1及任务自有编号/文件后保存申请工作稿；在工作稿页面打开“准备本次申请”，保存本次审批工作草稿。 |
| 04-07 | 从页面移除Ds，保存本次草稿；刷新页面重开“本次文件申请”，可见会签D2、下发Dd。 |
| 04-08 | 只把D1加入下发，保存、刷新并重开；可见会签D2，下发D1、Dd，证明两份名单双向不串改。 |
| 04-09 | 通过真实页面另开新申请选择相同T1；可见两份默认名单仍为D1、D2，批准人可见值与04-01一致，且本例未改公共矩阵。 |
| 04-10 | 在原任务自有工作稿真实页面核对二次确认的会签D2和下发D1、Dd，提交；页面可见当前阶段为正式主管审核。结束后保留该受理记录给Root review，不以提交受理宣称审批完成。 |

正式入口经本轮作者真实登录更正：动态菜单“文控中心→文件上传”直接进入 `/dcc/controlled-file/upload`，作者页面实际可见 `dcc-upload-single-page-workbench`；DCC工作台是其他页面上的可选可见入口，不要求它成为动态菜单。脚本已改为读取本例 `uploadMenuPath`，不手工直达功能URL替代菜单。上传页两个选择器为 `dcc-upload-signoff-departments` / `dcc-upload-distribution-departments`；工作稿页 `DetailApplicationPanel.vue` 的“准备本次申请”“保存本次申请草稿”“核对并提交初始申请”仍待schema恢复后的真实执行。

作者为 `DCC241016C04U`，角色code `DCC241016C04AUTHOR`；主管S、部门负责人L1/L2/LS/LD、批准人A、文控D均为本例前缀独立账号。tenant在登录页确认为“芋道源码”。admin只作配置操作者；账号/角色/菜单/组织/分类全部由页面创建与核验，完整清单见fixture-manifest.json。作者已真实从菜单进入上传页；审批路线和T1默认两部门仍因schema错误无法配置和核验，不能填写已就绪。

清理范围只限此编号产生的源文件上传临时物、受控工作稿及本次工作申请；若已有正式主管待办，必须使用正式页面撤回/作废/删除入口，保留trace给Root复核后再按Root review计划收尾。不得改变共享T1矩阵或共用类别/组织/用户配置。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置；缺口登记于prepared.json。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 原工作稿恢复及Root授权直接缺陷回归完成，12参数分支真实PASS。
4. [ ] Root review最终跨轮次证据、原单次提交及最小回归稿；统一工程收尾。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录；自然请求仅记录DCC路径、method和HTTP status。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全；浏览器/账号验证未放行前只填准备状态。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C04，不改其它用例对象或公共矩阵；专用对象准备已有用户授权，通过真实UI执行。
- 本目录由本例线程维护，Root独占协调目录。未改共享dispatch；阻塞证据完成后结束当前turn供Root读取，不轮询。

## 2026-10-16 UI fixture preparation BDD

- Given Root已核验int_qms运行Jar SHA b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071，独立账户准备已授权。
- When 管理员仅以配置身份从真实前端建立DCC241016C04自有账号、精确业务角色与最小菜单/按钮权限、组织及P1/T1，分别用新的context登录每个业务账号。
- Then 页面昵称/角色/入口及任务数据逐项可见；所有业务动作由对应身份执行；未核验部分保持BLOCKED；密码不进入脚本默认值、trace、截图或结果；不更改其他线程/公共基线。

## Runtime schema blocker BDD / RED / GREEN

- Given 本轮int_qms Jar SHA b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071已启动，作者账号、组织关系及T1分类节点均由真实页面建立。
- When 管理员点击本例“DCC241016C04正文→检查对应类别”，再由真实作者U从“文控中心→文件上传”进入正式组件。
- Then 应能读取正式类别与上传前置；实际弹窗/上传页显示“系统异常”，会签控件禁用，运行日志出现缺列异常。
- RED（环境前置）：真实可见错误及自然GET `/admin-api/dcc/file-type-taxonomies/106/active-category`、`/admin-api/dcc/file-categories`，HTTP均200但页面业务失败；日志行见backend-schema-blocker.txt。不解析响应JSON证明业务。
- GREEN：BLOCKED；本线程无数据库迁移、服务或生产源码修改授权，不补列、不编造公共矩阵，不以静态检查PASS替代业务复跑。

## Cleanup Keep


- doc/tasks/20261016-dcc-e2e-04/.gitignore
- doc/tasks/20261016-dcc-e2e-04/account-dept-page.png
- doc/tasks/20261016-dcc-e2e-04/account-fixture-ui.js
- doc/tasks/20261016-dcc-e2e-04/account-role-create-blocked.png
- doc/tasks/20261016-dcc-e2e-04/account-role-create-blocked.txt
- doc/tasks/20261016-dcc-e2e-04/account-role-menu.png
- doc/tasks/20261016-dcc-e2e-04/account-role-page.png
- doc/tasks/20261016-dcc-e2e-04/account-ui-discovery-trace.zip
- doc/tasks/20261016-dcc-e2e-04/account-ui-discovery.json
- doc/tasks/20261016-dcc-e2e-04/account-user-list.png
- doc/tasks/20261016-dcc-e2e-04/audit-evidence.js
- doc/tasks/20261016-dcc-e2e-04/audit-evidence.py
- doc/tasks/20261016-dcc-e2e-04/backend-schema-blocker.txt
- doc/tasks/20261016-dcc-e2e-04/closeout-apply-first-failed.txt
- doc/tasks/20261016-dcc-e2e-04/closeout-apply.txt
- doc/tasks/20261016-dcc-e2e-04/closeout-check.json
- doc/tasks/20261016-dcc-e2e-04/closeout-preview.txt
- doc/tasks/20261016-dcc-e2e-04/create-source-echo.py
- doc/tasks/20261016-dcc-e2e-04/create-source.py
- doc/tasks/20261016-dcc-e2e-04/credentials.dpapi.json
- doc/tasks/20261016-dcc-e2e-04/DCC241016C04-source-ECHO1.pdf
- doc/tasks/20261016-dcc-e2e-04/DCC241016C04-source-r2.pdf
- doc/tasks/20261016-dcc-e2e-04/e2e-04.js
- doc/tasks/20261016-dcc-e2e-04/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/execution-log.md
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery-menu.png
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery-trace.zip
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery.json
- doc/tasks/20261016-dcc-e2e-04/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/fixture.template.json
- doc/tasks/20261016-dcc-e2e-04/incident-audit-details.json
- doc/tasks/20261016-dcc-e2e-04/incident-current-edit-dialog-readback.json
- doc/tasks/20261016-dcc-e2e-04/incident-current-identity-readback.json
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-001-target-910426-audit-list.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-001-target-910426-audit-list.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-002-audit-11604.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-002-audit-11604.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-003-audit-11610.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-003-audit-11610.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-004-audit-11629.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-004-audit-11629.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-005-audit-11632.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-005-audit-11632.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-006-audit-11638.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-006-audit-11638.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-007-restored-910426-exact-query-readback.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-007-restored-910426-exact-query-readback.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-008-910426-restored-posts-edit-dialog-readonly.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-008-910426-restored-posts-edit-dialog-readonly.txt
- doc/tasks/20261016-dcc-e2e-04/incident-summary.json
- doc/tasks/20261016-dcc-e2e-04/incident.md
- doc/tasks/20261016-dcc-e2e-04/maintenance-stop.json
- doc/tasks/20261016-dcc-e2e-04/preflight-r2.js
- doc/tasks/20261016-dcc-e2e-04/prepare-ui.js
- doc/tasks/20261016-dcc-e2e-04/prepared.json
- doc/tasks/20261016-dcc-e2e-04/process-cleanup.json
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-001-config-directory-inventory.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-001-config-directory-inventory.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-002-config-directory-query-granted.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-002-config-directory-query-granted.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-003-task-existing-directory-search.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-003-task-existing-directory-search.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-004-task-directory-saved.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-004-task-directory-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-005-category-save-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-005-category-save-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-006-category-mapping-pass.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-006-category-mapping-pass.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-007-task-matrix-preview.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-007-task-matrix-preview.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-008-task-matrix-ready-preview.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-008-task-matrix-ready-preview.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-009-matrix-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-009-matrix-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-010-project-after-submit-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-010-project-after-submit-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-011-task-project-review-visible.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-011-task-project-review-visible.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-012-project-completed-terminal.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-012-project-completed-terminal.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-013-author-T1-preflight-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-013-author-T1-preflight-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-014-author-T1-confirmed-preflight.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-014-author-T1-confirmed-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/r3-detail-ui-001-04-09-new-request-original-defaults.png
- doc/tasks/20261016-dcc-e2e-04/r3-detail-ui-001-04-09-new-request-original-defaults.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-001-route-preflight.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-001-route-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-002-system-role-menu.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-002-system-role-menu.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-003-post-page.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-003-post-page.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-004-task-post-created.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-004-task-post-created.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-005-users-post-preparation.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-005-users-post-preparation.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-006-author-post-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-006-author-post-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-007-post-loop-observed.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-007-post-loop-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-008-own-approvers-posts-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-008-own-approvers-posts-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-009-route-after-posts.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-009-route-after-posts.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-010-menu-search-observed.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-010-menu-search-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-011-signature-authorization-page.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-011-signature-authorization-page.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-012-own-signature-authorizations-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-012-own-signature-authorizations-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-013-S-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-013-S-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-014-L1-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-014-L1-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-015-L2-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-015-L2-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-016-LS-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-016-LS-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-017-A-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-017-A-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-018-route-ready.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-018-route-ready.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-019-category-permissions-before.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-019-category-permissions-before.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-020-category-upload-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-020-category-upload-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-021-project-permission-entry.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-021-project-permission-entry.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-022-author-project-scope.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-022-author-project-scope.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-023-author-project-details.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-023-author-project-details.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-024-original-project-access-readback.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-024-original-project-access-readback.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-025-author-original-role-restored.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-025-author-original-role-restored.txt
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-001-original-draft-after-root-fix.png
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-001-original-draft-after-root-fix.txt
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-002-controlledFileId-tdz-readonly-repro.png
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-002-controlledFileId-tdz-readonly-repro.txt
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-001-detail-current-visible.png
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-001-detail-current-visible.txt
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-002-detail-loaded-system-error.png
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-002-detail-loaded-system-error.txt
- doc/tasks/20261016-dcc-e2e-04/r6-04-07-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-04-08-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-04-10-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-001-04-06-original-draft-restored-green.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-001-04-06-original-draft-restored-green.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-002-04-07-signoff-edit-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-002-04-07-signoff-edit-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-003-04-08-save-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-003-04-08-save-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-004-04-08-distribution-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-004-04-08-distribution-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-005-04-10-submit-preparation-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-005-04-10-submit-preparation-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-006-04-10-exact-lists-confirmation.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-006-04-10-exact-lists-confirmation.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-007-04-10-post-confirmation-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-007-04-10-post-confirmation-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-008-04-10-single-submit-supervisor-state-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-008-04-10-single-submit-supervisor-state-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-009-supervisor-approval-entry.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-009-supervisor-approval-entry.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-010-supervisor-todo-list.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-010-supervisor-todo-list.txt
- doc/tasks/20261016-dcc-e2e-04/r6-submit-confirmation.json
- doc/tasks/20261016-dcc-e2e-04/r7-save-echo-green-ui-001-echo-source-upload-observed.png
- doc/tasks/20261016-dcc-e2e-04/r7-save-echo-green-ui-001-echo-source-upload-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-001-echo-original-saved-lists.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-001-echo-original-saved-lists.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-002-echo-input-reverse-order.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-002-echo-input-reverse-order.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-003-echo-save-success-no-warning.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-003-echo-save-success-no-warning.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-004-echo-reloaded-green.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-004-echo-reloaded-green.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-005-original-submitted-once-still-supervisor.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-005-original-submitted-once-still-supervisor.txt
- doc/tasks/20261016-dcc-e2e-04/redact-trace.py
- doc/tasks/20261016-dcc-e2e-04/root-detail-resume-result.json
- doc/tasks/20261016-dcc-e2e-04/root-original-trigger-resume-result.json
- doc/tasks/20261016-dcc-e2e-04/root-save-echo-green.json
- doc/tasks/20261016-dcc-e2e-04/root-save-echo-warning.json
- doc/tasks/20261016-dcc-e2e-04/root-watch-resume-green.json
- doc/tasks/20261016-dcc-e2e-04/run-e2e.js
- doc/tasks/20261016-dcc-e2e-04/save-echo-green-before.json
- doc/tasks/20261016-dcc-e2e-04/session-1792113342543-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113897937-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113904522-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113910842-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113917518-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113924355-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113930863-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113937253-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792114994178-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792115737131-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792116136540-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792116830837-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792117883646-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118065621-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118068918-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118076477-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118084000-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118091669-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118099641-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118107306-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118114851-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118296997-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792119814666-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792120217457-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792155939249-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156040007-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156420527-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156427596-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792158943609-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792159005749-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792159399353-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792160018091-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792169717933-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792169935354-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792170894356-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792170934251-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171355939-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171463407-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171468803-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171474568-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171480183-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171539156-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171596123-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172057159-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172116092-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172259460-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172541669-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172626830-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172785586-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173328953-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173426510-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173735750-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173899858-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792174391099-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792174645708-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792175746105-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792176341073-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177125854-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177360423-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177572217-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792204746525-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792205086253-trace.zip
- doc/tasks/20261016-dcc-e2e-04/source-ECHO1-render.png
- doc/tasks/20261016-dcc-e2e-04/source-ECHO1-verification.json
- doc/tasks/20261016-dcc-e2e-04/source-r2-render.png
- doc/tasks/20261016-dcc-e2e-04/source-r2-verification.json
- doc/tasks/20261016-dcc-e2e-04/static-check.json
- doc/tasks/20261016-dcc-e2e-04/task.md
- doc/tasks/20261016-dcc-e2e-04/ui-001-author-after-first-password-change.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-author-after-first-password-change.txt
- doc/tasks/20261016-dcc-e2e-04/ui-001-DCC241016C04U-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-DCC241016C04U-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-001-existing-task-role.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-existing-task-role.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-menu-tree.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-menu-tree.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-profile-actual.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-profile-actual.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-DCC241016C04S-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-DCC241016C04S-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-basic-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-basic-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-menu-saved.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-menu-saved.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-DCC241016C04L1-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-DCC241016C04L1-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-004-all-task-roles.png
- doc/tasks/20261016-dcc-e2e-04/ui-004-all-task-roles.txt
- doc/tasks/20261016-dcc-e2e-04/ui-004-DCC241016C04L2-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-004-DCC241016C04L2-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-005-DCC241016C04LS-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-005-DCC241016C04LS-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-005-department-before-create.png
- doc/tasks/20261016-dcc-e2e-04/ui-005-department-before-create.txt
- doc/tasks/20261016-dcc-e2e-04/ui-006-DCC241016C04LD-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-006-DCC241016C04LD-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-006-department-form-visible.png
- doc/tasks/20261016-dcc-e2e-04/ui-006-department-form-visible.txt
- doc/tasks/20261016-dcc-e2e-04/ui-007-author-user-created.png
- doc/tasks/20261016-dcc-e2e-04/ui-007-author-user-created.txt
- doc/tasks/20261016-dcc-e2e-04/ui-007-DCC241016C04A-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-007-DCC241016C04A-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-008-DCC241016C04D-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-008-DCC241016C04D-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-009-dcc-permission-config-entry.png
- doc/tasks/20261016-dcc-e2e-04/ui-009-dcc-permission-config-entry.txt
- doc/tasks/20261016-dcc-e2e-04/ui-010-task-taxonomy-created.png
- doc/tasks/20261016-dcc-e2e-04/ui-010-task-taxonomy-created.txt
- doc/tasks/20261016-dcc-e2e-04/ui-011-task-type-category-mapping.png
- doc/tasks/20261016-dcc-e2e-04/ui-011-task-type-category-mapping.txt
- doc/tasks/20261016-dcc-e2e-04/ui-012-project-request-form.png
- doc/tasks/20261016-dcc-e2e-04/ui-012-project-request-form.txt
- doc/tasks/20261016-dcc-e2e-04/ui-013-author-formal-upload-entry.png
- doc/tasks/20261016-dcc-e2e-04/ui-013-author-formal-upload-entry.txt
- doc/tasks/20261016-dcc-e2e-04/ui-journal-incident-readonly.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r2-caafeb.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r3-detail.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r3-e537.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r4-detail-resume.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r5-watch-resume.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r6-494306-resume.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r7-save-echo-green.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r7b-save-echo-green.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-session.js
- doc/tasks/20261016-dcc-e2e-04/user-save-identity.js
- doc/tasks/20261016-dcc-e2e-04/user-save-identity.test.js
- doc/tasks/20261016-dcc-e2e-04/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/01-fresh-login.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/01-fresh-login.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/02-04-01.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/02-04-01.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/03-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/03-04-02a.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/04-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/04-04-02b.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/05-04-03.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/05-04-03.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/06-04-04.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/06-04-04.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/07-04-05a.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/07-04-05a.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/08-04-05b.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/08-04-05b.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/09-failure.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/09-failure.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/audit-evidence.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/authorization.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/create-source.py
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/DCC241020C04R1-source.pdf
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/execution-log.md
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/issues.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/rerun-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/rerun.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/run.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/runtime-check.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/source-ROUND1-render.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/source-ROUND1-verification.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/task.md
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04A.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04L1.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04L2.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04LS.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04S.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/manifest.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/01-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/06-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/06-04-05a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/07-04-05b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/08-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/06-04-05a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/07-04-05b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/08-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/prepared.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/static-check.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/author-route-preflight.png
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/author-route-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/preflight-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/runtime-identity.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-detail-controlledFileId/readonly-reproduction.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-detail-controlledFileId/source-fingerprint.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/approver-baseline-comparison.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/detail-console-errors.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/detail-source-fingerprint.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/runtime-identity.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/structure-check.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-watch-fixed/backend-readonly-blocker.txt
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-watch-fixed/readonly-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-01-folder-selector/01-fresh-login.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-01-folder-selector/01-fresh-login.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-01-folder-selector/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-01-folder-selector/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-01-folder-selector/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-02-through-work-draft/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-02-through-work-draft/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/attempt-02-through-work-draft/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/01-submitted-header-before-reload.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/01-submitted-header-before-reload.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/02-04-10.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/02-04-10.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/authorization.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/prior-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/rerun.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/run.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/final-readonly/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/01-04-06-existing-draft-readback.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/01-04-06-existing-draft-readback.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/02-04-06.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/02-04-06.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/03-failure.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/03-failure.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/authorization.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/prior-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/rerun.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/run.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-02/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/01-04-06-existing-draft-readback.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/01-04-06-existing-draft-readback.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/02-04-06.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/02-04-06.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/03-configuration-confirmation.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/03-configuration-confirmation.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/04-04-07.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/04-04-07.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/05-04-08-input-reverse-order.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/05-04-08-input-reverse-order.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/06-04-08.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/06-04-08.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/07-04-09.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/07-04-09.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/08-final-confirmation.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/08-final-confirmation.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/09-failure.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/09-failure.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/authorization.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/new-application-defaults.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/prior-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/rerun.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/run.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-03/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/01-04-06-existing-draft-readback.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/01-04-06-existing-draft-readback.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/02-failure.png
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/02-failure.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/authorization.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/failure-dom.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/prior-result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/rerun.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/result.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/run.js
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/resume-existing/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/process-cleanup.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/structure-check.json
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/closeout-preview.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/closeout-apply-blocked.txt
- doc/tasks/20261016-dcc-e2e-04/rerun-20261020-round1/closeout-check.json


- doc/tasks/20261016-dcc-e2e-04/.gitignore
- doc/tasks/20261016-dcc-e2e-04/account-dept-page.png
- doc/tasks/20261016-dcc-e2e-04/account-fixture-ui.js
- doc/tasks/20261016-dcc-e2e-04/account-role-create-blocked.png
- doc/tasks/20261016-dcc-e2e-04/account-role-create-blocked.txt
- doc/tasks/20261016-dcc-e2e-04/account-role-menu.png
- doc/tasks/20261016-dcc-e2e-04/account-role-page.png
- doc/tasks/20261016-dcc-e2e-04/account-ui-discovery-trace.zip
- doc/tasks/20261016-dcc-e2e-04/account-ui-discovery.json
- doc/tasks/20261016-dcc-e2e-04/account-user-list.png
- doc/tasks/20261016-dcc-e2e-04/audit-evidence.js
- doc/tasks/20261016-dcc-e2e-04/audit-evidence.py
- doc/tasks/20261016-dcc-e2e-04/backend-schema-blocker.txt
- doc/tasks/20261016-dcc-e2e-04/create-source.py
- doc/tasks/20261016-dcc-e2e-04/credentials.dpapi.json
- doc/tasks/20261016-dcc-e2e-04/DCC241016C04-source-r2.pdf
- doc/tasks/20261016-dcc-e2e-04/e2e-04.js
- doc/tasks/20261016-dcc-e2e-04/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/execution-log.md
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery-menu.png
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery-trace.zip
- doc/tasks/20261016-dcc-e2e-04/fixture-discovery.json
- doc/tasks/20261016-dcc-e2e-04/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/fixture.template.json
- doc/tasks/20261016-dcc-e2e-04/preflight-r2.js
- doc/tasks/20261016-dcc-e2e-04/prepare-ui.js
- doc/tasks/20261016-dcc-e2e-04/prepared.json
- doc/tasks/20261016-dcc-e2e-04/process-cleanup.json
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-001-config-directory-inventory.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-001-config-directory-inventory.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-002-config-directory-query-granted.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-002-config-directory-query-granted.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-003-task-existing-directory-search.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-003-task-existing-directory-search.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-004-task-directory-saved.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-004-task-directory-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-005-category-save-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-005-category-save-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-006-category-mapping-pass.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-006-category-mapping-pass.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-007-task-matrix-preview.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-007-task-matrix-preview.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-008-task-matrix-ready-preview.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-008-task-matrix-ready-preview.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-009-matrix-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-009-matrix-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-010-project-after-submit-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-010-project-after-submit-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-011-task-project-review-visible.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-011-task-project-review-visible.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-012-project-completed-terminal.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-012-project-completed-terminal.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-013-author-T1-preflight-observed.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-013-author-T1-preflight-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-014-author-T1-confirmed-preflight.png
- doc/tasks/20261016-dcc-e2e-04/r2-caafeb-ui-014-author-T1-confirmed-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/redact-trace.py
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/prepared.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/static-check.json
- doc/tasks/20261016-dcc-e2e-04/rounds/blocked-before-caafeb/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/author-route-preflight.png
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/author-route-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/e2e-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/evidence-audit.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/preflight-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/runtime-identity.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/trace.zip
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-caafeb/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/run-e2e.js
- doc/tasks/20261016-dcc-e2e-04/session-1792113342543-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113897937-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113904522-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113910842-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113917518-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113924355-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113930863-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792113937253-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792114994178-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792115737131-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792116136540-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792116830837-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792117883646-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118065621-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118068918-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118076477-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118084000-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118091669-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118099641-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118107306-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118114851-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792118296997-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792119814666-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792120217457-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792155939249-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156040007-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156420527-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792156427596-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792158943609-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792159005749-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792159399353-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792160018091-trace.zip
- doc/tasks/20261016-dcc-e2e-04/source-r2-render.png
- doc/tasks/20261016-dcc-e2e-04/source-r2-verification.json
- doc/tasks/20261016-dcc-e2e-04/static-check.json
- doc/tasks/20261016-dcc-e2e-04/task.md
- doc/tasks/20261016-dcc-e2e-04/ui-001-author-after-first-password-change.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-author-after-first-password-change.txt
- doc/tasks/20261016-dcc-e2e-04/ui-001-DCC241016C04U-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-DCC241016C04U-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-001-existing-task-role.png
- doc/tasks/20261016-dcc-e2e-04/ui-001-existing-task-role.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-menu-tree.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-menu-tree.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-profile-actual.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-author-profile-actual.txt
- doc/tasks/20261016-dcc-e2e-04/ui-002-DCC241016C04S-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-002-DCC241016C04S-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-basic-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-basic-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-menu-saved.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-author-menu-saved.txt
- doc/tasks/20261016-dcc-e2e-04/ui-003-DCC241016C04L1-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-003-DCC241016C04L1-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-004-all-task-roles.png
- doc/tasks/20261016-dcc-e2e-04/ui-004-all-task-roles.txt
- doc/tasks/20261016-dcc-e2e-04/ui-004-DCC241016C04L2-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-004-DCC241016C04L2-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-005-DCC241016C04LS-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-005-DCC241016C04LS-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-005-department-before-create.png
- doc/tasks/20261016-dcc-e2e-04/ui-005-department-before-create.txt
- doc/tasks/20261016-dcc-e2e-04/ui-006-DCC241016C04LD-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-006-DCC241016C04LD-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-006-department-form-visible.png
- doc/tasks/20261016-dcc-e2e-04/ui-006-department-form-visible.txt
- doc/tasks/20261016-dcc-e2e-04/ui-007-author-user-created.png
- doc/tasks/20261016-dcc-e2e-04/ui-007-author-user-created.txt
- doc/tasks/20261016-dcc-e2e-04/ui-007-DCC241016C04A-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-007-DCC241016C04A-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-008-DCC241016C04D-profile.png
- doc/tasks/20261016-dcc-e2e-04/ui-008-DCC241016C04D-profile.txt
- doc/tasks/20261016-dcc-e2e-04/ui-009-dcc-permission-config-entry.png
- doc/tasks/20261016-dcc-e2e-04/ui-009-dcc-permission-config-entry.txt
- doc/tasks/20261016-dcc-e2e-04/ui-010-task-taxonomy-created.png
- doc/tasks/20261016-dcc-e2e-04/ui-010-task-taxonomy-created.txt
- doc/tasks/20261016-dcc-e2e-04/ui-011-task-type-category-mapping.png
- doc/tasks/20261016-dcc-e2e-04/ui-011-task-type-category-mapping.txt
- doc/tasks/20261016-dcc-e2e-04/ui-012-project-request-form.png
- doc/tasks/20261016-dcc-e2e-04/ui-012-project-request-form.txt
- doc/tasks/20261016-dcc-e2e-04/ui-013-author-formal-upload-entry.png
- doc/tasks/20261016-dcc-e2e-04/ui-013-author-formal-upload-entry.txt
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r2-caafeb.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-session.js
- doc/tasks/20261016-dcc-e2e-04/verification-report.md

- doc/tasks/20261016-dcc-e2e-04/r3-detail-ui-001-04-09-new-request-original-defaults.png
- doc/tasks/20261016-dcc-e2e-04/r3-detail-ui-001-04-09-new-request-original-defaults.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-001-route-preflight.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-001-route-preflight.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-002-system-role-menu.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-002-system-role-menu.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-003-post-page.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-003-post-page.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-004-task-post-created.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-004-task-post-created.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-005-users-post-preparation.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-005-users-post-preparation.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-006-author-post-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-006-author-post-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-007-post-loop-observed.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-007-post-loop-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-008-own-approvers-posts-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-008-own-approvers-posts-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-009-route-after-posts.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-009-route-after-posts.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-010-menu-search-observed.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-010-menu-search-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-011-signature-authorization-page.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-011-signature-authorization-page.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-012-own-signature-authorizations-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-012-own-signature-authorizations-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-013-S-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-013-S-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-014-L1-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-014-L1-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-015-L2-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-015-L2-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-016-LS-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-016-LS-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-017-A-own-signature-enabled.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-017-A-own-signature-enabled.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-018-route-ready.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-018-route-ready.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-019-category-permissions-before.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-019-category-permissions-before.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-020-category-upload-saved.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-020-category-upload-saved.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-021-project-permission-entry.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-021-project-permission-entry.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-022-author-project-scope.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-022-author-project-scope.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-023-author-project-details.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-023-author-project-details.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-024-original-project-access-readback.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-024-original-project-access-readback.txt
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-025-author-original-role-restored.png
- doc/tasks/20261016-dcc-e2e-04/r3-e537-ui-025-author-original-role-restored.txt
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/approver-baseline-comparison.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/detail-console-errors.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/detail-source-fingerprint.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/runtime-identity.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/01-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-47-20-242Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/06-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-48-13-026Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/06-04-05a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/07-04-05b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/08-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-50-00-769Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/01-04-01.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/02-04-02a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/03-04-02b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/04-04-03.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/05-04-04.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/06-04-05a.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/07-04-05b.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/08-failure.png
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/result.json
- doc/tasks/20261016-dcc-e2e-04/runs/2026-10-16T17-53-32-047Z/trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792169717933-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792169935354-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792170894356-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792170934251-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171355939-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171463407-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171468803-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171474568-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171480183-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171539156-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792171596123-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172057159-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172116092-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172259460-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172541669-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172626830-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792172785586-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173328953-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173426510-trace.zip
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04A.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04L1.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04L2.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04LS.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/DCC241016C04S.png
- doc/tasks/20261016-dcc-e2e-04/signature-fixtures/manifest.json
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r3-detail.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r3-e537.jsonl

- doc/tasks/20261016-dcc-e2e-04/rounds/resume-e537/structure-check.json

- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-001-original-draft-after-root-fix.png
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-001-original-draft-after-root-fix.txt
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-002-controlledFileId-tdz-readonly-repro.png
- doc/tasks/20261016-dcc-e2e-04/r4-detail-resume-ui-002-controlledFileId-tdz-readonly-repro.txt
- doc/tasks/20261016-dcc-e2e-04/root-detail-resume-result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-detail-controlledFileId/readonly-reproduction.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-detail-controlledFileId/source-fingerprint.json
- doc/tasks/20261016-dcc-e2e-04/session-1792173735750-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792173899858-trace.zip
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r4-detail-resume.jsonl

- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-001-detail-current-visible.png
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-001-detail-current-visible.txt
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-002-detail-loaded-system-error.png
- doc/tasks/20261016-dcc-e2e-04/r5-watch-resume-ui-002-detail-loaded-system-error.txt
- doc/tasks/20261016-dcc-e2e-04/root-watch-resume-green.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-watch-fixed/backend-readonly-blocker.txt
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-watch-fixed/readonly-result.json
- doc/tasks/20261016-dcc-e2e-04/session-1792174391099-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792174645708-trace.zip
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r5-watch-resume.jsonl

- doc/tasks/20261016-dcc-e2e-04/maintenance-stop.json

- doc/tasks/20261016-dcc-e2e-04/incident-audit-details.json
- doc/tasks/20261016-dcc-e2e-04/incident-current-edit-dialog-readback.json
- doc/tasks/20261016-dcc-e2e-04/incident-current-identity-readback.json
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-001-target-910426-audit-list.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-001-target-910426-audit-list.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-002-audit-11604.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-002-audit-11604.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-003-audit-11610.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-003-audit-11610.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-004-audit-11629.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-004-audit-11629.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-005-audit-11632.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-005-audit-11632.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-006-audit-11638.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-006-audit-11638.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-007-restored-910426-exact-query-readback.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-007-restored-910426-exact-query-readback.txt
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-008-910426-restored-posts-edit-dialog-readonly.png
- doc/tasks/20261016-dcc-e2e-04/incident-readonly-ui-008-910426-restored-posts-edit-dialog-readonly.txt
- doc/tasks/20261016-dcc-e2e-04/incident-summary.json
- doc/tasks/20261016-dcc-e2e-04/incident.md
- doc/tasks/20261016-dcc-e2e-04/session-1792175746105-trace.zip
- doc/tasks/20261016-dcc-e2e-04/ui-journal-incident-readonly.jsonl
- doc/tasks/20261016-dcc-e2e-04/user-save-identity.js
- doc/tasks/20261016-dcc-e2e-04/user-save-identity.test.js

- doc/tasks/20261016-dcc-e2e-04/create-source-echo.py
- doc/tasks/20261016-dcc-e2e-04/DCC241016C04-source-ECHO1.pdf
- doc/tasks/20261016-dcc-e2e-04/r6-04-07-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-04-08-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-04-10-result.json
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-001-04-06-original-draft-restored-green.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-001-04-06-original-draft-restored-green.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-002-04-07-signoff-edit-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-002-04-07-signoff-edit-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-003-04-08-save-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-003-04-08-save-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-004-04-08-distribution-saved-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-004-04-08-distribution-saved-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-005-04-10-submit-preparation-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-005-04-10-submit-preparation-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-006-04-10-exact-lists-confirmation.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-006-04-10-exact-lists-confirmation.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-007-04-10-post-confirmation-observed.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-007-04-10-post-confirmation-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-008-04-10-single-submit-supervisor-state-reloaded.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-008-04-10-single-submit-supervisor-state-reloaded.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-009-supervisor-approval-entry.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-009-supervisor-approval-entry.txt
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-010-supervisor-todo-list.png
- doc/tasks/20261016-dcc-e2e-04/r6-494306-resume-ui-010-supervisor-todo-list.txt
- doc/tasks/20261016-dcc-e2e-04/r6-submit-confirmation.json
- doc/tasks/20261016-dcc-e2e-04/r7-save-echo-green-ui-001-echo-source-upload-observed.png
- doc/tasks/20261016-dcc-e2e-04/r7-save-echo-green-ui-001-echo-source-upload-observed.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-001-echo-original-saved-lists.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-001-echo-original-saved-lists.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-002-echo-input-reverse-order.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-002-echo-input-reverse-order.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-003-echo-save-success-no-warning.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-003-echo-save-success-no-warning.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-004-echo-reloaded-green.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-004-echo-reloaded-green.txt
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-005-original-submitted-once-still-supervisor.png
- doc/tasks/20261016-dcc-e2e-04/r7b-save-echo-green-ui-005-original-submitted-once-still-supervisor.txt
- doc/tasks/20261016-dcc-e2e-04/root-original-trigger-resume-result.json
- doc/tasks/20261016-dcc-e2e-04/root-save-echo-green.json
- doc/tasks/20261016-dcc-e2e-04/root-save-echo-warning.json
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/final-echo-green/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/result.json
- doc/tasks/20261016-dcc-e2e-04/rounds/resume-494306/verification-report.md
- doc/tasks/20261016-dcc-e2e-04/save-echo-green-before.json
- doc/tasks/20261016-dcc-e2e-04/session-1792176341073-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177125854-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177360423-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792177572217-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792204746525-trace.zip
- doc/tasks/20261016-dcc-e2e-04/session-1792205086253-trace.zip
- doc/tasks/20261016-dcc-e2e-04/source-ECHO1-render.png
- doc/tasks/20261016-dcc-e2e-04/source-ECHO1-verification.json
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r6-494306-resume.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r7-save-echo-green.jsonl
- doc/tasks/20261016-dcc-e2e-04/ui-journal-r7b-save-echo-green.jsonl
- doc/tasks/20261016-dcc-e2e-04/closeout-preview.txt
- doc/tasks/20261016-dcc-e2e-04/closeout-check.json

- doc/tasks/20261016-dcc-e2e-04/closeout-apply-first-failed.txt
- doc/tasks/20261016-dcc-e2e-04/closeout-apply.txt

## 恢复轮 BDD / 新前置 RED

- Given 新Jar/schema已恢复，专用类型126→类别908745匹配成功，专用矩阵保存D1/D2及A，项目申请30在页面COMPLETED并生成项目293和本例文件夹。
- When 作者U真实登录，从文控中心→文件上传选择本例项目、文件夹、T1R2，并确认正式类型切换提示。
- Then 应由正式预检生成两份默认D1/D2名单和可核对的批准人；实际页面显示EXECUTION_CONTRACT_UNREGISTERED_DEFINITION，两份默认名单为空。
- RED：preflight-r2.js退出21；真实DOM、截图、脱敏trace与仅method/path/status自然请求见rounds/resume-caafeb。route-preview为正式页面自然触发，未直接接口调用，不解析响应JSON替代DOM。
- GREEN：未执行；须由Root核对正式dcc-controlled-file-upload定义、执行合同及摘要。仅凭源码定位不能断言实际未知digest。本例不改共享流程定义/catalog或生产配置。



## 2026-10-17 e537 恢复轮 BDD

- Given Root正式运行证明已更新为e537、PID34096，复用项目293/类别908745及现有自有账号。
- When 从真实页面重核路线，按实际阻塞补齐任务账号系统岗位、阶段权限、签名授权和本人图片；核对类别UPLOAD/项目EDIT。
- Then 路线真实就绪后执行原12个分支，独立保存本轮证据；缺口不视作PASS，不提升OWNER、不改共享定义。

## e537恢复轮结果及保留

- 8 PASS / 1 FAIL / 3 BLOCKED；详情页初始化产品问题交Root，未最终提交。
- 岗位竞态误加已真实页面撤销并回读；临时本例配置角色已移除。
- 当前原工作稿2054545668044084183，不得重跑原建稿脚本重复创建。
- 本轮无需Git、服务或数据库操作；状态blocked，不标completed。

## 原工作稿续跑 BDD

- Given Root已将currentUserId声明前移，保留已存fileId2054545668044084183。
- When U以新context打开原已记录URL，先只读核对编号/版本/状态/名单，再执行04-06/07/08/10。
- Then 原名单恢复正确、两名单独立持久化、最终真实进入主管审核；不得新建重传，不改生产详情。

## Root声明前移后只读复验

- U新context打开原保存URL，currentUserId旧错未复现，新的pageerror为controlledFileId初始化前访问。刷新再现；正文DOM为空，DCC写请求0，无新建/重传。
- 源码只读定位：applicationRoundSelection在3068读取controlledFileId，3107建立的watch求值；controlledFileId在3545声明。生产归属仍由Root处理。
- 8PASS/1FAIL/3BLOCKED不变；保留原稿2054545668044084183。

## Watch注册根因修复后续验 BDD

- Given Root将延迟确认watch移至依赖声明链之后，原工作稿2054545668044084183未提交且保留。
- When 原U新context只读打开原URL，核对详情及保存名单后，继续04-06/07/08/10。
- Then 详情加载先记录GREEN，再在原稿完成名单独立持久化及主管待办提交；不重传重建，不改production。

## Watch声明链根因修复后：TDZ GREEN，详情业务读取仍BLOCKED

原U新context进入原fileId2054545668044084183后，页面已能渲染；刷新复验pageErrors=[]，currentUserId/controlledFileId TDZ均未复现。真实页面仍报“系统异常”，编号/版本等业务字段为空，无“准备本次申请”按钮；不能把骨架渲染当原稿成功加载。

正式页面自然GET详情HTTP200，未解析响应JSON；同期运行后端日志明确`Cannot execute statement in a READ ONLY transaction.`。链路：QueryService.getControlledFile只读事务→toRespVO:3726→AttachmentService.readOrdinaryAttachmentManifest:177→requireAttachmentVersion:252→Mapper.selectByIdAndTenantForUpdate（SELECT FOR UPDATE）。此为新后端阻塞，生产修复归Root，本例不改源码/数据库/服务。

root-watch-resume-green.json和rounds/resume-watch-fixed/readonly-result.json区分TDZ GREEN与业务读取BLOCKED；backend-readonly-blocker.txt为精确只读日志摘录。r5-watch-resume-ui-001/002截图及DOM证明页面。全程DCC写请求=[]，未重传重建、未最终提交。原8PASS/1FAIL/3BLOCKED保持，04-06/07/08/10须在原稿继续。

## Root运行态维护安全点

- 2026-10-17T02:21:58.368955+08:00：浏览器已关闭，本例Node进程0，停止新页面写入；保留原工作稿2054545668044084183。
- 等待Root健康成功及新PID/SHA后只复验原稿；不重建重传或重复提交。维护连接错误不记业务FAIL，当前8/1/3结论保持。

## 跨例岗位误写事件补全

正式操作日志11629原C02岗位、11632误加C04岗位、11638仅移除C04岗位，均业务编号910426。已按Root要求以精确username查询及GET id=910426编辑弹框只读回读，昵称匹配，岗位精确仅[DCC241016C02验证岗位]，无C04；取消未保存，update请求0。完整事件incident.md，当前弹框证据incident-current-edit-dialog-readback.json及incident-readonly-ui-008截图。任务runner加入精确身份保存校验，13项边界测试PASS，未新增产品防御或编辑其他例账号。

## PID16776原工作稿恢复 BDD

- Given Root新Jar49430662/PID16776健康及UI登录核验通过，原8PASS及工作稿2054545668044084183保留。
- When 原U新context只读重开原稿核对会签D2/DS、下发DD，再执行04-07/08保存刷新及04-10真实提交到主管节点。
- Then 所有12分支具有真实结果，原稿复用，不重传重建，不扩大权限，不编辑其他例账号。

## PID16776原稿续验结果

- 11PASS/1FAIL/0BLOCKED；04-08错误提示未忽略，实际名单回读正确。原稿单次提交至主管节点，不等同审批完成。
- 提交路径监听修正为自然/{id}/submit，旧路径wait超时后没有重试。
- 本轮页面/名单证明、错误原证据、单次提交/BPM及残留已写verification-report；生产比较逻辑Root独占。

## 04-08回显排序修复真实GREEN BDD

- Given Root已修保存回显按成员比较；原稿已单次提交，禁止退回或重复提交。
- When 按Root明确授权仅新建最小工作稿DCC241016C04DOC-ECHO1，复用原项目/类型/账号/源件；在详情下发DD基础加D1，以DD/D1顺序保存并刷新。
- Then 显示保存成功，无回显不一致，会签保持D2，下发为D1/DD；不提交新稿，不改公共矩阵或原审批。

## 最终业务验证

- 12PASS；04-08原RED保留，Root修复后唯一最小工作稿真实GREEN。
- 原申请单次提交/BPM保持，ECHO1工作稿submit0；两记录保留供Root复核。
- 先标ready_for_closeout完成证据清理预览；Git及最终工程收尾归Root，无本例提交推送授权，不标completed。

## 工程收尾状态

- 已先标ready_for_closeout；cleanup preview及最终apply通过，delete0/warnings none，未删除任务证据。首次apply输出日志keep遗漏导致占用拒删，原失败已保留，补keep后通过。
- 业务12PASS，工程因本例无提交推送授权且Root待review保持blocked，不标completed。原待主管申请及未提交回归稿保留，浏览器关闭。


## 2026-10-20 第一轮完整重跑

目标：按本轮明确授权，在int_qms 8061/48061以Playwright真实前端重跑04-01至04-10共12参数分支，不能复用历史PASS。
里程碑：规则/运行态核对 → 本人真实登录与路线预检 → 唯一新申请完整分支 → 独立结果及浏览器关闭证据。
验证：页面DOM/输入值/确认框/刷新状态为业务依据；自然请求仅路径、method、HTTP状态。
设计约束：仅本例目录；不改生产/权限/全局配置/数据库、不构建重启、不Git、不子Agent；每人一个部门；音视频只作附件。
BDD：Given 当前真实账号、P1/T1和新编号DCC241020C04R1；When 页面分别增删会签与下发并保存回读、另开新申请比较默认值、最后单次提交；Then 两份名单独立、公共矩阵不变、原新申请进入主管审核；缺权限或服务如实BLOCKED。
本次独立证据目录：rerun-20261020-round1；保留全部历史。当前历史12PASS仅属旧轮。
