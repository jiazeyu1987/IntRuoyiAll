# 任务：修复前端发布输入 lockfile 供应链门禁

- Task ID: `20260629-frontend-lockfile-supply-chain-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

修复前端发布输入的 pnpm 门禁问题，包括：
- `pnpm-lock.yaml` 中与当前供应链策略冲突的 tarball URL
- 干净发布 worktree 安装依赖时必须显式放行的 build scripts 配置

最终恢复干净前端发布 worktree 的 `pnpm install` 能力，为后续真实 `build-release -> publish-test` 提供可安装依赖且已提交到 git 的前端发布源。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-keyword-label\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，本次单独处理前端发布链路前置问题。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本任务命中发布链路经验，必须同步参考维护仓发布经验和 worktree 规则。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - 影响发布链路的前端依赖、锁文件和构建入口问题必须先在契约层收敛，再回到真实 `build-release`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 当前发布使用的是干净前端 detached worktree；修复应先在前端正式仓收口，再回灌到发布输入。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 锁文件与任务文档读写必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修复 lockfile 供应链策略冲突，不绕过 pnpm policy。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 干净前端发布源可安装依赖 -> Given 前端发布使用干净 worktree / When 在该 worktree 执行 pnpm install / Then 不再触发 tarball URL 供应链门禁和 ignored builds 门禁，vite CLI 可用。`

## Milestones

1. M1：创建任务文档并记录发布阻塞证据。`completed`
2. M2：先让前端锁文件校验失败暴露为 RED。`completed`
3. M3：最小修复 `pnpm-lock.yaml` 并验证供应链门禁通过。`completed`
4. M4：补齐 build scripts 放行配置并验证安装通过。`in_progress`
5. M5：回写验证结果并准备回到维护仓重试 build-release。`pending`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260629-21a09c9 install --frozen-lockfile`
- `Test-Path D:\ProjectPackage\Int\release-worktrees\IntRuoyi-frontend-20260629-63cedb3\node_modules\vite\bin\vite.js`

## Current Blockers

- 当前已提交 HEAD 仍缺少 pnpm build scripts 放行配置。锁文件供应链修复已提交，但新的干净前端发布 worktree执行 `pnpm install --frozen-lockfile` 时仍会报 `ERR_PNPM_IGNORED_BUILDS`，要求显式批准 `@parcel/watcher`、`@swc/core`、`core-js`、`core-js-pure`、`es5-ext`、`esbuild` 与 `vue-demi` 的 build scripts。下一步需将该官方放行配置纳入 git 提交。
