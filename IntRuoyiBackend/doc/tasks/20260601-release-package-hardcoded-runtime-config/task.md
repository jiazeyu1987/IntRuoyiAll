# 任务：发布包内置 DCC 与 EDHR 运行时配置

## 任务目标

- 修改 `publish-int-ruoyi.ps1`，让 `build-release` 打到 NAS 的发布包内置测试、正式、备份环境运行时配置。
- `deploy-release -Environment test|prod|backup` 从发布包读取对应环境配置生成远端 `.env`，不再要求部署时二次修改服务器或临时补环境变量。
- 前期按用户要求使用明文写死配置，不增加加密配置包或服务器二次解密流程。

## 前序任务检查

- 上一相关任务 `doc/tasks/20260601-pdf-worker-mime-runtime/task.md` 当前状态为 `blocked_on_missing_dcc_runtime_secrets`。
- 本任务直接处理该阻塞来源：DCC viewer token、OnlyOffice 与下载加密/EDHR S3 配置在发布包内置后，部署阶段不再因本机部署进程缺少这些变量而失败。

## BDD 场景

- BDD: NAS 发布包携带运行时配置 -> Given 发布脚本执行 `build-release` / When 发布包写入 NAS 前生成内容 / Then 包内必须包含 `runtime-env/test.env`、`runtime-env/prod.env`、`runtime-env/backup.env`，且包含 DCC 与 EDHR 必需配置。
- BDD: 部署阶段使用发布包配置 -> Given `deploy-release` 已从 NAS 下载发布包 / When 目标环境为 test、prod 或 backup / Then 脚本必须优先读取 `runtime-env/<env>.env` 写入远端 `.env`，不要求操作员二次手动配置这些 key。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：增加发布包配置契约 RED 测试。
- [x] M3：实现发布包明文运行时配置生成与部署读取。
- [x] M4：运行目标回归测试和 PowerShell 解析验证。
- [x] M5：收尾清理预览，更新任务状态。

## 预期验证

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "release_package_embeds_runtime_env" -q`
- PowerShell parser `ParseFile(script/deploy/publish-int-ruoyi.ps1)` 通过。

## 当前状态

status: completed

## Current Status

completed

## 进展记录

- M2 完成：新增发布包内置运行时配置契约测试，当前按预期 RED，证明发布脚本尚未生成/读取 `runtime-env/*.env`。
- M3 完成：发布脚本新增 DCC 前期硬编码默认值；`build-release` 会生成 `runtime-env/test.env`、`runtime-env/prod.env`、`runtime-env/backup.env`；`deploy-release` 从发布包读取目标环境 env 后再写远端 `.env`。
- M4 完成：目标发布配置回归与 PowerShell parser 均通过；完整发布工具测试文件仍有 3 个既有旧断言失败，和本任务无关。
- M5 完成：收尾清理预览通过，无待删除项、无阻塞、无警告。

## 最终验证

- `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "release_package_embeds_runtime_env or publish_runtime_requires_dcc_viewer_token_onlyoffice_and_download_encryption_configuration or publish_runtime_requires_dcc_signature_evidence_secret or publish_script_verifies_frontend_pdf_worker_mime_after_deploy" -q` -> PASS。
- PowerShell parser `ParseFile(script/deploy/publish-int-ruoyi.ps1)` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260601-release-package-hardcoded-runtime-config --mode preview` -> PASS。
- 完整 `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` 仍 FAIL 于 3 个既有旧断言：`NasShare` 默认值、两个旧 OnlyOffice 硬编码启动断言。
