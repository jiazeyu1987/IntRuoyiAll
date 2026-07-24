# Execution Log: 展厅批量 AI 请求超时修复

BDD: 批量封面请求必须覆盖默认 30 秒超时 -> Given 批量封面后端处理可能超过 30 秒 / When 前端调用 `POST /showroom/product/batch-generate-cover-image` / Then 请求必须显式使用 `SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT`，而不是落回 axios 默认 `30000ms`。

BDD: 批量语音请求必须与其他 AI 请求共享长超时配置 -> Given 批量语音同样属于长耗时 AI 任务 / When 前端调用 `POST /showroom/product/batch-generate-narration-audio` / Then 请求也必须显式使用 `SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT`。

RED: `node --test scripts/showroom-admin-ai-request-timeout.test.mjs` -> FAIL，`batchGenerateProductNarrationAudio` 与 `batchGenerateProductCoverImage` 两个 API 块都没有 `timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT`，仍会落回默认 30 秒超时。

GREEN: `node --test scripts/showroom-admin-ai-request-timeout.test.mjs` -> PASS，批量语音与批量封面请求已显式复用 `SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT`。

GREEN: `pnpm exec eslint src/api/showroom-admin/index.ts scripts/showroom-admin-ai-request-timeout.test.mjs --format stylish` -> PASS。
