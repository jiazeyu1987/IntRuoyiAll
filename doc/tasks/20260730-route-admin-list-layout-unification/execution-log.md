# Execution Log

## User Intent

- 用户要求不同浏览器中的工艺路线列表统一为“芋道源码 / admin”账号登录时的样式。
- 安全边界：只统一字段布局；导入、导出、复制、删除等操作继续遵循 `v-hasPermi` 权限，不提升普通用户权限。
- 实现边界：升级该列表的用户列配置 key，使旧配置不再生效；保留“显示字段”，用户后续仍可主动调整。

## Rule And Skill Intake

- 使用 `frontend-feature-delivery`，按 BDD + strict TDD 完成用户可见列表行为变更。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md`；适用门禁为前端最小静态合同、权限边界保持、共享分支并发基线和同文件选择性暂存。
- 已确认当前 `int_main` 工作区有并行脏改动，目标文件存在非本任务版本弹窗改动；本任务只修改不重叠的列表列布局区域。
- BASELINE: `git commit -m "基线: 保存当前工作区并行改动"` -> PASS，commit `67282a86`，保存任务开始前 36 个既有并行改动文件；当前任务目录保持未提交。

## BDD Scenarios

BDD: 工艺路线列表统一为 admin 字段布局 -> Given 任意已授权用户从任意浏览器进入工艺路线列表 / When 页面加载列表 / Then 固定显示路线编码、路线名称、状态、当前生效版本、待发布版本、关联产品、创建时间和操作列，并不显示负责人、关键工序和关系图列

BDD: 旧列配置不再影响统一布局 -> Given 用户曾保存过 `mes.pro.route.main` 的个人显示字段或列宽配置 / When 用户重新进入升级后的工艺路线列表 / Then 页面使用新的配置 key 和统一 admin 默认字段布局，旧配置不再生效

BDD: 显示字段能力继续可用 -> Given 用户进入统一后的工艺路线列表 / When 用户点击“显示字段”调整字段 / Then 新配置按升级后的列表 key 保存，不影响其它列表

BDD: 权限边界保持不变 -> Given 普通用户缺少导入、导出、复制或删除权限 / When 用户进入统一布局后的列表 / Then 对应按钮仍由既有权限指令隐藏，不因布局统一获得管理员操作

## Verification Evidence

- RED: `node tests/e2e/mes-route-admin-list-layout-static.spec.js` -> FAIL，符合预期：旧页面没有 `ROUTE_LIST_TABLE_KEY = 'mes.pro.route.main.admin-layout-v1'`，仍读取 `mes.pro.route.main`。
- GREEN: `node tests/e2e/mes-route-admin-list-layout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-list-edit-create-candidate-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-route-version-workspace-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/user-table-column-config-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mes-route-admin-list-layout-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mes-route-admin-list-layout-real.e2e.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: 官方登录前置 `scripts/preflight/login-preflight.mjs` -> PASS，入口 `http://127.0.0.1:8081/mes/pro/route`，身份标签 `芋道源码/admin`。
- GREEN: `node tests/e2e/mes-route-admin-list-layout-real.e2e.js` -> PASS；Chrome 与 Edge 均显示统一 admin 表头和显示字段勾选，admin 的导入、导出、复制、删除入口可见；读取新 key `mes.pro.route.main.admin-layout-v1`，未读取旧 key，MES/管理后台写请求数为 0，console error 数为 0。
- GREEN: 本地运行态归属核对 -> PASS；前端 PID `39032` 来自 `E:\IntRuoyi\IntRuoyiFronted`，后端 PID `50528` 使用 `E:\IntRuoyi\output\runtime\int_main` 稳定 Jar，`8081` HTTP 200，`48081` health `UP`。
- GREEN: `git diff --check -- <task-owned-files>` -> PASS，只有 Git 的 CRLF 提示。

## Milestone Updates

- 2026-07-30：升级工艺路线主列表配置 key 为 `mes.pro.route.main.admin-layout-v1`。
- 2026-07-30：默认隐藏负责人、关键工序、关系图，默认显示路线编码、路线名称、状态、当前生效版本、待发布版本、关联产品、创建时间、操作。
- 2026-07-30：保留“显示字段”自动保存和全部既有 `v-hasPermi` 权限指令。
- 2026-07-30：Chrome/Edge 真实只读路径验证通过，任务进入 `ready_for_closeout`。
- 2026-07-30：执行 `project-experience-consolidation`；经验合并到 `docs/frontend-development.md#前端列表跨账号默认列布局统一门禁`，并更新 `docs/experience-index.md`，未新建长期经验文档。
- 2026-07-30：首次 frontend feature evidence 校验失败，原因是证据文件缺少校验器要求的 `Acceptance`、`BDD:`、`RED:`、`GREEN:` 固定标记；已补齐格式，产品代码和测试结果不受影响。
- 2026-07-30：`validate_frontend_feature.py` 复跑 -> PASS。
- 2026-07-30：task-closeout-cleanup preview -> PASS，仅计划删除 `frontend-feature-evidence.md`，保留三个核心任务文档，无 blocked/warnings。
- 2026-07-30：task-closeout-cleanup apply -> PASS，已删除任务期辅助证据文件，未删除源码、测试或并行任务产物。
- 2026-07-30：提交前复核显示并行任务继续修改后端、eDHR 前端和路线版本任务文档；本任务共享文件 `route/index.vue`、`mes-route-list-edit-create-candidate-static.spec.js` 的未提交 diff 均只包含本任务布局 hunk，可按显式路径暂存。

## Blockers

- 当前分支 `int_main` 已领先 `origin/int_main` 15 个提交、落后 8 个提交，且存在多项并行未提交改动；提交和推送阶段需按共享分支门禁单独处理。
- 基线提交后又出现 `doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/` 并行文档改动；本任务不得暂存或修改这些文件。
