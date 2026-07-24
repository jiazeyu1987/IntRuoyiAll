# 任务：eDHR 批次行一键预检

## 任务目标

- 按导演优化文档 `P0-1` 实施：在 eDHR 批次执行列表行操作中增加“预检”入口。
- 从批次行自动带入 `routeId`，减少第二次演练前查库和手填路线 ID。
- 保持预检只读，不写租户数据。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-edhr-rehearsal-role-selector\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成并提交，不阻塞本次 P0 优化。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面继续遵循 IntPP 操作台风格，行级操作使用轻量 link button。
  - 错误必须可见，不得在缺少 routeId 时静默打开空预检。
  - 本切片只读，不创建/修改批次、权限、BPM 或模板。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，把路线 ID 从批次上下文自动带入，减少人工查库。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 批次行可一键预检 -> Given 批次列表行包含 routeId / When 用户点击该行“预检” / Then 预检对话框打开并自动填入路线 ID。`
- `BDD: 缺少路线 ID 必须阻塞 -> Given 批次列表行缺少 routeId / When 用户点击该行“预检” / Then 页面显示明确错误，不调用 readiness API。`
- `BDD: 行预检仍复用人员选择器 -> Given 预检对话框已打开 / When 用户选择执行人、审批人、归档员 / Then 提交仍走同一个 readiness API。`

## 里程碑

1. M1：创建任务包与 RED 静态合同。`DONE`
2. M2：实现批次行一键预检。`DONE`
3. M3：运行静态合同、类型检查和证据校验。`DONE`
4. M4：收尾清理预览并提交。`IN_PROGRESS`

## 预期验证

- `node tests/e2e/edhr-batch-row-readiness-static.spec.js`
- `node tests/e2e/edhr-rehearsal-readiness-panel-static.spec.js`
- `node tests/e2e/edhr-rehearsal-role-selector-static.spec.js`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`

## 当前状态

`COMPLETED`

已在 eDHR 批次执行列表行操作中新增“预检”，点击后自动带入该批次 `routeId` 打开预检对话框；若批次缺少路线 ID，会直接显示明确错误。

## Cleanup Keep

- `doc/tasks/20260622-edhr-batch-row-readiness/frontend-feature-evidence.md`
