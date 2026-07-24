# Task: AI big-model route sweep and local Codex CLI LLM backend

## Goal

Support the AI big-model top-level menu route sweep by enabling the AI backend module and making LLM-backed backend behavior use the local Codex CLI as the single LLM execution path required by the user.

## Scope

- Inspect existing AI module enablement, model factory, chat/write/mindmap services, and model-provider configuration.
- Enable the AI backend module where the visible AI routes require `/admin-api/ai/**` endpoints.
- Add or adjust backend behavior only where a real AI user path requires LLM execution.
- Use local Codex CLI as the LLM command integration point.
- Fail fast if the local `codex` executable is missing, exits non-zero, or returns no usable text.
- Do not add fallback to external providers, mock success, or swallowed exceptions.

## Milestones

- [x] M1: Previous unfinished backend task checked and blocked before new work.
- [x] M2: Task documentation created before backend discovery, tests, or production code changes.
- [x] M3: Backend AI LLM call sites and contracts identified.
- [x] M4: RED backend verification added and observed failing for current non-Codex behavior.
- [x] M5: Local Codex CLI LLM integration implemented with fail-fast behavior.
- [x] M6: Targeted backend tests and route audit verification completed.
- [x] M7: Evidence updated and backend task changes committed separately.

## Expected Verification

- Backend targeted tests prove LLM text generation invokes the configured local Codex CLI command.
- Missing `codex`, non-zero exit, timeout, or blank output produces an explicit failure instead of fallback.
- AI route sweep network requests do not report disabled AI module, missing route, or non-Codex provider behavior.

## Current Status

Completed. The AI module is enabled in the reactor and server jar, MySQL now has the required `ai_*` tables and Codex CLI seed data, and the real frontend AI write path now returns generated content through the local Codex CLI path.
