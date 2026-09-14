# 20260727 eDHR 特殊工序填写人来源

## Task Goal

在批次执行详情中，将 `来料检报告`、`灭菌报告`、`成品检报告`、`成品检记录` 四个特殊工序解析为工艺路线 `工序开始 > 批记录附件` 中配置的填写人，并让特殊工序操作权限使用同一来源。

## Milestones

1. 记录 BDD 与 RED 测试，复现特殊工序 `fillableUsers` 为空或仍由生产负责人处理的问题。
2. 实现后端特殊工序负责人解析，来源限定为路线版本快照中的 `batchRecordAttachmentOwners`。
3. 验证详情展示、跳过/完成/附件权限和既有普通路线表单填写人回归。
4. 修复真实 E2E 发现的前端展示缺口：特殊节点右侧操作区展示当前 task 填写人。

## Expected Verification

- `MesProEdhrBatchExecutionServiceTest` 新增用例 RED -> GREEN。
- 定向 Maven 测试覆盖新增用例和相邻 `fillableUsers` 回归。
- 前端仅补充特殊节点右侧“填写人”展示，继续使用后端 `tasks[].fillableUsers`，不从当前登录人或创建更新人推断。
- 使用 `芋道源码/admin` 在 `int_main` 本机真实页面跑 Playwright E2E。

## Current Status

ready_for_closeout

## 经验门禁

- eDHR 详情回填门禁：路线配置有值但详情接口为空时，必须从配置来源、执行任务快照字段和详情接口组装链路补齐后端数据链路，不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- eDHR 批次任务配置来源门禁：批次执行涉及 `routeSnapshotJson` 和 `batchUseConfigs` 时必须明确当前配置与发布快照边界，不得把发布快照作为通用 fallback 或用空绑定掩盖配置损坏。
- eDHR 单据填写人显示值门禁：特殊节点必须同时验证详情接口 `fillableUsers` 和右侧操作区可见填写人，接口正确但页面未渲染不得判定通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，后端统一从路线开始节点批记录附件负责人配置解析。
- 是否存在临时补丁或绕过：否。

## Closeout Blocker

- 实现和真实 E2E 验证已完成；主工作区仍有大量并行 dirty 改动，未按本任务执行提交/推送，不能标记 `completed`。
- 本任务创建的隔离 worktree：`D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727`，用于 8082/48082 真实 E2E；当前含任务自有临时脏改动，删除前需明确授权丢弃该 worktree 临时改动并释放端口登记。

## Cleanup Keep

- doc/tasks/20260727-edhr-special-node-filler-from-route-start/backend-api-evidence.md
- doc/tasks/20260727-edhr-special-node-filler-from-route-start/bug-regression-evidence.md
- doc/tasks/20260727-edhr-special-node-filler-from-route-start/frontend-feature-evidence.md
- doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-special-node-filler-yudao-real.cjs
- doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.json
- doc/tasks/20260727-edhr-special-node-filler-from-route-start/e2e-artifacts/special-node-filler-yudao-real.png
