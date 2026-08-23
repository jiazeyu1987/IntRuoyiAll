# 20260823 Submit Push Frontend Backend Code

## Task Goal

提交并推送当前 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下归属明确的前后端源码和测试改动；不混入根目录规则、历史任务记录、资源包、迁移包或其它非前后端范围文件。

## Milestones

- [x] 创建任务记录并读取提交、PowerShell、端口守卫和收尾规则。
- [x] 核对当前分支、远端、暂存区和前后端改动归属。
- [x] 完成本次前后端代码的提交前验证。
- [x] 精确暂存前后端代码范围并复核 staged 清单。
- [x] 完成本地 Git commit 并复扫残余改动；推送因网络前置条件失败。
- [x] 完成收尾清理并记录最终状态。

## Expected Verification

- `git status --short --branch`
- `git branch --show-current`
- `git remote -v`
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted`
- 后端目标测试/编译（按当前改动和可用依赖选择）
- `scripts\preflight\branch-runtime-port-guard.ps1`
- `git diff --cached --name-status`
- `git diff --cached --check`
- 推送前待推送历史大文件扫描
- `git push origin int_main`
- 推送后 `git status --short --branch`、前后端残余差异和 `git rev-list --count origin/int_main..HEAD`

## Current Status

blocked

## Applicable Experience Gates

- Git 提交与推送门禁：提交前确认分支、远端、暂存区和文件清单；推送后确认本地分支不再领先远端。
- 前后端代码范围门禁：只暂存 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下归属明确的代码和测试。
- 分支端口守卫门禁：提交或推送前运行 `scripts\preflight\branch-runtime-port-guard.ps1`，确认 `int_main` 使用 `8081/48081`。
- GitHub 大文件门禁：推送前扫描待推送对象，发现超过 100 MB blob 必须停止。
- 提交后残余改动复扫：每次提交后重新检查状态和前后端差异，防止延迟保存改动被遗漏或混入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务仅提交推送已有前后端改动。
- `是否从根因和长期维护角度解决`：是；按明确路径边界提交，不扩大范围或用宽泛暂存掩盖归属不清。
- `是否存在临时补丁或绕过`：否。

## Blockers

- 推送阻塞：GitHub HTTPS 当前不可达；本机配置的 `127.0.0.1:7890` 代理未监听，直连重试被重置。恢复网络或代理后需重新执行 `git push origin int_main`，并确认本地不再 ahead。

## Cleanup Keep

- doc/tasks/20260823-submit-push-frontend-backend-code/task.md
- doc/tasks/20260823-submit-push-frontend-backend-code/execution-log.md
- doc/tasks/20260823-submit-push-frontend-backend-code/verification-report.md
