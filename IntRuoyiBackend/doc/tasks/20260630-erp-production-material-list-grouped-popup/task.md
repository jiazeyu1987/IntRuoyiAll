# 任务：ERP 生产用料清单单据汇总与明细弹窗（后端）

- Task ID: `20260630-erp-production-material-list-grouped-popup`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

为 ERP 生产用料清单提供正式的“按单据汇总分页 + 按单据明细查询”后端接口，支持前端主表一单一行与弹窗查看整单子项，不改现有 `/page` 接口和库结构。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-scheduler-material-analysis-trace\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成；本次为新的 MES/ERP 查询交付任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/SQL/Markdown 与执行日志统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 若进入真实只读验证，先走官方 `login-preflight.mjs`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过正式分组查询与明细查询接口支撑单据级视图，不在前端用明细数据假分组。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 后端可返回生产用料清单单据汇总页 -> Given 当前租户存在多条生产用料清单明细 / When 调用 group-page / Then 返回按 sourceBillNo 聚合的单据汇总行。`
- `BDD: 后端可返回整单子项明细 -> Given 某 sourceBillNo 存在多条子项分录 / When 调用 detail-list / Then 返回该单据完整子项明细及关联工单字段。`

## Milestones

1. M1：建立后端任务文档并锁定接口边界。`completed`
2. M2：补 RED 测试与 Mapper/Service/VO 合同。`completed`
3. M3：实现分组接口与明细接口。`completed`
4. M4：完成定向验证并回填证据。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSchemaTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesKingdeeProductionMaterialListMapperXmlTest" -Dsurefire.failIfNoSpecifiedTests=false test`

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSchemaTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesKingdeeProductionMaterialListMapperXmlTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

## Current Blockers

- 无功能阻塞。
