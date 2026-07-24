﻿# EDHR 收尾动作五入口分组

## 任务目标

将批次详情“收尾/放行归档”区域从平铺按钮改成 5 个主入口：终态处理、归档打印、放行检查、放行审批、追溯记录。每个主入口内部承载原动作，保留现有接口、权限、禁用条件和业务流程。

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次保持蓝灰运维台风格，减少平铺按钮密度。
- 前端文案：已读取 `clear-frontend-copy` 与 `copy-standards.md`，按钮采用简短、正式、清晰中文。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定 5 个主入口与原动作绑定。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过动作语义分组降低收尾区拥挤度，并用静态测试防回退。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 收尾动作五入口分组 -> Given 用户打开批次详情收尾/放行归档区域 / When 页面展示收尾操作 / Then 顶层只展示终态处理、归档打印、放行检查、放行审批、追溯记录 5 个主入口，原关闭、归档、下载、预检、检查项、放行审批、事件、变更、审计、域追溯等动作仍可从对应入口执行。

BDD: 放行检查合并预检和检查项 -> Given 批次存在或尚未存在放行事务 / When 用户打开放行检查入口 / Then 可以执行放行预检，并在检查项页签查看检查项，错误直接暴露。

BDD: 归档打印合并生成和下载 -> Given 批次进入归档阶段 / When 用户打开归档打印入口 / Then 可以生成归档、重新生成归档或下载 PDF，原归档权限和工作任务要求保持不变。

BDD: 放行审批只突出当前动作 -> Given 放行状态处于预检通过或待审批 / When 用户查看放行审批入口 / Then 主按钮文案显示当前可执行动作，其他放行动作收在更多菜单中。

BDD: 追溯记录集中查询 -> Given 用户需要查看放行事件、变更记录、操作审计或域追溯 / When 打开追溯记录入口 / Then 可通过页签进入四类记录，不再平铺四个按钮。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前仍为平铺按钮。
- [x] M3：实现 5 个主入口和内部动作分组，不改变业务逻辑。
- [x] M4：运行静态测试和必要语法检查，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交或报告提交阻塞。

## 预期验证

- `node tests/e2e/edhr-closing-action-groups-static.spec.js` 先 RED 后 GREEN。
- 收尾区顶层只出现 5 个主入口。
- 原动作绑定仍存在：关闭批次、生成归档、下载、放行预检、放行检查项、放行事件、提交/批准/驳回/撤回、质量拒收、申请重开、变更记录、操作审计、域追溯。

## 当前状态

completed

## 实现结果

- 收尾区顶层从 15 个平铺动作收敛为 5 个主入口：`终态处理`、`归档打印`、`放行检查`、`放行审批`、`追溯记录`。
- `终态处理` 内保留关闭批次、质量拒收、申请重开；`归档打印` 内保留生成归档、重新生成、下载 PDF。
- `放行检查` 内合并预检结果和检查项页签；`放行审批` 内突出当前可执行主动作，其余提交/批准/驳回/撤回保留在更多菜单。
- `追溯记录` 内合并放行事件、变更记录、操作审计和域追溯页签。
- 保留原有 API、权限、禁用条件、路由跳转和错误暴露逻辑，不新增 fallback、mock 或静默降级。

## 最终验证

- RED: `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> FAIL，当前收尾区缺少 5 个主入口。
- GREEN: `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-closing-action-groups-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-closing-action-groups --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
