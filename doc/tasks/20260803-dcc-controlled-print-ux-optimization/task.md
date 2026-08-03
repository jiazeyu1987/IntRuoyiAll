# DCC 受控打印 UX/追溯优化

## Task Goal

优化 DCC 文控“受控打印”的前端操作反馈和业务追溯体验：打印完成后明确展示打印结果，记录区自动定位并高亮本次记录，无打印权限时给出清晰说明，打印表单提供结构化接收部门和常用位置选择，多份打印显示逐份副本编号，并显性展示当前直接打印/无需审批策略；完成后执行真实 Playwright E2E 验证。

## Scope

- 仅处理 DCC 受控打印相关页面、API 类型/展示、静态契约和真实 E2E。
- 不修改其它 DCC 上传、发布、分发、培训、MES/eDHR 或非受控打印场景。
- 不用 admin 完成业务 E2E，不用 API-only/SQL 创建打印记录，不 mock 打印成功。
- 若后端已有足够数据，则优先以前端展示和响应派生的正式副本编号实现；若发现缺少正式数据契约，会先记录 BLOCKED，不用临时绕过。

## Milestones

1. 读取规则和经验门禁，定位现有受控打印前后端实现。
2. 记录 BDD，新增前端静态契约 RED 覆盖成功反馈、记录高亮、权限提示、结构化字段、副本编号和审批策略展示。
3. 实现最小正式 UX 优化，必要时补充 API 类型/后端响应字段但不改业务状态。
4. 运行 GREEN：静态契约、类型检查或定向验证。
5. 运行真实 Playwright E2E：正向打印、结果弹窗/查看记录/高亮/副本编号/策略提示、负向权限提示。
6. 输出 `verification-report.md`，记录 PASS/BLOCKED 证据。

## BDD Scenarios

BDD: 打印完成后展示可审计结果 -> Given 有打印权限的非 admin 用户打印当前 ACTIVE 受控文件 When 页面提交受控打印 Then 页面显示成功结果弹窗 And 弹窗展示打印编号、份数、打印人、打印时间、副本编号和直接打印策略 And 用户可点击查看打印记录定位到本次记录。

BDD: 最新打印记录自动定位高亮 -> Given 用户完成一次受控打印 When 用户点击查看打印记录或记录区刷新 Then 打印记录表自动滚动到本次记录 And 最新记录以高亮样式展示一段时间。

BDD: 无打印权限时给出明确原因 -> Given 非 admin 用户无同一文件 PRINT 权限 When 用户进入同一 ACTIVE 文件受控浏览或详情页 Then 页面不显示受控打印按钮 And 显示只读权限提示说明当前用户无受控打印权限或当前文件类别不允许打印。

BDD: 打印表单结构化减少追溯歧义 -> Given 用户打开受控打印表单 When 填写接收部门和使用位置 Then 接收部门可从组织部门选择 And 使用位置可从常用位置选择或输入新位置 And 提交后记录中保留标准化文本。

BDD: 多份打印显示逐份副本编号 -> Given 用户打印份数大于 1 When 打印件、成功弹窗和记录区展示打印结果 Then 每份副本都有可见副本编号或编号范围，用于后续盘点追溯。

BDD: 预览态不被打印记录辅助接口阻断 -> Given 有受控文件预览权限的用户从受控文件列表点击预览 When 详情页以 `viewer=1` 只读预览态初始化 Then 页面加载受控文件预览和基础详情 And 不请求未渲染的受控打印记录接口 And 非预览追溯详情中的打印记录接口失败只在记录区显示真实错误，不阻断整页详情。

BDD: 预览态不加载未渲染的分发和流程打印辅助数据 -> Given 有受控文件预览权限的用户从受控文件列表点击预览 When 详情页以 `viewer=1` 只读预览态初始化 Then 页面不请求纸质分发记录 And 不请求流程打印模板数据 And 非预览详情仍保留对应功能区的数据加载。

## Expected Verification

- 前端静态契约先 RED 后 GREEN。
- `pnpm ts:check` 或任务范围内等价类型检查通过。
- 真实 Playwright E2E 使用非 admin 账号和真实页面路径，通过环境变量注入密码。
- 只读 API/DB 证明最终打印记录、份数、打印人、当前有效版本和直接打印状态。

## Applicable Gates

- `docs/e2e-rules.md#DCC 受控打印门禁`
- `docs/e2e-rules.md#真实 E2E 主链路与扩展诊断产物隔离门禁`
- `docs/frontend-development.md#前端静态契约隔离门禁`
- `docs/login-access.md#E2E 与数据约定`
- `docs/task-closeout-rules.md#任务验证脚本保留门禁`

## Current Status

completed

## Follow-up Bug: Preview Requests Unrendered Auxiliary Routes

