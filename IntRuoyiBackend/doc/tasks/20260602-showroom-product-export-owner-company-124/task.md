# 20260602 展厅产品导出所属公司 124 修复

## Task Goal

修复展厅产品管理点击导出时因产品 `owner_company_id=124` 无法反查公司而失败的问题，确保导出路径不再复用导入发布错误语义，并从正式公司契约处理历史脏所属公司 ID。

## Milestones

- [x] M1: 建立任务记录、确认上一个后端任务已完成，并定位导出失败根因。
- [x] M2: 补充 BDD 场景和 RED 回归测试，复现导出遇到历史 `owner_company_id=124` 的失败。
- [x] M3: 实现最小正式修复，不引入 fallback、降级、吞异常或临时绕过。
- [x] M4: 运行目标测试和相关回归验证，记录 GREEN 证据。
- [x] M5: 执行收尾清理预览，提交本任务直接产生的改动。

## Expected Verification

- `ShowroomProductExcelImportExportIntegrationTest` 中新增导出回归测试先在修复前失败，失败原因包含 `当前产品所属公司不存在，无法导入发布：124`。
- 修复后导出 `owner_company_id=124` 且 `product_owner_type=YINGTAI` 的历史产品成功，Excel `持证公司` 输出当前租户瑛泰公司正式名称。
- 相关产品 Excel 导入导出测试通过。
- 任务证据满足 bug-regression-fix-loop 校验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将产品导出所属公司文本解析调整为基于当前公司主数据的正式导出契约，并对历史瑛泰产品的旧 ID 做确定性归一，不吞掉不可识别的数据问题。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Notes

- 上一个后端任务 `20260602-showroom-images-broken` 已标记 `completed`。
- 当前后端仓库存在与本任务无关的 DCC/NAS 未提交改动和 `runtime/` 未跟踪目录，本任务不修改、不暂存。
- 初步定位：导出 `toProductExcelRow()` 调用 `resolveOwnerCompanyExcelText(fields.get("owner_company_id"))`，该方法复用导入发布错误文案，对历史 `owner_company_id=124` 反查失败时抛出 `当前产品所属公司不存在，无法导入发布：124`。
- RED 验证已复现用户报错：导出产品 `EXCEL-STALE-OWNER` 时因 `owner_company_id=124` 反查公司失败，测试在控制器导出调用处抛出同款错误。
- 修复后导出使用专用所属公司文本解析：有效 ID 仍按公司主数据输出；历史缺失 ID 只有在 `product_owner_type=YINGTAI` 且当前租户存在唯一瑛泰公司时归一到该公司名称；非瑛泰历史脏 ID 仍明确失败。
- Targeted 回归与整组产品 Excel 导入导出集成测试均已通过。
- `bug-regression-fix-loop` 证据校验通过；`task-closeout-cleanup` 预览结果 delete `<none>`、blocked `<none>`、warnings `<none>`。

## Blocker

- 无。
