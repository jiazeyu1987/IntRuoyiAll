# 任务：修复 DCC Office 预览本地 OnlyOffice 端口漂移

## 任务目标

修复后端本地/开发配置中 DCC OnlyOffice 预览地址默认指向 `http://127.0.0.1:8082`，而本机 OnlyOffice 服务和本地重启脚本实际使用 `http://127.0.0.1:8080`，导致前端受控 Office 预览加载错误文档服务脚本的问题。

## 上一任务检查

- 上一个后端任务 `20260603-restore-data-guide-alignment` 已因用户切换到 DCC Office 预览空白缺陷标记 `blocked`。
- 本任务只修改 DCC OnlyOffice 本地运行配置契约、任务证据和必要 YAML，不接管或回滚恢复数据、DCC 下载、发布脚本等无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅纠正本地运行配置来源，不增加备用端口或下载降级。
- `是否从根因和长期维护角度解决`：是。用配置契约测试锁定 `application-local.yaml`、`application-dev.yaml` 与本地 OnlyOffice 运行端口一致。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 本地 DCC Office 预览元数据必须指向真实 OnlyOffice 端口 -> Given 本机 OnlyOffice 服务由本地脚本运行在 `8080` / When 后端生成 DCC Office 预览元数据 / Then `onlyofficeBaseUrl` 必须使用 `http://127.0.0.1:8080`，不能漂移到未运行的 `8082`。

## 里程碑

- [x] M1：建立后端任务文档并隔离上一后端任务。
- [x] M2：补充 RED 配置契约测试，复现 local/dev YAML 指向 `8082`。
- [x] M3：最小修复 local/dev OnlyOffice base-url。
- [x] M4：运行配置契约测试与真实浏览器回归。
- [x] M5：记录最终证据并完成收尾。

## 预期验证

- RED：`python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` 失败，指出 local/dev YAML 仍指向 `http://127.0.0.1:8082`。
- GREEN：`python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` 通过。
- E2E：前端真实路径 `http://localhost:8081/dcc/controlled-file/detail/2054545668044047034?viewer=1&from=detail` 不再静默空白。

## 当前状态

completed

## 已完成工作

- `application-local.yaml` 与 `application-dev.yaml` 的 DCC OnlyOffice `base-url` 默认值已从 `http://127.0.0.1:8082` 修正为 `http://127.0.0.1:8080`。
- 新增 `script/tests/test_dcc_onlyoffice_local_runtime_config.py` 和 `yudao-server/src/test/java/cn/iocoder/yudao/server/DccOnlyOfficeLocalConfigTest.java`，锁定 local/dev YAML 与本地 OnlyOffice 端口一致。
- 本机 backend 已重启到 `48081`，真实前端路径复跑时预览元数据返回 `onlyofficeBaseUrl=http://127.0.0.1:8080`。

## 最终验证结果

- `python -m pytest script\tests\test_dcc_onlyoffice_local_runtime_config.py -q` -> PASS，2 tests passed。
- `mvn -pl yudao-server "-Dtest=DccOnlyOfficeLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS。
- `show-int-ruoyi-local-status.ps1` -> PASS，frontend/backend/OnlyOffice 均 HTTP 200。
- 真实前端路径复跑 -> PASS，同一 DCC xlsx 预览返回 `onlyofficeBaseUrl=http://127.0.0.1:8080` 且显示 OnlyOffice iframe。
- 收尾清理预览：`task_closeout.py --task-id 20260603-dcc-office-preview-runtime-config --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
