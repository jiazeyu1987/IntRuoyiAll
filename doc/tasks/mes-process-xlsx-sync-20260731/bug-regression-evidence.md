# MES 工序运行态路由缺失回归证据

## Bug Summary

用户反馈 `admin-api/mes/pro/mes-process/page` 返回“请求地址不存在”。预期行为是 MES 工序分页接口必须由正式 `/mes/pro/mes-process/page` Controller 处理，并返回与 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx` / `二代压力泵` Excel 行 2-33 完全一致的 32 条只读目录数据。

## Expected

- `/admin-api/mes/pro/mes-process/page` 必须进入正式 `MesProMesProcessController`，不能返回“请求地址不存在”。
- 登录态分页必须返回 `code=0`、`total=32`，首尾工序与 Excel 源一致。

## Reproduction

- 运行态 RED：`node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> FAIL，actuator mappings 缺少 `/mes/pro/mes-process/page`。
- 运行 Jar 核对：旧 `output\runtime\int_main\backend-runtime-control-20260731-200535.jar` 未加载新增 `MesProMesProcessController`。
- 数据链路 RED：路由加载后首次登录态请求 `/admin-api/mes/pro/mes-process/page?pageNo=1&pageSize=50` -> `code=500`，日志根因为本机库缺少 `mes_pro_mes_process_catalog`。

## Root Cause

- 当前 48081 后端仍运行旧 runtime Jar，构建时间早于 MES 工序独立 Controller，因此 Spring MVC 没有注册 `/mes/pro/mes-process/page`。
- 本机 Docker MySQL 尚未应用 `20260731_mes_process_catalog_from_pressure_pump_xlsx.sql`，导致新 Controller 进入服务层后查询目录表报表不存在。
- 本次未引入 fallback、空页兜底、替代接口或吞异常；修复收敛到正式 runtime Jar 加载和正式目录 SQL 数据链路。

## Regression Test

- 新增运行态检查脚本：`doc/tasks/mes-process-xlsx-sync-20260731/tools/check-mes-process-runtime-route.mjs`，通过 actuator mappings 断言本机 48081 已注册 `/mes/pro/mes-process/page`。
- 复跑既有静态合同：`node tests\e2e\mes-pro-mes-process-readonly-static.spec.js`，继续校验前端 API、页面、SQL、租户忽略和 Excel 32 行基线。
- 运行登录态 API 探针，断言分页返回 `code=0`、`total=32`、首条 `粗洗`、末条 `W包装打包`。

## RED:

- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> FAIL，缺少 MVC mapping `/mes/pro/mes-process/page`。
- 登录态 API 探针 -> FAIL，`code=500`，原因是 `mes_pro_mes_process_catalog` 表不存在。

## GREEN:

- `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，生成新 `yudao-server-exec.jar`。
- Runtime reload -> PASS，48081 新 PID `54564` 使用 `output\runtime\int_main\backend-runtime-control-20260801-002326.jar`，health `UP`。
- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> PASS，找到 MVC mapping `/mes/pro/mes-process/page`。
- Local SQL apply -> PASS，目录表 32 行，Excel 源行号 2-33，排序 1-32，设备明细 19 行。
- 登录态 API 探针 -> PASS，`/admin-api/mes/pro/mes-process/page?pageNo=1&pageSize=50` 返回 `code=0`、`total=32`，首条 `粗洗`，末条 `W包装打包`。
- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS。

## Risk And Scope

- 修改范围是本机运行态 Jar 切换、本机 Docker MySQL 应用既有正式 SQL、任务文档和运行态检查脚本；没有改动业务代码逻辑。
- 当前共享工作区存在无关第三方报工进度任务改动，且最新并发基线提交 `ec52d8dc8` 已混入多个任务区域；本轮未 stage、未 commit、未 push。
- 重新运行 `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 当前被无关历史 MES 测试 `testCompile` 缺失符号阻塞；本轮运行态路由、DB count、登录态分页和静态合同均已通过。

## Verification

- Bug regression validator should pass after this evidence file includes `Bug`、`Expected`、`Reproduction`、`Root Cause`、`RED:`、`GREEN:`、`Verification` 和 `Blockers` markers.
- 任务验证报告已同步记录运行态 Jar、SQL apply、API PASS 和 Maven 无关阻塞边界。

## Blockers And Follow-Up

- Final cleanup/commit/push 仍需在无关并发改动收敛后执行，避免把第三方报工进度任务文件混入本任务收尾。
- 若需要恢复 MES 模块 Maven 目标单测闭环，需要另开范围修复无关历史 MES 测试缺失符号。
