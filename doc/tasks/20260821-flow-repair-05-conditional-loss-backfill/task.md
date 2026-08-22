# 流程修复 5：工序级条件损耗回填

## Task Goal

本专项已完成工序级条件损耗实现并融合 `int_main`。范围仅包括正式一线生产事实校验、流程 1 五字段领料绑定快照只读校验、原因/签名/来源快照、正损耗条件损耗单写入和 `NO_LOSS` 事实输出；未扩展到流程 6 建批、流程 8 材料放行或流程 10 最终放行。

一线生产、一线 PQC 提交以及生产组长、PQC 组长复核只形成正式来源事实，不触发回填。流程修复 4 是活跃订单完成节点及三类回填的唯一编排所有者；流程修复 5 只是该节点中的条件损耗分支。

## Target State

1. 流程修复 4 在活跃订单双进度均为 100% 且生产组长点击完成时，统一启动批记录、过程检验和条件损耗三类回填。
2. 流程修复 5 对每个冻结工序校验正式一线生产事实和流程 1 的五字段领料绑定快照（`pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId`），并独立计算实际损耗；只读取校验，不创建或猜测领料关系。只有规范化数量严格大于 0 时才创建该工序损耗单。
3. 数量明确等于 0 且存在正式无损耗确认事实时，不创建损耗单、不生成任何零数量或无损耗报告；该工序必须输出 `hasActualLoss=false`、`lossQuantity=0`、`lossDecision=NO_LOSS`，流程 4 工序及订单完成 receipt 记录 `lossReportStatus=NOT_REQUIRED`、正式来源快照且不生成 `lossRecordId`。
4. 每个有损耗工序必须输出 `hasActualLoss=true` 且 `lossQuantity>0` 并创建损耗单；订单级完成 receipt 同时保存订单级 `hasActualLoss`、逐工序 decisions 和 `lossReportStatus=SUCCESS`。缺少 `lossRecordId` 不能推断 `hasActualLoss=false`。
5. 数量、五字段绑定快照或无损耗确认缺失、非法、矛盾时必须阻塞，禁止把缺失事实当作 0 或 false；`BLOCKED` 不产生成功 receipt，也不得驱动流程 6 建批。
6. 有损耗来源必须包含生产反馈、工序快照、损耗原因和明细、填写签名、生产组长复核签名、五字段领料绑定快照、来源 ID/hash 与完成版本。
7. 流程修复 4 提交三类回填成功 receipt 后，由流程修复 6 创建或复用批次执行；流程修复 5 不创建批次。
8. 流程修复 7 只消费并映射本专项输出的损耗来源、`NO_LOSS` 来源事实和绑定快照，不由本专项猜测批次映射。
9. 流程修复 8 拥有四份材料上传及放行硬门禁；流程修复 10 拥有最终放行状态、角色和审计；流程修复 11 拥有 BDD/TDD、回归和迁移总门禁。

流程 5 的内部接口只接受流程 4 冻结的完成上下文和流程 1 五字段绑定快照，返回逐工序 `REQUIRED/NO_LOSS/BLOCKED`、`hasActualLoss`、`lossQuantity`、来源快照及条件损耗结果；绑定缺失/变化使用 `LOSS_SOURCE_PICK_LIST_BINDING_REQUIRED` / `LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED`，事实与布尔值不一致使用 `LOSS_HAS_ACTUAL_LOSS_REQUIRED` / `LOSS_HAS_ACTUAL_LOSS_CONFLICT`。

跨线程唯一状态字段为 `lossReportStatus`：工序决策只允许 `REQUIRED`、`NO_LOSS`、`BLOCKED`；工序及订单完成 receipt 只允许 `SUCCESS` 或 `NOT_REQUIRED`。`REQUIRED` 必须为 `hasActualLoss=true` 且 `lossQuantity>0` 并成功写入损耗单；`NO_LOSS` 必须有正式零损耗事实、`hasActualLoss=false`、`lossQuantity=0`，receipt 使用 `lossReportStatus=NOT_REQUIRED` 且不含 `lossRecordId`；`BLOCKED` 不产生成功 receipt，也不得驱动流程 6 建批。

