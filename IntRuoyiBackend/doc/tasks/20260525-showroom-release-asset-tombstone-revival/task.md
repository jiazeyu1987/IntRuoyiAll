# 任务：修复展厅 release 复用资产仍返回 410

## 任务目标

- 修复 IntRuoyi 展厅 release 中已被 purge 的资产在后续 release 复用同一 `assetId + contentHash` 后，公开资产接口仍因旧 tombstone 返回 `410 Gone` 的缺陷。
- 保持 release 资产 fail-fast 语义：真实缺失、hash 不匹配或已 purge 且未被新 release 复用的资产仍必须报错。
- 修复后重新发布并验证测试服 Website 不再因 `SHOWROOM_RELEASE_ASSET_UNAVAILABLE` 冷启动失败。

## 前置任务检查

- 最近同仓任务：`20260525-full-test-publish-intruoyi-website`。
- 上一任务状态：`blocked`。
- 阻塞原因：测试服 current release manifest 引用大量音频资产，但公开资产接口返回 `410 Gone`，全量发布验收无法继续。

## BDD 场景

- BDD: 复用已 purge 资产必须撤销资产 tombstone -> Given release A 的资产因超出保留窗口被 purge 并写入 tombstone, When release B 再次引用相同 `assetId + contentHash` 且发布成功, Then `/showroom/assets/{assetId}/{contentHash}` 必须返回该资产内容而不是 `410 Gone`。
- BDD: 未复用的 purge 资产仍保持 Gone -> Given 某资产只属于已 purge release, When 前台请求该旧资产, Then 资产接口继续返回 `410 Gone`，不得从其他资源静默补齐。

## 里程碑

- [x] M1：复现测试服 current release 资产 410，并建立任务记录。
- [x] M2：补 RED 回归测试覆盖被 purge 后复用的资产仍返回 410。
- [x] M3：最小实现发布 upsert 资产时撤销对应 asset tombstone。
- [x] M4：运行后端定向测试与相关 release 回归测试。
- [x] M5：重新发布测试服并验证 Website `/` 与 `/showroom`。
- [x] M6：记录证据、closeout 预览，并按策略提交本任务变更。

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePurgeServiceTest test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomReleaseAssetApiTest,ShowroomReleaseManifestApiTest,ShowroomReleaseManifestQueryServiceTest" test`
- `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default`
- `http://172.30.30.58:8083/`
- `http://172.30.30.58:8083/showroom`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-release-asset-tombstone-revival/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260525-showroom-release-asset-tombstone-revival/backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-release-asset-tombstone-revival --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已复现测试服 current release `20260525T050130Z-9db881bf232f` 中 `product-10-audio-zh@fb4abbe683df5827c363be1477b9e66421944310f0910b115f263aaee838e315` 返回 `410 Gone`。
  - 已抽样确认 manifest 中 332 条音频资产有 276 条公开资产接口返回 `410`，图片资产抽样返回 `200`。
  - 已补充 `ShowroomReleasePurgeServiceTest.shouldClearAssetTombstoneWhenPurgedAssetIsReusedByNewRelease`，RED 复现复用资产仍被旧 tombstone 判 Gone。
  - 已在 release registry upsert 资产时清理同一 `assetId:contentHash` 的 asset tombstone；未新增 fallback，未改变真实缺失资产的 fail-fast 语义。
  - 已运行定向测试与 release 相关回归测试，均通过。
  - 已执行测试服全量发布，并在修复后的后端上使用测试租户真实账号触发一次展厅 release 发布。
  - 测试服 current release 已切换为 `20260525T061337Z-e03a7b68bf1a`，manifest `506` 个资产 HEAD 全量检查失败数为 `0`，原报错资产返回 `200`。
  - Playwright 已通过管理前端 `http://172.30.30.58:8081/showroom/company` 点击“手动发布展厅”，并在 Website `http://172.30.30.58:8083/showroom` 完成 current release 与资产访问验证。
- 阻塞与影响：
  - 暂无阻塞。

## 最终验证结果

- PASS: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePurgeServiceTest test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomReleaseAssetApiTest,ShowroomReleaseManifestApiTest,ShowroomReleaseManifestQueryServiceTest" test`
- PASS: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> tag `20260525_135729`
- PASS: Playwright 测试租户真实前端路径点击“手动发布展厅” -> release `20260525T061337Z-e03a7b68bf1a`
- PASS: `http://172.30.30.58:8083/showroom/release/current` -> current release `20260525T061337Z-e03a7b68bf1a`
- PASS: current manifest 资产 HEAD 全量扫描 -> `assetCount=506`，`failures=[]`
- PASS: `product-10-audio-zh@fb4abbe683df5827c363be1477b9e66421944310f0910b115f263aaee838e315` -> HTTP `200`
- PASS: `script\deploy\show-int-ruoyi-test-status.bat` -> 后端健康检查 HTTP 200，管理前端 HTTP 200。

## Cleanup Keep

- `doc/tasks/20260525-showroom-release-asset-tombstone-revival/bug-regression-evidence.md`
- `doc/tasks/20260525-showroom-release-asset-tombstone-revival/backend-api-evidence.md`
