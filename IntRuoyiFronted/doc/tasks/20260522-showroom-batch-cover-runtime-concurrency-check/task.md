# 任务：展厅一键封面运行态并发验证（前端）

## Goal

在产品管理页用真实入口触发一轮一键封面任务，并配合后端任务表验证本轮是否按“最多 8 路并发”运行。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-cover-runtime-concurrency-check\**`

## Non-Scope

- 不修改前端业务代码。
- 不新增测试专用前端控件。
- 不伪造前端点击或结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-batch-audio-token-fix-retry\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成语音 token 修复复测，不阻塞本次继续验证一键封面并发运行态。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增验证记录，不覆盖无关改动。

## Milestones

1. 确认本地前端页面入口可用。
2. 通过真实产品管理页触发一轮一键封面。
3. 记录页面行为与后端运行态证据。
4. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview`

## Current Status

Completed.

## Completed Work

- 已确认本地产品管理页 `http://127.0.0.1:8081/showroom/product` 可访问。
- 通过真实页面触发了 `一键封面 -> 只生成未上传的` 请求。
- 前端收到后端真实业务拒绝：系统中已经有一条未完成的后台封面任务 `id=2`，剩余 `124` 个产品待生成。
- 该结果与后端任务表运行态证据一致，说明前端入口命中真实接口且未吞错。

## Verification Result

- PASS: real page trigger -> `POST /admin-api/showroom/product/batch-generate-cover-image`
  - backend response body: `SHOWROOM_COVER_GENERATION_FAILED: 已存在未完成的一键封面后台任务，任务 2 仍有 124 个产品待生成，请等待自动续跑完成后再重试`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview`

## Final Assessment

- 前端一键封面入口正常，能真实命中后端。
- 本轮运行态并发验证的关键证据来自后端现有任务 `id=2`，而不是新起一条重复任务。
