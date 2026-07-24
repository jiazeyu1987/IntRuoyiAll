# 执行日志：修复展厅 release 复用资产仍返回 410

BDD: 复用已 purge 资产必须撤销资产 tombstone -> Given release A 的资产因超出保留窗口被 purge 并写入 tombstone, When release B 再次引用相同 `assetId + contentHash` 且发布成功, Then `/showroom/assets/{assetId}/{contentHash}` 必须返回该资产内容而不是 `410 Gone`。

BDD: 未复用的 purge 资产仍保持 Gone -> Given 某资产只属于已 purge release, When 前台请求该旧资产, Then 资产接口继续返回 `410 Gone`，不得从其他资源静默补齐。

INFO: 已采用 `bug-regression-fix-loop` 与 `backend-api-delivery` 工作流。
INFO: 上一同仓任务 `20260525-full-test-publish-intruoyi-website` 已记录为 blocked，阻塞原因是测试服 release 资产 410 导致 Website 冷启动失败。
EVIDENCE: `GET http://172.30.30.58:8083/showroom/release/current` -> PASS, current release 为 `20260525T050130Z-9db881bf232f`。
EVIDENCE: `GET http://172.30.30.58:8083/showroom/release/20260525T050130Z-9db881bf232f/manifest` -> PASS, manifest 包含 `product-10-audio-zh@fb4abbe683df5827c363be1477b9e66421944310f0910b115f263aaee838e315`。
EVIDENCE: `GET http://172.30.30.58:8083/showroom/assets/product-10-audio-zh/fb4abbe683df5827c363be1477b9e66421944310f0910b115f263aaee838e315` -> FAIL, HTTP 410。
EVIDENCE: 测试服 current manifest 音频资产抽样/HEAD 检查 -> 332 条音频中 `200=56`，`410=276`；图片资产抽样返回 `200`。
RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePurgeServiceTest test` -> FAIL, expected reason: `shouldClearAssetTombstoneWhenPurgedAssetIsReusedByNewRelease` 断言复用资产后 asset tombstone 应被清理，但实际 tombstone 仍存在，公开资产查询仍会按 Gone 处理。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePurgeServiceTest test` -> PASS, 2 tests。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomReleaseAssetApiTest,ShowroomReleaseManifestApiTest,ShowroomReleaseManifestQueryServiceTest" test` -> PASS, 6 tests。

EVIDENCE: 实现变更 -> `ShowroomReleaseRegistryService.upsertAsset` 在插入、复活或确认存在同一 `assetId + contentHash` 资产行后，删除 `showroom_release_tombstone` 中对应 `ASSET` tombstone；`ShowroomReleaseTombstoneMapper` 增加按 `resourceType + resourceKey` 删除方法。

EVIDENCE: 部署前复核 -> 测试服 release `20260525T051002Z-9db881bf232f` 仍包含原目标资产，音频资产 HEAD 检查 `200=56`、`410=276`，证明仅部署代码不会静默改写历史 tombstone，必须重新发布 release。

RED: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> FAIL, expected reason: 发布脚本内 `Start-Process` 捕获 `pnpm exec vite build --mode test` 输出时，管理前端 Vite 构建进程以 exit code `-1` 失败；同一环境变量下直接执行 Vite 构建可通过。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected reason: 新增 `test_publish_script_streams_local_frontend_build` 要求测试服发布脚本直接执行本地 Vite 构建，当前仍通过 `Invoke-CheckedCommand` 捕获 `pnpm`。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 24 passed；测试服发布脚本改为在保留 `NODE_OPTIONS=--max-old-space-size=8192` 与 `VITE_*` 环境变量的前提下，用 `Invoke-CheckedShell` 直接执行 `pnpm exec vite build --mode test`。

GREEN: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> PASS, tag `20260525_135729`，后端、管理前端、Website、MySQL、MinIO 已发布到测试服。

EVIDENCE: 测试服后端 readiness -> SSH 内网 `http://127.0.0.1:48081/actuator/health` 返回 `200`；Website `/showroom/release/current` 返回 `200`。

GREEN: Playwright 测试租户真实前端发布 -> `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-release-publish run-code --filename doc/tasks/20260525-showroom-release-asset-tombstone-revival/verify-showroom-release-publish-e2e.mjs` -> PASS, 点击管理前端 `手动发布展厅`，release `20260525T061337Z-e03a7b68bf1a`, assetCount `506`, documentCount `166`, failures `[]`。

GREEN: 测试服资产验证 -> `GET http://172.30.30.58:8083/showroom/release/current` -> current release `20260525T061337Z-e03a7b68bf1a`; current manifest 全部 `506` 个资产 HEAD 检查 failures `[]`；原报错资产 `product-10-audio-zh@fb4abbe683df5827c363be1477b9e66421944310f0910b115f263aaee838e315` -> HTTP `200`。

GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, 测试服后端健康检查 HTTP 200，管理前端 HTTP 200，后端/前端/Website 容器均运行 tag `20260525_135729`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-release-asset-tombstone-revival/bug-regression-evidence.md` -> PASS, Bug regression evidence is valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260525-showroom-release-asset-tombstone-revival/backend-api-evidence.md` -> PASS, Backend API evidence is valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-release-asset-tombstone-revival --mode preview` -> PASS, keep `task.md`、`execution-log.md`、`bug-regression-evidence.md`、`backend-api-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
