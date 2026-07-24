# 执行日志：修复展厅产品导入前端 30 秒超时

BDD: 大文件产品导入不被 30 秒前端超时中断 -> Given 用户在产品管理选择处理时间超过 30 秒的产品 Excel / When 点击导入确认 / Then 前端应等待导入接口返回并展示真实导入结果。

BDD: 导入接口仍保持失败可见 -> Given 导入接口返回业务失败或网络错误 / When 前端等待导入结果 / Then 页面应展示真实错误，不吞掉异常、不伪造成功。

INFO: `D:\Downloads\产品资料修改版-补充产品资料.xlsx` -> 文件大小 `174146045` bytes；`产品列表` sheet 维度 `A1:O166`；内嵌媒体 `165` 个，图片未压缩总量约 `188575293` bytes。

RED: direct backend import with test tenant `测试租户/aoteman` and `sameProductAction=SKIP` -> FAIL, 后端在 `418 ms` 返回 `code=500`、`msg=上传文件过大，请确保文件小于配置的大小限制！`；证明当前真实错误是上传大小前置条件不满足，前端 30 秒超时会遮蔽该错误。

RED: `node scripts\showroom-admin-product-import-form.test.mjs` -> FAIL, 新增测试 `Showroom product import API uses product import request timeout` 期望 `SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT = 5 * 60 * 1000` 且导入接口显式传入该 timeout，当前 `importProductExcel` 仍继承全局 `30000ms`。

GREEN: `node scripts\showroom-admin-product-import-form.test.mjs` -> PASS, 6 tests；产品导入 API 已显式使用 `SHOWROOM_PRODUCT_IMPORT_REQUEST_TIMEOUT = 5 * 60 * 1000`。

GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS，生成包含 `256MB/300MB` multipart 上限的新后端包。

VERIFY: 本机 `restart-ruoyi.bat` 返回非零，但后续健康检查确认 `http://127.0.0.1:48081/v3/api-docs` -> 200、`http://127.0.0.1:8081` -> 200；失败输出对应 Vite 启动期间短暂 `EMFILE: too many open files`，服务随后恢复。

GREEN: direct backend import after restart with test tenant `测试租户/aoteman` -> PASS, `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 返回 `code=0`，耗时 `59702 ms`，`totalRows=165`、`successCount=89`、`skippedCount=75`、`failureCount=1`，失败明细为第 50 行 `product_049` 当前产品缺少所属公司。

GREEN: Playwright real frontend import at `http://localhost:8081/showroom/product` with test tenant `测试租户/aoteman` -> PASS, 同一文件导入请求返回 `code=0`，等待响应耗时 `21058 ms`，弹窗展示 `总行数：165 / 成功发布：0 / 跳过无变化：164 / 失败数量：1`，浏览器控制台未出现 `timeout of 30000ms exceeded`、`ECONNABORTED` 或 `AxiosError`。

GREEN: `node tests\e2e\showroom-product-excel-import-export.spec.js` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-showroom-product-import-timeout\bug-regression-evidence.md` -> PASS。

## Bug Regression Summary

Bug: 产品管理导入 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 时前端报 `AxiosError: timeout of 30000ms exceeded`，用户看不到真实导入结果或真实失败原因。

Expected: 产品 Excel 导入应支持当前业务验收文件大小和处理时长；若导入失败，应展示后端真实错误，不能被全局 30 秒 Axios timeout 遮蔽。

Reproduction: 文件大小 `174146045` bytes，内嵌 `165` 个媒体文件；修复前后端直接导入返回上传文件过大，前端导入 API 未设置接口专用 timeout。

Root Cause: 后端 multipart 默认上限低于真实产品资料包；前端产品导入 API 同时继承全局 `30000ms` 请求超时，长上传或长处理会先抛 Axios timeout。

Verification: 前端导入 API 已使用 5 分钟专用 timeout；Playwright 真实前端导入同一文件返回 `code=0`，无 `timeout of 30000ms exceeded`、`ECONNABORTED` 或 `AxiosError`。

Blockers: 无阻塞。导入结果中的 `product_049` 失败是业务数据前置条件缺失，不是本次超时修复失败。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-showroom-product-import-timeout\execution-log.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode preview` -> PASS，计划仅保留 `task.md`、`execution-log.md`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode apply` -> PASS，仅删除附属 `bug-regression-evidence.md`，保留核心任务记录。

BLOCKED: user objective switched to `product_001` 透明封面导入与 Website 发布 E2E -> BLOCKED, 本任务尚未完成真实前端路径验证和提交；当前改动保持原样，后续需单独恢复验证与提交。

INFO: RESUME -> 用户要求提交前后端代码，已按本任务 `task.md` 的 completed 状态恢复收尾验证与提交流程。

GREEN: `node scripts\showroom-admin-product-import-form.test.mjs` -> PASS，6 tests。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode preview` -> PASS，keep `task.md`、`execution-log.md`，delete `<none>`，blocked `<none>`。
