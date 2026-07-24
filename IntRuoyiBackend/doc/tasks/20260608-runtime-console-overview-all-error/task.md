# 任务：恢复运行控制台概览状态

## 任务目标

修复本机运行控制台概览页组件显示“错误 unknown / ERROR”的运行态问题。目标是恢复本机后端 `48081` 到主工作区，并修复远程状态探针因固定 20 秒命令超时造成的假红；不得用 mock、默认成功或静默降级掩盖状态探针失败。

## 前置任务状态

- 已检查最近运行控制台任务 `20260608-runtime-console-empty-repo-root`，状态为 completed。
- 已检查当前最新任务 `20260608-runtime-build-release-showroom-option`，状态已记录为 blocked；阻塞原因为本机运行控制台后端不可用，需要先恢复运行态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；状态错误仍必须暴露真实错误，本次只把状态命令超时预算改为显式可配置并设置合理默认值。
- `是否从根因和长期维护角度解决`：是；先确认本机后端进程、健康检查、启动参数和运行控制台 API，再修复远程状态命令硬编码 20 秒超时导致的间歇性假红。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 本机后端健康后概览不全红 -> Given 本机后端 `48081` 正常运行 / When 运维打开运行控制台概览 / Then Local 后端状态不再显示 `错误 unknown / ERROR`。
- BDD: 后端不可用时不伪造成功 -> Given 本机后端 `48081` 未监听 / When 查询运行控制台概览 / Then 页面显示错误并保留真实失败原因。
- BDD: 远程状态探针在合理预算内返回真实状态 -> Given 远程状态脚本需要超过 20 秒但未超过配置的状态命令超时时间 / When 运维刷新运行控制台概览 / Then 状态卡显示脚本返回的真实 running 状态，而不是固定 20 秒超时导致的 `ERROR unknown`。

## 里程碑

- [x] M1：记录当前页面全错误和后端不可连接证据。
- [x] M2：定位本机后端启动来源、日志和参数。
- [x] M3：恢复本机后端运行态并确认健康检查。
- [x] M4：补充状态命令超时配置化修复，并用 RED/GREEN 单测验证。
- [x] M5：通过运行控制台真实页面验证状态恢复。
- [x] M6：记录验证证据、收尾预览，并按需提交当前任务改动。

## 预期验证

- `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 5`
- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest" test`
- Playwright 打开 `http://localhost:8081` 并进入运行控制台，确认 Local 后端状态不是全局错误。

## Current Status

completed: 已将本机 `8081/48081` 恢复到主仓库 `int_main`，新增 `yudao.runtime-control.status-command-timeout` 正式配置并默认 60 秒；API 与 Playwright 均确认 16 个状态项为 running。未执行正式服务器写动作。
