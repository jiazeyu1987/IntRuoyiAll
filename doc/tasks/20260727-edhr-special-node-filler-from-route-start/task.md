# 20260727 eDHR 特殊工序填写人来源

## Task Goal

在批次执行详情中，将 `来料检报告`、`灭菌报告`、`成品检报告`、`成品检记录` 四个特殊工序解析为工艺路线 `工序开始 > 批记录附件` 中配置的填写人，并让特殊工序操作权限使用同一来源。

## Milestones

1. 记录 BDD 与 RED 测试，复现特殊工序 `fillableUsers` 为空或仍由生产负责人处理的问题。
2. 实现后端特殊工序负责人解析，来源限定为路线版本快照中的 `batchRecordAttachmentOwners`。
3. 验证详情展示、跳过/完成/附件权限和既有普通路线表单填写人回归。

## Expected Verification

- `MesProEdhrBatchExecutionServiceTest` 新增用例 RED -> GREEN。
- 定向 Maven 测试覆盖新增用例和相邻 `fillableUsers` 回归。
- 不新增前端展示结构，继续使用后端 `tasks[].fillableUsers`。

## Current Status

in_progress

## 经验门禁

- eDHR 详情回填门禁：路线配置有值但详情接口为空时，必须从配置来源、执行任务快照字段和详情接口组装链路补齐后端数据链路，不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- eDHR 批次任务配置来源门禁：批次执行涉及 `routeSnapshotJson` 和 `batchUseConfigs` 时必须明确当前配置与发布快照边界，不得把发布快照作为通用 fallback 或用空绑定掩盖配置损坏。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，后端统一从路线开始节点批记录附件负责人配置解析。
- 是否存在临时补丁或绕过：否。
