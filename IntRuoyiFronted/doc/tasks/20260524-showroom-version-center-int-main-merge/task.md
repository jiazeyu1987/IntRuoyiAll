# 任务：展厅版本中心前端融合到 int_main

## 任务目标

- 将 `task/20260523-showroom-version-center-impl` 前端版本中心实现融合到 `int_main` 基线。
- 在不覆盖主工作区现有未提交改动的前提下，先完成前端代码层融合、冲突解决与回归验证。
- 若主工作区脏状态阻止最终推进 `int_main`，必须显式记录阻塞和影响。

## 非目标

- 不处理与版本中心无关的主工作区未提交改动。
- 不通过 stash、reset、checkout 覆盖用户当前在 `int_main` 工作区的本地修改。
- 不引入 mock 数据、fallback UI 或静默跳过验证。

## 前序任务检查

- 已检查前一任务 `doc/tasks/20260523-showroom-version-center-implementation/task.md`
- 前一任务状态：`已完成`
- 当前可直接基于已放行实现继续做融合处理

## 里程碑

- [x] M1：建立融合任务包与执行日志。
- [x] M2：在前端 worktree 分支吸收 `int_main` 最新已提交历史并解决冲突。
- [x] M3：完成前端受影响验证并记录证据。
- [ ] M4：尝试推进前端 `int_main`，若阻塞则记录精确原因与影响。

## 预期验证

- `BDD: <scenario> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS`
- 至少覆盖：
  - `int_main` 已提交历史吸收后的路由/入口/版本中心回归
  - 版本中心局部类型检查
  - 若无法推进 `int_main`，记录具体未提交冲突文件与影响

## 当前状态

- 状态：阻塞
- 已完成：
  - 已确认主工作区 `int_main` 存在与本任务重叠的未提交改动，不能直接安全合并
  - 已确认当前分支相对 `int_main` 非快进：`git rev-list --left-right --count int_main...task/20260523-showroom-version-center-impl -> 6 1`
  - 已在前端 worktree 分支完成 `int_main` 已提交历史吸收，并解决 `CompanyWorkbench.vue` 冲突
  - 已继续吸收后续新增的 `int_main` 提交（包括 `1882f42b`），并解决 `src/router/modules/showroom.ts` 冲突，保留版本中心与提示管理两个入口
  - 已保留版本中心入口，移除与独立工作台重复的旧公司历史恢复逻辑
  - 已完成前端验证：
    - `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）
- 待完成：
  - 在主工作区 `int_main` 清理或提交重叠未提交改动后，再执行真正的分支前推
- 阻塞与影响：
  - 主工作区 `int_main` 脏状态与本任务存在重叠文件；若直接在主工作区 merge，会污染用户未提交改动或触发冲突
  - 前端全量 `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` 仍会 Node OOM；局部临时 `vue-tsc` 在当前 `int_main` 基线下又会暴露仓库级全局类型声明问题，无法作为版本中心 merge 的独立绿灯
  - 当前只能完成“代码层融合 + 脚本回归通过的 merge 提交”；不能在不处理主工作区未提交改动的前提下安全移动 `int_main` 指针
  - `scripts/showroom-admin-prompt-management.test.mjs` 在当前 worktree 环境下缺少直接依赖 `@vue/compiler-sfc`，无法作为本次版本中心 merge 的独立绿灯；版本中心相关 48 条脚本回归已单独通过
