# 执行日志

BDD: 配置弹窗直接设置关键工序 -> Given 操作者进入工艺排产路线配置弹窗 / When 在“关键工序”列打开某行开关 / Then 该路线工序保存为关键工序，列表刷新后只保留一个关键工序。

BDD: 自动替换已有关键工序 -> Given 同一路线已有一行 `keyFlag=true` / When 操作者打开另一行关键工序开关 / Then 前端先把旧关键工序更新为 `false`，再把目标工序更新为 `true`。

BDD: 已启用路线禁止切换 -> Given 工艺路线状态为启用 / When 操作者查看配置弹窗关键工序列 / Then 开关置灰，不允许提交工序主数据修改，并提示需先停用路线。

RED: `node tests/e2e/mes-route-use-key-process-switch-static.spec.js` -> FAIL，当前配置表关键工序列缺少读取行级 `keyFlag` 的开关。

GREEN: `node tests/e2e/mes-route-use-key-process-switch-static.spec.js` -> PASS，关键工序列开关、行级 loading、权限/状态禁用、自动替换顺序和失败后重载契约通过。

GREEN: `node tests/e2e/mes-route-use-config-display-static.spec.js` -> PASS，既有排产用途配置表展示契约未被破坏。

GREEN: `node tests/e2e/mes-route-use-copy-buttons-static.spec.js` -> PASS，路线复制按钮契约未被破坏。

GREEN: `node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js` -> PASS，源路线跳转契约未被破坏。

GREEN: `node tests/e2e/mes-route-use-enabled-linkage-static.spec.js` -> PASS，用途启用联动契约未被破坏。

GREEN: `pnpm exec eslint src/views/mes/pro/route-use/RouteUsePage.vue src/api/mes/pro/route/process/index.ts tests/e2e/mes-route-use-key-process-switch-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/login-access.md` 与 Playwright 执行规范；本次真实 E2E 仅允许本机 `http://localhost:8081`、测试租户 `测试租户/aoteman/111111`，不访问测试服/正式服，不直接改库，不绕过真实页面路径。

GREEN: `node doc/tasks/20260708-schedule-route-key-process-switch/inspect-restore-route.cjs` -> PASS，已恢复前一次超时验证命中的测试租户真实路线 `RT000006`：`tenantId=122`，`routeId=922060`，`finalStatus=0`，`finalKeyProcessId=922661`。

GREEN: `node doc/tasks/20260708-schedule-route-key-process-switch/run-real-e2e.cjs` -> PASS，真实登录本机 `http://localhost:8081` 的测试租户 `测试租户/aoteman`，进入 `/mes/pro/schedule-route`，打开真实停用路线 `ROUTE-XLSX-00002` 的配置弹窗，通过“关键工序”列开关把关键工序从 `吹球囊成型(id=922509)` 切换到 `球囊裁剪(id=922510)`，后端校验同路线仅一个 `keyFlag=true`，随后通过页面开关恢复原关键工序；结果 `restored=true`、`routeStatusRestored=true`。

GREEN: real-e2e-artifacts -> PASS，真实 E2E 结果写入 `output/playwright/schedule-route-key-process-switch/result.json`，最终截图写入 `output/playwright/schedule-route-key-process-switch/schedule-route-key-process-switch-final.png`。
