# Execution Log

## User Intent

- 用户要求审批中心“上传审批”列表在操作列增加快速“审批”按钮，无需先进入详情。
- 现有进入详情后审批的能力必须保留。

## BDD

- BDD: 上传审批行内快速审批 -> Given 当前用户在审批中心待办列表看到可直接审核的 DCC 上传审批任务，When 用户点击该行“审批”，Then 页面打开现有审批确认弹窗并可通过正式统一审核接口提交。
- BDD: 详情审批入口保持不变 -> Given 当前待办行原本支持“处理”或“打开”进入详情，When 增加快速审批入口后，Then 原详情入口仍可见且路由行为不变。
- BDD: 非待办或不支持直接审核的行不误显示 -> Given 当前视图不是待办或任务不满足统一审核条件，When 表格渲染操作列，Then 不显示会触发无效提交的快速审批按钮。
- BDD: 最终文控批准保留模块处理 -> Given 当前节点需要盖章 PDF、存入路径和下发范围，When 审批中心渲染该待办，Then 不显示无法收集正式资料的快速审批按钮，并保留“处理”详情入口。
- BDD: DCC 统一审核委托正式工作流 -> Given 当前 DCC 待办支持快速审批，When 用户提交通过或驳回，Then DCC provider 将 taskId、受控文件 ID、签名密码和意见交给正式 DCC 工作流服务校验并执行。

## Command Intent

- 读取 `frontend-feature-delivery`、前端开发、E2E、任务收尾及 PowerShell/Git 门禁。
- 定位 `src/views/approval-center/index.vue`、审批中心 API 和相邻静态契约。
- 创建任务专用静态契约并执行 RED/GREEN。

## Milestone Updates

- 2026-08-04：M1 完成；已确认审批中心页面已有统一审核弹窗和 `/approval-center/tasks/review` 正式接口。
- 2026-08-04：根因确认；DCC TODO 摘要当前只声明 `PROCESS_IN_MODULE`，因此 `canReview(row)` 为 false，截图黄框位置不渲染行内按钮。
- 2026-08-04：设计边界确认；`DOC_CONTROL_APPROVAL` 需要盖章 PDF、确认目录及下发范围，继续只允许进入详情处理；前三个审核/批准节点可复用统一签名弹窗。

## Verification Evidence

- 待执行。

## Blockers

- 当前仓库在任务开始前存在并行脏改动且本地分支领先 `origin` 2 个提交；按项目规则需先保存既有改动基线，并确保本任务仅选择性提交自有文件。
