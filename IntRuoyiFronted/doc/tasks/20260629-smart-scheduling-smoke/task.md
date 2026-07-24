# 任务：排产冒烟测试与阻塞项盘点

- Task ID: `20260629-smart-scheduling-smoke`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

复用现有 `smart-scheduling-smoke-real-flow` 自动化脚本，在本机真实前端入口执行排产主链路冒烟，验证脚本静态契约、登录前置与真实运行态，定位当前阻塞项。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-showroom-award-generate-cover-version\task.md`
- 状态：`blocked`
- 处理说明：上一任务已因用户优先级切换暂停，不与本次排产冒烟混改。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：命令输出与任务文档写入统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：真实 Playwright E2E 前必须先执行 `login-preflight.mjs` 最小登录路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接以现有排产冒烟脚本和真实 UI 链路为准，不追加旁路验证。
- `是否存在临时补丁或绕过`：否。
- 用户于 2026-06-29 明确授权：若排产冒烟依赖的测试租户配置缺失，可从 `芋道源码/admin` 只读导出所需配置，再导入 `测试租户`；冲突数据允许覆盖，但范围仅限本次排产冒烟准备数据。

## BDD 场景

- `BDD: 排产静态契约可通过 -> Given 现有排产冒烟脚本与页面契约存在 / When 运行静态校验 / Then 先确认脚本依赖和关键页面片段未被破坏。`
- `BDD: 排产真实链路暴露真实阻塞 -> Given 测试租户可登录本机前端 / When 执行真实排产冒烟脚本 / Then 输出脚本成功证据或首个真实阻塞点。`
- `BDD: ERP 生产用料清单同步应成为前端 smoke 门禁 -> Given 冒烟脚本通过 ERP 页签新建并提交工单 / When 准备进入自动排产链路 / Then 脚本必须在前端触发并等待生产用料清单同步可见，否则 fail-fast 暴露上游前置缺失。`

## Milestones

1. M1：补建任务文档并阻断前序未完成任务。`completed`
2. M2：执行静态校验、登录前置与 experience-preflight。`completed`
3. M3：执行真实排产冒烟并收集产物。`completed`
4. M4：回填阻塞项与最终结论。`completed`
5. M5：补前端 smoke 的生产用料清单同步门禁并复跑。`completed`

## Expected Verification

- `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js`
- `node ..\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/task --target-text 生产排产`
- `node tests/e2e/smart-scheduling-smoke-real-flow.e2e.js`

## Current Blockers

- 无。前端真实 smoke `SMART-SCHED-20260630-RERUN11` 已完整通过。

## Final Verification Result

- `node tests/e2e/smart-scheduling-smoke-real-flow-static.spec.js`：PASS。
- `node ..\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/task --target-text 生产排产`：PASS。
- `MES_SMOKE_*` 真实整链路：`SMART-SCHED-20260629-LOCAL-FULL-13` PASS。
- `2026-06-30`：已把 `kingdeeProductionMaterialListSyncJob` 触发与 `/erp/production-material-list/page` 可见性等待纳入前端 smoke 脚本，避免自动排产前置漏检。
- `2026-06-30`：根因补数后，`SMART-SCHED-20260630-RERUN4` 的自动排产预览已从 `blockingIssueCount=12` 推进到 `blockingIssueCount=0`，说明前端新增的“生产用料清单同步门禁”已对准真实上游数据阻塞，而不是页面误判。
- `2026-06-30`：当前待继续验证的是把 `MES_SMOKE_ERP_UNIT_NUMBER` 恢复为历史成功值 `PCS` 后，整条前端 smoke 是否能继续推进到自动排产、第三方导入、归属、审批与进度回写。
- `2026-06-30`：已把日历校验改为跟随“本次发布任务的排程月份”，避免用系统当前月份误扫历史脏排程单。
- `2026-06-30`：当前测试租户真实可用的 smoke 审批账号已收口为 `smokeappr1`，只读账号收口为 `smokeread1`；旧脚本中的 `smokesup1 / smokenon1` 已不再适配当前测试租户用户基线。
- `2026-06-30`：`SMART-SCHED-20260630-RERUN11` 已完整通过，前端真实链路当前可复用的账号组合为 `aoteman / smokeplan1 / smokeappr1 / smokeread1`。
- 当前前端收口结论：
  - 自动排产入口权限应绑定 `mes:pro-auto-schedule:preview`。
  - 正向烟测只需要覆盖首个真实串行边界工序即可验证“导入 -> 归属 -> 确认报工 -> 审批 -> 进度回写”主链路。
  - 待归属页的人员选择依赖 `system:user:query`，若测试租户缺该权限，导入后仍无法完成确认报工。
