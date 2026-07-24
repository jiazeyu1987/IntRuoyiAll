# 任务：修复前端测试模式构建 EMFILE 阻塞

## 任务目标

- 修复 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 在测试服发布口径下执行 `pnpm exec vite build --mode test` 时的 `EMFILE: too many open files` 阻塞。
- 保持现有发布脚本语义不变，优先通过最小前端构建配置修复让发布链路恢复。

## 非目标

- 不改动测试服/正式服发布脚本的业务流程。
- 不引入 fallback 构建分支掩盖真实问题。
- 不顺手重构无关前端功能。

## 前序任务检查

- 已检查上一任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-version-center-release-readiness-check\task.md`
- 上一任务状态：`阻塞`
- 影响：该阻塞正是本任务要解除的发布前置条件，不另起并行冲突任务。

## 里程碑

- [x] M1：建立任务记录并复核当前阻塞命令。
- [x] M2：定位 `vite build --mode test` 的 `EMFILE` 根因。
- [x] M3：实施最小修复并验证发布口径构建恢复。
- [x] M4：更新发布就绪结论。

## 预期验证

- `pnpm exec vite build --mode test`
- 发布脚本口径模拟：
  - `NODE_OPTIONS=--max-old-space-size=8192`
  - `VITE_BASE_URL=http://172.30.30.58:48081`
  - `VITE_BASE_PATH=/`
  - `VITE_OUT_DIR=dist-intruoyi-test`
  - `pnpm exec vite build --mode test`

## 当前状态

- 状态：已完成

## Completed Work

- 定位到 `EMFILE` 来自 Windows 下 Vite/Rollup 大量并发读取 `element-plus` 与构建期 `auto-import` 声明写入叠加导致的文件句柄耗尽。
- 在 `build/vite/index.ts` 中将 `unplugin-auto-import` 的 `dts` 生成改为仅开发态启用，build 阶段关闭。
- 在 `vite.config.ts` 中引入 `graceful-fs` 对 Node `fs` 做 `gracefulify`，并将 Rollup `maxParallelFileOps` 收紧到 `1`，优先保证发布构建稳定。
- 补充静态回归脚本 `scripts/showroom-build-emfile-guard.test.mjs` 锁定两条构建护栏。
- 已确认版本中心前端功能脚本回归保持通过。

## Verification Evidence

- `node --test scripts/showroom-build-emfile-guard.test.mjs` -> PASS
- 发布脚本口径模拟（第一次）：
  - `NODE_OPTIONS=--max-old-space-size=8192`
  - `VITE_BASE_URL=http://172.30.30.58:48081`
  - `VITE_BASE_PATH=/`
  - `VITE_OUT_DIR=dist-intruoyi-test`
  - `pnpm exec vite build --mode test`
  - 结果：PASS
- 发布脚本口径模拟（第二次，同口径重跑） -> PASS
- `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）

## Notes

- 裸命令 `pnpm exec vite build --mode test` 与 `pnpm build:test` 在当前机器上仍可能触发 `EMFILE`；但测试服实际发布脚本使用的是“清理 `.vite` 缓存 + 明确 `NODE_OPTIONS/VITE_*` 环境变量”的构建口径，本任务已恢复该真实发布路径。
