# Execution Log

## Intent

用户要求在批次执行中，将 `灭菌报告`、`成品检报告`、`成品检记录`、`来料检报告` 四个特殊工序解析为工艺路线 `工序开始` 中设置的填写人。

## BDD

BDD: 特殊工序显示路线开始节点配置的填写人 -> Given 工艺路线版本快照包含 `batchRecordAttachmentOwners` 且 4 个附件配置分别绑定用户/角色 When 打开批次执行详情 Then 4 个特殊工序的 `fillableUsers` 分别等于对应配置解析出的当前租户启用用户。

BDD: 特殊工序操作权限使用对应填写人 -> Given 路线生产负责人和特殊工序附件负责人不是同一人 When 非对应附件负责人尝试跳过/完成/上传该特殊工序 Then 后端拒绝；When 对应附件负责人操作 Then 后端允许并保留既有门禁。

BDD: 普通路线表单填写人不受影响 -> Given 普通路线表单已有工作任务、表单权限规则或路线绑定填写人 When 打开批次执行详情 Then 普通表单仍按既有优先级解析 `fillableUsers`。

## Evidence

- Task directory created: `doc/tasks/20260727-edhr-special-node-filler-from-route-start`.
- Experience preflight: PASS, matched `docs/backend-development.md#edhr-详情回填门禁` and `docs/backend-development.md#edhr-批次任务配置来源门禁`.
