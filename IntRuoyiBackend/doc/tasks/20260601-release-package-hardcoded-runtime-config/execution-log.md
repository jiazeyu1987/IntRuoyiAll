# 执行日志：发布包内置 DCC 与 EDHR 运行时配置

BDD: NAS 发布包携带运行时配置 -> Given 发布脚本执行 `build-release` / When 发布包写入 NAS 前生成内容 / Then 包内必须包含 `runtime-env/test.env`、`runtime-env/prod.env`、`runtime-env/backup.env`，且包含 DCC 与 EDHR 必需配置。

BDD: 部署阶段使用发布包配置 -> Given `deploy-release` 已从 NAS 下载发布包 / When 目标环境为 test、prod 或 backup / Then 脚本必须优先读取 `runtime-env/<env>.env` 写入远端 `.env`，不要求操作员二次手动配置这些 key。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "release_package_embeds_runtime_env" -q` -> FAIL，发布脚本尚未实现 `Write-ReleaseRuntimeEnvPackage` / `Apply-ReleaseRuntimeEnvPackage`，发布包不会携带 `runtime-env/test.env|prod.env|backup.env`。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "release_package_embeds_runtime_env" -q` -> PASS，发布脚本已具备发布包内置 `runtime-env/test.env|prod.env|backup.env` 与部署读取契约。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "release_package_embeds_runtime_env or publish_runtime_requires_dcc_viewer_token_onlyoffice_and_download_encryption_configuration or publish_runtime_requires_dcc_signature_evidence_secret or publish_script_verifies_frontend_pdf_worker_mime_after_deploy" -q` -> PASS，DCC/EDHR 发布配置目标回归通过。

GREEN: PowerShell parser `ParseFile(script/deploy/publish-int-ruoyi.ps1)` -> PASS。

REGRESSION: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，剩余 3 个既有旧断言未通过：`NasShare` 默认值仍断言 `IT共享`，两个测试仍断言旧的硬编码 `docker compose up -d onlyoffice backend frontend`。本任务目标回归已通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260601-release-package-hardcoded-runtime-config --mode preview` -> PASS，keep `task.md` 与 `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-release-package-hardcoded-runtime-config --mode apply` -> PASS，deleted_paths `<none>`。
