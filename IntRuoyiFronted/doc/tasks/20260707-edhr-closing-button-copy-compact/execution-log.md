﻿# EDHR 收尾按钮文案精简执行日志

BDD: 收尾按钮短文案 -> Given 用户打开批次详情收尾/放行归档区域 / When 页面展示关闭、归档、放行、拒收、重开和追溯类操作 / Then 每个可见按钮文案不超过 4 个汉字，且原点击方法、权限和禁用条件保持不变。

RED: 待执行 `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> 预期 FAIL，当前按钮包含“关闭批次”“生成最终归档”“下载打印版PDF”“放行检查项”“质量终态拒收”等超过 4 字文案。

RED: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> FAIL，当前超长按钮为 `生成最终归档`、`下载打印版PDF`、`放行检查项`、`质量终态拒收`、`批次变更记录`、`批次操作审计`。

GREEN: `apply_patch` -> PASS，将收尾区长按钮改为 `生成归档`、`下载`、`放行检查`、`质量拒收`、`变更记录`、`操作审计`，保留原点击绑定、权限和禁用条件。

GREEN: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。

GREEN: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-closing-button-copy-compact --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
