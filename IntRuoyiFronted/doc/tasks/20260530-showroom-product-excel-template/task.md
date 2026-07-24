# 任务：展厅产品 Excel 模板对齐（前端）

## 任务目标

让后台“展厅 / 产品管理”的导入入口、导出文件名、列表渲染和真实 E2E 比对契约对齐参考文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx`，并配合后端完成测试租户真实导入导出比对。

## 里程碑

- [x] M1：建立任务文档并确认前置任务完成。
- [x] M2：解析参考 workbook 的当前 15 列结构。
- [x] M3：补前端静态 RED 测试，锁定下载文件名、导入提示和导出入口。
- [x] M4：更新导入提示、下载工具和产品列表空中文名显示。
- [x] M5：更新真实比对脚本，覆盖测试租户导入、导出、文本和图片规则。
- [x] M6：运行前端静态检查、TypeScript 和真实 E2E。
- [x] M7：前端提示与验收脚本统一使用 `产品名-中文`，并严格校验图片数量。
- [x] M8：等待正式参考文件实际保存为 `产品名-中文` 后，重新运行测试租户真实导入、导出和严格比对。

## BDD 场景

- BDD: 产品管理下载导入模板 -> Given 用户在展厅产品管理页打开导入弹窗 / When 点击下载模板 / Then 前端请求展厅产品导入模板接口并下载 `产品资料修改版-补充产品资料.xlsx`。
- BDD: 产品管理导出 workbook -> Given 用户点击导出 / When 后端返回 Excel / Then 前端以 `产品资料修改版-补充产品资料.xlsx` 下载。
- BDD: 当前参考列提示 -> Given 当前参考 workbook 已删除旧 `产品` 列 / When 用户查看导入弹窗 / Then 只提示 `产品名-中文`、`卖点文案`、`产品图` 等当前列。
- BDD: 空中文名列表可见 -> Given 测试租户产品存在空 `产品名-中文` / When 用户进入产品管理 / Then 列表不抛错，空中文名按空文本显示。
- BDD: 测试租户严格比对 -> Given 用户以测试租户导入参考 workbook / When 再次导出 / Then 15 列表头、产品编码顺序、文本字段和按当前封面状态计算的产品图数量严格一致。

## 完成工作

- 更新导入弹窗提示为当前参考列：`产品名-中文`、`卖点文案`、`产品图`。
- 前端静态测试覆盖导入/导出接口、文件名、导入提示和下载工具 Blob 清理规则。
- `download.ts` 下载链接进入 DOM 后点击，并延后释放 Blob URL，避免大文件下载未接管就被释放。
- 产品列表允许空中文名渲染，继续对缺失字段和类型错误快速暴露。
- 真实 E2E 比对脚本连接当前前端 `18081` 和后端 `18083`，导入参考 workbook 后导出并逐字段比对；最终产品列表核对使用页面真实登录态和租户请求头。

## 验证结果

- RED: `node tests\e2e\showroom-product-excel-template-static.spec.js` -> FAIL，旧下载名和提示仍未对齐参考文件。
- RED: `node tests\e2e\showroom-product-excel-import-export.spec.js` -> FAIL，下载工具未将 Blob 下载链接挂入 DOM 且释放过早。
- GREEN: `node tests\e2e\showroom-product-excel-template-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS。
- GREEN: `pnpm run ts:check` -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。
- RED: 将静态测试和验收脚本改为 `产品名-中文` 后，前端旧提示仍未对齐新权威列。
- GREEN: `node tests\e2e\showroom-product-excel-template-static.spec.js; node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS。
- GREEN: `node --check doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS。
- GREEN: 使用仅将 B1 改为 `产品名-中文` 的验收副本运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入导出比对 `160/160` 行，文本差异 `0`，图片行按导入后的当前封面状态严格一致。
- BLOCKED: 使用正式文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 运行同一脚本 -> FAIL，导入响应 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 中文名权威列必须使用 \`产品名-中文\`，不能继续使用 \`产品-中文\``；当前磁盘文件 B1 仍为 `产品-中文`，正式验收需先保存为新表头。
- GREEN: 正式文件解锁后，将 B1 从 `产品-中文` 修正为 `产品名-中文`，校验仍为 `160` 行、`15` 列、`71` 张图片；运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入正式文件并导出比对 `160/160` 行，表头一致、产品编码顺序一致、文本差异 `0`、图片行按当前封面状态严格一致。

## 当前状态

Completed: 前端提示、静态测试、类型检查和正式参考 workbook 测试租户导入导出严格比对均已通过。

## Cleanup Keep

- doc/tasks/20260530-showroom-product-excel-template/scripts/compare-reference-after-import.e2e.js
