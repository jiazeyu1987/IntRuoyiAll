# 任务：修复产品导入点击覆盖未生效

## 任务目标

修复展厅产品导入弹窗中“相同产品处理”点击“覆盖”后实际仍按跳过处理的问题，确保用户选择覆盖时前端提交 `sameProductAction=OVERWRITE`。

## 前序任务检查

- 已确认上一前端任务 `doc/tasks/20260531-showroom-product-import-same-action/task.md` 状态为 completed，不阻塞本任务。
- 当前前端仓库存在无关改动 `src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 和旧任务目录，本任务不触碰、不提交。

## BDD 场景

- BDD: 点击覆盖提交覆盖动作 -> Given 用户打开展厅产品导入弹窗 / When 点击“覆盖”并提交导入 / Then 前端提交 `sameProductAction=OVERWRITE`，后端按覆盖发布处理。
- BDD: 默认跳过保持不变 -> Given 用户打开展厅产品导入弹窗 / When 未点击覆盖直接提交 / Then 前端提交 `sameProductAction=SKIP`。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现并定位点击覆盖未生效原因。
- [x] M3：补充 RED 回归测试。
- [x] M4：最小修复导入弹窗单选按钮取值。
- [x] M5：运行 GREEN、类型检查、证据校验和 closeout 预览。
- [x] M6：提交本任务直接相关前端改动。

## 预期验证

- RED：`node scripts/showroom-admin-product-import-form.test.mjs` 先失败，证明当前单选按钮未使用当前 Element Plus 的 `value` 取值约定。
- GREEN：`node scripts/showroom-admin-product-import-form.test.mjs` 通过。
- REGRESSION：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` 通过。

## Current Status

completed

## 当前状态

status: completed

已完成修复：导入弹窗“跳过/覆盖”按钮改为使用 Element Plus 当前约定的 `value` 属性，确保点击“覆盖”后 `sameProductAction` 更新为 `OVERWRITE` 并随导入请求提交。

## 根因

导入弹窗的 `el-radio-button` 使用了旧式 `label="SKIP"` / `label="OVERWRITE"` 作为选项值；当前项目同类 Element Plus 单选按钮普遍使用 `value`。用户点击“覆盖”时存在 v-model 未按预期更新的风险，导致提交仍是默认 `SKIP`。

## 最终验证

- `node scripts/showroom-admin-product-import-form.test.mjs` -> PASS，5 tests。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-showroom-product-import-overwrite-click-fix/bug-regression-evidence.md` -> PASS。

## Cleanup Candidates

- doc/tasks/20260531-showroom-product-import-overwrite-click-fix/bug-regression-evidence.md
