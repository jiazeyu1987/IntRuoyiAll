# Task: 工艺路线底部保存统一保存关系图

## 任务目标

- 删除流转关系图页签工具栏里的 `保存关系图` 按钮。
- 底部 `保存` 统一保存工艺路线基础信息和当前流转关系图连线/布局。
- 保存顺序固定为：基础表单校验 -> 关系图预校验 -> 主表保存 -> 关系图保存。
- 关系图预校验不通过时立即阻断，主表和关系图都不保存。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，命令不使用 `&&`，中文读写显式 UTF-8。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell、前端页面样式、BDD/TDD 和项目防错门禁。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次只调整按钮与保存链路，不做无关视觉重设计。
- 前端交付技能：已读取 `frontend-feature-delivery` 和 `frontend-contract.md`，按 BDD + RED/GREEN 记录证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，将关系图保存能力收敛到底部统一保存入口。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 底部保存统一持久化关系图 -> Given 用户编辑工艺路线并调整流转关系图 / When 点击底部保存 / Then 系统先校验关系图，再保存主表和关系图。
- BDD: 关系图校验失败阻断主表保存 -> Given 当前流转关系图校验不通过 / When 用户点击底部保存 / Then 系统提示校验错误，且不调用主表保存。
- BDD: 关系图页签不再单独保存 -> Given 用户进入流转关系图页签 / When 查看工具栏 / Then 页面不再显示 `保存关系图` 按钮，线性关系草稿提示用户点击底部保存。

## 里程碑

- [x] M1：创建任务记录并读取经验门禁。
- [x] M2：补前端 RED 静态契约测试。
- [x] M3：实现底部保存统一保存关系图。
- [x] M4：运行目标验证并更新证据。
- [x] M5：执行 closeout preview；提交因真实路径验证阻塞暂不执行。

## 预期验证

- `node tests/e2e/mes-route-bottom-save-flow-graph-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-static.spec.js`
- `node tests/e2e/mes-route-flow-graph-one-screen-static.spec.js`
- `pnpm.cmd ts:check`

## 当前状态

BLOCKED_FOR_COMMIT：前端实现、静态契约测试和类型检查已通过；真实 E2E 登录与关系图接口可用，但本地后端 `/mes/pro/route/get` 与 `/mes/pro/route/page` 当前返回 `code=500,msg=系统异常`，导致编辑页主表保存路径无法完成真实验证。按项目提交规范，真实路径验证前置不满足，暂不提交。

## 已完成实现

- `RouteFlowGraphDesigner.vue`：移除工具栏独立 `保存关系图` 按钮；暴露 `validateBeforeSubmit()`、`saveFromParent()` 供父组件底部保存调用。
- `RouteFlowGraphDesigner.vue`：线性关系生成提示改为底部保存生效；关系图校验/保存失败均提示并继续抛错。
- `RouteFlowGraphDesigner.vue`：加载关系图时不再隐式调用工序更新接口补默认关键工序，避免已启用路线进入页面即被后端拒绝导致节点不渲染；默认关键工序仅本地展示。
- `RouteFormContent.vue`：`submitForm()` 调整为基础表单校验 -> 关系图预校验 -> 主表保存 -> 关系图保存 -> 单一成功提示。
- `RouteFormContent.vue`：仅 `update` 且存在 `formData.id` 时保存关系图，`create` 保持只创建主表。

## 最终验证结果

- PASS：`node tests/e2e/mes-route-bottom-save-flow-graph-static.spec.js`
- PASS：`node tests/e2e/mes-route-flow-graph-static.spec.js`
- PASS：`node tests/e2e/mes-route-flow-graph-one-screen-static.spec.js`
- PASS：`node --check tests/e2e/mes-route-flow-graph-real-flow.e2e.js`
- PASS：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- BLOCKED：`node tests/e2e/mes-route-flow-graph-real-flow.e2e.js`，阻塞原因为本地后端路线主表接口 500；直接 API 复现：登录成功、`/mes/pro/route-process-flow/get?routeId=922074` 成功，`/mes/pro/route/get?id=922074` 与 `/mes/pro/route/page?...` 返回 `{"code":500,"msg":"系统异常","data":null}`。

## Cleanup Keep

- `doc/tasks/20260709-route-save-flow-graph/task.md`
- `doc/tasks/20260709-route-save-flow-graph/execution-log.md`
- `doc/tasks/20260709-route-save-flow-graph/frontend-feature-evidence.md`
