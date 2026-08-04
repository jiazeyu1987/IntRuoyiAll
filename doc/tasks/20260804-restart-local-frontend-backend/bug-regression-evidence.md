# Bug Regression Evidence

## Bug Summary

后端 runtime Jar 启动到 Spring Bean 创建阶段后退出，`48081` 无监听。日志显示 `mesProRouteFlowConfigServiceImpl` 创建失败，根因是 `getRouteFlowProcessConfigList(Long, String)` 业务查询方法被误标注 `@Resource`，Spring 将其当作资源注入方法处理并要求单参数 setter。

## Expected Behavior

`MesProRouteFlowConfigServiceImpl` 的依赖字段可使用 `@Resource` 注入；业务方法不得带 `@Resource`。标准本机后端重启后，`http://127.0.0.1:48081/actuator/health` 必须返回 `UP`。

## Reproduction

- Runtime path: `output\runtime\int_main\backend-runtime-control-20260804-153949.jar`
- Failure log: `output\runtime\int_main\backend-runtime-control-20260804-153949.out.log`
- Root error: `@Resource annotation requires a single-arg method`

## Root Cause

`getRouteFlowProcessConfigList(Long, String)` 是业务查询方法，不是单参数资源注入 setter；误标注 `@Resource` 会让 Spring 在 Bean 后处理阶段把它当作资源注入方法解析并直接阻塞应用上下文启动。

## Regression Test

- `MesProRouteFlowConfigServiceImplTest#routeFlowProcessQueryMethods_shouldNotBeResourceInjectionMethods` 反射检查 `getRouteFlowProcessConfigList(Long, String)` 不得带 `@Resource`。

## RED:

- Runtime RED evidence: prior standard backend startup using `output\runtime\int_main\backend-runtime-control-20260804-153949.jar` failed with `@Resource annotation requires a single-arg method`, leaving `48081` without a listener.
- Unit RED was not re-observed in this continuation because the regression guard and source fix were already present in the working tree when this run started.

## GREEN:

- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest#routeFlowProcessQueryMethods_shouldNotBeResourceInjectionMethods" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0`。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health -TimeoutSec 10` -> PASS，`status=UP`。
- `Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/ -TimeoutSec 10` -> PASS，HTTP `200`。

## Verification

标准本机状态脚本返回 `Status: running`，`HTTP: frontend=HTTP 200; backend=HTTP 200`，`Runtime: frontend=listening; backend=listening`。

## Risk And Regression Scope

- 风险范围：MES 工艺路线流程配置服务 Spring 启动扫描。
- 预期最小修复：只移除误标注的业务方法 `@Resource`，不改变业务逻辑、接口、SQL 或运行端口。

## Blockers And Follow-Up

- 后端启动阻塞已解除。
- 当前工作区存在大量并行脏改动且 `int_main...origin/int_main [ahead 9]`；本次未提交、未推送，避免混入其它任务改动。
