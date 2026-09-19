# Verification Report: 提交推送前后端代码并启动本地运行态

## Scope

- Branch: `int_main`
- Runtime profile: `int_main`
- Frontend port: `8081`
- Backend port: `48081`
- Runtime start command: `pwsh -NoProfile -File .\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full`

## Verification Results

- Python script regression: `45 passed in 32.42s` with the task-owned pytest basetemp. The first default-temp run was blocked by Windows `PermissionError: [WinError 5]`; no product code was changed for this environment issue.
- Backend targeted JUnit: `RuntimeRestoreCandidateServiceImplTest`, `16 tests`, `BUILD SUCCESS`.
- Backend standard package after merge: all 31 Maven modules `SUCCESS`; `yudao-server-exec.jar` generated.
- Frontend type check: `pnpm ts:check` passed before and after merge.
- Frontend local build: `pnpm build:local` passed.
- Port guard: `scripts\preflight\branch-runtime-port-guard.ps1` passed.
- Working tree whitespace check: `git diff --check` passed.
- Implementation commit: `db8e6c8fc`.
- Merge commit: `bb7d0b33a`.
- Push result: `git push origin int_main` passed; `origin/int_main` reached `bb7d0b33a` at push time.
- Frontend runtime: `http://127.0.0.1:8081/` returned HTTP 200.
- Backend runtime: `http://127.0.0.1:48081/actuator/health` returned `status=UP`.
- Frontend listener: PID `41104`, `node.exe`, current `IntRuoyiFronted` Vite command.
- Backend listener: PID `8288`, `java.exe`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260919-120334.jar`.
- Backend startup log contains `Started YudaoServerApplication`; runtime Jar SHA-256: `7043D347CAA51C348C9D01F28CD5CA04F42D74CC2F9997A8AFF6EB98D84DECA1`.
- Runtime Jar last-write time was before the backend process start time.

## Warnings

- Vite emitted existing non-blocking warnings about the deprecated CJS API, stale Browserslist data, UnoCSS entry detection, and missing source maps. The dev server became ready and returned HTTP 200.
- Maven emitted existing compiler/deprecation and dependency-model warnings; the build completed successfully.

## Concurrent Changes Preserved

After this task's push, another author created local commit `a04a9143f` (`任务: 完善备份恢复证据绑定`) containing six backup-restore files outside this task's ownership. It remains local and was not pushed or modified by this task. The local runtime registry `intrruoyi-runtime\worktree-ports.json` also remains untracked and was not included.

## Closeout

- Cleanup preview: passed; only the task-owned `tmp-pytest/` tree was classified for deletion, while the three core task records were kept.
- Cleanup apply: passed; task-owned `tmp-pytest/` removed.
- Closeout records committed locally with `docs: close out local runtime submission task`.
- The closeout commit is intentionally not pushed because the local branch already contains unrelated concurrent commit `a04a9143f`; pushing the current tip would publish that unrelated work.
- Final task status: `completed`.
