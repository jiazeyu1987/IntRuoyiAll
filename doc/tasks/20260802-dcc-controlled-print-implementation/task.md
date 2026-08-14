# DCC 受控打印功能补齐/修复

## Task Goal

补齐 DCC 文控“受控打印”正式功能，使有打印权限的非 admin 用户可从当前有效受控文件的受控浏览/详情页发起打印，填写用途、份数、接收部门、使用位置后生成带受控信息的打印件，并形成可追溯打印记录；无打印权限用户必须被按钮隐藏、禁用或明确拒绝。

## Scope

- 仅处理 DCC 受控打印入口、后端记录、权限、水印/打印信息展示、打印记录查询与真实 Playwright E2E。
- 不修复其它 DCC 上传、升版、发布、审批、OnlyOffice、MES/eDHR 或非受控打印场景。
- 不使用 admin 账号完成业务 E2E，不用 API-only/SQL 创建打印记录，不 mock 上传或打印成功。
- 当前系统未发现独立受控打印审批链路；本任务按“直接受控打印 + 权限 + 水印 + 记录”补齐。若后续产品要求打印审批，需另立审批策略任务。
- 2026-08-02 用户已授权处理阻塞 DCC 受控打印 E2E 的 SHOWROOM 审批适配器运行态注册问题；该修复仅限恢复当前本机 `int_main` 最新 jar 启动，不扩大到其它 SHOWROOM 业务场景。

## Milestones

1. 建立任务文档、BDD 场景、RED/GREEN 证据结构。
2. 定位现有 DCC 受控文件、权限、菜单、前端入口和测试结构。
3. 写 RED：后端/数据库/前端最小契约证明缺少受控打印表、API、页面入口和字段。
4. 实现最小正式链路：数据库记录、后端 API、权限、前端按钮/表单/记录展示/打印件受控信息。
5. 运行 GREEN：后端定向测试、数据库/前端契约、类型检查或局部静态验证。
6. 修复并验证 SHOWROOM 审批适配器运行态注册阻塞，使包含受控打印功能的新 jar 可在 `48081` 启动。
7. 恢复本机运行态后跑真实 Playwright E2E：有权限打印、无权限阻断、只读 API/DB 核验。
8. 更新 verification-report.md，并按 closeout 规则清理任务自有临时产物。

## BDD Scenarios

BDD: 有权限用户打印当前有效受控文件 -> Given 任务自有受控文件为当前 ACTIVE 版本 When 有打印权限的非 admin 用户从受控浏览或详情页点击受控打印并填写必填信息 Then 页面生成带打印编号、文件编号、版本、打印人、打印时间的受控打印件 And 打印记录中出现本次记录。

BDD: 系统拒绝非当前有效版本打印 -> Given 同一文件存在非当前 ACTIVE 版本 When 用户尝试对非当前有效版本发起受控打印 Then 请求被拒绝 And 页面或接口明确提示只能打印当前有效版本。

BDD: 必填信息缺失时不能生成打印记录 -> Given 用户打开受控打印表单 When 打印用途、份数、接收部门或使用位置缺失 Then 表单不提交 And 后端不生成打印记录。

BDD: 无打印权限用户被阻断 -> Given 用户可登录但没有受控打印权限 When 用户进入同一 ACTIVE 文件的受控浏览或详情页 Then 受控打印入口不可用、隐藏或点击后明确权限拒绝 And 不生成打印记录。

BDD: 打印动作可追溯 -> Given 用户已完成一次受控打印 When 审计人员查看打印记录或只读核验接口/数据库 Then 可看到打印记录 ID、文件编号、版本、份数、打印人、打印时间、审批状态或直接打印状态。

BDD: SHOWROOM 审批适配器必须被正式注册 -> Given BPM 统一审批平台声明 SHOWROOM 模块必须接入 When 本机 `int_main` 后端启动并构建 `ApprovalTaskProviderRegistry` Then Spring 必须发现并注册 `ShowroomApprovalTaskAdapter` And `ApprovalModuleIntegrationGuard` 不再抛出 `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`。

## Expected Verification

