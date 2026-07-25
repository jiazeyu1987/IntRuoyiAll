# 提交前后端代码

## Task Goal

- 将当前根仓库 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 中已完成的前后端相关代码与任务记录提交并推送到 `origin/int_main`。
- 保持前后端目录共享根仓库提交边界，不拆分为子仓库提交。

## Milestones

- [x] 读取提交、PowerShell、编码、端口契约与经验门禁。
- [x] 确认前端、后端目录同属根仓库。
- [x] 保存开始本次提交前已存在的未跟踪任务记录基线提交。
- [x] 运行提交前验证与大文件门禁。
- [ ] 提交本次任务记录。
- [ ] 推送 `int_main` 到 `origin` 并确认不再 ahead。

## Expected Verification

- `git status --short --branch` 显示当前分支为 `int_main`。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过，确认 `int_main_d` 为前端 `8101`、后端 `48101`。
- 推送前历史对象扫描未发现超过 GitHub 100 MB 限制的 blob。
- `git push origin int_main` 成功。
- 推送后 `git status --short --branch` 不再显示 ahead。

## Current Status

in_progress

## 经验门禁

### GitHub 推送前历史大文件门禁

- Trigger: 推送到 GitHub remote，或提交/保留可能超过 GitHub 限制的构建结果、日志归档、operation JSON、压缩包等文件。
- Preflight check: 推送前扫描已提交历史中的 blob 大小，至少确认没有超过 GitHub 100 MB 单文件限制的对象。
- Blocker: `git push` 返回 `GH001: Large files detected` / `pre-receive hook declined`，或本地历史扫描发现任一 blob 超过 100 MB。
- Verification: 记录对象扫描结果、`git push origin int_main` 退出码、推送后的 `git status --short --branch`。
- Forbidden action: 不做历史重写、Git LFS 迁移、快照分支替代或 force push，除非用户明确授权。
- Evidence: `docs\release-build-preflight-lessons.md#2026-07-24-GitHub-推送前历史大文件门禁`。

### D-Main 端口契约门禁

- Trigger: 推送包含分支运行端口、runtime profile、前端 branch env 或启动脚本相关变更。
- Preflight check: 推送前运行 `scripts\preflight\branch-runtime-port-guard.ps1`。
- Blocker: `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 未识别为 `int_main_d`，或端口不是前端 `8101`、后端 `48101`。
- Verification: 记录端口守卫通过结果。
- Forbidden action: 不改写共享 `.env` 或 `application-local.yaml`，不静默换端口。
- Evidence: `docs\branch-runtime-ports.md`、`docs\local-runtime.md`。

## Verification Evidence

- 开始本次提交前仓库状态：`int_main...origin/int_main [ahead 1]`，未跟踪 `doc/tasks/20260725-start-d-main-runtime/`。
- 前后端目录检查：`IntRuoyiBackend` 与 `IntRuoyiFronted` 均显示根仓库 `int_main...origin/int_main [ahead 1]`。
- 既有未跟踪任务记录基线提交：`0a6622c8 docs: baseline d main runtime startup task`。
- `git diff --check`：通过，无 whitespace error。
- `scripts\preflight\branch-runtime-port-guard.ps1`：通过，`int_main/int_main_d` 前端 `8101`，后端 `48101`。
- GitHub 推送前历史对象扫描：最大 blob `4,177,309` bytes，路径 `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-biz-ip/src/main/resources/ip2region.xdb`，低于 100 MB。

## Blockers

- 待记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按提交和推送门禁处理根仓库状态，不拆分或绕过提交边界。
- `是否存在临时补丁或绕过`：否。