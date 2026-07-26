# 20260726-route-first-screen-load

## Task Goal

优化首次进入“工艺流程”页签/页面的首屏加载时间，减少列表首屏不必要的重型组件加载，同时保持现有工艺路线列表、弹窗、编辑页和权限行为不变。

## Milestones

- [x] 建立任务记录并保存进入任务前的脏工作区基线。
- [x] 定位工艺流程首屏加载链路和重型同步依赖。
- [x] 补充首屏按需加载静态契约，先 RED 再实现。
- [x] 实施工艺路线列表首屏按需加载优化。
- [x] 运行目标验证并记录结果。

## Expected Verification

- 目标静态契约证明工艺路线列表首屏不再同步导入重型弹窗/设计器。
- 受影响前端文件通过最小静态检查或等效结构验证。
- 若全量前端类型检查/构建存在无关历史阻塞，记录首个阻塞点并保留本任务最小验证证据。

## Current Status

ready_for_closeout

## Baseline

- Dirty worktree baseline commit: `697f4e3b chore: baseline dirty worktree before route load optimization`
- Baseline scope: 保存本任务开始前已有 tracked/untracked/staged 改动，当前任务不回滚、不覆盖这些改动。

## Implementation Evidence

- Static contract and task documents were captured by concurrent baseline commit `792fec93 chore: baseline dirty worktree before form slot badge`.
- Async component implementation was captured by concurrent baseline commit `377d00db chore: baseline follow-up dirty worktree before form slot badge`.
- Current HEAD contains the required code changes:
  - `IntRuoyiFronted/src/views/mes/pro/route/index.vue`
  - `IntRuoyiFronted/src/views/mes/pro/route/RouteForm.vue`
  - `IntRuoyiFronted/src/views/mes/pro/route/RouteFormContent.vue`
  - `IntRuoyiFronted/tests/e2e/mes-route-first-screen-defer-static.spec.js`

## Verification Result

- `node tests/e2e/mes-route-first-screen-defer-static.spec.js` -> RED before implementation, then GREEN after implementation.
- `pnpm ts:check` -> PASS.
- `pnpm build:local` -> BLOCKED/TIMEOUT after 604s; task-owned timeout process tree was stopped (`43028`, `17480`, `59032`). No build success was claimed.

## Closeout Blocker

- Current repository has concurrent dirty changes and additional ahead commits unrelated to this task. Per task ownership rules, this task cannot safely perform final closeout commit/push without capturing unrelated concurrent task work.

## Cleanup Keep

- doc/tasks/20260726-route-first-screen-load/frontend-feature-evidence.md

## Experience Gates

### Frontend page/style gate

- Trigger: 修改前端页面、表格、样式或页面级组件加载路径。
- Preflight check: 已读取 `docs/frontend-development.md` 与 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本任务不做视觉改版，仅保持现有工艺路线列表样式。
- Blocker: 若需要改后端接口、菜单权限、真实 E2E 登录或端口启动，必须先读取对应触发规则并确认前置条件。
- Verification: 以静态契约和受影响文件检查证明首屏导入链路收敛。
- Forbidden action: 禁止通过隐藏接口错误、mock 数据、占位成功或移除功能来换取加载速度。
- Evidence: `docs/experience-index.md` 前端页面路由，`frontend-feature-delivery` 技能契约。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是拆分首屏同步依赖并保持功能按需加载。
- `是否存在临时补丁或绕过`：否。
