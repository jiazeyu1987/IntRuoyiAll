# 验证报告

## Summary

- Runtime result: PASS。`int_main` 后端已重新监听 `48081`，健康状态连续两次为 `UP`。
- Build result: FAIL。标准重启脚本的 Maven package 被共享工作区中 25 个 MES 编译错误阻塞，未生成本任务新 Jar；该失败未被隐藏。
- Recovery source: 并行任务在构建失败后恢复了共享后端，本任务检测到新监听后未再次中断该运行态。

## Runtime Evidence

- Old PID: `59012`，运行 `backend-runtime-control-20260807-upload-taxonomy-permission.jar`，已由标准脚本停止。
- New PID: `59460`，启动时间 `2026-08-07 17:10:21`。
- Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-frontline-pqc-order-product-summary.jar`。
- SHA-256: `974F8BB0F65AC3D26F173B8DD874EEA9E110846E42426BB5BE6E031A7132CA3D`。
- Immutable Jar check: Jar 修改时间 `2026-08-07 16:51:35`，早于进程启动时间。
- Command ownership: `--server.port=48081`、`--spring.profiles.active=local`、`--yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend` 均存在。
- Health: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 连续两次返回 `status=UP`。

## Build Failure Evidence

- Command: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend`。
- Result: `BUILD FAILURE`，耗时 `09:11`，失败模块 `yudao-module-mes`，共 25 个编译错误。
- Representative failures: PQC 请求对象缺少 getter、PQC 服务接口与实现返回类型不一致、路线工序候选构造参数不一致。
- Impact: 本次标准脚本未构建 `yudao-server`，不能宣称当前共享源码可打包。
- Scope: 本任务未修改或回退这些并行开发文件。

## Residual Risk

- 当前运行 Jar 来自仍为 `in_progress` 的并行任务 `20260807-frontline-pqc-order-product-summary`。本报告只证明本地后端已重启且健康，不证明该并行功能任务已完成全部回归或验收。

## Closeout

- `task-closeout-cleanup --mode preview` -> PASS，`blocked=<none>`、`warnings=<none>`，仅保留三份正式任务记录。
- `task-closeout-cleanup --mode apply` -> PASS，无删除项。
- Final task status: `completed`。
