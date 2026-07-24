# 任务：ERP 生产用料清单存在但本地未同步排查（后端）

- Task ID: `20260630-erp-material-list-missing-local-sync-analysis`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

定位 `PPBOM0030818 / 881MO090863` 为什么在 ERP 可见，但本地 `mes_kingdee_production_material_list` 无记录。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-material-link-sample-gap-fix\task.md`
- 状态：`completed`
- 处理说明：上一任务已确认本地真实双向关联样本与运行态恢复，本次继续分析某张 ERP 单据未同步的后端根因。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`docs\integrations\kingdee-erp-official-docs.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/SQL/Markdown 输出统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - 必须以真实同步字段、同步窗口和正式接口口径分析，不得只凭页面展示推断。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。只读定位同步断点，不做手工补数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: ERP 源头存在而本地缺失时暴露同步断点 -> Given ERP 中存在生产用料清单单据 PPBOM0030818 且生产订单号为 881MO090863 / When 排查本地同步链路 / Then 能明确断点位于未拉取、被过滤、租户归属不符或运行态未执行，而不是笼统认为 ERP 无数据。`

## Milestones

1. M1：建立后端任务文档并复核本地库现状。`completed`
2. M2：阅读同步服务/Job/Mapper 与同步记录表。`completed`
3. M3：锁定缺失断点。`completed`
4. M4：形成结论与修复建议。`completed`

## Expected Verification

- 数据库只读查询
- 同步服务源码定位

## Current Blockers

- 无。已完成本机后端更新、真实回补与目标记录复核。
