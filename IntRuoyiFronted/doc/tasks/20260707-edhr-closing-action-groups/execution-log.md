﻿# EDHR 收尾动作五入口分组执行日志

BDD: 收尾动作五入口分组 -> Given 用户打开批次详情收尾/放行归档区域 / When 页面展示收尾操作 / Then 顶层只展示终态处理、归档打印、放行检查、放行审批、追溯记录 5 个主入口，原关闭、归档、下载、预检、检查项、放行审批、事件、变更、审计、域追溯等动作仍可从对应入口执行。

BDD: 放行检查合并预检和检查项 -> Given 批次存在或尚未存在放行事务 / When 用户打开放行检查入口 / Then 可以执行放行预检，并在检查项页签查看检查项，错误直接暴露。

BDD: 归档打印合并生成和下载 -> Given 批次进入归档阶段 / When 用户打开归档打印入口 / Then 可以生成归档、重新生成归档或下载 PDF，原归档权限和工作任务要求保持不变。

BDD: 放行审批只突出当前动作 -> Given 放行状态处于预检通过或待审批 / When 用户查看放行审批入口 / Then 主按钮文案显示当前可执行动作，其他放行动作收在更多菜单中。

BDD: 追溯记录集中查询 -> Given 用户需要查看放行事件、变更记录、操作审计或域追溯 / When 打开追溯记录入口 / Then 可通过页签进入四类记录，不再平铺四个按钮。

RED: 待执行 `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> 预期 FAIL，当前收尾区仍平铺 15 个按钮。

RED: `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> FAIL，当前收尾区缺少 `终态处理`、`归档打印`、`放行检查`、`放行审批`、`追溯记录` 5 个主入口。

GREEN: `apply_patch` -> PASS，将收尾区改为 5 个主入口；新增终态处理、归档打印、放行检查、放行审批、追溯记录抽屉/页签；保留原动作绑定、权限和禁用条件。

GREEN: `node tests/e2e/edhr-closing-action-groups-static.spec.js` -> PASS。

GREEN: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。

GREEN: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/edhr-closing-action-groups-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-closing-action-groups --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
