# Merge 1385 Dirty Worktree Execution Log

## Rule Read Evidence

- Read `AGENTS.md` instructions supplied in the user message.
- Read `docs/task-closeout-rules.md`.
- Read `docs/worktree-restrictions.md`.
- Read `docs/branch-runtime-ports.md`.
- Read `docs/backend-development.md`.
- Read `docs/worktree-memory.md` relevant merge/detached-head gates.
- Read `task-closeout-cleanup` skill and closeout rules.
- Read `project-experience-consolidation` skill.

## Authorization Evidence

- User requested: `提交并融合进int_main`.
- After blocker disclosure, user replied: `确认`.
- Scope now includes all currently dirty tracked changes and untracked files in `C:\Users\BJB110\.codex\worktrees\1385\IntRuoyi`.

## BDD

BDD: Authorized dirty batch is preserved -> Given the source worktree is detached and contains mixed dirty/untracked changes, When the user confirms full-batch fusion, Then the task creates a named integration branch and commits the exact authorized batch without discarding files.

BDD: Fusion reaches int_main only after verification -> Given the integration branch contains the authorized batch, When it is rebased or merged onto current `int_main`, Then required static/focused verification is recorded before final `int_main` completion.

## TDD / Verification Evidence

- GREEN: source-dirty-snapshot -> PASS, source HEAD b4303b4ed0c7f5344057694268b96fecdc5c2626, tracked patch SHA256 7FF07F342BFBDC8283D67B2C185A710234C0053A2B15EABE8796218B089E7A06, two non-ignored untracked files copied.
- GREEN: integration-worktree-created -> PASS, branch `codex/20260914-merge-1385-to-int-main`, path `D:\IntRuoyiWorktree\20260914-merge-1385-to-int-main`, profile `int_main`, slot 24, frontend 8158, backend 48158.
- GREEN: branch-runtime-port-guard -> PASS before batch commit.
- GREEN: dirty-patch-replay -> PASS, target tracked patch hash equals source tracked patch hash.
- GREEN: git-diff-check -> PASS before batch commit.
- GREEN: conflict-marker-scan -> PASS, no merge conflict markers under IntRuoyiBackend, IntRuoyiFronted, docs, or scripts.
