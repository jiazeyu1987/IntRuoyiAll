# 执行日志：展厅版本中心前端融合到 int_main

## 2026-05-24

- BDD: 前端融合准备 -> Given `task/20260523-showroom-version-center-impl` 已完成且 `int_main` 有新提交且主工作区脏, When 在隔离 worktree 分支吸收 `int_main` 已提交历史, Then 不覆盖主工作区未提交改动且产出可验证的融合结果
- RED: `git merge --ff-only int_main` -> FAIL, `task/20260523-showroom-version-center-impl` 与 `int_main` 已分叉，非快进
- INFO: `git status --short` @ `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> 主工作区存在与版本中心改动重叠的未提交文件，不能直接在主工作区安全 merge
- GREEN: `git merge --no-commit int_main` -> PASS（在前端 worktree 吸收 `int_main` 已提交历史，冲突已解）
- GREEN: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）
- RED: `pnpm exec vue-tsc --noEmit -p tsconfig.version-center.json` -> FAIL, 当前 `int_main` 基线缺少可直接复用的全局 auto-import 类型声明，局部配置无法单独收敛到版本中心改动
- RED: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> FAIL, Node OOM（仓库级既有问题，非版本中心 merge 特有）
- GREEN: `git merge --no-commit int_main` -> PASS（继续吸收最新 `int_main` 提交，解决 `src/router/modules/showroom.ts` 冲突）
- GREEN: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-company-version-tab.test.mjs` -> PASS（48 tests）
- RED: `node --test scripts/showroom-admin-prompt-management.test.mjs` -> FAIL, 当前 worktree 环境缺少 `@vue/compiler-sfc` 直接依赖，提示管理脚本无法执行
- BLOCKED: 前端主工作区 `int_main` 仍有与本任务重叠的未提交改动；当前不能安全执行真正的分支前推
