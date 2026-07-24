# 任务：修复 Vite 日期组件 dayjs localeData 默认导出报错

## 任务目标

修复本地前端启动后 Element Plus 日期组件加载 `dayjs/plugin/localeData.js` 时出现的浏览器运行时错误：`does not provide an export named 'default'`，确保 Vite 开发模式把相关 dayjs 插件作为可用的 ESM 依赖提供。

## 前序任务检查

- 已确认上一前端任务 `doc/tasks/20260531-showroom-product-import-same-action/task.md` 状态为 completed。
- 当前前端仓库已有未提交改动 `vite.config.ts`、`src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 与一个历史任务目录；本任务只增量处理 Vite/dayjs 运行时错误，不回退既有改动。

## BDD 场景

- BDD: Vite 开发模式加载日期组件依赖 -> Given 前端以 Vite dev 模式启动 / When Element Plus 日期组件请求 `dayjs/plugin/localeData.js` / Then 浏览器应拿到带默认导出的 ESM 预构建依赖，不出现 `does not provide an export named 'default'`。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：用本地静态回归用例复现缺失依赖配置。
- [x] M3：实现最小配置修复。
- [x] M4：运行 GREEN 与必要回归验证。
- [x] M5：记录证据、closeout 预览并清理本次临时产物。
- [x] M6：提交本任务改动。

## 预期验证

- RED：新增静态回归测试，先失败并指出 `vite.config.ts` 的 Windows 安全预构建白名单缺少 Element Plus 日期组件依赖的 dayjs 插件。
- GREEN：同一测试通过。
- REGRESSION：运行 `pnpm ts:check` 或等价类型检查；若默认堆限制失败，按既有任务证据使用 `NODE_OPTIONS=--max-old-space-size=8192` 复验。

## Current Status

completed

## 当前状态

status: completed

已修复 Windows 安全预构建 profile 缺少 Element Plus dayjs 插件依赖的问题，并完成回归验证。

## 完成工作

- 新增 `tests/e2e/vite-dayjs-plugin-optimize-deps.spec.js`，扫描 Element Plus `es` 产物中的 `dayjs/plugin/*.js` 导入并验证 Vite 预构建白名单覆盖。
- 在 `vite.config.ts` 的 `windowsSafeOptimizeInclude` 中补齐 Element Plus 日期/时间组件实际使用的 8 个 dayjs 插件子路径。

## 最终验证

- `node tests\e2e\vite-dayjs-plugin-optimize-deps.spec.js` -> PASS。
- `node tests\e2e\vite-element-plus-optimize-deps.spec.js` -> PASS。
- `VITE_OPTIMIZE_PROFILE=windows-safe` 临时 Vite dev 冷启动 -> PASS，生成的 `dayjs_plugin_localeData__js.js` 带默认导出。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-vite-dayjs-locale-data-export/bug-regression-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-vite-dayjs-locale-data-export --mode preview` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-vite-dayjs-locale-data-export --mode apply` -> PASS，已清理临时 Vite 日志；证据文件已按 closeout 规则删除。

## 提交状态

- `vite.config.ts` 的 Vite 修复已存在于当前 HEAD 提交 `a2503889c`。
- 本任务仅暂存并提交新增回归测试与任务记录，避免带入既有展厅错误格式化改动。

## Cleanup Candidates

- 已执行 closeout 清理；保留 `task.md` 与 `execution-log.md`。
