# 工艺路线开始/结束节点可点击与可连线

## 任务目标

- “工序开始”和“工序结束”节点可点击、可选中，并显示边界关系摘要。
- “工序开始”允许连接多个首工序；“工序结束”只允许一个末工序接入。
- 边界连接线作为真实草稿参与选择、删除、保存和刷新恢复。
- 普通工序最多一个后续、允许多个前置汇合；不新增属性编辑界面。

## 非目标

- 不提供开始/结束节点名称、备注等属性编辑。
- 不允许拖动开始/结束节点。
- 不改变现有工艺路线入口、权限、普通工序详情字段配置和顶部保存链路。

## 前置任务检查

- 已确认已提交任务 `20260710-route-flow-same-route-single-tab` 状态为 completed。
- 主工作区未提交任务 `20260710-route-flow-detail-partial-refresh` 与本任务修改同一组件，已在开始本任务前显式标记 blocked；当前隔离分支未包含其生产代码。

## 工作区与运行目标

- 分支：`codex/20260710-route-flow-boundary-links`
- 前端：`D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-route-flow-boundary-links\yudao-ui-admin-vue3`
- 后端：`D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-route-flow-boundary-links\ruoyi-vue-pro`
- 前端端口：`8094`
- 后端端口：`48094`
- 数据库：`127.0.0.1:23306/ruoyi-vue-pro`
- Redis：`127.0.0.1:26379`
- 文件服务：受保护默认配置 `config_id=28 / yudao / http://127.0.0.1:9000/yudao`，本任务不修改。

## BDD 场景

- BDD: 点击边界节点 -> Given 用户打开流转关系图 / When 点击工序开始或工序结束 / Then 对应边界节点显示选中状态和只读关系摘要，普通工序详情不被误用。
- BDD: 多开始分支汇合 -> Given 路线包含多个首工序和一个汇合工序 / When 从工序开始分别连接多个首工序并将分支汇合 / Then 草稿允许保存且刷新后边界关系完整恢复。
- BDD: 限制非法连接 -> Given 普通工序已有后续或工序结束已有入口 / When 用户尝试增加第二条受限连接 / Then 页面明确拒绝且不替换已有关系。
- BDD: 边界关系可删除 -> Given 用户选中开始或结束边界连接线 / When 删除该关系 / Then 草稿关系消失并在保存后持久化。

## 里程碑

1. [completed] 完成任务记录、经验门禁和 RED 静态测试。
2. [completed] 扩展前端 API 类型、边界选择状态和关系草稿模型。
3. [completed] 实现边界节点点击、连线、选择、删除与自动布局。
4. [completed] 运行静态测试、相关回归、ESLint 和 TypeScript。
5. [completed] 在隔离运行态完成测试租户真实 Playwright E2E。
6. [completed] 完成提交、快进融合和融合后复验。
7. [completed] 清理任务附属产物、停止隔离服务并移除 worktree。

## 预期验证

- `node --test tests/e2e/mes-route-flow-graph-static.spec.js tests/e2e/mes-route-flow-boundary-links-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/api/mes/pro/route/index.ts src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-boundary-links-static.spec.js tests/e2e/mes-route-flow-boundary-links-real.e2e.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `node tests/e2e/mes-route-flow-boundary-links-real.e2e.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260710-route-flow-boundary-links/frontend-feature-evidence.md`
- 测试租户 `aoteman` 通过 `http://127.0.0.1:8094` 真实点击、拖拽连线、删除、保存和刷新。

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`，中文文件使用显式 UTF-8，修改使用 `apply_patch`，命令不使用 `&&`。
- Worktree：已读取 `docs/worktree-memory.md`，前后端成对分支，端口使用 `8094/48094`，不复用主工作区 `8081/48081`。
- 前端样式：沿用现有流转图操作台样式，不做额外视觉重设计。
- 前端交付：保持现有路由、权限和错误反馈，不添加 mock、测试专用控件或静默降级。
- BDD/TDD：先写失败测试，再最小实现，记录 RED、GREEN、REGRESSION。
- 真实 E2E：执行前读取 `docs/login-access.md` 并运行官方登录 preflight，只操作测试租户。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；边界关系使用稳定 `START/END` 身份和独立 API 状态，不再从普通工序边运行时推断。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：前端实现已快进融合到 `int_main`；静态契约、ESLint、8GB TypeScript 和真实 Playwright E2E 均通过；测试路线恢复原拓扑，隔离服务、任务产物、worktree 和任务分支均已清理。

## Current Status

completed
