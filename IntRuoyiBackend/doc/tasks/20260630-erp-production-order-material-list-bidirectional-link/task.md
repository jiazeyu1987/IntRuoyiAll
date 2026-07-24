# 任务：生产订单与生产用料清单双向关联展示（后端）

- Task ID: `20260630-erp-production-order-material-list-bidirectional-link`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在后端正式接口中补齐生产工单页和 ERP 生产用料清单页所需的双向关联字段：生产工单接口返回关联生产用料清单摘要；生产用料清单分组/明细接口返回对应生产工单信息。基于现有 `mes_kingdee_production_material_list` 与 `mes_pro_work_order` 的正式关联字段实现，不改库结构。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-approval-center-tenant1-visibility-fix\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，可开始本次新任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`docs\integrations\kingdee-erp-official-docs.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/Markdown/日志统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - ERP 关联口径以现有同步 DTO、DO 与 Mapper 为准。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式 VO/查询服务收口关联展示，避免前端自行拼推断。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 生产工单接口返回生产用料清单摘要 -> Given 某生产工单对应一张或多张生产用料清单 / When 查询生产工单列表 / Then 返回可用于前端展示的生产用料清单单据号摘要与数量。`
- `BDD: 生产工单无关联时返回空结果 -> Given 某生产工单没有生产用料清单 / When 查询生产工单列表 / Then 接口返回空摘要而不是伪造默认值。`
- `BDD: 生产用料清单分组接口返回对应生产工单 -> Given 某生产用料清单已映射本地生产工单 / When 查询生产用料清单分组或明细 / Then 返回 workOrderId/workOrderCode 等对应字段。`
- `BDD: 生产用料清单未映射时保留空关联 -> Given 某生产用料清单未映射本地工单 / When 查询接口 / Then workOrderId/workOrderCode 为空，保持真实状态。`

## Milestones

1. M1：建立后端任务文档并锁定接口边界。`completed`
2. M2：补 RED 测试锁定 VO/查询合同。`completed`
3. M3：实现查询与响应映射。`completed`
4. M4：定向回归、证据回填与验证脚本。`completed`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListQueryServiceImplTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\backend-api-evidence.md`

## Final Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListQueryServiceImplTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\backend-api-evidence.md` -> PASS
- 本地真实数据扫描 -> PASS，响应字段已对外可见；当前测试租户暂无已同步出的真实双向关联样本。

## Current Blockers

- 无代码阻塞；仅当前本地测试租户暂无真实关联样本。
