# Feature

## Goal

将生产组长功能模块 tab 调整为与 PQC 组长一致的紧凑青绿色下划线样式，并将 tab 嵌入当前模块内容卡片顶部，使 tab 下方直接衔接列表、看板、表单或配置内容。

## Non-goals

- 不修改 API、请求参数、响应结构或数据源。
- 不修改后端、数据库、权限、菜单或路由。
- 不引入 mock、fallback、兼容分支或默认成功状态。

## Entry And Ownership

- 页面入口：生产组长工作台。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 测试：`IntRuoyiFronted/tests/e2e/production-leader-tabs-flat-style-static.spec.js` 及相邻生产组长/PQC 静态合同。
- 保护边界：`src/api/**`、后端模块、DTO、数据库和真实业务数据不在本任务修改范围内。

# Acceptance

- `AC-PL-TAB-01`：生产组长模块 tab 使用与 PQC 模块相同的 flat 下划线样式和青绿色 active bar。
- `AC-PL-TAB-02`：独立头部卡片不再占据 tab 与模块内容之间的空间。
- `AC-PL-TAB-03`：人员管理、报工管理、看板、异常、损耗管理和班组配置中的 tab 均位于对应内容之前。
- `AC-PL-TAB-04`：人员管理内仅有一个子 tab 时隐藏冗余子 tab 头，模块 tab 下直接衔接人员列表。
- `AC-PL-TAB-05`：现有接口、数据状态、加载态、空态、错误态和权限逻辑保持不变。

# API Contracts And Data States

- API contract：无变更，继续复用现有生产组长工作台请求与响应。
- Loading/empty/error：未改动现有加载、空列表和错误提示逻辑。
- Permission：未改动角色判断、菜单或按钮权限。
- Responsive：复用现有卡片和 Element Plus tabs 布局，未新增固定宽度。
- Accessibility：保留 `el-tabs` 原生键盘和 ARIA 行为，未改写交互控件语义。

# BDD

BDD: 生产组长模块 tab 紧凑衔接内容 -> Given 用户打开生产组长工作台 When 查看或切换人员管理、报工管理、看板、异常、损耗管理、班组配置 Then tab 使用与 PQC 一致的青绿色下划线选中态，且 tab 下方直接显示当前模块内容，不出现独立空白头部区域。

# RED

RED: `node tests\e2e\production-leader-tabs-flat-style-static.spec.js` -> FAIL，旧页面仍把生产组长模块 tab 放在独立头部卡片中，内容卡片缺少 flat 样式和紧凑衔接结构。

# GREEN

GREEN: `node tests\e2e\production-leader-tabs-flat-style-static.spec.js` -> PASS，生产组长六个模块均使用 shared flat tabs，并位于各自内容之前。

# Verification

- `node tests\e2e\production-leader-tabs-flat-style-static.spec.js`
- `node tests\e2e\production-leader-function-tabs-static.spec.js`
- `node tests\e2e\pqc-leader-module-tabs-static.spec.js`
- `node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-tabs-flat-style-static.spec.js IntRuoyiFronted/tests/e2e/production-leader-function-tabs-static.spec.js IntRuoyiFronted/tests/e2e/pqc-leader-module-tabs-static.spec.js doc/tasks/20260805-production-leader-tabs-flat-style`

# Blockers

- 当前共享 `int_main` 工作区同一 Vue 文件和相邻测试存在其它并发任务的 staged/unstaged 改动，本任务不能安全地独立提交或推送这些混合改动。
- 本任务未启动或修改本地服务；定向静态合同和 TypeScript 检查用于验证实现与回归。

# Follow-up Skills

- 收尾使用 `task-closeout-cleanup` 清理本临时 evidence 文件，保留核心任务记录。
- 经验核对使用 `project-experience-consolidation`；现有前端截图样式、静态合同隔离和共享分支并发门禁已覆盖本次经验，无需新增长期经验文档。
