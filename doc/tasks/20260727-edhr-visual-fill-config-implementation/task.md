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

in_progress：融合分支后已修复任务专用路线复制时未继承来源 ACTIVE 版本 `batchRecordAttachmentOwners` 的快照缺口；后端目标 RED/GREEN、核心回归、前端静态合同、`pnpm ts:check`、后端打包、slot 2 后端重启和完整真实 E2E 均已通过。真实 E2E 在 `芋道源码` 租户使用本地未跟踪 `local-input.json`，不依赖环境变量，验证管理员配置、`jiazeyu` 与 `wangxin` 两名员工共享辅助行待办，并在 finally 恢复报表配置、作废任务批次、删除任务路线副本。剩余工作为提交推送与最终收尾。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少辅助行、字段类型、责任范围或快照时按阶段明确失败。
- `是否从根因和长期维护角度解决`：是；在现有批记录规则、权限、执行快照和辅助模式链路上扩展。
- `是否存在临时补丁或绕过`：否。
