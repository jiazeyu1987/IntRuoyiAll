# E2E-01 默认值及编制人

## R3 round1 最新交付

本轮新授权完整重跑已完成：10/10 PASS，首次实际exit0，无重试。唯一正式交付rerun-20261020-r3-round1/handoff.json；本次临时源件清理、trace隐私/CRC和context/browser/runner全关通过，当前hold供Root review。旧结果/UNKNOWN保留，工程因当轮禁止Git提交推送blocked。

## Goal

独立验证E2E-01，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — rerun-20261020-r2-round1 已正式放行并完成首次真实完整重跑：原10分支PASS、实际exit0，本次临时清理及context/browser/runner全关已核验，独立handoff已交付，当前hold。工程因当轮禁止Git提交推送未收口；历史结果及早期资产UNKNOWN保留，不启动第二轮。

## rerun-20261020-r2-round1 准备

- 目标与里程碑：核对原合同、账号凭据来源；准备独立证据目录与默认仅预检入口；静态验证后等待放行。
- Given 既有U2 fixture及DPAPI来源；When 未获得新package READY和单独执行授权；Then 仅产生PREPARED_WAITING，不解密、不启动浏览器、不执行业务动作。
- 验证与设计约束：node语法检查及默认入口预检；仅本任务资产，原10分支不变，不改权限/生产代码，不构建、不重启、不提交推送、不启用子Agent；不得用历史PASS或REPAIR_ONLY替代本轮。
- 入口与记录：`rerun-20261020-r2-round1/round1.cjs`、同目录task.md/execution-log.md/verification-report.md；准备回执位于其evidence目录。

## 2026-10-20第一轮真实重跑

- 目标与里程碑：核对分支/端口PID/Jar；本人真实登录；完整执行10分支；确认取消和本次临时源件清理；保存独立结果与关闭回执并结束。
- Given 当前int_qms运行证明及原任务U2/P1R2/T1R2；When 本人从真实菜单重新选择项目/文件夹/类型、修改属性、上传本次唯一PDF并打开确认；Then 逐项核对10分支，取消且仅清理本次临时源件，不产生最终申请或版本。
- 预期验证：本次DOM事实、自然请求method/path/status、trace/截图、实际退出码、对象身份、浏览器/context/runner关闭；缺失分支不记PASS。
- 设计约束：每人仅一部门；音视频仅附件。本例PDF正文适用，不扩展媒体验证。仅编辑本任务目录，可最小校准脚本；不改全局配置/账号权限/其他资产、不调用API/DB或读取响应JSON作为oracle、不启动下一轮。

## 2026-10-19 r4证据边界核查

- 目标：核对失败运行DCC241016C01-20261016155412944能否关联到精确服务端临时资产及正式清理回执。
- 里程碑：读取r4派工与最新handoff；核对既有结果及新增来源；写repair-20261019-r4-handoff.json；结构验证。
- 预期验证：JSON可解析、证据路径存在、命令与退出码真实；没有新来源时记录UNKNOWN及可恢复条件，不重复无效检索。
- Given 历史失败只记录清理未完成且serverState=unknown；When 无新增可追溯资产身份或正式清理回执；Then 保留UNKNOWN，不推断资产存在、不推断已清理、不借独立成功运行覆盖旧结果。
- 设计约束：仅本例文档记录；不改生产代码或测试脚本，不运行Maven/E2E，不开启浏览器、不操作账号或业务数据，不部署重启、不发线程消息。工程收口仍受本轮禁止Git写操作限制。

## BDD

- Given U已登录，P1属性、组织关系及T1矩阵已维护
- When 进入上传，选P1、文件夹及T1；修改本次三个属性
- Then 默认值正确带出；编制人固定U，部门及主管来自组织；本次属性可改；矩阵与默认下发部门正确；培训默认未选。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] Root放行后真实前端执行及本轮临时上传收尾；专用fixture保留供Root review。
4. [x] Root review接受原10分支业务PASS，正式回执为协调目录e2e01-root-review.json；工程收口未完成。

## Subcases And Page Contract

