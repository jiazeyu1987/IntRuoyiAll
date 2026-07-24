BDD: 本地后端监听契约可被健康检查验证 -> Given 本地后端按项目约定启动 / When 检查 48081 健康接口 / Then 返回可用状态而不是连接拒绝。
BDD: 后端未启动或端口漂移时会被明确识别 -> Given 本地后端未监听 48081 / When 执行端口与配置核对 / Then 能明确指出缺失前置条件或错误端口。
INFO: previous-task-blocked -> PASS，后端上一任务已显式转 blocked，本轮可开始本地运行态排查。
GREEN: backend-port-contract-check -> PASS，`yudao-server/src/main/resources/application-local.yaml` 与 `script/deploy/restart-int-ruoyi-local.ps1` 均将本机后端端口固定为 `48081`，未发现后端本地端口契约漂移。
GREEN: backend-runtime-health -> PASS，`Get-NetTCPConnection -LocalPort 48081 -State Listen` 显示 Java 进程监听，`Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
GREEN: backend-startup-window-check -> PASS，当前 Java 进程启动时间为 `2026-07-01 13:46:38`，`yudao-server.log` 记录 Tomcat 于 `2026-07-01 13:47:36.961` 才开始监听 `48081`，应用于 `2026-07-01 13:47:37.050` 启动完成；拒连发生在此窗口内符合预期。
GREEN: backend-endpoint-check -> PASS，`yudao-server.log` 在 `2026-07-01 13:55:52` 已成功处理 `/admin-api/system/dict-data/simple-list` 与 `/admin-api/system/auth/get-permission-info`，当前运行态链路正常。
