# Verify GitHub Clone Build

## Task Goal

Verify whether a different computer can clone `https://github.com/jiazeyu1987/IntRuoyiAll.git`, obtain a checked-out branch, and build the committed frontend and backend code. Identify any content present locally but absent from the remote.

## Milestones

- [x] Identify the remote branch and current local working-tree state.
- [ ] Clone the remote repository into an isolated verification directory.
- [ ] Verify Git LFS objects are available after clone.
- [ ] Install frontend dependencies and build the frontend from the clone.
- [ ] Build the backend from the clone.
- [ ] Compare the remote commit with uncommitted local content.

## Expected Verification

- A fresh clone checks out a usable branch without manual branch repair.
- Git LFS objects are present and valid in the clone.
- Frontend dependency installation and a documented build command pass.
- Backend Maven package command passes.
- Any omitted local configuration or uncommitted code is explicitly reported.

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，使用全新克隆目录验证远端实际内容，而不是复用当前工作区的依赖或构建输出。
- 是否存在临时补丁或绕过：是，仅在 `output/verify-github-clone-build/` 创建任务专属的可删除验证克隆；不修改远端代码或配置。

## Experience Gate

### Git LFS Fresh-Clone Verification

- Trigger: GitHub clone, first remote bootstrap, Git LFS object verification, frontend/backend build portability audit.
- Preflight check: run `git ls-remote --symref origin HEAD`, clone the target branch into a new ignored directory, then run `git lfs fsck`.
- Blocker: remote has no usable branch, clone does not check out a branch, or `git lfs fsck` reports missing/corrupt objects.
- Verification: record the clone commit SHA, LFS result, frontend install/build result, backend package result, and current uncommitted local paths.
- Forbidden action: do not reuse current workspace `node_modules`, `target`, LFS cache, or build outputs as evidence; do not disable LFS integrity checks.
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24 GitHub 推送前历史大文件门禁`.

## Experience Documentation Note

- `docs/experience-index.md` exists and the Git LFS gate was read.
- `docs/powershell-memory.md`, referenced by the experience index, is missing. This task uses only ASCII paths and avoids Chinese command parameters, here-strings, SQL/stdin, or file writes outside `apply_patch`.

## Current Status

in_progress