- `01-defaults`：选择已维护项目后，三个项目属性组与 `project.defaults` 完全一致。
- `02-compiler`：提交确认框显示真实登录 U 的显示名/部门，且没有可编辑控件。
- `03-organization`：提交确认框显示真实组织解析出的直属主管及主管部门，且没有可编辑控件。
- `04-market-edit`：本次目标市场改成 `editedAttributes.targetMarkets`，页面选中项与预期一致。
- `05-identity-edit`：本次“是否为注册人”和“是否为生产方”分别改为预期值。
- `06-transfer-edit`：本次“文件转移”及条件说明改为预期值。
- `07-matrix`：T1 的会签部门、批准人、审批规则和原批准组逐项可见。
- `08-distribution`：首次进入时下发部门标签与默认会签部门集合一致，之后不通过第二条链路补齐。
- `09-training`：`data-testid=dcc-upload-need-training` 的复选框默认未选。
- `10-confirmation`：真实页面选择任务自有 PDF 后打开二次确认，核对项目、文件夹、类型、身份、矩阵、下发、培训及编制人/主管，取消后保留输入并从页面移除临时上传。

正式入口是动态菜单“文控中心 → 文件上传”，页面 URL `/dcc/controlled-file/upload`，路由组件为 `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`。稳定锚点包括 `dcc-upload-single-page-workbench`、`dcc-upload-section-scope`、`dcc-upload-section-preflight`、`dcc-upload-route-readiness`、`dcc-upload-need-training`、`dcc-upload-signoff-departments`、`dcc-upload-distribution-departments` 和 `dcc-upload-submit-confirmation`。页面自然请求只记录 method/path/status；脚本没有 API、HTTP、数据库或 mock 业务动作。

## Fixture Contract

最新授权以协调目录 `execution-authorization.md` 为准。脚本读取 `environment.fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST` 及同目录 `fixture-manifest.json`，要求 `verifiedThroughRealUi=true`、`fixtureComplete=true`、环境一致并存在真实页面证据。共享 `roleFixtureVerified=false` 不再阻塞本例；不修改共享 dispatch。用户名可作为脱敏身份标签记录；密码只在运行时环境变量或根 AGENTS 授权来源内存读取，不写任务资产。配置管理员只准备本例数据，业务 U/S/L/A/D 各用独立 context 登录，绝不借管理员代替业务身份。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C01，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-01/rerun-20261020-r2-round1
- doc/tasks/20261016-dcc-e2e-01/rerun-20261020-handoff.json
- doc/tasks/20261016-dcc-e2e-01/rerun-20261020-launcher-1792460016090.json
- doc/tasks/20261016-dcc-e2e-01/rerun-20261020-evidence-safety.json
- doc/tasks/20261016-dcc-e2e-01/rerun-20261020-process-close.json
- doc/tasks/20261016-dcc-e2e-01/rerun-20261020.cjs
- doc/tasks/20261016-dcc-e2e-01/repair-20261019-r4-handoff.json
- doc/tasks/20261016-dcc-e2e-01/task.md
- doc/tasks/20261016-dcc-e2e-01/execution-log.md
- doc/tasks/20261016-dcc-e2e-01/verification-report.md
- doc/tasks/20261016-dcc-e2e-01/prepared.json
- doc/tasks/20261016-dcc-e2e-01/e2e-01.cjs
- doc/tasks/20261016-dcc-e2e-01/fixture.template.json
- doc/tasks/20261016-dcc-e2e-01/ui-session.cjs
- doc/tasks/20261016-dcc-e2e-01/available-branches.cjs
- doc/tasks/20261016-dcc-e2e-01/run-r2.cjs
- doc/tasks/20261016-dcc-e2e-01/verify-evidence.cjs
- doc/tasks/20261016-dcc-e2e-01/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-01/root-handoff.json
- doc/tasks/20261016-dcc-e2e-01/launcher-receipt.json
- doc/tasks/20261016-dcc-e2e-01/evidence-safety-check.json
- doc/tasks/20261016-dcc-e2e-01/native-account-preflight.json
- doc/tasks/20261016-dcc-e2e-01/root-workflow-resume-result.json
- doc/tasks/20261016-dcc-e2e-01/generate-signature-images.cjs
- doc/tasks/20261016-dcc-e2e-01/.gitignore
- doc/tasks/20261016-dcc-e2e-01/artifacts