## BDD Acceptance

BDD: 正数损耗 -> Given 双100、五字段绑定快照和正式正损耗事实完整，When 流程 4 完成节点调用流程 5，Then 仅正数工序建损耗单，`hasActualLoss=true`、`lossQuantity>0`，receipt 的 `lossReportStatus=SUCCESS`。
BDD: 无损耗不建单 -> Given 正式零损耗事实和五字段绑定快照完整且数量为 0，When 调用流程 5，Then 输出 `NO_LOSS`、`hasActualLoss=false`、`lossQuantity=0`，receipt 的 `lossReportStatus=NOT_REQUIRED`，不生成 `lossRecordId`、损耗单或报告。
BDD: 缺失或阻塞 -> Given 零损耗事实、绑定或签名缺失/矛盾，When 流程 4 统一回填，Then 不把缺失推断为 false，不提交成功 receipt，流程 6 不得建批。

## Current Code Facts

- `MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl` 读取并校验正式签名反馈、五字段绑定快照、数量与 `hasActualLoss` 一致性；缺失正式零损耗事实返回阻塞，不把缺少 `lossRecordId` 推断为无损耗。
- `MesTeamLeaderActiveOrderReleaseLossReportWriterImpl` 对正数损耗输出 `REQUIRED`、`hasActualLoss=true`、`lossQuantity>0` 并写入正式损耗单；对明确零损耗输出 `NO_LOSS`、`hasActualLoss=false`、`lossQuantity=0`、`lossReportStatus=NOT_REQUIRED`，不创建损耗单或零数量报告；阻塞分支不写成功 receipt。
- `MesPqcReleaseDossierPortImpl`、PQC decision result 和前线 payload 已消费显式损耗状态及来源快照，条件门禁不再把损耗 evidence 当作所有工序的无条件成功条件。
- 流程5 task-owned 代码/测试由 `24fdf7767ac02c4b6d4a3c4709194e195fea624a` 完成，并以 `16e47106e043ad93b4d43d699d269996703a47e1` 融合到当前 `int_main`；当前主线已确认该提交为祖先。
- 流程4订单级 receipt 持久化、流程6建批消费、流程7映射、流程8材料放行、流程10最终状态和流程11迁移总门禁不由本专项拥有，仍需各线程验证。

## Root Cause

1. “损耗报告”被建模为三类资料中的固定必填项，而不是由每个工序损耗事实决定的条件资料。
2. 零损耗没有独立的正式事实字段，系统只能用缺失或阻塞表达，导致无法安全区分“确认无损耗”和“损耗来源缺失”。
3. 计划、写入和最终 dossier 完整性检查均以 `lossReportEvidenceIds` 非空为成功条件，缺少 `NOT_REQUIRED` 分支。
4. 现有回填发生在申请放行阶段且建批早于回填，损耗条件不能在唯一完成节点原子确定。

## Modification Boundary

- 已完成：流程5 task-owned Java、定向测试及本目录文档；代码提交和主线融合均已完成。
- 未做：SQL/数据库迁移、服务进程、写入型 E2E、流程4/6/7/8/10/11 的跨流程实现。
- 流程修复 5 对流程 1 五字段领料绑定快照只读校验和固化，不创建 `pickListBindingId`、不补建 `pickListId`、不生成 `batchPickListRelationId`、不猜测 bindingVersion 或 sourceSnapshotHash。
- 明确排除：活跃订单完成编排、批次执行创建/复用、批次映射、四份材料、最终放行状态/角色/审计和迁移总编排；这些分别属于流程修复 4、6、7、8、10、11。

## Milestones

