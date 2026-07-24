# 执行日志

BDD: Website 远端 HTML 入口不得被旧浏览器缓存 -> Given 发布包部署到远端 `intruoyi-website` Nginx / When 用户访问 `/`、`/index.html` 或 SPA fallback 路径 / Then 响应必须包含 `Cache-Control: no-store, no-cache, must-revalidate, max-age=0`、`Pragma: no-cache`、`Expires: 0`，避免旧 Chrome 持续加载旧 runtime。

BDD: Website 部署后必须读回真实入口和 release -> Given `publish-int-ruoyi.ps1 -Mode deploy-release` 已重建远端 Website / When 脚本进入 HTTP readiness 阶段 / Then 必须从 `http://${ServerHost}:$WebsiteHostPort/` 读回 HTML、入口 JS、scope/cache marker 和 scoped release current；任一不匹配必须失败，不能报告部署成功。

INFO: 任务范围为发布链路长期修复；不使用清浏览器缓存、手动改远端文件或跳过读回作为成功条件。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "website_nginx or public_website" -q` -> FAIL, `website.nginx.conf` 缺少 `location = /` 入口 no-store 配置。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "entry_bundle" -q` -> FAIL, `publish-int-ruoyi.ps1` 缺少 `Assert-PublicWebsiteEntryReadback` 部署后入口/bundle/header 读回门禁。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "website_nginx or public_website" -q` -> PASS, 3 passed, 41 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "entry_bundle" -q` -> PASS, 1 passed, 43 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 45 passed。

INFO: 部署前只读检查 `http://172.30.30.57:8083/` -> HTTP 200，`Cache-Control/Pragma/Expires` 为空，入口 JS 为 `/assets/index-Bl4Llfeh.js`。

INFO: 部署前只读检查旧 bundle `/assets/index-Bl4Llfeh.js` -> `Contains3GB=False`、`Contains1GB=True`、`ContainsSite=True`、`ContainsStage=True`。

RED: `powershell.exe ... publish-int-ruoyi.ps1 -Mode build-release -Component website -ReleaseTag 20260603_website_entry_readback_nostore -NasConfigPath 2056437a-...json` -> FAIL, Website 构建成功但 NAS 上传到 `\\172.30.30.4\质量体系文件\Backup` 权限不足；影响：该 NAS 配置不具备发布包写入权限。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode build-release -Component website -ReleaseTag 20260603_website_entry_readback_nostore -NasConfigPath manual-backup-publish-20260601-005831.json` -> PASS，发布包上传到 `Backup/ReleasePackage/20260603_website_entry_readback_nostore`。

RED: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment test -ReleaseTag 20260603_website_entry_readback_nostore ...` -> FAIL, website-only deploy 仍触发 backend `required-sql` 包门禁。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "required_sql_package_gate" -q` -> FAIL, expected reason: `Assert-RequiredDatabaseSqlScriptsInRelease` 未被 `$publishBackend` guard 包裹。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "required_sql_package_gate" -q` -> PASS, 1 passed, 44 deselected。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment test -ReleaseTag 20260603_website_entry_readback_nostore ...` -> PASS，测试服 read-back gate 通过，入口 `/assets/index-B1lPB_BO.js`，scoped release current `20260602T045631Z-be276b74dfa8-b111cad3b49c`。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode mark-tested -Component website -ReleaseTag 20260603_website_entry_readback_nostore ...` -> PASS，发布包标记 tested。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment prod -ConfirmText PROD -RequireTested -ReleaseTag 20260603_website_entry_readback_nostore ...` -> PASS，正式服 read-back gate 通过，入口 `/assets/index-B1lPB_BO.js`，scoped release current `20260602T065841Z-be276b74dfa8-ca5704904844`。

GREEN: 正式服独立读回 `/` -> HTTP 200，`Cache-Control=no-store, no-cache, must-revalidate, max-age=0`，`Pragma=no-cache`，`Expires=0`，入口 JS `/assets/index-B1lPB_BO.js`。

GREEN: 正式服独立读回 `/index.html` -> HTTP 200，`Cache-Control=no-store, no-cache, must-revalidate, max-age=0`，`Pragma=no-cache`，`Expires=0`。

GREEN: 正式服独立读回 `/assets/index-B1lPB_BO.js` -> HTTP 200，`Contains3GB=True`、`Contains1GB=False`、`ContainsSite=True`、`ContainsStage=True`。

GREEN: 正式服独立读回 `/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> HTTP 200，releaseId `20260602T065841Z-be276b74dfa8-ca5704904844`，installBytes `651196518`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260603-website-remote-entry-readback-gate\ci-cd-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-website-remote-entry-readback-gate\bug-regression-evidence.md` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_deploy_services.py -q` -> PASS，6 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS，4 passed。

INFO: `task-closeout-cleanup` preview -> ready；keep `task.md` 与 `execution-log.md`，delete `ci-cd-evidence.md` 与 `bug-regression-evidence.md`。

GREEN: `task-closeout-cleanup` apply -> PASS，仅删除本任务 `ci-cd-evidence.md` 与 `bug-regression-evidence.md`，保留 `task.md` 与 `execution-log.md`。
