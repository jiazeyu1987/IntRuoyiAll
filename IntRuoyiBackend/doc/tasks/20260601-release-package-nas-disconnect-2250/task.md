# 任务：修复发布包 NAS 清理 2250 误判失败

## 任务目标

修复 `publish-int-ruoyi.ps1 -Mode build-release` 在发布包已成功上传 NAS 后，最后执行 `net use <share> /delete /y` 返回 `NET HELPMSG 2250` 时被误判为构建发布包失败的问题。

## 前序任务检查

- 上一任务 `doc/tasks/20260601-test-server-showroom-product-import-413/task.md` 当前为 `blocked_on_test_server_authorization`，阻塞点是测试服务器发布/验证授权；本任务只处理本机发布脚本 NAS 清理误判，不触碰服务器。

## BDD 场景

BDD: 发布包已上传后 NAS 映射不存在不应误判为构建失败 -> Given 发布脚本已经输出 `Release package uploaded to NAS` / When 清理 SMB 映射时 `net use /delete` 返回 `NET HELPMSG 2250` / Then 脚本应把该精确结果视为映射已不存在并完成清理，发布包构建仍返回成功。

BDD: NAS 清理不得吞掉其它错误 -> Given 清理 SMB 映射出现非 2250 错误 / When `net use /delete` 返回非零退出码 / Then 脚本必须继续失败并输出原始错误。

## 里程碑

- [x] M1：建立任务文档并记录用户提供的失败日志根因。
- [x] M2：新增发布脚本 NAS 清理 2250 回归测试并记录 RED。
- [x] M3：最小修复 `Disconnect-NasReleaseShare`，只接受精确 2250 清理结果。
- [x] M4：运行 targeted 回归验证。
- [x] M5：记录 CI/CD 与 bug 证据，执行收尾清理预览。

## 预期验证

- RED：发布脚本文本回归测试在缺少 2250 处理时失败。
- GREEN：回归测试通过，确认脚本只对 `NET HELPMSG 2250` 做幂等清理处理。
- REGRESSION：不改变构建、上传、下载、mark-tested、deploy-release 的失败门禁；非 2250 清理错误仍 fail-fast。

## 当前状态

status: completed

## 当前结果

已修复发布包上传成功后 NAS 映射清理返回 `NET HELPMSG 2250` 被误判失败的问题。脚本现在只把精确 2250 识别为“映射已不存在，清理完成”；其它非零退出码仍然 fail-fast。

## 验证记录

- RED：`PublishIntRuoyiScriptTest` 在缺少 NAS disconnect cleanup helper 时失败。
- GREEN：`mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN：`mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest,UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，4 tests。
- GREEN：PowerShell parser -> PASS。
- GREEN：bug-regression evidence validator -> PASS。
- GREEN：CI/CD evidence validator -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，delete `<none>`、blocked `<none>`。
- GREEN：本地发布包 `tmp/publish-int-ruoyi/26-06-01_21-17-12/release-manifest.json` 存在，含镜像 tar、runtime-env、website 产物清单；用户日志已显示该包上传 NAS 成功。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "disconnects_nas_mapping_idempotently or frontend_nginx_allows_large_showroom_product_import_requests" -q` -> PASS，2 passed。

## 阻塞

None.
