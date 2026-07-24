# Task: DCC 审批路线新增按钮不可点击排查

## Goal

排查 `http://localhost:8081/dcc/controlled-file/routes` 页面中“新增路线”按钮不可点击的真实原因，明确问题属于前置数据未满足、权限控制、前端交互缺陷，还是后端返回导致的页面状态异常，并在确认属于代码缺陷时补齐最小修复与回归验证。

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则先明确阻塞原因和影响，再继续当前排查。
- 在修改生产代码前先创建任务文档和执行日志。
- 使用真实前端入口 `http://localhost:8081`、真实登录态和真实页面路径复现“新增路线”不可点击。
- 记录按钮的实际状态、触发条件、页面数据前置条件以及控制它的代码路径。
- 若属于代码缺陷，必须先补 RED 证据，再做最小修复并完成 GREEN 回归。
- 若属于业务前置条件或权限限制，必须记录精确前置条件与影响，不得用 fallback、mock 或绕过路径掩盖。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-route-last-process-key-flag-toggle-e2e/task.md`
- Status before this task: blocked.
- Impact: 该任务阻塞点是 `ROUTE-XLSX-00001` 缺少真实 BOM 主数据，范围在 MES 工艺路线启停验证，不阻塞当前 DCC 审批路线按钮排查。

## Milestones

- [x] M1: 检查上一条前端任务状态并确认当前任务可继续。
- [x] M2: 在排查前创建当前任务文档与执行日志。
- [x] M3: 通过真实页面路径复现“新增路线”不可点击并记录 RED 证据。
- [x] M4: 定位按钮不可点击的根因与控制代码。
- [x] M5: 结论确认当前问题不是代码缺陷，无需修改生产代码。
- [x] M6: 完成 GREEN 验证，更新任务文档并仅提交当前任务相关改动。

## Expected Verification

- Playwright 通过真实登录进入 `http://localhost:8081/dcc/controlled-file/routes`。
- 能明确记录“新增路线”按钮是禁用、隐藏、无响应还是被其他元素遮挡。
- 能给出导致按钮不可点击的精确前置条件或代码根因。
- 若有代码修复，相关 RED/GREEN 证据记录到 `execution-log.md`，且目标验证命令通过。

## Current Status

Completed. 已确认“新增路线”按钮在未选择文件类别时会被前端主动禁用；真实页面加载到 48 个可选类别后，只要先选择一个类别，按钮就会立刻恢复可点击并可继续打开新增路线弹窗。当前未发现权限缺失、接口失败或前端运行时错误。

## Blocker And Impact

- Blocker: none remaining.
- Impact: 当前影响不是系统缺陷，而是页面缺少显式提示，用户若未先选择文件类别，会误以为“新增路线”按钮失效。

## Final Verification Result

- Real page reproduction:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-approval-route-add-button run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-approval-route-add-button-disabled\scripts\inspect-add-route-button.mjs`
  - Result: PASS
- Verified facts:
  - 初始进入 `http://127.0.0.1:8081/dcc/controlled-file/routes` 时，“新增路线”按钮为 disabled。
  - 页面成功加载 48 个 DCC 文件类别选项，请求 `GET /admin-api/dcc/file-categories` 返回 `200` 且 `code=0`。
  - 选择首个类别“产品技术要求”后，按钮 disabled 状态解除。
  - 点击按钮后页面出现可见弹窗，未捕获 page error 或接口失败。
- Code root cause:
  - `src/views/dcc/controlled-file/routes/index.vue` 直接使用 `:disabled="!queryParams.categoryId"` 控制“新增路线”按钮，因此未选择类别时按钮不能点击是当前前端的显式设计。
