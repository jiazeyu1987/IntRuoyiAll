# E2E-21 审批/历史固定正文

## Goal

独立验证E2E-21，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 仅工程未收尾；最新rerun-20261020-r2-round4全原10分支本次真实UI PASS（10/0/0/0），见rerun-20261020-r2-round4/handoff.json。旧PASS未计，历史失败保留；禁止Git、未cleanup apply，HOLD。

## 20261020 凭据修复与本次定向复验

- 主修复记录：repair-20261020-credentials/task.md、verification-report.md、handoff.json。
- 根因：第四轮凭据注入将原UTF-8字节按UTF-16解码；原账号及密文有效，无需重置密码。
- RED/GREEN：公开样本编码契约测试先失败后通过。本次真实UI六原角色登录、原10分支PASS，10份trace与31张截图已验证；浏览器及context全部关闭。
- 边界：仅本例测试资产修复；未改产品、账号或服务。TASK-FIXED沿原真实UI保留的已完成任务链接复验，不声称存在新待办。原四轮失败证据不覆盖。

## 20261019-r2 本轮文档整理

- 目标：只更正旧报告与 Root 最新业务验收冲突，保留旧记录并注明替代证据。
- Given Root 已有 10 参数 PASS 而旧报告仍阻塞；When 核对原结果、trace与截图引用并归档旧报告；Then 当前结论一致、历史不丢失、业务与工程状态分离。
- 验证：仅文档结构/引用/参数计数/证据哈希检查，见repair-20261019-r2-validation.json；不运行E2E、Maven、类型检查或构建。
- 已通过分支不重跑；历史 prepared/gate/runner/manifest 中间状态不是当前验收。详见verification-report.md历史替代表。
- Root接受时间及历史运行SHA保持原值；本轮不将新一轮13/22/24待部署修复冒充已验收。

## BDD

- Given 任务及历史记录对应旧版，当前正式版内容已不同
- When 分别从审批任务、审批历史及版本历史打开正文并切换查看
- Then 显示对应任务/记录的固定正文及明确状态，不跳到当前版；检入小版本仅显示“草稿”，不显示小版本号；各入口仍按相应任务/历史权限校验。

- Given 已办页面生成APPROVAL_HISTORY正文上下文，When 原历史账号点击A/1的查看，Then 从实际URL解码并校验原申请和该账号任务身份，再验证A正文命中、B正文不命中；不制造上下文或补旧参数。

- Given 本例三个正向账号缺独立VIEW矩阵且两名审批人缺项目VIEW，When 本例配置人维护本例类别、原项目负责人为TASK/HISTORY添加项目查看，Then 保存回读保留原OWNER、仅补正向只读前置；三个拒绝账号不新增权限，不授作废或管理权。随后本人fresh登录验收。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色数据前置。
2. [x] 可复跑Playwright脚本、语法与结构检查、prepared.json。
3. [x] 本例真实UI fixture及原10参数已验证；任务自有资产保留，工程清理另行收口。
4. [x] Root review完成：原10参数PASS，九个trace与正文/拒绝证据已接受。
5. [ ] 工程收口：清理审核与cleanup apply、Git提交/推送；本轮均不执行。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 参数化子用例与正式入口

| 子用例 | Given | 页面入口与动作 | 页面断言 |
|---|---|---|---|
| E21-TASK | 任务自有 DCC 文件审批仍在目标审批人待办，正文为旧申请版本，当前正式版正文标记不同 | 真实登录目标审批人；从可见菜单进入“审批中心”→“待办”；按任务自有标记定位行，点击同一行“查看” | URL 为 `/dcc/controlled-file/detail/:id?...handling=approval...taskId=...`；正文身份显示目标旧版本和状态；正文渲染完成且内容标记匹配任务版本；不得显示当前正式版标记 |
| E21-HISTORY | 上述审批轮次已完成，且该历史任务对历史账号可读 | 真实登录历史账号；从“审批中心”→“已办”进入目标行，点击同一行“查看” | URL 为固定历史上下文；正文身份/状态与历史轮次一致；正文内容固定为历史版本，不能跟随当前正式版 |
| E21-VERSION | 同一文件版本历史同时保留正式旧版、当前版及检入工作小版本 | 真实页面从历史详情“版本历史”表定位旧版/工作稿行，点击同一行“查看详情”；必要时在版本选择器切换后重新读取 | viewer/traceability 只读页显示所选版本和状态；检入工作稿只显示“草稿”，不显示 `A/1-1` 等小版本号；切换后正文标记随所选版本变化且不可串版 |

