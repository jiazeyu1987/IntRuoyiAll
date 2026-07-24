# 20260615 展厅产品资料导出奖项页签可回导

## 任务目标

让产品管理导出的 `产品资料修改版-补充产品资料.xlsx` 同时包含 `产品列表` 与 `奖项` Sheet，并确保导出的 `奖项` Sheet 可被现有导入逻辑直接识别和导入。

## 里程碑

1. M1 审计：确认现有产品导出、模板、奖项导入图片解析和导出器结构。
2. M2 RED：新增导出包含奖项 Sheet、奖项封面嵌入、导出后回导、缺封面失败测试。
3. M3 GREEN：实现奖项导出行构建、奖项 Sheet 写入、封面嵌入、模板奖项示例。
4. M4 REGRESSION：运行 showroom Excel 相关 Maven 测试和服务端编译/打包必要检查。
5. M5 收尾：记录证据、清理预览并提交当前任务改动。

## 已完成工作

- 扩展 `/showroom/product/export-excel`：保留 `产品列表` Sheet，同时新增无表头 `奖项` Sheet。
- 奖项 Sheet 按导入合同输出 A-D 列，并将封面图片嵌入 E 列。
- 扩展 `/showroom/product/get-import-template`：模板包含 `产品列表` 与 `奖项` Sheet，奖项示例行带示例图片。
- 缺少可导出奖项、奖项封面为空、奖项封面内容不可读或奖项编码不符合 `AWARD-xxx` 时 fail fast。
- 新增/更新集成测试覆盖导出、模板、回导解析和失败场景。

## 预期验证

- 导出 workbook 包含 `产品列表` 和 `奖项` 两个 Sheet。
- `奖项` Sheet A-D 列为序号、中文名、日期/期限、颁发单位，E 列嵌入封面图片。
- 导出的 workbook 可被现有导入逻辑读取奖项并生成同样的 `AWARD-xxx` 编码。
- 奖项缺封面或封面内容不可读时导出失败，不生成不可回导文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少奖项封面或图片不可读时直接失败。
- `是否从根因和长期维护角度解决`：是；导出合同与导入合同对齐为同一 workbook 结构。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- Status: completed
- 状态：COMPLETED。
- 最终验证：`mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS, 32 tests。

## Current Status

completed
