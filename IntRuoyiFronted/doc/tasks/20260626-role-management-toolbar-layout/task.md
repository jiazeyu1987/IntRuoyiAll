# 任务：权限角色页筛选工具栏排版优化

## 任务目标

- 修复 `系统管理 / 角色管理 / 权限角色` 页面筛选工具栏在当前桌面宽度下控件相互遮挡的问题。
- 将查询字段区与操作按钮区改为更稳定的分组排布，保证在常见桌面宽度下不遮挡、在较窄宽度下可自然换行。
- 不修改现有筛选字段、按钮文案、权限门禁、查询参数和业务行为。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 frontend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-role-management-split-rename-navigation\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成角色管理三分改名与兼容路由，本次仅继续优化 `src/views/system/role/index.vue` 的筛选工具栏排版，不回退其文案与路由改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 工具栏继续沿用 IntPP 运维台紧凑、白底、轻边框、明确分组的风格，不做无关视觉重构。
  - 本轮优先做前端源码、静态合同和真实本机页面只读复验；如需登录验收，先跑官方 `login-preflight.mjs`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。通过正式布局分组与响应式换行解决遮挡，不添加特例分支或隐藏按钮。
- `是否从根因和长期维护角度解决`：是。将查询字段与操作按钮拆成稳定分组容器，避免继续依赖单个 `inline el-form` 自然流式排版。
- `是否存在临时补丁或绕过`：否。不会通过缩小文案、去掉标签或硬编码单次 margin 临时躲避遮挡。

## BDD 场景

- `BDD: 权限角色页查询字段与按钮分组显示 -> Given 用户打开权限角色页面 / When 工具栏渲染搜索字段和操作按钮 / Then 查询字段区与按钮区应按分组排布，不再互相挤压遮挡。`
- `BDD: 权限角色页工具栏在较窄宽度下可换行 -> Given 当前页面可用宽度不足以单行承载全部控件 / When 工具栏发生换行 / Then 控件应保持完整可见与稳定间距，不出现日期选择器压到其他字段或按钮的情况。`

## 里程碑

1. M1：创建任务台账并补静态 RED 布局合同。`COMPLETED`
2. M2：最小修改权限角色页工具栏结构和样式。`COMPLETED`
3. M3：运行 GREEN 静态验证与真实页面只读复验。`COMPLETED`
4. M4：回写证据并执行收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/role-management-toolbar-layout-static.spec.js`
- `node tests/e2e/role-management-split-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /system/role/permission-role --target-text 权限角色 --timeout 90000`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-role-management-toolbar-layout --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`

## 最终验证结果

- `node tests/e2e/role-management-toolbar-layout-static.spec.js` -> PASS
- `node tests/e2e/role-management-split-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /system/role/permission-role --target-text 权限角色 --timeout 90000` -> PASS
- `node_repl + Playwright(Edge executable)` -> PASS，真实页面证据位于 `D:\ProjectPackage\Int\IntRuoyi\output\playwright\20260626-role-management-toolbar-layout\permission-role-toolbar-layout.json` 与 `permission-role-toolbar-layout.png`；1366 宽度下 `filtersBox.height=58`、`actionsBox.y=214`，按钮组位于字段区下方，无遮挡。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-role-management-toolbar-layout --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS
