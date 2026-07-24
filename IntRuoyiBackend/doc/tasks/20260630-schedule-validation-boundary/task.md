# 任务：排产专用后端验证入口收敛

- Task ID: `20260630-schedule-validation-boundary`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在 `yudao-module-mes` 内为排产链路建立正式、可执行的后端目标测试入口，让排产相关回归可以只编译/运行排产链路测试，并把 Spring 测试扫描范围收敛到 `cn.iocoder.yudao.module.mes`，不再被同模块内无关 eDHR 或其他域的测试扫描问题卡住。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-showroom-product-import-create-missing\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成；本次开始新的 MES 后端验证边界任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Python/PowerShell/Maven 命令输出统一显式 UTF-8，不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式 profile/runner 收敛编译与运行边界，不靠手工挑命令碰运气。
- `是否存在临时补丁或绕过`：否。不会删除现有全量测试入口。

## BDD 场景

- `BDD: 排产目标测试只编译排产链路测试 -> Given yudao-module-mes 同时存在排产与 eDHR 测试源码 / When 运行排产专用后端验证入口 / Then 测试编译阶段只覆盖排产链路相关测试源码。`
- `BDD: 排产 DB 测试按 MES 基包收敛扫描 -> Given BaseDbUnitTest 默认读取 application-unit-test.yaml 的大基包 / When 运行排产专用后端验证入口 / Then Spring 测试环境中的 yudao.info.base-package 固定为 cn.iocoder.yudao.module.mes。`
- `BDD: 全量 Maven 入口不被偷偷改弱 -> Given 仓库仍需保留完整后端回归能力 / When 未使用排产专用入口 / Then 现有全量 Maven 行为保持不变。`

## Milestones

1. M1：建立任务文档并锁定排产链路测试范围。`completed`
2. M2：补 RED 合同测试或现状失败证据。`completed`
3. M3：实现后端 targeted profile/runner。`completed`
4. M4：运行 GREEN 验证并回填证据。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_targeted_test_profile.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\run_mes_schedule_targeted_tests.py`

## Current Blockers

- 无。排产专用后端验证入口已完成，`run_mes_schedule_targeted_tests.py` 可独立通过。

## Cleanup Keep

- `doc/tasks/20260630-schedule-validation-boundary/backend-api-evidence.md`
