# 执行日志：修复运行控制台总览超时

BDD: 总览接口不被单个慢探测拖到前端超时 -> Given 运行控制台需要展示 4 个环境和 4 个组件 / When 某个状态探测超过总览预算 / Then `/infra/runtime-control/overview` 应在前端 70 秒超时前返回，并将超时项标记为错误。

BDD: 慢探测错误必须显式展示 -> Given 某个远程状态脚本超时 / When 控制台展示状态矩阵 / Then 对应单元格应显示 `error` 与超时原因，不得静默降级为运行中。

REPRO: `Invoke-RestMethod http://localhost:48081/admin-api/infra/runtime-control/overview -TimeoutSec 70` -> FAIL, `seconds=70.02`, `The operation has timed out.`。

ROOT-CAUSE: `RuntimeControlServiceImpl.queryStatusesConcurrently()` 在同一条顺序 Stream 中先 `supplyAsync()` 再 `join()`，Stream 按元素推进导致每个状态探测提交后立即等待，16 个探测实际串行叠加，超过前端 70 秒超时。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getOverviewShouldQueryStatusesConcurrently test` -> FAIL, `maxActiveQueries` 未超过 1，证明总览状态探测没有重叠执行。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getOverviewShouldQueryStatusesConcurrently test` -> PASS。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 32 tests passed。

GREEN: 重新打包并重启本机 48081 后端，运行 jar `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260601-134533.jar`，`http://localhost:48081/v3/api-docs` -> HTTP 200。

GREEN: `Invoke-RestMethod http://localhost:48081/admin-api/infra/runtime-control/overview -TimeoutSec 70` -> PASS, `seconds=16.55`, `code=0`；Backup 后端与 Website 为 `running`，前端与整套返回真实 `degraded` 原因，未出现 `invalid-runtime-data-disk`。

VERIFY: Playwright 无痕会话使用注入 token 进入本机前端首页，未复现 `timeout of 70000ms exceeded`；该会话未展开到运行控制台动态路由，最终页面级判断以真实 `/overview` API 70 秒验证为准。
