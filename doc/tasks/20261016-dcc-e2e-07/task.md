# E2E-07 二次确认与取消

## Goal

独立验证E2E-07，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 第二轮新运行R2g1oei3业务3/3 PASS、无遗漏，浏览器/context均关闭、runner退出0；本轮结果见 rerun-20261020-round2-1792462139498/handoff.json。仅工程closeout因本轮禁止Git和cleanup apply保持blocked；历史证据保留，不启动下一轮。

## BDD

- Given 申请字段、附件、关联和名单均已填写
- When 点击确认，逐项核对汇总；取消后再打开；最后再次确认提交
- Then 汇总包含名称、编号、版本、项目、文件夹、类型、编制人/主管、生效方式、三个实际属性、附件、关联、会签、下发、批准人和培训；明确接收人为所选下发部门全部人员。取消保留输入且无审批任务；最终确认后进入主管待办。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 真实前端三组验收完成；任务自有数据按Root要求保留待review。
4. [x] Root review已ROOT_ACCEPTED，三分支原范围业务证据接受。

## Parameterized Subcases

- `IMMEDIATE_NO_TRAINING`: 生效方式“文控审核后立即生效”，培训未选。
- `DELAYED_NO_TRAINING`: 生效方式“延迟生效”，培训未选；本例只验证提交进入主管待办，不推进后续延迟确认任务。
- `DELAYED_TRAINING`: 生效方式“延迟生效”，培训已选；本例只验证提交进入主管待办，不推进培训或生效。

每个分支使用独立文件编号、源件、两个普通附件、申请 context 和主管 context；统一前缀 `DCC241016C07`。三分支均必须在确认汇总中断言：项目、文件夹、类型、编制人、主管、文件名、编号、初始版本、生效方式、三个实际属性、关联文件、会签、下发、下发范围、批准人、批准规则、培训和附件。

## Formal Page Entrances And Actors

- 申请人从真实可见菜单进入 DCC 文件上传页 `/dcc/controlled-file/upload`，使用页面项目、项目文件夹、文件类型、属性、会签/下发、附件和关联控件；源件及附件均由页面文件选择器上传。
- 主管从真实可见菜单进入审批中心待办 `/approval-center/todo`，按本例文件编号查询；提交后从该行点击页面“查看”，在详情处理态核对“审批阶段进度”、主管当前任务和“审核通过”按钮，但本例不点击审批通过。
- 申请人和直属主管必须是两个真实账号，页面分别确认租户和显示名；不能用 `admin` 冒充任一业务角色。精确角色 code、用户绑定、菜单/按钮权限、直属主管关系和全员下发名单由 Root 通过真实页面零写入核对后写入 fixture。
- 关联文件必须是本例同一 DCC 项目内、已生效且可在真实“本项目搜索”结果中辨认的 `DCC241016C07*` 文件；跨项目候选不得被选中。

## Script Assets

2026-10-17最新约定：gate=execute表示已有授权，采用PER_CASE_REAL_UI_MANIFEST。共享roleFixtureVerified=false不阻塞；旧prepare_only描述仅为历史。运行证明从dispatch.environment.runtimeEvidence读取。目前环境中断暂停见runtime-pause-environment-interruption-20261017.json。

- 本例资产与真实角色核验记录见fixture-manifest.json，目前verified=false（关联候选尚未确认受控生效）。
- 当前结果见result-20261017.json，四位审签人图片均已由本人真实页面启用，凭据只存本机DPAPI，不进入证据/提交。
- 被验收类别CAT采用PUBLIC_MATRIX；仅关联候选准备使用本例独立APPLICATION_CUSTOM类别RELCAT，避免改变公共文控岗位或借用其他用例数据。

