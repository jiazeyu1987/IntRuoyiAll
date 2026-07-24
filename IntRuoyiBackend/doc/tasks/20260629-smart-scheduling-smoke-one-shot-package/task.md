# 任务：排产冒烟单次导出导入准备包

- Task ID: `20260629-smart-scheduling-smoke-one-shot-package`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

沉淀一条正式、可重复执行的本机准备链路：从 `tenant_id=1 / 芋道源码 / admin` 导出排产冒烟所需的最小配置范围，导入 `tenant_id=122 / 测试租户`，且不覆盖测试租户既有烟测账号体系；导入一次后应可直接用于后续真实排产烟测。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-system-nas-full-config-tool\task.md`
- 状态：`completed`
- 处理说明：NAS 完整配置工具任务已完成，本次仅在任务目录新增排产冒烟准备脚本与证据，不混改现有 NAS 代码。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接复用正式岗位/角色配置包、用户角色分配与 `mes/pro/scheduler-workbench/route-config` 导出导入合同，并把 smoke E2E 默认审批人收口为 supervisor 用户名，不新增旁路 SQL 或 mock 数据。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 最小配置包不会覆盖测试租户既有烟测账号 -> Given 测试租户已有可登录烟测账号 / When 执行一次导入准备 / Then 账号本体与密码保持可用，只补齐其所需菜单、角色范围与路线配置。`
- `BDD: 最小配置包可完成排产冒烟权限准备 -> Given 芋道源码租户已有排产相关菜单/角色/套餐配置 / When 脚本导出并导入测试租户 / Then 测试租户现有烟测账号可得到排产冒烟所需的正式权限与菜单范围。`
- `BDD: 路线配置包可跨租户完成排产配置准备 -> Given 芋道源码租户已有路线用途/排产配置/路线资源 / When 脚本导出路线配置包并导入测试租户 / Then 测试租户可得到自动排产所需的路线配置基础。`
- `BDD: 单次准备链路输出可追溯证据 -> Given 脚本执行完成 / When 查看任务产物目录 / Then 能看到导出文件、预检结果、导入结果与执行汇总，支持后续直接复跑烟测。`

## Milestones

1. M1：创建任务文档并明确正式准备链路范围。`completed`
2. M2：实现最小配置范围导出/导入脚本并输出证据。`completed`
3. M3：用真实租户执行脚本并记录结果。`completed`
4. M4：复跑排产烟测并更新最终结论。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-one-shot-package\prepare-smart-scheduling-smoke-package.mjs`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\smart-scheduling-smoke-real-flow-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\smart-scheduling-smoke-real-flow.e2e.js`

## Final Result

- `2026-06-29` 已完成：改为从 `芋道源码` 导出最小正式范围的岗位配置包、角色配置包与路线配置包，在 `测试租户` 导入后保留 `smoke*` 账号本体与密码，并重新绑定所需角色。
- `2026-06-29` 已完成：智能排产 smoke E2E 默认审批人从固定昵称 `eDHR矩阵-审批人` 收口为当前 supervisor 用户名；因此导入一次后可直接复跑 smoke，无需再额外传 `MES_SMOKE_FEEDBACK_APPROVER_NAME`。
- 本次真实通过证据：
  - 准备链路：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-one-shot-package\artifacts\2026-06-29T13-50-09-314Z\summary.json`
  - 最终 smoke：`D:\ProjectPackage\Int\IntRuoyi\output\smart-scheduling-smoke\SMART-SCHED-20260629140340\smoke-report.json`

## Residual Risks

- `tenant package 113` 当前菜单范围仍未被本任务扩容到与源租户全量一致；准备脚本已输出缺口证据 `tenant-package-coverage.json`。这不再阻塞本次 smoke，因为 smoke 当前以 `smoke*` 账号角色与真实默认参数已能直接通过。
