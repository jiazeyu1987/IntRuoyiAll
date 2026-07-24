# Execution Log

BDD: backend-only 发布不构建 website -> Given 用户只需要发布后端修复 / When 执行统一发布脚本并显式选择 backend 组件 / Then 脚本只校验并构建 backend 所需内容，不执行 website/showroom 构建、同步与健康检查。

BDD: website-only 发布不要求 DCC backend 运行时门禁 -> Given 用户只需要手动发布展厅 / When 执行统一发布脚本并显式选择 website 组件 / Then 脚本只校验 website 所需前置，不因 DCC viewer token、OnlyOffice 或下载加密配置缺失而失败。

BDD: full 发布保持现有全量校验 -> Given 用户执行全量发布 / When 选择 full 组件 / Then backend、frontend、website 与相关运行时门禁、构建和部署流程保持现有严格校验。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "component or website_component or builds_backend_frontend_and_website_runtime or stages_website_runtime_before_switching_remote_directory"` -> FAIL，新增合同断言证明现状脚本没有组件参数，backend 发布仍会落到 website 路径和 DCC backend secret 门禁。

GREEN: 修改 `script/deploy/publish-int-ruoyi.ps1`，新增 `-Component full|backend|frontend|website`，并以 `$publishBackend / $publishFrontend / $publishWebsite` 控制 runtime gate、构建、镜像导出、远端拷贝、服务启动与健康检查。

GREEN: 为 `frontend-only` / `website-only` 路径补充远端 `.env` 读取与回填逻辑，避免非 backend 发布把远端现有 backend runtime 配置写成空值。

GREEN: 更新 `script/tests/test_publish_int_ruoyi_to_test_tooling.py`，将统一发布合同扩展为组件级合同，并同步 `NasShare` 改为配置驱动默认空值。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py` -> PASS，44 passed。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_deploy_services.py` -> PASS，6 passed。

GREEN: `python -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py` -> PASS，4 passed。

RED: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment test -ReleaseTag 20260603_website_entry_readback_nostore ...` -> FAIL，website-only 部署仍无条件执行 backend `Assert-RequiredDatabaseSqlScriptsInRelease`，发布包不包含 `required-sql` 时被 backend 门禁阻断。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "required_sql_package_gate" -q` -> FAIL，新增回归断言证明 `Assert-RequiredDatabaseSqlScriptsInRelease` 没有被 `$publishBackend` 包裹。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "required_sql_package_gate" -q` -> PASS，1 passed, 44 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，45 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_deploy_services.py -q` -> PASS，6 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS，4 passed。

GREEN: 真实 website-only 发布包部署到测试服与正式服均通过，证明 backend required SQL 门禁已限定在 backend 组件路径。
