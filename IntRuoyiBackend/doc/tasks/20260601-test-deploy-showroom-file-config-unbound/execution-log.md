# Execution Log

BDD: 测试服发布包恢复必须重绑定展厅文件配置 -> Given 发布包数据库仍包含本机 MinIO 配置 `127.0.0.1:9000` / When `deploy-release` 将发布包恢复到测试服 / Then 发布脚本必须在校验前把 `infra_file_config.id=28` 的 endpoint 重写为 `http://host.docker.internal:9000`、domain 重写为 `http://172.30.30.58:9000/yudao`，并同步替换 `infra_file.url` 中的本机地址。

BDD: 文件配置重绑定失败必须阻断发布 -> Given 导入后的 `infra_file_config.id=28` 仍包含本机地址 / When 发布脚本执行配置校验 / Then 脚本必须输出 `SHOWROOM_FILE_CONFIG_UNBOUND` 并失败，不得继续启动或返回成功。

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_applies_showroom_file_config_rebind_for_code_only_packages" -q` -> FAIL, 当前脚本仍使用 `if ($Mode -eq 'deploy-release' -and -not $SkipDatabaseSync)`，code-only 发布包不会生成和应用目标环境 rebind SQL。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_applies_showroom_file_config_rebind_for_code_only_packages" -q` -> PASS, 1 passed / 38 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS, 43 passed。

GREEN: PowerShell PSParser on `script\deploy\publish-int-ruoyi.ps1` -> PASS, `PSParser OK`。

INFO: 测试服只读 SQL 核验 -> PASS, `infra_file_config.id=28` 当前为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`，不再包含 `127.0.0.1:9000`。

GREEN: 测试服封面 HTTP 核验 -> PASS, `http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260530/product-product_001-cover.png` 和 `.../20260601/product-product_003-imported-cover-9bb9e37aa0cfa1e8.png` 均返回 `200 image/png`。

GREEN: Playwright 真实浏览器核验 -> PASS, 使用 `芋道源码/admin` 登录测试服 `http://172.30.30.58:8081/showroom/product`；页面 `加载失败` 数量为 0，前 20 个封面请求均返回 `200 image/png`，图片 DOM 均 `complete=true` 且 `naturalWidth > 0`，无 failed request。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260601-test-deploy-showroom-file-config-unbound --mode preview` -> PASS, keep `task.md` / `execution-log.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260601-test-deploy-showroom-file-config-unbound --mode apply` -> PASS, deleted_paths `<none>`。

INFO: 用户要求避免重跑完整发布并再次确认 -> 执行轻量复核，不重新发布、不改测试服数据。

GREEN: 测试服文件配置复核 -> PASS, `infra_file_config.id=28` 为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`has_localhost=0`。

GREEN: 测试服音频 HTTP 复核 -> PASS, `product_001` 至 `product_008` 最新中英音频共 14 条均通过 `8081/admin-api/infra/file/28/get/...` 返回 `HTTP 200 audio/vnd.wave`，MinIO 直连返回 `HTTP 200 audio/x-wav`。

GREEN: 测试服真实浏览器复核 -> PASS, Playwright 登录 `http://172.30.30.58:8081/showroom/product` 后 `loadFailedCount=0`、`visibleCoverImages=20`、`badImages=[]`、`failedRequests=[]`。

GREEN: follow-up cleanup preview/apply -> PASS, `verification-report.md` 已加入 Cleanup Keep，delete `<none>`，blocked `<none>`，warnings `<none>`。

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "showroom_file_config_rebind_sql_keeps_mysql_delimiter_literal" -q` -> FAIL, `New-ShowroomFileStoragePostImportSql` 在双引号 here-string 中直接写 `DELIMITER $$`，生成的 `post-import.sql` 变成空 delimiter，测试服 MySQL 执行失败。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 40 passed。

GREEN: PowerShell parse check for `script\deploy\publish-int-ruoyi.ps1` -> PASS, `POWERSHELL_PARSE_OK`。

GREEN: `mvn --% -pl yudao-server -Dtest=PublishIntRuoyiScriptTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 1 test passed。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi.ps1 -Mode deploy-release -Environment test -ReleaseTag "26-06-01 22:49:44" -NasConfigPath ...\nas-release-config\9a11bc63-3fa2-41ff-83c2-56e55c70688d.json -NasServer 172.30.30.4 -NasShare "IT共享" -NasReleaseRoot Backup/ReleasePackage` -> PASS, `Publish completed for test`；发布脚本验证 backend health 200、frontend 200、OnlyOffice 200、Website root/showroom 200、PDF worker `application/javascript`、展厅真实图片代理读取 `image/png`、scoped current release verified。

GREEN: `npx --yes --package @playwright/cli playwright-cli open http://172.30.30.58:8083/showroom` + `snapshot` + `console warning` -> PASS, 页面标题 `瑛泰展厅`，快照出现公司入口 `瑛泰 / 点击进入公司详情`，console warnings/errors 为 0。
