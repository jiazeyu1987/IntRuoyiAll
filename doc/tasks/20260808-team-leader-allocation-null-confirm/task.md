# 生产组长确认分配 null 参数修复

## Task Goal
修复生产组长“分配报工/确认分配”时请求参数为 null 导致后端返回 `请求参数不正确:不能为空null` 的问题，确保前端提交正式有效的分配对象，缺少必填选择时在页面侧明确阻止提交。

## Milestones
- [x] M1: 定位确认分配请求载荷中为 null 的字段和前后端契约来源。
- [x] M2: 补充 BDD 与 RED 回归测试，证明未选择正式活跃订单时不能提交 null。
- [x] M3: 实现最小正式修复，不引入 fallback、默认成功或吞异常。
- [x] M4: 运行目标验证与相邻回归，记录 GREEN/REGRESSION。
- [x] M5: 更新任务文档与收尾状态。

## Expected Verification
- 目标前端静态/单元回归覆盖确认分配载荷不包含 null 活跃订单 ID。
- 相关后端控制器/服务既有回归保持通过或记录明确 blocker。
- `git diff --check` 通过。

## Current Status
completed

实现、验证、经验沉淀和 task-closeout-cleanup apply 均已完成。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正确认分配提交契约，不用默认值掩盖缺失。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary
- 命中 `docs/frontend-development.md#前端选择弹框即时反馈门禁`：手工选择弹框必须区分未选择、加载、空和错误，不能用默认候选或吞异常掩盖正式选择缺失。
- 命中 `docs/backend-development.md#FIFO 自动分配当前工序快照边界`：FIFO/确认分配必须保持当前 `routeProcessId + processId` 正式快照边界，指定确认仍 fail-fast，禁止默认目标数量或空成功。
- 本任务采用前端提交契约修复：确认分配只提交当前页签的正式 `leaderType`，手工分配行不预填潜在无效活跃订单，缺少正式活跃订单 ID 时阻止提交。

## Cleanup Keep
- doc/tasks/20260808-team-leader-allocation-null-confirm/bug-regression-evidence.md