正式路由/组件依据：`/approval-center/todo`、`/approval-center/done` 使用 `src/views/approval-center/index.vue`；DCC 详情使用 `/dcc/controlled-file/detail/:id`，处理态由 `handling=approval&from=approval-center&processInstanceId&taskId` 锁定；版本历史“查看详情”进入 `viewer=1` 并携带精确 `bodyReadContext`；受控浏览版本追溯使用 `traceability=1&from=browser`。正文容器为 `data-testid="dcc-content-version-identity"`、`data-testid="protected-viewer-text-tools"` 或可见 `canvas.protected-viewer-canvas`。

角色和数据前置：必须由 Root 只读确认任务审批人、历史记录可读账号、版本历史阅读账号及其租户/项目/目录权限；不得用 `admin` 冒充多角色。任务数据必须由 Root 按 `DCC241016C21` 准备或确认，至少包含旧审批正文、当前正式版差异标记、可辨识工作稿和可访问版本历史。缺少任一独立角色、审批任务/历史记录、当前版与旧版正文差异或页面菜单权限时，本例标记 BLOCKED。

## 脚本与运行参数

- 脚本：`doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs`
- 静态语法：`node --check doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs`
- 准备态预检：`node doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs`（gate 非 `execute` 时不得启动浏览器）
- Root 放行后命令：`node doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs`；账号、租户、任务标记、版本标记仅从 `DCC_E2E21_*` 环境变量读取，密码不写入脚本或证据。
- Root review 修正：三类正文入口均强制读取真实 PDF/TEXT 正文搜索标记，旧标记必须命中、当前正式版标记必须未命中；空标记、仅 canvas 非空或仅版本标题均不能通过。执行先从页面打开当前版，再打开旧任务/历史/版本正文，证明当前版正文与固定旧正文不同且旧正文重复读取不漂移。
- 2026-10-16 授权更正：先读取 `doc/tasks/20261016-dcc-24-e2e-coordination/execution-authorization.md` 和最新 `dispatch.json`；`fixtureVerificationMode=PER_CASE_REAL_UI_MANIFEST`，本例 `gate=execute`、`accountSetupGate=authorized`。仅本例UI manifest核验后继续，不修改共享dispatch的 `roleFixtureVerified`。
- 权限与草稿分支：任务待办、审批历史、版本历史各有独立拒绝账号，拒绝必须有真实页面明确提示且正文 DOM/canvas 不存在；检入草稿逐列断言“版本=草稿”、固定状态文案、原始小版本号不出现在可见行。
- 证据与凭据：每个分支独立保存 `result.json`、`trace.zip`、最终截图；trace 在成功登录后才开始，失败登录会先导航空白页再生成脱敏 trace。所有凭据只从环境变量读取，不进入脚本、trace 或结果。

## 设计约束检查

- 只改本目录测试资产，保护现有源码；不重启服务、不跑共享Maven/构建、不改生产源码/共享.env、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C21，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-21/repair-20261020-credentials
- doc/tasks/20261016-dcc-e2e-21/report-history
- doc/tasks/20261016-dcc-e2e-21/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-21/repair-20261019-r2-validation.json

- doc/tasks/20261016-dcc-e2e-21/business-ui.cjs
- doc/tasks/20261016-dcc-e2e-21/fixture-setup-driver.cjs
- doc/tasks/20261016-dcc-e2e-21/root-integrated-result.json
- doc/tasks/20261016-dcc-e2e-21/runs/2026-10-18T21-20-32-738Z

- doc/tasks/20261016-dcc-e2e-21/task.md
- doc/tasks/20261016-dcc-e2e-21/execution-log.md
- doc/tasks/20261016-dcc-e2e-21/verification-report.md
- doc/tasks/20261016-dcc-e2e-21/prepared.json
- doc/tasks/20261016-dcc-e2e-21/e2e-21-real.cjs
- doc/tasks/20261016-dcc-e2e-21/redact-trace.py
- doc/tasks/20261016-dcc-e2e-21/execution-input.template.json
- doc/tasks/20261016-dcc-e2e-21/gate-check.json
- doc/tasks/20261016-dcc-e2e-21/fixture-manifest.json
