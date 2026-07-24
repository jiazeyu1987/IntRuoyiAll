# Execution Log: AI big-model route sweep and local Codex CLI LLM backend

BDD: LLM-backed AI behavior uses local Codex CLI -> Given an authenticated admin triggers an AI model feature that needs text generation, When the backend executes the LLM request, Then the request is executed by the local Codex CLI and no external provider fallback is used.

BDD: Codex CLI failures fail fast -> Given the local Codex CLI command is unavailable, exits non-zero, times out, or returns blank output, When the backend executes the LLM request, Then the API reports the exact failure and does not return mock or default-success content.

BDD: AI backend routes are enabled -> Given AI big-model menus are visible in the frontend, When the frontend calls `/admin-api/ai/**`, Then the request reaches the AI module controller instead of the default disabled-module handler.

## Evidence

- M1: Completed. Previous backend task `20260512-bpm-route-sweep` was already blocked before starting this task.
- M2: Completed. This task document and execution log were created before backend discovery, tests, or production code changes.
- M3: Completed. LLM-backed backend entry points were confirmed in `AiChatMessageServiceImpl`, `AiWriteServiceImpl`, `AiMindMapServiceImpl`, and Tinyflow workflow integration via `AiModelServiceImpl#getLLmProvider4Tinyflow`.
- RED precondition note: `mvn -pl yudao-server -Dtest=AiModuleEnablementTest test` failed too early because `-am` was missing and local reactor dependencies were not built, so it could not prove the target AI behavior.
- RED: `mvn -pl yudao-server -am -Dtest=AiModuleEnablementTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL.
- RED evidence: `root pom.xml must include yudao-module-ai because AI big-model menus are enabled` -> failed.
- RED evidence: `yudao-server/pom.xml must depend on cn.iocoder.boot:yudao-module-ai because /admin-api/ai/** routes are enabled` -> failed.
- RED evidence: `ClassNotFoundException: cn.iocoder.yudao.module.ai.controller.admin.chat.AiChatMessageController` -> failed.
- GREEN: `node doc\tasks\20260512-ai-model-route-codex-cli\scripts\generate-ai-base-schema.cjs` -> PASS, generated `sql\mysql\20260512_ai_base_schema.sql` with 14 AI tables.
- GREEN: imported `sql\mysql\20260512_ai_base_schema.sql` and `sql\mysql\20260512_ai_codex_seed.sql` into local MySQL -> PASS, 14 `ai_*` tables present and `CodexCli` seed rows available in `ai_api_key` and `ai_model`.
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS after enabling `yudao-module-ai`.
- GREEN: `mvn -pl yudao-server -am -Dtest=AiModuleEnablementTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS.
- GREEN evidence: the real frontend `/ai/write` user path now reaches `/admin-api/ai/write/generate-stream` with HTTP 200 and stores generated content in `ai_write`.
