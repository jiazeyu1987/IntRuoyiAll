# Task: 修复展厅主页页签与公司页签报错

## Goal

修复数字展厅前台进入“主页”页签与“公司”页签时报错的问题，确保真实前端入口中的这两个页签可以完成数据加载，而不是抛出前端运行时错误或展示错误提示。

## Scope

- 先检查并显式阻塞前一个未完成任务，再创建本任务文档。
- 复现 `http://localhost:8081` 中展厅前台“主页”与“公司”页签的报错路径。
- 记录 BDD 场景、RED 证据、根因和最小修复范围。
- 只修改与本次页签报错直接相关的展厅前台路由、数据解析、页面加载或定向测试。
- 保持无 fallback、无 mock、无静默降级。

## Non-Scope

- 不顺带改动展厅后台页签或 DCC、MES 等无关模块。
- 不新增展示层重设计。
- 不修改后端接口契约，除非根因证明当前前端违反了既有契约。

## Previous Task Check

- Previous task: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-frontstage-dynamic-route-guard\task.md`
- Status before this task: blocked.
- Impact: 上一任务的独立提交仍受 shared-foundation 文件边界阻塞，但已被显式标记为阻塞，不再阻塞本次缺陷修复记录的创建与执行。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Unrelated dirty files already exist in `.env`、MES 页面与其他 task docs。
- Impact: 本任务必须避免覆盖或提交无关改动。

## Milestones

- [ ] M1: 创建任务记录并确认主页/公司页签的真实报错链路。
- [ ] M2: 先补 RED 回归，证明当前页签存在可重复失败行为。
- [ ] M3: 实施最小修复并保持展厅其他前台路由行为不回退。
- [ ] M4: 运行 GREEN 定向验证并更新证据。
- [ ] M5: 完成收尾记录、清理预览与 task-scoped commit。

## Expected Verification

- `node --test scripts/showroom-frontstage.test.mjs`
- `node --test scripts/showroom-frontstage-runtime.test.mjs`
- `pnpm exec eslint src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/shared/payload.ts scripts/showroom-frontstage.test.mjs`
- 如本地登录前置齐全，则补充真实前端路径验证 `http://localhost:8081`

## Current Status

Completed. 前端公开 display 路由前缀已修正，且本地展厅公司现行内容与公司讲解已补齐；“主页 / 公司”页签恢复为可加载状态，不再出现红色报错提示。

## Final Verification Result

- PASS: `node --test scripts/showroom-frontstage.test.mjs`
- PASS: `node --test scripts/showroom-frontstage-runtime.test.mjs`
- PASS: `pnpm exec eslint src/api/showroom-frontstage/index.ts scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-runtime.test.mjs`
- PASS: Playwright CLI real-browser smoke on `http://127.0.0.1:8081/showroom/home`
  - title = `瑛泰`
  - errorCount = `0`
  - warningCount = `0`
- PASS: Playwright CLI real-browser smoke on `http://127.0.0.1:8081/showroom/company-intro`
  - title = `瑛泰`
  - rowCount = `4`
  - errorCount = `0`
  - warningCount = `0`

## Residual Risk

- None for this defect path. The follow-up backend task `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-live-persistence\` has already persisted company narration live data, and post-restart runtime checks now stay green.
