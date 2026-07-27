# Bug Regression Evidence

## Symptom

批记录表单列表可以加载，但选择已发布表单后右侧详情区域显示 `Request failed with status code 500`，页面顶部同时提示请求出错 500。

## Expected Behavior

选择有效的已发布批记录表单后，详情接口应成功返回并在右侧展示表单信息。

## Reproduction

- 路径：`http://127.0.0.1:8081/mes/pro/batch-record-form-list`
- 身份：本机默认 `芋道源码/admin`
- 失败请求：`GET /admin-api/mes/pro/batch-record-report/cell-rules?reportId=a5c282e25c7b4e7baaa08570f65e5607`
- 结果：HTTP 500，右侧详情显示 `Request failed with status code 500`。
- 日志：Jimu MiniDAO 更新模板缺失，并连续出现 Hutool、Spring、Tomcat 类加载失败。

## Root Cause

运行中的 PID `46388` 直接引用 Maven `target/yudao-server-exec.jar`。进程启动时间为 `2026-07-27 20:33:12`，Jar 后续于 `2026-07-27 20:54:09` 被重新打包覆盖。运行中归档被替换后，JVM 延迟加载嵌套依赖和资源时发生 `NoClassDefFoundError`/`TemplateNotFoundException`。

## Regression Test

- `RED: Playwright 真实页面 -> FAIL`：目标 `cell-rules` 请求返回 HTTP 500。
- `RED: runtime Jar integrity probe -> FAIL`：监听进程 Jar 位于 `target` 且 Jar 修改时间晚于进程启动时间。
- `GREEN: 标准重启与页面复验 -> PASS`：进程引用 `output\runtime\int_main` 的独立 Jar，Jar 修改时间不晚于进程启动时间，真实页面目标请求返回 HTTP 200。

## Fix

通过正式本地后端启动脚本停止已确认归属的旧进程，重新构建并复制不可变运行 Jar 后启动。

## Verification

- 标准重启后，后端 PID 为 `4000`，health 为 `UP`。
- 运行 Jar 位于 `output/runtime/int_main`，且修改时间早于进程启动时间。
- Playwright 真实页面中 `cell-rules` 与 `signature-cell-markers` 均返回 HTTP 200，右侧“产品信息”预览正常显示。
- 后端运行期间重新执行完整 Maven package 后，`target` Jar 已更新，但运行 PID 和独立运行 Jar 不变；再次通过 Playwright 复验 HTTP 200。

## Risk

仅重启本机 `int_main` 后端。不得停止未知进程、不得改端口、不得修改数据源或业务数据。

## Blockers And Follow-up

- 产品运行问题已解除。
- Git 收尾受共享 `int_main` 并行未提交改动阻塞，未提交或推送本任务文档与经验门禁。
