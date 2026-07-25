# 任务：放行预检错误提示 5 秒自动消失

## Task Goal

- 根据用户截图，将 eDHR 批次详情页放行预检错误提示改为显示后 5 秒自动消失。
- 保留真实错误暴露，不改后端接口、不吞异常、不把失败伪装为成功。

## Milestones

1. M1：定位错误提示来源，记录 BDD/TDD 验收场景。`in_progress`
2. M2：先补失败静态契约测试，证明当前没有 5 秒自动隐藏机制。`pending`
3. M3：实现最小前端状态逻辑，让 `releaseActionError` 显示后 5 秒自动清空。`pending`
4. M4：运行定向静态测试、类型检查或等效前端验证，记录证据。`pending`
5. M5：完成任务文档、收尾验证、提交并推送。`pending`

## Expected Verification

- `node tests/e2e/edhr-batch-release-state-ui-static.spec.js`
- `pnpm ts:check` 或若被既有问题阻塞，记录最小可复现 blocker 与影响。

## Current Status

- `in_progress`

## 经验门禁

### PowerShell / Git 提交与推送门禁

- Trigger: 本任务需要保存脏工作区基线、修改前端源码、提交并推送。
- Preflight check: 已读取 `docs/powershell-memory.md`，执行 `git status --short --branch`、`git branch --show-current`、`git remote -v`、`git diff --cached --name-status`。
- Blocker: 缺少 `origin`、发现敏感文件、无法区分当前任务文件、推送失败或本地分支仍 ahead 时不得标记完成。
- Verification: 记录基线提交、实现提交、收尾提交、推送后状态。
- Forbidden action: 禁止 force push、历史重写、destructive reset、跳过 push、把基线提交与本任务实现混在一起。

### 前端行为变更门禁

- Trigger: 修改 `IntRuoyiFronted` 下 Vue 组件与前端静态测试。
- Preflight check: 已读取 `docs/frontend-development.md` 与 `frontend-feature-delivery` 技能说明，保持现有组件、API、样式和错误展示模式。
- Blocker: 缺少页面入口、测试命令、API 契约，或验证失败时不得宣称完成。
- Verification: 先 RED 后 GREEN，至少运行受影响静态契约测试。
- Forbidden action: 禁止吞异常、默认成功、mock 数据、隐藏真实 API 错误。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；错误仍真实显示，只增加 5 秒后清空当前错误提示的前端状态管理。
- `是否从根因和长期维护角度解决`：是；集中封装 release action 错误的展示与定时清理，避免散落 setTimeout。
- `是否存在临时补丁或绕过`：否。

