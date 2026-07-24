# 任务：修复本地生产订单与生产用料清单缺少真实关联样本（后端）

- Task ID: `20260630-erp-production-material-link-sample-gap-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

定位并修复后端正式同步/关联逻辑中导致本地测试租户没有真实生产工单 <-> 生产用料清单关联样本的问题，确保正式字段 `workOrderId/workOrderCode/productionMaterialListCount` 能在本地真实数据上产出非空结果。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成查询展示字段扩展，本次继续修复真实样本缺失。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`docs\integrations\kingdee-erp-official-docs.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/Markdown/SQL 查询输出统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - 生产用料清单与生产工单的映射口径必须以现有同步字段、真实库和正式同步代码为准。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先修复正式同步映射逻辑或正式数据过滤逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: productionOrderNo 能映射本地工单时写入关联字段 -> Given 本地已存在 code 与生产用料清单 productionOrderNo 匹配的生产工单 / When 执行生产用料清单同步 / Then 记录写入 workOrderId/workOrderCode，前端查询可见。`
- `BDD: 已写入的关联字段能统计到生产工单页 -> Given 生产用料清单记录已写入 workOrderId / When 查询生产工单分页 / Then 返回 productionMaterialListCount 和 productionMaterialListSummary 非空。`
- `BDD: 无匹配时保留空关联并暴露原因 -> Given 生产用料清单 productionOrderNo 在本地不存在匹配工单 / When 执行同步 / Then 关联字段保持为空且回归测试能说明失败原因。`

## Milestones

1. M1：建立后端任务文档并完成根因复现。`completed`
2. M2：确认问题属于本地运行态损坏而非业务逻辑回归。`completed`
3. M3：重建并恢复最新 backend 运行态。`completed`
4. M4：完成真实接口与页面关联样本回归。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSyncServiceImplTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test`

## Completed Work

- 确认测试租户 `122` 的真实库数据已经具备正式关联结果：`mes_kingdee_production_material_list` 中 `workOrderId` 非空记录共有 `1147` 条，涉及 `75` 个工单，说明问题不在正式同步映射逻辑缺样本。
- 确认根因是本地 backend 可执行产物 `yudao-server-exec.jar` 损坏，导致 `restart-ruoyi-local-component.ps1 -Component backend -SkipBuild` 首次失败并报 `Invalid or corrupt jarfile`，进而使 48081 不能稳定提供最新关联结果。
- 重新打包 `yudao-server`、重启 backend 后，真实接口与真实页面均恢复到可见双向关联样本的状态。

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS。
- `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\script\deploy\restart-ruoyi-local-component.ps1 -Component backend -SkipBuild` -> PASS，`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- 真实接口回归 -> PASS：`/mes/pro/work-order/page`、`/erp/production-material-list/group-page`、`/erp/production-material-list/detail-list` 在测试租户 `122` 下均返回 `SMART-SCHED-20260630-RERUN9-MO <-> PPBOM00308992` 的真实关联样本。
- `bug-regression-evidence.md` 已记录本次运行态损坏的 RED / GREEN 证据；本次未新增或修改生产代码，因此未进入源码级回归测试。

## Current Blockers

- 无。此前阻塞为本地 backend 运行态损坏，现已恢复。
