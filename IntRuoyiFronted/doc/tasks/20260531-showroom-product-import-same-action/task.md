# 任务：展厅产品导入相同产品选择覆盖或跳过（前端）

## 任务目标

在展厅产品管理导入弹窗中提供“相同产品处理”选择，让用户导入时明确选择相同/无变化产品是跳过还是覆盖发布新版本，并把选择传给后端导入接口。

## 前序任务检查

- 已确认上一前端任务 `doc/tasks/20260531-showroom-import-result-dialog-linebreak/task.md` 状态为 completed，不阻塞本任务。
- 当前前端仓库存在无关改动 `src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 和一个已 Blocked 的运行控制台任务目录，本任务不触碰、不提交。

## BDD 场景

- BDD: 导入弹窗默认跳过相同产品 -> Given 用户打开展厅产品管理导入弹窗 / When 未调整相同产品处理方式 / Then 默认选择跳过相同产品。
- BDD: 用户选择覆盖相同产品 -> Given 用户打开展厅产品管理导入弹窗 / When 选择覆盖相同产品并提交 / Then 前端随导入请求发送覆盖参数。
- BDD: 导入中禁用选择 -> Given 导入请求正在提交 / When 弹窗处于 loading / Then 相同产品处理选择不可编辑，避免提交中改变语义。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：梳理现有导入弹窗和静态测试。
- [x] M3：补充 RED 前端测试。
- [x] M4：实现导入弹窗选择和 API 参数。
- [x] M5：运行 GREEN、类型检查、证据校验和 closeout 预览。
- [x] M6：提交本任务直接相关前端改动。

## 预期验证

- RED：`node scripts/showroom-admin-product-import-form.test.mjs` 先失败，证明弹窗缺少相同产品处理选择。
- GREEN：`node scripts/showroom-admin-product-import-form.test.mjs` 通过。
- REGRESSION：`pnpm ts:check` 通过。

## Current Status

completed

## 当前状态

status: completed

已完成导入弹窗“相同产品处理”选择、默认跳过、覆盖参数提交和测试覆盖；待随本任务提交落库。

## 完成工作

- 在展厅产品导入弹窗增加“相同产品处理”单选按钮组，提供“跳过”和“覆盖”。
- 默认值为 `SKIP`，每次打开弹窗重置为跳过；导入提交中禁用该选择。
- 上传 FormData 增加 `sameProductAction`，与后端新增必填参数对齐。

## 最终验证

- `node scripts/showroom-admin-product-import-form.test.mjs` -> PASS，5 tests。
- `pnpm ts:check` -> FAIL，Node 默认堆限制下 exit code 134，报 JavaScript heap out of memory。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260531-showroom-product-import-same-action/frontend-feature-evidence.md` -> PASS。
- 浏览器 E2E 入口检查 -> BLOCKED，`http://localhost:8081` 当前停留在启动页；本地后端 48081/48082 无监听，且前端日志曾出现 Vite 依赖更新 `EMFILE: too many open files`，属于本地运行前置条件缺失。

## Cleanup Candidates

- doc/tasks/20260531-showroom-product-import-same-action/frontend-feature-evidence.md
