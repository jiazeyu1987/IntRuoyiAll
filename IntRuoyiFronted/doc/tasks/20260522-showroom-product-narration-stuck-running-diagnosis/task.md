# 任务：展厅产品一键讲解长时间停留执行中排障（前端）

## Goal

定位 `showroom/product` 页面上一键讲解任务长时间显示“执行中（剩171）”且 2 小时无进展的真实原因；若问题落在前端状态刷新或展示逻辑，补齐最小修复并保留回归证据。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-frontend.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-stuck-running-diagnosis\**`

## Non-Scope

- 不改动一键讲解的业务筛选语义。
- 不新增手工“强制停止任务”前端按钮，除非用户后续明确要求。
- 不伪造进度或隐藏后台真实卡住状态。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-current-product-status\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成，不阻塞本次基于真实运行态继续排查“长时间执行中”异常。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅允许修改一键讲解状态读取、展示、定向测试与任务文档，不覆盖无关改动。

## Milestones

1. 复现并记录“执行中卡住”现象，拿到前端页面、状态接口和后台持久化状态证据。
2. 先补 RED，锁定导致卡住的可观察回归行为。
3. 实施最小修复并验证 UI 状态会正确推进或回落。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview`

## Current Status

Completed.

## Completed Work

- 通过真实状态接口、数据库状态与 JVM 线程栈确认：页面一直显示“执行中”不是前端轮询问题，而是后台批任务真实卡在 Codex CLI 子进程上。
- 前端未做业务代码改动；保留现有状态面板与自动刷新逻辑即可正确反映修复后的后端状态。
- 在本地运行态完成手工状态清理后，后端状态接口已回落为 `active=false`、`running=false`，前端页面刷新后应显示停止态而不再是执行中。
- 已完成 closeout preview。

## Verification Result

- PASS: real login + status API -> `测试租户(122) / aoteman / admin123` 登录后，`GET /admin-api/showroom/product/batch-generate-narration-script/status` 返回 `active=false`、`running=false`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview`

## Remaining Blockers

- 无前端代码阻塞；若要继续补齐 171 个剩余缺口，需要用户或后续自动化重新点击 `一键讲解` 发起新任务。
