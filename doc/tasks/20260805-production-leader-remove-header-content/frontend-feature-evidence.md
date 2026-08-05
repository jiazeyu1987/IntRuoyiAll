# Feature

## Goal

删除生产组长页面截图黄框中的冗余标题、说明和刷新按钮，使功能 Tab 下直接显示操作区和人员列表。

## Non-goals

- 不删除生产组长功能 Tab。
- 不删除“新增人员”、状态筛选、人员列表或分页刷新行为。
- 不修改 PQC 组长标题。
- 不修改 API、后端、权限、菜单、数据库或数据来源。

## Entry And Owned Files

- 页面：生产组长工作台。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 聚焦合同：`IntRuoyiFronted/tests/e2e/production-leader-remove-header-content-static.spec.js`。

# Acceptance

- `AC-REMOVE-01`：六个生产组长模块不再显示“生产组长”标题和说明。
- `AC-REMOVE-02`：人员管理不再显示“生产人员档案”标题和维护说明。
- `AC-REMOVE-03`：人员管理不再显示“刷新人员档案”按钮。
- `AC-REMOVE-04`：功能 Tab、新增人员、状态筛选、列表及分页刷新行为保持。
- `AC-REMOVE-05`：PQC 组长标题保持，不扩大删除范围。

# API Contracts And Data States

- API contract：无变更。
- Loading/empty/error：无变更。
- Permission：无变更。
- Responsive：删除展示节点，不新增宽度或断点规则。
- Accessibility：保留 Element Plus Tab、按钮、筛选和表格语义；只移除用户指定的冗余内容。

# BDD

BDD: 生产组长人员页只保留操作区和列表 -> Given 用户打开生产组长的人员管理 Tab When 页面完成渲染 Then 顶部不显示生产组长标题说明，人员区域不显示生产人员档案标题说明和刷新按钮，功能 Tab、新增人员、筛选与列表保持可用。

# RED

RED: `node tests\e2e\production-leader-remove-header-content-static.spec.js` -> FAIL，人员管理仍包含 `showProductionModuleTabs` 嵌入标题，后续还包含档案说明和刷新按钮。

# GREEN

GREEN: `node tests\e2e\production-leader-remove-header-content-static.spec.js` -> PASS。

# Verification

- `node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `node tests\e2e\production-leader-tabs-flat-style-static.spec.js`
- `node tests\e2e\production-leader-function-tabs-static.spec.js`
- `node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- <task paths>`

# Blockers

- 当前生产组长 flat Tab 基础实现本身仍是共享工作区未提交改动，本任务删除的标题节点依赖该未提交结构，无法相对 `HEAD` 构造独立、完整的实现提交。
- 当前 `int_main` 还包含非本任务 ahead 提交，不能替其它任务宽泛推送。
- 未启动本地服务或执行真实浏览器 E2E。

# Follow-up Skills

- 已按 `project-experience-consolidation` 核对现有经验归宿；前端截图静态合同和共享分支并发门禁已覆盖，无需新增长期经验文档。
