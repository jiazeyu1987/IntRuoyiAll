# 任务：DCC 审阅矩阵页签前端改造

## 任务目标

- 在文件类别页内新增 `DCC审阅矩阵` 页签。
- 以“每个文件类别一行”的表格方式展示与维护矩阵。
- 移除旧类别列表中的“审批矩阵”直达入口，统一改由新页签进入。

## 当前状态

`COMPLETED`

## Current Status

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-dcc-preview-file-name-recognition\task.md`
- 状态：`COMPLETED`
- 处理：上一前端任务已完成，不阻塞本次 DCC 页签改造。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务适用强制门禁：
  - 治理页保持 IntPP 运营台风格，使用紧凑表格、蓝/中性标签和连贯的 toolbar/table。
  - 不新增 mock 数据、假权限状态或吞掉接口报错。
  - 真实 E2E 前先记录 `experience-preflight`，当前阶段先做静态测试和 `ts:check`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，统一矩阵入口，避免类别列表和弹窗双入口分叉
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 审阅矩阵页签展示 -> Given 用户进入文件类别页面 When 切到 DCC审阅矩阵页签 Then 能看到类别编码、类别名称、启用状态、会签岗位、批准岗位、版本、生效时间、备注和操作列。`
- `BDD: 旧入口移除 -> Given 用户查看类别列表页签 When 查看操作列 Then 不再直接显示审批矩阵按钮，而改由 DCC审阅矩阵页签统一维护。`
- `BDD: 新页签可增删查改 -> Given 某类别有或无矩阵 When 点击新增/编辑/删除/预览 Then 前端分别调用矩阵详情、保存、删除和预览能力，并正确刷新行状态。`

## 里程碑

1. 补任务文档与执行日志。`DONE`
2. RED：补静态页签合同测试。`DONE`
3. GREEN：实现页签表格、API 与入口调整。`DONE`
4. GREEN：执行 `ts:check` 与静态脚本回归。`DONE`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-category-governance-summary-static.spec.js`
- 新增 `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-review-matrix-tab-static.spec.js`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`

## 预期交付物

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-dcc-review-matrix-tab\execution-log.md`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-dcc-review-matrix-tab\frontend-feature-evidence.md`

## 最终验证结果

- 已在 clean integration worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-int-main-one-shot-integration` 重放 DCC 审阅矩阵前端改动。
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-int-main-one-shot-integration install --frozen-lockfile` -> `PASS`
- `node ...\tests\e2e\dcc-category-governance-summary-static.spec.js` -> `PASS`
- `node ...\tests\e2e\dcc-route-instruction-alert-reduction-static.spec.js` -> `PASS`
- `node ...\tests\e2e\dcc-route-summary-static.spec.js` -> `PASS`
- `node ...\tests\e2e\dcc-review-matrix-tab-static.spec.js` -> `PASS`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm --dir D:\ProjectPackage\Int\IntRuoyiWorktrees\yudao-ui-admin-vue3-int-main-one-shot-integration ts:check` -> `PASS`
