# 任务：修复展厅产品导入前端 30 秒超时

## 任务目标

修复产品管理导入 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 时前端报 `AxiosError: timeout of 30000ms exceeded` 的问题，确保导入结果能够按真实后端处理结果展示，不因固定 30 秒请求超时提前失败。

## 前序任务检查

- 已确认上一前端任务 `doc/tasks/20260531-showroom-product-import-admin-real-e2e/task.md` 状态为 completed。
- 当前前端仓库存在无关改动 `src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 和旧任务目录，本任务不触碰、不提交这些无关文件。

## BDD 场景

- BDD: 大文件产品导入不被 30 秒前端超时中断 -> Given 用户在产品管理选择处理时间超过 30 秒的产品 Excel / When 点击导入确认 / Then 前端应等待导入接口返回并展示真实导入结果。
- BDD: 导入接口仍保持失败可见 -> Given 导入接口返回业务失败或网络错误 / When 前端等待导入结果 / Then 页面应展示真实错误，不吞掉异常、不伪造成功。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现并记录 `产品资料修改版-补充产品资料.xlsx` 真实导入耗时。
- [x] M3：补充前端请求超时 RED 回归测试。
- [x] M4：最小修复并运行 GREEN/前端回归。
- [x] M5：真实前端路径导入验证，收尾清理预览并提交本任务直接相关改动。

## 预期验证

- RED：前端产品导入请求在 30 秒超时场景下先失败。
- GREEN：产品导入接口允许等待超过 30 秒并返回真实结果。
- REGRESSION：相关前端导入测试通过；真实路径使用 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 不再出现 30 秒 Axios timeout。

## 当前状态

status: completed

## Current Status

completed

已完成修复与验证。产品导入 API 已使用 5 分钟专用 timeout；真实前端路径导入 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 不再出现 30 秒 Axios timeout，并显示后端真实导入结果。

## 最终验证

- `node scripts\showroom-admin-product-import-form.test.mjs` -> PASS，6 tests。
- `node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS。
- Playwright 真实前端导入 -> PASS，接口返回 `code=0`，无 `timeout of 30000ms exceeded`、`ECONNABORTED` 或 `AxiosError`。
