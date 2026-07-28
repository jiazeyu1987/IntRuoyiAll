# eDHR 可视化填写配置实现验证

## Task Goal

按 `doc/tasks/20260726-edhr-visual-fill-config-bdd-tdd-design/` 中的设计与验收计划，在 worktree `202607727_yingshe` 完成可视化填写配置开发、验证、融合到 `int_main`，并在融合后执行完整 E2E 验证。

## Milestones

1. `completed`：T01 辅助行模型与单元格规则保存读回。
2. `completed`：T02 类型纠错、下拉框和签名约束。
3. `completed`：T03 按辅助行保存填写责任。
4. `completed`：T04 旧规则显式 `ALL` 范围迁移合同。
5. `completed`：T05 工作任务责任快照。
6. `completed`：T06 当前用户有效范围与后端写入授权。
7. `completed`：T07 执行快照与辅助模式数据。
8. `completed`：T08 前端统一编辑器、辅助行填写人分配和辅助模式静态合同。
9. `completed`：T09 真实用户路径 E2E，复用现有“复制工艺路线、候选版本、工序表单配置、提交发布、启停和删除”页面能力，为工单创建 `CODX-VFC-*` 任务专用路线副本，绑定目标“粗洗工序生产记录”后验证管理员和两名员工路径，并在 finally 恢复报表配置、清理任务批次和路线副本。
10. `in_progress`：回归验证、提交推送、融合 `int_main`、融合后完整 E2E。

## Expected Verification

- 每个 TDD 阶段先记录 BDD 和 RED，再实现最小生产行为并记录 GREEN。
- 后端聚焦 Maven 测试、前端静态合同、真实 E2E 均按设计文档执行。
- 不新增独立辅助设计器、辅助布局表、单元格级责任覆盖层或独立辅助草稿对象。
- 缺少测试租户、账号、运行态、Git 凭据或主 worktree 合并条件时 fail fast 并记录 blocker。

## Experience Gate Summary

- Maven `-D...` 参数在 PowerShell 中必须整体加双引号。
- eDHR 批记录运行态不得从当前模板 JSON 静默回退；执行和责任范围必须来自已冻结快照。
- 前端静态合同需聚焦当前行为，不能用无关历史失败替代当前 RED/GREEN。
- 真实 E2E 必须走真实页面、真实账号和任务自有数据，不能用 mock 或 API-only 替代主路径。

## Current Status

completed：融合分支已通过 cherry-pick 集成到 `int_main` 并推送；`int_main` 稳定 Jar 主端口 `8081/48081` 完整真实 E2E 已通过。主工作区 cleanup apply 已删除任务临时证据文件，linked worktree 因 cherry-pick 历史无法 ff-only 但 `git cherry` 证明 5 个分支提交均已等价进入 `int_main`；任务 worktree 运行态已停止，残留目录已删除，slot 2 已释放。最终保留 `task.md`、`execution-log.md`、`verification-report.md` 与 `bug-regression-evidence.md`。

## Cleanup Keep

- doc/tasks/20260727-edhr-visual-fill-config-implementation/bug-regression-evidence.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少辅助行、字段类型、责任范围或快照时按阶段明确失败。
- `是否从根因和长期维护角度解决`：是；在现有批记录规则、权限、执行快照和辅助模式链路上扩展。
- `是否存在临时补丁或绕过`：否。
