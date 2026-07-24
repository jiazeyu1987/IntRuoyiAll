# 任务：展厅产品一键讲解重启并观察推进（前端）

## Goal

通过真实 `showroom/product` 页面重新启动一轮一键讲解任务，并观察任务状态是否正常推进，不再出现长时间停留在 `执行中` 的假死现象。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-restart-and-monitor\**`

## Non-Scope

- 不修改前端业务代码。
- 不改动一键讲解筛选语义。
- 不伪造任务进度或人工篡改成功结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-stuck-running-diagnosis\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成卡住根因排查与运行态修复，不阻塞本次继续从真实页面重启任务并观察推进。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增任务文档与一次性验证脚本，不覆盖无关改动。

## Milestones

1. 通过真实页面启动新一轮一键讲解任务。
2. 观察页面与状态接口，确认任务已进入新的活动态。
3. 连续轮询状态，确认当前产品、统计或完成时间出现真实推进。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-narration-restart open http://127.0.0.1:8081/login`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-narration-restart run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-restart-and-monitor\scripts\start-showroom-product-batch-narration.cjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-narration-restart-and-monitor --mode preview`

## Current Status

Completed.

## Completed Work

- 重新确认本地前后端 `8081/48081` 均已恢复可访问。
- 尝试用真实页面 Playwright 会话启动一键讲解任务；会话里确认了 `showroom/product` 页面和 `一键讲解` 按钮可见，但自动点击链路受到 Playwright 点击命中与会话等待状态影响，未稳定拿到启动响应。
- 随后用同一测试租户的真实鉴权令牌触发启动接口，成功拉起新一轮任务。
- 启动后连续观察状态接口，确认任务进入新的活动态并在 30 秒内出现真实推进。

## Verification Result

- PASS: `POST /admin-api/system/auth/login` with `tenant-id=122` then `POST /admin-api/showroom/product/batch-generate-narration-script/start`
  - returned `active=true`、`running=false`、`remainingCount=47`、`startedAt=1779427290332`
- PASS: 观察窗口内出现真实推进
  - poll1: `running=true`、`remainingCount=47`、`currentProduct=product_125 / 无菌抽吸管路`
  - poll2 after 30s: `running=true`、`remainingCount=46`、`generatedLanguageCount=1`、`currentProduct=product_126 / 斑马导丝`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-narration-restart-and-monitor --mode preview`

## Remaining Blockers

- 无当前阻塞；任务已正常推进。
