# 任务：展厅公司英文介绍可编辑真实 E2E 验证

## Goal

使用真实测试租户与真实前后端运行环境，对 `showroom/company` 中“英文介绍可手动修改、重新生成语音后可保存、播放入口使用音频组件”的链路进行一次完整 E2E 验证，确认当前实际系统行为与需求一致，不使用 mock 数据、不绕过前端路径。

## Scope

- 从 `http://localhost:8081/showroom/company` 真实进入后台页面。
- 使用测试租户真实登录后，执行 `AI生成介绍 -> 手改英文 -> 生成语音 -> 保存语音 -> 重新打开弹框确认英文回显 -> 调接口确认 live EN narration`。
- 记录任务文档与执行日志。

## Non-Scope

- 不修改生产代码或测试代码。
- 不通过接口直接替代前端点击路径。
- 不新增前端控件、假数据、fallback 或临时兼容逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-narration\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一任务已完成英文介绍可编辑与播放器交互改造；本次仅做真实数据 E2E 放行验证，不再改动实现。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在多组与本任务无关的在途改动与未跟踪任务目录。
- Impact: 本任务仅允许新增本任务文档与一次性 E2E 验证脚本，不触碰无关文件。

## Milestones

- [x] M1: 检查上一同仓任务状态并创建本任务文档。
- [x] M2: 编写一次性真实 E2E 验证脚本，锁定可观察路径。
- [x] M3: 使用 Playwright 运行真实用户路径验证并记录结果。
- [x] M4: 执行 closeout preview/apply，只保留 `task.md / execution-log.md`。
- [x] M5: 在不混入无关改动的前提下提交本任务文档收尾。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-editable-english-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-real-e2e\scripts\verify-showroom-company-editable-english-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-real-e2e --mode preview`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-real-e2e --mode apply`

## Current Status

Completed on 2026-05-21.

真实 E2E 已通过，当前仅剩 closeout 清理与任务文档提交收尾。

## Blockers And Impact

- Blocker: none.
- Impact: none.

## Final Verification Result

- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-editable-english-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-editable-english-real-e2e\scripts\verify-showroom-company-editable-english-real-e2e.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-real-e2e --mode preview`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-editable-english-real-e2e --mode apply`
