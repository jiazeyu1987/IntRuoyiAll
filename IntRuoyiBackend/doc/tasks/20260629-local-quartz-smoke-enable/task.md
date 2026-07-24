# 任务：本地排产冒烟显式开启 Quartz

- Task ID: `20260629-local-quartz-smoke-enable`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

修复本机排产冒烟链路在触发 ERP 同步定时任务时被 `application-local.yaml` 禁用 Quartz 的阻塞，改为 `local` 常态加载 Quartz，同时显式收口本地不该自动跑的 Quartz 与 `@Scheduled` 任务，保证排产冒烟不再依赖额外启动参数。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-showroom-cover-service-startup-fix\task.md`
- 状态：`completed`
- 处理说明：后端启动阻塞已完成修复，本任务继续处理排产冒烟剩余运行态阻塞。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：命中登录 / 真实 Playwright E2E 与 PowerShell/中文编码门禁；高风险长链路前需记录 `experience-preflight`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：真实排产冒烟前必须先走官方 `login-preflight.mjs` 最小登录路径。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：PowerShell 5.1 读写中文台账、输出日志、注入中文租户参数时必须显式 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。直接把 Quartz 恢复为 `local` 默认能力，并通过本地自动任务门禁显式控制“哪些任务允许自动跑”，避免再依赖额外启动参数切模式。
- 是否存在临时补丁或绕过：否。不会手工改库、接口绕过或永久改写 `application-local.yaml` 默认策略。

## BDD 场景

- `BDD: local 默认加载 Quartz 且无需额外启动参数 -> Given 本地后端使用统一重启脚本以 local profile 启动 / When 排产冒烟调用 ERP 同步定时任务触发接口 / Then 本地运行态提供 Quartz Scheduler，不再返回 [定时任务 - 已禁用]。`
- `BDD: local 自动任务按白名单收口 -> Given 本地后端启动后会同步 infra_job 到 Quartz / When local 自动任务收口器执行 / Then 非白名单 Quartz 自动任务会被暂停，且 DCC/展厅的 @Scheduled 本地默认不装配。`

## Milestones

1. M1：建立任务文档并记录阻塞与 BDD/TDD 范围。`completed`
2. M2：补 RED 证据，锁定本地 Quartz 禁用根因。`completed`
3. M3：实现 local 常态加载 Quartz 与本地自动任务收口。`completed`
4. M4：重启后端并验证 Quartz/本地任务门禁运行态。`in_progress`
5. M5：复跑真实排产冒烟并回填最终阻塞。`pending`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_restart_ruoyi_script.py -q`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra -Dtest=JobStartupSyncRunnerTest,LocalQuartzAutoPauseRunnerTest test`
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi-backend.bat`
- `Invoke-WebRequest http://localhost:48081/actuator/health`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /erp/kingdee-sync --target-text ERP生产订单同步`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\smart-scheduling-smoke-real-flow.e2e.js`

## Current Blockers

- 当前待验证项：需要在新的 `local` 常态 Quartz 运行态下重启并确认本地自动任务收口生效，然后继续复跑 admin 租户排产冒烟长链路。

## Final Verification Result

- 进行中：已完成 local Quartz 常态加载与本地自动任务收口代码、脚本和定向测试；运行态验证与冒烟复跑待本轮后端重启后回填。
