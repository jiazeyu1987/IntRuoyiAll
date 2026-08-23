# IntRuoyi Agent Instructions

Scope: This file governs work in the current `E:\IntRuoyi` workspace unless a nearer `AGENTS.md` overrides the same subject. This is a Windows workspace; use PowerShell-safe commands and explicit UTF-8 handling.

## Project Identity

- Workspace root: `E:\IntRuoyi`.
- Backend root: `E:\IntRuoyi\IntRuoyiBackend`.
- Frontend root: `E:\IntRuoyi\IntRuoyiFronted`.
- Main branch: `int_main`.
- Worktree root: `D:\IntRuoyiWorktree\`.
- Worktree restrictions: `docs\worktree-restrictions.md`.
- Branch runtime port matrix: `docs\branch-runtime-ports.md`; `int_main_d=8101/48101` at `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`, `int_main=8081/48081` at `E:\IntRuoyi`, `int_batch=8041/48041`, `int_shedule=8021/48021`, `int_qms=8061/48061`.
- Additional worktrees must use an atomically reserved profile slot in `1..50`; reserve it with `scripts\runtime\reserve-worktree-slot.ps1` before starting either service.
- Trigger-read rules live under `docs\*.md`; read the matching rule file before the triggering operation.
- Coordination docs live under root `doc\` and `docs\`; backend and frontend also contain their own `doc\` and `docs\` folders.
- Do not reuse paths or folder names from prior project instructions unless the user explicitly confirms they are relevant to the current task.
- The root, backend, and frontend folders may be non-Git directories in this workspace. Check Git state before assuming commits, branches, or worktrees are available.

## Technology Baseline

- Backend: Java 17, Maven multi-module Spring Boot project under `IntRuoyiBackend`; main app module is `yudao-server`; business modules include `yudao-module-system`, `infra`, `bpm`, `crm`, `erp`, `dcc`, `mdm`, `mes`, `srm`, `showroom`, and `ai`.
- Frontend: Vue 3, Vite, TypeScript, Element Plus, Pinia, UnoCSS, pnpm under `IntRuoyiFronted`.
- Test evidence exists in both repos: Java/JUnit tests, Node static contract tests, and Playwright real-flow E2E tests.
- Use `docs\engineering\technology-stack-routing.md` as the current stack routing evidence when choosing implementation and verification paths.

## 工艺路线三类配置术语契约

- 工艺路线中的“工序开始”“批记录表单”“表单槽位”是三个独立配置入口，不是三种可以互换的表单；开发、接口建模、页面展示和测试必须分别表达其职责。
- “批记录”“批记录表单”只指工序设置中每个对应工序绑定的批记录表单。工艺路线里的“批记录表单”字段必须按工序读取这条正式绑定关系并展示对应批记录表单，不得把“表单槽位”理解成批记录表单。
- “表单”“表单槽位”只指特殊表单或动态表单中心模板绑定，正式数据源是 `formBindings`。`formBindings` 不得替代、补齐或推断“批记录表单”字段。
- “工序开始”只指特殊节点的上传人、附件负责人或同类开始节点配置。该配置不属于批记录表单，也不属于表单槽位。
- 在批次执行中，“工序开始”配置用于确定特殊开始节点由谁上传附件、承担附件责任或执行同类开始动作；它不提供需要展示、填写或保存的表单内容。
- 在批次执行中，“表单槽位”用于按 `formBindings` 展示、填写和保存补充性的特殊表单或动态表单中心模板；它是工序运行时的补充表单链路，不是该工序的正式生产批记录。
- 在批次执行中，“批记录表单”是对应工序的正式生产批记录载体，用于查看、打开、填写和形成该工序的批记录数据；必须按当前工序在“工序设置”中的正式批记录表单绑定读取，`batchRecordFormNames` 也必须反映这条逐工序绑定。
- 用户提到“批记录”或“批记录表单”时，默认按工序设置中的逐工序批记录表单绑定理解；除非用户明确说“表单”“表单槽位”或 `formBindings`，不得切换到动态表单链路。
- 修改 `batchRecordFormNames`、工艺路线字段明细、节点红绿状态、批记录表单链接或相关接口时，必须先核对工序设置的逐工序批记录表单绑定来源、报表元数据和工序映射；不得使用 `formBindings`、默认 `MAIN` 槽位归类、工序开始上传人或其它特殊表单作为替代来源。
- 修改表单槽位时，只能影响 `formBindings` 及其动态表单展示、保存和运行态链路；不得改变“批记录表单”字段的值、配置状态或链接。
- 若接口或快照缺少正式的逐工序批记录表单绑定，必须阻塞并补齐正式数据链路；禁止用空值、`formBindings`、旧字段猜测、默认 `MAIN`、特殊节点配置或前端文案掩盖来源缺失。
- 验证必须分别覆盖三条独立链路：工序开始上传人、工序设置批记录表单、表单槽位 `formBindings`；任一测试不得以另一条链路的数据证明当前链路正确。

## Trigger-Read Rule Files

- Worktree operations: read `docs\worktree-restrictions.md` before creating, starting, stopping, restarting, merging, cleaning, or deleting any IntRuoyi worktree.
- Branch runtime port governance: read `docs\branch-runtime-ports.md` before changing local frontend/backend ports, branch startup scripts, worktree slot rules, or merge/push guards.
- Backend development: read `docs\backend-development.md` before modifying Java, Spring Boot, Maven, backend APIs, services, mappers, backend configuration, or backend tests.
- Frontend development: read `docs\frontend-development.md` before modifying Vue, TypeScript, Vite, routes, frontend APIs, styles, frontend configuration, or frontend tests.
- Local runtime operations: read `docs\local-runtime.md` before starting, stopping, restarting, or troubleshooting local frontend/backend services or ports.
- Server operations: read `docs\server-access.md` before any test, production, backup-server, SSH, remote status, remote restart, or remote deploy action.
- Login, tenant, and account use: read `docs\login-access.md` before login, E2E login setup, tenant selection, account use, or permission-path debugging.
- E2E and Playwright: read `docs\e2e-rules.md` before writing, modifying, running, or judging real-path E2E tests.
- Database, SQL, menu, and tenant data: read `docs\database-rules.md` before schema checks, SQL, migrations, menu permission changes, tenant bindings, or data repair.
- PowerShell and encoding: read `docs\powershell-encoding.md` before commands involving Chinese text, here-strings, SQL/stdin, SSH/MySQL stdin, or text file writes.
- PowerShell and Git orchestration: read `docs\powershell-memory.md` before Git commits, pushes, dirty-worktree baseline commits, multi-command PowerShell orchestration, or long-chain command execution.
- Task start, commits, cleanup, and closeout: read `docs\task-closeout-rules.md` before task documentation, implementation commits, cleanup preview/apply, or final closeout.
- Release, backup, restore, and rollback: read `docs\release-backup-restore.md` before build-release, publish, promote, backup, restore, rollback, or release troubleshooting.
- If a required trigger-read file is missing, fail fast and report the missing file and impact; do not continue with memory or improvised rules.

## Communication Policy

- Keep user-facing updates concise and necessary: blocking questions, blockers, completed work, verification results, or information explicitly requested by the user.
- When ambiguity affects safety, data, scope, release, or irreversible changes, stop and ask a concise action-oriented question before proceeding.
- 用户提示不合理时，先核对当前代码、文档和业务规则；发现错误或风险必须指出并给出可执行修正，不得盲从。
- 回复用户时默认按“用户不懂代码”处理：用简单、业务化的话说明做了什么、业务影响、风险和下一步；除非用户明确要求技术细节，不要用代码、文件名、函数、类、接口等技术表达来解释。

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

- Before backend implementation or verification, read `docs\backend-development.md`.
- Before SQL, migrations, menu permissions, tenant bindings, or schema-dependent changes, also read `docs\database-rules.md`.

## Frontend Work Rules

- Before frontend implementation or verification, read `docs\frontend-development.md`.
- Before real user-path verification, also read `docs\e2e-rules.md`.

## E2E, Data, and Tenant Safety

- E2E must use Playwright through real frontend user paths. APIs may be used only for final verification or read-only supporting checks.
- `int_main` default local frontend entry is `http://127.0.0.1:8081` or `http://localhost:8081` only after confirming the service is running.
- `int_main` default local backend API entry is `http://127.0.0.1:48081` only after confirming the service is running.
- Branch-specific local entries must follow `docs\branch-runtime-ports.md`; do not use `8081/48081` for `int_batch`, `int_shedule`, or `int_qms`.
- Write-type E2E must use a confirmed test tenant/account and create traceable, task-owned test data. Do not modify production tenant data, admin baseline data, or unrelated real business records.
- If login credentials, tenant baseline, menu permissions, runtime ports, database, Redis, or sample data are missing, fail fast. Do not replace real E2E with mocks, backup data, direct SQL shortcuts, or API-only paths.
- If a frontend entry, menu route, role binding, or dynamic route is missing, distinguish between product scope and environment/setup scope before changing code.

