# Execution Log

## BDD

BDD: 本机后端启动前必须拦截被篡改的展厅默认文件配置 -> Given 本机启动脚本准备启动后端 / When `infra_file_config.id=28` 的 bucket、endpoint 或 domain 偏离本机展厅受保护默认值 / Then 脚本必须直接失败，禁止继续启动。

BDD: 本机后端启动前必须拦截被篡改的展厅媒体 URL -> Given 本机展厅媒体记录依赖默认文件配置 28 / When `infra_file` 中任一 `showroom/%` 记录 URL 指向非默认受保护桶 / Then 脚本必须直接失败，禁止继续启动。

BDD: 本机 E2E 保护规则必须可回归验证 -> Given 启动脚本承担本机 fail-fast 保护 / When 未来有人修改脚本 / Then 自动化测试必须校验受保护默认 bucket、domain、endpoint 和错误码文案仍然存在。

## TDD Evidence

- STATUS: task-created -> 已建立本机 E2E 展厅文件配置保护任务，下一步补测试并记录 RED。
- RED: `python -m pytest script/tests/test_runtime_control_scripts.py -k showroom_default_file_config_from_e2e_mutation` -> FAIL, `restart-int-ruoyi-local.ps1` did not define `Assert-LocalShowroomFileConfigProtected`.
- GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -k showroom_default_file_config_from_e2e_mutation` -> PASS, `1 passed, 9 deselected`.
- GREEN: 脚本实现新增 `Assert-LocalShowroomFileConfigProtected`，在后端启动前强校验 `infra_file_config.id=28` 的 bucket、endpoint、domain 以及 `showroom/%` URL 域。
- REGRESSION: `powershell -ExecutionPolicy Bypass -File script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS, current local environment passed the new guard and completed backend restart.
- REGRESSION: `curl.exe -sS http://127.0.0.1:48081/actuator/health` -> PASS, got `{"status":"UP"}` after the guarded restart.
