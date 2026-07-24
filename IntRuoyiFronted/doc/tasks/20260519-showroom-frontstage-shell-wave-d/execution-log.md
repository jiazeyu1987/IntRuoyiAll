# Execution Log: Showroom Frontstage Shell Wave D Reviewer Supervision

BDD: Wave D integration convergence -> Given shared components, device shells, and canonical routes are all accepted / When the frontstage integration wave runs / Then real homepage entry, device-route rendering, and frontstage browsing must be verified through actual user paths or fail explicitly with concrete blockers.

RED: Wave D worker not launched yet -> FAIL, no integration/E2E worker has been started.

GREEN: `spawn_agent worker 019e3ecc-49aa-7f22-99d4-234b44cdc932` -> PASS, launched the sole integration/E2E worker with a narrow evidence-only write boundary.

GREEN: `node --test scripts/showroom-frontstage-integration.test.mjs` -> PASS, route convergence checks passed.

GREEN: `pnpm exec eslint scripts/showroom-frontstage-integration.test.mjs scripts/showroom-frontstage-integration.e2e.mjs` -> PASS.

REVIEW: Wave D -> REJECTED, real browser paths are reachable but live `/showroom/display/home` still lacks published `previewImageUrl` values, so the showroom image walls can only show `未发布预览图`.

GREEN: temporary local preview asset approval -> PASS, user explicitly approved reuse of current generated screenshots for local verification only.

GREEN: backend/local data remediation -> PASS, temporary screenshot assets were uploaded and published as live hall preview assets; `/showroom/display/home` now returns non-empty `previewImageUrl` values for all `8` hall entries.

GREEN: `node --test scripts/showroom-frontstage-route-convergence.test.mjs scripts/showroom-frontstage.test.mjs` -> PASS after preview-asset remediation.

GREEN: `playwright-cli -s=showroom-frontstage-integration run-code --filename scripts/showroom-frontstage-integration.e2e.mjs` -> PASS after preview-asset remediation, returned `blockers=[]` and verified `screen / pad / mobile` canonical home routes.

GREEN: local backend/runtime remediation -> PASS, the running `48081` process was refreshed to the rebuilt current-source jar, and the local `showroom_narration_version` schema was aligned by adding the missing `voice` column.

GREEN: local frontend asset proxy -> PASS, Vite local dev now proxies `/admin-api` file-resource paths so preview image URLs render through `8081` instead of falling back to the SPA shell.

ACCEPT: Wave D -> PASS for the local-verification gate after preview-asset data, backend runtime, local runtime schema, and local image-delivery path were all corrected.
