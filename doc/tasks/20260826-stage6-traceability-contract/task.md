# Stage6 Traceability Contract

## Goal

确保 Stage6 前后端使用统一的只读追溯契约：只接收 simulationRunId，调用 /stage6-idpr，消费 Stage5 已放行快照和正式追溯接口，不创建或审批上游业务事实。

## Milestones

1. 固定当前前后端偏差和 BDD/RED 证据。
2. 更新 Stage6 静态合同，精确锁定新接口和只读边界。
3. 运行前端静态合同、Java 合同和 MES 编译。
4. 提交并保护性提升 int_main，核对主工作树保留其它 dirty overlay。

## Expected Verification

- Stage6 前端静态合同通过。
- Stage6 Java 合同通过。
- MES 编译通过。
- /stage6-id、签名密码输入和上游造数调用不再属于 Stage6 契约。

## Current Status

ready_for_closeout

## Verification Evidence

- Stage6 isolated frontend static contract: PASS。
- Stage6 Java contracts: 9/9 PASS。
- MES module compile: BUILD SUCCESS。
- Main backend and frontend sources already use the same stage6-idpr and simulationRunId-only contract。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，统一前后端正式只读追溯接口。
- 是否存在临时补丁或绕过：否。
