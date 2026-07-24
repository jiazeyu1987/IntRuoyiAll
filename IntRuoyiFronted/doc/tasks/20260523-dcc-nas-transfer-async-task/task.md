# 任务：DCC NAS 转移前端异步任务化

## Goal

把 `NAS管理 -> 转移到 DCC` 的前端交互从“等待同步长请求返回”改成“创建后台转移任务 + 展示任务状态 + 轮询进度”，确保大目录转移不再因为默认 `30000ms` 请求超时而让用户误判失败。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\dcc\controlledFile\workflow.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-dcc-nas-transfer-async-task\**`

## Non-Scope

- 不重做 `NAS管理` 页面整体布局
- 不新增 mock、fallback、静默成功或“只把超时调大”的临时兜底方案
- 不修改与 NAS 转移无关的 showroom、infra、MES 页面

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-product-publish-narration-source-guard-fix\task.md`
- Status before this task: `Completed with runtime verification blocker`
- Impact on this task: 上一任务源码级验证已完成，阻塞仅在本地 showroom 真实运行态，与本次 `system/nas` 改动不重叠；本任务可继续推进，但不得混入其相关文件。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志。
- [ ] M2：梳理前端现有同步提交流程与可复用的任务状态轮询模式。
- [ ] M3：先补 RED，锁定“确认转移后立即返回任务态而不是等待长请求”的前端契约。
- [ ] M4：最小实现任务提交、轮询、完成态与失败态展示。
- [ ] M5：运行定向回归验证，回写证据并完成 closeout 预览。

## Expected Verification

- `node --test scripts/system-nas-management.test.mjs`
- `pnpm exec eslint src/views/system/nas/index.vue src/api/dcc/controlledFile/workflow.ts scripts/system-nas-management.test.mjs --format stylish`
- 如本地运行态可用，补充真实前端路径验证：`http://localhost:8081/system/nas`

## Current Status

Completed on 2026-05-23. 已通过真实 `芋道源码 / admin / admin123` 前端路径进入 `http://127.0.0.1:8081/system/nas`，完成 `测试连接 -> 刷新目录 -> 选择 -> 展开 1. QMS documents -> 勾选 5.STM实验室规程 -> 转移到 DCC -> 确认开始`，页面不再报 `timeout of 30000ms exceeded`，而是立即进入 `转移任务` 状态块并持续轮询。

## Final Verification Result

- PASS: `node --test scripts/system-nas-management.test.mjs`
- PASS: `pnpm exec eslint src/views/system/nas/index.vue src/api/dcc/controlledFile/workflow.ts scripts/system-nas-management.test.mjs --format stylish`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: 真实 Playwright 前端路径验证。`http://127.0.0.1:8081/system/nas` 使用 `芋道源码 / admin / admin123` 登录后，页面显示 `转移任务` 块，任务 `id=1` 从 `待处理条目=533 / 成功文件=113` 轮询更新到 `待处理条目=278 / 成功文件=381`，且页面全程未再出现 `timeout of 30000ms exceeded`

## Blockers

- 无当前任务阻塞
