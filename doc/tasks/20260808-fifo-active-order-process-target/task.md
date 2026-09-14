# 20260808 FIFO 活跃订单当前工序目标数量修复

## Task Goal

修复班组长报工点击 `FIFO 自动分配` 时，遇到活跃订单 `35` 提示“活跃订单缺少当前工序生产系数和目标数量快照”的问题。FIFO 自动分配应只消费包含当前工序快照的活跃订单；生产系数未设置时按业务默认值 `1` 计算目标数量。

## Milestones

- [x] 定位 FIFO 自动分配、活跃订单逐工序快照和目标数量解析链路。
- [x] 补充 BDD 与回归测试，覆盖跨 routeProcess 活跃订单和缺省生产系数。
- [x] 实现最小后端修复，不改变手工分配的 fail-fast 校验。
- [x] 运行目标回归与静态检查，记录 GREEN 证据。
- [x] 完成验证报告与任务收尾状态。

## Expected Verification

- `MesTeamLeaderFifoAllocationServiceTest` 覆盖 FIFO 预览跳过不含当前 `routeProcessId + processId` 快照的活跃订单。
- `MesTeamLeaderOrderProcessTargetServiceTest` 覆盖生产系数缺省按 `1`、目标数量按 ERP 数量派生。
- 目标 Maven 测试通过。
- `git diff --check` 通过。

## Applicable Gates

- `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`：保持正式路线/版本/工序快照来源，缺正式来源仍 fail fast。
- `docs/backend-development.md#fifo-自动分配当前工序快照边界`：FIFO 预览跳过非当前 routeProcess 快照，最终确认仍 fail-fast。
- 用户明确业务规则：生产系数未设置时为 `1`，不是静默降级。
- No-fallback 门禁：不吞异常、不返回默认成功；仅 FIFO 自动预览跳过不属于当前工序的活跃订单，手工指定错误活跃订单仍由 `requireTarget` 阻塞。

## Current Status

completed - FIFO 修复、目标回归、静态检查、隔离验证 worktree 清理和任务文档收尾均已完成；未执行 Git 提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；生产系数缺省为用户确认的业务默认值，非法非正系数仍失败。
- `是否从根因和长期维护角度解决`：是；在目标数量解析服务和 FIFO 自动候选过滤处修复，不改前端文案。
- `是否存在临时补丁或绕过`：否。