- 后端定向 Maven 测试覆盖当前有效版本、必填校验、权限拒绝、打印记录和打印件信息。
- 数据库/迁移契约证明存在受控打印记录表和必要字段。
- 前端静态契约证明受控浏览/详情页有“受控打印”入口、必填字段和记录展示，不复用“流程打印”。
- Playwright 真实页面验证使用非 admin 账号、环境变量注入密码、任务自有 ACTIVE 文件，不用 API-only 创建记录。
- 只读 API/DB 核验打印记录、打印份数、打印人、状态、文件版本。

## Applicable Gates

- `docs/e2e-rules.md#E2E 脚本入口存在性门禁`
- `docs/e2e-rules.md#DCC 文控审批处理入口门禁`
- `docs/e2e-rules.md#DCC 受控打印门禁`
- `docs/e2e-rules.md#真实 E2E 主链路与扩展诊断产物隔离门禁`
- `docs/e2e-rules.md#Playwright 浏览器可执行文件门禁`
- `docs/e2e-rules.md#Element Plus 下拉选择门禁`
- `docs/frontend-development.md#前端静态契约隔离门禁`
- `docs/database-rules.md#租户和菜单权限`

## Current Status

ready_for_closeout

## Verification Summary

- 2026-08-02 23:50，真实 Playwright E2E 已在 `8081/48081` 主运行态 PASS，后端 health `UP`，前端 HTTP `200`。
- 最终打印记录 ID `3`，打印编号 `DCCP-20260802235038-09C2EEA9`，文件 `CODX-DCC-ORIG-20260802101521`，版本 `V1.0`，状态 `DIRECT_PRINTED`，打印人 `王思雨 (wangsiyu)`，份数 `2`。
- 只读 DB 证明文件 `2054545668044070287` 为 `ACTIVE`，master `current_active_controlled_file_id` 指向该文件，`publishedFileId=9198354916366`，`stampedFileId=9198354916366`。
- 无打印权限账号 `zhangkeying` 登录同一受控浏览页后可见文件但 `visiblePrintButtonCount=0`，覆盖权限阻断。
- 当前仍未执行 closeout cleanup、提交和推送；任务状态标记为 `ready_for_closeout`。

## Resolved Blocker

- SHOWROOM 运行态阻塞已解除：当前 `48081` 进程运行 `E:\IntRuoyi\output\runtime\int_main\backend\yudao-server-exec-20260802-220742.jar`，受控打印接口在真实页面链路中成功执行。
- 为满足正向打印权限，按用户授权新增最小类别级规则 `dcc_file_category_permission_rule.id=2625`：`category_id=907233`、`action_type=PRINT`、`subject_type=USER`、`subject_id=910250`。
- 未使用 admin、未通过 API-only 或 SQL 创建打印记录、未 SQL 改文件状态。

## Closeout Progress

- `task-closeout-cleanup` preview/apply 已完成，仅删除本任务 5 张旧截图；最终 E2E 脚本、结果 JSON、3 张验收截图、`task.md`、`execution-log.md`、`verification-report.md` 均保留。
- 已按 `project-experience-consolidation` 将 DCC 受控打印 E2E 门禁沉淀到 `docs/e2e-rules.md#DCC 受控打印门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- 仍保持 `ready_for_closeout`：当前 `int_main` 工作区存在多个其它任务的未提交改动且分支已 ahead `origin/int_main`，本任务未执行宽泛基线提交、提交或推送，避免混入无关并发任务产物。

## Cleanup Keep

- doc/tasks/20260802-dcc-controlled-print-implementation/dcc-controlled-print-real.e2e.cjs
- doc/tasks/20260802-dcc-controlled-print-implementation/dcc-controlled-print-real-e2e-result.json
- doc/tasks/20260802-dcc-controlled-print-implementation/controlled-print-window-20260802155031.png
- doc/tasks/20260802-dcc-controlled-print-implementation/controlled-print-records-20260802155031.png
- doc/tasks/20260802-dcc-controlled-print-implementation/controlled-print-negative-20260802155031.png

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，补正式受控打印入口、记录和权限链路。
- `是否存在临时补丁或绕过`：否。
