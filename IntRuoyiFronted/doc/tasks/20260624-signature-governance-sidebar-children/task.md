# 任务：电子签名侧边栏子菜单仍不可见

## 任务目标

修复电子签名动态菜单已下发但侧边栏不显示子菜单的问题。后端菜单树中的 8 个电子签名子菜单应在前端侧边栏保持可见，不应被静态隐藏直达路由覆盖为隐藏。

## 里程碑

- [x] M1：用 RED 契约复现隐藏静态路由覆盖动态菜单可见性的问题。
- [x] M2：修复 `permission.ts` 的隐藏静态壳路由合并逻辑。
- [x] M3：运行前端契约测试。
- [x] M4：真实登录验证侧边栏可见 8 个电子签名子菜单。
- [x] M5：记录证据、清理临时产物并提交。

## 预期验证

- `node scripts/signature-governance-page-contract.test.mjs`
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，确认侧边栏 `电子签名` 下显示 8 个子菜单。

## 当前状态

已完成。已确认数据库菜单树正确，根因是前端隐藏静态直达路由与动态菜单合并时用 `staticChild.meta.hidden=true` 覆盖了动态菜单的 `hidden=false`，导致侧边栏子菜单不可见。现已改为保留静态组件入口，同时以动态菜单的 `hidden/alwaysShow` 为准。

## Current Status

completed

## 完成记录

- 已新增契约测试覆盖隐藏静态子路由合并逻辑，先 RED 后 GREEN。
- 已通过 Playwright 真实登录测试租户 `测试租户/aoteman`，打开 `http://localhost:8081/signature-governance/overview` 后侧边栏显示 8 个电子签名子菜单。
- 已确认本任务不修改电子签名业务 API、不引入 fallback、不改其他仓库脏改动。

## 前一任务检查

- 上一前端电子签名任务 `20260624-signature-governance-route-subtabs` 已完成。
- 当前前端仓库有其他任务脏改动；本任务只修改电子签名前端路由契约、权限路由合并逻辑和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本任务不改视觉风格，只修复菜单可见性。
- `docs/worktree-memory.md`：提交只暂存本任务文件，避免混入其他前端脏改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 动态菜单可见性不被隐藏静态直达路由覆盖 -> Given 后端下发电子签名子菜单 hidden=false / When 前端与 hidden=true 的静态直达路由合并 / Then 合并后的菜单仍保持 hidden=false 并在侧边栏显示。`
- `BDD: 电子签名侧边栏显示子菜单 -> Given 用户登录测试租户 / When 打开电子签名菜单 / Then 侧边栏显示 8 个电子签名子菜单。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-sidebar-children/task.md`
- `doc/tasks/20260624-signature-governance-sidebar-children/execution-log.md`
- `doc/tasks/20260624-signature-governance-sidebar-children/frontend-feature-evidence.md`