- `doc/tasks/20261016-dcc-e2e-07/e2e-07.mjs`: `--check` 只做静态检查，`--gate-check` 只读调度检查，`--run` 仅在 Root 将 gate 改为 `execute` 且提供完整 fixture/凭据环境变量后运行。
- `doc/tasks/20261016-dcc-e2e-07/fixture.example.json`: fixture 合同样例，不含账号密码、token 或业务真实值。
- `doc/tasks/20261016-dcc-e2e-07/sanitize-trace.py`: 仅在真实运行后将任务原始 trace 脱敏为可 review trace，并删除 raw trace。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C07，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-07/review-evidence-manifest.json
- doc/tasks/20261016-dcc-e2e-07/closeout-preview-status.json
- doc/tasks/20261016-dcc-e2e-07/.gitignore
- doc/tasks/20261016-dcc-e2e-07/A-approval-final-confirmed.json
- doc/tasks/20261016-dcc-e2e-07/A-continue-current-result.json
- doc/tasks/20261016-dcc-e2e-07/A-recovery-history.json
- doc/tasks/20261016-dcc-e2e-07/A-recovery-readonly.json
- doc/tasks/20261016-dcc-e2e-07/A-signature-current.json
- doc/tasks/20261016-dcc-e2e-07/acceptance-actions.mjs
- doc/tasks/20261016-dcc-e2e-07/acceptance-result.json
- doc/tasks/20261016-dcc-e2e-07/account-preflight-failure.json
- doc/tasks/20261016-dcc-e2e-07/account-preflight-menu.png
- doc/tasks/20261016-dcc-e2e-07/account-preflight.json
- doc/tasks/20261016-dcc-e2e-07/account-preflight.mjs
- doc/tasks/20261016-dcc-e2e-07/audit-credentials.mjs
- doc/tasks/20261016-dcc-e2e-07/audit-traces.py
- doc/tasks/20261016-dcc-e2e-07/C-signature-current.json
- doc/tasks/20261016-dcc-e2e-07/create-post-ui.mjs
- doc/tasks/20261016-dcc-e2e-07/create-users-ui.mjs
- doc/tasks/20261016-dcc-e2e-07/credential-audit.json
- doc/tasks/20261016-dcc-e2e-07/D-signature-current.json
- doc/tasks/20261016-dcc-e2e-07/directory-preview-permission-ui.json
- doc/tasks/20261016-dcc-e2e-07/driver-exit-check.json
- doc/tasks/20261016-dcc-e2e-07/e2e-07.mjs
- doc/tasks/20261016-dcc-e2e-07/execution-log.md
- doc/tasks/20261016-dcc-e2e-07/finalize-report.mjs
- doc/tasks/20261016-dcc-e2e-07/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-07/fixture.example.json
- doc/tasks/20261016-dcc-e2e-07/fixtures
- doc/tasks/20261016-dcc-e2e-07/generate-fixture-files.mjs
- doc/tasks/20261016-dcc-e2e-07/post-created-ui.json
- doc/tasks/20261016-dcc-e2e-07/post-page-preflight.json
- doc/tasks/20261016-dcc-e2e-07/post-preflight-ui.mjs
- doc/tasks/20261016-dcc-e2e-07/prepared.json
- doc/tasks/20261016-dcc-e2e-07/recon-ui.json
- doc/tasks/20261016-dcc-e2e-07/recon-ui.mjs
- doc/tasks/20261016-dcc-e2e-07/record-checkpoint.mjs
- doc/tasks/20261016-dcc-e2e-07/record-interruption.mjs
- doc/tasks/20261016-dcc-e2e-07/relation-active-ui.json
- doc/tasks/20261016-dcc-e2e-07/relation-selection-denial-ui.json
- doc/tasks/20261016-dcc-e2e-07/result-20261017.json
- doc/tasks/20261016-dcc-e2e-07/role-category-options.json
- doc/tasks/20261016-dcc-e2e-07/role-entry-debug.json
- doc/tasks/20261016-dcc-e2e-07/role-menu-assigned.png
- doc/tasks/20261016-dcc-e2e-07/role-menu-tree.json
- doc/tasks/20261016-dcc-e2e-07/role-menu-tree.png
- doc/tasks/20261016-dcc-e2e-07/run-acceptance.mjs
- doc/tasks/20261016-dcc-e2e-07/runs
- doc/tasks/20261016-dcc-e2e-07/runtime-pause-20261017.json
- doc/tasks/20261016-dcc-e2e-07/runtime-pause-environment-interruption-20261017.json
- doc/tasks/20261016-dcc-e2e-07/runtime-resume-20261017.json
- doc/tasks/20261016-dcc-e2e-07/runtime-resumed-final.json
- doc/tasks/20261016-dcc-e2e-07/S-signature-current.json
- doc/tasks/20261016-dcc-e2e-07/sanitize-saved-traces.py
- doc/tasks/20261016-dcc-e2e-07/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-07/setup-accounts-ui-preflight.json
- doc/tasks/20261016-dcc-e2e-07/setup-accounts-ui.mjs
- doc/tasks/20261016-dcc-e2e-07/setup-role-page.json
- doc/tasks/20261016-dcc-e2e-07/signature-entry-current.json
- doc/tasks/20261016-dcc-e2e-07/task.md
- doc/tasks/20261016-dcc-e2e-07/trace-audit-current.json
- doc/tasks/20261016-dcc-e2e-07/trace-sanitization.json
- doc/tasks/20261016-dcc-e2e-07/training-author-readonly.json
- doc/tasks/20261016-dcc-e2e-07/training-record-permission-ui.json
- doc/tasks/20261016-dcc-e2e-07/training-submit-readonly.json
- doc/tasks/20261016-dcc-e2e-07/training-supervisor-readonly.json
- doc/tasks/20261016-dcc-e2e-07/ui-driver.mjs
- doc/tasks/20261016-dcc-e2e-07/user-form-debug.json
- doc/tasks/20261016-dcc-e2e-07/user-option-preflight.json
- doc/tasks/20261016-dcc-e2e-07/verification-report.md
- doc/tasks/20261016-dcc-e2e-07/verify-signature-entry.mjs
