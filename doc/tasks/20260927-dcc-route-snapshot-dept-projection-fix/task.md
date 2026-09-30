# DCC 部门会签快照详情投影修复

## Goal

修复 DCC 详情页对部门会签义务的投影：当 DEPT 路由快照未持久化多候选部门，但同一阶段的任务负责人快照包含多个部门时，展示全部部门及其任务创建时负责人快照。

## BDD

- Given DEPT route snapshot 的 `candidateSourceIds` 为空、`candidateSourceId=700`，且同阶段任务负责人快照按顺序包含部门 700、701；When 调用详情快照投影；Then 候选部门、部门名称和 department obligations 均按 `[700, 701]` 展示。
- Given route snapshot 已持久化显式候选部门；When 调用详情快照投影；Then 显式 `candidateSourceIds` 优先，不从任务快照替换或扩展候选集合。
- Given 单部门无任务快照，或非 DEPT 路由存在部门任务快照；When 调用详情快照投影；Then 保留原单候选语义，非 DEPT 不推断部门。

## Design Constraints

- 仅修改 DCC 查询投影和单元测试。
- 不修改数据库、迁移、服务运行态、共享服务或 Git 历史。
- 任务快照部门 ID 使用去重且保持输入顺序的集合；部门名称与义务过滤使用同一候选 ID 集合。

## Verification

- RED: 新增的空 `candidateSourceIds` 双部门快照测试在修复前得到 `[700]`，未覆盖部门 701。
- GREEN: 修复后目标测试和完整 `DccControlledFileQueryServiceTest` 通过。
- 静态检查: `git diff --check`。

## Status

completed
