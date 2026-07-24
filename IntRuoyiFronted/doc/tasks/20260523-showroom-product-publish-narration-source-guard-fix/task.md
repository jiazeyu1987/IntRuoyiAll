# 任务：展厅产品发布前讲解稿 source revision 误拦截修复

## Goal

修复 `展厅 -> 产品管理` 单条 `发布` 按钮的前端预校验误拦截问题，确保：

- 当当前产品待发布 revision 仍复用已保存的中英文讲解稿 source revision 时，前端不再提前报错拦截；
- 前端仍然对缺稿、空稿和中英文讲解稿 source 不一致的异常状态显式失败；
- 继续沿用后端既有 `sourceRevisionId + 新 revision 复制发布` 正式链路，不新增 fallback、mock 或兼容分支。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-product-publish-narration-source.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-product-publish-narration-source-guard-fix\**`

## Non-Scope

- 不改后端 `publishProduct` 正式发布契约
- 不改批量发布、审批流或前台展示逻辑
- 不新增 fallback、静默降级、mock 成功或默认通过分支

## Previous Task Check

- 上一个同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-infra-runtime-control-panel\task.md`
- 启动前状态：`Blocked on final frontend regression and local menu application`
- 阻塞与影响：该任务已明确记录为被既有 `ts:check` 旧错误和本地菜单 SQL 前置条件阻塞，且代码范围在 `infra/runtime-control`，与本次 showroom 发布链路无直接重叠；本次允许在独立范围内继续缺陷修复，但不得混入该任务文件。

## Milestones

- [x] M1：核对前置任务状态并创建本任务文档、执行日志。
- [x] M2：先补 RED，锁定“单条发布允许复用已保存讲解稿 source revision”的前端约束。
- [x] M3：最小修改产品单条发布前校验与直发 payload source 选择逻辑。
- [x] M4：运行定向回归验证并更新日志证据。
- [x] M5：完成收尾记录并准备前端单仓提交边界。

## Expected Verification

- `node --test scripts/showroom-product-publish-narration-source.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-product-publish-narration-source.test.mjs`
- 如本地运行态可用，补充真实前端路径验证：从 `http://localhost:8081/showroom/product` 进入产品管理后点击单条 `发布`

## Current Status

- Completed on 2026-05-23.
- 当前阶段：
  - 前端误拦截修复已完成，定向 RED/GREEN 与 lint 已通过。
  - 真实 Playwright 点击链路被当前本地 `showroom/product` 空白壳运行态阻塞，已记录到执行日志。

## Verification Summary

- PASS: `node --test scripts/showroom-product-publish-narration-source.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-product-publish-narration-source.test.mjs`
- PASS: `node tests/e2e/showroom-product-publish-entry.spec.js`
- BLOCKED: Playwright 真实点击验证。影响：当前无法在本机继续复现“详细保存新 revision 后列表再次发布”的完整 UI 点击链路，但源码级回归已锁定并修复本次前端误拦截。

## Closeout

- PASS: `task_closeout.py --mode preview`
- INFO: `task_closeout.py --mode apply` 被工具状态识别阻塞；已按 preview 名单手工清理临时 Playwright 脚本与失败截图，当前任务目录仅保留主记录文件。
