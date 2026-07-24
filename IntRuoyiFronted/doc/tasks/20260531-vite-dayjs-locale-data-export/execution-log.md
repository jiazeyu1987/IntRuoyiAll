# 执行日志：修复 Vite 日期组件 dayjs localeData 默认导出报错

## BDD

- BDD: Vite 开发模式加载日期组件依赖 -> Given 前端以 Vite dev 模式启动 / When Element Plus 日期组件请求 `dayjs/plugin/localeData.js` / Then 浏览器应拿到带默认导出的 ESM 预构建依赖，不出现 `does not provide an export named 'default'`。

## TDD

- RED: `node tests\e2e\vite-dayjs-plugin-optimize-deps.spec.js` -> FAIL, expected reason: `Vite windows-safe optimizeDeps.include must pre-optimize dayjs/plugin/advancedFormat.js`。
- GREEN: `node tests\e2e\vite-dayjs-plugin-optimize-deps.spec.js` -> PASS。
- GREEN: `node tests\e2e\vite-element-plus-optimize-deps.spec.js` -> PASS。
- GREEN: `VITE_OPTIMIZE_PROFILE=windows-safe` 临时 Vite dev 冷启动后检查 `dayjs_plugin_localeData__js.js` -> PASS，文件包含默认导出。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-vite-dayjs-locale-data-export/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-vite-dayjs-locale-data-export --mode preview` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-vite-dayjs-locale-data-export --mode apply` -> PASS。

## 证据

- 用户提供浏览器错误：`use-date-table.mjs?v=2eee7f5f:3 Uncaught SyntaxError: The requested module '/node_modules/.pnpm/dayjs@1.11.13/node_modules/dayjs/plugin/localeData.js?v=2eee7f5f' does not provide an export named 'default'`。
- 根因：`VITE_OPTIMIZE_PROFILE=windows-safe` 的 `noDiscovery: true` 禁止自动发现依赖，但白名单缺少 Element Plus `es` 产物引用的 `dayjs/plugin/*.js` CommonJS 插件子路径。
- 修复：补齐 `advancedFormat`、`customParseFormat`、`dayOfYear`、`isSameOrAfter`、`isSameOrBefore`、`localeData`、`weekOfYear`、`weekYear` 8 个插件子路径。
- closeout：已清理本次临时 Vite stdout/stderr 日志；bug regression evidence 已通过校验后按 closeout 规则删除，核心证据保留在本日志。
- 提交状态：`vite.config.ts` 修复已存在于当前 HEAD `a2503889c`；本任务提交仅包含回归测试与任务记录，避免带入既有展厅错误格式化改动。
