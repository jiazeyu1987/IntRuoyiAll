# 任务：修复本地运行控制台后端重启脚本

## 任务目标

- 修复 `restart-int-ruoyi-local.ps1 -Component backend` 构建成功但后端没有在 `48081` 启动的问题。
- 保证本地 `localhost:8081` 运行控制台点击 `发布测试服` 时，能连到当前代码的本地后端动作接口，而不是命中旧实例或静态资源错误。
- 不执行真实发布动作，只做未登录接口探测和前端请求拦截验证。

## BDD 场景

- BDD: 本地后端重启后动作接口存在 -> Given 运维人员重启本地后端, When 探测 `POST /admin-api/infra/runtime-control/actions`, Then 请求进入当前后端 Controller，未登录返回 `401`，不得返回 `No static resource`。
- BDD: 本地前端发布请求路由正确 -> Given 本地前端 `localhost:8081` 打开运行控制台, When 填写 `发布测试服` 原因并确认, Then 浏览器发出到 `127.0.0.1:48081/admin-api/infra/runtime-control/actions` 的 POST，测试中止请求且不执行真实发布。

## 里程碑

- [x] M1: 复现本地后端重启失败原因。
- [x] M2: 增加失败脚本契约测试。
- [x] M3: 修复后端启动命令构造。
- [x] M4: 重启本地后端并验证接口。
- [x] M5: 回归本地前端 E2E 请求路由。

## 预期验证

- RED：`python -m pytest script\tests\test_runtime_control_scripts.py -q` 先失败，证明脚本仍使用易碎的 `java -jar` 续行命令。
- GREEN：上述测试通过。
- GREEN：`restart-int-ruoyi-local.ps1 -Component backend` 成功启动本地后端。
- GREEN：未登录 `POST http://127.0.0.1:48081/admin-api/infra/runtime-control/actions` 返回 `401`。
- GREEN：本地前端发布请求路由 E2E 通过，且请求被拦截中止。

## 当前状态

- 状态：completed
- 已完成：
  - 已复现：本地 `127.0.0.1:48081` 未监听。
  - 已定位：后端启动日志显示 `--server.port=48081`、JDBC URL 中 `&` 被 PowerShell 当作命令语法解析，后端未启动。
  - 已修复：后端启动命令改为 `$backendArgs` 参数数组并用 `& java @backendArgs` 调用。
  - 已验证脚本契约测试通过。
  - 已重启本地后端，`48081` 健康检查通过。
  - 已确认未登录动作接口返回 `401`，不再返回静态资源错误。
  - 已跑本地前端发布请求路由 E2E，测试中止请求，未执行真实发布。
  - 已完成相关脚本测试、bug evidence validator 与 closeout cleanup preview。
- 阻塞与影响：
  - 暂无。
