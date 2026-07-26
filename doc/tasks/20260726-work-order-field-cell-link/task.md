# 20260726 生产工单字段参与批记录单元格链接

## Task Goal

在“批记录单元格链接”页面支持从生产工单选择源字段，并在批次执行生成/打开时将生产工单字段值带入目标批记录单元格。

## Milestones

- [x] 识别现有单元格链接前端入口、保存契约和运行态回填链路。
- [x] 补充 BDD 场景和 RED 证据，锁定“生产工单字段作为左侧来源”的可观察行为。
- [x] 实现前端源字段选择、后端保存契约和批次执行回填逻辑。
- [x] 运行目标前后端验证，并记录 GREEN/REGRESSION 证据。
- [ ] 收尾清理、经验沉淀、提交并推送当前分支。

## Expected Verification

- 前端静态/契约测试覆盖左侧生产工单字段来源可选、选中后保存 payload 包含来源类型与字段编码。
- 后端单元/服务测试覆盖保存生产工单字段链接、运行态批次执行从工单取值并写入目标单元格。
- 必要时补充真实页面或最小 E2E 路径验证；若运行态前置服务缺失，按 fail-fast 记录 blocker。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是扩展正式来源类型与运行态取值链路，不做页面硬编码或默认成功。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`。
- 主工作区存在并行任务持续改动，已在 `D:\IntRuoyiWorktree\work-order-field-cell-link-20260726` 创建隔离 worktree，分支 `codex/work-order-field-cell-link-20260726`，slot 4，端口 8085/48085，未启动服务。
- 2026-07-26：隔离分支快进同步 `int_main` 到 `4533ac44`，避免把主分支并行路由配置大改混入本任务 diff；同步后仅保留 `MesProRouteFlowConfigServiceImpl` 的重复解析方法命名修正，用于解除目标 Maven 编译阻塞。
