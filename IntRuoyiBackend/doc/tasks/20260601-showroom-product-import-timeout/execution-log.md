# 执行日志：定位展厅产品导入耗时与前端超时

BDD: 真实产品资料导入耗时可见 -> Given 使用 `产品资料修改版-补充产品资料.xlsx` 通过真实导入接口导入 / When 后端处理完成 / Then 记录接口耗时、成功数、跳过数和失败数。

BDD: 后端导入错误必须快速可见 -> Given Excel 缺少必需字段或处理过程中发生真实错误 / When 调用导入接口 / Then 后端返回明确失败，不静默成功、不降级。

INFO: `D:\Downloads\产品资料修改版-补充产品资料.xlsx` -> 文件大小 `174146045` bytes；`产品列表` sheet 维度 `A1:O166`；内嵌媒体 `165` 个，图片未压缩总量约 `188575293` bytes。

RED: direct backend import with test tenant `测试租户/aoteman` and `sameProductAction=SKIP` -> FAIL, 后端在 `418 ms` 返回 `code=500`、`msg=上传文件过大，请确保文件小于配置的大小限制！`；后端当前不是慢路径，而是上传大小前置条件失败。

RED: `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增期望要求 `spring.servlet.multipart.max-file-size=256MB`、`max-request-size=300MB`，当前配置仍是 `100MB/120MB`。

GREEN: `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests；后端 multipart 默认上限已调整为 `256MB/300MB` 且仍保持有限值。

GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS，生成包含 `256MB/300MB` multipart 上限的新后端包。

VERIFY: 本机 `restart-ruoyi.bat` 返回非零，但后续健康检查确认 `http://127.0.0.1:48081/v3/api-docs` -> 200、`http://127.0.0.1:8081` -> 200；失败输出对应 Vite 启动期间短暂 `EMFILE: too many open files`，服务随后恢复。

GREEN: direct backend import after restart with test tenant `测试租户/aoteman` -> PASS, `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 返回 `code=0`，耗时 `59702 ms`，`totalRows=165`、`successCount=89`、`skippedCount=75`、`failureCount=1`，失败明细为第 50 行 `product_049` 当前产品缺少所属公司。

GREEN: Playwright real frontend import at `http://localhost:8081/showroom/product` with test tenant `测试租户/aoteman` -> PASS, 同一文件导入请求返回 `code=0`，等待响应耗时 `21058 ms`，浏览器控制台未出现 `timeout of 30000ms exceeded`、`ECONNABORTED` 或 `AxiosError`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，26 tests。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-showroom-product-import-timeout\bug-regression-evidence.md` -> PASS。

## Bug Regression Summary

Bug: 产品管理导入 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 时，真实文件大小为 `174146045` bytes，旧后端 multipart 默认上限 `100MB/120MB` 无法接收该文件。

Expected: 后端应以有限但覆盖当前产品资料业务文件的上传上限接收导入请求，并在导入处理完成后返回真实成功、跳过与失败明细。

Reproduction: 文件大小 `174146045` bytes，`产品列表` 为 `A1:O166`，内嵌 `165` 个媒体文件；修复前测试租户直接调用导入接口返回上传文件过大。

Root Cause: `yudao-server/src/main/resources/application.yaml` 中 `spring.servlet.multipart.max-file-size=100MB`、`max-request-size=120MB`，低于真实产品资料包大小。

Verification: 后端 multipart 默认上限已调整为有限的 `256MB/300MB`；新运行包直接导入真实文件返回 `code=0`，并通过产品导入集成回归测试。

Blockers: 无阻塞。真实导入中第 50 行 `product_049` 因当前产品缺少所属公司失败，属于业务数据前置条件缺失。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-showroom-product-import-timeout\execution-log.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode preview` -> PASS，计划仅保留 `task.md`、`execution-log.md`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode apply` -> PASS，仅删除附属 `bug-regression-evidence.md`，保留核心任务记录。

BLOCKED: user objective switched to `product_001` 透明封面导入与 Website 发布 E2E -> BLOCKED, 本任务尚未完成最终验证和提交；当前改动保持原样，后续需单独恢复验证与提交。

INFO: RESUME -> 用户要求提交前后端代码，已按本任务 `task.md` 的 completed 状态恢复收尾验证与提交流程。

GREEN: `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-showroom-product-import-timeout --mode preview` -> PASS，keep `task.md`、`execution-log.md`，delete `<none>`，blocked `<none>`。
