# 执行日志：修复发布包 NAS 清理 2250 误判失败

BDD: 发布包已上传后 NAS 映射不存在不应误判为构建失败 -> Given 发布脚本已经输出 `Release package uploaded to NAS` / When 清理 SMB 映射时 `net use /delete` 返回 `NET HELPMSG 2250` / Then 脚本应把该精确结果视为映射已不存在并完成清理，发布包构建仍返回成功。

BDD: NAS 清理不得吞掉其它错误 -> Given 清理 SMB 映射出现非 2250 错误 / When `net use /delete` 返回非零退出码 / Then 脚本必须继续失败并输出原始错误。

INFO: 用户提供的发布日志显示 Maven、前端、Website、Docker image build、docker save、NAS upload 均已成功；失败发生在 `net use "\\172.30.30.4\IT..." /delete /y`，输出 `NET HELPMSG 2250`，说明 SMB 映射在清理时已不存在。

RED: `mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，`PublishIntRuoyiScriptTest.nasDisconnectShouldTreatMissingMappingAsCompletedCleanupOnly` 断言失败：发布脚本缺少专用 NAS disconnect cleanup helper。

GREEN: `script/deploy/publish-int-ruoyi.ps1` -> PASS，`Disconnect-NasReleaseShare` 已改为调用 `Invoke-NasReleaseShareDisconnect`；该 helper 只在 `net use /delete` 退出码为 `2` 且输出包含 `NET HELPMSG 2250` 时视为映射已不存在，其它非零结果继续 `Fail`。

GREEN: `mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，1 test。

GREEN: `mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest,UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，4 tests，确认 NAS 清理修复与 413 Nginx 配置回归同时通过。

GREEN: PowerShell parser -> PASS，`publish-int-ruoyi.ps1` 无语法解析错误。

## Bug Regression Summary

Bug: `publish-int-ruoyi.ps1 -Mode build-release` 在发布包已成功上传 NAS 后，最后断开 SMB 映射返回 `NET HELPMSG 2250`，脚本仍以退出码 `2` 判定构建发布包失败。

Expected: 发布包已上传成功后，如果 `net use /delete` 明确返回 `NET HELPMSG 2250`，表示映射已不存在，应视为清理完成；其它非零清理错误仍必须失败。

Reproduction: 用户提供日志中 Maven、前端、Website、Docker image build、`docker save` 和 `Release package uploaded to NAS: Backup/ReleasePackage/26-06-01_21-17-12` 均已成功，随后 `net use ... /delete /y` 输出 `NET HELPMSG 2250` 并触发 `[FAIL] Shell command failed with exit code 2`。

Root Cause: `Disconnect-NasReleaseShare` 使用通用 `Invoke-CheckedShell` 执行 `net use /delete`，未区分“映射已不存在”的幂等清理结果和真实清理失败。

Verification: 新增 `PublishIntRuoyiScriptTest` 检查发布脚本必须通过专用 helper 处理 NAS disconnect，并显式识别 `NET HELPMSG 2250`；targeted Maven 测试和 PowerShell 解析检查均通过。

Blockers: 无。本次未重新执行完整 `build-release`，避免在 NAS 上重复生成新的大发布包；用户日志中的 `26-06-01_21-17-12` 包已显示上传成功。

## CI/CD Evidence Summary

Environment: local release package build mode and NAS release root.

Commands: targeted Maven regression tests and PowerShell parser check.

Secrets: NAS 凭据仍来自 `NasConfigPath`，未写入仓库。

Pipeline: build-release 上传包成功后执行 NAS disconnect cleanup；只有精确 2250 结果允许完成。

Verification: `docs/environments/ci-cd-evidence.md` 已记录本次 NAS cleanup 行为与回归测试。

Rollback: 回退 `publish-int-ruoyi.ps1` 和 `PublishIntRuoyiScriptTest.java`，再运行 targeted Maven 测试确认行为回退。

Blockers: 无。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260601-release-package-nas-disconnect-2250\execution-log.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs\environments\ci-cd-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-release-package-nas-disconnect-2250 --mode preview` -> PASS，keep `task.md`、`execution-log.md`，delete `<none>`，blocked `<none>`。

GREEN: 本地发布包核查 -> PASS，`tmp/publish-int-ruoyi/26-06-01_21-17-12/release-manifest.json` 存在，记录 `releaseTag=26-06-01 21:17:12`、`packageDirectoryName=26-06-01_21-17-12`，并包含 `intruoyi-images_26-06-01_21-17-12.tar`、`runtime-env/test.env|prod.env|backup.env`、Website dist 产物。

INFO: 由于用户日志已证明 `26-06-01_21-17-12` 包上传 NAS 成功，且失败点只在最后 SMB disconnect cleanup，本次未重复生成第二个大发布包。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "disconnects_nas_mapping_idempotently or frontend_nginx_allows_large_showroom_product_import_requests" -q` -> PASS，2 passed，35 deselected；脚本级测试覆盖 NAS 2250 幂等清理和发布用 Nginx 上传上限。
