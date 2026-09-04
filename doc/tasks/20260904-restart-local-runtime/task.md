# 20260904 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 前后端；如标准重启过程中出现编译错误，先按真实编译错误修复后再重启。

## Milestones

- [x] 读取本机运行、前后端开发、端口和收尾规则。
- [x] 记录当前工作区和端口运行态。
- [x] 执行标准本地 full 重启脚本。
- [x] 若出现编译错误，记录 RED，修复后取得 GREEN。
- [x] 验证后端 `48081` health 和前端 `8081` HTTP 状态。
- [x] 记录收尾状态与阻塞项。

## Expected Verification

- `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` 退出码为 `0`。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- 如出现编译修复，记录对应 RED/GREEN 命令和结果。

## Current Status

ready_for_closeout

请求范围内的前后端标准重启已完成并验证通过；cleanup apply 已通过且无删除项。当前未获得 Git 提交/推送授权，因此不执行提交、推送或远端操作，任务记录保留在 `ready_for_closeout`。

## Design Constraint Check

- 禁止换端口、跳过组件、使用旧 Jar 或 mock 成功。
- 禁止强停归属不明进程；仅允许停止确认属于 `E:\IntRuoyi` / `int_main` 的旧前后端进程。
- 当前请求未明确授权 Git 提交、推送、远程服务器、数据库写入或 E2E。
