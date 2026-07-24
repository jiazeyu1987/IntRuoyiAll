# 20260602 展厅产品正式版导入所属公司 124 修复

## Task Goal

修复导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 时第 2 行 `product_001` 因当前产品 `owner_company_id=124` 无法反查公司主数据而失败的问题，确保导入逻辑以正式、可维护方式处理当前产品所属公司校验。

## Milestones

- [x] M1: 记录任务、确认上一个后端任务已完成，并定位导入失败根因。
- [x] M2: 补充 BDD 场景和 RED 回归测试，复现 `owner_company_id=124` 反查公司缺失导致导入失败。
- [x] M3: 实现最小正式修复，不引入 fallback、降级、吞异常或临时绕过。
- [x] M4: 运行目标测试和相关回归验证，记录 GREEN 证据。
- [x] M5: 执行收尾清理预览，提交本任务直接产生的改动。

## Expected Verification

- `ShowroomProductExcelImportExportIntegrationTest` 中新增回归测试先在修复前失败，失败原因包含 `当前产品所属公司不存在，无法导入发布：124`。
- 修复后新增回归测试通过，并确认导入 `product_001` 不再依赖过期的当前产品公司 ID 反查。
- 相关产品 Excel 导入导出测试通过。
- 任务证据满足 bug-regression-fix-loop 校验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；当前目标是将 Excel 持证公司校验从脆弱的当前产品公司 ID 反查，调整为导入时可验证的正式公司契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Notes

- 上一个后端任务 `20260602-runtime-console-build-deploy-test-media` 已标记 `completed`。
- 当前后端仓库存在与本任务无关的 DCC/NAS 未提交改动和 `runtime/` 未跟踪目录，本任务不修改、不暂存。
- RED 验证已复现用户报错：第 2 行 `product_001` 因 `owner_company_id=124` 反查公司失败，导入结果 `successCount=0`。
- 修复后导入行的 `持证公司=瑛泰` 会解析到当前租户公司主数据并写回正确 `owner_company_id`，不再依赖旧产品中的 124。
- Targeted 回归与整组产品 Excel 导入导出集成测试均已通过。
- `bug-regression-fix-loop` 证据校验通过；`task-closeout-cleanup` 预览结果 delete `<none>`、blocked `<none>`。

## Blocker

- 已恢复并完成，无剩余阻塞。
