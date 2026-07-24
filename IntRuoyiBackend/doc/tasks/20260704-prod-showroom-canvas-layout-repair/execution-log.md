# 执行日志

BDD: prod showroom canvas layout repair -> Given production manual release fails because target scope hall item mappings lack canvas layout, When scoped layout data is generated and written only for the bound tenant, Then release assembly no longer fails on missing hall canvas layout.

GREEN: experience-preflight -> PASS, 已读取并应用 PowerShell、服务器访问、发布备份恢复、登录门禁；本任务允许在正式服执行受控 SQL 写入和发布验证，范围限 `yingtai-showroom/TEST` 绑定租户的展柜布局字段。
GREEN: prod-layout-sql-preview -> PASS, corrected default-layout plan matched code semantics: each hall grid row fills its own remaining width, coverage ~= 1.000000, target rows before update = 149.

GREEN: prod-layout-sql-apply -> PASS, scoped update executed only for `yingtai-showroom/TEST` bound tenant (`tenant_id=1`) and only rows with missing layout fields.
RED: prod-manual-publish-after-layout-v1 -> FAIL, request reached `/showroom/release/publish` but backend blocked on `SHOWROOM_CANVAS_LAYOUT_INVALID: hall canvas rectangles must cover full area`; v1 SQL used direct rounded `1 / row_count`, causing area drift at strict EPSILON boundary.

GREEN: prod-layout-sql-apply-v2 -> PASS, layout values recalculated using Java-equivalent `ratio(next) - ratio(current)` width/height semantics for the full `yingtai-showroom/TEST` tenant scope.
RED: prod-manual-publish-after-layout-v2 -> FAIL, request reached `/showroom/release/publish` and no longer failed on canvas layout; next blocker was `SHOWROOM_TARGET_NOT_FOUND: live hall ZH narration not found`.
RED: prod-batch-generate-hall-narration -> FAIL, production `/showroom/hall/batch-generate-narration-audio` matched 10 halls but succeeded 0 and failed 10; all failures were `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=400 ... ACCESS_DENIED:The token '****' is invalid!`.
INFO: prod-tts-config-readonly -> production saved NLS AccessToken masked as `bc44****2e69`, update_time `2026-05-27 00:15:23`; backend container has no `ALIYUN_NLS_ACCESS_TOKEN`, `ALIYUN_NLS_APPKEY`, `ALIYUN_ACCESS_KEY_ID`, or `ALIYUN_ACCESS_KEY_SECRET` runtime variables.
INFO: local-tts-config-readonly -> local saved NLS AccessToken masked as `8fbe****b985`, update_time `2026-07-04 17:03:26`; local AppKey masked as `1zFn****DJDT`, update_time `2026-06-27 00:52:40`.
GREEN: prod-tts-config-sync -> PASS, scoped production `infra_config` update changed only `yudao.ai.tts.aliyun-nls.access-token`, `yudao.ai.tts.aliyun-nls.appkey`, and `yudao.ai.tts.aliyun-nls.voice`; post-sync masked token `8fbe****b985`, appkey `1zFn****DJDT`, update_time `2026-07-04 21:11:39`.
GREEN: prod-batch-generate-hall-narration-after-tts-sync -> PASS, production `/showroom/hall/batch-generate-narration-audio` matched 10 halls, succeeded 10, failed 0.
RED: prod-manual-publish-after-hall-narration -> FAIL, request reached `/showroom/release/publish`; next blocker was `SHOWROOM_TARGET_NOT_FOUND: live preview asset is required for HALL:23`.
GREEN: prod-hall-preview-asset-repair -> PASS, business API `/showroom/hall/publish-preview-asset` published preview asset for `hall_09` (`hallId=23`, version `993`) and `hall_10` (`hallId=24`, version `994`) using existing file `2272`; post-check all 10 target halls have published preview assets.
RED: prod-manual-publish-after-preview-repair -> FAIL, request reached `/showroom/release/publish`; next blocker was `SHOWROOM_SCRIPT_MISSING: award ZH narration text is required`.
INFO: prod-award-description-readonly -> `yingtai-showroom/TEST` target scope had 46 current award revisions; all 46 had published ZH/EN narration script+audio, but all 46 were missing `description_zh` and `description_en`.
GREEN: prod-award-description-repair -> PASS, scoped SQL updated only current award revisions in the `yingtai-showroom/TEST` bound tenant, backfilling missing `description_zh/description_en` from their own published bilingual narration scripts; updated 46 rows, remaining missing descriptions 0/0.
GREEN: prod-manual-publish-final-e2e -> PASS, Playwright logged into production `芋道源码/admin`, opened `/showroom/company`, clicked `手动发布展厅`, confirmed dialog, and `/admin-api/showroom/release/publish` returned `code=0`; releaseId `20260704T132351Z-be276b74dfa8-856a86f095c1`, manifestHash `536559b5a906982ded290bd9475cbfeeb39e32e56b0a36dd0a774edad250e97c`, documentCount `196`, assetCount `618`, installBytes `747887446`, pageHasSystemError `false`.
INFO: remaining-followup -> scheduled auto-publish still logs `Dirty showroom release requires configured auto-publish site key and stage`; this was not the manual publish blocker and was not changed in this task.
