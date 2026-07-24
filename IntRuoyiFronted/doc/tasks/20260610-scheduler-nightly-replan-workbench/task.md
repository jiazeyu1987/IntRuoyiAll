# 20260610 排产员工作台前端增强

## 任务目标

配合后端目标 5 + 6，在排产员工作台展示夜间自动重排说明、今日产能、报工偏差、瓶颈建议和按顺序快捷入口。所有办理动作仍跳转现有页面。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。只增强现有工作台页面与 API 类型，不重复实现生产订单、排程、任务、报工页面。
- 是否存在临时补丁或绕过：否。

## 里程碑

1. 扩展静态契约测试。
2. 扩展 API 类型。
3. 调整工作台页面展示。
4. 前端静态测试、类型检查和 E2E 验证。

## 验证证据

- RED: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> FAIL，页面缺少快捷入口、夜间自动重排、报工偏差、瓶颈建议。
- GREEN: `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> PASS。
- E2E: `node tests/e2e/mes-pro-scheduler-workbench-real-flow.e2e.js` on `http://127.0.0.1:8094` with test tenant `测试租户/aoteman` -> PASS。
- MERGE E2E: `node tests/e2e/mes-pro-scheduler-workbench-real-flow.e2e.js` on `http://127.0.0.1:8081` with test tenant `测试租户/aoteman` -> PASS。

## 当前状态

已完成，已融合进 `int_main` 并完成合并后真实 E2E。
