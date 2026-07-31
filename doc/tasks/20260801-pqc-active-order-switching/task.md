# PQC 活跃订单切换来源实现

## Task Goal

实现 PQC 检验员切换订单、工序、员工的正式数据来源：

- 订单来源必须是当前活跃订单。
- 工序来源必须是所选活跃订单对应产品的工艺路线工序。
- 员工来源必须是所有 PQC 员工 + PQC 组长。
- PQC 组长列表查看、判定、修正和日志能力不得与生产组长任务冲突。

## Milestones

- [x] 梳理现有 PQC 填写页、组长工作台、活跃订单和工艺路线数据链路。
- [x] 按 BDD 写出订单、工序、员工来源的 RED 测试。
- [x] 实现后端/前端最小正式数据链路，不引入默认全量列表或静默降级。
- [x] 运行定向验证并记录 GREEN/REGRESSION 证据。
- [x] 完成收尾状态与验证报告。

## Expected Verification

- 前端静态契约覆盖 PQC 订单、工序、员工选择来源。
- 后端定向测试覆盖活跃订单、产品路线工序和 PQC 人员来源。
- `pnpm ts:check` 或记录无关历史阻塞。
- `mvn -pl yudao-module-mes -am` 定向测试或记录缺失前置阻塞。

## Current Status

ready_for_closeout

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本任务使用专用静态合同覆盖 PQC 选择来源，避免被无关宽合同影响。
- 命中 `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：Maven `-Dtest`、`-Dsurefire.failIfNoSpecifiedTests=false` 均使用 PowerShell 安全引号。
- 命中 `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：已生成 backend/frontend evidence 和 verification-report。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式来源接口/调用链。
- `是否存在临时补丁或绕过`：否。

## Closeout Notes

- 实现和验证已完成，但当前工作区存在并行无关改动与分支 ahead 状态；为避免混入其它任务改动，本任务未执行提交/推送。
- 2026-08-01 复核时，最近提交 `7186c11a2 chore: baseline dirty workspace before dcc auto classify` 已将本任务部分实现和证据连同其它任务改动纳入同一个基线提交；未改写历史，当前任务仍保持 `ready_for_closeout`。
- 本任务代码改动与生产组长任务不冲突：生产模式仍走设备账号授权工序/员工绑定链路，PQC 模式新增独立的活跃订单、路线工序和 PQC 人员链路。
