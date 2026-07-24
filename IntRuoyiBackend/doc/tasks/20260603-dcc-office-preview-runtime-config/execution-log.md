# 执行日志：DCC Office 预览本地 OnlyOffice 端口漂移

BDD: 本地 DCC Office 预览元数据必须指向真实 OnlyOffice 端口 -> Given 本机 OnlyOffice 服务由本地脚本运行在 `8080` / When 后端生成 DCC Office 预览元数据 / Then `onlyofficeBaseUrl` 必须使用 `http://127.0.0.1:8080`，不能漂移到未运行的 `8082`。

INFO: 上一后端任务 `20260603-restore-data-guide-alignment` 已标记 blocked，本任务只处理 DCC OnlyOffice 本地端口漂移。

RED: `python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> FAIL，`application-local.yaml` / `application-dev.yaml` 的 `DCC_ONLYOFFICE_BASE_URL` 默认值为 `http://127.0.0.1:8082`。

GREEN: `python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS，2 tests passed。

GREEN: `mvn -pl yudao-server "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Java 提交门禁 companion test 覆盖 local/dev YAML 不再包含 `8082` 且默认 `8080`。

GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS，本地重启脚本 OnlyOffice 契约通过。

VERIFY: 本机 backend 重启 -> PASS，`show-int-ruoyi-local-status.ps1` 显示 frontend=HTTP 200、backend=HTTP 200、OnlyOffice=HTTP 200。

E2E: 前端真实路径复跑 -> PASS，DCC 文件 `2054545668044047034` 的预览元数据返回 `onlyofficeBaseUrl=http://127.0.0.1:8080`，页面出现 OnlyOffice spreadsheet editor iframe。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-office-preview-runtime-config --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`，未执行 apply。
