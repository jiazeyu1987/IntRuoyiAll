# Execution Log: AI big-model route sweep and Codex CLI frontend alignment

BDD: AI big-model child routes are reachable -> Given an authenticated admin opens the AI big-model menu, When each visible child route is opened through the real frontend, Then the page renders without unhandled frontend errors or missing route failures.

BDD: AI LLM calls use local Codex CLI contract -> Given an AI big-model child page triggers LLM-backed behavior, When the frontend calls the backend API, Then it uses the backend contract wired to the local Codex CLI and does not select another provider or fallback silently.

## Evidence

- M1: Completed. Previous frontend task `20260512-infra-route-audit` was already blocked before starting this task.
- M2: Completed. This task document and execution log were created before route discovery, tests, or production code changes.
- M3: Completed. Visible AI big-model routes were inventoried from the real permission menu: 7 direct AI feature routes plus 9 console routes, 16 total.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session ai-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260512-ai-model-route-codex-cli\scripts\ai-route-sweep.js` -> FAIL, 15/16 visible AI routes fail.
- RED evidence: Most failing routes return `code=501` with `[AI 大模型 yudao-module-ai - 已禁用]` from `/admin-api/ai/**`.
- RED evidence: `AI 思维导图` reproduces browser console errors with SVG transform `translate(NaN,NaN) scale(...)`.
- RED evidence: `AI 音乐` reproduces a failed local asset request for `/src/assets/audio/response.mp3`.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session ai-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260512-ai-model-route-codex-cli\scripts\ai-route-sweep.js` -> PASS, 16/16 visible AI routes pass.
- GREEN evidence: `AI 音乐` no longer requests a missing local audio asset on first load.
- GREEN evidence: `AI 思维导图` no longer emits `translate(NaN,NaN)` console errors during the route sweep.
- GREEN: Playwright real-user path on `/ai/write` returned generated content in the preview textarea after clicking `生成`.