## Server, Release, Backup, and Worktree Safety

- Do not operate remote servers, production services, backups, restores, releases, rollbacks, or shared storage unless the user explicitly authorizes that task.
- If server, login, release, backup, restore, or worktree instructions are required but missing from current `docs\`, stop and report the missing document and impact.
- Do not delete, unmount, clear, or rewrite shared storage or deployment artifacts unless the task scope explicitly authorizes it and verification/rollback steps are documented.
- Release/build isolation must be explicit. If Git/worktree support is absent in this workspace, do not fabricate a branch, commit, or release workflow.
- Before creating, starting, stopping, restarting, merging, or cleaning any IntRuoyi worktree, read `docs\worktree-restrictions.md` and follow it as the authoritative worktree restriction file.
- Before committing, merging, or pushing branch runtime files, run `scripts\preflight\branch-runtime-port-guard.ps1`; the guard must protect `docs\branch-runtime-ports.md`, branch env files, startup scripts, and `.githooks`, including `post-merge` for fast-forward merge visibility and `pre-push` for final blocking.
- All IntRuoyi task worktrees must be created under `D:\IntRuoyiWorktree\` only.
- `D:\IntRuoyiWorktree\` 下的 worktree 不能占用 `48081`；`48081` 只保留给 `E:\IntRuoyi` 的 `int_main` 后端基准运行态。发现该端口被 `D:\IntRuoyiWorktree\` 下的 worktree 占用时必须 fail fast，不得强杀、不得随机换端口、不得冒充 `int_main` 成功启动。
- Before creating a worktree, resolve the absolute target path and verify it is a child path of `D:\IntRuoyiWorktree\`. If it is outside that root, fail fast and do not create the worktree.
- After creating an additional worktree and before starting frontend or backend, run `scripts\runtime\reserve-worktree-slot.ps1`; `slot >= 51`, base-port collisions, duplicate active profile slots, and duplicate active ports must fail fast.
- Do not create IntRuoyi worktrees under `E:\IntRuoyi`, `IntRuoyiBackend`, `IntRuoyiFronted`, `%TEMP%`, the user profile, or any prior-project directory.
- If `D:\IntRuoyiWorktree\` is missing or not writable, stop and report the missing precondition and impact instead of choosing another directory.

## Git Policy

- Git commits and pushes are not required for task completion. By default, do not create dirty-worktree baseline commits, implementation commits, closeout commits, merges, or pushes.
- A dirty working tree, local commits ahead of `origin`, a missing `origin`, or unavailable Git credentials/network does not block task implementation, verification, closeout, or completion unless the user explicitly requested the affected Git operation.
- Perform Git staging, commit, merge, push, branch, or worktree integration only when the user explicitly requests that operation for the current task.
- When Git operations are explicitly requested, use the repository's actual primary branch (`int_main` unless the user changes it), inspect `git status --short --branch` and the staged file list, and follow the applicable safeguards in `docs\powershell-memory.md`, `docs\worktree-restrictions.md`, and the branch runtime guard rules.
- Never stage, commit, merge, push, rewrite, discard, or clean unrelated user or concurrent-task changes.
- Never use force-push, history rewriting, or destructive reset unless the user explicitly requests that exact operation.
- This section overrides default commit, baseline-commit, push, and Git-based completion requirements in parent rules and project guidance such as `docs\task-closeout-rules.md` and `docs\powershell-memory.md`; their safety checks still apply whenever the user explicitly requests Git operations.

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

- Thread baseline: 子 Agent 调度必须直接调用 collaboration 接口；不得嵌套 functions.exec，也不得用 exec wait 代替 wait_agent。
