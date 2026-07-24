# 任务：修复展厅菜单标题占位

## 任务目标

修复侧边栏“展厅”下出现 `Please set title` 的问题。所有展厅相关菜单项必须展示明确的简体中文业务标题，不允许把前端默认占位文案暴露给用户。

## 里程碑

- [x] M1：读取经验门禁，确认上一前端任务已完成，创建任务文档。
- [x] M2：只读复现并定位缺失标题的菜单/路由来源。
- [x] M3：先写 RED 回归测试，锁定展厅菜单不得出现 `Please set title`。
- [x] M4：修复菜单标题来源，运行静态和类型验证。
- [x] M5：真实登录 E2E 验证侧边栏“展厅”菜单标题，提交本任务前端/后端相关改动。

## 预期验证

- `node tests/e2e/showroom-menu-title-placeholder-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- 真实 Playwright：本机 `http://localhost:8081` 使用 `测试租户/aoteman/111111` 登录，展开“展厅”，确认侧边栏不出现 `Please set title`。

## 当前状态

已完成。已修复单页 Layout 父壳路由丢失 `title/icon` 的问题，并移除菜单渲染里的英文占位 fallback；真实 E2E 展开展厅菜单后不再出现 `Please set title`。

## Current Status

completed

## 前一任务检查

- 前端上一相关任务 `20260624-signature-record-form-jump` 已提交并完成，允许继续本任务。
- 当前前端仓库存在其它任务脏改动，本任务只修改菜单标题问题直接相关文件和测试。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败必须阻塞，不切换账号或环境。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：用户可见文案必须简洁、明确、偏正式；本次不做无关视觉重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。修复应补齐真实标题来源，不新增备用占位隐藏问题。
- `是否从根因和长期维护角度解决`：是。定位缺失 `meta.title` 的菜单/路由源头并加回归测试。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 展厅菜单不显示英文占位 -> Given 用户登录后台并展开展厅菜单 / When 菜单树渲染完成 / Then 任一可见菜单标题都不应显示 Please set title。`
- `BDD: 缺失标题的展厅入口有中文业务名 -> Given 动态菜单包含展厅子路由 / When 前端合并动态路由与静态路由 / Then 该路由 meta.title 使用明确的简体中文业务标题。`
