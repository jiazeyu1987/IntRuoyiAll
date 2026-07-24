# 执行日志：修复本机重启脚本启动包

BDD: 本机后端重启脚本使用可执行包 -> Given 本机需要重新启动 48081 后端 / When 执行 restart-int-ruoyi-local.ps1 / Then 脚本应复制并启动 yudao-server-exec.jar，健康检查返回 UP。

RED: powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> FAIL，运行包复制为 31KB 的 `yudao-server.jar`，Java stderr 报 `没有主清单属性`，48081 健康检查超时。

GREEN: python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -k executable_backend_jar -q -> PASS，现有契约确认脚本使用 `target\yudao-server-exec.jar`，并禁止使用 `target\yudao-server.jar`。

GREEN: powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> PASS，脚本重新打包并派发 611MB 的可执行运行包。

GREEN: curl.exe --fail --silent --show-error --max-time 10 http://127.0.0.1:48081/actuator/health -> PASS，返回 `{"status":"UP"}`。
