# 20260830 Commit Current Code Push

## Task Goal

提交并推送当前 `int_main` 分支中可安全归属的当前代码与任务记录，推送到 `origin/int_main`。

## Milestones

- [x] 读取 Git、PowerShell、任务收尾和 worktree 相关规则。
- [x] 盘点当前分支、远端、暂存区、工作区变更和本地领先提交。
- [x] 排除测试临时目录、运行日志等不应提交产物，确认提交边界。
- [x] 提交可安全归属的当前变更。
- [x] 补齐最终复扫发现的 MES 报工数据结构化字段 key 与字段长度正式迁移。
- [x] 复核最终复扫发现的 BPM 审批角色展示和注册证提醒排序运行态脚本边界。
- [x] 运行推送前门禁并推送 `int_main`。
- [x] 复核推送后分支不再领先远端，记录最终结果。

## Expected Verification

- `git status --short --branch`
- `git diff --cached --name-status`
- `git diff --check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- GitHub 待推送对象大小扫描
- MES 单元格链接目标用例
- Release migration policy gate
- `pnpm ts:check`
- BPM 模型静态合同
- 提醒排序运行态脚本语法检查
- `git push origin int_main`
- 推送后 `git status --short --branch`
- 未跟踪真实 E2E 脚本安全边界核对

## Applicable Experience Gates

- Git 提交推送门禁：提交前必须确认当前分支、远端、暂存清单、冲突状态和脏文件边界；推送后必须确认本地分支不再领先远端。
- GitHub 大文件门禁：推送前扫描待推送对象，发现超过 100 MB blob 必须停止，不做历史重写或 LFS 迁移，除非用户另行明确授权。
- 本地主线领先远端复核门禁：记录 ahead/behind 数量和本地 ahead 提交清单；若 fetch 后远端也领先，必须先处理融合与验证，不得直接推送。
- Branch runtime port guard 门禁：提交和推送前运行 `scripts\preflight\branch-runtime-port-guard.ps1`，守住 `E:\IntRuoyi` 的 `int_main` 端口合同。
- 临时产物和未验证真实 E2E 边界门禁：`.pytest-temp/`、`LOG_FILE_IS_UNDEFINED` 等运行/测试产物不得混入提交；未跟踪真实 E2E 脚本若存在硬编码口令兜底或写真实数据且未完成归属/前置确认，不纳入安全提交边界。

## Current Status

completed - 当前代码、最终复扫补齐变更、任务记录和收尾复扫变更已提交并推送到 `origin/int_main`；最终推送后 `HEAD...origin/int_main` 为 `0 0`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，提交推送前对已暴露的测试契约失败做了最小根因修正，并记录真实 E2E 缺少密码前置和未跟踪写数据脚本边界，未用 mock 或跳过声明替代。
- `是否存在临时补丁或绕过`：否。
