# Request Analysis

## User Goal

在两个独立 worktree 中分别实现并验证 F5 审核副本上下限修正模块、F6 原始记录修改日志与重新电子签名模块；主 agent review 后合并进 `int_main`。

## Current System

- 主仓库位于 `E:\IntRuoyi`，当前分支为 `int_main`。
- 现有验收文档位于 `docs\acceptance\production-line-process-pool\`，已定义 F5/F6 的 BDD、TDD、E2E、测试数据和 review 门禁。
- MES 模块已有 F1/F7/F8 基础：
  - `mes_pro_process_pool`、`mes_pro_process_pool_event`、`mes_pro_process_pool_quantity_fragment`、`mes_pro_process_pool_pqc_record`、`mes_pro_process_pool_fifo_allocation_line` 正式迁移和测试建表 SQL。
  - `MesProcessPoolEventServiceImpl` 负责创建工序池提交事件和 PQC 事件。
  - `MesProcessPoolFifoAllocationService#validateOriginalFieldMutationAllowed` 已提供分配后字段锁定边界。
  - `ProcessPoolTimelineServiceImpl`、`MesProProcessPoolTimelineReadMapper.xml`、`TimelinePage.vue` 已提供时间轴只读入口，但审核副本和修改历史仍为空摘要。

## Constraints

- 严格 BDD + TDD，生产代码变更必须先有 RED 再实现 GREEN。
- 不允许 fallback、静默降级、默认成功、模拟成功或吞异常。
- worktree 只能创建在 `D:\IntRuoyiWorktree\` 下，且附加 worktree 需要登记槽位。
- F5 只能写审核副本和字段明细，不得改写原始 payload。
- F6 只能写原始记录 revision 和字段级修改日志，不得把上下限修正写回原始 payload。
- 已 FIFO 分配的数量片段或无法确认锁定状态的字段必须阻塞。

## Unknowns

- 当前 F1/F2/F3/F4/F7/F8 的正式实现完成度是否足以支撑 F5/F6 直接实现。
- 电子签名能力在 MES 工序池中应复用 DCC/eDHR 哪一条正式接口或仅保存已生成签名 ID。
- F5/F6 前端是否已有正式菜单入口和测试账号数据可运行真实 E2E。

## Risks

- 如果正式工序池基础模型不完整，F5/F6 需要补齐前置模型，可能扩大写 scope。
- 两个 worktree 同时修改同一工序池 schema、公共 VO、mapper 或前端页面时存在合并冲突。
- Playwright 写路径 E2E 需要真实运行态、登录账号、租户和可清理测试数据，缺任一项都必须阻塞而不能降级。

## Validation Surface

- Java/JUnit：MES 工序池 schema、审核副本服务、原始记录 revision 服务、FIFO 锁定规则。
- Node 静态合同：mapper、前端 API、页面只读/写入口边界。
- Playwright：审核副本生成/提交、原始记录修改、已分配字段拒绝。
- Git/worktree：两个 worktree 独立实现并合并进 `int_main`。

## Blocking Prerequisites

- 两个目标 worktree 路径必须可创建且位于 `D:\IntRuoyiWorktree\` 下。
- `scripts\runtime\reserve-worktree-slot.ps1` 必须可用并成功登记两个 worktree。
- 正式 F5/F6 所需基础模型、签名、FIFO 锁定查询或测试运行态若缺失，必须在任务日志中记录为具体 blocker 或作为正式实现 scope 补齐。
