# 前端真实 E2E 子 agent 证据：eDHR 审批追踪关闭闭环

- 生成时间：2026-07-02T01:10:27.743Z
- 前端 worktree：D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3
- 前端启动入口：`http://localhost:8081`（本 worktree `.env.local` 的 `VITE_PORT=8081`；启动命令 `pnpm dev`）
- 真实 E2E 复跑命令：`pnpm e2e:edhr:approval-tracking`
- 静态语法检查命令：`pnpm e2e:edhr:approval-tracking:check`
- 产物目录：`test-results/edhr-approval-tracking/`（截图、trace、result.json 均不提交）
- 当前状态：FAIL
- SUBMITTED 负向输入模式：draft-submit

## BDD

- BDD: Fresh eDHR 草稿创建 -> Given 真实测试租户工单/任务上下文, When 执行人从生产报工入口新增并打开 eDHR, Then open-or-create 必须返回 created=true 且记录 executionId/executionCode。
- BDD: DRAFT 禁止归档 -> Given 草稿 eDHR 执行记录, When 执行人通过真实 UI 查看详情, Then 页面不暴露可执行归档动作且不发起归档接口。
- BDD: SUBMITTED 禁止归档 -> Given 已提交待审批记录或可提交草稿二选一输入, When 用户通过真实 UI 查看待审批详情, Then 页面提示审批关闭后才可归档且不发起归档接口。
- BDD: FIELD_CHANGE 字段审计 -> Given 审批通过流程 fresh 草稿存在可编辑字段, When 执行人修改字段、填写原因并输入 FIELD_CHANGE 签名密码, Then 保存、verify-chain 和详情均返回/展示 VALID FIELD_CHANGE 证据。
- BDD: 提交后进入审批 -> Given 可提交草稿和执行人签名密码, When 执行人通过真实 UI 提交, Then 记录进入待审批状态并在审批列表可查询。
- BDD: 审批详情真实 API 展示 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表点击执行编号, Then 前端进入 `/mes/pro/feedback/edhr-approval/detail` 并等待真实 `/mes/pro/batch-record-execution/approval-detail` 响应展示同一执行编号。
- BDD: 审批通过关闭 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表真实 UI 点击通过并输入签名密码, Then 记录进入已关闭状态。
- BDD: 审批驳回留痕 -> Given 脚本提交出的真实 BPM 待办和业务可见执行编号, When 审批人从审批列表真实 UI 点击驳回并输入签名密码和驳回原因, Then 记录进入已驳回状态并保留原因。
- BDD: 我已审批列表可追溯 -> Given 本轮审批人刚完成通过和驳回动作, When 审批人打开 `/mes/pro/feedback/edhr-approval?tab=done` 并查询执行编号, Then 前端等待真实 `/mes/pro/batch-record-execution/approval-done-page` 响应并展示已关闭或已驳回记录。
- BDD: 关闭后可归档 -> Given 刚通过审批关闭的真实执行记录, When 授权用户通过真实 UI 输入封存密码生成归档, Then 前端发起归档接口并展示 sha256、signatureHash、approvalSnapshotId 和 approvalSnapshotHash 证据。
- BDD: 归档版本可查看 -> Given 刚生成的 SEALED 归档, When 用户点击执行详情中的“查看版本”, Then 前端请求真实 `/mes/pro/batch-record-execution-archive/page` 并在“归档版本”弹窗展示同一归档版本和 sha256。
- BDD: 受控归档下载 -> Given 刚生成的 SEALED 归档, When 用户通过真实 UI 点击下载归档, Then 浏览器必须从 `/mes/pro/batch-record-execution-archive/download` 获取非空下载文件，保存产物并重算 SHA-256，且 downloadedSha256 必须等于归档响应 sha256。
- BDD: 追踪与签名查询 -> Given 本轮通过与驳回流程已产生 BPM、人员、时间和原因事件, When 用户通过真实追踪/签名页查询, Then 页面展示 BPM 任务、人员、时间、原因和签名含义。

## RED

- RED: `pnpm e2e:edhr:approval-tracking` -> FAIL, page.waitForResponse: Timeout 60000ms exceeded while waiting for event "response"
- 影响：真实 UI E2E 未放行；不得提交为通过。
