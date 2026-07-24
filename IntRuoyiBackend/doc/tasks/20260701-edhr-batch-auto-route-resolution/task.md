# 任务：eDHR 批次执行自动识别工艺路线（后端）

- Task ID: `20260701-edhr-batch-auto-route-resolution`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

修改 `MesProEdhrBatchExecutionServiceImpl.openOrCreate(...)`，当请求未显式携带 `routeId` 时，后端根据工单正式生产任务上下文自动解析唯一工艺路线；若无法唯一解析，则返回正式失败原因。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\task.md`
- 状态：`blocked`
- 处理说明：上一后端任务已显式阻塞，不影响本轮 eDHR 路线自动识别修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/测试/Markdown 统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。由后端依据工单正式任务/路线上下文解析唯一路线，不保留 UI 手填 routeId 或“随便猜一条路线”的绕过逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: routeId 缺省时按工单自动解析路线 -> Given 工单存在唯一有效正式任务路线 / When 调用 openOrCreate 且 routeId 为空 / Then 服务自动解析该 routeId 并成功创建 eDHR 批次执行。`
- `BDD: routeId 缺省但无可用路线时 fail fast -> Given 工单没有可用正式任务路线 / When 调用 openOrCreate 且 routeId 为空 / Then 返回明确的工艺路线缺失错误，不创建批次执行。`

## Milestones

1. M1：确认现有工单/任务/路线关联数据源与现有测试基线。`completed`
2. M2：补 RED 测试，覆盖 routeId 缺省自动解析与缺失失败。`completed`
3. M3：实现后端自动解析逻辑并跑 GREEN。`completed`
4. M4：补 backend evidence。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest" test`

## Current Blockers

- 无。

## Final Verification Result

- 已实现 routeId 缺省时按工单正式生产任务自动解析唯一路线。
- 已补三条后端回归测试覆盖自动解析成功、无路线失败、多路线失败。
- `validate_backend_api.py` 已通过，closeout preview 已确认仅 `backend-api-evidence.md` 为默认可清理候选。
- 已刷新本地 `yudao-module-system` / `yudao-module-erp` SNAPSHOT，并补齐 `mes_pro_work_order` H2 测试表字段，后端定向 `mvn` 验证已通过。
