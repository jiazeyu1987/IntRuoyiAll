# 执行日志：展厅临时预览图资产本地验证

BDD: 临时预览图资产发布 -> Given 用户已明确批准本地验证临时方案，当可用截图被上传并发布为 preview 资产时，Then `/showroom/display/home` 应返回非空 `previewImageUrl`，以证明前台图片墙可读到真实发布资产。

RED: blocker diagnosed -> FAIL, 当前运行库的 `showroom_preview_asset_version` 与 `image/*` 文件均为空，前台首页只能显示“未发布预览图”。

GREEN: `POST /admin-api/infra/file/upload` with tenant `122` and test user `aoteman` -> PASS, temporary screenshot uploaded and stored as `infra_file.id = 2272`

GREEN: `INSERT INTO showroom_preview_asset_version ... target_type='HALL' ... image_file_id=2272 ... status='PUBLISHED'` -> PASS, `8` live hall preview rows created for local verification

FINDING: stale backend runtime jar -> FAIL, the running `48081` jar still used an older `ShowroomApiRuntime` that hardcoded empty hall preview URLs and private `/infra/file/get?id=...` file links

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt the backend from current source

GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after updating `ShowroomApiRuntime.fileUrl(...)` to emit public permitAll file paths and aligning the integration test

GREEN: runtime DB hotfix -> PASS, added missing `voice` column to local `showroom_narration_version` so the accepted frontstage shells no longer fail on live narration reads

GREEN: authenticated `GET http://127.0.0.1:48081/showroom/display/home` -> PASS, `8` hall entries now return non-empty `previewImageUrl` values like `/admin-api/infra/file/28/get/showroom/preview/temp/20260519/02-screen-default-entry.png`

GREEN: local runtime refresh -> PASS, `48081` now runs the rebuilt current-source jar and no longer hardcodes empty hall preview URLs or private `get?id=` links.
