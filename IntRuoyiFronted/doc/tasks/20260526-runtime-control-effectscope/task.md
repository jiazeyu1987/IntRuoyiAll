# 任务：修复运行控制台 EffectScope 初始化报错

## 任务目标

- 修复访问 `http://localhost:8081/infra/monitors/runtime-control` 时出现 `Uncaught TypeError: EffectScope is not a constructor` 的前端启动失败。
- 不通过 mock、静默降级、清空缓存说明或隐藏异常解决问题；应从 Vite 依赖优化配置或真实导入链路上消除初始化顺序错误。

## 前序任务检查

- 前端上一同仓任务：`doc/tasks/20260525-automation-2-ebr-visual-fidelity/task.md`
- 状态：已记录自动清理/合并阻塞，当前仓库工作区干净。
- 影响：该阻塞不影响本次运行控制台修复启动。

## BDD 场景

- BDD: 运行控制台冷启动依赖优化后可正常初始化 Pinia -> Given 前端开发服务使用空的 Vite optimized deps 缓存启动, When 用户访问 `/infra/monitors/runtime-control`, Then Pinia 创建 store 时 Vue `EffectScope` 已完成初始化，浏览器控制台不出现 `EffectScope is not a constructor`。

## 里程碑

- [x] M1：复现并定位 `EffectScope` 初始化顺序错误来源。
- [x] M2：新增会先失败的回归测试覆盖 Vite optimized deps 中 Vue/Pinia 初始化契约。
- [x] M3：实施最小可维护修复，消除错误的预构建输出。
- [x] M4：运行目标测试、类型/构建或浏览器验证，并记录 RED/GREEN 证据。
- [x] M5：完成 task-closeout-cleanup 预览、提交本任务改动。

## 预期验证

- RED：新增回归测试在当前配置下失败，证明 optimized deps 生成的 `pinia.js` 可在未初始化 Vue reactivity 的情况下调用 `effectScope`。
- GREEN：目标回归测试通过。
- GREEN：冷启动开发服务访问 `/infra/monitors/runtime-control` 不再出现 `EffectScope is not a constructor`。

## 当前状态

- 状态：completed。
- 已完成：前序任务检查、当前工作区检查、问题复现、回归测试、Vite cacheDir 隔离修复、浏览器验证和类型检查。
- 当前阻塞：无。

## 验证结果

- RED：`node tests\e2e\vite-cache-dir-isolation.spec.js` -> FAIL，共享 `node_modules\.vite` 缓存会让不同端口 dev server 互相覆盖 optimized deps。
- GREEN：`node tests\e2e\vite-cache-dir-isolation.spec.js` -> PASS。
- GREEN：`node tests\e2e\vite-element-plus-optimize-deps.spec.js` -> PASS。
- GREEN：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- GREEN：Playwright 访问 `http://localhost:8081/infra/monitors/runtime-control` -> PASS，跳转登录页且无 `EffectScope is not a constructor`、`Outdated Optimize Dep` 或动态导入失败日志。
- GREEN：`task_closeout.py --mode preview` -> PASS，无删除项、无阻塞。
- GREEN：`task_closeout.py --mode apply` -> PASS，无删除项、无阻塞。

## Cleanup Keep

- `doc/tasks/20260526-runtime-control-effectscope/bug-regression-evidence.md`

## Current Status

completed
