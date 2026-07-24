# Push Local Git Repo To GitHub

## Task Goal

Configure `E:\IntRuoyi` to push the local `int_main` branch to `https://github.com/jiazeyu1987/IntRuoyiAll.git`.

## Milestones

- [x] Confirm local repository status and branch.
- [x] Check whether the GitHub remote has existing refs.
- [x] Configure the GitHub remote.
- [x] Restore missing local Git LFS objects required by the first push.
- [x] Push the local `int_main` branch.
- [x] Verify remote tracking and task-owned status.

## Expected Verification

- `git ls-remote https://github.com/jiazeyu1987/IntRuoyiAll.git` is reachable.
- `git remote -v` lists the GitHub repository.
- `git push -u origin int_main` succeeds without force.
- `git status --short -- doc/tasks/push-local-git-repo-to-github docs/engineering/bootstrap-evidence.md` is clean after task-owned commits.

## Current Status

completed
