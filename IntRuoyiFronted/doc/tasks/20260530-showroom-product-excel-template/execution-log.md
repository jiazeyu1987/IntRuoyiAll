# 执行记录：展厅产品 Excel 模板对齐（前端）

## 2026-05-30 继续：统一 `产品名-中文`

BDD: 当前参考列提示 -> Given 当前参考 workbook 已删除旧 `产品` 列 / When 用户查看导入弹窗 / Then 提示当前 `产品名-中文`、`卖点文案`、`产品图` 列。

BDD: 测试租户严格比对 -> Given 用户以测试租户导入参考 workbook / When 再次导出 / Then 表头、产品编码、文本字段和按当前封面状态计算的产品图数量严格一致。

RED: `node tests\e2e\showroom-product-excel-template-static.spec.js; node tests\e2e\showroom-product-excel-import-export.spec.js` -> FAIL，静态契约改为 `产品名-中文` 后，旧导入提示仍未对齐新权威列。

GREEN: `node tests\e2e\showroom-product-excel-template-static.spec.js; node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS。

GREEN: `pnpm run ts:check` -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。

GREEN: `node --check doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS。

GREEN: 使用仅将 B1 改为 `产品名-中文` 的验收副本运行 `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入导出比对 `160/160` 行，文本差异 `0`，产品图数量按导入后的当前封面状态严格一致。

BLOCKED: 使用正式文件 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 运行同一 E2E -> FAIL，导入响应 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID: 中文名权威列必须使用 \`产品名-中文\`，不能继续使用 \`产品-中文\``；当前磁盘文件仍为旧 `产品-中文` 表头，正式验收需先保存为新表头。

BLOCKED: 重新检查正式文件 -> Sheet `产品列表` 仍为 `160` 行、`71` 张图片，表头第 2 列仍是 `产品-中文`；目录中存在 `~$产品资料修改版-补充产品资料.xlsx` 锁文件。同一正式 E2E 再次失败于 `SHOWROOM_PRODUCT_IMPORT_HEADER_INVALID`，需要先关闭/保存 Excel，让 B1 实际落盘为 `产品名-中文`。

BLOCKED: 第三次恢复检查正式文件 -> Sheet `产品列表` 仍为 `160` 行、`71` 张图片，表头第 2 列仍是 `产品-中文`；锁文件 `~$产品资料修改版-补充产品资料.xlsx` 仍存在。当前无法完成“正式参考 Excel 导入 -> 导出 -> 160 行严格比对”，需外部保存正式 workbook 为 `产品名-中文` 后恢复。

GREEN: 正式文件解锁后，将 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 的 B1 从 `产品-中文` 修正为 `产品名-中文`；校验 Sheet `产品列表` 仍为 `160` 行、`15` 列、`71` 张图片。

GREEN: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入正式参考文件后导出比对 `160/160` 行，表头一致，产品编码顺序一致，文本字段差异 `0`，导出图片行与导入后当前封面状态 `160/160` 一致。

BDD: 产品管理下载导入模板 -> Given 用户在展厅产品管理页打开导入弹窗 / When 点击下载模板 / Then 前端请求展厅产品导入模板接口并下载参考文件名。

BDD: 产品管理导出 workbook -> Given 用户点击导出 / When 后端返回 Excel / Then 前端以 `产品资料修改版-补充产品资料.xlsx` 下载。

BDD: 当前参考列提示 -> Given 当前参考 workbook 已删除旧 `产品` 列 / When 用户查看导入弹窗 / Then 提示当前 `产品-中文`、`卖点文案`、`产品图` 列。

BDD: 空中文名列表可见 -> Given 测试租户产品存在空 `产品-中文` / When 用户进入产品管理 / Then 列表不因 `nameCn` 为空抛错。

BDD: 测试租户严格比对 -> Given 用户以测试租户导入参考 workbook / When 再次导出 / Then 15 列表头、产品编码顺序和文本字段一致，产品图按“有图替换、无图保留封面”规则比对。

RED: `node tests\e2e\showroom-product-excel-template-static.spec.js` -> FAIL，旧实现下载名仍为 `展厅产品导入模板.xls`，导入提示仍含旧列。

GREEN: `node tests\e2e\showroom-product-excel-template-static.spec.js` -> PASS，导入模板下载名、导出文件名和当前列提示通过。

RED: `node tests\e2e\showroom-product-excel-import-export.spec.js` -> FAIL，下载链接未挂入 DOM，Blob URL 释放过早。

GREEN: `node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS，下载工具约束和导入/导出前端契约通过。

GREEN: `pnpm run ts:check` -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 通过。

RED: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> FAIL，测试租户真实导入后，产品列表因第 10 行空 `nameCn` 抛错，loading 遮罩未退出。

GREEN: 产品列表允许空中文名后，真实 E2E 可继续执行导出。

RED: `node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> FAIL，导出响应为 JSON 错误，后端暴露外部封面 URL 不支持。

GREEN: 后端支持外部封面 URL 后，`node doc\tasks\20260530-showroom-product-excel-template\scripts\compare-reference-after-import.e2e.js` -> PASS，测试租户真实导入 `160` 行、失败 `0`；导出后比对 `160/160` 行，15 列表头一致，产品编码顺序一致，文本字段差异 `0`，参考 71 行产品图全部保留，89 行为空图行保留已有封面。
