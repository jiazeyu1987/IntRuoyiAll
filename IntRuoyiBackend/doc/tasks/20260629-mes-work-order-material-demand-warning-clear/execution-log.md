# 执行日志：MES 工单清理剩余物料需求 warning 后端实现

BDD: 工单物料需求补齐后不再出现缺料需求 warning -> Given 工单具备完整物料需求前置 / When 执行自动排产预览 / Then 不再返回 issueType=MATERIAL_DEMAND。

BDD: 仍缺物料需求时继续暴露真实原因 -> Given 工单对应 BOM 或物料需求仍缺失 / When 执行自动排产预览 / Then 系统继续返回真实 warning，而不是静默消失。

GREEN: previous-task-check -> PASS，上一后端任务 `20260629-mes-work-order-zero-generated-followup` 已完成。
GREEN: experience-index-hit -> PASS，已命中并读取 `docs/powershell-memory.md`。
GREEN: experience-index-hit-login -> PASS，已命中并读取 `docs/login-access.md`。
GREEN: experience-preflight -> PASS，本次先在本机范围内执行真实库只读核查与真实预览复验；未确认根因前不做库写入。
GREEN: material-demand-root-cause -> PASS，真实库与代码双重核对确认 `MATERIAL_DEMAND` 由工单 `925553` 的工单 BOM 为空触发；同产品 `907175` 的在制工单 BOM 也整体为空。
GREEN: formal-bom-sync-root-cause -> PASS，真实 `/mes/pro/work-order/925553/sync-erp-bom` 与 `/mes/md/product-bom/907175/sync-erp-bom` 都失败在 `A002.11.001.257001` 缺少本地 MES 物料映射。
GREEN: mes-item-sync-blocker -> PASS，真实 `/mes/md/item/sync-kingdee` 失败并由日志锁定为 `张` 单位重复主数据导致的 `TooManyResultsException`。
GREEN: experience-preflight-write -> PASS，本轮正式库写入前已完成租户、接口权限、编码与命令路径复核。
GREEN: unit-master-repair -> PASS，真实 `/mes/md/item/update` 成功迁移物料 `919223` 的计量单位引用，真实 `/mes/md/unit-measure/delete?id=900055` 成功删除重复单位；`/mes/md/item/sync-kingdee` 恢复成功。
GREEN: erp-product-gap-fill -> PASS，真实 `/erp/product/create` 正式补入 `A002.11.001.257001` 与 `A001.05.102.0009`，随后真实 `/mes/md/item/sync-kingdee` 返回 `createdCount=2`。
GREEN: bom-sync-recovered -> PASS，真实 `/mes/md/product-bom/907175/sync-erp-bom` 与 `/mes/pro/work-order/925553/sync-erp-bom` 均成功返回 `syncedBomCount=29`；真实库回查产品 BOM、工单 BOM 有效行均为 `29`。
GREEN: material-demand-preview-cleared -> PASS，真实 `/mes/pro/auto-schedule/preview` 重放当前排产工单快照后返回 `generatedTaskCount=67`、`blockingIssueCount=0`、`issueTypeCounts={"MATERIAL":29}`，`MATERIAL_DEMAND` 已消失。
GREEN: boundary-confirmed -> PASS，当前仅剩 `MATERIAL` 缺料预警，属于真实库存/备料问题，不再属于 BOM 或主数据缺口。
