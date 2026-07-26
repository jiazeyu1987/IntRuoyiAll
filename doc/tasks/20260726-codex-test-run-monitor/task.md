# Codex 测试运行监控页签

## Task Goal

- 在系统测试管理中增加“运行监控”页签/区域，实时展示当前运行中的测试任务数量和每个任务的运行状态。
- 方法项按执行进度显示：已完成为绿色，当前执行为黄色，未开始为默认色。
- 目标项按验证进度显示：验证中为黄色，验证成功为绿色，验证失败为红色；点击失败目标可查看失败原因。

## Milestones

- [x] 确认现有测试管理、执行记录、Runner 回写和检查点数据模型。
- [x] 补充 BDD / RED 静态合同、后端单测和迁移合同。
- [x] 实现运行监控页签、监控 API、Runner 进度回写和 schema 字段。
- [x] 运行聚焦验证并记录结果。

## Expected Verification

- 聚焦静态合同覆盖运行监控页签、颜色状态、失败原因弹窗和轮询刷新。
- 必要后端测试覆盖监控数据/进度回写契约。
- 迁移合同覆盖运行监控进度字段和测试库 schema。
- 前端类型检查通过。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增 Runner progress 合同、后端持久化字段、监控查询接口和前端轮询 UI。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端静态契约隔离门禁：已新增 `system-codex-test-run-monitor-static.spec.js`，并保留原测试管理静态合同回归。
- Codex Runner 自动测试门禁：运行监控基于 Runner 注册、领取、执行期心跳、progress 回写和检查点结果回写，不使用 mock 或默认成功。
- 数据库门禁：新增字段前核对了 `system_codex_test_execution_case` 现有迁移和 H2 测试 schema，并补充幂等迁移。

## Known Runtime Granularity

- 当前 Runner 仍以单次 Codex CLI 执行完整自然语言方法文本；因此 Runner 能真实上报“进入方法阶段”和“逐个目标验证阶段”。
- 后端和前端已支持 `currentMethodSort=N` 的方法项级监控；若后续 Runner 拆分为逐方法项执行，只需按当前 progress 合同上报 N。