# 任务：PQC组长表单修改与放行前复核链路

## Task Goal

按用户确认的 7 条规则修复 PQC 组长页：

1. PQC 组长可以修改 PQC 组员提交的数据。
2. PQC 管理列表中“修改”和“复核”按钮常驻。
3. PQC 历史保留提交记录。
4. 复核通过后更新活跃订单可读取的 PQC 审核/汇集进度。
5. 放行前 PQC 表单数据都可以修改。
6. 放行后从 PQC 管理表单移除。
7. 放行后仍在 PQC 历史保留。

## Milestones

- [x] M1：建立 BDD/TDD 证据，先复现旧逻辑按钮和后端门禁问题。
- [x] M2：修复前端 PQC 管理/历史操作边界。
- [x] M3：修复后端 PQC 放行前修改、重复复核和放行后列表过滤链路。
- [x] M4：运行前端静态、后端定向和回归验证。
- [x] M5：先提交 worktree，再融合进 int_main。
- [x] M6：完成收尾清理和最终记录。

## Expected Verification

- 前端静态测试：PQC 管理列表“修改/复核”按钮常驻，PQC 历史不显示“修改/复核”。
- 后端定向测试：PQC 已审批但未放行仍允许原始记录修改；放行后管理列表移除但历史查询保留。
- 后端定向测试：PQC 重复复核允许生成最新复核记录，复核通过后保持正式汇集进度可被活跃订单放行链路读取。
- 编译与类型检查：MES Maven reactor 编译通过，Vue/TypeScript 类型检查通过。
- Git 验证：提交只包含本任务文件；融合到 int_main 前运行分支端口 guard；融合后保留并行未提交改动。

## Current Status

completed

- 实现、定向验证、worktree 提交、`int_main` 融合、任务中间产物清理、worktree 移除和槽位释放均已完成。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，前端按钮、后端复核/修改门禁和时间轴过滤都按正式业务状态收敛。
- 是否存在临时补丁或绕过：否。

## Experience Gate Summary

- 已读取 `docs/experience-index.md`，本任务命中 PQC 填写、PQC 组长复核、AC-M21 过程检验汇集、PQC 待检工单、前端写入成功与列表刷新失败等门禁。
- 适用门禁要求：PQC 复核通过必须形成正式结构化汇集事实；活跃订单放行只读取 CONFIRMED PQC 任务和汇集明细；前端写入成功后必须刷新正式列表，不用缓存或空结果冒充成功。
- 已通过 `project-experience-consolidation` 将并行脏主工作区的手工三方融合门禁合并到 `docs/worktree-memory.md`。
