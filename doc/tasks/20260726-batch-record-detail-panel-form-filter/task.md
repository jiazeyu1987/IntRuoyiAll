# 批记录详情面板表单过滤修复

## Task Goal

修复批记录表单配置页右侧“字段明细”面板：当选中“批记录表单”字段时，红框区域只显示批记录表单自身关联信息，不再显示其它路线表单或过程检验表单。

## Milestones

- [x] 固化用户截图中的回归行为和期望显示口径。
- [x] 新增最小静态回归测试，先证明当前实现会显示非批记录表单。
- [x] 修改前端右侧详情面板过滤逻辑，仅保留批记录表单相关条目。
- [x] 运行目标回归测试和必要静态验证。
- [x] 更新任务证据并进入收尾。

## Expected Verification

- `node tests/e2e/<target-static-spec>` 先 RED 后 GREEN。
- 受影响组件源码检查确认非批记录表单不会出现在“批记录表单”字段明细中。

## Current Status

ready_for_closeout

## Closeout Blockers

- 当前工作区存在并发任务改动，且 `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue` 同一文件包含非本任务引入的记录本开关/保存载荷变化。
- 既有合同 `node tests/e2e/mes-route-flow-legacy-batch-record-detail-static.spec.js` 失败在并发改动引入的 `batchRecordReports: processConfig.batchRecordReports` 断言上；本任务未修改该保存载荷逻辑。
- 因工作区仍有非本任务脏改动与本地分支 ahead 状态，本任务未执行最终提交、推送和 completed 标记。

## Cleanup Keep

- doc/tasks/20260726-batch-record-detail-panel-form-filter/bug-regression-evidence.md
- doc/tasks/20260726-batch-record-detail-panel-form-filter/frontend-feature-evidence.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，预期在右侧详情面板数据过滤口径上修复。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### eDHR 右侧红框元信息隐藏门禁

- Trigger: 修改 eDHR 批次详情右侧栏、单据卡片、截图红框区域或相关显示过滤逻辑。
- Preflight check: 区分右侧独立元信息区域与单据卡片内必要信息，修复时不得删除卡片自身的填写人或表单信息。
- Blocker: 若源码仍保留应隐藏的红框显示来源，或把单据卡片内必要信息一起删除，不得声明完成。
- Verification: 运行聚焦静态合同，证明红框区域不显示非目标表单，同时保留目标表单信息。
- Forbidden action: 禁止把红框信息移动到其它一级区域伪装删除；禁止顺手修改无关审批、提交或打开表单逻辑。
- Evidence: `docs/e2e-rules.md#edhr-右侧红框元信息隐藏门禁`。
