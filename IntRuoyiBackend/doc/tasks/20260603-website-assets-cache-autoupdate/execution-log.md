# 执行日志

BDD: hashed assets 可长期缓存且自动更新 -> Given Website 入口 HTML 每次重新读取 / When 入口引用新的 hash 资源文件名 / Then 浏览器可长期缓存旧 hash 资源，同时新发布通过新的 hash URL 自动更新。

BDD: 发布后必须验证 assets 缓存头 -> Given `publish-int-ruoyi.ps1 -Mode deploy-release -Component website` 已重建 Website / When 脚本读回入口 JS bundle / Then bundle 响应必须包含 `Cache-Control: public, max-age=31536000, immutable`，否则发布失败。

INFO: 上个任务 `20260603-website-remote-entry-readback-gate` 已完成并提交 `59e4f895ae`；本任务只补齐 hashed assets 缓存策略，不回退 HTML 入口 no-store。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "entry_bundle" -q` -> FAIL，发布脚本未包含 `public, max-age=31536000, immutable` bundle 缓存头读回合同。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "hashed_assets" -q` -> FAIL，`website.nginx.conf` 缺少 `location /assets/`。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "hashed_assets" -q` -> PASS，1 passed, 45 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "asset_cache or entry_bundle" -q` -> PASS，1 passed, 45 deselected。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，46 passed。

GREEN: `git diff --check -- script\deploy\int-ruoyi-test\website.nginx.conf script\deploy\publish-int-ruoyi.ps1 script\tests\test_publish_int_ruoyi_to_test_tooling.py doc\tasks\20260603-website-assets-cache-autoupdate` -> PASS。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode build-release -Component website -ReleaseTag 20260603_website_assets_cache_immutable ...` -> PASS，发布包上传至 `Backup/ReleasePackage/20260603_website_assets_cache_immutable`。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment test -ReleaseTag 20260603_website_assets_cache_immutable ...` -> PASS，测试服 read-back gate 通过。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode mark-tested -Component website -ReleaseTag 20260603_website_assets_cache_immutable ...` -> PASS。

GREEN: `powershell.exe ... publish-int-ruoyi.ps1 -Mode deploy-release -Component website -Environment prod -ConfirmText PROD -RequireTested -ReleaseTag 20260603_website_assets_cache_immutable ...` -> PASS，正式服 read-back gate 通过。

GREEN: 正式服独立读回 `/` -> HTTP 200，`Cache-Control=no-store, no-cache, must-revalidate, max-age=0`，入口 JS `/assets/index-B1lPB_BO.js`，CSS `/assets/index-DbLfBTKE.css`。

GREEN: 正式服独立读回 `/assets/index-B1lPB_BO.js` -> HTTP 200，`Cache-Control=public, max-age=31536000, immutable`，`Contains3GB=True`，`Contains1GB=False`。

GREEN: 正式服独立读回 `/assets/index-DbLfBTKE.css` -> HTTP 200，`Cache-Control=public, max-age=31536000, immutable`。

GREEN: 正式服独立读回 `/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> HTTP 200，releaseId `20260602T065841Z-be276b74dfa8-ca5704904844`。

GREEN: `task-closeout-cleanup --mode preview/apply` -> PASS，keep `task.md` 与 `execution-log.md`，delete `<none>`。
