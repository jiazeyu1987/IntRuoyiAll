# 20260820 Submit Frontend Backend Code

## Task Goal

提交当前 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下的前后端代码、测试和相关配置变更；不混入根目录规则、历史任务产物、资源文件、迁移包或其它非前后端代码文件。已根据用户追加要求推送当前 `int_main` 到 `origin`。

## Milestones

- [x] 创建任务记录并读取提交、PowerShell、编码和收尾规则。
- [x] 检查当前分支、远端、暂存区和前后端工作区状态。
- [x] 核对适用前后端验证规则并执行提交前验证。
- [x] 精确暂存前后端代码范围并复核 staged 清单。
- [x] 完成本地 Git commit 并复扫残余改动。
- [x] 完成 cleanup preview/apply 和最终记录。
- [x] 推送当前 `int_main` 到 `origin` 并复核不再领先。

## Expected Verification

- `git branch --show-current`
- `git remote -v`
- `git diff --check -- IntRuoyiBackend IntRuoyiFronted`
- 本轮改动对应的后端目标测试和前端静态合同测试
- `scripts\preflight\branch-runtime-port-guard.ps1`
- `git diff --cached --name-status`
- `git diff --cached --check`
- `git status --short --branch -- IntRuoyiBackend IntRuoyiFronted`

## Current Status

completed

已完成 cleanup、可归属前后端代码提交、提交后补充验证、复扫和远端推送。提交后出现的前端测试残余自检失败，未纳入本次提交并已在执行日志记录。

## Applicable Experience Gates

- `docs/experience-index.md` 已存在。
- Git 提交门禁：提交前确认分支、暂存清单和文件归属；禁止混入秘密文件、超大产物或无关任务文件。
- 前后端代码范围门禁：只暂存 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下的代码、测试和配置变更。
- 提交后残余改动复扫门禁：提交后立即检查前后端目录是否出现延迟保存或并行新增改动。
- 分支端口守卫门禁：提交前运行 `scripts\preflight\branch-runtime-port-guard.ps1`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本次按明确目录边界提交已存在且经过验证的前后端改动。
- `是否存在临时补丁或绕过`：否。
