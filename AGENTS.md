# IntRuoyi Agent Instructions

Scope: This file governs work in the current `E:\IntRuoyi` workspace unless a nearer `AGENTS.md` overrides the same subject. This is a Windows workspace; use PowerShell-safe commands and explicit UTF-8 handling.

## Project Identity

- Workspace root: `E:\IntRuoyi`.
- Backend root: `E:\IntRuoyi\IntRuoyiBackend`.
- Frontend root: `E:\IntRuoyi\IntRuoyiFronted`.
- Coordination docs live under root `doc\` and `docs\`; backend and frontend also contain their own `doc\` and `docs\` folders.
- Do not reuse paths or folder names from prior project instructions unless the user explicitly confirms they are relevant to the current task.
- The root, backend, and frontend folders may be non-Git directories in this workspace. Check Git state before assuming commits, branches, or worktrees are available.

## Technology Baseline

- Backend: Java 17, Maven multi-module Spring Boot project under `IntRuoyiBackend`; main app module is `yudao-server`; business modules include `yudao-module-system`, `infra`, `bpm`, `crm`, `erp`, `dcc`, `mdm`, `mes`, `srm`, `showroom`, and `ai`.
- Frontend: Vue 3, Vite, TypeScript, Element Plus, Pinia, UnoCSS, pnpm under `IntRuoyiFronted`.
- Test evidence exists in both repos: Java/JUnit tests, Node static contract tests, and Playwright real-flow E2E tests.
- Use `docs\engineering\technology-stack-routing.md` as the current stack routing evidence when choosing implementation and verification paths.

## Communication Policy

- Keep user-facing updates concise and necessary: blocking questions, blockers, completed work, verification results, or information explicitly requested by the user.
- When ambiguity affects safety, data, scope, release, or irreversible changes, stop and ask a concise action-oriented question before proceeding.

## Rule Precedence and Ownership

- The nearest applicable `AGENTS.md` governs conflicting rules for files under its scope; compatible parent rules remain active.
- Current task ownership applies only to files, processes, worktrees, logs, and temporary artifacts created or modified for this task.
- Do not modify unrelated concurrent task artifacts. If another task conflicts on the same file, branch, runtime port, database, or deployment target, stop and report the conflict.

## Strict No-Fallback Policy

- Do not introduce fallback, graceful degradation, compatibility shims, mock success, placeholder success, default-success values, or silent downgrade unless the user explicitly requests that exact fallback.
- Missing prerequisites must fail fast with the exact missing input, service, schema, environment, credential, data, or document, plus the impact.
- Do not swallow exceptions or hide backend/API/frontend errors. Surface failures in code, logs, tests, or UI as appropriate.
- If the user explicitly asks for a fallback, implement the smallest scoped fallback and document trigger conditions, risks, and removal or rollback strategy.

## Task Documentation

- Before changing files, running builds/tests/releases, modifying environments, or touching data, create or identify `doc\tasks\<task-id>\`.
- The task document must include task goal, milestones, expected verification, current status, and a fixed `设计约束检查` section:
  - `是否引入 fallback/降级/吞异常`：是/否；如是，记录用户明确要求、触发条件、风险和移除/回滚策略。
  - `是否从根因和长期维护角度解决`：是/否；如否，先阻塞并说明缺少的正式方案前置条件。
  - `是否存在临时补丁或绕过`：是/否；如是，记录用户批准范围、风险和后续处理。
- Create or update `doc\tasks\<task-id>\execution-log.md` with user intent, command intent, milestone updates, verification evidence, and blockers.
- If `docs\experience-index.md` exists, read it after creating the task directory, open only matching experience documents, and copy the applicable gate summary into the task document.
- If `docs\experience-index.md` is missing, record that fact in the task document. High-risk work is blocked until the missing experience gate is created or the user explicitly authorizes proceeding with documented risk.
- After each milestone, update task status, completed work, verification evidence, and remaining blockers.
- When implementation and required verification are complete but cleanup, merge, or worktree removal remains, set `## Current Status` to `ready_for_closeout`.
- When the task is finished, set status to `completed` and record the final verification result.

## BDD and Strict TDD

- Feature work, bug fixes, refactors, and behavior changes require BDD and strict TDD by default.
- Write observable `Given / When / Then` scenarios before implementation and record them in `execution-log.md` using `BDD: <scenario name> -> Given/When/Then`.
- Run or create a failing test first and record `RED: <command> -> FAIL, <expected reason>`.
- Implement the smallest formal solution, then record `GREEN: <command> -> PASS`.
- Run the relevant regression verification before completion.
- Documentation-only changes still require structural verification, but do not require production-code tests unless they change executable behavior.

## Backend Work Rules

