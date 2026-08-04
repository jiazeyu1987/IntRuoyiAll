# Backend API Evidence

## Scope

- Handler：统一审批中心 `POST /approval-center/tasks/review`
- Provider：`DccApprovalTaskAdapter`
- Domain service：复用 `DccControlledFileWorkflowService.approveTask/rejectTask`

## API And Data Contract

- 请求继续使用现有 `ApprovalTaskReviewReqVO`，不新增接口或字段。
- DCC 待办摘要仅在无需模块专属资料的审批节点声明 `APPROVE`、`REJECT`、`PROCESS_IN_MODULE`。
- `DOC_CONTROL_APPROVAL` 节点需要盖章 PDF、存入路径和下发范围，继续仅声明 `PROCESS_IN_MODULE`。
- DCC 审批提交将 `businessKey` 解析为受控文件 ID，将 `sourceTaskId` 作为 BPM taskId，并透传签名密码及审核意见。

## Auth Permissions Validation Error Behavior

- 正式权限、候选人、当前节点和签名密码校验继续由 `DccControlledFileWorkflowService` 执行。
- 非数字受控文件 businessKey、错误 sourceTaskType、缺 taskId/password 或未知审批结果必须 fail fast。
- 驳回原因继续由统一审批中心服务和 DCC 工作流服务校验，不返回默认成功。

## Required Config Services Fixtures Migrations

- 依赖现有 BPM、DCC 工作流与电子签名服务。
- 无 schema、迁移或配置变化。
- 单元测试使用 Mockito，不依赖数据库或运行态服务。

## BDD Scenarios

- Given 文控审核、会签审核或会签批准待办，When 审批中心请求通过或驳回，Then DCC provider 调用正式受控文件工作流服务并携带当前 taskId、密码和意见。
- Given 文控批准节点需要模块专属资料，When 审批中心生成待办摘要，Then 不声明统一快速审批能力，仅保留模块处理入口。
- Given DCC provider 收到无效来源或业务键，When 执行统一审核，Then 明确失败且不调用工作流服务。

## RED

- 待执行：`mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## GREEN

- 待执行。

## Contract Integration Verification

- 待执行 DCC adapter 定向 JUnit、审批中心相邻测试和真实页面验证。

## Observability

- 不新增日志吞噬；DCC 工作流服务现有业务异常由统一审批接口原样暴露。

## Blockers And Downstream Skills

- 若 Maven 受共享 `target` 或文件系统损坏影响，按 Windows Maven 门禁记录环境阻塞，不改用 mock 成功或跳过测试。

