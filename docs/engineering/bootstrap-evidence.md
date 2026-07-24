# Engineering Bootstrap Evidence

## Goal And Scope

- Goal: initialize `E:\IntRuoyi` as a local Git repository that contains both frontend and backend source trees.
- Scope: root repository metadata, root `.gitignore`, and local baseline commit only.

## Detected Stack

- Backend: Maven multi-module Java/Spring Boot project under `IntRuoyiBackend`, detected from `pom.xml`.
- Frontend: Vue/Vite/pnpm project under `IntRuoyiFronted`, detected from `package.json` and `pnpm-lock.yaml`.
- Repository shape: single local root repository containing both project folders.

## Local Prerequisites

- Git: `git version 2.51.2.windows.1`.
- Existing frontend/backend runtime prerequisites are unchanged by this task.

## Commands

- Status: `git -C E:\IntRuoyi status --short`.
- Ignore verification: `git -C E:\IntRuoyi check-ignore -v <path>`.
- Commit: `git -C E:\IntRuoyi commit -m "chore: initialize local repository baseline"`.
- Install/dev/test/lint/format/build: not changed by this repository bootstrap task; use the existing frontend/backend commands already documented in their project files.
- Smoke: `git -C E:\IntRuoyi status --short` and `git -C E:\IntRuoyi check-ignore -v <path>`.

## RED Evidence

- `git -C E:\IntRuoyi status --short` failed because `E:\IntRuoyi` was not a Git repository.
- `Test-Path E:\IntRuoyi\.gitignore` returned `False`.

## GREEN Evidence

- `git init -b main` initialized the root local repository.
- `git check-ignore -v` confirmed `node_modules`, Maven `target`, runtime output, root `output`, and `.env.local` are ignored.
- `git status --short --untracked-files=normal` showed the expected source/documentation roots for the first local commit.
- `git commit -m "chore: initialize local repository baseline"` created commit `c67686a52e6f960820854536b5f3756c7cf9741f`.
- `git status --short` returned clean after the baseline commit.

## Verification

- Local repository root: `E:\IntRuoyi`.
- Branch: `main`.
- Baseline commit: `c67686a52e6f960820854536b5f3756c7cf9741f`.
- Tracked files: 18300.
- Generated artifacts remain ignored: frontend `node_modules`, backend Maven `target`, runtime folders, root `output`, and `.env.local`.

## Repository Hygiene Rules

- Keep dependency folders, build outputs, runtime logs, local environment overrides, and task-local screenshots/scripts out of the root repository.
- Prefer committing source, required lockfiles, SQL/schema assets, test files, and durable documentation.
- Before future large commits, run `git check-ignore -v` on high-risk paths such as `node_modules`, `target`, `output`, `runtime`, and `.env.local`.

## Environment Variables And Examples

- No new environment variables are required.
- Existing `.env*` files are left in place; local-only or secret-bearing environment files are ignored by the root repository rules.

## CI Status

- No CI target is configured by this task.

## Known Blockers

- None currently.

## Next Specialist Skill

- Use `ci-cd-environment-delivery` if remote CI or deployment automation is requested later.
