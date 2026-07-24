# 20260615 展厅产品资料导出奖项页签可回导

## 任务目标

产品管理现有导出按钮继续下载 `产品资料修改版-补充产品资料.xlsx`，但导出的文件需包含可回导的 `奖项` Sheet；导入说明同步提示导出文件可再次导入。

## 里程碑

1. M1 审计：确认前端导出按钮、导入模板说明和 API 类型。
2. M2 RED：新增静态测试覆盖导出入口和导入说明。
3. M3 GREEN：更新导入/模板说明，不改变按钮位置和文件名。
4. M4 REGRESSION：运行前端静态测试和类型检查。
5. M5 收尾：记录证据、清理预览并提交当前任务改动。

## 已完成工作

- 更新产品导入弹窗说明：导出文件可再次导入，且包含 `产品列表` 与 `奖项` 页签。
- 明确奖项导入合同：A 列序号、B 列中文名、C 列日期/期限、D 列颁发单位、E 列首图封面，E 列必须放图片。
- 更新前端静态测试，锁定导出入口、文件名和奖项回导说明。

## 预期验证

- 导出仍调用 `/showroom/product/export-excel`，文件名仍为 `产品资料修改版-补充产品资料.xlsx`。
- 页面说明明确导出文件包含产品列表和奖项页签，可直接导入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；导出错误继续展示后端错误。
- `是否从根因和长期维护角度解决`：是；前端只表达后端正式导出合同，不本地伪造文件。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- Status: completed
- 状态：COMPLETED。
- 最终验证：`node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS；`node tests\e2e\showroom-product-excel-template-static.spec.js` -> PASS；`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## Current Status

completed
