# Execution Log

## User Intent

- 用户要求：“融合int_main,然后启动前后端”。

## Rule Reads

- Read: `docs\worktree-restrictions.md`
- Read: `docs\branch-runtime-ports.md`
- Read: `docs\local-runtime.md`
- Read: `docs\powershell-memory.md`
- Read: `docs\task-closeout-rules.md`
- Read: `docs\powershell-encoding.md`
- Read: `docs\experience-index.md`

## BDD Scenarios

- BDD: merge int_main into int_batch -> Given the current workspace is on `int_batch` with a clean worktree, When `origin/int_main` is merged, Then the merge completes without unresolved conflicts and the branch runtime port guard passes.
- BDD: start int_batch runtime -> Given the merged `int_batch` workspace has required Docker MySQL/Redis dependencies and ports `8041/48041` are available or owned by the same profile, When branch startup scripts are run, Then backend health is UP on `48041` and frontend is reachable on `8041`.

## Progress

- Created task directory: `doc\tasks\20260731-merge-int-main-start-runtime`.
- Initial Git check: current branch `int_batch`; remote `origin` exists; worktree clean before task documents.
- GREEN: experience-preflight -> PASS, applicable gates recorded in `task.md`.
- GREEN: git-fetch-int-main -> PASS, `origin/int_main` updated to `e9eca0b3`.
- GREEN: merge-conflict-resolution -> PASS, resolved `docs\e2e-rules.md` and `docs\experience-index.md` by preserving both sides' gate entries and removing conflict markers.
- GREEN: branch-runtime-port-guard -> PASS, `scripts\preflight\branch-runtime-port-guard.ps1` reported `int_batch` frontend `8041`, backend `48041`.
- GREEN: merge-commit -> PASS, created local merge commit `2c277c09f5c00fb33ee6a5181c4ce738fbdee252`.
- GREEN: runtime-preflight -> PASS, ports `8041/48041` were free; Docker MySQL `23306` and Redis `26379` were listening.
- BLOCKER: backend-build -> `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` failed in `yudao-module-bpm` compilation. Primary errors show Lombok-generated methods/builders unavailable, including `ApprovalTaskReviewContext#setLoginUserId`, `BusinessApprovalPolicyDO#builder`, and `BusinessApprovalPolicySaveReqVO#getPolicyMode`.
- BLOCKER: backend-build-proc-full -> `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` did not complete in bounded runtime and was stopped as this task's Maven process.
- BLOCKER: start-runtime -> backend package is not current, so frontend/backend were not started; old jar startup would violate no-fallback/no-silent-downgrade policy.
- GREEN: experience-consolidation -> PASS, merged reusable Git/Maven/startup lessons into `docs\powershell-memory.md`, `docs\backend-development.md`, and `docs\experience-index.md`.
- RED: JDK 21 backend compile -> FAIL, the project Java 17 baseline was not met and `yudao-module-bpm` reported missing Lombok-generated members.
- GREEN: provision-java17 -> PASS, Microsoft Build of OpenJDK 17.0.20 archive downloaded through the official `aka.ms` entry point and SHA-256 verified as `e46fd292317c6bb0a8fe9dc63115021329f3a63caeba791c185f89f3666a68e5`.
- GREEN: bpm-compile-jdk17 -> PASS, `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile`; 382 sources compiled and Maven reported `BUILD SUCCESS` in 33:14.
- RED: full-package-jdk17 -> FAIL in `yudao-module-dcc`, because `DccNasControlAuditServiceImpl` referenced four `NasRecursive*` types missing from the merge commit tree.
- ROOT CAUSE: merge-tree-integrity -> merge commit `2c277c09` omitted 1922 additions and 483 modifications from the normal three-way result, so the merge ancestry existed without the corresponding content.
- GREEN: recompute-merge-tree -> PASS, detached worktree under `D:\IntRuoyiWorktree\recompute-int-main-merge-20260731` reproduced only the two expected documentation conflicts and produced resolved tree `d4fa4b4d9e5ed6e2d87a1e84d5a56062093fdd65`.
- GREEN: preserve-preexisting-ignored-erp -> PASS, 10 ignored source collisions were inspected; 6 matched the resolved tree and 4 differing ERP runtime files were preserved by exact blob ID.
- GREEN: restore-merge-content -> PASS, all 2405 correction paths were restored and staged; index/worktree hashes match for representative NAS and ERP sources.
- GREEN: full-package-jdk17 -> PASS, `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` completed with `BUILD SUCCESS`; executable Jar size `501172025` bytes.
- BDD: immutable branch backend runtime -> Given the merged backend Jar is current, When the branch startup script launches the backend, Then the running process uses an independent Jar under `output\runtime\int_batch` and a subsequent Maven package does not replace the running archive.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> FAIL, branch backend startup script still referenced `yudao-server\target\yudao-server-exec.jar` directly and lacked stable runtime logging arguments.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> PASS, `12 passed`.
- GREEN: backend-runtime-start -> PASS, Java 17 process started on `48041`, running `output\runtime\int_batch\backend-*.jar`; health returned `UP` and the startup banner contained `项目启动成功！` when decoded with the Windows runtime encoding.
- GREEN: frontend-runtime-start -> PASS, Vite process belongs to this workspace, listens on `8041`, and `http://127.0.0.1:8041/` returned HTTP `200`.
- GREEN: immutable-runtime-regression -> PASS, a second `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` completed with `BUILD SUCCESS` while the backend remained healthy; the running Java command continued to reference the independent runtime Jar rather than the rebuilt target Jar.
- GREEN: stable-runtime-log-restart -> PASS, task-owned launch processes were stopped by exact PID ownership and restarted on the same ports with stdout/stderr under `output\runtime\int_batch`; final backend PID `52848`, frontend PID `40340`.
- WARNING: runtime-log-observation -> a non-fatal scheduled DCC temporary-file cleanup reported an existing missing-file error for tenant `122`; backend health, frontend proxy traffic, and requested startup remained available. No fallback, data repair, or unrelated business-code change was performed.
- GREEN: project-experience-consolidation -> PASS, the merge/build lessons remain merged into `docs\backend-development.md`, `docs\powershell-memory.md`, and `docs\experience-index.md`; the independent runtime Jar rule already has an existing home in `docs\local-runtime.md`, so no new long-term document was created.
- GREEN: implementation-commit -> PASS, commit `179de5e0` recorded the 2405 merge-tree corrections, stable branch runtime change, regression test, experience updates, and task verification records.
- GREEN: task-closeout-preview -> PASS, kept `task.md`, `execution-log.md`, `verification-report.md`, and `bug-regression-evidence.md`; selected only `.runtime\20260731-merge-int-main-start-runtime` for deletion with no blockers or warnings.
- GREEN: task-closeout-apply -> PASS, deleted the task-owned `.runtime\20260731-merge-int-main-start-runtime` directory while the live services continued writing under `output\runtime\int_batch`.
- BLOCKER: recompute-worktree-merge-abort -> `git merge --abort` could not reset four locally preserved ERP runtime files because the temporary recomputation index differed from its worktree.
- GREEN: recompute-worktree-remove -> PASS, after the resolved merge tree and preserved ERP blob evidence were committed in the primary workspace, the task-owned `D:\IntRuoyiWorktree\recompute-int-main-merge-20260731` was removed with `git worktree remove --force`; the path no longer exists and is no longer registered.