| Milestone | 内容 | 状态 |
| --- | --- | --- |
| M1 | 规则、经验、现有代码/测试只读审计 | completed |
| M2 | 损耗事实、零损耗和部分工序数据/状态设计 | completed |
| M3 | 接口、幂等、事务、追溯及跨线程契约 | completed |
| M4 | BDD、RED/GREEN/REGRESSION 计划与 blocker | completed |
| M5 | 按复核意见校正五份文档并重新验证 | completed |
| M6 | 同步流程 1/6 五字段绑定快照与 hasActualLoss 跨线程合同 | completed |

## Expected Verification

- 五份文档存在且章节覆盖目标态、当前事实、根因、边界、接口/数据/状态、BDD、RED/GREEN/REGRESSION、迁移/回滚、blocker 和流程修复 4/6/7/8/10/11 契约。
- 代码符合性结论：流程5条件分支已实现并通过主线核心验证；流程4/6/7/8/10/11 的跨流程 receipt、建批、映射、材料和最终放行仍不属于本专项结论。
- 主线验证证据：MES compile PASS；流程5核心21项 JUnit PASS；`git diff --check` PASS；runtime v5 guard PASS。

## Verification Status

BDD 已按正损耗、无损耗、阻塞、部分工序和幂等场景执行；GREEN/REGRESSION 证据已回填。写入型 E2E、数据库迁移和全链路流程仍 NOT_RUN。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺正式损耗事实、原因、签名、映射或来源快照时 fail fast。
- 是否从根因和长期维护角度解决：是；以工序级损耗决策和完成回执状态替代固定 evidence 必填。
- 是否存在临时补丁或绕过：否；禁止用空损耗单、零数量报告、默认原因、`formBindings`、默认 `MAIN`、旧批次或前端值掩盖来源缺失。

## Blockers

1. 流程4订单级完成 receipt 的持久化和流程6消费合同仍需跨线程验证；流程5仅保证输出字段和条件写入语义。
2. `MesFrontlineRuntimeConfigProcessScopeTest` 的静态断言在当前主线失败，涉及前线运行时参数校验，不属于流程5条件损耗 owner。
3. 流程7映射、流程8四材料门禁、流程10最终放行、流程11迁移/全链路回归仍未由本专项证明。
4. 历史完成记录缺少正式损耗事实或五字段绑定快照时，仍须由流程11迁移门禁阻塞。

## Migration and Rollback Boundary

- 迁移只允许把正式生产反馈、签名、复核、损耗明细、五字段绑定快照和完成事实全部可证明的记录绑定为 `SUCCESS` 或 `NOT_REQUIRED`；同时显式写入订单级/逐工序 `hasActualLoss`。证据不全保持 `BLOCKED_LEGACY`。
- 不为历史无损耗记录批量生成零损耗单；只能在有批准的正式零损耗事实字段/快照、五字段绑定快照且明确 `hasActualLoss=false`、`lossQuantity=0` 时标记 `NOT_REQUIRED`。
- 新实现失败时保留原始生产事实和审计事件，回滚只撤销新编排入口或绑定，不删除历史反馈、损耗单或批次。
- 已生成的损耗单不得通过删除改成无损耗；业务纠正必须另立受控任务并保留审计链。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260821-flow-repair-05-conditional-loss-backfill/task.md
- doc/tasks/20260821-flow-repair-05-conditional-loss-backfill/execution-log.md
- doc/tasks/20260821-flow-repair-05-conditional-loss-backfill/verification-report.md
- doc/tasks/20260821-flow-repair-05-conditional-loss-backfill/development-plan.md
- doc/tasks/20260821-flow-repair-05-conditional-loss-backfill/test-plan.md

已按复核意见修订职责、顺序、状态、错误码和 blocker；流程5代码与测试已融合并完成主线核心验证。数据库迁移、服务和写入型 E2E 未执行，跨流程 owner 证据仍按 Blockers 保留。