- Symptom: 用户在受控文件中点击预览时，页面提示 `请求地址不存在:admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records`。
- Similar issue: 继续排查发现 `viewer=1` 初始化仍会请求纸质分发记录和流程打印模板数据，但预览态不渲染纸质分发弹窗和流程打印动作。
- Expected: 只读预览态只加载预览和基础追溯信息，不应请求未渲染的受控打印记录区、纸质分发记录或流程打印模板；非预览追溯详情仍按功能区加载对应数据，并在对应区域展示真实加载错误。
- Scope: 仅修正 DCC 受控文件详情页初始化加载边界和静态契约；不改后端接口契约、不新增 fallback、不隐藏真实错误。

## Verification Summary

- Follow-up bug 修复：viewer 只读预览态不再请求未渲染的 `controlled-print/records`、纸质分发记录和流程打印模板辅助数据；非 viewer 详情/追溯页仍加载对应功能区数据，记录接口错误只显示在记录区，不阻断文件详情或预览初始化。
- Follow-up RED：`node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> FAIL，旧逻辑缺少 `!viewerMode.value` 加载门禁。
- Similar RED：`node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js` -> FAIL，旧逻辑在 viewer 预览态仍会请求 `getPaperDistributionRecords` 和 `getActiveApprovalPrintTemplate`。
- Follow-up GREEN/REGRESSION：受控打印静态契约、受控打印 UX 静态契约、受控浏览静态契约和 `pnpm ts:check` 均通过；相似问题源码/测试改动已由并发基线提交 `03646727b` 纳入历史，当前任务记录不改写历史。
- Continuation evidence：`node IntRuoyiFronted\tests\e2e\dcc-controlled-print-static.spec.js`、`validate_bug_regression.py` 和 `task_closeout.py --mode preview` 均 PASS，确认补充证据和长期门禁可保留。
- 真实 Playwright E2E：`node doc\tasks\20260803-dcc-controlled-print-ux-optimization\dcc-controlled-print-ux-real.e2e.cjs` -> PASS，最终打印记录 ID `9`，打印编号 `DCCP-20260803024527-7C69A88D`。
- 当前有效版证明：目标文件 `2054545668044070287` / `CODX-DCC-ORIG-20260802101521` / `V1.0` 为 `ACTIVE`，master 当前有效指针为 `2054545668044070287`。
- 正向 UX 证明：成功弹窗展示打印编号、副本编号、份数、打印人和直接打印策略；“查看打印记录”定位并高亮本次记录。
- 负向 UX 证明：`zhangkeying` 可从受控浏览进入同一文件追溯详情，但浏览页与详情页 `visiblePrintButtonCount=0`，详情页显示“无受控打印权限”说明。
- 只读 API/DB 证明：打印记录、份数、打印人、文件编号、版本和 `DIRECT_PRINTED` 状态与页面一致。
- 回归验证：任务静态契约、前端受控打印静态契约、受控浏览静态契约、`pnpm ts:check`、后端 `DccControlledPrintContractTest` 均通过。
- Cleanup：`task-closeout-cleanup` preview/apply 均通过，删除旧截图、临时 `frontend-feature-evidence.md` 和 `runtime-jar-inspect` jar 检查产物；保留 task/execution/verification、bug regression 证据、真实 E2E 脚本/结果和最终截图。
- Push Result：`git push origin int_main` 已成功，远端 `int_main` 更新到 `61d406ca6`；当前任务可标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；成功反馈、记录定位、权限说明、副本编号和直接打印策略均通过正式页面/API 字段验证；follow-up 从页面渲染边界修复预览态错误请求，覆盖打印记录、纸质分发记录和流程打印模板，不改接口、不降级。
- `是否存在临时补丁或绕过`：否；业务打印记录由真实页面路径创建，API/DB 仅用于只读核验。

## Cleanup Keep

- doc/tasks/20260803-dcc-controlled-print-ux-optimization/bug-regression-evidence.md
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/dcc-controlled-print-ux-real.e2e.cjs
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/dcc-controlled-print-ux-real-e2e-result.json
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/dcc-controlled-print-ux-static.spec.cjs
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-window-20260802184519.png
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-records-20260802184519.png
- doc/tasks/20260803-dcc-controlled-print-ux-optimization/controlled-print-ux-negative-20260802184519.png

## Cleanup Result

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-controlled-print-ux-optimization --mode preview` -> PASS, no blocked paths or warnings.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-dcc-controlled-print-ux-optimization --mode apply` -> PASS, deleted only cleanup-classified task-local temporary artifacts.

## Push Result

- Previous blocker: GitHub HTTPS proxy `127.0.0.1:7890` was not listening, so earlier push attempts failed and task stayed blocked.
- Recheck: `Test-NetConnection 127.0.0.1 -Port 7890` -> `TcpTestSucceeded=True`.
- Pre-push: branch `int_main`, remote `origin=https://github.com/jiazeyu1987/IntRuoyiAll.git`, largest pending blob about `228033` bytes.
- Final push: `git push origin int_main` -> PASS, `f08fa2a2d..61d406ca6 int_main -> int_main`.
- Impact: local task commits and concurrent baseline commits are now available on `origin/int_main`; task status is completed.
