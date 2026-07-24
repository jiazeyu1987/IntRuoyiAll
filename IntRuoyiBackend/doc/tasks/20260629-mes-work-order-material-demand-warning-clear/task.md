# 任务：MES 工单清理剩余物料需求 warning 后端实现

## 任务目标

- 继续排查工单 `TESTERPA9ED2D417434` 当前仅剩的 `MATERIAL_DEMAND` warning 后端根因。
- 优先补齐真实物料需求/BOM 前置，避免通过后端写死、跳过校验或隐藏 warning 的方式处理。
- 所有修改仅限本机范围并保持可追溯。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-work-order-zero-generated-followup\task.md`
- 状态：`COMPLETED`
- 处理说明：已解除 `LATEST_START` 发布阻断并成功生成排产任务；本次继续处理 `MATERIAL_DEMAND` warning。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 真实库查询与日志写入必须显式 UTF-8。
  - 若需要真实登录与页面复验，必须先执行官方 `login-preflight.mjs`。
  - 若需要真实库写入，必须先记录 `GREEN: experience-preflight -> PASS`。
  - 每次判断都必须落到真实预览或真实库核查，不得只凭代码阅读下结论。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先补真实物料需求链路，不隐藏 warning。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工单物料需求补齐后不再出现缺料需求 warning -> Given 工单具备完整物料需求前置 / When 执行自动排产预览 / Then 不再返回 issueType=MATERIAL_DEMAND。`
- `BDD: 仍缺物料需求时继续暴露真实原因 -> Given 工单对应 BOM 或物料需求仍缺失 / When 执行自动排产预览 / Then 系统继续返回真实 warning，而不是静默消失。`

## 里程碑

1. M1：建立后端物料 warning 跟进任务并继承上轮结论。`COMPLETED`
2. M2：定位 `MATERIAL_DEMAND` warning 根因。`COMPLETED`
3. M3：补齐物料需求前置并回查。`COMPLETED`
4. M4：确认该工单排产结果无剩余 warning 或记录真实边界。`COMPLETED`

## 预期验证

- 真实库核对工单、产品路线、BOM 与物料需求数据。
- 真实调用 `/admin-api/mes/pro/auto-schedule/preview`，确认 `MATERIAL_DEMAND` warning 是否消失。

## 最终结论

- 自动排产 `MATERIAL_DEMAND` warning 的直接原因已解除：`workOrderBomService.getWorkOrderMaterialDemandMapByWorkOrderIds(...)` 不再对工单 `925553` 返回空集合，`mes_pro_work_order_bom` 已通过正式同步恢复为 `29` 条有效行。
- 更上游的正式产品 BOM 主数据也已恢复：`mes_md_product_bom(item_id=907175, deleted=0)=29`。
- 修复闭环保持在正式主数据链路内完成：
  - 修复重复单位 `张` 的脏数据，恢复 `/mes/md/item/sync-kingdee`
  - 用正式 `/erp/product/create` 补齐缺失 ERP 产品 `A002.11.001.257001`、`A001.05.102.0009`
  - 再执行正式 `/mes/md/item/sync-kingdee`
  - 成功执行正式 `/mes/md/product-bom/907175/sync-erp-bom`
  - 成功执行正式 `/mes/pro/work-order/925553/sync-erp-bom`
- 真实排产预览复验：
  - 请求：`workOrderIds=[925553]`、`scheduleOrderIds=[48]`、`startTime=2026-06-29 19:10:00`、`capacityMode=PLANNED`、`preserveManualLockedTasks=true`
  - 结果：`generatedTaskCount=67`、`blockingIssueCount=0`
  - `issues` 中已不存在 `MATERIAL_DEMAND`，仅剩 `29` 条 `MATERIAL` 缺料预警
- 当前剩余预警的真实性边界：`MATERIAL` 是库存/备料不足，不再是“工单缺少物料需求/BOM 未生成”的主数据阻断。
