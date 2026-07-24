# 任务：排产单手动重排真实 E2E 样本可见性复核

- Task ID: `20260630-schedule-order-replan-real-e2e-sample-check`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `blocked`

## Task Goal

为排产单手动重排场景补一条真实 Playwright E2E 排查脚本，复核 `SCH-881MO090863-20260612-0001` 在测试租户真实页面中是否仍可见，并为后端“工单缺少生产用料清单”修复的页面级最终验收提供稳定样本入口。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-schedule-order-workorder-link\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成排产工单工单编码跳转增强；本次只新增真实 E2E 排查脚本与阻塞证据，不改业务页面逻辑。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实 E2E 前先走标准登录预检，不猜登录流程。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Node/Markdown/脚本文件统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过真实 E2E 脚本暴露样本漂移，而不是在页面里加兜底逻辑或伪造样本。
- `是否存在临时补丁或绕过`：否。脚本只做真实排查，不修改生产代码行为。

## BDD 场景

- `BDD: 真实排产单样本在测试租户可见时可进入手动重排 -> Given 测试租户页面中仍存在目标排产单 / When Playwright 登录并按工单编码筛选后打开手动重排 / Then 脚本应能命中目标排产单并继续验证后续阻塞是否仍为“工单缺少生产用料清单”。`
- `BDD: 真实样本漂移时应显式暴露 -> Given 目标排产单已不在测试租户页面可见范围 / When Playwright 登录并按工单编码筛选目标样本 / Then 脚本应以找不到目标行失败，并把失败点暴露为样本可见性问题。`

## Milestones

1. M1：建立前端任务文档并记录真实 E2E 样本排查目标。`completed`
2. M2：补真实 Playwright 排查脚本，覆盖登录、筛选、打开手动重排和阻塞信息采集。`completed`
3. M3：基于真实环境执行脚本并沉淀页面级最终验收结论。`blocked`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-order-replan-881mo090863-real-flow.e2e.js`

## Current Blockers

- 当前真实阻塞不是前端脚本缺失，而是目标样本 `SCH-881MO090863-20260612-0001` 与测试租户可见性不匹配：现有证据显示该排产单属于 `tenant_id=1`，而真实 E2E 使用的是测试租户 `tenant_id=122 / aoteman`。在未确认新的可见样本前，页面级最终验收只能阻塞并保留该脚本作为排障材料。
