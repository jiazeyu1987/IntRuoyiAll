# Frontend Feature Evidence

## Feature Goal

在审批中心待办列表为 DCC 上传审批任务增加行内“审批”入口，复用现有审核弹窗和统一审核 API。

## Non-Goals

- 不删除或修改详情页审批能力。
- 不修改后端接口、审批状态机、权限模型或 DCC 详情路由。
- 不为不支持统一审核的任务增加 fallback。

## Requirements And Acceptance

- AC1：无需模块专属资料的 DCC 上传审批待办行显示“审批”按钮。
- AC2：点击“审批”打开现有审核确认弹窗。
- AC3：提交继续调用 `/approval-center/tasks/review`。
- AC4：现有“处理/打开”详情入口继续保留。
- AC5：非待办或不可直接审核任务不显示快速审批按钮。

## UI Entry Points And Owned Files

- Route：`/approval-center/todo`
- Page：`IntRuoyiFronted/src/views/approval-center/index.vue`
- Focused contract：`IntRuoyiFronted/tests/e2e/approval-center-upload-quick-review-static.spec.js`

## API Contracts And Data States

- 列表：`GET /approval-center/tasks/page`
- 审核：`POST /approval-center/tasks/review`
- 状态：待办且任务由后端正式声明 `APPROVE` 与 `REJECT` 时允许快速审批；最终文控批准节点因需模块专属资料而不声明该能力。

## BDD Scenarios

- Given 可直接审核的 DCC 上传审批待办，When 点击行内“审批”，Then 打开现有审核确认弹窗。
- Given 行原有详情入口，When 增加快速审批，Then “处理/打开”仍存在且行为不变。
- Given 非待办或不可直接审核任务，When 渲染操作列，Then 不显示快速审批按钮。

## RED

- 待执行：`node tests/e2e/approval-center-upload-quick-review-static.spec.js`

## GREEN

- 待执行。

## Responsive And Accessibility

- 使用现有 Element Plus link button，保持操作列可换行/可点击语义和现有视觉规范。
- 保留清晰可见按钮文案“审批”，不依赖仅图标识别。

## Loading Empty Error Permission Checks

- 复用现有审核弹窗提交 loading、错误提示和权限判断。
- 空列表无按钮。
- 不改变现有列表加载错误、模块错误和详情跳转错误处理。

## E2E Or Component Verification Path

- Playwright 登录真实前端，进入审批中心待办，定位 DCC 上传审批目标行。
- 断言同一行存在“审批”与原详情入口。
- 点击“审批”，断言现有审核确认弹窗可见；不提交业务写入时关闭弹窗。

## Blockers And Follow-Up Skills

- 若缺少真实 DCC 上传审批待办或测试账号，真实 E2E 必须记录为 BLOCKED，不能用 API-only 或 mock 替代。
