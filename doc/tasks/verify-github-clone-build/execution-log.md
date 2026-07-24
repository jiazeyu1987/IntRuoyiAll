# Execution Log

## 2026-07-24

- Verification objective: simulate a different computer by cloning the GitHub remote into a new ignored directory, then build frontend and backend from that clone.
- BDD: fresh GitHub clone builds -> Given a computer with Git, Git LFS, Java, Maven, Node, and pnpm, When it clones the remote branch and runs the documented frontend/backend build commands, Then both projects should build without relying on files absent from the remote.
- GREEN: experience-preflight -> PASS, read the matching Git LFS large-file gate and will use a fresh clone plus `git lfs fsck`; no LFS bypass is allowed.
- INFO: `docs/powershell-memory.md` is missing although referenced by `docs/experience-index.md`; this task avoids Chinese command parameters and non-`apply_patch` text writes.
- Pending: remote default-branch inspection, fresh clone, Git LFS check, frontend build, backend build, and local-vs-remote comparison.