- Work under `IntRuoyiBackend` for Java/Spring changes.
- Follow existing module boundaries; do not move logic across modules without a documented design reason.
- Before writing SQL, migrations, menu permissions, tenant bindings, or schema-dependent code, verify current real schema with `SHOW TABLES`, `DESCRIBE`, existing migration files, or current mapper XML/contracts. Do not infer schema from DO class names alone.
- Prefer targeted Maven verification for the touched module, for example:
  - `mvn -pl yudao-module-mes -am test`
  - `mvn -pl yudao-server -am test`
- If Maven dependencies, Java runtime, database, Redis, or required test data are missing, fail fast and record the blocker.

## Frontend Work Rules

- Work under `IntRuoyiFronted` for the active Vue3 admin frontend.
- Use pnpm for frontend commands; do not switch package managers.
- Preserve existing Vue3/Vite/Element Plus patterns, route conventions, API wrappers, permission handling, and table/form styles.
- Frontend failures must be visible through UI feedback, console/network evidence, or test assertions. Do not use empty `catch {}` blocks or silent toasts to hide backend errors.
- Prefer targeted verification for touched areas:
  - `pnpm ts:check`
  - `pnpm build:local`
  - Existing `pnpm e2e:*` scripts that match the changed module.
- Do not add frontend controls only to make tests pass unless the user-approved scope includes that product change.

## E2E, Data, and Tenant Safety

- E2E must use Playwright through real frontend user paths. APIs may be used only for final verification or read-only supporting checks.
- Default local frontend entry is `http://127.0.0.1:8081` or `http://localhost:8081` only after confirming the service is running.
- Default local backend API entry is `http://127.0.0.1:48081` only after confirming the service is running.
- Write-type E2E must use a confirmed test tenant/account and create traceable, task-owned test data. Do not modify production tenant data, admin baseline data, or unrelated real business records.
- If login credentials, tenant baseline, menu permissions, runtime ports, database, Redis, or sample data are missing, fail fast. Do not replace real E2E with mocks, backup data, direct SQL shortcuts, or API-only paths.
- If a frontend entry, menu route, role binding, or dynamic route is missing, distinguish between product scope and environment/setup scope before changing code.

## Server, Release, Backup, and Worktree Safety

- Do not operate remote servers, production services, backups, restores, releases, rollbacks, or shared storage unless the user explicitly authorizes that task.
- If server, login, release, backup, restore, or worktree instructions are required but missing from current `docs\`, stop and report the missing document and impact.
- Do not delete, unmount, clear, or rewrite shared storage or deployment artifacts unless the task scope explicitly authorizes it and verification/rollback steps are documented.
- Release/build isolation must be explicit. If Git/worktree support is absent in this workspace, do not fabricate a branch, commit, or release workflow.

## Git and Commit Policy

- Check Git status in the exact repository that owns the files before editing or committing.
- If the owning directory is not a Git repository, record that no commit can be made and continue only if the task can be completed without Git integration.
- Never revert user changes or unrelated task changes unless the user explicitly asks.
- Commit only task-owned implementation changes after required verification passes and before destructive closeout or integration when a commit is possible and required.
- Commit final task/closeout records separately after closeout evidence is complete.
- If verification fails or prerequisites are missing, do not commit; report the blocker and impact.

## PowerShell and Encoding Safety

- PowerShell commands must not use `&&`; use separate lines or `;` when sequencing is necessary.
- Chinese text in Markdown, source files, SQL, JSON, CSV, logs, and generated docs must be UTF-8.
- On Windows PowerShell, do not rely on default `Get-Content`, `Set-Content`, `Add-Content`, `Out-File`, `>` or `>>` for Chinese text.
- Read Chinese text with `Get-Content -Encoding utf8`, `python -X utf8`, Node UTF-8 APIs, or `rg`.
- Write Chinese text with `apply_patch` or explicit UTF-8 APIs. Preserve existing confirmed encodings; if encoding is unknown or garbled, fail fast instead of silently rewriting.
- Before commands that pass Chinese parameters, Chinese regex, Chinese SQL, SSH/MySQL stdin, or multiline inline scripts, design the encoding path and verify exit codes.

## Refactor Policy

- Refactors must be justified from long-term maintainability and extensibility before code changes.
- Use BDD/TDD and task documentation for refactors.
- Prefer removing implicit fallback branches unless an explicit requirement, compliance rule, or SLO policy requires keeping them.

## Closeout Policy

- Before final summary, update task documents with milestone completion, verification evidence, blockers, and final status.
- Cleanup only current task-owned temporary artifacts. Do not remove unrelated outputs, logs, worktrees, or running processes.
- If cleanup, merge, worktree removal, or verification cannot be completed, mark the task blocked or `ready_for_closeout` with the exact reason.
- Preserve `task.md`, `execution-log.md`, and `verification-report.md` by default.
