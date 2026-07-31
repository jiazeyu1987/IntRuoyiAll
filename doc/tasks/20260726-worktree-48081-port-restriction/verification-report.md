# Verification Report

## Scope

- Updated root `AGENTS.md` only for the requested policy text.
- Added task-owned records under `doc\tasks\20260726-worktree-48081-port-restriction\`.
- Did not modify unrelated existing dirty files.

## Verification Evidence

- `rg -n -F '不能占用 `48081`' AGENTS.md` -> PASS, matched the new rule at `AGENTS.md:113`.
- `rg -n -F '48081' AGENTS.md docs\branch-runtime-ports.md docs\worktree-restrictions.md` -> PASS, confirms `48081` remains reserved for `E:\IntRuoyi` / `int_main` while the new root rule forbids `D:\IntRuoyiWorktree\` worktrees from occupying it.
- `git diff --check -- AGENTS.md doc\tasks\20260726-worktree-48081-port-restriction` -> PASS, with Git line-ending warning only for `AGENTS.md`.
- `python -X utf8 -c "<read AGENTS.md and task docs as UTF-8>"` -> PASS, `utf8-read-ok`.

## Closeout Status

- Implementation verification passed.
- Cleanup apply passed with no deleted paths, no blocked paths, and no warnings.
- Full commit/push closeout is blocked by pre-existing unrelated workspace state: `git status --short --branch` showed `int_main...origin/int_main [ahead 20]` and many unrelated modified/untracked files before this task began.
