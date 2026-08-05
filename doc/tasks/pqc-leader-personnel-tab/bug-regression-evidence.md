# PQC 人员接口运行态 404 回归证据

## Bug Summary

- 现象：PQC 组长打开 `人员管理` 时提示 `请求地址不存在:admin-api/mes/pro/process-pool/team-leader/pqc-personnel/list`。
- 期望：认证后的 `GET /admin-api/mes/pro/process-pool/team-leader/pqc-personnel/list` 进入正式 Controller，返回业务码 `0` 或明确的业务校验错误，不得返回“请求地址不存在”。

## Reproduction

- 用户真实页面路径已复现认证态接口不存在错误。
- `48081` 监听进程为 PID `60192`，运行包为 `output/runtime/int_main/backend-runtime-control-20260805-222248.jar`，健康检查为 `UP`。
- 只读检查运行包内嵌 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar`：
  - 存在 `MesProcessPoolTeamLeaderController.class`
  - 缺少 `MesPqcLeaderPersonnelService.class`
  - 缺少 `MesPqcLeaderPersonnelServiceImpl.class`
  - 缺少 `MesPqcLeaderPersonnelRespVO.class`

## Root Cause

源码中的 PQC personnel Controller 路由和服务已经存在，但 `48081` 仍运行 2026-08-05 22:22:40 生成的旧 Jar。旧 Jar 不含新增 PQC personnel 服务和 VO，Spring 无法加载包含这些依赖的新 Controller 版本，因此认证态请求落入不存在路由处理。

## Regression Test

- Controller 反射合同：`MesProcessPoolTeamLeaderControllerTest` 锁定四个 PQC personnel endpoint 和权限标识。
- 服务测试：`MesPqcLeaderPersonnelServiceTest` 锁定列表、关联、重复拒绝、状态更新和 scope 边界。
- 运行包合同：新 Jar 构建后必须只读检查内嵌 MES 模块包含上述 PQC class，再允许替换 `48081` 运行态。

## RED

- `RED: 运行中 Jar 内嵌 MES class 检查 -> FAIL，缺少 MesPqcLeaderPersonnelService、MesPqcLeaderPersonnelServiceImpl、MesPqcLeaderPersonnelRespVO`
- `RED: 用户真实页面 GET /admin-api/mes/pro/process-pool/team-leader/pqc-personnel/list -> FAIL，请求地址不存在`

## GREEN

- `GREEN: 隔离 worktree 目标 Surefire -> PASS，三个目标测试类共 21 个测试，失败 0、错误 0`
- `GREEN: 新 yudao-server-exec.jar 关键 class 检查 -> PASS，包含 PQC personnel Service、实现类、响应 VO 和 Controller`
- 待 `48081` 健康检查和认证态目标接口验证通过后补充运行态 GREEN。

## Risk And Regression Scope

- 只替换确认归属 `int_main` 的 PID `60192`，不停止其它 Java/Maven 进程。
- 新运行包必须来自隔离、可追溯、已验证的当前 HEAD，不从主工作区并行脏改动直接打包。
- 未登录 `401` 不能证明路由注册；必须用认证态真实页面或登录态请求取得业务响应。

## Blockers And Follow-up

- 当前仍需取得目标 Maven Surefire PASS，并生成包含新类的不可变运行 Jar。
